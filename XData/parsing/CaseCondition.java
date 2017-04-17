
	package parsing;
	import java.io.Serializable;
	import java.util.Vector;
	import parsing.Column;
	/**
	 * @author shree, 
	 * modified by Mathew on 26 Jan 2017
	 *
	 */
	public class CaseCondition implements Cloneable,Serializable{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//This holds the case condition that needs to be satisfied for getting constantValue
		Node whenNode;
		Node thenNode;
		
		/**
		 * @return the caseConditionNode
		 */
		public Node getWhenNode() {
			return whenNode;
		}
		/**
		 * @param caseConditionNode the caseConditionNode to set
		 */
		public void setWhenNode(Node caseConditionNode) {
			whenNode = caseConditionNode;
		}
		/**
		 * @return the ResultNode
		 */
		public Node getThenNode() {
			return thenNode;
		}
		/**
		 * @param caseConditionNode the caseConditionNode to set
		 */
		public void setThenNode(Node node) {
			thenNode = node;
		}
		
		@Override
		public CaseCondition clone() throws CloneNotSupportedException{
		
			Object obj= super.clone();
			Node left=new Node();
			Node right= new Node();
			
			if(this.getWhenNode() !=null)
				left=(Node)this.getWhenNode().clone(); 
			
			if(this.getThenNode()!=null)
				right=(Node)this.getThenNode().clone();
			
			((CaseCondition)obj).setWhenNode(left);
			((CaseCondition)obj).setThenNode(right);
			
			return (CaseCondition)obj;
		}
		
		@Override
		public String toString(){
			return " WHEN "+this.getWhenNode()+" THEN "+this.getThenNode();
		}
		//Application Testing
		String caseCondition;
		public String getCaseCondition() {
			return caseCondition;
		}

		/**
		 * @param caseCondition the caseCondition to set
		 */
		public void setCaseCondition(String caseCondition) {
			this.caseCondition = caseCondition;
		}
		/**
		 * @return the caseConditionNode
		 */
		Node caseConditionNode;
		public Node getCaseConditionNode() {
			return caseConditionNode;
		}

		/**
		 * @param caseConditionNode the caseConditionNode to set
		 */
		public void setCaseConditionNode(Node caseConditionNode) {
			this.caseConditionNode = caseConditionNode;
		}
		
		String constantValue;
		public String getConstantValue() {
			return constantValue;
		}

		/**
		 * @param constantValue the constantValue to set
		 */
		public void setConstantValue(String constantValue) {
			this.constantValue = constantValue;
		}
		//end
	
	
}
