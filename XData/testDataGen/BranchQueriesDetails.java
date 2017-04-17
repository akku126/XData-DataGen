package testDataGen;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.Node;
import parsing.Query;
import parsing.QueryParser;
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
	private QueryParser qParser1[];
	private QueryStructure qStructure1[];
	private Query branchQuery[] = null;
	private ArrayList<Node> allCondsForBranchQuery[];
	private ArrayList<Node> selectionCondsForBranchQuery[];
	private ArrayList<Node> stringSelectionCondsForBranchQuery[];
	private ArrayList<Node> isNullCondsForBranchQuery[];
	private ArrayList<Node> likeCondsForBranchQuery[];
	private HashMap<Table, Integer> noOfTuplesAddedToTablesForBranchQueries[];
	
	
	public void intitializeDetails(GenerateCVC1 cvc) throws Exception{
		
		/*qParser1 = new QueryParser[noOfBranchQueries];
		
		branchQuery = new Query[noOfBranchQueries];
		
		allCondsForBranchQuery = new ArrayList[noOfBranchQueries];
		
		selectionCondsForBranchQuery = new ArrayList[noOfBranchQueries];
		
		likeCondsForBranchQuery = new ArrayList[noOfBranchQueries];
		for(int i = 0; i < noOfBranchQueries; i++)
		{
			allCondsForBranchQuery[i] = new ArrayList<Node>();
			
			selectionCondsForBranchQuery[i] = new ArrayList<Node>();
			
			likeCondsForBranchQuery[i] = new ArrayList<Node>();
			
			if(branchQueryString[i] != null && !branchQueryString[i].equals(""))
			{
				qParser1[i] = new QueryParser( cvc.getTableMap() );
				
				qParser1[i].parseQuery("q1" + i, branchQueryString[i]);
				
				branchQuery[i] = qParser1[i].getQuery();
				
				allCondsForBranchQuery[i].addAll(qParser1[i].getAllConds());
				
				selectionCondsForBranchQuery[i].addAll(qParser1[i].getSelectionConds());
				
				likeCondsForBranchQuery[i].addAll(qParser1[i].getLikeConds());
			}
		}*/
		
		qStructure1 = new QueryStructure[noOfBranchQueries];
		
		branchQuery = new Query[noOfBranchQueries];
		
		allCondsForBranchQuery = new ArrayList[noOfBranchQueries];
		
		selectionCondsForBranchQuery = new ArrayList[noOfBranchQueries];
		 
		likeCondsForBranchQuery = new ArrayList[noOfBranchQueries];
		for(int i = 0; i < noOfBranchQueries; i++)
		{
			allCondsForBranchQuery[i] = new ArrayList<Node>();
			
			selectionCondsForBranchQuery[i] = new ArrayList<Node>();
			
			likeCondsForBranchQuery[i] = new ArrayList<Node>();
			
			if(branchQueryString[i] != null && !branchQueryString[i].equals(""))
			{
				qParser1[i] = new QueryParser( cvc.getTableMap() );
				//Application Testing - pass the new parameter
				
				qParser1[i].parseQuery("q1" + i, branchQueryString[i],cvc.getDBAppparams());
				
				branchQuery[i] = qParser1[i].getQuery();
				
				allCondsForBranchQuery[i].addAll(qParser1[i].getAllConds());
				
				selectionCondsForBranchQuery[i].addAll(qParser1[i].getSelectionConds());
				
				likeCondsForBranchQuery[i].addAll(qParser1[i].getLikeConds());
			}
		}

		logger.log(Level.FINE,"QueryParser foreignKeysModified = " + cvc.getForeignKeysModified());
		
	}


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


	public QueryParser[] getqParser1() {
		return qParser1;
	}


	public void setqParser1(QueryParser[] qParser1) {
		this.qParser1 = qParser1;
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
