package testDataGen;

import java.util.Vector;
import java.util.logging.Logger;

import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.QueryStructure;

public class ParseForDataGeneration {

	private static Logger logger = Logger.getLogger(ParseForDataGeneration.class.getName());
	
	/**
	 * This method will be moved to new DataGenerationParser
	 * 
	 * This method converts the standard query structure from parsing to a query structure required for data generation  
	 * 
	 * @param qs
	 * @return
	 * @throws Exception
	 */
		public QueryStructure parseForDataGeneration(QueryStructure qs) throws Exception{
			
			for(ConjunctQueryStructure con : qs.getConjuncts()){
				Vector<Node> newSubQCond = new Vector<Node>();
				Vector<Node> subQCondToRemove = new Vector<Node>();
			
				Vector<ConjunctQueryStructure> newConjuntList = new Vector<ConjunctQueryStructure>();
				
				for(Node n : con.getAllSubQueryConds()){
					
					//Convert IN and NOT IN node to EXISTS and NOT EXISTS node
					if(n.getType().equals(Node.getInNodeType()) || n.getType().equals(Node.getNotInNodeType()) ){
						
						subQCondToRemove.add(n);
						//Convert IN to EXISTS
						Node inNode=new Node();
						inNode.setType(Node.getBroNodeType());
						inNode.setOperator("=");

						//Create a new node with lhs=rhs.projected column - and add it to subQConditions
						Node rhs = new Node();
						rhs.setQueryIndex(0);
						rhs.setQueryType(2);
						
						Node rhsNew = new Node();
						if(n.getRight() != null && n.getRight().getSubQueryStructure() != null){
							Node nd = n.getRight().getSubQueryStructure().getLstProjectedCols().get(0);
							if(nd!= null){
								rhsNew = nd;	
							}				
							rhsNew.setSubQueryStructure(n.getRight().getSubQueryStructure());
							rhsNew.setQueryIndex(0);
							rhsNew.setQueryType(2);
						}
						
						//Added by Shree - end
						inNode.setQueryType(2);
						inNode.setQueryIndex(0);
						inNode.setLeft(n.getLeft());
						inNode.setRight(rhsNew);
						
						//set lhsrhs node with inNode - set rhs to contain the nodes in allDnfSelCond in the subQStructure inside rhs - if vector has more nodes, add them with AND node condition recursively
						
						Node sqNode = new Node();
						sqNode.setLeft(null);
						sqNode.setRight(null);				
						sqNode.setType(inNode.getType());
						
						sqNode.setLhsRhs(inNode);
						sqNode.setType(Node.getExistsNodeType());
						
						 Vector allCon = new Vector<>();  
						 if( n.getRight().getSubQueryStructure().getAllDnfSelCond() != null
								 && ! n.getRight().getSubQueryStructure().getAllDnfSelCond() .isEmpty()){
							 
							 allCon = n.getRight().getSubQueryStructure().getAllDnfSelCond().get(0);
						 }else{
							 allCon = null;
						 }
						 if(allCon != null && !allCon.isEmpty()){
							 Node condition1 = (Node)allCon.get(allCon.size()-1);
								condition1.setQueryIndex(0);
								condition1.setQueryType(2);
								sqNode.setSubQueryConds(allCon);
								 
						 }else{
							// create the final subquery node and return it
								sqNode.setType(Node.getInNodeType());
								sqNode.setLhsRhs(inNode);						
						 }
						if(n.getType().equals(Node.getInNodeType()) ){ 
						 	sqNode.setQueryIndex(0);
							sqNode.setQueryType(2);
							
							Node lhsrhs = sqNode.getLhsRhs();
							int queryIndex = lhsrhs.getRight().getQueryIndex();
							updateSubQueryConditions(queryIndex,lhsrhs,qs);
							//left.getSubQueryConds().add(lhsrhs);
							Node newNode = new Node();
							newNode.setQueryIndex(0);
							sqNode.setLhsRhs(newNode);
							sqNode.setType(Node.getExistsNodeType());	
							newSubQCond.add(sqNode);
						}
							//return sqNode;
					if(n.getType().equals(Node.getNotInNodeType()) ){
							Node notNode = new Node();	
						
						 	notNode.setType(Node.getNotNodeType());
							notNode.setRight(null);
							notNode.setLeft(sqNode);
							notNode.setQueryIndex(0);
							notNode.setQueryType(2);
							
							Node lhsrhs = notNode.getLeft().getLhsRhs();
							int queryIndex = lhsrhs.getRight().getQueryIndex();
							if( notNode.getLeft().getSubQueryConds() != null
									&& ! notNode.getLeft().getSubQueryConds().isEmpty()){
							 notNode.getLeft().getSubQueryConds().add(lhsrhs);
							}else{
								Vector newV = new Vector();
								newV.add(lhsrhs);
								 notNode.getLeft().setSubQueryConds(newV);
							}
							 notNode.getLeft().setLhsRhs(null);
							 notNode.getLeft().setType(Node.getNotExistsNodeType());	
							 notNode.getLeft().setQueryIndex( notNode.getQueryIndex());
							 notNode.getLeft().setQueryType( notNode.getQueryType());
							 newSubQCond.add(notNode);
							//setQueryTypeAndIndex(notNode,qStruct);
							//return notNode;
					}
					}
					
					if(n.getType().equals(Node.getExistsNodeType()) || n.getType().equals(Node.getNotExistsNodeType())){
						
						subQCondToRemove.add(n);
						Node existsNode=new Node();
						existsNode.setSubQueryStructure(n.getSubQueryStructure());
						existsNode.setType(Node.getExistsNodeType());
						existsNode.setSubQueryConds(null);
						existsNode.setQueryType(2);
						existsNode.setQueryIndex(qs.getWhereClauseSubqueries().size()-1);
						
						Node notNode = new Node();
						Node sqNode = new Node();
						Node rhs = new Node();
						rhs.setQueryType(2);
						rhs.setQueryIndex(qs.getWhereClauseSubqueries().size()-1);
						sqNode.setLhsRhs(rhs);
						sqNode.setLeft(rhs);
						sqNode.setType(Node.getExistsNodeType());
						sqNode.setQueryType(2);
						sqNode.setQueryIndex(0);
						
						existsNode.setQueryType(2);
						existsNode.setQueryIndex(qs.getWhereClauseSubqueries().size()-1);				
						
						if(n.getType().equals(Node.getNotExistsNodeType())){					
							//return existsNode;
							newSubQCond.add(existsNode);
						}else{
							notNode.setType(Node.getNotNodeType());
							notNode.setRight(null);
							notNode.setLeft(sqNode);
							notNode.setLhsRhs(sqNode);
							notNode.setQueryType(2); 
							notNode.setQueryIndex(qs.getWhereClauseSubqueries().size()-1);			
							newSubQCond.add(notNode);
						}
					}
				}
				con.allSubQueryConds.addAll(newSubQCond);
				con.allSubQueryConds.removeAll(subQCondToRemove);
			}
			return qs;
		}
		
		
		/**
		 * This method updates the sub query conditions from Where/From clause subqueries to the node level. 
		 * 
		 * @param queryIndex
		 * @param lhsrhs
		 * @param qStructure
		 */
		public static void updateSubQueryConditions(int queryIndex, Node lhsrhs, QueryStructure qStructure){
			if(qStructure.getWhereClauseSubqueries() != null && !qStructure.getWhereClauseSubqueries().isEmpty()){
				if(qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond() != null && !qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond().isEmpty()){
					qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond().get(qStructure.getWhereClauseSubqueries().get(queryIndex).getAllDnfSelCond().size()-1).add(lhsrhs);
				}
				if(qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts() != null && !qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().isEmpty()){
					qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.add(lhsrhs);
				}else{
					qStructure.getWhereClauseSubqueries().get(queryIndex).setConjuncts(new Vector<ConjunctQueryStructure >());
					qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().add(new ConjunctQueryStructure(new Vector<Node>()));
					qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.add(lhsrhs);
				}
			
					//If String selection conditions are there
					if(ConjunctQueryStructure.isStringSelection(lhsrhs,1) ){
						
						String str=lhsrhs.getRight().getStrConst();
					
							qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).stringSelectionConds.add(lhsrhs);
							qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.remove(lhsrhs);
					}
					else if(ConjunctQueryStructure.isStringSelection(lhsrhs,0)){
						qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).stringSelectionConds.add(lhsrhs);
						qStructure.getWhereClauseSubqueries().get(queryIndex).getConjuncts().get(0).selectionConds.remove(lhsrhs);
				
				}
			}
		}
		
}
