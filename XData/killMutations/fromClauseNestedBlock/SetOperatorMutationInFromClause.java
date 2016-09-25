package killMutations.fromClauseNestedBlock;

import java.util.logging.Logger;

import parsing.QueryParser;

import testDataGen.GenerateCVC1;

/*
 * This class generates data sets to kill Set Operator mutations in from clause nested subquery block
 * @author Shree
 * 
 */
public class SetOperatorMutationInFromClause {

	private static Logger logger = Logger.getLogger(SetOperatorMutationInFromClause.class.getName());
	
	/**
	 * Generates data to kill Set Operator mutations inside from clause nested subquery block
	 * 
	 * @param cvc
	 */
	public static void generateDataForkillingSetOperatorMutationsInFromSubquery(GenerateCVC1 cvc) throws Exception{
		
		
		GenerateCVC1 genSetOpCvc = new GenerateCVC1();
		//Initialize CVC with subquery qparser Setoperator query data and then call data generation for SetOperator.
		for(int i=0;i<cvc.getqParser().getFromClauseSubqueries().size();i++){
			QueryParser qpFrom = cvc.getqParser().getFromClauseSubqueries().get(i);
			if(qpFrom.setOperator!=null && qpFrom.setOperator.length()>0){
				
					//qpFrom.setQuery(cvc.getqParser().getQuery());
					/**Initialize the query details to the object*/
					//genSetOpCvc.initializeQueryDetails(qpFrom);
					//PreProcessingActivity.genDataForSetOp(genSetOpCvc,qpFrom.setOperator);
					
				
			}
		}
	}
	
		
}
