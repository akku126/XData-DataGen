package parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.impl.sql.compile.FromBaseTable;

import parsing.Table;
import parsing.JoinTreeNode;
import parsing.FromListElement;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.*;

public class OperateOnBaseTable {

	private static Logger logger = Logger.getLogger(OperateOnBaseTable.class.getName());
		
	public static FromListElement OperateOnBaseTableJSQL(net.sf.jsqlparser.schema.Table jsqlTable, JoinTreeNode jtn, QueryParser qParser, boolean isFromSubQuery, boolean isWhereSubQuery) throws Exception {
	try{
		String tableName = jsqlTable.getFullyQualifiedName().toUpperCase();// getWholeTableName();

		Table table = qParser.getTableMap().getTable(tableName.toUpperCase());

		String aliasName = "";
		if (jsqlTable.getAlias() == null) {
			aliasName = tableName;
		} else {
			aliasName = jsqlTable.getAlias().getName().toUpperCase();// getAlias();
		}
		if (qParser.getQuery().getRepeatedRelationCount().get(tableName) != null) {
			qParser.getQuery().putRepeatedRelationCount(tableName, qParser.getQuery()
					.getRepeatedRelationCount().get(tableName) + 1);
		} else {
			qParser.getQuery().putRepeatedRelationCount(tableName, 1);
		}
		String tableNameNo = tableName
				+ qParser.getQuery().getRepeatedRelationCount().get(tableName);

		
	//	Jointree processing
		jtn.setNodeType(JoinTreeNode.relation);
		jtn.setLeft(null);
		jtn.setRight(null);
		jtn.setRelName(tableName);
		jtn.setOc(0);//setting output cardinality
		jtn.setNodeAlias(aliasName);
		jtn.setTableNameNo(tableNameNo);
		
		//	FIXME: Mahesh some bug while adding baseRelation
		qParser.getQuery().addFromTable(table);
		qParser.getQuery().putBaseRelation(aliasName, tableName);				
		qParser.getQuery().putCurrentIndexCount(tableName, qParser.getQuery().getRepeatedRelationCount()
				.get(tableName.toUpperCase()) - 1);
		//what is the need for this currentIndexCount member if 
		//its value is obtained by just computed by decrementing RepeatedRelationCount member
		if (qParser.getQuery().getCurrentIndex().get(tableName) == null)
			qParser.getQuery().putCurrentIndex(tableName, 0);

		
		FromListElement frmListElement = new FromListElement();
		frmListElement.setAliasName(aliasName);
		frmListElement.setTableName(tableName);
		frmListElement.setTableNameNo(tableNameNo);
		frmListElement.setTabs(null);
		
		Util.updateTableOccurrences(isFromSubQuery, isWhereSubQuery, tableNameNo, qParser);
		
		return frmListElement;
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
		qParser.getQuery().addFromTable(table);

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
//		String tableNameNo = tableName
//				+ qParser.getQuery().getRepeatedRelationCount().get(tableName);
//		
		temp.setTableNameNo(tableNameNo);
		temp.setTabs(null);
		jtn.setTableNameNo(tableNameNo);
		
		Util.updateTableOccurrences(isFromSubQuery, isWhereSubQuery, tableNameNo, qParser);
		
		if (qParser.getQuery().getCurrentIndex().get(tableName) == null)
			qParser.getQuery().putCurrentIndex(tableName, 0);
		
		return temp;
	}
	

}
