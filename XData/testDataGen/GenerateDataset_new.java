package testDataGen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import util.Configuration;
import util.TableMap;



/**
 * The main class that is called to get data sets for the query
 * @author mahesh
 *
 */
@Deprecated
public class GenerateDataset_new {

	String filePath;

	private static Logger logger=Logger.getLogger(GenerateDataset_new.class.getName());
	
	public GenerateDataset_new(String filePath) throws Exception{		
		this.filePath = filePath;
	}
	@Deprecated
	public void generateDatasetForQuery(int assignmentId, int questionId,int queryId, String course_id, String orderindependent, String query, String queryDesc) throws Exception {
		this.generateDatasetForQuery(assignmentId,questionId,queryId,course_id, orderindependent, query, queryDesc,1);
	}

    @Deprecated
	public void generateDatasetForQuery(int assignmentId, int  questionId, int queryId,String course_id, String orderindependent, String query, String queryDesc, int dummy) throws Exception{


		logger.log(Level.INFO,"------------------------------------------------------------------------------------------\n\n");
		logger.log(Level.INFO,"QueryID:  "+queryId);
		logger.log(Level.INFO,"GENERATING DATASET FOR QUERY: \n" + query);
		logger.log(Level.INFO,"------------------------------------------------------------------------------------------\n\n");
		
		/** delete previous data sets*/		
		RelatedToPreprocessing.deletePreviousDatasets(this, query);
		
		/**Create object for generating data sets */
		GenerateCVC1 cvc = new GenerateCVC1();
		try{
			
		cvc.setFne(false); 
		cvc.setIpdb(false);
		cvc.setFilePath(this.getFilePath());
		//cvc.initializeConnectionDetails(assignmentId, questionId, queryId,course_id);
		
		/**Call pre processing functions before calling data generation methods */
		PreProcessingActivity.preProcessingActivity(cvc);
		
		
		/** Check the data sets generated for this query */
		ArrayList<String> dataSets = RelatedToPreprocessing.getListOfDataset(this);
		
		logger.log(Level.INFO,"\n\n***********************************************************************\n");
		logger.log(Level.INFO,"DATA SETS FOR QUERY "+queryId+" ARE GENERATED");
		logger.log(Level.INFO,"\n\n***********************************************************************\n");

		
		/**Upload the data sets into the database */
		TableMap tm = cvc.getTableMap();
		//WriteFileAndUploadDatasets.uploadDataset(this, assignmentId,questionId,queryId, course_id,dataSets,tm);			

		logger.log(Level.INFO,"\n***********************************************************************\n\n");
		logger.log(Level.INFO,"DATASET FOR QUERY "+queryId+" ARE UPLOADED");
		logger.log(Level.INFO,"\n***********************************************************************\n\n");
		}finally{
			if(cvc!= null && cvc.getConnection() != null){
			 cvc.closeConn();
			}
		}
	}
    @Deprecated
	public void generateDatasetForBranchQuery(int assignmentId,int questionId, int queryId, String course_id,String[] branchQuery,String[] branchResult, String query, String queryDesc) throws Exception
	{
		logger.log(Level.INFO,"------------------------------------------------------------------------------------------\n\n");
		logger.log(Level.INFO,"QueryID "+queryId);
		logger.log(Level.INFO,"GENERATING DATASET FOR QUERY: \n" + query);
		
		
			
		/** delete previous data sets*/		
	    RelatedToPreprocessing.deletePreviousDatasets(this, query);
	
		/**upload the details about the list of queries and the conditions between the branch queries*/
		BufferedWriter ord2 = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/branchQuery.txt"));
		for(String str : branchQuery)
		{
			ord2.write(str);
			ord2.newLine();
		}
		ord2.close();
		
		BufferedWriter ord3 = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/branchResult.txt"));
		for(String str : branchResult)
		{
			ord3.write(str);
			ord3.newLine();
		}
		ord3.close();
			
		/**Create object for generating data sets */
		GenerateCVC1 cvc = new GenerateCVC1();
		
		cvc.setFne(false);
		cvc.setIpdb(false);
		cvc.setFilePath(this.getFilePath() + "/" + queryId);
		
		/**Call pre processing functions before calling data generation methods */
		PreProcessingActivity.preProcessingActivity(cvc);
		
		
		/** Check the data sets generated for this query */
		ArrayList<String> dataSets = RelatedToPreprocessing.getListOfDataset(this);
		
		logger.log(Level.INFO,"\n\n***********************************************************************\n");
		logger.log(Level.INFO,"DATA SETS FOR QUERY "+queryId+" ARE GENERATED");
		logger.log(Level.INFO,"\n\n***********************************************************************\n");

		/**Upload the data sets into the database */
		TableMap tm = cvc.getTableMap();
	//	WriteFileAndUploadDatasets.uploadDataset(this, assignmentId,questionId,queryId, course_id,dataSets,tm);			

		logger.log(Level.INFO,"\n***********************************************************************\n\n");
		logger.log(Level.INFO,"DATASET FOR QUERY "+queryId+" ARE UPLOADED");
		logger.log(Level.INFO,"\n***********************************************************************\n\n");
		cvc.closeConn();
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public static void entry(String[] args) throws Exception{
		String filePath="4";

		String queryId = "A"+args[0]+"Q"+args[1]+"S"+args[2];
		String coursePath = System.getProperty("file.separator");
		GenerateDataset_new g=new GenerateDataset_new(filePath + "/" +args[3]+"/" + queryId);
		
        try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e1) {
			logger.log(Level.SEVERE,e1.getMessage(),e1);
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		} 
        
        int assignment_id = Integer.parseInt(args[0]);
		String question_id = args[1];
		int q_id = Integer.parseInt(question_id);
		int query_id = Integer.parseInt(args[2]);
		 String course_id = args[3];
         /*try(Connection dbcon = MyConnection.getDatabaseConnection()){
	    	if(dbcon!=null){
	    	    	  logger.log(Level.INFO,"Connected successfullly");
	    	    	  
	    	 }
	    	
	    	String sel="Select sql from xdata_instructor_query where assignment_id=? and question_id=? and query_id=?";
	    	
			try(PreparedStatement stmt=dbcon.prepareStatement(sel)){
				stmt.setInt(1,assignment_id); 
				stmt.setInt(2, q_id); 
				stmt.setInt(3, query_id);
				try(ResultSet rs=stmt.executeQuery()){ 
					rs.next();
					String sql=rs.getString("sql");
					g.generateDatasetForQuery(assignment_id,q_id,query_id, course_id,"true", sql, "", assignment_id);
				}
			}
	    }*/
	}
	

	public static void main(String[] args) throws Exception{
		String filePath="4";
		GenerateDataset_new g=new GenerateDataset_new(filePath + "/" + "A1Q30");
	
	//g.generateDatasetForQuery("A1Q1", "CS631","true", "SELECT course_id, title FROM course", "");
	//g.generateDatasetForQuery("A1Q2","CS631", "true", "SELECT course_id, title FROM course WHERE dept_name= 'Comp. Sci.'", "");
	//g.generateDatasetForQuery("A1Q3","CS631", "true", "SELECT DISTINCT course.course_id, course.title, ID from course natural join teaches WHERE (teaches.semester='Spring' or teaches.year='2010') and (course.title = 'CS-101' or (course.title = 'BIO-101' and course.credits = 6))", "");
	//g.generateDatasetForQuery("A1Q4", "CS631","true", "SELECT DISTINCT student.id, student.name FROM takes natural join student WHERE course_id ='CS-101'", "");
	//g.generateDatasetForQuery("A1Q5", "CS631","true", "SELECT DISTINCT course.dept_name FROM course NATURAL JOIN section WHERE section.semester='Spring' AND section.year='2010'", "");
	//g.generateDatasetForQuery("A1Q6","CS631", "true", "SELECT course_id, title FROM course WHERE credits > 3", "");
	//g.generateDatasetForQuery("A1Q7","CS631", "true", "select course_id, count(distinct id) from course natural left outer join takes group by course_id", "");
	//g.generateDatasetForQuery("A1Q8", "CS631","true", "SELECT DISTINCT course_id, title FROM course NATURAL JOIN section WHERE semester = 'Spring' AND year = 2010 AND course_id NOT IN (SELECT course_id FROM prereq)", "");
	//g.generateDatasetForQuery("A1Q99","CS631", "true", "WITH s as (SELECT id,time_slot_id,year,semester FROM takes NATURAL JOIN section GROUP BY id,time_slot_id,year,semester HAVING count(time_slot_id)>1) SELECT DISTINCT id,name FROM s NATURAL JOIN student", "");
	//g.generateDatasetForQuery("A1Q9","CS631", "true", "SELECT distinct A.id, A.name FROM (SELECT * from student NATURAL JOIN takes NATURAL JOIN section) A, (SELECT * from student NATURAL JOIN takes NATURAL JOIN section) B WHERE A.name = B.name and A.time_slot_id = B.time_slot_id and A.course_id <> B.course_id and A.semester = B.semester and A.year = B.year", "");
	//g.generateDatasetForQuery("A1Q10","CS631", "true", "SELECT DISTINCT dept_name FROM course WHERE credits = (SELECT max(credits) FROM course)", "");
	//g.generateDatasetForQuery("A1Q11","CS631", "true", "SELECT DISTINCT instructor.ID,name,course_id FROM instructor LEFT OUTER JOIN TEACHES ON instructor.ID = teaches.ID", "");
	//g.generateDatasetForQuery("A1Q12","CS631", "true", "SELECT student.id, student.name FROM student WHERE student.name like '%sr%'", "");
	//g.generateDatasetForQuery("A1Q13", "CS631","true", "SELECT id, name FROM student NATURAL LEFT OUTER JOIN (SELECT id, name, course_id FROM student NATURAL LEFT OUTER JOIN takes WHERE year = 2010 and semester = 'Spring') a", "");
	//g.generateDatasetForQuery("A1Q14", "CS631","true", "SELECT DISTINCT * FROM takes T WHERE (NOT EXISTS (SELECT id,course_id FROM takes S WHERE S.grade <> 'F' AND T.id=S.id AND T.course_id=S.course_id) and T.grade IS NOT NULL) or (T.grade <> 'F' AND T.grade IS NOT NULL)", "");
		
		//g.generateDatasetForQuery(1,2,0, "CS631","true", "SELECT c.dept_name, SUM(i.salary), MAX(i.salary) FROM course c INNER JOIN department d ON (c.dept_name = d.dept_name) INNER JOIN instructor i ON (d.dept_name = i.dept_name) GROUP BY c.dept_name HAVING SUM(i.salary)>100000 AND MAX(i.salary)<75000", "");
		//g.generateDatasetForQuery(1, 25, 0, "CS631","true", "select * from prereq where  prereq_id='qwe' and not exists (SELECT * from course where prereq.prereq_id=course.course_id)", "");
		g.generateDatasetForQuery(2, 15, 1, "CS631","true", "SELECT id, name FROM student where NOT EXISTS (select * from student s NATURAL JOIN takes WHERE takes.year = 2010 and takes.semester = 'Spring' and s.id=takes.id)", "");
		
		//g.generateDatasetForQuery(1, 29, 0, "CS631","true", "select prereq_id from prereq where course_id='CS-601'", "");

	}

}
