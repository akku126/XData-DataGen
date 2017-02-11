package testDataGen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.QueryParser;
import parsing.QueryStructure;

import util.Configuration;
import util.TableMap;

public class DataGenController{

	private static Logger logger=Logger.getLogger(DataGenController.class.getName());
	
	
	public static void generateDatasetForQuery(GenerateCVC1 cvc) throws Exception{

		logger.log(Level.INFO,"------------------------------------------------------------------------------------------\n\n");
		if(cvc.getConcatenatedQueryId() != null){
			logger.log(Level.INFO,"Data Generation process started for QueryID:  "+cvc.getConcatenatedQueryId());
		}else{
			logger.log(Level.INFO,"Data Generation process started for query.");
		}
		logger.log(Level.INFO,"------------------------------------------------------------------------------------------\n\n");
		try{
			/** delete previous data sets*/		
			//RelatedToPreprocessing.deletePreviousDatasets(cvc, cvc.getQueryString());
			RelatedToPreprocessing.deletePreviousDatasets(cvc, cvc.getQueryString());
			/**Call pre processing functions before calling data generation methods */
			preProcessingActivity(cvc,cvc.getTableMap());
			
			/** Check the data sets generated for this query */
			ArrayList<String> dataSets = RelatedToPreprocessing.getListOfDataset(cvc);
			
			logger.log(Level.INFO,"\n\n***********************************************************************\n");
			if(cvc.getConcatenatedQueryId() != null){
				logger.log(Level.INFO,"DATA SETS FOR QUERY "+cvc.getConcatenatedQueryId()+" ARE GENERATED");
			}else{
				logger.log(Level.INFO,"DATA SETS FOR QUERY ARE GENERATED");
			}
			logger.log(Level.INFO,"\n\n***********************************************************************\n");
			
		}finally{
			if(cvc != null && cvc.getConnection() != null){
			 cvc.closeConn();
			}
		}
	}

	
	public static void preProcessingActivity(GenerateCVC1 cvc, TableMap tableMap) throws Exception{
		

		/** check if there are branch queries and upload the details */
		//TODO: This is for application testing, a flag should be set
		//for calling this function
		RelatedToPreprocessing.uploadBranchQueriesDetails(cvc);
		
		
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
				/*cvc.setqParser( new QueryParser(tableMap));
				cvc.getqParser().parseQuery("q1", queryStr.toString());
				cvc.initializeQueryDetails(cvc.getqParser() );*/
				
				QueryStructure qStructure = new QueryStructure(tableMap);
				qStructure.buildQueryStructureJSQL(String.valueOf(cvc.getQueryId()), queryStr.toString(),false);
				cvc.setqStructure(qStructure);
				
				cvc.initializeQueryDetailsQStructure(qStructure);
				logger.log(Level.INFO,"File path = "+cvc.getFilePath());
				/*if(cvc.getqParser().setOperator!=null && cvc.getqParser().setOperator.length()>0){
					isSetOp = true;
					PreProcessingActivity.genDataForSetOp(cvc,cvc.getqParser().setOperator);
				}*/
				
				if(cvc.getqStructure().setOperator!=null && cvc.getqStructure().setOperator.length()>0){
					isSetOp = true;
					PreProcessingActivity.genDataForSetOp(cvc,cvc.getqStructure().setOperator);
				}
				
				else{

					cvc.getBranchQueries().intitializeDetails(cvc);

					/**Populate the values from the data base 
					 * Needed so that the generated values looks realistic */	
					RelatedToPreprocessing.populateData(cvc);

					/**Initialize cvc3 headers etc>,*/				
					cvc.initializeOtherDetails();

					/** Segregate selection conditions in each query block */
					RelatedToPreprocessing.segregateSelectionConditions(cvc);

					/** Call the method for generating the data sets */
					cvc.generateDatasetsToKillMutations();
				}
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,""+e.getStackTrace(),e);
			//e.printStackTrace();
			throw e;
		} 
		finally {
			if(input != null)
			input.close();
		}
	}
	
	
	
	
}
