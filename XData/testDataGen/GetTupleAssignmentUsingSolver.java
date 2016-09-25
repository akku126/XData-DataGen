package testDataGen;

import generateConstraints.GetCVC3HeaderAndFooter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Column;
import parsing.Node;
import util.Configuration;
import util.JoinAndUniqueAttributes;
import util.JoinGraphNode;
import util.Utilities;

/**
 * This class uses constraint solver to get the tuple assignment for each relation
 * @author mahesh
 *
 */
public class GetTupleAssignmentUsingSolver {
	private static Logger logger = Logger.getLogger(GetTupleAssignmentUsingSolver.class.getName());
	/**Indicate maximum number of attributes in any input relation*/
	int maxAttributes;

	int noOfRelations;

	/**To indicate which relations are joined*/
	HashMap<String, String> relationsJoined;

	/**To store the list of possible bit vectors used to represent join relation ships and unique elements*/
	Set<String> bitVectors;

	/**To keep which relations are mapped to which CVC array*/
	HashMap<String, String> tableMapping;

	/**keep track of relations id*/
	HashMap<String, String> relationsIds;

	/**To keep track of joined attributes between two relations*/
	HashMap<String, HashMap<Integer, ArrayList<Integer> > > joinedAttributes;

	/**Keep track of joined bit string*/
	HashMap<String, String> joinedString;

	/**To indicate if the two relations are joined*/
	boolean [][] areJoined;

	/**stores the tuple assignment object*/
	GetTupleAssignment gta;

	/**To keep track of id*/
	char idChar; 

	String filePath;
	/**
	 * Constructor
	 */
	public GetTupleAssignmentUsingSolver(GetTupleAssignment gt, String filepath) {

		/**initialize various elements*/
		gta = gt;		
		relationsJoined = new HashMap<String, String>();
		bitVectors = new HashSet<String>();
		tableMapping = new HashMap<String, String>();
		relationsIds = new HashMap<String, String>();
		joinedAttributes = new HashMap<String, HashMap<Integer, ArrayList<Integer> > >();
		joinedString = new HashMap<String, String>();
		idChar = 65;	
		filePath = filepath;
		/**get maximum number of attributes across all the input relations*/
		maxAttributes = getMaximumAttrsRelation(gta.getJoinGraph().getAllVertex());
		noOfRelations = getGta().getJoinGraph().getAllVertex().size();

		areJoined = new boolean[getMaxAttributes()][ getMaxAttributes()];

		/**initialize this with false*/
		for(boolean[] arr : areJoined)
			Arrays.fill(arr, false);

		/**Update the data structures to store join relation ships*/
		getJoinRelationships();

	}

	public boolean getTupleAssignment() throws Exception{
		long startAssgn = System.currentTimeMillis();
		String constraints = "";

		constraints += "%DATA TYPES \n \n";
		/**get data types needed*/
		constraints += getDataTypes();		

		constraints += "%VARIABLE NAMES \n \n";
		/**get the variables needed*/
		constraints += getVariableTypes();		

		constraints += "%FUNCTION DEFINITIONS \n \n";
		/**get the function definitions*/
		constraints += getFunctionDataTypes();

		/**get the constraints related to the given join graph*/
		constraints += getConstraintsForGraph();		

		/**get the footer for the CVC solver*/
		constraints += GetCVC3HeaderAndFooter.generateCvc3_Footer();

		long endAssgn = System.currentTimeMillis();

		//System.out.println(constraints);



		logger.log(Level.INFO,"Constraint Generation Time: " + (endAssgn-startAssgn) );
		return solveConstraints(constraints);


	}


	/**
	 * Defines various types of variables needed
	 * @return
	 */
	private String getVariableTypes() {

		String variables = "";

		/**to store the 'finalCount'*/
		variables += "FC: ATTRIBUTES;" +"\n";

		/**to store the value '1'*/
		variables += "ONE: ATTRIBUTES;" +"\n";

		/**to store the number of tuples for each relation*/
		variables += "COUNT: COUNTARRAY;" +"\n";

		/**to store the value 0*/
		variables += "ZERO: ATTRIBUTES = " + Utilities.getBinVal(0, getMaxAttributes()) +";\n";

		/**to store integer values for bitvector*/
		variables += "INTS: BITSINT; \n\n\n";

		return variables;
	}






	/**
	 * Get different function definitions used for tuple assignment
	 * @return
	 */
	private String getFunctionDataTypes() {

		String functionType = "";

		/**get definition for count multiplication*/
		functionType += getMulFunctioon();

		/**get function definition to indicate unique elements*/
		functionType += getUniqueFunction();

		/**get function definition to indicate join relaionship*/
		functionType += getJoinFunctions();

		/**get function definitions related to bits*/
		//functionType += getBitVectorFunctions();

		/**get function definition used during attribute inferencing*/
		//functionType += getUniqueInferFunction();


		return functionType;
	}


	/**
	 * Get function to indicate that the join of two relations must give finalCount
	 * @return
	 */
	private String getMulFunctioon() {

		return "MUL: (ATTRIBUTES,ATTRIBUTES) -> ATTRIBUTES =  LAMBDA (x,y : ATTRIBUTES):"+ "\n" + 
				"     IF (x = FC AND y = FC) THEN FC ELSE BVMULT( "+getMaxAttributes()+", x, y)" + "\n" +
				"     ENDIF; "+"\n\n" ;

	}


	/**
	 * Get definition of the function to indicate which set of attributes of the relation are unique 
	 * @return
	 */
	private String getUniqueFunction() {

		/**ARR indicate relation name and 
		 * 1s in Bit vector denote attribute position*/
		return "UNIQUE: (RELATIONS, ATTRIBUTES) -> BOOLEAN;\n\n";
	}


	private String getJoinFunctions() {

		String join = "";

		/** function to indicate join conditions between two relations*/
		join += getJoinAttributeFunction();

		/**function to indicate if there is join between two relations*/
		join += getJoinRelationsFunction();

		return join;
	}


	/**
	 * This function gives definition to indicate that two relations are joined
	 * It also stores which attribute is joined with which attribute
	 * @return
	 */
	private String getJoinAttributeFunction() {

		/**Example: if A.1=B.2 and A.2=B.3 , then
		 * we use another array where each element indicate the attribute on other side 
		 * i.e. ISJOIN(A,B,C,011) where C=[2,3]. 
		 * This is helpful in inferring unique elements*/

		//return "ISJOIN: (RELATIONS, RELATIONS, UNIQUEMAPPING, ATTRIBUTES) -> BOOLEAN;\n\n";
		return "ISJOIN: (RELATIONS, RELATIONS, ATTRIBUTES) -> BOOLEAN;\n\n";
	}


	/**
	 * This function give definition to indicate whether two relations are joined or not
	 * If they are joined, in which array the join conditions are stored  
	 */
	private String getJoinRelationsFunction() {

		return "JOINEDRELATIONS: (RELATIONS, RELATIONS, UNIQUEMAPPING) -> BOOLEAN;\n\n";
	}


	/**
	 * This method is used to get different CVC data types 
	 * needed for tuple assignment
	 */
	private String getDataTypes() {

		String dataType = "";

		/**get bit vector type*/
		dataType += getBitVector();

		/**get array type to indicate different possible values of bit vectors*/
		dataType += getArrayBitvector();

		/**get array type for relations*/
		dataType += getRelationType();

		/**get the input type for storing number of tuples for each relation*/
		dataType += getCountArrayType();

		/** get the input type to store conversions from bitvector to integer*/
		dataType += getConversionArray();

		/**get join array type*/
		dataType += getJoinArray();

		return  dataType + "\n\n";
	}

	private String getJoinArray() {

		return "UNIQUEMAPPING: TYPE = ARRAY INT OF ATTRIBUTES;\n";
	}

	/**
	 * Get data type to store conversions from bitvector to integer
	 * @return
	 */
	private String getConversionArray() {

		return "BITSINT: TYPE = ARRAY ATTRIBUTES OF INT;\n";
	}

	/**
	 * Get data type for count array
	 * Count array is indexed by relation names
	 * @return
	 */
	private String getCountArrayType() {

		return "COUNTARRAY: TYPE = ARRAY RELATIONS OF ATTRIBUTES;\n";
	}

	/**
	 * Gets array type to indicate the relations in the given input
	 * @return
	 */
	private String getRelationType() {

		//return "RELATIONS: TYPE = INT;\n";

		return "RELATIONS: TYPE = BITVECTOR("+ (int)Math.ceil(Math.log(getNoOfRelations())/ Math.log(2))+");\n";

		//return "RELATIONS: TYPE = SUBTYPE (LAMBDA (x: INT) : (x > -1 AND x < "+getNoOfRelations()+"));\n";

	}

	/**
	 * Get array of bits type
	 * Helpful to indicate the possible bitvectors in the given input
	 * @return
	 */
	private String getArrayBitvector() {

		return "ARRATTRIBUTES: TYPE = ARRAY INT OF ATTRIBUTES;\n";
	}

	/**
	 * Gets the bitvector type
	 * @return
	 */
	private String getBitVector() {

		return "ATTRIBUTES: TYPE = BITVECTOR("+getMaxAttributes()+");\n";

	}

	/**
	 * Adds constraints for different tuple assignment rules
	 * @param gta
	 * @return
	 */
	private String getTupleAssignmentRules() {

		String rules = "";

		rules += "%RULE 7 \n\n";
		/**get the constraint for rule 7*/
		rules += getRule7();


		/**get the constraint for rule 8*/
		rules += getRule8();

		return rules;		
	}

	/**
	 * Create constraint for rule 7
	 * Rule 7: If there are any join attributes of edge from relation 'A' to 'B', which form unique element 'u' and number of tuples of 'A' is 'finalCount', then
	 * number of tuples of 'B' becomes 'finalCount' and the attributes of 'B', which have join relationship with unique element 'u', become unique.
	 * @return
	 */
	private String getRule7() {

		String rule7 = "";

		/**for each set of relations (a,b,c)*/
		rule7 += "ASSERT FORALL (a, b : RELATIONS, c: UNIQUEMAPPING): "  + "\n";

		/**if (a,b) are joined and the join conditions are stored in c*/
		rule7 += "    IF JOINEDRELATIONS(a, b, c) THEN "  + "\n";

		/**for each bit vector*/
		rule7 += "        FORALL (d: ARRATTRIBUTES): "  + "\n";

		/**if this bit vector indicates the join conditions between (a,b)
		 * and the number of tuples of relation 'a' is FC */
		//rule7 += "           IF ISJOIN(a,b,c,d[0]) AND COUNT[a] = FC THEN "  + "\n";
		rule7 += "           IF ISJOIN(a,b,d[0]) AND COUNT[a] = FC THEN "  + "\n";

		/**for all bit vectors*/
		rule7 += "                FORALL (e: ATTRIBUTES): "  + "\n";	

		/**if the bit vector is unique element in relation 'a'
		 * the subset of join conditions form unique element*/
		rule7 += "                     IF UNIQUE(a, e)  AND NOT e = ZERO AND d[0]&e = e THEN "  + "\n";


		/**the number of tuples on relation 'b' becomes 'FC'
		 * the respective attributes of relation 'b' becomes unique*/
		rule7 += "                            COUNT[b] = FC AND UNIQUE(b, c[ INTS[ e ] ] ) "  + "\n";

		/**completing the if else chain*/
		rule7 += "                     ELSE "  + "\n";
		rule7 += "                            "+getTrueStatement()  + "\n";
		rule7 += "                     ENDIF"  + "\n";
		rule7 += "           ELSE "  + "\n";
		rule7 += "                 "+ getTrueStatement()  + "\n";
		rule7 += "           ENDIF"  + "\n";
		rule7 += "    ELSE "  + "\n";
		rule7 += "         "+getTrueStatement()  + "\n";
		rule7 += "    ENDIF;"  + "\n";


		return rule7;
	}


	/**
	 * This method generates various constraints related to the join given join graph
	 * @return
	 */
	private String getConstraintsForGraph() {

		String graphConstraints = "";

		graphConstraints += "%FINAL COUNT CONSTRAINTS \n\n";

		/**the constraint for final count*/
		graphConstraints += getCountConstraint();		

		graphConstraints += "%JOIN RELATIONSHIP CONSTRAINTS \n\n";

		/**get the constraints for join conditions*/
		graphConstraints += getJoinCondConstraints();

		graphConstraints += "%JOIN OF RELATIONS SHOULD OUTPUT 'FC' TUPLES \n\n";

		/**the join of input relations must give 'finalCount' tuples*/
		graphConstraints += getJoinCountConstraint();

		graphConstraints += "%FINAL COUNT OF RELATIONS CONSTRAINTS \n\n";

		/**get the constraint that the number of tuples in each relation can be 'FC' or 1*/
		graphConstraints += getCountConstraintForRelation();

		graphConstraints += "%RELATIONS WITH RESTIRCTED CARDINALITY \n\n";

		/**Get the constraint for all the relations that should contain only a single tuple*/
		graphConstraints += getRestrictedRelations(); 

		graphConstraints += "%UNIQUE KEY CONSTRAINTS \n";

		/**get unique key constraints*/
		graphConstraints += getUniqueConstraints();			

		graphConstraints += "%SINGLE VALUED CONSTRAINTS \n\n";

		/**get the constraints for bit vectors that should not represent unique key used*/
		graphConstraints += getSingleValuedConstraints();

		/**get the tuple assignment rules*/
		graphConstraints += getTupleAssignmentRules();

		graphConstraints += "%RELATION IDS \n\n";
		/**get the rules related to relation ids*/
		graphConstraints += getRelationIdConstraints();

		graphConstraints += "%LIST OF BITVECTORS USED \n\n";

		/**get the constraints for bit vectors used*/
		graphConstraints += getBitsConstraints();

		/**get the constraints for unique elements across all the join edges*/
		graphConstraints += getUniqueElemetsAcrossJoins();

		graphConstraints += "%BITS TO INT CONVERSION \n\n";

		/**get the constraints for bit vectors used*/
		graphConstraints += getIntsConversionArray();

		graphConstraints += "%NOT UNIQUE CONSTRAINTS \n\n";

		/**get the constraints for bit vectors that should not represent unique key used*/
		graphConstraints += getNotUniqueConstraints();

		/**get not join constraints*/
		graphConstraints += getJoinConstraints();


		return graphConstraints;
	}



	private String getRelationIdConstraints() {

		String constraint = ""; 

		int i =0;

		for(String rel: getTableMapping().values()){

			String bin = Utilities.getBinVal(i, (int)Math.ceil(Math.log(getNoOfRelations())/ Math.log(2)));
			//constraint += "ASSERT "+ rel +" = " + i +";\n";
			//getRelationsIds().put(rel, i);
			constraint += "ASSERT "+ rel +" = " +  bin +";\n";
			getRelationsIds().put(bin, rel);
			i++;
		}
		return constraint + "\n\n";
	}

	/**
	 * Generates constraints that the single valued attribute should not be unique, if it was not already inferred to be unique
	 * @return
	 */
	private String getSingleValuedConstraints() {

		String singleValued = "";

		HashMap<String, ArrayList<Node>> listAttrs = new HashMap<String, ArrayList<Node>>();

		/**segregate the single valued attributes for each relation*/
		for(Node sv: getGta().getSingleValuedAttributes()){

			if( listAttrs.containsKey( sv.getTableNameNo()))
				listAttrs.get( sv.getTableNameNo()).add(sv);
			else{
				listAttrs.put( sv.getTableNameNo(), new ArrayList<Node>());
				listAttrs.get( sv.getTableNameNo()).add(sv);
			}
		}

		for(String relation: listAttrs.keySet()){

			ArrayList<Node> list = listAttrs.get(relation);

			for(int i = 1; i <= list.size(); i++){

				/**get the subsets of given size*/
				List<Set<Integer>> res = getSubsets( list.size(), i);

				for(Set<Integer> set: res){

					HashSet<Node> single = new HashSet<Node>();

					/**get the single valued attributes */
					for(Integer j: set)
						single.add( list.get(j-1));

					boolean present = false;
					/**check if this is not in unique list*/
					for(HashSet<Node> unq: getGta().getUniqueElementsDup()){
						present = single.containsAll(unq);
						if( present )
							break;
					}

					if( ! present){



						int num = 0;

						for(Node n: single)
							num = num | ( 1 << n.getTable().getColumnIndex( n.getColumn().getColumnName()));

						if(getTableMapping().get(relation) != null) singleValued += "ASSERT NOT UNIQUE(" + getTableMapping().get(relation) + ", " + Utilities.getBinVal(num, getMaxAttributes()) + ");\n";
					}
				}
			}
		}

		return singleValued + "\n\n";
	}



	/**
	 * Generates constraints for bitvectors which should not represent unique constraints
	 * @return
	 */
	private String getNotUniqueConstraints() {

		String notUnique = "";


		/**If the size of bitvector is more than the number of attributes of the relation then the extra bit vectors should not represent unique bit vector*/
		for(String relationNo: getTableMapping().keySet()){			

			/**check if this table has less number of columns*/
			for(JoinGraphNode jgn: getGta().getJoinGraph().getAllVertex())
				if(jgn.getTableNameNo().equalsIgnoreCase(relationNo) && jgn.getTable().getColumnIndexList().size() < getMaxAttributes()){

					int size = jgn.getTable().getColumnIndexList().size();

					int remSize = getMaxAttributes() - size;

					String constraint = "";

					for(int i = 1; i < (int)Math.pow(2, remSize); i++)
						for(int j = 0; j < (int)Math.pow(2, size); j++)
							constraint += "ASSERT NOT UNIQUE(" + getTableMapping().get(relationNo) + ", " + Utilities.getBinVal( (i << size) + j, getMaxAttributes()) + ");\n";

					/*if( constraint.length() > 0)
						constraint = "ASSERT "+constraint.substring(0, constraint.lastIndexOf("AND")) + ";\n\n";*/

					notUnique += constraint;

				}

		}
		return notUnique + "\n\n\n";
	}

	/**
	 * This method gets the array that stores ints to bits conversion
	 * @return
	 */
	private String getIntsConversionArray() {

		String bits = "";

		double max = Math.pow(2, getMaxAttributes());

		for(long i = 0; i< max; i++)
			bits += "INTS[" + Utilities.getBinVal(i, getMaxAttributes()) + "] = " + i + " AND ";

		if(bits.length() > 0) bits = "ASSERT " + bits.substring(0, bits.lastIndexOf("AND")) + "; \n\n\n";

		return bits;
	}

	/**
	 * This method gets constraints for the relation whose count has to be constrained to 1
	 * @return
	 */
	private String getRestrictedRelations() {

		String constraint = "";

		/**for each relation in the join graph*/
		for(JoinGraphNode vertex : getGta().getJoinGraph().getAllVertex())

			/**if its count is restricted */
			if( vertex.getmaximumPossibleCardinality() != 100000) /**FIXME: If value 100000 changed in JoinGraphNode then change here*/

				constraint += "ASSERT COUNT[" + getTableMapping().get(vertex.getTableNameNo()) + "] = ONE;\n";

		return constraint + "\n\n";
	}
	/**
	 * Specify that each relation can contain 'n' or '1' tuple
	 * @return
	 */
	private String getCountConstraintForRelation() {

		String count = "";

		for(String relation: getTableMapping().values())
			count += "ASSERT COUNT[" + relation + "] = FC OR " + "COUNT[" + relation + "] = ONE;\n";

		return count + "\n\n";
	}
	/**
	 * Get the constraint to specify the value of final count
	 * @return
	 */
	private String getCountConstraint() {

		String count = "";

		/**ensure that FC is equal to required number of tuples*/
		count += "ASSERT FC = " + Utilities.getBinVal(getGta().getFinalCount(), getMaxAttributes()) +";\n";

		/**constraint for 1*/
		count += "ASSERT ONE = " + Utilities.getBinVal(1, getMaxAttributes()) +";\n";

		return count + "\n";
	}


	/**
	 * Get the constraint that specifies the the join of input relations should give 'finalCount' as output
	 * @return
	 */
	private String getJoinCountConstraint() {

		String joinCount = "";

		Iterator<String> it = getTableMapping().values().iterator();

		/**if there is only one relation*/
		if( getTableMapping().values().size() == 1)
			joinCount += "ASSERT COUNT["+it.next()+"] = FC;\n";

		else if ( getTableMapping().values().size() > 1){

			/**get constraint for two relations*/
			joinCount += "MUL(COUNT["+it.next()+"], COUNT["+it.next()+"])";	

			while(it.hasNext())
				joinCount = "MUL(COUNT["+it.next()+"]," + joinCount +")";

			joinCount = "ASSERT "+ joinCount +" = FC;\n";
		}

		return joinCount + "\n\n";
	}


	/**
	 * Get the unique key constraints for each set of attributes which form unique element
	 * @return
	 */
	private String getUniqueConstraints() {

		String unique = "";


		/**for each unique element*/
		for( HashSet<Node> unq: getGta().getUniqueElementsDup()){

			if(unq.size() == 0)continue;

			int num = 0;

			String table = "";

			/**for each node, get index of the node in the relation and set that bit to 1*/
			for(Node u: unq){

				table =  getTableMapping().get( u.getTableNameNo());
				num = num | (1 << u.getTable().getColumnIndex( u.getColumn().getColumnName()));
			}

			String bit = Utilities.getBinVal(num, getMaxAttributes());
			unique += "ASSERT UNIQUE("+table+", " + bit + ");\n";	

			/**add this bit string to the list of bitvectors*/
			getBitVectors().add(bit);
		}

		return unique + "\n\n";
	}


	/**
	 * Get the constraints that a set of relations are involved in joins
	 * @return
	 */
	private String getJoinCondConstraints() {

		String joinRelation = "";



		/**for each relation that is mapped*/
		for(String relation1 : getTableMapping().values()){

			for(String relation2 : getTableMapping().values()){

				/**if there is join relationship between these two relations*/
				if( getAreJoined()[relation1.charAt(0) - 65][relation2.charAt(0) - 65]){

					/**get the array of join attributes between these two relations*/
					HashMap<Integer, ArrayList<Integer> >  joinAttrs = getJoinedAttributes().get(relation1 + relation2 + "");

					String joinBinary = "0bin";

					for(int i = getMaxAttributes() ; i>= 1; i--){

						ArrayList<Integer> joins = joinAttrs.get(i);

						joinBinary += (joins != null && joins.get(0) != null)?"1":"0";
					}

					/**get join constraint*/
					//joinRelation += "ASSERT ISJOIN("+ relation1 +", " + relation2 +", " + relation1 + relation2 +", " + joinBinary +"); \n";
					joinRelation += "ASSERT ISJOIN("+ relation1 +", " + relation2 +", " + joinBinary +"); \n";

					/**stores the possible bit vector*/
					getBitVectors().add(joinBinary);

					/**update the join string*/
					getJoinedString().put(relation1 + relation2, joinBinary);


				}
			}
		}

		/**get array types for each relation*/


		return getRelationNames() + "\n\n" + joinRelation + "\n\n";
	}

	/**
	 * Get the constraint related to names of all the relations
	 * @return
	 */
	private String getRelationNames(){

		String relation = "";


		for(String rel: getTableMapping().values())
			relation += rel+", ";

		if(relation.length() > 0) relation = relation.substring(0, relation.lastIndexOf(",")) + ": RELATIONS;\n";

		for(String rel: getJoinedAttributes().keySet())
			relation += rel+", ";

		if(relation.length() > 0) relation = relation.substring(0, relation.lastIndexOf(",")) + ": UNIQUEMAPPING;\n";

		return relation;
	}
	private void getJoinRelationships() {


		/**get list of join conditions*/
		ArrayList<Node> joinConds = getGta().getJoinConds();

		/**get join constraint for each join condition*/
		for(Node jc: joinConds){

			/**check for left side table name*/
			checkIfAlreadyMapped( jc.getLeft().getTableNameNo() );

			/**check for right side table name*/
			checkIfAlreadyMapped(jc.getRight().getTableNameNo() );

			/**get index of left and right columns*/
			int leftIndex = jc.getLeft().getTable().getColumnIndex(jc.getLeft().getColumn().getColumnName());
			int rightIndex = jc.getRight().getTable().getColumnIndex(jc.getRight().getColumn().getColumnName());

			String left = getTableMapping().get( jc.getLeft().getTableNameNo() );
			String right = getTableMapping().get( jc.getRight().getTableNameNo() );

			/**if we already had found join condition between these two relations*/
			if( getJoinedAttributes().keySet().contains( left + right) ){

				HashMap<Integer, ArrayList<Integer> > leftMap = getJoinedAttributes().get(left + right + "");
				HashMap<Integer, ArrayList<Integer> > rightMap = getJoinedAttributes().get(right + left + "");


				/**add this join attribute*/
				if( leftMap.keySet().contains( leftIndex + 1))
					leftMap.get(leftIndex + 1).add(rightIndex + 1);
				else{
					ArrayList<Integer> list = new ArrayList<Integer>();
					list.add(rightIndex + 1);
					leftMap.put(leftIndex + 1, list);
				}


				if( rightMap.keySet().contains( rightIndex + 1))
					rightMap.get(rightIndex + 1).add(leftIndex + 1);
				else{
					ArrayList<Integer> list = new ArrayList<Integer>();
					list.add(leftIndex + 1);
					rightMap.put(rightIndex + 1, list);
				}
			}
			else{/**The join conditions between these relations is not seen so far*/


				ArrayList<Integer>  leftJoin = new ArrayList<Integer>();
				ArrayList<Integer> rightJoin = new ArrayList<Integer>();

				leftJoin.add(rightIndex + 1);
				rightJoin.add(leftIndex + 1);

				HashMap<Integer, ArrayList<Integer> > leftMap = new HashMap<Integer, ArrayList<Integer> >();
				HashMap<Integer, ArrayList<Integer> > rightMap = new HashMap<Integer, ArrayList<Integer> >();


				leftMap.put(leftIndex + 1, leftJoin);
				rightMap.put(rightIndex + 1, rightJoin);

				/**add this to the list*/
				getJoinedAttributes().put(left + right + "", leftMap);
				getJoinedAttributes().put(right + left + "", rightMap);

				/**update the join relationship between these two relations*/ 
				getAreJoined()[left.charAt(0) - 65][right.charAt(0) - 65] = true;
				getAreJoined()[right.charAt(0) - 65][left.charAt(0) - 65] = true;
			}

		}
	}

	private char checkIfAlreadyMapped( String table) {
		/**if left table already not mapped to CVC array*/
		if( !getTableMapping().keySet().contains( table) ){

			/**keep mapping for this relation*/
			getTableMapping().put( table, idChar + "");

			idChar ++;
		}
		return idChar;
	}


	/**
	 * Get the constraints that a set of relations are not involved in joins
	 * @return
	 */
	private String getJoinConstraints() {

		String notJoin = "", join = "", notIsJoin = "";

		/**to store relation names*/
		ArrayList<String> relationsList = new ArrayList<String>();

		/**to store joined relation names*/
		ArrayList<String> joinedList = new ArrayList<String>();

		/**for each relation that is mapped*/
		for(String relation1 : getTableMapping().values())
			relationsList.add(relation1);

		/**add all array names that store join conditions*/
		joinedList.addAll( getJoinedAttributes().keySet());

		/**for each relation that is mapped*/
		for(String relation1 : relationsList){		

			for(String relation2 : relationsList){

				/**for each array name that stores the join conditions*/
				for(String joinRelation: joinedList){

					/**if there is join relationship between these two relations*/
					if( getAreJoined()[relation1.charAt(0) - 65][relation2.charAt(0) - 65] &&
							(relation1 + relation2).equalsIgnoreCase(joinRelation)){

						join += "ASSERT JOINEDRELATIONS("+ relation1 +", " + relation2 +", " + joinRelation + "); \n";

						notIsJoin += getNotIsJoinConsraint(relation1, relation2);
					}
					else
						notJoin += "ASSERT NOT JOINEDRELATIONS("+ relation1 +", " + relation2 +", " + joinRelation + "); \n";

				}
				notJoin += "\n";
			}
		}		

		return "%JOINED RELATIONS ALONG WITH THE ARRAY WHICH STORES THEIR CONDITIONS ARE STORED \n\n"+
		join + "\n\n" + 
		"%NOT JOINED CONSTRAINTS\n\n" + notIsJoin + "\n\n"+ notJoin + "\n\n" ;
	}

	/**
	 * Get the constraint that a particular bit vector does not indicate join relation between the given two relation
	 * @param relation1
	 * @param relation2
	 * @return
	 */
	private String getNotIsJoinConsraint(String relation1, String relation2) {

		String notIsJoin = "";

		for(String bitString: getBitVectors())
			if( bitString.equalsIgnoreCase( getJoinedString().get( relation1 + relation2)) == false)
				//notIsJoin += "ASSERT NOT ISJOIN(" + relation1 + ", " + relation2 + "," + relation1 + relation2 + ", " + bitString + ");\n";
				notIsJoin += "ASSERT NOT ISJOIN(" + relation1 + ", " + relation2 + ", " + bitString + ");\n";

		return notIsJoin + "\n";
	}
	/**
	 * Used to get constraints for different possible bit vectors used to represent join relationships and unique elements
	 * @return
	 */
	private String getBitsConstraints() {

		String bitConstraint = "", bitType = "";

		for(String bit: getBitVectors()){

			/**get array variable*/
			bitType += idChar + ", ";

			bitConstraint += idChar+"[0] = " + bit +" AND ";
			idChar++;			

		}

		if(bitType.length() > 0) bitType = bitType.substring(0, bitType.lastIndexOf(",")) + " : ARRATTRIBUTES;\n";

		if(bitConstraint.length() > 0) bitConstraint = "ASSERT " + bitConstraint.substring(0,bitConstraint.lastIndexOf("AND")) + ";\n";


		return bitType + "\n\n" + bitConstraint + "\n\n";
	}

	/**
	 * A statement to indicate some thing that is always true
	 * @return
	 */
	private String getTrueStatement() {

		return "1 = 1";
	}


	/**
	 * Gets the constraint for rule 8 described below
	 *This rule is applied for the node when its actual cardinality is 'finalCount', 
	 * To apply this method we use all the edges of the node. The following is description of the rule
	 *  
	 *  1>> If there is any multi-attribute unique element and
	 *  2>> If 'r' proper and exhaustive subsets of this multi-attribute unique element are involved in joins with 'r' different relations
	 *  3>> then atleast one of this 'r' proper subsets must be unique (let say this edge is from 'jgn' to 'jgn2')
	 *  
	 * Approach for getting CVC constraint:
	 *   1>>For each relation, if this relation is involved in joins with more than one relation, hard code the rule at that relation
	 * @return
	 */
	private String getRule8() {

		String rule8 = "";

		/**for each node in the join graph*/
		for(JoinGraphNode jgn: getGta().getJoinGraph().getAllVertex()){

			/**get edges of this 'jgn'*/
			Map< JoinGraphNode, Vector< JoinAndUniqueAttributes > > edges = getGta().getJoinGraph().getNeighbours(jgn);

			/**if this relation is involved in multiple joins*/
			if( edges.size() > 1){

				String joinKey = getTableMapping().get(jgn.getTableNameNo());

				String constraint = "";
				String condition = "";
				/**for each relation with which have join with 'jgn'*/
				for(JoinGraphNode joined: edges.keySet()){

					String key = getTableMapping().get( joined.getTableNameNo());

					String joinString = getJoinedString().get( joinKey + key);

					/**get the constraint for this join*/
					constraint += "        (IF NOT x & " + joinString + " = ZERO THEN " + "\n";

					condition += " NOT x & " + joinString + " = ZERO OR ";

					/*constraint += "            COUNT[" + key + "] = FC AND UNIQUE(" + key + ", GETUNIQUE( "+
							joinKey+key+", x[0] & " + joinString + ") )\n";*/

					constraint += "            COUNT[" + key + "] = FC AND UNIQUE(" + key + ", "+ joinKey + key +  " [ INTS[ x & "+joinString+" ] ]  )\n";

					constraint += "         ELSE \n            NOT " + getTrueStatement() + "\n        ENDIF)\n";

					constraint += "        OR \n";
				}

				if( constraint.length() > 0){
					constraint = constraint.substring(0, constraint.lastIndexOf("OR"));

					condition = condition.substring(0, condition.lastIndexOf("OR"));

					String totConstraint = "ASSERT FORALL (x: ATTRIBUTES):\n";

					totConstraint += "    IF COUNT[" + joinKey +"] = FC AND UNIQUE(" + joinKey + ", x ) AND ( "+condition+" ) THEN \n" ;

					totConstraint += constraint;

					totConstraint += "\n    ELSE \n        " + getTrueStatement() + "\n     ENDIF;";

					rule8 += "\n\n%RULE 8 AT RELATION " + joinKey + "\n\n";

					rule8 += totConstraint + "\n\n";
				}
			}	
		}

		return "\n\n"+rule8 + "\n\n";
	}


	/**
	 * This returns constraints for inferring unique elemnts across join edges
	 * @return
	 */
	private String getUniqueElemetsAcrossJoins() {

		String unique = "";

		/**for each joined edge*/
		for(String joinRelation : getJoinedAttributes().keySet()){

			String constraint = "";
			/**get the join relationships*/
			HashMap<Integer, ArrayList<Integer>> joins = getJoinedAttributes().get(joinRelation);

			/**get the list of attributes involved in joins */
			Set keySet = joins.keySet();

			/**get all the subsets of this set*/
			List<Set<Integer>> res;
			for(int k = 1 ; k<= keySet.size(); k++){

				res = new ArrayList<Set<Integer>>();
				getSubsets( new ArrayList<Integer>( keySet ) , k, 0, new HashSet<Integer>(), res);

				/**for each subset get the bits set to 1*/
				for(Set<Integer> s: res){
					int key = 0, value = 0;

					for(Integer i : s){

						/**get the integer key value*/
						key = key | (1 << i-1 );

						/**get the binary string for corresponding joins*/
						for(int j: joins.get(i))
							value = value | ( 1 << j - 1);
					}

					constraint += joinRelation + "[" + key + "] = " + Utilities.getBinVal(value, getMaxAttributes()) + " AND ";
				}
			}

			if(constraint.length() > 0)
				constraint = "ASSERT "+constraint.substring(0, constraint.lastIndexOf("AND")) + ";\n\n";

			unique += constraint;
		}
		return unique;
	}




	/**
	 * Solves the given constraint and checks if solution existed or not
	 * @param constraints
	 * @return
	 */
	private boolean solveConstraints(String constraints) throws Exception{

		long startT, endT;

		/**write these constraints to a cvc file*/
		WriteFileAndUploadDatasets.writeFile(Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/tupleAssignment.cvc", constraints);

		String cmdString = "";

		cmdString = "#!/bin/bash\n";

		/**command to redirect output*/
		cmdString += Configuration.smtsolver+" "+ Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/tupleAssignment.cvc > tupleAssignmentOutput \n";

		/**command to check if solution is valid/ not i.e. possible or not*/
		//cmdString += Configuration.smtsolver+" "+ Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/tupleAssignment.cvc | grep -e 'Valid' > isValid \n";

		/**remove previous file*/
		cmdString += "rm "+Configuration.homeDir+"/temp_cvc"+ getFilePath() + "/assignment\n";

		/**command to check if solution exist or not*/
		cmdString += "grep -e 'Valid' "+ Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/tupleAssignmentOutput  > isValid \n";

		/**command to get tuple assignment (count) to each relation*/
		cmdString += "grep -e 'ASSERT (COUNT\\[' "+ Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/tupleAssignmentOutput >> "+
				Configuration.homeDir+"/temp_cvc"+ getFilePath() + "/assignment\n";

		/**command to get unique elements inferred*/
		cmdString += "grep -e 'ASSERT UNIQUE(' "+ Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/tupleAssignmentOutput >> "+
				Configuration.homeDir+"/temp_cvc"+ getFilePath() + "/assignment\n";

		/**write these command to a file*/
		WriteFileAndUploadDatasets.writeFile(Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/execTupleAssgnCVC", cmdString);

		startT = System.currentTimeMillis();
		/**execute these commands using process*/
		ProcessBuilder pb = new ProcessBuilder("/bin/bash", "execTupleAssgnCVC");

		pb.directory(new File(Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/"));

		Process myProcess = pb.start();
		int exitVal = myProcess.waitFor();

		endT = System.currentTimeMillis();
		logger.log(Level.INFO,"Constraint Solving Time: " + (endT-startT) );

		/**check if is success or not*/
		BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/isValid"));
		String line = input.readLine();

		input.close();
		
		Utilities.closeProcessStreams(myProcess);

		if( line != null )
			return false;

		updateJoinGraph();

		return true;
	}

	/**
	 * Assigns the tuple assignment done to base relations
	 */
	private void updateJoinGraph() throws Exception {

		BufferedReader input =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+ getFilePath() +"/assignment"));
		String line = "";

		getGta().setUniqueElements( new HashSet<HashSet<Node>>() );

		/**while there is input line*/
		while (( line = input.readLine()) != null){

			logger.log(Level.INFO,"LINE: " + line);

			String key, value;



			/**check if it is related to count value or unique value
			 * depending on this get key and binary string*/
			if(line.contains("COUNT")){
				key = line.substring(line.indexOf("[")+1, line.indexOf(']'));
				value = line.substring(line.indexOf("= 0bin")+6, line.indexOf(')'));
			}
			else{
				key = line.substring(line.indexOf("(")+1, line.indexOf(','));
				value = line.substring(line.indexOf(", 0bin")+6, line.indexOf(')'));
			}

			logger.log(Level.INFO,""+key+": " +value);
			int ival = Integer.parseInt(value,2);

			String tableNameNo = getRelation(getRelationsIds().get(key));


			/**update this count into the join graph node*/
			for(JoinGraphNode jn : getGta().getJoinGraph().getAllVertex())
				if(tableNameNo.equalsIgnoreCase( jn.getTableNameNo()))

					/**update this count into the join graph node, if it is count*/
					if( line.contains("COUNT") )
						jn.setCardinality(ival);

					else /**get the attributes which are set to 1 and add to unique list*/
						getGta().getUniqueElements().add( getUniqueElement(jn, value));

			/*if(line.contains("COUNT")){

				key = line.substring(line.indexOf("[")+1, line.indexOf(']'));

				System.out.println("KEY: " + key + " VALUE: " + value);

				int ival = Integer.parseInt(value,2);

				String tableNameNo = getRelation(key);

			 *//**update this count into the join graph node*//*
				for(JoinGraphNode jn : getGta().getJoinGraph().getAllVertex())
					if(tableNameNo.equalsIgnoreCase( jn.getTableNameNo()))
						jn.setCardinality(ival);

			}
			else {

				key = line.substring(line.indexOf("(")+1, line.indexOf(','));

				System.out.println("KEY: " + key + " VALUE: " + value);

				String tableNameNo = getRelation(key);

			  *//**get the attributes which are set to 1 *//*
				for(JoinGraphNode jn : getGta().getJoinGraph().getAllVertex())
					if(tableNameNo.equalsIgnoreCase( jn.getTableNameNo()))
						getGta().getUniqueElements().add( getUniqueElement(jn, value));
			}	*/

		}

		input.close();
	}

	/**
	 * This method creates unique elements for the given binary string and relation
	 * @param tableNameNo
	 * @param value
	 * @return
	 */
	private HashSet<Node> getUniqueElement(JoinGraphNode table, String value) throws Exception{

		HashSet<Node> unique = new HashSet<Node>();

		/**check if unique element has more attributes than that present in the relation*/
		if( checkIfMoreBitsSet(table, value) )
			return unique;
		/**for each string index*/
		for(int i = value.length() - 1 ; i >= 0 ; i--){

			if( value.charAt(i) == '1'){
				/**get column at this index*/
				Column col = table.getTable().getColumn(value.length() - 1- i);
				Node n = Node.createNode(col, table.getTable());
				n.setQueryIndex( table.getQueryIndex());
				n.setQueryType(table.getQueryType());
				n.setTableNameNo(table.getTableNameNo());
				unique.add(n);
			}
		}
		return unique;
	}

	/**
	 * Checks if the given string has bits set to 1 in the extra positions
	 * @param table
	 * @param value
	 * @return
	 */
	private boolean checkIfMoreBitsSet(JoinGraphNode table, String value) {

		int diff = getMaxAttributes() - table.getTable().getColumnIndexList().size() ;		

		for( int i = 0 ; i < diff; i++)
			if( value.charAt(i) == '1')
				return true;

		return false;
	}

	private String getRelation(String mapName){


		for(Map.Entry<String, String> entry : getTableMapping().entrySet())
			if (mapName.equals(entry.getValue()))
				return entry.getKey();

		return "";
	}
	/**
	 * get all subsets of given size
	 * @param superSet
	 * @param k
	 * @param idx
	 * @param current
	 * @param solution
	 */
	private static void getSubsets(List<Integer> superSet, int k, int idx, Set<Integer> current,List<Set<Integer>> solution) {

		/**successful stop clause*/
		if (current.size() == k) {
			solution.add(new HashSet(current));
			return;
		}

		/**unsuccessful stop clause*/
		if (idx == superSet.size()) return;
		Integer x = superSet.get(idx);
		current.add(x);

		/**"guess" x is in the subset*/
		getSubsets(superSet, k, idx+1, current, solution);
		current.remove(x);

		/**"guess" x is not in the subset*/
		getSubsets(superSet, k, idx+1, current, solution);
	}


	public List<Set<Integer>> getSubsets(int n, int k) {

		List<Integer> superSet = new ArrayList<Integer>();
		for(int i = 1 ; i<= n; i++)
			superSet.add(i);

		List<Set<Integer>> res = new ArrayList<Set<Integer>>();
		getSubsets(superSet, k, 0, new HashSet<Integer>(), res);

		return res;
	}

	/**
	 * Gets the maximum number of attributes in all the relations
	 * @param allVertex
	 * @return
	 */
	private int getMaximumAttrsRelation(Vector<JoinGraphNode> allVertex) {

		int maxAttrs = 0;

		/**for each relation*/
		for(JoinGraphNode jn: allVertex)
			/**if this relation has more number of attributes*/
			if ( jn.getTable().getColumnIndexList().size() > maxAttrs)
				maxAttrs = jn.getTable().getColumnIndexList().size();

		return maxAttrs;
	}

	public static void main(String[] args) throws Exception{

		GetTupleAssignmentUsingSolver gtas = new GetTupleAssignmentUsingSolver( new GetTupleAssignment(null, "4"), "4");

		gtas.setMaxAttributes(2);

		gtas.getTupleAssignment();

	}


	public HashMap<String, String> getRelationsJoined() {
		return relationsJoined;
	}
	public void setRelationsJoined(HashMap<String, String> relationsJoined) {
		this.relationsJoined = relationsJoined;
	}
	public Set<String> getBitVectors() {
		return bitVectors;
	}
	public void setBitVectors(Set<String> bitVectors) {
		this.bitVectors = bitVectors;
	}
	public GetTupleAssignment getGta() {
		return gta;
	}
	public void setGta(GetTupleAssignment gta) {
		this.gta = gta;
	}
	public HashMap<String, String> getTableMapping() {
		return tableMapping;
	}
	public void setTableMapping(HashMap<String, String> tableMapping) {
		this.tableMapping = tableMapping;
	}
	public HashMap<String, HashMap<Integer, ArrayList<Integer> > > getJoinedAttributes() {
		return joinedAttributes;
	}
	public void setJoinedAttributes(
			HashMap<String, HashMap<Integer, ArrayList<Integer> > > joinedAttributes) {
		this.joinedAttributes = joinedAttributes;
	}
	public boolean[][] getAreJoined() {
		return areJoined;
	}
	public void setAreJoined(boolean[][] areJoined) {
		this.areJoined = areJoined;
	}
	public HashMap<String, String> getJoinedString() {
		return joinedString;
	}
	public void setJoinedString(HashMap<String, String> joinedString) {
		this.joinedString = joinedString;
	}
	public char getIdChar() {
		return idChar;
	}
	public void setIdChar(char idChar) {
		this.idChar = idChar;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public int getMaxAttributes() {
		return maxAttributes;
	}

	public void setMaxAttributes(int maxAttributes) {
		this.maxAttributes = maxAttributes;
	}

	public HashMap<String, String> getRelationsIds() {
		return relationsIds;
	}

	public void setRelationsIds(HashMap<String, String> relationsIds) {
		this.relationsIds = relationsIds;
	}

	public int getNoOfRelations() {
		return noOfRelations;
	}

	public void setNoOfRelations(int noOfRelations) {
		this.noOfRelations = noOfRelations;
	}

}
