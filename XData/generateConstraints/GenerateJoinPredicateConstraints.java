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
			Table table1,table2;

			String temp1,temp2,joinTable,ColName;
			int t1Columnindex, t2Columnindex;
			int findex = 0;
			table1 = n1.getTable();
			table2 = n2.getTable();
			temp1 = table1.getTableName();
			temp2 = table2.getTableName();
			// TEMPCODE Rahul Sharma : Check if the tables are part of nested query, if so proceed further to generate sub query table constraints, otherwise break  
			boolean isPartOfSubQuery =  checkIfTablesArePartOfSubQuery(cvc,temp1,temp2);

			if(isPartOfSubQuery){
				try {
					ConstraintGenerator constrGen = new ConstraintGenerator();
					/** Commented lines 480:482; join and conditions conditions should be enforced on tempjoin table **/
					//String constraint1 = constrGen.genPositiveCondsForPred(queryBlock,n1, 1 );
					//String constraint2 = constrGen.genPositiveCondsForPred(queryBlock,n2, 1 );
					//constraintString += "(assert ("+(operator.equals("/=")? "not (= ": operator) +"  "+constraint1+ "  "+constraint2+ (operator.equals("/=")? " ) )":" ")+") )\n";

					joinTable = temp1 + "join" + temp2;
					if(!tablesAdded.contains(joinTable)){
						constraintString += "\n(declare-datatypes (("+joinTable +"_TupleType 0))" + "((("+joinTable +"_TupleType "; // TEMPCODE Rahul Sharma : fixed syntax error
					}

					for(String key : table1.getColumns().keySet()) {
						ColName = table1.getColumns().get(key).getColumnName();
						String s = table1.getColumns().get(key).getCvcDatatype();
						if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
							constraintString += "("+joinTable+"_"+table1.getColumns().get(key)+findex+" "+s + ") ";
						else {
							constraintString += "("+joinTable+"_"+table1.getColumns().get(key)+findex+" "+ColName + ") "; // TEMPCODE Rahul Sharma : Changed ColName to s (datatype of column)
						}							
						findex++;
					}
					int delimit = findex;
					for(String key : table2.getColumns().keySet()) {
						ColName = table2.getColumns().get(key).getColumnName();
						String s = table2.getColumns().get(key).getCvcDatatype();
						if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
							constraintString += "("+joinTable+"_"+table2.getColumns().get(key)+findex+" "+s + ") ";
						else {
							//constraintString += "("+joinTable+"_"+f2.getColumns().get(key)+findex+" "+ColName + ") ";
							constraintString += "("+joinTable+"_"+table2.getColumns().get(key)+findex+" "+ s + ") "; // TEMPCODE Rahul Sharma : Changed ColName to s (datatype of column)
						}
						findex++;
					}
					constraintString += ") )) )\n";
					
					//Now create the Array for this TupleType
					constraintString += "(declare-fun O_" + joinTable + "() (Array Int " + joinTable + "_TupleType))\n\n";


					t1Columnindex	= n1.getColumn().getTable().getColumnIndex(n1.getColumn().getColumnName());
					t2Columnindex	= n2.getColumn().getTable().getColumnIndex(n2.getColumn().getColumnName());

					// selection and join conditions from subquery :POOJA
					String constr="";
					String strConstr = "";

					ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
					Vector<Node> selectionAndJoinConds = new Vector<Node>();
					Vector<Node> correlationConds = new Vector<Node>();
					for(ConjunctQueryStructure cqs : queryBlock.getConjunctsQs()) {

						// SELECTION conditions
						Vector<Node> selConds = cqs.getSelectionConds();
						for(Node n: selConds) {
							if(!isCorrelated(n,cvc)) {
								selectionAndJoinConds.add(n);

								String s1 = constrGen.genPositiveCondsForPredF(queryBlock, n.getLeft(), "i1");
								String s2 = constrGen.genPositiveCondsForPredF(queryBlock, n.getRight(), "j1");
								constr += "\t\t ("+ (operator.equals("/=")? "not (= ": operator) +"  "+s1+ "  "+s2+ (operator.equals("/=")? " ) )":" "+ ")")+ "\n";

								//ConstraintObject constrnObj = new ConstraintObject();
								//constrnObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getLeft(), 1 ));
								//constrnObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getRight(),1 ));
								//constrnObj.setOperator(n.getOperator());
								//constrList.add(constrnObj);
							}
							else
								correlationConds.add(n);

						}

						// JOIN conditions
						Vector<Node> joinConds = cqs.getJoinCondsForEquivalenceClasses();
						for(Node n: joinConds) {
							if(!isCorrelated(n,cvc)) {
								selectionAndJoinConds.add(n);

								String s1 = constrGen.genPositiveCondsForPredF(queryBlock, n.getLeft(), "i1");
								String s2 = constrGen.genPositiveCondsForPredF(queryBlock, n.getRight(), "j1");
								constr += "\t\t ("+ (operator.equals("/=")? "not (= ": operator) +"  "+s1+ "  "+s2+ (operator.equals("/=")? " ) )":" "+ ")")+ " \n";

								ConstraintObject constrnObj = new ConstraintObject();
								constrnObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getLeft(), 1 ));
								constrnObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getRight(),1 ));
								constrnObj.setOperator(n.getOperator());
								constrList.add(constrnObj);
							}
							else
								correlationConds.add(n);
						}
						//STRING SELECTION conditions
						Vector<Node> stringSelectionConds = cqs.getStringSelectionConds();
						Vector<String> stringConstraints = new Vector<String>();

						for(int k=0; k<stringSelectionConds.size(); k++){
							//selectionAndJoinConds.add(stringSelectionConds.get(k));

							//int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
							//int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
							//for(int l=1;l<=count;l++){
							//strConstr = constrGen.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1);
							/*ConstraintObject constObj = new ConstraintObject();
	    						constObj.setLeftConstraint(stringConstraint);
	    						constrObjList.add(constObj);*/
							//stringConstraints.add(strConstr);
							//}

							stringConstraints.clear();
							String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
							strConstr = constrGen.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),1000); //FIXME: passing a temporary offset i.e. the last argument in this function call

							stringConstraints.add(strConstr);
							String tableName = stringSelectionConds.get(k).getLeft().getTableNameNo().replaceAll("\\d","").toLowerCase();
							strConstr = "";
							Vector<String> solvedStringConstraint=cvc.getStringSolver().solveConstraints(stringConstraints, cvc.getResultsetColumns(), cvc.getTableMap(), true);
							for(String str:solvedStringConstraint)	{
								str = str.substring(str.indexOf('(')+1, str.lastIndexOf(')'));
								strConstr +="\t\t"+ str.replace("assert","")+"\n";
								strConstr = tableName.equals(table1) ? strConstr.replace("1000","i1"): strConstr.replace("1000","j1") ;
							}
							constr += strConstr;

						}
					}
					
					//constr += constrGen.generateANDConstraints(constrList)+ "\n \t" + strConstr;
					constr = "(and \n" + constr + "\t\t)";


					ArrayList<String> jtColumns = new ArrayList<String>();
					jtColumns = createTempTableColumns(joinTable,table1,table2);

					constraintString += "(assert (forall ((i1 Int)(j1 Int)) \n\t(=> "+constr +"  ";
					constraintString += "\n \t(exists ((k1 Int)) "; // TEMPCODE : Rahul Sharma : added all other attributes
					constraintString+= "\n\t"+generateConstraintsForAllAttributes(table1,table2,jtColumns,joinTable) + ") \n) \n) \n)\n";

					cvc.tempJoinDefine.put(joinTable, constraintString);
					cvc.tempJoinColumns.put(joinTable, jtColumns);
					cvc.tempJoinSelectionAndJoinConds.put(joinTable, selectionAndJoinConds);
					cvc.tempJoinCorrelationConds.put(joinTable, correlationConds);
					/*
					 * Commented function calls at 622 and 623
					 * returning empty string coz the datatype declaration, and other constraints on tempjoin table are added in the end i.e. along with header and footer
					 */
					//constraintString += generateConstraintsForAllAndExistsAttributes(n1, n2, table1,table2,jtColumns,joinTable,1,selectionAndJoinConds)+"\n";
					//constraintString += generateConstraintsForCorrelationAttributes(cvc, joinTable)+"\n";
				}
				catch(Exception e){
					System.out.println(e);
				}
				return ""; 
			}
			// Join Temp table implementation end
			
			else { 
				/** TEST CODE: Pooja; Check if the tables are part of nested query, if so proceed further to generate sub query table constraints, otherwise break **/

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
		}
		//constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}
	/**
	 * TEST CODE: POOJA
	 */
	public static String getConstraintsForJoinsForNotExists(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2, String operator) {
		String constraintString = "";
		
		Table table1,table2;
		String temp1,temp2,joinTable,ColName;
		int t1Columnindex, t2Columnindex;
		int findex = 0;
		table1 = n1.getTable();
		table2 = n2.getTable();
		temp1 = table1.getTableName();
		temp2 = table2.getTableName();
		try {
			
			ConstraintGenerator constrGen = new ConstraintGenerator();
			joinTable = temp1 + "join" + temp2;
			if(!tablesAdded.contains(joinTable)){
				constraintString += "\n(declare-datatypes (("+joinTable +"_TupleType 0))" + "((("+joinTable +"_TupleType "; // TEMPCODE Rahul Sharma : fixed syntax error
			}

			for(String key : table1.getColumns().keySet()) {
				ColName = table1.getColumns().get(key).getColumnName();
				String s = table1.getColumns().get(key).getCvcDatatype();
				if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
					constraintString += "("+joinTable+"_"+table1.getColumns().get(key)+findex+" "+s + ") ";
				else {
					constraintString += "("+joinTable+"_"+table1.getColumns().get(key)+findex+" "+ColName + ") "; // TEMPCODE Rahul Sharma : Changed ColName to s (datatype of column)
				}							
				findex++;
			}
			int delimit = findex;
			for(String key : table2.getColumns().keySet()) {
				ColName = table2.getColumns().get(key).getColumnName();
				String s = table2.getColumns().get(key).getCvcDatatype();
				if(s!= null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
					constraintString += "("+joinTable+"_"+table2.getColumns().get(key)+findex+" "+s + ") ";
				else {
					constraintString += "("+joinTable+"_"+table2.getColumns().get(key)+findex+" "+ s + ") "; // TEMPCODE Rahul Sharma : Changed ColName to s (datatype of column)
				}
				findex++;
			}
			constraintString += ") )) )\n";
			
			//Now create the Array for this TupleType
			constraintString += "(declare-fun O_" + joinTable + "() (Array Int " + joinTable + "_TupleType))\n\n";


			t1Columnindex	= n1.getColumn().getTable().getColumnIndex(n1.getColumn().getColumnName());
			t2Columnindex	= n2.getColumn().getTable().getColumnIndex(n2.getColumn().getColumnName());

			// selection and join conditions from subquery :POOJA
			String constr="";
			String strConstr = "";

			ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
			Vector<Node> selectionAndJoinConds = new Vector<Node>();
			Vector<Node> correlationConds = new Vector<Node>();
			for(ConjunctQueryStructure cqs : queryBlock.getConjunctsQs()) {

				// SELECTION conditions
				Vector<Node> selConds = cqs.getSelectionConds();
				for(Node n: selConds) {
					if(!isCorrelated(n,cvc)) {
						selectionAndJoinConds.add(n);

						String s1 = constrGen.genPositiveCondsForPredF(queryBlock, n.getLeft(), "i1");
						String s2 = constrGen.genPositiveCondsForPredF(queryBlock, n.getRight(), "j1");
						constr += "\t\t ("+ (operator.equals("/=")? "not (= ": operator) +"  "+s1+ "  "+s2+ (operator.equals("/=")? " ) )":" "+ ")")+ "\n";

						//ConstraintObject constrnObj = new ConstraintObject();
						//constrnObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getLeft(), 1 ));
						//constrnObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getRight(),1 ));
						//constrnObj.setOperator(n.getOperator());
						//constrList.add(constrnObj);
					}
					else
						correlationConds.add(n);
				}

				// JOIN conditions
				Vector<Node> joinConds = cqs.getJoinCondsForEquivalenceClasses();
				for(Node n: joinConds) {
					if(!isCorrelated(n,cvc)) {
						selectionAndJoinConds.add(n);

						String s1 = constrGen.genPositiveCondsForPredF(queryBlock, n.getLeft(), "i1");
						String s2 = constrGen.genPositiveCondsForPredF(queryBlock, n.getRight(), "j1");
						constr += "\t\t ("+ (operator.equals("/=")? "not (= ": operator) +"  "+s1+ "  "+s2+ (operator.equals("/=")? " ) )":" "+ ")")+ " \n";

						ConstraintObject constrnObj = new ConstraintObject();
						constrnObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getLeft(), 1 ));
						constrnObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,n.getRight(),1 ));
						constrnObj.setOperator(n.getOperator());
						constrList.add(constrnObj);
					}
					else
						correlationConds.add(n);
				}
				//STRING SELECTION conditions
				Vector<Node> stringSelectionConds = cqs.getStringSelectionConds();
				Vector<String> stringConstraints = new Vector<String>();

				for(int k=0; k<stringSelectionConds.size(); k++){
					//selectionAndJoinConds.add(stringSelectionConds.get(k));

					//int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];
					//int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
					//for(int l=1;l<=count;l++){
					//strConstr = constrGen.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1);
					/*ConstraintObject constObj = new ConstraintObject();
						constObj.setLeftConstraint(stringConstraint);
						constrObjList.add(constObj);*/
					//stringConstraints.add(strConstr);
					//}

					stringConstraints.clear();
					String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
					strConstr = constrGen.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),1000); //FIXME: passing a temporary offset i.e. the last argument in this function call

					stringConstraints.add(strConstr);
					String tableName = stringSelectionConds.get(k).getLeft().getTableNameNo().replaceAll("\\d","").toLowerCase();
					strConstr = "";
					Vector<String> solvedStringConstraint=cvc.getStringSolver().solveConstraints(stringConstraints, cvc.getResultsetColumns(), cvc.getTableMap(), true);
					for(String str:solvedStringConstraint)	{
						str = str.substring(str.indexOf('(')+1, str.lastIndexOf(')'));
						strConstr +="\t\t"+ str.replace("assert","")+"\n";
						strConstr = tableName.equals(table1) ? strConstr.replace("1000","i1"): strConstr.replace("1000","j1") ;
					}
					constr += strConstr;

				}
			}
			
			//constr += constrGen.generateANDConstraints(constrList)+ "\n \t" + strConstr;
			constr = "(and \n" + constr + "\t\t)";


			ArrayList<String> jtColumns = new ArrayList<String>();
			jtColumns = createTempTableColumns(joinTable,table1,table2);

//			constraintString += "(assert (forall ((i1 Int)(j1 Int)) \n\t(=> "+constr +"  ";
//			constraintString += "\n \t(exists ((k1 Int)) "; // TEMPCODE : Rahul Sharma : added all other attributes
//			constraintString+= "\n\t"+generateConstraintsForAllAttributes(table1,table2,jtColumns,joinTable) + ") \n) \n) \n)\n";
//
//			cvc.tempJoinDefine.put(joinTable, constraintString);
//			cvc.tempJoinColumns.put(joinTable, jtColumns);
//			cvc.tempJoinSelectionAndJoinConds.put(joinTable, selectionAndJoinConds);
			
			if(!correlationConds.isEmpty()) {
				//in case of NOT EXISTS negate correlation conditions so that there is no tuple in subquery Table
				for(Node n: correlationConds) {
					String complementOp = Node.complimentOperator(n.getOperator());
					n.setOperator(complementOp);
				}
				constraintString += "(assert (forall ((i1 Int)(j1 Int)) \n\t(=> "+constr +"  ";
				constraintString += "\n \t(exists ((k1 Int)) "; // TEMPCODE : Rahul Sharma : added all other attributes
				constraintString+= "\n\t"+generateConstraintsForAllAttributes(table1,table2,jtColumns,joinTable) + ") \n) \n) \n)\n";

				cvc.tempJoinDefine.put(joinTable, constraintString);
				cvc.tempJoinColumns.put(joinTable, jtColumns);
				cvc.tempJoinSelectionAndJoinConds.put(joinTable, selectionAndJoinConds);
				cvc.tempJoinCorrelationConds.put(joinTable, correlationConds);
			}
			else {
				//TODO: Add new Code
				constraintString += "(assert (not (exists ((i1 Int)(j1 Int)) \n\t "+constr +"  \n\t) \n) )\n";

				cvc.tempJoinDefine.put(joinTable, constraintString);
				cvc.tempJoinColumns.put(joinTable,new ArrayList<String>());
			}
			/**If there are no correlation conditions then there is no need of creating a subquery table
			 * hence not storing selection conditions, join table columns etc */
//			cvc.tempJoinCorrelationConds.put(joinTable, correlationConds);
//			cvc.tempJoinColumns.put(joinTable, jtColumns);
//			cvc.tempJoinSelectionAndJoinConds.put(joinTable, selectionAndJoinConds);
		}
		catch(Exception e){
			System.out.println(e);
		}
		return ""; 
	}
	
/**
 * TEMPCODE Rahul Sharma
 * @param cvc
 * @param f1 : Table 1
 * @param f2 : Table 2
 * @param joinTable : Sub Query Table
 * @return correlation constraints
 */
	public static String generateConstraintsForCorrelationAttributes(GenerateCVC1 cvc, String joinTable, Vector<Node> correlationConds) {
		// TODO Auto-generated method stub
		String correlationConstraints = "";
		correlationConstraints += ConstraintGenerator.addCommentLine("CORRELATION CONSTRAINTS FOR SUB QUERY TABLE");
		if(correlationConds == null || correlationConds.isEmpty())
			return "";
		ArrayList<String> outerTables = getOuterTables(cvc.getBaseRelation());
		
		for(Node n:correlationConds) {
			String table1 = n.getLeft().getTableNameNo();
			String table2 = n.getRight().getTableNameNo();
			String operator = n.getOperator();
			//String innerTable,outerTable;
			if(outerTables.contains(table1)) {
				correlationConstraints += generateCorrelationConstraints(cvc,n,table2,table1,joinTable,operator);
			}
			else if(outerTables.contains(table2)){
				correlationConstraints += generateCorrelationConstraints(cvc,n,table1,table2,joinTable,operator);
			}
		}
//		Vector<QueryStructure> whereClauseSubqueries = cvc.getqStructure().getWhereClauseSubqueries();
//		for(int i=0;i<whereClauseSubqueries.size();i++) {
//			ArrayList<Node> selectionConditions = whereClauseSubqueries.get(i).getLstSelectionConditions();
//
//			for(int j=0;j<selectionConditions.size();j++) {
//				//System.out.println(selectionConditions.get(j).getRight().getColumn());
//				if(selectionConditions.get(j).getRight().getColumn()!=null) {
//					Node selectionCondition = selectionConditions.get(j);
//					String operator = selectionCondition.getOperator();
//					ArrayList<String> tablesInSelectionConditions = getListOfTablesInSelectionConditions(selectionCondition.toString(),operator);
//					ArrayList<String> innerTables = whereClauseSubqueries.get(i).getLstRelationInstances();
//					ArrayList<String> outerTables = getOuterTables(cvc.getBaseRelation());
//					if(innerTables.contains(tablesInSelectionConditions.get(0)) && outerTables.contains(tablesInSelectionConditions.get(1))) {
//						correlationConstraints += generateCorrelationConstraints(cvc,selectionCondition,tablesInSelectionConditions.get(0),tablesInSelectionConditions.get(1),joinTable,operator);
//					}
//					else if(innerTables.contains(tablesInSelectionConditions.get(1)) && outerTables.contains(tablesInSelectionConditions.get(0))){
//						correlationConstraints += generateCorrelationConstraints(cvc,selectionCondition,tablesInSelectionConditions.get(1),tablesInSelectionConditions.get(0),joinTable,operator);
//					}
//				}
//			}
//		}
		correlationConstraints += ConstraintGenerator.addCommentLine("CORRELATION CONSTRAINTS FOR SUB QUERY TABLE END");
		return correlationConstraints;
	}

   public static boolean isCorrelated(Node selectionCondition, GenerateCVC1 cvc) {
    	if(selectionCondition.getRight().getColumn()!=null) {
    		String operator = selectionCondition.getOperator();
    		ArrayList<String> tablesInSelectionConditions = getListOfTablesInSelectionConditions(selectionCondition.toString(),operator);
    		ArrayList<String> innerTables = cvc.getqStructure().getWhereClauseSubqueries().get(0).getLstRelationInstances();
    		ArrayList<String> outerTables = getOuterTables(cvc.getBaseRelation());
    		if(innerTables.contains(tablesInSelectionConditions.get(0)) && outerTables.contains(tablesInSelectionConditions.get(1))) {
    			return true;
    		}
    		else if(innerTables.contains(tablesInSelectionConditions.get(1)) && outerTables.contains(tablesInSelectionConditions.get(0))){
    			return true;
    		}
    	}
    	return false;
    }

	private static String getTableAttributeIndex(GenerateCVC1 cvc,String table, String attribute) {
		// TODO Auto-generated method stub
		Vector<String> columnIndices = cvc.getTableMap().getTable(table.toUpperCase()).getColumnIndexList();
		return columnIndices.indexOf(attribute)+"";
	}
	private static String getTableAttributeIndexForJoinTable(GenerateCVC1 cvc,String joinTable,String innerTable, String attribute) {
		// TODO Auto-generated method stub

		String[] tblList = joinTable.split("join");
		int index = 0;
		try {
			for(int i=0; i<tblList.length; i++) {
				String tbl = tblList[i];
				Vector<String> columnIndices = cvc.getTableMap().getTable(tbl.toUpperCase()).getColumnIndexList();
				if(tbl.equals(innerTable)) {
					index += columnIndices.indexOf(attribute);
					break;
				}	
				else
					index += cvc.getTableMap().getTable(tbl.toUpperCase()).getNoOfColumn();
			}
		}
		catch(Exception e) {
			System.out.println(e);
		}
		return ""+index;
	}


	private static String generateCorrelationConstraints(GenerateCVC1 cvc, Node selectionCondition, String innerTable, String outerTable,String joinTable,String operator) {
	// TODO Auto-generated method stub
		String constraints = "";
		String sC = selectionCondition.toString();
		innerTable = innerTable.replaceAll("\\d", "").toLowerCase();
		outerTable = outerTable.replaceAll("\\d", "").toLowerCase();
		
		String correlationAttribute = sC.substring(sC.indexOf(".")+1,sC.indexOf(operator));
		String joinTableIndex = getTableAttributeIndexForJoinTable(cvc, joinTable, innerTable, correlationAttribute);
		String outerTableIndex = getTableAttributeIndex(cvc, outerTable, correlationAttribute);
		
//		int tuplesInJoinTable, tuplesInOuterTable;
//		String t1_name = joinTable.split("join")[0].toLowerCase();
//		String t2_name = joinTable.split("join")[1].toLowerCase();
//		tuplesInJoinTable = cvc.getNoOfOutputTuples(t1_name)*cvc.getNoOfOutputTuples(t2_name);
//		tuplesInOuterTable = cvc.getNoOfOutputTuples(outerTable);
//		
//		for(int i=1; i<= tuplesInJoinTable; i++) {
//			for(int j=1; j<= tuplesInOuterTable; j++) {
//				if(operator.equals("/="))
//					constraints += "\n\t(not (= ("+ joinTable+"_"+correlationAttribute+joinTableIndex+" (select O_"+joinTable+" "+ i +")) ("+outerTable+"_"+correlationAttribute+outerTableIndex+" (select O_"+outerTable+" "+ j +")) ))";
//				else
//					constraints += "\n\t("+operator+" ("+ joinTable+"_"+correlationAttribute+joinTableIndex+" (select O_"+joinTable+" "+ i +")) ("+outerTable+"_"+correlationAttribute+outerTableIndex+" (select O_"+outerTable+" "+ j +")) )";
//			}
//		}
		//int offset1 = cvc.getRepeatedRelNextTuplePos().get(outerTable)[1];
		
		if(operator.equals("/="))
			constraints += "\n\t(not (= ("+ joinTable+"_"+correlationAttribute+joinTableIndex+" (select O_"+joinTable+" 1)) ("+outerTable+"_"+correlationAttribute+outerTableIndex+" (select O_"+outerTable+" 1)) ))";
		else
			constraints += "\n\t("+operator+" ("+ joinTable+"_"+correlationAttribute+joinTableIndex+" (select O_"+joinTable+" 1)) ("+outerTable+"_"+correlationAttribute+outerTableIndex+" (select O_"+outerTable+" 1)) )";
		
		//assuming join of two tables only
		if(operator.equals("/="))
			constraints = "(assert (and "+ constraints + "\n))";
		else
			constraints = "(assert (or "+ constraints + "\n))";
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
						constraintString += generateConstraintsForAllAndExistsAttributes(n1, n2, f1,f2,jt,joinTable,1,new Vector<Node>());
						// TEMPCODE END : Rahul Sharma
						}
				    }
					else {
						//TEST CODE: Pooja

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
				//relations.add(baseRelations.get(j).replaceAll("\\d", "").toUpperCase());
			}
			if(relations.contains(table1) && relations.contains(table2) && !table1.equals(table2))
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
		
//		String t1_name = t1.getTableName().toUpperCase();
//		String t2_name = t2.getTableName().toUpperCase();
//		String jt_name = jtName.toUpperCase();
		
		int count = 0,count1=0;
		for(String key : t1.getColumns().keySet()) {
			constraints[count] = "(= ("+t1_name+"_"+key+count1+" (select O_"+t1_name+" i1)) ("+jtColumns.get(count)+ " (select O_"+jt_name+" k1)))";
			constraintString+=constraints[count];
			count++;
			count1++;
		}
		
		int count2 = 0;
		for(String key : t2.getColumns().keySet()) {
			constraints[count] = "(= ("+t2_name+"_"+key+count2+" (select O_"+t2_name+" j1)) ("+jtColumns.get(count)+ " (select O_"+jt_name+" k1)))";
			constraintString+=constraints[count];
			count++;
			count2++;
		}
		
		
		int index = 0;
		String finalConstraints = constraints[index++];
		while(index < numberOfConstraints) {
			finalConstraints = finalConstraints+"\n\t"+constraints[index++];
		}
		finalConstraints = "(and \n\t"+finalConstraints + "\n)";
		return finalConstraints;
	}

	
	/**
	 * TEMPCODE Rahul Sharma
	 * @param f1 : Table 1
	 * @param f2 : Table 2
	 * @return : constraints with all attributes present for quantifiers [forall / exists]
	 */
	private static String generateConstraintsForAllAndExistsAttributes(Node n1, Node n2, Table t1, Table t2,ArrayList<String> jtColumns,String jtName,int tuplesInJoinTable,Vector<Node> selectionAndJoinConds) {
		// TODO Auto-generated method stub
		
		int tuplesInTable1 =2;
		int tuplesInTable2 =3;
		tuplesInJoinTable = 1;
		
		String t1_name = t1.getTableName().toLowerCase();
		String t2_name = t2.getTableName().toLowerCase();
		jtName = jtName.toLowerCase();
		String constraintString = "";
		String finalConstraints = "";
		for(int k=1; k<= tuplesInJoinTable; k++) {
			String constr = "\t";
			//selection conditions, Skipping string selection conditions for now FIXME
			for(Node n:selectionAndJoinConds) {
				constr += generateConstraintsForSelectionConditions(n, jtColumns, jtName, t1, t2, k)+"\n\t";
			}

			String constrTable1 = "";
			int count = 0;
			for(int i=1; i<= tuplesInTable1; i++) {
				String temp = "\t\t";
				int count1=0;
				int countJT = count;
				for(String key : t1.getColumns().keySet()) {
					temp += "(= ("+t1_name+"_"+key+count1+" (select O_"+t1_name+" "+i+")) ("+jtColumns.get(countJT)+ " (select O_"+jtName+" "+k+")))\n\t\t";
					countJT++;
					count1++;
				}
				temp = "\n\t  (and \n"+temp+"\n\t  )";
				constrTable1 += temp;
			}
			constrTable1 = "\t(or "+constrTable1+"\n\t)";

			count = t1.getNoOfColumn();
			String constrTable2 = "";
			for(int j=1; j<= tuplesInTable2; j++) {
				String temp = "\t\t";
				int count2=0;
				int countJT = count;
				for(String key : t2.getColumns().keySet()) {
					temp += "(= ("+t2_name+"_"+key+count2+" (select O_"+t2_name+" "+j+")) ("+jtColumns.get(countJT)+ " (select O_"+jtName+" "+k+")))\n\t\t";
					countJT++;
					count2++;
				}
				temp = "\n\t  (and \n"+temp+"\n\t  )";
				constrTable2 += temp;
			}
			constrTable2 = "\t(or "+constrTable2+"\n\t)";

			finalConstraints += "(and \n"+constr+"\n"+constrTable1+"\n"+constrTable2+"\n  )\n  ";
		}
		finalConstraints = "(assert \n (and \n  "+finalConstraints+"\n )\n)";
		
		return finalConstraints;
	}
	//***************************** TEST CODE: POOJA ****************/
	public static String generateConstraintsForAllAndExistsAttributesNew(GenerateCVC1 cvc, ArrayList<String> jtColumns,String jtName) {
		// TODO Auto-generated method stub
		
		if(jtColumns == null || jtColumns.isEmpty())
			return "";
		jtName = jtName.toLowerCase();
		String t1_name = jtName.split("join")[0].toLowerCase();
		String t2_name = jtName.split("join")[1].toLowerCase();
		
		Table t1 = cvc.getTableMap().getTable(t1_name.toUpperCase());
		Table t2 = cvc.getTableMap().getTable(t2_name.toUpperCase());
		
		int tuplesInTable1 = cvc.getNoOfOutputTuples(t1_name);
		int tuplesInTable2 =cvc.getNoOfOutputTuples(t2_name);
		int tuplesInJoinTable = tuplesInTable1*tuplesInTable2;
		
		Vector<Node> selectionAndJoinConds = cvc.tempJoinSelectionAndJoinConds.get(jtName);
		
		String finalConstraints = "";
		for(int k=1; k<= tuplesInJoinTable; k++) {
			String constr = "\t";
			//selection conditions, Skipping string selection conditions for now FIXME
			for(Node n:selectionAndJoinConds) {
				constr += generateConstraintsForSelectionConditions(n, jtColumns, jtName, t1, t2, k)+"\n\t";
			}
			
			String constrTable1 = "";
			int count = 0;
			for(int i=1; i<= tuplesInTable1; i++) {
				String temp = "\t\t";
				int count1=0;
				int countJT = count;
				for(String key : t1.getColumns().keySet()) {
					temp += "(= ("+t1_name+"_"+key+count1+" (select O_"+t1_name+" "+i+")) ("+jtColumns.get(countJT)+ " (select O_"+jtName+" "+k+")))\n\t\t";
					countJT++;
					count1++;
				}
				temp = "\n\t  (and \n"+temp+"\n\t  )";
				constrTable1 += temp;
			}
			constrTable1 = "\t(or "+constrTable1+"\n\t)";

			count = t1.getNoOfColumn();
			String constrTable2 = "";
			for(int j=1; j<= tuplesInTable2; j++) {
				String temp = "\t\t";
				int count2=0;
				int countJT = count;
				for(String key : t2.getColumns().keySet()) {
					temp += "(= ("+t2_name+"_"+key+count2+" (select O_"+t2_name+" "+j+")) ("+jtColumns.get(countJT)+ " (select O_"+jtName+" "+k+")))\n\t\t";
					countJT++;
					count2++;
				}
				temp = "\n\t  (and \n"+temp+"\n\t  )";
				constrTable2 += temp;
			}
			constrTable2 = "\t(or "+constrTable2+"\n\t)";

			finalConstraints += "(and \n"+constr+"\n"+constrTable1+"\n"+constrTable2+"\n  )\n  ";
		}
		finalConstraints = "(assert \n (and \n  "+finalConstraints+"\n )\n)";
		
		return finalConstraints;
	}

	/********************** TESE CODE END ********************/
	public static String generateConstraintsForSelectionConditions(Node n, ArrayList<String> jtColumns,String jtName,Table t1, Table t2, int tupleId) {
		String constr = "";
		String left = n.getLeft().getTableNameNo() !=null ? (n.getLeft().getTableNameNo().replaceAll("\\d", "")).toLowerCase() : null;
		String right = n.getRight().getTableNameNo()!= null ? (n.getRight().getTableNameNo().replaceAll("\\d", "")).toLowerCase() : null;
		int l_index=-1,r_index=-1;
		
		System.out.println(left+"\t"+right);
		
		if(n.getLeft() != null && n.getLeft().getColumn() != null) {
			if(t1.getTableName().equals(left)) 
				l_index = t1.getColumnIndex(n.getLeft().getColumn().getColumnName());
			else 
				l_index = t1.getNoOfColumn() + t2.getColumnIndex(n.getLeft().getColumn().getColumnName());
		}
		if(n.getRight() != null && n.getRight().getColumn() != null) {
			if(t1.getTableName().equals(right)) 
				r_index = t1.getColumnIndex(n.getRight().getColumn().getColumnName());
			else 
				r_index = t1.getNoOfColumn() + t2.getColumnIndex(n.getRight().getColumn().getColumnName());
		}
		if(l_index != -1 && r_index != -1) {
			constr = "( "+jtColumns.get(l_index)+" (select O_"+jtName+" "+tupleId+" )) ";
			constr += "( "+jtColumns.get(r_index)+" (select O_"+jtName+" "+tupleId+" )) ";
		}
		else {
			constr = "( "+jtColumns.get(l_index)+" (select O_"+jtName+" "+tupleId+" )) ";
			constr += n.getRight().getStrConst();
		}
		
		if(n.getOperator().equals("/="))
			return "(not (= "+constr + ") )";
		return "("+n.getOperator()+" "+constr+" )";
		
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
			checkRepeatedRelations(cvc,cvc.cloneNoOfOutputTuples()); // TEMPCODE Rahul Sharma  to handle repeated relations
			for(int i = 1; i <= cvc.getNoOfOutputTuples(nulled.getTableName()) ; i++){/**FIXME: Handle repeated relations*/
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
//           tempMap.put(entry.getKey().toLowerCase(), entry.getValue());
           tempMap.put(entry.getKey().toUpperCase(), entry.getValue());
        }
        cvc.setNoOfOutputTuples(tempMap);       
    }
	
	public static String genNegativeCondsEqClass(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2, int tuple){
		String constraintString = new String();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		ConstraintGenerator constrGen = new ConstraintGenerator();
		
		for(int i = 1; i <= cvc.getNoOfOutputTuples(c1.getTable().getTableName()) ; i++){
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
