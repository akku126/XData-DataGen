/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.*;


/**
 *
 * @author Bhanu Pratap Gupta
 */


public class TableMap implements Serializable{
    
	private static Logger logger = Logger.getLogger(TableMap.class.getName());
	private static final long serialVersionUID = 4802766539372778676L;
	private static TableMap instance = null;
    private Map<String,Table> tables = null;
    private static Map<Integer,TableMap>  schemaTableMap = null;
    private static Map<String,Table>  indexMap = new HashMap<String,Table>();
    public Map<String, Table> getTables() {
		return tables;
	}
    
	public void setTables(Map<String, Table> tables) {
		this.tables = tables;
	}

	private Vector<Table> topSortedTables = null;    
    private String database;
    private String schema;
    
    private transient Connection conn = null;
    public Graph<Table,ForeignKey> foreignKeyGraph = null;
    
    public static TableMap getInstances(Connection dbConn,int schemaId){
    	
    	if(schemaTableMap == null){
        	instance = new TableMap(dbConn);            
        	instance.createTableMap();    
        	schemaTableMap = new HashMap<Integer, TableMap>();
    		schemaTableMap.put(schemaId,instance);
    	}
    	else if(schemaTableMap != null && schemaTableMap.size() > 0){
	    	TableMap t = schemaTableMap.get(schemaId);
	    	
	    	if(t !=null && TableMap.instance != null){
	    		return TableMap.instance;
	    	}
	    	else{
    			 instance = new TableMap(dbConn);            
    	         instance.createTableMap();
    	         schemaTableMap.put(schemaId, instance);
    		 }
    	}  
        return instance; 
    }
    
    private TableMap(Connection dbConn){
        tables = new LinkedHashMap<String,Table>();
        conn = dbConn;
        
        try {
			database = dbConn.getCatalog();
		} catch (SQLException e1) {
			logger.log(Level.SEVERE,e1.getMessage(),e1);
			e1.printStackTrace();
		}
        
        try {
			schema = dbConn.getMetaData().getUserName();
		} catch (SQLException e) {
			logger.log(Level.SEVERE,e.getMessage(),e);
			e.printStackTrace();
		};
    }
    
    public void createTableMap() {  
    	
        try {
        
            PreparedStatement s = this.conn.prepareStatement("CREATE TEMPORARY TABLE TEMPX()");
            s.executeUpdate();
            
        	DatabaseMetaData meta = this.conn.getMetaData();
            
            ResultSet rs = null, rs1=null;
            
            // Note: Table types listed below are specific to PostgreSQL and could be different for other databases: Oracle, MySql etc
            // One way to work around is to remove the filter whole together which would mean all tables including the system tables will be returned.
            String tableFilter[] = {"TEMPORARY TABLE"};
                       
            rs = meta.getTables(database, "", "%", tableFilter);
            while (rs.next()) {                          
               String tableName = rs.getString("TABLE_NAME").toUpperCase(); 
               if(tables.get(tableName)==null){
            	   Table table = new Table(tableName);
                   tables.put(tableName, table );
                   indexMap.put(tableName, table );
               }               
            }
            
            //System.out.print(tables);
            
            rs = null;    
            rs = meta.getColumns(database, "", "%","%");           
            while (rs.next()) {
               String tableName = rs.getString("TABLE_NAME").toUpperCase();
               Table table = getTable(tableName);
               if(table==null)
                   continue;
               String columnName = rs.getString("COLUMN_NAME").toUpperCase();

               Column col = new Column(columnName,table);
              //  logger.log(Level.INFO,"Table Values in TableMap = "+table+" and column = "+columnName);
               col.setDataType(rs.getInt("DATA_TYPE"));
               if(col.getDataType()==Types.NUMERIC){ 
            	   
            	   String query = "SELECT " + columnName + " FROM " + table;
                   PreparedStatement statement = this.conn.prepareStatement(query);
                   
                   ResultSet resultSet = statement.executeQuery();
                   
                   ResultSetMetaData metadata = resultSet.getMetaData();
                   int precision = metadata.getPrecision(1);
                   int scale = metadata.getScale(1);
                   
                   col.setPrecision(precision);
                   col.setScale(scale);
            	   
            	   if(rs.getInt("DECIMAL_DIGITS")==0)
            		   col.setDataType(Types.INTEGER);
               }
               if(col.getDataType()==Types.CHAR)
                   col.setDataType(Types.VARCHAR);
               col.setColumnSize(rs.getInt("COLUMN_SIZE"));                              
               col.setIsNullable(rs.getString("IS_NULLABLE").equals("YES"));
               if(rs.getString("COLUMN_DEF")!=null && rs.getString("COLUMN_DEF").startsWith("nextval")){
                   col.setIsAutoIncement(true);                   
               }
               table.addColumn(col);
            }
              
            rs = null;
            rs = meta.getTables(database, "", "%", tableFilter);
            while (rs.next()) {                           
              String tableName = rs.getString("TABLE_NAME"); 
              rs1 = null;
              rs1 = meta.getPrimaryKeys(database, null, tableName);
            
           	  int size = rs1.getFetchSize();
           	  while(rs1.next()){
           		Table table = getTable(tableName.toUpperCase());
           		indexMap.remove(tableName.toUpperCase());
           		String columnName = rs1.getString("COLUMN_NAME").toUpperCase();
           		Column col = table.getColumn(columnName);
           		table.addColumnInPrimaryKey(col);
           		if(size == 1)
           			col.setIsUnique(true);
           	  }            
            }
            //For tables without primary keys, check if uniques indexes exists
            //We assume them as primary keys in the absence of primary key
            Iterator it = indexMap.keySet().iterator();
            while(it.hasNext()){
            	//get tablename from keyset using iterator
            	boolean listUniqueIndex = true;
            	String tname = (String)it.next();
                ResultSet rset = meta.getIndexInfo(database,null, tname.toLowerCase(),listUniqueIndex, true);
                while(rset.next()) { 
                    String indexName = rset.getString("INDEX_NAME");
                    //String table = rset.getString("TABLE_NAME");
                   // String schema = rset.getString("TABLE_SCHEM");
                    String columnName = rset.getString("COLUMN_NAME").toUpperCase();
                    if(indexName == null) {
                        continue;
                    }
                    Table table = getTable(tname.toUpperCase());
                    Column col = table.getColumn(columnName);
               		table.addColumnInPrimaryKey(col);
               		
                    System.out.println("*******************UNIQUE INDEX DETAILS*********************");
                    System.out.println("Table: " + schema + "." + table);
                    System.out.println("Index Name: " + indexName);
                    System.out.println("Column Name: " + columnName);
                    System.out.println("****************************************");
                }
            }
            foreignKeyGraph = new Graph<Table,ForeignKey>(true);
            // logger.log(Level.INFO,"Create Foreign Key Graph");
            rs = meta.getExportedKeys(conn.getCatalog(), "", null);
            while(rs.next()){
            	
            	String fkName = rs.getString("FK_NAME");
            	String fkTableName = rs.getString("FKTABLE_NAME").toUpperCase();
                String fkColumnName = rs.getString("FKCOLUMN_NAME").toUpperCase();
                Table fkTable = getTable(fkTableName);
                Column fkColumn = fkTable.getColumn(fkColumnName);
                if(fkColumnName.equals(""))
                	continue;
                int seq_no = rs.getInt("KEY_SEQ");
                
                String pkTableName = rs.getString("PKTABLE_NAME").toUpperCase();
                String pkColumnName = rs.getString("PKCOLUMN_NAME").toUpperCase();
                
                Table pkTable = getTable(pkTableName);
                pkTable.setIsExportedTable(true);
                Column pkColumn = pkTable.getColumn(pkColumnName);
                pkColumn.setIsUnique(true);
                fkColumn.setReferenceTableName(pkTableName);
                fkColumn.setReferenceColumn(pkColumn);
                                
                ForeignKey fk = fkTable.getForeignKey(fkName);
                fk.addFKeyColumn(fkColumn, seq_no);
                fk.addReferenceKeyColumn(pkColumn, seq_no);
                fk.setReferenceTable(pkTable);
                fkTable.addForeignKey(fk);
                // logger.log(Level.INFO,"Create Foreign Key Graph  For first RS = " + fkColumn + " - ref Table Name = "+pkTableName);
             
            }
            
           for(String tableName : tables.keySet()){
            	Table table = tables.get(tableName);
            	if(table.hasForeignKey()){
            		for(String fKeyName : table.getForeignKeys().keySet()){
            			ForeignKey fKey = table.getForeignKeys().get(fKeyName);
            			foreignKeyGraph.add(fKey.getReferenceTable(), table, fKey);
            		}	
            	}
            }
            
            topSortedTables = foreignKeyGraph.topSort();
            for(String tableName : tables.keySet()){
            	Table table = tables.get(tableName);
            	if(!topSortedTables.contains(table))
            		topSortedTables.add(table);
            }          
           
         //conn.close(); 
        }catch(Exception e){
        	logger.log(Level.SEVERE, "TableMap not created", e);
        	//e.printStackTrace();
        }
    }
    
    public Vector<Table> getAllTablesInTopSorted(){                
        return topSortedTables;        
    }
    
    public Table getTable(String tableName){                
        return (tables.get(tableName));
    }
    
    public Graph<Table,ForeignKey> getForeignKeyGraph(){
    	return foreignKeyGraph;
    }
    
    public Connection getConnection(){
    	return this.conn;
    }
    
    public static void main(String args[]){
        try{
        	TableMap tm = TableMap.getInstances(null,0);
        	 logger.log(Level.INFO,"Top Sorted Tables : "+tm.getAllTablesInTopSorted());
            
            /*Table table = tm.getTable("ROLLHIST");
            for(String columnName : table.getColumns().keySet()){
            	Column col = table.getColumns().get(columnName);
            	 logger.log(Level.INFO,col.getColumnName()+" is Nullable : "+col.isNullable());
            }*/
        }catch(Exception e){
        	logger.log(Level.SEVERE, "Message", e);
            //e.printStackTrace();;
        }
     
    }

}