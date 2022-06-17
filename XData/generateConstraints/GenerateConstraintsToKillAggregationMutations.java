package generateConstraints;

import java.util.ArrayList;
import java.util.Vector;

import parsing.AggregateFunction;
import parsing.Column;
import parsing.Node;
import testDataGen.GenerateCVC1;
import util.ConstraintObject;

/**
 * This class generate constraints for the distinct and similar values for the tuples,
 * which are useful to kill aggregation mutations
 * @author mahesh
 *
 */


public class GenerateConstraintsToKillAggregationMutations {
	
	/**
	 * Generate constraints for similar values and distinct vales across multiple tuples of the relation
	 * @param cvc
	 * @param af
	 * @param noofGroups
	 * @return
	 */
	public static String getAggConstraints(GenerateCVC1 cvc, AggregateFunction af, int noofGroups) {

		/** The constraints look like 
		 *  The aggregated attribute should be same in two tuples but this value should be different from the third tuple*/
		System.out.println("GENRErate constraints to kill aggregation mutations\n");
		/**Get the column involved in the aggregate function*/
		if(af==null) {
			return "";
		}
		Vector<Column> aggCols = null;//af.getAggExp().getColumnsFromNode();
		String tableNameNo= "";//af.getAggExp().getTableNameNo();

		if(af.getAggExp().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				 Node aggN= getTableNameNoForBAONode(af.getAggExp());
				 tableNameNo = aggN.getTableNameNo();
			 	 aggCols = aggN.getColumnsFromNode();
			}else{
				tableNameNo =af.getAggExp().getTableNameNo();/** If it is an aggregate function*/
				aggCols = af.getAggExp().getColumnsFromNode();
			}
		
		
		/**FIXME: If it is null, it means that aggregation is aliased.....*/
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		
		ArrayList<ConstraintObject> ORConstrList1 = new ArrayList<ConstraintObject>();
		ArrayList<ConstraintObject> ORconstrList2 = new ArrayList<ConstraintObject>();
		
		int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
		int count = cvc.getNoOfTuples().get(tableNameNo);


		String constraintString = "";
		constraintString += ConstraintGenerator.addCommentLine("DISTINCT TUPLES FOR KILLING AGGREGATION ");

		/**Generate the constraints that the aggregated attribute should be differ in atleast two tuples */
		//constraintString += "\nASSERT ";
		ConstraintObject constrORObj1 = new ConstraintObject();
		ConstraintObject constrORObj2 = new ConstraintObject();
		/** This constraint has to be generated in each group of this query block*/
		for(int i=1; i<=noofGroups; i++){
			//constraintString += "(";
			for(int j=1; j<=count;j++){
				for(int k=0;k<aggCols.size();k++){
					ConstraintObject constrObj = new ConstraintObject();
					Column col = aggCols.get(k);
					
					int pos1 = col.getTable().getColumnIndex(col.getColumnName());
					int pos2 = col.getTable().getColumnIndex(col.getColumnName());
					
					if(count == 1){/**if only a single tuple in the relation*/	
						constraintString += constrGen.getAssertTrue();
						continue;
					}

					if (noofGroups == 1 && j!= count){
						//constraintString += " DISTINCT ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset)+"") +") ";
						constrObj.setLeftConstraint(constrGen.getDistinctConstraint(col.getTableName(), col, (j+offset-1),pos1, col.getTableName(), col, (j+offset), pos2));
						constrList.add(constrObj);
					}
					else if (noofGroups == 1 && j == count){
						//constraintString += " DISTINCT ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,(offset)+"") +") ";
						constrObj.setLeftConstraint(constrGen.getDistinctConstraint(col.getTableName(), col, (j+offset-1),pos1, col.getTableName(), col, (j+offset), pos2));
						constrList.add(constrObj);
					}
					else if( i != noofGroups){
						//constraintString += " DISTINCT ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,((i)*count+j+offset-1)+"") +") ";
						constrObj.setLeftConstraint(constrGen.getDistinctConstraint(col.getTableName(), col,((i-1)*count+j+offset-1),pos1, col.getTableName(), col,((i)*count+j+offset-1), pos2));
						constrList.add(constrObj);
					}
					else{
					//	constraintString += " DISTINCT ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset-1)+"") +") ";
						constrObj.setLeftConstraint(constrGen.getDistinctConstraint(col.getTableName(), col,((i-1)*count+j+offset-1),pos1, col.getTableName(), col,(j+offset-1), pos2));
						constrList.add(constrObj);
					}
					if(k!=aggCols.size()-1){
						//constraintString += constrGen.generateANDConstraints(constrList);//" AND ";
					}
				}
				constraintString += constrGen.generateANDConstraints(constrList);//" AND ";
				
				}
			//if( j != count){
				//constraintString += " OR ";
				ConstraintObject constrObj = new ConstraintObject();
				constrObj.setLeftConstraint(constraintString);
				constrList.add(constrObj);
			//}
				constrORObj1.setLeftConstraint(constrGen.generateOrConstraints(constrList));
				ORConstrList1.add(constrORObj1);
			//constraintString += " ) OR ";
		}

		//constraintString = constraintString.substring(0, constraintString.lastIndexOf("OR")-1)+";\n";
		
		
		/**Generate the constraints that the aggregated attribute should be same in atleast two tuples */
		constraintString += ConstraintGenerator.addCommentLine("SIMILAR TUPLES FOR KILLING AGGREGATION\n ");
		//constraintString += "\nASSERT ";
	
		constrList = new ArrayList<ConstraintObject>();
		
		/** This constraint has to be generated in each group of this query block*/
		for(int i=1; i<=noofGroups; i++){
			//constraintString += "(";
			for(int j=1; j<=count;j++){
				for(int k=0;k<aggCols.size();k++){
					Column col = aggCols.get(k);

					if(count == 1){/**if only a single tuple in the relation*/	
						constraintString += constrGen.getAssertTrue();
						continue;
					}
					ConstraintObject constrObj = new ConstraintObject();
					int pos1 = col.getTable().getColumnIndex(col.getColumnName());
					int pos2 = col.getTable().getColumnIndex(col.getColumnName());
					
					
					if (noofGroups == 1&& j!= count){
						
						constrObj.setLeftConstraint(constrGen.getSolverMapping(col,j+offset-1+""));
						constrObj.setRightConstraint(constrGen.getSolverMapping(col,(j+offset)+""));
						constrObj.setOperator(" = ");
						constrList.add(constrObj);
						
						//constraintString += " ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") + " = O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset)+"") +") ";
					}
					else if (noofGroups == 1 && j == count){
						//constraintString += " ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") + " = O_"+ GenerateCVCConstraintForNode.cvcMap(col,(offset)+"") +") ";
						constrObj.setLeftConstraint(constrGen.getSolverMapping(col,j+offset-1+""));
						constrObj.setRightConstraint(constrGen.getSolverMapping(col,(offset)+""));
						constrObj.setOperator(" = ");
						constrList.add(constrObj);						
					}
					else if( i != noofGroups){
						//constraintString += " ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +" = O_"+ GenerateCVCConstraintForNode.cvcMap(col,((i)*count+j+offset-1)+"") +") ";
						constrObj.setLeftConstraint(constrGen.getSolverMapping(col,((i-1)*count+j+offset-1)+""));
						constrObj.setRightConstraint(constrGen.getSolverMapping(col,((i)*count+j+offset-1)+""));
						constrObj.setOperator(" = ");
						constrList.add(constrObj);
					}
					else{
						//constraintString += " ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +" = O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset-1)+"") +") ";
						constrObj.setLeftConstraint(constrGen.getSolverMapping(col,((i-1)*count+j+offset-1)+""));
						constrObj.setRightConstraint(constrGen.getSolverMapping(col,(j+offset-1)+""));
						constrObj.setOperator(" = ");
						constrList.add(constrObj);
					}
					if(k!=aggCols.size()-1){
						//constraintString += " AND ";
					}
				}
				constraintString += constrGen.generateANDConstraints(constrList);
				if( j != count){
					//constraintString += " OR ";
				}
			}
			//constraintString += constrGen.generateOrConstraints();
			constrORObj1 = new ConstraintObject();
			constrORObj1.setLeftConstraint(constrGen.generateOrConstraints(constrList));
			ORConstrList1.add(constrORObj1);
			
			constraintString += " ) OR ";
		}

		constraintString = constrGen.generateOrConstraints(ORConstrList1);
		//constraintString = constraintString.substring(0, constraintString.lastIndexOf("OR")-1)+";\n";

		return constraintString;
	}

	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return
	 */
	public static Node getTableNameNoForBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getTableNameNo() != null){
			return n1.getRight();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTableNameNo() != null){
			return n1.getLeft();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableNameNoForBAONode(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableNameNoForBAONode(n1.getRight());
			}
		}
		return null;
		
	}

	
}
