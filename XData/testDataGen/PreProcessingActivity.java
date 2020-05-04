package testDataGen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.GenerateDataForOriginalQuery;
import killMutations.MutationsInOuterBlock;
import killMutations.outerQueryBlock.SetOperatorMutations;
//import killMutations.outerQueryBlock.SetOperatorMutations;
import parsing.ConjunctQueryStructure;
import parsing.ForeignKey;
import parsing.Node;
import parsing.QueryParser;
import parsing.QueryStructure;
import parsing.RelationHierarchyNode;
import parsing.Table;
import util.Configuration;
import util.TagDatasets;
import util.Utilities;

/**
 * This class contains functions to do pre processing actions before actual data generation is done
 * @author mahesh
 *
 */
public class PreProcessingActivity {

	private static Logger logger = Logger.getLogger(PreProcessingActivity.class.getName());
	public static GenerateCVC1 convertSetQuerytoSubquery(GenerateCVC1 left, GenerateCVC1 right,String subqueryOpNode) throws Exception{

		List<Node> projectedRight=right.getOuterBlock().getProjectedCols();
		List<Node> projectLeft=left.getOuterBlock().getProjectedCols();

		Iterator<Node> rightIter=projectedRight.iterator();
		Iterator<Node> leftIter=projectLeft.iterator();

		//Setting projected attributes as corelation conditions
		//TODO correlation conditions are treated as joins now - need to change this
		while(leftIter.hasNext() && rightIter.hasNext()) {
			Vector<Node> v=new Vector<Node>();
			v.add(leftIter.next());
			v.add(rightIter.next());
			List<ConjunctQueryStructure> rightConjuncts=right.getOuterBlock().getConjunctsQs();
			if(rightConjuncts ==null || rightConjuncts.isEmpty()) {

				ConjunctQueryStructure c=new ConjunctQueryStructure(new Vector<Node>());
				right.getOuterBlock().getConjunctsQs().add(c);
			}

			rightConjuncts.get(0).getEquivalenceClasses().add(v);
		}


		Node n = new Node();
		n.setType(subqueryOpNode);


		Node subQnode=new Node();
		subQnode.setQueryType(2);

		n.setLhsRhs(subQnode);
		List<ConjunctQueryStructure> leftConjuncts=left.getOuterBlock().getConjunctsQs();
		if(leftConjuncts ==null || leftConjuncts.isEmpty()) {

			ConjunctQueryStructure c=new ConjunctQueryStructure(new Vector<Node>());
			left.getOuterBlock().getConjunctsQs().add(c);
		}

		n.setQueryIndex(leftConjuncts.get(0).allSubQueryConds.size());
		leftConjuncts.get(0).allSubQueryConds.add(n);

		left.getOuterBlock().getWhereClauseSubQueries().add(right.getOuterBlock());
		GenerateCVC1 cvcSetop= left;

		//cvcSetop.getBranchQueries().intitializeDetails(cvcSetop);	
		//RelatedToPreprocessing.populateData(cvcSetop);

		//base relations will be added in initializeOtherDetais()
		cvcSetop.getOuterBlock().setBaseRelations(new ArrayList<String>());

		//TODO:put tableNames with proper query type and query index
		cvcSetop.getBaseRelation().putAll(right.getBaseRelation());
		//cvcSetop.getCurrentIndexCount().putAll(right.getCurrentIndexCount());
		//cvcSetop.getNoOfOutputTuples().putAll(right.getNoOfOutputTuples());
		HashMap<String, Integer[]> hm = right.getTableNames();
		Iterator <String> iterator = hm.keySet().iterator();
		while (iterator.hasNext())
		{  
			String key = iterator.next().toString();  
			Integer[] value = hm.get(key);  
			if(cvcSetop.getTableNames().containsKey(key))
			{
				//FIXME:What if number of repeated relations is in double digits
				Integer i = Integer.parseInt(key.substring(key.length()-1));
				i += 1;
				key = key.substring(0, key.length()-1) + i;
				cvcSetop.getTableNames().put(key, value);
			}
			else
				cvcSetop.getTableNames().put(key,value);
		}

		PreProcessingActivity.setOriginalTablesForSetQuery(cvcSetop, right);


		cvcSetop.initializeOtherDetails();
		RelatedToPreprocessing.segregateSelectionConditions(cvcSetop);

		//Correcting differences observed
		ArrayList<RelationHierarchyNode> rhnList = new ArrayList<RelationHierarchyNode>();
		rhnList.add(cvcSetop.getOuterBlock().getWhereClauseSubQueries().get(0).getTopLevelRelation());
		cvcSetop.getOuterBlock().getTopLevelRelation().setNotExistsSubQueries(rhnList);
		cvcSetop.getOuterBlock().getWhereClauseSubQueries().get(0).setTopLevelRelation(null);
		//cvcSetop.getOuterBlock().setBaseRelations(left.getOuterBlock().getBaseRelations());
		//temp
		cvcSetop.getOuterBlock().getConjunctsQs().get(0).getAllSubQueryConds().get(0).getLhsRhs().setQueryIndex(0);
		cvcSetop.getOuterBlock().getConjunctsQs().get(0).getAllSubQueryConds().get(0).setQueryIndex(-1);
		//Conjunct c=new Conjunct(new Vector<Node>());
		//ArrayList<Conjunct> conjList = new ArrayList<Conjunct>();
		//cvcSetop.getOuterBlock().getWhereClauseSubQueries().get(0).setConjuncts(conjList);
		//cvcSetop.getOuterBlock().getWhereClauseSubQueries().get(0).getConjuncts().add(c);
		cvcSetop.getColNullValuesMap().putAll(right.getColNullValuesMap());

		//Also works for other set operator queries, only not null required
		cvcSetop.getqStructure().setOperator = "UNION";

		return cvcSetop;
	}


	public static void setOriginalTablesForSetQuery(GenerateCVC1 left,GenerateCVC1 right){
		left.setTablesOfOriginalQuery( new Vector<Table>() );
		left.getTablesOfOriginalQuery().addAll(left.getQuery().getFromTables().values());
		left.getTablesOfOriginalQuery().addAll(right.getQuery().getFromTables().values());
		Iterator<Table> iter1 = left.getTablesOfOriginalQuery().iterator();
		while(iter1.hasNext())
		{
			Table t = (Table)iter1.next();
			if(t.hasForeignKey())
			{
				Map<String, ForeignKey> fks = t.getForeignKeys();
				Iterator<ForeignKey> iter2 = fks.values().iterator();
				while(iter2.hasNext())
				{
					ForeignKey fk = (ForeignKey)iter2.next();
					if(!left.getTablesOfOriginalQuery().contains(fk.getReferenceTable()))
					{
						left.getTablesOfOriginalQuery().add(fk.getReferenceTable());
						iter1 = left.getTablesOfOriginalQuery().iterator();
					}
				}
			}
		}

		right.setTablesOfOriginalQuery(left.getTablesOfOriginalQuery());

	}	

	//Application Testing
	public static Vector<Node> preProcessingActivityForSchema(GenerateCVC1 cvc) throws Exception{


		/** check if there are branch queries and upload the details */
		//TODO: This is for application testing, a flag should be set
		//for calling this function
		RelatedToPreprocessing.uploadBranchQueriesDetails(cvc);
		/** To store input query string */
		String queryString = "";
		boolean isSetOp = false;
		BufferedReader input = null;
		StringBuffer queryStr = new StringBuffer();
		try {
			input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_smt" + cvc.getFilePath() + "/queries.txt"));
			/**Read the input query */
			while (( queryString = input.readLine()) != null){
				queryStr.append(queryString+"\n");
			}
			if(queryStr != null){
				/**Create a new query parser
			cvc.setqParser( new QueryParser(cvc.getTableMap()));
			cvc.getqParser().parseQuery("q1", queryStr.toString());
				 */
				//Trying with new query structure
				QueryStructure qStructure=new QueryStructure(cvc.getTableMap());
				qStructure.buildQueryStructure("q1", queryStr.toString(),cvc.getDBAppparams());
				cvc.setqStructure(qStructure);


				cvc.getDBAppparams().setSchemaProjectedColumns(cvc.getqStructure().getProjectedCols());				
				return cvc.getDBAppparams().getSchemaProjectedColumns();

				//end


			}
		}catch(Exception e){
			logger.log(Level.SEVERE,""+e.getStackTrace(),e);
			//e.printStackTrace();
			throw e;
		} 
		finally {
			if(cvc != null &&  cvc.getConnection() != null && ! cvc.getConnection().isClosed()){
				cvc.closeConn();
			}
			if(input != null)
				input.close();
		}
		return cvc.getDBAppparams().getSchemaProjectedColumns();
	}
	//end

	public static void preProcessingActivity(GenerateCVC1 cvc) throws Exception{


		/** check if there are branch queries and upload the details */
		//TODO: This is for application testing, a flag should be set
		//for calling this function
		if(Configuration.calledFromApplicationTester) {
			RelatedToPreprocessing.uploadBranchQueriesDetails(cvc);
		}
		/** To store input query string */
		String queryString = "";
		boolean isSetOp = false;
		BufferedReader input = null;
		StringBuffer queryStr = new StringBuffer();
		try {
			input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_smt" + cvc.getFilePath() + "/queries.txt"));
			/**Read the input query */
			while (( queryString = input.readLine()) != null){
				queryStr.append(queryString+"\n");
			}
			if(queryStr != null){
				/**Create a new query parser
			cvc.setqParser( new QueryParser(cvc.getTableMap()));
			cvc.getqParser().parseQuery("q1", queryStr.toString());
				 */
				//Trying with new query structure
				QueryStructure qStructure=new QueryStructure(cvc.getTableMap());
				qStructure.buildQueryStructure("q1", queryStr.toString(),cvc.getDBAppparams());
				cvc.setqStructure(qStructure);

				//Application Testing --- not need to execute further statements for schema parsing

				if(cvc.getDBAppparams().isSchemasetFlag()== true){
					cvc.getDBAppparams().setSchemaProjectedColumns(cvc.getqStructure().getProjectedCols());				
					return;
				}
				//end

				cvc.initializeQueryDetailsQStructure(cvc.getqStructure() );

				ArrayList<ForeignKey> foreignK = new ArrayList<ForeignKey>();

				for(ForeignKey key : cvc.getForeignKeysModified()) {
					if(!foreignK.contains(key)) {
						foreignK.add(key);
					}
				}
				cvc.setForeignKeysModified(foreignK);
				logger.log(Level.INFO," Query Parser output = "+ cvc.getqStructure());
				/**Delete data sets in the path*/
				//RelatedToPreprocessing.deleteDatasets(cvc.getFilePath());

				logger.log(Level.INFO,"File path = "+cvc.getFilePath());
				/**Check if the input query contains set operators */
				logger.log(Level.INFO,"cvc.getqParser().setOperator = " + cvc.getqStructure().setOperator);
				if(cvc.getqStructure().setOperator!=null && cvc.getqStructure().setOperator.length()>0){
					isSetOp = true;
					genDataForSetOp(cvc,cvc.getqStructure().setOperator);

				}
				/*else if(cvc.getqParser() != null && (cvc.getqParser().getFromClauseSubqueries() != null 
						|| cvc.getqParser().getWhereClauseSubqueries() != null) ){

					for(int i=0;i<cvc.getqParser().getFromClauseSubqueries().size();i++){
						QueryParser qpFrom = cvc.getqParser().getFromClauseSubqueries().get(i);
						if(qpFrom.setOperator!=null && qpFrom.setOperator.length()>0){
							isSetOp = true;
							genDataForSetOp(cvc,qpFrom.setOperator);

						}
					}

					for(int i=0;i<cvc.getqParser().getWhereClauseSubqueries().size();i++){
						QueryParser qpWhere = cvc.getqParser().getWhereClauseSubqueries().get(i);
						if(qpWhere.setOperator!=null && qpWhere.setOperator.length()>0){
							isSetOp = true;
							genDataForSetOp(cvc,qpWhere.setOperator);
						}
					}
				}*/

				else{

					cvc.getBranchQueries().intitializeDetails(cvc);

					/**Populate the values from the data base 
					 * Needed so that the generated values looks realistic */	
					RelatedToPreprocessing.populateData(cvc);

					/**Initialize cvc3 headers etc>,*/				
					cvc.initializeOtherDetails();
					cvc.setConstraintSolver(Configuration.getProperty("smtsolver"));

					if(Configuration.getProperty("smtsolver").equalsIgnoreCase("cvc3")){
						cvc.setSolverSpecificCommentCharacter("%");
					}else{
						cvc.setSolverSpecificCommentCharacter(";");
					}

					/** Segregate selection conditions in each query block */
					RelatedToPreprocessing.segregateSelectionConditions(cvc);

					/** Call the method for generating the data sets */
					//if(Configuration.getProperty("smtsolver").equalsIgnoreCase("cvc3")){
					cvc.generateDatasetsToKillMutations();
					//}else{
					//cvc.generateDatasetsToKillMutationsUsingSMT();
					//}

				}
				cvc.closeConn();
			}
		}catch(Exception e){
			logger.log(Level.SEVERE,""+e.getStackTrace(),e);
			e.printStackTrace();
			throw e;
		} 
		finally {
			if(cvc != null &&  cvc.getConnection() != null && ! cvc.getConnection().isClosed()){
				cvc.closeConn();
			}
			if(input != null)
				input.close();
		}
	}

	public static void genDataForSetOp(GenerateCVC1 cvc,String setOp) throws Exception{


		//String setOp=cvc.getqParser().setOperator;
		int count=0;
		GenerateCVC1 cvcCopy;

		if(setOp.equalsIgnoreCase("UNION")) {

			cvcCopy= (GenerateCVC1)Utilities.copy(cvc);
			cvcCopy.setConnection(cvc.getConnection());
			//**create object for handling set operations
			GenerateUnionCVC unionCVC= new GenerateUnionCVC(cvcCopy, cvcCopy.getqStructure());


			/*RelatedToPreprocessing.populateData(cvc);

				//**Generate CVC3 Header, This is need to initialize the CVC3 Data Type field of each column of each table 
				cvc.setCVC3_HEADER( GetCVC3HeaderAndFooter.generateCVC3_Header( cvc ) );

				cvc.getUnionCVC().generateDataForApp();

				cvc.getUnionCVC().generateToKill();

				continue;*/
			boolean flag=false;

			String mutationType = TagDatasets.MutationType.ORIGINAL.getMutationType() + TagDatasets.QueryBlock.NONE.getQueryBlock();
			logger.log(Level.INFO," mutationType = "+ mutationType);
			flag=GenerateDataForOriginalQuery.generateDataForOriginalQuery(unionCVC.genCVCleft, mutationType);
			logger.log(Level.INFO," flag variable value = "+ flag);
			if(flag==false) {
				GenerateDataForOriginalQuery.generateDataForOriginalQuery(unionCVC.genCVCright, mutationType);
			}


			//DS0 is generated
			count=1;

			//killing mutations in subqueries of set operators

			cvcCopy=(GenerateCVC1)Utilities.copy(cvc);
			cvcCopy.setConnection(cvc.getConnection());
			unionCVC= new GenerateUnionCVC(cvcCopy, cvcCopy.getqStructure());

			GenerateCVC1 cvcUnion= convertSetQuerytoSubquery(unionCVC.genCVCleft,unionCVC.genCVCright, Node.getNotExistsNodeType());
			cvcUnion.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
			cvcUnion.setRepeatedRelNextTuplePos(unionCVC.getGenCVC().getRepeatedRelNextTuplePos());
			cvcUnion.setResultsetTables(unionCVC.getGenCVC().getResultsetTables());
			cvcUnion.setResultsetColumns(unionCVC.getGenCVC().getResultsetColumns());
			cvcUnion.setNoOfOutputTuples(unionCVC.getGenCVC().getNoOfOutputTuples());
			cvcUnion.setForeignKeys(unionCVC.getGenCVC().getForeignKeys());
			cvcUnion.setForeignKeysModified(unionCVC.getGenCVC().getForeignKeysModified());
			cvcUnion.setUnionCVC(unionCVC);
			//cvcUnion.initializeQueryDetails(cvcUnion.getqParser());
			cvcUnion.setCount(count);
			MutationsInOuterBlock.generateDataForKillingMutantsInOuterQueryBlock(cvcUnion);
			count=cvcUnion.getCount();

			cvcCopy=(GenerateCVC1)Utilities.copy(cvc);
			cvcCopy.setConnection(cvc.getConnection());
			unionCVC= new GenerateUnionCVC(cvcCopy, cvcCopy.getqStructure());

			//unionCVC= new GenerateUnionCVC(cvc, cvc.getqParser());
			cvcUnion= convertSetQuerytoSubquery(unionCVC.genCVCright,unionCVC.genCVCleft, Node.getNotExistsNodeType());
			cvcUnion.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
			cvcUnion.setRepeatedRelNextTuplePos(unionCVC.getGenCVC().getRepeatedRelNextTuplePos());
			cvcUnion.setResultsetTables(unionCVC.getGenCVC().getResultsetTables());
			cvcUnion.setResultsetColumns(unionCVC.getGenCVC().getResultsetColumns());
			cvcUnion.setNoOfOutputTuples(unionCVC.getGenCVC().getNoOfOutputTuples());
			cvcUnion.setForeignKeys(unionCVC.getGenCVC().getForeignKeys());
			cvcUnion.setForeignKeysModified(unionCVC.getGenCVC().getForeignKeysModified());
			cvcUnion.setUnionCVC(unionCVC);
			//cvcUnion.initializeQueryDetails(cvcUnion.getqParser());
			cvcUnion.setCount(count);
			MutationsInOuterBlock.generateDataForKillingMutantsInOuterQueryBlock(cvcUnion);
			count=cvcUnion.getCount();

			//cvcCopy=cvc.copy();
			//unionCVC= new GenerateUnionCVC(cvcCopy, cvcCopy.getqParser());
			//SetOperatorMutations.generateDataToKillSetOperatorMutations(unionCVC);


		} else if (setOp.equalsIgnoreCase("INTERSECT")) {
			cvcCopy = (GenerateCVC1)Utilities.copy(cvc);
			cvcCopy.setConnection(cvc.getConnection());
			GenerateUnionCVC unionCVC= new GenerateUnionCVC(cvcCopy, cvcCopy.getqStructure());
			GenerateCVC1 cvcIntersect= convertSetQuerytoSubquery(unionCVC.genCVCleft,unionCVC.genCVCright, Node.getExistsNodeType());
			cvcIntersect.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
			cvcIntersect.setRepeatedRelNextTuplePos(unionCVC.getGenCVC().getRepeatedRelNextTuplePos());
			cvcIntersect.setResultsetTables(unionCVC.getGenCVC().getResultsetTables());
			cvcIntersect.setResultsetColumns(unionCVC.getGenCVC().getResultsetColumns());
			cvcIntersect.setNoOfOutputTuples(unionCVC.getGenCVC().getNoOfOutputTuples());
			cvcIntersect.setForeignKeys(unionCVC.getGenCVC().getForeignKeys());
			cvcIntersect.setForeignKeysModified(unionCVC.getGenCVC().getForeignKeysModified());
			cvcIntersect.setUnionCVC(unionCVC);
			cvcIntersect.generateDatasetsToKillMutations();
			count=cvcIntersect.getCount();

		} else if (setOp.equalsIgnoreCase("EXCEPT")) {
			cvcCopy = (GenerateCVC1)Utilities.copy(cvc);
			cvcCopy.setConnection(cvc.getConnection());
			GenerateUnionCVC unionCVC= new GenerateUnionCVC(cvcCopy, cvcCopy.getqStructure());
			GenerateCVC1 cvcExcept= convertSetQuerytoSubquery(unionCVC.genCVCleft,unionCVC.genCVCright, Node.getNotExistsNodeType());
			cvcExcept.setRepeatedRelationCount(unionCVC.getGenCVC().getRepeatedRelationCount());
			cvcExcept.setRepeatedRelNextTuplePos(unionCVC.getGenCVC().getRepeatedRelNextTuplePos());
			cvcExcept.setResultsetTables(unionCVC.getGenCVC().getResultsetTables());
			cvcExcept.setResultsetColumns(unionCVC.getGenCVC().getResultsetColumns());
			cvcExcept.setNoOfOutputTuples(unionCVC.getGenCVC().getNoOfOutputTuples());
			cvcExcept.setForeignKeys(unionCVC.getGenCVC().getForeignKeys());
			cvcExcept.setForeignKeysModified(unionCVC.getGenCVC().getForeignKeysModified());
			cvcExcept.setUnionCVC(unionCVC);
			cvcExcept.generateDatasetsToKillMutations();
			count=cvcExcept.getCount();
		}

		//Data generation for SETOP
		cvcCopy= (GenerateCVC1)Utilities.copy(cvc);
		cvcCopy.setConnection(cvc.getConnection());
		cvcCopy.setCount(count);
		GenerateUnionCVC unionCVC = new GenerateUnionCVC(cvcCopy, cvcCopy.getqStructure());
		unionCVC.getGenCVC().setCount(count);
		SetOperatorMutations.generateDataToKillSetOperatorMutations(unionCVC);




	}
}
