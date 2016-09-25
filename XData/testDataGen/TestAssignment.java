package testDataGen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.impl.sql.compile.DropTableNode;
import org.apache.derby.impl.sql.compile.StatementNode;
import com.google.gson.Gson;

import testDataGen.QueryStatusData.QueryStatus;
import util.DatabaseConnection;
import util.DatabaseHelper;
import util.FailedColumnValues;
import util.FailedDataSetValues;
import util.MyConnection;
import util.TableMap;

public class TestAssignment {

	private static Logger logger = Logger.getLogger(TestAssignment.class.getName());

	/**
	 * This method is used for Automated Testing of Instructor and student
	 * queries
	 * 
	 * @param assignment_id
	 * @return
	 * @throws Exception
	 */
	public List<String> evaluateAssignment(int assignment_id) throws Exception {
		String done = "SELECT assignment_id from xdata_assignment where assignment_id=?";
		String sep = "|";
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e1) {
			logger.log(Level.SEVERE, e1.getMessage(), e1);
			// TODO Auto-generated catch block
			// e1.printStackTrace();
		}
		logger.log(Level.INFO, "Testing started");
		try (Connection dbcon = MyConnection.getDatabaseConnection()) {
			if (dbcon != null) {
				logger.log(Level.INFO, "Connected successfullly");
			}
			try (PreparedStatement donestmt = dbcon.prepareStatement(done)) {
				donestmt.setInt(1, assignment_id);
				try (ResultSet rs = donestmt.executeQuery()) {
					if (!rs.next()) {
						logger.log(Level.INFO, "Assignment id not valid");
						return null;
					}
					// rs.next();
					List<String> corr_queries = new ArrayList<String>();
					List<Integer> corr_query_id = new ArrayList<Integer>();
					String updateString = "update xdata_student_queries set verifiedcorrect=? where assignment_id=? and question_id=? and rollnum=?";
					String QueryString = "select query_id,correctquery from xdata_qinfo where assignment_id =?";
					TestAnswer test = new TestAnswer();

					try (PreparedStatement qstmt = dbcon.prepareStatement(QueryString)) {
						qstmt.setInt(1, assignment_id);
						try (ResultSet Queries = qstmt.executeQuery()) {

							while (Queries.next()) {
								corr_queries.add(Queries.getString(2));
								corr_query_id.add(Queries.getInt(1));
							}
						} // try block for resultset ends
					} // try block for qstmt ends
					List<String> corrAns = new ArrayList<String>();
					for (int i = 0; i < corr_queries.size(); i++) {
						String filePath = Integer.toString(assignment_id);
						GenerateDataset_new g = new GenerateDataset_new(filePath);
						g.generateDatasetForQuery(assignment_id, corr_query_id.get(i), 1, "AutomatedTesting", "true",
								corr_queries.get(i), "");
						String StudQueryString = "select rollnum,querystring from xdata_student_queries where question_id=? and assignment_id=?";
						try (PreparedStatement studQueriesStmt = dbcon.prepareStatement(StudQueryString)) {
							studQueriesStmt.setInt(1, corr_query_id.get(i));
							studQueriesStmt.setInt(2, assignment_id);
							try (ResultSet StudQueries = studQueriesStmt.executeQuery()) {
								while (StudQueries.next()) {
									int flag = 0;
									PreparedStatement upstmt = dbcon.prepareStatement(updateString);
									// SQLParser sqlParser = new SQLParser();
									// StatementNode s = null;
									String qry = StudQueries.getString("querystring");
									String rolln = StudQueries.getString("rollnum");
									String OriginalQry = qry.replaceAll("''", "'");
									logger.log(Level.INFO, "queryString" + OriginalQry);
									qry = OriginalQry.trim().replaceAll("\n+", " ");
									qry = qry.trim().replaceAll(" +", " ");
									logger.log(Level.INFO, "Cleansed Query is " + qry);

									if (flag == 0) {
										// FailedDataSetValues
										// fdValue=test.testAnswer(assignment_id,corr_query_id.get(i),"AutomatedTesting",
										// OriginalQry ,"student",filePath);
										FailedDataSetValues fdValue = test.testAnswer(assignment_id,
												corr_query_id.get(i), "AutomatedTesting", OriginalQry, rolln, filePath,
												false);
										logger.log(Level.INFO, "ans:" + fdValue.getStatus());
										if (fdValue.getStatus().equalsIgnoreCase("failed")) {
											upstmt.setBoolean(1, false);
											corrAns.add("FAIL" + sep + assignment_id + sep + corr_query_id.get(i) + sep
													+ StudQueries.getString(2));
										} else {
											upstmt.setBoolean(1, true);
											corrAns.add("PASS" + sep + assignment_id + sep + corr_query_id.get(i) + sep
													+ StudQueries.getString(2));
										}
									}
									upstmt.setInt(2, assignment_id);
									upstmt.setInt(3, corr_query_id.get(i));
									upstmt.setString(4, StudQueries.getString(1));
									upstmt.executeUpdate();

								}
							} // try block for StudQueries
						} // try block for studQueriesStmt ends

					}
					for (int ind = 0; ind < corrAns.size(); ind++) {
						logger.log(Level.INFO, "Result:" + corrAns.get(ind));
					}
				
					return corrAns;
				} // try block for result set ends
			} // try block to close donestmt ends
		} catch (SQLException ex) {
			System.err.println("SQLEEException: " + ex.getMessage());
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			// ex.printStackTrace();
		}
		return null;
	}

	/**
	 * This method is invoked during the late submission of answers Triggered by
	 * instructor.
	 * 
	 */
	public boolean evaluateLateSubmission(Connection dbcon, Connection testCon, String[] args) throws SQLException {
		boolean status = false;

		int assignment_id = Integer.parseInt(args[0]);

		String rollNum = args[1];
		String courseId = args[2];
		int noOfQuestions = Integer.parseInt(args[3]);
		int marksToBeReduced = Integer.parseInt(args[4]);
		PopulateTestData populateTestData = new PopulateTestData();
		TestAnswer test = new TestAnswer();
		// int question_id = 1;
		try {
			for (int ij = 1; ij < noOfQuestions; ij++) {
				QueryStatus queryStatus = QueryStatus.Correct;
				boolean queryExists = false;
				GenerateCVC1 cvc = new GenerateCVC1();
				populateTestData.deleteAllTempTablesFromTestUser(testCon);
				cvc.initializeConnectionDetails(assignment_id, ij, 1, courseId);
				TableMap tm = cvc.getTableMap();

				String json1 = "";
				int question_id = ij;
				String updateString = "update xdata_student_queries set tajudgement=?, result = ? "
						+ "where assignment_id=? and question_id = ? and rollnum=? and course_id=?";

				PreparedStatement upstmt = dbcon.prepareStatement(updateString);

				// Get student answer for each question
				String getAnswersString = "select * from xdata_student_queries where assignment_id=? and rollnum=? and course_id=? and question_id=?";
				try (PreparedStatement getstmt = dbcon.prepareStatement(getAnswersString)) {
					getstmt.setInt(1, assignment_id);
					getstmt.setString(2, rollNum);
					getstmt.setString(3, courseId);
					getstmt.setInt(4, ij);

					try (ResultSet rs = getstmt.executeQuery()) {
						while (rs.next()) {
							int flag = 0;
							HashSet<String> hs = new HashSet<String>();
							Gson gson = new Gson();
							String json = "";
							boolean isLateSubmission = rs.getBoolean("late_submission_flag");
							// Start of student Query evaluation for a question
							// Do run student query against the default dataset
							// and datasets generated by XData
							String testQuery = rs.getString("querystring");
							if (testQuery != null && !testQuery.isEmpty()) {
								queryExists = true;
								try {
									testQuery = testQuery.trim().replaceAll("\n+", " ");
									testQuery = testQuery.trim().replaceAll(" +", " ");

									populateTestData.deleteAllTempTablesFromTestUser(testCon);
									populateTestData.createTempTables(testCon, assignment_id, question_id);
									try (PreparedStatement testStatement = testCon.prepareStatement(testQuery)) {
										testStatement.executeQuery();
									}
								} catch (Exception e) {
									logger.log(Level.SEVERE, e.getMessage(), e);
									upstmt.setBoolean(1, true);
									flag = 1;
								}

								if (flag == 0) {
									String ans = "";
									try {
										String queryId = "A" + assignment_id + "Q" + question_id + "S" + 1;
										// FailedDataSetValues failedDs =
										// test.testAnswerMatchAllOption(assignment_id,question_id,courseId,
										// OriginalQry, rollNum,
										// "4/"+courseId+"/"+queryId);
										FailedDataSetValues failedDs = test.testAnswer(assignment_id, question_id,
												courseId, testQuery, rollNum, "4/" + courseId + "/" + queryId,
												isLateSubmission);

										if (failedDs.getStatus().equalsIgnoreCase("Failed")) {
											Map<String, Map<String, ArrayList<String>>> failedStudDataMap = new HashMap<String, Map<String, ArrayList<String>>>();
											Map<String, Map<String, ArrayList<String>>> failedInstrDataMap = new HashMap<String, Map<String, ArrayList<String>>>();

											hs.add(failedDs.getDataSetIdList().get(0));
											String add_datasets = "insert into xdata_detectdataset values (?,?,?,?,?,?,?)";
											try (PreparedStatement add = dbcon.prepareStatement(add_datasets)) {

												for (int i = 0; i < failedDs.getDataSetIdList().size(); i++) {

													String dataset = failedDs.getDataSetIdList().get(i);
													// Load the failed dataset
													// into temp tables
													ArrayList<FailedColumnValues> failedList = new ArrayList<FailedColumnValues>();
													Map<String, ArrayList<String>> failedColMap = new HashMap<String, ArrayList<String>>();
													logger.log(Level.INFO, "Inside dataset list");

													// p.fetchAndPopulateTestDatabase(dbcon,
													// testCon,
													// assignment_id,question_id,1,
													// course_Id, dataset, tm);
													if (dataset.startsWith("DS")) {
														if (dataset.equalsIgnoreCase("DS_Default")) {
															populateTestData.deleteAllTempTablesFromTestUser(testCon);
															populateTestData.createTempTableData(dbcon, testCon,
																	assignment_id, question_id, courseId);
														} else {
															populateTestData.fetchAndPopulateTestDatabase(dbcon,
																	testCon, assignment_id, question_id, 1, courseId,
																	dataset, tm);
														}
													} else {
														populateTestData.createTempTableWithDefaultData(dbcon, testCon,
																assignment_id, question_id, courseId, dataset);
													}
													failedDs = this.getStudentOutput(testCon, dataset, testQuery,
															failedList, failedDs, failedStudDataMap, false);
													failedDs = this.getInstructorOutput(testCon, dataset,
															failedDs.getInstrQuery(), failedList, failedDs,
															failedInstrDataMap, false);

													logger.log(Level.INFO, "Answer is :" + ans);
													failedDs.getDsValueMap().put(dataset, failedList);
													json = gson.toJson(failedDs);
													logger.log(Level.INFO, "JSON VALUE OF FAILED DATASETS :" + json);
													add.setString(1, rollNum);
													add.setString(2, queryId);
													add.setString(3, dataset);
													add.setString(4, json);
													add.setInt(5, assignment_id);
													add.setInt(6, question_id);
													add.setString(7, courseId);
													// add.setString(4, );
													add.executeUpdate();

												}
											}
											json1 = gson.toJson(failedDs);
											upstmt.setBoolean(1, true);
										} else {
											queryStatus = QueryStatus.Correct;
											upstmt.setBoolean(1, true);

										}
										upstmt.setString(2, json);
										cvc.closeConn();
									} catch (Exception e) {
										logger.log(Level.SEVERE, "Exception caught here:", e);
										// e.printStackTrace();
										queryStatus = QueryStatus.Error;
										upstmt.setBoolean(1, true);

									}

								}
								upstmt.setString(2, json);

								upstmt.setInt(3, assignment_id);
								upstmt.setInt(4, question_id);
								// upstmt.setInt(3, question_id);
								upstmt.setString(5, testQuery);
								upstmt.setString(6, courseId);
								upstmt.executeUpdate();
							} else {

								upstmt.setBoolean(1, false);
								upstmt.setString(2, null);
								upstmt.setInt(3, assignment_id);
								upstmt.setInt(4, question_id);
								upstmt.setString(5, testQuery);
								upstmt.setString(6, courseId);

								upstmt.executeUpdate();
							}

							// If student answer exists

							// End of student Query evaluation for a question
						}
					}
				}
				if (!queryExists) {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollNum
							+ "' and course_id= '" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
					queryStatus = QueryStatus.NoDataset;
				} else if (queryStatus == QueryStatus.Correct) {
					int marksAfterReduction = 100 - marksToBeReduced;
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = true,score="
							+ marksAfterReduction + " where assignment_id = '" + assignment_id + "' and question_id='"
							+ question_id + "' and rollnum = '" + rollNum + "'and course_id= '" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
				} else {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = false, result=? where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollNum
							+ "'and course_id= '" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.setString(1, json1);
						pstmt3.executeUpdate();
					}
				}
				if (ij == noOfQuestions) {
					status = true;
					break;
				}
			} // End of for loop for number of questions
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught at end of TestAssignment.evaluate function :", e);
		}

		return status;
	}

	/**
	 * This method tests the passed student query across the instructor answer and with out saving anything in database. 
	 * 
	 * @param dbcon
	 * @param testCon
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	public FailedDataSetValues evaluateGuestAnswer(Connection dbcon, Connection testCon, String[] args) throws SQLException {
		String StudQueryString = args[4];
		TestAnswer test = new TestAnswer();
		QueryStatus queryStatus = QueryStatus.Correct;
		boolean queryExists = false;
		QueryStatusData statusData = new QueryStatusData();
		PopulateTestData populateTestData = new PopulateTestData();

		int assignment_id = Integer.parseInt(args[0]);
		int question_id = Integer.parseInt(args[1]);

		String qId = "A" + assignment_id + "Q" + question_id + "S" + 1;
		String rollNum = args[2];
		String courseId = args[3];
		String json = "";
		int flag = 0;
		FailedDataSetValues failedDs = new FailedDataSetValues();
		Gson gson = new Gson();
		
		String testQuery = StudQueryString;
		
		try {
			logger.log(Level.INFO, "queryString" + testQuery);
			testQuery = testQuery.trim().replaceAll("\n+", " ");
			testQuery = testQuery.trim().replaceAll(" +", " ");

			
			populateTestData.deleteAllTempTablesFromTestUser(testCon);
			populateTestData.createTempTables(testCon, assignment_id, question_id);
			try (PreparedStatement testStatement = testCon.prepareStatement(testQuery)) {
				testStatement.executeQuery();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			// e.printStackTrace();
			statusData.ErrorMessage = e.getMessage();
			queryStatus = QueryStatus.Error;			
			flag = 1;
		}
		if (flag == 0) {
			String ans = "";
			HashSet<String> hs = new HashSet<String>();
			try {
				String queryId = "A" + assignment_id + "Q" + question_id + "S" + 1;
				failedDs = test.testAnswer(assignment_id, question_id, courseId,
						StudQueryString, rollNum, "4/" + courseId + "/" + queryId, false);
				GenerateCVC1 cvc = new GenerateCVC1();
				cvc.initializeConnectionDetails(assignment_id, question_id, 1, courseId);
				TableMap tm = cvc.getTableMap();

				if (failedDs.getStatus().equalsIgnoreCase("Failed")) {
					
					// Get DS0
					Map<String, Map<String, ArrayList<String>>> failedStudDataMap = new HashMap<String, Map<String, ArrayList<String>>>();
					Map<String, Map<String, ArrayList<String>>> failedInstrDataMap = new HashMap<String, Map<String, ArrayList<String>>>();

					hs.add(failedDs.getDataSetIdList().get(0));
				

					for (int i = 0; i < failedDs.getDataSetIdList().size(); i++) {

						String dataset = failedDs.getDataSetIdList().get(i);
						// Load the failed dataset into
						// temp tables
						ArrayList<FailedColumnValues> failedList = new ArrayList<FailedColumnValues>();
						Map<String, ArrayList<String>> failedColMap = new HashMap<String, ArrayList<String>>();
						logger.log(Level.INFO, "Inside dataset list");

						if (dataset.startsWith("DS")) {
							if (dataset.equalsIgnoreCase("DS_Default")) {
								populateTestData.deleteAllTempTablesFromTestUser(testCon);
								populateTestData.createTempTableData(dbcon, testCon,
										assignment_id, question_id, courseId);
							} else {
								populateTestData.fetchAndPopulateTestDatabase(dbcon, testCon,
										assignment_id, question_id, 1, courseId, dataset, tm);
							}

						} else {
							populateTestData.createTempTableWithDefaultData(dbcon, testCon,
									assignment_id, question_id, courseId, dataset);
							
						}
						failedDs = this.getStudentOutput(testCon, dataset, StudQueryString,
								failedList, failedDs, failedStudDataMap, false);
						failedDs = this.getInstructorOutput(testCon, dataset,
								failedDs.getInstrQuery(), failedList, failedDs,
								failedInstrDataMap, false);

						logger.log(Level.INFO, "Answer is :" + ans);
						failedDs.getDsValueMap().put(dataset, failedList);
						json = gson.toJson(failedDs);
						logger.log(Level.INFO, "JSON VALUE OF FAILED DATASETS :" + json);

						queryStatus = QueryStatus.Incorrect;
					}
				}
			}catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				//statusData.ErrorMessage = e.getMessage();
				failedDs.setErrorMessage(e.getMessage());
				failedDs.setStatus(QueryStatus.Error.toString());			
				flag = 1;
			}
			
		}if (StudQueryString != null && StudQueryString.equals("")) {
			failedDs.setStatus(QueryStatus.NoDataset.toString());
		}
		return failedDs;
		
	}
	
	/**
	 * This method is called for evaluating student query in interactive mode
	 * 
	 * @param dbcon
	 * @param testCon
	 * @param args
	 * @return
	 * @throws SQLException
	 * @throws StandardException
	 */
	public QueryStatusData evaluateQuestion(Connection dbcon, Connection testCon, String[] args) throws SQLException {
		String StudQueryString = "select querystring from xdata_student_queries where " + "assignment_id =?  and "
				+ "question_id = ? and rollnum=?";
		TestAnswer test = new TestAnswer();
		QueryStatus queryStatus = QueryStatus.Correct;
		boolean queryExists = false;
		QueryStatusData statusData = new QueryStatusData();
		PopulateTestData populateTestData = new PopulateTestData();

		int assignment_id = Integer.parseInt(args[0]);
		int question_id = Integer.parseInt(args[1]);

		String qId = "A" + assignment_id + "Q" + question_id + "S" + 1;
		String rollNum = args[2];
		String courseId = args[3];
		String json1 = "";
		// try(PreparedStatement studQueriesStmt =
		// dbcon.prepareStatement(StudQueryString)){
		String updateString = "update xdata_student_queries set tajudgement=?, result = ? "
				+ "where assignment_id=? and question_id = ? and rollnum=? and course_id=?";

		try (PreparedStatement upstmt = dbcon.prepareStatement(updateString)) {
			try (PreparedStatement studQueriesStmt = dbcon.prepareStatement(StudQueryString)) {

				studQueriesStmt.setInt(1, assignment_id);
				studQueriesStmt.setInt(2, question_id);
				studQueriesStmt.setString(3, rollNum);
				logger.log(Level.INFO, studQueriesStmt.toString());
				try (ResultSet StudQueries = studQueriesStmt.executeQuery()) {
					HashSet<String> hs = new HashSet<String>();
					if (StudQueries.next()) {
						Gson gson = new Gson();
						String json = "";
						int flag = 0;
						hs.clear();
						// SQLParser sqlParser = new SQLParser();
						CCJSqlParserManager pm = new CCJSqlParserManager();
						StatementNode s = null;
						String qry = StudQueries.getString("querystring");
						String testQuery = StudQueries.getString("querystring");
						
						if (qry != null && !qry.isEmpty() && testQuery != null && !testQuery.isEmpty()) {
							queryExists = true;
							String OriginalQry = qry.replaceAll("''", "'");
							logger.log(Level.INFO, "queryString" + OriginalQry);
							qry = OriginalQry.trim().replaceAll("\n+", " ");
							qry = qry.trim().replaceAll(" +", " ");
							logger.log(Level.INFO, "Cleansed Query is " + qry);
							qry = qry.replace("NATURAL LEFT OUTER", "NATURAL");
							qry = qry.replace("NATURAL RIGHT OUTER", "NATURAL");
							try {
								logger.log(Level.INFO, "queryString" + testQuery);
								testQuery = testQuery.trim().replaceAll("\n+", " ");
								testQuery = testQuery.trim().replaceAll(" +", " ");

								
								populateTestData.deleteAllTempTablesFromTestUser(testCon);
								populateTestData.createTempTables(testCon, assignment_id, question_id);
								try (PreparedStatement testStatement = testCon.prepareStatement(testQuery)) {
									testStatement.executeQuery();
								}
							} catch (Exception e) {
								logger.log(Level.SEVERE, e.getMessage(), e);
								// e.printStackTrace();
								statusData.ErrorMessage = e.getMessage();
								queryStatus = QueryStatus.Error;
								upstmt.setBoolean(1, true);
								flag = 1;
							}
							if (s instanceof DropTableNode) {
								queryStatus = QueryStatus.Incorrect;
								flag = 1;
							}
							if (flag == 0) {
								String ans = "";
								try {
									String queryId = "A" + assignment_id + "Q" + question_id + "S" + 1;
									FailedDataSetValues failedDs = test.testAnswer(assignment_id, question_id, courseId,
											OriginalQry, rollNum, "4/" + courseId + "/" + queryId, false);
									GenerateCVC1 cvc = new GenerateCVC1();
									cvc.initializeConnectionDetails(assignment_id, question_id, 1, courseId);
									TableMap tm = cvc.getTableMap();

									if (failedDs.getStatus().equalsIgnoreCase("Failed")) {
										
										// Get DS0
										Map<String, Map<String, ArrayList<String>>> failedStudDataMap = new HashMap<String, Map<String, ArrayList<String>>>();
										Map<String, Map<String, ArrayList<String>>> failedInstrDataMap = new HashMap<String, Map<String, ArrayList<String>>>();

										hs.add(failedDs.getDataSetIdList().get(0));
										String add_datasets = "insert into xdata_detectdataset values (?,?,?,?,?,?,?)";
										try (PreparedStatement add = dbcon.prepareStatement(add_datasets)) {

											for (int i = 0; i < failedDs.getDataSetIdList().size(); i++) {

												String dataset = failedDs.getDataSetIdList().get(i);
												// Load the failed dataset into
												// temp tables
												ArrayList<FailedColumnValues> failedList = new ArrayList<FailedColumnValues>();
												Map<String, ArrayList<String>> failedColMap = new HashMap<String, ArrayList<String>>();
												logger.log(Level.INFO, "Inside dataset list");

												if (dataset.startsWith("DS")) {
													if (dataset.equalsIgnoreCase("DS_Default")) {
														populateTestData.deleteAllTempTablesFromTestUser(testCon);
														populateTestData.createTempTableData(dbcon, testCon,
																assignment_id, question_id, courseId);
													} else {
														populateTestData.fetchAndPopulateTestDatabase(dbcon, testCon,
																assignment_id, question_id, 1, courseId, dataset, tm);
													}

												} else {
													populateTestData.createTempTableWithDefaultData(dbcon, testCon,
															assignment_id, question_id, courseId, dataset);
													
												}
												failedDs = this.getStudentOutput(testCon, dataset, OriginalQry,
														failedList, failedDs, failedStudDataMap, false);
												failedDs = this.getInstructorOutput(testCon, dataset,
														failedDs.getInstrQuery(), failedList, failedDs,
														failedInstrDataMap, false);

												logger.log(Level.INFO, "Answer is :" + ans);
												failedDs.getDsValueMap().put(dataset, failedList);
												json = gson.toJson(failedDs);
												logger.log(Level.INFO, "JSON VALUE OF FAILED DATASETS :" + json);

												add.setString(1, rollNum);
												add.setString(2, queryId);
												add.setString(3, dataset);
												add.setString(4, json);
												add.setInt(5, assignment_id);
												add.setInt(6, question_id);
												add.setString(7, courseId);
												// add.setString(4, );
												add.executeUpdate();
												queryStatus = QueryStatus.Incorrect;
											}

										}
										json1 = gson.toJson(failedDs);
										upstmt.setBoolean(1, true);
									} else {
										queryStatus = QueryStatus.Correct;
										upstmt.setBoolean(1, true);

									}
									upstmt.setString(2, json);
									cvc.closeConn();
								} catch (Exception e) {
									logger.log(Level.SEVERE, "Exception caught here:", e);
									// e.printStackTrace();
									queryStatus = QueryStatus.Error;
									upstmt.setBoolean(1, true);
								}

							}
							upstmt.setString(2, json);

							upstmt.setInt(3, assignment_id);
							upstmt.setInt(4, question_id);
							// upstmt.setInt(3, question_id);
							upstmt.setString(5, StudQueries.getString(1));
							upstmt.setString(6, courseId);
							upstmt.executeUpdate();
						} else {

							upstmt.setBoolean(1, false);
							upstmt.setString(2, null);
							upstmt.setInt(3, assignment_id);
							upstmt.setInt(4, question_id);
							upstmt.setString(5, StudQueries.getString(1));
							upstmt.setString(6, courseId);

							upstmt.executeUpdate();
						}
					}
				} // close StudQueries resultset
			} // close Studqueries statement

		}

		if (!queryExists) {
			String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id = '"
					+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollNum
					+ "' and course_id= '" + courseId + "'";
			try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
				pstmt3.executeUpdate();
			}
			queryStatus = QueryStatus.NoDataset;
		} else if (queryStatus == QueryStatus.Correct) {
			String qryUpdate = "update xdata_student_queries set verifiedcorrect = true,score = 100 where assignment_id = '"
					+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollNum
					+ "'and course_id= '" + courseId + "'";
			try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
				pstmt3.executeUpdate();
			}
		} else {
			String qryUpdate = "update xdata_student_queries set verifiedcorrect = false, result=? where assignment_id = '"
					+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollNum
					+ "'and course_id= '" + courseId + "'";
			try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
				pstmt3.setString(1, json1);
				pstmt3.executeUpdate();
			}
		}
		statusData.Status = queryStatus;
		return statusData;
	}

	/**
	 * This method return the student query's output for the loaded data set
	 * Used to get results of student query on the failed data set for
	 * displaying failed test cases
	 * 
	 * @param testCon
	 * @param dataSetId
	 * @param OriginalQry
	 * @param failedList
	 * @param failedDs
	 * @param failedStudDataMap
	 * @param isDefaultDS
	 * @return
	 * @throws SQLException
	 */
	public FailedDataSetValues getStudentOutput(Connection testCon, String dataSetId, String OriginalQry,
			ArrayList<FailedColumnValues> failedList, FailedDataSetValues failedDs,
			Map<String, Map<String, ArrayList<String>>> failedStudDataMap, boolean isDefaultDS) {
		Map<String, ArrayList<String>> failedColMap = new HashMap<String, ArrayList<String>>();

		try {
			// execute student query on the failed DS and get the output
			try (PreparedStatement pp = testCon.prepareStatement(OriginalQry)) {
				try (ResultSet rr = pp.executeQuery()) {
					ResultSetMetaData metadata = rr.getMetaData();
					int no_of_columns = metadata.getColumnCount();
					String result = "";
					String columnName = "";

					for (int cl = 1; cl <= no_of_columns; cl++) {
						ArrayList<String> values = new ArrayList<String>();
						FailedColumnValues failedColumns = new FailedColumnValues();
						// out_assignment.println("<th>"+metadata.getColumnLabel(cl)+"</th>");
						// result+=metadata.getColumnName(cl)+"@@";
						// failedDs.getColValueMap().add(metadata.getColumnName(cl));
						columnName = metadata.getColumnName(cl);
						try (ResultSet rr1 = pp.executeQuery()) {
							metadata = rr1.getMetaData();
							while (rr1.next()) {
								// while(rr.next())
								// {
								int type = metadata.getColumnType(cl);
								values.add(rr1.getString(cl));

							}
							failedColumns.setColumnName(metadata.getColumnName(cl));
							failedColumns.setValues(values);
							failedColMap.put(columnName, values);
							failedList.add(failedColumns);
						}
					}
				} // try block to close rr resultset
			} // try block for pp statement ends
		} catch (SQLException e) {
			logger.log(Level.SEVERE,
					"Student test case output generation fails because of syntax error :" + e.getMessage(), e);
			
		}
		failedStudDataMap.put(dataSetId, failedColMap);
		// if(!isDefaultDS){
		failedDs.setStudentQueryOutput(failedStudDataMap);
		// }else if(isDefaultDS){

		// failedDs.setStudentQueryDefaultDSOutput(failedStudDataMap);
		// }
		return failedDs;
	}

	/**
	 * This method return the instructor query's output for the loaded dataset
	 * Used to get results of instructor query on the failed dataset for
	 * displaying failed test cases
	 * 
	 * @param testCon
	 * @param dataSetId
	 * @param OriginalQry
	 * @param failedList
	 * @param failedDs
	 * @param failedStudDataMap
	 * @param isDefaultDS
	 * @return
	 * @throws SQLException
	 */
	public FailedDataSetValues getInstructorOutput(Connection testCon, String dataSetId, String OriginalQry,
			ArrayList<FailedColumnValues> failedList, FailedDataSetValues failedDs,
			Map<String, Map<String, ArrayList<String>>> failedInstrDataMap, boolean isDefaultDS) {

		Map<String, ArrayList<String>> failedInstrColMap = new HashMap<String, ArrayList<String>>();
		try {
			try (PreparedStatement pp = testCon.prepareStatement(OriginalQry)) {
				try (ResultSet rr = pp.executeQuery()) {
					ResultSetMetaData metadata = rr.getMetaData();
					int no_of_columns = metadata.getColumnCount();
					String result = "";
					String columnName = "";
					// ArrayList <String> values = new ArrayList<String>();
					for (int cl = 1; cl <= no_of_columns; cl++) {
						ArrayList<String> values = new ArrayList<String>();
						FailedColumnValues failedColumns = new FailedColumnValues();
						// out_assignment.println("<th>"+metadata.getColumnLabel(cl)+"</th>");
						// result+=metadata.getColumnName(cl)+"@@";
						// failedDs.getColValueMap().add(metadata.getColumnName(cl));
						columnName = metadata.getColumnName(cl);
						try (ResultSet rr1 = pp.executeQuery()) {
							metadata = rr1.getMetaData();
							while (rr1.next()) {
								int type = metadata.getColumnType(cl);
								values.add(rr1.getString(cl));

							}

							failedColumns.setInstrColumnName(metadata.getColumnName(cl));
							failedColumns.setInstrValues(values);
							failedInstrColMap.put(columnName, values);
							//failedList.add(failedColumns);
						}
					}

				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Instructor output fails because of syntax error :" + e.getMessage(), e);
			//failedList.add(failedInstrColMap);
		}
		failedInstrDataMap.put(dataSetId, failedInstrColMap);
		// if(!isDefaultDS){
		failedDs.setInstrQueryOutput(failedInstrDataMap);
		// }else if(isDefaultDS){
		// failedDs.setInstrQueryDefaultDSOutput(failedInstrDataMap);
		// }
		return failedDs;
	}

	/**
	 * This method is called when the instructor evaluates the student queries
	 * after submission. Triggered from instructor during evaluation.
	 * 
	 * @param assignment_id
	 * @param question_id
	 * @throws SQLException
	 * @throws StandardException
	 */
	void evaluateQuestion(int assignment_id, int question_id, String course_Id) {

		String StudQueryString = "select rollnum,querystring,late_submission_flag from "
				+ "(select * from xdata_student_queries where assignment_id=? and question_id=? AND course_id=?) as C "
				+ "inner join xdata_users on C.rollnum= xdata_users.internal_user_id "
				+ "inner join xdata_roles xdr on xdata_users.internal_user_id = xdr.internal_user_id "
				+ "and xdr.role='student'";

		String resultToUpdate = "&&";
		String oldQueryId = "A" + assignment_id + "Q" + question_id + "S" + 1;// Query
																				// Id
																				// by
																				// student
																				// will
																				// always
																				// be
																				// 1
		TestAnswer test = new TestAnswer();
		try (Connection testCon = new DatabaseConnection().getTesterConnection(assignment_id)) {
			// dbcon = MyConnection.getExistingDatabaseConnection();
			try (Connection dbcon = MyConnection.getDatabaseConnection()) {
				if (dbcon != null) {
					logger.log(Level.INFO, "Connected successfullly");
				}
				String updateString = "update xdata_student_queries set tajudgement=?, result = ?  where assignment_id=? and question_id = ? and rollnum=? and course_id=?";
				try (PreparedStatement upstmt = dbcon.prepareStatement(updateString)) {
					try (PreparedStatement studQueriesStmt = dbcon.prepareStatement(StudQueryString)) {
						studQueriesStmt.setInt(1, assignment_id);
						studQueriesStmt.setInt(2, question_id);
						studQueriesStmt.setString(3, course_Id);

						logger.log(Level.INFO, "A" + assignment_id + "Q" + question_id + "S" + 1);
						// studQueriesStmt.setInt(2, assignment_id);
						try (ResultSet StudQueries = studQueriesStmt.executeQuery()) {

							HashSet<String> hs = new HashSet<String>();
							GenerateCVC1 cvc = new GenerateCVC1();
							cvc.initializeConnectionDetails(assignment_id, question_id, 1, course_Id);
							TableMap tm = cvc.getTableMap();
							cvc.closeConn();

							PopulateTestData p = new PopulateTestData();
							p.deleteAllTempTablesFromTestUser(testCon);
							p.createTempTables(testCon, assignment_id, question_id);
							// HashMap< String, String> hm=new HashMap<String,
							// String>();
							while (StudQueries.next()) {
								Gson gson = new Gson();
								String json = "";
								int flag = 0;
								hs.clear();
								// SQLParser sqlParser = new SQLParser();
								CCJSqlParserManager pm = new CCJSqlParserManager();

								StatementNode s = null;
								String qry = StudQueries.getString("querystring");
								boolean isLateSubmission = StudQueries.getBoolean("late_submission_flag");
								if (qry != null && !qry.isEmpty()) {
									String OriginalQry = qry.replaceAll("''", "'");
									logger.log(Level.INFO, "queryString" + OriginalQry);
									qry = OriginalQry.trim().replaceAll("\n+", " ");
									qry = qry.trim().replaceAll(" +", " ");
									qry = qry.replace("NATURAL LEFT OUTER", "NATURAL");
									qry = qry.replace("NATURAL RIGHT OUTER", "NATURAL");
									// if(qry.toLowerCase().contains("year")){
									// qry=qry.replaceAll("year","year1");
									// qry=qry.replaceAll("Year","year1");
									// qry=qry.replaceAll("YEAR","year1");
									// }
									logger.log(Level.INFO, "Cleansed Query is " + qry);
									try {
										// Statement stmt = pm.parse(new
										// StringReader(qry));
										// String modQuery =
										// TestAnswer.parseWithAsQueryJSQL(qry);//preParseQuery(testQuery);
										// if(modQuery != null ){
										// qry = modQuery;
										// }
										logger.log(Level.INFO, "queryString" + qry);
										qry = qry.trim().replaceAll("\n+", " ");
										qry = qry.trim().replaceAll(" +", " ");
										// Statement stmt = pm.parse(new
										// StringReader(qry));
									} catch (Exception e) {
										logger.log(Level.SEVERE, e.getMessage(), e);
										// e.printStackTrace();
										// This is set to true to say the
										// question is evaluated
										upstmt.setBoolean(1, true);
										flag = 1;
									}
									// TODO : To be corrected with JSQL PARSER
									/*
									 * if(s instanceof DropTableNode) {
									 * upstmt.setBoolean(1, false); flag=1; }
									 */
									if (flag == 0) {
										String ans = "";
										try {

											FailedDataSetValues failedDs = test.testAnswer(assignment_id, question_id,
													course_Id, OriginalQry, StudQueries.getString("rollnum"),
													"4/" + course_Id + "/" + oldQueryId, isLateSubmission);
											logger.log(Level.INFO, "Answer is :" + ans);

											if (failedDs.getStatus().equalsIgnoreCase("Failed")) {
												// This is set to true to say
												// the question is evaluated
												upstmt.setBoolean(1, true);
												// Get DS0
												hs.add(failedDs.getDataSetIdList().get(0));
												Map<String, Map<String, ArrayList<String>>> failedStudDataMap = new HashMap<String, Map<String, ArrayList<String>>>();
												Map<String, Map<String, ArrayList<String>>> failedInstrDataMap = new HashMap<String, Map<String, ArrayList<String>>>();

												String add_datasets = "insert into xdata_detectdataset values (?,?,?,?,?,?,?)";

												try (PreparedStatement add = dbcon.prepareStatement(add_datasets)) {
													for (int i = 0; i < failedDs.getDataSetIdList().size(); i++) {
														ArrayList<FailedColumnValues> failedList = new ArrayList<FailedColumnValues>();
														Map<String, ArrayList<String>> failedColMap = new HashMap<String, ArrayList<String>>();

														logger.log(Level.INFO, "Inside operator");
														String dataset = failedDs.getDataSetIdList().get(i);
														// p.fetchAndPopulateTestDatabase(dbcon,
														// testCon,
														// assignment_id,question_id,1,
														// course_Id, dataset,
														// tm);
														if (dataset.startsWith("DS")) {
															if (dataset.equalsIgnoreCase("DS_Default")) {
																p.createTempTableData(dbcon, testCon, assignment_id,
																		question_id, course_Id);

															} else {
																p.fetchAndPopulateTestDatabase(dbcon, testCon,
																		assignment_id, question_id, 1, course_Id,
																		dataset, tm);

															}
															// failedDs =
															// this.getStudentOutput(testCon,
															// dataset,
															// OriginalQry,
															// failedList,
															// failedDs,
															// failedStudDataMap,false);
															// failedDs=this.getInstructorOutput(testCon,
															// dataset,
															// failedDs.getInstrQuery(),
															// failedList,
															// failedDs,
															// failedInstrDataMap,false);

														} else {
															p.createTempTableWithDefaultData(dbcon, testCon,
																	assignment_id, question_id, course_Id, dataset);

															// failedDs =
															// this.getStudentOutput(testCon,
															// dataset,
															// OriginalQry,
															// failedList,
															// failedDs,
															// failedStudDataMap,true);
															// failedDs=this.getInstructorOutput(testCon,
															// dataset,
															// failedDs.getInstrQuery(),
															// failedList,
															// failedDs,
															// failedInstrDataMap,true);
														}
														failedDs = this.getStudentOutput(testCon, dataset, OriginalQry,
																failedList, failedDs, failedStudDataMap, false);
														failedDs = this.getInstructorOutput(testCon, dataset,
																failedDs.getInstrQuery(), failedList, failedDs,
																failedInstrDataMap, false);
														logger.log(Level.INFO, "\n connection val: " + dbcon);
														failedDs.getDsValueMap().put(dataset, failedList);
														json = gson.toJson(failedDs);
														logger.log(Level.INFO, "JSON VALUE OF DataSetValue field :"
																+ json + "\n $$$$$$$$$$$  connection :7 : " + dbcon);

														add.setString(1, StudQueries.getString("rollnum"));
														add.setString(2, oldQueryId);
														add.setString(3, dataset);
														add.setString(4, json);
														add.setInt(5, assignment_id);
														add.setInt(6, question_id);
														add.setString(7, course_Id);
														// add.setString(4, );
														add.executeUpdate();
														// } // While each DS
														// loop ends
													} // for each DSloop ends
												} // try block for statement
													// ends
											} else {
												upstmt.setBoolean(1, true);
											}
											json = gson.toJson(failedDs);
											logger.log(Level.INFO, "JSON VALUE OF DataSetValue field :" + json
													+ "\n connection : " + dbcon);

											upstmt.setString(2, json);

										} catch (Exception e) {
											logger.log(Level.SEVERE, "Exception caught here: " + e.getMessage(), e);
											// e.printStackTrace();
											upstmt.setBoolean(1, true);
										}
									}
									upstmt.setString(2, json);
									// upstmt.setString(3,course_Id);
									if (flag == 1) {
										DatabaseHelper.InsertIntoScores(dbcon, assignment_id, question_id, 1, course_Id,
												100, StudQueries.getString("rollnum"), "", 0F);
									}
									upstmt.setInt(3, assignment_id);
									upstmt.setInt(4, question_id);
									// upstmt.setInt(3, question_id);
									upstmt.setString(5, StudQueries.getString(1));
									upstmt.setString(6, course_Id);
									upstmt.executeUpdate();
								} else {
									DatabaseHelper.InsertIntoScores(dbcon, assignment_id, question_id, 1, course_Id,
											100, StudQueries.getString("rollnum"), "", 0F);
									upstmt.setBoolean(1, false);
									upstmt.setString(2, null);
									upstmt.setInt(3, assignment_id);
									upstmt.setInt(4, question_id);
									upstmt.setString(5, StudQueries.getString(1));
									upstmt.setString(6, course_Id);

									upstmt.executeUpdate();
								}
							}

							p.deleteAllTempTablesFromTestUser(testCon);
						} // try block for update resultset ends
					} // try block for update stment ends
					catch (Exception e) {
						logger.log(Level.SEVERE,
								"Exception caught at the end of TestAssignment method: " + e.getMessage(), e);
						// e.printStackTrace();
						// throw e;
					}
				} // try block for studQueries stmt ends
			} // try block for testcon ends
			catch (Exception e) {
				logger.log(Level.SEVERE, "Exception caught at TestAssginment.evaluate method : " + e.getMessage(), e);
				// e.printStackTrace();
				// throw e;
			}
		} // try block for dbcon ends
		catch (Exception e) {
			logger.log(Level.SEVERE, "Exception caught at end of TestAssignment.evaluate method: " + e.getMessage(), e);
			// e.printStackTrace();
			// throw e;
		}
	}

	/**
	 * This method is called when the student submits the query in
	 * non-interactive mode. The student query is tested correct or wrong for
	 * DS0 alone and status is returned.
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public QueryStatusData testQuery(String[] args) throws SQLException {
		// test here
		QueryStatusData queryStatus = new QueryStatusData();
		queryStatus.Status = QueryStatus.Correct;
		boolean isQueryExists = false;
		PopulateTestData populateTestData = new PopulateTestData();
		int assignment_id = Integer.parseInt(args[0]);
		int question_id = Integer.parseInt(args[1]);
		String rollnum = args[2];
		String courseId = args[3];

		try (Connection dbcon = MyConnection.getDatabaseConnection()) {
			try (Connection testCon = new DatabaseConnection().getTesterConnection(assignment_id)) {

				if (dbcon != null) {
					logger.log(Level.INFO, "Connected successfullly");
				}

				QueryStatus status = QueryStatus.Incorrect;
				// SQLParser sqlParser = new SQLParser();
				CCJSqlParserManager pm = new CCJSqlParserManager();
				String StudQueryString = "select querystring from xdata_student_queries where assignment_id=? and question_id=? and rollnum=? and course_id=?";

				TestAnswer test = new TestAnswer();
				try (PreparedStatement studQueryStmt = dbcon.prepareStatement(StudQueryString)) {
					studQueryStmt.setInt(1, assignment_id);
					studQueryStmt.setInt(2, question_id);
					studQueryStmt.setString(3, rollnum);
					studQueryStmt.setString(4, courseId);
					logger.log(Level.INFO, "A" + assignment_id + "Q" + question_id + "S" + 1);

					try (ResultSet studentQuery = studQueryStmt.executeQuery()) {
						if (studentQuery.next()) {
							String qry = studentQuery.getString("querystring");
							String testQuery = qry;
							int flag = 0;
							// SQLParser sqlParser = new SQLParser();
							StatementNode s = null;
							if (qry != null && !qry.isEmpty() && testQuery != null && !testQuery.isEmpty()) {
								isQueryExists = true;
								String OriginalQry = qry.replaceAll("''", "'");
								logger.log(Level.INFO, "queryString" + OriginalQry);
								qry = OriginalQry.trim().replaceAll("\n+", " ");
								qry = qry.trim().replaceAll(" +", " ");
								qry = qry.replace("NATURAL LEFT OUTER", "NATURAL");
								qry = qry.replace("NATURAL RIGHT OUTER", "NATURAL");

								logger.log(Level.INFO, "Cleansed Query is " + qry);
								try {
									// String modQuery =
									// TestAnswer.parseWithAsQueryJSQL(testQuery);//preParseQuery(testQuery);
									// if(modQuery != null ){
									// testQuery = modQuery;
									// }
									logger.log(Level.INFO, "queryString" + testQuery);
									testQuery = testQuery.trim().replaceAll("\n+", " ");
									testQuery = testQuery.trim().replaceAll(" +", " ");

									// Statement stmt = pm.parse(new
									// StringReader(testQuery));

									/*
									 * if(s instanceof InsertNode){ testQuery =
									 * TestAnswer.convertInsertQueryToSelect(
									 * testQuery); }else if(s instanceof
									 * DeleteNode){ testQuery =
									 * TestAnswer.convertDeleteQueryToSelect(
									 * testQuery); }else if(s instanceof
									 * UpdateNode){ testQuery =
									 * TestAnswer.convertUpdateQueryToSelect(
									 * testQuery); }
									 */

									/*
									 * if(testQuery.toLowerCase().contains(
									 * "year1")){ testQuery =
									 * testQuery.replaceAll("year1","year"); }
									 */
									populateTestData.deleteAllTempTablesFromTestUser(testCon);
									populateTestData.createTempTables(testCon, assignment_id, question_id);
									try (PreparedStatement testStatement = testCon.prepareStatement(testQuery)) {
										testStatement.executeQuery();
									}
								} catch (Exception e) {
									logger.log(Level.SEVERE, e.getMessage(), e);
									// e.printStackTrace();
									queryStatus.ErrorMessage = e.getMessage();
									queryStatus.Status = QueryStatus.Error;
									flag = 1;
								}

								if (flag == 0) {
									try {
										String queryId = "A" + assignment_id + "Q" + question_id + "S" + 1;
										status = test.testQueryAnswer(assignment_id, question_id, courseId, OriginalQry,
												rollnum, "4/" + courseId + "/" + queryId);
										// status = QueryStatus.Correct;
									} catch (Exception e) {
										logger.log(Level.SEVERE, "Exception caught here:" + e.getStackTrace(), e);
										// e.printStackTrace();
									}
								}
							}
						}
					} // try block for resultset studentQuery ends
				} // try block for studQueryStmt ends
				queryStatus.Status = status;

				if (!isQueryExists) {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollnum
							+ "' and course_id='" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
					queryStatus.Status = QueryStatus.NoDataset;
				} else if (queryStatus.Status == QueryStatus.Correct) {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = true where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollnum
							+ "' and course_id='" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
				} else {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollnum
							+ "' and course_id='" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
				}
				try {
					populateTestData.deleteAllTempTablesFromTestUser(testCon);
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}

				testCon.close();
				dbcon.close();
			} // try block for testconn ends
		} // try block for conn ends
			// logger.log(Level.INFO,"TestAssignment - DS0 MODE :
			// "+ro.kifs.diagnostic.Connection.getStillOpenedConnsStackTraces());

		return queryStatus;
	}

	/**
	 * This method is called when instructor evaluates the assignment.
	 * Instructor grades the student queries as correct or incorrect based on
	 * answer
	 * 
	 * @param args
	 */
	public static void entry(String[] args) {
		TestAssignment ta = new TestAssignment();
		try {
			int assignment_id = Integer.parseInt(args[0]);
			int question_id = Integer.parseInt(args[1]);
			String course_id = args[2];
			ta.evaluateQuestion(assignment_id, question_id, course_id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.log(Level.SEVERE, e.getMessage(), e);
			// e.printStackTrace();
		}
	}

	/**
	 * This method is called when the query equivalence is tested in TESTER mode
	 * 
	 * @param args
	 */
	public String checkQueryEquivalence(int assignment_id, int question_id, String course_Id) throws Exception {
		String status = "pass";
		TestAnswer test = new TestAnswer();
		boolean equivalenceStatus = true;
		try (Connection testCon = new DatabaseConnection().getTesterConnection(assignment_id)) {

			try (Connection dbcon = MyConnection.getDatabaseConnection()) {
				if (dbcon != null) {
					logger.log(Level.INFO, "Connected successfullly");
				}
				logger.log(Level.INFO, "A" + assignment_id + "Q" + question_id + "S" + 1);
				GenerateCVC1 cvc = new GenerateCVC1();
				cvc.initializeConnectionDetails(assignment_id, question_id, 1, course_Id);
				TableMap tm = cvc.getTableMap();
				cvc.closeConn();

				PopulateTestData p = new PopulateTestData();
				p.deleteAllTempTablesFromTestUser(testCon);
				p.createTempTables(testCon, assignment_id, question_id);
				Gson gson = new Gson();
				ArrayList<String> mutants = new ArrayList<String>();
				int index = 0;

				String json = "";
				String ans = "";
				String add_failed_datasets = "update xdata_qinfo set equivalence_failed_datasets = ?, equivalenceStatus = ? where assignment_id = ? and question_id=? and course_id=?";
				String mutantQuery = "select sql from xdata_instructor_query where assignment_id=? and question_id=? and course_id=?";
				try (PreparedStatement sqlQuery = dbcon.prepareStatement(mutantQuery)) {
					sqlQuery.setInt(1, assignment_id);
					sqlQuery.setInt(2, question_id);
					sqlQuery.setString(3, course_Id);
					ResultSet rSet = sqlQuery.executeQuery();
					while (rSet.next()) {
						if (index != 0) {
							mutants.add(rSet.getString("sql"));
						}
						index++;
					}
				}
				try (PreparedStatement update = dbcon.prepareStatement(add_failed_datasets)) {
					try {
						ArrayList<FailedDataSetValues> failedDSToJson = new ArrayList<FailedDataSetValues>();

						Map<Integer, FailedDataSetValues> failedDsMap = test.checkQueryEquivalence(assignment_id,
								question_id, course_Id);
						// for(FailedDataSetValues failedDs:listOfFailedDS){
						Iterator it = failedDsMap.keySet().iterator();
						while (it.hasNext()) {
							FailedDataSetValues failedDs = failedDsMap.get(it.next());
							logger.log(Level.INFO, "Answer is :" + ans);
							if (failedDs.getStatus().equalsIgnoreCase("Failed")) {
								// newFailedDSValue.setQuery_id(failedDs.getQuery_id());
								status = "failed";
								equivalenceStatus = false;
								// This is set to true to say the question is
								// evaluated
								// upstmt.setBoolean(1, true);
								// Get DS0
								Map<String, Map<String, ArrayList<String>>> failedStudDataMap = new HashMap<String, Map<String, ArrayList<String>>>();
								Map<String, Map<String, ArrayList<String>>> failedInstrDataMap = new HashMap<String, Map<String, ArrayList<String>>>();

								for (int i = 0; i < failedDs.getDataSetIdList().size(); i++) {

									FailedDataSetValues newFailedDSValue = new FailedDataSetValues();
									newFailedDSValue.setStatus(status);
									newFailedDSValue.setQuery_id(failedDs.getQuery_id());
									ArrayList<FailedColumnValues> failedList = new ArrayList<FailedColumnValues>();
									Map<String, ArrayList<String>> failedColMap = new HashMap<String, ArrayList<String>>();

									logger.log(Level.INFO, "Inside operator");
									String dataset = failedDs.getDataSetIdList().get(i);
									newFailedDSValue.setDataSetId(dataset);

									if (dataset.startsWith("DS")) {
										if (dataset.equalsIgnoreCase("DS_Default")) {
											p.createTempTableData(dbcon, testCon, assignment_id, question_id,
													course_Id);
										} else {
											p.fetchAndPopulateTestDatabase(dbcon, testCon, assignment_id, question_id,
													failedDs.getQuery_id(), course_Id, dataset, tm);
										}

									} else {
										p.createTempTableWithDefaultData(dbcon, testCon, assignment_id, question_id,
												course_Id, dataset);
									}
									newFailedDSValue = this.getStudentOutput(testCon, dataset, mutants.get(0),
											failedList, newFailedDSValue, failedStudDataMap, false);
									newFailedDSValue = this.getInstructorOutput(testCon, dataset,
											failedDs.getInstrQuery(), failedList, newFailedDSValue, failedInstrDataMap,
											false);

									newFailedDSValue.getDsValueMap().put(dataset, failedList);
									failedDSToJson.add(newFailedDSValue);
									/*
									 * json = gson.toJson(failedDs);
									 * logger.log(Level.INFO,
									 * "JSON VALUE OF DataSetValue field :" +
									 * json);
									 * 
									 * add.setString(1,StudQueries.getString(
									 * "rollnum")); add.setString(2,oldQueryId);
									 * add.setString(3, dataset);
									 * add.setString(4, json);
									 * add.setInt(5,assignment_id);
									 * add.setInt(6,question_id);
									 * add.setString(7,course_Id); //
									 * add.setString(4, ); add.executeUpdate();
									 */
									// } // While each DS loop ends
								} // for each DSloop ends
							} // if status failed loop ends
							else {
								// upstmt.setBoolean(1, true);
							}
							logger.log(Level.INFO, "JSON VALUE OF DataSetValue field :" + json);

						} // while iterator loop ends
						json = gson.toJson(failedDSToJson);
						update.setString(1, json);
						update.setBoolean(2, equivalenceStatus);
						update.setInt(3, assignment_id);
						update.setInt(4, question_id);
						update.setString(5, course_Id);
						update.execute();
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Exception caught here: " + e.getMessage(), e);
						e.printStackTrace();
					}

				}
				p.deleteAllTempTablesFromTestUser(testCon);
			} // try block for testcon ends
		} // try block for dbcon ends
		return status;
	}

	/*
	 * method for TESTING - parsing in batch. Assumption: queries are stored in
	 * file <srcFileName>, the non-parsing queries are stored at the end of
	 * execution in file <tarFileName>
	 * 
	 * To be deleted
	 */

	public static void readQueriesFromFileParseAndTest() {
		String srcFileName = "/home/shree/Documents/Assignment1/Q2/Records10.txt",
				tarFileName = "/home/shree/Documents/Assignment1/Q2/BadQueries.txt";

		TestAnswer ta = new TestAnswer();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(srcFileName));
			PrintWriter writer = new PrintWriter(tarFileName);
			String line = null;
			String query = "";

			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					if (query.trim() != "") {
						String serialNum = query.substring(0, query.indexOf(")") + 1);
						String actualQuery = query.substring(query.indexOf(")") + 1, query.indexOf(";"));
						Vector<String> columnmismatch = new Vector();
						System.out.println(serialNum);
						try {
							// ta.testQuery(args)
							Connection dbcon = getDBConnectionToTest();
							Connection testCon = getTesterConnectionToTest();

							HashMap<String, String> mutants = new HashMap<String, String>();
							mutants.put("q2", actualQuery);
							Vector<String> result = TestAnswer.checkAgainstOriginalQuery(mutants, "DS_TEST",
									"select student.id, name from student, takes, course where student.id=takes.id and takes.course_id = course.course_id and course.dept_name=\'Comp. Sci.\' and year<2010"
											+ " intersect select student.id, name from student, takes, course where student.id=takes.id and takes.course_id = course.course_id and course.dept_name=\'Comp. Sci.\' and year>2010;"
											+ " ",
									"", true, columnmismatch, testCon);

							if (result != null && !result.isEmpty()) {
								System.out.println("Bad query " + serialNum + " : " + actualQuery);
								writer.println(serialNum + actualQuery);
							} else {
								System.out.println("good query " + serialNum + " : " + actualQuery);
							}
						} catch (Exception e) {
							System.out.println("Bad query " + serialNum + " : " + actualQuery);
							writer.println(serialNum + actualQuery);
						}
					}
					query = "";
				} else {
					query += line;
					// System.out.println(query);
				}

			}
			reader.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		TestAssignment ta = new TestAssignment();
		try {
			readQueriesFromFileParseAndTest();
			/*
			 * for(int i = 0 ; i < 60; i++){ TestEvaluationThread r = new
			 * TestEvaluationThread(); Thread t1 = new Thread(r);
			 * System.out.println(
			 * "i created thread t1 and had it execute run method, which is currently looping for i = "
			 * + i); logger.log(Level.INFO,"Started Thread:" + t1);
			 * 
			 * 
			 * t1.start();
			 * 
			 * 
			 * System.out.println("Finished running thread No = "+i);
			 */
			// String arga[] = {String.valueOf(asID),
			// String.valueOf(questionID.trim()), studentID,courseID};
			// ta.testQuery(args);

			// int assignment_id=Integer.parseInt(args[0]);
			// int question_id=Integer.parseInt(args[1]);
			// String course_id=args[2];
			// ta.evaluateQuestion(assignment_id, question_id,course_id);

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This method is used for TESTING threads handled by XDATA during student
	 * submission. - LOAD TESTING
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public QueryStatusData testThreadsForQuery(String[] args) throws Exception {
		// test here
		QueryStatusData queryStatus = new QueryStatusData();
		queryStatus.Status = QueryStatus.Correct;
		boolean isQueryExists = false;
		PopulateTestData populateTestData = new PopulateTestData();
		int assignment_id = Integer.parseInt(args[0]);
		int question_id = Integer.parseInt(args[1]);
		String rollnum = args[2];
		String courseId = args[3];
		try (Connection dbcon = getDBConnectionToTest()) {
			try (Connection testCon = getTesterConnectionToTest()) {

				if (dbcon != null) {
					logger.log(Level.INFO, "Connected successfullly");
				}

				QueryStatus status = QueryStatus.Incorrect;
				// SQLParser sqlParser = new SQLParser();
				CCJSqlParserManager pm = new CCJSqlParserManager();
				String StudQueryString = "select querystring from xdata_student_queries where assignment_id=? and question_id=? and rollnum=? and course_id=?";

				TestAnswer test = new TestAnswer();
				try (PreparedStatement studQueryStmt = dbcon.prepareStatement(StudQueryString)) {
					studQueryStmt.setInt(1, assignment_id);
					studQueryStmt.setInt(2, question_id);
					studQueryStmt.setString(3, rollnum);
					studQueryStmt.setString(4, courseId);
					logger.log(Level.INFO, "A" + assignment_id + "Q" + question_id + "S" + 1);

					try (ResultSet studentQuery = studQueryStmt.executeQuery()) {
						if (studentQuery.next()) {
							String qry = studentQuery.getString("querystring");
							String testQuery = qry;
							int flag = 0;
							// SQLParser sqlParser = new SQLParser();
							StatementNode s = null;
							if (qry != null && !qry.isEmpty() && testQuery != null && !testQuery.isEmpty()) {
								isQueryExists = true;
								String OriginalQry = qry.replaceAll("''", "'");
								logger.log(Level.INFO, "queryString" + OriginalQry);
								qry = OriginalQry.trim().replaceAll("\n+", " ");
								qry = qry.trim().replaceAll(" +", " ");
								qry = qry.replace("NATURAL LEFT OUTER", "NATURAL");
								qry = qry.replace("NATURAL RIGHT OUTER", "NATURAL");
								/*
								 * if(qry.toLowerCase().contains("year")){
								 * qry=qry.replaceAll("year","year1");
								 * qry=qry.replaceAll("Year","year1");
								 * qry=qry.replaceAll("YEAR","year1"); }
								 */
								logger.log(Level.INFO, "Cleansed Query is " + qry);
								try {
									// testQuery =
									// TestAnswer.preParseQuery(testQuery);
									// String modQuery =
									// TestAnswer.parseWithAsQueryJSQL(testQuery);//preParseQuery(testQuery);
									// if(modQuery != null ){
									// testQuery = modQuery;
									// }
									logger.log(Level.INFO, "queryString" + testQuery);
									testQuery = testQuery.trim().replaceAll("\n+", " ");
									testQuery = testQuery.trim().replaceAll(" +", " ");
									/*
									 * if(testQuery.toLowerCase().contains(
									 * "year")){ testQuery =
									 * testQuery.replaceAll("year","year1");
									 * testQuery =
									 * testQuery.replaceAll("Year","year1");
									 * testQuery =
									 * testQuery.replaceAll("YEAR","year1"); }
									 */

									// s= sqlParser.Statement(testQuery, null);
									/*
									 * Statement stmt = pm.parse(new
									 * StringReader(testQuery));
									 * 
									 * if(s instanceof InsertNode){ testQuery =
									 * TestAnswer.convertInsertQueryToSelect(
									 * testQuery); }else if(s instanceof
									 * DeleteNode){ testQuery =
									 * TestAnswer.convertDeleteQueryToSelect(
									 * testQuery); }else if(s instanceof
									 * UpdateNode){ testQuery =
									 * TestAnswer.convertUpdateQueryToSelect(
									 * testQuery); }
									 */

									/*
									 * if(testQuery.toLowerCase().contains(
									 * "year1")){ testQuery =
									 * testQuery.replaceAll("year1","year"); }
									 */
									populateTestData.deleteAllTempTablesFromTestUser(testCon);
									populateTestData.createTempTablesForTestThread(testCon, assignment_id, question_id);
									try (PreparedStatement testStatement = testCon.prepareStatement(testQuery)) {
										testStatement.executeQuery();
									}
								} catch (Exception e) {
									logger.log(Level.SEVERE, e.getMessage(), e);
									// e.printStackTrace();
									queryStatus.ErrorMessage = e.getMessage();
									queryStatus.Status = QueryStatus.Error;
									flag = 1;
								}

								if (flag == 0) {
									try {
										String queryId = "A" + assignment_id + "Q" + question_id + "S" + 1;
										status = test.testQueryAnswerForTestThreads(assignment_id, question_id,
												courseId, OriginalQry, rollnum, "4/" + courseId + "/" + queryId);
										// status = QueryStatus.Correct;
									} catch (Exception e) {
										logger.log(Level.SEVERE, "Exception caught here:" + e.getStackTrace(), e);
										// e.printStackTrace();
									}
								}
							}
						}

					} // try block for resultset studentQuery ends
				} // try block for studQueryStmt ends
				queryStatus.Status = status;

				if (!isQueryExists) {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollnum
							+ "' and course_id='" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
					queryStatus.Status = QueryStatus.NoDataset;
				} else if (queryStatus.Status == QueryStatus.Correct) {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = true where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollnum
							+ "' and course_id='" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
				} else {
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id = '"
							+ assignment_id + "' and question_id='" + question_id + "' and rollnum = '" + rollnum
							+ "' and course_id='" + courseId + "'";
					try (PreparedStatement pstmt3 = dbcon.prepareStatement(qryUpdate)) {
						pstmt3.executeUpdate();
					}
				}

				populateTestData.deleteAllTempTablesFromTestUser(testCon);

				testCon.close();
			} // try block for testconn ends
			dbcon.close();

		} // try block for conn ends
			// logger.log(Level.INFO,"TestAssignment - DS0 MODE :
			// "+ro.kifs.diagnostic.Connection.getStillOpenedConnsStackTraces());

		return queryStatus;
	}

	/**
	 * This method is for TESTING STUDENT CONNECTIONS / SUBMISSIONS - LOAD
	 * TESTING
	 * 
	 * @return
	 * @throws SQLException
	 */

	public static Connection getDBConnectionToTest() throws SQLException {

		Connection conn = null;
		try {
			System.out.println("Number of connections = " + Thread.currentThread());

			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/xdata", "testing1", "password");
		} catch (ClassNotFoundException ex) {
			System.out.println("Error: unable to load driver class!");
			System.exit(1);
		}
		return conn;

	}

	/**
	 * This method helpd in getting connections for TESTING STUDENT CONNECTIONS
	 * / SUBMISSIONS - LOAD TESTING
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Connection getTesterConnectionToTest() throws SQLException {

		Connection conn = null;
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/xdata", "testing2", "password");
		} catch (ClassNotFoundException ex) {
			System.out.println("Error: unable to load driver class!");
			System.exit(1);
		}
		return conn;

	}
}
