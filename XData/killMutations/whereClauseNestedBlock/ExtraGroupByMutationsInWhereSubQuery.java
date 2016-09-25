package killMutations.whereClauseNestedBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsToKillExtraGroupByMutations;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Conjunct;
import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data set to kill extra group by attributes mutation in each where clause nested sub query block
 * @author mahesh
 *
 */
public class ExtraGroupByMutationsInWhereSubQuery {

	private static Logger logger = Logger.getLogger(ExtraGroupByMutationsInWhereSubQuery.class.getName());
	/**
	 * Generate data set to kill mutations with extra  group by attributes in each where clause nested sub query block
	 * @param cvc
	 */
	public static void generateDataForkillingExtraGroupByMutationsInWhereSubquery(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		try{
			/** we have to check if there are where clause sub queries in each conjunct of outer block of query */
			for(Conjunct con: cvc.getOuterBlock().getConjuncts()){
				
				/**For each where clause sub query blocks of this conjunct*/
				/** Kill mutations with extra  group by attributes in each where clause nested block of this query*/
				for(Node subQCond: con.getAllSubQueryConds()){
					
					/** get the index of this sub query node */
					int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);
					
					/** get the where clause subquery block */
					QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);
					
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"GENERATE DATA FOR KILLING EXTRA GROUP BY MUTATION IN WHERE CLAUSE NESTED SUBQUERY BLOCK: " + qbt);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					
					/** If there are no group by attributes, then no need to kill this mutation */
					if(qbt.getGroupByNodes() == null || qbt.getGroupByNodes().size() == 0)
						continue ;
					
					/** Initialize the data structures for generating the data to kill this mutation */
					cvc.inititalizeForDataset();
					
	
					/**set the type of mutation we are trying to kill*/
					cvc.setTypeOfMutation( TagDatasets.MutationType.EXTRAGROUPBY, TagDatasets.QueryBlock.WHERE_SUBQUERY );
					
					
					/**get extra columns in all the relations*/
					Map<String, String> tableOccurrence = new HashMap<String, String>();
					ArrayList<Column> extraColumn = GenerateConstraintsToKillExtraGroupByMutations.getExtraColumns(cvc, qbt, tableOccurrence);
					
					/**Extra attributes must be distinct in at least two values. So we are making them unique*/
					for(Column col: extraColumn){
						
						Node n = Node.createNode( col, col.getTable() );
						n.setTableNameNo(tableOccurrence.get(col.getTableName()));
						
						qbt.getUniqueElementsAdd().add( new HashSet<Node>( Arrays.asList(n) ));
					}
					
					/** get the tuple assignment for this query
					 * If no possible assignment then not possible to kill this mutation*/
					if( CountEstimationRelated.getCountAndTupleAssignmentToKillExtraGroupByMutations(cvc, qbt) == false)
						continue ;
					
					/** Add constraints for all the From clause nested sub query blocks */
					for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){								
							cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");
							
							cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
							
							cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");								
					}
					
					/** get constraints for this conjunct of outer query block, This also adds constraints for group by and having clause of this where clause sub query */
					cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, cvc.getOuterBlock(), con) );
					
					/**add the negative constraints for all the other conjuncts of outer query block */
					for(Conjunct outer: cvc.getOuterBlock().getConjuncts())
						if( !outer.equals(con))
							cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, cvc.getOuterBlock(), outer) );
						
					/** add group by and having clause constraints for outer query block */
					cvc.getConstraints().add( QueryBlockDetails.getGroupByAndHavingClauseConstraints(cvc, cvc.getOuterBlock())) ;
					
					/**Add other related constraints for the outer query block */
					cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock())) ;
					
					GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc,false);
					/**Get the constraints to kill this mutation*/
					cvc.getConstraints().add("\n%---------------------------------\n%CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES INSIDE WHERE CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n");
					cvc.getConstraints().add(GenerateConstraintsToKillExtraGroupByMutations.getExtraGroupByConstraints(cvc, qbt, extraColumn,tableOccurrence));
					cvc.getConstraints().add("\n%---------------------------------\n%END OF CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES INSIDE WHERE CLAUSE NESTED SUBQUERY BLOCK\n%---------------------------------\n");
					
					/** Call the method for the data generation*/
					GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc,false);
					
				}
			}
			
			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
			//cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
			cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
	}

}
