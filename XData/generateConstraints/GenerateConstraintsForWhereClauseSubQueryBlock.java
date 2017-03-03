package generateConstraints;

import java.util.Vector;

import parsing.AggregateFunction;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * This class contains methods to generate constraints for the where clause subquery blocks
 * The constraints include 
 *  1>> The constraints for the connective used between outer query block and where clause subquery block
 *  2>> The constraints for the conjuncts involved inside where clause nested subquery block (Whether to generate negative/positive constraints depends on the type of connective)
 *  3>> The constraints for the group by nodes and constrained aggregation inside where clause nested subquery block
 * @author mahesh
 *
 */

public class GenerateConstraintsForWhereClauseSubQueryBlock {

	/**
	 * The method that does the actual genartion of the constraints. It considers all the conditions inside where clause subquery block
	 * This method also adds conditions inside where clause subqueries including group by and aggregation constraints 
	 * @param cvc
	 * @param queryBlock
	 * @param conjunct
	 * @return
	 */
	public static String getConstraintsForWhereClauseSubQueryBlock(	GenerateCVC1 cvc, QueryBlockDetails queryBlock,	ConjunctQueryStructure conjunct) throws Exception{

		String constraintString = "";
		if(conjunct == null) return "";
		if(conjunct.getAllSubQueryConds() != null){
			for(int i=0; i < conjunct.getAllSubQueryConds().size(); i++){

				Node subQ = conjunct.getAllSubQueryConds().get(i);
				constraintString +="\n%---------------------------------------------------\n%CONSTRAINTS FOR WHERE CLAUSE SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";
				constraintString += getConstraintsForWhereSubQueryConnective(cvc, queryBlock, subQ);

				constraintString += "\n%---------------------------------------------------\n%CONSTRAINTS FOR CONDITIONS INSIDE WHERE CLAUSE SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";

				constraintString += getCVCForCondsInSubQ(cvc, queryBlock, subQ);

				constraintString += getConstraintsForGroupByAndHavingInSubQ(cvc, queryBlock, subQ);

				constraintString += "\n%---------------------------------------------------\n%END OF CONSTRAINTS FOR CONDITIONS INSIDE WHERE CLAUSE SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";
			}
		}

		return constraintString;
	}


	/**
	 * Getting constraints for the group by attributes and having clause constraints for this where clause subquery
	 * @param cvc
	 * @param queryBlock
	 * @param subQ
	 * @return
	 */
	public static String getConstraintsForGroupByAndHavingInSubQ(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node subQ) throws Exception{



		/**get index of the where clause sub query*/
		int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);

		/** Get the query block of this subquery node*/
		if( queryBlock.getWhereClauseSubQueries() != null &&  !queryBlock.getWhereClauseSubQueries().isEmpty()){
		QueryBlockDetails subQuery = queryBlock.getWhereClauseSubQueries().get(index);
		
		
		return getGroupByAndHavingConstraintsForWhereClauseSubqueryBlock(cvc, subQuery);
		}
		return null;
	}


	public static String getGroupByAndHavingConstraintsForWhereClauseSubqueryBlock(GenerateCVC1 cvc, QueryBlockDetails subQuery) throws Exception {

		String constraintString = "";

		/** Get number of groups of this where clause subquery block */
		int noOfGroups = subQuery.getNoOfGroups();

		/**get group by constraints */
		constraintString += "\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS\n%---------------------------------\n";
		constraintString += GenerateGroupByConstraints.getGroupByConstraints(cvc, subQuery.getGroupByNodes(), true, noOfGroups);
		constraintString += "\n%---------------------------------\n%END OF GROUP BY CLAUSE CONSTRAINTS\n%---------------------------------\n";



		/** Generate havingClause constraints */
		constraintString += "\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n";
		for(int j=0; j< noOfGroups;j ++)
			for(int k=0; k < subQuery.getAggConstraints().size();k++){
				constraintString += GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, subQuery, subQuery.getAggConstraints().get(k), subQuery.getFinalCount(), j);
			}
		constraintString += "\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n";

		return constraintString;
	}


	/**
	 * Given a Where clause SubQuery node, generates constraints for conditions inside that subQuery
	 * @param cvc
	 * @param queryBlock
	 * @param subQ
	 * @return
	 */
	/**FIXME: What about from clause sub queries inside where clause nested sub query	 (If we consider . For now not doing for more than one nesting level)
	 * FIXME: Write good documentation for this method
	 */
	public static String getCVCForCondsInSubQ(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node subQ) throws Exception{

		String constraintString="";

		/**get the index of this where clause subquery */
		int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);

		/** Used to store conditions of this subquery block*/
		Vector<Node> condsInSubQ = new Vector<Node>();

		/** Get the query block of this subquery node*/
		QueryBlockDetails subQuery = null;
		if(queryBlock.getWhereClauseSubQueries() != null && ! queryBlock.getWhereClauseSubQueries() .isEmpty()){
			subQuery= queryBlock.getWhereClauseSubQueries().get(index);
			
		}/*else if(queryBlock.getFromClauseSubQueries() != null && ! queryBlock.getFromClauseSubQueries() .isEmpty()
				&& queryBlock.getFromClauseSubQueries().get(index).getWhereClauseSubQueries() != null){
			
			index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);
			subQuery =  queryBlock.getWhereClauseSubQueries().get(index);
		}*/

		if(subQuery != null){
		/**Get the conditions of the subquery*/
		/**FIXME: What should be done if inside is ORing of conditions*/
		for(ConjunctQueryStructure con: subQuery.getConjunctsQs()){
			condsInSubQ.addAll(con.getStringSelectionConds());
			condsInSubQ.addAll(con.getSelectionConds());
			/** add equi joins*/
			for(Vector<Node> ecn: con.getEquivalenceClasses()){
				Node n1 = ecn.get(0);
				for(int l = 1; l<ecn.size(); l++){
					Node jn = new Node();
					jn.setLeft(n1);
					jn.setRight(ecn.get(l));
					jn.setOperator("=");
					//jn.setQueryIndex();
					condsInSubQ.add(jn);
				}
			}
			//condsInSubQ.addAll(con.getJoinConds());
			condsInSubQ.addAll(con.getAllConds());
		}
		
		return generateConstraintsForConditionsInWhereSubquery(cvc, subQ, condsInSubQ, subQuery);
		}
		return null;

	}


	/**
	 * used to get constraints for given set of where clause sub query conditions
	 * @param cvc
	 * @param subQ
	 * @param condsInSubQ
	 * @param subQuery
	 * @return
	 * @throws Exception
	 */
	public static String generateConstraintsForConditionsInWhereSubquery(GenerateCVC1 cvc, Node subQ, 	Vector<Node> condsInSubQ, QueryBlockDetails subQuery)
			throws Exception {

		String constraintString = "";

		/**Depending on the type of connective generate the constraints */
		if(subQ.getType().equals(Node.getNotExistsNodeType())){

			for (ConjunctQueryStructure conjuct: subQuery.getConjunctsQs()){
				constraintString += GenerateConstraintsForConjunct.generateNegativeConstraintsConjunct(cvc, subQuery, conjuct);
			}


			return constraintString;
		}

		else if(subQ.getType().equals(Node.getBroNodeSubQType())){	

			constraintString = getConstraintsForConditionsInSubquery(cvc, condsInSubQ, subQuery);

		}
		else {

			for(int i=0;i<condsInSubQ.size();i++){
				Node n = condsInSubQ.get(i);
				if(n.getLeft().getType().equals(Node.getColRefType()) && n.getRight().getType().equals(Node.getColRefType()) && n.getOperator().equals("="))
					constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, subQuery,n.getLeft(), n.getRight());	/** If it is equi join condition */

				else if(n.getLeft().getType().equalsIgnoreCase(Node.getColRefType()) && n.getRight().getType().equalsIgnoreCase(Node.getColRefType())    
						&& !n.getOperator().equalsIgnoreCase("")) /**if non equi join constraint*/

					constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, subQuery, n.getLeft(), n.getRight(), n.getOperator());

				else{
					String tableNameNo = n.getLeft().getTableNameNo();
					int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];

					/**The total number of tuples across all groups of this subquery*/
					int num = cvc.getNoOfTuples().get(tableNameNo) * subQuery.getNoOfGroups();
					for(int j=0;j<num;j++){
						if( UtilsRelatedToNode.isStringSelection(n,0)){/** If it is a string selection condition*/

							String subQueryConstraints = GenerateCVCConstraintForNode.genPositiveCondsForPred(subQuery, n, j+offset);
							String result = cvc.getStringSolver().solveConstraints(subQueryConstraints,cvc.getResultsetColumns(), cvc.getTableMap()).get(0);
							constraintString += result;							
						}
						else
							constraintString += "ASSERT "+ GenerateCVCConstraintForNode.genPositiveCondsForPred(subQuery, n, offset+j)+";\n";
					}
				}
			}
		}

		return constraintString;
	}


	public static String getConstraintsForConditionsInSubquery(GenerateCVC1 cvc, Vector<Node> condsInSubQ,	QueryBlockDetails subQuery)
			throws Exception {
		String constraintString = "";
		for(int i=0;i<condsInSubQ.size();i++){
			Node subQcond = condsInSubQ.get(i);

			Node left = subQcond.getLeft();
			Node right = subQcond.getRight();
			if(left.getType().equalsIgnoreCase(Node.getColRefType()) && right.getType().equalsIgnoreCase(Node.getColRefType())    
					&& subQcond.getOperator().equalsIgnoreCase("="))				/** If it is equi join condition */
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, subQuery, left, right);

			else if(left.getType().equalsIgnoreCase(Node.getColRefType()) && right.getType().equalsIgnoreCase(Node.getColRefType())    
					&& !subQcond.getOperator().equalsIgnoreCase("")) /**if non equi join constraint*/

				constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, subQuery, left, right, subQcond.getOperator());

			else {
				String tableNameNo = left.getTableNameNo();
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				if( UtilsRelatedToNode.isStringSelection(subQcond,0)){/** If it is a string selection condition*/

					/**Generate the constraints for each tuple across all the groups*/
					for(int j=0; j < cvc.getNoOfTuples().get(tableNameNo) * subQuery.getNoOfGroups(); j++){

						String subQueryConstraints = GenerateCVCConstraintForNode.genPositiveCondsForPred(subQuery, subQcond, j+offset);

						String result = cvc.getStringSolver().solveConstraints(subQueryConstraints,cvc.getResultsetColumns(), cvc.getTableMap()).get(0);
						constraintString += result;
					}
				}
				else /**Generate the constraints for each tuple across all the groups*/
					for(int j=0; j< cvc.getNoOfTuples().get(tableNameNo) * subQuery.getNoOfGroups(); j++)						
						constraintString += "ASSERT("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(subQuery, subQcond, j+offset)+");\n";
			}
		}
		return constraintString;
	}

	/**
	 * used to get constraints for where clause sub query conditions except the non equi join conditions in where clause sub query block
	 * @param cvc
	 * @param queryBlock
	 * @param subQ
	 * @return
	 * @throws Exception
	 */
	public static String getCVCForCondsInSubQExceptNonEquiJoinConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node subQ) throws Exception{


		/**get the index of this where clause sub query */
		int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);

		/** Used to store conditions of this sub query block*/
		Vector<Node> condsInSubQ = new Vector<Node>();
		if(queryBlock.getWhereClauseSubQueries() != null &&
				!queryBlock.getWhereClauseSubQueries().isEmpty()){
		/** Get the query block of this sub query node*/
		QueryBlockDetails subQuery = queryBlock.getWhereClauseSubQueries().get(index);

		/**Get the conditions of the sub query except no equi joins*/
		/**FIXME: What should be done if inside is ORing of conditions*/
		for(ConjunctQueryStructure con: subQuery.getConjunctsQs()){
			condsInSubQ.addAll(con.getStringSelectionConds());
			condsInSubQ.addAll(con.getSelectionConds());
			condsInSubQ.addAll(con.getJoinCondsAllOther());		
			condsInSubQ.addAll(con.getJoinCondsForEquivalenceClasses());
		}

		return generateConstraintsForConditionsInWhereSubquery(cvc, subQ, condsInSubQ, subQuery);
		}
		return null;
	}

	/**
	 * Given a sub query condition it returns the constraints for the connective used for this sub query block
	 * @param cvc
	 * @param queryBlock
	 * @param subQ
	 * @return
	 * @throws Exception
	 */

	public static String getConstraintsForWhereSubQueryConnective(GenerateCVC1 cvc, QueryBlockDetails queryBlock,	Node subQ) throws Exception{


		String constraintString = "";

		if(subQ.getType().equals(Node.getExistsNodeType()) || subQ.getType().equals(Node.getNotExistsNodeType()))
			return constraintString;
		
		int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);
		
		if(subQ.getLhsRhs() != null && subQ.getLhsRhs().getType().equals(Node.getAggrNodeType())){
			String op = subQ.getOperator();
			Node agg = null;
			String outerTableNo = "";
			if(op.contains("<>")) op = "/=";


			Node n = subQ.clone();
			n.setType(Node.getBroNodeType()); 

			if(n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				outerTableNo = n.getLeft().getTableNameNo();
				if(outerTableNo == null){
					outerTableNo = UtilsRelatedToNode.getTableNameNo(n.getRight().getAgg().getAggExp());	
				}
				n.setRight(n.getLhsRhs());
			}

			else if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				outerTableNo = n.getRight().getTableNameNo();				
				if(outerTableNo == null){
					outerTableNo = UtilsRelatedToNode.getTableNameNo(n.getLeft().getAgg().getAggExp());	
				}
				n.setLeft(n.getLhsRhs());
			}	
			
			/** Get the details of the query block*/
			int offset = cvc.getRepeatedRelNextTuplePos().get(outerTableNo)[1];
			if(queryBlock.getWhereClauseSubQueries() != null
					&& !queryBlock.getWhereClauseSubQueries().isEmpty()){
			/**get sub query block*/
			QueryBlockDetails subQuery = queryBlock.getWhereClauseSubQueries().get(index);
			int finalCount = subQuery.getFinalCount();

			/** do this for multiple number of tuples in case the outer has more than one tuple */
			for(int i=0; i < cvc.getNoOfTuples().get(outerTableNo);i++)

				/**If there are multiple groups inside the where clause sub query, then this must be equal to aggregation value in each group*/
				for(int j=0 ; j < subQuery.getNoOfGroups(); j++)
					constraintString += getCVCForAggInSubQConstraint(cvc, subQuery, n, finalCount, i+offset, j);/** Get the actual constraint for this node*/

			}
		}
		else{
			if(subQ.getLhsRhs() !=null ){
					Node left = subQ.getLhsRhs().getLeft();
					Node right = subQ.getLhsRhs().getRight();
					String op = subQ.getLhsRhs().getOperator();
					if(op != null && op.contains("<>")) op="/=";
		
					if(left != null && left.getTableNameNo() != null){
						String outerTableNo=left.getTableNameNo();
						int offset1 = cvc.getRepeatedRelNextTuplePos().get(outerTableNo)[1];
			
						if(right.getNodeType() != Node.getAllNodeType()
								&& right.getNodeType() != Node.getAnyNodeType()){
							
								String innerTableNo=right.getTableNameNo();
								int offset2 = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
								if(queryBlock.getWhereClauseSubQueries() != null
										&& !queryBlock.getWhereClauseSubQueries().isEmpty()){
								/**get sub query block*/
								QueryBlockDetails subQuery = queryBlock.getWhereClauseSubQueries().get(index);
								int finalCount = subQuery.getFinalCount();
					
								for(int i=0; i < cvc.getNoOfTuples().get(outerTableNo);i++)
					
									/**If there are multiple groups inside the where clause sub query, then this must be equal to aggregation value in each group*/
									for(int j=0 ; j < subQuery.getNoOfGroups(); j++)
										constraintString += getCVCForAggInSubQConstraint(cvc, subQuery, subQ.getLhsRhs(), finalCount, i+offset1, j);
								}
						}
					}
			}
		}

		constraintString += negateCondForDiffGroupSubQ(cvc, queryBlock, subQ);

		return constraintString;

	}


	/**
	 * In case we need to ensure different groups in subQ and outer tuples such as in cases where the table 
	 * within the subquery  and outside are the same and the subQ is >all or >max 
	 * i.e.To ensure that tuple generated for outer query do not affect tuples inside subquery
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param subQ
	 * @return
	 */
	public static String negateCondForDiffGroupSubQ(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node subQ) throws Exception{


		String returnString="ASSERT NOT (";
		int offset=0, count=0;
		String tableNameNumber = null;
		if(subQ.getType().equals(Node.getExistsNodeType()) || subQ.getType().equals(Node.getNotExistsNodeType()) )
			return "";
		/** Get the details about tuples of table this subquery node*/
		if(subQ.getType().equals(Node.getBroNodeSubQType())){
			if(subQ.getLeft() != null 
					&& subQ.getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNameNumber = UtilsRelatedToNode.getTableNameNo(subQ.getLeft());
			}else{
				tableNameNumber = subQ.getLeft().getTableNameNo();
			}
			offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
			count = cvc.getNoOfTuples().get(tableNameNumber);
		}
		else{
			
			if(subQ.getLhsRhs()!= null && subQ.getLhsRhs().getLeft() != null
					&& subQ.getLhsRhs().getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())
					){
				tableNameNumber = UtilsRelatedToNode.getTableNameNo(subQ.getLhsRhs().getLeft());
			}else{
				tableNameNumber = subQ.getLhsRhs().getLeft().getTableNameNo();
			}
			
		//	offset = cvc.getRepeatedRelNextTuplePos().get(subQ.getLhsRhs().getLeft().getTableNameNo())[1];
			//count = cvc.getNoOfTuples().get(subQ.getLhsRhs().getLeft().getTableNameNo());
		}

		offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
		count = cvc.getNoOfTuples().get(tableNameNumber);
		
		/** Get the index of this subquery node*/
		int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);

		for(int i=0;i<count;i++){
			Vector<Node> subQConds= new Vector<Node>();


			/**Get the selction conditions of the subquery*/
			/**FIXME: What should be done if inside is ORing of conditions*/
			for(ConjunctQueryStructure conjunct: queryBlock.getWhereClauseSubQueries().get(index).getConjunctsQs()){
				subQConds.addAll(conjunct.getSelectionConds());
				subQConds.addAll(conjunct.getStringSelectionConds());
			}

			for(Node cond:subQConds){

				/**if this table is occurred in outer table*/
				String tableName = null;
				if(cond.getLeft().getTable() != null)
					tableName = cond.getLeft().getTable().getTableName();
				else if ( cond.getRight().getTable()!=null )
					tableName = cond.getRight().getTable().getTableName();

				boolean present = false;
				for(String tableNameNo: queryBlock.getBaseRelations()){

					/**if present */
					if(tableNameNo.startsWith(tableName)  ){

						String position =  tableNameNo.substring(tableName.length());
						/** if the remaining string is integer*/
						try { 
							Integer.parseInt(position); 
							present = true;
							break;

						} catch(NumberFormatException e) { 
							continue;
						}						
					}

				}
				if( present ){
					String subQueryConstraints ="ASSERT NOT ";
					if( UtilsRelatedToNode.isStringSelection(cond,0)){

						subQueryConstraints += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, cond, i+offset);

						String result = cvc.getStringSolver().solveConstraints(subQueryConstraints,cvc.getResultsetColumns(), cvc.getTableMap()).get(0);
						returnString +=  result.substring(7,result.length()-2)+ " AND ";		
					}
					else
						returnString += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, cond, i+offset)+" AND ";
				}
			}
			if(returnString.endsWith("ASSERT NOT ("))
				returnString+="(1 /=1) AND ";
		}
		returnString=returnString.substring(0, returnString.length()-4)+");\n";

		return returnString;

	}

	/**
	 * Generate CVC constraints for aggregation conditions in subqueries
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @param totalRows
	 * @param outerTupleNo
	 * @return
	 */
	/**FIXME: Write good documentation for this function*/

	public static String getCVCForAggInSubQConstraint(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n, int totalRows, int outerTupleNo, int groupNo){ /** String returned by this function must prepend 'ASSERT' and append ';' to it. */
		if(n.getType().equalsIgnoreCase(Node.getBroNodeType())){			

			String returnStr = "";


			if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				AggregateFunction af = n.getLeft().getAgg();
				String tableNameNo = af.getAggExp().getTableNameNo();
				if(tableNameNo == null){
					tableNameNo = UtilsRelatedToNode.getTableNameNo(af.getAggExp());
				}
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				int myCount = cvc.getNoOfTuples().get(tableNameNo);

				int groupOffset = groupNo * myCount;

				String columnName=null;
				if(af.getAggExp().getColumn() != null){
					columnName = af.getAggExp().getColumn().getColumnName();
				}
				if(columnName == null){
					columnName = UtilsRelatedToNode.getColumnName(af.getAggExp()); 
				}

				if(af.getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){

					String maxStr="MAX_"+columnName+ ": "+columnName+";\nASSERT(";
					for(int i=1;i<=myCount;i++){
						maxStr+="("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+"=MAX_"+columnName+ ") OR";
						returnStr+= "ASSERT ("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+"<=MAX_"+columnName+ ");\n";
					}
					maxStr=maxStr.substring(0, maxStr.length()-3)+");\n";


					returnStr+= "ASSERT (MAX_"+columnName+n.getOperator()+"+O_"+ GenerateCVCConstraintForNode.cvcMap(n.getRight().getColumn(), outerTupleNo+"")+");\n";
					return maxStr+returnStr;
				}
				else if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
					String minStr="MIN_"+columnName+ ": "+columnName+";\nASSERT(";
					for(int i=1;i<=myCount;i++){
						minStr+="("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+"=MIN_"+columnName+ ") OR";
						returnStr += "ASSERT ("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+">=MIN_"+columnName+ ");\n";
					}
					minStr=minStr.substring(0, minStr.length()-3)+");\n";					
					returnStr+= "ASSERT (MIN_"+columnName+n.getOperator()+"+O_"+ GenerateCVCConstraintForNode.cvcMap(n.getRight().getColumn(), outerTupleNo+"")+");\n";
					return minStr+returnStr;
				}
				else if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())){
					return "";
				}

			}
			else if(n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){

				/**get aggregate function*/
				AggregateFunction af = n.getRight().getAgg();

				/**get table details*/
				String tableNameNo = af.getAggExp().getTableNameNo();
				if(tableNameNo == null){
					tableNameNo = UtilsRelatedToNode.getTableNameNo(af.getAggExp());
				}
				String columnName = null;
				if(af.getAggExp().getColumn() != null){
					columnName = af.getAggExp().getColumn().getColumnName();
				}
				if(columnName == null){
					columnName = UtilsRelatedToNode.getColumnName(af.getAggExp());
				}

				/**get tuple details*/
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				int myCount = cvc.getNoOfTuples().get(tableNameNo);

				int groupOffset = groupNo * myCount;

				if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
					String maxStr="MAX_"+columnName+ ": "+columnName+";\nASSERT(";
					for(int i=0;i<=myCount-1;i++){
						maxStr+="("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i + offset + groupOffset)+"")+"=MAX_"+columnName+ ") OR";
						returnStr+= "ASSERT ("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i + offset + groupOffset)+"")+"<=MAX_"+columnName+ ");\n";
					}
					maxStr=maxStr.substring(0, maxStr.length()-3)+");\n";
					Column col = null;
					if(n.getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())){
						col = UtilsRelatedToNode.getColumn(n.getLeft());
					}else{
						col = n.getLeft().getColumn();
					}
					returnStr+="ASSERT ("+ "O_"+ GenerateCVCConstraintForNode.cvcMap(col, outerTupleNo+"")+n.getOperator()+"MAX_"+columnName+");\n";

					return maxStr+returnStr;
				}
				else if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
					String minStr="MIN_"+columnName+ ": "+columnName+";\nASSERT(";
					for(int i=1;i<=myCount;i++){
						minStr+="("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i + offset + groupOffset)+"")+"=MIN_"+columnName+ ") OR";
						returnStr+= "ASSERT ("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i + offset + groupOffset)+"")+">=MIN_"+columnName+ ");\n";
					}
					minStr=minStr.substring(0, minStr.length()-3)+");\n";					
					returnStr+="ASSERT ("+ "O_"+ GenerateCVCConstraintForNode.cvcMap(n.getLeft().getColumn(), outerTupleNo+"")+n.getOperator()+"MIN_"+columnName+");\n";
					return minStr+returnStr;

				}
				else if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())){
					return "";
				}

			}

			String left = getCVCForAggInSubQConstraint(cvc, queryBlock, n.getLeft(), totalRows, outerTupleNo, groupNo);
			String right = getCVCForAggInSubQConstraint(cvc, queryBlock, n.getRight(), totalRows, outerTupleNo, groupNo);
			String returnValue="ASSERT (";
			if( right.contains(" OR ") ){
				String split[]=right.split(" OR ");
				for(int i=0;i<split.length;i++)
					returnValue +="("+left+""+n.getOperator()+split[i]+" OR ( ";
				returnValue=returnValue.substring(0, returnValue.lastIndexOf("OR")-1)+";\n";
				return returnValue;
			}
			else
				returnValue +=left+ n.getOperator() + right+");\n";
			return returnValue;
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			String constVal = n.getStrConst();
			if(queryBlock.getParamMap().get(constVal) == null)
				return n.getStrConst();
			else return queryBlock.getParamMap().get(constVal);
		}

		else if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			String innerTableNo = n.getTableNameNo();
			int offset2 = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
			int groupOffset = groupNo * cvc.getNoOfTuples().get(innerTableNo);
			return "O_"+ GenerateCVCConstraintForNode.cvcMap(n.getColumn(), outerTupleNo+offset2-1+"");
		}
		////////////////////////////////

		else if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			//Column aggColumn = n.getAgg().getAggCol();
			AggregateFunction af = n.getAgg();
			if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggAVG())){
				String returnStr = " (";
				//Actual count required my this table
				//Required to adapt when aggregation column has x tuples but total tuples in output are y >= x
				String tableNameNo = n.getAgg().getAggExp().getTableNameNo();
				int myCount = cvc.getNoOfTuples().get(tableNameNo);
				int multiples = totalRows/myCount;
				int extras = totalRows%myCount;
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				int groupOffset = groupNo * myCount;
				for(int i=0,j=0;i<myCount;i++,j++){
					if(j<extras)
						returnStr += (multiples+1)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+")";
					else
						returnStr += (multiples)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+")";

					if(i<myCount-1){
						returnStr += "+";
					}
				}
				return returnStr + ") / "+totalRows + " ";
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggSUM())){
				String returnStr = " ";
				//Actual count required my this table
				String tableNameNo = n.getAgg().getAggExp().getTableNameNo();
				int myCount = cvc.getNoOfTuples().get(tableNameNo);
				boolean isDistinct = af.isDistinct();
				int multiples = totalRows/myCount;
				int extras = totalRows%myCount;
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				int groupOffset = groupNo * myCount;
				if(isDistinct && myCount>1){//mahesh: add
					//there will be three elements in the group
					int ind=0;
					for(int m=0;m<2;m++){
						returnStr +="(";
						for(int i=0,j=0;i<myCount-1;i++,j++){
							if(j<extras)
								returnStr += (multiples+1)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+ind+groupOffset)+"")+")";
							else
								returnStr += (multiples)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+ind+groupOffset)+"")+")";//" DISTINCT (O_"+ cvcMap(col,group+offset-1+"") +", O_"+ cvcMap(col,(group+aliasCount-1+offset)+"") +") "
							if(i<myCount-2){
								returnStr += "+";
							}
						}
						//add contsraint for distinct
						returnStr+=" )) AND ";
						for(int i=0,j=0;i<myCount-2;i++,j++)
						{
							returnStr +="DISTINCT( "+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+ind+groupOffset)+"")+" , "+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+ind+1+groupOffset)+"")+") AND ";
						}
						returnStr = returnStr.substring(0,returnStr.lastIndexOf("AND")-1);
						ind++;
						returnStr +=") OR ";
					}
					if(returnStr.contains("OR"))
						returnStr = returnStr.substring(0,returnStr.lastIndexOf("OR")-1);
					return returnStr;
				}
				for(int i=0,j=0;i<myCount;i++,j++){
					if(j<extras)
						returnStr += (multiples+1)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+")";
					else
						returnStr += (multiples)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (i+offset+groupOffset)+"")+")";

					if(i<myCount-1){
						returnStr += "+";
					}
				}
				return returnStr;
			}


			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
				String tableNameNo = n.getAgg().getAggExp().getTableNameNo();
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				int groupOffset = groupNo * cvc.getNoOfTuples().get(tableNameNo);
				String returnStr = "";
				returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (1+offset+groupOffset)+"");
				return returnStr;
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
				String tableNameNo=n.getAgg().getAggExp().getTableNameNo();
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				int groupOffset = groupNo * cvc.getNoOfTuples().get(tableNameNo);
				String returnStr = "";
				returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), (1+offset+groupOffset)+"");
				return returnStr;
			}
			else return ""; //TODO: Code for COUNT
		}		
		else return ""; //TODO: Code for Binaty Arithmetic Operator. This will be required in case of complex (arbitrary) having clauses.
	}


	/**
	 * Generates constraints to kill all Vs Any mutations
	 * @param cvc
	 * @param subQ
	 * @return
	 * @throws Exception
	 */
	/**FIXME: What if there are more than one tuple in outer query block
	 * @param qbt TODO*/
	public static String subQueryConstraintsToKillAllAny(GenerateCVC1 cvc, QueryBlockDetails qbt, Node subQ) throws Exception{

		String constraintString = "";

		/**If connective is not of type exists*/
		if(!subQ.getType().equalsIgnoreCase(Node.getExistsNodeType())){

			/**get sub query connective details*/
			Node lr = subQ.getLhsRhs();
			Node lhs = lr.getLeft();
			Node rhs = lr.getRight();

			/**Get operator type*/
			String op = "";
			if(lr.getOperator().equalsIgnoreCase("<>"))		
				op = "/=";

			else

				op = lr.getOperator();

			int offset1 = cvc.getRepeatedRelNextTuplePos().get(lhs.getTableNameNo())[1];
			int offset2 = cvc.getRepeatedRelNextTuplePos().get(rhs.getTableNameNo())[1];


			constraintString += "\n%---------------------------------------------------\n%CONSTRAINTS TO KILL ALL/ANY SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";

			constraintString += "\nASSERT (" + GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, lhs,offset1) + " " 
					+ op +" " + GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, rhs,offset2) + ");";/**FIXME: Is qbt correct or do we need to pass sub query block*/

			constraintString += "\nASSERT NOT (" + GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, lhs,offset1) + " "
					+ op + " " + GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, rhs,offset2+1) + ");";

			constraintString += "\n%---------------------------------------------------\n%END OF CONSTRAINTS TO KILL ALL/ANY SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";


			constraintString += "\n%---------------------------------------------------\n%CONSTRAINTS FOR CONDITIONS INSIDE WHERE CLAUSE SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";

			constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getCVCForCondsInSubQ(cvc, qbt, subQ);

			constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForGroupByAndHavingInSubQ(cvc, qbt, subQ);

			constraintString += "\n%---------------------------------------------------\n%END OF CONSTRAINTS FOR CONDITIONS INSIDE WHERE CLAUSE SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";

		}
		return constraintString;
	}


	/**
	 * This function is used to generate constraints for the conditions inside the where clause sub query, in which we are killing the mutations
	 * @param cvc
	 * @param qbt
	 * @param con
	 * @param subqueryConjunct
	 * @param subQCond
	 * @param except
	 * @throws Exception
	 */
	public static void generateConstraintsForKillingMutationsInWhereSubqueryBlock( GenerateCVC1 cvc, QueryBlockDetails qbt, ConjunctQueryStructure con, ConjunctQueryStructure subqueryConjunct, Node subQCond, int except) throws Exception{

		/** Also Generates positive constraints for all the conditions of this sub query block conjunct*/
		/** And we need to add the positive conditions for all other where clause sub query blocks in this conjunct*/
		String constraintString = "";

		/**for each where clause sub query in this conjunct*/
		for(Node subQConds: con.getAllSubQueryConds()){


			constraintString +="\n%---------------------------------------------------\n%CONSTRAINTS FOR WHERE CLAUSE SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";
			constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereSubQueryConnective( cvc, cvc.getOuterBlock(), subQConds);
			constraintString += "\n%---------------------------------------------------\n%CONSTRAINTS FOR CONDITIONS INSIDE WHERE CLAUSE SUBQUERY CONNECTIVE \n%---------------------------------------------------\n\n";

			/** Used to store conditions of this sub query block*/
			Vector<Node> condsInSubQ = new Vector<Node>();

			/**get the list of conditions in this where clause sub query */
			condsInSubQ.addAll(subqueryConjunct.getStringSelectionConds());
			condsInSubQ.addAll(subqueryConjunct.getSelectionConds());
			condsInSubQ.addAll(subqueryConjunct.getLikeConds());

			/** add equi joins*/
			for(Vector<Node> ecn: subqueryConjunct.getEquivalenceClasses()){
				Node n1 = ecn.get(0);
				for(int l = 1; l<ecn.size(); l++){
					Node jn = new Node();
					jn.setLeft(n1);
					jn.setRight(ecn.get(l));
					jn.setOperator("=");
					condsInSubQ.add(jn);
				}
			}
			/**add all non equi join conditions, iff we are not killing non equi join mutations inside this where clause connective*/
			if( except == 1 && subQCond.equals(subQConds) == false)
				condsInSubQ.addAll(con.getAllConds());

			constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForConditionsInSubquery(cvc, condsInSubQ, qbt);
			
			constraintString +=  GenerateConstraintsForWhereClauseSubQueryBlock.getGroupByAndHavingConstraintsForWhereClauseSubqueryBlock(cvc, qbt);
		}
		
		cvc.getConstraints().add(constraintString);
	}

}
