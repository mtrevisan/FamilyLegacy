package io.github.mtrevisan.familylegacy.flef.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


class GenericTable{

	static final GenericKey NO_KEY = new GenericKey(null);


	private final String name;
	private final List<GenericColumn> columns;
	private final Set<String> primaryKeys;
	private final Set<String[]> uniques;
	private final Set<ForeignKey> foreignKeys;

	private final Map<String, Integer> columnIndex;
	private final Map<GenericKey, GenericRecord> records;


	GenericTable(final String name){
		this.name = name;
		columns = new ArrayList<>(0);
		primaryKeys = new HashSet<>(0);
		uniques = new HashSet<>(0);
		foreignKeys = new HashSet<>(0);

		columnIndex = new HashMap<>(0);
		records = new HashMap<>(0);
	}


	String getName(){
		return name;
	}

	List<GenericColumn> getColumns(){
		return columns;
	}

	GenericColumn findColumn(final String name){
		for(int i = 0, length = columns.size(); i < length; i ++){
			final GenericColumn column = columns.get(i);
			if(column.getName().equalsIgnoreCase(name))
				return column;
		}
		return null;
	}

	void addColumn(final GenericColumn column){
		columnIndex.put(column.getName(), columnIndex.size());

		columns.add(column);
	}

	Map<GenericKey, GenericRecord> getRecords(){
		return records;
	}

	void addRecord(final GenericRecord record){
		final GenericKey primaryKeyValue = extractKey(record);

		records.put(primaryKeyValue, record);
	}

	private GenericKey extractKey(final GenericRecord record){
		final Object[] primaryKeyValue = new Object[primaryKeys.size()];
		int index = 0;
		for(final String primaryKey : primaryKeys)
			primaryKeyValue[index ++] = getValueForColumn(record, primaryKey);
		return new GenericKey(primaryKeyValue);
	}

	Set<String> getPrimaryKeys(){
		return primaryKeys;
	}

	void addPrimaryKeyColumn(final String column){
		primaryKeys.add(column);
	}

	Set<String[]> getUniques(){
		return uniques;
	}

	void addUniques(final String[] columns){
		uniques.add(columns);
	}

	Set<ForeignKey> getForeignKeys(){
		return foreignKeys;
	}

	void addForeignKey(final ForeignKey foreignKey){
		foreignKeys.add(foreignKey);
	}


	boolean hasRecord(final GenericKey key){
		return records.containsKey(key);
	}

	boolean hasRecord(final String[] reference, final GenericKey key){
		//if reference is the primary key of this table
		if(primaryKeys.containsAll(List.of(reference)))
			return hasRecord(key);

		return (findRecord(reference, key) != null);
	}

	GenericRecord findRecord(final GenericKey key){
		return records.get(key);
	}

	//iterate over records searching for the one that has in the columns defined by `reference` the values inside `key`
	GenericRecord findRecord(final String[] reference, final GenericKey key){
		for(final GenericRecord record : records.values()){
			boolean found = true;
			for(int i = 0; i < reference.length; i ++){
				final String columnName = reference[i];
				final Object value = getValueForColumn(record, columnName);
				if(!Objects.equals(value, key.key()[i])){
					found = false;
					break;
				}
			}
			if(found)
				return record;
		}
		return null;
	}

	Object getValueForColumn(final GenericRecord record, final String columnName){
		final Integer index = columnIndex.get(columnName);
		return (index != null? record.getFields()[index]: NO_KEY);
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
