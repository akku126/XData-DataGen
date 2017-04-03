package killMutations;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GetCVC3HeaderAndFooter;
import generateSMTConstraints.GenerateCommonConstraintsForQuerySMT;
import generateSMTConstraints.GetSMTHeaderAndFooter;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * This class generates data sets for the original query. This data set is intended tom give non empty result for the original query
 * @author mahesh
 *
 */
public class GenerateDataForOriginalQuery {

	private static Logger logger = Logger.getLogger(GenerateDataForOriginalQuery.class.getName());
	
	
	public static boolean generateDataForOriginalQuery(GenerateCVC1 cvc, String mutationType) throws Exception{
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			return generateDataForOriginalQueryCVC(cvc, mutationType);
		}else{
			return generateDataForOriginalQueryUsingSMT(cvc, mutationType);
		}
	}
	
	/**
	 * Generates data set for the original query
	 * @param cvc
	 */
	public static boolean generateDataForOriginalQueryCVC(GenerateCVC1 cvc, String mutationType) throws Exception{

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR ORIGINAL QUERY: ");
		logger.log(Level.INFO,"---------------------------------\n");
		try{
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();
		 
			
	
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return false;
			
			
			//initializing once again after params are set up--- FIX once params are set properly
		//	cvc.inititalizeForDataset();
			
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation(mutationType);
			
			/**Get the null and database constraints - get the number of outout tuples
			 * and generate other constraints  accordingly*/
			 GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc,false);
			 
			/**Get the constraints for all the blocks of the query  */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );
			
			/** Call the method for the data generation*/
			return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc,false);
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}
	
	
	/**
	 * Generates data set for the original query using SMT LIB constraints
	 * @param cvc
	 */
	public static boolean generateDataForOriginalQueryUsingSMT(GenerateCVC1 cvc, String mutationType) throws Exception{

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR ORIGINAL QUERY: ");
		logger.log(Level.INFO,"---------------------------------\n");
		try{
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();
		 
			
	
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return false;
			
			
			//initializing once again after params are set up--- FIX once params are set properly
		//	cvc.inititalizeForDataset();
			
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation(mutationType);
			
			/**Get the null and database constraints - get the number of outout tuples
			 * and generate other constraints  accordingly*/
			 GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc,false);
			 
			/**Get the constraints for all the blocks of the query  */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlockSMT(cvc));
			cvc.setDatatypeColumns( new ArrayList<String>() );
			
			System.out.println("\n cvc.getConstraints () : \n");
			System.out.println(cvc.getConstraints());
			System.out.println("************************************************************************************************************************");
			//System.out.println("\n CVSTR : \n"+CVCStr);
			/** Call the method for the data generation*/
			return GenerateCommonConstraintsForQuerySMT.generateDataSetForConstraintsForSMT(cvc,false);
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}
}
