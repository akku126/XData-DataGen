package parsing;

import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.impl.sql.compile.AllResultColumn;
import org.apache.derby.impl.sql.compile.BinaryOperatorNode;
import org.apache.derby.impl.sql.compile.FromBaseTable;
import org.apache.derby.impl.sql.compile.FromList;
import org.apache.derby.impl.sql.compile.FromSubquery;
import org.apache.derby.impl.sql.compile.HalfOuterJoinNode;
import org.apache.derby.impl.sql.compile.JoinNode;
import org.apache.derby.impl.sql.compile.NotNode;
import org.apache.derby.impl.sql.compile.QueryTreeNode;
import org.apache.derby.impl.sql.compile.ResultColumn;
import org.apache.derby.impl.sql.compile.ResultColumnList;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SelectNode;
import org.apache.derby.impl.sql.compile.SubqueryNode;
import org.apache.derby.impl.sql.compile.ValueNode;

import parsing.QueryParser;
import parsing.Conjunct;
import parsing.FromListElement;
import parsing.JoinTreeNode;
import parsing.Node;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

public class ProcessResultSetNode {

	private static Logger logger = Logger.getLogger(ProcessResultSetNode.class.getName());
	
	/*
	//added temporarily by mathew on may 13 2016 
	//Traverses through the join tree to get the information about the relations involved in the joins
	public static void traverseJoinTree(JoinTreeNode root, ArrayList<String> joinTables){
		if(root == null|| root.getNodeType()==null)
			return;
		
		if(root.getNodeType().equals("INNER JOIN")){
			traverseJoinTree(root.getLeft(), joinTables);
			traverseJoinTree(root.getRight(),joinTables);
		}
		
		if(root.getNodeType().equals("LEFT OUTER JOIN") || root.getNodeType().equals("RIGHT OUTER JOIN")){
			traverseJoinTree(root.getLeft(),joinTables);
			traverseJoinTree(root.getRight(),joinTables);
		}
		
		if(root.getNodeType().equals("RELATION")){
			joinTables.add(root.getTableNameNo());
		}
	}*/
	
	public static void processResultSetNodeJSQL(PlainSelect plainSelect,boolean debug,parsing.QueryParser qParser) throws Exception{
		try{
		
		logger.log(Level.INFO,"\nOriginal Query : "+ qParser.getQuery().getQueryString());

		/*
		 * The fromTableList can contain nodes which are join nodes (or other
		 * complex nodes - as written by apache derby parser) so need to go
		 * through the list to identify individual tables.
		 */
		Vector<CaseCondition> caseConditionsVector = new Vector<CaseCondition>();
		qParser.queryAliases = new FromListElement();
		qParser.queryAliases.setAliasName("Q");
		qParser.queryAliases.setTableName(null);
		qParser.topLevelRelation = generateRelationHierarchyJSQL(plainSelect);//FIXME ANURAG
		
		//JoinTree
		qParser.root = new JoinTreeNode();
		Vector<Node> JoinConditions = new Vector<Node>(); 
		Vector<FromListElement> t = new Vector<FromListElement>();
		HashMap <Alias,FromItem> encounteredAliasMap = new HashMap();
		
		FromItem prev = plainSelect.getFromItem();
		encounteredAliasMap.put(prev.getAlias(), prev);

		if (plainSelect.getJoins()==null && ! (prev instanceof SubJoin)){
			//only one item
			if (prev instanceof net.sf.jsqlparser.schema.Table){
				FromListElement temp = OperateOnBaseTable.OperateOnBaseTableJSQL((net.sf.jsqlparser.schema.Table)prev, qParser.root,qParser, false, false);			
				t.add(temp);
			} else if (prev instanceof SubSelect){
				FromListElement temp =	OperateOnSubQueryJSQL.OperateOnSubquery((SubSelect) prev,qParser.allConds, qParser.root,true,false,qParser);
				t.add(temp);
				//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of from subqueries 
				//qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).parseQuery("q1", prev.toString());	
			}
		} 
		else if(prev instanceof SubJoin){
			FromListElement temp = null; 
			prev = ((SubJoin)plainSelect.getFromItem()).getLeft();
			Join aJ = ((SubJoin)plainSelect.getFromItem()).getJoin();
			List<Join> jList = new ArrayList<Join>();
			jList.add(aJ);
			temp = OperateOnJoinJSQL.OperateOnJoinsJSQL( jList,prev ,(new Vector<FromListElement>()),jList.size(),JoinConditions, qParser.root,false, false, qParser);
			t.add(temp);
			
		}
		else if(plainSelect.getJoins() instanceof SubJoin){
			FromListElement temp = null; 
			prev = ((SubJoin)plainSelect.getFromItem()).getLeft();
			Join aJ = ((SubJoin)plainSelect.getFromItem()).getJoin();
			List<Join> jList = new ArrayList<Join>();
			jList.add(aJ);
			temp = OperateOnJoinJSQL.OperateOnJoinsJSQL( jList,prev ,(new Vector<FromListElement>()),jList.size(),JoinConditions, qParser.root,false, false, qParser);
			t.add(temp);
			
		}
		
		else {    
				FromListElement temp = null; 
				temp = OperateOnJoinJSQL.OperateOnJoinsJSQL( plainSelect.getJoins(),prev ,(new Vector<FromListElement>()),plainSelect.getJoins().size(),JoinConditions, qParser.root,false, false, qParser);
				t.add(temp);
		}
						
		qParser.queryAliases.setTabs(t);
		// Geting SelectionClause and JoinClause equalities
		Expression whereClause = plainSelect.getWhere();

		if (whereClause != null) {

			/**********CALL LOCAL METHOD FOR HANDLING CASE CONDITION IN WHERE SUBQ **********/
			caseInWhereClause(whereClause,null,qParser);
			/*Vector<CaseCondition> caseConditions = null;
			if(qParser.getCaseConditionMap().containsKey(2)){
				caseConditions = qParser.getCaseConditionMap().get(2);
			}*/
					
			Node whereClausePred = new Node();
			whereClausePred = WhereClauseVectorJSQL.getWhereClauseVector(whereClause,null, qParser.queryAliases,true,0,qParser);
			


			/**If outer query block has where condition refers to aggregate function on 
				alias in from class sub query block
				change it to having clause and add it to qparser.having **/
			if(whereClausePred != null && whereClausePred.getLeft() != null &&
					whereClausePred.getLeft().getType() != null && 
					whereClausePred.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()) && 
					whereClausePred.getRight() != null && 
							whereClausePred.getRight().getType() != null && 
					whereClausePred.getRight().getType().equalsIgnoreCase(Node.getValType())){
				qParser.havingClause = whereClausePred;
				
			}
			else if( whereClausePred != null) 
				qParser.allConds.add(whereClausePred);

			/*mathew on 8 Jun 2016
			 *  if condition below assumes that there are no nested join
			 *  conditions
			 */ 
			qParser.setJoinConds(JoinConditions);
			//Add join condition
			if(!JoinConditions.isEmpty()){
				Vector<Node> allCondsDups=(Vector<Node>) qParser.allConds.clone();
				Node NewCond = new Node();
				NewCond.setType(Node.getAndNodeType());
				
				NewCond.setLeft(getHierarchyOfJoinNode(JoinConditions));
				NewCond.setRight(qParser.allConds.get(0));
				allCondsDups.remove(qParser.allConds.get(0));
				allCondsDups.add(NewCond);
				qParser.allConds.removeAllElements();
				qParser.allConds.addAll(allCondsDups);
			}
		
			/**If there are CASE conditions, then add them as AND / OR Conjuncts */
			/*if(caseConditions != null){
				//Get case condition node, create AND with case condition node and assignment part
				//create OR Nodes with each case condition node as left and right.
				for(CaseCondition cc: caseConditions){
					Node n = new Node();
					n.setLeft(cc.getCaseConditionNode());
					n.setType(Node.getAndNodeType());
					//Set the AND condition from 
						Node n1 = new Node();
						n1.setLeft(WhereClauseVectorJSQL.getWhereClauseVector(cc.getColValueForConjunct(),null,qParser.queryAliases,true,0,qParser));					
						n1.setRight(WhereClauseVectorJSQL.getWhereClauseVector(cc.getConstantValue(),null,qParser.queryAliases,true,0,qParser));
						n1.setOperator(cc.getCaseOperator());
					n.setRight(n1);
					
				}
				
			}*/
		} 
		else if(!JoinConditions.isEmpty()){
			if(!qParser.allConds.isEmpty()) {
				Vector<Node> allCondsDups=(Vector<Node>) qParser.allConds.clone();
				Node NewCond = new Node();
				NewCond.setType(Node.getAndNodeType());
				
				NewCond.setLeft(getHierarchyOfJoinNode(JoinConditions));
				NewCond.setRight(qParser.allConds.get(0));
				allCondsDups.remove(qParser.allConds.get(0));
				allCondsDups.add(NewCond);
				qParser.allConds.removeAllElements();
				qParser.allConds.addAll(allCondsDups);
				/*Node n1= JoinConditions.get(0);
				Node n2=qParser.allConds.get(0);
				Node n=new Node();
				n.setLeft(n1);
				n.setRight(n2);
				n.setType(Node.getAndNodeType());
				*/
				
				
			} else {
			 qParser.allConds.addAll(JoinConditions);
			}
		}
		
		modifyTreeForCompareSubQ(qParser);		
						
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
		/*if(!qParser.getCaseConditionMap().isEmpty()){
			//Get the case conditions in where clause
			if(qParser.getCaseConditionMap().containsKey(2)){
				for(CaseCondition cC : qParser.getCaseConditionMap().get(2)){
					// Get OR Nodes that contain AND nodes - call flattenORnodes method and add it to Conjuncts
					// 
				}
			}
		}*/
		// Getting Foreign Key into a vector of JoinClauseInfo object
		Util.foreignKeyClosure(qParser);
		if(qParser.isDeleteNode){
			return;
		}
		WhereClauseVectorJSQL.getAggregationDataStructures(plainSelect, qParser.queryAliases, qParser.getQuery().getFromTables(),false,false, qParser);
		WhereClauseVectorJSQL.getOrderByList(plainSelect, qParser.queryAliases, qParser.getQuery().getFromTables(),false,false, qParser);
			logger.log(Level.INFO,"\nJoin Tables : " + qParser.getQuery().getJoinTables());
			logger.log(Level.INFO,"Undirected Join Graph : "
					+ qParser.getQuery().getJoinGraph());

		List<SelectItem> rcList = plainSelect.getSelectItems();
		boolean isDistinct = false;
		
		/** Get selected item and check if it has selectExpression and the left or right node has
		 *  multiplication, division, addition or subtraction, In that case just take the column name
		 *  from left or right node traversing through that and then call where clause vector to
		 *  get the column details. ie., is node = column, then call where clause vector.
		 *  Add that node to rcList
		*/
		// Get Projected Columns in a list Vector<Node> projectionList: one
		// element for each expression in the projection list
		//Add projected columns		
		//ResultColumnList rcList = rsNode.getResultColumns();
		Vector<Node> tempProjectedCols = new Vector<Node>();
		caseConditionsVector = new Vector<CaseCondition>();
		for (int k=0;k<rcList.size();k++) {
	
			if (rcList.get(k) instanceof AllColumns) {
				tempProjectedCols.addAll(Util.addAllProjectedColumns(qParser.queryAliases,0,qParser));
			}
			else if (rcList.get(k) instanceof SelectExpressionItem){				
				SelectExpressionItem rc = (SelectExpressionItem)rcList.get(k);
				Expression exp = rc.getExpression();	
				String exposedName = "";
				if(exp instanceof Column){
					exposedName = exp.toString();
					if(plainSelect.getDistinct() != null){
						qParser.setIsDistinct(true);
						isDistinct = true;
					}
				}
				if (exp instanceof Parenthesis){			
					
					Expression expr = ((Parenthesis)exp).getExpression();
					
					if(((Parenthesis)exp).getExpression() instanceof Column){
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
					
					
					 List<Expression> whenClauses = ((CaseExpression) exp).getWhenClauses();
					 for(int i=0;i < whenClauses.size();i++ ){
						
						CaseCondition cC = new CaseCondition();
						Node n = WhereClauseVectorJSQL.getWhereClauseVector(((WhenClause)((CaseExpression) exp).getWhenClauses().get(i)).getWhenExpression(),exposedName, qParser.queryAliases,false,0,qParser);
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
				   qParser.getCaseConditionMap().put(1,caseConditionsVector);
					
				}else{
					Node n = WhereClauseVectorJSQL.getWhereClauseVector(exp,exposedName, qParser.queryAliases,false,0,qParser);
					n.setDistinct(isDistinct);
					tempProjectedCols.add(n);
				}
			}
		}
		qParser.projectedCols.addAll(tempProjectedCols);

		}catch(Exception e){
			logger.log(Level.SEVERE,"Error in Processing ResultSet Node : "+e.getMessage(),e);
			throw e;
		}
	}
	
	/**
	 * 
	 * @param rsNode
	 * @param debug
	 * @param qParser
	 * @throws Exception
	 */
	@Deprecated
	public static void processResultSetNode (ResultSetNode rsNode,boolean debug,parsing.QueryParser qParser) throws Exception{


		if (debug)
			logger.log(Level.INFO,"\nOriginal Query : "+ qParser.getQuery().getQueryString());
		
	
		FromList fromList = rsNode.getFromList();
		Vector<QueryTreeNode> fromTableList = fromList.getNodeVector(); // this
		// is
		// coming
		// from
		// derby
		// parser


		fromTableList.get(0).printSubNodes(2);

		/*
		 * The fromTableList can contain nodes which are join nodes (or other
		 * complex nodes - as written by apache derby parser) so need to go
		 * through the list to identify individual tables.
		 */

		qParser.queryAliases = new FromListElement();
		qParser.queryAliases.setAliasName("Q");
		qParser.queryAliases.setTableName(null);

		qParser.topLevelRelation = generateRelationHierarchy(rsNode);

		//JoinTree
		qParser.root = new JoinTreeNode();
		Vector<Node> JoinConditions = new Vector<Node>(); 
		Vector<FromListElement> t = new Vector<FromListElement>();
		for (int j = 0; j < fromTableList.size(); j++) {
			if (fromTableList.get(j) instanceof FromBaseTable) {
				FromListElement temp = OperateOnBaseTable.OperateOnBaseTable((FromBaseTable) fromTableList.get(j),false, "", qParser.root,qParser, false, false);
				t.add(temp);
			} else if (fromTableList.get(j) instanceof JoinNode) {
				FromListElement temp = new FromListElement();
				temp = OperateOnJoin.OperateOnJoinNode((JoinNode) fromTableList.get(j), "",JoinConditions, qParser.root, false, false,qParser);
				t.add(temp);
			} else if (fromTableList.get(j) instanceof FromSubquery) {
				FromListElement temp =	OperateOnSubQuery.OperateOnSubquery((FromSubquery) fromTableList.get(j),qParser.allConds, qParser.root,true,false,qParser);		
				t.add(temp);
			}
		}

		qParser.queryAliases.setTabs(t);


		// Geting SelectionClause and JoinClause equalities
		ValueNode whereClause = ((SelectNode) rsNode).getWhereClause();
		if (whereClause != null) {
			Node whereClausePred = new Node();
			whereClausePred = WhereClauseVector.getWhereClauseVector(whereClause,null, qParser.queryAliases,true,0,qParser);

			//FIXME: Mahesh Add to sub query if subquery references
			//whereClausePred = modifyNode(whereClausePred);

			if( whereClausePred != null) qParser.allConds.add(whereClausePred);
			//Add join condition
			if(!JoinConditions.isEmpty()){
				Vector<Node> allCondsDups=(Vector<Node>) qParser.allConds.clone();
				for (Node cond : qParser.allConds){
					Node NewCond = new Node();
					NewCond.setType(Node.getAndNodeType());
					NewCond.setLeft(JoinConditions.get(0));
					NewCond.setRight(cond);
					allCondsDups.remove(cond);
					allCondsDups.add(NewCond);
				}
				qParser.allConds.removeAllElements();
				qParser.allConds.addAll(allCondsDups);
			}

		}
		else if(!JoinConditions.isEmpty()){
			if(!qParser.allConds.isEmpty()) {
				Node n1= JoinConditions.get(0);
				Node n2=qParser.allConds.get(0);
				Node n=new Node();
				n.setLeft(n1);
				n.setRight(n2);
				n.setType(Node.getAndNodeType());
				qParser.allConds.removeAllElements();
				qParser.allConds.add(n);
			} else {
			 qParser.allConds.addAll(JoinConditions);
			}
		}

		modifyTreeForCompareSubQ(qParser);

		// Getting EquivalenyClass Elements	
		//EquivalenceClass.makeEquivalenceClasses(qParser);//Method to get equivalence classes for outer query block and each sub query

		QueryParser.flattenAndSeparateAllConds(qParser);
		for(Conjunct conjunct:qParser.conjuncts){
			conjunct.createEqClass();
		}
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
		//qParser.EqClass.addAll(EquivalenceClass.createEqClass(qParser));

		// Getting Foreign Key into a vector of JoinClauseInfo object
		Util.foreignKeyClosure(qParser);

		if(qParser.isDeleteNode){
			return;
		}

		// Get Projected Columns in a list Vector<Node> projectionList: one
		// element for each expression in the projection list



		WhereClauseVector.getAggregationDataStructures(rsNode, qParser.queryAliases, qParser.getQuery().getFromTables(),false,false, qParser);
		


		if (debug) {
			logger.log(Level.INFO,"\nJoin Tables : " + qParser.getQuery().getJoinTables());
			logger.log(Level.INFO,"Undirected Join Graph : "
					+ qParser.getQuery().getJoinGraph());
		}

		//Add projected columns		
		ResultColumnList rcList = rsNode.getResultColumns();
		Vector<Node> tempProjectedCols = new Vector<Node>();
		for (int k=0;k<rcList.size();k++) {

			if (rcList.getNodeVector().get(k) instanceof AllResultColumn) {
				tempProjectedCols.addAll(Util.addAllProjectedColumns(qParser.queryAliases,0,qParser));
			}
			else if (rcList.getNodeVector().get(k) instanceof ResultColumn){				
				ResultColumn rc = (ResultColumn)rcList.getNodeVector().get(k);
				ValueNode exp = rc.getExpression();			

				//projectedCols.add(getWhereClauseVector(exp, queryAliases,false));
				tempProjectedCols.add(WhereClauseVector.getWhereClauseVector(exp,rc.getName(), qParser.queryAliases,false,0,qParser));


				if(qParser.isUpdateNode){				
					qParser.updateColumn.add(WhereClauseVector.getWhereClauseVector(rc.getReference(),rc.getName(), qParser.queryAliases,false,0,qParser));					
				}


			}
		}
		//ProjectedColumns.add(tempProjectedCols);
		qParser.projectedCols.addAll(tempProjectedCols);
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
	public static boolean caseInWhereClause(Expression whereClause, Expression colExpression, QueryParser qParser) throws Exception{
		
		Vector<CaseCondition> caseConditionsVector = new Vector<CaseCondition>();
		boolean isCaseExpr = false;
		boolean isCaseExists = false;
		try{
		if(whereClause instanceof CaseExpression){
			parsing.Column nodeColumnValue = null;
			 List<Expression> whenClauses = ((CaseExpression) whereClause).getWhenClauses();
			 for(int i=0;i < whenClauses.size();i++ ){
				
				CaseCondition cC = new CaseCondition();
				Node n = WhereClauseVectorJSQL.getWhereClauseVector(((WhenClause)((CaseExpression) whereClause).getWhenClauses().get(i)).getWhenExpression(),null, qParser.queryAliases,true,0,qParser);
				cC.setCaseConditionNode(n);
				cC.setCaseCondition(n.toString());
			    cC.setConstantValue(((WhenClause)((CaseExpression) whereClause).getWhenClauses().get(i)).getThenExpression().toString());
			    if(colExpression!= null && colExpression instanceof Column){
			    	Node n1 = ((WhereClauseVectorJSQL.getWhereClauseVector((colExpression),null, qParser.queryAliases,true,0,qParser)));
			    	cC.setColValueForConjunct(UtilsRelatedToNode.getColumn(n1));
			    	nodeColumnValue = UtilsRelatedToNode.getColumn(n1);
			    	cC.setCaseOperator("=");
			    }
			    /*if(cC.getColValueForConjunct() == null){
			    	
			    	nodeColumnValue = UtilsRelatedToNode.getColumn(cC.getCaseConditionNode());
			    	
			    	if(cC.getCaseConditionNode() != null && nodeColumnValue != null){
			    		cC.setColValueForConjunct(nodeColumnValue);
			    	}
			    	cC.setCaseOperator("=");
			    }*/
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
			    	cC.setColValueForConjunct(UtilsRelatedToNode.getColumn(n1));
			    }
			   /* if(cC.getColValueForConjunct() == null){
			    	
			    	cC.setColValueForConjunct(nodeColumnValue);
			    }*/
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
	//Modified for JSQL Exists - Start
private static RelationHierarchyNode generateRelationHierarchyJSQL(FromItem frmItem) throws StandardException{
		
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
	
	private static RelationHierarchyNode generateRelationHierarchyNodeForJoinJSQL(List<Join> joinsList,FromItem frmItem) throws StandardException{
		List<Join> leftTreeList = new ArrayList();
		List<Join> rightTreeList = new ArrayList();

		int index = 0;
		RelationHierarchyNode l = null;
		RelationHierarchyNode r = null;
		RelationHierarchyNode node = null;
		if(joinsList != null){

			Iterator joinsIt = joinsList.iterator();
			while(joinsIt.hasNext()){
				Join join = (Join)joinsIt.next();	
				index ++;
				rightTreeList.add(join);
				//Last item will be processed and other items will be
				//in leftTreeList for recursive processing
				if(index != (joinsList.size())){
					leftTreeList.add(join);
				}

			}
			if(leftTreeList != null && !leftTreeList.isEmpty() && leftTreeList.size() > 0){			
				l = generateRelationHierarchyNodeForJoinJSQL(leftTreeList,frmItem);
			}else if(leftTreeList != null && leftTreeList.size() == 0){
				l = generateRelationHierarchyJSQL(frmItem);
			}
			if(rightTreeList != null && ! rightTreeList.isEmpty() && rightTreeList.size() > 0){
				//Get the last Join item in the join list and process
				r = generateRelationHierarchyJSQL(rightTreeList.get(rightTreeList.size()-1).getRightItem());
			}
			node = new RelationHierarchyNode("_JOIN_", l, r);
		}//If joins not null - ends
		return node;
	}
	private static RelationHierarchyNode generateRelationHierarchyJSQL(PlainSelect plainsel) throws StandardException{
			RelationHierarchyNode node = null;
			FromItem prev = plainsel.getFromItem();
			boolean isOuter = false;
			Join joinType = null;
			if(plainsel.getJoins() != null){
				//If Join contains LEft outer join or right outer join - call genRelHierarchyforOuterJoin method				
				for(int i=0;i<plainsel.getJoins().size();i++){
					joinType = plainsel.getJoins().get(i);
					if(joinType.isOuter()){
						isOuter = true;
						break;
					}
				}
				if(!isOuter){
					node = generateRelationHierarchyNodeForJoinJSQL(plainsel.getJoins(), plainsel.getFromItem());
				}else{
					node = generateRelationHierarchyForOuterJoin(plainsel.getJoins(),  plainsel.getFromItem());
				}
				
			}
			else if(plainsel.getWhere() != null){
				
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
		
	private static RelationHierarchyNode generateRelationHierarchyForOuterJoin(List <Join> joinList,FromItem frmItem) throws StandardException{
		
		RelationHierarchyNode node = null;
		String joinType = "";
		Iterator joinsIt = joinList.iterator(); 
		int index = 0;
		boolean isLeftOuter= false;
		boolean isRightOuter = false;
		List<Join> leftTreeList = new ArrayList();
		List<Join> rightTreeList = new ArrayList();
		RelationHierarchyNode l = null;
		RelationHierarchyNode r = null;
		for(int i=0;i<joinList.size();i++){
					Join join = joinList.get(i);
					
					if (join.isOuter()) { // checks if join is
						// HalfOuterJoin
						if (join.isRight()){ // checks f join type is
							// right outer join
							joinType = JoinClauseInfo.rightOuterJoin;
							isRightOuter = true;
						}
						else{
							joinType = JoinClauseInfo.leftOuterJoin;
							isLeftOuter = true; 
						}
						//If it is LOJ or ROJ - divide the result join node list into 2
						//left with all previous join nodes and right with other following nodes
						//make it as  binary relation node is processed and call generate hierarchy
						
						leftTreeList = new ArrayList();
						rightTreeList = new ArrayList();
						for(int j=0;j<i;j++){
							Join leftJoinItems = joinList.get(j);	
							leftTreeList.add(leftJoinItems);
						}
						for(int k=i;k<joinList.size();k++){
							Join rightJoinItem = joinList.get(k); 
							rightTreeList.add(rightJoinItem);
						}
						if(leftTreeList.size() > 0){
						l = generateRelationHierarchyNodeForJoinJSQL(leftTreeList,frmItem);
						}
						if(rightTreeList.size()>0){
						
							r = generateRelationHierarchyNodeForJoinJSQL(rightTreeList,frmItem);
						}
						if(l == null && r != null){
							if(isRightOuter){
								r.nodeType="_RIGHT_JOIN_";
							}
							else if(isLeftOuter){
								r.nodeType = "_LEFT_JOIN_";
							}
							else{
								r.nodeType = "_JOIN_";
							}
							return r;
						}
						else if(l!= null && r == null){
							if(isRightOuter){
								l.nodeType="_RIGHT_JOIN_";
							}
							else if(isLeftOuter){
								l.nodeType = "_LEFT_JOIN_";
							}
							else{
								l.nodeType = "_JOIN_";
							}
							return l;
						} 
						//Create new jtn node with left and right and type as binary Relation node - 
						//with join type as Outer Join - end the method. 
						//break;
						
					} 
					if(l != null && r != null){
						if(isRightOuter){
							node = new RelationHierarchyNode("_RIGHT_JOIN_", l, r);
							r.nodeType="_RIGHT_JOIN_";
						}
						else if(isLeftOuter){
							node = new RelationHierarchyNode("_LEFT_JOIN_", l, r);
						}
						else{
							node = new RelationHierarchyNode("_JOIN_", l, r);
						}
					}
					
					break;
		}	
		return node;		
		
	}
	private static RelationHierarchyNode generateRelationHierarchy(QueryTreeNode rs) throws StandardException{
		
		RelationHierarchyNode node = null;
		
		if(rs instanceof FromBaseTable){
			node = new RelationHierarchyNode(((FromBaseTable) rs).getBaseTableName());
		}
		else if(rs instanceof HalfOuterJoinNode){
			HalfOuterJoinNode joinNode = (HalfOuterJoinNode)rs;
			boolean isRightOuter = joinNode.isRightOuterJoin();
			
			RelationHierarchyNode l = generateRelationHierarchy(joinNode.getLeftResultSet());
			RelationHierarchyNode r = generateRelationHierarchy(joinNode.getRightResultSet());
			if(isRightOuter){
				node = new RelationHierarchyNode("_RIGHT_JOIN_", l, r);
			}
			else{
				node = new RelationHierarchyNode("_LEFT_JOIN_", l, r);
			}
		}
		else if(rs instanceof JoinNode){
			JoinNode joinNode = (JoinNode)rs;
			
			RelationHierarchyNode l = generateRelationHierarchy(joinNode.getLeftResultSet());
			RelationHierarchyNode r = generateRelationHierarchy(joinNode.getRightResultSet());
			node = new RelationHierarchyNode("_JOIN_", l, r);
		}
		else if(rs instanceof SelectNode){
		
			SelectNode selectNode = (SelectNode) rs;
			Vector<QueryTreeNode> v = selectNode.getFromList().getNodeVector();
			
			for(QueryTreeNode i : v){
				node = generateRelationHierarchy(i);
			}

			List<RelationHierarchyNode> l = new ArrayList<RelationHierarchyNode>();
			ValueNode w = selectNode.getWhereClause();
			getAllNotExistsQueries(w, l);
			if(node!=null) {
				node.setNotExistsSubQueries(l);
			}
		}
		
		return node;
	}
	
	/**
	 * This method returns a Node with join conditions as node inside node for sending to conjuncts
	 * for conversion
	 * 
	 * @param joinNodeList
	 * @return
	 * @throws Exception
	 */
	public static Node getHierarchyOfJoinNode(Vector<Node> joinNodeList) throws Exception{
		Node nd = new Node();
		nd.setType(Node.getAndNodeType());
		if(joinNodeList != null &&  joinNodeList.size() > 0 && !joinNodeList.isEmpty()){
		 nd.setLeft(joinNodeList.get(0));
		}else
			nd.setLeft(null);
		Vector<Node> newList = new Vector<Node>();
		for(int i = 1 ; i < joinNodeList.size() ; i ++){
			newList.add(joinNodeList.get(i));
		}
		if(newList != null && newList.size()>0 && !newList.isEmpty()){
			nd.setRight(getHierarchyOfJoinNode(newList));
		}else{
			nd.setRight(null);
		}
		return nd;
		
	}
	private static void getAllNotExistsQueriesJSQL(Expression w, List<RelationHierarchyNode> l) throws StandardException{
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
				//int subQueryType = subQueryNode.getSubqueryType();
				//FIXME chk whether call to getRelationalHierarchy is reqd.
				//l.add(generateRelationHierarchyJSQL(ps));
				//if(subQueryType ==16 ||subQueryType ==15 ){ //EXISTS Type
				/*	SelectItem resultSet = (SelectItem)ps.getSelectItems().get(0);
					if(resultSet instanceof SelectExpressionItem){
						Expression exp1 = ((SelectExpressionItem)resultSet).getExpression();
						 RelationHierarchyNode rl = null; 
						 rl = new RelationHierarchyNode(tableName);
						 
						
					}*/
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
					
					 l.add(generateRelationHierarchyJSQL(ps));
					 
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
					
					 l.add(generateRelationHierarchyJSQL(ps));
					 
				}else{
					FromItem frm = ps.getFromItem();
					if(frm instanceof Table){
						 l.add(generateRelationHierarchyJSQL(frm));
					}
				}
			}
		}
	}
	
	private static void getAllNotExistsQueries(ValueNode w, List<RelationHierarchyNode> l) throws StandardException{
		if(w instanceof BinaryOperatorNode){
			BinaryOperatorNode whereNode = (BinaryOperatorNode) w;
			getAllNotExistsQueries(whereNode.getRightOperand(), l);
			getAllNotExistsQueries(whereNode.getLeftOperand(), l);
		}
		else if(w instanceof NotNode){
			NotNode notOp = (NotNode) w;
			ValueNode temp = notOp.getOperand();
			if(temp instanceof SubqueryNode){
				SubqueryNode subQueryNode = (SubqueryNode)temp;
				int subQueryType = subQueryNode.getSubqueryType();
				if(subQueryType ==16 ||subQueryType ==15 ){ //EXISTS Type
					QueryTreeNode resultSet = subQueryNode.getResultSet();
					l.add(generateRelationHierarchy(resultSet));
				}
			}
		}
	}
	
	public static void modifyTreeForCompareSubQ(parsing.QueryParser qParser) {
		try{
			for (Node n: qParser.allConds)  // This is not only for outer block 
				Util.modifyTreeForComapreSubQ(n);
	
			for(parsing.QueryParser qp: qParser.getFromClauseSubqueries()){//For From clause subqueries
				for(Node n: qp.allConds)
					Util.modifyTreeForComapreSubQ(n);
			}
	
			for(parsing.QueryParser qp: qParser.getWhereClauseSubqueries()){//For where clause subqueries
				for(Node n: qp.allConds)
					Util.modifyTreeForComapreSubQ(n);
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"Error in modifyTreeForCompareSubQ : "+e.getMessage(),e);			
		}
	}
}
