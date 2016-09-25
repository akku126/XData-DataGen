package util;

import org.apache.tomcat.jdbc.pool.PoolProperties;

public class DataSource  extends DatasourceConnection{

	@Override
    public void setDatasourceDetails(PoolProperties poolProp){
    	poolProp.setUrl("jdbc:postgresql://" + Configuration.databaseIP + ":" + Configuration.databasePort + "/"+Configuration.databaseName);
    	poolProp.setDriverClassName("org.postgresql.Driver");
    	poolProp.setUsername(Configuration.existingDatabaseUser);
    	poolProp.setPassword(Configuration.existingDatabaseUserPasswd);
    }
}
