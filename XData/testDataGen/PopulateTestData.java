package testDataGen;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runtime;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import parsing.*;
import util.DataSetValue;
import util.*;

/*class CallableProcess implements Callable {
    private Process p;
    private String filePath="";
    private String cvcFileName="";

    public CallableProcess(Process pr){
    	p=pr;
    }
    public  CallableProcess(String fp,String fileName) {
    	filePath = fp;
    	cvcFileName = fileName;
       // p = process;
    }

    public Integer call() throws Exception {
    	  //Executing the CVC file generated for given query
		Runtime r = Runtime.getRuntime();
		Process myProcess = r.exec(Configuration.smtsolver+" "+Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcFileName);
		int ch;
	    InputStreamReader myIStreamReader = new InputStreamReader(myProcess.getInputStream());

		//Writing output to .out file
		BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcFileName.substring(0,cvcFileName.lastIndexOf(".cvc")) + ".out"));

		while ((ch = myIStreamReader.read()) != -1) 
		{ 
			out.write((char)ch); 
		} 	
		Utilities.closeProcessStreams(myProcess);

		out.close(); 

		return myProcess.waitFor();
    }
}*/


class CallableProcess implements Callable {
	private Process p;

	public CallableProcess(Process process) {
		p = process;
	}

	@Override
	public Integer call() throws Exception {
		return p.waitFor();
	}
}

public class PopulateTestData {

	private static Logger logger = Logger.getLogger(PopulateTestData.class.getName());

	public String getParameterMapping(HashMap<String,Node> paramConstraints, HashMap<String, String> paramMap){

		String retVal = "------------------------\nPARAMETER MAPPING\n------------------------\n";
		Iterator itr = paramConstraints.keySet().iterator();
		retVal += paramMap.toString() + "\n\n";
		while(itr.hasNext()){
			String key = (String)itr.next();
			retVal += "CONSTRAINT: "+paramConstraints.get(key)+"\n";
		}

		return retVal;
	}

	public void fetchAndPopulateTestDatabase(Connection dbcon, Connection testCon, int assignment_id,int question_id, int query_id, String course_id, String dataset_id,TableMap tableMap) throws Exception{

		String dataset_query="select value from xdata_datasetvalue where datasetid =? and assignment_id=? and question_id=? and query_id=? and course_id = ?";
		String dataset= "";

		PreparedStatement dstmt = dbcon.prepareStatement(dataset_query);
		dstmt.setString(1,dataset_id);
		dstmt.setInt(2, assignment_id);
		dstmt.setInt(3,question_id);
		dstmt.setInt(4,query_id);
		dstmt.setString(5,course_id);
		logger.log(Level.FINE,"Dataset_id is :"+dataset_id);
		logger.log(Level.FINE,"Query_id is :"+query_id);
		ResultSet dset=dstmt.executeQuery();
		try{
			while(dset.next()){
				dataset = dset.getString("value");

				Map<String , ArrayList> tables=new HashMap<String , ArrayList>();
				Gson gson = new Gson();
				//ArrayList dsList = gson.fromJson(value,ArrayList.class);
				Type listType = new TypeToken<ArrayList<DataSetValue>>() {
				}.getType();
				List<DataSetValue> dsList = new Gson().fromJson(dataset, listType);
				logger.log(Level.INFO,"dsList.size() = "+ dsList.size());
				for(int i = 0 ; i < dsList.size();i++ ){
					DataSetValue dsValue = dsList.get(i);
					String tname,values; 
					if(dsValue.getFilename().contains(".ref.")){
						tname = dsValue.getFilename().substring(0,dsValue.getFilename().indexOf(".ref.copy"));
					}else{
						tname = dsValue.getFilename().substring(0,dsValue.getFilename().indexOf(".copy"));
					}
					logger.log(Level.FINE,"table String:::::::::::::::::::::::::::"+tname);
					//for(String dsv: dsValue.getDataForColumn()){
					tables.put(tname, dsValue.getDataForColumn());	
					//}
				}

				int size = tableMap.foreignKeyGraph.topSort().size();
				for (int i=(size-1);i>=0;i--){
					String tableName = tableMap.foreignKeyGraph.topSort().get(i).toString();
					String del="delete from "+tableName;
					logger.log(Level.FINE,"DELETE::::::::::::::::::::::::"+del);
					PreparedStatement stmt=testCon.prepareStatement(del);
					try{
						stmt.executeUpdate();
					}catch(Exception e){
						logger.log(Level.SEVERE,"PopulateTestData.fetchAndPopulateTestDatabase -> ERROR:" + del,e);
						//logger.log(Level.SEVERE, ""+e.getStackTrace(),e);
						//e.printStackTrace();
					}finally{
						stmt.close();
					}
				}
				//If tables ontains foreign key relation they will be available in foreignKeyGraph
				for(int i=0;i<tableMap.foreignKeyGraph.topSort().size();i++){
					String tableName = tableMap.foreignKeyGraph.topSort().get(i).toString();
					if(tables.containsKey(tableName)){
						ArrayList <String> value=tables.get(tableName);

						for(String column: value)
						{
							String row=column.replaceAll("\\|", "','");
							String insert="insert into "+tableName+" Values ('"+row+"')";
							logger.log(Level.FINE,"Insert statement:::::::::::::::::::::::"+insert);

							PreparedStatement inst=testCon.prepareStatement(insert);
							try{
								inst.executeUpdate();
								tables.remove(tableName);
							}catch(Exception e){
								logger.log(Level.INFO,"PopulateTestData.fetchAndPopulateTestDatabase -> ERROR:" + insert);
								//logger.log(Level.SEVERE,""+e.getStackTrace(),e);
								//e.printStackTrace();
							}finally{
								inst.close();
							}
						}
					} 
				} 
				//Tables that are not in foreign key relation, just insert without any checks
				//Shree added this for relations not having foreign key
				Iterator it = tables.entrySet().iterator();
				while(it.hasNext()){

					java.util.Map.Entry<String,ArrayList> ent= (Map.Entry<String,ArrayList>)it.next();
					String tableName  = ent.getKey();
					ArrayList <String> value=ent.getValue();
					for(String column: value)
					{
						String row=column.replaceAll("\\|", "','");
						String insert="insert into "+tableName+" Values ('"+row+"')";
						logger.log(Level.INFO,"Insert statement:::::::::::::::::::::::"+insert);

						PreparedStatement inst=testCon.prepareStatement(insert);
						try{
							inst.executeUpdate(); 
							it.remove();//remove(tableName);
						}catch(Exception e){ 
							logger.log(Level.INFO,"PopulateTestData.fetchAndPopulateTestDatabase -> ERROR:" + insert);
							//logger.log(Level.SEVERE,""+e.getStackTrace(),e);
							//e.printStackTrace();
						}finally{
							inst.close();
						}
					}
				}


			}
		}catch(Exception e){
			logger.log(Level.SEVERE,""+e.getStackTrace(),e);
			dbcon.close();
			testCon.close();
			//e.printStackTrace();
			throw e;
		}finally{
			dset.close();
			dstmt.close();


		}
	}

	public void fetchAndPopulateTestDatabase(int assignment_id, int question_id, int query_id, String course_id, String dataset_id,TableMap tableMap) throws Exception{
		//Connection dbcon = MyConnection.getExistingDatabaseConnection();
		//Connection testCon = MyConnection.getTestDatabaseConnection();

		try(Connection dbcon = MyConnection.getDatabaseConnection()){
			try(Connection testCon = new DatabaseConnection().getTesterConnection(assignment_id)){
				fetchAndPopulateTestDatabase(dbcon, testCon, assignment_id, question_id,query_id,course_id, dataset_id, tableMap);
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"PopulateTestData Class:  "+e.getStackTrace(),e);
		}

	}


	public void captureACPData(String cvcFileName, String filePath, HashMap<String, Node> constraintsWithParams, HashMap<String, String> paramMap) throws Exception{
		String outputFileName = generateCvcOutput(cvcFileName, filePath);
		String copystmt = "";
		BufferedReader input =null;
		try{
			input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/" + outputFileName));
			String line = null; 
			File ACPFile = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/" + "PARAMETER_VALUES");
			if(!ACPFile.exists()){
				ACPFile.createNewFile();
			}
			copystmt = getParameterMapping(constraintsWithParams, paramMap);
			copystmt += "\n\n------------------------\nINSTANTIATIONS\n------------------------\n";
			setContents(ACPFile, copystmt+"\n", false);
			while (( line = input.readLine()) != null){
				if(line.contains("ASSERT (PARAM_")){//Output value for a parameterised aggregation
					String par = line.substring(line.indexOf("(PARAM_")+1, line.indexOf('=')-1);
					String val = line.substring(line.indexOf('=')+1,line.indexOf(')'));
					val = val.trim();
					copystmt = par + " = " + val;
					setContents(ACPFile, copystmt+"\n", true);
					//Now update the param map
					Iterator itr = paramMap.keySet().iterator();
					while(itr.hasNext()){
						String key = (String)itr.next();
						if(paramMap.get(key).equalsIgnoreCase(par)){
							paramMap.put(key, val);
						}
					}
				}				
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"PopulateTestData class - captureACPData Function:  "+e.getStackTrace(),e);
		}

		finally{
			if(input != null)
				input.close();
		}
	}


	public String generateCvcOutput(String cvcFileName, String filePath) throws Exception{
		int ch;
		try{
			//Executing the CVC file generated for given query
			Runtime r = Runtime.getRuntime();
			String[] smtCommand = new String[2];
			smtCommand[0] = Configuration.smtsolver;
			smtCommand[1] = Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcFileName;
			Process myProcess = r.exec(smtCommand);	

			ExecutorService service = Executors.newSingleThreadExecutor();

			try {


				InputStreamReader myIStreamReader = new InputStreamReader(myProcess.getInputStream());

				//Writing output to .out file
				BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcFileName.substring(0,cvcFileName.lastIndexOf(".cvc")) + ".out"));
				Callable<Integer> call = new CallableProcess(myProcess);
				Future<Integer> future = service.submit(call);
				int exitValue = future.get(60, TimeUnit.SECONDS);		    

				while ((ch = myIStreamReader.read()) != -1) 
				{ 
					out.write((char)ch); 
				} 	
				Utilities.closeProcessStreams(myProcess);

				out.close();

			} catch (ExecutionException e) {

				logger.log(Level.SEVERE,"ExecutionException in generateCvcOutput");
				Utilities.closeProcessStreams(myProcess);
				throw new Exception("Process failed to execute", e);
			} catch (TimeoutException e) {
				logger.log(Level.SEVERE,"TimeOutException in generateCvcOutput");
				Utilities.closeProcessStreams(myProcess);
				myProcess.destroy();		    	
				throw new Exception("Process timed out", e);
			} finally {
				service.shutdown();
			}			

		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			//e.printStackTrace();
			throw new Exception("Process interrupted or timed out.", e);
		}

		return cvcFileName.substring(0,cvcFileName.lastIndexOf(".cvc")) + ".out";
	}



	public String cutRequiredOutput(String cvcOutputFileName, String filePath){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(Configuration.homeDir+"/temp_cvc"+filePath+"/cut_" + cvcOutputFileName));
			out.close();
			File testFile = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/cut_" + cvcOutputFileName);
			BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cvcOutputFileName));
			try {
				String line = null; 
				while (( line = input.readLine()) != null){
					if(line.contains("ASSERT (O_") && line.contains("] = (") && !line.contains("THEN")){
						setContents(testFile, line+"\n", true);
					}
				}
			}catch(Exception e){
				logger.log(Level.SEVERE,"PopulateTestData-cutRequiredOutput :  "+e.getStackTrace(),e);
			}
			finally {
				input.close();
			}
		}
		catch (IOException ex){
			logger.log(Level.SEVERE,"PopulateTestData-cuteRequiredOutput :  "+ex.getMessage(),ex);
			//ex.printStackTrace();
		}
		return "cut_"+cvcOutputFileName;
	}

	public void setContents(File aFile, String aContents, boolean append)throws FileNotFoundException, IOException {
		if (aFile == null) {
			logger.log(Level.WARNING,"PopulateTesData.setContents : File is null");
			throw new IllegalArgumentException("File should not be null.");
		}
		if (!aFile.exists()) {
			logger.log(Level.WARNING,"PopulateTesData.setContents : File does not exists");

			throw new FileNotFoundException ("File does not exist: " + aFile);
		}
		if (!aFile.isFile()) {
			logger.log(Level.WARNING,"PopulateTesData.setContents : Should not be a directory");

			throw new IllegalArgumentException("Should not be a directory: " + aFile);
		}
		if (!aFile.canWrite()) {
			logger.log(Level.WARNING,"PopulateTesData.setContents : File Cannot be written");

			throw new IllegalArgumentException("File cannot be written: " + aFile);
		}

		Writer output = new BufferedWriter(new FileWriter(aFile,append));
		try {
			output.write( aContents );
		}
		catch(Exception e){
			logger.log(Level.SEVERE,"PopulateTestData.setContents(): "+e.getStackTrace(),e);
		}
		finally {
			output.flush();
			output.close();
		}
	}

	//Modified by Bhupesh
	public Vector<String> generateCopyFile (String cut_cvcOutputFileName, String filePath, 
			HashMap<String, Integer> noOfOutputTuples, TableMap tableMap,Vector<Column> columns,
			Set existingTableNames) throws Exception {
		Vector<String> listOfCopyFiles = new Vector();
		List <String> copyFileContents = new ArrayList<String>(); 
		String currentCopyFileName = "";
		File testFile = null;
		BufferedReader input = null;
		try{
			input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/" + cut_cvcOutputFileName));
			String line = null,copystmt=null; 
			while (( line = input.readLine()) != null){
				String tableName = line.substring(line.indexOf("_")+1,line.indexOf("["));
				if(!noOfOutputTuples.containsKey(tableName)){
					continue;
				}
				int index = Integer.parseInt(line.substring(line.indexOf('[')+1, line.indexOf(']')));
				if((index > noOfOutputTuples.get(tableName)) || (index <= 0)){
					continue;
				}
				currentCopyFileName = line.substring(line.indexOf("_")+1,line.indexOf("["));
				//Shree added to show 'tables in query' and 'reference tables' separately
				if( !(existingTableNames.contains(currentCopyFileName.toUpperCase()))){
					//If table name is not in existingTablename Set, it means it is a reference Table
					currentCopyFileName = currentCopyFileName+".ref";
				}
				testFile = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/" + currentCopyFileName + ".copy");
				if(!testFile.exists() || !listOfCopyFiles.contains(currentCopyFileName + ".copy")){
					if(testFile.exists()){
						testFile.delete();
					}
					testFile.createNewFile();
					listOfCopyFiles.add(currentCopyFileName + ".copy");
				}
				copystmt = getCopyStmtFromCvcOutput(line);

				copyFileContents.add(copystmt);
				////Putting back string values in CVC

				Table t=tableMap.getTable(tableName);

				String[] copyTemp=copystmt.split("\\|");
				copystmt="";
				String out="";

				for(int i=0;i<copyTemp.length;i++){

					String cvcDataType=t.getColumn(i).getCvcDatatype();
					if(cvcDataType.equalsIgnoreCase("INT") )
						continue;
					else if(cvcDataType.equalsIgnoreCase("REAL")){
						String str[]=copyTemp[i].trim().split("/");
						if(str.length==1)
							continue;
						double num=Integer.parseInt(str[0]);
						double den=Integer.parseInt(str[1]);
						copyTemp[i]=(num/den)+"";
					}
					else if(cvcDataType.equalsIgnoreCase("TIMESTAMP")){
						long l=Long.parseLong(copyTemp[i].trim())*1000;
						java.sql.Timestamp timeStamp=new java.sql.Timestamp(l);
						copyTemp[i]=timeStamp.toString();
					}
					else if(cvcDataType.equalsIgnoreCase("TIME")){

						int time=Integer.parseInt(copyTemp[i].trim());
						int sec=time%60;
						int min=((time-sec)/60)%60;
						int hr=(time-sec+min*60)/3600;
						copyTemp[i]=hr+":"+min+":"+sec;
					}
					else if(cvcDataType.equalsIgnoreCase("DATE")){
						long l=Long.parseLong(copyTemp[i].trim())*86400000;

						java.sql.Date date=new java.sql.Date(l);
						copyTemp[i]=date.toString();

					}
					else {

						String copyStr=copyTemp[i].trim();


						if(copyStr.endsWith("__"))
							copyStr = "";
						else if(copyStr.contains("__"))
							copyStr = copyStr.split("__")[1];


						/*&copyStr = copyStr.replace("_p", "+");
					copyStr = copyStr.replace("_m", "-");
					copyStr = copyStr.replace("_a", "&");
					copyStr = copyStr.replace("_s", " ");
					copyStr = copyStr.replace("_d", ".");
					copyStr = copyStr.replace("_c", ",");
					copyStr = copyStr.replace("_u", "_");*/

						copyStr = copyStr.replace("_p", "%");
						copyStr = copyStr.replace("_s", "+");
						copyStr = copyStr.replace("_d", ".");
						copyStr = copyStr.replace("_m", "-");
						copyStr = copyStr.replace("_s", "*");
						copyStr = copyStr.replace("_u", "_");
						copyStr = URLDecoder.decode(copyStr,"UTF-8");
						copyTemp[i]=copyStr.replace("_b", " ");

					}


				}
				for(String s:copyTemp){
					copystmt+=s+"|";
				}
				copystmt=copystmt.substring(0, copystmt.length()-1);



				setContents(testFile, copystmt+"\n", true);
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"PopulateTestData.generateCopyFile() : "+e.getStackTrace(),e);
		}
		finally{
			if(input != null)
				input.close();
		}
		return listOfCopyFiles;
	}

	public String getCopyStmtFromCvcOutput(String cvcOutputLine){
		String queryString = "";
		String tableName = cvcOutputLine.substring(cvcOutputLine.indexOf("_")+1,cvcOutputLine.indexOf("["));
		String temp = cvcOutputLine.substring(cvcOutputLine.indexOf("(")+1);
		String insertTupleValues = temp.substring(temp.indexOf("(")+1,temp.indexOf(")"));
		insertTupleValues = cleanseCopyString(insertTupleValues);		
		return insertTupleValues;
	}

	public String cleanseCopyString(String copyStr){

		copyStr = copyStr.replaceAll("\\b_", "");
		copyStr = copyStr.replaceAll("\\bNULL_\\w+", "");
		copyStr = copyStr.replaceAll("\\-9999[6789]", "");
		copyStr = copyStr.replace(",", "|");

		return copyStr;
	}

	public void populateTestDataForTesting(Vector<String> listOfCopyFiles, String filePath, TableMap tableMap, Connection conn, int assignmentId, int questionId){
		try{						
			deleteAllTempTablesFromTestUser(conn);
			//deleteAllTablesFromTestUser(conn);
			this.createTempTables(conn, assignmentId, questionId);
			BufferedReader br = null;

			for(int i=0;i<tableMap.foreignKeyGraph.topSort().size();i++){
				try{
					String tableName = tableMap.foreignKeyGraph.topSort().get(i).toString();
					if(listOfCopyFiles.contains(tableName+".copy")){
						listOfCopyFiles.remove(tableName+".copy");
						String copyFile = tableName+".copy";

						br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
						String str;
						String data="";
						while((str = br.readLine())!=null){
							data+=str+"@@";
						}

						uploadTestDataToTempTables(copyFile.substring(0, copyFile.indexOf(".copy")), data, filePath, conn);
					}else if(listOfCopyFiles.contains(tableName+".ref.copy")){
						listOfCopyFiles.remove(tableName+".ref.copy");
						String copyFile = tableName+".ref.copy";
						br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
						String str;
						String data="";
						while((str = br.readLine())!=null){
							data+=str+"@@";
						}

						uploadTestDataToTempTables(copyFile.substring(0, copyFile.indexOf(".ref.copy")), data, filePath, conn);
					}
				}catch(Exception e){
					logger.log(Level.SEVERE,"PopulateTestData.populateTestDataForTesting(): "+e.getStackTrace(),e);
				}finally{
					if(br != null){
						br.close();
					}
				}

			}
			for(int i=0;i<listOfCopyFiles.size();i++){
				try{
					String copyFile = listOfCopyFiles.get(i);
					br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+copyFile));
					String str;
					String data="";
					while((str = br.readLine())!=null){
						data+=str+"@@";
					}

					uploadTestDataToTempTables(copyFile.substring(0,copyFile.indexOf(".copy")), data, filePath, conn);
				}catch(Exception e){
					logger.log(Level.SEVERE,"PopulateTestData.populateTestDataForTesting(): "+e.getStackTrace(),e);
				}
				finally{
					if(br != null)
						br.close();
				}
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"PopulateTestData.populateTestDataForTesting: "+e.getStackTrace(),e);
			//e.printStackTrace();
		}finally{

			//conn.close();

		}


	}


	public void uploadTestDataToTempTables(String tablename,String copyFileData,String filePath, Connection conn) throws Exception{			
		String t[]=copyFileData.split("@@");
		for(int i=0;i<t.length;i++){			
			t[i]=t[i].replaceAll("\\|", "','");

			PreparedStatement smt=conn.prepareStatement("Insert into "+ tablename+" Values ('"+t[i]+"')");
			try{
				smt.executeUpdate();

			}catch(Exception e){
				//logger.log(Level.SEVERE,"PopulateTestData:uploadTestDataToTempTables->Error in "+tablename+"Insert into "+ tablename+" Values ('"+t[i]+"')",e);
				//logger.log(Level.SEVERE,e.getMessage(),e);
				//e.printStackTrace();
			}finally{
				smt.close();
			}

		}				
	}

	public void deleteAllTempTablesFromTestUser(Connection dbConn) throws Exception{
		Statement st = dbConn.createStatement();
		st = dbConn.createStatement();
		st.executeUpdate("DISCARD TEMPORARY");
		st.close();
	}

	public void deleteAllTablesFromTestUser(Connection conn) throws Exception{
		try{
			DatabaseMetaData dbm = conn.getMetaData();
			String[] types = {"TEMPORARY TABLE"};
			ResultSet rs = dbm.getTables(conn.getCatalog(), null, "%", types);		  

			while(rs.next()){
				String table=rs.getString("TABLE_NAME");		
				if(!table.equalsIgnoreCase("dataset") 
						&& !table.equalsIgnoreCase("xdata_temp1")
						&& !table.equalsIgnoreCase("xdata_temp2")){
					//PreparedStatement pstmt = conn.prepareStatement("delete from "+table);						
					PreparedStatement pstmt = conn.prepareStatement("Truncate table "+table +" cascade");
					pstmt.executeUpdate();
					pstmt.close();
				}

			} 

			rs.close();
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
		}

	}

	public void deleteDatasets(String filePath) throws Exception{
		Runtime r = Runtime.getRuntime();
		File f=new File(Configuration.homeDir+"/temp_cvc"+filePath+"/");
		File f2[]=f.listFiles();
		for(int i=0;i<f2.length;i++){
			if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
				Utilities.deletePath(Configuration.homeDir+"/temp_cvc"+filePath+"/"+f2[i].getName());
			}				
		}
	}

	//TODO:Refractor this method name and input prarameters. 
	//This is no longer used to check how many mutants were killed
	/**
	 * Executes CVC3 Constraints specified in the file "cvcOutputFileName"
	 * Stores the data set values inside the directory "datasetName"
	 * @param cvcOutputFileName
	 * @param query
	 * @param datasetName
	 * @param queryString
	 * @param filePath
	 * @param noOfOutputTuples
	 * @param tableMap
	 * @param columns
	 * @return
	 * @throws Exception 
	 */
	public boolean killedMutants(String cvcOutputFileName, Query query, String datasetName, String queryString, String filePath, 
			HashMap<String, Integer> noOfOutputTuples, TableMap tableMap,Vector<Column> columns, int assignmentId, int questionId, Set existingTableNames) throws Exception{
		String temp=""; 
		Process proc=null;
		boolean returnVal=false;
		String test = generateCvcOutput(cvcOutputFileName, filePath);
		BufferedReader br =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+filePath+"/"+test));
		String str = br.readLine();
		br.close();
		if((str == null || str.equals("") || str.equalsIgnoreCase("Valid."))) {
			return false;
		}

		String cutFile = cutRequiredOutput(test, filePath);
		Vector<String> listOfCopyFiles = generateCopyFile(cutFile, filePath, noOfOutputTuples, tableMap,columns,existingTableNames);			
		Vector<String> listOfFiles = (Vector<String>) listOfCopyFiles.clone();

		Runtime r = Runtime.getRuntime();

		//Process proc = r.exec("sh "+Configuration.scriptsDir+"/dir.sh "+datasetName+" "+filePath );			
		


		File datasetDir = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetName);
		boolean created = datasetDir.mkdirs();
		if(!created) {
			logger.log(Level.WARNING, "Could not create directory for dataset: "+datasetDir.getPath());
		}
		
		for(String i:listOfCopyFiles){				
			File src = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/"+i);
			File dest = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetName+"/"+i);
			Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		}	
		//Connection conn = MyConnection.getTestDatabaseConnection();
		try(Connection conn = new DatabaseConnection().getTesterConnection(assignmentId)){
			populateTestDataForTesting(listOfCopyFiles, filePath, tableMap,conn, assignmentId, questionId);

			for(String i : listOfFiles){				
				File src = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/"+i);
				File dest = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetName+"/"+i);
				Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			returnVal=true;

			//comment the two lines below if you do not want to measure how many mutants have been killed
			/*Vector<Mutant> mutants = generateJoinMutants(query);
				temp = mutantsKilledByDataset(mutants,datasetName, queryString, filePath);*/

		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			//e.printStackTrace();
			throw new Exception("Process exited", e);
		} finally {

			if (proc!=null)
				Utilities.closeProcessStreams(proc);
		}
		return returnVal;			
	}

	public void createTempTables(Connection conn, int assignId, int questionId) throws Exception {
		//Connection mainConn = MyConnection.getExistingDatabaseConnection();
		try(Connection mainConn = MyConnection.getDatabaseConnection()){	
			int schemaId = 0, optionalSchemaId=0;			

			try(PreparedStatement stmt = mainConn.prepareStatement("select defaultschemaid from xdata_assignment where assignment_id = ?")){
				stmt.setInt(1, assignId); 

				try(ResultSet result = stmt.executeQuery()){

					//Get optional Schema Id for this question
					try(PreparedStatement stmt1 = mainConn.prepareStatement("select optionalschemaid from xdata_qinfo where assignment_id = ? and question_id= ? ")){
						stmt1.setInt(1, assignId); 
						stmt1.setInt(2, questionId); 

						try(ResultSet resultSet = stmt1.executeQuery()){
							if(resultSet.next()){
								optionalSchemaId = resultSet.getInt("optionalschemaid");
							}
						}
					}
					if(result.next()){
						//If optional schema id exists and it is not same as default schema id, then set it as schemaId 
						if(optionalSchemaId != 0 && optionalSchemaId != result.getInt("defaultschemaid")){	
							schemaId = optionalSchemaId;
						} else{
							schemaId = result.getInt("defaultschemaid");
						}
					}

					if(schemaId != 0){				
						try(PreparedStatement stmt1 = mainConn.prepareStatement("select ddltext from xdata_schemainfo where schema_id = ?")){
							stmt1.setInt(1, schemaId);			
							try(ResultSet result1 = stmt1.executeQuery()){

								// Process the result			
								if(result1.next()){
									String fileContent= result1.getString("ddltext");
									byte[] dataBytes = fileContent.getBytes();
									String tempFile = "/tmp/dummy";

									FileOutputStream fos = new FileOutputStream(tempFile);
									fos.write(dataBytes);
									fos.close();

									ArrayList<String> listOfQueries = Utilities.createQueries(tempFile);
									String[] inst = listOfQueries.toArray(new String[listOfQueries.size()]);

									for (int i = 0; i < inst.length; i++) {
										// we ensure that there is no spaces before or after the request string  
										// in order to not execute empty statements  
										if (!inst[i].trim().equals("") && ! inst[i].trim().contains("drop table")) {
											String temp = inst[i].replaceAll("(?i)^[ ]*create[ ]+table[ ]+", "create temporary table ");
											try(PreparedStatement stmt2 = conn.prepareStatement(temp)){
												stmt2.executeUpdate();					
											}
										}
									}	
								}
							}//try-with-resource for ressultset result
						}//try-with-resource for stmt1		
					}	
				}//try-with-resource for ResultSet
			}//Try-with-resource for statement obj
		}//try-with-resource for Connection obj
		catch(Exception ex){
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			//ex.printStackTrace();
			throw ex;
		}


	}


	public void createTempTablesForTestThread(Connection conn, int assignId, int questionId) throws Exception {
		//Connection mainConn = MyConnection.getExistingDatabaseConnection();
		try(Connection mainConn = MyConnection.getDatabaseConnection()){	
			int schemaId = 0, optionalSchemaId=0;			

			try(PreparedStatement stmt = mainConn.prepareStatement("select defaultschemaid from xdata_assignment where assignment_id = ?")){
				stmt.setInt(1, assignId); 

				try(ResultSet result = stmt.executeQuery()){

					//Get optional Schema Id for this question
					try(PreparedStatement stmt1 = mainConn.prepareStatement("select optionalschemaid from xdata_qinfo where assignment_id = ? and question_id= ? ")){
						stmt1.setInt(1, assignId); 
						stmt1.setInt(2, questionId); 

						try(ResultSet resultSet = stmt1.executeQuery()){
							if(resultSet.next()){
								optionalSchemaId = resultSet.getInt("optionalschemaid");
							}
						}
					}
					if(result.next()){
						//If optional schema id exists and it is not same as default schema id, then set it as schemaId 
						if(optionalSchemaId != 0 && optionalSchemaId != result.getInt("defaultschemaid")){	
							schemaId = optionalSchemaId;
						} else{
							schemaId = result.getInt("defaultschemaid");
						}
					}

					if(schemaId != 0){				
						try(PreparedStatement stmt1 = mainConn.prepareStatement("select ddltext from xdata_schemainfo where schema_id = ?")){
							stmt1.setInt(1, schemaId);			
							try(ResultSet result1 = stmt1.executeQuery()){

								// Process the result			
								if(result1.next()){
									String fileContent= result1.getString("ddltext");
									byte[] dataBytes = fileContent.getBytes();
									String tempFile = "/tmp/dummy";

									FileOutputStream fos = new FileOutputStream(tempFile);
									fos.write(dataBytes);
									fos.close();

									ArrayList<String> listOfQueries = Utilities.createQueries(tempFile);
									String[] inst = listOfQueries.toArray(new String[listOfQueries.size()]);

									for (int i = 0; i < inst.length; i++) {
										// we ensure that there is no spaces before or after the request string  
										// in order to not execute empty statements  
										if (!inst[i].trim().equals("") && ! inst[i].trim().contains("drop table")) {
											String temp = inst[i].replaceAll("(?i)^[ ]*create[ ]+table[ ]+", "create temporary table ");
											try(PreparedStatement stmt2 = conn.prepareStatement(temp)){
												stmt2.executeUpdate();					
											}
										}
									}	
								}
							}//try-with-resource for ressultset result
						}//try-with-resource for stmt1		
					}	
				}//try-with-resource for ResultSet
			}//Try-with-resource for statement obj
		}//try-with-resource for Connection obj
		catch(Exception ex){
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			//ex.printStackTrace();
			throw ex;
		}


	}


	/**
	 * This method is used in TestAnswer class for evaluating the queries.
	 * If data generation fails, then this method is called while evaluation 
	 * and default sample data is loaded and is taken as dataset against which evaluation is done.
	 *  
	 * @param conn
	 * @param testConn
	 * @param assignmentId
	 * @param questionId
	 * @throws Exception
	 */
	public void createTempTableData(Connection conn, Connection testConn, int assignmentId, int questionId,String course_id) throws Exception{
		int connId = 2, schemaId = 15,optionalSchemaId=15;

		try(PreparedStatement stmt = conn.prepareStatement("select connection_id, defaultschemaid from xdata_assignment where assignment_id = ? and course_id = ?")){
			stmt.setInt(1, assignmentId);
			stmt.setString(2,course_id);
			try(ResultSet result = stmt.executeQuery()){


				//Get optional Schema Id for this question
				try(PreparedStatement statement = conn.prepareStatement("select optionalschemaid from xdata_qinfo where assignment_id = ? and question_id= ? and course_id = ?")){
					statement.setInt(1, assignmentId); 
					statement.setInt(2,questionId); 
					statement.setString(3,course_id);

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
		byte[] dataBytes = null;
		String tempFile = "";
		FileOutputStream fos = null;
		ArrayList<String> listOfQueries = null;
		String[] inst = null;
		if(connId != 0 && schemaId != 0){
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
								try(PreparedStatement stmt2 = testConn.prepareStatement(temp)){
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
								//System.out.println(inst[i]);
								try(PreparedStatement stmt2 = testConn.prepareStatement(inst[i])){
									stmt2.executeUpdate();		
								}
							}
						}

					}//try-with-resource resultset obj
				}//try-with-resource statement obj	
			}
		}

	}

	/**For the instructor given data sets during assignment creation
test student and instructor query options */
	/**
	 * First get sample data sets for question, if it is not there, then get
	 * it from assignment table
	 * If both are not there, dont set anythng
	 * @throws SQLException 
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	public String createTempTableWithDefaultData(Connection mainCon,Connection testConn,int assignmentId,
			int questionId,String course_id,String sampledata_id) throws Exception{
		String sampleDataName = "";
		byte[] dataBytes = null;
		String tempFile = "";
		FileOutputStream fos = null;
		ArrayList<String> listOfQueries = null;
		String[] inst = null;
		this.deleteAllTablesFromTestUser(testConn);
		try(PreparedStatement stmt = mainCon.prepareStatement("select sample_data_name,sample_data from xdata_sampledata where sampledata_id = ?")){
			stmt.setInt(1, Integer.parseInt(sampledata_id));			
			try(ResultSet result = stmt.executeQuery()){

				// Process the result			
				if(result.next()){
					sampleDataName = result.getString("sample_data_name");
					String sdContent= result.getString("sample_data");
					//String sdReplace=sdContent.replace("\\\\","'");
					//fc = sdReplace.replace("\t", "    ");
					tempFile = "/tmp/dummy";
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
							//System.out.println(inst[i]);
							try(PreparedStatement stmt2 = testConn.prepareStatement(inst[i])){
								stmt2.executeUpdate();		
							}
						}
					}

				}//try-with-resource resultset obj
			}//try-with-resource statement obj	
		} catch (SQLException e) {
			logger.log(Level.INFO,"------PopulateTestData - Load default data sets------");
			logger.log(Level.SEVERE,e.getMessage(),e);
			//throw e;
		}
		catch (FileNotFoundException e) {
			logger.log(Level.INFO,"------PopulateTestData - Load default data sets------");
			logger.log(Level.SEVERE,e.getMessage(),e);
			//throw e;
		}
		catch (IOException e) {
			logger.log(Level.INFO,"------PopulateTestData - Load default data sets------");
			logger.log(Level.SEVERE,e.getMessage(),e);
			//throw e;
		}
		return sampleDataName;

	}
	public void populateDataset(int assignmentId, int questionId, int query_id, String course_id, String datasetId, Connection mainConn, Connection testConn) throws Exception{
		GenerateCVC1 cvc = new GenerateCVC1();		
		cvc.initializeConnectionDetails(assignmentId, questionId, 1,course_id);
		TableMap tm = cvc.getTableMap();
		this.fetchAndPopulateTestDatabase(mainConn, testConn, assignmentId, questionId, query_id, course_id,datasetId, tm);
		cvc.closeConn();
	}

	public static void entry(String args[]) throws Exception{

		String datasetid=args[0];
		String assignment_id=args[1];
		String question_id = args[2];
		int questionId=Integer.parseInt(question_id);
		String course_id = args[3];
		//String questionid = "A"+assignment_id+"Q"+question_id+"S"+queryid;
		int query_id=1;
		if(args.length>3){
			query_id=Integer.parseInt(args[2]);

		}

		logger.log(Level.INFO,"------PopulateTestData-----entry()------");
		logger.log(Level.INFO,"Datasetid :"+datasetid);
		logger.log(Level.INFO,"QuestionId :"+query_id);
		GenerateCVC1 cvc = new GenerateCVC1();
		int assignId = Integer.parseInt(assignment_id);

		cvc.initializeConnectionDetails(assignId,questionId,query_id,course_id);
		TableMap tm = cvc.getTableMap();
		PopulateTestData p=new PopulateTestData();
		p.fetchAndPopulateTestDatabase(Integer.parseInt(assignment_id), questionId,query_id, course_id, datasetid, tm);
		cvc.closeConn();

	}

	public static void main(String args[]) throws Exception{
		/*
		PopulateTestData ptd = new PopulateTestData();
		String test = ptd.generateCvcOutput("cvc3_temp2.cvc");
		String cutFile = ptd.cutRequiredOutput(test);
		Vector<String> listOfCopyFiles = ptd.generateCopyFile(cutFile);
		ptd.populateTestDatabase(listOfCopyFiles);

		try{
			PreparedStatement pstmt = null;
			TableMap tableMap = TableMap.getInstances();
			Connection conn = (new MyConnection()).getExhistingDatabaseConnection();
			QueryParser qParser = new QueryParser(tableMap);
			qParser.parseQuery("q1", "select * from instructor inner join teaches using(instructor_id) inner join crse using(course_id)");
			Query query = qParser.getQuery();

			Vector<Mutant> mutants = ptd.generateJoinMutants(query);



		}catch(Exception e){
			e.printStackTrace();
		}

		ptd.killedMutants("cvc3_"+ count +".cvc", this.query, "DS"+count, queryString, filePath);
		 */
		/*String copyFile = "a.copy";
		System.out.println(copyFile.substring(0,copyFile.indexOf(".copy")));
		BufferedReader br = new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc4/"+copyFile));
		String str;
		String data="";
		while((str = br.readLine())!=null){
			System.out.println(str);
			data+=str+"@@";
		}//Process proc = r.exec("sh "+Configuration.scriptsDir+"/upload.sh " + copyFile.substring(0,copyFile.indexOf(".copy")) + " " + copyFile + " " + filePath+" "+Configuration.databaseIP+" "+Configuration.databaseName+" "+Configuration.testDatabaseUser+" "+Configuration.testDatabaseUserPasswd);
		//int errVal = proc.waitFor();
		new PopulateTestData().uploadToTestUser(copyFile.substring(0,copyFile.indexOf(".copy")) ,data,"4");
		 */
		/*String datasetid=args[0];
		String questionid=args[1];
		System.out.println("------PopulateTestData------");
		System.out.println("Datasetid :"+datasetid);
		System.out.println("QuestionId :"+questionid);
		GenerateCVC1 cvc = new GenerateCVC1();
		cvc.initializeConnectionDetails(1);
		TableMap tm = cvc.getTableMap();
		PopulateTestData p=new PopulateTestData();
		p.fetechAndPopulateTestDatabase(questionid, datasetid, tm);*/

		PopulateTestData p=new PopulateTestData();
		p.generateCvcOutput("cvc3_9.cvc", "4/A1Q23");

	}

}