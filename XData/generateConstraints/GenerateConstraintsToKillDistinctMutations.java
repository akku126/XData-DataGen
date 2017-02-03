package generateConstraints;

import java.util.ArrayList;
import java.util.Vector;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * Generates constraints to kill distinct mutations of the projected columns inside the given query block
 * @author mahesh
 *
 */
public class GenerateConstraintsToKillDistinctMutations {

	/**
	 * Get constraints for similar values of the projected columns of the given query block
	 * @param cvc
	 * @param qbt
	 * @return
	 */
	public static String getDistinctConstraints(GenerateCVC1 cvc, QueryBlockDetails queryBlock) {

		String constraintString = "";

		/** Get the projected columns of this query block */
		ArrayList<Node> projectedCols = queryBlock.getProjectedCols();

		/**For each projected column*/
		for(int i=0; i<projectedCols.size(); i++){

			/**Get the node in the list*/
			Node n = projectedCols.get(i);

			/**If this node is of column type*/
			if(n!= null && n.getType() != null 
					&& n.getType().equalsIgnoreCase(Node.getColRefType())){

				/**Get node details */
				Column c = n.getColumn();
				String tableNameNo = n.getTableNameNo();

				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];

				boolean subQuery = false;
				int noofGroups = -1;

				/** check if this node is of From clause sub query */
				for(QueryBlockDetails qbt: queryBlock.getFromClauseSubQueries())
					if(qbt.getProjectedCols().contains(n)){
						/**If this is present inside the from clause nested subquery blocks of this query block */
						noofGroups = qbt.getNoOfGroups();
						break;
					}

				/**GEt the number of tuples of this relation */		
				int count = cvc.getNoOfTuples().get(tableNameNo);

				int nextPos;/** To indicate the next position of the second tuple of this relation, if any */

				if(noofGroups > -1)
					nextPos = count;/** The second tuple corresponds to the next group tuple*/
				else
					nextPos = 1;


				if(count>1){/** If more than one tuple in this relation*/						
					constraintString += "\nASSERT O_"+ GenerateCVCConstraintForNode.cvcMap(c,(offset)+"") + " = O_" + GenerateCVCConstraintForNode.cvcMap(c,(offset+nextPos)+"") + ";";
				}
				else{/** If there is a single tuple in this relation, this means that this node is involved in 
				equi joins with other relation nodes*/

					/**Get the column name which is involved in equi joins with this node*/
					Column c1 = getNonUniqueMemberInEquivalenceClassOf(cvc, queryBlock, c);
					if(c1 != null)
						constraintString += "\nASSERT O_"+ GenerateCVCConstraintForNode.cvcMap(c1,(offset)+"") + " = O_" + GenerateCVCConstraintForNode.cvcMap(c1,(offset+nextPos)+"") + ";";
				}
			}
		}
		return constraintString;
	}


	/**
	 *  Get other column in the equivalence class of the input column
	 *  If more than one column is present in this equivalence class, it returns the column with atleast two tuples 
	 *  and which is not unique	  
	 * @param cvc
	 * @param queryBlock
	 * @param col
	 * @return
	 */
	public static Column getNonUniqueMemberInEquivalenceClassOf(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Column col){
		
		/**check equivalence classes in each conjunct of this query block */
		for(ConjunctQueryStructure con: queryBlock.getConjunctsQs()){
			
			/**Check each equivalence class of this conjunct*/
			Vector<Vector<Node>> equivalenceClasses = con.getEquivalenceClasses();
			
			for(int i=0; i < equivalenceClasses.size(); i++){
				/** Get one equivalence class*/
				Vector<Node> ec = equivalenceClasses.get(i);
				
				/**Check this equivalence class*/
				for(int j=0; j < ec.size(); j++){
					if(ec.get(j).getColumn() == col){/**If input column is present in this equivalence class*/
						
						for(int k=0; k < ec.size() && k != j; k++){/** Check for other column which is not same as this column in this equivalence class */
							
							Node n = ec.get(k);
							Column eqCol = ec.get(k).getColumn(); 
							
							/**If this column is not unique and if there are atleast two tuple*/
							if( !eqCol.isUnique() && cvc.getNoOfTuples().get(n.getTableNameNo())>1)
								return ec.get(k).getColumn();
						}
					}
				}
			}
			
		}
		return null;
	}
}
