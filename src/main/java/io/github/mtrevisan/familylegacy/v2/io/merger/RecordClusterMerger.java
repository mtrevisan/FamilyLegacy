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
package io.github.mtrevisan.familylegacy.v2.io.merger;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Merges a cluster of records (believed to represent the same entity) into a single record.
 * It uses trust‑weighted voting to choose the best values and merges child structures.
 */
public class RecordClusterMerger{

	/**
	 * Merges a list of records into one.
	 *
	 * @param records     the records (all considered the same entity)
	 * @param trustScores map from record to trust score; if null, they are computed on the fly
	 * @return the merged record, or null if the list is empty
	 */
	public static FLEFRecord merge(final List<FLEFRecord> records, Map<FLEFRecord, Double> trustScores){
		if(records == null || records.isEmpty())
			return null;
		if(records.size() == 1)
			return deepCopy(records.getFirst());

		if(trustScores == null){
			trustScores = new HashMap<>();
			for(final FLEFRecord r : records)
				trustScores.put(r, TrustScorer.score(r));
		}

		// Choose base record with highest trust
		final FLEFRecord base = Collections.max(records, Comparator.comparingDouble(trustScores::get));
		final FLEFRecord merged = deepCopy(base);

		// Merge children: add any child not already present
		final Set<String> existingSignatures = new HashSet<>();
		for(final FLEFRecord child : merged.getChildren())
			existingSignatures.add(signature(child));

		for(final FLEFRecord rec : records){
			if(rec == base)
				continue;
			for(final FLEFRecord child : rec.getChildren()){
				final String sig = signature(child);
				if(!existingSignatures.contains(sig)){
					merged.addChild(deepCopy(child));
					existingSignatures.add(sig);
				}
			}
			// If merged has no value and rec has, take it
			if(merged.getValue() == null && rec.getValue() != null)
				merged.setValue(rec.getValue());
		}

		// For conflicting simple fields (like sex, type), use trust-weighted majority
		// (implement as needed for specific fields)
		// Here we just keep base, but could add more logic.

		return merged;
	}

	/**
	 * Computes a signature for a record based on its tag, value, and children (sorted).
	 * This is used to deduplicate children during merge.
	 */
	private static String signature(final FLEFRecord record){
		final StringBuilder sb = new StringBuilder();
		sb.append(record.getTag());
		if(record.getValue() != null)
			sb.append('|')
				.append(record.getValue());
		// include children signatures sorted
		final List<String> childSigs = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			childSigs.add(signature(child));
		Collections.sort(childSigs);
		for(final String s : childSigs)
			sb.append('|')
				.append(s);
		return sb.toString();
	}

	private static FLEFRecord deepCopy(final FLEFRecord record){
		if(record == null)
			return null;

		final FLEFRecord copy = FLEFRecord.createChildWithTag(record.getTag());
		record.deepCopyTo(copy);
		return copy;
	}

}
