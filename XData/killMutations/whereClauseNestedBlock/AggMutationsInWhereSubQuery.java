package killMutations.whereClauseNestedBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsToKillAggregationMutations;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.KillCountMutations;
import killMutations.Utils;
import parsing.AggregateFunction;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill aggregation function mutations in the Where clause nested subquery block
 * @author mahesh
 *
 */
public class AggMutationsInWhereSubQuery {

	private static Logger logger = Logger.getLogger(AggMutationsInWhereSubQuery.class.getName());
	/**
	 * Generates data to kill aggregation function mutations inside each Where clause nested subquery block
	 * @param cvc
	 */
	/**FIXME: COUNT() to COUNT(*) is not added yet*/

	public static void generateDataForkillingAggMutationsInWhereSubquery(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/** we have to check if there are where clause subqueries in each conjunct of outer block of query */
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs()){

			/**For each where clause subquery blocks of this conjunct*/
			/** Kill aggregation  mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this subquery node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause subquery block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				/** TO indicate if there is COUNT() in aggregate function list*/
				boolean killCountMutants = false;

				int attempt = 0;

				/** Get the aggregate function list of this subquery block*/
				ArrayList<AggregateFunction> aggFunc = qbt.getAggFunc();

				/**Kill each aggregate function mutation*/
				for(int i=0; i< aggFunc.size(); i++){

					/** Initialize the data structures for generating the data to kill this mutation */
					cvc.inititalizeForDatasetQs();

					/**set the type of mutation we are trying to kill*/
					cvc.setTypeOfMutation( TagDatasets.MutationType.AGG, TagDatasets.QueryBlock.WHERE_SUBQUERY );
					
					/**Get aggregate function to kill in this iteration*/
					AggregateFunction af = aggFunc.get(i);

					/**Will generate datasets later to kill COUNT <-> COUNT(*) */
					if(af.getFunc().toUpperCase().contains("COUNT"))
						killCountMutants = true;

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"\nKILLING IN WHERE CLAUSE SUBQUERY BLOCK: " + af.toString()+"   ATTEMPT NUMBER: "+(attempt+1));
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					qbt.setConstrainedAggregation(true);
					
					/**get the count needed*/
					if(CountEstimationRelated.getCountNeededToKillAggregationMutation(cvc, qbt, attempt) == false)
						continue;

					/**assign the number of tuples for the this from clause subquery*/
					/**If tuple assignment is not possible then continue to kill next mutation*/
					if( QueryBlockDetails.getTupleAssignment( cvc, qbt, null) == false)
						continue ;
					
					/**get the tuple assignment for all other query blocks*/
					if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvc, qbt) == false)
						continue;
					
					
					
					/** get constraints for this conjunct of outer query block, This also adds constraints for all the conditions including group by and having clause of this where clause subquery */
					cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, cvc.getOuterBlock(), con) );
					
					/**add the negative constraints for all the other conjuncts of outer query block */
					for(ConjunctQueryStructure outer: cvc.getOuterBlock().getConjunctsQs())
						if( !outer.equals(con))
							cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, cvc.getOuterBlock(), outer) );
						
					/** add group by and having clause constraints for outer query block */
					cvc.getConstraints().add( QueryBlockDetails.getGroupByAndHavingClauseConstraints(cvc, cvc.getOuterBlock())) ;
					
					/**Add other related constraints for the outer query block */
					cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock())) ;
					
					
					cvc.getConstraints().add( ConstraintGenerator.addCommentLine("AGGREGATION CONSTRAINTS FOR WHERE CLAUSE SUBQUERY "));
					cvc.getConstraints().add( (GenerateConstraintsToKillAggregationMutations.getAggConstraints(cvc, af, qbt.getNoOfGroups())) );
					cvc.getConstraints().add( ConstraintGenerator.addCommentLine("END OF AGGREGATION CONSTRAINTS FOR WHERE CLAUSE SUBQUERY "));

					/** Call the method for the data generation*/
					GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);

					/**check whether the data generation is succeeded or not
					 * if not then give another attempt by changing number of tuples
					 */
					int [] list = Utils.checkIfSucces(cvc, attempt, i);
					
					attempt = list[0];
					i = list[1];
					
					
					/**Reset the variable*/
					qbt.setConstrainedAggregation(false);
					
					/**Will generate data sets later to kill COUNT <-> COUNT(*) */
					if(af.getFunc().toUpperCase().contains("COUNT"))			
						killCountMutants = true;	
				}
				
				if(killCountMutants)
					KillCountMutations.killCountMutations(cvc, qbt);
			}
		}

		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );

	}



}
