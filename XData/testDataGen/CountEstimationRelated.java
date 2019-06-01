

package testDataGen;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GetSolverHeaderAndFooter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.AggregateFunction;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import util.Configuration;
import util.Utilities;

/**
 * This class estimates the count needed to satisfy aggregation constraint in the given query block
 * Assigns these count to number of tuples for each base relation in this query block
 * This class also checks if the tuple assignment got is satisfiable
 * @author mahesh
 *
 */
public class CountEstimationRelated {
	private static Logger logger = Logger.getLogger(CountEstimationRelated.class.getName());
	/**
	 * Checks the assignment in noTuples and finds out if all the aggregations are satisfiable or not.
	 * @return
	 * @throws Exception
	 */
	/** TODO: WRITE DOC*/
	public static boolean isAssignmentSatisfiable(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{


		String CVCText = "";
		ConstraintGenerator consGen = new ConstraintGenerator();
	
		Iterator itr = queryBlock.getColAggMap().keySet().iterator();
		while(itr.hasNext()){
			Column c = (Column)itr.next();
			ArrayList<Node> aggCons = queryBlock.getColAggMap().get(c);
			//CVCText += cvc.getCVC3_HEADER();
			cvc.getDatatypeColumns().clear();
			CVCText += GetSolverHeaderAndFooter.generateSolver_Header(cvc);
			CVCText += consGen.generateCVCForNullCheckInHaving();
			for(int i=0;i<aggCons.size();i++)			
				CVCText += GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, queryBlock, aggCons.get(i), queryBlock.getFinalCount(), 0) ;


			//CVCText += GetSolverHeaderAndFooter.generateSolver_Footer(cvc);
			CVCText += GetSolverHeaderAndFooter.generateSolver_FooterForAgg(cvc);
			//Escape the whitespaces in the file path when writing File Name in BASH SCRIPT OR Enclose it with single quotes in BASH SCRIPT
			//This is OS specific.
			String filePath =  cvc.getFilePath().replace(" ", "\\ ");
			
			/**write these constraints into a file and execute*/
			Utilities.writeFile(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() + "/checkAggConstraints.smt", CVCText);
			Runtime r = Runtime.getRuntime();
			ConstraintGenerator.getAggConstraintExeFile(filePath,cvc);
			
			ProcessBuilder pb = new ProcessBuilder("/bin/bash", "checkAggConstraints");
			pb.directory(new File(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath()));
			Process myProcess = pb.start();
			
			
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(myProcess.getOutputStream()));
			InputStreamReader myIStreamReader = new InputStreamReader(myProcess.getInputStream());
			ExecutorService service = Executors.newSingleThreadExecutor();
			try{	
				Callable<Integer> call = new CallableProcess(myProcess);
				Future<Integer> future = service.submit(call);
				int exitValue = future.get(180, TimeUnit.SECONDS);
				
				if(myProcess.exitValue() != 0){
					logger.log(Level.SEVERE,"CountEstimationRelated.java: isAssignmentSatisfiable function :  Processing Aggregates failed.");
				}
				
			} catch (ExecutionException e) {
		        //throw new Exception("Process failed to execute", e);
		    } catch (TimeoutException e) {
		    	myProcess.destroy();		    	
		        //throw new Exception("Process timed out", e);
		    } finally {
		    	Utilities.closeProcessStreams(myProcess);
		    	service.shutdown();
		    }	
			
			
			writer.close();
			myIStreamReader.close();
			
			File f = new File(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() + "/isValid");
			if(f.length() == 0){//Means CVC failed
				return false;
			}
		}
		return true;
	}

	/**
	 * This method is used to get the number of tuples needed to satisfy constrained aggregation, if any
	 * @param cvc
	 * @param havingClauseConstraints
	 * @param startVal
	 * @return
	 * @throws Exception
	 */
	/**TODO: We are handling having clause with only ANDs */
	public static int estimateNoOfTuples(GenerateCVC1 cvc, QueryBlockDetails queryBlock, int startVal) throws Exception{

		/**Get list of having clause constraints*/
		ArrayList<Node> havingClauseConstraints = queryBlock.getAggConstraints();
		/** To hold the flattened constraints in the having clause */
		ArrayList<Node> aggConstraints = new ArrayList<Node>();

		/**Clear the data structure used to store details about the count estimation*/
		queryBlock.getFinalCountMap().clear();

		/** USed to store constraint string */
		String CVCStr = "";

		/**Maximum number of tuples to be tried for getting the count*/
		/**TODO: Relax this*/
		int limit = 32;

		/** Stores the no of tuples estimated to satisfy the constraints in the having clause.*/
		int finalCount= 0;		

		/**If there are aggregation constraints */
		if(havingClauseConstraints.size() != 0){ 

			/**Intialize */
			CVCStr = "";			
			aggConstraints.removeAll(aggConstraints);
			queryBlock.setColAggMap( new HashMap<Column, ArrayList<Node>>());

			/**Add this having clause constraints to this list*/
			aggConstraints.addAll(havingClauseConstraints);			

			/**Get the list of aggregation functions on each column */
			/**for each aggregation constraint */
			for(Node n: aggConstraints){

				/**Get the aggregation function names in this node */
				Vector<Node> aggs = n.getAggsFromAggConstraint();

				/**for each aggregation function */
				for(Node agg: aggs){

					/** get the column references from in aggregate function*/
					/**FIXME: Proble when COUNT(*) is present*/
					Vector<Column> cols = agg.getAgg().getAggExp().getColumnsFromNode();

					/**For each column, update data structure */
					for(Column col: cols){

						/**If this column is already present then append this aggregate function to that*/
						if(queryBlock.getColAggMap().containsKey(col)){

							ArrayList<Node> existing = queryBlock.getColAggMap().get(col);
							existing.add(n);
							queryBlock.getColAggMap().put(col, existing);

						}
						else{/** Add a new entry*/
							ArrayList<Node> newVector = new ArrayList<Node>();
							newVector.add(n);
							queryBlock.getColAggMap().put(col, newVector);
						}
					}
				}
			}

			/** Generate CVC file to determine count value */
			Iterator<Column> itr = queryBlock.getColAggMap().keySet().iterator();
			int c = 0;

			/**for each column in the list*/
			while(itr.hasNext()){
				if(startVal != 0)
					c = startVal;/**re initialize at the beginning of loop*/
				else
					c = 1;

				boolean countPresent = false;/**To indicate if there is count aggregation function*/
				boolean strictCount = false;/**To indicate if COUNT() aggregation involves 'equal' operation*/

				/** Stores constraint*/
				String cmdString = "";

				/**Get the column name*/
				Column col = itr.next();

				logger.log(Level.INFO,"\nEstimating count for:" + queryBlock.getColAggMap().get(col));

				/**Repeat untill we get a satisfiable count value*/

				/**get the list of aggregation constraints on this column*/
				ArrayList<Node> havingColConds = queryBlock.getColAggMap().get(col);

				/** Decide the starting value to try for count. This is solely based on the COUNT agg func.*/
				for(int i=0;i<havingColConds.size();i++){

					/**Get this aggregation condition*/
					Node cond = havingColConds.get(i);

					if(cond.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){
						if(cond.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())
								&& cond.getLeft().getAgg().getAggExp().getColumnsFromNode().contains(col)){/**If left side of node is COUNT() aggregation function*/
							if(cond.getOperator().equalsIgnoreCase("=")){/**We have to store this count*/

								/** Straightaway store the count*/
								c = Integer.parseInt(cond.getRight().getStrConst());								

								String tableName = col.getTableName();
								if(queryBlock.getFinalCountMap().containsKey(tableName)){/**If count for this table is already present*/
									if(c > queryBlock.getFinalCountMap().get(tableName)){/**check if new count is greater*/
										queryBlock.getFinalCountMap().put(tableName, c);
									}
								}
								else{/**just update this count*/
									queryBlock.getFinalCountMap().put(tableName, c);
								}

								/**Update the final count*/
								if(c > finalCount){
									finalCount = c;
								}
								strictCount = true;
								break;
							}
							/**Otherwise we cannot store the count*/
							//FIXME What if right side is also an aggregate?
							else if(cond.getOperator().equalsIgnoreCase(">") || cond.getOperator().equalsIgnoreCase(">=")){
								c = Integer.parseInt(cond.getRight().getStrConst());
								countPresent=true;
							}
							else{
								limit = Integer.parseInt(cond.getRight().getStrConst());
								countPresent=true;
							}
						}
					}
					else if(cond.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
						if(cond.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())
								&& cond.getRight().getAgg().getAggExp().getColumnsFromNode().contains(col)){/**If right side of node is COUNT() aggregation function*/
							if(cond.getOperator().equalsIgnoreCase("=")){/**We have to store this count*/

								/** Straight away store the count*/
								c = Integer.parseInt(cond.getRight().getStrConst());

								String tableName = col.getTableName();
								if(queryBlock.getFinalCountMap().containsKey(tableName)){/**If count for this table is already present*/
									if(c > queryBlock.getFinalCountMap().get(tableName)){/**check if new count is greater*/
										queryBlock.getFinalCountMap().put(tableName, c);
									}
								}
								else{/**just update this count*/
									queryBlock.getFinalCountMap().put(tableName, c);
								}

								/**Update the final count*/
								if(c > finalCount){
									finalCount = c;
								}

								strictCount = true;
								break;
							}
							/**Otherwise we cannot store the count*/
							else if(cond.getOperator().equalsIgnoreCase("<") || cond.getOperator().equalsIgnoreCase("<=")){
								c = Integer.parseInt(cond.getLeft().getStrConst());
								countPresent=true;
							}
							else{
								limit = Integer.parseInt(cond.getLeft().getStrConst());;
								countPresent = true;
							}
						}
					}
				}

				if(strictCount){/** Means aggregation is of the form COUNT() = integer, so continue for next aggregation constraint*/
					continue;
				}

				int countVal=0;
				for(;;c++){
					if(c > limit){
						logger.log(Level.INFO,"\nGiving up. Tried "+limit+" times. Returning: "+finalCount);
						return finalCount;
					}
					//Escape the whitespaces in the file path when writing File Name in BASH SCRIPT OR Enclose it with single quotes in BASH SCRIPT
					//This is OS specific.
					String filePath =  cvc.getFilePath().replace(" ", "\\ ");
					/**Get the constraints related CVC3*/
					//CVCStr = GenerateCVCConstraintForNode.generateCVCForCNTForPositiveINT( queryBlock, havingColConds, col, c);
					CVCStr = ConstraintGenerator.generateCVCForCNTForPositiveINT( queryBlock, havingColConds, col, c);

					/**write these constraints into a file and execute*/
					Utilities.writeFile(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() + "/getCount.smt", CVCStr);
					
					ConstraintGenerator.getCountExeFile(filePath, cmdString, cvc);
					
					
					ProcessBuilder pb = new ProcessBuilder("/bin/bash", "execCOUNT");
					pb.directory(new File(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath()));
					Process myProcess = pb.start();
					int exitVal = myProcess.waitFor();
					
					if(exitVal != 0 || myProcess.exitValue() != 0){
						logger.log(Level.SEVERE,"CountEstimationRelated.java: estimateNoOfTuples Method :  Processing Aggregates failed.");
					}
					
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(myProcess.getOutputStream()));
					InputStreamReader myIStreamReader = new InputStreamReader(myProcess.getInputStream());

					Utilities.closeProcessStreams(myProcess);					
					
					File f = new File(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() + "/isNotValid");
					if(f.length() != 0){
						continue;
					}

					/**No need to read from the cvc output file. Just use the one passed to CVC file.*/
					BufferedReader input = null;
					String countFromFile = "";
					try{
						input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc" + cvc.getFilePath() + "/COUNT"));
						String str = input.readLine();
						if(str != null && !str.isEmpty()){
							countFromFile = str;
						}else{
							countFromFile = "0";
						}
					}
					finally{
						writer.close();
						myIStreamReader.close();
						input.close();
					}
					countVal = Integer.parseInt(countFromFile);
					countVal = c;
					if(countVal > finalCount){
						finalCount = countVal;
					}
					break;
				}
				logger.log(Level.INFO,"# of CVC calls: "+c);

				/**Update the count for this table*/
				String tableName = col.getTableName();
				if(queryBlock.getFinalCountMap().containsKey(tableName)){
					if(countVal > queryBlock.getFinalCountMap().get(tableName)){
						queryBlock.getFinalCountMap().put(tableName, countVal);
					}
				}
				else{
					queryBlock.getFinalCountMap().put(tableName, countVal);
				}
				logger.log(Level.INFO,"\nCount for " + queryBlock.getColAggMap().get(col) + " : "+countVal);

			}
			/** Now we have the count value in finalCount */

			logger.log(Level.INFO,"Estimated finalCount: " + finalCount);
			return finalCount;
		}	
		else 
			return 1;/** No aggregation*/
	}

	/**
	 * Estimates the number of tuples needed to satisfy the constrained aggregation and
	 * assigns this to each base relation of this query block
	 * @return
	 * @throws Exception
	 */

	public static boolean estimateCountAndgetTupleAssignmentForQueryBlock(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{

		/** Initialize aggregation constraints data structure*/
		queryBlock.setAggConstraints( new ArrayList<Node>());

		Vector<Node> aggConstraints = new Vector<Node>();
		
		if(queryBlock.getHavingClause() != null)
			Utilities.flattenConstraints( aggConstraints, queryBlock.getHavingClause());


		RelatedToParameters.setupDataStructuresForParamConstraints(cvc,queryBlock );

		/**FIXME: Not sure whether this is needed or not*/
		for(ConjunctQueryStructure conjunct: queryBlock.getConjunctsQs())
			for(Node n: conjunct.getAllSubQueryConds())
				if(n.getType().equals(Node.getBroNodeSubQType()) )
					queryBlock.setConstrainedAggregation(true);

		/**Update query block aggregation list*/
		queryBlock.setAggConstraints( new ArrayList<Node>(aggConstraints));

		/** If there is having clause in this query block*/
		if(queryBlock.getAggConstraints() != null && queryBlock.getAggConstraints().size()!=0){

			queryBlock.setConstrainedAggregation(true);

			/** A flag to indicate whether tuple assignment is possible or not*/
			boolean success = false;


			logger.log(Level.INFO,"\n*****Estimation of No Of Tuples******");

			/** Get the number of tuples needed to satisfy this constrained aggregation*/
			queryBlock.setFinalCount( estimateNoOfTuples( cvc, queryBlock, 0) );

			/** Check if any count>0 is possible or not*/
			if(queryBlock.getFinalCount() == 0){
				logger.log(Level.INFO,"\nConstraint cannot be satisfied.");
				return false;
			}
			/** There is a possible count for this query block*/
			else{
				/** Get the tuple assignment with this count for this query block*/
				if( QueryBlockDetails.getTupleAssignment( cvc, queryBlock, null) == false)
					return false;

				logger.log(Level.INFO,"\n*****Checking Satisfiability of No Of Tuples******\n");

				/**Check if the given assignment is correct or not*/
				if(!isAssignmentSatisfiable(cvc, queryBlock)){
					logger.log(Level.WARNING,"\nFailed!\n");
				}
				else{
					success = true;
					logger.log(Level.INFO,"\nAssignment satisfiable!\n");
				}
			}

			/** If finalcount>0 and tuple assignment is not possible*/
			if(success == false){

				logger.log(Level.WARNING,"\nHeuristics failed. Query constants will be ignored. New constants will be used.");

				/**Add the aggregation constraints to constraintsWithparameters*/
				for(int i=0;i<queryBlock.getAggConstraints().size();i++){

					Node n = queryBlock.getAggConstraints().get(i);	

					ArrayList<String> paramsInNode = RelatedToParameters.replaceParamWithID( queryBlock, n, "", "AGG");

					queryBlock.getConstraintsWithParameters().put("AGG" + queryBlock.getpConstraintId(), n);

					queryBlock.getParamsNodeMap().put(paramsInNode, n);

					queryBlock.setpConstraintId( queryBlock.getpConstraintId() + 1);

					queryBlock.getAggConstraints().remove(i);

					i = -1;
				}

				/** helpful in killing aggregation mutants*/
				queryBlock.setFinalCount(3);

				return QueryBlockDetails.getTupleAssignment( cvc, queryBlock, null);
			}
			/** The tuple assignment method is successful*/
			return true;
		}
		/**If there is no constrained aggregation*/
		else{
			queryBlock.setFinalCount(1);
			return true;
		}
	}

	/**
	 * This function is used to get the count needed while killing aggregation mutations inside the given query block
	 * @param cvc
	 * @param qbt
	 * @param attempt TODO
	 * @return
	 * @throws Exception
	 */
	public static boolean  getCountNeededToKillAggregationMutation(GenerateCVC1 cvc, QueryBlockDetails qbt, int attempt) throws Exception{

		/**If there is constrained aggregation then get number of tuples needed to satisfy aggregation constraint*/
		if(qbt.getAggConstraints() != null && qbt.getAggConstraints().size() != 0 ){

			/**Update the constrained aggregation variable*/
			qbt.setConstrainedAggregation(true);

			if(attempt==0 && qbt.getFinalCount() < 3){/**Try with 3 tuple*/
				int newCount = CountEstimationRelated.estimateNoOfTuples( cvc, qbt, 3);
				if(newCount==0){/** Try with 2 tuple*/
					newCount = CountEstimationRelated.estimateNoOfTuples( cvc, qbt, 2);
					if(newCount==0){/** Not possible ---Fail*/
						logger.log(Level.WARNING,"Cannot generate data for killing Aggregation Mutants");
						return false;
					}
					else	qbt.setFinalCount(newCount);
				}
				else	qbt.setFinalCount(newCount);
			}
			else if(attempt==1){
				qbt.setFinalCount( qbt.getFinalCount() + 1 );
			}
			else if (attempt==2){
				qbt.setFinalCount( qbt.getFinalCount() - 2 );

			}
		}
		/**No constrained aggregation and to kill aggregation mutation we need three tuples*/
		else
			qbt.setFinalCount( 3 );

		return true;
	}

	/**
	 * This function is used to get the count needed while killing distinct mutations inside the given query block
	 * @param cvc
	 * @param qbt
	 * @return
	 * @throws Exception
	 */
	public static boolean  getCountNeededToKillDistinctMutation(GenerateCVC1 cvc, QueryBlockDetails qbt) throws Exception{
		
		/** We need 2 tuples to kill this mutation */
		
		if(qbt.getAggConstraints() != null && qbt.getAggConstraints().size() != 0 ){
			
			/**Update the constrained aggregation variable*/
			qbt.setConstrainedAggregation(true);
			
			if( qbt.getFinalCount() < 2){/**Try with 2 tuples*/
				int newCount = CountEstimationRelated.estimateNoOfTuples( cvc, qbt, 2);
				if(newCount==0){/** Not possible ---Fail*/
					logger.log(Level.WARNING,"Cannot generate data for killing Aggregation Mutants");
					return false;
				}
				else	
					qbt.setFinalCount(newCount);
			}
		}
		/**No constrained aggregation and to kill distinct mutation we need two tuples*/
		else
			qbt.setFinalCount( 2 );
		
		return true;
	}
	/**
	 * Gets the tuple assignment for each query block except for the given query block
	 * Returns whether tuple assignment is possible or not
	 * @param cvc
	 * @param qbt
	 * @param subQuery----1 means from sub query, 2 means where sub query
	 * @return
	 * @throws Exception
	 */
	public static boolean  getTupleAssignmentExceptQueryBlock(GenerateCVC1 cvc, QueryBlockDetails qbt) throws Exception{

		/**get the tuple assignment for the outer query block, if the given query block is not outer block*/
		if( !cvc.getOuterBlock().equals(qbt) && CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, cvc.getOuterBlock()) == false)
			return false;

		/** flag to indicate whether tuple assignment is possible or not*/
		boolean possible = true;

		/** get tuple assignment for each from clause sub query block */
		for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries() ){

			/**if given sub query block is from clause nested block*/
			if( !qb.equals(qbt))
				possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qb);

			/** If tuple assignment is not possible*/
			if(possible == false)
				break;
		}
		if(possible == false)
			return false;


		/** get tuple assignment for each where clause sub query block*/
		for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries()){

			/**if given sub query block is where clause nested block*/
			if( !qb.equals(qbt))
				possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qb);

			/** If tuple assignment is not possible*/
			if(possible == false)
				break;
		}

		if(possible == false)
			return false;
		
		return true;
	}

	/**
	 * Gets the tuple assignment needed for killing extra group by attributes
	 * @param cvc
	 * @param qbt
	 * @return
	 * @throws Exception
	 */
	public static boolean getCountAndTupleAssignmentToKillExtraGroupByMutations(GenerateCVC1 cvc, QueryBlockDetails qbt) throws Exception {
	
	
		/**If no aggregations then make the count as 2 for this block of query, useful to kill partial group by mutation*/			
		if( qbt.getAggConstraints() == null || qbt.getAggConstraints().size() == 0 ){
	
			qbt.setFinalCount(2);			
	
			/**get tuple assignment*/
			/**assign the number of tuples for the this query block*/
			if( QueryBlockDetails.getTupleAssignment( cvc, qbt, null) == false)
				return false;
	
			/**get tuple assignment for outer query block*/
			if(!qbt.equals(cvc.getOuterBlock()) && estimateCountAndgetTupleAssignmentForQueryBlock(cvc, cvc.getOuterBlock()) == false )
				return false;
	
			/** flag to indicate whether tuple assignment is possible or not*/
			boolean possible = true;
	
			/** get tuple assignment for each from clause sub query block except this subquery block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries() ){
	
				if(!qbt.equals(qb))
					possible = estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qb);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					break;
			}
			if(possible == false)
				return false;
	
			/** get tuple assignment for each where clause sub query block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries()){
	
				if(!qbt.equals(qb))
					possible = estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qb);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					break;
			}
	
			if(possible == false)
				return false;
	
	
		}
		/** get the tuple assignment for this query, where constrained aggregation is present in this query block
		 * If no possible assignment then not possible to kill this mutation*/
		else if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
			return false;
	
		return true;
	}

}

