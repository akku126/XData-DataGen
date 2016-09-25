package util;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyConnection {	
	
	private static Logger logger = Logger.getLogger(MyConnection.class.getName()); 

	
	static DataSource dataSource= new DataSource();
	
	//public static Connection getExistingDatabaseConnection() throws Exception{
		//return graderDatesource.getConnection();
	//}
	
	//public static Connection getTestDatabaseConnection() throws Exception{
		//return testerDatasource.getConnection();
	//}
	
	public static Connection getDatabaseConnection(){
		return dataSource.getConnection();
	}
	
	public static void main(String[] args) {
		/*MyConnection myConn = new MyConnection();
		try {
			Connection conn = myConn.getExistingDatabaseConnection();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		for (int x=0; x < 100; x++)
	    {
	        TestThread temp= new TestThread("Thread #" + x);
	        temp.start();
	        logger.log(Level.INFO,"Started Thread:" + x);
	    }
	}
	
}
