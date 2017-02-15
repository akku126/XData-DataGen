package parsing;

import java.util.Vector;
import java.util.logging.Logger;

import parsing.Node;

public class EquivalenceClass {
	
	private static Logger logger=Logger.getLogger(EquivalenceClass.class.getName());
	

	
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
