package util;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

public abstract class TesterDataSourceConn {
	
	private DataSource datasource = null;
    private Object lock = new Object();
    private static Map<String, DataSource> connMapTester = new HashMap<String,DataSource>();
   
    TesterDataSourceConn() {
    }
  
    public Connection getConnection(DatabaseConnectionDetails dbDetails) {
        try {
        
        	Connection conn = null;
        	String url = this.getConnectionUrl(dbDetails);;
        	
        	synchronized(lock){
        		//Initial request for a new DB type
        		if(connMapTester.isEmpty() || !(connMapTester.containsKey(url))){
        				//Add to connPerDB and connTypeDBMap
        				datasource = initDatasource(dbDetails);
        				connMapTester.put(url,datasource);	
        				conn = datasource.getConnection();
        			}
        		//Else return an existing connection from pool
        		else{
        			//get the existing pool
        			DataSource connToDataSource = connMapTester.get(url);
        			//get Connection from existing pool
        			conn = connToDataSource.getConnection();
        			
        		}
        	}
            //return datasource.getConnection();
            return conn;
        }catch (Exception e) {
            return null;
        }
    }
    
    public abstract void setDataSourceDetailsTemp(PoolProperties poolProp,DatabaseConnectionDetails dbDetails);
    
    public DataSource initDatasource(DatabaseConnectionDetails dbDetails) {
 
        PoolProperties p = new PoolProperties();
 
        setDataSourceDetailsTemp(p,dbDetails);
 
        p.setJmxEnabled(true);
        p.setTestWhileIdle(false);
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1");
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
 
        p.setMaxActive(500);
		p.setMaxIdle(40);
		p.setInitialSize(2);
		p.setMaxWait(30000);
	
		// In seconds
		p.setRemoveAbandonedTimeout(1200);
		
		p.setMinEvictableIdleTimeMillis(30000);
		p.setMinIdle(10);
 
        p.setLogAbandoned(true);       
        p.setRemoveAbandoned(true);
 
        p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
                + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
 
        datasource = new DataSource();
        datasource.setPoolProperties(p);
        return datasource;
    }
 
    public String getConnectionUrl(DatabaseConnectionDetails dbDetails){
    	String url="";
    	String dbType= dbDetails.getDbType(dbDetails.getDbType());
    	String dbName = dbDetails.getDbName();
    	
		if(dbType.equals("Oracle")){			
			url="jdbc:oracle:thin:@"+dbDetails.getJdbc_Url()+":"+dbDetails.getDbName();
			
		}else if(dbType.equals("MySql")){
			
			url="jdbc:mysql://"+dbDetails.getJdbc_Url()+"/"+dbDetails.getDbName();
		}
		else if(dbType.equals("PostgreSQL")){
			url="jdbc:postgresql://"+dbDetails.getJdbc_Url()+"/"+dbDetails.getDbName();
		}
		else if(dbType.equals("db2")){
			 
		}
		else if(dbType.equals("MSSQL")){
			url = "jdbc:sqlserver://" + dbDetails.getJdbc_Url() +
					   ";databaseName=" + dbName + ";user=" + dbDetails.getDbName() + ";password=" + dbDetails.getDbPwd() + ";";
		}
		return url;
    }
    
    public void closeDatasource() {
        datasource.close();
    }
}
