package parsing;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.impl.sql.compile.FromBaseTable;

import parsing.Table;
import parsing.JoinTreeNode;
import parsing.FromListElement;

import net.sf.jsqlparser.statement.select.*;

public class OperateOnBaseTable {

	private static Logger logger = Logger.getLogger(OperateOnBaseTable.class.getName());
	
	
	public static FromListElement OperateOnBaseTableJSQL(FromItem node,
			boolean isJoinTable, String subqueryAlias, JoinTreeNode jtn, QueryParser qParser, boolean isFromSubQuery, boolean isWhereSubQuery) throws Exception {
	try{
		net.sf.jsqlparser.schema.Table tab = (net.sf.jsqlparser.schema.Table)node; 
		String tableName = tab.getFullyQualifiedName().toUpperCase();// getWholeTableName();
		Table table = qParser.getTableMap().getTable(tableName.toUpperCase());
		
		String aliasName = "";
		if (tab.getAlias() == null) {
			aliasName = tableName;
		} else {
			aliasName = tab.getAlias().getName().toUpperCase();// getAlias();
		}
	//	Jointree processing
		jtn.setNodeType(JoinTreeNode.relation);
		jtn.setLeft(null);
		jtn.setRight(null);
		jtn.setRelName(tableName);
		jtn.setOc(0);//setting output cardinality
		jtn.setNodeAlias(aliasName);
		
		//	FIXME: Mahesh some bug while adding baseRelation
		Util.addFromTable(table,qParser);

		qParser.getQuery().putBaseRelation(aliasName, tableName);
		
		if (qParser.getQuery().getRepeatedRelationCount().get(tableName) != null) {
			qParser.getQuery().putRepeatedRelationCount(tableName, qParser.getQuery()
					.getRepeatedRelationCount().get(tableName) + 1);
		} else {
			qParser.getQuery().putRepeatedRelationCount(tableName, 1);
		}
		
		qParser.getQuery().putCurrentIndexCount(tableName, qParser.getQuery().getRepeatedRelationCount()
				.get(tableName.toUpperCase()) - 1);
		
		FromListElement temp = new FromListElement();
		temp.setAliasName(aliasName);
		temp.setTableName(tableName);
		String tableNameNo = tableName
				+ qParser.getQuery().getRepeatedRelationCount().get(tableName);
		temp.setTableNameNo(tableNameNo);
		temp.setTabs(null);
		jtn.setTableNameNo(tableNameNo);
		
		Util.updateTableOccurrences(isFromSubQuery, isWhereSubQuery, tableNameNo, qParser);
		
		if (qParser.getQuery().getCurrentIndex().get(tableName) == null)
			qParser.getQuery().putCurrentIndex(tableName, 0);
		return temp;
	}catch(Exception e){
		logger.log(Level.SEVERE,"Failed while Operating On Base Table : "+e.getMessage(),e);
		throw e;
	}
	
	}
	/**
	 * 
	 * @param node
	 * @param isJoinTable
	 * @param subqueryAlias
	 * @param jtn
	 * @param qParser
	 * @param isFromSubQuery
	 * @param isWhereSubQuery
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public static FromListElement OperateOnBaseTable(FromBaseTable node,
			boolean isJoinTable, String subqueryAlias, JoinTreeNode jtn, QueryParser qParser, boolean isFromSubQuery, boolean isWhereSubQuery) throws Exception {

		String tableName = node.getBaseTableName();
		Table table = qParser.getTableMap().getTable(tableName);
		
		
		String aliasName = "";
		if (node.getCorrelationName() == null) {
			aliasName = tableName;
		} else {
			aliasName = node.getCorrelationName();
		}
		
	//	Jointree processing
		jtn.setNodeType(JoinTreeNode.relation);
		jtn.setLeft(null);
		jtn.setRight(null);
		jtn.setRelName(tableName);
		jtn.setOc(0);//setting output cardinality
		jtn.setNodeAlias(aliasName);
		
		//	FIXME: Mahesh some bug while adding baseRelation
		Util.addFromTable(table,qParser);

		if (aliasName != null) {
			qParser.getQuery().putBaseRelation(aliasName, tableName);
		} else {
			qParser.getQuery().putBaseRelation(tableName, tableName);
		}
		
		if (qParser.getQuery().getRepeatedRelationCount().get(tableName.toUpperCase()) != null) {
			qParser.getQuery().putRepeatedRelationCount(tableName.toUpperCase(), qParser.getQuery()
					.getRepeatedRelationCount().get(tableName.toUpperCase()) + 1);
			//	query.putTableNameToQueryIndex(tableName +  (query.getRepeatedRelationCount().get(tableName)), queryType, queryIndex);
		} else {
			qParser.getQuery().putRepeatedRelationCount(tableName.toUpperCase(), 1);
			//query.putTableNameToQueryIndex(tableName +  "1", queryType, queryIndex);
		}
		
		qParser.getQuery().putCurrentIndexCount(tableName.toUpperCase(), qParser.getQuery().getRepeatedRelationCount()
				.get(tableName.toUpperCase()) - 1);
		
		FromListElement temp = new FromListElement();
		if (node.getCorrelationName() != null) {
			temp.setAliasName(node.getCorrelationName());
		} else {
			temp.setAliasName(node.getBaseTableName());
		}
		temp.setTableName(node.getBaseTableName());
		String tableNameNo = tableName
				+ qParser.getQuery().getRepeatedRelationCount().get(tableName.toUpperCase());
		temp.setTableNameNo(tableNameNo);
		temp.setTabs(null);
		jtn.setTableNameNo(tableNameNo);
		
		Util.updateTableOccurrences(isFromSubQuery, isWhereSubQuery, tableNameNo, qParser);
		
		if (qParser.getQuery().getCurrentIndex().get(tableName) == null)
			qParser.getQuery().putCurrentIndex(tableName, 0);
		
		return temp;
	}
}
