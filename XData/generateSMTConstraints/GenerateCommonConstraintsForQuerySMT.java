package generateSMTConstraints;

import generateConstraints.AddDataBaseConstraints;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsRelatedToBranchQuery;
import generateConstraints.GenerateJoinPredicateConstraints;
import generateConstraints.GetCVC3HeaderAndFooter;
import generateConstraints.TupleRange;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.RelationHierarchyNode;
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
				
				cvc.getConstraints().addAll(tempVector);				
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
			String CVC4_HEADER = GetSMTHeaderAndFooter.generateSMT_Header(cvc, false);
			
			
			 CVCStr = ";--------------------------------------------\n\n;MUTATION TYPE: " + 
					cvc.getTypeOfMutation() +"\n\n;--------------------------------------------\n\n\n\n  ;-----------------------------------------------------\n\n" + 
					CVC4_HEADER +CVCStr;
	
			/** Add not null constraints */
			//cvc.getConstraints().add("\n;---------------------------------\n;NOT NULL CONSTRAINTS\n%---------------------------------\n  Construction of Constraints under Progress \n");
			//CVCStr += GenerateCVCConstraintForNode.cvcSetNotNull(cvc);
			
			CVCStr += GetSMTHeaderAndFooter.generateSMT_Footer();
	
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
			rightCond.setStrConst(GenerateCVCConstraintForNodeSMT.cvcMapNodeSMT(n, (t != null?t.start:0) + ""));
			rightCond.setType(Node.getValType());
			newNode.setRight(rightCond);
			newNode.setOperator("=");
			sel.add(newNode);
		}
		
		return GenerateCommonConstraintsForQuerySMT.generateConstraintsForNoExtraTuplesSMT(cvc, cvc.getOuterBlock(), sel, cvc.getTupleRange());
	}
	
	// Generates Constraints for ensuring that no extra tuples are generated
	public static String generateConstraintsForNoExtraTuplesSMT(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ArrayList<Node> additionalSelConds, Map<String, TupleRange> allowedTuples) {
		String constraintString = "";
		
		Vector<String> orConstraints=new Vector<String>();
		
		Vector<String> orPrimaryKeyConstraints = new Vector<String>();
		
		ArrayList<String> relations = new ArrayList<String>();
		ArrayList<String> allRelations = new ArrayList<String>();
		try{
			RelationHierarchyNode hNode = queryBlock.getTopLevelRelation();
			
			flattenTree(hNode, relations);
			
			Map<String, Vector<Node> > selConds = new HashMap<String, Vector<Node> >();
			
			Map<String, Vector<Node> > joinConds = new HashMap<String, Vector<Node>>();
			
			// Selection conditions specific to each relation
			for(String s : relations) {
				selConds.put(s, new Vector<Node>());
				for(ConjunctQueryStructure con : queryBlock.getConjunctsQs()){
					if(con.getSelectionConds() != null) {
						for(Node n : con.getSelectionConds()){
							if(n.getLeft() != null 
									&& n.getLeft().getTable() != null
									&& n.getLeft().getTable().getTableName().equals(s)){
								Vector<Node> temp = selConds.get(s);
								temp.add(n);
								selConds.put(s, temp);
							} 
							else if(n.getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()) 
									&& (UtilsRelatedToNode.getTableName(n)).getTableName().equals(s)){
								Vector<Node> temp = selConds.get(s);
								temp.add(n);
								selConds.put(s, temp);
							}
						}
					}
				}
				
				for(Node n : additionalSelConds) {
					if(n.getLeft().getTable().getTableName().equals(s)){
						Vector<Node> temp = selConds.get(s);
						temp.add(n);
						selConds.put(s, temp);
					}
				}			
			}
			
			// Join conditions specific to each relation
			for(String s : relations) {
				for(ConjunctQueryStructure con : queryBlock.getConjunctsQs()){
					if(!con.getEquivalenceClasses().isEmpty()){
			
						/**Get the equivalence classes*/
						Vector<Vector<Node>> equivalenceClasses = con.getEquivalenceClasses();
			
						for(int i=0; i<equivalenceClasses.size(); i++){	/** For each equivalence class list*/
			
							/**Get this equivalence */
							Vector<Node> ec = equivalenceClasses.get(i);
							
							Node node = null;
							
							/**for each node in this equivalence*/
							for(int j=0;j<ec.size(); j++) {
								Node ece = ec.get(j);
								Table table = ece.getTable();
								
								if(table.getTableName().endsWith(s)){
									node = ece;
								}
							}
							
							if(node != null) {							
								joinConds.put(s, ec);							
							}
						}
					}
				}
			}
			
			Map<String, ArrayList<Column>> relationToPrimaryKeys = new HashMap<String, ArrayList<Column>>();
			
			for(int i=0; i < cvc.getResultsetTables().size(); i++){
	
				/** Get this data base table */
				Table table = cvc.getResultsetTables().get(i);
	
				/**Get table name */
				String tableName = table.getTableName();
	
				/**Get the primary keys of this table*/
				ArrayList<Column> primaryKeys = new ArrayList<Column>(table.getPrimaryKey());
				
				relationToPrimaryKeys.put(tableName, primaryKeys);
			}
			
			Map<String, Integer> totalTup = cvc.getNoOfOutputTuples();
			
			ArrayList<Integer> tupleCount = new ArrayList<Integer>();				
			
			Map<Integer, String> indexToTable = new HashMap<Integer, String>();
			Map<String, Integer> tableToIndex = new HashMap<String, Integer>(); 
			
			int j = 0;
			
			for(String s : relations){
				tupleCount.add(totalTup.get(s));
				indexToTable.put(j, s);
				tableToIndex.put(s, j);
				j++;
			}
			
			int numRelations = tupleCount.size();
			
			ArrayList<ArrayList<Integer>> combinations = new ArrayList<ArrayList<Integer>>();
			
		//	------------------generateCombinations(combinations, numRelations, tupleCount, new ArrayList<Integer>(), 0);
			
			System.out.print(relations);
			
			System.out.print(combinations);
			
			for(ArrayList<Integer> t: combinations) {
				Boolean process = false;
				
				for(int index = 0; index < t.size(); index++) {
					TupleRange temp = allowedTuples.get(indexToTable.get(index));
					
					String relation = indexToTable.get(index);
					
					if(temp != null && (t.get(index) < temp.start || t.get(index) > temp.end)){
						process = true;
						
						ArrayList<Column> primaryKeys = relationToPrimaryKeys.get(relation);
						
						String pkConstraint = "ASSERT ";
						
						for(int k = temp.start; k <= temp.end; k++){
						
							pkConstraint += "(";
							
							for(int p = 0; p < primaryKeys.size(); p++){
		
								Column pkeyColumn = primaryKeys.get(p);
								
								int pos = pkeyColumn.getTable().getColumnIndex(pkeyColumn.getColumnName());
			
								pkConstraint += "(O_" + relation + "[" + k + "]." + pos + " /= O_" + relation + "[" + t.get(index) +"]." + pos + ") OR ";						
							}
							
							pkConstraint = pkConstraint.substring(0,pkConstraint.length()-4);
							pkConstraint += ") AND ";
						}
						
						pkConstraint = pkConstraint.substring(0,pkConstraint.length()-5);
						pkConstraint += ";";
						orPrimaryKeyConstraints.add(pkConstraint);
					}
				}
				
				if(process) {
					for(int index = 0; index < t.size(); index++){
						String relation = indexToTable.get(index);
						
						Vector<Node> selCondsRel = selConds.get(relation);
						
						/**get negative conditions for these nodes*/
				//------------------------		Vector<Node> negativeSelConds = GenerateCVCConstraintForNode.getNegativeConditions(selCondsRel);
	
						/**Generate constraints for the negative conditions*/
				//		for(int i = 0; i < negativeSelConds.size(); i++){
			//	-----------------------			orConstraints.add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeSelConds.get(i), t.get(index))+";" +"\n" );
			//			}
						
						Vector<Node> ec = joinConds.get(relation);
						
						if(ec == null)
							continue;
						
						Node relationNode = null;
						
						for(int k = 0; k < ec.size(); k++){
							if(ec.get(k).getTable().getTableName().equals(relation)){
								relationNode = ec.get(k);
								break;
							}
						}
						
						for(int k=0;k<ec.size(); k++)
						{						
							Node ece = ec.get(k);
							if(ece.equals(relationNode))
								continue;
							int tupleIndex1 = 0;
							if(tableToIndex.get(ece.getTable().getTableName()) != null){
								tupleIndex1 = t.get(tableToIndex.get(ece.getTable().getTableName()));
							}
							int tupleIndex2 = 0;
							if(tableToIndex.get(relationNode.getTable().getTableName()) != null){
								tupleIndex2 = t.get(tableToIndex.get(relationNode.getTable().getTableName()));
							}
				//--------------------			orConstraints.add(GenerateJoinPredicateConstraints.genNegativeCondsEqClassForTuplePair(cvc, queryBlock, ece, relationNode, tupleIndex1, tupleIndex2));
						}
					}
				}
				
				String pkConst = "";
				String constraints = "";
				
				if(!orPrimaryKeyConstraints.isEmpty() && orPrimaryKeyConstraints.size() != 0){
			//	---------------	pkConst = processOrConstraintsNotExists(orPrimaryKeyConstraints);				
					System.out.print(pkConst);
					orPrimaryKeyConstraints.clear();
				}
				
				if(!orConstraints.isEmpty() && orConstraints.size() != 0){
			//	-------------	constraints = processOrConstraintsNotExists(orConstraints);
					orConstraints.clear();
				}
				
			//	if(process)
			//	---------------	constraintString += processImpliedConstraints(pkConst, constraints);
						
			}		
			
			return constraintString;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}
	
	private static String processImpliedConstraints(String left, String right){
		String result = "";
		
		left = left.replaceFirst("ASSERT ", "");
		left = left.replace(";", "");
		right = right.replaceFirst("ASSERT ", "");
		right = right.replace(";", "");
		
		result = "ASSERT " + left + " => " + right + ";\n";
		
		return result;
	}
	
	private static String processOrConstraintsNotExists(Vector<String> OrConstraints){

		String str = "ASSERT ";

		for(String constraint: OrConstraints)
			if( constraint.length() != 0) {
				int index = constraint.indexOf(";");
				String temp = constraint.substring(6, index);
				str += "(" + temp.trim() + ") OR ";
			}

		str = str.substring(0,str.length()-4);
		str+=";";

		return str;
	}
	
	
	
	// Flattens the hierarchial tree
	private static void flattenTree(RelationHierarchyNode hNode, ArrayList<String> relations){
		if(hNode == null)
			return;
		
		if(hNode.getNodeType().equals("_RELATION_")){
			relations.add(hNode.getTableName());
		} else if(hNode.getNodeType().equals("_LEFT_JOIN_")){
			flattenTree(hNode.getLeft(), relations);
		} else if(hNode.getNodeType().equals("_RIGHT_JOIN_")){
			flattenTree(hNode.getRight(), relations);
		} else {
			flattenTree(hNode.getLeft(), relations);
			flattenTree(hNode.getRight(), relations);
		}
	}
	
	private static void getAllRelations(RelationHierarchyNode root, ArrayList<String> relations){
		if(root == null)
			return;
		
		if(root.getNodeType().equals("_RELATION_")){
			relations.add(root.getTableName());
		} else {
			flattenTree(root.getLeft(), relations);
			flattenTree(root.getRight(), relations);
		}
	}
	
	
	

	
	
}