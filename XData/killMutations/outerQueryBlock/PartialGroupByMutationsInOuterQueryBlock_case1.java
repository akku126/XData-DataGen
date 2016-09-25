package killMutations.outerQueryBlock;

import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateConstraintsForPartialMultipleGroup;
import generateConstraints.GenerateGroupByConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Conjunct;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;

/**
 * This class Generates data to kill partial group by mutations inside outer block of the query
 * @author mahesh
 *
 */
public class PartialGroupByMutationsInOuterQueryBlock_case1 {

	private static Logger logger = Logger.getLogger(PartialGroupByMutationsInOuterQueryBlock_case1.class.getName());
	/**
	 * Generates data sets for killing partial group by attributes
	 * The data sets generated by this function are only capable of killing the mutations only if the group by attribute is projected in select list 
	 * @param cvc
	 */
	public static void generateDataForkillingParialGroupByMutationsInOuterQueryBlock(GenerateCVC1 cvc) throws Exception{
		
		/** keep a copy of this tuple assignment values */
		HashMap<String, Integer> noOfOutputTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfOutputTuples().clone();
		HashMap<String, Integer> noOfTuplesOrig = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
		HashMap<String, Integer[]> repeatedRelNextTuplePosOrig = (HashMap<String, Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		/**Get outer query block */
		QueryBlockDetails outer = cvc.getOuterBlock();
		ArrayList<Node> groupbyNodesOrig = (ArrayList<Node>)outer.getGroupByNodes().clone();
		int groupCntOriginal = cvc.getOuterBlock().getNoOfGroups();
		
		logger.log(Level.INFO,"\n----------------------------------");
		logger.log(Level.INFO,"GENERATE DATA FOR KILLING PARTIAL GROUP BY MUTATION IN OUTER BLOCK OF QUERY: " + outer);
		logger.log(Level.INFO,"\n----------------------------------\n");
		
		try{ 
			//If foreign key references are there on groupby attributes, handle them as joins with the foreign key reference tables
			//changePartialGroupByQueryForForeignKeys(cvc);
			/**Get group by nodes of this query block*/
			ArrayList<Node> groupbyNodesNew = new ArrayList<Node>();
			groupbyNodesNew.addAll(outer.getGroupByNodes());
			ArrayList<Node> groupbyNodes = (ArrayList<Node>)outer.getGroupByNodes().clone();
			
			//Update group by nodes to include foreign key relation attributes also.
			 
			cvc.getOuterBlock().setGroupByNodes(groupbyNodes);
			
			/**kill each group by attribute at a time*/
			for(Node tempgroupByNode : groupbyNodes){
				
				logger.log(Level.INFO,"\n----------------------------------");
				logger.log(Level.INFO,"KILLING PARTIAL GROUP BY MUTATIONS IN OUTER BLOCK OF QUERY: " + tempgroupByNode);
				logger.log(Level.INFO,"\n----------------------------------\n");
				
				/** Initialize the data structures for generating the data to kill this mutation */
				cvc.inititalizeForDataset();		
				//groupbyNodes = PartialGroupByMutationsInOuterQueryBlock_case1.updateGroupByNodes(cvc,groupbyNodesNew);
				//getRepeatedRelNextTuplePosForPartialGroupBy(cvc,groupbyNodesNew);
				/**set the type of mutation we are trying to kill*/
				cvc.setTypeOfMutation( TagDatasets.MutationType.PARTIALGROUPBY1, TagDatasets.QueryBlock.OUTER_BLOCK );
				
				/** get the tuple assignment for this query
				 * If no possible assignment then not possible to kill this mutation*/
				if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
					continue;			
						
				/**We need to generate two groups for this query block,
				 *  we should update the total number of tuples data structure*/
				//cvc.updateTotalNoOfOutputTuples(cvc.getOuterBlock(), cvc.getOuterBlock().getNoOfGroups());			
				
				/** Add constraints for all the From clause nested sub query blocks */
				for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries()){						
					cvc.getConstraints().add("\n%---------------------------------\n% FROM CLAUSE SUBQUERY\n%---------------------------------\n");
	
					cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc, qb) );
	
					cvc.getConstraints().add("\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY\n%---------------------------------\n");						
				}
				
				/** get constraints for this sub query block except group by clause constraints*/
				/** Add the positive conditions for each conjunct of this query block */
				for(Conjunct conjunct : outer.getConjuncts()){
					cvc.getConstraints().add("\n%---------------------------------\n% CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------\n");
					cvc.getConstraints().add( GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, outer, conjunct) );
					cvc.getConstraints().add("\n%---------------------------------\n% END OF CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------\n");				
				}
				
				/**Add other related constraints for outer query block */
				cvc.getConstraints().add( QueryBlockDetails.getOtherConstraintsForQueryBlock(cvc, outer) );
				
				/** add same group by constraints for this query block */
				cvc.getConstraints().add( "\n%-----------------------------------------------------------------------------------------\n%GROUP BY ATTRIBUTES MUST BE SAME IN SAME GROUP\n%--------------------------------------------------------------\n");				
				cvc.getConstraints().add( GenerateGroupByConstraints.getGroupByConstraints(cvc, groupbyNodes, false, outer.getNoOfGroups()) );
				
				/** Generate havingClause constraints for this sub query block*/
				cvc.getConstraints().add("\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS FOR SUBQUERY BLOCK\n%---------------------------------\n");
				for(int j=0; j< outer.getNoOfGroups();j ++)
					for(int k=0; k < outer.getAggConstraints().size();k++){
						cvc.getConstraints().add( GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, outer, outer.getAggConstraints().get(k), outer.getFinalCount(), j));
					}
				cvc.getConstraints().add("\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS FOR SUBQUERY BLOCK\n%---------------------------------\n");
				GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc, false);
				/** add  constraints to kill this mutation */
				cvc.getConstraints().add( "\n%-----------------------------------------------------------------------------------------\n%CONSTRAINTS TO KILL PARTIAL GROUP BY MUTATIONS WITH MULTIPLE GROUPS\n%--------------------------------------------------------------\n");
				cvc.getConstraints().add( GenerateConstraintsForPartialMultipleGroup.getConstraintsForPartialMultipleGroup(cvc, outer, tempgroupByNode) );
				cvc.getConstraints().add( "\n%-----------------------------------------------------------------------------------------\n%END OF CONSTRAINTS TO KILL PARTIAL GROUP BY MUTATIONS WITH MULTIPLE GROUPS\n%--------------------------------------------------------------\n");
				
				/** Call the method for the data generation*/
				GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc,false);
			} 
			
			/** Revert back to the old assignment */
			cvc.setNoOfTuples( (HashMap<String, Integer>) noOfTuplesOrig.clone() );
			cvc.getOuterBlock().setNoOfGroups(groupCntOriginal);
			cvc.getOuterBlock().setGroupByNodes((ArrayList<Node>)groupbyNodesOrig.clone());
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

	private  static ArrayList<Node> updateGroupByNodes(GenerateCVC1 cvc,ArrayList<Node> groupbyNodes){
		int groups = cvc.getOuterBlock().getNoOfGroups();
		
		try{
			
			//update number of groups also
			
			ArrayList<String> tableNames = new ArrayList<>();
			for(Node each: groupbyNodes){
				
				if(each.getColumn().getTable().hasForeignKey()){
					Table originalTable= each.getColumn().getTable();
					
					if(tableNames != null && tableNames.size() > 0 && ! tableNames.contains(originalTable)){
						//tableNames.add(originalTable.getTableName());
					}else{
						tableNames.add(originalTable.getTableName());
					}
					for (String fKeyName : originalTable.getForeignKeys().keySet()) {
						
						ForeignKey fKey = originalTable.getForeignKey(fKeyName);
						
						Vector<Column> fKeyColumns = fKey.getFKeyColumns();
						for(Column fk : fKeyColumns ){
							if(!(each.getColumn().getColumnName().equalsIgnoreCase(fk.getColumnName()))){
								//&& !(tableOccurrence.containsKey(fKey.getReferenceTable()))){
								//tableOccurrence.put(fKey.getReferenceKeyColumns().get(0).getTable().getTableName(),fKey.getReferenceKeyColumns().get(0).getTable().getTableName()+"1");
								if(tableNames != null && tableNames.size() > 0 && !tableNames.contains(fKey.getReferenceTable().getTableName())){
										tableNames.add(fKey.getReferenceTable().getTableName());
										cvc.getNoOfOutputTuples().put(fKey.getReferenceTable().getTableName(), cvc.getNoOfOutputTuples().get(fKey.getReferenceTable().getTableName()) + 2);
										groups++;
								}
								break;
							}
						}
				}
				}
			}
			//cvc.getOuterBlock().setNoOfGroups(groups);
		
			
			//If the existing group by attributes have foreign key relation, add referenced table's 
			 // columns also in the groupby nodes
			ArrayList<Node> newGrpByNodesAdded = new ArrayList<Node>();
			for(Node each: groupbyNodes){
				parsing.Column colName = each.getColumn();
				Table originalTable = colName.getTable();
				if(originalTable.hasForeignKey()){
					for (String fKeyName : originalTable.getForeignKeys().keySet()) {
						
						ForeignKey fKey = originalTable.getForeignKey(fKeyName);
						Vector<parsing.Column> fKeyColumns = fKey.getFKeyColumns();
						Vector<Node> nd = null;
						for(parsing.Column fk : fKeyColumns ){
							if(!(each.getColumn().getColumnName().equalsIgnoreCase(fk.getColumnName()))
									){
									//&& !(each.getColumn().equals(fKey.getReferenceKeyColumns()))){
								for(parsing.Column c : fKey.getReferenceKeyColumns()){
									
								
									Node n = Node.createNode( c, c.getTable() );
									n.setTableNameNo(c.getTableName()+"1");
									if(!newGrpByNodesAdded.contains(n)){
										newGrpByNodesAdded.add(n);
									}
									 
								}
								break;
							}
						}
						
						
					}
				}
			}
			groupbyNodes.addAll(newGrpByNodesAdded);
			//ends
			}catch(Exception e){
				e.printStackTrace();
			}
		
		HashMap<String, Integer[]> hm = new HashMap<String, Integer[]>();
		for(int i = 0 ; i < groups ; i ++){
			if(groupbyNodes != null && groupbyNodes.size() >= groups){
				//update RepeatedRelationTuple
				hm = getRepeatedRelNextTuplePosForApp(cvc,groupbyNodes.get(i).getTable(),groupbyNodes);
			}
		
		}
		cvc.setRepeatedRelNextTuplePos(hm);
		return groupbyNodes;
		
	}
	
	private static HashMap<String, Integer[]> getRepeatedRelNextTuplePosForApp(GenerateCVC1 cvc, Table referenceTable, ArrayList<Node> groupbyNodes) {
		HashMap<String, Integer[]> hm = new HashMap<String, Integer[]>();
		hm = (HashMap<String,Integer[]>)cvc.getRepeatedRelNextTuplePos().clone();
		
		HashMap<String, Integer[]> temp = cvc.getRepeatedRelNextTuplePos(); 
		Iterator<String> j = temp.keySet().iterator();
			while(j.hasNext()){
				String key = j.next();
				if(!hm.containsKey(referenceTable.getTableName())){
					hm.put(referenceTable.getTableName()+ (hm.size() + 1), hm.get(key));
				}
				else{ 
					hm.put(key, temp.get(key));
				}
			}
		
		return hm;
	}
	 
	private static void getRepeatedRelNextTuplePosForPartialGroupBy(GenerateCVC1 cvc,ArrayList<Node> groupbyNodes){
		HashMap<String, Integer[]> hm = new HashMap<String, Integer[]>();
		int groups = cvc.getOuterBlock().getNoOfGroups();
		
		ArrayList<String> tableNames = new ArrayList<>();
		for(Node each: groupbyNodes){
			
			if(each.getColumn().getTable().hasForeignKey()){
				Table originalTable= each.getColumn().getTable();
				
				if(tableNames != null && tableNames.size() > 0 && ! tableNames.contains(originalTable)){
					//tableNames.add(originalTable.getTableName());
				}else{
					tableNames.add(originalTable.getTableName());
				}
				for (String fKeyName : originalTable.getForeignKeys().keySet()) {
					
					ForeignKey fKey = originalTable.getForeignKey(fKeyName);
					
					Vector<Column> fKeyColumns = fKey.getFKeyColumns();
					for(Column fk : fKeyColumns ){
						if(!(each.getColumn().getColumnName().equalsIgnoreCase(fk.getColumnName()))){
							//&& !(tableOccurrence.containsKey(fKey.getReferenceTable()))){
							//tableOccurrence.put(fKey.getReferenceKeyColumns().get(0).getTable().getTableName(),fKey.getReferenceKeyColumns().get(0).getTable().getTableName()+"1");
							if(tableNames != null && tableNames.size() > 0 && !tableNames.contains(fKey.getReferenceTable().getTableName())){
									tableNames.add(fKey.getReferenceTable().getTableName());
									groups++;
							}
							break;
						}
					}
			}
			}
		}
		cvc.getOuterBlock().setNoOfGroups(groups);
		
		
		for(int i = 0 ; i < groups ; i ++){
			if(groupbyNodes != null && groupbyNodes.size() >= groups){
				//update RepeatedRelationTuple
				hm = getRepeatedRelNextTuplePosForApp(cvc,groupbyNodes.get(i).getTable(),groupbyNodes);
			}
		
		}
		cvc.setRepeatedRelNextTuplePos(hm);
		
	}
	
	private static void changePartialGroupByQueryForForeignKeys(GenerateCVC1 cvc){
		//String query  =  cvc.getQuery().getQueryString();
		
	}
	
}
