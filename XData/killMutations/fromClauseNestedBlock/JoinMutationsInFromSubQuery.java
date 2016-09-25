package killMutations.fromClauseNestedBlock;

import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Conjunct;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.RelatedToParameters;
import util.TagDatasets;

/**
 * This class generates data sets to kill non equi join class mutations in the from clause nested subquery block
 * @author mahesh
 *
 */
public class JoinMutationsInFromSubQuery {

	private static Logger logger= Logger.getLogger(JoinMutationsInFromSubQuery.class.getName());
	/**
	 * Generates data to kill non equi-join  class mutations inside from clause nested subquery block
	 * @param cvc
	 */
	public static void generateDataForkillingJoinMutationsInFromSubquery(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		
		/** Kill non equi join class mutations in each from clause nested block of this query*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries()){
			
			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"GENERATE DATA FOR Killing non-equi join clause Mutations Inside From clause subquery block: "+ qbt);
			logger.log(Level.INFO,"\n----------------------------------");
			
			/**Kill the non equi-join clause mutations in each conjunct of this sub query query block*/
			for(Conjunct conjunct: qbt.getConjuncts()){
				
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"NEW CONJUNCT IN NEC KILLING: " + conjunct);
				logger.log(Level.INFO,"\n----------------------------------");
				
				/**Get the non equi-join conditions of this conjunct*/
				Vector<Node > allConds = conjunct.getAllConds();
				
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"KILLING NON EQUI JOIN PREDICATES in FROM CLAUSE NESTED SUBQUERY BLOCK: " + allConds);
				logger.log(Level.INFO,"\n----------------------------------");
				
				/** Kill each non equi-join condition of this conjunct*/
				for(int i=0; i<allConds.size(); i++){
					
					
					Node pred = allConds.get(i);
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING NON EQUI JOIN PREDICATE: " + pred);
					logger.log(Level.INFO,"\n----------------------------------");
					
					/** Find the different relations involved in pred. Pred might be an arbitrary predicate */
					HashMap<String,Table> rels = UtilsRelatedToNode.getListOfRelationsFromNode(cvc, qbt, pred);
					
					Iterator rel = rels.keySet().iterator();
					while(rel.hasNext()){
						
						String CVCStr="";
						cvc.setConstraints( new ArrayList<String>());
						cvc.setStringConstraints( new ArrayList<String>());
						cvc.setTypeOfMutation("");

						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.NONEQUIJOIN, TagDatasets.QueryBlock.FROM_SUBQUERY );
						
						
						/** Assign the number of tuples and their positions */
						cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
						cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
						cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
						
						/** Add constraints related to parameters*/
						cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters(cvc, qbt));
						
						/** Add constraints for all the From clause nested subquery blocks except this sub query block */
						for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
							if(!(qb.equals(qbt))){
								cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");
								
								cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
								
								cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");
							}
						}
						
						String aliasName = (String)rel.next();
						String tableName = rels.get(aliasName).getTableName();
						
						/** FIXME: This function is generating constraints of form ASSERT NOT EXISTS (i: O_SECTION_INDEX_INT): ((O_SECTION[1].0>O_TAKES[1].1));
						* These are causing problem. Example query19*/
						/**FIXME: Also the repeated relations are not correctly handled in the below method */
						cvc.getConstraints().add( GenerateCVCConstraintForNode.genNegativeCondsForPredAgg( cvc,qbt, pred, aliasName, tableName) );
						
						/** get positive constraints for all conditions except all conditions of the conjunct */
						cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptNonEquiJoins(cvc, qbt, conjunct) );
						
						
						/** Add negative conditions for all other conjuncts of this subquery block */
						for(Conjunct inner: qbt.getConjuncts())
							if(inner != conjunct)
								cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );							
						
						
						/** get group by constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS FOR SUBQUERY BLOCK\n%---------------------------------\n");
						cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
						
						
						/** Generate havingClause constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS FOR SUBQUERY BLOCK\n%---------------------------------\n");
						for(int l=0; l< qbt.getNoOfGroups(); l++)
							for(int k=0; k < qbt.getAggConstraints().size();k++){
								cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
							}
						cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n");
						
						
												
						/** add constraints of outer query block */
						cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlockExceptSubQuries(cvc, cvc.getOuterBlock()) );
						
						
						cvc.setCVCStr(CVCStr);
						
						/** Call the method for the data generation*/
						GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
					}
					
				}
			}
			
		}
		
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}

}
