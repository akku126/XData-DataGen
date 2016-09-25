package generateConstraints;

import java.util.Vector;

import parsing.AggregateFunction;
import parsing.Column;
import parsing.Node;
import testDataGen.GenerateCVC1;

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

		int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
		int count = cvc.getNoOfTuples().get(tableNameNo);


		String constraintString = "";
		constraintString += "\n %DISTINCT TUPLES FOR KILLING AGGREGATION\n ";

		/**Generate the constraints that the aggregated attribute should be differ in atleast two tuples */
		constraintString += "\nASSERT ";

		/** This constraint has to be generated in each group of this query block*/
		for(int i=1; i<=noofGroups; i++){
			constraintString += "(";
			for(int j=1; j<=count;j++){
				for(int k=0;k<aggCols.size();k++){
					Column col = aggCols.get(k);

					if(count == 1){/**if only a single tuple in the relation*/	
						constraintString += "TRUE";
						continue;
					}

					if (noofGroups == 1 && j!= count)
						constraintString += " DISTINCT ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset)+"") +") ";
					else if (noofGroups == 1 && j == count)
						constraintString += " DISTINCT ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,(offset)+"") +") ";
					else if( i != noofGroups)
						constraintString += " DISTINCT ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,((i)*count+j+offset-1)+"") +") ";
					else
						constraintString += " DISTINCT ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +", O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset-1)+"") +") ";
					if(k!=aggCols.size()-1){
						constraintString += " AND ";
					}
				}
				if( j != count)
					constraintString += " OR ";
			}
			constraintString += " ) OR ";
		}

		constraintString = constraintString.substring(0, constraintString.lastIndexOf("OR")-1)+";\n";
		
		
		/**Generate the constraints that the aggregated attribute should be same in atleast two tuples */
		constraintString += "\n %SIMILAR TUPLES FOR KILLING AGGREGATION\n ";
		constraintString += "\nASSERT ";

		/** This constraint has to be generated in each group of this query block*/
		for(int i=1; i<=noofGroups; i++){
			constraintString += "(";
			for(int j=1; j<=count;j++){
				for(int k=0;k<aggCols.size();k++){
					Column col = aggCols.get(k);

					if(count == 1){/**if only a single tuple in the relation*/	
						constraintString += "TRUE";
						continue;
					}

					if (noofGroups == 1&& j!= count)
						constraintString += " ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") + " = O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset)+"") +") ";
					else if (noofGroups == 1 && j == count)
						constraintString += " ( O_"+ GenerateCVCConstraintForNode.cvcMap(col,j+offset-1+"") + " = O_"+ GenerateCVCConstraintForNode.cvcMap(col,(offset)+"") +") ";
					else if( i != noofGroups)
						constraintString += " ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +" = O_"+ GenerateCVCConstraintForNode.cvcMap(col,((i)*count+j+offset-1)+"") +") ";
					else
						constraintString += " ( O_" + GenerateCVCConstraintForNode.cvcMap(col,((i-1)*count+j+offset-1)+"") +" = O_"+ GenerateCVCConstraintForNode.cvcMap(col,(j+offset-1)+"") +") ";
					if(k!=aggCols.size()-1){
						constraintString += " AND ";
					}
				}
				if( j != count)
					constraintString += " OR ";
			}
			constraintString += " ) OR ";
		}

		constraintString = constraintString.substring(0, constraintString.lastIndexOf("OR")-1)+";\n";

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
