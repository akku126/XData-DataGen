package parsing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;


//Application Testing -- class to store all the parameters and constraints passed from DBridge
public class AppTest_Parameters implements Serializable {
	private static Logger logger = Logger.getLogger(AppTest_Parameters.class.getName());
	private  String dbridge_Constraints = "";
	private  ArrayList<String> dbridge_Param_Constraints = new ArrayList<String>();
	private  ArrayList<String> resultSetIndexArray = new ArrayList<String>();
	private  ArrayList<String> xdata_resultset_constraints = new ArrayList<String>();
	private  HashMap<String,String> dbridge_param_sel_map = new HashMap<String,String>();
	private  HashMap<String,String> parameters_Datatype = new HashMap<String,String>();
	private  HashMap<String,String> parameters_Datatype_Copy = new HashMap<String,String>();
	private  boolean schemasetFlag ;
	private  Vector<Node> schemaProjectedColumns ;
	private  boolean tupleCountFlag ;
	private  HashMap<String, Integer> tablestuplecount = new HashMap<String, Integer>();
	private  boolean xdataSchemasetFlag=false ;
	
	
	public String getDbridge_Constraints() {
		return dbridge_Constraints;
	}
	public void setDbridge_Constraints(String dbridge_Constraints) {
		this.dbridge_Constraints = dbridge_Constraints;
	}
	public ArrayList<String> getDbridge_Param_Constraints() {
		return dbridge_Param_Constraints;
	}
	public void setDbridge_Param_Constraints(
			ArrayList<String> dbridge_Param_Constraints) {
		this.dbridge_Param_Constraints = dbridge_Param_Constraints;
	}
	public ArrayList<String> getResultSetIndexArray() {
		return resultSetIndexArray;
	}
	public void setResultSetIndexArray(ArrayList<String> resultSetIndexArray) {
		this.resultSetIndexArray = resultSetIndexArray;
	}
	public ArrayList<String> getXdata_resultset_constraints() {
		return xdata_resultset_constraints;
	}
	public void setXdata_resultset_constraints(
			ArrayList<String> xdata_resultset_constraints) {
		this.xdata_resultset_constraints = xdata_resultset_constraints;
	}
	public HashMap<String, String> getDbridge_param_sel_map() {
		return dbridge_param_sel_map;
	}
	public void setDbridge_param_sel_map(
			HashMap<String, String> dbridge_param_sel_map) {
		this.dbridge_param_sel_map = dbridge_param_sel_map;
	}
	public HashMap<String, String> getParameters_Datatype() {
		return parameters_Datatype;
	}
	public void setParameters_Datatype(HashMap<String, String> parameters_Datatype) {
		this.parameters_Datatype = parameters_Datatype;
	}
	public HashMap<String, String> getParameters_Datatype_Copy() {
		return parameters_Datatype_Copy;
	}
	public void setParameters_Datatype_Copy(
			HashMap<String, String> parameters_Datatype_Copy) {
		this.parameters_Datatype_Copy = parameters_Datatype_Copy;
	}
	public boolean isSchemasetFlag() {
		return schemasetFlag;
	}
	public void setSchemasetFlag(boolean schemasetFlag) {
		this.schemasetFlag = schemasetFlag;
	}
	public Vector<Node> getSchemaProjectedColumns() {
		return schemaProjectedColumns;
	}
	public void setSchemaProjectedColumns(Vector<Node> schemaProjectedColumns) {
		this.schemaProjectedColumns = schemaProjectedColumns;
	}
	public boolean isTupleCountFlag() {
		return tupleCountFlag;
	}
	public void setTupleCountFlag(boolean tupleCountFlag) {
		this.tupleCountFlag = tupleCountFlag;
	}
	public HashMap<String, Integer> getTablestuplecount() {
		return tablestuplecount;
	}
	public void setTablestuplecount(HashMap<String, Integer> tablestuplecount) {
		this.tablestuplecount = tablestuplecount;
	}
	public boolean isXdataSchemasetFlag() {
		return xdataSchemasetFlag;
	}
	public void setXdataSchemasetFlag(boolean xdataSchemasetFlag) {
		this.xdataSchemasetFlag = xdataSchemasetFlag;
	}
	
}
