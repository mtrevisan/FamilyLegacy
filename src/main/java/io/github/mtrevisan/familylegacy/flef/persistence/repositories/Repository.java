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


	public static int save(final String tableName, final Map<String, Object> record){
		try{
			final int nextID = GraphDatabaseManager.count(tableName) + 1;
			if(((Number)record.get(EntityManager.PROPERTY_PRIMARY_KEY)).intValue() != nextID)
				System.out.println();
			record.put(EntityManager.PROPERTY_PRIMARY_KEY, nextID);
			GraphDatabaseManager.insert(record, tableName);

			return nextID;
		}
		catch(final Exception e){
			LOGGER.error("Error while inserting record: {}", e.getMessage(), e);

			return SAVING_ERROR;
		}
	}

	public static boolean update(final String tableName, final Map<String, Object> record){
		try{
			GraphDatabaseManager.update(tableName, EntityManager.PROPERTY_PRIMARY_KEY, record);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while updating record: {}", e.getMessage(), e);

			return false;
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
				tableNameEnd, EntityManager.PROPERTY_PRIMARY_KEY, recordIDEnd, null);

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
				relationshipName);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while deleting relationship: {}", e.getMessage(), e);

			return false;
		}
	}


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

	public static List<Map<String, Object>> findReferencingNodes(final String tableNameStart){
		try{
			return GraphDatabaseManager.findStartNodes(tableNameStart);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return Collections.emptyList();
		}
	}

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


	public static boolean hasNotes(final String tableName, final int recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_NOTE,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return !result.isEmpty();
	}

	public static boolean hasMedia(final String tableName, final int recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_MEDIA,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return !result.isEmpty();
	}

	public static boolean hasCulturalNorms(final String tableName, final int recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_CULTURAL_NORM,
			tableName, recordID,
			EntityManager.RELATIONSHIP_SUPPORTED_BY);
		return !result.isEmpty();
	}

	public static boolean hasAssertions(final String tableName, final int recordID){
		final String relationshipName = (tableName.equals(EntityManager.NODE_CITATION)
			? EntityManager.RELATIONSHIP_INFERRED_FROM
			: EntityManager.RELATIONSHIP_SUPPORTED_BY);
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_ASSERTION,
			tableName, recordID, relationshipName);
		return !result.isEmpty();
	}

	public static boolean hasEvents(final String tableName, final int recordID){
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

	public static boolean hasGroups(final String tableName, final int recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_GROUP,
			tableName, recordID,
			EntityManager.RELATIONSHIP_OF);
		return !result.isEmpty();
	}

	public static boolean hasSources(final String tableName, final int recordID){
		final String relationshipName = switch(tableName){
			case EntityManager.NODE_REPOSITORY -> EntityManager.RELATIONSHIP_STORED_IN;
			case EntityManager.NODE_PLACE -> EntityManager.RELATIONSHIP_CREATED_IN;
			case EntityManager.NODE_HISTORIC_DATE -> EntityManager.RELATIONSHIP_CREATED_ON;
			default -> null;
		};
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_SOURCE,
			tableName, recordID,
			relationshipName);
		return !result.isEmpty();
	}

	public static boolean hasCitations(final String tableName, final int recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_CITATION,
			tableName, recordID,
			EntityManager.RELATIONSHIP_QUOTES);
		return !result.isEmpty();
	}

	public static boolean hasPersonNames(final String tableName, final int recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_PERSON_NAME,
			tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return !result.isEmpty();
	}

	public static boolean hasPersonNameTransliterations(final String tableName, final int recordID){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_LOCALIZED_PERSON_NAME,
			tableName, recordID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR);
		return !result.isEmpty();
	}

	public static boolean hasTranscriptions(final String tableName, final int recordID, final String extractType){
		final List<Map<String, Object>> result = findReferencingNodes(EntityManager.NODE_LOCALIZED_TEXT,
			tableName, recordID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, EntityManager.PROPERTY_TYPE, extractType);
		return !result.isEmpty();
	}

	public static String getRestriction(final String tableName, final int recordID){
		final Map.Entry<String, Map<String, Object>> result = findReferencedNode(tableName, recordID,
			EntityManager.RELATIONSHIP_FOR);
		return (result != null
			? EntityManager.extractRecordRestriction(result.getValue())
			: null);
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
