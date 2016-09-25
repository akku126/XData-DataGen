package partialMarking;

import java.io.Serializable;
import java.util.ArrayList;

public class MarkInfo implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public float Marks;
	
	public int AssignmentId;
	
	public int QuestionId;
	
	public String StudentQuery;
	
	public String InstructorQuery;
	
	public ArrayList<QueryInfo> SubqueryData;
	
	public PartialMarkerConfig Configuration;
	
	public MarkInfo(){
		this.Marks = 0;
		this.SubqueryData = new ArrayList<QueryInfo>();
		this.Configuration = new PartialMarkerConfig();
	}
}
