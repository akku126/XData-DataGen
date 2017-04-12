package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintForUnintendedJoins;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.RelatedToParameters;
import util.TagDatasets;

/**
 * This class generates data sets to kill un-intended join mutations due to common names of the tables in outer block of the query
 * @author mahesh
 *
 */
public class UnintendedJoinsMutationsInOuterQueryBlock {

	private static Logger logger = Logger.getLogger(UnintendedJoinsMutationsInOuterQueryBlock.class.getName());
	
	public static void  generateDataForkillingUnintendedJoinsMutationsInOuterQueryBlockGen(GenerateCVC1 cvc) throws Exception{
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			generateDataForkillingUnintendedJoinsMutationsInOuterQueryBlock(cvc);
		}
		else{
			//generateDataForkillingUnintendedJoinsMutationsInOuterQueryBlockSMT(cvc);
		}
		
	}
	/**
	 * Generates constraints to kill unintended join mutations
	 * @param cvc
	 * @throws Exception
	 */
	public static void generateDataForkillingUnintendedJoinsMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{


		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING UNINTENDED JOINS DUE TO COMMON NAMES MUTATION IN OUTER BLOCK OF QUERY \n");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		try{
			/** Get outer query block of this query */
			QueryBlockDetails qbt = cvc.getOuterBlock();
	
			cvc.inititalizeForDatasetQs();
			
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return ;
			
			/** we have to kill the mutations in each conjunct*/
			for( ConjunctQueryStructure con: qbt.getConjunctsQs()){
	
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"NEW CONJUNCT IN KILLING UNINTENDED JOIN MUTATION: \n");
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				/** Initialize the data structures for generating the data to kill this mutation */
				cvc.setConstraints( new ArrayList<String>());
				cvc.setStringConstraints( new ArrayList<String>());
				cvc.setTypeOfMutation("");
				
				/**set the type of mutation we are trying to kill*/
				cvc.setTypeOfMutation( TagDatasets.MutationType.UNINTENDED, TagDatasets.QueryBlock.OUTER_BLOCK );
				
				/** Add constraints related to parameters*/
				cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt ));
				
				cvc.setResultsetTableColumns1( new HashMap<Table,Vector<String>>() );
				
				cvc.setCVCStr("");
	
				
				/** get the constraints to kill this mutation*/
				String constraintString =  GenerateConstraintForUnintendedJoins.getConstraintsForUnintendedJoin( cvc, qbt, con);
				
				if( constraintString == "")/**means there are no extra columns with same column name*/
					continue ;
				
				cvc.getConstraints().add("\n%---------------------------------\n%CONSTRAINTS TO KILL UNINTENDE JOINS IN OUTER QUERY BLOCK\n%---------------------------------\n");
				cvc.getConstraints().add( constraintString );
				cvc.getConstraints().add("\n%---------------------------------\n%END OF CONSTRAINTS TO KILL UNINTENDE JOINS IN OUTER QUERY BLOCK\n%---------------------------------\n");
				
				
				/** get the constraints for each from clause nested sub query block*/
				for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
					
					cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");				
					cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );				
					cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");
			}
				
				
				/**add the negative constraints for all the other conjuncts of this query block */
				for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
					if(inner != con)
						cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
	
				/**add positive constraints for all the conditions of this conjunct*/
				cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, con) );			
	
				/** get group by constraints */
				cvc.getConstraints().add("\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
				cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
				
				
				/** Generate havingClause constraints */
				cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
				for(int l=0; l< qbt.getNoOfGroups(); l++)
					for(int k=0; k < qbt.getAggConstraints().size();k++){
						cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
					}
				cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
				
				
										
				/** add other constraints of outer query block */
				cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );
				
				/** Call the method for the data generation*/
				GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
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

}
