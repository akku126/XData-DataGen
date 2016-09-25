package testDataGen;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Configuration;
import util.MyConnection;

public class Testing {

	private static Logger logger = Logger.getLogger(Testing.class.getName());
	public static void main(String[] args) throws SQLException, Exception {
		PreparedStatement smt;
		String filePath = "4";
		int assignmentId = 8;
		int questionId = 14;

		int rollno=1;
		String qID="";
		String quesDesc="";
		try(Connection conn = MyConnection.getDatabaseConnection()){
			String strFile = Configuration.homeDir+"/Experiments/Assign2/4.csv";
			Scanner sc = null;
			try {
				sc = new Scanner(new File(strFile));
	
			sc.useDelimiter("\\s*:\\s*");
			sc.next();sc.next();sc.next();
			rollno=1;
			qID=sc.next();
			quesDesc=sc.next();
			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE,e.getMessage(),e);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String quesID = null; 
			//System.out.println(quesDesc);
			while(sc.hasNext()){
				quesID=qID;
				if(quesID.startsWith("'extra'")){
					
					Pattern pattern = Pattern.compile("^'extra'.*(Q[0-9]+).*");
					Matcher matcher = pattern.matcher(quesID);
					if(matcher.find()){
						quesID = matcher.group(1);
					}
					//quesID=qID.substring(8);
					quesID = "A8" + quesID;
					filePath = "4/" + quesID;
				}
				//quesID=""+quesID.charAt(2);// + quesID.charAt(3);
				
				String insertquery="INSERT INTO xdata_student_queries VALUES ('d1','"+quesID+"','"+ "R" + rollno+"',";
				//System.out.println("Question Number "+quesID);
				//System.out.println("Question Description is "+quesDesc);
				sc.next();
				sc.next();
				sc.next();
				do{
					String query=sc.next();
					query=query.trim().replaceAll(";", " ");
					// parse the query
					String parsedquery="'";
					for(int j=1;j<query.length()-1;j++){
						if(query.charAt(j)=='\n'){
							parsedquery+=" ";
						}
						else if(query.charAt(j)=='\''){
							parsedquery+="'";
						}else{
							parsedquery+=query.charAt(j);
						}
					}
					parsedquery+="'";
					
					//for oracle
					//insertquery+=parsedquery+",'true',' ')";
					
					//for postgres
					insertquery+=parsedquery+",true,false," + assignmentId + "," + questionId + ");";
					
					//System.out.println("INSERT Query ===> "+insertquery);
					
					try {
						
						smt = conn.prepareStatement("select * from xdata_users where internal_user_id = ?");
						smt.setString(1, "R" + rollno);
						
						ResultSet rs = smt.executeQuery();
						if(!rs.next()){
							smt = conn.prepareStatement("insert into xdata_users values(?,?,?,?,?,?,?)");
							smt.setString(1, "R" + rollno);
							smt.setString(2, "user" + rollno);
							smt.setString(3, "email" + rollno + "@xdata.com");
							smt.setString(4, "");
							smt.setString(5, "pass");
							smt.setString(6, "student");
							smt.setString(7, "CS631");
							smt.executeUpdate();
						}
						
						smt=conn.prepareStatement(insertquery);
						smt.executeUpdate();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
						
					rollno++;
					insertquery="INSERT INTO xdata_student_queries VALUES ('d1','"+quesID+"','"+ "R" + rollno+"',";
					if(sc.hasNext()){
						qID=sc.next();
						quesDesc=sc.next();						
					}else{
						break;	//end of file
					}
				}while(qID.length()==0);			
			
			}
			try {
				new TestAnswer().test(filePath, assignmentId, questionId, 1,"CS631");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
	}
}
