package parsing;

import java.io.Serializable;
import java.util.List;

public class RelationHierarchyNode implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4303159147720807436L;

	String tableName;	
	
	List<RelationHierarchyNode> notExistSubqueries;
	
	RelationHierarchyNode left;
	
	RelationHierarchyNode right;
	
	String nodeType;
	
	public RelationHierarchyNode(String type, RelationHierarchyNode l, RelationHierarchyNode r){
		this.nodeType = type;
		this.left = l;
		this.right = r;
	}
	
	public RelationHierarchyNode(String type, List<RelationHierarchyNode> l){
		this.nodeType = type;
		this.notExistSubqueries = l;
	}
	
	public RelationHierarchyNode(String tableName){
		this.nodeType = "_RELATION_";
		this.tableName = tableName;
	}
	
	public RelationHierarchyNode getLeft() {
		return left;
	}

	public void setLeft(RelationHierarchyNode left) {
		this.left = left;
	}

	public RelationHierarchyNode getRight() {
		return right;
	}

	public void setRight(RelationHierarchyNode right) {
		this.right = right;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String name){
		this.tableName = name;
	}
	
	public void setNotExistsSubQueries(List<RelationHierarchyNode> l){
		this.notExistSubqueries = l;
	}
	
	public RelationHierarchyNode getNotExistsNode(int i){
		return this.notExistSubqueries.get(i);
	}
}
