/**
@author Bharath
@author Mathew
*/

package partialMarking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ForeignKey;
import parsing.JoinClauseInfo;
import parsing.Node;

public class CanonicalizeQuery {
	
	private static Logger logger=Logger.getLogger(CanonicalizeQuery.class.getName());
	
	/**
	 * This method canonicalizes the selection conditions, having clause conditions and projection conditions.
	 * This method also rearranges the left and right nodes alphabetically so that the conditions can be compared. 
	 * @param query
	 */
	public static void Canonicalize(QueryData queryData) throws Exception{		
		minimizeOuterJoins(queryData);	
		EliminateRedundantRelation.EliminateRedundantRelations(queryData);
		canonicalizeSelectionConditions(queryData);
		canonicalizeJoinConditions(queryData);
		canonicalizeProjectionSelectionToEquivalent(queryData);
		canonicalizeGroupBy(queryData);	
		canonicalizeHavingClause(queryData);
		canonicalizeOrderBy(queryData);
		canonicalizeDistinct(queryData);
		//EliminateRedundantRelation.EliminateRedundantRelations(queryData);		
		queryData.reAdjustJoins();
	}
	


	/** Written by Mathew on 22 April 2016
	 * 
	 * transforms every join condition to a condition of the form
	 *  Col1 <joinOp> Col2 
	 * such that  Col1 lexicographically appears before Col2 
	 */
	private static void canonicalizeJoinConditions(QueryData qd){
		if(qd != null && qd.WhereClauseQueries != null){
			for(QueryData q : qd.WhereClauseQueries){
				canonicalizeJoinConditions(q);
			}
		}
		if(qd != null && qd.FromClauseQueries != null){
			for(QueryData q : qd.FromClauseQueries){
				canonicalizeJoinConditions(q);
			}
		}
		if(qd != null && qd.getJoinConditions() != null){
			ArrayList<Node> joinConditions = qd.getJoinConditions();
			if(joinConditions != null){
				for(Node n: joinConditions){
					if(n.getJoinType()!=null&&n.getJoinType().equals(JoinClauseInfo.leftOuterJoin))
						continue;
					if(n.getJoinType()!=null&&n.getJoinType().equals(JoinClauseInfo.rightOuterJoin))
						continue;
					if(n.getJoinType()!=null&&n.getJoinType().equals(JoinClauseInfo.fullOuterJoin))
						continue;
					if(isNotOrdered(n)){
						Node leftNode=n.getLeft();
						Node rightNode=n.getRight();
						n.setLeft(rightNode);
						n.setRight(leftNode);

						if(n.getOperator().equals(">")||n.getOperator().equals("<")||
								n.getOperator().equals(">=")||n.getOperator().equals("<=")){
							invertNodeOperator(n);						
						}
					}

				}
			}
		}	
	}
	
	/** @author mathew on April 22 2016
	 * 
	 * returns true if the parameter node is an atomic join condition
	 * and if its operands are not lexicographically ordered
	 */
	public static boolean isNotOrdered(Node n){
		Node leftNode=n.getLeft();
		Node rightNode=n.getRight();
		if(leftNode==null||rightNode==null)
			return false;
		if(!leftNode.getType().equals(Node.getColRefType())||!rightNode.getType().equals(Node.getColRefType()))
			return false;
		String leftTableName=leftNode.getTable().getTableName();
		String rightTableName=rightNode.getTable().getTableName();
		if(leftTableName.compareTo(rightTableName)>0)
			return true;
		else if(leftTableName.compareTo(rightTableName)==0){
			String leftColName=leftNode.getColumn().getColumnName();
			String rightColName=rightNode.getColumn().getColumnName();
			if(leftColName.compareTo(rightColName)>0)
				return true;
		}
		
		return false;
	}

	
	/** @author Bharath, mathew
	 * 
	 *  converts the strictly greater than/less than operators
	 *  in atomic having conditions of selection types (one operand is a value)  to 
	 *  their non strict versions. 
	 */	private static void canonicalizeSelectionConditions(QueryData qd){
		if(qd != null && qd.WhereClauseQueries != null){
			for(QueryData q : qd.WhereClauseQueries){
				canonicalizeSelectionConditions(q);
			}
		}
		if(qd != null && qd.FromClauseQueries != null){
			for(QueryData q : qd.FromClauseQueries){
				canonicalizeSelectionConditions(q);
			}
		}
		
		if(qd != null && qd.getSelectionConditions() != null){
		ArrayList<Node> selectionConds = qd.getSelectionConditions();
		if(selectionConds != null){
		for(Node n: selectionConds){
		    checkBinaryHavingClauseNodes(n);
			checkSelectionHavingClauseNodes(n);
			
			// If selection condition involves two column references, interchange left/right to make it in sorted order.
//			if(n.getLeft().getNodeType().equals(Node.getColRefType()) && n.getRight().getNodeType().equals(Node.getColRefType())){
//				List<Node> temp = new Vector<Node>();
//				temp.add(n.getLeft());
//				temp.add(n.getRight());
//				Collections.sort(temp, new NodeComparator());
//				n.setLeft(temp.get(0));
//				n.setRight(temp.get(1));
//			}
			logger.info("after node"+n);
		}
		}
		}
	}
	
	
	private static void canonicalizeProjectionSelectionToEquivalent(QueryData qd){
		if(qd != null && qd.WhereClauseQueries != null){
		for(QueryData q : qd.WhereClauseQueries){
			canonicalizeProjectionSelectionToEquivalent(q);
		}
		}
		if(qd != null && qd.FromClauseQueries != null){
		for(QueryData q : qd.FromClauseQueries){
			canonicalizeProjectionSelectionToEquivalent(q);
		}}
		
		if(qd!= null && qd.getProjectionList()!= null){
			ArrayList<Node> projectionList = qd.getProjectionList();
		
		ArrayList<Node> selectionConds = qd.getSelectionConditions();
		
		Vector<Vector<Node>> eqClasses = qd.getEqClasses();
		
		Map<Node, Node > nodeToEqNode = new HashMap<Node, Node>();
				
		for(int i = 0; i < eqClasses.size(); i++){
			List<Node> eq = eqClasses.get(i);
			Collections.sort(eq, new NodeComparator());
		}		

		for(Vector<Node> t : eqClasses){
			
			if(t.size() == 0)
				continue;
			
			Node eqNode = t.get(0);
			
			for(int j = 0; j < t.size(); j++){
				nodeToEqNode.put(t.get(j), eqNode);
			}
		}
		
		for(int i = 0; i < projectionList.size(); i++){
			Node temp = projectionList.get(i);
			if(nodeToEqNode.containsKey(temp)){
				Node eqNode = nodeToEqNode.get(temp);
				temp.setTable(eqNode.getTable());
				temp.setTableAlias(eqNode.getTableAlias());
				temp.setColumn(eqNode.getColumn());
				temp.setTableNameNo(eqNode.getTableNameNo());				
			}
		}
		
		for(int i = 0; i < selectionConds.size(); i++){
			Node temp = selectionConds.get(i).getLeft();
			if(nodeToEqNode.containsKey(temp) && !selectionConds.get(i).getRight().equals(nodeToEqNode.get(temp))){
				Node eqNode = nodeToEqNode.get(temp);
				temp.setTable(eqNode.getTable());
				temp.setTableAlias(eqNode.getTableAlias());
				temp.setColumn(eqNode.getColumn());
				temp.setTableNameNo(eqNode.getTableNameNo());				
			}
		}
		}
	}
	
	
	private static void canonicalizeHavingClause(QueryData qd){
	
		if(qd != null && qd.WhereClauseQueries != null){
		for(QueryData q : qd.WhereClauseQueries){
			canonicalizeHavingClause(q);
		}
		}
		if(qd != null && qd.FromClauseQueries != null){
		for(QueryData q : qd.FromClauseQueries){
			canonicalizeHavingClause(q);
		}
		}
		
		//Node n = qd.getHavingClause();
		if(qd != null && qd.getHavingClause() != null){
			ArrayList <Node> havingClauses = qd.getHavingClause();
		
		for(Node n: havingClauses){
			
		
		if(n.getNodeType().equals(Node.getBroNodeType())){
			    checkBinaryHavingClauseNodes(n);
				checkSelectionHavingClauseNodes(n);
			
		}else if(n.getNodeType().equals(Node.getAndNodeType())){
			
			checkSelectionHavingClauseNodes(n.getLeft());
			checkBinaryHavingClauseNodes(n.getLeft());
			checkSelectionHavingClauseNodes(n.getRight());
			checkBinaryHavingClauseNodes(n.getRight());
		}
			// If having condition involves two column references, interchange left/right to make it in sorted order.
//			if(n.getLeft().getNodeType().equals(Node.getColRefType()) && n.getRight().getNodeType().equals(Node.getColRefType())){
//				List<Node> temp = new Vector<Node>();
//				temp.add(n.getLeft());
//				temp.add(n.getRight());
//				Collections.sort(temp, new NodeComparator());
//				n.setLeft(temp.get(0));
//				n.setRight(temp.get(1));
//			}
		}	
		}
	}
	
	private static void checkBinaryHavingClauseNodes(Node n){
			if(isNotOrdered(n)){
				Node leftNode=n.getLeft();
				Node rightNode=n.getRight();
				invertNodeOperator(n);
				n.setLeft(rightNode);
				n.setRight(leftNode);
			}
	}

	/** @author mathew on 24 April 2016
	 * replaces the operator of an atomic Join/Having condition
	 * with the corresponding inverse, so that the left and right
	 * operands can be ordered lexicographically for canonicalization 
	 */
	private static void invertNodeOperator(Node n){
		if(n.getOperator().equals(">")){
			n.setOperator("<");		
		}
		else if(n.getOperator().equals("<")){
			n.setOperator(">");
		}
		else if(n.getOperator().equals(">=")){
			n.setOperator("<=");
		}
		else if(n.getOperator().equals("<=")){
			n.setOperator(">=");
		}
	}

	/** @author mathew on 24 April 2016
	 *  is the renaming of the old method checkSelectionHavingClauseNodes
	 *  converts the strictly greater than/less than operators
	 *  in atomic having conditions of selection types (one operand is a value)  to 
	 *  their non strict versions. 
	 */
	private static void checkSelectionHavingClauseNodes(Node n){
		Node left = n.getLeft();
		Node right = n.getRight();
		//logger.info("Before node n"+n+" "+left.getNodeType()+" "+right.getNodeType());
		if(!left.getNodeType().equals(Node.getValType())&&
				!right.getNodeType().equals(Node.getValType()))
			return;
		
		if(left.getNodeType().equals(Node.getValType())){
			invertNodeOperator(n);
			n.setLeft(right);
			n.setRight(left);
		}
		left = n.getLeft();
		right = n.getRight();
		try{
			Integer.parseInt(right.getStrConst());
		}
		catch(Exception e){
			return;
		}

		if((n.getOperator().equals(">") || n.getOperator().equals("<"))){
			int scale=0;
			if(left.getType()!=null &&left.getType().equals(Node.getAggrNodeType())){
				if(left.getAgg()!=null && left.getAgg().getAggExp()!=null && left.getAgg().getAggExp().getColumn()!=null)
					scale=left.getAgg().getAggExp().getColumn().getScale();
			}
			else{ 
				if(left.getColumn()!=null)
					scale = left.getColumn().getScale();			
			}
			if(n.getOperator().equals(">") && right.getNodeType().equals(Node.getValType())){
				n.setOperator(">=");
				if(scale == 0){
					Integer value = Integer.parseInt(right.getStrConst());
					value = value + 1;

					right.setStrConst(value.toString());
				} else {
					Double value = Double.parseDouble(right.getStrConst());
					value = value + Math.pow(10, -1*scale);

					right.setStrConst(value.toString());
				}

			} else if(n.getOperator().equals("<") && right.getNodeType().equals(Node.getValType())){
				n.setOperator("<=");
				if(scale == 0){
					Integer value = Integer.parseInt(right.getStrConst());
					value = value - 1;

					right.setStrConst(value.toString());
				} else {
					Double value = Double.parseDouble(right.getStrConst());
					value = value - Math.pow(10, -1*scale);

					right.setStrConst(value.toString());
				}
		}
		}
		logger.info("After "+left.getNodeType()+" "+right);
	}

	/** @author mathew on 28 May
	 * The following method removes any redundant distinct qualifier from
	 * the project clause of the query
	 * */
	public static void canonicalizeDistinct(QueryData qData) {
		// TODO Auto-generated method stub
		if(!qData.hasDistinct)
			return;	

		Set<String> satisfyingTableNos=new HashSet<String>();
		Set<String> joinTableNameNos=new HashSet<String>();
		
		/* first finds the set of tables whose primary keys are in the
		 * project clause and stores in the set satisfyingTableNos. Note that 
		 * if primary keys of all the join tables 
		 * are present in the project clause, then the distinct qualifier 
		 * is trivally  redundant
		 */
		for(String tableNameNo:qData.getJoinTables()){
			joinTableNameNos.add(tableNameNo);
			String tableName=tableNameNo.substring(0, tableNameNo.length()-1);
			//System.out.println("TableName: "+tableName);
			Vector<Column> primKey=qData.WholeData.getTableMap().getTable(tableName).getPrimaryKey();
			if(primKeyContainedInProjectedColumns(primKey,qData)){
				//System.out.println("primKey found"+primKey);
				satisfyingTableNos.add(tableNameNo);
			}
		}
			//System.out.println("Satisfying tables before augmentation"+satisfyingTableNos);
			ArrayList<ForeignKey> foreignKeys = qData.WholeData.getForeignKeysModified();
			Map<String,HashMap<String,ArrayList<Pair>>> relationToRelationEqNodes=qData.getRelationToRelationEquivalentNodes();

			/* Now it checks if the every join table that are not 
			 * contained in the satisfyingTables set is a referenced relation.
			 * A referenced relation is relation to which a foreign key reference
			 * exists for a table in the satisfyingTables list s.t. 
			 * equi-join conditions exists between the corresponding columns
			 * of refering table and the referenced table. Any such referenced table 
			 * is added to the set of satisfying tables. The do while loop below
			 * computes the least fix point of the set of satisfying tables.
			 */
			List<String> referencedRelations=null;
			do{
				if(referencedRelations!=null)
					satisfyingTableNos.addAll(referencedRelations);
				referencedRelations=QueryData.getReferencedRelations(joinTableNameNos, 
						null, foreignKeys, relationToRelationEqNodes);
				referencedRelations.removeAll(satisfyingTableNos);
			}
			while(referencedRelations!=null && !referencedRelations.isEmpty());
			//System.out.println("Satisfying tables after augmentation"+satisfyingTableNos);
			if(satisfyingTableNos.containsAll(joinTableNameNos)){
				qData.hasDistinct=false;
				System.out.println("join tables are contained in satisfying tables");
			}	
	}

	/** @author mathew on 29 May 2016
	 * The method checks if every column in the input vector primKey
	 * or a member of its equivalence classe is present in the 
	 * projectlist of qData
	 */
	private static boolean primKeyContainedInProjectedColumns(Vector<Column> primKey, QueryData qData) {
		ArrayList<Node> projectedColumns=qData.getProjectionList();
		Map<Node,ArrayList<Node>>nodeToEqNodes=qData.getNodeToEquivalentNodes();
		//iterates through the set of columns in primKey
		for(Column pkColumn:primKey){
			boolean colFound=false;
			//iterates through the set of projected columns
			for(Node projColumn:projectedColumns){
				//checks if the table name and the column name of the
				//column in primKey matches with a projected column
				if(pkColumn.getTableName().equals(projColumn.getTable().getTableName())
						&& pkColumn.getColumnName().equals(projColumn.getColumn().getColumnName())){
					colFound=true;
					break;
				}
				//checks if the table name and the column name of the
				//column in primKey matches with respective
				//names of any member of the equivalence class of the projected column
				if(nodeToEqNodes.containsKey(projColumn)){
					for(Node eqColumn: nodeToEqNodes.get(projColumn)){
						if(pkColumn.getTableName().equals(eqColumn.getTable().getTableName())&&
								pkColumn.getColumnName().equals(eqColumn.getColumn().getColumnName())){
							colFound=true;
							break;
						}

					}
				}
				if(colFound==true)
					break;				
			}
			if(!colFound)
				return false;
		}
		return true;
	}

	/** @author Mathew on June 3 2016
	 * 
	 */
	public static void minimizeOuterJoins(QueryData qData) {
		if(qData==null||qData.getJoinConditions()==null)
			return;
		// TODO Auto-generated method stub
//		for(Node n:qData.getSelectionConditions()){		
//			if(n.getJoinType()!=null && n.getJoinType().equals(JoinClauseInfo.leftOuterJoin)){
//				Node rightNode=n.getRight();
//				if(existsNullFailingSelectionCondition(rightNode.getTableNameNo(),qData)){
//					n.setJoinType(JoinClauseInfo.innerJoin);
//				}
//			}
//			if(n.getJoinType()!=null && n.getJoinType().equals(JoinClauseInfo.rightOuterJoin)){
//				Node leftNode=n.getLeft();
//	
//				if(existsNullFailingSelectionCondition(leftNode.getTableNameNo(),qData)){
//					n.setJoinType(JoinClauseInfo.innerJoin);
//				}
//			}
//		}
		/* iterate through the set of join conditions
		 * 
		 */
		for(Node n:qData.getJoinConditions()){		
			/*
			 * if the condition is a left join then check if a column from the tableNameNo of 
			 * right node is involved in another inner join  condition or
			 *  a selection condition(, which is null failing). If this holds then
			 *  then the respective left join is equivalent to an inner join 
			 */
			if(n.getJoinType()!=null && n.getJoinType().equals(JoinClauseInfo.leftOuterJoin)){
				Node rightNode=n.getRight();
				if(existsNullFailingSelectionCondition(rightNode.getTableNameNo(),qData)){
					logger.info("converting outer joins to inner joins for node "+n);
					n.setJoinType(JoinClauseInfo.innerJoin);
					qData.setNumberOfOuterJoins(qData.getNumberOfOuterJoins()-1);
					qData.setNumberOfInnerJoins(qData.getNumberOfInnerJoins()+1);
					if(n.getOperator().equals("=")){
						qData.addToEquivalenceClasses(n.getLeft(),n.getRight());
					}
				}
			}
			/*
			 * if the condition is a right join then check if a column from the tableNameNo of 
			 * left node is involved in another inner join  condition or
			 *  a selection condition(, which is null failing). If this holds then
			 *  then the respective right join is equivalent to an inner join 
			 */

			if(n.getJoinType()!=null && n.getJoinType().equals(JoinClauseInfo.rightOuterJoin)){
				Node leftNode=n.getLeft();	
				if(existsNullFailingSelectionCondition(leftNode.getTableNameNo(),qData)){
					logger.info("converting outer joins to inner joins for node "+n);
					n.setJoinType(JoinClauseInfo.innerJoin);
					qData.setNumberOfOuterJoins(qData.getNumberOfOuterJoins()-1);
					qData.setNumberOfInnerJoins(qData.getNumberOfInnerJoins()+1);
					if(n.getOperator().equals("=")){
						qData.addToEquivalenceClasses(n.getLeft(),n.getRight());
					}
				}
			}
		}
	}
	
	/**  @author mathew on June 3 2016
	 *  iterates through the set of join (resp. selection) conditions and checks if 
	 * there is a join (resp. selection) condition which has a left column or a right column
	 * from the input tableNameNo
	 */
	public static boolean existsNullFailingSelectionCondition(String tableNameNo, QueryData qData){
		for(Node m:qData.getJoinConditions()){			
			if(m.getJoinType()!=null && m.getJoinType().equals(JoinClauseInfo.leftOuterJoin))
				continue;
			if(m.getJoinType()!=null && m.getJoinType().equals(JoinClauseInfo.rightOuterJoin))
				continue;
			if(m.getJoinType()!=null && m.getJoinType().equals(JoinClauseInfo.fullOuterJoin))
				continue;
			else {
				Node leftNode=m.getLeft();
				Node rightNode=m.getRight();
				if(leftNode.getNodeType().equals(Node.getColRefType())){
					if(leftNode.getTableNameNo().equals(tableNameNo))
						return true;
				}
				if(rightNode.getNodeType().equals(Node.getColRefType())){
					if( rightNode.getTableNameNo().equals(tableNameNo))
							return true;
				}
				
			}
		}
		for(Node m:qData.getSelectionConditions()){
			if(m.getJoinType()!=null && m.getJoinType().equals(JoinClauseInfo.leftOuterJoin))
				continue;
			if(m.getJoinType()!=null && m.getJoinType().equals(JoinClauseInfo.rightOuterJoin))
				continue;
			if(m.getJoinType()!=null && m.getJoinType().equals(JoinClauseInfo.fullOuterJoin))
				continue;
			else{
				Node leftNode=m.getLeft();
				Node rightNode=m.getRight();
				if(leftNode.getNodeType().equals(Node.getColRefType())){
					if(leftNode.getTableNameNo().equals(tableNameNo))
						return true;
				}
				if(rightNode.getNodeType().equals(Node.getColRefType())){
					if(rightNode.getTableNameNo().equals(tableNameNo))
							return true;
				}
			}
		}
		
		return false;
	}
	
	/** @author mathew on 22 June 2016
	 * 
	 * iterates through attributes from right to left of the order by list. If any attribute 
	 * a is funtionally determined by the rest of attributes in the 
	 * list, a is removed; otherwise a is replaced by the canonical representative of the equivalence class of
	 * a  
	 */
	public static void canonicalizeGroupBy(QueryData qData) {
		if(qData==null||qData.getGroupByNodes()==null)
			return;
		// TODO Auto-generated method stub
		ArrayList<Node> groupByNodes=qData.getGroupByNodes();
		if(groupByNodes.isEmpty())
			return;
		LinkedList<Node> prefix= new LinkedList<Node>();
		ArrayList<Node> suffix= new ArrayList<Node>();
		for(Node n: groupByNodes){
			prefix.add(n);
		}
		
		while(!prefix.isEmpty()){
			Node tailNode=prefix.removeLast();
			Vector<Node> tailVector=new Vector<Node>();
			tailVector.add(tailNode);
			logger.info("prefix :"+" "+prefix+ " tailnode: "+tailNode);
			LinkedList<Node> residue=new LinkedList<Node>();
			residue.addAll(prefix);
			residue.addAll(suffix);
			if(!functionallyDeterminesUptoEquivalence(residue,tailVector,qData, new ArrayList<String>()))
				suffix.add(0,getCanonicalRepresentative(tailNode,qData));
		}
		logger.info("canonicalized group by vector: "+suffix);
		qData.setGroupByNodes(suffix);
	}

	/** @author mathew on 20 June 2016
	 * 
	 * iterates through attributes from right to left of the order by list. If any attribute 
	 * a is funtionally determined by the set of attributes on the left hand side of the 
	 * list, a is removed; otherwise a is replaced by the canonical representative of the equivalence class of
	 * a  
	 */
	public static void canonicalizeOrderBy(QueryData qData) {
		if(qData==null||qData.getOrderByNodes()==null)
			return;
		// TODO Auto-generated method stub
		ArrayList<Node> orderByNodes=qData.getOrderByNodes();
		if(orderByNodes.isEmpty())
			return;
		LinkedList<Node> prefix= new LinkedList<Node>();
		ArrayList<Node> suffix= new ArrayList<Node>();
		for(Node n: orderByNodes){
			prefix.add(n);
		}
		
		while(!prefix.isEmpty()){
			Node tailNode=prefix.removeLast();
			logger.info("prefix :"+" "+prefix+ " tailnode: "+tailNode);
			Vector<Node> tailVector=new Vector<Node>();
			tailVector.add(tailNode);
			if(!functionallyDeterminesUptoEquivalence(prefix,tailVector,qData, new ArrayList<String>()))
				suffix.add(0,getCanonicalRepresentative(tailNode,qData));
		}
		logger.info("canonicalized order by vector: "+suffix);
		qData.setOrderByNodes(suffix);
	}
	
	/** @author mathew on 20 June 2016
	 * 
	 * Returns True if the set of attributes in the vector, key, 
	 * is funtionally determined by the set of attributes in the  
	 * list, prefix
	 */
	private static boolean functionallyDeterminesUptoEquivalence(LinkedList<Node> prefix, Vector<Node> key, 
			QueryData qData, ArrayList<String> visitedTableNameNos) {
		
		// empty set can not deterimine key
		if(prefix.isEmpty()){
			return false;
		}
		Map<Node, ArrayList<Node>> nodeToEqNodes=qData.getNodeToEquivalentNodes();
		
		//get all the equivalent keys thanks to the equivalence classes of attributes in the query
		Set<Vector<Node>> expandedKeys=expandUptoEquivalence(key, nodeToEqNodes);
		Set<Vector<Node>> validKeys=new HashSet<Vector<Node>>();
		
		for(Vector<Node> expKey:expandedKeys){
			// if prefix functionally determines any of the equivalent keys then return True
			if(containsElements(prefix,expKey)){
				return true;
			}
			/* if all the attributes in expKey belong to the same table T and if it T has no been visited before 
			 *  add expKey to validKeys  
			 */
			if(hasSameTableNameNo(expKey) && !visitedTableNameNos.contains(expKey.get(0).getTableNameNo()))
				validKeys.add(expKey);
		}
		logger.info("expanded: valid keys "+expandedKeys +": "+validKeys);

		for(Vector<Node> validKey:validKeys){
			/*extract the primary key columns of the table that contain validKey 
			 *  and store it in the vector primaryKeyCols. Note that if 
			 *   prefix funtionally determines primaryKeyCols, then prefix funtionally 
			 *   determines primaryKeyCols
			 */
			Vector<Column> primaryKeyCols=validKey.get(0).getTable().getPrimaryKey();
			Vector<Node> primaryKeyNodes=convertColumnVectorToNodeVector(primaryKeyCols,validKey.get(0));
			logger.info(" primaryKey"+primaryKeyNodes);
			if(containsElements(prefix,primaryKeyNodes))
				return true;
			else{
				/* prepare for recursion
				 *  checks if the table T containing primaryKeyNodes has already not been visited, 
				 *  if not T is added to the visited list of tables. Recursion proceeds  
				 *   as primaryKeyNodes takes the place of the key
				 */
				if(!visitedTableNameNos.contains(validKey.get(0).getTableNameNo())){
					ArrayList<String> visitedClone=(ArrayList<String>)visitedTableNameNos.clone();
					visitedClone.add(validKey.get(0).getTableNameNo());
					return functionallyDeterminesUptoEquivalence(prefix, primaryKeyNodes, qData, visitedClone);
				}
			}
			
		}
		
		return false;
	}
	
	/** @author mathew on 22 June 2016
	 * Returns true if for every node n1 in key, there exists a node n2 in sourceLists
	 * such that n1 and n2 represent the same column 
	 * 
	 */
	public static boolean containsElements(LinkedList<Node> sourceList, Vector<Node> keys){
		for(Node key:keys){
			boolean foundKey=false;
			for(Node sourceNode:sourceList){
				if(key.getTableNameNo().equalsIgnoreCase(sourceNode.getTableNameNo())&&
						key.getColumn().getColumnName().equalsIgnoreCase(sourceNode.getColumn().getColumnName())){
					foundKey=true;
					break;
				}
			}
			if(!foundKey)
				return false;
		}
		return true;
	}
	
	/** @ author mathew on 22 June 2016
	 * 
	 * Converts a vector of Columns to a Vector of Nodes that represents the same
	 */
	public static Vector<Node> convertColumnVectorToNodeVector(Vector<Column> colVector, Node sameNode){
		Vector<Node> nodeVector=new Vector<Node>();
		for(Column col:colVector){
			Node tempNode=new Node(sameNode);
			tempNode.setTableNameNo(sameNode.getTableNameNo());
			tempNode.setTable(sameNode.getTable());
			tempNode.setTableAlias(sameNode.getTableAlias());
			tempNode.setColumn(col);
			nodeVector.add(tempNode);
		}
		return nodeVector;
	}
	
	/** @ author mathew on 22 June 2016
	 *  Returns True if each node in the vector of nodes attribs
	 *  represents a column from the same table, otherwise returns false
	 */

	public static boolean hasSameTableNameNo(Vector<Node> attribs){
		String tableName="";
		for(Node col:attribs){
			if(tableName!=""){
				String tabName=col.getTableNameNo();
				if(!tabName.equalsIgnoreCase(tableName))
					return false;
			}
			else{
				tableName=col.getTableNameNo();
			}
		}
		return true;
	}

	/** @author mathew on 22 June 2016
	 *  
	 *  Returns the set of all  vectors that are equivalent to the 
	 *  vector of nodes key.
	 */
	private static Set<Vector<Node>> expandUptoEquivalence(Vector<Node> key,
			Map<Node, ArrayList<Node>> nodeToEqNodes) {
		Set<Vector<Node>> expandedKey=new HashSet<Vector<Node>>();
		expandedKey.add(key);
		for(int i=0;i<key.size();i++){
			expandedKey=expandAtIndex(expandedKey,i, nodeToEqNodes);
		}
		return expandedKey;
	}

	/** @author mathew on 22 June 2016
	 *  
	 *  Returns the set of all  vectors that are equivalent to the 
	 *  vector of nodes key by replacing the node c which is the index'th component of key by
	 *  any node c' that is equivalent to c.
	 */
	private static Set<Vector<Node>> expandAtIndex(Set<Vector<Node>> key, int index,
			Map<Node, ArrayList<Node>> nodeToEqNodes) {
		Set<Vector<Node>> expandedKey=new HashSet<Vector<Node>>();
		if(key.isEmpty())
			return key;
		Node nodeToExpand=key.iterator().next().get(index);

		ArrayList<Node> equivNodes=getEquivalentNodes(nodeToExpand, nodeToEqNodes);
		if(equivNodes==null||equivNodes.isEmpty())
			return key;
		for(Vector<Node> attribList:key){
			expandedKey.add(attribList);
			for(Node n: equivNodes){
				Vector<Node> attribListCopy=(Vector<Node>)attribList.clone();
				attribListCopy.removeElementAt(index);
				attribListCopy.add(index, n);
				expandedKey.add(attribListCopy);
			}
		}
		return expandedKey;
	}

	/** @author mathew on 22 June 2016
	 *  
	 *  Returns the set of all  nodes that are equivalent to the 
	 *  node key w.r.t the equivalence classes in the query
	 */
	public static ArrayList<Node> getEquivalentNodes(Node key, Map<Node, ArrayList<Node>> nodeToEqNodes){
		for(Node node: nodeToEqNodes.keySet()){
			if(key.getTableNameNo().equals(node.getTableNameNo())&&
					key.getColumn().getColumnName().equals(node.getColumn().getColumnName()))
				return nodeToEqNodes.get(node);
		}
		return null;
	}

	/** @author mathew on 22 June 2016
	 *  
	 *  Returns the  node c in the equivalence class S of 
	 *  node key s.t. c lexicographically precedes every other node in S
	 */
	public static Node getCanonicalRepresentative(Node n, QueryData qData){
		Map<Node, ArrayList<Node>> nodeToEqNodes=qData.getNodeToEquivalentNodes();
		if(!nodeToEqNodes.containsKey(n))
			return n;
		ArrayList<Node> eqNodes=nodeToEqNodes.get(n);
		if(eqNodes.isEmpty())
			return n;
		Node leastNode=n;
		for(Node m:eqNodes){
			if(m.toString().compareToIgnoreCase(leastNode.toString())<0)
				leastNode=m;
		}
		return leastNode;
	}
}
