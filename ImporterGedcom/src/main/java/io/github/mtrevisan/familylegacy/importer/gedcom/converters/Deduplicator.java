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

import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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


	private Deduplicator(){}


	/** Forgets the records seen so far; to be called before each conversion, as they belong to the model being built. */
	public static void reset(){
		CANONICAL_MAP.clear();
		CANONICAL_MAP_RELATIONSHIP.clear();
	}

	public static String getDeduplicatedRecordId(FLEFModel model, FLEFRecord record){
		if(record.getTag().equalsIgnoreCase(RelationshipReader.TYPE)){
			String type = FLEFRecordHelper.getChildValue(record, RelationshipReader.TAG_TYPE);
			if(RelationshipReader.isTypeBiologicalChild(type)){
				FLEFRecord subject = FLEFRecordHelper.findChild(record, RelationshipReader.TAG_SUBJECT);
				FLEFRecord target = FLEFRecordHelper.findChild(record, RelationshipReader.TAG_TARGET);

				StringBuilder sb = new StringBuilder();
				sb.append("|subject:");
				sb.append(computeSignature(subject));
				sb.append("|target:");
				sb.append(computeSignature(target));
				String thisSignature = sb.toString();

				FLEFRecord existingRecord = CANONICAL_MAP_RELATIONSHIP.get(thisSignature);
				if(existingRecord == null){
					CANONICAL_MAP_RELATIONSHIP.put(thisSignature, record);

					model.addRecord(record);

					return record.getId();
				}

				String existingRecordId = existingRecord.getId();
				record.setId(existingRecordId);

				return existingRecordId;
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

}
