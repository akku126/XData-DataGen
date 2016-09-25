package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHelper {
	
	private static Logger logger = Logger.getLogger(DatabaseHelper.class.getName());
	public static void InsertIntoScores(Connection conn, int assignmentId, int questionId, int queryId, String course_id, int maxMarks, String userId, String info, Float marks) throws SQLException{
		//String insertquery="INSERT INTO score VALUES (?,?,?,?,?,?,?,?,?)";
		 
		try{
			/*try(PreparedStatement smt=conn.prepareStatement(insertquery)){
				smt.setString(1, "A" + assignmentId + "Q" + questionId + "S" + queryId);
				smt.setString(2, userId);
				smt.setFloat(3, marks);
				smt.setInt(4, 0);
				smt.setInt(5, assignmentId);
				smt.setInt(6, questionId);
				smt.setInt(7, queryId);
				smt.setString(8, info);
				smt.setString(9,course_id);
				smt.executeUpdate();
				smt.close();
			}*/
			
			String updateScoreQuery = "update xdata_student_queries set score = ?,markinfo=?,max_marks=? where assignment_id=? and question_id=? and rollnum=?";
			try(PreparedStatement ps = conn.prepareStatement(updateScoreQuery)){
			ps.setFloat(1, marks);
			ps.setString(2, info);
			ps.setInt(3, maxMarks);

			ps.setFloat(4, assignmentId);
			ps.setInt(5, questionId);
			ps.setString(6, userId);
			ps.executeUpdate();
			}
			
		}catch (SQLException e) {
			 logger.log(Level.SEVERE,"Error in DatabaseHelper.java : InsertIntoScores : \n" + e.getMessage(),e);
			//String update="update score set result = ?, markinfo = ? where assignment_id=? and question_id=? and rollnum=? and course_id=?;";
			 String update="update xdata_student_queries set score = ?, markinfo = ? where assignment_id=? and question_id=? and rollnum=? and course_id=?;";			 
			try(PreparedStatement smt=conn.prepareStatement(update)){
				smt.setFloat(1, marks);
				smt.setString(2, info);
				smt.setInt(3, assignmentId);
				smt.setInt(4, questionId);
				smt.setString(5, userId);	
				smt.setString(6,course_id);
				smt.executeUpdate();
				smt.close();
			}
		}
	}
}
