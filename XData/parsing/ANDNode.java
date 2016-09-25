package parsing;

import java.io.Serializable;
import java.util.Vector;

public class ANDNode implements Serializable{

	public ANDNode() {
		// TODO Auto-generated constructor stub
		orNodes=new Vector<ORNode>();
		leafNodes=new Vector<Node>();
	}
	public Vector<ORNode> orNodes;
	public Vector<Node> leafNodes;
}
