package killMutations.outerQueryBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.GenerateDataForOriginalQuery;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**TODO: Ask AMol */
public class WhereClauseSubQueryConnectiveMutations {

	private static Logger logger = Logger.getLogger(WhereClauseSubQueryConnectiveMutations.class.getName());
	
	public static void killWhereClauseSubQueryConnectiveMutationsGen(GenerateCVC1 cvc) throws Exception{
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			killWhereClauseSubQueryConnectiveMutations(cvc);
		}
		else{
			//killWhereClauseSubQueryConnectiveMutationsSMT(cvc);
		}
		
	}
	
	public static void killWhereClauseSubQueryConnectiveMutations(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		//HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING WHERE CLAUSE CONNECTIVE MUTATIONS IN OUTER QUERY BLOCK");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();
		try{
			/**Kill the where clause connective mutations in each conjunct of this outer block of  query */
			for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){
				
				/**Get the where clause sub query connective conditions of this conjunct*/
				Vector<Node > subQConnectives = conjunct.getAllSubQueryConds();
				
				/** Kill each sub query connective of this conjunct*/
				for(int i = 0; i < subQConnectives.size(); i++){
					
					/**get the node*/
					Node n= subQConnectives.get(i);
					
					if( n.getType().equalsIgnoreCase(Node.getBroNodeSubQType()) ){/** kill =, >, < mutations*/
						
						generateDataSetToKillBroNodeSubQuery(cvc, qbt, conjunct, n);
					}
					/* the expression: n.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) 
					 * from the If condition below removed, and 
					 * All node, Any node type in the following condition added by mathew on 27 June 2016
					 */
					else if (n.getType().equalsIgnoreCase(Node.getAllNodeType())
					||n.getType().equalsIgnoreCase(Node.getAnyNodeType())){/** kill =, >, <, All Vs Any mutations*/
						
						generateDataSetToKillBroNodeSubQuery(cvc, qbt, conjunct, n);
						
						generateDataSetToKillAllAnyMutations(cvc, qbt, conjunct, n);
					} else if (n.getType().equalsIgnoreCase(Node.getExistsNodeType())) {
						//To kill missing EXISTS mutation
						Node sq=n.clone();
						sq.setType(Node.getNotExistsNodeType());
						subQConnectives.set(i,sq);
						GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvc,TagDatasets.MutationType.MISSING_SUBQUERY.getMutationType()+TagDatasets.QueryBlock.OUTER_BLOCK.getQueryBlock());
						subQConnectives.set(i, n);
						
					} else if (n.getType().equalsIgnoreCase(Node.getNotExistsNodeType())) {
						//To kill missing NOT EXISTS mutation
						Node sq=n.clone();
						sq.setType(Node.getExistsNodeType());
						subQConnectives.set(i,sq);
						GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvc,TagDatasets.MutationType.MISSING_SUBQUERY.getMutationType()+TagDatasets.QueryBlock.OUTER_BLOCK.getQueryBlock());
						subQConnectives.set(i, n);
					} 
					
					/**kill IN Vs NOT IN mutations*/
					else if(n.getType().equalsIgnoreCase(Node.getInNodeType())){
						
						generateDataSetToKillInConnective(cvc, qbt, conjunct, n);
					}
					
					/**Killing exists Vs Not exists mutations*/
					else if(n.getType().equalsIgnoreCase(Node.getExistsNodeType()) || n.getType().equalsIgnoreCase(Node.getNotExistsNodeType())){
						
						generateDataSetToKillInConnective(cvc, qbt, conjunct, n);
						
					}
				}
				
			}
			
			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
	//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
			cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
		}
	}

	
	/**
	 * Generates data sets to kill IN Vs NOT IN or EXISTS Vs NOT EXISTS
	 * @param cvc
	 * @param qbt
	 * @param conjunct
	 * @param n
	 */
	public static void generateDataSetToKillInConnective(GenerateCVC1 cvc, 	QueryBlockDetails qbt, ConjunctQueryStructure conjunct, Node n) throws Exception{
		
		try{
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();
			
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return ;
			
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.WHERECONNECTIVE, TagDatasets.QueryBlock.OUTER_BLOCK );
			
			
			/** Add constraints for all the From clause nested sub query blocks */
			for(QueryBlockDetails qb: qbt.getFromClauseSubQueries()){
				cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" FROM CLAUSE SUBQUERY"));
	
				cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
	
				cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF FROM CLAUSE SUBQUERY"));
			}
			
			
			/** Generate positive constraints for all the conditions of this  conjunct */
			cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );
	
	
			/** Add negative conditions for all other conjuncts of this query block*/
			for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
				if(inner != conjunct)
					cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
			
			
			/** get group by constraints */
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine("GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
			cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
	
	
			/** Generate havingClause constraints */
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine("HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
			for(int l=0; l< qbt.getNoOfGroups(); l++)
				for(int k=0; k < qbt.getAggConstraints().size();k++){
					cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
				}
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
	
			/** add other constraints of outer query block */
			cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, qbt) );
	
			/** Call the method for the data generation*/
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
		}
	}

	
	/**
	 * Generates data sets to kill all and any mutations
	 * @param cvc
	 * @param qbt
	 * @param conjunct
	 * @param n
	 */
	/**FIXME: Tuple assignment for this sub query node has to be changed*/
	public static void generateDataSetToKillAllAnyMutations(GenerateCVC1 cvc, QueryBlockDetails qbt, ConjunctQueryStructure conjunct,	Node subQ) throws Exception{
		try{
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();		
			
			/** get the index of this sub query node */
			int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);
	
			/** get the where clause sub query block */
			QueryBlockDetails subQuery = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);
			
			/**there should be two tuples inside the where clause sub query block to kill this mutation*/
			if(subQuery.getAggConstraints() != null && subQuery.getAggConstraints().size() != 0 ){
	
				/**Update the constrained aggregation variable*/
				subQuery.setConstrainedAggregation(true);
	
				if( subQuery.getFinalCount() < 2){/**Try with 2 tuples*/
					int newCount = CountEstimationRelated.estimateNoOfTuples( cvc, subQuery, 2);
					if(newCount==0){/** Not possible ---Fail*/
						
						logger.log(Level.WARNING,"Cannot generate data for killing Aggregation Mutants");
				
						return ;
					}
					else	
						subQuery.setFinalCount(newCount);
				}
			}
			/**No constrained aggregation and to kill distinct mutation we need two tuples*/
			else
				subQuery.setFinalCount( 2 );
			
			
			/**get the tuple assignment for the outer query block*/
			/**If tuple assignment is not possible then continue to kill next mutation*/
			if(CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, cvc.getOuterBlock())== false)
				return ;
	
			/** flag to indicate whether tuple assignment is possible or not*/
			boolean possible = true;
	
			/** get tuple assignment for each from clause sub query block except this sub query block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries() ){					
				possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qb);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					break;
			}
			if(possible == false)
				return ;
	
			/** get tuple assignment for each where clause sub query block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries()){
				if( !qb.equals(subQuery))
					possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qb);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					break;
			}
			
			if(possible == false)
				return ;
			
			/**assign the number of tuples for the this where clause sub query*/
			if( QueryBlockDetails.getTupleAssignment( cvc, subQuery, null) == false)
				return ;
			
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.WHERECONNECTIVE, TagDatasets.QueryBlock.OUTER_BLOCK );
			
			/** Add constraints for all the From clause nested sub query blocks */
			for(QueryBlockDetails qb: qbt.getFromClauseSubQueries()){
				cvc.getConstraints().add(ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));
	
				cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
	
				cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF FROM CLAUSE SUBQUERY "));
			}
			
			
			/** Add negative conditions for all other conjuncts of this query block*/
			for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
				if(inner != conjunct)
					cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
			
			
			/** get group by constraints */
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
			cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
	
	
			/** Generate havingClause constraints */
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine("HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
			for(int l=0; l< qbt.getNoOfGroups(); l++)
				for(int k=0; k < qbt.getAggConstraints().size();k++){
					cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
				}
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
	
			/** add other constraints of outer query block */
			cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, qbt) );
	
			
			/**Add positive constraints for all the conditions of this conjunct except all sub query conditions*/
			cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptWhereClauseSubQueryBlock(cvc, qbt, conjunct) );
			
			String constraintString = "";
			/**Add positive constraints for all the the where clause connectives except this node*/
			for(Node subq: conjunct.getAllSubQueryConds())
				
				if( !subq.equals(subQ)){/**Bug in equals method*/
					
					constraintString += ConstraintGenerator.addCommentLine("CONSTRAINTS FOR WHERE CLAUSE SUBQUERY CONNECTIVE ");
					constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereSubQueryConnective(cvc, qbt, subq);
	
					constraintString +=  ConstraintGenerator.addCommentLine("CONSTRAINTS FOR CONDITIONS INSIDE WHERE CLAUSE SUBQUERY CONNECTIVE ");
	
					constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getCVCForCondsInSubQ(cvc, qbt, subq);
			
					constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForGroupByAndHavingInSubQ(cvc, qbt, subq);
	
					constraintString += ConstraintGenerator.addCommentLine("END OF CONSTRAINTS FOR CONDITIONS INSIDE WHERE CLAUSE SUBQUERY CONNECTIVE ");
				
				}
					
			cvc.getConstraints().add( constraintString );	
			
			/**Add constraints to kill all any mutations in this sub query node*/
			cvc.getConstraints().add( GenerateConstraintsForWhereClauseSubQueryBlock.subQueryConstraintsToKillAllAny( cvc, qbt, subQ));
			
			
			/** Call the method for the data generation*/
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
			
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
		}
	}

	/**
	 * Generates data set to kill Binary relational mutations present in where clause connective
	 * @param cvc
	 * @param qbt
	 * @param conjunct
	 * @param subQ
	 */
	public static void generateDataSetToKillBroNodeSubQuery(GenerateCVC1 cvc, QueryBlockDetails qbt, ConjunctQueryStructure conjunct,	Node subQ) throws Exception{

		try{
			/**create a copy of sub query connective*/
			Node subQCopy = new Node(subQ);
			
			String ops[] = {"=","<",">"};
			
			for(String operator : ops){
				
				/**if it is same operator*/
				if( operator.equalsIgnoreCase( subQCopy.getOperator() ))
					continue;
				
				/**change the operator for this sub query node*/
				subQ.setOperator( operator);
				
				
				/** Initialize the data structures for generating the data to kill this mutation */
				cvc.inititalizeForDatasetQs();
				
				/** get the tuple assignment for this query
				 * If no possible assignment then not possible to kill this mutation*/
				if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
					continue;
				
				/**set the type of mutation we are trying to kill*/
				cvc.setTypeOfMutation( TagDatasets.MutationType.WHERECONNECTIVE, TagDatasets.QueryBlock.OUTER_BLOCK );
				
				
				/** Add constraints for all the From clause nested sub query blocks */
				for(QueryBlockDetails qb: qbt.getFromClauseSubQueries()){
					cvc.getConstraints().add( ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));
	
					cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
	
					cvc.getConstraints().add( ConstraintGenerator.addCommentLine("END OF FROM CLAUSE SUBQUERY "));
				}
				
				
				/** Generate positive constraints for all the conditions of this  conjunct */
				cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );
	
	
				/** Add negative conditions for all other conjuncts of this query block*/
				for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
					if(inner != conjunct)
						cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
				
				
				/** get group by constraints */
				cvc.getConstraints().add( ConstraintGenerator.addCommentLine("GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
				cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
	
	
				/** Generate havingClause constraints */
				cvc.getConstraints().add( ConstraintGenerator.addCommentLine("HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
				for(int l=0; l< qbt.getNoOfGroups(); l++)
					for(int k=0; k < qbt.getAggConstraints().size();k++){
						cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
					}
				cvc.getConstraints().add( ConstraintGenerator.addCommentLine("END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
	
				/** add other constraints of outer query block */
				cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, qbt) );
	
				/** Call the method for the data generation*/
				GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
				
			}
			
			/**revert back the assignment of sub query node*/
			subQ.setOperator( subQCopy.getOperator() );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
		}
	}

}
