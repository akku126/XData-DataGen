package testDataGen;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

import parsing.AppTest_Parameters;

public class GenerateDataSet {
		/*
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
		public List<String> generateDatasetForQuery(Connection conn,int queryId,String query, File schemaFile, File sampleDataFile, boolean orderDependent, String tempFilePath, AppTest_Parameters obj) throws Exception{
			String line,schema="",sampleData="";			
			
			schema=Utilities.readFile(schemaFile);
			
			sampleData=Utilities.readFile(sampleDataFile);
			
			return generateDatasetForQuery(conn, queryId, query, schema, sampleData, orderDependent, tempFilePath, obj);
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
		public List<String> generateDatasetForQuery(Connection conn,int queryId,String query, String schema, String sampleData, boolean orderDependent, String tempFilePath, AppTest_Parameters appTestParams) throws Exception{
			
			if(tempFilePath==null | tempFilePath.equals("")){
				tempFilePath="/tmp/"+queryId;
			}
			
			GenerateCVC1 cvc=new GenerateCVC1();
			cvc.setFilePath(tempFilePath);
			cvc.setFne(false); 
			cvc.setIpdb(false);
			cvc.setOrderindependent(orderDependent);	
			
			
			loadSchema(conn,schema);
			loadSampleData(conn,sampleData);
			
			cvc.setSchemaFile(schema);
			cvc.setDataFileName(sampleData);
			
			TableMap.clearAllInstances();
			cvc.setTableMap(TableMap.getInstances(conn, 1));
			cvc.setConnection(conn);
			
			deletePreviousDatasets(cvc);
			//Application Testing
			if(appTestParams==null)
				appTestParams=new AppTest_Parameters();
			cvc.setDBAppparams(appTestParams);
			//end
			FileWriter fw=new FileWriter(Configuration.homeDir+"/temp_smt" +cvc.getFilePath()+"/queries.txt");
			fw.write(query);
			fw.close();
			
			PreProcessingActivity.preProcessingActivity(cvc);
			return listOfDatasets(cvc);
				
		}
		
		/**
		 * Creates tables provided in the schema for the given connection
		 * @param conn
		 * @param schema
		 * @throws Exception
		 */
		public static void loadSchema(Connection conn,String schema) throws Exception{
			
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
			deleteAllTablesFromTestUser(conn);
			for (int i = 0; i < inst.length; i++) {
				 
				if (!inst[i].trim().equals("") && ! inst[i].trim().contains("drop table")) {
					DatabaseMetaData dbmd=conn.getMetaData();      
					String dbType = dbmd.getDatabaseProductName(); 
					String temp = "";
					if (dbType.equalsIgnoreCase("MySql"))
					{
						temp = inst[i].trim();
					}
					else if(dbType.equalsIgnoreCase("PostgreSQL")) {
						temp = inst[i].trim().replaceAll("(?i)^\\s*create\\s+table\\s+", "create temporary table ");	
					}
					PreparedStatement stmt2 = conn.prepareStatement(temp);
						stmt2.executeUpdate();	
					stmt2.close();
				}
				
			}
		}
		
		/**
		 * Loads datasets for the given connection
		 * @param conn
		 * @param sampleData
		 * @throws Exception
		 */
		public static void loadSampleData(Connection conn, String sampleData) throws Exception{
			
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
			String fileList[]=new File(Configuration.homeDir+"/temp_smt" + cvc.getFilePath()).list();
			for(int k=0;k<fileList.length;k++){
				fileListVector.add(fileList[k]);
			}
			Collections.sort(fileListVector);	        
			for(int i=0;i<fileList.length;i++)
			{
				File f1=new File(Configuration.homeDir+"/temp_smt" + cvc.getFilePath() +"/"+fileListVector.get(i));	          
				
				if(f1.isDirectory() && fileListVector.get(i).startsWith("DS"))
				{
					datasets.add(fileListVector.get(i));
				}
			}
			return datasets;
		}
		
		
		public static void deletePreviousDatasets(GenerateCVC1 cvc) throws IOException,InterruptedException {
			
			File f=new File(Configuration.homeDir+"/temp_smt"+cvc.getFilePath()+"/");
			
			if (f.exists()) {		
				File f2[]=f.listFiles();
				if (f2 != null)
				for(int i=0;i<f2.length;i++){
					if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
						
						Utilities.deletePath(Configuration.homeDir+"/temp_smt"+cvc.getFilePath()+"/"+f2[i].getName());
					}
				}
			}
			
			File dir= new File(Configuration.homeDir+"/temp_smt"+cvc.getFilePath());
			if (dir.exists()) {
				for (File file: dir.listFiles()) {
					file.delete();
				}
			}
			else {
				dir.mkdirs();
			}
		}
		
		public static void deleteAllTablesFromTestUser(Connection conn) throws Exception{
			try{
				DatabaseMetaData dbm = conn.getMetaData();

				// added by rambabu
				String dbType = dbm.getDatabaseProductName(); 
				System.out.println(dbType);
				
				if (dbType.equalsIgnoreCase("MySql"))
				{
					String[] types = {"TABLE"};
					ResultSet rs = dbm.getTables(conn.getCatalog(), null, "%", types);	
				
//					ResultSet rs = dbm.getTables(null, null, "%", types);
//					while (rs.next()) {
//					  System.out.println(rs.getString(3));
//					}
					String query= "SET FOREIGN_KEY_CHECKS = 0";
					PreparedStatement pstmt = conn.prepareStatement(query);
					pstmt.executeUpdate();
					pstmt.close();
					
					while(rs.next()){
						String table=rs.getString("TABLE_NAME");		
						if(!table.equalsIgnoreCase("dataset") 
								&& !table.equalsIgnoreCase("xdata_temp1")
								&& !table.equalsIgnoreCase("xdata_temp2")){
							System.out.println("drop table if exists "+table +" cascade");
							//PreparedStatement pstmt = conn.prepareStatement("delete from "+table);						
//							PreparedStatement pstmt = conn.prepareStatement("drop table if exists "+table +" cascade");
//							pstmt.executeUpdate();
//							pstmt.close();
							
							query = "drop table if exists "+table;
							PreparedStatement pstmt1 = conn.prepareStatement(query);
							pstmt1.executeUpdate();
							pstmt1.close();
						}

					}
					
					query= "SET FOREIGN_KEY_CHECKS = 1";
					PreparedStatement pstmt2 = conn.prepareStatement(query);
					pstmt2.executeUpdate();
					pstmt2.close();
					
					rs.close();
					
				}
				
				else if(dbType.equalsIgnoreCase("postgreSQL"))
				{
					String[] types = {"TEMPORARY TABLE"};
					ResultSet rs = dbm.getTables(conn.getCatalog(), null, "%", types);		  
		
					while(rs.next()){
						String table=rs.getString("TABLE_NAME");		
						if(!table.equalsIgnoreCase("dataset") 
								&& !table.equalsIgnoreCase("xdata_temp1")
								&& !table.equalsIgnoreCase("xdata_temp2")){
							//PreparedStatement pstmt = conn.prepareStatement("delete from "+table);						
							PreparedStatement pstmt = conn.prepareStatement("Truncate table "+table +" cascade");
							pstmt.executeUpdate();
							pstmt.close();
						}
		
					} 
				
					rs.close();
				}
				
			}catch(Exception e){
				System.out.println(e);
			}

		}
		
		public static void main(String[] args) throws Exception {
			
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
			
			int queryId=1;
			//String query = "select * from instructor where dept_name not in (select dept_name from department where building != 'Watson')" ;
			//String query = "select count(dept_name) from student group by name having count(id) < 10";
			//String query = "select course_id from section as S where semester = 'Fall' and year = 2009 and not exists (select * from section as T where semester = 'Spring' and year = 2010 and S.course_id = T.course_id)";
			//String query = "select name from instructor where salary > some (select salary from instructor where dept_name = ’Biology’)";
			//String query = "select count(distinct room_number) from classroom, department where department.dept_name = 'Comp. Sci.' and department.building = classroom.building";
			//String query = "with max_budget (value) as (select max(budget) from department) select budget from department, max_budget where department.budget = max_budget.value";
			//String query = "select dept_name, avg_salary from (select dept_name, avg (salary) from instructor group by dept name) as dept avg (dept_name, avg_salary) where avg_salary > 42000";
			//String query = "select dept_name from department where building = 'T%'";
			//String query = "select dept_name from department where building like '%Watson%'";
			//String query = "SELECT takes.course_id FROM student INNER JOIN takes ON(student.id=takes.id) INNER JOIN course ON(course.course_id=takes.course_id) WHERE student.id = '12345'";
			//String query="select * from student where tot_cred >5";	
			//String query ="select dept_name, avg(salary) as avg_salary from instructor group by dept_name having avg(salary) > 42000";
			//String query = "SELECT course_id, title FROM course INNER JOIN section USING(course_id) WHERE year > 2010 AND EXISTS (SELECT * FROM prereq WHERE prereq_id='CS-201')";
			//String query = "select name, title from (instructor natural join teaches) join course using (course_id)";
			//String query = "select id, name from student where tot_cred>30";

			/* I----*/
			  String query = "select id, name from student where tot_cred>30";
			 
			/* II----
			 * String query = "select * from student inner join department using (dept_name) where student.tot_cred > 40 and exists (select * from course where credits >=6 and course.dept_name = 'comp. sci.' )";
			 */
			
			/* III----
			  String query = "select dept_name, avg(salary) from instructor group by dept_name having avg(salary) > 100000";
			 */
			
			/* IV----
			  String query="select * from instructor natural join teaches where dept_name=? and year=?";
			 */
			/* V-----
			  String query = "select course_id from section as S where semester = 'Fall' and year = 2009 and not exists (select * from section as T where semester = 'Spring' and year = 2010 and S.course_id = T.course_id)";
			 */
			 /* VI----
			  String query = "SELECT dept_name, SUM(credits) FROM course INNER JOIN department USING (dept_name) WHERE credits <= 4 GROUP BY dept_name HAVING SUM(credits) < 13";
			 */
			/* VII-----
			  String query = "select * from instructor where dept_name in (select dept_name from department where building = 'Watson')";
			 */
			/* VIII----
			  String query = "(select course_id from section where semester = 'Fall' and year = 2009) except  (select course_id from section where semester = 'Spring' and year = 2010)";
			 */
			/* IX-----
			  String query = "select name, title from (instructor natural join teaches) join course using (course_id)";
			 //*/
			 /* X----
			  String query = "SELECT dept_name, COUNT(DISTINCT course_id) FROM course LEFT OUTER JOIN takes USING(course_id) GROUP BY dept_name";
			 //*/
			/* XI-----
			  String query = "SELECT takes.course_id FROM student INNER JOIN takes ON(student.id=takes.id) INNER JOIN course ON(course.course_id=takes.course_id) WHERE student.id = '12345'";
			 //*/
			/* XII----
			  String query = "select distinct s.id, s.name from student s, takes t where s.id = t.id and  t.grade != 'F'";
			 //*/
			/* XIII----
			  String query = "Select * from section join teaches on section.course_id = teaches.course_id ";
			 //*/
			/* XIV----
			  String query="select course_id,count(id) from course inner join takes where grade=?";			
			 //*/
			/* XV-----
			  String query = "SELECT id FROM takes WHERE grade < (SELECT MIN(grade) FROM takes WHERE year = 2010)";
			 //*/
			/* XVI----
			  String query = "select * from classroom, section where classroom.building = section.building and classroom.room_number = section.room_number";
			 //*/
			/* XVII----
			  String query = "SELECT dept_name, SUM(credits) FROM course INNER JOIN department USING (dept_name) WHERE credits <= 4 GROUP BY dept_name HAVING SUM(credits) < 13";	
			 //*/
			/* XVIII----
			  String query = "Select min(budget) from department";			
			 //*/
			/* XIX----
			  String query="select name,count(*) from student group by name";
			 //*/
			/* XX----
			  String query = "select id, name from student where tot_cred>30";
			 //*/
			/* XXI----
			  String query="select id,name from student";
			 //*/
			//String query = "SELECT course_id, title FROM course inner join section WHERE year = 2010 and  EXISTS (SELECT * FROM prereq WHERE prereq_id='CS-201' AND prereq.course_id = course.course_id) ";
			 //* ---->>>problem with this particular query
			 //*/
			//String query = "select name from instructor where salary is null";
			
			
			File schemaFile=new File("test/universityTest/DDL.sql");
			File sampleDataFile=new File("test/universityTest/sampleData.sql");
			boolean orderDependent=false;
			/* runtime analysis for regression test */
			long startTime = System.currentTimeMillis();
			String tempFilePath=File.separator +queryId;
			
			GenerateDataSet d=new GenerateDataSet();
			//Application Testing
			AppTest_Parameters obj = new AppTest_Parameters();


			//end
			d.generateDatasetForQuery(conn,queryId,query,  schemaFile,  sampleDataFile,  orderDependent,  tempFilePath, obj);
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
	        System.out.println("Total time taken for data generation of the query is: ");
	        System.out.print(elapsedTime);
		}
	
}
