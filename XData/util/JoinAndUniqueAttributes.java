package util;

import java.util.HashSet;
import java.util.Set;
import parsing.*;

/**
 * Used to store the unique attributes and join attributes of each edge in the join graph
 * If the edge is from relation A to relation B then this class stores the attributes of relation A
 * @author mahesh
 *
 */
public class JoinAndUniqueAttributes {
	Set< Node > joinAttributes;
	Set < Set< Node > > uniqueElements;
	
	public JoinAndUniqueAttributes(){
		joinAttributes = new HashSet< Node >();
		uniqueElements = new HashSet< Set< Node > >();
	}

	public void addJoinAttributes(Node joinNode){
		this.joinAttributes.add(joinNode);
	}
	
	public Set< Node > getJoinAttributes(){
		return this.joinAttributes;
	}
	
	public void addUniqueElements(Set< Node > unique){
		this.uniqueElements.add(unique);
	}
	
	public Set< Set< Node > > getUniqueElements(){
		return this.uniqueElements;
	}
	
	@Override
	public String toString(){
		String out = "";
		for(Node n: joinAttributes)
			out += n.toString() + " ";
		return out;
	}
}
