package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

public class GenerateConstraintForUnintendedJoins {

	/** 
	 * get the constraints to kill unintended joins in this conjunct of this query block
	 * @param cvc
	 * @param qbt
	 * @param con
	 * @return
	 */
	public static String getConstraintsForUnintendedJoin(GenerateCVC1 cvc, QueryBlockDetails qbt, ConjunctQueryStructure con) throws Exception{

		/** Stores the constraint*/
		String constraint = "";

		/**get the list of tables in this query block*/
		HashMap<String, Table> tables = QueryBlockDetails.getListOfTablesInQueryBlock(cvc, qbt);
		ConstraintGenerator constrGen = new ConstraintGenerator();
		/**for each table, get the extra column names which have same name but not involved in equivalence classes*/
		ArrayList< ArrayList<Node> > extraCommonCols = GenerateConstraintForUnintendedJoins.getExtraColumnsWithCommonName( cvc, tables, qbt, con);

		/**get the constraints such that the extra columns are not equal*/
		/**here we are making it to differ in first tuple
		 * But we could make it to differ across any tuple, if there are multiple relations */
		for( ArrayList<Node> extraCol: extraCommonCols){
			
			for(int i=0; i < extraCol.size() - 1; i++){
				
				Node n1 = extraCol.get(i);
				Node n2 = extraCol.get(i+1);
				//
				String constraintString = GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, qbt, n1, n2, "/=");
				
				String constraint1 = "";
				
				constraint += constrGen.revertAndToOR(constraintString);
				
				
				
			
				/**split this string at ';'*/
				/*for(String str: constraintString.split(";") )	
					if( str.length() >= 7)
						constraint1 += str.substring(7, str.length()) + " OR ";
				
				//any of these tuples can differ
				if(constraint1.length() >= 4 )
					constraint += "ASSERT " + constraint1.substring(0, constraint1.length() - 3) + ";\n";
					*/
			}
		}
		return constraint;
	}

	/**
	 * Checks if the given two columns are involved in equi joins
	 * @param qbt
	 * @param conj
	 * @param tableName
	 * @param tableName
	 * @param col1
	 * @param col2
	 * @return
	 */
	public static boolean involvedInEquiJoins(QueryBlockDetails qbt, ConjunctQueryStructure conj, String tableName1, String tableName2, String col1, String col2) {

		/**for each equivalence class in this conjunct*/
		for( Vector<Node> ec: conj.getEquivalenceClasses())

			/**for each node in this equivalance class*/
			for(int k = 0; k < ec.size(); k++)

				for(int l = 0; l < ec.size(); l++)

					if( k!= l)/**if this is not same equivalence class*/

						/**check if these correspond to this columns*/
						if(ec.get(k).getTableNameNo().equalsIgnoreCase(tableName1) &&
						   ec.get(k).getColumn().getColumnName().equalsIgnoreCase(col1) &&
						   ec.get(l).getTableNameNo().equalsIgnoreCase(tableName2) &&
						   ec.get(l).getColumn().getColumnName().equalsIgnoreCase(col2))	
								return true;
		return false;
	}

	/**
	 * Get the list of columns (in the form of nodes )in the given set of relations with same name but are not involved in equi joins
	 * @param tables
	 * @param qbt
	 * @param conj TODO
	 * @return
	 */
	public static ArrayList< ArrayList<Node> > getExtraColumnsWithCommonName(GenerateCVC1 cvc,  HashMap<String, Table> tablesList, QueryBlockDetails qbt, ConjunctQueryStructure conj) {

		/**stores list of tables in this query block*/
		ArrayList< ArrayList<Node> > commCols = new ArrayList< ArrayList<Node> >();

		/**get the list of tables in the given hash map*/
		//	ArrayList< Table > tables = new ArrayList<Table>( tablesList.values());

		/**get the list of all table occurrences*/
		ArrayList< String> tableNamesNo = new ArrayList<String>( tablesList.keySet());

		/**for each pair of table occurrences check*/
		for(int i = 0; i < tableNamesNo.size(); i++)
			for(int j = i + 1; j < tableNamesNo.size(); j++){

				/**get the first table name*/
				Table table1 = tablesList.get(tableNamesNo.get(i));

				/**get the second table name*/
				Table table2 = tablesList.get(tableNamesNo.get(j));

				/**for each column  in table1*/
				for(Column col1: table1.getColumns().values()){

					/**for each column in table2*/
					for(Column col2: table2.getColumns().values()){

						/**if these two columns have same name*/
						if( col1.getColumnName().equalsIgnoreCase( col2.getColumnName() ) ){

							/**check if these two columns are involved in the equi join conditions
							 * If these are not involved in equi joins*/
							if( !involvedInEquiJoins( qbt, conj, tableNamesNo.get(i), tableNamesNo.get(j), col1.getColumnName(), col2.getColumnName())){

								/**create a node for first column*/
								Node n1 = new Node();
								n1.setColumn(col1);
								
								/** set query type and index*/
								int qType = cvc.getTableNames().get( tableNamesNo.get(i))[0];
								int qIndex = cvc.getTableNames().get( tableNamesNo.get(i))[1];
								n1.setQueryIndex(qIndex);
								n1.setQueryType(qType);
								
								/**set other details*/
								n1.setTable(table1);
								n1.setTableAlias("");
								n1.setTableNameNo(tableNamesNo.get(i));
								n1.setType(Node.getColRefType());
								
								/**create a node for second column*/
								Node n2 = new Node();
								n2.setColumn(col2);

								/** set query type and index*/
								qType = cvc.getTableNames().get( tableNamesNo.get(j))[0];
								qIndex = cvc.getTableNames().get( tableNamesNo.get(j))[1];
								n2.setQueryIndex(qIndex);
								n2.setQueryType(qType);
								
								/**set other details*/
								n2.setTable(table2);
								n2.setTableAlias("");
								n2.setTableNameNo(tableNamesNo.get(j));
								n2.setType(Node.getColRefType());
								
								ArrayList<Node> list = new ArrayList<Node>();
								list.add( n1);
								list.add( n2);
								
								/**add this to the list*/
								commCols.add(list);
								
							}
						}
					}
				}
			}
		return commCols;		
	}

}
