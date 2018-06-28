package testDataGen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import parsing.Node;
import parsing.AppTest_Parameters;
import util.Configuration;
import util.TableMap;

public class Generatedataset_DBApps {
	//Application Testing
	public void generateDatasetForQueryPlusConstraints(String query, String constraints, ArrayList<String> paramconstraints,ArrayList<String>  resultSetIndexArray,ArrayList<String> xdata_resultset_constraints,HashMap<String,String>param_sel_map,HashMap<String,String> params_Datatype,String datasetcounter) throws Exception{
		
		AppTest_Parameters obj = new AppTest_Parameters();
		obj.setDbridge_Constraints(constraints);
		obj.setDbridge_Param_Constraints(paramconstraints);
		obj.setXdata_resultset_constraints(xdata_resultset_constraints);
		obj.setResultSetIndexArray(resultSetIndexArray);
		obj.setDbridge_param_sel_map(param_sel_map);
		obj.setParameters_Datatype(params_Datatype);
		obj.setParameters_Datatype_Copy(params_Datatype);		
		Class.forName("org.postgresql.Driver");
		
		String loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":" + Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");

		Connection conn=DriverManager.getConnection(loginUrl, Configuration.getProperty("existingDatabaseUser"), Configuration.getProperty("existingDatabaseUser"));;
		int queryId=1;
		//String query="select id,name from student";
		//String query="select * from student where id=?";
		File schemaFile=new File("/home/neha/tmp/DDL.sql");
		File sampleDataFile=new File("/home/neha/tmp/data.sql");
		boolean orderDependent=false;
		
		String tempFilePath=datasetcounter;
		
		//GenerateDataset d=new GenerateDataset();
		//call for each path in loops
		//estimateTupleCountForLoopsQueries(conn,queryId,query,  schemaFile,  sampleDataFile,  orderDependent,  tempFilePath);		
		GenerateDataSet d = new GenerateDataSet();
		d.generateDatasetForQuery(conn,queryId,query,  schemaFile,  sampleDataFile,  orderDependent,  tempFilePath,obj);
		conn.close();
	}
	
	

	public  Vector<Node> generateSchemaForQuery(String query) throws Exception{
		Class.forName("org.postgresql.Driver");
		
		String loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":" + Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");

		Connection conn=DriverManager.getConnection(loginUrl, Configuration.getProperty("existingDatabaseUser"), Configuration.getProperty("existingDatabaseUser"));;
		int queryId=1;			
		File schemaFile=new File("/home/neha/tmp/DDL.sql");
		File sampleDataFile=new File("/home/neha/tmp/data.sql");
		boolean orderDependent=false;
		String tempFilePath="4";
		
		Generatedataset_DBApps d=new Generatedataset_DBApps();
		AppTest_Parameters obj = new AppTest_Parameters();
		obj.setSchemasetFlag(true);
		
		Vector<Node> schemaOutput = null;
		schemaOutput = d.generateSchemaForGivenQuery(conn,queryId,query,  schemaFile,  sampleDataFile,  orderDependent,  tempFilePath, obj);
		conn.close();
		obj.setSchemasetFlag(false);

		return schemaOutput;
	}
	
	public Vector<Node> generateSchemaForGivenQuery(Connection conn,int queryId,String query, File schemaFile, File sampleDataFile, boolean orderDependent, String tempFilePath, AppTest_Parameters obj) throws Exception{
		String line,schema="",sampleData="";
		
		
		BufferedReader br=new BufferedReader(new FileReader(schemaFile));
		while((line=br.readLine())!=null) {
			schema+=line+"\n";
		}
		br.close();
		
		br=new BufferedReader(new FileReader(sampleDataFile));
		while((line=br.readLine())!=null) {
			sampleData+=line+"\n";
		}
		br.close();
		
		return generateSchemaForGivenQuery(conn, queryId, query, schema, sampleData, orderDependent, tempFilePath, obj);
	}
	
	/**
	 * This function generates test datasets for a query
	 * @param conn Database connection to use
	 * @param queryId The query id
	 * @param query The query for which dataset needs to be generated
	 * @param schema The schema against which the query has been written
	 * @param sampleData Sample data to generate realistic values
	 * @param orderDependent Whether the order of tuples in the result matter. Set this to true for queries that have ORDER BY
	 * @param tempFilePath File path to create temporary files and datasets
	 * @return List of dataset ids that have been generated
	 * @throws Exception
	 */
	public Vector<Node> generateSchemaForGivenQuery(Connection conn,int queryId,String query, String schema, String sampleData, boolean orderDependent, String tempFilePath, AppTest_Parameters obj) throws Exception{
		
		if(tempFilePath==null | tempFilePath.equals("")){
			tempFilePath="/tmp/"+queryId;
		}
		
		GenerateCVC1 cvc=new GenerateCVC1();
		cvc.setFilePath(tempFilePath);
		cvc.setFne(false); 
		cvc.setIpdb(false);
		cvc.setOrderindependent(orderDependent);	
		GenerateDataSet gd = new GenerateDataSet();
		
		gd.loadSchema(conn,schema);
		gd.loadSampleData(conn,sampleData);
		
		cvc.setSchemaFile(schema);
		cvc.setDataFileName(sampleData);
		
		TableMap.clearAllInstances();
		cvc.setTableMap(TableMap.getInstances(conn, 1));
		cvc.setConnection(conn);
		
		GenerateDataSet.deletePreviousDatasets(cvc);
		//Application Testing
		cvc.setDBAppparams(obj);
		//end
		FileWriter fw=new FileWriter(Configuration.homeDir+"/temp_cvc" +cvc.getFilePath()+"/queries.txt");
		fw.write(query);
		fw.close();
		
		return PreProcessingActivity.preProcessingActivityForSchema(cvc);
		
		
		
		
	}

/*
 * entry point for XDataPro
 */

	public void generateTestData(String xdataquery, String xdataConstraints,
			ArrayList<String> paramConstraints,
			HashMap<String, String> param_sel_map,
			HashMap<String, String> paramDatatype, String datasetcounter) {
		
		ArrayList<String>  resultSetIndexArray = new ArrayList<String>();
		ArrayList<String> xdata_resultset_constraints = new ArrayList<String>();
		try {
			generateDatasetForQueryPlusConstraints(xdataquery,xdataConstraints,paramConstraints,resultSetIndexArray,xdata_resultset_constraints,param_sel_map,paramDatatype,datasetcounter);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

}
