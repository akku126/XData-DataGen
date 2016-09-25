package parsing;

public class Aggregate {
	private Column aggregateColumn;
	private String aggregateFunction;
	private String relationOperator;
	private String constant;
	
	public Aggregate(Column aggregateColumn, String aggregateFunction){
		this.aggregateColumn = aggregateColumn;
		this.aggregateFunction = aggregateFunction;
	}
	
	public void setRelationOperator(String relationOperator){
		this.relationOperator = relationOperator;
	}

	public void setConstant(String constant){
		this.constant = constant;
	}

	public Column getAggregateColumn(){
		return this.aggregateColumn;
	}
	
	public String getAggregateFunction(){
		return this.aggregateFunction;
	}
	
	public String getRelationOperator(){
		return this.relationOperator;
	}
	
	public String getConstant(){
		return this.constant;
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append(aggregateFunction+"("+aggregateColumn.getColumnName()+")");
		return s.toString();
	
	}
}
