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


public class StoreManager implements StoreManagerInterface{

	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(\"?[^\\s\\r\\n(]+\"?)[^;]*?;");
	private static final Pattern ROW_DEFINITION_PATTERN = Pattern.compile("(?i)\\s+([^\\s\\r\\n,)]+).+?\\s+DEFAULT\\s+([^\\s\\r\\n,)]+)");
	//https://stackoverflow.com/questions/6720050/foreign-key-constraints-when-to-use-on-update-and-on-delete
	private static final String FOREIGN_KEY_TRUE_PATTERN = "FOREIGN\\s+KEY(\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\))?\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)(\\s+ON\\s+(DELETE\\s+(CASCADE|SET\\s+(NULL|DEFAULT)|NO\\s+ACTION|RESTRICT)))?";
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("(?i)(" + FOREIGN_KEY_TRUE_PATTERN + "),?");
	private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile("(?i)ALTER\\s+TABLE\\s+(\"?[^\\s\\r\\n(]+\"?)\\s+ADD\\s+CONSTRAINT\\s+(?:[^\\s]+)?\\s+" + FOREIGN_KEY_TRUE_PATTERN + ";");

	private static final String NO_ACTION = "NO ACTION";
	private static final String VALUE_NULL = "NULL";

	private static final Pattern PATTERN = Pattern.compile("(,[\\s\\r\\n]+){1,}\\)");


	private record ForeignKeyRule(String foreignKeyColumn, String referencedTable, String referencedID, String onDelete){}
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


	private final void initialize(final String sqlFile) throws IOException{
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
				foreignKeyRules.put(key, new ForeignKeyRule(foreignKeyColumn, referencedTable, referencedColumn, onDeleteAction));
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
			foreignKeyRules.put(key, new ForeignKeyRule(foreignKeyColumn, referencedTable, referencedColumn, onDeleteAction));
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
		//TODO
		final String primaryKeyColumn = "id";
		queue.add(new DeletionTask(tableName, primaryKeyColumn, recordID));
		while(!queue.isEmpty()){
			final DeletionTask task = queue.poll();

			//propagate deletion to connected tables
			final String currentTable = task.tableName;
			final List<ForeignKeyRule> connectedTables = extractForeignRecordsUponDelete(currentTable);
			for(int i = 0, lengthI = connectedTables.size(); i < lengthI; i ++){
				final ForeignKeyRule rule = connectedTables.get(i);

				final String foreignKeyColumn = rule.foreignKeyColumn;
				final String dependentTable = rule.referencedTable;
				switch(rule.onDelete){
					case "CASCADE":
						//delete any rows referencing the deleted row, or update the values of the referencing column(s) to the new values of the
						// referenced columns, respectively.
						final List<Integer> affectedIDs = deleteCascade(dependentTable, foreignKeyColumn, task.idValue);
						for(int j = 0, lengthJ = affectedIDs.size(); j < lengthJ; j ++)
							queue.add(new DeletionTask(dependentTable, foreignKeyColumn, affectedIDs.get(j)));
						break;

					case "SET NULL":
						//set all of the referencing columns, or a specified subset of the referencing columns, to `null`. A subset of columns
						// can only be specified for ON DELETE actions.
						setForeignKeyToNull(dependentTable, foreignKeyColumn, task.idValue);
						break;

					case "SET DEFAULT":
						//set all of the referencing columns, or a specified subset of the referencing columns, to their default values. A subset
						// of columns can only be specified for ON DELETE actions. (There must be a row in the referenced table matching the
						// default values, if they are not null, or the operation will fail.)
						Object defaultValue = null;
						for(int j = 0, lengthJ = tableCreations.size(); defaultValue == null && j < lengthJ; j++){
							final String tableCreation = tableCreations.get(j);
							final Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(tableCreation);
							if(tableMatcher.find()){
								final String tableCreationName = tableMatcher.group(1);
								if(!tableCreationName.equalsIgnoreCase(dependentTable))
									continue;

								final Matcher columnMatcher = ROW_DEFINITION_PATTERN.matcher(tableCreation);
								while(defaultValue == null && columnMatcher.find()){
									final String columnName = columnMatcher.group(1);
									final String columnDefault = columnMatcher.group(2);
									if(columnName.equalsIgnoreCase(foreignKeyColumn))
										defaultValue = columnDefault;
								}
							}
						}
						if(defaultValue == null)
							defaultValue = VALUE_NULL;
						setForeignKeyToDefault(dependentTable, foreignKeyColumn, defaultValue, task.idValue);
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

			//delete the record in the current table
			deleteRecord(currentTable, task.primaryKeyColumn, task.idValue);
		}
	}

	private List<Integer> deleteCascade(final String tableName, final String foreignKeyColumn, final Integer recordID){
		final List<Integer> deletedIDs = new ArrayList<>(0);
		final Iterator<Map<String, Object>> itr = store.get(tableName).values().iterator();
		while(itr.hasNext()){
			final Map<String, Object> record = itr.next();

			if(record.get(foreignKeyColumn).equals(recordID)){
				//TODO
				final String primaryKeyColumn = "id";
				deletedIDs.add((Integer)record.get(primaryKeyColumn));

				itr.remove();
			}
		}
		return deletedIDs;
	}

	private void setForeignKeyToNull(final String tableName, final String foreignKeyColumn, final Integer recordID){
		setForeignKeyToDefault(tableName, foreignKeyColumn, VALUE_NULL, recordID);
	}

	private void setForeignKeyToDefault(final String tableName, final String foreignKeyColumn, final Object defaultValue,
			final int recordID){
		for(final Map<String, Object> record : store.get(tableName).values())
			if(record.get(foreignKeyColumn).equals(recordID))
				record.put(foreignKeyColumn, defaultValue);
	}

	private void deleteRecord(final String tableName, final String primaryKeyColumn, final Integer recordID){
		store.get(tableName)
			.values()
			.removeIf(record -> record.get(primaryKeyColumn).equals(recordID));
	}


	/** NOTE: remember to appropriately manage on "CASCADE", "SET (NULL|DEFAULT)", on "RESTRICT". */
	private List<ForeignKeyRule> extractForeignRecordsUponDelete(final String tableName){
		final String keyStart = tableName.toUpperCase(Locale.ROOT) + ".";
		final List<ForeignKeyRule> result = new ArrayList<>(0);
		for(final Map.Entry<String, ForeignKeyRule> entry : foreignKeyRules.entrySet())
			if(entry.getKey().startsWith(keyStart))
				result.add(entry.getValue());
		return result;
	}

}
