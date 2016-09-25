package parsing;

import java.io.Serializable;
import java.util.Vector;

public class ORNode implements Serializable{

	
	private static final long serialVersionUID = 8983251754086400463L;
	public ORNode(){
		andNodes = new Vector<ANDNode>();
		leafNodes = new Vector<Node>();
	}
	public Vector<ANDNode> andNodes;
	public Vector<Node> leafNodes;
	
	
}
