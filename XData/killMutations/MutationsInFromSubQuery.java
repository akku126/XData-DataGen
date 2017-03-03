package killMutations;

import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.QueryParser;
import parsing.QueryStructure;

import killMutations.fromClauseNestedBlock.*;
import testDataGen.GenerateCVC1;

/**
 * This class generates data sets for killing each type of mutation inside each from clause nested sub query block
 * @author mahesh
 *
 */
public class MutationsInFromSubQuery {

	private static Logger logger = Logger.getLogger(MutationsInFromSubQuery.class.getName());
	public static void generateDataForKillingMutantsInFromSubQuery(GenerateCVC1 cvc) throws Exception{

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"FROM SUBQUERY MUTANTS  IN FROM CLAUSE NESTED SUBQUERY BLOCK QUERY");
		logger.log(Level.INFO,"---------------------------------\n");
		boolean hasSetOperator = false;
		for(int i=0;i<cvc.getqStructure().getFromClauseSubqueries().size();i++){
			QueryStructure qpFrom = cvc.getqStructure().getFromClauseSubqueries().get(i);
			if(qpFrom.setOperator!=null && qpFrom.setOperator.length()>0){
				hasSetOperator = true;
				/** Killing Set Operator Mutation in SubQuery **/
				SetOperatorMutationInFromClause.generateDataForkillingSetOperatorMutationsInFromSubquery(cvc);
			}
		}
		if(!hasSetOperator){
		/**killing equivalence class mutations in from clause nested sub query blocks*/
		EquivalenceMutationInFromSubQuery.generateDataForkillingEquivalenceClassMutationsInFromSubquery(cvc);

		/** killing join predicate mutations in from clause nested sub query blocks */
		JoinMutationsInFromSubQuery.generateDataForkillingJoinMutationsInFromSubquery(cvc);

		
		/** killing selection mutations in from clause nested sub query blocks*/
		SelectionMutationsInFromSubquery.generateDataForkillingSelectionMutationsInFromSubquery(cvc);

		/** killing string selection mutations in from clause nested subquery blocks*/
		StringSelectionMutationsInFromSubquery.generateDataForkillingStringSelectionMutationsInFromSubquery(cvc);

		/** killing like mutations in from clause nested subquery blocks*/
		LikeMutationsInFromSubquery.generateDataForkillingLikeMutationsInFromSubquery(cvc);

		/** killing like pattern mutations in from clause nested subquery blocks*/
		PatternMutationsInFromSubquery.generateDataForkillingMutations(cvc);
		
		/** killing aggregate function mutations in from clause nested subquery blocks*/
		AggMutationsInFromSubQuery.generateDataForkillingAggMutationsInFromSubquery(cvc);

		/** killing having clause mutations in from clause nested subquery blocks*/
		ConstrainedAggregationMutationsInFromSubQuery.generateDataForkillingConstrainedAggregationInFromSubquery(cvc);

		/** kill distinct mutations in from clause nested subquery blocks*/
		DistinctMutationsInFromSubQuery.generateDataForkillingDistinctMutationsInFromSubquery(cvc);

		/** Kill mutations for CASE statements in from subquery blocks*/
		CaseMutationsInFromSubQuery.generateDataForKillingCaseMutationsInFromSubquery(cvc);
		
		/** partial group by mutations in from clause nested subquery blocks*/
		PartialGroupByMutationsInFromSubQuery_case1.generateDataForkillingParialGroupByMutationsInFromSubquery(cvc);

		/** Killing partial group by mutations in from clause nested subquery blocks*/
		PartialGroupByMutationsInFromSubQuery_case2.generateDataForkillingParialGroupByMutationsInFromSubquery(cvc);

		/** Killing extra group by attribute mutations in from clause nested subquery blocks */
		ExtraGroupByMutationsInFromSubQuery.generateDataForkillingExtraGroupByMutationsInFromSubquery(cvc);

		/** Killing common name mutations in from clause nested subquery blocks*/
		UnintendedJoinsInFromSubQuery.generateDataForkillingUnintendedJoinsInFromSubquery(cvc);
		
		}
		
	}

}
