package killMutations.whereClauseNestedBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.UtilsRelatedToNode;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill having clause mutations in each Where clause nested subquery block
 * @author mahesh
 *
 */
public class ConstrainedAggregationMutationsInWhereSubQuery {

	private static Logger logger = Logger.getLogger(ConstrainedAggregationMutationsInWhereSubQuery.class.getName());
	/**
	 * Generates data to kill having clause mutations inside Where clause nested subquery block
	 * @param cvc
	 */
	public static void generateDataForkillingConstrainedAggregationInWhereSubquery(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		////HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();



		/** we have to check if there are where clause subqueries in each conjunct of outer block of query */
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs()){

			/**For each where clause subquery blocks of this conjunct*/
			/** Kill having clause  mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this subquery node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause subquery block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"GENERATE DATA FOR KILLING HAVING CLAUSE Mutations Inside WHERE clause subquery block: "+ qbt);
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				/**Get the having clause node of this subquery block*/
				Node havingClause = qbt.getHavingClause();

				if(havingClause == null)
					continue ;
				
				Node cloneHavingClause = havingClause.clone();
				ArrayList<Node> havingMutants = new ArrayList<Node>();

				if(havingClause !=null){/**If there is having clause*/

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING HAVING MUTATIONS IN WHERE CLAUSE SUBQUERY BLOCK" + havingClause);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					/**Get different mutations for this having clause*/
					havingMutants = UtilsRelatedToNode.getHavingMutations(cloneHavingClause);

					/**Kill each mutation*/
					for(int k=0;k<havingMutants.size();k++){

						logger.log(Level.INFO,"\n----------------------------------");
						logger.log(Level.INFO,"KILLING: "+havingMutants.get(k));
						logger.log(Level.INFO,"\n----------------------------------\n");
						
						
						/** generate data sets for this mutation, iff it is not same as original having clause */
						/** To propagate this change to outer query block, we should generate negative constraints for all other conjuncts of the outer query block*/
						if( !(havingMutants.get(k).equals(cloneHavingClause)) ){/**FIXME: Not working due to change in value while creating mutations*/

							/** Replace original having clause mutation with this mutation*/
							qbt.setHavingClause( havingMutants.get(k) );

							/** Initialize the data structures for generating the data to kill this mutation */
							cvc.inititalizeForDatasetQs();
							
							/**set the type of mutation we are trying to kill*/
							cvc.setTypeOfMutation( TagDatasets.MutationType.HAVING, TagDatasets.QueryBlock.WHERE_SUBQUERY );
							
							/** get the tuple assignment for this query
							 * If no possible assignment then not possible to kill this mutation*/
							if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
								continue;
							
							/** Add constraints for all the From clause nested sub query blocks */
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){								
									cvc.getConstraints().add( ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));
									
									cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
									
									cvc.getConstraints().add( ConstraintGenerator.addCommentLine(" END OF FROM CLAUSE SUBQUERY "));								
							}
							
							/** get constraints for this conjunct of outer query block, This also adds constraints for group by and having clause of this where clause sub query */
							/** Generate positive constraints for all the conditions of this sub query block conjunct */
							/**Here the mutated condition is also included in the list of the conditions of the sub query block*/
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, cvc.getOuterBlock(), con) );
							
							/**add the negative constraints for all the other conjuncts of outer query block */
							for(ConjunctQueryStructure outer: cvc.getOuterBlock().getConjunctsQs())
								if( !outer.equals(con))
									cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, cvc.getOuterBlock(), outer) );
								
							/** add group by and having clause constraints for outer query block */
							cvc.getConstraints().add( QueryBlockDetails.getGroupByAndHavingClauseConstraints(cvc, cvc.getOuterBlock())) ;
							
							/**Add other related constraints for the outer query block */
							cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock())) ;
							
							/** Call the method for the data generation*/
							GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
						}
					}
					/**Revert back to the old assignment*/
					qbt.setHavingClause( cloneHavingClause );
				}
			}
		}

		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}

}
