package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.Configuration;
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
	
	
	private static boolean isTempJoin = false;
	
	/**
	 * Constructor
	 */
	public GenerateJoinPredicateConstraints(){
		 
		 if(Configuration.getProperty("tempJoins").equalsIgnoreCase("true")){
			 this.isTempJoin = true;
		 }else {
			 this.isTempJoin = false;
		 }
	}
	
	
	public static String getConstraintsforEquivalenceClasses(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception{
		String constraintString="";
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins1(cvc, queryBlock, n1,n2);
			}
		}
		
		if(Configuration.getProperty("tempJoins").equalsIgnoreCase("true")){
			 isTempJoin = true;
		 }else {
			 isTempJoin = false;
		 }
		/*
		if(isTempJoin) {
			String constr,declare="";
			int st_index=0,end_index=0;
			constr = constraintString;
			while(constr.indexOf("(declare-datatypes ()") != -1) {
			st_index = constr.indexOf("(declare-datatypes ()");
			end_index = constr.indexOf("_TupleType))")+12;
			if(!declare.contains(constr.substring(st_index, end_index)))
				declare += constr.substring(st_index, end_index) + " \n";
			constr = constr.substring(0, st_index)+constr.substring(end_index);
			}
			
			constraintString =  declare + constr;
		}*/
		
		return constraintString;
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

	public static String getConstraintsForNonEquiJoinsTJ(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n) throws Exception{

		String constraintString = "";
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
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();
	
		/** Do a round robin for the smaller value of the group number */
		for(int k=1,l=1;; k++,l++){
			//constraintString += "ASSERT ("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
				//	GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, (l+offset2-1))+");\n";
			
			/*ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock,n1, ((k-1)*tuples1+offset1)));
			constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock, n2, (l+offset2-1)));
			constrObj.setOperator(operator);
			constrObjList.add(constrObj);*/
			
			constraintString += constrGen.getAssertConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1)),operator,constrGen.genPositiveCondsForPred(queryBlock, n2, (l+offset2-1)));
					
			
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
		//constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
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


		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();
	
		/** Do a round robin for the smaller value of the group number */
		for(int k=1,l=1;; k++,l++){
			//constraintString += "("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
				//	GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, (l+offset2-1))+") AND ";
			ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1)));
			constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock, n2, (l+offset2-1)));
			constrObj.setOperator(operator);
			constrObjList.add(constrObj);
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
		constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
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

		
		if(Configuration.getProperty("tempJoins").equalsIgnoreCase("true")){
			 isTempJoin = true;
		 }else {
			 isTempJoin = false;
		 }
		if(!isTempJoin) {
			/** get the details of each node */
			String t1 = getTableName(n1);
			String t2 = getTableName(n2);
	
			//int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
			//int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());
			
			//below two lines added by rambabu 
			int pos1 = cvc.getTableMap().getTable(t1.toUpperCase()).getColumnIndex(getColumn(n1).getColumnName());
			int pos2 = cvc.getTableMap().getTable(t2.toUpperCase()).getColumnIndex(getColumn(n2).getColumnName());
	
			String r1 = getTableNameNo(n1);
			String r2 = getTableNameNo(n2);
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
			
			ConstraintGenerator constrGen = new ConstraintGenerator();
			ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();
		
		
			for(int i=0; i<noOfgroups; i++){
				/**Do a round robin for the smaller value*/
				for(int k=1,l=1;; k++,l++){
	
					//constraintString += "ASSERT ("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((i*tuples1)+k+offset1-1))+ operator + 
						//	GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((i*tuples2)+l+offset2-1))+");\n";
					/*ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((i*tuples1)+k+offset1-1)));
					constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock, n2, ((i*tuples2)+l+offset2-1)));
					constrObj.setOperator(operator);
					constrObjList.add(constrObj);
					*/
					
					constraintString += constrGen.getAssertConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((i*tuples1)+k+offset1-1)),operator,constrGen.genPositiveCondsForPred(queryBlock, n2, ((i*tuples2)+l+offset2-1)));
	
					if(tuples1>tuples2){
						if(l==tuples2 && k<tuples1)	l=0;
						if(k>=tuples1) break;
					}
					else if(tuples1<tuples2){
						if(l<tuples2 && k==tuples1)	k=0;
						if(l>=tuples2) break;				
					}
					else{// if tuples1==tuples2 
						if(l==tuples1) break;
					}
				}
			
			} 
		}
		else if(isTempJoin){
	// Join Temp table implementation	
			Vector<String> tablesAdded = new Vector<String>();
			Table f1,f2;
			String temp1,temp2,joinTable,ColName;
			int t1Columnindex, t2Columnindex;
			int findex = 0;
			f1 = n1.getTable();
			f2 = n2.getTable();
			temp1 = f1.getTableName();
			temp2 = f2.getTableName();
			joinTable = temp1 + "join" + temp2;
				if(!tablesAdded.contains(joinTable)){
					constraintString += "\n (declare-datatypes () (("+joinTable +"_TupleType" + "("+joinTable +"_TupleType ";
				}
				
				for(String key : f1.getColumns().keySet()) {
					ColName = f1.getColumns().get(key).getColumnName();
					String s = f1.getColumns().get(key).getCvcDatatype();
						if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
							constraintString += "("+joinTable+"_"+f1.getColumns().get(key)+findex+" "+s + ") ";
						else
							constraintString += "("+joinTable+"_"+f1.getColumns().get(key)+findex+" "+ColName + ") ";						
						findex++;
				}
				int delimit = findex;
				for(String key : f2.getColumns().keySet()) {
					ColName = f2.getColumns().get(key).getColumnName();
					String s = f2.getColumns().get(key).getCvcDatatype();
						if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
							constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+s + ") ";
						else
							constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+ColName + ") ";						
						findex++;
				}
				constraintString += ") )) )\n";
				//Now create the Array for this TupleType
				constraintString += "(declare-fun O_" + joinTable + "() (Array Int " + joinTable + "_TupleType))\n\n";
				
			
			t1Columnindex	= n1.getColumn().getTable().getColumnIndex(n1.getColumn().getColumnName());
			t2Columnindex	= n2.getColumn().getTable().getColumnIndex(n2.getColumn().getColumnName());
			ConstraintGenerator constrGen = new ConstraintGenerator();
				
			String constraint1 = constrGen.genPositiveCondsForPredF(queryBlock, n1, "i1");
			String constraint2 = constrGen.genPositiveCondsForPredF(queryBlock, n2, "j1");
	
	
			
			constraintString += "(assert (forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");
			//constraintString += "(forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");
			
			
			String constraint3 = "("+joinTable+"_"+n1.getColumn().getColumnName()+t1Columnindex;
			constraint3 += "("+" select O_"+joinTable+" "+" k1 ) )";
			
			constraintString += "(exists ((k1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" "+ ")) \n");
			
			t2Columnindex += delimit;
			String constraint4 = "("+joinTable+"_"+n2.getColumn().getColumnName()+t2Columnindex;
			constraint4 += "("+" select O_"+joinTable+" "+" k1 ) )";
			
			constraintString += "(" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" "+ "))))) \n\n");
		
			//constraintString += "(assert (forall ((k1 Int)) (exists ((i1 Int)(j1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" " + " )\n");
			
			//constraintString +=  "("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" " +  ")))))\n");
			
		}
	// Join Temp table implementation end
		
	//	constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
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
		//int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		//int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());
		
		int pos1 = cvc.getTableMap().getTable(t1.toUpperCase()).getColumnIndex(n1.getColumn().getColumnName()); // added by rambabu
		int pos2 = cvc.getTableMap().getTable(t2.toUpperCase()).getColumnIndex(n2.getColumn().getColumnName()); // added by rambabu

		if(Configuration.getProperty("tempJoins").equalsIgnoreCase("true")){
			 isTempJoin = true;
		 }else {
			 isTempJoin = false;
		 }
		
		if(!isTempJoin) {
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
			ConstraintGenerator constrGen = new ConstraintGenerator();
			ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();
			
			for(int i=0; i<noOfgroups; i++){
				/**Do a round robin for the smaller value*/
				for(int k=1,l=1;; k++,l++){
	
					//constraintString += "("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((i*tuples1)+k+offset1-1))+ operator + 
						//	GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((i*tuples2)+l+offset2-1))+") AND ";
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((i*tuples1)+k+offset1-1)));
					constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock, n2, ((i*tuples2)+l+offset2-1)));
					constrObj.setOperator(operator);
					constrObjList.add(constrObj);
	
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
			constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
		}
		else if(isTempJoin){
			// Join Temp table implementation	
					Vector<String> tablesAdded = new Vector<String>();
					Table f1,f2;
					String temp1,temp2,joinTable,ColName;
					int t1Columnindex, t2Columnindex;
					int findex = 0;
					f1 = n1.getTable();
					f2 = n2.getTable();
					temp1 = f1.getTableName();
					temp2 = f2.getTableName();
					joinTable = temp1 + "join" + temp2;
						if(!tablesAdded.contains(joinTable)){
							constraintString += "\n (declare-datatypes () (("+joinTable +"_TupleType" + "("+joinTable +"_TupleType ";
						}
						
						for(String key : f1.getColumns().keySet()) {
							ColName = f1.getColumns().get(key).getColumnName();
							String s = f1.getColumns().get(key).getCvcDatatype();
								if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
									constraintString += "("+joinTable+"_"+f1.getColumns().get(key)+findex+" "+s + ") ";
								else
									constraintString += "("+joinTable+"_"+f1.getColumns().get(key)+findex+" "+ColName + ") ";						
								findex++;
						}
						int delimit = findex;
						for(String key : f2.getColumns().keySet()) {
							ColName = f2.getColumns().get(key).getColumnName();
							String s = f2.getColumns().get(key).getCvcDatatype();
								if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
									constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+s + ") ";
								else
									constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+ColName + ") ";						
								findex++;
						}
						constraintString += ") )) )\n";
						//Now create the Array for this TupleType
						constraintString += "(declare-fun O_" + joinTable + "() (Array Int " + joinTable + "_TupleType))\n\n";
						
					
					t1Columnindex	= n1.getColumn().getTable().getColumnIndex(n1.getColumn().getColumnName());
					t2Columnindex	= n2.getColumn().getTable().getColumnIndex(n2.getColumn().getColumnName());
					ConstraintGenerator constrGen = new ConstraintGenerator();
						
					String constraint1 = constrGen.genPositiveCondsForPredF(queryBlock, n1, "i1");
					String constraint2 = constrGen.genPositiveCondsForPredF(queryBlock, n2, "j1");
			
			
					
					constraintString += "(assert (forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");
					//constraintString += "(forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");
					
					
					String constraint3 = "("+joinTable+"_"+n1.getColumn().getColumnName()+t1Columnindex;
					constraint3 += "("+" select O_"+joinTable+" "+" k1 ) )";
					
					constraintString += "(exists ((k1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" "+ ") \n");
					
					t2Columnindex += delimit;
					String constraint4 = "("+joinTable+"_"+n2.getColumn().getColumnName()+t2Columnindex;
					constraint4 += "("+" select O_"+joinTable+" "+" k1 ) )";
					
					constraintString += " (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" "+ "))))) \n");
				
					constraintString += "(assert (forall ((k1 Int)) (exists ((i1 Int)(j1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" " + " )\n");
					
					constraintString +=  "("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" " +  ")))))\n");
					
		}
			// Join Temp table implementation end
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
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();
		
		/**Do a round robin for the smaller value of the group number*/
		for(int k=1,l=1;; k++,l++){
			//constraintString += "ASSERT ("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
					//GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((l-1)*tuples2+offset2))+");\n";
			
			/*ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1)));
			constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock, n2, ((l-1)*tuples2+offset2)));
			constrObj.setOperator(operator);
			constrObjList.add(constrObj);*/
			constraintString += constrGen.getAssertConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1)),operator,constrGen.genPositiveCondsForPred(queryBlock, n2, ((l-1)*tuples2+offset2)));
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
		//constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
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
		//int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		//int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());
		
		int pos1 = cvc.getTableMap().getTable(t1.toUpperCase()).getColumnIndex(n1.getColumn().getColumnName()); //added by rambabu
		int pos2 = cvc.getTableMap().getTable(t2.toUpperCase()).getColumnIndex(n2.getColumn().getColumnName()); //added by rambabu

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
			constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1)));
			constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock, n2, ((l-1)*tuples2+offset2)));
			constrObj.setOperator(operator);
			constrObjList.add(constrObj);
			
			//constraintString += "("+ GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1, ((k-1)*tuples1+offset1))+ operator +
			//		GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2, ((l-1)*tuples2+offset2))+") AND ";
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
		constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
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
			ConstraintGenerator constrGen = new ConstraintGenerator();
			ConstraintObject conObj = new ConstraintObject();
			ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
			
			//constraintString += "ASSERT ";

			for(int i=1;i<=count1;i++){
				for(int j=1;j<=count2;j++){
					String left ="", right = "";
					if(nulled.getQueryType() == 1 && queryBlock.getFromClauseSubQueries()!= null && queryBlock.getFromClauseSubQueries().size() != 0)
						left = ConstraintGenerator.getSolverMapping(nulled.getColumn(), (i-1)*cvc.getNoOfTuples().get(nulled.getTableNameNo())+offset1+"") ;
					else
						left = ConstraintGenerator.getSolverMapping(nulled.getColumn(), i+offset1-1+"") ;

					if(P0.getQueryType() == 1 && queryBlock.getFromClauseSubQueries()!= null && queryBlock.getFromClauseSubQueries().size() != 0)
						right =ConstraintGenerator.getSolverMapping(P0.getColumn(), (j-1)*cvc.getNoOfTuples().get(P0.getTableNameNo())+offset2+"") ;
					else
						right =ConstraintGenerator.getSolverMapping(P0.getColumn(), j+offset2-1+"") ;

					conObj.setLeftConstraint(left);
					conObj.setRightConstraint(right);
					conObj.setOperator("/=");
					
					constrList.add(conObj);
				}
			}

			  constraintString = constrGen.generateANDConstraintsWithAssert(constrList);//constraintString.substring(0, constraintString.length()-4);
			//constraintString += ";";
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
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		ConstraintGenerator constrGen = new ConstraintGenerator();
	
		if(cvc.isFne()){
			constraintString += "ASSERT NOT EXISTS (i: O_"+nulled.getTableName()+"_INDEX_INT): " +
					"(O_"+ GenerateCVCConstraintForNode.cvcMap(nulled, "i") + " = O_" + GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0) + ");";			
		}
		else{

			/** Open up FORALL and NOT EXISTS*/

			//constraintString += "ASSERT ";
			checkRepeatedRelations(cvc,cvc.getNoOfOutputTuples()); // TEMPCODE Rahul Sharma  to handle repeated relations
			for(int i = 1; i <= cvc.getNoOfOutputTuples().get(nulled.getTableName()) ; i++){/**FIXME: Handle repeated relations*/
				//constraintString += "(O_" + GenerateCVCConstraintForNode.cvcMap(nulled, i + "") + " /= O_" + GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0) + ") AND ";
				ConstraintObject constr = new ConstraintObject();
				constr.setLeftConstraint( ConstraintGenerator.getSolverMapping(nulled, i + ""));
				constr.setOperator("/=");
				constr.setRightConstraint(ConstraintGenerator.getSolverMapping(P0.getColumn(), P0));
				constrList.add(constr);				
			}
			constraintString = constrGen.generateANDConstraintsWithAssert(constrList);//constraintString.substring(0, constraintString.length()-4);
			//constraintString += ";";
		}
		return constraintString;
	}
	
	/**
     * TEMPCODE Rahul Sharma : to check if there is repeated relations, and remove them 
     * @param cvc
     * @param noOfOutputTuples
     */
    private static void checkRepeatedRelations(GenerateCVC1 cvc, HashMap<String, Integer> noOfOutputTuples) {
        // TODO Auto-generated method stub
        HashMap<String, Integer> tempMap = new HashMap<>(noOfOutputTuples.size());
        for (Map.Entry<String, Integer> entry : noOfOutputTuples.entrySet()) {
           tempMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        cvc.setNoOfOutputTuples(tempMap);       
    }
	
	public static String genNegativeCondsEqClass(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2, int tuple){
		String constraintString = new String();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		ConstraintGenerator constrGen = new ConstraintGenerator();
		
		for(int i = 1; i <= cvc.getNoOfOutputTuples().get(c1.getTable().getTableName()) ; i++){
			ConstraintObject constr = new ConstraintObject();
			constr.setLeftConstraint( ConstraintGenerator.getSolverMapping(c1.getColumn(), i + ""));
			constr.setOperator("/=");
			constr.setRightConstraint(ConstraintGenerator.getSolverMapping(c2.getColumn(), tuple +""));
			constrList.add(constr);
		}
		//constraintString = constraintString.substring(0, constraintString.length()-4);
		constraintString = constrGen.generateANDConstraintsWithAssert(constrList);
		return constraintString.trim();
	}
	
	public static String genNegativeCondsEqClassForTuplePair(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2, int tupleIndex1, int tupleIndex2){
		
		String constraintString = new String();
		ConstraintGenerator constrGen = new ConstraintGenerator();
		
		constraintString = constrGen.getAssertConstraint(c1.getColumn(), tupleIndex1, c2.getColumn(), tupleIndex2, "/=");
		
		/*constraintString += "ASSERT ";			
		constraintString += "(O_" + GenerateCVCConstraintForNode.cvcMap(c1.getColumn(), tupleIndex1 + "") + " /= O_" + GenerateCVCConstraintForNode.cvcMap(c2.getColumn(), tupleIndex2 + "") + ") AND ";
			
		constraintString = constraintString.substring(0, constraintString.length()-4);
		constraintString += ";"; */
		
		
		return constraintString;
	}
	
	public static ArrayList<ConstraintObject> genNegativeCondsEqClassForAllTuplePairs(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2, int tupleIndex1, int tupleIndex2){
		String constraintString = new String();
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		
		for(int i = 1; i <= tupleIndex1 ; i++){
				for(int j = 1; j <= tupleIndex2; j++){		
					
					ConstraintObject constr = new ConstraintObject();
					constr.setLeftConstraint( ConstraintGenerator.getSolverMapping(c1.getColumn(), i + ""));
					constr.setOperator("/=");
					constr.setRightConstraint(ConstraintGenerator.getSolverMapping(c2.getColumn(), j +""));
					constrList.add(constr);
			}
		}
		//constraintString = constrGen.generateANDConstraintsWithAssert(constrList);
		//return constraintString.trim();
		return constrList;
	}
	
	/**
	 * Generates positive constraints for the given set of nodes
	 * @param ec
	 */
	public static String genPositiveConds(GenerateCVC1 cvc,Vector<Node> ec){

		String constraintString = "";

		for(int i=0; i<ec.size()-1; i++)
		{
			Column col1 = ec.get(i).getColumn();
			Column col2 = ec.get(i+1).getColumn();

			constraintString += ConstraintGenerator.getPositiveStatement(col1, ec.get(i), col2, ec.get(i+1));
		}
		return constraintString;
	}

}
