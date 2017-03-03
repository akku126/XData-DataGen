package parsing;

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.iapi.types.SQLDouble;
import org.apache.derby.iapi.types.SQLInteger;
import org.apache.derby.impl.sql.compile.AggregateNode;
import org.apache.derby.impl.sql.compile.AllResultColumn;
import org.apache.derby.impl.sql.compile.AndNode;
import org.apache.derby.impl.sql.compile.BinaryArithmeticOperatorNode;
import org.apache.derby.impl.sql.compile.BinaryOperatorNode;
import org.apache.derby.impl.sql.compile.CharConstantNode;
import org.apache.derby.impl.sql.compile.ColumnReference;
import org.apache.derby.impl.sql.compile.CursorNode;
import org.apache.derby.impl.sql.compile.FromBaseTable;
import org.apache.derby.impl.sql.compile.FromList;
import org.apache.derby.impl.sql.compile.FromSubquery;
import org.apache.derby.impl.sql.compile.GroupByColumn;
import org.apache.derby.impl.sql.compile.GroupByList;
import org.apache.derby.impl.sql.compile.HalfOuterJoinNode;
import org.apache.derby.impl.sql.compile.InListOperatorNode;
import org.apache.derby.impl.sql.compile.IsNullNode;
import org.apache.derby.impl.sql.compile.JoinNode;
import org.apache.derby.impl.sql.compile.LikeEscapeOperatorNode;
import org.apache.derby.impl.sql.compile.NotNode;
import org.apache.derby.impl.sql.compile.NumericConstantNode;
import org.apache.derby.impl.sql.compile.OrNode;
import org.apache.derby.impl.sql.compile.QueryTreeNode;
import org.apache.derby.impl.sql.compile.ResultColumn;
import org.apache.derby.impl.sql.compile.ResultColumnList;
import org.apache.derby.impl.sql.compile.ResultSetNode;
import org.apache.derby.impl.sql.compile.SQLParser;
import org.apache.derby.impl.sql.compile.SelectNode;
import org.apache.derby.impl.sql.compile.SubqueryNode;
import org.apache.derby.impl.sql.compile.ValueNode;
import org.apache.derby.impl.sql.compile.ValueNodeList;

public class Utility {
	
	private static Logger logger = Logger.getLogger(Utility.class.getName());
	
	private static String getSelectClause (ResultColumnList rsColumnList) throws StandardException {
		if(rsColumnList==null)
			return null;
		String columnString = "";
		for (int i=1;i<=rsColumnList.size();i++) {
			ResultColumn rsColumn = rsColumnList.getResultColumn(i);
			if (rsColumn instanceof AllResultColumn) {
				columnString = "*";
			} else if (rsColumn.getExpression() instanceof ColumnReference){
				String colName = "";
				if (rsColumn.getTableName() != null && !rsColumn.getTableName().equals("")) {
					colName = rsColumn.getTableName()+"."+rsColumn.getName();
				} else {
					colName = rsColumn.getName();
				}
				
				if (rsColumn.getColumnName() != null && !rsColumn.getColumnName().equals("") && !rsColumn.getColumnName().equals(rsColumn.getName())) {
					colName = colName + " AS "+ rsColumn.getColumnName();
				}
				if (columnString.trim().equals("")) {
					columnString = colName;
				} else {
					columnString = columnString + ","+colName;
				}
			} else if (rsColumn.getExpression() instanceof AggregateNode){
				String colName = ((AggregateNode)rsColumn.getExpression()).getAggregateName();
				
				AggregateNode rsValueNode = (AggregateNode)rsColumn.getExpression();
				if (columnString.trim().equals("")) {
					columnString = colName;
				} else {
					columnString = columnString + ","+colName;
				}
			}
		}
		return columnString;
	}
	
	private static String getFromClause (QueryTreeNode node, QueryParser qp, HashMap<String, String> currentAT) throws Exception{
		if (node instanceof HalfOuterJoinNode) {
			HalfOuterJoinNode halfOuterJoinNode = (HalfOuterJoinNode) node;
			String leftPart = getFromClause(halfOuterJoinNode.getLeftResultSet(), qp, currentAT);
			String rightPart = getFromClause(halfOuterJoinNode.getRightResultSet(), qp, currentAT);
			ResultColumnList rsColumnList = halfOuterJoinNode.getUsingClause();
			ValueNode joinClause = halfOuterJoinNode.getJoinClause();
			String joinClauseString = "";
			String usingClause = null;
			if (rsColumnList != null) {
				usingClause = getSelectClause(rsColumnList);
			}
			String str;
			if (joinClause != null) {
				if ((str=getWhereClause(joinClause,false)) != null && !str.trim().equals("")) {
					joinClauseString = " ON ("+ getWhereClause(joinClause,false)+")";
				}
			} else if (usingClause != null && !usingClause.equals("")) {
				joinClauseString = " USING ("+ usingClause+")";
			}
			String finalString = "";
			if(halfOuterJoinNode.isRightOuterJoin())
				finalString = leftPart + " RIGHT OUTER JOIN "+rightPart+joinClauseString;
			else
				finalString = leftPart + " LEFT OUTER JOIN "+rightPart+joinClauseString;
			
			return finalString;
			
			
		} else if (node instanceof JoinNode) {
			JoinNode joinNode = (JoinNode) node;
			String leftPart = getFromClause(joinNode.getLeftResultSet(), qp, currentAT);
			String rightPart = getFromClause(joinNode.getRightResultSet(), qp, currentAT);
			ResultColumnList rsColumnList = joinNode.getUsingClause();
			ValueNode joinClause = joinNode.getJoinClause();
			String joinClauseString ="";
			String usingClause = getSelectClause(rsColumnList);
			String str;
			if (joinClause != null) {
				if ((str=getWhereClause(joinClause,false)) != null && !str.trim().equals("")) { 
					joinClauseString = " ON ("+ str+")";
				}
			} else if (usingClause != null && !usingClause.trim().equals("")) {
				joinClauseString = " USING ("+ usingClause+")";	
			} 
			
			String finalString = leftPart + " INNER JOIN "+rightPart+joinClauseString;
			return finalString;
		} else if (node instanceof FromBaseTable){
			FromBaseTable fromBaseTable = (FromBaseTable) node;
			if (fromBaseTable.getCorrelationName() != null) {
				if(currentAT !=null)
					currentAT.put(fromBaseTable.getCorrelationName(), fromBaseTable.getBaseTableName());
				return fromBaseTable.getBaseTableName() + " AS "+fromBaseTable.getTableName(); 
			} 
			if(currentAT!=null)
				currentAT.put(fromBaseTable.getCorrelationName(), fromBaseTable.getBaseTableName());
			return fromBaseTable.getTableName().getTableName();
		}
		else if (node instanceof FromSubquery){
			FromSubquery subquery = (FromSubquery)node;
			String aliasName = subquery.getCorrelationName();
			if(currentAT!=null)
				currentAT.put(aliasName, "SUBQUERY");
			ResultSetNode rsNode = subquery.getSubquery();
			String subqString = qp.getModifiedQuery(rsNode, true, qp);
			return "(" + subqString + ") " + aliasName; 
		}
		return null;
	}
	
	private static String getWhereClause(Object clause, boolean removeConstants) throws Exception{
		if (clause ==null) {
			return "";
		}
		String leftPart = "";
		String rightPart = "";
		if (clause instanceof AndNode) {
			AndNode andNode = ((AndNode)clause);
			if (andNode.getLeftOperand() == null) {
				leftPart = "";
			} else {
				leftPart = getWhereClause(andNode.getLeftOperand(),removeConstants); 
				if (!leftPart.equals("")) {
					leftPart = "(" + leftPart +")";
				}
			}
			
			if (andNode.getRightOperand() == null) {
				rightPart = "";
			} else {
				rightPart = getWhereClause(andNode.getRightOperand(),removeConstants); 
				if (!rightPart.equals("")) {
					rightPart = "(" + rightPart +")";
				}
			}
			
			if (!leftPart.equals("") && !rightPart.equals("")) {
				return leftPart + " AND " + rightPart;
			} 
			return leftPart + rightPart;
		} else if (clause instanceof OrNode) {
			OrNode orNode = (OrNode) clause;
			if ((orNode.getLeftOperand() == null)) {
				leftPart = "";
			} else {
				leftPart = getWhereClause(orNode.getLeftOperand(),removeConstants); 
				if (!leftPart.equals("")) {
					leftPart = "(" + leftPart +")";
				}
			}
			
			if ((orNode.getRightOperand() == null)) {
				rightPart = "";
			} else {
				rightPart = getWhereClause(orNode.getRightOperand(),removeConstants); 
				if (!rightPart.equals("")) {
					rightPart = "(" + rightPart +")";
				}
			}
			
			if (!leftPart.equals("") && !rightPart.equals("")) {
				return leftPart + " OR " + rightPart;
			} 
			return leftPart + rightPart;
		} else if (clause instanceof BinaryOperatorNode) {
			
			BinaryOperatorNode binaryOperatorNode = (BinaryOperatorNode) clause;
			//TODO Remove removeConstants' code if not required
			if(removeConstants){
				if(binaryOperatorNode.getLeftOperand() instanceof CharConstantNode 
						|| binaryOperatorNode.getLeftOperand() instanceof NumericConstantNode
						|| binaryOperatorNode.getRightOperand() instanceof CharConstantNode
						|| binaryOperatorNode.getRightOperand() instanceof NumericConstantNode)
					return "";
			}
			
			if (binaryOperatorNode.getLeftOperand() instanceof ColumnReference) {
				leftPart = binaryOperatorNode.getLeftOperand().getColumnName();
				if (binaryOperatorNode.getLeftOperand().getTableName()!=null) {
					leftPart = binaryOperatorNode.getLeftOperand().getTableName()+"."+leftPart;
				}
			} else if (binaryOperatorNode.getLeftOperand() instanceof CharConstantNode) {
				leftPart = "'"+((CharConstantNode)binaryOperatorNode.getLeftOperand()).getValue().getString()+"'";
			} else if (binaryOperatorNode.getLeftOperand() instanceof NumericConstantNode) {
				if (((NumericConstantNode)binaryOperatorNode.getLeftOperand()).getValue() instanceof SQLInteger) {
					leftPart = new Integer(((SQLInteger)((NumericConstantNode)binaryOperatorNode.getLeftOperand()).getValue()).getInt()).toString();
				} else if (((NumericConstantNode)binaryOperatorNode.getLeftOperand()).getValue() instanceof SQLDouble) {
					leftPart = new Double(((SQLDouble)((NumericConstantNode)binaryOperatorNode.getLeftOperand()).getValue()).getDouble()).toString();
				}
			} /*else if (binaryOperatorNode.getLeftOperand() instanceof BinaryArithmeticOperatorNode) {}*/
			
			if (binaryOperatorNode.getRightOperand() instanceof ColumnReference) {
				rightPart = binaryOperatorNode.getRightOperand().getColumnName();
				if (binaryOperatorNode.getRightOperand().getTableName()!=null) {
					rightPart = binaryOperatorNode.getRightOperand().getTableName()+"."+rightPart;
				}
			} else if (binaryOperatorNode.getRightOperand() instanceof CharConstantNode) {
				rightPart = "'"+((CharConstantNode)binaryOperatorNode.getRightOperand()).getValue().getString()+"'";
			} else if (binaryOperatorNode.getRightOperand() instanceof NumericConstantNode) {
				if (((NumericConstantNode)binaryOperatorNode.getRightOperand()).getValue() instanceof SQLInteger) {
					rightPart = new Integer(((SQLInteger)((NumericConstantNode)binaryOperatorNode.getRightOperand()).getValue()).getInt()).toString();
				} else if (((NumericConstantNode)binaryOperatorNode.getRightOperand()).getValue() instanceof SQLDouble) {
					rightPart = new Double(((SQLDouble)((NumericConstantNode)binaryOperatorNode.getRightOperand()).getValue()).getDouble()).toString();
				}
			} else if (binaryOperatorNode.getRightOperand() instanceof BinaryArithmeticOperatorNode) {
				BinaryArithmeticOperatorNode binaryArithmeticOperatorNode = (BinaryArithmeticOperatorNode) binaryOperatorNode.getRightOperand();
				
				rightPart = "(" + getWhereClause(binaryArithmeticOperatorNode.getLeftOperand(),false) 
					+ binaryArithmeticOperatorNode.getOperator1() + getWhereClause(binaryArithmeticOperatorNode.getRightOperand(),false) + ")"; 
			}
			return leftPart +" "+binaryOperatorNode.getOperator1()+" "+rightPart;
		} else if (clause instanceof InListOperatorNode) {
			InListOperatorNode inListOperatorNode = (InListOperatorNode) clause;
			ValueNode valueNode = inListOperatorNode.getLeftOperand();
			String str1 = "";
			if (valueNode instanceof ColumnReference) {
				ColumnReference columnReference = (ColumnReference) valueNode;
				if (columnReference.getTableName() != null) {
					str1 = columnReference.getTableName() + "." + columnReference.getColumnName();
				} else {
					str1 = columnReference.getColumnName();
				}
				ValueNodeList valueNodeList = inListOperatorNode.getRightOperandList();
				String str2 = "";
				for (int i=0;i<valueNodeList.size();i++) {
					if (valueNodeList.getNodeVector().get(i) instanceof CharConstantNode) {
						CharConstantNode charConstantNode = (CharConstantNode) valueNodeList.getNodeVector().get(i);
						str2 = str2 + ",'" + charConstantNode.getValue().getString()+"'";
					} else if (valueNodeList.getNodeVector().get(i) instanceof NumericConstantNode) {
						if ( ((NumericConstantNode)valueNodeList.getNodeVector().get(i)).getValue() instanceof SQLInteger) {
							str2 = str2 +","+ new Integer(((SQLInteger)((NumericConstantNode)valueNodeList.getNodeVector().get(i)).getValue()).getInt()).toString();
						} else if ( ((NumericConstantNode)valueNodeList.getNodeVector().get(i)).getValue() instanceof SQLDouble){
							str2 = str2 +","+ new Double(((SQLDouble)((NumericConstantNode)valueNodeList.getNodeVector().get(i)).getValue()).getDouble()).toString();
						}
					} else {
						throw new Exception("getStringWhereClause needs more programming ");
					}
				}
				str2 = str2.substring(1,str2.length());
				return str1 +" IN ("+str2 +")";
			} else {
				throw new Exception("getStringWhereClause needs more programming ");
			}
		} else if (clause instanceof NotNode) {
			NotNode notNode = (NotNode) clause;
			ValueNode valueNode = notNode.getOperand();
			return " NOT " + getWhereClause(valueNode,false);
		} else if (clause instanceof ColumnReference) {
			ColumnReference columnReference = (ColumnReference) clause;
			String str;
			str = columnReference.getColumnName();
			if (columnReference.getTableName()!=null) {
				str = columnReference.getTableName()+"."+str;
			}
			return str;
		} else if (clause instanceof IsNullNode) {
			IsNullNode isNullNode = (IsNullNode) clause;
			if (isNullNode.getOperand() instanceof ColumnReference) {
				return getWhereClause(isNullNode.getOperand(),false) + " IS NULL ";
			} 
			throw new Exception("getStringWhereClause needs more programming ");
		} else if (clause instanceof SubqueryNode) {
			SubqueryNode subqueryNode = (SubqueryNode) clause;
			String columnName = getWhereClause(subqueryNode.getLeftOperand(),false); 
			if (subqueryNode.getTableName() != null) {
				columnName = subqueryNode.getTableName() + "." +columnName;
			} 
			String subQueryString = getQueryString(subqueryNode.getResultSet());
			return columnName + " IN (" + subQueryString +")";
		} else if (clause instanceof LikeEscapeOperatorNode) {
			LikeEscapeOperatorNode likeEscapeOperatorNode = (LikeEscapeOperatorNode) clause;
			if (likeEscapeOperatorNode.getReceiver() instanceof ColumnReference) {
				if (likeEscapeOperatorNode.getLeftOperand() instanceof CharConstantNode ) {
					String columnName = likeEscapeOperatorNode.getReceiver().getColumnName();
					if (likeEscapeOperatorNode.getReceiver().getTableName() != null) {
						columnName = likeEscapeOperatorNode.getReceiver().getTableName() + "." +columnName;
					} 
					String str2 = ((CharConstantNode)likeEscapeOperatorNode.getLeftOperand()).getValue().getString();
					return columnName + " LIKE '"+str2+"'";
				} else {
					throw new Exception("getStringWhereClause needs more programming ");
				}
			} else {
				throw new Exception("getStringWhereClause needs more programming ");
			}
		} else if (clause instanceof NumericConstantNode) {
			if ( ((NumericConstantNode)clause).getValue() instanceof SQLInteger) {
				return new Integer(((SQLInteger)((NumericConstantNode)clause).getValue()).getInt()).toString();
			} else if ( ((NumericConstantNode)clause).getValue() instanceof SQLDouble){
				return new Double(((SQLDouble)((NumericConstantNode)clause).getValue()).getDouble()).toString();
			}
			return "";
		} else {
			throw new Exception("getStringWhereClause needs more programming ");
		}
	}

	public static String getGroupByClauseString(GroupByList groupByList) {
		String str = "";
		if (groupByList != null) {
			Vector v = groupByList.getNodeVector();
			for (int i=0;i<v.size();i++) {
				if (i!=0) {
					str += ",";
				}
				Object o = v.get(i);
				if (o instanceof GroupByColumn) {
					if (((GroupByColumn)o).getColumnExpression() instanceof ColumnReference) {
						ColumnReference colRef = (ColumnReference)((GroupByColumn)o).getColumnExpression();
						if (colRef.getTableName()!= null && !colRef.getTableName().equals("")) {
							str += colRef.getTableName()+"."+colRef.getColumnName();
						} else {
							str += colRef.getColumnName();
						}
					}
				}
			}
		}
		
		return str.equals("")?"":" GROUP BY "+str;
	}
	
	public static String getGroupByClauseAttributes(GroupByList groupByList) {
		String str = "";
		if (groupByList != null) {
			Vector v = groupByList.getNodeVector();
			for (int i=0;i<v.size();i++) {
				if (i!=0) {
					str += ",";
				}
				Object o = v.get(i);
				if (o instanceof GroupByColumn) {
					if (((GroupByColumn)o).getColumnExpression() instanceof ColumnReference) {
						ColumnReference colRef = (ColumnReference)((GroupByColumn)o).getColumnExpression();
						if (colRef.getTableName()!= null && !colRef.getTableName().equals("")) {
							str += colRef.getTableName()+"."+colRef.getColumnName();
						} else {
							str += colRef.getColumnName();
						}
					}
				}
			}
		}
		
		return str;
	}
	
	public static String getSelectClauseString(ResultSetNode rsNode) throws Exception{
		String selectStr = getSelectClause(rsNode.getResultColumns());
		String distinctStr = "";
		if ( ((SelectNode)rsNode).hasDistinct() ) {
			distinctStr = "DISTINCT ";
		}
		return "SELECT "+distinctStr+ selectStr;
	}
	
	public static String getFromClauseString(ResultSetNode rsNode, QueryParser qp, HashMap<String, String> currentAT) throws Exception{
		String tempStr = "";
		FromList fromList = rsNode.getFromList();
		for (int i=0;i<fromList.size();i++) {
			tempStr += getFromClause(fromList.getNodeVector().get(i), qp, currentAT);
			if(i<fromList.size()-1)
				tempStr += ", ";
		}
		String fromClause = " FROM "+ tempStr+" ";
		return fromClause;
		
	}
	
	public static String getWhereClauseString(ResultSetNode rsNode, boolean removeContants) throws Exception{
		Object object = ((SelectNode)rsNode).getWhereClause();
		String whereClause = getWhereClause(object,removeContants);
		if(!whereClause.trim().equals(""))
			whereClause = " WHERE "+whereClause+" ";
		return whereClause;
	}
	
	public static String getQueryString (ResultSetNode rsNode) throws Exception {
		String selectClauseString = getSelectClauseString(rsNode);
		String fromClauseString = getFromClauseString(rsNode, null, null);
		String whereClauseString = getWhereClauseString(rsNode, false);
		String groupByClauseString = getGroupByClauseString(((SelectNode)rsNode).getGroupByList());
		
		return selectClauseString + fromClauseString + whereClauseString + groupByClauseString;
	}
	
	public static boolean isContantWhereClause(Object clause, Vector joinAttribute) throws Exception{
		if (clause ==null) {
			return false;
		}
		boolean inLeftPart = false, inRightPart = false;
		if (clause instanceof AndNode) {
			AndNode andNode = ((AndNode)clause);
			
			//System.out.println("andNode.getLeftOperand() = "+andNode.getLeftOperand());
			if (andNode.getLeftOperand() == null)
				inLeftPart = false;
			else
				inLeftPart = isContantWhereClause(andNode.getLeftOperand(), joinAttribute);
			
			//System.out.println("andNode.getRightOperand() = "+andNode.getRightOperand());
			if (andNode.getRightOperand() == null)
				inRightPart = false;
			else
				inRightPart = isContantWhereClause(andNode.getRightOperand(), joinAttribute);
			
			return inLeftPart && inRightPart;
			
		} else if (clause instanceof OrNode) {
			OrNode orNode = (OrNode) clause;
			
			//System.out.println("orNode.getLeftOperand() = "+orNode.getLeftOperand());
			if ((orNode.getLeftOperand() == null))
				inLeftPart = false;
			else
				inLeftPart = isContantWhereClause(orNode.getLeftOperand(), joinAttribute); 
			
			//System.out.println("orNode.getRightOperand() = "+orNode.getRightOperand());
			if ((orNode.getRightOperand() == null))
				inRightPart = false;
			else
				inRightPart = isContantWhereClause(orNode.getRightOperand(), joinAttribute); 
			
			return inLeftPart || inRightPart;
			
		} else if (clause instanceof BinaryOperatorNode) {
			
			BinaryOperatorNode binaryOperatorNode = (BinaryOperatorNode) clause;
			
			//System.out.println("binaryOperatorNode.getLeftOperand() = "+binaryOperatorNode.getLeftOperand().getColumnName());
			//System.out.println("binaryOperatorNode.getRightOperand() = "+binaryOperatorNode.getRightOperand().getColumnName());
			
			if(binaryOperatorNode.getLeftOperand() instanceof CharConstantNode || binaryOperatorNode.getLeftOperand() instanceof NumericConstantNode ){
				if(joinAttribute.contains(binaryOperatorNode.getRightOperand().getColumnName().toString()))
					return true;
			}
			if (binaryOperatorNode.getRightOperand() instanceof CharConstantNode || binaryOperatorNode.getRightOperand() instanceof NumericConstantNode){
				if(joinAttribute.contains(binaryOperatorNode.getLeftOperand().getColumnName().toString()))
					return true;
			}
				
			if (binaryOperatorNode.getLeftOperand() instanceof BinaryArithmeticOperatorNode) {
				BinaryArithmeticOperatorNode binaryArithmeticOperatorNode = (BinaryArithmeticOperatorNode) binaryOperatorNode.getLeftOperand();
				
				inLeftPart = isContantWhereClause(binaryArithmeticOperatorNode.getLeftOperand(), joinAttribute) || isContantWhereClause(binaryArithmeticOperatorNode.getRightOperand(), joinAttribute);
			}
			
			if (binaryOperatorNode.getRightOperand() instanceof BinaryArithmeticOperatorNode) {
				BinaryArithmeticOperatorNode binaryArithmeticOperatorNode = (BinaryArithmeticOperatorNode) binaryOperatorNode.getRightOperand();
				
				inRightPart = isContantWhereClause(binaryArithmeticOperatorNode.getLeftOperand(), joinAttribute) || isContantWhereClause(binaryArithmeticOperatorNode.getRightOperand(), joinAttribute); 
			}
			
			return inLeftPart || inRightPart ;
			
		} else if (clause instanceof InListOperatorNode) {
			InListOperatorNode inListOperatorNode = (InListOperatorNode) clause;
			ValueNode valueNode = inListOperatorNode.getLeftOperand();
			
			if (valueNode instanceof ColumnReference) {
				boolean inRight = false;
				ValueNodeList valueNodeList = inListOperatorNode.getRightOperandList();
				
				for (int i=0;i<valueNodeList.size();i++) {
					if (valueNodeList.getNodeVector().get(i) instanceof CharConstantNode || valueNodeList.getNodeVector().get(i) instanceof NumericConstantNode) {
						if(joinAttribute.contains(valueNode.getColumnName().toString()))
							inRight = true;
					}else {
						throw new Exception("getStringWhereClause needs more programming ");
					}
				}
				
				return inRight ;
			} else {
				throw new Exception("getStringWhereClause needs more programming ");
			}
		} else if (clause instanceof NotNode) {
			NotNode notNode = (NotNode) clause;
			ValueNode valueNode = notNode.getOperand();
			return !isContantWhereClause(valueNode, joinAttribute);
		} else if (clause instanceof ColumnReference) {
			return false;
		} else if (clause instanceof IsNullNode) {
			return false;
			/*
			IsNullNode isNullNode = (IsNullNode) clause;
			if (isNullNode.getOperand() instanceof ColumnReference) {
				return isContantWhereClause(isNullNode.getOperand(),false) + " IS NULL ";
			} 
			throw new Exception("getStringWhereClause needs more programming ");
			*/
		} else if (clause instanceof SubqueryNode) {
			return false;
			/*
			SubqueryNode subqueryNode = (SubqueryNode) clause;
			String columnName = isContantWhereClause(subqueryNode.getLeftOperand(),false); 
			if (subqueryNode.getTableName() != null) {
				columnName = subqueryNode.getTableName() + "." +columnName;
			} 
			String subQueryString = getQueryString(subqueryNode.getResultSet());
			return columnName + " IN (" + subQueryString +")";
			*/
		} else if (clause instanceof LikeEscapeOperatorNode) {
			LikeEscapeOperatorNode likeEscapeOperatorNode = (LikeEscapeOperatorNode) clause;
			if (likeEscapeOperatorNode.getReceiver() instanceof ColumnReference) {
				if (likeEscapeOperatorNode.getLeftOperand() instanceof CharConstantNode || likeEscapeOperatorNode.getRightOperand() instanceof CharConstantNode ) {
					return true;
				} else {
					throw new Exception("getStringWhereClause needs more programming ");
				}
			} else {
				throw new Exception("getStringWhereClause needs more programming ");
			}
		} else if (clause instanceof NumericConstantNode) {
			return true;
		} else {
			throw new Exception("getStringWhereClause needs more programming ");
		}
	}
	
	public static void main(String[] args) {
		//String queryString = "select *   from rollhist right  outer join department on (rollhist.deptcode = department.deptcode), program where rollno = '11'";
		String queryString = "select count(studentid) from rollhist";
		SQLParser sqlParser = new SQLParser();
		try {
			ResultSetNode rsNode = ((CursorNode)sqlParser.Statement(queryString, null)).getResultSetNode();
			//System.out.println(getQueryString(rsNode));
			//System.out.println("Not removed : "+Utility.getWhereClauseString(rsNode,false));
			//System.out.println("Removed     : "+Utility.getWhereClauseString(rsNode,true));
		} catch (Exception e) {
			logger.log(Level.SEVERE,e.getMessage(),e);
			e.printStackTrace();
		}
	}
	
}
