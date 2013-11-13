package era_misa.tm.util;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.ansj.domain.Term;
import org.ansj.library.UserDefineLibrary;
import org.ansj.recognition.NatureRecognition;
import org.ansj.splitWord.analysis.ToAnalysis;

public class FormatDocument {

	private String mlType;
	private String sourceFilename;
	private ArrayList<Document> docs;
	private String trainFilename;
	private static String[] reversedDict;
	
	private HashSet<String> natureNameSet;
	private HashSet<String> goOnWordSet;
	private HashSet<String> stopWordSet;
	private HashMap<String,Integer> myDict;
	
	public FormatDocument() {
		// TODO Auto-generated constructor stub
	}
	
	public FormatDocument(String sourceFilename, String mlType) {
		// TODO Auto-generated constructor stub
		this(sourceFilename, sourceFilename.substring(0, sourceFilename.lastIndexOf(".")).concat("_train"), mlType);
	}

	public FormatDocument(String sourceFilename, String trainFilename, String mlType) {
		this.mlType = mlType;
		this.sourceFilename = sourceFilename;
		docs = new ArrayList<Document>();
		String[] rawDocs = RAndW.readTXT(sourceFilename, "utf-8");
		initDocument(rawDocs);
		
		natureNameSet = new HashSet<String>();
		goOnWordSet = new HashSet<String>(); 
		stopWordSet = new HashSet<String>();
		myDict = new HashMap<String,Integer>();
		this.initSegment(natureNameSet, goOnWordSet, stopWordSet, myDict);
		
		this.trainFilename = trainFilename;
	}
	
	public ArrayList<Document> getDocs() {
		return docs;
	}

	public void setDocs(ArrayList<Document> docs) {
		this.docs = docs;
	}
	
	public String getTrainFilename() {
		return trainFilename;
	}

	public void setTrainFilename(String trainFilename) {
		this.trainFilename = trainFilename;
	}

	private void initDocument(String[] rawDocs) {
		if (mlType.equalsIgnoreCase("svm")){
			for (int i = 0; i < rawDocs.length; i++) {
				//System.out.println(rawDocs[i]);
				int topicType = Integer.parseInt(rawDocs[i]);
				Document temp = new Document(rawDocs[++i], topicType);
				docs.add(temp);
			}
		}
		if (mlType.equalsIgnoreCase("lda")){
			for (int i = 0; i < rawDocs.length; i++) {
				//System.out.println(rawDocs[i]);
				Document temp = new Document(rawDocs[i]);
				docs.add(temp);
			}
		}
		System.out.println("原始文档的总数为：" + docs.size());
	}
	
	//分词，并进一步初始化每个文档
	public void preProcess() {
		loadUserDefineDict();
		loadUserDefineDict(this.initEmotion());
		
		List<Document> removedDocument = new ArrayList<Document>();
		for (int i = 0; i < docs.size(); i ++) {
			Document doc = docs.get(i);
			 //普通分词
			List<Term> terms = ToAnalysis.parse(docs.get(i).getText());
	        // 词性标注
	        new NatureRecognition(terms).recognition();
	        
	        List<Term> removedTerm = new ArrayList<Term>();
	        for (int j = 0; j < terms.size(); j++) {
	        	Term term = terms.get(j) ;
                //词性
                String ns = term.getNatrue().natureStr;
                //词名
                String strName = term.getName();
                
                //通过词性，停用词对文档进行过滤
                if ((natureNameSet.contains(ns) || goOnWordSet.contains(strName)) && !stopWordSet.contains(strName)) {
                	Document.updateDict(strName);
                }
                else {
                	removedTerm.add(term);
                }
	        }
	        //in the 'for' loop, be careful to use remove() of List
	        terms.removeAll(removedTerm);
	        removedTerm = null;
	        doc.setTerms(terms);
	        if (doc.termSize() < 2) {
	        	removedDocument.add(doc);
	        }
        }
		//剔除单词个数少于2的文档
		docs.removeAll(removedDocument);
		//为文档编号
		for (int i = 0; i < docs.size(); i++) {
			docs.get(i).setDocNum(i);
		}
		//获取字典的反向查询表，即通过字典中单词的值查找单词
		reversedDict = Document.reverseDict();
		
		//统计信息和中间结果的输出
		System.out.println("预处理后文档总数为：" + docs.size());
		System.out.println("词典的单词总数为：" + Document.dictSize());
		StringBuffer cleanDoc = new StringBuffer();
		StringBuffer segmentOfDoc = new StringBuffer();
		for (int i = 0; i < docs.size(); i ++) {
			cleanDoc.append(docs.get(i).getText());
			cleanDoc.append("\r\n");
			List<Term> terms = docs.get(i).getTerms();
			for (int j = 0; j < terms.size(); j++) {
				segmentOfDoc.append(terms.get(j).getName() + "\\" + terms.get(j).getNatrue().natureStr);
				segmentOfDoc.append(" ");
			}
			segmentOfDoc.append("\r\n");
		}
		RAndW.appendToFile(cleanDoc.toString(), new File(sourceFilename.substring(0, sourceFilename.lastIndexOf(".")).concat("_cleanDoc.txt")));
		RAndW.appendToFile(segmentOfDoc.toString(), new File(sourceFilename.substring(0, sourceFilename.lastIndexOf(".")).concat("_segmentDoc.txt")));
	}
	
	/*public void formatForSVM() {
		//格式化输出
		StringBuffer trainSet = new StringBuffer();
		for (int i = 0; i < docs.size(); i ++) {
			trainSet.append(docs.get(i).getIndexWeight());
		}
		RAndW.appendToFile(trainSet.toString(), new File(trainFilename));
	}
	
	public int[][] formatForLDA() {
		int[][] document = new int[docs.size()][];
		for (int i = 0; i < docs.size(); i++) {
			Document doc = docs.get(i);
			document[i] = doc.docToIndex();
		}
        return document;
	}*/
	
	public int[][] format() {
		if (mlType.equalsIgnoreCase("svm")){
			StringBuffer trainSet = new StringBuffer();
			for (int i = 0; i < docs.size(); i ++) {
				trainSet.append(docs.get(i).getIndexWeight());
			}
			RAndW.appendToFile(trainSet.toString(), new File(trainFilename));
		}
		if (mlType.equalsIgnoreCase("lda")){
			int[][] document = new int[docs.size()][];
			for (int i = 0; i < docs.size(); i++) {
				Document doc = docs.get(i);
				document[i] = doc.docToIndex();
			}
	        return document;
		}
		return null;
	}
	
	public static String dictKey(int index) {
		return reversedDict[index];
	}
	
	public static int dictSize() {
		return reversedDict.length;
	}
	
	private void initSegment(HashSet<String> natureNameSet, HashSet<String>goOnWordSet, HashSet<String>stopWordSet, HashMap<String,Integer> dict) {
		Properties props = new Properties();
		try {
            props.load(new InputStreamReader(FormatDocument.class.getResourceAsStream("/segment.properties"), "UTF-8"));
        } catch (Exception e) {
        	e.printStackTrace();
        }
		String[] natrueNames = props.getProperty("filterNatrueName").split(",");
		for (String natrueName : natrueNames) {
			natureNameSet.add(natrueName);
			//System.out.println(natrueName);
		}
		
		String[] goOnWords = props.getProperty("goOnWord").split(",");
		for (String goOnWord : goOnWords) {
			goOnWordSet.add(goOnWord);
			//System.out.println(goOnWord);
		}
		
		String[] stopWords = this.initStopWord();
		for (String stopWord : stopWords) {
			//System.out.println(stopWord);
			stopWordSet.add(stopWord);
		}
		
		String[] dicts = this.initMyDict();
		for (String words : dicts) {
			//System.out.println(stopWord);
			if (words.contains(",")) {
				String[] wordFreq = words.split(",");
				myDict.put(wordFreq[0], Integer.parseInt(wordFreq[1], 10));
			}
			else {
				myDict.put(words, 100);
			}
		}
	}
	
	private String[] initMyDict() {
		String filename = null;
		try {
			//System.out.println(LDAUtil.class.getResource("/myDict.txt").toURI().getPath());
			filename = FormatDocument.class.getResource("/myDict.txt").toURI().getPath();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return RAndW.readTXT(filename, "utf-8");
	}

	private String[] initStopWord() {
		String filename = null;
		try {
			filename = FormatDocument.class.getResource("/stopwords.txt").toURI().getPath();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return RAndW.readTXT(filename,"gbk");
	}
	
	private String[] initEmotion() {
		String filename = null;
		try {
			//System.out.println(LDAUtil.class.getResource("/weiboEmotion.txt").toURI().getPath());
			filename = FormatDocument.class.getResource("/weiboEmotion.txt").toURI().getPath();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return RAndW.readTXT(filename, "utf-8");
	}
	
	private void loadUserDefineDict() {
		// TODO Auto-generated method stub
		if (myDict.size() > 0) {
			Iterator<Entry<String,Integer>> iter = myDict.entrySet().iterator(); 
            while (iter.hasNext()) { 
                Map.Entry entry = (Map.Entry) iter.next(); 
                String key = (String)(entry.getKey());
                int val = (Integer)entry.getValue();
                UserDefineLibrary.insertWord(key,"userDefine",val);
            }
		}
	}
	
	private void loadUserDefineDict(String[] dict) {
		// TODO Auto-generated method stub
		for (String word : dict) {
			UserDefineLibrary.insertWord(word,"userDefine",100);
			//System.out.println(word);
		}
	}
	
	/*public static void main(String[] args) {
		String sourcefilename = "G:\\zjqTest\\text mining test\\svm\\rawTrainTest.txt";
		FormatDocument fd = new FormatDocument(sourcefilename);
		fd.preProcess();
		fd.formatForSVM();
	}*/
}
