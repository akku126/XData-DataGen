package parsing;

import java.util.Vector;

import parsing.Node;

public class GetNode {

	/*
	 * Get Selection Nodes in a separate vector selectionConds
	 */
	public static boolean getSelectionNode(ConjunctQueryStructure con, Node temp) {
		if (temp.containsConstant()) {
			con.selectionConds.add(temp);
			return true;
		}
		/*if(temp.getType().equals(Node.getBroNodeType()) && !temp.getOperator().equals("=")){
			selectionConds.add(temp);
			return true;
		}*/
		return false;
	}

	public static boolean getSelectionNode(Disjunct dis, Node temp) {
		if (temp.containsConstant()) {
			dis.selectionConds.add(temp);
			return true;
		}
		/*if(temp.getType().equals(Node.getBroNodeType()) && !temp.getOperator().equals("=")){
			selectionConds.add(temp);
			return true;
		}*/
		return false;
	}
	
	//TODO soon Add the function getlikeNOde to get all like nodes
	//---Added by bikash---------------------------------------
	/**
	 * If the node is of like type adds it to a list of like conditions
	 * @param temp The node that is to be checked for like condition
	 * @return Whether the node has like or not
	 */
	//function is same as the previous one but we may need to modify this later
	public static boolean getLikeNode(ConjunctQueryStructure con, Node temp){
		if(temp.getType().equalsIgnoreCase(Node.getLikeNodeType())){//CharConstantNode
			con.likeConds.add(temp);
			return true;
		}
		return false;
	}
	//**********************************************************************/

	public static boolean getLikeNode(Disjunct dis, Node temp){
		if(temp.getType().equalsIgnoreCase(Node.getLikeNodeType())){//CharConstantNode
			dis.likeConds.add(temp);
			return true;
		}
		return false;
	}	

	/*
	 * Get SubQueryNodes in a separate vector subQueryConds
	 */
	public static boolean getSubQueryNode(ConjunctQueryStructure con, Node temp) {
		if (temp.getType().equalsIgnoreCase(Node.getInNodeType())) {
			con.allSubQueryConds.add(temp);
			return true;
		}
		return false;
	}

	/*
	 * Get joinNodes in a separate vector joinConds
	 */
	//FIXME: Mahesh modify this method to handle suub queries also
	public static boolean getJoinNodesForEC(ConjunctQueryStructure con, Node temp) {
		if (temp.getType().equalsIgnoreCase(Node.getBroNodeType())
				&& temp.getOperator().equalsIgnoreCase("=")) {
			if (temp.getLeft()!=null && temp.getRight()!=null &&temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
					&& temp.getRight().getType().equalsIgnoreCase(Node.getColRefType())
					&& /* the following added by mathew on 2nd June 2016 in order to prevent 
					outer join conditions in the formation of equivalence classes */
					(temp.joinType==null  || temp.joinType.equals(JoinClauseInfo.innerJoin))) 
				{
				con.joinCondsAllOther.add(temp);
				return true;
			}
		}
		return false;
	}
	
	public static boolean getJoinNodesForEC(Disjunct dis, Node temp) {
		if (temp.getType().equalsIgnoreCase(Node.getBroNodeType())
				&& temp.getOperator().equalsIgnoreCase("=")) {
			if (temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
					&& temp.getRight().getType().equalsIgnoreCase(
							Node.getColRefType())) {
				dis.joinConds.add(temp);
				return true;
			}
		}
		return false;
	}
	
	public static Vector<Node> flattenNode(QueryParser qParser, Node node) {
		Vector<Node> flattenedNodes = new Vector<Node>();
		if(node != null){
		Node left=node.getLeft();
		if(node.getType().equalsIgnoreCase(Node.getNotNodeType())){
			//Node left=node.getLeft();
			if(left.getType().equalsIgnoreCase(Node.getInNodeType())){
				//Node lhsrhs = left.getLhsRhs();
				//int queryIndex = lhsrhs.getRight().queryIndex;
				//qParser.getWhereClauseSubqueries().get(queryIndex).getAllConds().add(lhsrhs);
				//left.getSubQueryConds().add(lhsrhs);
				//left.setLhsRhs(null);
				left.setType(Node.getNotExistsNodeType());	
				left.setQueryIndex(node.getQueryIndex());
				left.setQueryType(node.getQueryType());
			}
			else if(left.getType().equalsIgnoreCase(Node.getExistsNodeType())){			
				left.setType(Node.getNotExistsNodeType());				
			}
			else if(left.getType().equalsIgnoreCase(Node.getLikeNodeType())){
				String str=left.getOperator();
				left.setOperator("!"+str);
			}
			else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())){
				String str=left.getOperator();
				if(str.equals("="))
					left.setOperator("/=");
				else if(str.equals("/="))
					left.setOperator("=");
				else if(str.equals(">"))
					left.setOperator("<=");
				else if(str.equals(">="))
					left.setOperator("<");
				else if(str.equals("<"))
					left.setOperator(">=");
				else if(str.equals("<="))
					left.setOperator(">");
			}
			else if(left.getType().equalsIgnoreCase(Node.getIsNullNodeType())){
				left.setOperator("!=");
			}
			flattenedNodes.add(node.getLeft());
		}

		if (node.getType().equalsIgnoreCase(Node.getBroNodeType())) {
			flattenedNodes.add(node);
		}
		if (node.getType().equalsIgnoreCase(Node.getIsNullNodeType())) {
			flattenedNodes.add(node);
		}
		//For like --added by bikash
		if(node.getType().equalsIgnoreCase(Node.getLikeNodeType())){
			flattenedNodes.add(node);
		}
		// Not appropriate for OR nodes. Think something about it.
		// Perhaps the CNF conversion would help as mentioned in the function
		// flattenAllConds()
		else if (node.getType().equalsIgnoreCase(Node.getOrNodeType())) {

			//Modified by Amol
			flattenedNodes.addAll(flattenNode(qParser, node.getLeft()));


			//Added by Biplab
			flattenedNodes.addAll(flattenNode(qParser, node.getRight()));
		}
		else if (node != null && node.getType().equalsIgnoreCase(Node.getAndNodeType())) {
			flattenedNodes.addAll(flattenNode(qParser, node.getLeft()));
			flattenedNodes.addAll(flattenNode(qParser, node.getRight()));
		} else if (node.getType().equalsIgnoreCase(Node.getInNodeType())){
			
			if(node.getLhsRhs()!=null){
				Node lhsrhs = node.getLhsRhs();
				int queryIndex = lhsrhs.getRight().queryIndex;
				if(qParser.getWhereClauseSubqueries() != null && !qParser.getWhereClauseSubqueries().isEmpty()){
					qParser.getWhereClauseSubqueries().get(queryIndex).getAllConds().add(lhsrhs);
				} 
				//left.getSubQueryConds().add(lhsrhs);
				Node newNode = new Node();
				newNode.setQueryIndex(queryIndex);
				node.setLhsRhs(newNode);
				node.setType(Node.getExistsNodeType());	
				flattenedNodes.add(node);
			}
		}
		/* the expression: n.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) 
		 * from the If condition below removed by mathew on 29 June 2016
		 * corresponding All node type and Any node type expressions added
		 */
		else if( node.getType().equalsIgnoreCase(Node.getAllNodeType())
				|| node.getType().equalsIgnoreCase(Node.getAnyNodeType())
				|| node.getType().equalsIgnoreCase(Node.getBroNodeSubQType())) {
			//Commented the line below to prevent addition of lhsRhs node to be 
			//considered for equivalence classes.
			//flattenedNodes.addAll(flattenNode(node.getLhsRhs()));
			flattenedNodes.add(node);
			/*Vector<Node> temp = node.getSubQueryConds();	//Why add subQ conditions on the full query
			for (int i = 0; i < temp.size(); i++) {
				flattenedNodes.addAll(flattenNode(temp.get(i)));
			}*/
		} else if (node.getType().equalsIgnoreCase(Node.getExistsNodeType()) || node.getType().equalsIgnoreCase(Node.getNotExistsNodeType()))
		{
			flattenedNodes.add(node);
		}
			/*Vector<Node> temp = node.getSubQueryConds();
			for (int i = 0; i < temp.size(); i++) {
				flattenedNodes.addAll(flattenNode(temp.get(i)));
			}*/
		/*else if(node.getType().equalsIgnoreCase(Node.getNotExistsNodeType())){
			Node left1=node.getLhsRhs();
			left1.setType(Node.getNotExistsNodeType());
			flattenedNodes.add(node.getLhsRhs());
		}
		else if(node.getType().equalsIgnoreCase(Node.getNotInNodeType())){
			//Node left=node.getLeft();
			Node lhsrhs = node.getLhsRhs();
			int queryIndex = lhsrhs.getRight().queryIndex;
			qParser.getWhereClauseSubqueries().get(queryIndex).getAllConds().add(lhsrhs);
			//Node left1=node.getLhsRhs().getLeft();
			lhsrhs.getSubQueryConds().add(lhsrhs);
			lhsrhs.setLhsRhs(null);
			lhsrhs.setType(Node.getNotExistsNodeType());	
			lhsrhs.setQueryIndex(node.getQueryIndex());
			lhsrhs.setQueryType(node.getQueryType());
			
			flattenedNodes.add(node.getLhsRhs());
		}
		else if(left.getType().equalsIgnoreCase(Node.getIsNullNodeType())){
			
			left.setOperator("!=");
		}*/
		}
		return flattenedNodes;
	}
	
	public static ORNode flattenOr(Node node){
		ORNode rootOR = new ORNode();
		if(node != null){
		//if(!node.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()) &&
			//	!node.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
		if(node.getType().equalsIgnoreCase(Node.getNotNodeType())){
			Node left=node.getLeft();
			if(left.getType().equalsIgnoreCase(Node.getInNodeType())){
				///left.getSubQueryConds().add(left.getLhsRhs());
				//left.setLhsRhs(null);
				//left.setType(Node.getNotExistsNodeType());		
				
				Node lhsrhs = left.getLhsRhs();
				int queryIndex = lhsrhs.getRight().queryIndex;
				left.getSubQueryConds().add(lhsrhs);
				left.setLhsRhs(null);
				left.setType(Node.getNotExistsNodeType());	
				left.setQueryIndex(node.getQueryIndex());
				left.setQueryType(node.getQueryType());
				
			}
			else if(left.getType().equalsIgnoreCase(Node.getExistsNodeType()))
				left.setType(Node.getNotExistsNodeType());
			else if(left.getType().equalsIgnoreCase(Node.getLikeNodeType())){
				String str=left.getOperator();
				left.setOperator("!"+str);
			}
			else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())){
				String str=left.getOperator();
				if(str.equals("="))
					left.setOperator("/=");
				else if(str.equals("/="))
					left.setOperator("=");
				else if(str.equals(">"))
					left.setOperator("<=");
				else if(str.equals(">="))
					left.setOperator("<");
				else if(str.equals("<"))
					left.setOperator(">=");
				else if(str.equals("<="))
					left.setOperator(">");
			}
			else if(left.getType().equalsIgnoreCase(Node.getIsNullNodeType())){
				left.setOperator("!=");
			}
			rootOR.leafNodes.add(node.getLeft());
		}
		if (node.getType().equalsIgnoreCase(Node.getBroNodeType())) {
			rootOR.leafNodes.add(node);
		}
		if (node.getType().equalsIgnoreCase(Node.getIsNullNodeType())) {
			rootOR.leafNodes.add(node);
		}
		if(node.getType().equalsIgnoreCase(Node.getLikeNodeType())){
			rootOR.leafNodes.add(node);
		}
		else if (node.getType().equalsIgnoreCase(Node.getOrNodeType())) {
			ORNode leftOr = flattenOr(node.getLeft());
			ORNode rightOr = flattenOr(node.getRight());
			rootOR.andNodes.addAll(leftOr.andNodes);
			rootOR.andNodes.addAll(rightOr.andNodes);
			rootOR.leafNodes.addAll(leftOr.leafNodes);
			rootOR.leafNodes.addAll(rightOr.leafNodes);
		}
		else if (node.getType().equalsIgnoreCase(Node.getAndNodeType())) {
			rootOR.andNodes.add(flattenAnd(node));
		}
		/* the expression: node.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) removed 
		 * from the if condition below removed, and All node type and any node type add
		 *  in the following statement added by mathew on 29 June 2016
		 */
		else if (node.getType().equalsIgnoreCase(Node.getAllNodeType())
				||node.getType().equalsIgnoreCase(Node.getAnyNodeType())
				|| node.getType().equalsIgnoreCase(Node.getBroNodeSubQType())) {
			rootOR.leafNodes.add(node);
		}
		 else if (node.getType().equalsIgnoreCase(Node.getInNodeType())){
				
				Node lhsrhs = node.getLhsRhs();
				if(lhsrhs!=null) {
					int queryIndex = lhsrhs.getRight().queryIndex;

					//left.getSubQueryConds().add(lhsrhs);
					Node newNode = new Node();
					newNode.setQueryIndex(queryIndex);
					node.setLhsRhs(newNode);
					node.setType(Node.getExistsNodeType());	
					rootOR.leafNodes.add(node);
				}
			}
		 else if (node.getType().equalsIgnoreCase(Node.getExistsNodeType()) || node.getType().equalsIgnoreCase(Node.getNotExistsNodeType())) {
			rootOR.leafNodes.add(node);
		}
		 
		
		//}
		/*else if(node.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()) &&
				!node.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
			if(node.getRight().getType().equalsIgnoreCase(Node.getValType())){
				node.setLeft(node.getRight());
				rootOR.leafNodes.add(node);
			}
			
		}else if(!node.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()) &&
				node.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
			if(node.getLeft().getType().equalsIgnoreCase(Node.getValType())){
				node.setRight(node.getLeft());
				rootOR.leafNodes.add(node);
			}
		}*/
		}
		return rootOR;
		
	}
	
	public static ANDNode flattenAnd(Node node){
		ANDNode rootAND = new ANDNode();
		if(node!= null && node.getType().equalsIgnoreCase(Node.getNotNodeType())){
			Node left=node.getLeft();
			if(left.getType().equalsIgnoreCase(Node.getInNodeType())){
				//left.getSubQueryConds().add(left.getLhsRhs());
				//left.setLhsRhs(null);
				//left.setType(Node.getNotExistsNodeType());	
				
				Node lhsrhs = left.getLhsRhs();
				int queryIndex = lhsrhs.getRight().queryIndex;
				left.getSubQueryConds().add(lhsrhs);
				left.setLhsRhs(null);
				left.setType(Node.getNotExistsNodeType());	
				left.setQueryIndex(node.getQueryIndex());
				left.setQueryType(node.getQueryType());
				
			}
			else if(left.getType().equalsIgnoreCase(Node.getExistsNodeType()))
				left.setType(Node.getNotExistsNodeType());
			else if(left.getType().equalsIgnoreCase(Node.getLikeNodeType())){
				String str=left.getOperator();
				left.setOperator("!"+str);
			}
			else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())){
				String str=left.getOperator();
				if(str.equals("="))
					left.setOperator("/=");
				else if(str.equals("/="))
					left.setOperator("=");
				else if(str.equals(">"))
					left.setOperator("<=");
				else if(str.equals(">="))
					left.setOperator("<");
				else if(str.equals("<"))
					left.setOperator(">=");
				else if(str.equals("<="))
					left.setOperator(">");
			}
			else if(left.getType().equalsIgnoreCase(Node.getIsNullNodeType())){
				left.setOperator("!=");
			}
			rootAND.leafNodes.add(node.getLeft());
		}
		if (node != null && node.getType().equalsIgnoreCase(Node.getBroNodeType())) {
			rootAND.leafNodes.add(node);
		}
		if (node != null && node.getType().equalsIgnoreCase(Node.getIsNullNodeType())) {
			rootAND.leafNodes.add(node);
		}
		if(node != null && node.getType().equalsIgnoreCase(Node.getLikeNodeType())){
			rootAND.leafNodes.add(node);
		}
		else if (node != null && node.getType().equalsIgnoreCase(Node.getOrNodeType())) {
			rootAND.orNodes.add(flattenOr(node));
		}
		else if (node != null && node.getType().equalsIgnoreCase(Node.getAndNodeType())) {
			ANDNode leftAnd = flattenAnd(node.getLeft());
			ANDNode rightAnd = flattenAnd(node.getRight());
			rootAND.orNodes.addAll(leftAnd.orNodes);
			rootAND.orNodes.addAll(rightAnd.orNodes);
			rootAND.leafNodes.addAll(leftAnd.leafNodes);
			rootAND.leafNodes.addAll(rightAnd.leafNodes);
		}
		/* the expression: node.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) removed 
		 * from the if condition below removed, and All node type and any node type add
		 *  in the following if condition added by mathew on 29 June 2016
		 */
		else if (node != null && node.getType().equalsIgnoreCase(Node.getAllNodeType())
				&& node.getType().equalsIgnoreCase(Node.getAnyNodeType())
				|| ((node != null && node.getType().equalsIgnoreCase(Node.getBroNodeSubQType())))) {
			rootAND.leafNodes.add(node);
		} else if (node != null && node.getType().equalsIgnoreCase(Node.getInNodeType())){
			
			Node lhsrhs = node.getLhsRhs();
			if(lhsrhs!=null){
			int queryIndex = lhsrhs.getRight().queryIndex;
			
			//left.getSubQueryConds().add(lhsrhs);
			Node newNode = new Node();
			newNode.setQueryIndex(queryIndex);
			node.setLhsRhs(newNode);
			node.setType(Node.getExistsNodeType());	
			rootAND.leafNodes.add(node);
			}
		}else if ((node != null && node.getType().equalsIgnoreCase(Node.getExistsNodeType())) || (node != null && node.getType().equalsIgnoreCase(Node.getNotExistsNodeType()))) {
			rootAND.leafNodes.add(node);
		}
		return rootAND;	
	}
	
	public static Vector<Vector<Node>> flattenCNF(QueryParser qParser, Node node) {
		Vector<Vector<Node>> cnf= new Vector<Vector<Node>>();
		Vector<Node> flattenNodes = new Vector<Node>();
		if(node.getType().equalsIgnoreCase(Node.getNotNodeType())){
			Node left=node.getLeft();
			if(left.getType().equalsIgnoreCase(Node.getInNodeType())){
				//left.getSubQueryConds().add(left.getLhsRhs());
				//left.setLhsRhs(null);
				//left.setType(Node.getNotExistsNodeType());	
				
				Node lhsrhs = left.getLhsRhs();
				int queryIndex = lhsrhs.getRight().queryIndex;
				left.getSubQueryConds().add(lhsrhs);
				left.setLhsRhs(null);
				left.setType(Node.getNotExistsNodeType());	
				left.setQueryIndex(node.getQueryIndex());
				left.setQueryType(node.getQueryType());
				
			}
			else if(left.getType().equalsIgnoreCase(Node.getExistsNodeType()))
				left.setType(Node.getNotExistsNodeType());
			else if(left.getType().equalsIgnoreCase(Node.getLikeNodeType())){
				String str=left.getOperator();
				left.setOperator("!"+str);
			}
			else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())){
				String str=left.getOperator();
				if(str.equals("="))
					left.setOperator("/=");
				else if(str.equals("/="))
					left.setOperator("=");
				else if(str.equals(">"))
					left.setOperator("<=");
				else if(str.equals(">="))
					left.setOperator("<");
				else if(str.equals("<"))
					left.setOperator(">=");
				else if(str.equals("<="))
					left.setOperator(">");
			}
			else if(left.getType().equalsIgnoreCase(Node.getIsNullNodeType())){
				left.setOperator("!=");
			}
			flattenNodes.add(node.getLeft());
			cnf.add(flattenNodes);
		}
		if (node.getType().equalsIgnoreCase(Node.getBroNodeType())) {
			flattenNodes.add(node);
			cnf.add(flattenNodes);
		}
		if (node.getType().equalsIgnoreCase(Node.getIsNullNodeType())) {
			flattenNodes.add(node);
			cnf.add(flattenNodes);
		}
		if(node.getType().equalsIgnoreCase(Node.getLikeNodeType())){
			flattenNodes.add(node);
			cnf.add(flattenNodes);
		}
		else if (node.getType().equalsIgnoreCase(Node.getOrNodeType())) {
			cnf.addAll(flattenCNF(qParser, node.getRight()));
			cnf.addAll(flattenCNF(qParser, node.getLeft()));
			//flattenNodes.addAll(flattenNode(node.getLeft()));
		}
		else if (node.getType().equalsIgnoreCase(Node.getAndNodeType())) {
			flattenNodes.addAll(flattenNode(qParser, node.getLeft()));
			flattenNodes.addAll(flattenNode(qParser, node.getRight()));
			cnf.add(flattenNodes);
		}
		/* the expression: n.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) 
		 * from the If condition below removed by mathew on 29 June 2016
		 * corresponding All node type and Any node type expressions added
		 */
		else if (node.getType().equalsIgnoreCase(Node.getAllNodeType())
				|| node.getType().equalsIgnoreCase(Node.getAnyNodeType())
				|| node.getType().equalsIgnoreCase(Node.getBroNodeSubQType())) {
			//Commented the line below to prevent addition of lhsRhs node to be 
			//considered for equivalence classes.
			//flattenedNodes.addAll(flattenNode(node.getLhsRhs()));
			flattenNodes.add(node);	
			cnf.add(flattenNodes);
			/*Vector<Node> temp = node.getSubQueryConds();	//Why add subQ conditions on the full query
			for (int i = 0; i < temp.size(); i++) {
				flattenedNodes.addAll(flattenNode(temp.get(i)));
			}*/
		}  else if (node.getType().equalsIgnoreCase(Node.getInNodeType())){
			
			Node lhsrhs = node.getLhsRhs();
			if(lhsrhs!=null){
				int queryIndex = lhsrhs.getRight().queryIndex;

				//left.getSubQueryConds().add(lhsrhs);
				Node newNode = new Node();
				newNode.setQueryIndex(queryIndex);
				node.setLhsRhs(newNode);
				node.setType(Node.getExistsNodeType());	
				flattenNodes.add(node);
				cnf.add(flattenNodes);
			}
		}else if (node.getType().equalsIgnoreCase(Node.getExistsNodeType()) || node.getType().equalsIgnoreCase(Node.getNotExistsNodeType())) {
			flattenNodes.add(node);
			cnf.add(flattenNodes);
			//allDnfSubQuery.addAll(flattenCNF(node));
			/*Vector<Node> temp = node.getSubQueryConds();
			for (int i = 0; i < temp.size(); i++) {
				flattenedNodes.addAll(flattenNode(temp.get(i)));*/
		}
		return cnf;

	}
}
