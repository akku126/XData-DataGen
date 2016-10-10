package testDataGen;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;

import com.google.gson.Gson;

import util.Configuration;
import util.DataSetValue;
import util.DatabaseConnection;
import util.DatabaseHelper;
import util.FailedDataSetValues;
import util.MyConnection;
import util.TableMap;
import util.Utilities;

import java.sql.SQLException;
import java.util.*;
import java.sql.*;

import testDataGen.QueryStatusData.QueryStatus;
import parsing.AddSelectClauseForWithAs;
import partialMarking.MarkInfo;
import partialMarking.PartialMarker;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import testDataGen.PopulateTestData;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgresql.util.PSQLException;

public class TestAnswer {
	
	private static Logger logger = Logger.getLogger(TestAnswer.class.getName());
	public boolean isView=false;
	public boolean isCreateView=false;

	Vector<String>  datasets;
	public TestAnswer() {
		
	}
	public static void Dataset(String mut, String ds,String filePath, String query){		
	}
	
	/**
	 * This method parses WITH AS query using JSQL parser, processes it and returns
	 * the modified query
	 * 
	 * @param queryId
	 * @param queryString
	 * @param debug
	 * @throws Exception
	 */
	 public static String parseWithAsQueryJSQL(String orginalQuery) throws Exception{
		 logger.log(Level.INFO,"queryString before Parsing : " + orginalQuery);
		 
		 	String alteredWithQuery = null;
		 	if(orginalQuery != null && !orginalQuery.isEmpty()){
		 	orginalQuery=orginalQuery.trim().replaceAll("\n+", " ");
		 	orginalQuery=orginalQuery.trim().replaceAll(" +", " ");	
		 	orginalQuery = orginalQuery.replace( "NATURAL LEFT OUTER","LEFT OUTER");
		 	orginalQuery = orginalQuery.replace("NATURAL RIGHT OUTER","RIGHT OUTER");
			CCJSqlParserManager pm = new CCJSqlParserManager();
			Statement stmt = pm.parse(new StringReader(orginalQuery));
			PlainSelect plainSelect =  null;
			
			//Check if query contains WithItem list - then Query is of the form  WITH S AS ()
			if(((Select) stmt).getSelectBody() instanceof PlainSelect 
					&& ((Select)stmt).getWithItemsList() != null){
				PlainSelect selectClause =(PlainSelect) ((Select) stmt).getSelectBody();
				
				//Substitute the select body in WithItem to the PlainSelect inside select Body of 
				// top element.
				List withItemsList = ((Select)stmt).getWithItemsList();
				//Use tokenizer to find the alias and substitute subselect
				String newQuery = "";
				for(int i=0;i<withItemsList.size();i++){ 
					final WithItem item = (WithItem)withItemsList.get(i);
					//The from item uses WITH AS ALIAS 
					/**Changes for handling with as by  Start **/
					//Call the method to check WITH AS alias is used in FromItem,
					newQuery = modifyFromItemForWithAs(selectClause, item);
					logger.log(Level.INFO,"After modifying FROM ITEM of main query : details : ");
					logger.log(Level.INFO,"selectCaluse = \n" + selectClause.toString() +"\n");
					if(newQuery != null && !newQuery.isEmpty()){
						alteredWithQuery = newQuery;
					}
					newQuery = modifyWhereClauseforWithAs(selectClause, item);
					logger.log(Level.INFO,"After modifying WHERE part : details : ");
					
					logger.log(Level.INFO,"selectCaluse = \n" + selectClause.toString() +"\n");
					if(newQuery != null && !newQuery.isEmpty()){
						alteredWithQuery = newQuery;
					}
					if(selectClause.getJoins() != null ){
						List joinList = selectClause.getJoins();
						
						for(int j=0; j < joinList.size(); j++){
							Join jcl = (Join)joinList.get(j);
							
							if(jcl.getRightItem().toString().equalsIgnoreCase(item.getName())){
								//Get withItem and create new SubSelect if fromItem name is equal to with item name
								SubSelect sub = new SubSelect();
								Alias a = new Alias(item.getName());
								a.setUseAs(true);
								sub.setAlias(a);
								sub.setSelectBody(item.getSelectBody());
								
								AddSelectClauseForWithAs addSB = new AddSelectClauseForWithAs(item.getSelectBody());
								addSB.getNewFromItem(sub);
								newQuery = addSB.getNewQuery(j,jcl,selectClause,sub); 
								//This returns the new query - call JSQLParser on the new Query
								alteredWithQuery = newQuery;
								
							}
							
						}						
					}
				}
			}
		 	}
			return alteredWithQuery;		
	 }
	 /**
		 * This method modifies the WITH AS queries. If From Item of the main query block
		 * contains WITH AS Alias, then this method removes the alias and substitutes the original
		 * query with the WITH AS query and returns the modified query for processing.
		 * 
		 * @param selectClause
		 * @param item
		 * @return
		 */
		public static String modifyFromItemForWithAs(PlainSelect selectClause, WithItem item){
			
			String newQuery = "";
			if(selectClause.getFromItem().toString().equalsIgnoreCase(item.getName())){
				//Get withItem and create new SubSelect if fromItem name is equal to with item name
				SubSelect sub = new SubSelect();
				Alias a = new Alias(item.getName());
				a.setUseAs(true);
				sub.setAlias(a);
				sub.setSelectBody(item.getSelectBody());
				
				AddSelectClauseForWithAs addSB = new AddSelectClauseForWithAs(item.getSelectBody());
				addSB.getNewFromItem(sub);
				newQuery = addSB.getNewQuery(selectClause,sub); 
				
			}
			return newQuery;
		}
		/**
		 * This method modifies the WITH AS queries. If WHERE clause of the main query block
		 * contains WITH AS Alias, then this method removes the alias and substitutes the original
		 * query with the WITH AS query and returns the modified query for processing.
		 * 
		 * @param selectClause
		 * @param item
		 * @return
		 */
		public static String modifyWhereClauseforWithAs(PlainSelect selectClause, WithItem item){
			String newQuery = "";
			Expression newExp = null;
			Expression exp = selectClause.getWhere();
			if(exp != null && exp.toString().contains(item.getName())){
				if(exp instanceof BinaryExpression){
					Expression leftExpr = ((BinaryExpression)exp).getLeftExpression();
					Expression rightExpr = ((BinaryExpression)exp).getRightExpression();
					if(leftExpr != null && leftExpr.toString().contains(item.getName())){
						if(leftExpr instanceof SubSelect){
							SubSelect newS = ((SubSelect)leftExpr);
							PlainSelect ps = (PlainSelect)newS.getSelectBody();
							//Check if from item of SubSelect has WITH AS
							newQuery = TestAnswer.modifyFromItemForWithAs(ps, item);
							//New query contains alias name as same as in main query
							//So change alias name - under discussion
							((SubSelect) leftExpr).setSelectBody(ps);
							logger.log(Level.INFO,"Modified left expr - now proceed to modify WHERE --> \n" + newQuery);
						}
					}
					if(rightExpr != null && rightExpr.toString().contains(item.getName())){
						if(rightExpr instanceof SubSelect){
							SubSelect newS = ((SubSelect)rightExpr);
							PlainSelect ps = (PlainSelect)newS.getSelectBody();
							//Check if from item of SubSelect has WITH AS
							newQuery = TestAnswer.modifyFromItemForWithAs(ps, item);
							//New query contains alias name as same as in main query
							//So change alias name - under discussion
							((SubSelect) rightExpr).setSelectBody(ps);
							logger.log(Level.INFO,"Modified right expr - now proceed to modify WHERE --> \n" + newQuery);
						}
					}
				}
				else if(exp!= null && exp instanceof SubSelect){
					SubSelect newS = ((SubSelect)exp);
					PlainSelect ps = (PlainSelect)newS.getSelectBody();
					//Check if from item of SubSelect has WITH AS
					newQuery = TestAnswer.modifyFromItemForWithAs(ps, item);
					//New query contains alias name as same as in main query
					//So change alias name - under discussion
					((SubSelect) exp).setSelectBody(ps);
					logger.log(Level.INFO,"Modified where - now proceed to modify WHERE --> \n" + newQuery);
				}
				newQuery = selectClause.toString();
				logger.log(Level.INFO,"Modified Query fir WITH AS in  JOIN--> \n"+ newQuery);
			}
					
			return newQuery;
		}

	public static String preParseQuery(String queryString) throws Exception{

		StringTokenizer st=new StringTokenizer(queryString.trim());
		String token=st.nextToken();

		if(!token.equalsIgnoreCase("with")){
			return queryString;
		}
		int numberOfAlias=0;
		String aliasname[]=new String[10];
		String subquery[]=new String[10];

		while(true){

			String columnname="";
			aliasname[numberOfAlias]=st.nextToken();

			if(aliasname[numberOfAlias].contains("(")){

				columnname=aliasname[numberOfAlias].substring(aliasname[numberOfAlias].indexOf("("));
				columnname=columnname.substring(1,columnname.length()-1);	//remove ( & )

				aliasname[numberOfAlias]=aliasname[numberOfAlias].substring(0,aliasname[numberOfAlias].indexOf("("));           	

			}
			token=st.nextToken();   	// should be AS key word or should start with (

			if(token.startsWith("(")){
				while(!token.contains(")")){
					columnname+=token;
					token=st.nextToken();
				}
				columnname+=token;            	
				token=st.nextToken();	// should be AS key word
			}

			if(!token.equalsIgnoreCase("as")){            	
				Exception e= new Exception("Error while preparsing the with clause AS expected");
				throw e;
			}
			
			subquery[numberOfAlias]="(";
			queryString=queryString.substring(queryString.indexOf("(")+1);
			if(columnname.length()!=0){
				queryString=queryString.substring(queryString.indexOf("(")+1);
			}

			int count=1,i=0;
			while(count!=0){
				if(queryString.charAt(i)=='('){
					count++;
				}else if(queryString.charAt(i)==')'){
					count--;
				}
				subquery[numberOfAlias]+=queryString.charAt(i);
				i++;
			}
			queryString=queryString.substring(i).trim();

			if(columnname.length()!=0){
				columnname=columnname.substring(1,columnname.length()-1);
				String columnlist[]=columnname.split(",");
				int ctr=0;
				String temp=subquery[numberOfAlias];
				subquery[numberOfAlias]="";            	
				String tok=temp.substring(0,temp.indexOf("from"));
				for(int j=0;j<tok.length();j++){
					if(tok.charAt(j)==','){
						subquery[numberOfAlias]+=" as "+columnlist[ctr++]+" , ";
					}else{
						subquery[numberOfAlias]+=tok.charAt(j);
					}

				}            	            	
				subquery[numberOfAlias]+=" as "+columnlist[ctr]+" "+temp.substring(temp.indexOf("from"));
			}

			numberOfAlias++;
			if(queryString.charAt(0)!=','){            	
				break;
			}else{
				st=new StringTokenizer(queryString.substring(1).trim());
			}

		}

		String newquery="";
		/*Add the select part to new query */
		st=new StringTokenizer(queryString);                    
		
		while(st.hasMoreTokens()){
			
			token=st.nextToken();
			
			if(token.toLowerCase().equals("from")){
				newquery+=token+ " ";
				newquery = parseFromPart(st, newquery, numberOfAlias, subquery, aliasname);				
			}
			else{			
				newquery+=token+ " ";
			}
		}

		return newquery;
	}
	
	private static String parseFromPart(StringTokenizer st, String newquery, int numberOfAlias, String subquery[], String aliasname[]){
		
		String token;
		
		while(st.hasMoreTokens()){
			token=st.nextToken();            
					
			if(token.equalsIgnoreCase("where")||token.equalsIgnoreCase("group")){
				newquery+=token+ " ";
				break;
			}			
			
			if(token.equals(",")){
				newquery+=token+ " ";
			}
			if(token.contains(",")){
				token+=" ";
				String tablenames[]=token.split(",");
				for(int j=0;j<tablenames.length;j++){
					boolean isPresent=false;
					for(int k=0;k<numberOfAlias;k++){
						if(tablenames[j].equals(aliasname[k])){
							newquery+=subquery[k] + " " + aliasname[k]+" ";
							isPresent=true;
						}
					}
					if(!isPresent){
						newquery+=tablenames[j]+" ";
					}
					newquery+=",";
				}
				newquery=newquery.substring(0,newquery.length()-1);

			}else if(token.contains(")")){
				String relationName = token.substring(0, token.length() - 1);				
				
					boolean isPresent=false;
					for(int k=0;k<numberOfAlias;k++){
						if(relationName.equals(aliasname[k])){
							newquery+=subquery[k] + " " + aliasname[k]+" ";
							isPresent=true;
						}
					}
					if(!isPresent){
						newquery+=relationName + " ";
					}
					newquery+=")";
				
			}else{
				boolean isPresent=false;
				for(int k=0;k<numberOfAlias;k++){
					if(token.equals(aliasname[k])){
						newquery+=subquery[k] + " " + aliasname[k]+" ";
						isPresent=true;
					}
				}
				if(!isPresent){
					newquery+=token+" ";
				}
			}

		}
		 
		return newquery;		
	}
    
    public static Vector<String> checkAgainstOriginalQuery(HashMap<String,String> mutants, String datasetName, String queryString, String filePath, 
    		boolean orderIndependent, Vector<String> columnmismatch, Connection conn) {
    	PreparedStatement pstmt = null;
		ResultSet rs = null;
		String temp="";
		boolean isInsertquery=false;
		boolean isUpdatequery=false;
		boolean isDeletequery=false; 
		boolean next1 = true;
		boolean next2 = true;
		Vector<String> queryIds = new Vector<String>();		
		try{
			
			try{
				pstmt = conn.prepareStatement("drop table if exists dataset;");
				pstmt.execute();
				//Create temporary tables based on the dataset (data generated by Xdata) that is passed
				pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE dataset(name varchar(20))");
						//"AS (SELECT * FROM dataset WHERE (1=0))");
				pstmt.executeUpdate();
			}
			catch(SQLException ex){
				int errorCode = ex.getErrorCode();
				logger.log(Level.SEVERE,"SQL Exception: "+ex.getMessage(),ex);
			} 
			
			pstmt = conn.prepareStatement("insert into dataset values('" + datasetName + "')");
			pstmt.executeUpdate();
			int i=1;			

			logger.log(Level.INFO,"queryString" + queryString);
			queryString=queryString.trim().replaceAll("\n+", " ");
			queryString=queryString.trim().replaceAll(" +", " ");	
			queryString = queryString.trim().replace(";", " ");
		
			String mutant_query="";
			Collection c = mutants.keySet();
			Iterator itr = c.iterator();
			while(itr.hasNext()){
				Object Id = itr.next();
				String mutant_qry = mutants.get(Id);
				mutant_qry=mutant_qry.trim().replace(';', ' ');				
				
				if(isInsertquery){					
					mutant_qry=convertInsertQueryToSelect(mutant_qry);
				}else if(isDeletequery){
					mutant_qry=convertDeleteQueryToSelect(mutant_qry);
				}if(isUpdatequery){
					mutant_qry=convertUpdateQueryToSelect(mutant_qry);
				}
				
				//Parse the instructor query to get number of projected columns
				CCJSqlParserManager pm = new CCJSqlParserManager();
				Statement stmnt = pm.parse(new StringReader(queryString));
				PlainSelect plainSelect =  (PlainSelect)((Select) stmnt).getSelectBody();
				List<SelectItem> rcList = plainSelect.getSelectItems();
				
				
				PreparedStatement pstmt11 = conn.prepareStatement(queryString);
				PreparedStatement pstmt22 = conn.prepareStatement(mutant_qry);
				logger.log(Level.INFO,"****************************************");
				logger.log(Level.INFO,"Instructor query ----" +queryString.toString());
				logger.log(Level.INFO,"****************************************");
				logger.log(Level.INFO,"Mutant query ----" + mutant_qry.toString());
				logger.log(Level.INFO,"*****************************************");
				if(orderIndependent){
					try{				
						//Run both the queries against the temporary data set that is generated.
						 
						pstmt = conn.prepareStatement("with x1 as (" + queryString + ")," +
								" x2 as (" + mutant_qry + ") select 'Q" + i 
								+ " was killed by ' as const,dataset.name from dataset " +
								"where exists ((select * from x1) except all (select * from x2)) " +
								"or exists ((select * from x2) except all (select * from x1))");
						logger.log(Level.INFO,"Comparing Query *** " +pstmt.toString());		
						rs = pstmt.executeQuery();
						ResultSet rs11 = pstmt22.executeQuery();
						while(rs11.next()){
							//logger.log(Level.INFO,rs11.getString(1));
						}
						ResultSet rs22 = pstmt11.executeQuery();
						while(rs22.next()){
							//logger.log(Level.INFO,rs22.getString(1));
						}		
						//logger.log(Level.INFO,"--------------------------------------------- ");
					}catch(SQLException s){
						if(s instanceof PSQLException && s.getMessage().trim().equalsIgnoreCase("No results were returned by the query.".trim())){
							 
						}else{
							queryIds.add((String)Id);
							columnmismatch.add((String)Id);
							logger.log(Level.SEVERE,s.getMessage(), s);
							//throw s;
						}
						logger.log(Level.INFO," SQL EXCEPTION"+s.getMessage(),s);
					}
					catch(Exception ex){
						logger.log(Level.SEVERE,ex.getMessage(), ex);
						//ex.printStackTrace();
						try{
							pstmt = conn.prepareStatement(mutant_qry);
							rs = pstmt.executeQuery();
							ResultSetMetaData rsmd=rs.getMetaData();
							Vector<String> projectedCols = new Vector<String>();
							pstmt =conn.prepareStatement(queryString);
							rs =pstmt.executeQuery();
							ResultSetMetaData orgRsmd=rs.getMetaData();
							for(int k=1;k<=orgRsmd.getColumnCount();k++){
								projectedCols.add(orgRsmd.getColumnName(k));
							}
							if(orgRsmd.getColumnCount()!=rsmd.getColumnCount()){
								columnmismatch.add((String)Id);
							}
							else{
								for(int k=1;k<=rsmd.getColumnCount();k++){
									if(!projectedCols.contains(rsmd.getColumnName(k))){
										columnmismatch.add((String)Id);
										break;
									}
								}
							}
							queryIds.add((String)Id);
						}catch(Exception e){
							logger.log(Level.SEVERE,e.getMessage(), e);
							//e.printStackTrace();
							queryIds.add((String)Id);
						}
						finally{
							
							pstmt = conn.prepareStatement("drop table dataset;");
							pstmt.execute();
						}
					}finally{
						
						pstmt = conn.prepareStatement("drop table dataset;");
						pstmt.execute();
					}
					if(rs==null){
//						logger.log(Level.INFO,"rs is null");
					}
					
					else if(rs!=null && rs.next()){
						
						//logger.log(Level.INFO,"Adding Query Id = "+(String)Id);
						queryIds.add((String)Id);
						
					}else{
						//logger.log(Level.INFO,"rs is empty");
					}
				}
				else{
					PreparedStatement pstmt1 = conn.prepareStatement(queryString);
					PreparedStatement pstmt2 = conn.prepareStatement(mutant_qry);
	     			ResultSet rs1 = pstmt1.executeQuery();
					ResultSet rs2 = pstmt2.executeQuery();
					boolean outputEqual = true;
					int k = 1;
					while(rs1!=null && rs1.next() && rs2!=null && rs2.next()){
						
						while(rs1!=null && rs1.next() && rs2!=null && rs2.next()){
							if(rs1.equals(rs2)){
								
							}
							if(rs1.getString(k).equals(rs2.getString(k))){
								
							}
							else{
								outputEqual = false;
							}
							k++;
						}
					}
								
					if((rs1!=null && rs2 == null) || (rs1== null && rs2 !=null)){	
						outputEqual = false;
					}
					if(!outputEqual){
						queryIds.add((String)Id);
					}
					
					pstmt1.close();
					pstmt2.close();
					rs1.close();
					rs2.close();
				}
				
			}
		}catch(Exception e){
			logger.log(Level.SEVERE, "TestAnswer : ", e);
			//e.printStackTrace();
		}
		logger.log(Level.INFO,"--------------------------------------------- ");
		logger.log(Level.INFO,"Dataset: "+datasetName+" Killed mutants: "+queryIds);
		
		logger.log(Level.INFO,"--------------------------------------------- ");
		 
		try {
			pstmt.close();
			//rs.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "TestAnswer : ", e);
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		

			return queryIds;
    }
    
	/**
	 * 
	 * @param mutants
	 * @param datasetName
	 * @param queryString
	 * @param filePath
	 * @param orderIndependent
	 * @param columnmismatch
	 * @param conn
	 * @return
	 * @throws IOException
	 * @Deprecated
	 */
    @Deprecated
	public static Vector<String> mutantsKilledByDataset1(HashMap<String,String> mutants, String datasetName, String queryString, String filePath, boolean orderIndependent, Vector<String> columnmismatch,Connection conn) throws IOException{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		//Connection conn = null;
		String temp="";
		BufferedReader input=null;
		boolean isInsertquery=false;
		boolean isUpdatequery=false;
		boolean isDeletequery=false;
		
		Set<String> queryIds = new HashSet<String>();
		try{
			int i=1;
			Collection c = mutants.keySet();
			Iterator itr = c.iterator();
			while(itr.hasNext()){
				Object Id = itr.next();
				String mutant_qry = mutants.get(Id);
				mutant_qry=mutant_qry.trim().replace(';', ' ');				
				
				if(isInsertquery){					
					mutant_qry=convertInsertQueryToSelect(mutant_qry);
				}else if(isDeletequery){
					mutant_qry=convertDeleteQueryToSelect(mutant_qry);
				}if(isUpdatequery){
					mutant_qry=convertUpdateQueryToSelect(mutant_qry);
				}
				
				PreparedStatement pstmt11 = conn.prepareStatement(queryString);
				PreparedStatement pstmt22 = conn.prepareStatement(mutant_qry);
				
				if(orderIndependent){
					try{			
						pstmt = conn.prepareStatement("with x1 as (" + queryString + ")," +
								" x2 as (" + mutant_qry + ") " +
										"select 'Q" + i + " was killed by ' as const,dataset.name from " +
												"dataset where " +
												"exists ((select * from x1) except all (select * from x2)) " +
												"or exists ((select * from x2) except all (select * from x1))");
						rs = pstmt.executeQuery();
					
						ResultSet rs11 = pstmt22.executeQuery();
						
						while(rs11.next()){
							//logger.log(Level.INFO,rs11.getString(1) );
						}
						//logger.log(Level.INFO,"--------------------------------------------- ");
						ResultSet rs22 = pstmt11.executeQuery();
						while(rs22.next()){
							//logger.log(Level.INFO,rs22.getString(1));
						}
						//logger.log(Level.INFO,"--------------------------------------------- ");
						
					}catch(Exception ex){
						//ex.printStackTrace();
						//logger.log(Level.SEVERE,"Adding Query Id = "+(Integer)Id,ex);
						try{
							pstmt = conn.prepareStatement(mutant_qry);
							rs = pstmt.executeQuery();
							ResultSetMetaData rsmd=rs.getMetaData();
							Vector<String> projectedCols = new Vector<String>();
							pstmt =conn.prepareStatement(queryString);
							rs =pstmt.executeQuery();
							ResultSetMetaData orgRsmd=rs.getMetaData();
							for(int k=1;k<=orgRsmd.getColumnCount();k++){
								projectedCols.add(orgRsmd.getColumnName(k));
							}
							if(orgRsmd.getColumnCount()!=rsmd.getColumnCount()){
								//columnmismatch.add((String)Id);
								queryIds.add((String)Id);
							}
							else{
								for(int k=1;k<=rsmd.getColumnCount();k++){
									if(!projectedCols.contains(rsmd.getColumnName(k))){
										//columnmismatch.add((String)Id);
										queryIds.add((String)Id);
										break;
									}
								}
							}
						}catch(Exception e){
							logger.log(Level.SEVERE, "Message", e);
							//e.printStackTrace();
							queryIds.add((String)Id);
						}
					}
					if(rs==null){
//						logger.log(Level.INFO,"rs is null");
					}
					
					else if(rs!=null && rs.next()){
						
						//logger.log(Level.INFO,"Adding Query Id = "+(String)Id);
						queryIds.add((String)Id);
						
					}else{
						//logger.log(Level.INFO,"rs is empty");
					}
				}
				else{// order by clause is dere
					// check output of both the queries row by row
					PreparedStatement pstmt1 = conn.prepareStatement(queryString);
					PreparedStatement pstmt2 = conn.prepareStatement(mutant_qry);
					//logger.log(Level.INFO,"Mutant query "+mutant_qry);
					//logger.log(Level.INFO,"Query String query "+queryString);
					ResultSet rs1 = pstmt1.executeQuery();
					ResultSet rs2 = pstmt2.executeQuery();
					boolean outputEqual = true;
					while(rs1!=null && rs1.next() && rs2!=null && rs2.next()){
						if(rs1.equals(rs2)){
							
						}
						else{
							outputEqual = false;
						}
					}
					if((rs1!=null && rs2 == null) || (rs1== null && rs2 !=null)){	
						outputEqual = false;
					}
					if(outputEqual){
						queryIds.add((String)Id);
					}
				}
				
			}
		}catch(Exception e){
			logger.log(Level.SEVERE, "Message", e);
			//e.printStackTrace();
		}finally {
			if(input!=null)
				input.close();
		}
		Vector<String> res = new Vector<String>();
		
		for(String s : queryIds){
			res.add(s);
		}
		return res;
	}
	
    /**
     * 
     * @param queryString
     * @return
     * @throws Exception
     */
	public static String convertUpdateQueryToSelect(String queryString) throws Exception {
		
		String out="SELECT ";
		String tablename=queryString.trim().replaceAll("\\s+", " ").split(" ")[1];
		String st[]=queryString.split("=");		
		for(int i=1;i<st.length;i++){
			
			if(st[i].contains(",")){
				out+=" "+st[i].substring(0,st[i].indexOf(","))+",";
			}else if(st[i].toLowerCase().contains("where")){
				out+=" "+st[i].substring(0,st[i].toLowerCase().indexOf("where"))+" FROM "+tablename+" ";
				out+=st[i].substring(st[i].toLowerCase().indexOf("where"));
			}else{
				out+=" "+st[i]+" FROM "+tablename;
			}
			
		}
		return out;
	}
	/**
	 * 
	 * @param queryString
	 * @return
	 */
	public static String convertDeleteQueryToSelect(String queryString) {
		String out=queryString;
		out="SELECT * "+queryString.substring(queryString.toLowerCase().indexOf("from"),queryString.length());		
		return out;
	}
	public static String convertInsertQueryToSelect(String queryString) {
		String out=queryString;
		out=out.substring(queryString.toLowerCase().indexOf("select"), queryString.length());
		return out;
	}
	/**
	 * Download the datasets for all the queries for a question
	 * 
	 * @param assignmentId
	 * @param questionId
	 * @param queryId
	 * @param course_id
	 * @param conn
	 * @param dataSetForQueries
	 * @return
	 * @throws Exception
	 */
	public static Map<Integer,Vector<String>> downloadDataSetForAllQueries(int assignmentId, int questionId,int queryId,  
			String course_id, Connection conn,Map <Integer,Vector<String>> dataSetForQueries) throws Exception{
		
		Vector <String>datasets=new Vector<String>();
		String getDatasetQuery = null;
		getDatasetQuery = "Select * from xdata_datasetvalue where assignment_id = ? and question_id = ? and query_id = ? and course_id=?";

		PreparedStatement smt;
		smt=conn.prepareStatement(getDatasetQuery);
		smt.setInt(1, assignmentId);
		smt.setInt(2,questionId);
		smt.setInt(3,queryId);
		smt.setString(4,course_id);
		ResultSet rs=smt.executeQuery();
			while(rs.next()){
				
				String datasetid=rs.getString("datasetid");
				String datasetvalue=rs.getString("value");
				
				datasets.add(datasetid);
				dataSetForQueries.put(rs.getInt("query_id"), datasets);
			}
			return dataSetForQueries;
	}
	/**
	 * Download datasets for single query
	 * 
	 * @param assignmentId
	 * @param questionId
	 * @param queryId
	 * @param course_id
	 * @param conn
	 * @param filePath
	 * @param onlyFirst
	 * @return
	 * @throws Exception
	 */
	public static Map<Integer,Vector<String>> downloadDatasets(int assignmentId, int questionId,int queryId,  String course_id, Connection conn, String filePath, boolean onlyFirst) throws Exception{
		
		Vector <String>datasets=new Vector<String>();
		Map <Integer,Vector<String>> dataSetForQueries = new HashMap<Integer,Vector<String>>();
		String getDatasetQuery = null;
		
		if(!onlyFirst){
			getDatasetQuery = "Select * from xdata_datasetvalue where assignment_id = ? and question_id = ? and query_id = ? and course_id=?";
		}
		else{
			getDatasetQuery = "Select * from xdata_datasetvalue where assignment_id = ? and question_id = ?  and query_id = ? and course_id=? and datasetid = 'DS0'";
		}
 
		PreparedStatement smt;
		smt=conn.prepareStatement(getDatasetQuery);
		smt.setInt(1, assignmentId);
		smt.setInt(2,questionId);
		smt.setInt(3,queryId);
		smt.setString(4,course_id);
		ResultSet rs=smt.executeQuery();
			while(rs.next()){
				
				String datasetid=rs.getString("datasetid");
				String datasetvalue=rs.getString("value");
				
				datasets.add(datasetid);
				dataSetForQueries.put(rs.getInt("query_id"), datasets);
				
				String dsPath=Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetid;
				File f=new File(dsPath);
				if(!f.exists()){
					f.mkdirs();
				}
				else{
					Runtime r = Runtime.getRuntime();
					Process proc = r.exec("rm "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasetid+"/*");
									
					proc.waitFor();
					Utilities.closeProcessStreams(proc);
					proc.destroy();
				}
				//JSON implementation reqd and test here.
				//It holds JSON obj tat has list of Datasetvalue class
				
					Gson gson = new Gson();
					//ArrayList dsList = gson.fromJson(value,ArrayList.class);
					Type listType = new TypeToken<ArrayList<DataSetValue>>() {
	                }.getType();
	                
					List<DataSetValue> dsList = new Gson().fromJson(datasetvalue, listType);
					for(int i = 0 ; i < dsList.size();i++ ){
						DataSetValue dsValue = dsList.get(i);
						String tname,values;
						String tablename = dsValue.getFilename().substring(0,dsValue.getFilename().indexOf(".copy"));
								
						//String tablename=copyfile[i].split(".copy")[0];
						//String tabledata=copyfile[i].split(".copy")[1];
						FileWriter fos = new FileWriter(new File(dsPath, tablename+".copy").toString());
						BufferedWriter brd = new BufferedWriter(fos);
						String writedata="";
						for(int j=0;j<dsValue.getDataForColumn().size();j++){
							writedata+=dsValue.getDataForColumn().get(j)+"\n";					
						}
						//writedata=writedata.substring(0,writedata.length()-1);
						brd.write(writedata);
						brd.close();		
						fos.close();
					}			
			}
		rs.close();
		smt.close();
		
		return dataSetForQueries;
	}
	
	/** 
	 * This method test the student query against DS0 when student submits edited query in 
	 * non-interactive mode
	 * 
	 * @param assignmentId
	 * @param questionId
	 * @param query
	 * @param user
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public QueryStatus testQueryAnswer(int assignmentId, int questionId,String course_id, String query, String user, String filePath) throws Exception{
		

		int queryId = 1;
		String qId = "A"+assignmentId+"Q"+questionId+"S"+queryId;
		Map <Integer,Boolean> resultOfDatasetMatchForEachQuery  = new HashMap<Integer,Boolean>();
		ArrayList <Boolean> isDataSetVerified  = null;
		boolean isMatchAllQueryIncorrect = false;
		String matchAllFailedForDS = "";
		QueryStatus status = QueryStatus.Error;
		
		boolean isQueryPass = false;
		PopulateTestData p = new PopulateTestData();
		boolean orderIndependent = false;
		boolean isMatchAll = false;
		
		try(Connection conn = MyConnection.getDatabaseConnection()){
			try(Connection testConn = new DatabaseConnection().getTesterConnection(assignmentId)){
		
				String getMatchAllOption = "select matchallqueries,orderIndependent from xdata_qinfo where assignment_id = ? and question_id = ? and course_id= ?";
				try(PreparedStatement pst = conn.prepareStatement(getMatchAllOption)){
					pst.setInt(1,assignmentId);
					pst.setInt(2,questionId);
					pst.setString(3,course_id);
					try(ResultSet rset = pst.executeQuery()){
						rset.next();
						isMatchAll = rset.getBoolean("matchallqueries");
						orderIndependent = rset.getBoolean("orderIndependent");
					}
				}
				String qry = "select * from xdata_instructor_query where assignment_id = ? and question_id = ? and course_id=?";
				try(PreparedStatement pstmt = conn.prepareStatement(qry)){
				pstmt.setInt(1,assignmentId);
				pstmt.setInt(2,questionId);
				pstmt.setString(3,course_id);
				
				try(ResultSet rs = pstmt.executeQuery()){
				
						while(rs.next()){
								//If Instructor has chosen match All queries option, get all datasets
								// match student query against all and return true only if all ds matches
								String sqlQuery=rs.getString("sql");
								queryId = rs.getInt("query_id");
								qId = "A"+assignmentId +"Q"+questionId+"S"+queryId;
								query=checkForViews(query,user);
								HashMap<String,String> mutants = new HashMap<String,String>();
								mutants.put(qId, query);
								boolean incorrect=false;
								isDataSetVerified = new ArrayList<Boolean>();
								Map <Integer,Vector<String>>  datasetForQueryMap =  downloadDatasets(assignmentId,questionId, queryId,course_id, conn, filePath, true);
								if(datasetForQueryMap.isEmpty()){
									//Load the default sample data file
									boolean flag=true;
									Vector<String> cmismatch = new Vector<String>();
									try{
									 p.deleteAllTempTablesFromTestUser(testConn);
									}catch(Exception e){
										logger.log(Level.INFO,"Temporary Table does not exist",e);
										
									}
									try{
									 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
									}catch(Exception e){
										logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
										throw e;
									}

										Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, "DS_Default", sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
										logger.log(Level.INFO,"******************");
										logger.log(Level.INFO,"Default dataset Loaded : " + " " + killedMutants.size());
										logger.log(Level.INFO,"******************");
										for(int l=0;l<killedMutants.size();l++){
											if(mutants.containsKey(killedMutants.get(l))){
												flag = false;
												incorrect = true;
											}
										}
										if(!incorrect){
											//isDataSetVerified.add(true);
										}	
										else{
											
											resultOfDatasetMatchForEachQuery.put(queryId,false);
											logger.log(Level.INFO,"");
										}	
										
										if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
											resultOfDatasetMatchForEachQuery.put(queryId,true);
										}
									}else{
										for(Integer id : datasetForQueryMap.keySet()){
											 Vector<String> datasets = datasetForQueryMap.get(id);
											 if(datasets.size() == 0){
												conn.close();
												testConn.close();
												return QueryStatus.NoDataset;
												 }
											 boolean flag=true;
											 for(int i=0;i<datasets.size();i++){
													
													String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
												 	File ds=new File(dsPath);
												 	//GenerateCVC1 c = new GenerateCVC1();
													String copyFiles[] = ds.list();
													
													Vector<String> vs = new Vector<String>();
													for(int m=0;m<copyFiles.length;m++){
													    vs.add(copyFiles[m]);		    
													}
													
												 	// query output handling
													GenerateCVC1 cvc = new GenerateCVC1();
													Pattern pattern = Pattern.compile("^A([0-9]+)Q[0-9]+");
													Matcher matcher = pattern.matcher(qId);
													int assignId = 1;
													
													if (matcher.find()) {
													    assignId = Integer.parseInt(matcher.group(1));
													} 
													
													cvc.initializeConnectionDetails(assignId,questionId,queryId,course_id);
													TableMap tm = cvc.getTableMap();
													p.populateTestDataForTesting(vs, filePath+"/"+datasets.get(i), tm, testConn, assignId, questionId);
													
													
													Vector<String> cmismatch = new Vector<String>();
													logger.log(Level.INFO,datasets.get(i));
													
													
													Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, datasets.get(i), sqlQuery, dsPath, orderIndependent, cmismatch, testConn);
													logger.log(Level.INFO,"******************");
													logger.log(Level.INFO,datasets.get(i) + " " + killedMutants.size());
													logger.log(Level.INFO,"******************");
													for(int l=0;l<killedMutants.size();l++){
														if(mutants.containsKey(killedMutants.get(l))){
															flag = false;
															incorrect = true;
														}
													}
															if(!incorrect){
																//isDataSetVerified.add(true);
															}	
															else{
																
																resultOfDatasetMatchForEachQuery.put(queryId,false);
																logger.log(Level.INFO,"");
																}
															cvc.closeConn();									
												   }
											
										if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
											resultOfDatasetMatchForEachQuery.put(queryId,true);
										}
									 }//For each query
								}//else If dataset exists part ends 
						}//While next ResultSet
					}//close resultset try block
				}//close stmnt try blk
				/*******Check for Match all or match One Option Start******/	
				for(int i : resultOfDatasetMatchForEachQuery.keySet()){
					//Get first query's result as default
					if(i == 1){
						isQueryPass = resultOfDatasetMatchForEachQuery.get(i);
					}//For second query, get the query result
					boolean isCorrect = resultOfDatasetMatchForEachQuery.get(i);
					//If instructor selects match All result sets while creating the question,
					//then all query results should hold true for the student query to pass
					if(isMatchAll){
						isQueryPass = isCorrect && isQueryPass;	
					}
					else{
						 //This executes if instructor selects Match any result set option
						//If even one query result holds true, the student query is considered as pass
						 if(isCorrect){
							 isQueryPass = true;
							 break;
						}
					}
				}
				if(isQueryPass){
					status = QueryStatus.Correct;
				}
				else{
					status = QueryStatus.Incorrect;
				}		
			}//close connection
		}//close connection
		return status;
	}
	
	
/**
 * This method tests the student query against instructor query datasets and verifies 
 * against match all result sets or match any one result set.
 * This method is triggered during student submission in interactive mode.
 * 
 * @param assignmentId
 * @param questionId
 * @param query
 * @param user
 * @param filePath
 * @return String - passed if query passes and Failed::datasets failed will be returned.
 * @throws Exception
 */
@Deprecated	
//public String testAnswerMatchAllOption(int assignmentId,int questionId, String query, String user, String filePath) throws Exception{
	public FailedDataSetValues testAnswerMatchAllOption(int assignmentId,int questionId, String course_id,String query, String user, String filePath) throws Exception{
		
		int queryId = 1;
		String out="";
		Map <Integer,Boolean> resultOfDatasetMatchForEachQuery  = new HashMap<Integer, Boolean>();
		ArrayList <Boolean> isDataSetVerified  = null;
		boolean isMatchAll = false;
		boolean orderIndependent = false;
		boolean isMatchAllQueryIncorrect = false;
		String matchAllFailedForDS = "";
		String [] defaultDSIdsAssignment = new String[25];
		boolean isQueryPass = false;
		FailedDataSetValues failedDataSets = new FailedDataSetValues();
		int index = 0;
		//First instructor answer will be considered to show failed datasets
		String instrQuery = "";
	//Connection conn = MyConnection.getExistingDatabaseConnection();
	//Connection testConn = MyConnection.getTestDatabaseConnection();
	try(Connection conn = MyConnection.getDatabaseConnection()){
		try(Connection testConn = new DatabaseConnection().getTesterConnection(assignmentId)){
			GenerateCVC1 cvc = new GenerateCVC1();											
			cvc.initializeConnectionDetails(assignmentId,questionId,queryId,course_id);
		 	
			TableMap tm = cvc.getTableMap();
			String getMatchAllOption = "select matchallqueries,orderIndependent from xdata_qinfo where assignment_id = ? and question_id = ? and course_id = ?";
			try(PreparedStatement pst = conn.prepareStatement(getMatchAllOption)){
			
				pst.setInt(1,assignmentId);
				pst.setInt(2,questionId);
				pst.setString(3,course_id);
				try(ResultSet rset = pst.executeQuery()){
					rset.next();
					isMatchAll = rset.getBoolean("matchallqueries");
					orderIndependent = rset.getBoolean("orderIndependent");
				}
			}
			//Get default dataset Ids from assignment table
			String getDefaultDataSets = "select defaultDSetId from xdata_assignment where assignment_id = ? and course_id=?";
			try(PreparedStatement pst =conn.prepareStatement(getDefaultDataSets) ){
				pst.setInt(1,assignmentId);
				pst.setString(2,course_id);
				try(ResultSet rset = pst.executeQuery()){
					rset.next();
						String dsIds = rset.getString("defaultDSetId");
						Gson gson = new Gson();
						Type listType = new TypeToken<String[]>() {}.getType();
						defaultDSIdsAssignment = new Gson().fromJson(dsIds, listType);
					
				}
			}
			String [] defaultDSIdsPerQuestion=new String[25];
			String getDefaultDataSetsForQuestion = "select default_sampledataid from xdata_qinfo where assignment_id = ? and course_id=? and question_id=?";
			try(PreparedStatement pst =conn.prepareStatement(getDefaultDataSetsForQuestion) ){
				pst.setInt(1,assignmentId);
				pst.setString(2,course_id);
				pst.setInt(3, questionId);
				try(ResultSet rset = pst.executeQuery()){
					rset.next();
						String dsIds = rset.getString("default_sampledataid");
						Gson gson = new Gson();
						Type listType = new TypeToken<String[]>() {}.getType();
						defaultDSIdsPerQuestion = new Gson().fromJson(dsIds, listType);
					
				}
			}
			PopulateTestData p = new PopulateTestData();
			
			String qry = "select * from xdata_instructor_query where assignment_id = ? and question_id = ? and course_id = ?";
			try(PreparedStatement pstmt = conn.prepareStatement(qry)){
				pstmt.setInt(1,assignmentId);
				pstmt.setInt(2,questionId);
				pstmt.setString(3,course_id);
				try(ResultSet rs = pstmt.executeQuery()){
					while(rs.next()){
						
							//If Instructor has chosen match All queries option, get all datasets
							// match student query against all and return true only if all matches
							String sqlQuery=rs.getString("sql");
							if(index == 0){
								instrQuery = sqlQuery;
							}
							queryId = rs.getInt("query_id");							
							String qId = "A"+assignmentId +"Q"+questionId+"S"+queryId;
							query=checkForViews(query,user);
							HashMap<String,String> mutants = new HashMap<String,String>();
							mutants.put(qId, query);
							boolean incorrect=false;
							isDataSetVerified = new ArrayList<Boolean>();
							index++;
							
							//For the instructor given data sets during assignment creation
							  // test student and instructor query options 
							 
							  // First get sample data sets for question, if it is not there, then get
							  // it from assignment table
							  // If both are not there, don't set anythng - proceed with other code
							  
							if(defaultDSIdsPerQuestion != null){
								try{
									 p.deleteAllTempTablesFromTestUser(testConn);
									}catch(Exception e){
										logger.log(Level.INFO,"Temporary Table does not exist",e);
										
									}
									try{
									 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
									}catch(Exception e){
										logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
										throw e;
									}
								for(int dId= 0; dId < defaultDSIdsPerQuestion.length;dId++){
									Vector<String> cmismatch = new Vector<String>();
									logger.log(Level.INFO,"******************");
									logger.log(Level.INFO,"Default dataset "+defaultDSIdsPerQuestion[dId]+" Loaded : ");
									logger.log(Level.INFO,"******************");
									
									 String dsName = p.createTempTableWithDefaultData(conn,testConn,assignmentId,questionId,course_id,defaultDSIdsPerQuestion[dId].toString());
									 Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,defaultDSIdsPerQuestion[dId].toString(), sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
									 
									 for(int l=0;l<killedMutants.size();l++){
											if(mutants.containsKey(killedMutants.get(l))){
												logger.log(Level.INFO,"false" +killedMutants.get(l));
												incorrect = true;
												//Get failed datasets
												//matchAllFailedForDS = datasets.get(i);	
												failedDataSets.setStatus("Failed");
												failedDataSets.getDataSetIdList().add(defaultDSIdsPerQuestion[dId].toString());
												failedDataSets.setInstrQuery(instrQuery);
											}
										}
					
										if(!incorrect){
											//isDataSetVerified.add(true);
										}	
										else{											
											resultOfDatasetMatchForEachQuery.put(queryId,false);
											logger.log(Level.INFO,"");
										}	
										
										if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
											resultOfDatasetMatchForEachQuery.put(queryId,true);
										}
									 p.deleteAllTablesFromTestUser(testConn);
										
								}
								p.deleteAllTempTablesFromTestUser(testConn);
							}
							if((defaultDSIdsPerQuestion == null || (defaultDSIdsPerQuestion != null && defaultDSIdsPerQuestion.length == 0))
									&&  defaultDSIdsAssignment != null){
								p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
								for(int dId= 0; dId < defaultDSIdsAssignment.length;dId++){
									Vector<String> cmismatch = new Vector<String>();
									logger.log(Level.INFO,"******************");
									logger.log(Level.INFO,"Default dataset "+defaultDSIdsAssignment[dId]+" Loaded : ");
									logger.log(Level.INFO,"******************");
									 String dsName = p.createTempTableWithDefaultData(conn,testConn,assignmentId,questionId,course_id,defaultDSIdsAssignment[dId].toString());
									 Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,defaultDSIdsAssignment[dId].toString(), sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
									 
									 for(int l=0;l<killedMutants.size();l++){
											if(mutants.containsKey(killedMutants.get(l))){
												logger.log(Level.INFO,"false" +killedMutants.get(l));
												incorrect = true;
												//Get failed datasets
												//matchAllFailedForDS = datasets.get(i);	
												failedDataSets.setStatus("Failed");
												failedDataSets.getDataSetIdList().add(defaultDSIdsAssignment[dId].toString());
												failedDataSets.setInstrQuery(instrQuery);
											}
										}
					
										if(!incorrect){
											//isDataSetVerified.add(true);
										}	
										else{											
											resultOfDatasetMatchForEachQuery.put(queryId,false);
											logger.log(Level.INFO,"");
										}	
										
										if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
											resultOfDatasetMatchForEachQuery.put(queryId,true);
										}
									 
										p.deleteAllTablesFromTestUser(testConn);
								}
								p.deleteAllTempTablesFromTestUser(testConn);
							}
								Map <Integer,Vector<String>>  datasetForQueryMap =  downloadDatasets(assignmentId,questionId, queryId,course_id, conn, filePath, false);
								//For given query, fetch datasets, if data sets are empty - load default sample data 1 and run the query
								if(datasetForQueryMap.isEmpty()  && defaultDSIdsAssignment == null && defaultDSIdsPerQuestion == null){
								
								 //Load the default sample data file
								boolean flag=true;
								Vector<String> cmismatch = new Vector<String>();
								try{
									 p.deleteAllTempTablesFromTestUser(testConn);
									}catch(Exception e){
										logger.log(Level.INFO,"Temporary Table does not exist",e);
										
									}
									try{
									 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
									}catch(Exception e){
										logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
										throw e;
									}
									Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, "DS_Default", sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
									logger.log(Level.INFO,"******************");
									logger.log(Level.INFO,"Default dataset Loaded : " + " " + killedMutants.size());
									logger.log(Level.INFO,"******************");
									for(int l=0;l<killedMutants.size();l++){
										if(mutants.containsKey(killedMutants.get(l))){
											logger.log(Level.INFO,"false" +killedMutants.get(l));
											incorrect = true;
											//Get failed datasets
											//matchAllFailedForDS = datasets.get(i);	
											failedDataSets.setStatus("Failed");
											failedDataSets.getDataSetIdList().add("DS_Default");
											failedDataSets.setInstrQuery(instrQuery);
										}
									}
				
									if(!incorrect){
										//isDataSetVerified.add(true);
									}	
									else{
										
										resultOfDatasetMatchForEachQuery.put(queryId,false);
										logger.log(Level.INFO,"");
									}	
									
									if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
										resultOfDatasetMatchForEachQuery.put(queryId,true);
									}
								}else{
									for(Integer id : datasetForQueryMap.keySet()){
										
									 Vector<String> datasets = datasetForQueryMap.get(id);
									 		//For each Xdata generated datasets, load them in temp table and test student and instructor query 
										 for(int i = 0; i < datasets.size(); i++){
											boolean flag = true;
											
											//load the contents of DS
											String dsPath = Configuration.homeDir + "/temp_cvc" + filePath + "/" + datasets.get(i);
											File ds=new File(dsPath);
										 	//GenerateCVC1 c = new GenerateCVC1();
											String copyFiles[] = ds.list();
											
											Vector<String> vs = new Vector<String>();
											for(int m=0;m<copyFiles.length;m++){
											    vs.add(copyFiles[m]);
											}
											
											Pattern pattern = Pattern.compile("^A([0-9]+)Q[0-9]+");
											Matcher matcher = pattern.matcher(qId);
											int assignId = 1;
											
											if (matcher.find()) {
											    assignId = Integer.parseInt(matcher.group(1));
											}
											
											
											p.populateTestDataForTesting(vs, filePath + "/" + datasets.get(i), tm, testConn, assignId, questionId);
											Vector<String> cmismatch=new Vector<String>();
											Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, datasets.get(i), sqlQuery, dsPath, orderIndependent, cmismatch, testConn);
											
											for(int l=0;l<killedMutants.size();l++){
												if(mutants.containsKey(killedMutants.get(l))){
													logger.log(Level.INFO,"false" +killedMutants.get(l));
													incorrect = true;
													//Get failed datasets
													//matchAllFailedForDS = datasets.get(i);	
													failedDataSets.setStatus("Failed");
													failedDataSets.getDataSetIdList().add(datasets.get(i));
													failedDataSets.setInstrQuery(instrQuery);
												}
											}
						
											if(!incorrect){
												//isDataSetVerified.add(true);
											}	
											else{
												//isDataSetVerified.add(false);
												//If query fails, add the status of query to false
												resultOfDatasetMatchForEachQuery.put(queryId,false);
												 //  out+= matchAllFailedForDS+"::Failed:::";
												logger.log(Level.INFO,"");			
											}
											
											cvc.closeConn();
										 }
									
									}
								//resultOfDatasetMatchForEachQuery.put(queryId,isDataSetVerified);
								//If there are no items in resultOfDatasetMatchForEachQuery for given queryId
								//then the query is "Correct and passed" - so set TRUE
								if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
									resultOfDatasetMatchForEachQuery.put(queryId,true);
								}
								}//Else if data exists part ends
								
								
								
					}//While next ResultSet
					
					}//close resultset try block
				//  *******Check for Match all or match One Option Start //
				int query_id = 0;
			
				for(int i : resultOfDatasetMatchForEachQuery.keySet()){
					//Get first query's result as default
					if(i == 1){
						isQueryPass = resultOfDatasetMatchForEachQuery.get(i);
					}//For second query, get the query result
					boolean isCorrect = resultOfDatasetMatchForEachQuery.get(i);
					//If instructor selects match All result sets while creating the question,
					//then all query results should hold true for the student query to pass
					if(isMatchAll){
						isQueryPass = isCorrect && isQueryPass;	
					}
					else{
						 //This excutes if instructor selects Match any result set option
						//If even one query result holds true, the student query is considered as pass
						 if(isCorrect){
							 isQueryPass = true;
							 break;
						 }
					}			
				} 
				if(isQueryPass){
					logger.log(Level.INFO,"Question passed the datasets expected");
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = true where assignment_id ='"+assignmentId+"' and question_id = '"+questionId+"' and rollnum = '"+user+"' and course_id='"+course_id+"'";
					PreparedStatement pstmt3 = conn.prepareStatement(qryUpdate);
					pstmt3.executeUpdate();
					pstmt3.close();
					out+="Passed:::";
				}
				else{
					String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id ='"+ assignmentId+"' and question_id = '"+questionId+"' and rollnum = '"+user+"' and course_id='"+course_id+"'";
					PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate);
					//pstmt2.setString(1,out.trim());
					pstmt2.executeUpdate();
				
				}
			}
		}//close connection try block
	}//close connection try block
			return failedDataSets;
			
			
}

/**
 * This method gets the default Datasets at assignment level
 * 
 * @param assignmentId
 * @param course_id
 * @param conn
 * @return
 * @throws Exception
 */
public String [] getDefaultDSForAssignment(int assignmentId,String course_id,Connection conn) throws Exception{
	String [] defaultDSIdsAssignment = new String[25];
	//Get default dataset Ids from assignment table
	String getDefaultDataSets = "select defaultDSetId from xdata_assignment where assignment_id = ? and course_id=?";
	try(PreparedStatement pst =conn.prepareStatement(getDefaultDataSets) ){
		pst.setInt(1,assignmentId);
		pst.setString(2,course_id);
		try(ResultSet rset = pst.executeQuery()){
			rset.next();
				String dsIds = rset.getString("defaultDSetId");
				//MODIFIED by ANURAG
				//dsIds = "[\""+dsIds+"\"]";
				Gson gson = new Gson();
				Type listType = new TypeToken<String[]>() {}.getType();
				defaultDSIdsAssignment = new Gson().fromJson(dsIds, listType);
			
		}
	}catch(Exception e){
		logger.log(Level.SEVERE,"Exception caught here: "+e.getMessage(),e);
		e.printStackTrace();
		throw e;
	}
	return defaultDSIdsAssignment;
	
}

/**
 * This method gets the default datasets that are at each instructor question level
 * 
 * @param assignmentId
 * @param questionId
 * @param course_id
 * @param conn
 * @return
 * @throws Exception
 */
public String[] getDefaultDSForQuestion(int assignmentId,int questionId,String course_id,Connection conn) throws Exception{
	String[] defaultDSIdsPerQuestion = new String[25];
	String getDefaultDataSetsForQuestion = "select default_sampledataid from xdata_qinfo where assignment_id = ? and course_id=? and question_id=?";
	try(PreparedStatement pst =conn.prepareStatement(getDefaultDataSetsForQuestion) ){
		pst.setInt(1,assignmentId);
		pst.setString(2,course_id);
		pst.setInt(3, questionId);
		try(ResultSet rset = pst.executeQuery()){
			rset.next();
				String dsIds = rset.getString("default_sampledataid");

				Gson gson = new Gson();
				Type listType = new TypeToken<String[]>() {}.getType();
				defaultDSIdsPerQuestion = new Gson().fromJson(dsIds, listType);
			
		}
	}
	catch(Exception e){
		logger.log(Level.SEVERE,"Exception caught here: "+e.getMessage(),e);
		e.printStackTrace();
		throw e;
	}
	return defaultDSIdsPerQuestion;
}
/**
 * This method tests the student queries against SQL query submitted by the instructor 
 * for evaluation
 * 
 * Same method is used for evaluating in interactive mode also.
 * 
 * @param assignmentId
 * @param questionId
 * @param query
 * @param user
 * @param filePath
 * @return
 * @throws Exception
 */
//public String testAnswer(int assignmentId,int questionId, String query, String user, String filePath) throws Exception{
public FailedDataSetValues testAnswer(int assignmentId,int questionId, String course_id,
	String query, String user, String filePath, boolean isLateSubmission, String studRole) throws Exception{
	
	FailedDataSetValues failedDataSets = new FailedDataSetValues();
	int queryId = 1;
	String qId = "A"+assignmentId +"Q"+questionId+"S"+queryId;
	ArrayList <Boolean> isDataSetVerified = null;
	boolean isMatchAll = false;
	boolean orderIndependent = false;
	String matchAllFailedForDS = "";
	boolean isQueryPass = false;
	String out="";
	int maxMarks = 0;
	int reduceLateSubmissionMarks = 0;
	int index = 0;
	String [] defaultDSIdsAssignment=new String[25];
	String [] defaultDSIdsPerQuestion=new String[25];
	//First instructor answer will be considered to show failed datasets
	String instrQuery = "";
	Map <Integer,Boolean> resultOfDatasetMatchForEachQuery  = new HashMap<Integer, Boolean>();
	try(Connection conn = MyConnection.getDatabaseConnection()){
		try(Connection testConn = new DatabaseConnection().getTesterConnection(assignmentId)){
			
			String getMatchAllOption = "select matchallqueries,orderIndependent,latesubmissionmarks from xdata_qinfo where assignment_id = ? and question_id = ? and course_id=?";
			try(PreparedStatement pst = conn.prepareStatement(getMatchAllOption)){
				pst.setInt(1,assignmentId);
				pst.setInt(2,questionId);
				pst.setString(3,course_id);
				try(ResultSet rset = pst.executeQuery()){
					rset.next(); 
					isMatchAll = rset.getBoolean("matchallqueries");
					orderIndependent = rset.getBoolean("orderIndependent");
					reduceLateSubmissionMarks = rset.getInt("latesubmissionmarks");
				}///close resultset try block
			}//close stmt try blck
			
			defaultDSIdsAssignment = this.getDefaultDSForAssignment(assignmentId,course_id,conn);
			defaultDSIdsPerQuestion = this.getDefaultDSForQuestion(assignmentId,questionId,course_id,conn);
			
			String qry = "select * from xdata_instructor_query a inner join xdata_qinfo b on a.assignment_id = b.assignment_id and " +
					"a.question_id = b.question_id  where a.assignment_id = ?" +
					" and a.question_id = ? and a.course_id=?";
			try(PreparedStatement pstmt = conn.prepareStatement(qry)){
			pstmt.setInt(1,assignmentId);
			pstmt.setInt(2,questionId); 
			pstmt.setString(3,course_id);
			try(ResultSet rs = pstmt.executeQuery()){
				PopulateTestData p = new PopulateTestData();
				
				GenerateCVC1 cvc = new GenerateCVC1();
				cvc.initializeConnectionDetails(assignmentId,questionId,queryId,course_id);
				TableMap tm = cvc.getTableMap();
				cvc.closeConn();	
			//For each instructor answer loop to compare datasets
			while(rs.next()){
				
				boolean incorrect=false;
				isDataSetVerified = new ArrayList<Boolean>();
				
				String sqlQuery=rs.getString("sql");
				queryId = rs.getInt("query_id");
				query=checkForViews(query,user);
				maxMarks = rs.getInt("totalmarks");
				
				if(index == 0){
					instrQuery = sqlQuery;
				}
				
				qId = "A"+assignmentId +"Q"+questionId+"S"+queryId;
				HashMap<String,String> mutants = new HashMap<String,String>();
				mutants.put(qId, query);
				index++;
				
				try{
					 p.deleteAllTempTablesFromTestUser(testConn);
					}catch(Exception e){
						logger.log(Level.INFO,"Temporary Table does not exist",e);
						throw e;
					}
					try{
					 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
					}catch(Exception e){
						logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
						throw e;
					}
				 
				  //First get default sample data sets for the instructor answer, if it is not there, then get
				  // it from assignment table. If both are not there, don't set anythng - proceed with other code
				  
				try{
				if(defaultDSIdsPerQuestion != null){
					
					for(int dId= 0; dId < defaultDSIdsPerQuestion.length;dId++){
						Vector<String> cmismatch = new Vector<String>();
						logger.log(Level.INFO,"******************");
						logger.log(Level.INFO,"Default dataset "+defaultDSIdsPerQuestion[dId]+" Loaded : ");
						logger.log(Level.INFO,"******************");
						
						 String dsName = p.createTempTableWithDefaultData(conn,testConn,assignmentId,questionId,course_id,defaultDSIdsPerQuestion[dId].toString());
						 Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,defaultDSIdsPerQuestion[dId].toString(), sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
						 
						 resultOfDatasetMatchForEachQuery = this.processResult(failedDataSets,killedMutants, mutants, incorrect, instrQuery,queryId,resultOfDatasetMatchForEachQuery,defaultDSIdsPerQuestion[dId].toString());
						 if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
								resultOfDatasetMatchForEachQuery.put(queryId,true);
							}						
					}
					p.deleteAllTempTablesFromTestUser(testConn);
				}
				}catch(Exception e){
					logger.log(Level.SEVERE,"Exception caught here: "+e.getMessage(),e);
					e.printStackTrace();
				}
				
				//Load default datasets for assignment 				 
				try{
						if((defaultDSIdsPerQuestion == null || (defaultDSIdsPerQuestion != null && defaultDSIdsPerQuestion.length == 0))
								&&  defaultDSIdsAssignment != null){
							
							for(int dId= 0; dId < defaultDSIdsAssignment.length;dId++){
								Vector<String> cmismatch = new Vector<String>();
								logger.log(Level.INFO,"******************");
								logger.log(Level.INFO,"Default dataset "+defaultDSIdsAssignment[dId]+" Loaded : ");
								logger.log(Level.INFO,"******************");
								
								 String dsName = p.createTempTableWithDefaultData(conn,testConn,assignmentId,questionId,course_id,defaultDSIdsAssignment[dId].toString());
								 Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,defaultDSIdsAssignment[dId].toString(), sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
								 
								 resultOfDatasetMatchForEachQuery = this.processResult(failedDataSets,killedMutants, mutants, incorrect, instrQuery,queryId,resultOfDatasetMatchForEachQuery,defaultDSIdsAssignment[dId].toString());
								 if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
										resultOfDatasetMatchForEachQuery.put(queryId,true);
									}
							}
							p.deleteAllTempTablesFromTestUser(testConn);
						}	
				}catch(Exception e){
					logger.log(Level.SEVERE,"Exception caught here: "+e.getMessage(),e);
					e.printStackTrace();
				}
				
				try{
						Map <Integer,Vector<String>>  datasetForQueryMap =	downloadDatasets(assignmentId,questionId,queryId,course_id,conn,filePath, false);
						//Even if no default data sets are there and no datasets are available for the query, , check against the sample Data file that the assignment uses.
						
						//Check if this method is executed, else remove this if loop.
						if(datasetForQueryMap.isEmpty() && defaultDSIdsAssignment == null 
								&& defaultDSIdsPerQuestion == null ){
							 //Load the default sample data file
							/*boolean flag=true;
							Vector<String> cmismatch = new Vector<String>();
						
							Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, "DS_Default", sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
							logger.log(Level.INFO,"******************");
							logger.log(Level.INFO,"Default dataset Loaded : " + " " + killedMutants.size());
							logger.log(Level.INFO,"******************");
							resultOfDatasetMatchForEachQuery = this.processResult(failedDataSets,killedMutants, mutants, incorrect, instrQuery,queryId,resultOfDatasetMatchForEachQuery);
							
							if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
								resultOfDatasetMatchForEachQuery.put(queryId,true);
							}*/
						}
							//Load all generated datasets and evaluate instructor against that.
						else{
								for(Integer id : datasetForQueryMap.keySet()){
									
									Vector<String> datasets = datasetForQueryMap.get(id);
									for(int i=0;i<datasets.size();i++){
									 	
										boolean flag=true;
										//load the contents of DS
										String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
										File ds=new File(dsPath);
										String copyFiles[] = ds.list();
										
										Vector<String> vs = new Vector<String>();
										for(int m=0;m<copyFiles.length;m++){
										    vs.add(copyFiles[m]);
										 }				 		
									 	// query output handling
										Pattern pattern = Pattern.compile("^A([0-9]+)Q[0-9]+");
										Matcher matcher = pattern.matcher(qId);
										int assignId = 1;
										if (matcher.find()) {
										    assignId = Integer.parseInt(matcher.group(1));
										}
					
										p.populateTestDataForTesting(vs, filePath+"/"+datasets.get(i), tm, testConn, assignmentId, questionId);
										Vector<String> cmismatch=new Vector<String>();
										Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,datasets.get(i), sqlQuery, filePath,orderIndependent,cmismatch,testConn);			
										resultOfDatasetMatchForEachQuery = this.processResult(failedDataSets,killedMutants, mutants, incorrect, instrQuery,queryId,resultOfDatasetMatchForEachQuery,datasets.get(i));
										 }		   
								  }
									//If there are no items in resultOfDatasetMatchForEachQuery for given queryId
									//then the query id
									if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
										resultOfDatasetMatchForEachQuery.put(queryId,true);
									}
									logger.log(Level.INFO,"******************");
									logger.log(Level.INFO,"Student Id : "+user+" evaluated against DS Id: ");
									logger.log(Level.INFO,"******************");
						}//Else data exists part ends
				}catch(Exception e){
				logger.log(Level.SEVERE,"Exception caught here: "+e.getMessage(),e);
				e.printStackTrace();
			}
			}
			}//close resultset try block
		}//close statement try block
			
		/*******Check for Match all or match One Option Start******/
		isQueryPass = this.getMatchForAllQueries(resultOfDatasetMatchForEachQuery,isQueryPass,isMatchAll);
	      //Set Marks for the failedStudentQuery
			failedDataSets = this.getMarkDetails(conn,failedDataSets, isQueryPass,studRole,assignmentId,questionId,course_id,query,user,isLateSubmission,maxMarks,reduceLateSubmissionMarks);
			return failedDataSets;
			
		}// try block for TestConn ends
	}//try block for Conn ends 
}

/**
 * This method checks if the student query passes all instructor query datasets or any one intructor answer
 * 
 * @param resultOfDatasetMatchForEachQuery
 * @param isQueryPass
 * @param isMatchAll
 * @return
 */
public boolean getMatchForAllQueries(Map<Integer,Boolean> resultOfDatasetMatchForEachQuery, boolean isQueryPass, boolean isMatchAll){
	for(int i : resultOfDatasetMatchForEachQuery.keySet()){
		//Get first query's result as default
		if(i == 1){
			isQueryPass = resultOfDatasetMatchForEachQuery.get(i);
		}//For second query, get the query result
		boolean isCorrect = resultOfDatasetMatchForEachQuery.get(i);
		//If instructor selects match All result sets while creating the question,
		//then all query results should hold true for the student query to pass
		if(isMatchAll){
			isQueryPass = isCorrect && isQueryPass;	
		}
		else{ 
			 //This excutes if instructor selects Match any result set option
			//If even one query result holds true, the student query is considered as pass
			 if(isCorrect){
				 isQueryPass = true;
				 break;
			 }
		}
	
	}
	return isQueryPass;
}

/**
 * This method processes the result of checking the instructor and student query equivalence
 * 
 * @param failedDataSets
 * @param killedMutants
 * @param mutants
 * @param incorrect
 * @param instrQuery
 * @param queryId
 * @param resultOfDatasetMatchForEachQuery
 * @return
 */
public Map<Integer,Boolean> processResult(FailedDataSetValues failedDataSets,Vector<String> killedMutants, HashMap<String,String> mutants, 
		boolean incorrect, String instrQuery,int queryId, Map <Integer,Boolean> resultOfDatasetMatchForEachQuery, String dataSetId ){
	
	
	for(int l=0;l<killedMutants.size();l++){
		if(mutants.containsKey(killedMutants.get(l))){
			incorrect =true;
			logger.log(Level.INFO," Eavaluation Failed for queryId :" +killedMutants.get(l));
			//out+=datasets.get(i)+"::Failed:::";
			failedDataSets.setStatus("Failed");
			failedDataSets.getDataSetIdList().add(dataSetId);
			failedDataSets.setInstrQuery(instrQuery);
		}
	}
		if(!incorrect){
			//isDataSetVerified.add(true);
		}	
		else{
			//If query fails, add the status of query to false
			resultOfDatasetMatchForEachQuery.put(queryId,false);
			logger.log(Level.INFO,"");			
		}
		
		return resultOfDatasetMatchForEachQuery;
}

/**
 * This method calls the partial marking part to calculate partial marks for failed queries. 
 * If the query is correct, it awards 100 marks and updates the DB with same.
 * Marks for late submission are detected based on lateSubmissionFlag.
 * 
 * @param conn
 * @param failedDataSets
 * @param isQueryPass
 * @param studRole
 * @param assignmentId
 * @param questionId
 * @param course_id
 * @param query
 * @param user
 * @param isLateSubmission
 * @param maxMarks
 * @param reduceLateSubmissionMarks
 * @return
 * @throws Exception
 */
public FailedDataSetValues getMarkDetails(Connection conn, FailedDataSetValues failedDataSets, boolean isQueryPass, String studRole, 
		int assignmentId, int questionId,String course_id,String query,
		String user,boolean isLateSubmission, int maxMarks,
					float reduceLateSubmissionMarks) throws Exception{

	MarkInfo markInfo = new MarkInfo();
	try{
		if(isQueryPass){
			logger.log(Level.INFO,"Question passed the datasets expected");
			if(!studRole.equals("guest")){
				String qryUpdate = "update xdata_student_queries set verifiedcorrect = true where assignment_id ='"+assignmentId+"' and question_id = '"+questionId+"' and rollnum = '"+user+"' and course_id='"+course_id+"'";
				try(PreparedStatement pstmt3 = conn.prepareStatement(qryUpdate)){
					pstmt3.executeUpdate();
				}
			}
			//out+="Passed:::";
			
			//If late submission, reduce marks as given in instructor query
			if(isLateSubmission){
				markInfo.Marks = maxMarks - reduceLateSubmissionMarks;
			}else{
				markInfo.Marks = maxMarks;
			}
			failedDataSets.setMaxMarks(maxMarks);
			failedDataSets.setMarks(markInfo.Marks);
		}
		else{
			if(!studRole.equals("guest")){
				String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id ='"+ assignmentId+"' and question_id = '"+questionId+"' and rollnum = '"+user+"' and course_id='"+course_id+"'";		
				try(PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate)){
				//pstmt2.setString(1,out.trim());
					pstmt2.executeUpdate(); 
				}
			}
			// Initiate partial marking
			String qry = "select * from xdata_instructor_query where assignment_id = ? and question_id = ? and course_id= ?";
			try(PreparedStatement pstmt = conn.prepareStatement(qry)){
				pstmt.setInt(1,assignmentId);
				pstmt.setInt(2,questionId);
				pstmt.setString(3,course_id);
				try(ResultSet rs = pstmt.executeQuery()){
					while(rs.next()){	
						int queryId = rs.getInt("query_id");
						try{
							PartialMarker marker = new PartialMarker(assignmentId, questionId, queryId,course_id,user,query);
							if(!studRole.equals("guest")){
								
							}else{
								
							}
							MarkInfo result = marker.getMarks();
							if(result.Marks > markInfo.Marks)
								markInfo = result;
								
							//markInfo.Marks = 0;
						}
						catch(Exception ex){
							logger.log(Level.SEVERE,ex.getMessage(), ex);
							ex.printStackTrace();
						}		
					}
				}//close resultset try
			}//close connection try
		}		
			Gson gson = new Gson();
			String info = gson.toJson(markInfo);
			if(!studRole.equals("guest")){
				DatabaseHelper.InsertIntoScores(conn, assignmentId, questionId, 1, course_id, maxMarks, user, info, markInfo.Marks);
			}
			failedDataSets.setMaxMarks(maxMarks);
			failedDataSets.setMarks(markInfo.Marks);
	}catch(Exception e){
		logger.log(Level.SEVERE, e.getMessage(),e);
	}
		return failedDataSets;
}
/**
 * This method checks the multiple SQL queries for the equivalence and gives the dataset in which the 
 * queries are not equivalent 
 * 
 */
public Map<Integer,FailedDataSetValues> checkQueryEquivalence(int assignmentId,int questionId, String course_id) throws Exception{
	FailedDataSetValues failedDataSets = new FailedDataSetValues();
	Map<Integer,FailedDataSetValues> failedDataSetsPerQuery = new HashMap<Integer,FailedDataSetValues>();
	String filePath = "";
	String [] defaultDSIdsAssignment=new String[25];
	String [] defaultDSIdsPerQuestion=new String[25];
	ArrayList<Boolean> isDataSetVerified = null;
	HashMap<String,String> mutants = new HashMap<String,String>();
	boolean incorrect = false;
	String query =  "";
	int queryId = 0;
	String qId = "";
	int index =0;
	String out="";
	String instrQuery = "";
	//ArrayList<FailedDataSetValues> listOfFailedDS = new ArrayList<FailedDataSetValues>();
	Map <Integer,Vector<String>> dataSetForQueries = new HashMap<Integer,Vector<String>>();
	
	Map <Integer,Boolean> resultOfDatasetMatchForEachQuery  = new HashMap<Integer, Boolean>();
	
	try(Connection conn = MyConnection.getDatabaseConnection()){
		
		try(Connection testConn = new DatabaseConnection().getTesterConnection(assignmentId)){
			
		//Get default dataset Ids from assignment table
		String getDefaultDataSets = "select defaultDSetId from xdata_assignment where assignment_id = ? and course_id=?";
		try(PreparedStatement pst =conn.prepareStatement(getDefaultDataSets) ){
			pst.setInt(1,assignmentId);
			pst.setString(2,course_id);
			try(ResultSet rset = pst.executeQuery()){
				rset.next();
					String dsIds = rset.getString("defaultDSetId");
					Gson gson = new Gson();
					Type listType = new TypeToken<String[]>() {}.getType();
					defaultDSIdsAssignment = new Gson().fromJson(dsIds, listType);
				
			}
		}
		
		String getDefaultDataSetsForQuestion = "select default_sampledataid from xdata_qinfo where assignment_id = ? and course_id=? and question_id=?";
		try(PreparedStatement pst =conn.prepareStatement(getDefaultDataSetsForQuestion) ){
			pst.setInt(1,assignmentId);
			pst.setString(2,course_id);
			pst.setInt(3, questionId);
			try(ResultSet rset = pst.executeQuery()){
				rset.next();
					String dsIds = rset.getString("default_sampledataid");
					Gson gson = new Gson();
					Type listType = new TypeToken<String[]>() {}.getType();
					defaultDSIdsPerQuestion = new Gson().fromJson(dsIds, listType);
				
			}
		}
		String qry = "select * from xdata_instructor_query a inner join xdata_qinfo b on a.assignment_id = b.assignment_id and " +
				"a.question_id = b.question_id where a.assignment_id = ?" +
				" and a.question_id = ? and a.course_id=?";
		try(PreparedStatement pstmt = conn.prepareStatement(qry)){
			pstmt.setInt(1,assignmentId);
			pstmt.setInt(2,questionId); 
			pstmt.setString(3,course_id);
			try(ResultSet rs = pstmt.executeQuery()){
				PopulateTestData p = new PopulateTestData();
				
			//For each SQL answer loop to compare datasets
			while(rs.next()){
				 incorrect=false;
				isDataSetVerified = new ArrayList<Boolean>();
				
				String sqlQuery=rs.getString("sql");
				queryId = rs.getInt("query_id");
				qId = "A"+assignmentId +"Q"+questionId+"S"+queryId;
				filePath="4/"+course_id+"/"+qId;
				if(index ==0){
					instrQuery = sqlQuery;
				}else{
					mutants.put(qId, sqlQuery);
				}
				index++;
				dataSetForQueries = downloadDataSetForAllQueries(assignmentId, questionId, queryId, course_id, testConn, dataSetForQueries);
			}
			
			GenerateCVC1 cvc = new GenerateCVC1();
			cvc.initializeConnectionDetails(assignmentId,questionId,queryId,course_id);
			TableMap tm = cvc.getTableMap();
			cvc.closeConn();
			/********run the queries against defaut data set for the application ********/
			if(defaultDSIdsPerQuestion != null){
				try{
					 p.deleteAllTempTablesFromTestUser(testConn);
					}catch(Exception e){
						logger.log(Level.INFO,"Temporary Table does not exist",e);
						
					}
					try{
					 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
					}catch(Exception e){
						logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
						throw e;
					}
				for(int dId= 0; dId < defaultDSIdsPerQuestion.length;dId++){
					Vector<String> cmismatch = new Vector<String>();
					logger.log(Level.INFO,"******************");
					logger.log(Level.INFO,"Default dataset "+defaultDSIdsPerQuestion[dId]+" Loaded : ");
					logger.log(Level.INFO,"******************");
					
					 String dsName = p.createTempTableWithDefaultData(conn,testConn,assignmentId,questionId,course_id,defaultDSIdsPerQuestion[dId].toString());
					 Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,defaultDSIdsPerQuestion[dId].toString(), instrQuery,"NoPath",true, cmismatch, testConn);
					 
					 for(int l=0;l<killedMutants.size();l++){
							if(mutants.containsKey(killedMutants.get(l))){
								logger.log(Level.INFO,"false" +killedMutants.get(l));
								incorrect = true;
								//Get failed datasets
								//matchAllFailedForDS = datasets.get(i);
								
								failedDataSets.setStatus("Failed");
								failedDataSets.getDataSetIdList().add(defaultDSIdsPerQuestion[dId].toString());
								failedDataSets.setInstrQuery(instrQuery);
							}
						}
	
						if(!incorrect){
							//isDataSetVerified.add(true);
						}	
						else{											
							resultOfDatasetMatchForEachQuery.put(queryId,false);
							logger.log(Level.INFO,"");
						}	
						
						if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
							resultOfDatasetMatchForEachQuery.put(queryId,true);
						}
						//listOfFailedDS.add(failedDataSets);
						failedDataSetsPerQuery.put(0, failedDataSets);
					 p.deleteAllTablesFromTestUser(testConn);
						
				}
				p.deleteAllTempTablesFromTestUser(testConn);
			}
			/******Run each query against the default data set for the question **********/
			if((defaultDSIdsPerQuestion == null || (defaultDSIdsPerQuestion != null && defaultDSIdsPerQuestion.length == 0))
					&&  defaultDSIdsAssignment != null){
				try{
					 p.deleteAllTempTablesFromTestUser(testConn);
					}catch(Exception e){
						logger.log(Level.INFO,"Temporary Table does not exist",e);
						
					}
					try{
					 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
					}catch(Exception e){
						logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
						throw e;
					}
				for(int dId= 0; dId < defaultDSIdsAssignment.length;dId++){
					Vector<String> cmismatch = new Vector<String>();
					logger.log(Level.INFO,"******************");
					logger.log(Level.INFO,"Default dataset "+defaultDSIdsAssignment[dId]+" Loaded : ");
					logger.log(Level.INFO,"******************");
					
					 String dsName = p.createTempTableWithDefaultData(conn,testConn,assignmentId,questionId,course_id,defaultDSIdsAssignment[dId].toString());
					 Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,defaultDSIdsAssignment[dId].toString(), instrQuery,"NoPath", true, cmismatch, testConn);
					 
					 for(int l=0;l<killedMutants.size();l++){
							if(mutants.containsKey(killedMutants.get(l))){
								logger.log(Level.INFO,"false" +killedMutants.get(l));
								incorrect = true;
								//Get failed datasets
								//matchAllFailedForDS = datasets.get(i);	
								failedDataSets.setStatus("Failed");
								failedDataSets.getDataSetIdList().add(defaultDSIdsAssignment[dId].toString());
								failedDataSets.setInstrQuery(instrQuery);
							}
						}
					 if(!incorrect){
							//isDataSetVerified.add(true);
						}else{											
							resultOfDatasetMatchForEachQuery.put(queryId,false);
							logger.log(Level.INFO,"");
						}	
						if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
							resultOfDatasetMatchForEachQuery.put(queryId,true);
						}
						//listOfFailedDS.add(failedDataSets);
						failedDataSetsPerQuery.put(0, failedDataSets);
					 p.deleteAllTablesFromTestUser(testConn);
				}
				p.deleteAllTempTablesFromTestUser(testConn);
			}	// Default data set for question level ends
			
			/************************************************************/
			/*******Get the datasets for each query and run them *******/
			Map <Integer,Vector<String>>  datasetForQueryMap =	downloadDataSetForAllQueries(assignmentId,questionId,queryId,course_id,conn,dataSetForQueries);
			if(datasetForQueryMap.isEmpty() && defaultDSIdsAssignment == null 
					&& defaultDSIdsPerQuestion == null ){
				//Load the default sample data file
				boolean flag=true;
				Vector<String> cmismatch = new Vector<String>();
				try{
					 p.deleteAllTempTablesFromTestUser(testConn);
					}catch(Exception e){
						logger.log(Level.INFO,"Temporary Table does not exist",e);
						
					}
					try{
					 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
					}catch(Exception e){
						logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
						throw e;
					} 
				Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, "DS_Default", instrQuery,"NoPath", true, cmismatch, testConn);
					logger.log(Level.INFO,"******************");
					logger.log(Level.INFO,"Default dataset Loaded : " + " " + killedMutants.size());
					logger.log(Level.INFO,"******************");
					for(int l=0;l<killedMutants.size();l++){
						if(mutants.containsKey(killedMutants.get(l))){
							incorrect =true;
							logger.log(Level.INFO,"false" +killedMutants.get(l));
							out+="Default Data"+"::Failed:::";
							failedDataSets.setStatus("Failed");
							failedDataSets.getDataSetIdList().add("DS_Default");
							failedDataSets.setInstrQuery(instrQuery);
						}
					}
					if(!incorrect){
						//isDataSetVerified.add(true);
					}else{
						resultOfDatasetMatchForEachQuery.put(queryId,false);
						logger.log(Level.INFO,"");
					}	
					//listOfFailedDS.add(failedDataSets);
					failedDataSetsPerQuery.put(0, failedDataSets);
					if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
						resultOfDatasetMatchForEachQuery.put(queryId,true);
					}
			}else{

				for(Integer id : datasetForQueryMap.keySet()){
					failedDataSets  = new FailedDataSetValues();
					 Vector<String> datasets = datasetForQueryMap.get(id);
					 filePath = "4/"+course_id+"/A"+assignmentId+"Q"+questionId+"S"+id;
					 failedDataSets.setQuery_id(id);
				for(int i=0;i<datasets.size();i++){
				 	
					boolean flag=true;
					//load the contents of DS
					String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
					File ds=new File(dsPath);
					String copyFiles[] = ds.list();
					
					Vector<String> vs = new Vector<String>();
					for(int m=0;m<copyFiles.length;m++){
					    vs.add(copyFiles[m]);
					 }				 		
				 	// query output handling
					
					Pattern pattern = Pattern.compile("^A([0-9]+)Q[0-9]+");
					Matcher matcher = pattern.matcher(qId);
					int assignId = 1;
					
					if (matcher.find()) {
					    assignId = Integer.parseInt(matcher.group(1));
					}

					p.populateTestDataForTesting(vs, filePath+"/"+datasets.get(i), tm, testConn, assignmentId, questionId);
					Vector<String> cmismatch=new Vector<String>();
					Vector<String> killedMutants = checkAgainstOriginalQuery(mutants,datasets.get(i), instrQuery, filePath,true,cmismatch,testConn);
					for(int l=0;l<killedMutants.size();l++){
						if(mutants.containsKey(killedMutants.get(l))){
							incorrect =true;
							logger.log(Level.INFO,"false" +killedMutants.get(l));
							out+=datasets.get(i)+"::Failed:::";
							failedDataSets.setStatus("Failed");
							failedDataSets.getDataSetIdList().add(datasets.get(i));
							failedDataSets.setInstrQuery(instrQuery);
						}
					}
						if(!incorrect){
							//isDataSetVerified.add(true);
						}	
						else{
							//If query fails, add the status of query to false
							resultOfDatasetMatchForEachQuery.put(queryId,false);
							logger.log(Level.INFO,"");			
						}
						
					 }	
				//listOfFailedDS.add(failedDataSets);
						failedDataSetsPerQuery.put(id, failedDataSets);
					}
				//Used when more than one correct answer is specified
				//This will store the dataset verification result for each query 
					//resultOfDatasetMatchForEachQuery.put(queryId,isDataSetVerified);
				
				//If there are no items in resultOfDatasetMatchForEachQuery for given queryId
				//then the query id
				if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
					resultOfDatasetMatchForEachQuery.put(queryId,true);
				}
				
			}
		
			}//close for each SQL answer try resultset loop
		}
		HashMap<Integer, Boolean> finalMatchForQueries = new HashMap<Integer, Boolean>();
		boolean isMatchAll = false;
		boolean isQueryPass = false;
	
		
		for(int i : resultOfDatasetMatchForEachQuery.keySet()){
			//Get first query's result as default
			if(i == 1){
				isQueryPass = resultOfDatasetMatchForEachQuery.get(i);
			}//For second query, get the query result
			boolean isCorrect = resultOfDatasetMatchForEachQuery.get(i);
			//If instructor selects match All result sets while creating the question,
			//then all query results should hold true for the student query to pass
			if(isMatchAll){
				isQueryPass = isCorrect && isQueryPass;	
			}
			else{ 
				 //This excutes if instructor selects Match any result set option
				//If even one query result holds true, the student query is considered as pass
				 if(isCorrect){
					 isQueryPass = true;
					 break;
				 }
			}
		
		}
			
		if(isQueryPass){
			
		}else{
			//return failedDataSets;
		}
			
		}
		}
	return failedDataSetsPerQuery;
}

/**
 * This method checks if the query student query creates any views
 * 
 * @param query
 * @param user
 * @return
 * @throws Exception
 */
public String checkForViews(String query, String user) throws Exception{
		
		try(Connection conn = MyConnection.getDatabaseConnection()){
			HashMap<String, String> hm=new HashMap<String,String>();
			
				String out=query;
				String qry=query.replaceAll("\n"," ").replaceAll("\\s+", " ").toLowerCase();
				if(qry.startsWith("create view ")){
					isCreateView=true;
					
					String vname=query.substring(12).split("\\s")[0];
					String vquery=query.substring(12).split("\\s")[2];
					vquery=vquery.replaceAll("\n", " ").replaceAll("\\s+", " ").replaceAll("'", "''");
					String ins = "INSERT INTO xdata_views VALUES ("+vname+",'"+user+"',"+vquery+");";			
					try(PreparedStatement smt = conn.prepareStatement(ins)){
						smt.executeUpdate();
					}
					conn.close();
					return out;			
				}
				
				String q="Select * from xdata_views where rollnum = ?";
				
				try(PreparedStatement smt = conn.prepareStatement(q)){
					smt.setString(1, user);
					try(ResultSet rs=smt.executeQuery()){
						while(rs.next()){
							hm.put(rs.getString("vname"), rs.getString("viewquery"));
						}
					}
				}
				
				String newquery="";
		        /*Add the select part to new query */
		        StringTokenizer st=new StringTokenizer(query);                    
		        String token=st.nextToken();        
		        while(!token.equalsIgnoreCase("from")){        
		            newquery+=token+" ";
		            token=st.nextToken();
		        }
		
		        newquery+="from ";
		        /*Add the new from part*/
		        while(st.hasMoreTokens()){
		            token=st.nextToken();            
		            if(token.equalsIgnoreCase("where")||token.equalsIgnoreCase("group")){
		                newquery+=token+ " ";
		                break;
		            }
		            if(token.equals(",")){
		                newquery+=token+ " ";
		            }
		            if(token.contains(",")){
		                String tablenames[]=token.split(",");
		                for(int j=0;j<tablenames.length;j++){
		                    boolean isPresent=false;
		                    if(hm.containsKey(tablenames[j])){
		                        newquery+=hm.get(tablenames[j]) + " " + tablenames[j]+" ";
		                        isPresent=true;            
		                    }
		                    if(!isPresent){
		                        newquery+=tablenames[j]+" ";
		                    }
		                    newquery+=" ,";
		                }
		                newquery=newquery.substring(0,newquery.length()-1);
		                
		            }else{
		                boolean isPresent=false;
		                if(hm.containsKey(token)){
		                    newquery+=hm.get(token) + " " + token+" ";
		                    isPresent=true;            
		                }
		                if(!isPresent){
		                    newquery+=token+" ";
		                }
		            }
		            
		        }
		        /*Add the remaning part of query*/
		        while(st.hasMoreTokens()){
		            token=st.nextToken();
		            newquery+=token+ " ";
		        }
		        return out;
		}//try block for connection ends
		
	}

	// This method is only used for experiments and makes some assumptions which might not be true always
	public void test(String filePath, int assignmentId, int questionId, int queryId, String course_id) throws Exception{
		
		String qry = "select * from xdata_instructor_query where assignment_id = ? and question_id = ? and query_id = ? and course_id = ?";    	
		
		try(Connection conn = MyConnection.getDatabaseConnection()){
			try(Connection testConn = new DatabaseConnection().getTesterConnection(assignmentId)){
		
				try(PreparedStatement pstmt = conn.prepareStatement(qry)){
					pstmt.setInt(1, assignmentId);
					pstmt.setInt(2, questionId);
					pstmt.setInt(3, queryId);
					pstmt.setString(4,course_id);
					try(ResultSet rs = pstmt.executeQuery()){	
						// query output handling
						GenerateCVC1 cvc = new GenerateCVC1();
						cvc.initializeConnectionDetails(assignmentId, questionId, queryId,course_id);
						TableMap tm = cvc.getTableMap();
						cvc.closeConn();
						PopulateTestData p = new PopulateTestData();
						if(rs.next()){
							//delete the previous datasets		
							Runtime r = Runtime.getRuntime();
							File f=new File(Configuration.homeDir+"/temp_cvc"+filePath+"/");
							File f2[]=f.listFiles();
							if(f2 != null){
								for(int i=0;i<f2.length;i++){
									if(f2[i].isDirectory() && f2[i].getName().startsWith("DS")){
										Process proc = r.exec("rm -rf "+Configuration.homeDir+"/temp_cvc"+filePath+"/"+f2[i].getName());
										Utilities.closeProcessStreams(proc);
									}					
								}
							}
							String sqlQuery = rs.getString("sql");
							String getMutant = "select * from xdata_student_queries where assignment_id = ? and question_id = ? and course_id= ?";
							HashMap<String,String> mutants = new HashMap<String,String>();
							
					        try(PreparedStatement pstmt1 = conn.prepareStatement(getMutant)){
						        pstmt1.setInt(1, assignmentId);
						        pstmt1.setInt(2, questionId);
						        pstmt1.setString(3, course_id);
								try(ResultSet rs1 = pstmt1.executeQuery()){
									while(rs1.next()){
										String queryvariantid = rs1.getString("rollnum");
										String qryString = rs1.getString("querystring");
										mutants.put(queryvariantid, qryString);
									}
								}//try block for resultset rs1 ends
					        }//try block for pstmt1 ends
					        
							Map <Integer,Vector<String>>  datasetForQueryMap =  downloadDatasets(assignmentId,questionId,queryId,course_id,conn,filePath, false);
							datasets = datasetForQueryMap.get(queryId);
							Vector<String> cmismatch=new Vector<String>();
							for(int i=0;i<datasets.size();i++){
								String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
							 	File ds=new File(dsPath);
							 	String copyFiles[] = ds.list();
								
								Vector<String> vs = new Vector<String>();
								for(int m=0;m<copyFiles.length;m++){
								    vs.add(copyFiles[m]);
								}
								
								p.populateTestDataForTesting(vs, filePath+"/"+datasets.get(i), tm, testConn, assignmentId, questionId);
								
								Vector<String> killedMutants = mutantsKilledByDataset1(mutants, datasets.get(i), sqlQuery, dsPath, true,cmismatch, testConn);
				
								logger.log(Level.INFO,datasets.get(i)+" "+killedMutants.size());
								
								HashMap<String,String> mutantsdups=(HashMap<String, String>) mutants.clone();
								for(int l=0;l<killedMutants.size();l++){
									if(mutants.containsKey(killedMutants.get(l))){
										String qryUpdate = "update xdata_student_queries set verifiedcorrect = false where assignment_id = ? and question_id = ? and rollnum = ? and course_id=?";		
										try(PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate)){
											pstmt2.setInt(1, assignmentId);
											pstmt2.setInt(2, questionId);
											pstmt2.setString(3, killedMutants.get(l));
											pstmt2.setString(4,course_id);
											pstmt2.executeUpdate();
										}
										mutants.remove(killedMutants.get(l));
									}
								}
								for(int l=0;l<cmismatch.size();l++){
									String qryUpdate = "update xdata_student_queries set columnmismatch = true where assignment_id = ? and question_id = ? and rollnum = ? and course_id=?";
									try(PreparedStatement pstmt2 = conn.prepareStatement(qryUpdate)){
										pstmt2.setInt(1, assignmentId);
										pstmt2.setInt(2, questionId);
										pstmt2.setString(3, cmismatch.get(l));
										pstmt2.setString(4,course_id);
										pstmt2.executeUpdate();
									}
								}
								cmismatch.clear();
							 }
				
							Collection cl = mutants.keySet();
							Iterator itr = cl.iterator();
							int ctr=0;
							while(itr.hasNext()){
								String temp = (String) itr.next();
								String qryUpdate = "update xdata_student_queries set verifiedcorrect = true where assignment_id = ? and question_id = ? and rollnum = ? and course_id = ?";
								try(PreparedStatement pstmt3 = conn.prepareStatement(qryUpdate)){
									pstmt3.setInt(1, assignmentId);
									pstmt3.setInt(2, questionId);
									pstmt3.setString(3, temp);
									pstmt3.setString(4,course_id);
									pstmt3.executeUpdate();
								}
								ctr++;
							}
							logger.log(Level.INFO,""+ctr);
							//}
						}
						}//try block for resultset ends
				}//try block for statement ends
						}//try bloak for testcon ends
		}//try block for conn ends
	}
	
	//This method can be removed
	/*
	private Vector<String> mutantsKilledByQueryPlan(
			HashMap<String, String> mutants, String sqlQuery) {
		// TODO Auto-generated method stub
		Vector<String> queryIds = new Vector<String>();
		try {
			//Connection conn = MyConnection.getTestDatabaseConnection();
		//	Connection conn = MyConnection.getTestDatabaseConnection();
			
			String OriginalPlan="explain analyze "+sqlQuery;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(OriginalPlan);
			OriginalPlan="";
			while(rs.next()){
				OriginalPlan+=rs.getString(1)+"\n";
				int last=OriginalPlan.lastIndexOf("(actual time");
				if(last != -1)
					OriginalPlan=OriginalPlan.substring(0, last);
				last=OriginalPlan.lastIndexOf("Total runtime");
				if(last!= -1)
					OriginalPlan=OriginalPlan.substring(0, last);
			}

			String mutant_query="";
			Collection c = mutants.keySet();
			Iterator itr = c.iterator();
			while(itr.hasNext()){
				Object Id = itr.next();
				//logger.log(Level.INFO,"\nId="+Id);
				String mutant_qry = mutants.get(Id);
				
				//logger.log(Level.INFO,"Student: "+mutant_qry);
				mutant_qry=mutant_qry.trim().replace(';', ' ');				
				mutant_qry="explain analyze "+mutant_qry;
				Statement pstmt=conn.createStatement();
				try{
					ResultSet rset=pstmt.executeQuery(mutant_qry);
					String mutant_plan="";
					while(rset.next()){
						mutant_plan+=rset.getString(1)+"\n";
						int last=mutant_plan.lastIndexOf("(actual time");
						if(last != -1)
							mutant_plan=mutant_plan.substring(0, last);
						last=mutant_plan.lastIndexOf("Total runtime");
						if(last!= -1)
							mutant_plan=mutant_plan.substring(0, last);
					}
					if(!OriginalPlan.equalsIgnoreCase(mutant_plan)){
						queryIds.add((String)Id);
					}
				} catch (Exception e){
					queryIds.add((String)Id);
				}
			}
			
			//conn.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queryIds;
	}*/
	
	public void generateResults(String file, BufferedWriter bfrd) throws Exception {			//bfrd written to write no of incorrect queries. Delete if it gives errors
		
		//Connection conn = MyConnection.getExistingDatabaseConnection();
		try(Connection conn = MyConnection.getDatabaseConnection()){

		String filePath = "4/";			
		//String fileName="OldAssignment/Assign"+file;
		//String fileName="Assignment/Assign"+file;
		String fileName="NewAssignment/Assign"+file;
		//String strFile = Configuration.homeDir+"/temp_cvc"+filePath+"/CS_387_Quiz_0.csv";
		String strFile = Configuration.homeDir+"/temp_cvc"+filePath+"/"+fileName;	
		Scanner sc = new Scanner(new File(strFile));
		sc.useDelimiter("\\s*:\\s*");
		//sc.nextLine();
		sc.next();sc.next();sc.next();
		
		int rollno=1;
		String qID=sc.next();
		String quesDesc=sc.next();
		String quesID; 
		
		while(sc.hasNext()){
			int total=0,incorrect=0;
			quesID=qID.trim();
//			logger.log(Level.INFO,"quesID " + quesID);
			if(quesID.startsWith("'extra'")){
				quesID=qID.substring(8);					
			}
			
			logger.log(Level.INFO,"Question Number "+quesID);
			logger.log(Level.INFO,"Question Description is "+quesDesc);
			sc.next();
			sc.next();
			sc.next();
			datasets = null;//downloadDatasets(Integer.parseInt(quesID.substring(1, quesID.length()-1)),1,conn,filePath, false);
			
			do{
				String query=sc.next();
				query=query.replaceAll(";", " ").trim();				
				query=query.substring(1,query.length()-1).replaceAll("\n", " ").replaceAll("''","'").trim();
				logger.log(Level.INFO,rollno+") : "+query);
				rollno++;
				/*try{
					PreparedStatement smt;
					Connection conn = (new MyConnection()).getExhistingDatabaseConnection();
					smt=conn.prepareStatement(query);
					smt.executeQuery();	
				}catch(Exception e){					
					e.printStackTrace();
				}*/
				try{
					total++;
//					logger.log(Level.INFO,"total " + total);
					String res= null;//testAnswer(quesID.substring(1, quesID.length()-1),query,rollno+"",filePath);
					logger.log(Level.INFO,"Result "+res);
					if(res.contains("Failed")){
						incorrect++;
						logger.log(Level.INFO,"incorrect " + incorrect);
					}
					logger.log(Level.INFO,"");
				}catch(Exception e){
					logger.log(Level.SEVERE,"Result Failed",e);
					incorrect++;
					logger.log(Level.SEVERE,"incorrect " + incorrect,e);
					//e.printStackTrace();
				}
				
				if(sc.hasNext()){
					qID=sc.next();
					quesDesc=sc.next();
				}else{
					break;	//end of file
				}
			}while(qID.length()==0);	
			bfrd.write(quesID + " Total Queries :" + total + " ");
			int correct = total - incorrect;
			bfrd.write(quesID + " Correct Queries :" + correct + " ");
			bfrd.write(quesID + " Incorrect Queries :" + incorrect + "\n");
		}
		}//try block for connection ends
		
		//conn.close();
	}

	public static void main(String args[])throws Exception{
		
		int assignmentId = 8;
		int questionId = 14;
		String filePath = "4/A" + assignmentId + "Q" + questionId;
		long startTime = System.currentTimeMillis();
		(new TestAnswer()).test(filePath, assignmentId, questionId, 1,"CS631");
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		logger.log(Level.INFO,""+totalTime);

	}
	
			
	
	/** 
	 * This method test the student query against DS0 when student submits edited query in 
	 * non-interactive mode
	 * 
	 * @param assignmentId
	 * @param questionId
	 * @param query
	 * @param user
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public QueryStatus testQueryAnswerForTestThreads(int assignmentId, int questionId,String course_id, String query, String user, String filePath) throws Exception{
		

		int queryId = 1;
		String qId = "A"+assignmentId+"Q"+questionId+"S"+queryId;
		Map <Integer,Boolean> resultOfDatasetMatchForEachQuery  = new HashMap<Integer,Boolean>();
		ArrayList <Boolean> isDataSetVerified  = null;
		boolean isMatchAllQueryIncorrect = false;
		String matchAllFailedForDS = "";
		QueryStatus status = QueryStatus.Error;
		
		boolean isQueryPass = false;
		PopulateTestData p = new PopulateTestData();
		boolean orderIndependent = false;
		boolean isMatchAll = false;
		TestAssignment tt = new TestAssignment();
		try(Connection conn = TestAssignment.getDBConnectionToTest()){
			try(Connection testConn = TestAssignment.getTesterConnectionToTest()){
		
				String getMatchAllOption = "select matchallqueries,orderIndependent from xdata_qinfo where assignment_id = ? and question_id = ? and course_id= ?";
				try(PreparedStatement pst = conn.prepareStatement(getMatchAllOption)){
					pst.setInt(1,assignmentId);
					pst.setInt(2,questionId);
					pst.setString(3,course_id);
					try(ResultSet rset = pst.executeQuery()){
						rset.next();
						isMatchAll = rset.getBoolean("matchallqueries");
						orderIndependent = rset.getBoolean("orderIndependent");
					}
				}
				String qry = "select * from xdata_instructor_query where assignment_id = ? and question_id = ? and course_id=?";
				try(PreparedStatement pstmt = conn.prepareStatement(qry)){
				pstmt.setInt(1,assignmentId);
				pstmt.setInt(2,questionId);
				pstmt.setString(3,course_id);
				
				try(ResultSet rs = pstmt.executeQuery()){
				
						while(rs.next()){
								//If Instructor has chosen match All queries option, get all datasets
								// match student query against all and return true only if all ds matches
								String sqlQuery=rs.getString("sql");
								queryId = rs.getInt("query_id");
								qId = "A"+assignmentId +"Q"+questionId+"S"+queryId;
								query=checkForViews(query,user);
								HashMap<String,String> mutants = new HashMap<String,String>();
								mutants.put(qId, query);
								boolean incorrect=false;
								isDataSetVerified = new ArrayList<Boolean>();
								Map <Integer,Vector<String>>  datasetForQueryMap =  downloadDatasets(assignmentId,questionId, queryId,course_id, conn, filePath, true);
								if(datasetForQueryMap.isEmpty()){
										//conn.close();
										//testConn.close();
										//return QueryStatus.NoDataset;
									 //Load the default sample data file
									boolean flag=true;
									Vector<String> cmismatch = new Vector<String>();
									
									try{
										 p.deleteAllTempTablesFromTestUser(testConn);
										}catch(Exception e){
											logger.log(Level.INFO,"Temporary Table does not exist",e);
											
										}
										try{
										 p.createTempTableData(conn,testConn,assignmentId,questionId,course_id);
										}catch(Exception e){
											logger.log(Level.SEVERE,"Temp Table creation error. Table already exists",e);
											throw e;
										}
										Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, "DS_Default", sqlQuery,"NoPath", orderIndependent, cmismatch, testConn);
										logger.log(Level.INFO,"******************");
										logger.log(Level.INFO,"Default dataset Loaded : " + " " + killedMutants.size());
										logger.log(Level.INFO,"******************");
										for(int l=0;l<killedMutants.size();l++){
											if(mutants.containsKey(killedMutants.get(l))){
												flag = false;
												incorrect = true;
											}
										}
										if(!incorrect){
											//isDataSetVerified.add(true);
										}	
										else{
											
											resultOfDatasetMatchForEachQuery.put(queryId,false);
											logger.log(Level.INFO,"");
										}	
										
										if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
											resultOfDatasetMatchForEachQuery.put(queryId,true);
										}
									}else{
										for(Integer id : datasetForQueryMap.keySet()){
											 Vector<String> datasets = datasetForQueryMap.get(id);
											 if(datasets.size() == 0){
												conn.close();
												testConn.close();
												return QueryStatus.NoDataset;
												 }
											 boolean flag=true;
											 for(int i=0;i<datasets.size();i++){
													
													String dsPath = Configuration.homeDir+"/temp_cvc"+filePath+"/"+datasets.get(i);
												 	File ds=new File(dsPath);
												 	//GenerateCVC1 c = new GenerateCVC1();
													String copyFiles[] = ds.list();
													
													Vector<String> vs = new Vector<String>();
													for(int m=0;m<copyFiles.length;m++){
													    vs.add(copyFiles[m]);		    
													}
													
												 	// query output handling
													GenerateCVC1 cvc = new GenerateCVC1();
													Pattern pattern = Pattern.compile("^A([0-9]+)Q[0-9]+");
													Matcher matcher = pattern.matcher(qId);
													int assignId = 1;
													
													if (matcher.find()) {
													    assignId = Integer.parseInt(matcher.group(1));
													} 
													
													cvc.initializeConnectionDetails(assignId,questionId,queryId,course_id);
													TableMap tm = cvc.getTableMap();
													p.populateTestDataForTesting(vs, filePath+"/"+datasets.get(i), tm, testConn, assignId, questionId);
													
													
													Vector<String> cmismatch = new Vector<String>();
													logger.log(Level.INFO,datasets.get(i));
													
													
													Vector<String> killedMutants = checkAgainstOriginalQuery(mutants, datasets.get(i), sqlQuery, dsPath, orderIndependent, cmismatch, testConn);
													logger.log(Level.INFO,"******************");
													logger.log(Level.INFO,datasets.get(i) + " " + killedMutants.size());
													logger.log(Level.INFO,"******************");
													for(int l=0;l<killedMutants.size();l++){
														if(mutants.containsKey(killedMutants.get(l))){
															flag = false;
															incorrect = true;
														}
													}
															if(!incorrect){
																//isDataSetVerified.add(true);
															}	
															else{
																
																resultOfDatasetMatchForEachQuery.put(queryId,false);
																logger.log(Level.INFO,"");
																}
															cvc.closeConn();									
												   }
											
										if(!(resultOfDatasetMatchForEachQuery.containsKey(queryId))){
											resultOfDatasetMatchForEachQuery.put(queryId,true);
										}
									 }//For each query
								}//else If dataset exists part ends 
						}//While next ResultSet
					}//close resultset try block
				}//close stmnt try blk
				/*******Check for Match all or match One Option Start******/	
				for(int i : resultOfDatasetMatchForEachQuery.keySet()){
					//Get first query's result as default
					if(i == 1){
						isQueryPass = resultOfDatasetMatchForEachQuery.get(i);
					}//For second query, get the query result
					boolean isCorrect = resultOfDatasetMatchForEachQuery.get(i);
					//If instructor selects match All result sets while creating the question,
					//then all query results should hold true for the student query to pass
					if(isMatchAll){
						isQueryPass = isCorrect && isQueryPass;	
					}
					else{
						 //This executes if instructor selects Match any result set option
						//If even one query result holds true, the student query is considered as pass
						 if(isCorrect){
							 isQueryPass = true;
							 break;
						}
					}
				}
				if(isQueryPass){
					status = QueryStatus.Correct;
				}
				else{
					status = QueryStatus.Incorrect;
				}	
				testConn.close();
			}//close connection
		conn.close();
		}//close connection
		return status;
	}
	
	
	
}

