package generateConstraints;

import java.sql.Types;
import java.util.*;

import parsing.CaseCondition;
import parsing.Column;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * Common methods for dealing with nodes
 * @author mahesh
 *
 */
public class UtilsRelatedToNode {

	/**
	 * Check whether this Node has conditions which invovle Strings
	 * @param n--condition
	 * @param flag
	 * @return
	 * @throws Exception
	 */

	public static boolean isStringSelection(Node n,int flag) throws Exception{
		if(n.getLeft().getType().equals(Node.getColRefType())/* */){
			if(flag==1  && !n.getRight().getType().equals(Node.getValType()))
				return false;
			
			int i=n.getLeft().getColumn().getDataType();
			if(i== Types.VARCHAR || i==Types.CHAR || i==Types.LONGVARCHAR){
				if(flag ==0 && n.getRight().getType().equals(Node.getColRefType())){
					return false;
				}
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Used to get number of groups for the query block in which this node is present
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static int getNoOfGroupsForThisNode(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n) {
		
		/** If it is from clause sub query*/
		if(n.getQueryType() == 1)
			if(queryBlock.getFromClauseSubQueries() != null && queryBlock.getFromClauseSubQueries().size() != 0)
				return queryBlock.getFromClauseSubQueries().get(n.getQueryIndex()).getNoOfGroups();
			else
				return queryBlock.getNoOfGroups();
		
		/** If this is where clause subquery node */
		else if (n.getQueryType() == 2)
			if(queryBlock.getWhereClauseSubQueries() != null && queryBlock.getWhereClauseSubQueries().size() != 0)
				return queryBlock.getWhereClauseSubQueries().get(n.getQueryIndex()).getNoOfGroups();
			else
				return queryBlock.getNoOfGroups();
		return queryBlock.getNoOfGroups();
	}
	
	/**
	 * This method is used to get index of this where clause subquery block 
	 * @param n
	 * @return
	 */
	public static int getQueryIndexOfSubQNode(Node n) {
		if(n == null)
			return -1; 
		int index;
		/** get the index of this where clause subquery */
		if(n.getType().equals(Node.getNotExistsNodeType()) || n.getType().equals(Node.getExistsNodeType())){
			
			if( n.getLhsRhs() != null)
				index = n.getLhsRhs().getQueryIndex();
			else
				if(n.getSubQueryConds() != null && n.getSubQueryConds().firstElement() != null 
				&& n.getSubQueryConds().firstElement().getRight() != null){
					
					index = n.getSubQueryConds().firstElement().getRight().getQueryIndex();/**for NOT IN Type*/
				}
				else{
					index = n.getQueryIndex();
				}
		}
		else if(n.getLhsRhs() != null && n.getLhsRhs().getType().equals(Node.getAggrNodeType()))
			index = n.getLhsRhs().getQueryIndex();
		else if(n.getType() != null && n.getType().equals(Node.getBroNodeSubQType()))
			index = n.getQueryIndex();
		else
			if(n.getLhsRhs() != null && n.getLhsRhs().getRight() != null){
				index = n.getLhsRhs().getRight().getQueryIndex();
			}else{
				index = n.getLhsRhs().getQueryIndex();
			}
		return index;
	}
	
	/**
	 * Used to get number of tuples for the relation to which the given node corresponds
	 * This does not include the number of groups, if the input node corresponds to subquery blocks
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static int getNoOfTuplesForThisNode(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node node) throws Exception{
		
		/**If this node corresponds to from clause nested subquery block */
		if(node.getQueryType() == 1)
			/**If there are from clause nested subqueries, 
			 * it means this node is in outer query block but the relation used is inside the from clause nested subquery block*/
			if(queryBlock.getFromClauseSubQueries() != null && queryBlock.getFromClauseSubQueries().size() != 0)
				/** We should get the number of groups of this from clause subquery block */
				return queryBlock.getFromClauseSubQueries().get(node.getQueryIndex()).getNoOfGroups();
			else/** Means this node is inside from clause subquery block */
				return cvc.getNoOfTuples().get(node.getTableNameNo());
		else /**If this node is inside where clause subqueries or in outer query block then just get the number of tuples of that relation only*/
			return cvc.getNoOfTuples().get(node.getTableNameNo());
		
	}
	
	/**
	 * Find the different relations involved in pred. Pred might be an arbitrary predicate 
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static HashMap<String,Table> getListOfRelationsFromNode(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n){
		HashMap<String, Table> rels = new HashMap<String, Table>();

		if(n.getType() != null && n.getType().equalsIgnoreCase(Node.getColRefType())){
			if(!rels.containsKey(n.getTableAlias())){
				rels.put(n.getTableAlias(), n.getTable());
			}
		}
		else{
			if(n.getLeft() != null)
				rels.putAll(getListOfRelationsFromNode(cvc, queryBlock, n.getLeft()));
			if(n.getRight() != null)
				rels.putAll(getListOfRelationsFromNode(cvc, queryBlock, n.getRight()));
		}	
		return rels;
	}
	
	public static int getMaxCountForPredAgg(GenerateCVC1 cvc, Node n){
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return cvc.getNoOfOutputTuples().get(n.getColumn().getTableName());/**FIXME: Handle repeated relations */
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			return 0;
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) || 
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			int a = getMaxCountForPredAgg(cvc, n.getLeft());
			int b = getMaxCountForPredAgg(cvc, n.getRight());
			if(a > b) return a; 
			return b;
		}
		return 0;		
	}
	
	
	/**
	 * Get all the having clause mutations
	 * @param havingClause
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Node> getHavingMutations(Node havingClause) throws Exception {
	
		ArrayList<Node> mutantList = new ArrayList<Node>();
		
		mutantList = getMutants(havingClause,havingClause);
	
		return mutantList;
	}
	
	/** 
	 * Actual method that gets all the having clause mutations
	 * @param havingClause
	 * @param pointer
	 * @return
	 * @throws Exception
	 */

	public static ArrayList<Node> getMutants(Node havingClause,Object pointer) throws Exception{
		
		ArrayList<Node> list= new ArrayList<Node>();
		if(!(((Node)pointer).getNodeType().equalsIgnoreCase(Node.getBroNodeType() )))
			if( ((Node)pointer).getLeft()!=null){
				list.addAll(getMutants(havingClause,((Node)pointer).getLeft()));
			}
		if( ((Node)pointer).getNodeType().equalsIgnoreCase(Node.getBroNodeType() )){

			Node copy=havingClause.clone();
			Node clone=((Node)pointer).clone();
			String opr=((Node)pointer).getOperator();
			Node mut=new Node();

			((Node)pointer).setOperator(">");
			mut=havingClause.clone();
			list.add(mut.clone());

			((Node)pointer).setOperator("<");
			mut=havingClause.clone();
			list.add(mut);

			((Node)pointer).setOperator("=");
			mut=havingClause.clone();
			list.add(mut);

			((Node)pointer).setOperator("/=");
			mut=havingClause.clone();
			list.add(mut);

			//Make original operator
			((Node)pointer).setOperator(opr);
		}

		if(!(((Node)pointer).getNodeType().equalsIgnoreCase(Node.getBroNodeType() )))
			if( ((Node)pointer).getRight()!=null){
				list.addAll(getMutants(havingClause,((Node)pointer).getRight()));
			}

		return list;
	}
	
	
	/**
	 * Finding the Maximum And Minum Values for a given column
	 * @param c-column
	 * @param conds
	 * @return
	 */
	
	
	public static int[] getMaxMinForIntCol(Column c,Vector<Node> conds){
		int[] maxMin=new int[2];
		maxMin[0]=(int)c.getMaxVal();
		maxMin[1]=(int)c.getMinVal();
		for(Node temp:conds){
			Node l=temp.getLeft();
			Node r=temp.getRight();
			System.out.println("r.getStrConst() ====  "+r.getStrConst());
			String regex = "[0-9]+";
			if(l.getTable().getTableName().equalsIgnoreCase(c.getTableName())
					&& l.getColumn().getColumnName().equalsIgnoreCase(c.getColumnName())
					&& r.getStrConst()!=null && (r.getStrConst()).matches(regex)){
				int num=Integer.parseInt(r.getStrConst());
				if(temp.getOperator().equalsIgnoreCase("<")){				
					if(maxMin[0]>num-1)
						maxMin[0]=num-1;
				}
				else if(temp.getOperator().equalsIgnoreCase("<=")){	
					if(maxMin[0]>num)
						maxMin[0]=num;
				}
				else if(temp.getOperator().equalsIgnoreCase(">")){
					if(maxMin[1]<num+1)
						maxMin[1]=num+1;
				}
				else if(temp.getOperator().equalsIgnoreCase(">=")){
					if(maxMin[1]<num)
						maxMin[1]=num;
				}
				else if(temp.getOperator().equalsIgnoreCase("=")){
					maxMin[1]=num;
					maxMin[0]=num;
					return maxMin;
				}
			}
		}
		return maxMin;
	}

	/**
	 * Finding the Maximum And Minum Values for a given column
	 * @param c-column
	 * @param conds
	 * @return
	 */
	
	
	public static int[] getMaxMinForHaving(Column c,Node n){
		int[] maxMin=new int[2];
		maxMin[0]=(int)c.getMaxVal();
		maxMin[1]=(int)c.getMinVal();
		if(n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			if(n.getLeft() != null && n.getRight() != null){
				
				
				Node l=n.getLeft();
				Node r=n.getRight();
				
				maxMin[1] = getExpressionValue(n,"MIN");
				maxMin[0] = getExpressionValue(n,"MAX");
				
				System.out.println("r.getStrConst() ====  "+r.getStrConst()+" --exprVal--"+ maxMin[0]+maxMin[1]);
				String regex = "[0-9]+";
				if(l != null && r != null
						&& (getTableName(l)).getTableName().equalsIgnoreCase(c.getTableName())
						&& (getColumnName(l)).equalsIgnoreCase(c.getColumnName())
						&& r.getStrConst()!=null && (r.getStrConst()).matches(regex)){
					int num=Integer.parseInt(r.getStrConst());
					//int num = exprVal;
					if(n.getOperator().equalsIgnoreCase("<")){				
						if(maxMin[0]>num-1)
							maxMin[0]=num-1;
					}
					else if(n.getOperator().equalsIgnoreCase("<=")){	
						if(maxMin[0]>num)
							maxMin[0]=num;
					}
					else if(n.getOperator().equalsIgnoreCase(">")){
						if(maxMin[1]<num+1)
							maxMin[1]=num+1;
					}
					else if(n.getOperator().equalsIgnoreCase(">=")){
						if(maxMin[1]<num)
							maxMin[1]=num;
					}
					else if(n.getOperator().equalsIgnoreCase("=")){
						maxMin[1]=num;
						maxMin[0]=num;
						return maxMin;
					}
				}
			}
		}
		return maxMin;
	}

	
	/**
	 * Checks whether the given node involves any column from the given relation name
	 * @param cond
	 * @param tableName
	 * @return
	 */
	public static boolean checkIfCorrespondToThisTable(Node cond,	String tableName) {
		
		if(cond == null)
			return false;
		
		boolean present = false;
		if( cond.getTable() != null && cond.getTable().getTableName().equalsIgnoreCase(tableName))
			present = true;
		
		/**if left side is present check if left side correspond to this table*/
		if(cond.getLeft() != null)
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableName);
		
		/** check for right side*/
		if( present == false && cond.getRight() != null) //getLeft??
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableName);
		
		if( present == false && cond.getLhsRhs() != null)
			present = checkIfCorrespondToThisTable(cond.getLhsRhs(), tableName);
	
		return present;
	}
	
	/**
	 * Checks whether the given node involves any column from the given relation occurrence
	 * @param cond
	 * @param tableNameNo
	 * @return
	 */
	public static boolean checkIfCorrespondToThisTableOccurrence(Node cond,	String tableNameNo) {
		
		if(cond == null)
			return false;
		
		boolean present = false;
		
		/**if left side is present check if left side correspond to this table*/
		if(cond.getLeft() != null)
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableNameNo);
		
		/** check for right side*/
		if( present == false && cond.getRight() != null)
			present = checkIfCorrespondToThisTable(cond.getLeft(), tableNameNo);
		
		if( present == false && cond.getLhsRhs() != null)
			present = checkIfCorrespondToThisTable(cond.getLhsRhs(), tableNameNo);
		
		if( present == false && cond.getTableNameNo() != null && cond.getTableNameNo().equalsIgnoreCase(tableNameNo))
			present = true;
		
		return present;
	}

	public static Vector<Node> getJoinConditions(Vector<Vector<Node>> eqClass) {
	
		Vector<Node> eqJoin = new Vector<Node>();
	
	
		for(Vector<Node> ec: eqClass)
			eqJoin.addAll(UtilsRelatedToNode.getJoinCondition(ec));
	
		return eqJoin;
	}

	public static Vector<Node> getJoinCondition(Vector<Node> ec) {
	
		Vector<Node> eqJoin = new Vector<Node>();
	
		for(int i=0; i<ec.size(); i++)
			for(int j = i+1; j<ec.size(); j++)
				eqJoin.add(UtilsRelatedToNode.createJoinNode(ec.get(i), ec.get(j)));
		return eqJoin;
	
	}

	public static Node createJoinNode(Node node1, Node node2) {
	
		Node join = new Node();
		join.setLeft(node1);
		join.setRight(node2);
		join.setOperator( "=");
		return join;
	}

	/**
	 * Gets the like mutations for the given like condition
	 * @param likeCond
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<ArrayList<Node>> getLikeMutations(Node likeCond) throws Exception{
		
		ArrayList<ArrayList<Node>> likeMutants = new ArrayList<ArrayList<Node>>();
		
		ArrayList<Node> d1=new ArrayList<Node>();
		Node lcm = null;
		lcm = likeCond.clone();
		lcm.setOperator("~");
		d1.add(lcm);
		likeMutants.add(d1);
		
		
		ArrayList<Node> d2=new ArrayList<Node>();
		lcm = likeCond.clone();
		lcm.setOperator("i~");
		d2.add(lcm);
		lcm= likeCond.clone();
		lcm.setOperator("!~");
		d2.add(lcm);
		likeMutants.add(d2);
		
		ArrayList<Node> d3=new ArrayList<Node>();
		lcm = likeCond.clone();
		lcm.setOperator("!i~");
		d3.add(lcm);
		lcm = likeCond.clone();
		lcm.setOperator("!~");
		d3.add(lcm);
		
		return likeMutants;
	}
	
	/**
	 * Gets the like pattern mutations for the given like condition
	 * @param likeCond
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Node> getLikePatternMutations(Node likeCond) throws Exception{
		
		ArrayList<Node> patternMutants = new ArrayList<Node>();
		
		Node lcm = null;
		String pattern= likeCond.getRight().getStrConst();
		int l=pattern.length();
		
		for(int i=0;i<l;i++) {
			if(pattern.charAt(i)=='_'){
				lcm = likeCond.clone();
				String newPattern=pattern.substring(0,i)+pattern.substring(i+1,l);
				lcm.getRight().setStrConst(newPattern);
				patternMutants.add(lcm);
			} else if(pattern.charAt(i)=='%') {
				lcm = likeCond.clone();
				String newPattern=pattern.substring(0,i)+"__"+pattern.substring(i+1,l);
				lcm.getRight().setStrConst(newPattern);
				patternMutants.add(lcm);
			}
		}
		
		return patternMutants;
	}

	/**
	 * Used to get mutations for the given selection condition
	 * @param selectionCond
	 * @return
	 */
	public static Vector<Node> getSelectionCondMutations(Node selectionCond) throws Exception{
		
		Vector<Node> scMutants = new Vector<Node>();
		
		Node scm = null;
		scm = selectionCond.clone();
		scm.setOperator("=");					
		scMutants.add(scm);
	
		scm = selectionCond.clone();
		scm.setOperator("<");
		scMutants.add(scm);
	
		scm = selectionCond.clone();
		scm.setOperator(">");
		scMutants.add(scm);
	
		scm = selectionCond.clone();
		scm.setOperator("/=");
		scMutants.add(scm);
		
		scm = selectionCond.clone();
		scm.setOperator("=");
		scm.setIsMutant(true);
		Node right = scm.getRight();
		Node left = scm.getLeft();
		int scale = 0;
		if(left.getType() != null && left.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			scale = getScale(left);
		}else{
			 left.getColumn().getScale();
		}
		
		int epsilon = (int) Math.pow(10, scale);
		String strConst = right.getStrConst();
		strConst = "(" + strConst + " + 1/" + epsilon + ")";
		right.setStrConst(strConst);
		scMutants.add(scm);
		
		scm = selectionCond.clone();
		scm.setOperator("=");
		scm.setIsMutant(true);
		right = scm.getRight();
		left = scm.getLeft();
		scale = 0;
		if(left.getType() != null && left.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			scale = getScale(left);
		}else{
			 left.getColumn().getScale();
		}
		epsilon = (int) Math.pow(10, scale);
		strConst = right.getStrConst();
		strConst = "(" + strConst + " - 1/" + epsilon + ")";
		right.setStrConst(strConst);
		scMutants.add(scm);

		return scMutants;
	}

	/**
	 * Used to get mutations for the given selection condition
	 * @param stringCond
	 * @return
	 */
	public static Vector<Node> getStringSelectionCondMutations(Node stringCond) throws Exception{
		
		Vector<Node> scMutants = new Vector<Node>();
		
		Node scm = null;
		scm = stringCond.clone();
		scm.setOperator("=");					
		scMutants.add(scm);
	
		scm = stringCond.clone();
		scm.setOperator("<");
		scMutants.add(scm);
	
		scm = stringCond.clone();
		scm.setOperator(">");
		scMutants.add(scm);
	
		scm = stringCond.clone();
		scm.setOperator("/=");
		scMutants.add(scm);
		
		return scMutants;
	}

	/**
	 * Used to get mutations for the given case conditions
	 * @param CaseCondition
	 * @return
	 */
	public static Vector<CaseCondition> getCaseCondMutations(CaseCondition caseCond) throws Exception{
		
		Vector<CaseCondition> scMutants = new Vector<CaseCondition>();
		
		CaseCondition scm = new CaseCondition();
		if(scm.caseOperator != null){
			scm = caseCond.clone();
			scm.caseOperator = "=";					
			scMutants.add(scm);
		 
			scm = caseCond.clone();
			scm.caseOperator ="<";
			scMutants.add(scm);
		
			scm = caseCond.clone();
			scm.caseOperator =">";
			scMutants.add(scm);
		
			scm = caseCond.clone();
			scm.caseOperator = "/=";
			scMutants.add(scm);
			
			scm = caseCond.clone();
			scm.caseOperator = "=";
			//(scm.getCaseConditionNode()).setIsMutant(true);
			Node right = scm.getWhenNode().getRight();
			Node left = scm.getWhenNode().getLeft();
			int scale = 0;
			if(left.getType() != null && left.getType().equalsIgnoreCase(Node.getBaoNodeType())){
				scale = getScale(left);
			}else{
				 left.getColumn().getScale();
			}
			
			int epsilon = (int) Math.pow(10, scale);
			String strConst = right.getStrConst();
			strConst = "(" + strConst + " + 1/" + epsilon + ")";
			right.setStrConst(strConst);
			scMutants.add(scm);
			
			scm = caseCond.clone();
			scm.caseOperator ="=";
			//(scm.getCaseConditionNode()).setIsMutant(true);
			right = (scm.getWhenNode()).getRight();
			left = (scm.getWhenNode()).getLeft();
			scale = 0;
			if(left.getType() != null && left.getType().equalsIgnoreCase(Node.getBaoNodeType())){
				scale = getScale(left);
			}else{
				 left.getColumn().getScale();
			}
			epsilon = (int) Math.pow(10, scale);
			strConst = right.getStrConst();
			strConst = "(" + strConst + " - 1/" + epsilon + ")";
			right.setStrConst(strConst);
			scMutants.add(scm);
		}
		return scMutants;
	}

	
	
	/**
	 * check if this node corresponds to the given relation occurrence
	 * @param n
	 * @param col
	 * @param relationNo
	 * @return
	 */
	public static boolean presentNode(Node n, Column col, String relationNo) {
		
		boolean present = false;
		
		if( n.getLeft() != null && n.getLeft().getTableNameNo() != null)
			present = present || presentNode(n.getLeft(), col, relationNo);
		
		if( present == false && n.getRight() != null && n.getRight().getTableNameNo() != null)
			present = present || presentNode(n.getRight(), col, relationNo);
		
		if( present == false && n.getTableNameNo() != null){
			
			if(n.getTableNameNo().equalsIgnoreCase(relationNo) && 
					n.getColumn().getColumnName().equalsIgnoreCase(col.getColumnName()) )
				present = true;
		}
		return present;
	}

	/**
	 * Checks if the given column of the relation occurrence is present in the given conditions
	 * @param conds
	 * @param col
	 * @param relationNo
	 * @return
	 */
	public static boolean presentInConds(Vector<Node> conds, Column col, String relationNo) {
		
		/**for each node*/
		for(Node n: conds)
			if( presentNode(n, col, relationNo))
				return true;
		return false;
	}
	/**
	 * This method returns the scale of a column value in aggregate that holds expression on the 
	 * column.
	 * 
	 * @param n1
	 * @return
	 */
	
	public static int getScale(Node n1){
		if(n1.getRight() != null && n1.getRight().getColumn() != null){
			return n1.getRight().getColumn().getScale();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getColumn() != null){
			return n1.getLeft().getColumn().getScale();	
		}
		else {
			if(n1.getLeft() != null){
				return getScale(n1.getLeft());
			}else if(n1.getRight() != null){
				return getScale(n1.getRight());
			}
		}
		return 0;
	}
	
	/**
	 * This method returns the tablename of a column value in aggregate/BAO that holds expression on the 
	 * column.
	 * 
	 * @param n1
	 * @return
	 */
	
	public static Table getTableName(Node n1){
		if(n1.getRight() != null && n1.getRight().getTable() != null){
			return n1.getRight().getTable();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTable() != null){
			return n1.getLeft().getTable();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableName(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableName(n1.getRight());
			}
		}
		return null;
	}
	/**
	 * This method returns the column name of a column value in aggregate that holds expression on the 
	 * column.
	 * 
	 * @param n1
	 * @return
	 */
	
	public static String getColumnName(Node n1){
		if(n1.getRight() != null && n1.getRight().getColumn() != null){
			return n1.getRight().getColumn().getColumnName();	
		}	
		else if(n1.getLeft() != null && n1.getLeft().getColumn() != null){
			return n1.getLeft().getColumn().getColumnName();	
		}
		else {
			if(n1.getLeft() != null){
				return getColumnName(n1.getLeft());
			}else if(n1.getRight() != null){
				return getColumnName(n1.getRight());
			}
		}
		return null;
	}
	
	/**
	 * This method returns the column value in aggregate that holds expression on the 
	 * column.
	 * 
	 * @param n1
	 * @return
	 */
	
	public static Column getColumn(Node n1){
		if(n1.getRight() != null && n1.getRight().getColumn() != null){
			return n1.getRight().getColumn();	
		}	
		else if(n1.getLeft() != null && n1.getLeft().getColumn() != null){
			return n1.getLeft().getColumn();	
		}
		else {
			if(n1.getLeft() != null){
				return getColumn(n1.getLeft());
			}else if(n1.getRight() != null){
				return getColumn(n1.getRight());
			}
		}
		return null;
	}
	
	
	/**
	 * This method returns the column name of a column value in aggregate that holds expression on the 
	 * column.
	 * 
	 * @param n1
	 * @return
	 */
	
	public static String getTableNameNo(Node n1){
		if(n1.getRight() != null && n1.getRight().getTableNameNo() != null){
			return n1.getRight().getTableNameNo();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTableNameNo() != null){
			return n1.getLeft().getTableNameNo();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableNameNo(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableNameNo(n1.getRight());
			}
		}
		return null;
	}
	
	/**
	 * Get the expression value of aggregate node for estimating number of tuples
	 * 
	 * @param n
	 * @param maxOrMin
	 * @return
	 */
	public static  int getExpressionValue(Node n, String maxOrMin){
		int i=0, expr=0;
		
		if(n.getType().equalsIgnoreCase(Node.getValType())){
				return Integer.parseInt(n.toString());
						
		}else if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			if(maxOrMin.equalsIgnoreCase("MIN")){
				return (int) n.getColumn().getMinVal();
			}else if(maxOrMin.equalsIgnoreCase("MAX")){
				return (int) n.getColumn().getMaxVal();
			}
			
		}else if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			return getExpressionValue(n.getAgg().getAggExp(), maxOrMin);
		}
		else if(n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			if(n.getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				i = getExprVal(n.getLeft(),n.getOperator(),n.getRight(), maxOrMin);
				
			}else if(n.getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				i = getExprVal(n.getRight(),n.getOperator(),n.getLeft(),maxOrMin);
			}
			else{
				
					return getExpressionValue(n.getLeft(),maxOrMin);
				
				
			}
			//if(n.getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())){
			//	return (getExpressionValue(n.getLeft())+n.getLeft().getOperator()+getExpressionValue(n.getRight()));
			/*}else{
				return getExpressionValue(n.getRight());
			}*/
		}
		else{
			i = getExprVal(n.getLeft(),n.getOperator(),n.getRight(),maxOrMin);
			
		}
		/*if(left.getType().equalsIgnoreCase(Node.getValType())){
			return Integer.parseInt(rt.toString());
		}
		else if(left.getType().equalsIgnoreCase(Node.getColRefType())){
			return (int) rt.getColumn().getMinVal();
			
		}else if(left.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			return getExpressionValue(left.getAgg().getAggExp().getRight(), rt.getAgg().getAggExp().getOperator(),left.getAgg().getAggExp().getLeft());
			
		}
		else if(left.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			expr = getExpressionValue(left.getRight(), left.getOperator(), left.getLeft()); 
		}
		else{
			i = 
		}*/
		return i;
	}
	
	public static int getExprVal(Node left,String operator,Node right,String maxOrMin){
		int i = 0;
		if(right.getType().equalsIgnoreCase(Node.getValType())){
			
			if(left.getOperator().equalsIgnoreCase("+")){
				i = getExpressionValue(left, maxOrMin)+Integer.parseInt(right.toString());
			}else if(left.getOperator().equalsIgnoreCase("-")){
				i = getExpressionValue(left, maxOrMin)-Integer.parseInt(right.toString());
			}else if(left.getOperator().equalsIgnoreCase("*")){
				i = getExpressionValue(left, maxOrMin)*Integer.parseInt(right.toString());
			}else if(left.getOperator().equalsIgnoreCase("/")){
				i = getExpressionValue(left,maxOrMin)/Integer.parseInt(right.toString());
			}
		}
		else if(left.getType().equalsIgnoreCase(Node.getValType())){
			
			if(left.getOperator().equalsIgnoreCase("+")){
				i = Integer.parseInt(left.toString())+getExpressionValue(right,maxOrMin);
			}else if(left.getOperator().equalsIgnoreCase("-")){
				i = Integer.parseInt(left.toString())-getExpressionValue(right,maxOrMin);
			}else if(left.getOperator().equalsIgnoreCase("*")){
				i = Integer.parseInt(left.toString())*getExpressionValue(right,maxOrMin);
			}else if(left.getOperator().equalsIgnoreCase("/")){
				i = Integer.parseInt(left.toString())/getExpressionValue(right,maxOrMin);
			}
		}
		
		
		return i;
		}
	
}
