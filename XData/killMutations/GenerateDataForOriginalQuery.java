package killMutations;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import generateConstraints.GenerateCommonConstraintsForQuery;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * This class generates data sets for the original query. This data set is intended tom give non empty result for the original query
 * @author mahesh
 *
 */
public class GenerateDataForOriginalQuery {

	private static Logger logger = Logger.getLogger(GenerateDataForOriginalQuery.class.getName());
	/**
	 * Generates data set for the original query
	 * @param cvc
	 */
	public static boolean generateDataForOriginalQuery(GenerateCVC1 cvc, String mutationType) throws Exception{

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR ORIGINAL QUERY: ");
		logger.log(Level.INFO,"---------------------------------\n");
		try{
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDataset();
		 
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation(mutationType);
	
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return false;
			
			
			//initializing once again after params are set up--- FIX once params are set properly
			cvc.inititalizeForDataset();
			
			/**Get the null and database constraints - get the number of outout tuples
			 * and generate other constraints  accordingly*/
			 GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc,false);
			 
			/**Get the constraints for all the blocks of the query -Shree commented */
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
}
