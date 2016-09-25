package killMutations.outerQueryBlock;
//akshay

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import killMutations.GenerateDataForOriginalQuery;
import parsing.Conjunct;
import parsing.ForeignKey;
import parsing.Node;
import parsing.Table;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintsToKillDistinctMutations;
import generateConstraints.GenerateConstraintsToKillSetOperatorMutations;
import generateConstraints.AddDataBaseConstraints;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.GenerateUnionCVC;
import testDataGen.PreProcessingActivity;
import testDataGen.QueryBlockDetails;
import util.TagDatasets;
import util.Utilities;

public class SetOperatorMutations {

	public static boolean generateDataWithOneTupleInLeftAndNotInRight(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		GenerateCVC1 right = cvc.getGenCVCright();
		
		GenerateCVC1 cvcNotExists = PreProcessingActivity.convertSetQuerytoSubquery(left,right, Node.getNotExistsNodeType());
		cvcNotExists.setRepeatedRelationCount(cvc.getGenCVC().getRepeatedRelationCount());
		cvcNotExists.setRepeatedRelNextTuplePos(cvc.getGenCVC().getRepeatedRelNextTuplePos());
		cvcNotExists.setResultsetTables(cvc.getGenCVC().getResultsetTables());
		cvcNotExists.setResultsetColumns(cvc.getGenCVC().getResultsetColumns());
		cvcNotExists.setNoOfOutputTuples(cvc.getGenCVC().getNoOfOutputTuples());
		cvcNotExists.setForeignKeys(cvc.getGenCVC().getForeignKeys());
		cvcNotExists.setForeignKeysModified(cvc.getGenCVC().getForeignKeysModified());
		cvcNotExists.setUnionCVC(cvc);
		cvcNotExists.inititalizeForDataset();
		cvcNotExists.setCount(cvc.getGenCVC().getCount());
		//cvcNotExists.initializeQueryDetails(cvcNotExists.getqParser());
		
		/*
		//initialize for data generation
		cvcNotExists.inititalizeForDataset();
		
		//set type of mutation to be killed
		cvcNotExists.setTypeOfMutation(TagDatasets.MutationType.SETOP, TagDatasets.QueryBlock.OUTER_BLOCK);
		
		//get constraints for all the blocks of the left query
		left.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(left));
		*/
		
		return GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvcNotExists, "DATASET TO KILL SET OPERATOR MUTATIONS");
	}
	
	public static boolean generateDataWithOneTupleInRightAndNotInLeft(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		GenerateCVC1 right = cvc.getGenCVCright();
		
		GenerateCVC1 cvcNotExists = PreProcessingActivity.convertSetQuerytoSubquery(right,left, Node.getNotExistsNodeType());
		cvcNotExists.setRepeatedRelationCount(cvc.getGenCVC().getRepeatedRelationCount());
		cvcNotExists.setRepeatedRelNextTuplePos(cvc.getGenCVC().getRepeatedRelNextTuplePos());
		cvcNotExists.setResultsetTables(cvc.getGenCVC().getResultsetTables());
		cvcNotExists.setResultsetColumns(cvc.getGenCVC().getResultsetColumns());
		cvcNotExists.setNoOfOutputTuples(cvc.getGenCVC().getNoOfOutputTuples());
		cvcNotExists.setForeignKeys(cvc.getGenCVC().getForeignKeys());
		cvcNotExists.setForeignKeysModified(cvc.getGenCVC().getForeignKeysModified());
		cvcNotExists.setUnionCVC(cvc);
		cvcNotExists.inititalizeForDataset();
		cvcNotExists.setCount(cvc.getGenCVC().getCount());
		//cvcNotExists.initializeQueryDetails(cvcNotExists.getqParser());

		return GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvcNotExists, "DATASET TO KILL SET OPERATOR MUTATIONS");
	}
	
	public static boolean generateDataWithTwoTuplesInLeftAndNotInRight(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		GenerateCVC1 right = cvc.getGenCVCright();
		
		GenerateCVC1 cvcNotExists = PreProcessingActivity.convertSetQuerytoSubquery(left,right, Node.getNotExistsNodeType());
		cvcNotExists.setRepeatedRelationCount(cvc.getGenCVC().getRepeatedRelationCount());
		cvcNotExists.setRepeatedRelNextTuplePos(cvc.getGenCVC().getRepeatedRelNextTuplePos());
		cvcNotExists.setResultsetTables(cvc.getGenCVC().getResultsetTables());
		cvcNotExists.setResultsetColumns(cvc.getGenCVC().getResultsetColumns());
		cvcNotExists.setNoOfOutputTuples(cvc.getGenCVC().getNoOfOutputTuples());
		cvcNotExists.setForeignKeys(cvc.getGenCVC().getForeignKeys());
		cvcNotExists.setForeignKeysModified(cvc.getGenCVC().getForeignKeysModified());
		cvcNotExists.setUnionCVC(cvc);
		cvcNotExists.inititalizeForDataset();
		cvcNotExists.setCount(cvc.getGenCVC().getCount());
		//cvcNotExists.initializeQueryDetails(cvcNotExists.getqParser());
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = cvcNotExists.getOuterBlock();
		
		/**set the type of mutation we are trying to kill*/
		cvcNotExists.setTypeOfMutation(TagDatasets.MutationType.SETOP, TagDatasets.QueryBlock.OUTER_BLOCK);
		
		/**The projected columns become single valued attributes because the two tuples will have same value for this projected columns 
		 * FIXME: What if finalCount>2 */
		qbt.setSingleValuedAttributesAdd(new HashSet<Node>(qbt.getProjectedCols()));
						
		/** Initialize aggregation constraints data structure*/
		qbt.setAggConstraints(new ArrayList<Node>());
		
		/**get aggregation in this query block*/
		Vector<Node> aggConstraints = new Vector<Node>();
		if(qbt.getHavingClause() != null)
			Utilities.flattenConstraints(aggConstraints, qbt.getHavingClause());
		
		/**Update query block aggregation list*/
		qbt.setAggConstraints( new ArrayList<Node>(aggConstraints));
		
		/**get the count needed*/
		if(CountEstimationRelated.getCountNeededToKillDistinctMutation(cvcNotExists, qbt) == false)
			return false;

		/** get table name */
		String tableNameNo = null;
		
		Node n1 = null;
		for(Node n: qbt.getProjectedCols())
			for(QueryBlockDetails qb : cvcNotExists.getOuterBlock().getFromClauseSubQueries())
				if(!qb.getProjectedCols().contains(n)){
					tableNameNo = n.getTableNameNo();
					n1 = n;
				}
		
		/**If this relation is involved in equi-joins then we can ensure multiple tuples at the output, even if this relation contains a single tuple 
		 * FIXME: But what if all the relations in this equivalence class contains a single tuple*/
		for(Conjunct con: qbt.getConjuncts())
			for(Vector<Node> ec: con.getEquivalenceClasses())
				for(Node n2: ec)
					if(n2.getTableNameNo().equalsIgnoreCase(tableNameNo))
						tableNameNo = null;
		
		
		/**assign the number of tuples for the this query block*/
		if( QueryBlockDetails.getTupleAssignment(cvcNotExists, qbt, tableNameNo) == false)
			return false;

		/**get the tuple assignment for all other query blocks*/
		if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvcNotExists, qbt) == false)
			return false;
		
		/** Add constraints for all the blocks of the query */
		cvcNotExists.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(cvcNotExists));
		
		//get constraints for distinct tuples with same values for projected attributes
		cvcNotExists.getConstraints().add(GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(cvcNotExists, cvcNotExists.getOuterBlock()));
		
		//adding database constraints
		//cvcNotExists.getConstraints().add(AddDataBaseConstraints.addDBConstraints(cvcNotExists));
		//FIXME:change name of variable to something else or handle differently
		cvcNotExists.getOuterBlock().setConstrainedAggregation(true);
		//cvcNotExists.getConstraints().add(AddDataBaseConstraints.addDBConstraints(cvcNotExists));
		//cvcNotExists.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlockExceptSubQuries(cvcNotExists, cvcNotExists.getOuterBlock()));
		
		//data generation for the constraints
		return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvcNotExists);
	}
	
	public static boolean generateDataWithTwoTuplesInLeft(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		
		GenerateCVC1 right = cvc.getGenCVCright();
		
		PreProcessingActivity.setOriginalTablesForSetQuery(left, right);
			
		//initialize for data generation
		left.inititalizeForDataset();
		left.setCount(cvc.getGenCVC().getCount());
	
		/** Get outer query block of this query */
		QueryBlockDetails qbt = left.getOuterBlock();
		
		//set type of mutation to be killed
		left.setTypeOfMutation(TagDatasets.MutationType.SETOP, TagDatasets.QueryBlock.OUTER_BLOCK);
		
		/**The projected columns become single valued attributes because the two tuples will have same value for this projected columns 
		 * FIXME: What if finalCount>2 */
		qbt.setSingleValuedAttributesAdd(new HashSet<Node>(qbt.getProjectedCols()));
						
		/** Initialize aggregation constraints data structure*/
		qbt.setAggConstraints(new ArrayList<Node>());
		
		/**get aggregation in this query block*/
		Vector<Node> aggConstraints = new Vector<Node>();
		if(qbt.getHavingClause() != null)
			Utilities.flattenConstraints(aggConstraints, qbt.getHavingClause());
		
		/**Update query block aggregation list*/
		qbt.setAggConstraints( new ArrayList<Node>(aggConstraints));
		
		/**get the count needed*/
		if(CountEstimationRelated.getCountNeededToKillDistinctMutation(left, qbt) == false)
			return false;

		/** get table name */
		String tableNameNo = null;
		
		Node n1 = null;
		for(Node n: qbt.getProjectedCols())
			for(QueryBlockDetails qb : left.getOuterBlock().getFromClauseSubQueries())
				if(!qb.getProjectedCols().contains(n)){
					tableNameNo = n.getTableNameNo();
					n1 = n;
				}
		
		/**If this relation is involved in equi-joins then we can ensure multiple tuples at the output, even if this relation contains a single tuple 
		 * FIXME: But what if all the relations in this equivalence class contains a single tuple*/
		for(Conjunct con: qbt.getConjuncts())
			for(Vector<Node> ec: con.getEquivalenceClasses())
				for(Node n2: ec)
					if(n2.getTableNameNo().equalsIgnoreCase(tableNameNo))
						tableNameNo = null;
		
		
		/**assign the number of tuples for the this query block*/
		if( QueryBlockDetails.getTupleAssignment(left, qbt, tableNameNo) == false)
			return false;

		/**get the tuple assignment for all other query blocks*/
		if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(left, qbt) == false)
			return false;
		
		/** Add constraints for all the blocks of the query */
		left.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(left));
		
		//get constraints for distinct tuples with same values for projected attributes
		left.getConstraints().add(GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(left, left.getOuterBlock()));
			
		//FIXME:change name of variable to something else or handle differently
		left.getOuterBlock().setConstrainedAggregation(true);
		
		//data generation for the constraints
		return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(left);
	}
	
	public static boolean generateDataWithTwoTuplesInRight(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 right = cvc.getGenCVCright();
		GenerateCVC1 left = cvc.getGenCVCleft();
		
		PreProcessingActivity.setOriginalTablesForSetQuery(left, right);
	
		//initialize for data generation
		right.inititalizeForDataset();
		right.setCount(cvc.getGenCVC().getCount());
	
		/** Get outer query block of this query */
		QueryBlockDetails qbt = right.getOuterBlock();
		
		//set type of mutation to be killed
		right.setTypeOfMutation(TagDatasets.MutationType.SETOP, TagDatasets.QueryBlock.OUTER_BLOCK);
		
		/**The projected columns become single valued attributes because the two tuples will have same value for this projected columns 
		 * FIXME: What if finalCount>2 */
		qbt.setSingleValuedAttributesAdd(new HashSet<Node>(qbt.getProjectedCols()));
						
		/** Initialize aggregation constraints data structure*/
		qbt.setAggConstraints(new ArrayList<Node>());
		
		/**get aggregation in this query block*/
		Vector<Node> aggConstraints = new Vector<Node>();
		if(qbt.getHavingClause() != null)
			Utilities.flattenConstraints(aggConstraints, qbt.getHavingClause());
		
		/**Update query block aggregation list*/
		qbt.setAggConstraints( new ArrayList<Node>(aggConstraints));
		
		/**get the count needed*/
		if(CountEstimationRelated.getCountNeededToKillDistinctMutation(right, qbt) == false)
			return false;

		/** get table name */
		String tableNameNo = null;
		
		Node n1 = null;
		for(Node n: qbt.getProjectedCols())
			for(QueryBlockDetails qb : right.getOuterBlock().getFromClauseSubQueries())
				if(!qb.getProjectedCols().contains(n)){
					tableNameNo = n.getTableNameNo();
					n1 = n;
				}
		
		/**If this relation is involved in equi-joins then we can ensure multiple tuples at the output, even if this relation contains a single tuple 
		 * FIXME: But what if all the relations in this equivalence class contains a single tuple*/
		for(Conjunct con: qbt.getConjuncts())
			for(Vector<Node> ec: con.getEquivalenceClasses())
				for(Node n2: ec)
					if(n2.getTableNameNo().equalsIgnoreCase(tableNameNo))
						tableNameNo = null;
		
		
		/**assign the number of tuples for the this query block*/
		if( QueryBlockDetails.getTupleAssignment(right, qbt, tableNameNo) == false)
			return false;

		/**get the tuple assignment for all other query blocks*/
		if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(right, qbt) == false)
			return false;
		
		/** Add constraints for all the blocks of the query */
		right.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(right));
			
		//get constraints for distinct tuples with same values for projected attributes
		right.getConstraints().add(GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(right, right.getOuterBlock()));
			
		//FIXME:change name of variable to something else or handle differently
		right.getOuterBlock().setConstrainedAggregation(true);
		
		//data generation for the constraints
		return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(right);
	}

	public static boolean generateDataWithTwoTuplesInLeftAndRight(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		GenerateCVC1 right = cvc.getGenCVCright();
		
		//initialize for data generation
		left.setRepeatedRelationCount(cvc.getGenCVC().getRepeatedRelationCount());
		left.setResultsetTables(cvc.getGenCVC().getResultsetTables());
		left.setResultsetColumns(cvc.getGenCVC().getResultsetColumns());
		left.setCount(cvc.getGenCVC().getCount());
		
		PreProcessingActivity.setOriginalTablesForSetQuery(left, right);
		

		left.inititalizeForDataset();
		right.inititalizeForDataset();
				
		//set type of mutation to be killed
		left.setTypeOfMutation(TagDatasets.MutationType.SETOP, TagDatasets.QueryBlock.OUTER_BLOCK);
		
		/** Get outer query block of this query */
		QueryBlockDetails qbtLeft = left.getOuterBlock();
				
		/**The projected columns become single valued attributes because the two tuples will have same value for this projected columns 
		 * FIXME: What if finalCount>2 */
		qbtLeft.setSingleValuedAttributesAdd(new HashSet<Node>(qbtLeft.getProjectedCols()));
						
		/** Initialize aggregation constraints data structure*/
		qbtLeft.setAggConstraints(new ArrayList<Node>());
		
		/**get aggregation in this query block*/
		Vector<Node> aggConstraintsLeft = new Vector<Node>();
		if(qbtLeft.getHavingClause() != null)
			Utilities.flattenConstraints(aggConstraintsLeft, qbtLeft.getHavingClause());
		
		/**Update query block aggregation list*/
		qbtLeft.setAggConstraints( new ArrayList<Node>(aggConstraintsLeft));
		
		/**get the count needed*/
		if(CountEstimationRelated.getCountNeededToKillDistinctMutation(left, qbtLeft) == false)
			return false;

		/** get table name */
		String tableNameNoLeft = null;
		
		Node n1 = null;
		for(Node n: qbtLeft.getProjectedCols())
			for(QueryBlockDetails qb : left.getOuterBlock().getFromClauseSubQueries())
				if(!qb.getProjectedCols().contains(n)){
					tableNameNoLeft = n.getTableNameNo();
					n1 = n;
				}
		
		/**If this relation is involved in equi-joins then we can ensure multiple tuples at the output, even if this relation contains a single tuple 
		 * FIXME: But what if all the relations in this equivalence class contains a single tuple*/
		for(Conjunct con: qbtLeft.getConjuncts())
			for(Vector<Node> ec: con.getEquivalenceClasses())
				for(Node n2: ec)
					if(n2.getTableNameNo().equalsIgnoreCase(tableNameNoLeft))
						tableNameNoLeft = null;
		
		
		/**assign the number of tuples for the this query block*/
		if( QueryBlockDetails.getTupleAssignment(left, qbtLeft, tableNameNoLeft) == false)
			return false;

		/**get the tuple assignment for all other query blocks*/
		if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(left, qbtLeft) == false)
			return false;
		
		/** Add constraints for all the blocks of the query */
		left.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(left));
		
		/** Get outer query block of this query */
		QueryBlockDetails qbtRight = right.getOuterBlock();
		
		/**The projected columns become single valued attributes because the two tuples will have same value for this projected columns 
		 * FIXME: What if finalCount>2 */
		qbtRight.setSingleValuedAttributesAdd(new HashSet<Node>(qbtRight.getProjectedCols()));
						
		/** Initialize aggregation constraints data structure*/
		qbtRight.setAggConstraints(new ArrayList<Node>());
		
		/**get aggregation in this query block*/
		Vector<Node> aggConstraintsRight = new Vector<Node>();
		if(qbtRight.getHavingClause() != null)
			Utilities.flattenConstraints(aggConstraintsRight, qbtRight.getHavingClause());
		
		/**Update query block aggregation list*/
		qbtRight.setAggConstraints( new ArrayList<Node>(aggConstraintsRight));
		
		/**get the count needed*/
		if(CountEstimationRelated.getCountNeededToKillDistinctMutation(right, qbtRight) == false)
			return false;

		/** get table name */
		String tableNameNoRight = null;
		
		Node n3 = null;
		for(Node n: qbtRight.getProjectedCols())
			for(QueryBlockDetails qb : right.getOuterBlock().getFromClauseSubQueries())
				if(!qb.getProjectedCols().contains(n)){
					tableNameNoRight = n.getTableNameNo();
					n3 = n;
				}
		
		/**If this relation is involved in equi-joins then we can ensure multiple tuples at the output, even if this relation contains a single tuple 
		 * FIXME: But what if all the relations in this equivalence class contains a single tuple*/
		for(Conjunct con: qbtRight.getConjuncts())
			for(Vector<Node> ec: con.getEquivalenceClasses())
				for(Node n4: ec)
					if(n4.getTableNameNo().equalsIgnoreCase(tableNameNoRight))
						tableNameNoRight = null;
		
		
		/**assign the number of tuples for the this query block*/
		if( QueryBlockDetails.getTupleAssignment(right, qbtRight, tableNameNoRight) == false)
			return false;

		/**get the tuple assignment for all other query blocks*/
		if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(right, qbtRight) == false)
			return false;
		
		/** Add constraints for all the blocks of the query */
		left.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(right));
		
		HashMap <String,Integer> hmLeft = (HashMap<String,Integer>)left.getNoOfOutputTuples().clone();
		HashMap <String,Integer> hmRight = (HashMap<String,Integer>)right.getNoOfOutputTuples().clone();
		Iterator <String> iterator = hmRight.keySet().iterator();		   
		while (iterator.hasNext())
		{  
		   String key = iterator.next().toString();  
		   Integer value = hmRight.get(key);  
		   if(hmLeft.containsKey(key))
		   {
			   value = value + hmLeft.get(key);
			   hmLeft.remove(key);
			   hmLeft.put(key, value);
		   }
		   else
			   hmLeft.put(key,value);
		}
		left.setNoOfOutputTuples(hmLeft);
		
		//copy table names from right to left since the data generation will happen on left cvc
		HashMap<String, Integer[]> hm = right.getTableNames();
		iterator = hm.keySet().iterator();
		while (iterator.hasNext())
		{  
		   String key = iterator.next().toString();  
		   Integer[] value = hm.get(key);  
		   if(left.getTableNames().containsKey(key))
		   {
			   //FIXME:What if number of repeated relations is in double digits
			   Integer i = Integer.parseInt(key.substring(key.length()-1));
			   i += 1;
			   key = key.substring(0, key.length()-1) + i;
			   left.getTableNames().put(key, value);
		   }
		   else
			   left.getTableNames().put(key,value);
		}
		
		//Might be error with not assigning proper repeated relation count to left
		left.setRepeatedRelNextTuplePos(new HashMap<String, Integer[]>());
		left.setNoOfTuples(new HashMap<String,Integer>());
		left.setCurrentIndexCount(new HashMap<String,Integer>());
		/** Update repeated relation next position etc..*/
		Iterator<String> itr = left.getRepeatedRelationCount().keySet().iterator();
		while(itr.hasNext())
		{
			String tableName = itr.next();
			int c = left.getRepeatedRelationCount().get(tableName);
			for(int i=1;i<=c;i++)
			{
				Integer[] tuplePos = new Integer[32];
				tuplePos[1] = i;//Meaning first tuple is at pos i
				left.getRepeatedRelNextTuplePos().put(tableName+i, tuplePos);
				left.getNoOfTuples().put(tableName+i, 1);
				left.getCurrentIndexCount().put(tableName+i, i);
			}
		}
		
		//get constraints for distinct tuples with same values for projected attributes
		left.getConstraints().add(GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(left, left.getOuterBlock()));
		left.getConstraints().add(GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(right, right.getOuterBlock()));
		
		//FIXME:change name of variable to something else or handle differently
		right.getOuterBlock().setConstrainedAggregation(true);
		left.getConstraints().add(AddDataBaseConstraints.addDBConstraints(right));
		left.getOuterBlock().setConstrainedAggregation(true);
		
		left.getConstraints().add(GenerateConstraintsToKillSetOperatorMutations.generateConstraintsForMatchingTuples(cvc));
		
		//data generation for the constraints
		return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(left);		
	}
	
	public static boolean generateDataWithOneTupleInLeftAndRight(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		GenerateCVC1 right = cvc.getGenCVCright();
		
		//initialize for data generation
		left.setCount(cvc.getGenCVC().getCount());
		left.setRepeatedRelationCount(cvc.getGenCVC().getRepeatedRelationCount());
		left.setResultsetTables(cvc.getGenCVC().getResultsetTables());
		left.setResultsetColumns(cvc.getGenCVC().getResultsetColumns());
		
		PreProcessingActivity.setOriginalTablesForSetQuery(left, right);

		left.inititalizeForDataset();
		
		/**set the type of mutation we are trying to kill*/
		left.setTypeOfMutation(TagDatasets.MutationType.SETOP, TagDatasets.QueryBlock.OUTER_BLOCK);

		/** get the tuple assignment for this query
		 * If no possible assignment then not possible to kill this mutation*/
		if(GenerateCVC1.tupleAssignmentForQuery(left) == false)
			return false;

		HashMap <String,Integer> hmLeft = (HashMap<String,Integer>)left.getNoOfOutputTuples().clone();
		HashMap <String,Integer> hmRight = (HashMap<String,Integer>)right.getNoOfOutputTuples().clone();
		Iterator <String> iterator = hmRight.keySet().iterator();		   
		while (iterator.hasNext())
		{  
		   String key = iterator.next().toString();  
		   Integer value = hmRight.get(key);  
		   if(hmLeft.containsKey(key))
		   {
			   value = value + hmLeft.get(key);
			   hmLeft.remove(key);
			   hmLeft.put(key, value);
		   }
		   else
			   hmLeft.put(key,value);
		}
		left.setNoOfOutputTuples(hmLeft);
		
		//copy table names from right to left since the data generation will happen on left cvc
		HashMap<String, Integer[]> hm = right.getTableNames();
		iterator = hm.keySet().iterator();
		while (iterator.hasNext())
		{  
		   String key = iterator.next().toString();  
		   Integer[] value = hm.get(key);  
		   if(left.getTableNames().containsKey(key))
		   {
			   //FIXME:What if number of repeated relations is in double digits
			   Integer i = Integer.parseInt(key.substring(key.length()-1));
			   i += 1;
			   key = key.substring(0, key.length()-1) + i;
			   left.getTableNames().put(key, value);
		   }
		   else
			   left.getTableNames().put(key,value);
		}
		
		//Might be error with not assigning proper repeated relation count to left
		left.setRepeatedRelNextTuplePos(new HashMap<String, Integer[]>());
		left.setNoOfTuples(new HashMap<String,Integer>());
		left.setCurrentIndexCount(new HashMap<String,Integer>());
		/** Update repeated relation next position etc..*/
		Iterator<String> itr = left.getRepeatedRelationCount().keySet().iterator();
		while(itr.hasNext())
		{
			String tableName = itr.next();
			int c = left.getRepeatedRelationCount().get(tableName);
			for(int i=1;i<=c;i++)
			{
				Integer[] tuplePos = new Integer[32];
				tuplePos[1] = i;//Meaning first tuple is at pos i
				left.getRepeatedRelNextTuplePos().put(tableName+i, tuplePos);
				left.getNoOfTuples().put(tableName+i, 1);
				left.getCurrentIndexCount().put(tableName+i, i);
			}
		}
		
		/**Get the constraints for all the blocks of the query */
		left.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(left));
		left.getConstraints().add(GenerateConstraintsToKillSetOperatorMutations.generateConstraintsForMatchingTuples(cvc));
		
		//data generation for the constraints
		return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(left);
	}
	
	public static boolean generateDataWithTwoTuplesInLeftAndOneInRight(GenerateUnionCVC cvc) throws Exception
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		GenerateCVC1 right = cvc.getGenCVCright();
				
		//initialize for data generation
		left.setCount(cvc.getGenCVC().getCount());
		left.setRepeatedRelationCount(cvc.getGenCVC().getRepeatedRelationCount());
		left.setResultsetTables(cvc.getGenCVC().getResultsetTables());
		left.setResultsetColumns(cvc.getGenCVC().getResultsetColumns());
				
		
		PreProcessingActivity.setOriginalTablesForSetQuery(left, right);
		
		left.inititalizeForDataset();
		
		/** Get outer query block of this query */
		QueryBlockDetails qbt = left.getOuterBlock();
		
		//set type of mutation to be killed
		left.setTypeOfMutation(TagDatasets.MutationType.SETOP, TagDatasets.QueryBlock.OUTER_BLOCK);
		
		/**The projected columns become single valued attributes because the two tuples will have same value for this projected columns 
		 * FIXME: What if finalCount>2 */
		qbt.setSingleValuedAttributesAdd(new HashSet<Node>(qbt.getProjectedCols()));
						
		/** Initialize aggregation constraints data structure*/
		qbt.setAggConstraints(new ArrayList<Node>());
		
		/**get aggregation in this query block*/
		Vector<Node> aggConstraints = new Vector<Node>();
		if(qbt.getHavingClause() != null)
			Utilities.flattenConstraints(aggConstraints, qbt.getHavingClause());
		
		/**Update query block aggregation list*/
		qbt.setAggConstraints( new ArrayList<Node>(aggConstraints));
		
		/**get the count needed*/
		if(CountEstimationRelated.getCountNeededToKillDistinctMutation(left, qbt) == false)
			return false;

		/** get table name */
		String tableNameNo = null;
		
		Node n1 = null;
		for(Node n: qbt.getProjectedCols())
			for(QueryBlockDetails qb : left.getOuterBlock().getFromClauseSubQueries())
				if(!qb.getProjectedCols().contains(n)){
					tableNameNo = n.getTableNameNo();
					n1 = n;
				}
		
		/**If this relation is involved in equi-joins then we can ensure multiple tuples at the output, even if this relation contains a single tuple 
		 * FIXME: But what if all the relations in this equivalence class contains a single tuple*/
		for(Conjunct con: qbt.getConjuncts())
			for(Vector<Node> ec: con.getEquivalenceClasses())
				for(Node n2: ec)
					if(n2.getTableNameNo().equalsIgnoreCase(tableNameNo))
						tableNameNo = null;
		
		
		/**assign the number of tuples for the this query block*/
		if( QueryBlockDetails.getTupleAssignment(left, qbt, tableNameNo) == false)
			return false;

		/**get the tuple assignment for all other query blocks*/
		if(CountEstimationRelated.getTupleAssignmentExceptQueryBlock(left, qbt) == false)
			return false;
		
		/** Add constraints for all the blocks of the query */
		left.getConstraints().add(QueryBlockDetails.getConstraintsForQueryBlock(left));
		
		HashMap <String,Integer> hmLeft = (HashMap<String,Integer>)left.getNoOfOutputTuples().clone();
		HashMap <String,Integer> hmRight = (HashMap<String,Integer>)right.getNoOfOutputTuples().clone();
		Iterator <String> iterator = hmRight.keySet().iterator();		   
		while (iterator.hasNext())
		{  
		   String key = iterator.next().toString();  
		   Integer value = hmRight.get(key);  
		   if(hmLeft.containsKey(key))
		   {
			   value = value + hmLeft.get(key);
			   hmLeft.remove(key);
			   hmLeft.put(key, value);
		   }
		   else
			   hmLeft.put(key,value);
		}
		left.setNoOfOutputTuples(hmLeft);
		
		//copy table names from right to left since the data generation will happen on left cvc
		HashMap<String, Integer[]> hm = right.getTableNames();
		iterator = hm.keySet().iterator();
		while (iterator.hasNext())
		{  
		   String key = iterator.next().toString();  
		   Integer[] value = hm.get(key);  
		   if(left.getTableNames().containsKey(key))
		   {
			   //FIXME:What if number of repeated relations is in double digits
			   Integer i = Integer.parseInt(key.substring(key.length()-1));
			   i += 1;
			   key = key.substring(0, key.length()-1) + i;
			   left.getTableNames().put(key, value);
		   }
		   else
			   left.getTableNames().put(key,value);
		}
		
		//Might be error with not assigning proper repeated relation count to left
		left.setRepeatedRelNextTuplePos(new HashMap<String, Integer[]>());
		left.setNoOfTuples(new HashMap<String,Integer>());
		left.setCurrentIndexCount(new HashMap<String,Integer>());
		/** Update repeated relation next position etc..*/
		Iterator<String> itr = left.getRepeatedRelationCount().keySet().iterator();
		while(itr.hasNext())
		{
			String tableName = itr.next();
			int c = left.getRepeatedRelationCount().get(tableName);
			for(int i=1;i<=c;i++)
			{
				Integer[] tuplePos = new Integer[32];
				tuplePos[1] = i;//Meaning first tuple is at pos i
				left.getRepeatedRelNextTuplePos().put(tableName+i, tuplePos);
				left.getNoOfTuples().put(tableName+i, 1);
				left.getCurrentIndexCount().put(tableName+i, i);
			}
		}
		
		//get constraints for distinct tuples with same values for projected attributes
		left.getConstraints().add(GenerateConstraintsToKillDistinctMutations.getDistinctConstraints(left, left.getOuterBlock()));
		left.getConstraints().add(GenerateConstraintsToKillSetOperatorMutations.generateConstraintsForMatchingTuples(cvc));
			
		//FIXME:change name of variable to something else or handle differently
		left.getOuterBlock().setConstrainedAggregation(true);
		
		//data generation for the constraints
		return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(left);
	}
	
	public static void generateDataToKillSetOperatorMutations(GenerateUnionCVC cvc) throws Exception
	{
		boolean flag,flag2,flag3;
		
		//generate dataset 3
		GenerateUnionCVC cvcCopy = cvc.copy();
		flag = generateDataWithTwoTuplesInLeftAndNotInRight(cvcCopy);
		if(flag)
			cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
		
		//generate dataset 4
		cvcCopy = cvc.copy();
		flag2 = generateDataWithTwoTuplesInLeftAndRight(cvcCopy);
		if(flag2)
			cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
				
		//if(!flag)
		{
			//generate dataset 1
			cvcCopy = cvc.copy();
			flag = generateDataWithOneTupleInLeftAndNotInRight(cvcCopy);
			if(flag)
				cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
			
			if(!flag)
			{
				//generate dataset 2
				cvcCopy = cvc.copy();
				flag = generateDataWithOneTupleInRightAndNotInLeft(cvcCopy);
				if(flag)
					cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
			}
			
			//generate dataset 8
			cvcCopy = cvc.copy();
			flag = generateDataWithTwoTuplesInLeftAndOneInRight(cvcCopy);
			if(flag)
				cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
		}
		
		//if(!flag2)
		{
			//generate dataset 7
			cvcCopy = cvc.copy();
			flag2 = generateDataWithOneTupleInLeftAndRight(cvcCopy);
			if(flag2)
				cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
			
			//if(!flag2)
			{
				//generate dataset 2
				cvcCopy = cvc.copy();
				flag2 = generateDataWithOneTupleInRightAndNotInLeft(cvcCopy);
				if(flag2)
					cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
				
				//if(!flag)
				{
					//generate dataset 5
					cvcCopy = cvc.copy();
					flag = generateDataWithTwoTuplesInLeft(cvcCopy);
					if(flag)
						cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
					
					//if(!flag)
					{
						//generate dataset 6
						cvcCopy = cvc.copy();
						flag = generateDataWithTwoTuplesInRight(cvcCopy);
						if(flag)
							cvc.getGenCVC().setCount(cvc.getGenCVC().getCount() + 1);
					}
				}
			}
		}
	}
}