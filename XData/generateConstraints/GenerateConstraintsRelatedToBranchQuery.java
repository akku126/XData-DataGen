package generateConstraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Query;
import parsing.QueryParser;
import parsing.Table;
import testDataGen.DataType;
import testDataGen.GenerateCVC1;
import util.ConstraintObject;

/**
 * Adds constraints related to branch query. This is Biplab's Work
 * @author mahesh
 *
 */
public class GenerateConstraintsRelatedToBranchQuery {

	private static Logger logger = Logger.getLogger(GenerateConstraintsRelatedToBranchQuery.class.getName());
	public static String addBranchQueryConstraints( GenerateCVC1 cvc) throws Exception{

		String temp = "";

		int noOfBranchQueries = cvc.getBranchQueries().getNoOfBranchQueries();
		HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[] = cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries();		
		ArrayList<String>[] branchResultString = cvc.getBranchQueries().getBranchResultString();
		ArrayList<String>[] branchOperators = cvc.getBranchQueries().getBranchOperators();
		ArrayList<Node> stringSelectionCondsForBranchQuery[] = cvc.getBranchQueries().getStringSelectionCondsForBranchQuery();
		ArrayList<Node> selectionCondsForBranchQuery[] =  cvc.getBranchQueries().getSelectionCondsForBranchQuery();
		ArrayList<Node> allCondsForBranchQuery[] = cvc.getBranchQueries().getAllCondsForBranchQuery();
		ArrayList<Node> isNullCondsForBranchQuery[] = cvc.getBranchQueries().getIsNullCondsForBranchQuery();
		ArrayList<Node> likeCondsForBranchQuery[] = cvc.getBranchQueries().getLikeCondsForBranchQuery();



		HashMap<Table, Integer> tempTuplesAdded = new HashMap<Table, Integer>();

		Vector<Node> projectedColBranchQuery[] = new Vector[noOfBranchQueries];
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		
		/** Adding constraints for results of branch query*/
		for(int i = 0; i < noOfBranchQueries; i++)
			projectedColBranchQuery[i] = cvc.getBranchQueries().getqParser1()[i].getProjectedCols();

		tempTuplesAdded = new HashMap<Table, Integer>();

		for(int i = 0; i < noOfBranchQueries; i++)
			for(Table table : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
				tempTuplesAdded.put(table, 0);

		for(int i = 0; i < noOfBranchQueries; i++){

			for(int j = 0; j < branchResultString[i].size(); j++){

				if(!branchResultString[i].get(j).equals("NULL")){

					if(projectedColBranchQuery[i].get(j).getType().equals(Node.getColRefType())){

						Column tempCol = projectedColBranchQuery[i].get(j).getColumn();
						Table tempTable = tempCol.getTable();

						if(new DataType().getDataType(tempCol.getDataType()) >= 3){

							for(int k = 1; k <= noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable); k++){

								Node tempNode1 = new Node();
								tempNode1.setType(Node.getColRefType());
								tempNode1.setTable(tempTable);
								tempNode1.setColumn(projectedColBranchQuery[i].get(j).getColumn());

								Node tempNode2 = new Node();
								tempNode2.setType(Node.getValType());
								tempNode2.setStrConst(branchResultString[i].get(j));

								Node tempNode = new Node();
								tempNode.setType(Node.getBroNodeType());

								if(branchOperators[i].get(j).equals("!=") || branchOperators[i].get(j).equals("<>"))
									tempNode.setOperator("!=");
								else
									tempNode.setOperator(branchOperators[i].get(j));

								tempNode.setLeft(tempNode1);
								tempNode.setRight(tempNode2);

								stringSelectionCondsForBranchQuery[i].add(tempNode);

							}							
						}
						else{

							for(int k = 1; k <= noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable); k++){
															
							//	temp += "\nASSERT (O_" + tempTable + "[" + ( cvc.getNoOfOutputTuples().get(tempTable.getTableName()) + k + tempTuplesAdded.get(tempTable)) + 
								//		"]." + tempTable.getColumnIndex(projectedColBranchQuery[i].get(j).getColumn().getColumnName()) + " " + branchOperators[i].get(j) +
								//		" " + branchResultString[i].get(j) + ");";
								
								temp +=  constraintGen.getStringConstraints(tempTable,  
										(cvc.getNoOfOutputTuples().get(tempTable.getTableName()) + k + tempTuplesAdded.get(tempTable)), 
										(tempTable.getColumnIndex(projectedColBranchQuery[i].get(j).getColumn().getColumnName())), 
										branchOperators[i].get(j), branchResultString[i].get(j), projectedColBranchQuery[i].get(j).getColumn());
							}
						}
					}
				}
			}

			for(Table table : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
				tempTuplesAdded.put(table, tempTuplesAdded.get(table) + noOfTuplesAddedToTablesForBranchQueries[i].get(table));
		}
		/** End of adding constraints for results of branch query*/

		for(int i = 0; i < noOfBranchQueries; i++)
			for(Table table : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
				tempTuplesAdded.put(table, 0);

		for(int i = 0; i < noOfBranchQueries; i++){

			for(Node selConds : selectionCondsForBranchQuery[i]){

				Table tempTab = selConds.getLeft().getTable();

				for(int j = 1; j <= noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab); j++){

					/**FIXME: MAhesh: Should specify query block here*/
					//temp += "\nASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(cvc, selConds, cvc.getNoOfOutputTuples().get(tempTab.getTableName()) + j + tempTuplesAdded.get(tempTab))+";\n";
				}
			}

			for(Node allConds : allCondsForBranchQuery[i]){

				Table tempTab = allConds.getLeft().getTable();

				for(int j = 1; j <= noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab); j++){

					/**FIXME: MAhesh: Should specify query block here*/
					//temp += "\nASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(cvc, allConds, cvc.getNoOfOutputTuples().get(tempTab.getTableName()) + j + tempTuplesAdded.get(tempTab))+";\n";
				}
			}

			ArrayList<String> stringConstraintsForBranchQuery = new ArrayList<String>();

			for(int k=0; k<stringSelectionCondsForBranchQuery[i].size(); k++){

				logger.log(Level.INFO,"stringSelectionCondsForBranchQuery[i] " + k + " " + stringSelectionCondsForBranchQuery[i].get(k) + stringSelectionCondsForBranchQuery[i].get(k).getRight().getStrConst());
				if(stringSelectionCondsForBranchQuery[i].get(k).getRight().getStrConst().charAt(1) == '?'){

					Node colDepn = stringSelectionCondsForBranchQuery[i].get(k);
					int projColNo = Integer.parseInt(colDepn.getRight().getStrConst().substring(2, colDepn.getRight().getStrConst().length() - 1));

					Column prevCol = projectedColBranchQuery[i - 1].get(projColNo - 1).getColumn();
					Table prevTable = prevCol.getTable();

					int prevTableStartIndex = cvc.getNoOfOutputTuples().get(prevTable.getTableName()) + tempTuplesAdded.get(prevTable) - noOfTuplesAddedToTablesForBranchQueries[i - 1].get(prevTable) + 1;

					for(int j = prevTableStartIndex + 1; j < prevTableStartIndex + noOfTuplesAddedToTablesForBranchQueries[i - 1].get(prevTable); j++){

						
						temp += constraintGen.getStringConstraints(prevTable, prevTableStartIndex, prevTable.getColumnIndex(prevCol.getColumnName()), "=",
								prevTable, j, prevTable.getColumnIndex(prevCol.getColumnName()),prevCol,prevCol);
					}	

					Column currCol = colDepn.getLeft().getColumn();
					Table currTable = colDepn.getLeft().getTable();
					String operand = "";
					int currTableStartIndex = cvc.getNoOfOutputTuples().get(currTable.getTableName()) + tempTuplesAdded.get(currTable) + 1;

					for(int j = currTableStartIndex; j < currTableStartIndex + noOfTuplesAddedToTablesForBranchQueries[i].get(currTable); j++){

						//temp += "\nASSERT ";

						//if(colDepn.getOperator().equals("<>") || colDepn.getOperator().equals("!=") || colDepn.getOperator().equals("/="))
							//temp += "NOT ";

						//temp += "(O_" + currTable.getTableName() + "[" + j + "]." + currTable.getColumnIndex(currCol.getColumnName());

						if(colDepn.getOperator().equals("<>") || colDepn.getOperator().equals("!=") || colDepn.getOperator().equals("/=")){
							//temp += " = ";
							operand ="!=";
							//temp += constraintGen.getStringConstraints(cvc, currTable, j, currTable.getColumnIndex(currCol.getColumnName()), operand,
							//		prevTable, prevTableStartIndex, prevTable.getColumnIndex(prevCol.getColumnName()),currCol,prevCol);							
						}

						else{
							//temp += " " + colDepn.getOperator() + " ";
						}

					//	temp += "O_" + prevTable.getTableName() + "[" + prevTableStartIndex + "]." + prevTable.getColumnIndex(prevCol.getColumnName()) + ");";
						
						temp += constraintGen.getStringConstraints(currTable, j, currTable.getColumnIndex(currCol.getColumnName()), operand,
										prevTable, prevTableStartIndex, prevTable.getColumnIndex(prevCol.getColumnName()),currCol,prevCol);	
						
					}

					stringSelectionCondsForBranchQuery[i].remove(k);

					k--;					
					continue;
				}

				Table table = stringSelectionCondsForBranchQuery[i].get(k).getLeft().getTable();

				int count = cvc.getNoOfOutputTuples().get(table.getTableName());

				for(int j = 1; j <= noOfTuplesAddedToTablesForBranchQueries[i].get(table); j++){

					/**FIXME: MAhesh: Should specify query block here*/
					//stringConstraintsForBranchQuery.add("ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(cvc, stringSelectionCondsForBranchQuery[i].get(k),count + j + tempTuplesAdded.get(table))+";\n");
				}
			}

			for(int k=1; k<=likeCondsForBranchQuery[i].size(); k++){

				Table table = likeCondsForBranchQuery[i].get(k).getLeft().getTable();
				int count = cvc.getNoOfOutputTuples().get(table.getTableName());

				for(int j = 1; j <= noOfTuplesAddedToTablesForBranchQueries[i].get(table); j++){

					/**FIXME: MAhesh: Should specify query block here*/
					//stringConstraintsForBranchQuery.add("ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(cvc, likeCondsForBranchQuery[i].get(k),count + j + tempTuplesAdded.get(table))+";\n");				
				}
			}

			Vector<String> tempVector;

			try {
				tempVector = cvc.getStringSolver().solveConstraints( new Vector<String>( stringConstraintsForBranchQuery ), cvc.getResultsetColumns(), cvc.getTableMap());

				for(String str : tempVector)
					temp += "\n" + str;

			} catch (Exception e1) {
				logger.log(Level.SEVERE,e1.getMessage(),e1);
				//e1.printStackTrace();
			}

			for(Table table : noOfTuplesAddedToTablesForBranchQueries[i].keySet())

				tempTuplesAdded.put(table, tempTuplesAdded.get(table) + noOfTuplesAddedToTablesForBranchQueries[i].get(table));
		}

		HashMap<Table, Integer> tempTuplesAddedForBranchQueries = new HashMap<Table, Integer>();

		for(int i = 0; i < noOfBranchQueries; i++){

			if(!noOfTuplesAddedToTablesForBranchQueries[i].isEmpty()){

				for(Table tempTab : noOfTuplesAddedToTablesForBranchQueries[i].keySet()){

					if(tempTuplesAddedForBranchQueries.containsKey(tempTab))

						tempTuplesAddedForBranchQueries.put(tempTab, tempTuplesAddedForBranchQueries.get(tempTab) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab));
					else

						tempTuplesAddedForBranchQueries.put(tempTab, noOfTuplesAddedToTablesForBranchQueries[i].get(tempTab));
				}
			}
		}

		/** Adding constraints so that group by column is different from primary query */
		temp += ConstraintGenerator.addCommentLine(" BRANCH QUERY CONSTRAINTS FOR DIFFERENT GROUP BY COLUMN FROM PRIMARY QUERY");

		Vector<Node> tempGroupByNodes = cvc.getqStructure().getGroupByNodes();

		if(tempGroupByNodes != null && tempGroupByNodes.size() > 0)
		{
			for(Node tempNode : tempGroupByNodes){

				Column tempGroupByCol = tempNode.getColumn();
				Table tempGroupByTable = tempGroupByCol.getTable();

				if(tempTuplesAddedForBranchQueries.containsKey(tempGroupByTable)){
				
					for(int j = 1; j <= cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()); j++){

						for(int k = cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + 1; k <= cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAddedForBranchQueries.get(tempGroupByTable); k++){

							temp += constraintGen.getStringConstraints(tempGroupByTable, j, tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()), "!=",
									tempGroupByTable, k,  tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()),tempGroupByCol,tempGroupByCol);
							
							
							//temp += "\nASSERT NOT(O_" + tempGroupByTable.getTableName() + "[" + j + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName());
							//temp += " = O_" + tempGroupByTable.getTableName() + "[" + k + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()) + ");";
						}
					}
				}
			}
		}
		/**End of adding constraints so that group by column is different from primary query*/

		/**Adding constraints so that group by column is different for tuples of different branch queries */
		temp += ConstraintGenerator.addCommentLine(" BRANCH QUERY CONSTRAINTS FOR DIFFERENT GROUP BY COLUMN FROM OTHER BRANCH QUERIES");

		tempTuplesAdded = new HashMap<Table, Integer>();

		for(Table table : cvc.getResultsetTables())
			tempTuplesAdded.put(table, 0);

		for(int i = 0; i < noOfBranchQueries; i++){

			Vector<Node> branchQueryGroupByNodes = cvc.getBranchQueries().getqParser1()[i].getGroupByNodes();

			if(branchQueryGroupByNodes != null && branchQueryGroupByNodes.size() > 0){

				for(Node tempNode : branchQueryGroupByNodes){

					Column tempGroupByCol = tempNode.getColumn();
					Table tempGroupByTable = tempGroupByCol.getTable();

					for(int j = 1; j <= cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable); j++){

						temp += constraintGen.getStringConstraints(tempGroupByTable, 
								 (cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + 1) 
								 , tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()), "!=",
								tempGroupByTable, j,  tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()),tempGroupByCol,tempGroupByCol);
						
						
						//temp += "\nASSERT NOT(O_" + tempGroupByTable.getTableName() + "[" + (cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + 1) + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName());
						//temp += " = O_" + tempGroupByTable.getTableName() + "[" + j + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()) + ");";
					}

					for(int j = cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempGroupByTable) + 1; j <= tempTuplesAddedForBranchQueries.get(tempGroupByTable); j++){

						temp += constraintGen.getStringConstraints(tempGroupByTable, 
								(cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + 1) 
								 , tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()), "!=",
								tempGroupByTable, j,  tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()),tempGroupByCol,tempGroupByCol);
						
						
						//temp += "\nASSERT NOT(O_" + tempGroupByTable.getTableName() + "[" + (cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + 1) + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName());
						//temp += " = O_" + tempGroupByTable.getTableName() + "[" + j + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()) + ");";
					}
				}
			}

			for(Table table : noOfTuplesAddedToTablesForBranchQueries[i].keySet())

				tempTuplesAdded.put(table, tempTuplesAdded.get(table) + noOfTuplesAddedToTablesForBranchQueries[i].get(table));
		}
		/**End of adding constraints so that group by column is different for tuples of different branch queries*/


		/**Adding constraints so that group by column is same for all tuples of branch query*/
		temp += ConstraintGenerator.addCommentLine(" BRANCH QUERY CONSTRAINTS FOR SAME GROUP BY COLUMN OF A BRANCH QUERY");

		tempTuplesAdded = new HashMap<Table, Integer>();

		for(Table table : cvc.getResultsetTables())
			tempTuplesAdded.put(table, 0);

		for(int i = 0; i < noOfBranchQueries; i++){

			Vector<Node> branchQueryGroupByNodes = cvc.getBranchQueries().getqParser1()[i].getGroupByNodes();

			if(branchQueryGroupByNodes != null && branchQueryGroupByNodes.size() > 0){

				for(Node tempNode : branchQueryGroupByNodes){

					Column tempGroupByCol = tempNode.getColumn();
					Table tempGroupByTable = tempGroupByCol.getTable();

					for(int j = cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + 2; j <= cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempGroupByTable); j++){

						temp += constraintGen.getStringConstraints(tempGroupByTable, 
								( cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + 1)
								 , tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()), "=",
								tempGroupByTable, j,  tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()),tempGroupByCol,tempGroupByCol);
						
						
						//temp += "\nASSERT (O_" + tempGroupByTable.getTableName() + "[" + ( cvc.getNoOfOutputTuples().get(tempGroupByTable.getTableName()) + tempTuplesAdded.get(tempGroupByTable) + 1) + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName());
						//temp += " = O_" + tempGroupByTable.getTableName() + "[" + j + "]." + tempGroupByTable.getColumnIndex(tempGroupByCol.getColumnName()) + ");";
					}
				}
			}

			for(Table table : noOfTuplesAddedToTablesForBranchQueries[i].keySet())

				tempTuplesAdded.put(table, tempTuplesAdded.get(table) + noOfTuplesAddedToTablesForBranchQueries[i].get(table));
		}
		/**End of adding constraints so that group by column is same for all tuples of branch query*/


		/** Adding constraints for aggregation fns of branch query */
		temp += ConstraintGenerator.addCommentLine(" BRANCH QUERY GROUP BY CONSTRAINTS");

		tempTuplesAdded = new HashMap<Table, Integer>();

		for(Table table : cvc.getResultsetTables())
			tempTuplesAdded.put(table, 0);

		for(int i = 0; i < noOfBranchQueries; i++){

			if(cvc.getBranchQueries().getqParser1()[i].getHavingClause() != null){

				Node tempHavingClause = cvc.getBranchQueries().getqParser1()[i].getHavingClause();
				Vector<Node> tempHavingClauseArray = new Vector<Node>();

				while(tempHavingClause.getOperator().equals("AND"))	{

					tempHavingClauseArray.add(tempHavingClause.getRight());
					tempHavingClause = tempHavingClause.getLeft();
				}

				tempHavingClauseArray.add(tempHavingClause);

				for(Node havingClause : tempHavingClauseArray){

					Table tempHavingTable = havingClause.getLeft().getAgg().getAggExp().getTable();
					int tempHavingColIndex = tempHavingTable.getColumnIndex(havingClause.getLeft().getAgg().getAggExp().getColumn().getColumnName());

					int tempHavingVal = Integer.parseInt(havingClause.getRight().getStrConst());
					String tempHavingOp = havingClause.getOperator();

					if(havingClause.getLeft().getAgg().getFunc().equals("MIN"))	{

						temp += constraintGen.getStringConstraints(tempHavingTable, 
								(cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + 
										tempTuplesAdded.get(tempHavingTable) + 1) 
								 , tempHavingColIndex, tempHavingOp,Integer.toString(tempHavingVal),havingClause.getLeft().getAgg().getAggExp().getColumn());
					
						//temp += "\nASSERT (O_" + tempHavingTable.getTableName() + "[" + (cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + 
						//		tempTuplesAdded.get(tempHavingTable) + 1) + "]." + tempHavingColIndex;
						//temp += " " + tempHavingOp + " " + tempHavingVal + ");";

						for(int j = cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + 2; j <= cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable); j++){


							temp += constraintGen.getStringConstraints(tempHavingTable, 
									(cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + 1)
									 , tempHavingColIndex, " <= ",tempHavingTable, j, tempHavingColIndex, havingClause.getLeft().getAgg().getAggExp().getColumn(),havingClause.getLeft().getAgg().getAggExp().getColumn());
							
						//	temp += "\nASSERT (O_" + tempHavingTable.getTableName() + "["
						//	+ (cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + 1) + "]." + tempHavingColIndex;
						//	temp += " <= O_" + tempHavingTable.getTableName() + "[" + j + "]." + tempHavingColIndex + ");";
						}
					}

					if(havingClause.getLeft().getAgg().getFunc().equals("MAX")){

						temp += constraintGen.getStringConstraints(tempHavingTable, 
								(cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable))
								 , tempHavingColIndex, tempHavingOp,Integer.toString(tempHavingVal),havingClause.getLeft().getAgg().getAggExp().getColumn());
				
						
					//	temp += "\nASSERT (O_" + tempHavingTable.getTableName() + "[" 
					//			+ (cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable)) + "]." 
					//			+ tempHavingColIndex;
					//	temp += " " + tempHavingOp + " " + tempHavingVal + ");";

						for(int j = cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + 1; j < cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable); j++){

							temp += constraintGen.getStringConstraints(tempHavingTable, 
									(cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable))
									 , tempHavingColIndex, "  >=  ",tempHavingTable, j, tempHavingColIndex, havingClause.getLeft().getAgg().getAggExp().getColumn(),havingClause.getLeft().getAgg().getAggExp().getColumn());
						
							
							//temp += "\nASSERT (O_" + tempHavingTable.getTableName() + "[" 
							//		+ (cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable)) + "]." 
							//		+ tempHavingColIndex;
							//temp += " >= O_" + tempHavingTable.getTableName() + "[" + j + "]." + tempHavingColIndex + ");";
						}
					}

					if(havingClause.getLeft().getAgg().getFunc().equals("SUM")){

						//temp += "\nASSERT (";
						String constraint = "";
						for(int j = cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + 1; j <= cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable); j++)
							constraint = constraintGen.getConstraintsForSUMWithAssert(tempHavingOp, Integer.toString(tempHavingVal), constraint, tempHavingTable, j, tempHavingColIndex,havingClause.getLeft().getAgg().getAggExp().getColumn());
							//temp += "O_" + tempHavingTable.getTableName() + "[" + j + "]." + tempHavingColIndex + " + ";
						
					//	temp = temp.substring(0, temp.length() - 3);
					//	temp += " " + tempHavingOp + " " + tempHavingVal + ");";
						temp += constraint;
						
					}

					if(havingClause.getLeft().getAgg().getFunc().equals("AVG")){

						//temp += "\nASSERT ((";
						String constraint = "";
						for(int j = cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + 1; j <= cvc.getNoOfOutputTuples().get(tempHavingTable.getTableName()) + tempTuplesAdded.get(tempHavingTable) + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable); j++)
							constraint = constraintGen.getConstraintsForSUMWithoutAssert(tempHavingOp, Integer.toString(tempHavingVal), constraint, tempHavingTable, j, tempHavingColIndex,havingClause.getLeft().getAgg().getAggExp().getColumn());
							//temp += "O_" + tempHavingTable.getTableName() + "[" + j + "]." + tempHavingColIndex + " + ";

						//temp = temp.substring(0, temp.length() - 3);
						//temp += ") / " + noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable);
						//temp += " " + tempHavingOp + " " + tempHavingVal;
						
						temp += constraintGen.getConstraintsForAVG(constraint,noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable),
																	tempHavingOp,Integer.toString(tempHavingVal),havingClause.getLeft().getAgg().getAggExp().getColumn());
					}
				}
			}

			for(Table table : noOfTuplesAddedToTablesForBranchQueries[i].keySet())
				tempTuplesAdded.put(table, tempTuplesAdded.get(table) + noOfTuplesAddedToTablesForBranchQueries[i].get(table));
		}
		/**End of adding constraints for aggregation fns of branch query*/

		/** Added for primary key constraints of branchQuery*/
		temp += ConstraintGenerator.addCommentLine(" PRIMARY KEY CONSTRAINTS FOR BRANCH QUERY");
		
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		ConstraintObject con = new ConstraintObject();
		String impliedCond = "" ;
		ArrayList<ConstraintObject> conList = new ArrayList<ConstraintObject>();
		String 	constraintString = "";
		
		ArrayList<ConstraintObject> impliedConstraintList = new ArrayList<ConstraintObject>();
		ConstraintObject  impliedConstraint = new ConstraintObject();
		String 	impliedConstraintString = "";
		
		ConstraintObject primaryKeyConstraint = new ConstraintObject();
		
		for(Table table : tempTuplesAddedForBranchQueries.keySet()){

			int noOfTuples = cvc.getNoOfOutputTuples().get(table.getTableName());

			for(int i = 1; i <= tempTuplesAddedForBranchQueries.get(table); i++){

				for(int j = 1; j < noOfTuples + i; j++){

				/*	temp += "\nASSERT (";

					for(Column tempPKCol : table.getPrimaryKey()){

						temp += "(O_" + table.getTableName() + "[" + (noOfTuples + i) + "]." + table.getColumnIndex(tempPKCol.getColumnName());
						temp += " = O_" + table.getTableName() + "[" + j + "]." + table.getColumnIndex(tempPKCol.getColumnName()) + ") AND ";
					}

					temp = temp.substring(0, temp.length() - 5);
					temp += ") => (";

					for(Column tempCol : table.getColumns().values())
						if(!table.getPrimaryKey().contains(tempCol)){

							temp += "(O_" + table.getTableName() + "[" + (noOfTuples + i) + "]." + table.getColumnIndex(tempCol.getColumnName());
							temp += " = O_" + table.getTableName() + "[" + j + "]." + table.getColumnIndex(tempCol.getColumnName()) + ") AND ";
						}

					temp = temp.substring(0, temp.length() - 5);
					temp += ");";
					
					*/
					
					
					/**Generate the constraint for each primary key attribute */					
					for(int p=0; p<table.getPrimaryKey().size();p++){
						
						con = new ConstraintObject();
						impliedConstraint = new ConstraintObject();
						/** Get column details */
						Column pkeyColumn = table.getPrimaryKey().get(p);
						int pos = table.getColumnIndex(pkeyColumn.getColumnName());

						/**If this pk attribute is equal*/
						
						con = constraintGenerator.getConstraint(table.getTableName(),Integer.valueOf(j),Integer.valueOf(pos),
								 table.getTableName(),Integer.valueOf(j),Integer.valueOf(pos),
								pkeyColumn,pkeyColumn,"=");
						//get primary Key constraint and add to constraint list
						conList.add(con);
						
					}
					constraintString =constraintGenerator.generateANDConstraints(conList);
			
					boolean x = false;
					for(String col : table.getColumns().keySet()){
						if(!( table.getPrimaryKey().toString().contains(col))){
							x = true;
							int pos = table.getColumnIndex(col);

							/**This attribute has to be equal */
							impliedConstraint = constraintGenerator.getConstraint(table.getTableName(),Integer.valueOf(j),Integer.valueOf(pos),
									 table.getTableName(),Integer.valueOf(j),Integer.valueOf(pos),
									table.getColumn(col),table.getColumn(col),"=");
							//get primary Key constraint implied other constraints and add to constraint list
							impliedConstraintList.add(impliedConstraint);
						}
					}
					impliedConstraintString =constraintGenerator.generateANDConstraints(conList);
					
					//Generate single constraint with => as operator and left and right as FKConstraints and implied constraints based on solver.
					
					primaryKeyConstraint.setLeftConstraint(constraintString);
					primaryKeyConstraint.setOperator(" =>");
					primaryKeyConstraint.setRightConstraint(impliedConstraintString);
					
					temp += constraintGenerator.getImpliedConstraints(primaryKeyConstraint,x);
					
					
				}
			}
		}
		/** End of Added for primary key constraints of branchQuery */


		/**Added for foreign key constraints of branchQuery*/
		temp += ConstraintGenerator.addCommentLine(" FOREIGN KEY CONSTRAINTS FOR BRANCH QUERY");

		tempTuplesAdded = new HashMap<Table, Integer>();

		for(Table table : cvc.getResultsetTables())
			tempTuplesAdded.put(table, 0);
		
		con = new ConstraintObject();
		String isNullCon = "" ;
		conList = new ArrayList<ConstraintObject>();
		constraintString = "";
		
		ArrayList<String> isNullConstraintList = new ArrayList<String>();
		String  isNullConstraintString = "";
		for(int j = 0; j < noOfBranchQueries; j++){

			for(Table tempTab : noOfTuplesAddedToTablesForBranchQueries[j].keySet()){

				Collection<ForeignKey> tempFKCollect = null;
				String temp1 = "";
				String temp2 = "";
				Vector<Table> tempTablesChanged = new Vector<Table>();

				if(tempTab.getForeignKeys() != null){

					tempFKCollect = tempTab.getForeignKeys().values();

					
					for(int k = 1; k <= noOfTuplesAddedToTablesForBranchQueries[j].get(tempTab); k++){

						for(ForeignKey tempFK : tempFKCollect){

							temp1 = "";
							temp2 = "";

							Vector<Column> fCol = tempFK.getFKeyColumns();
							Vector<Column> pCol = tempFK.getReferenceKeyColumns();

							for (Column fSingleCol : fCol){

								Column pSingleCol = pCol.get(fCol.indexOf(fSingleCol));

								if(fSingleCol.getCvcDatatype() != null){

									int tupleIndexofFCol = cvc.getNoOfOutputTuples().get(fSingleCol.getTableName()) + k + tempTuplesAdded.get(fSingleCol.getTable());

									int tupleIndexofPCol = cvc.getNoOfOutputTuples().get(pSingleCol.getTableName()) + k + tempTuplesAdded.get(pSingleCol.getTable());
									int pos1 = fSingleCol.getTable().getColumnIndex(fSingleCol.getColumnName());
									int pos2 = pSingleCol.getTable().getColumnIndex(pSingleCol.getColumnName());
									
									con = constraintGenerator.getConstraint(fSingleCol.getTableName(),tupleIndexofFCol,Integer.valueOf(pos1),
											pSingleCol.getTableName(),tupleIndexofPCol,Integer.valueOf(pos2),fSingleCol,pSingleCol,"=");
									//Add single constraint to the list of constraints. Later call constraintGenerator.generateANDConstraints to AND all constraints based on solver.
									conList.add(con);
									
									if(fSingleCol.isNullable()){
											isNullCon = constraintGenerator.getIsNullCondition(fSingleCol.getTableName(),fSingleCol,Integer.toString(tupleIndexofFCol));
											//Add single constraint to the list of constraints. Later call constraintGenerator.getNullConditionConjuncts to AND all Null constraints based on solver.
											isNullConstraintList.add(isNullCon);
									}
									
									
									/*temp1 += "(O_" + GenerateCVCConstraintForNode.cvcMap(fSingleCol, "" + tupleIndexofFCol) + " = O_" + 
											GenerateCVCConstraintForNode.cvcMap(pSingleCol, "" + tupleIndexofPCol) + ") AND ";

									if(fSingleCol.getCvcDatatype().equals("INT")|| fSingleCol.getCvcDatatype().equals("REAL") 
											|| fSingleCol.getCvcDatatype().equals("DATE") || fSingleCol.getCvcDatatype().equals("TIME") || fSingleCol.getCvcDatatype().equals("TIMESTAMP"))

										temp2 += "ISNULL_" + fSingleCol.getColumnName() + "(O_" + GenerateCVCConstraintForNode.cvcMap(fSingleCol, "" + tupleIndexofFCol) + ") AND ";

									else

										temp2 += "ISNULL_" + fSingleCol.getCvcDatatype() + "(O_" + GenerateCVCConstraintForNode.cvcMap(fSingleCol, "" + tupleIndexofFCol) + ") AND ";
									*/
								}
							}

							/*temp1 = temp1.substring(0, temp1.length() - 5);
							temp2 = temp2.substring(0, temp2.length() - 5);
							temp += "ASSERT (" + temp1 + ") OR (" + temp2 + ");\n";*/

						}
						
						constraintString =constraintGenerator.generateANDConstraints(conList);
						isNullConstraintString = constraintGenerator.getNullConditionConjuncts(isNullConstraintList);
						temp += constraintGenerator.getFKConstraint(constraintString, isNullConstraintString); 
						
					}
				}
			}

			for(Table table : noOfTuplesAddedToTablesForBranchQueries[j].keySet())
				tempTuplesAdded.put(table, tempTuplesAdded.get(table) + noOfTuplesAddedToTablesForBranchQueries[j].get(table));
		}
		/**End of Added for foreign key constraints of branchQuery*/


		/**Adding NOT NULL constraints to BRANCHQUERY*/
		String retVal = ConstraintGenerator.addCommentLine("NOT NULL CONSTRAINTS FOR BRANCHQUERY\n\n");
		Iterator<Column> itrCol = cvc.getColNullValuesMap().keySet().iterator();

		while(itrCol.hasNext()){

			Column col = itrCol.next();
			String tabName = col.getTableName();

			if(tempTuplesAddedForBranchQueries.keySet().contains(col.getTable())){

				for(int i = 1; i <= tempTuplesAddedForBranchQueries.get(col.getTable()); i++){

					retVal += constraintGenerator.getAndSetNotNullValuesBeforeFooter(col, (cvc.getNoOfOutputTuples().get(tabName) + i)); 
					
					/*if(col.getCvcDatatype().equals("INT") || col.getCvcDatatype().equals("REAL") || col.getCvcDatatype().equals("TIME")
							||col.getCvcDatatype().equals("TIMESTAMP") || col.getCvcDatatype().equals("DATE"))

						retVal += "\nASSERT NOT ISNULL_"+col.getColumnName()+"(O_"+ GenerateCVCConstraintForNode.cvcMap(col, (cvc.getNoOfOutputTuples().get(tabName) + i)+"")+");";
					else
						retVal += "\nASSERT NOT ISNULL_"+col.getCvcDatatype()+"(O_"+ GenerateCVCConstraintForNode.cvcMap(col, (cvc.getNoOfOutputTuples().get(tabName) + i)+"")+");";
						*/
				}
			}
		}
		temp += retVal;
		/** End of Adding NOT NULL constraints to BRANCHQUERY*/


		return temp;
	}



	public static void adjustNoOfTuplesForBranchQuery( GenerateCVC1 cvc) throws Exception{

		Query branchQuery[] = cvc.getBranchQueries().getBranchQuery();

		if( branchQuery != null){

			int noOfBranchQueries = cvc.getBranchQueries().getNoOfBranchQueries();
			HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[] = cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries();
			noOfTuplesAddedToTablesForBranchQueries = new HashMap[noOfBranchQueries];


			QueryParser qParser1[] = cvc.getBranchQueries().getqParser1();

			for(int i = 0; i < noOfBranchQueries; i++){

				Vector<Table> tempTablesChanged = new Vector<Table>();
				noOfTuplesAddedToTablesForBranchQueries[i] = new HashMap<Table, Integer>();

				if(branchQuery[i] != null && !branchQuery[i].equals("")){					

					for(Table branchTable : branchQuery[i].getFromTables().values()){

						tempTablesChanged.add(branchTable);
						noOfTuplesAddedToTablesForBranchQueries[i].put(branchTable, 1);
					}

					for(int j = 0; j < tempTablesChanged.size(); j++){

						Collection<ForeignKey> tempFKCollect = null;

						if(tempTablesChanged.get(j).getForeignKeys() != null){

							tempFKCollect = tempTablesChanged.get(j).getForeignKeys().values();

							for(ForeignKey tempFK : tempFKCollect){

								if(!tempTablesChanged.contains(tempFK.getReferenceTable())){

									tempTablesChanged.add(tempFK.getReferenceTable());
									noOfTuplesAddedToTablesForBranchQueries[i].put(tempFK.getReferenceTable(), 1);
								}
							}
						}
					}
				}	
			}


			/**Creating multiple tuples for a group-by branch query*/
			for(int i = 0; i < noOfBranchQueries; i++){

				if(qParser1[i].getHavingClause() != null){

					Node tempHavingClause = qParser1[i].getHavingClause();
					Vector<Node> tempHavingClauseArray = new Vector<Node>();

					while(tempHavingClause.getOperator().equals("AND")){

						tempHavingClauseArray.add(tempHavingClause.getRight());
						tempHavingClause = tempHavingClause.getLeft();
					}

					tempHavingClauseArray.add(tempHavingClause);
					Table tempHavingTable = null;

					Vector<Table> tempHavingTableArray = new Vector<Table>();
					Vector<Node> havingGroup = (Vector<Node>)tempHavingClauseArray.clone();

					for(int j = 0; j < havingGroup.size(); j++){

						tempHavingTable = havingGroup.get(j).getLeft().getAgg().getAggExp().getTable();

						if(!tempHavingTableArray.contains(tempHavingTable))

							tempHavingTableArray.add(tempHavingTable);

						String func = havingGroup.get(j).getLeft().getAgg().getFunc();
						String op = havingGroup.get(j).getOperator();
						int val = Integer.parseInt(havingGroup.get(j).getRight().getStrConst());

						if(func.equals("COUNT")){

							if(op.equals("="))
								noOfTuplesAddedToTablesForBranchQueries[i].put(tempHavingTable, val);

							else if(op.equals(">=")){

								if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable) < val)
									noOfTuplesAddedToTablesForBranchQueries[i].put(tempHavingTable, val);
							}

							else if(op.equals(">")){

								if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable) < val + 1)
									noOfTuplesAddedToTablesForBranchQueries[i].put(tempHavingTable, val + 1);
							}

							havingGroup.remove(j);
							j--;
						}
					}

					int j = 0;

					while(j < havingGroup.size()){

						Vector<Node> tempGroup = new Vector<Node>();
						tempGroup.add(havingGroup.get(j));
						havingGroup.remove(j);

						while(j < havingGroup.size()){

							if(tempGroup.get(0).getLeft().getAgg().getAggExp().getColumn().equals(havingGroup.get(j).getLeft().getAgg().getAggExp().getColumn())){

								tempGroup.add(havingGroup.get(j));
								havingGroup.remove(j);
							}

							else
								j++;
						}

						Table tempTable = tempGroup.get(0).getLeft().getAgg().getAggExp().getTable();
						Column tempCol = tempGroup.get(0).getLeft().getAgg().getAggExp().getColumn();

						String op1 = null;
						String op2 = null;
						String op3 = null;
						String op4 = null;

						int val1 = 0;
						int val2 = 0;
						int val3 = 0;
						int val4 = 0;

						for(int k = 0; k < tempGroup.size(); k++){

							if(tempGroup.get(k).getLeft().getAgg().getFunc().equals("SUM")){

								if(op1 == null){

									op1 = tempGroup.get(k).getOperator();
									val1 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(op1.equals("="))
									continue;

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) > val1){

									op1 = tempGroup.get(k).getOperator();
									val1 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) == val1 && tempGroup.get(k).getOperator().equals(">"))
									op1 = ">";
							}

							else if(tempGroup.get(k).getLeft().getAgg().getFunc().equals("AVG")){

								if(op2 == null){

									op2 = tempGroup.get(k).getOperator();
									val2 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(op2.equals("="))
									continue;

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) < val2){

									op2 = tempGroup.get(k).getOperator();
									val2 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) == val2 && tempGroup.get(k).getOperator().equals("<"))
									op2 = "<";
							}

							else if(tempGroup.get(k).getLeft().getAgg().getFunc().equals("MIN")){

								if(op3 == null){

									op3 = tempGroup.get(k).getOperator();
									val3 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(op3.equals("="))
									continue;

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) < val3)	{

									op3 = tempGroup.get(k).getOperator();
									val3 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) == val3 && tempGroup.get(k).getOperator().equals("<"))
									op3 = "<";
							}

							else if(tempGroup.get(k).getLeft().getAgg().getFunc().equals("MAX")){

								if(op4 == null){

									op4 = tempGroup.get(k).getOperator();
									val4 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(op4.equals("="))
									continue;

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) > val4){

									op4 = tempGroup.get(k).getOperator();
									val4 = Integer.parseInt(tempGroup.get(k).getRight().getStrConst());
								}

								else if(Integer.parseInt(tempGroup.get(k).getRight().getStrConst()) == val4 && tempGroup.get(k).getOperator().equals(">"))
									op4 = ">";
							}
						}

						if(op1 != null){

							for(Node tempNode : qParser1[i].getSelectionConds()){

								if(tempNode.getLeft().getColumn().equals(tempCol)){

									op2 = tempNode.getOperator();
									val2 = Integer.parseInt(tempNode.getRight().getStrConst());

									if(op1.equals("=")){

										if(op2.equals("=")){

											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2);
											break;
										}

										else if(op2.equals("<=")){

											if(val1 % val2 == 0){

												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val2)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2);
											}

											else{

												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val2 + 1)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2 + 1);
											}
										}

										else if(op2.equals("<")){

											if(val1 % (val2 - 1) == 0){

												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val2 - 1))
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val2 - 1));
											}

											else{

												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val2 - 1) + 1)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val2 - 1) + 1);
											}
										}
									}

									if(op1.equals(">="))
									{
										if(op2.equals("=") || op2.equals("<="))
										{
											if(val1 % val2 == 0)
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val2)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2);
											}
											else
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val2 + 1)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2 + 1);
											}
										}
										else if(op2.equals("<"))
										{
											if(val1 % (val2 - 1) == 0)
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val2 - 1))
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val2 - 1));
											}
											else
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val2 - 1) + 1)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val2 - 1) + 1);
											}
										}
									}
									if(op1.equals(">"))
									{
										if(op2.equals("=") || op2.equals("<="))
										{
											if((val1 + 1) % val2 == 0)
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / val2)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / val2);
											}
											else
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / val2 + 1)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / val2 + 1);
											}
										}
										else if(op2.equals("<"))
										{
											if(val1 % (val2 - 1) == 0)
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / (val2 - 1))
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / (val2 - 1));
											}
											else
											{
												if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / (val2 - 1) + 1)
													noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / (val2 - 1) + 1);
											}
										}
									}
								}
							}
						}
						if(op1 != null && op2 != null)
						{
							if(op1.equals("="))
							{
								if(op2.equals("="))
									noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2);
								else if(op2.equals("<="))
								{
									for(int l = val2; l >= 1; l++)
										if(val1 % l == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / l)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / l);
											break;
										}
								}
								else if(op2.equals("<"))
								{
									for(int l = val2 - 1; l >= 1; l++)
										if(val1 % l == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / l)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / l);
											break;
										}
								}
							}
							else if(op1.equals(">="))
							{
								if(op2.equals("=") || op2.equals("<="))
								{
									if(val1 % val2 == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val2)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2);
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val2 + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val2 + 1);
									}
								}
								else if(op2.equals("<"))
								{
									if(val1 % (val2 - 1) == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val2 - 1))
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val2 - 1));
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val2 - 1) + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val2 - 1) + 1);
									}
								}
							}
							else if(op1.equals(">"))
							{
								if(op2.equals("=") || op2.equals("<="))
								{
									if((val1 + 1) % val2 == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / val2)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / val2);
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / val2 + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / val2 + 1);
									}
								}
								else if(op2.equals("<"))
								{
									if((val1 + 1) % (val2 - 1) == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / (val2 - 1))
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / (val2 - 1));
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / (val2 - 1) + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / (val2 - 1) + 1);
									}
								}
							}
						}
						if(op1 != null && op3 != null)
						{
							if(op1.equals("=") || op1.equals(">="))
							{
								if((op3.equals("=") || op3.equals("<=")) && val1 > val3)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op3.equals("<") && val1 > val3 - 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
							else if(op1.equals(">"))
							{
								if((op3.equals("=") || op3.equals("<=")) && val1 + 1 > val3)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op3.equals("<") && val1 + 1 > val3 - 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
						}
						if(op1 != null && op3 != null && op4 != null)
						{
							if(op3.equals("=") || op3.equals("<="))
							{
								if(op1.equals("=") || op1.equals(">="))
								{
									if((op4.equals("=") || op4.equals("<=")) && val1 - val3 > val4)
									{
										if((val1 - val3) % val4 == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3) / val4 + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3) / val4 + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3) / val4 + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3) / val4 + 2);
										}
									}
									else if(op4.equals("<") && val1 - val3 > val4 - 1)
									{
										if((val1 - val3) % (val4 - 1) == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3) / (val4 - 1) + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3) / (val4 - 1) + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3) / (val4 - 1) + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3) / (val4 - 1) + 2);
										}
									}
								}
								else if(op1.equals(">"))
								{
									if((op4.equals("=") || op4.equals("<=")) && val1 - val3 + 1 > val4)
									{
										if((val1 - val3 + 1) % val4 == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / val4 + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / val4 + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / val4 + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / val4 + 2);
										}
									}
									else if(op4.equals("<") && val1 - val3 + 1 > val4 - 1)
									{
										if((val1 - val3 + 1) % (val4 - 1) == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / (val4 - 1) + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / (val4 - 1) + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / (val4 - 1) + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / (val4 - 1) + 2);
										}
									}
								}
							}
							if(op3.equals("<"))
							{
								if(op1.equals("=") || op1.equals(">="))
								{
									if((op4.equals("=") || op4.equals("<=")) && val1 - val3 + 1 > val4)
									{
										if((val1 - val3 + 1) % val4 == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / val4 + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / val4 + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / val4 + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / val4 + 2);
										}
									}
									else if(op4.equals("<") && val1 - val3 + 1 > val4 - 1)
									{
										if((val1 - val3 + 1) % (val4 - 1) == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / (val4 - 1) + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / (val4 - 1) + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 1) / (val4 - 1) + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 1) / (val4 - 1) + 2);
										}
									}
								}
								else if(op1.equals(">"))
								{
									if((op4.equals("=") || op4.equals("<=")) && val1 - val3 + 2 > val4)
									{
										if((val1 - val3 + 2) % val4 == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 2) / val4 + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 2) / val4 + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 2) / val4 + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 2) / val4 + 2);
										}
									}
									else if(op4.equals("<") && val1 - val3 + 2 > val4 - 1)
									{
										if((val1 - val3 + 2) % (val4 - 1) == 0)
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 2) / (val4 - 1) + 1)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 2) / (val4 - 1) + 1);
										}
										else
										{
											if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 - val3 + 2) / (val4 - 1) + 2)
												noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 - val3 + 2) / (val4 - 1) + 2);
										}
									}
								}
							}
						}
						else if(op1 != null && op4 != null)
						{
							if(op1.equals("=") || op1.equals(">="))
							{
								if((op4.equals("=") || op4.equals("<=")) && val1 > val4)
								{
									if(val1 % val4 == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val4)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val4);
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / val4 + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / val4 + 1);
									}
								}
								else if(op4.equals("<") && val1 > val4 - 1)
								{
									if(val1 % (val4 - 1) == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val4 - 1))
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val4 - 1));
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < val1 / (val4 - 1) + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, val1 / (val4 - 1) + 1);
									}
								}
							}
							else if(op1.equals(">"))
							{
								if((op4.equals("=") || op4.equals("<=")) && val1 + 1 > val4)
								{
									if((val1 + 1) % val4 == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / val4)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / val4);
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / val4 + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / val4 + 1);
									}
								}
								else if(op4.equals("<") && val1 + 1 > val4 - 1)
								{
									if((val1 + 1) % (val4 - 1) == 0)
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / (val4 - 1))
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / (val4 - 1));
									}
									else
									{
										if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < (val1 + 1) / (val4 - 1) + 1)
											noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, (val1 + 1) / (val4 - 1) + 1);
									}
								}
							}
						}
						if(op3 != null && op4 != null)
						{
							if(op3.equals("=") || op3.equals("<="))
							{
								if((op4.equals("=") || op4.equals(">=")) && val3 < val4)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op4.equals(">") && val3 < val4 + 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
							else if(op3.equals("<"))
							{
								if((op4.equals("=") || op4.equals(">=")) && val3 - 1 < val4)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op4.equals(">") && val3 - 1 < val4 + 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
						}
						if(op3 != null && op2 != null)
						{
							if(op3.equals("=") || op3.equals("<="))
							{
								if((op2.equals("=") || op2.equals(">=")) && val3 < val2)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op2.equals(">") && val3 < val2 + 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
							else if(op3.equals("<"))
							{
								if((op2.equals("=") || op2.equals(">=")) && val3 - 1 < val2)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op2.equals(">") && val3 - 1 < val2 + 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
						}
						if(op4 != null && op2 != null)
						{
							if(op4.equals("=") || op4.equals(">="))
							{
								if((op2.equals("=") || op2.equals("<=")) && val4 > val2)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op2.equals("<") && val4 > val2 - 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
							else if(op3.equals(">"))
							{
								if((op2.equals("=") || op2.equals("<=")) && val4 + 1 > val2)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
								else if(op2.equals("<") && val4 + 1 > val2 - 1)
								{
									if(noOfTuplesAddedToTablesForBranchQueries[i].get(tempTable) < 2)
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTable, 2);
								}
							}
						}
						j = 0;
					}
					Vector<Table> tempFromTables = new Vector<Table>();
					tempFromTables.addAll(branchQuery[i].getFromTables().values());
					for(Table tempTab : tempFromTables)
					{
						if(!tempTab.equals(tempHavingTable))
						{
							Collection<ForeignKey> tempFKCollect = null;
							if(tempTab.getForeignKeys() != null)
							{
								tempFKCollect = tempTab.getForeignKeys().values();
								for(ForeignKey tempFK : tempFKCollect)
								{
									if(tempFK.getReferenceTable().equals(tempHavingTable))
										noOfTuplesAddedToTablesForBranchQueries[i].put(tempTab, noOfTuplesAddedToTablesForBranchQueries[i].get(tempHavingTable));
								}
							}
						}
					}
					for(int k = 0; k < tempFromTables.size(); k++)
					{
						Collection<ForeignKey> tempFKCollect = null;
						if(tempFromTables.get(k).getForeignKeys() != null)
						{
							tempFKCollect = tempFromTables.get(k).getForeignKeys().values();
							for(ForeignKey tempFK : tempFKCollect)
							{
								if(!tempFromTables.contains(tempFK.getReferenceTable()))
								{
									tempFromTables.add(tempFK.getReferenceTable());
									noOfTuplesAddedToTablesForBranchQueries[i].put(tempFK.getReferenceTable(), noOfTuplesAddedToTablesForBranchQueries[i].get(tempFromTables.get(k)));
								}
							}
						}
					}
				}
			}
			//End of creating multiple tuples for a group-by branch query
		}
		
	}



	public static HashMap<Table, Integer> checkForTuplesAddedForBranchQuery(GenerateCVC1 cvc) {
		
		HashMap<Table, Integer> tempTuplesAddedForBranchQueries = new HashMap<Table, Integer>();
		
		for(int i = 0; i < cvc.getBranchQueries().getNoOfBranchQueries(); i++)
		{
			if(!cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries()[i].isEmpty())
			{
				for(Table tempTab : cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries()[i].keySet())
				{
					if(tempTuplesAddedForBranchQueries.containsKey(tempTab))
						tempTuplesAddedForBranchQueries.put(tempTab, tempTuplesAddedForBranchQueries.get(tempTab) + cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries()[i].get(tempTab));
					else
						tempTuplesAddedForBranchQueries.put(tempTab, cvc.getBranchQueries().getNoOfTuplesAddedToTablesForBranchQueries()[i].get(tempTab));
				}
			}
		}
		
		return tempTuplesAddedForBranchQueries;
	}
}
