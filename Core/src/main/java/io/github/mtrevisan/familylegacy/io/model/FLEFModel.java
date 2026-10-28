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
package io.github.mtrevisan.familylegacy.io.model;

import io.github.mtrevisan.familylegacy.io.grammar.XrefPathIndex;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.CaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * FLEF data model.
 * Contains the header and a list of top-level records.
 */
public class FLEFModel{

	private static final Logger LOGGER = LoggerFactory.getLogger(FLEFModel.class);


	public static final String FLEF_LINK_PREFIX = "flef:";
	private static final String TAG_NOTE_TRANSLATION_TEXT = "translation.text";


	private FLEFRecord header;
	private final List<FLEFRecord> records = new ArrayList<>();

	private final Map<String, List<FLEFRecord>> recordsByType = new HashMap<>();
	private final Map<String, FLEFRecord> recordsById = new HashMap<>();

	private final Map<String, FLEFRecord> placesById = new HashMap<>();
	private final Map<String, FLEFRecord> eventsById = new HashMap<>();
	private final Map<String, FLEFRecord> questionsById = new HashMap<>();


	public FLEFRecord getHeader(){
		if(header == null)
			header = FLEFRecord.createEmpty();
		return header;
	}

	/**
	 * NOTE: Used by {@link io.github.mtrevisan.familylegacy.io.FLEFParser#parse(String)} only.
	 */
	public void setHeader(final FLEFRecord header){
		this.header = header;
	}

	public List<FLEFRecord> getRecords(){
		return records;
	}

	public void addRecord(final FLEFRecord record){
		final String id = record.getId();
		if(recordsById.containsKey(id))
			// Optionally remove existing record to allow replacement/update
			removeRecord(id);

		recordsById.put(id, record);
		if(id.startsWith(PlaceHandler.ID_PREFIX))
			placesById.put(id, record);
		if(id.startsWith(EventHandler.ID_PREFIX))
			eventsById.put(id, record);
		if(id.startsWith(ResearchQuestionHandler.ID_PREFIX))
			questionsById.put(id, record);

		records.add(record);
		if(record.getTag() != null)
			recordsByType.computeIfAbsent(record.getTag(), k -> new ArrayList<>())
				.add(record);
	}

	/**
	 * Replaces all the records (and their indices) with the given ones.
	 * <p>
	 * NOTE: Used by {@link ModelHistory} only.
	 */
	void replaceRecords(final List<FLEFRecord> newRecords){
		records.clear();
		recordsByType.clear();
		recordsById.clear();
		placesById.clear();
		eventsById.clear();
		questionsById.clear();

		for(final FLEFRecord record : newRecords)
			addRecord(record);
	}

	public boolean hasRecordsByType(final String type){
		return !getRecordsByType(type).isEmpty();
	}

	public List<FLEFRecord> getRecordsByType(final String type){
		return recordsByType.getOrDefault(type.toLowerCase(Locale.ROOT), List.of());
	}

	public FLEFRecord getRecordById(final String id){
		if(id == null)
			return null;

		return recordsById.get(id);
	}

	public Map<String, FLEFRecord> getPlacesById(){
		return placesById;
	}

	public Map<String, FLEFRecord> getEventsById(){
		return eventsById;
	}

	public Map<String, FLEFRecord> getQuestionsById(){
		return questionsById;
	}

	public boolean isEmpty(){
		return records.isEmpty();
	}

	public int getRecordCount(){
		return records.size();
	}

	public FLEFRecord removeRecord(final String id){
		if(id == null)
			return null;

		final FLEFRecord record = recordsById.remove(id);
		placesById.remove(id);
		eventsById.remove(id);
		questionsById.remove(id);

		if(record != null){
			records.remove(record);

			final String type = record.getTag();
			final List<FLEFRecord> list = recordsByType.get(type);
			if(list != null){
				list.remove(record);
				if(list.isEmpty())
					recordsByType.remove(type);
			}
		}

		return record;
	}

	public boolean hasRecord(final String id){
		if(id == null)
			return false;

		return recordsById.containsKey(id);
	}

	public Set<String> getRecordTypes(){
		return recordsByType.keySet();
	}

	/**
	 * Finds all records in the model that reference the given target record ID.
	 * <p>
	 * This method uses the {@link XrefPathIndex} generated from the FLEF grammar to dynamically
	 * find every possible path that can contain a reference to the target record type.
	 * This avoids hardcoding every record type and specific path, making the code resilient to protocol changes.
	 *
	 * @param targetId the ID of the record to search for
	 * @return a map grouping the referencing records by their handler class
	 */
	public Map<Class<? extends RecordTypeHandler<?>>, Set<FLEFRecord>> getReferencingRecords(final String targetId){
		final Map<Class<? extends RecordTypeHandler<?>>, Set<FLEFRecord>> result = new HashMap<>();
		if(targetId == null || !XrefPathIndex.isInitialized())
			return result;

		final FLEFRecord targetRecord = getRecordById(targetId);
		if(targetRecord == null)
			return result;

		final String targetRecordType = targetRecord.getTag();
		final String targetUnionType = CaseUtils.toCamelCase(targetRecordType, true, '_')
			+ XrefPathIndex.RECORD_UNION_NAME;
		final Set<String> xrefPaths = XrefPathIndex.getPaths(targetUnionType);
		if(xrefPaths.isEmpty())
			return result;

		for(final FLEFRecord record : records){
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

	/**
	 * Redirects to {@code newId} every reference to {@code oldId}, strictly across pre-indexed Xref paths derived from the grammar,
	 * together with the {@code flef:} links in notes, then removes the record {@code oldId}.
	 * Highly optimized to avoid global record lookups per path iteration.
	 *
	 * @param oldId the ID to replace
	 * @param newId the target ID, of an existing record
	 * @throws IllegalArgumentException if {@code newId} is not a record of this model
	 */
	public void replaceRecordId(final String oldId, final String newId){
		if(!XrefPathIndex.isInitialized())
			throw new IllegalStateException("Xref paths index is not initialized");

		if(StringUtils.isEmpty(oldId) || oldId.equals(newId))
			return;

		final FLEFRecord newRecord = getRecordById(newId);
		if(newRecord == null)
			throw new IllegalArgumentException("Record does not exist: " + newId);
		final String newRecordType = newRecord.getTag();
		final String newTargetRecordType = CaseUtils.toCamelCase(newRecordType, true, '_')
			+ XrefPathIndex.RECORD_UNION_NAME;
		final Set<String> xrefPaths = XrefPathIndex.getPaths(newTargetRecordType);
		if(xrefPaths.isEmpty())
			return;

		// Iterate once through all records in the model to update structural Xref fields
		for(final FLEFRecord record : records){
			final String recordType = record.getTag();
			for(final String path : xrefPaths){
				final int searchRecordTypeIndex = path.indexOf('.');
				final String searchRecordType = path.substring(0, searchRecordTypeIndex);
				if(!Strings.CI.equals(searchRecordType, recordType))
					continue;

				final String searchPath = path.substring(searchRecordTypeIndex + 1);
				final List<FLEFRecord> targetNodes = FLEFRecordHelper.findChildren(record, searchPath);
				for(final FLEFRecord node : targetNodes)
					if(oldId.equals(node.getValue())){
						node.setValue(newId);

						LOGGER.info("Updated Xref ID from {} to {} in record {}, path {}", oldId, newId, record.getId(),
							searchPath);
					}
			}
		}

		// Construct the markdown link pattern for FLEF: URI references (e.g., flef:I12); the id ends at the first character that cannot
		// belong to an id, so that flef:I1 does not match flef:I12
		final Pattern flefLinkOld = Pattern.compile(Pattern.quote(FLEF_LINK_PREFIX + oldId) + "(?![A-Za-z0-9_-])");
		final String flefLinkNew = Matcher.quoteReplacement(FLEF_LINK_PREFIX + newId);
		// Iterate through all records to update flef: links inside NoteStructure text fields and translations
		for(final FLEFRecord record : records)
			updateNoteLinksRecursive(record, flefLinkOld, flefLinkNew);

		// The old record is now unreferenced: remove it from the records and from every index
		removeRecord(oldId);
	}

	/**
	 * Recursively searches for note nodes and updates markdown flef: URI references in their text and translations.
	 *
	 * @param record      the current FLEF record being inspected
	 * @param flefLinkOld the pattern of the old {@code flef:} link
	 * @param flefLinkNew the replacement {@code flef:} link
	 */
	private void updateNoteLinksRecursive(final FLEFRecord record, final Pattern flefLinkOld, final String flefLinkNew){
		if(record == null)
			return;

		// If the record is a note structure, check and replace within its text field and the text of its translations
		if(NoteHandler.isOfType(record)){
			final List<FLEFRecord> textNodes = new ArrayList<>(FLEFRecordHelper.findChildren(record, NoteReader.TAG_TEXT));
			textNodes.addAll(FLEFRecordHelper.findChildren(record, TAG_NOTE_TRANSLATION_TEXT));
			for(final FLEFRecord textNode : textNodes){
				final String text = textNode.getValue();
				if(text == null)
					continue;

				final String updatedText = flefLinkOld.matcher(text).replaceAll(flefLinkNew);
				if(!updatedText.equals(text)){
					textNode.setValue(updatedText);

					LOGGER.info("Updated markdown note link to {} in record {}", flefLinkNew, record.getId());
				}
			}
		}

		// Recursively inspect child nodes (including translations or nested structures)
		final List<FLEFRecord> children = record.getChildren();
		for(int i = 0, childrenSize = children.size(); i < childrenSize; i ++){
			final FLEFRecord child = children.get(i);

			updateNoteLinksRecursive(child, flefLinkOld, flefLinkNew);
		}
	}


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final FLEFModel other = (FLEFModel)obj;
		return (Objects.equals(header, other.header)
			&& Objects.equals(new HashSet<>(records), new HashSet<>(other.records)));
	}

	@Override
	public int hashCode(){
		int hash = Objects.hash(header);
		hash = 31 * hash + childrenHash(records);
		return hash;
	}

	private int childrenHash(final List<FLEFRecord> children){
		int result = 0;
		for(final FLEFRecord child : children)
			result ^= child.hashCode();
		return result;
	}

	@Override
	public String toString(){
		return "FLEFModel{"
			+ "header=" + (header != null? header.getTag(): "null")
			+ ", records=" + records.size()
			+ '}';
	}

}
