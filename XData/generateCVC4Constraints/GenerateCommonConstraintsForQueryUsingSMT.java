package generateCVC4Constraints;

import generateConstraints.AddDataBaseConstraints;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;

import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

public class GenerateCommonConstraintsForQueryUsingSMT {

	private static Logger logger=Logger.getLogger(GenerateCommonConstraintsForQueryUsingSMT.class.getName());
	
	public static void generateNullandDBConstraintsUsingSMT(GenerateCVC1 cvc, Boolean unique) throws Exception {
	try{
		/** Add null constraints for the query */
		 getNullConstraintsForQuery(cvc);


		if( cvc.getCVCStr() == null)
			cvc.setCVCStr("");
		String CVCStr = cvc.getCVCStr();

		/**Add constraints related to database */
		CVCStr += AddDataBaseConstraintsSMT.addDBConstraintsForSMT(cvc);
		cvc.setCVCStr(CVCStr);
	}catch (TimeoutException e){
		logger.log(Level.SEVERE,e.getMessage(),e);		
		throw e;
	}catch(Exception e){
		logger.log(Level.SEVERE,e.getMessage(),e);		
		throw e;
	}
	}

	
	
	

	/**
	 * Used to get null constraints for each block of the input query
	 * @param cvc
	 */
	public static void getNullConstraintsForQuery(GenerateCVC1 cvc) throws Exception{

		try{
		/**Generate null constraints for outer query block */
		cvc.getConstraints().add( "\n%---------------------------------\n%NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY\n%---------------------------------\n" );
		cvc.getConstraints().add( getNullCOnstraintsForQueryBlockSMT(cvc, cvc.getOuterBlock()) );
		cvc.getConstraints().add( "\n%---------------------------------\n%END OF NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY\n%---------------------------------\n" );

		/**Generate null constraints for each from clause sub query block */
		for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getFromClauseSubQueries()){

			cvc.getConstraints().add( "\n%---------------------------------\n%NULL CONSTRAINTS FOR FROM CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
			cvc.getConstraints().add( getNullCOnstraintsForQueryBlockSMT(cvc, queryBlock) );
			cvc.getConstraints().add( "\n%---------------------------------\n%END OF NULL CONSTRAINTS FOR FROM CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
		}

		/**Generate null constraints for each where clause sub query block */
		for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getWhereClauseSubQueries()){

			cvc.getConstraints().add( "\n%---------------------------------\n%NULL CONSTRAINTS FOR WHERE CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
			cvc.getConstraints().add( getNullCOnstraintsForQueryBlockSMT(cvc, queryBlock) );
			cvc.getConstraints().add( "\n%---------------------------------\n%END OF NULL CONSTRAINTS FOR WHERE CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n" );
		}
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}
	
	
	
	/**
	 * Used to get null constraints for given query block
	 * @param cvc
	 * @param queryBlock
	 * @return
	 */
	public static String getNullCOnstraintsForQueryBlockSMT(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{

		String ConstraintString = "";
		try{
			ArrayList< Node > isNullConds = new ArrayList<Node>();
			/** Get constraints for each conjunct*/
			for(ConjunctQueryStructure conjunct : queryBlock.getConjunctsQs()){
	
				/**Get null conditions in this conjunct*/
				isNullConds.addAll( new ArrayList<Node>(conjunct.getIsNullConds()));
	
				/** for each node in the null conditions */
				for(Node n:isNullConds){
	
					Node relation = n.getLeft();
					int noOfTuples = cvc.getNoOfTuples().get(relation.getTableNameNo()) * queryBlock.getNoOfGroups();
					int offset = cvc.getRepeatedRelNextTuplePos().get(n.getLeft().getTableNameNo())[1];
					for(int i=0; i < noOfTuples; i++)
						if(n.getOperator().equals("="))
							ConstraintString += GenerateCommonConstraintsForNodeSMT.cvcSetNull(cvc, n.getLeft().getColumn(), (offset+i)+"");
	
				}
			}
			return ConstraintString;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}
}
