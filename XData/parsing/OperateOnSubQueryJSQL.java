package parsing;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.ExceptOp;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.IntersectOp;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperation;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.UnionOp;

public class OperateOnSubQueryJSQL {

	private static Logger logger = Logger.getLogger(OperateOnSubQueryJSQL.class.getName());
	public static FromListElement OperateOnSubquery(SubSelect subquery,Vector<Node> allConds, JoinTreeNode jtn,boolean fromSubquery, boolean whereSubquery, parsing.QueryParser qParser) throws Exception {
		try{
		String aliasName = "";
		if(subquery.getAlias() != null){
			aliasName = subquery.getAlias().getName().toUpperCase();
		}
		boolean isSetOp = false;
		FromListElement sqa = new FromListElement();
		Vector<FromListElement> t = new Vector<FromListElement>();
		Vector<Node> tempProjectedCols = new Vector<Node>();
		//ValueNode sqWhereClause=null;
		Expression sqWhereClause=null;

		Node whereClausePred = new Node();
		//	mahesh: create new query parser for from clause subquery
		parsing.QueryParser fromClause=new parsing.QueryParser(qParser.getTableMap());
		fromClause.setQuery(qParser.getQuery());
		parsing.QueryParser whereClause=new parsing.QueryParser(qParser.getTableMap());
		whereClause.setQuery(qParser.getQuery());
		int queryType = 0;
		if(fromSubquery) queryType = 1;
		if(whereSubquery) queryType = 2;

		//If it is instance of SetOperationList - UNION,EXCEPT OR INTERSECT
		if((subquery.getSelectBody()) instanceof SetOperationList){
			isSetOp = true;
			SetOperationList setOpList = (SetOperationList)( subquery.getSelectBody());
			QueryParser qp = new QueryParser(qParser.getTableMap());
			//Get common SET Operation
			SetOperation setOperation =  setOpList.getOperations().get(0);
	
				//Test in different scenarios - joins in SET  Op and test
				//Get the select list to check it has select statement or nested SET operation
				qp.parseQueriesForSetOp(setOpList,true); 
				QueryParser newSetQp = new QueryParser(qParser.getTableMap());
				if(fromSubquery){
					qParser.getFromClauseSubqueries().add(qp);
					qParser.getSubQueryNames().put(aliasName, qParser.getFromClauseSubqueries().size()-1);
					if(qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1)!= null && 
							qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).setOperator != null
							 && qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).setOperator.length() > 0){
						
								newSetQp = qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1);
								t.add(newSetQp.getRightQuery().getQueryAliases());
								t.add(newSetQp.getLeftQuery().getQueryAliases());
								
						
								qParser.getQuery().getFromTables().putAll(newSetQp.getRightQuery().getQuery().getFromTables());
								qParser.getQuery().getFromTables().putAll(newSetQp.getLeftQuery().getQuery().getFromTables());
								
								if(qp.setOperator.equalsIgnoreCase("UNION")) {
									qParser.getQuery().getRepeatedRelationCount().putAll(newSetQp.getRightQuery().getQuery().getRepeatedRelationCount());
									qParser.getQuery().getRepeatedRelationCount().putAll(newSetQp.getLeftQuery().getQuery().getRepeatedRelationCount());
									
									qParser.setTableNames(newSetQp.getRightQuery().getTableNames());
									qParser.setTableNames(newSetQp.getLeftQuery().getTableNames());
									
									qParser.getQuery().getCurrentIndexCount().putAll(newSetQp.getRightQuery().getQuery().getCurrentIndexCount());
									qParser.getQuery().getCurrentIndexCount().putAll(newSetQp.getLeftQuery().getQuery().getCurrentIndexCount());
									
									qParser.getQueryAliases().addTabs(newSetQp.getRightQuery().getQueryAliases());
									qParser.getQueryAliases().addTabs(newSetQp.getLeftQuery().getQueryAliases());
									if(newSetQp.getQueryAliases()  == null){
										newSetQp.queryAliases = new FromListElement();
									}
										newSetQp.getQueryAliases().addTabs(qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getLeftQuery().getQueryAliases());
										newSetQp.getQueryAliases().addTabs(qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getRightQuery().getQueryAliases());
									
									qParser.getProjectedCols().addAll(newSetQp.getRightQuery().getProjectedCols());
									qParser.getProjectedCols().addAll(newSetQp.getLeftQuery().getProjectedCols());
									
									newSetQp.getProjectedCols().addAll(qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getLeftQuery().getProjectedCols());
									newSetQp.getProjectedCols().addAll(qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getRightQuery().getProjectedCols());

									
								} else if (qp.setOperator.equalsIgnoreCase("INTERSECT")) {
									qParser.getQuery().getRepeatedRelationCount().putAll(newSetQp.getLeftQuery().getQuery().getRepeatedRelationCount());
									qParser.setTableNames(newSetQp.getLeftQuery().getTableNames());
									qParser.getQuery().getCurrentIndexCount().putAll(newSetQp.getLeftQuery().getQuery().getCurrentIndexCount());
									
									qParser.getQueryAliases().addTabs(newSetQp.getLeftQuery().getQueryAliases());
									qParser.getProjectedCols().addAll(newSetQp.getLeftQuery().getProjectedCols());
									newSetQp.getProjectedCols().addAll(qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getLeftQuery().getProjectedCols());
									 
									if(newSetQp.getQueryAliases() == null){
										newSetQp.queryAliases = new FromListElement();
									}
										newSetQp.getQueryAliases().addTabs(newSetQp.getLeftQuery().getQueryAliases());
									
									//qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getQuery().getRepeatedRelationCount());
								//	cvcIntersect.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
									
								}else if (qp.setOperator.equalsIgnoreCase("EXCEPT")) {
									
									qParser.getQuery().getRepeatedRelationCount().putAll(newSetQp.getLeftQuery().getQuery().getRepeatedRelationCount());
									qParser.setTableNames(newSetQp.getLeftQuery().getTableNames());
									qParser.getQuery().getCurrentIndexCount().putAll(newSetQp.getLeftQuery().getQuery().getCurrentIndexCount());
									
									qParser.getQueryAliases().addTabs(newSetQp.getLeftQuery().getQueryAliases());
									qParser.getProjectedCols().addAll(newSetQp.getLeftQuery().getProjectedCols());
									
									newSetQp.getProjectedCols().addAll(newSetQp.getLeftQuery().getProjectedCols());
									if(newSetQp.getQueryAliases() == null){
										newSetQp.queryAliases = new FromListElement();
									}
									newSetQp.getQueryAliases().addTabs(newSetQp.getLeftQuery().getQueryAliases());
									
									
									//qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getQuery().getRepeatedRelationCount());
									//cvcExcept.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
								}
								
								
								
					}
				}else if(whereSubquery){

					whereClause.queryAliases.setAliasName(aliasName);
					qParser.getWhereClauseSubqueries().add(qp);
					if(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1)!= null && 
							qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).setOperator != null
							 && qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).setOperator.length() > 0){
						
								t.add(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getRightQuery().getQueryAliases());
								t.add(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getLeftQuery().getQueryAliases());
								
								newSetQp = qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1);
								
								qParser.getQuery().getFromTables().putAll(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getRightQuery().getQuery().getFromTables());
								qParser.getQuery().getFromTables().putAll(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getLeftQuery().getQuery().getFromTables());
								
								if(qp.setOperator.equalsIgnoreCase("UNION")) {
									qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getRightQuery().getQuery().getRepeatedRelationCount());
									qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getLeftQuery().getQuery().getRepeatedRelationCount());
									//qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getLeftQuery().getQuery().getRepeatedRelationCount());
									
								/*	qParser. getRepeatedRelNextTuplePos().putAll(qParser.getRightQuery().getQuery().getRepeatedRelNextTuplePos());
									 
									cvcUnion.setRepeatedRelNextTuplePos(unionCVC.getGenCVC().getRepeatedRelNextTuplePos());
									cvcUnion.setResultsetTables(unionCVC.getGenCVC().getResultsetTables());
									cvcUnion.setResultsetColumns(unionCVC.getGenCVC().getResultsetColumns());
									cvcUnion.setNoOfOutputTuples(unionCVC.getGenCVC().getNoOfOutputTuples());
									cvcUnion.setForeignKeys(unionCVC.getGenCVC().getForeignKeys());
									cvcUnion.setForeignKeysModified(unionCVC.getGenCVC().getForeignKeysModified());*/
								} else if (qp.setOperator.equalsIgnoreCase("INTERSECT")) {
									qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getRightQuery().getQuery().getRepeatedRelationCount());
									
									
								//	cvcIntersect.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
									
								}else if (qp.setOperator.equalsIgnoreCase("EXCEPT")) {
									qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getRightQuery().getQuery().getRepeatedRelationCount());
									
									//qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getQuery().getRepeatedRelationCount());
									//cvcExcept.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
								}
								
						}
					
					
				}
				
				if(qp.setOperator.equalsIgnoreCase("UNION")) {
					qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getRightQuery().getQuery().getRepeatedRelationCount());
					qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getLeftQuery().getQuery().getRepeatedRelationCount());
					
				/*	qParser. getRepeatedRelNextTuplePos().putAll(qParser.getRightQuery().getQuery().getRepeatedRelNextTuplePos());
					 
					cvcUnion.setRepeatedRelNextTuplePos(unionCVC.getGenCVC().getRepeatedRelNextTuplePos());
					cvcUnion.setResultsetTables(unionCVC.getGenCVC().getResultsetTables());
					cvcUnion.setResultsetColumns(unionCVC.getGenCVC().getResultsetColumns());
					cvcUnion.setNoOfOutputTuples(unionCVC.getGenCVC().getNoOfOutputTuples());
					cvcUnion.setForeignKeys(unionCVC.getGenCVC().getForeignKeys());
					cvcUnion.setForeignKeysModified(unionCVC.getGenCVC().getForeignKeysModified());*/
				} else if (qp.setOperator.equalsIgnoreCase("INTERSECT")) {
					qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getQuery().getRepeatedRelationCount());
				//	cvcIntersect.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
					
				}else if (qp.setOperator.equalsIgnoreCase("EXCEPT")) {
					qParser.getQuery().getRepeatedRelationCount().putAll(qParser.getQuery().getRepeatedRelationCount());
					//cvcExcept.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
				}
				
				sqa.setTabs(t);
				return sqa;
			} 
		
		if(fromSubquery && !isSetOp){
			fromClause.queryAliases=new FromListElement();
			PlainSelect plainSelect = (PlainSelect)subquery.getSelectBody();
			FromItem prev = plainSelect.getFromItem();
		 
			fromClause.queryAliases.setAliasName(aliasName);
			fromClause.queryAliases.setTableName(null);
			
			qParser.getFromClauseSubqueries().add(fromClause);
			qParser.getSubQueryNames().put(aliasName, qParser.getFromClauseSubqueries().size()-1);
		}
		if(whereSubquery && !isSetOp){
			whereClause.queryAliases=new FromListElement();
			PlainSelect plainSelect = (PlainSelect)subquery.getSelectBody();
			FromItem prev = plainSelect.getFromItem();
		 
			whereClause.queryAliases.setAliasName(aliasName);
			qParser.getWhereClauseSubqueries().add(whereClause);
		}
			sqa.setAliasName(aliasName);

			//	JoinTree
			jtn.setNodeAlias(aliasName);

			
			PlainSelect plainSelect = (PlainSelect)subquery.getSelectBody();
			FromItem prev = plainSelect.getFromItem();

			HashMap <Alias,FromItem> encounteredAliasMap = new HashMap();
			encounteredAliasMap.put(prev.getAlias(), prev);
			sqa.setTableName(null);

			if (plainSelect.getJoins()==null){
				//only one item
				
				if(prev instanceof net.sf.jsqlparser.schema.Table){
					FromListElement temp = OperateOnBaseTable.OperateOnBaseTableJSQL((net.sf.jsqlparser.schema.Table)prev, jtn, qParser, fromSubquery, whereSubquery);
					t.add(temp);
				}else if(prev instanceof SubSelect){
					//QueryParser qp = new QueryParser(qParser.getTableMap());
					
					FromListElement temp = 	temp = OperateOnSubQueryJSQL.OperateOnSubquery(((SubSelect)prev), allConds, jtn, fromSubquery, whereSubquery, qParser);
					
					t.add(temp);
				}	
			
			} else {
				
				FromListElement temp = OperateOnJoinJSQL.OperateOnJoinsJSQL(plainSelect.getJoins(),prev,(new Vector<FromListElement>()),plainSelect.getJoins().size(),
						(fromSubquery)?fromClause.allConds:whereClause.allConds, jtn,fromSubquery, whereSubquery,qParser);
				t.add(temp);	
			} 
			sqa.setTabs(t);
			// 	Geting SelectionClause and JoinClause equalities		
			sqWhereClause = plainSelect.getWhere();
			
			/*
			 * 	This will also collect data for killing IN clause mutants
			 */
			if (sqWhereClause != null) {
				/**********CALL LOCAL METHOD FOR HANDLING CASE CONDITION IN WHERE SUBQ **********/
				caseInWhereClause(sqWhereClause,null,whereClause);
				
				whereClausePred = WhereClauseVectorJSQL.getWhereClauseVector(sqWhereClause,null, sqa ,true, queryType, qParser);
			}
			
			//FIXME: Mahesh Add code to collect projected columns,groupby and having clause of subquery
			//ResultSetNode rs=subquery.getSubquery();//FIXME
			WhereClauseVectorJSQL.getAggregationDataStructures(plainSelect, sqa, qParser.getQuery().getFromTables(),fromSubquery,whereSubquery,qParser);
			//	Add projected columns	for this subquery	
			List<SelectItem> rcList = plainSelect.getSelectItems();
			Vector<CaseCondition> caseConditionsVector = new Vector<CaseCondition>();
			boolean isDistinct = false;
			
			for (int k=0;k<rcList.size();k++) {

				if (rcList.get(k) instanceof AllColumns) {
					tempProjectedCols.addAll(Util.addAllProjectedColumns(sqa, queryType,qParser));
				}
				else if (rcList.get(k) instanceof SelectExpressionItem){				
					SelectExpressionItem rc = (SelectExpressionItem)rcList.get(k);
					Expression exp = rc.getExpression();
					if(rc.getAlias() != null && rc.getAlias().getName() != null){
						encounteredAliasMap.put(rc.getAlias(), prev);
					}
					String exposedName = "";
					
						//Set Column alias if present inside subQuery
						if(rc.getAlias() !=null && rc.getAlias().getName() != null){
							exposedName = rc.getAlias().getName();
						}else{
							//Else set column names as exposed name
							exposedName = exp.toString();
						}
					
					if (exp instanceof Parenthesis){			
						
						Expression expr = ((Parenthesis)exp).getExpression();
						
						if(expr instanceof Column){
							if(plainSelect.getDistinct() != null){
								qParser.setIsDistinct(true);
								isDistinct = true;
							}
						}
						
						
						
						if(expr instanceof CaseExpression){
							exp = expr;
						}
						
						
					}
					if(exp instanceof CaseExpression ) {
						
						if(exp instanceof Column || (exp instanceof Function && rc.getAlias()!= null)){
							//Set Column alias if present inside subQuery
							if(rc.getAlias() !=null && rc.getAlias().getName() != null){
								exposedName = rc.getAlias().getName();
							}else{
								//Else set column names as exposed name
								exposedName = exp.toString();
							}
						}
						
						
						 List<Expression> whenClauses = ((CaseExpression) exp).getWhenClauses();
						 for(int i=0;i < whenClauses.size();i++ ){
							
							CaseCondition cC = new CaseCondition();
							Node n = WhereClauseVectorJSQL.getWhereClauseVector(((WhenClause)((CaseExpression) exp).getWhenClauses().get(i)).getWhenExpression(),exposedName, sqa,false,queryType,qParser);
							cC.setCaseConditionNode(n);
							cC.setCaseCondition(n.toString());
						    cC.setConstantValue(((WhenClause)((CaseExpression) exp).getWhenClauses().get(i)).getThenExpression().toString());
						    caseConditionsVector.add(cC);
						   // qParser.getCaseConditions().add(cC);
						 }
						 //Add the else clause if present as the last item
						 if(((CaseExpression) exp).getElseExpression() != null){
							CaseCondition cC = new CaseCondition();
							//cC.setCaseConditionNode(n);
							cC.setCaseCondition("else");
						    cC.setConstantValue(((CaseExpression) exp).getElseExpression().toString());
						    caseConditionsVector.add(cC);
						 }
						 //Add Case conditions to queryparser
						 fromClause.getCaseConditionMap().put(1,caseConditionsVector);
					}
					else{
						Node n = WhereClauseVectorJSQL.getWhereClauseVector(exp,exposedName, sqa,false,queryType,qParser);
						n.setDistinct(isDistinct);
						tempProjectedCols.add(n);
					}
					
					
					//	projectedCols.add(getWhereClauseVector(exp, sqa,false));
					//tempProjectedCols.add(WhereClauseVectorJSQL.getWhereClauseVector(exp,exposedName, sqa,false, queryType,qParser));

					if(qParser.isUpdateNode){
						//FIXME
						//qParser.updateColumn.add(WhereClauseVectorJSQL.getWhereClauseVector(rc.getReference(),rc.getName(),qParser.queryAliases,false, queryType,qParser));					
					}


				}
			}
			if(whereSubquery){
				if(sqWhereClause != null && whereClausePred != null){
					//allConds.add(whereClausePred);
					if(whereClause.allConds.isEmpty())
						whereClause.allConds.add(whereClausePred);
					else {
						Node n1=whereClause.allConds.get(0);
						Node n2=whereClausePred;
						Node n=new Node();
						n.setLeft(n1);
						n.setRight(n2);
						n.setType(Node.getAndNodeType());
						whereClause.allConds.removeAllElements();
						whereClause.allConds.add(n);
					}
				}
				for(Node pc: tempProjectedCols)
					whereClause.projectedCols.add(pc);
				whereClause.queryAliases.setTabs(t);
			}
			else if(fromSubquery){ 
				if(sqWhereClause != null && whereClausePred != null) {
					//allConds.add(whereClausePred); 
					if(fromClause.allConds.isEmpty())
						fromClause.allConds.add(whereClausePred);
					else {
						Node n1=fromClause.allConds.get(0);
						Node n2=whereClausePred;
						Node n=new Node();
						n.setLeft(n1);
						n.setRight(n2);
						n.setType(Node.getAndNodeType());
						fromClause.allConds.removeAllElements();
						fromClause.allConds.add(n);
					}
					//for(Node pc: tempProjectedCols)
					//	fromClause.projectedCols.add(pc);
				//	fromClause.queryAliases.setTabs(t);
				} 
				for(Node pc: tempProjectedCols)
					fromClause.projectedCols.add(pc);
				fromClause.queryAliases.setTabs(t);
			}
			else
			{
				if(sqWhereClause != null) allConds.add(whereClausePred);
				for(Node pc: tempProjectedCols)
					qParser.getProjectedCols().add(pc);
				//	this.ProjectedColumns.add(tempProjectedCols);

			}
		return sqa;
		}catch(Exception e){
		logger.log(Level.SEVERE,"Failed while Operating on SubQuery : "+e.getMessage(),e);
		throw new Exception(e.getMessage());
		}
	}
	
	/**
	 * This method gets the case statements in where part of the query and adds it to query parser.
	 * Removes the where condition from the whereclause so that the where clause predicates are not generated
	 * for the same. 
	 * 
	 * @param whereClause
	 * @param colExpression
	 * @param qParser
	 * @return
	 * @throws Exception
	 */
	private static boolean caseInWhereClause(Expression whereClause, Expression colExpression, QueryParser qParser) throws Exception{
		
		Vector<CaseCondition> caseConditionsVector = new Vector<CaseCondition>();
		boolean isCaseExpr = false;
		boolean isCaseExists = false;
		try{
		if(whereClause instanceof CaseExpression){
			
			 List<Expression> whenClauses = ((CaseExpression) whereClause).getWhenClauses();
			 for(int i=0;i < whenClauses.size();i++ ){
				
				CaseCondition cC = new CaseCondition();
				Node n = WhereClauseVectorJSQL.getWhereClauseVector(((WhenClause)((CaseExpression) whereClause).getWhenClauses().get(i)).getWhenExpression(),null, qParser.queryAliases,true,0,qParser);
				cC.setCaseConditionNode(n);
				cC.setCaseCondition(n.toString());
			    cC.setConstantValue(((WhenClause)((CaseExpression) whereClause).getWhenClauses().get(i)).getThenExpression().toString());
			    if(colExpression!= null && colExpression instanceof Column){
			    	Node n1 = ((WhereClauseVectorJSQL.getWhereClauseVector((colExpression),null, qParser.queryAliases,true,0,qParser)));
			    	cC.setColValueForConjunct(n1.getColumn());
			    	cC.setCaseOperator("=");
			    }
			    caseConditionsVector.add(cC);
			   // qParser.getCaseConditions().add(cC);
			 }
			 isCaseExpr = true;
			 //Add the else clause if present as the last item
			 if(((CaseExpression) whereClause).getElseExpression() != null){
				CaseCondition cC = new CaseCondition();
				//cC.setCaseConditionNode(n);
				cC.setCaseCondition("else");
			    cC.setConstantValue(((CaseExpression) whereClause).getElseExpression().toString());
			    if(colExpression != null && colExpression instanceof Column){
			    	Node n1 = ((WhereClauseVectorJSQL.getWhereClauseVector((colExpression),null, qParser.queryAliases,true,0,qParser)));
			    	cC.setColValueForConjunct(n1.getColumn());
			    }
			    caseConditionsVector.add(cC);
			 }
			 //Add Case conditions to queryparser
		   qParser.getCaseConditionMap().put(2,caseConditionsVector);
		   return isCaseExpr;
		}
		else if(whereClause instanceof BinaryExpression){
			Expression binaryLeftExp = ((BinaryExpression)whereClause).getLeftExpression();
			Expression binaryRightExp = ((BinaryExpression)whereClause).getRightExpression();
			if(binaryLeftExp != null){
				isCaseExists= caseInWhereClause(binaryLeftExp,binaryRightExp,qParser);
				//If Case stmnt exists, rearrange Where clause to omit CASE condition
				if(isCaseExists){
					((BinaryExpression) whereClause).setLeftExpression(null);
					((BinaryExpression) whereClause).setRightExpression(null);
				}
			}
			
			if(binaryRightExp != null){
				isCaseExists = caseInWhereClause(binaryRightExp,binaryLeftExp,qParser);
				//If Case stmnt exists, rearrange Where clause to omit CASE condition
				if(isCaseExists){
					((BinaryExpression) whereClause).setLeftExpression(null);
					((BinaryExpression) whereClause).setRightExpression(null);
				}
			}	
		}
		else if( whereClause instanceof Parenthesis){
			Expression caseExpr = ((Parenthesis)whereClause).getExpression();
			
			if(caseExpr instanceof CaseExpression){
				isCaseExists = caseInWhereClause(caseExpr,colExpression,qParser);
				//If Case stmnt exists, rearrange Where clause to omit CASE condition
				if(isCaseExists){
					((Parenthesis) whereClause).setExpression(null);
				}
				return isCaseExists;
			}
		}
		return isCaseExpr;
		}catch(Exception e){
			logger.log(Level.SEVERE,"Error in Processing case condition in where clause: "+e.getMessage(),e);
			throw e;
		}
	}
}
