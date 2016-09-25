/**
 * 
 */
package partialMarking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mathew
 *
 */
public class Util {
	

	

	public static void copyDatabaseTables(Connection srcConn, Connection tarConn, String tableName) throws Exception{
		String selQuery="SELECT * FROM "+tableName;
		PreparedStatement selStmt=srcConn.prepareStatement(selQuery);
		ResultSet tableValues=selStmt.executeQuery();
		while(tableValues.next()){
			String insertHead="INSERT INTO "+tableName+"(";
			String insertTail=" VALUES(";
			ResultSetMetaData metaData=tableValues.getMetaData();
			for(int i=1;i<=metaData.getColumnCount();i++){
				if(i==1){
					insertHead+=metaData.getColumnName(i);
					insertTail+="?";
				}
				else if(metaData.getColumnName(i).contains("evaluationstatus")||metaData.getColumnName(i).contains("learning_mode"))
					continue;

				else{
					insertHead+=","+metaData.getColumnName(i);
					insertTail+=",?";
				}
			}
			String insertQuery= insertHead+")"+insertTail+")";		
//			System.out.println(insertQuery);
			PreparedStatement insStmt=tarConn.prepareStatement(insertQuery);
			int k=0;
			for(int j=1;j<=metaData.getColumnCount();j++){
				if(metaData.getColumnName(j).contains("evaluationstatus")||metaData.getColumnName(j).contains("learning_mode"))
					continue;
				else if((metaData.getColumnType(j)==Types.NUMERIC)||(metaData.getColumnType(j)==Types.INTEGER)){
					insStmt.setInt(++k, tableValues.getInt(j));
				}
				else if(metaData.getColumnType(j)==Types.BOOLEAN){
					insStmt.setBoolean(++k, tableValues.getBoolean(j));
				}
				else if(metaData.getColumnType(j)==Types.DECIMAL||metaData.getColumnType(j)==Types.FLOAT){
					insStmt.setDouble(++k, tableValues.getDouble(j));
				}
				else if(metaData.getColumnType(j)==Types.TIMESTAMP){
					insStmt.setTimestamp(++k, tableValues.getTimestamp(j));
				}
				else if(metaData.getColumnType(j)==Types.TIME){
					insStmt.setTime(++k, tableValues.getTime(j));
				}
				else if(metaData.getColumnType(j)==Types.VARCHAR){
					insStmt.setString(++k, tableValues.getString(j));
				}
				else if(metaData.getColumnType(j)==Types.DATE)
				{
					insStmt.setDate(++k, tableValues.getDate(j));
				}
//				else if(metaData.getColumnLabel(j)=="learning_mode") {
//					if(tableValues.getBoolean(j)==false)
//						insStmt.setBoolean(j, false);
//					else
//						insStmt.setBoolean(j, true);
//				}
				else{
					insStmt.setString(++k, tableValues.getString(j));
				}
			}
			System.out.println(insStmt.toString());
			try{
			insStmt.executeUpdate();
			}
			catch(Exception e){
				System.out.println(insStmt.toString());
				System.out.println(e.getMessage());
			}
		}
	}
	
	public static void copyDatabaseTables(Connection srcConn, Connection tarConn) throws Exception{
		String srcQuery="SELECT table_name from information_schema.tables where table_schema='testing1'";
		PreparedStatement srcStmt= srcConn.prepareStatement(srcQuery);
		PreparedStatement tarStmt= tarConn.prepareStatement(srcQuery);
		ResultSet srcRes=srcStmt.executeQuery();
		ResultSet tarRes=tarStmt.executeQuery();
        
		List<String> srcTables=new ArrayList<String>();
		while(srcRes.next()){
			srcTables.add(srcRes.getString(1));
		}
		List<String> tarTables=new ArrayList<String>();
		while(tarRes.next()){
			tarTables.add(tarRes.getString(1));
		}
		for(String tableName:srcTables){
			if(!tarTables.contains(tableName))
				continue;
			String selQuery="SELECT * FROM "+tableName;
			PreparedStatement selStmt=srcConn.prepareStatement(selQuery);
			ResultSet tableValues=selStmt.executeQuery();
			while(tableValues.next()){
				String insertHead="INSERT INTO "+tableName+"(";
				String insertTail=" VALUES(";
				ResultSetMetaData metaData=tableValues.getMetaData();
				for(int i=1;i<metaData.getColumnCount();i++){
					if(i==1){
						insertHead+=metaData.getColumnName(i);
						insertTail+="?";
					}
					else{
						insertHead+=","+metaData.getColumnName(i);
						insertTail+=",?";
					}
				}
				String insertQuery= insertHead+")"+insertTail+")";				
				PreparedStatement insStmt=tarConn.prepareStatement(insertQuery);
				for(int j=1;j<metaData.getColumnCount();j++){
					if(metaData.getColumnType(j)==Types.NUMERIC||metaData.getColumnType(j)==Types.INTEGER){
						insStmt.setInt(j, tableValues.getInt(j));
					}
					else if(metaData.getColumnType(j)==Types.BOOLEAN){
						insStmt.setBoolean(j, tableValues.getBoolean(j));
					}
					else if(metaData.getColumnType(j)==Types.DECIMAL||metaData.getColumnType(j)==Types.FLOAT){
						insStmt.setDouble(j, tableValues.getDouble(j));
					}
					else if(metaData.getColumnType(j)==Types.TIMESTAMP){
						insStmt.setTimestamp(j, tableValues.getTimestamp(j));
					}
					else if(metaData.getColumnType(j)==Types.TIME){
						insStmt.setTime(j, tableValues.getTime(j));
					}
					else if(metaData.getColumnType(j)==Types.VARCHAR){
						insStmt.setString(j, tableValues.getString(j));
					}
					else if(metaData.getColumnType(j)==Types.DATE)
					{
						insStmt.setDate(j, tableValues.getDate(j));
					}
//					else if(metaData.getColumnLabel(j)=="learning_mode") {
//						if(tableValues.getBoolean(j)==false)
//							insStmt.setBoolean(j, false);
//						else
//							insStmt.setBoolean(j, true);
//					}
					else{
						insStmt.setString(j, tableValues.getString(j));
					}
				}
				System.out.println(insStmt.toString());
				try{
				insStmt.executeUpdate();
				}
				catch(Exception e){
					System.out.println(e.getMessage());
				}
			}
		}
	}


	/**
	 * 
	 */
	public Util() {
		// TODO Auto-generated constructor stub
	}

}
