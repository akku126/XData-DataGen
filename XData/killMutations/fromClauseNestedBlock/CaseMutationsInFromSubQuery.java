/**
 * 
 */
package killMutations.fromClauseNestedBlock;

import generateConstraints.Constraints;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForCaseConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.CaseCondition;
import parsing.CaseExpression;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * @author shree
 *
 */
public class CaseMutationsInFromSubQuery {

	private static Logger logger = Logger.getLogger(CaseMutationsInFromSubQuery.class.getName());
	
	public static void generateDataForKillingCaseMutationsInFromSubquery(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		
		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING CASE CONDITION MUTATIONS IN FROM SUBQUERY");
		logger.log(Level.INFO,"\n----------------------------------\n");
	
		Constraints localConstraints=new Constraints();
		
		
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries()){
			
			/**Get the selection conditions of this conjunct*/
			Map<Integer, CaseExpression> ccMap = qbt.getCaseConditionMap();
			
			//If the Case condition is in projected cols, apply the constraints directly.Following code will do that.
			if(ccMap != null){
				if(ccMap.containsKey(1)){
					generateDataForCaseInProjectedColumns(cvc,qbt);
				} 
				else if(ccMap.containsKey(2)){
				//If case condition is in Where clause, add the constraints to conjunts and call generate dataset for conjunts
					generateDataForWherePredicate(cvc,qbt);
				}else if(ccMap.containsKey(3)){
				//If case condition is in having clause, add then to having clause and call generate data set for having clause constraints
				}else{
					//order-by needs to be handled
				}
			}
			
		}
		
	}


	public static void generateDataForCaseInProjectedColumns(GenerateCVC1 cvc, QueryBlockDetails qbt) throws Exception{
	
		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR CASE CONDITION IN PROJECTED COLUMNS OF OUTER QUERY BLOCK");
		logger.log(Level.INFO,"\n----------------------------------\n");
	
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		Constraints localConstraints=new Constraints();
		ArrayList<Node> projCols = qbt.getProjectedCols();
		
		for(Node n : projCols){
			if(n.getType().equalsIgnoreCase(Node.getCaseNodeType())){
			
			ArrayList<CaseCondition> selectionConds = n.getCaseExpression().getWhenConditionals();
			try{
				/** Kill each selection condition of this conjunct*/
				for(int i=0; i < (selectionConds.size()-1); i++){
					
					CaseCondition sc = selectionConds.get(i);
					/** Generate data set to kill each mutation*/
							logger.log(Level.INFO,"\n----------------------------------");
							logger.log(Level.INFO,"Killing CaseMutations in projected column");
							logger.log(Level.INFO,"\n----------------------------------\n");
							
							/** Initialize the data structures for generating the data to kill this mutation */
							//selectionConds.set(i,scMutants.get(j) );
							
							cvc.inititalizeForDatasetQs();
							 
							/**set the type of mutation we are trying to kill*/
							cvc.setTypeOfMutation( TagDatasets.MutationType.CASECONDITION, TagDatasets.QueryBlock.OUTER_BLOCK );
							
							/** get the tuple assignment for this query
							 * If no possible assignment then not possible to kill this mutation*/
							if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
								continue;
							//Generate Constraints to make sure this condition is true and all before conditions are false
							String constraintString="";
							/*******Code from selection mutations code Start******/
							
							cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc));
							
							cvc.getConstraints().add(GenerateConstraintsForCaseConditions.getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted));
							
							/** Add the encountered case condition to the list*/
							caseConditionCompleted.add(sc);
							cvc.getStringConstraints().add(Constraints.getStringConstraints(localConstraints));
							GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
							
							/*******Code from selection mutations code end******/
				}//for each casecondition ends
				//Else condition Part
				if(n.getCaseExpression().getElseConditional() != null){
					
					cvc.inititalizeForDatasetQs();
					CaseCondition sc = n.getCaseExpression().getElseConditional();
					/**set the type of mutation we are trying to kill*/
					cvc.setTypeOfMutation( TagDatasets.MutationType.CASECONDITION, TagDatasets.QueryBlock.OUTER_BLOCK );
					
					/** get the tuple assignment for this query
					 * If no possible assignment then not possible to kill this mutation*/
					GenerateCVC1.tupleAssignmentForQuery(cvc);
						
					//Generate Constraints to make sure this condition is true and all before conditions are false
					String constraintString="";
					
					/*******Code from selection mutations code Start******/
					cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc));
					cvc.getConstraints().add(GenerateConstraintsForCaseConditions.getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted));
					
					/** Add the encountered case condition to the list*/
					//caseConditionCompleted.add(sc);
					cvc.getStringConstraints().add(Constraints.getStringConstraints(localConstraints));
					GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
				}
			}//try ends
			catch (TimeoutException e){
				logger.log(Level.SEVERE,e.getMessage(),e);		
				throw e;
			}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
			}
		}//If Case condition Node
		}
	}
	
	/**
	 * This method generated constraints for CASE statement in where clause.
	 * 
	 * @param cvc
	 * @throws Exception
	 */
	public static void generateDataForWherePredicate(GenerateCVC1 cvc,QueryBlockDetails qbt) throws Exception{
		
	
		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR CASE CONDITION IN WHERE CLAUSE OF OUTER QUERY BLOCK");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		/** Get outer query block of this query */
		//QueryBlockDetails qbt = cvc.getOuterBlock();
		Constraints localConstraints=new Constraints();
		HashMap<Integer,CaseExpression> ccMap = (HashMap)qbt.getCaseConditionMap();
		
		//1 is Key value for case stmnt in projected cols
		ArrayList<CaseCondition> selectionConds = ccMap.get(2).getWhenConditionals();
		try{
			/** Kill each selection condition of this conjunct*/
			
			cvc.inititalizeForDatasetQs();
			 
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.CASECONDITION, TagDatasets.QueryBlock.FROM_SUBQUERY );
			
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false){}
				//continue;
			//Generate Constraints to make sure this condition is true and all before conditions are false
			String constraintString="";
			/*******Code from selection mutations code Start******/
			
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc));
			
			//cvc.getConstraints().add(GenerateConstraintsForCaseConditions.getConstraintsInCaseStatement(cvc,sc,caseConditionCompleted));
			
			/** Add the encountered case condition to the list*/
			//caseConditionCompleted.add(sc); 
			
			constraintString += "\n%---------------------------------\n%CASE CONDITION CONSTRAINTS\n%---------------------------------\n";
			constraintString += "ASSERT(() ";
			constraintString += GenerateConstraintsForCaseConditions.getCaseConditionConstraintsForOriginalQuery(cvc,qbt);
			constraintString += ")";
			constraintString += "\n%---------------------------------\n%END OF CASE CONDITION CONSTRAINTS\n%---------------------------------\n";
			cvc.getConstraints().add(constraintString);
			
			cvc.getStringConstraints().add(Constraints.getStringConstraints(localConstraints));
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
			
		}//try ends
		catch(Exception e){
		logger.log(Level.SEVERE,e.getMessage(),e);
		throw e;
		}
	}
	
}
