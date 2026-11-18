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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The sex of an individual, as the preferred assertion among the {@code sex} individual attributes of the individual (the
 * "Preferred assertion" rules of the protocol).
 * <p>
 * The sex is single-valued, so the competing assertions are all the IndividualAttributeRecords of type {@code sex} of the individual,
 * and the one to use is selected as follows:
 * <ol>
 *   <li>the PREFERRED target of the applicable ConclusionRecord (its RESOLVES contains at least two of the competing assertions, and
 *   its PREFERRED is one of them) with the strongest PROOF_STATUS (proven, then supported), and among equals the most recent by
 *   AUDIT;</li>
 *   <li>otherwise, the only assertion, if there is only one;</li>
 *   <li>otherwise, the assertion with the strictly highest CONFIDENCE;</li>
 *   <li>otherwise, the sex is undetermined ({@link SexType#UNKNOWN}).</li>
 * </ol>
 * An assertion whose value is {@code unknown} (or is not a sex) says nothing about the sex: it competes for the conclusions (a
 * researcher may have judged it) but it is not a candidate of the steps 2 and 3, so that {@code male} and {@code unknown}
 * together give {@code male}.
 * <p>
 * The assertions are never changed: this only selects one of them for presentation and convenience features.
 */
public final class PreferredSex{

	private static final int NO_RANK = 0;


	private PreferredSex(){}


	/**
	 * @param individual	The IndividualRecord (may be {@code null}).
	 * @param model	Where the attributes and the conclusions are looked up (may be {@code null}).
	 * @return	The preferred sex, {@link SexType#UNKNOWN} if it is undetermined.
	 */
	public static SexType of(final FLEFRecord individual, final RecordLookup model){
		final FLEFRecord assertion = preferredAssertion(individual, model);
		return (assertion != null
			? SexType.fromRaw(FLEFRecordHelper.getChildValue(assertion, IndividualAttributeReader.TAG_VALUE))
			: SexType.UNKNOWN);
	}

	/** @return	The raw value (male, female, unknown) of the {@linkplain #of(FLEFRecord, RecordLookup) preferred sex}. */
	public static String rawOf(final FLEFRecord individual, final RecordLookup model){
		return of(individual, model).getRawSex();
	}

	/**
	 * @return	The assertion of the preferred sex, {@code null} if the sex is undetermined (or the preferred assertion says
	 * {@code unknown} through a conclusion: then the sex is unknown, but there is a record that tells it).
	 */
	public static FLEFRecord preferredAssertion(final FLEFRecord individual, final RecordLookup model){
		if(individual == null || individual.getId() == null || model == null)
			return null;

		return select(assertions(individual.getId(), model), model);
	}

	/**
	 * For those who already have the attributes of every individual (so that they need not look for them in the whole model).
	 *
	 * @param attributes	The attributes of one individual (of any type: the ones that are not a sex are ignored), may be {@code null}.
	 * @param model	Where the conclusions are looked up (may be {@code null}).
	 * @return	The preferred sex, {@link SexType#UNKNOWN} if it is undetermined.
	 */
	public static SexType ofAttributes(final List<FLEFRecord> attributes, final RecordLookup model){
		if(attributes == null || model == null)
			return SexType.UNKNOWN;

		final List<FLEFRecord> competing = new ArrayList<>(2);
		for(final FLEFRecord attribute : attributes)
			if(IndividualAttributeReader.ENUM_TYPE_SEX.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
				competing.add(attribute);
		final FLEFRecord assertion = select(competing, model);
		return (assertion != null
			? SexType.fromRaw(FLEFRecordHelper.getChildValue(assertion, IndividualAttributeReader.TAG_VALUE))
			: SexType.UNKNOWN);
	}

	/**
	 * For whoever needs the sex of many individuals: finding the assertions of each in the whole model would be quadratic.
	 *
	 * @return	The {@code sex} attributes of the model, by individual.
	 */
	public static Map<String, List<FLEFRecord>> indexByIndividual(final RecordLookup model){
		final Map<String, List<FLEFRecord>> index = new HashMap<>();
		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeReader.TYPE);
		for(final FLEFRecord attribute : attributes){
			if(!IndividualAttributeReader.ENUM_TYPE_SEX.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
				continue;

			final String individualId = FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL);
			if(individualId != null)
				index.computeIfAbsent(individualId, k -> new ArrayList<>(2))
					.add(attribute);
		}
		return index;
	}

	/** @return	The preferred sex of the individual, given the {@linkplain #indexByIndividual(RecordLookup) index} of the assertions. */
	public static SexType of(final FLEFRecord individual, final Map<String, List<FLEFRecord>> index, final RecordLookup model){
		return (individual != null && individual.getId() != null? ofAttributes(index.get(individual.getId()), model): SexType.UNKNOWN);
	}

	private static FLEFRecord select(final List<FLEFRecord> competing, final RecordLookup model){
		if(competing.isEmpty())
			return null;

		final FLEFRecord concluded = concludedAssertion(competing, model);
		if(concluded != null)
			return concluded;

		final List<FLEFRecord> candidates = new ArrayList<>(competing.size());
		for(final FLEFRecord assertion : competing)
			if(isInformative(assertion))
				candidates.add(assertion);
		if(candidates.size() == 1)
			return candidates.getFirst();

		return strictlyMostConfident(candidates);
	}

	/** @return	All the {@code sex} attributes of the individual, the informative or not. */
	public static List<FLEFRecord> assertions(final String individualId, final RecordLookup model){
		final List<FLEFRecord> assertions = new ArrayList<>(2);
		if(individualId == null || model == null)
			return assertions;

		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeReader.TYPE);
		for(final FLEFRecord attribute : attributes)
			if(IndividualAttributeReader.ENUM_TYPE_SEX.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE))
					&& individualId.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL)))
				assertions.add(attribute);
		return assertions;
	}

	private static boolean isInformative(final FLEFRecord assertion){
		return (SexType.fromRaw(FLEFRecordHelper.getChildValue(assertion, IndividualAttributeReader.TAG_VALUE)) != SexType.UNKNOWN);
	}


	/** Step 1: the preferred assertion of the strongest, most recent, applicable conclusion. */
	private static FLEFRecord concludedAssertion(final List<FLEFRecord> competing, final RecordLookup model){
		final Set<String> competingIds = new HashSet<>(competing.size() << 1);
		for(final FLEFRecord assertion : competing)
			competingIds.add(assertion.getId());
		if(competingIds.size() < 2)
			return null;

		FLEFRecord bestPreferred = null;
		int bestRank = NO_RANK;
		String bestDate = null;
		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionReader.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			final int rank = rankOfStatus(ConclusionReader.extractProofStatus(conclusion));
			if(rank == NO_RANK || rank < bestRank)
				continue;

			final String preferredId = armValue(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
			if(preferredId == null || !competingIds.contains(preferredId) || countResolved(conclusion, competingIds) < 2)
				continue;

			final String date = latestAuditDate(conclusion);
			if(rank > bestRank || isMoreRecent(date, bestDate)){
				bestRank = rank;
				bestDate = date;
				bestPreferred = model.getRecordById(preferredId);
			}
		}
		return bestPreferred;
	}

	private static int countResolved(final FLEFRecord conclusion, final Set<String> competingIds){
		final Set<String> resolved = new HashSet<>(4);
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES)){
			final String id = armValue(target);
			if(id != null && competingIds.contains(id))
				resolved.add(id);
		}
		return resolved.size();
	}

	private static int rankOfStatus(final String status){
		if(ConclusionReader.ENUM_PROOF_STATUS_PROVEN.equals(status))
			return 2;
		if(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED.equals(status))
			return 1;
		return NO_RANK;
	}

	/** @return	The latest of the creation and update dates of the audit (the ISO instants compare as text), {@code null} if it has none. */
	private static String latestAuditDate(final FLEFRecord record){
		String latest = FLEFRecordHelper.getChildValue(record, AuditReader.TAG_AUDIT + "." + AuditReader.TAG_CREATION_DATE);
		final List<FLEFRecord> updates = FLEFRecordHelper.findChildren(record, AuditReader.TAG_AUDIT + "." + AuditReader.TAG_UPDATE);
		for(final FLEFRecord update : updates){
			final String date = FLEFRecordHelper.getChildValue(update, AuditReader.TAG_DATE);
			if(isMoreRecent(date, latest))
				latest = date;
		}
		return latest;
	}

	private static boolean isMoreRecent(final String date, final String than){
		return (date != null && (than == null || date.compareTo(than) > 0));
	}


	/** Step 3: the assertion with the strictly highest confidence, {@code null} if there is none (a tie, or no confidence at all). */
	private static FLEFRecord strictlyMostConfident(final List<FLEFRecord> candidates){
		FLEFRecord best = null;
		int bestRank = NO_RANK;
		boolean tie = false;
		for(final FLEFRecord candidate : candidates){
			final int rank = rankOfConfidence(FLEFRecordHelper.getChildValue(candidate, IndividualAttributeReader.TAG_CONFIDENCE));
			if(rank > bestRank){
				bestRank = rank;
				best = candidate;
				tie = false;
			}
			else if(rank == bestRank)
				tie = true;
		}
		return (best != null && !tie? best: null);
	}

	private static int rankOfConfidence(final String confidence){
		if(ConfidenceReader.ENUM_HIGH.equals(confidence))
			return 3;
		if(ConfidenceReader.ENUM_MEDIUM.equals(confidence))
			return 2;
		if(ConfidenceReader.ENUM_LOW.equals(confidence))
			return 1;
		return NO_RANK;
	}


	/** @return	The value of the alternative a "oneof" holds (an Xref), {@code null} if there is none. */
	private static String armValue(final FLEFRecord union){
		return (union == null || union.getChildren().isEmpty()? null: union.getChildren().getFirst().getValue());
	}

}
