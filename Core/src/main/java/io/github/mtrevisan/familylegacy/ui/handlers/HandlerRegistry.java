/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.handlers;

import io.github.mtrevisan.familylegacy.io.grammar.XrefPathIndex;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.CaseUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Registry for RecordTypeHandler instances.
 * Maps record type names (e.g., "INDIVIDUAL") to their handlers.
 */
public final class HandlerRegistry{

	private static final Map<Class<? extends RecordTypeHandler<?>>, String> HANDLERS_TYPE = new ConcurrentHashMap<>();

	private static final Map<String, RecordTypeHandler<?>> HANDLERS = new HashMap<>();
	static{
		HANDLERS.put(AgentHandler.TYPE, AgentHandler.getInstance());
		HANDLERS.put(ConclusionHandler.TYPE, ConclusionHandler.getInstance());
		HANDLERS.put(ContactHandler.TYPE, ContactHandler.getInstance());
		HANDLERS.put(ContactNameHandler.TYPE, ContactNameHandler.getInstance());
		HANDLERS.put(ContextImpactHandler.TYPE, ContextImpactHandler.getInstance());
		HANDLERS.put(CulturalNormHandler.TYPE, CulturalNormHandler.getInstance());
		HANDLERS.put(MediaHandler.TYPE, MediaHandler.getInstance());
		HANDLERS.put(EventHandler.TYPE, EventHandler.getInstance());
		HANDLERS.put(EventParticipationHandler.TYPE, EventParticipationHandler.getInstance());
		HANDLERS.put(GroupAttributeHandler.TYPE, GroupAttributeHandler.getInstance());
		HANDLERS.put(GroupHandler.TYPE, GroupHandler.getInstance());
		HANDLERS.put(HeaderHandler.TYPE, HeaderHandler.getInstance());
		HANDLERS.put(HistoricEventHandler.TYPE, HistoricEventHandler.getInstance());
		HANDLERS.put(IdentityHypothesisHandler.TYPE, IdentityHypothesisHandler.getInstance());
		HANDLERS.put(IndividualAttributeHandler.TYPE, IndividualAttributeHandler.getInstance());
		HANDLERS.put(IndividualHandler.TYPE, IndividualHandler.getInstance());
		HANDLERS.put(NameHandler.TYPE, NameHandler.getInstance());
		HANDLERS.put(NoteHandler.TYPE, NoteHandler.getInstance());
		HANDLERS.put(PartHandler.TYPE, PartHandler.getInstance());
		HANDLERS.put(PersonalNameHandler.TYPE, PersonalNameHandler.getInstance());
		HANDLERS.put(PlaceCitationHandler.TYPE, PlaceCitationHandler.getInstance());
		HANDLERS.put(PlaceHandler.TYPE, PlaceHandler.getInstance());
		HANDLERS.put(PlaceRelationshipHandler.TYPE, PlaceRelationshipHandler.getInstance());
		HANDLERS.put(RelationshipHandler.TYPE, RelationshipHandler.getInstance());
		HANDLERS.put(RepositoryCitationHandler.TYPE, RepositoryCitationHandler.getInstance());
		HANDLERS.put(RepositoryHandler.TYPE, RepositoryHandler.getInstance());
		HANDLERS.put(ResearchActivityHandler.TYPE, ResearchActivityHandler.getInstance());
		HANDLERS.put(ResearchQuestionHandler.TYPE, ResearchQuestionHandler.getInstance());
		HANDLERS.put(ResearchTaskHandler.TYPE, ResearchTaskHandler.getInstance());
		HANDLERS.put(SourceCitationHandler.TYPE, SourceCitationHandler.getInstance());
		HANDLERS.put(SourceHandler.TYPE, SourceHandler.getInstance());
		HANDLERS.put(TextValueVariantHandler.TYPE, TextValueVariantHandler.getInstance());
	}


	private HandlerRegistry(){}


	/** @return the types of record that have a handler */
	public static Set<String> types(){
		return Set.copyOf(HANDLERS.keySet());
	}


	/**
	 * Returns the handler for the given record type.
	 * <p>
	 * Ensures the registry is initialized before accessing it.
	 *
	 * @param handlerClass	The handler class (e.g., {@code IndividualHandler.class}).
	 * @return	The handler, or {@code null} if not able to instantiate.
	 */
	public static RecordTypeHandler<?> getHandler(final Class<? extends RecordTypeHandler<?>> handlerClass){
		final String type = HANDLERS_TYPE.computeIfAbsent(handlerClass,
			k -> getHandlerType(handlerClass));
		return HANDLERS.get(type);
	}

	/**
	 * Returns the handler for the given record type.
	 * <p>
	 * Ensures the registry is initialized before accessing it.
	 *
	 * @param type	The record type.
	 * @return	The handler, or {@code null} if not able to return a class.
	 */
	public static RecordTypeHandler<?> getHandler(final String type){
		return HANDLERS.get(type.toUpperCase(Locale.ROOT));
	}


	public static String getHandlerType(final Class<? extends RecordTypeHandler<?>> handlerClass){
		try{
			final Field field = handlerClass.getDeclaredField("TYPE");
			field.setAccessible(true);
			return (String)field.get(null);
		}
		catch(final NoSuchFieldException | IllegalAccessException e){
			throw new IllegalArgumentException(
				"Handler class " + handlerClass.getName() + " does not define a static `TYPE` field",
				e);
		}
	}


	/**
	 * Finds all records in the model that reference the given target record ID.
	 * <p>
	 * This method uses the {@link XrefPathIndex} generated from the FLEF grammar to dynamically
	 * find every possible path that can contain a reference to the target record type.
	 * This avoids hardcoding every record type and specific path, making the code resilient to protocol changes.
	 *
	 * @param model the model
	 * @param targetId the ID of the record to search for
	 * @return a map grouping the referencing records by their handler class
	 */
	public static Map<Class<? extends RecordTypeHandler<?>>, Set<FLEFRecord>> getReferencingRecords(final FLEFModel model, final String targetId){
		final Map<Class<? extends RecordTypeHandler<?>>, Set<FLEFRecord>> result = new HashMap<>();
		if(targetId == null || !XrefPathIndex.isInitialized())
			return result;

		final FLEFRecord targetRecord = model.getRecordById(targetId);
		if(targetRecord == null)
			return result;

		final String targetRecordType = targetRecord.getTag();
		final String targetUnionType = CaseUtils.toCamelCase(targetRecordType, true, '_')
			+ XrefPathIndex.RECORD_UNION_NAME;
		final Set<String> xrefPaths = XrefPathIndex.getPaths(targetUnionType);
		if(xrefPaths.isEmpty())
			return result;

		for(final FLEFRecord record : model.getRecords()){
			final String recordType = record.getTag();
			for(final String path : xrefPaths){
				final int searchRecordTypeIndex = path.indexOf('.');
				if(searchRecordTypeIndex == -1)
					continue;

				final String searchRecordType = path.substring(0, searchRecordTypeIndex);
				if(!Strings.CI.equals(searchRecordType, recordType))
					continue;

				final String searchPath = path.substring(searchRecordTypeIndex + 1);
				final List<FLEFRecord> targetNodes = FLEFRecordHelper.findChildren(record, searchPath);
				for(final FLEFRecord node : targetNodes){
					if(targetId.equals(node.getValue())){
						final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(recordType);
						if(handler != null)
							// Use LinkedHashSet to avoid duplicates in O(1) while preserving insertion order
							result.computeIfAbsent(handler.getClassType(), k -> new LinkedHashSet<>())
								.add(record);

						// Found a match in this record, move to the next record
						break;
					}
				}
			}
		}
		return result;
	}

}
