package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import parsing.JoinClauseInfo;
import parsing.Node;
import partialMarking.QueryData;

public class SerializeXML {
	public static PrintWriter  out;
	public static int idCounter=1;
	public static String spaceTab="    ";
	
	public static void serializeXML(String fileName, QueryData qData) throws IOException, CloneNotSupportedException{
		  out = new PrintWriter(new FileWriter(fileName));
		  printHead();
		  printHasDistinct(qData);
		  printProjectedColumns(qData);
		  printJoinConditions(qData);
		  printSelectionConditions(qData);
		  printGroupByColumns(qData);
		  printHavingConditions(qData);
		  printJoinTables(qData);
		  printRedundantTables(qData);
		  printSubqueryConnectives(qData);
		  printTail();
		  out.flush();
		  out.close();
	}
	public static void printProjectedColumns(QueryData qData){
		 out.println("<item text=\"Projected Columns\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(parsing.Node n:toSetOfNodes(qData.getProjectionList())){
			 out.println(spaceTab+"<item text=\""+ n.toString() +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
	}
	
	public static void printHasDistinct(QueryData qData){
		 out.println("<item text=\"Distinct Present\" open=\"1\" id=\""+ idCounter++ +"\">");
		if(qData.hasDistinct){
			 out.println(spaceTab+"<item text=\"True\" id=\""+ idCounter++ +"\"/>");
		 }
		else{
			out.println(spaceTab+"<item text=\"False\" id=\""+ idCounter++ +"\"/>");
		}
		 out.println("</item>");
	}
	
	public static Set<Node> toSetOfNodes(List<Node> nodes){
		Set<Node> tempSet=new HashSet<Node>();
		for(Node n:nodes)
			tempSet.add(n);
		return tempSet;
	}
	
	public static parsing.Node cloneNodeForXMLserialization(parsing.Node m) throws CloneNotSupportedException{
		parsing.Node n=m.clone();
		if(n.getOperator().equals("<")){
			n.setOperator("&lt;");
		}
		else if(n.getOperator().equals("<=")){
			n.setOperator("&lt;=");
		}
		return n;
		
	}
	
	public static void printJoinConditions(QueryData qData) throws CloneNotSupportedException{
		 out.println("<item text=\"Join Conditions\" open=\"1\" id=\""+ idCounter++ +"\">");
		 out.println("<item text=\"Outer\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(parsing.Node n:toSetOfNodes(qData.getJoinConditions())){
			 if(n.getJoinType().equals(JoinClauseInfo.leftOuterJoin)
					 ||n.getJoinType().equals(JoinClauseInfo.rightOuterJoin)
					 ||n.getJoinType().equals(JoinClauseInfo.fullOuterJoin))
			 out.println(spaceTab+"<item text=\""+ cloneNodeForXMLserialization(n).toString() +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
		 out.println("<item text=\"Inner\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(parsing.Node n:toSetOfNodes(qData.getJoinConditions())){
			 if(!n.getJoinType().equals(JoinClauseInfo.leftOuterJoin)
					 && !n.getJoinType().equals(JoinClauseInfo.rightOuterJoin)
					 && !n.getJoinType().equals(JoinClauseInfo.fullOuterJoin))

			 out.println(spaceTab+"<item text=\""+ cloneNodeForXMLserialization(n).toString() +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
		 out.println("</item>");
	}
	public static void printSelectionConditions(QueryData qData) throws  CloneNotSupportedException{
		 out.println("<item text=\"Selection Conditions\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(parsing.Node n:toSetOfNodes(qData.getSelectionConditions())){
			 out.println(spaceTab+"<item text=\""+ cloneNodeForXMLserialization(n).toString() +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
	}
	
	public static void printSubqueryConnectives(QueryData qData){
		
		 out.println("<item text=\"Subquery Connectives\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(String str:qData.getSubQConnectives()){
			 out.println(spaceTab+"<item text=\""+ str +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
	}
	
	public static void printHavingConditions(QueryData qData) throws CloneNotSupportedException{
		 out.println("<item text=\"Having Conditions\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(parsing.Node n:toSetOfNodes(qData.getHavingClause())){
			 out.println(spaceTab+"<item text=\""+ cloneNodeForXMLserialization(n).toString() +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
	}
	
	public static void printGroupByColumns(QueryData qData) {
		 out.println("<item text=\"GroupBy Columns\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(parsing.Node n:toSetOfNodes(qData.GroupByNodes)){
			 out.println(spaceTab+"<item text=\""+ n.toString() +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
	}
	
	public static void printJoinTables(QueryData qData){
		 out.println("<item text=\"Tables\" open=\"1\" id=\""+ idCounter++ +"\">");
		 for(String str:qData.getRelations()){
			 out.println(spaceTab+"<item text=\""+ str +"\" id=\""+ idCounter++ +"\"/>");
		 }
		 out.println("</item>");
	}
	
	public static void printRedundantTables(QueryData qData){
		 out.println("<item text=\"Redundant Tables\" open=\"1\" id=\""+ idCounter++ +"\">");
		 if(qData.RedundantRelations!=null){
			 for(String str:qData.RedundantRelations){
				 out.println(spaceTab+"<item text=\""+ str +"\" id=\""+ idCounter++ +"\"/>");
			 }
		 }
		 out.println("</item>");
	}
	
	public static void printHead(){
		out.println("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
		out.println("<tree id=\"0\">");
	
	}
	
	public static void printTail(){
		out.println("</tree>");
	}

}
