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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mtrevisan.familylegacy.flef.helpers.JavaHelper;
import io.github.mtrevisan.familylegacy.flef.helpers.LZMAManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphDatabaseManager.class);


	public enum OnDeleteType{CASCADE, RELATIONSHIP_ONLY, RESTRICT}

	public static final String LABEL_APPLICATION = "Application";

	private static final String PROPERTY_ON_DELETE = "onDelete";
	private static final String PROPERTY_ON_DELETE_START = PROPERTY_ON_DELETE + "Start";
	private static final String PROPERTY_ON_DELETE_END = PROPERTY_ON_DELETE + "End";

	private static final String QUERY_PARAMETER_NODE = "n";
	private static final String QUERY_PARAMETER_RELATIONSHIP = "r";
	private static final String QUERY_CLEAR_ALL_RELATIONSHIPS = "MATCH ()-[" + QUERY_PARAMETER_RELATIONSHIP + "]->() DELETE "
		+ QUERY_PARAMETER_RELATIONSHIP;
	private static final String QUERY_CLEAR_ALL_NODES = "MATCH (" + QUERY_PARAMETER_NODE + ") DELETE " + QUERY_PARAMETER_NODE;
	private static final String QUERY_COUNT_PARAMETER = "count";
	private static final String QUERY_COUNT_NODES = "MATCH (" + QUERY_PARAMETER_NODE + ":{}) RETURN COUNT("
		+ QUERY_PARAMETER_NODE + ") AS " + QUERY_COUNT_PARAMETER;
	private static final String QUERY_ALL_NODES = "MATCH (" + QUERY_PARAMETER_NODE + ") RETURN " + QUERY_PARAMETER_NODE;
	private static final String QUERY_ALL_NODES_EXCLUDE_LABEL = "MATCH (" + QUERY_PARAMETER_NODE + ") WHERE '{}' NOT IN labels("
		+ QUERY_PARAMETER_NODE + ") RETURN " + QUERY_PARAMETER_NODE;
	private static final String QUERY_ALL_RELATIONSHIPS = "MATCH ()-[" + QUERY_PARAMETER_RELATIONSHIP + "]-() RETURN "
		+ QUERY_PARAMETER_RELATIONSHIP;
	private static final String QUERY_ALL_CONNECTING_ANY_NODES = "MATCH (" + QUERY_PARAMETER_NODE + ":{})-[]-() RETURN "
		+ QUERY_PARAMETER_NODE;
	private static final String QUERY_ALL_CONNECTING_NODES = "MATCH (" + QUERY_PARAMETER_NODE + ":{})-[:{}]-(:{} {{}: {}}) RETURN "
		+ QUERY_PARAMETER_NODE;
	private static final String QUERY_ALL_CONNECTING_NODES_WITH_PROPERTY = "MATCH (" + QUERY_PARAMETER_NODE
		+ ":{})-[:{} {{}: {}}]->(:{} {{}: {}}) RETURN " + QUERY_PARAMETER_NODE;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String LINE_SEPARATOR = "\n";
	private static final String PRINT_SEPARATOR = "|";
	private static final String PRINT_LABEL_SEPARATOR = ",";
	private static final String DATA_START_SEPARATORS = "|{\"";


	private static GraphDatabaseService graphDB;


	private GraphDatabaseManager(){}


	private static Transaction getTransaction(){
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


	public static byte[] store(final String excludeLabel) throws IOException{
		try{
			final String content = logDatabase(excludeLabel);
			return LZMAManager.compress(content);
		}
		catch(final JsonProcessingException jpe){
			LOGGER.error("Error while storing database", jpe);

			throw new IOException(jpe);
		}
	}

	public static void restore(final byte[] database) throws IOException{
		try(final Transaction tx = getTransaction()){
			clearDatabaseInternal(tx);


			final Map<String, Node> nodes = new HashMap<>(0);

			final String decompressed = LZMAManager.decompress(database);
			final String[] lines = StringUtils.split(decompressed, LINE_SEPARATOR);
			for(int i = 0, length = lines.length; i < length; i ++){
				final String line = lines[i];

				int index = line.indexOf(DATA_START_SEPARATORS);
				final String pre = line.substring(0, index);
				final String data = line.substring(index + 1);
				final int pipes = StringUtils.countMatches(pre, PRINT_SEPARATOR);
				if(pipes == 1){
					//a node
					index = line.indexOf(PRINT_SEPARATOR);
					final String nodeID = pre.substring(0, index);
					final String[] nodeLabels = StringUtils.split(pre.substring(index + 1), PRINT_LABEL_SEPARATOR);
					final Map<String, Object> properties = OBJECT_MAPPER.readValue(data, new TypeReference<>(){});

					final Node node = tx.createNode(Label.label(nodeLabels[0]));
					for(final Map.Entry<String, Object> entry : properties.entrySet())
						node.setProperty(entry.getKey(), entry.getValue());

					nodes.put(nodeID, node);
				}
				else if(pipes == 2){
					//a relationship
					index = line.indexOf(PRINT_SEPARATOR);
					final Node nodeStart = nodes.get(pre.substring(0, index));
					final int index2 = line.indexOf(PRINT_SEPARATOR, index + 1);
					final Node nodeEnd = nodes.get(pre.substring(index + 1, index2));
					index = line.indexOf(PRINT_SEPARATOR, index2 + 1);
					final String relationshipName = pre.substring(index2 + 1, index);
					final Map<String, Object> properties = OBJECT_MAPPER.readValue(data, new TypeReference<>(){});

					final Relationship relationship = nodeStart.createRelationshipTo(nodeEnd, RelationshipType.withName(relationshipName));
					for(final Map.Entry<String, Object> property : properties.entrySet())
						relationship.setProperty(property.getKey(), property.getValue());
				}
			}

			tx.commit();
		}
		catch(final JsonProcessingException jpe){
			LOGGER.error("Error while restoring database", jpe);

			throw new IOException(jpe);
		}
	}

	public static void clearDatabase(){
		try(final Transaction tx = getTransaction()){
			clearDatabaseInternal(tx);

			tx.commit();
		}
	}

	private static void clearDatabaseInternal(final Transaction tx){
		tx.execute(QUERY_CLEAR_ALL_RELATIONSHIPS);

		tx.execute(QUERY_CLEAR_ALL_NODES);
	}


	public static int count(final String tableName){
		try(final Transaction tx = getTransaction()){
			final Result result = tx.execute(JavaHelper.textFormat(QUERY_COUNT_NODES, tableName));

			return (result.hasNext()
				? ((Number)result.next().get(QUERY_COUNT_PARAMETER)).intValue()
				: 0);
		}
	}


	public static void insert(final Map<String, Object> record, final String... tableNames){
		try(final Transaction tx = getTransaction()){
			final Label[] labels = Arrays.stream(tableNames)
				.map(Label::label)
				.toArray(Label[]::new);

			final Node node = tx.createNode(labels);
			for(final Map.Entry<String, Object> entry : record.entrySet())
				node.setProperty(entry.getKey(), entry.getValue());

			tx.commit();
		}
	}

	public static void update(final String tableName, final String primaryColumnName, final Map<String, Object> record)
			throws StoreException{
		try(final Transaction tx = getTransaction()){
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
		try(final Transaction tx = getTransaction()){
			final Node node = tx.findNode(Label.label(tableName), primaryPropertyName, propertyValue);
			return (node != null? node.getAllProperties(): null);
		}
	}

	public static List<Map<String, Object>> findAll(final String tableName){
		try(final Transaction tx = getTransaction()){
			final ResourceIterator<Node> itr = tx.findNodes(Label.label(tableName));

			final List<Map<String, Object>> records = new ArrayList<>(0);
			while(itr.hasNext())
				records.add(itr.next().getAllProperties());
			return records;
		}
	}

	public static NavigableMap<Integer, Map<String, Object>> findAllNavigable(final String tableName){
		try(final Transaction tx = getTransaction()){
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
		try(final Transaction tx = getTransaction()){
			final ResourceIterator<Node> itr = tx.findNodes(Label.label(tableName), propertyName, propertyValue);

			final List<Map<String, Object>> records = new ArrayList<>(0);
			while(itr.hasNext())
				records.add(itr.next().getAllProperties());
			return records;
		}
	}

	public static void delete(final String tableName, final String primaryPropertyName, final Object nodeID) throws StoreException{
		try(final Transaction tx = getTransaction()){
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
		try(final Transaction tx = getTransaction()){
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
			final Map<String, Object> properties = (record == null? new HashMap<>(1): new HashMap<>(record));
			addOnDeleteProperties(properties, onDeleteStart, onDeleteEnd);

			for(final Map.Entry<String, Object> property : properties.entrySet())
				relationship.setProperty(property.getKey(), property.getValue());
		}
	}

	private static void addOnDeleteProperties(final Map<String, Object> properties, final OnDeleteType onDeleteStart,
			final OnDeleteType onDeleteEnd){
		if(Objects.equals(onDeleteStart, onDeleteEnd))
			properties.put(PROPERTY_ON_DELETE, onDeleteStart.toString());
		else{
			properties.put(PROPERTY_ON_DELETE_START, onDeleteStart.toString());
			properties.put(PROPERTY_ON_DELETE_END, onDeleteEnd.toString());
		}
	}

	public static boolean deleteRelationship(final String tableNameStart, final String primaryPropertyNameStart, final Object nodeIDStart,
			final String tableNameEnd, final String primaryPropertyNameEnd, final Object nodeIDEnd,
			final String relationshipName, final String propertyName, final Object propertyValue){
		try(final Transaction tx = getTransaction()){
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
			if(propertyName != null){
				for(final Relationship relationship : relationships)
					if(relationship.getProperty(propertyName).equals(propertyValue) && relationship.getEndNode().equals(nodeEnd))
						relationship.delete();

			}
			else
				for(final Relationship relationship : relationships)
					if(relationship.getEndNode().equals(nodeEnd))
						relationship.delete();

			tx.commit();

			return hasRelationships;
		}
	}


	public static Map.Entry<String, Map<String, Object>> findOtherNode(final String tableNameStart, final String primaryPropertyNameStart,
			final Object nodeIDStart, final String relationshipName) throws StoreException{
		try(final Transaction tx = getTransaction()){
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
			String otherNodeLabels = null;
			for(final Relationship relationship : relationships){
				final Node endNode = relationship.getEndNode();
				otherRecord = endNode.getAllProperties();
				otherNodeLabels = concatenateLabels(endNode);
			}
			return new AbstractMap.SimpleImmutableEntry<>(otherNodeLabels, otherRecord);
		}
	}

	public static Map.Entry<String, Map<String, Object>> findOtherNode(final String tableNameStart, final String primaryPropertyNameStart,
		final Object nodeIDStart, final String relationshipName, final String propertyName, final Object propertyValue) throws StoreException{
		try(final Transaction tx = getTransaction()){
			final Node nodeStart = tx.findNode(Label.label(tableNameStart), primaryPropertyNameStart, nodeIDStart);
			if(nodeStart == null)
//				throw StoreException.create("Start node with {} {} not found in {}", primaryPropertyNameStart.toUpperCase(Locale.ROOT),
//					nodeIDStart, tableNameStart.toUpperCase(Locale.ROOT));
				return null;

			final ResourceIterable<Relationship> relationships = nodeStart.getRelationships(RelationshipType.withName(relationshipName));
			if(relationships.stream().count() > 1)
				throw StoreException.create("More than one node found");
			Map<String, Object> otherRecord = null;
			String otherNodeLabels = null;
			for(final Relationship relationship : relationships)
				if(relationship.getProperty(propertyName).equals(propertyValue)){
					final Node endNode = relationship.getEndNode();
					otherRecord = endNode.getAllProperties();
					otherNodeLabels = concatenateLabels(endNode);
				}
			return new AbstractMap.SimpleImmutableEntry<>(otherNodeLabels, otherRecord);
		}
	}

	public static List<Map<String, Object>> findStartNodes(final String tableNameStart){
		try(final Transaction tx = getTransaction()){
			final Result result = tx.execute(JavaHelper.textFormat(QUERY_ALL_CONNECTING_ANY_NODES, tableNameStart));

			final List<Map<String, Object>> otherNodes = new ArrayList<>();
			while(result.hasNext()){
				final Node node = (Node)result.next()
					.get(QUERY_PARAMETER_NODE);

				otherNodes.add(node.getAllProperties());
			}
			return otherNodes;
		}
	}

	public static List<Map<String, Object>> findStartNodes(final String tableNameStart,
			final String tableNameEnd, final String primaryPropertyNameEnd, final Object nodeIDEnd,
			final String relationshipName){
		try(final Transaction tx = getTransaction()){
			final Result result = tx.execute(JavaHelper.textFormat(QUERY_ALL_CONNECTING_NODES,
				tableNameStart,
				relationshipName,
				tableNameEnd, primaryPropertyNameEnd, nodeIDEnd));

			final List<Map<String, Object>> otherNodes = new ArrayList<>();
			while(result.hasNext()){
				final Node node = (Node)result.next()
					.get(QUERY_PARAMETER_NODE);

				otherNodes.add(node.getAllProperties());
			}
			return otherNodes;
		}
	}

	public static List<Map<String, Object>> findStartNodes(final String tableNameStart, final String tableNameEnd,
			final String primaryPropertyNameEnd, final Object nodeIDEnd, final String relationshipName, final String propertyName,
			final Object propertyValue){
		try(final Transaction tx = getTransaction()){
			final Result result = tx.execute(JavaHelper.textFormat(QUERY_ALL_CONNECTING_NODES_WITH_PROPERTY, tableNameStart,
				relationshipName, propertyName, propertyValue, tableNameEnd, primaryPropertyNameEnd, nodeIDEnd));

			final List<Map<String, Object>> otherNodes = new ArrayList<>();
			while(result.hasNext()){
				final Node node = (Node)result.next()
					.get(QUERY_PARAMETER_NODE);

				otherNodes.add(node.getAllProperties());
			}
			return otherNodes;
		}
	}


	public static String logDatabase(final String excludeLabel) throws JsonProcessingException{
		try(final Transaction tx = getTransaction()){
			final StringJoiner sj = new StringJoiner(LINE_SEPARATOR);

			Result result = tx.execute(excludeLabel == null
				? QUERY_ALL_NODES
				: JavaHelper.textFormat(QUERY_ALL_NODES_EXCLUDE_LABEL, excludeLabel));
			while(result.hasNext()){
				final Node node = (Node)result.next()
					.get(QUERY_PARAMETER_NODE);
				final String nodeID = node.getElementId();
				final String properties = OBJECT_MAPPER.writeValueAsString(node.getAllProperties());
				sj.add(nodeID + PRINT_SEPARATOR + concatenateLabels(node) + PRINT_SEPARATOR + properties);
			}

			result = tx.execute(QUERY_ALL_RELATIONSHIPS);
			while(result.hasNext()){
				final Relationship relationship = (Relationship)result.next().get(QUERY_PARAMETER_RELATIONSHIP);
				final String startNodeID = relationship.getStartNode().getElementId();
				final String endNodeID = relationship.getEndNode().getElementId();
				final String properties = OBJECT_MAPPER.writeValueAsString(relationship.getAllProperties());
				sj.add(startNodeID + PRINT_SEPARATOR + endNodeID + PRINT_SEPARATOR + relationship.getType() + PRINT_SEPARATOR + properties);
			}

			return sj.toString();
		}
	}

	private static String concatenateLabels(final Node node){
		final StringJoiner sj = new StringJoiner(PRINT_LABEL_SEPARATOR);
		for(final Label label : node.getLabels())
			sj.add(label.name());
		return sj.toString();
	}

}
