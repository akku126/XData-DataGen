package testDataGen;

import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateConstraintsForHavingClause;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Conjunct;
import parsing.Node;

/**
 * TODO: GOOD DOC
 * @author mahesh
 *
 */
/**FIXME: Whether these parametrs are for query block/ or entire query*/
public class RelatedToParameters {

	private static Logger logger = Logger.getLogger(RelatedToParameters.class.getName());
	public static boolean isParameterized(Node n){
		if(n.getLeft()==null || n.getLeft().getType()==null)
			return false;
		if(n.getLeft().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getLeft().getStrConst().contains("$")){
				return true;
			}
			return false;
		}
		else if(n.getRight()==null)
			return false;
		else if(n.getRight().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getRight().getStrConst().contains("$")){
				return true;
			}
			return false;			
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType())){
			return isParameterized(n.getLeft()) || isParameterized(n.getRight());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			return isParameterized(n.getLeft()) || isParameterized(n.getRight());
		}
		else{
			return false;
		}
	}

	public static ArrayList<String> replaceParamWithID(QueryBlockDetails queryBlock, Node n, String paramContent, String paramType){
		if(n.getLeft().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getLeft().getStrConst().contains(paramContent)){
				String param = "PARAM_"+paramType+"_"+ queryBlock.getParamCount();
				queryBlock.getParamMap().put(n.getLeft().getStrConst(), param);
				queryBlock.setParamCount(queryBlock.getParamCount() + 1) ;
				ArrayList<String> p= new ArrayList<String>();
				p.add(param);
				return p;
			}
		}
		else if(n.getRight().getType().equalsIgnoreCase(Node.getValType())){
			if(n.getRight().getStrConst().contains(paramContent)){
				String param = "PARAM_"+paramType+"_"+queryBlock.getParamCount();
				queryBlock.getParamMap().put(n.getRight().getStrConst(), param);
				queryBlock.setParamCount(queryBlock.getParamCount() + 1) ;
				ArrayList<String> p= new ArrayList<String>();
				p.add(param);
				return p;
			}
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			ArrayList<String> p1 = replaceParamWithID( queryBlock, n.getLeft(), paramContent, paramType);
			ArrayList<String> p2 = replaceParamWithID( queryBlock, n.getRight(), paramContent, paramType);
			p1.addAll(p2);
			return p1;
		}
		return null;
	}

	public static HashMap<String, Node> removeParameterizedConstraints( QueryBlockDetails queryBlock, ArrayList<Node> pConstraints, String paramType){
		HashMap<String, Node> retVal = new HashMap<String, Node>();
		for(int i=0;i<pConstraints.size();i++){
			Node n = pConstraints.get(i);
			if(isParameterized(n)){
				ArrayList<String> paramsInNode = replaceParamWithID( queryBlock, n, "$", paramType);
				queryBlock.getParamsNodeMap().put(paramsInNode, n);
				retVal.put(paramType+queryBlock.getpConstraintId(),n);
				queryBlock.setpConstraintId(queryBlock.getpConstraintId() + 1);
				//Don't remove these constraints. Let them be there. Just replace the $ value with the parameter name that we have supplied
				//pConstraints.removeElementAt(i);
			}
		}
		return retVal;
	}

	/**TODO: Add doc for this */
	public static String addDatatypeForParameters( GenerateCVC1 cvc, QueryBlockDetails queryBlock  ){
		String retVal = "";
		Iterator<ArrayList<String>> itr = queryBlock.getParamsNodeMap().keySet().iterator();
		int val = 0;
		boolean isMaxOrMin = false;
		while(itr.hasNext()){
			ArrayList<String> params = itr.next();
			Node n = queryBlock.getParamsNodeMap().get(params);
			//String datatype = getDatatypeFromNode(n);
			//modified by bikash to ensure that datatype is the same as column name to ensure that range constraints get satisfied
			//FIXME: What if left side is aggregation
			String datatype = null;

			if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()))//If the left side of node is aggregate function
			{
				if(n.getLeft().getAgg().getAggExp().getType().equalsIgnoreCase(Node.getBaoNodeType()))
					datatype = getTableNameNoForBAONode(n.getLeft().getAgg().getAggExp());
				else
					datatype = n.getLeft().getAgg().getAggExp().getColumn().getColumnName();
				 
			}
			else if (n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType()))//If the right side of node is aggregate function
			{
				if(n.getRight().getAgg().getAggExp().getType().equalsIgnoreCase(Node.getBaoNodeType()))
					datatype = getTableNameNoForBAONode(n.getRight().getAgg().getAggExp());
				else
					datatype = n.getRight().getAgg().getAggExp().getColumn().getColumnName();
					
			}
			else if(n.getLeft() != null && n.getLeft().getColumn()!=null)//if left side is not aggregate
			{
				datatype = n.getLeft().getColumn().getColumnName();
			}
			else if(n.getRight() != null && n.getRight().getColumn() != null)
			{
				datatype = n.getRight().getColumn().getColumnName();
			}
			//System.out.println("datatype: "+datatype);*/
			//String datatype = getDatatypeFromNode(n);
			if(n.getType().equalsIgnoreCase(Node.getBroNodeType())){
				if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()) ||
						n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){
					if(n.getAggFuncFromNode().getFunc().equalsIgnoreCase("MAX")){
						isMaxOrMin = true;
						Vector<Column> cols = n.getAggFuncFromNode().getAggExp().getColumnsFromNode();
						val = 0;
						for(int i=0;i<cols.size();i++){
							if(val < cols.get(i).getMaxVal())	val = (int)cols.get(i).getMaxVal();
						}
					}
					if(n.getAggFuncFromNode().getFunc().equalsIgnoreCase("MIN")){
						isMaxOrMin = true;
						Vector<Column> cols = n.getAggFuncFromNode().getAggExp().getColumnsFromNode();
						val = 1000000;
						for(int i=0;i<cols.size();i++){
							if(val > cols.get(i).getMinVal())	val = (int)cols.get(i).getMinVal();
						}
					}
				}
			}
			//Add the data type for all the params to CVC
			for(int i=0;i<params.size();i++){
				retVal += params.get(i) + " : " + datatype +";\n";
				if(isMaxOrMin)
					//retVal += "ASSERT "+params.get(i)+" = "+val+";\n";
					retVal += "ASSERT "+n+" = "+val+";\n";
			}
			isMaxOrMin = false;
		}

		return retVal;
	}

	/**
	 * Adds constraints to hash map "constraintsWithParameters" for parameterized aggregation or selection conditions
	 * @param queryBlock
	 */
	public static void setupDataStructuresForParamConstraints(QueryBlockDetails queryBlock) {


		if( queryBlock.getHavingClause() != null){
			queryBlock.getConstraintsWithParameters().putAll(removeParameterizedConstraints( queryBlock, queryBlock.getAggConstraints(), "AGG"));
			//These constraints have to be put into CVC with new variables for the (unsatisfiable) constants/parameters.
		}

		for(Conjunct con: queryBlock.getConjuncts()){
			
			if( con.getSelectionConds() != null && con.getSelectionConds().size() != 0)			//Selection conds are already flattened
				queryBlock.getConstraintsWithParameters().putAll(removeParameterizedConstraints(queryBlock, new ArrayList( con.getSelectionConds() ), "SEL"));


			if( con.getLikeConds() != null && con.getLikeConds().size()!=0)		//Like conds are already flattened
				queryBlock.getConstraintsWithParameters().putAll(removeParameterizedConstraints( queryBlock, new ArrayList(con.getLikeConds()), "LIKE"));
		}		
	}


	public static String getConstraintsForParameters(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{

		if( queryBlock.getConstraintsWithParameters() == null || queryBlock.getConstraintsWithParameters().size()==0){
			return "";
		}

		String retVal = "";
		Iterator itr = queryBlock.getConstraintsWithParameters().keySet().iterator();	

		while(itr.hasNext()){

			String key = (String) itr.next();
			Node n = queryBlock.getConstraintsWithParameters().get(key);

			if(key.contains("AGG"))

				retVal += GenerateConstraintsForHavingClause.getCVCForHavingConstraintRepeated( cvc, queryBlock, n, queryBlock.getFinalCount(), key, 0)+";\n";

			else if(key.contains("SEL"))

				for(int l=0; l < queryBlock.getFinalCount(); l++){
					retVal += "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred( queryBlock, n, l, key)+";\n";

				}
		}
		return retVal;
	}

	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return datatype of column in BAO node
	 */
	public static String getTableNameNoForBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getColumn() != null){
			return n1.getRight().getColumn().getColumnName();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getColumn() != null){
			return n1.getLeft().getColumn().getColumnName();	
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
	
}
