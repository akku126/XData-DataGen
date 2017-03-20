package util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import parsing.*;

/**
 * Generic Directed/Undirected Weighted Graph.
 * Multiple edges can be present between pair of vertices but should be of different weights
 * @author Bhanu Pratap Gupta
 */
public class Graph<Vertex,Edge> implements Serializable{
    
    /**
	 * 
	 */
	private static Logger logger = Logger.getLogger(Graph.class.getName());
	private static final long serialVersionUID = 4741992949281148008L;
	/**
     * The implementation here is basically an adjacency list, but instead
     * of an array of lists, a Map is used to map each vertex to its list of 
     * adjacent vertices.
     */   
	private boolean isDirected;
    private Map<Vertex,Map<Vertex,Vector<Edge>>> neighbours = new LinkedHashMap<Vertex,Map<Vertex,Vector<Edge>>>();
    private Vector<Vertex> vertices = new Vector<Vertex>();
    
    
    /**
     * Constructor
     * @param isDirected
     */
    public Graph(boolean isDirected) {
	this.isDirected = isDirected;
	}
    
    /**
     * String representation of graph.
     */
    @Override
	public String toString () {
        StringBuffer s = new StringBuffer();
        for (int i =0; i<vertices.size(); i++){
        	Vertex v = vertices.get(i);
        	s.append("\n    " + v + " <- " + neighbours.get(v));
        } 
        return s.toString();                
    }
    
    /**
     * @return all the vertices of the graph
     */
    public Vector<Vertex> getAllVertex(){
    	return vertices;
    }
    
    /**
     * @return single vertex 
     */
    public Vertex getVertex(int i){
    	return vertices.get(i);
    }
    
    /**
     * @return size of the graph in term of no. of nodes
     */
    public int size(){
    	return vertices.size();
    }
    
    /**
     * @param vertex
     * @return all the edges connected to the input vertex 
     */
    public Map<Vertex,Vector<Edge>> getNeighbours(Vertex vertex){
    	return neighbours.get(vertex);
    }
    
    /**
     * @param vertex1
     * @param vertex2
     * @return All edges between the two vertex
     */
    public Vector<Edge> getEdges(Vertex vertex1, Vertex vertex2){
    	return neighbours.get(vertex1).get(vertex2);
    }
    
    /**
     * Add a vertex to the graph.  Nothing happens if vertex is already in graph.
     */
    public void addVertex (Vertex vertex) {
        if (vertices.contains(vertex)) return;
        vertices.add(vertex);
        neighbours.put(vertex, new LinkedHashMap<Vertex,Vector<Edge>>());
    }
    
    /**
     * Add an edge to the graph; if either vertex does not exist, it's added.
     * This implementation allows the creation of multi-edges and self-loops.
     */
    public void add (Vertex from, Vertex to, Edge edge) {
        this.addVertex(from);
        this.addVertex(to);
        
        Vector<Edge> edges = neighbours.get(from).get(to);
        if(edges == null)
        	edges = new Vector<Edge>();
        
        if(!edges.contains(edge)){
        	edges.add(edge);
            neighbours.get(from).put(to,edges);
            if(!isDirected)
            	neighbours.get(to).put(from,edges);
        }
    }
    
    /**
     * @return empty subGraph 
     */
    public Graph<Vertex, Edge> createSubGraph(){
    	Graph<Vertex, Edge> subGraph = new Graph<Vertex, Edge>(this.isDirected);
    	return subGraph;
    }
    
    /**
     * Add a vertex and edges to the subgraph from it's original graph
     * @param originalGraph
     * @param vertex
     */
    public void add (Graph<Vertex,Edge> originalGraph, Vertex vertex) {
        if (originalGraph.contains(vertex))
        	this.addVertex(vertex);
        for(Vertex from: this.getAllVertex()){
        	for(Vertex to : originalGraph.getNeighbours(from).keySet()){
        		if(this.neighbours.containsKey(to)){
        			Vector<Edge> edges = originalGraph.getNeighbours(from).get(to);
        			this.neighbours.get(from).put(to, edges);
            		if(!isDirected)
            			this.neighbours.get(to).put(from,edges);
            	}
        	}
        }
    }
    
       
    /**
     * True iff graph contains vertex.
     */
    public boolean contains (Vertex vertex) {
        return neighbours.containsKey(vertex);
    }
    
    /**
     * Remove an edge from the graph.  Nothing happens if no such edge.
     * @throws IllegalArgumentException if either vertex doesn't exist.
     */
    public void remove (Vertex from, Vertex to) {
        if (!(this.contains(from) && this.contains(to)))
            throw new IllegalArgumentException("Nonexistent vertex");
        neighbours.get(from).remove(to);
        if(!isDirected)
        	neighbours.get(to).remove(from);
    }
    
    /**
     * Report (as a Map) the out-degree of each vertex.
     */
    public Map<Vertex,Integer> outDegree () {
        Map<Vertex,Integer> result = new LinkedHashMap<Vertex,Integer>();
        for (Vertex v: neighbours.keySet()) result.put(v, neighbours.get(v).size());
        return result;
    }
    
    /**
     * Report (as a Map) the in-degree of each vertex.
     */
    public Map<Vertex,Integer> inDegree () {
        Map<Vertex,Integer> result = new LinkedHashMap<Vertex,Integer>();
        for(Vertex v: neighbours.keySet())
        	result.put(v, 0);      							 // Set all in-degrees as 0
        for(Vertex from: neighbours.keySet()) {
            for (Vertex to: neighbours.get(from).keySet()) {
                result.put(to, result.get(to) + 1);           // Increment in-degree
            }
        }
        return result;
    }
    
    /**
     * Report (as a Vector) the topological sort of the vertices; null for no such sort.
     */
    public Vector<Vertex> topSort () {
        Map<Vertex, Integer> degree = inDegree();
        // Determine all vertices with zero in-degree
        Stack<Vertex> zeroVerts = new Stack<Vertex>();        // Stack as good as any here
        for (Vertex v: degree.keySet()) {
            if (degree.get(v) == 0) zeroVerts.push(v);
        }
        // Determine the topological order
        Vector<Vertex> result = new Vector<Vertex>();
        while (!zeroVerts.isEmpty()) {
            Vertex v = zeroVerts.pop();                  // Choose a vertex with zero in-degree
            result.add(v);                        		 // Vertex v is next in topol order
            // "Remove" vertex v by updating its neighbors
            for (Vertex neighbor: neighbours.get(v).keySet()) {
                degree.put(neighbor, degree.get(neighbor) - 1);
                // Remember any vertices that now have zero in-degree
                if (degree.get(neighbor) == 0) zeroVerts.push(neighbor);
            }
        }
        // Check that we have used the entire graph (if not, there was a cycle)
        if (result.size() != neighbours.size()) return null;
        return result;
    }
    
    /**
     * Mark vertex visited recursively 
     * @param v
     * @param visited
     */
    private void MarkVisited(Vertex v, Map<Vertex,Boolean> visited){
    	visited.put(v, true);
    	for (Vertex to: neighbours.get(v).keySet()) {
    		if(visited.get(to)==false)
    			MarkVisited(to,visited);
        }
    }
    
    /**
     * Find whether graph is connected or not 
     */    
    public boolean IsConnected(){
    	boolean connected = true;
    	Map<Vertex,Boolean> visited = new LinkedHashMap<Vertex,Boolean>();
    	for (Vertex vertex: this.getAllVertex()){
    		for (Vertex v: this.getAllVertex()){
    			visited.put(v, false);
    		}
    		connected = true;
    		MarkVisited(vertex,visited);
    		for (Vertex v: this.getAllVertex()){
    			if(visited.get(v)==false){
        			connected = false;
        			break;
        		}
    		}
    		if(connected)
    			return connected;
    	}
    	
    	return connected;
    }
    
    /**
     * True iff graph is a DAG (directed acyclic graph).
     */
    public boolean isDag () {
        return topSort() != null;
    }
    
    /**
     * Main program (for testing).
     */
    public static void main (String[] args) {
        Graph<Table,Integer> graph = new Graph<Table,Integer>(true);
        Table a = new Table("A");
        Table b = new Table("B");
        Table c = new Table("C");
        Table d = new Table("D");
        
        graph.add(a,d,1);
        graph.add(a,b,5);
        graph.add(a,c,2);
        graph.add(c,d,2);
        
        
        Graph<Table,Integer> subGraph = graph.createSubGraph();
        //subGraph.add(graph,a);
        subGraph.add(graph,c);
        subGraph.add(graph,d);
        
        logger.log(Level.INFO,"Original graph: " + graph);
        logger.log(Level.INFO,"Graph is connected : "+graph.IsConnected());
        logger.log(Level.INFO,"\nSub graph: " + subGraph);
        logger.log(Level.INFO,"SubGraph is connected : "+subGraph.IsConnected());
    }
}



