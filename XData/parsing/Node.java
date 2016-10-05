package parsing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

import parsing.Table;
import util.*;

public class Node implements Cloneable, Serializable{
	
	private static final long serialVersionUID = -7192918525557389737L;
	private static String valType = "VALUE";
	private static String colRefType = "COLREF";
	private static String andNodeType = "AND NODE";
	private static String orNodeType = "OR NODE";
	private static String notNodeType = "NOT NODE";
	private static String inNodeType = "IN";
	private static String notInNodeType = "NOT IN";
//	private static String allAnyNodeType = "ALL/ANY"; commented by mathew on 29 June 2016
	private static String allNodeType = "ALL";
	private static String anyNodeType = "ANY";
	private static String existsNodeType = "EXISTS";
	private static String notExistsNodeType = "NOT EXISTS";
	private static String broNodeType = "BRO NODE";
	private static String baoNodeType = "BAO NODE";
	private static String isNullNodeType = "IS NULL NODE";
	private static String aggrNodeType = "AGGREGATE NODE";
	private static String paramNodeType = "PARAMETER NODE";
	private static String likeNodeType= "LIKE NODE";   
	private static String broNodeSubQType= "BRO NODE SUBQUERY";
	private static String stringFuncType= "STRING FUNCTION NODE";
	private static String extractFuncType ="EXTRACT NODE";
	private static String caseNodeType ="CASE NODE";
	
	AggregateFunction agg;

	String type;
	String operator; //operator +,-,<,>, etc. Valid only for type NODE

	Table table;	//has value if colRefType
	String tableAlias;
	Column column;	//has value if colRefType
	String strConst;	//has value if valType
	String tableNameNo;
	String joinType;
	boolean isDistinct = false; //has value if colRefType in projection list
	Boolean isMutant;
	
	
	Node lhsRhs; 	//has value if IN NODE
	Vector<Node> subQueryConds; //has value if any sub query node: IN NODE, EXISTS NODE etc.
	CaseCondition CaseConditionNode;
	
	/**
	 * @return the ccNode
	 */
	public CaseCondition getCaseConjunctNode() {
		return CaseConditionNode;
	}
	/**
	 * @param ccNode the ccNode to set
	 */
	public void setCcNode(CaseCondition CaseConditionNode) {
		this.CaseConditionNode = CaseConditionNode;
	}
	Node left;
	Node right;
	//Mahesh
	int queryType;//0-outer query, 1-from subquery, 2-where subquery
	int queryIndex;//Stores the index of sub query (If this node is inside sub query)
	//Mahesh
	public Node(){
		type = null;
		queryType=-1;
		queryIndex=-1;
		isMutant = false;
		subQueryParser=null;
	}
	
	//the following line added by mathew on 1st october 2016
		QueryParser subQueryParser;
		
		public QueryParser getSubQueryParser(){
			return subQueryParser;
		}
		
		public void setSubQueryParser(QueryParser subQP){
			this.subQueryParser=subQP;
		}
	
	/**
	 * Copy constructor. Added by Mahesh
	 * @param functionName
	 * @return
	 */
	public Node(Node n){

		if(n == null){
			return;
		}
		if(n.getAgg() == null)
			this.agg =null;
		else
			this.agg = new AggregateFunction(n.getAgg());
		if(n.getType() == null)
			this.type = null;
		else
			this.type = new String(n.getType());
		if(n.getOperator() == null)
			this.operator = null;
		else
			this.operator = new String(n.getOperator());
		if(n.getTable() == null)
			this.table = null;
		else
			this.table = new Table(n.getTable());
		this.table = n.getTable();
		if(n.getTableAlias() == null)
			this.tableAlias = null;
		else
			this.tableAlias = new String(n.getTableAlias());
		this.tableAlias = n.getTableAlias();
		if(n.getColumn() == null)
			this.column = null;
		else
			this.column = new Column(n.getColumn());
		if(n.getStrConst() == null)
			this.strConst=null;
		else
			this.strConst = new String(n.getStrConst());
		if(n.getTableNameNo() == null)
			this.tableNameNo =null;
		else
			this.tableNameNo = new String(n.getTableNameNo());
		if(n.getJoinType() == null)
			this.joinType = null;
		else
			this.joinType = new String(n.getJoinType());
		this.isDistinct = n.isDistinct;
		if(n.getLhsRhs() == null)
			this.lhsRhs = null;
		else
			this.lhsRhs = new Node(n.getLhsRhs());
		if(n.getSubQueryConds() == null)
			this.subQueryConds = null;
		else{
			this.subQueryConds = new Vector<Node>();
			//Make a deep copy
			for(Node n2: n.getSubQueryConds())
				this.subQueryConds.add(new Node(n2));
		}
		if(n.getLeft() == null)
			this.left =null;
		else
			this.left = new Node(n.getLeft());
		if(n.getRight() == null)
			this.right = null;
		else
			this.right = new Node(n.getRight());

		this.queryIndex = n.getQueryIndex();
		this.queryType = n.getQueryType();
	}

	/*public Node(Node n){

		Node n1 = new Node();

		if(n == null){
			return;
		}

		this.agg = n.getAgg();

		this.type = n.getType();

		this.operator = n.getOperator();
		if(n.getTable() == null)
			this.table = null;
		else
			this.table = new Table(n.getTable());
		this.table = n.getTable();
		if(n.getTableAlias() == null)
			this.tableAlias = null;
		else
			this.tableAlias = new String(n.getTableAlias());
		this.tableAlias = n.getTableAlias();
		if(n.getColumn() == null)
			this.column = null;
		else
			this.column = new Column(n.getColumn());

		this.strConst = n.getStrConst();
		if(n.getTableNameNo() == null)
			this.tableNameNo =null;
		else
			this.tableNameNo = new String(n.getTableNameNo());
		this.joinType = n.getJoinType();
		this.isDistinct = n.isDistinct;

		this.lhsRhs = n.getLhsRhs();

		this.subQueryConds = n.getSubQueryConds();


		this.left = new Node(n.getLeft());

		this.right = new Node(n.getRight());

		this.queryIndex = n.getQueryIndex();
		this.queryType = n.getQueryType();
	}*/

	public boolean nodeContainsAggFunction(String functionName){
		if(this.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){
			if(this.getLeft().getAgg().getFunc().equalsIgnoreCase(functionName)){
				return true;
			}
		}
		else if(this.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
			if(this.getRight().getAgg().getFunc().equalsIgnoreCase(functionName)){
				return true;
			}
		}
		return false;
	}

	public static String getParamNodeType() {
		return paramNodeType;
	}

	public static void setParamNodeType(String paramNodeType) {
		Node.paramNodeType = paramNodeType;
	}

	public static void setAggrNodeType(String aggrNodeType) {
		Node.aggrNodeType = aggrNodeType;
	}

	/**Modified by Mahesh
	 * perform cloning of object
	 * @return
	 */
	/*public Object clone() throws CloneNotSupportedException{
		Object obj = super.clone();
		return obj;
	}*/

	@Override
	public Node clone() throws CloneNotSupportedException{
		/*Node obj=new Node();// = super.clone();
		Node left=new Node(),right=new Node();
		if(this.getLeft() !=null)
			left=(Node)this.getLeft().clone();
		if(this.getRight()!=null)
			right=(Node)this.getRight().clone();
		((Node)obj).setLeft((Node)left);
		((Node)obj).setRight((Node)right);
		return obj;*/
		Object obj= super.clone();
		Node left=new Node();
		Node right= new Node();
		if(this.getLeft() !=null)
			left=this.getLeft().clone(); 
		if(this.getRight()!=null)
			right=this.getRight().clone();
		((Node)obj).setLeft(left);
		((Node)obj).setRight(right);
		return (Node)obj;
	}



	/**
	 * This method creates a node based on the column of table
	 * @param column
	 * @param node
	 * @return node
	 * @throws Exception
	 */
	public static Node createNode(Column c, Table table)  throws Exception{
		Node n1 = new Node();
		n1.setColumn(c);
		n1.setLeft(null);
		n1.setRight(null);
		n1.setTable(table);
		n1.setType(Node.getColRefType());

		return n1;
	}

	public String getTableNameNo() {
		return tableNameNo;
	}

	public void setTableNameNo(String tableNameNo) {
		this.tableNameNo = tableNameNo;
	}
	//change made by Junaid
	public void setJoinType(String joinT){
		joinType = joinT;
	}
	public String getJoinType(){
		return joinType;
	}
	public static String getExistsNodeType() {
		return existsNodeType;
	}

	public static String getNotExistsNodeType() {
		return notExistsNodeType;
	}

//commented by mathew on 29th June 2016
//	public static String getAllAnyNodeType() {
//		return allAnyNodeType;
//	}
	
	public static String getAllNodeType() {
		return allNodeType;
	}

	public static String getAnyNodeType() {
		return anyNodeType;
	}

	public static String getStringFuncNodeType() {
		return stringFuncType;
	}


	public Node getLhsRhs() {
		return lhsRhs;
	}

	public void setLhsRhs(Node lhsRhs) {
		this.lhsRhs = lhsRhs;
	}

	public Vector<Node> getSubQueryConds() {
		return this.subQueryConds;
	}

	public void setSubQueryConds(Vector<Node> subQueryConds) {
		this.subQueryConds = subQueryConds;
	}

	public boolean isDistinct(){
		return isDistinct;
	}
	
	public void setDistinct(boolean isDistinct){
		this.isDistinct = this.isDistinct;
	}
	
	public static String getAggrNodeType() {
		return aggrNodeType;
	}

	public AggregateFunction getAgg() {
		return agg;
	}

	public void setAgg(AggregateFunction agg) {
		this.agg = agg;
	}

	public String getTableAlias() {
		return tableAlias;
	}
	public void setTableAlias(String alias) {
		this.tableAlias = alias;
	}
	public Column getColumn() {
		return column;
	}
	public void setColumn(Column column) {
		this.column = column;
	}
	public Node getLeft() {
		return left;
	}
	public void setLeft(Node left) {
		this.left = left;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	public Node getRight() {
		return right;
	}
	public void setRight(Node right) {
		this.right = right;
	}
	public String getStrConst() {
		return strConst;
	}
	public void setStrConst(String strConst) {
		this.strConst = strConst;
	}
	public Table getTable() {
		return table;
	}
	public void setTable(Table table) {
		this.table = table;
	}
	public static String getColRefType() {
		return colRefType;
	}
	public static String getIsNullNodeType() {
		return isNullNodeType;
	}
	public static String getBroNodeType() {
		return broNodeType;
	}
	public static String getAndNodeType() {
		return andNodeType;
	}
	public static String getInNodeType() {
		return inNodeType;
	}
	public static String getNotNodeType() {
		return notNodeType;
	}

	public String getNodeType() {
		return type;
	}

	public static String getOrNodeType() {
		return orNodeType;
	}
	public static String getValType() {
		return valType;
	}

	public static String getLikeNodeType() {
		return likeNodeType;
	}
	public static String getBroNodeSubQType(){
		return broNodeSubQType;
	}

	public int getQueryType(){
		return queryType;
	}

	public void setQueryType(int type){
		this.queryType = type;
	}

	public int getQueryIndex(){
		return queryIndex;
	}

	public void setQueryIndex(int index){
		this.queryIndex = index;
	}

	public AggregateFunction getAggFuncFromNode(){
		if(this.getType().equalsIgnoreCase(Node.getBroNodeType())){
			if(this.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				return this.getLeft().getAgg();
			}
			else{
				return this.getRight().getAgg();
			}
		}
		return null;
	}

	/*	
	 * ReWrite This - Rewritten. Check it.
	 */ 

	@Override
	public String toString(){
		String retString="";
		if(this.getType()!=null&&this.getType().equalsIgnoreCase(Node.getNotNodeType())){
			retString+=" "+Node.getNotNodeType()+" ";
			if(this.getLeft()!=null){
				retString+=this.getLeft().toString();
			}
			return retString;
		}
		else if(this.getType()!=null&&this.getType().equalsIgnoreCase(Node.getInNodeType())){
			if(this.getLeft()!=null){
				retString+=" "+this.getLeft();
			}
			retString+=" "+Node.getInNodeType()+" ";
			
			if(this.getRight()!=null&&this.getRight().getSubQueryParser()!=null){
				retString+="("+this.getRight().getSubQueryParser().getQuery().getQueryString()+")";;
			}
			return retString;
		}
		else if(this.getType()!=null&& this.getType().equalsIgnoreCase(Node.getAnyNodeType())){
			retString+=" "+Node.getAnyNodeType()+" ";
			if(this.getSubQueryParser()!=null){
				retString+="("+this.getSubQueryParser().getQuery().getQueryString()+")";
			}
			return retString;
		}
		else if(this.getType()!=null&&this.getType().equalsIgnoreCase(Node.getAllNodeType())){
			retString+=" "+Node.getAllNodeType()+" ";
			if(this.getSubQueryParser()!=null){
				retString+="("+this.getSubQueryParser().getQuery().getQueryString()+")";
			}
			return retString;
		}
		 else if(this.getType()!=null&&this.getType().equalsIgnoreCase(Node.getExistsNodeType())){
			retString+=" "+Node.getExistsNodeType()+" ";
			if(this.getSubQueryParser()!=null){
				retString+="("+this.getSubQueryParser().getQuery().getQueryString()+")";
			}
			return retString;
		}
		if(this.getType()!=null&&this.getType().equalsIgnoreCase(Node.getValType())){
			return this.getStrConst();
		}
		else if(this.getType()!=null&&this.getType().equalsIgnoreCase(Node.getColRefType())){
			//return (this.getColumn().getTableName()+"."+this.getColumn().getColumnName());
			return (this.getTableNameNo()+"."+this.getColumn().getColumnName());
		}
		else if(this.getType()!=null&&this.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			return (this.getAgg().getFunc() + "(" + this.getAgg().getAggExp().toString() + ")");
		}
		else if (this.getLeft()!=null&&this.getRight()!=null){
			return "(" + this.getLeft().toString()  + this.getOperator() + this.getRight().toString() + ")";
		}
		else if(this.getOperator()!=null) {
			return this.getOperator();
		}
		else {
			return "";
		}
	}


	public String toCVCString(int base, HashMap<String, String> paramMap){
		if(this.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			return this.getAgg().getFunc();
		}
		else if(this.getType().equalsIgnoreCase(Node.getValType())){ //this value has to be a number
			String s = paramMap.get(this.getStrConst());
			if(s!=null){
				if(s.contains("PARAM"))
					return paramMap.get(this.getStrConst());
				else{
					if(base == 16)
						return Utilities.getHexVal(Integer.parseInt(paramMap.get(this.getStrConst())),5);
					else //if (base == 10)
						return Integer.parseInt(paramMap.get(this.getStrConst())) + "";
				}  
			}

			if(base == 16)
				return Utilities.getHexVal(Integer.parseInt(this.getStrConst()),5);
			else //if (base == 10)
				return Integer.parseInt(this.getStrConst()) + "";
		}
		else{
			if(this.getOperator().equalsIgnoreCase("<")){
				if(base == 16)
					return "BVLT(" + this.getLeft().toCVCString(base, paramMap)  + "," + this.getRight().toCVCString(base, paramMap) + ");";
				else //if(base == 10)
					return this.getLeft().toCVCString(base, paramMap) + this.getOperator() + this.getRight().toCVCString(base, paramMap);
			} 
			else if(this.getOperator().equalsIgnoreCase("<=")){
				if(base == 16)
					return "BVLE(" + this.getLeft().toCVCString(base, paramMap)  + "," + this.getRight().toCVCString(base, paramMap) + ");";
				else //if(base == 10)
					return this.getLeft().toCVCString(base, paramMap) + this.getOperator() + this.getRight().toCVCString(base, paramMap);
			} 
			else if(this.getOperator().equalsIgnoreCase(">")){
				if(base == 16)	
					return "BVGT(" + this.getLeft().toCVCString(base, paramMap)  + "," + this.getRight().toCVCString(base, paramMap) + ");";
				else //if(base == 10)
					return this.getLeft().toCVCString(base, paramMap) + this.getOperator() + this.getRight().toCVCString(base, paramMap);
			} 
			else if(this.getOperator().equalsIgnoreCase(">=")){
				if(base == 16)
					return "BVGE(" + this.getLeft().toCVCString(base, paramMap)  + "," + this.getRight().toCVCString(base, paramMap) + ");";
				else //if(base == 10)
					return this.getLeft().toCVCString(base, paramMap) + this.getOperator() + this.getRight().toCVCString(base, paramMap);
			} 
			else
				return "(" + this.getLeft().toCVCString(base, paramMap)  + this.getOperator() + this.getRight().toCVCString(base, paramMap) + ");";
		}
	}


	public static void printPredicateVector(Vector<Node> v){
		for(int i=0;i< v.size();i++){
			System.out.print(v.get(i).toString()+", ");
		}
	}

	public static String getBaoNodeType() {
		return baoNodeType;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public Boolean getIsMutant() {
		return isMutant;
	}
	public void setIsMutant(Boolean mutant) {
		this.isMutant = mutant;
	}


	//We differenciate between join and selection conditions in case of arithmetic node
	public boolean containsConstant(){
		/*if(this.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			return false;
		}
		else*/ if(this.getType().equalsIgnoreCase(Node.getValType())){
			return true;
		}		
		else if(this.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			return this.getLeft().containsConstant() && this.getRight().containsConstant();
		}
		else if(this.getType().equalsIgnoreCase(Node.getBroNodeType())){
			//if(!this.getLeft().getType().equalsIgnoreCase(Node.aggrNodeType) &&
				//	!this.getRight().getType().equalsIgnoreCase(Node.aggrNodeType)){

			//following if condition added by mathew to avoid null pointer exception
			if(this.getLeft()!=null && this.getRight()!=null)
				return this.getLeft().containsConstant() || this.getRight().containsConstant();
		//	}
				/*else if(this.getLeft().getType().equalsIgnoreCase(Node.aggrNodeType) &&
					!this.getRight().getType().equalsIgnoreCase(Node.aggrNodeType)){
				
				this.setLeft(this.getRight());
				this.getRight().containsConstant();
				
			}else if(this.getRight().getType().equalsIgnoreCase(Node.aggrNodeType) &&
					!this.getLeft().getType().equalsIgnoreCase(Node.aggrNodeType)){
				
				this.setRight(this.getLeft());
				this.getLeft().containsConstant();
				
			}*/
			
		}
		return false;
	}



	public boolean containsLike(){
		if(this.getType().equalsIgnoreCase(Node.getValType())){
			return true;
		}	
		else if(this.getType().equalsIgnoreCase(Node.getLikeNodeType())){
			return this.getLeft().containsLike() || this.getRight().containsLike();
		}
		return false;
	}

	/*
	 * Returns all the AggregateFunction objects within a Node which is an Aggregation constraint
	 */
	public Vector<Node> getAggsFromAggConstraint(){
		if(this==null){
			return null;
		}
		Vector<Node> retVal = new Vector<Node>();
		Node aggCons = this;
		if(aggCons.getType().equalsIgnoreCase(Node.getBroNodeType()) || aggCons.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			Vector<Node> aggs1 = aggCons.getLeft().getAggsFromAggConstraint();
			Vector<Node> aggs2 = aggCons.getRight().getAggsFromAggConstraint();
			if(aggs1!=null) retVal.addAll(aggs1);
			if(aggs2!=null) retVal.addAll(aggs2);
			return retVal;
		}
		else if(aggCons.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			retVal.add(aggCons);
			return retVal;
		}
		else
			return null;
	}

	/*
	 * Returns all the column references from the expression in an Aggregate function
	 */
	public Vector<Column> getColumnsFromNode(){
		if(this.getType().equalsIgnoreCase(Node.getColRefType())){
			Vector<Column> ret = new Vector<Column>();
			ret.add(this.getColumn());
			return ret;
		}
		else if(this.getType().equalsIgnoreCase(Node.getValType())){
			Vector<Column> ret = new Vector<Column>();
			return ret;
		}
		else if(this.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			Vector<Column> ret1 = new Vector<Column>();
			ret1 = this.getLeft().getColumnsFromNode();
			Vector<Column> ret2 = new Vector<Column>();
			ret2 = this.getRight().getColumnsFromNode();
			ret1.addAll(ret2);
			return ret1;
		}
		else return new Vector<Column>();
	}


	/*@Override
	public boolean equals(Object obj) {
	    if(!(obj instanceof Node) && !(obj instanceof String))
	    {
	        return false;
	    }
	    else
	    {
	        if(obj instanceof Node)
	            return func.toLowerCase().equals(((AggregateFunction)obj).getFunc().toLowerCase()) && aggExp.getColumn().toString().toLowerCase().equals(((AggregateFunction)obj).getAggExp().getColumn().toString().toLowerCase()) 
	            		&& aggExp.getColumn().getTableName().toString().toLowerCase().equals(((AggregateFunction)obj).getAggExp().getColumn().getTableName().toString().toLowerCase()) ;
	        	return this.toString().toLowerCase().equals(((Node)obj).toString().toLowerCase());
	        else
	            return this.toString().toLowerCase().equals(((String)obj).toLowerCase());
	    }

	}*/

	/**
	 * Added by Mahesh
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		//		System.out.println("HashCode for node: " + this);
		int result = 1;
		result = prime * result + ((agg == null) ? 0 : agg.hashCode());

		//	System.out.println("agg hashcode: " + ((agg == null) ? 0 : agg.hashCode()));

		result = prime * result + ((column == null) ? 0 : column.hashCode());

		//		System.out.println("Column hashcode: " + ((column == null) ? 0 : column.hashCode()));

		/*	result = prime * result
				+ ((isDistinct == null) ? 0 : isDistinct.hashCode());
		result = prime * result
				+ ((joinType == null) ? 0 : joinType.hashCode());*/
		result = prime * result + ((left == null) ? 0 : left.hashCode());

		//		System.out.println("left hashcode: " + ((left == null) ? 0 : left.hashCode()));

		/*	 	result = prime * result + ((lhsRhs == null) ? 0 : lhsRhs.hashCode());*/
		result = prime * result
				+ ((operator == null) ? 0 : operator.hashCode());

		//		System.out.println("operator hashcode: " + ((operator == null) ? 0 : operator.hashCode()));
		/*	result = prime * result + queryIndex;
		result = prime * result + queryType;*/
		result = prime * result + ((right == null) ? 0 : right.hashCode());

		//		System.out.println("right hashcode: " + ((right == null) ? 0 : right.hashCode()));
		/*result = prime * result
				+ ((strConst == null) ? 0 : strConst.hashCode());
		result = prime * result
				+ ((subQueryConds == null) ? 0 : subQueryConds.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		result = prime * result
				+ ((tableAlias == null) ? 0 : tableAlias.hashCode());*/
		result = prime * result
				+ ((tableNameNo == null) ? 0 : tableNameNo.hashCode());

		//		 System.out.println("tablename mo hashcode: " + ((tableNameNo == null) ? 0 : tableNameNo.hashCode()));

		//	System.out.println("Value for node: " +result); 
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		// return new Integer(result).hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		//		System.out.println("Calling equals for: "+this+" "+obj);
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (agg == null) {
			if (other.agg != null)
				return false;
		} else if (!agg.equals(other.agg))
			return false;
		if (column == null) {
			if (other.column != null)
				return false;
		} else if (!column.equals(other.column))
			return false;
		/*if (isDistinct == null) {
			if (other.isDistinct != null)
				return false;
		} else if (!isDistinct.equals(other.isDistinct))
			return false;
		if (joinType == null) {
			if (other.joinType != null)
				return false;
		} else if (!joinType.equals(other.joinType))
			return false;*/
		if (left == null) {
			if (other.left != null)
				return false;
		} else if (!left.equals(other.left))
			return false;
		/*if (lhsRhs == null) {
			if (other.lhsRhs != null)
				return false;
		} else if (!lhsRhs.equals(other.lhsRhs))
			return false;*/
		if (operator == null) {
			if (other.operator != null)
				return false;
		} else if (!operator.equals(other.operator))
			return false;
		/*if (queryIndex != other.queryIndex)
			return false;
		if (queryType != other.queryType)
			return false;*/
		if (right == null) {
			if (other.right != null)
				return false;
		} else if (!right.equals(other.right))
			return false;
		/*if (strConst == null) {
			if (other.strConst != null)
				return false;
		} else if (!strConst.equals(other.strConst))
			return false;
		if (subQueryConds == null) {
			if (other.subQueryConds != null)
				return false;
		} else if (!subQueryConds.equals(other.subQueryConds))
			return false;*/
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		/*if (tableAlias == null) {
			if (other.tableAlias != null)
				return false;
		} else if (!tableAlias.equals(other.tableAlias))
			return false;*/
		if (tableNameNo == null) {
			if (other.tableNameNo != null)
				return false;
		} else if (!tableNameNo.equals(other.tableNameNo))
			return false;
		/*if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;*/
		return true;
	}
	/**
	 * @return the notInNodeType
	 */
	public static String getNotInNodeType() {
		return notInNodeType;
	}
	/**
	 * @param notInNodeType the notInNodeType to set
	 */
	public static void setNotInNodeType(String notInNodeType) {
		Node.notInNodeType = notInNodeType;
	}
	/**
	 * @return the extractFuncType
	 */
	public static String getExtractFuncType() {
		return extractFuncType;
	}
	/**
	 * @param extractFuncType the extractFuncType to set
	 */
	public static void setExtractFuncType(String extractFuncType) {
		Node.extractFuncType = extractFuncType;
	}
	/**
	 * @return the caseNodeType
	 */
	public static String getCaseNodeType() {
		return caseNodeType;
	}
	/**
	 * @param caseNodeType the caseNodeType to set
	 */
	public static void setCaseNodeType(String caseNodeType) {
		Node.caseNodeType = caseNodeType;
	}

}
