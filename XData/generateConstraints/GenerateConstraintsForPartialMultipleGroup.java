package generateConstraints;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * This class generates constraints for killing partial group by mutations, by generating two groups of data
 * This generates constraints considering multile groups 
 * @author mahesh
 *
 */
public class GenerateConstraintsForPartialMultipleGroup {
	
	private static Logger logger = Logger.getLogger(GenerateConstraintsForPartialMultipleGroup.class.getName());

	/**
	 * Generates constraints for killing partial group by mutation
	 * @param cvc
	 * @param queryBlock
	 * @param groupbyNode
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForPartialMultipleGroup(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node groupByNode) throws Exception{
		
		try{
		/** get the equivalence classes in which this group by node is present */
		ArrayList<ArrayList<Node>> eqClassForGroupByNode = new ArrayList<ArrayList<Node>>();
		
		/** check in each conjunct of this query block*/
		/**FIXME: what if this group node is present in the equivalence class of the query blocks in which this 'queryBlock' is nested (Eg: from clause nested block and outer block*/
		for(ConjunctQueryStructure con: queryBlock.getConjunctsQs())
			for(Vector<Node> ec: con.getEquivalenceClasses())
				if(ec.contains(groupByNode))
					eqClassForGroupByNode.add( new ArrayList<Node>(ec));
		
		/** Stores constraints for the group by nodes to have same value across multiple groups*/
		String multiGroupSameValue = "";
		ConstraintGenerator constrGen = new ConstraintGenerator();
		multiGroupSameValue += ConstraintGenerator.addCommentLine("ALL OTHER GROUP BY ATTRIBUTES MUST BE SAME ACROSS MULTIPLE GROUPS");
		
		/** Add constraints such that all group by nodes of this query block except the given groupByNode, are equal across multiple groups  */
		for(Node groupbyNode: queryBlock.getGroupByNodes()){
			
			if( groupbyNode.equals(groupByNode)) continue; /**This groupByNode should contain different values across multiple groups */
			
			/** A flag to indicate if this groupbyNOde is present in the equivalence class of the input groupByNode */
			boolean present = false;
			
			/** If this group by node is involved in joins with other group by attribute then they both must contain same value*/
			for(ArrayList<Node> ec: eqClassForGroupByNode)
				if( ec.contains(groupbyNode)){
					present = true;
					break;
				}
			if( present ) continue;/** If present then no need generate constraint, as for given group by node we will generate constraint*/
			
			/** Get the table tables for groupbyNode */			
			Column g = groupbyNode.getColumn();
			String t = g.getTableName();
			String tableNameNo = groupbyNode.getTableNameNo();
			
			/** Get tuple details */
			int noOfTuples = 0;//cvc.getNoOfTuples().get(tableNameNo);
			if(cvc.getNoOfTuples().containsKey(tableNameNo)){
				noOfTuples = cvc.getNoOfTuples().get(tableNameNo);
			}else{
				noOfTuples = 2;
			}
			
			int offset = 0;//cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
			
			if(cvc.getRepeatedRelNextTuplePos().containsKey(tableNameNo)){
				offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
			}else{
				offset = 1;
			}
			
			//int Index = cvc.getTableMap().getTable(t).getColumnIndex(g.getColumnName());
			int Index = cvc.getTableMap().getTable(t.toUpperCase()).getColumnIndex(g.getColumnName()); //added by rambabu
			
			/**Generate constraints for each group */
			for(int i=1; i <= queryBlock.getNoOfGroups(); i++){				
						
				if( queryBlock.getNoOfGroups() != 1 && i != queryBlock.getNoOfGroups())/**If this group is not the last group */{
					multiGroupSameValue +=  constrGen.getAssertConstraint(t,g, ((i-1)*noOfTuples + 1 + offset -1),Index, t, g,((i)*noOfTuples + 1+ offset -1),Index, " = ");
					//"ASSERT O_"+t+"["+((i-1)*noOfTuples + 1 + offset -1)+"]."+Index+ " = O_"+t+"["+((i)*noOfTuples + 1+ offset -1)+"]."+Index+";\n";
				}
				else if( queryBlock.getNoOfGroups() > 1) /** Iff multiple groups are present */{
					multiGroupSameValue += constrGen.getAssertConstraint(t,g, (i*noOfTuples+ offset -1),Index, t,g, (1  + offset -1),Index, " = ");
					//"ASSERT O_"+t+"["+(i*noOfTuples+ offset -1)+"]."+Index+ " = O_"+t+"["+(1  + offset -1)+"]."+Index+";\n";
				}
			}
		}
		
		
		multiGroupSameValue += ConstraintGenerator.addCommentLine("END OF ALL OTHER GROUP BY ATTRIBUTES MUST BE SAME ACROSS MULTIPLE GROUPS ");

		
		/** To store constraints for this group by Node to have different value across multiple groups */
		String multiGroupDiffValue = "";
		
		multiGroupDiffValue += ConstraintGenerator.addCommentLine("KILLING GROUP BY ATTRIBUTES MUST BE DIFF ACROSS MULTIPLE GROUPS ");

		/** Get the table tables for groupByNode */	
		Column g = groupByNode.getColumn();
		String t = g.getTableName();
		String tableNameNo = groupByNode.getTableNameNo();
		
		/** Get tuple detals */
		int count = 0;//cvc.getNoOfTuples().get(tableNameNo);
		if(cvc.getNoOfTuples().containsKey(tableNameNo)){
			count = cvc.getNoOfTuples().get(tableNameNo);
		}else{
			count = 2;
		}
		int offset = 0;//cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
		if(cvc.getRepeatedRelNextTuplePos().containsKey(tableNameNo)){
			offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
		}else{
			offset  = 1;
		}
		//int Index = cvc.getTableMap().getTable(t).getColumnIndex(g.getColumnName());
		int Index = cvc.getTableMap().getTable(t.toUpperCase()).getColumnIndex(g.getColumnName()); //added by rambabu
		
		/**Generate constraints for each group */
		for(int i=1; i <= queryBlock.getNoOfGroups(); i++){	
			
			if( queryBlock.getNoOfGroups() != 1 && i != queryBlock.getNoOfGroups())/**If this group is not the last group */{
				multiGroupDiffValue += constrGen.getAssertDistinctConstraint(t,g,((i-1)*count + 1 + offset -1),Index,t,g,((i)*count + 1+ offset -1),Index);
				//"ASSERT  DISTINCT("+ "O_"+t+"["+((i-1)*count + 1 + offset -1)+"]."+Index+ " ,  O_"+t+"["+((i)*count + 1+ offset -1)+"]."+Index+");\n";
			}
			else if( queryBlock.getNoOfGroups() > 1) /** Iff multiple groups are present */{
				
				multiGroupDiffValue += constrGen.getAssertDistinctConstraint(t,g,(i*count+ offset -1),Index,t,g,(1  + offset -1),Index);
				//"ASSERT DISTINCT( O_"+t+"["+(i*count+ offset -1)+"]."+Index+ ",  O_"+t+"["+(1  + offset -1)+"]."+Index+");\n";
			}
		}
		 
			multiGroupDiffValue += ConstraintGenerator.addCommentLine("END OF KILLING GROUP BY ATTRIBUTES MUST BE DIFF ACROSS MULTIPLE GROUPS ");
		
		
		return multiGroupSameValue + "\n" + multiGroupDiffValue;
		
		
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			e.printStackTrace();
			throw e;
		}
		
	
	}
	
}
