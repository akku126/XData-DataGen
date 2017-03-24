package generateCVC4Constraints;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;

public class GenerateCommonConstraintsForNodeSMT {

	private static Logger logger=Logger.getLogger(GenerateCommonConstraintsForNodeSMT.class.getName());
	/**
	 * Returns the cvc statement for assignment of a NULL value to a particular Tuple
	 * Accordingly also sets whether the null value for that column has been used or not 
	 * This is done in the HashMap colNullValuesMap
	 * @param cvc
	 * @param c
	 * @param index
	 * @return
	 */
	public static String cvcSetNull(GenerateCVC1 cvc, Column c, String index){
		HashMap<String, Integer> nullValues = cvc.getColNullValuesMap().get(c);
		Iterator<String> itr = nullValues.keySet().iterator();
		boolean foundNullVal = false;
		String nullVal = "";
		while(itr.hasNext()){
			nullVal = itr.next();
			if(nullValues.get(nullVal)==0){
				nullValues.put(nullVal, Integer.parseInt(index));
				foundNullVal = true;
				break;
			}
		}
		/** If found */
		if(foundNullVal){
			return "\n (ASSERT O_"+cvcMapSMT(c, index)+" = "+nullVal+")";
		}
		else{
			System.out.println("In cvcSetNull Function: "+"Unassigned Null value cannot be found due to insufficiency");
			logger.log(Level.WARNING,"Unassigned Null value cannot be found due to insufficiency");		
			return "";
		}
	}

	

	/**
	 * Used to get SMT LIB constraint for this column for the given tuple position
	 * @param col
	 * @param index
	 * @return
	 */
	public static String cvcMapSMT(Column col, String index){
			Table table = col.getTable();
			String tableName = col.getTableName();
			String columnName = col.getColumnName();
			
			String smtCond = "";
			String colName =tableName+"_"+columnName;		
			smtCond = "("+colName+" "+"(select O_"+tableName+" "+index +") )";
			return smtCond;
		}

	/**
	 * Used to get SMT LIB constraint for this column
	 * @param col
	 * @param n
	 * @return
	 */
	public static String cvcMapSMT(Column col, Node n){
		Table table = col.getTable();
		String tableName = col.getTableName();
		String columnName = col.getColumnName();
		String tableNo = n.getTableNameNo();
		
		int index = Integer.parseInt(tableNo.substring(tableNo.length()-1));
		int pos = table.getColumnIndex(columnName);
		
		String smtCond = "";
		String colName =tableName+"_"+columnName;		
		smtCond = "("+colName+" "+"(select O_"+tableName+" "+index +") )";
		return smtCond;
	}

	
}
