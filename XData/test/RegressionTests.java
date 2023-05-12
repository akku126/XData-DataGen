package test;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.util.*;
import dnl.utils.text.table.TextTable;



import parsing.Table;
import testDataGen.GenerateDataSet;
import testDataGen.PopulateTestData;
import util.Configuration;
import util.TableMap;
import util.Utilities;
public class RegressionTests {

	String basePath;
	String schema;
	String sampleData;

	public RegressionTests(String basePath, String schemaFile, String sampleDataFile) {
		super();
		this.basePath = basePath;
		this.schema = Utilities.readFile(new File(schemaFile));
		this.sampleData = Utilities.readFile(new File(sampleDataFile));
	}

	private Connection getTestConn() throws Exception{
		
		//added by rambabu
		String tempDatabaseType = Configuration.getProperty("tempDatabaseType");
		String loginUrl = "";
		Connection conn = null;
		
		//choosing connection based on database type 
		if(tempDatabaseType.equalsIgnoreCase("postgresql"))
		{
			Class.forName("org.postgresql.Driver");
			
			loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":" + Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");
			conn = DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"), Configuration.getProperty("testDatabaseUserPasswd"));;
		}
		else if(tempDatabaseType.equalsIgnoreCase("mysql"))
		{
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			loginUrl = "jdbc:mysql://" + Configuration.getProperty("databaseIP") + ":" + Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");
			conn=DriverManager.getConnection(loginUrl, Configuration.getProperty("testDatabaseUser"), Configuration.getProperty("testDatabaseUserPasswd"));;				
		}		
		return conn;
	}

	/**
	 * Load queries from the queries.txt file
	 * @return Map of queryId,query
	 * @throws IOException
	 */
	private Map<Integer,String> getQueries()	throws IOException {
		Map<Integer,String> queryMap=new HashMap<Integer,String>();
		
		String fullPath=basePath+File.separator+"queries.txt";
		
		FileReader fileReader = new FileReader(new File(fullPath));
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			if(line.trim().startsWith("--"))
				continue;
			//The queries file contains entries for queries in the format id|query
			String[] lineArr=line.split("\\|", 2);
			if(lineArr.length<2)
				continue;
			if(lineArr[1]==null || lineArr[1].trim().equals(""))
				continue;
			Integer queryId=0;
			try {
				queryId=Integer.parseInt(lineArr[0].trim());
			} catch(NumberFormatException nfe) {
				continue;
			}
			queryMap.put(queryId, lineArr[1]);
		}
		fileReader.close();
		
		
		return queryMap;
	}
	
	/**
	 * Gets mutants from the mutants.txt file
	 * @return map of queryId, list of mutants
	 * @throws IOException
	 */
	private Map<Integer,List<String>> getMutants() throws IOException	{
		Map<Integer,List<String>> mutantMap=new HashMap<Integer,List<String>>();
		
		String fullPath=basePath+File.separator+"mutants.txt";
		
		FileReader fileReader = new FileReader(new File(fullPath));
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			if(line.trim().startsWith("--"))
				continue;
			//The mutants file contains entries for queries in the format id|query. The id should match the query
			String[] lineArr=line.split("\\|", 2);
			if(lineArr.length<2)
				continue;
			if(lineArr[1]==null || lineArr[1].trim().equals(""))
				continue;
			Integer queryId=0;
			try {
				queryId=Integer.parseInt(lineArr[0].trim());
			} catch(NumberFormatException nfe) {
				continue;
			}
			List<String> mutantList= mutantMap.get(queryId);
			if(mutantList==null)	{
				mutantList=new ArrayList<String>();
			}
			mutantList.add(lineArr[1]);
			mutantMap.put(queryId, mutantList);
		}
		fileReader.close();
		
		
		return mutantMap;
	}
	
	
	private List<String> generateDataSets(Integer queryId, String query)	{
		
		try(Connection conn =getTestConn()){

			boolean orderDependent=false;
			String tempFilePath=File.separator+queryId;
			GenerateDataSet d=new GenerateDataSet();
			List<String> datasets=d.generateDatasetForQuery(conn,queryId,query,  schema,  sampleData,  orderDependent,  tempFilePath, null);
			return datasets;
		} catch(Exception e) {
			e.printStackTrace();
		}

		return null;
		
	}
	
	/**
	 * Tests if the basic dataset produces a non-empty result
	 * @param queryId queryId of the dataset
	 * @param query query
	 * @return
	 */
	private boolean testBasicDataset(Integer queryId, String query)	{
		
		try(Connection testConn=getTestConn()){
			String filePath=queryId+"";
			
			PopulateTestData.deleteAllTablesFromTestUser(testConn);
			GenerateDataSet.loadSchema(testConn, schema);
			GenerateDataSet.loadSampleData(testConn, sampleData);
			
			TableMap tableMap=TableMap.getInstances(testConn, 1);
			//System.out.println("Testing BASIC Dataset >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			PopulateTestData.loadCopyFileToDataBase(testConn, "DS0", filePath, tableMap);
			//PopulateTestData.loadSQLFilesToDataBase(testConn, "DS0.sql", filePath);
			
			PreparedStatement ptsmt=testConn.prepareStatement(query);
			ResultSet rs=ptsmt.executeQuery();
			if(rs.next()) {
				return true;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean testMutantKilling(Integer queryId, List<String> datasets, String query, String mutant) {
		
		for(String datasetId:datasets) {
			
			try(Connection testConn=getTestConn()){
				String filePath=queryId+"";
				

				GenerateDataSet.loadSchema(testConn, schema);
				GenerateDataSet.loadSampleData(testConn, sampleData);

				TableMap tableMap=TableMap.getInstances(testConn, 1);
				//System.out.println("MUTANT TESTING: dataset id "+datasetId+" >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				PopulateTestData.loadCopyFileToDataBase(testConn, datasetId, filePath, tableMap);
				//PopulateTestData.loadSQLFilesToDataBase(testConn, datasetId+".sql", filePath);
				
				
				//String testQuery= "with q as ("+query+") , m as("+mutant+") (select * from q EXCEPT ALL m) UNION ALL (select * from m EXCEPT ALL q)";
				
				String testQuery="(("+query+") EXCEPT ALL ("+mutant+")) UNION (("+mutant+") EXCEPT ALL ("+query+"))";
				
				
				PreparedStatement ptsmt=testConn.prepareStatement(testQuery);
				ResultSet rs=ptsmt.executeQuery();
				
				//added by Akanksha
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnsNumber = rsmd.getColumnCount();
				//Added by Akanksha ends
				
				if(rs.next()) {
					//Added by Akanksha
					System.out.println("");
					System.out.println(mutant+" "+"Failed on following testcase");
					
					//Added by Akku
					
					for(int f=0;f<tableMap.foreignKeyGraph.topSort().size();f++){
						String tableName = tableMap.foreignKeyGraph.topSort().get(f).toString();
						String selectQuery = "SELECT * FROM " + tableName;

				        try (PreparedStatement stmt = testConn.prepareStatement(selectQuery)) {
				            try (ResultSet rs1 = stmt.executeQuery()) {
				            	ResultSetMetaData rsmd1 = rs1.getMetaData();
				                int columnCount = rsmd1.getColumnCount();
				                String colnames[]=new String[columnCount];
				                for (int i = 0; i < columnCount; i++) { 
			                        String columnName = rsmd1.getColumnName(i+1);
			                        colnames[i]=columnName;
			                      
				                }
                               String colvalues[][] =new String[0][columnCount];
				                while (rs1.next()) {
				                	String rowadd[]=new String[columnCount];
				                    for (int i = 0; i < columnCount; i++) {
				                       
				                        String columnValue = rs1.getString(i+1);
				                        rowadd[i]=columnValue;
				                       
				                    }
				                    colvalues = Arrays.copyOf( colvalues,  colvalues.length + 1); // increase the array size by 1
				                    colvalues[ colvalues.length - 1] = rowadd;
				                   
				                }
				             
				                TextTable tt = new TextTable(colnames, colvalues);
				                
				        		tt.printTable();
				               
				            }
				        }
				    }
					//Added by Akku ends
					System.out.println("Result \n");
					
					for (int i = 1; i <= columnsNumber; i++) {
					    System.out.print(rsmd.getColumnName(i) + " | ");
					}
					System.out.println();
					
					do {
					    for (int i = 1; i <= columnsNumber; i++) {
					        System.out.print(rs.getString(i) + " | ");
					    }
					    System.out.println();
					}while (rs.next());
					//Added by Akanksha end's,changed below return value to false.
					return false;
				}
			}catch(SQLException e) {
				//Added by Akanksha
				System.out.println("got exception->");
				e.printStackTrace();
				
				//Added by Akanksha ends,changed below return value to false.
				return false;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public Map<Integer,List<String>> runRegressionTests() {
		
		Map<Integer,String> queryMap;
		Map<Integer,List<String>> mutantMap;
		
		Map<Integer,List<String>> testResult=new LinkedHashMap<Integer,List<String>>();
		
		try {	
			queryMap = getQueries();
			mutantMap = getMutants();
		}catch(Exception e) {
			System.out.println("Error reading queries or mutants");
			e.printStackTrace();
			return null;
		}
		
		for(Integer queryId:queryMap.keySet()) {
			List<String> errors=new ArrayList<String>();
			
			String query=queryMap.get(queryId);
			System.out.println(query);
			List<String> datasets;
			//Generate datasets
			datasets=generateDataSets(queryId,query);
			
			if(datasets==null || datasets.isEmpty()) {
				System.out.println("************************Empty dataset");
				errors.add("Exception in generating datasets");
				testResult.put(queryId, errors);
				continue;
			}
				
			//Check if DS0 works
			try {
				if(testBasicDataset(queryId,query)==false) {
					errors.add("Basic datasets failed");
					System.out.println("BASIC dataset failed: "+queryId);
				}
					
			} catch (Exception e)	{
				e.printStackTrace();
				errors.add("Exception in running query on basic test case");
				testResult.put(queryId, errors);
				System.out.println("EXCEPTION: (query: "+queryId+" ) Exception in running query on basic test case");
				continue;
			}
			
			
			//Check mutation killing
			for(String mutant:mutantMap.get(queryId))	{
				try {
					if(testMutantKilling(queryId, datasets, query, mutant)==false) {
						errors.add(mutant);
						//Below LINE COMMENTED BY akanksha
						//System.out.println(" FAILED FOR MUTANT (query: "+queryId+" )"+mutant);
					}
						
				}catch (Exception e)	{
					e.printStackTrace();
					errors.add("Exception in killing mutant query:"+mutant);
					testResult.put(queryId, errors);
				}
			}
			
			if(!errors.isEmpty())
				testResult.put(queryId, errors);
			
			//added by rambabu for testing
			//Below line commented by Akanksha
			//System.out.println("query id done: "+ queryId);
			
		}
		
		return testResult;
	}
	
	public static void main(String[] args)	throws Exception{
		
		String basePath="test/universityTest";
		//Path of file containing schema
		String schemaFile="test/universityTest/DDL.sql";
		//Path of file containing sampleData
		String sampleDataFile="test/universityTest/sampleData.sql";
		/* runtime analysis for regression test */
		long startTime = System.currentTimeMillis();
		//System.out.println("Starting time of regression test is:");
        //System.out.println(startTime);
		RegressionTests r=new RegressionTests(basePath,schemaFile,sampleDataFile);
		Map<Integer,List<String>> errorsMap=r.runRegressionTests();
		
		String errors=""; 
		if(errorsMap==null)
			System.out.println("Exception......");
		else if(errorsMap.isEmpty()) {
			errors="All Test Cases Passed";
		} else {
			errors+="Following Test Cases Failed\n";
			for(Integer key:errorsMap.keySet()) {
				errors+=key+"|";
				for(String err:errorsMap.get(key)) {
					errors+=err+"|";
				}
				errors+="\n";
			}
		}
		Utilities.writeFile(basePath+File.separator+"test_result.log", errors);
		//Added by Akanksha,commented the below print statement
	 	System.out.println(errors);
		
		//Added by Akanksha ends
		long stopTime = System.currentTimeMillis();
		//System.out.println("Stopping time of regression test is: ");
	    //System.out.println(stopTime);
        long elapsedTime = stopTime - startTime;
        System.out.println("Total time taken by regression test is: ");
        System.out.print(elapsedTime);
		
	}
	
}
