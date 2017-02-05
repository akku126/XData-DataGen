package parsing;

import java.io.Serializable;
import java.util.Vector;

public class FromClauseElement extends parsing.FromListElement implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5103941369704318519L;
	Vector<FromClauseElement> bag=new Vector<FromClauseElement>();
	//the following line added by mathew on 1st october 2016
	QueryStructure subQueryStructure;
	
	public QueryStructure getSubQueryStructure(){
		return subQueryStructure;
	}
	
	public void setSubQueryStructure(QueryStructure subQP){
		this.subQueryStructure=subQP;
	}
	
	public void addToBag(FromClauseElement fle){
		this.bag.addElement(fle);
	}

	public Vector<FromClauseElement> getBag() {
		return bag;
	}
	public void setBag(Vector<FromClauseElement> tabs) {
		this.bag = tabs;
	}
	@Override
	public String toString(){
		String retString= " aliasName: "+this.getAliasName() +
				" tableName: "+getTableName()+
				" tableNameNo: "+this.getTableNameNo();
		if(this.getSubQueryStructure()!=null)
			retString+= " subQuery ";
		return retString;
	}
}