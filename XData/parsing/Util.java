package parsing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;

import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import org.apache.derby.impl.sql.compile.FromBaseTable;
import org.apache.derby.impl.sql.compile.FromSubquery;
import org.apache.derby.impl.sql.compile.JoinNode;
import org.apache.derby.impl.sql.compile.QueryTreeNode;
import parsing.ForeignKey;
import parsing.JoinClauseInfo;
import parsing.QueryAliasMap;
import parsing.Table;
import parsing.Column;
import parsing.FromListElement;
import parsing.Node;
import util.Graph;

public class Util {

	private static Logger logger = Logger.getLogger(Util.class.getName()); 
	
	/**
	 * 
	 * @param colName
	 * @param tabName
	 * @param f
	 * @param x
	 * @param qParser
	 * @return
	 */
	public static Node getColumnFromOccurenceInJC(String colName, String tabName,
			FromListElement f, int x, QueryParser qParser) {

		Column col = null;
		String columnName = colName; // Column name will be exact. Tablename can
		// be an alias.
		if (f.getTableName() != null) { // FromListElement is a base table. Take
			// the column directly.
			String fromTableName = f.getTableName();
			if (qParser.getQuery().getFromTables().get(fromTableName.toUpperCase()) != null) {

				Table t = qParser.getQuery().getFromTables().get(fromTableName.toUpperCase());				
				
				/*@author mathew on 20 June 2016
				 *  the following if condition added to extract relative name of
				 *  the column name
				 */
				if(columnName.contains(".")){
					columnName=columnName.substring(colName.indexOf('.')+1).toUpperCase();
				}

				col = t.getColumn(columnName.toUpperCase());
				if(col==null){					
					return null;
				}
				Node n = new Node();
				n.setColumn(col);
				n.setTable(col.getTable());
				//FIXME: mahesh set table aias
				if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
					n.setTableNameNo(f.getTableNameNo());
					}//This may not be correct - FIXME Test and fix
					else{
						n.setTableNameNo(f.getTableName()+"1");
					}
					n.setType(Node.getColRefType());
				return n;
			} else
				return null;
		}
		/*
		 * If baseTable is null then it is either Join Node or Sub query
		 * Difference is only that a join node will have an array of
		 * FromListElement of 2 elements while a subquery node may have a longer
		 * array
		 */
		else {
			Boolean found = false;
			FromListElement temp = null;
			if (tabName != null && !tabName.isEmpty()) {
				for (int i = 0; i < f.getTabs().size(); i++) { 
					temp = f.getTabs().get(i);
					if (temp.getAliasName() != null) {
						if (temp.getAliasName().equalsIgnoreCase(tabName)) {							
							found = true;
							break;
						}

					}
					if (temp.getTableName() != null) {
						if (temp.getTableName().equalsIgnoreCase(tabName)) {							
							found = true;
							break;
						}
					} else {
						found = false;
					}
				}
				if (found) {					
					Node n = getColumnFromOccurenceInJC(colName, tabName, temp,
							1,qParser);
					if (n != null) {
						return n;
					}
				} else {
					for (int i = 0; i < f.getTabs().size(); i++) {
						temp = f.getTabs().get(i);
						if (temp.getTableName() == null) {
							Node n = getColumnFromOccurenceInJC(colName,
									tabName, temp, qParser);
							if (n != null) {
								return n;
							}
						}
					}
				}
			} else {
				for (int i = 0; i < f.getTabs().size(); i++) {
					Node n = getColumnFromOccurenceInJC(colName, tabName, f
							.getTabs().get(i), 1, qParser);
					if (n != null) {
						return n;
					}
				}
			}
			return null;
		}
		
	
	}
	
	public static Node getColumnFromOccurenceInJC(String colName, String tabName,
			FromListElement f, QueryParser qParser) {
		Column col = null;
		String columnName = colName; // Column name will be exact. Tablename can
		// be an alias.

 
			if (f.getTableName() != null && !f.getTableName().isEmpty()) { // FromListElement is a base table. Take
			// the column directly.
			if (f.getAliasName()!= null && !f.getAliasName().isEmpty() && tabName != null) {
				if (!f.getAliasName().equalsIgnoreCase(tabName)) {
					return null;
				} 
			} 

			 else if (f.getTableName() != null && !f.getTableName().isEmpty() && tabName != null && f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()) {
				if (!f.getTableName().equalsIgnoreCase(tabName) ) {
					return null; 
				}
			}
			String fromTableName = f.getTableName();
			if (qParser.getQuery().getFromTables().get(fromTableName.toUpperCase()) != null && f.getTableNameNo()!= null &&!f.getTableNameNo().isEmpty()) {
				Table t = qParser.getQuery().getFromTables().get(fromTableName.toUpperCase());
				col = t.getColumn(columnName.toUpperCase());
				if (col == null)
					return null;
 
				Node n = new Node();
				n.setColumn(col);
				if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
				n.setTableNameNo(f.getTableNameNo());
				}//This may not be correct - FIXME Test and fix
				else{
					n.setTableNameNo(f.getTableName()+"1");
				}
				n.setType(Node.getColRefType());
				
				return n;
			} else
				return null;
				
		}
		/*
		 * If baseTable is null then it is either Join Node or Sub query
		 * Difference is only that a join node will have an array of
		 * FromListElement of 2 elements while a subquery node may have a longer
		 * array
		 */
		else { // Subquery Node or joinNode;
			Boolean found = false;
			FromListElement temp = null;
			if (tabName != null && !tabName.isEmpty()) {
				for (int i = 0; i < f.getTabs().size(); i++) {
					temp = f.getTabs().get(i);
					if (temp.getAliasName() != null && !temp.getAliasName().isEmpty()) {
						if (temp.getAliasName().equalsIgnoreCase(tabName)) {							
							found = true;
							break;
						}

					} 
					if (temp.getTableName() != null && !temp.getTableName().isEmpty()) {
						if (temp.getTableName().equalsIgnoreCase(tabName)) {							
							found = true;
							break;
						}
					} else {
						found = false;
					}
				}
				if (found) {					
					Node n = getColumnFromOccurenceInJC(colName, tabName, temp,
							1,qParser);
					if (n != null) {
						return n;
					}
				} else {
					for (int i = 0; i < f.getTabs().size(); i++) {
						temp = f.getTabs().get(i);
						if (temp.getTableName() == null) {
							Node n = getColumnFromOccurenceInJC(colName,
									tabName, temp, qParser);
							if (n != null) {
								return n;
							}
						}
					}
				}
			} else {
				for (int i = 0; i < f.getTabs().size(); i++) {
					Node n = getColumnFromOccurenceInJC(colName, tabName, f
							.getTabs().get(i), 1, qParser);
					if (n != null) {
						return n;
					}
				}
			}
			return null;
		}
	
	}
	
	public static Node getNodeForCount(Vector<FromListElement> fle, QueryParser qParser) {
		
		for(FromListElement f:fle){
			String fromTableName = f.getTableName();
			if (fromTableName != null && f.getTableNameNo()!= null &&!f.getTableNameNo().isEmpty()) {
				Table t=qParser.getTableMap().getTable(fromTableName);
				Column col = t.getColumn(0);
				if (col == null)
					return null;

				Node n = new Node();
				n.setTable(t);
				n.setTableAlias(t.getAliasName());
				n.setColumn(col);
				if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
					n.setTableNameNo(f.getTableNameNo());
				}//This may not be correct - FIXME Test and fix
				else{
					n.setTableNameNo(f.getTableName()+"1");
				}
				n.setType(Node.getColRefType());
				return n;
			} else if (f.getTabs()!=null && !f.getTabs().isEmpty()){
				for (int i = 0; i < f.getTabs().size(); i++) {
					Node n = getNodeForCount(f.getTabs().get(i), qParser);
					n.setType(Node.getColRefType());
					if (n != null) {
						return n;
					}
				}
			}
			else if(f.getSubQueryParser()!=null){
				Node n=getNodeForCount(f.getSubQueryParser().fromListElements,f.getSubQueryParser());
				if(n!=null){
					n.setType(Node.getColRefType());
					return n;
				}
			}
		}
		return null;
}
	
	public static Node getNodeForCount(FromListElement f, QueryParser qParser) {
		
			String fromTableName = f.getTableName();
			if (fromTableName != null && qParser.getQuery().getFromTables().get(fromTableName.toUpperCase()) != null 
					&& f.getTableNameNo()!= null &&!f.getTableNameNo().isEmpty()) {
				
				Table t = qParser.getQuery().getFromTables().get(fromTableName.toUpperCase());
				Column col = t.getColumn(0);
				if (col == null)
					return null;
 
				Node n = new Node();
				n.setTable(t);
				n.setTableAlias(t.getAliasName());
				n.setColumn(col);
				if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
					n.setTableNameNo(f.getTableNameNo());
				}//This may not be correct - FIXME Test and fix
				else{
					n.setTableNameNo(f.getTableName()+"1");
				}
				n.setType(Node.getColRefType());
				return n;
			} else{
			for (int i = 0; i < f.getTabs().size(); i++) {
					Node n = getNodeForCount(f.getTabs().get(i), qParser);
					n.setType(Node.getColRefType());
					if (n != null) {
						return n;
					}
				}
			}
			return null;
	}
	//Added by Biplab to return ColumnList. Required if the group by column is same as the joining column. Then two columns with same name must be added to the groupByList
	public static Vector<Node> getColumnListFromOccurenceInJC(String colName, String tabName, FromListElement f, int x, QueryParser qParser) {
		Vector<Node> nList = new Vector<Node>();
		Column col = null;
		String columnName = colName; // Column name will be exact. Table name can
		// be an alias.
		if (f.getTableName() != null && !f.getTableName().isEmpty()) { // FromListElement is a base table. Take
			// the column directly.
			if(f.getAliasName() != null && tabName.equals(f.getAliasName())){
				
				String fromTableName = f.getTableName();
				if (qParser.getQuery().getFromTables().get(fromTableName.toUpperCase()) != null) {
	
					Table t = qParser.getQuery().getFromTables().get(fromTableName.toUpperCase());				
					col = t.getColumn(columnName.toUpperCase());
					if(col==null){					
						return nList;
					}
					Node n = new Node();
					n.setColumn(col);
					n.setTable(col.getTable());
					if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
						n.setTableNameNo(f.getTableNameNo());
						}//This may not be correct - FIXME Test and fix
						else{
							n.setTableNameNo(f.getTableName()+"1");
						}
					//mahesh: added this
					n.setType(Node.getColRefType());
					nList.add(n);
				} 
			}
			else if(f.getAliasName() == null || (f.getAliasName() != null && f.getAliasName().equals(f.getTableName()))){
				
				String fromTableName = f.getTableName();
				if (qParser.getQuery().getFromTables().get(fromTableName.toUpperCase()) != null) {
	
					Table t = qParser.getQuery().getFromTables().get(fromTableName.toUpperCase());				
					col = t.getColumn(columnName.toUpperCase());
					if(col==null){					
						return nList;
					}
					Node n = new Node();
					n.setColumn(col);
					n.setTable(col.getTable());
					if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
						n.setTableNameNo(f.getTableNameNo());
						}//This may not be correct - FIXME Test and fix
						else{
							n.setTableNameNo(f.getTableName()+"1");
						}
					//mahesh: added this
					n.setType(Node.getColRefType());
					nList.add(n);
				} 
				
			}else{
				return nList;
			}
		}
		else
		{
			for (int i = 0; i < f.getTabs().size(); i++) {
				Vector<Node> n = getColumnListFromOccurenceInJC(colName, tabName, f
						.getTabs().get(i), 1,qParser);
				if (n.size() != 0) {
					for(Node nSingle : n)
					{
						logger.log(Level.INFO,"n2 " + nSingle.getTable());
									nList.add(nSingle);
					}
				}
			}
		}
		return nList;
	}
	
	//FIXME: This method and addToSubquery have bugs
	public static Node modifyNode(Node whereClausePred,QueryParser qParser) {
		if(whereClausePred == null )
			return null;
		if (whereClausePred.getType().equalsIgnoreCase(Node.getAndNodeType()) 
				|| whereClausePred.getType().equalsIgnoreCase(Node.getOrNodeType())){ //If AND or OR Node then traverse left and right children	

			String Op = whereClausePred.getOperator();
			String type = whereClausePred.getType();
			Node left = modifyNode(whereClausePred.getLeft(),qParser);
			Node right = modifyNode(whereClausePred.getRight(),qParser);
			//return the updated node 
			if(left == null)
				return right;
			if(right == null)
				return left;

			Node n = new Node();
			n.setType(type);
			n.setOperator(Op);
			n.setLeft(left);
			n.setRight(right);
			return n;
		}
		
		/**get the query block in which the relation occurrence of this condition exists
		 * Depending on this add the condition to sub query*/
		
		//once above is done remove the below code
		//FIXME: Below two  conditions are incorrect. Check Q9 new code
		if((whereClausePred.getLeft() != null && whereClausePred.getLeft().getQueryType() > 0)|| (whereClausePred.getRight() != null && whereClausePred.getRight().getQueryType() > 0)){//If binary relational or arithmetic node and is aliased column inside sub query
			addToSubQuery(whereClausePred,qParser);
			return whereClausePred;
		}
		if(whereClausePred.getQueryType() > 0){//If this condition uses aliased column
			addToSubQuery(whereClausePred,qParser);
			return null;
		}

		return whereClausePred;		
	}
	
	
	public static void addToSubQuery(Node n,QueryParser qParser){
		boolean flag = false;//To indicate whether to add to havingClause or where Clause
		boolean flag1 = false;//To indicate whether Where or from clause subquery
		int index = -1;

		if(n.getRight() != null && n.getRight().getQueryType() > 0 ){// if right side of binary relational condition and it uses aliased column
			if(n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())) flag = true;
			if(n.getRight().getQueryType() == 2 ) flag1 = true;//Where sub query
			index = n.getRight().getQueryIndex();
		}

		if (n.getLeft() != null &&  n.getLeft().getQueryType() > 0 ){//if left side of binary relational condition
			if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())) flag = true;
			if(n.getLeft().getQueryType() == 2 ) flag1 = true;//Where sub query
			index = n.getLeft().getQueryIndex();
		}

		if (n.getLeft() == null && n.getRight() == null && n.getQueryType() > 0){
			if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())) flag = true;
			if(n.getQueryType() == 2 ) flag1 = true;//Where sub query
			index = n.getQueryIndex(); 
		}

		if(flag){//aggregate node
			//Add this condition to having clause 
			Node left;
			if(flag1 == true)	left = qParser.getWhereClauseSubqueries().get(index).getHavingClause();
			else 				left = qParser.getFromClauseSubqueries().get(index).getHavingClause();
			if(left == null){
				if(flag1 == true)   qParser.getWhereClauseSubqueries().get(index).setHavingClause(n);
				else            qParser.getFromClauseSubqueries().get(index).setHavingClause(n);
				return;
			}
			Node n1 = new Node();
			n1.setOperator("AND");
			n1.setType(Node.getAndNodeType());
			n1.setRight(n);
			n1.setLeft(left);
			if(flag1 == true)     qParser.getWhereClauseSubqueries().get(index).setHavingClause(n1);
			else       qParser.getFromClauseSubqueries().get(index).setHavingClause(n1);
		}
		else {//Add to where clause conditions
			if(flag1 == true)    qParser.getWhereClauseSubqueries().get(index).allConds.add(n);
			else          qParser.getFromClauseSubqueries().get(index).allConds.add(n);
		}
	}
	
	
	public static Vector<Node> getAllProjectedColumns(Vector<FromListElement> visitedFLEs, QueryParser qParser){
		Vector<Node> projectedColumns=new Vector<Node>();
		for(FromListElement fle:visitedFLEs){
			if(fle!=null && fle.getTableName()!=null){
				Table t=qParser.getTableMap().getTable(fle.getTableName());
				if(t!=null){
					Iterator colItr=t.getColumns().values().iterator();
					while(colItr.hasNext()){
						Column col=(Column)colItr.next();
						Node n = new Node();
						n.setColumn(col);
						n.setTable(col.getTable());
						n.setLeft(null);
						n.setRight(null);
						n.setOperator(null);
						n.setType(Node.getColRefType());
						n.setTableNameNo(fle.getTableNameNo());
						projectedColumns.add(n);
					}
				}
			}
			else if(fle!=null && fle.getTabs()!=null && !fle.getTabs().isEmpty()){
				projectedColumns.addAll(getAllProjectedColumns(fle.getTabs(),qParser));				
			}
			else if(fle!=null && fle.getSubQueryParser()!=null){
				projectedColumns.addAll(fle.getSubQueryParser().getProjectedCols());
			}
		}
		return projectedColumns;
	}
	
	public static Vector<Node> addAllProjectedColumns(FromListElement q,int queryType, QueryParser qParser) {

		Vector<Node> projectedCols = new Vector<Node>();
		for (int i = 0; i < q.getTabs().size(); i++) {
			if (q.getTabs().get(i).getTableName() != null) {
				Table t = qParser.getTableMap().getTable(q.getTabs().get(i).getTableName().toUpperCase());
				Iterator itr = t.getColumns().values().iterator();
				//mahesh
				Vector<Node> tempProjectedCols = new Vector<Node>();
				while (itr.hasNext()) {
					Node c = new Node();
					Column col = (Column) itr.next();
					c.setColumn(col);
					c.setTable(col.getTable());
					c.setLeft(null);
					c.setRight(null);
					c.setOperator(null);
					c.setType(Node.getColRefType());
					c.setTableNameNo(q.getTabs().get(i).getTableNameNo().toUpperCase());
					/*if(subQueryNames.containsKey(tableName)){//If this node is inside a sub query
						c.setQueryType(1);
						c.setQueryIndex(subQueryNames.get(tableName));
					}
					else{*/
						c.setQueryType(queryType);
						if(queryType == 1) c.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
						if(queryType == 2) c.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					/*}*/
					
					projectedCols.add(c);
					tempProjectedCols.add(c);

				}
			} else {
				projectedCols.addAll(addAllProjectedColumns(q.getTabs().get(i),queryType,qParser));
			}
		}
		return projectedCols;
	
	}
	
	/**
	 * Used to update the details about table occurrence
	 * @param fromSubquery
	 * @param whereSubquery
	 * @param temp
	 */
	public static void updateTableOccurrences(boolean fromSubquery,	boolean whereSubquery, String tableNameNo, QueryParser qParser) {
		Integer[] li= new Integer[2]; 
		if(fromSubquery){
			li[0]=1;
			li[1]=qParser.getFromClauseSubqueries().size()-1;
		}
		else if(whereSubquery){
			li[0]=2;
			li[1]=qParser.getWhereClauseSubqueries().size()-1;			
		}
		else{
			li[0]=0;
			li[1]=0;
		}
		
		qParser.getTableNames().put(tableNameNo, li);
		
	
	}
	
//	// adds the form table to the query
//	public static void addFromTable(Table table, QueryParser qParser) {
//		qParser.getQuery().addFromTable(table);
//		// fromTableMap.put(table.getTableName(), table);
//	}
	
	public static String chop(String str) {
		char LF = '\n';
		char CR = '\r';
		if (str == null) {
			return null;
		}
		int strLen = str.length();
		if (strLen < 2) {
			return "";
		}
		int lastIdx = strLen - 1;
		String ret = str.substring(0, lastIdx);
		char last = str.charAt(lastIdx);
		if (last == LF) {
			if (ret.charAt(lastIdx - 1) == CR) {
				return ret.substring(0, lastIdx - 1);
			}
		}
		return ret;
	}
	
	
	/*
	 * Added by Bhupesh currentQueryLevel is in the set: {'Q', 'SQ1', 'SQ2',
	 * ..., 'SQn'} where SQn is the last level of sub query nesting and Q is the
	 * main query Sample query: select * from (select * from dept d1 join
	 * (select * from crse c) x using (dept_name)) d join teaches using
	 * (course_id);
	 * 
	 * select * from (select * from dept d1 join } SQ1 (select * from teaches t
	 * } } SQ2 ) x using (id) } } ) d join crse using (dept_name)
	 * 
	 * Corresponding Map: queryLevel queryLevelOrTable aliasOfSubQueryOrTable
	 * --------------------------------------------------------------- Q CRSE
	 * NO_ALIAS Q SQ1 d SQ1 SQ2 x SQ1 DEPT d1 SQ2 TEACHES t
	 */
	public static Column getColumn(String colName, String aliasIfAny,
			String currentQueryLevel, QueryParser qParser) {

		Column col = null;

		if (aliasIfAny != null) {
			for (QueryAliasMap qamElement : qParser.getQam()) {
				if (qamElement.queryId.equalsIgnoreCase(currentQueryLevel)) { // Compare
					// Sub
					// Query
					// level
					if (qamElement.aliasOfSubqueryOrTable
							.equalsIgnoreCase(aliasIfAny)) { // Compare
						// AliasName
						if (qamElement.queryIdOrTableName.contains("SQ")) { // If
							// inside
							// another
							// Sub
							// Query
							// Then recurse but now the alias name does not
							// matter. The column can be with any alias inside
							// the new subquery.
							return getColumn(colName, null,
									qamElement.queryIdOrTableName,qParser);
						} else {// If not a sub query then its a table
							// If colName exists in the table: queryLevelOrTable
							// then return that column
							Table table = qParser.getTableMap()
									.getTable(qamElement.queryIdOrTableName);
							for (int i = 0; i < table.getColumns().size(); i++) {
								if (table.getColumn(i).getColumnName().equalsIgnoreCase(colName)) {
									return table.getColumn(i);
								}
							}
						}
					}
				}
			}
		} else { // alias is not given. Now, find the column. Same as above but
			// without the aliasName check.
			for (QueryAliasMap qamElement : qParser.getQam()) {
				if (qamElement.queryId.equalsIgnoreCase(currentQueryLevel)) {
					if (qamElement.queryIdOrTableName.contains("SQ")) {
						return getColumn(colName, null,
								qamElement.queryIdOrTableName,qParser);
					} else {
						// If colName exists in the table: queryLevelOrTable
						// then return that column
						Table table = qParser.getTableMap()
								.getTable(qamElement.queryIdOrTableName);
						for (int i = 0; i < table.getColumns().size(); i++) {
							if (table.getColumn(i).getColumnName()
									.equalsIgnoreCase(colName)) {
								return table.getColumn(i);
							}
						}
					}
				}
			}
		}
		return col;
	}
	
	/*
	 * private Vector<Table> getTables(String tableOrAliasName){ Vector<Table>
	 * tables = new Vector<Table>(); for(String tableName :
	 * query.getFromTables().keySet()){ Table table =
	 * query.getFromTables().get(tableName);
	 * if(table.getTableName().equalsIgnoreCase(tableOrAliasName) ||
	 * (table.getAliasName()!=null &&
	 * table.getAliasName().equals(tableOrAliasName))){ tables.add(table); } }
	 * return tables; }
	 */
	// returns all the columns having name searchcolumnName from all the tabes
	// in from clause
	public static Vector<Column> getColumns(String searchcolumnName, QueryParser qParser) {
		Vector<Column> columns = new Vector<Column>();
		// traverse through the tables in the form clause
		for (String tableName : qParser.getQuery().getFromTables().keySet()) {
			Table table = qParser.getQuery().getFromTables().get(tableName.toUpperCase());
			// traverse all the columns of the table
			for (String columnName : table.getColumns().keySet()) {
				Column column = table.getColumn(columnName);
				// if column name matches
				if (column.getColumnName().equalsIgnoreCase(searchcolumnName)) {
					columns.add(column); // add the column to columns
				}
			}
		}
		return columns;
	}

	// retrives column from table tableName and having name as columnName
	public static Column getColumn(String columnName, String tableName, QueryParser qParser) {
		Table table = qParser.getQuery().getFromTables().get(tableName.toUpperCase());
		if (table == null) {// Then tablename is the alias of the table
			Vector<String> tableNames = qParser.getQuery().getTableOfAlias(tableName);
			for (int i = 0; i < tableNames.size(); i++) {
				table = qParser.getQuery().getFromTables().get(tableNames.get(i).toUpperCase());
				if (table != null)
					break;
			}
		}
		return table.getColumn(columnName);
	}

	// returns all the columns having name columnName and table alias name
	// tableOrAlisName
	// modify for the case when tableoraliasname is the alias of a subquery
	public static Vector<Column> getJoinColumns(String columnName,
			String tableOrAliasName, QueryParser qParser) {
		Vector<Column> columns = new Vector<Column>();
		Table table = null;
		// Added by Bhupesh
		Vector<String> tableNames = new Vector<String>();
		tableNames = qParser.getQuery().getTableOfAlias(tableOrAliasName);
		for (int i = 0; i < tableNames.size(); i++) {
			table = qParser.getTableMap().getTable(tableNames.get(i));
			if (table != null) {
				if (table.getColumns().get(columnName) != null) {
					columns.add(table.getColumns().get(columnName));
				}
			}
		}
		return columns;
	}

	public static Vector<Column> getJoinColumnsJSQL(String columnName, FromItem node, QueryParser qParser)
			throws Exception {
		Vector<Column> columns = new Vector<Column>();
		// check if node is child of the FromBaseTable
		if (node instanceof net.sf.jsqlparser.schema.Table) {
			net.sf.jsqlparser.schema.Table fbTable = (net.sf.jsqlparser.schema.Table) node;// typecasting the node
			// to FromBaseTable
			String tableName = fbTable.getFullyQualifiedName().toUpperCase();// extracting the base
			// table
			if (qParser.getTableMap().getTable(tableName.toUpperCase()).getColumn(columnName) != null)// if
				// the table contains the column of name columnName
				columns.add(qParser.getTableMap().getTable(tableName.toUpperCase()).getColumn(columnName));
			//add the column to columns
		}
		// check if the node is an instance of JoinNode
		else if (node instanceof Join) {
			// create a join node by typecasting node
			Join joinNode = (Join) node;
			// recursively calls the getJoinColumns on left child of the node
			Vector<Column> leftColumns = getJoinColumnsJSQL(columnName, joinNode.getRightItem(),qParser);
			// recursively calls the getJoinColumns on right child of the node
			//Vector<Column> rightColumns = getJoinColumns(columnName, joinNode
			//		.getRightResultSet(),qParser);

			for (Column column : leftColumns)
				columns.add(column);
			//for (Column column : rightColumns)
			//	columns.add(column);

		} else if (node instanceof SubSelect) {
			SubSelect fromSubquery = (SubSelect) node;
			PlainSelect ps = (PlainSelect)fromSubquery.getSelectBody();
			columns.addAll(getJoinColumnsJSQL(columnName, ps.getFromItem(),qParser));
			if (ps.getJoins() != null) {
				for (Iterator joinsIt = ps.getJoins().iterator(); joinsIt.hasNext();) {
					Join join = (Join) joinsIt.next();
					columns.addAll(getJoinColumnsJSQL(columnName,join.getRightItem(),qParser));
				}
			}
		}
		return columns;
	}
	

	public static Vector<Column> getJoinColumnsForJSQL(String columnName, Vector<FromListElement> node, QueryParser qParser)
			throws Exception {
		Vector<Column> columns = new Vector<Column>();
		//if(frm != null){
		//	columns = getJoinColumnsJSQL(columnName, frm, qParser);
		//}else{
			for(int i=0; i< node.size();i++ ){
				FromListElement fle = node.get(i);
				//net.sf.jsqlparser.schema.Table fbTable = (net.sf.jsqlparser.schema.Table) node;// typecasting the node
				// to FromBaseTable
				if(fle != null && fle.tableName != null){
					String tableName = fle.tableName.toUpperCase();// extracting the base
					// table
					if (qParser.getTableMap().getTable(tableName.toUpperCase()).getColumn(columnName) != null)// if				
						columns.add(qParser.getTableMap().getTable(tableName.toUpperCase()).getColumn(columnName));// add
					
					//Shree added for join conditions
					else{
					if(node.get(i) != null && node.get(i).getTabs() != null)
						columns.addAll(getJoinColumnsForJSQL(columnName,node.get(i).getTabs(),qParser));
					} 
				}else{
					columns.addAll(getJoinColumnsForJSQL(columnName, node.get(i).getTabs(),qParser));
					
				}
			//	}
		}
		
		return columns;
	}
	
	public static Vector<Column> getJoinColumns(String columnName, QueryTreeNode node, QueryParser qParser)
			throws Exception {
		Vector<Column> columns = new Vector<Column>();
		// check if node is child of the FromBaseTable
		if (node instanceof FromBaseTable) {
			FromBaseTable fbTable = (FromBaseTable) node;// typecasting the node
			// to FromBaseTable
			String tableName = fbTable.getBaseTableName();// extracting the base
			// table
			if (qParser.getTableMap().getTable(tableName).getColumn(columnName) != null)// if
				// the
				// table
				// contains
				// the
				// column
				// of
				// name
				// columnName
				columns.add(qParser.getTableMap().getTable(tableName).getColumn(columnName));// add
			// the
			// column
			// to
			// columns
		}
		// check if the node is an instance of JoinNode
		else if (node instanceof JoinNode) {
			// create a join node by typecasting node
			JoinNode joinNode = (JoinNode) node;
			// recursively calls the getJoinColumns on left child of the node
			Vector<Column> leftColumns = getJoinColumns(columnName, joinNode
					.getLeftResultSet(),qParser);
			// recursively calls the getJoinColumns on right child of the node
			Vector<Column> rightColumns = getJoinColumns(columnName, joinNode
					.getRightResultSet(),qParser);

			for (Column column : leftColumns)
				columns.add(column);
			for (Column column : rightColumns)
				columns.add(column);

		} else if (node instanceof FromSubquery) {
			FromSubquery fromSubquery = (FromSubquery) node;
			columns = getJoinColumns(columnName, fromSubquery.getSubquery()
					.getFromList().getNodeVector().get(0),qParser);
		}
		return columns;
	}
	
	
	/*
	 * Added by Mahesh
	 * To get join conditions where the join conditions are on aliased names ( which may be aliased for aggregates also)
	 */
	public static Vector<Node> getAliasedNodes(Vector<FromListElement> fle, List <String> cols, QueryParser qParser) {
		Vector<Node> list = new Vector<Node>();
		for(String colName: cols){
			if(qParser.getQuery().getQueryString().toLowerCase().contains(("as "+colName.toLowerCase()))){//It is aliased column
				Vector<Node> names = new Vector<Node>();
				names = qParser.getAliasedToOriginal().get(colName);

				for(Node name: names){
					String tableNameNo;
					//If aggregate node
					if(name.getType().equalsIgnoreCase(Node.getAggrNodeType()))

						tableNameNo = name.getAgg().getAggExp().getTableNameNo().toUpperCase();

					else tableNameNo = name.getTableNameNo().toUpperCase(); //Not an aggregate node


					if(checkIn(tableNameNo,fle)){//Should add iff and only if it is aliased column in this from list elements
						list.add(name);
					}
				}
			}
		}
		return list;
	}
	
	/*
	 * Checks whether the tableNameNo is present in the given list of from list elements
	 */

	public static boolean checkIn(String tableNameNo, Vector<FromListElement> fle) {
		for(FromListElement fl: fle){
			if(fl.getTableName() == null){
				if(checkIn(tableNameNo, fl.getTabs()) == true)
					return true;
			}
			else if(fl.getTableNameNo().equalsIgnoreCase(tableNameNo))
				return true;
		}
		return false;
	}
	
	/*Added by Mahesh
	 * Handling joins that may involve sub queries 
	 */ 

	public static ArrayList<String> getColumnNames( Vector<Node> projectedCols){

		ArrayList <String> cols = new ArrayList<String>();
		for(Node n: projectedCols){
			String name;
			if(n!= null){
			//If aggregate node
			if(n.getType().equalsIgnoreCase(Node.getAggrNodeType()))
				name = n.getAgg().getAggAliasName();
			else
				name = n.getColumn().getAliasName() ;
			cols.add( name.toUpperCase());
			}
		} 
		return cols;
	}
	
	//Added By Ankit
	//Modified by Mahesh
	//Modify--to include element from projected class...not from original table
	public static ArrayList<String> getAllColumnofElement(Vector<FromListElement> t, QueryParser qParser) throws Exception{

		ArrayList <String>allColumn=new ArrayList();
		for(int i=0;i<t.size();i++){
			FromListElement f=t.get(i);
			if(f.getTableName()!=null){
				for(String columnName : qParser.getTableMap().getTable(f.getTableName()).getColumns().keySet()){
					if(!allColumn.contains(columnName))
						allColumn.add(columnName);            	
				}
			}
			
		}
		return allColumn;    	
	}
	
	public static ArrayList<String> getAllColumnofElementForJSQL(Vector<FromListElement> t,ArrayList<String> allColumn,QueryParser qParser) throws Exception{

		for(int i=0;i<t.size();i++){
			FromListElement f=t.get(i);
			if(f.getTableName()!=null){
				for(String columnName : qParser.getTableMap().getTable(f.getTableName()).getColumns().keySet()){
					if(!allColumn.contains(columnName))
						allColumn.add(columnName);            	
				}
			}
			else{
				if(f.getTabs() != null && f.getTabs().size() > 0)
					allColumn.addAll(getAllColumnofElementForJSQL(f.getTabs(),new ArrayList<String>(),qParser) );
			}
		}
		return allColumn;    	
	}
	
	public static void setQueryTypeAndIndex(boolean isFromSubquery,	boolean isWhereSubquery, boolean rightSubquery, Node n, QueryParser qParser) {
		if(isFromSubquery){
			n.setQueryType(1);
			n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
		}
		else if(isWhereSubquery){
			n.setQueryType(2);
			n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
		}
		else if(rightSubquery){
			n.setQueryType(1);
			n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
		}
		else
			n.setQueryType(0);
	}
	
	
	
	/*
	 * If the sub query is of the form col relop subQ then when parsing the relop the condition is added as a BRO condition. This function 
	 * takes the BRO nodes and add it to subquery if the left or right of the bro node is a subquery
	 */
	//FIXME: Mahesh may be problematic because node may be aggregate node
	public static void modifyTreeForComapreSubQ(Node n){
		if( n == null)
			return ;
		if(n.getType().equals(Node.getBroNodeType()) || n.getType().equals(Node.getLikeNodeType())){

			if(n.getLeft().getType().equals(Node.getStringFuncNodeType())){
				Node temp=n.getLeft();
				n.setLeft(temp.getLeft());
				if(temp.getLeft().getType().equals(Node.getColRefType())) {
					if(temp.getOperator().equalsIgnoreCase("upper") || temp.getOperator().equalsIgnoreCase("lower")){
						if(n.getOperator().equals("=") || n.getOperator().equals("~"))
							n.setOperator("i~");	
						else if(n.getOperator().equals("<>")) //operator here cannot be not like- not like is added only after flattening the not node
							n.setOperator("!i~");
						else if(n.getOperator().equals(">") || n.getOperator().equals(">=")){
							String str=n.getRight().getStrConst();
							n.getRight().setStrConst(str.toLowerCase());
						}
						else if(n.getOperator().equals("<") || n.getOperator().equals("<=")){
							String str=n.getRight().getStrConst();
							n.getRight().setStrConst(str.toUpperCase());
						}
					}
				}
				else if(temp.getLeft().getType().equals(Node.getValType())){						
					if(temp.getOperator().equalsIgnoreCase("upper")){
						n.getLeft().setStrConst(temp.getLeft().getStrConst());
					}
					else if(temp.getOperator().equalsIgnoreCase("lower")){
						n.getLeft().setStrConst(temp.getLeft().getStrConst().toLowerCase());
					}
				}
			}
			if (n.getRight()==null){
				logger.log(Level.INFO,"modifyTreeForComapreSubQ : n.getRight is NULL");
			}
			else if(n.getRight().getType().equals(Node.getStringFuncNodeType())){
				Node temp=n.getRight();
				n.setRight(temp.getLeft());
				if(temp.getLeft().getType().equals(Node.getColRefType())) {
					if(temp.getOperator().equalsIgnoreCase("upper") || temp.getOperator().equalsIgnoreCase("lower")){
						if(n.getOperator().equals("=") || n.getOperator().equals("~"))
							n.setOperator("i~");	
						else if(n.getOperator().equals("<>")) //operator here cannot be not like- not like is added only after flattening the not node
							n.setOperator("!i~");
						else if(n.getOperator().equals(">") || n.getOperator().equals(">=")){
							String str=n.getLeft().getStrConst();
							n.getLeft().setStrConst(str.toLowerCase());
						}
						else if(n.getOperator().equals("<") || n.getOperator().equals("<=")){
							String str=n.getLeft().getStrConst();
							n.getLeft().setStrConst(str.toUpperCase());
						}
					}
				}
				else if(temp.getLeft().getType().equals(Node.getValType())){
					if(temp.getOperator().equalsIgnoreCase("upper")){
						n.getRight().setStrConst(temp.getLeft().getStrConst().toUpperCase());
					}
					else if(temp.getOperator().equalsIgnoreCase("lower")){
						n.getRight().setStrConst(temp.getLeft().getStrConst().toLowerCase());
					}
				}
			}


			if(n.getRight()!=null && n.getRight().getType().equalsIgnoreCase(Node.getBroNodeSubQType())){
				n.setType(Node.getBroNodeSubQType());
				n.setSubQueryConds(n.getRight().getSubQueryConds());
				n.getRight().setSubQueryConds(null);
				if(n.getRight().getLhsRhs() != null){
					n.getRight().setAgg(n.getRight().getLhsRhs().getAgg());//added by mahesh
				}
				n.setLhsRhs(n.getRight().getLhsRhs());
				n.getRight().setLhsRhs(null);
				n.getRight().setType(Node.getAggrNodeType());
			}
			if(n.getLeft().getType().equalsIgnoreCase(Node.getBroNodeSubQType())){
				n.setType(Node.getBroNodeSubQType());
				n.setSubQueryConds(n.getLeft().getSubQueryConds());
				n.getLeft().setSubQueryConds(null);
				n.setLhsRhs(n.getLeft().getLhsRhs());
				n.getLeft().setLhsRhs(null);
				n.getLeft().setType(Node.getAggrNodeType());
			}
			if(n.getRight()!=null && n.getRight().getType().equalsIgnoreCase(Node.getColRefType()) && !n.getLeft().getType().equalsIgnoreCase(Node.getColRefType())){
				Node temp=n.getLeft();
				n.setLeft(n.getRight());
				n.setRight(temp);
				if(n.getOperator().equals(">"))
					n.setOperator("<");
				else if(n.getOperator().equals("<"))
					n.setOperator(">");
				else if(n.getOperator().equals(">="))
					n.setOperator("<=");
				else if(n.getOperator().equals("<="))
					n.setOperator(">=");
			}


		}
		if(n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			modifyTreeForComapreSubQ(n.getLeft());
			modifyTreeForComapreSubQ(n.getRight());
		}
		/* the expression: n.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) 
		 * from the If condition below removed by mathew on 29 June 2016
		 */
		if(n.getType().equalsIgnoreCase(Node.getAllNodeType())|| 
				n.getType().equalsIgnoreCase(Node.getAnyNodeType())||	n.getType().equalsIgnoreCase(Node.getInNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getExistsNodeType()) || n.getType().equalsIgnoreCase(Node.getBroNodeSubQType())
				||n.getType().equalsIgnoreCase(Node.getNotExistsNodeType())){
			if(n.getSubQueryConds()!=null){
			for(Node subQ:n.getSubQueryConds()){
				modifyTreeForComapreSubQ(subQ);
			}
			}
		}


	}



/* Getting Foreign Key closure */
public static void foreignKeyClosure(QueryParser qParser) {
	/*
	for (String tableName : query.getFromTables().keySet()) {
		Table table = query.getFromTables().get(tableName);
		
		if (table.hasForeignKey()) {
		
			for (String fKeyName : table.getForeignKeys().keySet()) {
				ForeignKey fKey = table.getForeignKey(fKeyName);
				Vector<Column> fKeyColumns = fKey.getFKeyColumns();
				// Vector<Column> refKeyColumns =
				// fKey.getReferenceKeyColumns();

				for (Column fKeyColumn : fKeyColumns) {
		
					JoinClauseInfo foreignKey = new JoinClauseInfo(fKeyColumn, fKeyColumn.getReferenceColumn(),JoinClauseInfo.FKType);
					foreignKey.setConstant(fKeyName);
					this.foreignKeyVector.add(foreignKey);
				}
			}
		}
	}
	
	 */
	//Changed by Biplab the original code is commented out above
	Vector<Table> fkClosure = new Vector<Table>();
	LinkedList<Table> fkClosureQueue = new LinkedList<Table>();
	logger.log(Level.FINE,"FOREIGN KEY GRAPH : \n"+qParser.getTableMap().foreignKeyGraph);
	for (String tableName : qParser.getQuery().getFromTables().keySet()) {
		fkClosure.add( qParser.getTableMap().getTables().get(tableName.toUpperCase()));
		fkClosureQueue.addLast(qParser.getTableMap().getTables().get(tableName.toUpperCase()));
		logger.log(Level.FINE,"fkClosureQueue.add tables: \n "+qParser.getTableMap().getTables().get(tableName.toUpperCase()));
	}
	while(!fkClosureQueue.isEmpty())
	{
		Table table = fkClosureQueue.removeFirst();
		logger.log(Level.FINE,"fkClosureQueue Not Empty and contains table \n"+table.getTableName());
		for(Table tempTable : qParser.getTableMap().foreignKeyGraph.getAllVertex())
		{  
			Map<Table,Vector<ForeignKey>> neighbours = qParser.getTableMap().foreignKeyGraph.getNeighbours(tempTable);
			for(Table neighbourTable : neighbours.keySet())
			{
				if(neighbourTable.equals(table) && !fkClosure.contains(tempTable))
				{
					fkClosure.add(tempTable);
					fkClosureQueue.addLast(tempTable);
				}
			}
		}
	}
	Graph<Table, ForeignKey> tempForeignKeyGraph = qParser.getTableMap().foreignKeyGraph.createSubGraph();
	for(Table table : fkClosure)
		tempForeignKeyGraph.add(qParser.getTableMap().foreignKeyGraph, table);
	fkClosure = tempForeignKeyGraph.topSort();

	for(Table table : fkClosure)
		fkClosureQueue.addFirst(table);
	fkClosure.removeAllElements();
	fkClosure.addAll(fkClosureQueue);

	while(!fkClosureQueue.isEmpty())
	{
		Table table = fkClosureQueue.removeFirst();

		if(table.getForeignKeys() != null)
		{
			for (String fKeyName : table.getForeignKeys().keySet())
			{
				ForeignKey fKey = table.getForeignKey(fKeyName);
				qParser.getForeignKeyVectorModified().add(fKey);
				Vector<Column> fKeyColumns = fKey.getFKeyColumns();
				for (Column fKeyColumn : fKeyColumns)
				{
					JoinClauseInfo foreignKey = new JoinClauseInfo(fKeyColumn, fKeyColumn.getReferenceColumn(),JoinClauseInfo.FKType);
					foreignKey.setConstant(fKeyName);
					qParser.getForeignKeyVector().add(foreignKey);
				}
			}
		}
	}
//Changed by Biplab till here
	qParser.setForeignKeyVectorOriginal((Vector<JoinClauseInfo>) qParser.getForeignKeyVector().clone());

	// Now taking closure of foreign key conditions
	/*
	 * Altered closure algorithm so that the last foreign key in the chain is not added if it is nullable
	 * If the foreign key from this relation to other relations is nullale, 
	 * then this relation must not appear in the closure.
	 */

	//Commented out by Biplab
	/*for (int i = 0; i < this.foreignKeyVector.size(); i++) {
		JoinClauseInfo jci1 = this.foreignKeyVector.get(i);

		for (int j = i + 1; j < this.foreignKeyVector.size(); j++) {
			JoinClauseInfo jci2 = this.foreignKeyVector.get(j);
			if (jci1.getJoinTable2() == jci2.getJoinTable1()
					&& jci1.getJoinAttribute2() == jci2.getJoinAttribute1()) {
				//Check to see if the from column is nullable. If so, do not add the FK.
				//if(jci1.getJoinAttribute1().isNullable()){
				//	continue;
				//}
				JoinClauseInfo foreignKey = new JoinClauseInfo(jci1.getJoinAttribute1(), jci2.getJoinAttribute2(),JoinClauseInfo.FKType);
				if (!this.foreignKeyVector.contains(foreignKey)) {
					this.foreignKeyVector.add(foreignKey);
				}
			}
		}
	}*/
	//Commented out by Biplab till here

	// Convert the closure to type Vector<Node>
	foreignKeyInNode(qParser);
}

/*
 * Convert the foreignKeyClosure of type Vector<JoinClauseInfo> to a type of
 * Vector<Node>.
 */

public static void foreignKeyInNode(QueryParser qParser) {
	for (int i = 0; i < qParser.getForeignKeyVector().size(); i++) {
		Node left = new Node();
		left.setColumn(qParser.getForeignKeyVector().get(i).getJoinAttribute1());
		left.setTable(qParser.getForeignKeyVector().get(i).getJoinAttribute1()
				.getTable());
		left.setLeft(null);
		left.setRight(null);
		left.setOperator(null);
		left.setType(Node.getColRefType());

		Node right = new Node();
		right.setColumn(qParser.getForeignKeyVector().get(i).getJoinAttribute2());
		right.setTable(qParser.getForeignKeyVector().get(i).getJoinAttribute2()
				.getTable());
		right.setLeft(null);
		right.setRight(null);
		right.setOperator(null);
		right.setType(Node.getColRefType());

		Node refJoin = new Node();
		refJoin.setColumn(null);
		refJoin.setTable(null);
		refJoin.setLeft(left);
		refJoin.setRight(right);
		refJoin.setType(Node.getBaoNodeType());
		refJoin.setOperator("=");
		refJoin.setStrConst(qParser.getForeignKeyVector().get(i).getConstant());
		qParser.getForeignKeys().add(refJoin);
	}
}


/** @author mathew on 22 June 2016
 * Returns true if for every node n1 in key, there exists a node n2 in sourceLists
 * such that n1 and n2 represent the same column 
 * 
 */
public static boolean containsElements(List<Node> sourceList, Vector<Node> keys){
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

public static boolean containsElement(List<Node> sourceList, Node key){
		for(Node sourceNode:sourceList){
			if(key.getTableNameNo().equalsIgnoreCase(sourceNode.getTableNameNo())&&
					key.getColumn().getColumnName().equalsIgnoreCase(sourceNode.getColumn().getColumnName())){
				return true;				
			}
		}
			return false;

}


public static Set<Node> toSetOfNodes(List<Node> nodes){
	Set<Node> tempSet=new HashSet<Node>();
	if(nodes!=null){
		for(Node n:nodes)
			tempSet.add(n);
	}
	return tempSet;
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

/*
 * remove duplicates from a list of input selection/join conditions
 */
public static ArrayList<Node> removeDuplicates(ArrayList<Node> selectionConds) {
	// TODO Auto-generated method stub
	boolean removedFlag;
	do{
		removedFlag=false;
		if(selectionConds!=null){
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
		}
			
	} while(removedFlag);
	return selectionConds;
}

public static Node getNodeForCount(Vector<FromClauseElement> fle, QueryStructure qParser) {
	
	for(FromClauseElement f:fle){
		String fromTableName = f.getTableName();
		if (fromTableName != null && f.getTableNameNo()!= null &&!f.getTableNameNo().isEmpty()) {
			Table t=qParser.getTableMap().getTable(fromTableName);
			Column col = t.getColumn(0);
			if (col == null)
				return null;

			Node n = new Node();
			n.setTable(t);
			n.setTableAlias(t.getAliasName());
			n.setColumn(col);
			if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
				n.setTableNameNo(f.getTableNameNo());
			}//This may not be correct - FIXME Test and fix
			else{
				n.setTableNameNo(f.getTableName()+"1");
			}
			n.setType(Node.getColRefType());
			return n;
		} else if (f.getBag()!=null && !f.getBag().isEmpty()){
			for (int i = 0; i < f.getBag().size(); i++) {
				Node n = getNodeForCount(f.getBag().get(i), qParser);
				n.setType(Node.getColRefType());
				if (n != null) {
					return n;
				}
			}
		}
		else if(f.getSubQueryStructure()!=null){
			Node n=getNodeForCount(f.getSubQueryStructure().fromListElements,f.getSubQueryStructure());
			if(n!=null){
				n.setType(Node.getColRefType());
				return n;
			}
		}
	}
	return null;
}

public static Node getNodeForCount(FromClauseElement f, QueryStructure qParser) {
	
	String fromTableName = f.getTableName();
	if (fromTableName != null && qParser.getQuery().getFromTables().get(fromTableName.toUpperCase()) != null 
			&& f.getTableNameNo()!= null &&!f.getTableNameNo().isEmpty()) {
		
		Table t = qParser.getQuery().getFromTables().get(fromTableName.toUpperCase());
		Column col = t.getColumn(0);
		if (col == null)
			return null;

		Node n = new Node();
		n.setTable(t);
		n.setTableAlias(t.getAliasName());
		n.setColumn(col);
		if(f.getTableNameNo() != null && ! f.getTableNameNo().isEmpty()){
			n.setTableNameNo(f.getTableNameNo());
		}//This may not be correct - FIXME Test and fix
		else{
			n.setTableNameNo(f.getTableName()+"1");
		}
		n.setType(Node.getColRefType());
		return n;
	} else{
	for (int i = 0; i < f.getBag().size(); i++) {
			Node n = getNodeForCount(f.getBag().get(i), qParser);
			n.setType(Node.getColRefType());
			if (n != null) {
				return n;
			}
		}
	}
	return null;
}	

/*
 * Convert the foreignKeyClosure of type Vector<JoinClauseInfo> to a type of
 * Vector<Node>.
 */

public static void foreignKeyInNode(QueryStructure qParser) {
	for (int i = 0; i < qParser.getForeignKeyVector().size(); i++) {
		Node left = new Node();
		left.setColumn(qParser.getForeignKeyVector().get(i).getJoinAttribute1());
		left.setTable(qParser.getForeignKeyVector().get(i).getJoinAttribute1()
				.getTable());
		left.setLeft(null);
		left.setRight(null);
		left.setOperator(null);
		left.setType(Node.getColRefType());

		Node right = new Node();
		right.setColumn(qParser.getForeignKeyVector().get(i).getJoinAttribute2());
		right.setTable(qParser.getForeignKeyVector().get(i).getJoinAttribute2()
				.getTable());
		right.setLeft(null);
		right.setRight(null);
		right.setOperator(null);
		right.setType(Node.getColRefType());

		Node refJoin = new Node();
		refJoin.setColumn(null);
		refJoin.setTable(null);
		refJoin.setLeft(left);
		refJoin.setRight(right);
		refJoin.setType(Node.getBaoNodeType());
		refJoin.setOperator("=");
		refJoin.setStrConst(qParser.getForeignKeyVector().get(i).getConstant());
		qParser.getForeignKeys().add(refJoin);
	}
}

/** @author mathew
 * 
 * @param qStruct
 * 
 * Computes transitive closure of foreign key relationships over tables in the input QueryStructure
 */
public static void FKClosure(QueryStructure qStruct){
	Graph<Table, ForeignKey> foreignKeyGraph=qStruct.getTableMap().foreignKeyGraph;
	boolean updateFlag;
	logger.fine("New Foreign Key Closure algorithm");
	/**
	 *  computes the least fix point of the binary operation OP on the set of foreign key dependencies, where
	 *  OP(FK1, FK2)=FK3 s.t. FK3.foreignKeyColumns=FK1.foreignKeyColumns
	 *  and FK3.referencedKeyColumns=FK2.referencedKeyColumns, when FK1.referencedColumns=FK2.foreignKeyColumns, 
	 *  OP(FK1,FK2)=undefined, otherwise;
	 * 
	 */
	do{
		updateFlag=false;
		for(Table midTable:foreignKeyGraph.getAllVertex()){
			Map<String,ForeignKey> midTableFKMap=midTable.getForeignKeys();
			if(midTableFKMap==null||midTableFKMap.keySet().isEmpty()||midTableFKMap.values().isEmpty())
				continue;
			Vector<Column> midTablePKColumns=midTable.getPrimaryKey();
			//midTable is the intermediate Table to which FK1 refers to
			for(String midTableFKName:midTableFKMap.keySet()){
				//note that midTableForeignKey is the FK2 
				ForeignKey midTableForeignKey=midTableFKMap.get(midTableFKName);
				if(midTablePKColumns.containsAll(midTableForeignKey.getFKeyColumns())){
					Map<Table,Vector<ForeignKey>> leftTableFKMap=foreignKeyGraph.getNeighbours(midTable);
					if(leftTableFKMap==null)
						continue;
					//leftTable is the Table from which FK1 refers to midTable
					for(Table leftTable:leftTableFKMap.keySet()){
						Vector<ForeignKey> leftTableFKeys=leftTableFKMap.get(leftTable);
						if(leftTableFKeys==null)
							continue;
						//note that leftTableForeignKey is the FK1
						for(ForeignKey leftTableForeignKey:leftTableFKeys){							
							if(leftTableForeignKey.getReferenceTable().equals(midTable))
								//note that OP is defined only if the condition is true
							{
								String newFKname=null;
								boolean newFKnameSet=false;
								ForeignKey newFK=null;
								int seq_no=1;							
								for(int i=0;i<leftTableForeignKey.getFKeyColumns().size();i++){
									boolean breakCalled=false;
									for(int j=0;j<midTableForeignKey.getFKeyColumns().size();j++){
										if(leftTableForeignKey.getReferenceKeyColumns().get(i).equals(midTableForeignKey.getFKeyColumns().get(j)))
											//note that OP is defined only if the condition is true
										{
											String pottFKname=leftTable.getTableName()+"_"+leftTableForeignKey.getFKeyColumns().get(i)+
													"_"+midTableForeignKey.getReferenceTable().getTableName()+"_fkeyInf";
											if(!newFKnameSet){
												//the check below to ensure that pottFKname is not already defined
												if(leftTable.getForeignKeys().get(pottFKname)!=null){
													breakCalled=true;
													break;
												}
												newFKname=pottFKname;
												newFKnameSet=true;
											}
											//now adds the FK to the leftTable
											//note that Table.getForeignKey(String fkName) return a new FK if none exists with the name
											// fkName, otherwise it return an existing one, which which case the respective 
											// referencing and referenced columns are updated using the index seq_no
											if(newFKnameSet){
												newFK = leftTable.getForeignKey(newFKname);
												newFK.setReferenceTable(midTableForeignKey.getReferenceTable());
												newFK.addFKeyColumn(leftTableForeignKey.getFKeyColumns().get(i), seq_no);
												newFK.addReferenceKeyColumn(midTableForeignKey.getReferenceKeyColumns().get(j),seq_no);
												seq_no++;
												leftTable.addForeignKey(newFK);
											}
										}
									}
									if(breakCalled)
										break;
								}
								if(newFK!=null){
									foreignKeyGraph.add(midTableForeignKey.getReferenceTable(), leftTable, newFK);
									logger.info("New foreign key: "+newFKname+" added \n"+newFK.getFKTablename()+":"+newFK.getFKeyColumns()+"-->"+newFK.getReferenceTable().getTableName()+":"+newFK.getReferenceKeyColumns());
									updateFlag=true;

								}

							}
						}
						
					}
				}
			}
		}
	}while(updateFlag);
}

/* Getting Foreign Key closure */
public static void foreignKeyClosure(QueryStructure qParser) {
	FKClosure(qParser);//added by mathew to enable foreignKey Closure
	Vector<Table> fkClosure = new Vector<Table>();
	LinkedList<Table> fkClosureQueue = new LinkedList<Table>();
	logger.log(Level.FINE,"FOREIGN KEY GRAPH : \n"+qParser.getTableMap().foreignKeyGraph);		
	//for (String tableName : qParser.getQuery().getFromTables().keySet()) {	
	for (String tableName : qParser.getLstRelations()) {
		//fkClosure.add( qParser.getTableMap().getTables().get(tableName.toUpperCase()));
		//fkClosureQueue.addLast(qParser.getTableMap().getTables().get(tableName.toUpperCase()));
		fkClosure.add( qParser.getTableMap().getTables().get(tableName));// added by ram for mysql
		fkClosureQueue.addLast(qParser.getTableMap().getTables().get(tableName)); // added by ram for mysql
		//logger.log(Level.FINE,"fkClosureQueue.add tables: \n "+qParser.getTableMap().getTables().get(tableName.toUpperCase()));
		logger.log(Level.FINE,"fkClosureQueue.add tables: \n "+qParser.getTableMap().getTables().get(tableName)); // added by ram for mysql 
	}
	while(!fkClosureQueue.isEmpty())
	{
		Table table = fkClosureQueue.removeFirst();
		logger.log(Level.FINE,"fkClosureQueue Not Empty and contains table \n"+table.getTableName());
		for(Table tempTable : qParser.getTableMap().foreignKeyGraph.getAllVertex())
		{  
			Map<Table,Vector<ForeignKey>> neighbours = qParser.getTableMap().foreignKeyGraph.getNeighbours(tempTable);
			for(Table neighbourTable : neighbours.keySet())
			{
				if(neighbourTable.equals(table) && !fkClosure.contains(tempTable))
				{
					fkClosure.add(tempTable);
					fkClosureQueue.addLast(tempTable);
				}
			}
		}
	}	
	Graph<Table, ForeignKey> tempForeignKeyGraph = qParser.getTableMap().foreignKeyGraph.createSubGraph();
	for(Table table : fkClosure)
		tempForeignKeyGraph.add(qParser.getTableMap().foreignKeyGraph, table);
	fkClosure = tempForeignKeyGraph.topSort();

	for(Table table : fkClosure)
		fkClosureQueue.addFirst(table);
	fkClosure.removeAllElements();
	fkClosure.addAll(fkClosureQueue);

	while(!fkClosureQueue.isEmpty())
	{
		Table table = fkClosureQueue.removeFirst();

		if(table.getForeignKeys() != null)
		{
			for (String fKeyName : table.getForeignKeys().keySet())
			{
				ForeignKey fKey = table.getForeignKey(fKeyName);
				qParser.getForeignKeyVectorModified().add(fKey);
				Vector<Column> fKeyColumns = fKey.getFKeyColumns();
				for (Column fKeyColumn : fKeyColumns)
				{
					JoinClauseInfo foreignKey = new JoinClauseInfo(fKeyColumn, fKeyColumn.getReferenceColumn(),JoinClauseInfo.FKType);
					foreignKey.setConstant(fKeyName);
					qParser.getForeignKeyVector().add(foreignKey);
				}
			}
		}
	}
//Changed by Biplab till here
	qParser.setForeignKeyVectorOriginal((Vector<JoinClauseInfo>) qParser.getForeignKeyVector().clone());

	// Now taking closure of foreign key conditions
	/*
	 * Altered closure algorithm so that the last foreign key in the chain is not added if it is nullable
	 * If the foreign key from this relation to other relations is nullale, 
	 * then this relation must not appear in the closure.
	 */

	//Commented out by Biplab
	/*for (int i = 0; i < this.foreignKeyVector.size(); i++) {
		JoinClauseInfo jci1 = this.foreignKeyVector.get(i);

		for (int j = i + 1; j < this.foreignKeyVector.size(); j++) {
			JoinClauseInfo jci2 = this.foreignKeyVector.get(j);
			if (jci1.getJoinTable2() == jci2.getJoinTable1()
					&& jci1.getJoinAttribute2() == jci2.getJoinAttribute1()) {
				//Check to see if the from column is nullable. If so, do not add the FK.
				//if(jci1.getJoinAttribute1().isNullable()){
				//	continue;
				//}
				JoinClauseInfo foreignKey = new JoinClauseInfo(jci1.getJoinAttribute1(), jci2.getJoinAttribute2(),JoinClauseInfo.FKType);
				if (!this.foreignKeyVector.contains(foreignKey)) {
					this.foreignKeyVector.add(foreignKey);
				}
			}
		}
	}*/
	//Commented out by Biplab till here

	// Convert the closure to type Vector<Node>
	foreignKeyInNode(qParser);
}


public static Vector<Node> getAllProjectedColumns(Vector<FromClauseElement> visitedFLEs, QueryStructure qParser){
	Vector<Node> projectedColumns=new Vector<Node>();
	for(FromClauseElement fle:visitedFLEs){
		if(fle!=null && fle.getTableName()!=null){
			Table t=qParser.getTableMap().getTable(fle.getTableName());
			if(t!=null){
				Iterator colItr=t.getColumns().values().iterator();
				while(colItr.hasNext()){
					Column col=(Column)colItr.next();
					Node n = new Node();
					n.setColumn(t.getColumn(col.getColumnName()));
					n.setTable(col.getTable());
					n.setLeft(null);
					n.setRight(null);
					n.setOperator(null);
					n.setType(Node.getColRefType());
					n.setTableNameNo(fle.getTableNameNo());
					projectedColumns.add(n);
				}
			}
		}
		else if(fle!=null && fle.getBag()!=null && !fle.getBag().isEmpty()){
			projectedColumns.addAll(getAllProjectedColumns(fle.getBag(),qParser));				
		}
		else if(fle!=null && fle.getSubQueryStructure()!=null){
			projectedColumns.addAll(fle.getSubQueryStructure().getProjectedCols());
		}
	}
	return projectedColumns;
}



public static void copyDatabaseTables(Connection srcConn, Connection tarConn, String tableName) throws Exception{
	String selQuery="SELECT * FROM "+tableName;
	PreparedStatement selStmt=srcConn.prepareStatement(selQuery);
	ResultSet tableValues=selStmt.executeQuery();
	while(tableValues.next()){
		String insertHead="INSERT INTO "+tableName+"(";
		String insertTail=" VALUES(";
		ResultSetMetaData metaData=tableValues.getMetaData();
		for(int i=1;i<=metaData.getColumnCount();i++){
			if(i==1){
				insertHead+=metaData.getColumnName(i);
				insertTail+="?";
			}
			else if(metaData.getColumnName(i).contains("evaluationstatus")||metaData.getColumnName(i).contains("learning_mode"))
				continue;

			else{
				insertHead+=","+metaData.getColumnName(i);
				insertTail+=",?";
			}
		}
		String insertQuery= insertHead+")"+insertTail+")";		
		PreparedStatement insStmt=tarConn.prepareStatement(insertQuery);
		int k=0;
		for(int j=1;j<=metaData.getColumnCount();j++){
			if(metaData.getColumnName(j).contains("evaluationstatus")||metaData.getColumnName(j).contains("learning_mode"))
				continue;
			else if((metaData.getColumnType(j)==Types.NUMERIC)||(metaData.getColumnType(j)==Types.INTEGER)){
				insStmt.setInt(++k, tableValues.getInt(j));
			}
			else if(metaData.getColumnType(j)==Types.BOOLEAN){
				insStmt.setBoolean(++k, tableValues.getBoolean(j));
			}
			else if(metaData.getColumnType(j)==Types.DECIMAL||metaData.getColumnType(j)==Types.FLOAT){
				insStmt.setDouble(++k, tableValues.getDouble(j));
			}
			else if(metaData.getColumnType(j)==Types.TIMESTAMP){
				insStmt.setTimestamp(++k, tableValues.getTimestamp(j));
			}
			else if(metaData.getColumnType(j)==Types.TIME){
				insStmt.setTime(++k, tableValues.getTime(j));
			}
			else if(metaData.getColumnType(j)==Types.VARCHAR){
				insStmt.setString(++k, tableValues.getString(j));
			}
			else if(metaData.getColumnType(j)==Types.DATE)
			{
				insStmt.setDate(++k, tableValues.getDate(j));
			}
//			else if(metaData.getColumnLabel(j)=="learning_mode") {
//				if(tableValues.getBoolean(j)==false)
//					insStmt.setBoolean(j, false);
//				else
//					insStmt.setBoolean(j, true);
//			}
			else{
				insStmt.setString(++k, tableValues.getString(j));
			}
		}
		try{
		insStmt.executeUpdate();
		}
		catch(Exception e){
			logger.log(Level.WARNING,e.getMessage(), e);
		}
	}
}

public static void copyDatabaseTables(Connection srcConn, Connection tarConn) throws Exception{
	String srcQuery="SELECT table_name from information_schema.tables where table_schema='testing1'";
	PreparedStatement srcStmt= srcConn.prepareStatement(srcQuery);
	PreparedStatement tarStmt= tarConn.prepareStatement(srcQuery);
	ResultSet srcRes=srcStmt.executeQuery();
	ResultSet tarRes=tarStmt.executeQuery();
    
	List<String> srcTables=new ArrayList<String>();
	while(srcRes.next()){
		srcTables.add(srcRes.getString(1));
	}
	List<String> tarTables=new ArrayList<String>();
	while(tarRes.next()){
		tarTables.add(tarRes.getString(1));
	}
	for(String tableName:srcTables){
		if(!tarTables.contains(tableName))
			continue;
		String selQuery="SELECT * FROM "+tableName;
		PreparedStatement selStmt=srcConn.prepareStatement(selQuery);
		ResultSet tableValues=selStmt.executeQuery();
		while(tableValues.next()){
			String insertHead="INSERT INTO "+tableName+"(";
			String insertTail=" VALUES(";
			ResultSetMetaData metaData=tableValues.getMetaData();
			for(int i=1;i<metaData.getColumnCount();i++){
				if(i==1){
					insertHead+=metaData.getColumnName(i);
					insertTail+="?";
				}
				else{
					insertHead+=","+metaData.getColumnName(i);
					insertTail+=",?";
				}
			}
			String insertQuery= insertHead+")"+insertTail+")";				
			PreparedStatement insStmt=tarConn.prepareStatement(insertQuery);
			for(int j=1;j<metaData.getColumnCount();j++){
				if(metaData.getColumnType(j)==Types.NUMERIC||metaData.getColumnType(j)==Types.INTEGER){
					insStmt.setInt(j, tableValues.getInt(j));
				}
				else if(metaData.getColumnType(j)==Types.BOOLEAN){
					insStmt.setBoolean(j, tableValues.getBoolean(j));
				}
				else if(metaData.getColumnType(j)==Types.DECIMAL||metaData.getColumnType(j)==Types.FLOAT){
					insStmt.setDouble(j, tableValues.getDouble(j));
				}
				else if(metaData.getColumnType(j)==Types.TIMESTAMP){
					insStmt.setTimestamp(j, tableValues.getTimestamp(j));
				}
				else if(metaData.getColumnType(j)==Types.TIME){
					insStmt.setTime(j, tableValues.getTime(j));
				}
				else if(metaData.getColumnType(j)==Types.VARCHAR){
					insStmt.setString(j, tableValues.getString(j));
				}
				else if(metaData.getColumnType(j)==Types.DATE)
				{
					insStmt.setDate(j, tableValues.getDate(j));
				}
//				else if(metaData.getColumnLabel(j)=="learning_mode") {
//					if(tableValues.getBoolean(j)==false)
//						insStmt.setBoolean(j, false);
//					else
//						insStmt.setBoolean(j, true);
//				}
				else{
					insStmt.setString(j, tableValues.getString(j));
				}
			}
			try{
			insStmt.executeUpdate();
			}
			catch(Exception e){
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
}
}
