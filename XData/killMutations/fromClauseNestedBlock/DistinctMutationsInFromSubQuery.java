package killMutations.fromClauseNestedBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsToKillDistinctMutations;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;
import util.Utilities;

/**
 * This class generates data sets to kill distinct clause mutations in each from clause nested subquery block
 * @author mahesh
 *
 */
public class DistinctMutationsInFromSubQuery {
	
	private static Logger logger = Logger.getLogger(DistinctMutationsInFromSubQuery.class.getName());
	/**
	 * Generates data to kill distinct clause mutations inside from clause nested subquery block
	 * @param cvc
	 */
	public static void generateDataForkillingDistinctMutationsInFromSubquery(GenerateCVC1 cvc) throws Exception{
	
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		
		/** Kill distinct clause mutations in each from clause nested block of this query*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries()){
			
			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"GENERATE DATA FOR KILLING DISTINCT CLAUSE MUTATIONS IN FROM CLAUSE NESTED SUBQUERY BLOCK: "+ qbt);
			logger.log(Level.INFO,"\n----------------------------------");
			/** If there is an unconstrained aggregation there is no need to generate distinct tuples- 
			 * Mutation like SUM Vs SUM(DISTINCT) are part of aggregation mutations */
			if(qbt.getAggFunc() != null && qbt.getAggFunc().size() != 0)
				continue ;
			
			/**FIXME: If any sub set of the projected columns of this query block form group by attributes for any from clause sub query block (linked to this query block) 
			 * then it is not possible to kill this mutation, because the sub query will output only one tuple
			 * But it may be possible to kill if there are joins in this query block involved with the from clause nested sub query block */
			/*for(GenerateCVC1 cvc1: cvc.FromClauseSubQueries)
				if(cvc.projectedCols.containsAll(cvc1.groupByNodes))
					return;*/
			
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();

			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.DISTINCT, TagDatasets.QueryBlock.FROM_SUBQUERY );
			
			/**The projected columns become single valued */
			qbt.setSingleValuedAttributesAdd( new HashSet<Node>(qbt.getProjectedCols()) );
			
			
			/** Initialize aggregation constraints data structure*/
			qbt.setAggConstraints( new ArrayList<Node>());
			
			/**get aggregation in this query block*/
			Vector<Node> aggConstraints = new Vector<Node>();
			if(qbt.getHavingClause() != null)
				Utilities.flattenConstraints( aggConstraints, qbt.getHavingClause());
			
			/**Update query block aggregation list*/
			qbt.setAggConstraints( new ArrayList<Node>(aggConstraints));
	
			
			/**get the count needed*/
			if(CountEstimationRelated.getCountNeededToKillDistinctMutation(cvc, qbt) == false)
				continue;				
			if(qbt.getProjectedCols() != null && qbt.getProjectedCols().size() > 0){
			/** get table name */
			String tableNameNo = qbt.getProjectedCols().get(0).getTableNameNo();
			
			/**assign the number of tuples for the this from clause subquery*/
			if( QueryBlockDetails.getTupleAssignment( cvc, qbt, tableNameNo) == false)
				continue ;
			
			/**get the tuple assignment for all other query blocks*/
			if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvc, qbt) == false)
				continue;
			
			
			/** Add constraints for all the blocks of the query */
			QueryBlockDetails.getConstraintsForQueryBlock(cvc);

			cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" AGGREGATION CONSTRAINTS FOR FROM CLAUSE SUBQUERY "));
			cvc.getConstraints().add( (GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(cvc, qbt)) );
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" END OF AGGREGATION CONSTRAINTS FOR FROM CLAUSE SUBQUERY "));

			/** Call the method for the data generation*/
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);	

			/**Reset the variable*/
			qbt.setConstrainedAggregation(false);
			}
		}
		
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
	//	cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		
	}

}
