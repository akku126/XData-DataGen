package parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;

public class ProcessSelectClause {
	private static Logger logger = Logger.getLogger(ProcessSelectClause.class.getName());
	
	
	/** @author mathew on 1st october 2016 
	 * 
	 * 
	 * @param plainSelect
	 * @param debug
	 * @param qParser
	 * @throws Exception
	 * 
	 * processes the  plainSelect clause, calls the respective methods for processing its components/sub-clauses, which 
	 * in turn stores the objects that represent the sub-clauses  in the appropriate data structures in the 
	 * qParser object (3rd argument) . 
	 * 
	 */

	public static void ProcessSelect(PlainSelect plainSelect, boolean debug, parsing.QueryParser qParser) throws Exception {
		logger.info("processing select query"+plainSelect.toString());
		Vector<Node> joinConditions=new Vector<Node>();
		ProcessSelectClause.processFromClause(plainSelect,qParser,joinConditions);
		for(Node n:joinConditions){
			logger.info("joinCondition "+n);
		}
//		display(qParser.fromListElements);

		if(plainSelect.getDistinct()!=null){
			qParser.setIsDistinct(true);
		}
		ProcessSelectClause.processWhereClause(plainSelect,qParser);
		
		if(!joinConditions.isEmpty()){
			if(!qParser.allConds.isEmpty()) {
				Vector<Node> allCondsDups=(Vector<Node>) qParser.allConds.clone();
				Node NewCond = new Node();
				NewCond.setType(Node.getAndNodeType());

				NewCond.setLeft(ProcessResultSetNode.getHierarchyOfJoinNode(joinConditions));
				NewCond.setRight(qParser.allConds.get(0));
				allCondsDups.remove(qParser.allConds.get(0));
				allCondsDups.add(NewCond);
				qParser.allConds.removeAllElements();
				qParser.allConds.addAll(allCondsDups);
			}
			else {
				 qParser.allConds.addAll(joinConditions);
			}
		}

		
		ProcessResultSetNode.modifyTreeForCompareSubQ(qParser);		
		
		QueryParser.flattenAndSeparateAllConds(qParser);
				
		for(Conjunct conjunct:qParser.conjuncts)			
			conjunct.createEqClass();
		
		
		for(parsing.QueryParser qp: qParser.getFromClauseSubqueries()){//For From clause subqueries
			
			QueryParser.flattenAndSeparateAllConds(qp);
			for(Conjunct conjunct:qp.conjuncts){
				conjunct.createEqClass();
			}
		}
		for(parsing.QueryParser qp: qParser.getWhereClauseSubqueries()){//For Where clause subqueries
			
			QueryParser.flattenAndSeparateAllConds(qp);
			for(Conjunct conjunct:qp.conjuncts){
				conjunct.createEqClass();
			}
		}
		
		Util.foreignKeyClosure(qParser);
		if(qParser.isDeleteNode){
			return;
		}
		
		ProcessSelectClause.processProjectionList(plainSelect,qParser);
		ProcessSelectClause.processGroupByList(plainSelect,qParser);
		ProcessSelectClause.processHavingClause(plainSelect,qParser);
		ProcessSelectClause.processOrderByList(plainSelect,qParser);

		//System.out.println(qParser.toString());
	}

	private static void processWhereClause(PlainSelect plainSelect, QueryParser qParser) throws Exception{
		// TODO Auto-generated method stub
		Expression whereClauseExpression = plainSelect.getWhere();
		if(whereClauseExpression==null)
			return;
		ProcessResultSetNode.caseInWhereClause(whereClauseExpression,null,qParser);
		Node whereClause=ProcessSelectClause.processJoinExpression(whereClauseExpression,qParser.fromListElements, qParser,plainSelect);
		//System.out.println(" where clause "+whereClause);
		
		if( whereClause != null) 
			qParser.allConds.add(whereClause);
	}

	private static void processHavingClause(PlainSelect plainSelect, QueryParser qParser) throws Exception{
		// TODO Auto-generated method stub
		// get having clause
		Expression hc = plainSelect.getHaving();
		if(hc==null)
			return;
		Node havingClause=ProcessSelectClause.processJoinExpression(hc,qParser.fromListElements, qParser,plainSelect);
		qParser.setHavingClause(havingClause);
		logger.info(hc+" having clause "+havingClause);
	}

	
	/** @author mathew on 4th october 2016 
	 * 
	 * 
	 * @param plainSelect
	 * @param qParser
	 * @throws Exception
	 * 
	 * processes the group by list of plainSelect, and stores the elements as column nodes in groupByNodes list in 
	 * qParser object (2nd argument) . If the order by element is an alias or an unqualified column, it resolves the 
	 * table name and column name of the element.
	 * 
	 */

	private static void processGroupByList(PlainSelect plainSelect, QueryParser qParser) throws Exception{
		// TODO Auto-generated method stub
		if (plainSelect.getGroupByColumnReferences() == null||plainSelect.getGroupByColumnReferences().isEmpty()) 
			return;

		List<Expression> gbl = plainSelect.getGroupByColumnReferences();
		for (int i = 0; i < gbl.size(); i++) {
			Column gbc;
			Expression groupExpression=gbl.get(i);

			if (groupExpression instanceof Column){
				gbc = (Column)groupExpression;
			} else {
				continue;
			}
			
			Node groupByColumn=ProcessSelectClause.processJoinExpression(groupExpression,qParser.fromListElements, qParser,plainSelect);
			if(groupByColumn.getTableNameNo()==null||groupByColumn.getTableNameNo().isEmpty()){
				for(Node n:Util.getAllProjectedColumns(qParser.fromListElements, qParser)){
					if(n.getColumn().getColumnName().equalsIgnoreCase(groupByColumn.getColumn().getColumnName())){
						groupByColumn.setTable(n.getTable());
						groupByColumn.setTableNameNo(n.getTableNameNo());
						break;
					}
				}
			}
			if(groupByColumn.getTableNameNo()==null||groupByColumn.getTableNameNo().isEmpty()){
				List<SelectItem> projectedItems=plainSelect.getSelectItems();
				for(int j=0;j<projectedItems.size();j++){
					SelectItem projectedItem=projectedItems.get(j);
					if(projectedItem instanceof net.sf.jsqlparser.statement.select.SelectExpressionItem){
						SelectExpressionItem selExpItem=(net.sf.jsqlparser.statement.select.SelectExpressionItem)projectedItem;
						Expression e=selExpItem.getExpression();
						if(e instanceof net.sf.jsqlparser.expression.Parenthesis){
							net.sf.jsqlparser.expression.Parenthesis p=(net.sf.jsqlparser.expression.Parenthesis) e;
							e=p.getExpression();
						}
						if(selExpItem.getAlias()!=null){
							if(groupByColumn.getColumn().getColumnName().equalsIgnoreCase(selExpItem.getAlias().getName())){
								groupByColumn =ProcessSelectClause.processJoinExpression(e,qParser.fromListElements, qParser,plainSelect);
								logger.info(groupByColumn+" alias name resolved " +selExpItem.getAlias().getName());
								break;
							}
						}
					}
				}

			}
			qParser.groupByNodes.addElement(groupByColumn);
			logger.info(groupExpression.toString()+ " group by column "+groupByColumn);

		}

	}
	
	/** @author mathew on 1st october 2016 
	 * 
	 * 
	 * @param plainSelect
	 * @param qParser
	 * @throws Exception
	 * 
	 * processes the order by list of plainSelect, and stores the elements as column nodes in orderByElements list in 
	 * qParser object (2nd argument) . If the order by element is an alias or an unqualified column, it resolves the 
	 * table name and column name of the element.
	 * 
	 */

	private static void processOrderByList(PlainSelect plainSelect, QueryParser qParser) throws Exception {
		if (plainSelect.getOrderByElements() == null||plainSelect.getOrderByElements().isEmpty()) 
			return;

		List<OrderByElement> obl = plainSelect.getOrderByElements();
		for (int i = 0; i < obl.size(); i++) {
			Column obc;
			Expression orderExpression=obl.get(i).getExpression();

			if (orderExpression instanceof Column){
				obc = (Column)orderExpression;
			} else {
				continue;
			}

			Node orderByColumn=ProcessSelectClause.processJoinExpression(orderExpression,qParser.fromListElements, qParser,plainSelect);
			if(orderByColumn.getTableNameNo()==null||orderByColumn.getTableNameNo().isEmpty()){
				for(Node n:Util.getAllProjectedColumns(qParser.fromListElements, qParser)){
					if(n.getColumn().getColumnName().equalsIgnoreCase(orderByColumn.getColumn().getColumnName())){
						orderByColumn.setTable(n.getTable());
						orderByColumn.setTableNameNo(n.getTableNameNo());
						break;
					}
				}
			}
			if(orderByColumn.getTableNameNo()==null||orderByColumn.getTableNameNo().isEmpty()){
				List<SelectItem> projectedItems=plainSelect.getSelectItems();
				for(int j=0;j<projectedItems.size();j++){
					SelectItem projectedItem=projectedItems.get(j);
					if(projectedItem instanceof net.sf.jsqlparser.statement.select.SelectExpressionItem){
						SelectExpressionItem selExpItem=(net.sf.jsqlparser.statement.select.SelectExpressionItem)projectedItem;
						Expression e=selExpItem.getExpression();
						if(e instanceof net.sf.jsqlparser.expression.Parenthesis){
							net.sf.jsqlparser.expression.Parenthesis p=(net.sf.jsqlparser.expression.Parenthesis) e;
							e=p.getExpression();
						}
						if(selExpItem.getAlias()!=null){
							if(orderByColumn.getColumn().getColumnName().equalsIgnoreCase(selExpItem.getAlias().getName())){
								orderByColumn =ProcessSelectClause.processJoinExpression(e,qParser.fromListElements, qParser,plainSelect);
								logger.info(orderByColumn+" alias name resolved " +selExpItem.getAlias().getName());
								break;
							}
						}
					}
				}

			}
			qParser.orderByNodes.addElement(orderByColumn);
			logger.info(orderExpression.toString()+ " order by column "+orderByColumn);
		}

	}

	/** @author mathew on 3rd October 2016
	 * 
	 * @param plainSelect
	 * @param qParser
	 * @param joinConditions
	 * @throws Exception
	 * 
	 * Processes the From Clause of the first argument, plainSelect. From list elements in the from clause are extracted and stored 
	 * respecting their  hierarchy/nestedness in the fromListElements list in the qParser object (2nd argument), also the join conditions involved are 
	 * extracted and stored in the 3rd argument, joinConditions
	 * 
	 */
	private static void processFromClause(PlainSelect plainSelect, QueryParser qParser, Vector<Node> joinConditions) throws Exception{
		// TODO Auto-generated method stub
		FromItem firstFromItem=plainSelect.getFromItem();

		FromListElement leftFLE=null, rightFLE=null;
		
		
		if(firstFromItem instanceof net.sf.jsqlparser.schema.Table){
			net.sf.jsqlparser.schema.Table jsqlTable=(net.sf.jsqlparser.schema.Table)firstFromItem;
			leftFLE = new FromListElement();
			ProcessSelectClause.processFromListTable(jsqlTable, leftFLE, qParser);
			qParser.fromListElements.addElement(leftFLE);
		}
		else if(firstFromItem instanceof SubJoin){
			SubJoin subJoin=(SubJoin) firstFromItem;
			Join join=subJoin.getJoin();
			//System.out.println(" subJoinAlias "+subJoin.getAlias().getName()+" on Expression"+	join.getOnExpression());			
			Vector<FromListElement> tempElements=new Vector<FromListElement>();
			ProcessSelectClause.processFromListSubJoin(subJoin, tempElements, joinConditions, qParser,plainSelect);
			leftFLE=new FromListElement();
			if(subJoin.getAlias()!=null){
				leftFLE.setAliasName(subJoin.getAlias().getName());
			}
			leftFLE.setTabs(tempElements);
			qParser.fromListElements.addElement(leftFLE);
		}
		else if(firstFromItem instanceof SubSelect){
			SubSelect subSelect=(SubSelect) firstFromItem;
			SelectBody selBody=subSelect.getSelectBody();
			QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
			leftFLE=new FromListElement();
			leftFLE.setSubQueryParser(subQueryParser);
			if(subSelect.getAlias()!=null){
				leftFLE.setAliasName(subSelect.getAlias().getName());
			}
			qParser.fromListElements.addElement(leftFLE);					
			ProcessSelectClause.processFromListSubSelect(subSelect,subQueryParser,qParser);

		}
		
		if(plainSelect.getJoins()!=null && !plainSelect.getJoins().isEmpty()){
			for(Join join:plainSelect.getJoins()){

				FromItem fromItem=join.getRightItem();
				if(fromItem instanceof net.sf.jsqlparser.schema.Table){
					net.sf.jsqlparser.schema.Table jsqlTable=(net.sf.jsqlparser.schema.Table)fromItem;
					rightFLE=new FromListElement();
					ProcessSelectClause.processFromListTable(jsqlTable, rightFLE, qParser);
					qParser.fromListElements.addElement(rightFLE);
				}
				else if(fromItem instanceof SubJoin){
					SubJoin subJoin=(SubJoin) fromItem;
					Vector<FromListElement> tempElements=new Vector<FromListElement>();
					ProcessSelectClause.processFromListSubJoin(subJoin, tempElements, joinConditions, qParser,plainSelect);
					rightFLE=new FromListElement();
					if(subJoin.getAlias()!=null){
						rightFLE.setAliasName(subJoin.getAlias().getName());
					}				
					rightFLE.setTabs(tempElements);
					qParser.fromListElements.addElement(rightFLE);

				}
				else if(fromItem instanceof SubSelect){
					SubSelect subSelect=(SubSelect) fromItem;					
					QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
					rightFLE=new FromListElement();
					rightFLE.setSubQueryParser(subQueryParser);
					if(subSelect.getAlias()!=null){
						rightFLE.setAliasName(subSelect.getAlias().getName());
					}
					qParser.fromListElements.addElement(rightFLE);
					
					ProcessSelectClause.processFromListSubSelect(subSelect,subQueryParser,qParser);					

				}
				Expression e=join.getOnExpression();
				if(e!=null){
					Node joinCondition=ProcessSelectClause.processJoinExpression(e,qParser.fromListElements, qParser,plainSelect);
					joinConditions.add(joinCondition);
				}
				leftFLE=rightFLE;//reset leftFLE to the previously visited FLE
			}
		}
	}

	/** @author mathew on 1st october 2016 
	 * 
	 * 
	 * @param plainSelect
	 * @param qParser
	 * @throws Exception
	 * 
	 * processes the projection list of plainSelect, for each element of the project list checks for the various possibilities - 
	 * column expression , or an all column object. In case of the former, check if there is a  case expression is present,
	 * in which case the parser's case condition map is updated with the respective objects from the case condition
	 * 
	 */
	private static void processProjectionList(PlainSelect plainSelect, QueryParser qParser) throws Exception{
		// TODO Auto-generated method stub
		Vector<CaseCondition> caseConditionsVector = new Vector<CaseCondition>();
		
		List<SelectItem> projectedItems=plainSelect.getSelectItems();
		for(int i=0;i<projectedItems.size();i++){
			SelectItem projectedItem=projectedItems.get(i);
			if(projectedItem instanceof net.sf.jsqlparser.statement.select.AllColumns){
				for(Node n:Util.getAllProjectedColumns(qParser.fromListElements, qParser)){
						logger.info(" all column, select all columns... "+n);
						qParser.projectedCols.add(n);
				}
			}
			else if(projectedItem instanceof net.sf.jsqlparser.statement.select.SelectExpressionItem){
				SelectExpressionItem selExpItem=(net.sf.jsqlparser.statement.select.SelectExpressionItem)projectedItem;
				Expression e=selExpItem.getExpression();
				if(selExpItem.getAlias()!=null){
					logger.info(" alias present " +selExpItem.getAlias().getName());
				}
					
				if(e instanceof net.sf.jsqlparser.expression.Parenthesis){
					net.sf.jsqlparser.expression.Parenthesis p=(net.sf.jsqlparser.expression.Parenthesis) e;
					Expression exp=p.getExpression();
					if(exp instanceof net.sf.jsqlparser.expression.CaseExpression)
						e=exp;
				}
				if(e instanceof net.sf.jsqlparser.expression.CaseExpression){
					
					 List<Expression> whenClauses = ((CaseExpression) e).getWhenClauses();
					 for(int j=0;j < whenClauses.size();j++ ){
						
						CaseCondition cC = new CaseCondition();
						Node n = WhereClauseVectorJSQL.processWhereClauseVector(((WhenClause)((CaseExpression) e).getWhenClauses().get(j)).getWhenExpression(), qParser.fromListElements, qParser,plainSelect);
						cC.setCaseConditionNode(n);
						cC.setCaseCondition(n.toString());
					    cC.setConstantValue(((WhenClause)((CaseExpression) e).getWhenClauses().get(j)).getThenExpression().toString());
					    caseConditionsVector.add(cC);
					   // qParser.getCaseConditions().add(cC);
					 }
					 //Add the else clause if present as the last item
					 if(((CaseExpression) e).getElseExpression() != null){
						CaseCondition cC = new CaseCondition();
						//cC.setCaseConditionNode(n);
						cC.setCaseCondition("else");
					    cC.setConstantValue(((CaseExpression) e).getElseExpression().toString());
					    caseConditionsVector.add(cC);
					 }
					 //Add Case conditions to queryparser
				   qParser.getCaseConditionMap().put(1,caseConditionsVector);
				}
				else{
					Node projectedColumn=ProcessSelectClause.processJoinExpression(e,qParser.fromListElements, qParser,plainSelect);
					if(projectedColumn.getAgg()!=null&&selExpItem.getAlias()!=null){
						projectedColumn.getAgg().setAggAliasName(selExpItem.getAlias().getName());
					}
					else if(selExpItem.getAlias()!=null){
						projectedColumn.setAliasName(selExpItem.getAlias().getName());
					}
					qParser.projectedCols.add(projectedColumn);
					logger.info("Select Expression"+projectedItem.toString()+ " "+projectedColumn);

					if(qParser.setOperator==null||qParser.setOperator.isEmpty()){
						if(projectedColumn.getTableNameNo()==null||projectedColumn.getTableNameNo().isEmpty()){
							logger.info(" Column name could not be resolved, query parsing failed, exception thrown, query: "+plainSelect.toString());
							throw new Exception(" Column name could not be resolved, query parsing failed, exception thrown");
						}
					}
					

				}
			}
		}
	}

	public static void display(Vector<FromListElement> visitedFromListElements) {
		for(FromListElement fle:visitedFromListElements){
			if(fle!=null && (fle.getTableName()!=null||fle.getTableNameNo()!=null))
				System.out.println(fle.toString());
			else if(fle!=null && fle.getSubQueryParser()!=null){
				System.out.println(fle.toString());
				display(fle.getSubQueryParser().getFromListElements());
			}
			else if(fle!=null && fle.getTabs()!=null && !fle.getTabs().isEmpty()){
				System.out.println(fle.toString());
				display(fle.getTabs());				
			}
	
		}
	}
	
	public static void processFromListTable(net.sf.jsqlparser.schema.Table jsqlTable, FromListElement frmListElement, QueryParser qParser){
		String tableName = jsqlTable.getFullyQualifiedName().toUpperCase();// getWholeTableName();
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
		
		frmListElement.setAliasName(aliasName);
		frmListElement.setTableName(tableName);
		frmListElement.setTableNameNo(tableNameNo);
		frmListElement.setTabs(null);	
		logger.info("Table added"+frmListElement);
	}

	public static void processFromListSubJoin(SubJoin subJoin, Vector<FromListElement> visitedFromListElements, Vector<Node> joinConditions,
			QueryParser qParser, PlainSelect plainSelect) throws Exception{
		logger.info("processing subjoin"+ subJoin.toString());

		FromItem leftFromItem=subJoin.getLeft();
		FromListElement leftFLE=null, rightFLE=null;

		//JSQL parser restricts the left from item of a sub join  to be a Table
		if(leftFromItem instanceof net.sf.jsqlparser.schema.Table){
			net.sf.jsqlparser.schema.Table jsqlTable=(net.sf.jsqlparser.schema.Table)leftFromItem;
			leftFLE=new FromListElement();
			ProcessSelectClause.processFromListTable(jsqlTable, leftFLE, qParser);
			//logger.info(leftFLE.toString());
			visitedFromListElements.add(leftFLE);
		}
		else if(leftFromItem instanceof SubJoin){
			SubJoin leftSubJoin=(SubJoin) leftFromItem;
			Vector<FromListElement> tempElements=new Vector<FromListElement>();
			ProcessSelectClause.processFromListSubJoin(leftSubJoin, tempElements, joinConditions, qParser,plainSelect);
			leftFLE=new FromListElement();
			if(leftSubJoin.getAlias()!=null){
				leftFLE.setAliasName(leftSubJoin.getAlias().getName());
			}
			leftFLE.setTabs(tempElements);
			visitedFromListElements.add(leftFLE);
		}
		else if(leftFromItem instanceof SubSelect){
			SubSelect subSelect=(SubSelect) leftFromItem;					
			QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
			leftFLE=new FromListElement();
			leftFLE.setSubQueryParser(subQueryParser);
			if(subSelect.getAlias()!=null){
				leftFLE.setAliasName(subSelect.getAlias().getName());
			}
			qParser.fromListElements.addElement(leftFLE);
			
			ProcessSelectClause.processFromListSubSelect(subSelect,subQueryParser,qParser);					

		}
		FromItem rightFromItem=subJoin.getJoin().getRightItem();
		if(rightFromItem instanceof net.sf.jsqlparser.schema.Table){
			net.sf.jsqlparser.schema.Table jsqlTable=(net.sf.jsqlparser.schema.Table)rightFromItem;
			rightFLE=new FromListElement();
			ProcessSelectClause.processFromListTable(jsqlTable, rightFLE, qParser);
			//logger.info(rightFLE.toString());
			visitedFromListElements.add(rightFLE);
		}
		else if(rightFromItem instanceof SubJoin){
			SubJoin rightSubJoin=(SubJoin) rightFromItem;
			Vector<FromListElement> tempElements=new Vector<FromListElement>();
			ProcessSelectClause.processFromListSubJoin(rightSubJoin, tempElements, joinConditions, qParser,plainSelect);
			rightFLE=new FromListElement();
			if(rightSubJoin.getAlias()!=null){
				rightFLE.setAliasName(rightSubJoin.getAlias().getName());
			}
			rightFLE.setTabs(tempElements);
			visitedFromListElements.add(rightFLE);
		}
		else if(rightFromItem instanceof SubSelect){
			SubSelect subSelect=(SubSelect) rightFromItem;					
			QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
			rightFLE=new FromListElement();
			rightFLE.setSubQueryParser(subQueryParser);
			if(subSelect.getAlias()!=null){
				rightFLE.setAliasName(subSelect.getAlias().getName());
			}
			qParser.fromListElements.addElement(rightFLE);
			
			ProcessSelectClause.processFromListSubSelect(subSelect,subQueryParser,qParser);					

		}
		Join join=subJoin.getJoin();
		Expression e=join.getOnExpression();
		if(e!=null){
			Node joinCondition=processJoinExpression(e,visitedFromListElements, qParser,plainSelect);
			joinConditions.add(joinCondition);
		}
	}

	
	public static Node processJoinExpression(Expression e, Vector<FromListElement> visitedElements, QueryParser qParser, PlainSelect plainSelect) throws Exception{
		Node n=WhereClauseVectorJSQL.processWhereClauseVector(e, visitedElements , qParser, plainSelect);
		return n;
	}
	
	public static void processFromListSubSelect(SubSelect subSelect, QueryParser subQueryParser,QueryParser parentQueryParser) throws Exception {
		// TODO Auto-generated method stub
		logger.info(" Processing subselect, selbody:"+subSelect.getSelectBody().toString());
		if(subSelect.getAlias()!=null)
		logger.info(" subselect alias "+subSelect.getAlias().getName());

		parentQueryParser.getFromClauseSubqueries().add(subQueryParser);
		subQueryParser.parentQueryParser=parentQueryParser;
		subQueryParser.setQuery(new Query("q2",subSelect.getSelectBody().toString()));
		subQueryParser.getQuery().setRepeatedRelationCount(parentQueryParser.getQuery().getRepeatedRelationCount());
		subQueryParser.parseQueryJSQL("q2", subSelect.getSelectBody().toString(), true);
		
	}
	
	
	public static void processWhereSubSelect(SubSelect subSelect, QueryParser subQueryParser,QueryParser parentQueryParser) throws Exception {
		// TODO Auto-generated method stub
		
		logger.info(" Processing subselect, selbody:"+subSelect.getSelectBody().toString());

		parentQueryParser.getWhereClauseSubqueries().add(subQueryParser);
		subQueryParser.parentQueryParser=parentQueryParser;
		subQueryParser.setQuery(new Query("q2",subSelect.getSelectBody().toString()));
		subQueryParser.getQuery().setRepeatedRelationCount(parentQueryParser.getQuery().getRepeatedRelationCount());
		subQueryParser.parseQueryJSQL("q2", subSelect.getSelectBody().toString(), true);
		
	}
}
