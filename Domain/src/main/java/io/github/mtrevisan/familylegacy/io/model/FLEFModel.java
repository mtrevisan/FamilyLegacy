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

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.Modifications;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import io.github.mtrevisan.familylegacy.io.model.record.TrackedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


/**
 * FLEF data model.
 * Contains the header and a list of top-level records.
 */
public class FLEFModel implements RecordLookup{

	public static final String FLEF_LINK_PREFIX = "flef:";


	private FLEFRecord header;
	private final TrackedList<FLEFRecord> records = new TrackedList<>(10);

	private final Map<String, List<FLEFRecord>> recordsByType = new HashMap<>();
	private final Map<String, FLEFRecord> recordsById = new HashMap<>();

	private final Map<String, FLEFRecord> placesById = new HashMap<>();
	private final Map<String, FLEFRecord> eventsById = new HashMap<>();
	private final Map<String, FLEFRecord> questionsById = new HashMap<>();
	private final Set<String> duplicateIds = new LinkedHashSet<>();

	/** Indexes derived from the records (see {@link #derived(Object, Supplier)}), dropped at every change of the model. */
	private final Map<Object, Object> derivedIndexes = new ConcurrentHashMap<>();

	/** The free tags of the records: of the application, not of the file (see {@link RecordTags}); shared by the read-only views. */
	private RecordTags tags = new RecordTags();


	/**
	 * An index (or any value) computed from the records of the model, kept until the model changes: it saves the scan of the whole model
	 * to each lookup of a single record. The records are also edited in place, which the model cannot see, so whoever finds such a change
	 * (as {@link ModelHistory} does at every checkpoint) must call {@link #invalidateDerived()}.
	 *
	 * @param key	Identifies the index (a private constant of the class that builds it).
	 * @param builder	Computes the value from the model; it may call back the model but not {@code derived} with the same key.
	 * @return	The value, built if it was not there.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T derived(final Object key, final Supplier<T> builder){
		Object value = derivedIndexes.get(key);
		if(value == null){
			value = builder.get();
			derivedIndexes.put(key, value);
		}
		return (T)value;
	}

	/** Drops the indexes of {@link #derived(Object, Supplier)}: the records changed. */
	public void invalidateDerived(){
		derivedIndexes.clear();
	}


	/** @return	The free tags attached to the records of this model (they are not written to the file). */
	public RecordTags tags(){
		return tags;
	}

	/**
	 * NOTE: Used by {@link ModelHistory} only: a read-only view shows the tags of the model it is a copy of.
	 */
	void shareTags(final RecordTags shared){
		tags = shared;
	}


	public FLEFRecord getHeader(){
		if(header == null)
			header = FLEFRecord.createEmpty();
		return header;
	}

	/**
	 * NOTE: Used by {@link io.github.mtrevisan.familylegacy.io.FLEFParser#parse(String)} and {@link ModelHistory#readOnlyView()} only.
	 */
	public void setHeader(final FLEFRecord header){
		Modifications.touch();
		this.header = header;
	}

	/**
	 * The ids that were found twice at the top level while parsing (the last record won).
	 */
	public Set<String> getDuplicateIds(){
		return Collections.unmodifiableSet(duplicateIds);
	}

	/**
	 * NOTE: Used by {@link io.github.mtrevisan.familylegacy.io.FLEFParser#parse(String)} only.
	 */
	public void markDuplicateId(final String id){
		if(id != null)
			duplicateIds.add(id);
	}

	/** @return	The value of {@link Modifications#current()} at the last time that a record was added, removed or moved (not modified). */
	public long structureStamp(){
		return records.lastChange();
	}

	public List<FLEFRecord> getRecords(){
		return records;
	}

	/**
	 * Adds the record, replacing the one with the same id (if any).
	 *
	 * @throws IllegalArgumentException	If the record is {@code null} or has no id.
	 */
	public void addRecord(final FLEFRecord record){
		if(record == null || record.getId() == null)
			throw new IllegalArgumentException("A record must have an id to be added to the model");

		invalidateDerived();
		final String id = record.getId();
		if(recordsById.containsKey(id))
			// Optionally remove existing record to allow replacement/update
			removeRecord(id);

		recordsById.put(id, record);
		if(id.startsWith(PlaceReader.ID_PREFIX))
			placesById.put(id, record);
		if(id.startsWith(EventReader.ID_PREFIX))
			eventsById.put(id, record);
		if(id.startsWith(ResearchQuestionReader.ID_PREFIX))
			questionsById.put(id, record);

		records.add(record);
		if(record.getTag() != null)
			recordsByType.computeIfAbsent(record.getTag(), k -> new ArrayList<>())
				.add(record);
	}

	/**
	 * Puts the given records in the given order, in the places they have in the model, and leaves the other records where they are: it is
	 * how the order of a list of an editor that shows records of the model (the attributes of an individual, the relationships of a group)
	 * is kept, since the order of the records is the one of the file. Nothing happens if a record is not one of the model, or is given twice.
	 *
	 * @param ordered	The records, in the new order.
	 */
	public void reorderRecords(final List<FLEFRecord> ordered){
		if(ordered == null || ordered.size() < 2)
			return;

		for(final FLEFRecord record : ordered)
			if(record == null || record.getId() == null || recordsById.get(record.getId()) != record)
				return;
		final Set<FLEFRecord> subset = identitySet(ordered);
		if(subset.size() != ordered.size())
			return;

		boolean changed = placeInOrder(records, ordered, subset);
		//(the records of a type are in the model in the same relative order as in the whole)
		final Map<String, List<FLEFRecord>> byType = new HashMap<>();
		for(final FLEFRecord record : ordered)
			byType.computeIfAbsent(record.getTag(), k -> new ArrayList<>())
				.add(record);
		for(final Map.Entry<String, List<FLEFRecord>> entry : byType.entrySet()){
			final List<FLEFRecord> sameType = recordsByType.get(entry.getKey());
			if(sameType != null)
				changed |= placeInOrder(sameType, entry.getValue(), identitySet(entry.getValue()));
		}
		if(changed)
			invalidateDerived();
	}

	private static Set<FLEFRecord> identitySet(final List<FLEFRecord> records){
		final Set<FLEFRecord> set = Collections.newSetFromMap(new IdentityHashMap<>());
		set.addAll(records);
		return set;
	}

	/** Puts the records of the subset, in the order given, in the places that the subset has in the list. */
	private static boolean placeInOrder(final List<FLEFRecord> target, final List<FLEFRecord> ordered, final Set<FLEFRecord> subset){
		boolean changed = false;
		int next = 0;
		for(int i = 0, size = target.size(); i < size && next < ordered.size(); i ++)
			if(subset.contains(target.get(i))){
				final FLEFRecord record = ordered.get(next ++);
				if(target.get(i) != record){
					target.set(i, record);
					changed = true;
				}
			}
		return changed;
	}

	/**
	 * Replaces all the records (and their indices) with the given ones.
	 * <p>
	 * NOTE: Used by {@link ModelHistory} only.
	 */
	void replaceRecords(final List<FLEFRecord> newRecords){
		invalidateDerived();
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

	@Override
	public List<FLEFRecord> getRecordsByType(final String type){
		return recordsByType.getOrDefault(type.toLowerCase(Locale.ROOT), List.of());
	}

	@Override
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

		invalidateDerived();
		//the identifier may be given to another record: it does not inherit the tags
		tags.clear(id);
		final FLEFRecord record = recordsById.remove(id);
		placesById.remove(id);
		eventsById.remove(id);
		questionsById.remove(id);

		if(record != null){
			records.removeIf(r -> r == record);

			final String type = record.getTag();
			final List<FLEFRecord> list = recordsByType.get(type);
			if(list != null){
				list.removeIf(r -> r == record);
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

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final FLEFModel other = (FLEFModel)obj;
		if(!Objects.equals(getHeader(), other.getHeader()))
			return false;

		//the same records in the same order (e.g. a copy) are equal without building the sets
		final int size = records.size();
		if(size == other.records.size()){
			boolean sameOrder = true;
			for(int i = 0; sameOrder && i < size; i ++)
				sameOrder = records.get(i).equals(other.records.get(i));
			if(sameOrder)
				return true;
		}

		return Objects.equals(new HashSet<>(records), new HashSet<>(other.records));
	}

	@Override
	public int hashCode(){
		//the same value of Objects.hash(getHeader()), without the array
		int hash = 31 + Objects.hashCode(getHeader());
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
