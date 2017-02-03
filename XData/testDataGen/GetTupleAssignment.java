package testDataGen;


import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.*;
import util.*;

/**
 * Given the join conditions and the count needed to satisfy the constrained aggregation this class
 * assigns the number of tuples for each base relation
 * Here base relation can be a from clause subquery
 * @author mahesh
 *
 */

/**
 * In the present implementation we consider the following assumptions
 *  1>> The join conditions between two relations are only equi-join conditions
 *  2>> There are no repeated relations in the same block of query. But there can be repeated relations across nested subqueries and outer query
 *  3>> We consider only 1:n, n:1, n:n, 1:1 as possible assignments (FIXME: Can we relax this??)
 */


public class GetTupleAssignment {
	private static Logger logger = Logger.getLogger(GetTupleAssignment.class.getName());
	public Graph< JoinGraphNode, JoinAndUniqueAttributes> joinGraph;

	/** Stores the attributes that should have same value across all tuples in a group */
	public Set < Node > singleValuedAttributes;

	/** store the unique keys i.e. keys which have distinct values */
	public HashSet < HashSet< Node > > uniqueElements;

	public ArrayList<Node> joinConds;
	public ArrayList<Node> groupByNodes;
	public ArrayList<Node> selectionConds;
	public ArrayList<Node> stringSelectionConds;
	public ArrayList<ArrayList<Node>> equivalenceClasses;
	public int finalCount;

	public HashMap<String, String> tableMapping;/** This is used to map table to the other table name (useful when sub queries are involved in joins) */
	public HashMap<String, String> columnMapping;
	public String defaultRootNode;

	/**stores a copy of single valued attributes. 
	 * Needed when we are using a constraint solver for tuple assignment*/
	public Set < Node > singleValuedAttributesDup;

	/** stores a copy of unique elements.
	 * Needed when we are using a constraint solver for tuple assignment */
	public HashSet < HashSet< Node > > uniqueElementsDup;

	private String filePath;

	/**
	 * Constructor
	 */
	public GetTupleAssignment(String defaultRootNode, String filePath){
		joinGraph = new Graph<JoinGraphNode, JoinAndUniqueAttributes>(true);/**It is a directed graph*/
		uniqueElements = new HashSet<HashSet< Node > >();
		singleValuedAttributes = new HashSet< Node >();
		singleValuedAttributesDup = new HashSet< Node >();
		uniqueElements = new HashSet<HashSet< Node > >();
		uniqueElementsDup = new HashSet<HashSet<Node>>();
		joinConds = new ArrayList<Node>();
		groupByNodes = new ArrayList<Node>();
		selectionConds = new ArrayList<Node>();
		stringSelectionConds = new ArrayList<Node>();
		equivalenceClasses = new ArrayList<ArrayList<Node>>();
		tableMapping = new HashMap<String, String>();
		columnMapping = new HashMap<String, String>();
		this.defaultRootNode = defaultRootNode;
		this.filePath = filePath;
	}

	/**
	 * This method is overall method that does each step of the algorithm, which involves
	 *  1>>Constructing the join graph based on the join conditions
	 *  2>>Inferring the properties of the attributes
	 *  3>>Choosing the relation whose  cardinality can be equal to 'finalCount'
	 *  4>>Getting the actual cardinality for each relation node in the join graph
	 * 
	 * This method return only join graph. join graph, where each node/vertex contains its actual cardinality. This also contains which attributes have to be unique and single valued. It won't assign the number of tuples to the base relation.
	 * @return  boolean variable whether assignment is possible or not
	 * @throws Exception
	 */
	public boolean getTupleAssignmentForQuery() throws Exception{

		logger.log(Level.INFO,"");
		logger.log(Level.INFO,"ENTER TUPLE ASIGNMENT METHOD :");
		logger.log(Level.INFO,"");


		/** Constructing the join graph */
		joinGraph = constructJoinGraph();

		long startAttribute = System.currentTimeMillis();
		/** Inference the attribute properties */
		getAttributeInferences();
		long endAttribute = System.currentTimeMillis();


		/**get minimum set of unique elements*/
		minimumSetOfUniqueElements();

		/** segregate unique elements of each relation occurrence */
		segregateUniqueElements();


		long startAssgn, endAssgn;

		startAssgn = System.currentTimeMillis();
		/** Choose the relation whose  cardinality = 'finalCount'. This acts as root node to start traversing the graph */
		JoinGraphNode jgn = chooseRootNode();

		/** This means there is no  possible relation which can have cardinality as 'finalCount' */
		if(jgn.getTable() == null)
			return false;

		/** get the actual cardinality */
		boolean possible = getActualCardinality(jgn);

		endAssgn = System.currentTimeMillis();



		logger.log(Level.INFO,"-------------------------------------------------");
		logger.log(Level.INFO,"start Attr: "+startAttribute);
		logger.log(Level.INFO,"end Attr: "+endAttribute);
		logger.log(Level.INFO,"start Assgn: "+startAssgn);
		logger.log(Level.INFO,"end assgn: "+endAssgn);
		logger.log(Level.INFO,"Attribute Inference Time: " + (endAttribute-startAttribute) );
		logger.log(Level.INFO,"Tuple  Assignment Time: " + (endAssgn-startAssgn) );
		logger.log(Level.INFO,"-------------------------------------------------");

		//possible = false;

		if(possible == false){
			logger.log(Level.INFO,"-------------------------------------------------");
			logger.log(Level.INFO,"CALLING TUPLE ASSIGNMENT USING CONSTRAINT SOLVER");
			logger.log(Level.INFO,"-------------------------------------------------");

			GetTupleAssignmentUsingSolver gtas = new GetTupleAssignmentUsingSolver(this, getFilePath());
			possible = gtas.getTupleAssignment();

			/**keep minimum set of unique elements*/
			/**Strictly speaking it is not needed*/
			//minimumSetOfUniqueElements();

			logger.log(Level.INFO,"-------------------------------------------------");
			logger.log(Level.INFO,"END OF CALLING TUPLE ASSIGNMENT USING CONSTRAINT SOLVER");
			logger.log(Level.INFO,"-------------------------------------------------");

			return possible;
		}
		logger.log(Level.INFO,"---------------------------");
		logger.log(Level.INFO,"EXIT TUPLE ASSIGNMENT METHOD");
		logger.log(Level.INFO,"---------------------------");

		return true;
	}


	/**
	 * Constructs a join graph based on the join conditions of the query block
	 * @return
	 */
	public Graph<JoinGraphNode, JoinAndUniqueAttributes> constructJoinGraph()  throws Exception{

		logger.log(Level.INFO,"Construction the join graph for " + joinConds);

		Graph< JoinGraphNode, JoinAndUniqueAttributes> joinGraph = new Graph<JoinGraphNode, JoinAndUniqueAttributes>(true);//It is a directed graph

		for(Node n: joinConds){
			boolean fromNode =false, toNode = false;
			JoinGraphNode jgn1 = new JoinGraphNode( n.getLeft());
			JoinGraphNode jgn2 = new JoinGraphNode( n.getRight());

			if(joinGraph.getAllVertex().contains(jgn1))
				fromNode = true;
			if(joinGraph.getAllVertex().contains(jgn2))
				toNode = true;


			/** get join attributes and unique attributes of each relation */
			JoinAndUniqueAttributes jau1 = new JoinAndUniqueAttributes();
			JoinAndUniqueAttributes jau2 = new JoinAndUniqueAttributes();

			if(fromNode && toNode && (joinGraph.getNeighbours(jgn1).get(jgn2) != null) && (joinGraph.getNeighbours(jgn2).get(jgn1) != null)){
				/** Add this join condition */

				joinGraph.getNeighbours(jgn1).get(jgn2).get(0).getJoinAttributes().add(n.getLeft());//Because only single edge

				joinGraph.getNeighbours(jgn2).get(jgn1).get(0).getJoinAttributes().add(n.getRight());

			}

			else{
				jau1.addJoinAttributes(n.getLeft());

				jau2.addJoinAttributes(n.getRight());

				/** It is directed graph */
				joinGraph.add(jgn1, jgn2, jau1 ) ;
				joinGraph.add(jgn2, jgn1, jau2 ) ;
			}
		}
		logger.log(Level.INFO,"Join graph constructed");
		return joinGraph;
	}



	/**
	 * This method finds the resultant single valued attributes and unique elements
	 */
	/** FIXME: Problem when a relation is repeated inside subqery and outer query block and has join condition between them */
	public void getAttributeInferences() throws Exception{

		logger.log(Level.INFO,"Inferring the properties of attributes of in the join conditions ");

		/**Rule 1: Every Group by attribute is a single valued attribute*/
		singleValuedAttributes.addAll(groupByNodes);

		/**Rule 2: Every attribute which is involved in the predicates of the from A.a=constant is a single valued attribute.
		 * These conditions might be in selection conditions or string selection conditions*/
		singleValuedAttributes.addAll( getConstConds(selectionConds) );
		singleValuedAttributes.addAll( getConstConds(stringSelectionConds) );



		/**Rule 3: Every set of attributes declared as primary key or unique key, is unique in the group
		 *get primary keys of each table involved in the join conditions*/
		Set< String > visited = new HashSet< String >(); 
		for(Node n: this.joinConds){

			/**If the primary keys of this node are not already added*/
			if( ! visited.contains(n.getLeft().getTable().getTableName())){
				HashSet<Node> un = getPrimaryKeyNode(n.getLeft());
				if(un.size() != 0) uniqueElements.add( un );
				visited.add(n.getLeft().getTable().getTableName());
			}

			/**If the primary keys of this node are not already added*/			
			if(! visited.contains(n.getRight().getTable().getTableName())){
				HashSet<Node> un = getPrimaryKeyNode(n.getRight());
				if(un.size() != 0) uniqueElements.add( un );
				visited.add(n.getRight().getTable().getTableName());
			}

		}


		int sSize ;
		int uSize ;
		boolean flag = false;/**Flag is used to keep track if rule 6 changes the set of elements*/
		do{
			sSize = singleValuedAttributes.size();
			uSize = uniqueElements.size();

			logger.log(Level.INFO,"At the start of loop SV attributes size:  " + sSize + " unique elements size: "+ uSize);

			/**Rule 4: If any non empty set of attributes of relation form a unique element and every attribute of that unique
			 *element is a single valued attribute then every other attribute of the relation is a single valued attribute.*/
			outer: 	for(Set< Node > v: uniqueElements){
				if(v.size() == 0)
					continue outer;
				for(Node n: v){

					/**check if this node is single valued*/
					if( !(singleValuedAttributes.contains(n)))
						continue outer;
				}

				/**add remaining attributes of the relation to the set of single valued attributes*/
				Iterator < Node > it = v.iterator();
				Node n= it.next();
				HashMap <String, Column> columns = n.getTable().getColumns();
				for(Column col: columns.values()){
					singleValuedAttributes.add( createNode(col, n) );
				}
			}

			/**Rule 5: If any attribute, Ri .x, is a single valued attribute then every attribute of equivalence class in which Ri .x is
			 *present becomes a single valued attribute.*/
			Set<Node> sv = new HashSet<Node>();
			sv.addAll(singleValuedAttributes);

			for(Node n: sv){
				for(ArrayList<Node> v: equivalenceClasses){
					if( v.contains(n))/**FIXME: Some problem here...this is calling equals()method of node*/
						singleValuedAttributes.addAll(v);
				}
			}

			/**Rule 6: If an attribute of unique element is single valued then remaining attributes of unique element become unique.*/
			flag = false;

			Set< HashSet<Node>> uniqueElementsSet = new HashSet<HashSet<Node>>();
			Set< HashSet<Node>> uniqueElementsRemoveSet = new HashSet<HashSet<Node>>();


			logger.log(Level.INFO,"Rule 6 for " + uniqueElements);

			for(HashSet< Node > u: uniqueElements){

				if(u.size() == 0)
					continue;
				HashSet< Node > uDup = new HashSet< Node >();
				uDup.addAll(u);

				for(Node n: u){
					if( singleValuedAttributes.contains(n))
						uDup.remove(n);
				}

				/**First condition is needed because, all attributes of unique element may be single valued. In that case we should not remove it.*/
				if( uDup.size() != 0 && (!(uDup.equals(u))) ){

					logger.log(Level.INFO,"-----------------------");
					logger.log(Level.INFO,"Rule 6 is true : " + u);
					logger.log(Level.INFO,"New set : " + uDup);
					logger.log(Level.INFO,"-----------------------");

					uniqueElementsSet.add(uDup);
					uniqueElementsRemoveSet.add(u);

					flag = true;
				}
			}

			boolean rem = uniqueElements.removeAll(uniqueElementsRemoveSet);
			boolean add = uniqueElements.addAll(uniqueElementsSet);
			logger.log(Level.INFO,"Rule 6 changed elements: " + flag);

			//System.out.println("At the end of loop SV attributes size:  " + singleValuedAttributes.size() + " unique elements size: "+ uniqueElements.size());

		}while( (sSize != singleValuedAttributes.size()) || (uSize != uniqueElements.size()) ||(flag == true) );/**there is change in the set of attributes  of single valued or unique elements*/ 

		logger.log(Level.INFO,"");
		//System.out.println("UNIQUE ELEMENTS: " + uniqueElements);
		logger.log(Level.INFO,"");
		//System.out.println("SINGLE VALUED ATTRIBUTES: " +  singleValuedAttributes);
		logger.log(Level.INFO,"");

		/**keep a copy of unique elements and single valued attributes*/
		uniqueElementsDup.addAll((HashSet < HashSet< Node > >)uniqueElements.clone());

		for(Node sv: singleValuedAttributes)
			singleValuedAttributesDup.add( sv.clone() );
	}


	/**
	 * gets the conditions of the form A.x = const
	 * @param conds
	 * @return set of nodes
	 * @throws Exception
	 */
	public Set<Node> getConstConds(ArrayList<Node> conds) throws Exception{

		Set < Node > singleValuedAttributes = new HashSet< Node >();
		for(Node n: conds){

			/**if condition is of form A.a=constant*/
			if(n.getType().equalsIgnoreCase(Node.getBroNodeType()) && (n.getLeft().getType().equalsIgnoreCase(Node.getValType()) || n.getRight().getType().equalsIgnoreCase(Node.getValType()))){

				Node n1  = new Node();
				if ( !(n.getLeft().getType().equalsIgnoreCase(Node.getValType())) )
					n1 = n.getLeft();
				else
					n1 = n.getRight();
				singleValuedAttributes.add(n1);
			}
		}
		return singleValuedAttributes;
	}


	/**
	 * Creates a node for each primary key of the table
	 * @param node
	 * @return node
	 */
	public HashSet< Node > getPrimaryKeyNode(Node n) throws Exception{

		HashSet< Node > list = new HashSet< Node >();		

		/**for left side of join condition get primary keys*/
		Vector< Column > primaryKeys = new Vector< Column >();

		if(!tableMapping.containsValue(n.getTableNameNo())){/**If this table is not a sub query node*/
			primaryKeys = n.getTable().getPrimaryKey();

			for(Column c: primaryKeys){
				list.add(createNode(c, n));
			}
		}
		return list;
	}


	/**
	 * This method creates a node based on the column of table
	 * @param column
	 * @param node
	 * @return node
	 * @throws Exception
	 */
	public static Node createNode(Column c, Node n)  throws Exception{
		Node n1 = new Node();
		n1.setColumn(c);
		n1.setLeft(null);
		n1.setRight(null);
		n1.setQueryIndex(n.getQueryIndex());
		n1.setQueryType(n.getQueryType());
		n1.setTable(n.getTable());
		n1.setTableNameNo(n.getTableNameNo());
		n1.setType(Node.getColRefType());

		return n1;
	}
	/**
	 * This method chooses the node that can contain cardinality as 'finalCount's
	 */
	public JoinGraphNode chooseRootNode()  throws Exception{
		logger.log(Level.INFO,"Choosing the relation whose cardinality can be (acts as starting point for the join graph): " + finalCount);

		updateMaximumPossibleCardinalityForRelation();

		/**choose the relation whose cardinality can be 'finalCount'*/
		JoinGraphNode jgn = getPossibleRootNode();

		logger.log(Level.INFO,"");
		logger.log(Level.INFO,"The node chosen with cardinality = 'finalCount' is : " + jgn);
		logger.log(Level.INFO,"");

		return jgn;
	}

	/**
	 * This method finds if there are any relation whose maximum cardinality is constrained
	 */
	public void updateMaximumPossibleCardinalityForRelation() {

		/**If every attribute of unique element is a single valued then the maximum cardinality of relation should be 1*/
		for(Set< Node > unique: uniqueElements){
			if(unique.size() == 0)
				continue;
			if(singleValuedAttributes.containsAll(unique)){

				/**get the node for this unique element*/
				Iterator < Node > it = unique.iterator();
				Node n= it.next();
				JoinGraphNode jgn = new JoinGraphNode(n);

				/**update the actual cardinality*/
				for(JoinGraphNode vertex : joinGraph.getAllVertex())
					if(jgn.equals(vertex))
						vertex.setmaximumPossibleCardinality(1);
			}
		}
	}

	/**
	 * Separates the unique elements of each relation and are stored into the edges of the join graph
	 */
	public void segregateUniqueElements()  throws Exception{

		logger.log(Level.INFO,"Finding the join attributes which form unique element");
		logger.log(Level.INFO,"");

		for(Set< Node > unique: uniqueElements){
			if(unique.size() == 0)
				continue;

			/**get the relation occurrence 
			 *FIXME: As sub query group by nodes are added to unique elements, which relation should we consider (because group by nodes can be from different relations)*/
			Iterator < Node > it = unique.iterator();
			Node n= it.next();
			JoinGraphNode jgn = new JoinGraphNode(n);

			segregateUniqueElementsOfNode(unique, jgn);
		}
		logger.log(Level.INFO,"");
		logger.log(Level.INFO,"The join attributes which form unique element are found");
	}

	/**
	 * This method does the actual segregation of join attributes into unique elements across all edges of the given node
	 * @param unique element set
	 * @param join graph node
	 */
	public void segregateUniqueElementsOfNode(Set<Node> unique, JoinGraphNode jgn)  throws Exception{
		logger.log(Level.INFO,"");
		logger.log(Level.INFO,"Finding the join attributes which form unique element for the edge from " + jgn);

		/**Get the edges of this vertex 'jgn'*/
		Map< JoinGraphNode, Vector< JoinAndUniqueAttributes > > edges = joinGraph.getNeighbours(jgn);

		if(edges == null || edges.size() == 0)
			return;
		
		/**get the join attributes in each edge of this node*/
		Iterator itr = edges.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry pairs = (Map.Entry)itr.next();

			System.out.print(" to " + pairs.getKey());
			logger.log(Level.INFO,"");

			Vector< JoinAndUniqueAttributes > attributes = (Vector< JoinAndUniqueAttributes >) pairs.getValue();

			for(JoinAndUniqueAttributes jua: attributes){

				/**get the join attributes*/
				Set< Node > joinAttributes = jua.getJoinAttributes();

				/**if any subset of these join attributes is an unique element*/
				if(joinAttributes.containsAll(unique)){

					Set<Set<Node>> unq = new HashSet<Set<Node>>();
					for(Set<Node> set: jua.getUniqueElements()){
						Set<Node> s = new HashSet<Node>();

						for(Node n: set)
							s.add(n.clone());

						unq.add(s);
					}

					/**remove previous unique elements which are superset of of this unique set and are involved in joins*/
					for(Set<Node> u: unq)
						if(u.containsAll(unique))
							jua.getUniqueElements().remove(unique);					

					/**Add the join attributes of the edge that form unique element to the set unique[edge]*/
					jua.addUniqueElements(unique);
				}
			}
			logger.log(Level.INFO,"Completed for this edge ");
			logger.log(Level.INFO,"");
		}
	}




	/**
	 * Choose the relation whose  cardinality can be 'finalCount'
	 * @return
	 */
	public JoinGraphNode getPossibleRootNode()  throws Exception{


		/**get all the vertices of join graph*/
		Vector< JoinGraphNode > vertices = joinGraph.getAllVertex();

		for(JoinGraphNode jgn: vertices){
			if(defaultRootNode != null){
				if(jgn.getTableNameNo().equalsIgnoreCase(defaultRootNode))
					if(jgn.getmaximumPossibleCardinality() >= finalCount)
						return jgn;
			}
			else if(jgn.getmaximumPossibleCardinality() >= finalCount)
				return jgn;
		}
		return new JoinGraphNode(null);
	}


	/**
	 * Gets actual cardinality for each relation
	 * @param root---indicates the relation/node whose  cardinality is 'finalCount'. This acts as starting point to traverse the graph
	 */
	public boolean getActualCardinality(JoinGraphNode root)  throws Exception{

		/**FIXME: Add support to check when this is not possible*/	
		logger.log(Level.INFO,"");
		logger.log(Level.INFO,"Getting the cardinality");

		/**Set used to keep the nodes whose actual cardinality is 'finalCount'*/
		Set< JoinGraphNode > nList = new HashSet< JoinGraphNode >();
		nList.add(root);

		/**make the actual cardinality of the root node to be 'finalCount'*/
		setActualCardinalityOfNode(root);

		Iterator < JoinGraphNode > it = nList.iterator();

		/**Repeat this until nList is empty*/
		while(it.hasNext()) {
			boolean flag1 = false;/**flag used to keep track if there is a change in the unique elements*/

			/**Get the edges of this vertex 'jgn'*/
			JoinGraphNode jgn = it.next();

			logger.log(Level.INFO," The new node chosen from nList is: " + jgn);

			Map< JoinGraphNode, Vector< JoinAndUniqueAttributes > > edges = joinGraph.getNeighbours(jgn);

			/**Traverse the edges of this node either in BFS or DFS*/
			Iterator itr = edges.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry pairs = (Map.Entry)itr.next();

				System.out.print("Applying rule 7 from node " + jgn);

				/**Rule 7*/
				applyRule7(pairs);

				//We have to keep minimum set of unique elements
				boolean flag = modifyUniqueElements();


				int size = uniqueElements.size();

				/**Add the node to the list, if there is change in the set of unique elements. This needed to avoid following cases
				 *Let 'A' and 'B' are in join graph and both have unique elements involved in joins. Let say we start from 'A'
				 *Now 'B' is added to nList
				 *In next iteration again 'A' will be added which results in infinite loop*/

				/**get the actual cardinality of the node*/
				if(size != uniqueElements.size() || flag == true){
					logger.log(Level.INFO,"Adding to nList "+ pairs.getKey());

					nList.add((JoinGraphNode)pairs.getKey());

					logger.log(Level.INFO,"nList now is: " + nList);
				}
			}


			/**Rule 8:*/
			Set<JoinGraphNode> jgn1 = applyRule8(jgn);
			if(jgn1 != null && !jgn1.isEmpty()){

				logger.log(Level.INFO,"Adding to nList by rule 8: " + jgn1);
				nList.addAll(jgn1);

				/**Rule 9 may have modified uniqueElements*/
				flag1 = true;//modifyUniqueElements();

				/**FIXME: Which key should be added to nList??
				 *For this flag is used while removing from nList??*/
			}

			/**Remove this element from nList*/
			if(!flag1) {
				nList.remove(jgn);
				logger.log(Level.INFO,"nList after removing "+ jgn +" is: " + nList);
			}

			/**update iterator*/
			it = nList.iterator();
		}

		return true;
	}

	/**
	 * Rule 7 apply
	 * @param pairs
	 * @return
	 * @throws Exception
	 */
	public void applyRule7(Map.Entry pairs) throws Exception {


		System.out.print(" to the node " + pairs.getKey() + "\n");

		Vector< JoinAndUniqueAttributes > attributes = (Vector< JoinAndUniqueAttributes >) pairs.getValue();

		/**Rule 7: If there are any join attributes of edge from relation 'A' to 'B', which form unique element 'u' and number of tuples of 'A' is 'finalCount', then
		 *number of tuples of 'B' becomes 'finalCount' and the attributes of 'B', which have join relationship with unique element 'u', become unique.*/
		for(JoinAndUniqueAttributes jua: attributes){
			if(jua.getUniqueElements().size() > 0){

				/**Modify the actual cardinality of the relation*/
				setActualCardinalityOfNode((JoinGraphNode)pairs.getKey());

				/**Add the corresponding join Attributes to the set of uniqueElements*/
				uniqueElements.addAll(updateUniqueElements(pairs));


			}
		}

	}

	/**
	 * This is Rule 8: This rule is applied for the node when its actual cardinality is 'finalCount', 
	 * To apply this method we use all the edges of the node. The following is description of the rule
	 *  
	 *  1>> get the unique elements of node 'jgn'
	 *  2>> If there is any multi-attribute unique element and
	 *  3>> If 'r' proper and exhaustive subsets of this multi-attribute unique element are involved in joins with 'r' different relations
	 *  4>> then atleast one of this 'r' proper subsets must be unique (let say this edge is from 'jgn' to 'jgn2')
	 *  5>> and we have to again apply rule 7 between the edges of 'jgn', 'jgn2' and 'jgn2' is added to nList

	 * @param jgn
	 */
	private Set<JoinGraphNode> applyRule8(JoinGraphNode jgn) throws Exception{

		logger.log(Level.INFO,"");
		logger.log(Level.INFO,"APPLYING RULE 8 for the node " + jgn);
		logger.log(Level.INFO,"");
		//This rule 9 may be applied  multiple times at the given node because there may be multiple multi attribute unique elements at the node

		Set<JoinGraphNode> jgn1 = new HashSet<JoinGraphNode>();
		//Step1
		Set< Set<Node> > unique = getMultiAttributeUniqueElementsOfNode(jgn);

		logger.log(Level.INFO,"Multi attribute unique elements of node " + jgn +" are " + unique);
		Set< Node > joinAttrs = new HashSet< Node >();
		boolean flag = false;//To check whether rule 9 is applicable for the node 'jgn'
		Iterator< Set<Node>> it =unique.iterator();
		//If there are any multi attribute unique elements
		while(it.hasNext()){
			int noOfEdges = 0;//To keep track of how many edges of this node are visited

			Set<Node > un = it.next();

			//get all edges of this 'jgn' node
			Map< JoinGraphNode, Vector< JoinAndUniqueAttributes > > edges = joinGraph.getNeighbours(jgn);

			//get the join attributes in each edge of this node
			Iterator itr = edges.entrySet().iterator();
			while (itr.hasNext() && flag == false) {//for each edge and iff rule 8 is not applied

				Map.Entry pairs = (Map.Entry)itr.next();

				logger.log(Level.INFO,"Traversing the edge from "+ jgn + " to " + pairs.getKey());

				Vector< JoinAndUniqueAttributes > attributes = (Vector< JoinAndUniqueAttributes >) pairs.getValue();

				for(JoinAndUniqueAttributes jua: attributes){

					/**if this join attributes in this edge contains all unique elements then continue*/
					if( jua.getJoinAttributes().containsAll(un))
						continue ;

					//get the join attributes along this edge
					joinAttrs.addAll(jua.getJoinAttributes());

					//If this set of join attributes are equal to unique element
					//If this edge do not contain all the attributes of unique element
					if(joinAttrs.containsAll(un) && (!jua.getJoinAttributes().containsAll(un))){//We need 'r' different edges (for step3 mentioned in above comments)
						//if(noOfEdges > 1){//We need 'r' different edges (for step3 mentioned in above comments)
						flag = true;
						jgn1.add((JoinGraphNode)pairs.getKey());
						//Atleast one of this 'r' relations must contain actual cardinality as 'finalCount'  
						//FIXME: Here We are choosing the relation along present edge. Will this cause problems??
						//Need to prove that any relation can work??
						//I think it won't cause problem because we are not missing any possible solution by making subset of unique elements
						//as unique. We will miss solution iff we are making single valued attributes as unique or vice versa.


						//get edge between 'jgn' and 'jgn1' and apply rule 7
						Vector< JoinAndUniqueAttributes > edge = joinGraph.getEdges(jgn, (JoinGraphNode)pairs.getKey());
						//Map.Entry pair = new AbstractMap.SimpleEntry<JoinGraphNode, Vector< JoinAndUniqueAttributes >>((JoinGraphNode)pairs.getKey(), edge);

						//update the unique elements across this edge 
						Set<Node> newUnq = new HashSet<Node>();
						for(Node u: un)
							if(jua.getJoinAttributes().contains(u))
								newUnq.add(u);

						edge.get(0).addUniqueElements(newUnq);
						//apply rule 7
						//applyRule7(pair);

						//}

					}
				}
			}		
		}
		return jgn1;		
	}


	public Set<Set<Node>> getMultiAttributeUniqueElementsOfNode(JoinGraphNode jgn) {

		Set< Set<Node> > unique = new HashSet< Set< Node>>();

		for(Set< Node > u: uniqueElements){
			if(u.size() > 1){//If multi attribute 
				Iterator < Node > it = u.iterator();
				Node n= it.next();
				//If this unique element is from the same relation occurrence as that of 'jgn'
				if(n.getTableNameNo().equalsIgnoreCase(jgn.getTableNameNo()))
					unique.add(u);
			}
		}
		return unique;
	}


	/**
	 * Makes the actual cardinality of the relation/node to be 'finalCount'
	 * @param root2
	 * @param finalCount
	 */
	public void setActualCardinalityOfNode(JoinGraphNode vertex) throws Exception{

		for(JoinGraphNode jgn: joinGraph.getAllVertex()){
			if(jgn.equals(vertex)){
				jgn.setCardinality(finalCount);
				break;
			}
		}
	}



	/**
	 * This method updates the unique elements
	 * We need to update both the datastructures i.e. unique elements set and also unique join attributes across the edge
	 * @param pairs
	 */
	public HashSet<HashSet< Node >> updateUniqueElements(Entry pairs) throws Exception{

		logger.log(Level.INFO,"Updating unique elements for the node " + pairs.getKey());
		//get the attributes of the edge
		Vector< JoinAndUniqueAttributes > attributes = (Vector< JoinAndUniqueAttributes >) pairs.getValue();
		HashSet<HashSet< Node >> newUnique = new HashSet< HashSet<Node> >();

		for(JoinAndUniqueAttributes jua: attributes){
			//for each unique element involved in joins along this edge
			for(Set< Node > unique: jua.getUniqueElements()){
				for(Node u: unique){
					HashSet<Node> un = new HashSet<Node>();
					//get the corresponding node of the other side relation involved in join
					for(Node n: joinConds){
						//If this node is left side of join then add right side node of the join condition
						if(n.getLeft().getTableNameNo().equalsIgnoreCase(u.getTableNameNo()) && n.getLeft().getColumn().equals(u.getColumn()))
							un.add(n.getRight());
						//If this node is right side of join then add left side node of the join condition						
						else if(n.getRight().getTableNameNo().equalsIgnoreCase(u.getTableNameNo()) && n.getRight().getColumn().equals(u.getColumn()))
							un.add(n.getLeft());
					}
					if( !un.isEmpty() && un.size() != 0)
					newUnique.add(un);
				}
			}

		}

		logger.log(Level.INFO,"");
		logger.log(Level.INFO,"New set of unique elements are " + newUnique);
		//Add this unique element to the unique set of the edge
		for(HashSet<Node> un: newUnique)
			segregateUniqueElementsOfNode(un, (JoinGraphNode)pairs.getKey());

		logger.log(Level.INFO,"Unique Elements are updated for the node "+ pairs.getKey());
		return newUnique;
	}


	/**
	 * This method keeps minimum set of unique elements
	 * i.e. if (a,b) is unique and (a) has become unique, then it is sufficient to keep only (a) 
	 * @return
	 */
	public boolean modifyUniqueElements() throws Exception{

		logger.log(Level.INFO,"Keeping minimum set of unique elements by removing super sets in " + uniqueElements);

		boolean change = false;
		List<Set<Node> > uniqueElementsDup = new ArrayList< Set<Node>>(uniqueElements);

		for(int i=0; i<uniqueElementsDup.size();i++){
			for(int j=i+1; j<uniqueElementsDup.size(); j++){
				//if one set is subset of other 
				if(uniqueElementsDup.get(j).size() != 0 && uniqueElementsDup.get(i).containsAll(uniqueElementsDup.get(j))){
					change = true;
					//update the unique elements
					Set<Node> unq = uniqueElementsDup.get(i);
					uniqueElements.remove(unq);
					//update the unique elements across the edges of node
					//updateUniqueElementsOfJoinNode(uniqueElementsDup.get(i));
					Iterator<Node> it = unq.iterator();
					JoinGraphNode jgn = new JoinGraphNode(it.next());
					segregateUniqueElementsOfNode(unq, jgn);
				}
				else if(uniqueElementsDup.get(i).size() != 0 && uniqueElementsDup.get(j).containsAll(uniqueElementsDup.get(i))){
					change = true;
					//update the unique elements
					Set<Node> unq = uniqueElementsDup.get(j);
					uniqueElements.remove(unq);
					//update the unique elements across the edges of node
					//updateUniqueElementsOfJoinNode(uniqueElementsDup.get(i));
					Iterator<Node> it = unq.iterator();
					JoinGraphNode jgn = new JoinGraphNode(it.next());
					segregateUniqueElementsOfNode(unq, jgn);
				}
			}
		}

		logger.log(Level.INFO,"New set of unique elements " + uniqueElements);
		logger.log(Level.INFO,"");
		return change;
	}

	/**
	 * Keeps minimum set of unique elements
	 */
	private void minimumSetOfUniqueElements() {

		/**keep a copy of unique elements*/
		List<Set<Node> > uniqueElementsDup = new ArrayList< Set<Node>>(uniqueElements);

		for(int i=0; i<uniqueElementsDup.size();i++)
			for(int j=i+1; j<uniqueElementsDup.size(); j++)

				/**if one set is subset of other*/
				if(uniqueElementsDup.get(j).size() != 0 && uniqueElementsDup.get(i).containsAll(uniqueElementsDup.get(j)))
					uniqueElements.remove(uniqueElementsDup.get(i));

				else if(uniqueElementsDup.get(i).size() != 0 && uniqueElementsDup.get(j).containsAll(uniqueElementsDup.get(i)))
					uniqueElements.remove(uniqueElementsDup.get(j));


	}
	/** 
	 * This method updates the unique elements of the node involved in the join conditions
	 * @param set
	 *//*
	//This method does almost same activity as that of segregateUniqueElementsOfNode() method except for loop in the inner most if. 
	//So segregateUniqueElementsOfNode() modified to handle this case also
	public void updateUniqueElementsOfJoinNode(Set<Node> uniqueElementsSet) {

		Iterator<Node> it = uniqueElementsSet.iterator();
		if( !it.hasNext())
			return ;
		JoinGraphNode jgn = new JoinGraphNode(it.next());

		//Get the edges of this vertex 'jgn'
		Map< JoinGraphNode, Vector< JoinAndUniqueAttributes > > edges = joinGraph.getNeighbours(jgn);

		//get the join attributes in each edge of this node
		Iterator itr = edges.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry pairs = (Map.Entry)itr.next();
			Vector< JoinAndUniqueAttributes > attributes = (Vector< JoinAndUniqueAttributes >) pairs.getValue();

			for(JoinAndUniqueAttributes jua: attributes){
				//get the join attributes
				Set< Node > joinAttributes = jua.getJoinAttributes();

				//if these join attributes is an unique element
				if(joinAttributes.containsAll(uniqueElementsSet)){
					//remove previous unique elements which are superset of of this unique set and are involved in joins
					for(Set<Node> unique: jua.getUniqueElements()){
						if(unique.containsAll(uniqueElementsSet))
							jua.getUniqueElements().remove(unique);
					}
					//Add the join attributes of the edge that form unique element to the set unique[edge]
					jua.addUniqueElements(uniqueElementsSet);
				}
			}
		}

	}
	  */

	/**
	 * Returns the actual cardinality of the node/relation
	 * @param key
	 * @return
	 */
	public int getActualCardinalityOfNode(JoinGraphNode vertex) {

		for(JoinGraphNode jgn: joinGraph.getAllVertex()){
			if(jgn.equals(vertex)){
				return jgn.getCardinality();
			}
		}

		return -1;
	}



	/**
	 * Creates mapping for table names and column names for the given sub query
	 * This method creates a new relation for the entire sub query, whose schema is union of group by nodes and projected columns of the sub query and primary key consists of group by nodes
	 * @param subQuery 
	 */
	public void createRelationNodeForSubquery(QueryBlockDetails subQuery, int subQueryNumber) throws Exception{


		/**get the new name for this relation*/
		String tabName = "S"+Integer.toString(subQueryNumber);


		/**The schema of relation contains union of group by nodes and projected columns*/
		Set<Node> schema = new HashSet<Node>();

		HashSet<Node> groupbyNodes = new HashSet<Node>();
		for(Node gn: subQuery.getGroupByNodes())
			groupbyNodes.add(new Node(gn));

		schema.addAll(groupbyNodes);

		Set<Node> proj = new HashSet<Node>();
		for(Node pn: subQuery.getProjectedCols()){
			Node nn = new Node(pn);
			proj.add(nn);
		}

		schema.addAll(proj);

		List<Node> schemaList = new ArrayList<Node>(schema);
		/**create table with this schema*/
		Table newTable = getTable(tabName, schemaList);

		uniqueElements.add(groupbyNodes);

		int colNumber = 0;




		/**get mapping for each column*/
		for(Node n: schemaList){

			Node n1 = new Node(n);
			String table = "", col = "";

			/**If this is aggregate Node*/
			if(n1.getType().equalsIgnoreCase(Node.getAggrNodeType())){
				continue;
			}
			else{
				/**get table name*/
				table = n.getTableNameNo();

				/**get column name*/
				col = n.getColumn().getColumnName();
			}
			/**map this new table*/
			tableMapping.put(table, tabName);

			/**map this new column*/
			columnMapping.put(col +""+colNumber, table+"."+col);

			/**update col name and table name and table*/ 
			n1.setTableNameNo(tabName);
			n1.getColumn().setColumnName(col +Integer.toString(colNumber));
			n1.setTable(newTable);
			n1.setQueryIndex(subQueryNumber);
			
			//groupbyNodes.add(n);
			ArrayList<Node> joinconds = new ArrayList<Node>();
			joinconds.addAll((ArrayList<Node>)joinConds.clone());
			for(Node jn: joinconds){//for each join condition

				if(jn.getLeft().equals(n)){
					Node jn1 = new Node(jn);
					jn1.setLeft(n1);
					joinConds.remove(jn);
					joinConds.add(jn1);
				}
				else if (jn.getRight().equals(n)){
					Node jn1 = new Node(jn);
					jn1.setRight(n1);
					joinConds.remove(jn);
					joinConds.add(jn1);
				}
			}

			//update group by nodes
			ArrayList<Node> groupNodes = new ArrayList<Node>();
			groupNodes = (ArrayList<Node>)groupByNodes.clone();
			/*for (Node gg: groupByNodes)
				groupNodes.add(gg);*/
			for(Node gb: groupNodes){
				if(gb.equals(n)){
					groupByNodes.remove(gb);
					groupByNodes.add(n1);
				}
			}
			//The group by nodes form unique element (primary key for this relations)
			uniqueElements.remove(groupbyNodes);

			groupbyNodes.remove(n);
			groupbyNodes.add(n1);

			//update unique elements
			HashSet<HashSet<Node>> uniqueElementsDup = new HashSet<HashSet<Node>>(); 
			uniqueElementsDup.addAll(uniqueElements);

			for(HashSet<Node> ue: uniqueElementsDup){
				if(ue.contains(n)){
					HashSet<Node> ueDup = new HashSet(ue);
					uniqueElements.remove(ue);
					ueDup.remove(n);
					ueDup.add(n1);
					uniqueElements.add(ueDup);
				}
			}

			uniqueElements.add(groupbyNodes);


			/** update equivalence classes */
			ArrayList<ArrayList<Node>> equivalenceClass = new ArrayList<ArrayList<Node>>(); 
			equivalenceClass.addAll(equivalenceClasses);
			for(ArrayList<Node> ec: equivalenceClass){
				for(Node e: ec){
					if(e.equals(n)){
						ArrayList<Node> ec2 = (ArrayList<Node>)ec.clone();
						equivalenceClasses.remove(ec);
						ec2.remove(e);
						ec2.add(n1);
						equivalenceClasses.add(ec2);
					}
				}
			}
			//Update selection and string selection conditions of the form A.x=co of sub query
			Vector<Node> selectionCondsDup = new Vector<Node>();
			Vector<Node> stringSelectionCondsDup = new Vector<Node>();
			selectionCondsDup.addAll(selectionConds);
			stringSelectionCondsDup.addAll(stringSelectionConds);

			for(Node sel: selectionCondsDup){
				if(sel.getLeft().equals(n)){
					Node selDup = new Node(sel);
					selDup.setLeft(n1);
					selectionConds.remove(sel);
					selectionConds.add(selDup);
				}
				if(sel.getRight().equals(n)){
					Node selDup = new Node(sel);
					selDup.setRight(n1);
					selectionConds.remove(sel);
					selectionConds.add(selDup);					
				}
				if(sel.equals(n)){
					selectionConds.remove(sel);
					selectionConds.add(n1);
				}
			}

			for(Node sel: stringSelectionCondsDup){
				if(sel.getLeft().equals(n)){
					Node selDup = new Node(sel);
					selDup.setLeft(n1);
					stringSelectionConds.remove(sel);
					stringSelectionConds.add(selDup);
				}
				if(sel.getRight().equals(n)){
					Node selDup = new Node(sel);
					selDup.setRight(n1);
					stringSelectionConds.remove(sel);
					stringSelectionConds.add(selDup);					
				}
				if(sel.equals(n)){
					stringSelectionConds.remove(sel);
					stringSelectionConds.add(n1);
				}
			}

			colNumber ++;

		}
		if(tableMapping.containsKey(defaultRootNode))
			defaultRootNode = tableMapping.get(defaultRootNode);
	}




	private Table getTable(String tabName, List<Node> schema) {

		Table table = new Table(tabName);

		int colNo = 0;
		for(Node n: schema){

			String tableName, colName;
			/**If this is aggregate Node*/
			if(n.getType().equalsIgnoreCase(Node.getAggrNodeType())){
				continue;
			}
			else{
				/**get table name*/
				tableName = n.getTableNameNo();

				/**get column name*/
				colName = n.getColumn().getColumnName();
			}
			Column col = new Column(colName + colNo, tabName);
			table.addColumn(col);

			/*    *//**map this new table*//*
			tableMapping.put(n.getTableNameNo(), tabName);

			 *//**map this new column*//*
			columnMapping.put(col +""+colNo, table+"."+col);*/

			colNo++;
		}
		return table;
	}

	/**
	 * Below are stters and getters for the data members
	 * @return
	 */
	public Graph<JoinGraphNode, JoinAndUniqueAttributes> getJoinGraph() {
		return joinGraph;
	}


	public void setJoinGraph(Graph<JoinGraphNode, JoinAndUniqueAttributes> joinGraph) {
		this.joinGraph = joinGraph;
	}


	public Set<Node> getSingleValuedAttributes() {
		return singleValuedAttributes;
	}


	public void setSingleValuedAttributes(Set<Node> singleValuedAttributes) {
		this.singleValuedAttributes = singleValuedAttributes;
	}


	public Set<HashSet<Node>> getUniqueElements() {
		return uniqueElements;
	}


	public void setUniqueElements(HashSet<HashSet<Node>> uniqueElements) {
		this.uniqueElements = uniqueElements;
	}

	public void addUniqueElements(HashSet<Node> uniqueElement) {
		this.uniqueElements.add(uniqueElement);
	}

	public void removeUniqueElements(Set<Node> uniqueElement) {
		this.uniqueElements.remove(uniqueElement);
	}
	public ArrayList<Node> getJoinConds() {
		return joinConds;
	}


	public void setJoinConds(ArrayList<Node> joinConds) {
		this.joinConds = joinConds;
	}


	public ArrayList<Node> getGroupByNodes() {
		return groupByNodes;
	}


	public void setGroupByNodes(ArrayList<Node> groupByNodes) {
		this.groupByNodes = groupByNodes;
	}


	public ArrayList<Node> getSelectionConds() {
		return selectionConds;
	}


	public void setSelectionConds(ArrayList<Node> selectionConds) {
		this.selectionConds = selectionConds;
	}


	public ArrayList<Node> getStringSelectionConds() {
		return stringSelectionConds;
	}


	public void setStringSelectionConds(ArrayList<Node> stringSelectionConds) {
		this.stringSelectionConds = stringSelectionConds;
	}


	public ArrayList<ArrayList<Node>> getEquivalenceClasses() {
		return equivalenceClasses;
	}


	public void setEquivalenceClasses(ArrayList<ArrayList<Node>> equivalenceClasses) {
		this.equivalenceClasses = equivalenceClasses;
	}


	public int getFinalCount() {
		return finalCount;
	}


	public void setFinalCount(int finalCount) {
		this.finalCount = finalCount;
	}


	public HashMap<String, String> getTableMapping() {
		return tableMapping;
	}

	public void setTableMapping(HashMap<String, String> tableMapping) {
		this.tableMapping = tableMapping;
	}

	public HashMap<String, String> getColumnMapping() {
		return columnMapping;
	}

	public void setColumnMapping(HashMap<String, String> columnMapping) {
		this.columnMapping = columnMapping;
	}

	/**
	 * This method creates the original names for each node that are mapped for handling the sub queries
	 */
	public void getOriginalColumnNames(TableMap tm) {

		//Handle the single valued attributes
		HashSet<Node> singleValuedAttributesDup = new HashSet<Node>();
		singleValuedAttributesDup.addAll(singleValuedAttributes);

		for(Node n: singleValuedAttributesDup){
			if( getTableMapping().containsValue(n.getTableNameNo()) ){//If this node is mapped
				Node nDup = new Node(n);
				//get the column
				String column = n.getColumn().getColumnName();
				//get the original string that contains original table name and column name
				String originalName = getColumnMapping().get(column);

				String tableNameNo = originalName.split("\\.")[0];

				String columnName = originalName.split("\\.")[1];

				String tableName = tableNameNo.substring(0, tableNameNo.length()-1);
				Table table = tm.getTable(tableName); 

				nDup.setTableNameNo(tableNameNo);
				nDup.getColumn().setColumnName(columnName);
				nDup.setTable(table);

				singleValuedAttributes.remove(n);
				singleValuedAttributes.add(nDup);
			}
		}

		//Handle unique elements
		boolean change;

		do{
			change = false;
			HashSet<HashSet<Node>> uniqueElementsDup = new HashSet<HashSet<Node>>(); 
			uniqueElementsDup.addAll(uniqueElements);

			Iterator< HashSet<Node>>  it = uniqueElementsDup.iterator();

			while(it.hasNext() && change == false){


				HashSet<Node> unq = new HashSet<Node>();
				unq.addAll(it.next());


				Iterator<Node> it1 = unq.iterator();
				while(it1.hasNext() && change == false){

					Node u = new Node(it1.next());

					if( getTableMapping().containsValue(u.getTableNameNo()) ){//If this node is mapped
						uniqueElements.remove(unq);
						unq.remove(u);

						//get the column
						String column = u.getColumn().getColumnName();
						//get the original string that contains original table name and column name
						String originalName = getColumnMapping().get(column);
						String tableNameNo = originalName.split("\\.")[0];
						String columnName = originalName.split("\\.")[1];

						String tableName = tableNameNo.substring(0, tableNameNo.length()-1);
						Table table = tm.getTable(tableName); 

						u.setTableNameNo(tableNameNo);
						u.getColumn().setColumnName(columnName);
						u.setTable(table);
						unq.add(u);

						uniqueElements.add(unq);
						change = true;
					}
				}
			}


			/*	for(HashSet<Node> unq: uniqueElementsDup){	
				for(Node u: unq){
					if( getTableMapping().containsValue(u.getTableNameNo()) ){//If this node is mapped
						HashSet<Node> unqDup = new HashSet<Node>();
						unqDup.addAll(unq);

						uniqueElements.remove(unqDup);

						Node uDup = new Node(u);

						//get the column
						String column = u.getColumn().getColumnName();
						//get the original string that contains original table name and column name
						String originalName = getColumnMapping().get(column);
						String tableNameNo = originalName.split("\\.")[0];
						String columnName = originalName.split("\\.")[1];

						uDup.setTableNameNo(tableNameNo);
						uDup.getColumn().setColumnName(columnName);

						unqDup.remove(u);
						unqDup.add(uDup);

						uniqueElements.add(unqDup);
						change = true;
					}
				}
			}*/

		}while(change == true);



		/*HashSet<HashSet<Node>> uniqueElementsDup = new HashSet<HashSet<Node>>(); 
		uniqueElementsDup.addAll(uniqueElements);

		for(HashSet<Node> unq: uniqueElementsDup){


			for(Node u: unq){
				if( getTableMapping().containsValue(u.getTableNameNo()) ){//If this node is mapped
					HashSet<Node> unqDup = new HashSet<Node>();
					unqDup.addAll(unq);

					uniqueElements.remove(unqDup);

					Node uDup = new Node(u);

					//get the column
					String column = u.getColumn().getColumnName();
					//get the original string that contains original table name and column name
					String originalName = getColumnMapping().get(column);
					String tableNameNo = originalName.split("\\.")[0];
					String columnName = originalName.split("\\.")[1];

					uDup.setTableNameNo(tableNameNo);
					uDup.getColumn().setColumnName(columnName);

					unqDup.remove(u);
					unqDup.add(uDup);

					uniqueElements.add(unqDup);

				}
			}
		}*/

	}

	public String getDefaultRootNode() {
		return defaultRootNode;
	}

	public void setDefaultRootNode(String defaultRootNode) {
		this.defaultRootNode = defaultRootNode;
	}

	public Set<Node> getSingleValuedAttributesDup() {
		return singleValuedAttributesDup;
	}

	public void setSingleValuedAttributesDup(Set<Node> singleValuedAttributesDup) {
		this.singleValuedAttributesDup = singleValuedAttributesDup;
	}

	public HashSet<HashSet<Node>> getUniqueElementsDup() {
		return uniqueElementsDup;
	}

	public void setUniqueElementsDup(HashSet<HashSet<Node>> uniqueElementsDup) {
		this.uniqueElementsDup = uniqueElementsDup;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

}

