/**
 * @author mathew - code is an adaptation of the previous code from
 * parsing.QueryParser.java, acknowledgements to its authors
 *
 */
package parsing;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.ExceptOp;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.IntersectOp;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.MinusOp;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperation;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.UnionOp;
import net.sf.jsqlparser.statement.select.WithItem;
import parsing.ANDNode;
import parsing.AggregateFunction;
import parsing.CaseCondition;
import parsing.Column;
import parsing.ForeignKey;
import parsing.JoinClauseInfo;
import parsing.JoinTreeNode;
import parsing.ORNode;
import parsing.Query;
import parsing.RelationHierarchyNode;
import parsing.TreeNode;
import parsing.Node;
import parsing.CaseExpression;
import parsing.ProcessSelectClause;
import parsing.Util;
import util.TableMap;


	public class QueryStructure implements Serializable, QueryStructureInterface{

		private static Logger logger = Logger.getLogger(QueryStructure.class.getName());
		private static final long serialVersionUID = 8049915037697741933L;
		public ORNode orNode;
		private Query query;
		private TableMap tableMap;

		private Vector<TreeNode> inOrderList;

		private Vector<JoinClauseInfo> foreignKeyVector;

		private Vector<ForeignKey> foreignKeyVectorModified;

		private Vector<JoinClauseInfo> foreignKeyVectorOriginal;
		
		private HashMap<Integer, Vector<JoinClauseInfo>> inConds;
		public Vector<Node> allConds;
		private Vector<Node> selectionConds;
		private Vector<Node> likeConds;
		public Vector<Node> isNullConds;
		private Vector<Node> inClauseConds;
		Vector<Vector<Node>> equivalenceClasses;
		Vector<Node> joinConds;
		private Vector<Node> foreignKeys;
		public Vector<Node> projectedCols;
		private Node queryType; // To store outer query information

		//added by mathew on 2 september 2016
		public QueryStructure parentQueryParser;
		public Vector<FromClauseElement> fromListElements;
		
		//Map of case conditions: Key value holds the part of query in which the case conditions occur.
		//Value holds the case condition vector. Last element of vector contains the else condition
		private HashMap<Integer,CaseExpression> caseConditionMap;
		
		//private Vector<CaseCondition> caseConditions;
		
		/**
		 * 1- projected cols
		 * 2- where clause
		 * 3- having
		 * 4- order-by
		 * These serve as key for retrieving CASE statements in different part of the query
		 * */
		static int[] caseIndex ={1,2,3,4};
		
		private HashMap<Integer,Vector<CaseCondition>> caseMap = new HashMap<Integer,Vector<CaseCondition>>();
		
		public String setOperator;
		public QueryStructure leftQuery;
		public QueryStructure rightQuery; 

		// aggregation
		Vector<AggregateFunction> aggFunc;
		Vector<Node> groupByNodes;
		Vector<Node> lsthavingClauses;
		Node havingClause;

		JoinTreeNode root;//currently not used
		public RelationHierarchyNode topLevelRelation;//currently not used
		
		//added by mathew on 18 June 2016 for processing order by clause
		//order by 
		Vector<Node> orderByNodes;
		
		//Representation in DNF
		Vector<Vector<Node>> dnfCond;
		// Subquery
		private Vector<Node> allCondsExceptSubQuery;
		Vector<Node> allSubQueryConds;
		private Vector<Node> allAnyConds;
		Vector<Node> lhsRhsConds;
		public int paramCount;
		public Vector<ConjunctQueryStructure> conjuncts;
		Vector<Vector<Node>> allDnfSelCond;
		Vector<Vector<Node>> dnfLikeConds;
		Vector<Vector<Node>> dnfIsNullConds;
		Vector<Vector<Node>> allDnfSubQuery;
		Vector<Vector<Node>> dnfJoinCond;
		
		private boolean isDistinct;

		//To store from clause subqueries
		private Vector<QueryStructure> FromClauseSubqueries;//To store from clause subqueries
		private Vector<QueryStructure> WhereClauseSubqueries;//To store where clause sub queries



		
		
		/** The following data members added by mathew on 11 Oct 2016
		 */				
		// Selection conditions includes string selection conditions, join conditions
		private ArrayList<Node> lstSelectionConds;
		
		// Join conditions are binary relational atomic conditions
		// in which both operands are column references and it does not include 
		// selection conditions 
		private ArrayList<Node> lstJoinConditions;

		
		//Having clause present
		private ArrayList<Node> lstHavingConditions;
		
		// Projected attributes
		private ArrayList<Node> lstProjectedCols;

		public ArrayList<Node> lstGroupByNodes;
		
		public ArrayList<Node> lstOrderByNodes;
		
		
		// The subquery Connectives in the Query
		private ArrayList<String> lstSubQConnectives;
		
		// The Set Operators used
		private ArrayList<String> lstSetOperators;
		
		//The aggregate functions used in select condition
		private ArrayList<AggregateFunction> lstAggregateList;

		
		// Relations involved
		private ArrayList<String> lstRelations;
		
		// Relation numbers involved, added by mathew on sep 7 2016 
		private ArrayList<String> lstRelationInstances;
		
		// The number of outer joins (left + right) present
		private int numberOfOuterJoins;
		
		// The number of inner joins
		private int numberOfInnerJoins;

		// Redundant relations
		public ArrayList<String> lstRedundantRelations;
		
		// Equivalence classes
		private ArrayList<ArrayList<Node>> lstEqClasses;
		
		private ArrayList<ForeignKey> lstForeignKeysModified;
		
		
		public void initializeQueryListStructures(){
			if(setOperator==null||setOperator.isEmpty()){
				this.lstSelectionConds=new ArrayList<Node>();
				this.lstJoinConditions=new ArrayList<Node>();
				this.lstHavingConditions=new ArrayList<Node>();
				this.lstProjectedCols=new ArrayList<Node>();
				this.lstGroupByNodes=new ArrayList<Node>();
				this.lstOrderByNodes=new ArrayList<Node>();
				this.lstSubQConnectives=new ArrayList<String>();
				this.lstSetOperators=new ArrayList<String>();
				this.lstAggregateList=new ArrayList<AggregateFunction>();
				this.lstRedundantRelations=new ArrayList<String>();
				this.lstEqClasses=new ArrayList<ArrayList<Node>>();
				this.lstForeignKeysModified = new ArrayList<ForeignKey>( getForeignKeyVectorModified());
				
				for(QueryStructure fromSubQuery:this.FromClauseSubqueries)
					fromSubQuery.initializeQueryListStructures();
				for(QueryStructure whereSubQuery:this.WhereClauseSubqueries)
					whereSubQuery.initializeQueryListStructures();			
				for(ConjunctQueryStructure con:this.conjuncts){
					getSelectionConditionsAndEqClasses(con);
				}

				this.lstJoinConditions.addAll(this.getJoinConds());
				//remove duplicates
				ArrayList<Node> tempLst=(ArrayList<Node>)lstJoinConditions.clone();
				lstJoinConditions.clear();
				lstJoinConditions.addAll(Util.toSetOfNodes(tempLst));

				/* initialize number of inner and outer joins
				 */
				for(Node n:lstJoinConditions){
					if(n.getJoinType()!=null&&(n.getJoinType().equals(JoinClauseInfo.leftOuterJoin)
							||n.getJoinType().equals(JoinClauseInfo.rightOuterJoin)
							||n.getJoinType().equals(JoinClauseInfo.fullOuterJoin))){
						this.numberOfOuterJoins++;
					}
					else
						this.numberOfInnerJoins++;
				}


				this.lstHavingConditions.addAll(this.getlstHavingClauses());

				this.lstProjectedCols.addAll(this.getProjectedCols());

				this.lstGroupByNodes.addAll(this.getGroupByNodes());

				this.lstOrderByNodes.addAll(this.getOrderByNodes());

				if(this.setOperator!=null&&!this.setOperator.isEmpty())
					this.lstSetOperators.add(this.setOperator);

				this.lstAggregateList.addAll(this.getAggFunc());

//				getFromTablesAndInstances(this.fromListElements);
				
				lstForeignKeysModified.addAll(this.foreignKeyVectorModified);
			}
			else{
				this.initializeQueryListStructuresForSetOperator();
				this.leftQuery.initializeQueryListStructures();
				this.rightQuery.initializeQueryListStructures();
			}
		}
		
		private void initializeQueryListStructuresForSetOperator() {
			// TODO Auto-generated method stub
			this.lstProjectedCols=new ArrayList<Node>();
			this.lstProjectedCols.addAll(this.getProjectedCols());
		}

		private void getFromTablesAndInstances(Vector<FromClauseElement> fromListElements) {
			for(FromClauseElement fle:fromListElements){
				if(fle!=null&&fle.getTableName()!=null&&!fle.getTableName().isEmpty())
					lstRelations.add(fle.getTableName());
				if(fle!=null&&fle.getTableNameNo()!=null&&!fle.getTableNameNo().isEmpty())
					lstRelationInstances.add(fle.getTableNameNo());
				if(fle.getBag()!=null&&!fle.getBag().isEmpty())
					getFromTablesAndInstances(fle.getBag());
			}
			
		}

		private void getSelectionConditionsAndEqClasses(ConjunctQueryStructure con){
				if(con.selectionConds != null ) 
					lstSelectionConds.addAll(con.selectionConds);
				if(con.stringSelectionConds != null)					
					lstSelectionConds.addAll(con.stringSelectionConds);
				if(con.joinCondsForEquivalenceClasses != null){
					//lstSelectionConds.addAll(con.joinCondsForEquivalenceClasses);
					this.lstJoinConditions.addAll(con.joinCondsForEquivalenceClasses);
				}
				if(con.joinCondsAllOther!=null){
					//lstSelectionConds.addAll(con.joinCondsAllOther);
					this.lstJoinConditions.addAll(con.joinCondsAllOther);
				}
				if(con.likeConds != null)
					lstSelectionConds.addAll(con.likeConds);
				
				
				

				if(con.getEquivalenceClasses() != null && con.getEquivalenceClasses().size() > 0) {
					for(Vector<Node> eqClass:con.getEquivalenceClasses()){
						ArrayList<Node> tempClass=new ArrayList<Node>();
						tempClass.addAll(eqClass);
						this.lstEqClasses.add(tempClass);
					}
				}

				for(DisjunctQueryStructure dis : con.disjuncts){
					if(dis.selectionConds != null && dis.selectionConds.size() > 0) {
						this.lstSelectionConds.addAll(dis.selectionConds);
					}

					if(dis.getEquivalenceClasses() != null && dis.getEquivalenceClasses().size() > 0) {
						for(Vector<Node> eqClass:dis.getEquivalenceClasses()){
							ArrayList<Node> tempClass=new ArrayList<Node>();
							tempClass.addAll(eqClass);
							this.lstEqClasses.add(tempClass);
						}
					}

					if(dis.conjuncts != null && dis.conjuncts.size() > 0){
						for(ConjunctQueryStructure conjunct: dis.conjuncts){
							getSelectionConditionsAndEqClasses(conjunct);
						}
					}
				}
				
				Vector <Node> subQConds = con.getAllSubQueryConds();
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
							||n.getNodeType().equals(Node.getNotInNodeType())){

						// commented by mathew on 18 oct 2016
//						if(this.isInSubQ && n.getNodeType().equals(Node.getExistsNodeType())){
//							this.lstSubQConnectives.add(Node.getInNodeType());
//						}else if(this.isInSubQ && n.getNodeType().equals(Node.getNotExistsNodeType())){
//								this.lstSubQConnectives.add(Node.getNotInNodeType());	
//						}else{
						if(n.getNodeType().equals(Node.getExistsNodeType()) && n.isInNode){
							this.lstSubQConnectives.add(Node.getInNodeType());
						}else if(n.getNodeType().equals(Node.getNotExistsNodeType()) && n.isInNode){
							this.lstSubQConnectives.add(Node.getNotInNodeType());
						}else {
							this.lstSubQConnectives.add(n.getNodeType());
						}
//						}
					}
				}

		}
				
		// Gets the list of selection conditions
		public ArrayList<Node> getLstSelectionConditions(){
			return this.lstSelectionConds;
		}
		
		// Sets the list of selection conditions
		public void setSelectionConditions(ArrayList<Node> data){
			lstSelectionConds = data;
		}
		
		// Gets the list of selection conditions
		public ArrayList<Node> getLstJoinConditions(){
			return this.lstJoinConditions;
		}
		
		// Sets the list of selection conditions
		public void setLstJoinConditions(ArrayList<Node> data){
			this.lstJoinConditions = data;
		}
		
		// Gets the list of having conditions
		public ArrayList<Node> getLstHavingConditions(){
			return this.lstHavingConditions;
		}
		
		// Sets the list of selection conditions
		public void setLstHavingConditions(ArrayList<Node> data){
			this.lstHavingConditions = data;
		}
		
		// Gets the list of projected columns
		public ArrayList<Node> getLstProjectedCols(){
			return this.lstProjectedCols;
		}

		// Sets the list of projected columns
		public void setLstProjectedCols(ArrayList<Node> data){
			this.lstProjectedCols = data;
		}

		// Gets the list of group by nodes
		public ArrayList<Node> getLstGroupByNodes(){
			return this.lstGroupByNodes;
		}

		// Sets the list of group by nodes
		public void setLstGroupByNodes(ArrayList<Node> data){
			this.lstGroupByNodes = data;
		}
		
		// Gets the list of order by nodes
		public ArrayList<Node> getLstOrderByNodes(){
			return this.lstOrderByNodes;
		}

		// Sets the list of order by nodes
		public void setLstOrderByNodes(ArrayList<Node> data){
			this.lstOrderByNodes = data;
		}
		
		// Gets the list of subquery connectives 
		public ArrayList<String> getLstSubQConnectives(){
			return this.lstSubQConnectives;
		}

		// Sets the list of subquery connectives 
		public void setLstLstSubQConnectives(ArrayList<String> connectives){
			this.lstSubQConnectives = connectives;
		}
		
		// Gets the list of set operators 
		public ArrayList<String> getLstSetOpetators(){
			return this.lstSetOperators;
		}

		// Sets the list of set operators 
		public void setLstSetOperators(ArrayList<String> setOperators){
			this.lstSetOperators = setOperators;
		}
		
		// Gets the list of aggregate functions 
		public ArrayList<AggregateFunction> getLstAggregateList(){
			return this.lstAggregateList;
		}

		// Sets the list of aggregate functions 
		public void setLstAggregateList(ArrayList<AggregateFunction> aggs){
			this.lstAggregateList = aggs;
		}

		
		public ArrayList<String> getLstRelationInstances(){
			return this.lstRelationInstances;
		}	

		// Sets the list of relations
		public void setLstRelationInstances(ArrayList<String> data){
			this.lstRelationInstances = data;
		}

		// Gets the list of relations
		public ArrayList<String> getLstRelations(){
			return this.lstRelations;
		}
		
		public void addFromTable(FromClauseElement fle){
			this.lstRelationInstances.add(fle.getTableNameNo());
			this.lstRelations.add(fle.getTableName());
		}
			
		// Sets the list of relations
		public void setLstRelations(ArrayList<String> data){
			this.lstRelations = data;
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
		
		public ArrayList<ArrayList<Node>> getLstEqClasses(){
			return lstEqClasses;
		}

		// Gets the list of relations
		public ArrayList<String> getLstRedundantRelations(){
			return this.lstRedundantRelations;
		}
			
		// Sets the list of relations
		public void setLstRedundantRelations(ArrayList<String> data){
			this.lstRedundantRelations = data;
		}
		
		// Gets the list of relations
		public ArrayList<ForeignKey> getLstForeignKeysModified(){
			return this.lstForeignKeysModified;
		}
			
		// Sets the list of relations
		public void setLstForeignKeysModified(ArrayList<ForeignKey> data){
			this.lstForeignKeysModified = data;
		}
		
		@Override
		public String toString(){
			String retString="";
			
			if(this.setOperator!=null){
				retString+=" set operator query\n";
				retString+=" operation: "+this.setOperator+" \n";
			}
				
			retString+=" Projection list \n";
			for(Node n:this.getProjectedCols())
				retString+=" "+n;
			retString+="\n From list \n";
			fromListElementsToString(this.fromListElements);
			retString+="\n Where Clause \n";
			for(Node n:this.getAllConds())
				retString+=" "+n;
			retString+="\n DNF Sel conds \n";
			for(Vector<Node> v:this.getAllDnfSelCond())
				retString+=" "+v;
			retString+="\n Group By \n";
			for(Node n:this.groupByNodes)
				retString+=" "+n;
			retString+="\n Having \n";
			for(Node n:this.lsthavingClauses)
				retString+=" "+n;
			retString+="\n Order By \n";
			for(Node n:this.orderByNodes)
				retString+=" "+n;
			if( this.getFromClauseSubqueries().size()>0){
			retString+="\n From Subqueries \n";
			for(QueryStructure qp:this.getFromClauseSubqueries())
				retString+="          "+qp.toString();	
			}
			if( this.getWhereClauseSubqueries().size()>0){
				retString+="\n Where Subqueries  \n";
				for(QueryStructure qp:this.getWhereClauseSubqueries())
					retString+="          "+qp.toString();	
			}

			return retString;
		}
		
		private static String fromListElementsToString(Vector<FromClauseElement> visitedFromListElements) {
			String retString="";
			for(FromClauseElement fle:visitedFromListElements){
				if(fle!=null && (fle.getTableName()!=null||fle.getTableNameNo()!=null))
					retString+="\n "+fle.toString();
				else if(fle!=null && fle.getSubQueryStructure()!=null){
					retString+="\n "+fle.toString();
					retString+=fromListElementsToString(fle.getSubQueryStructure().getFromListElements());
				}
				else if(fle!=null && fle.getBag()!=null && !fle.getBag().isEmpty()){
					retString+="\n "+fle.toString();
					retString+=fromListElementsToString(fle.getBag());				
				}	
			}
			return retString;
		}

		public Query getQuery() {
			return query;
		}

		public void setQuery(Query query) {
			this.query = query;
		}


		public TableMap getTableMap() {
			return tableMap;
		}

		public void setTableMap(TableMap tableMap) {
			this.tableMap = tableMap;
		}


		public void setFromClauseSubqueries(Vector<QueryStructure> fromClauseSubqueries) {
			FromClauseSubqueries = fromClauseSubqueries;
		}



		public Vector<ConjunctQueryStructure> getConjuncts() {
			return conjuncts;
		}

		public void setConjuncts(Vector<ConjunctQueryStructure> conjuncts) {
			this.conjuncts = conjuncts;
		}

		public Vector<Vector<Node>> getDnfCond() {
			return dnfCond;
		}

		public void setDnfCond(Vector<Vector<Node>> dnfCond) {
			this.dnfCond = dnfCond;
		}

		public Vector<Vector<Node>> getAllDnfSelCond() {
			return allDnfSelCond;
		}

		public void setAllDnfSelCond(Vector<Vector<Node>> allDnfSelCond) {
			this.allDnfSelCond = allDnfSelCond;
		}

		public Vector<Vector<Node>> getDnfLikeConds() {
			return dnfLikeConds;
		}

		public void setDnfLikeConds(Vector<Vector<Node>> dnfLikeConds) {
			this.dnfLikeConds = dnfLikeConds;
		}

		public Vector<Vector<Node>> getDnfIsNullConds() {
			return dnfIsNullConds;
		}

		public void setDnfIsNullConds(Vector<Vector<Node>> dnfIsNullConds) {
			this.dnfIsNullConds = dnfIsNullConds;
		}

		public Vector<Vector<Node>> getAllDnfSubQuery() {
			return allDnfSubQuery;
		}

		public void setAllDnfSubQuery(Vector<Vector<Node>> allDnfSubQuery) {
			this.allDnfSubQuery = allDnfSubQuery;
		}

		public Vector<Vector<Node>> getDnfJoinCond() {
			return dnfJoinCond;
		}

		public void setDnfJoinCond(Vector<Vector<Node>> dnfJoinCond) {
			this.dnfJoinCond = dnfJoinCond;
		}


		public Vector<Node> getLhsRhsConds() {
			return lhsRhsConds;
		}

		public JoinTreeNode getRoot() {
			return root;
		}

		public Vector<Node> getAllCondsExceptSubQuery() {
			return allCondsExceptSubQuery;
		}

		public void setAllCondsExceptSubQuery(Vector<Node> allCondsExceptSubQuery) {
			this.allCondsExceptSubQuery = allCondsExceptSubQuery;
		}

		public Vector<Node> getAllSubQueryConds() {
			return allSubQueryConds;
		}

		public void setSubQueryConds(Vector<Node> subQueryConds) {
			this.allSubQueryConds = subQueryConds;
		}

		public Vector<AggregateFunction> getAggFunc() {
			return aggFunc;
		}

		public void setAggFunc(Vector<AggregateFunction> aggFunc) {
			this.aggFunc = aggFunc;
		}

		public Vector<Node> getGroupByNodes() {
			return groupByNodes;
		}

		public void setGroupByNodes(Vector<Node> groupByNodes) {
			this.groupByNodes = groupByNodes;
		}

		public Node getHavingClause() {
			return havingClause;
		}
		
		public void setHavingClause(Node n) {
			this.havingClause = n;
		}
		
		public Vector<Node> getlstHavingClauses() {
			return lsthavingClauses;
		}

		public void setlstHavingClauses(Node n) {		
			
				if(n != null && n.getNodeType()!=null&& !n.getNodeType().equals(Node.getAndNodeType()) && n.getNodeType().equals(Node.getBroNodeType())){
					this.getlstHavingClauses().add(n);
				}else if(n != null && n.getNodeType()!=null && n.getNodeType().equals(Node.getAndNodeType())){
					this.setlstHavingClauses(n.getLeft());
					this.setlstHavingClauses(n.getRight());
				}	
		}
		
		/* @author mathew on June 18 2016
		 * getter-setter functions for order by list
		 */
		public Vector<Node> getOrderByNodes() {
			return orderByNodes;
		}

		public void setOrderByNodes(Vector<Node> orderByNodes) {
			this.orderByNodes = orderByNodes;
		}
		
		
		/* added by mathew on 1st october 2016
		 * getter-sett function for fromListElements
		 */
		public Vector<FromClauseElement> getFromListElements(){
			return this.fromListElements;
		}
		
		public void setFromListElements(Vector<FromClauseElement> FLEs){
			this.fromListElements=FLEs;
		}

		public Vector<Node> getSelectionConds() {
			return selectionConds;
		}

		public Vector<Node> getIsNullConds() {
			return isNullConds;
		}

		public Vector<Node> getLikeConds() {
			return likeConds;
		}

		public Vector<Node> getAllConds() {
			return allConds;
		}

		public Vector<Vector<Node>> getEquivalenceClasses() {
			return equivalenceClasses;
		}

		public Vector<Node> getForeignKeys() {
			return foreignKeys;
		}

		public HashMap<Integer, Vector<JoinClauseInfo>> getInConds() {
			return inConds;
		}

		public Vector<Node> getJoinConds() {
			return joinConds;
		}

		public Vector<Node> getProjectedCols() {
			return projectedCols;
		}


		public Vector<QueryStructure> getFromClauseSubqueries(){
			return this.FromClauseSubqueries;
		}

		public Vector<QueryStructure> getWhereClauseSubqueries(){
			return this.WhereClauseSubqueries;
		}

		public QueryStructure(TableMap tableMap) {
			this.tableMap = tableMap;
			this.inOrderList = new Vector<TreeNode>();
			this.foreignKeyVector = new Vector<JoinClauseInfo>();
			orNode = new ORNode();
			this.conjuncts = new Vector<ConjunctQueryStructure>();
			dnfCond = new Vector<Vector<Node>>();
			dnfJoinCond = new Vector<Vector<Node>>();
			dnfLikeConds = new Vector<Vector<Node>>();
			allDnfSelCond = new Vector<Vector<Node>>();
			dnfIsNullConds = new Vector<Vector<Node>>();
			allDnfSubQuery =new Vector<Vector<Node>>();
			//setCaseConditions(new Vector<CaseCondition>());
			caseConditionMap = new HashMap<Integer,CaseExpression>();
			//orNode = new ORNode();
			this.foreignKeyVectorModified = new Vector<ForeignKey>();

			inConds = new HashMap<Integer, Vector<JoinClauseInfo>>();
			allConds = new Vector<Node>();
			equivalenceClasses = new Vector<Vector<Node>>();

			joinConds = new Vector<Node>();
			foreignKeys = new Vector<Node>();
			projectedCols = new Vector<Node>();
			selectionConds = new Vector<Node>();
			likeConds = new Vector<Node>();
			isNullConds = new Vector<Node>();
			inClauseConds = new Vector<Node>();
			aggFunc = new Vector<AggregateFunction>();
			groupByNodes = new Vector<Node>();
			//following line added by mathew on 25 June 2016
			this.orderByNodes=new Vector<Node>();
			lsthavingClauses = new Vector<Node>();
			allSubQueryConds = new Vector<Node>();
			allCondsExceptSubQuery = new Vector<Node>();

			root = null;
			lhsRhsConds = new Vector<Node>();

			this.FromClauseSubqueries = new Vector<QueryStructure>();
			this.WhereClauseSubqueries = new Vector<QueryStructure>();
			//added by mathew on oct 1st 2016
			this.fromListElements=new Vector<FromClauseElement>();
			this.lstRelations=new ArrayList<String>();
			this.lstRelationInstances=new ArrayList<String>();

		}
		
		/** @author mathew
		 * @param input
		 */
		public static String replaceFormatForRowLists(String input){
			// 1st pattern (a1,b1,...)=(a2,b2,...)
			Pattern p=Pattern.compile("(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))\\s*=\\s*(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))");
			Matcher m=p.matcher(input);
			int index=0;
			while(m.find(index)){
				index=m.start()+1;
				input=m.replaceAll(" ROW "+m.group(1)+"= ROW "+m.group(2));
			}
			//2nd pattern (a1,b1,...)!=(a2,b2,...)
			p=Pattern.compile("(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))\\s*!=\\s*(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))");
			m=p.matcher(input);
			index=0;
			while(m.find(index)){
				index=m.start()+1;
				input=m.replaceAll(" ROW "+m.group(1)+"!= ROW "+m.group(2));
			}
			//3rd pattern (a1,b1,...)<>(a2,b2,...)
			p=Pattern.compile("(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))\\s*<>\\s*(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))");
			m=p.matcher(input);
			index=0;
			while(m.find(index)){
				index=m.start()+1;
				input=m.replaceAll(" ROW "+m.group(1)+"<> ROW "+m.group(2));
			}
			
			//4th pattern (a1,b1,...) in or (a1,b1,...) IN
			p=Pattern.compile("(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))\\s*[Ii][Nn] ");
			m=p.matcher(input);
			index=0;
			while(m.find(index)){
				index=m.start()+1;
				input=m.replaceAll(" ROW "+m.group(1)+" IN ");
			}

			//5th pattern NOT IN version of 4th pattern
			p=Pattern.compile("(\\([^\\(&&[^\\)]]+,[^\\(&&[^\\)]]+\\))\\s*[nN][oO][tT]\\s+[Ii][Nn] ");
			m=p.matcher(input);
			index=0;
			while(m.find(index)){
				index=m.start()+1;
				input=m.replaceAll(" ROW "+m.group(1)+" NOT IN ");
			}
			logger.log(Level.FINE, "replaced string after reformatting row lists: "+input);
			return input;
		}

		/**@author mathew
		 * 
		 * @param input
		 * 
		 * replace a patterns of the form <group by (a, b, )> by <group by a, b, > in input  
		 */
		public static String replaceFormatForGroupBy(String input){
			
			Pattern p=Pattern.compile("[gG][rR][oO][uU][pp]\\s+[bB][yY]\\s*\\(([^\\(&&[^\\)]]+)\\)");
			Matcher m=p.matcher(input);
			int index=0;
			while(m.find(index)){
				index=m.start()+1;
				input=m.replaceAll(" GROUP BY "+m.group(1)+" ");
			}
			return input;
			
		}
		
		public static String replaceKey(String query, String src, String target){
			String srcUpper=src.toUpperCase();
			String targetUpper=target.toUpperCase();
			query=query.replace(src, targetUpper);
			query=query.replace(srcUpper, targetUpper);
			return query;
		}

	   
		
		public void buildQueryStructure(String queryId, String queryString)  throws Exception {
			AppTest_Parameters dbAppParameters=new AppTest_Parameters();
			buildQueryStructure(queryId, queryString,dbAppParameters);
		}
		
		
		/**
		 * Parses the given query and builds query structure for data generation
		 * @param queryId Query Id 
		 * @param queryString Query String
		 * @param dbAppParameters Application parameters
		 * @throws Exception
		 */
		
		public void buildQueryStructure(String queryId, String queryString,AppTest_Parameters dbAppParameters ) throws Exception {
			
			try{
				queryString=queryString.trim().replaceAll("\n+", " ");
				queryString=queryString.trim().replaceAll(" +", " ");
				
				//JSQL PArser does not accept NATURAL LEFT OUTER . So Replace for parsing - To be changed in parser
				//queryString = queryString.replace( "NATURAL LEFT OUTER","NATURAL");
				//queryString = queryString.replace("NATURAL RIGHT OUTER","NATURAL");
				
				//establishing eqivalence of LEFT (resp. RIGHT, resp. FULL) JOIN and
				// LEFT (resp. RIGHT, resp. FULL) OUTER JOIN, added by mathew on 7/FEB/2016
				queryString = replaceKey(queryString, "left join","left outer join");
				queryString = replaceKey(queryString, "right join","right outer join");
				queryString = replaceKey(queryString, "full join","full outer join");
				
				queryString = replaceKey(queryString, "except all","except");
				queryString = replaceKey(queryString, "union all","union");
				queryString = replaceKey(queryString, "intersect all","intersect");

				queryString = replaceFormatForRowLists(queryString);
				queryString = replaceFormatForGroupBy(queryString);

				buildQueryStructureJSQL(queryId, queryString, true,dbAppParameters);

			}catch(ParseException ex){
				
				//logger.log(Level.SEVERE," Function buildQueryStructure : "+ex.getMessage(),ex);
				String tempStr="";
				for(StackTraceElement ele:ex.getStackTrace())
					tempStr+="\n"+ele.toString();

				throw new Exception("QueryStructure.java: buildQueryStructure() : JSQLParser Error : Query Parsing failed for the following query : \n"+queryString+" \n. \n Please check the logs for details.\n"+tempStr); 
			} catch(Exception e){
				//logger.log(Level.SEVERE," Function buildQueryStructure : "+e.getMessage(),e);			
				String tempStr="";
				for(StackTraceElement ele:e.getStackTrace())
					tempStr+="\n"+ele.toString();
				throw new Exception("QueryStructure.java: buildQueryStructure() : Building Query Structure failed for the following query : \n"+queryString+" \n. \n Please check the logs for details.\n"+tempStr);  
			}
		}
	   
	    /* rename of old method parseQueryJSQL
	     *       
	     */
		public void inToExists()
		{
			for(int i=0;i<this.getWhereClauseSubqueries().size();i++)
			{
				QueryStructure q = this.getWhereClauseSubqueries().get(i);
				if(q.getQueryType().type!=null && q.getQueryType().type.equalsIgnoreCase("IN"))
				{
					q.getQueryType().type="EXISTS";
					Node temp = new Node();
					temp.left = q.getQueryType().left;
					temp.right = q.getLstProjectedCols().get(0);
					temp.operator = "=";
					q.getLstJoinConditions().add(temp);
					q.getQueryType().setLeft(null);
					this.getLstSubQConnectives().set(i, "EXISTS");
					q.setIsDistinct(false);
				}
			}
		}
		public void notInToNotExists()
		{
			for(int i=0;i<this.getWhereClauseSubqueries().size();i++)
			{
				QueryStructure q = this.getWhereClauseSubqueries().get(i);
				if(q.getQueryType().type!=null && q.getQueryType().type.equalsIgnoreCase("NOT IN"))
				{
					q.getQueryType().type="NOT EXISTS";
					Node temp = new Node();
					temp.left = q.getQueryType().left;
					temp.right = q.getLstProjectedCols().get(0);
					temp.operator = "=";
					q.getLstJoinConditions().add(temp);
					q.getQueryType().setLeft(null);
					this.getLstSubQConnectives().set(i, "NOT EXISTS");
					q.setIsDistinct(false);
				}
			}
		}
		public void anyToExists()
		{
			for(int i=0;i<this.getWhereClauseSubqueries().size();i++)
			{
				QueryStructure q = this.getWhereClauseSubqueries().get(i);
				if(q.getQueryType().right!=null && q.getQueryType().right.type!=null && q.getQueryType().right.type.equalsIgnoreCase("ANY"))
				{
					q.getQueryType().type="EXISTS";
					Node temp = new Node();
					temp.left = q.getQueryType().left;
					temp.right = q.getLstProjectedCols().get(0);
					temp.operator = q.getQueryType().operator;
					q.getLstJoinConditions().add(temp);
					q.getQueryType().setLeft(null);
					this.getLstSubQConnectives().add("EXISTS");
					q.setIsDistinct(false);
				}
			}
		}
		public void flattenFromClauseInnerJoin()
		{
			boolean isInner=true;
			for(Node joinConds : this.getLstJoinConditions())
			{
				if(!joinConds.joinType.equalsIgnoreCase("INNER JOIN"))
				{
					isInner=false;
					break;
				}
			}
			if(isInner)
			{
				for(int i=0;i<this.getFromClauseSubqueries().size();i++)
				{
					QueryStructure temp = this.getFromClauseSubqueries().elementAt(i);
					this.lstSelectionConds.addAll(temp.getLstSelectionConditions());
					this.lstRelationInstances.addAll(temp.getLstRelationInstances());
					this.lstJoinConditions.addAll(temp.getLstJoinConditions());
				}
				this.getFromClauseSubqueries().clear();
			}
		}
		public void allToNotExists()
		{
			for(int i=0;i<this.getWhereClauseSubqueries().size();i++)
			{
				QueryStructure q = this.getWhereClauseSubqueries().get(i);
				if(q.getQueryType().right!=null && q.getQueryType().right.type!=null && q.getQueryType().right.type.equalsIgnoreCase("ALL"))
				{
					q.getQueryType().type="NOT EXISTS";
					Node temp = new Node();
					temp.left = q.getQueryType().left;
					temp.right = q.getLstProjectedCols().get(0);
					temp.operator = q.getQueryType().operator;
					temp.compliment();
					q.getLstJoinConditions().add(temp);
					q.getQueryType().setLeft(null);
					this.getLstSubQConnectives().add("NOT EXISTS");
					q.setIsDistinct(false);
				}
			}
		}
		public void buildQueryStructureJSQL(String queryId, String queryString, boolean debug,AppTest_Parameters dbAppParameters)
				throws Exception {
			logger.fine("beginning to parse query");
			try{
				if(this.query==null)
					this.query = new Query(queryId, queryString);
				else
					this.query.setQueryString(queryString);
				logger.fine("querystring is : "+queryString);
				CCJSqlParserManager pm = new CCJSqlParserManager();
				Statement stmt = pm.parse(new StringReader(queryString));
				//SQLParser sqlParser = new SQLParser();
				PlainSelect plainSelect = null;
				this.queryType = new Node();
				if (stmt instanceof Select){
					//Check if it is plain select statement without with clause
					if(((Select) stmt).getSelectBody() instanceof PlainSelect &&
							((Select)stmt).getWithItemsList() == null){						
						 plainSelect = (PlainSelect)((Select) stmt).getSelectBody();
//						ProcessResultSetNode.processResultSetNodeJSQL(plainSelect, debug, this);
						ProcessSelectClause.ProcessSelect(plainSelect,  this, dbAppParameters);
						
						// add transformation
						for(int i=0;i<this.getWhereClauseSubqueries().size();i++)
						{
							ConjunctQueryStructure conjunct = this.getConjuncts().get(0);
							Node temp = conjunct.allSubQueryConds.elementAt(i);
							this.getWhereClauseSubqueries().elementAt(i).setQueryType(temp);
						}
						// Convert IN to EXISTS
						inToExists();

  					   // Convert NOT IN to NOT EXISTS
						notInToNotExists();
						
					   //Convert ANY to EXISTS
						anyToExists();
				
						// Convert ALL to NOT EXISTS
						allToNotExists();	
						
						//Flatten from Clause
						flattenFromClauseInnerJoin();
						
					}
			
					//Check if query contains WithItem list - then Query is of the form  WITH S AS ()
					if(((Select) stmt).getSelectBody() instanceof PlainSelect 
							&& ((Select)stmt).getWithItemsList() != null){
																
						stmt=transformQueryForWithAs((Select)stmt);
						//PlainSelect selectClause =(PlainSelect) ((Select) stmt).getSelectBody();
						
						String alteredWithQuery=((Select)stmt).getSelectBody().toString();
						logger.log(Level.FINE,"transformed query after substitution of Witt aliases\n"+ alteredWithQuery);
		
						//ProcessResultSetNode.processResultSetNodeJSQL((PlainSelect)((Select) stmt).getSelectBody(), debug, this);
						ProcessSelectClause.ProcessSelect((PlainSelect)((Select) stmt).getSelectBody(),  this, dbAppParameters);
						
						// add transformation
						for(int i=0;i<this.getWhereClauseSubqueries().size();i++)
						{
							ConjunctQueryStructure conjunct = this.getConjuncts().get(0);
							Node temp = conjunct.allSubQueryConds.elementAt(i);
							this.getWhereClauseSubqueries().elementAt(i).setQueryType(temp);
						}
						// Convert IN to EXISTS
						inToExists();

  					   // Convert NOT IN to NOT EXISTS
						notInToNotExists();
						
					   //Convert ANY to EXISTS
						anyToExists();
				
						// Convert ALL to NOT EXISTS
						allToNotExists();	
						
						//Flatten from Clause
						flattenFromClauseInnerJoin();
						
					}
					
					//If it is instance of SetOperationList - UNION,EXCEPT OR INTERSECT
					else if(((Select) stmt).getSelectBody() instanceof SetOperationList){
						
						stmt=transformQueryForWithAs((Select)stmt);
						String alteredWithQuery=((Select)stmt).getSelectBody().toString();
						logger.log(Level.FINE,"transformed query after substitution of with aliases\n"+ alteredWithQuery);
						
						SetOperationList setOpList = (SetOperationList)((Select) stmt).getSelectBody();
									
							//Test in different scenarios - joins in SET  Op and test
							//Get the select list to check it has select statement or nested SET operation
							processQueriesForSetOp(setOpList,debug, dbAppParameters); 
						} 
					}
				}catch (ParseException e){
					logger.log(Level.SEVERE,"Error in Query parsing : "+e.getMessage(),e);
					throw new Exception(e.getMessage());
			}
				
			}
		
		/** @author mathew
		 *  Transforms subjoin statements in from items eg: (A Natural Join B)
		 *  Splits the subjoin into Left (in this case <A>), and Join ( in the above case <Natural Join B>), deal with them separately 
		 */

		SubJoin transformSubJoinForWithAs(WithItem srcWithItem, SubJoin subJoin){
			FromItem leftFromItem=subJoin.getLeft();
			//checks the left item (assumed to be a  Table), if its name is the same as the name of the 
			// input with item, the name is substitued by its corresponding definition
			if(leftFromItem instanceof net.sf.jsqlparser.schema.Table){			
				net.sf.jsqlparser.schema.Table tarTable= (net.sf.jsqlparser.schema.Table)leftFromItem;
				if(tarTable.getName().equalsIgnoreCase(srcWithItem.getName())){
					logger.log(Level.FINE,"found as from item");
					//Get withItem and create new SubSelect if fromItem name is equal to with item name
					Alias a;
					if(tarTable.getAlias()!=null)
						a=new Alias(tarTable.getAlias().getName());
					else
						a = new Alias(srcWithItem.getName());
					a.setUseAs(true);
					SubSelect sub = new SubSelect();
					sub.setSelectBody(srcWithItem.getSelectBody());
					sub.setAlias(a);
					subJoin.setLeft(sub);					
				}
			}
			//checks if the left item (assumed to be a  Table or SubJoin itself) is a SubJoin, if then recursive call
			else if(leftFromItem instanceof SubJoin){
				transformSubJoinForWithAs(srcWithItem,(SubJoin)leftFromItem);
			}
			// if fromitem is a subselect , then call the corresponding method that handles it
			else if(leftFromItem instanceof SubSelect){
				logger.log(Level.FINE,"processing subselect");
				SubSelect leftSubSelect=(SubSelect) leftFromItem;
				transformSubSelectForWithAs(srcWithItem, leftSubSelect);
			}
			// deals with the right item of the subjoin
			FromItem rightFromItem=subJoin.getJoin().getRightItem();
			//checks if the right item (assumed to be a  Table or SubJoin itself) is a Table, then if its name is the same as the name of the 
			// input with item, the name is substituted by its corresponding definition

			if(rightFromItem instanceof net.sf.jsqlparser.schema.Table){			
				net.sf.jsqlparser.schema.Table tarTable= (net.sf.jsqlparser.schema.Table)rightFromItem;
				if(tarTable.getName().equalsIgnoreCase(srcWithItem.getName())){
					logger.log(Level.FINE,"found as from item");
					//Get withItem and create new SubSelect if fromItem name is equal to with item name
					Alias a;
					if(tarTable.getAlias()!=null)
						a=new Alias(tarTable.getAlias().getName());
					else
						a = new Alias(srcWithItem.getName());
					a.setUseAs(true);
					SubSelect sub = new SubSelect();
					sub.setSelectBody(srcWithItem.getSelectBody());
					sub.setAlias(a);
					subJoin.getJoin().setRightItem(sub);					
				}
			}
			//checks if the right item (assumed to be a  Table or SubJoin itself) is a SubJoin, if then recursive call
			else if(rightFromItem instanceof SubJoin){
				transformSubJoinForWithAs(srcWithItem,(SubJoin)rightFromItem);
			}
			else if(rightFromItem instanceof SubSelect){
				logger.log(Level.FINE,"processing subselect");
				SubSelect rightSubSelect=(SubSelect) rightFromItem;
				transformSubSelectForWithAs(srcWithItem, rightSubSelect);
			}
			return subJoin;
		}

		/** @author mathew
		 *  Transforms the given input PlainSelect statement by substituting  alias name of its first argument
		 *   with its corresponding definition in its select body
		 * 
		 */

		PlainSelect transformPlainSelectForWithAs(WithItem srcWithItem, PlainSelect tarSelectClause){
			// Starts by dealing with from items, a from item can be a Table name, a subselect statement, or a subjoin statement
			FromItem tarFromItem=tarSelectClause.getFromItem();
			// if fromitem is a Table then check if its name is equal to the name of the input withitem, if yes substitutes its occurence
			// by its definition
			if(tarFromItem instanceof net.sf.jsqlparser.schema.Table){
				net.sf.jsqlparser.schema.Table tarTable= (net.sf.jsqlparser.schema.Table)tarFromItem;
				if(tarTable.getName().equalsIgnoreCase(srcWithItem.getName())){
					logger.log(Level.FINE,"found as from item");
					//Get withItem and create new SubSelect if fromItem name is equal to with item name
					Alias a;
					if(tarTable.getAlias()!=null)
						a=new Alias(tarTable.getAlias().getName());
					else
						a = new Alias(srcWithItem.getName());
					a.setUseAs(true);
					SubSelect sub = new SubSelect();
					sub.setSelectBody(srcWithItem.getSelectBody());
					sub.setAlias(a);
					tarSelectClause.setFromItem(sub);					
				}	
			}
			// if fromitem is a subselect , then call the corresponding method that handles it
			else if(tarFromItem instanceof SubSelect){
				logger.log(Level.FINER,"processing subselect");
				SubSelect tarSubSelect=(SubSelect) tarFromItem;
				transformSubSelectForWithAs(srcWithItem, tarSubSelect);
			}
			// if fromitem is a subjoin, then call the corresponding method that handles it
			else if(tarFromItem instanceof SubJoin){
				logger.log(Level.FINER,"processing subjoin");
				SubJoin tarSubJoin=(SubJoin)tarFromItem;
				tarSubJoin=transformSubJoinForWithAs(srcWithItem,tarSubJoin);
			}
		
			// WITH AS names can be in joins as well, call the corresponding method that handles it
			if(tarSelectClause.getJoins() != null ){
				logger.log(Level.FINER,"processing joins");
				List<Join> joinList = tarSelectClause.getJoins();
				transformJoinsForWithAs(srcWithItem, joinList);

			}
			// Now deal with its where clauses
			transformWhereClauseForWithAs(srcWithItem, tarSelectClause.getWhere());
			return tarSelectClause;
		}
		
		/** @author mathew  
		 *  Extract the select statement from the subselect. It can be either a PlainSelect 
		 *  or a SetOperation, deal with their transformations separately
		 */

		private void transformSubSelectForWithAs(WithItem srcWithItem, SubSelect tarSubSelect) {
			//when the select body is a plain select statement, then call the corresponding method that handles it
			if(tarSubSelect.getSelectBody() instanceof PlainSelect){
				PlainSelect tempSelect=(PlainSelect)tarSubSelect.getSelectBody();
				transformPlainSelectForWithAs(srcWithItem, tempSelect);
			}
			//where the select body is a set operation (union, intersect etc), then call the corresponding method that handles it
			else if(tarSubSelect.getSelectBody() instanceof SetOperationList){
				SetOperationList setOpList=(SetOperationList)tarSubSelect.getSelectBody();
				transformSetOperationListForWithAs(srcWithItem, setOpList);		
			}

		}

		/** @author mathew
		 *  Transforms joinList in the from clause of a select statement by looking for occurences 
		 *  of the name of the input with item, if any occurence is found its corresponding definition is substituted as a subselect
		 *  Each join item is assumed to be a table, a subselect, or a subjoin
		 */
		private void transformJoinsForWithAs(WithItem srcWithItem, List<Join> joinList){
			for(int k=0; k < joinList.size(); k++){
				Join jcl = joinList.get(k);	
				FromItem tarJoinFromItem=jcl.getRightItem();				
				//if the join item is a table, then call the corresponding method that handles it
				if(tarJoinFromItem instanceof net.sf.jsqlparser.schema.Table){
					net.sf.jsqlparser.schema.Table tarTable= (net.sf.jsqlparser.schema.Table)tarJoinFromItem;
					if(tarTable.getName().equalsIgnoreCase(srcWithItem.getName())){
						logger.log(Level.FINER,"found with alias in from item of join");
						//Get withItem and create new SubSelect if fromItem name is equal to with item name
						Alias a;
						if(tarTable.getAlias()!=null)
							a=new Alias(tarTable.getAlias().getName());
						else
							a = new Alias(srcWithItem.getName());
						a.setUseAs(true);
						SubSelect sub = new SubSelect();
						sub.setSelectBody(srcWithItem.getSelectBody());
						sub.setAlias(a);
						jcl.setRightItem(sub);
					}	
				}
				//if join item is a subselect, then call the corresponding method that handles it
				else if(tarJoinFromItem instanceof SubSelect){
					logger.log(Level.FINER,"processing subselect in join");
					SubSelect tarSubSelect=(SubSelect) tarJoinFromItem;
					PlainSelect tempSelect=(PlainSelect)tarSubSelect.getSelectBody();
					transformPlainSelectForWithAs(srcWithItem, tempSelect);
				}
				//if join item is a subjoin, then call the corresponding method that handles it
				else if(tarJoinFromItem instanceof SubJoin){
					logger.log(Level.FINER,"processing subjoin in join");
					SubJoin tarSubJoin=(SubJoin)tarJoinFromItem;
					tarSubJoin=transformSubJoinForWithAs(srcWithItem,tarSubJoin);
				}

			}
		}
		
		/** @author mathew
		 *  Transforms the expression in the where clause of a select statement by looking for occurences 
		 *  of the name of the input with item, if any occurence is found its corresponding definition is substituted as a subselect
		 */
		private void transformWhereClauseForWithAs(WithItem srcWithItem, Expression whereClause) {
			//if whereClause is a subselect, then call the corresponding method that handles it
			if(whereClause instanceof SubSelect){
				SubSelect subSelect=(SubSelect) whereClause;
				transformPlainSelectForWithAs(srcWithItem, (PlainSelect)(subSelect.getSelectBody()));
			}
			//if whereClause is a BinaryExpression (AND, OR etc.), then 
			//split its operands and recursive calls using them as arguments 
			if(whereClause instanceof BinaryExpression){
				BinaryExpression binExpression = ((BinaryExpression) whereClause);
				transformWhereClauseForWithAs(srcWithItem, binExpression.getLeftExpression());
				transformWhereClauseForWithAs(srcWithItem, binExpression.getRightExpression());
			}
			//if whereClause is a Exists Expression, then recurisively call using its right expression as argument
			else if(whereClause instanceof ExistsExpression){
				logger.log(Level.FINER,"transforming exists in where clause");
				ExistsExpression existsExpression = (ExistsExpression)whereClause;
				transformWhereClauseForWithAs(srcWithItem,existsExpression.getRightExpression());
			}
			//if whereClause is a InExpression, then handle its left and right operands separately
			else if(whereClause instanceof InExpression){
				logger.log(Level.FINER,"transforming inExpression in where clause");
				InExpression inExpression = (InExpression)whereClause;
				if (inExpression.getLeftItemsList() instanceof SubSelect){
					transformWhereClauseForWithAs(srcWithItem, (SubSelect)inExpression.getLeftItemsList());
				}
				if(inExpression.getRightItemsList() instanceof SubSelect){
					transformWhereClauseForWithAs(srcWithItem, (SubSelect)inExpression.getRightItemsList());
				}
			}
			//if whereClause is a All comparision Expression, then recurisively call using its subselect as argument
			else if(whereClause instanceof AllComparisonExpression){
				logger.log(Level.FINER,"transforming all comparison in where clause");
				AllComparisonExpression ace = (AllComparisonExpression) whereClause;
				transformPlainSelectForWithAs(srcWithItem, (PlainSelect)(ace.getSubSelect().getSelectBody()));
			}
			//if whereClause is a Any comparision Expression, then recurisively call using its subselect as argument
			else if(whereClause instanceof AnyComparisonExpression){
				logger.log(Level.FINER,"transforming any comparison in where clause");
				AnyComparisonExpression ace = (AnyComparisonExpression) whereClause;
				transformPlainSelectForWithAs(srcWithItem, (PlainSelect)(ace.getSubSelect().getSelectBody()));
			}

		}

		/** @author mathew
		 *  Transforms the given input Select statement by substituting  alias names with their corresponding definitions
		 *  Also translates select statements in other with items
		 */
		private Select transformQueryForWithAs(Select selectClause) {
			// TODO Auto-generated method stub
			if(selectClause==null){
				logger.log(Level.FINE,"Empty select");
				return null;
			}
			
			List<WithItem> withItemsList=selectClause.getWithItemsList();
			if(withItemsList==null||withItemsList.isEmpty())
				return selectClause;
			
			for(int i=0;i<withItemsList.size();i++){
				// normalize column names in with items eg: With A(a) as (select name from ...)
				// is normalized to With A(a) as (select name as a from ...)
				WithItem srcWithItem=normalizeWithItem(withItemsList.get(i));
				logger.log(Level.FINER,"normalized with item"+srcWithItem);
				// now translate the subsequent with items by substituting the definition 
				// of withItem under consideration in their select bodies
				for(int j=i+1;j<withItemsList.size();j++){
					WithItem tarWithItem=withItemsList.get(j);
					//withItems select body can be a PlainSelect or a SetOperation
					if(tarWithItem.getSelectBody() instanceof PlainSelect){
						PlainSelect tarSelectClause =(PlainSelect) tarWithItem.getSelectBody();
						transformPlainSelectForWithAs(srcWithItem, tarSelectClause);
					}
					else if(tarWithItem.getSelectBody() instanceof SetOperationList){
						SetOperationList setOpList=(SetOperationList)tarWithItem.getSelectBody();
						transformSetOperationListForWithAs(srcWithItem, setOpList);
					}
				}
				
				// Now  translate the select body of the input select statement  by substituting the definition 
				// of withItem under consideration in its select bodies

				//select body can be a PlainSelect or a SetOperation
				if(selectClause.getSelectBody() instanceof PlainSelect){
					PlainSelect tarSelectClause =(PlainSelect) selectClause.getSelectBody();
					transformPlainSelectForWithAs(srcWithItem, tarSelectClause);
				}
				else if(selectClause.getSelectBody() instanceof SetOperationList){
					SetOperationList setOpList=(SetOperationList)selectClause.getSelectBody();
					transformSetOperationListForWithAs(srcWithItem, setOpList);
				}
			}
			return selectClause;
		}

		/** @author mathew
		 *  Transforms set operation statements eg: (A Union (B INTERSECT C)
		 *  Splits the operands and deals with them separately 
		 */

		private void transformSetOperationListForWithAs(WithItem srcWithItem, SetOperationList setOpList) {
			// TODO Auto-generated method stub
			for(SelectBody selBody:setOpList.getSelects()){
				if(selBody instanceof PlainSelect){
					PlainSelect tarSelectClause=(PlainSelect) selBody;
					transformPlainSelectForWithAs(srcWithItem, tarSelectClause);
				}
				else if(selBody instanceof SetOperationList){
					transformSetOperationListForWithAs(srcWithItem, (SetOperationList)selBody);
				}
			}

		}

		/** @author mathew  
		 * 	 normalize column names in with items eg: With A(a) as (select name from ...)			
		 *  is normalized to With A(a) as (select name as a from ...)
		 */
		private WithItem normalizeWithItem(WithItem withItem) {
			// TODO Auto-generated method stub	
			
			if(withItem.getSelectBody() instanceof PlainSelect){
				PlainSelect selectClause =(PlainSelect) withItem.getSelectBody();
				normalizeSelectedColumnsForWithItem(withItem, selectClause);
				
			}
			if(withItem.getSelectBody() instanceof SetOperationList){
				SetOperationList setOpList=(SetOperationList)withItem.getSelectBody();
				normalizeSelectedColumnsForWithItem(withItem, setOpList);

			}
			logger.log(Level.FINE," normalized with item "+withItem.getName()+" withItemBody: "+withItem.getSelectBody());
			return withItem;
		}
		
		
		/** @author mathew  
		 * 	 deals with normalization of Setoperation Queries. Eg. A, B, C are normalized individually 
		 *  in a select query of the form A UNION B UNION C
		 */
		private void normalizeSelectedColumnsForWithItem(WithItem withItem, SetOperationList setOpList) {
			for(SelectBody selBody:setOpList.getSelects()){
				if(selBody instanceof PlainSelect){
				PlainSelect selectClause=(PlainSelect) selBody;
				normalizeSelectedColumnsForWithItem(withItem, selectClause);
				}
				else if(selBody instanceof SetOperationList){
					normalizeSelectedColumnsForWithItem(withItem, (SetOperationList)selBody);
				}
			}
		}

		/** @author mathew  
		 * 	 deals with normalization of columns items in PlainSelect Queries. eg: With A(a) as (select name from ...)			
		 *  is normalized to With A(a) as (select name as a from ...)
		 *  
		 */
		private void normalizeSelectedColumnsForWithItem(WithItem withItem, PlainSelect selectClause) {
			// TODO Auto-generated method stub
			if(withItem.getWithItemList()!=null &&!withItem.getWithItemList().isEmpty() ){
				for(int i=0;i<withItem.getWithItemList().size();i++){
					SelectItem withSelItem=withItem.getWithItemList().get(i);
					if(selectClause.getSelectItems()!=null &&  !selectClause.getSelectItems().isEmpty() && selectClause.getSelectItems().size()>i){
						SelectItem sItem=selectClause.getSelectItems().get(i);
						if(sItem instanceof SelectExpressionItem){
							SelectExpressionItem selExpItem=(SelectExpressionItem)sItem;
							Alias a = new Alias(withSelItem.toString());
							a.setUseAs(true);
							selExpItem.setAlias(a);
						}
					}
				}
			}
		}


		
		/** modified by mathew on 22 August 2016
		 * 
		 * operator precedence is assumed to equal for UNION, EXCEPT/MINUS, INTERSECT, and are treated 
		 * depending of the their order from left to right in a set operation list
		 * 
		 * This method gets the set operations from the SelectList and iterates to find 
		 * if any recursive Set Operations are there. 
		 * It calls parseQueryJSQL for the left and right items joined by the SET OPERATION
		 * and returns the result for further Set Operation processing.
		 * 
		 * @param setOpList
		 * @throws Exception
		 */
		public void processQueriesForSetOp(SetOperationList setOpList, boolean debug,AppTest_Parameters dbAppParameters) throws Exception {
			
			logger.log(Level.FINE," set operation List"+setOpList.toString());
			SetOperation setOperation =  setOpList.getOperations().get(0);
			
			if(setOperation instanceof ExceptOp || setOperation instanceof MinusOp || setOperation instanceof IntersectOp || setOperation instanceof UnionOp){
				
				if(setOperation instanceof ExceptOp || setOperation instanceof MinusOp){
					setOperator="EXCEPT";
				} else if (setOperation instanceof IntersectOp) {
					setOperator="INTERSECT";
				} else if(setOperation instanceof UnionOp){ 
					setOperator="UNION";
				}
				List<SelectBody> selectList = setOpList.getSelects();
				Iterator<SelectBody> selectListIt =selectList.iterator();
				if(selectListIt.hasNext()){
					Object nxtElement = selectListIt.next();				
					leftQuery = new QueryStructure(this.tableMap);
					leftQuery.queryType = new Node();
					if(nxtElement instanceof PlainSelect){
						PlainSelect left= (PlainSelect)nxtElement;
						
						if(leftQuery.query==null)
							leftQuery.query= new Query("q2", left.toString());
						else
							leftQuery.query.setQueryString(left.toString());
						
						//ProcessResultSetNode.processResultSetNodeJSQL(left, debug, leftQuery);
						ProcessSelectClause.ProcessSelect(left, leftQuery, dbAppParameters);
						this.projectedCols.addAll(leftQuery.projectedCols);
					}
					else if(nxtElement instanceof SetOperationList){
						leftQuery.buildQueryStructureJSQL("q2",((SetOperationList)nxtElement).toString(),debug, dbAppParameters);
					}
				}if(selectList.size()==2&&selectListIt.hasNext()){
					Object nxtElement = selectListIt.next();

					rightQuery = new QueryStructure(this.tableMap);
					rightQuery.queryType = new Node();
					if(nxtElement instanceof PlainSelect){
						PlainSelect right=(PlainSelect) nxtElement;
						if(rightQuery.query==null)
							rightQuery.query=new Query("q3",right.toString());
						else
							rightQuery.query.setQueryString(right.toString());
						
						//ProcessResultSetNode.processResultSetNodeJSQL(right, debug, rightQuery);			
						ProcessSelectClause.ProcessSelect(right, rightQuery, dbAppParameters);
						if(projectedCols.isEmpty())
							this.projectedCols.addAll(rightQuery.projectedCols);
						}
					else if(nxtElement instanceof SetOperationList){
						rightQuery.buildQueryStructureJSQL("q3",((SetOperationList)nxtElement).toString(),debug, dbAppParameters);
					}
				}
				/*The following else added by mathew on  22 August 2016
				 * To handle the case when selectList (has size>=3) is a chain of set operation list of more 
				 * than two elements in which case the 
				 * set operations from 2nd to last are added to tempSetOpList. Note that the first operation was already 
				 * processed by the preceding first if statement. Recursive call with argument as tempSetOpList
				 * */
				else{
					rightQuery = new QueryStructure(this.tableMap);
					SetOperationList tempSetOpList=new SetOperationList();
					List<Boolean> tempBrackets=new ArrayList<Boolean>();
					List<SetOperation> tempListOperations=new ArrayList<SetOperation>();
					List<SelectBody> tempListSelectBodies=new ArrayList<SelectBody>();
					for(int i=1;i<setOpList.getBrackets().size();i++)
						tempBrackets.add(setOpList.getBrackets().get(i));
					for(int i=1;i<setOpList.getOperations().size();i++)
						tempListOperations.add(setOpList.getOperations().get(i));
					for(int i=1;i<setOpList.getSelects().size();i++)
						tempListSelectBodies.add(setOpList.getSelects().get(i));
					tempSetOpList.setBracketsOpsAndSelects(tempBrackets, tempListSelectBodies, tempListOperations);
					rightQuery.buildQueryStructureJSQL("q3",tempSetOpList.toString(),debug, dbAppParameters);
				}
			}
			this.initializeQueryListStructures();
		}
			


		
		private String parseFromPart(StringTokenizer st, String newquery, int numberOfAlias, String subquery[], String aliasname[]){
			
			String token;
			
			while(st.hasMoreTokens()){
				token=st.nextToken();            
						
				if(token.equalsIgnoreCase("where")||token.equalsIgnoreCase("group")){
					newquery+=token+ " ";
					break;
				}			
				
				if(token.equals(",")){
					newquery+=token+ " ";
				}
				if(token.contains(",")){
					token+=" ";
					String tablenames[]=token.split(",");
					for(int j=0;j<tablenames.length;j++){
						boolean isPresent=false;
						for(int k=0;k<numberOfAlias;k++){
							if(tablenames[j].equals(aliasname[k])){
								newquery+=subquery[k] + " " + aliasname[k]+" ";
								isPresent=true;
							}
						}
						if(!isPresent){
							newquery+=tablenames[j]+" ";
						}
						newquery+=",";
					}
					newquery=newquery.substring(0,newquery.length()-1);

				}else if(token.contains(")")){
					String relationName = token.substring(0, token.length() - 1);				
					
						boolean isPresent=false;
						for(int k=0;k<numberOfAlias;k++){
							if(relationName.equals(aliasname[k])){
								newquery+=subquery[k] + " " + aliasname[k]+" ";
								isPresent=true;
							}
						}
						if(!isPresent){
							newquery+=relationName + " ";
						}
						newquery+=")";
					
				}else{
					boolean isPresent=false;
					for(int k=0;k<numberOfAlias;k++){
						if(token.equals(aliasname[k])){
							newquery+=subquery[k] + " " + aliasname[k]+" ";
							isPresent=true;
						}
					}
					if(!isPresent){
						newquery+=token+" ";
					}
				}

			}
			
			return newquery;		
		}

		public Vector<TreeNode> getInOrderList() {
			return inOrderList;
		}

		public void setInOrderList(Vector<TreeNode> inOrderList) {
			this.inOrderList = inOrderList;
		}


		public Vector<JoinClauseInfo> getForeignKeyVector() {
			return foreignKeyVector;
		}

		public void setForeignKeyVector(Vector<JoinClauseInfo> foreignKeyVector) {
			this.foreignKeyVector = foreignKeyVector;
		}

		public Vector<ForeignKey> getForeignKeyVectorModified() {
			return foreignKeyVectorModified;
		}

		public void setForeignKeyVectorModified(
				Vector<ForeignKey> foreignKeyVectorModified) {
			this.foreignKeyVectorModified = foreignKeyVectorModified;
		}

		public Vector<JoinClauseInfo> getForeignKeyVectorOriginal() {
			return foreignKeyVectorOriginal;
		}

		public void setForeignKeyVectorOriginal(
				Vector<JoinClauseInfo> foreignKeyVectorOriginal) {
			this.foreignKeyVectorOriginal = foreignKeyVectorOriginal;
		}


		public Vector<Node> getInClauseConds() {
			return inClauseConds;
		}

		public void setInClauseConds(Vector<Node> inClauseConds) {
			this.inClauseConds = inClauseConds;
		}		

		public QueryStructure getLeftQuery() {
			return leftQuery;
		}

		public void setLeftQuery(QueryStructure leftQuery) {
			this.leftQuery = leftQuery;
		}

		public QueryStructure getRightQuery() {
			return rightQuery;
		}

		public void setRightQuery(QueryStructure rightQuery) {
			this.rightQuery = rightQuery;
		}

		public Vector<Node> getAllAnyConds() {
			return allAnyConds;
		}

		public void setAllAnyConds(Vector<Node> allAnyConds) {
			this.allAnyConds = allAnyConds;
		}

		public int getParamCount() {
			return paramCount;
		}

		public void setParamCount(int paramCount) {
			this.paramCount = paramCount;
		}
		
		public static String[] cvcRelationalOperators = { "DUMMY", "=", "/=", ">",
				">=", "<", "<=", "&&"}; // IsNull and IsNotNull not supported currently
										/*&& added by mathew on 1st Aug 2016.*/


		public static String[] getCvcRelationalOperators() {
			return cvcRelationalOperators;
		}

		public static void setCvcRelationalOperators(String[] cvcRelationalOperators) {
			QueryStructure.cvcRelationalOperators = cvcRelationalOperators;
		}

		public void setInConds(HashMap<Integer, Vector<JoinClauseInfo>> inConds) {
			this.inConds = inConds;
		}

		public void setAllConds(Vector<Node> allConds) {
			this.allConds = allConds;
		}

		public void setSelectionConds(Vector<Node> selectionConds) {
			this.selectionConds = selectionConds;
		}

		public void setLikeConds(Vector<Node> likeConds) {
			this.likeConds = likeConds;
		}

		public void setIsNullConds(Vector<Node> isNullConds) {
			this.isNullConds = isNullConds;
		}

		public void setEquivalenceClasses(Vector<Vector<Node>> equivalenceClasses) {
			this.equivalenceClasses = equivalenceClasses;
		}

		public void setJoinConds(Vector<Node> joinConds) {
			this.joinConds = joinConds;
		}

		public void setForeignKeys(Vector<Node> foreignKeys) {
			this.foreignKeys = foreignKeys;
		}

		public void setProjectedCols(Vector<Node> projectedCols) {
			this.projectedCols = projectedCols;
		}

		public void setRoot(JoinTreeNode root) {
			this.root = root;
		}

		public void setAllSubQueryConds(Vector<Node> allSubQueryConds) {
			this.allSubQueryConds = allSubQueryConds;
		}

		public void setLhsRhsConds(Vector<Node> lhsRhsConds) {
			this.lhsRhsConds = lhsRhsConds;
		}

		public void setWhereClauseSubqueries(Vector<QueryStructure> whereClauseSubqueries) {
			WhereClauseSubqueries = whereClauseSubqueries;
		}
		
		public boolean getIsDistinct() {
			return this.isDistinct;
		}

		public void setIsDistinct(boolean isDistinct) {
			this.isDistinct = isDistinct;
		}

		public boolean alreadyNotExistInEquivalenceClass(ArrayList<Node> S, Node ece) {
			for (int i = 0; i < S.size(); i++) {
				Node temp = S.get(i);
				if (temp.getTableNameNo().equalsIgnoreCase(ece.getTableNameNo())
						&& temp.getColumn().getColumnName().equalsIgnoreCase(ece.getColumn().getColumnName()) )
					/*if(temp.getTable() == ece.getTable() &&
						temp.getColumn() == ece.getColumn())*/
					return false;
			}

			return true;
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
			//reviseJoinTables();
			reviseProjectedColumns();
			reviseGroupByColumns();
			reviseSelectionConditions();
			reviseHavingConditions();
			reviseOrderByColumns();
			updateEquivalenceClasses();

		}
		
		public void updateEquivalenceClasses() {
			// TODO Auto-generated method stub
			ArrayList<String> eliminatedRelations=this.lstRedundantRelations;
			boolean updateFlag=false;
			do{
				updateFlag=false;
				for(ArrayList<Node> eqClass:this.getLstEqClasses()){
					for(String elimRelation:eliminatedRelations){
						for(int i=0;i<eqClass.size();i++){
							Node n=eqClass.get(i);
							if(n.getTableNameNo().equalsIgnoreCase(elimRelation)){
								logger.log(Level.FINER," eq member deleted"+n+" from "+eqClass);
								eqClass.remove(i);
								updateFlag=true;
							}
						}
					}
				}
			}while(updateFlag);
		}
		
		public Map<String, HashMap<String, ArrayList<Pair>>> getRelationToRelationEquivalentNodes(){
			Map<String, ArrayList<Node>> relationToEqNodes = new HashMap<String, ArrayList<Node>>();
			Map<Node, ArrayList<Node> > nodeToEqNodes = new HashMap<Node, ArrayList<Node>>();
			Map<String, HashMap<String, ArrayList<Pair>>> relationToRelationEqNodes = new HashMap<String, HashMap<String, ArrayList<Pair>>>(); 
			if(lstEqClasses != null){
				for(ArrayList<Node> t : lstEqClasses){
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

		
		/*@ only the method name changed by mathew on May 5 2016
		 *  old name setRelations changed to reviseRelations
		 */
		public void reviseRelations(){
			Boolean found = false;
			
			Vector<String> tables = new Vector<String>(this.getLstRelations());
			
			//added by mathew on May 9, 2016
			this.lstRelations.clear();
			
			for(String n : tables){
				found = false;
				for(String t : lstRedundantRelations){
					/*assumes that tableNameNos are of the of the form
					 *  <tableNamei>, where 0 \leq i \leq 9 and 
					 *  tableName is the name of a table in schema
					 */
					if(t.substring(0, t.length()-1).equals(n)){
						found = true;
					}
				}
				
				if(!found)
					this.lstRelations.add(n);
			}
		   if(lstRelationInstances != null && lstRelationInstances.size()>0)
			   lstRelationInstances.removeAll(lstRedundantRelations);
		   if(lstRedundantRelations!=null && lstRedundantRelations.size()>0)
			   this.setNumberOfInnerJoins(numberOfInnerJoins-lstRedundantRelations.size());
		   else 
			   this.setNumberOfInnerJoins(numberOfInnerJoins);
		}
		
				
		/*@ author mathew on May 7 2016
		 *  The following method, to be called after redundant (eliminatable)
		 *   relations are derived, to update projection conditions, so that
		 *   any column from a redundant relation occuring in the above 
		 *   conditions is replaced an equivalent column
		 */
		public void reviseProjectedColumns(){
			if(this.lstRedundantRelations==null || this.lstRedundantRelations.isEmpty())
				return;
			
			ArrayList<Node> tempProjectionList=new ArrayList<Node>();
			tempProjectionList.addAll(this.lstProjectedCols);
			for(Node n:this.lstProjectedCols){
				if(lstRedundantRelations.contains(n.getTableNameNo())){
					Node eqNode=getAlternateEquivalentColumnNode(n);
					if(eqNode!=null){
						tempProjectionList.remove(n);
						tempProjectionList.add(eqNode);
					}
				}
			}
			
			this.lstProjectedCols.clear();
			this.lstProjectedCols.addAll(tempProjectionList);
			
		}
		
		/*@ author mathew on May 7 2016
		 *  The following method, to be called after redundant (eliminatable)
		 *   relations are derived, to update grouping conditions, so that
		 *   any column from a redundant relation occuring in the above 
		 *   conditions is replaced an equivalent column
		 */
		public void reviseGroupByColumns(){
			if(this.lstRedundantRelations==null || this.lstRedundantRelations.isEmpty())
				return;
			
			ArrayList<Node> tempGroupByNodes=new ArrayList<Node>();
			tempGroupByNodes.addAll(lstGroupByNodes);
			for(Node n:this.lstGroupByNodes){
				if(lstRedundantRelations.contains(n.getTableNameNo())){
					Node eqNode=getAlternateEquivalentColumnNode(n);
					if(eqNode!=null){
						tempGroupByNodes.remove(n);
						tempGroupByNodes.add(eqNode);
					}
				}
			}
			
			lstGroupByNodes.clear();
			lstGroupByNodes.addAll(tempGroupByNodes);

			Boolean found = false;
		
			/*
			 * Remove duplicate nodes since revising group by nodes 
			 * can introduce two different nodes that represent the same 
			 * columns 
			 */
			do{
				found=false;
				for(Node n:lstGroupByNodes){
					for(Node m:lstGroupByNodes){
						if(n!=m){
							if(n.getTableNameNo().equals(m.getTableNameNo())&&n.getColumn().getColumnName()
									.equals(m.getColumn().getColumnName())){
								lstGroupByNodes.remove(m);
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
		
		/*@ author mathew on July 18 2016
		 *  The following method, to be called after redundant (eliminatable)
		 *   relations are derived, to update order by columns, so that
		 *   any column from a redundant relation occurring in the above 
		 *   conditions is replaced an equivalent column
		 */
		public void reviseOrderByColumns(){
			if(this.lstRedundantRelations==null || this.lstRedundantRelations.isEmpty())
				return;
			
			ArrayList<Node> tempOrderByNodes=new ArrayList<Node>();
			tempOrderByNodes.addAll(this.lstOrderByNodes);
			for(Node n:this.lstOrderByNodes){
				if(lstRedundantRelations.contains(n.getTableNameNo())){
					Node eqNode=getAlternateEquivalentColumnNode(n);
					if(eqNode!=null){
						tempOrderByNodes.remove(n);
						tempOrderByNodes.add(eqNode);
					}
				}
			}
			
			lstOrderByNodes.clear();
			lstOrderByNodes.addAll(tempOrderByNodes);
			
		}
		
		/*@ author mathew on May 7 2016
		 *  The following method, to be called after redundant (eliminatable)
		 *   relations are derived, to update selection conditions, so that
		 *   any column from a redundant relation occuring in the above 
		 *   conditions is replaced an equivalent column
		 */
		public void reviseSelectionConditions(){
			if(this.lstRedundantRelations==null || this.lstRedundantRelations.isEmpty())
				return;
			reviseBinaryConditions(this.lstSelectionConds);	
			reviseBinaryConditions(this.lstJoinConditions);
		}
		
		/*@ author mathew on May 7 2016
		 *  The following method, to be called after redundant (eliminatable)
		 *   relations are derived, to update having conditions, so that
		 *   any column from a redundant relation occuring in the above 
		 *   conditions is replaced an equivalent column
		 */
		public void reviseHavingConditions(){
			if(this.lstRedundantRelations==null || this.lstRedundantRelations.isEmpty())
				return;
			ArrayList<Node> havingConds=new ArrayList<Node>();
			for(Node n:this.lstHavingConditions)
				havingConds.add(n);
			
			reviseBinaryConditions(havingConds);		
			
			this.lstHavingConditions=new ArrayList<Node>();
			for(Node n:havingConds)
				lstHavingConditions.add(n);
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
					if(QueryData.isMemberOf(n.getLeft().getTableNameNo(), lstRedundantRelations)
						||QueryData.isMemberOf(n.getRight().getTableNameNo(),lstRedundantRelations)){
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
		
		/** @author mathew on 14 sep 2016 
		 * Is intended to be called just before computing partial marks,
		 *  Iterates through all selection conditions, if any of its member is a join condition,
		 *  then removes it from selection condition and adds it to join condition
		 */
		public void reAdjustJoins() {
			// TODO Auto-generated method stub
			ArrayList<Node> tempSelectionConds=new ArrayList<Node>();
			if(lstSelectionConds!=null){
				tempSelectionConds.addAll(lstSelectionConds);
				for(Node n:lstSelectionConds){
					if(n.getLeft().getType().equals(Node.getColRefType())&&n.getRight().getType().equals(Node.getColRefType())){
						if(!lstJoinConditions.contains(n))
							lstJoinConditions.add(n);
						tempSelectionConds.remove(n);
					}			
				}
				lstSelectionConds.clear();
				lstSelectionConds.addAll(tempSelectionConds);
			}
		}
		
		/*@ author mathew on May 7 2016
		 *  The following method creates/returns a map of key-value pairs, 
		 *  where keys are nodes that represent columns in the given query 
		 *  and the corresponding values are lists of nodes that represents
		 *  the columns that are equivalent with the key  
		 */

		public Map<Node, ArrayList<Node>> getNodeToEquivalentNodes(){
			Map<Node, ArrayList<Node> > nodeToEqNodes = new HashMap<Node, ArrayList<Node>>();
			if(lstEqClasses != null){
				/*
				 * iterate through each class in the set of classes in eqClasses
				 */			
				for(ArrayList<Node> t : lstEqClasses){
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
								if(!lstRedundantRelations.contains(relationNameNoNew)){
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
							if(!lstRedundantRelations.contains(relationNameNoNew)){
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
				if(QueryData.isMemberOf(leftNode.getTableNameNo(),lstRedundantRelations)){
					leftNodeNew=getAlternateEquivalentColumnNode(leftNode);
					if(leftNodeNew==null)
						return null;

				}
			}
			n.setLeft(leftNodeNew);
			
			Node rightNode=n.getRight();
			Node rightNodeNew=n.getRight();
			if(rightNode!=null&& !rightNode.getNodeType().equals(Node.getValType())){
				if(QueryData.isMemberOf(rightNode.getTableNameNo(),lstRedundantRelations)){
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
		
		/* @author mathew on 14 Sep 2016
		 * Adds left and right node as members of the same equivalence class
		 * 
		 */
		public void addToLstEquivalenceClasses(Node left, Node right) {
			// TODO Auto-generated method stub	
			boolean modified=false;
			for(ArrayList<Node> eqClass:this.lstEqClasses){
				if(eqClass.contains(left)&&!eqClass.contains(right)){
					eqClass.add(right);
					modified=true;
				}
				if(eqClass.contains(right)&&!eqClass.contains(left)){
					eqClass.add(left);
					modified=true;
				}
			}
			/* if left and right are not present in any of the existing equivalence
			 * class then create a new equivalence class with elements left and right
			 */
			if(!modified){
				ArrayList<Node> eqClass=new ArrayList<Node>();
				eqClass.add(left);
				eqClass.add(right);
				lstEqClasses.add(eqClass);
			}
		}

		/**
		 * modified by mathew 
		 * takes the possibly complex expression of nodes stored in qStruct.allConds, splits it into atomic conditions, 
		 * disjunct of atomic conditions, separates selection conditions, join conditions, is null conditions, subQuery conditions,
		 *  like conditions etc., stores each conjunct in a disjunct in list qStruct.conjuncts		 */ 
		 
		 /* Revamp allConds. It should now contain the distinct predicates not
		 * containing a AND (or OR but ORs not considered for the moment) TODO: Do
		 * something about the presence of ORs: Need to convert the predicate into
		 * CNF and then create datasets by nulling each pair Eg.: if R.a = S.b OR
		 * T.c = U.d is the predicate, then create datasets by killing each of the
		 * following: 1. R.a and Tc 2. R.a and U.d 3. S.b and T.c 4. S.b and U.d 
		 */

		public static void flattenAndSeparateAllConds(QueryStructure qParser) {
			if(qParser.allConds == null)
				return ;

			Vector<Node> allCondsDuplicate;
			allCondsDuplicate = (Vector<Node>) qParser.allConds.clone();

			qParser.allConds.removeAllElements();
			Vector<Vector<Node>> allDnfDuplicate;
			allDnfDuplicate =(Vector<Vector<Node>>) qParser.dnfCond.clone();

			qParser.dnfCond.removeAllElements();
			Node temp;
			for (int i = 0; i < allCondsDuplicate.size(); i++) {
				if(allCondsDuplicate.get(i) != null)
					qParser.allConds.addAll(parsing.GetNodeQueryStructure.flattenNode(qParser, allCondsDuplicate.get(i)));
			}
		
			for (int i = 0; i < allCondsDuplicate.size(); i++) {
				if(allCondsDuplicate.get(i) != null)
					qParser.dnfCond.addAll(parsing.GetNodeQueryStructure.flattenCNF(qParser, allCondsDuplicate.get(i)));
			}			


			for (int i=0;i< allCondsDuplicate.size() ; i++) {
				if(allCondsDuplicate.get(i)!=null){
				ORNode t = GetNodeQueryStructure.flattenOr(allCondsDuplicate.get(i));
					for(Node n: t.leafNodes){
						qParser.orNode.leafNodes.add(n);
					}
					
					for(ANDNode n: t.andNodes){
						qParser.orNode.andNodes.add(n);
					}
					
//					qParser.orNode=GetNodeQueryStructure.flattenOr(allCondsDuplicate.get(i));
				}

			}

			ConjunctQueryStructure.createConjuncts(qParser);

			allCondsDuplicate.removeAllElements();
			allCondsDuplicate = (Vector<Node>) qParser.allConds.clone();

			allDnfDuplicate.removeAllElements();
			allDnfDuplicate = (Vector<Vector<Node>>) qParser.dnfCond.clone();

			for(Vector<Node> conjunct:allDnfDuplicate)
			{
				Vector<Node> subCond=new Vector<Node>();
				Vector<Node> temp1 = new Vector<Node>();
				temp1=(Vector<Node>) conjunct.clone();
				for(Node n:conjunct)
				{
					String type=n.getType();
					/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
					 * from the If condition below removed by mathew on 29 June 2016
					 * corresponding All node type and Any node type expressions added
					 */

					if(type.equalsIgnoreCase(Node.getAllNodeType()) || type.equalsIgnoreCase(Node.getAnyNodeType())
							|| type.equalsIgnoreCase(Node.getInNodeType()) ||
							type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
							||type.equalsIgnoreCase(Node.getNotInNodeType())//added by mathew on 17 oct 2016
							||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
						if(n.getSubQueryConds() != null){
						subCond.addAll(n.getSubQueryConds());
						}
						else{
							subCond.add(n);
						}
						temp1.remove(n);
					}
				}
				qParser.dnfCond.remove(conjunct);
				if(!temp1.isEmpty())
				{
					qParser.dnfCond.add(temp1);
				}
				if(!subCond.isEmpty())
				{
					qParser.allDnfSubQuery.add(subCond);
				}
			}

			for(Node n:allCondsDuplicate){
				String type=n.getType();
				/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
				 * from the If condition below removed by mathew on 29 June 2016
				 * corresponding All node type and Any node type expressions added
				 */
				if((type.equalsIgnoreCase(Node.getAllNodeType())
						||type.equalsIgnoreCase(Node.getAnyNodeType())
						|| type.equalsIgnoreCase(Node.getInNodeType()) ||
						type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
						||type.equalsIgnoreCase(Node.getNotInNodeType())
						||type.equalsIgnoreCase(Node.getNotExistsNodeType()))){
					qParser.allSubQueryConds.add(n);
					qParser.allConds.remove(n);
				}
			}

			for(Vector<Node> conjunct:allDnfDuplicate)
			{
				Vector<Node> subCond=new Vector<Node>();
				Vector<Node> temp1 = new Vector<Node>();
				temp1=(Vector<Node>) conjunct.clone();
				for(Node n:conjunct)
				{
					if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
							&& n.getOperator().equalsIgnoreCase("=")) {
						if (n.getLeft()!=null && n.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
								&& n.getRight()!=null&& n.getRight().getType().equalsIgnoreCase(
										Node.getColRefType())) {
							subCond.add(n);
							temp1.remove(n);
						}
					}

				}
				qParser.dnfCond.remove(conjunct);
				if(!temp1.isEmpty())
				{
					qParser.dnfCond.add(temp1);
				}
				if(!subCond.isEmpty())
				{
					qParser.dnfJoinCond.add(subCond);
				}
			}
			

			// Now separate Join Conds for EC And Selection Conds and Non Equi join
			// conds
			for (int i = 0; i < allCondsDuplicate.size(); i++) {
				temp = allCondsDuplicate.get(i);

				ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());

				boolean isJoinNodeForEC = GetNodeQueryStructure.getJoinNodesForEC(con, temp);
				// Remove that object from allConds. Because that will now be a part
				// of some or the other equivalence class and be handeled
				if (isJoinNodeForEC) {
					isJoinNodeForEC = false;
					qParser.joinConds.add(temp);//added by mathew on 17 oct 2016
					qParser.allConds.remove(temp);
				}			
			}
			
			// added by mathew on 18 oct 2016
			// Now separate Non-Equi/Outer join conds
			for (int i = 0; i < allCondsDuplicate.size(); i++) {
				temp = allCondsDuplicate.get(i);

				ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());

				boolean isJoinNodeAllOther = parsing.GetNodeQueryStructure.getJoinNodesAllOther(con, temp);
				// Remove that object from allConds. Because that will now be a part
				// of some or the other equivalence class and be handeled
				if (isJoinNodeAllOther) {
					isJoinNodeAllOther = false;
					qParser.joinConds.add(temp);//added by mathew on 17 oct 2016
					qParser.allConds.remove(temp);
				}			
			}

			for(Vector<Node> conjunct:allDnfDuplicate)
			{
				Vector<Node> subCond=new Vector<Node>();
				Vector<Node> temp1 = new Vector<Node>();
				temp1=(Vector<Node>) conjunct.clone();
				for(Node n:conjunct)
				{
					if (n.containsConstant()) {
						subCond.add(n);
						temp1.remove(n);

					}
				}
				qParser.dnfCond.remove(conjunct);
				if(!temp1.isEmpty())
				{
					qParser.dnfCond.add(temp1);
				}
				if(!subCond.isEmpty())
				{
					qParser.allDnfSelCond.add(subCond);
				}
			}

			// Now separate Selection conds into the vector Selection Conds
			for (int i = 0; i < allCondsDuplicate.size(); i++) {
				temp = allCondsDuplicate.get(i);

				ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());

				boolean isSelection = GetNodeQueryStructure.getSelectionNode(con,temp);
				if (isSelection) {
					isSelection = false;
					// remove it from allConds as it is added to selection
					// conditions
					qParser.allConds.remove(temp);
				}
			}

			for(Vector<Node> conjunct:allDnfDuplicate)
			{
				//Vector<Node> 
				Vector<Node> subCond=new Vector<Node>();
				Vector<Node> temp1 = new Vector<Node>();
				temp1=(Vector<Node>) conjunct.clone();
				for(Node n:conjunct)
				{
					if(n.getType().equalsIgnoreCase(Node.getLikeNodeType())){//CharConstantNode
						subCond.add(n);
						temp1.remove(n);

					}    
				}
				qParser.dnfCond.remove(conjunct);
				if(!temp1.isEmpty())
				{
					qParser.dnfCond.add(temp1);
				}
				if(!subCond.isEmpty())
				{
					qParser.dnfLikeConds.add(subCond);
				}
			}

			//Added by Bikash----------------------------------------------------
			//For the like operator
			for(int i=0;i<allCondsDuplicate.size();i++){
				temp = allCondsDuplicate.get(i);

				ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());
				boolean isLikeType = GetNodeQueryStructure.getLikeNode(con,temp);
				if(isLikeType){
					isLikeType = false;
					//remove it from allConds as it is added to like conditions
					qParser.allConds.remove(temp);
				}
			}

			//***************************************************************************/
			for(Vector<Node> conjunct:allDnfDuplicate)
			{
				//Vector<Node> 
				Vector<Node> subCond=new Vector<Node>();
				Vector<Node> temp1 = new Vector<Node>();
				temp1=(Vector<Node>) conjunct.clone();
				for(Node n:conjunct)
				{
					if(n.getType().equals(Node.getIsNullNodeType())){
						subCond.add(n);
						temp1.remove(n);

					}    
				}
				qParser.dnfCond.remove(conjunct);
				if(!temp1.isEmpty())
				{
					qParser.dnfCond.add(temp1);
				}
				if(!subCond.isEmpty())
				{
					qParser.dnfIsNullConds.add(subCond);
				}
			}

			for(Node n:allCondsDuplicate){
				if(n.getType().equals(Node.getIsNullNodeType())){
					qParser.isNullConds.add(n);
					qParser.allConds.remove(n);
				}
			}
			//Now get the lhsRhs conditions in a separate vector, lhsRhsConds
			//This has to be added in each and every killing procedure as positive cond
			for(int i=0;i<qParser.allSubQueryConds.size();i++){
				Node n = qParser.allSubQueryConds.get(i);
				if(n.getLhsRhs()==null || n.getType().equalsIgnoreCase(Node.getExistsNodeType()) || n.getType().equalsIgnoreCase(Node.getNotExistsNodeType()))	
					continue;
				Vector<Node> lhsRhs = parsing.GetNodeQueryStructure.flattenNode(qParser, n.getLhsRhs());
				qParser.lhsRhsConds.addAll(lhsRhs);				//Why is this variable required???
			}

			for(Node n: qParser.allSubQueryConds){
				if(n.getSubQueryConds()!=null){
					Vector<Node> subQConds=(Vector<Node>)n.getSubQueryConds().clone();
					n.getSubQueryConds().removeAllElements();
					for(Node subQ:subQConds){
						n.getSubQueryConds().addAll(parsing.GetNodeQueryStructure.flattenNode(qParser,subQ));
						//n.setSubQueryConds(flattenNode(subQ));
					}
				}
			}

		}
		/**
		 * Key Value:  
		 * 1- projected cols    
		 * 2- where clause    
		 * 3- having    
		 * 4- order-by    
		 * 
		 * @return the caseConditionMap
		 */
		public HashMap<Integer,CaseExpression> getCaseConditionMap() {
			return caseConditionMap;
		}

		/**
		 * @param caseConditionMap the caseConditionMap to set
		 */
		public void setCaseConditionMap(HashMap<Integer,CaseExpression> caseConditionMap) {
			this.caseConditionMap = caseConditionMap;
		}

		public Node getQueryType() {
			return queryType;
		}

		public void setQueryType(Node queryType) {
			this.queryType = queryType;
		}

		

	}
