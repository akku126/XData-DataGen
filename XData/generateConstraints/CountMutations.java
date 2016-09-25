package generateConstraints;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Conjunct;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * Generates constraints for killing count mutations
 * @author mahesh
 *
 */
public class CountMutations {

	private static Logger logger = Logger.getLogger(CountMutations.class.getName());
	
	/**
	 * Generates constraints for killing count mutations
	 * Procedure:
	 *  1. check if column is nullable
	 *  2. if yes then generate nullable constraint
	 *  3. if not, then do not add constraint
	 * @param cvc
	 * @param qbt
	 */
	public static String constraintsForKillingCountMutants(GenerateCVC1 cvc,	QueryBlockDetails qbt) {

		String countMutation = "";
		/**for each relation in the given query block*/
		for(String relationNo: qbt.getBaseRelations()){

			/**Get base table name for this relation*/
			String tableName = relationNo.substring(0, relationNo.length()-1);/**FIXME: If the relation occurrence >= 10 then problem*/

			/**get data base table for this relation*/
			Table table = cvc.getQuery().getFromTables().get(tableName);

			/**get the list of columns in this table*/
			HashMap<String, Column> columns = table.getColumns();

			Iterator<String> colNames = columns.keySet().iterator();

			while(colNames.hasNext()){

				Column col = columns.get(colNames.next());

				String constraint = "";
				/**check if this column of this relation occurrence is nullable */
				if( isNullable(qbt,col, relationNo)){

					logger.log(Level.INFO, col+" Is Nullable");

					/**get the number of tuples of this relation occurrence*/
					int count = cvc.getNoOfTuples().get(relationNo);
					int offset = cvc.getRepeatedRelNextTuplePos().get(relationNo)[1];

					/**FIXME: But this may conflict with NOT NULL constraint*/
					for(int i = 1; i <= count; i++)
						constraint += GenerateCVCConstraintForNode.cvcSetNull(cvc, col, (i + offset - 1) + "") + " OR ";

					constraint = constraint.substring(0, constraint.lastIndexOf("OR"));
				}

				countMutation += constraint;
			}
		}

		return countMutation;
			
	}


	/**
	 * Check if the column of the given relation occurrence is nullable or not
	 * @param qbt TODO
	 * @param col
	 * @param relationNo
	 * @return
	 */
	private static boolean isNullable(QueryBlockDetails qbt, Column col, String relationNo) {

		/**if the database constraint specifies that column is not nullable*/
		if(!col.isNullable())
			return false;

		/**FIXME: How to handle ORing of conditions*/
		/**if this column is involved in any selection or string selection or like conditions*/
		for(Conjunct con: qbt.getConjuncts()){

			/** check for non equi join conditions */
			if( UtilsRelatedToNode.presentInConds(con.getAllConds(), col, relationNo))
				return false;


			/** check for equi join conditions */
			if( UtilsRelatedToNode.presentInConds( UtilsRelatedToNode.getJoinConditions(con.getEquivalenceClasses()), col, relationNo))
				return false;

			/** check for selection conditions */
			if( UtilsRelatedToNode.presentInConds(con.getSelectionConds(), col, relationNo))
				return false;

			/** check for string selection conditions*/
			if( UtilsRelatedToNode.presentInConds(con.getStringSelectionConds(), col, relationNo))
				return false;

			/** check for like conditions */
			if( UtilsRelatedToNode.presentInConds(con.getLikeConds(), col, relationNo))
				return false;
		}
		return true;
	}

}
