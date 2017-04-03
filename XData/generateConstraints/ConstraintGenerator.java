package generateConstraints;

import generateSMTConstraints.GenerateCVCConstraintForNodeSMT;

import java.util.ArrayList;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Node;
import parsing.Table;

import testDataGen.GenerateCVC1;
import util.ConstraintObject;

/**
 * This class generates the constraints based on solver defined in XData.Properties file. 
 * 
 * @author shree
 *
 */
public class ConstraintGenerator {
	private static Logger logger = Logger.getLogger(ConstraintGenerator.class.getName());
	
	/**
	 * This method takes in the col name, table name, offset and position for columns on which the constraint is to be generated.
	 * This also takes the operator that joins the constraints. Checks the ConstraintContext for CVC/SMT.
	 * If it is CVC, it returns CVC format constraint, otherwise return SMT format constraint.
	 * 
	 * @param cvc
	 * @param tableName1
	 * @param offset1
	 * @param pos1
	 * @param tableName2
	 * @param offset2
	 * @param pos2
	 * @param col1
	 * @param col2
	 * @param operator
	 * @return
	 */
	public ConstraintObject getConstraint(GenerateCVC1 cvc, String tableName1, Integer offset1, Integer pos1, String tableName2, Integer offset2, Integer pos2,
			Column col1, Column col2,String operator){
		
		ConstraintObject con = new ConstraintObject();
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			con.setLeftConstraint("O_"+tableName1+"[" + offset1 + "]."+pos1);
			con.setRightConstraint("O_"+tableName2+"["+ offset2 +"]."+pos2);
			con.setOperator(operator);
		}
		else{
										
			con.setLeftConstraint(col1.getColumnName()+pos1+" (select O_"+tableName1+" " + offset1 +")");
			con.setRightConstraint(col2.getColumnName()+pos2+" (select O_"+tableName2+" "+ offset2 +")");
			con.setOperator(operator.toLowerCase());
		}
		return con;
	}
	
	/**
	 * This method returns a String with ISNULL constraint
	 * 
	 * @param cvc
	 * @param col
	 * @param offSet
	 * @return
	 */
	public String getIsNullCondition(GenerateCVC1 cvc,Column col,String offSet){
		
		String isNullConstraint = "";
		if(col.getCvcDatatype().equals("INT")|| col.getCvcDatatype().equals("REAL") || col.getCvcDatatype().equals("DATE") 
				|| col.getCvcDatatype().equals("TIME") || col.getCvcDatatype().equals("TIMESTAMP"))
		{	
			if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
				isNullConstraint ="ISNULL_" + col.getColumnName() + "(O_" + cvcMap(col, offSet + "") + ")";
			}
			else{
				isNullConstraint ="(ISNULL_" + col.getColumnName() + " " + smtMap(col, offSet+ "") + ")";
			}
		}else{
			if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
				isNullConstraint = "ISNULL_" + col.getCvcDatatype() + "(O_" + cvcMap(col, offSet+ "") + ")";
			}else{
				isNullConstraint ="(ISNULL_" + col.getCvcDatatype() + " " + smtMap(col, offSet+ "") + ")";
			}
		}
		return isNullConstraint;
	}
	
	/**
	 * This method takes list of constraints of type String and returns AND'ed constraint String based on the solver used.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String getNullConditionConjuncts(GenerateCVC1 cvc,ArrayList<String> constraintList){
		String constraint = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			for(String con : constraintList){
				constraint += con +" AND ";
			}
			constraint = constraint.substring(0,constraint.length()-5);
		}else{
			String constr1 ="";
			for(String  con : constraintList){
				constr1 = getNullConditionForStrings(con,constr1);
				constraint = constr1;
			}
			
		}
		return constraint;
	}
	
	/**
	 * This method gets tow string constraints and returns a single OR'red constraint for foreign keys 
	 * 
	 * @param cvc
	 * @param fkConstraint
	 * @param nullConstraint
	 * @return
	 */
	public String getFKConstraint(GenerateCVC1 cvc, String fkConstraint, String nullConstraint){
		
		String fkConstraints = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			if(nullConstraint != null && !nullConstraint.isEmpty()){
				fkConstraints += "ASSERT (" + fkConstraint + ") OR (" + nullConstraint + ");\n";
			}else{
				fkConstraints += "ASSERT (" + fkConstraint + ");\n";
			}
		}else{
			if(nullConstraint != null && !nullConstraint.isEmpty()){
				fkConstraints = "(assert (or "+fkConstraint + " "+nullConstraint+"))\n";
			}else{
				fkConstraints = "(assert "+ fkConstraint +" ) \n";
			}
		}
		return fkConstraints;
	}
	/**
	 * This method will return SMT constraints of the form (or (StringValue) (ISNULL_COLNAME (colName tableNameNo_colName)) )
	 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
	 * 	
	 * @param con
	 * @param s1
	 * @return
	 */
	public String getNullConditionForStrings(String con, String s1){
		
		String cvcStr ="";
		
		if(s1 != null && !s1.isEmpty()){
			cvcStr += " (and ";
			cvcStr += s1;
		}
		
		if(con != null){
			cvcStr += con;
		}
		if(s1 != null && !s1.isEmpty()){
			cvcStr +=")  ";
		}
		return cvcStr;
	}

	
	/**
	 * This method takes in the ConstraintObject List as input and generates a constraint String AND + OR conditions.
	 * The returned string holds the AND + ORconstraints in SMT or CVC format. 
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateAndOrConstraints(GenerateCVC1 cvc, ArrayList<ConstraintObject> AndConstraintList,ArrayList<ConstraintObject> OrConstraintList){
		String constraint = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			constraint += "\nASSERT ";
			constraint += generateCVCAndConstraints(AndConstraintList);
			constraint = constraint.substring(0,constraint.length()-5);
			
			constraint += generateCVCOrConstraints(OrConstraintList);
			constraint = constraint.substring(0,constraint.length()-4);
			constraint +=";\n";
			
		}else{
			constraint += "\n (assert "; 
			constraint += generateSMTAndConstraints(AndConstraintList);
			constraint += generateSMTOrConstraints(OrConstraintList,constraint);
			constraint += " )";
		}
		
		return constraint;
	}
	
	/**
	 * This method takes in the ConstraintObject List as input and generates a constraint String OR + AND all conditions.
	 * The returned string holds the OR + AND constraints in SMT or CVC format. 
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateOrAndConstraints(GenerateCVC1 cvc, ArrayList<ConstraintObject> AndConstraintList,ArrayList<ConstraintObject> OrConstraintList){
		String constraint = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			constraint += "\nASSERT ";
			constraint += generateCVCOrConstraints(OrConstraintList);
			constraint = constraint.substring(0,constraint.length()-4);
			
			constraint += generateCVCAndConstraints(AndConstraintList);
			constraint = constraint.substring(0,constraint.length()-5);
	
			constraint +=";\n";
			
		}else{
			constraint += "\n (assert "; 
			constraint += generateSMTOrConstraints(OrConstraintList);
			constraint += generateSMTAndConstraints(AndConstraintList,constraint);
			constraint += " )";
		}
		
		return constraint;
	}
	
	
	/**
	 * This method takes in the ConstraintObject List as input and generates a constraint String AND'ing all conditions.
	 * The returned string holds the AND'ed constraints in SMT or CVC format. 
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateANDConstraints(GenerateCVC1 cvc, ArrayList<ConstraintObject> constraintList){
		String constraint = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			constraint = generateCVCAndConstraints(constraintList);
		}else{
			constraint = generateSMTAndConstraints(constraintList);
		}
		return constraint;
	}
	
	/**
	 * This method returns a constraint for null value in the query depending on solver.
	 * 
	 * @param cvc
	 * @param c
	 * @param index
	 * @param nullVal
	 * @return
	 */
	public String getAssertNullValue(GenerateCVC1 cvc,Column c,String index,String nullVal){
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			return "\nASSERT O_"+cvcMap(c, index)+" = "+nullVal+";";
		}else{
			return "\n (assert (= ("+smtMap(c, index)+") "+nullVal+"  ))";
		}
	}
	
	/**
	 * This method returns constraintString for primary keys with =>. It takes left and right constraint strings and adds => on them and returns new string. 
	 * 
	 * @param cvc
	 * @param impliedConObj
	 * @param isImplied
	 * @return
	 */
	public String getImpliedConstraints(GenerateCVC1 cvc, ConstraintObject impliedConObj, boolean isImplied){
		String constrString = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			if(isImplied){
				constrString = "ASSERT ("+impliedConObj.getRightConstraint() + " "+impliedConObj.getOperator()+" "+impliedConObj.getLeftConstraint()+");\n";
			}else{
				constrString = "ASSERT ("+impliedConObj.getRightConstraint() + ") "+impliedConObj.getOperator()+" TRUE; \n";
			}
		}else{
			if(isImplied){
				constrString = "\n (assert (=> "+impliedConObj.getRightConstraint()+" "+ impliedConObj.getLeftConstraint()+"))";
			}else{
				constrString = "\n (assert (=> "+impliedConObj.getRightConstraint()+" true))";
			}
		}
		return constrString;
	}
	/**
	 * This method takes in the ConstraintObject List as input and generates a constraint String OR'ing all conditions.
	 * The returned string holds the OR'ed constraints in SMT or CVC format.
	 *  
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateOrConstraints(GenerateCVC1 cvc, ArrayList<ConstraintObject> constraintList){
		String constraint = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			constraint = generateCVCOrConstraints(constraintList);
		}else{
			constraint = generateSMTOrConstraints(constraintList);
		}
		return constraint;
	}
	
	/**
	 * This method takes in the ConstraintObject List as input and generates a constraint String with NOT conditions.
	 * The returned string holds the NOT constraints in SMT or CVC format.
	 *  
	 * @param cvc
	 * @param constraintList
	 * @return
	
	public String generateNotConstraints(GenerateCVC1 cvc, ArrayList<ConstraintObject> constraintList){
		String constraint = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			constraint = generateCVCNotConstraints(constraintList);
		}else{
			constraint = generateSMTNotConstraints(cvc,constraintList);
		}
		return constraint;
	} */
	
	
/**
 * This method returns AND'ed constraints with Assert and ; as required for CVC solver. 
 * @param constraintList
 * @return
 */
public String generateCVCAndConstraints(ArrayList<ConstraintObject> constraintList){
	
	String constraint = "";
	
	for(ConstraintObject con : constraintList){
		constraint += "("+con.getLeftConstraint()+" "+con.getOperator()+" "+con.getRightConstraint()+") AND " ;
	}
	constraint = constraint.substring(0,constraint.length()-5);
	
	return constraint;
}

/**
 * This method will return SMT constraints of the form (or (StringValue) 
 * (and (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) )
 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
 * 
 */
public String generateSMTAndConstraints(ArrayList<ConstraintObject> constraintList){
	
	String constraintStr = "";
	String constr1 ="";
	for(ConstraintObject con : constraintList){
		constr1 =  getSMTAndConstraint(con,constr1);
		constraintStr = constr1;
	}
	return constraintStr;
}

public String generateSMTAndConstraints(ArrayList<ConstraintObject> constraintList,String constraintStr){
	String constr1 ="";
	for(ConstraintObject con : constraintList){
		constr1 =  getSMTAndConstraint(con,constr1);
		constraintStr = constr1;
	
	}
	return constraintStr;
}


/**
 * This method will return SMT constraints of the form (or (StringValue) 
 * (or (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) )
 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
 * 
 */
public String generateSMTOrConstraints(ArrayList<ConstraintObject> constraintList){
	
	String constraintStr = "";
	for(ConstraintObject con : constraintList){
		constraintStr += getSMTOrConstraint(con,constraintStr);
	}
	return constraintStr;
}

public String generateSMTOrConstraints(ArrayList<ConstraintObject> constraintList,String constraintStr){

	for(ConstraintObject con : constraintList){
		constraintStr += getSMTOrConstraint(con,constraintStr);
	}
	return constraintStr;
}
/**
 * This method will return SMT constraints of the form (or (StringValue) (distinct (colname tableNameNo_colName)(colName tableNameNo_colName)) )
 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
 * 	
 * @param con
 * @param s1
 * @return
 */
public String getSMTAndConstraint(ConstraintObject con, String s1){
	
	String cvcStr ="";
	
	if(s1 != null && !s1.isEmpty()){
		cvcStr += " (and ";
		cvcStr += s1;
	}
	
	if(con != null){
		cvcStr += "(" +con.getOperator()+"  (" +con.getLeftConstraint()+") ("+con.getRightConstraint()+") )";
	}
	if(s1 != null && !s1.isEmpty()){
		cvcStr +=")  ";
	}
	return cvcStr;
}

/**
 * This method will return SMT constraints of the form (or (StringValue) (distinct (colname tableNameNo_colName)(colName tableNameNo_colName)) )
 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
 * 	
 * @param con
 * @param s1
 * @return
 */
public String getSMTOrConstraint(ConstraintObject con, String s1){
	
	String cvcStr ="";
	
	if(s1 != null && !s1.isEmpty()){
		cvcStr += " (or ";
		cvcStr += s1;
	}
	
	if(con != null){
		cvcStr += "(" +con.getOperator()+"  (" +con.getLeftConstraint()+") ("+con.getRightConstraint()+") )";
	}
	if(s1 != null && !s1.isEmpty()){
		cvcStr +=")  ";
	}
	return cvcStr;
}



/**
 * This method returns OR'ed constraints with Assert and ; as required for CVC solver. 
 * @param constraintList
 * @return
 */
public String generateCVCOrConstraints(ArrayList<ConstraintObject> constraintList){
	
	String constraint = "";
	
	for(ConstraintObject con : constraintList){
		constraint += "("+con.getLeftConstraint()+" "+con.getOperator()+" "+con.getRightConstraint()+") OR " ;
	}
	constraint = constraint.substring(0,constraint.length()-4);
	
	return constraint;
}

/**
 * This method returns OR'ed constraints with Assert and ; as required for CVC solver. 
 * @param constraintList
 * @return
 
public String generateCVCNotConstraints(ArrayList<ConstraintObject> constraintList){
	
	String constraint = "";
	constraint += "ASSERT NOT ";
	for(ConstraintObject con : constraintList){
		constraint += "("+con.getLeftConstraint()+con.getOperator()+con.getRightConstraint()+") OR " ;
	}
	constraint = constraint.substring(0,constraint.length()-4);
	constraint +="; \n";
	return constraint;
}*/



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
	 * Used to get SMT LIB constraint for this column for the given tuple position
	 * 
	 * @param col
	 * @param index
	 * @return
	 */
	public static String smtMap(Column col, String index){
			Table table = col.getTable();
			String tableName = col.getTableName();
			String columnName = col.getColumnName();
			int pos = table.getColumnIndex(columnName);
			
			String smtCond = "";
			//String colName =tableName+"_"+columnName;
			String colName = columnName+pos;
			smtCond = "("+colName+" "+"(select O_"+tableName+" "+index +") )";
			return smtCond;
		}

	/**
	 * Used to get SMT LIB constraint for this column
	 * @param col
	 * @param n
	 * @return
	 */
	public static String smtMap(Column col, Node n){
		Table table = col.getTable();
		String tableName = col.getTableName();
		String columnName = col.getColumnName();
		String tableNo = n.getTableNameNo();
		
		int index = Integer.parseInt(tableNo.substring(tableNo.length()-1));
		int pos = table.getColumnIndex(columnName);
		
		String smtCond = "";
		//String colName =tableName+"_"+columnName;
		String colName = columnName+pos;
		smtCond = "("+colName+" "+"(select O_"+tableName+" "+index +") )";
		return smtCond;
	}

	/**
	 * Used to get SMT constraint for the given node for the given tuple position
	 * @param n
	 * @param index
	 * @return
	 */
	public static String cmtMapNode(Node n, String index){
		if(n.getType().equalsIgnoreCase(Node.getValType())){
			return n.getStrConst();
		}
		else if(n.getType().equalsIgnoreCase(Node.getColRefType())){
			return smtMap(n.getColumn(), index);
		}
		else if(n.getType().toString().equalsIgnoreCase("i")){
			return "i";
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType())){
			return "("+ n.getOperator()+" "+cmtMapNode(n.getLeft(), index) +" " + cmtMapNode(n.getRight(), index)+")";
		}
		else return "";
	}

	
}
