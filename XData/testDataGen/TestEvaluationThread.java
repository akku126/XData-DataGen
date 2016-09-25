package testDataGen;

public class TestEvaluationThread implements Runnable{



		 @Override
		public void run()
		 {
			 int asID = 17;
			 String questionID = "1";
			 String studentID = "student";
			 String courseID="CS631";
			 
			 
		   
		try{
		     TestAssignment ta = new TestAssignment();
		     String arga[] = {String.valueOf(asID), String.valueOf(questionID.trim()), studentID,courseID};
		     //ta.main(arga);
		     
		     for(int i=0;i<300000;i++){} //a simple delay block to clarify.
		     ta.testThreadsForQuery(arga);
		     
		     System.out.println("finished thread execution in run method");
		     System.out.println("******************************************************");
		 } catch (Exception e) {
				//logger.log(Level.SEVERE,e.getMessage(),e);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }

		
		

}
