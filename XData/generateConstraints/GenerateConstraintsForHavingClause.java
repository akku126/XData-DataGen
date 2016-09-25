package generateConstraints;

import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.AggregateFunction;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * Contains methods to generates constraints for the having clause of the query block
 * @author mahesh
 *
 */
public class GenerateConstraintsForHavingClause {

	private static Logger logger = Logger.getLogger(GenerateConstraintsForHavingClause.class.getName());
	public static String getHavingClauseConstraints(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node havingClause, int totalRows, int groupNumber) throws Exception{
		
		/**If there is no having clause*/
		if(havingClause == null || havingClause.getType() == null){
			return "";
		}
		else{
			String returnStr = "";
			returnStr = getCVCForHavingConstraintRepeated(cvc, queryBlock, havingClause,totalRows,"",groupNumber);
			if(returnStr.equalsIgnoreCase(""))
				return "";
			else
				return returnStr += ";";
		}
	}
	
	
	
	/**FIXME: Write good doc for this function*/
	/**
	 * Generate Having Clause constraints for a given group number
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @param totalRows
	 * @param paramId: This is not used in this function, then why did this was there??
	 * @param groupNumber
	 * @return
	 * @throws Exception
	 */
	public static String getCVCForHavingConstraintRepeated(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n, int totalRows, String paramId, int groupNumber) throws Exception {

		if(n.getType().equalsIgnoreCase(Node.getBroNodeType())){
			String returnStr = "";
			if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				
				if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
					returnStr = "MAX: TYPE = SUBTYPE(LAMBDA(x: INT): " + " x " + n.getOperator() + n.getRight().toCVCString(10, queryBlock.getParamMap());
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){ 
						returnStr += " AND x > 0 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x < 10000000 );";
					}
					else{//operator is = or /=
						returnStr += ");";
					}
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getLeft(), totalRows, paramId, groupNumber);
				}
				else if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
					returnStr = "MIN: TYPE = SUBTYPE(LAMBDA(x: INT): " + " x " + n.getOperator() + n.getRight().toCVCString(10, queryBlock.getParamMap());
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){ 
						returnStr += " AND x > 0 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x < 10000000 );";
					}
					else{//operator is = or /=
						returnStr += ");";
					}
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock,  n.getLeft(), totalRows, paramId, groupNumber);
				}
				else if(n.getLeft().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())){
					return "";
				}
				else 
					return getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getLeft(), totalRows, paramId, groupNumber) +
							n.getOperator() + 
							getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(), totalRows, paramId, groupNumber);				
			}
			else if(n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
				if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
					returnStr = "MAX: TYPE = SUBTYPE(LAMBDA(x: INT): " + n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " " + n.getOperator() + " x ";
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){
						returnStr += " AND x < 10000000 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x > 0 );";
					}
					else{//operator is = or /=
						returnStr += ";";
					}
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(), totalRows, paramId, groupNumber);
				}
				else if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
					returnStr = "MIN: TYPE = SUBTYPE(LAMBDA(x: INT): " + n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " " + n.getOperator() + " x ";
					if(n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")){
						returnStr += " AND x < 10000000 );";
					}
					else if(n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")){
						returnStr += " AND x > 0 );";
					}
					else{//operator is = or /=
						returnStr += ");";
					}
					return returnStr + getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(), totalRows, paramId, groupNumber);
				}
				else if(n.getRight().getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggCOUNT())){
					return "";
				}
				else 
					return getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getLeft(), totalRows, paramId, groupNumber) +
							n.getOperator() + 
							getCVCForHavingConstraintRepeated(cvc, queryBlock, n.getRight(),totalRows, paramId, groupNumber);				
			}
			else{
				logger.log(Level.INFO,"Not an Aggregation!!");
				return "";
			}
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			String constVal = n.getStrConst();
			if( queryBlock.getParamMap().get(constVal) == null )
				return n.getStrConst();
			else return queryBlock.getParamMap().get(constVal);
		}
		else if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){
			//Column aggColumn = n.getAgg().getAggCol();
			AggregateFunction af = n.getAgg();
			if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggAVG())){
				String returnStr = "\nASSERT (";
				//Actual count required my this table
				//Required to adapt when aggregation column has x tuples but total tuples in output are y >= x
				//int myCount = af.getNoOfOutputTuples(noOfOutputTuples);
				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				
				}
				int myCount = cvc.getNoOfTuples().get(innerTableNo);
				int multiples = totalRows/myCount;
				int extras = totalRows%myCount;

				int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
				boolean isDistinct=af.isDistinct();

				for(int i=1,j=0;i<=myCount;i++,j++){

					int tuplePos=(groupNumber)*myCount+i;

					if(j<extras)
						returnStr += (multiples+1)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";
					else
						returnStr += (multiples)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";

					if(i<myCount){
						returnStr += "+";
					}
				}
				return returnStr + ") / "+totalRows + " ";
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggSUM())){
				String returnStr = "\nASSERT ";
				//Actual count required my this table
				
				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				
				}
				int myCount = cvc.getNoOfTuples().get(innerTableNo);
				int multiples = totalRows/myCount;
				int extras = totalRows%myCount;

				int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
				boolean isDistinct=af.isDistinct();
				

				for(int i=1,j=0;i<=myCount;i++,j++){
					int tuplePos=(groupNumber)*myCount+i;

					if(j<extras)
						returnStr += (multiples+1)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";
					else
						returnStr += (multiples)+"*("+ GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"")+")";

					if(i<myCount){
						returnStr += "+";
					}
				}
				return returnStr;
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMAX())){
				String returnStr = "";
				returnStr += "\nASSERT EXISTS(i: MAX): (";

				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				
				}
				int myCount = cvc.getNoOfTuples().get(innerTableNo);
				int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];

				for(int i=1;i<=totalRows;i++){
					int tuplePos=(groupNumber)*myCount+i;
					//Add the expression value to the other side of eqn - ie., to i value so that condition is satisfied
					//If Aggregate has an expression
					returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"") + " <= " +"i ";//+GenerateCVCConstraintForNode.cvcMapNode(getValueForExpressionsInAgg(af),tuplePos+offset-1+"");//+"i ";
					if(i<totalRows){
						returnStr += " AND ";
					}
				}
				returnStr += ") AND (";
				for(int i=1;i<=totalRows;i++){
					int tuplePos=(groupNumber)*myCount+i;
					returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"") + " = " +"i ";//+GenerateCVCConstraintForNode.cvcMapNode(getValueForExpressionsInAgg(af),tuplePos+offset-1+"");//"i ";
					if(i<totalRows){
						returnStr += " OR ";
					}
				}
				returnStr += ") ";
				return returnStr;
			}
			else if(n.getAgg().getFunc().equalsIgnoreCase(AggregateFunction.getAggMIN())){
				String returnStr = "";
				returnStr += "\nASSERT EXISTS(i: MIN): (";

				String innerTableNo=af.getAggExp().getTableNameNo();
				if(innerTableNo == null){
					innerTableNo = getTableNameNoForBAONode(af.getAggExp());
				
				}
				int myCount = cvc.getNoOfTuples().get(innerTableNo);
				int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];

				for(int i=1;i<=totalRows;i++){
					int tuplePos=(groupNumber)*myCount+i;
					returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"") + " >= " + "i ";
					if(i<totalRows){
						returnStr += " AND ";
					}
				}
				returnStr += ") AND (";
				for(int i=1;i<=totalRows;i++){
					int tuplePos=(groupNumber)*myCount+i;
					returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos+offset-1+"") + " = " + "i ";
					if(i<totalRows){
						returnStr += " OR ";
					}
				}
				returnStr += ") ";
				return returnStr;
			}
			else return ""; //TODO: Code for COUNT
		}		
		else return ""; //TODO: Code for Binaty Arithmetic Operator. This will be required in case of complex (arbitrary) having clauses.
	}
	
	
	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return
	 */
	public static String getTableNameNoForBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getTableNameNo() != null){
			return n1.getRight().getTableNameNo();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTableNameNo() != null){
			return n1.getLeft().getTableNameNo();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableNameNoForBAONode(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableNameNoForBAONode(n1.getRight());
			}
		}
		return null;
		
	}
	
	public static Node getValueForExpressionsInAgg(AggregateFunction af){
		//String iVal = "i";
		
		Node newAgg = af.getAggExp();
		
		if(af.getAggExp().getType().equalsIgnoreCase(Node.getBaoNodeType())){
			newAgg = replaceIValue(af.getAggExp());
		}
		return newAgg;
		
	}
	
	public static Node replaceIValue(Node n1){
		Node nd = new Node(n1);
		
		
		if(nd.getRight() != null && nd.getRight().getTableNameNo() != null){
			
			nd.getRight().setTableNameNo("");
			nd.getRight().getColumn().setColumnName("i");
			nd.getRight().setType("i");
			return nd;
		}	
		else if(nd.getLeft() != null && nd.getLeft().getTableNameNo() != null){
			nd.getLeft().setTableNameNo("");
			nd.getLeft().setTableNameNo("");
			nd.getLeft().getColumn().setColumnName("i");
			nd.getLeft().setType("i");
			return nd;
		}
		else {
			if(nd.getLeft() != null){
				nd.setLeft(replaceIValue(nd.getLeft()));
				return nd;
			}else if(nd.getRight() != null){
				nd.setRight(replaceIValue(nd.getRight()));
				return nd;
			}
		}
		return nd;
	}
	

}
