package parsing;
import java.util.Vector;
import org.apache.derby.impl.sql.compile.*;
/**
 * 
 * @author Sandeep Patidar
 * @co-author Suhas Vijay Kajbaje
 */
public class TreeNode {
	private String nodeType;
	//private String name;
	private QueryTreeNode nodeValue;
	//private TreeNode next;
	//private TreeNode prev;
	private Vector<TreeNode> children;
	private Vector<JoinClauseInfo> joinClauseInfoVector;
	private boolean isInnerJoin;
	public static final String rootNode = "ROOT";
	public static final String cpNode = "CP";	// Cartesian product
	public static final String joinNode = "JOIN";
	public static final String leafNode = "RELATION";
	public static final String selectionNode = "SELECTION";
	
	public TreeNode(String nodeType, QueryTreeNode value){
		this.nodeType = nodeType;
		this.nodeValue = value;
		this.children = new Vector<TreeNode>();
		this.joinClauseInfoVector = new Vector<JoinClauseInfo>();
	}
	
	public String getNodeType(){
		return nodeType;
	}
	
	public void setIsInnerJoin(boolean flag){
		this.isInnerJoin=flag;
	}
	
	public boolean getIsInnerJoin(){
		return this.isInnerJoin;
	}
	
	public QueryTreeNode getNodeValue(){
		return this.nodeValue;
	}
	
	public String getNodeName(){
		String nodeName="";
		if(getNodeType().equals(TreeNode.joinNode))
			nodeName = "-JoinNode-";
		else{
			nodeName = ((FromBaseTable)getNodeValue()).getBaseTableName();
		}
		return nodeName;
	}
	
	public void addChild(TreeNode node){
		this.children.add(node);
	}
	
	public Vector<TreeNode> getChildren(){
		return this.children;
	}
	
	public void setJoinClauseInfoVector(Vector<JoinClauseInfo> joinClauseInfoVector){
		this.joinClauseInfoVector = joinClauseInfoVector;
	}
	
	public Vector<JoinClauseInfo> getJoinClauseInfoVector(){
		return joinClauseInfoVector;
	}
	
	/*@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		for(int i=0; i<joinClauseInfoVector.size(); i++){
			if(i==0)
				s.append(joinClauseInfoVector.get(i).toString());
			else
				s.append(" and "+joinClauseInfoVector.get(i).toString());
		}
		return s.toString();
	}*/
}
