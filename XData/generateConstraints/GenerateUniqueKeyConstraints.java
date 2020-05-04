package generateConstraints;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import parsing.Column;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;

/**
 * This class contain methods for generating constraints for the unique keys across all the groups of the query block
 * @author mahesh
 *
 */
public class GenerateUniqueKeyConstraints {

	
	/**
	 * Generates constraints so that the values for the given set of attributes to be distinct across all tuples generated  
	 * @param cvc
	 * @param qb
	 * @param uniqueElements
	 * @return
	 */

	public static String generateUniqueConstraints(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Set< HashSet<Node>> uniqueElements){

		if(uniqueElements == null || uniqueElements.size() == 0)
			return "";

		String uniqueConstraint = "";
		String distinct = "";
		
		/** Get number of groups */
		int noOfGroups = queryBlock.getNoOfGroups();
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		/** or each unique element */
		for(HashSet<Node> unique: uniqueElements){
			
			/**Here each unique element is from the same relation 
			 * FIXME: Is this valid??*/
			Iterator<Node> it = unique.iterator(); 

			/** get a node from this unique element */
			Node n = it.next();
			noOfGroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n);
			String tableNameNo = n.getTableNameNo();

			
			int count = cvc.getNoOfTuples().get(tableNameNo);
			if( count*noOfGroups == 1)
				continue;
			
			for(int k=1; k<=count*noOfGroups; k++){
				for(int j=k+1; j<=count*noOfGroups; j++){

					//distinct += "ASSERT ";


					for(Node u: unique){
						ConstraintObject constObj = new ConstraintObject();
						Column g = u.getColumn();
						String t = g.getTableName();
						int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
						//int Index = cvc.getTableMap().getTable(t).getColumnIndex(g.getColumnName());
						int Index = cvc.getTableMap().getTable(t.toUpperCase()).getColumnIndex(g.getColumnName()); //added by rambabu
						

						constObj.setLeftConstraint(constraintGen.getDistinctConstraint(t, u.getColumn(), (k + offset -1), Index, t, u.getColumn(), (j +  offset -1), Index));
						constrList.add(constObj);
						
						//distinct += " DISTINCT ( O_"+t+"["+(k + offset -1)+"]."+Index+" , O_"+t+"["+ (j +  offset -1) +"]."+Index + ") OR ";
					}

					//int lastIndex = distinct.lastIndexOf("OR");
					//distinct = distinct.substring(0, lastIndex-1) + " ;\n ";
					distinct = constraintGen.generateOrConstraintsWithAssert(constrList);
				}
			}
		}
		return distinct;
	}
}
