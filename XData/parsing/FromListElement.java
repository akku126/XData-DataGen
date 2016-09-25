package parsing;

import java.io.Serializable;
import java.util.Vector;

public class FromListElement implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5103941369704318519L;
	public String tableName;
	String aliasName;
	public String tableNameNo;
	Vector<FromListElement> tabs=new Vector<FromListElement>();
	
	public void addTabs(FromListElement fle){
		this.tabs.addElement(fle);
	}
	public String getTableNameNo() {
		return tableNameNo;
	}
	public void setTableNameNo(String tableNameNo) {
		this.tableNameNo = tableNameNo;
	}
	public String getAliasName() {
		return aliasName;
	}
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public Vector<FromListElement> getTabs() {
		return tabs;
	}
	public void setTabs(Vector<FromListElement> tabs) {
		this.tabs = tabs;
	}
}