package generateConstraints;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import parsing.Column;
import parsing.ForeignKey;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;

/**
 * This class generates constraints for killing partial group by mutations in a single group
 * This generates constraints considering multiple groups 
 * @author mahesh
 *
 */
public class GenerateConstraintsForPartialGroup_case2 {


	/**
	 * Generates constraints for killing partial group by mutation
	 * @param cvc
	 * @param queryBlock
	 * @param groupByNode
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForPartialSingleGroup(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node groupByNode) throws Exception {


		/** This group by attribute must be distinct across multiple groups */
		String diffValue = "";
		ConstraintGenerator constrGen = new ConstraintGenerator();
		diffValue += ConstraintGenerator.addCommentLine("KILLING GROUP BY ATTRIBUTE IS DIFF ACROSS MULTIPLE GROUPS");
		ArrayList< ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		
		for(int i=1; i<= queryBlock.getNoOfGroups(); i++){
			if ( queryBlock.getNoOfGroups() == 1 ) 
				continue;
			//diffValue += "ASSERT ";

			/** Get the table for groupbyNode */			
			Column g = groupByNode.getColumn();
			String t = g.getTableName();
			String tableNameNo = groupByNode.getTableNameNo();

			/** Get tuple detals */
			int noOfTuples = cvc.getNoOfTuples().get(tableNameNo);
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
			int Index = cvc.getTableMap().getTable(t).getColumnIndex(g.getColumnName());			


			if( i != queryBlock.getNoOfGroups())/**If this group is not the last group */{
				//diffValue +=  " DISTINCT ( O_"+t+"["+((i-1)*noOfTuples + 1 + offset -1)+"]."+Index+ ",  O_"+t+"["+((i)*noOfTuples + 1+ offset -1)+"]."+Index+") OR ";
				ConstraintObject constrObj = new ConstraintObject();
				constrObj.setLeftConstraint(constrGen.getDistinctConstraint(t, g, ((i-1)*noOfTuples + 1 + offset -1), Index, t, g, ((i)*noOfTuples + 1+ offset -1), Index));
				constrList.add(constrObj);
				
			}
			else {
				//diffValue += " DISTINCT ( O_"+t+"["+(i*noOfTuples+ offset -1)+"]."+Index+ ",  O_"+t+"["+(1  + offset -1)+"]."+Index+") OR ";
				ConstraintObject constrObj = new ConstraintObject();
				constrObj.setLeftConstraint(constrGen.getDistinctConstraint(t, g, (i*noOfTuples+ offset -1), Index, t, g, (1  + offset -1), Index));
				constrList.add(constrObj);
			}

			diffValue += constrGen.generateOrConstraintsWithAssert(constrList)+"\n";
			//int lastIndex = diffValue.lastIndexOf("OR");
			//diffValue = diffValue.substring(0, lastIndex-1) + " ;\n ";					
		}	

		/** This group by node must be distinct in atleast two tuples in the same group*/
		diffValue += ConstraintGenerator.addCommentLine("KILLING GROUP BY ATTRIBUTES MUST BE DIFF IN SAME GROUP");

		/** add constraints for the tuples to be distinct within group*/
		diffValue += getGroupByConstraintsForDiffValue(cvc, queryBlock, groupByNode);

		diffValue += ConstraintGenerator.addCommentLine("END OF KILLING GROUP BY ATTRIBUTES MUST BE DIFF IN SAME GROUP");

		return diffValue;
	}




	/**
	 * Gets the constraints for the group by attribute to be distinct in at least one tuple in the same group
	 * @return
	 * @throws Exception
	 */
	public static String getGroupByConstraintsForDiffValue(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node groupByNode) throws Exception{

		if(groupByNode == null)
			return "";
		ConstraintGenerator constrGen = new ConstraintGenerator();
		String distinct = "";

		ArrayList< ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		for(int i=1; i <= queryBlock.getNoOfGroups(); i++){/** we have to generate these constraints for each group */

			//distinct += "ASSERT ";

			/** Get the table for groupbyNode */
			Column g = groupByNode.getColumn();
			String t = g.getTableName();
			String tableNameNo=groupByNode.getTableNameNo();

			/** Get tuple details */
			int count = cvc.getNoOfTuples().get(tableNameNo);
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
			int Index = cvc.getTableMap().getTable(t).getColumnIndex(g.getColumnName());


			int group = (i-1)*count;/**get group number*/

			for(int k=1; k<=count;k++){
				for(int l=k+1; l<=count;l++)/** Ensure that this attribute differs in atleast one two tuples*/{
					
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(constrGen.getDistinctConstraint(t, g, (group+k-1+offset), Index, t, g, (group+l-1+offset), Index));
					constrList.add(constrObj);
					//distinct += " DISTINCT (O_"+t+"["+(group+k-1+offset)+"]."+Index+ " , O_"+t+"["+(group+l-1+offset)+"]."+Index+") OR ";
				}
			}
			distinct += constrGen.generateOrConstraintsWithAssert(constrList);
			
			if( constrList == null || (constrList!= null && constrList.isEmpty()))
				distinct += constrGen.getAssertTrue();
			else{
				//int lastIndex = distinct.lastIndexOf("OR");
				distinct = distinct +  "\n ";
			}
		}

		return distinct;
	}

	/**
	 * If node is involved in foreign key relationship with outer/or other sub query node 
	 * and we are killing group by mutations using case2 then we should adjust the number of tuples in fk table 
	 * @param node
	 * @param equivalence
	 */
	//FIXME: Handle multi attribute foreinkeys(All/some of the attributes may/may not be in equivalence class). We should not unncessarily increase the number of tuples
	public static String adjustNoOfTuplesForiegnKeyTables(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node node, Vector<Vector<Node>> equivalence) {

		String out ="";
		ConstraintGenerator constrGen = new ConstraintGenerator();
		//check if the node is outside of this query block
		for(Vector<Node> ec: equivalence){
			for(Node n: ec){
				if(n.getQueryType() == node.getQueryType() && n.getQueryIndex() == node.getQueryIndex())//If these nodes are from same block then nothing need to be done
					continue;
				boolean flag = false;
				//If this not a foreign key relation ship 
				Vector<Column> primaryKeys = n.getTable().getPrimaryKey();
				Map<String,ForeignKey> fkeys = node.getTable().getForeignKeys();
				Iterator it = fkeys.entrySet().iterator();

				while (it.hasNext() && flag== false) {
					Map.Entry pairs = (Map.Entry)it.next();
					ForeignKey fk= (ForeignKey)pairs.getValue();
					boolean flag1 = false;
					for(Column col: fk.getFKeyColumns()){//check if this node forms a foreign key
						if(col.getColumnName().equalsIgnoreCase(node.getColumn().getColumnName())){
							if(col.getReferenceTableName().equalsIgnoreCase(n.getTable().getTableName())){//If it refernced key is same as 'n' table
								if(col.getReferenceColumn().getColumnName().equalsIgnoreCase(n.getColumn().getColumnName())){
									flag = true;
									break;
								}
							}
						}
					}
				}
				if(!flag)
					continue;
				//get the number of tuples of this node
				int noofTuples1 = cvc.getNoOfTuples().get(node.getTableNameNo());
				int nofGroups1 = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, node);

				//get the number of tuples and number of groups for the equivalence class node
				int noofTuples2 = cvc.getNoOfTuples().get(n.getTableNameNo());
				int nofGroups2 = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n);

				//node is distinct in atleast one tuple in the same group 
				//So we should increase the number of tuples in each group
				for(int i=1;i<=nofGroups1;i++){
					for(int j=2; j<=noofTuples1;j++){


						//get the table name and column name for this 'node' 
						Column fColumn = node.getColumn();
						String fTable = fColumn.getTableName();

						String fTableNameNo = node.getTableNameNo();
						int fOffset = cvc.getRepeatedRelNextTuplePos().get(fTableNameNo)[1];
						int fIndex = cvc.getTableMap().getTable(fTable).getColumnIndex(fColumn.getColumnName());

						//get primary key relation details
						Column pColumn = n.getColumn();
						String pTable = pColumn.getTableName();

						String pTableNameNo = n.getTableNameNo();
						int pOffset = cvc.getRepeatedRelNextTuplePos().get(pTableNameNo)[1];
						int pIndex = cvc.getTableMap().getTable(pTable).getColumnIndex(pColumn.getColumnName());

						//get the offset for the foreign key table
						int ptupleOffset = noofTuples2*nofGroups2;

						//Increase the number of tuples of foreign key table
						if(cvc.getNoOfOutputTuples().get(pTable) == null)
							cvc.getNoOfOutputTuples().put(pTable, 1);
						else
							cvc.getNoOfOutputTuples().put(pTable, cvc.getNoOfOutputTuples().get(pTable) + 1);

						//generate the constraint
						out += constrGen.getAssertConstraint(fTable,fColumn, (i*j+ fOffset -1),fIndex, pTable,pColumn, (ptupleOffset + 1), pIndex, " = ");
								//"ASSERT O_"+fTable+"["+(i*j+ fOffset -1)+"]."+fIndex+ " = O_"+pTable+"["+(ptupleOffset + 1)+"]."+pIndex+";\n";
					}
				}
			}
		}
		return out;
	}


}
