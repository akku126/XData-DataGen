package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

//import testDataGen.TestAssignment;

public class RunTest {
	
	static CreateAssignment ca;
	static String folderPath = "";
	 
	public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		int assignmentNo = 1001;
		String courseId = "AutomatedTesting";
		int qryId = 1;
		int quesId = 1;
		//no loading schema as of now
		ca = new CreateAssignment();
		System.out.println("Loading Assignment");
		try {
			ca.clearDB(assignmentNo);
			
			int conn_Id = ca.addDBConnection(courseId);
			ca.createAssignment(courseId, assignmentNo,15,conn_Id );
			//get the instructor query from file
			loadInstructorQueries(courseId, assignmentNo);
			
			loadMutantQueries(assignmentNo);
			//ca.addInstructorQuery(courseId, qryId, Integer.toString(quesId), iqry, assignmentNo);
			
			System.out.println("Running Tests");
			//now run the tests
			//TestAssignment ta = new TestAssignment();
			List<String> corrQueries = new ArrayList<String>();
			//corrQueries.addAll(ta.evaluateAssignment(assignmentNo));
			outputAnswerToFile(corrQueries,assignmentNo);
			System.out.println("DONE :"+folderPath);
		} catch (Exception e){
			ca.clearDB(assignmentNo);
			
			System.out.println(e.getStackTrace());
			e.printStackTrace();
		}
		//clear up the db
		//ca.clearMutants(assignmentNo);
		//ca.clearInstructorQuery(assignmentNo);
		//ca.clearAssignment(assignmentNo);
		ca.clearDB(assignmentNo);
	}
	
	private static void outputAnswerToFile(List<String> result,int id)throws Exception{
		File file = new File(folderPath+"Assignment"+id+"/result.txt");

		// if file doesnt exists, then create it
		if (!file.exists()) { 
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		for (int i=0; i<result.size(); i++){
			bw.write(result.get(i)+"\n");
		}
		bw.close();
	}
	
	private static void loadInstructorQueries(String courseId,int id){
		String fileName = folderPath+"Assignment"+id+"/InstructorQuery.txt";
		System.out.println(fileName);
		try {
			BufferedReader ord3 = new BufferedReader(new FileReader(fileName));
			String qry="";
			while ((qry=ord3.readLine())!=null){
				String tq = qry.trim();
				String[] q = tq.split("\\|");
				//addInstructorQuery(String courseId, int qryid,int quesId, String sql, int assId)
				ca.addInstructorQuery(courseId, Integer.parseInt(q[0]),Integer.parseInt(q[0]), q[1], id);
				//ca.generateDataset(id, Integer.parseInt(q[0]), "/home/temp_cvc");
			}
			ord3.close();
		} catch (Exception e){
			System.out.println(e.toString());
		}
	}
	
	private static void loadMutantQueries(int id){
		//String fileName = "/home/shree/automatedTesting/"+"Assignment"+id+"/MutantQuery.txt";
		String fileName = folderPath+"Assignment"+id+"/MutantQuery.txt";
		String qry;
		try {
			BufferedReader ord3 = new BufferedReader(new FileReader(fileName));
			while ((qry=ord3.readLine())!=null){
				String tq = qry.trim();
				if (tq.length()>0){
					String[] data = tq.split("\\|",3);
					//addMutants(int assId, int qid, String rollnum, String qstr, String tajudge,String course_id)
					ca.addMutants(id, Integer.parseInt(data[0]), data[1], data[2], false,"AutomatedTesting");
				}
			}
			
		} catch (Exception e){
			System.out.println(e.toString());
			e.printStackTrace();
			
		}
	}
}
