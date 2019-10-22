package generateConstraints;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

import oracle.net.aso.a;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import testDataGen.DataType;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.Configuration;
import util.Utilities;

/**
 * FIXME: Add GOOD DOC FOR THIS
 * @author mahesh
 *
 */
public class GetSolverHeaderAndFooter {

	/*
	 * Since we have commented the generation of modified query, we have also not considered generating ctid values.
	 * Hence we also comment the function generateCVC3_Header which uses the number of ctid values for the count of the total records of that table.
	 * We then create a new function as below which just adds the different values to the input database.
	 * Also, we are not considering the input database. Hence there is no need for the input tuples and its range.
	 */

	public static String  generateSolver_Header( GenerateCVC1 cvc) throws Exception {
		return generateSolver_Header(cvc, false);
	}
	
	/**
	 * For all the columns in the equivalence class, aggregate all the values into a single set. Since all the columns are equivalent we 
	 * can put the same set for all the columns in the map.
	 * @param columnToStringsMap
	 * @param equivalenceClass
	 */
	public static void copyConstantsForEquivalenceClasses (Map<String, Set<String>> columnToStringsMap, Vector<Node> equivalenceClass) {
		Set<String> allConstantSet = new HashSet<>();
		List<String> allColumnsList = new ArrayList<>(); 
		for (Node n: equivalenceClass) {
			String columnName = n.getColumn().getColumnName();
			if (columnToStringsMap.containsKey(columnName)) {
				allConstantSet.addAll(columnToStringsMap.get(columnName));
			}
			allColumnsList.add(columnName);
		}
		for (String tempColumnName: allColumnsList) {
			columnToStringsMap.put(tempColumnName, allConstantSet);
		}
	}
	
	/**
	 * 
	 * This function will find all the string constants that are declared in the query for each column.
	 * For example if the query is "select * from employee where dept = 'CSE'", we will add to the map the column dept and the string 'CSE'.
	 * 
	 * @param cvc
	 * @param equivalenceClasses1
	 * @return columnToStringMap
	 */
	public static Map<String, Set<String>> getStringConstantsInQuery (GenerateCVC1 cvc, Vector<Vector<Node>> equivalenceClasses1) {
		Map<String, Set<String>> columnToStringsMap = new HashMap<>();
		List<ConjunctQueryStructure> conjunctQueryStructureList = cvc.getOuterBlock().getConjunctsQs();
		for (ConjunctQueryStructure conjunctQueryStructure : conjunctQueryStructureList){
			appendStringConstants(columnToStringsMap, conjunctQueryStructure);
		};
		List<QueryBlockDetails> whereClauseQueriesList = cvc.getOuterBlock().getWhereClauseSubQueries();
		for (QueryBlockDetails query: whereClauseQueriesList) {
			for (ConjunctQueryStructure conjunctQueryStructure: query.getConjunctsQs())
				appendStringConstants(columnToStringsMap, conjunctQueryStructure);
			for (QueryBlockDetails whereSubQuery: query.getWhereClauseSubQueries()) {
				appendStringConstatsForQuery(columnToStringsMap, whereSubQuery);
			}
		}
		for (Vector<Node> equivalenceClass: equivalenceClasses1)
			copyConstantsForEquivalenceClasses(columnToStringsMap, equivalenceClass);
		return columnToStringsMap;
	}
	
	private static void appendStringConstatsForQuery (Map<String, Set<String>> columnToStringsMap,
			QueryBlockDetails query) {
		for (ConjunctQueryStructure conjunctQueryStructure: query.getConjunctsQs())
			appendStringConstants(columnToStringsMap, conjunctQueryStructure);
		for (QueryBlockDetails whereSubQuery: query.getWhereClauseSubQueries()) {
			appendStringConstatsForQuery(columnToStringsMap, whereSubQuery);
		}
	}

	/**
	 * A conjunct query structure contains all the conditions that are declared in a query. We will parse through the ConjunctQueryStructure and find all the
	 * string constants used in the query and their associated columns. We will finally add them to the map.
	 * @param columnToStringsMap
	 * @param conjunctQueryStructure
	 */
	private static void appendStringConstants(Map<String, Set<String>> columnToStringsMap,
			ConjunctQueryStructure conjunctQueryStructure) {
		if (conjunctQueryStructure.getSelectionConds() != null && !conjunctQueryStructure.getSelectionConds().isEmpty()) {
			for (Node condition: conjunctQueryStructure.getSelectionConds()) {
				if(DataType.getDataType(condition.getLeft().getColumn().getDataType()) != 3) // check if the column is of String type
					continue;
				Set<String> tempList;
				if (!columnToStringsMap.containsKey(condition.getLeft().getColumn().getColumnName())) {
					tempList = new HashSet<>();
					columnToStringsMap.put(condition.getLeft().getColumn().getColumnName(), tempList);
				}
				tempList = columnToStringsMap.get(condition.getLeft().getColumn().getColumnName());
				tempList.add(condition.getRight().getStrConst());
			}
		}
	}
	

	/**
	 * Generates CVC Data Type for each column
	 * First gets the name of equivalence columns from the file equivalenceColumns in the path  
	 * Unique flag ensures that the values in the enumerations specific to the string fields does not contain
	 * duplicate values.
	 */
	public static String  generateSolver_Header( GenerateCVC1 cvc, Boolean unique) throws Exception{
		DataType dt = new DataType();

		String cvc_tuple = "", tableName="";
		String tempStr = "";
		HashMap<Vector<String>,Boolean> equivalenceColumns = new HashMap<Vector<String>, Boolean>();
		
		
		String equivalenceColumnsFile=Configuration.homeDir+"/temp_smt" +cvc.getFilePath() + "/equivalenceColumns";
		
		File f = new File(equivalenceColumnsFile);
		f.createNewFile(); //this will create the equivalence columns file iff it is not already present
		
		
		BufferedReader readEqCols =  new BufferedReader(new FileReader(equivalenceColumnsFile));
		String eqCol = "";
		Map <Integer,Column> columnNameEq = new HashMap<Integer,Column>();
		try{
		while((eqCol = readEqCols.readLine()) != null){
			Vector<String> vecEqCol = new Vector<String>();
			String[] t;
			t = eqCol.split(" ");
			for(int i=0;i<t.length;i++){
				vecEqCol.add(t[i]);
			}
			equivalenceColumns.put(vecEqCol, false);
		}
		ConstraintGenerator constraintGen = new ConstraintGenerator();				
		String cvc_datatype = constraintGen.getHeader();
		
		Vector<Vector<Node>>  equivalenceClasses1 = new Vector<Vector<Node>>();
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs())
			equivalenceClasses1.addAll(con.getEquivalenceClasses());
		
		for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries())
			for(ConjunctQueryStructure con: qb.getConjunctsQs())
				equivalenceClasses1.addAll(con.getEquivalenceClasses());
		
		for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries())
			for(ConjunctQueryStructure con: qb.getConjunctsQs())
				equivalenceClasses1.addAll(con.getEquivalenceClasses());
		
		//function to get all the string constants used in equality constraints
		Map<String, Set<String>> columnToStringsMap =  getStringConstantsInQuery(cvc, equivalenceClasses1);
		
		
		Vector<Column> eqColsVector = new Vector<Column>();
		for(int i=1; i < cvc.getResultsetColumns().size(); i++){
			if(i > 1){
				cvc_datatype = "";
			}
			Column column = cvc.getResultsetColumns().get(i);
			String columnName = column.getColumnName();		
			int columnType = 0;
		
			
			
			Vector<String> columnValue = column.getColumnValues();
			if (columnToStringsMap.containsKey(columnName)) {				
				columnValue.addAll(columnToStringsMap.get(columnName));
				columnValue = new Vector<>(new HashSet<>(columnValue));
			}
			columnType = dt.getDataType(column.getDataType());
			cvc_tuple += columnName+", ";
			boolean columnEquivalentAlreadyAdded = false;
			String equivalentColumn = "";
			String isNullMembers = "";
			switch(columnType){//FIXME: ADD CORRECT CODE FOR MIN AND MAXIMUM VALUES OF COLUMNS
								//This approach cause problems in some cases
			case 1://case 2:
			{
				int min, max;
				min = max = 0;
				column.setCvcDatatype("Int");
				boolean limitsDefined = false;
				if( columnValue.size()>0 ) {
					for (int j=0; j<columnValue.size(); j++) {
						int colValue = (int) Float.parseFloat(columnValue.get(j));
						//Commented out by Biplab
						//								isNullMembers += "ASSERT NOT ISNULL_"+columnName+"("+colValue+");\n";
						if (!limitsDefined) {
							min = colValue;
							max = colValue;
							limitsDefined = true;
							continue;
						}
						if (min > colValue) {
							min = colValue;
						}
						if(max < colValue) {
							max = colValue;
						}
					}
				}
				
				cvc_datatype += constraintGen.getIntegerDatatypes(column, min, max);
				cvc_datatype += constraintGen.getIntegerNullDataValues(column);
				
				//Adding support for NULLs
				//Consider the range -99996 to -99999 as NULL integers
			/*	cvc_datatype = "\n"+columnName+" : TYPE = SUBTYPE (LAMBDA (x: INT) : (x > "+(min-4)+" AND x < "+(max+4)+") OR (x > -100000 AND x < -99995));\n";
			

				
				
				cvc_datatype += "ISNULL_" + columnName +" : "+ columnName + " -> BOOLEAN;\n";
				*/
				HashMap<String, Integer> nullValuesInt = new HashMap<String, Integer>();
				for(int k=-99996;k>=-99999;k--){
					//isNullMembers += "ASSERT ISNULL_"+columnName+"("+k+");\n";
					nullValuesInt.put(k+"",0);
				}
				cvc.getColNullValuesMap().put(column, nullValuesInt);
				column.setMinVal(min);
				column.setMaxVal(max);
			
				break;
			} 
			case 2:
			{
				double min, max;
				max = 0;
				min= 0;
				boolean limitsDefined = false;
				column.setCvcDatatype("Real");
				if(columnValue.size()>0){
					for(int j=0; j<columnValue.size(); j++){
						double colValue = Double.parseDouble(columnValue.get(j));
						String strColValue=util.Utilities.covertDecimalToFraction(colValue+"");
						//Commented out by Biplab
						//							isNullMembers += "ASSERT NOT ISNULL_"+columnName+"("+strColValue+");\n";
						if(!limitsDefined){
							min = colValue;
							max = colValue;
							limitsDefined = true;
							continue;
						}
						if(min > colValue)
							min = colValue;
						if(max < colValue)
							max = colValue;
					}
				}
				//Adding support for NULLs
				//Consider the range -99996 to -99999 as NULL integers
				String maxStr=util.Utilities.covertDecimalToFraction(max+"");
				String minStr=util.Utilities.covertDecimalToFraction(min+"");
				
				cvc_datatype += constraintGen.getRealDatatypes(column,min,max);
				cvc_datatype += constraintGen.getRealNullDataValues(column);
				
				/*cvc_datatype = "\n"+columnName+" : TYPE = SUBTYPE (LAMBDA (x: REAL) : (x >= "+(minStr)+" AND x <= "+(maxStr)+") OR (x > -100000 AND x < -99995));\n";
			
				cvc_datatype += "ISNULL_" + columnName +" : "+ columnName + " -> BOOLEAN;\n";
				*/
				HashMap<String, Integer> nullValuesInt = new HashMap<String, Integer>();
				for(int k=-99996;k>=-99999;k--){
					//isNullMembers += "ASSERT ISNULL_"+columnName+"("+k+");\n";
					nullValuesInt.put(k+"",0);
				}
				cvc.getColNullValuesMap().put(column, nullValuesInt);
				//cvc_datatype += isNullMembers;
				column.setMinVal(min);
				column.setMaxVal(max);
				
				break;
			}
			case 3:
			{
				String colValue = "";
				HashSet<String> uniqueValues = new HashSet<String>();
				isNullMembers = "";
				
				cvc_datatype += constraintGen.getStringDataTypes(columnValue,column,unique);
				
				HashMap<String, Integer> nullValuesChar = new HashMap<String, Integer>();
				for(int k=1;k<=4;k++){
					//isNullMembers += "ASSERT ISNULL_" + columnName+"(NULL_"+columnName+"_"+k+");\n";
					nullValuesChar.put("NULL_"+columnName+"_"+k, 0);
				}						
				//cvc_datatype += isNullMembers;
				
				//If there are more than one equivalence classes, then set the cvcDatatype for the columns
				for(Vector<Node> v: equivalenceClasses1){
				
					Column baseEqClass = v.get(0).getColumn();					 
		 			for(int cj=1;cj<v.size();cj++){
						Column cjColumn = v.get(cj).getColumn();
						//If ResultSet column is an equivalence class column, 
						//set the cvcdatatype of this column as the Column name of equivalence class
						// This should be done only for Strings (Char, Varchar,etc.,)
						if((columnName.equals(cjColumn.getColumnName())
								|| (column.getReferenceColumn() != null && column.getReferenceColumn().toString().equalsIgnoreCase(cjColumn.getColumnName()))) 
								&& columnType == 3){	
							column.setCvcDatatype(baseEqClass.getColumnName()); 	
							columnType = -1;
							break; 
						} else{
							column.setCvcDatatype(columnName);
						}
					} 
					if(columnType == -1){
						break;
					}
				}
				//If there are no equivalence classes or there is only one set, then set column name itself
				if(equivalenceClasses1 == null ||
						(equivalenceClasses1 != null && equivalenceClasses1.size() == 0)){
					column.setCvcDatatype(columnName);
				}
				cvc.getColNullValuesMap().put(column, nullValuesChar);
				
				break;
			}
			case 5: //DATE
			{
				long min, max;
				min = max = 0;
				boolean limitsDefined = false;
				if(columnValue.size()>0){
					for(int j=0; j<columnValue.size(); j++){
						java.sql.Date t= java.sql.Date.valueOf(columnValue.get(j));

						long colValue = t.getTime()/86400000;

						isNullMembers += "ASSERT NOT ISNULL_"+columnName+"("+colValue+");\n";
						if(!limitsDefined){
							min = colValue;
							max = colValue;
							limitsDefined = true;
							continue;
						}
						if(min > colValue)
							min = colValue;
						if(max < colValue)
							max = colValue;
					}
				}
				//Adding support for NULLs
				//Consider the range -99996 to -99999 as NULL integers
				/*cvc_datatype = "\n"+columnName+" : TYPE = SUBTYPE (LAMBDA (x: INT) : (x > "+(min-1)+" AND x < "+(max+1)+") OR (x > -100000 AND x < -99995));\n";
				cvc_datatype += "ISNULL_" + columnName +" : "+ columnName + " -> BOOLEAN;\n";
				*/
				HashMap<String, Integer> nullValuesInt = new HashMap<String, Integer>();
				for(int k=-99996;k>=-99999;k--){
					//isNullMembers += "ASSERT ISNULL_"+columnName+"("+k+");\n";
					nullValuesInt.put(k+"",0);
				}
				
				cvc.getColNullValuesMap().put(column, nullValuesInt);
				//cvc_datatype += isNullMembers;
				column.setMinVal(min);
				column.setMaxVal(max); 
				column.setCvcDatatype("DATE");
				break;
			}
			case 6: //TIME
			{
				long min, max;
				min = max = 0;
				boolean limitsDefined = false;
				if(columnValue.size()>0){
					for(int j=0; j<columnValue.size(); j++){
						Time t= Time.valueOf(columnValue.get(j));

						long colValue = (t.getTime()%86400000)/1000;

						//isNullMembers += "ASSERT NOT ISNULL_"+columnName+"("+colValue+");\n";
						if(!limitsDefined){
							min = colValue;
							max = colValue;
							limitsDefined = true;
							continue;
						}
						if(min > colValue)
							min = colValue;
						if(max < colValue)
							max = colValue;
					}
				}
				//Adding support for NULLs
				//Consider the range -99996 to -99999 as NULL integers
				/*cvc_datatype = "\n"+columnName+" : TYPE = SUBTYPE (LAMBDA (x: INT) : (x > "+(min-1)+" AND x < "+(max+1)+") OR (x > -100000 AND x < -99995));\n";
				cvc_datatype += "ISNULL_" + columnName +" : "+ columnName + " -> BOOLEAN;\n";
				*/
				HashMap<String, Integer> nullValuesInt = new HashMap<String, Integer>();
				for(int k=-99996;k>=-99999;k--){
					//isNullMembers += "ASSERT ISNULL_"+columnName+"("+k+");\n";
					nullValuesInt.put(k+"",0);
				}
				cvc.getColNullValuesMap().put(column, nullValuesInt);
				//cvc_datatype += isNullMembers;
				column.setMinVal(min);
				column.setMaxVal(max);
				column.setCvcDatatype("TIME");
				break;
			}
			case 7: //TIMESTAMP
			{
				long min, max;
				min = max = 0;
				boolean limitsDefined = false;
				if(columnValue.size()>0){
					for(int j=0; j<columnValue.size(); j++){
						Timestamp t= Timestamp.valueOf(columnValue.get(j));

						long colValue = t.getTime()/1000; //converting from milli sec to sec

						//isNullMembers += "ASSERT NOT ISNULL_"+columnName+"("+colValue+");\n";
						if(!limitsDefined){
							min = colValue;
							max = colValue;
							limitsDefined = true;
							continue;
						}
						if(min > colValue)
							min = colValue;
						if(max < colValue)
							max = colValue;
					}
				}
				//Adding support for NULLs
				//Consider the range -99996 to -99999 as NULL integers
				/*cvc_datatype = "\n"+columnName+" : TYPE = SUBTYPE (LAMBDA (x: INT) : (x > "+(min-1)+" AND x < "+(max+1)+") OR (x > -100000 AND x < -99995));\n";
				cvc_datatype += "ISNULL_" + columnName +" : "+ columnName + " -> BOOLEAN;\n";
				*/
				HashMap<String, Integer> nullValuesInt = new HashMap<String, Integer>();
				for(int k=-99996;k>=-99999;k--){
				//	isNullMembers += "ASSERT ISNULL_"+columnName+"("+k+");\n";
					nullValuesInt.put(k+"",0);
				}
				cvc.getColNullValuesMap().put(column, nullValuesInt);
				//cvc_datatype += isNullMembers;
				column.setMinVal(min);
				column.setMaxVal(max);
				column.setCvcDatatype("TIMESTAMP");
				break;
			}

			}
			if(!cvc.getDatatypeColumns().contains(columnName)){//prevent repetition of data types in cvc file
				cvc.getDatatypeColumns().add(columnName);
				tempStr += cvc_datatype+"\n";
				
				cvc_datatype += constraintGen.getNullMembers(column);
				
				HashMap<String, Integer> nullValuesChar = new HashMap<String, Integer>();
				for(int k=-99996;k>=-99999;k--){
					nullValuesChar.put(k+"",0);
				}
				//colNullValuesMap.put(column, nullValuesChar);
				//cvc_datatype += isNullMembers;
			}
		}

		//Adjust datatypes for strings : In case of foreign keys or equivalence classes the colums may hav different names 
		//Still they must take same values when there is an equality implied
		Vector<Node> tempForeignKeys=new Vector<Node>();
		Vector<Node> tempForeignKeys1=new Vector<Node>();
		for(Node n :cvc.getForeignKeys()){
			if(n.getLeft().getColumn().getCvcDatatype() != null && n.getRight().getColumn().getCvcDatatype() != null && !n.getLeft().getColumn().getCvcDatatype().equals(n.getRight().getColumn().getCvcDatatype())){
				tempForeignKeys.add(n);
			}
		}

		/**FIXME: ISSUES WITH EQUIVALENCE CLASSES */
		Vector<Vector<Node>>  equivalenceClasses = new Vector<Vector<Node>>();
		for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs())
			equivalenceClasses.addAll(con.getEquivalenceClasses());
		
		for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries())
			for(ConjunctQueryStructure con: qb.getConjunctsQs())
				equivalenceClasses.addAll(con.getEquivalenceClasses());
		
		for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries())
			for(ConjunctQueryStructure con: qb.getConjunctsQs())
				equivalenceClasses.addAll(con.getEquivalenceClasses());
		
		//This is for setting column name as CVC type for equivalence classes - Shree start
		for(Vector<Node> v: equivalenceClasses){ 
			Column c1= v.get(0).getColumn();
			int dataType1 = c1.getDataType();
			//String s=v.get(0).getColumn().getCvcDatatype();
			if(dataType1 == 1|| dataType1== 2|| dataType1== 4|| dataType1 == 5 || dataType1 ==6 || dataType1 ==7)
				continue;	
			for(int j=1;j<v.size();j++){
					int dataType2 = v.get(j).getColumn().getDataType();
					Column c2 = v.get(j).getColumn();
					
					if(dataType1 == dataType2 && dt.getDataType(dataType2) == 3 && !(c1.getCvcDatatype().equalsIgnoreCase(c2.getCvcDatatype()))){
						c2.setCvcDatatype(c1.getColumnName());
										
					}
			} 
									
			}
		//This is for setting column name as CVC type for equivalence classes - Shree End
		
		for(Vector<Node> v: equivalenceClasses){
			//Column c=v.get(0).getColumn();
			String s=v.get(0).getColumn().getCvcDatatype();
			if(s != null && (s.equals("INT") || s.equals("REAL") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
				continue;
			for(int i=0;i<v.size();i++){
				Column c=v.get(i).getColumn();
				if(c!= null && c.getCvcDatatype() != null && !c.getCvcDatatype().equals(s))
					c.setCvcDatatype(s);
				
				for(Node n :tempForeignKeys){
					if(n.getLeft().getColumn().equals(c)){
						n.getRight().getColumn().setCvcDatatype(s);
						tempForeignKeys1.add(n);
					}
					else if(n.getRight().getColumn().equals(c)){
						n.getLeft().getColumn().setCvcDatatype(s);
						tempForeignKeys1.add(n);
					}
				}
				tempForeignKeys.removeAll(tempForeignKeys1);
				tempForeignKeys1=new Vector<Node>();

			}
		}

		for(Node n:tempForeignKeys){
			n.getLeft().getColumn().setCvcDatatype(n.getRight().getColumn().getCvcDatatype());
		}
		tempStr += constraintGen.getTupleTypesForSolver(cvc);
		/*
		 * End building datatypes for all the columns
		 * Now build the tuple types
		 */
	/*	tempStr += "\n%Tuple Types for Relations\n";
		Column c;
		Table t;
		String temp;
		Vector<String> tablesAdded = new Vector<String>();

		for(int i=0;i<cvc.getResultsetTables().size();i++){
			t = cvc.getResultsetTables().get(i);
			temp = t.getTableName();
			if(!tablesAdded.contains(temp)){
				tempStr += temp + "_TupleType: TYPE = [";
			}
			for(int j=0;j<cvc.getResultsetColumns().size();j++){
				c = cvc.getResultsetColumns().get(j);
				if(c.getTableName().equalsIgnoreCase(temp)){
					String s=c.getCvcDatatype();
					if(s!= null && (s.equals("INT") || s.equals("REAL") || s.equals("TIME") || s.equals("DATE") || s.equals("TIMESTAMP")))
						tempStr += c.getColumnName() + ", ";
					else
						tempStr+=c.getCvcDatatype()+", ";
				}
			}
			tempStr = tempStr.substring(0, tempStr.length()-2);
			tempStr += "];\n";
			
			 * Now create the Array for this TypleType
			 
			tempStr += "O_" + temp + ": ARRAY INT OF " + temp + "_TupleType;\n";
		}*/
		
	}
		finally{
			if(readEqCols != null)
				readEqCols.close();
		}
		return tempStr;
	}

	public static String generateSolver_Footer(GenerateCVC1 cvc) {
		
		String temp="";
		
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		temp += constraintGen.getFooter(cvc);
		
		return temp;
	}
	
	public static String generateSolver_FooterForAgg(GenerateCVC1 cvc) {
		
		String temp="";
		
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		temp += constraintGen.getFooterForAgg(cvc);
		
		return temp;
	}
}
