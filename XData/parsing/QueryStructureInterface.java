/**
 * 
 */
package parsing;

import java.util.Vector;
import parsing.ForeignKey;
import util.TableMap;

/**
 * @author shree
 *
 */
public interface QueryStructureInterface {
		
		public TableMap getTableMap();
		public void setTableMap(TableMap tableMap);
		public void buildQueryStructureJSQL(String queryString) 
				throws Exception;
		
		public Vector<ForeignKey> getForeignKeyVectorModified();
		public void setForeignKeyVectorModified(
				Vector<ForeignKey> foreignKeyVectorModified);
		
		public String toString();	
	}

