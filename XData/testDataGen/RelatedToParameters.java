package testDataGen;

import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateConstraintsForHavingClause;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import stringSolver.AppTest_StringConstraintSolver;
import stringSolver.StringConstraint;
import stringSolver.StringConstraintSolver;

/**
 * TODO: GOOD DOC
 * @author mahesh
 *
 */
/**FIXME: Whether these parametrs are for query block/ or entire query*/
public class RelatedToParameters {

	private static Logger logger = Logger.getLogger(RelatedToParameters.class.getName());
	public static boolean isParameterized(Node n){
		if(n.getLeft()==null || n.getLeft().getType()==null)
			return false;
		if(n.getLeft().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getLeft().getStrConst().contains("$")){
				return true;
			}
			return false;
		}
		else if(n.getRight()==null)
			return false;
		else if(n.getRight().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getRight().getStrConst().contains("$")){
				return true;
			}
			return false;			
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType())){
			return isParameterized(n.getLeft()) || isParameterized(n.getRight());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			return isParameterized(n.getLeft()) || isParameterized(n.getRight());
		}
		else{
			return false;
		}
	}

	public static ArrayList<String> replaceParamWithID(QueryBlockDetails queryBlock, Node n, String paramContent, String paramType){
		if(n.getLeft().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getLeft().getStrConst().contains(paramContent)){
				String param = "PARAM_"+paramType+"_"+ queryBlock.getParamCount();
				queryBlock.getParamMap().put(n.getLeft().getStrConst(), param);
				queryBlock.setParamCount(queryBlock.getParamCount() + 1) ;
				ArrayList<String> p= new ArrayList<String>();
				p.add(param);
				return p;
			}
		}
		else if(n.getRight().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getRight().getStrConst().contains(paramContent)){
				String param = "PARAM_"+paramType+"_"+queryBlock.getParamCount();
				queryBlock.getParamMap().put(n.getRight().getStrConst(), param);
				queryBlock.setParamCount(queryBlock.getParamCount() + 1) ;
				ArrayList<String> p= new ArrayList<String>();
				p.add(param);
				return p;
			}
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			ArrayList<String> p1 = replaceParamWithID( queryBlock, n.getLeft(), paramContent, paramType);
			ArrayList<String> p2 = replaceParamWithID( queryBlock, n.getRight(), paramContent, paramType);
			p1.addAll(p2);
			return p1;
		}
		return null;
	}

	public static HashMap<String, Node> removeParameterizedConstraints( QueryBlockDetails queryBlock, ArrayList<Node> pConstraints, String paramType){
		HashMap<String, Node> retVal = new HashMap<String, Node>();
		queryBlock.setParamCount(0);
		for(int i=0;i<pConstraints.size();i++){
			Node n = pConstraints.get(i);
			if(isParameterized(n)){
				ArrayList<String> paramsInNode = replaceParamWithID( queryBlock, n, "$", paramType);
				queryBlock.getParamsNodeMap().put(paramsInNode, n);
				retVal.put(paramType+queryBlock.getpConstraintId(),n);
				queryBlock.setpConstraintId(queryBlock.getpConstraintId() + 1);
				//Don't remove these constraints. Let them be there. Just replace the $ value with the parameter name that we have supplied
				//pConstraints.removeElementAt(i);
			}
		}
		return retVal;
	}

	/**TODO: Add doc for this */
	public static String addDatatypeForParameters( GenerateCVC1 cvc, QueryBlockDetails queryBlock  ){
		String retVal = "";
		Iterator<ArrayList<String>> itr = queryBlock.getParamsNodeMap().keySet().iterator();
		int val = 0;
		boolean isMaxOrMin = false;
		//Application Testing		
		ArrayList<String> xdata_param_constraints = cvc.getDBAppparams().getDbridge_Param_Constraints();			
		HashMap<String,String> xdata_param_selMap = cvc.getDBAppparams().getDbridge_param_sel_map();
		HashMap<String,String> params_Datatype = (HashMap<String, String>) cvc.getDBAppparams().getParameters_Datatype().clone(); 
		//end
		while(itr.hasNext()){
			ArrayList<String> params = itr.next();
			Node n = queryBlock.getParamsNodeMap().get(params);
			//String datatype = getDatatypeFromNode(n);
			//modified by bikash to ensure that datatype is the same as column name to ensure that range constraints get satisfied
			//FIXME: What if left side is aggregation
			String datatype = null;

			if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()))//If the left side of node is aggregate function
			{
				if(n.getLeft().getAgg().getAggExp().getType().equalsIgnoreCase(Node.getBaoNodeType()))
					datatype = getTableNameNoForBAONode(n.getLeft().getAgg().getAggExp());
				else
					datatype = n.getLeft().getAgg().getAggExp().getColumn().getColumnName();
				 
			}
			else if (n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType()))//If the right side of node is aggregate function
			{
				if(n.getRight().getAgg().getAggExp().getType().equalsIgnoreCase(Node.getBaoNodeType()))
					datatype = getTableNameNoForBAONode(n.getRight().getAgg().getAggExp());
				else
					datatype = n.getRight().getAgg().getAggExp().getColumn().getColumnName();
					
			}
			else if(n.getLeft() != null && n.getLeft().getColumn()!=null)//if left side is not aggregate
			{
				datatype = n.getLeft().getColumn().getColumnName();
			}
			else if(n.getRight() != null && n.getRight().getColumn() != null)
			{
				datatype = n.getRight().getColumn().getColumnName();
			}
			//System.out.println("datatype: "+datatype);*/
			//String datatype = getDatatypeFromNode(n);
			if(n.getType().equalsIgnoreCase(Node.getBroNodeType())){
				if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()) ||
						n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
					if(n.getAggFuncFromNode().getFunc().equalsIgnoreCase("MAX")){
						isMaxOrMin = true;
						Vector<Column> cols = n.getAggFuncFromNode().getAggExp().getColumnsFromNode();
						val = 0;
						for(int i=0;i<cols.size();i++){
							if(val < cols.get(i).getMaxVal())	val = (int)cols.get(i).getMaxVal();
						}
					}
					if(n.getAggFuncFromNode().getFunc().equalsIgnoreCase("MIN")){
						isMaxOrMin = true;
						Vector<Column> cols = n.getAggFuncFromNode().getAggExp().getColumnsFromNode();
						val = 1000000;
						for(int i=0;i<cols.size();i++){
							if(val > cols.get(i).getMinVal())	val = (int)cols.get(i).getMinVal();
						}
					}
				}
			}
			//Application Testing --Add the data type for all the params to CVC
			
			for(int i=0;i<params.size();i++){
				
				retVal = params.get(i) + " : " + datatype +";\n" + retVal;
				//to collect all string constraints
				Vector<String> Stringvec = new Vector();
				String newconsformat ="";
				boolean notflag = false;
				if(xdata_param_selMap.containsKey(params.get(i).toString())){
					String keyValue = xdata_param_selMap.get(params.get(i));
					if(! retVal.contains(keyValue + " : ")){
						retVal = keyValue + " : " + datatype +";\n" + retVal;
						retVal+= "ASSERT NOT ISNULL_"+datatype+"("+keyValue+");"+"\n";
						
					}
					if(! retVal.contains("ASSERT ("+ keyValue +" = "+params.get(i)))
						retVal+= "ASSERT ("+ keyValue +" = "+params.get(i)+" );"+"\n";
											
						for(String paramsstr : xdata_param_constraints){
							if(paramsstr.contains(keyValue)){								
								String tempstr = paramsstr;
								tempstr =tempstr.replace(keyValue, params.get(i));
								 notflag=false;
								if(tempstr.contains("NOT")){
									notflag=true;
									//tempstr=tempstr.replace("NOT ", "");
								}
									
								if(cvc.getDBAppparams().getParameters_Datatype_Copy().get(keyValue).toLowerCase().contains("string")){
									//need to call string constraint solver
									 newconsformat = "(O_";
									String tableName ="";
									while(tempstr.length()>0){
										if(tempstr.charAt(0)==' ')
											tempstr = tempstr.substring(1);
										else
											break;
									}
									String[] tempstr_array = tempstr.split(" ");
									
									tempstr="";
									if(!notflag){
										tempstr+=tempstr_array[0]+" "+tempstr_array[1]+" "+ "'";
										for(int arsize=2;arsize<tempstr_array.length;arsize++){
											tempstr+=tempstr_array[arsize];
											if(arsize<tempstr_array.length-1)
												tempstr+=" ";
										}
									}
										
									else{
										tempstr+=tempstr_array[1]+" ";//+tempstr_array[2]+" "+"'";
										if(tempstr_array[2].equals("!=")){
											tempstr+="= '";
											notflag=false;
										}
										else if(tempstr_array[2].equals("==")){
											tempstr+="/= '";
											notflag=false;
										}
										else
											tempstr+=tempstr_array[2]+" "+"'";
											
										for(int arsize=3;arsize<tempstr_array.length;arsize++){
											tempstr+=tempstr_array[arsize];
											if(arsize<tempstr_array.length-1)
												tempstr+=" ";
										}
									}
										
									
									tempstr+="'";
									for(Entry<ArrayList<String>, Node> strtmp : queryBlock.getParamsNodeMap().entrySet()){
										if(strtmp.getKey().toString().contains(params.get(i))){
											tableName=strtmp.getValue().toString();
											break;
										}
									}
									//String tableName = queryBlock.getParamsNodeMap()..get(params.get(i)).toString();
									tableName = tableName.substring(1,tableName.indexOf(datatype)-2);
									for(Table tbstr : cvc.getResultsetTables()){
										if(tbstr.toString().equalsIgnoreCase(tableName)){
											int clmnindex =0;
											for(String clmn :tbstr.getColumns().keySet()){
												if(clmn.equalsIgnoreCase(datatype)){
													newconsformat+=tableName+"[1]."+clmnindex;
													break;
												}
												clmnindex++;
											}
										}
									}
									tempstr =tempstr.replace(params.get(i), newconsformat);
									tempstr =tempstr.replace("!","/");								
									tempstr+=")";								
									
									Stringvec.add(tempstr);
//									
								}
								
								else{
									paramsstr=paramsstr.replace("!","/");
									if(paramsstr.contains("NOT ")){
										if(! retVal.contains( "ASSERT NOT ("+paramsstr.replace("NOT ", "")+");"))
											retVal+= "ASSERT NOT ("+paramsstr.replace("NOT ", "")+");"+"\n";
									
										if(! retVal.contains( "ASSERT NOT ("+tempstr.replace("NOT ", "")+");"))
											retVal+= "ASSERT NOT ("+tempstr.replace("NOT ", "")+");"+"\n";
									}
									else{
										if(! retVal.contains("ASSERT ("+paramsstr+");"))
											retVal+= "ASSERT ("+paramsstr+");"+"\n";
										
										if(! retVal.contains( "ASSERT ("+tempstr.replace("NOT ", "")+");"))
											retVal+= "ASSERT ("+tempstr.replace("NOT ", "")+");"+"\n";
									}
								}
								
								
							}
						}
						if(Stringvec.size()>0){
							AppTest_StringConstraintSolver obstrsolver = new AppTest_StringConstraintSolver();
							try {
								
								Vector<String> res_string = obstrsolver.solveConstraints_Apptest(Stringvec,cvc.getResultsetColumns(),cvc.getTableMap());
								String new_res_string="";
								if(res_string.size()>0){
									for(int rssize=0;rssize<res_string.size();rssize++){
										new_res_string=res_string.get(rssize);
										new_res_string=new_res_string.replace("ASSERT ","");
										new_res_string=new_res_string.replace(newconsformat, params.get(i));
										new_res_string=new_res_string.replace(")", "");
										new_res_string=new_res_string.replace(";", "");
										new_res_string=new_res_string.replace("\n", "");
										if(!notflag){
											retVal+= "ASSERT ("+new_res_string+");"+"\n";
											new_res_string=new_res_string.replace(params.get(i), keyValue);
											retVal+= "ASSERT ("+new_res_string+");"+"\n";
										}
										else{
											retVal+= "ASSERT NOT ("+new_res_string+");"+"\n";
											new_res_string=new_res_string.replace(params.get(i), keyValue);
											retVal+= "ASSERT NOT ("+new_res_string+");"+"\n";
										}
									}
									
								}
								
									
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if(params_Datatype.containsKey(keyValue))
							params_Datatype.remove(keyValue);
					
				}
//				
				//end
				if(isMaxOrMin)
					//retVal += "ASSERT "+params.get(i)+" = "+val+";\n";
					retVal += "ASSERT "+n+" = "+val+";\n";
			}
			isMaxOrMin = false;
		}
		//Application Testing
				if(params_Datatype != null && params_Datatype.size()>0){
					//add the datatype of the parameters first
					 for (Entry<String, String> entry : params_Datatype.entrySet()){
						 if(! retVal.contains(entry.getKey() + " : " + entry.getValue())){
							 if(! entry.getValue().toLowerCase().contains("string")){
								 String data_type = entry.getValue().toUpperCase();
								 if(data_type.equals("FLOAT") || data_type.equals("DOUBLE"))
									 data_type = "REAL";
								 retVal = entry.getKey() + " : " + data_type+";\n" + retVal;
							 }
						 }
					 }
					//add program parameters
					 for (Entry<String, String> entry : params_Datatype.entrySet()){
						 if(! retVal.contains(entry.getKey() + " : " + entry.getValue())){
							 if(! entry.getValue().toLowerCase().contains("string")){
								 String data_type = entry.getValue().toUpperCase();
								 if(data_type.equals("FLOAT") || data_type.equals("DOUBLE"))
									 data_type = "REAL";
								// retVal += entry.getKey() + " : " + data_type+";\n";
								 for(String paramsstr : xdata_param_constraints){
										if(paramsstr.contains(entry.getKey())){
											if(paramsstr.contains("NOT ")){
												if(paramsstr.contains("!=")){
													paramsstr=paramsstr.replace("NOT ", "");
													paramsstr=paramsstr.replace("!=", "=");
												}
											}
											paramsstr=paramsstr.replace("!","/");
											if(paramsstr.contains("NOT ")){										
												if(! retVal.contains( "ASSERT NOT ("+paramsstr.replace("NOT ", "")+");"))
													retVal+= "ASSERT NOT ("+paramsstr.replace("NOT ", "")+");"+"\n";
											}
											else{
												if(! retVal.contains("ASSERT ("+paramsstr+");"))
													retVal+= "ASSERT ("+paramsstr+");"+"\n";
											}
											
										}
								 }
								 
								}
							 else{
								 for(String paramsstr : xdata_param_constraints){
									 
									 boolean flagfornot =false;
									 if(paramsstr.contains(entry.getKey())){
										 if(paramsstr.contains("NOT "))
											 flagfornot = true;
										 if(flagfornot && paramsstr.contains("!=")){
											 paramsstr=paramsstr.replace("NOT ", "");
											 paramsstr=paramsstr.replace("!=", "=");
											 flagfornot=false;
										 }
										 //need to code
										 String[] tempstr_array = paramsstr.split(" ");
										 String paramVal ="";
										 String op ="";
										 String operand ="";
										 if(!flagfornot && tempstr_array.length>2){
											 paramVal+=tempstr_array[2]+" ";
											 op = tempstr_array[1];
											 operand = tempstr_array[0];
										 }
										 else{
											 op = tempstr_array[2];
											 operand = tempstr_array[1];
										 }
										// op =op.replace("!","/");
										 for(int l=3;l<tempstr_array.length;l++){
											 paramVal+=tempstr_array[l];
											 if(l<tempstr_array.length-1)
												 paramVal+=" ";
										 }
										 if(! op.equals("=")){
											 StringConstraintSolver obstrsolver = new StringConstraintSolver();
											 StringConstraint s=new StringConstraint(operand +" "+op+" '"+paramVal+"'"); 	
											 //Need to first check for related parameters then call for solving single constraints
											 StringConstraintSolver scsobj = new StringConstraintSolver();
											String  paramValnew = scsobj.solveSingleConstraint(s);
											//System.out.println("paramValnew---"+paramValnew);
											paramVal = paramValnew;
										 }
										
										 paramVal = paramVal.replace("%","_p");
										 paramVal = paramVal.replace("+","_s");								 
										 paramVal = paramVal.replace("-","_m");
										 paramVal = paramVal.replace("*","_s");
										 paramVal = paramVal.replace( "_","_u");
										 paramVal = paramVal.replace(".","_d");
										 paramVal = paramVal.replace(" ","_ub");
										 //call the string solver
										 
										 //end
										 retVal+= "DATATYPE \n";
										 if(! retVal.contains("STR"+entry.getKey()+" = "+paramVal+ " END ")){
											 retVal+= "STR"+entry.getKey()+" = "+paramVal+ " END ; \n";
											 retVal+= entry.getKey() +" : "+ "STR"+entry.getKey()+"; \n";
											 retVal+= "ASSERT ("+entry.getKey()+" = "+paramVal+");"+"\n";
										 }
									 }
									 else{
										 if(! retVal.contains("STR"+entry.getKey()+" = ")){
											 retVal+= "DATATYPE \n";
											 retVal+= "STR"+entry.getKey()+" = xyz END ; \n";
											 retVal+= entry.getKey() +" : "+ "STR"+entry.getKey()+"; \n";
											 retVal+= "ASSERT ("+entry.getKey()+" = "+"xyz"+");"+"\n";
										 }
										 
										 
									 }
								 }
							 }
						 }
					 }
				}
				//end
		return retVal;
	}

	/**
	 * Adds constraints to hash map "constraintsWithParameters" for parameterized aggregation or selection conditions
	 * @param queryBlock
	 */
	public static void setupDataStructuresForParamConstraints(QueryBlockDetails queryBlock) {


		if( queryBlock.getHavingClause() != null){
			queryBlock.getConstraintsWithParameters().putAll(removeParameterizedConstraints( queryBlock, queryBlock.getAggConstraints(), "AGG"));
			//These constraints have to be put into CVC with new variables for the (unsatisfiable) constants/parameters.
		}

		for(ConjunctQueryStructure con: queryBlock.getConjunctsQs()){
			
			if( con.getSelectionConds() != null && con.getSelectionConds().size() != 0)			//Selection conds are already flattened
				queryBlock.getConstraintsWithParameters().putAll(removeParameterizedConstraints(queryBlock, new ArrayList( con.getSelectionConds() ), "SEL"));


			if( con.getLikeConds() != null && con.getLikeConds().size()!=0)		//Like conds are already flattened
				queryBlock.getConstraintsWithParameters().putAll(removeParameterizedConstraints( queryBlock, new ArrayList(con.getLikeConds()), "LIKE"));
		}		
	}


	public static String getConstraintsForParameters(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{

		if( queryBlock.getConstraintsWithParameters() == null || queryBlock.getConstraintsWithParameters().size()==0){
			return "";
		}

		String retVal = "";
		Iterator itr = queryBlock.getConstraintsWithParameters().keySet().iterator();	

		while(itr.hasNext()){

			String key = (String) itr.next();
			Node n = queryBlock.getConstraintsWithParameters().get(key);

			if(key.contains("AGG"))

				retVal += GenerateConstraintsForHavingClause.getCVCForHavingConstraintRepeated( cvc, queryBlock, n, queryBlock.getFinalCount(), key, 0)+";\n";

			else if(key.contains("SEL"))

				for(int l=0; l < queryBlock.getFinalCount(); l++){
					retVal += "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred( queryBlock, n, l, key)+";\n";

				}
		}
		return retVal;
	}

	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return datatype of column in BAO node
	 */
	public static String getTableNameNoForBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getColumn() != null){
			return n1.getRight().getColumn().getColumnName();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getColumn() != null){
			return n1.getLeft().getColumn().getColumnName();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableNameNoForBAONode(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableNameNoForBAONode(n1.getRight());
			}
		}
		return null;
		
	}
	
}
