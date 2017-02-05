package parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import parsing.AggregateFunction;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Disjunct;
import parsing.ForeignKey;
import parsing.JoinClauseInfo;
import parsing.JoinTreeNode;
import parsing.Node;
import parsing.QueryParser;
import parsing.FromClauseElement;
import parsing.Pair;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

// Query Data represents the information required for scoring corresponding to the query block
public class QueryData {
	
	private static Logger logger = Logger.getLogger(QueryData.class.getName());
	
	// Selection conditions includes string selection conditions, join conditions
	private ArrayList<Node> selectionConds;
	
	// Added by Mathew on 22 April 2016
	// Join conditions are binary relational atomic conditions
	// in which both operands are column references and it does not include 
	// selection conditions 
	private ArrayList<Node> joinConditions;

	
	// Equivalence classes
	private Vector<Vector<Node>> eqClasses;
	
	// Projected attributes
	private ArrayList<Node> projectionList;
	
	// Relations involved
	private ArrayList<String> relations;
	
	// Relation numbers involved, added by mathew on sep 7 2016 
	private ArrayList<String> relationInstances;
	
	// The tables involved in joins
	private ArrayList<String> joinTables;
	
	// The subquery Connectives in the Query
	private ArrayList<String> subQConnectives;
	
	// The Set Operators used
	private ArrayList<String> setOpetators;
	
	//The aggregate functions used in select condition
	private ArrayList<AggregateFunction> aggregateList;
	
	// The number of outer joins (left + right) present
	private int numberOfOuterJoins;
	
	// The number of inner joins
	private int numberOfInnerJoins;
	
	// Contains info about the table map, join tree etc
	public GenerateCVC1 WholeData;
	
	//Having clause present
	private ArrayList<Node> havingClause;
	
	// Query Parser instance
	public QueryParser Parser;
	
	// Query block details
	public QueryBlockDetails QueryBlock;
	
	// Redundant relations
	public ArrayList<String> RedundantRelations;
	
	// From clause sub queries
	public Vector<QueryData> FromClauseQueries;
	
	// Where clause sub queries
	public Vector<QueryData> WhereClauseQueries;
	
	public ArrayList<Node> GroupByNodes;
	
	public ArrayList<Node> orderByNodes;
	
	public boolean hasDistinct=false;
	
	// Gets the selection conditions
	public ArrayList<Node> getSelectionConditions(){
		return this.selectionConds;
	}
	
	// Sets the selection conditions
	public void setSelectionConditions(ArrayList<Node> data){
		this.selectionConds = data;
	}
	
	// Gets the selection conditions
	public ArrayList<Node> getJoinConditions(){
		return this.joinConditions;
	}
	
	// Sets the selection conditions
	public void setJoinConditions(ArrayList<Node> data){
		this.joinConditions = data;
	}
	
	// Gets the equivalence classes
	public Vector<Vector<Node>> getEqClasses(){
		return this.eqClasses;
	}
	
	// Sets the equivalence classes
	public void setEqClasses(Vector<Vector<Node>> data){
		this.eqClasses = data;
	}
	
	// Gets the projection list
	public ArrayList<Node> getProjectionList(){
		return this.projectionList;
	}
	
	// Sets the projection list
	public void setProjectionList(ArrayList<Node> data){
		this.projectionList = data;
	}
	
	// Gets the set of Grouping attributes in a list
	public ArrayList<Node> getGroupByNodes(){
		return this.GroupByNodes;
	}
	
	// Sets the Grouping attributes
	public ArrayList<Node> setGroupByNodes(ArrayList<Node> data){
		return this.GroupByNodes=data;
	}

	
	// Gets the Order BY list
	public ArrayList<Node> getOrderByNodes(){
		return this.orderByNodes;
	}
	
	// Sets the Order By list
	public void setOrderByNodes(ArrayList<Node> data){
		this.orderByNodes = data;
	}
	
	public ArrayList<String> getRelationInstances(){
		return this.relationInstances;
	}	
	
	// Gets the relations
	public ArrayList<String> getRelations(){
		return this.relations;
	}
		
	// Sets the relations
	public void setRelations(ArrayList<String> data){
		this.relations = data;
	}
	
	// Gets the join tables
	public ArrayList<String> getJoinTables(){
		return this.joinTables;
	}
	
	// Sets the join tables
	public void setJoinTables(ArrayList<String> tables){
		this.joinTables = tables;
	}
	
	// Gets the number of outer joins
	public int getNumberOfOuterJoins(){
		return this.numberOfOuterJoins;
	}
	
	// Sets the number of outer joins
	public void setNumberOfOuterJoins(int joins){
		this.numberOfOuterJoins = joins;
	}
	
	// Gets the number of inner joins
	public int getNumberOfInnerJoins(){
		return this.numberOfInnerJoins;
	}
	
	// Sets the number of inner joins
	public void setNumberOfInnerJoins(int joins){
		this.numberOfInnerJoins = joins;
	}
	
	// Creates a query data instance
	public QueryData(){}
	
	// Creates a query data instance
	public QueryData(QueryParser qp, QueryBlockDetails qb, GenerateCVC1 data) throws CloneNotSupportedException{
		this.Parser = qp;
		this.QueryBlock = qb;
		this.WholeData = data;
		this.joinTables = new ArrayList<String>();
		this.selectionConds = new ArrayList<Node>();
		this.joinConditions=new ArrayList<Node>();
		this.eqClasses = new Vector<Vector<Node>>();
		this.projectionList = new ArrayList<Node>();
		this.relations = new ArrayList<String>();
		this.relationInstances = new ArrayList<String>();
		this.RedundantRelations=new ArrayList<String>();
		this.FromClauseQueries = new Vector<QueryData>();
		this.WhereClauseQueries = new Vector<QueryData>();
		this.GroupByNodes = new ArrayList<Node>();
		this.orderByNodes= new ArrayList<Node>();
		this.havingClause = new ArrayList<Node>();
		this.subQConnectives = new ArrayList<String>();
		this.aggregateList= new ArrayList<AggregateFunction>();
		this.setOpetators = new ArrayList<String>();
		this.hasDistinct = false;
		
		this.Initialize();
	}
	
	// Initializes the query data with the information corresponding to the query block and parser
	public void Initialize() throws CloneNotSupportedException{
		
		for(int i = 0; i < this.Parser.getFromClauseSubqueries().size(); i++){			
			this.FromClauseQueries.add(new QueryData(this.Parser.getFromClauseSubqueries().get(i), this.QueryBlock.getFromClauseSubQueries().get(i), this.WholeData));
		}

		/** Initialize the where clause nested sub query blocks */
		for(int i = 0; i < this.Parser.getWhereClauseSubqueries().size(); i++){			
			this.WhereClauseQueries.add(new QueryData(this.Parser.getWhereClauseSubqueries().get(i), this.QueryBlock.getWhereClauseSubQueries().get(i), this.WholeData));
		}
		
		this.hasDistinct = this.Parser.getIsDistinct();
		
		//changed by mathew on 8 May 2016, previously group by nodes where added from QueryBlock data member
		for(Node n : this.QueryBlock.getGroupByNodes()){
			this.GroupByNodes.add(n);
		}
		
		//add by mathew on 19 June 2016, 
		for(Node n : Parser.getOrderByNodes()){
			this.orderByNodes.add(n);
		}

		
		for(Node n: this.Parser.getProjectedCols()){
			if(n != null)
				this.projectionList.add(n);
			if(n != null && n.isDistinct()){
				this.hasDistinct = true;
			}
			
			
		}
		
		if(this.Parser.getHavingClause()!= null){
			this.setHavingClause(this.Parser.getHavingClause());
		}
		
		if(this.Parser.setOperator != null){
			this.setOpetators.add(this.Parser.setOperator);
		}
		
		/*
		 *  Added by mathew on 22 April 2016
		 *  initializing join conditions from the respective member in QueryParser
		 */
		for(Node n: this.Parser.getAllConds())
			this.joinConditions.add(n);
		
		ArrayList<ConjunctQueryStructure> conjuncts = this.QueryBlock.getConjunctsQs();

		for(ConjunctQueryStructure con: conjuncts){
			
			getSelectionConditionsAndEqClass(con, this.selectionConds, this.eqClasses);
		}
		//Get subquery connectives for All/ANY/EXISTS/NOT EXISTS (IN/NOT IN are converted to EXISTS/NOT EXISTS)
		Vector<ConjunctQueryStructure> conjunctList = null;//this.Parser.getConjuncts();
		for(ConjunctQueryStructure c : conjunctList){
			Vector <Node> subQConds = c.getAllSubQueryConds();
			for(Node n : subQConds){
				/* the expression: n.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) 
				 * from the If condition below removed, and 
				 * All node, Any node type in the following condition added by mathew on 27 June 2016
				 */

				if(n.getNodeType().equals(Node.getAllNodeType())
						||n.getNodeType().equals(Node.getAnyNodeType())
						||n.getNodeType().equals(Node.getExistsNodeType())
						||n.getNodeType().equals(Node.getNotExistsNodeType())
						||n.getNodeType().equals(Node.getInNodeType())
						||n.getNodeType().equals(Node.getNotInNodeType())
						){
					if(this.Parser.isInSubQ && n.getNodeType().equals(Node.getExistsNodeType())){
						this.subQConnectives.add(Node.getInNodeType());
					}else if(this.Parser.isInSubQ && n.getNodeType().equals(Node.getNotExistsNodeType())){
							this.subQConnectives.add(Node.getNotInNodeType());	
					}else{
						this.subQConnectives.add(n.getNodeType());	
					}
				}
			}
		}
		
		Vector <AggregateFunction> aggNodeList = this.Parser.getAggFunc();
		for(AggregateFunction af : aggNodeList){
			this.aggregateList.add(af);
		}
		
		Vector<String> tables = new Vector<String>();
		getFromTables(this.Parser.newQueryAliases, tables);
		ArrayList<String> tempFromTables=new ArrayList<String>();
		tempFromTables.addAll(tables);
		this.setRelations(tempFromTables);
		
		initializeRelationInstances();

		
		traverseJoinTree(this.Parser.getRoot());
		flatten(this.Parser.getRoot());
	}
	
	private void initializeRelationInstances() {
		// TODO Auto-generated method stub
		for(String table: this.Parser.getTableNames().keySet()){
			relationInstances.add(table);
		}
	}

	void getFromTables(FromClauseElement fle, Vector<String> tables){
		if(fle == null || fle.getBag() == null)
			return;
		
		for(FromClauseElement f : fle.getBag()){
			if(f.getTableName() != null && !f.getTableName().isEmpty()){
				tables.add(f.getTableName());
			}
			
			getFromTables(f, tables);
		}		
	}
	
	void getFromTables(FromListElement fle, Vector<String> tables){
		if(fle == null ||  fle.getTabs() == null)
			return;
		
		for(FromListElement f : fle.getTabs()){
			if(f.getTableName() != null && !f.getTableName().isEmpty()){
				tables.add(f.getTableName());
			}
			
			getFromTables(f, tables);
		}		
	}
	
	/*@ author mathew on May 7 2016
	 *  The following method, to be called after redundant (eliminatable)
	 *   relations are derived, to update the relations, 
	 *   projection/selection/having/grouping conditions, so that
	 *   any column from a redundant relation occuring in the above 
	 *   conditions is replaced an equivalent column 
	 *    
	 */
	public void reviseAfterFindingRedundantRelations(){
		reviseRelations();
		reviseJoinTables();
		reviseProjectedColumns();
		reviseGroupByColumns();
		reviseSelectionConditions();
		reviseHavingConditions();
		reviseOrderByColumns();
		updateEquivalenceClasses();

	}
	
	public void updateEquivalenceClasses() {
		// TODO Auto-generated method stub
		ArrayList<String> eliminatedRelations=this.RedundantRelations;
		boolean updateFlag=false;
		do{
			updateFlag=false;
			for(Vector<Node> eqClass:this.getEqClasses()){
				for(String elimRelation:eliminatedRelations){
					for(int i=0;i<eqClass.size();i++){
						Node n=eqClass.get(i);
						if(n.getTableNameNo().equalsIgnoreCase(elimRelation)){
							logger.info(" eq member deleted"+n+" from "+eqClass);
							eqClass.remove(i);
							updateFlag=true;
						}
					}
				}
			}
		}while(updateFlag);
	}

	
	/*@ only the method name changed by mathew on May 5 2016
	 *  old name setRelations changed to reviseRelations
	 */
	public void reviseRelations(){
		Boolean found = false;
		
		Vector<String> tables = new Vector<String>();
		getFromTables(this.Parser.newQueryAliases, tables);
		
		//added by mathew on May 9, 2016
		this.relations.clear();
		
		for(String n : tables){
			found = false;
			for(String t : this.RedundantRelations){
				/*assumes that tableNameNos are of the of the form
				 *  <tableNamei>, where 0 \leq i \leq 9 and 
				 *  tableName is the name of a table in schema
				 */
				if(t.substring(0, t.length()-1).equals(n)){
					found = true;
				}
			}
			
			if(!found)
				this.relations.add(n);
		}
		
	   relationInstances.removeAll(RedundantRelations);
	}
	
		
	
	/*@ author mathew on May 7 2016
	 *  The following method, to be called after redundant (eliminatable)
	 *    relations are derived, to update join tables
	 */
	public void reviseJoinTables(){
		ArrayList<String> temp=new ArrayList<String>();
		for(String table:this.getJoinTables()){
			if(!RedundantRelations.contains(table)){
				temp.add(table);
			}
			else{
				this.numberOfInnerJoins--;
			}
		}
		this.joinTables=temp;
	}

	/*@ author mathew on May 7 2016
	 *  The following method, to be called after redundant (eliminatable)
	 *   relations are derived, to update grouping conditions, so that
	 *   any column from a redundant relation occuring in the above 
	 *   conditions is replaced an equivalent column
	 */
	public void reviseGroupByColumns(){
		if(this.RedundantRelations==null || this.RedundantRelations.isEmpty())
			return;
		
		ArrayList<Node> tempGroupByNodes=new ArrayList<Node>();
		tempGroupByNodes.addAll(GroupByNodes);
		for(Node n:this.GroupByNodes){
			if(RedundantRelations.contains(n.getTableNameNo())){
				Node eqNode=getAlternateEquivalentColumnNode(n);
				if(eqNode!=null){
					tempGroupByNodes.remove(n);
					tempGroupByNodes.add(eqNode);
				}
			}
		}
		
		GroupByNodes.clear();
		GroupByNodes.addAll(tempGroupByNodes);

		Boolean found = false;
	
		/*
		 * Remove duplicate nodes since revising group by nodes 
		 * can introduce two different nodes that represent the same 
		 * columns 
		 */
		do{
			found=false;
			for(Node n:GroupByNodes){
				for(Node m:GroupByNodes){
					if(n!=m){
						if(n.getTableNameNo().equals(m.getTableNameNo())&&n.getColumn().getColumnName()
								.equals(m.getColumn().getColumnName())){
							GroupByNodes.remove(m);
							found=true;
							break;
						}
					}
				}
				if(found){
					break;
				}
			}
		}while(found);

	}
	
	/*@ author mathew on May 7 2016
	 *  The following method, to be called after redundant (eliminatable)
	 *   relations are derived, to update projection conditions, so that
	 *   any column from a redundant relation occuring in the above 
	 *   conditions is replaced an equivalent column
	 */
	public void reviseProjectedColumns(){
		if(this.RedundantRelations==null || this.RedundantRelations.isEmpty())
			return;
		
		ArrayList<Node> tempProjectionList=new ArrayList<Node>();
		tempProjectionList.addAll(projectionList);
		for(Node n:this.projectionList){
			if(RedundantRelations.contains(n.getTableNameNo())){
				Node eqNode=getAlternateEquivalentColumnNode(n);
				if(eqNode!=null){
					tempProjectionList.remove(n);
					tempProjectionList.add(eqNode);
				}
			}
		}
		
		projectionList.clear();
		projectionList.addAll(tempProjectionList);
		
	}
	
	/*@ author mathew on July 18 2016
	 *  The following method, to be called after redundant (eliminatable)
	 *   relations are derived, to update order by columns, so that
	 *   any column from a redundant relation occurring in the above 
	 *   conditions is replaced an equivalent column
	 */
	public void reviseOrderByColumns(){
		if(this.RedundantRelations==null || this.RedundantRelations.isEmpty())
			return;
		
		ArrayList<Node> tempOrderByNodes=new ArrayList<Node>();
		tempOrderByNodes.addAll(orderByNodes);
		for(Node n:this.orderByNodes){
			if(RedundantRelations.contains(n.getTableNameNo())){
				Node eqNode=getAlternateEquivalentColumnNode(n);
				if(eqNode!=null){
					tempOrderByNodes.remove(n);
					tempOrderByNodes.add(eqNode);
				}
			}
		}
		
		orderByNodes.clear();
		orderByNodes.addAll(tempOrderByNodes);
		
	}
	
	/*@ author mathew on May 7 2016
	 *  The following method, to be called after redundant (eliminatable)
	 *   relations are derived, to update selection conditions, so that
	 *   any column from a redundant relation occuring in the above 
	 *   conditions is replaced an equivalent column
	 */
	public void reviseSelectionConditions(){
		if(this.RedundantRelations==null || this.RedundantRelations.isEmpty())
			return;
		reviseBinaryConditions(this.selectionConds);	
		reviseBinaryConditions(this.joinConditions);
	}
	
	/*@ author mathew on May 7 2016
	 *  The following method, to be called after redundant (eliminatable)
	 *   relations are derived, to update having conditions, so that
	 *   any column from a redundant relation occuring in the above 
	 *   conditions is replaced an equivalent column
	 */
	public void reviseHavingConditions(){
		if(this.RedundantRelations==null || this.RedundantRelations.isEmpty())
			return;
		ArrayList<Node> havingConds=new ArrayList<Node>();
		for(Node n:havingClause)
			havingConds.add(n);
		
		reviseBinaryConditions(havingConds);		
		
		havingClause=new ArrayList<Node>();
		for(Node n:havingConds)
			havingClause.add(n);
	}
	
	/** @author mathew on 14 sep 2016 
	 * Is intended to be called just before computing partial marks,
	 *  Iterates through all selection conditions, if any of its member is a join condition,
	 *  then removes it from selection condition and adds it to join condition
	 */
	public void reAdjustJoins() {
		// TODO Auto-generated method stub
		ArrayList<Node> tempSelectionConds=new ArrayList<Node>();
		tempSelectionConds.addAll(selectionConds);
		for(Node n:selectionConds){
			if(n.getLeft().getNodeType().equals(Node.getColRefType())&&n.getRight().getNodeType().equals(Node.getColRefType())){
				if(!joinConditions.contains(n))
					joinConditions.add(n);
				tempSelectionConds.remove(n);
			}			
		}
		selectionConds.clear();
		selectionConds.addAll(tempSelectionConds);

	}
	
	/**
	 * @author mathew on Sep 11 2016
	 * returns true iff the first arugment string is a member of the second argument, which is a list
	 * 
	 */
	public static boolean isMemberOf(String element, List<String> list){
		if(element==null)
			return false;
		for(String s:list){
			if(element.equalsIgnoreCase(s))
				return true;
		}
		return false;
	}
	
	/*@ author mathew on May 5 2016
	 *  The following method accepts a list of nodes that are supposedly
	 *   binary atomic conditions in where/join clauses.. 
	 *   It proceses the left operand and right operand of each node
	 *   in the list, and whenever the operands are columns from a redundant
	 *   relation, it replaces with an equivalent column
	 */
	public void reviseBinaryConditions(ArrayList<Node> binaryConds){
		Boolean found = false;
		do{
			found=false;
			for(Node n:binaryConds){				
				Node lNode=n.getLeft();
				Node rNode=n.getRight();
				
				if(rNode.getColumn()!=null && lNode.getColumn()!=null &&
						rNode.getTableNameNo()!=null && lNode.getTableNameNo()!=null && n.getOperator().equals("=") &&
						lNode.getTableNameNo().equals(rNode.getTableNameNo()) &&
						lNode.getColumn().getColumnName().equals(rNode.getColumn().getColumnName())){
					binaryConds.remove(n);
					found=true;
					break;
				}

				
				
				if(n.getJoinType()!=null &&(n.getJoinType().equals(JoinClauseInfo.leftOuterJoin)||n.getJoinType().equals(JoinClauseInfo.rightOuterJoin)))
					continue;
				if(isMemberOf(n.getLeft().getTableNameNo(), RedundantRelations)
					||isMemberOf(n.getRight().getTableNameNo(),RedundantRelations)){
					Node eqNode=getAlternateEquivalentBinaryNode(n);
					if(eqNode==null){
						binaryConds.remove(n);
						found=true;
						break;
					}
					
					Node leftNode=eqNode.getLeft();
					Node rightNode=eqNode.getRight();
					/*
					 * If both left hand and right has side are the same columns from the same table
					 * then this skip the creation and insertion of new node
					 */
					if(rightNode.getColumn()!=null && leftNode.getColumn()!=null &&
							rightNode.getTableNameNo()!=null && leftNode.getTableNameNo()!=null && eqNode.getOperator().equals("=") &&
							leftNode.getTableNameNo().equals(rightNode.getTableNameNo()) &&
							leftNode.getColumn().getColumnName().equals(rightNode.getColumn().getColumnName())){
						binaryConds.remove(n);
						found=true;
						break;
					}
					else if(!leftNode.equals(n.getLeft())||!rightNode.equals(n.getRight())){
						binaryConds.remove(n);
						binaryConds.add(eqNode);
						found=true;
						break;
					}
				}
			}
		} while(found);
	}

	/*@ author mathew on May 5 2016
	 *  The following method given a node that represents an 
	 *  atomic binary condition from having/where/join clauses,  
	 *  processes the left operand and right operand of the 
	 *    node and whenever the operands are columns from a redundant
	 *   relation, it replaces with a node that represents an equivalent column
	 */

	public Node getAlternateEquivalentBinaryNode(Node n){

		Node leftNode = n.getLeft();
		Node leftNodeNew=n.getLeft();
		
		if(leftNode!=null&&!leftNode.getNodeType().equals(Node.getValType())){
			if(isMemberOf(leftNode.getTableNameNo(),RedundantRelations)){
				leftNodeNew=getAlternateEquivalentColumnNode(leftNode);
				if(leftNodeNew==null)
					return null;

			}
		}
		n.setLeft(leftNodeNew);
		
		Node rightNode=n.getRight();
		Node rightNodeNew=n.getRight();
		if(rightNode!=null&& !rightNode.getNodeType().equals(Node.getValType())){
			if(isMemberOf(rightNode.getTableNameNo(),RedundantRelations)){
				rightNodeNew=getAlternateEquivalentColumnNode(rightNode);
			}
			if(rightNodeNew==null)
				return null;
		}
		n.setRight(rightNodeNew);
		
		if(rightNodeNew.getColumn()!=null && leftNodeNew.getColumn()!=null &&
				rightNodeNew.getTableNameNo()!=null && leftNodeNew.getTableNameNo()!=null && n.getOperator().equals("=") &&
				leftNodeNew.getTableNameNo().equals(rightNodeNew.getTableNameNo()) &&
				leftNodeNew.getColumn().getColumnName().equals(rightNodeNew.getColumn().getColumnName())){
			return null;
		}

		return n;

	}
	
	/*@ author mathew on May 7 2016
	 *  The following method given a node that is supposedly column type 
	 *   and whenever the node is a column from a redundant
	 *   relation, it returns node that represents
	 *    an equivalent column
	 */

	public Node getAlternateEquivalentColumnNode(Node n){
		Map<Node,ArrayList<Node>> nodeToEqNodes=getNodeToEquivalentNodes();
		
		// if the given node is an aggregate node
		if(n.getType().equals(Node.getAggrNodeType())){				
			if(n.getAgg().getAggExp() != null && n.getAgg().getAggExp().getTable() != null){				

				String tableNameNo=n.getAgg().getAggExp().getTableNameNo();
				String colName=n.getAgg().getAggExp().getColumn().getColumnName();

				if(tableNameNo==null || colName==null)
					return null;
				
				/*
				 * iterates through the map of nodeToEquivalent nodes and 
				 * finds a column node n that is equivalent to the input node
				 * s.t. n is not from a redundant relation
				 */
				for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
					if(e.getKey().getTableNameNo().equals(tableNameNo)&&e.getKey().getColumn().getColumnName().equals(colName)){
						
						/*
						 * find an equivalent column c from a table other than the deletedTable
						 *  and replaced the nodes  table and column with c's table and column
						 */
						for(Node m:e.getValue()){
							String relationNameNew=m.getTable().getTableName();
							String relationNameNoNew=m.getTableNameNo();
							if(!RedundantRelations.contains(relationNameNoNew)){
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
								return nodeNew;

							}
						}

					}

				}

			}
		}
		/* Otherwise it is a standard (non-aggregate) column node
		 * 
		 */
		else{
			String tableNameNo=n.getTableNameNo();
			String colName=n.getColumn().getColumnName();

			if(tableNameNo==null || colName==null)
				return null;

			/*
			 * iterates through the map of nodeToEquivalent nodes and 
			 * finds a column node n that is equivalent to the input node
			 * s.t. n is not from a redundant relation
			 */
			for(Entry<Node, ArrayList<Node>> e: nodeToEqNodes.entrySet()){
				if(e.getKey().getTableNameNo().equals(tableNameNo) && e.getKey().getColumn().getColumnName().equals(colName)){
					/*
					 * find an equivalent column c from a table other than the deletedTable
					 *  and replaced the nodes  table and column with c's table and column
					 */
					for(Node m:e.getValue()){
						String relationNameNew=m.getTable().getTableName();
						String relationNameNoNew=m.getTableNameNo();
						if(!RedundantRelations.contains(relationNameNoNew)){
							String colNameNew=m.getColumn().getColumnName();
							Node nodeNew=new Node(n);
							
							Column tempCol=new Column(colNameNew,relationNameNew);
							parsing.Table tempTable=new parsing.Table(relationNameNew);
							tempTable.addColumn(tempCol);
							
							nodeNew.setTable(tempTable);
							nodeNew.setColumn(tempCol);
							nodeNew.setTableNameNo(m.getTableNameNo());
							nodeNew.setTableAlias(m.getTableAlias());
							return nodeNew;

						}
					}

					
				}

			}

		}

		return null;
	}
	
	public Map<String, HashMap<String, ArrayList<Pair>>> getRelationToRelationEquivalentNodes(){
		Map<String, ArrayList<Node>> relationToEqNodes = new HashMap<String, ArrayList<Node>>();
		Map<Node, ArrayList<Node> > nodeToEqNodes = new HashMap<Node, ArrayList<Node>>();
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
					nodeToEqNodes.put(n1, temp);
					
					ArrayList<Node> temp2 = null;
					if(relationToEqNodes.containsKey(n1.getTableNameNo())){
						temp2 = relationToEqNodes.get(n1.getTableNameNo());
					}
					else{
						temp2 = new ArrayList<Node>();
					}

					temp2.add(n1);

					relationToEqNodes.put(n1.getTableNameNo(), temp2);


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

		return relationToRelationEqNodes;
	}
	
	/* Returns the set of relations R s.t. key attributes
	 * in R are referred to by foreign keys of another table R'  and the corresponding attributes 
	 * of referencing table R' and referenced table R are part of the equivalence
	 * relation induced by the query. Also it is made sure that R does not contain foreignkey 
	 * references to relations in baseTables (which are the set of non eliminated tables so far)
	 */

	public static ArrayList<String> getReferencedRelations(Set<String> baseTables, ArrayList<String> eliminateRelations, ArrayList<ForeignKey> foreignKeys, Map<String, HashMap<String, ArrayList<Pair>>> relationToRelationEqNodes){	

		Boolean isReferenced = false;
		ArrayList<String> referencedRelations = new ArrayList<String>();

		for(String cand: baseTables){

			HashMap<String, ArrayList<Pair>> data = relationToRelationEqNodes.get(cand);
			isReferenced = false;

			if(data == null)
				continue;

			for(Entry<String, ArrayList<Pair>> entry: data.entrySet()){
				String key = entry.getKey();

				/*
				 * Make sure that the table under consideration is not an eliminated relation  
				 */
				if(eliminateRelations!=null && eliminateRelations.contains(key))
					continue;

				ArrayList<Pair> values = entry.getValue();
				

				/* assumes that tableNameNos are of the form tableNamei, where i is between 0 to 9,
				 */
				String candTableName=cand.substring(0, cand.length()-1);
				String keyTableName=key.substring(0, key.length()-1);
				/*
				 * Make sure that any referenced table does not refer to another table 
				 * via a foreign key relation
				 */
				ForeignKey fk= getForeignKey(candTableName, keyTableName, foreignKeys);

				if(fk != null){
					isReferenced = false;
					break;
				}

				fk = getForeignKey(keyTableName, candTableName, foreignKeys);

				if(fk == null){
					isReferenced = false;
					continue;
				}

				Vector<Column> candKeys = fk.getReferenceKeyColumns();
				Vector<Column> otherKeys = fk.getFKeyColumns();

				isReferenced = true;
				for(int i = 0; i < candKeys.size(); i++){
					Column canCol = candKeys.get(i);
					Column othCol = otherKeys.get(i);
					Boolean found = false;
					for(Pair v : values){
						if(v.first.getColumn().getColumnName().equalsIgnoreCase(canCol.getColumnName()) && v.second.getColumn().getColumnName().equalsIgnoreCase(othCol.getColumnName())){
							found = true;
							break;
						} 
					}

					if(!found){
						isReferenced = false;
						break;
					}
				}

				if(!isReferenced)
					break;
			}

			if(isReferenced){
				referencedRelations.add(cand);
			}
		}
		return referencedRelations;
	}

	
	/*@ author mathew on May 7 2016
	 *  The following method creates/returns a map of key-value pairs, 
	 *  where keys are nodes that represent columns in the given query 
	 *  and the corresponding values are lists of nodes that represents
	 *  the columns that are equivalent with the key  
	 */

	public Map<Node, ArrayList<Node>> getNodeToEquivalentNodes(){
		Map<Node, ArrayList<Node> > nodeToEqNodes = new HashMap<Node, ArrayList<Node>>();
		if(eqClasses != null){
			/*
			 * iterate through each class in the set of classes in eqClasses
			 */			
			for(Vector<Node> t : eqClasses){
				/*
				 * for every member m in the class t, add m 
				 * as a key and every member m' != m to its 
				 * corresponding list of values in the nodeToEquivalentNode
				 * map
				 */				
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

					nodeToEqNodes.put(n1, temp);
				}
			}
		}
		return nodeToEqNodes;
	}
	
	
	public int getRelationCount(){
		
		return this.relations.size();
		
		/*Boolean found = false;
		int count = 0;
		for(String n : this.WholeData.getRepeatedRelationCount().keySet()){
			found = false;
			for(String t : this.RedundantRelations){
				if(t.equals(n)){
					found = true;
				}
			}
			
			if(!found)
				count++;
		}
		
		return count;*/
	}	
	
	// Gets all the selection conditions and eq classes corresponding to the query block
	private void getSelectionConditionsAndEqClass(Conjunct_ToDel con, ArrayList<Node> selectionConds, Vector<Vector<Node>> eqClasses){

		if(con.selectionConds != null ) 
			selectionConds.addAll(con.selectionConds);
		if(con.stringSelectionConds != null)					
			selectionConds.addAll(con.stringSelectionConds);
		if(con.joinConds != null){
			selectionConds.addAll(con.joinConds);
		}
		if(con.likeConds != null)
			selectionConds.addAll(con.likeConds);

		
		if(con.getEquivalenceClasses() != null && con.getEquivalenceClasses().size() > 0) {
			eqClasses.addAll(con.getEquivalenceClasses());
		}
		
		for(Disjunct dis : con.disjuncts){
			if(dis.selectionConds != null && dis.selectionConds.size() > 0) {
				selectionConds.addAll(dis.selectionConds);
			}
			
			if(dis.getEquivalenceClasses() != null && dis.getEquivalenceClasses().size() > 0) {
				eqClasses.addAll(dis.getEquivalenceClasses());
			}
			
			if(dis.conjuncts != null && dis.conjuncts.size() > 0){
				for(Conjunct_ToDel conjunct: dis.conjuncts){
					getSelectionConditionsAndEqClass(conjunct, selectionConds, eqClasses);
				}
			}
		}
	}
	
	// Gets all the selection conditions and eq classes corresponding to the query block
		private void getSelectionConditionsAndEqClass(ConjunctQueryStructure con, ArrayList<Node> selectionConds, Vector<Vector<Node>> eqClasses){

			if(con.selectionConds != null ) 
				selectionConds.addAll(con.selectionConds);
			if(con.stringSelectionConds != null)					
				selectionConds.addAll(con.stringSelectionConds);
			if(con.joinCondsAllOther != null){
				selectionConds.addAll(con.joinCondsAllOther);
			}
			if(con.likeConds != null)
				selectionConds.addAll(con.likeConds);

			
			if(con.getEquivalenceClasses() != null && con.getEquivalenceClasses().size() > 0) {
				eqClasses.addAll(con.getEquivalenceClasses());
			}
			
			for(DisjunctQueryStructure dis : con.disjuncts){
				if(dis.selectionConds != null && dis.selectionConds.size() > 0) {
					selectionConds.addAll(dis.selectionConds);
				}
				
				if(dis.getEquivalenceClasses() != null && dis.getEquivalenceClasses().size() > 0) {
					eqClasses.addAll(dis.getEquivalenceClasses());
				}
				
				if(dis.conjuncts != null && dis.conjuncts.size() > 0){
					for(ConjunctQueryStructure conjunct: dis.conjuncts){
						getSelectionConditionsAndEqClass(conjunct, selectionConds, eqClasses);
					}
				}
			}
		}
	
	
	// Traverses through the join tree to the get the information about the relations involved in the joins
	private void traverseJoinTree(JoinTreeNode root){
		if(root == null|| root.getNodeType()==null)
			return;
		
		if(root.getNodeType().equals("INNER JOIN")){
			this.numberOfInnerJoins++;
			traverseJoinTree(root.getLeft());
			traverseJoinTree(root.getRight());
		}
		
		if(root.getNodeType().equals("LEFT OUTER JOIN") || root.getNodeType().equals("RIGHT OUTER JOIN")){
			this.numberOfOuterJoins++;
			traverseJoinTree(root.getLeft());
			traverseJoinTree(root.getRight());
		}
		
		if(root.getNodeType().equals("RELATION")){
			this.joinTables.add(root.getTableNameNo());
		}
	}
	
	private void flatten(JoinTreeNode root){		
		invertRightJoins(root);
		
		Boolean change = true;
		
		while(change){
			change = false;
			change = flattenInnerJoin(root, change);			
		}
		
		nullifyFlattenNodes(root);
	}
	
	private void nullifyFlattenNodes(JoinTreeNode root){
		if(root == null)
			return;		
	}

	
	// Converts all the right joins to corresponding left joins
	private void invertRightJoins(JoinTreeNode root){
		if(root == null || root.getNodeType()== null  || root.getNodeType().equals("RELATION"))
			return;
		
		if(root.getNodeType().equals("RIGHT JOIN")){
			root.setNodeType("LEFT JOIN");
			JoinTreeNode temp = root.getRight();			
			root.setRight(root.getLeft());
			root.setLeft(temp);
		}
		
		invertRightJoins(root.getLeft());
		invertRightJoins(root.getRight());
	}
	
	private Boolean flattenInnerJoin(JoinTreeNode root, Boolean change){
		if(root == null || root.getNodeType().equals("RELATION")){
			return change;
		}
		
		JoinTreeNode left = root.getLeft();
		JoinTreeNode right = root.getRight();
		
		if(left==null||right==null||left.getNodeType()==null||right.getNodeType()==null)
			return change;

		
		if(!root.getIsFlattened() && root.getNodeType().equals("INNER JOIN") && (left.getNodeType().equals("RELATION") || left.getIsFlattened()) 
				&& (right.getNodeType().equals("RELATION") || right.getIsFlattened())){
			change = true;
			root.setIsFlattened(true);
			Vector<String> nodes = root.getFlattenedNodes();
			
			if(left.getIsFlattened()){
				nodes.addAll(left.getFlattenedNodes());
				
				/*use of tempNodes added by mathew on 14 may
				 * to avoid the generation concurrent modification exception  
				 */
				Vector<Node> tempNodes=new Vector<Node>();
				tempNodes.addAll(left.getJoinPred());
				for(Node n: tempNodes){
					root.addJoinPred(n);	
				}				
			}
			else{
				nodes.add(left.getRelName());
			}
			
			if(right.getIsFlattened()){
				nodes.addAll(right.getFlattenedNodes());
				
				for(Node n: right.getJoinPred()){
					root.addJoinPred(n);	
				}
			}
			else{
				nodes.add(right.getRelName());
			}
			
			root.setFlattenedNodes(nodes);
		}		
		
		return flattenInnerJoin(left, change) || flattenInnerJoin(right, change);
	}

	/**
	 * @return the havingList
	 */
	public ArrayList<Node> getHavingClause() {
		return havingClause;
	}

	/**
	 * @param havingList the havingList to set
	 */
	public void setHavingClause(ArrayList<Node> havingClause) {
		this.havingClause = havingClause;
	}

	public void setHavingClause(Node n){
		
		if(n != null && n.getNodeType()!=null&& !n.getNodeType().equals(Node.getAndNodeType()) && n.getNodeType().equals(Node.getBroNodeType())){
			this.havingClause.add(n);
		}else if(n != null && n.getNodeType()!=null && n.getNodeType().equals(Node.getAndNodeType())){
			this.setHavingClause(n.getLeft());
			this.setHavingClause(n.getRight());
		}
	}

	/**
	 * @return the subQConnectives
	 */
	public ArrayList<String> getSubQConnectives() {
		return subQConnectives;
	}

	/**
	 * @param subQConnectives the subQConnectives to set
	 */
	public void setSubQConnectives(ArrayList<String> subQConnectives) {
		this.subQConnectives = subQConnectives;
	}

	/**
	 * @return the aggregateList
	 */
	public ArrayList<AggregateFunction> getAggregateList() {
		return aggregateList;
	}

	/**
	 * @param aggregateList the aggregateList to set
	 */
	public void setAggregateList(ArrayList<AggregateFunction> aggregateList) {
		aggregateList = aggregateList;
	}

	/**
	 * @return the setOpetators
	 */
	public ArrayList<String> getSetOpetators() {
		return setOpetators;
	}

	/**
	 * @param setOpetators the setOpetators to set
	 */
	public void setSetOpetators(ArrayList<String> setOpetators) {
		this.setOpetators = setOpetators;
	}

	/* @author mathew on 14 Sep 2016
	 * Adds left and right node as members of the same equivalence class
	 * 
	 */
	public void addToEquivalenceClasses(Node left, Node right) {
		// TODO Auto-generated method stub	
		boolean modified=false;
		for(Vector<Node> eqClass:this.getEqClasses()){
			if(eqClass.contains(left)&&!eqClass.contains(right)){
				eqClass.addElement(right);
				modified=true;
			}
			if(eqClass.contains(right)&&!eqClass.contains(left)){
				eqClass.addElement(left);
				modified=true;
			}
		}
		/* if left and right are not present in any of the existing equivalence
		 * class then create a new equivalence class with elements left and right
		 */
		if(!modified){
			Vector<Node> eqClass=new Vector<Node>();
			eqClass.add(left);
			eqClass.add(right);
			eqClasses.add(eqClass);
		}
	}
	
	/*
	 * Returns the foreign key with referencing table relation1 and referenced table relation 2
	 * from the set of foreign keys foreignKeys
	 */	
	public static ForeignKey getForeignKey(String relation1, String relation2, ArrayList<ForeignKey> foreignKeys){
		for(ForeignKey fk : foreignKeys){
			if((fk.getFKTablename().equalsIgnoreCase(relation1) && fk.getReferenceTable().getTableName().equalsIgnoreCase(relation2))){
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
