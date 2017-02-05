package parsing;

import java.util.Comparator;

import parsing.Node;


public class NodeComparator implements Comparator<Node>{

	@Override
	public int compare(Node o1, Node o2) {
		
		if(!o1.getTableNameNo().equals(o2.getTableNameNo()))		
			return o1.getTableNameNo().compareTo(o2.getTableNameNo());
		else 
			return o1.getColumn().getColumnName().compareTo(o2.getColumn().getColumnName());
	}

}
