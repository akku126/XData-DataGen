/** @author mathew
 * 
 */
package parsing;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import parsing.Node;
import parsing.Util;

public abstract class ProcessSelectClauseAbstract {
	
	public static Logger logger=Logger.getLogger(ProcessSelectClauseAbstract.class.getName());
	
	/**	 method copied and adapted by mathew for query structure from parsing.Util.java
	 * 
	 * @param qStruct
	 */
	public static void modifyTreeForCompareSubQ(QueryStructure qStruct) {
		try{
			for (Node n: qStruct.allConds)   
				Util.modifyTreeForComapreSubQ(n);
		}catch(Exception e){
			logger.log(Level.SEVERE,"Error in modifyTreeForCompareSubQ : "+e.getMessage(),e);			
		}
	}
	
	/**
	 * @author mathew
	 * 
	 * displays the contents of a list of FromListElements, by hierachically traversing the iterating the list, 
	 * if any member fle represents a subquery, then it traverses into the fromListElements of the subquery,
	 * if any member fle represents a subjoin, then it traverses into it (by recursion on getTabs)
	 * 
	 * @param visitedFromListElements
	 */
	public static void display(Vector<FromClauseElement> visitedFromListElements) {
		for(FromClauseElement fle:visitedFromListElements){
			if(fle!=null && (fle.getTableName()!=null||fle.getTableNameNo()!=null))
				System.out.println(fle.toString());
			else if(fle!=null && fle.getSubQueryStructure()!=null){
				System.out.println(fle.toString());
				display(fle.getSubQueryStructure().getFromListElements());
			}
			else if(fle!=null && fle.getBag()!=null && !fle.getBag().isEmpty()){
				System.out.println(fle.toString());
				display(fle.getBag());				
			}

		}
	}
	
	/**
	 * @author mathew 
	 * 
	 * finds the root node of a query structure tree
	 * */
	
	public static QueryStructure findRootQueryStructure(QueryStructure qStruct){
		if(qStruct.parentQueryParser==null)
			return qStruct;
		else 
			return findRootQueryStructure(qStruct.parentQueryParser);
	}
	
	// list of abstract methods that is to be implemented by a child
	public abstract void ProcessSelect(PlainSelect plainSelect, boolean debug, QueryStructure qStruct) throws Exception;
	public abstract  boolean caseInWhereClause(Expression whereClause, Expression colExpression, QueryStructure qStruct, PlainSelect plainSelect) throws Exception;
	public abstract void processWhereClause(PlainSelect plainSelect, QueryStructure qStruct) throws Exception;
	public abstract void processGroupByList(PlainSelect plainSelect, QueryStructure qStruct) throws Exception;
	public abstract void processOrderByList(PlainSelect plainSelect, QueryStructure qStruct) throws Exception;
	public abstract void processFromClause(PlainSelect plainSelect, QueryStructure qStruct, Vector<Node> joinConditions) throws Exception;
	public abstract void processProjectionList(PlainSelect plainSelect, QueryStructure qStruct) throws Exception;
	public abstract void processFromListTable(net.sf.jsqlparser.schema.Table jsqlTable, FromClauseElement frmListElement, QueryStructure qStruct);
	public abstract void processFromListSubJoin(SubJoin subJoin, Vector<FromClauseElement> visitedFromListElements, Vector<Node> joinConditions,
			QueryStructure qStruct, PlainSelect plainSelect) throws Exception;
	public abstract Node processExpression(Object clause, Vector<FromClauseElement> fle,
			QueryStructure qStruct, PlainSelect plainSelect, String joinType) throws Exception;
	public abstract void processFromListSubSelect(SubSelect subSelect, QueryStructure subQueryParser,QueryStructure parentQueryParser) throws Exception;
	public abstract void processWhereSubSelect(SubSelect subSelect, QueryStructure subQueryParser,QueryStructure parentQueryParser) throws Exception;
	public abstract Node transformToAbsoluteTableNames(Node n, Vector<FromClauseElement> fleList, boolean aliasNameFound, QueryStructure qStruct) throws Exception;
	public abstract Node traverseAncestorsForAbsoluteNameTransformations(Node n,
			Vector<FromClauseElement> fleList, boolean aliasNameFound, QueryStructure qStruct);
	public abstract Node transformToAbsoluteNamesForAliasNameFoundSubquery(Node n,QueryStructure subQueryStructure);
}
