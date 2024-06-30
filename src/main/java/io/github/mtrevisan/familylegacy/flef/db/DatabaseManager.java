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
package io.github.mtrevisan.familylegacy.flef.db;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DatabaseManager implements DatabaseManagerInterface{

	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(\"?[^\\s\\r\\n(]+\"?)[^;]*?;");
	//https://stackoverflow.com/questions/6720050/foreign-key-constraints-when-to-use-on-update-and-on-delete
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("(?i)(([^\\s]+)\\s+[^\\s]+\\s+)?(FOREIGN\\s+KEY(\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\))?\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)(\\s+ON\\s+(?:DELETE|UPDATE)\\s+[^,);\r\n]+)?),?");
	private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile("(?i)ALTER\\s+TABLE.*?ADD\\s+CONSTRAINT.*?FOREIGN\\s+KEY.*?;");


	private final String jdbcURL;
	private final String user;
	private final String password;

	private Graph<String, DefaultEdge> graph;


	public DatabaseManager(final String jdbcURL, final String user, final String password){
		this.jdbcURL = jdbcURL;
		this.user = user;
		this.password = password;
	}


	public void initialize(final String sqlFile) throws SQLException, IOException{
		try(final Connection connection = DriverManager.getConnection(jdbcURL, user, password)){
			final String sql = Files.readString(Paths.get(sqlFile));

			//separate table creation and foreign key constraints
			final Collection<String> tableCreations = new ArrayList<>(0);
			final Collection<String> foreignKeyConstraints = new ArrayList<>(0);

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
				final Collection<String> currentForeignKeyConstraints = new ArrayList<>(0);
				while(foreignKeyMatcher.find()){
					String foreignKey = foreignKeyMatcher.group(3);

					createTableStatement = createTableStatement.replaceAll(Pattern.quote(foreignKey), "");

					if(foreignKeyMatcher.group(4) == null)
						//add table column
						foreignKey = foreignKey.replace("REFERENCES", "(" + foreignKeyMatcher.group(2) + ") REFERENCES");

					final String plainTableNameFrom = StringUtils.replaceChars(foreignKeyMatcher.group(6), "\"", "");
					if(!plainTableNameFrom.equalsIgnoreCase(plainTableNameTo)){
						directedEdgesFrom.add(plainTableNameFrom);
						directedEdgesTo.add(plainTableNameTo);
					}

					foreignKey = "ALTER TABLE " + tableName + " ADD " + foreignKey;

					currentForeignKeyConstraints.add(foreignKey);
				}

				createTableStatement = createTableStatement.replaceAll("--[^\\r\\n]+[\\r\\n]+", "")
					.replaceAll("/*.*?\\*/", "")
					.replaceAll("(,[\\s\\r\\n]+){1,}\\)", ")");
				tableCreations.add(createTableStatement);
				foreignKeyConstraints.addAll(currentForeignKeyConstraints);
			}

			for(int i = 0, length = directedEdgesFrom.size(); i < length; i ++)
				graph.addEdge(directedEdgesFrom.get(i), directedEdgesTo.get(i));

			final Matcher foreignKeyMatcher = ALTER_TABLE_PATTERN.matcher(sql);
			while(foreignKeyMatcher.find())
				foreignKeyConstraints.add(foreignKeyMatcher.group());

			//execute table creation scripts
			for(final String tableCreation : tableCreations)
				RunScript.execute(connection, new StringReader(tableCreation));

			//execute foreign key constraint scripts
			for(final String foreignKeyConstraint : foreignKeyConstraints)
				RunScript.execute(connection, new StringReader(foreignKeyConstraint));

			System.out.println("Database initialized successfully.");
		}
	}


	@Override
	public Map<String, Integer> extractIdentifierToIDMap(final String tableName){
		try{
			return extractIdenToIDMap(tableName, "identifier");
		}
		catch(SQLException e){
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<String, Integer> extractDateToIDMap(final String tableName){
		try{
			return extractIdenToIDMap(tableName, "date");
		}
		catch(SQLException e){
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<String, Integer> extractTypeToIDMap(final String tableName){
		try{
			return extractIdenToIDMap(tableName, "type");
		}
		catch(SQLException e){
			return Collections.emptyMap();
		}
	}

	public Map<String, Integer> extractIdenToIDMap(final String tableName, final String iden) throws SQLException{
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
	public Map<String, Integer> extractPersonIdentifierToIDMap(final String tableName){
		final String sql = "SELECT lt.text AS text, pn.id as id FROM PERSON_NAME pn, LOCALIZED_TEXT_JUNCTION ltj, LOCALIZED_TEXT lt" +
			" WHERE pn.NAME_ID = ltj.REFERENCE_ID AND ltj.REFERENCE_TABLE = 'person' AND lt.ID = ltj.LOCALIZED_TEXT_ID";

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
		catch(SQLException e){
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<String, Integer> extractGroupIdentifierToIDMap(final String tableName){
		//TODO
		final String sql = "SELECT lt.text AS text, pn.id as id FROM GROUP g, GROUP_JUNCTION gj, PERSON_NAME pn, LOCALIZED_TEXT_JUNCTION ltj, LOCALIZED_TEXT lt" +
			" WHERE pn.NAME_ID = ltj.REFERENCE_ID AND ltj.REFERENCE_TABLE = 'person' AND lt.ID = ltj.LOCALIZED_TEXT_ID";

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
		catch(SQLException e){
			return Collections.emptyMap();
		}
	}


	@Override
	public void insertDatabase(final Map<String, TreeMap<Integer, Map<String, Object>>> database) throws SQLException{
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
	public void insert(final String tableName, final Map<String, Object> record) throws SQLException{
		final int length = record.size();
		final StringJoiner sql = new StringJoiner(", ",
			"INSERT INTO \"" + tableName.toUpperCase(Locale.ROOT) + "\" (",
			") VALUES (" + StringUtils.repeat(", ?", length).substring(2) + ")");
		final Object[] fields = new Object[length];

		int index = 0;
		for(final Map.Entry<String, Object> entry : record.entrySet()){
			sql.add(entry.getKey().toUpperCase(Locale.ROOT));
			fields[index] = entry.getValue();

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
	public void update(final String tableName, final Map<String, Object> record) throws SQLException{
		final StringJoiner sql = new StringJoiner(", ",
			"UPDATE \"" + tableName.toUpperCase(Locale.ROOT) + "\" SET ",
			" WHERE ID = " + record.get("id"));
		for(final Map.Entry<String, Object> entry : record.entrySet())
			if(!"id".equalsIgnoreCase(entry.getKey()))
				sql.add(entry.getKey().toUpperCase(Locale.ROOT) + " = " + entry.getValue());

		try(
			final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
			final PreparedStatement stmt = connection.prepareStatement(sql.toString())){
			stmt.executeUpdate();
		}
	}

}
