package testDataGen;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

import parsing.Node;
import parsing.Query;
import parsing.QueryStructure;
import parsing.Table;

/**
 * This class stores details about branch queries
 * @author mahesh
 *
 */
/**
 * FIXME: GOOD DOC
 * @author mahesh
 *
 */
public class BranchQueriesDetails implements Serializable{

	private static Logger logger = Logger.getLogger(BranchQueriesDetails.class.getName());
	private static final long serialVersionUID = -6782217390261516398L;

	/**the number of branch queries in the input*/
	private int noOfBranchQueries;
	
	private String[] branchQueryString; 
	private ArrayList<String>[] branchResultString;
	private ArrayList<String>[] branchOperators;
	private QueryStructure qStructure1[];
	private Query branchQuery[] = null;
	private ArrayList<Node> allCondsForBranchQuery[];
	private ArrayList<Node> selectionCondsForBranchQuery[];
	private ArrayList<Node> stringSelectionCondsForBranchQuery[];
	private ArrayList<Node> isNullCondsForBranchQuery[];
	private ArrayList<Node> likeCondsForBranchQuery[];
	private HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[];
	
	

	public int getNoOfBranchQueries() {
		return noOfBranchQueries;
	}

	public void setNoOfBranchQueries(int noOfBranchQueries) {
		this.noOfBranchQueries = noOfBranchQueries;
	}

	public String[] getBranchQueryString() {
		return branchQueryString;
	}

	public void setBranchQueryString(String[] branchQueryString) {
		this.branchQueryString = branchQueryString;
	}

	public ArrayList<String>[] getBranchResultString() {
		return branchResultString;
	}

	public void setBranchResultString(ArrayList<String>[] branchResultString) {
		this.branchResultString = branchResultString;
	}

	public ArrayList<String>[] getBranchOperators() {
		return branchOperators;
	}

	public void setBranchOperators(ArrayList<String>[] branchOperators) {
		this.branchOperators = branchOperators;
	}



	public QueryStructure[] getqStructure1() {
		return qStructure1;
	}


	public void setqStructure1(QueryStructure[] qStructure1) {
		this.qStructure1 = qStructure1;
	}


	public Query[] getBranchQuery() {
		return branchQuery;
	}


	public void setBranchQuery(Query[] branchQuery) {
		this.branchQuery = branchQuery;
	}


	public ArrayList<Node>[] getAllCondsForBranchQuery() {
		return allCondsForBranchQuery;
	}


	public void setAllCondsForBranchQuery(ArrayList<Node>[] allCondsForBranchQuery) {
		this.allCondsForBranchQuery = allCondsForBranchQuery;
	}


	public ArrayList<Node>[] getSelectionCondsForBranchQuery() {
		return selectionCondsForBranchQuery;
	}


	public void setSelectionCondsForBranchQuery(
			ArrayList<Node>[] selectionCondsForBranchQuery) {
		this.selectionCondsForBranchQuery = selectionCondsForBranchQuery;
	}


	public ArrayList<Node>[] getStringSelectionCondsForBranchQuery() {
		return stringSelectionCondsForBranchQuery;
	}


	public void setStringSelectionCondsForBranchQuery(
			ArrayList<Node>[] stringSelectionCondsForBranchQuery) {
		this.stringSelectionCondsForBranchQuery = stringSelectionCondsForBranchQuery;
	}


	public ArrayList<Node>[] getIsNullCondsForBranchQuery() {
		return isNullCondsForBranchQuery;
	}


	public void setIsNullCondsForBranchQuery(
			ArrayList<Node>[] isNullCondsForBranchQuery) {
		this.isNullCondsForBranchQuery = isNullCondsForBranchQuery;
	}


	public ArrayList<Node>[] getLikeCondsForBranchQuery() {
		return likeCondsForBranchQuery;
	}


	public void setLikeCondsForBranchQuery(ArrayList<Node>[] likeCondsForBranchQuery) {
		this.likeCondsForBranchQuery = likeCondsForBranchQuery;
	}


	public HashMap<Table, Integer>[] getNoOfTuplesAddedToTablesForBranchQueries() {
		return noOfTuplesAddedToTablesForBranchQueries;
	}


	public void setNoOfTuplesAddedToTablesForBranchQueries(
			HashMap<Table, Integer>[] noOfTuplesAddedToTablesForBranchQueries) {
		this.noOfTuplesAddedToTablesForBranchQueries = noOfTuplesAddedToTablesForBranchQueries;
	}
	
}
