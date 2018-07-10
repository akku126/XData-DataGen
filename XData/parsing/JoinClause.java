package parsing;
/**
 * 
 * @author bhanu
 *
 *TODO Remove this class and replace by JoinClauseInfo
 */

public class JoinClause {
	/*private Vector<Column> joinAttributes1;
	private Vector<Column> joinAttributes2;
	private Table joinTable1;
	private Table joinTable2;
	private String joinType;
	public static final String innerJoin = "INNER JOIN";
	public static final String fullOuterJoin = "FULL OUTER JOIN";
	public static final String leftOuterJoin = "LEFT OUTER JOIN";
	public static final String rightOuterJoin = "RIGHT OUTER JOIN";
	
	public JoinClause(String joinType) {
		this.joinType = joinType;
		joinAttributes1 = new Vector<Column>();
		joinAttributes2 = new Vector<Column>();
	}
	
	public Table getJoinTable1(){
		return joinTable1;
	}
	
	public Table getJoinTable2(){
		return joinTable2;
	}
	
	public String getJoinType(){
		return joinType;
	}
	*/
	/*public void addJoinCondition(Column joinAttribute1, Column joinAttribute2){
		if(joinAttributes1.size()!=0){
			if(joinTable1.getTableName().equalsIgnoreCase(joinAttribute1.getTableName())){
				joinAttributes1.add(joinAttribute1);
				joinAttributes2.add(joinAttribute2);
			}else if(joinTable2.getTableName().equalsIgnoreCase(joinAttribute1.getTableName())){
				joinAttributes2.add(joinAttribute1);
				joinAttributes1.add(joinAttribute2);
			}
		}else{
			joinTable1 = joinAttribute1.getTable();
			joinAttributes1.add(joinAttribute1);
			joinTable2 = joinAttribute2.getTable();			
			joinAttributes2.add(joinAttribute2);
		}
		
	}*/
	
	/*public boolean contains(Vector<Column> columns){
		if(joinAttributes1.size()!=columns.size())
			return false;
		
		boolean flag = true;
		for(Column col: joinAttributes1){
			if(!columns.contains(col))
				flag = false;
		}
		if(flag)
			return true;
		
		flag = true;
		for(Column col: joinAttributes2){
			if(!columns.contains(col))
				flag = false;
		}
		return flag;
	}*/
	
	/*public boolean containsEitherFullOrPartialKey(Vector<Column> columns){
		boolean flag = true;
		if(joinAttributes1.size()>columns.size())
			return false;
		
		
		for(Column col: joinAttributes1){
			if(!columns.contains(col))
				flag = false;
		}
		if(flag)
			return true;
		
		flag = true;
		for(Column col: joinAttributes2){
			if(!columns.contains(col))
				flag = false;
		}
		return flag;
	}*/
	/*
	public boolean contains(String tableName){
		if(joinAttributes1.get(0).getTableName().equalsIgnoreCase(tableName) || joinAttributes2.get(0).getTableName().equalsIgnoreCase(tableName))
			return true;
		
		return false;
	}
	
	
	public Vector<Column> getOneTableJoinAttributes(String tableName){
		if(joinAttributes1.size()!=0 && joinAttributes1.get(0).getTableName().equalsIgnoreCase(tableName))
			return joinAttributes1;
		
		if(joinAttributes2.size()!=0 && joinAttributes2.get(0).getTableName().equalsIgnoreCase(tableName))
			return joinAttributes2;
		
		return null;
	}
	
	public Vector getJoinAttribute1(){
		return joinAttributes1;
	}
	
	public Vector getJoinAttribute2(){
		return joinAttributes2;
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		for(int i=0; i<joinAttributes2.size(); i++){
			Column col1 = joinAttributes1.get(i);
			Column col2 = joinAttributes2.get(i);
			s.append("(");
			if(i<joinAttributes2.size()-1){
				s.append(col1.getTableName()+"."+col1.getColumnName()+"=");
				s.append(col2.getTableName()+"."+col2.getColumnName()+") and ");
			}
			else{
				s.append(col1.getTableName()+"."+col1.getColumnName()+"=");
				s.append(col2.getTableName()+"."+col2.getColumnName());
			}
				
		}
		s.append(")");
		return s.toString();
	
	}
	*/
}
