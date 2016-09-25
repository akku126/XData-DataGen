package stringSolver;

import java.util.Vector;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicOperations;
import dk.brics.automaton.RegExp;


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
	StringConstraint(String constr){
		String[] arr=constr.trim().split("\\s+",3);
		var1=arr[0];
		operator=arr[1];
		if(arr[2].startsWith("\'")){
			constant=arr[2].substring(1,arr[2].length()-1);
			var2=null;
		}
		else if(arr[2].startsWith("_")){
			constant=arr[2].substring(1,arr[2].length()-1);
			operator="L"+operator;
		}
		else {
			var2=arr[2];
			constant=null;
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
