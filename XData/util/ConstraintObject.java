package util;


import java.util.logging.Logger;

/**
 * This class holds the Constraint String generated for 
 * columns / already generated constraint string with which more constraints are to be concatenated.
 * 
 * @author shree
 *
 */
public class ConstraintObject {

	private static Logger logger = Logger.getLogger(ConstraintObject.class.getName());
	
	
	//This variable holds the generated constraint String(which can have AND'ed / OR'ed constraints) that has  
	//conjunction with or constraints and vice versa.  
	//This can also hold some simple assert constraints with which more constraints will be concatenated 
	private String assertConstraint;
	
	//Experimental variable to hold the column level constraint as a whole - with pos, index, table name and column name
	//Will not be feasible as it differs for CVC/SMT
	private String baseConstraintString;
	
	//This holds the left side constraints usually constraint on column with pos, index and table name.
	private String leftConstraint;
	
	//This holds the right side constraints usually constraint on column with pos, index and table name.
	private String rightConstraint;
	
	//This holds the operator with which left and right constraints are related
	private String operator;
	
	/**
	 * 
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}
	
	/**
	 * 
	 * @param logger the logger to set
	 */
	public static void setLogger(Logger logger) {
		ConstraintObject.logger = logger;
	}
	
	/**
	 * 
	 * @return the assertConstraint
	 */
	public String getAssertConstraint() {
		return assertConstraint;
	}
	
	/**
	 * 
	 * @param assertConstraint the assertConstraint to set
	 */
	public void setAssertConstraint(String assertConstraint) {
		this.assertConstraint = assertConstraint;
	}
	
	/**
	 * 
	 * @return the rightConstraint
	 */
	public String getRightConstraint() {
		return rightConstraint;
	}
	
	/**
	 * 
	 * @param rightConstraint the rightConstraint to set
	 */
	public void setRightConstraint(String rightConstraint) {
		this.rightConstraint = rightConstraint;
	}
	
	/**
	 * 
	 * @return the operator
	 */
	public String getOperator() {
		return operator;
	}
	
	/**
	 * 
	 * @param operator the operator to set
	 */
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	/**
	 * 
	 * @return the leftConstraint
	 */
	public String getLeftConstraint() {
		return leftConstraint;
	}
	
	/**
	 * 
	 * @param leftConstraint the leftConstraint to set
	 */
	public void setLeftConstraint(String leftConstraint) {
		this.leftConstraint = leftConstraint;
	}
}
