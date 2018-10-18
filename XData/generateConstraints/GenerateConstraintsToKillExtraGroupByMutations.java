package generateConstraints;

import java.util.ArrayList;
import java.util.Map;
import parsing.Column;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;

/**
 * This class generates constraints to kill the extra group by mutations
 * @author mahesh
 *
 */
public class GenerateConstraintsToKillExtraGroupByMutations {

	/** Generates constraints to kill extra group by mutations in this query block */
	public static String getExtraGroupByConstraints(GenerateCVC1 cvc,	QueryBlockDetails queryBlock, ArrayList<Column> extraColumn, Map<String, String> tableOccurrence) throws Exception{

		/** Used to store the constraint*/
		String extraGroupBy = "";
		
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		/**If there are no group  by attributes, then nothing need to be done*/
		if(queryBlock.getGroupByNodes() == null || queryBlock.getGroupByNodes().size() == 0)
			return extraGroupBy;



		/**FIXME: Will these extra columns affect tuple assignment method 
		 * If yes, then this method should be called before tuple assignment method in mutation killing methods OR
		 * Call tuple assignment method from here and then get constraints for all other conditions*/

		/**Generate constraint for each extra column */
		for(Column col: extraColumn){

			/**Get column and table details */
			String t1 = col.getTableName();
			String tableNameNo = tableOccurrence.get(t1);
			int Index = cvc.getTableMap().getTable(t1).getColumnIndex(col.getColumnName());
			
			int offset = 0; 
			/*if(cvc.getRepeatedRelNextTuplePos().containsKey(tableNameNo) ){*/
					offset=cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
			/*}
			else{
				offset=1;
			}*/
			/**Generate constraints in each group of this query block*/
			for(int m=1; m <= queryBlock.getNoOfGroups() ; m++){

				//extraGroupBy += "ASSERT ";

				/**get no of tuples */
				int count = 0;//cvc.getNoOfTuples().get(tableNameNo);
				//if(cvc.getNoOfTuples().containsKey((tableNameNo))){
					count = cvc.getNoOfTuples().get(tableNameNo);
				/*}else if(cvc.getNoOfOutputTuples().containsKey(t1)){
					count = 1;
				}*/

				/** get group number*/
				int group = (m-1)*count;

				/**If there is a single tuple */
				if(count == 1){
					extraGroupBy +=constrGen.getAssertTrue();
					continue;
				}
				ConstraintObject constrObj = new ConstraintObject();
				/**To kill this mutation
				 * This column has to be distinct in at least two tuples*/
				for(int k=1; k<=count;k++){
					for(int l=k+1; l<=count;l++)
						//extraGroupBy += 
						constrObj.setLeftConstraint(constrGen.getDistinctConstraint(t1, col, (group+k-1+offset), Index, t1, col, (group+l-1+offset), Index));
						constrList.add(constrObj);
					//" DISTINCT (O_"+t1+"["+(group+k-1+offset)+"]."+Index+ " , O_"+t1+"["+(group+l-1+offset)+"]."+Index+") OR ";
				}
				
				extraGroupBy += constrGen.generateOrConstraintsWithAssert(constrList) +"\n";
				//int lastIndex = extraGroupBy.lastIndexOf("OR");
				//extraGroupBy = extraGroupBy.substring(0, lastIndex-1) + " ;\n ";
			}
		}

		return extraGroupBy;
	}


	
	
	//public static ArrayList<Column> getExtraColumns(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Map<String, String> tableOccurrence) throws Exception{
public static ArrayList<Column> getExtraColumns(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Map<String, String> tableOccurrence) throws Exception{
		/** get the list of tables which contain group by nodes of this query block */
		/** Along with the base table names, get their occurrences */
		/** Store base table names */
		ArrayList<Table> tempFromTables = new ArrayList<Table>();

		/**Stores relations occurrences*/
		//tempFromTables = getListOfRelations( cvc,queryBlock.getGroupByNodes(), tableOccurrence );
 tempFromTables = getListOfRelations(cvc, queryBlock.getGroupByNodes(), tableOccurrence );

		/** get all the extra columns of these tables*/
		ArrayList<Column> extraColumn = new ArrayList<Column>();
		//extraColumn = getListOfExtraColumns(cvc,tempFromTables, queryBlock.getGroupByNodes());
extraColumn = getListOfExtraColumns(tempFromTables, queryBlock.getGroupByNodes());
		return extraColumn;
	}

	/**
	 * This function is used to get the list of extra columns apart from the group by attributes
	 * @param tempFromTables
	 * @param groupbyNodes
	 * @return
	 */
	//public static ArrayList<Column> getListOfExtraColumns(GenerateCVC1 cvc,ArrayList<Table> tempFromTables, ArrayList<Node> groupbyNodes) throws Exception {
public static ArrayList<Column> getListOfExtraColumns(	ArrayList<Table> tempFromTables, ArrayList<Node> groupbyNodes) throws Exception {

		/** Store the list of columns*/
		ArrayList<Column> extraColumn = new ArrayList<Column>();

		/** For each table */
		for(Table table: tempFromTables){
			for(int j=0;j<table.getNoOfColumn();j++){/**For each column of this table*/

				/**Get this column */
				Column col = table.getColumn(j);

				/**Indicates if this is a group by node */
				boolean flag=true;

				/** check if this column is a group by node */
				for(Node each: groupbyNodes){
					if(each.getColumn().getColumnName().equalsIgnoreCase(col.getColumnName())){
						flag=false;
						break;
					}
				}
			if(flag)/** If this not a group by node column */
					extraColumn.add(col);
			//}
		}
		/** Added by Shree for adding reference columns to missing group by dataset generation **/
		/**Check if the projected tables refers to any other table. If so add the referenced columns also
		 * in missing group By **/
			
			/*for(Node each: groupbyNodes){
				Column colName = each.getColumn();
				Table originalTable = colName.getTable();
				if(originalTable.hasForeignKey()){
					for (String fKeyName : originalTable.getForeignKeys().keySet()) {
						
						ForeignKey fKey = originalTable.getForeignKey(fKeyName);
						Vector<Column> fKeyColumns = fKey.getFKeyColumns();
						for(Column fk : fKeyColumns ){
							if(!(each.getColumn().getColumnName().equalsIgnoreCase(fk.getColumnName()))
									&& !(extraColumn.containsAll(fKey.getReferenceKeyColumns()))){
								extraColumn.addAll(fKey.getReferenceKeyColumns());
								break;
							}
						}
						
						
					}
				}
			}*/
		/*Add the tables referred by this table also in the list - referenced tables and the referenced columns*/
		/*Table thisTable = cvc.getQuery().getFromTables().get(table);
		if(thisTable.hasForeignKey()){
			//If this table has foreign key, find the referenced table and add the referencing columns to extra Column
			//cvc.getRepeatedRelationCount().keySet()
			String tableName = "";//cvc.getTableMap().foreignKeyGraph.topSort().get(i).toString();
			if(table.getForeignKeys().containsKey(thisTable)){
				//thisTable.getForeignKey(keyName)
				//cvc.getTableMap().
			}
			
		}
}

		Graph<Table, JoinClauseInfo> joinGraph = cvc.getQuery().getJoinGraph();
		// Graph<Table,ForeignKey> foreignKeyGraph =
		// tableMap.getForeignKeyGraph();
		for (String tableName : cvc.getQuery().getFromTables().keySet()) {
			
			Table table = cvc.getQuery().getFromTables().get(tableName);
			if (table.hasForeignKey()) {
				
				for (String fKeyName : table.getForeignKeys().keySet()) {
					
					ForeignKey fKey = table.getForeignKey(fKeyName);
					Vector<Column> fKeyColumns = fKey.getFKeyColumns();
					// Vector<Column> refKeyColumns =
					// fKey.getReferenceKeyColumns();
					boolean joinFound = false;
					if (joinGraph.getNeighbours(table) != null) {
						for (Table joinTable : joinGraph.getNeighbours(table)
								.keySet()) {
							
							//Just using Existing class itself to extract foreign key relations 
						
							for (JoinClauseInfo joinClauseInfo : joinGraph
									.getEdges(table, joinTable)) {
								if (joinClauseInfo.contains(fKey
										.getFKTablename())
										&& joinClauseInfo.contains(fKey
												.getReferenceTable()
												.getTableName())) {
								
									//joinFound = true;
									extraColumn.addAll(fKey.getReferenceKeyColumns());
								
								}
							}
						}
					}
				}
			}
		}*/
		
		}	
		return extraColumn;
	}

	/**
	 * This function returns the list of relations of group by nodes
	 * @param groupbyNodes
	 * @param tableOccurrence 
	 * @return
	 */
	public static ArrayList<Table> getListOfRelations(GenerateCVC1 cvc, ArrayList<Node> groupbyNodes, Map<String, String> tableOccurrence) throws Exception{

		ArrayList<Table> tempFromTables = new ArrayList<Table>();
		//String tableName = "";
		/**Get for each group by node */
		for(Node tempgroupByNodeNew : groupbyNodes){
			/**If this table is not already in the list */
			if(!tempFromTables.contains(tempgroupByNodeNew.getColumn().getTable()))		{		
				/** Add this table */
				tempFromTables.add(tempgroupByNodeNew.getColumn().getTable());
				}
			
			/*if(tempgroupByNodeNew.getColumn().getTable().hasForeignKey()){
				Table originalTable= tempgroupByNodeNew.getColumn().getTable();
			
				for (String fKeyName : originalTable.getForeignKeys().keySet()) {
					
					ForeignKey fKey = originalTable.getForeignKey(fKeyName);
					
					Vector<Column> fKeyColumns = fKey.getFKeyColumns();
					for(Column fk : fKeyColumns ){
						if(!(tempgroupByNodeNew.getColumn().getColumnName().equalsIgnoreCase(fk.getColumnName()))
								&& !(tableOccurrence.containsKey(fKey.getReferenceTable()))){
							tableOccurrence.put(fKey.getReferenceKeyColumns().get(0).getTable().getTableName(),fKey.getReferenceKeyColumns().get(0).getTable().getTableName()+"1");
							break;
						}
					}
			}
			}*/
			/**If this table occurrence is not already in the list */
			if( !tableOccurrence.containsValue(tempgroupByNodeNew.getType()))
				/**Add the occurrence of this relation*/
				tableOccurrence.put(tempgroupByNodeNew.getColumn().getTable().getTableName(), tempgroupByNodeNew.getTableNameNo());
		}

		return tempFromTables;
	}


}
