package killMutations.outerQueryBlock;

import generateConstraints.UtilsRelatedToNode;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.GenerateDataForOriginalQuery;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill having clause mutations in each outer query block
 * @author mahesh
 *
 */
public class ConstrainedAggregationMutationsInOuterQueryBlock {

	private static Logger logger = Logger.getLogger(ConstrainedAggregationMutationsInOuterQueryBlock.class.getName());
	
	public static void generateDataForkillingConstrainedAggregationInOuterQueryBlockGen(GenerateCVC1 cvc) throws Exception{
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingConstrainedAggregationInOuterQueryBlock(cvc);
		}
		else{
			//generateDataForkillingConstrainedAggregationInOuterQueryBlockSMT(cvc);
		}
	}
	
	/**
	 * Generates data to kill having clause mutations inside outer query block
	 * @param cvc
	 */
	public static void generateDataForkillingConstrainedAggregationInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		
		/**Get outer query block */
		QueryBlockDetails outer = cvc.getOuterBlock();
		
		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING HAVING MUTATIONS IN OUTER BLOCK OF QUERY: " + outer);
		logger.log(Level.INFO,"----------------------------------\n");
		
		try{
			/**Get the having clause node of this query block*/
			Node havingClause = outer.getHavingClause();
			
			if(havingClause == null)
				return ;
			
			Node cloneHavingClause = havingClause.clone();
			ArrayList<Node> havingMutants = new ArrayList<Node>();
			
			if(havingClause !=null && havingClause.getType()!=null){/**If there is having clause*/
				
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"KILLING HAVING MUTATIONS IN OUTER BLOCK OF QUERY: " + havingClause);
				logger.log(Level.INFO,"----------------------------------\n");
				
				/**Get different mutations for this having clause*/
				havingMutants = UtilsRelatedToNode.getHavingMutations(cloneHavingClause);
				
				/**Kill each mutation*/
				for(int k=0;k<havingMutants.size();k++){
					
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING: "+havingMutants.get(k));
					logger.log(Level.INFO,"----------------------------------\n");
					
					/** generate data sets for each mutation */
					if( !(havingMutants.get(k).equals(cloneHavingClause)) ){
						
						
						
						/** Replace original having clause mutation with this mutation*/
						outer.setHavingClause( havingMutants.get(k) );
						
						String mutationType = TagDatasets.MutationType.HAVING.getMutationType() + " "+TagDatasets.QueryBlock.OUTER_BLOCK.getQueryBlock();
						
						/**Use the original method */
						GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvc, mutationType);
					}
				}
				
				/**Revert back to the old assignment*/
				outer.setHavingClause( cloneHavingClause );
			}
			
			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		//	cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
			cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
		}
	}

}
