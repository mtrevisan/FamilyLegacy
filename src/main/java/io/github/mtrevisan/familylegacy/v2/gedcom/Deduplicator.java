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
package io.github.mtrevisan.familylegacy.v2.gedcom;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

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


	public static String getDeduplicatedRecordId(FLEFModel model, FLEFRecord record){
		if(record.getTag().equalsIgnoreCase(RelationshipHandler.TYPE)){
			String type = FLEFRecordHelper.getChildValue(record, "type");
			if("biological_child".equals(type)){
				FLEFRecord subject = FLEFRecordHelper.findChild(record, "subject");
				String subjectId = subject.getTheOnlyChild()
					.getValue();
				FLEFRecord target = FLEFRecordHelper.findChild(record, "target");
				String targetId = target.getTheOnlyChild()
					.getValue();

				List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
				for(final FLEFRecord relationship : relationships){
					if(!"adoptive_child".equalsIgnoreCase(FLEFRecordHelper.getChildValue(relationship, "type")))
						continue;
					if(!subjectId.equalsIgnoreCase(relationship.extractReferencedId("subject", IndividualHandler.TYPE)))
						continue;
					if(!targetId.equalsIgnoreCase(relationship.extractReferencedId("target", IndividualHandler.TYPE)))
						continue;

					//relationship duplicate found
					return relationship.getId();
				}

				StringBuilder sb = new StringBuilder();
				sb.append("|subject:");
				sb.append(GEDCOMHelper.computeSignature(subject));
				sb.append("|target:");
				sb.append(GEDCOMHelper.computeSignature(target));
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

		String thisSignature = GEDCOMHelper.computeSignature(record);
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

}
