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
package io.github.mtrevisan.familylegacy.v2.ui.tools.statistics;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Computes the statistics of a FLEF model in a single pass and reports
 * progress to a callback, so the caller can drive a progress bar while
 * the computation runs on a background thread.
 * <p>
 * The three sections (completeness, distribution, coverage) share the
 * indices built at the beginning (event type by id, participants by
 * individual, parents by individual), so the model is traversed once
 * for indexing and once for accumulation instead of three times.
 */
public final class StatisticsCalculator{


	/** Receives progress updates; may be called from a background thread. */
	@FunctionalInterface
	public interface Progress{
		void update(int percent, String message);
	}


	private static final String TAG_NAME = "name";
	private static final String TAG_PART = "part";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_EVENT = "event";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_INDIVIDUAL = IndividualHandler.TYPE;

	private static final String EVENT_BIRTH = "birth";
	private static final String EVENT_DEATH = "death";

	private static final int TOP_SURNAMES = 20;
	private static final int TOP_GIVEN_NAMES = 20;
	private static final int TOP_SOURCES = 10;


	private final FLEFModel model;


	public StatisticsCalculator(final FLEFModel model){
		this.model = Objects.requireNonNull(model, "Model must not be null");
	}


	/**
	 * Computes the statistics.
	 *
	 * @param progress the progress callback; may be {@code null}
	 * @return the statistics, never {@code null}
	 */
	public Statistics compute(final Progress progress){
		final Progress p = (progress != null? progress: (percent, message) -> {
		});

		p.update(0, "Indexing events…");

		// --- Index: event type by event id ---
		final Map<String, String> eventTypeById = new HashMap<>();
		final List<FLEFRecord> events = model.getRecordsByType(EventHandler.TYPE);
		for(final FLEFRecord e : events){
			final String id = e.getId();
			final String t = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(id != null && t != null)
				eventTypeById.put(id, t.toLowerCase(Locale.ROOT));
		}

		// --- Index: participants and their birth/death events ---
		p.update(5, "Indexing participations…");
		final Set<String> hasBirth = new HashSet<>();
		final Set<String> hasDeath = new HashSet<>();
		for(final FLEFRecord part : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String indId = extractParticipantId(part);
			if(indId == null)
				continue;
			final String eventId = FLEFRecordHelper.getChildValue(part, TAG_EVENT);
			if(eventId == null)
				continue;
			final String type = eventTypeById.get(eventId);
			if(EVENT_BIRTH.equals(type))
				hasBirth.add(indId);
			else if(EVENT_DEATH.equals(type))
				hasDeath.add(indId);
		}

		// --- Index: individuals with at least one parent relationship ---
		p.update(10, "Indexing parent relationships…");
		final Set<String> withParents = new HashSet<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, RelationshipHandler.TAG_TYPE);
			if(type == null)
				continue;

			if(!type.toLowerCase(Locale.ROOT).endsWith("_child"))
				continue;

			final String subj = relationship.extractReferencedId(RelationshipHandler.TAG_SUBJECT, IndividualHandler.TYPE);
			if(subj != null)
				withParents.add(subj);
		}

		// --- Completeness ---
		p.update(20, "Computing completeness…");
		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualHandler.TYPE);
		final List<Statistics.Completeness.Category> categories = new ArrayList<>();

		final List<FLEFRecord> noBirth = new ArrayList<>();
		final List<FLEFRecord> noDeath = new ArrayList<>();
		final List<FLEFRecord> noParents = new ArrayList<>();
		final List<FLEFRecord> noSource = new ArrayList<>();
		final List<FLEFRecord> noSex = new ArrayList<>();

		for(final FLEFRecord ind : individuals){
			final String id = ind.getId();
			if(id == null)
				continue;
			if(!hasBirth.contains(id))
				noBirth.add(ind);
			if(!hasDeath.contains(id))
				noDeath.add(ind);
			if(!withParents.contains(id))
				noParents.add(ind);
			if(!hasDirectSource(ind))
				noSource.add(ind);
			final String sex = FLEFRecordHelper.getChildValue(ind, IndividualHandler.TAG_SEX);
			if(sex == null || sex.isBlank())
				noSex.add(ind);
		}

		categories.add(new Statistics.Completeness.Category("Without birth date", List.copyOf(noBirth)));
		categories.add(new Statistics.Completeness.Category("Without death date", List.copyOf(noDeath)));
		categories.add(new Statistics.Completeness.Category("Without parents", List.copyOf(noParents)));
		categories.add(new Statistics.Completeness.Category("Without source citation", List.copyOf(noSource)));
		categories.add(new Statistics.Completeness.Category("Without recorded sex", List.copyOf(noSex)));

		final List<FLEFRecord> sources = model.getRecordsByType(SourceHandler.TYPE);
		final Statistics.Completeness completeness = new Statistics.Completeness(
			individuals.size(), events.size(), sources.size(), categories);

		// --- Distribution ---
		p.update(50, "Computing name distribution…");
		final Map<String, Integer> surnameCounts = new HashMap<>();
		final Map<String, Integer> givenCounts = new HashMap<>();
		int male = 0, female = 0, unknownSex = 0;

		for(final FLEFRecord ind : individuals){
			for(final FLEFRecord nameBlock : FLEFRecordHelper.findChildren(ind, TAG_NAME))
				for(final FLEFRecord part : FLEFRecordHelper.findChildren(nameBlock, TAG_PART)){
					final String type = FLEFRecordHelper.getChildValue(part, TAG_TYPE);
					final String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
					if(value == null || value.isBlank())
						continue;
					final String key = value.toLowerCase(Locale.ROOT)
						.trim();
					if("family".equals(type))
						surnameCounts.merge(key, 1, Integer::sum);
					else if("given".equals(type))
						givenCounts.merge(key, 1, Integer::sum);
				}

			final String sex = FLEFRecordHelper.getChildValue(ind, IndividualHandler.TAG_SEX);
			if(IndividualHandler.ENUM_SEX_MALE.equalsIgnoreCase(sex))
				male ++;
			else if(IndividualHandler.ENUM_SEX_FEMALE.equalsIgnoreCase(sex))
				female ++;
			else
				unknownSex++;
		}

		final List<Statistics.NameGroup> allSurnames = NameClusterer.cluster(surnameCounts);
		final List<Statistics.NameGroup> topSurnames = limit(allSurnames, TOP_SURNAMES);

		final List<Statistics.NameGroup> allGiven = NameClusterer.cluster(givenCounts);
		final List<Statistics.NameGroup> topGiven = limit(allGiven, TOP_GIVEN_NAMES);

		final int totalSurnames = surnameCounts.values().stream()
			.mapToInt(Integer::intValue)
			.sum();
		final int totalGivenNames = givenCounts.values().stream()
			.mapToInt(Integer::intValue)
			.sum();

		final Statistics.Distribution distribution = new Statistics.Distribution(
			male, female, unknownSex,
			totalSurnames, totalGivenNames,
			topSurnames, topGiven);

		// --- Coverage ---
		p.update(80, "Computing source coverage…");
		final Map<String, int[]> byTag = new LinkedHashMap<>();
		final Map<String, Integer> sourceUseCount = new HashMap<>();

		for(final FLEFRecord record : model.getRecords()){
			final String tag = record.getTag();
			if(tag == null)
				continue;
			final int[] counts = byTag.computeIfAbsent(tag, k -> new int[2]);
			counts[0]++;
			boolean cited = false;
			for(final FLEFRecord child : record.getChildren())
				if(TAG_SOURCE.equalsIgnoreCase(child.getTag())){
					cited = true;
					final String sid = extractSourceId(child);
					if(sid != null)
						sourceUseCount.merge(sid, 1, Integer::sum);
				}
			if(cited)
				counts[1]++;
		}

		final List<Statistics.TypeCoverage> coverageByType = new ArrayList<>(byTag.size());
		for(final Map.Entry<String, int[]> e : byTag.entrySet())
			coverageByType.add(new Statistics.TypeCoverage(e.getKey(), e.getValue()[0], e.getValue()[1]));
		coverageByType.sort(Comparator.comparing(Statistics.TypeCoverage::tag));

		final List<Map.Entry<String, Integer>> sortedSources = new ArrayList<>(sourceUseCount.entrySet());
		sortedSources.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		final List<Statistics.SourceUse> topSources = new ArrayList<>();
		for(int i = 0; i < Math.min(TOP_SOURCES, sortedSources.size()); i++){
			final Map.Entry<String, Integer> e = sortedSources.get(i);
			final FLEFRecord source = model.getRecordById(e.getKey());
			final String displayName = (source != null
				? SourceHandler.getInstance()
				.getDisplayText(source, model)
				: e.getKey());
			topSources.add(new Statistics.SourceUse(e.getKey(), displayName, e.getValue()));
		}

		int totalRecords = 0;
		for(final int[] c : byTag.values())
			totalRecords += c[0];

		int totalCitations = 0;
		for(final int c : sourceUseCount.values())
			totalCitations += c;

		final Statistics.Coverage coverage = new Statistics.Coverage(totalRecords, totalCitations, coverageByType,
			topSources);

		p.update(100, "Done");
		return new Statistics(completeness, distribution, coverage);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static <T> List<T> limit(final List<T> list, final int max){
		return (list.size() <= max? list: List.copyOf(list.subList(0, max)));
	}

	private static boolean hasDirectSource(final FLEFRecord record){
		for(final FLEFRecord child : record.getChildren())
			if(TAG_SOURCE.equalsIgnoreCase(child.getTag()))
				return true;
		return false;
	}

	private static String extractParticipantId(final FLEFRecord participation){
		final FLEFRecord block = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(block == null)
			return null;
		final FLEFRecord oneof = block.getTheOnlyChild();
		if(oneof == null || !TAG_INDIVIDUAL.equalsIgnoreCase(oneof.getTag()))
			return null;
		final FLEFRecord ref = oneof.getTheOnlyChild();
		return (ref != null? ref.getValue(): oneof.getValue());
	}

	private static String extractSourceId(final FLEFRecord sourceChild){
		final String direct = FLEFRecordHelper.getChildValue(sourceChild, TAG_SOURCE);
		if(direct != null)
			return direct;
		final FLEFRecord inner = FLEFRecordHelper.findChild(sourceChild, TAG_SOURCE);
		if(inner != null)
			return (inner.getTheOnlyChild() != null
				? inner.getTheOnlyChild()
				.getValue()
				: inner.getValue());
		return null;
	}

}
