/**
 * Copyright (c) 2024 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.flef.sql;

import io.github.mtrevisan.familylegacy.flef.helpers.TimeWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class SQLFileParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(SQLFileParser.class);

	private static final String COMMENT_LINE_START = "--";
	private static final String COMMENT_BLOCK_START = "/*";
	private static final String COMMENT_BLOCK_END = "*/";
	private static final String SORT_DIRECTION_ASC = "ASC";
	private static final String SORT_DIRECTION_DESC = "DESC";
	private static final String NOT = "NOT";
	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(?:(IF\\s+NOT\\s+EXISTS)?\\s+)?\"?([^\\s\"(]+)\"?");
	private static final Pattern COLUMN_DEFINITION_PATTERN = Pattern.compile("(?i)\"?([^\\s\"]+)\"?\\s+([^\\s,]+(?:\\s*\\(([^)]+)\\))?)(\\s+(?:NOT\\s+)?NULL)?(\\s+UNIQUE)?(\\s+PRIMARY\\s+KEY(?:\\s+(ASC|DESC))?)?(\\s+FOREIGN\\s+KEY\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\))?");
	private static final Pattern PRIMARY_KEY_CONSTRAINT_PATTERN = Pattern.compile("(?i)CONSTRAINT\\s+([^\\s]+)\\s+PRIMARY\\s+KEY\\s+\\(\\s+\"?([^\\s\"]+)\"?\\s+\\)(?:\\s+(ASC|DESC))?");
	private static final Pattern UNIQUE_CONSTRAINT_PATTERN = Pattern.compile("(?i)CONSTRAINT\\s+([^\\s]+)\\s+UNIQUE\\s+\\(\\s+\"?([^\\s\"]+)\"?\\s+\\)");
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("(?i)FOREIGN\\s+KEY\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)");
	private static final Pattern UNIQUE_PATTERN = Pattern.compile("(?i)UNIQUE\\s+\\(\\s*\"?([^\\s\",]+)\"?\\s*\\)");


	private final Map<String, GenericTable> tables = new HashMap<>();


	public void load(final String grammarFile, final String dataFile) throws SQLGrammarException, SQLDataException{
		parse(grammarFile);

		populate(dataFile);
	}

	public void parse(final String grammarFile) throws SQLGrammarException{
		final TimeWatch watch = TimeWatch.start();

		LOGGER.info("Parsing FLeF format...");

		try(final BufferedReader reader = new BufferedReader(new FileReader(grammarFile, StandardCharsets.UTF_8))){
			String line;
			GenericTable currentTable = null;
			boolean inBlockComment = false;
			while((line = reader.readLine()) != null){
				line = line.trim();

				//manage comments:
				if(inBlockComment){
					final int blockCommentEnd = line.indexOf(COMMENT_BLOCK_END);
					if(blockCommentEnd < 0)
						continue;

					line = line.substring(blockCommentEnd + COMMENT_BLOCK_END.length());
					inBlockComment = false;
				}
				else{
					final int blockCommentStart = line.indexOf(COMMENT_BLOCK_START);
					if(blockCommentStart >= 0){
						final String preLine = line.substring(0, blockCommentStart);
						inBlockComment = true;

						final int blockCommentEnd = line.indexOf(COMMENT_BLOCK_END, blockCommentStart);
						if(blockCommentEnd >= 0){
							final String postLine = line.substring(blockCommentEnd + COMMENT_BLOCK_END.length());
							inBlockComment = false;
							line = preLine + postLine;
						}
						else
							line = preLine;
					}
					final int lineCommentStart = line.indexOf(COMMENT_LINE_START);
					if(lineCommentStart >= 0)
						line = line.substring(0, lineCommentStart);
				}
				if(line.isEmpty())
					continue;


				//manage sql line:
				Matcher matcher = CREATE_TABLE_PATTERN.matcher(line);
				if(matcher.find()){
					if(currentTable != null)
						tables.put(currentTable.getName(), currentTable);

					final String tableName = matcher.group(2)
						.toLowerCase(Locale.ROOT);
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


			//validate:
			validatePrimaryKeys();
			validateForeignKeys();
		}
		catch(final IOException ioe){
			throw new SQLGrammarException(ioe);
		}
		finally{
			watch.stop();

			LOGGER.info("Parsed FLeF format in {}", watch.toStringMillis());
		}
	}

	private static void handlePrimaryKeyConstraint(final Matcher matcher, final GenericTable currentTable){
		final String[] primaryKeyNames = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.split(",");
		final String primaryKeySortOrder = matcher.group(3);
		for(final String primaryKeyName : primaryKeyNames){
			final GenericColumn primaryKeyColumn = currentTable.findColumn(primaryKeyName);
			if(primaryKeyColumn == null)
				throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + primaryKeyName
					+ " for primary key");

			currentTable.addPrimaryKeyColumn(primaryKeyName);
			primaryKeyColumn.setNotNullable();
			primaryKeyColumn.setPrimaryKeyOrder(primaryKeySortOrder == null
				|| primaryKeySortOrder.toUpperCase(Locale.ROOT).contains(SORT_DIRECTION_ASC)
				? SORT_DIRECTION_ASC: SORT_DIRECTION_DESC);
		}
	}

	private static void handleUniqueConstraint(final Matcher matcher, final GenericTable currentTable){
		final String[] uniqueNames = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.split(",");
		for(int i = 0, length = uniqueNames.length; i < length; i ++){
			final String uniqueName = uniqueNames[i];

			final GenericColumn uniqueColumn = currentTable.findColumn(uniqueName);
			if(uniqueColumn == null)
				throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + uniqueName + " for unique");
		}

		currentTable.addUniques(uniqueNames);
	}

	private static void handleForeignKey(final Matcher matcher, final GenericTable currentTable){
		final String[] columnName = matcher.group(1)
			.toLowerCase(Locale.ROOT)
			.split(",");
		final String foreignTable = matcher.group(2)
			.toLowerCase(Locale.ROOT);
		final String[] foreignColumn = matcher.group(3)
			.toLowerCase(Locale.ROOT)
			.split(",");
		if(columnName.length != foreignColumn.length)
			throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have the same amount of foreign columns as "
				+ foreignTable);
		for(int i = 0, length = columnName.length; i < length; i ++){
			final GenericColumn foreignKeyColumn = currentTable.findColumn(columnName[i]);
			if(foreignKeyColumn == null)
				throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + columnName[i]
					+ " for primary key");

			foreignKeyColumn.setForeignKey(foreignTable, foreignColumn[i]);
		}
		currentTable.addForeignKey(new ForeignKey(columnName, foreignTable, foreignColumn));
	}

	private static void handleUnique(final Matcher matcher, final GenericTable currentTable){
		final String columnName = matcher.group(1)
			.toLowerCase(Locale.ROOT);
		final GenericColumn column = currentTable.findColumn(columnName);
		if(column == null)
			throw new IllegalArgumentException("Table " + currentTable.getName() + " does not have column " + columnName + " for unique");

		currentTable.addUniques(new String[]{columnName});
	}

	private static void handleColumnDefinition(final Matcher matcher, final GenericTable currentTable){
		final String columnName = matcher.group(1)
			.toLowerCase(Locale.ROOT);
		final String columnType = matcher.group(2)
			.toLowerCase(Locale.ROOT);
		final String columnSize = matcher.group(3);
		final GenericColumn column = new GenericColumn(columnName, columnType,
			(columnSize != null? Integer.parseInt(columnSize): null));

		final String notNull = matcher.group(4);
		if(notNull != null && !notNull.toUpperCase(Locale.ROOT).contains(NOT))
			column.setNotNullable();
		final String unique = matcher.group(5);
		if(unique != null)
			currentTable.addUniques(new String[]{columnName});
		final String primaryKey = matcher.group(6);
		if(primaryKey != null){
			currentTable.addPrimaryKeyColumn(columnName);

			final String primaryKeySortOrder = matcher.group(7);
			column.setPrimaryKeyOrder(primaryKeySortOrder == null
				|| primaryKeySortOrder.toUpperCase(Locale.ROOT).contains(SORT_DIRECTION_ASC)? SORT_DIRECTION_ASC: SORT_DIRECTION_DESC);
		}
		final String foreignKey = matcher.group(8);
		if(foreignKey != null){
			final String foreignTable = matcher.group(9)
				.toLowerCase(Locale.ROOT);
			final String foreignColumn = matcher.group(10)
				.toLowerCase(Locale.ROOT);

			column.setForeignKey(foreignTable, foreignColumn);
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


	public void populate(final String dataFile) throws SQLDataException{
		try{
			final DataPopulator populator = new DataPopulator();
			populator.populate(tables, dataFile);
		}
		catch(final IOException ioe){
			throw new SQLDataException(ioe);
		}
	}



	public static void main(final String[] args) throws SQLGrammarException, SQLDataException{
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
					for(int j = 0, length = columns.size(); j < length; j ++){
						if(i == fields.length)
							break;

						if(fields[i] != null)
							result.put(columns.get(j).getName(), fields[i]);

						i++;
					}
					System.out.println(result);
				});
				System.out.println();
			}
		});
	}

}
