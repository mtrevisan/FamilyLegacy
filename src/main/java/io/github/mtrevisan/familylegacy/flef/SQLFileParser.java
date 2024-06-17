package io.github.mtrevisan.familylegacy.flef;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SQLFileParser{

	private static final String SQL_COMMENT = "--";
	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("CREATE\\s+TABLE\\s+(?:(IF\\s+NOT\\s+EXISTS)?\\s+)?\"?([^\\s\"]+)\"?");
	private static final Pattern COLUMN_DEFINITION_PATTERN = Pattern.compile("\"?([^\\s\"]+)\"?\\s+([^\\s]+(?:\\s*\\(([^)]+)\\))?)(\\s+(?:NOT\\s+)?NULL)?(\\s+PRIMARY\\s+KEY(?:\\s+(ASC|DESC))?)?");
	private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("CONSTRAINT\\s+([^\\s]+)\\s+PRIMARY\\s+KEY\\s+\\(\\s+\"?([^\\s\"]+)\"?\\s+\\)(?:\\s+(ASC|DESC))?");
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("FOREIGN\\s+KEY\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)");
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

				matcher = CONSTRAINT_PATTERN.matcher(line);
				if(matcher.find()){
					handleConstraint(matcher, currentTable);
					continue;
				}

				matcher = FOREIGN_KEY_PATTERN.matcher(line);
				if(matcher.find()){
					handleForeignKey(matcher, currentTable);
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

	private static void handleConstraint(final Matcher matcher, final GenericTable currentTable){
		final String primaryKeyName = matcher.group(2);
		final String primaryKeySortOrder = matcher.group(3);
		final GenericColumn primaryKeyColumn = currentTable.findColumn(primaryKeyName);
		if(primaryKeyColumn == null)
			throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + primaryKeyName);

		currentTable.addPrimaryKeyColumn(primaryKeyName);
		primaryKeyColumn.setPrimaryKeyOrder(primaryKeySortOrder == null || primaryKeySortOrder.contains(SORT_DIRECTION_ASC)
			? SORT_DIRECTION_ASC: SORT_DIRECTION_DESC);
	}

	private static void handleForeignKey(final Matcher matcher, final GenericTable currentTable){
		final String columnName = matcher.group(1);
		final String foreignTable = matcher.group(2);
		final String foreignColumn = matcher.group(3);
		final GenericColumn foreignKeyColumn = currentTable.findColumn(columnName);
		foreignKeyColumn.setForeignKeyTable(foreignTable);
		foreignKeyColumn.setForeignKeyColumn(foreignColumn);
		currentTable.addForeignKey(new ForeignKey(columnName, foreignTable, foreignColumn));
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
		final String primaryKey = matcher.group(5);
		if(primaryKey != null){
			currentTable.addPrimaryKeyColumn(columnName);

			final String primaryKeySortOrder = matcher.group(6);
			column.setPrimaryKeyOrder(primaryKeySortOrder == null || primaryKeySortOrder.contains(SORT_DIRECTION_ASC)? SORT_DIRECTION_ASC: SORT_DIRECTION_DESC);
		}

		currentTable.addColumn(column);
	}

	private void validatePrimaryKeys(){
		for(final GenericTable table : tables.values()){
			final String tableName = table.getName();

			final List<String> primaryKeys = table.getPrimaryKeys();
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

			final List<ForeignKey> foreignKeys = table.getForeignKeys();
			if(!foreignKeys.isEmpty()){
				for(final ForeignKey entry : foreignKeys){
					final String referencedTableName = entry.foreignTable();
					final GenericTable referencedTable = tables.get(referencedTableName);
					final String referencedColumnName = entry.foreignColumn();

					final String tableColumnName = entry.columnName();
					final GenericColumn tableColumn = table.findColumn(tableColumnName);
					if(tableColumn == null)
						throw new IllegalArgumentException("Table " + tableName + " does not have column " + tableColumnName);

					if(referencedTable == null)
						throw new IllegalArgumentException("Table " + tableName + ", referenced table " + referencedTableName
							+ " does not exists");

					final GenericColumn referencedColumn = referencedTable.findColumn(referencedColumnName);
					if(referencedColumn == null)
						throw new IllegalArgumentException("Table " + tableName + ", referenced table " + referencedTable.getName()
							+ " does not contains column " + referencedColumnName);
					if(!referencedColumn.isPrimaryKey())
						throw new IllegalArgumentException("Table " + tableName + ", referenced column " + referencedTable.getName()
							+ "." + referencedColumnName + " is not a primary key");
					if(!Objects.equals(tableColumn.getType(), referencedColumn.getType())
							|| !Objects.equals(tableColumn.getSize(), referencedColumn.getSize()))
						throw new IllegalArgumentException("Column " + tableName + "." + tableColumnName + " and referenced column "
							+ referencedTable.getName() + "." + referencedColumnName + " does not have the same type or size");
				}
			}
		}
	}


	public Map<String, GenericTable> getTables(){
		return tables;
	}


	public static void main(final String[] args) throws IOException{
		final SQLFileParser parser = new SQLFileParser();
		parser.parse("src/main/resources/gedg/treebard/FLeF.sql");

		parser.getTables()
			.forEach((tableName, table) -> System.out.println(table));
	}

}
