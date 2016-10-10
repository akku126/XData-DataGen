package partialMarking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import parsing.QueryParser;
import testDataGen.GenerateCVC1;
import util.MyConnection;

public class QueryDetails {
	
	// Start Region - Private members
	
	private QueryParser parser;
	
	private GenerateCVC1 data;
		
	// End Region - Private members
	
	// Start Region - Public members
	
	public QueryData OuterQuery;
	public String query;

	// End Region - Public members
	
	public QueryParser getParser(){
		return this.parser;
	}
	
	public void setQueryParser(QueryParser qp){
		this.parser = qp;
	}
	
	public GenerateCVC1 getData(){
		return this.data;
	}
	
	public void setData(GenerateCVC1 data){
		this.data = data;
	}
	
	public QueryDetails(){
	}
	
	public int InitializeInstructorQuery(int assignmentId, int questionId, int queryId) throws Exception {		
		String qry = "select * from xdata_instructor_query where assignment_id = ? and question_id = ? and query_id = ?";		
		//Connection conn = MyConnection.getExistingDatabaseConnection();
		try(Connection conn = MyConnection.getDatabaseConnection()){
			try(PreparedStatement pstmt = conn.prepareStatement(qry)){
				pstmt.setInt(1, assignmentId);
				pstmt.setInt(2, questionId);
				pstmt.setInt(3, queryId);
				
				try(ResultSet rs = pstmt.executeQuery()){
					int marks = 0;		
					
					String sqlQuery = null;
					if(rs.next()){
						sqlQuery = rs.getString("sql");
						marks = rs.getInt("marks");
					}
					this.query = sqlQuery;
					this.initialize(assignmentId, questionId, sqlQuery);
					return marks;
				}
			}
			}
			
		
	}
	
	public void InitializeStudentQuery(int aId, int qId, String rollNum, String guestStudentQuery) throws Exception{		
		String qry = "select * from xdata_student_queries where assignment_id = ? and question_id = ? and rollnum = ?";		
		if(guestStudentQuery == null){
		//Connection conn = MyConnection.getExistingDatabaseConnection();
		try(Connection conn = MyConnection.getDatabaseConnection()){
			try(PreparedStatement pstmt = conn.prepareStatement(qry)){
				pstmt.setInt(1, aId);
				pstmt.setInt(2, qId);
				pstmt.setString(3, rollNum);
				
				try(ResultSet rs = pstmt.executeQuery()){
					String sqlQuery = null;
					if(rs.next()){	
						sqlQuery = rs.getString("querystring");
					}else{
						sqlQuery = guestStudentQuery;
					}
					this.query = sqlQuery;
					this.initialize(aId, qId, sqlQuery);
		}
		}
		}
	}else{
		String sqlQuery = guestStudentQuery;
		this.query = sqlQuery;
		this.initialize(aId, qId, sqlQuery);
	}
		
	}

	
	public void initialize(int assignmentId, int questionId, String query) throws Exception {
		GenerateCVC1 cvc = new GenerateCVC1();
				
		cvc.initializeConnectionDetails(assignmentId, questionId, 1,"");
		
		cvc.setqParser( new QueryParser(cvc.getTableMap()));
		
		cvc.closeConn();

		/** Parse the query */
		cvc.getqParser().parseQuery("q1", query);

		this.parser = cvc.getqParser();
						
		/**Initialize the query details to the object*/
		cvc.initializeQueryDetails(parser);
		
		this.data = cvc;
		
		this.OuterQuery = new QueryData(this.parser, this.data.outerBlock, this.data);		
		
		cvc.closeConn();
	}


}
