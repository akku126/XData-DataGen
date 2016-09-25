package partialMarking;

import java.io.File;
import java.io.StringReader;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parsing.AggregateFunction;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

import testDataGen.GenerateCVC1;
import testDataGen.PopulateTestData;
import testDataGen.TestAnswer;
import util.Configuration;
import util.DatabaseConnection;
import util.MyConnection;
import util.TableMap;

/**
 * This class finds whether the student query needs to be partially marked for DISTINCT keyword
 * This class gets instructor query and it is mutated and tested against instructor query without distinct. 
 * If both output matches, then distinct does not matter.
 *  
 * No marks will be reduced as it does not make any difference.
 * 
 * Else if the output is different, then DISTINCT keyword matters and partial marking weightage needs to be considered.  * 

 * @author shree
 */
public class EvaluateDistinct {

	public static Logger logger = Logger.getLogger(EvaluateDistinct.class.getName());
	public static boolean evaluate(QueryDetails InstructorQuery,QueryDetails StudentQuery,int assignmentId,int questionId,int  queryId, String course_id) throws Exception{
		boolean isEvaluateDistinct = false;
		boolean incorrect = false;
		//get instructor query
		//get student query
		String instrQuery = InstructorQuery.query;
		String studentQuery = StudentQuery.query;
		TestAnswer ta = new TestAnswer();
		PopulateTestData p = new PopulateTestData();
		
		//Mutate studentquery - send student query to a method, -that does single distinct mutation by using parsetree and regenerating the query
		// then- get projectionlist, remove distinct and return the query
		String studentQueryMutation = mutateForDistinct(instrQuery);
		//String studentQueryMutation = mutateForDistinct(studentQuery);
		String qId = "A"+assignmentId +"Q"+questionId+"S"+queryId;
		String filePath = "4/"+course_id+"/"+qId;
		//get connection to populate temp tables with data for running inst and student queries
		HashMap<String,String> mutants = new HashMap<String,String>();
		mutants.put(qId, studentQuery);
		GenerateCVC1 cvc = new GenerateCVC1();
		cvc.initializeConnectionDetails(assignmentId, questionId, 1,course_id);
		Connection testConn = new DatabaseConnection().getTesterConnection(assignmentId);
		Connection conn = MyConnection.getDatabaseConnection();
		Map <Integer,Vector<String>>  datasetForQueryMap =  
				TestAnswer.downloadDatasets(assignmentId,questionId, queryId,course_id, conn, filePath, true);
		
		/*************************************/
		if(datasetForQueryMap.isEmpty()){
			//conn.close();
			//testConn.close();
			//return QueryStatus.NoDataset;
		 //Load the default sample data file
		boolean flag=true;
		Vector<String> cmismatch = new Vector<String>();
			p.deleteAllTempTablesFromTestUser(testConn);
		 	p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);

			Vector<String> killedMutants = TestAnswer.checkAgainstOriginalQuery(mutants, "DS_Default", instrQuery,"NoPath", true, cmismatch, testConn);
			logger.log(Level.INFO,"******************");
			logger.log(Level.INFO,"Default dataset Loaded : " + " " + killedMutants.size());
			logger.log(Level.INFO,"******************");
			for(int l=0;l<killedMutants.size();l++){
				if(mutants.containsKey(killedMutants.get(l))){
					flag = false;
					incorrect = true;
					break;
				}
			}
			if(!incorrect){
				//isDataSetVerified.add(true);
			}	
			else{
				isEvaluateDistinct = true;
				//resultOfDatasetMatchForEachQuery.put(queryId,false);
				logger.log(Level.INFO,"");		
			}	
			
		
		}else{
			for(Integer id : datasetForQueryMap.keySet()){
				 Vector<String> datasets = datasetForQueryMap.get(id);
				 if(datasets.size() == 0){
					conn.close();
					testConn.close();
					//ERROR NO DATASET AVAILABLE
					 }
				 boolean flag=true;
				 for(int i=0;i<datasets.size();i++){
						
						String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
					 	File ds=new File(dsPath);
						String copyFiles[] = ds.list();
						
						Vector<String> vs = new Vector<String>();
						for(int m=0;m<copyFiles.length;m++){
						    vs.add(copyFiles[m]);		    
						}
					 	// query output handling
						Pattern pattern = Pattern.compile("^A([0-9]+)Q[0-9]+");
						Matcher matcher = pattern.matcher(qId);
						int assignId = 1;
						
						if (matcher.find()) {
						    assignId = Integer.parseInt(matcher.group(1));
						} 
						
						cvc.initializeConnectionDetails(assignId,questionId,queryId,course_id);
						TableMap tm = cvc.getTableMap();
						p.populateTestDataForTesting(vs, filePath+"/"+datasets.get(i), tm, testConn, assignId, questionId);
						
						
						Vector<String> cmismatch = new Vector<String>();
						logger.log(Level.INFO,datasets.get(i));
						
						
						Vector<String> killedMutants = TestAnswer.checkAgainstOriginalQuery(mutants, datasets.get(i), instrQuery, dsPath, true, cmismatch, testConn);
						logger.log(Level.INFO,"******************");
						logger.log(Level.INFO,datasets.get(i) + " " + killedMutants.size());
						logger.log(Level.INFO,"******************");
						for(int l=0;l<killedMutants.size();l++){
							if(mutants.containsKey(killedMutants.get(l))){
								flag = false;
								incorrect = true;
							}
						}
								if(!incorrect){
									
									//isDataSetVerified.add(true);
								}	
								else{
									isEvaluateDistinct = true;
									//resultOfDatasetMatchForEachQuery.put(queryId,false);
									logger.log(Level.INFO,"");
									break;
									}
								cvc.closeConn();									
					   }
				
			
		 }//For each query
	}
		/*************************************/
		//run student query and instructor query against the datasets
		//if same output - then we dont consider distinct
		
		
		// else we consider partial marks for distinct as it changes output.
		return isEvaluateDistinct;
	}
	
	/**
	 * This method removes the distinct key word in the instructor query and returns the mutated query.
	 * Only single distinct mutations are handled.
	 * 
	 * @param studentQuery
	 * @return
	 */
	public static String mutateForDistinct(String instrQuery) throws Exception{
		String newQuery = "";
		PlainSelect plainSelect = null;
		String newInstrQuery = TestAnswer.parseWithAsQueryJSQL(instrQuery);
		boolean isSelectDistinct = false;
		boolean isAggregateWithDistinct = false;
		boolean isHavingWithDistinct = false;
		boolean isSubqWithDistinct = false;
		
		CCJSqlParserManager pm = new CCJSqlParserManager();
		Statement stmt = pm.parse(new StringReader(instrQuery));
		//If Distinct is in select items
		if(stmt instanceof Select){
			newQuery = mutateBasicQuery(plainSelect,stmt, newQuery);
		}
		return newQuery;
	}
	/**
	 * 
	 */
	public static String mutateBasicQuery (PlainSelect plainSelect, Statement stmt, String newQuery ){
		
		
		boolean isSelectDistinct = false;
		boolean isAggregateWithDistinct = false;
		boolean isHavingWithDistinct = false;
		boolean isSubqWithDistinct = false;
		if(((Select) stmt).getSelectBody() instanceof PlainSelect){
		plainSelect = (PlainSelect)((Select) stmt).getSelectBody();
		if(plainSelect.getDistinct() != null){
				plainSelect.setDistinct(null);
				isSelectDistinct = true;
			
			newQuery = stmt.toString();
		}
		//Distinct in Having clause
		else if(plainSelect.getHaving() != null){
			Expression expr = plainSelect.getHaving();
			
			if(expr instanceof BinaryExpression){
				BinaryExpression expr1 = (BinaryExpression)plainSelect.getHaving();
				
				if(expr1.getLeftExpression() != null
						&&expr1.getLeftExpression() instanceof Function
						&& ((Function)expr1.getLeftExpression()).isDistinct()){
					
					isHavingWithDistinct = true;
					((Function)expr1.getLeftExpression()).setDistinct(false);
					
				}
				else if(expr1.getRightExpression() != null
						&&expr1.getRightExpression() instanceof Function
						&& ((Function)expr1.getRightExpression()).isDistinct()){
					isHavingWithDistinct = true;
					((Function)expr1.getRightExpression()).setDistinct(false);
					
				}
			}
			newQuery = stmt.toString();
		}

		//Distinct in aggregate in projected columns
		else if(plainSelect.getSelectItems() != null && plainSelect.getSelectItems().size() > 0){
				//If Distinct is in aggregate of select items
			List<SelectItem> rcList = plainSelect.getSelectItems();
			Vector<AggregateFunction> tempAggFunc=new Vector<AggregateFunction>();
			AggregateFunction aggFunc = new AggregateFunction();
			for(int i=0;i<rcList.size();i++){

				if(rcList.get(i) instanceof SelectExpressionItem){

					SelectExpressionItem rc = (SelectExpressionItem)rcList.get(i);
					if(rc.getExpression() instanceof Function){
						Function an = (Function)rc.getExpression();
						if(an.isDistinct() && !isSelectDistinct){
							an.setDistinct(false);
							isAggregateWithDistinct = true;
							break;
						}
					}
					
				}
			}
		
		newQuery = stmt.toString();
		}
	//If distinct is in from sub-query
	
	//If distinct is in where sub-query
		else if(plainSelect.getWhere() != null){
			newQuery = mutateForSubQ(plainSelect, isSubqWithDistinct, stmt);
		}
		}else{
			newQuery = stmt.toString();
		}
		return newQuery;
	}
	
	/**
	 * This method gets the outerblock and checks if the subqueries have Distinct 
	 * if so the query is mutated to without DISTINCT and returned
	 * 
	 * @param plainSelect
	 * @param isSubqWithDistinct
	 * @param stmt
	 * @return
	 */
	public static String mutateForSubQ(PlainSelect plainSelect,boolean isSubqWithDistinct, Statement stmt){
		String newQuery = "";
		//If subquery is >/>=/</<= or = - check if projected columns contain distinct
		 if(plainSelect.getWhere() != null){
			BinaryExpression binSelectedExp = null;
			if(plainSelect.getWhere() instanceof BinaryExpression){
				//If Expr is <.<= or >, >= or =
				BinaryExpression expr = (BinaryExpression)plainSelect.getWhere();
				Expression binExp1 = null,binExp2 = null;
				if( expr.getLeftExpression() != null && ! (expr.getLeftExpression() instanceof Column)){
					binExp1 = expr.getLeftExpression();
				}else if(expr.getRightExpression() !=null && ! (expr.getRightExpression() instanceof Column)){
					binExp2 = expr.getRightExpression();
				}
				//If binar expression is any of mentioned in comment above
				if(binExp1 != null && (binExp1 instanceof GreaterThan || binExp1 instanceof MinorThan
						|| binExp1 instanceof MinorThanEquals || binExp1 instanceof GreaterThanEquals)){
					
						binSelectedExp = ((BinaryExpression)binExp1);
					
					
					if((binSelectedExp.getLeftExpression() != null 
							&& binSelectedExp.getLeftExpression() instanceof SubSelect)){
						PlainSelect sel = (PlainSelect)((SubSelect)binSelectedExp.getLeftExpression()).getSelectBody();
						if(sel.getDistinct() != null){
							isSubqWithDistinct = true;
							sel.setDistinct(null);
						}
					}		 
					else if((binSelectedExp.getRightExpression() != null 
							&& binSelectedExp.getRightExpression() instanceof SubSelect)){
						PlainSelect sel = (PlainSelect)((SubSelect)binSelectedExp.getRightExpression()).getSelectBody();
						if(sel.getDistinct() != null){
							isSubqWithDistinct = true;
							sel.setDistinct(null);
						}
					}
						
					}
				
				else if(binExp2 != null && (binExp2 instanceof GreaterThan || binExp2 instanceof MinorThan
						|| binExp2 instanceof MinorThanEquals || binExp2 instanceof GreaterThanEquals)){
					
					binSelectedExp = ((BinaryExpression)binExp2);
					
					if((binSelectedExp.getLeftExpression() != null 
							&& binSelectedExp.getLeftExpression() instanceof SubSelect)){
						PlainSelect sel = (PlainSelect)((SubSelect)binSelectedExp.getLeftExpression()).getSelectBody();
						if(sel.getDistinct() != null){
							isSubqWithDistinct = true;
							sel.setDistinct(null);
						}
						
						else if(sel.getHaving() != null || sel.getWhere() != null || sel.getSelectItems() != null){
							newQuery = mutateBasicQuery(plainSelect, stmt,newQuery);
						}
					}		 
					else if((binSelectedExp.getRightExpression() != null 
							&& binSelectedExp.getRightExpression() instanceof SubSelect)){
						PlainSelect sel = (PlainSelect)((SubSelect)binSelectedExp.getRightExpression()).getSelectBody();
						if(sel.getDistinct() != null){
							isSubqWithDistinct = true;
							sel.setDistinct(null);
						}
						else if(sel.getHaving() != null || sel.getWhere() != null || sel.getSelectItems() != null){
							newQuery = mutateBasicQuery(plainSelect, stmt,newQuery);
						}
					}
						
					}
				}
			newQuery = stmt.toString();
			}
			return newQuery;
	}
}
