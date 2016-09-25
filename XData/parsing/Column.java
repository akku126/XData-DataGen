package parsing;
import java.io.Serializable;
import java.util.Vector;

import parsing.Table;

/**
 *
 * @author Bhanu Pratap Gupta
 * modified by Sandeep Patidar
 */
public class Column	implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5747121620303960746L;
	private String columnName;
	private String columnAliasName;
	private int dataType;
	private String tableName;
	private Table table;
	private int columnSize;
	private boolean isNullable;
	//private boolean isPartOfPrimaryKey;
	private boolean isUnique;
	private boolean references=false;
	private String referenceTableName;
	private Column referenceColumn;
	private boolean isAutoIncrement=false;
	private double minVal, maxVal;
	private String cvcDatatype;
	private int precision, scale;

	///////// Vector for getting values from database
	private Vector<String> columnValues ;

	public Column(String columnName, String tableName){
		this.columnName = columnName;
		this.tableName = tableName;
		minVal = -1;
		maxVal = -1;
	}

	/**
	 * Copy constructor. Added by Mahesh
	 */
	public Column (Column col){
		if(col == null)
			return ;
		if(col.getColumnName() == null)
			this.columnName = null;
		else
			this.columnName = new String(col.getColumnName());
		if(col.getAliasName() == null)
			this.columnAliasName =null;
		else
			this.columnAliasName = new String(col.getAliasName());
		this.dataType  = col.getDataType();
		if(col.getTableName() == null)
			this.tableName = null;
		else
			this.tableName = new String(col.getTableName());
		/*if(col.getTable() == null)
			this.table = null;
		else
			this.table = new Table(col.getTable());*/
		this.table = col.getTable();
		this.columnSize = col.getColumnSize();
		this.isNullable = col.isNullable();
		this.isUnique = col.isUnique();
		this.references = col.references();
		if(col.getReferenceTableName() == null)
			this.referenceTableName = null;
		else
			this.referenceTableName = new String(col.getReferenceTableName());
		if(col.getReferenceColumn() == null)
			this.referenceColumn = null;
		else
			this.referenceColumn = new Column(col.getReferenceColumn());
		this.isAutoIncrement = col.isAutoIncement();
		this.minVal = col.getMinVal();
		this.maxVal = col.getMaxVal();

		if(col.getCvcDatatype() == null)
			this.cvcDatatype = null;
		else
			this.cvcDatatype = new String(col.getCvcDatatype());
		if(col.getColumnValues() == null)
			this.columnValues = null;
		else
			this.columnValues = new Vector<String>(col.getColumnValues());


	}
	public void setMinVal(double minVal){
		this.minVal = minVal;
	}

	public void setMaxVal(double maxVal){
		this.maxVal = maxVal;
	}

	public double getMinVal(){
		return this.minVal;
	}

	public double getMaxVal(){
		return this.maxVal;
	}


	public Column(String columnName, Table table){
		this.columnName = columnName;
		this.table = table;
		setTableName(table.getTableName());
	}

	public void intializeColumnValuesVector(){
		this.columnValues = new Vector<String>();
	}

	public boolean addColumnValues(String value){
		if(!columnValues.contains(value)){
			this.columnValues.add(value);
			return true;
		}else
			return false;
	}

	public Vector<String> getColumnValues(){
		return this.columnValues;
	}

	@Override
	public String toString(){
		return columnName;
	}        

	public String getColumnName(){
		return columnName;
	}

	public void setColumnName(String columnName){
		this.columnName = columnName;
	}
	public String getAliasName() {
		return columnAliasName;
	}

	public void setAliasName(String columnAliasName) {
		this.columnAliasName = columnAliasName;
	}

	public int getDataType(){
		return dataType;
	}

	public void setDataType(int dataType){
		this.dataType = dataType; 
	}

	public Table getTable(){
		return table;
	}

	public String getTableName(){
		return tableName;
	}

	public void setTableName(String tableName){
		this.tableName = tableName;
	}

	public int getColumnSize(){
		return columnSize;                      
	}

	public void setColumnSize(int columnSize){
		this.columnSize = columnSize;                      
	}

	public boolean isNullable(){
		return isNullable;
	}

	public void setIsNullable(boolean isNullable){
		this.isNullable = isNullable;
	}

	/*public boolean isPartOfPrimaryKey(){
          return isPartOfPrimaryKey;
      }
      public void setIsPartOfPrimaryKey(boolean isPartOfPrimaryKey){
          this.isPartOfPrimaryKey = isPartOfPrimaryKey;
      }*/
	public boolean isUnique(){
		return isUnique;
	}

	public void setIsUnique(boolean isUnique){
		this.isUnique = isUnique;
	}

	public void setReferenceColumn(Column referenceColumn){
		this.referenceColumn = referenceColumn;
	}

	public Column getReferenceColumn(){
		return referenceColumn;
	}

	public String getReferenceTableName(){
		return referenceTableName;
	}

	public void setReferenceTableName(String referenceTableName){
		this.references = true;
		this.referenceTableName = referenceTableName;
	}

	public boolean references(){
		return references;
	}

	public boolean isAutoIncement(){
		return isAutoIncrement;
	}

	public void setIsAutoIncement(boolean isAutoIncrement){
		this.isAutoIncrement = isAutoIncrement;
	}

	public String getCvcDatatype() {
		return cvcDatatype;
	}

	public void setCvcDatatype(String cvcDatatype) {
		this.cvcDatatype = cvcDatatype;
	}
	
	public int getPrecision() {
		return this.precision;
	}

	public void setPrecision(int  precision) {
		this.precision = precision;
	}
	
	public int getScale() {
		return this.scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	//Below methods are added by Mahesh
	@Override
	public int hashCode() {

	//	System.out.println("hashcode for column: " + this);
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((columnAliasName == null) ? 0 : columnAliasName.hashCode());

	//	System.out.println("columnalias hashcode: " + ((columnAliasName == null) ? 0 : columnAliasName.hashCode()));

		result = prime * result
				+ ((columnName == null) ? 0 : columnName.hashCode());

	//	System.out.println("columnName hashcode: " + ((columnName == null) ? 0 : columnName.hashCode()));

		result = prime * result + columnSize;
		//result = prime * result	+ ((columnValues == null) ? 0 : columnValues.hashCode());

	//	System.out.println("column values hashcode: " + ((columnValues == null) ? 0 : columnValues.hashCode()));

	/*	result = prime * result
				+ ((cvcDatatype == null) ? 0 : cvcDatatype.hashCode());

		System.out.println("cvc datatype hashcode: " + ((cvcDatatype == null) ? 0 : cvcDatatype.hashCode()));

		result = prime * result + dataType;
		result = prime * result + (isAutoIncrement ? 1231 : 1237);
		result = prime * result + (isNullable ? 1231 : 1237);
		result = prime * result + (isUnique ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(maxVal);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minVal);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((referenceColumn == null) ? 0 : referenceColumn.hashCode());

		System.out.println("reference column  hashcode: " + ((referenceColumn == null) ? 0 : referenceColumn.hashCode()));*/

		result = prime
				* result
				+ ((referenceTableName == null) ? 0 : referenceTableName
						.hashCode());

	//	System.out.println("refrence table hashcode: " + ((referenceTableName == null) ? 0 : referenceTableName.hashCode()));

		/*result = prime * result + (references ? 1231 : 1237);
		result = prime * result + ((table == null) ? 0 : table.hashCode());

		System.out.println("Table hashcode: " + ((table == null) ? 0 : table.hashCode()));*/

		result = prime * result
				+ ((tableName == null) ? 0 : new Integer(tableName.hashCode()).hashCode());

//		System.out.println("tableName hashcode: " + ((tableName == null) ? 0 : tableName.hashCode()));

		//return new Integer(result).hashCode();
//		System.out.println("Value for column: " + result);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Column other = (Column) obj;
		if (columnAliasName == null) {
			if (other.columnAliasName != null)
				return false;
		} else if (!columnAliasName.equals(other.columnAliasName))
			return false;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (columnSize != other.columnSize)
			return false;
		if (columnValues == null) {
			if (other.columnValues != null)
				return false;
		} else if (!columnValues.equals(other.columnValues))
			return false;
		if (cvcDatatype == null) {
			if (other.cvcDatatype != null)
				return false;
		} else if (!cvcDatatype.equals(other.cvcDatatype))
			return false;
		if (dataType != other.dataType)
			return false;
		if (isAutoIncrement != other.isAutoIncrement)
			return false;
		if (isNullable != other.isNullable)
			return false;
		if (isUnique != other.isUnique)
			return false;
		if (Double.doubleToLongBits(maxVal) != Double
				.doubleToLongBits(other.maxVal))
			return false;
		if (Double.doubleToLongBits(minVal) != Double
				.doubleToLongBits(other.minVal))
			return false;
		if (referenceColumn == null) {
			if (other.referenceColumn != null)
				return false;
		} else if (!referenceColumn.equals(other.referenceColumn))
			return false;
		if (referenceTableName == null) {
			if (other.referenceTableName != null)
				return false;
		} else if (!referenceTableName.equals(other.referenceTableName))
			return false;
		if (references != other.references)
			return false;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}

}