import java.util.*;
import com.microsoft.z3.*;

public class selectClause {
	class TestFailedException extends Exception
	{
		
		public TestFailedException()
		{
			super("Check FAILED");
		}
		
		
	};
	
	public BoolExpr createTableExpr(Context ctx, TupleSort row, Sort columns[], int tSize, int k, String tableName) 
	{
		Sort rowType;
		ListSort table;
		rowType = row;
		table = ctx.mkListSort(ctx.mkSymbol(tableName + "_list"), rowType);
		Expr nil = ctx.mkConst(table.getNilDecl());
		Expr rowExpr[];
		rowExpr = new Expr[k];
		for(int i=0; i<k; i++) {
			Expr rowElements[];
			rowElements = new Expr[tSize];
			for(int j = 0; j < tSize; j++) {
				rowElements[j] = ctx.mkConst(tableName + "_rowEle_" + String.valueOf(i) + "_" + String.valueOf(j), columns[j]);
			}
			rowExpr[i] = row.mkDecl().apply(rowElements);
			
		}
		
		Expr tableList = ctx.mkApp(table.getConsDecl(), rowExpr[k-1], nil);
		for(int i=k-2; i>-1; i--) {
			tableList = ctx.mkApp(table.getConsDecl(), rowExpr[i], tableList);
		}
		Expr t = ctx.mkConst(tableName, table);
		BoolExpr ans = ctx.mkEq(t, tableList);
		return ans;
	}
	
	public BoolExpr createPrimaryKeyExpr(Context ctx, TupleSort row, int k, Sort columns[], int tSize, int keyColumns[], int keyColumnCount, String tableName) {
		// Creating the sorts that make up the tuplesort for primary key
		Sort pkeySort[];
		pkeySort = new Sort[keyColumnCount];
		for(int i = 0; i < keyColumnCount; i++) {
			pkeySort[i] = columns[keyColumns[i]];
		}
		
		//Creating the symbols which represents the elements of tuplesort for primary key
		Symbol pkeySymbols[];
		pkeySymbols = new Symbol[keyColumnCount];
		for(int i = 0; i < keyColumnCount; i++) {
			pkeySymbols[i] = ctx.mkSymbol(String.valueOf(i) + "_pkey_" + tableName); 
		}
		
		// Creating the tuplesort for primary key
		TupleSort pkey = ctx.mkTupleSort(ctx.mkSymbol(tableName + "_pkeyCombo_"),
				pkeySymbols,
				pkeySort
				);
		
		// Not needed
		/*
		 * // Creating the FuncDecl for getting the elements of a primary key tuple
		 * FuncDecl pkeyElement[]; pkeyElement = new FuncDecl[keyColumnCount];
		 * 
		 * for(int i = 0; i < keyColumnCount; i++) { pkeyElement[i] =
		 * pkey.getFieldDecls()[i];
		 }
		 */

		Expr pkeyTupleExprs[];
		pkeyTupleExprs = new Expr[k];
		// Creates expressions for T(Scores_i.0, Scores_i.2) for all i
		for(int i=0; i<k; i++) {
			Expr keyElements[];
			keyElements = new Expr[keyColumnCount];
			// Creates Scores_i.0, Scores_i.2 for given i
			for(int j = 0; j < keyColumnCount; j++) {
				keyElements[j] = ctx.mkConst(tableName + "_rowEle_" + String.valueOf(i) + "_" + String.valueOf(j), columns[j]);
			}
			// Creates the primary key tuple for given i
			pkeyTupleExprs[i] = pkey.mkDecl().apply(keyElements);
		}
		// Creates Distinct(T(Scores_0.0, Scores_0.2), .... , T(Scores_k-1.0, Scores_k-1.2))
		BoolExpr primaryKeyExpr = ctx.mkDistinct(pkeyTupleExprs);
		return primaryKeyExpr;	//  call getSort() to get the TupleSort of primary key
	}

	// TODO: Extend to include foreign key constraints are over nullable types
	public BoolExpr createForeignKeyExpr(Context ctx, int k1, int k2, TupleSort foreignKey, Sort fKeyColumnSorts[], int keyColumns1[], int keyColumns2[], int fsize, String tableName1, String tableName2) {
		// Declaring the functions representing Set of foreign keys in table1 and table2
		String fKey1Func = "foreignKey_" + tableName1 + "_set";
		String fKey2Func = "foreignKey_" + tableName2 + "_set";
		FuncDecl setTable1 = ctx.mkFuncDecl(fKey1Func, foreignKey, ctx.getBoolSort());
		FuncDecl setTable2 = ctx.mkFuncDecl(fKey2Func, foreignKey, ctx.getBoolSort());
		
		// Creating the tuples that make up the foreign key in table1
		Expr fKeyTupleExprs[];
		fKeyTupleExprs = new Expr[k1];
		
		for(int i = 0; i < k1; i++) {
			Expr fKeyElements[];
			fKeyElements = new Expr[fsize];
			// Creates Scores_i.0 for given i
			for(int j = 0; j < fsize; j++) {
				fKeyElements[j] = ctx.mkConst(tableName1 + "_rowEle_" + String.valueOf(i) + "_" + String.valueOf(keyColumns1[j]), fKeyColumnSorts[j]);
			}
			fKeyTupleExprs[i] = foreignKey.mkDecl().apply(fKeyElements);
		}
		
		// Applying function setTable1 on foreign key tuples of table 1 and storing their conjuction
		BoolExpr setTable1DefTrue[];
		setTable1DefTrue = new BoolExpr[k1];
		BoolExpr setTable1Def = ctx.mkTrue();
		for(int i = 0; i < k1; i++) {
			setTable1DefTrue[i] = ctx.mkEq(setTable1.apply(fKeyTupleExprs[i]), ctx.mkTrue());
			setTable1Def = ctx.mkAnd(setTable1Def, setTable1DefTrue[i]);
		}
		
		// Creating the tuples that make up the foreign key in table2
		Expr fKeyTupleExprs2[];
		fKeyTupleExprs2 = new Expr[k2];
		
		for(int i = 0; i < k1; i++) {
			Expr fKeyElements[];
			fKeyElements = new Expr[fsize];
			// Creates Scores_i.0 for given i
			for(int j = 0; j < fsize; j++) {
				fKeyElements[j] = ctx.mkConst(tableName2 + "_rowEle_" + String.valueOf(i) + "_" + String.valueOf(keyColumns1[j]), fKeyColumnSorts[j]);
			}
			fKeyTupleExprs2[i] = foreignKey.mkDecl().apply(fKeyElements);
		}
		
		// Applying function setTable2 on foreign key tuples of table 2 and storing their conjuction
		BoolExpr setTable2DefTrue[];
		setTable2DefTrue = new BoolExpr[k1];
		BoolExpr setTable2Def = ctx.mkTrue();
		for(int i = 0; i < k1; i++) {
			setTable2DefTrue[i] = ctx.mkEq(setTable2.apply(fKeyTupleExprs2[i]), ctx.mkTrue());
			setTable2Def = ctx.mkAnd(setTable2Def, setTable2DefTrue[i]);
		}
		//TODO: Assert if x is not equal to any of the fkeytupleExprs2[i] then false.
		
		// Asserting the foreign key condition that fkeys in table1 is a subset of pkeys in table2
//		(assert (forall ((x foreignKey)) (=> (table1 x) (table2 x))))
		Symbol x_name = ctx.mkSymbol("x");
		Expr x = ctx.mkConst(x_name, foreignKey);
		BoolExpr lhs = (BoolExpr)setTable1.apply(x);
		Pattern p = ctx.mkPattern(lhs);
		Expr subsetEq = ctx.mkImplies(lhs, (BoolExpr)setTable2.apply(x));
		BoolExpr quantifiedSubsetEq = ctx.mkForall( new Expr[] {x},
//		new Sort[] {foreignKey}, /* types of quantified variables */
//				new Symbol[] {x_name}, /* names of quantified variables */
				subsetEq, 1, new Pattern[] { p } /* patterns */, null, null, null);
	
		BoolExpr finalExpr = ctx.mkAnd(setTable1Def, setTable2Def, quantifiedSubsetEq);
		return finalExpr;
	}

	
	
	public BoolExpr[] assertFilterAxiom(Context ctx, BoolExpr f, Expr x0, Symbol x0_Name, String FilterName, TupleSort rows) { // f contains free vairable x0
//    	Sort a
		ListSort list;
		list = ctx.mkListSort("x1", rows);
		FuncDecl filter = ctx.mkFuncDecl(FilterName, list, list);
		Expr nil = ctx.mkConst(list.getNilDecl());
		// TODO: Chek expr as an input
		BoolExpr NilFilter = ctx.mkEq(filter.apply(nil), nil);
		
//    	Expr x0 = ctx.mkConst("x0", rows);
		Expr x1 = ctx.mkConst("x1", list); 
		Expr FilterFunc = filter.apply(ctx.mkApp(list.getConsDecl(), x0, x1));
		Pattern p = ctx.mkPattern(FilterFunc);
		Expr ite = ctx.mkITE(f, ctx.mkApp(list.getConsDecl(), x0, filter.apply(x1)), filter.apply(x1));
		Expr FinalExpr = ctx.mkEq(FilterFunc, ite);
		BoolExpr q = ctx.mkForall(new Expr[] {x0, x1},
//				new Sort[] {rows}, /* types of quantified variables */
//				new Symbol[] {x0_Name}, /* names of quantified variables */
				FinalExpr, 1, new Pattern[] { p } /* patterns */, null, null, null);
		
		return new BoolExpr[] {NilFilter, q};
	}
	
	public BoolExpr[] assertMapAxiom(Context ctx, Expr f, Expr x0, Symbol x0_Name, TupleSort selectTuples, String MapName, TupleSort rows) { // f(tuple) contains free variable x0
//    	Sort a
		ListSort list;
		ListSort list1 = ctx.mkListSort("t", selectTuples);
		list = ctx.mkListSort("x1_list", rows);
		FuncDecl map = ctx.mkFuncDecl(MapName, list, list1);
		Expr nil = ctx.mkConst(list.getNilDecl()), nil1 = ctx.mkConst(list1.getNilDecl());
		BoolExpr NilMap = ctx.mkEq(map.apply(nil), nil1);
		
//    	Expr x0 = ctx.mkConst("x0", rows);
		Expr x1 = ctx.mkConst("x1", list);
		Expr MapFunc = map.apply(ctx.mkApp(list.getConsDecl(), x0, x1));
		Pattern p = ctx.mkPattern(MapFunc);
		Expr right = ctx.mkApp(list1.getConsDecl(), f, map.apply(x1));
//    	Expr ite = ctx.mkITE(f, ctx.mkApp(list.getConsDecl(), x0, map.apply(x1)), filter.apply(x1));
		Expr FinalExpr = ctx.mkEq(MapFunc, right);
		BoolExpr q = ctx.mkForall( new Expr[] {x0, x1},
//				new Sort[] {rows}, /* types of quantified variables */
//				new Symbol[] {x0_Name}, /* names of quantified variables */
				FinalExpr, 1, new Pattern[] { p } /* patterns */, null, null, null);
		
		return new BoolExpr[] {NilMap, q};
	}
	
	public BoolExpr[] assertReduceAxiom(Context ctx, Expr x0, Expr x1, String x0_name, String x1_name, Expr t, ListSort list, Sort reduction, String ReduceName) {
		FuncDecl reduce = ctx.mkFuncDecl(ReduceName, new Sort[] {list, reduction}, reduction);
		Expr x = ctx.mkConst("x_" + ReduceName, reduction);
		Expr nil = ctx.mkConst(list.getNilDecl());
		Expr nilFunc = reduce.apply(nil, x);
		Pattern p = ctx.mkPattern(nilFunc);
		Expr NilMap = ctx.mkEq(nilFunc, x);
		BoolExpr finalNil = ctx.mkForall( new Expr[] {x},
				NilMap, 1, new Pattern[] { p } /* patterns */, null, null, null);
		
		Expr x2 = ctx.mkConst("x2_" + ReduceName, list);
		Expr LHS = reduce.apply(ctx.mkApp(list.getConsDecl(), x0, x2), x1);
		Expr RHS = reduce.apply(x2, t);
		Pattern final_p = ctx.mkPattern(LHS);
		Expr finalExpr = ctx.mkEq(LHS, RHS);
		BoolExpr q = ctx.mkForall( new Expr[] {x0, x1, x2},
				finalExpr, 1, new Pattern[] { final_p } /* patterns */, null, null, null);
		
		
		
		return new BoolExpr[] {finalNil};
	}
	
	public BoolExpr[] assertCrossAxiom(Context ctx, Expr x0, TupleSort table1, TupleSort table2, Symbol x0_name, String CrossName) {
		ListSort list1;
		ListSort list2 = ctx.mkListSort("cross_table1_list", table1);
		list1 = ctx.mkListSort("cross_table2_list", table2);
		TupleSort crosstable = ctx.mkTupleSort( ctx.mkSymbol("CrossTuple_"+CrossName),
				new Symbol[] {table1.getName(), table2.getName()},
				new Sort[] {table1, table2} 
				);
		ListSort finalList = ctx.mkListSort("cross_finaltable_list", crosstable);
		
		FuncDecl cross = ctx.mkFuncDecl(CrossName, new Sort[] {list1, list2}, finalList);
		Expr nil1 = ctx.mkConst(list1.getNilDecl());
		Expr nil2 = ctx.mkConst(list2.getNilDecl());
		Expr nilCross = ctx.mkConst(finalList.getNilDecl());
		Expr x_1 = ctx.mkConst("cross_x1", list1);
		Expr x_2 = ctx.mkConst("cross_x2", list2);
		Expr nilFunc1 = cross.apply(nil1, x_1);
		Expr nilFunc2 = cross.apply(x_2, nil2);
		Pattern p1 = ctx.mkPattern(nilFunc1);
		Pattern p2 = ctx.mkPattern(nilFunc2);
		Expr NilMap1 = ctx.mkEq(nilFunc1, nilCross);
		Expr NilMap2 = ctx.mkEq(nilFunc2, nilCross);
		BoolExpr finalNil1 = ctx.mkForall( new Expr[] {x_1},
				NilMap1, 1, new Pattern[] { p1 } /* patterns */, null, null, null);
		BoolExpr finalNil2 = ctx.mkForall( new Expr[] {x_2},
				NilMap2, 1, new Pattern[] { p2 } /* patterns */, null, null, null);
		
		FuncDecl cr = ctx.mkFuncDecl(CrossName+"temp", new Sort[] {table1, list1, list2, list2}, finalList);
		
		Expr x1 = ctx.mkConst("x1_"+CrossName, list1);
		Expr x2 = ctx.mkConst("x2_"+CrossName, list2);
		Expr x3 = ctx.mkConst("x3_"+CrossName, list2);
		Expr CrossExprLHS_3 = cross.apply(ctx.mkApp(list1.getConsDecl(), x0, x1), ctx.mkApp(list1.getConsDecl(), x2, x3));
		Expr CrossExprRHS_3 = cr.apply(x0, x1, ctx.mkApp(list1.getConsDecl(), x2, x3), ctx.mkApp(list1.getConsDecl(), x2, x3));
		Expr CrossExprLHS_4 = cr.apply(x0, x1, nil2, x2);
		Expr CrossExprRHS_4 = cross.apply(x1, x2);
		Expr x4 = ctx.mkConst("x4_"+CrossName, list2);
		Expr CrossExprLHS_5 = cr.apply(x0, x1, ctx.mkApp(list1.getConsDecl(), x2, x3), x4);
		
		//TODO: Create a tuple constructor here and use it
		//		Expr CrossExprRHS_5 = ctx.mkApp(list1.getConsDecl(), , x3);
		
		// TODO: Finish off the final bool expressions for forall using the corrections made
		return new BoolExpr[] {finalNil1, finalNil2};
	}
	
	public Expr createSelectTupleExpr(Context ctx, Expr x0, TupleSort x0_sort, String selectName){
		FuncDecl clause1 = x0_sort.getFieldDecls()[1];
		FuncDecl clause2 = x0_sort.getFieldDecls()[2];
		
		Sort sort1 = clause1.getRange();
		Sort sort2 = clause2.getRange();
//		TupleSort select = ctx.mkTupleSort( ctx.mkSymbol(selectName),
//							new Symbol[] {clause1.getRange().getName(), clause2.getRange().getName()},
//							new Sort[] {clause1.getRange(), clause2.getRange()} 
//							);
		TupleSort select = ctx.mkTupleSort(ctx.mkSymbol("selectCond"), new Symbol[] {ctx.mkSymbol("Select_Number"), ctx.mkSymbol("Select_points")}, new Sort[] {ctx.getIntSort(), ctx.getIntSort()});
		Expr expr = select.mkDecl().apply(clause1.apply(x0), clause2.apply(x0));
		
		return expr;
	}
	
	public BoolExpr createWhereCondExpr(Context ctx, Expr x0, TupleSort x0_sort) {
		
		FuncDecl clause1 = x0_sort.getFieldDecls()[1];
		FuncDecl clause2 = x0_sort.getFieldDecls()[2];
		BoolExpr expr1 = ctx.mkEq(clause1.apply(x0), ctx.mkInt(10));
		BoolExpr expr2 = ctx.mkGt((ArithExpr)clause2.apply(x0), ctx.mkInt(0));
		BoolExpr andCond = ctx.mkAnd(expr1, expr2);
		return andCond;
		
	}
	
	public static void main(String[] args) {
		selectClause p = new selectClause();
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		Context ctx = new Context(cfg);
		// Creating the student table
		int k = 3;
		
		///////////////////////////// CREATING TABLES
		// Creating the sorts that make up the tuplesort for primary key
		Sort columnSorts[];
		// TODO: Restrict size of string
		// TODO: Check for Nullable and non-nullable entries
		columnSorts = new Sort[] {ctx.getIntSort(), ctx.getStringSort()}; // How to restrict to size 100?
		
		//Creating the symbols which represents the elements of tuplesort for primary key
		Symbol columnSymbols[];
		columnSymbols = new Symbol[] {ctx.mkSymbol("Students.StudentNr"), ctx.mkSymbol("Students.StudentName")};
		
		// Creating the tuplesort for primary key
		TupleSort row = ctx.mkTupleSort(ctx.mkSymbol("Students" + "_rowSort"),
				columnSymbols,
				columnSorts
				);
		
		int noOfColumns = 2;
		int noOfRows = k;
		BoolExpr tableDecl = p.createTableExpr(ctx, row, columnSorts, noOfColumns, noOfRows, "Students");
		BoolExpr pkey = p.createPrimaryKeyExpr(ctx, row, noOfRows, columnSorts, noOfColumns, new int[] {0}, 1, "Students");

		
		BoolExpr StudentsTableCreation[] =  new BoolExpr[] { tableDecl, pkey};
		
		System.out.println("Table1 Creation finished");
		/////////////////////////////CREATING TABLE2////////////////////////
		
		
		int k1 = k, k2 = k;
		
		TupleSort foreignKey = ctx.mkTupleSort(ctx.mkSymbol("fkeySort"), new Symbol[] {ctx.mkSymbol("Students.StudentNr")}, new Sort[] {ctx.getIntSort()}); 
		
		int fkeyColumns2[] = new int[] {0};
		String table2Name = "Students";
		
		// Creating the sorts that make up the tuplesort for primary key
		Sort columnSorts2[];
		columnSorts2 = new Sort[] {ctx.getIntSort(), ctx.getIntSort(), ctx.getIntSort()};
		
		//Creating the symbols which represents the elements of tuplesort for primary key
		Symbol columnSymbols2[];
		columnSymbols2 = new Symbol[] {ctx.mkSymbol("Scores.StudentID"), ctx.mkSymbol("Scores.CourseID"), ctx.mkSymbol("Scores.Points")};
//		System.out.println("Table1 Creation finsihed");
		// Creating the tuplesort for primary key
		TupleSort row2 = ctx.mkTupleSort(ctx.mkSymbol("Scores" + "_rowSort"),
				columnSymbols2,
				columnSorts2
				);
//		System.out.println("Table1 Creation finsihed");
		int noOfColumns2 = 3;
		int noOfRows2 = k1;
		int fsize = 1;
		int fkeyColumns1[] = new int[] {0};
		Sort fkeyColumnSorts[] = new Sort[] {ctx.getIntSort()};
		BoolExpr tableDecl2 = p.createTableExpr(ctx, row2, columnSorts2, noOfColumns2, noOfRows2, "Scores");
		BoolExpr pkey2 = p.createPrimaryKeyExpr(ctx, row2, noOfRows, columnSorts2, noOfColumns2, new int[] {0, 1}, 2, "Scores");
		BoolExpr fkey = p.createForeignKeyExpr(ctx, k1, k2, foreignKey, fkeyColumnSorts, fkeyColumns1, fkeyColumns2, fsize, "Scores", table2Name);
		
		
		BoolExpr ScoresTableCreation[] = new BoolExpr[] { tableDecl2, pkey2, fkey};
		
		
		
		////////////////////////////////////TABLE CREATION END
		System.out.println("Table Creation finished");
		
		
		
		
		// Getting the model for the query q : SELECT StudentID, Points	FROM Scores WHERE Scores.CourseID = 10 AND Scores.Points > 0
		
		// [[q]] = Map[T(x.0, x.2)](Filter[(x.1 = 10) ∧ (x.2 > 0)](Scores))
		// To create [[q]], we need an expression for (x.1 = 10) ∧ (x.2 > 0) and T(x.0, x.2)
//		Symbol x00_name = ctx.mkSymbol("x00");
//		Expr x00 = ctx.mkConst(x00_name, row2);
//		BoolExpr f = p.createWhereCondExpr(ctx, x00, row2);
		
		Symbol x01_name = ctx.mkSymbol("x01");
		Expr x01 = ctx.mkConst(x01_name, row2);
		Expr g = p.createSelectTupleExpr(ctx, x01, row2, "selectCond");
		
		// Now creating the Filter for the where condition and using that and select condition creating the Map
//		BoolExpr FilterExprs[] = p.assertFilterAxiom(ctx, f, x00, x00_name, "ScoreWhereCondition", row2);
		
		TupleSort select = ctx.mkTupleSort(ctx.mkSymbol("selectCond"), new Symbol[] {ctx.mkSymbol("Select_Number"), ctx.mkSymbol("Select_points")}, new Sort[] {ctx.getIntSort(), ctx.getIntSort()});
		BoolExpr MapExprs[] = p.assertMapAxiom(ctx, g, x01, x01_name, select, "ScoreSelectCondition", row2);
		
		Solver solver = ctx.mkSolver();
		solver.push();
		for (BoolExpr a : StudentsTableCreation) {
			solver.add(a);
		}
		for (BoolExpr a : ScoresTableCreation) {
			solver.add(a);
		}
//		for (BoolExpr a : FilterExprs) {
//			solver.add(a);
//		}
		for (BoolExpr a : MapExprs) {
			solver.add(a);
		}
		System.out.println(solver);
		Model model = null;
        if (Status.SATISFIABLE == solver.check()){
        	System.out.println("Generating Model : ");
            model = solver.getModel();
            System.out.println("THE GENERATED MODEL : ");
            System.out.println(model);
        }
        else{
            System.out.println("BUG, the constraints are unsatisfiable.");
//            model = solver.getModel();
//            System.out.println(model);
        }
	}
	
}
