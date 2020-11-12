package generateConstraints;

import generateConstraints.TupleRange;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.JoinTreeNode;
import parsing.Node;
import parsing.RelationHierarchyNode;
import parsing.Table;
import testDataGen.PopulateTestData;
import testDataGen.GenerateCVC1;
import testDataGen.GenerateDataset_new;
import testDataGen.QueryBlockDetails;
import util.Configuration;
import util.ConstraintObject;
import util.Utilities;

/**
 * Generates constraints related to null conditions and database constraints
 * @author mahesh
 *
 */
public class GenerateCommonConstraintsForQuery {

	private static Logger logger=Logger.getLogger(GenerateCommonConstraintsForQuery.class.getName());
	/**
	 * This method generates the null constraints and database constaints
	 * like primary key, foreign key constraints. This method also changes 
	 * the noOfOutputTuples parameter in cvc based on foreign key relations
	 * 
	 * @param cvc
	 * @param unique
	 * @throws Exception
	 */
	public static void generateNullandDBConstraints(GenerateCVC1 cvc, Boolean unique) throws Exception {

		try{
			/** Add null constraints for the query */
			getNullConstraintsForQuery(cvc);

			if( cvc.getCVCStr() == null)
				cvc.setCVCStr("");
			String CVCStr = cvc.getCVCStr();

			/**Add constraints related to database */
			CVCStr += AddDataBaseConstraints.addDBConstraints(cvc);

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
	 * 
	 * @param cvc Object used for generating constraints
	 * @param unique Whether all string variables have different values or not
	 * @return If data generation was successful or not 
	 * @throws Exception
	 */
	public static boolean generateDataSetForConstraints(GenerateCVC1 cvc, Boolean unique) throws Exception{
		try{

			if( cvc.getCVCStr() == null)
				cvc.setCVCStr("");
			String CVCStr = cvc.getCVCStr();

			/** Solve the string constraints for the query */
			if(!cvc.getStringConstraints().isEmpty()) {
				cvc.getConstraints().add( ConstraintGenerator.addCommentLine("TEMP VECTOR CONSTRAINTS"));
				//To be added after String constraints issues for SMTLIB are sorted out
				//Vector<String> tempVector = cvc.getStringSolver().solveOrConstraints( new Vector<String>(cvc.getStringConstraints()), cvc.getResultsetColumns(), cvc.getTableMap());		
				//cvc.getConstraints().addAll(tempVector);				
			}

			cvc.setCVCStr(CVCStr);
			/** Add constraints, if there are branch queries*/
			if( cvc.getBranchQueries().getBranchQuery() != null)
			{
				cvc.getConstraints().add( ConstraintGenerator.addCommentLine("BRANCHQUERY CONSTRAINTS"));
				cvc.getConstraints().add( GenerateConstraintsRelatedToBranchQuery.addBranchQueryConstraints(cvc));
				cvc.getConstraints().add( ConstraintGenerator.addCommentLine("END OF BRANCHQUERY CONSTRAINTS"));
			}

			if(cvc.getOuterBlock().isConstrainedAggregation())
				cvc.getConstraints().add(addNoExtraTuple(cvc));		

			for(int k=0; k < cvc.getConstraints().size(); k++){
				CVCStr += "\n" + cvc.getConstraints().get(k);
			}


			/** Add not null constraints */
			cvc.getConstraints().add( ConstraintGenerator.addCommentLine(" NOT NULL CONSTRAINTS"));
			//CVCStr += GenerateCVCConstraintForNode.cvcSetNotNull(cvc);
			/* Removing NUll enumerations*/
			if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3"))
			{	
				CVCStr += GenerateCVCConstraintForNode.solverSetNotNull(cvc);
			}
			else
				CVCStr += GenerateCVCConstraintForNode.primaryKeysSetNotNull(cvc);
			cvc.setDatatypeColumns( new ArrayList<String>() );
			String CVC3_HEADER = GetSolverHeaderAndFooter.generateSolver_Header(cvc, unique);

			/** add mutation type and CVC3 header*/
			CVCStr =  ConstraintGenerator.addCommentLine("MUTATION TYPE: " +cvc.getTypeOfMutation()) + CVC3_HEADER + CVCStr;

			CVCStr += GetSolverHeaderAndFooter.generateSolver_Footer(cvc);
			cvc.setCVCStr(CVCStr);

			/** Add extra tuples to satisfy branch queries constraints*/
			for(int i = 0; i < cvc.getBranchQueries().getNoOfBranchQueries(); i++){

				HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[] = cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries();

				for(Table tempTab : noOfTuplesAddedToTablesForBranchQueries[i].keySet())

					cvc.getNoOfOutputTuples().put(tempTab.getTableName(), cvc.getNoOfOutputTuples().get(tempTab.getTableName()) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab));
			}
			Boolean success = false;
			if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){

				/** Call CVC3 Solver with constraints */
				logger.log(Level.INFO,"cvc count =="+cvc.getCount());
				Utilities.writeFile(Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/cvc3_" + cvc.getCount() + ".smt", CVCStr);

				success= new PopulateTestData().killedMutants("cvc3_" + cvc.getCount() 
				+ ".smt", cvc.getQuery(), 
				"DS" + cvc.getCount(), cvc.getQueryString(), cvc.getFilePath(), cvc.getNoOfOutputTuples(), cvc.getTableMap(), 
				cvc.getResultsetColumns(), cvc.getRepeatedRelationCount().keySet(),cvc.getDBAppparams()) ;

				/*	Boolean success= new PopulateTestData().killedMutants("cvc3_" + cvc.getCount() 
					+ ".cvc", cvc.getQuery(), 
					"DS" + cvc.getCount(), cvc.getQueryString(), cvc.getFilePath(), cvc.getNoOfOutputTuples(), cvc.getTableMap(), 
					cvc.getResultsetColumns(), cvc.getRepeatedRelationCount().keySet(),cvc.getDBAppparams()) ;
				 */
				cvc.setOutput( cvc.getOutput() + success);
				cvc.setCount(cvc.getCount() + 1);
			}else{
				/** Call CVC3 Solver with constraints */
				logger.log(Level.INFO,"cvc count =="+cvc.getCount());

				Utilities.writeFile(Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/z3_" + cvc.getCount() + ".smt", CVCStr);
				
				success= new PopulateTestData().killedMutantsForSMT("z3_" + cvc.getCount() 
				+ ".smt", cvc.getQuery(), 
				"DS" + cvc.getCount(), cvc.getQueryString(), cvc.getFilePath(), cvc.getNoOfOutputTuples(), cvc.getTableMap(), 
				cvc.getResultsetColumns(), cvc.getRepeatedRelationCount().keySet(),cvc.getDBAppparams()) ;
				cvc.setOutput( cvc.getOutput() + success);
				cvc.setCount(cvc.getCount() + 1);
			}
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

	public static boolean generateDataSetForConstraints(GenerateCVC1 cvc) throws Exception{
		try {
			generateNullandDBConstraints(cvc,false);
			return generateDataSetForConstraints(cvc, false);
		} catch (TimeoutException e) {
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		} catch(Exception e) {
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
			cvc.getConstraints().add(  ConstraintGenerator.addCommentLine("NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY"));
			cvc.getConstraints().add( getNullCOnstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );
			cvc.getConstraints().add(  ConstraintGenerator.addCommentLine("END OF NULL CONSTRAINTS FOR OUTER BLOCK OF QUERY"));

			/**Generate null constraints for each from clause sub query block */
			for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getFromClauseSubQueries()){

				cvc.getConstraints().add(  ConstraintGenerator.addCommentLine("NULL CONSTRAINTS FOR FROM CLAUSE NESTED SUBQUERY BLOCK"));
				cvc.getConstraints().add( getNullCOnstraintsForQueryBlock(cvc, queryBlock) );
				cvc.getConstraints().add(  ConstraintGenerator.addCommentLine("END OF NULL CONSTRAINTS FOR FROM CLAUSE NESTED SUBQUERY BLOCK"));
			}

			/**Generate null constraints for each where clause sub query block */
			for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getWhereClauseSubQueries()){

				cvc.getConstraints().add(  ConstraintGenerator.addCommentLine("NULL CONSTRAINTS FOR WHERE CLAUSE NESTED SUBQUERY BLOCK"));
				cvc.getConstraints().add( getNullCOnstraintsForQueryBlock(cvc, queryBlock) );
				cvc.getConstraints().add(  ConstraintGenerator.addCommentLine("END OF NULL CONSTRAINTS FOR WHERE CLAUSE NESTED SUBQUERY BLOCK"));
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
	public static String getNullCOnstraintsForQueryBlock(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{

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
							ConstraintString += GenerateCVCConstraintForNode.cvcSetNull(cvc, n.getLeft().getColumn(), (offset+i)+"");

				}
			}
			return ConstraintString;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}

	// Generates Constraints for ensuring that no extra tuples are generated
	public static String generateConstraintsForNoExtraTuples(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ArrayList<Node> additionalSelConds, Map<String, TupleRange> allowedTuples) {
		String constraintString = "";

		ArrayList<ConstraintObject> orConstraints=new ArrayList<ConstraintObject>();
		Vector<ArrayList<ConstraintObject>> orConstraintsVector = new Vector<ArrayList<ConstraintObject>>();

		//Vector<String> orPrimaryKeyConstraints = new Vector<String>();
		Vector<ArrayList<ConstraintObject>> orPrimaryKeyConstraints = new Vector<ArrayList<ConstraintObject>>();

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

			generateCombinations(combinations, numRelations, tupleCount, new ArrayList<Integer>(), 0);

			ConstraintGenerator constraintGenerator = new ConstraintGenerator();
			ConstraintObject con = new ConstraintObject();
			ConstraintObject constr1 = new ConstraintObject();
			String impliedCond = "" ;
			ArrayList<ConstraintObject> conList = new ArrayList<ConstraintObject>();
			ArrayList<ConstraintObject> constrList1 = new ArrayList<ConstraintObject>();
			int pkConstraintIndex = 1;

			for(ArrayList<Integer> t: combinations) {
				Boolean process = false;

				for(int index = 0; index < t.size(); index++) {
					TupleRange temp = allowedTuples.get(indexToTable.get(index));

					String relation = indexToTable.get(index);

					if(temp != null && (t.get(index) < temp.start || t.get(index) > temp.end)){
						process = true;

						ArrayList<Column> primaryKeys = relationToPrimaryKeys.get(relation);

						String pkConstraint = "";// "ASSERT ";

						for(int k = temp.start; k <= temp.end; k++){

							//pkConstraint += "(";

							for(int p = 0; p < primaryKeys.size(); p++){

								con = new ConstraintObject();

								Column pkeyColumn = primaryKeys.get(p);

								int pos = pkeyColumn.getTable().getColumnIndex(pkeyColumn.getColumnName());

								//pkConstraint += "(O_" + relation + "[" + k + "]." + pos + " /= O_" + relation + "[" + t.get(index) +"]." + pos + ") OR ";		

								con = constraintGenerator.getConstraint(relation,Integer.valueOf(k),Integer.valueOf(pos),
										relation,Integer.valueOf(t.get(index)),Integer.valueOf(pos),
										pkeyColumn,pkeyColumn,"/=");
								//get primary Key constraint and add to constraint list
								if(! conList.contains(con)){
									conList.add(con);
								}

							}

							constraintString =constraintGenerator.generateOrConstraintsWithAssert(conList);
							constr1 = new ConstraintObject();
							constr1.setLeftConstraint(constraintString);
							//constraintString = constraintString.substring(0,pkConstraint.length()-4);
							constrList1.add(constr1);
							//pkConstraint += ") AND ";
						}

						pkConstraint = constraintGenerator.generateANDConstraintsWithAssert(constrList1);

						//pkConstraint = pkConstraint.substring(0,pkConstraint.length()-5);
						//pkConstraint += ";";
						//orPrimaryKeyConstraints.add(pkConstraint);
						orPrimaryKeyConstraints.add(constrList1);
						pkConstraintIndex++;
					}
				}

				if(process) {
					for(int index = 0; index < t.size(); index++){
						String relation = indexToTable.get(index);

						Vector<Node> selCondsRel = selConds.get(relation);

						/**get negative conditions for these nodes*/
						Vector<Node> negativeSelConds = GenerateCVCConstraintForNode.getNegativeConditions(selCondsRel);

						/**Generate constraints for the negative conditions*/
						for(int i = 0; i < negativeSelConds.size(); i++){
							//	orConstraints.add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeSelConds.get(i), t.get(index))+";" +"\n" );
							ConstraintObject constrObj = new ConstraintObject();
							constrObj.setLeftConstraint(constraintGenerator.genPositiveCondsForPredAsString(cvc,queryBlock, negativeSelConds.get(i), t.get(index)));
							orConstraints.add(constrObj);
						}
						orConstraintsVector.add(orConstraints);
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
							ConstraintObject constrObj = new ConstraintObject();
							constrObj.setLeftConstraint(GenerateJoinPredicateConstraints.genNegativeCondsEqClassForTuplePair(cvc, queryBlock, ece, relationNode, tupleIndex1, tupleIndex2));

							orConstraints.add(constrObj);
						}
						orConstraintsVector.add(orConstraints);
					}
				}

				String pkConst = "";
				String constraints = "";

				if(!orPrimaryKeyConstraints.isEmpty() && orPrimaryKeyConstraints.size() != 0){
					pkConst = processOrConstraintsNotExists(cvc,orPrimaryKeyConstraints);				
					orPrimaryKeyConstraints.clear();
				}

				if(!orConstraints.isEmpty() && orConstraints.size() != 0){
					constraints = processOrConstraintsNotExists(cvc,orConstraintsVector);
					orConstraints.clear();
				}

				if(process){
					if(pkConst != null && !pkConst.isEmpty() && constraints != null && !constraints.isEmpty() ){
						constraintString += constraintGenerator.processImpliedConstraints(pkConst, constraints);
					}
				}
			}		

			return constraintString;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
	}

	/*private static String processImpliedConstraints(String left, String right){
		String result = "";

		left = left.replaceFirst("ASSERT ", "");
		left = left.replace(";", "");
		right = right.replaceFirst("ASSERT ", "");
		right = right.replace(";", "");

		result = "ASSERT " + left + " => " + right + ";\n";

		return result;
	}*/

	private static String addNoExtraTuple(GenerateCVC1 cvc){
		ArrayList<Node> sel = new ArrayList<Node>();
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();

		ArrayList<Node> groupByNodes = cvc.outerBlock.getGroupByNodes();
		for(Node n : groupByNodes){
			String relation = n.getTable().getTableName();
			TupleRange t = cvc.getTupleRange().get(relation);			
			Node newNode = new Node();
			newNode.setLeft(n);
			newNode.setType(Node.getBroNodeType());
			Node rightCond = new Node(); 
			rightCond.setStrConst(constraintGenerator.getMapNode(n, (t != null?t.start:0) + ""));
			rightCond.setType(Node.getValType());
			newNode.setRight(rightCond);
			newNode.setOperator("=");
			sel.add(newNode);
		}

		return GenerateCommonConstraintsForQuery.generateConstraintsForNoExtraTuples(cvc, cvc.getOuterBlock(), sel, cvc.getTupleRange());
	}

	// Generates all the combinations of the tuples
	private static void generateCombinations(ArrayList<ArrayList<Integer>> combinations, int numRelations, ArrayList<Integer> tupleCount, ArrayList<Integer> temp, int relationIndex){
		if(temp.size() == numRelations){
			combinations.add(new ArrayList<Integer>(temp));
			temp = new ArrayList<Integer>();
			return;
		}

		int c = tupleCount.get(relationIndex);

		for(int j = 1; j <= c; j++){
			temp.add(j);
			generateCombinations(combinations, numRelations, tupleCount, temp, relationIndex + 1);
			temp.remove(relationIndex);
		}
	}

	private static String processOrConstraintsNotExists(GenerateCVC1 cvc, Vector<ArrayList<ConstraintObject>> OrConstraints){

		String str = "";//"ASSERT ";
		ConstraintGenerator constrGen = new ConstraintGenerator();
		for(ArrayList constraint: OrConstraints){

			//for(ConstraintObject con : constraint){
			//		str += "(" + temp.trim() + ") OR ";
			//}
			str = constrGen.generateOrConstraintsWithAssert(constraint);
		}
		//str = str.substring(0,str.length()-4);
		//str+=";";

		return str;
	}

	private static JoinTreeNode getTheJoinNode(Node relation, JoinTreeNode root){		
		if(root.getNodeType().equals("RELATION")){
			return null;
		}

		JoinTreeNode left = root.getLeft();
		JoinTreeNode right = root.getRight();

		JoinTreeNode leftRes = null, rightRes = null;

		if(left != null && left.getNodeType().equals("RELATION")){
			if(left.getTableNameNo().equals(relation.getTableNameNo())){
				leftRes = root;
			}
		}
		else{
			leftRes = getTheJoinNode(relation, left);
		}

		if(right != null && right.getNodeType().equals("RELATION")){
			if(right.getTableNameNo().equals(relation.getTableNameNo())){
				rightRes = root;
			}
		}
		else{
			rightRes = getTheJoinNode(relation, right);
		}

		if(leftRes == null && rightRes == null)
			return null;

		if(leftRes != null)
			return leftRes;
		else
			return rightRes;

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
