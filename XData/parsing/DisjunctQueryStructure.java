package parsing;

import java.sql.Types;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.ANDNode;
import parsing.ORNode;
import parsing.Node;

public class DisjunctQueryStructure {
	private static Logger logger = Logger.getLogger(DisjunctQueryStructure.class.getName());
	public Vector<Node> allConds;
	public Vector<Node> selectionConds;
	public Vector<Node> stringSelectionConds;
	public Vector<Node> isNullConds;
	public Vector<Node> likeConds;
	Vector<Vector<Node>> equivalenceClasses;
	public Vector<Node> allSubQueryConds;
	public Vector<Node> joinCondsForEquivalenceClasses;
	public Vector<Node> joinCondsAllOther;
	public Vector<ConjunctQueryStructure> conjuncts;
	
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
	public Vector<Node> getJoinCondsForEquivalenceClasses() {
		return joinCondsForEquivalenceClasses;
	}
	public void setJoinCondsForEquivalenceClasses(Vector<Node> joinConds) {
		this.joinCondsForEquivalenceClasses = joinConds;
	}
	public Vector<Node> getJoinCondsAllOther() {
		return joinCondsAllOther;
	}
	public void setJoinCondsAllOther(Vector<Node> joinConds) {
		this.joinCondsAllOther = joinConds;
	}	
	public DisjunctQueryStructure(ORNode orNode){
		allConds=new Vector<Node>();
		selectionConds=new Vector<Node>();
		stringSelectionConds = new Vector<Node>();
		isNullConds = new Vector<Node>();
		likeConds = new Vector<Node>();
		allSubQueryConds = new Vector<Node>();
		joinCondsForEquivalenceClasses = new Vector<Node>();
		joinCondsAllOther = new Vector<Node>();
		equivalenceClasses = new Vector<Vector<Node>>();
		Node temp;
		Vector<Node> allCondsDuplicate = (Vector<Node>) orNode.leafNodes.clone();
		allConds.addAll((Vector<Node>)orNode.leafNodes.clone());
		conjuncts=new Vector<ConjunctQueryStructure>();
		
		for(Node n:allCondsDuplicate){
			String type=n.getType();
			/* the expression: type.equalsIgnoreCase(Node.getAllAnyNodeType()) 
			 * from the If condition below removed by mathew on 29 June 2016
			 * corresponding All node type and Any node type expressions added
			 */
			if(type.equalsIgnoreCase(Node.getAllNodeType())
					||type.equalsIgnoreCase(Node.getAnyNodeType())
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
			boolean isJoinNodeForEC =  GetNodeQueryStructure.getJoinNodesForEC(this,temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeForEC) {
				isJoinNodeForEC = false;
				allConds.remove(temp);
			}
		}
		
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);
			boolean isJoinNodeAllOther =  GetNodeQueryStructure.getJoinNodesAllOther(this,temp);
			// Remove that object from allConds. Because that will now be a part
			// of some or the other equivalence class and be handeled
			if (isJoinNodeAllOther) {
				isJoinNodeAllOther = false;
				allConds.remove(temp);
			}
		}

		
		// Now separate Selection conds into the vector Selection Conds
		for (int i = 0; i < allCondsDuplicate.size(); i++) {
			temp = allCondsDuplicate.get(i);
			boolean isSelection = GetNodeQueryStructure.getSelectionNode(this,temp);
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
			boolean isLikeType = GetNodeQueryStructure.getLikeNode(this,temp);
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
		
		for(ANDNode andNode: orNode.andNodes){
			conjuncts.add(new ConjunctQueryStructure(andNode));
		}
	}
	
	public void createEqClass(){

		Node temp;
		HashMap<Integer, Node> hm = new HashMap<Integer, Node>();
		// First flatten all the join/selection condition trees
		// And select the join conditions in a special vector joinConds
		
				
		// Now create equivalence classes from joinConds

		Vector<Node> allJoinConds = new Vector<Node>();
		allJoinConds.addAll(joinCondsForEquivalenceClasses);
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
					logger.log(Level.FINE,"Comparing " + temp1.toString()
							+ " and " + temp2.toString());

					if (temp1.getLeft() != null) {
						if (temp1.getLeft().getColumn().getColumnName()
								.equalsIgnoreCase(
										temp2.getColumn().getColumnName())
								&& temp1.getLeft().getColumn().getTableName()
										.equalsIgnoreCase(
												temp2.getColumn()
														.getTableName())) {
							logger.log(Level.INFO,"Added " + temp1.toString());
							ec.add(temp1.getLeft());
							ec.add(temp1.getRight());
							tempAllJoinConds.remove(k);
							k = 0;
							// count--;
							// flag = 1;
						} else if (temp1.getRight().getColumn().getColumnName()
								.equalsIgnoreCase(
										temp2.getColumn().getColumnName())
								&& temp1.getRight().getColumn().getTableName()
										.equalsIgnoreCase(
												temp2.getColumn()
														.getTableName())) {
							logger.log(Level.INFO,"Added " + temp1.toString());
							
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
			logger.log(Level.INFO,"Added EC: " + ec.toString());
			ec.removeAllElements();
		}
		removeDuplicates(equivalenceClasses);
		for(ConjunctQueryStructure conjunct:conjuncts){
			conjunct.createEqClass();
		}
	}

	public void removeDuplicates(Vector<Vector<Node>> ecs) {
		for (int i = 0; i < ecs.size(); i++) {
			Vector<Node> ec = ecs.get(i);
			for (int j = 0; j < ec.size(); j++) {
				for (int k = j + 1; k < ec.size(); k++) {
					if (ec.get(j).getTable() == ec.get(k).getTable()
							&& ec.get(j).getColumn() == ec.get(k).getColumn()
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
				if(str!=null)
					n.getRight().setStrConst("'"+str+"'");
				stringSelectionConds.add(n);
				selectionConds.remove(n);
			}
			else if(isStringSelection(n,0)){
				stringSelectionConds.add(n);
				selectionConds.remove(n);
			}
			else if(n.getLeft().getColumn()!=null && n.getLeft().getColumn().getCvcDatatype().equals("DATE")){
				java.sql.Date d=java.sql.Date.valueOf(n.getRight().getStrConst());
				n.getRight().setStrConst(""+d.getTime()/86400000);
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
		
		for(ConjunctQueryStructure conjunct:conjuncts){
			conjunct.seggregateSelectionConds();
		}
	}
	
	public static boolean isStringSelection(Node n,int flag) {
		if(n.getLeft().getType().equals(Node.getColRefType())/* */){
			if(flag==1  && !n.getRight().getType().equals(Node.getValType()))
				return false;
			
			//TODO:String mutation for parameters
			if(n.getRight() != null && n.getRight().getStrConst() != null &&
					n.getRight().getStrConst().startsWith("$0"))
				return false;
			
			int i=n.getLeft().getColumn().getDataType();
			if(i== Types.VARCHAR || i==Types.CHAR || i==Types.LONGVARCHAR){
				return true;
			}
		}
		
		return false;
	}
}
