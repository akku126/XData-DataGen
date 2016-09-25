package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

class TestThread extends Thread{
	public TestThread(String s){
		super(s);
	}
	
	@Override
	public void run(){
		try {
			//Connection conn = MyConnection.getExistingDatabaseConnection();
			try(Connection conn = MyConnection.getDatabaseConnection()){
				String create="CREATE TEMPORARY TABLE X(Y varchar(10))";
				Statement st = conn.createStatement();
				st.execute(create);
				
				PreparedStatement stmt = null;
				
				for(int i = 0; i < 100; i++){
					stmt = conn.prepareStatement("insert into x values(?)");
					stmt.setString(1, getName());
					stmt.execute();	
				}
				
				create = "SELECT Y FROM X LIMIT 1";
				stmt = conn.prepareStatement(create);
				
				ResultSet rs = stmt.executeQuery();
				
				if(rs.next()){
					System.out.println("Thread:" + getName() + ":" + rs.getString("Y"));
				}
				
				st = conn.createStatement();
				st.execute("DISCARD TEMPORARY");	
			}//close - try for connection
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
}