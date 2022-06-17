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
import util.Configuration;
import util.TableMap;
import util.Utilities;

/**
 * 
 * @author bikash
 *
 */
public class StringConstraintSolver implements Serializable
{
	private static Logger logger = Logger.getLogger(StringConstraintSolver.class.getName());
	private static final long serialVersionUID = 4110121024171756914L;
	int count;			//count of files
	
	//Constructor
	public StringConstraintSolver()	{
		//stringConstraintsMap = new HashMap<String,Vector<String>>();
		count=0;
	}
	
	
	public Vector<String> solveConstraints(String assertConstraints,Vector<Column> columns,TableMap tableMap) throws Exception{
		Vector<String> constraints=new Vector<String>();
		constraints.add(assertConstraints);
		return solveConstraints(constraints,columns,tableMap,true);
	}
	
	public String removeBraces(String str){
		while(str.startsWith("(")){
			str=str.substring(1, str.length()-2);
			str=str.trim();
		}
		return str;
	}

	//Added by Amol to handle OR conditions in string constraints.
	public Vector<String> solveOrConstraints(Vector<String> assertConstraints,Vector<Column> columns,TableMap tableMap) throws Exception {
		Vector<String> stringConstraints = new Vector<String>();
		Vector<String> Final=new Vector<String>();
		for(String str:assertConstraints){
			if(str.startsWith("ASSERT")){
				str=str.substring(7, str.length());
				str=str.substring(0, str.length()-1);
				str=str.trim();
			}
			//str=removeBraces(str);
/*			if(str.startsWith("(")){
				
			}*/
			String Ors[]=str.split(" OR ");
			for(String EachOr : Ors){
				//EachOr=removeBraces(EachOr);
				String AndConstraint[] = EachOr.trim().split(" AND ");
				for(String And: AndConstraint){
					//And=removeBraces(And);
					And=And.trim();
					stringConstraints.add(And);
				}
				Vector<String> ret1;
				ret1=solveConstraints(stringConstraints, columns,tableMap,true);
				if(ret1!=null){
					Final.addAll(ret1);
					break;
				}
			}
		}
		return Final;
		
	}
	
	//Added by Shree to handle OR conditions in String conditions in SMT LIB constraint format
	public Vector<String> solveOrConstraintsForSMT(Vector<String> assertConstraints,Vector<Column> columns,TableMap tableMap) throws Exception {
		Vector<String> stringConstraints = new Vector<String>();
		Vector<String> Final=new Vector<String>();
		String finalStr ="";
		for(String str:assertConstraints){
			if(str.startsWith("ASSERT")){
				str=str.substring(7, str.length());
				str=str.substring(0, str.length()-1);
				str=str.trim();
			}
			String Ors[]=str.split(" OR ");
			for(String EachOr : Ors){
				//EachOr=removeBraces(EachOr);
				stringConstraints.clear();
				String AndConstraint[] = EachOr.trim().split(" AND ");
				for(String And: AndConstraint){
					//And=removeBraces(And);
					And=And.trim();
					stringConstraints.add(And);
				}
				Vector<String> ret1;
				ret1= solveConstraints(stringConstraints, columns,tableMap,true);
				
				if(ret1!=null){
					//NEW CODE: POOJA
					for(String conStr: ret1) {
						String temp = conStr;
						if(conStr.contains("assert")) {
							temp = conStr.split("assert")[1].trim();
							temp = temp.substring(0,temp.lastIndexOf(")")).trim();
						}
						finalStr += temp+" ";
					}
					
					//Final.addAll(ret1);
					//break;
				}
			}
		}
		finalStr = "(assert (or "+finalStr+" ))";
		Final.add(finalStr);
		//System.out.println(finalStr);
		return Final;
		
	}
	
	/**
	 * Takes a Vector of String constraints and instantiates the corresponding string variables to CVC compatible form
	 * @param dataConstraints String constraints for generation of datasets
	 * @return CVC constraints corresponding to the given string constraints
	 */
	public Vector<String> solveConstraints(Vector<String> assertConstraints,Vector<Column> columns,TableMap tableMap, boolean withAssert) throws Exception {
		Vector<StringConstraint> resultingConstraints = new Vector<StringConstraint>();
		Vector<StringConstraint> stringConstraints = new Vector<StringConstraint>();
		//Vector<String> assertOrNot=new Vector<String>();
		//System.out.println("INPUT Constraints:\n "+assertConstraints);
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
					//FIXME: Temporary fix
					if(And.contains("assert")) {
						And = And.split("assert")[1].trim();
						And = And.substring(0,And.lastIndexOf(")")).trim();
						StringConstraint s=new StringConstraint(And.substring(And.indexOf("(")+1, And.lastIndexOf(")"))); //removing brackets
						stringConstraints.add(s);
					}
					else {
						StringConstraint s=new StringConstraint(And.substring(And.indexOf("(")+1, And.lastIndexOf(")"))); //removing brackets
						stringConstraints.add(s);
					}
					
				}
			}
		}
		//System.out.println(stringConstraints);
		
		//TODO:correct this
		//Collections.sort(stringConstraints, new StringConstraint());
		//-----------------------simplify constraints
		//Collections.sort(constraints, new StringConstraint());
		Vector<StringConstraint> equal=new Vector<StringConstraint>();
		boolean flag=removeRedundantConds(stringConstraints,equal);
		if(flag==false){
				return null;
		}		
		for(StringConstraint s:equal)	//constraints of the form of var=const can be assigned directly
			resultingConstraints.add(s);
		

		
		HashMap<String,String> equalVar=new HashMap<String,String>();
		flag=removeEqualConds(stringConstraints,equalVar);
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

			getReleatedConstraints(s.var1,relatedConstraints,stringConstraints);

			if(s.var2!=null)
				getReleatedConstraints(s.var2,relatedConstraints,stringConstraints);


			stringConstraints.removeAll(relatedConstraints);
			Vector<StringConstraint> rel=new Vector<StringConstraint>();
			rel.addAll(relatedConstraints);
			Vector<StringConstraint> result= solveRelatedConstraints(rel);	
			if(result== null || result.size()==0 ){
				Vector<String> v=new Vector<String>();
				if(Configuration.getProperty("smtsolver").equalsIgnoreCase("cvc3"))
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
			String str=addValueToColumn(c.var1,c.constant,columns,tableMap);
			//c.constant=i+"";
			//vStr.add("ASSERT  ("+c.var1+"="+i+");\n"); // Old code, constraint string is in cvc3 format
			
			String finalstr = "(assert (= " + c.var1 +" "+ str + "  ))\n"; // added by POOJA
			vStr.add(finalstr);
			//System.out.println("FINAL Constraints:  "+finalstr);
			//vStr.add("ASSERT  ("+c.var1+"="+str+");\n");
			//vStr.add(c.toString());
			
		}
		return vStr;
	}
		
	/**
	 * Recursive function to find which constraints depend on the variable var
	 * @param var Variable on which the constraints must depend
	 * @param relatedConstraints HashSet to which related constraints are written
	 * @param dataConstraints Vector from which the constraints are considered
	 * @return 
	 */
	public void  getReleatedConstraints(String var,HashSet<StringConstraint> relatedConstraints,Vector<StringConstraint> dataConstraints)	{
		if(dataConstraints.size()==0) return ;
		int l=dataConstraints.size();
		for(int i=0;i<l;i++){
			if(dataConstraints.size()<=i) return;
			if(dataConstraints.get(i).var1.equalsIgnoreCase(var)){
				relatedConstraints.add(dataConstraints.get(i));
				Vector<StringConstraint> givenConstraints=(Vector<StringConstraint>)dataConstraints.clone();
				givenConstraints.remove(i);
				if(dataConstraints.get(i).var2!=null)
					getReleatedConstraints(dataConstraints.get(i).var2,relatedConstraints,givenConstraints);
			}
			if(dataConstraints.get(i).var2!=null && dataConstraints.get(i).var2.equalsIgnoreCase(var)){
				relatedConstraints.add(dataConstraints.get(i));
				Vector<StringConstraint> givenConstraints=(Vector<StringConstraint>)dataConstraints.clone();
				givenConstraints.remove(i);
				getReleatedConstraints(dataConstraints.get(i).var1,relatedConstraints,givenConstraints);
			}
		}	
		return;
	}
	
	/**
	 * Removes the inequalities by replacing it with < or > and then solves the grouped constraints
	 * @param toBeAdded constraints that remain to be added
	 * @param added Constraints that have been added and need to be passed to string solver
	 * @param noOfVar number of variables in the constraints
	 * @return
	 */
	public HashMap<Integer,String> solveGroupedConstraintsWithInequalities(Vector<StringConstraint> toBeAdded, Vector<StringConstraint> added,int noOfVar){
		if(toBeAdded != null && toBeAdded.size()!=0){
			Vector<StringConstraint> temp=(Vector<StringConstraint>)toBeAdded.clone();
			for(StringConstraint c:temp){
				toBeAdded.remove(c);
				if(c.operator.equals("<>") && c.var2 != null){
					StringConstraint constraint= new StringConstraint(c.var1+" < "+c.var2);
					added.add(constraint);
					HashMap<Integer,String> values= solveGroupedConstraintsWithInequalities(toBeAdded,added,noOfVar);
					if(values!=null) /////////////// if var1 <var2 makes the contrints unsolvable try with var1 >var2
						return values;
					added.remove(constraint);
					constraint= new StringConstraint(c.var1+" > "+c.var2);
					added.add(constraint);
					return solveGroupedConstraintsWithInequalities(toBeAdded,added,noOfVar); // if even this is unsolvable then the inequality is unsolvable
				}
				else{
					added.add(c);
				}
			}
		}
		
		return solveMultipleUsingGraph(added,noOfVar);
	}
	
	/**
	 * Finds the solution to the related strings
	 * @param constraints The related String constraints
	 * @return Value of strings satisfying the constrained
	 */
	private Vector<StringConstraint> solveRelatedConstraints(Vector<StringConstraint> constraints)	{
		Vector<StringConstraint> result=new Vector<StringConstraint>();
		
		
		boolean flag=true;
		int length=constraints.size();
	
		
		//----------------------Solve for Single variable---------------------
		for(int i=0;i<length;i++){		
			StringConstraint s=constraints.get(i);
	
			if(s.var2!=null || s.operator.startsWith("L")){ 
				flag=false;
				break;
			}
		}
	
		if(flag){
			
			String str;
			if(length==1)		//only a single constraint
				str=solveSingleConstraint(constraints.get(0));
			
			else{				//multiple constraints				
				str=solveUsingAutomaton(constraints);
				
			}
			if(str==null) return null;
			else result.add(new StringConstraint(constraints.get(0).var1+" = '"+ str+"'"));	
			return result;
		}
		/*
		//--------------------------Solve for multiple variables - only variables are present-------------------------
		flag=true;
		for(int i=0;i<length;i++){		
			StringConstraint s=constraints.get(i);
			if(s.var2==null){ 
				flag=false;
				break;
			}
		}
		if(flag){
			result= solveMultipleWithoutAutomata(constraints);
			return(result);
		}
		
		//----------------------------Solve for multiple variables - like and other constant comparison only on a single variable
		flag=true;
		String var=null;
		for(int i=0;i<length;i++){		
			StringConstraint s=constraints.get(i);
			if(s.var2==null)
				if(var==null){ 
					var=s.var1;
				}
				else{
					if(!s.var1.equalsIgnoreCase(var)){
						flag=false;
						break;
					}
				}		
		}
		if(flag){
			result= solveMultipleUsingAutomata(constraints);
			return(result);
		}
		*/
		//-----------------------------multiple variables by enumerating possible combinations----------------------------------------
		
		/*v=solveMultipleUsingGraph(constraints);	//if hashmap is not to be used uncomment this line and convert v to a vector of strings
		for(StringConstraint s:v){						//
			result.add(s.toString());					//
		}												//*/
		BiMap<String,Integer> varMap;
		varMap =HashBiMap.create();
		int j=0;
		for(int i=0;i<length;i++){		
			StringConstraint s=constraints.get(i);
			
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
		HashMap<Integer,String> val= solveGroupedConstraintsWithInequalities(constraints, new Vector<StringConstraint>(),varMap.size());
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
	
	/**
	 * If there is only a single constraint on a single variable of the for <var> op <expr> this function can solve by substituting
	 * @param str The single string constraint
	 * @return The value of the string
	 */
	public String solveSingleConstraint(StringConstraint str){
		if(str.operator.equalsIgnoreCase(">=") || str.operator.equalsIgnoreCase("<=") ){
			return str.constant;
		}
		
		//Modified by Biplab. Original code above
//		if(str.operator.equalsIgnoreCase(">")){
		if(str.operator.equalsIgnoreCase(">") || str.operator.equals("<>") || str.operator.equals("!=") || str.operator.equals("/=")){
			
			StringBuffer temp=new StringBuffer(str.constant);
			int l=temp.length();
			for(int i=0;i<l;i++){
				
				//Modified by Biplab. Original code above
//				if(temp.charAt(i)!='~'){
//					temp.setCharAt(0,(char)(temp.charAt(0)+1));
				if(temp.charAt(i) != '~' && temp.charAt(i) != '9' && temp.charAt(i) != 'Z' && temp.charAt(i) != 'z'){
					temp.setCharAt(i,(char)(temp.charAt(i)+1));
					
					return temp.toString();
				}
			}
			return temp.toString()+"A";
		}
		if(str.operator.equalsIgnoreCase("<")){
			StringBuffer temp=new StringBuffer(str.constant);
			int l=temp.length();
			for(int i=0;i<l;i++){
				
				//Modified by Biplab. Original code above
//				if(temp.charAt(i)!='~'){
//					temp.setCharAt(0,(char)(temp.charAt(0)-1));
				if(temp.charAt(i) != '~' && temp.charAt(i) != '0' && temp.charAt(i) != 'A' && temp.charAt(i) != 'a'){
					temp.setCharAt(i,(char)(temp.charAt(i)-1));
					
					return temp.toString();
				}
			}
			
			//Modified by Biplab. Original code above
//			return temp.deleteCharAt(temp.length()-1).toString();
			temp.setCharAt(0,(char)(temp.charAt(0)-1));
			return temp.toString();
		}
		if(str.operator.equalsIgnoreCase("i~") || str.operator.equalsIgnoreCase("~")){
			String temp=str.constant.replace('_', 'a');
			return temp.replace("%", "aa");
		}
		if(str.operator.equalsIgnoreCase("!i~") || str.operator.equalsIgnoreCase("!~")){
			StringBuffer temp=new StringBuffer(str.constant);
			if(temp.length()==0) return "a";
			int i=0;
			while(temp.charAt(i) == '_' || temp.charAt(i)== '%')	i++;
			if(temp.charAt(i)!='~')
				temp.setCharAt(i,(char)(temp.charAt(i)+1));
			else 
				temp.setCharAt(i,(char)(temp.charAt(i)-1));
			String temp1=temp.toString().replace('_', 'a');
			return temp1.replace("%", "aa");
		}
		
		return "";
	}
	
	/**
	 * Given a set of constraints on a single variable returns the value satisfying those constraints
	 * @param constraints
	 * @return value satisfying those constraints
	 */
	private String solveUsingAutomaton(Vector<StringConstraint> constraints){
		Automaton a=StringConstraint.giveAutomatonForConstraints(constraints);
		return BasicOperations.getShortestExample(a,true);
	}
	
	/**
	 * If all the string constraints have only variables then this function is used to assign suitable values to the strings
	 * @param constraints
	 * @return
	 */
	private Vector<String> solveMultipleWithoutAutomata(Vector<StringConstraint> constraints){
		
		Vector<String> values=new Vector<String>();
		
		return values;
	}
	
	/**
	 * In case only one variable has constraints with constants that variable is found using automata and remaining variables are claculated from it
	 * @param constraints
	 * @return
	 */
	private Vector<String> solveMultipleUsingAutomata(Vector<StringConstraint> constraints){
		
		Vector<String> values=new Vector<String>();
		
		return values;
	}
	
	/**
	 * Enumerates some possible values of the strings and checks for satisfiability
	 * @param constraints
	 * @return
	 */
	private HashMap<Integer,String> solveMultipleUsingGraph(Vector<StringConstraint> constraints,int numberOfVariables){
		
		StringGraph g=new StringGraph(numberOfVariables);
		if(g.add(constraints)==false)
			return null;
		for(StringNode sn:g.node){
			sn.lessEqual.removeAll(sn.less); //if a var is in both and varEquql list no need to keep it in both
		}
		return g.solve();
	}
	
	/**
	 * Removes the conditions that are can be directly satisfied var = const
	 * @param constraints The String constraints from which redundant constraints are to be removed
	 * @return If it is impossible to satisfy the constraints returns false
	 */
	public static boolean removeRedundantConds(Vector<StringConstraint> input,Vector<StringConstraint> equal){
		
		String var,constant;
		StringConstraint sc=null;
		while(true){
			var="";
			constant="";
			for(StringConstraint s:input){
				if(s.operator.equalsIgnoreCase("=") && s.constant!=null){
					var=s.var1;
					constant=s.constant;
					sc=s;
				}
			}
			if(var.equalsIgnoreCase(""))
				break;
			equal.add(sc);
			input.remove(sc);
			Vector<StringConstraint> redundant=new Vector<StringConstraint>();
			for(StringConstraint s:input){
				if(s.var2!=null && s.var2.equalsIgnoreCase(var)){
					s.var2=null;
					s.constant=new String(constant);
				}
				else if(s.var1.equalsIgnoreCase(var) && s.var2!=null){
					s.var1=s.var2;
					s.var2=null;
					s.constant=constant;
					if(s.operator.equalsIgnoreCase(">"))
						s.operator="<";
					else if(s.operator.equalsIgnoreCase(">="))
						s.operator="<=";
					else if(s.operator.equalsIgnoreCase("<="))
						s.operator=">=";
					else if(s.operator.equalsIgnoreCase("<"))
						s.operator=">";
				}
				else if(s.var1.equalsIgnoreCase(var) && s.var2==null){
					if(s.operator.equalsIgnoreCase(">")){
						if(constant.compareTo(s.constant)>0)
							redundant.add(s);
						else return false;
					}
						
					else if(s.operator.equalsIgnoreCase(">=")){
						if(constant.compareTo(s.constant)>=0)
							redundant.add(s);
						else return false;
					}
						
					else if(s.operator.equalsIgnoreCase("<=")){
						if(constant.compareTo(s.constant)<=0)
							redundant.add(s);
						else return false;
					}
					else if(s.operator.equalsIgnoreCase("<")){
						if(constant.compareTo(s.constant)<0)
							redundant.add(s);
						else return false;
					}
					else if(s.operator.equalsIgnoreCase("=")){
						if(constant.compareTo(s.constant)==0)
							redundant.add(s);
						else return false;
					}
					else if(s.operator.equalsIgnoreCase("~")){
						 Pattern p = Pattern.compile(s.constant);
						 Matcher m = p.matcher(constant);
						 if(m.matches())
							 redundant.add(s);
						 else return false;
					}
						
					else if(s.operator.equalsIgnoreCase("!~")){
						 Pattern p = Pattern.compile(s.constant);
						 Matcher m = p.matcher(constant);
						 if(!m.matches())
							 redundant.add(s);
						 else return false;
					}
					else if(s.operator.equalsIgnoreCase("i~")){
						 Pattern p = Pattern.compile(s.constant.toLowerCase());
						 Matcher m = p.matcher(constant.toLowerCase());
						 if(m.matches())
							 redundant.add(s);
						 else return false;
					}
					else if(s.operator.equalsIgnoreCase("!i~")){
						 Pattern p = Pattern.compile(s.constant.toLowerCase());
						 Matcher m = p.matcher(constant.toLowerCase());
						 if(!m.matches())
							 redundant.add(s);
						 else return false;
					}
				}
			}
			input.removeAll(redundant);
			
		}
		return true;
	}
	
	/**
	 * Simplifies the constraints by replacing conditions of the form var1=var2 by replacing all instances of var1 with var2
	 * @param input All constraints
	 * @param equal Constraints of equality type
	 * @return If it is impossible to satisfy the constraints returns false
	 */
	public static boolean removeEqualConds(Vector<StringConstraint> input,HashMap<String,String> equal){
		String str1,str2;
		if(input.size()==0)	return true;
		while(true){
			str1=new String("");
			str2=new String("");
			Iterator<StringConstraint> i=input.iterator();
			StringConstraint temp=null;
			while(i.hasNext()){
				temp=i.next();
				if(temp.operator.equals("=") && temp.var2!=null){
					str1=new String(temp.var1);
					str2=new String(temp.var2);
					break;
				}		
			}
			if(str1.equalsIgnoreCase("")) break;
			equal.put(str1,str2);
			input.remove(temp);
			i=input.iterator();
			while(i.hasNext()) {
				temp=i.next();
				if(temp.var1.equals(str1))	temp.var1=str2;
				if(temp.var2!= null && temp.var2.equals(str1))	temp.var2=str2;		
			}
			Iterator<String> mapIter=equal.keySet().iterator();
			while(mapIter.hasNext()) {
				String str=mapIter.next();
				if(equal.get(str).equals(str1)){
					equal.put(str,str2);
				}
			}
			
		}
		
		return true;
	}
	
	String addValueToColumn(String var,String value,Vector<Column> columns,TableMap tableMap) throws Exception{
		int pos=-1;
		//String tableName= (var.split("\\["))[0].substring(2);
		String table= (var.split("\\["))[0];
		//String tableName=table.split("O_")[1];
		//int columnIndex=Integer.parseInt((var.split("\\."))[1]);
		
		/*
		 * TEST CODE: Pooja
		 */
		String temp = var.split(" ")[0];
		String tokens[] = temp.split("_");
		String tableName = tokens[0].substring(1);
		// This for loop is required when relation name contains underscore("_") 
		// In variable 'var', table name is in lower case and column name in upper case; eg: player_match_ROLE_DESC3
		for(int i=1; i<tokens.length; i++) {
			if(!Character.isUpperCase(tokens[i].charAt(0)))
				tableName += "_"+tokens[i];
			else
				break;
		}
		
		int columnIndex=Integer.parseInt(""+(temp.charAt(temp.length()-1)));
		//Table t=tableMap.getTable(tableName);
		Table t=tableMap.getTable(tableName.toUpperCase()); //added by rambabu
		String cvcDataType=t.getColumn(columnIndex).getCvcDatatype();
		for(Column c:columns){
			if(c.getColumnName().equals(cvcDataType)){
				
				int len=c.getColumnValues().size();
				for(int i=0;i<len;i++){
					if(c.getColumnValues().get(i) == null && value != null) 
						continue;
						
					if((value==null && c.getColumnValues().get(i) == null) || c.getColumnValues().get(i).equals(value)){
						pos=i;
						break;
					}
				}
				if(pos==-1){
					pos=len;
					c.getColumnValues().add(value);
				}
				break;
			}
		}
		if(cvcDataType != null && value != null){
			value="_"+Utilities.escapeCharacters(cvcDataType)+"__"+Utilities.escapeCharacters(value);
		}
		//return pos;
		return value;
	}
		
	
	public static void main(String[] args) throws Exception
	{
		StringConstraintSolver s=new StringConstraintSolver();
		Vector<String> vec = new Vector();
//		vec.add("ASSERT (O_CRSE[2].2 > 'ggegeg');");
//		vec.add("ASSERT (O_CRSE[2].2 != 'h');");
		vec.add("ASSERT (O_TAKES[2].1 != '');");
		vec.add("ASSERT (O_TAKES[2].1 != 'MU-199');");
		vec.add("ASSERT (O_STUDENT[2].1 > 'z');");
//		vec.add("ASSERT (O_STUDENT[2].1 < '0');");
		vec.add("ASSERT (O_TAKES[2].3 != 'Fall');");
		//vec.add("ASSERT (O_CRSE[2].1 > 'igegeg');");
		//vec.add("ASSERT (O_CRSE[2].3 > O_CRSE[2].1);");	
		//vec.add("ASSERT (O_CRSE[2].3 < O_CRSE[2].2);");
		//vec.add("ASSERT (O_CRSE[2].3 > O_CRSE[2].2);");
		//vec.add("ASSERT (O_CRSE[2].4 < O_CRSE[2].3);");
		//vec.add("ASSERT (O_CRSE[2].1 < 'jk');");
		//vec.add("ASSERT (O_CRSE[2].5 ~ '%Abc%ddhsh%');");
		//vec.add("ASSERT (O_CRSE[2].5 ~ '%qwAb%pqw');");
//		vec.add("ASSERT (A ~= B);");
//		vec.add("ASSERT (A > 'aB3');");
		//vec.add("ASSERT (A >= B);");
		//vec.add("ASSERT (A >= D);");
//		vec.add("ASSERT (C <> D);");
		//vec.add("ASSERT (C <= D);");
		long t=System.currentTimeMillis();
		Vector ret=s.solveConstraints(vec,null,null,true);
		if(ret==null) //System.out.println("UNSAT");
		//System.out.println(ret.size());
		for(int i=0;i<ret.size();i++){
			//System.out.println(ret.get(i));
		//System.out.println("time taken "+(System.currentTimeMillis()-t));
		}
		
	}		
}
