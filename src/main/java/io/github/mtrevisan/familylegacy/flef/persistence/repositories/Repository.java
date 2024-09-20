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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


public class Repository{

	private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);

	public static final int SAVING_ERROR = -1;


	private Repository(){}


	public static boolean load(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		store.forEach((tableName, records) -> records.forEach((recordID, record) -> {
			//TODO
			save(tableName, record);
		}));

		return true;
	}

	public static int count(final String tableName){
		return GraphDatabaseManager.count(tableName);
	}


	public static int save(final String tableName, final Map<String, Object> record){
		try{
			final int nextID = GraphDatabaseManager.count(tableName) + 1;
			if(((Number)record.get(EntityManager.PROPERTY_NAME_PRIMARY_KEY)).intValue() != nextID)
				System.out.println();
			record.put(EntityManager.PROPERTY_NAME_PRIMARY_KEY, nextID);
			GraphDatabaseManager.insert(tableName, record);

			return nextID;
		}
		catch(final Exception e){
			LOGGER.error("Error while inserting record: {}", e.getMessage(), e);

			return SAVING_ERROR;
		}
	}

	public static boolean update(final String tableName, final Map<String, Object> record){
		try{
			GraphDatabaseManager.update(tableName, EntityManager.PROPERTY_NAME_PRIMARY_KEY, record);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while updating record: {}", e.getMessage(), e);

			return false;
		}
	}

	public static Map<String, Object> findByID(final String tableName, final Integer recordID){
		try{
			return GraphDatabaseManager.findBy(tableName, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordID);
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
			GraphDatabaseManager.delete(tableName, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordID);

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

				GraphDatabaseManager.delete(tableName, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordID);
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
			GraphDatabaseManager.upsertRelationship(tableNameStart, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDEnd,
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
			GraphDatabaseManager.upsertRelationship(tableNameStart, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDEnd,
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
			GraphDatabaseManager.deleteRelationship(tableNameStart, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDEnd, null);

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
			GraphDatabaseManager.deleteRelationship(tableNameStart, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDStart,
				tableNameEnd, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDEnd,
				relationshipName);

			return true;
		}
		catch(final Exception e){
			LOGGER.error("Error while deleting relationship: {}", e.getMessage(), e);

			return false;
		}
	}


	public static Map<String, Object> findReferencedRecord(final String tableNameStart, final Integer recordIDStart,
			final String relationshipName){
		try{
			return GraphDatabaseManager.findOtherRecord(tableNameStart, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDStart, relationshipName);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return null;
		}
	}

	public static Map<String, Object> findReferencedRecord(final String tableNameStart, final Integer recordIDStart,
			final String relationshipName, final String propertyName, final Object propertyValue){
		try{
			return GraphDatabaseManager.findOtherRecord(tableNameStart, EntityManager.PROPERTY_NAME_PRIMARY_KEY, recordIDStart, relationshipName,
				propertyName, propertyValue);
		}
		catch(final Exception e){
			LOGGER.error("Error while searching other node in a relationship: {}", e.getMessage(), e);

			return null;
		}
	}


	public static String logDatabase(){
		return GraphDatabaseManager.logDatabase();
	}

}
