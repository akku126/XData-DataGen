package testDataGen;

import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.GetCVC3HeaderAndFooter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import killMutations.outerQueryBlock.SelectionMutationsInOuterQueryBlock;
import parsing.AggregateFunction;
import parsing.Column;

import parsing.ConjunctQueryStructure;
import parsing.ForeignKey;
import parsing.Node;
import parsing.QueryParser;
import parsing.QueryStructure;
import parsing.Table;

public class GenerateUnionCVC implements Serializable{
	
	QueryParser qParser;
	QueryParser leftqParser;
	QueryParser rightqParser;
	
	QueryStructure qStructure;
	QueryStructure leftqStructure;
	QueryStructure rightqStructure;
	
	GenerateCVC1 genCVC;
	GenerateCVC1 genCVCleft;
	GenerateCVC1 genCVCright; 
	
	private static Logger logger = Logger.getLogger(GenerateUnionCVC.class.getName());
	/*public GenerateUnionCVC(GenerateCVC1 cvc,QueryParser qParser) throws Exception{
		this.qParser=qParser;
		this.genCVC=cvc;
		leftqParser=qParser.leftQuery;
		rightqParser=qParser.rightQuery;
		
		genCVCleft=new GenerateCVC1();
		genCVCleft.setFne(false);
		genCVCleft.setIpdb(false);
		genCVCleft.setFilePath(cvc.getFilePath());
		genCVCleft.initializeQueryDetails(leftqParser);
		genCVCleft.setqParser(leftqParser);
		genCVCleft.setQuery(genCVCleft.getqParser().getQuery());
		genCVCleft.setQueryString(genCVCleft.getQuery().getQueryString());
		genCVCleft.setRepeatedRelationCount(genCVCleft.getQuery().getRepeatedRelationCount());
		genCVCleft.setNoOfOutputTuples((HashMap<String,Integer>)genCVCleft.getQuery().getRepeatedRelationCount().clone());
		//genCVCleft.setAssignmentId(cvc.getAssignmentId());
		//genCVCleft.setQuestionId(cvc.getQuestionId());
		RelatedToPreprocessing.populateData(genCVCleft);
		genCVCleft.setTableMap(cvc.getTableMap());
		genCVCleft.initializeOtherDetails();
	//	genCVCleft.allConds = genCVCleft.qParser.getAllConds();
	//	genCVCleft.selectionConds = genCVCleft.qParser.getSelectionConds();
	//	genCVCleft.stringSelectionConds = new Vector<Node>();
	//	genCVCleft.likeConds=new Vector<Node>();
	//	genCVCleft.isNullConds= qParser.getIsNullConds();
	//	genCVCleft.getOuterBlock().aggConstraints=new Vector<Node>();
	//	genCVCleft.equivalenceClasses = genCVCleft.qParser.getEquivalenceClasses();
	//	genCVCleft.joinConds = genCVCleft.qParser.getJoinConds();
		genCVCleft.setForeignKeys(new ArrayList<Node>(genCVCleft.getqParser().getForeignKeys()));	
		genCVCleft.outerBlock.setProjectedCols(new ArrayList<Node>(genCVCleft.getqParser().getProjectedCols()));
	//	genCVCleft.constraintsWithParameters = new HashMap<String, Node>();
		//Data structures for Aggregation
		genCVCleft.outerBlock.setAggFunc(new ArrayList<AggregateFunction>(genCVCleft.getqParser().getAggFunc()));
		genCVCleft.outerBlock.setGroupByNodes(new ArrayList<Node>(getqParser().getGroupByNodes()));
		genCVCleft.outerBlock.setHavingClause(genCVCleft.getqParser().getHavingClause());
	//	genCVCleft.allSubQueryConds = genCVCleft.qParser.getAllSubQueryConds();
	//	genCVCleft.allCondsExceptSubQuery = genCVCleft.qParser.getAllCondsExceptSubQuery();
	//	genCVCleft.root = genCVCleft.getqParser().getRoot();		
	//	genCVCleft.aggConstraints = new Vector<Node>();
	//	genCVCleft.aggConstraints.removeAllElements();	
		//DNF
	//	genCVCleft.dnfSelCond=genCVCleft.qParser.getAllDnfSelCond();
	//	genCVCleft.dnfLikeCond=genCVCleft.qParser.getDnfLikeConds();
	//	genCVCleft.dnfSubQuery=genCVCleft.qParser.getAllDnfSubQuery();
	//	genCVCleft.EqClass=genCVCleft.qParser.getEqClass();
		genCVCleft.outerBlock.setConjuncts(new ArrayList<Conjunct>(genCVCleft.getqParser().getConjuncts()));
		RelatedToParameters.setupDataStructuresForParamConstraints(genCVCleft.outerBlock);
		GetCVC3HeaderAndFooter.generateCVC3_Header(genCVCleft);
		genCVCleft.setForeignKeysModified(new ArrayList<ForeignKey>(qParser.leftQuery.getForeignKeyVectorModified()));
//		genCVCleft.dnfStrCond=new Vector<Vector<Node>>();
//		genCVCleft.seggregateDnfSelConditions();	
		//for(Conjunct conjunct : genCVCleft.outerBlock.getConjuncts()){
		//	conjunct.seggregateSelectionConds();
		//}
		RelatedToPreprocessing.segregateSelectionConditions(genCVCleft);
		
		
		
			
		//End transfered
		
		
		
		
		genCVCright=new GenerateCVC1();
		genCVCright.setFne(false);
		genCVCright.setIpdb(false);		
		genCVCright.setFilePath(cvc.getFilePath());
		genCVCright.initializeQueryDetails(rightqParser);
		genCVCright.setqParser(rightqParser);
		genCVCright.setQuery(genCVCright.getqParser().getQuery());
		genCVCright.setQueryString(genCVCright.getQuery().getQueryString());
		genCVCright.setRepeatedRelationCount(genCVCright.getQuery().getRepeatedRelationCount());
		
		genCVCright.setNoOfOutputTuples((HashMap<String,Integer>)genCVCright.getQuery().getRepeatedRelationCount().clone());
		//genCVCright.setAssignmentId(cvc.getAssignmentId());
		//genCVCright.setQuestionId(cvc.getQuestionId());
		RelatedToPreprocessing.populateData(genCVCright);
		genCVCright.setTableMap(cvc.getTableMap());
		genCVCright.initializeOtherDetails();
//		genCVCright.allConds = genCVCright.qParser.getAllConds();
//		genCVCright.selectionConds = genCVCright.qParser.getSelectionConds();
//		genCVCright.stringSelectionConds = new Vector<Node>();
//		genCVCright.likeConds=new Vector<Node>();
//		genCVCright.isNullConds= qParser.getIsNullConds();
//		genCVCright.aggConstraints=new Vector<Node>();			
//		genCVCright.equivalenceClasses = genCVCright.qParser.getEquivalenceClasses();
//		genCVCright.joinConds = genCVCright.qParser.getJoinConds();
		genCVCright.setForeignKeys(new ArrayList(genCVCright.getqParser().getForeignKeys()));	
		genCVCright.outerBlock.setProjectedCols(new ArrayList(genCVCright.getqParser().getProjectedCols()));
		//genCVCright.constraintsWithParameters = new HashMap<String, Node>();
		//genCVCright.dnfSelCond=genCVCright.qParser.getAllDnfSelCond();
		//genCVCright.dnfLikeCond=genCVCright.qParser.getDnfLikeConds();
		//genCVCright.dnfSubQuery=genCVCright.qParser.getAllDnfSubQuery();
		//genCVCright.EqClass=genCVCright.qParser.getEqClass();
		genCVCright.outerBlock.setConjuncts(new ArrayList(genCVCright.getqParser().getConjuncts()));
		//Data structures for Aggregation
		genCVCright.outerBlock.setAggFunc(new ArrayList(genCVCright.getqParser().getAggFunc()));
		genCVCright.outerBlock.setGroupByNodes(new ArrayList(genCVCright.getqParser().getGroupByNodes()));
		genCVCright.outerBlock.setHavingClause(genCVCright.getqParser().getHavingClause());
	//	genCVCright.allSubQueryConds = genCVCright.qParser.getAllSubQueryConds();
	//	genCVCright.allCondsExceptSubQuery = genCVCright.qParser.getAllCondsExceptSubQuery();
	//	genCVCright.root = genCVCright.qParser.getRoot();		
	//	genCVCright.aggConstraints = new Vector<Node>();
	//	genCVCright.aggConstraints.removeAllElements();		
		RelatedToParameters.setupDataStructuresForParamConstraints(genCVCright.outerBlock);
		GetCVC3HeaderAndFooter.generateCVC3_Header(genCVCright);
		genCVCright.setForeignKeysModified(new ArrayList(qParser.rightQuery.getForeignKeyVectorModified()));
	//	genCVCright.dnfStrCond=new Vector<Vector<Node>>();
	//	genCVCright.seggregateDnfSelConditions();
		//for(Conjunct conjunct : genCVCright.outerBlock.getConjuncts()){
		//	conjunct.seggregateSelectionConds();
		//}
		RelatedToPreprocessing.segregateSelectionConditions(genCVCright);
		//Create New CVC Object with combined Constraints
//		cvc.initializeQueryDetails(qParser);
//		cvc.setQuery(cvc.getqParser().getQuery());
//		cvc.setQueryString(cvc.getQuery().getQueryString());
//		cvc.setRepeatedRelationCount(getRepeatedRelationCountForApp());		
//		cvc.setResultsetTables(new ArrayList(getResultSetTables()));
//		cvc.setResultsetColumns(getResultSetColumns());
//		cvc.setNoOfOutputTuples(getNoOfOutputTuplesForApp());
//		cvc.allConds = getAllCondsForApp();
//		cvc.selectionConds = getSelectionCondsForApp();
//		cvc.stringSelectionConds=getStringSelectionCondsForApp();
//		cvc.likeConds=getLikeCondsForApp();
//		cvc.isNullConds= getIsNullCondsForApp();
//		cvc.outerBlock.setAggConstraints(new ArrayList(getAggConstraintsForApp()));
//		cvc.equivalenceClasses = getEquivalenceClassesForApp();
//		cvc.joinConds = getJoinCondsForApp();
//		cvc.setForeignKeys(new ArrayList(getForeignKeysForApp()));
//		cvc.constraintsWithParameters = new HashMap<String, Node>();
//		cvc.outerBlock.setParamMap(getParamMapForApp());		
		
		//Data structures for Aggregation
		//TODO add propoer function for following datatype
//		cvc.outerBlock.setAggFunc(new ArrayList(getAggFuncForApp()));
//		cvc.outerBlock.setGroupByNodes(new ArrayList(getGroupByNodesForApp()));
		
//		cvc.outerBlock.setHavingClause(getHavingClauseForApp());
	//	cvc.allSubQueryConds = getAllSubQueryCondsForApp();
	//	cvc.allCondsExceptSubQuery = getAllCondsExceptSubQueryForApp();
//		cvc.root = qParser.getRoot();
		
//		cvc.aggConstraints = new Vector<Node>();
//		cvc.aggConstraints.removeAllElements();		
		//End transfered
		
//		RelatedToParameters.setupDataStructuresForParamConstraints(cvc.outerBlock);
		//cvc.setupDataStructuresForParamConstraints();		
//		GetCVC3HeaderAndFooter.generateCVC3_Header(cvc);
	//	cvc.seggregateSelectionConds();	
//		cvc.outerBlock.setConjuncts(new ArrayList(getConjunctsForApp()));
	//	cvc.dnfSelCond=getDNFSelCondsForApp();
	//	cvc.dnfLikeCond=getdnfLikeCondForApp();
	//	cvc.dnfStrCond=getdnfStrCondForApp();
	//	cvc.dnfSubQuery=getdnfSubQueryForApp();
//		cvc.setForeignKeysModified(new ArrayList(getForeignKeyModifiedForApp()));
	
		genCVC.setRepeatedRelationCount(getRepeatedRelationCountForApp());
		
		genCVC.setRepeatedRelNextTuplePos(getRepeatedRelNextTuplePosForApp());
		Vector<Table> tmp = getResultSetTables();
		ArrayList<Table> temp = new ArrayList<Table>();
		temp.addAll(tmp);
		genCVC.setResultsetTables(temp);
		genCVC.setResultsetColumns(getResultSetColumns());
		genCVC.setNoOfOutputTuples(getNoOfOutputTuplesForApp());
		Vector<Node> tmpNode = getForeignKeysForApp();
		ArrayList<Node> tempNode = new ArrayList<Node>();
		tempNode.addAll(tmpNode);
		genCVC.setForeignKeys(tempNode);
		Vector<ForeignKey> tmpFKey = getForeignKeyModifiedForApp();
		ArrayList<ForeignKey> tempFKey = new ArrayList<ForeignKey>();
		tempFKey.addAll(tmpFKey);
		genCVC.setForeignKeysModified(tempFKey);
	}*/
	
	private Vector<ForeignKey> getForeignKeyModifiedForApp(){
		Vector<ForeignKey> fk=new Vector<ForeignKey>();
		fk.addAll(genCVCleft.getForeignKeysModified());
		fk.addAll(genCVCright.getForeignKeysModified());
		return fk;
	}
	
	public GenerateUnionCVC(GenerateCVC1 cvc,QueryStructure qStructure) throws Exception{
		this.qStructure=qStructure;
		this.genCVC=cvc;
		leftqStructure=qStructure.leftQuery;
		rightqStructure=qStructure.rightQuery;
		
		genCVCleft=new GenerateCVC1();
		genCVCleft.setFne(false);
		genCVCleft.setIpdb(false);
		genCVCleft.setConnection(cvc.getConnection());
		genCVCleft.setFilePath(cvc.getFilePath());
		genCVCleft.initializeQueryDetailsQStructure(leftqStructure);
		genCVCleft.setqStructure(leftqStructure);
		genCVCleft.setQuery(genCVCleft.getqStructure().getQuery());
		genCVCleft.setQueryString(genCVCleft.getQuery().getQueryString());
		genCVCleft.setRepeatedRelationCount(genCVCleft.getQuery().getRepeatedRelationCount());
		genCVCleft.setNoOfOutputTuples((HashMap<String,Integer>)genCVCleft.getQuery().getRepeatedRelationCount().clone());
		//genCVCleft.setAssignmentId(cvc.getAssignmentId());
		//genCVCleft.setQuestionId(cvc.getQuestionId());
		RelatedToPreprocessing.populateData(genCVCleft);
		genCVCleft.setTableMap(cvc.getTableMap());
		genCVCleft.initializeOtherDetails();
	//	genCVCleft.allConds = genCVCleft.qParser.getAllConds();
	//	genCVCleft.selectionConds = genCVCleft.qParser.getSelectionConds();
	//	genCVCleft.stringSelectionConds = new Vector<Node>();
	//	genCVCleft.likeConds=new Vector<Node>();
	//	genCVCleft.isNullConds= qParser.getIsNullConds();
	//	genCVCleft.getOuterBlock().aggConstraints=new Vector<Node>();
	//	genCVCleft.equivalenceClasses = genCVCleft.qParser.getEquivalenceClasses();
	//	genCVCleft.joinConds = genCVCleft.qParser.getJoinConds();
		genCVCleft.setForeignKeys(new ArrayList<Node>(genCVCleft.getqStructure().getForeignKeys()));	
		genCVCleft.outerBlock.setProjectedCols(new ArrayList<Node>(genCVCleft.getqStructure().getProjectedCols()));
	//	genCVCleft.constraintsWithParameters = new HashMap<String, Node>();
		//Data structures for Aggregation
		genCVCleft.outerBlock.setAggFunc(new ArrayList<AggregateFunction>(genCVCleft.getqStructure().getAggFunc()));
		genCVCleft.outerBlock.setGroupByNodes(new ArrayList<Node>(genCVCleft.getqStructure().getGroupByNodes()));
		genCVCleft.outerBlock.setHavingClause(genCVCleft.getqStructure().getHavingClause());
	//	genCVCleft.allSubQueryConds = genCVCleft.qParser.getAllSubQueryConds();
	//	genCVCleft.allCondsExceptSubQuery = genCVCleft.qParser.getAllCondsExceptSubQuery();
	//	genCVCleft.root = genCVCleft.getqParser().getRoot();		
	//	genCVCleft.aggConstraints = new Vector<Node>();
	//	genCVCleft.aggConstraints.removeAllElements();	
		//DNF
	//	genCVCleft.dnfSelCond=genCVCleft.qParser.getAllDnfSelCond();
	//	genCVCleft.dnfLikeCond=genCVCleft.qParser.getDnfLikeConds();
	//	genCVCleft.dnfSubQuery=genCVCleft.qParser.getAllDnfSubQuery();
	//	genCVCleft.EqClass=genCVCleft.qParser.getEqClass();
		genCVCleft.outerBlock.setConjunctsQs(new ArrayList<ConjunctQueryStructure>(genCVCleft.getqStructure().getConjuncts()));
		RelatedToParameters.setupDataStructuresForParamConstraints(genCVCleft.outerBlock);
		GetCVC3HeaderAndFooter.generateCVC3_Header(genCVCleft);
		genCVCleft.setForeignKeysModified(new ArrayList<ForeignKey>(genCVCleft.getqStructure().getForeignKeyVectorModified()));
//		genCVCleft.dnfStrCond=new Vector<Vector<Node>>();
//		genCVCleft.seggregateDnfSelConditions();	
/*		for(Conjunct conjunct : genCVCleft.outerBlock.getConjuncts()){
			conjunct.seggregateSelectionConds();
		}
		RelatedToPreprocessing.segregateSelectionConditions(genCVCleft);
		
		for (Vector<Node> leftStrConds : genCVCleft.getDnfStrCond()) {
			//Vector<Node> leftSelConds = iterator.next();
			Vector<Node> StrCond = null;
			for(Vector<Node> rightStrConds: genCVCright.getDnfStrCond()){
				StrCond=new Vector<Node>();
				StrCond.addAll(rightStrConds);
				StrCond.addAll(leftStrConds);
				dnfStrCond.add(StrCond);
			}
			if(StrCond==null){
				dnfStrCond.add(leftStrConds);
			}
			
		}
		return dnfStrCond;
	}*/
	
/*	private  Vector<Vector<Node>> getdnfLikeCondForApp(){
		Vector<Vector<Node>> dnfLikeCond=new Vector<Vector<Node>>();
		
		for (Vector<Node> leftLikeConds : genCVCleft.getDnfLikeCond()) {
			//Vector<Node> leftSelConds = iterator.next();
			Vector<Node> LikeCond = null;
			for(Vector<Node> rightLikeConds: genCVCright.getDnfLikeCond()){
				LikeCond=new Vector<Node>();
				LikeCond.addAll(rightLikeConds);
				LikeCond.addAll(leftLikeConds);
				dnfLikeCond.add(LikeCond);
			}
			if(LikeCond==null){
				dnfLikeCond.add(leftLikeConds);
			}
			
		}
		return dnfLikeCond;
	}*/
	
/*	private  Vector<Vector<Node>> getDNFSelCondsForApp(){
		Vector<Vector<Node>> dnfSelConds=new Vector<Vector<Node>>();
		
		for (Vector<Node> leftSelConds : genCVCleft.getDnfSelCond()) {
			//Vector<Node> leftSelConds = iterator.next();
			Vector<Node> SelCond = null;
			for(Vector<Node> rightSelConds: genCVCright.getDnfSelCond()){
				SelCond=new Vector<Node>();
				SelCond.addAll(rightSelConds);
				SelCond.addAll(leftSelConds);
				dnfSelConds.add(SelCond);
			}
			if(SelCond==null){
				dnfSelConds.add(leftSelConds);
			}
			*/
		//End transfered
		
		
		
		
		genCVCright=new GenerateCVC1();
		genCVCright.setFne(false);
		genCVCright.setIpdb(false);	
		genCVCright.setConnection(cvc.getConnection());
		genCVCright.setFilePath(cvc.getFilePath());
		genCVCright.initializeQueryDetailsQStructure(rightqStructure);
		genCVCright.setqStructure(rightqStructure);
		genCVCright.setQuery(genCVCright.getqStructure().getQuery());
		genCVCright.setQueryString(genCVCright.getQuery().getQueryString());
		genCVCright.setRepeatedRelationCount(genCVCright.getQuery().getRepeatedRelationCount());
		
		genCVCright.setNoOfOutputTuples((HashMap<String,Integer>)genCVCright.getQuery().getRepeatedRelationCount().clone());
		//genCVCright.setAssignmentId(cvc.getAssignmentId());
		//genCVCright.setQuestionId(cvc.getQuestionId());
		RelatedToPreprocessing.populateData(genCVCright);
		genCVCright.setTableMap(cvc.getTableMap());
		genCVCright.initializeOtherDetails();
//		genCVCright.allConds = genCVCright.qParser.getAllConds();
//		genCVCright.selectionConds = genCVCright.qParser.getSelectionConds();
//		genCVCright.stringSelectionConds = new Vector<Node>();
//		genCVCright.likeConds=new Vector<Node>();
//		genCVCright.isNullConds= qParser.getIsNullConds();
//		genCVCright.aggConstraints=new Vector<Node>();			
//		genCVCright.equivalenceClasses = genCVCright.qParser.getEquivalenceClasses();
//		genCVCright.joinConds = genCVCright.qParser.getJoinConds();
		genCVCright.setForeignKeys(new ArrayList(genCVCright.getqStructure().getForeignKeys()));	
		genCVCright.outerBlock.setProjectedCols(new ArrayList(genCVCright.getqStructure().getProjectedCols()));
		//genCVCright.constraintsWithParameters = new HashMap<String, Node>();
		//genCVCright.dnfSelCond=genCVCright.qParser.getAllDnfSelCond();
		//genCVCright.dnfLikeCond=genCVCright.qParser.getDnfLikeConds();
		//genCVCright.dnfSubQuery=genCVCright.qParser.getAllDnfSubQuery();
		//genCVCright.EqClass=genCVCright.qParser.getEqClass();
		genCVCright.outerBlock.setConjunctsQs(new ArrayList(genCVCright.getqStructure().getConjuncts()));
		//Data structures for Aggregation
		genCVCright.outerBlock.setAggFunc(new ArrayList(genCVCright.getqStructure().getAggFunc()));
		genCVCright.outerBlock.setGroupByNodes(new ArrayList(genCVCright.getqStructure().getGroupByNodes()));
		genCVCright.outerBlock.setHavingClause(genCVCright.getqStructure().getHavingClause());
	//	genCVCright.allSubQueryConds = genCVCright.qParser.getAllSubQueryConds();
	//	genCVCright.allCondsExceptSubQuery = genCVCright.qParser.getAllCondsExceptSubQuery();
	//	genCVCright.root = genCVCright.qParser.getRoot();		
	//	genCVCright.aggConstraints = new Vector<Node>();
	//	genCVCright.aggConstraints.removeAllElements();		
		RelatedToParameters.setupDataStructuresForParamConstraints(genCVCright.outerBlock);
		GetCVC3HeaderAndFooter.generateCVC3_Header(genCVCright);
		genCVCright.setForeignKeysModified(new ArrayList(genCVCright.getqStructure().getForeignKeyVectorModified()));
	//	genCVCright.dnfStrCond=new Vector<Vector<Node>>();
	//	genCVCright.seggregateDnfSelConditions();
/*		for(Conjunct conjunct : genCVCright.outerBlock.getConjuncts()){
			conjunct.seggregateSelectionConds();
		}*/
		RelatedToPreprocessing.segregateSelectionConditions(genCVCright);
		/*Create New CVC Object with combined Constraints*/
//		cvc.initializeQueryDetails(qParser);
//		cvc.setQuery(cvc.getqParser().getQuery());
//		cvc.setQueryString(cvc.getQuery().getQueryString());
//		cvc.setRepeatedRelationCount(getRepeatedRelationCountForApp());		
//		cvc.setResultsetTables(new ArrayList(getResultSetTables()));
//		cvc.setResultsetColumns(getResultSetColumns());
//		cvc.setNoOfOutputTuples(getNoOfOutputTuplesForApp());
//		cvc.allConds = getAllCondsForApp();
//		cvc.selectionConds = getSelectionCondsForApp();
//		cvc.stringSelectionConds=getStringSelectionCondsForApp();
//		cvc.likeConds=getLikeCondsForApp();
//		cvc.isNullConds= getIsNullCondsForApp();
//		cvc.outerBlock.setAggConstraints(new ArrayList(getAggConstraintsForApp()));
//		cvc.equivalenceClasses = getEquivalenceClassesForApp();
//		cvc.joinConds = getJoinCondsForApp();
//		cvc.setForeignKeys(new ArrayList(getForeignKeysForApp()));
//		cvc.constraintsWithParameters = new HashMap<String, Node>();
//		cvc.outerBlock.setParamMap(getParamMapForApp());		
		
		//Data structures for Aggregation
		//TODO add propoer function for following datatype
//		cvc.outerBlock.setAggFunc(new ArrayList(getAggFuncForApp()));
//		cvc.outerBlock.setGroupByNodes(new ArrayList(getGroupByNodesForApp()));
		
//		cvc.outerBlock.setHavingClause(getHavingClauseForApp());
	//	cvc.allSubQueryConds = getAllSubQueryCondsForApp();
	//	cvc.allCondsExceptSubQuery = getAllCondsExceptSubQueryForApp();
//		cvc.root = qParser.getRoot();
		
//		cvc.aggConstraints = new Vector<Node>();
//		cvc.aggConstraints.removeAllElements();		
		//End transfered
		
//		RelatedToParameters.setupDataStructuresForParamConstraints(cvc.outerBlock);
		//cvc.setupDataStructuresForParamConstraints();		
//		GetCVC3HeaderAndFooter.generateCVC3_Header(cvc);
	//	cvc.seggregateSelectionConds();	
//		cvc.outerBlock.setConjuncts(new ArrayList(getConjunctsForApp()));
	//	cvc.dnfSelCond=getDNFSelCondsForApp();
	//	cvc.dnfLikeCond=getdnfLikeCondForApp();
	//	cvc.dnfStrCond=getdnfStrCondForApp();
	//	cvc.dnfSubQuery=getdnfSubQueryForApp();
//		cvc.setForeignKeysModified(new ArrayList(getForeignKeyModifiedForApp()));
		genCVC.setRepeatedRelationCount(getRepeatedRelationCountForApp());
		genCVC.setRepeatedRelNextTuplePos(getRepeatedRelNextTuplePosForApp());
		Vector<Table> tmp = getResultSetTables();
		ArrayList<Table> temp = new ArrayList<Table>();
		temp.addAll(tmp);
		genCVC.setResultsetTables(temp);
		genCVC.setResultsetColumns(getResultSetColumns());
		genCVC.setNoOfOutputTuples(getNoOfOutputTuplesForApp());
		Vector<Node> tmpNode = getForeignKeysForApp();
		ArrayList<Node> tempNode = new ArrayList<Node>();
		tempNode.addAll(tmpNode);
		genCVC.setForeignKeys(tempNode);
		Vector<ForeignKey> tmpFKey = getForeignKeyModifiedForApp();
		ArrayList<ForeignKey> tempFKey = new ArrayList<ForeignKey>();
		tempFKey.addAll(tmpFKey);
		genCVC.setForeignKeysModified(tempFKey);
	}
	
	private HashMap<String, Integer> getRepeatedRelationCountForApp() {
		HashMap<String, Integer> hm = new HashMap<String, Integer>();
		hm = (HashMap<String,Integer>)genCVCleft.getRepeatedRelationCount().clone();
		
		HashMap<String, Integer> temp = genCVCright.getRepeatedRelationCount(); 
		Iterator<String> j = temp.keySet().iterator();
			while(j.hasNext()){
				String key = j.next();
				if(hm.containsKey(key)){
					hm.put(key, hm.get(key)+temp.get(key));
				}
				else{
					hm.put(key, temp.get(key));
				}
			}
		
		return hm;
	}
	
	private HashMap<String, Integer[]> getRepeatedRelNextTuplePosForApp() {
		HashMap<String, Integer[]> hm = new HashMap<String, Integer[]>();
		hm = (HashMap<String,Integer[]>)genCVCleft.getRepeatedRelNextTuplePos().clone();
		
		HashMap<String, Integer[]> temp = genCVCright.getRepeatedRelNextTuplePos(); 
		Iterator<String> j = temp.keySet().iterator();
			while(j.hasNext()){
				String key = j.next();
				if(hm.containsKey(key)){
					hm.put(key.substring(0, key.length()-2) + (hm.size() + 1), temp.get(key));
				}
				else{
					hm.put(key, temp.get(key));
				}
			}
		
		return hm;
	}
	
	private HashMap<String, Integer> getNoOfOutputTuplesForApp() {
		// TODO Auto-generated method stub
		
		HashMap <String,Integer>hm=(HashMap<String,Integer>)genCVC.getRepeatedRelationCount().clone();
		Iterator <String>iterator = hm.keySet().iterator();		   
		while (iterator.hasNext()) {  
		   String key = iterator.next().toString();  
		   Integer value = hm.get(key);  
		   //hm.put(key,value+1);		   
		   hm.put(key,2);
		   
		   
		}
		ArrayList<Table>v=genCVC.getResultsetTables();
		Iterator <Table>it=v.iterator();
		while(it.hasNext()){
			Table t=it.next();
			hm.put(t.getTableName(), 2);
		}
		return hm;
	}
	
	private Vector<Table> getResultSetTables() {
		Vector<Table> retVal = new Vector<Table>();
		
		for(int j=0;j<genCVCleft.getResultsetTables().size();j++){
			Table t = genCVCleft.getResultsetTables().get(j);
			if(!retVal.contains(t)){
				retVal.add(t);
			}
		}
		for(int j=0;j<genCVCright.getResultsetTables().size();j++){
			Table t = genCVCright.getResultsetTables().get(j);
			if(!retVal.contains(t)){
				retVal.add(t);
			}
		}		
		return retVal;
	}
	
	private Vector<Column> getResultSetColumns() {
		Vector<Column> retVal = new Vector<Column>();
		
		for(int j=0;j<genCVCleft.getResultsetColumns().size();j++){
			Column c = genCVCleft.getResultsetColumns().get(j);
			if(!retVal.contains(c)){
				retVal.add(c);
			}
		}
		for(int j=1;j<genCVCright.getResultsetColumns().size();j++){		// j=1 because at j=0 we have a dummy column
			Column c = genCVCright.getResultsetColumns().get(j);			
			if(!retVal.contains(c)){
				retVal.add(c);
			}
		}		
		return retVal;
	}
	


	private Vector<Node> getAllCondsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.getqStructure().getAllConds());
		ac.addAll(genCVCright.getqStructure().getAllConds());		
		return ac;
	}
	
	private Vector<ConjunctQueryStructure> getConjunctsForApp() {
		Vector<ConjunctQueryStructure> cngts = new Vector<ConjunctQueryStructure>();
		ArrayList<ConjunctQueryStructure> leftConjuncts=genCVCleft.outerBlock.getConjunctsQs();
		ArrayList<ConjunctQueryStructure> rightConjuncts=genCVCright.outerBlock.getConjunctsQs();
		for(ConjunctQueryStructure leftConjunct:leftConjuncts){
			ConjunctQueryStructure temp=null;
			for(ConjunctQueryStructure rightConjunct:rightConjuncts){
				temp = new ConjunctQueryStructure(leftConjunct, rightConjunct);
			}
			if(temp==null){
				cngts.add(leftConjunct);
			}
		}
		if(leftConjuncts.isEmpty()){
			for(ConjunctQueryStructure rightConjunct:rightConjuncts){
				cngts.add(rightConjunct);
			}
		}
		return cngts;
	}
	
	
	
	private Vector<Node> getSelectionCondsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.getqStructure().getSelectionConds());
		ac.addAll(genCVCright.getqStructure().getSelectionConds());
		return ac;
	}
	
/*	private Vector<Node> getStringSelectionCondsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.stringSelectionConds);
		ac.addAll(genCVCright.stringSelectionConds);
		
		HashMap <String,Node>hm=new HashMap<String,Node>();
		for(int i=0;i<ac.size();i++){
			if(!hm.containsKey(ac.get(i).toString())){
				hm.put(ac.get(i).toString(), ac.get(i));
			}
		}
		ac = new Vector<Node>();
		Iterator<Node> it=hm.values().iterator();
		while(it.hasNext()){
			ac.add(it.next());
		}
		return ac;
	}*/
	
/*	private Vector<Node> getLikeCondsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.likeConds);
		ac.addAll(genCVCright.likeConds);
		return ac;	
	}
	private Vector<Node> getIsNullCondsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.isNullConds);
		ac.addAll(genCVCright.isNullConds);
		return ac;
	}
	private Vector<Vector<Node>> getEquivalenceClassesForApp() {
		Vector<Vector<Node>> ac = new Vector<Vector<Node>>();
		ac.addAll(genCVCleft.qParser.getEquivalenceClasses());
		ac.addAll(genCVCright.qParser.getEquivalenceClasses());
		return ac;
	}

	private Vector<Node> getJoinCondsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.qParser.getJoinConds());
		ac.addAll(genCVCright.qParser.getJoinConds());
		return ac;
	}*/
	
	private Vector<Node> getForeignKeysForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.getqStructure().getForeignKeys());
		ac.addAll(genCVCright.getqStructure().getForeignKeys());
		HashMap <String,Node>hm=new HashMap<String,Node>();
		for(int i=0;i<ac.size();i++){
			if(!hm.containsKey(ac.get(i).toString())){
				hm.put(ac.get(i).toString(), ac.get(i));
			}
		}
		ac = new Vector<Node>();
		Iterator<Node> it=hm.values().iterator();
		while(it.hasNext()){
			ac.add(it.next());
		}
		return ac;
	}
	private HashMap<String, String> getParamMapForApp() {
		HashMap<String, String> temp = new HashMap<String, String>();
		temp.putAll(genCVCleft.outerBlock.getParamMap());
		temp.putAll(genCVCright.outerBlock.getParamMap());
		return temp;
	}
	
	
	private Vector<Node> getAggConstraintsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.outerBlock.getAggConstraints());
		ac.addAll(genCVCright.outerBlock.getAggConstraints());
		return ac;
	}
	
	private Vector<AggregateFunction> getAggFuncForApp(){
		Vector<AggregateFunction> ac = new Vector<AggregateFunction>();
		ac.addAll(genCVCleft.getqStructure().getAggFunc());
		ac.addAll(genCVCright.getqStructure().getAggFunc());		
		return ac;
	}
	private Vector<Node> getGroupByNodesForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.getqStructure().getGroupByNodes());
		ac.addAll(genCVCright.getqStructure().getGroupByNodes());		
		return ac;
	}
	private Node getHavingClauseForApp() {
		/*Vector<Node> ac = new Vector<Node>();
		ac.add(genCVCleft.qParser.getHavingClause());
		ac.add(genCVCright.qParser.getHavingClause());		
		return ac;*/
		return null;
	}
/*	private Vector<Node> getAllSubQueryCondsForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.qParser.getAllSubQueryConds());
		ac.addAll(genCVCright.qParser.getAllSubQueryConds());		
		return ac;
	}
	private Vector<Node> getAllCondsExceptSubQueryForApp() {
		Vector<Node> ac = new Vector<Node>();
		ac.addAll(genCVCleft.qParser.getAllCondsExceptSubQuery());
		ac.addAll(genCVCright.qParser.getAllCondsExceptSubQuery());		
		return ac;
	}*/
	
	private Vector<String> genNonEmptyResult() throws Exception{
		Vector<String> NonEmptyConstraints=new Vector<String>();
		ArrayList<Node>left=genCVCleft.outerBlock.getProjectedCols();
		ArrayList<Node>right=genCVCright.outerBlock.getProjectedCols();
		String str="ASSERT NOT (";
		for(int j=0;j<genCVCright.getNoOfOutputTuples().size();j++){
			//str+="( ";
			for(int i=0;i<left.size();i++){
				Node n1=left.get(i);
				Node n2=right.get(i);
				str+=GenerateCVCConstraintForNode.cvcMapNode(n1, "1") +"=" +GenerateCVCConstraintForNode.cvcMapNode(n2, ""+(j+1))+" AND ";
			}
			str+="( ";
			boolean flag=true;
			for(ConjunctQueryStructure conjunct : genCVCright.outerBlock.getConjunctsQs()){	
				str+="( ";
				for(int k=0; k<conjunct.getSelectionConds().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getSelectionConds().get(k),(j+1)) + " AND ";
				}
				for(int k=0; k<conjunct.getJoinCondsAllOther().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getJoinCondsAllOther().get(k),(j+1)) + " AND ";
				}
				for(int k=0; k<conjunct.getJoinCondsForEquivalenceClasses().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getJoinCondsForEquivalenceClasses().get(k),(j+1)) + " AND ";
				}
				str=str.substring(0, str.length()-5);
				str+=" ) ";
				str+=" OR ";
				flag=false;
			}
			str=str.substring(0, str.length()-4);
			///str+=")";
			if(flag)
				str=str.substring(0,str.length()-7);
			str += " ) AND ";
		}
		str=str.substring(0, str.length()-7);
		str+=");\n";
		if(!genCVCright.outerBlock.getConjunctsQs().get(0).getJoinCondsAllOther().isEmpty()){
			str="";
			String str1="ASSERT NOT EXISTS ( ";
			Map <String,Character> hm= new HashMap<String, Character>();
			for(ConjunctQueryStructure conjunct : genCVCright.outerBlock.getConjunctsQs()){	
				str+="( ";
				for(int i=0;i<left.size();i++){
					Node n1=left.get(i);
					Node n2=right.get(i);
					str+=GenerateCVCConstraintForNode.cvcMapNode(n1, "1") + "=";
					char index='i';
					if(hm.containsKey(n2.getTable().getTableName())){
						 index=hm.get(n2.getTable().getTableName());
					}
					else
					{
						 Iterator it=hm.entrySet().iterator();
						 index='i';
						 while(it.hasNext()){
							 Map.Entry pairs = (Map.Entry)it.next();
							 char temp=(Character) pairs.getValue();
							 if(temp>index)
								 index=temp;
						 }
						 index++;
						 hm.put(n2.getTable().getTableName(),index);
					}
					str+=GenerateCVCConstraintForNode.cvcMapNode(n2, index+"")+" AND ";
				}
				for(int k=0; k<conjunct.getSelectionConds().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getSelectionConds().get(k),hm) + " AND ";
				}
				for(int k=0; k<conjunct.getJoinCondsAllOther().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getJoinCondsAllOther().get(k),hm) + " AND ";
				}
				for(int k=0; k<conjunct.getJoinCondsForEquivalenceClasses().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getJoinCondsForEquivalenceClasses().get(k),hm) + " AND ";
				}
				
				//for(int k=0;k<conjunct.getAllSubQueryConds().size();k++){
					str+=GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(genCVCright,genCVCright.outerBlock,conjunct);
				//}
				str=str.substring(0, str.length()-5);
				str+=" ) ";
				str+=" OR ";
				//flag=false;
			}
			str=str.substring(0, str.length()-4);
			Iterator it=hm.entrySet().iterator();
			while(it.hasNext()){
			    Map.Entry pairs = (Map.Entry)it.next();
				str1+=" "+pairs.getValue()+": O_"+pairs.getKey()+"_INDEX_INT,";
			 }
			str1=str1.substring(0, str1.length()-1);
			str1+=") :"+str+";";
			NonEmptyConstraints.add(str1);
		}
		else if(!genCVCright.outerBlock.getConjunctsQs().get(0).getJoinCondsForEquivalenceClasses().isEmpty()){
			str="";
			String str1="ASSERT NOT EXISTS ( ";
			Map <String,Character> hm= new HashMap<String, Character>();
			for(ConjunctQueryStructure conjunct : genCVCright.outerBlock.getConjunctsQs()){	
				str+="( ";
				for(int i=0;i<left.size();i++){
					Node n1=left.get(i);
					Node n2=right.get(i);
					str+=GenerateCVCConstraintForNode.cvcMapNode(n1, "1") + "=";
					char index='i';
					if(hm.containsKey(n2.getTable().getTableName())){
						 index=hm.get(n2.getTable().getTableName());
					}
					else
					{
						 Iterator it=hm.entrySet().iterator();
						 index='i';
						 while(it.hasNext()){
							 Map.Entry pairs = (Map.Entry)it.next();
							 char temp=(Character) pairs.getValue();
							 if(temp>index)
								 index=temp;
						 }
						 index++;
						 hm.put(n2.getTable().getTableName(),index);
					}
					str+=GenerateCVCConstraintForNode.cvcMapNode(n2, index+"")+" AND ";
				}
				for(int k=0; k<conjunct.getSelectionConds().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getSelectionConds().get(k),hm) + " AND ";
				}
				for(int k=0; k<conjunct.getJoinCondsAllOther().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getJoinCondsAllOther().get(k),hm) + " AND ";
				}
				for(int k=0; k<conjunct.getJoinCondsForEquivalenceClasses().size();k++)
				{
					str+=GenerateCVCConstraintForNode.genPositiveCondsForPred(genCVCright.outerBlock,conjunct.getJoinCondsForEquivalenceClasses().get(k),hm) + " AND ";
				}
				//for(int k=0;k<conjunct.getAllSubQueryConds().size();k++){
					str+=GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(genCVCright,genCVCright.outerBlock,conjunct);
				//}
				str=str.substring(0, str.length()-5);
				str+=" ) ";
				str+=" OR ";
				//flag=false;
			}
			str=str.substring(0, str.length()-4);
			Iterator it=hm.entrySet().iterator();
			while(it.hasNext()){
			    Map.Entry pairs = (Map.Entry)it.next();
				str1+=" "+pairs.getValue()+": O_"+pairs.getKey()+"_INDEX_INT,";
			 }
			str1=str1.substring(0, str1.length()-1);
			str1+=") :"+str+";";
			NonEmptyConstraints.add(str1);
		}
		else
			NonEmptyConstraints.add(str);
		return NonEmptyConstraints;
	}
	
	private String getAppConstraints(boolean isLeft) throws Exception{
		String appConstraints = "%------------------------\n%UNION CONSTRAINTS\n%---------------------\n\n";
		
		
		if(genCVC.getqStructure().setOperator.equalsIgnoreCase("UNION") ){
			
			appConstraints="%------------------------\n%UNION CONSTRAINTS\n%---------------------\n\n";
			appConstraints+="%-----------------Projecting Column--------------%\n";
			ArrayList<Node>left=genCVCleft.outerBlock.getProjectedCols();
			//for(int i=0;i<left.size();i++){
			for(int i=0;i<1;i++){
				Node n=left.get(i);
				String cond="ASSERT ("+GenerateCVCConstraintForNode.cvcMapNode(n, "1") +"/=" +GenerateCVCConstraintForNode.cvcMapNode(n, "2") +");";				
				appConstraints+=cond+"\n";
			}
			ArrayList<Node>right=genCVCright.outerBlock.getProjectedCols();
			//for(int i=0;i<right.size();i++){
			for(int i=0;i<1;i++){
				Node n=right.get(i);
				String cond="ASSERT ("+GenerateCVCConstraintForNode.cvcMapNode(n, "1") +"/=" +GenerateCVCConstraintForNode.cvcMapNode(n, "2") +");";				
				appConstraints+=cond+"\n";
			}
			
			appConstraints+="\n%--------------Second Tuple -----------------%\n";
			for(int i=0;i<left.size();i++){
				Node n1=left.get(i);
				Node n2=right.get(i);
				String cond="ASSERT ("+GenerateCVCConstraintForNode.cvcMapNode(n1, "2") +"=" +GenerateCVCConstraintForNode.cvcMapNode(n2, "2") +");";
				appConstraints+=cond+"\n";
			}
			
			//Modified by Amol to handle DNF selection conditions.
			appConstraints+="\n\n\n";
			//appConstraints+="%---------------Selection Cond----------------%\n";
			
			String leftcond="";
			for (ConjunctQueryStructure conjunct : genCVCleft.outerBlock.getConjunctsQs()){
				leftcond += GenerateConstraintsForConjunct.getConstraintsForConjuct(genCVCleft, genCVCleft.outerBlock,conjunct);
				leftcond +="\n\n";
			}
/*			for(Vector<Node> selconds : genCVCleft.dnfSelCond){
				for(int k=0; k<selconds.size(); k++){
					leftcond+= genCVCleft.genPositiveCondsForPred(selconds.get(k),2)+" AND ";
				}
				leftcond=leftcond.substring(0, leftcond.length()-5);
				leftcond+=" OR ";
			}
			leftcond=leftcond.substring(0, leftcond.length()-4);*/
			appConstraints+=leftcond+";\n";
			String rightcond="";
			for (ConjunctQueryStructure conjunct : genCVCright.outerBlock.getConjunctsQs()){
				rightcond += GenerateConstraintsForConjunct.getConstraintsForConjuct(genCVCright, genCVCright.outerBlock,conjunct);
				rightcond +="\n\n";
			}
/*			for(Vector<Node> selconds : genCVCright.dnfSelCond){
				for(int k=0; k<selconds.size(); k++){
					rightcond+= genCVCright.genPositiveCondsForPred(selconds.get(k),2)+" AND ";
				}
				rightcond=rightcond.substring(0, rightcond.length()-5);
				rightcond+=" OR ";
			}
			rightcond=rightcond.substring(0, rightcond.length()-4);*/
			appConstraints+=rightcond+";\n";
/*			for(int k=0; k<genCVCleft.selectionConds.size(); k++){
				String cond="ASSERT ";
				if(isLeft){
					cond+="NOT ";
				}
				cond+= genCVCleft.genPositiveCondsForPred(genCVCleft.selectionConds.get(k),2)+";";
				appConstraints+=cond+"\n";
			}
			for(int k=0; k<genCVCright.selectionConds.size(); k++){
				String cond="ASSERT ";
				if(!isLeft){
					cond+="NOT ";
				}
				cond+=genCVCright.genPositiveCondsForPred(genCVCright.selectionConds.get(k),2)+";";
				appConstraints+=cond+"\n";
			}*/
			
			/*appConstraints+="\n\n%--------------String Selection Cond-----------------%\n";
			for(int k=0; k<genCVCleft.stringSelectionConds.size(); k++){
				String cond="ASSERT ";
				if(isLeft){
					cond+="NOT ";
				}
				cond+=genCVCleft.genPositiveCondsForPred(genCVCleft.stringSelectionConds.get(k),2)+";";
				appConstraints+=cond+"\n";
			}
			for(int k=0; k<genCVCright.stringSelectionConds.size(); k++){
				String cond="ASSERT ";
				if(!isLeft){
					cond+="NOT ";
				}
				cond+=genCVCright.genPositiveCondsForPred(genCVCright.stringSelectionConds.get(k),2)+";";
				appConstraints+=cond+"\n";
			}*/
			
		}else if(genCVC.getqStructure().setOperator.equalsIgnoreCase("EXCEPT")){
			//TODO
		}
		return appConstraints;
	}
	
	
	public void generateDataForApp() throws Exception {
		
		genCVC.inititalizeForDatasetQs();
		genCVCleft.inititalizeForDatasetQs();
		genCVCright.inititalizeForDatasetQs();
		genCVC.outerBlock.setNonEmptyConstraints(new ArrayList(genNonEmptyResult()));
		ArrayList<String> AllConstraints=new ArrayList<String>();
		AllConstraints=genCVCleft.getConstraints();
		ArrayList<String> Constraints=new ArrayList<String>();
		Constraints.add(AllConstraints.get(0));
		Constraints.addAll(genCVC.outerBlock.getNonEmptyConstraints());
		genCVC.setConstraints(Constraints);
		for(String constraint:Constraints){
			logger.log(Level.INFO,"Set Operation: "+constraint);
		}
		ArrayList<String> StringConstraints=new ArrayList<String>();
		StringConstraints.add(AllConstraints.get(1));
		genCVC.setStringConstraints(StringConstraints);
		GenerateCommonConstraintsForQuery.generateDataSetForConstraints(genCVC);
	/*	genCVCleft.resultsetColumns=genCVC.resultsetColumns;
		genCVCleft.resultsetTables=genCVC.resultsetTables;
		genCVCleft.noOfOutputTuples=genCVC.noOfOutputTuples;
		//genCVC.appConstraints = getAppConstraints(true);
		genCVCleft.generateDataForQueryWithoutAgg();*/
/*		genCVCleft.appConstraints=genCVC.appConstraints;	
		genCVCleft.populateData(qParser.getQuery());
		genCVCleft.generateDataForQueryWithoutAgg();
		genCVCright.appConstraints=genCVC.appConstraints;
		genCVCright.populateData(qParser.getQuery());
		genCVCright.generateDataForQueryWithoutAgg();*/
/*		if(!genCVC.isDatasetGenerated()){			
			genCVC.appConstraints = getAppConstraints(true);
			genCVCleft.generateDataForQueryWithoutAgg();
			genCVCright.generateDataForQueryWithoutAgg();
		}	*/	
		
		//Generate left subquery constraints
		
		
		
	}
	
	//Generate the constraint to ensure left side generated tuples would match with right side tuples.
	
	public Vector<String> getMatchingConstraints() throws Exception{

		Vector<String> NonEmptyConstraints=new Vector<String>();
		ArrayList<Node>left=genCVCleft.outerBlock.getProjectedCols();
		//We are considering only one table from left subquery.
		//This needs to be extended to make it generalised. 
		String LeftTable=genCVCleft.outerBlock.getProjectedCols().get(0).getTable().getTableName();
		ArrayList<Node>right=genCVCright.outerBlock.getProjectedCols();
		String RightTable=genCVCright.outerBlock.getProjectedCols().get(0).getTable().getTableName();
		for(int l=0;l<genCVCleft.getNoOfOutputTuples().get(LeftTable);l++){
			String str="ASSERT (";
			for(int j=0;j<genCVCright.getNoOfOutputTuples().get(RightTable);j++){
				//str+="(";
				for(int i=0;i<left.size();i++){
					Node n1=left.get(i);
					Node n2=right.get(i);
					str+=GenerateCVCConstraintForNode.cvcMapNode(n1, ""+(l+1)) +"=" +GenerateCVCConstraintForNode.cvcMapNode(n2, ""+(j+1))+" AND ";
				}
			
				/*str+="( ";
				boolean flag=true;
				for(Vector<Node> selconds : genCVCleft.dnfSelCond){
					for(int k=0; k<selconds.size(); k++){
						str+= genCVCleft.genPositiveCondsForPred(selconds.get(k),(j+1))+" AND ";
					}
					str=str.substring(0, str.length()-5);
					str+=" OR ";
					flag=false;
				}
				str+=")";
				if(flag)
					str=str.substring(0,str.length()-3);
				str=str.substring(0, str.length()-5);
				str+=") OR ";*/
			}
			str=str.substring(0, str.length()-4);
			str+=");";
			NonEmptyConstraints.add(str);
		}
		String leftcond="";
		for (ConjunctQueryStructure conjunct : genCVCleft.outerBlock.getConjunctsQs()){
			leftcond += GenerateConstraintsForConjunct.getConstraintsForConjuct(genCVCleft, genCVCleft.outerBlock,conjunct);
			leftcond +="\n\n";
		}
		NonEmptyConstraints.add(leftcond);
		return NonEmptyConstraints;
	}
	//Generate the constraint to ensure left side generated tuples would not match any tuple from right side subquery.
	public Vector<String> getNonMatchingConstraints() throws Exception{
		Vector<String> NonEmptyConstraints=new Vector<String>();
		ArrayList<Node>left=genCVCleft.outerBlock.getProjectedCols();
		ArrayList<Node>right=genCVCright.outerBlock.getProjectedCols();
		for(int l=0;l<genCVCleft.getNoOfOutputTuples().size();l++){
			String str="ASSERT NOT (";
			for(int j=0;j<genCVCright.getNoOfOutputTuples().size();j++){
				for(int i=0;i<left.size();i++){
					Node n1=left.get(i);
					Node n2=right.get(i);
					str+=GenerateCVCConstraintForNode.cvcMapNode(n1, ""+(l+1)) +"=" +GenerateCVCConstraintForNode.cvcMapNode(n2, ""+(j+1))+" AND ";
				}
/*				str+="( ";
				boolean flag=true;
				for(Vector<Node> selconds : genCVCright.dnfSelCond){
					for(int k=0; k<selconds.size(); k++){
						str+= genCVCright.genPositiveCondsForPred(selconds.get(k),(j+1))+" AND ";
					}
					str=str.substring(0, str.length()-5);
					str+=" OR ";
					flag=false;
				}
				
				str+=")";
				if(flag)
					str=str.substring(0,str.length()-3);*/
				

			}
			str=str.substring(0, str.length()-4);
			str+=");\n";
			NonEmptyConstraints.add(str);
		}
		String rightcond="";
		for (ConjunctQueryStructure conjunct : genCVCright.outerBlock.getConjunctsQs()){
			rightcond += GenerateConstraintsForConjunct.getConstraintsForConjuct(genCVCright, genCVCright.outerBlock,conjunct);
			rightcond +="\n\n";
		}
		NonEmptyConstraints.add(rightcond);
		return NonEmptyConstraints;
	}
	
	public void generateToKill() throws Exception {
		
		
		//Killing left subquery mutations
		genCVCleft.outerBlock.setNonEmptyConstraints(new ArrayList(getNonMatchingConstraints()));
		genCVCleft.setForeignKeys(genCVC.getForeignKeys());
		genCVCleft.setForeignKeysModified(genCVC.getForeignKeysModified());
		genCVCleft.setResultsetColumns(genCVC.getResultsetColumns());
		genCVCleft.setResultsetTables(genCVC.getResultsetTables());
		genCVCleft.setNoOfOutputTuples(genCVC.getNoOfOutputTuples());
		genCVCleft.setCount(genCVC.getCount());
		SelectionMutationsInOuterQueryBlock.generateDataForkillingSelectionMutationsInOuterQueryBlock(genCVCleft);
		//genCVCleft.generateDataForKillingSelectionMutantsWithoutAgg();
		//Killing right subquery mutations
		genCVCright.outerBlock.setNonEmptyConstraints(new ArrayList(getMatchingConstraints()));
		genCVCright.setForeignKeys(genCVC.getForeignKeys());
		genCVCright.setForeignKeysModified(genCVC.getForeignKeysModified());
		genCVCright.setResultsetColumns(genCVC.getResultsetColumns());
		genCVCright.setResultsetTables(genCVC.getResultsetTables());
		genCVCright.setNoOfOutputTuples(genCVC.getNoOfOutputTuples());
		genCVCright.setCount(genCVCleft.getCount());
		SelectionMutationsInOuterQueryBlock.generateDataForkillingSelectionMutationsInOuterQueryBlock(genCVCright);
		//genCVCright.generateDataForKillingSelectionMutantsWithoutAgg();
/*		genCVC.appConstraints = getAppConstraints(true);		
		genCVC.generateDataForKillingEquiClassMutants();
		if(!genCVC.isDatasetGenerated()){			
			genCVC.appConstraints = getAppConstraints(true);
			genCVC.generateDataForKillingEquiClassMutants();
		}
		
		//Killing Selection Class Mutants
		genCVC.appConstraints = getAppConstraints(false);		
		genCVC.generateDataForKillingSelectionMutantsWithoutAgg();
		if(!genCVC.isDatasetGenerated()){	
			genCVC.appConstraints = getAppConstraints(true);
			genCVC.generateDataForKillingSelectionMutantsWithoutAgg();
		}*/
		/*
		//Killing String Selection Class Mutants
		genCVC.appConstraints = getAppConstraints(false);		
		genCVC.generateDataForKillingStringSelectionMutantsWithoutAgg();
		if(!genCVC.isDatasetGenerated()){			
			genCVC.appConstraints = getAppConstraints(true);
			genCVC.generateDataForKillingStringSelectionMutantsWithoutAgg();
		}
			
		//Killing String Like Class Mutants
		genCVC.appConstraints = getAppConstraints(false);		
		genCVC.generateDataForKillingLikeMutantsWithoutAgg();
		if(!genCVC.isDatasetGenerated()){			
			genCVC.appConstraints = getAppConstraints(true);
			genCVC.generateDataForKillingLikeMutantsWithoutAgg();
		}
		*/
		
		
	}

	public QueryParser getqParser() {
		return qParser;
	}

	public void setqParser(QueryParser qParser) {
		this.qParser = qParser;
	}

	public QueryParser getLeftqParser() {
		return leftqParser;
	}

	public void setLeftqParser(QueryParser leftqParser) {
		this.leftqParser = leftqParser;
	}

	public QueryParser getRightqParser() {
		return rightqParser;
	}

	public void setRightqParser(QueryParser rightqParser) {
		this.rightqParser = rightqParser;
	}

	public GenerateCVC1 getGenCVC() {
		return genCVC;
	}

	public void setGenCVC(GenerateCVC1 genCVC) {
		this.genCVC = genCVC;
	}

	public GenerateCVC1 getGenCVCleft() {
		return genCVCleft;
	}

	public void setGenCVCleft(GenerateCVC1 genCVCleft) {
		this.genCVCleft = genCVCleft;
	}

	public GenerateCVC1 getGenCVCright() {
		return genCVCright;
	}

	public void setGenCVCright(GenerateCVC1 genCVCright) {
		this.genCVCright = genCVCright;
	}
	
	//copy function to work on copies instead of touching the original one
	public GenerateUnionCVC copy() throws Exception{
		//TODO: change implementation to provide faster copy
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();
        out.close();
        
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        GenerateUnionCVC obj = (GenerateUnionCVC)in.readObject();
		return obj;
	}
	
	//Commented out methods for experimenting
	/*	private  Vector<Vector<Node>> getdnfSubQueryForApp(){
	Vector<Vector<Node>> dnfSubQuery=new Vector<Vector<Node>>();
	
	for (Vector<Node> leftdnfSubQuery : genCVCleft.getDnfSubQuery()) {
		//Vector<Node> leftSelConds = iterator.next();
		Vector<Node> SubQuery = null;
		for(Vector<Node> rightdnfSubQuery: genCVCright.getDnfSubQuery()){
			SubQuery=new Vector<Node>();
			SubQuery.addAll(rightdnfSubQuery);
			SubQuery.addAll(leftdnfSubQuery);
			dnfSubQuery.add(SubQuery);
		}
		if(SubQuery==null){
			dnfSubQuery.add(leftdnfSubQuery);
		}
		
	}
	return dnfSubQuery;
}*/

/*	private  Vector<Vector<Node>> getdnfStrCondForApp(){
	Vector<Vector<Node>> dnfStrCond=new Vector<Vector<Node>>();
	
	for (Vector<Node> leftStrConds : genCVCleft.getDnfStrCond()) {
		//Vector<Node> leftSelConds = iterator.next();
		Vector<Node> StrCond = null;
		for(Vector<Node> rightStrConds: genCVCright.getDnfStrCond()){
			StrCond=new Vector<Node>();
			StrCond.addAll(rightStrConds);
			StrCond.addAll(leftStrConds);
			dnfStrCond.add(StrCond);
		}
		if(StrCond==null){
			dnfStrCond.add(leftStrConds);
		}
		
	}
	return dnfStrCond;
}*/

/*	private  Vector<Vector<Node>> getdnfLikeCondForApp(){
	Vector<Vector<Node>> dnfLikeCond=new Vector<Vector<Node>>();
	
	for (Vector<Node> leftLikeConds : genCVCleft.getDnfLikeCond()) {
		//Vector<Node> leftSelConds = iterator.next();
		Vector<Node> LikeCond = null;
		for(Vector<Node> rightLikeConds: genCVCright.getDnfLikeCond()){
			LikeCond=new Vector<Node>();
			LikeCond.addAll(rightLikeConds);
			LikeCond.addAll(leftLikeConds);
			dnfLikeCond.add(LikeCond);
		}
		if(LikeCond==null){
			dnfLikeCond.add(leftLikeConds);
		}
		
	}
	return dnfLikeCond;
}*/

/*	private  Vector<Vector<Node>> getDNFSelCondsForApp(){
	Vector<Vector<Node>> dnfSelConds=new Vector<Vector<Node>>();
	
	for (Vector<Node> leftSelConds : genCVCleft.getDnfSelCond()) {
		//Vector<Node> leftSelConds = iterator.next();
		Vector<Node> SelCond = null;
		for(Vector<Node> rightSelConds: genCVCright.getDnfSelCond()){
			SelCond=new Vector<Node>();
			SelCond.addAll(rightSelConds);
			SelCond.addAll(leftSelConds);
			dnfSelConds.add(SelCond);
		}
		if(SelCond==null){
			dnfSelConds.add(leftSelConds);
		}
		
	}
	return dnfSelConds;
}*/

}
