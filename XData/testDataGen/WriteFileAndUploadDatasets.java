package testDataGen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import util.Configuration;
import util.DataSetValue;
import util.DatabaseConnection;
import util.FailedColumnValues;
import util.FailedDataSetValues;
import util.MyConnection;
import util.TableMap;

/**
 * Common methods
 * @author mahesh
 *
 */
public class WriteFileAndUploadDatasets {

	private static Logger logger = Logger.getLogger(WriteFileAndUploadDatasets.class.getName());
	public static void writeFile(String filePath, String content){
		try(java.io.FileWriter fw=new java.io.FileWriter(filePath, false)){
			fw.write(content);
			fw.flush();
		}catch(Exception e){
			logger.log(Level.SEVERE, "Message", e);
			//e.printStackTrace();
		}
	}
	
	
	/**
	 * Update the query info table
	 * @param gd
	 * @param queryId
	 * @param query
	 * @param queryDesc
	 * @throws Exception
	 */
	@Deprecated
	public static void updateQueryInfo(GenerateDataset_new gd,int assignmentId,int questionId,int queryId, String query,String queryDesc) throws Exception{

	}

	/**
	 * Upload the data sets into inout database
	 * @param gd
	 * @param queryId
	 * @param dataSets
	 */
	public static void uploadDataset(GenerateDataset_new gd, int assignmentId,int questionId, int queryId, String course_id,ArrayList<String> dataSets,TableMap tableMap) throws Exception{
		
		String prevDatasets = "SELECT datasetid,tag FROM xdata_datasetvalue WHERE assignment_id = '" + assignmentId + "' and question_id='"+questionId+"' and query_id ='"+queryId+"' and course_id= '"+course_id+"'";
		String existingDataSets = "SELECT * from xdata_datasetvalue where assignment_id = '" + assignmentId + "' and question_id='"+questionId+"' and query_id ='"+queryId+"' and course_id= '"+course_id+"'";
		try(Connection conn = MyConnection.getDatabaseConnection()){
			try(Connection testCon = new DatabaseConnection().getTesterConnection(assignmentId)){

				PopulateTestData p = new PopulateTestData();
				p.deleteAllTempTablesFromTestUser(testCon);
				p.createTempTables(testCon, assignmentId, questionId);
				Gson gson = new Gson();
				FailedDataSetValues instrDs = new FailedDataSetValues();
				
				String datasetid="";
				int maxid=0;
				String json1="";
				try(PreparedStatement smt = conn.prepareStatement(prevDatasets)){
					try(ResultSet rs =smt.executeQuery()){
						while(rs.next()){
							datasetid=rs.getString(1);
							int id=Integer.parseInt(datasetid.substring(2));
//To be tested
							if(id > maxid){
							//	maxid=id;
							}
						}
					}//try-with-resources - PreparedStatement rs closed
				}//try-with-resources - PreparedStatement smt closed
				for(int i=0;i<dataSets.size();i++){
					boolean dataExists = false;
					String dsPath = Configuration.homeDir+"/temp_cvc"+gd.getFilePath()+"/"+dataSets.get(i);
					ArrayList <String> copyFileList=new ArrayList<String>();
					ArrayList <String> copyFilesWithFk = new ArrayList<String>();
					Pattern pattern = Pattern.compile("^DS([0-9]+)$");
					Matcher matcher = pattern.matcher(dataSets.get(i));
					int dsId = 1;
					
					if (matcher.find()) {
						dsId = Integer.parseInt(matcher.group(1));
					}					
					String cvcPath = Configuration.homeDir+"/temp_cvc"+gd.getFilePath()+"/cvc3_"+dsId+".cvc";
					File ds=new File(dsPath);		 	
					String copyFiles[] = ds.list();
					String datasetvalue="",st="";
					if(copyFiles != null && copyFiles.length==0){
						continue;
					}else if(copyFiles == null){
						continue;
					}
					ArrayList <DataSetValue> dsList = new ArrayList<DataSetValue>();
					String line="";
					String tag="";
					try(BufferedReader b = new BufferedReader(new FileReader(cvcPath))){
						
						String substr = "%MUTATION TYPE:";
						while ((line = b.readLine()) != null) {
							   if(line.startsWith(substr)){
								   tag=line.substring(line.lastIndexOf(substr) + substr.length()).trim();
								   break;
							   }
						}
					}
					
					for(int j=0;j<copyFiles.length;j++){
					//	if(copyFiles[j].contains(".ref")){
						//	copyFileList.add(copyFiles[j].substring(0,copyFiles[j].indexOf(".ref")));
						//}else{
							copyFileList.add(copyFiles[j].substring(0,copyFiles[j].indexOf(".copy")));
						//}
					}
					 /**Delete existing entries in Temp tables **/
					int size = tableMap.foreignKeyGraph.topSort().size();
					for (int fg=(size-1);fg>=0;fg--){
						String tableName = tableMap.foreignKeyGraph.topSort().get(fg).toString();
							String del="delete from "+tableName;
							try(PreparedStatement stmt=testCon.prepareStatement(del)){
							try{
								stmt.executeUpdate();
								
							}catch(Exception e){
								logger.log(Level.FINE," Contraint violated ERROR:" + del+"/n while inserting datasets");
								//e.printStackTrace();
							}finally{
								
								stmt.close();
							}
							}
					}
					//This part helps in identifying the order of foreign key dependence and helps in
					//populating the data accordingly.
					for(int f=0;f<tableMap.foreignKeyGraph.topSort().size();f++){
						String tableName = tableMap.foreignKeyGraph.topSort().get(f).toString();
						String tName="";
						if(copyFileList.contains(tableName) || copyFileList.contains(tableName+".ref")){
							if(copyFileList.contains(tableName+".ref")){
								tName = tableName+".ref";
							}else
								tName = tableName;
							 DataSetValue dsValue = new DataSetValue();
							 dsValue.setFilename(tName+".copy");
							 copyFilesWithFk.add(tName+".copy");
							 BufferedReader br = new BufferedReader(new FileReader(dsPath+"/"+tName+".copy"));
							 
							 while((st=br.readLine())!=null){
								 	String row=st.replaceAll("\\|", "','");
									String insert="insert into "+tableName+" Values ('"+row+"')";
									
								try(PreparedStatement inst=testCon.prepareStatement(insert)){
									try{
										inst.executeUpdate();
										//If constraint not violated, that means the record is encountered first time
										dsValue.addData(st);	
									}catch(Exception e){
										//If exception occurs, then this is duplicate column
										logger.log(Level.FINE," Contraint violated ERROR:" + inst+"/n while inserting datasets");
										//e.printStackTrace();
								} finally{
										inst.close();
									}
									}
							 } 
							 dsList.add(dsValue);
							 br.close();
						}
						
					} 
					
					for(int j=0;j<copyFiles.length;j++){
						//If the copy file (table name) is not in foreign key graph
						
						String copyFileName = copyFiles[j];
						if(copyFilesWithFk.contains(copyFileName)){
							continue;
						}else{
							//Check for primary keys constraint and add the data to avoid duplicates
							DataSetValue dsValue = new DataSetValue();
							dsValue.setFilename(copyFileName);
							String tname =copyFileName.substring(0,copyFileName.indexOf(".copy"));
							 
							 BufferedReader br = new BufferedReader(new FileReader(dsPath+"/"+copyFileName));
							 while((st=br.readLine())!=null){
								 
								 String row=st.replaceAll("\\|", "','");
									String insert="insert into "+tname+" Values ('"+row+"')";
									
										try(PreparedStatement inst=testCon.prepareStatement(insert)){
											try{
												inst.executeUpdate();
												//If constraint not violated, that means the record is encountered first time
												//or table has no primary key
												dsValue.addData(st);	
											}catch(Exception e){
												//If exception occurs, then this is duplicate column
												logger.log(Level.FINE," Contraint violated ERROR:" + inst+"/n while inserting datasets");
												e.printStackTrace();
											} finally{
												inst.close();
											}
										}
								// dsValue.addData(st);
							 }
							  dsList.add(dsValue);
								br.close();
						}
					}
					  
					Type listType =new TypeToken<ArrayList<DataSetValue>>() {
		            }.getType();
		          
		            
					String json = gson.toJson(dsList);
					datasetid="DS"+(dsId +maxid);
					
					//Save this to DB and process this in show data generated
				
					  //Once the datasets are loaded to temp tables, run instructor query and save the result
		            //For 'showGeneratedDataSet' UI display - start
					
					//to hold the column names for display 
					ArrayList <FailedColumnValues> failedList = new ArrayList<FailedColumnValues>();
					//to hold the output values for each column
					Map <String,ArrayList<String>> instrColMap = new LinkedHashMap<String,ArrayList<String>>();
					Map <String,Map<String,ArrayList<String>>> instrDataMap = new HashMap<String,Map<String,ArrayList<String>>>();
					String instrQuery = "";
					
					try(PreparedStatement smt = conn.prepareStatement("select sql from xdata_instructor_query where assignment_id=? and question_id=? and query_id=? and course_id=?")){
						smt.setInt(1, assignmentId);
						smt.setInt(2, questionId);
						smt.setInt(3, queryId);
						smt.setString(4,course_id);
						try(ResultSet rset= smt.executeQuery()){
							if(rset.next()){
								instrQuery = rset.getString("sql");
							}
						}
						
					}
					System.out.println("instrQuery = "+ instrQuery);
					if(instrQuery != null){
						TestAnswer testAns = new TestAnswer();
			
						//If there are two columns with same name like count(distinct col_name) and count(col_name)
						// output will have col name as count for both - to use this as key, suffix with index 1,2,.,
						// and use that as key in map to store output values for the column.
						List<String> existingColNames = new ArrayList<String>();  
						int index = 1;
					try(PreparedStatement pp=testCon.prepareStatement(instrQuery)){
						try(ResultSet rr=pp.executeQuery()){
							ResultSetMetaData metadata = rr.getMetaData();
							int no_of_columns=metadata.getColumnCount();
							String result="";
							String columnName = "";
							//ArrayList <String> values = new ArrayList<String>();
							
							//put all col names in a list. Then whn columns is encountered, delete frm list
							//if col name still exists, then there is duplicate col name - suffix with index
							for(int cl=1;cl<=no_of_columns;cl++)
							{
								existingColNames.add(metadata.getColumnName(cl));
							}
							for(int cl=1;cl<=no_of_columns;cl++)
							{
								 ArrayList <String> values = new ArrayList<String>();
								FailedColumnValues failedColumns = new FailedColumnValues();
								
								existingColNames.remove(metadata.getColumnName(cl));
								
								//After removing , if still coName exists it is duplicate column- so suffix with index.
								if(existingColNames.contains(metadata.getColumnName(cl))){
									
									columnName = metadata.getColumnName(cl)+index;
									index ++;
									existingColNames.add(metadata.getColumnName(cl));
									
								}else{
									columnName = metadata.getColumnName(cl);
									
								}
								
								try(ResultSet rr1=pp.executeQuery()){
								 metadata = rr1.getMetaData();
								while(rr1.next())
								{
									int type = metadata.getColumnType(cl);
									values.add(rr1.getString(cl));
									
								}	
								
								failedColumns.setInstrColumnName(metadata.getColumnName(cl));
								failedColumns.setInstrValues(values);
								instrColMap.put(columnName,values);
								failedList.add(failedColumns);
							}
								}
								
						}
					}catch(Exception e){
						logger.log(Level.FINE," WriteFileAndUploadDataSets:" + e.getMessage(),e);
					}
					
					instrDataMap.put(datasetid, instrColMap);
					instrDs.getInstrQueryOutput().put(datasetid, instrColMap);
					instrDs.getDsValueMap().put(datasetid,failedList);
					}
					
					try(PreparedStatement smt = conn.prepareStatement(existingDataSets)){
												
						try(ResultSet rs =smt.executeQuery()){
							while(rs.next()){
								
								String existingDataSetId = rs.getString("datasetid");
								if(datasetid.equalsIgnoreCase(existingDataSetId)){
									dataExists = true;
									break;
								}
							}
						}
					}
					if(dataExists){
						String insertquery="update xdata_datasetvalue set datasetid=?,value=? where assignment_id=? and question_id=? and query_id=? and course_id=? and datasetid=?";
						try(PreparedStatement smt = conn.prepareStatement(insertquery)){
								smt.setString(1,datasetid);
								smt.setString(2,json);
								smt.setInt(3,assignmentId);
								smt.setInt(4,questionId);
								smt.setInt(5,queryId);
								smt.setString(6, course_id);
								smt.setString(7,datasetid);
								
								smt.executeUpdate(); 			 	
						}
					} 
					if(!dataExists){
					//For 'showGeneratedDataSet' UI display - End
					String insertquery="INSERT INTO xdata_datasetvalue VALUES ('"+queryId+"','"+datasetid+"','"+json+"','"+tag+"','"+assignmentId+"','"+questionId+"','"+queryId+"','"+course_id+"')";
										
					try(PreparedStatement smt = conn.prepareStatement(insertquery)){
						smt.executeUpdate(); 			 	
					}
					}
				}//for each dataSet loop ends
				
				json1 = gson.toJson(instrDs);		
				
				String update="update xdata_instructor_query set resultondataset=? where assignment_id=? and question_id=? and query_id=? and course_id=?";
				
				try(PreparedStatement smt = conn.prepareStatement(update)){
					smt.setString(1,json1);
					smt.setInt(2, assignmentId);
					smt.setInt(3, questionId);
					smt.setInt(4, queryId);
					smt.setString(5,course_id);
					smt.executeUpdate(); 			 	
				}
	}//try-with-resources - Connection testCon ends
	}//try-with-resources -Connection conn ends 
}
}
