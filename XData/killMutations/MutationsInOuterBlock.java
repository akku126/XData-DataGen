package killMutations;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.outerQueryBlock.*;
import testDataGen.GenerateCVC1;

/**
 * This class generates data sets for killing each type of mutation in outer query block
 * @author mahesh
 *
 */
public class MutationsInOuterBlock {

	private static Logger logger = Logger.getLogger(MutationsInOuterBlock.class.getName());
	
	public static void generateDataForKillingMutantsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"MUTANTS  IN OUTER QUERY BLOCK QUERY");
		logger.log(Level.INFO,"---------------------------------\n");
		try{
			/**killing equivalence class mutations in in outer query blocks*/
			EquivalenceMutationInOuterQueryBlock.generateDataForkillingEquivalenceClassMutationsInOuterQueryBlock(cvc);
	
			/** killing join predicate mutations in outer query blocks */
			JoinMutationsInOuterQueryBlock.generateDataForkillingJoinMutationsInOuterQueryBlock(cvc);
	
			/** killing selection mutations in outer query blocks*/
			SelectionMutationsInOuterQueryBlock.generateDataForkillingSelectionMutationsInOuterQueryBlock(cvc);
	
			/** killing string selection mutations in outer query blocks*/
			StringSelectionMutationsInOuterQueryBlock.generateDataForkillingStringSelectionMutationsInOuterQueryBlock(cvc);
	
			/** killing like mutations in outer query blocks*/
			LikeMutationsInOuterQueryBlock.generateDataForkillingLikeMutationsInOuterQueryBlock(cvc);
	
			//** killing like pattern mutation in outer query blocks*/
			PatternMutationOuterQueryBlock.generateDataForkillingMutations(cvc);
			
			/** killing null mutations in outer query blocks*/
			//NullMutationsInOuterBlock.generateDataForkillingSelectionMutationsInOuterQueryBlock(cvc);
			
			/** killing aggregate function mutations in outer query blocks*/
			AggMutationsInOuterQueryBlock.generateDataForkillingAggMutationsInOuterQueryBlock(cvc);
	
			
			WhereClauseSubQueryConnectiveMutations.killWhereClauseSubQueryConnectiveMutations(cvc);
			
			/**Killing missing join condition with reference relations **/
			MissingJoinMutations.generateDataForkillingMissingJoinMutations(cvc);
			
			/** killing having clause mutations in outer query blocks*/
			ConstrainedAggregationMutationsInOuterQueryBlock.generateDataForkillingConstrainedAggregationInOuterQueryBlock(cvc);
	
			/** kill distinct mutations in outer query blocks*/
			DistinctMutationsInOuterQueryBlock.generateDataForkillingDistinctMutationsInOuterQueryBlock(cvc);

			/** Kill mutations for CASE statements in outer query block*/
			CaseMutationsInOuterQueryBlock.generateDataForKillingCaseMutationsInOuterQueryBlock(cvc);
			
			/**Partial group by mutations were here and then moved to last to add foreign key referenced attributes to groupby ***/
			
			
			/** Killing extra group by attribute mutations in outer query blocks */
			ExtraGroupByMutationsInOuterQueryBlock.generateDataForkillingExtraGroupByMutationsInOuterQueryBlock(cvc);
			ExtraGroupByMutationsInOuterQueryBlock.generateDataForkillingExtraGroupByMutationsInOuterQueryBlock2(cvc);
	
			/** Killing common name mutations in outer query blocks*/
			UnintendedJoinsMutationsInOuterQueryBlock.generateDataForkillingUnintendedJoinsMutationsInOuterQueryBlock(cvc);
			
			/** Killing mutations resulting from column replacement */
			ColumnReplacementMutations.generateDataForkillingColumnReplacementMutationsInProjection(cvc);	
			
			/** partial group by mutations in outer query blocks*/
			PartialGroupByMutationsInOuterQueryBlock_case1.generateDataForkillingParialGroupByMutationsInOuterQueryBlock(cvc);
	
			/** Killing partial group by mutations in outer query blocks*/
			PartialGroupByMutationsInOuterQueryBlock_case2.generateDataForkillingParialGroupByMutationsInOuterQueryBlock(cvc);
	
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Process interrupted or timed out.", e);
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}
}
