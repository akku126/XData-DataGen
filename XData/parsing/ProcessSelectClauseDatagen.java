package parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import generateConstraints.UtilsRelatedToNode;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import parsing.CaseCondition;
import parsing.JoinClauseInfo;
import parsing.Node;
import parsing.RelationHierarchyNode;
import parsing.Table;

public class ProcessSelectClauseDatagen extends ProcessSelectClause{
	private  Logger logger = Logger.getLogger(ProcessSelectClauseDatagen.class.getName());

	static ProcessSelectClauseDatagen processSelectClauseDatagenObj;

	public static ProcessSelectClauseDatagen getInstance(){
		if(processSelectClauseDatagenObj==null){
			processSelectClauseDatagenObj=new ProcessSelectClauseDatagen();
		}
		return processSelectClauseDatagenObj;
	}

	//invisible constructor for prevention of creating objects using new operator
	private ProcessSelectClauseDatagen(){
	}

	@Override
	public void ProcessSelect(PlainSelect plainSelect, QueryStructure qStruct) throws Exception {
		super.ProcessSelect(plainSelect, qStruct);
		qStruct.topLevelRelation = generateRelationHierarchyJSQL(plainSelect);		
	}

	/** method copied and adapted by mathew for query structure from parsing.QueryParser.java
	 * 
	 * This method gets the case statements in where part of the query and adds it to query parser.
	 * Removes the where condition from the whereclause so that the where clause predicates are not generated
	 * for the same. 
	 * 
	 * @param whereClause
	 * @param colExpression
	 * @param qStruct
	 * @return
	 * @throws Exception
	 */
	public  boolean caseInWhereClause(Expression whereClause, Expression colExpression, QueryStructure qStruct, PlainSelect plainSelect) throws Exception{

		Vector<CaseCondition> caseConditionsVector = new Vector<CaseCondition>();
		boolean isCaseExpr = false;
		boolean isCaseExists = false;
		try{
			if(whereClause instanceof CaseExpression){
				parsing.Column nodeColumnValue = null;
				List<Expression> whenClauses = ((CaseExpression) whereClause).getWhenClauses();
				for(int i=0;i < whenClauses.size();i++ ){

					CaseCondition cC = new CaseCondition();
					Node n = processExpression(((WhenClause)((CaseExpression) whereClause).getWhenClauses().get(i)).getWhenExpression(), qStruct.fromListElements,qStruct,plainSelect,null);
					qStruct.getCaseConditionMap().put(2,n.getCaseExpression());
					isCaseExists = true;
				}

				return isCaseExists;
			}
			else if(whereClause instanceof BinaryExpression){
				Expression binaryLeftExp = ((BinaryExpression)whereClause).getLeftExpression();
				Expression binaryRightExp = ((BinaryExpression)whereClause).getRightExpression();
				if(binaryLeftExp != null){
					isCaseExists= caseInWhereClause(binaryLeftExp,binaryRightExp,qStruct,plainSelect);
					//If Case stmnt exists, rearrange Where clause to omit CASE condition
					if(isCaseExists){
						((BinaryExpression) whereClause).setLeftExpression(null);
						((BinaryExpression) whereClause).setRightExpression(null);
					}
				}

				if(binaryRightExp != null){
					isCaseExists = caseInWhereClause(binaryRightExp,binaryLeftExp,qStruct,plainSelect);
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
					isCaseExists = caseInWhereClause(caseExpr,colExpression,qStruct,plainSelect);
					//If Case stmnt exists, rearrange Where clause to omit CASE condition
					if(isCaseExists){
						((Parenthesis) whereClause).setExpression(null);
					}
					
				}
			}
			return isCaseExists;
		}catch(Exception e){
			logger.log(Level.SEVERE,"Error in Processing case condition in where clause: "+e.getMessage(),e);
			throw e;
		}
	}

	//Check if the following methods are needed and why are they added to avoid extra tuples
	/**
	 * This method returns a Node with join conditions as node inside node for sending to conjuncts
	 * for conversion
	 * 
	 * @param joinNodeList
	 * @return
	 * @throws Exception
	 */
	public RelationHierarchyNode generateRelationHierarchyJSQL(FromItem frmItem) throws Exception{

		RelationHierarchyNode node = null; 

		if(frmItem instanceof net.sf.jsqlparser.schema.Table){
			node = new RelationHierarchyNode(((net.sf.jsqlparser.schema.Table)frmItem).getFullyQualifiedName().toUpperCase());
		} 

		else if(frmItem instanceof SelectBody){

			PlainSelect selectNode = (PlainSelect) frmItem;
			//Vector<QueryTreeNode> v = selectNode..getFromList().getNodeVector();

			//for(QueryTreeNode i : v){
			node = generateRelationHierarchyJSQL(selectNode.getFromItem());
			//}

			List<RelationHierarchyNode> l = new ArrayList<RelationHierarchyNode>();
			Expression w = selectNode.getWhere();
			getAllNotExistsQueriesJSQL(w, l); 
			if(node!=null) {
				node.setNotExistsSubQueries(l);
			}
		}
		//Modified for JSQL Exists - End
		return node;
	}

	public RelationHierarchyNode generateRelationHierarchyJSQL(PlainSelect plainsel) throws Exception{
		RelationHierarchyNode node = null;
		FromItem prev = plainsel.getFromItem();
		if(plainsel.getWhere() != null){

			node = generateRelationHierarchyJSQL(prev);

			List<RelationHierarchyNode> l = new ArrayList<RelationHierarchyNode>();
			Expression w = plainsel.getWhere();
			getAllNotExistsQueriesJSQL(w, l); 
			if(node !=null) {
				node.setNotExistsSubQueries(l);
			}
		}

		else{
			node = generateRelationHierarchyJSQL(prev);
		}
		return node;
	}//Added for Exists Subqueries-End

	private void getAllNotExistsQueriesJSQL(Expression w, List<RelationHierarchyNode> l) throws Exception{
		if(w instanceof BinaryExpression){
			BinaryExpression whereNode = (BinaryExpression) w;
			getAllNotExistsQueriesJSQL(whereNode.getRightExpression(), l);
			getAllNotExistsQueriesJSQL(whereNode.getLeftExpression(), l);
		} 
		else if(w instanceof Parenthesis){
			Expression temp = ((Parenthesis) w).getExpression();
			if(temp instanceof ExistsExpression || temp instanceof InExpression){
				getAllNotExistsQueriesJSQL(temp, l);
			}
		} 
		else if((w instanceof ExistsExpression && ((ExistsExpression)w).isNot()) ){
			ExistsExpression notOp = (ExistsExpression) w;
			Expression temp = notOp.getRightExpression();
			if(temp instanceof SubSelect){
				SubSelect subQueryNode = (SubSelect)temp;

				PlainSelect ps = (PlainSelect)subQueryNode.getSelectBody();
				if(ps.getJoins() != null && ps.getJoins().size() > 0){

					l.add(generateRelationHierarchyJSQL(ps));

				}else{
					FromItem frm = ps.getFromItem();
					if(frm instanceof Table){
						l.add(generateRelationHierarchyJSQL(frm));
					}
				}

				//}
		}
		}else if(w instanceof InExpression && ((InExpression)w).isNot()){
			InExpression notOp = (InExpression) w;

			if(notOp.getRightItemsList() instanceof SubSelect){
				SubSelect subQueryNode =(SubSelect)notOp.getRightItemsList();

				PlainSelect ps = (PlainSelect)subQueryNode.getSelectBody();

				if(ps.getJoins() != null && ps.getJoins().size() > 0){

					// l.add(generateRelationHierarchyJSQL(ps));

				}else{
					FromItem frm = ps.getFromItem();
					if(frm instanceof Table){
						l.add(generateRelationHierarchyJSQL(frm));
					}
				}
			}
			else if(notOp.getLeftItemsList() instanceof SubSelect){
				SubSelect subQueryNode =(SubSelect)notOp.getLeftItemsList();

				PlainSelect ps = (PlainSelect)subQueryNode.getSelectBody();

				if(ps.getJoins() != null && ps.getJoins().size() > 0){

					// l.add(generateRelationHierarchyJSQL(ps));

				}else{
					FromItem frm = ps.getFromItem();
					if(frm instanceof Table){
						l.add(generateRelationHierarchyJSQL(frm));
					}
				}
			}
		}
	}
	
	/** @author mathew
	 * 
	 * @param jsqlTable
	 * @param frmListElement
	 * @param qStruct
	 * 
	 * deals with the case when the fromListElement is a table, in which case 
	 * the frmListElement argument is used to encode its details  
	 * 
	 */
	@Override
	public void processFromListTable(net.sf.jsqlparser.schema.Table jsqlTable, FromClauseElement frmListElement, QueryStructure qStruct){
		String tableName = jsqlTable.getFullyQualifiedName().toUpperCase();// getWholeTableName();
		Table table = qStruct.getTableMap().getTable(tableName.toUpperCase());
		String aliasName = "";
		if (jsqlTable.getAlias() == null) {
			aliasName = tableName;
		} else {
			aliasName = jsqlTable.getAlias().getName().toUpperCase();// getAlias();
		}
		if (qStruct.getQuery().getRepeatedRelationCount().get(tableName) != null) {
			qStruct.getQuery().putRepeatedRelationCount(tableName, qStruct.getQuery()
					.getRepeatedRelationCount().get(tableName) + 1);
		} else {
			qStruct.getQuery().putRepeatedRelationCount(tableName, 1);
		}
		String tableNameNo = tableName
				+ qStruct.getQuery().getRepeatedRelationCount().get(tableName);

		frmListElement.setAliasName(aliasName);
		frmListElement.setTableName(tableName);
		frmListElement.setTableNameNo(tableNameNo);
		frmListElement.setBag(null);	
		qStruct.addFromTable(frmListElement);
		
		qStruct.getQuery().addFromTable(table);
		qStruct.getQuery().putBaseRelation(aliasName, tableName);
		
		logger.info("Table added"+frmListElement);
	}
	
	@Override
	public Node processExpressionForAnyComparison(AnyComparisonExpression ace, QueryStructure qStruct) throws Exception{
		SubSelect ss = ace.getSubSelect();

		QueryStructure subQueryParser=new QueryStructure(qStruct.getTableMap());
		Node anyNode=new Node();
		anyNode.setSubQueryStructure(subQueryParser);
		anyNode.setType(Node.getAnyNodeType());
		processWhereSubSelect(ss,subQueryParser,qStruct);
		if(anyNode.getSubQueryStructure() != null && anyNode.getSubQueryStructure().getAllDnfSelCond() != null && !anyNode.getSubQueryStructure().getAllDnfSelCond().isEmpty()){
			anyNode.setSubQueryConds(anyNode.getSubQueryStructure().getAllDnfSelCond().get(0));
		}
		return anyNode;
	}
	
	@Override
	public Node processExpressionForAllComparison(AllComparisonExpression ace, QueryStructure qStruct) throws Exception{
		SubSelect ss = ace.getSubSelect();

		QueryStructure subQueryParser=new QueryStructure(qStruct.getTableMap());
		Node allNode=new Node();
		allNode.setSubQueryStructure(subQueryParser);
		allNode.setType(Node.getAllNodeType());
		
		processWhereSubSelect(ss,subQueryParser,qStruct);
		//Set details required for AND / ALL node correctly
		if(allNode.getSubQueryStructure() != null && allNode.getSubQueryStructure().getProjectedCols() != null){
			
			for(Node projectedCol : allNode.getSubQueryStructure().getProjectedCols()){
				allNode = projectedCol;
				allNode.setSubQueryStructure(subQueryParser);
				allNode.setType(Node.getAllNodeType());
			}
		}
		return allNode;	
	}
	
	@Override
	public Node processExpressionForGreaterThan(GreaterThan broNode, Vector<FromClauseElement> fle,
			QueryStructure qStruct, PlainSelect plainSelect, String joinType) throws Exception{
				
		Node n = new Node();
		n.setType(Node.getBroNodeType());
		n.setOperator(QueryStructure.cvcRelationalOperators[3]);
		n.setLeft(processExpression(broNode.getLeftExpression(), fle, qStruct,plainSelect,joinType));
		n.setRight(processExpression(broNode.getRightExpression(),fle, qStruct,plainSelect,joinType));
		//setQueryTypeAndIndex(n,qStruct);
		n.setQueryType(2);
		n.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
		
		if(n.getLeft() != null && n.getLeft().getSubQueryConds() != null && n.getLeft().getSubQueryConds().size() > 0 && n.getSubQueryConds()!=null){
			n.setSubQueryConds(n.getLeft().getSubQueryConds());
			n.getLeft().getSubQueryConds().clear();
		}
		else  { 
			if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 && n.getSubQueryConds()!=null){
				n.setSubQueryConds(n.getRight().getSubQueryConds());
				n.getRight().getSubQueryConds().clear();
			}
		}

		//commented by mathew on 17 october 2016
		if(broNode.getRightExpression() instanceof AllComparisonExpression ||
				broNode.getLeftExpression() instanceof AllComparisonExpression){

			Node sqNode = new Node();
			sqNode.setType(Node.getAllNodeType());
			
			sqNode.setLhsRhs(n);
			return sqNode;
		}

		if(broNode.getRightExpression() instanceof AnyComparisonExpression ||
				broNode.getLeftExpression() instanceof AnyComparisonExpression){
			Node sqNode = new Node();
			sqNode.setType(Node.getAnyNodeType());					
			sqNode.setLhsRhs(n);
			return sqNode; 
		} 
		
		//the following added by mathew on 17 oct 2016
		if(n.getLeft().getType().equals(Node.getAllNodeType())||n.getRight().getType().equals(Node.getAllNodeType())||
				n.getLeft().getType().equals(Node.getAnyNodeType()) ||
				n.getRight().getType().equals(Node.getAnyNodeType()))
			n.setType(Node.getBroNodeSubQType());
		else if(n.getLeft().getType().equals(Node.getColRefType())&&n.getRight().getType().equals(Node.getColRefType())){
			if(joinType!=null)
				n.setJoinType(joinType);
			else
				n.setJoinType(JoinClauseInfo.innerJoin);
		}
		//n.setLhsRhs(n);
		//n.setType(n.getType());
		//setQueryTypeAndIndex(n,qStruct);
						
		return n;
	}
	
	@Override
	public Node ProcessExpressionForSubselect(SubSelect sqn, Vector<FromClauseElement> fle, QueryStructure qStruct,
			PlainSelect plainSelect, String joinType) throws Exception{
		QueryStructure subQueryParser=new QueryStructure(qStruct.getTableMap());
		Node node=new Node();
		processWhereSubSelect(sqn,subQueryParser,qStruct);
		
		List<SelectItem> rcList = ((PlainSelect)sqn.getSelectBody()).getSelectItems();	

		SelectExpressionItem rc = (SelectExpressionItem)rcList.get(0);
		
		//Added by Shree for subQ data gen - start
		node.setQueryType(2);
		node.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);								
		//Added by Shree for subQ data gen - end
		
		node = new Node(subQueryParser.getProjectedCols().get(0));
		node.setSubQueryConds(subQueryParser.getAllSubQueryConds());
		node.setSubQueryStructure(subQueryParser);
		node.setType(Node.getColRefType());
		
		if(rc.getExpression() instanceof Function){ 
			Function an = (Function)rc.getExpression();
			String aggName = an.getName();
			ExpressionList expL = an.getParameters();
			AggregateFunction af = new AggregateFunction();
			Node n = processExpression(expL.getExpressions().get(0),  subQueryParser.getFromListElements(), subQueryParser,plainSelect,joinType); 
			n.getColumnsFromNode().add(n.getColumn());
			af.setAggExp(n);
			af.setFunc(aggName.toUpperCase());
			af.setDistinct(an.isDistinct());				

			Node rhs = new Node();
			rhs.setAgg(af);
			rhs.setType(Node.getAggrNodeType());
			//Shree added this to set table name , table name no in node level 
			rhs.setTable(af.getAggExp().getTable());
			rhs.setTableAlias(af.getAggExp().getTableAlias());
			rhs.setTableNameNo(af.getAggExp().getTableNameNo());
			
			rhs.setQueryType(2);
			rhs.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
			if(node.getSubQueryStructure() != null && node.getSubQueryStructure().getAllDnfSelCond()  != null && !node.getSubQueryStructure().getAllDnfSelCond().isEmpty()){
				///Check .get(0) - again
				rhs.setSubQueryConds(node.getSubQueryStructure().getAllDnfSelCond().get(0));
			}
			
			// create the final subquery node and return it
			Node sqNode = new Node();
			if(node.getSubQueryStructure() != null && node.getSubQueryStructure().getAllDnfSelCond()  != null && !node.getSubQueryStructure().getAllDnfSelCond().isEmpty()){				
				sqNode.setSubQueryConds(node.getSubQueryStructure().getAllDnfSelCond().get(0));
			}
			
			if(qStruct.getWhereClauseSubqueries() != null && !qStruct.getWhereClauseSubqueries().isEmpty()){						
				if(!qStruct.getWhereClauseSubqueries().get(0).getConjuncts().isEmpty() 
						&& qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds != null){
					//If subquery condition is a String selection condition, add it as a conjunt
						qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds.addAll(rhs.getSubQueryConds());
						
						for(Node nod: rhs.getSubQueryConds()){		
							
							if(ConjunctQueryStructure.isStringSelection(nod,1) ){										
								String str=nod.getRight().getStrConst();
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).stringSelectionConds.add(nod);
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds.remove(nod);
							}
							else if(ConjunctQueryStructure.isStringSelection(nod,0)){
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).stringSelectionConds.add(nod);
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds.remove(nod);
							}
						}
					}							
					sqNode.setType(Node.getBroNodeSubQType());
					sqNode.setLhsRhs(rhs);
					return sqNode;
			}
		}
		else if(rc.getExpression() instanceof Parenthesis && (((Parenthesis)rc.getExpression()).getExpression()) instanceof Column){
			//the result of subquery must be a single tuple
			logger.log(Level.WARNING,"the result of subquery must be a single tuple");
		}
		else{
			//It is a scalar subquery that returns only one value
			Node retNode = new Node();
			retNode.setQueryType(2);
			retNode.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
		
			retNode.setType(Node.getBroNodeSubQType());
			retNode.setLhsRhs(node);
			
			if(node.getSubQueryStructure() != null && node.getSubQueryStructure().getAllDnfSelCond()  != null && !node.getSubQueryStructure().getAllDnfSelCond().isEmpty()){
				///Check .get(0) - again
				retNode.setSubQueryConds(node.getSubQueryStructure().getAllDnfSelCond().get(0));
			}
			
			if(qStruct.getWhereClauseSubqueries() != null && !qStruct.getWhereClauseSubqueries().isEmpty()){
				
				if(!qStruct.getWhereClauseSubqueries().get(0).getConjuncts().isEmpty() 
						&& qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds != null){
						qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds.addAll(node.getSubQueryConds());
						
						for(Node nod: node.getSubQueryConds()){		
							
							if(ConjunctQueryStructure.isStringSelection(nod,1) ){										
								String str=nod.getRight().getStrConst();										
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).stringSelectionConds.add(nod);
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds.remove(nod);
							}
							else if(ConjunctQueryStructure.isStringSelection(nod,0)){
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).stringSelectionConds.add(nod);
								qStruct.getWhereClauseSubqueries().get(0).getConjuncts().get(0).selectionConds.remove(nod);
							}
						}
					}
			}
				return retNode;
		}
		
		return null;
	}
	
	@Override
	public Node ProcessExpressionForExists(ExistsExpression sqn, Vector<FromClauseElement> fle, QueryStructure qStruct,
			PlainSelect plainSelect, String joinType) throws Exception{
	
	
		SubSelect subS = (SubSelect)sqn.getRightExpression();


		QueryStructure subQueryParser=new QueryStructure(qStruct.getTableMap());
		Node existsNode=new Node();
		existsNode.setSubQueryStructure(subQueryParser);
		existsNode.setType(Node.getExistsNodeType());
		existsNode.setSubQueryConds(null);
		existsNode.setQueryType(2);
		existsNode.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
		processWhereSubSelect(subS,subQueryParser,qStruct);
		
		//setQueryTypeAndIndex(existsNode,qStruct);
		Node notNode = new Node();
		
		Node sqNode = new Node();
		Node rhs = new Node();
		rhs.setQueryType(2);
		rhs.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
		sqNode.setLhsRhs(rhs);
		sqNode.setType(Node.getExistsNodeType());
		
		existsNode.setQueryType(2);
		existsNode.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);				
		
		if(!((ExistsExpression) sqn).isNot()){					
			return existsNode;
		}else{
			notNode.setType(Node.getNotNodeType());
			notNode.setRight(null);
			notNode.setLeft(sqNode);
			notNode.setQueryType(2); 
			notNode.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);			
			return notNode;

		}
	}
	
	@Override
	public Node ProcessExpressionForIn(InExpression sqn, Vector<FromClauseElement> fle, QueryStructure qStruct,
			PlainSelect plainSelect, String joinType) throws Exception{
	
		SubSelect subS=null;
		Node inNode=new Node();
		inNode.setType(Node.getBroNodeType());
		inNode.setOperator("=");

		Node notNode = new Node();	
		Node rhs = new Node();

		if (sqn. getLeftItemsList() instanceof SubSelect){
			subS=(SubSelect)sqn.getLeftItemsList();
		}
		else if(sqn. getRightItemsList() instanceof SubSelect){ 
			subS=(SubSelect)sqn.getRightItemsList();
		}
		
		QueryStructure subQueryParser=new QueryStructure(qStruct.getTableMap());
		rhs.setSubQueryStructure(subQueryParser);	
		processWhereSubSelect(subS,subQueryParser,qStruct);
		
		//Added by Shree for subQ data gen - start
		rhs.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
		rhs.setQueryType(2);
		
		Node rhsNew = new Node();
		if(rhs.getSubQueryStructure() != null){
			//if(rhs.getSubQueryStructure().getAllDnfSelCond() != null && !rhs.getSubQueryStructure().getAllDnfSelCond().isEmpty()){
				//Get all SubQ condition
				//FIXME change .get(0) - test for all conditions in subQ
				//Vector<Node> nd = rhs.getSubQueryStructure().getAllDnfSelCond().get(0);
				Node nd = rhs.getSubQueryStructure().getLstProjectedCols().get(0);
				//Construct Node tree from the condition nodes as AND node if more than one condition exists else add the node to rhs
				if(nd!= null){
					rhsNew = nd;	
				}				
				else{
					//Call a method that adds all the selection condition as AND nodes to the rhsNew node
					
				}
				rhsNew.setSubQueryStructure(subQueryParser);
				rhsNew.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
				rhsNew.setQueryType(2);
			//}
		}
		
		//Added by Shree - end
		
		Node lhs = processExpression(sqn. getLeftExpression(), fle, qStruct,plainSelect,joinType);
		lhs.setQueryType(2);
		lhs.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
		inNode.setQueryType(2);
		inNode.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
		inNode.setLeft(lhs);
		inNode.setRight(rhsNew);
		
		//set lhsrhs node with inNode - set rhs to contain the nodes in allDnfSelCond inside subQStructure inside rhs - if vector has more nodes, add them with AND node condition recursively
		
		Node sqNode = new Node();
		sqNode.setLeft(null);
		sqNode.setRight(null);				
		sqNode.setType(inNode.getType());
		//setQueryTypeAndIndex(sqNode,qStruct);
		
		//Added by Shree for subQ data gen - start
		sqNode.setLhsRhs(inNode);
		sqNode.setType(Node.getInNodeType());
		
		 Vector allCon = new Vector<>();  
		 if( qStruct.getWhereClauseSubqueries().get((qStruct.getWhereClauseSubqueries().size()-1)).getAllDnfSelCond() != null
				 && !qStruct.getWhereClauseSubqueries().get((qStruct.getWhereClauseSubqueries().size()-1)).getAllDnfSelCond().isEmpty()){
			 
			 allCon = qStruct.getWhereClauseSubqueries().get((qStruct.getWhereClauseSubqueries().size()-1)).getAllDnfSelCond().get(0);
		 }else{
			 allCon = null;
		 }
		 if(allCon != null && !allCon.isEmpty()){
			 Node condition1 = (Node)allCon.get(allCon.size()-1);
				condition1.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
				condition1.setQueryType(2);
				sqNode.setSubQueryConds(allCon);
				 
		 }else{
			// create the final subquery node and return it
				sqNode.setType(Node.getInNodeType());
				sqNode.setLhsRhs(inNode);						
		 }
		
		 
		//Added by Shree for subQ data gen - end
		if(!sqn.isNot()){	
			//return inNode
			sqNode.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
			sqNode.setQueryType(2);
			return sqNode;
		}else{
			notNode.setType(Node.getNotNodeType());
			notNode.setRight(null);
			notNode.setLeft(sqNode);
			notNode.setQueryIndex(qStruct.getWhereClauseSubqueries().size()-1);
			notNode.setQueryType(2);
			//setQueryTypeAndIndex(notNode,qStruct);
			return notNode;
		}
		
	}

}
