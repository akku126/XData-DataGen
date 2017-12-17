package killMutations.outerQueryBlock;

import generateConstraints.ConstraintGenerator;
import generateConstraints.Constraints;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForDisjunct;
import generateConstraints.GenerateJoinPredicateConstraints;
import generateConstraints.UtilRelatedToConstraints;
import generateConstraints.UtilsRelatedToNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Disjunct;
import parsing.DisjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;
import util.TagDatasets;

/**
 * This class generates data sets to kill selection conditions (of the form A.x relOP const) class mutations in the outer query block
 * @author mahesh
 *
 */
public class SelectionMutationsInOuterQueryBlock {

	private static Logger logger = Logger.getLogger(SelectionMutationsInOuterQueryBlock.class.getName());
	

	/**
	 * Generates data to kill selection conditions mutations inside outer block
	 * @param cvc
	 */
	public static void generateDataForkillingSelectionMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{

		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();

		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING SELECTION CLAUSE MUTATIONS IN OUTER QUERY BLOCK");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();
		try{
			/**Kill the selection clause mutations in each conjunct of this outer block of  query */
			for(ConjunctQueryStructure conjunct: qbt.getConjunctsQs()){
				Constraints constraints=new Constraints();
				for(ConjunctQueryStructure innerConjunct:qbt.getConjunctsQs()){
					if(conjunct!=innerConjunct){
						constraints = Constraints.mergeConstraints(cvc,constraints, GenerateConstraintsForConjunct.generateNegativeConstraintsForConjunct(cvc, qbt, innerConjunct));
					}
				}
				
				killSelectionMutationsInConjunct(cvc, conjunct, constraints);
				/*
	
				System.out.println("\n----------------------------------");
				System.out.println("NEW CONJUNCT IN SELCTION CLAUSE MUTATIONS KILLING: " + conjunct);
				System.out.println("\n----------------------------------");
	
				*//**Get the selection conditions of this conjunct*//*
				Vector<Node > selectionConds = conjunct.getSelectionConds();
	
				*//** Kill each selection condition of this conjunct*//*
				for(int i=0; i < selectionConds.size(); i++){
	
					System.out.println("\n----------------------------------");
					System.out.println("\n\nGETTING SELECTION MUTANTS\n");
					System.out.println("\n----------------------------------");
	
	
					Node sc = selectionConds.get(i);
	
					Vector<Node> scMutants = Utils.getSelectionCondMutations(sc);
	
					*//** Generate data set to kill each mutation*//*
					for(int j=0; j<scMutants.size(); j++){
	
						*//**If this mutation is not same as that of original condition*//*
						if(!( scMutants.get(j).getOperator().equalsIgnoreCase(sc.getOperator())) ){
	
							System.out.println("\n----------------------------------");
							System.out.println("KILLING : " + scMutants.get(j));
							System.out.println("----------------------------------\n");
	
							*//** This is required so that the tuple assignment for the subquery is fine*//*
							selectionConds.set(i,scMutants.get(j) );
	
							*//** Initialize the data structures for generating the data to kill this mutation *//*
							cvc.inititalizeForDataset();
	
							*//**set the type of mutation we are trying to kill*//*
							cvc.setTypeOfMutation( TagDatasets.MutationType.SELCTION, TagDatasets.QueryBlock.OUTER_BLOCK );
							
							*//** get the tuple assignment for this query
							 * If no possible assignment then not possible to kill this mutation*//*
							if(GenerateCVC1_new.tupleAssignmentForQuery(cvc) == false)
								continue;
	
							*//** Add constraints for all the From clause nested sub query blocks *//*
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
								cvc.getConstraints().add(ConstraintGenerator.addCommentLine("FROM CLAUSE SUBQUERY "));
	
								cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
	
								cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" END OF FROM CLAUSE SUBQUERY "));
							}
	
	
							*//** Generate positive constraints for all the conditions of this  conjunct *//*
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );
	
	
							*//** Add negative conditions for all other conjuncts of this query block*//*
							for(Conjunct inner: qbt.getConjuncts())
								if(inner != conjunct)
									cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
	
							*//** get group by constraints *//*
							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
							cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
	
	
							*//** Generate havingClause constraints *//*
							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
							for(int l=0; l< qbt.getNoOfGroups(); l++)
								for(int k=0; k < qbt.getAggConstraints().size();k++){
									cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
							}
							cvc.getConstraints().add(ConstraintGenerator.addCommentLine("END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK "));
	
							*//** add other constraints of outer query block *//*
							cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, qbt) );
	
							*//** Call the method for the data generation*//*
							GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
						}
					}
					*//**Revert the change in selection conditions list of this subquery block *//*
					selectionConds.set(i,sc);
				
				}
			*/}
			
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
	
	public static void killSelectionMutationsInConjunct(GenerateCVC1 cvc,ConjunctQueryStructure conjunct,Constraints constraints) throws Exception{
		
		Constraints localConstraints=new Constraints();
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();		

		/**Get the selection conditions of this conjunct*/
		Vector<Node > selectionConds = conjunct.getSelectionConds();
		try{
			/** Kill each selection condition of this conjunct*/
			for(int i=0; i < selectionConds.size(); i++){
				
				Node sc = selectionConds.get(i);
	
				Vector<Node> scMutants =  UtilsRelatedToNode.getSelectionCondMutations(sc);
				
				/** Generate data set to kill each mutation*/
				for(int j=0; j<scMutants.size(); j++){
					/**If this mutation is not same as that of original condition*/
					if(!( scMutants.get(j).getOperator().equalsIgnoreCase(sc.getOperator())) || scMutants.get(j).getIsMutant()){
	
						logger.log(Level.INFO,"\n----------------------------------");
						logger.log(Level.INFO,"Killing selectionMutations");
						logger.log(Level.INFO,"\n----------------------------------\n");
						
						/** Initialize the data structures for generating the data to kill this mutation */
						selectionConds.set(i,scMutants.get(j) );
						
						cvc.inititalizeForDatasetQs();
						 
						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.SELCTION, TagDatasets.QueryBlock.OUTER_BLOCK );
						
						/** get the tuple assignment for this query
						 * If no possible assignment then not possible to kill this mutation*/
						if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
							continue;
						
						
						localConstraints = GenerateConstraintsForConjunct.getConstraintsInConjuct(cvc, cvc.getOuterBlock(), conjunct);
						
						localConstraints = Constraints.mergeConstraints(cvc,localConstraints,constraints);
						
	/*					for(Disjunct disjunct:conjunct.disjuncts){
							localConstraints=Constraints.mergeConstraints(localConstraints,GenerateConstraintsForDisjunct.getConstraintsForDisjuct(cvc, cvc.getOuterBlock(), disjunct));
						}
						*/
						//cvc.getConstraints().add(Constraints.getConstraint(localConstraints));
						//cvc.getStringConstraints().add(Constraints.getStringConstraints(localConstraints));
						cvc.getConstraints().add(Constraints.getConstraint(cvc,localConstraints));
						cvc.getStringConstraints().addAll(Constraints.getStringConstraints(cvc,localConstraints));
						ArrayList<String> strConstraints =  Constraints.getStringConstraints(cvc,localConstraints);
						for(String constraint : strConstraints){
							cvc.getStringConstraints().add(constraint.toString());
						}
						GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
					}
				}
				selectionConds.set(i,sc);
			}
			ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
			ConstraintGenerator constrGen = new ConstraintGenerator();
			for(DisjunctQueryStructure disjunct:conjunct.disjuncts){
				String constraintString="";
				
				localConstraints.constraints.removeAllElements();
				localConstraints.stringConstraints.removeAllElements();
				
				Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
				for(int k=0; k<equivalenceClasses.size(); k++){
					Vector<Node> ec = equivalenceClasses.get(k);
					for(int i=0;i<ec.size()-1;i++){
						
						Node n1 = ec.get(i);
						Node n2 = ec.get(i+1);
						ConstraintObject constrObj = new ConstraintObject();
						constrObj.setLeftConstraint( GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, cvc.getOuterBlock(), n1,n2) );
						constrList.add(constrObj);
					
						//constraintString +=+" AND ";
					}
				}
				
				Vector<Node> selConds = conjunct.getSelectionConds();
				for(int k=0; k< selConds.size(); k++){
	
					String tableNo = selConds.get(k).getLeft().getTableNameNo();
					int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
					int count = cvc.getNoOfTuples().get(tableNo) * qbt.getNoOfGroups();/** We should generate the constraints across all groups */
					for(int l=1;l<=count;l++){
						//constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, selectionConds.get(k),l+offset-1) +" AND ";
						ConstraintObject constrObj = new ConstraintObject();
						constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, selectionConds.get(k),l+offset-1));
						constrList.add(constrObj);
					}
				}
				
				Vector<Node> allConds = conjunct.getAllConds();
				for(int k=0; k<allConds.size(); k++){
					//constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, qbt, allConds) +" AND ";
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, qbt, allConds));
					constrList.add(constrObj);
				}
			
				
			
				//constrList = new ArrayList<ConstraintObject>();
				String stringConstraint="";
				
				Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();
				for(int k=0; k<stringSelectionConds.size(); k++){
	
					String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
					int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
					int count = cvc.getNoOfTuples().get(tableNo) * qbt.getNoOfGroups();/** We should generate the constraints across all groups */;
					for(int l=1;l<=count;l++){
						//stringConstraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, stringSelectionConds.get(k),l+offset-1) +" AND ";
						//ConstraintObject constrObj = new ConstraintObject();
						//constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, stringSelectionConds.get(k),l+offset-1));
						//constrList.add(constrObj);
						localConstraints.stringConstraints.add(constrGen.genPositiveCondsForPred(qbt, stringSelectionConds.get(k),l+offset-1)); 
					}
				}
				
				Vector<Node> likeConds = conjunct.getLikeConds();
				for(int k=0; k<likeConds.size(); k++){
	
					String tableNo = likeConds.get(k).getLeft().getTableNameNo();
					int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
					int count = cvc.getNoOfTuples().get(tableNo) * qbt.getNoOfGroups();/** We should generate the constraints across all groups */;
					for(int l=1;l<=count;l++){
						//stringConstraint+= GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, likeConds.get(k),l+offset-1)+" AND ";
						ConstraintObject constrObj = new ConstraintObject();
						constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, likeConds.get(k),l+offset-1));
						constrList.add(constrObj);
					}
				}
			//	stringConstraint += constrGen.generateANDConstraints(constrList);
				//if(!stringConstraint.equalsIgnoreCase("")){
					//stringConstraint = stringConstraint.substring(0, stringConstraint.length()-5);
	
				//}
				
				constraintString += constrGen.generateANDConstraints(constrList);
				if(!constraintString.equalsIgnoreCase("")){
					//constraintString=constraintString.substring(0,constraintString.length()-5);
					localConstraints.constraints.add(constraintString);
				}
				
				localConstraints=Constraints.mergeConstraints(cvc,localConstraints, constraints);
				
				DisjunctQueryStructure killDisjunct = null;
				for(DisjunctQueryStructure innerDisjunct:conjunct.disjuncts){
					if(innerDisjunct.equals(disjunct))
						killDisjunct=innerDisjunct;
					else{
						localConstraints=Constraints.mergeConstraints(cvc,localConstraints,GenerateConstraintsForDisjunct.getConstraintsForDisjuct(cvc, qbt, innerDisjunct));
					}
				}
				
				killSelectionMutationsInDisjunct(cvc, killDisjunct, localConstraints);
				
			}
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
		logger.log(Level.SEVERE,e.getMessage(),e);
		throw e;
		}
	}
	
	public static void killSelectionMutationsInDisjunct(GenerateCVC1 cvc,DisjunctQueryStructure disjunct,Constraints constraints) throws Exception{
		Constraints localConstraints=new Constraints();
		
		Vector<Node > selectionConds = disjunct.getSelectionConds();
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvc.getOuterBlock();		
		try{
			for(int i=0; i < selectionConds.size(); i++){
				
				Node sc = selectionConds.get(i);
	
				Vector<Node> scMutants = UtilsRelatedToNode.getSelectionCondMutations(sc);
				
				ConstraintGenerator constrGen = new ConstraintGenerator();
				ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
				
				/** Generate data set to kill each mutation*/
				for(int j=0; j<scMutants.size(); j++){
					/**If this mutation is not same as that of original condition*/
					if(!( scMutants.get(j).getOperator().equalsIgnoreCase(sc.getOperator())) ){
	
						logger.log(Level.INFO,"\n----------------------------------");
						logger.log(Level.INFO,"KILLING : " + scMutants.get(j));
						logger.log(Level.INFO,"\n----------------------------------\n");
						
						
						/** Initialize the data structures for generating the data to kill this mutation */
						cvc.inititalizeForDatasetQs();
						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.SELCTION, TagDatasets.QueryBlock.OUTER_BLOCK );
						
						/** get the tuple assignment for this query
						 * If no possible assignment then not possible to kill this mutation*/
						if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
							continue;
						
						String constraintString="";
						String tableNo = scMutants.get(j).getLeft().getTableNameNo();
						int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
						int count = cvc.getNoOfTuples().get(tableNo) * qbt.getNoOfGroups();/** We should generate the constraints across all groups */
						for(int l=1;l<=count;l++) {
							//constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, scMutants.get(j),l+offset-1) +" AND ";
							
							ConstraintObject constrObj = new ConstraintObject();
							constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, scMutants.get(j) ,l+offset-1));
							constrList.add(constrObj);
						}
						
						Vector<Node> allConds = disjunct.getAllConds();
						
						/**get constraint*/
						String constraint = "";//GenerateJoinPredicateConstraints.getNegativeConstraintsForNonEquiJoins(cvc, qbt, allConds) ;
						
						ConstraintObject constrObj1 = new ConstraintObject();
						constrObj1.setLeftConstraint(GenerateJoinPredicateConstraints.getNegativeConstraintsForNonEquiJoins(cvc, qbt, allConds) );
						constrList.add(constrObj1);
						
						//constraint=UtilRelatedToConstraints.removeAssert(constraint);
						
						//if(!constraint.equalsIgnoreCase("")){
						//	constraintString += "(" + constraint + ") AND ";
						//}
						constraintString += constrGen.generateANDConstraints(constrList);
						constraint = constraintString;
						
						constrList = new ArrayList<ConstraintObject>();
						Vector<Node> negativeSelConds = new Vector<Node>();
						for(int k=0;k<selectionConds.size();k++){
							Node node=selectionConds.get(k);
							if(k!=j){
								if(node.getType().equalsIgnoreCase(Node.getBroNodeType()) || node.getType().equalsIgnoreCase(Node.getBaoNodeType()) ||node.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
										node.getType().equalsIgnoreCase(Node.getAndNodeType()) || node.getType().equalsIgnoreCase(Node.getOrNodeType())){				
									
									negativeSelConds.add( GenerateCVCConstraintForNode.getNegativeCondition(node) );
								}
							}
						}
						
						/**Generate constraints for the negative conditions*/
						for(int k = 0; k < negativeSelConds.size(); k++){
	
							/**get table details*/
							tableNo = negativeSelConds.get(k).getLeft().getTableNameNo();
							offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
							count = cvc.getNoOfTuples().get(tableNo)* qbt.getNoOfGroups();/** We should generate the constraints across all groups */;;
							for(int l = 1; l <= count; l++){
								
								
								ConstraintObject constrObj = new ConstraintObject();
								constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, negativeSelConds.get(k),l+offset-1) );
								constrList.add(constrObj);
								
								//constraintString+=GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, negativeSelConds.get(k),l+offset-1) + " AND ";
							}
						}
						constraintString += constrGen.generateANDConstraints(constrList);
						constraint = constraintString;
						
						constrList = new ArrayList<ConstraintObject>();
						if(!constraint.equalsIgnoreCase("")){
							//constraint=constraint.substring(0, constraint.length()-5);
							localConstraints.constraints.add(constraint);
						}
						
						/**Generate negative constraints for string selection conditions */
						Vector<Node> stringSelectionConds = disjunct.getStringSelectionConds();	
	
						/**get negative conditions for these nodes*/
						Vector<Node> negativeStringSelConds = GenerateCVCConstraintForNode.getNegativeConditions(stringSelectionConds);
	
						constraint="";
						/**Generate constraints for the negative conditions*/
						for(int k = 0; k < negativeStringSelConds.size(); k++){
	
							/**get table details*/
							tableNo = negativeStringSelConds.get(k).getLeft().getTableNameNo();
							offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
							count = cvc.getNoOfTuples().get(tableNo)* qbt.getNoOfGroups();/** We should generate the constraints across all groups */;;
							for(int l = 1; l <= count; l++){
									//constraint += GenerateCVCConstraintForNode + " AND ";
									ConstraintObject constrObj = new ConstraintObject();
									constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, negativeStringSelConds.get(k),l+offset-1));
									constrList.add(constrObj);		
							}
						}
						constraint +=  constrGen.generateANDConstraints(constrList);
						/**Generate negative constraints for like conditions */
						Vector<Node> likeConds = disjunct.getLikeConds();
	
						/**get negative conditions for these nodes*/
						Vector<Node> negativeLikeConds = GenerateCVCConstraintForNode.getNegativeConditions(likeConds);
						constrList = new ArrayList<ConstraintObject>();
						constraint="";
						for(int k=0; k<likeConds.size(); k++){
	
							/**get table details*/
							tableNo = negativeLikeConds.get(k).getLeft().getTableNameNo();
							offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
							count = cvc.getNoOfTuples().get(tableNo)* qbt.getNoOfGroups();/** We should generate the constraints across all groups */;;
							for(int l=1;l<=count;l++){
								//constraint+=GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, negativeLikeConds.get(k),l+offset-1) +" AND ";
							
								ConstraintObject constrObj = new ConstraintObject();
								constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, negativeLikeConds.get(k),l+offset-1));
								constrList.add(constrObj);
							}
						}
						constraint += constrGen.generateANDConstraints(constrList);
						
						if(!constraint.equalsIgnoreCase("")){
							constraint=constraint.substring(0, constraint.length()-5);
							localConstraints.stringConstraints.add(constraint);
						}
						
						localConstraints = Constraints.mergeConstraints(cvc,localConstraints, constraints);
						
						for(ConjunctQueryStructure conjunct:disjunct.conjuncts){
							localConstraints=Constraints.mergeConstraints(cvc,localConstraints,GenerateConstraintsForConjunct.generateNegativeConstraintsForConjunct(cvc, cvc.getOuterBlock(), conjunct));
						}
	
						cvc.getConstraints().add(Constraints.getConstraint(cvc,localConstraints));
					//	cvc.getStringConstraints().add(Constraints.getStringConstraints(cvc,localConstraints));
						ArrayList<String> strConstraints =  Constraints.getStringConstraints(cvc,localConstraints);
						for(String constraintobj : strConstraints){
							cvc.getStringConstraints().add(constraintobj.toString());
						}
						GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
					}
				}
			}
			
			for(ConjunctQueryStructure conjunct:disjunct.conjuncts){
				String constraintString="";
				localConstraints.constraints.removeAllElements();
				localConstraints.stringConstraints.removeAllElements();
				ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
				ConstraintGenerator constrGen = new ConstraintGenerator();
				
				Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
				for(int k=0; k<equivalenceClasses.size(); k++){
					Vector<Node> ec = equivalenceClasses.get(k);
					for(int i=0;i<ec.size()-1;i++){
						Node n1 = ec.get(i);
						Node n2 = ec.get(i+1);
						
						ConstraintObject constrObj = new ConstraintObject();
						constrObj.setLeftConstraint( GenerateJoinPredicateConstraints.genNegativeConds(cvc, cvc.getOuterBlock(), n1,n2) );
						constrList.add(constrObj);
						
						
						//constraintString += GenerateJoinPredicateConstraints.genNegativeConds(cvc, cvc.getOuterBlock(), n1,n2) +" AND ";
					}
				}
				constraintString += constrGen.generateANDConstraints(constrList);
				Vector<Node> allConds = disjunct.getAllConds();
				constrList = new ArrayList<ConstraintObject>();
				
				/**get constraint*/
				String constraint = "";//GenerateJoinPredicateConstraints.getNegativeConstraintsForNonEquiJoins(cvc, qbt, allConds) ;
	
				ConstraintObject constrObj1 = new ConstraintObject();
				constrObj1.setLeftConstraint(GenerateJoinPredicateConstraints.getNegativeConstraintsForNonEquiJoins(cvc, qbt, allConds) );
				constrList.add(constrObj1);
				
				//constraint = constrGen.generateANDConstraints(constrList);
				
				//constraint=UtilRelatedToConstraints.removeAssert(constraint);
				
				//if(!constraint.equalsIgnoreCase("")){
					//constraintString+=constraint + " AND ";
				//}
	
				/** Now generate Negative constraints for selection conditions */
				Vector<Node> selConds = disjunct.getSelectionConds();
	
				/**get negative conditions for these nodes*/
				Vector<Node> negativeSelConds = GenerateCVCConstraintForNode.getNegativeConditions(selConds);
	
				/**Generate constraints for the negative conditions*/
				for(int k = 0; k < negativeSelConds.size(); k++){
	
					/**get table details*/
					String tableNo = negativeSelConds.get(k).getLeft().getTableNameNo();
					int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
					int count = cvc.getNoOfTuples().get(tableNo)* qbt.getNoOfGroups();/** We should generate the constraints across all groups */;;
					for(int l = 1; l <= count; l++){
						//constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, negativeSelConds.get(k),l+offset-1) + " AND ";
					
						ConstraintObject constrObj = new ConstraintObject();
						constrObj.setLeftConstraint(GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, negativeSelConds.get(k),l+offset-1));
						constrList.add(constrObj);
					}
					constraintString += constrGen.generateANDConstraints(constrList);
				}
	
				if(!constraintString.equalsIgnoreCase("")){
					//constraintString=constraintString.substring(0, constraint.length()-5);
				}
				
				localConstraints.constraints.add(constraintString);
				String stringConstraint="";
	
				/**Generate negative constraints for string selection conditions */
				Vector<Node> stringSelectionConds = disjunct.getStringSelectionConds();	
	
				/**get negative conditions for these nodes*/
				Vector<Node> negativeStringSelConds = GenerateCVCConstraintForNode.getNegativeConditions(stringSelectionConds);
	
				/**Generate constraints for the negative conditions*/
				for(int k = 0; k < negativeStringSelConds.size(); k++){
	
					/**get table details*/
					String tableNo = negativeStringSelConds.get(k).getLeft().getTableNameNo();
					int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
					int count = cvc.getNoOfTuples().get(tableNo)* qbt.getNoOfGroups();/** We should generate the constraints across all groups */;;
					for(int l = 1; l <= count; l++){
						//stringConstraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, negativeStringSelConds.get(k),l+offset-1) +" AND ";
						ConstraintObject constrObj = new ConstraintObject();
						constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, negativeStringSelConds.get(k),l+offset-1) );
						constrList.add(constrObj);
					}
					
				}
	
	
				/**Generate negative constraints for like conditions */
				Vector<Node> likeConds = disjunct.getLikeConds();
	
				/**get negative conditions for these nodes*/
				Vector<Node> negativeLikeConds = GenerateCVCConstraintForNode.getNegativeConditions(likeConds);
	
				for(int k=0; k<likeConds.size(); k++){
	
					/**get table details*/
					String tableNo = negativeLikeConds.get(k).getLeft().getTableNameNo();
					int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
	
					int count = cvc.getNoOfTuples().get(tableNo)* qbt.getNoOfGroups();/** We should generate the constraints across all groups */;;
					for(int l=1;l<=count;l++){
						//stringConstraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, negativeLikeConds.get(k),l+offset-1) +" AND ";
						ConstraintObject constrObj = new ConstraintObject();
						constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt, negativeLikeConds.get(k),l+offset-1) );
						constrList.add(constrObj);
					}
				}
				stringConstraint += constrGen.generateANDConstraints(constrList);
				
				//if(!stringConstraint.equalsIgnoreCase("")){
				//	stringConstraint = stringConstraint.substring(0, stringConstraint.length()-5);
			//	}
				localConstraints.stringConstraints.add(stringConstraint);
				
				localConstraints = Constraints.mergeConstraints(cvc,localConstraints, constraints);
				for(ConjunctQueryStructure innerConjunct:disjunct.conjuncts){
					if(innerConjunct!=conjunct){
						localConstraints = Constraints.mergeConstraints(cvc,localConstraints, GenerateConstraintsForConjunct.generateNegativeConstraintsForConjunct(cvc, qbt, innerConjunct));
					}
				}
				killSelectionMutationsInConjunct(cvc,conjunct,localConstraints);
				
			}
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
		logger.log(Level.SEVERE,e.getMessage(),e);
		throw e;
		}
	}

}
