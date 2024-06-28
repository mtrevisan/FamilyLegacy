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
package io.github.mtrevisan.familylegacy.flef;

import io.github.mtrevisan.familylegacy.flef.sql.GenericColumn;
import io.github.mtrevisan.familylegacy.flef.sql.GenericRecord;
import io.github.mtrevisan.familylegacy.flef.sql.GenericTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DatabaseManager{

	private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?i)CREATE\\s+TABLE\\s+(\"?[^\\s\\r\\n(]+\"?)[^;]*?;");
	//https://stackoverflow.com/questions/6720050/foreign-key-constraints-when-to-use-on-update-and-on-delete
	private static final Pattern FOREIGN_KEY_PATTERN = Pattern.compile("(?i)(([^\\s]+)\\s+[^\\s]+\\s+)?(FOREIGN\\s+KEY(\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\))?\\s+REFERENCES\\s+\"?([^\\s\"]+)\"?\\s+\\(\\s*\"?([^\\s\"]+)\"?\\s*\\)(\\s+ON\\s+(?:DELETE|UPDATE)\\s+[^,]+)?),?");
	private static final Pattern ALTER_TABLE_PATTERN = Pattern.compile("(?i)ALTER\\s+TABLE.*?ADD\\s+CONSTRAINT.*?FOREIGN\\s+KEY.*?;");


	private final String jdbcURL;
	private final String user;
	private final String password;


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

			final Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(sql);
			while(tableMatcher.find()){
				String createTableStatement = tableMatcher.group();
				final String tableName = tableMatcher.group(1);

				//remove inline foreign keys and add them to the foreignKeyConstraints list
				final Matcher foreignKeyMatcher = FOREIGN_KEY_PATTERN.matcher(createTableStatement);
				final Collection<String> currentForeignKeyConstraints = new ArrayList<>(0);
				while(foreignKeyMatcher.find()){
					String foreignKey = foreignKeyMatcher.group(3);

					createTableStatement = createTableStatement.replaceAll(Pattern.quote(foreignKey), "");

					if(foreignKeyMatcher.group(4) == null)
						//add table column
						foreignKey = foreignKey.replace("REFERENCES", "(" + foreignKeyMatcher.group(2) + ") REFERENCES");

					foreignKey = "ALTER TABLE " + tableName + " ADD " + foreignKey;

					currentForeignKeyConstraints.add(foreignKey);
				}

				createTableStatement = createTableStatement.replaceAll("--[^\\r\\n]+[\\r\\n]+", "")
					.replaceAll("/*.*?\\*/", "")
					.replaceAll("(,[\\s\\r\\n]+){1,}\\)", ")");
				tableCreations.add(createTableStatement);
				foreignKeyConstraints.addAll(currentForeignKeyConstraints);
			}

			final Matcher foreignKeyMatcher = ALTER_TABLE_PATTERN.matcher(sql);
			while(foreignKeyMatcher.find())
				foreignKeyConstraints.add(foreignKeyMatcher.group());

			//execute table creation scripts
//			for(final String tableCreation : tableCreations)
//				RunScript.execute(connection, new StringReader(tableCreation));

			//execute foreign key constraint scripts
//			for(final String foreignKeyConstraint : foreignKeyConstraints)
//				RunScript.execute(connection, new StringReader(foreignKeyConstraint));

			System.out.println("Database initialized successfully.");
		}
	}


	public void insert(final GenericTable table, final GenericRecord record) throws SQLException{
		final StringJoiner fieldNames = new StringJoiner(", ", "(", ")");
		final StringJoiner placeholders = new StringJoiner(", ", "(", ")");

		final String tableName = table.getName();
		final List<GenericColumn> columns = table.getColumns();
		for(int i = 0, length = columns.size(); i < length; i ++){
			fieldNames.add(columns.get(i).getName());
			placeholders.add("?");
		}

		final String sql = "INSERT INTO \"" + tableName + "\" " + fieldNames + " VALUES " + placeholders;

		try(
				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
			 	final PreparedStatement pstmt = connection.prepareStatement(sql);
				){
			final Object[] fields = record.getFields();
			for(int i = 0, length = fields.length; i < length; i ++)
				pstmt.setObject(i + 1, fields[i]);

			pstmt.executeUpdate();
		}
	}


	public void update(final GenericTable table, final GenericRecord record) throws SQLException{
		final String tableName = table.getName();
		final List<GenericColumn> columns = table.getColumns();
		final Object[] fields = record.getFields();

		//TODO

		final String sql = "UPDATE \"" + tableName + "\" SET SOURCE_ID = ?, LOCATION = ?, EXTRACT_ID = ?, EXTRACT_TYPE = ? WHERE ID = ?";

		try(
				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
				final PreparedStatement pstmt = connection.prepareStatement(sql);
				){
			pstmt.executeUpdate();
		}
	}

}
