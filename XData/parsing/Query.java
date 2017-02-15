package parsing;



import java.io.Serializable;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.impl.sql.compile.ResultSetNode;
import parsing.Table;
import parsing.JoinClauseInfo;
import util.*;

/**
 * @author Bhanu Pratap Gupta
 */

public class Query implements Serializable{
	
	private static Logger logger = Logger.getLogger(Query.class.getName());
	private static final long serialVersionUID = 4162569452627702020L;
	private String queryString;
	private String modifiedQuery;
	private String queryForGroupBy;
	private ResultSetNode parsedQuery ;
	private Map<String,Table> fromTables;
	private Graph<Table,JoinClauseInfo> joinGraph;//Info
	private Vector<Table> joinTables;
	private Vector<JoinClauseInfo> joinClauses;
	//private Vector<WhereClause> whereClauses;
	public Vector<Column> projectedColumns;
	private Vector<Column> groupByColumns;
	// added by sandeep & suhas
	private Vector<JoinClauseNew> joinClausesNew;
	private AggregateClause aggregateClause;
	private HashMap<String,Integer> currentIndex;
	private HashMap<String,String> baseRelation; /* Stores the base relation for each repeated occurrence of a relation */
	private HashMap<String,String> aliasTableNameNo;
	// Added by Shree to get the tableName corresponding to the single table alias name
	private HashMap<String,String> aliasTableName;
	
	private HashMap<String,Integer> currentIndexCount; /*Maintains the increment for each repeated occurrence of a relation. For instance if R repeats twice as R1 and R2 then the currentIndexCount of R is 0, R1 is 1 and that of R2 is 2. */
	private HashMap<String,Integer> repeatedRelationCount; /* For each relation maintains a count of how many times it repeats. Incides of a relation should be incremented by this number */
	//private HashMap<String, int [] > tableNameToQuery;//keeps mapping from table name froom clause sub query index
	//Added by Bhupesh
	/*
	 * Used for storing which aliases point to which tables. In case of subqueries, one alias can point to more than one table.
	 */
	private String aliasTables[][];
	int aliasIndex;

	transient Connection conn ;
	
	public Query(String query){
		this.queryString = query;
		joinTables = new Vector<Table>();
		fromTables = new LinkedHashMap<String, Table>();
		projectedColumns = new Vector<Column>();
		
		joinGraph = new Graph<Table,JoinClauseInfo>(false);
		//joinClauses = new Vector<JoinClause>();
		//joinClausesNew = new Vector<JoinClauseNew>();
		//whereClauses = new Vector<WhereClause>();
		groupByColumns = new Vector<Column>();
		aggregateClause = new AggregateClause();
		currentIndex = new HashMap<String,Integer>();
		baseRelation = new HashMap<String,String>();
		aliasTableNameNo = new HashMap<String,String>();
		aliasTableName  = new HashMap<String,String>();
		currentIndexCount = new HashMap<String,Integer>();
		repeatedRelationCount = new HashMap<String,Integer>();
		aliasTables = new String[50][2];
		aliasIndex = 0;
		//tableNameToQuery = new HashMap<String, int []>();
	}
	
	public void addGroupByColumn (Column col) {
		this.groupByColumns.add(col);
	}
	
	public Vector<Column> getGroupByColumns() {
		return this.groupByColumns;
	}
	
	public ResultSetNode getParsedQuery() {
		return parsedQuery;
	}

	public void setParsedQuery(ResultSetNode parsedQuery) {
		this.parsedQuery = parsedQuery;
	}
	
	public String getModifiedQueryString() {
		return modifiedQuery;
	}
	public void setModifiedQueryString(String modifiedQuery) {
		this.modifiedQuery = modifiedQuery;
	}
	
	public void setQueryForGroupBy(String queryForGroupBy){
		this.queryForGroupBy = queryForGroupBy ;
	}
	
	public String getQueryForGroupBy(){
		return queryForGroupBy;
	}
	
	
	public String getQueryString() {
		return queryString;
	}
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public Vector<Table> getJoinTables() {
		return joinTables;
	}
	
	public Map<String,Table> getFromTables() {
		return fromTables;
	}
	
	public void setProjectedColumns(Vector<Column> columns){
		projectedColumns = columns;
	}
	
	public Vector<Column> getProjectedColumns(){
		return projectedColumns;
	}
	
	public Graph<Table,JoinClauseInfo> getJoinGraph(){
		return joinGraph;
	}
	//Info
	public Vector<JoinClauseInfo> getAllJoinClauses(){
		return joinClauses;
	}
	
	public Vector<JoinClauseNew> getAllJoinClausesNew(){
		return joinClausesNew;
	}
	
	//Info
	public void addToJoinGraph(Table table1, Table table2, JoinClauseInfo joinClauseInfo){
		joinGraph.add(table1, table2, joinClauseInfo);
		joinClauses.add(joinClauseInfo);
	}
	
	public void addJoinClauseNew(JoinClauseNew joinClauseNew){
		joinClausesNew.add(joinClauseNew);
	}
	

	
	public void addFromTable(Table table){
		logger.log(Level.INFO,"addFromTable : Table Name "+table);
		fromTables.put(table.getTableName(), table);
	}
	
	public void addJoinTable(Table table){
		joinTables.add(table);
	}
	
	public void setSelectClauseAggregateFunctions(Aggregate aggr){
		aggregateClause.addSelectClause(aggr);
	}
	
	public Vector<Aggregate> getSelectClauseAggregateFunctions(){
		return aggregateClause.getSelectClause();
	}
	
	public void setHavingClauseAggregateFunctions(Aggregate aggr){
		aggregateClause.addHavingClause(aggr);
	}
	
	public Vector<Aggregate> getHavingClauseAggregateFunctions(){
		return aggregateClause.getHavingClause();
	}
	
	public AggregateClause getAggregateClause(){
		return aggregateClause;
	}
	
	public void putCurrentIndex(String tableName, int index){
		this.currentIndex.put(tableName, index);
	}
	
	public void putBaseRelation(String aliasName, String tableName){
		this.baseRelation.put(aliasName, tableName);
	}
	public void putAliasTableNameNo(String aliasName,String tableNameno){
		this.aliasTableNameNo.put(aliasName,tableNameno);
	}
	//Added by Shree
	public HashMap<String,String> getAliasTableName() {
		return aliasTableName;
	}

	public void putAliasTableName(String aliasName, String tableName) {
		this.aliasTableName.put(aliasName,tableName);
	}
	//Added By shree ends
	
	public void putCurrentIndexCount(String tableName, int index){
		this.currentIndexCount.put(tableName, index);
	}
	
	public void putRepeatedRelationCount(String tableName, int index){
		this.repeatedRelationCount.put(tableName, index);
	}
	
	public void putAliasTables(String aliasName, String tableName){
		this.aliasTables[aliasIndex][0] = aliasName;
		this.aliasTables[aliasIndex][1] = tableName;
		aliasIndex++;
	}
	
	public HashMap<String,Integer> getCurrentIndex(){
		return this.currentIndex;
	}
	
	public HashMap<String,String> getBaseRelation(){
		return this.baseRelation;
	}
	public HashMap<String,String> getAliasTableNameNo(){
		return this.aliasTableNameNo;
	}
	
	public HashMap<String,Integer> getCurrentIndexCount(){
		return this.currentIndexCount;
	}
	
	public HashMap<String,Integer> getRepeatedRelationCount(){
		return this.repeatedRelationCount;
	}
	
	//added by mathew on 1st october 2016
	public void setRepeatedRelationCount(HashMap<String,Integer> repeatedRC){
		this.repeatedRelationCount=repeatedRC;
	}

	
	/*public void putTableNameToQueryIndex(String tableName, int queryType, int queryIndex){
		int [] a = new int[2];
		a[0] = queryType;
		a[1] = queryIndex;
		this.tableNameToQuery.put(tableName, a);
	}
	
	public HashMap<String, int []> getTableNameToQueryIndex(){
		return this.tableNameToQuery;
	}*/
	
	public Vector<String> getTableOfAlias(String aliasName){
		Vector<String> listOfTables = new Vector<String>(); 
		for(int i=0;i<aliasTables.length;i++){
			if(aliasTables[i][0]==null){
				break;
			}
			else{
				if(aliasTables[i][0].equalsIgnoreCase(aliasName)){
					listOfTables.add(aliasTables[i][1]);
				}
			}
		}
		return listOfTables;
	}
	/*
	public Vector getJoinAttribute(Vector<JoinClause> joinClause){
		Vector joinAttribute = new Vector();
		
		for(int i=0; i< joinClause.size(); i++){
			JoinClause jc = joinClause.get(i);
			Table table1 = jc.getJoinTable1();
			Table table2 = jc.getJoinTable2();
			
			Vector temp_for_joinAttributes = jc.getOneTableJoinAttributes(table1.getTableName());
			temp_for_joinAttributes.addAll(jc.getOneTableJoinAttributes(table2.getTableName()));
			
			for(int j=0; j<temp_for_joinAttributes.size(); j++){
				if(!joinAttribute.contains(temp_for_joinAttributes.get(j).toString()))
					joinAttribute.add(temp_for_joinAttributes.get(j).toString());
			}
		}
		return joinAttribute;
	}*/


}