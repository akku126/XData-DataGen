package killMutations.whereClauseNestedBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.RelatedToEquivalenceClassMutations;
import generateConstraints.UtilsRelatedToNode;

import java.util.*;
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
 * This class generates data sets to kill equivalence class mutations in the Where clause nested subquery block
 * @author mahesh
 *
 */
public class EquivalenceMutationInWhereSubQuery {

	private static Logger logger = Logger.getLogger(EquivalenceMutationInWhereSubQuery.class.getName());
	/**
	 * Generates data to kill equivalence class mutations inside Where clause nested subquery block
	 * @param cvc
	 * @throws Exception
	 */
	public static void generateDataForkillingEquivalenceClassMutationsInWhereSubquery(GenerateCVC1 cvc) throws Exception{


		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();


		/** we have to check if there are where clause sub queries in each conjunct of outer block of query */
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs()){

			/**For each where clause sub query blocks of this conjunct*/
			/** Kill equivalence class mutations in each where clause nested block of this query*/
			for(Node subQCond: con.getAllSubQueryConds()){

				/** get the index of this sub query node */
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQCond);

				/** get the where clause sub query block */
				QueryBlockDetails qbt = cvc.getOuterBlock().getWhereClauseSubQueries().get(index);

				/** Kill equivalence class mutations in each conjunct of this where clause nested block of this query*/
				for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"NEW CONJUNCT IN EC KILLING IN WHERE CLAUSE SUBQUERY BLOCK: " + conjunct);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					
					Vector<Vector<Node>> equivalenceClassesOrig = (Vector<Vector<Node>>)conjunct.getEquivalenceClasses().clone();

					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING EC IN WHERE CLAUSE SUBQUERY BLOCK: " + equivalenceClassesOrig);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					
					/** For each equivalence class of this sub query */
					//for(int i=0; i< equivalenceClassesOrig.size(); i++){
					for(int i=(equivalenceClassesOrig.size()-1); i >= 0; i--){	
						
						/**Get the equivalence class that is to be killed*/					
						Vector<Node> ec = (Vector<Node>)equivalenceClassesOrig.get(i).clone();

						/**Update the equivalence classes...these are used during tuple assignment*/
						conjunct.setEquivalenceClasses((Vector<Vector<Node>>)equivalenceClassesOrig.clone());
						conjunct.getEquivalenceClasses().remove(ec);

						/** In this iteration we are killing equivalence  class 'ec'*/
						qbt.setEquivalenceClassesKilled( new ArrayList<Node>(ec) ); 

						/** Initialize the data structures for generating the data to kill this mutation */
						cvc.inititalizeForDatasetQs();


						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.EQUIVALENCE, TagDatasets.QueryBlock.WHERE_SUBQUERY );


						/** get the tuple assignment for this query
						 * If no possible assignment then not possible to kill this mutation*/
						if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
							continue;

						/** keep a copy of this tuple assignment values */
						HashMap<String, Integer> noOfOutputTuplesOrig1 = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
						HashMap<String, Integer> noOfTuplesOrig1 = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
						HashMap<String, Integer[]> repeatedRelNextTuplePosOrig1 = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

						for(int j=0;j<ec.size(); j++){
							logger.log(Level.INFO,"\n----------------------------------\n");
							
							cvc.setConstraints( new ArrayList<String>());
							cvc.setStringConstraints( new ArrayList<String>());
							cvc.setCVCStr("");

							/** Add constraints related to parameters*/
							cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt));

							cvc.setResultsetTableColumns1( new HashMap<Table,Vector<String>>() );


							cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig1.clone() );
							cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig1.clone() );
							cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig1.clone() );

							String CVCStr = "";


							Node eceNulled = ec.get(j);


							CVCStr += "%DataSet Generated By Nulling: "+ eceNulled.toString() + "\n";



							/** Add constraints for all the From clause nested subquery blocks except this sub query block */
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){								
								cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");

								cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );

								cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");								
							}


							if(  RelatedToEquivalenceClassMutations.getConstraintsForNulledColumns(cvc, qbt, ec, eceNulled) == false)
								continue;	


							/** Add negative conditions for all other conjuncts of this sub query block*/
							for(ConjunctQueryStructure inner: qbt.getConjunctsQs())
								if(inner != conjunct)
									cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );


							/** get constraints for this conjunct of outer query block, except for where clause sub query block */
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptWhereClauseSubQueryBlock( cvc, cvc.getOuterBlock(), con) );

							/** Also Generates positive constraints for all the conditions of this sub query block conjunct*/
							/** And we need to add the positive conditions for all other where clause sub query blocks in this conjunct*/
							GenerateConstraintsForWhereClauseSubQueryBlock.generateConstraintsForKillingMutationsInWhereSubqueryBlock(cvc, qbt, con, conjunct, subQCond, 0);

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
					/** revert back the equivalence classes of this conjunct */
					conjunct.setEquivalenceClasses((Vector<Vector<Node>>)equivalenceClassesOrig.clone());
				}
			}

		}

		/** Revert back to the old assignment */
		cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		//	cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
		cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
	}
}
