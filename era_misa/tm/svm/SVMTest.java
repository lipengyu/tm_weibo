package era_misa.tm.svm;

import java.io.IOException;

import era_misa.tm.util.FormatDocument;


public class SVMTest {

	public SVMTest() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String sourcefilename = "G:\\zjqTest\\text mining test\\svm\\rawTrainTest.txt";
		FormatDocument fd = new FormatDocument(sourcefilename,"svm");
		fd.preProcess();
		fd.format();
		
		String[] train_param = {"-v", "4","-c","4",fd.getTrainFilename(),"G:\\zjqTest\\text mining test\\svm\\heart_scale.model"};
		try {
			svm_train.main(train_param);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[]  predict_param = {fd.getTrainFilename(),"G:\\zjqTest\\text mining test\\svm\\heart_scale.model","G:\\zjqTest\\text mining test\\svm\\heart_output"};
		try {
			svm_predict.main(predict_param);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
