package killMutations.outerQueryBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * Generates dataset to kill null mutations in each conjunct
 * @author mahesh
 *
 */
public class NullMutationsInOuterBlock {

	private static Logger logger = Logger.getLogger(NullMutationsInOuterBlock.class.getName());
	/**
	 * Generates data to kill null conditions mutations inside outer block
	 * @param cvc
	 */
	/**FIXME: Bugs in this approach to kill mutations. Verify it*/
	public static void generateDataForkillingSelectionMutationsInOuterQueryBlock(	GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING SELECTION CLAUSE MUTATIONS IN OUTER QUERY BLOCK");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();
		
		/**Kill the null mutations in each conjunct of this outer block of  query */
		for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){
			
			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"NEW CONJUNCT IN NULL CLAUSE MUTATIONS KILLING: " + conjunct);
			logger.log(Level.INFO,"\n----------------------------------\n");
			
			
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();

			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.NULL, TagDatasets.QueryBlock.OUTER_BLOCK );
			
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				continue;
			
			
			/** Add constraints for all the From clause nested subquery blocks */
			for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
				cvc.getConstraints().add(ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));

				cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );

				cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" END OF FROM CLAUSE SUBQUERY "));
			}
			
			
			/** Generate positive constraints for all the conditions of this  conjunct */
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
	}
}
