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

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;


//https://www.tutorialspoint.com/sql/sql-backup-database.htm
//https://www.postgresql.org/docs/current/sql-createtable.html
//https://neo4j.com/docs/java-manual/current/
//https://neo4j.com/docs/getting-started/languages-guides/java/neo4j-ogm/
//https://neo4j.com/docs/ogm-manual/current/tutorial/
//https://neo4j.com/docs/java-reference/current/java-embedded/setup/
public class GraphDatabaseManager{

	private static GraphDatabaseService graphDB;


	public static Transaction getTransaction(){
		if(graphDB == null){
			final Path path = new File("genealogy_db")
				.toPath();
			final DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(path)
				.setConfig(GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds(60))
				.setConfig(GraphDatabaseSettings.preallocate_logical_logs, true)
				.build();
			Runtime.getRuntime()
				.addShutdownHook(new Thread(managementService::shutdown));
			graphDB = managementService.database("neo4j");
		}
		return graphDB.beginTx();
	}


	public final void insert(final String tableName, final Map<String, Object> record) throws SQLException{
		final Transaction tx = GraphDatabaseManager.getTransaction();
		try{
			final Node car = tx.createNode(Label.label(tableName));
			for(final Map.Entry<String, Object> entry : record.entrySet()){
				car.setProperty("make", "tesla");
			}
			car.setProperty("model", "model3");

			tx.commit();
		}
		catch(final Exception e){
			if(tx != null)
				tx.rollback();

			throw e;
		}
		finally{
			if(tx != null)
				tx.close();
		}

//		final int length = record.size();
//		final StringJoiner sql = new StringJoiner(", ",
//			"INSERT INTO " + getTableName(tableName) + " (",
//			") VALUES (" + StringUtils.repeat(", ?", length).substring(2) + ")");
//		final Object[] fields = new Object[length];
//
//		//collect values
//		int index = 0;
//		for(final Map.Entry<String, Object> entry : record.entrySet()){
//			final String key = entry.getKey();
//			final Object value = entry.getValue();
//
//			sql.add(key.toUpperCase(Locale.ROOT));
//			fields[index] = value;
//
//			index ++;
//		}
//
//		try(
//				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
//				final PreparedStatement stmt = connection.prepareStatement(sql.toString())){
//			for(int i = 0; i < length; i ++)
//				stmt.setObject(i + 1, fields[i]);
//
//			stmt.executeUpdate();
//		}
	}

	public final void update(final String tableName, final Map<String, Object> record) throws SQLException{
//		final StringJoiner sql = new StringJoiner(", ",
//			"UPDATE " + getTableName(tableName) + " SET ",
//			" WHERE ID = " + record.get("id"));
//		//TODO speed up execution?
//		for(final Map.Entry<String, Object> entry : record.entrySet()){
//			final String key = entry.getKey();
//			final Object value = entry.getValue();
//
//			if(!"id".equalsIgnoreCase(key))
//				sql.add(key.toUpperCase(Locale.ROOT) + " = " + value);
//		}
//
//		try(
//				final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
//				final PreparedStatement stmt = connection.prepareStatement(sql.toString())){
//			stmt.executeUpdate();
//		}
	}


	public void delete(final String tableName, final Integer recordID) throws SQLException{
//		final String sql = String.format("DELETE FROM %s WHERE id = ?", getTableName(tableName));
//		try(
//			final Connection connection = DriverManager.getConnection(jdbcURL, user, password);
//			final PreparedStatement stmt = connection.prepareStatement(sql)){
//			stmt.setInt(1, recordID);
//			stmt.executeUpdate();
//		}
	}

	private static String getTableName(String tableName){
		return "\"" + tableName.toUpperCase(Locale.ROOT) + "\"";
	}


	public static void main(String[] args) {
		final Transaction tx = getTransaction();
		try{
			final Node car = tx.createNode(Label.label("Car"));
			car.setProperty("make", "tesla");
			car.setProperty("model", "model3");

			final Node owner = tx.createNode(Label.label("Person"));
			owner.setProperty("firstName", "baeldung");
			owner.setProperty("lastName", "baeldung");

			final Relationship relationship = owner.createRelationshipTo(car, RelationshipType.withName("owner"));
			relationship.setProperty("message", "brave Neo4j");

			final Result result = tx.execute(
				"MATCH (c:Car) <-[owner]- (p:Person) "
					+ "WHERE c.make = 'tesla'"
					+ "RETURN p.firstName, p.lastName");
			while(result.hasNext()){
				final Map<String, Object> row = result.next();
				System.out.println("First Name: " + row.get("p.firstName"));
				System.out.println("Last Name: " + row.get("p.lastName"));
			}

			final Node firstNode = tx.findNode(Label.label("Car"), "make", "tesla");
			final Node secondNode = tx.findNode(Label.label("Person"), "firstName", "baeldung");
			firstNode.getSingleRelationship(RelationshipType.withName("owner"), Direction.INCOMING)
				.delete();
			firstNode.delete();
			secondNode.delete();

			tx.commit();
		}
		catch(final Exception e){
			if(tx != null)
				tx.rollback();

			throw e;
		}
		finally{
			if(tx != null)
				tx.close();
		}

//		System.out.println("Database initialized successfully.");
	}

}
