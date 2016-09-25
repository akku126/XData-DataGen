package util;

import org.apache.tomcat.jdbc.pool.PoolProperties;
 
public class GraderDatasource extends GraderDatasourceConn{

	@Override
	public void setDataSourceDetailsTemp(PoolProperties poolProp,DatabaseConnectionDetails dbDetails){
		
		String url="";
    	String dbType= dbDetails.getDbType(dbDetails.getDbType());
    	String dbName = dbDetails.getDbName();
    	
		if(dbType.equals("Oracle")){			
			setDatasourceDetailsOracle( poolProp, dbDetails);
			
		}else if(dbType.equals("MySql")){
			
			setDatasourceDetailsMySQL( poolProp, dbDetails);
		}
		else if(dbType.equals("PostgreSQL")){
			setDatasourceDetailsPostgresql( poolProp, dbDetails);
		}
		else if(dbType.equals("db2")){
			setDatasourceDetailsDB2( poolProp, dbDetails);
		}
		else if(dbType.equals("MSSQL")){
			setDatasourceDetailsMSSQL( poolProp, dbDetails);
		}
		
	}
    public void setDatasourceDetailsPostgresql(PoolProperties poolProp,DatabaseConnectionDetails dbDetails){
    	poolProp.setUrl("jdbc:postgresql://"+dbDetails.getJdbc_Url()+"/"+dbDetails.getDbName());
    	poolProp.setDriverClassName("org.postgresql.Driver");
    	poolProp.setUsername(Configuration.existingDatabaseUser);
    	poolProp.setPassword(Configuration.existingDatabaseUserPasswd);
    }
    
	 public void setDatasourceDetailsOracle(PoolProperties poolProp,DatabaseConnectionDetails dbDetails){
	    	poolProp.setUrl("jdbc:oracle:thin:@"+dbDetails.getJdbc_Url()+":"+dbDetails.getDbName());
	    	poolProp.setDriverClassName("org.postgresql.Driver");
	    	poolProp.setUsername(Configuration.existingDatabaseUser);
	    	poolProp.setPassword(Configuration.existingDatabaseUserPasswd);
	    }
	    public void setDatasourceDetailsMSSQL(PoolProperties poolProp,DatabaseConnectionDetails dbDetails){
	    	poolProp.setUrl("jdbc:sqlserver://" + dbDetails.getJdbc_Url() +
				   ";databaseName=" + dbDetails.getDbName() + ";user=" + dbDetails.getDbUser() + ";password=" + dbDetails.getDbPwd());
	    	poolProp.setDriverClassName("org.postgresql.Driver");
	    	poolProp.setUsername(Configuration.existingDatabaseUser);
	    	poolProp.setPassword(Configuration.existingDatabaseUserPasswd);
	    }
	    public void setDatasourceDetailsMySQL(PoolProperties poolProp,DatabaseConnectionDetails dbDetails){
	    	poolProp.setUrl("jdbc:mysql://"+dbDetails.getJdbc_Url()+"/"+dbDetails.getDbName());
	    	poolProp.setDriverClassName("org.postgresql.Driver");
	    	poolProp.setUsername(Configuration.existingDatabaseUser);
	    	poolProp.setPassword(Configuration.existingDatabaseUserPasswd);
	    }
	    public void setDatasourceDetailsDB2(PoolProperties poolProp,DatabaseConnectionDetails dbDetails){
	    	/*poolProp.setUrl("jdbc:postgresql://" + Configuration.databaseIP + ":" + Configuration.databasePort + "/"+Configuration.databaseName);
	    	poolProp.setDriverClassName("org.postgresql.Driver");
	    	poolProp.setUsername(Configuration.existingDatabaseUser);
	    	poolProp.setPassword(Configuration.existingDatabaseUserPasswd);
	    	*/
	    }
	    
}