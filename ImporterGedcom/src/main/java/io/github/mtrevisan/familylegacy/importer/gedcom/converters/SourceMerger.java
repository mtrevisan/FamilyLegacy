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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Merges the sources that are the same source: the ones with the same title that have a media in common (the same document).
 * <p>
 * The first source survives, completed with what the others say more; the references to the others follow the survivor.
 */
final class SourceMerger{

	private SourceMerger(){}


	/**
	 * @param sourceMap	The sources by id: the merged ones are removed from it.
	 */
	static void merge(final FLEFModel model, final Map<String, FLEFRecord> sourceMap){
		//the survivor of each (title, media) pair
		final Map<String, FLEFRecord> survivors = new HashMap<>();
		final Map<String, String> merged = new HashMap<>();
		for(final FLEFRecord source : new ArrayList<>(model.getRecordsByType(SourceReader.TYPE))){
			final List<FLEFRecord> titles = FLEFRecordHelper.findChildren(source, SourceReader.TAG_TITLE);
			//the first title is the title of the source (the others are alternatives, e.g. the abbreviation)
			final String title = StringUtils.lowerCase(StringUtils.normalizeSpace(titles.isEmpty()? null: FLEFRecordHelper.getChildValue(titles.getFirst(), "value")));
			if(StringUtils.isEmpty(title))
				continue;

			FLEFRecord survivor = null;
			final List<String> mediaIds = mediaOf(source);
			for(final String mediaId : mediaIds){
				final FLEFRecord found = survivors.get(title + "|" + mediaId);
				if(found != null && !found.getId().equals(source.getId())){
					survivor = found;
					break;
				}
			}
			if(survivor == null){
				for(final String mediaId : mediaIds)
					survivors.putIfAbsent(title + "|" + mediaId, source);
				continue;
			}

			mergeInto(survivor, source);
			for(final String mediaId : mediaOf(survivor))
				survivors.putIfAbsent(title + "|" + mediaId, survivor);
			merged.put(source.getId(), survivor.getId());
		}
		if(merged.isEmpty())
			return;

		for(final FLEFRecord record : model.getRecords())
			remap(record, merged);
		for(final String duplicateId : merged.keySet()){
			model.removeRecord(duplicateId);
			sourceMap.remove(duplicateId);
		}
	}

	private static List<String> mediaOf(final FLEFRecord source){
		return FLEFRecordHelper.findChildren(source, SourceReader.TAG_MEDIA).stream()
			.map(FLEFRecord::getValue)
			.filter(StringUtils::isNotBlank)
			.toList();
	}

	/** What the duplicate says that the survivor does not (a date, a repository, a note) is not lost: it is added to the survivor. */
	private static void mergeInto(final FLEFRecord survivor, final FLEFRecord duplicate){
		final Set<String> known = new HashSet<>();
		final Set<String> knownTags = new HashSet<>();
		for(final FLEFRecord child : survivor.getChildren()){
			known.add(Deduplicator.computeSignature(child));
			knownTags.add(child.getTag());
		}
		for(final FLEFRecord child : new ArrayList<>(duplicate.getChildren())){
			final String tag = child.getTag();
			if(AuditReader.TAG_AUDIT.equals(tag) || known.contains(Deduplicator.computeSignature(child)))
				continue;
			//a field that can be only one stays the one of the survivor
			final boolean repeatable = (SourceReader.TAG_MEDIA.equals(tag) || SourceReader.TAG_TITLE.equals(tag) || "note".equals(tag));
			if(!repeatable && knownTags.contains(tag))
				continue;

			//the audit is the last field of a record
			final List<FLEFRecord> children = survivor.getChildren();
			int index = children.size();
			for(int i = 0; i < children.size(); i ++)
				if(AuditReader.TAG_AUDIT.equals(children.get(i).getTag())){
					index = i;
					break;
				}
			children.add(index, child);
		}
	}

	/** Makes the references to a merged source refer to the survivor. */
	private static void remap(final FLEFRecord node, final Map<String, String> merged){
		if(SourceCitationReader.TAG_SOURCE.equals(node.getTag()) && node.getValue() != null && merged.containsKey(node.getValue()))
			node.setValue(merged.get(node.getValue()));
		for(final FLEFRecord child : node.getChildren())
			remap(child, merged);
	}

}
