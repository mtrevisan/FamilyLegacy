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

import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;


/**
 * The free tags that the researcher attaches to records: lightweight, user-defined classifications ("Brick Wall", "To Verify", ...) for
 * what is temporary, specific of a project or cross-cutting, and does not deserve a structured record.
 * <p>
 * The tags are <b>not part of the FLEF file</b> (the protocol does not know them): they belong to the application, which keeps them
 * for each file apart (see {@code TagStore}), as it does for the bookmarks. A model carries its tags ({@link FLEFModel#tags()}), so that
 * whoever has the model (search, reports, assistant, merge) can read them; the read-only views of the model share them.
 * <p>
 * A tag is free text: it is trimmed, its inner spaces are collapsed, and two tags that differ only by case are the same tag (the first
 * spelling written is the one kept for a record). There is no controlled vocabulary.
 * <p>
 * Thread-safe: the tags of a record are immutable lists replaced as a whole.
 */
public final class RecordTags{

	/** The types of record that can be tagged. */
	public static final Set<String> TAGGABLE_TYPES = Set.of(IndividualReader.TYPE, GroupReader.TYPE, PlaceReader.TYPE, SourceReader.TYPE,
		RepositoryReader.TYPE, ResearchQuestionReader.TYPE, ResearchActivityReader.TYPE, ResearchTaskReader.TYPE);

	private static final String SEPARATORS = ",;";


	private final Map<String, List<String>> tagsById = new ConcurrentHashMap<>();
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();


	/** @return	Whether a record of that type (as {@link FLEFModel#getRecordsByType} names it, in any case) can be tagged. */
	public static boolean isTaggable(final String recordType){
		return (recordType != null && TAGGABLE_TYPES.contains(recordType.toUpperCase(Locale.ROOT)));
	}

	/** @return	The tag in its normal form (trimmed, inner spaces collapsed), empty if there is nothing to it. */
	public static String normalize(final String tag){
		return (tag != null? StringUtils.normalizeSpace(tag): StringUtils.EMPTY);
	}

	/**
	 * @param text	Tags typed one after the other, separated by commas or semicolons.
	 * @return	The tags, normalized, without the empty ones and the repeated ones.
	 */
	public static List<String> parse(final String text){
		final List<String> tags = new ArrayList<>();
		if(StringUtils.isNotBlank(text))
			for(final String part : StringUtils.split(text, SEPARATORS))
				addDistinct(tags, part);
		return tags;
	}

	private static void addDistinct(final List<String> tags, final String tag){
		final String normalized = normalize(tag);
		if(!normalized.isEmpty() && tags.stream().noneMatch(normalized::equalsIgnoreCase))
			tags.add(normalized);
	}

	private static List<String> distinctSorted(final Collection<String> tags){
		final List<String> result = new ArrayList<>(tags.size());
		for(final String tag : tags)
			addDistinct(result, tag);
		result.sort(String.CASE_INSENSITIVE_ORDER);
		return List.copyOf(result);
	}


	/** @return	The tags of the record, sorted, never {@code null}. */
	public List<String> get(final String recordId){
		final List<String> tags = (recordId != null? tagsById.get(recordId): null);
		return (tags != null? tags: List.of());
	}

	public boolean has(final String recordId, final String tag){
		final String normalized = normalize(tag);
		return (!normalized.isEmpty() && get(recordId).stream().anyMatch(normalized::equalsIgnoreCase));
	}

	/** @return	Whether the record has all the tags (always {@code true} if none is asked for). */
	public boolean hasAll(final String recordId, final Collection<String> required){
		for(final String tag : required)
			if(!has(recordId, tag))
				return false;
		return true;
	}

	public boolean isEmpty(){
		return tagsById.isEmpty();
	}

	/** Replaces the tags of the record (the empty ones and the repeated ones are dropped). */
	public void set(final String recordId, final Collection<String> tags){
		if(recordId == null)
			return;

		final List<String> sorted = distinctSorted(tags != null? tags: List.of());
		final List<String> before = (sorted.isEmpty()? tagsById.remove(recordId): tagsById.put(recordId, sorted));
		if(!sorted.equals(before != null? before: List.of()))
			fireChanged();
	}

	public void add(final String recordId, final String tag){
		final List<String> tags = new ArrayList<>(get(recordId));
		tags.add(tag);
		set(recordId, tags);
	}

	public void remove(final String recordId, final String tag){
		final List<String> tags = new ArrayList<>(get(recordId));
		if(tags.removeIf(normalize(tag)::equalsIgnoreCase))
			set(recordId, tags);
	}

	/** Takes away every tag of the record. */
	public void clear(final String recordId){
		set(recordId, List.of());
	}

	/**
	 * A record that absorbs another (or takes its identifier) has the tags of both: the tags of the absorbed one are added to the ones of the
	 * survivor and taken from it.
	 */
	public void merge(final String absorbedId, final String survivorId){
		if(absorbedId == null || survivorId == null || absorbedId.equals(survivorId))
			return;

		final List<String> absorbed = tagsById.get(absorbedId);
		if(absorbed == null)
			return;

		final List<String> union = new ArrayList<>(get(survivorId));
		union.addAll(absorbed);
		tagsById.remove(absorbedId);
		tagsById.put(survivorId, distinctSorted(union));
		fireChanged();
	}

	/** Drops the tags of the records that are not there any more. */
	public void retainOnly(final Predicate<String> recordExists){
		if(tagsById.keySet().removeIf(id -> !recordExists.test(id)))
			fireChanged();
	}

	/**
	 * @param recordExists	Tells whether a record is in the file (the tags of a record that is not are not counted).
	 * @return	Every tag in use, with the number of records that carry it, sorted by tag (the spelling is the first met).
	 */
	public Map<String, Integer> counts(final Predicate<String> recordExists){
		final Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for(final Map.Entry<String, List<String>> entry : tagsById.entrySet())
			if(recordExists.test(entry.getKey()))
				for(final String tag : entry.getValue())
					counts.merge(tag, 1, Integer::sum);
		return counts;
	}

	/** @return	The identifiers of the records with all the tags, in no particular order. */
	public Set<String> idsWithAll(final Collection<String> required){
		final Set<String> ids = ConcurrentHashMap.newKeySet();
		for(final String id : tagsById.keySet())
			if(hasAll(id, required))
				ids.add(id);
		return ids;
	}

	/** @return	A copy of every tag of every record. */
	public Map<String, List<String>> snapshot(){
		return Map.copyOf(tagsById);
	}

	/** Replaces every tag with the given ones (what the store has for the file). */
	public void replaceAll(final Map<String, ? extends Collection<String>> all){
		tagsById.clear();
		for(final Map.Entry<String, ? extends Collection<String>> entry : all.entrySet()){
			final List<String> sorted = distinctSorted(entry.getValue());
			if(!sorted.isEmpty())
				tagsById.put(entry.getKey(), sorted);
		}
		fireChanged();
	}

	/** Tells when a tag is added, changed or taken away (on the thread that did it). */
	public void addChangeListener(final Runnable listener){
		listeners.add(listener);
	}

	public void removeChangeListener(final Runnable listener){
		listeners.remove(listener);
	}

	private void fireChanged(){
		for(final Runnable listener : listeners)
			listener.run();
	}

}
