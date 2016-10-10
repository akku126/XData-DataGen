package parsing;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.JSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.Statement;
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
import org.apache.derby.impl.sql.compile.CursorNode;
import org.apache.derby.impl.sql.compile.DeleteNode;
import org.apache.derby.impl.sql.compile.InsertNode;
import org.apache.derby.impl.sql.compile.IntersectOrExceptNode;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SQLParser;
import org.apache.derby.impl.sql.compile.SelectNode;
import org.apache.derby.impl.sql.compile.StatementNode;
import org.apache.derby.impl.sql.compile.UnionNode;
import org.apache.derby.impl.sql.compile.UpdateNode;
import parsing.AggregateFunction;
import parsing.Conjunct;
import parsing.ForeignKey;
import parsing.FromListElement;
import parsing.JoinClauseInfo;
import parsing.JoinTreeNode;
import parsing.Node;
import parsing.Query;
import parsing.TreeNode;
import parsing.Utility;
import util.Graph;
import util.TableMap;
import parsing.Table;

class QueryAliasMap {
	/** Data Structure to avoid aliasing in queries.*/

	String queryId;
	String queryIdOrTableName;
	String aliasOfSubqueryOrTable;

	QueryAliasMap() {
		queryId = null;
		queryIdOrTableName = null;
		aliasOfSubqueryOrTable = null;
	}
}

public class QueryParser implements Serializable{

	private static Logger logger = Logger.getLogger(QueryParser.class.getName());
	private static final long serialVersionUID = 8049915037697741933L;
	public ORNode orNode;
	private Query query;
	private TableMap tableMap;

	private Vector<TreeNode> inOrderList;
	private Vector<JoinClauseInfo> joinClauseInfoVector;

	private Vector<JoinClauseInfo> selectionClauseVector;
	private Vector<JoinClauseInfo> foreignKeyVector;


	private Vector<ForeignKey> foreignKeyVectorModified;

	private Vector<JoinClauseInfo> foreignKeyVectorOriginal;
	private Vector<Vector> equivalenceClassVector;
	// currentAliasTables holds the Aliasname and the tablename for the current
	// level of the query.
	// If table is a subquery, it holds alias and "SUBQUERY"
	private HashMap<String, String> currentAliasTables;

	
	
	public Vector<Node> subQJC;

	// Data Structure to avoid aliasing in queries.
	private Vector<QueryAliasMap> qam;


	private String currentQueryId;
	private int tableNo; // required for maintaining the repeated occurences of
	// tables.
	// Data Structure to kill IN Clause Mutants and perhaps other subquery
	// mutants also
	private HashMap<Integer, Vector<JoinClauseInfo>> inConds;
	public Vector<Node> allConds;
	private Vector<Node> selectionConds;
	private Vector<Node> likeConds;
	Vector<Node> isNullConds;
	private Vector<Node> inClauseConds;
	Vector<Vector<Node>> equivalenceClasses;
	Vector<Node> joinConds;
	private Vector<Node> foreignKeys;
	Vector<Node> projectedCols;

	//added by mathew on 2 september 2016
	public QueryParser parentQueryParser;
	public Vector<FromListElement> fromListElements;
	
	//Map of case conditions: Key value holds the part of query in which the case conditions occur.
	//Value holds the case condition vector. Last element of vector contains the else condition
	private HashMap<Integer,Vector<CaseCondition>> caseConditionMap;
	
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
	public boolean isDeleteNode;
	public boolean isUpdateNode;
	public boolean isInSubQ;
	public Vector<Node> updateColumn;

	public QueryParser leftQuery;
	public QueryParser rightQuery; 

	// aggregation
	Vector<AggregateFunction> aggFunc;
	Vector<Node> groupByNodes;
	Node havingClause;
	JoinTreeNode root;
	HashMap<String, Node> constraintsWithParameters;
	
	//added by mathew on 18 June 2016 for processing order by clause
	//order by 
	Vector<Node> orderByNodes;
	
	// Subquery
	private Vector<Node> allCondsExceptSubQuery;
	Vector<Node> allSubQueryConds;
	private Vector<Node> allAnyConds;
	Vector<Node> lhsRhsConds;
	int paramCount;
	// private Node whereClausePred;

	Vector<Conjunct> conjuncts;
	Vector<Vector<Node>> allDnfSelCond;
	Vector<Vector<Node>> dnfLikeConds;
	Vector<Vector<Node>> dnfIsNullConds;
	Vector<Vector<Node>> allDnfSubQuery;
	Vector<Vector<Node>> dnfJoinCond;
	
	private boolean isDistinct;

	private Vector<Node> joinConditionList;

	//To store from clause subqueries
	private Vector<QueryParser> FromClauseSubqueries;//To store from clause subqueries
	private Vector<QueryParser> WhereClauseSubqueries;//To store where clause sub queries
	private HashMap<String, Vector<Node>> aliasedToOriginal;//To store the mapping from aliased columns names to original names

	private HashMap<String, Integer> subQueryNames;//TO store names for the sub queries

	private HashMap<String, Integer[]> tableNames;//It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]

	//Representation in DNF
	Vector<Vector<Node>> dnfCond;

	static String[] cvcRelationalOperators = { "DUMMY", "=", "/=", ">",
		">=", "<", "<=", "&&"}; // IsNull and IsNotNull not supported currently
								/*&& added by mathew on 1st Aug 2016.*/

	public FromListElement queryAliases;
	
	public RelationHierarchyNode topLevelRelation;
	
	@Override
	public String toString(){
		String retString=" Projection list \n";
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
		if(this.havingClause!=null)
			retString+=" "+this.havingClause;
		retString+="\n Order By \n";
		for(Node n:this.orderByNodes)
			retString+=" "+n;
		if( this.getFromClauseSubqueries().size()>0){
		retString+="\n From Subqueries \n";
		for(QueryParser qp:this.getFromClauseSubqueries())
			retString+="          "+qp.toString();	
		}
		if( this.getWhereClauseSubqueries().size()>0){
			retString+="\n Where Subqueries  \n";
			for(QueryParser qp:this.getWhereClauseSubqueries())
				retString+="          "+qp.toString();	
		}

		return retString;
	}
	
	private static String fromListElementsToString(Vector<FromListElement> visitedFromListElements) {
		String retString="";
		for(FromListElement fle:visitedFromListElements){
			if(fle!=null && (fle.getTableName()!=null||fle.getTableNameNo()!=null))
				retString+="\n "+fle.toString();
			else if(fle!=null && fle.getSubQueryParser()!=null){
				retString+="\n "+fle.toString();
				retString+=fromListElementsToString(fle.getSubQueryParser().getFromListElements());
			}
			else if(fle!=null && fle.getTabs()!=null && !fle.getTabs().isEmpty()){
				retString+="\n "+fle.toString();
				retString+=fromListElementsToString(fle.getTabs());				
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


	public Vector<JoinClauseInfo> getJoinClauseInfoVector() {
		return joinClauseInfoVector;
	}

	public void setJoinClauseInfoVector(Vector<JoinClauseInfo> joinClauseInfoVector) {
		this.joinClauseInfoVector = joinClauseInfoVector;
	}


	public Vector<QueryAliasMap> getQam() {
		return qam;
	}

	public void setQam(Vector<QueryAliasMap> qam) {
		this.qam = qam;
	}

	public void setQueryAliases(FromListElement queryAliases) {
		this.queryAliases = queryAliases;
	}



	public void setFromClauseSubqueries(Vector<QueryParser> fromClauseSubqueries) {
		FromClauseSubqueries = fromClauseSubqueries;
	}


	public HashMap<String, Vector<Node>> getAliasedToOriginal() {
		return aliasedToOriginal;
	}

	public void setAliasedToOriginal(HashMap<String, Vector<Node>> aliasedToOriginal) {
		this.aliasedToOriginal = aliasedToOriginal;
	}


	public HashMap<String, Integer> getSubQueryNames() {
		return subQueryNames;
	}

	public void setSubQueryNames(HashMap<String, Integer> subQueryNames) {
		this.subQueryNames = subQueryNames;
	}


	public HashMap<String, Integer[]> getTableNames() {
		return tableNames;
	}

	public void setTableNames(HashMap<String, Integer[]> tableNames) {
		this.tableNames = tableNames;
	}



	public Vector<Conjunct> getConjuncts() {
		return conjuncts;
	}

	public void setConjuncts(Vector<Conjunct> conjuncts) {
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




	// @junaid modified
	public FromListElement getQueryAliases() {
		return queryAliases;
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

	public void setHavingClause(Node havingClause) {
		this.havingClause = havingClause;
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
	public Vector<FromListElement> getFromListElements(){
		return this.fromListElements;
	}
	
	public void setFromListElements(Vector<FromListElement> FLEs){
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


	public Vector<QueryParser> getFromClauseSubqueries(){
		return this.FromClauseSubqueries;
	}

	public Vector<QueryParser> getWhereClauseSubqueries(){
		return this.WhereClauseSubqueries;
	}

	public QueryParser(TableMap tableMap) {
		this.tableMap = tableMap;
		this.inOrderList = new Vector<TreeNode>();
		this.joinClauseInfoVector = new Vector<JoinClauseInfo>();
		this.selectionClauseVector = new Vector<JoinClauseInfo>();
		this.foreignKeyVector = new Vector<JoinClauseInfo>();
		orNode = new ORNode();
		this.conjuncts = new Vector<Conjunct>();
		dnfCond = new Vector<Vector<Node>>();
		dnfJoinCond = new Vector<Vector<Node>>();
		dnfLikeConds = new Vector<Vector<Node>>();
		allDnfSelCond = new Vector<Vector<Node>>();
		dnfIsNullConds = new Vector<Vector<Node>>();
		allDnfSubQuery =new Vector<Vector<Node>>();
		//setCaseConditions(new Vector<CaseCondition>());
		caseConditionMap = new HashMap<Integer,Vector<CaseCondition>>();
		//orNode = new ORNode();
		this.foreignKeyVectorModified = new Vector<ForeignKey>();

		this.equivalenceClassVector = new Vector<Vector>();
		this.currentAliasTables = new HashMap<String, String>();
		qam = new Vector<QueryAliasMap>();
		this.currentQueryId = "Q";
		inConds = new HashMap<Integer, Vector<JoinClauseInfo>>();
		allConds = new Vector<Node>();
		equivalenceClasses = new Vector<Vector<Node>>();

		aliasedToOriginal = new HashMap<String, Vector<Node>>();
		subQueryNames = new HashMap<String, Integer>();
		tableNames = new HashMap<String, Integer[]>();

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
		havingClause = new Node();
		allSubQueryConds = new Vector<Node>();
		allCondsExceptSubQuery = new Vector<Node>();
		subQJC = new Vector<Node>();
		tableNo = 0;

		joinConditionList = new Vector<Node>();
		root = null;
		constraintsWithParameters = new HashMap<String, Node>();
		lhsRhsConds = new Vector<Node>();

		updateColumn=new Vector<Node>();
		this.FromClauseSubqueries = new Vector<QueryParser>();
		this.WhereClauseSubqueries = new Vector<QueryParser>();
		//added by mathew on oct 1st 2016
		this.fromListElements=new Vector<FromListElement>();
	}

	public String getModifiedQuery(ResultSetNode rsNode, boolean debug,
			QueryParser qp) throws Exception {
		HashMap<String, String> currentAT = new HashMap<String, String>();
		// this.currentAliasTables = new HashMap<String, String>();
		String fromClauseString = Utility.getFromClauseString(rsNode, qp,
				currentAT);
		// currentAT.putAll(this.getCurrentAliasTables());
		String whereCluase = Utility.getWhereClauseString(rsNode, true);
		String groupByClause = Utility
				.getGroupByClauseAttributes(((SelectNode) rsNode)
						.getGroupByList());

		/*
		 * Currently not handling Group by cluase if (getGroupByColumns() !=
		 * null && getGroupByColumns().size()>0) { remainingQuery =
		 * remainingQuery.substring(0,remainingQuery.indexOf("GROUP BY")); }
		 */

		String selectClause = "SELECT ";
		// boolean b = false;

		for (String aliasName : currentAT.keySet()) {
			if (currentAT.get(aliasName).equalsIgnoreCase("SUBQUERY")) {
				selectClause = selectClause + aliasName + ".*, ";
			} else {
				String tableName = query.getTableOfAlias(aliasName).get(0);
				selectClause = selectClause + aliasName + ".CTID AS "
						+ tableName + "CTID, ";
				selectClause = selectClause + aliasName + ".*, ";
			}
		}
		/*
		 * for(String tableName : query.getFromTables().keySet()){ Table table =
		 * query.getFromTables().get(tableName); tableName =
		 * tableName.toUpperCase(); if(table.getAliasName()!=null) selectClause
		 * += table.getAliasName(); else selectClause += tableName;
		 * 
		 * selectClause += ".CTID AS "; selectClause += tableName+"CTID, ";
		 * 
		 * if(table.getAliasName()!=null) selectClause +=
		 * table.getAliasName()+".*, "; else selectClause += tableName+".*, "; }
		 */
		selectClause = selectClause.substring(0, selectClause.length() - 2)
				.toString();

		// This code checks if the projected columns have a foreign key
		// relationship with any other table that is not projected.
		// If so it adds the table to the projection list.
		// Info added for JoinClause
		Graph<Table, JoinClauseInfo> joinGraph = query.getJoinGraph();
		// Graph<Table,ForeignKey> foreignKeyGraph =
		// tableMap.getForeignKeyGraph();
		for (String tableName : query.getFromTables().keySet()) {
			Table table = query.getFromTables().get(tableName);
			if (table.hasForeignKey()) {
				for (String fKeyName : table.getForeignKeys().keySet()) {
					ForeignKey fKey = table.getForeignKey(fKeyName);
					Vector<Column> fKeyColumns = fKey.getFKeyColumns();
					// Vector<Column> refKeyColumns =
					// fKey.getReferenceKeyColumns();
					boolean joinFound = false;
					if (joinGraph.getNeighbours(table) != null) {
						for (Table joinTable : joinGraph.getNeighbours(table)
								.keySet()) {
							for (JoinClauseInfo joinClauseInfo : joinGraph
									.getEdges(table, joinTable)) {
								if (joinClauseInfo.contains(fKey
										.getFKTablename())
										&& joinClauseInfo.contains(fKey
												.getReferenceTable()
												.getTableName())) {
									// if(joinClause.contains(fKeyColumns) &&
									// joinClause.contains(refKeyColumns)){
									joinFound = true;
								}
							}
						}
					}
					if (!joinFound) {
						Table referenceTable = fKey.getReferenceTable();
						if (query.getBaseRelation().containsKey(
								referenceTable.getTableName())) {
							continue;
						}
						if (!selectClause.contains(referenceTable.getTableName())) {
							fromClauseString += JoinClauseInfo.leftOuterJoin+ " " + referenceTable.getTableName()+ " ON (";
							for (Column fKeyColumn : fKeyColumns) {
								if (table.getAliasName() != null)
									fromClauseString += table.getAliasName()+ "." + fKeyColumn.getColumnName()+ "=";
								else
									fromClauseString += table.getTableName()+ "." + fKeyColumn.getColumnName()+ "=";

								fromClauseString += fKeyColumn
										.getReferenceTableName()
										+ "."
										+ fKeyColumn.getReferenceColumn()
										.getColumnName();
								fromClauseString += " AND ";
							}
							fromClauseString = fromClauseString.substring(0,
									fromClauseString.length() - 5);
							fromClauseString += ")";

							if (!query.getFromTables().containsKey(referenceTable)) {
								selectClause += ", "+ referenceTable.getTableName()+ ".CTID AS "+ referenceTable.getTableName()	+ "CTID";
								selectClause += ", "+ referenceTable.getTableName() + ".* ";
							} 
						}
					}
				}
			}// till here: add FK tables to projection list
		}
  
		query.setQueryForGroupBy("SELECT DISTINCT " + groupByClause
				+ fromClauseString + whereCluase);
		// TODO Why is group by clause not added? 
		String modifiedQueryString = selectClause + fromClauseString
				+ whereCluase;// +" order by random()";
		this.currentAliasTables = new HashMap<String, String>();
		if (debug)
			logger.log(Level.INFO,"\nModified Query : " + modifiedQueryString);
		return modifiedQueryString;  
	} 
          
	public void parseQuery(String queryId, String queryString) throws Exception {
		try{
			queryString=queryString.trim().replaceAll("\n+", " ");
			queryString=queryString.trim().replaceAll(" +", " ");
			
			//JSQL PArser does not accept NATURAL LEFT OUTER . So Replace for parsing - To be changed in parser
			queryString = queryString.replace( "NATURAL LEFT OUTER","NATURAL");
			queryString = queryString.replace("NATURAL RIGHT OUTER","NATURAL");
			
			//establishing eqivalence of LEFT (resp. RIGHT, resp. FULL) JOIN and
			// LEFT (resp. RIGHT, resp. FULL) OUTER JOIN, added by mathew on 7/FEB/2016
			queryString = queryString.replace( "LEFT JOIN","LEFT OUTER JOIN");
			queryString = queryString.replace("RIGHT JOIN","RIGHT OUTER JOIN");
			queryString = queryString.replace("FULL JOIN","FULL OUTER JOIN");

			parseQueryJSQL(queryId, queryString, true);
		}catch(ParseException ex){
			
			logger.log(Level.SEVERE," Function parseQuery : "+ex.getMessage(),ex);
			throw new Exception("QueryParser.java: parseQuery() : JSQLParser Error : Query Parsing failed for the following query : \n"+queryString+" \n. \n Please check the logs for details."); 
		} catch(Exception e){
			logger.log(Level.SEVERE," Function parseQuery : "+e.getMessage(),e);			
			throw new Exception("QueryParser.java: parseQuery() : JSQLParser Error : Query Parsing failed for the following query : \n"+queryString+" \n. \n Please check the logs for details.");  
		}
	}
    
	public void parseQueryJSQL(String queryId, String queryString, boolean debug)
			throws Exception {
		logger.info("beginning to parse query");
		try{
			if(this.query==null)
				this.query = new Query(queryId, queryString);
			else
				this.query.setQueryString(queryString);
			
			CCJSqlParserManager pm = new CCJSqlParserManager();
			Statement stmt = pm.parse(new StringReader(queryString));
			//SQLParser sqlParser = new SQLParser();
			PlainSelect plainSelect = null;
			
			if (stmt instanceof Select){
				//Check if it is plain select statement without with clause
				if(((Select) stmt).getSelectBody() instanceof PlainSelect &&
						((Select)stmt).getWithItemsList() == null){						
					 plainSelect = (PlainSelect)((Select) stmt).getSelectBody();
					ProcessResultSetNode.processResultSetNodeJSQL(plainSelect, debug, this);
//					ProcessSelectClause.ProcessSelect(plainSelect, debug, this);
				}
				
				//Check if query contains WithItem list - then Query is of the form  WITH S AS ()
				if(((Select) stmt).getSelectBody() instanceof PlainSelect 
						&& ((Select)stmt).getWithItemsList() != null){
															
					stmt=transformQueryForWithAs((Select)stmt);
					//PlainSelect selectClause =(PlainSelect) ((Select) stmt).getSelectBody();
					
					String alteredWithQuery=((Select)stmt).getSelectBody().toString();
					logger.info("transformed query after substitution of Witt aliases\n"+ alteredWithQuery);
	
					ProcessResultSetNode.processResultSetNodeJSQL((PlainSelect)((Select) stmt).getSelectBody(), debug, this);
					//ProcessSelectClause.ProcessSelect((PlainSelect)((Select) stmt).getSelectBody(), debug, this);
				}
				
				//If it is instance of SetOperationList - UNION,EXCEPT OR INTERSECT
				else if(((Select) stmt).getSelectBody() instanceof SetOperationList){
					
					stmt=transformQueryForWithAs((Select)stmt);
					String alteredWithQuery=((Select)stmt).getSelectBody().toString();
					logger.info("transformed query after substitution of with aliases\n"+ alteredWithQuery);
					
					SetOperationList setOpList = (SetOperationList)((Select) stmt).getSelectBody();
								
						//Test in different scenarios - joins in SET  Op and test
						//Get the select list to check it has select statement or nested SET operation
						parseQueriesForSetOp(setOpList,debug); 
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
				logger.info("found as from item");
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
			logger.info("processing subselect");
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
				logger.info("found as from item");
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
			logger.info("processing subselect");
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
				logger.info("found as from item");
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
			logger.info("processing subselect");
			SubSelect tarSubSelect=(SubSelect) tarFromItem;
			transformSubSelectForWithAs(srcWithItem, tarSubSelect);
		}
		// if fromitem is a subjoin, then call the corresponding method that handles it
		else if(tarFromItem instanceof SubJoin){
			logger.info("processing subjoin");
			SubJoin tarSubJoin=(SubJoin)tarFromItem;
			tarSubJoin=transformSubJoinForWithAs(srcWithItem,tarSubJoin);
		}
	
		// WITH AS names can be in joins as well, call the corresponding method that handles it
		if(tarSelectClause.getJoins() != null ){
			logger.info("processing joins");
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
			Join jcl = (Join)joinList.get(k);	
			FromItem tarJoinFromItem=jcl.getRightItem();				
			//if the join item is a table, then call the corresponding method that handles it
			if(tarJoinFromItem instanceof net.sf.jsqlparser.schema.Table){
				net.sf.jsqlparser.schema.Table tarTable= (net.sf.jsqlparser.schema.Table)tarJoinFromItem;
				if(tarTable.getName().equalsIgnoreCase(srcWithItem.getName())){
					logger.info("found with alias in from item of join");
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
				logger.info("processing subselect in join");
				SubSelect tarSubSelect=(SubSelect) tarJoinFromItem;
				PlainSelect tempSelect=(PlainSelect)tarSubSelect.getSelectBody();
				transformPlainSelectForWithAs(srcWithItem, tempSelect);
			}
			//if join item is a subjoin, then call the corresponding method that handles it
			else if(tarJoinFromItem instanceof SubJoin){
				logger.info("processing subjoin in join");
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
			logger.info("transforming exists in where clause");
			ExistsExpression existsExpression = (ExistsExpression)whereClause;
			transformWhereClauseForWithAs(srcWithItem,existsExpression.getRightExpression());
		}
		//if whereClause is a InExpression, then handle its left and right operands separately
		else if(whereClause instanceof InExpression){
			logger.info("transforming inExpression in where clause");
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
			logger.info("transforming all comparison in where clause");
			AllComparisonExpression ace = (AllComparisonExpression) whereClause;
			transformPlainSelectForWithAs(srcWithItem, (PlainSelect)(ace.getSubSelect().getSelectBody()));
		}
		//if whereClause is a Any comparision Expression, then recurisively call using its subselect as argument
		else if(whereClause instanceof AnyComparisonExpression){
			logger.info("transforming any comparison in where clause");
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
			logger.info("Empty select");
			return null;
		}
		
		List<WithItem> withItemsList=selectClause.getWithItemsList();
		if(withItemsList==null||withItemsList.isEmpty())
			return selectClause;
		
		for(int i=0;i<withItemsList.size();i++){
			// normalize column names in with items eg: With A(a) as (select name from ...)
			// is normalized to With A(a) as (select name as a from ...)
			WithItem srcWithItem=normalizeWithItem((WithItem)withItemsList.get(i));
			logger.info("normalized with item"+srcWithItem);
			// now translate the subsequent with items by substituting the definition 
			// of withItem under consideration in their select bodies
			for(int j=i+1;j<withItemsList.size();j++){
				WithItem tarWithItem=(WithItem)withItemsList.get(j);
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
		logger.info(" normalized with item "+withItem.getName()+" withItemBody: "+withItem.getSelectBody());
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
	public void parseQueriesForSetOp(SetOperationList setOpList, boolean debug) throws Exception {
		
		logger.info(" set operation List"+setOpList.toString());
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
				leftQuery = new QueryParser(this.tableMap);
				if(nxtElement instanceof PlainSelect){
					PlainSelect left= (PlainSelect)nxtElement;
					
					if(leftQuery.query==null)
						leftQuery.query= new Query("q2", left.toString());
					else
						leftQuery.query.setQueryString(left.toString());
					
					ProcessResultSetNode.processResultSetNodeJSQL(left, debug, leftQuery);
					//ProcessSelectClause.ProcessSelect(left, debug, leftQuery);
					this.projectedCols.addAll(leftQuery.projectedCols);
				}
				else if(nxtElement instanceof SetOperationList){
					leftQuery.parseQueryJSQL("q2",((SetOperationList)nxtElement).toString(),debug);
				}
			}if(selectList.size()==2&&selectListIt.hasNext()){
				Object nxtElement = selectListIt.next();

				rightQuery = new QueryParser(this.tableMap);
				if(nxtElement instanceof PlainSelect){
					PlainSelect right=(PlainSelect) nxtElement;
					if(rightQuery.query==null)
						rightQuery.query=new Query("q3",right.toString());
					else
						rightQuery.query.setQueryString(right.toString());
					
					ProcessResultSetNode.processResultSetNodeJSQL(right, debug, rightQuery);			
					//ProcessSelectClause.ProcessSelect(right, debug, rightQuery);
					if(projectedCols.isEmpty())
						this.projectedCols.addAll(rightQuery.projectedCols);
					}
				else if(nxtElement instanceof SetOperationList){
					rightQuery.parseQueryJSQL("q3",((SetOperationList)nxtElement).toString(),debug);
				}
			}
			/*The following else added by mathew on  22 August 2016
			 * To handle the case when selectList (has size>=3) is a chain of set operation list of more 
			 * than two elements in which case the 
			 * set operations from 2nd to last are added to tempSetOpList. Note that the first operation was already 
			 * processed by the preceding first if statement. Recursive call with argument as tempSetOpList
			 * */
			else{
				rightQuery = new QueryParser(this.tableMap);
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
				rightQuery.parseQueryJSQL("q3",tempSetOpList.toString(),debug);
			}
			
		}
	}
		
	/**
	 * This method parses the query using the DERBY parser
	 * @param queryId
	 * @param queryString
	 * @param debug
	 * @throws Exception
	 */
	@Deprecated
	public void parseQuery(String queryId, String queryString, boolean debug)
			throws Exception {
		logger.log(Level.WARNING,"ParseQuery : Call to deprecated Method");
		queryString=queryString.trim().replaceAll("\n+", " ");
		queryString=queryString.trim().replaceAll(" +", " ");		
		if(queryString.toLowerCase().contains("year")){
			queryString=queryString.replaceAll("year","year1");
			queryString=queryString.replaceAll("Year","year1");
			queryString=queryString.replaceAll("YEAR","year1");
		}
		queryString=preParseQuery(queryId,queryString);		
		this.query = new Query(queryId, queryString);
		SQLParser sqlParser = new SQLParser();
		
		query.setQueryString(queryString);


		BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out));
		//queryString = generateCleanQry(queryString, "", "");		
		
		StatementNode s=sqlParser.Statement(queryString, null);
		ResultSetNode rsNode ;

		if(s instanceof InsertNode){
			rsNode = ((InsertNode)s).getResultSetNode();

		}else if(s instanceof DeleteNode){
			rsNode = ((DeleteNode)s).getResultSetNode();
			isDeleteNode=true;
			havingClause=null;
		}else if(s instanceof UpdateNode){
			rsNode = ((UpdateNode)s).getResultSetNode();
			isUpdateNode=true;
			havingClause=null;			
		}else{
			rsNode = ((CursorNode)s).getResultSetNode();
		}	

		if( rsNode instanceof UnionNode){

			UnionNode unionNode=(UnionNode)rsNode;
			setOperator="UNION";
			leftQuery = new QueryParser(this.tableMap);
			String left=queryString.substring(0,queryString.toLowerCase().indexOf("union"));
			leftQuery.parseQuery("q2", left);

			rightQuery = new QueryParser(this.tableMap);
			String right=queryString.substring(queryString.toLowerCase().indexOf("union")+5);
			rightQuery.parseQuery("q3", right);

		}else if (rsNode instanceof IntersectOrExceptNode){

			IntersectOrExceptNode node =(IntersectOrExceptNode)rsNode;
			setOperator="INTERSECTEXCEPT";

			leftQuery = new QueryParser(this.tableMap);
			String left=queryString.substring(0,queryString.toLowerCase().indexOf("except"));
			leftQuery.parseQuery("q2", left);

			rightQuery = new QueryParser(this.tableMap);
			String right=queryString.substring(queryString.toLowerCase().indexOf("except")+6);
			rightQuery.parseQuery("q3", right);

		}else{
			ProcessResultSetNode.processResultSetNode(rsNode, debug, this);
		}

	}

	/**
	 * This method was used for tokenizing WITH AS queries 
	 * 
	 * @param queryId
	 * @param queryString
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public String preParseQuery(String queryId,String queryString) throws Exception{
		logger.log(Level.WARNING,"preParseQuery : Call to deprecated Method");
		StringTokenizer st=new StringTokenizer(queryString.trim());
		String token=st.nextToken();

		
		if(!token.equalsIgnoreCase("with")){
			return queryString;
		}
		int numberOfAlias=0;
		String aliasname[]=new String[10];
		String subquery[]=new String[10];

		while(true){

			String columnname="";
			aliasname[numberOfAlias]=st.nextToken();

			if(aliasname[numberOfAlias].contains("(")){

				columnname=aliasname[numberOfAlias].substring(aliasname[numberOfAlias].indexOf("("));
				columnname=columnname.substring(1,columnname.length()-1);	//remove ( & )

				aliasname[numberOfAlias]=aliasname[numberOfAlias].substring(0,aliasname[numberOfAlias].indexOf("("));           	

			}
			token=st.nextToken();   	// should be AS key word or should start with (

			if(token.startsWith("(")){
				while(!token.contains(")")){
					columnname+=token;
					token=st.nextToken();
				}
				columnname+=token;            	
				token=st.nextToken();	// should be AS key word
			}

			if(!token.equalsIgnoreCase("as")){            	
				Exception e= new Exception("Error while preparsing the with clause AS expected");
				throw e;
			}
			
			subquery[numberOfAlias]="(";
			queryString=queryString.substring(queryString.indexOf("(")+1);
			if(columnname.length()!=0){
				queryString=queryString.substring(queryString.indexOf("(")+1);
			}

			int count=1,i=0;
			while(count!=0){
				if(queryString.charAt(i)=='('){
					count++;
				}else if(queryString.charAt(i)==')'){
					count--;
				}
				subquery[numberOfAlias]+=queryString.charAt(i);
				i++;
			}
			queryString=queryString.substring(i).trim();

			if(columnname.length()!=0){
				columnname=columnname.substring(1,columnname.length()-1);
				String columnlist[]=columnname.split(",");
				int ctr=0;
				String temp=subquery[numberOfAlias];
				subquery[numberOfAlias]="";            	
				String tok=temp.substring(0,temp.indexOf("from"));
				for(int j=0;j<tok.length();j++){
					if(tok.charAt(j)==','){
						subquery[numberOfAlias]+=" as "+columnlist[ctr++]+" , ";
					}else{
						subquery[numberOfAlias]+=tok.charAt(j);
					}

				}            	            	
				subquery[numberOfAlias]+=" as "+columnlist[ctr]+" "+temp.substring(temp.indexOf("from"));
			}

			numberOfAlias++;
			if(queryString.charAt(0)!=','){            	
				break;
			}else{
				st=new StringTokenizer(queryString.substring(1).trim());
			}

		}

		String newquery="";
		/*Add the select part to new query */
		st=new StringTokenizer(queryString);                    
		//token=st.nextToken();
		
		while(st.hasMoreTokens()){
			
			token=st.nextToken();
			
			if(token.toLowerCase().equals("from")){
				newquery+=token+ " ";
				newquery = parseFromPart(st, newquery, numberOfAlias, subquery, aliasname);				
			}
			else{			
				newquery+=token+ " ";
			}
		}

		return newquery;
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

	public Vector<JoinClauseInfo> getSelectionClauseVector() {
		return selectionClauseVector;
	}

	public void setSelectionClauseVector(
			Vector<JoinClauseInfo> selectionClauseVector) {
		this.selectionClauseVector = selectionClauseVector;
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

	public Vector<Vector> getEquivalenceClassVector() {
		return equivalenceClassVector;
	}

	public void setEquivalenceClassVector(Vector<Vector> equivalenceClassVector) {
		this.equivalenceClassVector = equivalenceClassVector;
	}

	public Vector<Node> getSubQJC() {
		return subQJC;
	}

	public void setSubQJC(Vector<Node> subQJC) {
		this.subQJC = subQJC;
	}

	public HashMap<String, String> getCurrentAliasTables() {
		return currentAliasTables;
	}

	public void setCurrentAliasTables(HashMap<String, String> currentAliasTables) {
		this.currentAliasTables = currentAliasTables;
	}

	public String getCurrentQueryId() {
		return currentQueryId;
	}

	public void setCurrentQueryId(String currentQueryId) {
		this.currentQueryId = currentQueryId;
	}

	public int getTableNo() {
		return tableNo;
	}

	public void setTableNo(int tableNo) {
		this.tableNo = tableNo;
	}

	public Vector<Node> getInClauseConds() {
		return inClauseConds;
	}

	public void setInClauseConds(Vector<Node> inClauseConds) {
		this.inClauseConds = inClauseConds;
	}

	

	public boolean isDeleteNode() {
		return isDeleteNode;
	}

	public void setDeleteNode(boolean isDeleteNode) {
		this.isDeleteNode = isDeleteNode;
	}

	public boolean isUpdateNode() {
		return isUpdateNode;
	}

	public void setUpdateNode(boolean isUpdateNode) {
		this.isUpdateNode = isUpdateNode;
	}

	public Vector<Node> getUpdateColumn() {
		return updateColumn;
	}

	public void setUpdateColumn(Vector<Node> updateColumn) {
		this.updateColumn = updateColumn;
	}

	public QueryParser getLeftQuery() {
		return leftQuery;
	}

	public void setLeftQuery(QueryParser leftQuery) {
		this.leftQuery = leftQuery;
	}

	public QueryParser getRightQuery() {
		return rightQuery;
	}

	public void setRightQuery(QueryParser rightQuery) {
		this.rightQuery = rightQuery;
	}

	public HashMap<String, Node> getConstraintsWithParameters() {
		return constraintsWithParameters;
	}

	public void setConstraintsWithParameters(
			HashMap<String, Node> constraintsWithParameters) {
		this.constraintsWithParameters = constraintsWithParameters;
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

	public Vector<Node> getJoinConditionList() {
		return joinConditionList;
	}

	public void setJoinConditionList(Vector<Node> joinConditionList) {
		this.joinConditionList = joinConditionList;
	}

	public static String[] getCvcRelationalOperators() {
		return cvcRelationalOperators;
	}

	public static void setCvcRelationalOperators(String[] cvcRelationalOperators) {
		QueryParser.cvcRelationalOperators = cvcRelationalOperators;
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

	public void setWhereClauseSubqueries(Vector<QueryParser> whereClauseSubqueries) {
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

	/**
	 * Revamp allConds. It should now contain the distinct predicates not
	 * containing a AND (or OR but ORs not considered for the moment) TODO: Do
	 * something about the presence of ORs: Need to convert the predicate into
	 * CNF and then create datasets by nulling each pair Eg.: if R.a = S.b OR
	 * T.c = U.d is the predicate, then create datasets by killing each of the
	 * following: 1. R.a and Tc 2. R.a and U.d 3. S.b and T.c 4. S.b and U.d
	 */

	public static void flattenAndSeparateAllConds(QueryParser qParser) {
		if(qParser.allConds == null)
			return ;

		Vector<Node> allCondsDuplicate = new Vector<Node>();
		allCondsDuplicate = (Vector<Node>) qParser.allConds.clone();

		qParser.allConds.removeAllElements();
		Vector<Vector<Node>> allDnfDuplicate= new Vector<Vector<Node>>();
		allDnfDuplicate =(Vector<Vector<Node>>) qParser.dnfCond.clone();

		qParser.dnfCond.removeAllElements();
		Node temp;
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			if(allCondsDuplicate.get(i) != null)
				qParser.allConds.addAll(GetNode.flattenNode(qParser, allCondsDuplicate.get(i)));
		}

		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			if(allCondsDuplicate.get(i) != null)
				qParser.dnfCond.addAll(GetNode.flattenCNF(qParser, allCondsDuplicate.get(i)));
		}

		for (int i=0;i< allCondsDuplicate.size() ; i++) {
			if(allCondsDuplicate.get(i)!=null){
			ORNode t = GetNode.flattenOr(allCondsDuplicate.get(i));
				for(Node n: t.leafNodes){
					qParser.orNode.leafNodes.add(n);
				}
				
				for(ANDNode n: t.andNodes){
					qParser.orNode.andNodes.add(n);
				}
				
//				qParser.orNode=GetNode.flattenOr(allCondsDuplicate.get(i));
			}

		}

		Conjunct.createConjuncts(qParser);

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
						||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
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
				qParser.allDnfSubQuery.add(subCond);
			}
		}

		for(Node n:allCondsDuplicate){
			String type=n.getType();
			/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
			 * from the If condition below removed by mathew on 29 June 2016
			 * corresponding All node type and Any node type expressions added
			 */
			if(type.equalsIgnoreCase(Node.getAllNodeType())
					||type.equalsIgnoreCase(Node.getAnyNodeType())
					|| type.equalsIgnoreCase(Node.getInNodeType()) ||
					type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
					||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
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

			Conjunct con = new Conjunct( new Vector<Node>());

			boolean isJoinNodeForEC = GetNode.getJoinNodesForEC(con, temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeForEC) {
				isJoinNodeForEC = false;
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

			Conjunct con = new Conjunct( new Vector<Node>());

			boolean isSelection = GetNode.getSelectionNode(con,temp);
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

			Conjunct con = new Conjunct( new Vector<Node>());
			boolean isLikeType = GetNode.getLikeNode(con,temp);
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
			Vector<Node> lhsRhs = GetNode.flattenNode(qParser, n.getLhsRhs());
			qParser.lhsRhsConds.addAll(lhsRhs);				//Why is this variable required???
		}

		for(Node n: qParser.allSubQueryConds){
			if(n.getSubQueryConds()!=null){
				Vector<Node> subQConds=(Vector<Node>)n.getSubQueryConds().clone();
				n.getSubQueryConds().removeAllElements();
				for(Node subQ:subQConds){
					n.getSubQueryConds().addAll(GetNode.flattenNode(qParser,subQ));
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
	public HashMap<Integer,Vector<CaseCondition>> getCaseConditionMap() {
		return caseConditionMap;
	}

	/**
	 * @param caseConditionMap the caseConditionMap to set
	 */
	public void setCaseConditionMap(HashMap<Integer,Vector<CaseCondition>> caseConditionMap) {
		this.caseConditionMap = caseConditionMap;
	}

}
