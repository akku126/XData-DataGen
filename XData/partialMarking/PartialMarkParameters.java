package partialMarking;

public class PartialMarkParameters {

	private int Relation;
	
public int AssignmentId;

public int QuestionId;

public int QueryId;

	private int Predicate =1;
	
	private int Projection=1;
	
	private int Joins =1;
	
	private int WhereSubQueries=1;
	
	private int FromSubQueries=1;
	
	private int OuterQuery=1;
	
	private int GroupBy=1;

	private int HavingClause=1;
	
	private int SubQConnective=1;
	
	private int Aggregates=1;
	
	private int SetOperators=1;
	
	private int Distinct=1;
	/**
	 * @return the predicate
	 */
	public int getPredicate() {
		return Predicate;
	}

	/**
	 * @param predicate the predicate to set
	 */
	public void setPredicate(int predicate) {
		Predicate = predicate;
	}

	/**
	 * @return the projection
	 */
	public int getProjection() {
		return Projection;
	}

	/**
	 * @param projection the projection to set
	 */
	public void setProjection(int projection) {
		Projection = projection;
	}

	/**
	 * @return the joins
	 */
	public int getJoins() {
		return Joins;
	}

	/**
	 * @param joins the joins to set
	 */
	public void setJoins(int joins) {
		Joins = joins;
	}

	/**
	 * @return the whereSubQueries
	 */
	public int getWhereSubQueries() {
		return WhereSubQueries;
	}

	/**
	 * @param whereSubQueries the whereSubQueries to set
	 */
	public void setWhereSubQueries(int whereSubQueries) {
		WhereSubQueries = whereSubQueries;
	}

	/**
	 * @return the fromSubQueries
	 */
	public int getFromSubQueries() {
		return FromSubQueries;
	}

	/**
	 * @param fromSubQueries the fromSubQueries to set
	 */
	public void setFromSubQueries(int fromSubQueries) {
		FromSubQueries = fromSubQueries;
	}

	/**
	 * @return the outerQuery
	 */
	public int getOuterQuery() {
		return OuterQuery;
	}

	/**
	 * @param outerQuery the outerQuery to set
	 */
	public void setOuterQuery(int outerQuery) {
		OuterQuery = outerQuery;
	}

	/**
	 * @return the groupBy
	 */
	public int getGroupBy() {
		return GroupBy;
	}

	/**
	 * @param groupBy the groupBy to set
	 */
	public void setGroupBy(int groupBy) {
		GroupBy = groupBy;
	}

	/**
	 * @return the relation
	 */
	public int getRelation() {
		return Relation;
	}

	/**
	 * @param relation the relation to set
	 */
	public void setRelation(int relation) {
		Relation = relation;
	}

	/**
	 * @return the havingClause
	 */
	public int getHavingClause() {
		return HavingClause;
	}

	/**
	 * @param havingClause the havingClause to set
	 */
	public void setHavingClause(int havingClause) {
		HavingClause = havingClause;
	}

	/**
	 * @return the subQConnective
	 */
	public int getSubQConnective() {
		return SubQConnective;
	}

	/**
	 * @param subQConnective the subQConnective to set
	 */
	public void setSubQConnective(int subQConnective) {
		SubQConnective = subQConnective;
	}

	/**
	 * @return the aggregates
	 */
	public int getAggregates() {
		return Aggregates;
	}

	/**
	 * @param aggregates the aggregates to set
	 */
	public void setAggregates(int aggregates) {
		Aggregates = aggregates;
	}

	/**
	 * @return the setOperators
	 */
	public int getSetOperators() {
		return SetOperators;
	}

	/**
	 * @param setOperators the setOperators to set
	 */
	public void setSetOperators(int setOperators) {
		SetOperators = setOperators;
	}

	/**
	 * @return the distinct
	 */
	public int getDistinct() {
		return Distinct;
	}

	/**
	 * @param distinct the distinct to set
	 */
	public void setDistinct(int distinct) {
		this.Distinct = distinct;
	}
	
	public Integer getValue(){
		int total = 0;
	
		return (this.Projection+this.Joins+this.WhereSubQueries+this.FromSubQueries+this.OuterQuery+this.HavingClause
				+this.SubQConnective+this.Aggregates+this.SetOperators+this.Distinct);
		
	}
	
}
