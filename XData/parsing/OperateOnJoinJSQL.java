package parsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.impl.sql.compile.AndNode;
import org.apache.derby.impl.sql.compile.BinaryRelationalOperatorNode;
import org.apache.derby.impl.sql.compile.LikeEscapeOperatorNode;
import org.apache.derby.impl.sql.compile.QueryTreeNode;

import parsing.Column;
import parsing.FromListElement;
import parsing.JoinClause;
import parsing.JoinClauseInfo;
import parsing.JoinClauseNew;
import parsing.JoinTreeNode;
import parsing.Node;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.statement.select.*;

public class OperateOnJoinJSQL {

	private static Logger logger = Logger.getLogger(OperateOnJoinJSQL.class.getName());
	/**
	 * 
	 */
	public static FromListElement OperateOnJoinsJSQL(List<Join> joinList, FromItem frmItem,Vector<FromListElement> frmList, int orgSize,Vector<Node> allConds, 
			JoinTreeNode jtn,boolean isFromSubquery, boolean isWhereSubquery, QueryParser qparser, AppTest_Parameters dbApparameters) throws Exception{
		FromListElement jtan = new FromListElement();
		boolean hasOuter = false;
		Join joinItem= null;
		try{
		for(int i=0;i< joinList.size();i++){
			joinItem = joinList.get(i);
			if(joinItem.isOuter()){
				hasOuter = true;
				break;
			}
		}
		if(hasOuter){
			jtan = OperateOnOuterJoins(joinList, frmItem, frmList, orgSize, allConds, jtn, isFromSubquery, isWhereSubquery, qparser, dbApparameters);
		}else{
			jtan = OperateOnJoinNodesJSQL(joinList, frmItem, frmList, orgSize, allConds, jtn, isFromSubquery, isWhereSubquery, qparser,  dbApparameters);
		}
		}catch(Exception e){
			logger.log(Level.SEVERE,"Failed while Operating On Joins : "+e.getMessage(),e);
			throw e;
		}
		
		return jtan;
	}
	/**
	 * This method gets the join node list, and from item that holds first Join item
	 * Recursively calls the OperateOnJoinNode to get the new FromListElement and 
	 * returns the result to calling method.
	 * 
	 * In case there is a OuterJoin, then it splits the join node list to form a 
	 * binary relation node and processes it and returns the result to calling method.
	 * 
	 * @param joinList
	 * @param frmItem
	 * @param allConds
	 * @param jtn
	 * @param isFromSubquery
	 * @param isWhereSubquery
	 * @param qparser
	 * @return
	 * @throws Exception
	 */
	public static FromListElement OperateOnJoinNodesJSQL(List<Join> joinList, FromItem frmItem,Vector<FromListElement> frmList, int orgSize,Vector<Node> allConds, 
			JoinTreeNode jtn,boolean isFromSubquery, boolean isWhereSubquery, QueryParser qparser, AppTest_Parameters dbApparameters) throws Exception{
		FromListElement jtan = new FromListElement();
		
		List<Join> leftTreeList = new ArrayList();
		List<Join> rightTreeList = new ArrayList();
		Join joinNode = null;

		jtan.setAliasName(null);
		jtan.setTableName(null);
		FromItem rt = null;
		FromItem lt = null;
//		FromItem left = null;
//		FromListElement flm = new FromListElement();
		//Vector <FromListElement> frmList = new Vector<FromListElement>();
		Vector <FromListElement> t = new Vector<FromListElement>();
		try{
		Iterator joinsIt = joinList.iterator(); 
		int index = 0;
				//Copy the joinList in leftTreeList and RightTreeList for recursive processing
				while(joinsIt.hasNext()){
					Join join = (Join)joinsIt.next();	
					index ++;
					rightTreeList.add(join);
					//Last item will be processed and other items will be
					//in leftTreeList for recursive processing
					if((index != (joinList.size()))){
						leftTreeList.add(join);
						
					}
					//If join item contains sub Joins - like nested joins in from clause
					if(join.getRightItem() instanceof SubJoin){
						FromListElement temp = null; 
						FromItem fromItemOriginal = frmItem;
						frmItem = ((SubJoin)join.getRightItem()).getLeft();
						Join aJ = ((SubJoin)join.getRightItem()).getJoin();
						List<Join> jList = new ArrayList<Join>();
						jList.add(aJ);
						
						FromListElement fle =  OperateOnJoinNodesJSQL(jList, frmItem,frmList, 
								orgSize,allConds, jtn, isFromSubquery, isWhereSubquery,qparser, dbApparameters);
						
						if(leftTreeList.size()==1){		
							frmList.add(fle);
						}
						t.add(fle);
						frmItem = fromItemOriginal;
					}
					
				}
				//Code for processing other Join nodes
				if(leftTreeList.size() > 0){
					FromListElement fle =  OperateOnJoinNodesJSQL(leftTreeList, frmItem,frmList, 
							orgSize,allConds, jtn, isFromSubquery, isWhereSubquery,qparser, dbApparameters);
					if(leftTreeList.size()==1){		
						frmList.add(fle);
					}
					t.add(fle);
				}
								
				//get leftFromItem to pass if the list contains no nodes, that means only one join condition
				//exists. So the left will be the FromItem in plainselect.
				if(leftTreeList.size()==0){
					lt = frmItem;
				
				}else if(leftTreeList.size() > 0){//Last join node processed first. In this case										
					//FromItem should be previous node's fromItem
					lt=null;					
				}
				if(rightTreeList.size() > 0){
					joinNode = rightTreeList.get(rightTreeList.size()-1);	 

					rt = joinNode.getRightItem();
					rightTreeList.remove(rightTreeList.size()-1);
					Vector<FromListElement> frmListToProcess = new Vector();
					FromListElement frmListElmnt = new FromListElement();
					
					//In many places frmList.get(0) is used. 
					//So set all frmList elements in new frmListElement at position 0
					if(frmList != null && frmList.size()>0){
							for(int k=0; k<frmList.size();k++){
								frmListElmnt.addTabs(frmList.get(k));
							}
							frmListToProcess.add(frmListElmnt);
					}else 
						frmListToProcess.addAll(frmList);

					FromListElement temp = OperateOnJoinNode(joinNode,rt,lt,frmListToProcess,"",allConds,jtn,isFromSubquery,isWhereSubquery,qparser, dbApparameters);		
					ArrayList<String> joinTables=new ArrayList<String>();
					t.add(temp);
					frmList.add(temp);
					
				}

		jtan.setTabs(t);
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			e.printStackTrace();
			throw e;
		}
		return jtan;
	}
	/**
	 * This method processes the nodes for  Left Outer and Right Outer Joins
	 * 
	 */
	public static FromListElement OperateOnOuterJoins(List<Join> joinList, FromItem frmItem,Vector<FromListElement> frmList, int orgSize,Vector<Node> allConds, 
			JoinTreeNode jtn,boolean isFromSubquery, boolean isWhereSubquery, QueryParser qparser,AppTest_Parameters dbApparameters) throws Exception{
		
		//Code for Outer Joins - Start
		FromListElement jtan = new FromListElement();
		List<Join> leftTreeList = new ArrayList();
		List<Join> rightTreeList = new ArrayList();
		Join joinNode = null;
		jtan.setAliasName(null);
		jtan.setTableName(null);
		Vector <FromListElement> t = new Vector<FromListElement>();
		
		
		FromItem rt = null;
		FromItem lt = null;
		FromItem left = null;
		FromListElement flm = new FromListElement();
		String joinType;
		//Write a separate method to handle this and it should return a JoinTreeNode with left and right
		//properly and that is to be returned to calling method.
		for(int i=0;i<joinList.size();i++){
			Join join = joinList.get(i);
			if (join.isOuter()) { // checks if join is
				// HalfOuterJoin
				if (join.isRight()) // checks f join type is
					// right outer join
					joinType = JoinClauseInfo.rightOuterJoin;
				else
					joinType = JoinClauseInfo.leftOuterJoin;				
				//If it is LOJ or ROJ - divide the result join node list into 2
				//left with all previous join nodes and right with other following nodes
				//make it a binary relation node and call Operate on Join
				leftTreeList = new ArrayList();
				rightTreeList = new ArrayList();
				for(int j=0;j<i;j++){
					Join leftJoinItems = joinList.get(j);	
					leftTreeList.add(leftJoinItems);
				}
				for(int k=i;k<joinList.size();k++){
					Join rightJoinItems = joinList.get(k); 
					rightTreeList.add(rightJoinItems);
				}
				if(leftTreeList.size() > 0){
				FromListElement temp = OperateOnJoinNodesJSQL(leftTreeList,frmItem,frmList,leftTreeList.size(),
						 allConds,jtn, isFromSubquery, isWhereSubquery,qparser,dbApparameters);
				//leftFle.add(temp);
				frmList.add(temp);
				t.add(temp);
				}
				if(rightTreeList.size()>0){
				FromListElement temp1 = OperateOnJoinNodesJSQL(rightTreeList,frmItem,frmList,rightTreeList.size(),
						 allConds,jtn, isFromSubquery, isWhereSubquery,qparser,  dbApparameters);

				frmList.add(temp1);
				t.add(temp1);
				}
				//Create new jtn node with left and right and type as binary Relation node - 
				//with join type as Outer Join - end the method. 
				
				
			} else {
				joinType = JoinClauseInfo.innerJoin; // full outer join is not
				// working .. so assume all others are inner join
			}
			//Code for Outer Join - end
		}
		jtan.setTabs(t);
		return jtan;
	}

	/**
	 * This method gets the join node, the from Items of join node and previous join node
	 * to find join columns and returns the new Fromlist element to calling method
	 * 
	 * @param joinNode
	 * @param rt - This node's FromItem
	 * @param lt - Previous Join node from item , if no previous item is there, then Plainselect From item is used
	 * @param aliasName
	 * @param allConds
	 * @param jtn
	 * @param isFromSubquery
	 * @param isWhereSubquery
	 * @param qParser
	 * @return
	 * @throws Exception
	 */
	public static FromListElement OperateOnJoinNode(Join joinNode,FromItem rt,FromItem lt, Vector<FromListElement> frmItemList,
			String aliasName, Vector<Node> allConds, JoinTreeNode jtn, boolean isFromSubquery, boolean isWhereSubquery, QueryParser qParser, AppTest_Parameters dbApparameters)
					throws Exception {
		Vector<Column> joinColumns1 = null;
		Vector<Column> joinColumns2 = null;
		FromListElement jtan = new FromListElement();
		jtan.setAliasName(null);
		jtan.setTableName(null);

		FromListElement rightF = new FromListElement();
		FromListElement leftF = new FromListElement();

		Vector<FromListElement> t = new Vector<FromListElement>();
		Vector<FromListElement> leftFle = new Vector<FromListElement>();
		Vector<FromListElement> rightFle = new Vector<FromListElement>();
		
		/* The following line added by mathew on May 14 2016, 
		 * in order to store the old value of jtn, which will subsequently 
		 * be modified, this is required when lt value is null, 
		 * which indicates that the input Join is a complex join
		 * in which one of the operands itself is an Join
		 */
		JoinTreeNode tempJtn=new JoinTreeNode(jtn);

		String joinType = null;

		if (joinNode.isOuter()) { // checks if join is HalfOuterJoin
			if (joinNode.isRight()) // checks f join type is right outer join
				joinType = JoinClauseInfo.rightOuterJoin;
			else
				joinType = JoinClauseInfo.leftOuterJoin;
		} else {
			joinType = JoinClauseInfo.innerJoin; // full outer join is not
		}
		// JoinTree processing

		jtn.setNodeType(joinType);
		if (joinType.equalsIgnoreCase(JoinClauseInfo.innerJoin)) {
			jtn.setInnerJoin(true);
		} else{
			jtn.setInnerJoin(false);
		}

		JoinTreeNode leftChild = new JoinTreeNode();
		JoinTreeNode rightChild = new JoinTreeNode();
		if(lt != null){
			if (lt instanceof net.sf.jsqlparser.schema.Table) {
				FromListElement temp = OperateOnBaseTable.OperateOnBaseTableJSQL((net.sf.jsqlparser.schema.Table)lt, leftChild,qParser, isFromSubquery, isWhereSubquery);
				t.add(temp);
				leftFle.add(temp);
			} else if (lt instanceof SubSelect) {
				FromListElement temp;
				OperateOnSubQueryJSQL.OperateOnSubquery( (SubSelect)lt, allConds, leftChild, true, false,qParser, dbApparameters);
				temp=qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getQueryAliases();
				t.add(temp);
				//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of subqueries 
				//qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).parseQuery("q1", lt.toString());
				leftFle.add(temp);
				leftF.setTabs(leftFle);
			}
		}
		
		if (rt instanceof net.sf.jsqlparser.schema.Table) {
			FromListElement temp;
			temp = OperateOnBaseTable.OperateOnBaseTableJSQL((net.sf.jsqlparser.schema.Table)rt
					, rightChild, qParser, isFromSubquery, isWhereSubquery);
			t.add(temp);
			rightFle.add(temp);
		} else if (rt instanceof SubSelect) {
			FromListElement temp;
			OperateOnSubQueryJSQL.OperateOnSubquery((SubSelect) rt, allConds, rightChild,true,false,qParser, dbApparameters);
			temp=qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getQueryAliases();
			t.add(temp);
			//@author mathew on 28 June 2016, the following line added for enabling recursive parsing of subqueries 
			//qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).parseQuery("q1", rt.toString());
			rightFle.add(temp);
			rightF.setTabs(rightFle);
		} else if(rt instanceof SubJoin){
			t.addAll(frmItemList);
			rightFle.addAll(frmItemList);
		}

		jtan.setTabs(t);

		Vector<JoinClause> joinClauses = new Vector<JoinClause>();
		JoinClauseNew joinClauseNew = new JoinClauseNew(joinType);
		JoinClauseInfo joinClauseInfo = null;
		Vector<Node > leftSet, rightSet, set;//To handle aliased columns involved in joins
		leftSet = new Vector<Node>();
		rightSet = new Vector<Node>();
		set = new Vector<Node>();

		if (joinNode.getUsingColumns() != null || joinNode.isNatural() || joinNode.isOuter()) {

			List<net.sf.jsqlparser.schema.Column> columnList = null;
			List<String> li = null;
			/*
			 * If any side of join is sub query then 
			 * the join conditions should contain only the projected columns, which
			 * may be aliased,  of the sub query
			 */

			if (lt != null && lt instanceof SubSelect ){//&& lt instanceof SubSelect ){//If sub query then we should add only the projected columns

				li = getCommonColumnsForNaturalJoin(t.get(1),qParser.getFromClauseSubqueries().get
						(qParser.getFromClauseSubqueries().size()-1).getProjectedCols(),
						qParser);
				//get left and right aliased set
				set = Util.getAliasedNodes(leftFle, li,qParser);
				if(set != null)
					leftSet = set;
				set = Util.getAliasedNodes(rightFle, li,qParser);
				if(set != null)
					rightSet = set;
				joinNode.getUsingColumns();
				
			}
			if (rt instanceof SubSelect ) {//If sub query then we should add only the projected columns

				li = getCommonColumnsForNaturalJoin(t.get(0), qParser.getFromClauseSubqueries().get(
						qParser.getFromClauseSubqueries().size()-1).getProjectedCols(),qParser); 
				//get left and right aliased set
				set = Util.getAliasedNodes(leftFle, li,qParser);
				if(set != null)
					leftSet = set;
				set = Util.getAliasedNodes(rightFle, li,qParser);
				if(set != null)
					rightSet = set;
		
			}
			else if(joinNode.getUsingColumns() != null){
				columnList = new ArrayList<net.sf.jsqlparser.schema.Column>(joinNode.getUsingColumns());
			}else{
				//Shree added and commented for JOINS 
					li=getCommonColumnsForNaturalJoin(t,frmItemList,lt,qParser,isFromSubquery,isWhereSubquery);								
			}
			 
			if (columnList==null){
				//populate column list
				columnList = new ArrayList<net.sf.jsqlparser.schema.Column>();
				for (int j=0; j<li.size(); j++){
					net.sf.jsqlparser.schema.Table tempTab = new net.sf.jsqlparser.schema.Table("null","null"); 
					columnList.add(new net.sf.jsqlparser.schema.Column(tempTab,li.get(j)));
				}
			}	
			Node joinCond = null;
			for (int i = 0; i < columnList.size(); i++) {

				net.sf.jsqlparser.schema.Column column = columnList.get(i);

				if(lt!= null)
					joinColumns1= Util.getJoinColumnsJSQL(column.getColumnName().toUpperCase(),lt ,qParser);
				else if(frmItemList != null && !frmItemList.isEmpty())
					joinColumns1 = Util.getJoinColumnsForJSQL(column.getColumnName().toUpperCase(),frmItemList ,qParser);
		 
				if(rt instanceof SubJoin){
					joinColumns2 = Util.getJoinColumnsJSQL(column.getColumnName().toUpperCase(), ((SubJoin) rt).getJoin().getRightItem(),qParser);
					if(joinColumns2 == null || (joinColumns2 != null && joinColumns2.isEmpty())){
						joinColumns2 = Util.getJoinColumnsJSQL(column.getColumnName().toUpperCase(), ((SubJoin) rt).getLeft(),qParser);
					}
				}else{
					joinColumns2 = Util.getJoinColumnsJSQL(column.getColumnName().toUpperCase(), rt,qParser);
				}
				/*
				 * Here Cross join is needed. This is because a using clause
				 * does not really specify what tables it is joining. We may
				 * have A Join B Using(x). Now when we join (A Join B) with
				 * another able say R Using (x), we cannot say which x is
				 * involved in the join because from (A Join B), we get only one
				 * x. Hence we need to specify 2 join conditions as: A.x = R.x
				 * and B.x = R.x. These joins are just used to build equivalence
				 * classes.
				 */
 
				//FIXME: Mahesh add join conditions to the sub queries separately
				for (int j = 0; j < joinColumns1.size(); j++) {
					for (int k = 0; k < joinColumns2.size(); k++) {
						JoinClause joinClause = null;
						// TODO
						if(!(joinNode.getOnExpression() instanceof EqualsTo)){
						joinClauseInfo = new JoinClauseInfo(
								joinColumns1.get(j), joinColumns2.get(k),
								JoinClauseInfo.nonEquiJoinTpe);
						}else if(joinNode.getOnExpression() instanceof EqualsTo || joinNode.getOnExpression() == null){
							joinClauseInfo = new JoinClauseInfo(
									joinColumns1.get(j), joinColumns2.get(k),
									JoinClauseInfo.equiJoinType);
						}
						joinClauseNew.add(joinClauseInfo);
						qParser.getJoinClauseInfoVector().add(joinClauseInfo);

						boolean leftSubquery = false;
						/*
						 * Get equi joins also in the Node form
						 */
						Node left = new Node();
						Column leftCol = joinColumns1.get(j);
						
						
						if (lt != null && lt instanceof SubSelect){ 
							left = Util.getColumnFromOccurenceInJC(leftCol
									.getColumnName(), leftCol.getTableName().toUpperCase(), leftF, qParser);
							leftSubquery = true;
						}
						
						else{
						if(frmItemList!= null && !frmItemList.isEmpty() && !(rt instanceof SubJoin)){
							left = Util.getColumnFromOccurenceInJC(leftCol
									//.getColumnName(), leftCol.getTableName().toUpperCase(),jtan,qParser);
									.getColumnName(), leftCol.getTableName().toUpperCase(),frmItemList.get(0),qParser);
						}else{
							left = Util.getColumnFromOccurenceInJC(leftCol
									//.getColumnName(), leftCol.getTableName().toUpperCase(),jtan,qParser);
									.getColumnName(), leftCol.getTableName().toUpperCase(),jtan,qParser);						
							
						}		
						}
						left.setTableAlias("");
						left.setTable(leftCol.getTable());
						left.setType(Node.getColRefType());
						left.setLeft(null);
						left.setRight(null);

						Util.setQueryTypeAndIndex(isFromSubquery, isWhereSubquery,
								leftSubquery, left,qParser);
						
						boolean rightSubquery =false;

						Column rightCol = joinColumns2.get(k);
						Node right = new Node();
						if (rt instanceof SubSelect){ 
							right = Util.getColumnFromOccurenceInJC(rightCol
									.getColumnName(), rightCol.getTableName().toUpperCase(), rightF, qParser);
							rightSubquery = true;
						}
						else{
							right = Util.getColumnFromOccurenceInJC(rightCol
									.getColumnName(), rightCol.getTableName().toUpperCase(), jtan, qParser);
						}
						right.setTableAlias("");
						right.setTable(rightCol.getTable());
						right.setType(Node.getColRefType());
						right.setLeft(null);
						right.setRight(null);


						Util.setQueryTypeAndIndex(isFromSubquery, isWhereSubquery,
								rightSubquery, right,qParser);

						Node usingJoin = new Node();
						usingJoin.setType(Node.getBroNodeType());
						usingJoin.setOperator(getSetOperator(joinNode));
						usingJoin.setLeft(left);
						usingJoin.setRight(right);
						usingJoin.setJoinType(joinType);

						Util.setQueryTypeAndIndex(isFromSubquery, isWhereSubquery,
								rightSubquery, usingJoin,qParser);
						if(joinCond==null)
							joinCond=usingJoin;
						else{
							Node Temp= new Node();
							Temp.setType( Node.getAndNodeType());
							Temp.setLeft(joinCond);
							Temp.setRight(usingJoin);
							Util.setQueryTypeAndIndex(isFromSubquery, isWhereSubquery,
									rightSubquery, Temp, qParser);
							joinCond=Temp;
						}
						jtn.addJoinPred(usingJoin);
						//FIXME: Mahesh cross product this left and right with left and right aliased nodes
						//Need to update joinClauseInfo (get column from Node). What about aggregate Node??
						for(int l=0; l<leftSet.size(); l++){
							Node n = new Node();
							n = leftSet.get(l);

							if(n.getType().equalsIgnoreCase(Node.getAggrNodeType()))
							{
								if(!(joinNode.getOnExpression() instanceof EqualsTo)){
									joinClauseInfo = new JoinClauseInfo(
											joinColumns1.get(j), n.getAgg().getAggExp().getColumn() ,//FIXME: Second column name, we are not storing aggregate name
											JoinClauseInfo.nonEquiJoinTpe);
								}else{
								joinClauseInfo = new JoinClauseInfo(
										joinColumns1.get(j), n.getAgg().getAggExp().getColumn() ,//FIXME: Second column name, we are not storing aggregate name
										JoinClauseInfo.equiJoinType);
								}
							}
							else{
								if(!(joinNode.getOnExpression() instanceof EqualsTo)){
									joinClauseInfo = new JoinClauseInfo(
											joinColumns1.get(j), n.getColumn(),//FIXME: If aggregate Node
											JoinClauseInfo.nonEquiJoinTpe);
								}else{
								joinClauseInfo = new JoinClauseInfo(
										joinColumns1.get(j), n.getColumn(),//FIXME: If aggregate Node
										JoinClauseInfo.equiJoinType);
								}
							}
							joinClauseNew.add(joinClauseInfo);
							qParser.getJoinClauseInfoVector().add(joinClauseInfo);

							usingJoin = new Node();
							usingJoin.setType(Node.getBroNodeType());
							usingJoin.setOperator(getSetOperator(joinNode));
							
							usingJoin.setLeft(left);
							usingJoin.setRight(n);
							usingJoin.setJoinType(joinType);
							allConds.add(usingJoin);
							jtn.addJoinPred(usingJoin);

						}
						for(int m=0; m<rightSet.size(); m++){
							Node n = new Node();
							n = rightSet.get(m);

							if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){
								if(!(joinNode.getOnExpression() instanceof EqualsTo)){
									joinClauseInfo = new JoinClauseInfo(
											n.getAgg().getAggExp().getColumn() ,joinColumns2.get(j),//FIXME: Second column name, we are not storing aggregate name
											JoinClauseInfo.nonEquiJoinTpe);
								}else{
								joinClauseInfo = new JoinClauseInfo(
										n.getAgg().getAggExp().getColumn() ,joinColumns2.get(j),//FIXME: Second column name, we are not storing aggregate name
										JoinClauseInfo.equiJoinType);
								}
							}

							else{
								if(!(joinNode.getOnExpression() instanceof EqualsTo)){
									joinClauseInfo = new JoinClauseInfo(
											n.getColumn() , joinColumns2.get(k),//FIXME: If aggregate Node
											JoinClauseInfo.nonEquiJoinTpe);
								}else{
								joinClauseInfo = new JoinClauseInfo(
										n.getColumn() , joinColumns2.get(k),//FIXME: If aggregate Node
										JoinClauseInfo.equiJoinType);
								}
							}
							joinClauseNew.add(joinClauseInfo);
							qParser.getJoinClauseInfoVector().add(joinClauseInfo);

							usingJoin = new Node();
							usingJoin.setType(Node.getBroNodeType());
							usingJoin.setOperator(getSetOperator(joinNode));
							
							usingJoin.setLeft(n);
							usingJoin.setRight(right);
							usingJoin.setJoinType(joinType);
							allConds.add(usingJoin);
							jtn.addJoinPred(usingJoin);
						}
					}
				}

			}
			allConds.add(joinCond);
		} else if (joinNode.getOnExpression()!=null) {
			/*
			 * For join conditions specified as "Join On (Expr)" where Expr is
			 * an expression involving attributes from the two children of the
			 * join. R.a = S.b + 1 R.a = S.b + T.c R.a + Q.d = S.b + T.c R.a +
			 * Q.d -1 = S.b + T.c + 1 etc.
			 */
			// TODO modify the equivalence class in case of subquery
			Expression preds = joinNode.getOnExpression();


			Node n = new Node();
			//Shree changed parameter for Handling Joins
			//In case of EXPRESSIONS - This is not correct - Pass all fle items including jtan ???
			//FIXME - Create new obj Fromlist vector with jtan and frmItemList and pass it
		    FromListElement fleItem = new FromListElement();
		    fleItem.addTabs(jtan);
		    
			if(frmItemList!= null && !frmItemList.isEmpty()){
				 fleItem.addTabs(frmItemList.get(0));
				n = WhereClauseVectorJSQL.getWhereClauseVector(preds, null,fleItem,false,0,qParser,dbApparameters);//mahesh:fix this null and 0
			}else
				n = WhereClauseVectorJSQL.getWhereClauseVector(preds, null,fleItem,false,0,qParser,dbApparameters);//mahesh:fix this null and 0

			allConds.add(n);

			jtn.addJoinPred(n);
		}
		//JoinTreeNode processing
		
		/*
		 * if else added by mathew on April 14 2016, 
		 * in order avoid neglecting of the old join tree node
		 * stored in tempJtn. In the prior code, in which only
		 * the part in else block was present, old join tree node
		 * was not used
		 */
		if(lt==null){
		jtn.setLeft(tempJtn);
		jtn.setRight(rightChild);
		}
		else{
		jtn.setLeft(leftChild);
		jtn.setRight(rightChild);
		}

		//To handle left and right set cross product
		for(int i=0; i<leftSet.size(); i++){
			for(int j=0; j<rightSet.size(); j++){

				Node n = new Node();
				n = leftSet.get(i);
				Node n1 = new Node();
				n1 = rightSet.get(j);

				Column c1,c2;

				if(n.getType().equalsIgnoreCase(Node.getAggrNodeType()))
					c1 = n.getAgg().getAggExp().getColumn();
				else 
					c1 = n.getColumn();

				if(n1.getType().equalsIgnoreCase(Node.getAggrNodeType()))
					c2 = n1.getAgg().getAggExp().getColumn();
				else 
					c2 = n1.getColumn();

				if(!(joinNode.getOnExpression() instanceof EqualsTo)){

					joinClauseInfo = new JoinClauseInfo(
							c1, c2,//FIXME: If aggregate Node
							JoinClauseInfo.nonEquiJoinTpe);
				}else{
				joinClauseInfo = new JoinClauseInfo(
						c1, c2,//FIXME: If aggregate Node
						JoinClauseInfo.equiJoinType);
				}

				joinClauseNew.add(joinClauseInfo);
				qParser.getJoinClauseInfoVector().add(joinClauseInfo);
				Node usingJoin = new Node();
				usingJoin.setType(Node.getBroNodeType());
				usingJoin.setOperator(getSetOperator(joinNode));
				
				usingJoin.setLeft(n);
				usingJoin.setRight(n1);
				usingJoin.setJoinType(joinType);
				allConds.add(usingJoin);
				jtn.addJoinPred(usingJoin);

			}
		}
		return jtan;
	}

		/**Get the operator used in JoinCondition for Theta Joins
		 * 
		 * @param joinNode
		 * @return
		 */
		private static String getSetOperator(Join joinNode){
			
			String operator="";
			if(joinNode.getOnExpression() instanceof MinorThanEquals){
				operator = "<=";
			}
			else if(joinNode.getOnExpression() instanceof MinorThan){
				operator = "<";
			}
			else if(joinNode.getOnExpression() instanceof GreaterThanEquals){
				operator = ">=";
			}
			else if(joinNode.getOnExpression() instanceof GreaterThan){
				operator = ">";
			}
			else if(joinNode.getOnExpression() instanceof NotEqualsTo){
				operator = "!=";
			}
			else if(joinNode.getOnExpression() instanceof EqualsTo){
				operator = "=";
			}
			else{
				operator = "=";
			}
			return operator;
		}
		
	//If one side of join is sub query
	public static List<String> getCommonColumnsForNaturalJoin(FromListElement f1, Vector<Node> projectedCols,QueryParser qParser) throws Exception{//one part sub query and other part base query
		//get columns of right base table
		ArrayList <String> tableColumn1, tableColumn2;
		tableColumn1 = new ArrayList<String>();
		tableColumn2 = new ArrayList<String>();    	

		Vector<FromListElement> fle = new Vector<FromListElement>();
		fle.add(f1);
		tableColumn1 = Util.getAllColumnofElement(fle,qParser);

		tableColumn2 = Util.getColumnNames(projectedCols);

		tableColumn1.retainAll(tableColumn2);

		return tableColumn1;
	}

	//If both sides of join are sub queries
	public static List<String> getCommonColumnsForNaturalJoin(	Vector<Node> projectedCols1, Vector<Node> projectedCols2) throws Exception{
		ArrayList <String> tableColumn1, tableColumn2;
		tableColumn1 = new ArrayList<String>();
		tableColumn2 = new ArrayList<String>();

		tableColumn1 = Util.getColumnNames(projectedCols1);
		tableColumn2 = Util.getColumnNames(projectedCols2);

		tableColumn1.retainAll(tableColumn2);			

		return tableColumn1;
	}

	//FIXME: Mahesh....change this function to include only the columns that are projected by from clause subqueries
	public static List<String> getCommonColumnsForNaturalJoin(Vector<FromListElement> t,QueryParser qParser) throws Exception{    	

		ArrayList <String>tableColumn1,tableColumn2;
		tableColumn1=new ArrayList<String>();    		
		tableColumn2=new ArrayList<String>();


		FromListElement f1=t.get(0);
		FromListElement f2=t.get(1);

		if(f1.tableName != null && f2.tableName!=null){

			for(String columnName : qParser.getTableMap().getTable(f1.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn1.add(columnName);            	
			}			
			for(String columnName : qParser.getTableMap().getTable(f2.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn2.add(columnName);            	
			}
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;
		}else if(f1.tableName==null && f2.tableName!=null){

			tableColumn1=Util.getAllColumnofElement(f1.getTabs(),qParser);
			for(String columnName : qParser.getTableMap().getTable(f2.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn2.add(columnName);            	
			}
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;				

		}else if(f1.tableName!=null && f2.tableName==null){

			for(String columnName : qParser.getTableMap().getTable(f1.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn1.add(columnName);            	
			}			
			tableColumn2=Util.getAllColumnofElement(f2.getTabs(),qParser);			
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;							
		}
		tableColumn1 = Util.getAllColumnofElement(f1.getTabs(),qParser);
		tableColumn2=Util.getAllColumnofElement(f1.getTabs(),qParser);
		tableColumn1.retainAll(tableColumn2);			
		return tableColumn1;

	}
	
	public static List<String> getCommonColumnsForNaturalJoin(Vector<FromListElement> t,Vector<FromListElement> fleList,FromItem leftFrmItm,QueryParser qParser,boolean isFromClauseSubquery, boolean isWhereClauseSubquery) throws Exception{    	

		ArrayList <String>tableColumn1,tableColumn2;
		tableColumn1=new ArrayList<String>();    		
		tableColumn2=new ArrayList<String>();


		FromListElement f1=t.get(0);
		FromListElement f2 = null;
		FromItem f3 = null;
		String tbName2="";
	    
		f3= leftFrmItm;
		if((isFromClauseSubquery || isWhereClauseSubquery) && t.size() > 1){
			f2 = t.get(1);
		}
		else{
			if(f3!= null && f3 instanceof net.sf.jsqlparser.schema.Table){
				tbName2 = ((net.sf.jsqlparser.schema.Table)f3).getFullyQualifiedName();
			}
			if(tbName2 != null && tbName2.isEmpty()){
				if(fleList != null && !fleList.isEmpty() && fleList.size()>0){
					f2= fleList.get(0);
				} 
			}
			if(t != null && t.size() > 1 && f2 == null && tbName2.isEmpty()){
				f2 = t.get(1);
			} 
		}
		if(f1.tableName != null && f2 !=null && f2.tableName != null){
		
			for(String columnName : qParser.getTableMap().getTable(f1.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn1.add(columnName);            	
			}		
			if(f2 != null && f2.tableName != null){
			for(String columnName : qParser.getTableMap().getTable(f2.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn2.add(columnName);            	
			}
			}else if(tbName2 != null && !tbName2.isEmpty()){
				for(String columnName : qParser.getTableMap().getTable(tbName2.toUpperCase()).getColumns().keySet()){
					tableColumn2.add(columnName); }
			}
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;
		}else if(f1.tableName==null && f2 !=null && f2.tableName != null){
			
			tableColumn1=Util.getAllColumnofElementForJSQL(f1.getTabs(),new ArrayList<String>(),qParser);
			if(f2 != null && f2.tableName != null){
			for(String columnName : qParser.getTableMap().getTable(f2.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn2.add(columnName);            	
			}}else if(tbName2 != null && !tbName2.isEmpty()){
				for(String columnName : qParser.getTableMap().getTable(tbName2.toUpperCase()).getColumns().keySet()){
					tableColumn2.add(columnName); }
			}		
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;				

		}
		else if(f1.tableName !=null && f2 ==null && tbName2 != null && !tbName2.isEmpty()){

			for(String columnName : qParser.getTableMap().getTable(f1.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn1.add(columnName);            	
			}
			if(tbName2 != null && !tbName2.isEmpty()){
				for(String columnName : qParser.getTableMap().getTable(tbName2.toUpperCase()).getColumns().keySet()){
					tableColumn2.add(columnName); }
			}		
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;				

		}
		else if(f1.tableName!=null && f2 !=null && f2.tableName == null &&  !tbName2.isEmpty()){

			for(String columnName : qParser.getTableMap().getTable(f1.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn1.add(columnName);            	
			}	
			for(String columnName : qParser.getTableMap().getTable(tbName2.toUpperCase()).getColumns().keySet()){
				tableColumn2.add(columnName); }
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;	
		}
		else if((f1.tableName!=null && f2 !=null && f2.tableName == null && tbName2.isEmpty() )){
			
			for(String columnName : qParser.getTableMap().getTable(f1.tableName.toUpperCase()).getColumns().keySet()){
				tableColumn1.add(columnName);            	
			}
			if(f2 != null && f2.getTabs().size()>0){
				//tableColumn2=Util.getAllColumnofElement(f2.getTabs(),qParser);
				tableColumn2=Util.getAllColumnofElementForJSQL(f2.getTabs(),new ArrayList<String>(),qParser);			
			}
			tableColumn1.retainAll(tableColumn2);			
			return tableColumn1;							
		}
	
		if(f1 != null && f1.getTabs() != null && f1.getTabs().size()>0){
		tableColumn1 = Util.getAllColumnofElementForJSQL(f1.getTabs(),new ArrayList<String>(),qParser);
		}if(f2 != null && f2.getTabs() != null && f2.getTabs().size()>0){
		tableColumn2=Util.getAllColumnofElementForJSQL(f2.getTabs(),new ArrayList<String>(),qParser);
		}
		tableColumn1.retainAll(tableColumn2);			
		return tableColumn1;

	}

	// returns all the Binary operator node from the query tree node
	private static Vector<BinaryRelationalOperatorNode> operateOnJoinClause(
			QueryTreeNode node) {
		Vector<BinaryRelationalOperatorNode> nodes = new Vector<BinaryRelationalOperatorNode>();
		if (node instanceof BinaryRelationalOperatorNode) {
			nodes.add((BinaryRelationalOperatorNode) node);
		} else if(node instanceof LikeEscapeOperatorNode) {
			//nodes.add((LikeEscapeOperatorNode) node);
		}
		else {
			// TODO consider OR nodes also??
			AndNode andNode = (AndNode) node;
			// recurssively call for left node and right node
			Vector<BinaryRelationalOperatorNode> leftNodes = operateOnJoinClause(andNode
					.getLeftOperand());
			Vector<BinaryRelationalOperatorNode> rightNodes = operateOnJoinClause(andNode
					.getRightOperand());
			// add the leftnodes to nodes
			for (BinaryRelationalOperatorNode bNode : leftNodes)
				nodes.add(bNode);
			// add the rightnodes to nodes
			for (BinaryRelationalOperatorNode bNode : rightNodes)
				nodes.add(bNode);
		}
		return nodes;
	}

/**
 * @return 
 */
public static FromListElement getFromListForJoinItems(Join joinNode,Join leftJoinNode,FromItem rt,FromItem lt,FromItem leftFrmItm, Vector<FromListElement> frmItemList,
		String aliasName, Vector<Node> allConds, JoinTreeNode jtn, boolean isFromSubquery, boolean isWhereSubquery, QueryParser qParser, AppTest_Parameters dbApparameters) throws Exception{
	Vector<Column> joinColumns1 = null;
	Vector<Column> joinColumns2 = null;
	FromListElement jtan = new FromListElement();
	jtan.setAliasName(null);
	jtan.setTableName(null);

	FromListElement rightF = new FromListElement();
	FromListElement leftF = new FromListElement();

	Vector<FromListElement> t = new Vector<FromListElement>();
	Vector<FromListElement> leftFle = new Vector<FromListElement>();
	Vector<FromListElement> rightFle = new Vector<FromListElement>();

	String joinType = null;

	if (joinNode.isOuter()) { // checks if join is HalfOuterJoin
		if (joinNode.isRight()) // checks f join type is right outer join
			joinType = JoinClauseInfo.rightOuterJoin;
		else
			joinType = JoinClauseInfo.leftOuterJoin;
	} else {
		joinType = JoinClauseInfo.innerJoin; // full outer join is not
	}
	// JoinTree processing

	jtn.setNodeType(joinType);
	if (joinType.equalsIgnoreCase(JoinClauseInfo.innerJoin)) {
		jtn.setInnerJoin(true);
	} else{
		jtn.setInnerJoin(false);
	}

	JoinTreeNode leftChild = new JoinTreeNode();
	JoinTreeNode rightChild = new JoinTreeNode();
	if(lt != null || leftJoinNode != null){
		if(lt == null)
			lt = leftJoinNode.getRightItem();
		if (lt instanceof net.sf.jsqlparser.schema.Table) {
			FromListElement temp = OperateOnBaseTable.OperateOnBaseTableJSQL((net.sf.jsqlparser.schema.Table)lt,
					 leftChild,qParser, isFromSubquery, isWhereSubquery);
			t.add(temp);
			leftFle.add(temp);
			//Util.updateTableOccurrences(isFromSubquery, isWhereSubquery, temp,qParser);
		} else if (lt instanceof SubSelect) {
			FromListElement temp;
			//temp = 
			OperateOnSubQueryJSQL.OperateOnSubquery( (SubSelect)lt, allConds, leftChild, true, false,qParser, dbApparameters);
			temp=qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getQueryAliases();
			t.add(temp);
			leftFle.add(temp);
			leftF.setTabs(leftFle);
		}
	}
	
	if (rt instanceof net.sf.jsqlparser.schema.Table) {
		FromListElement temp;
		temp = OperateOnBaseTable.OperateOnBaseTableJSQL((net.sf.jsqlparser.schema.Table)rt
				, rightChild, qParser, isFromSubquery, isWhereSubquery);
		t.add(temp);
		rightFle.add(temp);
	} else if (rt instanceof SubSelect) {
		FromListElement temp;
		OperateOnSubQueryJSQL.OperateOnSubquery((SubSelect) rt, allConds, rightChild,true,false,qParser, dbApparameters);
		temp=qParser.getFromClauseSubqueries().get(qParser.getFromClauseSubqueries().size()-1).getQueryAliases();
		t.add(temp);
		rightFle.add(temp);
		rightF.setTabs(rightFle);
	}

	jtan.setTabs(t);
	return jtan;
}
}