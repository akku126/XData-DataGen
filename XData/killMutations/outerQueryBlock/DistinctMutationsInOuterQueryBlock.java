package killMutations.outerQueryBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsToKillDistinctMutations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;
import util.Utilities;

/**
 * This class generates data sets to kill distinct clause mutations in the outer block of the query
 * @author mahesh
 *
 */
public class DistinctMutationsInOuterQueryBlock {

	private static Logger logger = Logger.getLogger(DistinctMutationsInOuterQueryBlock.class.getName());

	public static void  generateDataForkillingDistinctMutationsInOuterQueryBlockGen (GenerateCVC1 cvc) throws Exception{

		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingDistinctMutationsInOuterQueryBlock(cvc);
		}
		else{
			//generateDataForkillingDistinctMutationsInOuterQueryBlockSMT(cvc);
		}

	}
	/**
	 * Generates data to kill distinct clause mutations in the outer block of the query
	 * @param cvc
	 */

	public static void generateDataForkillingDistinctMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING DISTINCT CLAUSE MUTATIONS IN OUTER QUERY BLOCK");
		logger.log(Level.INFO,"----------------------------------\n");
		try{
			/** Get outer query block of this query */
			QueryBlockDetails qbt = cvc.getOuterBlock();

			/** If there is an unconstrained aggregation there is no need to generate distinct tuples- 
			 * Mutation like SUM Vs SUM(DISTINCT) are part of aggregation mutations */
			if(qbt.getAggFunc() != null && qbt.getAggFunc().size() != 0)
				return ;

			/**FIXME: If any sub set of the projected columns of this query block form group by attributes for any from clause sub query block (linked to this query block) 
			 * then it is not possible to kill this mutation, because the sub query will output only one tuple
			 * But it may be possible to kill if there are joins involved with the from clause nested sub query block */
			/*for(GenerateCVC1 cvc1: cvc.FromClauseSubQueries)
				if(cvc.projectedCols.containsAll(cvc1.groupByNodes))
					return;*/
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();

			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation( TagDatasets.MutationType.DISTINCT, TagDatasets.QueryBlock.OUTER_BLOCK );

			/**The projected columns become single valued attributes because the two tuples will have same value for this projected columns 
			 * FIXME: What if finalCount>2 */
			qbt.setSingleValuedAttributesAdd( new HashSet<Node>(qbt.getProjectedCols()) );


			/** Initialize aggregation constraints data structure*/
			qbt.setAggConstraints( new ArrayList<Node>());

			/**get aggregation in this query block*/
			Vector<Node> aggConstraints = new Vector<Node>();
			if(qbt.getHavingClause() != null)
				Utilities.flattenConstraints( aggConstraints, qbt.getHavingClause());

			/**Update query block aggregation list*/
			qbt.setAggConstraints( new ArrayList<Node>(aggConstraints));

			/**get the count needed*/
			if(CountEstimationRelated.getCountNeededToKillDistinctMutation(cvc, qbt) == false)
				return ;

			/** get table name */
			String tableNameNo = null;

			Node n1 = null;
			for(Node n: qbt.getProjectedCols())
				for(QueryBlockDetails qb : cvc.getOuterBlock().getFromClauseSubQueries())
					if(!qb.getProjectedCols().contains(n)){
						tableNameNo = n.getTableNameNo();
						n1 = n;
					}

			/**If this relation is involved in equi-joins then we can ensure multiple tuples at the output, even if this relation contains a single tuple 
			 * FIXME: But what if all the relations in this equivalence class contains a single tuple*/
			for(ConjunctQueryStructure con: qbt.getConjunctsQs())
				for(Vector<Node> ec: con.getEquivalenceClasses())
					for(Node n2: ec)
						if(n2.getTableNameNo().equalsIgnoreCase(tableNameNo))
							tableNameNo = null;


			/**assign the number of tuples for the this query block*/
			if( QueryBlockDetails.getTupleAssignment( cvc, qbt, tableNameNo) == false)
				return ;

			/**get the tuple assignment for all other query blocks*/
			if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvc, qbt) == false)
				return ;

			/** Add constraints for all the blocks of the query */
			cvc.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(cvc));

			cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" DISTINCT CONSTRAINTS FOR OUTER BLOCK OF QUERY"));
			cvc.getConstraints().add( (GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(cvc, qbt)) );
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" END OF DISTINCT CONSTRAINTS FOR OUTER BLOCK OF QUERY"));

			/** Call the method for the data generation*/
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);

			/**Reset the variable*/
			qbt.setConstrainedAggregation(false);

			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
			cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
			cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
		}
	}

}
