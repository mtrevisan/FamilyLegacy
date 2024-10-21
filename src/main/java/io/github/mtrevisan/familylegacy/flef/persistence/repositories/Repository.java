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
package io.github.mtrevisan.familylegacy.flef.persistence.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;


public class Repository{

	private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);

	public static final int SAVING_ERROR = -1;


	private Repository(){}


	public static byte[] store(){
		try{
			return GraphDatabaseManager.store(null);
		}
		catch(final Exception e){
			LOGGER.error("Error while storing database: {}", e.getMessage(), e);

			return null;
		}
	}

	public static byte[] storeGenealogyDataOnly(){
		try{
			return GraphDatabaseManager.store(GraphDatabaseManager.LABEL_APPLICATION);
		}
		catch(final Exception e){
			LOGGER.error("Error while storing database: {}", e.getMessage(), e);

			return null;
		}
	}

	public static boolean restore(final byte[] database){
		try{
			GraphDatabaseManager.restore(database);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while restoring database: {}", e.getMessage(), e);

			return false;
		}
	}

	public static int count(final String tableName){
		return GraphDatabaseManager.count(tableName);
	}


	public static int upsert(final Map<String, Object> record, final String... tableNames){
		try{
			return (int)GraphDatabaseManager.upsert(EntityManager.PROPERTY_PRIMARY_KEY, record, tableNames);
		}
		catch(final Exception e){
			LOGGER.error("Error while upserting record: {}", e.getMessage(), e);

			return SAVING_ERROR;
		}
	}

	public static Map<String, Object> findByID(final String tableName, final Integer recordID){
		try{
			return GraphDatabaseManager.findBy(tableName, EntityManager.PROPERTY_PRIMARY_KEY, recordID);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching record: {}", e.getMessage(), e);

			return null;
		}
	}

	public static List<Map<String, Object>> findAll(final String tableName){
		try{
			return GraphDatabaseManager.findAll(tableName);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching all records: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}

	public static NavigableMap<Integer, Map<String, Object>> findAllNavigable(final String tableName){
		try{
			return GraphDatabaseManager.findAllNavigable(tableName);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching all records: {}", e.getMessage(), e);

			return Collections.emptyNavigableMap();
		}
	}

	public static List<Map<String, Object>> findAllBy(final String tableName, final String propertyName, final Object propertyValue){
		try{
			return GraphDatabaseManager.findAllBy(tableName, propertyName, propertyValue);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching all records: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}

	public static boolean deleteNode(final String tableName, final int recordID){
		try{
			GraphDatabaseManager.delete(tableName, EntityManager.PROPERTY_PRIMARY_KEY, recordID);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while deleting record: {}", e.getMessage(), e);

			return false;
		}
	}

	public static boolean deleteNodes(final String tableName, final List<Integer> recordIDs){
		try{
			for(int i = 0, length = recordIDs.size(); i < length; i ++){
				final Object recordID = recordIDs.get(i);

				GraphDatabaseManager.delete(tableName, EntityManager.PROPERTY_PRIMARY_KEY, recordID);
			}

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while deleting record: {}", e.getMessage(), e);

			return false;
		}
	}


	public static boolean upsertRelationship(final String tableNameStart, final Integer recordIDStart,
			final String tableNameEnd, final Integer recordIDEnd,
			final String relationshipName, final Map<String, Object> record,
			final GraphDatabaseManager.OnDeleteType onDelete){
		try{
			GraphDatabaseManager.upsertRelationship(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				relationshipName, record, onDelete, onDelete);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while upserting relationship: {}", e.getMessage(), e);

			return false;
		}
	}

	public static boolean upsertRelationship(final String tableNameStart, final Integer recordIDStart,
			final String tableNameEnd, final Integer recordIDEnd,
			final String relationshipName, final Map<String, Object> record,
			final GraphDatabaseManager.OnDeleteType onDeleteStart, final GraphDatabaseManager.OnDeleteType onDeleteEnd){
		try{
			GraphDatabaseManager.upsertRelationship(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				relationshipName, record, onDeleteStart, onDeleteEnd);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while upserting relationship: {}", e.getMessage(), e);

			return false;
		}
	}

	public static boolean deleteRelationship(final String tableNameStart, final Integer recordIDStart,
			final String tableNameEnd, final Integer recordIDEnd){
		try{
			GraphDatabaseManager.deleteRelationship(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				null, null, null);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while deleting relationship: {}", e.getMessage(), e);

			return false;
		}
	}

	public static boolean deleteRelationship(final String tableNameStart, final Integer recordIDStart,
			final String tableNameEnd, final Integer recordIDEnd,
			final String relationshipName){
		try{
			GraphDatabaseManager.deleteRelationship(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				relationshipName, null, null);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while deleting relationship: {}", e.getMessage(), e);

			return false;
		}
	}

	public static boolean deleteRelationship(final String tableNameStart, final Integer recordIDStart,
			final String tableNameEnd, final Integer recordIDEnd,
			final String relationshipName, final String propertyName, final Object propertyValue){
		try{
			GraphDatabaseManager.deleteRelationship(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				relationshipName, propertyName, propertyValue);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while deleting relationship: {}", e.getMessage(), e);

			return false;
		}
	}


	public static List<Map<String, Object>> findRelationships(final String tableNameStart, final Object nodeIDStart,
			final String tableNameEnd, final Object nodeIDEnd,
			final String relationshipName){
		try{
			return GraphDatabaseManager.findRelationships(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, nodeIDStart,
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, nodeIDEnd,
				relationshipName);
		}
		catch(final Exception e){
			LOGGER.error("Error while retrieving relationships: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}


	/**
	 * Finds the referenced node in the graph database based on the given parameters, that is the node reachable from the node with label
	 * `tableNameStart` and ID `recordIDStart` through the relationship `relationshipName`.
	 *
	 * @param tableNameStart	The starting table name in the relationship.
	 * @param recordIDStart	The record ID of the starting node.
	 * @param relationshipName	The name of the relationship.
	 * @return	The referenced node as a {@link Map.Entry} object, where the key is the node label and the value is a map representing the
	 * 	node properties. Returns {@code null} if no referenced node is found.
	 */
	public static Map.Entry<String, Map<String, Object>> findReferencedNode(final String tableNameStart, final Integer recordIDStart,
			final String relationshipName){
		try{
			return GraphDatabaseManager.findOtherNode(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, recordIDStart,
				relationshipName);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return null;
		}
	}

	/**
	 * Finds the referenced node in the graph database based on the given parameters, that is the node reachable from the node with label
	 * `tableNameStart` and ID `recordIDStart` through the relationship `relationshipName` that has a property
	 * `propertyName` with value `propertyValue`.
	 *
	 * @param tableNameStart	The starting table name in the relationship.
	 * @param recordIDStart	The record ID of the starting node.
	 * @param relationshipName	The name of the relationship.
	 * @param propertyName	The property name of the relationship.
	 * @param propertyValue	The property value of the relationship.
	 * @return	The referenced node as a {@link Map.Entry} object, where the key is the node label and the value is a map representing the
	 * 	node properties. Returns {@code null} if no referenced node is found.
	 */
	public static Map.Entry<String, Map<String, Object>> findReferencedNode(final String tableNameStart, final Integer recordIDStart,
			final String relationshipName, final String propertyName, final Object propertyValue){
		try{
			return GraphDatabaseManager.findOtherNode(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, recordIDStart,
				relationshipName, propertyName, propertyValue);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return null;
		}
	}

	public static List<Map.Entry<String, Map<String, Object>>> findReferencedNodes(final String tableNameStart, final Object nodeIDStart,
			final String relationshipName){
		try{
			return GraphDatabaseManager.findOtherNodes(tableNameStart, EntityManager.PROPERTY_PRIMARY_KEY, nodeIDStart,
				relationshipName);
		}
		catch(final Exception e){
			LOGGER.error("Error while retrieving relationships: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}

	public static List<Map<String, Object>> findReferencedNodes(final String tableNameStart,
			final String tableNameEnd, final Object nodeIDEnd,
			final String relationshipName){
		return findReferencedNodes(tableNameEnd, nodeIDEnd, relationshipName).stream()
			.filter(entry -> entry.getKey().equals(tableNameStart))
			.map(Map.Entry::getValue)
			.toList();
	}

	public static Map.Entry<String, Map<String, Object>> findReferencingNode(final String tableNameEnd, final Integer recordIDEnd,
			final String relationshipName){
		try{
			final List<Map.Entry<String, Map<String, Object>>> startNodes = GraphDatabaseManager.findStartNodes(
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				relationshipName);
			return (startNodes.isEmpty()? null: startNodes.getFirst());
		}
		catch(final Exception e){
			LOGGER.error("Error while searching start node in a relationship: {}", e.getMessage(), e);

			return null;
		}
	}


	/**
	 * Finds all the referencing nodes in the graph database, that is all the nodes reachable from node with label `tableNameStart`.
	 *
	 * @param tableNameStart	The starting table name in the relationship.
	 * @return	A list of maps representing the referencing nodes.
	 */
	public static List<Map<String, Object>> findReferencingNodes(final String tableNameStart){
		try{
			return GraphDatabaseManager.findStartNodes(tableNameStart);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}

	/**
	 * Finds the referencing nodes in the graph database based on the given parameters, that is all the nodes with label `tableNameStart`
	 * that points to node with label `tableNameEnd` and ID `recordIDEnd` through relationship `relationshipName`.
	 *
	 * @param tableNameStart	The starting table name in the relationship.
	 * @param tableNameEnd	The ending table name in the relationship.
	 * @param recordIDEnd	The record ID of the ending node.
	 * @param relationshipName	The name of the relationship.
	 * @return	A list of maps representing the referencing nodes.
	 */
	public static List<Map<String, Object>> findReferencingNodes(final String tableNameStart,
			final String tableNameEnd, final Integer recordIDEnd,
			final String relationshipName){
		try{
			return GraphDatabaseManager.findStartNodes(tableNameStart,
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				relationshipName);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}

	/**
	 * Finds the referencing nodes in the graph database based on the given parameters, that is all the nodes with label `tableNameStart`
	 * that points to node with label `tableNameEnd` and ID `recordIDEnd` through relationship `relationshipName` that has a property
	 * `propertyName` with value `propertyValue`.
	 *
	 * @param tableNameStart	The starting table name in the relationship.
	 * @param tableNameEnd	The ending table name in the relationship.
	 * @param recordIDEnd	The record ID of the ending node.
	 * @param relationshipName	The name of the relationship.
	 * @param propertyName	The property name of the relationship.
	 * @param propertyValue	The property value of the relationship.
	 * @return	A list of maps representing the referencing nodes.
	 */
	public static List<Map<String, Object>> findReferencingNodes(final String tableNameStart,
			final String tableNameEnd, final Integer recordIDEnd,
			final String relationshipName, final String propertyName, final Object propertyValue){
		try{
			return GraphDatabaseManager.findStartNodes(tableNameStart, tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd,
				relationshipName, propertyName, propertyValue);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}


	/**
	 * Checks whether a given record in a specified table has notes referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has notes.
	 */
	public static boolean hasNotes(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_NOTE,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has localized extracts referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has notes.
	 */
	public static boolean hasTranscribedExtracts(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_LOCALIZED_TEXT,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR, EntityManager.PROPERTY_TYPE, "extract");
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has media referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has media.
	 */
	public static boolean hasMedia(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_MEDIA,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has cultural norms referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has cultural norms.
	 */
	public static boolean hasCulturalNorms(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_CULTURAL_NORM,
			tableName, recordID,
			EntityManager.RELATIONSHIP_SUPPORTED_BY);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has assertions referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has assertions.
	 */
	public static boolean hasAssertions(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = (tableName.equals(EntityManager.NODE_CITATION)
			? findReferencingNodes(EntityManager.NODE_ASSERTION, tableName, recordID, EntityManager.RELATIONSHIP_INFERRED_FROM)
			: findReferencedNodes(EntityManager.NODE_ASSERTION, tableName, recordID, EntityManager.RELATIONSHIP_SUPPORTED_BY));
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has events referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has events.
	 */
	public static boolean hasEvents(final String tableName, final Integer recordID){
		final String relationshipName = switch(tableName){
			case EntityManager.NODE_EVENT_TYPE -> EntityManager.RELATIONSHIP_OF_TYPE;
			case EntityManager.NODE_PLACE -> EntityManager.RELATIONSHIP_HAPPENED_IN;
			case EntityManager.NODE_HISTORIC_DATE -> EntityManager.RELATIONSHIP_HAPPENED_ON;
			default -> EntityManager.RELATIONSHIP_FOR;
		};
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_EVENT,
			tableName, recordID,
			relationshipName);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has groups referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has groups.
	 */
	public static boolean hasGroups(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_GROUP,
			tableName, recordID,
			EntityManager.RELATIONSHIP_OF);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has sources referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has sources.
	 */
	public static boolean hasSources(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_SOURCE,
			tableName, recordID,
			EntityManager.RELATIONSHIP_STORED_IN);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has citations referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has citations.
	 */
	public static boolean hasCitations(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_CITATION,
			tableName, recordID,
			EntityManager.RELATIONSHIP_QUOTES);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has person names referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has person names.
	 */
	public static boolean hasPersonNames(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_PERSON_NAME,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has person name transliterations referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record has person name transliterations.
	 */
	public static boolean hasPersonNameTransliterations(final String tableName, final Integer recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_LOCALIZED_PERSON_NAME,
			tableName, recordID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has transcriptions referenced to it.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @param transcriptionType	The type of the trascription ('name' or 'extract').
	 * @return	Whether the record has transcriptions.
	 */
	public static boolean hasTranscriptions(final String tableName, final Integer recordID, final String transcriptionType){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_LOCALIZED_TEXT,
			tableName, recordID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, EntityManager.PROPERTY_TYPE, transcriptionType);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has a relationship with the given name.
	 *
	 * @param tableNameStart	The name of the starting table.
	 * @param recordIDStart	The ID of the starting record.
	 * @param tableNameEnd	The name of the end table.
	 * @param relationshipName	The name of the relationship.
	 * @return	Whether the record has transcriptions.
	 */
	public static boolean hasReference(final String tableNameStart, final Integer recordIDStart,
			final String tableNameEnd,
			final String relationshipName){
		final List<Map<String, Object>> result = Repository.findReferencingNodes(tableNameEnd,
			tableNameStart, recordIDStart,
			relationshipName);
		return !result.isEmpty();
	}

	/**
	 * Checks whether a given record in a specified table has a relationship with the given name.
	 *
	 * @param tableNameStart	The name of the starting table.
	 * @param recordIDStart	The ID of the starting record.
	 * @param tableNameEnd	The name of the end table.
	 * @param relationshipName	The name of the relationship.
	 * @return	Whether the record has transcriptions.
	 */
	public static boolean hasReference(final String tableNameStart, final Integer recordIDStart,
			final String tableNameEnd,
			final String relationshipName, final String propertyName, final Object propertyValue){
		final List<Map<String, Object>> result = Repository.findReferencingNodes(tableNameEnd,
			tableNameStart, recordIDStart,
			relationshipName, propertyName, propertyValue);
		return !result.isEmpty();
	}


	/**
	 * Checks whether a given record in a specified table has a reference to an owner (as a person).
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record references a place.
	 */
	public static boolean hasOwner(final String tableName, final Integer recordID){
		return (findReferencedNode(tableName, recordID,
			EntityManager.RELATIONSHIP_OWNED_BY) != null);
	}

	/**
	 * Checks whether a given record in a specified table has a reference to a starting date.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record references a starting date.
	 */
	public static boolean hasDateStart(final String tableName, final Integer recordID){
		return (findReferencedNode(tableName, recordID,
			EntityManager.RELATIONSHIP_STARTED_ON) != null);
	}

	/**
	 * Checks whether a given record in a specified table has a reference to an ending date.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record references an ending date.
	 */
	public static boolean hasDateEnd(final String tableName, final Integer recordID){
		return (findReferencedNode(tableName, recordID,
			EntityManager.RELATIONSHIP_ENDED_ON) != null);
	}

	/**
	 * Checks whether a given record in a specified table has a reference to a place.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record references a place.
	 */
	public static boolean hasPlace(final String tableName, final Integer recordID){
		final String relationshipName = switch(tableName){
			case EntityManager.NODE_REPOSITORY -> EntityManager.RELATIONSHIP_LOCATED_IN;
			case EntityManager.NODE_SOURCE -> EntityManager.RELATIONSHIP_CREATED_IN;
			case EntityManager.NODE_CULTURAL_NORM -> EntityManager.RELATIONSHIP_APPLIES_IN;
			case EntityManager.NODE_EVENT -> EntityManager.RELATIONSHIP_HAPPENED_IN;
			default -> null;
		};
		final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(
			tableName, recordID,
			relationshipName);
		return (placeNode != null);
	}

	/**
	 * Checks whether a given record in a specified table has a reference to a date.
	 *
	 * @param tableName	The name of the table.
	 * @param recordID	The ID of the record.
	 * @return	Whether the record references a date.
	 */
	public static boolean hasDate(final String tableName, final Integer recordID){
		final String relationshipName = switch(tableName){
			case EntityManager.NODE_SOURCE, EntityManager.NODE_MEDIA -> EntityManager.RELATIONSHIP_CREATED_ON;
			case EntityManager.NODE_EVENT -> EntityManager.RELATIONSHIP_HAPPENED_ON;
			default -> null;
		};
		final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(
			tableName, recordID,
			relationshipName);
		return (placeNode != null);
	}

	public static Map<String, Object> getDepiction(final String tableName, final Integer recordID){
		final Map.Entry<String, Map<String, Object>> photoRecord = Repository.findReferencedNode(
			tableName, recordID,
			EntityManager.RELATIONSHIP_DEPICTED_BY);
		return (photoRecord != null? photoRecord.getValue(): null);
	}


	public static String getRestriction(final String tableName, final Integer recordID){
		final List<Map<String, Object>> restrictions = findReferencingNodes(EntityManager.NODE_RESTRICTION,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return (restrictions.isEmpty()
			? null
			: EntityManager.extractRecordRestriction(restrictions.getFirst()));
	}


	public static String logDatabase(){
		try{
			return GraphDatabaseManager.logDatabase(null);
		}
		catch(final JsonProcessingException jpe){
			LOGGER.error("Error while printing database", jpe);

			throw new RuntimeException(jpe);
		}
	}

	public static String logDatabaseGenealogyDataOnly(){
		try{
			return GraphDatabaseManager.logDatabase(GraphDatabaseManager.LABEL_APPLICATION);
		}
		catch(final JsonProcessingException jpe){
			LOGGER.error("Error while printing database", jpe);

			throw new RuntimeException(jpe);
		}
	}

}
