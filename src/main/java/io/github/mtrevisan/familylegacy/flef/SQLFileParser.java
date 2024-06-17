package io.github.mtrevisan.familylegacy.flef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SQLFileParser{

	private static final String SQL_COMMENT = "--";
	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("CREATE\\s+TABLE\\s+(?:(IF\\s+NOT\\s+EXISTS)?\\s+)?\"?([^\\s\"]+)\"?");
	private static final Pattern COLUMN_DEFINITION_PATTERN = Pattern.compile("\"?([^\\s\"]+)\"?\\s+([^\\s]+(?:\\s*\\(([^)]+)\\))?)(\\s+(?:NOT\\s+)?NULL)?(\\s+UNIQUE)?(\\s+PRIMARY\\s+KEY(?:\\s+(ASC|DESC))?)?");
	private static final Pattern PRIMARY_KEY_CONSTRAINT_PATTERN = Pattern.compile("CONSTRAINT\\s+([^\\s]+)\\s+PRIMARY\\s+KEY\\s+\\(\\s+\"?([^\\s\"]+)\"?\\s+\\)(?:\\s+(ASC|DESC))?");
	private static final Pattern UNIQUE_CONSTRAINT_PATTERN = Pattern.compile("CONSTRAINT\\s+([^\\s]+)\\s+UNIQUE\\s+\\(\\s+\"?([^\\s\"]+)\"?\\s+\\)");
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("FOREIGN\\s+KEY\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)");
	private static final Pattern UNIQUE_PATTERN = Pattern.compile("UNIQUE\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)");
	private static final String SORT_DIRECTION_ASC = "ASC";
	private static final String SORT_DIRECTION_DESC = "DESC";
	private static final String SQL_NOT = "NOT";


	private final Map<String, GenericTable> tables = new HashMap<>();


	public void parse(final String filePath) throws IOException{
		try(final BufferedReader reader = new BufferedReader(new FileReader(filePath))){
			String line;
			GenericTable currentTable = null;
			while((line = reader.readLine()) != null){
				line = line.trim();

				if(line.isEmpty() || line.startsWith(SQL_COMMENT))
					continue;

				Matcher matcher = CREATE_TABLE_PATTERN.matcher(line);
				if(matcher.find()){
					if(currentTable != null)
						tables.put(currentTable.getName(), currentTable);

					final String tableName = matcher.group(2);
					currentTable = new GenericTable(tableName);
					continue;
				}

				matcher = PRIMARY_KEY_CONSTRAINT_PATTERN.matcher(line);
				if(matcher.find()){
					handlePrimaryKeyConstraint(matcher, currentTable);
					continue;
				}

				matcher = UNIQUE_CONSTRAINT_PATTERN.matcher(line);
				if(matcher.find()){
					handleUniqueConstraint(matcher, currentTable);
					continue;
				}

				matcher = FOREIGN_KEY_PATTERN.matcher(line);
				if(matcher.find()){
					handleForeignKey(matcher, currentTable);
					continue;
				}

				matcher = UNIQUE_PATTERN.matcher(line);
				if(matcher.find()){
					handleUnique(matcher, currentTable);
					continue;
				}

				matcher = COLUMN_DEFINITION_PATTERN.matcher(line);
				if(matcher.find())
					handleColumnDefinition(matcher, currentTable);
			}

			if(currentTable != null)
				tables.put(currentTable.getName(), currentTable);
		}

		validatePrimaryKeys();
		validateForeignKeys();
	}

	private static void handlePrimaryKeyConstraint(final Matcher matcher, final GenericTable currentTable){
		final String[] primaryKeyNames = matcher.group(2).split(",");
		final String primaryKeySortOrder = matcher.group(3);
		for(final String primaryKeyName : primaryKeyNames){
			final GenericColumn primaryKeyColumn = currentTable.findColumn(primaryKeyName);
			if(primaryKeyColumn == null)
				throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + primaryKeyName
					+ " for primary key");

			currentTable.addPrimaryKeyColumn(primaryKeyName);
			primaryKeyColumn.setNullable(false);
			primaryKeyColumn.setPrimaryKeyOrder(primaryKeySortOrder == null || primaryKeySortOrder.contains(SORT_DIRECTION_ASC)
				? SORT_DIRECTION_ASC: SORT_DIRECTION_DESC);
		}
	}

	private static void handleUniqueConstraint(final Matcher matcher, final GenericTable currentTable){
		final String[] uniqueNames = matcher.group(2).split(",");
		for(final String uniqueName : uniqueNames){
			final GenericColumn uniqueColumn = currentTable.findColumn(uniqueName);
			if(uniqueColumn == null)
				throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + uniqueName + " for unique");
		}

		currentTable.addUniques(uniqueNames);
	}

	private static void handleForeignKey(final Matcher matcher, final GenericTable currentTable){
		final String[] columnName = matcher.group(1).split(",");
		final String foreignTable = matcher.group(2);
		final String[] foreignColumn = matcher.group(3).split(",");
		if(columnName.length != foreignColumn.length)
			throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have the same amount of foreign columns as "
				+ foreignTable);
		for(int i = 0, length = columnName.length; i < length; i ++){
			final GenericColumn foreignKeyColumn = currentTable.findColumn(columnName[i]);
			if(foreignKeyColumn == null)
				throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + foreignKeyColumn
					+ " for primary key");

			foreignKeyColumn.setForeignKeyTable(foreignTable);
			foreignKeyColumn.setForeignKeyColumn(foreignColumn[i]);
		}
		currentTable.addForeignKey(new ForeignKey(columnName, foreignTable, foreignColumn));
	}

	private static void handleUnique(final Matcher matcher, final GenericTable currentTable){
		final String columnName = matcher.group(1);
		final GenericColumn column = currentTable.findColumn(columnName);
		if(column == null)
			throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + column + " for unique");

		currentTable.addUniques(new String[]{columnName});
	}

	private static void handleColumnDefinition(final Matcher matcher, final GenericTable currentTable){
		final String columnName = matcher.group(1);
		final String columnType = matcher.group(2);
		final String columnSize = matcher.group(3);
		final GenericColumn column = new GenericColumn(columnName, columnType,
			(columnSize != null? Integer.parseInt(columnSize): null));

		final String notNull = matcher.group(4);
		if(notNull != null && !notNull.contains(SQL_NOT))
			column.setNullable(true);
		final String unique = matcher.group(5);
		if(unique != null)
			currentTable.addUniques(new String[]{columnName});
		final String primaryKey = matcher.group(6);
		if(primaryKey != null){
			currentTable.addPrimaryKeyColumn(columnName);

			final String primaryKeySortOrder = matcher.group(7);
			column.setPrimaryKeyOrder(primaryKeySortOrder == null || primaryKeySortOrder.contains(SORT_DIRECTION_ASC)? SORT_DIRECTION_ASC: SORT_DIRECTION_DESC);
		}

		currentTable.addColumn(column);
	}

	private void validatePrimaryKeys(){
		for(final GenericTable table : tables.values()){
			final String tableName = table.getName();

			final Set<String> primaryKeys = table.getPrimaryKeys();
			if(primaryKeys.isEmpty())
				throw new IllegalArgumentException("Table " + tableName + " does not have primary keys");
			for(final String primaryKeyName : primaryKeys)
				if(table.findColumn(primaryKeyName) == null)
					throw new IllegalArgumentException("Table " + tableName + " does not have column " + primaryKeyName);
		}
	}

	private void validateForeignKeys(){
		for(final GenericTable table : tables.values()){
			final String tableName = table.getName();

			final Set<ForeignKey> foreignKeys = table.getForeignKeys();
			if(!foreignKeys.isEmpty())
				for(final ForeignKey entry : foreignKeys){
					final String[] tableColumnName = entry.columnName();
					final String referencedTableName = entry.foreignTable();
					final String[] referencedColumnName = entry.foreignColumn();

					for(int i = 0, length = tableColumnName.length; i < length; i ++){
						final String column = tableColumnName[i];

						if(table.findColumn(column) == null)
							throw new IllegalArgumentException("Table " + tableName + " does not have column " + column);
					}

					final GenericTable referencedTable = tables.get(referencedTableName);
					if(referencedTable == null)
						throw new IllegalArgumentException("Table " + tableName + ", referenced table " + referencedTableName
							+ " does not exists");

					for(int i = 0, length = referencedColumnName.length; i < length; i ++){
						final String column = referencedColumnName[i];

						final GenericColumn referencedColumn = referencedTable.findColumn(column);
						if(referencedColumn == null)
							throw new IllegalArgumentException("Table " + tableName + ", referenced table " + referencedTable.getName()
								+ " does not contains column " + column);
						if(!referencedColumn.isPrimaryKey())
							throw new IllegalArgumentException("Table " + tableName + ", referenced column " + referencedTable.getName()
								+ "." + column + " is not a primary key");
						final GenericColumn tableColumn = table.findColumn(tableColumnName[i]);
						if(!Objects.equals(tableColumn.getType(), referencedColumn.getType())
								|| !Objects.equals(tableColumn.getSize(), referencedColumn.getSize()))
							throw new IllegalArgumentException("Column " + tableName + "." + tableColumnName[i] + " and referenced column "
								+ referencedTable.getName() + "." + column + " does not have the same type or size");
					}
				}
		}
	}


	public void populate(final String filePath) throws IOException{
		final DataPopulator populator = new DataPopulator();
		populator.populate(tables, filePath);
	}


	public static void main(final String[] args) throws IOException{
		final SQLFileParser parser = new SQLFileParser();
		parser.parse("src/main/resources/gedg/treebard/FLeF.sql");

		parser.tables
			.forEach((tableName, table) -> System.out.println(table));


		System.out.println();


		parser.populate("src/main/resources/gedg/treebard/FLeF.data");

		parser.tables.forEach((tableName, table) -> {
			final Map<GenericKey, GenericRecord> records = table.getRecords();
			if(!records.isEmpty()){
				System.out.println(tableName);
				records.forEach((key, datum) -> {
					final List<GenericColumn> columns = table.getColumns();
					final Map<String, Object> result = new LinkedHashMap<>(columns.size());
					final Object[] fields = datum.getFields();
					int i = 0;
					for(final GenericColumn column : columns){
						if(i == fields.length)
							break;

						if(fields[i] != null)
							result.put(column.getName(), fields[i]);

						i ++;
					}
					System.out.println(result);
				});
				System.out.println();
			}
		});
	}

}
