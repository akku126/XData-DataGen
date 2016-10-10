package partialMarking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import parsing.AggregateFunction;
import parsing.Node;
import util.MyConnection;

public class PartialMarker {
	private static Logger logger = Logger.getLogger(PartialMarker.class.getName());
	// The unique identifier of the assignment
	int assignmentId;
	
	// The unique identifier for the question within the assignment
	int questionId;
	
	// The unique identifier for the query within the question
	int queryId;
	
	//Unique identifier for the course
	String course_id;
	
	// The id of the student
	String studentId;
	
	// Maximum marks
	int maxMarks;
	
	String guestStudentQuery;
	
	// Details corresponding to the instructor query
	public QueryDetails InstructorQuery;
	
	// Details corresponding to the student query 
	public QueryDetails StudentQuery;
	
	// Configuration values required for the scoring function
	public PartialMarkerConfig Configuration;
	
	// Returns the assignment id
	public int getAssignmentId(){
		return this.assignmentId;
	}
	
	// Returns the question id
	public int getQuestionId(){
		return this.questionId;
	}
	
	// Returns the question id
	public int getQueryId(){
		return this.queryId;
	}
	
	// Returns the student id
	public String getStudentId(){
		return this.studentId;
	}
	
	// Sets the assignment id
	public void setAssignmentId(int aId){
		this.assignmentId = aId;
	}
	
	// Sets the question id
	public void setQuestionId(int aId){
		this.questionId = aId;
	}
	
	// Sets the question id
	public void setQueryId(int qId){
		this.queryId = qId;
	}
	
	// Sets the student id
	public void setStudentQuery(String query){
		this.studentId = query;
	}
	
	// Returns an instance of the partial marker
	public PartialMarker(int assignmentId, int quesId, int queryId, String course_id, String rollNum){
		this.assignmentId = assignmentId;
		this.questionId = quesId;
		this.queryId = queryId;
		this.maxMarks = 100;
		this.course_id = course_id;
		this.studentId = rollNum;
		this.InstructorQuery = new QueryDetails();
		this.StudentQuery = new QueryDetails();
		this.Configuration = new PartialMarkerConfig();
		this.guestStudentQuery = null;
		try{
			this.Configuration.setConfigurationValues(assignmentId, quesId, queryId);
		} catch (Exception ex){
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			//ex.printStackTrace();
		}
	}
	
	public PartialMarker(int assignmentId, int quesId, int queryId, String course_id, String rollNum,String studQuery){
		this.assignmentId = assignmentId;
		this.questionId = quesId;
		this.queryId = queryId;
		this.maxMarks = 100;
		this.course_id = course_id;
		this.studentId = rollNum;
		this.InstructorQuery = new QueryDetails();
		this.StudentQuery = new QueryDetails();
		this.Configuration = new PartialMarkerConfig();
		this.guestStudentQuery = studQuery;
		try{
			this.Configuration.setConfigurationValues(assignmentId, quesId, queryId);
		} catch (Exception ex){
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			//ex.printStackTrace();
		}
	}
	
	
	private void initialize(){
		try{		
			this.maxMarks = this.InstructorQuery.InitializeInstructorQuery(this.assignmentId, this.questionId, this.queryId);
			this.StudentQuery.InitializeStudentQuery(this.assignmentId, this.questionId, this.studentId,this.guestStudentQuery);
		}
		catch(Exception ex){
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			//ex.printStackTrace();
		}
	}
	
	// Returns the marks corresponding to the query of the student in comparison to the instructor query
	public MarkInfo getMarks() throws Exception{
		
		this.initialize();
			
		// Canonicalizing the queries
		CanonicalizeQuery.Canonicalize(this.InstructorQuery.OuterQuery);
		CanonicalizeQuery.Canonicalize(this.StudentQuery.OuterQuery);
		
		//Check for distinct
		boolean evaluateDistinct = EvaluateDistinct.evaluate(this.InstructorQuery,this.StudentQuery,this.assignmentId, this.questionId, this.queryId, this.course_id);		
		float maxMainQueryScore = this.calculateScore(evaluateDistinct, this.InstructorQuery.OuterQuery, this.InstructorQuery.OuterQuery, 0).Marks;
		
		MarkInfo result = this.calculateScore(evaluateDistinct,this.InstructorQuery.OuterQuery, this.StudentQuery.OuterQuery, 0);
		float studentQueryScore=result.Marks;
	
		result.Configuration = this.Configuration;
		
		float mainQueryScore = result.Marks;
		
		// Cleaning up the connections
		this.cleanup();
		
		// Setting the negative scores to zero
		if(mainQueryScore < 0) 
			mainQueryScore = 0;
		
		result.Marks = mainQueryScore/maxMainQueryScore * this.maxMarks ;
		System.out.println("Computed Marks="+result.Marks+ " student score="+studentQueryScore +" mainqueryScore="+maxMainQueryScore);
		return result;
	}
		
	public static void main(String args[]) throws Exception{		
		//Connection conn = MyConnection.getExistingDatabaseConnection();
		try(Connection conn = MyConnection.getDatabaseConnection()){
		
			String instructorQuery=" SELECT distinct time_slot.day FROM teaches, section, time_slot where teaches.course_id=section.course_id AND teaches.semester=section.semester AND teaches.year=section.year AND teaches.sec_id=section.sec_id AND section.time_slot_id= time_slot.time_slot_id AND section.semester='Fall' AND section.year='2009' and teaches.id='22222'";
			String studentAnswer="select time_slot.time_slot_id FROM teaches, section, time_slot where teaches.course_id=section.course_id AND teaches.semester=section.semester AND teaches.year=section.year AND teaches.sec_id=section.sec_id AND section.time_slot_id= time_slot.time_slot_id AND section.semester='Fall' AND section.year='2009' and teaches.id='22222'";
		String courseId = "CS632";
		int assignmentId =11;
		int questionId = 1;
		String rollnum = "09005027";
		int queryId = 1;		
		float ta = 50;
		int maxMarks = 100;
		String oldQueryId = "A" + assignmentId + "Q" + questionId + "S" + queryId;
		String desc = "DUMMY";
		
		insertIntoQinfo(courseId, assignmentId, questionId, queryId, instructorQuery, desc, maxMarks, maxMarks);
		
		//WriteFileAndUploadDatasets.updateQueryInfo(new GenerateDataset_new(""), assignmentId, questionId, queryId, instructorQuery, "Find, for each course, the number of distinct students who have taken the course");
		
		try(PreparedStatement stmt = conn.prepareStatement("select * from xdata_student_queries where assignment_id = ? and question_id = ? and rollnum = ?")){
		stmt.setInt(1, assignmentId);
		stmt.setInt(2, questionId);
		stmt.setString(3, rollnum);
		try(ResultSet rs = stmt.executeQuery()){
		
		if(!rs.next()){		
			String insertquery = "INSERT INTO xdata_student_queries (dbid, queryid, rollnum, querystring, assignment_id, question_id,course_id) VALUES (?,?,?,?,?,?,?)";			
			try(PreparedStatement stmt1 = conn.prepareStatement(insertquery)){
			stmt1.setString(1, "d1");
			stmt1.setString(2, oldQueryId);
			stmt1.setString(3, rollnum);
			stmt1.setString(4, studentAnswer);
			stmt1.setInt(5, assignmentId);
			stmt1.setInt(6, questionId);
			stmt1.setString(7,courseId);
			stmt1.executeUpdate();
			}
		} else {
			try(PreparedStatement stmt1 = conn.prepareStatement("update xdata_student_queries set querystring = ? where assignment_id = ? and question_id = ? and rollnum = ? and course_id=?")){
				stmt1.setString(1, studentAnswer);
				stmt1.setInt(2, assignmentId);
				stmt1.setInt(3, questionId);
				stmt1.setString(4, rollnum);			
				stmt1.setString(5,courseId);
				stmt1.executeUpdate();
			}
		}
		}	
				
		PartialMarker part = new PartialMarker(assignmentId, questionId, queryId, courseId, rollnum);
		MarkInfo result = part.getMarks();
		
		Gson gson = new Gson();
		String json = gson.toJson(result);
		String updateScoreQuery = "update xdata_student_queries set score = ?,markinfo=?,max_marks=? where assignment_id=? and question_id=? and rollnum=?";
		try(PreparedStatement ps = conn.prepareStatement(updateScoreQuery)){
		ps.setFloat(1, result.Marks);

		ps.setString(2, json);
		ps.setInt(3, maxMarks);
		ps.setFloat(4, assignmentId);
		ps.setInt(5,questionId);
		ps.setString(6, rollnum);
		ps.executeUpdate();
		}
		
		/*try(PreparedStatement stmt1 = conn.prepareStatement("select * from score where assignment_id = ? and question_id = ? and rollnum = ?")){
			stmt1.setInt(1, assignmentId);
			stmt1.setInt(2, questionId);
			stmt1.setString(3, rollnum);
			try(ResultSet rs = stmt1.executeQuery()){

			
			if(!rs.next()){
				String scoreQuery = "insert into score values(?, ?, ?, ?, ?, ?, ?, ?)";		
				try(PreparedStatement stmt2 = conn.prepareStatement(scoreQuery)){		
					stmt2.setString(1, oldQueryId);
					stmt2.setString(2, rollnum);
					stmt2.setFloat(3, result.Marks);
					stmt2.setFloat(4, ta);
					stmt2.setFloat(5, assignmentId);
					stmt2.setInt(6, questionId);
					stmt2.setInt(7, maxMarks);
					stmt2.setString(8, json);
					stmt2.executeUpdate();
				}		
			} else {
				try(PreparedStatement stmt2 = conn.prepareStatement("update score set result = ?, markInfo = ? where assignment_id = ? and question_id = ? and rollnum = ?")){
					stmt2.setFloat(1, result.Marks);
					stmt2.setString(2, json);
					stmt2.setInt(3, assignmentId);
					stmt2.setInt(4, questionId);
					stmt2.setString(5, rollnum);			
					stmt2.executeUpdate();
				}
				
			}
			}
		}*/
		}
		}
	}
	
	private static void insertIntoQinfo(String courseId, int asId, int qId, int queryId, String query, String desc, int maxMarks, int marks) throws Exception{		
		//Connection conn = MyConnection.getExistingDatabaseConnection();
		try(Connection conn = MyConnection.getDatabaseConnection()){
		
		try(PreparedStatement stmt = conn.prepareStatement("select * from xdata_qinfo where course_id = ? and assignment_id = ? and question_id = ?")){
			stmt.setString(1, courseId);
			stmt.setInt(2, asId);
			stmt.setInt(3, qId);		
			
			ResultSet rs = stmt.executeQuery();
			if(!rs.next()){		
				try(PreparedStatement stmt1 = conn.prepareStatement("INSERT INTO xdata_qinfo VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")){
					stmt1.setString(1, courseId);
					stmt1.setInt(2, asId);
					stmt1.setInt(3, qId);		
					stmt1.setString(4, desc); 
					stmt1.setString(5, "");
					stmt1.setInt(6, maxMarks); 
					stmt1.setBoolean(7, false);
					stmt1.setBoolean(8, false); 
					stmt1.setBoolean(9, true);
					stmt1.setInt(10, queryId);
					stmt1.setInt(11, 15);
					stmt1.setBoolean(12, true);
					stmt1.executeUpdate();
				}
			} else {
				try(PreparedStatement stmt1 = conn.prepareStatement("update xdata_qinfo set querytext = ?, totalmarks = ? where course_id = ? and assignment_id = ? and question_id = ?")){
					stmt1.setString(1, desc); 
					stmt1.setInt(2, maxMarks);
					stmt1.setString(3, courseId);
					stmt1.setInt(4, asId);
					stmt1.setInt(5, qId);
					stmt1.executeUpdate();
				}
			}
		
			try(PreparedStatement stmt1 = conn.prepareStatement("select * from xdata_instructor_query where course_id = ? and assignment_id = ? and question_id = ? and query_id = ?")){
				stmt1.setString(1, courseId);
				stmt1.setInt(2, asId);
				stmt1.setInt(3, qId);
				stmt1.setInt(4, queryId);
				
				rs = stmt1.executeQuery();
				if(!rs.next()){
					try(PreparedStatement stmt2 = conn.prepareStatement("insert into xdata_instructor_query Values (?,?,?,?,?,?)")){
						stmt2.setInt(1, asId); 
						stmt2.setInt(2, qId);
						stmt2.setString(3, query); 
						//stmt2.setString(4,""); 
						stmt2.setString(4, courseId); 
						stmt2.setInt(5, queryId);
						stmt2.setInt(6, marks);
						
						stmt2.executeUpdate();
					}
				} else {
					try(PreparedStatement stmt2 = conn.prepareStatement("update xdata_instructor_query set sql = ?, marks = ? where course_id = ? and assignment_id = ? and question_id = ? and query_id = ?")){
						stmt2.setString(1, query); 
						stmt2.setInt(2, marks);
						stmt2.setString(3, courseId);
						stmt2.setInt(4, asId);
						stmt2.setInt(5, qId);
						stmt2.setInt(6, queryId);
						stmt2.executeUpdate();
					}
				}	
			}
		}
	}
	}

	// Calculates a score based on the relations involved in the join
	// Number of inner and outer joins are also compared
	private float getJoinScore(QueryData masterData, QueryData slaveData){
		float score = compare(masterData.getJoinTables(), slaveData.getJoinTables());
		
		score = masterData.getNumberOfOuterJoins() == slaveData.getNumberOfOuterJoins() ? score + 1 : score - 0.5f;
		score = masterData.getNumberOfInnerJoins() == slaveData.getNumberOfInnerJoins() ? score + 1 : score - 0.5f;
		
		return score;
	}	
	private float compare(ArrayList<String> master, ArrayList<String> slave) {
		float score = 0;
		for(String n1 : slave){
			Boolean found = false;
			for(String n2 : master){				
				if(n1.equals(n2)){
					found = true;
					break;
				}
			}
			
			if(found){
				score++;
			}
			else{
				score=score-0.5f;
			}
		}
		
		return score;
	}

	
private Boolean checkSelectionEquality(Node n1, Node n2){
		
		if(!n1.getOperator().equals(n2.getOperator()))
			return false;
		
		if(!n1.getLeft().getTable().equals(n2.getLeft().getTable()))
			return false;
		
		if(!n1.getLeft().getColumn().getColumnName().equals(n2.getLeft().getColumn().getColumnName()))
			return false;
		
		if(n1.getRight().getNodeType().equals(Node.getColRefType())){
			
			if(!n2.getRight().getNodeType().equals(Node.getColRefType()))
				return  false;
			
			if(!n1.getRight().getTable().equals(n2.getRight().getTable()))
				return false;
			
			if(!n1.getRight().getColumn().getColumnName().equals(n2.getRight().getColumn().getColumnName()))
				return false;
		}
		
		if(n1.getRight().getNodeType().equals(Node.getValType())){
			if(!n2.getRight().getNodeType().equals(Node.getValType()))
				return  false;
			
			if(!n1.getRight().getStrConst().equals(n2.getRight().getStrConst()))
				return false;
		}
		
		return true;
	}
	
private Boolean checkProjectionEquality(Node n1, Node n2){

	if(n1.getNodeType().equals(Node.getAggrNodeType())){
		
		if(!n2.getNodeType().equals(Node.getAggrNodeType()))
			return false;
		
		AggregateFunction agg1 = n1.getAgg();
		AggregateFunction agg2 = n2.getAgg();
		
		if(!agg1.getFunc().equals(agg2.getFunc()))
			return false;
		
		if(!agg1.getAggExp().getColumn().getTable().getTableName().equals(agg2.getAggExp().getColumn().getTable().getTableName()))
			return false;
		
		if(!agg1.getAggExp().getColumn().getColumnName().equals(agg2.getAggExp().getColumn().getColumnName()))
			return false;
	}
	
	if(n1.getNodeType().equals(Node.getColRefType())){
		
		if(!n1.getTable().equals(n2.getTable()))
			return false;
		
		if(!n1.getColumn().getColumnName().equals(n2.getColumn().getColumnName()))
			return false;
	}
	
	return true;
}
	
/* recoded by mathew on 12 May 2016, 
 * 
 * checks the syntactic equivalence of two nodes that represents atomic having clause expressions
 */
private Boolean checkHavingClauseEquality(Node n1, Node n2){

//check for the equivalence of operator
if(!n1.getOperator().equals(n2.getOperator()))
	return false;

//check for equivalence of right nodes

//if right node of n1 is a column reference
if(n1.getRight().getNodeType().equals(Node.getColRefType())){

	if(!n2.getRight().getNodeType().equals(Node.getColRefType()))
		return  false;

	if(!n1.getRight().getTable().equals(n2.getRight().getTable()))
		return false;

	if(!n1.getRight().getColumn().getColumnName().equals(n2.getRight().getColumn().getColumnName()))
		return false;
}

//if right node of n1 is a constant value
if(n1.getRight().getNodeType().equals(Node.getValType())){
	if(!n2.getRight().getNodeType().equals(Node.getValType()))
		return  false;

	if(!n1.getRight().getStrConst().equals(n2.getRight().getStrConst()))
		return false;
}

//if right node of n1 is an aggregate expression
if(n1.getRight().getNodeType().equals(Node.getAggrNodeType())){

	if(!n2.getRight().getNodeType().equals(Node.getAggrNodeType()))
		return false;

	AggregateFunction agg1 = n1.getRight().getAgg();
	AggregateFunction agg2 = n2.getRight().getAgg();

	if(!agg1.getFunc().equals(agg2.getFunc()))
		return false;

	if(!agg1.getAggExp().getColumn().getTable().getTableName().equals(agg2.getAggExp().getColumn().getTable().getTableName()))
		return false;

	if(!agg1.getAggExp().getColumn().getColumnName().equals(agg2.getAggExp().getColumn().getColumnName()))
		return false;
}

//check for equivalence of left nodes

//if left node of n1 is a column reference
if(n1.getLeft().getNodeType().equals(Node.getColRefType())){

	if(!n2.getLeft().getNodeType().equals(Node.getColRefType()))
		return  false;

	if(!n1.getLeft().getTable().equals(n2.getLeft().getTable()))
		return false;

	if(!n1.getLeft().getColumn().getColumnName().equals(n2.getLeft().getColumn().getColumnName()))
		return false;
}

//if left node of n1 is a constant value
if(n1.getLeft().getNodeType().equals(Node.getValType())){
	if(!n2.getLeft().getNodeType().equals(Node.getValType()))
		return  false;

	if(!n1.getLeft().getStrConst().equals(n2.getLeft().getStrConst()))
		return false;
}

//if left node of n1 is a aggregate expression
if(n1.getLeft().getNodeType().equals(Node.getAggrNodeType())){

	if(!n2.getLeft().getNodeType().equals(Node.getAggrNodeType()))
		return false;

	AggregateFunction agg1 = n1.getLeft().getAgg();
	AggregateFunction agg2 = n2.getLeft().getAgg();

	if(!agg1.getFunc().equals(agg2.getFunc()))
		return false;

	if(!agg1.getAggExp().getColumn().getTable().getTableName().equals(agg2.getAggExp().getColumn().getTable().getTableName()))
		return false;

	if(!agg1.getAggExp().getColumn().getColumnName().equals(agg2.getAggExp().getColumn().getColumnName()))
		return false;
}
	
	return true;
}

/**
 * This method checks whether the aggregateFunction SUM,COUNT,etc., matches with student query
 * 
 * @param master
 * @param slave
 * @return
 */
private Boolean checkAggregateName(AggregateFunction master, AggregateFunction slave){
	if(master.getFunc() != null && slave.getFunc() == null){
		return false;
	}
	if(master.getFunc() != null && slave.getFunc() == null){
		return false;
	}
	if(!(master.getFunc().equalsIgnoreCase(slave.getFunc()))){
		return false;
	}
	return true;
}

private float compareHavingClause(ArrayList<Node> master, ArrayList<Node> slave){
	float score = 0;
	for(Node n1 : slave){
		Boolean found = false;
		for(Node n2 : master){				
			if(checkHavingClauseEquality(n1, n2)){
				found = true;
				break;
			}
		}
		
		if(found){
			score++;
		}
		else{
			score=score-0.5f;
		}
	}
	
	return score;

	
	/*if(master.getNodeType().equals(Node.getBroNodeType()) && slave.getNodeType().equals(Node.getBroNodeType())){
		this.uniqueHavingClause++;
		if(checkHavingClauseEquality(master,slave)){
			score ++;
		}else{ 
			score--;
		}
	}
	else if(master.getNodeType().equals(Node.getAndNodeType()) && slave.getNodeType().equals(Node.getAndNodeType())){
		compareHavingClause(master.getLeft(),slave.getLeft());
		compareHavingClause(master.getRight(),slave.getRight());
		
	}
	return score;*/
}
	
private float compareSelection(List<Node> master, List<Node> slave){
	float score = 0;
	for(Node n1 : slave){
		Boolean found = false;
		for(Node n2 : master){				
			if(checkSelectionEquality(n1, n2)){
				found = true;
				break;
			}
		}
		
		if(found){
			score++;
		}
		else{
			score=score-0.5f;
		}
	}
	
	return score;
}

private float compareProjection(ArrayList<Node> master, ArrayList<Node> slave){
	float score = 0;
	for(Node n1 : slave){
		Boolean found = false;
		for(Node n2 : master){				
			if(checkProjectionEquality(n1, n2)){
				found = true;
				break;
			}
		}
		if(found){
			score++;
		}
		else{
			score=score-0.5f;
		}
	}		
	return score;
}
	
private float compareAggregates(ArrayList<AggregateFunction> master, ArrayList<AggregateFunction> slave){
	float score = 0;
	for(AggregateFunction n1 : slave){
		Boolean found = false;
		for(AggregateFunction n2 : master){				
			//Aggregate Name should match and the column also should match
			if(checkAggregateName(n1,n2) && checkProjectionEquality(n1.getAggExp(), n2.getAggExp())){
				found = true;
				break;
			}
		}
		if(found){
			score++;
		}
		else{
			score=score-0.5f;
		}
	}		
	return score;
}
	
	/*private QueryInfo populateQueryInfo(QueryData instructorData, QueryData studentData, int level, boolean isEvaluateDistinct){
		
		QueryInfo qInfo = new QueryInfo();
		qInfo.Level = level;
		QueryInfo InstrQinfo = new QueryInfo();
		QueryInfo StudentQinfo = new QueryInfo();
		//qInfo.InstructorQuery = instructorData;
		//qInfo.StudentQuery = studentData;
		
		for(Node n: instructorData.getSelectionConditions()){
			InstrQinfo.Predicates.add(n.toString());
		}
		
		for(Node n: studentData.getSelectionConditions()){
			StudentQinfo.Predicates.add(n.toString());
		}
		
		for(Node n: instructorData.getProjectionList()){
			InstrQinfo.Projections.add(n.toString());
		}
		
		for(Node n: studentData.getProjectionList()){
			StudentQinfo.Projections.add(n.toString());
		}
		
		for(Node n: instructorData.GroupByNodes){
			InstrQinfo.GroupBy.add(n.toString());
		}
		
		for(Node n: studentData.GroupByNodes){
			StudentQinfo.GroupBy.add(n.toString());
		}
		
		for(String n: instructorData.getRelations()){
			InstrQinfo.Relations.add(n);
		}
		
		for(String n: studentData.getRelations()){
			StudentQinfo.Relations.add(n);
		}
		
		for(Node n : instructorData.getHavingClause()){
			InstrQinfo.HavingClause.add(n.toString());
		}
		
		for(Node n : studentData.getHavingClause()){
			StudentQinfo.HavingClause.add(n.toString());
		}
		
		for(String n : instructorData.getSubQConnectives()){
			InstrQinfo.SubQConnective.add(n);
		}
		for(String n : studentData.getSubQConnectives()){
			StudentQinfo.SubQConnective.add(n);
		}
		
		for(AggregateFunction n : instructorData.getAggregateList()){
			InstrQinfo.Aggregates.add(n.toString());
		}
		for(AggregateFunction n : studentData.getAggregateList()){
			StudentQinfo.Aggregates.add(n.toString());
		}
		
		for(String n : instructorData.getSetOpetators()){
			InstrQinfo.SetOperators.add(n);
		}
		for(String n : studentData.getSetOpetators()){
			StudentQinfo.SetOperators.add(n);
		}
		
		
		InstrQinfo.InnerJoins = instructorData.getNumberOfInnerJoins();
		StudentQinfo.InnerJoins = studentData.getNumberOfInnerJoins();
		
		InstrQinfo.OuterJoins = instructorData.getNumberOfOuterJoins();
		StudentQinfo.OuterJoins = studentData.getNumberOfOuterJoins();
		
		//qInfo.instructorDistinct = instructorData.hasDistinct;
		//qInfo.studentDistinct = studentData.hasDistinct;
		qInfo.instructorInfo = InstrQinfo;
		qInfo.studentInfo = StudentQinfo;
		
		return qInfo;
	}*/
	// Compares query data corresponding to the instructor and student
	
	
private QueryInfo populateQueryInfo(QueryData instructorData, QueryData studentData, int level, boolean isEvaluateDistinct){
		
		QueryInfo qInfo = new QueryInfo();
		qInfo.Level = level;
		
		for(Node n: instructorData.getSelectionConditions()){
			Node newN=this.checkTableOccurence(n,instructorData.getSelectionConditions());
			qInfo.InstructorPredicates.add(newN.toString());
			//qInfo.Predicates.add(qInfo.InstructorPredicates);
		}
		
		for(Node n: studentData.getSelectionConditions()){
			
			Node newN=this.checkTableOccurence(n,studentData.getSelectionConditions());
			qInfo.StudentPredicates.add(newN.toString());
			//qInfo.Predicates.add(qInfo.StudentPredicates);
		}
		
		for(Node n: instructorData.getProjectionList()){
			
			Node newN=this.checkTableOccurence(n,instructorData.getProjectionList());
			qInfo.InstructorProjections.add(newN.toString()); 
			//qInfo.Projections.add(qInfo.InstructorProjections);
			
		}
		
		for(Node n: studentData.getProjectionList()){
			Node newN=this.checkTableOccurence(n,studentData.getProjectionList());
			qInfo.StudentProjections.add(newN.toString());
			//qInfo.Projections.add(qInfo.StudentProjections);
		}
		
		for(Node n: instructorData.GroupByNodes){
			Node newN=this.checkTableOccurence(n,instructorData.GroupByNodes);
			qInfo.InstructorGroupBy.add(newN.toString());
			//qInfo.GroupBy.add(qInfo.InstructorGroupBy);
		}
		
		for(Node n: studentData.GroupByNodes){
			Node newN=this.checkTableOccurence(n,studentData.GroupByNodes);
			qInfo.StudentGroupBy.add(newN.toString());
			//qInfo.GroupBy.add(qInfo.StudentGroupBy);
		} 
		
		for(String n: instructorData.getRelations()){
			qInfo.InstructorRelations.add(n.toString());
			//qInfo.Relations.add(qInfo.InstructorRelations);
		}
		
		for(String n: studentData.getRelations()){
			qInfo.StudentRelations.add(n);
			//qInfo.Relations.add(qInfo.StudentRelations);
		}
		
		for(Node n : instructorData.getHavingClause()){
			Node newN=this.checkTableOccurence(n,instructorData.getHavingClause());
			qInfo.InstructorHavingClause.add(newN.toString());
			//qInfo.HavingClause.add(qInfo.InstructorHavingClause);
		}
		
		for(Node n : studentData.getHavingClause()){
			Node newN=this.checkTableOccurence(n,studentData.getHavingClause());
			qInfo.StudentHavingClause.add(newN.toString());
			//qInfo.HavingClause.add(qInfo.StudentHavingClause);
		}
		
		for(String n : instructorData.getSubQConnectives()){
			qInfo.InstructorSubQConnective.add(n);
			//qInfo.SubQConnective.add(qInfo.InstructorSubQConnective);
		}
		for(String n : studentData.getSubQConnectives()){
			qInfo.StudentSubQConnective.add(n);
			//qInfo.SubQConnective.add(qInfo.StudentSubQConnective);
		}
		
		for(AggregateFunction n : instructorData.getAggregateList()){
			qInfo.InstructorAggregates.add(n.toString());
			//qInfo.Aggregates.add(qInfo.InstructorAggregates);
		}
		for(AggregateFunction n : studentData.getAggregateList()){
			qInfo.StudentAggregates.add(n.toString());
			//qInfo.Aggregates.add(qInfo.StudentAggregates);
		}
		
		for(String n : instructorData.getSetOpetators()){
			qInfo.InstructorSetOperators.add(n);
			//qInfo.SetOperators.add(qInfo.InstructorSetOperators);
		}
		for(String n : studentData.getSetOpetators()){
			qInfo.StudentSetOperators.add(n);
			//qInfo.SetOperators.add(qInfo.StudentSetOperators);
		}
		qInfo.InstructorInnerJoins = instructorData.getNumberOfInnerJoins();
		qInfo.StudentInnerJoins = studentData.getNumberOfInnerJoins();
		//qInfo.InnerJoins.add(qInfo.InstructorInnerJoins);
		//qInfo.InnerJoins.add(qInfo.StudentInnerJoins);
		
		qInfo.InstructorOuterJoins = instructorData.getNumberOfOuterJoins();
		qInfo.StudentOuterJoins = studentData.getNumberOfOuterJoins();
		//qInfo.OuterJoins.add(qInfo.InstructorOuterJoins);
		//qInfo.OuterJoins.add(qInfo.StudentOuterJoins);
		
		qInfo.instructorDistinct = instructorData.hasDistinct;
		qInfo.studentDistinct = studentData.hasDistinct;
		//qInfo.Distinct.add(qInfo.instructorDistinct );
		//qInfo.Distinct.add(qInfo.studentDistinct );
		 
		return qInfo;
	}

		private Node checkTableOccurence(Node n,ArrayList<Node> nodeList) {
			Node newNode = null;
			String tableNameNumber = null; 
			String num = null;
			int numValue=0;
			String newTableName = null;
			if(n != null && nodeList.size() >0){
			//if n is binary node - like bro node or and node, get left and right and call the same method
			if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getAndNodeType())){
				//newNode = this.checkTableOccurence(n, nodeList);
				if(n.getLeft() != null){
					newNode = this.checkTableOccurence(n.getLeft(),nodeList);
					n.setLeft(newNode);
				}
				if(n.getRight() != null){
					newNode = this.checkTableOccurence(n.getRight(), nodeList);
					n.setRight(newNode);
				}
				 return n;
			}
				
			//if n is aggr node - chk node itself or get agg function and get the table name number
			if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){
				
				if(n.getTableNameNo() != null){
					tableNameNumber = n.getTableNameNo();
					num = tableNameNumber.substring(tableNameNumber.length()-1,tableNameNumber.length());
					if(isInteger(num)){
						numValue = (Integer.parseInt(num)+1);
						if(numValue == 1){
						newTableName = n.getTable().toString()+numValue;
						for(Node nn : nodeList){
							if(nn.toString().contains(newTableName)){
								//do nothing
							}
							else{
								n.setTableNameNo(n.getTable().toString());							
							}
						}
						}
					}
				}
				
				newNode = this.checkTableOccurence(n.getAgg().getAggExp(), nodeList);
				n.getAgg().setAggExp(newNode);
				return n;
				
			}
			//if n is column reference node, get the table name number
			//manipulate it to next number and search the list of nodes for the new table number
			
			if(n.getType().equalsIgnoreCase(Node.getColRefType())){
				tableNameNumber = n.getTableNameNo();
				num = tableNameNumber.substring(tableNameNumber.length()-1,tableNameNumber.length());
				if(isInteger(num)){
				numValue = (Integer.parseInt(num)+1);
				if(numValue == 1){
				newTableName = n.getTable().toString()+numValue;
				for(Node nn : nodeList){
					if(nn.toString().contains(newTableName)){
						return n;
					}
					else{
						try {
							newNode = n.clone();
						} catch (CloneNotSupportedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						newNode.setTableNameNo(n.getTable().toString());
						return newNode;
					}
				}
				}else{
					return n;
				}
			}else{
				return n;
			}
			}
			if(n.getType().equalsIgnoreCase(Node.getValType())){
				return n;
				
			}
			}
			   //if result is null - ie., not tablename exists with new number,  change the input node and set to tablename.column name and return the new node
			//if result matches any, then return the node as it is. No changes reqd.
			return newNode;
					
		}
		
		/* The following method converts returns the parameter as 
		 * it is if its value is greater than or equal to zero, and
		 * otherwise returns zero if the 
		 * input parameter has a negative  value
		 * 
		 */
		public static float normalizeNegativeValuesToZero(float d){
			if(d>=0)
				return d;
			else
				return 0;
		}
	
		
	private MarkInfo calculateScore(boolean isEvaluateDistinct, QueryData instructorData, QueryData studentData, int level){
		
		int distinctWeightage = 0;
		MarkInfo marks = new MarkInfo();
		
		MarkInfo whereSubQuery = this.compareListOfQueries(isEvaluateDistinct,instructorData.WhereClauseQueries, studentData.WhereClauseQueries, level + 1);
		
		MarkInfo fromSubQuery = this.compareListOfQueries(isEvaluateDistinct,instructorData.FromClauseQueries, studentData.FromClauseQueries, level + 1);
		
		ArrayList<QueryInfo> temp = new ArrayList<QueryInfo>();
		temp.addAll(whereSubQuery.SubqueryData);
		temp.addAll(fromSubQuery.SubqueryData);
		if(isEvaluateDistinct){
			distinctWeightage = this.Configuration.Distinct;
		}else{
			distinctWeightage = 0;
		}
		
		float totalWeightage = this.Configuration.Predicate + this.Configuration.Relation + this.Configuration.Projection + this.Configuration.Joins + this.Configuration.GroupBy + this.Configuration.HavingClause + this.Configuration.SubQConnective + this.Configuration.Aggregates + this.Configuration.SetOperators + distinctWeightage;
		
		float predWeightage = (this.Configuration.Predicate * 100)/totalWeightage;
		float relationWeightage = (this.Configuration.Relation * 100)/totalWeightage;
		float projWeightage = (this.Configuration.Projection* 100)/totalWeightage;
		float joinWeightage = (this.Configuration.Joins * 100)/totalWeightage;
		float groupByWeightage = (this.Configuration.GroupBy * 100)/totalWeightage;
		float havingClauseWeightage = (this.Configuration.HavingClause * 100)/totalWeightage;
		float subQConnectiveWeightage = (this.Configuration.SubQConnective * 100)/totalWeightage;
		float aggregateWeightage = (this.Configuration.Aggregates * 100) / totalWeightage;
		float setOperatorWeightage = (this.Configuration.SetOperators * 100) / totalWeightage;
		float distinctOpWeightage = (distinctWeightage * 100) / totalWeightage;

		
		float uniquePredicates = instructorData.getSelectionConditions().size();
		float uniqueRelations = instructorData.getRelationCount();
		float uniqueProj = instructorData.getProjectionList().size();
		float instructorJoin = getJoinScore(instructorData, instructorData);
		float uniqueGroupBy = instructorData.GroupByNodes.size();
		float uniqueHavingClause = instructorData.getHavingClause().size();
		float uniqueSubQConnective = instructorData.getSubQConnectives().size();
		float uniqueAggregates = instructorData.getAggregateList().size(); 
		float uniqueSetOperators = instructorData.getSetOpetators().size();
		float uniqueDistinct = 1;
	
float perPredicate = uniquePredicates == 0 ? 0 : predWeightage/uniquePredicates;
		
		float perRelation = uniqueRelations == 0 ? 0 : relationWeightage/uniqueRelations;
		
		float perProjection = uniqueProj == 0 ? 0 : projWeightage/uniqueProj;
		
		float perJoin = instructorJoin == 0 ? 0 : joinWeightage/instructorJoin;
		
		float perGroupBy = uniqueGroupBy == 0 ? 0 : groupByWeightage/uniqueGroupBy;
		
		float perHavingClause = uniqueHavingClause == 0 ? 0 : havingClauseWeightage/uniqueHavingClause;
		
		float perSubQConnective = uniqueSubQConnective == 0 ? 0 : subQConnectiveWeightage/uniqueSubQConnective;
		
		float perAggregate = uniqueAggregates == 0 ? 0 : aggregateWeightage/ uniqueAggregates;
		
		float perSetOperator = uniqueSetOperators == 0 ? 0 : setOperatorWeightage / uniqueSetOperators;
		
		float perDistinctOperator = uniqueDistinct == 0? 0 : distinctOpWeightage / uniqueDistinct;
		
		QueryInfo qInfo = populateQueryInfo(instructorData, studentData, level, isEvaluateDistinct);
		
		float predicateScore = compareSelection(instructorData.getSelectionConditions(), studentData.getSelectionConditions());
		float predicateScoreTotal=(perPredicate==0&&predicateScore!=0)?-predWeightage/2:
			perPredicate*normalizeNegativeValuesToZero(predicateScore);
				
		
		float projectionScore = compareProjection(instructorData.getProjectionList(), studentData.getProjectionList());		
		projectionScore = instructorData.hasDistinct == studentData.hasDistinct ? projectionScore : projectionScore/2;
		float projectionScoreTotal=(perProjection==0 && projectionScore!=0)?-projWeightage/2:
			perProjection*normalizeNegativeValuesToZero(projectionScore);
		
		float relationScore = compare(instructorData.getRelations(), studentData.getRelations());
		float relationScoreTotal=(perRelation==0 && relationScore!=0)?-relationWeightage/2:
			perRelation*normalizeNegativeValuesToZero(relationScore);
		
		float joinScore = getJoinScore(instructorData, studentData);
		float joinScoreTotal=(perJoin==0 && joinScore!=0)?-joinWeightage/2:
			perJoin*normalizeNegativeValuesToZero(joinScore);
		
		float groupByScore = compareProjection(instructorData.GroupByNodes, studentData.GroupByNodes);
		float groupByScoreTotal=(perGroupBy==0 && groupByScore!=0)?-groupByWeightage/2:
			perGroupBy*normalizeNegativeValuesToZero(groupByScore);
		
		float havingClauseScore = compareHavingClause(instructorData.getHavingClause(), studentData.getHavingClause());
		float havingClauseScoreTotal=(perHavingClause==0 && havingClauseScore!=0)?-havingClauseWeightage/2:
			perHavingClause*normalizeNegativeValuesToZero(havingClauseScore);
		
		float subQConnectiveScore = compare(instructorData.getSubQConnectives(),studentData.getSubQConnectives());
		float subQConnectiveScoreTotal=(perSubQConnective==0 && subQConnectiveScore!=0)?-subQConnectiveWeightage/2:
			perSubQConnective*normalizeNegativeValuesToZero(subQConnectiveScore);
		
		float aggregateScore = compareAggregates(instructorData.getAggregateList(), studentData.getAggregateList());
		float aggregateScoreTotal=(perAggregate==0 && aggregateScore!=0)?-aggregateWeightage/2:
			perAggregate*normalizeNegativeValuesToZero(aggregateScore);

		float setOperatorScore = compare(instructorData.getSetOpetators(),studentData.getSetOpetators());
		float setOperatorScoreTotal=(perSetOperator==0 && setOperatorScore!=0)?-setOperatorWeightage/2:
			perSetOperator*normalizeNegativeValuesToZero(setOperatorScore);
		
		float distinctOperatorScore = 0;
		
		if(isEvaluateDistinct){
			//if(instructorData.hasDistinct || studentData.hasDistinct){
				if(instructorData.hasDistinct && studentData.hasDistinct){
					distinctOperatorScore++;
				}else{
					//Even if any one query doesnot has Distinct - there is a mismatch
					distinctOperatorScore=distinctOperatorScore-0.5f;
				}
			//}
		}else{
			distinctOperatorScore++;
		}
		float distinctOperatorScoreTotal=(perDistinctOperator==0 && distinctOperatorScore!=0)?-distinctWeightage/2:
			perDistinctOperator*distinctOperatorScore;
		
		qInfo.studentPredicateMarks= perPredicate * predicateScore;
		qInfo.studentRelationsMarks = perRelation * relationScore;
		qInfo.studentProjectionMarks = perProjection * projectionScore;
		qInfo.studentInnerJoinMarks = perJoin * joinScore ;
		qInfo.studentGroupbyMarks =  perGroupBy * groupByScore;
		qInfo.studentHavingMarks =perHavingClause * havingClauseScore ;
		qInfo.studentSubqMarks = perSubQConnective * subQConnectiveScore;
		qInfo.studentAggregateMarks = perAggregate * aggregateScore ;
		qInfo.studentSetOperatorMarks = perSetOperator * setOperatorScore;
		qInfo.studentDistinctMarks = perDistinctOperator * distinctOperatorScore;
		
		qInfo.instructorPredicateMarks = perPredicate * uniquePredicates;
		qInfo.instructorRelationMarks = perRelation * uniqueRelations;
		qInfo.instructorProjectionMarks =  perProjection * uniqueProj;
		qInfo.instructorInnerJoinMarks = perJoin * instructorJoin;
		qInfo.instructorGroupbyMarks = perGroupBy * uniqueGroupBy;
		qInfo.instructorHavingMarks =  perHavingClause * uniqueHavingClause;
		qInfo.instructorSubqMarks = perSubQConnective * uniqueSubQConnective;
		qInfo.instructorAggregateMarks = perAggregate * uniqueAggregates;
		qInfo.instructorSetOperatorMarks = perSetOperator * uniqueSetOperators ;
		qInfo.instructorDistinctMarks = perDistinctOperator * uniqueDistinct;
		
		temp.add(qInfo);
		
		marks.SubqueryData = temp;
		
		float student =  normalizeNegativeValuesToZero(predicateScoreTotal + relationScoreTotal + projectionScoreTotal 
				+ joinScoreTotal + groupByScoreTotal + havingClauseScoreTotal + subQConnectiveScoreTotal + 
			aggregateScoreTotal + setOperatorScoreTotal + distinctOperatorScoreTotal);
				
		float instructor = perPredicate * uniquePredicates + perRelation * uniqueRelations 
				+ perProjection * uniqueProj + perJoin * instructorJoin + perGroupBy * uniqueGroupBy 
				+ perHavingClause * uniqueHavingClause + perSubQConnective * uniqueSubQConnective 
				+ perAggregate * uniqueAggregates + perSetOperator * uniqueSetOperators 
				+ perDistinctOperator * uniqueDistinct;
		
		float score = student/instructor * this.maxMarks;
		
		marks.Marks = this.Configuration.OuterQuery * score + this.Configuration.FromSubQueries * fromSubQuery.Marks + this.Configuration.WhereSubQueries * whereSubQuery.Marks;
		
		return marks;
	}
	
	
	
	// Compares all permutations of the queries and allocates the maximum mark.
	private MarkInfo compareListOfQueries(boolean isEvaluateDistinct, Vector<QueryData> master, Vector<QueryData> slave, int level){
		int result = 0;
		
		MarkInfo marks = new MarkInfo();
		ArrayList<QueryInfo> currentInfo = null;
		ArrayList<QueryInfo> maxInfo = new ArrayList<QueryInfo>();
				
		int masterCount = master.size();		
		int slaveCount = slave.size();
		
		ArrayList<ArrayList<Integer>> combinations = new ArrayList<ArrayList<Integer>>();
		if(masterCount < slaveCount){						
			generateCombinations(combinations, masterCount, slaveCount, new ArrayList<Integer>(), 0);
			
			result = 0;
			for(ArrayList<Integer> combination : combinations){
				int score = 0;
				currentInfo = new ArrayList<QueryInfo>();
				for(int i = 0; i < combination.size(); i++){					
					MarkInfo e = this.calculateScore(isEvaluateDistinct,master.get(i), slave.get(combination.get(i)), level);
					currentInfo.addAll(e.SubqueryData);
					score += e.Marks;
				}
				
				if(score > result){
					result = score;
					maxInfo = currentInfo;
				}
			}
		} else {						
			generateCombinations(combinations, slaveCount, masterCount, new ArrayList<Integer>(), 0);
			
			result = 0;
			for(ArrayList<Integer> combination : combinations){
				int score = 0;
				currentInfo = new ArrayList<QueryInfo>();
				for(int i = 0; i < combination.size(); i++){
					MarkInfo e = this.calculateScore(isEvaluateDistinct,master.get(combination.get(i)), slave.get(i), level);
					currentInfo.addAll(e.SubqueryData);
					score += e.Marks;
				}
				
				if(score > result){
					result = score;
					maxInfo = currentInfo;
				}
			}
		}
		
		logger.log(Level.INFO,combinations.toString());
		logger.log(Level.INFO,"size ="+combinations.size());
		
		marks.SubqueryData = maxInfo;
		marks.Marks = result/(Math.abs(masterCount - slaveCount) + 1); 
		return marks;
	}
	public boolean isInteger( String input )
	{
	   try 
	   {
	      Integer.parseInt( input );
	      return true;
	   }
	   catch( Exception e)
	   {
	      return false;
	   }
	}
	
	// Generates all the combinations
	private static void generateCombinations(ArrayList<ArrayList<Integer>> combinations, int limit,  int total, ArrayList<Integer> temp, int index){
		if(temp.size() == limit){
			combinations.add(new ArrayList<Integer>(temp));
			temp = new ArrayList<Integer>();
			return;
		}
		
		for(int j = index; j < total; j++){
			temp.add(j);
			generateCombinations(combinations, limit, total, temp, index + 1);
			temp.remove(index);
		}
	}
	
	private void cleanup(){
		try {
			this.InstructorQuery.getData().closeConn();
			this.StudentQuery.getData().closeConn();
		}
		catch(Exception ex){
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			//ex.printStackTrace();
		}
	}
}
