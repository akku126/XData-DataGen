package util;

import parsing.Node;
import parsing.Table;

/**
 * This class stores details about each node in the join graph.
 * @author mahesh
 *
 */
public class JoinGraphNode {
	Table table;
	String tableNameNo;
	int queryType;
	int queryIndex;
	int maximumPossibleCardinality;
	int cardinality;
	
	
	/**
	 * Constructor
	 */
	/*public JoinGraphNode(Table table, String tableNameNo){
		this.table = table;
		this.tableNameNo = tableNameNo;
		this.isFromSubQuery = false;
		this.isWhereSubQuery = false;
		this.relativeCardinality = 1;
		this.actualCardinality = 1;
		this.noOfGroups = 1;
	}*/
	public JoinGraphNode(Node n){
		if(n == null){
			this.table = null;
			return;
		}
			
		this.table = n.getTable();
		this.tableNameNo = n.getTableNameNo();
		this.queryType = n.getQueryType();
		this.queryIndex = n.getQueryIndex();
		this.maximumPossibleCardinality = 100000;//FIXME: Mahesh....We have to choose infinite here...But how to define infinite
		this.cardinality = 1;
	}
	
	public Table getTable(){
		return this.table;
	}
	
	public void setTableName( Table table){
		this.table = table;
	}
	
	public String getTableNameNo(){
		return this.tableNameNo;
	}
	
	public void setTableNameNo( String tableNameNo){
		this.tableNameNo = tableNameNo;
	}
	
	
	
	public int getmaximumPossibleCardinality(){
		return this.maximumPossibleCardinality;
	}
	
	public void setmaximumPossibleCardinality(int maximumPossibleCardinality){
		this.maximumPossibleCardinality = maximumPossibleCardinality;
	}
	

	public int getCardinality(){
		return this.cardinality;
	}
	
	public void setCardinality(int cardinality){
		this.cardinality = cardinality;
	}
	

	public int getQueryType() {
		return queryType;
	}

	public void setQueryType(int queryType) {
		this.queryType = queryType;
	}

	public int getQueryIndex() {
		return queryIndex;
	}

	public void setQueryIndex(int queryIndex) {
		this.queryIndex = queryIndex;
	}
	
	@Override
	public String toString(){
		//return this.table.toString() +" " +this.tableNameNo;
		return this.tableNameNo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tableNameNo == null) ? 0 : tableNameNo.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JoinGraphNode other = (JoinGraphNode) obj;
		if (tableNameNo == null) {
			if (other.tableNameNo != null)
				return false;
		} else if (!tableNameNo.equals(other.tableNameNo))
			return false;
		return true;
	}

	
	/*@Override
	public boolean equals(Object obj) {
	    if(!(obj instanceof JoinGraphNode) && !(obj instanceof String))
	    {
	        return false;
	    }
	    else
	    {
	        if(obj instanceof JoinGraphNode)
	        	return this.toString().toLowerCase().equals(((JoinGraphNode)obj).toString().toLowerCase());
	        else
	            return this.toString().toLowerCase().equals(((String)obj).toLowerCase());
	    }

	}*/
}
