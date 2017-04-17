package stringSolver;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.*;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import parsing.Column;
import parsing.Table;
import util.TableMap;
import util.Utilities;

public class AppTest_StringConstraintSolver implements Serializable {
	
	private static Logger logger = Logger.getLogger(StringConstraintSolver.class.getName());
	private static final long serialVersionUID = 1L;

	public Vector<String> solveConstraints_Apptest(Vector<String> assertConstraints,Vector<Column> columns,TableMap tableMap) throws Exception {
		//System.out.println("assertConstraints " + assertConstraints);
		Vector<StringConstraint> resultingConstraints = new Vector<StringConstraint>();
		Vector<StringConstraint> stringConstraints = new Vector<StringConstraint>();
		StringConstraintSolver ssobj = new StringConstraintSolver();
		//Vector<String> assertOrNot=new Vector<String>();
		int l = assertConstraints.size();
		if(l==0)
			return(new Vector<String>());
		for(String str:assertConstraints){
			if(str == "")
				continue;
			String Ors[]=str.split(" OR ");
			for(String EachOr : Ors){
				String AndConstraint[] = EachOr.trim().split(" AND ");
				for(String And: AndConstraint){
					StringConstraint s=new StringConstraint(And.substring(str.indexOf("(")+1, str.lastIndexOf(")"))); //removing brackets
					stringConstraints.add(s);
				}
			}
		}
		
		
		//TODO:correct this
		//Collections.sort(stringConstraints, new StringConstraint());
		//-----------------------simplify constraints
		//Collections.sort(constraints, new StringConstraint());
		Vector<StringConstraint> equal=new Vector<StringConstraint>();
		boolean flag=StringConstraintSolver.removeRedundantConds(stringConstraints,equal);
		if(flag==false){
				return null;
		}		
		for(StringConstraint s:equal)	//constraints of the form of var=const can be assigned directly
			resultingConstraints.add(s);
		

		
		HashMap<String,String> equalVar=new HashMap<String,String>();
		flag=StringConstraintSolver.removeEqualConds(stringConstraints, equalVar);
		if(flag==false){
				return null;
		}
		
		if(stringConstraints.size()==0){
			
		}


		HashSet<StringConstraint> relatedConstraints=null;		//hashset would prevent duplicates
		while(stringConstraints.size()>0){  //group constraints on related variables together
			relatedConstraints=new HashSet<StringConstraint>();
			StringConstraint s=stringConstraints.get(0);
			relatedConstraints.add(s);
			stringConstraints.remove(0);

//			System.out.println("s = " + s);
			ssobj.getReleatedConstraints(s.var1,relatedConstraints,stringConstraints);
//			System.out.println("relatedConstraints = " + relatedConstraints);
			if(s.var2!=null)
				ssobj.getReleatedConstraints(s.var2,relatedConstraints,stringConstraints);
//			System.out.println("relatedConstraints = " + relatedConstraints);

			stringConstraints.removeAll(relatedConstraints);
			Vector<StringConstraint> rel=new Vector<StringConstraint>();
			rel.addAll(relatedConstraints);
			Vector<StringConstraint> result= solveRelatedConstraints_AppTest(rel,columns,tableMap);	
//			System.out.println("result.size() = " + result.get(0));	
			if(result== null || result.size()==0 ){
				Vector<String> v=new Vector<String>();
				v.add("ASSERT  (1=2);\n");
				return v;
			}
			else{
				resultingConstraints.addAll(result);
			}
				
			
		}
		
		Vector<StringConstraint> tempConstraints=new Vector<StringConstraint>();
		Iterator<String> iterEqualVal=equalVar.keySet().iterator();
		while(iterEqualVal.hasNext()){
			flag=false;
			String str=iterEqualVal.next();			
			for(StringConstraint c:resultingConstraints){
				if(c.var1.equals(equalVar.get(str))){
					StringConstraint s =new StringConstraint();
					s.var1=str;
					s.operator="=";
					s.constant=c.constant;
					tempConstraints.add(s);
					flag=true;
				}
			}
			if(flag==false){
				StringConstraint s =new StringConstraint();
				s.var1=str;
				s.operator="=";
				s.constant="A";
				tempConstraints.add(s);
				s.var1=equalVar.get(str);
				s.operator="=";
				s.constant="A";
				tempConstraints.add(s);
			}
			resultingConstraints.addAll(tempConstraints);
		}
		
		if(columns==null){
			Vector<String> v=new Vector<String>();
			v.add(resultingConstraints.toString());
			return v;
		}
		
		Vector<String> vStr=new Vector<String>();
		//int i=0;
		for(StringConstraint c:resultingConstraints){
			//Application Testing--comment this function call and call the new func
			//String str=addValueToColumn(c.var1,c.constant,columns,tableMap);			
			String str=addValueToColumn_AppTest(c.var1,c.operator,c.constant,columns,tableMap);
			//c.constant=i+"";
			//vStr.add("ASSERT  ("+c.var1+"="+i+");\n");
			vStr.add("ASSERT  ("+c.var1+"="+str+");\n");
			//vStr.add(c.toString());
			
		}
		return vStr;
	}
	
	private Vector<StringConstraint> solveRelatedConstraints_AppTest(Vector<StringConstraint> constraints,Vector<Column> columns,TableMap tableMap)	{
		Vector<StringConstraint> result=new Vector<StringConstraint>();
		
		
		boolean flag=true;
		int length=constraints.size();
		//System.out.println("length = " + length);
		
		//----------------------Solve for Single variable---------------------
		for(int i=0;i<length;i++){		
			StringConstraint s=constraints.get(i);
			//System.out.println("s ++ " + s.var1 + " " + s.var2 + " " + s.constant + " " +s.operator);
			if(s.var2!=null || s.operator.startsWith("L")){ 
				flag=false;
				break;
			}
		}
		//System.out.println("flag = " + flag);
		if(flag){
			
			String str,opr;
			if(length==1)		//only a single constraint
				
			{
				StringConstraint sc = constraints.get(0);
				str = sc.constant.toString();
				opr=sc.operator;
			}  
			
			else{				//multiple constraints		
				//Application Testing
				
				//str=solveUsingAutomaton(constraints);
				str=solveUsingAutomaton_AppTest(constraints,columns,tableMap);
				opr="=";
				
				
			}
			if(str==null) return null;
			//Application Testing
			else result.add(new StringConstraint(constraints.get(0).var1+" "+opr+" '"+ str+"'"));	
			//else result.add(new StringConstraint(str));	--neha need to uncomment this
			//end
			return result;
		}
													//*/
		BiMap<String,Integer> varMap;
		varMap =HashBiMap.create();
		int j=0;
		for(int i=0;i<length;i++){		
			StringConstraint s=constraints.get(i);
			//System.out.println("s1 = " + s);
			Integer lhs=varMap.get(s.var1);
			if(lhs==null){
				varMap.put(s.var1,j);
				lhs=j;
				j++;			
			}
			s.var1=lhs+"";
			
			if(s.var2==null) continue;
			Integer rhs=varMap.get(s.var2);
			if(rhs==null){
				varMap.put(s.var2,j);
				rhs=j;
				j++;			
			}
			s.var2=rhs+"";		
		}
		
		
		//HashMap<Integer,String> val=solveMultipleUsingGraph(constraints,varMap.size());
		StringConstraintSolver ssobj = new StringConstraintSolver();
		HashMap<Integer,String> val= ssobj.solveGroupedConstraintsWithInequalities(constraints, new Vector<StringConstraint>(),varMap.size());
		if(val==null)  return null;
		BiMap<Integer,String> inverseMap=varMap.inverse();
		for(Integer temp:val.keySet()){
			StringConstraint s=new StringConstraint();
			s.operator = "=";
			s.var1=inverseMap.get(temp);
			s.constant=val.get(temp);
			result.add(s);
		}
		
		return result;
	}
	
	//Application Testing -added to assign the value to the string from the column data value domain only

		String addValueToColumn_AppTest(String var,String op,String value,Vector<Column> columns,TableMap tableMap) throws Exception{
			int pos=-1;
			//String tableName= (var.split("\\["))[0].substring(2);
			String table= (var.split("\\["))[0];
			String tableName=table.split("O_")[1];
			int columnIndex=Integer.parseInt((var.split("\\."))[1]);
			Table t=tableMap.getTable(tableName);
			String cvcDataType=t.getColumn(columnIndex).getCvcDatatype();
			String res = null;
			for(Column c:columns){
				if(c.getColumnName().equals(cvcDataType)){
					
					int len=c.getColumnValues().size();
					for(int i=0;i<len;i++){
						int index = c.getColumnValues().get(i).indexOf("__");
						int l;
						String colVal = c.getColumnValues().get(i).substring(index+1);
						//colVal = colVal.replace(" ","");
						if(op.equalsIgnoreCase("=") ||op.equalsIgnoreCase(">=") ||op.equalsIgnoreCase("<=")){
							if(colVal.equals(value)){
								pos=i;
								res= c.getColumnValues().get(pos);
								break;
							}
						}
						else if(op.equalsIgnoreCase("!=") ||op.equalsIgnoreCase("<>") ||op.equalsIgnoreCase("/=")){
							if(! (colVal.equals(value))){
								pos=i;
								res= c.getColumnValues().get(pos);
								break;
							}
						}
						else if(op.equalsIgnoreCase("<")){
							int length1 = colVal.length();
							int length2 = value.length();
							for(int j =0;j<length1 && j<length2;j++){
								if(colVal.charAt(j) < value.charAt(j)){
									pos=i;
									res= c.getColumnValues().get(pos);
									break;
								}
								else
									break;
							}
							
						}
						else if(op.equalsIgnoreCase(">")){
							int length1 = colVal.length();
							int length2 = value.length();
							for(int j =0;j<length1 && j<length2;j++){
								if(colVal.charAt(j) > value.charAt(j)){
									pos=i;
									res= c.getColumnValues().get(pos);
									break;
								}
								else
									break;
							}
							
						}
					}
					if(pos==-1){
						pos=len;
						c.getColumnValues().add(value);
						res = value;
					}
					break;
				}
			}
			if(cvcDataType != null && value != null){
				value="_"+Utilities.escapeCharacters(cvcDataType)+"__"+Utilities.escapeCharacters(res);
				
			}
			//return pos;
			return value;
		}
		//end
		
		public String solveUsingAutomaton_AppTest(Vector<StringConstraint> constraints,Vector<Column> columns,TableMap tableMap){
			
			String var = constraints.get(0).var1;
			String table= (var.split("\\["))[0];
			String tableName=table.split("O_")[1];
			int columnIndex=Integer.parseInt((var.split("\\."))[1]);
			Table t=tableMap.getTable(tableName);
			String cvcDataType=t.getColumn(columnIndex).getCvcDatatype();
			String res = null;
			String op="",value="";
			int pos=-1;
			boolean find = false;
			for(Column c:columns){
				if(c.getColumnName().equals(cvcDataType)){
					
					int len=c.getColumnValues().size();
					for(int i=0;i<len && find==false;i++){
						int index = c.getColumnValues().get(i).indexOf("__");
						int l;
						String colVal = c.getColumnValues().get(i).substring(index+1);
						
						for(StringConstraint sc : constraints){
							op=sc.operator;
							value=sc.constant;
							pos=-1;
							if(! c.getColumnValues().contains(value) && (op.equalsIgnoreCase("==") || op.equalsIgnoreCase("="))){
								c.getColumnValues().add(value);
								//pos = len;
								len++;
							}
							else if(op.equalsIgnoreCase("=") ||op.equalsIgnoreCase(">=") ||op.equalsIgnoreCase("<=") || op.equalsIgnoreCase("==")){
								if(colVal.equals(value)){
									pos=i;
									res= c.getColumnValues().get(pos);
									//break;
								}
							}
							else if(op.equalsIgnoreCase("!=") ||op.equalsIgnoreCase("<>") ||op.equalsIgnoreCase("/=")){
								if(! (colVal.equals(value))){
									pos=i;
									res= c.getColumnValues().get(pos);
									//break;
								}
							}
							else if(op.equalsIgnoreCase("<")){
								int length1 = colVal.length();
								int length2 = value.length();
								for(int j =0;j<length1 && j<length2;j++){
									if(colVal.charAt(j) < value.charAt(j)){
										pos=i;
										res= c.getColumnValues().get(pos);
										//break;
									}
									else
										break;
								}
								
							}
							else if(op.equalsIgnoreCase(">")){
								int length1 = colVal.length();
								int length2 = value.length();
								for(int j =0;j<length1 && j<length2;j++){
									if(colVal.charAt(j) > value.charAt(j)){
										pos=i;
										res= c.getColumnValues().get(pos);
										break;
									}
									else
										break;
								}
								
							}
							if(pos==-1){
								break;
							}
						}
						if(pos!=-1 && res!=null){
							find =true;
						}
						}
						
						
						
						
						
					if(pos==-1){
						//call for old automation
					}
					
				}
			}
			if(cvcDataType != null && res != null){
				try {
					//value=Utilities.escapeCharacters(cvcDataType)+"__"+Utilities.escapeCharacters(res);
					value=res;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			//return pos;
			return value;
			
			
		}
		
	
}
