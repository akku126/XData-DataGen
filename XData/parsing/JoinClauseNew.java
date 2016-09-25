package parsing;

import java.util.Vector;
/**
 * 
 * @author Sandeep Patidar
 * @co-author Suhas Vijay Kajbaje
 * TODO Remove this class and replace usages by JoinClauseInfo
 */
public class JoinClauseNew {
	private Vector<JoinClauseInfo> joinClauseInfoVector;
	private String joinType;
	public static final String innerJoin = "INNER JOIN";
	public static final String fullOuterJoin = "FULL OUTER JOIN";
	public static final String leftOuterJoin = "LEFT OUTER JOIN";
	public static final String rightOuterJoin = "RIGHT OUTER JOIN";
	
	public String getJoinType(){
		return joinType;
	}
	public JoinClauseNew(String joinType) {
		this.joinType = joinType;
		joinClauseInfoVector = new Vector<JoinClauseInfo>();
	}
	
	public void add(JoinClauseInfo joinClauseInfo){
		this.joinClauseInfoVector.add(joinClauseInfo);
	}
	
	public Vector<JoinClauseInfo> getJoinClauseInfoVector(){
		return this.joinClauseInfoVector;
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		for(int i=0; i<joinClauseInfoVector.size(); i++){
			if(i==0)
				s.append(joinClauseInfoVector.get(i).toString());
			else
				s.append(" and "+joinClauseInfoVector.get(i).toString());
		}
		return s.toString();
	}
}
