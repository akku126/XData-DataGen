package parsing;

import java.io.Serializable;
import java.util.Vector;

import parsing.Table;
/**
 * 
 * @author Bhanu Pratap Gupta
 *
 */
public class ForeignKey implements Serializable{
	
	
	private static final long serialVersionUID = 5177685982626414427L;
	private String fkName;
	private String fkTableName;
	private Table referenceTable;
	private Vector<Column> fKey;
	private Vector<Column> referenceKey;
	
	public ForeignKey(String fkName) {
		this.fkName = fkName;
		fKey = new Vector<Column>();
		referenceKey = new Vector<Column>();
	}
	
	public void addFKeyColumn(Column col, int sequence){
		fkTableName = col.getTableName();
		fKey.insertElementAt(col, sequence-1);
	}
	
	public void addReferenceKeyColumn(Column col, int sequence){
		referenceKey.insertElementAt(col, sequence-1);
	}
	
	public String getKeyName(){
		return fkName;
	}
	
	public String getFKTablename(){
		return fkTableName;
	}
	
	
	public Table getReferenceTable(){
		return referenceTable;
	}
	
	public void setReferenceTable(Table referenceTable){
		this.referenceTable = referenceTable;
	}
	
	public Vector<Column> getFKeyColumns(){
		return fKey;
	}
	
	public Vector<Column> getReferenceKeyColumns(){
		return referenceKey;
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append("(");
		for(int i=0; i<fKey.size(); i++){
			Column col = fKey.get(i);
			if(i<fKey.size()-1)
				s.append(col.getColumnName()+" and ");
			else
				s.append(col.getColumnName());
		}
		s.append(")");
		return s.toString();
	}
}
