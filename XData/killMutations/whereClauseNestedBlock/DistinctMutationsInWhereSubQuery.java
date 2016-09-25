package killMutations.whereClauseNestedBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsToKillDistinctMutations;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Conjunct;
import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;
import util.Utilities;

/**
 * This class generates data sets to kill distinct clause mutations in each where clause nested subquery block
 * @author mahesh
 *
 */
public class DistinctMutationsInWhereSubQuery {

	private static Logger logger = Logger.getLogger(DistinctMutationsInWhereSubQuery.class.getName());
	/**
	 * Generates data to kill distinct clause mutations inside from clause nested subquery block
	 * @param cvc
	 */

	public static void generateDataForkillingDistinctMutationsInWhereSubquery(	GenerateCVC1 cvc) {

		try{
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/** we have to check if there are where clause subqueries in each conjunct of outer block of query */
		for(Conjunct con: cvc.getOuterBlock().getConjuncts()){

			/**For each where clause subquery blocks of this conjunct*/
			/** Kill distinct clause mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this subquery node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause subquery block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"GENERATE DATA FOR KILLING DISTINCT CLAUSE MUTATIONS IN WHERE CLAUSE NESTED SUBQUERY BLOCK: "+ qbt);
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				
				/** If there is an unconstrained aggregation there is no need to generate distinct tuples- 
				 * Mutation like SUM Vs SUM(DISTINCT) are part of aggregation mutations */
				if(qbt.getAggFunc() != null && qbt.getAggFunc().size() != 0)
					continue ;

				
				/** Initialize the data structures for generating the data to kill this mutation */
				cvc.inititalizeForDataset();


				/**set the type of mutation we are trying to kill*/
				cvc.setTypeOfMutation( TagDatasets.MutationType.DISTINCT, TagDatasets.QueryBlock.WHERE_SUBQUERY );
				
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
				
				/** get table name */
				String tableNameNo = qbt.getProjectedCols().get(0).getTableNameNo();
				
				/**assign the number of tuples for the this where clause subquery*/
				if( QueryBlockDetails.getTupleAssignment( cvc, qbt, tableNameNo) == false)
					continue ;
				
				/**get the tuple assignment for all other query blocks*/
				if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvc, qbt) == false)
					continue;
				
				/** get constraints for this conjunct of outer query block, This also adds constraints for group by and having clause of this where clause subquery */
				cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, cvc.getOuterBlock(), con) );
				
				/**add the negative constraints for all the other conjuncts of outer query block */
				for(Conjunct outer: cvc.getOuterBlock().getConjuncts())
					if( !outer.equals(con))
						cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, cvc.getOuterBlock(), outer) );
					
				/** add group by and having clause constraints for outer query block */
				cvc.getConstraints().add( QueryBlockDetails.getGroupByAndHavingClauseConstraints(cvc, cvc.getOuterBlock())) ;
				
				/**Add other related constraints for the outer query block */
				cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock())) ;
				
				
				
				cvc.getConstraints().add("\n%---------------------------------\n% DISTINCT CONSTRAINTS FOR WHERE CLAUSE SUBQUERY\n%---------------------------------\n");
				cvc.getConstraints().add( (GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(cvc, qbt)) );
				cvc.getConstraints().add("\n%---------------------------------\n% END OF DISTINCT CONSTRAINTS FOR WHERE CLAUSE SUBQUERY\n%---------------------------------\n");

				/** Call the method for the data generation*/
				GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);

				/**Reset the variable*/
				qbt.setConstrainedAggregation(false);
			}
		}



		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	
		} catch(Exception e){
			logger.log(Level.WARNING, "Failed to generate dataset for killing distinct mutations in where clause subquery:"+e.getMessage());
		}
	
	}

}
