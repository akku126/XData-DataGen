package parsing;

import java.io.Serializable;

import parsing.Table;

/**
 * 
 * @author Sandeep Patidar
 * @co-author Suhas Vijay Kajbaje
 *	This class contains the a.x=b.y type statements in join clause or from where clause.
 *	RHS can be constant when it is where clause.
 */
public class JoinClauseInfo implements Serializable{
	
	private static final long serialVersionUID = 4984911924364475017L;
	private Column joinAttributes1;
	private Column joinAttributes2;
	private Table joinTable1;
	private Table joinTable2;
	private String constant;
	private String type;
	private String binaryOperator;
	public static final String joinType = "JOIN TYPE";
	public static final String equiJoinType = "EQUI JOIN TYPE";
	public static final String nonEquiJoinTpe = "NON EQUI JOIN TYPE";
	public static final String selectionType = "CONSTANT";
	public static final String FKType = "FOREIGN KEY";
	public static final String innerJoin = "INNER JOIN";
	public static final String fullOuterJoin = "FULL OUTER JOIN";
	public static final String leftOuterJoin = "LEFT OUTER JOIN";
	public static final String rightOuterJoin = "RIGHT OUTER JOIN";

	//private TreeNode Node1;
	//private TreeNode Node2;

	public JoinClauseInfo(Column col1, Column col2, String type) {
		this.joinAttributes1 = col1;
		this.joinTable1 = col1.getTable();
		this.joinAttributes2 = col2;
		this.joinTable2 = col2.getTable();
		this.type = type;
	}
	
	public JoinClauseInfo(Column col1, String constant, String binaryOperator, String type) {
		this.joinAttributes1 = col1;
		this.joinTable1 = col1.getTable();
		this.constant = constant;
		this.binaryOperator = binaryOperator;
		this.type = type;
	}
	
	public boolean contains(String tableName){
		if(joinAttributes1.getTableName().equalsIgnoreCase(tableName) || joinAttributes2.getTableName().equalsIgnoreCase(tableName))
			return true;
		
		return false;
	}
	
	public Table getJoinTable1(){
		return joinTable1;
	}
	
	public Table getJoinTable2(){
		return joinTable2;
	}
	
	public Column getJoinAttribute1(){
		return joinAttributes1;
	}
	
	public Column getJoinAttribute2(){
		return joinAttributes2;
	}
	
	public String getConstant(){
		return constant;
	}
	public void setConstant(String constant){
		this.constant=constant;
	}
	
	public String getBinaryOperator(){
		return binaryOperator;
	}
	//Added by bhupesh
	public String getType(){
		return type;
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("(");
		s.append(joinAttributes1.getTableName()+"."+joinAttributes1.getColumnName()+"=");
		if(type.equals(joinType) || type.equals(FKType))
			s.append(joinAttributes2.getTableName()+"."+joinAttributes2.getColumnName());
		else if(type.equals(selectionType))
			s.append(constant);
			
		s.append(")");
		return s.toString();
	
	}
	
}
