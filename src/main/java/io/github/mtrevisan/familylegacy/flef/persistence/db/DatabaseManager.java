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
import org.h2.tools.RunScript;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//https://www.tutorialspoint.com/sql/sql-backup-database.htm
//https://www.postgresql.org/docs/current/sql-createtable.html
public class DatabaseManager implements DatabaseManagerInterface{

	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(\"?[^\\s\\r\\n(]+\"?)[^;]*?;");
	//https://stackoverflow.com/questions/6720050/foreign-key-constraints-when-to-use-on-update-and-on-delete
	private static final String FOREIGN_KEY_TRUE_PATTERN = "FOREIGN\\s+KEY(\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\))?\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)(\\s+ON\\s+(DELETE\\s+(CASCADE|SET\\s+(NULL|DEFAULT)|NO\\s+ACTION|RESTRICT)))?";
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("(?i)(" + FOREIGN_KEY_TRUE_PATTERN + "),?");
	private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile("(?i)ALTER\\s+TABLE\\s+(\"?[^\\s\\r\\n(]+\"?)\\s+ADD\\s+CONSTRAINT\\s+(?:[^\\s]+)?\\s+" + FOREIGN_KEY_TRUE_PATTERN + ";");

	private static final String NO_ACTION = "NO ACTION";
	private static final String ALTER_TABLE_ADD_FOREIGN_KEY_ON_DELETE = "ALTER TABLE %s ADD FOREIGN KEY (\"%s\") REFERENCES \"%s\"(\"%s\") ON DELETE %s";

	private static final Pattern PATTERN = Pattern.compile("(,[\\s\\r\\n]+){1,}\\)");


	private record ForeignKeyRule(String foreignKeyColumn, String referencedTable, String referencedID, String onDelete){}


	private final String jdbcURL;
	private final String user;
	private final String password;

	private List<String> tableCreations;
	private Graph<String, DefaultEdge> graph;
	private final Map<String, ForeignKeyRule> foreignKeyRules = new HashMap<>(0);


	public DatabaseManager(final String jdbcURL, final String user, final String password){
		this.jdbcURL = jdbcURL;
		this.user = user;
		this.password = password;
	}


	public final void initialize(final String sqlFile) throws SQLException, IOException{
		try(final Connection connection = DriverManager.getConnection(jdbcURL, user, password)){
			final String sql = Files.readString(Paths.get(sqlFile));

			//separate table creation and foreign key constraints
			tableCreations = new ArrayList<>(0);
			final List<String> foreignKeyConstraints = new ArrayList<>(0);

			graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
			final List<String> directedEdgesFrom = new ArrayList<>(0);
			final List<String> directedEdgesTo = new ArrayList<>(0);

			final Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(sql);
			while(tableMatcher.find()){
				String createTableStatement = tableMatcher.group();
				final String tableName = tableMatcher.group(1);

				final String plainTableNameTo = StringUtils.replaceChars(tableName, "\"", "");
				graph.addVertex(plainTableNameTo);

				//remove inline foreign keys and add them to the foreignKeyConstraints list
				final Matcher foreignKeyMatcher = FOREIGN_KEY_PATTERN.matcher(createTableStatement);
				while(foreignKeyMatcher.find()){
					final String foreignKeyColumn = foreignKeyMatcher.group(3);
					final String referencedTable = foreignKeyMatcher.group(4);
					final String referencedColumn = foreignKeyMatcher.group(5);
					final String onDeleteAction = (foreignKeyMatcher.group(8) != null? foreignKeyMatcher.group(8): NO_ACTION);

					//remove inline foreign keys
					createTableStatement = createTableStatement.replaceAll(Pattern.quote(foreignKeyMatcher.group()), "");

					//add directed edge in the graph
					final String plainTableNameFrom = StringUtils.replaceChars(referencedTable, "\"", "");
					if(!plainTableNameFrom.equalsIgnoreCase(plainTableNameTo)){
						directedEdgesFrom.add(plainTableNameFrom);
						directedEdgesTo.add(plainTableNameTo);
					}

					final String foreignKey = String.format(ALTER_TABLE_ADD_FOREIGN_KEY_ON_DELETE,
						tableName, foreignKeyColumn, referencedTable, referencedColumn, onDeleteAction);

					foreignKeyConstraints.add(foreignKey);

					//store foreign key rule
					final String key = plainTableNameTo + "." + foreignKeyColumn;
					foreignKeyRules.put(key, new ForeignKeyRule(foreignKeyColumn, referencedTable, referencedColumn, onDeleteAction));
				}

				createTableStatement = cleanCreateTableStatement(createTableStatement);
				tableCreations.add(createTableStatement);
			}

			for(int i = 0, length = directedEdgesFrom.size(); i < length; i ++)
				graph.addEdge(directedEdgesFrom.get(i), directedEdgesTo.get(i));

			final Matcher foreignKeyMatcher = ALTER_TABLE_PATTERN.matcher(sql);
			while(foreignKeyMatcher.find()){
				foreignKeyConstraints.add(foreignKeyMatcher.group());

				//store foreign key rule
				final String tableName = foreignKeyMatcher.group(1);
				final String foreignKeyColumn = foreignKeyMatcher.group(3);
				final String referencedTable = foreignKeyMatcher.group(4);
				final String referencedColumn = foreignKeyMatcher.group(5);
				final String onDeleteAction = (foreignKeyMatcher.group(8) != null? foreignKeyMatcher.group(8): NO_ACTION);
				final String key = tableName + "." + foreignKeyColumn;
				foreignKeyRules.put(key, new ForeignKeyRule(foreignKeyColumn, referencedTable, referencedColumn, onDeleteAction));
			}

			//execute table creation scripts
			for(int i = 0, length = tableCreations.size(); i < length; i ++)
				RunScript.execute(connection, new StringReader(tableCreations.get(i)));

			//execute foreign key constraint scripts
			for(int i = 0, length = foreignKeyConstraints.size(); i < length; i ++)
				RunScript.execute(connection, new StringReader(foreignKeyConstraints.get(i)));

			System.out.println("Database initialized successfully.");
		}
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
	public final Map<String, Integer> extractIdentifierToIDMap(final String tableName){
		try{
			return extractIdenToIDMap(tableName, "identifier");
		}
		catch(final SQLException e){
			return Collections.emptyMap();
		}
	}

	@Override
	public final Map<String, Integer> extractDateToIDMap(final String tableName){
		try{
			return extractIdenToIDMap(tableName, "date");
		}
		catch(final SQLException e){
			return Collections.emptyMap();
		}
	}

	@Override
	public final Map<String, Integer> extractTypeToIDMap(final String tableName){
		try{
			return extractIdenToIDMap(tableName, "type");
		}
		catch(final SQLException e){
			return Collections.emptyMap();
		}
	}

	private Map<String, Integer> extractIdenToIDMap(final String tableName, final String iden) throws SQLException{
		final String sql = "SELECT " + iden + ", id FROM " + tableName;

		final Map<String, Integer> identifierToIDMap = new HashMap<>();
		try(
				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
				final PreparedStatement stmt = connection.prepareStatement(sql)){
			final ResultSet rs = stmt.executeQuery();
			while(rs.next()){
				final String identifier = rs.getString(iden);
				final int id = rs.getInt("id");
				identifierToIDMap.put(identifier, id);
			}
		}
		return identifierToIDMap;
	}

	@Override
	public final Map<String, Integer> extractPersonIdentifierToIDMap(final String tableName){
		final String sql = "SELECT lt.text AS text, pn.id as id FROM PERSON_NAME pn, LOCALIZED_TEXT_JUNCTION ltj, LOCALIZED_TEXT lt" +
			" WHERE ltj.REFERENCE_TABLE = 'person' AND lt.ID = ltj.LOCALIZED_TEXT_ID";

		final Map<String, Integer> identifierToIDMap = new HashMap<>();
		try(
				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
				final PreparedStatement stmt = connection.prepareStatement(sql)){
			final ResultSet rs = stmt.executeQuery();
			while(rs.next()){
				final String identifier = rs.getString("text");
				final int id = rs.getInt("id");
				identifierToIDMap.put(identifier, id);
			}
			return identifierToIDMap;
		}
		catch(final SQLException e){
			return Collections.emptyMap();
		}
	}

	@Override
	public final Map<String, Integer> extractGroupIdentifierToIDMap(final String tableName){
		//TODO
		final String sql = "SELECT lt.text AS text, pn.id as id FROM GROUP g, GROUP_JUNCTION gj, PERSON_NAME pn, LOCALIZED_TEXT_JUNCTION ltj, LOCALIZED_TEXT lt" +
			" WHERE ltj.REFERENCE_TABLE = 'person' AND lt.ID = ltj.LOCALIZED_TEXT_ID";

		final Map<String, Integer> identifierToIDMap = new HashMap<>();
		try(
				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
				final PreparedStatement stmt = connection.prepareStatement(sql)){
			final ResultSet rs = stmt.executeQuery();
			while(rs.next()){
				final String identifier = rs.getString("text");
				final int id = rs.getInt("id");
				identifierToIDMap.put(identifier, id);
			}
			return identifierToIDMap;
		}
		catch(final SQLException e){
			return Collections.emptyMap();
		}
	}


	@Override
	public final void insertDatabase(final Map<String, TreeMap<Integer, Map<String, Object>>> database) throws SQLException{
		final TopologicalOrderIterator<String, DefaultEdge> iterator = new TopologicalOrderIterator<>(graph);
		while(iterator.hasNext()){
			final String sortedTableName = iterator.next();

			final TreeMap<Integer, Map<String, Object>> tableRecords = database.get(sortedTableName.toLowerCase(Locale.ROOT));
			if(tableRecords == null || tableRecords.isEmpty())
				continue;

			for(final Map<String, Object> record : tableRecords.values())
				insert(sortedTableName, record);
		}
	}


	@Override
	public final void insert(final String tableName, final Map<String, Object> record) throws SQLException{
		final int length = record.size();
		final StringJoiner sql = new StringJoiner(", ",
			"INSERT INTO " + getTableName(tableName) + " (",
			") VALUES (" + StringUtils.repeat(", ?", length).substring(2) + ")");
		final Object[] fields = new Object[length];

		//collect values
		int index = 0;
		for(final Map.Entry<String, Object> entry : record.entrySet()){
			final String key = entry.getKey();
			final Object value = entry.getValue();

			sql.add(key.toUpperCase(Locale.ROOT));
			fields[index] = value;

			index ++;
		}

		try(
				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
				final PreparedStatement stmt = connection.prepareStatement(sql.toString())){
			for(int i = 0; i < length; i ++)
				stmt.setObject(i + 1, fields[i]);

			stmt.executeUpdate();
		}
	}

	@Override
	public final void update(final String tableName, final Map<String, Object> record) throws SQLException{
		final StringJoiner sql = new StringJoiner(", ",
			"UPDATE " + getTableName(tableName) + " SET ",
			" WHERE ID = " + record.get("id"));
		//TODO speed up execution?
		for(final Map.Entry<String, Object> entry : record.entrySet()){
			final String key = entry.getKey();
			final Object value = entry.getValue();

			if(!"id".equalsIgnoreCase(key))
				sql.add(key.toUpperCase(Locale.ROOT) + " = " + value);
		}

		try(
				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
				final PreparedStatement stmt = connection.prepareStatement(sql.toString())){
			stmt.executeUpdate();
		}
	}


	@Override
	public void delete(final String tableName, final Integer recordID) throws SQLException{
		final String sql = String.format("DELETE FROM %s WHERE id = ?", getTableName(tableName));
		try(
			final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
			final PreparedStatement stmt = connection.prepareStatement(sql)){
			stmt.setInt(1, recordID);
			stmt.executeUpdate();
		}
	}

	private static String getTableName(String tableName){
		return "\"" + tableName.toUpperCase(Locale.ROOT) + "\"";
	}

}
