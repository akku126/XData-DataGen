package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.RelatedToEquivalenceClassMutations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Conjunct;
import parsing.JoinClauseInfo;
import parsing.Node;
import parsing.Table;

import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.RelatedToParameters;
import util.TagDatasets;

/**
 * This class is used for generating dataset for killing missing join mutation 
 * of relation referenced by relations in the query
 * 
 * This class needs to be re-worked. Tuple assignments needs to be revisited.
 * @author shree
 *
 */

public class MissingJoinMutations {

	private static Logger logger = Logger.getLogger(MissingJoinMutations.class.getName());
	
	public static void generateDataForkillingMissingJoinMutations(GenerateCVC1 cvc) throws Exception{
		// keep a copy of this tuple assignment values 
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		HashMap<String, Integer> repeatedRelationCountOrig = (HashMap<String, Integer>) cvc.getRepeatedRelationCount().clone();
		HashMap<String, Integer[]> tableNameOrig =(HashMap<String, Integer[]>) cvc.getTableNames().clone();
		//ArrayList <Conjunct> conOriginal =new ArrayList<Conjunct>(cvc.getOuterBlock().getConjuncts());
		ArrayList <Conjunct> conOriginal = cloneListOfConjuncts(cvc.getOuterBlock().getConjuncts());
		
		//Vector<Conjunct> parserConjucts = new Vector<Conjunct>(cvc.getqParser().getConjuncts());
		Vector<Conjunct> parserConjucts = cloneConjuncts(cvc.getqParser().getConjuncts());
		
		 
		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING MISSING JOIN CLAUSE MUTATIONS IN OUTER BLOCK OF QUERY");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		try{
				
		
				//ADD JOIN CONDITION IF REFERENCE ATTRIBUTES are there in projected cols or selection condition or having clause
			if(cvc.getqParser()!= null && !(cvc.getqParser().setOperator !=null)){
				cvc.inititalizeForDataset();
				
				//Update the conjunct with additional Missing join conditions
				updateCVCForMissingJoins(cvc,noOfTuplesOrig,noOfOutputTuplesOrig,repeatedRelNextTuplePosOrig,repeatedRelationCountOrig);
				
				/** get the tuple assignment for this query
				 * If no possible assignment then not possible to kill this mutation*/
				if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
					return ;
			

				/** Get outer query block of this query */
				QueryBlockDetails qbt =cvc.getOuterBlock();
				
				/** we have to kill the mutations in each conjunct*/
		
				/**Kill the non equi-join clause mutations in each conjunct of this outer block of  query */
				for(Conjunct conjunct: qbt.getConjuncts()){
		
					Vector<Vector<Node>> equivalenceClassesOrig = (Vector<Vector<Node>>)conjunct.getEquivalenceClasses().clone();
					 
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"NEW CONJUNCT IN NEC KILLING: " + conjunct);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					
					/**Get the non equi-join conditions of this conjunct*/
					Vector<Node > allConds = conjunct.getAllConds();
		
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING NON EQUI JOIN PREDICATES IN OUTER BLOCK OF QUERY: " + allConds);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"NEW CONJUNCT IN EC KILLING: " + conjunct);
					logger.log(Level.INFO,"\n----------------------------------\n");
					
					/** Keep a copy of the original equivalence classes*/
				
		
					logger.log(Level.INFO,"\n----------------------------------");
					logger.log(Level.INFO,"KILLING EC: " + equivalenceClassesOrig);
					logger.log(Level.INFO,"----------------------------------\n");
					
					/** Kill each equivalence clause mutations of this conjunct*/
				//	for(int i=0; i<equivalenceClassesOrig.size();i++){
					for(int i=(equivalenceClassesOrig.size()-1); i >= 0; i--){	
						/**Get the equivalence class that is to be killed*/					
						Vector<Node> ec = (Vector<Node>)equivalenceClassesOrig.get(i).clone();
		
						/**Update the equivalence classes...these are used during tuple assignment*/
						conjunct.setEquivalenceClasses((Vector<Vector<Node>>)equivalenceClassesOrig.clone());
						conjunct.getEquivalenceClasses().remove(ec);
		
						/** In this iteration we are killing equivalence  class 'ec'*/
						qbt.setEquivalenceClassesKilled( new ArrayList<Node>(ec) ); 
		
						/** Initialize the data structures for generating the data to kill this mutation */
						cvc.inititalizeForDataset();
		
						/**set the type of mutation we are trying to kill*/
						cvc.setTypeOfMutation( TagDatasets.MutationType.MISSINGJOINS,TagDatasets.QueryBlock.OUTER_BLOCK);
						
						/** get the tuple assignment for this query
						 * If no possible assignment then not possible to kill this mutation*/
						if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
							continue;
		
						/** keep a copy of this tuple assignment values */
						//HashMap<String, Integer> noOfOutputTuplesOrig1 = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
						//HashMap<String, Integer> noOfTuplesOrig1 = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
						//HashMap<String, Integer[]> repeatedRelNextTuplePosOrig1 = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
						
						for(int j=0;j<ec.size(); j++){
							
							logger.log(Level.INFO,"----------------------------------\n");
							
							/** In this iteration we are killing equivalence  class 'ec'*/
							qbt.setEquivalenceClassesKilled( new ArrayList<Node>(ec) ); 
					
							cvc.setConstraints( new ArrayList<String>());
							cvc.setStringConstraints( new ArrayList<String>());
							cvc.setCVCStr("");	
							/** Add constraints related to parameters*/
							cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt ));
							cvc.setResultsetTableColumns1( new HashMap<Table,Vector<String>>() );

							//cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig1.clone() );
							//cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig1.clone() );
							//cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig1.clone() );
							 
							String CVCStr = "";
							
							/** Add constraints for all the From clause nested sub query blocks */
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
									cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");
									
									cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
									
									cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");
							}
							
							Node eceNulled = ec.get(j);
							CVCStr += "%DataSet Generated By Nulling: "+ eceNulled.toString() + "\n";
							
							if( RelatedToEquivalenceClassMutations.getConstraintsForNulledColumns(cvc, qbt, ec, eceNulled) == false)
								continue;
							
							/** Generate positive constraints for all the conditions of this  conjunct */
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, conjunct) );
							
							
							/** Add negative conditions for all other conjuncts of this query block*/
							for(Conjunct inner: qbt.getConjuncts())
								if(inner != conjunct)
									cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	
								
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
							cvc.setCVCStr(CVCStr);
							
							/** Call the method for the data generation*/
							GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
						}
					}
					/** revert back the equivalence classes of this conjunct */
					conjunct.setEquivalenceClasses((Vector<Vector<Node>>)equivalenceClassesOrig.clone());
				
					//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
							
				} 
				cvc.getqParser().setConjuncts(new Vector(parserConjucts));
				cvc.getOuterBlock().setConjuncts( new ArrayList(conOriginal));
				// Revert back to the old assignment 
				cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
		//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
				cvc.setRepeatedRelationCount((HashMap<String, Integer>)repeatedRelationCountOrig.clone());
				cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
				cvc.setTableNames( (HashMap<String, Integer[]>)tableNameOrig.clone());
			/********************************************************************************************************/
			}
		
			}catch (TimeoutException e){
				logger.log(Level.SEVERE,e.getMessage(),e);		
				throw e;
			}catch(Exception e){
				logger.log(Level.SEVERE,e.getMessage(),e);
				throw e;
			}
	}
		
	public static void updateCVCForMissingJoins(GenerateCVC1 cvc, HashMap<String, Integer> noOfTuplesOrig,HashMap<String, Integer> noOfOutputTuplesOrig,HashMap<String, Integer[]> repeatedRelNextTuplePosOrig,HashMap<String, Integer>  repeatedRelationCountOrig){
		
		QueryBlockDetails qbt = cvc.getOuterBlock();
		/**get the list of tables in this query block*/
		HashMap<String, Table> tables = QueryBlockDetails.getListOfTablesInQueryBlock(cvc, qbt);

		/**for each table, get the extra column names which have same name but not involved in equivalence classes*/
		Vector<Vector<Node>> additionalJoins = getMissingJoinConditions( cvc, tables, qbt, noOfTuplesOrig, noOfOutputTuplesOrig,repeatedRelNextTuplePosOrig,repeatedRelationCountOrig);
		Vector<Vector<Node>> additionalEqvClasses=new Vector<Vector<Node>>();
		boolean eqClassNotExists = false;
		/** Frame new equivalence classes and generate equi joins on them **/
		/** get the additional Join node conditions **/

		/* if(qbt.getConjuncts() != null 
				 && qbt.getConjuncts().size() > 0
				 && qbt.getConjuncts().get(0).getEquivalenceClasses() != null){
			 
			 for(int k = 0; k < additionalJoins.size(); k++){
				 if(!(qbt.getConjuncts().get(0).getEquivalenceClasses().contains(additionalJoins.get(k)))){
					 ((Conjunct)qbt.getConjuncts().get(0)).getEquivalenceClasses().add(additionalJoins.get(k));
				 }
			 }
		 }
		 else {	
			 // Conjunct.createConjunct(cvc.getqParser(),additionalJoins);
			 //	 qbt.getConjuncts().addAll(new ArrayList<Conjunct>(cvc.getqParser().getConjuncts()) );
			 if(additionalJoins != null && additionalJoins.size() > 0){
				 ((Conjunct)qbt.getConjuncts().get(0)).getEquivalenceClasses().addAll(additionalJoins);
			 }
		}*/
		 if(qbt.getConjuncts() != null 
				 && qbt.getConjuncts().size() > 0
				 && additionalJoins != null && additionalJoins.size() > 0){
				 for(int k = 0; k < additionalJoins.size(); k++){
					 if(!(qbt.getConjuncts().get(0).getEquivalenceClasses().contains(additionalJoins.get(k)))){
						 additionalEqvClasses.add(additionalJoins.get(k));
						 eqClassNotExists = true;
					 }
					 
				 }qbt.getConjuncts().get(0).getEquivalenceClasses().addAll(additionalJoins);
				 
				 }
	}

	public static Vector<Vector<Node>> getMissingJoinConditions(GenerateCVC1 cvc, HashMap<String, Table> tablesList,QueryBlockDetails qbt,
			HashMap<String, Integer> noOfTuplesOrig,HashMap<String, Integer> noOfOutputTuplesOrig,HashMap<String, Integer[]> repeatedRelNextTuplePosOrig,HashMap<String, Integer>  repeatedRelationCountOrig){ 
	
		Vector<Vector<Node>>  equivalenceClasses = new Vector<Vector<Node>>();
		Vector<Vector<Node>> addConditions = new Vector<Vector<Node>>();
	// First flatten all the join/selection condition trees
	// And select the join conditions in a special vector joinConds
			
	/**get the list of all table occurrences*/
	ArrayList< String> tableNamesNo = new ArrayList<String>( tablesList.keySet());
	ArrayList<String> existingRelation = new ArrayList<String>();
	
	/**for each pair of table occurrences check*/
	for(int i = 0; i < tableNamesNo.size(); i++){
		
		String tableName = tableNamesNo.get(i).substring(0, tableNamesNo.get(i).length()-1);
		int getTableNameNo = Integer.parseInt(tableNamesNo.get(i).substring(tableNamesNo.get(i).length()-1, tableNamesNo.get(i).length())); /*To be changed for tablenameno >= 10*/
		
		for(int m=0;m<cvc.getqParser().getForeignKeyVectorOriginal().size();m++){
			
		
			JoinClauseInfo  jnClause = cvc.getqParser().getForeignKeyVectorOriginal().get(m);
			if(!(existingRelation.contains(jnClause.getJoinTable1().getTableName()+jnClause.getJoinTable2().getTableName()+jnClause.getJoinAttribute1().getColumnName()))
					&& jnClause.getJoinAttribute1().getColumnName().equals(jnClause.getJoinAttribute2().getColumnName())){
				
			existingRelation.add(jnClause.getJoinTable1().getTableName()+jnClause.getJoinTable2().getTableName()+jnClause.getJoinAttribute1().getColumnName());
			if(jnClause.getJoinTable1().getTableName().equalsIgnoreCase(tableName)){
				
				//If foreign key reference exists for the table used in the query
				//Add the relation to equivalence classes
				
				getTableNameNo = getTableNameNo;//+1;
				String tableNo = jnClause.getJoinTable1().getTableName()+(getTableNameNo);
				/**create a node for first column*/
				Node n1 = new Node();
				n1.setColumn(jnClause.getJoinAttribute1());
				
				/** set query type and index*/
				int qType = 0;
				int qIndex = -1;
				n1.setQueryIndex(qIndex);
				n1.setQueryType(qType);
				
				 
				/**set other details*/
				n1.setTable((jnClause.getJoinTable1()));
				n1.setTableAlias("");
				n1.setTableNameNo(jnClause.getJoinTable1().getTableName()+String.valueOf(getTableNameNo));
				n1.setType(Node.getColRefType());
				
				//Set details of newly added reference table in CVC
				//Set the tuple position based on existing tuple position and add 1 to existing tuple position

				int noOfTuples = 1;
				Integer qindex [] = {0,-1};
				if(!cvc.getRepeatedRelationCount().containsKey(jnClause.getJoinTable1().getTableName())){
					 cvc.getRepeatedRelationCount().put(jnClause.getJoinTable1().getTableName(), getTableNameNo);
				} 
				cvc.getTableNames().put(jnClause.getJoinTable1().getTableName()+String.valueOf(getTableNameNo),qindex);
				HashMap<String,Integer> noOfOutputTuples = (HashMap<String,Integer>)cvc.getQuery().getRepeatedRelationCount().clone();
				
				if(noOfOutputTuples.containsKey(jnClause.getJoinTable1())
						&& noOfOutputTuples.get(jnClause.getJoinTable1())<noOfTuples){
					
					noOfOutputTuples.put(jnClause.getJoinTable1().getTableName(), noOfTuples);
				}
				
				if(!noOfOutputTuples.containsKey(jnClause.getJoinTable1())){
					noOfOutputTuples.put(jnClause.getJoinTable1().getTableName(), noOfTuples);
				}
				else{		
					noOfOutputTuples = (HashMap<String,Integer>)cvc.getQuery().getRepeatedRelationCount().clone();
				}
				HashMap<String, Integer[]> repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
				
				Iterator<String> itr = cvc.getRepeatedRelationCount().keySet().iterator();
				while(itr.hasNext()){
					tableName = itr.next();
					int c =  cvc.getRepeatedRelationCount().get(tableName);
					for(int i1=1;i1<=c;i1++){
						Integer[] tuplePos = new Integer[32];
						tuplePos[1] = i1;//Meaning first tuple is at pos i
						repeatedRelNextTuplePos.put(tableName+i1, tuplePos);
						cvc.getNoOfTuples().put(tableName+i1, 1);
						cvc.getCurrentIndexCount().put(tableName+i1, i);
					}
				}

				int cnt = 0;
			
				/**create a node for second column*/
				Node n2 = new Node();
				n2.setColumn(jnClause.getJoinAttribute2());

				/** set query type and index*/
				qType = 0;// cvc.getTableNames().get( tableNamesNo.get(j))[0];
				qIndex = -1;//cvc.getTableNames().get( tableNamesNo.get(j))[1];
				n2.setQueryIndex(qIndex);
				n2.setQueryType(qType);
				
				/**set other details*/ 
				n2.setTable(jnClause.getJoinTable2());
				n2.setTableAlias("");
				
				noOfTuples = 1;
				if(!cvc.getRepeatedRelationCount().containsKey(jnClause.getJoinTable2().getTableName())){
					 cvc.getRepeatedRelationCount().put(jnClause.getJoinTable2().getTableName(), getTableNameNo);
				}
				n2.setTableNameNo(jnClause.getJoinTable2().getTableName()+getTableNameNo);
				cvc.getTableNames().put(jnClause.getJoinTable2().getTableName()+String.valueOf(getTableNameNo),qindex);
				noOfOutputTuples = (HashMap<String,Integer>)cvc.getQuery().getRepeatedRelationCount().clone();
				
				if(noOfOutputTuples.containsKey(jnClause.getJoinTable2())
						&& noOfOutputTuples.get(jnClause.getJoinTable2())<noOfTuples){
					
					noOfOutputTuples.put(jnClause.getJoinTable2().getTableName(), noOfTuples);
				}
				
				if(!noOfOutputTuples.containsKey(jnClause.getJoinTable2())){
					noOfOutputTuples.put(jnClause.getJoinTable2().getTableName(), noOfTuples);
				}
				else{		
					noOfOutputTuples = (HashMap<String,Integer>)cvc.getQuery().getRepeatedRelationCount().clone();
				}
				repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
				
				itr = cvc.getRepeatedRelationCount().keySet().iterator();
				while(itr.hasNext()){
					tableName = itr.next();
					int c =  cvc.getRepeatedRelationCount().get(tableName);
					for(int i1=1;i1<=c;i1++){
						Integer[] tuplePos = new Integer[32];
						tuplePos[1] = i1;//Meaning first tuple is at pos i
						repeatedRelNextTuplePos.put(tableName+i1, tuplePos);
						cvc.getNoOfTuples().put(tableName+i1, 1);
						cvc.getCurrentIndexCount().put(tableName+i1, i);
					}
				}
				
				n2.setType(Node.getColRefType());
				
				Vector<Node> list = new Vector<Node>();
				Vector<Node> addConditionList = new Vector<Node>();
			
				Node eqNode = new Node();
				eqNode.setRight(n1);
				eqNode.setLeft(n2);
				eqNode.setOperator("=");
				eqNode.setType(Node.getBroNodeType());
				//list.add(eqNode);
				list.add(n1);
				list.add(n2);
				addConditionList.add(eqNode);
				
				equivalenceClasses.add(list);
				addConditions.add(addConditionList);
			}
			
			}
		}
		
	}
	//Add the equivalence classes to the conjuncts also
	 Conjunct.createConjunct(cvc.getqParser(),addConditions);
     for(int k = 0 ; k < cvc.getqParser().getConjuncts().size();k++){
		 if(! qbt.getConjuncts().containsAll(cvc.getqParser().getConjuncts())){
			 qbt.setConjuncts(new ArrayList<Conjunct>(cvc.getqParser().getConjuncts()));
		 } 
     }
	
	//END -  GET FOREIGN KEY REFERENCES and if exists add those attributes to Join Conditions
	return equivalenceClasses;
	 
}
	/**
	 * This method clones the original conjunct values that are used for restoring the conjuncts later
	 * after data generation for this part is done and returns Vector of Conjuncts
	 * 
	 * @param existingConjuncts
	 * @return
	 */
	public static Vector cloneConjuncts(Vector<Conjunct> existingConjuncts){
		
		Vector<Conjunct> newConjunct = new Vector<Conjunct>();
		for(int i = 0;i< existingConjuncts.size(); i++){
			
			Conjunct newCon = new Conjunct(existingConjuncts.get(i).getAllConds());
			newCon.setAllConds((Vector<Node>)existingConjuncts.get(i).getAllConds().clone());
			newCon.setAllSubQueryConds((Vector<Node>)existingConjuncts.get(i).getAllSubQueryConds().clone());
			newCon.setEquivalenceClasses((Vector<Vector<Node>>)existingConjuncts.get(i).getEquivalenceClasses().clone());
			newCon.setIsNullConds((Vector<Node>)existingConjuncts.get(i).getIsNullConds().clone());
			newCon.setJoinConds((Vector<Node>)existingConjuncts.get(i).getJoinConds().clone());
			newCon.setLikeConds((Vector<Node>)existingConjuncts.get(i).getLikeConds().clone());
			newCon.setSelectionConds((Vector<Node>)existingConjuncts.get(i).getSelectionConds().clone());
			newCon.setStringSelectionConds((Vector<Node>)existingConjuncts.get(i).getStringSelectionConds().clone());
		
		
			newConjunct.add(newCon); 
			
		}
		return newConjunct;

	}
	 
	/**
	 * This method clones the original conjunct values that are used for restoring the conjuncts later
	 * after data generation for this part is done and returns ArrayList of Conjuncts
	 * 
	 * @param existingConjuncts
	 * @return
	 */
	public static ArrayList cloneListOfConjuncts(ArrayList<Conjunct> existingConjuncts){
		
		ArrayList<Conjunct> newConjunct = new ArrayList<Conjunct>();
		for(int i = 0;i< existingConjuncts.size(); i++){
			
			Conjunct newCon = new Conjunct(existingConjuncts.get(i).getAllConds());
			newCon.setAllConds((Vector<Node>)existingConjuncts.get(i).getAllConds().clone());
			newCon.setAllSubQueryConds((Vector<Node>)existingConjuncts.get(i).getAllSubQueryConds().clone());
			newCon.setEquivalenceClasses((Vector<Vector<Node>>)existingConjuncts.get(i).getEquivalenceClasses().clone());
			newCon.setIsNullConds((Vector<Node>)existingConjuncts.get(i).getIsNullConds().clone());
			newCon.setJoinConds((Vector<Node>)existingConjuncts.get(i).getJoinConds().clone());
			newCon.setLikeConds((Vector<Node>)existingConjuncts.get(i).getLikeConds().clone());
			newCon.setSelectionConds((Vector<Node>)existingConjuncts.get(i).getSelectionConds().clone());
			newCon.setStringSelectionConds((Vector<Node>)existingConjuncts.get(i).getStringSelectionConds().clone());
			
			newConjunct.add(newCon); 
			
		}
		return newConjunct;

	}
	
	/***AFTER GENERATING COMMON CONSTRAINTS AND REVERTING EQUIVALENCE CLASSES CODE
	 * 	/*	for(int i=0; i<allConds.size(); i++){
		
						Node pred = allConds.get(i);
		
						 logger.log(Level.INFO, "\n----------------------------------");
						logger.log(Level.INFO, "KILLING NON EQUI JOIN PREDICATE: " + pred);
						logger.log(Level.INFO,"\n----------------------------------\n");
						
						
						// Find the different relations involved in pred. Pred might be an arbitrary predicate 
						HashMap<String,Table> rels = UtilsRelatedToNode.getListOfRelationsFromNode(cvc, qbt, pred);
		
						Iterator rel = rels.keySet().iterator();
						while(rel.hasNext()){
		
							String CVCStr="";
							cvc.setConstraints( new ArrayList<String>());
							cvc.setStringConstraints( new ArrayList<String>());
							cvc.setTypeOfMutation("");
							cvc.setCVCStr("");
							
							//set the type of mutation we are trying to kill
							cvc.setTypeOfMutation( TagDatasets.MutationType.NONEQUIJOIN, TagDatasets.QueryBlock.OUTER_BLOCK );
							
							// Assign the number of tuples and their positions 
							//cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
							//cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
							//cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
		
							// Add constraints related to parameters
							cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt));
		
							// Add constraints for all the From clause nested subquery blocks
							for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
								cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");
		
								cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
		
								cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");
							}
		
							String aliasName = (String)rel.next();
							String tableName = rels.get(aliasName).getTableName();
							
							// FIXME: This function is generating constraints of form ASSERT NOT EXISTS (i: O_SECTION_INDEX_INT): ((O_SECTION[1].0>O_TAKES[1].1));
							//These are causing problem. Example query19
							//FIXME: Also the repeated relations are not correctly handled in the below method 
							cvc.getConstraints().add( GenerateCVCConstraintForNode.genNegativeCondsForPredAgg(cvc, qbt, pred, aliasName, tableName) );
							
							// get positive constraints for all conditions except all conditions of the conjunct 
							cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuctExceptNonEquiJoins(cvc, qbt, conjunct) );
							
							// Add negative conditions for all other conjuncts of this query block
							for(Conjunct inner: qbt.getConjuncts())
								if(inner != conjunct)
									cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );
							
							// get group by constraints 
							cvc.getConstraints().add("\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
							cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
							
							
							// Generate havingClause constraints 
							cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
							for(int l=0; l< qbt.getNoOfGroups(); l++)
								for(int k=0; k < qbt.getAggConstraints().size();k++){
									cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
								}
							cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK\n%---------------------------------\n");
							
							
													
							// add other constraints of outer query block 
							cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );
							
							cvc.setCVCStr(CVCStr);
							
							// Call the method for the data generation
							GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
							
							
						}
					}
				}*/
	
	
	/**AFTER *********************************************************************************************/
	//INSERT AFTER THIS COMMENT LINE
	
	//Get outer query block of this query 
	/*QueryBlockDetails qbt = cvc.getOuterBlock();

	//ADD JOIN CONDITION IF REFERENCE ATTRIBUTES are there in projected cols or selection condition or having clause
	
	cvc.inititalizeForDataset();
	
	//get the tuple assignment for this query
	 //If no possible assignment then not possible to kill this mutation
	if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
		return ;
	
	//we have to kill the mutations in each conjunct
	for( Conjunct con: qbt.getConjuncts()){
		//add the negative constraints for all the other conjuncts of this query block 
		for(Conjunct inner: qbt.getConjuncts())
			if(inner != con)
				cvc.getConstraints().add( GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, qbt, inner) );	

		//add positive constraints for all the conditions of this conjunct
		cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qbt, con) );			
	}
	//for(){
		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"NEW CONJUNCT IN KILLING MISSING JOIN CLAUSE MUTATIONS: \n");
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		// Initialize the data structures for generating the data to kill this mutation 
		cvc.setConstraints( new ArrayList<String>());
		cvc.setStringConstraints( new ArrayList<String>());
		cvc.setTypeOfMutation("");
		
		//set the type of mutation we are trying to kill
		cvc.setTypeOfMutation( TagDatasets.MutationType.MISSINGJOINS, TagDatasets.QueryBlock.OUTER_BLOCK );
		
		// Add constraints related to parameters
		cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters( cvc, qbt ));
		
		cvc.setResultsetTableColumns1( new HashMap<Table,Vector<String>>() );
		
		cvc.setCVCStr("");

		
		// get the constraints to kill this mutation
		String constraintString = GenerateConstraintsForMissingJoinMutation.generateConstraintsForMissingJoin(cvc,qbt);


				if( constraintString == "")//means there are no attributes referring to other tables
					//continue ;
					return;
				
				cvc.getConstraints().add("\n%---------------------------------\n%CONSTRAINTS TO KILL MISSING JOINS IN OUTER QUERY BLOCK\n%---------------------------------\n");
				cvc.getConstraints().add( constraintString );
				cvc.getConstraints().add("\n%---------------------------------\n%END OF CONSTRAINTS TO KILL MISSING JOINS IN OUTER QUERY BLOCK\n%---------------------------------\n");
				
				
				// get the constraints for each from clause nested sub query block
				for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){
					
					cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY MISSING JOIN MUTATION\n%---------------------------------\n");				
					cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );				
					cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY MISSING JOIN MUTATION\n%---------------------------------\n");
			}
				
				
				
				// get group by constraints 
				cvc.getConstraints().add("\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK MISSING JOIN MUTATION\n%---------------------------------\n");
				cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints( cvc, qbt) );
				
				
				// Generate havingClause constraints 
				cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK MISSING JOIN MUTATION\n%---------------------------------\n");
				for(int l=0; l< qbt.getNoOfGroups(); l++)
					for(int k=0; k < qbt.getAggConstraints().size();k++){
						cvc.getConstraints().add(GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qbt, qbt.getAggConstraints().get(k), qbt.getFinalCount(), l) );
					}
				cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS FOR OUTER QUERY BLOCK MISSING JOIN MUTATION\n%---------------------------------\n");
				
				
										
				// add other constraints of outer query block 
				cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, cvc.getOuterBlock()) );
				
				//Call the method for the data generation
				GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc);
				
			//}
			// Revert back to the old assignment 
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
	//		cvc.setNoOfOutputTuples( (HashMap<String, Integer>) noOfOutputTuplesOrig.clone() );
			cvc.setRepeatedRelationCount((HashMap<String, Integer>)repeatedRelationCountOrig.clone());
			cvc.setRepeatedRelNextTuplePos( (HashMap<String, Integer[]>)repeatedRelNextTuplePosOrig.clone() );
			cvc.setTableNames( (HashMap<String, Integer[]>)tableNameOrig.clone());
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			throw e;
		}
	}*/
	
	
	/*
	 //Get outer query block of this query 
QueryBlockDetails qbt = cvc.getOuterBlock();
//cvc.getBaseRelation().containsKey(cvc.getqParser().getProjectedCols().get(0));
//If cvc base relation contains relation used in query and the foreign key attribute is used in projected cols or selection condition 
//then add those relations also to the proj cols. and the foreign key condition to the selection condition 
//and generate dataset
for(int i=0;i<cvc.getTableMap().foreignKeyGraph.topSort().size();i++){
	String tableName = cvc.getTableMap().foreignKeyGraph.topSort().get(i).toString();
	for(int j=0;j < cvc.getqParser().getProjectedCols().size(); j++){
		if(tableName.equalsIgnoreCase(cvc.getqParser().getProjectedCols().get(j).getTable().getTableName())){
			//If projected cols has referenced relation attribute, then add the foreign key condition to selection condition
			
			
			
		}
	}
}
	  */
	 
}
