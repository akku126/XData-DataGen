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
import util.*;


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
				
				while ((ch = myIStreamReader.read()) != -1) 
				{ 
					out.write((char)ch); 
				} 	
				Callable<Integer> call = new CallableProcess(myProcess);
				Future<Integer> future = service.submit(call);
				int exitValue = future.get(300000L, TimeUnit.MILLISECONDS);		    

				
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
			HashMap<String, Integer> noOfOutputTuples, TableMap tableMap,Vector<Column> columns, Set existingTableNames) throws Exception{
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
		Vector<String> listOfCopyFiles = generateCopyFile(cutFile, filePath, noOfOutputTuples, 
				tableMap,columns,existingTableNames);			
		Vector<String> listOfFiles = (Vector<String>) listOfCopyFiles.clone();

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
		//try(Connection conn = (new DatabaseConnection().getTesterConnection(assignmentId)).getTesterConn()){
		//	populateTestDataForTesting(listOfCopyFiles, filePath, tableMap,conn, assignmentId, questionId);

			for(String i : listOfFiles){				
				File src = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/"+i);
				File dest = new File(Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetName+"/"+i);
				Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			returnVal=true;

		/*}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			//e.printStackTrace();
			throw new Exception("Process exited", e);
		} finally {

			if (proc!=null)
				Utilities.closeProcessStreams(proc);
		}*/
		return returnVal;			
	}
	
	public void deleteAllTempTablesFromTestUser(Connection dbConn) throws Exception{
		Statement st = dbConn.createStatement();
		st = dbConn.createStatement();
		st.executeUpdate("DISCARD TEMPORARY");
		st.close();
	}


}
