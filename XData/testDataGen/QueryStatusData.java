package testDataGen;

public class QueryStatusData {
	
	// Enum representing the status of running the query against dataset
	public enum QueryStatus {
		Error,
		NoDataset,
		Incorrect,
		Correct
	}
	
	public String ErrorMessage;
	public QueryStatus Status;
	public float marks;
	
}