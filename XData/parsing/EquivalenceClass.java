package parsing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Node;

public class EquivalenceClass {
	
	private static Logger logger=Logger.getLogger(EquivalenceClass.class.getName());
	public static Vector<Vector<Vector<Node>>> createEqClass(QueryParser qParser){
		Node temp;
		Vector<Vector<Vector<Node>>> EqClassTemp=new Vector<Vector<Vector<Node>>>();
		HashMap<Integer, Node> hm = new HashMap<Integer, Node>();
		for(Vector<Node> joincond: qParser.dnfJoinCond){
			Vector<Node> joinCondDups= new Vector<Node>();
			joinCondDups.addAll(joincond);
			Vector<Vector<Node>> ecs1 = new Vector<Vector<Node>>();
			Vector<Node> ec1 = new Vector<Node>();
			temp = new Node();
			Node temp3, temp4;
			Vector<Node> tempAllJoinConds1 = (Vector) joinCondDups.clone();

			int count1 = 0;
			int flag1 = 0;
			while (tempAllJoinConds1.size() > 0) {
				temp = tempAllJoinConds1.get(0).getLeft();
				ec1.add(temp);
				temp = tempAllJoinConds1.get(0).getRight();
				ec1.add(temp);
				tempAllJoinConds1.remove(0);
				int cond_iter = 0;
				for (int j = 0; j < ec1.size(); j++) {
					// count = 0;
					for (int k = 0; k < tempAllJoinConds1.size(); k++) {
						temp3 = tempAllJoinConds1.get(k);
						temp4 = ec1.get(j);
						logger.log(Level.FINE,"Comparing " + temp3.toString()
								+ " and " + temp4.toString());
						if (temp3.getLeft() != null) {
							if (temp3.getLeft().getColumn().getColumnName()
									.equalsIgnoreCase(
											temp4.getColumn().getColumnName())
											&& temp3.getLeft().getColumn().getTableName()
											.equalsIgnoreCase(
													temp4.getColumn()
													.getTableName())) {
								
								logger.log(Level.INFO,"Added " + temp3.toString());
								ec1.add(temp3.getLeft());
								ec1.add(temp3.getRight());
								tempAllJoinConds1.remove(k);
								k = 0;
								// count--;
								// flag = 1;
							} else if (temp3.getRight().getColumn().getColumnName()
									.equalsIgnoreCase(
											temp4.getColumn().getColumnName())
											&& temp3.getRight().getColumn().getTableName()
											.equalsIgnoreCase(
													temp4.getColumn()
													.getTableName())) {

								logger.log(Level.INFO,"Added " + temp3.toString());
								
								ec1.add(temp3.getLeft());
								ec1.add(temp3.getRight());
								tempAllJoinConds1.remove(k);
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
				ecs1.add(new Vector<Node>(ec1));
				logger.log(Level.INFO,"Added EC: " + ec1.toString());
			
				ec1.removeAllElements();
			}
			EqClassTemp.add(ecs1);
		}
		return EqClassTemp;
	}
	
	/*
	 * Mahesh added
	 * A wrapper that is used to create equivalence classes for outer query block and all from clause and outer clause sub queries
	 */
	public static void makeEquivalenceClasses(parsing.QueryParser qParser) {

		qParser.equivalenceClasses.addAll(createEquivalenceClasses(qParser));//for outer query block
		for(QueryParser qp: qParser.getFromClauseSubqueries()){//For From clause subqueries
			qp.equivalenceClasses.addAll(createEquivalenceClasses(qp));
		}

		for(QueryParser qp: qParser.getWhereClauseSubqueries()){
			qp.equivalenceClasses.addAll(createEquivalenceClasses(qp));
		}
	}
	
	/*
	 * New Function for Equivalence Classes. Deriving equivalence classes from
	 * the vector of Node class
	 */

	public static Vector<Vector<Node>> createEquivalenceClasses(QueryParser qParser) {
		Node temp;
		HashMap<Integer, Node> hm = new HashMap<Integer, Node>();
		// First flatten all the join/selection condition trees
		// And select the join conditions in a special vector joinConds

		QueryParser.flattenAndSeparateAllConds(qParser);

		// Now create equivalence classes from joinConds

		Vector<Node> allJoinConds = new Vector<Node>();
		allJoinConds.addAll(qParser.joinConds);
		// allJoinConds.addAll(subQJC);

		Vector<Vector<Node>> ecs = new Vector<Vector<Node>>();
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

					String col1, col2, tab1, tab2, col3, tab3;
					//FIXME: If aggregate nodes then?
					if (temp1.getLeft() != null) {

						if(temp1.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())){							
							col1 = temp1.getLeft().getAgg().getAggExp().getColumn().getColumnName();
							tab1 = temp1.getLeft().getAgg().getAggExp().getColumn().getTableName();
						}
						else{
							col1 = temp1.getLeft().getColumn().getColumnName();
							tab1 = temp1.getLeft().getColumn().getTableName();
						}


						if(temp2.getType().equalsIgnoreCase(Node.getAggrNodeType())){							
							col2 = temp2.getAgg().getAggExp().getColumn().getColumnName();
							tab2 = temp2.getAgg().getAggExp().getColumn().getTableName();
						}
						else{
							col2 = temp2.getColumn().getColumnName();
							if(temp2.getLeft() != null)
								tab2 = temp2.getLeft().getColumn().getTableName();
							else
								tab2 = temp2.getTable().getTableName();//Added by mahesh
						}


						if(temp1.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())){							
							col3 = temp1.getRight().getAgg().getAggExp().getColumn().getColumnName();
							tab3 = temp1.getRight().getAgg().getAggExp().getColumn().getTableName();
						}
						else{
							col3 = temp1.getRight().getColumn().getColumnName();
							tab3 = temp1.getRight().getColumn().getTableName();
						}

						/*if (temp1.getLeft().getColumn().getColumnName()
								.equalsIgnoreCase(
										temp2.getColumn().getColumnName())
								&& temp1.getLeft().getColumn().getTableName()
										.equalsIgnoreCase(
												temp2.getColumn()
														.getTableName())) {*/
						if(col1.equalsIgnoreCase(col2) && tab1.equalsIgnoreCase(tab2)){
							logger.log(Level.INFO,"Added " + temp1.toString());
							ec.add(temp1.getLeft());
							ec.add(temp1.getRight());
							tempAllJoinConds.remove(k);
							k = 0;
							// count--;
							// flag = 1;
						} /*else if (temp1.getRight().getColumn().getColumnName()
								.equalsIgnoreCase(
										temp2.getColumn().getColumnName())
								&& temp1.getRight().getColumn().getTableName()
										.equalsIgnoreCase(
												temp2.getColumn()
														.getTableName())) {*/
						else if(col3.equalsIgnoreCase(col2) && tab3.equalsIgnoreCase(tab2)){
						
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
			ecs.add(new Vector<Node>(ec));
			logger.log(Level.INFO,"Added EC: " + ec.toString());
			ec.removeAllElements();
		}
		removeDuplicates(ecs);
		for (int l = 0; l < qParser.subQJC.size(); l++) {
			String ltabName = "";
			if (qParser.subQJC.get(l).getLeft() != null) {
				if (qParser.subQJC.get(l).getLeft().getTable() != null) {
					ltabName = qParser.subQJC.get(l).getLeft().getTable()
							.getTableName();
				} else {
					ltabName = qParser.subQJC.get(l).getLeft().getTableNameNo();
					ltabName = Util.chop(ltabName);
				}
				String lcolName = qParser.subQJC.get(l).getLeft().getColumn()
						.getColumnName();

				String rtabName = "";
				if (qParser.subQJC.get(l).getRight().getTable() != null) {
					rtabName = qParser.subQJC.get(l).getRight().getTable()
							.getTableName();

				} else {
					rtabName = qParser.subQJC.get(l).getRight().getTableNameNo();
					rtabName = Util.chop(rtabName);
				}
				String rcolName = qParser.subQJC.get(l).getRight().getColumn()
						.getColumnName();

				for (int i = 0; i < ecs.size(); i++) {
					Vector<Node> ecTemp = ecs.get(i);
					for (int k = 0; k < ecTemp.size(); k++) {
						String tabName = ecTemp.get(k).getTable()
								.getTableName();
						String colName = ecTemp.get(k).getColumn()
								.getColumnName();
						if (ltabName.equals(tabName)) {
							if (lcolName.equals(colName)) {
								// ecs.get(i).add(subQJC.get(l).getRight());
								hm.put(i, qParser.subQJC.get(l).getRight());
							}

						} else if (rtabName.equals(tabName)) {
							if (rcolName.equals(colName)) {
								// ecs.get(i).add(subQJC.get(l).getLeft());
								hm.put(i, qParser.subQJC.get(l).getLeft());
							}

						}
					}
				}
			}
			Iterator it = hm.keySet().iterator();
			for (int i = 0; i < ecs.size(); i++) {
				if (hm.get(i) != null) {
					ecs.get(i).add(hm.get(i));
				}
			}
		}

		logger.log(Level.INFO,"Final Equivalence Classes:\n"+ecs.toString());
		return ecs;
	}
	
	public static void removeDuplicates(Vector<Vector<Node>> ecs) {
		for (int i = 0; i < ecs.size(); i++) {
			Vector<Node> ec = ecs.get(i);
			for (int j = 0; j < ec.size(); j++) {
				for (int k = j + 1; k < ec.size(); k++) {//FIXME: If aggregate node

					/*Node n = new Node();
					Node n1 = new Node();
					n = ec.get(j);
					n1 = ec.get(k);
					String col1, col2, tab1, tab2, name1, name2;
					if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){							
						col1 = n.getAgg().getAggExp().getColumn().getColumnName();
						tab1 = n.getAgg().getAggExp().getColumn().getTableName();
						name1 = n.getAgg().getAggExp().getTableNameNo();
					}
					else{
						col1 = n.getColumn().getColumnName();
						tab1 = n.getColumn().getTableName();
						name1 = n.getTableAlias();
					}

					if(n1.getType().equalsIgnoreCase(Node.getAggrNodeType())){							
						col2 = n1.getAgg().getAggExp().getColumn().getColumnName();
						tab2 = n1.getAgg().getAggExp().getColumn().getTableName();
					}
					else{
						col2 = n1.getColumn().getColumnName();
						tab2 = n1.getColumn().getTableName();
						name2 = n1.getTableAlias();
					}*/

					if (ec.get(j).getTable() == ec.get(k).getTable()
							&& ec.get(j).getColumn() == ec.get(k).getColumn()
							&& ec.get(j).getTableAlias().equalsIgnoreCase(
									ec.get(k).getTableAlias())) 
					/*if(col1.equalsIgnoreCase(col2) && tab1.equalsIgnoreCase(tab2) && name1.equalsIgnoreCase(name2)) */{
						ec.removeElementAt(j);
						j = -1;
						break;
					}
				}
			}
		}

		// Remove duplicate ECs
		// Worst programming.
		// TODO: Just for the time being. Later fix create Equivalence classes
		// not to create multiple ECs with same elements
		// int flag = 0;
		// for(int i=0;i<ecs.size();i++){
		// Vector<Node> ec = ecs.get(i);
		// for(int j=0;j<ec.size();j++){
		// for(int k=i+1;k<ecs.size();k++){
		// Vector<Node> ec2 = ecs.get(k);
		// for(int l=0;l<ec2.size();l++){
		// if(ec.get(j).getColumn().getColumnName().equalsIgnoreCase(ec2.get(l).getColumn().getColumnName())
		// &&
		// ec.get(j).getColumn().getTableName().equalsIgnoreCase(ec2.get(l).getColumn().getTableName())){
		// ecs.remove(k);
		// flag=1;
		// break;
		// }
		// }
		// if(flag==1){
		// flag=0;
		// i=1;
		// break;
		// }
		// }
		// }
		// }
	}
	
	/*
	 * Commented because its useless now.
	 * 
	 * public Vector<Node> addIfAlreadyNotExists(Vector<Node> ec1, Vector<Node>
	 * ec2){ Vector<Node> v = new Vector<Node>(); v.addAll(ec1); for(int
	 * i=0;i<ec2.size();i++){ if(alreadyNotExistInEquivalenceClass(v,
	 * ec2.get(i))){ v.add(ec2.get(i)); } } return v; }
	 */
	
	
	
}
