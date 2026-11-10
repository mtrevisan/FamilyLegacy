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
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
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
 * Merges the places that are the same place even if the file gives them under different jurisdictions (e.g. "Canizzano, Treviso, Italia"
 * and "Canizzano, Treviso, Veneto, Italia"): the ones with the same name and the same coordinates, whose types do not contradict each
 * other.
 * <p>
 * The first place survives, completed with what the others say more; the references to the others follow the survivor.
 */
final class PlaceMerger{

	private PlaceMerger(){}


	static void merge(final FLEFModel model){
		final Map<String, List<FLEFRecord>> survivorsByKey = new HashMap<>();
		final Map<String, String> merged = new HashMap<>();
		for(final FLEFRecord place : new ArrayList<>(model.getRecordsByType(PlaceReader.TYPE))){
			final String name = nameOf(place);
			final String coordinates = StringUtils.trimToNull(FLEFRecordHelper.getChildValue(place, PlaceReader.TAG_LOCATION_COORDINATES));
			//without coordinates there is nothing that tells that two places with the same name are the same one
			if(StringUtils.isEmpty(name) || coordinates == null)
				continue;

			final List<FLEFRecord> candidates = survivorsByKey.computeIfAbsent(name + "|" + coordinates, k -> new ArrayList<>(1));
			FLEFRecord survivor = null;
			for(final FLEFRecord candidate : candidates)
				if(compatibleTypes(candidate, place)){
					survivor = candidate;
					break;
				}
			if(survivor == null){
				candidates.add(place);
				continue;
			}

			mergeInto(survivor, place);
			merged.put(place.getId(), survivor.getId());
		}
		if(merged.isEmpty())
			return;

		for(final FLEFRecord record : model.getRecords())
			remap(record, merged);
		for(final String duplicateId : merged.keySet())
			model.removeRecord(duplicateId);
		cleanRelationships(model);
	}

	/** The first name of the place, without capitals and with the spaces normalized. */
	private static String nameOf(final FLEFRecord place){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(place, PlaceReader.TAG_NAME);
		return StringUtils.lowerCase(StringUtils.normalizeSpace(names.isEmpty()? null: FLEFRecordHelper.getChildValue(names.getFirst(),
			NameReader.TAG_VALUE)));
	}

	private static boolean compatibleTypes(final FLEFRecord first, final FLEFRecord second){
		final String firstType = FLEFRecordHelper.getChildValue(first, PlaceReader.TAG_TYPE);
		final String secondType = FLEFRecordHelper.getChildValue(second, PlaceReader.TAG_TYPE);
		return (firstType == null || secondType == null || firstType.equals(secondType));
	}

	/** What the duplicate says that the survivor does not (another name, a note, a source) is not lost: it is added to the survivor. */
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
			final boolean repeatable = (PlaceReader.TAG_NAME.equals(tag) || PlaceReader.TAG_SOURCE.equals(tag) || PlaceReader.TAG_NOTE.equals(tag));
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

	/** Makes the references to a merged place (the citations and the ends of the relationships) refer to the survivor. */
	private static void remap(final FLEFRecord node, final Map<String, String> merged){
		if(PlaceCitationReader.TAG_PLACE.equalsIgnoreCase(node.getTag()) && node.getValue() != null && merged.containsKey(node.getValue()))
			node.setValue(merged.get(node.getValue()));
		for(final FLEFRecord child : node.getChildren())
			remap(child, merged);
	}

	/** The relationships that became a place contained in itself are removed, and the ones that became the same are kept once. */
	private static void cleanRelationships(final FLEFModel model){
		final Set<String> seen = new HashSet<>();
		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipReader.TYPE)){
			final String subject = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_SUBJECT_PLACE);
			final String target = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_TARGET_PLACE);
			final String type = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_TYPE);
			if(subject != null && (subject.equals(target) || !seen.add(subject + "->" + target + "|" + type)))
				toRemove.add(relationship.getId());
		}
		for(final String id : toRemove)
			model.removeRecord(id);
	}

}
