package killMutations.fromClauseNestedBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsToKillAggregationMutations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.KillCountMutations;
import killMutations.Utils;
import parsing.AggregateFunction;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data sets to kill aggregation function mutations in the from clause nested sub query block
 * @author mahesh
 *
 */
public class AggMutationsInFromSubQuery {

	private static Logger logger = Logger.getLogger(AggMutationsInFromSubQuery.class.getName());
	/**
	 * Generates data to kill aggregation function mutations inside each from clause nested sub query block
	 * @param cvc
	 */
	/**FIXME: COUNT() to COUNT(*) is not added yet*/

	public static void generateDataForkillingAggMutationsInFromSubquery(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/** Kill aggregation mutations in each from clause nested block of this query*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries()){

			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"GENERATE DATA FOR Killing AGGREGATION FUNCTION Mutations Inside From clause subquery block: "+ qbt);
			logger.log(Level.INFO,"----------------------------------\n");
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
				cvc.setTypeOfMutation( TagDatasets.MutationType.AGG, TagDatasets.QueryBlock.FROM_SUBQUERY );

				/**Get aggregate function to kill in this iteration*/
				AggregateFunction af = aggFunc.get(i);

				/**Will generate datasets later to kill COUNT <-> COUNT(*) */
				if(af.getFunc().toUpperCase().contains("COUNT"))
					killCountMutants = true;

				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"\nKILLING IN FROM CLAUSE SUBQUERY BLOCK: " + af.toString()+"   ATTEMPT NUMBER: "+(attempt+1));
				logger.log(Level.INFO,"----------------------------------\n");
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
				

				/** Add constraints for all the blocks of the query */
				cvc.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(cvc));

				cvc.getConstraints().add("\n%---------------------------------\n% AGGREGATION CONSTRAINTS FOR FROM CLAUSE SUBQUERY\n%---------------------------------\n");
				cvc.getConstraints().add( (GenerateConstraintsToKillAggregationMutations.getAggConstraints(cvc, af, qbt.getNoOfGroups())) );
				cvc.getConstraints().add("\n%---------------------------------\n% END OF AGGREGATION CONSTRAINTS FOR FROM CLAUSE SUBQUERY\n%---------------------------------\n");

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

		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );

	}


	

}
