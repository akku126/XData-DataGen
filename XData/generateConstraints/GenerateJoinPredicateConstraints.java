package generateConstraints;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;

/**
 * This class is used to generate constraints for the join predicates
 * The join predicates can be equi-join or non-equi join predicates
 * TODO: Handling join conditions which involve aggregations like SUM(A.x) = B.x is part of future work
 * @author mahesh
 *
 */
public class GenerateJoinPredicateConstraints {
	
	private static Logger logger = Logger.getLogger(GenerateJoinPredicateConstraints.class.getName());
	/**
	 * Get the constraints for equivalence Classes by Considering repeated relations
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @return
	 * @throws Exception
	 */

	public static String getConstraintsForEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2) throws Exception{

		String constraintString = "";

		if(n1.getQueryType() == n2.getQueryType()){/**If both nodes are of same type (i.e. either from clause sub qury nodes or where clause sub query nodes or outer query block nodes*/
			if(n1.getQueryIndex() != n2.getQueryIndex()){/**This means these nodes correspond to two different from clause sub queries and are joined in the outer query block*/
				return getConstraintsForJoinsInDiffSubQueryBlocks(cvc, queryBlock, n1, n2, "=");
			}
			else{/**these are either correspond to from clause/ where clause/ outer clause*/
				return getConstraintsForJoinsInSameQueryBlock(cvc, queryBlock, n1, n2, "=");
			}
		}
		else{/**This means one node correspond to from/Where clause sub query and other node correspond to outer query block*/
			return getConstraintsForEquiJoinsInSubQBlockAndOuterBlock(cvc, queryBlock, n1, n2, "=");
		}		
	}

	/**
	 * Get the constraints for equivalence Classes by Considering repeated relations
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @return
	 * @throws Exception
	 */

	public static String getConstraintsForEquiJoins1(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2) throws Exception{

		String constraintString = "";

		if(n1.getQueryType() == n2.getQueryType()){/**If both nodes are of same type (i.e. either from clause sub query nodes or where clause sub query nodes or outer query block nodes*/
			if(n1.getQueryIndex() != n2.getQueryIndex()){/**This means these nodes correspond to two different from clause sub queries and are joined in the outer query block*/
				return getConstraintsForJoinsInDiffSubQueryBlocks1(cvc, queryBlock, n1, n2, "=");
			}
			else{/**these are either correspond to from clause/ where clause/ outer clause*/
				return getConstraintsForJoinsInSameQueryBlock1(cvc, queryBlock, n1, n2, "=");
			}
		}
		else{/**This means one node correspond to from clause sub query and other node correspond to outer query block*/
			return getConstraintsForEquiJoinsInSubQBlockAndOuterBlock1(cvc, queryBlock, n1, n2, "=");
		}		
	}

	/**
	 * Wrapper method Used to generate constraints for the non equi join conditions of the conjunct
	 * @param cvc
	 * @param queryBlock
	 * @param allConds
	 * @return
	 * @throws Exception
	 */

	public static String getConstraintsForNonEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Vector<Node> allConds) throws Exception{

		String constraintString = "";
		for(Node n: allConds)
			constraintString += getConstraintsForNonEquiJoins(cvc, queryBlock, n.getLeft(), n.getRight(), n.getOperator());
		return constraintString;
	}


	/**
	 * Wrapper method Used to generate constraints negative for the non equi join conditions of the conjunct
	 * @param cvc
	 * @param queryBlock
	 * @param allConds
	 * @return
	 * @throws Exception
	 */
	public static String getNegativeConstraintsForNonEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Vector<Node> allConds) throws Exception{



		Vector<Node> allCondsDup = new Vector<Node>();

		for(Node node: allConds){
			Node n = new Node(node);
			if(n.getOperator().equals("="))
				n.setOperator("/=");
			else if(n.getOperator().equals("/="))
				n.setOperator("=");
			else if(n.getOperator().equals(">"))
				n.setOperator("<=");
			else if(n.getOperator().equals("<"))
				n.setOperator(">=");
			else if(n.getOperator().equals("<="))
				n.setOperator(">");
			else if(n.getOperator().equals(">="))
				n.setOperator("<");
		}

		return getConstraintsForNonEquiJoins(cvc, queryBlock, allCondsDup);
	}

	/**
	 * Used to generate constraints for the non equi join conditions of the conjunct
	 * @param cvc
	 * @param queryBlock
	 * @param left
	 * @param right
	 * @param operator
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForNonEquiJoins(GenerateCVC1 cvc,	QueryBlockDetails queryBlock, Node left, Node right, String operator) throws Exception{

		if(left.getQueryType() == right.getQueryType()){/**If both nodes are of same type (i.e. either from clause sub qury nodes or where clause sub query nodes or outer query bl;ock nodes)*/
			if(left.getQueryIndex() != right.getQueryIndex()){/**This means these nodes correspond to two different from clause sub queries and are joined in the outer query block*/
				return getConstraintsForJoinsInDiffSubQueryBlocks(cvc, queryBlock, left, right, operator);
			}
			else{/**these are either correspond to from clause/ where clause/ outer clause*/
				return getConstraintsForJoinsInSameQueryBlock(cvc, queryBlock, left, right, operator);
			}
		}
		else{/**This means one node correspond to from clause sub query and other node correspond to outer query block*/
			return getConstraintsForEquiJoinsInSubQBlockAndOuterBlock(cvc, queryBlock, left, right, operator);
		}
	}


	/**
	 * Gets constraints for nodes which are involved in join conditions where one node is in outer query block and other node is in from clause sub query
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 */
	public static String getConstraintsForEquiJoinsInSubQBlockAndOuterBlock(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2, String operator) {
		String constraintString = "";

		/** Let make n1 as sub query node and n2 as outer query node */
		if(n1.getQueryType() == 0){
			Node temp = new Node(n1);
			n1 = new Node(n2);
			n2 = temp;			
		}

		int leftGroup = 1;

		/**get number of groups for the from clause nested subquery block*/
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		/** get the details of each node */
		String t1 = getTableName(n1);
		String t2 = getTableName(n2);
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());

		String r1 = getTableNameNo(n1);
		String r2 = getTableNameNo(n2);
		int offset1= cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2= cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1=0, tuples2=0;
		if(cvc.getNoOfTuples().containsKey(r1)){
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if(cvc.getNoOfTuples().containsKey(r2)){
			tuples2 = cvc.getNoOfTuples().get(r2);
		}

		/** Do a round robin for the smaller value of the group number */
		for(int k=1,l=1;; k++,l++){
			constraintString += "ASSERT ("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
					GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, (l+offset2-1))+");\n";
			if(leftGroup>tuples2){
				if(l==tuples2 && k<leftGroup)	l=0;
				if(k>=leftGroup) break;
			}
			else if(leftGroup<tuples2){
				if(l<tuples2 && k==leftGroup)	k=0;
				if(l>=tuples2) break;				
			}
			else{//if tuples1==tuples2
				if(l==leftGroup) break;
			}
		}

		return constraintString;
	}

	/**
	 * Gets constraints for nodes which are involved in join conditions where one node is in outer query block and other node is in from clause sub query
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 */
	public static String getConstraintsForEquiJoinsInSubQBlockAndOuterBlock1(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2, String operator) {
		String constraintString = "";

		/** Let make n1 as sub query node and n2 as outer query node */
		if(n1.getQueryType() == 0){
			Node temp = new Node(n1);
			n1 = new Node(n2);
			n2 = temp;			
		}

		int leftGroup = 1;

		/**get number of groups for the from clause nested subquery block*/
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		/** get the details of each node */
		String t1 = n1.getColumn().getTableName();
		String t2 = n2.getColumn().getTableName();
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());

		String r1 = n1.getTableNameNo();
		String r2 = n2.getTableNameNo();
		int offset1= cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2= cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1=0, tuples2=0;
		if(cvc.getNoOfTuples().containsKey(r1)){
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if(cvc.getNoOfTuples().containsKey(r2)){
			tuples2 = cvc.getNoOfTuples().get(r2);
		}

		/** Do a round robin for the smaller value of the group number */
		for(int k=1,l=1;; k++,l++){
			constraintString += "("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
					GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, (l+offset2-1))+") AND ";
			if(leftGroup>tuples2){
				if(l==tuples2 && k<leftGroup)	l=0;
				if(k>=leftGroup) break;
			}
			else if(leftGroup<tuples2){
				if(l<tuples2 && k==leftGroup)	k=0;
				if(l>=tuples2) break;				
			}
			else{//if tuples1==tuples2
				if(l==leftGroup) break;
			}
		}

		return constraintString;
	}

	/**
	 * Gets  constraints for nodes which are involved in join conditions which are in same query block
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 */
	public static String getConstraintsForJoinsInSameQueryBlock(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2, String operator) {

		String constraintString = "";

		/** get the details of each node */
		String t1 = getTableName(n1);
		String t2 = getTableName(n2);

		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());

		String r1 = getTableNameNo(n1);
		String r2 = getTableNameNo(n2);
		//System.out.println("relation2 name num  ---"+r2);
		logger.log(Level.INFO,"relation2 name num  ---"+r2);
		
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1 = 0, tuples2=0;
		if(cvc.getNoOfTuples().containsKey(r1)){

			tuples1 = cvc.getNoOfTuples().get(r1);
		}

		if(cvc.getNoOfTuples().containsKey(r2)){

			tuples2 = cvc.getNoOfTuples().get(r2);
		}

		int noOfgroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		for(int i=0; i<noOfgroups; i++){
			/**Do a round robin for the smaller value*/
			for(int k=1,l=1;; k++,l++){

				constraintString += "ASSERT ("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((i*tuples1)+k+offset1-1))+ operator + 
						GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((i*tuples2)+l+offset2-1))+");\n";

				if(tuples1>tuples2){
					if(l==tuples2 && k<tuples1)	l=0;
					if(k>=tuples1) break;
				}
				else if(tuples1<tuples2){
					if(l<tuples2 && k==tuples1)	k=0;
					if(l>=tuples2) break;				
				}
				else{/** if tuples1==tuples2 */
					if(l==tuples1) break;
				}
			}
		}
		return constraintString;
	}

	public static String getTableName(Node n1){
		if(n1.getColumn() != null )
			return n1.getColumn().getTableName();
		else if (n1.getLeft().getColumn() != null)
			return n1.getLeft().getColumn().getTableName();
		else
			return n1.getLeft().getColumn().getTableName();
	}

	public static String getTableNameNo(Node n1){
		if(n1.getTableNameNo() != null )
			return n1.getTableNameNo();
		else if (n1.getLeft().getTableNameNo() != null)
			return n1.getLeft().getTableNameNo();
		else
			return n1.getLeft().getTableNameNo();
	}


	public static Column getColumn(Node n1){
		if(n1.getColumn() != null )
			return n1.getColumn();
		else if (n1.getLeft().getColumn() != null)
			return n1.getLeft().getColumn();
		else
			return n1.getLeft().getColumn();
	}
	/**
	 * Gets  constraints for nodes which are involved in join conditions which are in same query block
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 */
	public static String getConstraintsForJoinsInSameQueryBlock1(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2, String operator) {

		String constraintString = "";

		/** get the details of each node */
		String t1 = n1.getColumn().getTableName();
		String t2 = n2.getColumn().getTableName();
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());

		String r1 = n1.getTableNameNo();
		String r2 = n2.getTableNameNo();
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1 = 0, tuples2=0;
		if(cvc.getNoOfTuples().containsKey(r1)){

			tuples1 = cvc.getNoOfTuples().get(r1)*UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);
		}

		if(cvc.getNoOfTuples().containsKey(r2)){

			tuples2 = cvc.getNoOfTuples().get(r2)*UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);
		}

		int noOfgroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		for(int i=0; i<noOfgroups; i++){
			/**Do a round robin for the smaller value*/
			for(int k=1,l=1;; k++,l++){

				constraintString += "("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((i*tuples1)+k+offset1-1))+ operator + 
						GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((i*tuples2)+l+offset2-1))+") AND ";

				if(tuples1>tuples2){
					if(l==tuples2 && k<tuples1)	l=0;
					if(k>=tuples1) break;
				}
				else if(tuples1<tuples2){
					if(l<tuples2 && k==tuples1)	k=0;
					if(l>=tuples2) break;				
				}
				else{/** if tuples1==tuples2 */
					if(l==tuples1) break;
				}
			}
		}
		return constraintString;
	}

	/**
	 * Gets constraints for nodes which are involved in join conditions where each node is in different from clause sub queries
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param operator
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForJoinsInDiffSubQueryBlocks(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2, String operator) throws Exception{
		String constraintString = "";

		int leftGroup = 1, rightGroup = 1;

		/**get number of groups for each node */
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		rightGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);

		/**Get the details of each node */
		String t1 = getTableName(n1);
		String t2 = getTableName(n2);
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());

		String r1 = getTableNameNo(n1);
		String r2 = getTableNameNo(n2);
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];


		/** Get number of tuples of each relation occurrence */
		int tuples1=0, tuples2=0;
		if(cvc.getNoOfTuples().containsKey(r1)){
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if(cvc.getNoOfTuples().containsKey(r2)){
			tuples2 = cvc.getNoOfTuples().get(r2);
		}

		/**Do a round robin for the smaller value of the group number*/
		for(int k=1,l=1;; k++,l++){
			constraintString += "ASSERT ("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
					GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((l-1)*tuples2+offset2))+");\n";
			
			if(leftGroup>rightGroup){
				if(l==rightGroup && k<leftGroup)	l=0;
				if(k>=leftGroup) break;
			}
			else if(leftGroup<rightGroup){
				if(l<rightGroup && k==leftGroup)	k=0;
				if(l>=rightGroup) break;				
			}
			else{/**if tuples1==tuples2*/
				if(l==leftGroup) break;
			}
		}

		return constraintString;
	}


	/**
	 * Gets constraints for nodes which are involved in join conditions where each node is in different from clause sub queries
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param operator
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForJoinsInDiffSubQueryBlocks1(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2, String operator) throws Exception{
		String constraintString = "";

		int leftGroup = 1, rightGroup = 1;

		/**get number of groups for each node */
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		rightGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);

		/**Get the details of each node */
		String t1 = n1.getColumn().getTableName();
		String t2 = n2.getColumn().getTableName();
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());

		String r1 = n1.getTableNameNo();
		String r2 = n2.getTableNameNo();
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];


		/** Get number of tuples of each relation occurrence */
		int tuples1=0, tuples2=0;
		if(cvc.getNoOfTuples().containsKey(r1)){
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if(cvc.getNoOfTuples().containsKey(r2)){
			tuples2 = cvc.getNoOfTuples().get(r2);
		}
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();
		/**Do a round robin for the smaller value of the group number*/
		for(int k=1,l=1;; k++,l++){
			//Populate constraint Object list and call AND function
			ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(cvc,queryBlock, n1, ((k-1)*tuples1+offset1)));
			constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(cvc,queryBlock, n2, ((l-1)*tuples2+offset2)));
			constrObj.setOperator(operator);
			constrObjList.add(constrObj);
			
			constraintString += "("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
					GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((l-1)*tuples2+offset2))+") AND ";
			if(leftGroup>rightGroup){
				if(l==rightGroup && k<leftGroup)	l=0;
				if(k>=leftGroup) break;
			}
			else if(leftGroup<rightGroup){
				if(l<rightGroup && k==leftGroup)	k=0;
				if(l>=rightGroup) break;				
			}
			else{/**if tuples1==tuples2*/
				if(l==leftGroup) break;
			}
		}
		constraintString =constrGen.generateAssertANDConstraints(cvc, constrObjList);
		return constraintString;
	}

	/**
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param nulled
	 * @param P0
	 * @return
	 * @throws Exception
	 */
	/**FIXME: What if there are multiple groups in this query block*/
	public static String genNegativeConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node nulled, Node P0) throws Exception{
		String constraintString = new String();
		if(cvc.isFne()){
			String tableName=nulled.getTable().getTableName();
			constraintString += "ASSERT NOT EXISTS (i: O_"+tableName+"_INDEX_INT): " +
					"(O_"+ GenerateCVCConstraintForNode.cvcMap(nulled.getColumn(), "i") + 
					" = O_"+ GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0)+");";			
		}
		else{
			/**
			 * Open up FORALL and NOT EXISTS
			 */
			/**Get table names*/
			String nulledTableNameNo = nulled.getTableNameNo();
			String tablenameno = P0.getTableNameNo();

			int count1 = -1, count2 = -1;

			/**Get the number of tuples for the both nodes */
			count1 = UtilsRelatedToNode.getNoOfTuplesForThisNode(cvc, queryBlock, nulled);
			count2 = UtilsRelatedToNode.getNoOfTuplesForThisNode(cvc, queryBlock, P0);

			/**Get next position for these tuples*/
			int offset1= cvc.getRepeatedRelNextTuplePos().get(nulledTableNameNo)[1];			
			int offset2= cvc.getRepeatedRelNextTuplePos().get(tablenameno)[1];

			constraintString += "ASSERT ";

			for(int i=1;i<=count1;i++){
				for(int j=1;j<=count2;j++){
					String left ="", right = "";
					if(nulled.getQueryType() == 1 && queryBlock.getFromClauseSubQueries()!= null && queryBlock.getFromClauseSubQueries().size() != 0)
						left = "(O_"+ GenerateCVCConstraintForNode.cvcMap(nulled.getColumn(), (i-1)*cvc.getNoOfTuples().get(nulled.getTableNameNo())+offset1+"") ;
					else
						left = "(O_"+ GenerateCVCConstraintForNode.cvcMap(nulled.getColumn(), i+offset1-1+"") ;

					if(P0.getQueryType() == 1 && queryBlock.getFromClauseSubQueries()!= null && queryBlock.getFromClauseSubQueries().size() != 0)
						right = "O_"+ GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), (j-1)*cvc.getNoOfTuples().get(P0.getTableNameNo())+offset2+"") ;
					else
						right = "O_"+ GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), j+offset2-1+"") ;

					constraintString += left +" /= "+ right+") AND ";
				}
			}

			constraintString = constraintString.substring(0, constraintString.length()-4);
			constraintString += ";";
		}
		return constraintString;
	}

	/**
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param nulled
	 * @param P0
	 * @return
	 */
	/**FIXME: What if there are multiple groups in this query block*/
	public static String genNegativeConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Column nulled, Node P0){
		String constraintString = new String();

		if(cvc.isFne()){
			constraintString += "ASSERT NOT EXISTS (i: O_"+nulled.getTableName()+"_INDEX_INT): " +
					"(O_"+ GenerateCVCConstraintForNode.cvcMap(nulled, "i") + " = O_" + GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0) + ");";			
		}
		else{

			/** Open up FORALL and NOT EXISTS*/

			constraintString += "ASSERT ";
			for(int i = 1; i <= cvc.getNoOfOutputTuples().get(nulled.getTableName()) ; i++){/**FIXME: Handle repeated relations*/
				constraintString += "(O_" + GenerateCVCConstraintForNode.cvcMap(nulled, i + "") + " /= O_" + GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0) + ") AND "; 
			}
			constraintString = constraintString.substring(0, constraintString.length()-4);
			constraintString += ";";
		}
		return constraintString;
	}
	
	public static String genNegativeCondsEqClass(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2, int tuple){
		String constraintString = new String();
		
		for(int i = 1; i <= cvc.getNoOfOutputTuples().get(c1.getTable().getTableName()) ; i++){
			constraintString += "(O_" + GenerateCVCConstraintForNode.cvcMap(c1.getColumn(), i + "") + " /= O_" + GenerateCVCConstraintForNode.cvcMap(c2.getColumn(), tuple + "") + ") AND "; 
		}
		constraintString = constraintString.substring(0, constraintString.length()-4);		
		return constraintString.trim();
	}
	
	public static String genNegativeCondsEqClassForTuplePair(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2, int tupleIndex1, int tupleIndex2){
		String constraintString = new String();
		
		constraintString += "ASSERT ";			
		constraintString += "(O_" + GenerateCVCConstraintForNode.cvcMap(c1.getColumn(), tupleIndex1 + "") + " /= O_" + GenerateCVCConstraintForNode.cvcMap(c2.getColumn(), tupleIndex2 + "") + ") AND ";
			
		constraintString = constraintString.substring(0, constraintString.length()-4);
		constraintString += ";";
		
		return constraintString;
	}
	
	public static String genNegativeCondsEqClassForAllTuplePairs(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2, int tupleIndex1, int tupleIndex2){
		String constraintString = new String();
		
		for(int i = 1; i <= tupleIndex1 ; i++){
				for(int j = 1; j <= tupleIndex2; j++){			
					constraintString += "(O_" + GenerateCVCConstraintForNode.cvcMap(c1.getColumn(), i + "") + " /= O_" + GenerateCVCConstraintForNode.cvcMap(c2.getColumn(), j + "") + ") AND ";
			}
		}
		constraintString = constraintString.substring(0, constraintString.length()-4);		
		return constraintString.trim();
	}
	
	/**
	 * Generates positive constraints for the given set of nodes
	 * @param ec
	 */
	public static String genPositiveConds(Vector<Node> ec){

		String constraintString = "";

		for(int i=0; i<ec.size()-1; i++)
		{
			Column col1 = ec.get(i).getColumn();
			Column col2 = ec.get(i+1).getColumn();

			constraintString += GenerateCVCConstraintForNode.getCvc3StatementPositive(col1, ec.get(i), col2, ec.get(i+1));
		}
		return constraintString;
	}

}
