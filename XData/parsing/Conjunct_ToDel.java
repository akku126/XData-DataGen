package parsing;

import java.io.Serializable;
import java.sql.Types;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Node;

@Deprecated
public class Conjunct_ToDel implements Serializable{

	//private static Logger logger = Logger.getLogger(Conjunct.class.getName());
	private static final long serialVersionUID = 7305518445567727632L;

	public Vector<Node> getAllConds() {
		return allConds;
	}
	public void setAllConds(Vector<Node> allConds) {
		this.allConds = allConds;
	}
	public Vector<Node> getSelectionConds() {
		return selectionConds;
	}
	public void setSelectionConds(Vector<Node> selectionConds) {
		this.selectionConds = selectionConds;
	}
	public Vector<Node> getStringSelectionConds() {
		return stringSelectionConds;
	}
	public void setStringSelectionConds(Vector<Node> stringSelectionConds) {
		this.stringSelectionConds = stringSelectionConds;
	}
	public Vector<Node> getIsNullConds() {
		return isNullConds;
	}
	public void setIsNullConds(Vector<Node> isNullConds) {
		this.isNullConds = isNullConds;
	}
	public Vector<Node> getLikeConds() {
		return likeConds;
	}
	public void setLikeConds(Vector<Node> likeConds) {
		this.likeConds = likeConds;
	}
	public Vector<Vector<Node>> getEquivalenceClasses() {
		return equivalenceClasses;
	}
	public void setEquivalenceClasses(Vector<Vector<Node>> equivalenceClasses) {
		this.equivalenceClasses = equivalenceClasses;
	}
	public Vector<Node> getAllSubQueryConds() {
		return allSubQueryConds;
	}
	public void setAllSubQueryConds(Vector<Node> allSubQueryConds) {
		this.allSubQueryConds = allSubQueryConds;
	}
	public Vector<Node> getJoinConds() {
		return joinConds;
	}
	public void setJoinConds(Vector<Node> joinConds) {
		this.joinConds = joinConds;
	}

	public Vector<Node> allConds;
	public Vector<Node> selectionConds;
	public Vector<Node> stringSelectionConds;
	public Vector<Node> isNullConds;
	public Vector<Node> likeConds;
	Vector<Vector<Node>> equivalenceClasses;
	public Vector<Node> allSubQueryConds;
	public Vector<Node> joinConds;
	public Vector<Disjunct> disjuncts;
	
	public Conjunct_ToDel(Conjunct_ToDel left,Conjunct_ToDel right){
		disjuncts=new Vector<Disjunct>();
		allConds=new Vector<Node>();
		selectionConds=new Vector<Node>();
		stringSelectionConds= new Vector<Node>();
		isNullConds=new Vector<Node>();
		likeConds=new Vector<Node>();
		equivalenceClasses=new Vector<Vector<Node>>();
		allSubQueryConds=new Vector<Node>();
		joinConds=new Vector<Node>();
		allConds.addAll(left.getAllConds());
		allConds.addAll(right.getAllConds());
		selectionConds.addAll(left.getSelectionConds());
		selectionConds.addAll(right.getSelectionConds());
		stringSelectionConds.addAll(left.getStringSelectionConds());
		stringSelectionConds.addAll(right.getStringSelectionConds());
		isNullConds.addAll(left.getIsNullConds());
		isNullConds.addAll(right.getIsNullConds());
		likeConds.addAll(left.getLikeConds());
		likeConds.addAll(right.getLikeConds());
		equivalenceClasses.addAll(left.getEquivalenceClasses());
		equivalenceClasses.addAll(right.getEquivalenceClasses());
		allSubQueryConds.addAll(left.getAllSubQueryConds());
		allSubQueryConds.addAll(right.getAllSubQueryConds());
		joinConds.addAll(left.getJoinConds());
		joinConds.addAll(right.getJoinConds());
	}
	
	public Conjunct_ToDel(ANDNode andNode){
		allConds=new Vector<Node>();
		selectionConds=new Vector<Node>();
		stringSelectionConds = new Vector<Node>();
		isNullConds = new Vector<Node>();
		likeConds = new Vector<Node>();
		allSubQueryConds = new Vector<Node>();
		joinConds = new Vector<Node>();
		equivalenceClasses = new Vector<Vector<Node>>();
		Node temp;
		Vector<Node> allCondsDuplicate = (Vector<Node>) andNode.leafNodes.clone();
		allConds.addAll((Vector<Node>)andNode.leafNodes.clone());
		disjuncts=new Vector<Disjunct>();
		
		for(Node n:allCondsDuplicate){
			String type=n.getType();
			/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
			 * from the If condition below removed by mathew on 29 June 2016
			 * corresponding All node type and Any node type expressions added
			 */
			if(type.equalsIgnoreCase(Node.getAllNodeType()) || type.equalsIgnoreCase(Node.getAnyNodeType())
					|| type.equalsIgnoreCase(Node.getInNodeType()) ||
					type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
					||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
				allSubQueryConds.add(n);
				allConds.remove(n);
			}
		}
		
		
		// Now separate Join Conds for EC And Selection Conds and Non Equi join
		// conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);
			boolean isJoinNodeForEC =  false;//GetNode.getJoinNodesForEC(this,temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeForEC) {
				isJoinNodeForEC = false;
				allConds.remove(temp);
			}
		}

		
		// Now separate Selection conds into the vector Selection Conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);
			boolean isSelection = false;//GetNode.getSelectionNode(this,temp);
			if (isSelection) {
				isSelection = false;
				// remove it from allConds as it is added to selection
				// conditions
				allConds.remove(temp);
			}
		}
		
		
		//Added by Bikash----------------------------------------------------
		//For the like operator
		for(int i=0;i<allCondsDuplicate.size();i++){
			temp = allCondsDuplicate.get(i);
			boolean isLikeType = false;//GetNode.getLikeNode(this,temp);
			if(isLikeType){
				isLikeType = false;
				//remove it from allConds as it is added to like conditions
				allConds.remove(temp);
			}
		}
		

		for(Node n:allCondsDuplicate){
			if(n.getType().equals(Node.getIsNullNodeType())){
				isNullConds.add(n);
				allConds.remove(n);
			}
		}
		
		for(ORNode orNode: andNode.orNodes){
			disjuncts.add(new Disjunct(orNode));
		}
		
	}
	
	public Conjunct_ToDel(Vector<Node> allCond) {
		// TODO Auto-generated constructor stub
		allConds=new Vector<Node>();
		selectionConds=new Vector<Node>();
		stringSelectionConds = new Vector<Node>();
		isNullConds = new Vector<Node>();
		likeConds = new Vector<Node>();
		allSubQueryConds = new Vector<Node>();
		joinConds = new Vector<Node>();
		equivalenceClasses = new Vector<Vector<Node>>();
		Node temp;
		Vector<Node> allCondsDuplicate = (Vector<Node>) allCond.clone();
		allConds.addAll((Vector<Node>)allCond.clone());
		disjuncts=new Vector<Disjunct>();
		
		for(Node n:allCondsDuplicate){
			String type=n.getType();
			/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
			 * from the If condition below removed by mathew on 29 June 2016
			 * corresponding All node type and Any node type expressions added
			 */
			if(type.equalsIgnoreCase(Node.getAllNodeType()) || type.equalsIgnoreCase(Node.getAnyNodeType())
					|| type.equalsIgnoreCase(Node.getInNodeType()) ||
					type.equalsIgnoreCase(Node.getExistsNodeType()) || type.equalsIgnoreCase(Node.getBroNodeSubQType())
					||type.equalsIgnoreCase(Node.getNotExistsNodeType())){
				allSubQueryConds.add(n);
				allConds.remove(n);
			}
		}
		
		
		// Now separate Join Conds for EC And Selection Conds and Non Equi join
		// conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);
			boolean isJoinNodeForEC = false;// GetNode.getJoinNodesForEC(this,temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeForEC) {
				isJoinNodeForEC = false;
				allConds.remove(temp);
			}
		}

		
		// Now separate Selection conds into the vector Selection Conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);
			boolean isSelection = false;//GetNode.getSelectionNode(this,temp);
			if (isSelection) {
				isSelection = false;
				// remove it from allConds as it is added to selection
				// conditions
				allConds.remove(temp);
			}
		}
		
		
		//Added by Bikash----------------------------------------------------
		//For the like operator
		for(int i=0;i<allCondsDuplicate.size();i++){
			temp = allCondsDuplicate.get(i);
			boolean isLikeType = false;//GetNode.getLikeNode(this,temp);
			if(isLikeType){
				isLikeType = false;
				//remove it from allConds as it is added to like conditions
				allConds.remove(temp);
			}
		}
		

		for(Node n:allCondsDuplicate){
			if(n.getType().equals(Node.getIsNullNodeType())){
				isNullConds.add(n);
				allConds.remove(n);
			}
		}
		
		//TODO: Consider subquery conditions.  
		

		//Now get the lhsRhs conditions in a separate vector, lhsRhsConds
		//This has to be added in each and every killing procedure as positive cond
	/*	for(int i=0;i<allSubQueryConds.size();i++){
			Node n = allSubQueryConds.get(i);
			if(n.getLhsRhs()==null)	
				continue;
			Vector<Node> lhsRhs = flattenNode(n.getLhsRhs());
			lhsRhsConds.addAll(lhsRhs);				//Why is this variable required???
		}
		
		for(Node n:allSubQueryConds){
			Vector<Node> subQConds=(Vector<Node>)n.getSubQueryConds().clone();
			n.getSubQueryConds().removeAllElements();
			for(Node subQ:subQConds){
				n.getSubQueryConds().addAll(flattenNode(subQ));
				//n.setSubQueryConds(flattenNode(subQ));
			}
		}*/
	}
	/*public boolean getJoinNodesForEC(Node temp) {
		if (temp.getType().equalsIgnoreCase(Node.getBroNodeType())
				&& temp.getOperator().equalsIgnoreCase("=")) {
			if (temp.getLeft().getType().equalsIgnoreCase(Node.getColRefType())
					&& temp.getRight().getType().equalsIgnoreCase(
							Node.getColRefType())) {
				joinConds.add(temp);
				return true;
			}
		}
		return false;
	}
	public boolean getSelectionNode(Node temp) {
		if (temp.containsConstant()) {
			selectionConds.add(temp);
			return true;
		}
		if(temp.getType().equals(Node.getBroNodeType()) && !temp.getOperator().equals("=")){
			selectionConds.add(temp);
			return true;
		}
		return false;
	}
	
	public boolean getLikeNode(Node temp){
		if(temp.getType().equalsIgnoreCase(Node.getLikeNodeType())){//CharConstantNode
			likeConds.add(temp);
			return true;
		}
		return false;
	}*/
	
	public void createEqClass(){

		Node temp;
		HashMap<Integer, Node> hm = new HashMap<Integer, Node>();
		// First flatten all the join/selection condition trees
		// And select the join conditions in a special vector joinConds
		
				
		// Now create equivalence classes from joinConds
		
		Vector<Node> allJoinConds = new Vector<Node>();
		allJoinConds.addAll(joinConds);
		// allJoinConds.addAll(subQJC);
		Vector<Node> ec = new Vector<Node>();
		temp = new Node();
		Node temp1, temp2;
		Vector<Node> tempAllJoinConds = (Vector) allJoinConds.clone();
		
		int count = 0;
		int flag = 0;
		while (tempAllJoinConds.size() > 0) {
			
			temp = tempAllJoinConds.get(0).getLeft();
			ec.add(temp);
			temp = tempAllJoinConds.get(0).getRight();
			ec.add(temp);
			tempAllJoinConds.remove(0);
			int cond_iter = 0;
			for (int j = 0; j < ec.size(); j++) {
				// count = 0;
				for (int k = 0; k < tempAllJoinConds.size(); k++) {
					temp1 = tempAllJoinConds.get(k);
					temp2 = ec.get(j);

					//Conjunct.logger.log(Level.INFO,"Comparing " + temp1.toString()+ " and " + temp2.toString());

					if (temp1.getLeft() != null) {
						if (temp1.getLeft().getColumn().getColumnName()
								.equalsIgnoreCase(
										temp2.getColumn().getColumnName())
								&& temp1.getLeft().getTableNameNo().toUpperCase()
										.equalsIgnoreCase(
												temp2.getTableNameNo().toUpperCase())) {
							
						//	Conjunct.logger.log(Level.INFO,"Added " + temp1.toString());
							ec.add(temp1.getLeft());
							ec.add(temp1.getRight());
							tempAllJoinConds.remove(k);
							k = 0;
							// count--;
							// flag = 1;
						} else if (temp1.getRight().getColumn().getColumnName()
								.equalsIgnoreCase(
										temp2.getColumn().getColumnName())
								&& temp1.getRight().getTableNameNo().toUpperCase()
										.equalsIgnoreCase(
												temp2.getTableNameNo().toUpperCase())) {
							
						//	Conjunct.logger.log(Level.INFO,"Added " + temp1.toString());
							ec.add(temp1.getLeft());
							ec.add(temp1.getRight());
							tempAllJoinConds.remove(k);
							k = 0;
							// count--;
							// flag = 1;
						}
					}
					// if(flag == 1){
					// flag=0;
					// }
					// else
					// count++;
				}
			}

			// tempAllJoinConds.remove(count);
			equivalenceClasses.add(new Vector<Node>(ec));

			//Conjunct.logger.log(Level.INFO,"Added EC: " + ec.toString());
			ec.removeAllElements();
		}
		removeDuplicates(equivalenceClasses);
		for(Disjunct disjunct:disjuncts){
			disjunct.createEqClass();
		}
	}
	public void removeDuplicates(Vector<Vector<Node>> ecs) {
		for (int i = 0; i < ecs.size(); i++) {
			Vector<Node> ec = ecs.get(i);
			for (int j = 0; j < ec.size(); j++) {
				for (int k = j + 1; k < ec.size(); k++) {
					if (ec.get(j).getTable() == ec.get(k).getTable()
							&& ec.get(j).getColumn() == ec.get(k).getColumn()
							&& ec.get(j).getTableAlias() != null 
							&& ec.get(k).getTableAlias() != null 
							&& ec.get(j).getTableAlias().equalsIgnoreCase(
									ec.get(k).getTableAlias())) {
						ec.removeElementAt(j);
						j = -1;
						break;
					}
				}
			}
		}
	}
	
	public void seggregateSelectionConds(){
		Vector<Node> selectCondsClone = (Vector<Node>)selectionConds.clone();
		for(Node n: selectCondsClone){			
			if(isStringSelection(n,1) ){
				String str=n.getRight().getStrConst();
				if(str!=null && !str.contains("'"))
					n.getRight().setStrConst("'"+str+"'");
				stringSelectionConds.add(n);
				selectionConds.remove(n);
			}
			else if(isStringSelection(n,0)){
				stringSelectionConds.add(n);
				selectionConds.remove(n);
			}
			else if(n.getLeft().getColumn()!=null && n.getLeft().getColumn().getCvcDatatype().equals("DATE")){
				if(n.getRight() != null && n.getRight().getLeft() != null && !(n.getRight().getLeft().getNodeType().equals(Node.getExtractFuncType()))){
					java.sql.Date d=java.sql.Date.valueOf(n.getRight().getStrConst());
					n.getRight().setStrConst(""+d.getTime()/86400000);
				}else if(n.getRight() != null && n.getRight().getLeft() != null && n.getRight().getLeft().getNodeType().equals(Node.getExtractFuncType())){
					n.getRight().setLeft(null);
				}
			}
			else if(n.getLeft().getColumn()!=null && n.getLeft().getColumn().getCvcDatatype().equals("TIME")){
				java.sql.Time t=java.sql.Time.valueOf(n.getRight().getStrConst());
				n.getRight().setStrConst(""+(t.getTime()%86400)/1000);
			}
			else if(n.getLeft().getColumn()!=null && n.getLeft().getColumn().getCvcDatatype().equals("TIMESTAMP")){
				java.sql.Timestamp ts=java.sql.Timestamp.valueOf(n.getRight().getStrConst());
				n.getRight().setStrConst(""+ts.getTime()/1000);
			}
			
		}
		for(Node n :likeConds){
			n.getRight().setStrConst("'"+n.getRight().getStrConst()+"'");
		}
		for(Node n:allSubQueryConds){
			Vector<Node> subQ=n.getSubQueryConds();
			if(subQ!=null){
				for(Node subQCond:subQ){
					if(subQCond.getRight() == null) continue;
					String str=subQCond.getRight().getStrConst();
					if(str==null) continue;
					if(isStringSelection(subQCond,1) ){
						subQCond.getRight().setStrConst("'"+str+"'");
					}
					else if(subQCond.getLeft().getColumn().getCvcDatatype().equals("DATE")){
						java.sql.Date d=java.sql.Date.valueOf(str);
						subQCond.getRight().setStrConst(""+d.getTime()/86400000);
					}
					else if(subQCond.getLeft().getColumn().getCvcDatatype().equals("TIME")){
						java.sql.Time t=java.sql.Time.valueOf(str);
						subQCond.getRight().setStrConst(""+(t.getTime()%86400)/1000);
					}
					else if(subQCond.getLeft().getColumn().getCvcDatatype().equals("TIMESTAMP")){
						java.sql.Timestamp ts=java.sql.Timestamp.valueOf(str);
						subQCond.getRight().setStrConst(""+ts.getTime()/1000);
					}
				
				}
			
			}
		}
		for(Disjunct disjunct:disjuncts){
			disjunct.seggregateSelectionConds();
		}
	}
	
	public static boolean isStringSelection(Node n,int flag) {
		if(n.getLeft().getType().equals(Node.getColRefType())/* */){
			if(flag==1  && !n.getRight().getType().equals(Node.getValType()))
				return false;
			int i=n.getLeft().getColumn().getDataType();
			if(i== Types.VARCHAR || i==Types.CHAR || i==Types.LONGVARCHAR){
				return true;
			}
		}
		
		return false;
	}
	
	
	
	public static void createConjunct(QueryStructure qStructure, Vector<Vector<Node>> dnfCond2){
		for(Vector<Node> conjunct: dnfCond2){
			qStructure.conjuncts.add(new ConjunctQueryStructure(conjunct));		
		}
	}
	
	public static void createConjuncts(QueryStructure qStructure){
		if(qStructure.orNode != null) {
			for(ANDNode andNode:qStructure.orNode.andNodes){
				qStructure.conjuncts.add(new ConjunctQueryStructure(andNode));
			}
			if(!qStructure.orNode.leafNodes.isEmpty())
				qStructure.conjuncts.add(new ConjunctQueryStructure(qStructure.orNode.leafNodes));
		}
		
	}
}
