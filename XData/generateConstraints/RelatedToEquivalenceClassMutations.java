package generateConstraints;

import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

public class RelatedToEquivalenceClassMutations {

	private static Logger logger = Logger.getLogger(RelatedToEquivalenceClassMutations.class.getName());
	/**
	 * This function segregates nulled columns and gets constraints for them
	 * @param cvc
	 * @param qbt
	 * @param ec
	 * @param eceNulled
	 * @return
	 * @throws Exception
	 */
	public static boolean getConstraintsForNulledColumns(GenerateCVC1 cvc,	QueryBlockDetails qbt, Vector<Node> ec, Node eceNulled)	throws Exception {
		
		Table tableNulled = eceNulled.getTable();
		Column colNulled = eceNulled.getColumn();
		
		ArrayList<Node> S = new ArrayList<Node>();
		S.add(eceNulled);
		ArrayList<Node> nullableFKs = new ArrayList<Node>();
		
		for(int k=0; k<ec.size(); k++){
			Node ece = ec.get(k);
			Table tableEce = ece.getTable();
			Column colEce = ece.getColumn();
			/**TODO: Maintain a data structure for list of PK and FK so as to compare in one pass.*/
			for(int l=0; l < cvc.getForeignKeys().size(); l++){
				
				/** In ForeignKeys Left points to the foreign key attribute while the right points to the Primary or the referenced column */
				Node fk = cvc.getForeignKeys().get(l);
				
				/** Adapted for Nullable foreign keys.
				 *  If FK is not nullable, then we need to null it along with the referenced relation which is eceNulled
				 */
				if( (fk.getLeft().getTable() == tableEce && fk.getLeft().getColumn() == colEce) 
						&& (fk.getRight().getTable() == tableNulled && fk.getRight().getColumn() == colNulled)){
					if(!colEce.isNullable())
						S.add(ece);/**To be taken along with nulled Column*/
					if(colEce.isNullable())
						nullableFKs.add(ece);/**To be taken along with nulled column, in case P.size > 1 */
				}
	
			}
		}
		
		/**
		 * Now, we have in S, the nulled column along with the foreign key columns in that equivalence class
		 * that reference the nulled column and are not nullable.
		 * But, if EC - S, contains a nullable column which references the nulled column, 
		 * AND also contains some other column which may or may not be nullable, 
		 * then we cannot assign NULL to the nullable FK column. We then need to nullify the nullable FK column
		 * along with the nulled column. If EC - S, just contains a single nullable column referencing the 
		 * nulled column, then we can assign NULL to that column in order to nullify the nulled column.   
		 */
		
		/** Form P = EC - S */
		ArrayList<Node> P = new ArrayList<Node>();
		for(int k=0; k<ec.size(); k++)						
			if(cvc.getqStructure().alreadyNotExistInEquivalenceClass(S, ec.get(k)))
				P.add(ec.get(k));
		
		
		logger.log(Level.INFO,"R.a = "+eceNulled);
		logger.log(Level.INFO,"EC : "); Node.printPredicateVector(ec);
		/** For Now : if P is empty continue; */
		if(P.size() == 0)
			return false;
		
		/**check if the column in P is referencing the nulled column and is nullable. 
		 * If so, then we can nullify the nulled relation inspite of the FK	 */
		if(P.size() == 1 
				&& P.get(0).getColumn().getReferenceColumn() == eceNulled.getColumn()
				&& P.get(0).getColumn().isNullable()){
			
			String outerTableNo= P.get(0).getTableNameNo(); 
			int offset = cvc.getRepeatedRelNextTuplePos().get(outerTableNo)[1];
			int count = cvc.getNoOfTuples().get(outerTableNo);
	
			for(int k=1;k<=count;k++){
				cvc.getConstraints().add(GenerateCVCConstraintForNode.cvcSetNull(cvc,P.get(0).getColumn(), k+offset-1+""));
			}
		}
		
		else{
			/**
			 * Otherwise, we need to nullify the foreign keys along with the nulled column.
			 * Note that we need to do this irrespective of whether the column in P is FK 
			 * or whether it is nullable. Because, then we cannot assign NULL to any of the columns in P
			 * This is because, NULLs cannot be equated and hence the joins in P will not be propagated up.
			 */
			P.removeAll(nullableFKs);
			if(P.size()==0)	return false;
			//Now continue
			for(int k=0;k<P.size()-1;k++)
				cvc.getConstraints().add( GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, qbt, P.get(k), P.get(k+1)) );
				
		}
		
		
		/** Now generate negative conditions for Nulled relation */
		cvc.getConstraints().add( GenerateJoinPredicateConstraints.genNegativeConds(cvc, qbt, eceNulled, P.get(0)) );
		
		return true;
	}

}
