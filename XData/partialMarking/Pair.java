package partialMarking;

import parsing.Node;

public class Pair {
	
	public Node first;
	
	public Node second;
	
	public Pair(Node n1, Node n2){
		this.first = n1;
		this.second = n2;
	}
	
	@Override
	public String toString(){
		return this.first.toString() + "->" + this.second.toString();
	}

}
