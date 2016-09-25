package parsing;

import java.io.Serializable;
import java.util.Vector;

public class AggregateClause implements Serializable{

	
	private static final long serialVersionUID = 7302495259682507671L;
	private Vector<Aggregate> selectClause;
	private Vector<Aggregate> havingClause;
	
	public AggregateClause(){
		selectClause = new Vector<Aggregate>();
		havingClause = new Vector<Aggregate>();
	}
	
	public void addSelectClause(Aggregate aggr){
		this.selectClause.add(aggr);
	}
	
	public Vector<Aggregate> getSelectClause(){
		return this.selectClause;
	}
	
	public void addHavingClause(Aggregate aggr){
		this.havingClause.add(aggr);
	}
	
	public Vector<Aggregate> getHavingClause(){
		return this.havingClause;
	}
	
	public static void main(String args[]){
		
	}
}
