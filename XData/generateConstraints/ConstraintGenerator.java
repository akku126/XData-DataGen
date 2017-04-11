package generateConstraints;

import generateSMTConstraints.GenerateCVCConstraintForNodeSMT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Node;
import parsing.Table;

import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;
import util.Utilities;

/**
 * This class generates the constraints based on solver in XData.Properties file
 * 
 * @author shree
 *
 */
//FIXME:  Needs fine tuning - methods can be combined adding more parameters in constraint object.
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
			constraint += generateSMTAndConstraints(AndConstraintList,null);
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
			constraint += generateSMTOrConstraints(OrConstraintList,null);
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
			constraint = generateSMTAndConstraints(constraintList,null);
		}
		return constraint;
	}
	
	/**
	 * This method takes in the ConstraintObject List as input and generates an ASSERT constraint String AND'ing all conditions.
	 * The returned string holds the AND'ed constraints in SMT or CVC format. 
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	
	public String generateAssertANDConstraints(GenerateCVC1 cvc, ArrayList<ConstraintObject> constraintList){
	String constraint = "";
	if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
		constraint = "\n ASSERT " + generateCVCAndConstraints(constraintList)+";";
	}else{
		constraint = "\n (assert "+generateSMTAndConstraints(constraintList,null)+")";
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
			constraint = generateSMTOrConstraints(constraintList,null);
		}
		return constraint;
	}
	
	/**
	 * This method takes in the ConstraintObject List as input and generates an ASSERT constraint String OR'ing all conditions.
	 * The returned string holds the OR'ed constraints in SMT or CVC format.
	 *  
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateAssertOrConstraints(GenerateCVC1 cvc, ArrayList<ConstraintObject> constraintList){
		String constraint = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			constraint = "\n ASSERT"+generateCVCOrConstraints(constraintList)+";";
		}else{
			constraint = "\n (assert "+generateSMTOrConstraints(constraintList,null)+")";
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
	//constraint += "(";
	for(ConstraintObject con : constraintList){
		constraint += "("+con.getLeftConstraint()+" "+con.getOperator()+" "+con.getRightConstraint()+") AND " ;
	}
	constraint = constraint.substring(0,constraint.length()-5);
	//constraint += ")";
	return constraint;
}

/**
 * This method will return SMT constraints of the form (or (StringValue) 
 * (and (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) )
 * StringValue holds the previous constraint of same form thus forming nested structure as required for SMT.
 * 
 */
/*public String generateSMTAndConstraints(ArrayList<ConstraintObject> constraintList){
	
	String constraintStr = "";
	String constr1 ="";
	for(ConstraintObject con : constraintList){
		constr1 =  getSMTAndConstraint(con,constr1);
		constraintStr = constr1;
	}
	return constraintStr;
}*/

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
/*public String generateSMTOrConstraints(ArrayList<ConstraintObject> constraintList){
	
	String constraintStr = "";
	for(ConstraintObject con : constraintList){
		constraintStr += getSMTOrConstraint(con,constraintStr);
	}
	return constraintStr;
}*/

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
	public static String smtMapNode(Node n, String index){
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
			return "("+ n.getOperator()+" "+smtMapNode(n.getLeft(), index) +" " + smtMapNode(n.getRight(), index)+")";
		}
		else return "";
	}

	
	//Specific methods for each datatype in solver file
	/**
	 * This method returns the header value for the Solver depending on whether solver is CVC / SMT
	 * If solver is CVC - it returns nothing
	 * If solver is SMT - it returns all options required for producing required output.
	 * 
	 * @param cvc
	 * @return
	 */
	public String getHeader(GenerateCVC1 cvc){
		String header = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			return header;
		}
		else{
			header = "(set-logic ALL_SUPPORTED)";
			header += "\n (set-option:produce-models true) \n (set-option :interactive-mode true) \n (set-option :produce-assertions true) \n (set-option :produce-assignments true) ";
		}
		return header;
	}
	
	/**
	 * This method returns the Constraint String for defining and declaring integer type data
	 * 
	 * @param cvc
	 * @param col
	 * @param minVal
	 * @param maxVal
	 * @return
	 */
	public String getIntegerDatatypes(GenerateCVC1 cvc, Column col, int minVal, int maxVal){
		
		String constraint ="";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			constraint = "\n"+col+" : TYPE = SUBTYPE (LAMBDA (x: INT) : (x > "+(minVal-4)+" AND x < "+(maxVal+4)+") OR (x > -100000 AND x < -99995));\n";
		}
		else{
			constraint = "\n(define-sort i_"+col+"() Int)";
			constraint += "\n(define-fun get"+col+" ((i_"+col+" Int)) Bool \n\t\t(or (and " +
																												"\n\t\t\t(> i_"+col+" "+((minVal-4)>0?(minVal-4):0)+") " +
																												"\n\t\t\t(< i_"+col+" "+(maxVal+4)+")) " +
																											"\n\t\t    (and " +
																												"\n\t\t\t(> i_"+col+" (- "+100000+")) " +
																												"\n\t\t\t(< i_"+col+" "+"(- "+99995+")))))";			
		}
		return constraint;
	}
	
	/**
	 * This method returns a constraint string that holds the allowed null values for the integer data defined
	 * 
	 * @param cvc
	 * @param col
	 * @return
	 */
	public String getIntegerNullDataValues(GenerateCVC1 cvc, Column col){
		String constraint ="";
		String isNullMembers = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			
			constraint = "ISNULL_" + col +" : "+ col + " -> BOOLEAN;\n";
			for(int k=-99996;k>=-99999;k--){
				isNullMembers += "ASSERT ISNULL_"+col+"("+k+");\n";
			}
			constraint += isNullMembers;
		}else{
			HashMap<String, Integer> nullValuesInt = new HashMap<String, Integer>();
			for(int k=-99996;k>=-99999;k--){
				nullValuesInt.put(k+"",0);
			}
			constraint += defineIsNull(nullValuesInt, col);			
		}
		return constraint;
	}
	
	/**
	 * This method returns the Constraint String for defining and declaring Real type data
	 * 
	 * @param cvc
	 * @param col
	 * @param minVal
	 * @param maxVal
	 * @return
	 */
	public String getRealDatatypes(GenerateCVC1 cvc, Column col, double minVal, double maxVal){
		String constraint ="";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			String maxStr=util.Utilities.covertDecimalToFraction(maxVal+"");
			String minStr=util.Utilities.covertDecimalToFraction(minVal+"");
			constraint = "\n"+col+" : TYPE = SUBTYPE (LAMBDA (x: REAL) : (x >= "+(minStr)+" AND x <= "+(maxStr)+") OR (x > -100000 AND x < -99995));\n";			
		}
		else{
			constraint += "\n(define-sort r_"+col+"() Real)";
			constraint += "\n(define-fun get"+col+" ((r_"+col+" Real)) Bool \n\t\t(or (and " +
																												"\n\t\t\t(>= r_"+col+" "+minVal+") " +
																												"\n\t\t\t(<= r_"+col+" "+maxVal+")) " +
																											"\n\t\t    (and " +
																												"\n\t\t\t(> r_"+col+" (- "+100000+")) " +
																												"\n\t\t\t(< r_"+col+" "+"(- "+99995+")))))";
			
			
		}
		return constraint;
	}
	
	/**
	 * This method returns a constraint string that holds the allowed null values for the Real data defined
	 * 
	 * @param cvc
	 * @param col
	 * @return
	 */
	public String getRealNullDataValues(GenerateCVC1 cvc, Column col){
		String constraint ="";
		String isNullMembers = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			
			constraint += "ISNULL_" + col +" : "+ col + " -> BOOLEAN;\n";
			for(int k=-99996;k>=-99999;k--){
				isNullMembers += "ASSERT ISNULL_"+col+"("+k+");\n";
				
			}
			constraint += isNullMembers;			
		}else{
			HashMap<String, Integer> nullValuesInt = new HashMap<String, Integer>();
			for(int k=-99996;k>=-99999;k--){
				nullValuesInt.put(k+"",0);
			}
			constraint +=defineIsNull(nullValuesInt, col);			
		}
		return constraint;
	}
	
	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Null values for given column
	 * 
	 * @param colValueMap
	 * @param col
	 * @return
	 */
	public static String defineIsNull(HashMap<String, Integer> colValueMap, Column col){
		String IsNullValueString = "";
		if(col.getCvcDatatype().equalsIgnoreCase("Int")){
			IsNullValueString +="\n(declare-const null_"+col+" i_"+col+")"; //declare constant of form   (declare-const null_column_name ColumnName)
			IsNullValueString += "\n(define-fun ISNULL_"+col+" ((null_"+col+" i_"+col+")) Bool ";
		}else if(col.getCvcDatatype().equalsIgnoreCase("Real")){
			IsNullValueString +="\n(declare-const null_"+col+" r_"+col+")"; //declare constant of form   (declare-const null_column_name ColumnName)
			IsNullValueString += "\n(define-fun ISNULL_"+col+" ((null_"+col+" r_"+col+")) Bool ";
		}else{
			IsNullValueString +="\n(declare-const null_"+col+" "+col+")"; //declare constant of form   (declare-const null_column_name ColumnName)
			IsNullValueString += "\n(define-fun ISNULL_"+col+" ((null_"+col+" "+col+")) Bool ";
			
		}
		
		IsNullValueString += getOrForNullDataTypes("null_"+col, colValueMap.keySet(), "");//Get OR of all null columns
		IsNullValueString += ")";
		return IsNullValueString;
	}
	
	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Not-Null values for given column
	 * @param colValueMap
	 * @param col
	 * @return
	 */
	public static String defineNotIsNull(HashMap<String, Integer> colValueMap, Column col){
		String NotIsNullValueString = "";
		
		if(col.getCvcDatatype().equalsIgnoreCase("Int")){
			NotIsNullValueString +="\n(declare-const notnull_"+col+" i_"+col+")"; //declare constant of form   (declare-const null_column_name ColumnName)
			NotIsNullValueString += "\n(define-fun NOTISNULL_"+col+" ((notnull_"+col+" i_"+col+")) Bool ";
		}else if(col.getCvcDatatype().equalsIgnoreCase("Real")){
			NotIsNullValueString +="\n(declare-const notnull_"+col+" r_"+col+")"; //declare constant of form   (declare-const null_column_name ColumnName)
			NotIsNullValueString += "\n(define-fun NOTISNULL_"+col+" ((notnull_"+col+" r_"+col+")) Bool ";
		}else{
			NotIsNullValueString +="\n(declare-const notnull_"+col+" "+col+")"; //declare constant of form   (declare-const null_column_name ColumnName)
			NotIsNullValueString += "\n(define-fun NOTISNULL_"+col+" ((notnull_"+col+" "+col+")) Bool ";
			
		}
		
		NotIsNullValueString += getOrForNullDataTypes("notnull_"+col, colValueMap.keySet(), "");;//Get OR of all non-null columns
		
		NotIsNullValueString += ")";
		return NotIsNullValueString;
	}
	
	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Null values for a column concatenated with OR
	 * @param colconst
	 * @param columnValues
	 * @param tempString
	 * @return
	 */
	public static String getOrForNullDataTypes(String colconst, Set columnValues, String tempString){
		
	
		Iterator it = columnValues.iterator();
		int index = 0;
		while(it.hasNext()){
			index++;
			tempString = getIsNullOrString(colconst,((String)it.next()),tempString);
			
		}
		return tempString;
	}
	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Not-Null values for a column concatenated with OR
	 * @param colconst
	 * @param colValue
	 * @param tempstring
	 * @return
	 */
	public static String getIsNullOrString(String colconst,String colValue,String tempstring){
		
		String tStr = "";
		
		if(tempstring != null && !tempstring.isEmpty()){
			tStr = "(or "+tempstring;	
		}
		if(colValue != null && colValue.startsWith("-")){
			tStr +=" (= "+colconst+" (- "+colValue.substring(1,colValue.length())+"))";
		}else{
			tStr +=" (= "+colconst+" "+colValue+")";
		}
		
		if(tempstring != null && !tempstring.isEmpty()){
			tStr += ")";	
		}
		
		return tStr;
	}
	/**
	 * This method returns the Constraint String for defining and declaring String / VARCHAR type data
	 * @param cvc
	 * @param columnValue
	 * @param col
	 * @param unique
	 * @return
	 * @throws Exception
	 */
	public String getStringDataTypes(GenerateCVC1 cvc, Vector<String> columnValue,Column col,boolean unique) throws Exception{
		String constraint = "";
		String colValue = "";
		HashSet<String> uniqueValues = new HashSet<String>();
		String isNullMembers = "";
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			//If CVC Solver
			constraint = "\nDATATYPE \n"+col+" = ";
			if(columnValue.size()>0){
				if(!unique || !uniqueValues.contains(columnValue.get(0))){
					colValue =  Utilities.escapeCharacters(col.getColumnName())+"__"+Utilities.escapeCharacters(columnValue.get(0));//.trim());
					constraint += "_"+colValue;
					isNullMembers += "ASSERT NOT ISNULL_"+col+"(_"+colValue+");\n";
					uniqueValues.add(columnValue.get(0));
				}				
				colValue = "";
				for(int j=1; j<columnValue.size() || j < 4; j++){
					if(j<columnValue.size())
					{
						if(!unique || !uniqueValues.contains(columnValue.get(j))){
							colValue =  Utilities.escapeCharacters(col.getColumnName())+"__"+Utilities.escapeCharacters(columnValue.get(j));
						}
					}
					else {
						if(!uniqueValues.contains(((Integer)j).toString())){
							colValue =  Utilities.escapeCharacters(col.getColumnName())+"__"+j;
						}else{
							continue;
						}
					}					 
					if(!colValue.isEmpty()){
						constraint = constraint+" | "+"_"+colValue;
						isNullMembers += "ASSERT NOT ISNULL_"+col+"(_"+colValue+");\n";
					}
				}
			}			
			//Adding support for NULLs
			if(columnValue.size()!=0){
				constraint += " | ";
			}
			for(int k=1;k<=4;k++){
				constraint += "NULL_"+col+"_"+k;
				if(k < 4){
					constraint += " | ";
				}
			}						
			constraint = constraint+" END\n;";
			constraint += "ISNULL_" + col +" : "+ col + " -> BOOLEAN;\n";
			HashMap<String, Integer> nullValuesChar = new HashMap<String, Integer>();
			for(int k=1;k<=4;k++){
				isNullMembers += "ASSERT ISNULL_" + col+"(NULL_"+col+"_"+k+");\n";
				nullValuesChar.put("NULL_"+col+"_"+k, 0);
			}						
			constraint += isNullMembers;
			
		}
		else{//If SMT SOLVER
			
			constraint +="\n"+"(declare-datatypes () (("+col;
			HashMap<String, Integer> nullValuesChar = new HashMap<String, Integer>();
			if(columnValue.size()>0){
				if(!unique || !uniqueValues.contains(columnValue.get(0))){
					colValue =  Utilities.escapeCharacters(col.getColumnName())+"__"+Utilities.escapeCharacters(columnValue.get(0));//.trim());
					constraint += " (_"+colValue+") ";
					uniqueValues.add(columnValue.get(0));
				}
				colValue = "";
				for(int j=1; j<columnValue.size() || j < 4; j++){
					if(j<columnValue.size())
					{
						if(!unique || !uniqueValues.contains(columnValue.get(j))){
							colValue =  Utilities.escapeCharacters(col.getColumnName())+"__"+Utilities.escapeCharacters(columnValue.get(j));
						}
					}
					else {
						if(!uniqueValues.contains(((Integer)j).toString())){
							colValue =  Utilities.escapeCharacters(col.getColumnName())+"__"+j;
						}else{
							continue;
						}
					}
					 
					if(!colValue.isEmpty()){
						constraint += ""+"(_"+colValue+")";
					}
				}
			}			
			//Adding support for NULL values
			if(columnValue.size()!=0){
				constraint += " (";
			}
			for(int k=1;k<=4;k++){
				constraint += "NULL_"+col+"_"+k;
				if(k < 4){
					constraint += ") (";
				}
			}						
			constraint += "))))"+"\n;";		
			
		
			for(int k=1;k<=4;k++){
				//isNullMembers += "ASSERT ISNULL_" + columnName+"(NULL_"+columnName+"_"+k+");\n";
				nullValuesChar.put("NULL_"+col+"_"+k, 0);
			}	
			constraint += defineNotIsNull(nullValuesChar, col);
			constraint +=defineIsNull(nullValuesChar, col);
			}

		
		return constraint;
	}

	/**
	 * This method returns the null integer values for CVC data type.
	 * @param cvc
	 * @param col
	 * @return
	 */
	public String getNullMembers(GenerateCVC1 cvc, Column col){
	String isNullMembers = "";
		
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			for(int k=-99996;k>=-99999;k--){
				isNullMembers += "ASSERT ISNULL_"+col+"("+k+");\n";
			}
		}else{
			isNullMembers ="";
		}
		return isNullMembers;
	}
	
	/**
	 * This method returns the Footer Constraints based on the solver 
	 * @param cvc
	 * @return
	 */
	public String getFooter(GenerateCVC1 cvc){
		
		String temp = "";
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			temp += "\n\nQUERY FALSE;";			// need to right generalize one
			temp += "\nCOUNTERMODEL;";
		}
		else{
			temp += "\n\n(check-sat)";			// need to right generalize one
			temp += "\n(get-assertions) \n (get-assignment) \n(get-model)";
		}
		return temp;
	}
	
	/**
	 * This method returns the constraint String that holds the Tuple Types based on solver
	 * 
	 * @param cvc
	 * @param col
	 * @return
	 */
	public String getTupleTypesForSolver(GenerateCVC1 cvc){
		
		String tempStr = "";
		Column c;
		Table t;
		String temp;
		Vector<String> tablesAdded = new Vector<String>();
		if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
			
				tempStr += "\n%Tuple Types for Relations\n";			 
				for(int i=0;i<cvc.getResultsetTables().size();i++){
					t = cvc.getResultsetTables().get(i);
					temp = t.getTableName();
					if(!tablesAdded.contains(temp)){
						tempStr += temp + "_TupleType: TYPE = [";
					}
					for(int j=0;j<cvc.getResultsetColumns().size();j++){
						c = cvc.getResultsetColumns().get(j);
						if(c.getTableName().equalsIgnoreCase(temp)){
							String s=c.getCvcDatatype();
							if(s!= null && (s.equalsIgnoreCase("INT") || s.equalsIgnoreCase("REAL") || s.equalsIgnoreCase("TIME") || s.equalsIgnoreCase("DATE") || s.equalsIgnoreCase("TIMESTAMP")))
								tempStr += c.getColumnName() + ", ";
							else
								tempStr+=c.getCvcDatatype()+", ";
						}
					}
					tempStr = tempStr.substring(0, tempStr.length()-2);
					tempStr += "];\n";
					/*
					 * Now create the Array for this TupleType
					 */
					tempStr += "O_" + temp + ": ARRAY INT OF " + temp + "_TupleType;\n";
				}
		}
		else{
			
			tempStr += "\n;%Tuple Types for Relations\n";
			for(int i=0;i<cvc.getResultsetTables().size();i++){
				int index = 0;
				t = cvc.getResultsetTables().get(i);
				temp = t.getTableName();
				if(!tablesAdded.contains(temp)){
					tempStr += "\n (declare-datatypes () (("+temp +"_TupleType" + "("+temp +"_TupleType ";
				}
				for(int j=0;j<cvc.getResultsetColumns().size();j++){				
					c = cvc.getResultsetColumns().get(j);
					if(c.getTableName().equalsIgnoreCase(temp)){						
						String s=c.getCvcDatatype();
						if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
							tempStr += "("+c+index+" "+s + ") ";
						else
							tempStr+= "("+c+index+" "+c.getColumnName() + ") ";						
						index++;
					}
				}
				tempStr += ") )) )\n";
				//Now create the Array for this TupleType
				tempStr += "(declare-fun O_" + temp + "() (Array Int " + temp + "_TupleType))";
			}
			
		}
		return tempStr;
		
	}
	
	
	
	/**
	 * Generate CVC3 constraints for the given node and its tuple position
	 * @param queryBlock
	 * @param n
	 * @param index
	 * @return
	 */
	public static String genPositiveCondsForPred(GenerateCVC1 cvc,QueryBlockDetails queryBlock, Node n, int index){
		if(n.getType().equalsIgnoreCase(Node.getColRefType())){			
			if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
				return "O_"+cvcMap(n.getColumn(), index+"");
			}else{				
				return smtMap(n.getColumn(), index+"");
			}
		}
		else if(n.getType().equalsIgnoreCase(Node.getValType())){
			if(!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		}
		else if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) || n.getType().equalsIgnoreCase(Node.getBaoNodeType()) || n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType()) || n.getType().equalsIgnoreCase(Node.getOrNodeType())){
			return "("+ genPositiveCondsForPred(cvc,queryBlock, n.getLeft(), index) +" "+ n.getOperator() +" "+ 
					genPositiveCondsForPred(cvc,queryBlock, n.getRight(), index) +")";
		}	
		return null;
	}
	
	
}

