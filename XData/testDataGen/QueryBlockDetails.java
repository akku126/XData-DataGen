package testDataGen;

import generateConstraints.Constraints;
import generateConstraints.GenerateConstraintsForCaseConditions;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.GenerateUniqueKeyConstraints;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.AggregateFunction;
import parsing.CaseCondition;
import parsing.Column;
//import parsing.Conjunct;
import parsing.CaseExpression;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.QueryParser;
import parsing.QueryStructure;
import parsing.RelationHierarchyNode;
import parsing.Table;

/**
 * This class stores details of a query block
 * If the given query contains nested subqueries 
 * then we have to create a new query block for each nested subquery block and for the outer query block
 * 
 * @author mahesh
 *
 */
public class QueryBlockDetails implements Serializable{

	
	private static final long serialVersionUID = 6036368868346030666L;
	private static Logger logger = Logger.getLogger(QueryBlockDetails.class.getName());

	public ArrayList<String> getNonEmptyConstraints() {
		return NonEmptyConstraints;
	}


	public void setNonEmptyConstraints(ArrayList<String> nonEmptyConstraints) {
		NonEmptyConstraints = nonEmptyConstraints;
	}


	/** The list of the columns that are projected out in this query block */
	private ArrayList<Node> projectedCols;

	/** The list of the aggregation functions that are projected out in this query block*/
	private ArrayList<AggregateFunction> aggFunc;

	/** Stores details about the where clause of this query block */
	private ArrayList<ConjunctQueryStructure> conjuncts;
	
	/** Stores details about the where clause of this query block from query structure*/
	private ArrayList<ConjunctQueryStructure> conjunctsqs;


	/** The group by nodes of this query block*/
	private ArrayList<Node> groupByNodes;	

	/** The having clause of this query block, if any */
	private Node havingClause;

	/** The havingClause is flattenned into this vector
	 * FIXME: We are handling only ANDing of conditions in havingClause but not ORing of the conditions */
	private ArrayList<Node> aggConstraints;


	/**The number of tuples needed to satisfy the constrained aggregation in this query block */
	private int finalCount;

	/*** The number of groups of tuples to be generated by this query block */
	private int noOfGroups;

	private ArrayList<String> NonEmptyConstraints;
	/** Stores details about each from clause subquery block */
	private ArrayList<QueryBlockDetails> fromClauseSubQueries;

	/** Stores details about each where clause subquery block */
	private ArrayList<QueryBlockDetails> whereClauseSubQueries;

	/** Stores the Case conditions if the query contains CASE statements*/
	private Map<Integer,CaseExpression> caseConditionMap;

	/** Used to store the attributes(can be a single attribute or multiple attributes) which must contain distinct values across multiples tuples of the relation
	 * Examples include the attributes which form unique/primary key */
	Set< HashSet<Node>> uniqueElements;

	/** Used to store the attributes which must contain same value across multiples tuples of the group
	 * Examples include group by attributes */
	private Set<Node> singleValuedAttributes;

	/** To kill some mutations we might require some of the attributes to be distinct. These are stored here */
	private Set<HashSet<Node>> uniqueElementsAdd;

	/** To kill some mutations we might require some of the attributes to be single valued. These are stored here */

	private Set<Node> singleValuedAttributesAdd;
	
	/** Stores the equivalence class that is being killed */
	private ArrayList<Node> equivalenceClassesKilled;

	/** Stores the names of base relations (Can be repeated) in this query block */
	ArrayList<String> baseRelations; 
	/**
	 * The  variables uniqueElements, singleValuedAttributes, singleValuedAttributesAdd, uniqueElementsAdd and equivalenceClassesKilled
	 * are used by tuple assignment method
	 */

	/** Indicates whether this query block contains constrained aggregation or not */
	private boolean isConstrainedAggregation;
	
	/** for each relation occurrence, stores the count needed to satisfy the constrained aggregation function on that relation */
	private HashMap<String, Integer> finalCountMap;

	/** Stores the list of aggregate functions over a column of the relation*/
	private HashMap<Column, ArrayList<Node>> colAggMap;

	/**Related to general  parameters in the input query*/
	private HashMap<String, String> paramMap;

	private HashMap<ArrayList<String>, Node> paramsNodeMap;

	private int pConstraintId;

	private int paramCount;
	
	private HashMap<String, Node> constraintsWithParameters;
	
	private RelationHierarchyNode topLevelRelation;
	
	
	/**
	 * The constructor for this method
	 */
	public QueryBlockDetails(){
		projectedCols = new ArrayList<Node>();
		aggFunc = new ArrayList<AggregateFunction>();
		conjuncts = new ArrayList<ConjunctQueryStructure>();
		groupByNodes = new ArrayList<Node>();
		isConstrainedAggregation = false;
		havingClause = null;
		aggConstraints = new ArrayList<Node>();
		finalCount = 0;
		noOfGroups = 1;
		uniqueElements = new HashSet<HashSet<Node>>();
		uniqueElementsAdd = new HashSet<HashSet<Node>>();
		singleValuedAttributes = new HashSet<Node>();
		singleValuedAttributesAdd = new HashSet<Node>();
		equivalenceClassesKilled = new ArrayList<Node>();
		fromClauseSubQueries = new ArrayList<QueryBlockDetails>();
		whereClauseSubQueries = new ArrayList<QueryBlockDetails>();
		baseRelations = new ArrayList<String>();
		finalCountMap = new HashMap<String, Integer>();
		paramMap = new HashMap<String, String>();
		paramsNodeMap = new HashMap<ArrayList<String>, Node>();
		paramCount = 0;
		pConstraintId = 0;
		constraintsWithParameters = new HashMap<String, Node>();
		//setCaseConditionMap(new HashMap<Integer,Vector<CaseCondition>>());
	}


	/**
	 * This function initialize the query block details
	 * @param qp
	 */
	/*public static QueryBlockDetails intializeQueryBlockDetails(QueryParser qp){

		QueryBlockDetails qbt = new QueryBlockDetails();
		
		qbt.setProjectedCols( new ArrayList<Node>(qp.getProjectedCols()) );
		qbt.setAggFunc( new ArrayList<AggregateFunction>(qp.getAggFunc()) );
		//qbt.setConjuncts( new ArrayList<Conjunct>(qp.getConjuncts()) );
		qbt.setGroupByNodes( new ArrayList<Node>(qp.getGroupByNodes()) );
		qbt.setCaseConditionMap(new HashMap<Integer,Vector<CaseCondition>> (qp.getCaseConditionMap()) );
		qbt.setCaseConditionMap(new HashMap<Integer,Vector<CaseCondition>> (qp.getCaseConditionMap()) );
		
		qbt.setHavingClause(qp.getHavingClause());
		if(qbt.getHavingClause() != null)
			qbt.setConstrainedAggregation(true);
		
		qbt.setTopLevelRelation(qp.topLevelRelation);

		return qbt;
	}*/



	/**
	 * This function initialize the query block details
	 * @param qp
	 */
	public static QueryBlockDetails intializeQueryBlockDetails(QueryStructure qs){

		QueryBlockDetails qbt = new QueryBlockDetails();
		
		qbt.setProjectedCols( new ArrayList<Node>(qs.getProjectedCols()) );
		qbt.setAggFunc( new ArrayList<AggregateFunction>(qs.getAggFunc()) );
		qbt.setConjunctsQs( new ArrayList<ConjunctQueryStructure>(qs.getConjuncts()) );
		qbt.setGroupByNodes( new ArrayList<Node>(qs.getGroupByNodes()) );
		qbt.setNoOfGroups(qs.getGroupByNodes().size());
		qbt.setBaseRelations(new ArrayList<String>(qs.getQuery().getBaseRelation().values()));
		
		qbt.setCaseConditionMap(new HashMap<Integer,CaseExpression> (qs.getCaseConditionMap()) );
		qbt.setCaseConditionMap(new HashMap<Integer,CaseExpression> (qs.getCaseConditionMap()) );
		
		
		qbt.setHavingClause(qs.getHavingClause());
			if(qbt.getHavingClause() != null)
				qbt.setConstrainedAggregation(true);
		
		qbt.setTopLevelRelation(qs.topLevelRelation);

		return qbt;
	}

	

	/**
	 * This method translates the given final count into number of tuples per base relation
	 * This method treats the nested sub query as a relation and assigns the number of tuples for subquery to the number of groups of the subquery
	 * @param queryBlock
	 * @param rootTableName
	 * @return
	 */
	public static boolean getTupleAssignment(GenerateCVC1 cvc, QueryBlockDetails queryBlock,	String rootTableName) throws Exception{

		/** get join conditions of this query block*/
		Vector<Node > joinConds = new Vector<Node>();
		try{
			/**TODO: Which conjunct should be consider for the tuple assignment
			 * For now consider all the conjuncts, but incorrect
			 */
			for(ConjunctQueryStructure con: queryBlock.getConjunctsQs())
				for(Node n: con.getJoinCondsAllOther())
					joinConds.add(new Node(n));
			
			for(ConjunctQueryStructure con: queryBlock.getConjunctsQs())
				for(Node n: con.getJoinCondsForEquivalenceClasses())
					joinConds.add(new Node(n));
			
			/** If there are no join conditions or the final count is 1 , then we can directly assign the count to base relation*/
			if(joinConds == null || joinConds.size() == 0 || queryBlock.getFinalCount() == 1)			
				return GetTupleAssignmentForQueryBlock.getTupleAssignmentWithoutJoins(cvc, queryBlock);		
	
			return GetTupleAssignmentForQueryBlock.getTupleAsgnmentForQueryBlock(cvc, queryBlock, rootTableName);
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
	}


	public RelationHierarchyNode getTopLevelRelation() {
		return topLevelRelation;
	}


	public void setTopLevelRelation(RelationHierarchyNode topLevelRelation) {
		this.topLevelRelation = topLevelRelation;
	}


	/**
	 * A wrapper method to get constraints for  all the blocks of the query
	 * @param cvc
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForQueryBlock(GenerateCVC1 cvc) throws Exception{

		return getConstraintsForQueryBlock(cvc, cvc.getOuterBlock());
	}
	
	/**
	 * This method is used to get constraints for all the conditions involved in this query block, including from clause sub query blocks, if any
	 * @param qb 
	 * @param cvc 
	 */
	public static String getConstraintsForQueryBlock(GenerateCVC1 cvc, QueryBlockDetails qb) throws Exception{

		String constraintString = "";
		try{
			constraintString += "%---------------------------------------------\n%CONSTRAINTS OF THIS BLOCK \n%--------------------------------------------\n";
			constraintString +=  getConstraintsForQueryBlockExceptSubQuries(cvc, qb);
			constraintString += "%---------------------------------------------\n%END OF CONSTRAINTS OF THIS BLOCK \n%--------------------------------------------\n";
	
	
			/** Add constraints related to From clause subqueries block */
			for(QueryBlockDetails qbt: qb.getFromClauseSubQueries()){
	
				constraintString += "\n%---------------------------------\n% FROM CLAUSE SUBQUERY BLOCK\n%---------------------------------\n";
				constraintString += getConstraintsForQueryBlock(cvc, qbt);			
				constraintString += "\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY BLOCK\n%---------------------------------\n";
			}
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}

		return constraintString;

	}

	/**
	 * This method is used to get constraints for all the conditions involved in this query block, including from clause sub query blocks, if any
	 * @param qb 
	 * @param cvc 
	 */
	public static String getConstraintsForQueryBlock(GenerateCVC1 cvc, QueryBlockDetails qb, Node n) throws Exception{

		String constraintString = "";
		try{
			constraintString += "%---------------------------------------------\n%CONSTRAINTS OF THIS BLOCK \n%--------------------------------------------\n";
			constraintString +=  getConstraintsForQueryBlockExceptSubQuries(cvc, qb, n);
			constraintString += "%---------------------------------------------\n%END OF CONSTRAINTS OF THIS BLOCK \n%--------------------------------------------\n";
	
	
			/** Add constraints related to From clause subqueries block */
			for(QueryBlockDetails qbt: qb.getFromClauseSubQueries()){
	
				constraintString += "\n%---------------------------------\n% FROM CLAUSE SUBQUERY BLOCK\n%---------------------------------\n";
				constraintString += getConstraintsForQueryBlock(cvc, qbt);			
				constraintString += "\n%---------------------------------\n% END OF FROM CLAUSE SUBQUERY BLOCK\n%---------------------------------\n";
			}
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}

		return constraintString;

	}

	/** 
	 * This method is used to get constraints for all the conditions of this query block
	 * @param cvc
	 * @param qb
	 * @return
	 */
	public static String getConstraintsForQueryBlockExceptSubQuries(GenerateCVC1 cvc, QueryBlockDetails qb) throws Exception{
		String constraintString = "";
		Constraints constraints=new Constraints();
		constraints.constraints.add("");
		constraints.stringConstraints.add("");
		try{
			/** Add the positive conditions for each conjunct of this query block */
			for(ConjunctQueryStructure conjunct : qb.getConjunctsQs()){
				constraintString += "\n%---------------------------------\n% CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------\n";
				//constraintString += GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qb, conjunct);
				constraints=Constraints.orConstraints(constraints, GenerateConstraintsForConjunct.getConstraintsInConjuct(cvc, qb, conjunct));
				
				/**FIXME: Handle OR + Where clause Sub query Correctly
				 * Right Now we assume that if there is Where sub query then no ORing of conds
				 * Use Amol generalized approach*/
				constraintString += "\n%---------------------------------\n% WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
				constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(cvc, qb, conjunct);
				constraintString += "\n%---------------------------------\n% END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
				
				//constraintString += "\n%---------------------------------\n% END OF CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------\n";
			}
			constraintString += Constraints.getConstraint(constraints);
			String stringConstraints= Constraints.getStringConstraints(constraints);
			cvc.getStringConstraints().add(stringConstraints);
			
			//constraintString += getCaseConditionConstraints(cvc);
			constraintString += getGroupByAndHavingClauseConstraints(cvc, qb);
	
			constraintString += getOtherConstraintsForQueryBlock(cvc, qb);
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return constraintString;
	}
	
	/** 
	 * This method is used to get constraints for all the conditions of this query block
	 * @param cvc
	 * @param qb
	 * @return
	 */
	public static String getConstraintsForQueryBlockExceptSubQuries(GenerateCVC1 cvc, QueryBlockDetails qb, Node n) throws Exception{
		String constraintString = "";
		Constraints constraints=new Constraints();
		constraints.constraints.add("");
		constraints.stringConstraints.add("");
		try{
			/** Add the positive conditions for each conjunct of this query block */
			for(ConjunctQueryStructure conjunct : qb.getConjunctsQs()){
				constraintString += "\n%---------------------------------\n% CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------\n";
				//constraintString += GenerateConstraintsForConjunct.getConstraintsForConjuct(cvc, qb, conjunct);
				constraints=Constraints.orConstraints(constraints, GenerateConstraintsForConjunct.getConstraintsInConjuct(cvc, qb, conjunct));
				
				/**FIXME: Handle OR + Where clause Sub query Correctly
				 * Right Now we assume that if there is Where sub query then no ORing of conds
				 * Use Amol generalized approach*/
				constraintString += "\n%---------------------------------\n% WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
				constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(cvc, qb, conjunct);
				constraintString += "\n%---------------------------------\n% END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
				
				//constraintString += "\n%---------------------------------\n% END OF CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------\n";
				
			}
			//FOR CASE CONDITION IN WHERE CLAUSE - ADD CONSTRAINTS APPENDING AND HERE
			
			constraintString += Constraints.getConstraint(constraints);
			String stringConstraints= Constraints.getStringConstraints(constraints);
			cvc.getStringConstraints().add(stringConstraints);
			
			//constraintString += getCaseConditionConstraints(cvc);
			constraintString += getGroupByClauseConstraints(cvc, qb, n);
	
			constraintString += getOtherConstraintsForQueryBlock(cvc, qb);
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return constraintString;
	}


	/** 
	 * Used to get app constraints, parameter constraints and unique key constraints for the given query block
	 * @param cvc
	 * @param qb
	 * @return
	 */
	public static String getOtherConstraintsForQueryBlock(GenerateCVC1 cvc,	QueryBlockDetails qb) throws Exception{

		String constraintString = "";
		try{
			/**Add constraints related to parameters in the query block*/
			constraintString += "\n%---------------------------------\n%PARAMETERIZED CLAUSE CONSTRAINTS\n%---------------------------------\n";		 
			constraintString += RelatedToParameters.getConstraintsForParameters( cvc, qb);
			constraintString += "\n%---------------------------------\n%END OF PARAMETERIZED CLAUSE CONSTRAINTS\n%---------------------------------\n";
			
			/**Application constraints if any*/
			constraintString += "\n%---------------------------------\n%APPLICATION CONSTRAINTS\n%---------------------------------\n";		
			/*FIXME: constraintString += NonEmptyConstraints;*/
			constraintString += "\n%---------------------------------\n%END OF APPLICATION CONSTRAINTS\n%---------------------------------\n";
			
			constraintString += "\n%---------------------------------\n% UNIQUE  KEY CONSTRAINTS \n%---------------------------------\n";
			constraintString += GenerateUniqueKeyConstraints.generateUniqueConstraints(cvc, qb, qb.getUniqueElements());
			constraintString += "\n%---------------------------------\n%END OF UNIQUE  KEY CONSTRAINTS \n%---------------------------------\n";
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return constraintString;
	}


	/**
	 * Used to get group by and having clause constraints for the given query block
	 * @param cvc
	 * @param qb
	 * @return
	 * @throws Exception
	 */
	public static String getGroupByAndHavingClauseConstraints(GenerateCVC1 cvc, QueryBlockDetails qb)	throws Exception {

		String constraintString = "";
		try{
			/** get group by constraints */
			constraintString += "\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS\n%---------------------------------\n";
			constraintString += GenerateGroupByConstraints.getGroupByConstraints(cvc, qb);
			constraintString += "\n%---------------------------------\n%END OF GROUP BY CLAUSE CONSTRAINTS\n%---------------------------------\n";
	
	
			/** Generate havingClause constraints */
			constraintString += "\n%---------------------------------\n%HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n";
			for(int j=0; j< qb.getNoOfGroups();j ++)
				for(int k=0; k < qb.getAggConstraints().size();k++){
					constraintString += GenerateConstraintsForHavingClause.getHavingClauseConstraints(cvc, qb, qb.getAggConstraints().get(k), qb.getFinalCount(), j);
				}
			constraintString += "\n%---------------------------------\n%END OF HAVING CLAUSE CONSTRAINTS\n%---------------------------------\n";
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return constraintString;
	}
	
	/**
	 * Used to get group by and having clause constraints for the given query block
	 * @param cvc
	 * @param qb
	 * @return
	 * @throws Exception
	 */
	public static String getGroupByClauseConstraints(GenerateCVC1 cvc, QueryBlockDetails qb, Node n)	throws Exception {

		String constraintString = "";
		try{
			/** get group by constraints */
			constraintString += "\n%---------------------------------\n%GROUP BY CLAUSE CONSTRAINTS\n%---------------------------------\n";
			constraintString += GenerateGroupByConstraints.getGroupByConstraints(cvc, qb, n);
			constraintString += "\n%---------------------------------\n%END OF GROUP BY CLAUSE CONSTRAINTS\n%---------------------------------\n";
			
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return constraintString;
	}
	/**
	 * This method generated the constraint string for the case conditions specified 
	 * in projected columns or where clauses
	 * 
	 * @param cvc
	 * @return
	 * @throws Exception
	 */
	public static String getCaseConditionConstraints(GenerateCVC1 cvc) throws Exception{
		String constraintString = "";
		
		try{
			
			constraintString += "\n%---------------------------------\n%CASE CONDITION CONSTRAINTS\n%---------------------------------\n";
			constraintString += GenerateConstraintsForCaseConditions.getCaseConditionConstraintsForOriginalQuery(cvc,cvc.getOuterBlock());
			constraintString += "\n%---------------------------------\n%END OF CASE CONDITION CONSTRAINTS\n%---------------------------------\n";
			
		}catch(Exception e){
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return constraintString;
	}

	/**
	 * Get list of from tables in this query block, in the form of table name and its occurrence
	 * @param cvc
	 * @param qbt
	 * @return
	 */
	public static HashMap<String, Table> getListOfTablesInQueryBlock(GenerateCVC1 cvc,	QueryBlockDetails qbt) {
	
		/**stores list of tables in this query block*/
		 HashMap<String, Table> tables = new HashMap<String, Table>();
	
		/**Get the list of tables in this query block*/
		for(String relation : qbt.getBaseRelations()){
	
			/**Get base table name for this relation*/
			String tableName = relation.substring(0, relation.length()-1);/**FIXME: If the relation occurrence >= 10 then problem*/
	
			/**Get the table details from base table*/			
			for(int i=0; i < cvc.getResultsetTables().size(); i++){
	
				Table table1 = cvc.getResultsetTables().get(i);
	
				if(table1.getTableName().equalsIgnoreCase(tableName)){/**The data base relation is found*/
	
					/**If this table is not already added*/
					if( !tables.containsKey(table1))
						tables.put(relation, table1);
					break ; 
				}
			}
		}
	
		return tables;
	}
	
	/**Below are the setters and getters for the variables of this class */
	public ArrayList<Node> getProjectedCols() {
		return projectedCols;
	}

	public void setProjectedCols(ArrayList<Node> projectedCols) {
		this.projectedCols = projectedCols;
	}

	public ArrayList<AggregateFunction> getAggFunc() {
		return aggFunc;
	}

	public void setAggFunc(ArrayList<AggregateFunction> aggFunc) {
		this.aggFunc = aggFunc;
	}

	/*public ArrayList<Conjunct> getConjuncts() {
		return conjuncts;
	}

	public void setConjuncts(ArrayList<Conjunct> conjuncts) {
		this.conjuncts = conjuncts;
	}*/
	
	public ArrayList<ConjunctQueryStructure> getConjunctsQs() {
		return conjunctsqs;
	}

	public void setConjunctsQs(ArrayList<ConjunctQueryStructure> conjuncts) {
		this.conjunctsqs = conjuncts;
	}
	

	public ArrayList<Node> getGroupByNodes() {
		return groupByNodes;
	}

	public void setGroupByNodes(ArrayList<Node> groupByNodes) {
		this.groupByNodes = groupByNodes;
	}

	public boolean isConstrainedAggregation() {
		return isConstrainedAggregation;
	}

	public void setConstrainedAggregation(boolean isConstrainedAggregation) {
		this.isConstrainedAggregation = isConstrainedAggregation;
	}

	public Node getHavingClause() {
		return havingClause;
	}

	public void setHavingClause(Node havingClause) {
		this.havingClause = havingClause;
	}

	public ArrayList<Node> getAggConstraints() {
		return aggConstraints;
	}

	public void setAggConstraints(ArrayList<Node> aggConstraints) {
		this.aggConstraints = aggConstraints;
	}

	public int getFinalCount() {
		return finalCount;
	}

	public void setFinalCount(int finalCount) {
		this.finalCount = finalCount;
	}

	public int getNoOfGroups() {
		return noOfGroups;
	}

	public void setNoOfGroups(int noOfGroups) {
		this.noOfGroups = noOfGroups;
	}

	public Set<HashSet<Node>> getUniqueElements() {
		return uniqueElements;
	}

	public void setUniqueElements(Set<HashSet<Node>> uniqueElements) {
		this.uniqueElements = uniqueElements;
	}

	public Set<Node> getSingleValuedAttributes() {
		return singleValuedAttributes;
	}

	public void setSingleValuedAttributes(Set<Node> singleValuedAttributes) {
		this.singleValuedAttributes = singleValuedAttributes;
	}

	public Set<HashSet<Node>> getUniqueElementsAdd() {
		return uniqueElementsAdd;
	}

	public void setUniqueElementsAdd(Set<HashSet<Node>> uniqueElementsAdd) {
		this.uniqueElementsAdd = uniqueElementsAdd;
	}

	public Set<Node> getSingleValuedAttributesAdd() {
		return singleValuedAttributesAdd;
	}

	public void setSingleValuedAttributesAdd(Set<Node> singleValuedAttributesAdd) {
		this.singleValuedAttributesAdd = singleValuedAttributesAdd;
	}

	public ArrayList<Node> getEquivalenceClassesKilled() {
		return equivalenceClassesKilled;
	}

	public void setEquivalenceClassesKilled(ArrayList<Node> equivalenceClassesKilled) {
		this.equivalenceClassesKilled = equivalenceClassesKilled;
	}	

	public ArrayList<QueryBlockDetails> getFromClauseSubQueries() {
		return fromClauseSubQueries;
	}

	public void setFromClauseSubQueries(
			ArrayList<QueryBlockDetails> fromClauseSubQueries) {
		this.fromClauseSubQueries = fromClauseSubQueries;
	}

	public ArrayList<QueryBlockDetails> getWhereClauseSubQueries() {
		return whereClauseSubQueries;
	}

	public void setWhereClauseSubQueries(
			ArrayList<QueryBlockDetails> whereClauseSubQueries) {
		this.whereClauseSubQueries = whereClauseSubQueries;
	}


	public ArrayList<String> getBaseRelations() {
		return baseRelations;
	}


	public void setBaseRelations(ArrayList<String> baseRelations) {
		this.baseRelations = baseRelations;
	}


	public HashMap<String, Integer> getFinalCountMap() {
		return finalCountMap;
	}


	public void setFinalCountMap(HashMap<String, Integer> finalCountMap) {
		this.finalCountMap = finalCountMap;
	}


	public HashMap<Column, ArrayList<Node>> getColAggMap() {
		return colAggMap;
	}


	public void setColAggMap(HashMap<Column, ArrayList<Node>> colAggMap) {
		this.colAggMap = colAggMap;
	}


	public HashMap<String, String> getParamMap() {
		return paramMap;
	}


	public void setParamMap(HashMap<String, String> paramMap) {
		this.paramMap = paramMap;
	}

	public HashMap<ArrayList<String>, Node> getParamsNodeMap() {
		return paramsNodeMap;
	}


	public void setParamsNodeMap(HashMap<ArrayList<String>, Node> paramsNodeMap) {
		this.paramsNodeMap = paramsNodeMap;
	}


	public int getpConstraintId() {
		return pConstraintId;
	}


	public void setpConstraintId(int pConstraintId) {
		this.pConstraintId = pConstraintId;
	}


	public int getParamCount() {
		return paramCount;
	}


	public void setParamCount(int paramCount) {
		this.paramCount = paramCount;
	}



	public HashMap<String, Node> getConstraintsWithParameters() {
		return constraintsWithParameters;
	}


	public void setConstraintsWithParameters(HashMap<String, Node> constraintsWithParameters) {
		this.constraintsWithParameters = constraintsWithParameters;
	}

	/**
	 * @return the caseConditionMap
	 */
	public Map<Integer,CaseExpression> getCaseConditionMap() {
		return caseConditionMap;
	}


	/**
	 * @param caseConditionMap the caseConditionMap to set
	 */
	public void setCaseConditionMap(Map<Integer,CaseExpression> caseConditionMap) {
		this.caseConditionMap = caseConditionMap;
	}

}
