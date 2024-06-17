package io.github.mtrevisan.familylegacy.flef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GenericTable{

	private final String name;
	private final List<GenericColumn> columns;
	private final List<String> primaryKeys;
	private final List<String[]> uniques;
	private final List<ForeignKey> foreignKeys;

	private final List<GenericRecord> records;


	public GenericTable(final String name){
		this.name = name;
		this.columns = new ArrayList<>(0);
		this.primaryKeys = new ArrayList<>(0);
		this.uniques = new ArrayList<>(0);
		this.foreignKeys = new ArrayList<>(0);

		this.records = new ArrayList<>(0);
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

	public List<String> getPrimaryKeys(){
		return primaryKeys;
	}

	public void addPrimaryKeyColumn(final String column){
		primaryKeys.add(column);
	}

	public List<String[]> getUniques(){
		return uniques;
	}

	public void addUniques(final String[] columns){
		uniques.add(columns);
	}

	public List<ForeignKey> getForeignKeys(){
		return foreignKeys;
	}

	public void addForeignKey(final ForeignKey foreignKey){
		foreignKeys.add(foreignKey);
	}

	@Override
	public String toString(){
		return "Table{"
			+ "name='" + name + '\''
			+ (!columns.isEmpty()? ", columns=" + columns: "")
			+ (!primaryKeys.isEmpty()? ", primaryKeys=" + primaryKeys: "")
			+ (!uniques.isEmpty()? ", uniques=" + Arrays.toString(uniques.stream().map(Arrays::toString).toArray()): "")
			+ (!foreignKeys.isEmpty()? ", foreignKeys=" + foreignKeys: "")
			+ (!records.isEmpty()? ", records=" + records: "")
			+ '}';
	}

}
