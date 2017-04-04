	package testDataGen;
	import java.sql.Connection;
	import java.sql.DriverManager;
	import java.sql.PreparedStatement;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.List;
	import java.util.logging.Level;
	import util.Configuration;
	import util.TableMap;
	import util.Utilities;
	import java.io.BufferedReader;
	import java.io.File;
	import java.io.FileOutputStream;
	import java.io.FileReader;
	import java.io.FileWriter;
	import java.io.IOException;
	
	public class GenerateDataset {
		/**
		 * This function generates test datasets for a query
		 * @param conn Database connection to use
		 * @param queryId The query id
		 * @param query The query for which dataset needs to be generated
		 * @param schemaFile File containing the schema against which the query has been written
		 * @param sampleDataFile File containing sample data to generate realistic values
		 * @param orderDependent Whether the order of tuples in the result matter. Set this to true for queries that have ORDER BY
		 * @param tempFilePath File path to create temporary files and datasets
		 * @return List of dataset ids that have been generated
		 * @throws Exception
		 */
		public List<String> generateDatasetForQuery(Connection conn,int queryId,String query, File schemaFile, File sampleDataFile, boolean orderDependent, String tempFilePath) throws Exception{
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
			
			return generateDatasetForQuery(conn, queryId, query, schema, sampleData, orderDependent, tempFilePath);
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
		public List<String> generateDatasetForQuery(Connection conn,int queryId,String query, String schema, String sampleData, boolean orderDependent, String tempFilePath) throws Exception{
			
			if(tempFilePath==null | tempFilePath.equals("")){
				tempFilePath="/tmp/"+queryId;
			}
			
			GenerateCVC1 cvc=new GenerateCVC1();
			cvc.setFilePath(tempFilePath);
			cvc.setFne(false); 
			cvc.setIpdb(false);
			cvc.setOrderindependent(orderDependent);	
			
			
			checkSchema(conn,schema);
			checkSampleData(conn,sampleData);
			
			cvc.setSchemaFile(schema);
			cvc.setDataFileName(sampleData);
			
			TableMap.clearAllInstances();
			cvc.setTableMap(TableMap.getInstances(conn, 1));
			cvc.setConnection(conn);
			
			deletePreviousDatasets(cvc);
			FileWriter fw=new FileWriter(Configuration.homeDir+"/temp_cvc" +cvc.getFilePath()+"/queries.txt");
			fw.write(query);
			fw.close();
			
			PreProcessingActivity.preProcessingActivity(cvc);
			return listOfDatasets(cvc);
			
			
		}
		
		private void checkSchema(Connection conn,String schema) throws Exception{
			
			byte[] dataBytes = null;
			String tempFile = "";
			FileOutputStream fos = null;
			ArrayList<String> listOfQueries = null;
			ArrayList<String> listOfDDLQueries = new ArrayList<String>();
			String[] inst = null;
			
			dataBytes = schema.getBytes();
			tempFile = "/tmp/dummy";
			
			 fos = new FileOutputStream(tempFile);
			fos.write(dataBytes);
			fos.close();
			listOfQueries = Utilities.createQueries(tempFile);
			inst = listOfQueries.toArray(new String[listOfQueries.size()]);
			listOfDDLQueries.addAll(listOfQueries);
			for (int i = 0; i < inst.length; i++) {
				 
				if (!inst[i].trim().equals("") && ! inst[i].trim().contains("drop table")) {
					String temp = inst[i].replaceAll("(?i)^[ ]*create[ ]+table[ ]+", "create temporary table ");
					PreparedStatement stmt2 = conn.prepareStatement(temp);
						stmt2.executeUpdate();	
					stmt2.close();
				}
			}
		}
		
		private void checkSampleData(Connection conn, String sampleData) throws Exception{
			
			byte[] dataBytes = null;
			String tempFile = "/tmp/dummy";
			FileOutputStream fos = null;
			ArrayList<String> listOfQueries = null;
			String[] inst = null;
		
			dataBytes = sampleData.getBytes(); 
			fos = new FileOutputStream(tempFile);
			fos.write(dataBytes);
			fos.close();
			
			listOfQueries = Utilities.createQueries(tempFile);
			inst = listOfQueries.toArray(new String[listOfQueries.size()]);
			 
			for (int i = 0; i < inst.length; i++) {
				if (!inst[i].trim().equals("") && !inst[i].contains("drop table") && !inst[i].contains("delete from")) {
					
					PreparedStatement stmt = conn.prepareStatement(inst[i]);
						stmt.executeUpdate();							
						stmt.close();
				}
			}
		}
		
		private List<String> listOfDatasets(GenerateCVC1 cvc) {
			ArrayList<String> fileListVector = new ArrayList<String>();		
			ArrayList<String> datasets = new ArrayList<String>();
			String fileList[]=new File(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath()).list();
			for(int k=0;k<fileList.length;k++){
				fileListVector.add(fileList[k]);
			}
			Collections.sort(fileListVector);	        
			for(int i=0;i<fileList.length;i++)
			{
				File f1=new File(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() +"/"+fileListVector.get(i));	          
				
				if(f1.isDirectory() && fileListVector.get(i).startsWith("DS"))
				{
					datasets.add(fileListVector.get(i));
				}
			}
			return datasets;
		}
		
		
		public static void deletePreviousDatasets(GenerateCVC1 cvc) throws IOException,InterruptedException {
			
			File f=new File(Configuration.homeDir+"/temp_cvc"+cvc.getFilePath()+"/");
			
			if(f.exists()){		
				File f2[]=f.listFiles();
				if(f2 != null)
				for(int i=0;i<f2.length;i++){
					if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
						
						Utilities.deletePath(Configuration.homeDir+"/temp_cvc"+cvc.getFilePath()+"/"+f2[i].getName());
					}
				}
			}
			
			File dir= new File(Configuration.homeDir+"/temp_cvc"+cvc.getFilePath());
			if(dir.exists()){
				for(File file: dir.listFiles()) {
					file.delete();
				}
			}
			else{
				dir.mkdirs();
			}
		}
		
		public static void main(String[] args) throws Exception {
			
			Class.forName("org.postgresql.Driver");
			
			String loginUrl = "jdbc:postgresql://" + Configuration.getProperty("databaseIP") + ":" + Configuration.getProperty("databasePort") + "/" + Configuration.getProperty("databaseName");
			Connection conn=DriverManager.getConnection(loginUrl, Configuration.getProperty("existingDatabaseUser"), Configuration.getProperty("existingDatabaseUserPasswd"));;
			
			int queryId=1;
			String query="select id,name from student ";
			//String query = "select ID, course_id from teaches";
			File schemaFile=new File("/tmp/DDL.sql");
			File sampleDataFile=new File("/tmp/data.sql");
			boolean orderDependent=false;
			String tempFilePath="4/Generated/New";
			
			GenerateDataset d=new GenerateDataset();
			d.generateDatasetForQuery(conn,queryId,query,  schemaFile,  sampleDataFile,  orderDependent,  tempFilePath);
			
		}
}
