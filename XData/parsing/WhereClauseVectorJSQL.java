package parsing;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.impl.sql.compile.ColumnReference;

import parsing.AggregateFunction;
import parsing.FromListElement;
import parsing.JoinTreeNode;
import parsing.Node;
import parsing.Table;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.DoubleAnd;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
//import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

public class WhereClauseVectorJSQL {
	private static Logger logger = Logger.getLogger(WhereClauseVectorJSQL.class.getName()); 
	

	/** @author mathew on 18 June 2016
	 * This method handles the order by clause 
	 * 
	 * @param plainSelectStmt
	 * @param queryAliases
	 * @param fromTables
	 * @param fromSubquery
	 * @param whereSubquery
	 * @param qParser
	 * @throws Exception
	 */
	public static void getOrderByList(PlainSelect plainSelectStmt,FromListElement queryAliases, Map<String,Table> fromTables,boolean fromSubquery,boolean whereSubquery, QueryParser qParser) throws Exception {
		Vector< Node> tempOrderBy= new Vector< Node>();
		int queryType = 0;
		if(fromSubquery) queryType = 1;
		if(whereSubquery) queryType = 2;
		
		if (plainSelectStmt.getOrderByElements() != null) {
			List<OrderByElement> obl = plainSelectStmt.getOrderByElements();
			for (int i = 0; i < obl.size(); i++) {
				Column obc;
				Expression orderExpression=obl.get(i).getExpression();

				if (orderExpression instanceof Column){
					obc = (Column)orderExpression;
				} else {
					continue;
				}
				logger.log(Level.INFO, "OrderByColumn References: "+ i+"th value " + obc.getTable() + "." + obc.getColumnName());
				logger.log(Level.INFO,"queryAliases" + queryAliases.getAliasName());

				Node n = null;
				n = Util.getColumnFromOccurenceInJC(obc.getColumnName(), obc.getTable().getFullyQualifiedName().toUpperCase(), queryAliases, 1, qParser);
				if(n==null)//This raises because order by column may be an aliased column
				{
					//String colName=cr.getColumnName();
					String colName=obc.getColumnName();
					logger.log(Level.INFO,"Aliased obc column- "+" as "+colName.toLowerCase());

					String []subQuerySelect=qParser.getQuery().getQueryString().toLowerCase().split("as "+colName.toLowerCase());
					colName=subQuerySelect[0].toLowerCase().trim().toLowerCase();
					int lastIndex=colName.lastIndexOf(' ');
					logger.log(Level.INFO,"last: "+lastIndex+" colName: "+colName);
					colName=colName.substring(lastIndex+1);
					logger.log(Level.INFO,"last: "+lastIndex+" colName: "+colName);

					if(colName.contains("max")||colName.contains("count")||colName.contains("sum")||colName.contains("avg")||colName.contains("min")){
						//convert this colName into aggregate node type and call getWhereClause()
						AggregateFunction af = new AggregateFunction();

						String aggName=colName.split("\\(")[0].trim();
						logger.log(Level.INFO,"agg: "+aggName);
						af.setFunc(aggName.toUpperCase());
						colName=colName.substring(colName.indexOf("(")+1,colName.indexOf(")"));
					}
					colName=colName.toUpperCase();									
					n=Util.getColumnFromOccurenceInJC(colName,obc.getTable().getFullyQualifiedName().toUpperCase(), queryAliases,1,qParser);
				}

				//Storing sub query details
				n.setQueryType(queryType);
				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				logger.log(Level.INFO,"nSingle.getTable() " + n);
				//groupByNodes.add(nSingle);
				tempOrderBy.add(n);

			}
		}
		
		if(whereSubquery)
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).orderByNodes = tempOrderBy;//.add(tempGroupBy);//FIXME: mahesh change this
		else if(fromSubquery)
			qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).orderByNodes = tempOrderBy;//add(tempGroupBy);
		else
			qParser.orderByNodes = tempOrderBy;//.add(tempGroupBy);
	}

	
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
	public static void getAggregationDataStructures(PlainSelect rsNode,FromListElement queryAliases, Map<String,Table> fromTables,boolean fromSubquery,boolean whereSubquery, QueryParser qParser) throws Exception {

		// Get group by columns
		Vector< Node> tempGroupBy= new Vector< Node>();
		int queryType = 0;
		if(fromSubquery) queryType = 1;
		if(whereSubquery) queryType = 2;


		if (rsNode.getGroupByColumnReferences() != null) {
			List<Expression> gbl = rsNode.getGroupByColumnReferences();
			for (int i = 0; i < gbl.size(); i++) {
				Column gbc;
				
				if (gbl.get(i) instanceof Column){
					gbc = (Column)gbl.get(i);
				} else {
					continue;
				}
				logger.log(Level.INFO, "GroupByColumn References in getAggregationDataStructures: "+ i+"th value " + gbc.getTable() + "." + gbc.getColumnName());
				logger.log(Level.INFO,"queryAliases" + queryAliases.getAliasName());

				Vector<Node> n = null;
				//n = Util.getColumnListFromOccurenceInJC(cr.getColumnName(), cr.getTableName(), queryAliases, 1, qParser);
				n = Util.getColumnListFromOccurenceInJC(gbc.getColumnName(), gbc.getTable().getFullyQualifiedName().toUpperCase(), queryAliases, 1, qParser);
				if(n==null|| n.size()==0)//This raises because group by column may be an aliased column
				{//FIXME: MAHESH CHANGE THE BELOW CODE TO USE BUILT IN FUNCTIONS
					//String colName=cr.getColumnName();
					String colName=gbc.getColumnName();
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
					n=Util.getColumnListFromOccurenceInJC(colName,gbc.getTable().getFullyQualifiedName().toUpperCase(), queryAliases,1,qParser);

				}

				for(Node nSingle : n)
				{
					//Storing sub query details
					nSingle.setQueryType(queryType);
					if(queryType == 1) nSingle.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
					if(queryType == 2) nSingle.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					logger.log(Level.INFO,"nSingle.getTable() " + nSingle.getTable()+" node "+nSingle);
					//groupByNodes.add(nSingle);
					//FIXME: Mahesh- Add the group by nodes to its list
					tempGroupBy.add(nSingle);
				}		

			}
		/*	try{
			//If the existing group by attributes have foreign key relation, add referenced table's 
			 // columns also in the groupby nodes
			for(Node each: tempGroupBy){
				parsing.Column colName = each.getColumn();
				Table originalTable = colName.getTable();
				if(originalTable.hasForeignKey()){
					for (String fKeyName : originalTable.getForeignKeys().keySet()) {
						
						ForeignKey fKey = originalTable.getForeignKey(fKeyName);
						Vector<parsing.Column> fKeyColumns = fKey.getFKeyColumns();
						Vector<Node> nd = null;
						for(parsing.Column fk : fKeyColumns ){
							if(!(each.getColumn().getColumnName().equalsIgnoreCase(fk.getColumnName()))
									&& !(each.getColumn().equals(fKey.getReferenceKeyColumns()))){
								for(parsing.Column c : fKey.getReferenceKeyColumns()){
									//tempGroupBy.add(c.getReferenceColumn());
										nd=Util.getColumnListFromOccurenceInJC(c.getColumnName(),each.getTable().getTableName().toUpperCase(), queryAliases,1,qParser);
										for(Node nSingle : nd)
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
								break;
							}
						}
						
						
					}
				}
			}
			//ends
			}catch(Exception e){
				e.printStackTrace();
			}*/
		}
		
		if(whereSubquery)
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).groupByNodes = tempGroupBy;//.add(tempGroupBy);//FIXME: mahesh change this
		else if(fromSubquery)
			qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).groupByNodes = tempGroupBy;//add(tempGroupBy);
		else
			qParser.groupByNodes = tempGroupBy;//.add(tempGroupBy);

		//Get Aggregations
		List<SelectItem> rcList = rsNode.getSelectItems();
		Vector<AggregateFunction> tempAggFunc=new Vector<AggregateFunction>();
		AggregateFunction aggFunc = new AggregateFunction();
		for(int i=0;i<rcList.size();i++){

			if(rcList.get(i) instanceof SelectExpressionItem){

				SelectExpressionItem rc = (SelectExpressionItem)rcList.get(i);
				logger.log(Level.INFO,"ResultColumn rc-" + i + " = " +rc.getExpression());
	
				if(rc.getExpression() instanceof Function){					
					AggregateFunction af = new AggregateFunction();
					Function an = (Function)rc.getExpression();
					String aggName = an.getName();
					Node n = null;
					if(an.getParameters() != null){
						n = WhereClauseVectorJSQL.getWhereClauseVector(an, null,queryAliases,false, queryType,qParser);//mahesh: change null to actual name
					}
					aggFunc.setAggExp(n);
					aggFunc.setFunc(aggName.toUpperCase());
					aggFunc.setDistinct(an.isDistinct());
					
					if(rc.getAlias() != null){
						aggFunc.setAggAliasName(rc.getAlias().getName());
					} 
					if(n != null && n.getAgg() != null){
					tempAggFunc.add(n.getAgg()); 
					}
					else if(aggName.toUpperCase().contains("COUNT") && an.isAllColumns()){
						//In case of count(*)				 
						if(aggFunc.getAggExp() == null){
							if(i>0 && rcList.get(i-1) instanceof SelectExpressionItem){
								SelectExpressionItem rcPrev = (SelectExpressionItem)rcList.get(i-1);
								if(rcPrev.getExpression() instanceof Column){
									Node n1 = WhereClauseVectorJSQL.getWhereClauseVector(rcPrev.getExpression(), null,queryAliases,false, queryType,qParser);
									aggFunc.setAggExp(n1);
									aggFunc.setFunc(aggName.toUpperCase());
									if(rc.getAlias() != null){
										aggFunc.setAggAliasName(rc.getAlias().getName());
									}   
									aggFunc.setDistinct(an.isDistinct());									 
								}
							}
							//Instead of adding to aggregate function - add it to Having clause
							tempAggFunc.add(aggFunc); 				
						}			
					}else{
						tempAggFunc.add(aggFunc); 
					}
				}
			}
		}
		//add this to list 
		if(whereSubquery)
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).aggFunc = tempAggFunc;//.add(tempAggFunc);
		else if(fromSubquery)
			qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).aggFunc = tempAggFunc;//.add(tempAggFunc);
		else
			qParser.aggFunc = tempAggFunc;
		
		// get having clause
		Expression hc = rsNode.getHaving();
		//havingClause = getWhereClauseVector(hc, queryAliases,false);
		//add this to having list
		//FIXME: Mahesh Add to sub query if subquery references
		
		Node n = WhereClauseVectorJSQL.getWhereClauseVector(hc, null,queryAliases,false,queryType,qParser);

		if(whereSubquery)
			//this.WhereClauseSubqueries.get(WhereClauseSubqueries.size()-1).HavingClause.add(getWhereClauseVector(hc, null,queryAliases,false));
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).havingClause = n;
		else if(fromSubquery)
			qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).havingClause = n;
		else{
			n = Util.modifyNode(n,qParser);
			//FIXME: If outer block uses FROM clause aggregate alias and 
			//outer block also has Having clause aggregates, this needs
			//to be changed.
			if(qParser.havingClause!=null && qParser.havingClause.getType() == null)
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
		} else if (clause instanceof Parenthesis){
			 boolean isNot = ((Parenthesis) clause).isNot();
			Node n= getWhereClauseVector(((Parenthesis)clause).getExpression(), exposedName, fle, isWhereClause, queryType, qParser);
			if(clause instanceof Parenthesis && isNot){
				Node left = n.getLeft();
				Node right = n.getRight();
				if(left != null && left.getNodeType() != null && 
						left.getNodeType().equals(Node.getBroNodeType())
						&& left.getOperator() != null && left.getOperator().equalsIgnoreCase("=")){
					left.setOperator("/=");
				}
				if(right != null && right.getNodeType() != null && 
						right.getNodeType().equals(Node.getBroNodeType())
						&& right.getOperator() != null && right.getOperator().equalsIgnoreCase("=")){
					right.setOperator("/=");
				}
			
				if(left != null && left.getNodeType() != null && 
						left.getNodeType().equals(Node.getBroNodeType())
						&& left.getOperator() != null && left.getOperator().equalsIgnoreCase("=")){
					left.setOperator("/=");
				}
				if(right != null && right.getNodeType() != null && 
						right.getNodeType().equals(Node.getBroNodeType())
						&& right.getOperator() != null && right.getOperator().equalsIgnoreCase("=")){
					right.setOperator("/=");
				}
			}
			return n;
		}
		else if (clause instanceof Function) {
			Function an = (Function)clause;
			String funcName = an.getName();
			
			//All these are string manipulation functions and not aggregate function
			if(! (funcName.equalsIgnoreCase("Lower") || funcName.equalsIgnoreCase("substring") || funcName.equalsIgnoreCase("upper")
					||funcName.equalsIgnoreCase("trim") || funcName.equalsIgnoreCase("postion") || funcName.equalsIgnoreCase("octet_length")
					|| funcName.equalsIgnoreCase("bit_length") || funcName.equalsIgnoreCase("char_length") || funcName.equalsIgnoreCase("overlay"))){
				
				AggregateFunction af = new AggregateFunction();
				if (an.getParameters()!=null){
					ExpressionList anList = an.getParameters();
					List<Expression> expList = anList.getExpressions();//FIXME not only 1 expression but all expressions
	 				Node n = getWhereClauseVector(expList.get(0),null, fle, isWhereClause, queryType, qParser);
					af.setAggExp(n);
					
				} else {
					af.setAggExp(null);
				}
				
				af.setFunc(funcName.toUpperCase());
				af.setDistinct(an.isDistinct());
				af.setAggAliasName(exposedName);
			
				Node agg = new Node();
				agg.setAgg(af);
				agg.setType(Node.getAggrNodeType());
				//Shree added this to set Table, TableNameNo for Aggregate function node - @ node level
				if(af.getAggExp() != null){
					agg.setTable(af.getAggExp().getTable());
					agg.setTableNameNo(af.getAggExp().getTableNameNo());
					agg.setTableAlias(af.getAggExp().getTableAlias());
					agg.setColumn(af.getAggExp().getColumn());
					
				}//Added by Shree for count(*) 
				else if(af.getFunc().toUpperCase().contains("COUNT") && an.isAllColumns()){
				
					if(af.getAggExp() == null){
								Node n1 = Util.getNodeForCount(fle, qParser);
								af.setAggExp(n1);
								af.setFunc(funcName.toUpperCase());
								af.setDistinct(an.isDistinct());
								
								agg.setTable(af.getAggExp().getTable());
								agg.setTableNameNo(af.getAggExp().getTableNameNo());
								agg.setTableAlias(af.getAggExp().getTableAlias());
								agg.setColumn(af.getAggExp().getColumn());
								
								agg.setLeft(null);
								agg.setRight(null);
							}
					
						}
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
			}
			else {
				//String function manipulation
				Node n=new Node();
				if (an.getParameters()!=null){
					ExpressionList anList = an.getParameters();
					List<Expression> expList = anList.getExpressions();//FIXME not only 1 expression but all expressions
	 				n = getWhereClauseVector(expList.get(0),null, fle, isWhereClause, queryType, qParser);		
				}
				return n;
			}
		} else if (clause instanceof DoubleValue) {
			Node n = new Node();
			n.setType(Node.getValType());
			String s=((((DoubleValue)clause).getValue()))+"";
			//String str=(BigIntegerDecimal)((((NumericConstantNode) clause).getValue()).getDouble()).toString();
			s=util.Utilities.covertDecimalToFraction(s);
			n.setStrConst(s);
			n.setLeft(null);
			n.setRight(null);
			return n;

		}else if (clause instanceof LongValue){
			Node n = new Node();
			n.setType(Node.getValType());
			String s=((((LongValue)clause).getValue()))+"";
			s=util.Utilities.covertDecimalToFraction(s);
			n.setStrConst(s);
			n.setLeft(null);
			n.setRight(null);
			return n;
		}
		else if (clause instanceof StringValue) {
			Node n = new Node();
			n.setType(Node.getValType());
			n.setStrConst(((StringValue) clause).getValue());
			n.setLeft(null); 
			n.setRight(null); 
			return n; 
		} else if (clause instanceof Column) {
			Column columnReference = (Column) clause;
			Node n = new Node();
			String colName= columnReference.getColumnName();
			String tableName  = columnReference.getTable().getFullyQualifiedName();
			String tableNameNumber = "";
			List <FromListElement> frmElementList = fle.getTabs();
			 if(qParser.getQuery().getQueryString().toLowerCase().contains(("as "+tableName.toLowerCase()))){
					//tableName = null;
				}
			 
			if(qParser.getQuery().getQueryString().toLowerCase().contains(("as "+colName.toLowerCase()))){
				
				Vector <Node> value = qParser.getAliasedToOriginal().get(colName);
				  
				if(value != null && value.size() > 0){
					n = value.get(0);//FIXME: vector of nodes not a single node
				}
				return n;
  
			}
			
			else{
				n = Util.getColumnFromOccurenceInJC(colName,tableName, fle, qParser);
				if (n == null) {//then probably the query is correlated				
					n = Util.getColumnFromOccurenceInJC(colName,tableName, qParser.getQueryAliases(),qParser);
				}	
			} 
			if(n == null) {
				logger.log(Level.WARNING,"WhereClauseVectorJSQL : Util.getColumnFromOccurenceInJC is not able to find matching Column - Node n = null");
				return null;
			}
			 
			n.setType(Node.getColRefType());
			if (columnReference.getTable().getFullyQualifiedName() != null) {
				n.setTableAlias(columnReference.getTable().getFullyQualifiedName());
			} else {
				n.setTableAlias("");
			} 

			if(n.getColumn() != null){
				n.getColumn().setAliasName(exposedName);
				n.setTable(n.getColumn().getTable());
				
			}
			if(n.getTableNameNo() == null || n.getTableNameNo().isEmpty()){
				n.setTableNameNo(tableNameNumber);
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

		} else if (clause instanceof AndExpression) {
			BinaryExpression andNode = ((BinaryExpression) clause);
			if (andNode.getLeftExpression() != null
					&& andNode.getRightExpression() != null) {
				
				/*if(andNode.getLeftExpression() instanceof ExtractExpression) {
					//type new_name = (type) ;
					return null;
				}else if(andNode.getRightExpression() instanceof ExtractExpression){
					return null;
				}*/
				Node n = new Node();
				Node left = new Node();
				Node right = new Node();
				n.setType(Node.getAndNodeType());
				n.setOperator("AND");
				left = getWhereClauseVector(andNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser);
				right = getWhereClauseVector(andNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser);
				
				
				n.setLeft(left);
				n.setRight(right);

				//Storing sub query details
				n.setQueryType(queryType);
				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
			}

		} else if (clause instanceof OrExpression) {
			BinaryExpression orNode = ((BinaryExpression) clause);
			if (orNode.getLeftExpression() != null
					&& orNode.getRightExpression() != null) {
				Node n = new Node();
				n.setType(Node.getOrNodeType());
				n.setOperator("OR");
				n.setLeft(getWhereClauseVector(orNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));

				n.setRight(getWhereClauseVector(orNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));

				//Storing sub query details
				n.setQueryType(queryType);
				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
			}
		} 

		//Added by Bikash ---------------------------------------------------------------------------------
		else if(clause instanceof LikeExpression){
			BinaryExpression likeNode=((BinaryExpression)clause);
			if (likeNode.getLeftExpression() !=null && likeNode.getRightExpression()!=null )
			{
				//if(likeNode.getReceiver() instanceof ColumnReference && (likeNode.getLeftOperand() instanceof CharConstantNode || likeNode.getLeftOperand() instanceof ParameterNode))
				{
					Node n=new Node();
					if(! likeNode.isNot()){
						n.setType(Node.getLikeNodeType());
						n.setOperator("~");
					}
					else{
						n.setType(Node.getLikeNodeType());
						n.setOperator("!~");
					}
					n.setLeft(getWhereClauseVector(likeNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
					n.setRight(getWhereClauseVector(likeNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));

					//Storing sub query details
					n.setQueryType(queryType);
					if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
					if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					return n;
				}
			}
		}

		else if(clause instanceof JdbcParameter){
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
		else if (clause instanceof Addition){
			BinaryExpression baoNode = ((BinaryExpression)clause);
			Node n = new Node();
			n.setType(Node.getBaoNodeType());
			n.setOperator("+");
			n.setLeft(getWhereClauseVector(baoNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(baoNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			
			n=getTableDetailsForArithmeticExpressions(n);
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		}
		else if (clause instanceof Subtraction){
			BinaryExpression baoNode = ((BinaryExpression)clause);
			Node n = new Node();
			n.setType(Node.getBaoNodeType());
			n.setOperator("-");
			n.setLeft(getWhereClauseVector(baoNode.getLeftExpression(),exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(baoNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n=getTableDetailsForArithmeticExpressions(n);
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		}
		else if (clause instanceof Multiplication){
			BinaryExpression baoNode = ((BinaryExpression)clause);
			Node n = new Node();
			n.setType(Node.getBaoNodeType());
			n.setOperator("*");
			n.setLeft(getWhereClauseVector(baoNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(baoNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n=getTableDetailsForArithmeticExpressions(n);
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		}
		else if (clause instanceof Division){
			BinaryExpression baoNode = ((BinaryExpression)clause);
			Node n = new Node();
			n.setType(Node.getBaoNodeType());
			n.setOperator("/");
			n.setLeft(getWhereClauseVector(baoNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(baoNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n=getTableDetailsForArithmeticExpressions(n);
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		}

		else if (clause instanceof NotEqualsTo) {
			NotEqualsTo broNode = (NotEqualsTo)clause;
			/*if(broNode.getLeftExpression() instanceof ExtractExpression) {
				//type new_name = (type) ;
				Node n = new Node();
				return n;
			}else if(broNode.getRightExpression() instanceof ExtractExpression){
				Node n = new Node();
				return n;
			}*/
			
			//BinaryRelationalOperatorNode broNode = ((BinaryRelationalOperatorNode) clause);			
			Node n = new Node();
			n.setType(Node.getBroNodeType());
			n.setOperator(QueryParser.cvcRelationalOperators[2]);
			n.setLeft(getWhereClauseVector(broNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(broNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			return n;
			} 
		else if (clause instanceof DoubleAnd) {
			DoubleAnd broNode = (DoubleAnd)clause;	
			
			Node n = new Node();
			n.setType(Node.getBroNodeType());
			n.setOperator(QueryParser.cvcRelationalOperators[7]);
			n.setLeft(getWhereClauseVector(broNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(broNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			return n;
			} 
		else if (clause instanceof IsNullExpression) {
			IsNullExpression isNullNode = (IsNullExpression) clause;
			Node n = new Node();
			n.setType(Node.getIsNullNodeType());
			n.setLeft(getWhereClauseVector(isNullNode.getLeftExpression(),exposedName, fle,isWhereClause, queryType,qParser));
			if(((IsNullExpression) clause).isNot()){
				n.setOperator("!=");
			}else{
				n.setOperator("=");
			}
			n.setRight(null);
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			return n;
		} else if (clause instanceof InExpression){ 
			//handles NOT and NOT IN both
			InExpression sqn = (InExpression)clause;
			Node rhs = null;
			qParser.isInSubQ = true; 
			Node notNode = new Node();
			/*if(sqn.getLeftExpression() instanceof ExtractExpression) {
				//type new_name = (type) ;
				Node n = new Node();
				return n;
			}else if(sqn.getLeftItemsList() instanceof ExtractExpression){
				Node n = new Node();
				return n;
			}
			else if(sqn.getRightItemsList() instanceof ExtractExpression){
				Node n = new Node();
				return n;
			}*/					
			//Node lhs = getWhereClauseVector(sqn.getLeftExpression(),exposedName, fle,isWhereClause, queryType,qParser);
			Vector<Node> thisSubQConds = new Vector<Node>();
		
			
			if (sqn. getLeftItemsList() instanceof SubSelect){
				//Shree changed isWhereclause to true
				/*if(isWhereClause 
						&& qParser.getWhereClauseSubqueries() != null
						&& !qParser.getWhereClauseSubqueries().isEmpty()){
				OperateOnSubQueryJSQL.OperateOnSubquery((SubSelect)sqn.getLeftItemsList(), thisSubQConds,new JoinTreeNode(),false,true,qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1));
				}
				else{*/
					OperateOnSubQueryJSQL.OperateOnSubquery((SubSelect)sqn.getLeftItemsList(), thisSubQConds,new JoinTreeNode(),false,true,qParser);
				//} 
			}
			else if(sqn. getRightItemsList() instanceof SubSelect){ 
			/*	if(isWhereClause 
						&& qParser.getWhereClauseSubqueries() != null
						&& !qParser.getWhereClauseSubqueries().isEmpty()){
				OperateOnSubQueryJSQL.OperateOnSubquery((SubSelect)sqn.getRightItemsList(), thisSubQConds,new JoinTreeNode(),false,true,qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1));
				} 
				else{*/
					OperateOnSubQueryJSQL.OperateOnSubquery((SubSelect)sqn.getRightItemsList(), thisSubQConds,new JoinTreeNode(),false,true,qParser);
				//}
			}
			
			FromListElement fle1 = null;
			//if conditions added by mathew to avoid null pointer exception
			if(!qParser.getWhereClauseSubqueries().isEmpty()){
				//fle1 = qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size() -1).getQueryAliases();
				fle1 = new FromListElement();
				//fle1.addTabs(fle);
				//fle1.addTabs(qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size() -1).getQueryAliases());
				for(QueryParser qp : qParser.getWhereClauseSubqueries()){	
					// fle1 = new FromListElement();
				    fle1.addTabs(qp.getQueryAliases());
				}
				fle1.addTabs(fle);
			}
			if(fle1 == null){
				 fle1 = new FromListElement();
				fle1.addTabs(fle);
				
			}
			 
			Node lhs = getWhereClauseVector(sqn. getLeftExpression(),"", fle1,isWhereClause,queryType,qParser);
			
			// Extract the projected column and create a colref node
			//chnaged for errors
			SubSelect subS = null;
			if(sqn. getLeftItemsList() instanceof SubSelect){
				subS = (SubSelect)sqn.getLeftItemsList();
			}else if(sqn. getRightItemsList() instanceof SubSelect){
				subS  = (SubSelect)sqn.getRightItemsList();
			}
			if(subS != null){
			List<SelectItem> rcList = ((PlainSelect)subS.getSelectBody()).getSelectItems();
			
			if (rcList.get(0) instanceof AllColumns) {
				Vector <Node> nodeVect = Util.addAllProjectedColumns(qParser.queryAliases,0,qParser);
			    //rhs = nodeVect.get(0);
			    rhs = Util.getColumnFromOccurenceInJC(nodeVect.get(0).getColumn().getColumnName(), 
			    		nodeVect.get(0).getColumn().getTable().getTableName(), fle1, qParser);
				 
			}
			else if (rcList.get(0) instanceof SelectExpressionItem){		
			SelectExpressionItem rc = (SelectExpressionItem)rcList.get(0);
			Column cr ;
			String aggName="" ;
			Function an = new Function();
			
			//mahesh chane
			Expression exp = rc.getExpression();
			if( exp instanceof Function ){
				an = (Function)exp;
				aggName = an.getName();
				ExpressionList expL = an.getParameters();
				cr = (net.sf.jsqlparser.schema.Column)expL.getExpressions().get(0);
			}
			else {
				cr = (net.sf.jsqlparser.schema.Column) exp;
			}
			rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr.getTable().getFullyQualifiedName(), fle1, qParser);
			if (rhs == null) {
				rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), cr.getTable().getFullyQualifiedName(), qParser.getQueryAliases(),qParser);
			}
			rhs.setType(Node.getColRefType());
			if (cr.getTable().getFullyQualifiedName() != null) {
				rhs.setTableAlias(cr.getTable().getFullyQualifiedName());
			} else {
				rhs.setTableAlias("");
			}

			if( exp instanceof Function ){
				AggregateFunction af = new AggregateFunction();
				af.setAggExp(rhs);
				af.setFunc(aggName.toUpperCase());
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
			}
			//qParser.getAllConds().addAll(qParser.getWhereClauseSubqueries().get((qParser.getWhereClauseSubqueries().size()-1)).allConds);
			rhs.setQueryType(2);
			rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			// This holds the condition in the outer block for IN queries
			//Ex: where d.dept_name IN (select c.dept_name from course where condition1) 
			//- here we get d.dept_name = c.dept_name condition in cond node
			Node cond = new Node();
			cond.setType(Node.getBroNodeType());
			cond.setLeft(lhs);
			cond.setRight(rhs);
			cond.setOperator("="); 
			cond.setAgg(rhs.getAgg());
			cond.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			cond.setQueryType(2);		
			
			//Create the condition given in the inner subq block-
			//create node for Condition 1 in above example
			//it is in whereclauseSubQ.allcondition
			Node sqNode = new Node();
			sqNode.setLeft(null);
			sqNode.setRight(null);

			
			 Vector allCon = qParser.getWhereClauseSubqueries().get((qParser.getWhereClauseSubqueries().size()-1)).allConds;
			 if(allCon != null && !allCon.isEmpty()){
				 Node condition1 = (Node)allCon.get(allCon.size()-1);
					//Create a new node "AND" with the two conditions and set it in LHSRHS of returning node
					Node finalLhsRhsNode = new Node();
					finalLhsRhsNode.setType(Node.getAndNodeType());
					finalLhsRhsNode.setLeft(cond);
					finalLhsRhsNode.setRight(condition1);
					finalLhsRhsNode.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					finalLhsRhsNode.setQueryType(2);
					
					// create the final subquery node and return it
					sqNode.setType(Node.getInNodeType());
					sqNode.setSubQueryConds(thisSubQConds);
					
					sqNode.setLhsRhs(finalLhsRhsNode);
			 }else{
				// create the final subquery node and return it
					sqNode.setType(Node.getInNodeType());
					sqNode.setSubQueryConds(thisSubQConds);

					sqNode.setLhsRhs(cond);
					
			 }			
				//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of subqueries 
			//	qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).parseQuery("q1", subS.toString());

			if(sqn.isNot()){
				//sqNode.setType(Node.getNotInNodeType());
				
				notNode.setType(Node.getNotNodeType());
				notNode.setRight(null);
				notNode.setLeft(sqNode);
				notNode.setQueryType(2); 
				return notNode;
			//	if(thisSubQConds!= null && thisSubQConds.isEmpty()){
				//	thisSubQConds.addAll(qParser.getWhereClauseSubqueries().get((qParser.getWhereClauseSubqueries().size()-1)).allConds);
				//}
			}else{			
				sqNode.setType(Node.getInNodeType());
				//The following line added by mathew in order to explicate that we this is a where clause subquery
				sqNode.setQueryType(2);
			} 
			
			return sqNode;
			}
			
		} else if (clause instanceof ExistsExpression){
			ExistsExpression sqn = (ExistsExpression)clause;
			Node notNode = new Node();;
			Node lhs = null;// getWhereClauseVector(sqn.getRightExpression(),exposedName, fle,isWhereClause, queryType,qParser);
			Vector<Node> thisSubQConds = new Vector<Node>();
			Alias a = null; 
			SubSelect subS = (SubSelect)sqn.getRightExpression();
			//Shree changed isWhereClause to true
			OperateOnSubQueryJSQL.OperateOnSubquery(subS, thisSubQConds, qParser.root ,false,true,qParser);
			FromListElement fle1 = null;
			if( qParser.getWhereClauseSubqueries() != null && ! qParser.getWhereClauseSubqueries().isEmpty()){
				fle1 =  qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();
			}
			else if( qParser.getFromClauseSubqueries() != null && ! qParser.getFromClauseSubqueries().isEmpty()){
				fle1 = qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getQueryAliases();
			}
			
			Node sqNode = new Node();
			sqNode.setSubQueryConds(thisSubQConds);
			   
			Node rhs = new Node();
			rhs.setQueryType(2);
			rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			sqNode.setLhsRhs(rhs);
			sqNode.setType(Node.getExistsNodeType());
			
			if(!((ExistsExpression) clause).isNot()){
				
				if(qParser.getWhereClauseSubqueries() != null && !qParser.getWhereClauseSubqueries().isEmpty()){
					thisSubQConds.addAll(qParser.getWhereClauseSubqueries().get((qParser.getWhereClauseSubqueries().size()-1)).allConds);
				}
				else if(qParser.getFromClauseSubqueries() != null && !qParser.getFromClauseSubqueries().isEmpty()){
					thisSubQConds.addAll(qParser.getFromClauseSubqueries().get((qParser.getFromClauseSubqueries().size()-1)).allConds);
				}
				
				sqNode.setType(Node.getExistsNodeType());
				//if(thisSubQConds!= null && thisSubQConds.isEmpty()){
				//	thisSubQConds.addAll(qParser.getWhereClauseSubqueries().get((qParser.getWhereClauseSubqueries().size()-1)).allConds);
				//}
			}else{
				notNode.setType(Node.getNotNodeType());
				notNode.setRight(null);
				notNode.setLeft(sqNode);
				notNode.setQueryType(2); 
				return notNode;
				
			}
			//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of subqueries 
			//qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).parseQuery("q1", subS.toString());

			return sqNode;
			
		}
		else if (clause instanceof SubSelect) {
			SubSelect sqn = (SubSelect) clause;
			//added by bikash
			Vector<Node> thisSubQConds = new Vector<Node>();
			//FIXME: mahesh changed this.But not sure
			//Shree changed isWhereClause to true
			OperateOnSubQueryJSQL.OperateOnSubquery(sqn, thisSubQConds,new JoinTreeNode(),false,true,qParser);

			FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();
			
			//PlainSelect ps = sqn.getSelectBody();
			List<SelectItem> rcList = ((PlainSelect)sqn.getSelectBody()).getSelectItems();	
			
			SelectExpressionItem rc = (SelectExpressionItem)rcList.get(0);
			Vector <Column> aggColumnList = new Vector<Column>();

			//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of subqueries 
			//qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).parseQuery("q1", sqn.toString());
			if(rc.getExpression() instanceof Function){ 

				Function an = (Function)rc.getExpression();
				String aggName = an.getName();
				ExpressionList expL = an.getParameters();
				AggregateFunction af = new AggregateFunction();
				Node n = getWhereClauseVector(expL.getExpressions().get(0), exposedName,fle1,isWhereClause, 2, qParser);			 
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
				rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				rhs.setSubQueryConds(thisSubQConds);

				// create the final subquery node and return it
				Node sqNode = new Node();
				sqNode.setType(Node.getBroNodeSubQType());
				sqNode.setSubQueryConds(thisSubQConds);
				sqNode.setLhsRhs(rhs);
				return sqNode;
			}
			else if(rc.getExpression() instanceof ColumnReference || 
					(((Parenthesis)rc.getExpression()).getExpression()) instanceof Column){
				//the result of subquery must be a single tuple
				logger.log(Level.WARNING,"the result of subquery must be a single tuple");
		    }
		}
		else if(clause instanceof Between){
			
			//FIXME: Mahesh If aggregate in where (due to aliased) then add to list of having clause of the subquery
			
			Between bn=(Between)clause;
			Node n=new Node();
			n.setType(Node.getAndNodeType());
			
			Node l=new Node();
			l.setLeft(getWhereClauseVector(bn.getLeftExpression(),exposedName,fle,isWhereClause, queryType,qParser));
			l.setOperator(">=");
			l.setRight(getWhereClauseVector(bn.getBetweenExpressionStart(),exposedName,fle,isWhereClause, queryType,qParser));
			l.setType(Node.getBroNodeType());
			n.setLeft(l);

			Node r=new Node();
			r.setLeft(getWhereClauseVector(bn.getLeftExpression(),exposedName,fle,isWhereClause, queryType,qParser));
			r.setOperator("<=");
			r.setRight(getWhereClauseVector(bn.getBetweenExpressionEnd(),exposedName,fle,isWhereClause, queryType,qParser));
			r.setType(Node.getBroNodeType());
			n.setRight(r);

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			return n;
			//throw new Exception("getWhereClauseVector needs more programming \n"+clause.getClass()+"\n"+clause.toString());
		} else if (clause instanceof EqualsTo){

			BinaryExpression bne = (BinaryExpression)clause;
			Node n = new Node();
			/*if(bne.getLeftExpression() instanceof ExtractExpression) {
				return n;
			}else if(bne.getRightExpression() instanceof ExtractExpression){
				return n;
			}*/
			n.setType(Node.getBroNodeType());
			n.setOperator("=");
			Node ndl = getWhereClauseVector(bne.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser);
			if(ndl != null){
				n.setLeft(ndl);
			}
			Node ndr = getWhereClauseVector(bne.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser);
			if(ndr!= null){
				n.setRight(ndr);
			}
			
			if((ndl == null && ndr ==null)){
				return null;
			}
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
				n.setSubQueryConds(n.getLeft().getSubQueryConds());
				n.getLeft().getSubQueryConds().clear();
			}
			else{ 				
				if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
				n.setSubQueryConds(n.getRight().getSubQueryConds());
				n.getRight().getSubQueryConds().clear();
				}
			}

			if(((EqualsTo) clause).getRightExpression() instanceof AllComparisonExpression ||
					((EqualsTo) clause).getLeftExpression() instanceof AllComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAllNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode; 
			}  

			if(((EqualsTo) clause).getRightExpression() instanceof AnyComparisonExpression ||
					((EqualsTo) clause).getLeftExpression() instanceof AnyComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAnyNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode; 
			}  

			
			return n;
		} else if (clause instanceof GreaterThan){
			GreaterThan broNode = (GreaterThan)clause;
			/*if(broNode.getLeftExpression() instanceof ExtractExpression) {
				//type new_name = (type) ;
				Node n = new Node();
				return n;
			}else if(broNode.getRightExpression() instanceof ExtractExpression){
				Node n = new Node();
				return n;
			}*/
			//BinaryRelationalOperatorNode broNode = ((BinaryRelationalOperatorNode) clause);			
			Node n = new Node();
			n.setType(Node.getBroNodeType());
			n.setOperator(QueryParser.cvcRelationalOperators[3]);
			n.setLeft(getWhereClauseVector(broNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(broNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			
			if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
				n.setSubQueryConds(n.getLeft().getSubQueryConds());
				n.getLeft().getSubQueryConds().clear();
			}
			else{ 
				if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
				n.setSubQueryConds(n.getRight().getSubQueryConds());
				n.getRight().getSubQueryConds().clear();
				}
			}
			
			if(((GreaterThan) clause).getRightExpression() instanceof AllComparisonExpression ||
					((GreaterThan) clause).getLeftExpression() instanceof AllComparisonExpression){

				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAllNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			}
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode;
			} 

			if(((GreaterThan) clause).getRightExpression() instanceof AnyComparisonExpression ||
					((GreaterThan) clause).getLeftExpression() instanceof AnyComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAnyNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode; 
			}  
			
			return n;
		}
		else if (clause instanceof GreaterThanEquals){
			GreaterThanEquals broNode = (GreaterThanEquals)clause;
			/*if(broNode.getLeftExpression() instanceof ExtractExpression) {
				//type new_name = (type) ;
				Node n = new Node();
				return n;
			}else if(broNode.getRightExpression() instanceof ExtractExpression){
				Node n = new Node();
				return n;
			}*/
			//BinaryRelationalOperatorNode broNode = ((BinaryRelationalOperatorNode) clause);			
			Node n = new Node();
			n.setType(Node.getBroNodeType());
			n.setOperator(QueryParser.cvcRelationalOperators[4]);
			n.setLeft(getWhereClauseVector(broNode.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(broNode.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));
 
			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
			
			//Code added for ALL / ANY subqueries - Start
			//FIXME ANy condition needs to be tested - IS this correct???
			if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
				n.setSubQueryConds(n.getLeft().getSubQueryConds());
				n.getLeft().getSubQueryConds().clear();
			}
			else{ 
				if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
				n.setSubQueryConds(n.getRight().getSubQueryConds());
				n.getRight().getSubQueryConds().clear();
				}
			}
			
			if(((GreaterThanEquals) clause).getRightExpression() instanceof AllComparisonExpression ||
					((GreaterThanEquals) clause).getLeftExpression() instanceof AllComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAllNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			}
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode;
			}
			
			if(((GreaterThanEquals) clause).getRightExpression() instanceof AnyComparisonExpression ||
					((GreaterThanEquals) clause).getLeftExpression() instanceof AnyComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAnyNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode; 
			}  

			return n;
		}
		else if (clause instanceof MinorThan){
			BinaryExpression bne = (BinaryExpression)clause;
			/*if(bne.getLeftExpression() instanceof ExtractExpression) {
				//type new_name = (type) ;
				Node n = new Node();
				return n;
			}else if(bne.getRightExpression() instanceof ExtractExpression){
				Node n = new Node();
				return n;
			}*/
			Node n = new Node();
			n.setType(Node.getBroNodeType());
			n.setOperator("<");
			n.setLeft(getWhereClauseVector(bne.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(bne.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
				n.setSubQueryConds(n.getLeft().getSubQueryConds());
				n.getLeft().getSubQueryConds().clear();
			}
			else{ 
				if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
				n.setSubQueryConds(n.getRight().getSubQueryConds());
				n.getRight().getSubQueryConds().clear();
				}
			}
			
			if(((MinorThan) clause).getRightExpression() instanceof AllComparisonExpression ||
					((MinorThan) clause).getLeftExpression() instanceof AllComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAllNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode;
			} 

			if(((MinorThan) clause).getRightExpression() instanceof AnyComparisonExpression ||
					((MinorThan) clause).getLeftExpression() instanceof AnyComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAnyNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode; 
			}  
			
			return n;
		} else if (clause instanceof MinorThanEquals){
			BinaryExpression bne = (BinaryExpression)clause;
			Node n = new Node();
			/*if(bne.getLeftExpression() instanceof ExtractExpression) {
				//type new_name = (type) ;
				return n;
			}else if(bne.getRightExpression() instanceof ExtractExpression){
				return n;
			}*/
			n.setType(Node.getBroNodeType());
			n.setOperator("<=");
			n.setLeft(getWhereClauseVector(bne.getLeftExpression(), exposedName, fle, isWhereClause, queryType,qParser));
			n.setRight(getWhereClauseVector(bne.getRightExpression(), exposedName, fle, isWhereClause, queryType,qParser));

			//Storing sub query details
			n.setQueryType(queryType);
			if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

			if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
				n.setSubQueryConds(n.getLeft().getSubQueryConds());
				n.getLeft().getSubQueryConds().clear();
			}
			else{ 
				if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
				n.setSubQueryConds(n.getRight().getSubQueryConds());
				n.getRight().getSubQueryConds().clear();
				}
			}
			if(((MinorThanEquals) clause).getRightExpression() instanceof AllComparisonExpression ||
					((MinorThanEquals) clause).getLeftExpression() instanceof AllComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAllNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode;
			} 
			
			if(((MinorThanEquals) clause).getRightExpression() instanceof AnyComparisonExpression ||
					((MinorThanEquals) clause).getLeftExpression() instanceof AnyComparisonExpression){
				Node sqNode = new Node();
				/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
				 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
				 */
				sqNode.setType(Node.getAnyNodeType());
				if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
			} 
				else{ 
					sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
			} 
				sqNode.setLhsRhs(n);
				return sqNode; 
			}  
			
			return n;
		} else if(clause instanceof CaseExpression){
			CaseExpression expr =  (CaseExpression)clause;
		//	Node newCaseNode = new Node();
			Node n = new Node();
			List<Expression> whenExprList = expr.getWhenClauses();
			Vector <Node> caseConditionNode = new Vector<Node>();
			//If it is a case expression, then create a vector of nodes that holds case condition and else cond
			//Add that to cvc or qparser and return a node that is of type casecondition.
			if(! isWhereClause){
			
				 n.setQueryType(queryType);
				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					
				if(expr.getElseExpression() != null){
					n = getWhereClauseVector(expr.getElseExpression(), exposedName, fle, isWhereClause, queryType,qParser);
				}
				else if(expr.getWhenClauses() != null){
					for(int i = 0; i < expr.getWhenClauses().size();i++){
						Expression ex = expr.getWhenClauses().get(i);
						n = getWhereClauseVector(ex, exposedName, fle, isWhereClause, queryType,qParser);
					}
				}
				return n;
			}else{
				return null;
			}
		}
		else if (clause instanceof AllComparisonExpression){
			Vector<Node> thisSubQConds = new Vector<Node>();
			//FIXME: mahesh changed this.But not sure
			AllComparisonExpression ace = (AllComparisonExpression)clause;

			SubSelect ss = ace.getSubSelect();
			//Shree changed isWhereclause to true
			 OperateOnSubQueryJSQL.OperateOnSubquery(ss, thisSubQConds,qParser.root,false, true,qParser);
			FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();

 
			// Extract the projected column and create a colref node
			PlainSelect sbod = (PlainSelect)ss.getSelectBody();
			List<SelectItem> siList = sbod.getSelectItems();
			Node rhs=null;
			SelectItem sei = siList.get(0);
			int i=0;
			if (sei instanceof SelectExpressionItem){

				Expression exp1 = ((SelectExpressionItem)sei).getExpression();
				
				if (exp1 instanceof net.sf.jsqlparser.schema.Column){
					net.sf.jsqlparser.schema.Column cr = (net.sf.jsqlparser.schema.Column)exp1;
					String colName= cr.getColumnName();
					String tableName  = cr.getTable().getFullyQualifiedName();
					String tableNameNumber = "";

					//Call where clause again for Column reference 
					rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(),tableName, fle1, qParser);
					if (rhs == null) {
						rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), tableName,qParser.getQueryAliases(), qParser);
					} 
					rhs.setType(Node.getColRefType());
					rhs.setQueryType(2);
					rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);				
					rhs.setSubQueryConds(thisSubQConds);
					
				}
			}
			//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of subqueries 
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).parseQuery("q1", sbod.toString());
			return rhs;
		}
		else if (clause instanceof AnyComparisonExpression){
			Vector<Node> thisSubQConds = new Vector<Node>();
			//FIXME: mahesh changed this.But not sure
			AnyComparisonExpression ace = (AnyComparisonExpression)clause;
			SubSelect ss = ace.getSubSelect();
			//Shree changed isWhereclause to true
			 OperateOnSubQueryJSQL.OperateOnSubquery(ss, thisSubQConds,qParser.root,false, true,qParser);
			FromListElement fle1 =qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).getQueryAliases();

 
			// Extract the projected column and create a colref node
			PlainSelect sbod = (PlainSelect)ss.getSelectBody();
			List<SelectItem> siList = sbod.getSelectItems();
			Node rhs=null;
			SelectItem sei = siList.get(0);
			int i=0;
			if (sei instanceof SelectExpressionItem){
				Expression exp1 = ((SelectExpressionItem)sei).getExpression();
				
				if (exp1 instanceof net.sf.jsqlparser.schema.Column){
					net.sf.jsqlparser.schema.Column cr = (net.sf.jsqlparser.schema.Column)exp1;
					String colName= cr.getColumnName();
					String tableName  = cr.getTable().getFullyQualifiedName();
					String tableNameNumber = "";

					//Call where clause again for Column reference 
					rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(),tableName, fle1, qParser);
					if (rhs == null) {
						rhs = Util.getColumnFromOccurenceInJC(cr.getColumnName(), tableName,qParser.getQueryAliases(), qParser);
					} 
					rhs.setType(Node.getColRefType());
					rhs.setQueryType(2);
					rhs.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					rhs.setSubQueryConds(thisSubQConds);
					
				}
			}
			//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of subqueries 
			qParser.getWhereClauseSubqueries().get(qParser.getWhereClauseSubqueries().size()-1).parseQuery("q1", sbod.toString());
			return rhs;
		}
		else if(clause instanceof ExtractExpression){
			//To extract approximate no: of days in month is considered as 30.
			/*Assuming the ExtractExpression clause holds name and Column alone*/
				ExtractExpression exp = (ExtractExpression)clause;
				Node n=new Node(); // Main node
				String name = exp.getName();
				Node table = getWhereClauseVector(exp.getExpression(),exposedName,fle,isWhereClause, queryType,qParser);
				if(name.equalsIgnoreCase("year")){
					//Formula : 1970+ (col Name/(30*12)) - create 5 nodes
					n.setOperator("+"); // Main node
					n.setType(Node.getBaoNodeType());
					n.setQueryType(queryType);
					if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
					if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					Node n1 = new Node(); //Left of main node - level 1
					n1.setType(Node.getValType());
					String s="1970";
					s=util.Utilities.covertDecimalToFraction(s);
					n1.setStrConst(s);
					Node i = new Node();
					i.setType(Node.getExtractFuncType());
					n1.setLeft(i);
					//n1.setLeft(null);
					n1.setRight(null);
					n1.setTable(table.getTable());
					n1.tableNameNo = table.getTableNameNo();
					n1.setColumn(table.getColumn());
					n.setLeft(n1);
					
					//Call method to get Node :
					
					n.setRight(getYearCalc(exp,exposedName,fle,isWhereClause,queryType,qParser));
					
					
				}else if(name.equalsIgnoreCase("month")){
					//Formula 1 : (col Name/30) MOD 12 But as CVC does not support MOD rewrite the formula 
					// Formula 2: a mod b = a - (a/b) *b => (col Name/30)-((col Name/30)/12) * 12) create 12 nodes
					n.setOperator("-"); // Main node
					n.setType(Node.getBaoNodeType());
					n.setQueryType(queryType);
					if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
					if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					
						//LEFT OF MAIN NODE - considered as level 0
						Node nl1 = new Node(); 
							nl1.setOperator("/");
							nl1.setType(Node.getBaoNodeType());
							nl1.setQueryType(queryType);
							if(queryType == 1) nl1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
							if(queryType == 2)nl1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
							//Left of node at level 1
							nl1.setLeft(getWhereClauseVector(exp.getExpression(),exposedName,fle,isWhereClause, queryType,qParser));
							
								Node nl1r1 = new Node();
								nl1r1.setType(Node.getValType());
								String s="30";
								s=util.Utilities.covertDecimalToFraction(s);
								nl1r1.setStrConst(s);
								Node i = new Node();
								i.setType(Node.getExtractFuncType());
								nl1r1.setLeft(i);
								//nl1r1.setLeft(null);
								nl1r1.setRight(null);
						nl1.setRight(nl1r1);
					n.setTable(table.getTable());
					n.tableNameNo = table.getTableNameNo();
					n.setColumn(table.getColumn());
					n.setLeft(nl1);
					
					//RIGHT OF MAIN NODE  - considered as level 0
					Node nr1 = new Node();
						nr1.setOperator("*");
						nr1.setType(Node.getBaoNodeType());
						nr1.setQueryType(queryType);
						if(queryType == 1) nr1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
						if(queryType == 2) nr1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
						//Left Node at level 1
							Node nr1l1 = new Node();
								nr1l1.setOperator("/");
								nr1l1.setType(Node.getBaoNodeType());
								nr1l1.setQueryType(queryType);
								if(queryType == 1) nr1l1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
								if(queryType == 2) nr1l1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
										//Left Node at level 2
										Node nr1l2 = new Node();
											nr1l2.setOperator("/");
											nr1l2.setType(Node.getBaoNodeType());
											nr1l2.setQueryType(queryType);
											if(queryType == 1) nr1l2.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
											if(queryType == 2) nr1l2.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
											//Left Node at level 3
											nr1l2.setLeft(getWhereClauseVector(exp.getExpression(),exposedName,fle,isWhereClause, queryType,qParser));
											
											//Right Node at level 3
											Node nr1r3 = new Node();
												nr1r3.setType(Node.getValType());
												String st="30";
												st=util.Utilities.covertDecimalToFraction(st);
												nr1r3.setStrConst(st);
												Node i1 = new Node();
												i1.setType(Node.getExtractFuncType());
												nr1r3.setLeft(i1);
												nr1r3.setRight(null);
											nr1l2.setRight(nr1r3);
											
										
								nr1l1.setLeft(nr1l2);	
									//Right Node at level 2
										Node nr1r2 = new Node();
										nr1r2.setType(Node.getValType());
										String s1="12";
										s1=util.Utilities.covertDecimalToFraction(s1);
										nr1r2.setStrConst(s1);
										Node i2 = new Node();
										i2.setType(Node.getExtractFuncType());
										nr1r2.setLeft(i2);
										//nr1r2.setLeft(null);
										nr1r2.setRight(null);
								nr1l1.setRight(nr1r2);
						nr1.setLeft(nr1l1);
						
						//Right Node at level 1
							Node nr1r1 = new Node();
							nr1r1.setType(Node.getValType());
							String s2="12";
							s2=util.Utilities.covertDecimalToFraction(s2);
							nr1r1.setStrConst(s2);
							Node i4 = new Node();
							i4.setType(Node.getExtractFuncType());
							nr1r1.setLeft(i4);
							//nr1r1.setLeft(null);
							nr1r1.setRight(null);
						nr1.setRight(nr1r1);
					
				 n.setRight(nr1);
					
				}else if(name.equalsIgnoreCase("day")){
					//Formula 1: approximate Date : (column value -((col Value/(30*12))*365) MOD 30)
					//Formula 2 : eliminating MOD :[ (column value -((col Value/(30*12))*365)) - ([(column value -((col Value/(30*12))*365))/30] * 30) ]
					n.setOperator("-"); // Main node
					n.setType(Node.getBaoNodeType());
					n.setQueryType(queryType);
					n.setTable(table.getTable());
					n.tableNameNo = table.getTableNameNo();
					n.setColumn(table.getColumn());
					if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
					if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					//Left of main node
					n.setLeft(getDayCalc(exp, exposedName, fle, isWhereClause, queryType, qParser));
						//Right of main node
						Node nr1 = new Node();
						nr1.setOperator("*");
						nr1.setType(Node.getBaoNodeType());
						nr1.setQueryType(queryType);
						if(queryType == 1) nr1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
						if(queryType == 2) nr1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
						
							Node nr1l1 = new Node();
							nr1l1.setOperator("/");
							nr1l1.setType(Node.getBaoNodeType());
							nr1l1.setQueryType(queryType);
							if(queryType == 1) nr1l1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
							if(queryType == 2) nr1l1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
							nr1l1.setLeft(getDayCalc(exp, exposedName, fle, isWhereClause, queryType, qParser));
									Node nr1r2 = new Node();
									nr1r2.setType(Node.getValType());
									String st="30";
									st=util.Utilities.covertDecimalToFraction(st);
									nr1r2.setStrConst(st);
									Node i = new Node();
									i.setType(Node.getExtractFuncType());
									nr1r2.setLeft(i);
									//nr1r2.setLeft(null);
									nr1r2.setRight(null);
							nr1l1.setRight(nr1r2);
						nr1.setLeft(nr1l1);
						
							Node nr1r1 = new Node();
							nr1r1.setType(Node.getValType());
							String s2="30";
							s2=util.Utilities.covertDecimalToFraction(s2);
							nr1r1.setStrConst(s2);
							Node i1 = new Node();
							i1.setType(Node.getExtractFuncType());
							nr1r1.setLeft(i1);
							//nr1r1.setLeft(null);
							nr1r1.setRight(null);
						nr1.setRight(nr1r1);
					n.setRight(nr1);
				}
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
	

	
	

	
	private static Node transformToAbsoluteTableNames(Node n, Vector<FromListElement> fleList, boolean aliasNameFound, QueryParser qParser) throws Exception {
		// TODO Auto-generated method stub
		for(FromListElement fle:fleList){
			if(fle!=null&&fle.getTableName()!=null){
				if(fle.getTableName().equalsIgnoreCase(n.getTableNameNo())){
					n.setTableNameNo(fle.getTableNameNo());
					Table table=qParser.getTableMap().getTable(fle.getTableName());
					n.setTable(table);
					logger.info("table Name Found "+n);
					return n;
				}
				else if(fle.getAliasName().equalsIgnoreCase(n.getTableNameNo())){
					n.setTableNameNo(fle.getTableNameNo());
					Table table=qParser.getTableMap().getTable(fle.getTableName());
					if(table!=null)
						n.setTable(table);		
					logger.info("alias Name Found "+n);
					return n;
				}
				else if(aliasNameFound){
					logger.info("alias Name Found but not n");
					Table table=qParser.getTableMap().getTable(fle.getTableName());
					parsing.Column c;
					if((c=table.getColumn(n.getColumn().getColumnName().toUpperCase()))!=null){
						n.setTableNameNo(fle.getTableNameNo());
						n.setTable(table);
						return n;
					}
				}
			}
			if(aliasNameFound){
				logger.info("alias name found"+n);
				if(fle.getTabs()!=null&&!fle.getTabs().isEmpty()){
					Node k= transformToAbsoluteTableNames(n,fle.getTabs(),false,qParser);
					if(!n.getTableNameNo().equalsIgnoreCase(k.getTableNameNo()))
						return k;
				}
				if(fle.getSubQueryParser()!=null){
					Node k= transformToAbsoluteTableNames(n,fle.getSubQueryParser().getFromListElements(),true,fle.getSubQueryParser());
					if(k!=null&&!n.getTableNameNo().equalsIgnoreCase(k.getTableNameNo()))
						return k;					
					
				}
				logger.info(" Alias name found, but column name cannot be resolved");
			}
			if(fle!=null&&fle.getTableName()==null && fle.getAliasName()!=null){
				logger.info(" alias name is not null, but table name is null");
				if(fle.getAliasName().equalsIgnoreCase(n.getTableNameNo())){
					if(fle.getSubQueryParser()!=null){
						Node k= transformToAbsoluteTableNames(n,fle.getSubQueryParser().getFromListElements(),true,fle.getSubQueryParser());
						
						if(k!=null&&!n.getTableNameNo().equalsIgnoreCase(k.getTableNameNo()))
							return k;		
					}
					else {
						Node k= transformToAbsoluteTableNames(n,fle.getTabs(),true, qParser);
						if(k!=null&& !n.getTableNameNo().equalsIgnoreCase(k.getTableNameNo()))
							return k;		
					}
				}						
			}
			if(fle!=null && fle.getTabs()!=null && !fle.getTabs().isEmpty()){
				logger.info(" tabs is not null");
				Node k= transformToAbsoluteTableNames(n,fle.getTabs(),false,qParser);
				if(!n.getTableNameNo().equalsIgnoreCase(k.getTableNameNo()))
					return k;
			}
			if(fle!=null && fle.getSubQueryParser()!=null){
				logger.info(" subQueryParser: checking projected cols");
				
				for(Node m:fle.getSubQueryParser().getProjectedCols()){
					if(m.getAgg()!=null && m.getAgg().getAggAliasName()!=null){
						if(n.getColumn().getColumnName().equalsIgnoreCase(m.getAgg().getAggAliasName())){
							logger.info(" agg alias Name "+m.getAgg().getAggAliasName()+" node "+m);
							return m;
						}
					}					
					if(m.getColumn()!=null&&m.getColumn().getColumnName().equalsIgnoreCase(n.getColumn().getColumnName())){	
						logger.info(" column Name found in subQueryParser "+m);
						return m;
					}
					if(m.getAliasName()!=null&&m.getAliasName().equalsIgnoreCase(n.getColumn().getColumnName())){
						logger.info(" column Name found as alias in subQueryParser "+m);
						return m;
					}
				}	
				
				Node k=transformToAbsoluteTableNames(n,fle.getSubQueryParser().getFromListElements(),false, fle.getSubQueryParser());
				if(!n.getTableNameNo().equalsIgnoreCase(k.getTableNameNo()))
					return k;


			}
		}
		
		return n;
	}


	/**
	 * This method returns a node that contains the following structure for Year calculation to handle
	 * queries with  EXTRACT function  :  (Date col Name/(30*12))
	 *  
	 * @param exp
	 * @param exposedName
	 * @param fle
	 * @param isWhereClause
	 * @param queryType
	 * @param qParser
	 * @return
	 * @throws Exception
	 */
	public static Node getYearCalc(ExtractExpression exp, String exposedName, FromListElement fle,boolean isWhereClause, int queryType, QueryParser qParser) throws Exception{
		//Set right value
		Node l=new Node(); // Right of main node - level 1
		//The call will create a left node at level 2
		l.setLeft(getWhereClauseVector(exp.getExpression(),exposedName,fle,isWhereClause, queryType,qParser));
		l.setType(Node.getBaoNodeType());
		l.setOperator("/");
		l.setQueryType(queryType);
		if(queryType == 1) l.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
		if(queryType == 2)l.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
		
				Node rt = new Node();//Right of node level 2
				rt.setOperator("*");
				rt.setType(Node.getBaoNodeType());
				rt.setQueryType(queryType);
				if(queryType == 1) rt.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
				if(queryType == 2) rt.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
					Node leftN = new Node(); //Left node at level 3
					leftN.setType(Node.getValType());
					String s1="30";
					s1=util.Utilities.covertDecimalToFraction(s1);
					leftN.setStrConst(s1);
					Node i = new Node();
					i.setType(Node.getExtractFuncType());
					leftN.setLeft(i);
					//leftN.setLeft(null);
					leftN.setRight(null);
					rt.setLeft(leftN);
					
					Node rightN = new Node();//Right node at level 3 
					rightN.setType(Node.getValType());
					String s2="12";
					s2=util.Utilities.covertDecimalToFraction(s2);
					rightN.setStrConst(s2);
					Node i1 = new Node();
					i1.setType(Node.getExtractFuncType());
					rightN.setLeft(i1);
					//rightN.setLeft(null);
					rightN.setRight(null);
				rt.setRight(rightN);
	
	l.setRight(rt);
	return l;
	}
	
	/**
	 * This method returns a node that contains the following structure for Day calculation to handle
	 * queries with  EXTRACT function  : [Date col Name-((getYearCalc) * 365))]
	 *  
	 * @param exp
	 * @param exposedName
	 * @param fle
	 * @param isWhereClause
	 * @param queryType
	 * @param qParser
	 * @return
	 * @throws Exception
	 */
	public static Node getDayCalc(ExtractExpression exp, String exposedName, FromListElement fle,boolean isWhereClause, int queryType, QueryParser qParser) throws Exception{
	Node n1 = new Node();
	
	n1.setLeft(getWhereClauseVector(exp.getExpression(),exposedName,fle,isWhereClause, queryType,qParser));
	n1.setType(Node.getBaoNodeType());
	n1.setOperator("-");
	n1.setQueryType(queryType);
	
	if(queryType == 1)n1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
	if(queryType == 2)n1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
	
		Node nr1 = new Node();
			nr1.setOperator("*");
			nr1.setType(Node.getBaoNodeType());
			nr1.setQueryType(queryType);
			if(queryType == 1) nr1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
			if(queryType == 2) nr1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
		
		nr1.setLeft(getYearCalc(exp, exposedName, fle, isWhereClause, queryType, qParser));
		
			Node nr1r2 = new Node();
				nr1r2.setType(Node.getValType());
				String s2="365";
				s2=util.Utilities.covertDecimalToFraction(s2);
				nr1r2.setStrConst(s2);
				Node i1 = new Node();
				i1.setType(Node.getExtractFuncType());
				nr1r2.setLeft(i1);
				//nr1r2.setLeft(null);
				nr1r2.setRight(null);
		nr1.setRight(nr1r2);
	n1.setRight(nr1);
	return n1;
	}
	
	
	/**
	 * If node contains expression, set the table name in node as any column and table name in the expression.
	 * To be tested - currently any column inside expression will be returned.
	 * FIXME is that correct?
	 * 
	 * @param n
	 * @return
	 */
	public static Node getTableDetailsForArithmeticExpressions(Node n){
		
		if(n!= null && n.getColumn() == null && n.getLeft() != null && n.getLeft().getColumn() != null && n.getLeft().getTable() != null){
			n.setColumn(n.getLeft().getColumn());
			n.setTable(n.getLeft().getTable());
			n.setTableNameNo(n.getLeft().getTableNameNo());
		}else if(n!= null && n.getLeft() != null){
			n.setLeft(getTableDetailsForArithmeticExpressions(n.getLeft()));
		}
		
		if(n!= null && n.getColumn() == null && n.getRight() != null && n.getRight().getColumn() != null && n.getRight().getTable() != null){
			n.setColumn(n.getRight().getColumn());
			n.setTable(n.getRight().getTable());
			n.setTableNameNo(n.getRight().getTableNameNo());
		}else if(n != null && n.getRight()!= null){
			n.setRight(getTableDetailsForArithmeticExpressions(n.getRight()));
		}

		return n;
		
	}


	public static Node processWhereClauseVector(Object clause, Vector<FromListElement> fle,
			QueryParser qParser, PlainSelect plainSelect) throws Exception {
		try{
			if (clause == null) {
				return null;
			} else if (clause instanceof Parenthesis){
				 boolean isNot = ((Parenthesis) clause).isNot();
				Node n= processWhereClauseVector(((Parenthesis)clause).getExpression(), fle,  qParser,plainSelect);
				if(clause instanceof Parenthesis && isNot){
					Node left = n.getLeft();
					Node right = n.getRight();
					if(left != null && left.getNodeType() != null && 
							left.getNodeType().equals(Node.getBroNodeType())
							&& left.getOperator() != null && left.getOperator().equalsIgnoreCase("=")){
						left.setOperator("/=");
					}
					if(right != null && right.getNodeType() != null && 
							right.getNodeType().equals(Node.getBroNodeType())
							&& right.getOperator() != null && right.getOperator().equalsIgnoreCase("=")){
						right.setOperator("/=");
					}
				
					if(left != null && left.getNodeType() != null && 
							left.getNodeType().equals(Node.getBroNodeType())
							&& left.getOperator() != null && left.getOperator().equalsIgnoreCase("=")){
						left.setOperator("/=");
					}
					if(right != null && right.getNodeType() != null && 
							right.getNodeType().equals(Node.getBroNodeType())
							&& right.getOperator() != null && right.getOperator().equalsIgnoreCase("=")){
						right.setOperator("/=");
					}
				}
				return n;
			}
			else if (clause instanceof Function) {
				Function an = (Function)clause;
				String funcName = an.getName();
			
				
				//All these are string manipulation functions and not aggregate function
				if(! (funcName.equalsIgnoreCase("Lower") || funcName.equalsIgnoreCase("substring") || funcName.equalsIgnoreCase("upper")
						||funcName.equalsIgnoreCase("trim") || funcName.equalsIgnoreCase("postion") || funcName.equalsIgnoreCase("octet_length")
						|| funcName.equalsIgnoreCase("bit_length") || funcName.equalsIgnoreCase("char_length") || funcName.equalsIgnoreCase("overlay"))){
					
					AggregateFunction af = new AggregateFunction();
					if (an.getParameters()!=null){
						ExpressionList anList = an.getParameters();
						List<Expression> expList = anList.getExpressions();//FIXME not only 1 expression but all expressions
		 				Node n = processWhereClauseVector(expList.get(0), fle,  qParser,plainSelect);
						af.setAggExp(n);
						
					} else {
						af.setAggExp(null);
					}
					
					af.setFunc(funcName.toUpperCase());
					af.setAggAliasName(funcName.toUpperCase());
					af.setDistinct(an.isDistinct());
					//af.setAggAliasName(exposedName);
		
					Node agg = new Node();
					agg.setAgg(af);
					agg.setType(Node.getAggrNodeType());
					//Shree added this to set Table, TableNameNo for Aggregate function node - @ node level
					if(af.getAggExp() != null){
						agg.setTable(af.getAggExp().getTable());
						agg.setTableNameNo(af.getAggExp().getTableNameNo());
						agg.setTableAlias(af.getAggExp().getTableAlias());
						agg.setColumn(af.getAggExp().getColumn());
						
					}//Added by Shree for count(*) 
					else if(af.getFunc().toUpperCase().contains("COUNT") && an.isAllColumns()){				
						if(af.getAggExp() == null){
									//Node n1 = Util.getNodeForCount(fle, qParser);
								Node n1 = Util.getNodeForCount(fle, qParser);
									af.setAggExp(n1);
									af.setFunc(funcName.toUpperCase());
									af.setDistinct(an.isDistinct());
									
									agg.setTable(af.getAggExp().getTable());
									agg.setTableNameNo(af.getAggExp().getTableNameNo());
									agg.setTableAlias(af.getAggExp().getTableAlias());
									agg.setColumn(af.getAggExp().getColumn());
									
									agg.setLeft(null);
									agg.setRight(null);
								}
							}
					//Storing sub query details
//					agg.setQueryType(queryType);
//					if(queryType == 1) agg.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//					if(queryType == 2) agg.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
//					//Adding this to the list of aliased names
//					if(exposedName !=null){
//						Vector<Node> present = new Vector<Node>();
//						if( qParser.getAliasedToOriginal().get(exposedName) != null)
//							present = qParser.getAliasedToOriginal().get(exposedName);
//						present.add(agg);
//						qParser.getAliasedToOriginal().put(exposedName, present);
//					}
		
					return agg;
				}
				else {
					//String function manipulation
					Node n=new Node();
					if (an.getParameters()!=null){
						ExpressionList anList = an.getParameters();
						List<Expression> expList = anList.getExpressions();//FIXME not only 1 expression but all expressions
		 				n = processWhereClauseVector(expList.get(0),fle, qParser,plainSelect);		
					}
					return n;
				}
			} else if (clause instanceof DoubleValue) {
				Node n = new Node();
				n.setType(Node.getValType());
				String s=((((DoubleValue)clause).getValue()))+"";
				//String str=(BigIntegerDecimal)((((NumericConstantNode) clause).getValue()).getDouble()).toString();
				s=util.Utilities.covertDecimalToFraction(s);
				n.setStrConst(s);
				n.setLeft(null);
				n.setRight(null);
				return n;

			}else if (clause instanceof LongValue){
				Node n = new Node();
				n.setType(Node.getValType());
				String s=((((LongValue)clause).getValue()))+"";
				s=util.Utilities.covertDecimalToFraction(s);
				n.setStrConst(s);
				n.setLeft(null);
				n.setRight(null);
				return n;
			}
			else if (clause instanceof StringValue) {
				Node n = new Node();
				n.setType(Node.getValType());
				n.setStrConst(((StringValue) clause).getValue());
				n.setLeft(null); 
				n.setRight(null); 
				return n; 
			} else if (clause instanceof Column) {
				Column columnReference = (Column) clause;
				String colName= columnReference.getColumnName();
				String tableName  = columnReference.getTable().getFullyQualifiedName();

				Node n = new Node();
				n.setTableNameNo(tableName);
				n.setColumn(new parsing.Column(colName, tableName));
				//List <FromListElement> frmElementList = fle.getTabs();
//				 if(qParser.getQuery().getQueryString().toLowerCase().contains(("as "+tableName.toLowerCase()))){
//					}
//				 
//				if(qParser.getQuery().getQueryString().toLowerCase().contains(("as "+colName.toLowerCase()))){
//					
//					Vector <Node> value = qParser.getAliasedToOriginal().get(colName);
//					  
//					if(value != null && value.size() > 0){
//						n = value.get(0);//FIXME: vector of nodes not a single node
//					}
//					return n;
	//  
//				}
//				
//				else{
//					n = Util.getColumnFromOccurenceInJC(colName,tableName, fle, qParser);
//					if (n == null) {//then probably the query is correlated				
//						n = Util.getColumnFromOccurenceInJC(colName,tableName, qParser.getQueryAliases(),qParser);
//					}	
//				} 
//				if(n == null) {
//					logger.log(Level.WARNING,"WhereClauseVectorJSQL : Util.getColumnFromOccurenceInJC is not able to find matching Column - Node n = null");
//					return null;
//				}
				 
				n.setType(Node.getColRefType());
				if (tableName != null) {
					n.setTableAlias(tableName);
				} else {
					n.setTableAlias("");
				} 

				if(n.getColumn() != null){
					//n.getColumn().setAliasName(exposedName);
					n.setTable(n.getColumn().getTable());
					
				}
				//if(n.getTableNameNo() == null || n.getTableNameNo().isEmpty()){
					//n.setTableNameNo(tableNameNumber);
				//} 
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
				
				if(n.getTableNameNo()==null||n.getTableNameNo().isEmpty()){
					for(Node m:Util.getAllProjectedColumns(qParser.fromListElements, qParser)){
						if(m.getColumn().getColumnName().equalsIgnoreCase(n.getColumn().getColumnName())){
							n.setTable(m.getTable());
							n.setTableNameNo(m.getTableNameNo());
							break;
						}
					}
				}

				
//				Node tempn=n;
				n=transformToAbsoluteTableNames(n,fle,false, qParser);
//				System.out.println("Before "+tempn+ "after transforming to absolute names "+n);
//				Scanner scan = new Scanner(System.in);
//				String s = scan.next();
				


				if(n.getTableNameNo()==null||n.getTableNameNo().isEmpty()){
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
								if(n.getColumn().getColumnName().equalsIgnoreCase(selExpItem.getAlias().getName())){
									n =ProcessSelectClause.processJoinExpression(e,qParser.fromListElements, qParser,plainSelect);
									break;
								}
							}
						}
					}

				}
				


				
				return n;

			} else if (clause instanceof AndExpression) {
				BinaryExpression andNode = ((BinaryExpression) clause);
				if (andNode.getLeftExpression() != null
						&& andNode.getRightExpression() != null) {
					
					/*if(andNode.getLeftExpression() instanceof ExtractExpression) {
						//type new_name = (type) ;
						return null;
					}else if(andNode.getRightExpression() instanceof ExtractExpression){
						return null;
					}*/
					Node n = new Node();
					Node left = new Node();
					Node right = new Node();
					n.setType(Node.getAndNodeType());
					n.setOperator("AND");
					left = processWhereClauseVector(andNode.getLeftExpression(), fle, qParser,plainSelect);
					right = processWhereClauseVector(andNode.getRightExpression(), fle, qParser,plainSelect);
					
					
					n.setLeft(left);
					n.setRight(right);

					return n;
				}

			} else if (clause instanceof OrExpression) {
				BinaryExpression orNode = ((BinaryExpression) clause);
				if (orNode.getLeftExpression() != null
						&& orNode.getRightExpression() != null) {
					Node n = new Node();
					n.setType(Node.getOrNodeType());
					n.setOperator("OR");
					n.setLeft(processWhereClauseVector(orNode.getLeftExpression(),  fle, qParser,plainSelect));
					n.setRight(processWhereClauseVector(orNode.getRightExpression(), fle, qParser,plainSelect));

					//Storing sub query details
//					n.setQueryType(queryType);
//					if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//					if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

					return n;
				}
			} 

			//Added by Bikash ---------------------------------------------------------------------------------
			else if(clause instanceof LikeExpression){
				BinaryExpression likeNode=((BinaryExpression)clause);
				if (likeNode.getLeftExpression() !=null && likeNode.getRightExpression()!=null )
				{
					//if(likeNode.getReceiver() instanceof ColumnReference && (likeNode.getLeftOperand() instanceof CharConstantNode || likeNode.getLeftOperand() instanceof ParameterNode))
					{
						Node n=new Node();
						if(! likeNode.isNot()){
							n.setType(Node.getLikeNodeType());
							n.setOperator("~");
						}
						else{
							n.setType(Node.getLikeNodeType());
							n.setOperator("!~");
						}
						n.setLeft(processWhereClauseVector(likeNode.getLeftExpression(),fle,qParser,plainSelect));
						n.setRight(processWhereClauseVector(likeNode.getRightExpression(),  fle,qParser,plainSelect));

						//Storing sub query details
//						n.setQueryType(queryType);
//						if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//						if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
						return n;
					}
				}
			}

			else if(clause instanceof JdbcParameter){
				Node n = new Node();
				n.setType(Node.getValType());		
				n.setStrConst("$"+qParser.paramCount);
				qParser.paramCount++;
				n.setLeft(null);
				n.setRight(null);

				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
			}

			//**********************************************************************************/
			else if (clause instanceof Addition){
				BinaryExpression baoNode = ((BinaryExpression)clause);
				Node n = new Node();
				n.setType(Node.getBaoNodeType());
				n.setOperator("+");
				n.setLeft(processWhereClauseVector(baoNode.getLeftExpression(), fle, qParser,plainSelect));
				n.setRight(processWhereClauseVector(baoNode.getRightExpression(),fle, qParser,plainSelect));
				
				n=getTableDetailsForArithmeticExpressions(n);
				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				return n;
			}
			else if (clause instanceof Subtraction){
				BinaryExpression baoNode = ((BinaryExpression)clause);
				Node n = new Node();
				n.setType(Node.getBaoNodeType());
				n.setOperator("-");
				n.setLeft(processWhereClauseVector(baoNode.getLeftExpression(), fle,qParser,plainSelect));
				n.setRight(processWhereClauseVector(baoNode.getRightExpression(), fle, qParser,plainSelect));
				n=getTableDetailsForArithmeticExpressions(n);
				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				return n;
			}
			else if (clause instanceof Multiplication){
				BinaryExpression baoNode = ((BinaryExpression)clause);
				Node n = new Node();
				n.setType(Node.getBaoNodeType());
				n.setOperator("*");
				n.setLeft(processWhereClauseVector(baoNode.getLeftExpression(), fle, qParser,plainSelect));
				n.setRight(processWhereClauseVector(baoNode.getRightExpression(),fle, qParser,plainSelect));
				n=getTableDetailsForArithmeticExpressions(n);
				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				return n;
			}
			else if (clause instanceof Division){
				BinaryExpression baoNode = ((BinaryExpression)clause);
				Node n = new Node();
				n.setType(Node.getBaoNodeType());
				n.setOperator("/");
				n.setLeft(processWhereClauseVector(baoNode.getLeftExpression(),fle, qParser,plainSelect));
				n.setRight(processWhereClauseVector(baoNode.getRightExpression(),fle, qParser,plainSelect));
				n=getTableDetailsForArithmeticExpressions(n);
				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				return n;
			}

			else if (clause instanceof NotEqualsTo) {
				NotEqualsTo broNode = (NotEqualsTo)clause;
				/*if(broNode.getLeftExpression() instanceof ExtractExpression) {
					//type new_name = (type) ;
					Node n = new Node();
					return n;
				}else if(broNode.getRightExpression() instanceof ExtractExpression){
					Node n = new Node();
					return n;
				}*/
				
				//BinaryRelationalOperatorNode broNode = ((BinaryRelationalOperatorNode) clause);			
				Node n = new Node();
				n.setType(Node.getBroNodeType());
				n.setOperator(QueryParser.cvcRelationalOperators[2]);
				n.setLeft(processWhereClauseVector(broNode.getLeftExpression(), fle, qParser,plainSelect));
				n.setRight(processWhereClauseVector(broNode.getRightExpression(), fle, qParser,plainSelect));

				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
				} 
			else if (clause instanceof DoubleAnd) {
				DoubleAnd broNode = (DoubleAnd)clause;	
				
				Node n = new Node();
				n.setType(Node.getBroNodeType());
				n.setOperator(QueryParser.cvcRelationalOperators[7]);
				n.setLeft(processWhereClauseVector(broNode.getLeftExpression(), fle, qParser,plainSelect));
				n.setRight(processWhereClauseVector(broNode.getRightExpression(), fle,qParser,plainSelect));

				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
				} 
			else if (clause instanceof IsNullExpression) {
				IsNullExpression isNullNode = (IsNullExpression) clause;
				Node n = new Node();
				n.setType(Node.getIsNullNodeType());
				n.setLeft(processWhereClauseVector(isNullNode.getLeftExpression(),fle,qParser,plainSelect));
				if(((IsNullExpression) clause).isNot()){
					n.setOperator("!=");
				}else{
					n.setOperator("=");
				}
				n.setRight(null);
				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				return n;
			} else if (clause instanceof InExpression){ 
				//handles NOT and NOT IN both
				InExpression sqn = (InExpression)clause;
				SubSelect subS=null;
				Node inNode=new Node();
				inNode.setType(Node.getInNodeType());
								
				
				Node notNode = new Node();				   
				
				
				Node rhs = new Node();
			
				
				if (sqn. getLeftItemsList() instanceof SubSelect){
					subS=(SubSelect)sqn.getLeftItemsList();
				}
				else if(sqn. getRightItemsList() instanceof SubSelect){ 
					subS=(SubSelect)sqn.getRightItemsList();
				}
				QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
				rhs.setSubQueryParser(subQueryParser);	
				ProcessSelectClause.processWhereSubSelect(subS,subQueryParser,qParser);
				
								 
				Node lhs = processWhereClauseVector(sqn. getLeftExpression(), fle, qParser,plainSelect);
				
				inNode.setLeft(lhs);
				inNode.setRight(rhs);

				if(!sqn.isNot()){					
					return inNode;
				}else{
					notNode.setType(Node.getNotNodeType());
					notNode.setRight(null);
					notNode.setLeft(inNode);
					return notNode;
					
				}
				
			} else if (clause instanceof ExistsExpression){
				
				ExistsExpression sqn = (ExistsExpression)clause;
				SubSelect subS = (SubSelect)sqn.getRightExpression();

				
				QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
				Node existsNode=new Node();
				existsNode.setSubQueryParser(subQueryParser);
				existsNode.setType(Node.getExistsNodeType());
				existsNode.setSubQueryConds(null);
				ProcessSelectClause.processWhereSubSelect(subS,subQueryParser,qParser);
								
				
				Node notNode = new Node();				   
				
				if(!((ExistsExpression) clause).isNot()){					
					return existsNode;
				}else{
					notNode.setType(Node.getNotNodeType());
					notNode.setRight(null);
					notNode.setLeft(existsNode);
					return notNode;
					
				}
				
			}
			else if (clause instanceof SubSelect) {
				SubSelect sqn = (SubSelect) clause;
				
				QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
				Node node=new Node();
				node.setSubQueryParser(subQueryParser);
				node.setType(Node.getBroNodeSubQType());
				ProcessSelectClause.processWhereSubSelect(sqn,subQueryParser,qParser);
								

				
				//PlainSelect ps = sqn.getSelectBody();
				List<SelectItem> rcList = ((PlainSelect)sqn.getSelectBody()).getSelectItems();	
				
				SelectExpressionItem rc = (SelectExpressionItem)rcList.get(0);

				if(rc.getExpression() instanceof Function){ 
					return node;
				}
				else if(rc.getExpression() instanceof ColumnReference || 
						(((Parenthesis)rc.getExpression()).getExpression()) instanceof Column){
					//the result of subquery must be a single tuple
					logger.log(Level.WARNING,"the result of subquery must be a single tuple");
			    }
			}
			else if(clause instanceof Between){
				
				//FIXME: Mahesh If aggregate in where (due to aliased) then add to list of having clause of the subquery
				
				Between bn=(Between)clause;
				Node n=new Node();
				n.setType(Node.getAndNodeType());
				
				Node l=new Node();
				l.setLeft(processWhereClauseVector(bn.getLeftExpression(),fle,qParser,plainSelect));
				l.setOperator(">=");
				l.setRight(processWhereClauseVector(bn.getBetweenExpressionStart(),fle,qParser,plainSelect));
				l.setType(Node.getBroNodeType());
				n.setLeft(l);

				Node r=new Node();
				r.setLeft(processWhereClauseVector(bn.getLeftExpression(), fle,qParser,plainSelect));
				r.setOperator("<=");
				r.setRight(processWhereClauseVector(bn.getBetweenExpressionEnd(),fle,qParser,plainSelect));
				r.setType(Node.getBroNodeType());
				n.setRight(r);

				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				return n;
				//throw new Exception("getWhereClauseVector needs more programming \n"+clause.getClass()+"\n"+clause.toString());
			} else if (clause instanceof EqualsTo){

				BinaryExpression bne = (BinaryExpression)clause;
				Node n = new Node();
				/*if(bne.getLeftExpression() instanceof ExtractExpression) {
					return n;
				}else if(bne.getRightExpression() instanceof ExtractExpression){
					return n;
				}*/
				n.setType(Node.getBroNodeType());
				n.setOperator("=");
				Node ndl = processWhereClauseVector(bne.getLeftExpression(), fle, qParser,plainSelect);
				if(ndl != null){
					n.setLeft(ndl);
				}
				Node ndr = processWhereClauseVector(bne.getRightExpression(), fle, qParser,plainSelect);
				if(ndr!= null){
					n.setRight(ndr);
				}
				
				if((ndl == null && ndr ==null)){
					return null;
				}
				
				return n;
			} else if (clause instanceof GreaterThan){
				GreaterThan broNode = (GreaterThan)clause;
				/*if(broNode.getLeftExpression() instanceof ExtractExpression) {
					//type new_name = (type) ;
					Node n = new Node();
					return n;
				}else if(broNode.getRightExpression() instanceof ExtractExpression){
					Node n = new Node();
					return n;
				}*/
				//BinaryRelationalOperatorNode broNode = ((BinaryRelationalOperatorNode) clause);			
				Node n = new Node();
				n.setType(Node.getBroNodeType());
				n.setOperator(QueryParser.cvcRelationalOperators[3]);
				n.setLeft(processWhereClauseVector(broNode.getLeftExpression(), fle, qParser,plainSelect));
				n.setRight(processWhereClauseVector(broNode.getRightExpression(),fle, qParser,plainSelect));

				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				
				if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					n.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
				}
				else{ 
					if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
					n.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
					}
				}
				
				if(((GreaterThan) clause).getRightExpression() instanceof AllComparisonExpression ||
						((GreaterThan) clause).getLeftExpression() instanceof AllComparisonExpression){

					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAllNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				}
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode;
				} 

				if(((GreaterThan) clause).getRightExpression() instanceof AnyComparisonExpression ||
						((GreaterThan) clause).getLeftExpression() instanceof AnyComparisonExpression){
					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAnyNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				} 
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode; 
				}  
				
				return n;
			}
			else if (clause instanceof GreaterThanEquals){
				GreaterThanEquals broNode = (GreaterThanEquals)clause;
				/*if(broNode.getLeftExpression() instanceof ExtractExpression) {
					//type new_name = (type) ;
					Node n = new Node();
					return n;
				}else if(broNode.getRightExpression() instanceof ExtractExpression){
					Node n = new Node();
					return n;
				}*/
				//BinaryRelationalOperatorNode broNode = ((BinaryRelationalOperatorNode) clause);			
				Node n = new Node();
				n.setType(Node.getBroNodeType());
				n.setOperator(QueryParser.cvcRelationalOperators[4]);
				n.setLeft(processWhereClauseVector(broNode.getLeftExpression(), fle, qParser,plainSelect));
				n.setRight(processWhereClauseVector(broNode.getRightExpression(), fle, qParser,plainSelect));
	 
				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
				
				//Code added for ALL / ANY subqueries - Start
				//FIXME ANy condition needs to be tested - IS this correct???
				if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					n.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
				}
				else{ 
					if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
					n.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
					}
				}
				
				if(((GreaterThanEquals) clause).getRightExpression() instanceof AllComparisonExpression ||
						((GreaterThanEquals) clause).getLeftExpression() instanceof AllComparisonExpression){
					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAllNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				}
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode;
				}
				
				if(((GreaterThanEquals) clause).getRightExpression() instanceof AnyComparisonExpression ||
						((GreaterThanEquals) clause).getLeftExpression() instanceof AnyComparisonExpression){
					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAnyNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				} 
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode; 
				}  

				return n;
			}
			else if (clause instanceof MinorThan){
				BinaryExpression bne = (BinaryExpression)clause;
				/*if(bne.getLeftExpression() instanceof ExtractExpression) {
					//type new_name = (type) ;
					Node n = new Node();
					return n;
				}else if(bne.getRightExpression() instanceof ExtractExpression){
					Node n = new Node();
					return n;
				}*/
				Node n = new Node();
				n.setType(Node.getBroNodeType());
				n.setOperator("<");
				n.setLeft(processWhereClauseVector(bne.getLeftExpression(), fle,qParser,plainSelect));
				n.setRight(processWhereClauseVector(bne.getRightExpression(),fle,qParser,plainSelect));

				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					n.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
				}
				else{ 
					if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
					n.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
					}
				}
				
				if(((MinorThan) clause).getRightExpression() instanceof AllComparisonExpression ||
						((MinorThan) clause).getLeftExpression() instanceof AllComparisonExpression){
					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAllNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				} 
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode;
				} 

				if(((MinorThan) clause).getRightExpression() instanceof AnyComparisonExpression ||
						((MinorThan) clause).getLeftExpression() instanceof AnyComparisonExpression){
					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAnyNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				} 
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode; 
				}  
				
				return n;
			} else if (clause instanceof MinorThanEquals){
				BinaryExpression bne = (BinaryExpression)clause;
				Node n = new Node();
				/*if(bne.getLeftExpression() instanceof ExtractExpression) {
					//type new_name = (type) ;
					return n;
				}else if(bne.getRightExpression() instanceof ExtractExpression){
					return n;
				}*/
				n.setType(Node.getBroNodeType());
				n.setOperator("<=");
				n.setLeft(processWhereClauseVector(bne.getLeftExpression(), fle,qParser,plainSelect));
				n.setRight(processWhereClauseVector(bne.getRightExpression(),fle, qParser,plainSelect));

				//Storing sub query details
//				n.setQueryType(queryType);
//				if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//				if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);

				if(n.getLeft() != null && n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
					n.setSubQueryConds(n.getLeft().getSubQueryConds());
					n.getLeft().getSubQueryConds().clear();
				}
				else{ 
					if(n.getRight() != null &&n.getRight().getSubQueryConds() != null && n.getRight().getSubQueryConds().size() >0 ){
					n.setSubQueryConds(n.getRight().getSubQueryConds());
					n.getRight().getSubQueryConds().clear();
					}
				}
				if(((MinorThanEquals) clause).getRightExpression() instanceof AllComparisonExpression ||
						((MinorThanEquals) clause).getLeftExpression() instanceof AllComparisonExpression){
					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAllNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAllNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				} 
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode;
				} 
				
				if(((MinorThanEquals) clause).getRightExpression() instanceof AnyComparisonExpression ||
						((MinorThanEquals) clause).getLeftExpression() instanceof AnyComparisonExpression){
					Node sqNode = new Node();
					/* the expression: Node.getAllAnyNodeType() from the statement below removed, and 
					 * Node.getAnyNodeType() in the following statement added by mathew on 27 June 2016
					 */
					sqNode.setType(Node.getAnyNodeType());
					if(n.getLeft().subQueryConds != null && n.getLeft().subQueryConds.size() > 0){
						sqNode.setSubQueryConds(n.getLeft().getSubQueryConds());
						n.getLeft().getSubQueryConds().clear();
				} 
					else{ 
						sqNode.setSubQueryConds(n.getRight().getSubQueryConds());
						n.getRight().getSubQueryConds().clear();
				} 
					sqNode.setLhsRhs(n);
					return sqNode; 
				}  
				
				return n;
			} else if(clause instanceof CaseExpression){
				CaseExpression expr =  (CaseExpression)clause;
				Node n = new Node();
				List<Expression> whenExprList = expr.getWhenClauses();
				Vector <Node> caseConditionNode = new Vector<Node>();
				//If it is a case expression, then create a vector of nodes that holds case condition and else cond
				//Add that to cvc or qparser and return a node that is of type casecondition.

					if(expr.getElseExpression() != null){
						n = processWhereClauseVector(expr.getElseExpression(), fle, qParser,plainSelect);
					}
					else if(expr.getWhenClauses() != null){
						for(int i = 0; i < expr.getWhenClauses().size();i++){
							Expression ex = expr.getWhenClauses().get(i);
							n = processWhereClauseVector(ex, fle,qParser,plainSelect);
						}
		
					return n;
					}else{
						return null;
					}
			}
			else if (clause instanceof AllComparisonExpression){
				
				AllComparisonExpression ace = (AllComparisonExpression)clause;
				SubSelect ss = ace.getSubSelect();
				
				QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
				Node allNode=new Node();
				allNode.setSubQueryParser(subQueryParser);
				allNode.setType(Node.getAllNodeType());
				ProcessSelectClause.processWhereSubSelect(ss,subQueryParser,qParser);
				
				return allNode;				

			}
			else if (clause instanceof AnyComparisonExpression){
				AnyComparisonExpression ace = (AnyComparisonExpression)clause;
				SubSelect ss = ace.getSubSelect();
				
				QueryParser subQueryParser=new QueryParser(qParser.getTableMap());
				Node anyNode=new Node();
				anyNode.setSubQueryParser(subQueryParser);
				anyNode.setType(Node.getAnyNodeType());
				ProcessSelectClause.processWhereSubSelect(ss,subQueryParser,qParser);
				
				return anyNode;
			}
			else if(clause instanceof ExtractExpression){
				//To extract approximate no: of days in month is considered as 30.
				/*Assuming the ExtractExpression clause holds name and Column alone*/
					ExtractExpression exp = (ExtractExpression)clause;
					Node n=new Node(); // Main node
					String name = exp.getName();
					Node table = processWhereClauseVector(exp.getExpression(),fle, qParser,plainSelect);
					if(name.equalsIgnoreCase("year")){
						//Formula : 1970+ (col Name/(30*12)) - create 5 nodes
						n.setOperator("+"); // Main node
						n.setType(Node.getBaoNodeType());
//						n.setQueryType(queryType);
//						if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//						if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
						Node n1 = new Node(); //Left of main node - level 1
						n1.setType(Node.getValType());
						String s="1970";
						s=util.Utilities.covertDecimalToFraction(s);
						n1.setStrConst(s);
						Node i = new Node();
						i.setType(Node.getExtractFuncType());
						n1.setLeft(i);
						//n1.setLeft(null);
						n1.setRight(null);
						n1.setTable(table.getTable());
						n1.tableNameNo = table.getTableNameNo();
						n1.setColumn(table.getColumn());
						n.setLeft(n1);
						
						//Call method to get Node :
						
						//n.setRight(getYearCalc(exp,exposedName,fle,isWhereClause,queryType,qParser));
						
						
					}else if(name.equalsIgnoreCase("month")){
						//Formula 1 : (col Name/30) MOD 12 But as CVC does not support MOD rewrite the formula 
						// Formula 2: a mod b = a - (a/b) *b => (col Name/30)-((col Name/30)/12) * 12) create 12 nodes
						n.setOperator("-"); // Main node
						n.setType(Node.getBaoNodeType());
//						n.setQueryType(queryType);
//						if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//						if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
						
							//LEFT OF MAIN NODE - considered as level 0
							Node nl1 = new Node(); 
								nl1.setOperator("/");
								nl1.setType(Node.getBaoNodeType());
//								nl1.setQueryType(queryType);
//								if(queryType == 1) nl1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//								if(queryType == 2)nl1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
								//Left of node at level 1
								nl1.setLeft(processWhereClauseVector(exp.getExpression(),fle,qParser,plainSelect));
								
									Node nl1r1 = new Node();
									nl1r1.setType(Node.getValType());
									String s="30";
									s=util.Utilities.covertDecimalToFraction(s);
									nl1r1.setStrConst(s);
									Node i = new Node();
									i.setType(Node.getExtractFuncType());
									nl1r1.setLeft(i);
									//nl1r1.setLeft(null);
									nl1r1.setRight(null);
							nl1.setRight(nl1r1);
						n.setTable(table.getTable());
						n.tableNameNo = table.getTableNameNo();
						n.setColumn(table.getColumn());
						n.setLeft(nl1);
						
						//RIGHT OF MAIN NODE  - considered as level 0
						Node nr1 = new Node();
							nr1.setOperator("*");
							nr1.setType(Node.getBaoNodeType());
//							nr1.setQueryType(queryType);
//							if(queryType == 1) nr1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//							if(queryType == 2) nr1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
							//Left Node at level 1
								Node nr1l1 = new Node();
									nr1l1.setOperator("/");
									nr1l1.setType(Node.getBaoNodeType());
//									nr1l1.setQueryType(queryType);
//									if(queryType == 1) nr1l1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//									if(queryType == 2) nr1l1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
											//Left Node at level 2
											Node nr1l2 = new Node();
												nr1l2.setOperator("/");
												nr1l2.setType(Node.getBaoNodeType());
//												nr1l2.setQueryType(queryType);
//												if(queryType == 1) nr1l2.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//												if(queryType == 2) nr1l2.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
												//Left Node at level 3
												nr1l2.setLeft(processWhereClauseVector(exp.getExpression(),fle,qParser,plainSelect));
												
												//Right Node at level 3
												Node nr1r3 = new Node();
													nr1r3.setType(Node.getValType());
													String st="30";
													st=util.Utilities.covertDecimalToFraction(st);
													nr1r3.setStrConst(st);
													Node i1 = new Node();
													i1.setType(Node.getExtractFuncType());
													nr1r3.setLeft(i1);
													nr1r3.setRight(null);
												nr1l2.setRight(nr1r3);
												
											
									nr1l1.setLeft(nr1l2);	
										//Right Node at level 2
											Node nr1r2 = new Node();
											nr1r2.setType(Node.getValType());
											String s1="12";
											s1=util.Utilities.covertDecimalToFraction(s1);
											nr1r2.setStrConst(s1);
											Node i2 = new Node();
											i2.setType(Node.getExtractFuncType());
											nr1r2.setLeft(i2);
											//nr1r2.setLeft(null);
											nr1r2.setRight(null);
									nr1l1.setRight(nr1r2);
							nr1.setLeft(nr1l1);
							
							//Right Node at level 1
								Node nr1r1 = new Node();
								nr1r1.setType(Node.getValType());
								String s2="12";
								s2=util.Utilities.covertDecimalToFraction(s2);
								nr1r1.setStrConst(s2);
								Node i4 = new Node();
								i4.setType(Node.getExtractFuncType());
								nr1r1.setLeft(i4);
								//nr1r1.setLeft(null);
								nr1r1.setRight(null);
							nr1.setRight(nr1r1);
						
					 n.setRight(nr1);
						
					}else if(name.equalsIgnoreCase("day")){
						//Formula 1: approximate Date : (column value -((col Value/(30*12))*365) MOD 30)
						//Formula 2 : eliminating MOD :[ (column value -((col Value/(30*12))*365)) - ([(column value -((col Value/(30*12))*365))/30] * 30) ]
						n.setOperator("-"); // Main node
						n.setType(Node.getBaoNodeType());
//						n.setQueryType(queryType);
						n.setTable(table.getTable());
						n.tableNameNo = table.getTableNameNo();
						n.setColumn(table.getColumn());
//						if(queryType == 1) n.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//						if(queryType == 2) n.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
						//Left of main node
//						n.setLeft(getDayCalc(exp, exposedName, fle, isWhereClause, queryType, qParser));
							//Right of main node
							Node nr1 = new Node();
							nr1.setOperator("*");
							nr1.setType(Node.getBaoNodeType());
//							nr1.setQueryType(queryType);
//							if(queryType == 1) nr1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//							if(queryType == 2) nr1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
							
								Node nr1l1 = new Node();
								nr1l1.setOperator("/");
								nr1l1.setType(Node.getBaoNodeType());
//								nr1l1.setQueryType(queryType);
//								if(queryType == 1) nr1l1.setQueryIndex(qParser.getFromClauseSubqueries().size()-1);
//								if(queryType == 2) nr1l1.setQueryIndex(qParser.getWhereClauseSubqueries().size()-1);
//								nr1l1.setLeft(getDayCalc(exp, exposedName, fle, isWhereClause, queryType, qParser));
										Node nr1r2 = new Node();
										nr1r2.setType(Node.getValType());
										String st="30";
										st=util.Utilities.covertDecimalToFraction(st);
										nr1r2.setStrConst(st);
										Node i = new Node();
										i.setType(Node.getExtractFuncType());
										nr1r2.setLeft(i);
										//nr1r2.setLeft(null);
										nr1r2.setRight(null);
								nr1l1.setRight(nr1r2);
							nr1.setLeft(nr1l1);
							
								Node nr1r1 = new Node();
								nr1r1.setType(Node.getValType());
								String s2="30";
								s2=util.Utilities.covertDecimalToFraction(s2);
								nr1r1.setStrConst(s2);
								Node i1 = new Node();
								i1.setType(Node.getExtractFuncType());
								nr1r1.setLeft(i1);
								//nr1r1.setLeft(null);
								nr1r1.setRight(null);
							nr1.setRight(nr1r1);
						n.setRight(nr1);
					}
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

