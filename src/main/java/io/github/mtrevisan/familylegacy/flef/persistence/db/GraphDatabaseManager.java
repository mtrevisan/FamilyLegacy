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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


//https://www.tutorialspoint.com/sql/sql-backup-database.htm
//https://www.postgresql.org/docs/current/sql-createtable.html
//https://neo4j.com/docs/java-manual/current/
//https://neo4j.com/docs/getting-started/languages-guides/java/neo4j-ogm/
//https://neo4j.com/docs/ogm-manual/current/tutorial/
//https://neo4j.com/docs/java-reference/current/java-embedded/setup/
public class GraphDatabaseManager{

	public enum OnDeleteType{CASCADE, RELATIONSHIP_ONLY, RESTRICT}

	private static final String PROPERTY_ON_DELETE = "___onDelete__";


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


	public static void clearDatabase(){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			tx.execute("MATCH ()-[r]->() DELETE r");

			tx.execute("MATCH (n) DELETE n");

			tx.commit();
		}
	}

	public static void insert(final String tableName, final Map<String, Object> record){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node node = tx.createNode(Label.label(tableName));
			for(final Map.Entry<String, Object> entry : record.entrySet())
				node.setProperty(entry.getKey(), entry.getValue());

			tx.commit();
		}
	}

	public static void update(final String tableName, final String primaryColumnName, final Map<String, Object> record)
			throws StoreException{
		final Object nodeID = record.get(primaryColumnName);
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node node = tx.findNode(Label.label(tableName), primaryColumnName, nodeID);
			if(node == null)
				throw StoreException.create("Node with " + primaryColumnName.toUpperCase(Locale.ROOT) + " " + nodeID
					+ " not found in " + tableName.toUpperCase(Locale.ROOT));

			for(final Map.Entry<String, Object> entry : record.entrySet())
				node.setProperty(entry.getKey(), entry.getValue());

			tx.commit();
		}
	}

	public static Map<String, Object> findBy(final String tableName, final String primaryPropertyName, final Object nodeID){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node node = tx.findNode(Label.label(tableName), primaryPropertyName, nodeID);
			return node.getAllProperties();
		}
	}

	public static List<Map<String, Object>> findAllBy(final String tableName, final String propertyName, final Object propertyValue){
		final List<Map<String, Object>> records = new ArrayList<>(0);
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final ResourceIterator<Node> itr = tx.findNodes(Label.label(tableName), propertyName, propertyValue);
			while(itr.hasNext())
				records.add(itr.next().getAllProperties());
			return records;
		}
	}

	public static void delete(final String tableName, final String primaryPropertyName, final Object nodeID) throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node node = tx.findNode(Label.label(tableName), primaryPropertyName, nodeID);
			if(node == null)
				throw StoreException.create("Node with " + primaryPropertyName.toUpperCase(Locale.ROOT) + " " + nodeID
					+ " not found in " + tableName);

			final Deque<Node> nodesToDelete = new ArrayDeque<>();
			nodesToDelete.push(node);
			while(!nodesToDelete.isEmpty()){
				final Node currentNode = nodesToDelete.pop();
				for(final Relationship relationship : currentNode.getRelationships()){
					final Node otherNode = relationship.getOtherNode(currentNode);

					if(otherNode != null && otherNode != currentNode){
						final OnDeleteType onDelete = (relationship.hasProperty(PROPERTY_ON_DELETE)
							? OnDeleteType.valueOf((String)relationship.getProperty(PROPERTY_ON_DELETE))
							: OnDeleteType.RESTRICT);
						switch(onDelete){
							case CASCADE -> nodesToDelete.push(otherNode);
							case RELATIONSHIP_ONLY -> {}
							case RESTRICT ->
								//produce an error indicating that the deletion would create a foreign key constraint violation (this is the
								// default action)
								throw StoreException.create("Cannot remove node, there's a reference to it from node with {} {}, relation {} ",
									primaryPropertyName.toUpperCase(Locale.ROOT), nodeID, relationship);
						}
					}

					relationship.delete();
				}

				currentNode.delete();
			}

			tx.commit();
		}
	}


	public static void upsertRelationship(final String tableNameStart, final String primaryPropertyNameStart, final Object nodeIDStart,
			final String tableNameEnd, final String primaryPropertyNameEnd, final Object nodeIDEnd,
			final String relationshipName, final Map<String, Object> record, final OnDeleteType onDelete) throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
					nodeIDStart, tableNameStart);
			final Node nodeEnd = tx.findNode(Label.label(tableNameEnd), primaryPropertyNameEnd, nodeIDEnd);
			if(nodeEnd == null)
				throw StoreException.create("End node with {} {} not found in {}", primaryPropertyNameEnd.toUpperCase(Locale.ROOT),
					nodeIDEnd, tableNameEnd);

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			final boolean hasRelationships = relationships.iterator()
				.hasNext();
			final Relationship relationship = (hasRelationships
				? findRelationship(relationships, nodeEnd)
				: nodeStart.createRelationshipTo(nodeEnd, RelationshipType.withName(relationshipName)));
			setRelationshipProperties(relationship, record, onDelete);

			tx.commit();
		}
	}

	private static Relationship findRelationship(final ResourceIterable<Relationship> relationships, final Node nodeEnd){
		for(final Relationship relationship : relationships)
			if(relationship.getEndNode().equals(nodeEnd))
				return relationship;
		return null;
	}

	private static void setRelationshipProperties(final Relationship relationship, final Map<String, Object> record,
			final OnDeleteType onDelete){
		if(relationship != null){
			final Map<String, Object> rec = (record == null? new HashMap<>(1): new HashMap<>(record));
			rec.put(PROPERTY_ON_DELETE, onDelete.toString());
			for(final Map.Entry<String, Object> entry : rec.entrySet())
				relationship.setProperty(entry.getKey(), entry.getValue());
		}
	}

	public static boolean deleteRelationship(final String tableNameStart, final String primaryPropertyNameStart, final Object nodeIDStart,
			final String tableNameEnd, final String primaryPropertyNameEnd, final Object nodeIDEnd,
			final String relationshipName) throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
					nodeIDStart, tableNameStart);
			final Node nodeEnd = tx.findNode(Label.label(tableNameEnd), primaryPropertyNameEnd, nodeIDEnd);
			if(nodeEnd == null)
				throw StoreException.create("End node with {} {} not found in {}", primaryPropertyNameEnd.toUpperCase(Locale.ROOT),
					nodeIDEnd, tableNameEnd);

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			final boolean hasRelationships = relationships.iterator()
				.hasNext();
			for(final Relationship relationship : relationships)
				if(relationship.getEndNode().equals(nodeEnd))
					relationship.delete();

			tx.commit();

			return hasRelationships;
		}
	}


	public static Map<String, Object> findOtherRecord(final String tableNameStart, final String primaryPropertyNameStart,
			final Object nodeIDStart, final String relationshipName) throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
					nodeIDStart, tableNameStart);

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			if(relationships.stream().count() > 1)
				throw StoreException.create("More than one node found");
			Map<String, Object> otherRecord = null;
			for(final Relationship relationship : relationships)
				otherRecord = relationship.getEndNode()
					.getAllProperties();

			tx.commit();

			return otherRecord;
		}
	}

	public static Map<String, Object> findOtherRecord(final String tableNameStart, final String primaryPropertyNameStart,
		final Object nodeIDStart, final String relationshipName, final String propertyName, final Object propertyValue) throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
					nodeIDStart, tableNameStart);

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			if(relationships.stream().count() > 1)
				throw StoreException.create("More than one node found");
			Map<String, Object> otherRecord = null;
			for(final Relationship relationship : relationships)
				if(relationship.getProperty(propertyName).equals(propertyValue))
					otherRecord = relationship.getEndNode()
						.getAllProperties();

			tx.commit();

			return otherRecord;
		}
	}


	public static void main(String[] args) throws StoreException{
		GraphDatabaseManager.clearDatabase();

		Map<String, Object> carRecord = new HashMap<>(3);
		carRecord.put("id", 1);
		carRecord.put("make", "tesla");
		carRecord.put("model", "model3");
		GraphDatabaseManager.insert("Car", carRecord);

		carRecord.put("model", "model4");
		GraphDatabaseManager.update("Car", "id", carRecord);

		Map<String, Object> ownerRecord = new HashMap<>(3);
		ownerRecord.put("id", 2);
		ownerRecord.put("firstName", "baeldung");
		ownerRecord.put("lastName", "baeldung");
		GraphDatabaseManager.insert("Person", ownerRecord);

		List<Map<String, Object>> res = GraphDatabaseManager.findAllBy("Car", "id", 1);
		Map<String, Object> carTesla = GraphDatabaseManager.findBy("Car", "id", 1);

		Map<String, Object> relationshipRecord = new HashMap<>(1);
		relationshipRecord.put("licenseID", 12345);
		GraphDatabaseManager.upsertRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner", relationshipRecord, OnDeleteType.CASCADE);

		boolean deleted = GraphDatabaseManager.deleteRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner");

		GraphDatabaseManager.upsertRelationship("Person", "id", 2,
			"Car", "id", 1,
			"owner", relationshipRecord, OnDeleteType.CASCADE);

		GraphDatabaseManager.findOtherRecord("Person", "id", 2,
			"owner");
		GraphDatabaseManager.findOtherRecord("Person", "id", 2,
			"owner", "licenseID", 12345);

		try(final Transaction tx = getTransaction()){
			final Result result = tx.execute(
				"MATCH (c:Car) <-[owner]- (p:Person) "
					+ "WHERE c.make = 'tesla'"
					+ "RETURN p.firstName, p.lastName");
			while(result.hasNext()){
				final Map<String, Object> row = result.next();
				System.out.println("First Name: " + row.get("p.firstName"));
				System.out.println("Last Name: " + row.get("p.lastName"));
			}

			tx.commit();
		}

		try(Transaction tx = GraphDatabaseManager.getTransaction()){
			Result nodeCountResult = tx.execute("MATCH (n) RETURN count(n) AS nodeCount");
			Object nodeCount = nodeCountResult.stream().iterator().next().get("nodeCount");

			Result res2 = tx.execute("MATCH ()-[r]-() RETURN r");
			while(res2.hasNext()){
				final Object row = res2.next();
				System.out.println("First Name: " + row);
			}
			Result relationshipCountResult = tx.execute("MATCH ()-[r]-() RETURN count(r) AS relationshipCount");
			Object relationshipCount = relationshipCountResult.stream().iterator().next().get("relationshipCount");

			System.out.println(nodeCount + "/" + relationshipCount);
			tx.commit();
		}

//		GraphDatabaseManager.deleteRelationship("Person", "id", 2,
//			"Car", "id", 1,
//			"owner");

		GraphDatabaseManager.delete("Car", "id", 1);

		try(Transaction tx = GraphDatabaseManager.getTransaction()){
			Result nodeCountResult = tx.execute("MATCH (n) RETURN count(n) AS nodeCount");
			Object nodeCount = nodeCountResult.stream().iterator().next().get("nodeCount");

			Result res2 = tx.execute("MATCH ()-[r]-() RETURN r");
			while(res2.hasNext()){
				final Object row = res2.next();
				System.out.println("First Name: " + row);
			}
			Result relationshipCountResult = tx.execute("MATCH ()-[r]-() RETURN count(r) AS relationshipCount");
			Object relationshipCount = relationshipCountResult.stream().iterator().next().get("relationshipCount");

			System.out.println(nodeCount + "/" + relationshipCount);
			tx.commit();
		}
	}

}
