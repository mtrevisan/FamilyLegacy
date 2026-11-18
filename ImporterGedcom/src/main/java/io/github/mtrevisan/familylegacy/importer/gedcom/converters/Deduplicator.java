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
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Removes duplicate records from a FLEFModel.
 * <p>
 * Two records are considered duplicates if they have the same content
 * when ignoring:
 * <ul>
 *   <li>the record's ID (the {@code id} property)</li>
 *   <li>any child with tag {@code "audit"}</li>
 * </ul>
 * The deduplication works for any record type (Individual, Group, Note, Place, etc.).
 */
public final class Deduplicator{

	private static final Map<String, FLEFRecord> CANONICAL_MAP = new HashMap<>();
	private static final Map<String, FLEFRecord> CANONICAL_MAP_RELATIONSHIP = new HashMap<>();


	/** The ids of the records merged into another one (duplicate id → surviving id). */
	private static final Map<String, String> ALIASES = new HashMap<>();

	/** The media by uri. */
	private static final Map<String, FLEFRecord> MEDIA_BY_URI = new HashMap<>();


	private Deduplicator(){}


	/** Forgets the records seen so far; to be called before each conversion, as they belong to the model being built. */
	public static void reset(){
		CANONICAL_MAP.clear();
		CANONICAL_MAP_RELATIONSHIP.clear();
		ALIASES.clear();
		MEDIA_BY_URI.clear();
	}

	/** Registers a record as merged into another one, so that the references to it can follow. */
	public static void registerAlias(final String duplicateId, final String survivingId){
		ALIASES.put(duplicateId, survivingId);
	}

	/** Returns the id of the record that survived in place of the given one, which is the given one itself if it was not merged. */
	public static String resolveAlias(final String id){
		return ALIASES.getOrDefault(id, id);
	}

	public static String getDeduplicatedRecordId(FLEFModel model, FLEFRecord record){
		if(record.getTag().equalsIgnoreCase(RelationshipReader.TYPE)){
			String type = FLEFRecordHelper.getChildValue(record, RelationshipReader.TAG_TYPE);
			String linkSignature = null;
			//the same child of the same parent is one link, however many times the file says it (the family, the individual, an association):
			//the type is part of the signature, as a child can be the biological child of a couple and the step child of one of them
			if(ArrayUtils.contains(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES, type)){
				FLEFRecord subject = FLEFRecordHelper.findChild(record, RelationshipReader.TAG_SUBJECT);
				FLEFRecord target = FLEFRecordHelper.findChild(record, RelationshipReader.TAG_TARGET);

				linkSignature = "|type:" + type + "|subject:" + computeSignature(subject) + "|target:" + computeSignature(target);
			}
			//two associates with the same role are the same relationship written from both sides ("collateral relative of the third
			//degree" of each other): the file says it in the individual of each one; with different roles (a guardian and a ward) they are two
			else if(RelationshipReader.isTypeAssociate(type))
				linkSignature = associateSignature(record, type);

			if(linkSignature != null){
				FLEFRecord existingRecord = CANONICAL_MAP_RELATIONSHIP.get(linkSignature);
				if(existingRecord == null){
					CANONICAL_MAP_RELATIONSHIP.put(linkSignature, record);

					model.addRecord(record);

					return record.getId();
				}

				mergeInto(existingRecord, record);

				String existingRecordId = existingRecord.getId();
				record.setId(existingRecordId);

				return existingRecordId;
			}
		}

		//the same file is the same media, whatever else is said about it (a date, a description)
		if(MediaReader.isOfType(record)){
			final FLEFRecord survivor = mergeMedia(record);
			if(survivor != null){
				record.setId(survivor.getId());
				return survivor.getId();
			}
		}

		String thisSignature = computeSignature(record);
		FLEFRecord existingRecord = CANONICAL_MAP.get(thisSignature);
		if(existingRecord == null){
			CANONICAL_MAP.put(thisSignature, record);

			model.addRecord(record);

			return record.getId();
		}

		String existingRecordId = existingRecord.getId();
		record.setId(existingRecordId);

		return existingRecordId;
	}


	/**
	 * The signature of an association: the two records it links, in no order, and its role (without capitals and spaces around), so
	 * that "A is a friend of B" and "B is a friend of A" are the same, and "A is the guardian of B" and "B is the ward of A" are not.
	 */
	private static String associateSignature(FLEFRecord record, String type){
		List<String> ends = new ArrayList<>(2);
		for(String tag : List.of(RelationshipReader.TAG_SUBJECT, RelationshipReader.TAG_TARGET)){
			FLEFRecord end = FLEFRecordHelper.findChild(record, tag);
			ends.add(end != null? computeSignature(end.getChildren().isEmpty()? end: end.getChildren().getFirst()): StringUtils.EMPTY);
		}
		Collections.sort(ends);
		String role = StringUtils.lowerCase(StringUtils.trimToEmpty(FLEFRecordHelper.getChildValue(record, RelationshipReader.TAG_ROLE)));
		return "|type:" + type + "|ends:" + ends.get(0) + "~" + ends.get(1) + "|role:" + role;
	}

	/** The children of a link that say who it links and what it is: the ones that make two links the same one. */
	private static final List<String> LINK_TAGS = List.of(RelationshipReader.TAG_SUBJECT, RelationshipReader.TAG_TARGET,
		RelationshipReader.TAG_TYPE, RelationshipReader.TAG_ROLE, AuditReader.TAG_AUDIT);

	/** What the second statement of a link says that the first does not (a source, a note, a date) is not lost: it is added to the first. */
	private static void mergeInto(FLEFRecord existing, FLEFRecord duplicate){
		List<String> known = new ArrayList<>();
		for(FLEFRecord child : existing.getChildren())
			known.add(computeSignature(child));
		for(FLEFRecord child : new ArrayList<>(duplicate.getChildren()))
			if(!LINK_TAGS.contains(child.getTag()) && !known.contains(computeSignature(child)))
				existing.addChild(child);
	}

	/**
	 * Computes a content signature for a record, ignoring its ID and audit children.
	 * The signature is a string that represents the record's structure and values.
	 * It is deterministic regardless of child order.
	 *
	 * @param record the record
	 * @return a signature string
	 */
	public static String computeSignature(FLEFRecord record){
		StringBuilder sb = new StringBuilder();
		sb.append(record.getTag());
		sb.append('|');

		// Include the scalar value (if any)
		String value = record.getValue();
		if(value != null){
			sb.append(value);
		}
		sb.append('|');

		// Collect signatures of all children except those with tag "audit"
		List<String> childSignatures = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if(AuditReader.TAG_AUDIT.equals(child.getTag())){
				continue;
			}
			childSignatures.add(computeSignature(child));
		}
		// Sort to make order-independent
		Collections.sort(childSignatures);
		sb.append(String.join(",", childSignatures));

		return sb.toString();
	}

	/** What the duplicate says that the survivor does not (a date, a description, a note) is not lost: it is added to the survivor. */
	private static void mergeMediaInto(final FLEFRecord existing, final FLEFRecord duplicate){
		final Set<String> known = new HashSet<>();
		final Set<String> knownTags = new HashSet<>();
		for(final FLEFRecord child : existing.getChildren()){
			known.add(computeSignature(child));
			knownTags.add(child.getTag());
		}
		for(final FLEFRecord child : new ArrayList<>(duplicate.getChildren())){
			final String tag = child.getTag();
			if(MediaReader.TAG_URI.equals(tag) || MediaReader.TAG_KIND.equals(tag) || MediaReader.TAG_AUDIT.equals(tag))
				continue;
			final boolean isNote = MediaReader.TAG_NOTE.equals(tag);
			if(isNote? known.contains(computeSignature(child)): knownTags.contains(tag))
				continue;

			//the notes, the privacy and the audit are the last fields of a MediaRecord
			final List<FLEFRecord> children = existing.getChildren();
			int index = children.size();
			for(int i = 0; i < children.size(); i ++){
				final String childTag = children.get(i).getTag();
				if(MediaReader.TAG_NOTE.equals(childTag) || MediaReader.TAG_PRIVACY.equals(childTag) || MediaReader.TAG_AUDIT.equals(childTag)){
					index = i;
					break;
				}
			}
			children.add(index, child);
		}
	}

	/**
	 * Registers a media by its uri, or merges it into the one already registered with the same uri.
	 *
	 * @return	The media that survives, {@code null} if the given one is the first with its uri (or has no uri to compare).
	 */
	public static FLEFRecord mergeMedia(final FLEFRecord media){
		final String uri = StringUtils.trimToNull(FLEFRecordHelper.getChildValue(media, MediaReader.TAG_URI));
		if(uri == null || "unknown".equals(uri))
			return null;

		final FLEFRecord existing = MEDIA_BY_URI.get(uri);
		if(existing == null){
			MEDIA_BY_URI.put(uri, media);
			return null;
		}

		mergeMediaInto(existing, media);
		return existing;
	}
}
