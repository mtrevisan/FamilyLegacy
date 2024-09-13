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
package io.github.mtrevisan.familylegacy.flef.persistence.db;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// https://neo4j.com/docs/getting-started/get-started-with-neo4j/graph-database/
// https://www.baeldung.com/java-neo4j
public class StoreManager implements StoreManagerInterface{

	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("CREATE\\s+TABLE\\s+\"?([^\\s\\r\\n(\"]+)\"?[^;]*?;", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROW_DEFINITION_PATTERN = Pattern.compile("([^\\s\\r\\n,)]+)\\s+[^\\s]+?\\s+(?:NOT\\s+NULL\\s+)?DEFAULT\\s+([^\\s\\r\\n,)]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern PRIMARY_KEY_PATTERN = Pattern.compile("\"?([^\\s\\r\\n(\"]+)\"?\\s+[^\\s+]+\\s+PRIMARY\\s+KEY", Pattern.CASE_INSENSITIVE);
	//https://stackoverflow.com/questions/6720050/foreign-key-constraints-when-to-use-on-update-and-on-delete
	private static final String FOREIGN_KEY_TRUE_PATTERN = "FOREIGN\\s+KEY(\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\))?\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)(\\s+ON\\s+(DELETE\\s+(CASCADE|SET\\s+(NULL|DEFAULT)|NO\\s+ACTION|RESTRICT)))?";
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("(" + FOREIGN_KEY_TRUE_PATTERN + "),?", Pattern.CASE_INSENSITIVE);
	private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile("ALTER\\s+TABLE\\s+(\"?[^\\s\\r\\n(]+\"?)\\s+ADD\\s+CONSTRAINT\\s+(?:[^\\s]+)?\\s+" + FOREIGN_KEY_TRUE_PATTERN + ";", Pattern.CASE_INSENSITIVE);

	private static final String NO_ACTION = "NO ACTION";

	private static final Pattern PATTERN = Pattern.compile("(,[\\s\\r\\n]+){1,}\\)");

	private static final Map<String, Class<?>> SQL_TO_JAVA_TYPE = new HashMap<>(17);
	static{
		//https://www.dofactory.com/sql/bit
		SQL_TO_JAVA_TYPE.put("BOOLEAN", Boolean.class);
		SQL_TO_JAVA_TYPE.put("BIT", Boolean.class);
		SQL_TO_JAVA_TYPE.put("TINYINT", Byte.class);
		SQL_TO_JAVA_TYPE.put("SMALLINT", Short.class);
		SQL_TO_JAVA_TYPE.put("INT", Integer.class);
		SQL_TO_JAVA_TYPE.put("INTEGER", Integer.class);
		SQL_TO_JAVA_TYPE.put("BIGINT", Long.class);
		SQL_TO_JAVA_TYPE.put("CHAR", String.class);
		SQL_TO_JAVA_TYPE.put("VARCHAR", String.class);
		SQL_TO_JAVA_TYPE.put("CHARACTER VARYING", String.class);
		SQL_TO_JAVA_TYPE.put("TEXT", String.class);
		SQL_TO_JAVA_TYPE.put("FLOAT", Float.class);
		SQL_TO_JAVA_TYPE.put("DOUBLE", Double.class);
		SQL_TO_JAVA_TYPE.put("REAL", BigDecimal.class);
		SQL_TO_JAVA_TYPE.put("DECIMAL", BigDecimal.class);
		SQL_TO_JAVA_TYPE.put("NUMERIC", BigDecimal.class);
	}


	private record ForeignKeyRule(String foreignTable, String foreignKeyColumn, String referencedTable, String referencedKeyColumn,
		String onDelete){}
	private record DeletionTask(String tableName, String primaryKeyColumn, Integer idValue){}


	private List<String> tableCreations;
	private final Map<String, ForeignKeyRule> foreignKeyRules = new HashMap<>(0);

	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;


	public static StoreManager create(final String sqlFile) throws IOException{
		return new StoreManager(sqlFile, new TreeMap<>());
	}

	public static StoreManager create(final String sqlFile, final Map<String, TreeMap<Integer, Map<String, Object>>> store)
			throws IOException{
		return new StoreManager(sqlFile, store);
	}


	private StoreManager(final String sqlFile, final Map<String, TreeMap<Integer, Map<String, Object>>> store) throws IOException{
		initialize(sqlFile);

		this.store = store;
	}


	private void initialize(final String sqlFile) throws IOException{
		final String sql = Files.readString(Paths.get(sqlFile));

		//separate table creation and foreign key constraints
		tableCreations = new ArrayList<>(0);

		final Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(sql);
		while(tableMatcher.find()){
			String createTableStatement = tableMatcher.group();
			final String tableName = tableMatcher.group(1);

			final String plainTableNameTo = StringUtils.replaceChars(tableName, "\"", "");

			//remove inline foreign keys and add them to the foreignKeyConstraints list
			final Matcher foreignKeyMatcher = FOREIGN_KEY_PATTERN.matcher(createTableStatement);
			while(foreignKeyMatcher.find()){
				final String foreignKeyColumn = foreignKeyMatcher.group(3);
				final String referencedTable = foreignKeyMatcher.group(4);
				final String referencedColumn = foreignKeyMatcher.group(5);
				final String onDeleteAction = (foreignKeyMatcher.group(8) != null? foreignKeyMatcher.group(8): NO_ACTION);

				//remove inline foreign keys
				createTableStatement = createTableStatement.replaceAll(Pattern.quote(foreignKeyMatcher.group()), "");

				//store foreign key rule
				final String key = plainTableNameTo + "." + foreignKeyColumn;
				foreignKeyRules.put(key, new ForeignKeyRule(plainTableNameTo, foreignKeyColumn, referencedTable, referencedColumn,
					onDeleteAction));
			}

			createTableStatement = cleanCreateTableStatement(createTableStatement);
			tableCreations.add(createTableStatement);
		}

		final Matcher foreignKeyMatcher = ALTER_TABLE_PATTERN.matcher(sql);
		while(foreignKeyMatcher.find()){
			//store foreign key rule
			final String tableName = foreignKeyMatcher.group(1);
			final String foreignKeyColumn = foreignKeyMatcher.group(3);
			final String referencedTable = foreignKeyMatcher.group(4);
			final String referencedColumn = foreignKeyMatcher.group(5);
			final String onDeleteAction = (foreignKeyMatcher.group(8) != null? foreignKeyMatcher.group(8): NO_ACTION);
			final String key = tableName + "." + foreignKeyColumn;
			foreignKeyRules.put(key, new ForeignKeyRule(tableName, foreignKeyColumn, referencedTable, referencedColumn, onDeleteAction));
		}

		System.out.println("Database initialized successfully.");
	}

	private static String cleanCreateTableStatement(final String createTableStatement){
		final String commentFreeLine = createTableStatement.replaceAll("--[^\\r\\n]+[\\r\\n]+", "")
			.replaceAll("/\\*.*?\\*/", "")
			.replaceAll("\\s+", " ")
			.trim();
		return PATTERN.matcher(commentFreeLine)
			.replaceAll(")");
	}


	@Override
	public void delete(final String tableName, final Integer recordID) throws StoreException{
		final Deque<DeletionTask> queue = new ArrayDeque<>();
		queue.add(new DeletionTask(tableName, getPrimaryKeyColumn(tableName), recordID));
		while(!queue.isEmpty()){
			final DeletionTask task = queue.poll();

			//propagate deletion to connected tables
			final String currentTable = task.tableName
				.toLowerCase(Locale.ROOT);
			final List<ForeignKeyRule> connectedTables = extractForeignRecordsUponDelete(currentTable);
			for(int i = 0, tableCount = connectedTables.size(); i < tableCount; i ++){
				final ForeignKeyRule rule = connectedTables.get(i);

				final String foreignTable = rule.foreignTable
					.toLowerCase(Locale.ROOT);
				final String foreignKeyColumn = rule.foreignKeyColumn
					.toLowerCase(Locale.ROOT);
				final String dependentTable = rule.referencedTable
					.toLowerCase(Locale.ROOT);
				switch(rule.onDelete){
					case "CASCADE":
						//delete any rows referencing the deleted row, or update the values of the referencing column(s) to the new values of the
						// referenced columns, respectively.
						final List<Integer> affectedIDs = deleteCascade(dependentTable, getPrimaryKeyColumn(dependentTable), task.idValue);
						for(int j = 0, length = affectedIDs.size(); j < length; j ++)
							queue.add(new DeletionTask(foreignTable, getPrimaryKeyColumn(foreignTable), affectedIDs.get(j)));
						break;

					case "SET NULL":
						//set all of the referencing columns, or a specified subset of the referencing columns, to `null`. A subset of columns
						// can only be specified for ON DELETE actions.
						setForeignKeyToNull(foreignTable, foreignKeyColumn, task.idValue);
						break;

					case "SET DEFAULT":
						//set all of the referencing columns, or a specified subset of the referencing columns, to their default values. A subset
						// of columns can only be specified for ON DELETE actions. (There must be a row in the referenced table matching the
						// default values, if they are not null, or the operation will fail.)
						final String defaultValue = extractDefaultValue(foreignTable, foreignKeyColumn);
						setForeignKeyToDefault(foreignTable, foreignKeyColumn, defaultValue, task.idValue);
						break;

					case NO_ACTION:
						//produce an error indicating that the deletion or update would create a foreign key constraint violation. If the
						// constraint is deferred, this error will be produced at constraint check time if there still exist any referencing rows.
						// This is the default action.
					case "RESTRICT":
						//produce an error indicating that the deletion or update would create a foreign key constraint violation. This is the
						// same as NO ACTION except that the check is not deferrable.
						throw StoreException.create("Cannot remove record, there's a reference to it from table {}, foreign key column {} ",
							dependentTable, foreignKeyColumn);

					default:
						throw new UnsupportedOperationException("Unsupported ON DELETE action: " + rule.onDelete);
				}
			}
		}
	}

	private List<Integer> deleteCascade(final String tableName, final String foreignKeyColumn, final Integer recordID){
		final List<Integer> deletedIDs = new ArrayList<>(0);
		final Iterator<Map<String, Object>> itr = store.get(tableName)
			.values()
			.iterator();
		while(itr.hasNext()){
			final Map<String, Object> record = itr.next();

			if(record.get(foreignKeyColumn).equals(recordID)){
				final String primaryKeyColumn = getPrimaryKeyColumn(tableName);
				deletedIDs.add((Integer)record.get(primaryKeyColumn));

				itr.remove();
			}
		}
		return deletedIDs;
	}

	private String getPrimaryKeyColumn(final String tableName){
		String primaryKeyColumn = null;
		for(int j = 0, length = tableCreations.size(); j < length; j ++){
			final String tableCreation = tableCreations.get(j);
			final Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(tableCreation);
			if(!tableMatcher.find())
				continue;

			final String tableCreationName = tableMatcher.group(1);
			if(!tableCreationName.equalsIgnoreCase(tableName))
				continue;

			final Matcher columnMatcher = PRIMARY_KEY_PATTERN.matcher(tableCreation);
			columnMatcher.find();
			primaryKeyColumn = columnMatcher.group(1)
				.toLowerCase(Locale.ROOT);
			break;
		}
		return primaryKeyColumn;
	}

	private void setForeignKeyToNull(final String tableName, final String columnName, final Integer recordID){
		setForeignKeyToDefault(tableName, columnName, null, recordID);
	}

	private void setForeignKeyToDefault(final String tableName, final String columnName, final String defaultValue, final int recordID){
		final TreeMap<Integer, Map<String, Object>> records = store.get(tableName);
		if(records != null)
			for(final Map<String, Object> record : records.values())
				if(record.get(columnName).equals(recordID)){
					final Object currentValue = record.get(columnName);
					final Class<?> type = (currentValue != null? currentValue.getClass(): getColumnType(tableName, columnName));
					if(type == null)
						throw new IllegalArgumentException("Column type of " + columnName + " in table " + tableName + " not handled");
					final Object value = convertToType(type, defaultValue);

					record.put(columnName, value);
				}
	}

	/**
	 * Converts the specified value to the desired type.
	 * @param type	The class representation of the desired type.
	 * @param value	The string value to be converted.
	 * @return	Value converted to the desired type.
	 * @throws NumberFormatException	If value cannot be converted to the desired type.
	 */
	private static Object convertToType(final Class<?> type, final String value) throws NumberFormatException{
		if(type == Boolean.class)
			return Boolean.parseBoolean(value);
		if(type == Byte.class)
			return Byte.parseByte(value);
		if(type == Short.class)
			return Short.parseShort(value);
		if(type == Integer.class)
			return Integer.parseInt(value);
		if(type == Long.class)
			return Long.parseLong(value);
		if(type == Float.class)
			return Float.parseFloat(value);
		if(type == Double.class)
			return Double.parseDouble(value);
		if(type == BigDecimal.class)
			return new BigDecimal(value);
		return value;
	}

	private Class<?> getColumnType(final String tableName, final String columnName){
		Class<?> columnClass = null;
		for(int j = 0, length = tableCreations.size(); j < length; j ++){
			final String tableCreation = tableCreations.get(j);
			final Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(tableCreation);
			if(!tableMatcher.find())
				continue;

			final String tableCreationName = tableMatcher.group(1);
			if(!tableCreationName.equalsIgnoreCase(tableName))
				continue;

			final String regex = "\\b" + columnName + "\\b\\s+([a-zA-Z]+)(?:\\(\\d+\\))?";
			final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			final Matcher columnMatcher = pattern.matcher(tableCreation);
			columnMatcher.find();
			final String columnType = columnMatcher.group(1);
			columnClass = SQL_TO_JAVA_TYPE.get(columnType.toUpperCase(Locale.ROOT));
			break;
		}
		return columnClass;
	}

	private String extractDefaultValue(final String tableName, String columnName){
		String defaultValue = null;
		for(int j = 0, length = tableCreations.size(); defaultValue == null && j < length; j ++){
			final String tableCreation = tableCreations.get(j);
			final Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(tableCreation);
			if(!tableMatcher.find())
				continue;
			final String tableCreationName = tableMatcher.group(1);
			if(!tableCreationName.equalsIgnoreCase(tableName))
				continue;

			final Matcher columnMatcher = ROW_DEFINITION_PATTERN.matcher(tableCreation);
			while(defaultValue == null && columnMatcher.find())
				if(columnMatcher.group(1).equalsIgnoreCase(columnName))
					defaultValue = columnMatcher.group(2);
		}
		return defaultValue;
	}

	private List<ForeignKeyRule> extractForeignRecordsUponDelete(final String tableName){
		final List<ForeignKeyRule> result = new ArrayList<>(0);
		for(final Map.Entry<String, ForeignKeyRule> entry : foreignKeyRules.entrySet())
			if(entry.getValue().referencedTable.equalsIgnoreCase(tableName))
				result.add(entry.getValue());
		return result;
	}

}