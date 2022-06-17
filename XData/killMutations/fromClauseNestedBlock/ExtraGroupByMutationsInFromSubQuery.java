package killMutations.fromClauseNestedBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsToKillExtraGroupByMutations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class generates data set to kill extra group by attributes mutation in each from clause nested sub query block
 * @author mahesh
 *
 */
public class ExtraGroupByMutationsInFromSubQuery {

	private static Logger logger = Logger.getLogger(ExtraGroupByMutationsInFromSubQuery.class.getName());
	/**
	 * Generate data set to kill mutations with extra  group by attributes in each from clause nested sub query block
	 * @param cvc
	 */
	public static void generateDataForkillingExtraGroupByMutationsInFromSubquery(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		//HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		try{
		/** Kill mutations with extra  group by attributes in each from clause nested block of this query*/
		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries()){
			
			logger.log(Level.INFO,"\n----------------------------------");
			logger.log(Level.INFO,"GENERATE DATA FOR KILLING EXTRA GROUP BY MUTATION IN FROM CLAUSE NESTED SUBQUERY BLOCK: " + qbt);
			logger.log(Level.INFO,"\n----------------------------------");
			
			/** If there are no group by attributes, then no need to kill this mutation */
			if(qbt.getGroupByNodes() == null || qbt.getGroupByNodes().size() == 0)
				continue ;
			
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();
			

			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.EXTRAGROUPBY, TagDatasets.QueryBlock.FROM_SUBQUERY );
			
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
			
			/**Get the constraints for all the blocks of the query */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );
			
			GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc,false);
			/**Get the constraints to kill this mutation*/
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine("CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES INSIDE FROM CLAUSE NESTED SUBQUERY BLOCK"));
			cvc.getConstraints().add(GenerateConstraintsToKillExtraGroupByMutations.getExtraGroupByConstraints(cvc, qbt, extraColumn,tableOccurrence));
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF CONSTRAINTS TO KILL EXTRA GROUP BY ATTRIBUTES INSIDE FROM CLAUSE NESTED SUBQUERY BLOCK"));
			
			/** Call the method for the data generation*/
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc,false);
			
		}
		
		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		}
		catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
	}

}
