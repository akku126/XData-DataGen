package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used when the student query is evaluated against Instructor
 * query and failed datasets are generated in case if student query fails.
 * 
 * @author shree
 *
 */
public class FailedDataSetValues {

	private String status = "passed";
	private float Marks = 0.0f;
	private String ErrorMessage = "";
	//This query id is used for displaying failed datasets when query equivalence is tested in TESTER mode
	private int queryId = 0;
	//If output for one specific dataset is to be stored using this bean, use this variable
	private String dataSetId;
	//Tag of failed dataset id
	private String dataSetName;
	
	private ArrayList<String> dataSetIdList = new ArrayList<String>();
	//This map stores columnName and failed DS values - incorrect output's - column name and its values
	private Map<String,ArrayList<FailedColumnValues>> dsValueMap = new HashMap<String,ArrayList<FailedColumnValues>>();
	
	//This map stores columnName and failed DS values - incorrect output's - column name and its values
	//	private Map<String,ArrayList<FailedColumnValues>> defaultDSMap = new HashMap<String,ArrayList<FailedColumnValues>>();
		
		
	//Holds dataSetId as key and the inner map holds ColumnName as key and output for that column as list of String values for student query
	private Map<String,Map<String,ArrayList<String>>> studentQueryOutput = new HashMap<String,Map<String,ArrayList<String>>>();
	//Holds dataSetId as key and the inner map holds ColumnName as key and output for that column as list of String values for instructor query
	private Map<String,Map<String,ArrayList<String>>> instrQueryOutput = new HashMap<String,Map<String,ArrayList<String>>>();
	
	//This holds the failed default data set Id's [sample data ids of sample data loaded by instructor]
	private String[] failedDefaultDataSets = new String[25];
	//Holds dataSetId as key and the inner map holds ColumnName as key and output for that column as list of String values for student query
	private Map<String,Map<String,ArrayList<String>>> studentQueryDefaultDSOutput = new HashMap<String,Map<String,ArrayList<String>>>();
	//Holds dataSetId as key and the inner map holds ColumnName as key and output for that column as list of String values for instructor query
	private Map<String,Map<String,ArrayList<String>>> instrQueryDefaultDSOutput = new HashMap<String,Map<String,ArrayList<String>>>();
	
	private String instrQuery = "";
	//public void setDsValueMap(Map<String, ArrayList<FailedColumnValues>> dsValueMap) {
	//	this.dsValueMap = dsValueMap;
	//}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public ArrayList<String> getDataSetIdList() {
		return dataSetIdList;
	}
	public void setDataSetIdList(ArrayList<String> dataSetIdList) {
		this.dataSetIdList = dataSetIdList;
	}
	//public HashMap<String, ArrayList<String>> getColValueMap() {
	//	return colValueMap;
	//}
	//public void setColValueMap(HashMap<String, ArrayList<String>> colValueMap) {
	//	this.colValueMap = colValueMap;
	//}
	public Map<String,ArrayList<FailedColumnValues>> getDsValueMap() {
		return dsValueMap;
	}
	public void setDsValueMap(
			Map<String,ArrayList<FailedColumnValues>> dsValueMap) {
		this.dsValueMap = dsValueMap;
	}
	/**
	 * @return the studentQueryOutput
	 */
	public Map<String, Map<String, ArrayList<String>>> getStudentQueryOutput() {
		return studentQueryOutput;
	}
	/**
	 * @param studentQueryOutput the studentQueryOutput to set
	 */
	public void setStudentQueryOutput(Map<String, Map<String, ArrayList<String>>> studentQueryOutput) {
		this.studentQueryOutput = studentQueryOutput;
	}
	/**
	 * @return the instrQueryOutput
	 */
	public Map<String, Map<String, ArrayList<String>>> getInstrQueryOutput() {
		return instrQueryOutput;
	}
	/**
	 * @param instrQueryOutput the instrQueryOutput to set
	 */
	public void setInstrQueryOutput(Map<String, Map<String, ArrayList<String>>> instrQueryOutput) {
		this.instrQueryOutput = instrQueryOutput;
	}
	/**
	 * @return the instrQuery
	 */
	public String getInstrQuery() {
		return instrQuery;
	}
	/**
	 * @param instrQuery the instrQuery to set
	 */
	public void setInstrQuery(String instrQuery) {
		this.instrQuery = instrQuery;
	}
	/**
	 * @return the failedDefaultDataSets
	 */
	public String[] getFailedDefaultDataSets() {
		return failedDefaultDataSets;
	}
	/**
	 * @param failedDefaultDataSets the failedDefaultDataSets to set
	 */
	public void setFailedDefaultDataSets(String[] failedDefaultDataSets) {
		this.failedDefaultDataSets = failedDefaultDataSets;
	}
	/**
	 * @return the studentQueryDefaultDSOutput
	 */
	public Map<String,Map<String,ArrayList<String>>> getStudentQueryDefaultDSOutput() {
		return studentQueryDefaultDSOutput;
	}
	/**
	 * @param studentQueryDefaultDSOutput the studentQueryDefaultDSOutput to set
	 */
	public void setStudentQueryDefaultDSOutput(
			Map<String,Map<String,ArrayList<String>>> studentQueryDefaultDSOutput) {
		this.studentQueryDefaultDSOutput = studentQueryDefaultDSOutput;
	}
	/**
	 * @return the instrQueryDefaultDSOutput
	 */
	public Map<String,Map<String,ArrayList<String>>> getInstrQueryDefaultDSOutput() {
		return instrQueryDefaultDSOutput;
	}
	/**
	 * @param instrQueryDefaultDSOutput the instrQueryDefaultDSOutput to set
	 */
	public void setInstrQueryDefaultDSOutput(
			Map<String,Map<String,ArrayList<String>>> instrQueryDefaultDSOutput) {
		this.instrQueryDefaultDSOutput = instrQueryDefaultDSOutput;
	}
	/**
	 * @return the defaultDSMap
	 */
	/*public Map<String,ArrayList<FailedColumnValues>> getDefaultDSMap() {
		return defaultDSMap;
	}
	/**
	 * @param defaultDSMap the defaultDSMap to set
	 */
	/*public void setDefaultDSMap(Map<String,ArrayList<FailedColumnValues>> defaultDSMap) {
		this.defaultDSMap = defaultDSMap;
	}*/
	/**
	 * @return the query_id
	 */
	public int getQuery_id() {
		return queryId;
	}
	/**
	 * @param query_id the query_id to set
	 */
	public void setQuery_id(int queryId) {
		this.queryId = queryId;
	}
	/**
	 * @return the dataSetId
	 */
	public String getDataSetId() {
		return dataSetId;
	}
	/**
	 * @param dataSetId the dataSetId to set
	 */
	public void setDataSetId(String dataSetId) {
		this.dataSetId = dataSetId;
	}
	public String getDataSetName() {
		return dataSetName;
	}
	public void setDataSetName(String dataSetName) {
		this.dataSetName = dataSetName;
	}
	public String getErrorMessage() {
		return ErrorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		ErrorMessage = errorMessage;
	}
	public float getMarks() {
		return Marks;
	}
	public void setMarks(float marks) {
		Marks = marks;
	}
	
}
