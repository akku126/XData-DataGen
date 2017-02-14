package parsing;

import parsing.Table;
/**
 * 
 * @author Sandeep Patidar
 * @co-author Suhas Vijay Kajbaje
 *@version: Initial version: Not written anything
 */
public class WhereClause {
	private Column joinAttributes1;
	private Column joinAttributes2;
	private Table joinTable1;
	private Table joinTable2;
	//private Table joinTable2;
	
	public static final String innerJoin = "INNER JOIN";
	public static final String fullOuterJoin = "FULL OUTER JOIN";
	public static final String leftOuterJoin = "LEFT OUTER JOIN";
	public static final String rightOuterJoin = "RIGHT OUTER JOIN";
	
	public WhereClause(String joinType) {
		
	}
	
	public Table getJoinTable1(){
		return joinTable1;
	}
	
	public Table getJoinTable2(){
		return joinTable2;
	}
	
	
	
	
	
	
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		
		s.append(")");
		return s.toString();
	
	}
}
