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
