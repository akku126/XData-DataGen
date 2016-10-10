
package testDataGen;

import generateConstraints.GetCVC3HeaderAndFooter;
import generateConstraints.TupleRange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.GenerateDataForOriginalQuery;
import killMutations.MutationsInFromSubQuery;
import killMutations.MutationsInOuterBlock;
import killMutations.MutationsInWhereSubQuery;
import parsing.Column;
import parsing.Conjunct;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Query;
import parsing.QueryParser;
import parsing.Table;
import testDataGen.PopulateTestData;
import stringSolver.StringConstraintSolver;
import util.DatabaseConnection;
import util.MyConnection;
import util.TableMap;
import util.TagDatasets;
import util.Utilities;
import util.TagDatasets.MutationType;
import util.TagDatasets.QueryBlock;

public class GenerateCVC1 implements Serializable{
	/**FIXME: appConstraints, NonEmptyConstraints...whether they are common for query or for each block of query*/

	/**
	 * 
	 */
	private static Logger logger = Logger.getLogger(GenerateCVC1.class.getName());
	private static final long serialVersionUID = 2996426044633967059L;
	/** Details about the the tables in the input database */
	private TableMap tableMap;
	private Query query; 
	private Query topQuery;

	/** The parser stores the details of the query after the input query is parsed	 */
	private QueryParser qParser;

	/** Stores the base relation for each repeated occurrence of a relation  */

	private HashMap<String,String> baseRelation; 

	/** Maintains the increment for each repeated occurrence of a relation. For instance if R repeats twice as R1 and R2 
	 * then the currentIndexCount of R is 0, R1 is 1 and that of R2 is 2.  */
	private  HashMap<String,Integer> currentIndexCount; 

	/**  For each relation maintains a count of how many times it repeats. Incides of a relation should be incremented by this number */
	private HashMap<String,Integer> repeatedRelationCount;

	/** Stores the positions at which the indexes for each repeated relations start */
	private HashMap<String, Integer[]> repeatedRelNextTuplePos;

	/** Stores details about the number of tuples for each occurrence of the relation in the input query */
	private HashMap<String, Integer> noOfTuples;

	/** Stores the no of tuples to be generated for each relation */
	private HashMap<String,Integer> noOfOutputTuples;

	/** Stores details about the outer block of the query */
	public QueryBlockDetails outerBlock;

	/** Stores details about the foreign keys of the tables of the query*/
	private ArrayList<Node> foreignKeys;

	/** Stores details about the foreign keys of the tables of the query*/
	private ArrayList<ForeignKey> foreignKeysModified;

	/** Stores the list of constraints for this data generation step*/
	private ArrayList<String> constraints;

	/** Stores the list of string constraints for this data generation step*/
	private ArrayList<String> stringConstraints;

	private String CVCStr;

	private HashMap<Table,Vector<String> > resultsetTableColumns1;

	private Vector<Column> resultsetColumns;

	/** Keeps track of which columns have which null values and which of them have been used*/
	private HashMap<Column, HashMap<String, Integer>> colNullValuesMap;	

	/** Reference to string solver */
	private StringConstraintSolver stringSolver;

	/**FORALL/ NOT EXISTS:	fne = true/false*/
	private boolean fne;		

	/**FIXME: Why this??*/
	private ArrayList<String> datatypeColumns;

	private String output;

	/** Used to store the English description of the query */
	private String queryString;

	/** Sets the path to the location where the file containing queries is located and where the output files will be generated */
	private String filePath;

	/** Used to number the data sets */
	private int count;


	private ArrayList<Table> resultsetTables;

	/** I/P DATABASE: 		ipdb = true/false*/
	private boolean ipdb;   		

	/**Stores CVC3 Header*/
	private String CVC3_HEADER;

	/**stores details about the query, if query consists of set operations*/
	private GenerateUnionCVC unionCVC;


	/**details about branch queries of the input if any*/
	private BranchQueriesDetails branchQueries; 

	private Vector<Table> tablesOfOriginalQuery;

	/**Indicates the type of mutation we are trying to kill*/
	private String typeOfMutation;

	/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
	private HashMap<String, Integer[]> tableNames;

	/** Contains the list of equi join condition for each table in the given query. Used during foreign key constraints*/
	private HashMap<String, Vector<Vector<Node>> > equiJoins; 
	
	// Assignment Id
	private int assignmentId;
	private int questionId;
	private int queryId;
	private String courseId; 
	
	private transient Connection connection;
	
	private Map<String, TupleRange> allowedTuples;

	/** The constructor for this class */
	public GenerateCVC1 (){
		baseRelation = new HashMap<String, String>();
		currentIndexCount = new HashMap<String, Integer>();
		repeatedRelationCount = new HashMap<String, Integer>();
		repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
		noOfTuples = new HashMap<String, Integer>();
		noOfOutputTuples = new HashMap<String, Integer>();
		outerBlock = new QueryBlockDetails();
		colNullValuesMap = new HashMap<Column, HashMap<String,Integer>>();
		datatypeColumns = new ArrayList<String>();
		resultsetColumns = new Vector<Column>();
		resultsetTableColumns1 = new HashMap<Table, Vector<String>>();
		resultsetTables = new ArrayList<Table>();
		stringSolver = new StringConstraintSolver();	
		branchQueries = new BranchQueriesDetails();
		tableNames = new HashMap<String, Integer[]>();
		equiJoins = new HashMap<String, Vector<Vector<Node>>>();
		allowedTuples = new HashMap<String, TupleRange>();
	}


	public void closeConn() {
		try{
			this.connection.close();
		}catch(SQLException e){};
	}
	/** 
	 * This method initializes all the details about the given query whose details are stored in the query Parser
	 * @param qParser
	 */
	public void initializeQueryDetails (QueryParser queryParser) throws Exception{
		try{
			qParser = queryParser;
			this.setFne(false);
			query = qParser.getQuery();
			queryString = query.getQueryString();
			//currentIndex = query.getCurrentIndex();
			baseRelation = query.getBaseRelation();
			currentIndexCount = query.getCurrentIndexCount();
			repeatedRelationCount = query.getRepeatedRelationCount();
	
			/** Initialize the foreign key details*/
			foreignKeys = new ArrayList<Node>( qParser.getForeignKeys());
			foreignKeysModified = new ArrayList<ForeignKey>( qParser.getForeignKeyVectorModified());		
	
			/** Initiliaze the outer query block*/
			outerBlock = QueryBlockDetails.intializeQueryBlockDetails(queryParser);
				
			/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
			tableNames = qParser.getTableNames();
	
			/** Initialize each from clause nested sub query blocks */
			for(QueryParser qp: qParser.getFromClauseSubqueries())
				outerBlock.getFromClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );
	
			/** Initialize the where clause nested sub query blocks */
			for(QueryParser qp: qParser.getWhereClauseSubqueries())
				outerBlock.getWhereClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(), e);
			//e.printStackTrace();
			throw e;
		}
	}


	public void initializeOtherDetails() throws Exception{

		try{
			/**Update the  base relations in each block of the query*/
			RelatedToPreprocessing.getRelationOccurredInEachQueryBlok(this);
	
			/**Sort the foreign keys based on topological sorting of foreign keys*/
			RelatedToPreprocessing.sortForeignKeys(this);
	
			/**Generate CVC3 Header, This is need to initialize the CVC3 Data Type field of each column of each table */
			this.setCVC3_HEADER( GetCVC3HeaderAndFooter.generateCVC3_Header(this) );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}


	/**
	 * Initializes the elements necessary for data generation
	 * Call this function after the previous data generation has been done and 
	 * constraints for the current data generation have not been added
	 */
	public void inititalizeForDataset() throws Exception{

		constraints = new ArrayList<String>();
		stringConstraints = new ArrayList<String>();
		CVCStr = "";
		typeOfMutation = "";
		try{
			

			/** Add additional groupBy attributes if the relation of groupby attributes references to any other relation**/
			// Make the referenced relation a join with existing relation
			//Update no of output tuples, no of groups, repeated relation count, table occurrences to add additional
			//datasets
			
			
			/** initialize the no of output tuples*/
			noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
	
			/**Merging noOfOutputTuples, if input query has set operations*/
			if(qParser.setOperator!=null && qParser.setOperator.length()>0){  
	
				/**Initialize the number of tuples in left side query of the set operation*/
				noOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCleft().query.getRepeatedRelationCount().clone();
	
				/**Now get the no of tuples for each relation on right side query of the set operation and add to the data structure*/
				HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCright().query.getRepeatedRelationCount().clone();
	
				/**get iterator*/
				Iterator rt=RightnoOfOutputTuples.entrySet().iterator();
	
				/**while there are values in the hash map*/
				while(rt.hasNext()){
					Map.Entry pairs=(Entry) rt.next();
	
					/**get table name*/
					String table=(String) pairs.getKey();
	
					/**get the number of tuples*/
					int noOfTuples = (Integer) pairs.getValue();
	
					/**Update the data structure*/
					if(noOfOutputTuples.containsKey(table)&&noOfOutputTuples.get(table)<noOfTuples){
						noOfOutputTuples.put(table, noOfTuples);
					}
					if(!noOfOutputTuples.containsKey(table)){
						noOfOutputTuples.put(table, noOfTuples);
					}
				}
			}else if(!qParser.getFromClauseSubqueries().isEmpty()
					&& qParser.getFromClauseSubqueries().get(0) != null){
				QueryParser qp = new QueryParser(qParser.getTableMap());
				qp = qParser.getFromClauseSubqueries().get(0);
				
				if(qp.setOperator!=null && qp.setOperator.length()>0){
					
					/**Initialize the number of tuples in left side query of the set operation*/
					noOfOutputTuples = (HashMap<String,Integer>)qp.getLeftQuery().getQuery().getRepeatedRelationCount().clone();
		
					/**Now get the no of tuples for each relation on right side query of the set operation and add to the data structure*/
					HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)qp.getRightQuery().getQuery().getRepeatedRelationCount().clone();
		
					/**get iterator*/
					Iterator rt=RightnoOfOutputTuples.entrySet().iterator();
		
					/**while there are values in the hash map*/
					while(rt.hasNext()){
						Map.Entry pairs=(Entry) rt.next();
		
						/**get table name*/
						String table=(String) pairs.getKey();
		
						/**get the number of tuples*/
						int noOfTuples = (Integer) pairs.getValue();
		
						/**Update the data structure*/
						if(noOfOutputTuples.containsKey(table)&&noOfOutputTuples.get(table)<noOfTuples){
							noOfOutputTuples.put(table, noOfTuples);
						}
						if(!noOfOutputTuples.containsKey(table)){
							noOfOutputTuples.put(table, noOfTuples);
						}
					}
					
				}
			}
			
			else{		
				this.noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
			}
			for(String tempTable : noOfOutputTuples.keySet())
				if(noOfOutputTuples.get(tempTable) != null && noOfOutputTuples.get(tempTable) >= 1)
					logger.log(Level.INFO,"START COUNT for " + tempTable + " = " + noOfOutputTuples.get(tempTable));
	
			repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
	
			/** Update repeated relation next position etc..*/
			Iterator<String> itr = repeatedRelationCount.keySet().iterator();
			while(itr.hasNext()){
				String tableName = itr.next();
				int c =  repeatedRelationCount.get(tableName);
				for(int i=1;i<=c;i++){
					Integer[] tuplePos = new Integer[32];
					tuplePos[1] = i;//Meaning first tuple is at pos i
					repeatedRelNextTuplePos.put(tableName+i, tuplePos);
					noOfTuples.put(tableName+i, 1);
					currentIndexCount.put(tableName+i, i);
				}
			}
	
			/** Initializes the data structures that are used/updated by the tuple assignment method*/
			initilizeDataStructuresForTupleAssignment(outerBlock);
			for(QueryBlockDetails qbt: getOuterBlock().getFromClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			for(QueryBlockDetails qbt: getOuterBlock().getWhereClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			
			/**get the list of equi join conditions for each table in the query*/
			GenerateCVC1.getListOfEquiJoinConditions( this );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}

	/**
	 * This method initializes the data structures that are used by the tuple assignment method
	 * @param queryBlock
	 */
	public void initilizeDataStructuresForTupleAssignment(QueryBlockDetails queryBlock){

		/** Add constraints related to parameters*/
		this.getConstraints().add(RelatedToParameters.addDatatypeForParameters( this, queryBlock));

		/**initialize other elements*/
		queryBlock.setUniqueElements(new HashSet<HashSet<Node>>());
		queryBlock.setUniqueElementsAdd(new HashSet<HashSet<Node>>());

		queryBlock.setSingleValuedAttributes(new HashSet<Node>());
		queryBlock.setSingleValuedAttributesAdd(new HashSet<Node>());

		queryBlock.setNoOfGroups(1);
		queryBlock.setFinalCount(0);

		queryBlock.setEquivalenceClassesKilled( new ArrayList<Node>());

	}
	
	/**
	 * Generates datasets to kill each type of mutation for the original query
	 * @throws Exception
	 */
	public void generateDatasetsToKillMutations() throws Exception {

		try{
			/**Generate data for the original query*/
			
			String mutationType = TagDatasets.MutationType.ORIGINAL.getMutationType() + TagDatasets.QueryBlock.NONE.getQueryBlock();
			
			GenerateDataForOriginalQuery.generateDataForOriginalQuery(this, mutationType);		
			
			/**Generate data sets to kill mutations in outer query block */
			MutationsInOuterBlock.generateDataForKillingMutantsInOuterQueryBlock(this);
	
			/**Generate data sets  to kill mutations in from clause nested sub query blocks */
			MutationsInFromSubQuery.generateDataForKillingMutantsInFromSubQuery(this);
	
			/**Generate data sets  to kill mutations in where clause nested sub query blocks */
			MutationsInWhereSubQuery.generateDataForKillingMutantsInWhereSubQuery(this);
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			this.closeConn();
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			this.closeConn();
			throw new Exception("Internal Error", e);
		}
	}

	/**
	 * A wrapper method that is used to get the number of tuples for each base relation occurrence 
	 * in each block of the query
	 */
	public static boolean tupleAssignmentForQuery(GenerateCVC1 cvc) throws Exception{

		try{
			if( CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, cvc.getOuterBlock()) == false)
				return false;
	
			return getTupleAssignmentForSubQueries(cvc);
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}


	/**
	 * estimate the number of tuples for each relation in each sub query block
	 * @throws Exception
	 */
	public static boolean getTupleAssignmentForSubQueries(GenerateCVC1 cvc) throws Exception{

		/** flag to indicate whether tuple assignment is possible or not*/
		boolean possible ;
		try{
			/** get tuple assignment for each from clause sub query block*/
			for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries() ){
	
				possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qbt);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					return false;
			}
	
			/** get tuple assignment for each where clause sub query block*/
			for(QueryBlockDetails qbt: cvc.getOuterBlock().getWhereClauseSubQueries()){
	
				possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qbt);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					return false;
			}
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
		/** For all blocks the tuple assignment is successful*/
		return true;

	}



	/**
	 * Gets the list of all equi join conditions on each column of each table 
	 */
	public static void getListOfEquiJoinConditions(GenerateCVC1 cvc) throws Exception{

		try{
			cvc.setEquiJoins( new HashMap<String, Vector<Vector<Node>>>());
			/**get list of equi joins in outer query block*/
			getListOfEquiJoinConditionsInQueryBlock(cvc, cvc.getOuterBlock());
	
			/**get list of join conditions in each from clause nested sub query block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries())
				getListOfEquiJoinConditionsInQueryBlock(cvc, qb);
	
			/**get list of join conditions in each where clause nested sub query block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries())
				getListOfEquiJoinConditionsInQueryBlock(cvc, qb);
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}

	/**
	 * Gets the list of equi join conditions in this query block
	 * @param cvc
	 * @param outerBlock2
	 */
	public static void getListOfEquiJoinConditionsInQueryBlock(	GenerateCVC1 cvc, QueryBlockDetails queryBlock) {

		/**for each conjunct*/
		for(Conjunct con: queryBlock.getConjuncts()){
			
			/**get the list of equi join conditions*/
			Vector<Vector<Node>> eqClass = con.getEquivalenceClasses();
			
			/**for every equivalence class*/
			for(Vector<Node> ec: eqClass){
				
				/**for every node in this equivalence class*/
				for(Node n: ec){
					
					String key =  n.getTable().getTableName() ;
					/**if this relation is present in the hash map*/
					if( cvc.getEquiJoins().containsKey(key) ){
						
						/**add this equivalence class to the list, if already not added*/
						if( !cvc.getEquiJoins().get(key).contains(ec) ){
							
							cvc.getEquiJoins().get(key).add(ec);
						}
					}
					else{
						
						Vector< Vector< Node >> eq = new Vector<Vector<Node>>();
						eq.add(ec);
						cvc.getEquiJoins().put(key, eq);
					}
				}
			}
		}

	}


	/**Below are the setters and getters for the variables of this class */
	public TableMap getTableMap() {
		return tableMap;
	}

	public void setTableMap(TableMap tableMap) {
		this.tableMap = tableMap;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public Query getTopQuery() {
		return topQuery;
	}

	public void setTopQuery(Query topQuery) {
		this.topQuery = topQuery;
	}

	public QueryParser getqParser() {
		return qParser;
	}

	public void setqParser(QueryParser qParser) {
		this.qParser = qParser;
	}

	public HashMap<String, String> getBaseRelation() {
		return baseRelation;
	}

	public void setBaseRelation(HashMap<String, String> baseRelation) {
		this.baseRelation = baseRelation;
	}

	public HashMap<String, Integer> getCurrentIndexCount() {
		return currentIndexCount;
	}

	public void setCurrentIndexCount(HashMap<String, Integer> currentIndexCount) {
		this.currentIndexCount = currentIndexCount;
	}

	public HashMap<String, Integer> getRepeatedRelationCount() {
		return repeatedRelationCount;
	}

	public void setRepeatedRelationCount(
			HashMap<String, Integer> repeatedRelationCount) {
		this.repeatedRelationCount = repeatedRelationCount;
	}

	public HashMap<String, Integer[]> getRepeatedRelNextTuplePos() {
		return repeatedRelNextTuplePos;
	}

	public void setRepeatedRelNextTuplePos(
			HashMap<String, Integer[]> repeatedRelNextTuplePos) {
		this.repeatedRelNextTuplePos = repeatedRelNextTuplePos;
	}

	public HashMap<String, Integer> getNoOfTuples() {
		return noOfTuples;
	}

	public void setNoOfTuples(HashMap<String, Integer> noOfTuples) {
		this.noOfTuples = noOfTuples;
	}

	public HashMap<String, Integer> getNoOfOutputTuples() {
		return noOfOutputTuples;
	}

	public void setNoOfOutputTuples(HashMap<String, Integer> noOfOutputTuples) {
		this.noOfOutputTuples = noOfOutputTuples;
	}

	public QueryBlockDetails getOuterBlock() {
		return outerBlock;
	}

	public void setOuterBlock(QueryBlockDetails outerBlock) {
		this.outerBlock = outerBlock;
	}


	public ArrayList<Node> getForeignKeys() {
		return foreignKeys;
	}


	public void setForeignKeys(ArrayList<Node> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}


	public ArrayList<ForeignKey> getForeignKeysModified() {
		return foreignKeysModified;
	}


	public void setForeignKeysModified(ArrayList<ForeignKey> foreignKeysModified) {
		this.foreignKeysModified = foreignKeysModified;
	}


	public ArrayList<String> getConstraints() {
		return constraints;
	}


	public void setConstraints(ArrayList<String> constraints) {
		this.constraints = constraints;
	}


	public ArrayList<String> getStringConstraints() {
		return stringConstraints;
	}


	public void setStringConstraints(ArrayList<String> stringConstraints) {
		this.stringConstraints = stringConstraints;
	}


	public String getCVCStr() {
		return CVCStr;
	}


	public void setCVCStr(String cVCStr) {
		CVCStr = cVCStr;
	}


	public HashMap<Table, Vector<String>> getResultsetTableColumns1() {
		return resultsetTableColumns1;
	}


	public void setResultsetTableColumns1(HashMap<Table, Vector<String>> resultsetTableColumns1) {
		this.resultsetTableColumns1 = resultsetTableColumns1;
	}


	public HashMap<Column, HashMap<String, Integer>> getColNullValuesMap() {
		return colNullValuesMap;
	}


	public void setColNullValuesMap(HashMap<Column, HashMap<String, Integer>> colNullValuesMap) {
		this.colNullValuesMap = colNullValuesMap;
	}

	public StringConstraintSolver getStringSolver() {
		return stringSolver;
	}


	public void setStringSolver(StringConstraintSolver stringSolver) {
		this.stringSolver = stringSolver;
	}


	public Vector<Column> getResultsetColumns() {
		return resultsetColumns;
	}


	public void setResultsetColumns(Vector<Column> resultsetColumns) {
		this.resultsetColumns = resultsetColumns;
	}


	public boolean isFne() {
		return fne;
	}


	public void setFne(boolean fne) {
		this.fne = fne;
	}


	public ArrayList<String> getDatatypeColumns() {
		return datatypeColumns;
	}


	public void setDatatypeColumns(ArrayList<String> datatypeColumns) {
		this.datatypeColumns = datatypeColumns;
	}


	public String getOutput() {
		return output;
	}


	public void setOutput(String output) {
		this.output = output;
	}


	public String getQueryString() {
		return queryString;
	}


	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}


	public String getFilePath() {
		return filePath;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public int getCount() {
		return count;
	}


	public void setCount(int count) {
		this.count = count;
	}


	public ArrayList<Table> getResultsetTables() {
		return resultsetTables;
	}


	public void setResultsetTables(ArrayList<Table> resultsetTables) {
		this.resultsetTables = resultsetTables;
	}


	public boolean isIpdb() {
		return ipdb;
	}


	public void setIpdb(boolean ipdb) {
		this.ipdb = ipdb;
	}


	public String getCVC3_HEADER() {
		return CVC3_HEADER;
	}


	public void setCVC3_HEADER(String cVC3_HEADER) {
		CVC3_HEADER = cVC3_HEADER;
	}


	public GenerateUnionCVC getUnionCVC() {
		return unionCVC;
	}


	public void setUnionCVC(GenerateUnionCVC unionCVC) {
		this.unionCVC = unionCVC;
	}
	
	public int getAssignmentId() {
		return assignmentId;
	} 


	public void setAssignmentId(int id) {
		this.assignmentId = id;
	}
	
	public int getQuestionId() {
		return questionId;
	}


	public void setQuestionId(int qId) {
		this.questionId = qId;
	}	
	public Map<String, TupleRange> getTupleRange(){
		return this.allowedTuples;
	}
	
	public void updateTupleRange(String relation, int x, int y){
		this.allowedTuples.put(relation, new TupleRange(x, y));
	}
	
	public void setTupleRange(Map<String, TupleRange> tupleRange){
		this.allowedTuples = tupleRange;
	}

	/**
	 * This function is used to update the total number of output tuples data structure,
	 * @param queryBlock
	 * @param noOfGroups: Specifies the number of groups to be generated by this query block
	 */
	public  void updateTotalNoOfOutputTuples(QueryBlockDetails queryBlock, int noOfGroups) {

		/**for each base relation in the query block*/
		for(String tableNameNo: queryBlock.getBaseRelations()){

			/**Indicates the count of relation*/
			int prevCount, prevTotCount;

			if( noOfTuples.get( tableNameNo ) != null){

				/**get the count*/
				prevCount = noOfTuples.get(tableNameNo);

				/**total count contributed by this relation*/
				prevTotCount = prevCount * queryBlock.getNoOfGroups();			


				/**get the new total count contributed by this relation*/
				int totCount = prevCount * noOfGroups;

				/**get table name */
				String tableName = tableNameNo.substring(0, tableNameNo.length()-1);

				/**update the total number of output tuples data structre*/
				if( noOfOutputTuples.get(tableName) == null)
					noOfOutputTuples.put(tableNameNo, totCount );
				else
					noOfOutputTuples.put(tableName, noOfOutputTuples.get(tableName)+ totCount - prevTotCount );
			}
		}

		/**Update the number of groups*/
		queryBlock.setNoOfGroups(noOfGroups);

	}


	public BranchQueriesDetails getBranchQueries() {
		return branchQueries;
	}


	public void setBranchQueries(BranchQueriesDetails branchQueries) {
		this.branchQueries = branchQueries;
	}


	public Vector<Table> getTablesOfOriginalQuery() {
		return tablesOfOriginalQuery;
	}


	public void setTablesOfOriginalQuery(Vector<Table> tablesOfOriginalQuery) {
		this.tablesOfOriginalQuery = tablesOfOriginalQuery;
	}


	public String getTypeOfMutation() {
		return typeOfMutation;
	}


	/**sets the type of mutation we are trying to kill*/
	public void setTypeOfMutation(MutationType mutationType, QueryBlock queryBlock) {

		this.typeOfMutation = mutationType.getMutationType() + queryBlock.getQueryBlock();
	}


	public void setTypeOfMutation(String typeOfMutation) {
		this.typeOfMutation = typeOfMutation;
	}


	public HashMap<String, Integer[]> getTableNames() {
		return tableNames;
	}


	public void setTableNames(HashMap<String, Integer[]> tableNames) {
		this.tableNames = tableNames;
	}


	public HashMap<String, Vector<Vector<Node>>> getEquiJoins() {
		return equiJoins;
	}


	public void setEquiJoins(HashMap<String, Vector<Vector<Node>>> equiJoins) {
		this.equiJoins = equiJoins;
	}
	
	public Connection getConnection() {
		return this.connection;
	}
	
	public void setConnection(Connection conn){
		this.connection = conn;
	}
	
	public void initializeConnectionDetails(int assignId, int questionId, int queryId, String course_id) throws Exception {
		
			try(Connection conn = MyConnection.getDatabaseConnection()){
			int connId = 0, schemaId = 0, optionalSchemaId=0;
				
			this.assignmentId = assignId;
			this.questionId = questionId;
			this.setQueryId(queryId);
			this.setCourseId(course_id);
		//	String questionId = queryId.substring(queryId.indexOf("Q")+1,queryId.indexOf("Q")+2);
			int q_id = questionId;
			
			try(PreparedStatement stmt = conn.prepareStatement("select connection_id, defaultschemaid from xdata_assignment where assignment_id = ?")){
				stmt.setInt(1, assignmentId); 
				 
				try(ResultSet result = stmt.executeQuery()){
				
				//Get optional Schema Id for this question
				try(PreparedStatement statement = conn.prepareStatement("select optionalschemaid from xdata_qinfo where assignment_id = ? and question_id= ? ")){
					statement.setInt(1, assignmentId); 
					statement.setInt(2,q_id); 
					
					try(ResultSet resultSet = statement.executeQuery()){
						if(resultSet.next()){
							optionalSchemaId = resultSet.getInt("optionalschemaid");
						}
					}
				}
				
				if(result.next()){
					connId = result.getInt("connection_id");
					//If optional schema id exists and it is not same as default schema id, then set it as schemaId 
					if(optionalSchemaId != 0 && optionalSchemaId != result.getInt("defaultschemaid")){	
						schemaId = optionalSchemaId;
					} else{
						schemaId = result.getInt("defaultschemaid");
					}
				} 
				}
			}
				if(connId != 0 && schemaId != 0){
					 
					try(Connection assignmentConn = new DatabaseConnection().getTesterConnection(assignmentId)){
					PopulateTestData p = new PopulateTestData();
					p.deleteAllTempTablesFromTestUser(assignmentConn);
					byte[] dataBytes = null;
					String tempFile = "";
					FileOutputStream fos = null;
					ArrayList<String> listOfQueries = null;
					ArrayList<String> listOfDDLQueries = new ArrayList<String>();
					String[] inst = null;
					
					if(assignmentConn != null){
					
						try(PreparedStatement stmt1 = conn.prepareStatement("select ddltext from xdata_schemainfo where schema_id = ?")){
						stmt1.setInt(1, schemaId);			
						try(ResultSet result = stmt1.executeQuery()){
						
						    
						// Process the result			
						if(result.next()){
							String fileContent= result.getString("ddltext");
							// CCJSqlParserManager fleParser = new CCJSqlParserManager();
								//Statement parsedStmt = fleParser.parse(new StringReader(fileContent));
							dataBytes = fileContent.getBytes();
							tempFile = "/tmp/dummy";
							
							 fos = new FileOutputStream(tempFile);
							fos.write(dataBytes);
							fos.close();
							
							
							listOfQueries = Utilities.createQueries(tempFile);
							inst = listOfQueries.toArray(new String[listOfQueries.size()]);
							listOfDDLQueries.addAll(listOfQueries);
							for (int i = 0; i < inst.length; i++) {
								// we ensure that there is no spaces before or after the request string  
								// in order to not execute empty statements  
								if (!inst[i].trim().equals("") && ! inst[i].trim().contains("drop table")) {
									//Changed for MSSQL testing
									//String temp = inst[i].replaceAll("(?i)^[ ]*create[ ]+table[ ]+", "create table ##");
									//stmt = assignmentConn.prepareStatement(temp);
									String temp = inst[i].replaceAll("(?i)^[ ]*create[ ]+table[ ]+", "create temporary table ");
									PreparedStatement stmt2 = assignmentConn.prepareStatement(temp);
										stmt2.executeUpdate();	
									stmt2.close();
								
									    
								}
							}
						}
						}
						}
						try(PreparedStatement stmt2 = conn.prepareStatement("select sample_data from xdata_sampledata where schema_id = ?")){
							stmt2.setInt(1, schemaId);			
							try(ResultSet result = stmt2.executeQuery()){
								if(result.next()){
									String sdContent= result.getString("sample_data");
								//	String sdReplace=sdContent.replace("\\\\","'");
									//fc = sdContent.replace("\t", "    ");
									dataBytes = sdContent.getBytes(); 
									fos = new FileOutputStream(tempFile);
									fos.write(dataBytes);
									fos.close();
									
									listOfQueries = Utilities.createQueries(tempFile);
									inst = listOfQueries.toArray(new String[listOfQueries.size()]);
									 
									for (int i = 0; i < inst.length; i++) {
										// we ensure that there is no spaces before or after the request string  
										// in order to not execute empty statements  
										 
										if (!inst[i].trim().equals("") && !inst[i].contains("drop table") && !inst[i].contains("delete from")) {
											//System.out.println(inst[i]);
										//Changed for MSSQL TESTING
											//String temp = inst[i].replaceAll("(?i)^[ ]*insert[ ]+into[ ]+", "insert into [xdata].[dbo].##");
											//stmt = assignmentConn.prepareStatement(temp+";");
											
											PreparedStatement stmt3 = assignmentConn.prepareStatement(inst[i]);
												stmt3.executeUpdate();							
												stmt3.close();
										}
									}
								} 
						}
						}
					} 
					
				
					
					this.connection = assignmentConn;
		
					//this.tableMap = TableMap.getInstances(this.connection,schemaId,listOfDDLQueries);
					this.tableMap = TableMap.getInstances(this.connection,schemaId);
					
				}
			}
			}
		catch(Exception ex){
			//this.closeConn();
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			//ex.printStackTrace();
			throw ex;
		}
	}
	
	public GenerateCVC1 copy() throws Exception{
		//TODO: change implementation to provide faster copy
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();
        out.close();
        
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
        GenerateCVC1    obj = (GenerateCVC1)in.readObject();
		return obj;
	}


	public int getAssignId() {
		// TODO Auto-generated method stub
		return this.getAssignmentId();
	}


	public int getQueryId() {
		return queryId;
	}


	public void setQueryId(int queryId) {
		this.queryId = queryId;
	}


	public String getCourseId() {
		return courseId;
	}


	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}
}

