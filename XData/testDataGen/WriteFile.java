package testDataGen;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author shree
 *
 */
public class WriteFile {

	private static Logger logger = Logger.getLogger(WriteFile.class.getName());
	public static void writeFile(String filePath, String content){
		try(java.io.FileWriter fw=new java.io.FileWriter(filePath, false)){
			fw.write(content);
			fw.flush();
		}catch(Exception e){
			logger.log(Level.SEVERE, "Message", e);
			
		}
	}
	
	
}
