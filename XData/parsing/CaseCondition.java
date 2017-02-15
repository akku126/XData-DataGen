
	package parsing;
	import java.io.Serializable;
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
				left=this.getWhenNode().clone(); 
			
			if(this.getThenNode()!=null)
				right=this.getThenNode().clone();
			
			((CaseCondition)obj).setWhenNode(left);
			((CaseCondition)obj).setThenNode(right);
			
			return (CaseCondition)obj;
		}
		
		@Override
		public String toString(){
			return " WHEN "+this.getWhenNode()+" THEN "+this.getThenNode();
		}
		
	
	
}
