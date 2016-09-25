//@author Bharath
//@author Mathew 
package partialMarking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.Set;

import parsing.Column;
import parsing.ForeignKey;
import parsing.Node;
import parsing.QueryParser;

public class EliminateRedundantRelation {

	private static Logger logger = Logger.getLogger(EliminateRedundantRelation.class.getName());

	public static void EliminateRedundantRelations(QueryData queryData) throws CloneNotSupportedException{
		EliminateRedundantRelation.EliminateRelations(queryData);
		if(queryData!=null){
			queryData.reviseAfterFindingRedundantRelations();
		}
	}

	private static void EliminateRelations(QueryData query) throws CloneNotSupportedException{
		if(query==null)
			return;

		if(query != null && query.WhereClauseQueries != null){
			for(QueryData qd : query.WhereClauseQueries){
				EliminateRedundantRelation.EliminateRelations(qd);
				qd.reviseAfterFindingRedundantRelations();
			}}

		if(query != null && query.FromClauseQueries != null){
			for(QueryData qd : query.FromClauseQueries){
				EliminateRedundantRelation.EliminateRelations(qd);
				qd.reviseAfterFindingRedundantRelations();
			}}

		ArrayList<String> eliminateRelations = new ArrayList<String>();

		QueryParser parser = query.Parser;	

		Vector<Node> groupByCols=parser.getGroupByNodes();

		Map<String,ArrayList<Node>> relationToGroupByCols=EliminateRedundantRelation.createRelationToGroupByColumns(groupByCols);

		ArrayList<Node> havingConds=query.getHavingClause();
		Map<String,ArrayList<Node>> relationToHavingConds=EliminateRedundantRelation.createRelationToHavingConditions(havingConds);

		
		ArrayList<Node> selectionConds = query.getSelectionConditions();
		selectionConds.addAll(query.getJoinConditions());
		
		//selectionConds=removeDuplicates(selectionConds);

		Map<String, ArrayList<Node>> relationToSelConds=createRelationToSelectionConditions(selectionConds);
		
		Map<String, ArrayList<Node>> relationToProjCols=createRelationToProjectedColumns(parser.getProjectedCols());

		Map<String, ArrayList<Node>> relationToOrderByCols=createRelationToProjectedColumns(parser.getOrderByNodes());

		Vector<Vector<Node>> eqClasses = query.getEqClasses();		
		

		Map<Node, ArrayList<Node> > nodeToEqNodes = new HashMap<Node, ArrayList<Node>>();
		Map<String, ArrayList<Node>> relationToEqNodes = new HashMap<String, ArrayList<Node>>();
		Map<String, HashMap<String, ArrayList<Pair>>> relationToRelationEqNodes = new HashMap<String, HashMap<String, ArrayList<Pair>>>(); 
		if(eqClasses != null){
			for(Vector<Node> t : eqClasses){
				for(Node n1 : t){

					ArrayList<Node> temp = null;
					if(nodeToEqNodes.containsKey(n1)){
						temp = nodeToEqNodes.get(n1);
					}
					else{
						temp = new ArrayList<Node>();
					}

					for(Node n2 : t){
						if(!n1.equals(n2)){
							temp.add(n2);
						}
					}

					ArrayList<Node> temp2 = null;
					if(relationToEqNodes.containsKey(n1.getTableNameNo())){
						temp2 = relationToEqNodes.get(n1.getTableNameNo());
					}
					else{
						temp2 = new ArrayList<Node>();
					}

					temp2.add(n1);

					relationToEqNodes.put(n1.getTableNameNo(), temp2);

					nodeToEqNodes.put(n1, temp);
				}
			}
		}

		for (Entry<String, ArrayList<Node>> entry : relationToEqNodes.entrySet()) {

			String key = entry.getKey();
			ArrayList<Node> value = entry.getValue();

			Map<String, ArrayList<Pair>> data = relationToRelationEqNodes.get(key);

			if(data == null){
				relationToRelationEqNodes.put(key, new HashMap<String, ArrayList<Pair>>());
				data = relationToRelationEqNodes.get(key);
			}

			for(Node n : value){		    	
				ArrayList<Node> nodes = nodeToEqNodes.get(n);

				for(Node n1 : nodes){
					String temp = n1.getTableNameNo();

					ArrayList<Pair> tempData = null;

					if(data.containsKey(temp)){
						tempData = data.get(temp);
					}
					else {
						tempData = new ArrayList<Pair>();
					}			    	

					tempData.add(new Pair(n, n1));
					data.put(temp, tempData);	
				}	    	
			}
		}
		

		logger.info(" relation to Selection Conds Map "+relationToSelConds);
		//		for(Entry<String, ArrayList<Node>> entry:relationToSelConds.entrySet()){
		//		System.out.println("key="+entry.getKey());
		//		System.out.println("Values:");
		//		for(Node n:entry.getValue()){
		//			System.out.print(n.getLeft().getTable().getTableName()+":");
		//			System.out.print(n.getLeft().getColumn().getTableName()+":");
		//			System.out.print(n.getLeft().getColumn().getColumnName()+"\n");
		//			System.out.println(n.getLeft()+"="+n.getRight());
		//		}
		//	}
		logger.info(" relation to Projection Conds Map "+relationToProjCols);
		logger.info(" relation to Group By Cols Map "+relationToGroupByCols);
		logger.info(" relation to Having Conds Map "+relationToHavingConds);
		logger.info(" relation to Order By Conds Map "+relationToOrderByCols);
		
		/////////////////////code for revised detection of redundant relation starts here

		/** Get the list of foreign keys*/
		ArrayList<ForeignKey> foreignKeys = query.WholeData.getForeignKeysModified();
	
		Set<String> baseTables=new HashSet<String>();
		//for(String table: query.WholeData)
		for(String table: query.getRelationInstances()){
			baseTables.add(table);
		}
		logger.info( " baseTables "+baseTables);


		Set<String> baseTablesOld;

		/*
		 * Do while loop below computes a fix point of the  eliminateRelations
		 * which in each iteration stores the set of eliminated relations so far
		 * 
		 * eliminated relations are removed from set baseTables which is the set of 
		 * non eliminated relations in the query, hence the termination condition 
		 * depends on the fixpoint of baseTables
		 */
		do{
			baseTablesOld=new HashSet<String>();

			for(String table: baseTables)
				baseTablesOld.add(table);			

			/* referencedRelations are the set of relations R s.t. key attributes
			 * in R are refered to by foreign keys of another table R'  and the corresponding attributes 
			 * of referencing table R' and referenced table R are part of the eqivalence
			 * relation induced by the query
			 */
			ArrayList<String> referencedRelations=QueryData.getReferencedRelations(baseTables, eliminateRelations, foreignKeys, relationToRelationEqNodes);	
			logger.info("Referenced Relations:"+referencedRelations);

			/* any referenced relation is a candidates for 
			 * a redundant relation if every projected (resp. selected) column c1 from this relation 
			 * have an equivalent projected (resp. selected ) column c2 with c1.tableName != c2.tableName
			 */

			/*
			 * Now check if for any referenced relation R, every selected (resp. projected) column c1 
			 * there is another equivalent column c2 s.t c1.tableName!= c2.tableName  
			 */


			for(String refTable: referencedRelations){
				
				if(isInvolvedInMultipleSelConditions(refTable,relationToSelConds))
					continue;

				boolean altEqSelFlag=existsAlternateEquivalentSelectionConditions(refTable, relationToSelConds, nodeToEqNodes, baseTables);
				if(!altEqSelFlag){
					logger.info("Table "+refTable+ " is not a candidate");
					continue;
				}				
				boolean altEqProjFlag=existsAlternateEquivalentProjections(refTable, relationToProjCols, nodeToEqNodes, baseTables);

				if(!altEqProjFlag){
					logger.info("Table "+refTable+ " is not a candidate");
					continue;
				}
				boolean altEqGroupByFlag=existsAlternateEquivalentGroupByCols(refTable, relationToGroupByCols, nodeToEqNodes, baseTables);
				if(!altEqGroupByFlag){
					logger.info("Table "+refTable+ " is not a candidate");
					continue;
				}
				boolean altEqHavingFlag=existsAlternateEquivalentHavingConditions(refTable, relationToHavingConds, nodeToEqNodes, baseTables);
				if(!altEqHavingFlag){
					logger.info("Table "+refTable+ " is not a candidate");
					continue;
				}
				boolean altEqOrderByFlag=existsAlternateEquivalentProjections(refTable, relationToOrderByCols, nodeToEqNodes, baseTables);
				if(!altEqOrderByFlag){
					logger.info("Table "+refTable+ " is not a candidate");
					continue;
				}

				if(altEqSelFlag && altEqProjFlag && altEqGroupByFlag && altEqHavingFlag && altEqOrderByFlag){
					logger.info("Relation "+refTable+ " is a redundant relation");
					foreignKeys=EliminateRedundantRelation.removeForeignKey(refTable, foreignKeys);
					eliminateRelations.add(refTable);
					baseTables.remove(refTable);
					relationToProjCols=updateRelationsToProjectedColumns(baseTables, refTable, relationToProjCols,nodeToEqNodes);
					relationToSelConds=updateRelationsToSelectionConditions(baseTables, refTable, relationToSelConds, nodeToEqNodes);
					relationToGroupByCols=updateRelationsToGroupByColumns(baseTables, refTable, relationToGroupByCols, nodeToEqNodes);
					relationToHavingConds=updateRelationsToHavingConditions(baseTables, refTable, relationToHavingConds, nodeToEqNodes);
					relationToOrderByCols=updateRelationsToProjectedColumns(baseTables, refTable, relationToOrderByCols,nodeToEqNodes);
					

				}
			}

		} while(!baseTables.containsAll(baseTablesOld));

		/////////////////////code for revised detection of redundant relation ends here

		logger.info(" eliminated relations "+eliminateRelations);
		
		if(query.RedundantRelations.isEmpty())
			query.RedundantRelations = eliminateRelations;
		else
			query.RedundantRelations.addAll(eliminateRelations);
	
	}
	
	/*
	 * Returns true if the input table tableNameNo has multiple join condtions associated with it
	 */
	private static boolean isInvolvedInMultipleSelConditions(String tableNameNo,
			Map<String, ArrayList<Node>> relationToSelConds) {
		// TODO Auto-generated method stub
		ArrayList<Node> selConds=relationToSelConds.get(tableNameNo);
		ArrayList<Node> joinConds=new ArrayList<Node>();
		/*Note that selConds contain both joinConditions and Selection Conditions (one of the operands is a constant)
		 * Hence, filter join conditions and put it seperately to joinConds
		 */
		for(Node selCond:selConds){
			if(selCond.getLeft().getNodeType().equals(Node.getColRefType())&&selCond.getRight().getNodeType().equals(Node.getColRefType()))
				joinConds.add(selCond);
		}
		if(joinConds!=null&&joinConds.size()>0){
			joinConds=removeDuplicates(joinConds);
			if(joinConds.size()>=2){
				logger.info("Table "+tableNameNo +" is involved in multiple join conditions, and hence cannot be removed");
				return true;
			}
		}
		return false;
	}

	/*
	 * remove duplicates from a list of input selection/join conditions
	 */
	private static ArrayList<Node> removeDuplicates(ArrayList<Node> selectionConds) {
		// TODO Auto-generated method stub
		boolean removedFlag;
		do{
			removedFlag=false;
			for(int i=0;i<selectionConds.size()-1;i++){
				Node src=selectionConds.get(i);
				boolean found=false;
				for(int j=i+1;j<selectionConds.size();j++){
					Node tar=selectionConds.get(j);
					if(src==tar){
						found=true;
						break;
					}
					String srcLeftStr=src.getLeft().toString();
					String srcRightStr=src.getRight().toString();
					String tarLeftStr=tar.getLeft().toString();
					String tarRightStr=tar.getRight().toString();
					if(srcLeftStr.equalsIgnoreCase(tarLeftStr)&&srcRightStr.equalsIgnoreCase(tarRightStr)){
						found=true;
						break;
					}
				}
				if(found){
					selectionConds.remove(i);
					removedFlag=true;
					break;
				}
			}
		} while(removedFlag);
		return selectionConds;
	}

	public static Map<String, ArrayList<Node>> createRelationToProjectedColumns(Vector<Node> projectedCols){
		Map<String, ArrayList<Node>> relationToProjCols = new HashMap<String, ArrayList<Node>>();

		for(Node n: projectedCols){

			if(n == null)
				continue;

			String relation = "";

			if(n.getType()!=null&& n.getType().equals(Node.getAggrNodeType())){				
				if(n.getAgg().getAggExp() != null && n.getAgg().getAggExp().getTable() != null){				
					//relation = n.getAgg().getAggExp().getTable().getTableName();
					relation = n.getAgg().getAggExp().getTableNameNo();
				}
			} else{
				if(n.getTable() != null)
					//relation = n.getTable().getTableName();
					relation = n.getTableNameNo();
			}

			if(relation.isEmpty())
				continue;

			ArrayList<Node> temp = new ArrayList<Node>();

			if(relationToProjCols.containsKey(relation)){
				temp = relationToProjCols.get(relation);
			}

			temp.add(n);			
			relationToProjCols.put(relation, temp);
		}
		return relationToProjCols;
	}
	
	public static Map<String,ArrayList<Node>> createRelationToGroupByColumns(Vector<Node> groupByCols){
		
		Map<String, ArrayList<Node>> relationToGroupByCols = new HashMap<String, ArrayList<Node>>();
		if(groupByCols!=null){
			for(Node n:groupByCols){

				if(n == null)
					continue;
				String relation = "";


				if(n.getTable() != null)
					//relation = n.getTable().getTableName();
					relation = n.getTableNameNo();

				if(relation.isEmpty())
					continue;

				ArrayList<Node> temp = new ArrayList<Node>();

				if(relationToGroupByCols.containsKey(relation)){
					temp = relationToGroupByCols.get(relation);
				}

				temp.add(n);			
				relationToGroupByCols.put(relation, temp);

			}
		}
		return relationToGroupByCols;
	}
	
	public static Map<String,ArrayList<Node>> createRelationToHavingConditions(ArrayList<Node> havingConds){
		
		Map<String, ArrayList<Node>> relationToHavingConds = new HashMap<String, ArrayList<Node>>();

		if(havingConds != null){
			for(Node n : havingConds){
				Node left = n.getLeft();
				String relation="";
				if(left!=null && !left.getType().equals(Node.getValType())){
					if(left.getType().equals(Node.getAggrNodeType())){				
						if(left.getAgg().getAggExp() != null && left.getAgg().getAggExp().getTable() != null){				
							//relation = left.getAgg().getAggExp().getTable().getTableName();
							relation = left.getAgg().getAggExp().getTableNameNo();
						}
					} 
					else{
						if(left.getTable() != null)
							//relation = left.getTable().getTableName();
							relation = left.getTableNameNo();
					}			
					if(relation.isEmpty())
						continue;
					ArrayList<Node> temp = new ArrayList<Node>();
					if(relationToHavingConds.containsKey(relation)){
						temp = relationToHavingConds.get(relation);
					}

					temp.add(n);			
					relationToHavingConds.put(relation, temp);
				}
				Node right=n.getRight();
				if(right!=null && !right.getType().equals(Node.getValType())){

					relation="";
					if(right.getType().equals(Node.getAggrNodeType())){				
						if(right.getAgg().getAggExp() != null && right.getAgg().getAggExp().getTable() != null){				
							//relation = right.getAgg().getAggExp().getTable().getTableName();
							relation = right.getAgg().getAggExp().getTableNameNo();
						}
					} else{
						if(right.getTable() != null)
							//relation = right.getTable().getTableName();
							relation = right.getTableNameNo();
					}				
					if(relation.isEmpty())
						continue;

					ArrayList<Node> temp = new ArrayList<Node>();

					if(relationToHavingConds.containsKey(relation)){
						temp = relationToHavingConds.get(relation);
					}
					temp.add(n);
					relationToHavingConds.put(relation, temp);					
				}
			}
		}
		return relationToHavingConds;
	}
	
	public static Map<String, ArrayList<Node>>  createRelationToSelectionConditions(ArrayList<Node> selectionConds){
		Map<String, ArrayList<Node>> relationToSelConds = new HashMap<String, ArrayList<Node>>();

		if(selectionConds != null){
			for(Node n : selectionConds){
				Node left = n.getLeft();
				String relation="";
				/*if condition added by mathew on jan 12 2016 to avoid null pointer exception
				* and checks if left node is not of value type and
				* is associated with a column in a table
				*/
				if(left!=null && left.getTable()!=null){
					//relation=left.getTable().getTableName();
					relation=left.getTableNameNo();
					
					if(relation.isEmpty())
						continue;
					
					ArrayList<Node> temp = new ArrayList<Node>();

					if(relationToSelConds.containsKey(relation)){
						temp = relationToSelConds.get(relation);
					}

					temp.add(n);			
					relationToSelConds.put(relation, temp);
				}
				Node right=n.getRight();
				relation="";
				
				/*Following if condition  to avoid null pointer exception
				* and checks if left node is not of value type and
				* is associated with a column in a table
				*/
				if(right!=null && right.getTable()!=null){
					//relation=right.getTable().getTableName();
					relation=right.getTableNameNo();
					if(relation.isEmpty())
						continue;

					ArrayList<Node> temp = new ArrayList<Node>();

					if(relationToSelConds.containsKey(relation)){
						temp = relationToSelConds.get(relation);
					}
					temp.add(n);
					relationToSelConds.put(relation, temp);					
				}
			}
		}
		return relationToSelConds;
	}

	/*
	 * The following method is called after a table is eliminated to update the 
	 * relationsToHavingConditions map. The columns in the old deleted table 
	 * in the map are replaced with equivalent columns 
	 */

	public static Map<String, ArrayList<Node>> updateRelationsToHavingConditions(Set<String> baseTables, String deletedTable, Map<String, ArrayList<Node>> relationToHavingConds, Map<Node, ArrayList<Node>> nodeToEqNodes){
		
		Map<String, ArrayList<Node>> relationToHavingCondsNew=new HashMap<String, ArrayList<Node>>();
		for(Entry<String, ArrayList<Node>> entry: relationToHavingConds.entrySet()){
			for(Node n:entry.getValue()){
				if(n == null)
					continue;

				Node leftNode = n.getLeft();
				Node leftNodeNew = new Node(leftNode);

				String relationNameNo = "";
				String colName = "";
				String relationNameNoNew=entry.getKey();
				
				/*
				 * The following condition makes sures that lhs of the having condition is not a value literal
				 */
				if(leftNode!=null && !leftNode.getType().equals(Node.getValType())){
					/*
					 * checks if lhs of having condition is an aggregated col, in which case table name no and column name are extracted
					 */
					if(leftNode.getType().equals(Node.getAggrNodeType())){				
						if(leftNode.getAgg().getAggExp() != null && 
								leftNode.getAgg().getAggExp().getTableNameNo() != null && leftNode.getAgg().getAggExp().getColumn() != null){				
							//relationName = leftNode.getAgg().getAggExp().getTable().getTableName();
							relationNameNo = leftNode.getAgg().getAggExp().getTableNameNo();
							colName=leftNode.getAgg().getAggExp().getColumn().getColumnName();
						}
					} 
					/*
					 * otherwise it is an standard column, in which case table name and column name are extracted
					 */
					else{
						if(leftNode.getTableNameNo() != null && leftNode.getColumn()!=null){
							//relationName = leftNode.getTable().getTableName();
							relationNameNo = leftNode.getTableNameNo();
							colName=leftNode.getColumn().getColumnName();
						}
					}					
				}
				/*
				 * If the table in the left node is the deleted table
				 */
				if(relationNameNo!=null && colName!=null && relationNameNo.equals(deletedTable)){

					for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
						/*
						 * Search for equivalent column in the NodeToEqNodes Map
						 */
						if(e.getKey().getTableNameNo().equals(relationNameNo) && e.getKey().getColumn().getColumnName().equals(colName))
						{
							for(Node m:e.getValue()){
								/*
								 * The follow condition makes sures that equivalent Node m
								 * has a table name from baseTables (which does not contain deleted 
								 * Table). If the condition is satisfied the new node 
								 * leftNodeNew is updated with the equivalent table name and
								 * column name
								 */
								if(baseTables.contains(m.getTableNameNo())){
									String relationNameNew=m.getTable().getTableName();
									String colNameNew=m.getColumn().getColumnName();
									Column tempCol=new Column(colNameNew,relationNameNew);
									parsing.Table tempTable=new parsing.Table(relationNameNew);
									tempTable.addColumn(tempCol);
									if(leftNodeNew.getType().equals(Node.getAggrNodeType())){				
										leftNodeNew.setTableNameNo(m.getTableNameNo());
										leftNodeNew.setTableAlias(m.getTableAlias());
										leftNodeNew.setTable(tempTable);
										leftNodeNew.setColumn(tempCol);
										if(leftNodeNew.getAgg().getAggExp() != null){				
										leftNodeNew.getAgg().getAggExp().setTable(tempTable);
										leftNodeNew.getAgg().getAggExp().setTableNameNo(m.getTableNameNo());			
										leftNodeNew.getAgg().getAggExp().setColumn(tempCol);
										leftNodeNew.getAgg().getAggExp().setTableAlias(m.getTableAlias());
										break;
										}
									} 
									else{
										leftNodeNew.setTableNameNo(m.getTableNameNo());
										leftNodeNew.setTable(tempTable);
										leftNodeNew.setColumn(tempCol);
										leftNodeNew.setTableAlias(m.getTableAlias());
										break;
									}
								}
							}
						}
					}
				}

				Node rightNode=n.getRight();
				Node rightNodeNew=new Node(rightNode);				
				relationNameNo=null;
				colName=null;
				/*
				 * The following condition makes sures that rhs of the having condition is not a value literal
				 */
				if(rightNode!=null && !rightNode.getType().equals(Node.getValType())){
					/*
					 * checks if rhs of having condition is an aggregated col, in which case table name and column name are extracted
					 */
					if(rightNode.getType().equals(Node.getAggrNodeType())){				
						if(rightNode.getAgg().getAggExp() != null && 
								rightNode.getAgg().getAggExp().getTableNameNo() != null && 
								rightNode.getAgg().getAggExp().getColumn() != null){				
							//relationName = rightNode.getAgg().getAggExp().getTable().getTableName();
							relationNameNo = rightNode.getAgg().getAggExp().getTableNameNo();
							colName=rightNode.getAgg().getAggExp().getColumn().getColumnName();
						}
					} 
					/*
					 * otherwise it is an standard column, in which case table name and column name are extracted
					 */
					else{
						if(rightNode.getTable() != null && rightNode.getColumn()!=null){
							//relationName = rightNode.getTable().getTableName();
							relationNameNo = rightNode.getTableNameNo();
							colName=rightNode.getColumn().getColumnName();
						}
					}					
				}
				/*
				 * If the table in the right node is the deleted table
				 */				
				if(relationNameNo!=null && colName!=null && relationNameNo.equals(deletedTable)){
					for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
						/*
						 * Search for equivalent column in the NodeToEqNodes Map
						 */
						if(e.getKey().getTableNameNo().equals(relationNameNo) && e.getKey().getColumn().getColumnName().equals(colName))
						{
								for(Node m:e.getValue()){
									/*
									 * The follow condition makes sures that equivalent Node m
									 * has a table name from baseTables (which does not contain deleted 
									 * Table). If the condition is satisfied the new node 
									 * RightNodeNew is updated with the equivalent table name and
									 * column name
									 */
									if(baseTables.contains(m.getTableNameNo())){
										String relationNameNew=m.getTable().getTableName();
										String colNameNew=m.getColumn().getColumnName();
										Column tempCol=new Column(colNameNew,relationNameNew);
										parsing.Table tempTable=new parsing.Table(relationNameNew);
										tempTable.addColumn(tempCol);
										
										if(rightNodeNew.getType().equals(Node.getAggrNodeType())){				
											if(rightNodeNew.getAgg().getAggExp() != null){				
											rightNodeNew.getAgg().getAggExp().setTable(tempTable);
											rightNodeNew.getAgg().getAggExp().setTableNameNo(m.getTableNameNo());			
											rightNodeNew.getAgg().getAggExp().setColumn(tempCol);
											rightNodeNew.getAgg().getAggExp().setTableAlias(m.getTableAlias());
											}
											rightNodeNew.setTableNameNo(m.getTableNameNo());
											rightNodeNew.setTable(tempTable);
											rightNodeNew.setColumn(tempCol);
											rightNodeNew.setTableAlias(m.getTableAlias());
											break;
										} 
										else{
											rightNodeNew.setTableNameNo(m.getTableNameNo());
											rightNodeNew.setTable(tempTable);
											rightNodeNew.setColumn(tempCol);
											rightNodeNew.setTableAlias(m.getTableAlias());
											break;
										}
										
									}
								}
							}
						}
					}
				
				/*
				 * If both left hand and right has side are the same columns from the same table
				 * then this skip the creation and insertion of new node
				 */
				if(rightNodeNew!=null && leftNodeNew!=null && rightNodeNew.getColumn()!=null && 
						leftNodeNew.getColumn()!=null && n.getOperator().equals("=") &&
						leftNodeNew.getTableNameNo().equals(rightNodeNew.getTableNameNo()) &&
						leftNodeNew.getColumn().getColumnName().equals(rightNodeNew.getColumn().getColumnName()))
					continue;
				else{
					/*create a new node and insert leftNodeNew and rightNodeNew at its left
					 * and right respectively
					 * */
					Node nodeNew=new Node(n);
					nodeNew.setLeft(leftNodeNew);
					nodeNew.setRight(rightNodeNew);
					ArrayList<Node> temp = new ArrayList<Node>();
					if(relationToHavingCondsNew.containsKey(relationNameNoNew)){
						temp = relationToHavingCondsNew.get(relationNameNoNew);
					}

					temp.add(nodeNew);			
					relationToHavingCondsNew.put(relationNameNoNew, temp);
				}
			}

		}
		return relationToHavingCondsNew;	
	}

	/*
	 * The following method is called after a table is eliminated to update the 
	 * relationsToSelectionConditions map. The columns in the old deleted table 
	 * in the map are replaced with equivalent columns 
	 */

	public static Map<String, ArrayList<Node>> updateRelationsToSelectionConditions(Set<String> baseTables, String deletedTable, Map<String, ArrayList<Node>> relationToSelConds, Map<Node, ArrayList<Node>> nodeToEqNodes){

		Map<String, ArrayList<Node>> relationToSelCondsNew=new HashMap<String, ArrayList<Node>>();
		for(Entry<String, ArrayList<Node>> entry: relationToSelConds.entrySet()){
			for(Node n:entry.getValue()){
				if(n == null)
					continue;

				Node leftNode = n.getLeft();
				Node leftNodeNew = new Node(leftNode);

				String relationNameNo = "";
				String colName = "";
				String relationNameNoNew=entry.getKey();

				if(leftNode!=null){
					relationNameNo=leftNode.getTableNameNo();
					colName=leftNode.getColumn().getColumnName();
					/*
					 * If the table in the left node is the deleted table
					 */
					if(relationNameNo!=null && colName!=null && relationNameNo.equals(deletedTable)){

						for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
							/*
							 * Search for equivalent column in the NodeToEqNodes Map
							 */
							if(e.getKey().getTableNameNo().equals(relationNameNo) && e.getKey().getColumn().getColumnName().equals(colName))
							{
								for(Node m:e.getValue()){
									/*
									 * The follow condition makes sures that equivalent Node m
									 * has a table name from baseTables (which does not contain deleted 
									 * Table). If the condition is satisfied the new node 
									 * leftNodeNew is updated with the equivalent table name and
									 * column name
									 */
									if(baseTables.contains(m.getTableNameNo())){
										String relationNameNew=m.getTable().getTableName();
										String colNameNew=m.getColumn().getColumnName();
										Column tempCol=new Column(colNameNew,relationNameNew);
										parsing.Table tempTable=new parsing.Table(relationNameNew);
										tempTable.addColumn(tempCol);
										leftNodeNew.setTable(tempTable);
										leftNodeNew.setColumn(tempCol);
										leftNodeNew.setTableNameNo(m.getTableNameNo());
										leftNodeNew.setTableAlias(m.getTableAlias());

										break;
									}
								}
							}
						}
					}

				}
				Node rightNode=n.getRight();
				Node rightNodeNew=new Node(rightNode);
				if(rightNode!=null && rightNode.getTableNameNo()!=null && rightNode.getColumn()!=null){
					relationNameNo=rightNode.getTableNameNo();
					colName=rightNode.getColumn().getColumnName();
					/*
					 * If the table in the right node is the deleted table
					 */				
					if(relationNameNo!=null && colName!=null && relationNameNo.equals(deletedTable)){
						for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
							/*
							 * Search for equivalent column in the NodeToEqNodes Map
							 */
							if(e.getKey().getTableNameNo().equals(relationNameNo) && e.getKey().getColumn().getColumnName().equals(colName))
							{
								for(Node m:e.getValue()){
									/*
									 * The follow condition makes sures that equivalent Node m
									 * has a table name from baseTables (which does not contain deleted 
									 * Table). If the condition is satisfied the new node 
									 * RightNodeNew is updated with the equivalent table name and
									 * column name
									 */
									if(baseTables.contains(m.getTableNameNo())){
										String relationNameNew=m.getTable().getTableName();
										String colNameNew=m.getColumn().getColumnName();
										Column tempCol=new Column(colNameNew,relationNameNew);
										parsing.Table tempTable=new parsing.Table(relationNameNew);
										tempTable.addColumn(tempCol);
										rightNodeNew.setTable(tempTable);
										rightNodeNew.setColumn(tempCol);
										rightNodeNew.setTableNameNo(m.getTableNameNo());												
										rightNodeNew.setTableAlias(m.getTableAlias());
										break;
									}
								}
							}
						}
					}

				}
				/*
				 * If both left hand and right has side are the same columns from the same table
				 * then this skip the creation and insertion of new node
				 */
				if(rightNodeNew.getColumn()!=null && leftNodeNew.getColumn()!=null &&
						rightNodeNew.getTableNameNo()!=null && leftNodeNew.getTableNameNo()!=null && n.getOperator().equals("=") &&
						leftNodeNew.getTableNameNo().equals(rightNodeNew.getTableNameNo()) &&
						leftNodeNew.getColumn().getColumnName().equals(rightNodeNew.getColumn().getColumnName()))
					continue;
				else{
					/*create a new node and insert leftNodeNew and rightNodeNew at its left
					 * and right respectively
					 * */
					Node nodeNew=new Node(n);
					nodeNew.setLeft(leftNodeNew);
					nodeNew.setRight(rightNodeNew);
					ArrayList<Node> temp = new ArrayList<Node>();
					if(relationToSelCondsNew.containsKey(relationNameNoNew)){
						temp = relationToSelCondsNew.get(relationNameNoNew);
					}

					temp.add(nodeNew);			
					relationToSelCondsNew.put(relationNameNoNew, temp);
				}
			}

		}
		return relationToSelCondsNew;	
	}

	public static Map<String, ArrayList<Node>> updateRelationsToGroupByColumns(Set<String> baseTables, String deletedTable, Map<String, ArrayList<Node>> relationToGroupByCols, Map<Node, ArrayList<Node>>nodeToEqNodes){
		Map<String, ArrayList<Node>> relationToGroupByColsNew=new HashMap<String, ArrayList<Node>>();

		for(Entry<String, ArrayList<Node>> entry: relationToGroupByCols.entrySet()){
			if(entry.getKey().equals(deletedTable)){
				ArrayList<Node> groupByCols=entry.getValue();

				for(Node n:groupByCols){
					if(n == null)
						continue;

					//String relationName = "";
					String relationNameNo = "";
					String colName = "";

					if(n.getTable() != null){
						relationNameNo = n.getTableNameNo();
						colName=n.getColumn().getColumnName();
						if(relationNameNo!=null && colName!=null){								
							for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
								if(e.getKey().getTableNameNo().equals(relationNameNo) && e.getKey().getColumn().getColumnName().equals(colName))
								{
									/*
									 * find an equivalent column c from a table other than the deletedTable
									 *  and replaced the nodes  table and column with c's table and column
									 */
									for(Node m:e.getValue()){
										String relationNameNew=m.getTable().getTableName();
										String relationNameNoNew=m.getTableNameNo();
										if(baseTables.contains(relationNameNew)){
											String colNameNew=m.getColumn().getColumnName();

											Node nodeNew=new Node(n);
											Column tempCol=new Column(colNameNew,relationNameNew);
											parsing.Table tempTable=new parsing.Table(relationNameNew);
											tempTable.addColumn(tempCol);
											nodeNew.setTable(tempTable);
											nodeNew.setColumn(tempCol);
											nodeNew.setTableNameNo(m.getTableNameNo());			
											nodeNew.setTableAlias(m.getTableAlias());

											ArrayList<Node> temp = new ArrayList<Node>();

											if(relationToGroupByColsNew.containsKey(relationNameNoNew)){
												temp = relationToGroupByColsNew.get(relationNameNoNew);
											}

											temp.add(nodeNew);			
											relationToGroupByColsNew.put(relationNameNoNew, temp);
											break;
										}
									}
								}
							}
						}

					}
				}

			}
			else{
				relationToGroupByColsNew.put(entry.getKey(), entry.getValue());
			}

		}

		return relationToGroupByColsNew;
	}

	/*
	 * The following method is called after a table is eliminated to update the 
	 * relationsToProjectionColumns map. The columns in the old deleted table 
	 * in the map are replaced with equivalent columns 
	 */

	public static Map<String, ArrayList<Node>> updateRelationsToProjectedColumns(Set<String> baseTables, String deletedTable, Map<String, ArrayList<Node>> relationToProjCols, Map<Node, 
			ArrayList<Node>> nodeToEqNodes) throws CloneNotSupportedException{
		Map<String, ArrayList<Node>> relationToProjColsNew=new HashMap<String, ArrayList<Node>>();

		for(Entry<String, ArrayList<Node>> entry: relationToProjCols.entrySet()){
			if(entry.getKey().equals(deletedTable)){

				ArrayList<Node> projCols=entry.getValue();

				for(Node n:projCols){
					if(n == null)
						continue;

					String relationNameNo = "";
					String colName = "";

					/*
					 * If the node is a aggregated projection column
					 * 
					 */
					if(n.getType().equals(Node.getAggrNodeType())){				
						if(n.getAgg().getAggExp() != null &&  n.getAgg().getAggExp().getTableNameNo() != null){				
							relationNameNo = n.getAgg().getAggExp().getTableNameNo();
							colName=n.getAgg().getAggExp().getColumn().getColumnName();
							if(relationNameNo!=null && colName!=null){
								for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
									if(e.getKey().getTableNameNo().equals(relationNameNo) && e.getKey().getColumn().getColumnName().equals(colName))
									{
										/*
										 * find an equivalent column c from a table other than the deletedTable
										 *  and replaced the nodes  table and column with c's table and column
										 */
										for(Node m:e.getValue()){
											String relationNameNoNew=m.getTableNameNo();
											String relationNameNew=m.getTable().getTableName();
											if(baseTables.contains(relationNameNoNew)){
												String colNameNew=m.getColumn().getColumnName();
												Node nodeNew=new Node(n);
												
												Column tempCol=new Column(colNameNew,relationNameNew);
												parsing.Table tempTable=new parsing.Table(relationNameNew);
												tempTable.addColumn(tempCol);

												nodeNew.getAgg().getAggExp().setTable(tempTable);
												nodeNew.getAgg().getAggExp().setColumn(tempCol);
												nodeNew.getAgg().getAggExp().setTableNameNo(m.getTableNameNo());																								
												nodeNew.getAgg().getAggExp().setTableAlias(m.getTableAlias());
												
												nodeNew.setTable(tempTable);
												nodeNew.setColumn(tempCol);
												nodeNew.setTableNameNo(m.getTableNameNo());
												nodeNew.setTableAlias(m.getTableAlias());


												ArrayList<Node> temp = new ArrayList<Node>();
												if(relationToProjColsNew.containsKey(relationNameNoNew)){
													temp = relationToProjColsNew.get(relationNameNoNew);
												}

												temp.add(nodeNew);			
												relationToProjColsNew.put(relationNameNoNew, temp);
												break;
											}
										}
									}
								}
							}
						}
					} 
					/* Otherwise the node is a standard projection condition */
					else
					{
						if(n.getTable() != null){
							relationNameNo = n.getTableNameNo();
							colName=n.getColumn().getColumnName();
							if(relationNameNo!=null && colName!=null){								
								for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
									if(e.getKey().getTableNameNo().equals(relationNameNo) && e.getKey().getColumn().getColumnName().equals(colName))
									{
										/*
										 * find an equivalent column c from a table other than the deletedTable
										 *  and replaced the nodes  table and column with c's table and column
										 */
										for(Node m:e.getValue()){
											String relationNameNoNew=m.getTableNameNo();
											if(baseTables.contains(relationNameNoNew)){

												Node nodeNew=new Node(n);
												nodeNew.setTable(m.getTable());
												nodeNew.setTableAlias(m.getTableAlias());
												nodeNew.setColumn(m.getColumn());
												nodeNew.setTableNameNo(m.getTableNameNo());		
												ArrayList<Node> temp = new ArrayList<Node>();

												if(relationToProjColsNew.containsKey(relationNameNoNew)){
													temp = relationToProjColsNew.get(relationNameNoNew);
												}

												temp.add(nodeNew);			
												relationToProjColsNew.put(relationNameNoNew, temp);
												break;
											}
										}
									}
								}
							}

						}
					}

				}
			}
			else{
				relationToProjColsNew.put(entry.getKey(), entry.getValue());
			}

		}
		return relationToProjColsNew;
	}

	
	/* the following function determines the existence of equivalent column c1 for every
	 column c2 in having conditions over refTable s.t. c1 belongs to a table different from refTable*/
	public static boolean existsAlternateEquivalentHavingConditions(String refTable, Map<String, ArrayList<Node>> relationToHavingConds, Map<Node,ArrayList<Node>> nodeToEqNodes, Set<String> baseTables){

		boolean altEqHavingFlag=true;
		if(relationToHavingConds.containsKey(refTable)){
			ArrayList<Node> havingConds=relationToHavingConds.get(refTable);
			for(Node havingCond:havingConds){
				boolean foundFlag=false;
				Node left=havingCond.getLeft();
				/*
				 * The following condition makes sures that lhs of the having condition is not a value literal
				 */
				if(left!=null && !left.getType().equals(Node.getValType())){
					Column	havingColumn=null;
					String havingTableNameNo=null;
					/*
					 * checks if lhs of having condition is an aggregated col, in which case table name and column name are extracted
					 */
					if(left.getType().equals(Node.getAggrNodeType())){				
						if(left.getAgg().getAggExp() != null &&  left.getAgg().getAggExp().getColumn() != null){		
							havingColumn=left.getAgg().getAggExp().getColumn();		
							havingTableNameNo=left.getAgg().getAggExp().getTableNameNo();
						}
					} 
					/*
					 * Otherwise lhs of having condition is a standard col, in which case table name and column name are extracted
					 */
					else{
						havingTableNameNo=left.getTableNameNo();
						if(left.getColumn()!=null)
							havingColumn=left.getColumn();
					}
					if(havingTableNameNo.equals(refTable)){
						foundFlag=false;
						/*
						 *  Check for the existence of equivalent column with a table that is different from having column's
						 */
						for(Entry<Node, ArrayList<Node>> entry: nodeToEqNodes.entrySet()){
							if(entry.getKey().getColumn().getColumnName().equals(havingColumn.getColumnName()) && entry.getKey().getTableNameNo().equals(havingTableNameNo)){
								boolean diffTableFound=false;
								for(Node eqNode:entry.getValue()){
									String eqTableNameNo=eqNode.getTableNameNo();
									if(!baseTables.contains(eqTableNameNo))
										continue;
									if(!eqTableNameNo.equals(havingTableNameNo)){
										diffTableFound=true;
										break;
									}
								}
								if(diffTableFound){
									foundFlag=true;
									break;
								}
							}

						}						
					}
				}

				Node right=havingCond.getRight();
				/*
				 * The following condition makes sures that rhs of the having condition is not a value literal
				 */

				if(right!=null && !right.getType().equals(Node.getValType())){
					Column	havingColumn=null;
					String havingTableNameNo=null;
					/*
					 * checks if rhs of having condition is an aggregated col, in which case table name and column name are extracted
					 */

					if(right.getType().equals(Node.getAggrNodeType())){				
						if(right.getAgg().getAggExp() != null &&  right.getAgg().getAggExp().getColumn() != null){	
							havingColumn=right.getAgg().getAggExp().getColumn();
							havingTableNameNo=right.getAgg().getAggExp().getTableNameNo();
						}
					} 
					/*
					 * Otherwise rhs of having condition is a standard col, in which case table name and column name are extracted
					 */

					else{
						havingTableNameNo=right.getTableNameNo();
						if(right.getColumn()!=null)
							havingColumn=right.getColumn();
					}
					if(havingTableNameNo.equals(refTable)){
						foundFlag=false;

						/*
						 *  Check for the existence of equivalent column with a table that is different from having column's
						 */
						for(Entry<Node, ArrayList<Node>> entry: nodeToEqNodes.entrySet()){
							if(entry.getKey().getColumn().getColumnName().equals(havingColumn.getColumnName()) && 
									entry.getKey().getTableNameNo().equals(havingTableNameNo))
							{
								boolean diffTableFound=false;
								for(Node eqNode:entry.getValue()){
									String eqTableNameNo=eqNode.getTableNameNo();
									if(!baseTables.contains(eqTableNameNo))
										continue;
									if(!eqTableNameNo.equals(havingTableNameNo)){
										diffTableFound=true;
										break;
									}
								}
								if(diffTableFound){
									foundFlag=true;
									break;
								}
							}

						}
					}
				}

				if(!foundFlag){
					altEqHavingFlag=false;
					break;
				}
			}		
		}
		return altEqHavingFlag;

	}

	/* the following function determines the existence of equivalent column c1 for every
	sel/proj column c2 from refTable s.t. c1 belongs to a table different from refTable*/
	public static boolean existsAlternateEquivalentSelectionConditions(String refTable, Map<String, ArrayList<Node>> relationToSelConds, Map<Node,ArrayList<Node>> nodeToEqNodes, Set<String> baseTables){

		boolean altEqSelFlag=true;
		if(relationToSelConds.containsKey(refTable)){
			ArrayList<Node> selConds=relationToSelConds.get(refTable);
			for(Node selCond:selConds){
				boolean foundFlag=true;
				Node left=selCond.getLeft();
				if(left!=null && left.getColumn()!=null){
					Column	selColumn=left.getColumn();
					if(left.getTableNameNo().equals(refTable)){
						foundFlag=false;
						
						for(Entry<Node, ArrayList<Node>> entry: nodeToEqNodes.entrySet()){
							if(entry.getKey().getColumn().getColumnName().equals(selColumn.getColumnName()) &&
									entry.getKey().getTableNameNo().equals(left.getTableNameNo()))
							{
								boolean diffTableFound=false;
								for(Node eqNode:entry.getValue()){
									String eqTableNameNo=eqNode.getTableNameNo();
									if(!baseTables.contains(eqTableNameNo))
										continue;
									if(!eqTableNameNo.equals(left.getTableNameNo())){
										diffTableFound=true;
										break;
									}
								}
								if(diffTableFound){
									foundFlag=true;
									break;
								}
							}

						}						
					}
				}
				if(!foundFlag){
					altEqSelFlag=false;
					break;
				}


				Node right=selCond.getRight();
				if(right!=null && right.getColumn()!=null){
					Column	selColumn=right.getColumn();
					/* Resets found flag to false if right node's table name is the same as the refTable,
					 *  as one need to find an alternate column with a different table name 
					 */
					if(right.getTableNameNo().equals(refTable)){
						foundFlag=false;
						for(Entry<Node, ArrayList<Node>> entry: nodeToEqNodes.entrySet()){
							if(entry.getKey().getColumn().getColumnName().equals(selColumn.getColumnName()) &&
									entry.getKey().getTableNameNo().equals(right.getTableNameNo()))
							{
								boolean diffTableFound=false;
								for(Node eqNode:entry.getValue()){
									String eqTableNameNo=eqNode.getTableNameNo();
									if(!baseTables.contains(eqTableNameNo))
										continue;
									if(!eqTableNameNo.equals(right.getTableNameNo())){
										diffTableFound=true;
										break;
									}
								}
								if(diffTableFound){
									foundFlag=true;
									break;
								}
							}

						}	
					}
				}

				if(!foundFlag){
					altEqSelFlag=false;
					break;
				}
			}		
		}
		return altEqSelFlag;

	}
	
	/* returns true iff both nodeA and nodeB represent the same columns
	 * 
	 */
	private static boolean isEquivalentColumns(Node nodeA, Node nodeB) {
		// TODO Auto-generated method stub
		if(nodeA.toString().equalsIgnoreCase(nodeB.toString()))
			return true;
		else
			return false;
	}

	/* the following function determines the existence of column c1 equivalent to column  c2, for every
	projection condition over c2 from refTable s.t. c1 belongs to a table different from refTable*/

	public static boolean existsAlternateEquivalentProjections(String refTable, Map<String, ArrayList<Node>> relationToProjCols, Map<Node,ArrayList<Node>> nodeToEqNodes, Set<String> baseTables){

		boolean altEqProjFlag=true;

		if(relationToProjCols.containsKey(refTable)){
			ArrayList<Node> projCols=relationToProjCols.get(refTable);
			for(Node projCol:projCols){
				boolean foundFlag=false;
				for(Entry<Node, ArrayList<Node>> entry: nodeToEqNodes.entrySet()){
					if(entry.getKey().getColumn().getColumnName().equals(projCol.getColumn().getColumnName())){
						boolean diffTableFound=false;
						for(Node eqNode:entry.getValue()){
							String eqTableNameNo=eqNode.getTableNameNo();
							if(!baseTables.contains(eqTableNameNo))
								continue;
							if(!eqTableNameNo.equals(projCol.getTableNameNo())){
								diffTableFound=true;
								break;
							}
						}
						if(diffTableFound){
							foundFlag=true;
							break;
						}
					}
				}
				if(!foundFlag){
					altEqProjFlag=false;
					break;
				}

			}
		}
		return altEqProjFlag;
	}

	/* the following function determines the existence of column c1 equivalent to column  c2, for every
	grouping column over c2 from refTable s.t. c1 belongs to a table different from refTable*/

	public static boolean existsAlternateEquivalentGroupByCols(String refTable, Map<String, ArrayList<Node>> relationToGroupByCols, Map<Node,ArrayList<Node>> nodeToEqNodes, Set<String> baseTables){

		boolean altEqGroupByFlag=true;

		if(relationToGroupByCols.containsKey(refTable)){
			ArrayList<Node> groupByCols=relationToGroupByCols.get(refTable);
			for(Node groupByCol:groupByCols){
				boolean foundFlag=false;
				for(Entry<Node, ArrayList<Node>> entry: nodeToEqNodes.entrySet()){
					if(entry.getKey().getColumn().getColumnName().equals(groupByCol.getColumn().getColumnName())){
						boolean diffTableFound=false;
						for(Node eqNode:entry.getValue()){
							String eqTableNameNo=eqNode.getTableNameNo();
							if(!baseTables.contains(eqTableNameNo))
								continue;
							if(!eqTableNameNo.equals(groupByCol.getTableNameNo())){
								diffTableFound=true;
								break;
							}
						}
						if(diffTableFound){
							foundFlag=true;
							break;
						}
					}
				}
				if(!foundFlag){
					altEqGroupByFlag=false;
					break;
				}

			}
		}
		return altEqGroupByFlag;
	}


	/*
	 * Returns the foreign key with referencing table relation1 and referenced table relation 2
	 * from the set of foreign keys foreignKeys
	 */	
	public static ForeignKey getForeignKey(String relation1, String relation2, ArrayList<ForeignKey> foreignKeys){
		for(ForeignKey fk : foreignKeys){
			if((fk.getFKTablename().equals(relation1) && fk.getReferenceTable().getTableName().equals(relation2))){
				return fk;
			}
		}				
		return null;
	}

	/*
	 * Removes the foreign key with relation as referencing or referenced table 
	 * from the set of foreign keys foreignKeys
	 */	

	public static ArrayList<ForeignKey> removeForeignKey(String relation, ArrayList<ForeignKey> foreignKeys){	
		ArrayList<ForeignKey> fksCopy=new ArrayList<ForeignKey>();
		for(ForeignKey fk : foreignKeys){
			if(!fk.getFKTablename().equals(relation) && !fk.getReferenceTable().getTableName().equals(relation))
				fksCopy.add(fk);
			else
				logger.info(" Foreign key removed with table: "+relation);
		}
		return fksCopy;
	}
}
