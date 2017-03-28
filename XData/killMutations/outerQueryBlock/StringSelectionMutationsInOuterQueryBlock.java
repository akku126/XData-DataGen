package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill string selection conditions (of the form A.x relOP const) class mutations in outer block
 * @author mahesh
 *
 */
public class StringSelectionMutationsInOuterQueryBlock {

	private static Logger logger = Logger.getLogger(StringSelectionMutationsInOuterQueryBlock.class.getName());
	
	public static void  generateDataForkillingStringSelectionMutationsInOuterQueryBlockGen(	GenerateCVC1 cvc) throws Exception{
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingStringSelectionMutationsInOuterQueryBlock(cvc);
		}
		else{
			//generateDataForkillingStringSelectionMutationsInOuterQueryBlockSMT(cvc);
		}
	}
	/**
	 * Generates data to kill string selection conditions mutations inside outer block
	 * @param cvc
	 */
	public static void generateDataForkillingStringSelectionMutationsInOuterQueryBlock(	GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING STRING SELECTION CLAUSE MUTATIONS IN OUTER QUERY BLOCK");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();
		try{
			/**Kill the string selection clause mutations in each conjunct of this outer block of  query */
			for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){
	
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"NEW CONJUNCT IN STRING SELCTION CLAUSE MUTATIONS KILLING: " + conjunct);
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				/**Get the string selection conditions of this conjunct*/
				Vector<Node > stringSelectionConds = conjunct.getStringSelectionConds();
	
				/** Kill each string selection condition of this conjunct*/
				for(int i=0; i < stringSelectionConds.size(); i++){
	
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"\n\nGETTING STRING SELECTION MUTANTS\n");
					logger.log(Level.INFO,"\n----------------------------------\n");
	
					
					Node sc = stringSelectionConds.get(i);
	
					Vector<Node> scMutants = UtilsRelatedToNode.getStringSelectionCondMutations(sc);
	
					/** Generate data set to kill each mutation*/
					for(int j=0; j<scMutants.size(); j++){
	
						/**If this mutation is not same as that of original condition*/
						if(!( scMutants.get(j).getOperator().equalsIgnoreCase(sc.getOperator())) ){
	
							logger.log(Level.INFO,"\n----------------------------------");
							logger.log(Level.INFO,"KILLING : " + scMutants.get(j));
							logger.log(Level.INFO,"\n----------------------------------\n");
							
							/** This is required so that the tuple assignment for the subquery is fine*/
							stringSelectionConds.set(i,scMutants.get(j) );
	
							/** Initialize the data structures for generating the data to kill this mutation */
							cvc.inititalizeForDatasetQs();
	
							/**set the type of mutation we are trying to kill*/
							cvc.setTypeOfMutation( TagDatasets.MutationType.STRING, TagDatasets.QueryBlock.OUTER_BLOCK );
							
							/** get the tuple assignment for this query
							 * If no possible assignment then not possible to kill this mutation*/
							if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
								continue;
	
							/** Add constraints for all the From clause nested subquery blocks */
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
								cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");
	
								cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
	
								cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");
							}
	
	
							/** Generate positive constraints for all the conditions of this  conjunct */
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );
	
	
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
	
							/** Call the method for the data generation*/
							GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
						}
					}
					/**Revert the change in string selection conditions list of this subquery block */
					stringSelectionConds.set(i,sc);
				}
			}
			
			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
	//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
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
