package killMutations.whereClauseNestedBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintForUnintendedJoins;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.RelatedToParameters;
import util.TagDatasets;

/**
 * This class generates data sets to kill un-intended join mutations due to common names of the tables in each where clause nested sub query block
 * @author mahesh
 *
 */
public class UnintendedJoinsMutationsInWhereSubQuery {

	private static Logger logger = Logger.getLogger(UnintendedJoinsMutationsInWhereSubQuery.class.getName());
	/**
	 * Generates constraints to kill unintended join mutations
	 * @param cvc
	 * @throws Exception
	 */
	public static void generateDataForkillingUnintendedJoinsMutationsInWhereSubquery(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		cvc.inititalizeForDatasetQs();

		/** get the tuple assignment for this query
		 * If no possible assignment then not possible to kill this mutation*/
		if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
			return ;

		/** we have to check if there are where clause sub queries in each conjunct of outer block of query */
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs()){

			/**For each where clause sub query blocks of this conjunct*/
			/** Kill unintended join mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this sub query node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause sub query block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				/** Kill unintended join mutations in each conjunct of this where clause nested block of this query*/
				for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"NEW CONJUNCT IN KILLING UNINTENDED JOIN MUTATION: \n");
					logger.log(Level.INFO,"---------------------------------\n");
					
					/** Initialize the data structures for generating the data to kill this mutation */
					cvc.setConstraints( new ArrayList<String>());
					cvc.setStringConstraints( new ArrayList<String>());

					/** Add constraints related to parameters*/
					cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt ));

					cvc.setResultsetTableColumns1( new HashMap<Table,Vector<String>>() );

					cvc.setCVCStr("");

					cvc.setTypeOfMutation("");

					/**set the type of mutation we are trying to kill*/
					cvc.setTypeOfMutation( TagDatasets.MutationType.UNINTENDED, TagDatasets.QueryBlock.WHERE_SUBQUERY );
					
					/** get the constraints to kill this mutation*/
					String constraintString =  GenerateConstraintForUnintendedJoins.getConstraintsForUnintendedJoin( cvc, qbt, conjunct);

					if( constraintString == "")/**means there are no extra columns with same column name*/
						continue ;

					cvc.getConstraints().add("\n%---------------------------------\n%CONSTRAINTS TO KILL UNINTENDE JOINS IN WHERE CLAUSE SUBQ EURY BLOCK\n%---------------------------------\n");
					cvc.getConstraints().add( constraintString );
					cvc.getConstraints().add("\n%---------------------------------\n%END OF CONSTRAINTS TO KILL UNINTENDE JOINS IN WHERE CLAUSE SUBQ EURY BLOCK\n%---------------------------------\n");
					
					
					/** get the constraints for each from clause nested sub query block except this from clause nested sub query block*/
					for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){						

						cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");				
						cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );				
						cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");						
					}
					
					
					/**add the negative constraints for all the other conjuncts of this query block */
					for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
						if(inner != conjunct)
							cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );
					
					
					/** get constraints for this conjunct of outer query block, This also adds constraints for group by and having clause of this where clause sub query */
					/** Also Generates positive constraints for all the conditions of this sub query block conjunct */
					cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, cvc.getOuterBlock(), con) );
					
					/**add the negative constraints for all the other conjuncts of outer query block */
					for(ConjunctQueryStructure outer: cvc.getOuterBlock().getConjunctsQs())
						if( !outer.equals(con))
							cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, cvc.getOuterBlock(), outer) );
						
					/** add group by and having clause constraints for outer query block */
					cvc.getConstraints().add( QueryBlockDetails.getGroupByAndHavingClauseConstraints(cvc, cvc.getOuterBlock())) ;
					
					/**Add other related constraints for the outer query block */
					cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock())) ;
					
					/** Call the method for the data generation*/
					GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
				}
			}
		}


		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}

}
