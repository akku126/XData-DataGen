package killMutations.whereClauseNestedBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.UtilsRelatedToNode;

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill selection conditions (of the form A.x relOP const) class mutations in the Where clause nested subquery block
 * @author mahesh
 *
 */
public class SelectionMutationsInWhereSubquery {

	private static Logger logger = Logger.getLogger(SelectionMutationsInWhereSubquery.class.getName());
	/**
	 * Generates data to kill selection conditions mutations inside Where clause nested subquery block
	 * @param cvc
	 */
	public static void generateDataForkillingSelectionMutationsInWhereSubquery(	GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/** we have to check if there are where clause subqueries in each conjunct of outer block of query */
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs()){

			/**For each where clause subquery blocks of this conjunct*/
			/** Kill selection clause  mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this subquery node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause subquery block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				/** Kill selection clause mutations in each conjunct of this where clause nested block of this query*/
				for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"NEW CONJUNCT IN SELCTION CLAUSE MUTATIONS KILLING: " + conjunct);
					logger.log(Level.INFO,"\n---------------------------------");
					
					/**Get the selection conditions of this conjunct*/
					Vector<Node > selectionConds = conjunct.getSelectionConds();

					/** Kill each selection condition of this conjunct*/
					for(int i=0; i < selectionConds.size(); i++){

						logger.log(Level.INFO,"\n----------------------------------");
						logger.log(Level.INFO,"\n\nGETTING SELECTION MUTANTS\n");
						logger.log(Level.INFO,"\n---------------------------------");
						
						Node sc = selectionConds.get(i);

						Vector<Node> scMutants = UtilsRelatedToNode.getSelectionCondMutations(sc);
						

						/** Generate data set to kill each mutation*/
						for(int j=0; j<scMutants.size(); j++){
							
							/**If this mutation is not same as that of original condition*/
							if(!( scMutants.get(j).getOperator().equalsIgnoreCase(sc.getOperator())) ){
								
								logger.log(Level.INFO,"\n----------------------------------");
								logger.log(Level.INFO,"KILLING : " + scMutants.get(j));
								logger.log(Level.INFO,"---------------------------------\n");
								
								/** This is required so that the tuple assignment for the subquery is fine*/
								selectionConds.set(i,scMutants.get(j) );	

								/** Initialize the data structures for generating the data to kill this mutation */
								cvc.inititalizeForDatasetQs();

								/**set the type of mutation we are trying to kill*/
								cvc.setTypeOfMutation( TagDatasets.MutationType.SELCTION, TagDatasets.QueryBlock.WHERE_SUBQUERY );
								
								/** get the tuple assignment for this query
								 * If no possible assignment then not possible to kill this mutation*/
								if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
									continue;


								/** Add constraints for all the From clause nested subquery blocks except this sub query block */
								for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){								
									cvc.getConstraints().add(ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));

									cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );

									cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF FROM CLAUSE SUBQUERY "));								
								}

								
								/** Add negative conditions for all other conjuncts of this subquery block*/
								for(ConjunctQueryStructure inner: qbt.getConjunctsQs()){
									if(inner != conjunct){
										cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
									}
								}

								/** get constraints for this conjunct of outer query block, except for where clause sub query block */
								cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptWhereClauseSubQueryBlock( cvc, cvc.getOuterBlock(), con) );
								
								/** Also Generates positive constraints for all the conditions of this sub query block conjunct*/
								/** And we need to add the positive conditions for all other where clause sub query blocks in this conjunct*/
								GenerateConstraintsForWhereClauseSubQueryBlock.generateConstraintsForKillingMutationsInWhereSubqueryBlock(cvc, qbt, con, conjunct, subQCond, 0);

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
						/**Revert the change in selection conditions list of this subquery block */
						selectionConds.set(i,sc);
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
