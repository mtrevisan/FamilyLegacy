package io.github.mtrevisan.familylegacy.v2.gedcom;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.HashMap;
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


	private Deduplicator(){}


	public static String getDeduplicatedRecordId(FLEFModel model, FLEFRecord record){
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
