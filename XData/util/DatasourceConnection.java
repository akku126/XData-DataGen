package util;

import java.sql.Connection;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
 
public abstract class DatasourceConnection {
 
    private DataSource datasource = null;
    private Object lock = new Object();
 
    DatasourceConnection() {
    }
  
    public Connection getConnection() {
        try {
        	synchronized(lock){
	            if (datasource == null) {
	                initDatasource();
	            }
        	}
        	
            return datasource.getConnection();
        } catch (Exception e) {
            return null;
        }
    }
    
    public abstract void setDatasourceDetails(PoolProperties poolProp);
    
    public void initDatasource() {
 
        PoolProperties p = new PoolProperties();
 
        setDatasourceDetails(p);
 
        p.setJmxEnabled(true);
        p.setTestWhileIdle(false);
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1");
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);

		
        p.setMaxActive(200);
		p.setMaxIdle(40);
		p.setInitialSize(40);
		p.setMaxWait(10000);
		
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
 
    }
 
    public void closeDatasource() {
        datasource.close();
    }
}
