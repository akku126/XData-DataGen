package parsing;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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

public class QueryStructureDatagen extends QueryStructure{
	 static Logger logger = Logger.getLogger(QueryStructureDatagen.class.getName());
		private static final long serialVersionUID = 8049915037697741933L;
		
	
	public QueryStructureDatagen(TableMap tableMap) {
		super(tableMap);
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
		
		Iterator t = tables.iterator();
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
				
				String qs = "select distinct " + column.getColumnName() + " from " + table.getTableName() + " limit 50";
				
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
				if(rsmd.getColumnTypeName(1).equals("varchar"))
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
			logger.log(Level.INFO,"fkClosureQueue Not Empty and contains table \n"+table.getTableName());
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
	
	
	
	
	

	 /* Revamp allConds. It should now contain the distinct predicates not
	 * containing a AND (or OR but ORs not considered for the moment) TODO: Do
	 * something about the presence of ORs: Need to convert the predicate into
	 * CNF and then create datasets by nulling each pair Eg.: if R.a = S.b OR
	 * T.c = U.d is the predicate, then create datasets by killing each of the
	 * following: 1. R.a and Tc 2. R.a and U.d 3. S.b and T.c 4. S.b and U.d 
	 */

	public static void flattenAndSeparateAllConds(QueryStructure qParser) {
		if(qParser.allConds == null)
			return ;

		Vector<Node> allCondsDuplicate;
		allCondsDuplicate = (Vector<Node>) qParser.allConds.clone();

		qParser.allConds.removeAllElements();
		Vector<Vector<Node>> allDnfDuplicate;
		allDnfDuplicate =(Vector<Vector<Node>>) qParser.dnfCond.clone();

		qParser.dnfCond.removeAllElements();
		Node temp;
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			if(allCondsDuplicate.get(i) != null)
				qParser.allConds.addAll(parsing.GetNodeQueryStructure.flattenNode(qParser, allCondsDuplicate.get(i)));
		}
	
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			if(allCondsDuplicate.get(i) != null)
				qParser.dnfCond.addAll(parsing.GetNodeQueryStructure.flattenCNF(qParser, allCondsDuplicate.get(i)));
		}			


		for (int i=0;i< allCondsDuplicate.size() ; i++) {
			if(allCondsDuplicate.get(i)!=null){
			ORNode t = GetNodeQueryStructure.flattenOr(allCondsDuplicate.get(i));
				for(Node n: t.leafNodes){
					qParser.orNode.leafNodes.add(n);
				}
				
				for(ANDNode n: t.andNodes){
					qParser.orNode.andNodes.add(n);
				}
				
//				qParser.orNode=GetNodeQueryStructure.flattenOr(allCondsDuplicate.get(i));
			}

		}

		ConjunctQueryStructure.createConjuncts(qParser);

		allCondsDuplicate.removeAllElements();
		allCondsDuplicate = (Vector<Node>) qParser.allConds.clone();

		allDnfDuplicate.removeAllElements();
		allDnfDuplicate = (Vector<Vector<Node>>) qParser.dnfCond.clone();

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				String type=n.getType();
				/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
				 * from the If condition below removed by mathew on 29 June 2016
				 * corresponding All node type and Any node type expressions added
				 */

				if(type.equalsIgnoreCase(Node.getAllNodeType()) || type.equalsIgnoreCase(Node.getAnyNodeType())
						|| type.equalsIgnoreCase(Node.getInNodeType()) ||
						type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
						||type.equalsIgnoreCase(Node.getNotInNodeType())//added by mathew on 17 oct 2016
						||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
					if(n.getSubQueryConds() != null){
					subCond.addAll(n.getSubQueryConds());
					}
					else{
						subCond.add(n);
					}
					temp1.remove(n);
				}
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.allDnfSubQuery.add(subCond);
			}
		}

		for(Node n:allCondsDuplicate){
			String type=n.getType();
			/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
			 * from the If condition below removed by mathew on 29 June 2016
			 * corresponding All node type and Any node type expressions added
			 */
			if((type.equalsIgnoreCase(Node.getAllNodeType())
					||type.equalsIgnoreCase(Node.getAnyNodeType())
					|| type.equalsIgnoreCase(Node.getInNodeType()) ||
					type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
					||type.equalsIgnoreCase(Node.getNotInNodeType())
					||type.equalsIgnoreCase(Node.getNotExistsNodeType()))){
				qParser.allSubQueryConds.add(n);
				qParser.allConds.remove(n);
			}
		}

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
						&& n.getOperator().equalsIgnoreCase("=")) {
					if (n.getLeft()!=null && n.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
							&& n.getRight()!=null&& n.getRight().getType().equalsIgnoreCase(
									Node.getColRefType())) {
						subCond.add(n);
						temp1.remove(n);
					}
				}

			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.dnfJoinCond.add(subCond);
			}
		}
		

		// Now separate Join Conds for EC And Selection Conds and Non Equi join
		// conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);

			ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());

			boolean isJoinNodeForEC = GetNodeQueryStructure.getJoinNodesForEC(con, temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeForEC) {
				isJoinNodeForEC = false;
				qParser.joinConds.add(temp);//added by mathew on 17 oct 2016
				qParser.allConds.remove(temp);
			}			
		}
		
		// added by mathew on 18 oct 2016
		// Now separate Non-Equi/Outer join conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);

			ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());

			boolean isJoinNodeAllOther = parsing.GetNodeQueryStructure.getJoinNodesAllOther(con, temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeAllOther) {
				isJoinNodeAllOther = false;
				qParser.joinConds.add(temp);//added by mathew on 17 oct 2016
				qParser.allConds.remove(temp);
			}			
		}

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if (n.containsConstant()) {
					subCond.add(n);
					temp1.remove(n);

				}
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.allDnfSelCond.add(subCond);
			}
		}

		// Now separate Selection conds into the vector Selection Conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);

			ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());

			boolean isSelection = GetNodeQueryStructure.getSelectionNode(con,temp);
			if (isSelection) {
				isSelection = false;
				// remove it from allConds as it is added to selection
				// conditions
				qParser.allConds.remove(temp);
			}
		}

		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			//Vector<Node> 
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if(n.getType().equalsIgnoreCase(Node.getLikeNodeType())){//CharConstantNode
					subCond.add(n);
					temp1.remove(n);

				}    
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.dnfLikeConds.add(subCond);
			}
		}

		//Added by Bikash----------------------------------------------------
		//For the like operator
		for(int i=0;i<allCondsDuplicate.size();i++){
			temp = allCondsDuplicate.get(i);

			ConjunctQueryStructure con = new ConjunctQueryStructure( new Vector<Node>());
			boolean isLikeType = GetNodeQueryStructure.getLikeNode(con,temp);
			if(isLikeType){
				isLikeType = false;
				//remove it from allConds as it is added to like conditions
				qParser.allConds.remove(temp);
			}
		}

		//***************************************************************************/
		for(Vector<Node> conjunct:allDnfDuplicate)
		{
			//Vector<Node> 
			Vector<Node> subCond=new Vector<Node>();
			Vector<Node> temp1 = new Vector<Node>();
			temp1=(Vector<Node>) conjunct.clone();
			for(Node n:conjunct)
			{
				if(n.getType().equals(Node.getIsNullNodeType())){
					subCond.add(n);
					temp1.remove(n);

				}    
			}
			qParser.dnfCond.remove(conjunct);
			if(!temp1.isEmpty())
			{
				qParser.dnfCond.add(temp1);
			}
			if(!subCond.isEmpty())
			{
				qParser.dnfIsNullConds.add(subCond);
			}
		}

		for(Node n:allCondsDuplicate){
			if(n.getType().equals(Node.getIsNullNodeType())){
				qParser.isNullConds.add(n);
				qParser.allConds.remove(n);
			}
		}
		//Now get the lhsRhs conditions in a separate vector, lhsRhsConds
		//This has to be added in each and every killing procedure as positive cond
		for(int i=0;i<qParser.allSubQueryConds.size();i++){
			Node n = qParser.allSubQueryConds.get(i);
			if(n.getLhsRhs()==null || n.getType().equalsIgnoreCase(Node.getExistsNodeType()) || n.getType().equalsIgnoreCase(Node.getNotExistsNodeType()))	
				continue;
			Vector<Node> lhsRhs = parsing.GetNodeQueryStructure.flattenNode(qParser, n.getLhsRhs());
			qParser.lhsRhsConds.addAll(lhsRhs);				//Why is this variable required???
		}

		for(Node n: qParser.allSubQueryConds){
			if(n.getSubQueryConds()!=null){
				Vector<Node> subQConds=(Vector<Node>)n.getSubQueryConds().clone();
				n.getSubQueryConds().removeAllElements();
				for(Node subQ:subQConds){
					n.getSubQueryConds().addAll(parsing.GetNodeQueryStructure.flattenNode(qParser,subQ));
					//n.setSubQueryConds(flattenNode(subQ));
				}
			}
		}

	}
	
}	
