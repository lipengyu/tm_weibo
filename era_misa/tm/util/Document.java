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
	
	private int docNum = -1;	//文档序号
	private int topicType = -1;	//文档类别，SVM用到
	private String text;	//文档内容
	//private HashMap<String, Integer> termWeight; //文档中单词对应权重，这里取词频，SVM
	private List<Term> terms;	//文档中单词序列
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
	
	/*//统计文档中各个单词的词频
	public void countTerm(String word) {
		if (termWeight.containsKey(word)) {
			termWeight.put(word, termWeight.get(word)+1);
		}
		else
			termWeight.put(word, 1);
	}*/
	
	//统计文档中各个单词的词频
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

	//将文档中的单词和对应的权重， 按单词序号转换成SVM要求格式的字符串
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
		  
        // 对HashMap中的key 进行排序  
        Collections.sort(infoIds, new Comparator<Map.Entry<Integer, Integer>>() {  
            public int compare(Map.Entry<Integer, Integer> o1,  
            		Map.Entry<Integer, Integer> o2) {   
            	return (o1.getKey() - o2.getKey());
            }  
        });  
        // 对HashMap中的key 进行排序后  显示排序结果
        StringBuffer sb = new StringBuffer(new Integer(getTopicType()).toString());
        sb.append(" ");
        for (int i = 0; i < infoIds.size(); i++) {   
            sb.append(infoIds.get(i).getKey() + ":" + infoIds.get(i).getValue());
            sb.append(" ");
        }
        sb.append("\r\n");
        return sb;
	}
	
	//将文档中的单词转换为数组，索引为单词的位置，值为字典中单词对应的值
	public int[] docToIndex() {
		int[] doc = new int[terms.size()];
		for (int i = 0; i < terms.size(); i++) {
			doc[i] = dictValue(terms.get(i).getName());
		}
        return doc;
	}
	
	//更新词典
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
	
	//取词典中单词对应的值
	public static int dictValue(String word) {
		return dict.get(word);
	}
	
	//将字典转换为字符串数组，索引为字典中词对应的值，值为单词
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
	
	/*//取词典中值对应的单词
	public static String dictKey(int value) {
		return reverseDict()[value];
	}*/
	
	/*//文档中单词的个数
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
	
	//文档中单词的个数
	public int termSize() {
		return terms.size();
	}
	
	//字典中单词的个数
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
