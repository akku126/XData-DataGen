package stringSolver;
import java.util.*;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;




class StringNode{
	int maxL=8;
	int minL=1;
	int value;
	HashSet<Integer> less;
	HashSet<Integer> lessEqual;
	HashSet<Integer> caseInsensitiveEqual;
	Vector<StringConstraint> constraints;
	Set<Integer> notEqualLength;
}

public class StringGraph {
	
	StringNode[] node;
	StringGraph(int no){
		node=new StringNode[no];
		for(int i=0;i<no;i++){
			node[i]=new StringNode();
			node[i].value=i;
			node[i].less=new HashSet<Integer>();
			node[i].lessEqual=new HashSet<Integer>();
			node[i].caseInsensitiveEqual=new HashSet<Integer>();
			node[i].constraints=new Vector<StringConstraint>();	
			node[i].notEqualLength=new HashSet<Integer>();
		}
	}
	
	/**
	 * Adds a given constraint to the graph
	 * @param c The constraint
	 * @return False- The graph has cycles and hence the constraints cannot be solved. True- The constraints may be solved
	 */
	boolean add(StringConstraint c){
		String op=c.operator;
		if(c.var2==null){
			if(!op.startsWith("L")){
				node[Integer.parseInt(c.var1)].constraints.add(c);
				return true;
			}
			else if(op.equals("L>")){
				int var1=Integer.parseInt(c.var1);
				int len=Integer.parseInt(c.constant);
				if(node[var1].minL<len+1)
					node[var1].minL=len+1;
				if(node[var1].maxL<node[var1].minL)
					return false;
				else return true;
			}
			else if(op.equals("L>=")){
				int var1=Integer.parseInt(c.var1);
				int len=Integer.parseInt(c.constant);
				if(node[var1].minL<len)
					node[var1].minL=len;
				if(node[var1].maxL<node[var1].minL)
					return false;
				else return true;
			}
			else if(op.equals("L<")){
				int var1=Integer.parseInt(c.var1);
				int len=Integer.parseInt(c.constant);
				if(node[var1].maxL>len+1)
					node[var1].maxL=len+1;
				if(node[var1].maxL<node[var1].minL)
					return false;
				else return true;
			}
			else if(op.equals("L<=")){
				int var1=Integer.parseInt(c.var1);
				int len=Integer.parseInt(c.constant);
				if(node[var1].maxL>len)
					node[var1].maxL=len;
				if(node[var1].maxL<node[var1].minL)
					return false;
				else return true;
			}
			else if(op.equals("L=")){
				int var1=Integer.parseInt(c.var1);
				int len=Integer.parseInt(c.constant);
				if(node[var1].maxL>len){
					node[var1].maxL=len;
				}
				if(node[var1].minL<len){
					node[var1].minL=len;
				}
				if(node[var1].maxL<node[var1].minL)
					return false;
				else return true;
			}
			/*else if(op.equals("L<>")){
				int var1=Integer.parseInt(c.var1);
				int len=Integer.parseInt(c.constant);
				node[var1].notEqualLength.add(len);
				return true;
			}*/
		}
		if(op.equalsIgnoreCase("<")){
			/*String temp=c.var1;
			c.operator=">";
			c.var1=c.var2;
			c.var2=temp;
			op=">";*/
			int less=Integer.parseInt(c.var1);
			int var=Integer.parseInt(c.var2);
			node[var].less.add(less);
			return true;
		}
		else if(op.equalsIgnoreCase("<=")){
			int less=Integer.parseInt(c.var1);
			int var=Integer.parseInt(c.var2);
			node[var].lessEqual.add(less);
			return true;
		}
		if(op.equalsIgnoreCase(">")){
			int var=Integer.parseInt(c.var1);
			int less=Integer.parseInt(c.var2);
			if(var==less)	return false;
			
			node[var].less.add(less);
			return true;
		}
		else if(op.equalsIgnoreCase(">=")){
			int var=Integer.parseInt(c.var1);
			int less=Integer.parseInt(c.var2);
			
			node[var].lessEqual.add(less);
			return true;
		}
		else if(op.equalsIgnoreCase("~=")){
			int var1=Integer.parseInt(c.var1);
			int var2=Integer.parseInt(c.var2);
			if(var1==var2)	return false;
			node[var2].caseInsensitiveEqual.add(var1);
			node[var1].caseInsensitiveEqual.add(var2);
			return true;
		}
		
		
		return false;
	}
	
	boolean add(Vector<StringConstraint> v){
		for(StringConstraint  c :v){
			if(add(c)==false)
				return false;
		}
		return true;
	}
	
	String getSmallestStringFromAutomata(int varNo){
		int max=node[varNo].maxL;
		int min=node[varNo].minL;
		
		Automaton a=StringConstraint.giveAutomatonForConstraints(node[varNo].constraints);		
		
		if (a.isSingleton() && a.singleton.length() >=max && a.singleton.length()<=min)
			return a.getSingleton();
		else if (min >= 0)
			return getStrings(a.initial, new StringBuilder(),min,max,node[varNo].notEqualLength, 0);
		return null;	
	}
	

	
	/**
	 * Returns the set of accepted strings of the given length.
	 */
	public static String getStrings(Automaton a, int min,int max) {
		String str=null;
		if (a.isSingleton() && a.singleton.length() >=max && a.singleton.length()<=min)
			return a.singleton;
		else if (min >= 0)
			str=getStrings(a.initial, new StringBuilder(),min,max,null, 0);
		return str;
	}
	
	private static String getStrings(State s, StringBuilder path, int min,int max,Set<Integer> notEqualLengths,int length) {
		if(length>=min && length<=max && s.accept && !notEqualLengths.contains(length)){
			//strings.add(path.toString());
			return path.toString();
		}
		
		
		if(length>max)
			return null;
		else{
			Vector<Transition> temp=s.getTransitions();
			for (Transition t : temp)
				for (int n = t.min; n <= t.max; n+=1) {
					path.append((char)n);
					String str=getStrings(t.to, path, min,max,notEqualLengths,length +1);
					if (str!=null)
						return str;
					path.deleteCharAt(path.length() - 1);
				}
		}
		return null;
	}
		
		
		
		
		
		
	/*	if (length == 0) {
			if (s.accept)
				strings.add(path.toString());
		} else 
			for (Transition t : s.transitions)
				for (int n = t.min; n <= t.max; n+=1) {
					path.append((char)n);
					getStrings(t.to, strings, path, length - 1,0);
					path.deleteCharAt(path.length() - 1);
				}
		
		
	}*/
	
	
	
	
	
	HashMap<Integer,String> solve(){
		HashMap<Integer,String> map= new HashMap<Integer,String>();
		while(true){
			boolean flag=false;
			Vector<Integer> toSolve=new Vector<Integer>();
			for(int i=0;i<node.length;i++){
				if(node[i].value >=0 &&node[i].less.size()==0 && node[i].lessEqual.size()==0){
					toSolve.add(i);
				}
			}
			if(toSolve.size()==0){
				for(int i=0;i<node.length;i++){
					if(node[i].value >=0 &&node[i].less.size()==0){
						toSolve.add(i);
					}
					else flag=true;
				}
			
				if(toSolve.size()==0)
					return null;
				else if(flag=false){
					toSolve.removeAllElements();
					for(int i=0;i<node.length;i++){
						if(node[i].value >=0 &&node[i].lessEqual.size()==0){
							toSolve.add(i);
						}
					}
				}
			}
			Map<Integer,String> temp=findValuesFor(toSolve);
			
			if(temp==null) //could not solve
				return null;
			
			for(int i:toSolve){      //mark it solved
				node[i].value=-1;			
			}
			
			for(int i=0;i<node.length;i++){
				if(node[i].value>=0){
					for(int j:(HashSet<Integer>)(node[i].less).clone()){
						for(int k:toSolve){
							if(j==k){
								StringConstraint s=new StringConstraint();
								s.var1=new String(i+"");
								s.operator=">";
								s.constant=temp.get(k);
								node[i].constraints.add(s);
								node[i].less.remove(k);
							}
						}
					}
					for(int j:(HashSet<Integer>)node[i].lessEqual.clone()){
						for(int k:toSolve){
							if(j==k){
								StringConstraint s=new StringConstraint();
								s.var1=new String(i+"");
								s.operator=">=";
								s.constant=temp.get(k);
								node[i].constraints.add(s);
								node[i].lessEqual.remove(k);
							}
						}
					}
					for(int j:(HashSet<Integer>)node[i].caseInsensitiveEqual.clone()){
						for(int k:toSolve){
							if(j==k){
								StringConstraint s=new StringConstraint();
								s.var1=new String(i+"");
								s.operator="i~";
								s.constant=temp.get(k);
								node[i].constraints.add(s);
								node[i].lessEqual.remove(k);
							}
						}
					}
					
					
				}
			}
			
			map.putAll(temp);
			if(map.size()==node.length)
				break;
		}
		return map;
	}
	
	Map<Integer,String> findValuesFor(Vector<Integer> v){
		
		Map<Integer,String> value=new HashMap<Integer,String>();
		for(int i:v){
			String str=getSmallestStringFromAutomata(i);
			if (str==null) return null;
			node[i].value=-1;
			value.put(i,str);
			for(int j:node[i].lessEqual){
				if(node[j].value==-1)		continue;
				StringConstraint s=new StringConstraint();
				s.var1=new String(j+"");
				s.operator="<=";
				s.constant=str+"";
				node[j].constraints.add(s);
			}
			for(int j:node[i].caseInsensitiveEqual){
				if(node[j].value==-1)		continue;
				StringConstraint s=new StringConstraint();
				s.var1=new String(j+"");
				s.operator="i~";
				s.constant=str+"";
				node[j].constraints.add(s);
			}
		}
		return value;
	}
	
	
}
