package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import util.MyConnection;

/*
 * loads the specified schema into the schema info table
 */
public class LoadSchema {
	
	Connection dbcon;
	
	public LoadSchema()throws Exception{
		try {
			initDB();
		} catch (Exception e){
			e.printStackTrace();
			throw new Exception(e);
		}
	}
	
	public void initLoadSchema(String courseId, String fileName)throws Exception{
		int newSchemaId = getNewSchemaId();
		//get the file contents
		String content="";
		String t;
		try {
			BufferedReader ord3 = new BufferedReader(new FileReader(fileName));
			while ((t=ord3.readLine())!=null){
				content +=t;
			}
			ord3.close();
		} catch (Exception e){
			e.printStackTrace();
			throw new Exception(e);
		}
		PreparedStatement stmt = dbcon.prepareStatement("Insert into xdata_schemainfo values(?, ?, ?, ? )");
		stmt.setString(1, courseId);
		stmt.setInt(2, newSchemaId);
		stmt.setString(3, fileName);
		stmt.setString(4, content);
		stmt.executeUpdate();
		closeDB();
	}
	
	private void closeDB()throws Exception{
		dbcon.close();
	}
	
	private void initDB() throws Exception{
		try {
			dbcon = MyConnection.getDatabaseConnection();
			
		} catch (Exception err) {
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	
	private int getNewSchemaId()throws Exception{
		PreparedStatement stmt;
		int newSchemaId = 1;
		// Get schema id
		try {
			stmt = dbcon.prepareStatement("SELECT MAX(schema_id) as schemaId from xdata_schemainfo");
			ResultSet rs = stmt.executeQuery();
	
			if (rs.next()) {
				newSchemaId = rs.getInt("schemaId") + 1;
			}
		} catch (Exception err) {
			err.printStackTrace();
			throw new Exception(err);
		}
		stmt.close();
		return newSchemaId;
	}
	
	/*private void insertIntoSchemaInfo(String courseId,int newSchemaId, String fileName, String file )throws Exception{
		PreparedStatement stmt;
		stmt = dbcon.prepareStatement("Insert into xdata_schemainfo values(?, ?, ?, ? )");
		stmt.setString(1, courseId);
		stmt.setInt(2, newSchemaId);
		stmt.setString(3, fileName);
		stmt.setString(4, file);
		stmt.executeUpdate();
	}*/
	
}
