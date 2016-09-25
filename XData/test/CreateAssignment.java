package test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import util.MyConnection;

public class CreateAssignment {
	
	
	private String jdbcURL = "jdbc:postgresql://localhost:5432/xdata";
	
	public CreateAssignment(){
		
	}
	 
	private String getCurrentDate(){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		   //get current date time with Date()
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	public void createAssignment(String courseId, int assId, int schemaId,int connId)throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("Insert into xdata_assignment values(?,?,?,?,?,?,?,?,?,?,?)");
			stmt.setString(1, courseId);
			stmt.setInt(2, assId);
			stmt.setString(3,"NC");
			stmt.setTime(4, null);
			stmt.setTime(5, null);
			stmt.setBoolean(6, false);
			stmt.setInt(7,connId);
			stmt.setInt(8,schemaId);
			stmt.setString(9,"AutoTesting");
			stmt.setString(10,"[13]");
			stmt.setBoolean(11,false);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	private void clearAssignment(int assId)throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("delete from xdata_assignment where assignment_id=?");
			stmt.setInt(1, assId);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	/*
	 * 	Type	Not Null	Default	Constraints	Actions	Comment
assignment_id	integer	
NOT NULL
[pk]	Browse	Alter	Privileges	Drop	
question_id	integer	
NOT NULL
[pk]	Browse	Alter	Privileges	Drop	
sql	text	
Browse	Alter	Privileges	Drop	
status	character varying(4)	
Browse	Alter	Privileges	Drop	
course_id	character varying(20)	
NOT NULL
[pk]	Browse	Alter	Privileges	Drop	
query_id	integer	
NOT NULL
[pk]	Browse	Alter	Privileges	Drop	
marks
	 */ 
	public void addInstructorQuery(String courseId, int qryid,int quesId, String sql, int assId)throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			sql = sql.replace(";", " ");
			PreparedStatement stmt = dbcon.prepareStatement("Insert into xdata_instructor_query(course_id,assignment_id,question_id,sql,query_id,evaluationstatus,marks) values(?,?, ?, ?, ?,?,?)");
			stmt.setString(1, courseId);
			stmt.setInt(2, assId);
			stmt.setInt(3, quesId);
			stmt.setString(4,sql.trim());
			stmt.setInt(5, qryid);
			stmt.setBoolean(6, false);
			stmt.setInt(7, 100);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			err.printStackTrace();
			throw new Exception(err);
		}
		addQInfoEntry(courseId, assId, qryid, quesId, 100,sql);
	}
	
	
	
	/*
	 * Add database connection for this course
	 * */
	public int addDBConnection(String courseId)throws Exception{
	try {
		Connection dbcon = MyConnection.getDatabaseConnection();
		int connId=0;
		try(PreparedStatement stmt  = dbcon
				.prepareStatement("INSERT INTO xdata_database_connection VALUES (?,DEFAULT,?,?,?,?,?,?,?,?,?)")){
	 
			stmt.setString(1, courseId);
			stmt.setString(2, "localConnection");
			stmt.setString(3, "01"); 
			//stmt.setString(4, "jdbc:postgresql://10.129.22.35:5432/xdata");
			stmt.setString(4, jdbcURL);
			stmt.setString(5, "testing1"); 
			stmt.setString(6, "testing1");
			stmt.setString(7, "testing2");
			stmt.setString(8, "testing2");
			stmt.setString(9, "xdata");
			stmt.setString(10,"localhost:5432");
			stmt.executeUpdate();
			stmt.close();
		}
		try(PreparedStatement stmt1  = dbcon
				.prepareStatement("select max(connection_id) from xdata_database_connection where course_id=?")){
			stmt1.setString(1, courseId);
			ResultSet rset = stmt1.executeQuery();
			rset.next();
			connId = rset.getInt(1);
			stmt1.close();
		}
		dbcon.close();
		return connId;
	} catch (Exception err) {
		err.printStackTrace();
		throw new Exception(err);
	}
	}
			
	//	course_id	assignment_id	question_id	querytext	correctquery	totalmarks	learningmode	
	//ignoreduplicates	matchallqueries	query_id	optionalschemaid	orderindependent
	private void addQInfoEntry(String course_id,int assId,int quesId,int query_id,int totalmarks,String corr_query)throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("Insert into xdata_qinfo (course_id,assignment_id,question_id,query_id,totalmarks,correctquery,matchallqueries,orderindependent ) values(?,?, ?, ?, ?,?,?,?)");
			stmt.setString(1, course_id);
			stmt.setInt(2, assId);
			stmt.setInt(3, quesId);
			stmt.setInt(4,query_id);
			stmt.setInt(5,totalmarks );
			stmt.setString(6, corr_query);
			stmt.setBoolean(7,false);
			stmt.setBoolean(8,true);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	private void clearInstructorQuery(int assId) throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("delete from xdata_instructor_query where assignment_id=?");
			stmt.setInt(1, assId);
			stmt.executeUpdate();
				
			//Clear Qinfo table also
			PreparedStatement stmt1 = dbcon.prepareStatement("delete from xdata_qinfo where assignment_id=?");
			stmt1.setInt(1, assId);
			stmt1.executeUpdate();
			
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	/*
	 * 	Type	Not Null	Default	Constraints	Actions	Comment
dbid	text	
Browse	Alter	Privileges	Drop	
queryid	text	
NOT NULL
Browse	Alter	Privileges	Drop	
rollnum	text	
NOT NULL
[pk]	Browse	Alter	Privileges	Drop	
querystring	text	
NOT NULL
Browse	Alter	Privileges	Drop	
tajudgement	boolean	
Browse	Alter	Privileges	Drop	
verifiedcorrect	boolean	
Browse	Alter	Privileges	Drop	
assignment_id	integer	
NOT NULL
[pk]	Browse	Alter	Privileges	Drop	
question_id	integer	
NOT NULL
[pk]	Browse	Alter	Privileges	Drop	
result	text	
Browse	Alter	Privileges	Drop	
course_id	character varying(20
	 *
	 */
	public void addMutants(int assId, int qid, String rollnum, String qstr, Boolean tajudge,String course_id) throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("Insert into xdata_student_queries(assignment_id,question_id,querystring,rollnum,tajudgement,course_id,queryid) values(?, ?, ?, ?, ?, ?,?)");
			stmt.setInt(1, assId);
			stmt.setInt(2, qid);
			stmt.setString(3,qstr);
			stmt.setString(4, rollnum);
			stmt.setBoolean(5,tajudge);
			stmt.setString(6,course_id);
			stmt.setInt(7, qid);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	private void clearMutants(int assId)throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("delete from xdata_student_queries where assignment_id=?");
			stmt.setInt(1, assId);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
		
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	
	private void clearQInfo(int assId)throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("delete from xdata_qinfo where assignment_id=?");
			stmt.setInt(1, assId);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
		
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	
	/*
	 * Generates dataset for the row in query table with given assignement id and query id
	 */
	public void generateDataset(int assId,int qId,String filePath)throws Exception{
		testDataGen.GenerateDataset_new genData = new testDataGen.GenerateDataset_new(filePath);
		//now getting the query
		Connection dbcon = MyConnection.getDatabaseConnection();
		PreparedStatement stmt = dbcon.prepareStatement("select sql from xdata_instructor_query where assignment_id=? and question_id=?");
		stmt.setInt(1, assId);
		stmt.setInt(2, qId);
		ResultSet rs=stmt.executeQuery();
		rs.next();
		String qrySql = rs.getString(1);
		genData.generateDatasetForQuery(assId,qId,qId,"AutomatedTesting", "true", qrySql,"");
	}
	
	/*private void clearQueryInfo(String queryId)throws Exception{
		try {
			Connection dbcon = (new DatabaseConnection()).dbConnection();
			PreparedStatement stmt = dbcon.prepareStatement("delete from queryinfo where query_id like ?");
			stmt.setInt(1, Integer.valueOf(queryId));
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			err.printStackTrace();
			throw new Exception(err);
		}
	}*/
	private void clearDataSets()throws Exception{
		try {
			Connection dbcon = MyConnection.getDatabaseConnection();
			PreparedStatement stmt = dbcon.prepareStatement("delete from  xdata_datasetvalue");
			//stmt.setString(1, queryId);
			stmt.executeUpdate();
			stmt.close();
			dbcon.close();
		} catch (Exception err) {
			err.printStackTrace();
			throw new Exception(err);
		}
	}
	public void clearDB(int ass)throws Exception{
		clearAssignment(ass);
		clearInstructorQuery(ass);
		clearMutants(ass);
		clearQInfo(ass);
		clearDataSets();
	//	clearQueryInfo("Q"+qid+"A"+ass);
		//clearDataSets("Q"+qid+"A"+ass);
	}
}
