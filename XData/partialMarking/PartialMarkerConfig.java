package partialMarking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

import com.google.gson.Gson;

import util.MyConnection;

public class PartialMarkerConfig {
	
	private static Logger logger = Logger.getLogger(PartialMarkerConfig.class.getName());
	public int Relation=1;
	
	public int Predicate=1;
	
	public int Projection=1;
	
	public int Joins=1;
	
	public int WhereSubQueries=1;
	
	public int FromSubQueries=1;
	
	public int OuterQuery=1;
	
	public int GroupBy=1;
	
	public int HavingClause=1;
	
	public int SubQConnective=1;
	
	public int Aggregates=1;
	
	public int SetOperators=1;
	
	public int Distinct=1;
	
	public int OrderBy=1;
	
	public void setConfigurationValues(int assignment_id,int question_id, int query_id) throws Exception {
		 
		/*Properties properties=new Properties();
		
		InputStream inputStream = PartialMarkerConfig.class.getResourceAsStream("partialMarker.properties");
		 
		if (inputStream != null) {
			properties.load(inputStream);
		} else {
			throw new FileNotFoundException("property file partial marker config not found in the classpath");
		}*/
		try(Connection conn = MyConnection.getDatabaseConnection()){
			
			try(PreparedStatement stmt = conn.prepareStatement("select * from xdata_instructor_query where assignment_id = ? and question_id = ? and query_id = ?")){
				stmt.setInt(1, assignment_id);
				stmt.setInt(2, question_id);
				stmt.setInt(3, query_id);
				try(ResultSet rs = stmt.executeQuery()){

					if(rs.next()){	
						Gson gson = new Gson();
						Type listType = new TypeToken<PartialMarkParameters>() {}.getType();
						PartialMarkParameters partialMarks = new Gson().fromJson(rs.getString("partialmarkinfo"), listType);
						if(partialMarks!=null){
							Relation = partialMarks.getRelation();
							Predicate = partialMarks.getPredicate();
							Projection =partialMarks.getProjection();
							Joins = partialMarks.getJoins();
							GroupBy = partialMarks.getGroupBy();
							WhereSubQueries = partialMarks.getWhereSubQueries();
							FromSubQueries = partialMarks.getFromSubQueries();
							OuterQuery = partialMarks.getOuterQuery();
							HavingClause = partialMarks.getHavingClause();
							SubQConnective = partialMarks.getSubQConnective();
							Aggregates = partialMarks.getAggregates();
							Distinct = partialMarks.getDistinct();
						}
						else{
							Relation=1;
							Predicate=1;
							Projection=1;
							Joins=1;
							GroupBy=1;
							WhereSubQueries=1;
							FromSubQueries=1;
							OuterQuery=2;
							HavingClause=1;
							SubQConnective=1;
							Aggregates=1;							
							Distinct=1;
							SetOperators=1;
							OrderBy=1;

						}
					}
				}
			}
							
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			//e.printStackTrace();
		}
	}
}
