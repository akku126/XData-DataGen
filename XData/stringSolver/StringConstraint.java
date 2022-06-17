package stringSolver;

import java.util.Vector;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.RegExp;
import generateConstraints.ConstraintGenerator;
import generateConstraints.GetSolverHeaderAndFooter;

public class StringConstraint {
	String var1;
	String var2;
	String constant;
	String operator;
	final static char minChar='!';
	final static char maxChar='~';
	//final static char minChar='A';
	//final static char maxChar='z';
	StringConstraint(String v1,String v2, String c, String op){
		var1=v1;
		var2=v2;
		c=constant;
		op=operator;
	}
	/*
	 * constr = operator (tableName_columnNameCOLID (select O_tableName tupleId)) 'constant'
	 * var1 = tableName_columnNameCOLID (select O_tableName tupleId)
	 * operators:  =, <, >, not(=, ~ (LIKE), !~ (NOT LIKE), i~ (ILIKE), !i~ (NOT ILIKE)
	 */
	public StringConstraint(String constr){
		constr = constr.trim();
		//System.out.println("Constraint string: "+constr);
		//TEST CODE....
//		Context ctx = ConstraintGenerator.ctx;
//		ConstraintGenerator c = new ConstraintGenerator();
//		Params p = ctx.mkParams();
//		p.add("smt.macro_finder", true);
//		p.add("model.compact", false);
//
//		Solver s = ctx.mkSolver();
//		s.setParameters(p);
//		String temp = "(declare-fun intNullVal () Int)\n"
//				+ "(assert (= intNullVal (- 99996)))\n"
//				+ "\n"
//				+ "(declare-fun realNullVal () Real)\n"
//				+ "(assert (= realNullVal (- 99996.0)))"
//				+ "(declare-datatypes ((GRADE 0)) (((_GRADE__B) (_GRADE__C) (_GRADE__C_m) (_GRADE__B_m) (_GRADE__B_p2B) (_GRADE__C_p2B) (_GRADE__A_m) (_GRADE__F) (_GRADE__A) (_GRADE__H) (_GRADE__I) (_GRADE__J) (_GRADE__K) (_GRADE__L) (_GRADE__M) (_GRADE__N) (_GRADE__O) (_GRADE__G) (NULL_GRADE_1))))"
//				+ "(declare-datatypes ((ID 0)) (((_ID__00128) (_ID__12345) (_ID__19991) (_ID__23121) (_ID__44553) (_ID__45678) (_ID__54321) (_ID__55739) (_ID__70557) (_ID__76543) (_ID__76653) (_ID__98765) (_ID__98988) (_ID__V14) (_ID__V15) (_ID__V16) (_ID__V17) (_ID__V18) (_ID__V19) (_ID__V20) (NULL_ID_1))))\n"
//				+ "(declare-datatypes ((COURSE_ID 0)) (((_COURSE_uID__BIO_m301) (_COURSE_uID__CS_m347) (_COURSE_uID__FIN_m201) (_COURSE_uID__CS_m315) (_COURSE_uID__EE_m181) (_COURSE_uID__BIO_m101) (_COURSE_uID__MU_m199) (_COURSE_uID__HIS_m351) (_COURSE_uID__PHY_m101) (_COURSE_uID__CS_m319) (_COURSE_uID__CS_m101) (_COURSE_uID__CS_m190) (_COURSE_uID__V13) (_COURSE_uID__V14) (_COURSE_uID__V15) (_COURSE_uID__V16) (_COURSE_uID__V17) (_COURSE_uID__V18) (_COURSE_uID__V19) (_COURSE_uID__V20) (NULL_COURSE_ID_1))))\n"
//				+ "(declare-datatypes ((SEC_ID 0)) (((_SEC_uID__2) (_SEC_uID__1) (_SEC_uID__V3) (_SEC_uID__V4) (_SEC_uID__V5) (_SEC_uID__V6) (_SEC_uID__V7) (_SEC_uID__V8) (_SEC_uID__V9) (_SEC_uID__V10) (_SEC_uID__V11) (_SEC_uID__V12) (_SEC_uID__V13) (_SEC_uID__V14) (_SEC_uID__V15) (_SEC_uID__V16) (_SEC_uID__V17) (_SEC_uID__V18) (_SEC_uID__V19) (_SEC_uID__V20) (NULL_SEC_ID_1))))\n"
//				+ "(declare-datatypes ((SEMESTER 0)) (((_SEMESTER__Summer) (_SEMESTER__Spring) (_SEMESTER__Fall) (_SEMESTER__V4) (_SEMESTER__V5) (_SEMESTER__V6) (_SEMESTER__V7) (_SEMESTER__V8) (_SEMESTER__V9) (_SEMESTER__V10) (_SEMESTER__V11) (_SEMESTER__V12) (_SEMESTER__V13) (_SEMESTER__V14) (_SEMESTER__V15) (_SEMESTER__V16) (_SEMESTER__V17) (_SEMESTER__V18) (_SEMESTER__V19) (_SEMESTER__V20) (NULL_SEMESTER_1))))"
//				+ "(declare-fun checkYEAR (Int) Bool)\n"
//				+ "(assert (forall ((i_YEAR Int))\n"
//				+ "  (= (checkYEAR i_YEAR)\n"
//				+ "     (or (and (> i_YEAR 2008) (< i_YEAR 2011)) (= i_YEAR (- 99996))))))\n"
//				+ "\n"
//				+ "(declare-fun ISNULL_YEAR (Int) Bool)\n"
//				+ "(assert (forall ((year Int)) (= (ISNULL_YEAR year) (or (= year intNullVal)))))\n"
//				+ "\n"
//				+ ""
//				+ "(declare-datatypes ((takes_TupleType 0)) (((takes_TupleType (takes_ID0 ID) (takes_COURSE_ID1 COURSE_ID) (takes_SEC_ID2 SEC_ID) (takes_SEMESTER3 SEMESTER) (takes_YEAR4 Int) (takes_GRADE5 GRADE)))))"
//				+ "(declare-fun O_takes () (Array Int takes_TupleType))"
//				+ "(assert (= (takes_GRADE5 (select O_takes 1)) _GRADE__G  ))";  // 
//		byte[] data =  temp.getBytes(); 
//		
//		String smtStr = "";
//		try{
//			smtStr = new String(data, "UTF-8");
//			BoolExpr[] exprs = ctx.parseSMTLIB2String(smtStr, null, null, null, null);
//			System.out.println("Length "+exprs.length);
//			for(BoolExpr b: exprs) {
//				System.out.println(">>>>");
//				try {
//				for(Expr e: b.getArgs())
//					System.out.println(e);
//				}
//				catch(Exception e) {
//					System.out.println("Exception: " + e);
//				}
//			}
//		}
//		catch(Exception e) {
//			System.out.println("Exception: " + e);
//		}
		
		/******* TEST CODE **************/
		if(constr.contains("'")) {
			if(constr.contains("not")) {// constr = not (operator (tableName_columnNameCOLID (select O_tableName tupleId)) 'constant')
				constr = constr.split("not")[1].trim();
				constr = constr.substring(1,constr.length()-1); // removing parenthesis
				operator = "!"+constr.substring(0,1);
			}
			else if(constr.contains("!i~")) // not ilike
				operator = constr.substring(0,3);

			else if(constr.contains("!~") || constr.contains("i~")) 
				operator = constr.substring(0,2);

			else
				operator = constr.substring(0,1);

			var1 = constr.substring( constr.indexOf("("), constr.lastIndexOf(")")+1);
			constant = constr.substring(constr.indexOf("'")+1, constr.lastIndexOf("'"));
			var2 = null;
		}
		 /* 
		 * Temporary code. FIXME: Add code for var1 relop var2
		 * Does XData support var1 relop var2 where var2 is output of constrained aggregation?
		 */
		else {
			operator = "=";
			var1 = "1";
			var2 = "1";
			constant = null;
		}
	}
	StringConstraint(){}
	@Override
	public StringConstraint clone(){
		StringConstraint c=new StringConstraint();
		c.var1=new String(var1);
		c.var2=new String(var2);
		c.operator=new String(operator);
		c.operator=new String(operator);
		return c;
	}
	
	@Override
	public String toString(){
		if(var2==null)
			return new String(var1+" "+operator+" '"+constant+"'");
		else 
			return new String(var1+" "+operator+" "+var2);
	}
	
	
	public int compareTo(StringConstraint s1,StringConstraint s2){
		return s1.toString().compareTo(s2.toString());
	}
	
	@Override
	public int hashCode(){
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object j){
		String s=j.toString();
		return this.toString().equals(s);
	}
	
	
	/**
	 * Given a set of constraints on a single variable returns the value satisfying those constraints
	 * @param constraints
	 * @return value satisfying those constraints
	 */
	public static Automaton giveAutomatonForConstraints(Vector<StringConstraint> constraints){
		Automaton a=new RegExp("["+minChar+"-"+maxChar+"]*").toAutomaton();
		Automaton b=new Automaton();
		for(StringConstraint s:constraints){
			if(s.operator.equalsIgnoreCase(">")){
				String temp=convertForGreater(s.constant);
				b=new RegExp(temp).toAutomaton();
			}
			if(s.operator.equalsIgnoreCase("<")){
				String temp=convertForLess(s.constant);
				b=new RegExp(temp).toAutomaton();			
			}
			if(s.operator.equalsIgnoreCase(">=")){
				String temp=convertForGreaterEqual(s.constant);
				b=new RegExp(temp).toAutomaton();				
			}
			if(s.operator.equalsIgnoreCase("<=")){
				String temp=convertForLessEqual(s.constant);
				b=new RegExp(temp).toAutomaton();				
			}
			if(s.operator.equalsIgnoreCase("~")){
				String temp=convertForLike(s.constant);
				b=new RegExp(temp).toAutomaton();	
			}
			if(s.operator.equalsIgnoreCase("i~")){
				String temp=convertForILike(s.constant);
				b=new RegExp(temp).toAutomaton();				
			}
			if(s.operator.equalsIgnoreCase("!~")){
				String temp=convertForLike(s.constant);
				b=new RegExp(temp).toAutomaton();
				b=BasicOperations.complement(b);				
			}
			if(s.operator.equalsIgnoreCase("!i~")){
				String temp=convertForILike(s.constant);
				b=new RegExp(temp).toAutomaton();
				b=BasicOperations.complement(b);				
			}
			
			
			//Added by Biplab
			if(s.operator.equalsIgnoreCase("!=") || s.operator.equalsIgnoreCase("<>") || s.operator.equalsIgnoreCase("/=")){
				if(s.constant.equals(""))
				{
					String temp1=convertForGreater(s.constant);
					b=new RegExp(temp1).toAutomaton();
				}
				else
				{
					Automaton c = new Automaton();
					String str = convertForGreater(s.constant);
					Automaton d = new RegExp(str).toAutomaton();
					str = convertForLess(s.constant);
					Automaton e = new RegExp(str).toAutomaton();
					b = BasicOperations.union(d, e);
					str = convertForGreaterEqual("0");
					d = new RegExp(str).toAutomaton();
					str = convertForLess(":");
					e = new RegExp(str).toAutomaton();
					c = BasicOperations.intersection(d, e);
					str = convertForGreaterEqual("A");
					d = new RegExp(str).toAutomaton();
					str = convertForLess("[");
					e = new RegExp(str).toAutomaton();
					c = BasicOperations.union(c, BasicOperations.intersection(d, e));
					str = convertForGreaterEqual("a");
					d = new RegExp(str).toAutomaton();
					str = convertForLess("{");
					e = new RegExp(str).toAutomaton();
					c = BasicOperations.union(c, BasicOperations.intersection(d, e));
					b = BasicOperations.intersection(b, c);
				}
			}
			//Added by Biplab
			
			
			a=BasicOperations.intersection(a, b);
		}
		return a;
	}
	
	/**
	 * Generates a regular expression for any like operator on str	
	 * @param str
	 * @return The generated regular expression
	 */
	private static String convertForLike(String str)	{
		str=str.replace("_","["+minChar +"-~]");
		str=str.replace("%", "[ -~]*");
		return str;
	}
		
	/**
	 * Generates a regular expression for any ilike operator on str	
	 * @param str
	 * @return The generated regular expression
	 */ 
	private static String convertForILike(String str)	{
		char[] temp=str.toCharArray().clone();
		str="";
		for(int tempPos=0;tempPos<temp.length;tempPos++)
		{
			if (temp[tempPos]>='A' && temp[tempPos]<='Z')	{
					str=str+"["+temp[tempPos]+"|"+(char)(temp[tempPos]+32)+"]";
					
				}				
			else if(temp[tempPos]>='a' && temp[tempPos]<='z')	{
				str=str+"["+temp[tempPos]+"|"+(char)(temp[tempPos]-32)+"]";
					
				}
			else str+=temp[tempPos];
			
		}
		//str=new String(temp);
		str=str.replace("_","["+minChar +"-~]");
		str=str.replace("%", "[ -~]*");
		
		return str;
	}
	
	/**
	 * Generates a regular expression for any string greater than str	
	 * @param str
	 * @return The generated regular expression
	 */
	private static String convertForGreater(String str)	{
		String greater=str+"["+minChar +"-~][ -~]*";
		int l = str.length();
		for(int i=0;i<l;i++)	{
			String temp="|"+str;
			char c = str.charAt(i);
			c++;
			temp=temp.substring(0,i+1);
			
			temp+="["+c+"-~]["+minChar +"-~]*"; 
			greater+=temp;
		}
		return greater;
	}
	
	/**
	 * Generates a regular expression for any string greater than or equal to str	
	 * @param str
	 * @return The generated regular expression
	 */
	private static String convertForGreaterEqual(String str)	{
		return str+"|"+convertForGreater(str);
	}
	
	/**
	 * Generates a regular expression for any string less than str	
	 * @param str
	 * @return The generated regular expression
	 */
	private static String convertForLess(String str)	{
		String less="";
		int l = str.length();
		for(int i=0;i<l;i++)	{
			char c=str.charAt(i);
			c--;
			String temp=str.substring(0,i);
			temp+="["+minChar +"-"+c+"]["+minChar +"-~]*";
			
			less+="|"+temp;
		}
		return less.substring(1,less.length());
	}
	
	/**
	 * Generates a regular expression for any string less than or equal to str	
	 * @param str
	 * @return The generated regular expression
	 */
	private static String convertForLessEqual(String str)	{
		return str+"|"+convertForLess(str);
	}


}
