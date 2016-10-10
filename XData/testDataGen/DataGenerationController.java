package testDataGen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.QueryParser;
import util.Configuration;
import util.TableMap;

public class DataGenerationController {

	private static Logger logger = Logger.getLogger(DataGenerationController.class.getName());
	
	public static void preProcessingActivity(GenerateCVC1 cvc, TableMap tableMap) throws Exception{
		
		/** To store input query string */
		cvc.setTableMap(tableMap);
		String queryString = "";
		boolean isSetOp = false;
		BufferedReader input = null;
		StringBuffer queryStr = new StringBuffer();
		try {
			input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() + "/queries.txt"));
			/**Read the input query */
			while (( queryString = input.readLine()) != null){
				queryStr.append(queryString);
			}
			if(queryStr != null){
				/**Create a new query parser*/
				cvc.setqParser( new QueryParser(tableMap));

				/** Parse the query */
				cvc.getqParser().parseQuery("q1", queryStr.toString());
				
				/**Initialize the query details to the object*/
				cvc.initializeQueryDetails(cvc.getqParser() );
				logger.log(Level.INFO,"File path = "+cvc.getFilePath());
				if(cvc.getqParser().setOperator!=null && cvc.getqParser().setOperator.length()>0){
					isSetOp = true;
					//genDataForSetOp(cvc,cvc.getqParser().setOperator);
				}
				
			}
		}//try ends
		catch(Exception e){
			logger.log(Level.SEVERE,""+e.getStackTrace(),e);
			throw e;
		} 
		finally {
			if(input != null)
			input.close();
		}

	}
}
