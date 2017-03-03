package parsing;

import java.sql.Types;
import java.util.Vector;

import parsing.ANDNode;
import parsing.JoinClauseInfo;
import parsing.ORNode;
import parsing.Node;

public class GetNodeQueryStructure {

	/*
	 * Get Selection Nodes in a separate vector selectionConds
	 */
	public static boolean getSelectionNode(ConjunctQueryStructure con, Node temp) {
		if (temp.containsConstant()) {
			con.selectionConds.add(temp);
			return true;
		}
		//If left or right node in the Conjunct holds the correlated table, then the node should be treated as selection condition
		if((temp.getLeft() != null && temp.getLeft().isCorrelated)
				|| (temp.getRight() != null && temp.getRight().isCorrelated)){
			con.selectionConds.add(temp);
			return true;
		}
		
		/*if(temp.getLeft() != null && temp.getLeft().column != null &&  (temp.getLeft().column.getDataType() != Types.VARCHAR || temp.getLeft().column.getDataType() != Types.CHAR  ||  temp.getLeft().column.getDataType() != Types.LONGVARCHAR)
				&&temp.getRight() != null && temp.getRight().column != null && (temp.getRight().column.getDataType() != Types.VARCHAR || temp.getRight().column.getDataType() != Types.CHAR  || temp.getRight().column.getDataType() != Types.LONGVARCHAR))
		{
			con.selectionConds.add(temp);
			return true;
		}*/
		
		return false;
	}

	public static boolean getSelectionNode(DisjunctQueryStructure dis, Node temp) {
		if (temp.containsConstant()) {
			dis.selectionConds.add(temp);
			return true;
		}
		//If left or right node in the Conjunct holds the correlated table, then the node should be treated as selection condition
		
		if((temp.getLeft() != null && temp.getLeft().isCorrelated)
				|| (temp.getRight() != null && temp.getRight().isCorrelated)){
			dis.selectionConds.add(temp);
			return true;
		}
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

	public static boolean getLikeNode(DisjunctQueryStructure dis, Node temp){
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
	 * Get equi-inner join nodes  in a separate vector 
	 */
	//FIXME: Mahesh modify this method to handle suub queries also
	public static boolean getJoinNodesForEC(ConjunctQueryStructure con, Node temp) {
		if (temp.getType().equalsIgnoreCase(Node.getBroNodeType())
				&& temp.getOperator().equalsIgnoreCase("=")) {
			if (temp.getLeft()!=null && temp.getRight()!=null 
					&&temp.getLeft().getType()!=null && temp.getRight().getType()!=null
					&&temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
					&& temp.getRight().getType().equalsIgnoreCase(Node.getColRefType())
					&& /* the following added by mathew on 2nd June 2016 in order to prevent 
					outer join conditions in the formation of equivalence classes */
					(temp.getJoinType()==null  || temp.getJoinType().equals(JoinClauseInfo.innerJoin))
					&& !temp.getLeft().isCorrelated && !temp.getRight().isCorrelated )
			{
				con.joinCondsForEquivalenceClasses.add(temp);
				return true;
			}
			
			//Modify :  If the = operatore on bronode contains correlation condition, it should be treated as selection condition and not as join Condition/Equivalence Condition
			if(temp.getLeft()!=null && temp.getRight()!=null 
					&&temp.getLeft().getType()!=null && temp.getRight().getType()!=null
					&&temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
					&& temp.getRight().getType().equalsIgnoreCase(Node.getColRefType())
					)
					
					{
				
			}
		}
		return false;
	} 
	
	/*
	 * Get non equi/non-inner join nodes  in a separate vector 
	 */
	public static boolean getJoinNodesAllOther(ConjunctQueryStructure con, Node temp) {
		if (temp!=null&&temp.getLeft()!=null && temp.getRight()!=null
				&&temp.getLeft().getType()!=null && temp.getRight().getType()!=null
				&& temp.getLeft().getTable() != null && temp.getRight().getTable() != null 
				&& !temp.getLeft().getTable().getTableName().equalsIgnoreCase(temp.getRight().getTable().getTableName())
				&& temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
				&& temp.getRight().getType().equalsIgnoreCase(Node.getColRefType()) 
				&& temp.getType().equalsIgnoreCase(Node.getBroNodeType())){

			if(temp.getOperator().equalsIgnoreCase("=")){
				if (temp.getJoinType()!=null&&(temp.getJoinType().equals(JoinClauseInfo.leftOuterJoin)|| temp.getJoinType().equals(JoinClauseInfo.rightOuterJoin)
						|| temp.getJoinType().equals(JoinClauseInfo.fullOuterJoin))) 
				{
					con.joinCondsAllOther.add(temp);
					return true;
				}
			}
			if	(temp.getJoinType()==null && !temp.getOperator().equalsIgnoreCase("=")) {
				con.joinCondsAllOther.add(temp);
				return true;
			}
			if (temp.getOperator().equalsIgnoreCase("/=")
					|| temp.getOperator().equalsIgnoreCase(">")
					|| temp.getOperator().equalsIgnoreCase(">=")
					|| temp.getOperator().equalsIgnoreCase("<")
					|| temp.getOperator().equalsIgnoreCase("<=")
					) {
				con.joinCondsAllOther.add(temp);
				return true;

			}
		}
		return false;
	}
	
	public static boolean getJoinNodesAllOther(DisjunctQueryStructure dis, Node temp) {
		if (temp.getType().equalsIgnoreCase(Node.getBroNodeType()) &&
				temp.getLeft()!=null && temp.getRight()!=null
						&&temp.getLeft().getType()!=null && temp.getRight().getType()!=null
				&& temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
				&& temp.getRight().getType().equalsIgnoreCase(Node.getColRefType())) {
			if (temp.getOperator().equalsIgnoreCase("/=")
					|| temp.getOperator().equalsIgnoreCase(">")
					|| temp.getOperator().equalsIgnoreCase(">=")
					|| temp.getOperator().equalsIgnoreCase("<")
					|| temp.getOperator().equalsIgnoreCase("<=")
					) {
				return true;
			}
			if(temp.getOperator().equalsIgnoreCase("=") && temp.getJoinType()!=null){
				if ( temp.getJoinType().equals(JoinClauseInfo.leftOuterJoin)|| temp.getJoinType().equals(JoinClauseInfo.rightOuterJoin)
						|| temp.getJoinType().equals(JoinClauseInfo.fullOuterJoin)) 
				{				
					dis.joinCondsAllOther.add(temp);
					return true;
				}
			}
			if	(temp.getJoinType()==null && !temp.getOperator().equalsIgnoreCase("=")) {
				dis.joinCondsAllOther.add(temp);
				return true;
			}
		}
		return false;
	}

	public static boolean getJoinNodesForEC(DisjunctQueryStructure dis, Node temp) {
		if (temp.getType().equalsIgnoreCase(Node.getBroNodeType())&&
				temp.getLeft()!=null && temp.getRight()!=null
						&&temp.getLeft().getType()!=null && temp.getRight().getType()!=null
				&& temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
				&& temp.getRight().getType().equalsIgnoreCase(Node.getColRefType())) {
			if (temp.getOperator().equalsIgnoreCase("=") && (temp.getJoinType()==null  || temp.getJoinType().equals(JoinClauseInfo.innerJoin))) {
				dis.joinCondsForEquivalenceClasses.add(temp);
				return true;
			}
		}
		return false;
	}
	




	public static Vector<Node> flattenNode(parsing.QueryStructure qStructure, Node node) {
		Vector<Node> flattenedNodes = new Vector<Node>();
		if(node != null){
			Node left=node.getLeft();
			if(node.getType().equalsIgnoreCase(Node.getNotNodeType())){
				//Node left=node.getLeft();
				if(left.getType().equalsIgnoreCase(Node.getInNodeType())){
					/* 
					 * left.setType(Node.getNotExistsNodeType());	
					 * 				left.setQueryIndex(node.getQueryIndex()); 
					 * left.setQueryType(node.getQueryType());
					 * commented by mathew and the line below added
					 */
					//left.isInNode = true;
					//left.setType(Node.getNotExistsNodeType());	
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
				else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())
						||left.getType().equals(Node.getBroNodeSubQType())//added by mathew on 17 oct 2016
						){
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
				flattenedNodes.add(left);
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
				flattenedNodes.addAll(flattenNode(qStructure, node.getLeft()));


				//Added by Biplab
				flattenedNodes.addAll(flattenNode(qStructure, node.getRight()));
			}
			else if (node != null && node.getType().equalsIgnoreCase(Node.getAndNodeType())) {
				flattenedNodes.addAll(flattenNode(qStructure, node.getLeft()));
				flattenedNodes.addAll(flattenNode(qStructure, node.getRight()));
			} else if (node.getType().equalsIgnoreCase(Node.getInNodeType())){

				/*if(node.getLhsRhs()!=null){
					Node lhsrhs = node.getLhsRhs();
					int queryIndex = lhsrhs.getRight().getQueryIndex();
					updateSubQueryConditions(queryIndex,lhsrhs,qStructure);
					//left.getSubQueryConds().add(lhsrhs);
					Node newNode = new Node();
					newNode.setQueryIndex(queryIndex);
					node.setLhsRhs(newNode);
					node.setType(Node.getExistsNodeType());	
					flattenedNodes.add(node);
				}*/			
				flattenedNodes.add(node);//added by mathew on 17 oct 2016

			}
			/* the expression: n.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) 
			 * from the If condition below removed by mathew on 29 June 2016
			 * corresponding All node type and Any node type expressions added
			 */
			else if( node.getType().equalsIgnoreCase(Node.getAllNodeType())
					|| node.getType().equalsIgnoreCase(Node.getAnyNodeType())
					|| node.getType().equalsIgnoreCase(Node.getBroNodeSubQType())) {

				flattenedNodes.add(node);
			} else if (node.getType().equalsIgnoreCase(Node.getExistsNodeType()) || node.getType().equalsIgnoreCase(Node.getNotExistsNodeType()))
			{
				flattenedNodes.add(node);
			}
		}
		return flattenedNodes;
	}

	public static ORNode flattenOr(Node node){
		ORNode rootOR = new ORNode();
		if(node == null)
			return rootOR;

		//if(!node.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()) &&
		//	!node.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
		if(node.getType().equalsIgnoreCase(Node.getNotNodeType())){
			Node left=node.getLeft();
			if(left.getType().equalsIgnoreCase(Node.getInNodeType())){

				/*
				Node lhsrhs = left.getLhsRhs();
				int queryIndex = lhsrhs.getRight().getQueryIndex();
				left.getSubQueryConds().add(lhsrhs);
				left.setLhsRhs(null);
				left.setType(Node.getNotExistsNodeType());	
				left.setQueryIndex(node.getQueryIndex());
				left.setQueryType(node.getQueryType());
				the above line commented by mathew on Oct 17 2016, and the 
				following line added*/
				left.setType(Node.getNotInNodeType());
			}
			else if(left.getType().equalsIgnoreCase(Node.getExistsNodeType()))
				left.setType(Node.getNotExistsNodeType());
			else if(left.getType().equalsIgnoreCase(Node.getLikeNodeType())){
				String str=left.getOperator();
				left.setOperator("!"+str);
			}
			else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())
					||left.getType().equals(Node.getBroNodeSubQType())//added by mathew on 17 oct 2016
					){
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
			rootOR.leafNodes.add(left);
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
				int queryIndex = lhsrhs.getRight().getQueryIndex();

				//left.getSubQueryConds().add(lhsrhs);
				Node newNode = new Node();
				newNode.setQueryIndex(queryIndex);
				node.setLhsRhs(newNode);
				node.setType(Node.getExistsNodeType());	
				rootOR.leafNodes.add(node);
			}
			rootOR.leafNodes.add(node);//added by mathew on 17 Oct 2016

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

		return rootOR;

	}

	public static ANDNode flattenAnd(Node node){
		ANDNode rootAND = new ANDNode();
		if(node!= null && node.getType().equalsIgnoreCase(Node.getNotNodeType())){
			Node left=node.getLeft();
			if(left.getType().equalsIgnoreCase(Node.getInNodeType())){
				left.setType(Node.getNotInNodeType());
			}
			else if(left.getType().equalsIgnoreCase(Node.getExistsNodeType()))
				left.setType(Node.getNotExistsNodeType());
			else if(left.getType().equalsIgnoreCase(Node.getLikeNodeType())){
				String str=left.getOperator();
				left.setOperator("!"+str);
			}
			else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())
					||left.getType().equals(Node.getBroNodeSubQType())//added by mathew on 17 oct 2016
					){
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
			rootAND.leafNodes.add(left);
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
		else if (node != null && node.getType()!=null &&  (node.getType().equalsIgnoreCase(Node.getAllNodeType())
				|| node.getType().equalsIgnoreCase(Node.getAnyNodeType())
				||  node.getType().equalsIgnoreCase(Node.getBroNodeSubQType()))) {
			rootAND.leafNodes.add(node);
		} else if (node != null && node.getType().equalsIgnoreCase(Node.getInNodeType())){

			Node lhsrhs = node.getLhsRhs();
			if(lhsrhs!=null){
				int queryIndex = lhsrhs.getRight().getQueryIndex();

				//left.getSubQueryConds().add(lhsrhs);
				Node newNode = new Node();
				newNode.setQueryIndex(queryIndex);
				node.setLhsRhs(newNode);
				node.setType(Node.getExistsNodeType());	
				rootAND.leafNodes.add(node);
			}
			rootAND.leafNodes.add(node); //line added by mathew on 17 october 201
		}else if ((node != null && node.getType().equalsIgnoreCase(Node.getExistsNodeType())) || (node != null && node.getType().equalsIgnoreCase(Node.getNotExistsNodeType()))) {
			rootAND.leafNodes.add(node);
		}
		return rootAND;	
	}



	public static Vector<Vector<Node>> flattenCNF(parsing.QueryStructure qStructure, Node node) {
		Vector<Vector<Node>> cnf= new Vector<Vector<Node>>();
		Vector<Node> flattenNodes = new Vector<Node>();
		if(node.getType().equalsIgnoreCase(Node.getNotNodeType())){
			Node left=node.getLeft();
			if(left.getType().equalsIgnoreCase(Node.getInNodeType())){
				left.setType(Node.getNotInNodeType());
			}
			else if(left.getType().equalsIgnoreCase(Node.getExistsNodeType()))
				left.setType(Node.getNotExistsNodeType());
			else if(left.getType().equalsIgnoreCase(Node.getLikeNodeType())){
				String str=left.getOperator();
				left.setOperator("!"+str);
			}
			else if(left.getType().equalsIgnoreCase(Node.getBroNodeType())
					||left.getType().equals(Node.getBroNodeSubQType())//line added by mathew on 17 oct 2016
					){
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
			flattenNodes.add(left);
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
			cnf.addAll(flattenCNF(qStructure, node.getRight()));
			cnf.addAll(flattenCNF(qStructure, node.getLeft()));
			//flattenNodes.addAll(flattenNode(node.getLeft()));
		}
		else if (node.getType().equalsIgnoreCase(Node.getAndNodeType())) {
			flattenNodes.addAll(flattenNode(qStructure, node.getLeft()));
			flattenNodes.addAll(flattenNode(qStructure, node.getRight()));
			cnf.add(flattenNodes);
		}
		/* the expression: n.getType().equalsIgnoreCase(Node.getAllAnyNodeType()) 
		 * from the If condition below removed by mathew on 29 June 2016
		 * corresponding All node type and Any node type expressions added
		 */
		else if (node.getType().equalsIgnoreCase(Node.getAllNodeType())
				|| node.getType().equalsIgnoreCase(Node.getAnyNodeType())
				|| node.getType().equalsIgnoreCase(Node.getBroNodeSubQType())) {
			flattenNodes.add(node);	
			cnf.add(flattenNodes);
		}  else if (node.getType().equalsIgnoreCase(Node.getInNodeType())){

			Node lhsrhs = node.getLhsRhs();
			if(lhsrhs!=null){
				int queryIndex = lhsrhs.getRight().getQueryIndex();

				//left.getSubQueryConds().add(lhsrhs);
				Node newNode = new Node();
				newNode.setQueryIndex(queryIndex);
				node.setLhsRhs(newNode);
				node.setType(Node.getExistsNodeType());	
				flattenNodes.add(node);
				cnf.add(flattenNodes);
			}
			flattenNodes.add(node);//added by mathew on 17 oct 2016
			cnf.add(flattenNodes);//added by mathew on 17 oct 2016

		}else if (node.getType().equalsIgnoreCase(Node.getExistsNodeType()) || node.getType().equalsIgnoreCase(Node.getNotExistsNodeType())) {
			flattenNodes.add(node);
			cnf.add(flattenNodes);
			//allDnfSubQuery.addAll(flattenCNF(node));
		}
		return cnf;

	}
	
	public static void updateSubQueryConditions(int queryIndex, Node lhsrhs, QueryStructure qStructure){
		if(qStructure.getWhereClauseSubqueries() != null && !qStructure.getWhereClauseSubqueries().isEmpty()){
			if(qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond() != null && !qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond().isEmpty()){
				qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond().get(qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond().size()-1).add(lhsrhs);
			}
			if(qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts() != null && !qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().isEmpty()){
				qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.add(lhsrhs);
			}else{
				qStructure.getWhereClauseSubqueries().get(queryIndex).setConjuncts(new Vector<ConjunctQueryStructure >());
				qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().add(new ConjunctQueryStructure(new Vector<Node>()));
				qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.add(lhsrhs);
			}
			//for(Node n: lhsrhs){			
				if(ConjunctQueryStructure.isStringSelection(lhsrhs,1) ){
					
					String str=lhsrhs.getRight().getStrConst();
					
					//if(str!=null && !str.contains("'"))
						//n.getRight().setStrConst("'"+str+"'");
						qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).stringSelectionConds.add(lhsrhs);
						qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.remove(lhsrhs);
				}
				else if(ConjunctQueryStructure.isStringSelection(lhsrhs,0)){
					qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).stringSelectionConds.add(lhsrhs);
					qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.remove(lhsrhs);
			//	}
			}
		}
	}
}
