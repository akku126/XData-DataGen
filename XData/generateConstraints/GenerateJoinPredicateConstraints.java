package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.relation.Relation;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.QueryStructure;
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
	private static Vector<String> tablesAdded = new Vector<String>();
	
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

		if(left.getQueryType() == right.getQueryType()){/**If both nodes are of same type (i.e. either from clause sub query nodes or where clause sub query nodes or outer query block nodes)*/
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
//			Vector<String> tablesAdded = new Vector<String>();
			Table f1,f2;
			String temp1,temp2,joinTable,ColName;
			int t1Columnindex, t2Columnindex;
			int findex = 0;
			f1 = n1.getTable();
			f2 = n2.getTable();
			temp1 = f1.getTableName();
			temp2 = f2.getTableName();
			// TEMPCODE Rahul Sharma : Check if the tables are part of nested query, if so proceed further to generate sub query table constraints, otherwise break  
			boolean isPartOfSubQuery =  checkIfTablesArePartOfSubQuery(cvc,temp1,temp2);
//			if(temp1==temp2) { // correlation is present in the outer and inner queries // TEMPCODE Rahul Sharma
////				System.out.println("Correlation");
//				String innerQueryTable = String.join("join",queryBlock.getBaseRelations()).toLowerCase().replaceAll("\\d", "");
//				String outerQueryTable = queryBlock.getTopLevelRelation().getTableName().toString().toLowerCase();
//				String correlationAttribute = n1.getColumn().toString();
//				String innerTableAttibuteIndex = getInnerTableAttributeIndex(innerQueryTable,correlationAttribute);
//				String outerTableAttibuteIndex = getOuterTableAttributeIndex(outerQueryTable,correlationAttribute);
//				String correlation_operator = operator.toString();
//				constraintString = "(assert (forall ((i1 Int)) ";
//				constraintString+= "(exists ((j1 Int)) ";
//				constraintString+= "( "+correlation_operator+" ( "+innerQueryTable+"_"+correlationAttribute+innerTableAttibuteIndex
//									+ "( select O_"+innerQueryTable +" i1 )) ("+outerQueryTable+"_"+correlationAttribute+outerTableAttibuteIndex
//									+ " (select O_"+outerQueryTable+" j1 )) )"
//									+ ") ) )";
////				System.out.println(constraintString);
//				
//			}
			if(isPartOfSubQuery){
				joinTable = temp1 + "join" + temp2;
					if(!tablesAdded.contains(joinTable)){
						constraintString += "\n(declare-datatypes (("+joinTable +"_TupleType 0))" + "((("+joinTable +"_TupleType "; // TEMPCODE Rahul Sharma : fixed syntax error
					}
					
					for(String key : f1.getColumns().keySet()) {
						ColName = f1.getColumns().get(key).getColumnName();
						String s = f1.getColumns().get(key).getCvcDatatype();
							if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
								constraintString += "("+joinTable+"_"+f1.getColumns().get(key)+findex+" "+s + ") ";
							else {
//								constraintString += "("+joinTable+"_"+f1.getColumns().get(key)+findex+" "+ColName + ") ";
								constraintString += "("+joinTable+"_"+f1.getColumns().get(key)+findex+" "+ColName + ") "; // TEMPCODE Rahul Sharma : Changed ColName to s (datatype of column)
							}							
							findex++;
					}
					int delimit = findex;
					for(String key : f2.getColumns().keySet()) {
						ColName = f2.getColumns().get(key).getColumnName();
						String s = f2.getColumns().get(key).getCvcDatatype();
							if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
								constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+s + ") ";
							else {
//								constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+ColName + ") ";
								constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+ s + ") "; // TEMPCODE Rahul Sharma : Changed ColName to s (datatype of column)
							}
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
		
		
				
				constraintString += "(assert (forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " ) )":" "+ ")");
				//constraintString += "(forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");
				
				
//				String constraint3 = "("+joinTable+"_"+n1.getColumn().getColumnName()+t1Columnindex;
//				constraint3 += "("+" select O_"+joinTable+" "+" k1 ) )";
				
//				constraintString += "(exists ((k1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" "+ ") \n"); // TEMPCODE : Rahul Sharma : Removed one extra ")" from the end
				constraintString += "(exists ((k1 Int)) "; // TEMPCODE : Rahul Sharma : added all other attributes
				
//				t2Columnindex += delimit;
//				String constraint4 = "("+joinTable+"_"+n2.getColumn().getColumnName()+t2Columnindex;
//				constraint4 += "("+" select O_"+joinTable+" "+" k1 ) )";
				
//				constraintString += "(" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" "+ "))))) )\n\n");
			
				//constraintString += "(assert (forall ((k1 Int)) (exists ((i1 Int)(j1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" " + " )\n");
				
				//constraintString +=  "("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" " +  ")))))\n");
				//TEMPCODE START : Rahul Sharma
				ArrayList<String> jt = new ArrayList<String>();
				jt = createTempTableColumns(joinTable,f1,f2);
				constraintString+= generateConstraintsForAllAttributes(f1,f2,jt,joinTable) + ") ) ) )";
//				System.out.println(constraintString);
				constraintString += generateConstraintsForAllAndExistsAttributes(f1,f2,jt,joinTable,1);
				constraintString += generateConstraintsForCorrelationAttributes(cvc,f1,f2,joinTable);
				//TEMPCODE END : Rahul Sharma
				
			}
		}
	// Join Temp table implementation end
		
	//	constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}
/**
 * TEMPCODE Rahul Sharma
 * @param cvc
 * @param f1 : Table 1
 * @param f2 : Table 2
 * @param joinTable : Sub Query Table
 * @return correlation constraints
 */
private static String generateConstraintsForCorrelationAttributes(GenerateCVC1 cvc, Table f1, Table f2, String joinTable) {
		// TODO Auto-generated method stub
		String correlationConstraints = "";
		correlationConstraints += ConstraintGenerator.addCommentLine("CORRELATION CONSTRAINTS FOR SUB QUERY TABLE");
		Vector<QueryStructure> whereClauseSubqueries = cvc.getqStructure().getWhereClauseSubqueries();
		for(int i=0;i<whereClauseSubqueries.size();i++) {
			ArrayList<Node> selectionConditions = whereClauseSubqueries.get(i).getLstSelectionConditions();
			if(selectionConditions.get(i).getRight().getColumn()!=null) {
				for(int j=0;j<selectionConditions.size();j++) {
					Node selectionCondition = selectionConditions.get(j);
					String operator = selectionCondition.getOperator();
					ArrayList<String> tablesInSelectionConditions = getListOfTablesInSelectionConditions(selectionCondition.toString(),operator);
					ArrayList<String> innerTables = whereClauseSubqueries.get(i).getLstRelationInstances();
					ArrayList<String> outerTables = getOuterTables(cvc.getBaseRelation());
					if(innerTables.contains(tablesInSelectionConditions.get(0)) && outerTables.contains(tablesInSelectionConditions.get(1))) {
						correlationConstraints += generateCorrelationConstraints(cvc,selectionCondition,tablesInSelectionConditions.get(0),tablesInSelectionConditions.get(1),joinTable,operator);
					}
					else if(innerTables.contains(tablesInSelectionConditions.get(1)) && outerTables.contains(tablesInSelectionConditions.get(0))){
						correlationConstraints += generateCorrelationConstraints(cvc,selectionCondition,tablesInSelectionConditions.get(1),tablesInSelectionConditions.get(0),joinTable,operator);
					}
				}
			}
		}
		correlationConstraints += ConstraintGenerator.addCommentLine("CORRELATION CONSTRAINTS FOR SUB QUERY TABLE END");
		return correlationConstraints;
	}



	private static String getTableAttributeIndex(GenerateCVC1 cvc,String table, String attribute) {
		// TODO Auto-generated method stub
		// FIXME : 
		Vector<String> columnIndices = cvc.getTableMap().getTable(table.toUpperCase()).getColumnIndexList();
		return columnIndices.indexOf(attribute)+"";
	}



	private static String generateCorrelationConstraints(GenerateCVC1 cvc, Node selectionCondition, String innerTable, String outerTable,String joinTable,String operator) {
	// TODO Auto-generated method stub
		String constraints = "";
		String sC = selectionCondition.toString();
		innerTable = innerTable.replaceAll("\\d", "").toLowerCase();
		outerTable = outerTable.replaceAll("\\d", "").toLowerCase();
		String correlationAttribute = sC.substring(sC.indexOf(".")+1,sC.indexOf(operator));
		String innerTableIndex = getTableAttributeIndex(cvc, innerTable, correlationAttribute);
		String outerTableIndex = getTableAttributeIndex(cvc, outerTable, correlationAttribute);
		constraints += "(assert ("+operator+" ("+ joinTable+"_"+correlationAttribute+innerTableIndex+" (select O_"+joinTable+" 1)) ("+outerTable+"_"+correlationAttribute+outerTableIndex+" (select O_"+outerTable+" 1)) ))";
	return constraints;
}


	private static ArrayList<String> getOuterTables(HashMap<String, String> baseRelation) {
	// TODO Auto-generated method stub
		Iterator<Entry<String, String>> it = baseRelation.entrySet().iterator();
		ArrayList<String> tables = new ArrayList<String>();
		while(it.hasNext()) {
			Map.Entry<String, String> temp = (Map.Entry<String, String>) it.next();
			tables.add(temp.getValue());
		}
	return tables;
}


	private static ArrayList<String> getListOfTablesInSelectionConditions(String selectionCondition,String operator) {
	// TODO Auto-generated method stub
		StringTokenizer st = new StringTokenizer(selectionCondition,operator);
		String table1 = st.nextToken();
		String table2 = st.nextToken();
		table1 = table1.substring(1,table1.indexOf('.'));
		table2 = table2.substring(0,table2.indexOf('.'));
		ArrayList<String> tables = new ArrayList<String>();
		tables.add(table1);
		tables.add(table2);
	return tables;
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
					// TEMPCODE Rahul Sharma : Check if the tables are part of nested query, if so proceed further to generate sub query table constraints, otherwise break  
					boolean isPartOfSubQuery =  checkIfTablesArePartOfSubQuery(cvc,temp1,temp2);
					if(isPartOfSubQuery) {
						joinTable = temp1 + "join" + temp2;
						if(!tablesAdded.contains(joinTable)){
	//						constraintString += "\n (declare-datatypes () (("+joinTable +"_TupleType" + "("+joinTable +"_TupleType ";
							// TEMPCODE START : Rahul Sharma
							// handled incorrect parenthesis
							constraintString = "(declare-datatypes (("+joinTable +"_TupleType 0))" + "((("+joinTable +"_TupleType ";
							// TEMPCODE END : Rahul Sharma
							
							
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
							constraintString += "(declare-fun O_" + joinTable + " () (Array Int " + joinTable + "_TupleType))\n\n";
						
						
						t1Columnindex = n1.getColumn().getTable().getColumnIndex(n1.getColumn().getColumnName());
						t2Columnindex = n2.getColumn().getTable().getColumnIndex(n2.getColumn().getColumnName());
						
						ConstraintGenerator constrGen = new ConstraintGenerator();
							
						String constraint1 = constrGen.genPositiveCondsForPredF(queryBlock, n1, "i1");
						String constraint2 = constrGen.genPositiveCondsForPredF(queryBlock, n2, "j1");
												
						constraintString += "(assert (forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");
						//constraintString += "(forall ((i1 Int)(j1 Int))(=> ("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");
						
						
	//					String constraint3 = "("+joinTable+"_"+n1.getColumn().getColumnName()+t1Columnindex;
	//					constraint3 += "("+" select O_"+joinTable+" "+" k1 ) )";
						
	//					constraintString += "(exists ((k1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" "+ ") \n"); // TEMPCODE Rahul Sharma : Commented
						constraintString += "(exists ((k1 Int)) ";
						
						t2Columnindex += delimit;
	//					String constraint4 = "("+joinTable+"_"+n2.getColumn().getColumnName()+t2Columnindex;
	//					constraint4 += "("+" select O_"+joinTable+" "+" k1 ) )";
	//					
	//					constraintString += " (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" "+ "))))) )\n");
						
						//TEMPCODE START : Rahul Sharma
						ArrayList<String> jt = new ArrayList<String>();
						jt = createTempTableColumns(joinTable,f1,f2);
						constraintString+= generateConstraintsForAllAttributes(f1,f2,jt,joinTable) + ") ) ) )";
	//					System.out.println(constraintString);
						//TEMPCODE END : Rahul Sharma
						
						// TEMPCODE START : Rahul Sharma
						// commented these lines, [FIXME: this constraints leads to infinite loops]
	//					constraintString += "(assert (forall ((k1 Int)) (exists ((i1 Int)(j1 Int)) (and (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" " + " )\n");
						 
	//					constraintString += "(assert (forall ((k1 Int)) (=> (and (<= 0 k1) (<= k1 10))" 
	//							+ "(exists ((i1 Int)(j1 Int)) (and (and (<= 0 i1) (<= i1 10)) (and (<= 0 j1) (<= j1 10)) " 
	//							+ " (" + (operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint3+ (operator.equals("/=")? " )":" " + " ))\n");
						
	//					constraintString +=  "("+ (operator.equals("/=")? "not (= ": operator) +"  "+constraint2+ "  "+constraint4+ (operator.equals("/=")? " )":" " +  ")))))\n");
						constraintString += generateConstraintsForAllAndExistsAttributes(f1,f2,jt,joinTable,1);
						// TEMPCODE END : Rahul Sharma
						}
				    }
					
		}
			// Join Temp table implementation end
		
		return constraintString;
	}

	/**
	 * TEMPCODE Rahul Sharma : To check if the tables are part of sub query - to generate sub query table constraints
	 * @param queryBlock : query structure
	 * @param table1 : table1 name
	 * @param table2 : table2 name
	 * @return true if tables are a part of subquery, false otherwise
	 */
	private static boolean checkIfTablesArePartOfSubQuery(GenerateCVC1 cvc, String table1, String table2) {
		// TODO Auto-generated method stub
		Vector<QueryStructure> subqueries;
		subqueries = cvc.getqStructure().getWhereClauseSubqueries();
		for(int i=0;i<subqueries.size();i++) {
			ArrayList<String> baseRelations = subqueries.get(i).getLstRelations();
			ArrayList<String> relations = new ArrayList<String>();
			for(int j=0;j<baseRelations.size();j++) {
				relations.add(baseRelations.get(j).replaceAll("\\d", "").toLowerCase());
			}
			if(relations.contains(table1) && relations.contains(table2))
				return true;
		}
		return false;
	}


	/**
	 * TEMPCODE Rahul Sharma
	 * @param joinTable
	 * @param t1
	 * @param t2
	 * @return
	 */
	private static ArrayList<String> createTempTableColumns(String joinTable, Table t1, Table t2) {
		// TODO Auto-generated method stub
		ArrayList<String> columns = new ArrayList<String>();
		int count = 0;
		String columnName;
		HashMap<String,Column> t1_columns = t1.getColumns();
		for (Column c : t1_columns.values()) {
			columnName = joinTable+"_"+c.getColumnName()+count;
			columns.add(columnName);
			count++;
		}
		
		HashMap<String,Column> t2_columns = t2.getColumns();
		for (Column c : t2_columns.values()) {
			columnName = joinTable+"_"+c.getColumnName()+count;
			columns.add(columnName);
			count++;
		}
		return columns;
	}


	/**
	 * TEMPCODE Rahul Sharma
	 * @param f1 : Table 1
	 * @param f2 : Table 2
	 * @return : constraints with all attributes present for quantifiers [forall / exists]
	 */
	private static String generateConstraintsForAllAttributes(Table t1, Table t2,ArrayList<String> jtColumns,String jtName) {
		// TODO Auto-generated method stub
		String constraintString = "";
		int numberOfConstraints = jtColumns.size();
		String constraints[] = new String[numberOfConstraints];
		
		String t1_name = t1.getTableName().toLowerCase();
		String t2_name = t2.getTableName().toLowerCase();
		String jt_name = jtName.toLowerCase();
		
		int count = 0,count1=0;
		for(String key : t1.getColumns().keySet()) {
			constraints[count] = "(= ("+t1_name+"_"+key+count1+" (select O_"+t1_name+" i1)) ("+jtColumns.get(count)+ " (select O_"+jt_name+" k1)))";
//			System.out.println(constraints[count]);
			constraintString+=constraints[count];
			count++;
			count1++;
		}
		
		int count2 = 0;
		for(String key : t2.getColumns().keySet()) {
			constraints[count] = "(= ("+t2_name+"_"+key+count2+" (select O_"+t2_name+" j1)) ("+jtColumns.get(count)+ " (select O_"+jt_name+" k1)))";
//			System.out.println(constraints[count]);
			constraintString+=constraints[count];
			count++;
			count2++;
		}
		
//		System.out.println(jt.getTableName());
//		for(String key : jt.getColumns().keySet()) {
//			ColName = jt.getColumns().get(key).getColumnName();
//			System.out.println(ColName);
//		}
		
		int index = 0;
		String finalConstraints = constraints[index++];
		while(index < numberOfConstraints) {
			finalConstraints = "(and "+finalConstraints+" "+constraints[index++]+")";
		}
//		System.out.println(finalConstraints);
		return finalConstraints;
	}

	
	/**
	 * TEMPCODE Rahul Sharma
	 * @param f1 : Table 1
	 * @param f2 : Table 2
	 * @return : constraints with all attributes present for quantifiers [forall / exists]
	 */
	private static String generateConstraintsForAllAndExistsAttributes(Table t1, Table t2,ArrayList<String> jtColumns,String jtName,int noOfOutputTuples) {
		// TODO Auto-generated method stub
		
//		System.out.println(cvc.getNoOfOutputTuples().toString());
//		System.out.println("********");
//		System.out.println(cvc.getNoOfTuples().toString());
		
		String constraintString = "";
		int numberOfConstraints = jtColumns.size();
		String constraints[] = new String[numberOfConstraints];
		
		String t1_name = t1.getTableName().toLowerCase();
		String t2_name = t2.getTableName().toLowerCase();
		String jt_name = jtName.toLowerCase();
		String table1pk = "";
		String table2pk = "";
		int count = 0,count1=0;
		for(String key : t1.getColumns().keySet()) {
			constraints[count] = "(= ("+t1_name+"_"+key+count1+" (select O_"+t1_name+" j1)) ("+jtColumns.get(count)+ " (select O_"+jt_name+" replaceIndex)))";
//			System.out.println(constraints[count]);
			constraintString+=constraints[count];
			if(key.equalsIgnoreCase(t1.getPrimaryKey().elementAt(0).toString())) {
				table1pk = constraints[count];
			}
			count++;
			count1++;
		}
		
		int count2 = 0;
		for(String key : t2.getColumns().keySet()) {
			constraints[count] = "(= ("+t2_name+"_"+key+count2+" (select O_"+t2_name+" k1)) ("+jtColumns.get(count)+ " (select O_"+jt_name+" replaceIndex)))";
//			System.out.println(constraints[count]);
			constraintString+=constraints[count];
			if(key.equalsIgnoreCase(t2.getPrimaryKey().elementAt(0).toString())) {
				table2pk = constraints[count];
			}
			count++;
			count2++;
		}
		
//		System.out.println(constraintString);
//		System.out.println(jt.getTableName());
//		for(String key : jt.getColumns().keySet()) {
//			ColName = jt.getColumns().get(key).getColumnName();
//			System.out.println(ColName);
//		}
//		String finalConstraints = "(assert (forall ((i1 Int)) (exists ((j1 Int)(k1 Int)) (=>";
		String finalConstraints = "";
		
		
		for(int replaceIndex=1;replaceIndex<=noOfOutputTuples;replaceIndex++) {
			String tempConstraints = "(assert (exists ((j1 Int)(k1 Int)) (=>";
			
			tempConstraints += "(and "+ table1pk + " " + table2pk +")";
			finalConstraints +="\n"+tempConstraints;
			tempConstraints = "";
			int index = 0;
			tempConstraints += constraints[index++];
			while(index < numberOfConstraints) {
				tempConstraints = "(and "+tempConstraints+" "+constraints[index++]+")";
			}
			tempConstraints += ") ) )";
//			tempConstraints = tempConstraints.replaceAll("replaceIndex", replaceIndex+"");
//			System.out.println(tempConstraints);
			finalConstraints +=tempConstraints + "\n";
			finalConstraints = finalConstraints.replaceAll("replaceIndex", replaceIndex+"");
		}
		
//		System.out.println(finalConstraints);
		return finalConstraints;
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
