package era_misa.tm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ansj.domain.Term;

public class Document {

	private static HashMap<String, Integer> dict = null;
	
	private int docNum = -1;	//�ĵ����
	private int topicType = -1;	//�ĵ����SVM�õ�
	private String text;	//�ĵ�����
	//private HashMap<String, Integer> termWeight; //�ĵ��е��ʶ�ӦȨ�أ�����ȡ��Ƶ��SVM
	private List<Term> terms;	//�ĵ��е�������
	//private HashMap<Integer, Integer> indexWeight;
	
	/*public Document() {
		// TODO Auto-generated constructor stub
	}*/
	
	public Document(String text) {
		// TODO Auto-generated constructor stub
		this(text, -1);
	}
	
	public Document(String text, int topicType) {
		// TODO Auto-generated constructor stub
		this(text, topicType, -1);
	}
	
	public Document(String text, int topicType, int docNum) {
		if (dict == null) {
			dict = new HashMap<String, Integer>();
		}
		this.text = text;
		this.topicType = topicType;
		this.docNum = docNum;
		//termWeight = new HashMap<String, Integer>();
		terms = new ArrayList<Term>();
	}
	
	/*//ͳ���ĵ��и������ʵĴ�Ƶ
	public void countTerm(String word) {
		if (termWeight.containsKey(word)) {
			termWeight.put(word, termWeight.get(word)+1);
		}
		else
			termWeight.put(word, 1);
	}*/
	
	//ͳ���ĵ��и������ʵĴ�Ƶ
	public HashMap<String, Integer> calTermWeight() {
		HashMap<String, Integer> termWeight = new HashMap<String, Integer>();
		for (int i = 0; i < terms.size(); i++) {
			String word = terms.get(i).getName();
			if (termWeight.containsKey(word)) {
				termWeight.put(word, termWeight.get(word)+1);
			}
			else
				termWeight.put(word, 1);
		}
		return termWeight;
	}

	//���ĵ��еĵ��ʺͶ�Ӧ��Ȩ�أ� ���������ת����SVMҪ���ʽ���ַ���
	public StringBuffer getIndexWeight() {
		//HashMap<Integer, Integer> indexWeight = new HashMap<Integer, Integer>((int)(termWeight.size()/0.8));
		HashMap<String, Integer> termWeight = calTermWeight();
		HashMap<Integer, Integer> indexWeight = new HashMap<Integer, Integer>((int)(termWeight.size()/0.8));
		Iterator<Entry<String,Integer>> iter = termWeight.entrySet().iterator();
		while (iter.hasNext()) { 
	        Map.Entry entry = (Map.Entry) iter.next(); 
	        String key = (String)entry.getKey();
	        int val = (Integer)(entry.getValue());
	        indexWeight.put(dict.get(key), val);
	    }
		
		List<Map.Entry<Integer, Integer>> infoIds = new ArrayList<Map.Entry<Integer, Integer>>(indexWeight.entrySet());  
		  
        // ��HashMap�е�key ��������  
        Collections.sort(infoIds, new Comparator<Map.Entry<Integer, Integer>>() {  
            public int compare(Map.Entry<Integer, Integer> o1,  
            		Map.Entry<Integer, Integer> o2) {   
            	return (o1.getKey() - o2.getKey());
            }  
        });  
        // ��HashMap�е�key ���������  ��ʾ������
        StringBuffer sb = new StringBuffer(new Integer(getTopicType()).toString());
        sb.append(" ");
        for (int i = 0; i < infoIds.size(); i++) {   
            sb.append(infoIds.get(i).getKey() + ":" + infoIds.get(i).getValue());
            sb.append(" ");
        }
        sb.append("\r\n");
        return sb;
	}
	
	//���ĵ��еĵ���ת��Ϊ���飬����Ϊ���ʵ�λ�ã�ֵΪ�ֵ��е��ʶ�Ӧ��ֵ
	public int[] docToIndex() {
		int[] doc = new int[terms.size()];
		for (int i = 0; i < terms.size(); i++) {
			doc[i] = dictValue(terms.get(i).getName());
		}
        return doc;
	}
	
	//���´ʵ�
	public static void updateDict(String word) {
		if (dict == null) {
			dict = new HashMap<String, Integer>();
		}
		if (dict.containsKey(word)) {
			return;
		}
		else {
			dict.put(word, dict.size());
		}
	}
	
	//ȡ�ʵ��е��ʶ�Ӧ��ֵ
	public static int dictValue(String word) {
		return dict.get(word);
	}
	
	//���ֵ�ת��Ϊ�ַ������飬����Ϊ�ֵ��дʶ�Ӧ��ֵ��ֵΪ����
	public static String[] reverseDict() {
		String[] indexWord = new String[dict.size()];
		Iterator<Entry<String,Integer>> iter = dict.entrySet().iterator();
		while (iter.hasNext()) { 
            Map.Entry entry = (Map.Entry) iter.next(); 
            int val = (Integer)(entry.getValue());
            String key = (String)entry.getKey();
            indexWord[val] = key;
        }
		return indexWord;
	}
	
	/*//ȡ�ʵ���ֵ��Ӧ�ĵ���
	public static String dictKey(int value) {
		return reverseDict()[value];
	}*/
	
	/*//�ĵ��е��ʵĸ���
	public int termSize() {
		int size = 0;
		Iterator<Entry<String,Integer>> iter = termWeight.entrySet().iterator();
		while (iter.hasNext()) { 
	        Map.Entry entry = (Map.Entry) iter.next(); 
	        int val = (Integer)(entry.getValue());
	        size += val;
	    }
		return size;
	}*/
	
	//�ĵ��е��ʵĸ���
	public int termSize() {
		return terms.size();
	}
	
	//�ֵ��е��ʵĸ���
	public static int dictSize() {
		return dict.size();
	}
	
	public static HashMap<String, Integer> getDict() {
		return dict;
	}

	public static void setDict(HashMap<String, Integer> dict) {
		Document.dict = dict;
	}

	public int getDocNum() {
		return docNum;
	}

	public void setDocNum(int docNum) {
		this.docNum = docNum;
	}

	public int getTopicType() {
		return topicType;
	}

	public void setTopicType(int topicType) {
		this.topicType = topicType;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public List<Term> getTerms() {
		return terms;
	}

	public void setTerms(List<Term> terms) {
		this.terms = terms;
	}
	
	/*public static void main(String[] args) {
		Document d = new Document("", 1, 1);
		d.getIndexWeight();
		System.out.println(d.size());
	}*/
}
