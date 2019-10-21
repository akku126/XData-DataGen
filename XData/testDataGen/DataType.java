package testDataGen;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataType {
	
	private static Logger logger = Logger.getLogger(DataType.class.getName());
	public String getDataType(String type){
		String datatype = "";
		
//		if(type.trim().equalsIgnoreCase("BIT") || type.trim().equalsIgnoreCase("TINYINT") || type.trim().equalsIgnoreCase("SMALLINT") || type.trim().equalsIgnoreCase("INTEGER") || type.trim().equalsIgnoreCase("BIGINT") || type.trim().equalsIgnoreCase("INT"))
//			datatype = "INT";
//		else if (type.trim().equalsIgnoreCase("bpchar") || )
//			datatype = "INT";
//		else if ()
			
		
		
		return datatype;
	}
	
	public static int getDataType(int type){
		String datatype = "";
		int datatype_int = 0; 
		
		switch(type){
			case -7://BIT = -7;
			case -6://TINYINT = -6;
			case 5://SMALLINT = 5;
			case 4://INTEGER = 4;
			case -5://BIGINT = -5;
				datatype = "INT";
				datatype_int = 1; // in case of INTEGER
				break;
			
			case 6://FLOAT = 6;
			case 7://REAL = 7;
			case 8://DOUBLE = 8;
			case 2://NUMERIC = 2;
			case 3://DECIMAL = 3;
				datatype = "REAL";
				datatype_int = 2; 
				break;
				
			case 1://CHAR = 1;
			case 12://VARCHAR = 12;
			case -1://LONGVARCHAR = -1;
			case -15://NCHAR = -15;
			case -16://LONGNVARCHAR = -16;
			case -9://NVARCHAR = -9;
				datatype = "STRING";
				datatype_int = 3; // in case of STRING
				break;
				
			case 16://BOOLEAN = 16;
				datatype = "BOOLEAN";
				datatype_int = 4;
				break;
				
			case 91://DATE = 91;
				datatype = "DATE";
				datatype_int = 5;
				break;
			case 92://TIME = 92;
				datatype = "TIME";
				datatype_int = 6;
				break;
			case 93://TIMESTAMP = 93;
				datatype = "TIMESTAMP";
				datatype_int = 7;
				break;

//		  BINARY = -2;
//		  VARBINARY = -3;
//		  LONGVARBINARY = -4;
//		  NULL = 0;
//		  OTHER = 1111;
//		  JAVA_OBJECT = 2000;
//		  DISTINCT = 2001;
//		  STRUCT = 2002;
//		  ARRAY = 2003;
//		  BLOB = 2004;
//		  CLOB = 2005;
//		  REF = 2006;
//		  DATALINK = 70;
//		  ROWID = -8;
//		  NCLOB = 2011;
//		  SQLXML = 2009;
		}
		
		//return datatype;
		return datatype_int;
	}
	    
	public static void main(String[] args) throws Exception {
		//Types type = new Types(); 
		DataType d = new DataType();
	        //connection = MyConnection.getExistingDatabaseConnection();
	    	/*try(Connection connection = MyConnection.getDatabaseConnection()){
		        DatabaseMetaData metadata = connection.getMetaData();
		        try(ResultSet resultSet = metadata.getColumns(null, null, "rollhist", null)){
			        while (resultSet.next()) {
			        	String name = resultSet.getString("COLUMN_NAME");
			        	String type = resultSet.getString("TYPE_NAME");
			        	int size = resultSet.getInt("COLUMN_SIZE");
			         
			        }
			        
			       try(PreparedStatement pstmt = connection.prepareStatement("select * from rollhist where studentid = 70626")){
			    	   try(ResultSet rs = pstmt.executeQuery()){
					        ResultSetMetaData rsmd = rs.getMetaData();
					        
					        for(int i=1; i<=rsmd.getColumnCount(); i++)
					        {
					        	int datatype = rsmd.getColumnType(i);
					        	
					        	logger.log(Level.INFO,"datatype = " + datatype + "; our type = " + d.getDataType(datatype) );
					        	
					        } 
			    	   }
			       }
		        }
	   } catch (SQLException e) {
	    	logger.log(Level.SEVERE,e.getMessage(),e);
	        e.printStackTrace();
	    }  */
		
	}
}
