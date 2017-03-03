package parsing;

import parsing.Table;

public class EquivalenceClassElement {
	private Column column;
	private Table table;
	
	public EquivalenceClassElement(Column col, Table table) {
		this.column = col;
		this.table = table;
	}
	
	public Table getTable(){
		return this.table;
	}
	
	public Column getColumn(){
		return this.column;
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append(table.getTableName()+"."+column.getColumnName());
		return s.toString();
	
	}
}
