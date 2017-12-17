package killMutations.outerQueryBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

public class PatternMutationOuterQueryBlock {

	private static Logger logger = Logger.getLogger(PatternMutationOuterQueryBlock.class.getName());
	
	public static void  generateDataForkillingMutationsGen ( GenerateCVC1 cvc) throws Exception{
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingMutations(cvc);
		}
		else{
			//generateDataForkillingMutationsSMT(cvc);
		}
		
	}
	/**
	 * Generates data to kill like pattern mutations inside outer query block
	 * @param cvc
	 */
	public static void generateDataForkillingMutations( GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING LIKE PATTERN MUTATIONS IN OUTER QUERY BLOCK");
		logger.log(Level.INFO,"\n----------------------------------\n");
		try{
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();

		/**Kill the like clause mutations in each conjunct of this outer block of  query */
		for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){

			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"NEW CONJUNCT IN LIKE CLAUSE MUTATIONS KILLING: " + conjunct);
			logger.log(Level.INFO,"\n----------------------------------\n");
			
			/**Get the like conditions of this conjunct*/
			Vector<Node> likeConds = conjunct.getLikeConds();

			/** Kill each like condition of this conjunct*/
			for(int i=0; i < likeConds.size(); i++){

				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"\n\nGETTING LIKE MUTANTS\n");
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				Node lc = likeConds.get(i);
				
				ArrayList<Node> patternMutants = UtilsRelatedToNode.getLikePatternMutations(lc);
				
				/** Generate data set to kill each mutation*/
				for(int j=0; j<patternMutants.size(); j++){

						logger.log(Level.INFO,"\n----------------------------------");
						logger.log(Level.INFO,"KILLING : " + patternMutants.get(j));
						logger.log(Level.INFO,"\n----------------------------------\n");
						
						/** This is required so that the tuple assignment for the subquery is fine*/
						likeConds.set(i,patternMutants.get(j) );

						/** Initialize the data structures for generating the data to kill this mutation */
						cvc.inititalizeForDatasetQs();

						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.PATTERN, TagDatasets.QueryBlock.OUTER_BLOCK );

						/** get the tuple assignment for this query
						 * If no possible assignment then not possible to kill this mutation*/
						if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
							continue;

						/** Add constraints for all the From clause nested subquery blocks */
						for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY"));

							cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );

							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF FROM CLAUSE SUBQUERY"));
						}


						/** Generate postive constraints for all the conditions of this  conjunct */
						cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );


						/** Add negative conditions for all other conjuncts of this query block*/
						for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
							if(inner != conjunct)
								cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	

						/** get group by constraints */
						
						cvc.getConstraints().add(ConstraintGenerator.addCommentLine("GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
						cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );


						/** Generate havingClause constraints */
						cvc.getConstraints().add(ConstraintGenerator.addCommentLine("HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
						for(int l=0; l< qbt.getNoOfGroups(); l++)
							for(int k=0; k < qbt.getAggConstraints().size();k++){
								cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
							}
						cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));

						/** add other constraints of outer query block */
						cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );

						/** Call the method for the data generation*/
						GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
					
				}
				/**Revert the change in like conditions list of this  block */
				likeConds.set(i,lc);
			}
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
