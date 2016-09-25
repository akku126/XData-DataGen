package partialMarking;

import java.util.Comparator;

import parsing.Node;

public class NodeComparator implements Comparator<Node>{

	@Override
	public int compare(Node o1, Node o2) {
		
		if(!o1.getTable().getTableName().equals(o2.getTable().getTableName()))		
			return o1.getTable().getTableName().compareTo(o2.getTable().getTableName());
		else 
			return o1.getColumn().getColumnName().compareTo(o2.getColumn().getColumnName());
	}

}
