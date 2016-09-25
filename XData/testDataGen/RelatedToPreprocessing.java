package testDataGen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Conjunct;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Query;
import parsing.Table;
import util.Configuration;
import util.DatabaseConnection;
import util.MyConnection;
import util.Utilities;

/**
 * Contains methods needed to preprocessing actions
 * @author mahesh
 *
 */
public class RelatedToPreprocessing {

	private static Logger logger = Logger.getLogger(RelatedToPreprocessing.class.getName());
	/**
	 * 
	 * @param filePath
	 */
	@Deprecated
	public static void deleteDatasets(String filePath) {

	}

	/**
	 * Generates data values from the input data base
	 * These values are used while generating the data for the query
	 * @param cvc	 
	 */
	public static void populateData(GenerateCVC1 cvc) throws Exception{

		/**if there are branch queries*/

		if( cvc.getBranchQueries().getBranchQueryString() != null && !cvc.getBranchQueries().getBranchQueryString().equals(""))
			populateDataWithBranchQueries(cvc);
		else
			populateDataWithoutBranchQueries(cvc);
	}

	/**
	 * Generates data values for the set of branch queries
	 * @param cvc
	 * @throws Exception
	 */
	public static void populateDataWithBranchQueries(GenerateCVC1 cvc) throws Exception{
		int connId = 0, schemaId = 0,optionalSchemaId=0;
		try(Connection conn = MyConnection.getDatabaseConnection()){
			try(PreparedStatement stmt = conn.prepareStatement("select connection_id, defaultschemaid from xdata_assignment where assignment_id = ?")){
				stmt.setInt(1, cvc.getAssignmentId());
				 
				try(ResultSet result = stmt.executeQuery()){
					
					
					//Get optional Schema Id for this question
					try(PreparedStatement statement = conn.prepareStatement("select optionalschemaid from xdata_qinfo where assignment_id = ? and question_id= ? ")){
						statement.setInt(1, cvc.getAssignmentId()); 
						statement.setInt(2,cvc.getQuestionId()); 
						
						try(ResultSet resultSet = statement.executeQuery()){
							if(resultSet.next()){
								optionalSchemaId = resultSet.getInt("optionalschemaid");
							}
						} //try-with-resources - ResultSet -resultSet obj
					}//try-with-resources -PreparedStatement statement obj
					if(result.next()){
						connId = result.getInt("connection_id");
						//If optional schema id exists and it is not same as default schema id, then set it as schemaId 
						if(optionalSchemaId != 0 && optionalSchemaId != result.getInt("defaultschemaid")){	
							schemaId = optionalSchemaId;
						} else{
							schemaId = result.getInt("defaultschemaid");
						}
					} 
				}//try-with-resource - ResultSet obj
			}//try-with-resource - Preparedstmt obj
			try(Connection assignmentConn = (new DatabaseConnection()).getTesterConnection(cvc.getAssignmentId())){
				PopulateTestData p = new PopulateTestData();
				p.deleteAllTempTablesFromTestUser(assignmentConn);
				if(connId != 0 && schemaId != 0){
					byte[] dataBytes = null;
					String tempFile = "";
					FileOutputStream fos = null;
					ArrayList<String> listOfQueries = null;
					String[] inst = null;
					if(assignmentConn != null){
					
						try(PreparedStatement stmt = conn.prepareStatement("select ddltext from xdata_schemainfo where schema_id = ?")){
							stmt.setInt(1, schemaId);			
							try(ResultSet result = stmt.executeQuery()){
							
								// Process the result			
								if(result.next()){
									String fileContent= result.getString("ddltext");
									//String fr=fileContent.replace("\\\\","'");
									//String fc = fr.replace("\t", "    ");
									dataBytes = fileContent.getBytes();
									tempFile = "/tmp/dummy";
									
									fos = new FileOutputStream(tempFile);
									fos.write(dataBytes);
									fos.close();
									
									listOfQueries = Utilities.createQueries(tempFile);
									inst = listOfQueries.toArray(new String[listOfQueries.size()]);
									
									for (int i = 0; i < inst.length; i++) {
										// we ensure that there is no spaces before or after the request string  
										// in order to not execute empty statements  
										if (!inst[i].trim().equals("") && ! inst[i].trim().contains("drop table")) {
											String temp = inst[i].replaceAll("(?i)^[ ]*create[ ]+table[ ]+", "create temporary table ");
											try(PreparedStatement stmt2 = assignmentConn.prepareStatement(temp)){
												stmt2.executeUpdate();	
											}
										}
									}
								}
							}
						}
						try(PreparedStatement stmt = conn.prepareStatement("select sample_data from xdata_sampledata where schema_id = ?")){
							stmt.setInt(1, schemaId);			
							try(ResultSet result = stmt.executeQuery()){
							
								// Process the result			
								if(result.next()){
									String sdContent= result.getString("sample_data");
									//String sdReplace=sdContent.replace("\\\\","'");
									//fc = sdReplace.replace("\t", "    ");
									dataBytes = sdContent.getBytes(); 
									fos = new FileOutputStream(tempFile);
									fos.write(dataBytes);
									fos.close();
									
									listOfQueries = Utilities.createQueries(tempFile);
									inst = listOfQueries.toArray(new String[listOfQueries.size()]);
										
										for (int i = 0; i < inst.length; i++) {
											// we ensure that there is no spaces before or after the request string  
											// in order to not execute empty statements  
											if (!inst[i].trim().equals("") && !inst[i].contains("drop table") && !inst[i].contains("delete from")) {
												
												try(PreparedStatement stmt2 = assignmentConn.prepareStatement(inst[i])){
													stmt2.executeUpdate();		
												}
											}
								}
							}//try-with-resource resultset obj
						}//try-with-resource statement obj
					}
				  }
				}
				
				Query query = cvc.getQuery();
				Query branchQuery[] = cvc.getBranchQueries().getBranchQuery();
				ArrayList<String> branchResult[] = cvc.getBranchQueries().getBranchResultString();
				int noOfBranchQueries = cvc.getBranchQueries().getNoOfBranchQueries();
				
				Column column;
				Table table;
				Collection<Table> tables  = new Vector<Table>();
				tables.addAll(query.getFromTables().values());
		
				for(int i = 0; i < noOfBranchQueries; i++)
					for(Table tempTable : branchQuery[i].getFromTables().values())
						if(!tables.contains(tempTable))
							tables.add(tempTable);
		
				logger.log(Level.INFO,"tables  = " + tables);
		
				//Also add the foreign key tables
				Iterator iter = tables.iterator();
				while(iter.hasNext()){
					Table t = (Table)iter.next();
					if(t.hasForeignKey()){
						Map<String, ForeignKey> fks = t.getForeignKeys();
						Iterator iter2 = fks.values().iterator();
						while(iter2.hasNext()){
							ForeignKey fk = (ForeignKey)iter2.next();
							if(!tables.contains(fk.getReferenceTable())){
								tables.add(fk.getReferenceTable());
								iter = tables.iterator();
							}
						}
					}
				}
		
				cvc.setTablesOfOriginalQuery( new Vector<Table>() );
				cvc.getTablesOfOriginalQuery().addAll( query.getFromTables().values() );
				
				
				Iterator iter1 = cvc.getTablesOfOriginalQuery().iterator();
				
				while(iter1.hasNext()){
					Table t = (Table)iter1.next();
					
					if(t.hasForeignKey()){
						Map<String, ForeignKey> fks = t.getForeignKeys();
						
						Iterator iter2 = fks.values().iterator();
						
						while(iter2.hasNext()){
							ForeignKey fk = (ForeignKey)iter2.next();
							
							if(!cvc.getTablesOfOriginalQuery().contains(fk.getReferenceTable())){
								
								cvc.getTablesOfOriginalQuery().add(fk.getReferenceTable());
								
								iter1 = cvc.getTablesOfOriginalQuery().iterator();
							}
						}
					}
				}
		
				Iterator t = tables.iterator();
				cvc.getResultsetColumns().add(new Column("dummy","dummy"));
				
				while(t.hasNext()){
					table = (Table)t.next();
					
					cvc.getResultsetTables().add(table);
					
					Collection columns = table.getColumns().values();
					
					Iterator c = columns.iterator();
					
					while(c.hasNext()){
						column = (Column)c.next();
						
						column.intializeColumnValuesVector();
						
						String qs = "select distinct " + column.getColumnName() + " from " + table.getTableName() + " limit 50";
						
						PreparedStatement ps = assignmentConn.prepareStatement(qs);
						
						ResultSet rs = ps.executeQuery();
						
						int count = 0;
						
						ResultSetMetaData rsmd = rs.getMetaData();
						
						logger.log(Level.INFO,"rsmd " + rsmd.getColumnName(1) + " " + rsmd.getColumnTypeName(1));
						
						while(rs != null && rs.next()){
							
							String temp = rs.getString(column.getColumnName().toUpperCase());
							
							if(temp != null)
							{
								column.addColumnValues(temp);
								count++;
							}
						}
						if(rsmd.getColumnTypeName(1).equals("varchar"))
						{
							while(count < 20)
							{
								count++;
								column.addColumnValues(column.getColumnName() + "_" + count);
							}
						}
						/*if(rsmd.getColumnTypeName(1).equals("numeric") && column.getColumnName())
						{
							while(count < 50)
							{
								String value=Integer.toString(count); 
								count++;
								column.addColumnValues(value);
							} 
						}*/
						for(int i = 0; i < noOfBranchQueries; i++)
						{
							Vector<Node> tempColNode = cvc.getBranchQueries().getqParser1()[i].getProjectedCols();
							for(int j = 0; j < tempColNode.size(); j++)
								if(tempColNode.get(j).getColumn().equals(column))
								{
									column.addColumnValues(branchResult[i].get(j));
									logger.log(Level.INFO,"Extra col = " + branchResult[i].get(j));
								}
						}
						cvc.getResultsetColumns().add(column);
						rs.close();
						ps.close();
					}
				}
				}//try-with resource - Close assignmentConn obj
		
	 }//try-with-resource - connection obj
	}

	/**
	 * generates data values for this query
	 * @param cvc
	 * @throws Exception
	 */
	public static void populateDataWithoutBranchQueries(GenerateCVC1 cvc) throws Exception{


		Query query = cvc.getQuery();
		int connId = 0, schemaId = 0,optionalSchemaId=0;
		
		try(Connection conn= MyConnection.getDatabaseConnection()){
			try(PreparedStatement stmt = conn.prepareStatement("select connection_id, defaultschemaid from xdata_assignment where assignment_id = ?")){
				stmt.setInt(1, cvc.getAssignmentId());				
				try(ResultSet result = stmt.executeQuery()){

				//Get optional Schema Id for this question
				try(PreparedStatement statement = conn.prepareStatement("select optionalschemaid from xdata_qinfo where assignment_id = ? and question_id= ? ")){
					statement.setInt(1, cvc.getAssignmentId()); 
					statement.setInt(2,cvc.getQuestionId()); 
					
					try(ResultSet resultSet = statement.executeQuery()){
						if(resultSet.next()){
							optionalSchemaId = resultSet.getInt("optionalschemaid");
						}
					}//try-with-resources - ResultSet resultSet ends
				}//try-with-resources -PreparedStatement statement ends
				if(result.next()){
					connId = result.getInt("connection_id");
					//If optional schema id exists and it is not same as default schema id, then set it as schemaId 
					if(optionalSchemaId != 0 && optionalSchemaId != result.getInt("defaultschemaid")){	
						schemaId = optionalSchemaId;
					} else{
						schemaId = result.getInt("defaultschemaid");
					}
				} 
				}//try-with-resources - ResultSet result Ends
			}//try-with-resources - PreparedStatement stmt ends
			try(Connection assignmentConn = (new DatabaseConnection()).getGraderConnection(cvc.getAssignmentId())){
				PopulateTestData p = new PopulateTestData();
				p.deleteAllTempTablesFromTestUser(assignmentConn);
				byte[] dataBytes = null;
				String tempFile = "";
				
				ArrayList<String> listOfQueries = null;
				String[] inst = null;
				if(connId != 0 && schemaId != 0){			
					if(assignmentConn != null){
						try(PreparedStatement stmt = conn.prepareStatement("select ddltext from xdata_schemainfo where schema_id = ?")){
							stmt.setInt(1, schemaId);			
							try(ResultSet result = stmt.executeQuery()){
							
							// Process the result			
							if(result.next()){
								dataBytes = result.getBytes("ddltext");
								
								tempFile = "/tmp/dummy";
								
								try(FileOutputStream fos = new FileOutputStream(tempFile)){
									fos.write(dataBytes);
								}
								
								listOfQueries = Utilities.createQueries(tempFile);
								inst = listOfQueries.toArray(new String[listOfQueries.size()]);
								
								for (int i = 0; i < inst.length; i++) {
									// we ensure that there is no spaces before or after the request string  
									// in order to not execute empty statements  
									if (!inst[i].trim().equals("")) {
										String temp = inst[i].replaceAll("(?i)^[ ]*create[ ]+table[ ]+", "create temporary table ");
										try(PreparedStatement stmt2 = assignmentConn.prepareStatement(temp)){
											stmt2.executeUpdate();	
										}
										
									}
								}
							}
							}
						}
						try(PreparedStatement stmt = conn.prepareStatement("select sample_data from xdata_sampledata where schema_id = ?")){
							stmt.setInt(1, schemaId);			
							try(ResultSet result = stmt.executeQuery()){
							
							// Process the result			
							if(result.next()){
								dataBytes = result.getBytes("sample_data");
								try(FileOutputStream fos = new FileOutputStream(tempFile)){
									fos.write(dataBytes);
								}
								
								listOfQueries = Utilities.createQueries(tempFile);
								inst = listOfQueries.toArray(new String[listOfQueries.size()]);
								
								for (int i = 0; i < inst.length; i++) {
									// we ensure that there is no spaces before or after the request string  
									// in order to not execute empty statements  
									if (!inst[i].trim().equals("")) {
										
										try(PreparedStatement stmt2 = assignmentConn.prepareStatement(inst[i])){
											stmt2.executeUpdate();
										}
									}
					}
				}
				}//try-with-resources ResultSet result  ends
			}//try-with-resources - PreparedStatement stmt ends
		   }
		}
		
				/**Gets the name of tables required for the query and the name columns of that query. 
				 * Also checks for any foreign key reference and adds the referenced table to the list of tables being considered.*/
		
				Column column;
				Table table;
				Collection<Table> tables  = new Vector<Table>();
				tables.addAll(query.getFromTables().values());
				//Also add the foreign key tables
				Iterator iter = tables.iterator();
				while(iter.hasNext()){
					Table t = (Table)iter.next();
					if(t.hasForeignKey()){
						Map<String, ForeignKey> fks = t.getForeignKeys();
						Iterator iter2 = fks.values().iterator();
						while(iter2.hasNext()){
							ForeignKey fk = (ForeignKey)iter2.next();
							if(!tables.contains(fk.getReferenceTable())){
								tables.add(fk.getReferenceTable());
								iter = tables.iterator();
							}
						}
					}
				}
		
				/** Then for each column it gets a max of 50 distinct values from the tables existing in the database */
		
				Iterator t = tables.iterator();
				cvc.getResultsetColumns().add(new Column("dummy","dummy"));
				while(t.hasNext()){
					table = (Table)t.next();
					cvc.getResultsetTables().add(table);
					Collection columns = table.getColumns().values();
					Iterator c = columns.iterator();
					while(c.hasNext()){
						column = (Column)c.next();
						column.intializeColumnValuesVector();
						String qs = "select distinct " + column.getColumnName() + " from " + table.getTableName() + " limit 50";				
						PreparedStatement ps = assignmentConn.prepareStatement(qs);
						ResultSet rs = ps.executeQuery();
						while(rs != null && rs.next()){
							String temp = rs.getString(column.getColumnName().toUpperCase());
							if(temp != null)
								column.addColumnValues(temp);
						}
						cvc.getResultsetColumns().add(column);
						ps.close();
						rs.close();				
					}
				}
			  }
			}
	}
	/**
	 * Segregate selection conditions of the query
	 * @param cvc
	 */
	public static void segregateSelectionConditions(GenerateCVC1 cvc) throws Exception{

		/** Segregate selection conditions of the outer block of query */
		segregateSelectionConditionsForQueryBlock(cvc, cvc.getOuterBlock());

		/** Segregate selection conditions of each from clause nested sub query block */
		for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getFromClauseSubQueries())
			segregateSelectionConditionsForQueryBlock(cvc, queryBlock);

		/** Segregate selection conditions of each where clause nested sub query block */
		for(QueryBlockDetails queryBlock: cvc.getOuterBlock().getWhereClauseSubQueries())
			segregateSelectionConditionsForQueryBlock(cvc, queryBlock);

		int noOfBranchQueries = cvc.getBranchQueries().getNoOfBranchQueries();
		/**separate conditions in branch queries*/
		cvc.getBranchQueries().setStringSelectionCondsForBranchQuery( new ArrayList[ noOfBranchQueries ] );
		
		
		for(int i = 0; i < noOfBranchQueries; i++)
			cvc.getBranchQueries().getStringSelectionCondsForBranchQuery()[i] = new ArrayList<Node>();
		if( cvc.getBranchQueries().getBranchQueryString() != null)
			seggregateSelectionCondsForBranchQuery( cvc );
	}

	/**
	 * Segregate selection conditions in the given query block
	 * @param cvc
	 * @param queryBlock
	 */
	public static void segregateSelectionConditionsForQueryBlock(GenerateCVC1 cvc, QueryBlockDetails queryBlock) {

		/** Segregate selection conditions of each conjunct of this query block */
		for(Conjunct conjunct : queryBlock.getConjuncts()){
			conjunct.seggregateSelectionConds();
		}

	}





	public static void deletePreviousDatasets(GenerateDataset_new g, String query) throws IOException,InterruptedException {

		Runtime r = Runtime.getRuntime();
		logger.log(Level.INFO,Configuration.homeDir+"/temp_cvc"+g.getFilePath()+"/");
		File f=new File(Configuration.homeDir+"/temp_cvc"+g.getFilePath()+"/");
		
		if(f.exists()){		
			File f2[]=f.listFiles();
			if(f2 != null)
			for(int i=0;i<f2.length;i++){
				if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
					
					Process proc = r.exec("rm -rf "+Configuration.homeDir+"/temp_cvc"+g.getFilePath()+"/"+f2[i].getName());
					proc.waitFor();
					Utilities.closeProcessStreams(proc);
				}
			}
		}
		

		File dir= new File(Configuration.homeDir+"/temp_cvc"+g.getFilePath());
		if(dir.exists()){
			for(File file: dir.listFiles()) {
				file.delete();
			}
		}
		else{
			dir.mkdirs();
		}
		
		BufferedWriter ord = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+g.getFilePath()+"/queries.txt"));
		BufferedWriter ord1 = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+g.getFilePath()+"/queries_mutant.txt"));
		ord.write(query);
		ord1.write(query);
		ord.close();
		ord1.close();
	}


	/** 
	 * Get the list of datasets generated
	 * @param gd
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> getListOfDataset(GenerateDataset_new gd) throws Exception{

		ArrayList<String> fileListVector = new ArrayList<String>();		
		ArrayList<String> datasets = new ArrayList<String>();

		String fileList[]=new File(Configuration.homeDir+"/temp_cvc" + gd.getFilePath()).list();

		for(int k=0;k<fileList.length;k++){
			fileListVector.add(fileList[k]);
		}
		Collections.sort(fileListVector);	        
		for(int i=0;i<fileList.length;i++)
		{
			File f1=new File(Configuration.homeDir+"/temp_cvc" + gd.getFilePath() +"/"+fileListVector.get(i));	          
			if(f1.isDirectory() && fileListVector.get(i).substring(0,2).equals("DS"))
			{
				datasets.add(fileListVector.get(i));
			}
		}

		return datasets;
	}


	/**
	 * Sorts the foreign keys using the topological sort. 
	 * This is required to ensure that the extra tuples are added in the right order without violating foreign key constraints
	 * @param cvc
	 * @throws Exception
	 */
	public static void sortForeignKeys(GenerateCVC1 cvc ) throws Exception{

		Vector<Table> sortedTable = cvc.getTableMap().getAllTablesInTopSorted();
		int len = sortedTable.size();

		ArrayList<Node> foreignKeyTemp=(ArrayList<Node>)cvc.getForeignKeys().clone();
		cvc.getForeignKeys().removeAll(foreignKeyTemp);

		for(int i=0;i<len;i++){
			for(Node n:foreignKeyTemp){

				String ftable = n.getLeft().getTable().getTableName();
				String ptable = n.getRight().getTable().getTableName();
				if(sortedTable.get(i).getTableName().equals(ptable))

					for(int j=i+1;j<len;j++)						
						if(sortedTable.get(j).getTableName().equals(ftable))							
							cvc.getForeignKeys().add(n);				
			}
		}
	}

	/**
	 * This function finds the list of base relations occurred in each query block (The occurrence of each base relation)
	 * @param generateCVC1_new
	 */
	public static void getRelationOccurredInEachQueryBlok(	GenerateCVC1 cvc) {

		/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
		HashMap<String, Integer[]> tableNames = cvc.getTableNames();

		/**Get iterator for this hash map*/
		Iterator<String> it = tableNames.keySet().iterator();

		/**if there are tables in the hash map*/
		while(it.hasNext()){

			/**get the table occurrence*/
			String relationOccurrence = it.next();

			/**get the query type in which this relation occurred*/
			int queryType = tableNames.get(relationOccurrence)[0];

			/**get query index*/
			int queryIndex = tableNames.get(relationOccurrence)[1];

			/**if it is from clause sub query, add this to the list of base relations of the from clause sub queries*/
			if( queryType == 1)
				cvc.getOuterBlock().getFromClauseSubQueries().get(queryIndex).getBaseRelations().add(relationOccurrence);

			/**if it is where clause sub query, add this to the list of base relations of the where clause sub queries*/
			else if( queryType == 2)
				cvc.getOuterBlock().getWhereClauseSubQueries().get(queryIndex).getBaseRelations().add(relationOccurrence);

			/**if it is outer block of query, add this to the list of base relations of outer block of query*/
			else if( queryType == 0)
				cvc.getOuterBlock().getBaseRelations().add(relationOccurrence);
		}
	}

	/**
	 * Store details about branch queries in the input
	 * @param cvc
	 */
	public static void uploadBranchQueriesDetails(GenerateCVC1 cvc) {

		/**get the object that stores branch queries details*/
		BranchQueriesDetails branchQueries = cvc.getBranchQueries();

		int count=1;
		BufferedReader input2 = null;
		BufferedReader input3 = null;
		
		BufferedReader input4 = null;
		branchQueries.setNoOfBranchQueries(0);
		try
		{
			input2 =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+ cvc.getFilePath() +"/branchQuery.txt"));
			String str = null;
			while((str = input2.readLine()) != null)
				branchQueries.setNoOfBranchQueries( branchQueries.getNoOfBranchQueries() + 1 );
			
		}
		catch(Exception e)
		{
			logger.log(Level.SEVERE," "+e.getStackTrace(),e);
		}finally{
			try {
				if(input2 != null)
					input2.close();
			} catch (IOException e) {
				logger.log(Level.INFO,"Error in RelatedToPreProcessing : uploadBranchQueriesDetails Method :" + e.getMessage());
				
			}
		}

		branchQueries.setBranchQueryString( new String[ branchQueries.getNoOfBranchQueries()] );
		branchQueries.setBranchResultString( new ArrayList[ branchQueries.getNoOfBranchQueries()] );
		branchQueries.setBranchOperators( new ArrayList[ branchQueries.getNoOfBranchQueries()] );


		/** creates a file queries.txt in the specified folder*/

		logger.log(Level.INFO,Configuration.homeDir+"/temp_cvc"+ cvc.getFilePath() +"/queries.txt");
		try
		{
			input4 =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+ cvc.getFilePath() +"/branchQuery.txt"));
			input3 =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+ cvc.getFilePath() +"/branchResult.txt"));

			/*for(int i = 0; i < branchQueries.getNoOfBranchQueries(); i++)
				branchQueries.getBranchQueryString()[i] = input4.readLine().trim();

			for(int i = 0; i < branchQueries.getNoOfBranchQueries(); i++)
			{
				String[] tempStrArray = input3.readLine().trim().split("\\s+");

				branchQueries.getBranchResultString()[i] = new ArrayList<String>();
				branchQueries.getBranchOperators()[i] = new ArrayList<String>();


				for(int j = 0; j < tempStrArray.length; j++)
				{
					if(tempStrArray[j].equals("NULL"))
					{
						branchQueries.getBranchOperators()[i].add("=");
						branchQueries.getBranchResultString()[i].add(tempStrArray[j]);
					}
					else
					{
						branchQueries.getBranchOperators()[i].add(tempStrArray[j]);
						branchQueries.getBranchResultString()[i].add(tempStrArray[j + 1]);
						j++;
					}
					logger.log(Level.INFO,i + " branchOperators " + branchQueries.getBranchOperators()[i] + " branchResultString " + branchQueries.getBranchResultString()[i]);
				}
			}*/
		
		}
		catch(Exception e)
		{
			logger.log(Level.SEVERE,""+e.getMessage(),e);
		}finally{
			try {
				if(input4 != null)
					input4.close();
				if(input3 != null)
					input3.close();
			} catch (IOException e) {
				
				logger.log(Level.SEVERE,"Error in RelatedToPreProcessing : uploadBranchQueriesDetails Method :" + e.getMessage(),e);
			}
			
		}
		logger.log(Level.INFO,"branchQueryString " + branchQueries.getBranchQueryString());

	}


	public static void seggregateSelectionCondsForBranchQuery( GenerateCVC1 cvc) throws Exception{
		
		for(int i = 0; i < cvc.getBranchQueries().getNoOfBranchQueries(); i++)
		{
			Vector<Node> selectCondsClone = (Vector<Node>)cvc.getBranchQueries().getStringSelectionCondsForBranchQuery()[i].clone();
			for(Node n: selectCondsClone){			
				if( Conjunct.isStringSelection(n,1) ){
					String str=n.getRight().getStrConst();
					if(str!=null)
						n.getRight().setStrConst("'"+str+"'");
					cvc.getBranchQueries().getStringSelectionCondsForBranchQuery()[i].add(n);
					cvc.getBranchQueries().getSelectionCondsForBranchQuery()[i].remove(n);
				}
				else if( Conjunct.isStringSelection(n,0) ){
					cvc.getBranchQueries().getStringSelectionCondsForBranchQuery()[i].add(n);
					cvc.getBranchQueries().getSelectionCondsForBranchQuery()[i].remove(n);
				}
				else if(n.getLeft().getColumn()!=null && n.getLeft().getColumn().getCvcDatatype().equals("DATE")){
					java.sql.Date d=java.sql.Date.valueOf(n.getRight().getStrConst());
					n.getRight().setStrConst(""+d.getTime()/86400000);
				}
				else if(n.getLeft().getColumn()!=null && n.getLeft().getColumn().getCvcDatatype().equals("TIME")){
					java.sql.Time t=java.sql.Time.valueOf(n.getRight().getStrConst());
					n.getRight().setStrConst(""+(t.getTime()%86400)/1000);
				}
				else if(n.getLeft().getColumn()!=null && n.getLeft().getColumn().getCvcDatatype().equals("TIMESTAMP")){
					java.sql.Timestamp ts=java.sql.Timestamp.valueOf(n.getRight().getStrConst());
					n.getRight().setStrConst(""+ts.getTime()/1000);
				}
			}
			for(Node n : cvc.getBranchQueries().getLikeCondsForBranchQuery()[i]){
				n.getRight().setStrConst("'"+n.getRight().getStrConst()+"'");
			}
		}
	}

}
