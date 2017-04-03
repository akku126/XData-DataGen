package generateSMTConstraints;

import generateConstraints.GenerateCVCConstraintForNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

public class AddDataBaseConstraintsSMT {

	private static Logger logger=Logger.getLogger(AddDataBaseConstraintsSMT.class.getName());
	
	
	/**
	 * Generates constraints specific to the database
	 * @param cvc
	 * @return
	 * @throws Exception
	 */
	public static String addDBConstraintsForSMT(GenerateCVC1 cvc) throws Exception{
		
		String dbConstraints = "";		

		
		/** The primary keys have to be distinct across all the tuples needed to satisfy constrained aggregation
		 * If there is no constrained aggregation, then primary key values can be same or distinct
		 * But if there is constrained aggregation then primary key has to be distinct across all tuples 
		 * Otherwise if solver chooses same value then the input tuples may not satisfy constrained aggregation
		 * These constraints must be added before foreign key constraints and this need not be done for the extra tuples added to satisfy constrained aggregation 
		 * These constraints must be added to only that occurrence of the table
		 * FIXME: Killing partial group by case 2 is a special case here
		 * FIXME: We should consider repeated relation occurrences here*/
		String unConstraints = "\n;---------------------------------\n;UNIQUE CONSTRAINTS  FOR PRIMARY KEY TO SATISFY CONSTRAINED AGGREGATION\n;---------------------------------\n";

		
		
		try{
		/** Add constraints for outer query block, if there is constrained aggregation */
			if(cvc.getOuterBlock().isConstrainedAggregation())
				unConstraints += getUniqueConstraintsForPrimaryKeys(cvc, cvc.getOuterBlock());
	
			/** Add constraints for each from clause nested sub query block, if there is constrained aggregation */
			for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getFromClauseSubQueries())
				if(queryBlock.isConstrainedAggregation())/** if there is constrained aggregation */
					unConstraints += getUniqueConstraintsForPrimaryKeys(cvc, queryBlock);	
	
			/** Add constraints for each where clause nested sub query block, if there is constrained aggregation */
			for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getWhereClauseSubQueries())
				if(queryBlock.isConstrainedAggregation())/** if there is constrained aggregation */
					unConstraints += getUniqueConstraintsForPrimaryKeys(cvc, queryBlock);	
	
			unConstraints += "\n;---------------------------------\n;END OF UNIQUE CONSTRAINTS  FOR PRIMARY KEY TO SATISFY CONSTRAINED AGGREGATION\n;---------------------------------\n";
	
			/**Generate foreign key constraints */
			dbConstraints += "\n;---------------------------------\n;FOREIGN  KEY CONSTRAINTS \n;---------------------------------\n";
			dbConstraints += generateConstraintsForForeignKeys(cvc);
			dbConstraints += "\n;---------------------------------\n;END OF FOREIGN  KEY CONSTRAINTS \n;---------------------------------\n";
	
			/** Now add primary key constraints */
			dbConstraints += "\n;---------------------------------\n;PRIMARY KEY CONSTRAINTS \n;---------------------------------\n";
			dbConstraints += generateConstraintsForPrimaryKeys(cvc);
			dbConstraints += "\n;---------------------------------\n;END OF PRIMARY KEY CONSTRAINTS \n;---------------------------------\n";
	
			//dbConstraints += "\n;---------------------------------\n;CONSTRAINTS FOR TUPLE INDICES \n;---------------------------------\n";
			//dbConstraints += generateConstraintsForTupleIndices(cvc);
			//dbConstraints += "\n;---------------------------------\n;END OF CONSTRAINTS FOR TUPLE INDICES \n;---------------------------------\n";
		}catch(Exception e){
			logger.log(Level.SEVERE,"\n Exception in AddDatabaseConstraints.java:Function addDBConstraints :",e);
			throw e;
		}
		
		return dbConstraints + unConstraints;
		
	}

	

	/**
	 * This method generates constraints for the primary keys of the tables used in the query
	 * @param cvc
	 * @return
	 */
	public static String generateConstraintsForPrimaryKeys(GenerateCVC1 cvc) throws Exception{
		
		String pkConstraint = "";
		try{
			/** For each table in the result tables */
			for(int i=0; i < cvc.getResultsetTables().size(); i++){
	
				/** Get this data base table */
				Table table = cvc.getResultsetTables().get(i);
	
				/**Get table name */
				String tableName = table.getTableName();
	
				/**Get the primary keys of this table*/
				ArrayList<Column> primaryKeys = new ArrayList<Column>( table.getPrimaryKey() );
	
				/**If there are no primary keys, then nothing need to be done */
				if( primaryKeys.size() <= 0)
					continue;
	
				/**If there are no tuples for this query */			
				if( cvc.getNoOfOutputTuples().get(tableName)==null && cvc.getTablesOfOriginalQuery().contains(table))
					cvc.getNoOfOutputTuples().put(tableName, 1);
	
				else if ( cvc.getNoOfOutputTuples().get(tableName)==null)				
					cvc.getNoOfOutputTuples().put(tableName, 0);
	
				/**Get the number of tuples for this relation  */
				int noOfTuples = cvc.getNoOfOutputTuples().get(tableName);
	
				/**If there is a single tuple then nothing need to be done */
				if(noOfTuples == 1)
					continue ;
	
				/** The constraint says "If the primary key attribute is same across two tuples, then all the other attributes have to be same */
				/**Generate this constraint */
				for(int k=1; k<=noOfTuples; k++){
					for(int j=k+1; j<=noOfTuples; j++){
						
					
						pkConstraint +="\n (assert ";
						String tempConstString1 = "";
						//**Generate the constraint for each primary key attribute 					
						for(int p=0; p<primaryKeys.size();p++){
							tempConstString1 = "";
							String tempConstString2 = "";
							//** Get column details 
							Column pkeyColumn = primaryKeys.get(p);				
							int pos = table.getColumnIndex(pkeyColumn.getColumnName());
							
							tempConstString2 += getSMTAndConstraint(pkeyColumn,pkeyColumn ,Integer.valueOf(k), Integer.valueOf(j), tempConstString1,Integer.valueOf(pos),pos);
								tempConstString1 = tempConstString2;
						}
						
						pkConstraint += "(=> "+tempConstString1;
						
						boolean x = false;
						
						if(table.getColumns().size() > 1){
							pkConstraint += "(and ";
						}
						
						for(String col : table.getColumns().keySet()){
							
							int indx = 0;
							String tString1 = "";
							String tString2 = "";
							if(!( primaryKeys.toString().contains(col))){
								x = true;
								int pos = table.getColumnIndex(col);
								
								if(indx == 0 ){
									tString1 += " (= ("+col+pos+" (select O_"+ table.getTableName()+" " + k +")) " +
											"("+col+pos+" (select O_"+table.getTableName()+" "+ j +") )"+" )";									
								}
								else if(indx > 0){
									tString2 += getSMTAndConstraint(table.getColumns().get(col),table.getColumns().get(col) ,Integer.valueOf(k), Integer.valueOf(j), tString1,pos,pos);
									tString1 = tString2;
								}
								
								indx ++;
								pkConstraint += tString1;
							}
						}
						if(x == false){
							pkConstraint += "true\n";	
						}
						pkConstraint +=")";
						pkConstraint += ") )";
						
					}
				}
	
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"\n Exception in AddDatabaseConstraints.java: Function generateConstraintsForPrimaryKeys : ",e);
			throw e;
		}
		return pkConstraint;
	}


	/**
	 * Get CVC constraints for foreign keys
	 * @param foreignKey
	 * @param fkCount
	 * @param fkOffset
	 * @param pkOffset
	 * @return
	 */
	public static String getCVCforForeignKey(ForeignKey foreignKey, int fkCount, int fkOffset, int pkOffset) {


		String fkConstraint = "";

		/** Get foreign key column details */					
		Vector<Column> fCol = (Vector<Column>)foreignKey.getFKeyColumns().clone();

		/** Get primary key column details*/
		Vector<Column> pCol = (Vector<Column>)foreignKey.getReferenceKeyColumns().clone();

		String tempConstString1 = "";
		fkConstraint += "(assert ";
		/** Get the constraints for this foreign key */
		for(int j=1;j <= fkCount; j++){
			tempConstString1 = "";
			String temp1 = "";
			String temp2 = "";
			String tempConstString2 = "";
			
			for (Column fSingleCol : fCol)
			{
				Column pSingleCol = pCol.get(fCol.indexOf(fSingleCol));
							
				if(fSingleCol.getCvcDatatype() != null)
				{
					int pos1 = fSingleCol.getTable().getColumnIndex(fSingleCol.getColumnName());
					int pos2 = pSingleCol.getTable().getColumnIndex(pSingleCol.getColumnName());
					
						tempConstString2 = getSMTAndConstraint(fSingleCol,pSingleCol ,Integer.valueOf(j + fkOffset -1), Integer.valueOf(j + pkOffset - 1), tempConstString1, pos1,pos2);
						tempConstString1 = tempConstString2;
					
				
				//Commented - Nullable Foreign keys - how to handle them in SMT LIB
					if(fSingleCol.isNullable()){
						temp2+= "(and ";
						
						if(fSingleCol.getCvcDatatype().equals("INT")|| fSingleCol.getCvcDatatype().equals("REAL") || fSingleCol.getCvcDatatype().equals("DATE") 
								|| fSingleCol.getCvcDatatype().equals("TIME") || fSingleCol.getCvcDatatype().equals("TIMESTAMP"))
							
							temp2 += "(ISNULL_" + fSingleCol.getColumnName() + "(" + GenerateCVCConstraintForNodeSMT.cvcMapSMT(fSingleCol, j + fkOffset -1 + "") + "))";
						else
							temp2 += "(ISNULL_" + fSingleCol.getCvcDatatype() + "(" + GenerateCVCConstraintForNodeSMT.cvcMapSMT(fSingleCol, j + fkOffset -1 + "") + "))";
						temp2 += ")";
					}
				}
				}
			if(temp2 != null && !temp2.isEmpty()){
				temp1 = "(or "+tempConstString1 + " "+temp2;
				tempConstString1 = temp1;
			}
			}
		
			fkConstraint += tempConstString1;					
			fkConstraint += " ) \n";
		
		return fkConstraint;
	}

	
	
	/**
	 * Generates unique constraints for the primary keys across all the tuples of a relation occurrence in the query block
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static String getUniqueConstraintsForPrimaryKeys (GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception {

		String constraintString = "";
		try{
			/** For each relation that is present in this query block*/
			/** Here we are considering the repeated relation occurrences */
			for(String relation : queryBlock.getBaseRelations()){
	
				/**Get base table name for this relation*/
				String tableName = relation.substring(0, relation.length()-1);/**FIXME: If the relation occurrence >= 10 then problem*/
	
				Table table = cvc.getQuery().getFromTables().get(tableName);
				/**If there is no table */
				if(table == null)
					continue ;
	
				/**Get the primary keys of this table*/
				ArrayList<Column> primaryKeys = new ArrayList<Column>( table.getPrimaryKey() );
	
				/**If there are no primary keys, then nothing need to be done */
				if( primaryKeys.size() <= 0)
					continue;
	
				/**Get the number of tuples for this relation occurrence */
				int noOfTuples;
				if(cvc.getNoOfTuples().get(relation) != null)
					noOfTuples = cvc.getNoOfTuples().get(relation);
				else
					continue;
	
				/**Get the number of groups of this query block*/
				int noOfGroups = queryBlock.getNoOfGroups();
	
				/**Total number of tuples */
				int totalTuples = noOfGroups * noOfTuples;
				
				/**Get the the position from which tuples of this relation starts*/
				int offset = cvc.getRepeatedRelNextTuplePos().get(relation)[1];
		
				cvc.updateTupleRange(tableName, offset, totalTuples + offset - 1);
	
				/**If only single tuple, then nothing need to be done */
				if(totalTuples == 1)
					continue;
			
				/** Get the actual constraints */
				for(int k = 1; k <= totalTuples; k++){
					for(int j = k+1; j <= totalTuples; j++){
	
						constraintString += "\n (assert ";
						
						String tempConstString1 = "";
						
						/** Any of the attribute of the primary key can be distinct across multiple tuples*/
						for(int p = 0; p < primaryKeys.size(); p++){
							
							String tempConstString2 = "";
						//	constraintString += "or (";
							/** Get column details */
							Column pkeyColumn = primaryKeys.get(p);
	
							/**get the column index in the base table*/
							int pos = table.getColumnIndex(pkeyColumn.getColumnName());
							//For first primary key column
							if(p == 0){
								tempConstString1 += " (distinct ("+pkeyColumn.getColumnName()+pos+" (select O_"+tableName+" " + (k + offset - 1) +")) ("+pkeyColumn.getColumnName()+pos+" (select O_"+tableName+" "+ (j + offset - 1) +") )"+" )";
								tempConstString1 += ")";
							}
							else if(p > 0){
								tempConstString2 += "( or ("; 
								tempConstString2 += getSMTOrConstraintForPrimaryKey(pkeyColumn, tableName,Integer.valueOf(k + offset - 1) ,  Integer.valueOf(j + offset - 1) , tempConstString1,pos,pos);
								tempConstString2 += ") )";
								tempConstString1 = tempConstString2;
							}
							}
						constraintString += tempConstString1;					
						constraintString += " ) \n";
					}
				}
			}
	
			return constraintString;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}
	
	

	/**
	 * Generates constraints to satisfy foreign key relationships
	 * @param cvc
	 * @return
	 */
	public static String generateConstraintsForForeignKeys(GenerateCVC1 cvc) throws Exception{

		String fkConstraint = "";/** To store constraints for foreign keys*/
		try{
			/** Get the list of foreign keys*/
			ArrayList<ForeignKey> foreignKeys = cvc.getForeignKeysModified();
	
			/**For each foreign key */
			for(int i=0; i < foreignKeys.size(); i++){
	
				/** Get this foreign key */
				ForeignKey foreignKey = foreignKeys.get(i);
	
				/** Get foreign key table details */
				String fkTableName = foreignKey.getFKTablename();
	
				/**Get the number of tuples of foreign key table*/
				Integer[] fkCount = {0};/**one variable is sufficient, but primitives are immutable*/
	
				/**If FK Table do not contain any tuple, at least one tuple should be there */
				if( cvc.getNoOfOutputTuples().get(fkTableName) == null || cvc.getNoOfOutputTuples().get(fkTableName) == 0) {
	
					fkCount[0] = 1;
	
					/** Update the number of tuples data structure */
					cvc.getNoOfOutputTuples().put(fkTableName,1);				
				}
				else/**Get the number of tuples of FK table */
					fkCount[0] = cvc.getNoOfOutputTuples().get(fkTableName);
	
				/**check if the foreign key table is present across any query block
				 * Also we need to check if all the attributes of the foreign key are involved in joins in that query block
				 * If yes then we should not add the extra tuples in the primary key table  because the join conditions ensure that the foreign key relationship is satisfied 
				 * If no, we should add the extra tuples in the primary key table 
				 * These extra tuples are added for that occurrence of the relation in the query 
				 * In either case we will decrement the foreign key table count as for that many tuples we ensured the primary key relationship*/
	
	
				fkConstraint += generateForeignKeyConstraints(cvc, foreignKey, fkCount);
	
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"\n Exception in AddDatabaseConstraints.java: Function generateConstraintsForForeignKeys : ",e);
			throw e;
		}
		return fkConstraint;
	}

	/**
	 * gets the foreign key constraint for the given foreign key with given foreign key count
	 * @param cvc
	 * @param foreignKey
	 * @param fkCount
	 * @return
	 */
	public static String generateForeignKeyConstraints(GenerateCVC1 cvc, ForeignKey foreignKey, Integer[] fkCount) throws Exception{

		try{
			/**If there are no tuples in the foreign key table*/
			if( fkCount[0] <= 0)
				return "";	
			
			/**stores the constraint*/
			String fkConstraint = "";
			/** Get foreign key table details */
			String fkTableName = foreignKey.getFKTablename();		
	
			/**get the list of  equi join conditions on this foreign key table*/
			Vector< Vector< Node > > equiJoins = cvc.getEquiJoins().get(fkTableName);
	
			/**stores whether there are any equi joins conditions are between foreign key and primary key columns*/
			HashMap<String, Boolean> presentinJoin = new HashMap<String, Boolean>();

			String violate = "";
			/**If there are tuples left out in the foreign key table
			 * Get constraints for these extra tuples */
			if( fkCount[0] > 0){
	
				/**get the repeated relations for this foreign key table*/
				int repeatedCount = -1;
				if( cvc.getRepeatedRelationCount().get(fkTableName) != null)
					repeatedCount = cvc.getRepeatedRelationCount().get(fkTableName);
	
	
				/**check for each occurrence of this foreign key table, if*/
				/**joins between foreign key and primary key table are not true then add foreign key constraints for that relation occurrence*/
				for(int i = 1; i <= repeatedCount; i++){
	
					String fkTableNameNo = fkTableName + i;
	
					/**means this foreign key relation occurrence do not have join conditions*/
					if( !presentinJoin.containsKey(fkTableNameNo)){
	
						/**get the total count for this relation occurrence*/
						int count = getTotalNumberOfTuples(cvc, fkTableNameNo);
	
						/**decrement count*/
						fkCount[0] -= count;
	
						/**get the foreign key constraint*/
						fkConstraint += getFkConstraint(cvc, foreignKey, fkTableNameNo, count, 0);
	
						/**get the primary key tuple offset */
						int pkOffset = cvc.getNoOfOutputTuples().get( foreignKey.getReferenceTable().getTableName() ) - fkCount[0];
	
						//violate += getNegativeCondsForExtraTuples(cvc, foreignKey, fkTableNameNo, count, 0, pkOffset);			
					}
				}
	
				/**once done for all relation occurrences in original query, then 
				 * get constraints for the extra tuples (Added due to foreign key relation ship)*/
				/**get the number of tuples for which foreign keys are already added*/
				int fOffset = cvc.getNoOfOutputTuples().get(foreignKey.getFKTablename()) - fkCount[0];
	
				/**get the foreign key constraint*/
				fkConstraint += getFkConstraint(cvc, foreignKey, null, fkCount[0], fOffset);
	
				//violate += getNegativeCondsForExtraTuples(cvc, foreignKey, fkTableNameNo, fkCount[0], 0, pkOffset);	
			}
			
			return fkConstraint + "\n"+ violate;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}

	public static String getFkConstraint(GenerateCVC1 cvc, ForeignKey foreignKey, String fkTableNameNo, int fkCount, int fOffset) {

		/**used to store foreign key occurrence*/
		String fkConstraint = "";

		if(fkCount <= 0)
			return fkConstraint;

		/** Get foreign key table details */
		String ftableName = foreignKey.getFKTablename();					

		/** Get primary key table details*/
		String pkTableName = foreignKey.getReferenceTable().getTableName();		

		/** Get details about the number of extra tuples to be added for primary key table */	
		int pkCount = 0;

		/**To indicate the tuple starting position in the primary key table*/
		int offset = 0;

		/** update the extra tuples to be added for the primary key table*/
		/**if there are no tuple*/
		if( cvc.getNoOfOutputTuples().get(pkTableName) == null || cvc.getNoOfOutputTuples().get(pkTableName) == 0){

			pkCount = fkCount;
			offset = 1;
		}

		else{

			int totalCount = cvc.getNoOfOutputTuples().get(pkTableName);
			offset = totalCount + 1;
			pkCount = fkCount;
		}


		/**updates the number of tuples for primary key and foreign key table*/
		updateTheNumberOfTuples(cvc, pkTableName, fkCount, pkCount);


		/**get the tuple offsets for both primary key table and foreign key table based on the relation occurrences*/
		int fkOffset;

		/**get repeated offset for foreign key table*/
		if(fkTableNameNo != null)
			fkOffset = cvc.getRepeatedRelNextTuplePos().get(fkTableNameNo)[1];
		else
			fkOffset = fOffset + 1;


		fkConstraint = getCVCforForeignKey( foreignKey, fkCount, fkOffset, offset);


		return fkConstraint;
	}	



	/**
	 * Updates the number of tuples of the tables
	 * @param cvc
	 * @param ptableName
	 * @param fkCount
	 * @param pkCount
	 */
	public static void updateTheNumberOfTuples(GenerateCVC1 cvc, String ptableName, int fkCount, int pkCount) {

		/**update the number of tuples for the whole foreign key table relation*//*
		if( cvc.getNoOfOutputTuples().get(ftableName) == null || cvc.getNoOfOutputTuples().get(ftableName) == 0) 
			cvc.getNoOfOutputTuples().put(ftableName, fkCount);
		else
			cvc.getNoOfOutputTuples().put(ftableName, fkCount + cvc.getNoOfOutputTuples().get(ftableName) );*/

		/**update the number of tuples for the whole primary key table relation*/
		if( cvc.getNoOfOutputTuples().get(ptableName) == null || cvc.getNoOfOutputTuples().get(ptableName) == 0) 
			cvc.getNoOfOutputTuples().put(ptableName, pkCount);

		else
			cvc.getNoOfOutputTuples().put(ptableName, pkCount + cvc.getNoOfOutputTuples().get(ptableName) );
	}

	/**
	 * Gets the total number of tuples for this foreign key table occurrence
	 * @param cvc
	 * @param fkTableNameNo
	 * @return
	 */
	public static int getTotalNumberOfTuples(GenerateCVC1 cvc,	String fkTableNameNo) {

		if(fkTableNameNo == null)
			return -1;

		/**get the query block type and query index of in which this foreign key table is present*/
		int queryType = cvc.getTableNames().get(fkTableNameNo)[0];
		int queryIndex = cvc.getTableNames().get(fkTableNameNo)[1];


		/**get the total number of tuples of this relation occurrence in this query block*/
		int totalCount = -1;

		if( queryType == 0) /** means the foreign key table is present in outer block of query*/
			totalCount = cvc.getNoOfTuples().get(fkTableNameNo) * cvc.getOuterBlock().getNoOfGroups();

		else if( queryType == 1)/** the foreign key table is present in from clause nested sub query block*/
			totalCount = cvc.getNoOfTuples().get(fkTableNameNo) * cvc.getOuterBlock().getFromClauseSubQueries().get( queryIndex).getNoOfGroups();

		else if( queryType == 2)/** the foreign key table is present in where clause nested sub query block*/
			totalCount = cvc.getNoOfTuples().get(fkTableNameNo) * cvc.getOuterBlock().getWhereClauseSubQueries().get( queryIndex).getNoOfGroups();

		return totalCount;
	}


	
/**
 * This method will return SMT constraints of the form (or (StringValue) (distinct (colname tableNameNo_colName)(colName tableNameNo_colName)) )
 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
 * 	
 * @param col
 * @param tableName
 * @param index1
 * @param index2
 * @param s1
 * @return
 */
public static String getSMTOrConstraintForPrimaryKey(Column col, String tableName, Integer index1,Integer index2, String s1,Integer pos1, Integer pos2){
		
		String cvcStr ="";
		cvcStr += " (or ";
				
		if(s1 != null){
			cvcStr += s1;
		}
		
		if(col != null && tableName != null){
			cvcStr += "(distinct " + cvcMapSMT(col,tableName,index1,pos1) +" "+cvcMapSMT(col,tableName,index2,pos2)+")";
		}
		cvcStr +=")  ";
		return cvcStr;
	}
/**
 * This method will return SMT constraints of the form (or (StringValue) (distinct (colname tableNameNo_colName)(colName tableNameNo_colName)) )
 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
 * 	
 * @param col
 * @param tableName
 * @param index1
 * @param index2
 * @param s1
 * @return
 */
public static String getSMTAndConstraint(Column fKeyCol, Column pKeyCol, Integer fKeyIndex,Integer pKeyIndex, String s1,Integer pos1,Integer pos2){
	
String cvcStr ="";

		
if(s1 != null && !s1.isEmpty()){
	cvcStr += " (and ";
	cvcStr += s1;
}

if(fKeyCol != null && pKeyCol != null){
	cvcStr += "(=  " + cvcMapSMT(fKeyCol,fKeyCol.getTableName(),fKeyIndex,pos1) +" "+cvcMapSMT(pKeyCol,pKeyCol.getTableName(),pKeyIndex,pos2)+" )";
}
if(s1 != null && !s1.isEmpty()){
	cvcStr +=")  ";
}
return cvcStr;
}


/**
 * Used to get SMT LIB constraint for this column for the given tuple position
 * 
 * @param col
 * @param index
 * @return
 */
public static String cvcMapSMT(Column col, String index){
		Table table = col.getTable();
		String tableName = col.getTableName();
		String columnName = col.getColumnName();
		int pos = table.getColumnIndex(columnName);
		String smtCond = "";
		String colName =columnName;//tableName+"_"+columnName;		
		smtCond = "("+colName+pos+" "+"(select O_"+tableName+" "+index +") )";
		return smtCond;
	}



/**
 * Used to get SMT LIB constraint for this column for the given tuple position
 * @param col
 * @param index
 * @return
 */
public static String cvcMapSMT(Column col, String tableName,Integer index,Integer pos){

		String columnName = col.getColumnName();
		
		String smtCond = "";
		String colName =columnName ;//tableName+"_"+columnName;		
		smtCond = "("+colName+pos+" "+"(select O_"+tableName+" "+index +") )";
		return smtCond;
	}
	


	
}
