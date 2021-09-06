package generateConstraints;

import java.util.*;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import parsing.Column;
//import parsing.Conjunct;
import parsing.ConjunctQueryStructure;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;

import com.microsoft.z3.*;

/**
 * This class contains the methods for generating constraints related to database such as foreign key constraints and check constraints
 * @author mahesh
 *
 */

public class AddDataBaseConstraints {

	private static Logger logger=Logger.getLogger(AddDataBaseConstraints.class.getName());

	/**
	 * Generates constraints specific to the database
	 * @param cvc
	 * @return
	 * @throws Exception
	 */
	public static String addDBConstraints(GenerateCVC1 cvc) throws Exception{

		String dbConstraints = "";		


		/** The primary keys have to be distinct across all the tuples needed to satisfy constrained aggregation
		 * If there is no constrained aggregation, then primary key values can be same or distinct
		 * But if there is constrained aggregation then primary key has to be distinct across all tuples 
		 * Otherwise if solver chooses same value then the input tuples may not satisfy constrained aggregation
		 * These constraints must be added before foreign key constraints and this need not be done for the extra tuples added to satisfy constrained aggregation 
		 * These constraints must be added to only that occurrence of the table
		 * FIXME: Killing partial group by case 2 is a special case here
		 * FIXME: We should consider repeated relation occurrences here*/
		String unConstraints = ConstraintGenerator.addCommentLine("UNIQUE CONSTRAINTS FOR PRIMARY KEY TO SATISFY CONSTRAINED AGGREGATION");		

		try{
			/** Add constraints for outer query block, if there is constrained aggregation */
			if(cvc.getOuterBlock().isConstrainedAggregation())
				unConstraints += getUniqueConstraintsForPrimaryKeys(cvc, cvc.getOuterBlock());

			/** Add constraints for each from clause nested sub query block, if there is constrained aggregation */
			for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getFromClauseSubQueries())
				if(queryBlock.isConstrainedAggregation())/** if there is constrained aggregation */
					unConstraints += getUniqueConstraintsForPrimaryKeys(cvc, queryBlock);	

			/** Add constraints for each where clause nested sub query block, if there is constrained aggregation */
			for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getWhereClauseSubQueries()) {
				if(queryBlock.isConstrainedAggregation()) {  /** if there is constrained aggregation */
					unConstraints += getUniqueConstraintsForPrimaryKeys(cvc, queryBlock);
				}
			}
			
			unConstraints +=  ConstraintGenerator.addCommentLine("END OF UNIQUE CONSTRAINTS  FOR PRIMARY KEY TO SATISFY CONSTRAINED AGGREGATION");
			
			/**Generate foreign key constraints */
			dbConstraints +=  ConstraintGenerator.addCommentLine("FOREIGN KEY CONSTRAINTS");
			dbConstraints += generateConstraintsForForeignKeys(cvc);
			dbConstraints +=  ConstraintGenerator.addCommentLine("END OF FOREIGN  KEY CONSTRAINTS");
			
			ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		    dbConstraints += ConstraintGenerator.addCommentLine("DOMAIN CONSTRAINTS");
			Vector<Quantifier> domainConstraints = constraintGenerator.getDomainConstraintsforZ3(cvc);
			dbConstraints += domainConstraints.stream().map(
	        		quantifier -> "(assert "+quantifier.toString()+")"
	        		).collect(Collectors.joining("\n\n"));
			dbConstraints += ConstraintGenerator.addCommentLine("END OF DOMAIN CONSTRAINTS");

			dbConstraints += constraintGenerator.generateCVCForNullCheckInHaving();
			
			/** Now add primary key constraints */
			dbConstraints +=  ConstraintGenerator.addCommentLine("PRIMARY KEY CONSTRAINTS");
			dbConstraints += generateConstraintsForPrimaryKeys(cvc);
			dbConstraints +=  ConstraintGenerator.addCommentLine("END OF PRIMARY KEY CONSTRAINTS");

			//dbConstraints += "\n"+cvc.getSolverSpecialCharacter()+"---------------------------------\n"+cvc.getSolverSpecialCharacter()+"CONSTRAINTS FOR TUPLE INDICES \n"+cvc.getSolverSpecialCharacter()+"---------------------------------\n";
			//dbConstraints += generateConstraintsForTupleIndices(cvc);
			//dbConstraints += "\n"+cvc.getSolverSpecialCharacter()+"---------------------------------\n"+cvc.getSolverSpecialCharacter()+"END OF CONSTRAINTS FOR TUPLE INDICES \n"+cvc.getSolverSpecialCharacter()+"---------------------------------\n";
		} catch(Exception e) {
			logger.log(Level.SEVERE,"\n Exception in AddDatabaseConstraints.java:Function addDBConstraints :",e);
			throw e;
		}		
		return dbConstraints + unConstraints;		
	}


	/**
	 * Generates constraints for indices for the number of tuples of each base relation
	 * @param cvc
	 * @return
	 */
	public static String generateConstraintsForTupleIndices(GenerateCVC1 cvc) {

		String constraintString = "";
		try{
			/** For each table in the result tables */
			for(int i=0; i < cvc.getResultsetTables().size(); i++){

				/** Get this data base table */
				Table table = cvc.getResultsetTables().get(i);

				/**Get table name */
				String tableName = table.getTableName();

				/**If there are no tuples for this query */		
				if( cvc.getNoOfOutputTuples().get(tableName) == null)
					continue ;

				/**Get the number of tuples for this relation  */
				int noOfTuples = cvc.getNoOfOutputTuples().get(tableName);

				/**Check for branch queries*/
				HashMap<Table, Integer> tempTuplesAddedForBranchQueries = new HashMap<Table, Integer>();

				tempTuplesAddedForBranchQueries = GenerateConstraintsRelatedToBranchQuery.checkForTuplesAddedForBranchQuery(cvc);


				/**Get the constraint */

				/**if there are branch queries*/
				if(!tempTuplesAddedForBranchQueries.isEmpty() && tempTuplesAddedForBranchQueries.keySet().contains(table))

					constraintString += "O_"+tableName+"_INDEX_INT : TYPE = SUBTYPE (LAMBDA (x: INT) : x > 0 AND x < "+(noOfTuples+1+tempTuplesAddedForBranchQueries.get(table))+");\n";
				else

					constraintString += "O_"+tableName+"_INDEX_INT : TYPE = SUBTYPE (LAMBDA (x: INT) : x > 0 AND x < "+(noOfTuples+1)+");\n";
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"\n Exception in AddDatabaseConstraints.java: Function generateConstraintsForTupleIndices : ",e);
			throw e;
		}
		return constraintString;
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
				ArrayList<Column> primaryKey = new ArrayList<Column>( table.getPrimaryKey() );
				
				ArrayList<Column> primaryKeys = new ArrayList<Column>();
				
				for (Column element : primaryKey) { 
					
		            if (!primaryKeys.contains(element)) { 
		            	primaryKeys.add(element); 
		            	
		            } 
		        } 

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
//				if(cvc.getNoOfOutputTuples().containsKey(tableName.toUpperCase()))
//					noOfTuples = cvc.getNoOfOutputTuples().get(tableName.toUpperCase());
				
//				System.out.println("************************");
//				System.out.println(cvc.getNoOfOutputTuples());
//				System.out.println("************************");
//				
				/**If there is a single tuple then nothing need to be done */
				if(noOfTuples == 1)
					continue ;
				
				ConstraintGenerator constraintGenerator = new ConstraintGenerator();
				ConstraintObject con = new ConstraintObject();
				String impliedCond = "" ;
				ArrayList<ConstraintObject> conList = new ArrayList<ConstraintObject>();
				String 	constraintString = "";

				ArrayList<ConstraintObject> impliedConstraintList = new ArrayList<ConstraintObject>();
				ConstraintObject  impliedConstraint = new ConstraintObject();
				String 	impliedConstraintString = "";

				ConstraintObject primaryKeyConstraint = new ConstraintObject();
				/** The constraint says "If the primary key attribute is same across two tuples, then all the other attributes have to be same */
				/**Generate this constraint */

				if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){

					for(int k=1; k<=noOfTuples; k++){
						for(int j=k+1; j<=noOfTuples; j++){

							pkConstraint += "ASSERT (";

							/**Generate the constraint for each primary key attribute */					
							for(int p=0; p<primaryKeys.size();p++){

								/** Get column details */
								Column pkeyColumn = primaryKeys.get(p);
								int pos = table.getColumnIndex(pkeyColumn.getColumnName());

								/**If this pk attribute is equal*/
								pkConstraint += "O_" + tableName + "[" + k + "]." + pos + " = O_" + tableName + "[" + j +"]." + pos + " AND ";						
							}

							pkConstraint = pkConstraint.substring(0,pkConstraint.length()-4);
							pkConstraint += ") => ";

							boolean x = false;
							for(String col : table.getColumns().keySet()){
								if(!( primaryKeys.toString().contains(col))){
									x = true;
									int pos = table.getColumnIndex(col);

									/**This attribute has to be equal */
									pkConstraint += "(O_"+tableName+"["+k+"]."+pos+" = O_"+tableName+"["+ j +"]."+pos+") AND ";
								}
							}
							if(x == false){
								pkConstraint += "TRUE;\n";	//TODO: Should it imply FALSE???
							}
							else
								pkConstraint = pkConstraint.substring(0,pkConstraint.length()-4)+";\n";
						}


					}
				}
				
				
				else{
					
					
					for(int k=1; k<=noOfTuples; k++){
						for(int j=k+1; j<=noOfTuples; j++){
							
							pkConstraint += "(assert (=> ";
							
							if(primaryKeys.size() > 1)
								pkConstraint += "(and ";

							/**Generate the constraint for each primary key attribute */					
							for(int p=0; p<primaryKeys.size();p++){

								/** Get column details */
								Column pkeyColumn = primaryKeys.get(p);
								IntExpr smtK = (IntExpr) ConstraintGenerator.ctx.mkInt(k);
								IntExpr smtJ = (IntExpr) ConstraintGenerator.ctx.mkInt(j);
								
								/**If this pk attribute is equal*/
								//pkConstraint += "O_" + tableName + "[" + k + "]." + pos + " = O_" + tableName + "[" + j +"]." + pos + " AND ";	
								pkConstraint += "(= ";
								pkConstraint += ConstraintGenerator.smtMap(table.getColumn(pkeyColumn.getColumnName()), smtK).toString();
								pkConstraint += ConstraintGenerator.smtMap(table.getColumn(pkeyColumn.getColumnName()), smtJ).toString() + " )";

							}
							
							if(primaryKeys.size() > 1)
								pkConstraint += " ) ";

							//pkConstraint = pkConstraint.substring(0,pkConstraint.length()-4);
							
							//pkConstraint += "(and ";
                            String temp = "";
							boolean x = false;
							for(String col : table.getColumns().keySet()){
								
								if(!( primaryKeys.toString().contains(col))){
									x = true;
									IntExpr smtK = (IntExpr) ConstraintGenerator.ctx.mkInt(k);
									IntExpr smtJ = (IntExpr) ConstraintGenerator.ctx.mkInt(j);

									/**This attribute has to be equal */
									//pkConstraint += "(O_"+tableName+"["+k+"]."+pos+" = O_"+tableName+"["+ j +"]."+pos+") AND ";
//									pkConstraint += "(= ";
//									pkConstraint += ConstraintGenerator.smtMap(table.getColumn(col),smtK).toString();
//									pkConstraint += ConstraintGenerator.smtMap(table.getColumn(col), smtJ).toString() + " )";
//									
									temp += "(= ";
									temp += ConstraintGenerator.smtMap(table.getColumn(col),smtK).toString();
									temp += ConstraintGenerator.smtMap(table.getColumn(col), smtJ).toString() + " )";
									
								}
							}
							/*if(x == false){
								pkConstraint += "TRUE;\n";	//TODO: Should it imply FALSE???
							}
							else
								pkConstraint = pkConstraint.substring(0,pkConstraint.length()-4)+";\n";
								*/
							if(!temp.isEmpty())
								pkConstraint += "(and " + temp + ")";
							else
								pkConstraint += "(= 1 2)";
							
							//pkConstraint += ") )) \n\n";
							pkConstraint += " )) \n\n";
						}


					}
				
						/*
						int p = 0;
						String temp1 = "";

						Column pkeyColumn = primaryKeys.get(p);
						int pos = table.getColumnIndex(pkeyColumn.getColumnName());
						temp1 = "\n (assert (forall ((ipk"+p+" Int) (jpk"+p+" Int))";
						temp1 += "(=> (= "+ConstraintGenerator.smtMap(pkeyColumn,"ipk"+p)+" "+ConstraintGenerator.smtMap(pkeyColumn,"jpk"+p)+")";

						for(String col : table.getColumns().keySet()){
							if(!( primaryKeys.toString().contains(col))){
								impliedConstraint = new ConstraintObject();
								impliedConstraint.setLeftConstraint(ConstraintGenerator.smtMap(table.getColumn(col),"ipk"+p));
								impliedConstraint.setRightConstraint(ConstraintGenerator.smtMap(table.getColumn(col),"jpk"+p));
								impliedConstraint.setOperator("=");
								//get primary Key constraint implied other constraints and add to constraint list
								impliedConstraintList.add(impliedConstraint);
							}
						}
						impliedConstraintString =constraintGenerator.generateANDConstraints(impliedConstraintList);
						if(impliedConstraintString != null && !impliedConstraintString.isEmpty()){
							temp1 +=impliedConstraintString +")";
						}else{
							temp1 += " true)";
						}
						temp1 +=")) \n";

						pkConstraint += temp1;
					//}
						*/
				}


			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"\n Exception in AddDatabaseConstraints.java: Function generateConstraintsForPrimaryKeys : ",e);
			throw e;
		}
		return pkConstraint;
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

			/**get the list of equi join conditions on this foreign key table*/
			Vector< Vector< Node > > equiJoins = cvc.getEquiJoins().get(fkTableName);

			/**stores whether there are any equi joins conditions between foreign key and primary key columns*/
			HashMap<String, Boolean> presentinJoin = new HashMap<String, Boolean>();

			/**if there are equi joins*/
			//			if( equiJoins != null && equiJoins.size() != 0){
			//	
			//				/**check of these equi joins conditions are between foreign key and primary key columns*/
			//				presentinJoin = checkIfForeignKeysInvoledInJoins(foreignKey, equiJoins);
			//	
			//				/**if any of these equi joins are between foreign key and primary key table, then no need to generate the constraints*/
			//				for( String fkTableNo: presentinJoin.keySet()){
			//	
			//					/**get the total count for this relation occurrence*/
			//					int totalCount = getTotalNumberOfTuples(cvc, fkTableNo);
			//	
			//					/**decrement count*/
			//					fkCount[0] -= totalCount;
			//				}
			//			}
			//	
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

					/**means this foreign key relation occurrence does not have join conditions*/
					if(!presentinJoin.containsKey(fkTableNameNo)) {

						/**get the total count for this relation occurrence*/
						int count = getTotalNumberOfTuples(cvc, fkTableNameNo);

						/**decrement count*/
						fkCount[0] -= count;

						/**get the foreign key constraint*/
						fkConstraint += getFkConstraint(cvc, foreignKey, fkTableNameNo, count, 0);

						/**get the primary key tuple offset */
						int pkOffset = cvc.getNoOfOutputTuples().get(foreignKey.getReferenceTable().getTableName()) - fkCount[0];

						//violate += getNegativeCondsForExtraTuples(cvc, foreignKey, fkTableNameNo, count, 0, pkOffset);			
					}
				}

				/**once done for all relation occurrences in original query, then 
				 * get constraints for the extra tuples (Added due to foreign key relationship)*/
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


	/**
	 * check whether there are any equi join conditions between primary key and foreign key table 
	 * @param foreignKey
	 * @param equiJoins
	 * @return
	 */
	private static HashMap<String, Boolean> checkIfForeignKeysInvoledInJoins(ForeignKey foreignKey, Vector<Vector<Node>> equiJoins) throws Exception{

		HashMap<String, Boolean> presentJoins = new HashMap<String, Boolean>();
		try{
			/** Get foreign key table details */
			String fkTableName = foreignKey.getFKTablename();			
			Vector<Column> fCol = (Vector<Column>)foreignKey.getFKeyColumns().clone();

			/**get the occurrence of the foreign key table in this equi join condition*/
			for(Vector< Node> eq: equiJoins){

				String fkTableNameNo = null;
				for(Node n: eq){

					/**if this is foreign key table*/
					if( n.getTable().getTableName().equals(fkTableName))
						fkTableNameNo = n.getTableNameNo();
				}

				if( fkTableNameNo == null)/**there is no foreign key in this equi join conditions*/
					continue ;

				if( checkIfForeignKeysInvoledInEquiJoins(foreignKey, equiJoins) )/**if present in joins*/
					presentJoins.put(fkTableNameNo, true);

			}
			return presentJoins;
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
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
	 * Used to check if the foreign key of this table involved in joins with the primary key
	 * @param cvc
	 * @param eqClasses
	 * @return
	 * @throws Exception
	 */
	public static boolean checkIfForeignKeysInvoledInEquiJoins( ForeignKey foreignKey, Vector<Vector<Node>> eqNodes) throws Exception{

		try{
			/** Get foreign key table details */
			String ftableName = foreignKey.getFKTablename();			
			Vector<Column> fCol = (Vector<Column>)foreignKey.getFKeyColumns().clone();

			/**A boolean vector to indicate which attribute of this foreign key are involved in joins */
			ArrayList<Boolean> presenList = new ArrayList<Boolean>();

			/**traverse each attribute of this foreign key*/
			for(int i=0; i < fCol.size(); i++){

				/**get the attribute of this foreign key table*/
				Column c = fCol.get(i);

				/**flag to indicate if this attribute of foreign key is present in join conditions*/
				boolean present = false;



				for(Vector<Node> eqClasses: eqNodes){

					boolean pkPresent = false;/**To indicate whether primary key is found in this equivalence class*/

					boolean fkPresent = false;/**To indicate whether foreign key is found in this equivalence class*/

					/**for each node in this equivalence class*/
					for(Node n: eqClasses){

						/**FIXME: Repeated relation in same block of the query*/
						/**check if foreign key table is present in this equivalence class*/
						if( fkPresent == false && c.getColumnName().equalsIgnoreCase(n.getColumn().getColumnName()) && c.getTableName().equalsIgnoreCase( n.getColumn().getTableName()))
							fkPresent = true;

						/**check if foreign key table is present in this equivalence class*/
						if( pkPresent == false && c.getReferenceColumn().getColumnName().equalsIgnoreCase(n.getColumn().getColumnName()) && c.getReferenceTableName().equalsIgnoreCase( n.getColumn().getTableName()) )
							pkPresent = true;

						if( pkPresent && fkPresent){
							present = true;
							break;
						}
					}
					if(present)
						break;
				}
				/**update the list*/
				presenList.add(i, present);

			}

			/**If all the attributes are involved in joins then nothing need to be done
			 * But if at least one of the attributes is not involved in joins then its better to add extra tuples */
			int i = 0;
			for(i = 0; i < presenList.size(); i++)
				if( presenList.get(i) == false) /**if it is not present in the joins*/
					return false;

			if( i == presenList.size()) /**means all the attributes are present in the joins*/
				return true;
			return false;

		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}

    // TODO: maybe rename fOffset param - almost identical to fkOffset
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
		pkCount = fkCount;
		if( cvc.getNoOfOutputTuples().get(pkTableName) == null || cvc.getNoOfOutputTuples().get(pkTableName) == 0){
			offset = 1;
		}

		else{
			offset = cvc.getNoOfOutputTuples().get(pkTableName) + 1;
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

		if (cvc.getConstraintSolver().equals("cvc3")) {
			fkConstraint = getSMTforForeignKey(cvc, foreignKey, fkCount, fkOffset, offset);
		} else {
			Vector<BoolExpr> fkConstraintsVector = getSMTforForeignKeyZ3(cvc, foreignKey, fkCount, fkOffset, offset);
	        fkConstraint = fkConstraintsVector.stream().map(
	        		assertion -> "(assert "+assertion.toString()+")"
	        		).collect(Collectors.joining("\n\n"));
		}
		return fkConstraint;
	}	



	/**
	 * Get CVC constraints for foreign keys
	 * @param foreignKey
	 * @param fkCount
	 * @param fkOffset
	 * @param pkOffset
	 * @return
	 */

	public static String getSMTforForeignKey(GenerateCVC1 cvc, ForeignKey foreignKey, int fkCount, int fkOffset, int pkOffset) {
		String fkConstraint = "";

		/** Get foreign key column details */					
		Vector<Column> fCol = (Vector<Column>) foreignKey.getFKeyColumns().clone();

		/** Get primary key column details*/
		Vector<Column> pCol = (Vector<Column>) foreignKey.getReferenceKeyColumns().clone();

		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		ConstraintObject con = new ConstraintObject();
		String isNullCon = "" ;
		ArrayList<ConstraintObject> conList = new ArrayList<ConstraintObject>();
		String 	constraintString = "";

		ArrayList<String> isNullConstraintList = new ArrayList<String>();
		String  isNullConstraintString = "";

		/** Get the constraints for this foreign key */
		/*for(int j=1;j <= fkCount; j++){
			String temp1 = "";
			String temp2 = "";
			for (Column fSingleCol : fCol)
			{
				Column pSingleCol = pCol.get(fCol.indexOf(fSingleCol));
				if(fSingleCol.getCvcDatatype() != null)
				{
					int pos1 = fSingleCol.getTable().getColumnIndex(fSingleCol.getColumnName());
					int pos2 = pSingleCol.getTable().getColumnIndex(pSingleCol.getColumnName());
					String tableName1 = fSingleCol.getTable().getTableName();
					String tableName2 = pSingleCol.getTable().getTableName();

					con = constraintGenerator.getConstraint(tableName1,Integer.valueOf(j + fkOffset -1 ),Integer.valueOf(pos1),
							tableName2,Integer.valueOf(j + pkOffset - 1),Integer.valueOf(pos2),fSingleCol,pSingleCol,"=");
					//Add single constraint to the list of constraints. Later call constraintGenerator.generateANDConstraints to AND all constraints based on solver.
					if(!conList.contains(con)){
						conList.add(con);
					}	
					if(fSingleCol.isNullable()){
							isNullCon = constraintGenerator.getIsNullCondition(tableName1,fSingleCol,(Integer.valueOf(j + fkOffset -1)).toString());
							//Add single constraint to the list of constraints. Later call constraintGenerator.getNullConditionConjuncts to AND all Null constraints based on solver.
							if(! isNullConstraintList.contains(isNullCon)){
								isNullConstraintList.add(isNullCon);
							}
					}
				}				

			 //Commented - below code - used before SMT LIB implementation  
//			  }
//			if(temp1 != null && !temp1.isEmpty()){
//				temp1 = temp1.substring(0, temp1.length() - 5);
//			}
//			
//			if(!temp2.isEmpty()){
//				temp2 = temp2.substring(0, temp2.length() - 5);
//				fkConstraint += "ASSERT (" + temp1 + ") OR (" + temp2 + ");\n";
//			}
//			else {
//				fkConstraint += "ASSERT (" + temp1 + ");\n";
//			 
			}

		constraintString =constraintGenerator.generateANDConstraints(conList);
		isNullConstraintString = constraintGenerator.getNullConditionConjuncts(isNullConstraintList);
		fkConstraint = constraintGenerator.getFKConstraint(constraintString, isNullConstraintString); 

		}*/

		//Implementation to be changed for SMT constraint Solver to use FORALL
		Vector<Column> temp = new Vector<Column>();

		for(int j=1;j <= fkCount; j++){
			String temp1 = "";
			String temp2 = "";
			temp.clear();
			for (Column fSingleCol : fCol)
			{   
				if(!temp.contains(fSingleCol)) {
					temp.add(fSingleCol);
				}
				else
					continue;
				Column pSingleCol = pCol.get(fCol.indexOf(fSingleCol));
				if(fSingleCol.getCvcDatatype() != null)
				{
					temp1 += "(O_" + GenerateCVCConstraintForNode.cvcMap(fSingleCol, j + fkOffset -1 + "") + " = O_" + 
							GenerateCVCConstraintForNode.cvcMap(pSingleCol, (j + pkOffset - 1) + "" ) + ") AND ";
					if(fSingleCol.isNullable()){
						if(fSingleCol.getCvcDatatype().equals("INT")|| fSingleCol.getCvcDatatype().equals("REAL") || fSingleCol.getCvcDatatype().equals("DATE") || fSingleCol.getCvcDatatype().equals("TIME") || fSingleCol.getCvcDatatype().equals("TIMESTAMP"))
							temp2 += "ISNULL_" + fSingleCol.getColumnName() + "(O_" + GenerateCVCConstraintForNode.cvcMap(fSingleCol, j + fkOffset -1 + "") + ") AND ";
						else
							temp2 += "ISNULL_" + fSingleCol.getCvcDatatype() + "(O_" + GenerateCVCConstraintForNode.cvcMap(fSingleCol, j + fkOffset -1 + "") + ") AND ";
					}
				}
			}
			if(temp1 != null && !temp1.isEmpty()){
				temp1 = temp1.substring(0, temp1.length() - 5);
			}

			if(!temp2.isEmpty()){
				temp2 = temp2.substring(0, temp2.length() - 5);
				fkConstraint += "ASSERT (" + temp1 + ") OR (" + temp2 + ");\n";
			}
			else {
				fkConstraint += "ASSERT (" + temp1 + ");\n";
			}
		}

		return fkConstraint;
	}

	/**
	 * Get Z3 constraints for foreign keys as a Vector<BoolExpr>
	 * @param foreignKey
	 * @param fkCount
	 * @param fkOffset
	 * @param pkOffset
	 * @return
	 */

	public static Vector<BoolExpr> getSMTforForeignKeyZ3(GenerateCVC1 cvc, ForeignKey foreignKey, int fkCount, int fkOffset, int pkOffset) {

		/** Get foreign key column details */					
		Vector<Column> fCol = (Vector<Column>) foreignKey.getFKeyColumns().clone();

		/** Get primary key column details*/
		Vector<Column> pCol = (Vector<Column>) foreignKey.getReferenceKeyColumns().clone();

		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
				
		Vector<Column> temp = new Vector<Column>();
		Vector<BoolExpr> fkConstraints = new Vector<BoolExpr>();
		Vector<BoolExpr> orConstraints = new Vector<BoolExpr>();
		
		/* With forall construct
		 * 
		for (Column fSingleCol : fCol)
		{	
			if(!temp.contains(fSingleCol)) {
				temp.add(fSingleCol);
			}
			else
				continue;
			
			Column pSingleCol = pCol.get(fCol.indexOf(fSingleCol));
			String tableName1 = fSingleCol.getTable().getTableName();
			if(fSingleCol.getCvcDatatype() != null)
			{
				fkConstraint += "\n (assert (forall ((ifk"+j+" Int)) (exists ((jfk"+j+" Int)) ";
				if(fSingleCol.isNullable()){
					fkConstraint +="(or ";
				}
				fkConstraint += "(= "+constraintGenerator.smtMap(fSingleCol, "ifk"+j)+" "+constraintGenerator.smtMap(pSingleCol, "jfk"+j)+")";

				if(fSingleCol.isNullable()){
					fkConstraint += constraintGenerator.getIsNullCondition(tableName1,fSingleCol,"ifk"+j) +")";
				}
				fkConstraint +="))) \n";
			}
		}
		*/
		Context ctx = ConstraintGenerator.ctx;
		for (int j = 1; j <= fkCount; j++) {
			temp.clear();
			for (int k = 0; k < fCol.size(); k++) {
				orConstraints.clear();
				Column fSingleCol = fCol.get(k);

				if (!temp.contains(fSingleCol)) {
					temp.add(fSingleCol);
				} else {
					continue;
				}

				Column pSingleCol = pCol.get(k);
				String tableName1 = fSingleCol.getTable().getTableName();
				if (fSingleCol.getCvcDatatype() != null) {
					IntExpr fIndex = (IntExpr) ctx.mkInt(j + fkOffset -1);  // making type cast explicit
					IntExpr pIndex = (IntExpr) ctx.mkInt(j + pkOffset -1);
					orConstraints.add(ctx.mkEq(ConstraintGenerator.smtMap(fSingleCol, fIndex), ConstraintGenerator.smtMap(pSingleCol, pIndex)));

					if (fSingleCol.isNullable()) {
						orConstraints.add(constraintGenerator.getIsNullConditionZ3(tableName1,fSingleCol, j + fkOffset -1 + ""));
					}
					else
						fkConstraints.add(ctx.mkOr(orConstraints.toArray(new BoolExpr[orConstraints.size()])));
				}
			}
			
		}
	    return fkConstraints;
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
	 * Used to check if the foreign key of this table involved in joins with the primary key
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<Boolean> checkIfForeignKeysInvoledInJoins( QueryBlockDetails queryBlock, ForeignKey foreignKey) throws Exception{

		try{
			/**get the list of equivalence classes in this query block*/
			Vector<Vector<Node>> eqClasses = new Vector< Vector<Node>>();

			/**FIXME: Adding equivalence classes across all conjuncts. But we should consider only one conjunct*/
			for(ConjunctQueryStructure con: queryBlock.getConjunctsQs())
				eqClasses.addAll(con.getEquivalenceClasses());	


			/** Get foreign key table details */
			String ftableName = foreignKey.getFKTablename();			
			Vector<Column> fCol = (Vector<Column>)foreignKey.getFKeyColumns().clone();

			/**A boolean vector to indicate which attribute of this foreign key are involved in joins */
			ArrayList<Boolean> presenList = new ArrayList<Boolean>();

			/**traverse each attribute of this foreign key*/
			for(int i=0; i < fCol.size(); i++){

				/**get the attribute of this foreign key table*/
				Column c = fCol.get(i);

				/**flag to indicate if this attribute of foreign key is present in join conditions*/
				boolean present = false;

				boolean pkPresent = false;/**To indicate whether primary key is found in this equivalence class*/

				boolean fkPresent = false;/**To indicate whether foreign key is found in this equivalence class*/

				/**for each equivalence class*/
				for(Vector<Node> ec: eqClasses){

					/**for each node in this equivalence class*/
					for(Node n: ec){

						/**FIXME: Repeated relation in same block of the query*/
						/**check if foreign key table is present in this equivalence class*/
						if( fkPresent == false && c.getColumnName().equalsIgnoreCase(n.getColumn().getColumnName()) && c.getTableName().equalsIgnoreCase( n.getColumn().getTableName()))
							fkPresent = true;

						/**check if foreign key table is present in this equivalence class*/
						if( pkPresent == false && c.getReferenceColumn().getColumnName().equalsIgnoreCase(n.getColumn().getColumnName()) && c.getReferenceTableName().equalsIgnoreCase( n.getColumn().getTableName()) )
							pkPresent = true;

						if( pkPresent && fkPresent){
							present = true;
							break;
						}
					}
				}

				/**update the list*/
				presenList.add(i, present);

			}

			return presenList;

		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
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
			ArrayList<ConstraintObject> conList = new ArrayList<ConstraintObject>();
			ConstraintGenerator constraintGenerator = new ConstraintGenerator();

			/** For each relation that is present in this query block*/
			/** Here we are considering the repeated relation occurrences */
			for(String relation : queryBlock.getBaseRelations()){

				/**Get base table name for this relation*/
				String tableName = relation.substring(0, relation.length()-1);/**FIXME: If the relation occurrence >= 10 then problem*/

				/**Get the table details from base table*/
				/*Table table = null;
				for(int i=0; i < cvc.getResultsetTables().size(); i++){
					Table table1 = (Table)cvc.getResultsetTables().get(i);
					if(table1.getTableName().equalsIgnoreCase(tableName)){*//**The data base relation is found*//*
						table = table1;
						break ; 
					}
				}*/

				Table table = cvc.getQuery().getFromTables().get(tableName);
				/**If there is no table */
				if(table == null)
					continue ;

				/**Get the primary keys of this table*/
				ArrayList<Column> primaryKey = new ArrayList<Column>( table.getPrimaryKey() );
				
				ArrayList<Column> primaryKeys = new ArrayList<Column>();
				
				for (Column element : primaryKey) { 
		            if (!primaryKeys.contains(element)) { 
		            	primaryKeys.add(element); 
		            } 
		        } 


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

				
				if(!cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
					
					constraintString += "(assert (or \n";
					
					for(int p = 0; p < primaryKeys.size(); p++){
						ConstraintObject con = new ConstraintObject();
						/** Get column details */
						Column pkeyColumn = primaryKeys.get(p);

						/**get the column index in the base table*/
						int pos = table.getColumnIndex(pkeyColumn.getColumnName());
						
						for(int k=1; k <= totalTuples; k++) {
							if(k==1)
							constraintString += "\t(distinct ";
							
							constraintString += "\n \t ("+tableName+"_"+pkeyColumn.getColumnName()+pos+" (select O_"+tableName+" " + Integer.valueOf(k + offset - 1) +")"+")";
						
							if(k==totalTuples)
							constraintString += "\n\t) \n";
						}
						
					}
					constraintString += "))\n";
					
				}
				else {
				/** Get the actual constraints */
					
					
					
					
					for(int p = 0; p < primaryKeys.size(); p++){
						ConstraintObject con = new ConstraintObject();
						/** Get column details */
						Column pkeyColumn = primaryKeys.get(p);

						/**get the column index in the base table*/
						int pos = table.getColumnIndex(pkeyColumn.getColumnName());
						

							//conList.add(con);
							con.setLeftConstraint(constraintGenerator.getDistinctConstraints(tableName, pkeyColumn,totalTuples , pos));
							conList.add(con);
							//constraintString +=
							//constraintString += " DISTINCT ( O_"+tableName+"[" + (k + offset - 1) + "]."+pos+" , O_"+tableName+"["+ (j + offset - 1) +"]."+pos + ") OR ";
						}
						constraintString += constraintGenerator.generateOrConstraintsWithAssert(conList)+"\n";
							
					}
					
			}
			return constraintString;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}


	/**
	 * Ensures that the extra tuples added to satisfy foreign key relationship do not modify the query result
	 * Ensures by violating at least one of the conditions of the query 
	 * The conditions can be equivalence classes or non equi joins or selection conditions or like conditions
	 * @param cvc
	 * @param foreignKey
	 * @param fkTableNameNo
	 * @param fkCount
	 * @param fkOffset
	 * @param pkOffset
	 * @return
	 */
	/*public static String getNegativeCondsForExtraTuples(GenerateCVC1 cvc, ForeignKey foreignKey, String fkTableNameNo, int fkCount, int fkOffset, int pkOffset) {

		//**Store the constraint
		String fkViolate = "";

		Vector<String> orConstraints = new Vector<String>();

		//** Get primary key table name
		String pkTableName = foreignKey.getReferenceTable().getTableName();

		//**get the repeated relations for this foreign key table
		int repeatedCount = -1;

		if( cvc.getRepeatedRelationCount().get(pkTableName) != null)

			repeatedCount = cvc.getRepeatedRelationCount().get(pkTableName);

		else //**if primary key table is not involved in the query then nothing has to be done
			return fkViolate;


		//** get names of pk attributes and names of fk attributes 
		Vector<String> pkNames = new Vector<String>();
		Vector<String> fkNames = new Vector<String>();

		for(Column c: (Vector<Column>)foreignKey.getFKeyColumns().clone() )
			fkNames.add(c.getColumnName());

		for(Column c: (Vector<Column>)foreignKey.getReferenceKeyColumns().clone() )
			pkNames.add(c.getColumnName());

		//** If it is involved in the query, then the extra tuples added for foreign key relationship should not affect the result of constrained aggregation 


		//**get the equivalence classes of the primary key table
		Vector< Vector< Node>> eqClass = getEquivalenceClassesOfRelation(cvc, pkTableName);
		Vector<Node> eqJoinConds = UtilsRelatedToNode.getJoinConditions(eqClass);

		//*get the non equi joins of the primary key table

		//**generate for each extra tuple
		for(int i = 1; i <= fkCount; i++){

			//**for each equi join condition
			for( Node jn: eqJoinConds){

				String violate = "";
				//**check if this join condition is not between foreign key and primary key columns
				if(pkFkColumnsInvolved(jn, pkNames, fkNames) == false){

					Node pkNode = null, otherNode = null;
					if(jn.getLeft().getTable().getTableName().equalsIgnoreCase(pkTableName)){
						pkNode = jn.getLeft();
						otherNode = jn.getRight();
					}
					else{
						otherNode = jn.getLeft();
						pkNode = jn.getRight();
					}

					//**the number of extra tuples added in pkTable will be equal to number of tuples in FKTable i.e. 'fkCount'
					for(int j = 1; j <= fkCount; j++){	
						//**get constraint for primary key node
						String pKey = GenerateCVCConstraintForNode.cvcMapNode( pkNode, (pkOffset + j )+"");

						//**get the number of tuples on other relation
						int otherCount = 0;
						if(otherNode.getTable().getTableName() != null)
							otherCount = cvc.getNoOfOutputTuples().get(otherNode.getTable().getTableName()) ;

						for(int k = 1; k <= otherCount; k++){

							String other = GenerateCVCConstraintForNode.cvcMapNode( otherNode, k +"");

							violate += " NOT ( " + other + " = " + pKey + " ) AND";
						}
					}

					violate = "("+ violate.substring(0, violate.lastIndexOf("AND")) +")";
					orConstraints.add("ASSERT " + violate + ";");
				}				
			}


		}

		if(!orConstraints.isEmpty() && orConstraints.size() != 0){
			fkViolate +=  GenerateConstraintsForConjunct.processOrConstraintsNotExists(orConstraints);
			orConstraints.clear();
		}

		//*fkViolate = oldMethodForEqClass(cvc, fkTableNameNo, fkCount, fkOffset,
				//pkOffset, fkViolate, pkTableName, repeatedCount, pkNames,
				//fkNames);

		return fkViolate;
	}*/


	/**
	 * check if the given condition is between primary and foreign key columns
	 * @param jn
	 * @param pkNames
	 * @param fkNames
	 * @return
	 */
	public static boolean pkFkColumnsInvolved(Node jn, Vector<String> pkNames,
			Vector<String> fkNames) {

		if( fkNames.contains(jn.getLeft().getColumn().getColumnName() )&& pkNames.contains(jn.getRight().getColumn().getColumnName() ) )
			return true;

		if( pkNames.contains(jn.getLeft().getColumn().getColumnName() )&& fkNames.contains(jn.getRight().getColumn().getColumnName() ) )
			return true;

		return false;
	}


	/**
	 * This method gets the equivalence classes of the relation across all query blocks
	 * @param cvc
	 * @param pkTableName
	 * @return
	 */
	public static Vector<Vector<Node>> getEquivalenceClassesOfRelation(GenerateCVC1 cvc, String pkTableName) {

		Vector< Vector< Node >> eqClass = new Vector<Vector<Node>>();

		eqClass.addAll( getEqClassInBlock(cvc.getOuterBlock(), pkTableName));

		for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries())
			eqClass.addAll( getEqClassInBlock(qbt, pkTableName));

		for(QueryBlockDetails qbt: cvc.getOuterBlock().getWhereClauseSubQueries())
			eqClass.addAll( getEqClassInBlock(qbt, pkTableName));

		return eqClass;
	}




	/**
	 * Get equivalence classes in this query block
	 * @param outerBlock
	 * @param pkTableName
	 * @return
	 */
	public static Vector< Vector< Node >> getEqClassInBlock(QueryBlockDetails qbt, String pkTableName) {


		Vector< Vector< Node >> eqClass = new Vector<Vector<Node>>();
		for(ConjunctQueryStructure con: qbt.getConjunctsQs())
			for(Vector<Node> ec: con.getEquivalenceClasses() )
				if( relatedToRelation(ec,pkTableName))
					eqClass.add(ec);

		return eqClass;
	}


	public static boolean relatedToRelation(Vector<Node> ec, String pkTableName) {
		for(Node n: ec)
			if(n.getTable().getTableName().equalsIgnoreCase(pkTableName))
				return true;
		return false;
	}



	/*private static String oldMethodForEqClass(GenerateCVC1 cvc,
			String fkTableNameNo, int fkCount, int fkOffset, int pkOffset,
			String fkViolate, String pkTableName, int repeatedCount,
			Vector<String> pkNames, Vector<String> fkNames) {
		//**check for each occurrence of this foreign key table, if
		//**joins between foreign key and primary key table are not true then add foreign key constraints for that relation occurrence
		for(int l = 1; l <= repeatedCount; l++){

			String pkTableNameNo = pkTableName + l;

			//**get the query block type and query index of in which this primary key table is present
			int queryType = cvc.getTableNames().get(pkTableNameNo)[0];
			int queryIndex = cvc.getTableNames().get(pkTableNameNo)[1];

			//**The conditions can be equivalence classes or non equi joins of primary key table or
			 //*  selection conditions or like conditions of primary key table


			//**get equivalence classes of primary key table
			Vector< Vector< Node>> eqClass = getEquivalenceClassesOfThisRelation(cvc, pkTableNameNo, queryType, queryIndex);

			String violate = "";
			//** The extra tuples added should fail on at least one of the conditions

			//**generate for each extra tuple
			for(int i = 1; i <= fkCount; i++){


				//**for each equivalence class
				for( Vector< Node > ec: eqClass){

					Node fkNode = null, pkNode = null;

					for(Node n: ec){

						//**get the condition related to foreign key table
						if(fkNode == null && UtilsRelatedToNode.checkIfCorrespondToThisTableOccurrence(n, fkTableNameNo))
							fkNode = n;

						//**get the condition related to primary key table
						if(pkNode == null && UtilsRelatedToNode.checkIfCorrespondToThisTableOccurrence(n, pkTableNameNo))
							pkNode = n;

						//**if there is a join condition between foreign key and primary key table
						if( fkNode != null && pkNode != null){

							//**check if this join condition is not between foreign key and primary key columns
							if( !fkNames.contains( fkNode.getColumn().getColumnName() ) && !pkNames.contains( pkNode.getColumn().getColumnName() ) ){

								//**the number of extra tuples added in pkTable will be equal to number of tuples in FKTable i.e. 'fkCount'
								for(int j = 1; j <= fkCount; j++){	

									//**get constraint for foreign key node
									String fKey = GenerateCVCConstraintForNode.cvcMapNode( fkNode, (fkOffset + j )+"");

									for(int k = 1; k <= fkCount; k++){

										//**get constraint for primary key node
										String pKey = GenerateCVCConstraintForNode.cvcMapNode( pkNode, (pkOffset + k )+"");

										fkViolate += " NOT ( " + fKey + " = " + pKey + " ) AND";
									}
								}
							}
							pkNode = null;
							fkNode = null;
						}
					}
				}
			}
		}
		return fkViolate;
	}*/


	private static Vector< Vector< Node >> getEquivalenceClassesOfThisRelation(	GenerateCVC1 cvc, String pkTableNameNo, int queryType, int queryIndex) {



		/**get the equivalence classes of this query block*/
		Vector< Vector< Node >> eqClass = new Vector<Vector<Node>>();

		if( queryType == 1) /**if from clause nested sub query block*/

			for(ConjunctQueryStructure con: cvc.getOuterBlock().getFromClauseSubQueries().get(queryIndex).getConjunctsQs())

				eqClass.addAll( con.getEquivalenceClasses()) ;

		else if ( queryType == 2) /**if where clause sub query block*/

			for(ConjunctQueryStructure con: cvc.getOuterBlock().getWhereClauseSubQueries().get(queryIndex).getConjunctsQs())

				eqClass.addAll( con.getEquivalenceClasses()) ;

		else

			for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs())

				eqClass.addAll( con.getEquivalenceClasses()) ;

		return eqClass;
	}

}
