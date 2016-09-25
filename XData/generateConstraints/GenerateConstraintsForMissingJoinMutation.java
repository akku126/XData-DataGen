package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import killMutations.outerQueryBlock.JoinMutationsInOuterQueryBlock;

import parsing.Conjunct;
import parsing.Disjunct;
import parsing.JoinClauseInfo;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/** 
 * get the constraints to kill unintended joins in this conjunct of this query block
 */
@Deprecated
public class GenerateConstraintsForMissingJoinMutation {

	private static Logger logger = Logger.getLogger(GenerateConstraintsForMissingJoinMutation.class.getName());
	/**
	 * This method returns the constraints for missing joins in case the projected or selected columns
	 * contains reference relation, adding those relations to the query
	 * 
	 * @param cvc
	 * @param qbt
	 * @param con
	 * @return
	 * @throws Exception
	 */
	public static String  generateConstraintsForMissingJoin(GenerateCVC1 cvc, QueryBlockDetails qbt) throws Exception{
		
		/** Stores the constraint*/
		String constraint = "";

		/**get the list of tables in this query block*/
		HashMap<String, Table> tables = QueryBlockDetails.getListOfTablesInQueryBlock(cvc, qbt);

		/**for each table, get the extra column names which have same name but not involved in equivalence classes*/
		Vector<Vector<Node>> additionalJoins = GenerateConstraintsForMissingJoinMutation.getMissingJoinConditions( cvc, tables, qbt);
		
		/** Frame new equivalence classes and generate equi joins on them **/
		/** get the additional Join node conditions **/
	//	qbt.g.getEquivalenceClasses().addAll(additionalJoins);
		//Vector<Vector<Node>> equivalenceClasses = (con.getEquivalenceClasses());
		 if(cvc.getOuterBlock().getConjuncts() != null 
				 && cvc.getOuterBlock().getConjuncts().size() > 0
				 && cvc.getOuterBlock().getConjuncts().get(0).getEquivalenceClasses() != null){
			 
			 cvc.getOuterBlock().getConjuncts().get(0).getEquivalenceClasses().addAll(additionalJoins);
		 }
		 else {
			 
			 Conjunct.createConjunct(cvc.getqParser(),additionalJoins);
			 qbt.getConjuncts().addAll(new ArrayList<Conjunct>(cvc.getqParser().getConjuncts()) );
		 }
		 
		/** killing join predicate mutations in outer query blocks */
		JoinMutationsInOuterQueryBlock.generateDataForkillingJoinMutationsInOuterQueryBlock(cvc);
		
		for(int k=0; k<additionalJoins.size(); k++){
			Vector<Node> ec = additionalJoins.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				//constraint += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, qbt, n1,n2) +"\n";
				//constraint += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, qbt, n1,n2,"/=") +"\n";
			}
		}		
		return constraint;
		
	}
	
	
	/**
	 * Get the list of columns (in the form of nodes )in the given set of relations with same name but are not involved in equi joins
	 * @param tables
	 * @param qbt
	 * @param conj TODO
	 * @return
	 */
	public static Vector<Vector<Node>> getMissingJoinConditions(GenerateCVC1 cvc,  HashMap<String, Table> tablesList, QueryBlockDetails qbt) {
		
		HashMap<String, Vector<Vector<Node>>> equiJoins= cvc.getEquiJoins();//new Vector<Vector<Node>>();
		Vector<Vector<Node>>  equivalenceClasses = new Vector<Vector<Node>>();
		Vector<Node> newEqClasses = new Vector<Node>(); 
		Vector<Disjunct> disjuncts=new Vector<Disjunct>();
		Node temp;
		HashMap<Integer, Node> hm = new HashMap<Integer, Node>();
		Vector<Node> joinConds = new Vector<Node>();
		// First flatten all the join/selection condition trees
		// And select the join conditions in a special vector joinConds
				
		// Now create equivalence classes from joinConds
		Vector<Node> allJoinConds = new Vector<Node>();
		//START -  GET FOREIGN KEY REFERENCES and if exists add those attributes to Join Conditions
		Vector<Node> projCols = cvc.getqParser().getProjectedCols();
		
			//Add foreign key condition to join condition
			/*for(int m=0;m<cvc.getTableMap().foreignKeyGraph.topSort().size();m++){
				String tableName = cvc.getTableMap().foreignKeyGraph.topSort().get(m).toString();
				
				for(int i=0;i < projCols.size(); i++){
					
					//for(int j= 0; j < cvc.getqParser().getForeignKeyVectorOriginal().size(); j++){
					
						//if(projCols.equals(cvc.getqParser().getForeignKeyVectorOriginal().get(j).getJoinAttribute1())){
						if(projCols.get(i).getColumn().getTableName().equalsIgnoreCase(tableName)){
							//If projected cols has referenced relation attribute, then add the foreign key condition to selection condition
							
							Node joinNode = new Node();
							
							
					}
				}*/
		/**get the list of all table occurrences*/
		ArrayList< String> tableNamesNo = new ArrayList<String>( tablesList.keySet());

		/**for each pair of table occurrences check*/
		for(int i = 0; i < tableNamesNo.size(); i++){
			
			String tableName = tableNamesNo.get(i).substring(0, tableNamesNo.get(i).length()-1);
			for(int m=0;m<cvc.getqParser().getForeignKeyVectorOriginal().size();m++){
				JoinClauseInfo  jnClause = cvc.getqParser().getForeignKeyVectorOriginal().get(m);
				if(jnClause.getJoinTable1().getTableName().equalsIgnoreCase(tableName)){
					//Then foreign key reference exists for the table used in the query
					//Add the relation to equivalence calsses
					
					int getTableNameNo = Integer.parseInt(tableNamesNo.get(i).substring(tableNamesNo.get(i).length()-1, tableNamesNo.get(i).length())); /*To be changed for tablenameno >= 10*/
					String tableNo = jnClause.getJoinTable1().getTableName()+(getTableNameNo+1);
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
					n1.setTableNameNo(tableNamesNo.get(i));
					//n1.setTableNameNo(tableNo);
					n1.setType(Node.getColRefType());
					
					int cnt = 0;
					//cnt = cvc.getNoOfOutputTuples().get(jnClause.getJoinTable1().getTableName());
				//	cvc.getNoOfOutputTuples().put(tableNo,cnt+1);
					//updateRepeatedTuples(cvc,tableNo,1,jnClause.getJoinTable1().getTableName());
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
					n2.setTableNameNo(jnClause.getJoinTable2()+"1");
					cvc.getRepeatedRelationCount().put(jnClause.getJoinTable2().getTableName(), 1);
					Integer [] tuplePos = {1,1};
					cvc.getRepeatedRelNextTuplePos().put(jnClause.getJoinTable2()+"1",tuplePos);
					cvc.getNoOfOutputTuples().put(jnClause.getJoinTable2()+"1",1);
					n2.setType(Node.getColRefType());
					Integer qindex [] = {0,0};
					cvc.getTableNames().put(jnClause.getJoinTable2()+"1",qindex);
				//	qbt.setNoOfGroups(qbt.getNoOfGroups());
					
					Vector<Node> list = new Vector<Node>();
					//list.add(n1);
					//list.add(n2);
					
					Node eqNode = new Node();
					eqNode.setLeft(n1);
					eqNode.setRight(n2);
					eqNode.setOperator("=");
					eqNode.setType(Node.getBroNodeType());
					list.add(eqNode);
					
					equivalenceClasses.add(list);
					
				}
			}
			
		}
		
		
		//END -  GET FOREIGN KEY REFERENCES and if exists add those attributes to Join Conditions
		
		return equivalenceClasses;
		
	}

	
	public static void updateRepeatedTuples(GenerateCVC1 cvc, String tableNameNo,int count, String tableName){
		/** get the table name*/
		//String tableNameNo = relation.getTableNameNo();

		//int count = relation.getCardinality();

		int prevCount ;/**To denote the number of tuples of this relation occurrence previously*/

		if(cvc.getNoOfTuples().get(tableNameNo) == null){/**If the count was not updated previously for this relation occurrence*/
			cvc.getNoOfTuples().put(tableNameNo, count);
			prevCount = 0;
		}
		else{
			prevCount = cvc.getNoOfTuples().get(tableNameNo);				
			cvc.getNoOfTuples().put(tableNameNo,  prevCount + count - 1);
		}

		//String tableName = relation.getTable().getTableName();

		/** update the repeated relation position */
		int thisTablePos=Integer.parseInt(tableNameNo.substring(tableNameNo.length()-1));
		for(int i=thisTablePos+1;i <= cvc.getRepeatedRelationCount().get(tableName);i++){
			Integer[] j = cvc.getRepeatedRelNextTuplePos().get(tableName+(thisTablePos+1));
			j[1] = j[1] + count - 1;
			cvc.getRepeatedRelNextTuplePos().put(tableName+(thisTablePos+1), j);
		}

	}
	
}
