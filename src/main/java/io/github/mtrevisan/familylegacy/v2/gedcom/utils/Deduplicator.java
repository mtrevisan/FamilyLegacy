package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.Strings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

	private Deduplicator(){
	}

	/**
	 * Deduplicates all records in the model.
	 * Modifies the model in place.
	 *
	 * @param model the FLEFModel to deduplicate
	 */
	public static void deduplicate(FLEFModel model){
		// Group top‑level records by their tag (type)
		Map<String, List<FLEFRecord>> recordsByType = new HashMap<>();
		for(FLEFRecord record : model.getRecords()){
			String tag = record.getTag();
			recordsByType.computeIfAbsent(tag, k -> new ArrayList<>()).add(record);
		}

		// For each type, deduplicate by content signature
		for(List<FLEFRecord> records : recordsByType.values()){
			if(records.size() < 2) continue;

			Map<String, FLEFRecord> canonicalMap = new LinkedHashMap<>();
			for(FLEFRecord record : records){
				String signature = computeSignature(record);
				FLEFRecord existing = canonicalMap.get(signature);
				if(existing == null){
					canonicalMap.put(signature, record);
				}
				else{
					// Duplicate found: remove this record and update references
					String canonicalId = existing.getId();
					String duplicateId = record.getId();
					if(duplicateId != null && !duplicateId.equals(canonicalId)
							&& !duplicateId.startsWith("I") && !duplicateId.startsWith("F") && !duplicateId.startsWith("E")){
						updateReferences(model, duplicateId, canonicalId);
						FLEFRecord removedRecord = model.removeRecord(duplicateId);
						System.out.println("Removed duplicate: " + removedRecord + ", keep " + canonicalId);
					}
				}
			}
		}
	}

	/**
	 * Computes a content signature for a record, ignoring its ID and audit children.
	 * The signature is a string that represents the record's structure and values.
	 * It is deterministic regardless of child order.
	 *
	 * @param record the record
	 * @return a signature string
	 */
	private static String computeSignature(FLEFRecord record){
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
			if("audit".equals(child.getTag())){
				continue;
			}
			childSignatures.add(computeSignature(child));
		}
		// Sort to make order-independent
		Collections.sort(childSignatures);
		sb.append(String.join(",", childSignatures));

		return sb.toString();
	}

	/**
	 * Updates all references in the model from oldId to newId.
	 * Traverses all records (including header) and replaces any value that equals oldId.
	 *
	 * @param model the model
	 * @param oldId the ID to replace
	 * @param newId the replacement ID
	 */
	private static void updateReferences(FLEFModel model, String oldId, String newId){
		if(Strings.CS.equals(oldId, newId)) return;

		Deque<FLEFRecord> stack = new ArrayDeque<>();

		// Add top‑level records
		for(FLEFRecord record : model.getRecords()){
			stack.push(record);
		}
		// Add header if present
		if(model.getHeader() != null){
			stack.push(model.getHeader());
		}

		while(!stack.isEmpty()){
			FLEFRecord current = stack.pop();

			// Check the current record's value (if it's a reference node)
			String currentValue = current.getValue();
			if(oldId.equals(currentValue)){
				current.setValue(newId);
			}

			// Push children
			for(FLEFRecord child : current.getChildren()){
				stack.push(child);
			}
		}
	}

}
