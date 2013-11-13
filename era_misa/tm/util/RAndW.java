package era_misa.tm.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class RAndW {

	/*public ReadAndWrite() {
		// TODO Auto-generated constructor stub
	}*/
	
	public static String[] readTXT(String filename, String encoding) {
		File file = new File(filename);
		BufferedReader reader = null;
		ArrayList<String> doc = new ArrayList<String>();
		try {
		     if (encoding.equalsIgnoreCase("gbk"))
		    	 reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"gbk"));
		     else
		    	 reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
		     String tempString = null;
		     // 一次读入一行，直到读入null为文件结束
		     while ((tempString = reader.readLine()) != null && tempString != "") {
		    	 //System.out.println(tempString);
		    	 tempString = cleanStr(tempString);
		    	 doc.add(tempString);
		      }
		      reader.close();
		  } catch (IOException e) {
		      e.printStackTrace();
		  } finally {
		      if (reader != null) {
		          try {
		              reader.close();
		          } catch (IOException e1) {
		          }
		      }
		  }
		return doc.toArray(new String[doc.size()]);
	}
	
	private static String cleanStr(String rawStr) {
		while(true) {
			int start,end;
			if ((start = rawStr.indexOf("//@")) == -1)
				break;
			if ((end = rawStr.indexOf(":", start)) == -1)
				break;
			rawStr = rawStr.substring(0, start).concat(rawStr.substring(end+1, rawStr.length()));
		}
		
		while(true) {
			int start,end;
			if ((start = rawStr.indexOf("@")) == -1)
				break;
			if ((end = rawStr.indexOf(" ", start+1)) == -1 && (end = rawStr.indexOf(":", start+1)) == -1) {
				rawStr = rawStr.substring(0, start);
				break;
			}
			
			rawStr = rawStr.substring(0, start).concat(rawStr.substring(end+1, rawStr.length()));
		}
		
		while(rawStr.contains(" ")) {
			rawStr = rawStr.replaceAll(" ", "");
		}
		return rawStr.trim();
	}
	
	public static void appendToFile(String content, File file) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "utf-8"));
            writer.write(content);
        } catch(IOException ioe) {
        	ioe.printStackTrace();
        } finally {
        if (writer != null){
        	 try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	 writer = null;
        }
         
        }
    }
}
