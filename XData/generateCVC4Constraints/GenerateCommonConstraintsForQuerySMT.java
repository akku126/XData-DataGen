package generateCVC4Constraints;

import generateConstraints.AddDataBaseConstraints;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsRelatedToBranchQuery;
import generateConstraints.GetCVC3HeaderAndFooter;
import generateConstraints.TupleRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;

import testDataGen.GenerateCVC1;
import testDataGen.GenerateDataset_new;
import testDataGen.PopulateTestData;
import testDataGen.QueryBlockDetails;
import testDataGen.WriteFile;
import util.Configuration;

public class GenerateCommonConstraintsForQuerySMT {

	private static Logger logger=Logger.getLogger(GenerateCommonConstraintsForQuerySMT.class.getName());
	
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
							ConstraintString += GenerateCVCConstraintForNodeSMT.cvcSetNull(cvc, n.getLeft().getColumn(), (offset+i)+"");
	
				}
			}
			return ConstraintString;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}
	
	
	/**
	 * 
	 * @param cvc Object used for generating constraints
	 * @param unique Whether all string variables have different values or not
	 * @return If data generation was successful or not 
	 * @throws Exception
	 */
	public static boolean generateDataSetForConstraintsForSMT(GenerateCVC1 cvc, Boolean unique) throws Exception{
		try{
			if( cvc.getCVCStr() == null)
				cvc.setCVCStr("");
			String CVCStr = cvc.getCVCStr();
	 
			/** Solve the string constraints for the query */
			if(!cvc.getStringConstraints().isEmpty()) {
			//	cvc.getConstraints().add("\n;---------------------------------\n; TEMP VECTOR CONSTRAINTS\n%---------------------------------\n");
				Vector<String> tempVector = cvc.getStringSolver().solveOrConstraintsForSMT( new Vector<String>(cvc.getStringConstraints()), cvc.getResultsetColumns(), cvc.getTableMap());		
				
				//cvc.getConstraints().addAll(tempVector);				
			}
	
			cvc.setCVCStr(CVCStr);
			/** Add constraints, if there are branch queries*/
			if( cvc.getBranchQueries().getBranchQuery() != null)
			{
				//cvc.getConstraints().add("\n;---------------------------------\n;BRANCHQUERY CONSTRAINTS\n%---------------------------------\n");
				//cvc.getConstraints().add( GenerateConstraintsRelatedToBranchQuery.addBranchQueryConstraintsForSMT( cvc ));
				//cvc.getConstraints().add("\n;---------------------------------\n;END OF BRANCHQUERY CONSTRAINTS\n%---------------------------------\n");
			}
			
			if(cvc.getOuterBlock().isConstrainedAggregation())
				//cvc.getConstraints().add(addNoExtraTuple(cvc));		
	
			for(int k=0; k < cvc.getConstraints().size(); k++){
				//CVCStr = "\n" + cvc.getConstraints().get(k);
			}
	
			cvc.setDatatypeColumns( new ArrayList<String>() );
			String CVC4_HEADER = GetCVC4HeaderAndFooter.generateCVC4_Header(cvc, false);
			
			
			 CVCStr = ";--------------------------------------------\n\n;MUTATION TYPE: " + 
					cvc.getTypeOfMutation() +"\n\n;--------------------------------------------\n\n\n\n  ;Construction Under Progress \n\n" + 
					CVC4_HEADER +CVCStr;
	
			/** Add not null constraints */
			//cvc.getConstraints().add("\n;---------------------------------\n;NOT NULL CONSTRAINTS\n%---------------------------------\n  Construction of Constraints under Progress \n");
			//CVCStr += GenerateCVCConstraintForNode.cvcSetNotNull(cvc);
			
			CVCStr += GetCVC4HeaderAndFooter.generateCvc4_Footer();
	
			cvc.setCVCStr(CVCStr);
			System.out.println("\n CVSTR : \n"+CVCStr);
			/** Add extra tuples to satisfy branch queries constraints*/
			for(int i = 0; i < cvc.getBranchQueries().getNoOfBranchQueries(); i++){
				
				HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[] = cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries();
				
				for(Table tempTab : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
					
					cvc.getNoOfOutputTuples().put(tempTab.getTableName(), cvc.getNoOfOutputTuples().get(tempTab.getTableName()) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab));
			}
	
			/** Call CVC3 Solver with constraints */
			logger.log(Level.INFO,"cvc count =="+cvc.getCount());
			WriteFile.writeFile(Configuration.homeDir + "temp_cvc" + cvc.getFilePath() + "/cvc4_" + cvc.getCount() + ".cvc", CVCStr);
		
			Boolean success= new PopulateTestData().killedMutants("cvc4_" + cvc.getCount() 
					+ ".cvc", cvc.getQuery(), 
					"DS" + cvc.getCount(), cvc.getQueryString(), cvc.getFilePath(), cvc.getNoOfOutputTuples(), cvc.getTableMap(), 
					cvc.getResultsetColumns(), cvc.getRepeatedRelationCount().keySet()) ;
			cvc.setOutput( cvc.getOutput() + success);
			cvc.setCount(cvc.getCount() + 1);
	
			/** remove extra tuples for Branch query */		
			for(int i = 0; i < cvc.getBranchQueries().getNoOfBranchQueries(); i++){
				
				HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[] = cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries();
				
				for(Table tempTab : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
					
					cvc.getNoOfOutputTuples().put(tempTab.getTableName(), cvc.getNoOfOutputTuples().get(tempTab.getTableName()) - noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab));
			}
			

			/**Upload DB as and when the constraints are generated **/
			ArrayList<String> newList = new ArrayList<String>();
			newList.add("DS"+(cvc.getCount()-1));
			logger.log(Level.INFO,"\n\n***********************************************************************\n");
			if(cvc.getConcatenatedQueryId() != null){
				logger.log(Level.INFO,"DATA SETS FOR QUERY "+cvc.getConcatenatedQueryId()+" ARE GENERATED");
			}else{
				logger.log(Level.INFO,"DATA SETS FOR QUERY ARE GENERATED");
			}
			logger.log(Level.INFO,"\n\n***********************************************************************\n");
			GenerateDataset_new fp = new GenerateDataset_new( cvc.getFilePath());
			
			
			return success;
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,"Timeout in generating dataset "+cvc.getCount()+" : "+e.getMessage());		
			
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage());		
		}
		return false;
	
	}
		
	private static String addNoExtraTuple(GenerateCVC1 cvc){
		ArrayList<Node> sel = new ArrayList<Node>();
		
		ArrayList<Node> groupByNodes = cvc.outerBlock.getGroupByNodes();
		for(Node n : groupByNodes){
			String relation = n.getTable().getTableName();
			TupleRange t = cvc.getTupleRange().get(relation);			
			Node newNode = new Node();
			newNode.setLeft(n);
			newNode.setType(Node.getBroNodeType());
			
			Node rightCond = new Node();
			//Set SMT condition for the node
			//rightCond.setStrConst(GenerateCVCConstraintForNode.cvcMapNode(n, (t != null?t.start:0) + ""));
			rightCond.setType(Node.getValType());
			newNode.setRight(rightCond);
			newNode.setOperator("=");
			sel.add(newNode);
		}
		
		return "";//GenerateCommonConstraintsForQuery.generateConstraintsForNoExtraTuplesForSMT(cvc, cvc.getOuterBlock(), sel, cvc.getTupleRange());
	}
}
