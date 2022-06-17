package killMutations.whereClauseNestedBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.RelatedToParameters;
import util.TagDatasets;

/**
 * This class generates data sets to kill non equi join class mutations in the Where clause nested subquery block
 * @author mahesh
 *
 */
public class JoinMutationsInWhereSubQuery {

	private static Logger logger = Logger.getLogger(JoinMutationsInWhereSubQuery.class.getName());
	/**
	 * Generates data to kill non equi-join  class mutations inside Where clause nested subquery block
	 * @param cvc
	 */
	public static void generateDataForkillingJoinMutationsInWhereSubquery(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		//HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/** we have to check if there are where clause subqueries in each conjunct of outer block of query */
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs()){

			/**For each where clause subquery blocks of this conjunct*/
			/** Kill non equi join  mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this subquery node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause subquery block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				/** Kill non equi join mutations in each conjunct of this where clause nested block of this query*/
				for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"NEW CONJUNCT IN EC KILLING IN WHERE CLAUSE SUBQUERY BLOCK: " + conjunct);
					logger.log(Level.INFO,"----------------------------------\n");
					
					
					/**Get the non equi-join conditions of this conjunct*/
					ArrayList<Node > allConds = new ArrayList(conjunct.getAllConds());

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING NON EQUI JOIN PREDICATES IN WHERE CLAUSE NESTED SUBQUERY BLOCK: " + allConds);
					logger.log(Level.INFO,"----------------------------------\n");
					
					
					/** Kill each non equi-join condition of this conjunct*/
					for(int i=0; i<allConds.size(); i++){

						Node pred = allConds.get(i);

						logger.log(Level.INFO,"\n----------------------------------");
						logger.log(Level.INFO,"KILLING NON EQUI JOIN PREDICATE: " + pred);
						logger.log(Level.INFO,"----------------------------------\n");
						
						
						/** Find the different relations involved in pred. Pred might be an arbitrary predicate */
						HashMap<String,Table> rels = UtilsRelatedToNode.getListOfRelationsFromNode(cvc, qbt, pred);

						Iterator rel = rels.keySet().iterator();
						while(rel.hasNext()){

							String CVCStr="";
							cvc.setConstraints( new ArrayList<String>());
							cvc.setStringConstraints( new ArrayList<String>());
							cvc.inititalizeForDatasetQs();
							cvc.setTypeOfMutation("");
							
							/**set the type of mutation we are trying to kill*/
							cvc.setTypeOfMutation( TagDatasets.MutationType.NONEQUIJOIN, TagDatasets.QueryBlock.WHERE_SUBQUERY );

							/** Assign the number of tuples and their positions */
							cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
							//							cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
							cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );

							/** Add constraints related to parameters*/
							cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt));

							/** Add constraints for all  From clause nested sub query blocks  */
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){								
								cvc.getConstraints().add(ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));

								cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );

								cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" END OF FROM CLAUSE SUBQUERY "));								
							}

							String aliasName = (String)rel.next();
							String tableName = rels.get(aliasName).getTableName();

							/** FIXME: This function is generating constraints of form ASSERT NOT EXISTS (i: O_SECTION_INDEX_INT): ((O_SECTION[1].0>O_TAKES[1].1));
							 * These are causing problem. Example query19*/
							/**FIXME: Also the repeated relations are not correctly handled in the below method */
							cvc.getConstraints().add( GenerateCVCConstraintForNode.genNegativeCondsForPredAgg(cvc, qbt, pred, aliasName, tableName) );							


							/** Add negative conditions for all other conjuncts of this sub query block */
							for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
								if(inner != conjunct)
									cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	

							/** get constraints for this conjunct of outer query block, except for where clause sub query block */
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptWhereClauseSubQueryBlock( cvc, cvc.getOuterBlock(), con) );

							/** Also Generates positive constraints for all the conditions of this sub query block conjunct*/
							/** And we need to add the positive conditions for all other where clause sub query blocks in this conjunct*/
							GenerateConstraintsForWhereClauseSubQueryBlock.generateConstraintsForKillingMutationsInWhereSubqueryBlock(cvc, qbt, con, conjunct, subQCond, 1);


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
				}
			}
		}


		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}

	
	
}
