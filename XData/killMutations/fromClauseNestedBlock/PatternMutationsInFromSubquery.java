package killMutations.fromClauseNestedBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
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
 * This class generates data sets to kill like pattern mutations in the from clause nested subquery block
 * @author bikash
 *
 */
public class PatternMutationsInFromSubquery {
	private static Logger logger = Logger.getLogger(PatternMutationsInFromSubquery.class.getName());
	/**
	 * Generates data to kill like pattern mutations inside from clause nested subquery block
	 * @param cvc
	 */
public static void generateDataForkillingMutations( GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		//HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		
		/** Kill like clause mutations in each from clause nested block of this query*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries()){
			
			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"GENERATE DATA FOR Killing LIKE clause Mutations Inside From clause subquery block: "+ qbt);
			logger.log(Level.INFO,"----------------------------------\n");
			
			
			/**Kill the like clause mutations in each conjunct of this sub query query block*/
			for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){
				
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"NEW CONJUNCT IN LIKE CLAUSE MUTATIONS KILLING: " + conjunct);
				logger.log(Level.INFO,"----------------------------------\n");
				
				/**Get the like conditions of this conjunct*/
				Vector<Node > likeConds = conjunct.getLikeConds();
				
				/** Kill each like condition of this conjunct*/
				for(int i=0; i < likeConds.size(); i++){
					
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"\n\nGETTING LIKE PATTERN MUTANTS\n");
					logger.log(Level.INFO,"----------------------------------\n");
					
					Node lc = likeConds.get(i);
					
					ArrayList<Node> patternMutants = UtilsRelatedToNode.getLikePatternMutations(lc);
					
					/** Generate data set to kill each mutation*/
					for(int j=0; j<patternMutants.size(); j++){
							
							logger.log(Level.INFO,"\n----------------------------------");
							logger.log(Level.INFO,"KILLING : " + patternMutants.get(j));
							logger.log(Level.INFO,"----------------------------------\n");
							
							/** This is required so that the tuple assignment for the subquery is fine*/
							likeConds.set(i,patternMutants.get(j) );	
							
							/** Initialize the data structures for generating the data to kill this mutation */
							cvc.inititalizeForDatasetQs();


							/**set the type of mutation we are trying to kill*/
							cvc.setTypeOfMutation( TagDatasets.MutationType.PATTERN, TagDatasets.QueryBlock.FROM_SUBQUERY );
							
							/** get the tuple assignment for this query
							 * If no possible assignment then not possible to kill this mutation*/
							if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
								continue;
							
							
							/** Add constraints for all the From clause nested subquery blocks except this sub query block */
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
								if(!(qb.equals(qbt))){
									cvc.getConstraints().add(ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));
									
									cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
									
									cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF FROM CLAUSE SUBQUERY "));
								}
							}
							
							
							/** Generate positive constraints for all the conditions of this sub query block conjunct */
							/**Here the mutated condition is also included in the list of like conditions of the subquery block*/
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );
							
							/** Add negative conditions for all other conjuncts of this subquery block*/
							for(ConjunctQueryStructure inner: qbt.getConjunctsQs()){
								if(inner != conjunct){
									cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
								}
							}
							
							/** get group by constraints */
							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("GROUP BY CLAUSE CONSTRAINTS FOR SUBQUERY BLOCK "));
							cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
							
							
							/** Generate havingClause constraints */
							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("HAVING CLAUSE CONSTRAINTS FOR SUBQUERY BLOCK "));
							for(int l=0; l< qbt.getNoOfGroups(); l++)
								for(int k=0; k < qbt.getAggConstraints().size();k++){
									cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
								}
							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF HAVING CLAUSE CONSTRAINTS "));
							
							/** add constraints of outer query block */
							cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlockExceptSubQuries(cvc, cvc.getOuterBlock()) );
							
							/** Call the method for the data generation*/
							GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
						
					}
					/**Revert the change in selection conditions list of this subquery block */
					likeConds.set(i,lc);
				}
			}
		}
		
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
	//	cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}

}
