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

import io.github.mtrevisan.familylegacy.flef.helpers.JavaHelper;
import org.apache.commons.lang3.StringUtils;
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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;


public class GraphDatabaseManager{

	public enum OnDeleteType{CASCADE, RELATIONSHIP_ONLY, RESTRICT}

	private static final String PROPERTY_ON_DELETE = "onDelete";
	private static final String PROPERTY_ON_DELETE_START = "onDeleteStart";
	private static final String PROPERTY_ON_DELETE_END = "onDeleteEnd";
	private static final String QUERY_COUNT_PARAMETER = "nodeCount";
	private static final String QUERY_COUNT = "MATCH (n:{}) RETURN COUNT(n) AS " + QUERY_COUNT_PARAMETER;


	private static GraphDatabaseService graphDB;


	private GraphDatabaseManager(){}


	static Transaction getTransaction(){
		if(graphDB == null){
			final Path path = Path.of("genealogy_db");
			final DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(path)
				.setConfig(GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds(60))
				.setConfig(GraphDatabaseSettings.preallocate_logical_logs, true)
				.setConfig(GraphDatabaseSettings.pagecache_buffered_flush_enabled, true)
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


	public static int count(final String tableName){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Result result = tx.execute(JavaHelper.textFormat(QUERY_COUNT, tableName));

			return (result.hasNext()
				? ((Number)result.next().get(QUERY_COUNT_PARAMETER)).intValue()
				: 0);
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
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Object nodeID = record.get(primaryColumnName);
			final Node node = tx.findNode(Label.label(tableName), primaryColumnName, nodeID);
			if(node == null)
				throw StoreException.create("Node with {} {} not found in {}", primaryColumnName.toUpperCase(Locale.ROOT), nodeID,
					tableName.toUpperCase(Locale.ROOT));

			for(final Map.Entry<String, Object> entry : record.entrySet())
				node.setProperty(entry.getKey(), entry.getValue());

			tx.commit();
		}
	}

	public static Map<String, Object> findBy(final String tableName, final String primaryPropertyName, final Object propertyValue){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node node = tx.findNode(Label.label(tableName), primaryPropertyName, propertyValue);
			return (node != null? node.getAllProperties(): null);
		}
	}

	public static List<Map<String, Object>> findAll(final String tableName){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final ResourceIterator<Node> itr = tx.findNodes(Label.label(tableName));

			final List<Map<String, Object>> records = new ArrayList<>(0);
			while(itr.hasNext())
				records.add(itr.next().getAllProperties());
			return records;
		}
	}

	//FIXME to be removed?
	public static NavigableMap<Integer, Map<String, Object>> findAllNavigable(final String tableName){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final ResourceIterator<Node> itr = tx.findNodes(Label.label(tableName));

			final NavigableMap<Integer, Map<String, Object>> records = new TreeMap<>();;
			while(itr.hasNext()){
				final Map<String, Object> record = itr.next().getAllProperties();
				records.put(extractRecordID(record), record);
			}
			return records;
		}
	}

	public static List<Map<String, Object>> findAllBy(final String tableName, final String propertyName, final Object propertyValue){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final ResourceIterator<Node> itr = tx.findNodes(Label.label(tableName), propertyName, propertyValue);

			final List<Map<String, Object>> records = new ArrayList<>(0);
			while(itr.hasNext())
				records.add(itr.next().getAllProperties());
			return records;
		}
	}

	public static void delete(final String tableName, final String primaryPropertyName, final Object nodeID) throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node node = tx.findNode(Label.label(tableName), primaryPropertyName, nodeID);
			if(node == null)
				throw StoreException.create("Node with {} {} not found in {}", primaryPropertyName.toUpperCase(Locale.ROOT), nodeID,
					tableName.toUpperCase(Locale.ROOT));

			final Deque<Node> nodesToDelete = new ArrayDeque<>();
			nodesToDelete.push(node);
			while(!nodesToDelete.isEmpty()){
				final Node currentNode = nodesToDelete.pop();
				for(final Relationship relationship : currentNode.getRelationships()){
					final Node otherNode = relationship.getOtherNode(currentNode);

					if(otherNode != null && otherNode != currentNode){
						String onDeletePropertyName = (relationship.getStartNode().equals(currentNode)
							? PROPERTY_ON_DELETE_START
							: PROPERTY_ON_DELETE_END);
						if(!relationship.hasProperty(onDeletePropertyName))
							onDeletePropertyName = PROPERTY_ON_DELETE;
						final OnDeleteType onDelete = (relationship.hasProperty(onDeletePropertyName)
							? OnDeleteType.valueOf((String)relationship.getProperty(onDeletePropertyName))
							: OnDeleteType.RESTRICT);
						switch(onDelete){
							case CASCADE -> nodesToDelete.push(otherNode);
							case RELATIONSHIP_ONLY -> {}
							case RESTRICT ->
								//produce an error indicating that the deletion would create a foreign key constraint violation (this is the
								// default action)
								throw StoreException.create("Cannot remove node, there's a reference to it from node with {} {}, relation {} ",
									primaryPropertyName.toUpperCase(Locale.ROOT), nodeID, relationship.toString().toUpperCase(Locale.ROOT));
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
			final String relationshipName, final Map<String, Object> record, final OnDeleteType onDeleteStart, final OnDeleteType onDeleteEnd)
			throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
					nodeIDStart, tableNameStart.toUpperCase(Locale.ROOT));
			final Node nodeEnd = tx.findNode(Label.label(tableNameEnd), primaryPropertyNameEnd, nodeIDEnd);
			if(nodeEnd == null)
				throw StoreException.create("End node with {} {} not found in {}", primaryPropertyNameEnd.toUpperCase(Locale.ROOT),
					nodeIDEnd, tableNameEnd.toUpperCase(Locale.ROOT));

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			final boolean hasRelationships = relationships.iterator()
				.hasNext();
			final Relationship relationship = (hasRelationships
				? findRelationship(relationships, nodeEnd)
				: nodeStart.createRelationshipTo(nodeEnd, RelationshipType.withName(relationshipName)));
			setRelationshipProperties(relationship, record, onDeleteStart, onDeleteEnd);

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
			final OnDeleteType onDeleteStart, final OnDeleteType onDeleteEnd){
		if(relationship != null){
			final Map<String, Object> rec = (record == null? new HashMap<>(1): new HashMap<>(record));
			if(Objects.equals(onDeleteStart, onDeleteEnd))
				rec.put(PROPERTY_ON_DELETE, onDeleteStart.toString());
			else{
				rec.put(PROPERTY_ON_DELETE_START, onDeleteStart.toString());
				rec.put(PROPERTY_ON_DELETE_END, onDeleteEnd.toString());
			}
			for(final Map.Entry<String, Object> entry : rec.entrySet())
				relationship.setProperty(entry.getKey(), entry.getValue());
		}
	}

	public static boolean deleteRelationship(final String tableNameStart, final String primaryPropertyNameStart, final Object nodeIDStart,
			final String tableNameEnd, final String primaryPropertyNameEnd, final Object nodeIDEnd,
			final String relationshipName){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
//				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
//					nodeIDStart, tableNameStart.toUpperCase(Locale.ROOT));
				return false;
			final Node nodeEnd = tx.findNode(Label.label(tableNameEnd), primaryPropertyNameEnd, nodeIDEnd);
			if(nodeEnd == null)
//				throw StoreException.create("End node with {} {} not found in {}", primaryPropertyNameEnd.toUpperCase(Locale.ROOT),
//					nodeIDEnd, tableNameEnd.toUpperCase(Locale.ROOT));
				return false;

			final ResourceIterable<Relationship> relationships = (relationshipName != null
				? nodeStart.getRelationships(RelationshipType.withName(relationshipName))
				: nodeStart.getRelationships());
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
//				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
//					nodeIDStart, tableNameStart.toUpperCase(Locale.ROOT));
				return null;

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			if(relationships.stream().count() > 1)
				throw StoreException.create("More than one node found from {} {} with relationship {}",
					primaryPropertyNameStart.toUpperCase(Locale.ROOT), nodeIDStart, relationshipName.toUpperCase(Locale.ROOT));
			Map<String, Object> otherRecord = null;
			for(final Relationship relationship : relationships)
				otherRecord = relationship.getEndNode()
					.getAllProperties();
			return otherRecord;
		}
	}

	public static Map<String, Object> findOtherRecord(final String tableNameStart, final String primaryPropertyNameStart,
		final Object nodeIDStart, final String relationshipName, final String propertyName, final Object propertyValue) throws StoreException{
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
//				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
//					nodeIDStart, tableNameStart.toUpperCase(Locale.ROOT));
				return null;

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			if(relationships.stream().count() > 1)
				throw StoreException.create("More than one node found");
			Map<String, Object> otherRecord = null;
			for(final Relationship relationship : relationships)
				if(relationship.getProperty(propertyName).equals(propertyValue))
					otherRecord = relationship.getEndNode()
						.getAllProperties();
			return otherRecord;
		}
	}


	public static String logDatabase(){
		try(final Transaction tx = GraphDatabaseManager.getTransaction()){
			final StringJoiner sj = new StringJoiner("\n");
			Result result = tx.execute("MATCH (n) RETURN n");
			while(result.hasNext())
				sj.add(result.next().get("n").toString());

			sj.add(StringUtils.EMPTY);

			result = tx.execute("MATCH ()-[r]->() RETURN r");
			while(result.hasNext())
				sj.add(result.next().get("r").toString());

			return sj.toString();
		}
	}

}
