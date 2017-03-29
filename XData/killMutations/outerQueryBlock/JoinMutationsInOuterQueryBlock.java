package killMutations.outerQueryBlock;

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
import java.util.concurrent.TimeoutException;
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
 * This class generates data sets to kill non equi join class mutations in the outer query block
 * @author mahesh
 *
 */
public class JoinMutationsInOuterQueryBlock {

	private static Logger logger = Logger.getLogger(JoinMutationsInOuterQueryBlock.class.getName());
	
	public static void generateDataForkillingJoinMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingJoinMutationsInOuterQueryBlockCVC(cvc);
		}
		else{
			//generateDataForkillingJoinMutationsInOuterQueryBlockSMT(cvc);
		}
	}
	
	
	/**
	 * Generates data to kill non equi-join  class mutations inside outer query block
	 * @param cvc
	 */
	public static void generateDataForkillingJoinMutationsInOuterQueryBlockCVC(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING NON-EQUI JOIN CLAUSE MUTATIONS IN OUTER BLOCK OF QUERY");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();
		try{
			/**Kill the non equi-join clause mutations in each conjunct of this outer block of  query */
			for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){
	
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"NEW CONJUNCT IN NEC KILLING: " + conjunct);
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				
				/**Get the non equi-join conditions of this conjunct*/
				Vector<Node > allConds = conjunct.getAllConds();
	
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"KILLING NON EQUI JOIN PREDICATES IN OUTER BLOCK OF QUERY: " + allConds);
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				/** Kill each non equi-join condition of this conjunct*/
				for(int i=0; i<allConds.size(); i++){
	
					Node pred = allConds.get(i);
	
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING NON EQUI JOIN PREDICATE: " + pred);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					
					/** Find the different relations involved in pred. Pred might be an arbitrary predicate */
					HashMap<String,Table> rels = UtilsRelatedToNode.getListOfRelationsFromNode(cvc, qbt, pred);
	
					Iterator rel = rels.keySet().iterator();
					while(rel.hasNext()){
	
						String CVCStr="";
						cvc.setConstraints( new ArrayList<String>());
						cvc.setStringConstraints( new ArrayList<String>());
						cvc.setTypeOfMutation("");
						cvc.setCVCStr("");
						
						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.NONEQUIJOIN, TagDatasets.QueryBlock.OUTER_BLOCK );
						
						/** Assign the number of tuples and their positions */
						cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
						cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
						cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	
						/** Add constraints related to parameters*/
						cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt));
	
						/** Add constraints for all the From clause nested subquery blocks*/
						for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
							cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");
	
							cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
	
							cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");
						}
	
						String aliasName = (String)rel.next();
						String tableName = rels.get(aliasName).getTableName();
						
						/** FIXME: This function is generating constraints of form ASSERT NOT EXISTS (i: O_SECTION_INDEX_INT): ((O_SECTION[1].0>O_TAKES[1].1));
						* These are causing problem. Example query19*/
						/**FIXME: Also the repeated relations are not correctly handled in the below method */
						cvc.getConstraints().add( GenerateCVCConstraintForNode.genNegativeCondsForPredAgg(cvc, qbt, pred, aliasName, tableName) );
						
						/** get positive constraints for all conditions except all conditions of the conjunct */
						cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptNonEquiJoins(cvc, qbt, conjunct) );
						
						/** Add negative conditions for all other conjuncts of this query block*/
						for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
							if(inner != conjunct)
								cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );
						
						/** get group by constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
						cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
						
						
						/** Generate havingClause constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
						for(int l=0; l< qbt.getNoOfGroups(); l++)
							for(int k=0; k < qbt.getAggConstraints().size();k++){
								cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
							}
						cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
						
						
												
						/** add other constraints of outer query block */
						cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );
						
						cvc.setCVCStr(CVCStr);
						
						/** Call the method for the data generation*/
						GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
						
						
					}
				}
			}
	
			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
			cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
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
