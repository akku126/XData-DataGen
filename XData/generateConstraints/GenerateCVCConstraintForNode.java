package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.z3.*;

import parsing.AggregateFunction;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import testDataGen.DataType;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;

/**
 * This class contain actual methods that map a given node into CVC3 constraint
 * @author mahesh
 *
 */
public class GenerateCVCConstraintForNode {

	private static Logger logger = Logger.getLogger(GenerateCVCConstraintForNode.class.getName());
	
	/**
	 * Generate CVC3 constraints for the given node and its tuple position
	 * @param queryBlock
	 * @param n
	 * @param index
	 * @return
	 */
	public static String genPositiveCondsForPred( QueryBlockDetails queryBlock, Node n, int index){
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return "O_"+cvcMap(n.getColumn(), index+"");
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			if(!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) || n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			return "("+ genPositiveCondsForPred( queryBlock, n.getLeft(), index) +" "+ n.getOperator() +" "+ 
					genPositiveCondsForPred( queryBlock, n.getRight(), index) +")";
		}
		return null;
	}

	/**
	 * Generate CVC3 constraints for the given node and its tuple position
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @param index
	 * @return
	 */
	public static String genPositiveCondsForPredAgg(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n, int index){
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			if(index <= cvc.getNoOfOutputTuples(n.getColumn().getTableName())){/**FIXME: Handle repeated relations */
				return cvcMap(n.getColumn(), index+"");
			}
			else return cvcMap(n.getColumn(), "1");
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			if(!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) || 
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			return "("+genPositiveCondsForPred( queryBlock, n.getLeft()) + n.getOperator() + genPositiveCondsForPred( queryBlock, n.getRight())+")";
		}
		return "";
	}

	/**
	 * Generate CVC3 constraints for the given node
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static String genPositiveCondsForPred(QueryBlockDetails queryBlock, Node n){
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return "O_"+cvcMap(n.getColumn(), n);
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){

			if(!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) ||n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			return "("+ genPositiveCondsForPred( queryBlock, n.getLeft()) + " "+n.getOperator() + " "+
					genPositiveCondsForPred( queryBlock, n.getRight())+")";
		}
		return null;
	}



	/**
	 * Used to get negative CVC3 constraints for the predicate
	 * @param cvc
	 * @param queryBlock
	 * @param pred
	 * @param nulledAliasName
	 * @param nulledTableName
	 * @return
	 */
	/** FIXME: This function is generating constraints of form ASSERT NOT EXISTS (i: O_SECTION_INDEX_INT): ((O_SECTION[1].0>O_TAKES[1].1));
	 * These are causing problem. Example query19*/
	/**FIXME: Also the repeated relations are not correctly handled in the below method */
	public static String genNegativeCondsForPredAgg(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node pred, String nulledAliasName, String nulledTableName){

		String constraintString = new String();
		int index = UtilsRelatedToNode.getMaxCountForPredAgg(cvc, pred);

		constraintString = //"ASSERT NOT EXISTS (i: O_" + nulledTableName + "_INDEX_INT): (" + 
				genPositiveCondsForPredAgg(cvc, queryBlock, pred, index);// + ");";
		return constraintString;
	}

	/**
	 * Used to get CVC3 constraint for this column for the given tuple position
	 * @param col
	 * @param index
	 * @return
	 */
	public static String cvcMap(Column col, String index){
		Table table = col.getTable();
		String tableName = col.getTableName();
		String columnName = col.getColumnName();
		int pos = table.getColumnIndex(columnName);
		return tableName+"["+index+"]."+pos;	
	}

	/**
	 * Used to get CVC3 constraint for this column
	 * @param col
	 * @param n
	 * @return
	 */
	public static String cvcMap(Column col, Node n){
		Table table = col.getTable();
		String tableName = col.getTableName();
		String aliasName = col.getAliasName();
		String columnName = col.getColumnName();
		String tableNo = n.getTableNameNo();
		int index = Integer.parseInt(tableNo.substring(tableNo.length()-1));
		int pos = table.getColumnIndex(columnName);
		return tableName+"["+index+"]."+pos;	
	}


	/**
	 * Used to get CVC3 constraint for the given node for the given tuple position
	 * @param n
	 * @param index
	 * @return
	 */
	public static String cvcMapNode(Node n, String index){
		if(n.getType().equalsIgnoreCase(Node.getValType())){
			return n.getStrConst();
		}
		else if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return "O_"+cvcMap(n.getColumn(), index);
		}
		else if(n.getType().toString().equalsIgnoreCase("i")){
			return "i";
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			return "("+cvcMapNode(n.getLeft(), index) + n.getOperator() + cvcMapNode(n.getRight(), index)+")";
		}
		else return "";
	}

	/**
	 * Returns the cvc statement for assignment of a NULL value to a particular Tuple
	 * Accordingly also sets whether the null value for that column has been used or not 
	 * This is done in the HashMap colNullValuesMap
	 * @param cvc
	 * @param c
	 * @param index
	 * @return
	 */
	public static String cvcSetNull(GenerateCVC1 cvc, Column c, String index){
		HashMap<String, Integer> nullValues = cvc.getColNullValuesMap().get(c);
		Iterator<String> itr = nullValues.keySet().iterator();
		ConstraintGenerator consGen = new ConstraintGenerator();
		boolean foundNullVal = false;
		String nullVal = "";
		while(itr.hasNext()){
			nullVal = itr.next();
			if(nullValues.get(nullVal)==0){
				nullValues.put(nullVal, Integer.parseInt(index));
				foundNullVal = true;
				break;
			}
		}
		/** If found */
		if(foundNullVal){
			 return consGen.getAssertNullValue(c,index,nullVal);
			//return "\nASSERT O_"+cvcMap(c, index)+" = "+nullVal+";";
		}
		
		else{
			logger.log(Level.WARNING,"Unassigned Null value cannot be found due to insufficiency");		
			return "";
		}
	}

	/**
	 * Returns not null constraint in CVC for all the tuples which are not explicitly stated as NULLs
	 * Care must be taken to call this method at the end, after adding all the constraints, so that
	 * the necessary explicit NULL constraints are already added.
	 * @param cvc
	 * @returns
	 */

	public static String cvcSetNotNull(GenerateCVC1 cvc){
		
		String retVal = ConstraintGenerator.addCommentLine(" NOT NULL CONSTRAINTS\n\n");
		Iterator<Column> itrCol = cvc.getColNullValuesMap().keySet().iterator();
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		
		while(itrCol.hasNext()){
			Column col = itrCol.next();
			HashMap<String, Integer> nullValues = cvc.getColNullValuesMap().get(col);
			String tabName = col.getTableName();
			for(int j=1; j <= cvc.getNoOfOutputTuples(tabName); j++){
				/*if(!nullValues.values().contains(j))
					if(col != null && col.getCvcDatatype() != null && (col.getCvcDatatype().equalsIgnoreCase("INT") || col.getCvcDatatype().equalsIgnoreCase("REAL") || col.getCvcDatatype().equalsIgnoreCase("TIME")
							||col.getCvcDatatype().equalsIgnoreCase("TIMESTAMP") || col.getCvcDatatype().equalsIgnoreCase("DATE")))
						retVal += "\nASSERT NOT ISNULL_"+col.getColumnName()+"(O_"+cvcMap(col, j+"")+");";
					else
						retVal += "\nASSERT NOT ISNULL_"+col.getCvcDatatype()+"(O_"+cvcMap(col, j+"")+");";
						*/
				if(!nullValues.values().contains(j)) {
					retVal += constraintGenerator.getAndSetNotNullValuesBeforeFooter(col, j);
				}
			}


			//			//Added by Biplab for adding NOT NULL constraints to BRANCHQUERY
			//			for(int i = 0; i < noOfBranchQueries; i++)
			//				if(tablesChangedForbranchQuery[i].contains(col.getTable()))
			//				{
			//					if(col.getCvcDatatype().equals("INT") || col.getCvcDatatype().equals("REAL") || col.getCvcDatatype().equals("TIME")
			//							||col.getCvcDatatype().equals("TIMESTAMP") || col.getCvcDatatype().equals("DATE"))
			//						retVal += "\nASSERT NOT ISNULL_"+col.getColumnName()+"(O_"+cvcMap(col, (noOfOutputTuples.get(tabName) + 1)+"")+");";
			//					else
			//						retVal += "\nASSERT NOT ISNULL_"+col.getCvcDatatype()+"(O_"+cvcMap(col, (noOfOutputTuples.get(tabName) + 1)+"")+");";
			//				}
			//			//End of Added by Biplab for adding NOT NULL constraints to BRANCHQUERY


		}
		return retVal;
	}

	/**
	 * Returns not null constraint in CVC for all the tuples which are not explicitly stated as NULLs
	 * Care must be taken to call this method at the end, after adding all the constraints, so that
	 * the necessary explicit NULL constraints are already added.
	 * @param cvc
	 * @returns
	 */

	public static String solverSetNotNull(GenerateCVC1 cvc){
		
		String retVal = ConstraintGenerator.addCommentLine("NOT NULL CONSTRAINTS\n\n");
		Iterator<Column> itrCol = cvc.getColNullValuesMap().keySet().iterator();
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		
		while(itrCol.hasNext()){
			Column col = itrCol.next();
			HashMap<String, Integer> nullValues = cvc.getColNullValuesMap().get(col);
			String tabName = col.getTableName();
			for(int j=1; j <= cvc.getNoOfOutputTuples(tabName); j++){
				if(!nullValues.values().contains(j))
					
					retVal += constraintGenerator.getAndSetNotNullValuesBeforeFooter(col, j);
					
			}


			//			//Added by Biplab for adding NOT NULL constraints to BRANCHQUERY
			//			for(int i = 0; i < noOfBranchQueries; i++)
			//				if(tablesChangedForbranchQuery[i].contains(col.getTable()))
			//				{
			//					if(col.getCvcDatatype().equals("INT") || col.getCvcDatatype().equals("REAL") || col.getCvcDatatype().equals("TIME")
			//							||col.getCvcDatatype().equals("TIMESTAMP") || col.getCvcDatatype().equals("DATE"))
			//						retVal += "\nASSERT NOT ISNULL_"+col.getColumnName()+"(O_"+cvcMap(col, (noOfOutputTuples.get(tabName) + 1)+"")+");";
			//					else
			//						retVal += "\nASSERT NOT ISNULL_"+col.getCvcDatatype()+"(O_"+cvcMap(col, (noOfOutputTuples.get(tabName) + 1)+"")+");";
			//				}
			//			//End of Added by Biplab for adding NOT NULL constraints to BRANCHQUERY


		}
		return retVal;
	}
	
public static String primaryKeysSetNotNull(GenerateCVC1 cvc){
		
		String retVal = ConstraintGenerator.addCommentLine("NOT NULL CONSTRAINTS\n\n");

		try{
			/** For each table in the result tables */
			for(int i=0; i < cvc.getResultsetTables().size(); i++){

				/** Get this data base table */
				Table table = cvc.getResultsetTables().get(i);

				/**Get the primary keys of this table*/
				ArrayList<Column> primaryKey = new ArrayList<Column>( table.getPrimaryKey() );
				
				ArrayList<Column> primaryKeys = new ArrayList<Column>();
				
				for (Column element : primaryKey) { 
		            if (!primaryKeys.contains(element)) { 
		            	primaryKeys.add(element); 
		            } 
		        } 

				/**If there are no primary keys, then nothing need to be done */
				if( primaryKeys.size() <= 0) {
					continue;
				}

				int p = 0;
				String temp1 = "";

				
				temp1 = "\n(assert (forall ((ipk"+p+" Int)) \n";
				temp1 += " (and \n";

				for(String col : table.getColumns().keySet()){
					if( primaryKeys.toString().contains(col)){
						Column pkeyColumn = primaryKeys.get(p);
						String col_datatype = pkeyColumn.getCvcDatatype();
						IntExpr ipk0 = ConstraintGenerator.ctx.mkIntConst("ipk0");
						if(col_datatype!= null && (col_datatype.equalsIgnoreCase("INT") || col_datatype.equalsIgnoreCase("REAL") || col_datatype.equalsIgnoreCase("TIME") || col_datatype.equalsIgnoreCase("DATE") || col_datatype.equalsIgnoreCase("TIMESTAMP"))) {
							temp1 += "\t(not ( ISNULL_"+col+ConstraintGenerator.smtMap(pkeyColumn, ipk0).toString() + ") )\n";
						} else {
							temp1 += "\t(not ( ISNULL_"+pkeyColumn.getCvcDatatype()+ConstraintGenerator.smtMap(pkeyColumn, ipk0).toString() + ") )\n";
						}
						p++;
					}
				}
				
				temp1 +=" )\n))\n";
				retVal += temp1;

			}
		}catch(Exception e){
			logger.log(Level.SEVERE,"\n Exception in GenerateCVCconstraintforNode.java: Function primaryKeysSetNotNull : ",e);
			throw e;
		}
		
		retVal += ConstraintGenerator.addCommentLine("END OF NOT NULL CONSTRAINTS\n\n");

		return retVal;
	}
	
	
	/**
	 * DOC FOR THIS METHOD
	 * @param queryBlock
	 * @param vn
	 * @param c
	 * @param countVal
	 * @return
	 */
	@Deprecated
	public static String generateCVCForCNTForPositiveINTToDel(QueryBlockDetails queryBlock, ArrayList<Node> vn, Column c, int countVal){

		String CVCStr = "";
		int min = 0,min1=0, max=0,max1=0;
		/*CVCStr += "SUM: BITVECTOR(20);\nMIN: BITVECTOR(20);\nMAX: BITVECTOR(20);\nAVG: BITVECTOR(20);\nCOUNT: BITVECTOR(20);";
		CVCStr += "\nMIN1: BITVECTOR(20);\nMAX1: BITVECTOR(20);\n\n";*/
		
		CVCStr += "SUM: INT;\nMIN: INT;\nMAX: INT;\nAVG: REAL;\nCOUNT: INT;";
		CVCStr += "\nMIN1: INT;\nMAX1: INT;\n\n";
		
		/*if(countVal == 0){
			CVCStr += "ASSERT BVLE(COUNT," + Utilities.getHexVal(32,5) + ");\n";//30 because CNT is always CNT+2 = 32 (max in CVC)
		}
		else{
			CVCStr += "ASSERT COUNT = " + Utilities.getHexVal(countVal,5) + ";\n";
		}*/
		if(countVal == 0){
			CVCStr += "ASSERT (COUNT <  32);\n";//30 because CNT is always CNT+2 = 32 (max in CVC)
		}
		else{
			CVCStr += "ASSERT (COUNT = " + countVal + ");\n";
		}		
		
		/*
		CVCStr += "\n\nASSERT BVLE(MIN1,MIN);\nASSERT BVGE(MAX1,MAX);\n";
		CVCStr += "%ASSERT BVGE(COUNT,0hex0);\nASSERT BVLE(MIN,MAX);\n";
		CVCStr += "ASSERT BVGE(MAX1,AVG);\nASSERT BVGE(AVG,MIN1);\n";
		*/
		
		CVCStr += "\n\nASSERT (MIN1 <= MIN);\nASSERT (MAX1 >= MAX);\n";
		CVCStr += "ASSERT (COUNT > 0);\nASSERT (MIN <= MAX);\n";
		CVCStr += "ASSERT (MAX1 >= AVG);\nASSERT (AVG >= MIN1);\n";


		/*CVCStr += "ASSERT BVGE(SUM,BVMULT(20,MIN,COUNT)) " +
				"AND  BVLE(SUM,BVMULT(20,MAX,COUNT));\n";
		CVCStr += "ASSERT " +
				"(BVLT(BVMULT(20,AVG,BVSUB(20,COUNT,0hex00001)), SUM) " +
				"AND BVGT(BVMULT(20,AVG,BVPLUS(20,COUNT,0hex00001)), SUM)) \n" +
				"OR \n" +
				"(BVGT(BVMULT(20,AVG,BVSUB(20,COUNT,0hex00001)), SUM) " +
				"AND BVLT(BVMULT(20,AVG,BVPLUS(20,COUNT,0hex00001)), SUM));\n";
		*/
		
		CVCStr += "ASSERT (SUM  >= MIN * COUNT);\n " +
				" ASSERT (SUM <= MAX * COUNT);\n";
		CVCStr += "ASSERT (AVG * COUNT = SUM);\n";

		DataType dt = new DataType();
	
				if((dt.getDataType(c.getDataType())==1 || dt.getDataType(c.getDataType())==2) && c.getMinVal() != -1){
					Vector< Node > selectionConds = new Vector<Node>();
					for(ConjunctQueryStructure conjunct : queryBlock.getConjunctsQs())
						selectionConds.addAll(conjunct.getSelectionConds());
		
					/** if there is a selection condition on c that limits the min val of c */
					min=(UtilsRelatedToNode.getMaxMinForIntCol(c, selectionConds))[1];
					//CVCStr += "\nASSERT MIN1 = "+Utilities.getHexVal(min,5)+";";
					CVCStr += "\nASSERT (MIN1 = " + min +");\n";
				}
		
				if((dt.getDataType(c.getDataType())==1 || dt.getDataType(c.getDataType())==2) && c.getMaxVal() != -1){
		
					Vector< Node > selectionConds = new Vector<Node>();
					for(ConjunctQueryStructure conjunct : queryBlock.getConjunctsQs())
						selectionConds.addAll(conjunct.getSelectionConds());
		
					/**if there is a selection condition on c that limits the max val of c*/
					max=(UtilsRelatedToNode.getMaxMinForIntCol(c, selectionConds))[0];		
					//CVCStr += "\nASSERT MAX1 = "+Utilities.getHexVal(max,5)+";";
					CVCStr += "\nASSERT (MAX1 = " + max +");\n";
				}
				
				for(String s: queryBlock.getParamMap().values()){
					s = s.trim();
					if(s.contains("PARAM")){
						CVCStr += "\n (declare-const "+s+" (_ BitVec 32))";
					}
		}
		 
		for(Node n: vn){
			if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())
					|| n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())
							|| n.getRight().getNodeType().equalsIgnoreCase(Node.getAggrNodeType())){
				AggregateFunction agg = null;
				if(n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()))
					agg = n.getLeft().getAgg();
				else
					agg = n.getRight().getAgg();
				
				
				if(agg.getAggExp().getLeft() != null && agg.getAggExp().getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())){
			//	int constValue  = this.getMaxMinForHaving(n,min)[1];
				
					if((dt.getDataType(c.getDataType())==1 || dt.getDataType(c.getDataType())==2) && c.getMinVal() != -1)
					{
					 	min1 = min+((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[1]);			
						CVCStr = CVCStr.replace("\nASSERT (MIN1 = "+ min +");\n","\nASSERT (MIN1 = " + min1 +");\n");
					}
					if((dt.getDataType(c.getDataType())==1 || dt.getDataType(c.getDataType())==2) && c.getMaxVal() != -1)
					{
						max1 = max+ ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[0]);		
						
						CVCStr = CVCStr.replace("\nASSERT (MAX1 = " + max +");\n","\nASSERT (MAX1 = " + max1 +");\n");
					}
					
				}else if(agg.getAggExp().getRight() != null && agg.getAggExp().getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
					//int constValue  = this.getConstantVal(n);
					if((dt.getDataType(c.getDataType())==1 || dt.getDataType(c.getDataType())==2) && c.getMinVal() != -1)
					{
						min1 = min+((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[1]);			
						CVCStr = CVCStr.replace("\nASSERT (MIN1 = "+ min +");\n","\nASSERT (MIN1 = " + min1 +");\n");
					}
					if((dt.getDataType(c.getDataType())==1 || dt.getDataType(c.getDataType())==2) && c.getMaxVal() != -1)
					{
						
						max1 = max+ ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[0]);		
						CVCStr = CVCStr.replace("\nASSERT (MAX1 = " + max +");\n","\nASSERT (MAX1 = " + max1 +");\n");
					}
					
				}
			}
			CVCStr += "\nASSERT " + n.toCVCString(10, queryBlock.getParamMap()) + ";";
		}
		CVCStr += "\n\nQUERY FALSE;\nCOUNTEREXAMPLE;\nCOUNTERMODEL;";
		return CVCStr;
	}

	/**
	 * Generates positive CVC3 constraint for given nodes and columns
	 * @param col1
	 * @param n1
	 * @param col2
	 * @param n2
	 * @return
	 */
	/*public static String getCvc3StatementPositive(Column col1, Node n1, Column col2, Node n2){

		return "ASSERT O_"+cvcMap(col1, n1) +" = O_"+cvcMap(col2, n2)+";";
	}*/


	/**
	 * Generates negative constraints for the given string selection node
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	/*public static String genNegativeStringCond(GenerateCVC1 cvc,QueryBlockDetails queryBlock, Node n){
		logger.log(Level.INFO, "Node type: "+n.getType() + n.getLeft() + n.getOperator() + n.getRight());
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return "O_"+cvcMap(n.getColumn(), n);
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){

			if(!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) ||n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
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
			else if(n.getOperator().equalsIgnoreCase("~"))
				n.setOperator("!i~");
			return "("+ constraintGenerator.genPositiveCondsForPred(cvc,queryBlock, n.getLeft()) + " "+n.getOperator() + " "+
			constraintGenerator.genPositiveCondsForPred(cvc,queryBlock, n.getRight()) +")";
		}
		return null;
	}*/


	/*public static String genPositiveCondsForPred(QueryBlockDetails queryBlock, Node n, Map<String,Character> hm){
		Character index = null;
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			if(hm.containsKey(n.getTable().getTableName())){
				index=hm.get(n.getTable().getTableName());
			}
			else
			{
				Iterator it = hm.entrySet().iterator();
				index='i';
				while(it.hasNext()){
					Map.Entry pairs = (Map.Entry)it.next();
					char temp=(Character) pairs.getValue();
					if(temp>index)
						index=temp;
				}
				index++;
				hm.put(n.getTable().getTableName(),index);
			}
			return "O_"+cvcMap(n.getColumn(), index+"");
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			if(!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) || n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			return "("+ genPositiveCondsForPred( queryBlock, n.getLeft(), hm) +" "+ n.getOperator() +" "+ 
					genPositiveCondsForPred( queryBlock, n.getRight(), hm)+")";
		}
		return "";
	}*/

	/*public static String genPositiveCondsForPred(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n, int index, String paramId){//For parameters
		
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		return constraintGenerator.genPositiveCondsForPred(cvc, queryBlock, n, index);
		
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return "O_"+cvcMap(n.getColumn(), index+"");
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			if(!paramId.contentEquals("")){//If parameterized, then return the parameter identifier
				String constVal = n.getStrConst();
				return queryBlock.getParamMap().get(constVal);
			}
			else return n.getStrConst();
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) || 
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			return "("+ genPositiveCondsForPred( queryBlock, n.getLeft(), index, paramId) + n.getOperator() +
					genPositiveCondsForPred( queryBlock, n.getRight(), index, paramId)+")";
		}
		return null;
	}*/

	public static String genNegativeCondsForPred(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node node, int i) throws Exception{
		//need to change it for joins etc
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		Node n=node.clone();

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
		return constraintGenerator.genPositiveCondsForPred(queryBlock, n,i);
	}

	/**
	 * Used to get negative conditions for the given set of conditions
	 * @param stringSelectionConds
	 * @return
	 */
	public static Vector<Node> getNegativeConditions(Vector<Node> conditions) {
		
		Vector<Node> conditionsDup = new Vector<Node>();
		for(Node node: conditions)
			if(node.getType().equalsIgnoreCase(Node.getBroNodeType()) || node.getType().equalsIgnoreCase(Node.getBaoNodeType()) ||node.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
					node.getType().equalsIgnoreCase(Node.getAndNodeType()) || node.getType().equalsIgnoreCase(Node.getOrNodeType())){				

				conditionsDup.add( getNegativeCondition(node) );
			}

		return conditionsDup;
	}

	
	public static Node getNegativeCondition(Node node) {
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
		else if(n.getOperator().equalsIgnoreCase("~"))
			n.setOperator("!i~");
		
		return n;
	}
	
	public static String generateNegativeConditionsForNodesList(GenerateCVC1 cvc, QueryBlockDetails queryBlock, List<Node> selConds) throws Exception{
		
		String returnString = "";// "ASSERT(";
		
		int predCount = 0;
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList <ConstraintObject> conObjList = new ArrayList<ConstraintObject>();
		
		for(int k = 0; k < selConds.size(); k++){
			
			/**get table details*/
			String tableNo = selConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l = 1; l <= count; l++){
				ConstraintObject constrObj = new ConstraintObject();
				if(predCount == 0){
					returnString += GenerateCVCConstraintForNode.genNegativeCondsForPred(cvc,queryBlock, selConds.get(k),l+offset-1);
					constrObj.setLeftConstraint(returnString);
					conObjList.add(constrObj);
				}
				else{
					//returnString += " OR " + GenerateCVCConstraintForNode.genNegativeCondsForPred(cvc,queryBlock, selConds.get(k),l+offset-1);
					returnString =  GenerateCVCConstraintForNode.genNegativeCondsForPred(cvc,queryBlock, selConds.get(k),l+offset-1);
					constrObj.setLeftConstraint(returnString);
					conObjList.add(constrObj);
				}
				
				predCount++;
			}
		}
		returnString = constrGen.generateOrConstraintsWithAssert(conObjList);
		// returnString += "); \n";
		return returnString;
	}
	
	public static int getConstantVal(Node n, int min){
		int retVal=0;
		
		
		
		return retVal;
		
	}
}
