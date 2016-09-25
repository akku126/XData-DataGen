/**
 * 
 */
package parsing;

import java.io.Serializable;
import java.util.Vector;
import parsing.Column;
/**
 * @author shree
 *
 */
public class CaseCondition implements Cloneable,Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//This holds the case condition that needs to be satisfied for getting constantValue
	Node caseConditionNode;
	String caseOperator = "";
	/**
	 * @return the caseOperator
	 */
	public String getCaseOperator() {
		return caseOperator;
	}

	/**
	 * @param caseOperator the caseOperator to set
	 */
	public void setCaseOperator(String caseOperator) {
		this.caseOperator = caseOperator;
	}

	//This variable holds the column name to which the "then"  value will be assigned
	Column colValueForConjunct;
	/**
	 * @return the colValueForConjunct
	 */
	public Column getColValueForConjunct() {
		return colValueForConjunct;
	}

	/**
	 * @param colValueForConjunct the colValueForConjunct to set
	 */
	public void setColValueForConjunct(Column colValueForConjunct) {
		this.colValueForConjunct = colValueForConjunct;
	}

	//This variable holds the constant given in the "then" part
	String constantValue;

	
	//To handle Case within Case condition, vector to hold inner CaseConditions
	Vector <CaseCondition> caseWithinCase = new Vector<CaseCondition>();
	
	String caseCondition;

	/**
	 * @return the caseConditionNode
	 */
	public Node getCaseConditionNode() {
		return caseConditionNode;
	}

	/**
	 * @param caseConditionNode the caseConditionNode to set
	 */
	public void setCaseConditionNode(Node caseConditionNode) {
		this.caseConditionNode = caseConditionNode;
	}

	/**
	 * @return the constantValue
	 */
	public String getConstantValue() {
		return constantValue;
	}

	/**
	 * @param constantValue the constantValue to set
	 */
	public void setConstantValue(String constantValue) {
		this.constantValue = constantValue;
	}

	/**
	 * @return the caseCondition
	 */
	public String getCaseCondition() {
		return caseCondition;
	}

	/**
	 * @param caseCondition the caseCondition to set
	 */
	public void setCaseCondition(String caseCondition) {
		this.caseCondition = caseCondition;
	}
	
	@Override
	public CaseCondition clone() throws CloneNotSupportedException{
	
		Object obj= super.clone();
		Node left=new Node();
		Node right= new Node();
		
		if(this.getCaseConditionNode() !=null)
			left=this.getCaseConditionNode().clone(); 
		
		((CaseCondition)obj).setCaseConditionNode(left);
		return (CaseCondition)obj;
	}
	
}
