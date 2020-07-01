package parsing;

import generateConstraints.AddDataBaseConstraints;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import testDataGen.GenerateCVC1;
import util.Graph;
import util.TableMap;

/**
 * Basic <i>QueryStructure</i> class contains the parsed query structure. 
 * 
 * This class uses the <i>QueryStructure</i> class. It derives the elements from queryStructure  
 * that are necessary for data generation and updates the CVC.  
 * Ex: Foreign key relations,number of foreign key tuples in output, relationship between the subqueries and 
 * outer query block, etc.,  
 * 
 * @author shree
 *
 */
public class QueryStructureForDataGen{

//Construct CVC datastructure using Q.Structure with additional info needed for data generation
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger=Logger.getLogger(QueryStructureForDataGen.class.getName());
	
	public QueryStructureForDataGen(){
		
	}
	
	
	public QueryStructureForDataGen(TableMap tableMap) {
		//super(tableMap);
		// TODO Auto-generated constructor stub
	}
	
	//Update the outer block with no Of tuples, updated table name no:'s - including subQ relations, then call foreign key relations
	//Update no of output tuples, table names, etc.,
	public static void updateTableNamesOfOriginalQuery(QueryStructure qs,GenerateCVC1 cvc) throws Exception{		
		
		//cvc.setTablesOfOriginalQuery( new Vector<Table>() );
		cvc.getTablesOfOriginalQuery().addAll( qs.getQuery().getFromTables().values() );
		
		if(qs.getFromClauseSubqueries() != null && qs.getFromClauseSubqueries().size() > 0){
			for(QueryStructure fromQs : qs.getFromClauseSubqueries()){		
				Iterator it  = fromQs.getQuery().getFromTables().keySet().iterator();
				while(it.hasNext()){
					Table t = fromQs.getQuery().getFromTables().get(it.next());
					if(!cvc.getTablesOfOriginalQuery().contains(t)){
						cvc.getTablesOfOriginalQuery().add(t);
					}
				}
				//Call the same method recursively to update Subq within this SubQuery
				updateTableNamesOfOriginalQuery(fromQs,cvc);	
			} 
			
		}
		
		if(qs.getWhereClauseSubqueries() != null && qs.getWhereClauseSubqueries().size() > 0){
			for(QueryStructure whereQs : qs.getWhereClauseSubqueries()){
				
				Iterator it  = whereQs.getQuery().getFromTables().keySet().iterator();
				while(it.hasNext()){
					Table t = whereQs.getQuery().getFromTables().get(it.next());
					if(!cvc.getTablesOfOriginalQuery().contains(t)){
						cvc.getTablesOfOriginalQuery().add(t);
					}
				}
				//Call the same method recursively to update Subq within this SubQuery
				updateTableNamesOfOriginalQuery(whereQs,cvc);	
			}
		}
		updateResultSetTables(cvc.getTablesOfOriginalQuery(),cvc);
	}
	
	public static void updateResultSetTables(Vector<Table> tables,GenerateCVC1 cvc) throws Exception{
		
		Column column;
		Table table;
		
		Iterator iter1 = cvc.getTablesOfOriginalQuery().iterator();
		
		while(iter1.hasNext()){
			Table t = (Table)iter1.next();
			
			if(t.hasForeignKey()){
				Map<String, ForeignKey> fks = t.getForeignKeys();
				
				Iterator iter2 = fks.values().iterator();
				
				while(iter2.hasNext()){
					ForeignKey fk = (ForeignKey)iter2.next();
					
					if(!cvc.getTablesOfOriginalQuery().contains(fk.getReferenceTable())){
						
						cvc.getTablesOfOriginalQuery().add(fk.getReferenceTable());
						
						iter1 = cvc.getTablesOfOriginalQuery().iterator();
					}
				}
			}
		}
		
		//Iterator t = tables.iterator();
		Iterator<Table> t = tables.iterator(); //added by rambabu
		cvc.getResultsetColumns().add(new Column("dummy","dummy"));
		
		while(t.hasNext()){
			table = (Table)t.next();
			if(cvc.getResultsetTables() != null && !cvc.getResultsetTables().contains(table)){
				cvc.getResultsetTables().add(table);
			}else if(cvc.getResultsetTables() == null){
				cvc.getResultsetTables().add(table);
			}else{
				continue;
			}
			Collection columns = table.getColumns().values();
			
			Iterator c = columns.iterator();
			
			while(c.hasNext()){
				column = (Column)c.next();
				
				column.intializeColumnValuesVector();
				// String qs = "select distinct " + column.getColumnName() + " from " + table.getTableName().toLowerCase() + " limit 50";
				String qs = "select distinct " + column.getColumnName() + " from " + table.getTableName() + " limit 50";// added by rambabu for my sql
				PreparedStatement ps = cvc.getConnection().prepareStatement(qs);
				
				ResultSet rs = ps.executeQuery();
				
				int count = 0;
				
				ResultSetMetaData rsmd = rs.getMetaData();
				
				logger.log(Level.INFO,"rsmd " + rsmd.getColumnName(1) + " " + rsmd.getColumnTypeName(1));
				
				while(rs != null && rs.next()){
					
					String temp = rs.getString(column.getColumnName().toUpperCase());
					
					if(temp != null)
					{
						column.addColumnValues(temp);
						count++;
					}
				}
				//if(rsmd.getColumnTypeName(1).equals("varchar"))
				if(rsmd.getColumnTypeName(1).equalsIgnoreCase("varchar")) // added by rambabu for mysql
				{
					while(count < 20)
					{
						count++;
						column.addColumnValues(column.getColumnName() + "_" + count);
					}
				}
				
				cvc.getResultsetColumns().add(column);
				rs.close();
				ps.close();
			}
		}
		
	}
	
	public static void updateBaseRelations(QueryStructure qs,GenerateCVC1 cvc){
		for(FromClauseElement fle : qs.getFromListElements()){
			if(fle.getAliasName() != null  && qs.getQuery().getBaseRelation().get(fle.getAliasName()) != null ){
				qs.getQuery().getBaseRelation().put(fle.getAliasName(),fle.getTableNameNo());
			}
		}
		
		if(qs.getFromClauseSubqueries() != null && qs.getFromClauseSubqueries().size() > 0){
			for(QueryStructure frmSubQ : qs.getFromClauseSubqueries()){
				for(FromClauseElement fle : frmSubQ.getFromListElements()){
					if(fle.getAliasName() != null  && frmSubQ.getQuery().getBaseRelation().get(fle.getAliasName()) != null ){
						frmSubQ.getQuery().getBaseRelation().put(fle.getAliasName(),fle.getTableNameNo());
					}
				}
				//Call the same method recursively to update Subq within this SubQuery
				updateBaseRelations(frmSubQ,cvc);
			}
		}
		
		if(qs.getWhereClauseSubqueries() != null && qs.getWhereClauseSubqueries().size() > 0){
			for(QueryStructure whrSubQ : qs.getWhereClauseSubqueries()){
				for(FromClauseElement fle : whrSubQ.getFromListElements() ){
					if(fle.getAliasName() != null   &&  whrSubQ.getQuery().getBaseRelation().get(fle.getAliasName()) != null){
				
					if(fle.getAliasName() != null){
						whrSubQ.getQuery().getBaseRelation().put(fle.getAliasName(),fle.getTableNameNo());
					}
					}
				}
				//Call the same method recursively to update Subq within this SubQuery
				updateBaseRelations(whrSubQ,cvc);
			}
		}
	}
	
	
	public static void updateTableNames(QueryStructure qs,GenerateCVC1 cvc) {
	
		Integer[] li= new Integer[2]; 		
		li[0]=0;
		li[1]=0;
		for(String rel  : qs.getLstRelationInstances()){
			cvc.getTableNames().put(rel , li);
		}
		//Update FromClauseSubQueries
		for(int i = 0; i< qs.getFromClauseSubqueries().size(); i++){	
			
			li= new Integer[2]; 		
			
			li[0]=1;
			li[1]=i;
			//li[1]=qp.getFromClauseSubqueries().size()-1;
			if(qs.getFromClauseSubqueries() != null && qs.getFromClauseSubqueries().size() > 0){
				for(QueryStructure fromQs : qs.getFromClauseSubqueries()){
					for(String rel  : fromQs.getLstRelationInstances()){
						
						cvc.getTableNames().put(rel , li);
					}
					//Call the same method recursively to populate the subQueries inside this subquery
					updateTableNames(fromQs,cvc);
				}
			}
			
			//Update from subQ occurence			
		}
		
		//update WhereClauseSubQueries
		for(int i = 0; i< qs.getWhereClauseSubqueries().size(); i++){
			
			li= new Integer[2]; 		
			
			li[0]=2;
			li[1]=i;
			//li[1]=qp.getWhereClauseSubqueries().size()-1;
			//Update from subQ occurence
			if(qs.getWhereClauseSubqueries() != null && qs.getWhereClauseSubqueries().size() > 0){
				for(QueryStructure whereQs : qs.getWhereClauseSubqueries()){
					for(String rel  : whereQs.getLstRelationInstances()){
						
						cvc.getTableNames().put(rel , li);
					}
					//Call the same method recursively to populate the subQueries inside this subquery
					updateTableNames(whereQs,cvc);
				}
			}
		}		
	}
	
	
	//Update the Foreign table details of relations used in the queryblocks
	
	public static void foreignKeyClosure(QueryStructure qStructure) {

		//If subquery tables have different foreign key mapping, they also needs to be added to the outer query structure
		//or will be in subQstructure --  chk
		
		Vector<Table> fkClosure = new Vector<Table>();
		LinkedList<Table> fkClosureQueue = new LinkedList<Table>();
		logger.log(Level.INFO,"FOREIGN KEY GRAPH : \n"+qStructure.getTableMap().foreignKeyGraph);
		for (String tableName : qStructure.getQuery().getFromTables().keySet()) {
			
			fkClosure.add( qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
			fkClosureQueue.addLast(qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
			logger.log(Level.INFO,"fkClosureQueue.add tables: \n "+qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
		}
		
			for(QueryStructure fromQs : qStructure.getFromClauseSubqueries()){
				for (String tableName : fromQs.getQuery().getFromTables().keySet()) {
					
					fkClosure.add( qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
					fkClosureQueue.addLast(qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
					logger.log(Level.INFO,"fkClosureQueue.add tables: \n "+qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
					
				}
			}
		
		for(QueryStructure whereQs : qStructure.getWhereClauseSubqueries()){
				for (String tableName : whereQs.getQuery().getFromTables().keySet()) {

					fkClosure.add( qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
					fkClosureQueue.addLast(qStructure.getTableMap().getTables().get(tableName.toUpperCase()));
					logger.log(Level.INFO,"fkClosureQueue.add tables: \n "+qStructure.getTableMap().getTables().get(tableName.toUpperCase()));

				}			
		}
				
		while(!fkClosureQueue.isEmpty())
		{
			Table table = fkClosureQueue.removeFirst();
			logger.log(Level.FINE,"fkClosureQueue Not Empty and contains table \n"+table.getTableName());
			for(Table tempTable : qStructure.getTableMap().foreignKeyGraph.getAllVertex())
			{  
				Map<Table,Vector<ForeignKey>> neighbours = qStructure.getTableMap().foreignKeyGraph.getNeighbours(tempTable);
				for(Table neighbourTable : neighbours.keySet())
				{
					if(neighbourTable.equals(table) && !fkClosure.contains(tempTable))
					{
						fkClosure.add(tempTable);
						fkClosureQueue.addLast(tempTable);
					}
				}
			}
		}
		Graph<Table, ForeignKey> tempForeignKeyGraph = qStructure.getTableMap().foreignKeyGraph.createSubGraph();
		for(Table table : fkClosure)
			tempForeignKeyGraph.add(qStructure.getTableMap().foreignKeyGraph, table);
		fkClosure = tempForeignKeyGraph.topSort();

		for(Table table : fkClosure)
			fkClosureQueue.addFirst(table);
		fkClosure.removeAllElements();
		fkClosure.addAll(fkClosureQueue);

		while(!fkClosureQueue.isEmpty())
		{
			Table table = fkClosureQueue.removeFirst();

			if(table.getForeignKeys() != null)
			{
				for (String fKeyName : table.getForeignKeys().keySet())
				{
					ForeignKey fKey = table.getForeignKey(fKeyName);
					qStructure.getForeignKeyVectorModified().add(fKey);
					Vector<Column> fKeyColumns = fKey.getFKeyColumns();
					for (Column fKeyColumn : fKeyColumns)
					{
						JoinClauseInfo foreignKey = new JoinClauseInfo(fKeyColumn, fKeyColumn.getReferenceColumn(),JoinClauseInfo.FKType);
						foreignKey.setConstant(fKeyName);
						qStructure.getForeignKeyVector().add(foreignKey);
					}
				}
			}
		}
	//Changed by Biplab till here
		qStructure.setForeignKeyVectorOriginal((Vector<JoinClauseInfo>) qStructure.getForeignKeyVector().clone());


		// Convert the closure to type Vector<Node>
		foreignKeyInNode(qStructure);
	}

	/*
	 * Convert the foreignKeyClosure of type Vector<JoinClauseInfo> to a type of
	 * Vector<Node>.
	 */

	public static void foreignKeyInNode(QueryStructure qStructure) {
		for (int i = 0; i < qStructure.getForeignKeyVector().size(); i++) {
			Node left = new Node();
			left.setColumn(qStructure.getForeignKeyVector().get(i).getJoinAttribute1());
			left.setTable(qStructure.getForeignKeyVector().get(i).getJoinAttribute1()
					.getTable());
			left.setLeft(null);
			left.setRight(null);
			left.setOperator(null);
			left.setType(Node.getColRefType());

			Node right = new Node();
			right.setColumn(qStructure.getForeignKeyVector().get(i).getJoinAttribute2());
			right.setTable(qStructure.getForeignKeyVector().get(i).getJoinAttribute2()
					.getTable());
			right.setLeft(null);
			right.setRight(null);
			right.setOperator(null);
			right.setType(Node.getColRefType());

			Node refJoin = new Node();
			refJoin.setColumn(null);
			refJoin.setTable(null);
			refJoin.setLeft(left);
			refJoin.setRight(right);
			refJoin.setType(Node.getBaoNodeType());
			refJoin.setOperator("=");
			refJoin.setStrConst(qStructure.getForeignKeyVector().get(i).getConstant());
			qStructure.getForeignKeys().add(refJoin);
		}
	}

	public static void parseForDataGeneration(QueryStructure qs){
		
	}
}
