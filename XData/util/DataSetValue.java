package util;

import java.util.ArrayList;

/**
 * This bean class is used for storing the generated data set for diaplying in show Generated Data
 * 
 * @author shree
 *
 */
public class DataSetValue {

	/*This variable holds the TableName*/
	private String filename;
	/*This variable holds the column names in the table*/
	private ArrayList <String> columnNames = new ArrayList<String>();
	/*This variable holds the data generated for the specific column.*/
	private ArrayList <String> dataForColumn = new ArrayList<String>();
	/*This list holds list of DataSetValue for the table that contains columnName and data for column*/
	private ArrayList<DataSetValue> dsvList = new ArrayList<DataSetValue>();
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public ArrayList <String> getColumnNames() {
		return columnNames;
	}
	
	public void setColumnNames(ArrayList <String> columnNames) {
		this.columnNames = columnNames;
	}
	
	public void addColumnName(String columnName) {
		this.columnNames.add(columnName);
	} 
	/**
	 * This holds the data generated for the specific column 
	 * 
	 * @return the dataForColumn
	 */
	public ArrayList <String> getDataForColumn() {
		return dataForColumn;
	}
	/**
	 * @param dataForColumn the dataForColumn to set
	 */
	public void setDataForColumn(ArrayList <String> dataForColumn) {
		this.dataForColumn = dataForColumn;
	}
	
	/**
	 * @param dataForColumn the dataForColumn to set
	 */
	public void addData(String data) {
		this.dataForColumn.add(data);
	}
	/**
	 * @return the dsvList
	 */
	public ArrayList<DataSetValue> getDsvList() {
		return dsvList; 
	}
	/**
	 * @param dsvList the dsvList to set
	 */
	public void setDsvList(ArrayList<DataSetValue> dsvList) {
		this.dsvList = dsvList;
	}
	
	/**
	 * @param dsvList the dsvList to set
	 */ 
	public void addDataSetItem(DataSetValue dsv) {
		this.dsvList.add(dsv);
	}
	
}
