package testDataGen;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
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
			
			if(f.exists()){		
				File f2[]=f.listFiles();
				if(f2 != null)
				for(int i=0;i<f2.length;i++){
					if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
						
						Utilities.deletePath(Configuration.homeDir+"/temp_smt"+cvc.getFilePath()+"/"+f2[i].getName());
					}
				}
			}
			
			File dir= new File(Configuration.homeDir+"/temp_smt"+cvc.getFilePath());
			if(dir.exists()){
				for(File file: dir.listFiles()) {
					file.delete();
				}
			}
			else{
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
			
			//TEMPCODE START : Rahul Sharma
			// REGRESSION TEST
			int regression_test = 0;
			if(regression_test==1) {
				int start = 1;
				int end = 54;
//				end = start = 27;
				for(int i=start;i<=end;i++) {
					int queryId = i;
					String query = readQueryFromFile("test/universityTest/queries.txt", queryId+"");
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
					try {
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
						List<String> dataset = d.generateDatasetForQuery(conn,queryId,query,  schemaFile,  sampleDataFile,  orderDependent,  tempFilePath, obj);
						for(String s:dataset) {
							System.out.println(s);
						}
						
						long stopTime = System.currentTimeMillis();
						long elapsedTime = stopTime - startTime;
				        System.out.println("Total time taken for data generation of the query " +queryId+" is : ");
				        System.out.println(elapsedTime);
					}
					catch(Exception e) {
						e.printStackTrace();
						System.out.println("Query : "+query+"\n"+e);
					}
				}
				//TEMPCODE END : Rahul Sharma
			}
			else {
			
			
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
				
				
				//String query = "select id, name from student where tot_cred>=30";
	
				int queryId=1000;
				//String query = "SELECT DISTINCT player.player_id, player.player_name FROM player INNER JOIN player_match ON player.player_id = player_match.player_id WHERE player.country_name =  'Australia' and (role_desc = 'Captain' or role_desc = 'CaptainKeeper') ORDER BY player_name;";
				//String query = "(select course_id from section where semester = 'Fall' and year = 2009) union (select course_id from section where semester = 'Spring' and year = 2010)";
//				String query = "SELECT course_id, title FROM course INNER JOIN section USING(course_id) WHERE year = 2010 AND EXISTS (SELECT * FROM prereq WHERE prereq_id='CS-201' AND prereq.course_id = course.course_id)";
				//String query = "select * from (select * from student where dept_name = 'Comp.Sci') AS q1 natural full outer join (select * from takes where semester = 'Spring' and year = 2009) AS q2";
			//String query = "select team_name from team where team_id not in (select match_winner from match where venue_id = (select venue_id from venue where venue_name = 'M Chinnaswamy Stadium'))";
				
				//Ques 1
			    //String query = "SELECT player_name FROM player WHERE dob >= '1990-01-01' AND country_name = 'India'";
				
				//Ques 2
				//String query ="SELECT DISTINCT player.player_name FROM ball_by_ball, player WHERE ball_by_ball.bowler = player.player_id AND ball_by_ball.extra_runs > 2 ORDER BY player.player_name ASC";
				
				//Ques 3
				//String query ="SELECT team_name, win_margin FROM match, team where team_id = match_winner AND win_margin > 10 ORDER BY win_margin DESC, team_name"; 

				//Ques 4
				//String query ="SELECT DISTINCT player.player_id, player.player_name FROM player INNER JOIN player_match ON player.player_id = player_match.player_id WHERE player.country_name =  'Australia' and (role_desc = 'Captain' or role_desc = 'CaptainKeeper') ORDER BY player_name;";
				
				//Ques 5
				//String query ="SELECT DISTINCT player.player_name FROM match, player_match, player WHERE match.match_id = player_match.match_id AND match.man_of_match = player_match.player_id AND player.player_id = player_match.player_id AND (player_match.role_desc = 'Captain' OR player_match.role_desc = 'CaptainKeeper');";
				
				//Ques 6
				//String query ="SELECT DISTINCT player.player_name FROM match, ball_by_ball, player WHERE match.match_id = ball_by_ball.match_id AND player.player_id = ball_by_ball.striker AND ball_by_ball.runs_scored = 6 AND (match.season_year = 2011 OR match.season_year = 2013);";
				
				//Ques 7
				//String query ="SELECT DISTINCT player.player_name FROM player,ball_by_ball WHERE player.player_id = ball_by_ball.bowler AND player.bowling_skill = 'Right-arm medium' AND ball_by_ball.out_type = 'caught and bowled'";
				
				//Ques 8
				//String query ="SELECT DISTINCT player.player_name FROM match INNER JOIN venue ON match.venue_id = venue.venue_id, player RIGHT OUTER JOIN player_match ON player_match.player_id = player.player_id WHERE venue.venue_name = 'Eden Gardens' AND match.match_id = player_match.match_id AND player.country_name != 'India' order by player.player_name";
				
				//Ques 9
				//String query ="SELECT team.team_name FROM team, match WHERE match.season_year = 2015 AND match.toss_winner != match.match_winner AND team.team_id = match.match_winner";
				
				//Ques 10
				//String query ="SELECT DISTINCT player.player_name, player.country_name FROM player, ball_by_ball, player_match WHERE player_match.match_id = ball_by_ball.match_id AND player_match.player_id = ball_by_ball.bowler AND player_match.player_id = player.player_id AND (player_match.role_desc = 'Captain' OR player_match.role_desc = 'CaptainKeeper')";
				
				//Ques 11
				//String query ="SELECT DISTINCT player_id,player_name FROM ball_by_ball,player,match,venue WHERE match.match_id = ball_by_ball.match_id AND venue.venue_id = match.venue_id AND player_id = striker AND batting_hand = 'Left-hand bat' AND runs_scored = 4 AND city_name = 'Pune'";
				
				//Ques 12
				//String query ="SELECT player_name FROM player WHERE country_name = 'Sri Lanka' EXCEPT SELECT player_name FROM player RIGHT OUTER JOIN ball_by_ball on ball_by_ball.striker = player.player_id";
				
				//Ques 13
				//String query ="SELECT team_name FROM team EXCEPT SELECT team.team_name FROM team, match LEFT OUTER JOIN venue ON venue.venue_id = match.venue_id WHERE team.team_id = match.match_winner AND venue.venue_name = 'M Chinnaswamy Stadium'";
				
				//Ques 14
				//String query ="SELECT player.player_name FROM ball_by_ball, player, match WHERE match.match_id = ball_by_ball.match_id AND player.player_id = ball_by_ball.striker AND ball_by_ball.out_type = 'bowled' AND match.season_year = 2017 INTERSECT SELECT player_name FROM ball_by_ball, player, match WHERE match.match_id = ball_by_ball.match_id AND player.player_id = ball_by_ball.striker AND ball_by_ball.out_type = 'run out' AND match.season_year = 2017";
				
				//Ques 15
				//String query ="SELECT team_name FROM team EXCEPT select team_name FROM ball_by_ball,match,player_match,team WHERE team.team_id = player_match.team_id AND player_match.match_id = match.match_id AND striker = player_match.player_id AND match.match_id = ball_by_ball.match_id AND out_type = 'caught and bowled'";
				
				//Ques 16
				//String query ="with temp1 as (SELECT player.player_name FROM ball_by_ball, player, match WHERE match.match_id = ball_by_ball.match_id AND player.player_id = ball_by_ball.striker AND ball_by_ball.out_type = 'bowled' AND match.season_year = 2017), temp2 as (SELECT player_name FROM ball_by_ball, player, match WHERE match.match_id = ball_by_ball.match_id AND player.player_id = ball_by_ball.striker AND ball_by_ball.out_type = 'run out' AND match.season_year = 2017) select temp1.player_name from temp1 inner join temp2 on temp2.player_name = temp1.player_name;";
				
				//FROM clause subquery
				//String query ="select distinct pl.player_id,pl.player_name from (select distinct * from player_match where role_desc = 'Captain' or role_desc = 'CaptainKeeper') as p inner join player as pl on p.player_id = pl.player_id where pl.country_name = 'Australia' order by pl.player_name asc";
				
				
				//String query="select venue_id,venue_name,player_id,player_name from (select venue.venue_id,venue_name, player_id, player_name,rank() over (partition by venue.venue_id order by player_name) from (select venue_id,man_of_match,count(*) as cnt from match group by venue_id,man_of_match order by cnt desc, venue_id) as A, player,venue where A.venue_id = venue.venue_id and  A.man_of_match = player.player_id order by cnt desc) as C where rank = 1 order by venue_id";
				//String query="";
				
				//String query = "with dept_count(dept_name, cnt) as (select  dept_name, count(*) from takes, course where takes.course_id = course.course_id group by dept_name), maxcnt(cnt) as (select max(cnt) from dept_count) select dept_name from dept_count, maxcnt where dept_count.cnt = maxcnt.cnt";
				
				//String query = "select course_id from section as S where semester = 'Fall' and year = 2009 and exists (select * from section as T where semester = 'Spring' and year = 2010 and S.course_id = T.course_id)";
				
			/*
			 * String query = "SELECT course_id, title FROM course " +
			 * "inner join section WHERE year = 2010 and course.course_id = 'CS-203' " +
			 * "and  EXISTS (SELECT * FROM prereq WHERE prereq_id='CS-201' " +
			 * "AND prereq.course_id = course.course_id) ";
			 */
						// + ")";
				 /* ---->>>problem with this particular query
				 */
	
				  // String query="select id,name from student";
				 //*/
				//String query = "SELECT course_id, title FROM course inner join section WHERE year = 2010 and  EXISTS (SELECT * FROM prereq WHERE prereq_id='CS-201' AND prereq.course_id = course.course_id) ";
				 //* ---->>>problem with this particular query
				 //*/
	//			String query = "select name from instructor where salary is null";
				// Query to test in clause in nested subqueries
	//			String query = "SELECT course_id, title FROM course "
	//					+ "inner join section WHERE course.course_id = 'CS-203' "
	//					+ "and course_id in (SELECT prereq.course_id FROM prereq WHERE prereq_id='CS-201' "
	//					// + "AND prereq.course_id = course.course_id) ";
	//					+ ")";
				
	//			String query = "SELECT course_id FROM section";
	//			String query = "SELECT course_id FROM section where semester='Summer'";
	//			String query = "SELECT course_id FROM section natural join course";
				
	//			String query="SELECT course.course_id, title FROM course inner join section ON course.course_id = section.course_id WHERE course.course_id = 'CS-203' and section.course_id in (SELECT prereq.course_id FROM prereq WHERE prereq_id='CS-201' AND prereq.course_id = course.course_id)";
				
				
	//			String query = "SELECT course_id, title FROM course "
	//					+ "inner join section WHERE year = 2010 and course.course_id = 'CS-203' "
	//					+ "and course_id in (SELECT prereq.course_id FROM prereq WHERE prereq_id='CS-201' "
	//					// + "AND prereq.course_id = course.course_id) ";
	//					+ ")";
				
	//			String query = "SELECT course_id, title FROM course "
	//					+ "inner join section WHERE year = 2010 and course.course_id = 'CS-203'";
	//					// + "AND prereq.course_id = course.course_id) ";
				
				
				// queries TEMPCODE : Rahul Sharma
				
//				String query = "SELECT * FROM instructor "
//							 + "WHERE dept_name in (SELECT dept_name FROM department "
//							 + 						"WHERE building = 'Watson')";
//				
//				queryId = 1000;
//				String query = "SELECT course_id, title "
//							 + "FROM course INNER JOIN section USING(course_id) "
//							 + "WHERE year = 2010 AND "
//							 + "EXISTS (SELECT * FROM prereq "
//							 + 			"WHERE prereq_id='CS-201' AND "
//							 + 			"prereq.course_id = course.course_id)";
//				
//				
	//			String query = "SELECT takes.course_id "
	//					+ "FROM student INNER JOIN takes "
	//					+ "ON(student.id=takes.id) "
	//					+ "WHERE student.id = '12345'";
				
	//			String query = "SELECT takes.course_id "
	//					+ "FROM student INNER JOIN takes "
	//					+ "ON(student.id=takes.id) INNER JOIN course "
	//					+ "ON(course.course_id=takes.course_id) "
	//					+ "WHERE student.id = '12345'";
	//			
	//			String query = "SELECT course_id, title FROM course "
	//					+ "inner join section WHERE year = 2010 and course.course_id = 'CS-203' "
	//					+ "and course_id in (SELECT prereq.course_id FROM prereq WHERE prereq_id='CS-201' )";
	//			
	//			String query = "SELECT course_id, title FROM course "
	//					+ "inner join section WHERE year = 2010 and course.course_id = 'CS-203' "
	//					+ "and course_id in (SELECT prereq.course_id FROM prereq WHERE prereq_id='CS-201' "
	//					 + "AND prereq.course_id = course.course_id)";
				
//				String query="SELECT course_id, title \n" + 
//						"    FROM course inner join section \n" + 
//						"    WHERE year = 2010 and course.course_id='CS-203' and course_id in \n" + 
//						"    (SELECT prereq.course_id FROM prereq WHERE prereq_id='CS-201');";
				
//				String query = "SELECT id,course_id "
//							 + "FROM student LEFT OUTER JOIN "
//							 + "(SELECT * FROM takes WHERE takes.year=2018) "
//							 + "USING(id)";
				
//				String query = "SELECT course_id, title \n" + 
//						"    FROM course INNER JOIN section USING(course_id) \n" + 
//						"    WHERE year = 2010 \n" + 
//						"    AND EXISTS (SELECT * FROM prereq \n" + 
//						"                WHERE prereq_id='CS-201' AND \n" + 
//						"                      prereq.course_id = course.course_id)";
//				
//				String query = "SELECT name FROM instructor " + 
//							   "WHERE EXISTS (SELECT * FROM teaches t1 "
//							   + 			 "WHERE instructor.ID = t1.ID and "
//							   + 			 "EXISTS (SELECT * FROM teaches t2 "
//							   + 					 "WHERE t2.ID=t1.ID and t2.year=2010))";
				
//				String query = "SELECT name FROM instructor "
//						     + "WHERE EXISTS (SELECT * FROM teaches t1 "
//						     + 				 "WHERE instructor.ID = t1.ID and "
//						     + 				 "NOT EXISTS (SELECT * FROM teaches t2 "
//						     + 							 "WHERE t2.ID=t1.ID and t2.year=2010))\n" + 
//						"";
				
//				
				// TEMPCODE Rahul Sharma : QUERIES (FOR SUBQUERY TABLE)
//				queryId = 500;
//				String query = "SELECT DISTINCT t1.ID, name, t1.year "
//					     + "FROM takes t1 , student s2 "
//					     + "WHERE t1.ID=s2.ID and "
//					     + "EXISTS ( SELECT s1.course_id from takes t2 inner join section s1"
//					     + 			" ON t2.course_id=s1.course_id WHERE t2.ID=s2.ID )";
//				
//				queryId=501;
//				String query = "SELECT DISTINCT t1.ID, name, t1.year "
//						     + "FROM takes t1 , student s2 "
//						     + "WHERE t1.ID=s2.ID and "
//						     + "EXISTS ( SELECT s1.course_id, min(t2.year) from takes t2 inner join section s1"
//						     + 			" ON t2.course_id=s1.course_id WHERE t2.ID=s2.ID group by s1.course_id )";
//				
				
//				queryId=502;
//				String query = "SELECT * FROM takes t1 ,student s2 " 
//						 +" WHERE t1.ID=s2.ID and "
//						 +" t1.year = (SELECT min(t2.year) FROM takes t2 INNER JOIN section s1 "
//						 +"            ON t2.course_id=s1.course_id "
//						 +"            WHERE t2.ID=s2.ID)";
//				
			
//				
				queryId=503;
				String query = "SELECT course_id, title FROM course "
						 + "inner join section WHERE year = 2010 and course.course_id = 'CS-203' "
					     + "and "
					     + " exists (SELECT prereq.course_id FROM prereq join teaches "
					     		+ "on teaches.course_id = prereq.course_id "
					     		+ "WHERE prereq_id = 'CS-201' and course.course_id = prereq.course_id )";
				
				
//				queryId=504;
//				String query = "SELECT DISTINCT t1.ID, s1.name, t1.year "
//					     + "FROM takes t1 , student s1 "
//					     + "WHERE t1.ID=s1.ID and "
//					     + "EXISTS (SELECT * from takes t2 inner join section s2 "
//					     + 			"ON t2.course_id=s2.course_id WHERE t2.year > t1.year)"; 
//				
//				
//				queryId=505;
//				String query="SELECT course.course_id,title "
//						+ "FROM course INNER JOIN section "
//						+ "ON course.course_id=section.course_id "
//						+ "WHERE year=2010 "
//						+ "and course.course_id='CS-203' and "
//						+ "EXISTS "
//						+ "(SELECT prereq.prereq_id,count(*) "
//						+ "FROM prereq join teaches "
//						+ "ON teaches.course_id=prereq.course_id "
//						+ "WHERE prereq_id='CS-201' and "
//						+ "course.course_id=prereq.course_id "
//						+ "GROUP BY prereq.prereq_id)";
//				
//				queryId = 506;
//				String query="SELECT * FROM takes t1 ,student s2 " + 
//						"    WHERE t1.ID=s2.ID and " + 
//						"          t1.year = (SELECT min(t2.year) " + 
//						"                     FROM takes t2 INNER JOIN section s1 " + 
//						"                     ON t2.course_id=s1.course_id " + 
//						"                     WHERE t2.ID=s2.ID and " + 
//						"                           EXISTS (SELECT c1.building " + 
//						"                                   FROM classroom c1 INNER JOIN section s3 " + 
//						"                                   ON c1.building=s3.building " + 
//						"                                   WHERE c1.capacity>50))";
//				
//				queryId = 507;
//				String query = "SELECT name FROM instructor "
//						+ "WHERE EXISTS (SELECT * FROM teaches t1 "
//						+ "       WHERE instructor.ID = t1.ID  and EXISTS (select * from teaches t2 where t2.ID=t1.ID and t1.year=2010))";
//						
				
				// TEMPCODE Rahul Sharma : Queries end				
			
			 //     query ="SELECT team.team_name FROM team, match WHERE match.season_year = 2015 AND match.toss_winner != match.match_winner AND team.team_id = match.match_winner;\n";
				
//				queryId = 1002;     // Query not supported
//				String query = "SELECT s1 . course_id , s1 . building FROM section s1 WHERE\n"
//						+ "semester = 'Fall' and year = 2009 and ( s1 . course_id , s1 .\n"
//						+ "building ) IN ( SELECT s2 . course_id , s2 . building FROM\n"
//						+ "section s2 WHERE semester = 'Spring' and year = 2010)";
				
				//AGGREGATION operation
//				queryId = 1000;
//				String query = "select dept_name, avg(salary) as avg_salary from instructor group by dept_name having count(name) > 4;";
				
//				queryId = 5;
//				String query = "select avg (salary) from instructor where dept_name= 'Comp. Sci.'";
				
				System.out.println("\n"+query+"\n");
				
				File schemaFile=new File("test/universityTest/DDL.sql");
				File sampleDataFile=new File("test/universityTest/sampleData.sql");
				boolean orderDependent=false;
				long startTime = System.currentTimeMillis();
				String tempFilePath=File.separator +queryId;
				
				GenerateDataSet d=new GenerateDataSet();
				//Application Testing
				AppTest_Parameters obj = new AppTest_Parameters();
	
	
				//end
				List<String> dataset = d.generateDatasetForQuery(conn,queryId,query,  schemaFile,  sampleDataFile,  orderDependent,  tempFilePath, obj);
				for(String s:dataset) {
					System.out.println(s);
				}
				long stopTime = System.currentTimeMillis();
				long elapsedTime = stopTime - startTime;
		        System.out.println("Total time taken for data generation of the query is : ");
		        System.out.print(elapsedTime);
			}
		}

		//TEMPCODE Rahul Sharma
		public static String readQueryFromFile(String fileName,String queryId) throws IOException {
			File file = new File(fileName); 
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(file)); 
			  String st; 
			  while ((st = br.readLine()) != null) {
				//System.out.println(st);
			if(st.length()>0) {
			    StringTokenizer stok = new StringTokenizer(st,"|");
				    if(stok.nextToken().contentEquals(queryId))
				    	return stok.nextToken();
				}
			}
		    return "";
		}

}

