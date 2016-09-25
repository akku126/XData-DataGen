package parsing;

import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.iapi.types.BigIntegerDecimal;
import org.apache.derby.iapi.types.SQLInteger;
import org.apache.derby.impl.sql.compile.AggregateNode;
import org.apache.derby.impl.sql.compile.AndNode;
import org.apache.derby.impl.sql.compile.BetweenOperatorNode;
import org.apache.derby.impl.sql.compile.BinaryArithmeticOperatorNode;
import org.apache.derby.impl.sql.compile.BinaryRelationalOperatorNode;
import org.apache.derby.impl.sql.compile.CharConstantNode;
import org.apache.derby.impl.sql.compile.ColumnReference;
import org.apache.derby.impl.sql.compile.FromSubquery;
import org.apache.derby.impl.sql.compile.GroupByColumn;
import org.apache.derby.impl.sql.compile.GroupByList;
import org.apache.derby.impl.sql.compile.IsNullNode;
import org.apache.derby.impl.sql.compile.LikeEscapeOperatorNode;
import org.apache.derby.impl.sql.compile.NotNode;
import org.apache.derby.impl.sql.compile.NumericConstantNode;
import org.apache.derby.impl.sql.compile.OrNode;
import org.apache.derby.impl.sql.compile.ParameterNode;
import org.apache.derby.impl.sql.compile.QueryTreeNode;
import org.apache.derby.impl.sql.compile.ResultColumn;
import org.apache.derby.impl.sql.compile.ResultColumnList;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SelectNode;
import org.apache.derby.impl.sql.compile.SimpleStringOperatorNode;
import org.apache.derby.impl.sql.compile.SubqueryNode;
import org.apache.derby.impl.sql.compile.ValueNode;

import parsing.AggregateFunction;
import parsing.FromListElement;
import parsing.JoinTreeNode;
import parsing.Node;
import parsing.Table;

public class WhereClauseVector {
	
	private static Logger logger = Logger.getLogger(WhereClauseVector.class.getName());
	
	/**
	 * This method handles the aggregations in the where clause predicate
	 * 
	 * @param rsNode
	 * @param queryAliases
	 * @param fromTables
	 * @param fromSubquery
	 * @param whereSubquery
	 * @param qParser
	 * @throws Exception
	 */
	public static void getAggregationDataStructures(ResultSetNode rsNode,FromListElement queryAliases, Map<String,Table> fromTables,boolean fromSubquery,boolean whereSubquery, QueryParser qParser) throws Exception {

		// Get group by columns
		Vector< Node> tempGroupBy= new Vector< Node>();
		int queryType = 0;
		if(fromSubquery) queryType = 1;
		if(whereSubquery) queryType = 2;


		if (((SelectNode) rsNode).getGroupByList() != null) {
			GroupByList gbl = ((SelectNode) rsNode).getGroupByList();
			Vector<QueryTreeNode> vgbc = gbl.getNodeVector();
			for (int i = 0; i < vgbc.size(); i++) {
				GroupByColumn gbc = (GroupByColumn) vgbc.get(i);
				logger.log(Level.INFO, "GroupByColumn References in getAggregationDataStructures: "+ i+"th value " + gbc.getTableName() + "." + gbc.getColumnName());
				ColumnReference cr = (ColumnReference) gbc
						.getColumnExpression();
				logger.log(Level.INFO, "ColumnReferences for gbc in getAggregationDataStructures:"+ i + " = " + cr.getTableName() + "." + cr.getColumnName());
				logger.log(Level.INFO,"queryAliases" + queryAliases.getAliasName());

				Vector<Node> n = null;
				n = Util.getColumnListFromOccurenceInJC(cr.getColumnName(), cr
						.getTableName(), queryAliases, 1, qParser);
				if(n==null|| n.size()==0)//This raises because group by column may be an aliased column
				{//FIXME: MAHESH CHANGE THE BELOW CODE TO USE BUILT IN FUNCTIONS
					String colName=cr.getColumnName();
					logger.log(Level.INFO,"Aliased gbc column- "+" as "+colName.toLowerCase());
					
					String []subQuerySelect=qParser.getQuery().getQueryString().toLowerCase().split("as "+colName.toLowerCase());
					colName=subQuerySelect[0].toLowerCase().trim().toLowerCase();
					int lastIndex=colName.lastIndexOf(' ');

					logger.log(Level.INFO,"last: "+lastIndex+" colName: "+colName);
					colName=colName.substring(lastIndex+1);
					logger.log(Level.INFO,"last: "+lastIndex+" colName: "+colName);
					Node n1;
					if(colName.contains("max")||colName.contains("count")||colName.contains("sum")||colName.contains("avg")||colName.contains("min")){
						//convert this colName into aggregate node type and call getWhereClause()
						AggregateFunction af = new AggregateFunction();

						String aggName=colName.split("\\(")[0].trim();
						logger.log(Level.INFO,"agg: "+aggName);
						af.setFunc(aggName.toUpperCase());
						colName=colName.substring(colName.indexOf("(")+1,colName.indexOf(")"));
					}
					colName=colName.toUpperCase();
					n=Util.getColumnListFromOccurenceInJC(colName,cr.getTableName(), queryAliases,1,qParser);

				}

				for(Node nSingle : n)
				{
					//Storing sub query details
					nSingle.setQueryType(queryType);
					if(queryType == 1) nSingle.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
					if(queryType == 2) nSingle.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

					logger.log(Level.INFO,"nSingle.getTable() " + nSingle.getTable());
					//groupByNodes.add(nSingle);
					//FIXME: Mahesh- Add the group by nodes to its list
					tempGroupBy.add(nSingle);
				}		

			}
		}
		if(whereSubquery)
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).groupByNodes = tempGroupBy;//.add(tempGroupBy);//FIXME: mahesh change this
		else if(fromSubquery)
			qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).groupByNodes = tempGroupBy;//add(tempGroupBy);
		else
			qParser.groupByNodes = tempGroupBy;//.add(tempGroupBy);

		//Get Aggregations
		ResultColumnList rcList = rsNode.getResultColumns();
		Vector<AggregateFunction> tempAggFunc=new Vector<AggregateFunction>();
		for(int i=0;i<rcList.size();i++){

			if(rcList.getNodeVector().get(i) instanceof ResultColumn){

				ResultColumn rc = (ResultColumn)rcList.getNodeVector().get(i);
				logger.log(Level.INFO,"ResultColumn rc-" + i + " = " + rc.getColumnName());

				if(rc.getExpression() instanceof AggregateNode){					
					AggregateFunction af = new AggregateFunction();
					AggregateNode an = (AggregateNode)rc.getExpression();
					String aggName = an.getAggregateName();
					QueryTreeNode qtn = an.getOperand();
					Node n = WhereClauseVector.getWhereClauseVector(qtn, null,queryAliases,false, queryType,qParser);//mahesh: change null to actual name
					af.setAggExp(n);
					af.setFunc(aggName);
					af.setDistinct(an.isDistinct());
					af.setAggAliasName(rc.getName());
					tempAggFunc.add(af);
				}
			}
		}
		//add this to list
		if(whereSubquery)
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).aggFunc = tempAggFunc;//.add(tempAggFunc);
		else if(fromSubquery)
			qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).aggFunc = tempAggFunc;//.add(tempAggFunc);
		else
			qParser.aggFunc = tempAggFunc;//.add(tempAggFunc);
		
		// get having clause
		ValueNode hc = ((SelectNode) rsNode).havingClause;
		//havingClause = getWhereClauseVector(hc, queryAliases,false);
		//add this to having list
		//FIXME: Mahesh Add to sub query if subquery references
		Node n = WhereClauseVector.getWhereClauseVector(hc, null,queryAliases,false,queryType,qParser);

		if(whereSubquery)
			//this.WhereClauseSubqueries.get(WhereClauseSubqueries.size()-1).HavingClause.add(getWhereClauseVector(hc, null,queryAliases,false));
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).havingClause = n;
		else if(fromSubquery)
			qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).havingClause = n;
		else{

			n = Util.modifyNode(n,qParser);
			qParser.havingClause = n;
		}

	}
	/**
	 * This method takes the where object as input and finds the type of object
	 * and converts them to Node structure for processing and data generation 
	 * 
	 * @param clause
	 * @param exposedName
	 * @param fle
	 * @param isWhereClause
	 * @param queryType
	 * @param qParser
	 * @return
	 * @throws Exception
	 */
	public static Node getWhereClauseVector(Object clause, String exposedName, FromListElement fle,boolean isWhereClause, int queryType, QueryParser qParser)
			throws Exception {
	try{
		if (clause == null) {
			return null;
		} else if (clause instanceof AggregateNode) {
			AggregateNode an = (AggregateNode)clause;
			String aggName = an.getAggregateName();
			ColumnReference cr = (ColumnReference)an.getOperand();
			AggregateFunction af = new AggregateFunction();
			QueryTreeNode qtn = an.getOperand();
			Node n = getWhereClauseVector(qtn,null, fle, isWhereClause, queryType, qParser);
			af.setAggExp(n);
			af.setFunc(aggName);
			af.setDistinct(an.isDistinct());
			af.setAggAliasName(exposedName);

			Node agg = new Node();
			agg.setAgg(af);
			agg.setType(Node.getAggrNodeType());

			//Storing sub query details
			agg.setQueryType(queryType);
			if(queryType == 1) agg.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) agg.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			//Adding this to the list of aliased names
			if(exposedName !=null){
				Vector<Node> present = new Vector<Node>();
				if( qParser.getAliasedToOriginal().get(exposedName) != null)
					present = qParser.getAliasedToOriginal().get(exposedName);
				present.add(agg);
				qParser.getAliasedToOriginal().put(exposedName, present);
			}

			return agg;

		} else if (clause instanceof NumericConstantNode) {
			if (((NumericConstantNode) clause).getValue() instanceof SQLInteger) {
				Node n = new Node();
				n.setType(Node.getValType());
				n
				.setStrConst(new Integer(
						((SQLInteger) ((NumericConstantNode) clause)
								.getValue()).getInt()).toString());
				n.setLeft(null);
				n.setRight(null);
				return n;
			} else if (((NumericConstantNode) clause).getValue() instanceof BigIntegerDecimal) {
				Node n = new Node();
				n.setType(Node.getValType());
				String s=((((NumericConstantNode)clause).getValue()).getDouble())+"";
				//String str=(BigIntegerDecimal)((((NumericConstantNode) clause).getValue()).getDouble()).toString();
				s=util.Utilities.covertDecimalToFraction(s);
				n.setStrConst(s);
				n.setLeft(null);
				n.setRight(null);
				return n;
			}

		} else if (clause instanceof CharConstantNode) {
			Node n = new Node();
			n.setType(Node.getValType());
			n.setStrConst(((CharConstantNode) clause).getString());
			//n.setStrConst("'"+((CharConstantNode) clause).getString()+"'");
			n.setLeft(null);
			n.setRight(null);
			return n;
		} else if (clause instanceof ColumnReference) {
			ColumnReference columnReference = (ColumnReference) clause;
			Node n = new Node();
			String colName= columnReference.getColumnName();			
			String tableName = columnReference.getTableName();

			if(qParser.getQuery().getQueryString().toLowerCase().contains(("as "+colName.toLowerCase()))){//Handling aliased columns
				//			String subQueryRefrence=columnReference.getTableName();

				Vector< Node > value = qParser.getAliasedToOriginal().get(colName);
				n = value.get(0);//FIXME: vector of nodes not a single node
				return n;

			}			
			else{
				n = Util.getColumnFromOccurenceInJC(colName,columnReference.getTableName(), fle, qParser);
				if (n == null) {//then probably the query is correlated				
					n = Util.getColumnFromOccurenceInJC(colName,columnReference.getTableName(), qParser.getQueryAliases(),qParser);
				}	
			}
			if(n == null) {
				logger.log(Level.WARNING,"WhereClauseVectorJSQL : Util.getColumnFromOccurenceInJC is not able to find matching Column - Node n = null");
				return null;
			}
			n.setType(Node.getColRefType());
			if (columnReference.getTableName() != null) {
				n.setTableAlias(columnReference.getTableName());
			} else {
				n.setTableAlias("");
			}

			if(n.getColumn() != null){
				n.getColumn().setAliasName(exposedName);
				n.setTable(n.getColumn().getTable());
			}

			n.setLeft(null);
			n.setRight(null);


			//Storing sub query details
			if(qParser.getSubQueryNames().containsKey(tableName)){//If this node is inside a sub query
				n.setQueryType(1);
				n.setQueryIndex(qParser.getSubQueryNames().get(tableName));
			}
			else if(qParser.getTableNames().containsKey(tableName)){
				n.setQueryType(qParser.getTableNames().get(tableName)[0]);
				n.setQueryIndex(qParser.getTableNames().get(tableName)[1]);
			}
			else{
				n.setQueryType(queryType);
				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			}
			if(exposedName !=null){
				Vector<Node> present = new Vector<Node>();
				if( qParser.getAliasedToOriginal().get(exposedName) != null)
					present = qParser.getAliasedToOriginal().get(exposedName);
				present.add(n);
				qParser.getAliasedToOriginal().put(exposedName, present);
			}
			return n;

		} else if (clause instanceof AndNode) {
			AndNode andNode = ((AndNode) clause);
			if (andNode.getLeftOperand() != null
					&& andNode.getRightOperand() != null) {
				Node n = new Node();
				Node left = new Node();
				Node right = new Node();
				n.setType(Node.getAndNodeType());
				n.setOperator("AND");
				left = getWhereClauseVector(andNode.getLeftOperand(), exposedName, fle, isWhereClause, queryType,qParser);
				right = getWhereClauseVector(andNode.getRightOperand(), exposedName, fle, isWhereClause, queryType,qParser);
				/*
				//Mahesh
				if(left.queryType != 0){//If column is an aliased column of sub query
				    //If aggregate then add to havingClause of sub query
					addToSubQuery(left);
					left = null;
				}

				if(right.queryType != 0){//If column is an aliased column of sub query
				    //If aggregate then add to havingClause of sub query
					addToSubQuery(right);
					right=null;
				}

				if(left == null)
					return right;
				if(right == null)
					return left;
				 */
				n.setLeft(left);
				n.setRight(right);

				//Storing sub query details
				n.setQueryType(queryType);
				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
			}

		} else if (clause instanceof OrNode) {
			OrNode orNode = ((OrNode) clause);
			if (orNode.getLeftOperand() != null
					&& orNode.getRightOperand() != null) {
				Node n = new Node();
				n.setType(Node.getOrNodeType());
				n.setOperator("OR");
				n.setLeft(getWhereClauseVector(orNode.getLeftOperand(), exposedName, fle, isWhereClause, queryType,qParser));

				n.setRight(getWhereClauseVector(orNode.getRightOperand(), exposedName, fle, isWhereClause, queryType,qParser));

				//Storing sub query details
				n.setQueryType(queryType);
				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
			}
		} else if (clause instanceof BinaryRelationalOperatorNode) {
			BinaryRelationalOperatorNode broNode = ((BinaryRelationalOperatorNode) clause);			
			Node n = new Node();
			n.setType(Node.getBroNodeType());
			n.setOperator(QueryParser.cvcRelationalOperators[broNode.getOperator()]);
			n.setLeft(getWhereClauseVector(broNode.getLeftOperand(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(broNode.getRightOperand(), exposedName, fle, isWhereClause, queryType,qParser));

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			/*
			//FIXME: Mahesh when original subquery calls this then it is added to havingClause of sub query 
			if(n.getRight().getQueryType() != 0 || n.getLeft().getQueryType() != 0){//Aliased column name is used in outer query
				addToSubQuery(n);
				n = null;
			}*/
			return n;
		} 

		//Added by Bikash ---------------------------------------------------------------------------------
		else if(clause instanceof LikeEscapeOperatorNode){
			LikeEscapeOperatorNode likeNode=((LikeEscapeOperatorNode)clause);
			if (likeNode.getReceiver() !=null && likeNode.getLeftOperand()!=null )
			{
				//if(likeNode.getReceiver() instanceof ColumnReference && (likeNode.getLeftOperand() instanceof CharConstantNode || likeNode.getLeftOperand() instanceof ParameterNode))
				{
					Node n=new Node();
					n.setType(Node.getLikeNodeType());
					n.setOperator("~");
					n.setLeft(getWhereClauseVector(likeNode.getReceiver(), exposedName, fle, isWhereClause, queryType,qParser));
					n.setRight(getWhereClauseVector(likeNode.getLeftOperand(), exposedName, fle, isWhereClause, queryType,qParser));
					/*
					//Mahesh
					if(n.getRight().getQueryType() != 0 || n.getLeft().getQueryType() != 0){//Aliased column name is used in outer query
						addToSubQuery(n);
						n = null;
					}
					 */
					//Storing sub query details
					n.setQueryType(queryType);
					if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
					if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					return n;
				}
			}
		}

		else if(clause instanceof ParameterNode){
			Node n = new Node();
			n.setType(Node.getValType());		
			n.setStrConst("$"+qParser.paramCount);
			qParser.paramCount++;
			n.setLeft(null);
			n.setRight(null);

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			return n;
		}

		//**********************************************************************************/
		else if (clause instanceof BinaryArithmeticOperatorNode) {
			BinaryArithmeticOperatorNode baoNode = ((BinaryArithmeticOperatorNode) clause);
			Node n = new Node();
			n.setType(Node.getBaoNodeType());
			n.setOperator(baoNode.getOperator1());
			n.setLeft(getWhereClauseVector(baoNode.getLeftOperand(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(baoNode.getRightOperand(), exposedName, fle, isWhereClause, queryType,qParser));
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		}
		/*
		 * else if (clause instanceof InListOperatorNode) { //In List: Where we
		 * have a list of concrete values within parenthesis }
		 */
		else if (clause instanceof NotNode) {
			NotNode nn = ((NotNode) clause);
			Node n = new Node();
			n.setType(Node.getNotNodeType());
			n.setLeft(getWhereClauseVector(nn.getOperand(), exposedName,fle,isWhereClause, queryType,qParser));
			n.setRight(null);
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		} else if (clause instanceof IsNullNode) {
			IsNullNode isNullNode = (IsNullNode) clause;
			Node n = new Node();
			n.setType(Node.getIsNullNodeType());
			n.setLeft(getWhereClauseVector(isNullNode.getOperand(),exposedName, fle,isWhereClause, queryType,qParser));
			n.setOperator("=");
			n.setRight(null);
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		} else if (clause instanceof SubqueryNode) {

			SubqueryNode sqn = (SubqueryNode) clause;
			if (sqn.getSubqueryType() == 1 || sqn.getSubqueryType() == 2) { // IN
				// SubQuery
				// Type
				Node lhs = getWhereClauseVector(sqn.getLeftOperand(),exposedName, fle,isWhereClause, queryType,qParser);
				Vector<Node> thisSubQConds = new Vector<Node>();
				FromSubquery subq = new FromSubquery();
				subq.setSubquery(sqn.getResultSet());
				//FromListElement fle1 = OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode());
				//FIXME: mahesh changed this.But not sure
				OperateOnSubQuery.OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode(),false,isWhereClause,qParser);
				FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();
				// Extract the projected column and create a colref node
				ResultColumnList rcl = subq.getSubquery().getResultColumns();
				ResultColumn rc = (ResultColumn) rcl.getNodeVector().get(0);
				ColumnReference cr ;
				String aggName="" ;
				AggregateNode an=new AggregateNode() ;
				//mahesh chane
				ValueNode exp = rc.getExpression();
				if( exp instanceof AggregateNode ){
					an = (AggregateNode)exp;
					aggName = an.getAggregateName();
					cr = (ColumnReference)an.getOperand();
				}
				else
					cr = (ColumnReference) exp;
				Node rhs;
				//rhs= getWhereClauseVector(exp, fle,isWhereClause);


				rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr
						.getTableName(), fle1, qParser);
				if (rhs == null) {
					rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr
							.getTableName(), qParser.getQueryAliases(),qParser);
				}
				rhs.setType(Node.getColRefType());
				if (cr.getTableName() != null) {
					rhs.setTableAlias(cr.getTableName());
				} else {
					rhs.setTableAlias("");
				}

				if( exp instanceof AggregateNode ){
					AggregateFunction af = new AggregateFunction();
					af.setAggExp(rhs);
					af.setFunc(aggName);
					af.setDistinct(an.isDistinct());
					Node rhs1= new Node();
					rhs1.setAgg(af);
					rhs1.setType(Node.getAggrNodeType());
					rhs1.setTableAlias(rhs.getTableAlias());
					rhs1.setColumn(rhs.getColumn());
					rhs1.setTableNameNo(rhs.getTableNameNo());
					rhs1.setTable(rhs.getTable());
					rhs=rhs1;
				}

				rhs.setQueryType(2);
				rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				Node cond = new Node();
				cond.setType(Node.getBroNodeType());
				cond.setLeft(lhs);
				cond.setRight(rhs);
				cond.setOperator("=");
				cond.setAgg(rhs.getAgg());
				// create the final subquery node and return it
				Node sqNode = new Node();
				sqNode.setType(Node.getInNodeType());
				sqNode.setSubQueryConds(thisSubQConds);
				sqNode.setLhsRhs(cond);
				return sqNode;
				// subQueryConds.add(rhs);
				// subQueryConds.add(thisSubQConds);
				// return null;
			} else if (sqn.getSubqueryType() >= 3
					&& sqn.getSubqueryType() <= 14 && sqn.getSubqueryType()%2==1) { 
				// (=/>/</..) ANY SubQury Type , modulo condition added by mathew on 29 June 2016
				Node lhs = getWhereClauseVector(sqn.getLeftOperand(), exposedName,fle,isWhereClause, queryType,qParser);
				Vector<Node> thisSubQConds = new Vector<Node>();
				FromSubquery subq = new FromSubquery();
				subq.setSubquery(sqn.getResultSet());
				//FromListElement fle1 = OperateOnSubquery(subq, thisSubQConds ,new JoinTreeNode(),this.FromClauseSubqueries, !isWhereClause);//FIXME JoinTreeNode is a patch. Have to pass it here, but we won't use this because its in the where clause subquery.

				//FIXME: mahesh changed this.But not sure
				OperateOnSubQuery.OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode(),false, isWhereClause,qParser);
				FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();


				// Extract the projected column and create a colref node
				ResultColumnList rcl = subq.getSubquery().getResultColumns();
				ResultColumn rc = (ResultColumn) rcl.getNodeVector().get(0);
				ColumnReference cr = (ColumnReference) rc.getExpression();
				Node rhs;
				rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr
						.getTableName(), fle1, qParser);
				if (rhs == null) {
					rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr
							.getTableName(), qParser.getQueryAliases(), qParser);
				}
				rhs.setType(Node.getColRefType());
				// create the condition node as X = Y for X in (select Y from
				// ...)

				rhs.setQueryType(2);
				rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				Node cond = new Node();
				cond.setType(Node.getBroNodeType());
				cond.setLeft(lhs);
				cond.setRight(rhs);

				String operator = sqn.subQType(sqn.getSubqueryType());
				cond.setOperator(operator.substring(0, 1));
				//cond.setOperator("=");

				// create the final subquery node and return it
				Node sqNode = new Node();
				/*The following commented by mathew on 29 June 2016 and the line below 
				 * added for distinguishing all nodes from any nodes
				 */ 
				//sqNode.setType(Node.getAllAnyNodeType());
				sqNode.setType(Node.getAnyNodeType());
				sqNode.setSubQueryConds(thisSubQConds);
				sqNode.setLhsRhs(cond);
				return sqNode;
			}
			else if (sqn.getSubqueryType() >= 3
					&& sqn.getSubqueryType() <= 14 && sqn.getSubqueryType()%2==0) { 
				// (=/>/</..) ALL SubQury Type, modulo condition added by mathew on 29 June 2016
				Node lhs = getWhereClauseVector(sqn.getLeftOperand(), exposedName,fle,isWhereClause, queryType,qParser);
				Vector<Node> thisSubQConds = new Vector<Node>();
				FromSubquery subq = new FromSubquery();
				subq.setSubquery(sqn.getResultSet());
				//FromListElement fle1 = OperateOnSubquery(subq, thisSubQConds ,new JoinTreeNode(),this.FromClauseSubqueries, !isWhereClause);//FIXME JoinTreeNode is a patch. Have to pass it here, but we won't use this because its in the where clause subquery.

				//FIXME: mahesh changed this.But not sure
				OperateOnSubQuery.OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode(),false, isWhereClause,qParser);
				FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();


				// Extract the projected column and create a colref node
				ResultColumnList rcl = subq.getSubquery().getResultColumns();
				ResultColumn rc = (ResultColumn) rcl.getNodeVector().get(0);
				ColumnReference cr = (ColumnReference) rc.getExpression();
				Node rhs;
				rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr
						.getTableName(), fle1, qParser);
				if (rhs == null) {
					rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr
							.getTableName(), qParser.getQueryAliases(), qParser);
				}
				rhs.setType(Node.getColRefType());
				// create the condition node as X = Y for X in (select Y from
				// ...)

				rhs.setQueryType(2);
				rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				Node cond = new Node();
				cond.setType(Node.getBroNodeType());
				cond.setLeft(lhs);
				cond.setRight(rhs);

				String operator = sqn.subQType(sqn.getSubqueryType());
				cond.setOperator(operator.substring(0, 1));
				//cond.setOperator("=");

				// create the final subquery node and return it
				Node sqNode = new Node();
				/*The following commented by mathew on 29 June 2016 and the line below 
				 * added for distinguishing all nodes from any nodes
				 */ 
				//sqNode.setType(Node.getAllAnyNodeType());
				sqNode.setType(Node.getAllNodeType());
				sqNode.setSubQueryConds(thisSubQConds);
				sqNode.setLhsRhs(cond);
				return sqNode;
			} else if (sqn.getSubqueryType() == 15
					|| sqn.getSubqueryType() == 16) { // EXISTS SubQuery Type
				Node lhs = getWhereClauseVector(sqn.getLeftOperand(),exposedName, fle,isWhereClause, queryType,qParser);
				Vector<Node> thisSubQConds = new Vector<Node>();
				FromSubquery subq = new FromSubquery();
				subq.setSubquery(sqn.getResultSet());
				//FromListElement fle1 = OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode(),this.FromClauseSubqueries, !isWhereClause);
				//FIXME: mahesh changed this.But not sure
				OperateOnSubQuery.OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode(),false,isWhereClause,qParser);
				FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();
				Node sqNode = new Node();
				sqNode.setType(Node.getExistsNodeType());
				sqNode.setSubQueryConds(thisSubQConds);
				Node rhs = new Node();
				rhs.setQueryType(2);
				rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				sqNode.setLhsRhs(rhs);
				return sqNode;
				
			}else if (sqn.getSubqueryType() >=17
					|| sqn.getSubqueryType() <= 22) {
				//added by bikash
				//Node lhs = getWhereClauseVector(sqn.getLeftOperand(), fle);
				Vector<Node> thisSubQConds = new Vector<Node>();
				FromSubquery subq = new FromSubquery();
				subq.setSubquery(sqn.getResultSet());

				//FromListElement fle1 = OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode(),this.FromClauseSubqueries, !isWhereClause);

				//FIXME: mahesh changed this.But not sure
				OperateOnSubQuery.OperateOnSubquery(subq, thisSubQConds,new JoinTreeNode(),false,isWhereClause,qParser);
				FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();


				ResultColumnList rcl = subq.getSubquery().getResultColumns();
				ResultColumn rc = (ResultColumn) rcl.getNodeVector().get(0);
				if(rc.getExpression() instanceof AggregateNode){
					sqn.getResultSet();
					//Node rhs=getWhereClauseVector((AggregateNode)rc.getExpression(),fle1);

					AggregateNode an = (AggregateNode)rc.getExpression();
					String aggName = an.getAggregateName();
					ColumnReference cr = (ColumnReference)an.getOperand();
					AggregateFunction af = new AggregateFunction();
					QueryTreeNode qtn = an.getOperand();
					Node n = getWhereClauseVector(qtn, exposedName,fle1,isWhereClause, 2, qParser);
					af.setAggExp(n);
					af.setFunc(aggName);
					af.setDistinct(an.isDistinct());


					Node rhs = new Node();
					rhs.setAgg(af);
					rhs.setType(Node.getAggrNodeType());

					rhs.setQueryType(2);
					rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

					//Node cond = new Node();
					//cond.setType(Node.getBroNodeSubQType());
					//cond.setLeft(lhs);
					//cond.setRight(rhs);
					String operator = sqn.subQType(sqn.getSubqueryType());
					//cond.setOperator(operator);
					// create the final subquery node and return it
					Node sqNode = new Node();
					sqNode.setType(Node.getBroNodeSubQType());
					sqNode.setSubQueryConds(thisSubQConds);
					sqNode.setLhsRhs(rhs);
					return sqNode;
				}
				else if(rc.getExpression() instanceof ColumnReference){
					//the result of subquery must be a single tuple
					logger.log(Level.WARNING,"the result of subquery must be a single tuple");
				}
			}
		}
		else if(clause instanceof SimpleStringOperatorNode){
			SimpleStringOperatorNode sso=(SimpleStringOperatorNode) clause;
			Node n=new Node();
			n.setOperator(sso.getOperatorString());
			n.setLeft(getWhereClauseVector(sso.getOperand(),exposedName, fle,isWhereClause, queryType,qParser));
			n.setType(Node.getStringFuncNodeType());

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			if( n.getLeft().getQueryType() != 0){//Aliased column name is used in outer query
				Util.addToSubQuery(n,qParser);
				n = null;
			}

			return n;
		}
		else if(clause instanceof BetweenOperatorNode){

			//FIXME: Mahesh If aggregate in where (due to aliased) then add to list of having clause of the subquery


			BetweenOperatorNode bn=(BetweenOperatorNode)clause;
			Node n=new Node();
			n.setType(Node.getAndNodeType());
			Node l=new Node();
			l.setLeft(getWhereClauseVector(bn.getLeftOperand(),exposedName,fle,isWhereClause, queryType,qParser));
			l.setOperator(">=");
			l.setRight(getWhereClauseVector(bn.getRightOperandList().getNodeVector().get(0),exposedName,fle,isWhereClause, queryType,qParser));
			l.setType(Node.getBroNodeType());
			n.setLeft(l);

			Node r=new Node();
			r.setLeft(getWhereClauseVector(bn.getLeftOperand(),exposedName,fle,isWhereClause, queryType,qParser));
			r.setOperator("<=");
			r.setRight(getWhereClauseVector(bn.getRightOperandList().getNodeVector().get(1),exposedName,fle,isWhereClause, queryType,qParser));
			r.setType(Node.getBroNodeType());
			n.setRight(r);

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			return n;
		}
		else {
			logger.log(Level.SEVERE,"getWhereClauseVector needs more programming ");
			throw new Exception("getWhereClauseVector needs more programming ");
		}
	}catch(Exception e){
		logger.log(Level.SEVERE,e.getMessage(),e);
		throw e;
	}
	return null;
	}
}
