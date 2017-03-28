package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateConstraintsToKillExtraGroupByMutations;
import parsing.AggregateFunction;
import parsing.Column;
import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class generates data set to kill extra group by attributes mutation
 * @author mahesh
 *
 */
public class ExtraGroupByMutationsInOuterQueryBlock {

	private static Logger logger = Logger.getLogger(ExtraGroupByMutationsInOuterQueryBlock.class.getName());
	
	public static void generateDataForkillingExtraGroupByMutationsInOuterQueryBlockGen(GenerateCVC1 cvc) throws Exception {
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingExtraGroupByMutationsInOuterQueryBlock(cvc);
		}
		else{
			//generateDataForkillingExtraGroupByMutationsInOuterQueryBlockSMT(cvc);
		}
	}
	
	public static void  generateDataForkillingExtraGroupByMutationsInOuterQueryBlock2Gen(GenerateCVC1 cvc) throws Exception {
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingExtraGroupByMutationsInOuterQueryBlock2(cvc);
		}
		else{
			//generateDataForkillingExtraGroupByMutationsInOuterQueryBlock2SMT(cvc);
		}
	}
	/**
	 * Generate data set to kill mutations with extra  group by attributes in outer block of the query
	 * 
	 * Approach:
	 * Get all the attributes of the tables related to group by nodes
	 * Get all the constraints for the query
	 * For the extra attributes which are not present in the group by list, make them differ in value in at least two tuples
	 * Assumption: There are at least two tuples to satisfy constrained aggregation in the outer block of query
	 * 
	 * @param cvc
	 */
	public static void generateDataForkillingExtraGroupByMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception {

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		/**Get outer query block */
		QueryBlockDetails outer = cvc.getOuterBlock();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING EXTRA GROUP BY MUTATION IN OUTER BLOCK OF QUERY: " + outer);
		logger.log(Level.INFO,"\n----------------------------------\n");

		try{
			/** If there are no group by attributes, then no need to kill this mutation */
			if(outer.getGroupByNodes() == null || outer.getGroupByNodes().size() == 0)
				return ;
	
	
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();
	
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.EXTRAGROUPBY, TagDatasets.QueryBlock.OUTER_BLOCK );
	
			
			/**get extra columns in all the relations*/
			Map<String, String> tableOccurrence = new HashMap<String, String>();
			ArrayList<Column> extraColumn = GenerateConstraintsToKillExtraGroupByMutations.getExtraColumns(cvc,outer, tableOccurrence);
			 
			/**Extra attributes must be distinct in at least two values. So we are making them unique*/
			for(Column col: extraColumn){
				
				Node n = Node.createNode( col, col.getTable() );
				n.setTableNameNo(tableOccurrence.get(col.getTableName()));
				
				outer.getUniqueElementsAdd().add( new HashSet<Node>( Arrays.asList(n) ));
			}			
				
			if( CountEstimationRelated.getCountAndTupleAssignmentToKillExtraGroupByMutations(cvc, outer) == false)
				return;
	
			/**Get the constraints for all the blocks of the query */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );
	
			GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc,false);
			
			/**Get the constraints to kill this mutation*/
			cvc.getConstraints().add("\n%---------------------------------\n%CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES\n%---------------------------------\n");
			cvc.getConstraints().add(GenerateConstraintsToKillExtraGroupByMutations.getExtraGroupByConstraints(cvc, outer, extraColumn,tableOccurrence));
			cvc.getConstraints().add("\n%---------------------------------\n%END OF CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES\n%---------------------------------\n");
	
			/** Call the method for the data generation*/
			//GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc,false);
	
	
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
	 * Generate data set to kill mutations with extra  group by attributes in outer block of the query
	 * 
	 * Approach:
	 * Get all the attributes of the tables related to group by nodes - G
	 * Get all the constraints for the query
	 * For the extra attributes which are not present in the group by list, generate two datasets for each extra attribute g.
	 * First dataset: Two groups which agree on G and not on g. Union of the two groups satisfies constraint aggregation
	 * Second dataset: Two groups which agree on G and not on g. Each of the two groups satisfies constraint aggregation
	 * Refer: VLDB Journal paper
	 * @param cvc
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static void generateDataForkillingExtraGroupByMutationsInOuterQueryBlock2(GenerateCVC1 cvc) throws Exception {
		try{		
			/** keep a copy of this tuple assignment values */
			HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
			HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
			HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
	
			/**Get outer query block */
			QueryBlockDetails outer = cvc.getOuterBlock();
	
			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"GENERATE DATA FOR KILLING EXTRA GROUP BY MUTATION IN OUTER BLOCK OF QUERY: " + outer);
			logger.log(Level.INFO,"\n----------------------------------\n");
			
			/** If there are no group by attributes, then no need to kill this mutation */
			if(outer.getGroupByNodes() == null || outer.getGroupByNodes().size() == 0)
				return ;
	
			/**get extra columns in all the relations*/
			Map<String, String> tableOccurrence = new HashMap<String, String>();
			ArrayList<Column> extraColumn = GenerateConstraintsToKillExtraGroupByMutations.getExtraColumns(cvc,outer, tableOccurrence);
			
			HashSet<HashSet<Node>> temp = (HashSet<HashSet<Node>>) ((HashSet<HashSet<Node>>) outer.getUniqueElementsAdd()).clone();
			
			for(Column col: extraColumn){
				
				Boolean firstRun = true;
				String havingUnionConstraint = "";
				String havingUnionNegConstraint = "";
				String havingEachGroupConstraint = "";
				String havingEachGroupNegConstraint = "";
				
				for(int i = 0; i < 2; i++) {
				
					/** Initialize the data structures for generating the data to kill this mutation */
					cvc.inititalizeForDatasetQs();
					
					ArrayList<Column> extra = new ArrayList<Column>();
					extra.add(col);
		
					/**set the type of mutation we are trying to kill*/
					cvc.setTypeOfMutation( TagDatasets.MutationType.EXTRAGROUPBY, TagDatasets.QueryBlock.OUTER_BLOCK );
					
					Node n = Node.createNode( col, col.getTable() );
					n.setTableNameNo(tableOccurrence.get(col.getTableName()));
					
					outer.setUniqueElementsAdd(temp);
					
					outer.getUniqueElementsAdd().add( new HashSet<Node>( Arrays.asList(n) ));
					
					if( CountEstimationRelated.getCountAndTupleAssignmentToKillExtraGroupByMutations(cvc, outer) == false)
						continue;
					
					cvc.updateTotalNoOfOutputTuples(cvc.getOuterBlock(), 2);
					
					/**Get the constraints for all the blocks of the query */
					cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, cvc.getOuterBlock(), n));
					
					QueryBlockDetails qb = cvc.getOuterBlock();
					
					if(firstRun){		
						firstRun = false;
						// Constraint for each group
						for(int j=0; j< qb.getNoOfGroups();j ++)
							for(int k=0; k < qb.getAggConstraints().size();k++){
								String constraint = GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qb, qb.getAggConstraints().get(k), qb.getFinalCount(), j);
								havingEachGroupConstraint += constraint;
								havingEachGroupNegConstraint += negateConstraint(constraint);
							}
						
						// Constraint for both groups taken together
						for(int k=0; k < qb.getAggConstraints().size();k++){
							AggregateFunction agg = getAggNode(qb.getAggConstraints().get(k));
							if(agg == null)
								continue;
							String innerTableNo = agg.getAggExp().getTableNameNo();
							//If agg exp does not have tableNameNo, it can have a BAO Node (expression)
							//Which contains TableNameNo in internal node
							if(innerTableNo == null){
								innerTableNo = getTableNameNoForAggWithBAONode(agg.getAggExp());
							}
							
							int myCount = cvc.getNoOfTuples().get(innerTableNo);
							HashMap<String, Integer> noOfTuples = cvc.getNoOfTuples();
							noOfTuples.put(innerTableNo, myCount * qb.getNoOfGroups());
							cvc.setNoOfTuples(noOfTuples);
							String constraint = GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qb, qb.getAggConstraints().get(k), qb.getFinalCount() * qb.getNoOfGroups(), 0);
							havingUnionConstraint += constraint;
							havingUnionNegConstraint += negateConstraint(constraint); 
						}
					}
					
					if(i == 0){
						/** Generate havingClause constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n");
						cvc.getConstraints().add(havingEachGroupConstraint);
						cvc.getConstraints().add(havingUnionNegConstraint);
						cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n");
					}
					else{
						/** Generate havingClause constraints */
						cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n");
						cvc.getConstraints().add(havingUnionConstraint);
						cvc.getConstraints().add(havingEachGroupNegConstraint);
						cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n");
						
					}
					
					/** Call the method for the data generation*/
					GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
				}
			}
	
			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
			//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
			cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}
		catch(Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			//ex.printStackTrace();
			throw ex;
		}
	}
	
	private static String negateConstraint(String constraint){
		String negConstraint = constraint;
		
		negConstraint = negConstraint.replaceFirst("ASSERT ", "ASSERT NOT(");
		negConstraint = negConstraint.replace(";", ");");
				
		return negConstraint;	
	}
	
	private static AggregateFunction getAggNode(Node n){
		if(n == null)
			return null;
		
		if(n.getAgg() != null) {
			return n.getAgg();
		}
		else{
			AggregateFunction left = getAggNode(n.getLeft());
			AggregateFunction right = getAggNode(n.getRight());
			
			if(left == null && right == null)
				return null;
			
			if(left != null)
				return left;
			
			return right;
		}
	}
	
	
	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return
	 */
	public static String getTableNameNoForAggWithBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getTableNameNo() != null){
			return n1.getRight().getTableNameNo();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTableNameNo() != null){
			return n1.getLeft().getTableNameNo();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableNameNoForAggWithBAONode(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableNameNoForAggWithBAONode(n1.getRight());
			}
		}
		return null;
		
	}


}
