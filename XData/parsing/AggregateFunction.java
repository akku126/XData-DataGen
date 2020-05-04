package parsing;

import java.util.HashMap;
import java.util.Vector;

public class AggregateFunction implements java.io.Serializable{

	
	private static final long serialVersionUID = 7929469164175666861L;
	private static String aggSUM = "SUM";
	private static String aggAVG = "AVG"; 
	private static String aggMIN = "MIN"; 
	private static String aggMAX = "MAX"; 
	private static String aggCOUNT = "COUNT"; 
	
	Node aggExp;
	Column aggCol;
	boolean isDistinct;
	String func;
	String aggAliasName;

	/**
	 * Constructor. Added by Mahesh
	 */
	public AggregateFunction(){
		this.aggExp = null;
	}
	
	/**
	 * Copy constructor. Added by Mahesh
	 * @param functionName
	 * @return
	 */
	public AggregateFunction(AggregateFunction af){
		if(af == null)
			return ;
		this.aggExp = new Node(af.getAggExp());
		this.aggCol = new Column(af.getAggCol());
		this.isDistinct = af.isDistinct();
		this.func = af.getFunc();
		this.aggAliasName = af.getAggAliasName();
	}
	
	@Override
	public String toString(){
		if(aggExp!=null)
			return(this.func+"("+this.aggExp.toString()+")");
		return "";
	}
	
	public String getFunc() {
		return func;
	}

	public void setFunc(String func) {
		this.func = func;
	}

	public static String getAggAVG() {
		return aggAVG;
	}

	public static String getAggCOUNT() {
		return aggCOUNT;
	}

	public static String getAggMAX() {
		return aggMAX;
	}

	public static String getAggMIN() {
		return aggMIN;
	}

	public static String getAggSUM() {
		return aggSUM;
	}

	public boolean isDistinct() {
		return isDistinct;
	}

	public void setDistinct(boolean isDistinct) {
		this.isDistinct = isDistinct;
	}

	public Column getAggCol() {
		return aggCol;
	}

	public void setAggCol(Column aggCol) {
		this.aggCol = aggCol;
	}
	
	public Node getAggExp() {
		return aggExp;
	}

	public void setAggExp(Node aggExp) {
		this.aggExp = aggExp;
	}
	
	public void setAggAliasName(String name){
		this.aggAliasName=name;
	}
	
	public String getAggAliasName(){
		return aggAliasName;
	}
	
	public int getNoOfOutputTuples(HashMap<String, Integer> noOfTuplesMap){
		Node n = this.getAggExp();
		Vector<Column> cols = n.getColumnsFromNode();
		return noOfTuplesMap.get(cols.get(0).getTableName());
	}
	
	
	@Override
	public boolean equals(Object obj) {
	    if(!(obj instanceof AggregateFunction) && !(obj instanceof String))
	    {
	        return false;
	    }
	    else
	    {
	        if(obj instanceof AggregateFunction)
	            /*return func.toLowerCase().equals(((AggregateFunction)obj).getFunc().toLowerCase()) && aggExp.getColumn().toString().toLowerCase().equals(((AggregateFunction)obj).getAggExp().getColumn().toString().toLowerCase()) 
	            		&& aggExp.getColumn().getTableName().toString().toLowerCase().equals(((AggregateFunction)obj).getAggExp().getColumn().getTableName().toString().toLowerCase()) ;*/
	        	return this.toString().toLowerCase().equals(((AggregateFunction)obj).toString().toLowerCase());
	        else
	            return this.toString().toLowerCase().equals(((String)obj).toLowerCase());
	    }

	}

	
	
}
