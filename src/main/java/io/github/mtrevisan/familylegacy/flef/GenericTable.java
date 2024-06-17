package io.github.mtrevisan.familylegacy.flef;

import java.util.ArrayList;
import java.util.List;


public class GenericTable{

	private final String name;
	private final List<GenericColumn> columns;
	private final List<GenericRecord> records;
	private final List<String> primaryKey;
	private final List<String> foreignKeys;


	public GenericTable(final String name){
		this.name = name;
		this.columns = new ArrayList<>();
		this.records = new ArrayList<>();
		this.primaryKey = new ArrayList<>();
		this.foreignKeys = new ArrayList<>();
	}


	public String getName(){
		return name;
	}

	public List<GenericColumn> getColumns(){
		return columns;
	}

	public GenericColumn findColumn(final String name){
		for(int i = 0, length = columns.size(); i < length; i ++){
			final GenericColumn column = columns.get(i);
			if(column.getName().equals(name))
				return column;
		}
		return null;
	}

	public void addColumn(final GenericColumn column){
		columns.add(column);
	}

	public List<GenericRecord> getRecords(){
		return records;
	}

	public void addRecord(final GenericRecord record){
		records.add(record);
	}

	public List<String> getPrimaryKey() {
		return primaryKey;
	}

	public void addPrimaryKeyColumn(final String column) {
		primaryKey.add(column);
	}

	public List<String> getForeignKeys(){
		return foreignKeys;
	}

	public void addForeignKey(final String foreignKey){
		foreignKeys.add(foreignKey);
	}

	@Override
	public String toString(){
		return "Table{"
			+ "name='" + name + '\''
			+ (!columns.isEmpty()? ", columns=" + columns: "")
			+ (!records.isEmpty()? ", records=" + records: "")
			+ (!primaryKey.isEmpty()? ", primaryKey=" + primaryKey: "")
			+ (!foreignKeys.isEmpty()? ", foreignKeys=" + foreignKeys: "")
			+ '}';
	}

}
