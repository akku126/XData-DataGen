package util;

import java.util.ArrayList;

public class FailedColumnValues {

	private String columnName = "";
	private ArrayList<String> values = new ArrayList<String>();
	
	private String instrColumnName="";
	private ArrayList<String> instrValues = new ArrayList<String>();
	
	
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public ArrayList<String> getValues() {
		return values;
	}
	public void setValues(ArrayList<String> values) {
		this.values = values;
	}
	/**
	 * @return the instrColumnName
	 */
	public String getInstrColumnName() {
		return instrColumnName;
	}
	/**
	 * @param instrColumnName the instrColumnName to set
	 */
	public void setInstrColumnName(String instrColumnName) {
		this.instrColumnName = instrColumnName;
	}
	/**
	 * @return the instrValues
	 */
	public ArrayList<String> getInstrValues() {
		return instrValues;
	}
	/**
	 * @param instrValues the instrValues to set
	 */
	public void setInstrValues(ArrayList<String> instrValues) {
		this.instrValues = instrValues;
	}
	
	
}
