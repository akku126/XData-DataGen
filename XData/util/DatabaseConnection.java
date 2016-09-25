package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

	private static Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

	
	@Deprecated
	public static Connection getConnection(DatabaseConnectionDetails dbDetails) throws Exception{
		
		String dbType = dbDetails.getDbType(dbDetails.getDbType());
		Connection con = null;
		if(dbType.equals("Oracle")){			
			con = getOracleConnection(dbDetails);
			
		}else if(dbType.equals("MySql")){
			
			con = getMySqlConnection(dbDetails); 
		}
		else if(dbType.equals("PostgreSQL")){
			con=getPostgreSQLConnection(dbDetails);
		}
		else if(dbType.equals("db2")){
			 
		}
		else if(dbType.equals("MSSQL")){
			con = getMicorsoftSQLConnection(dbDetails);
		}
			return con;
	}

	@Deprecated
	private static Connection getOracleConnection(DatabaseConnectionDetails dbDetails) throws Exception {
		    String driver = "oracle.jdbc.driver.OracleDriver";
		    String url = "jdbc:oracle:thin:@"+dbDetails.getJdbc_Url()+":"+dbDetails.getDbName();
		    String username = dbDetails.getDbUser();
		    String password = dbDetails.getDbPwd();
		    logger.log(Level.INFO,"----------------GET ORACLE CONNECTION-----------");
		    Class.forName(driver); // load Oracle driver
		    Connection conn = DriverManager.getConnection(url, username, password);

		    return conn;
		  }
	@Deprecated
		private static Connection getMySqlConnection(DatabaseConnectionDetails dbDetails) throws Exception {
		    String driver = "org.gjt.mm.mysql.Driver";
		    
		    String url="jdbc:mysql://"+dbDetails.getJdbc_Url()+"/"+dbDetails.getDbName();
		    String username = dbDetails.getDbUser();
		    String password = dbDetails.getDbPwd();
		    Class.forName(driver); // load MySQL driver
		    Connection conn = DriverManager.getConnection(url, username, password);
			
		    return conn;
		  }
		  @Deprecated
		  private static Connection getPostgreSQLConnection(DatabaseConnectionDetails dbDetails) throws Exception{
			  String driver = "org.postgresql.Driver";
			  
			  String url="jdbc:postgresql://"+dbDetails.getJdbc_Url()+"/"+dbDetails.getDbName();
			    String username = dbDetails.getDbUser();
			    String password = dbDetails.getDbPwd();
			    Class.forName(driver); // load POSTGRESQL driver
			    Connection conn = DriverManager.getConnection(url, username, password);
				
			    return conn;
		  }
		  @Deprecated
			private static Connection getMicorsoftSQLConnection (
					DatabaseConnectionDetails dbDetails) throws Exception{
				Connection con = null;
				try{
					String dbUserName = dbDetails.getDbUser();
					String dbPassword = dbDetails.getDbPwd();
					Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

					String dbHostName = "windowsHostName";
					String dbName = dbDetails.getDbName();
	
					String connectionUrl = "jdbc:sqlserver://" + dbDetails.getJdbc_Url() +
					   ";databaseName=" + dbName + ";user=" + dbUserName + ";password=" + dbPassword + ";";
					con = DriverManager.getConnection(connectionUrl);
				}catch(Exception e){
					logger.log(Level.SEVERE, "DB Connection with SQL SERVER Error", e);
					//e.printStackTrace();
					throw e;
				}
				return con;
			}
		  
		  /**
		    * This method gets the grader connection specified by the instructor while creating the assignment
		    * The Grader connection is used for running the instructor queries for generating data sets
		    * 
		    * @param assignment_id
		    * @return
		    */
		   public Connection getGraderConnection(int assignment_id){
			   Connection graderConn = null;
			  
			   try(Connection con =MyConnection.getDatabaseConnection()){
				   try(PreparedStatement pstmnt = con.prepareStatement("select connection_id,course_id from xdata_assignment where assignment_id=?")){
				   pstmnt.setInt(1, assignment_id);
				   
				   try(ResultSet rs = pstmnt.executeQuery()){
					   if(rs.next()){
						   int conn_id = rs.getInt("connection_id");
						   String course_id=rs.getString("course_id");
						   PreparedStatement smt = con.prepareStatement("select * from xdata_database_connection where connection_id = ? and course_id=?");
						   smt.setInt(1,conn_id);
						   smt.setString(2,course_id);
						   ResultSet rSet = smt.executeQuery();
						   if(rSet.next()){
							  	String jdbc = rSet.getString("jdbcdata");
								String dbUser = rSet.getString("database_user");
								String dbPassword = rSet.getString("database_password");
								
								DatabaseConnectionDetails dbDetails = new DatabaseConnectionDetails();
								DatabaseConnection dbConnection = new DatabaseConnection();
								dbDetails.setConnName(rSet.getString("connection_name"));
								dbDetails.setDbName(rSet.getString("database_name"));
								dbDetails.setDbType(rSet.getString("database_type"));
								dbDetails.setDbUser(rSet.getString("database_user"));
								dbDetails.setDbPwd(rSet.getString("database_password"));
								dbDetails.setJdbc_Url(rSet.getString("jdbcdata"));
								
								//graderConn = dbConnection.getConnection(dbDetails);
								GraderDatasource dataSource = new GraderDatasource();
								graderConn = dataSource.getConnection(dbDetails);								
	
						   }	 						
				   }//end result set try
				   }//end statement try
			   }//end connection try
				  
				   
			   }catch(Exception e){
				   logger.log(Level.SEVERE, "Grader Connection Error: ", e);
				   //e.printStackTrace();
			   }
			   return graderConn;
		   }
		   
		   /**
			 * This method gets the tester connection specified by the instructor while creating the assignment
			 * The Tester connection is used for running the student queries for generating data sets and these datasets 
			 * are compared with instructor's dataset
			 * 
			 * @param assignment_id
			 * @return
			 */
			 public Connection getTesterConnection(int assignment_id){
				   Connection testerConn = null; 
				  
				   try(Connection con =MyConnection.getDatabaseConnection()){
					   try(PreparedStatement pstmnt = con.prepareStatement("select connection_id,course_id from xdata_assignment where assignment_id=?")){
					   pstmnt.setInt(1, assignment_id);
					   
					   try(ResultSet rs = pstmnt.executeQuery()){
						   if(rs.next()){
							   int conn_id = rs.getInt("connection_id");
							   String course_id = rs.getString("course_id");
							   PreparedStatement smt = con.prepareStatement("select * from xdata_database_connection where connection_id = ? and course_id=?");
							   smt.setInt(1,conn_id);
							   smt.setString(2,course_id);
							   ResultSet rSet = smt.executeQuery();
							   if(rSet.next()){
								   
								  	String jdbc = rSet.getString("jdbcdata");
									String dbUser = rSet.getString("test_user");
									String dbPassword = rSet.getString("test_password");
								 	
									DatabaseConnectionDetails dbDetails = new DatabaseConnectionDetails();
									DatabaseConnection dbConnection = new DatabaseConnection();
									dbDetails.setConnName(rSet.getString("connection_name"));
									dbDetails.setDbName(rSet.getString("database_name"));
									dbDetails.setDbType(rSet.getString("database_type"));
									dbDetails.setDbUser(rSet.getString("test_user"));
									dbDetails.setDbPwd(rSet.getString("test_password"));
									dbDetails.setJdbc_Url(rSet.getString("jdbcdata"));
									//testerConn = dbConnection.getConnection(dbDetails);
									TesterDatasource dataSource = new TesterDatasource();
									//alternate db access
									if(course_id.equals("AutomatedTesting")){
									   testerConn = this.alternateDBAccess(dbDetails);
									}else{
										testerConn = dataSource.getConnection(dbDetails);
									}
							   }	   
						   }
					   }//end result set try
					   }//end statement try
				   }//end connection try
				   catch(Exception e){
					   logger.log(Level.SEVERE, "Tester Connection Error:", e);
				    // e.printStackTrace(); 
			   }
				  return testerConn;
			 }
			 /*
			  * Created BY : Anurag for Automated Testing
			  */
			 public Connection getDownloadDataSetConnection()throws Exception{
				 Connection conn=null;
				 try {
					 Class.forName("org.postgresql.Driver");
					 conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/xdata","testing1","password");
				 }
				 catch(ClassNotFoundException ex) {
					 System.out.println("Error: unable to load driver class!");
					 System.exit(1);
				 }
				 return conn;
			 }
			 private Connection alternateDBAccess(DatabaseConnectionDetails dcd)throws Exception{
				 Connection conn=null;
				 try {
					 Class.forName("org.postgresql.Driver");
					 conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/xdata","testing2","password");
				 }
				 catch(ClassNotFoundException ex) {
					 System.out.println("Error: unable to load driver class!");
					 System.exit(1);
				 }
				 return conn;
			 }
}
