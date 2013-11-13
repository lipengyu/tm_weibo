package era_misa.tm.lda;

import java.io.File;

import era_misa.tm.util.FormatDocument;
import era_misa.tm.util.RAndW;

/*
 * (C) Copyright 2005, Gregor Heinrich (gregor :: arbylon : net) 
 * LdaGibbsSampler is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * LdaGibbsSampler is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */


public class LdaGibbsSampler {
    /**
     * document data (term lists)
     */
    int[][] documents;
    /**
     * vocabulary size
     */
    int V;
    /**
     * number of topics
     */
    int K;
    /**
     * Dirichlet parameter (document--topic associations)
     */
    double alpha;
    /**
     * Dirichlet parameter (topic--term associations)
     */
    double beta;
    /**
     * topic assignments for each word.
     * N * M 维，第一维是文档，第二维是word
     */
    int z[][];
    /**
     * nw[i][j] number of instances of word i (term?) assigned to topic j.
     */
    int[][] nw;
    /**
     * nd[i][j] number of words in document i assigned to topic j.
     */
    int[][] nd;
    /**
     * nwsum[j] total number of words assigned to topic j.
     */
    int[] nwsum;
    /**
     * nasum[i] total number of words in document i.
     */
    int[] ndsum;
    /**
     * cumulative statistics of theta
     */
    double[][] thetasum;
    /**
     * cumulative statistics of phi
     */
    double[][] phisum;
    /**
     * size of statistics
     */
    int numstats;
    /**
     * sampling lag (?)
     */
    private static int THIN_INTERVAL = 20;

    /**
     * burn-in period
     */
    private static int BURN_IN = 100;

    /**
     * max iterations
     */
    private static int ITERATIONS = 1000;

    /**
     * sample lag (if -1 only one sample taken)
     */
    private static int SAMPLE_LAG;

    private static int dispcol = 0;

    /**
     * Initialise the Gibbs sampler with data.
     * 
     * @param V
     *            vocabulary size
     * @param data
     */
    public LdaGibbsSampler(int[][] documents, int V) {

        this.documents = documents;
        this.V = V;
    }

    /**
     * Initialisation: Must start with an assignment of observations to topics ?
     * Many alternatives are possible, I chose to perform random assignments
     * with equal probabilities
     * 
     * @param K
     *            number of topics
     * @return z assignment of topics to words
     */
    public void initialState(int K) {
        int i;

        int M = documents.length;

        // initialise count variables.
        nw = new int[V][K];
        nd = new int[M][K];
        nwsum = new int[K];
        ndsum = new int[M];

        // The z_i are are initialised to values in [1,K] to determine the
        // initial state of the Markov chain.
        // 为了方便，他没用从狄利克雷参数采样，而是随机初始化了�?

        z = new int[M][];
        for (int m = 0; m < M; m++) {
            int N = documents[m].length;
            z[m] = new int[N];
            for (int n = 0; n < N; n++) {
            	//随机初始化！
                int topic = (int) (Math.random() * K);
                z[m][n] = topic;
                // number of instances of word i assigned to topic j
                // documents[m][n] 是第m个doc中的第n个词
                nw[documents[m][n]][topic]++;
                // number of words in document i assigned to topic j.
                nd[m][topic]++;
                // total number of words assigned to topic j.
                nwsum[topic]++;
            }
            // total number of words in document i
            ndsum[m] = N;
        }
    }

    /**
     * Main method: Select initial state ? Repeat a large number of times: 1.
     * Select an element 2. Update conditional on other elements. If
     * appropriate, output summary for each run.
     * 
     * @param K
     *            number of topics
     * @param alpha
     *            symmetric prior parameter on document--topic associations
     * @param beta
     *            symmetric prior parameter on topic--term associations
     */
    private void gibbs(int K, double alpha, double beta) {
        this.K = K;
        this.alpha = alpha;
        this.beta = beta;

        // init sampler statistics
        if (SAMPLE_LAG > 0) {
            thetasum = new double[documents.length][K];
            phisum = new double[K][V];
            numstats = 0;
        }

        // initial state of the Markov chain:
        //启动马尔科夫链需要一个起始状�?
        initialState(K);

        //每一轮sample
        for (int i = 0; i < ITERATIONS; i++) {

            // for all z_i
            for (int m = 0; m < z.length; m++) {
                for (int n = 0; n < z[m].length; n++) {

                    // (z_i = z[m][n])
                    // sample from p(z_i|z_-i, w)
                	//核心步骤，�?过论文中表达式（78）为文档m中的第n个词采样新的topic
                    int topic = sampleFullConditional(m, n);
                    z[m][n] = topic;
                }
            }

            // get statistics after burn-in
            //如果当前迭代轮数已经超过 burn-in的限制，并且正好达到 sample lag间隔
            //则当前的这个状�?是要计入总的输出参数的，否则的话忽略当前状�?，继续sample
            if ((i > BURN_IN) && (SAMPLE_LAG > 0) && (i % SAMPLE_LAG == 0)) {
                updateParams();
            }
        }
    }

    /**
     * Sample a topic z_i from the full conditional distribution: p(z_i = j |
     * z_-i, w) = (n_-i,j(w_i) + beta)/(n_-i,j(.) + W * beta) * (n_-i,j(d_i) +
     * alpha)/(n_-i,.(d_i) + K * alpha)
     * 
     * @param m
     *            document
     * @param n
     *            word
     */
    private int sampleFullConditional(int m, int n) {

        // remove z_i from the count variables
    	//这里首先要把原先的topic z(m,n)从当前状态中移除
        int topic = z[m][n];
        nw[documents[m][n]][topic]--;
        nd[m][topic]--;
        nwsum[topic]--;
        ndsum[m]--;

        // do multinomial sampling via cumulative method:
        double[] p = new double[K];
        for (int k = 0; k < K; k++) {
        	//nw 是第i个word被赋予第j个topic的个�?
        	//在下式中，documents[m][n]是word id，k为第k个topic
        	//nd 为第m个文档中被赋予topic k的词的个�?
            p[k] = (nw[documents[m][n]][k] + beta) / (nwsum[k] + V * beta)
                * (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
        }
        // cumulate multinomial parameters
        for (int k = 1; k < p.length; k++) {
            p[k] += p[k - 1];
        }
        // scaled sample because of unnormalised p[]
        double u = Math.random() * p[K - 1];
        for (topic = 0; topic < p.length; topic++) {
            if (u < p[topic])
                break;
        }

        // add newly estimated z_i to count variables
        nw[documents[m][n]][topic]++;
        nd[m][topic]++;
        nwsum[topic]++;
        ndsum[m]++;

        return topic;
    }

    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    private void updateParams() {
        for (int m = 0; m < documents.length; m++) {
            for (int k = 0; k < K; k++) {
                thetasum[m][k] += (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
            }
        }
        for (int k = 0; k < K; k++) {
            for (int w = 0; w < V; w++) {
                phisum[k][w] += (nw[w][k] + beta) / (nwsum[k] + V * beta);
            }
        }
        numstats++;
    }

    /**
     * Retrieve estimated document--topic associations. If sample lag > 0 then
     * the mean value of all sampled statistics for theta[][] is taken.
     * 
     * @return theta multinomial mixture of document topics (M x K)
     */
    public double[][] getTheta() {
        double[][] theta = new double[documents.length][K];

        if (SAMPLE_LAG > 0) {
            for (int m = 0; m < documents.length; m++) {
                for (int k = 0; k < K; k++) {
                    theta[m][k] = thetasum[m][k] / numstats;
                }
            }

        } else {
            for (int m = 0; m < documents.length; m++) {
                for (int k = 0; k < K; k++) {
                    theta[m][k] = (nd[m][k] + alpha) / (ndsum[m] + K * alpha);
                }
            }
        }

        return theta;
    }

    /**
     * Retrieve estimated topic--word associations. If sample lag > 0 then the
     * mean value of all sampled statistics for phi[][] is taken.
     * 
     * @return phi multinomial mixture of topic words (K x V)
     */
    public double[][] getPhi() {
        double[][] phi = new double[K][V];
        if (SAMPLE_LAG > 0) {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    phi[k][w] = phisum[k][w] / numstats;
                }
            }
        } else {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < V; w++) {
                    phi[k][w] = (nw[w][k] + beta) / (nwsum[k] + V * beta);
                }
            }
        }
        return phi;
    }

    /**
     * Configure the gibbs sampler
     * 
     * @param iterations
     *            number of total iterations
     * @param burnIn
     *            number of burn-in iterations
     * @param thinInterval
     *            update statistics interval
     * @param sampleLag
     *            sample interval (-1 for just one sample at the end)
     */
    public void configure(int iterations, int burnIn, int thinInterval,
        int sampleLag) {
        ITERATIONS = iterations;
        BURN_IN = burnIn;
        THIN_INTERVAL = thinInterval;
        SAMPLE_LAG = sampleLag;
    }
    
    //�ҳ���������ֵ
    public static int indexOfMax(double[] arr) {
    	int index = -1;
    	double max = -1.0;
    	for(int i = 0; i < arr.length; i++) {
    		if (max < arr[i]) {
    			max = arr[i];
    			index = i;
    		}
    	}
    	if (index > -1)
    		arr[index] = -1.0;
    	return index;
    }
    
    /**
     * Driver with example data.
     * 
     * @param args
     */
    public static void main(String[] args) {
    	String sourceFilename = "G:\\zjqTest\\text mining test\\svm\\status_wh_baoyu_remove_repetition.txt";
    	//File resultFile = new File("G:\\zjqTest\\text mining test\\svm\\status_wh_baoyu(20 topic_v1).txt");
    	FormatDocument ldaUtil = new FormatDocument(sourceFilename,"lda");
    	ldaUtil.preProcess();
        int[][] documents = ldaUtil.format();
        
       /* StringBuffer sb_doc_content = new StringBuffer();
        for (int i = 0; i < documents.length; i++) {
        	for (int j = 0; j < documents[i].length; j++) {
        		sb_doc_content.append(FormatDocument.dictKey(documents[i][j]));
        		sb_doc_content.append(" ");
        	}
        	sb_doc_content.append("\r\n");
        }
        RAndW.appendToFile(sb_doc_content.toString(), resultFile);*/
        //System.out.println(ldaUtil.getWordIndex().size());
        //words in documents
        /*int[][] documents = { {1, 4, 3, 2, 3, 1, 4, 3, 2, 3, 1, 4, 3, 2, 3, 6},
            {2, 2, 4, 2, 4, 2, 2, 2, 2, 4, 2, 2},
            {1, 6, 5, 6, 0, 1, 6, 5, 6, 0, 1, 6, 5, 6, 0, 0},
            {5, 6, 6, 2, 3, 3, 6, 5, 6, 2, 2, 6, 5, 6, 6, 6, 0},
            {2, 2, 4, 4, 4, 4, 1, 5, 5, 5, 5, 5, 5, 1, 1, 1, 1, 0},
            {5, 4, 2, 3, 4, 5, 6, 6, 5, 4, 3, 2}};
        int i = 0;
        for(String str : ldaUtil.indexTOWord()) {
        	appendToFile((i++) + "\t" + str+"\r\n");
        }
        System.out.println(i);*/
        // vocabulary
        int V = FormatDocument.dictSize();
        int M = documents.length;
        // # topics
        for (int K = 20; K < 21; K++) {
            // good values alpha = 2, beta = .5
            double alpha = 50.0/K;
            double beta = .1;

            LdaGibbsSampler lda = new LdaGibbsSampler(documents, V);
            
            //�趨sample��������������10000�֣�burn-in 2000�֣�����������û�ã���Ϊ����ʾ
            //���ĸ�������sample lag���������Ҫ����Ϊ����Ʒ���ǰ��״̬conditional dependent������Ҫ������������
            lda.configure(10000, 5000, 100, 10);//lda.configure(10000, 2000, 100, 10);
            
            lda.gibbs(K, alpha, beta);

            //���ģ�Ͳ�����������ʽ ��81���루82��
            double[][] theta = lda.getTheta();
            double[][] phi = lda.getPhi();
            
            File resultDTFile = new File("G:\\zjqTest\\text mining test\\svm\\status_wh_baoyu("+K+" topic)Document-Topic.txt");
            StringBuffer sb_dt_title = new StringBuffer();
            sb_dt_title.append("Document--Topic Associations, Theta[d][k] (alpha=" 
                + alpha + ")\r\n");  
            sb_dt_title.append("d\\k\t"); 
            for (int m = 0; m < theta[0].length; m++) {  
            	sb_dt_title.append(m + "\t");  
            }  
            sb_dt_title.append("\r\n");
            RAndW.appendToFile(sb_dt_title.toString(), resultDTFile);
            
            StringBuffer sb_dt_content = new StringBuffer();
            for (int m = 0; m < theta.length; m++) {  
            	 sb_dt_content.append(m + "\t");  
                for (int k = 0; k < theta[m].length; k++) {  
                	sb_dt_content.append(theta[m][k] + " ");   
                }  
                sb_dt_content.append("\r\n");  
            }  
            sb_dt_content.append("\r\n");
            RAndW.appendToFile(sb_dt_content.toString(), resultDTFile);
            
            File resultTTFile = new File("G:\\zjqTest\\text mining test\\svm\\status_wh_baoyu("+K+" topic)Topic--Term.txt");
            StringBuffer sb_tw_title = new StringBuffer();
            sb_tw_title.append("Topic--Term Associations, Phi[k][w] (beta=" + beta  
                + ")\r\n");  
     
            sb_tw_title.append("k\\w\t");  
            for (int w = 0; w < phi[0].length; w++) {  
            	sb_tw_title.append( w + "\t");  
            }  
            sb_tw_title.append("\r\n");
            RAndW.appendToFile(sb_tw_title.toString(), resultTTFile);
            
            StringBuffer sb_tw_content = new StringBuffer();
            for (int k = 0; k < phi.length; k++) {  
            	sb_tw_content.append("Topic #" + k + "\t");
                for (int w = 0; w < phi[k].length; w++) {
                	int index = indexOfMax(phi[k]);
                	//System.out.println(index);
                	String word = (index > -1) ? FormatDocument.dictKey(index):"**";
                	sb_tw_content.append(word + " ");
                }  
                sb_tw_content.append("\r\n");
            }
            RAndW.appendToFile(sb_tw_content.toString(), resultTTFile);
        }
        /*int K = 20;
        // good values alpha = 2, beta = .5
        double alpha = 50.0/K;
        double beta = .1;

        LdaGibbsSampler lda = new LdaGibbsSampler(documents, V);
        
        //�趨sample��������������10000�֣�burn-in 2000�֣�����������û�ã���Ϊ����ʾ
        //���ĸ�������sample lag���������Ҫ����Ϊ����Ʒ���ǰ��״̬conditional dependent������Ҫ������������
        lda.configure(10000, 5000, 100, 10);//lda.configure(10000, 2000, 100, 10);
        
        lda.gibbs(K, alpha, beta);

        //���ģ�Ͳ�����������ʽ ��81���루82��
        double[][] theta = lda.getTheta();
        double[][] phi = lda.getPhi();
        
        StringBuffer sb_dt_title = new StringBuffer();
        sb_dt_title.append("Document--Topic Associations, Theta[d][k] (alpha=" 
            + alpha + ")\r\n");  
        sb_dt_title.append("d\\k\t"); 
        for (int m = 0; m < theta[0].length; m++) {  
        	sb_dt_title.append(m + "\t");  
        }  
        sb_dt_title.append("\r\n");
        RAndW.appendToFile(sb_dt_title.toString(), resultFile);
        
        StringBuffer sb_dt_content = new StringBuffer();
        for (int m = 0; m < theta.length; m++) {  
        	 sb_dt_content.append(m + "\t");  
            for (int k = 0; k < theta[m].length; k++) {  
            	sb_dt_content.append(theta[m][k] + " ");   
            }  
            sb_dt_content.append("\r\n");  
        }  
        sb_dt_content.append("\r\n");
        RAndW.appendToFile(sb_dt_content.toString(), resultFile);
        
        StringBuffer sb_tw_title = new StringBuffer();
        sb_tw_title.append("Topic--Term Associations, Phi[k][w] (beta=" + beta  
            + ")\r\n");  
 
        sb_tw_title.append("k\\w\t");  
        for (int w = 0; w < phi[0].length; w++) {  
        	sb_tw_title.append( w + "\t");  
        }  
        sb_tw_title.append("\r\n");
        RAndW.appendToFile(sb_tw_title.toString(), resultFile);
        
        StringBuffer sb_tw_content = new StringBuffer();
        for (int k = 0; k < phi.length; k++) {  
        	sb_tw_content.append("Topic #" + k + "\t");
            for (int w = 0; w < phi[k].length; w++) {
            	int index = indexOfMax(phi[k]);
            	//System.out.println(index);
            	String word = (index > -1) ? FormatDocument.dictKey(index):"**";
            	sb_tw_content.append(word + " ");
            }  
            sb_tw_content.append("\r\n");
        }
        RAndW.appendToFile(sb_tw_content.toString(), resultFile);*/
    }
    /*public static void main(String[] args) {
    	double[] arr = {5, 4, 2, 9, 6, 3};
    	//System.out.println(indexOfMax(arr));
    	for (int i = 0; i<arr.length; i++) {
    		System.out.print(indexOfMax(arr)+" ");
    	}
    }*/
}