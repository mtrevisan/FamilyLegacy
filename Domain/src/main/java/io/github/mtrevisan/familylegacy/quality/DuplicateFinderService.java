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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.merger.TextSimilarity;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Finds possible duplicate individuals by comparing not only their own
 * data, but also their relationships: parents, children, siblings,
 * spouses, events they participated in, attributes, sources, and places.
 * <p>
 * <b>Why the relationships matter.</b> Two individuals with the same
 * name may be different people; two individuals with slightly different
 * names but the same parents and the same birth year are almost
 * certainly the same person. The service weights the shared
 * relationships heavily for this reason.
 * <p>
 * <b>Blocking.</b> The service never compares every pair of individuals.
 * Each profile is placed in one or more blocks keyed by the prefix of
 * its given name and of its family name, together with its sex, and
 * only profiles that share a block are compared. With twelve thousand
 * individuals, this reduces the number of comparisons from tens of
 * millions to tens of thousands.
 * <p>
 * <b>Sex as a hard filter.</b> When both individuals have a known and
 * different sex, the pair is discarded before scoring: a male and a
 * female cannot be the same historical person, no matter how similar
 * the rest of the data looks.
 */
public final class DuplicateFinderService{

	private static final Pattern PUNCTUATION_RUN = Pattern.compile("[\\p{Punct}]+");
	private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
	public static final int DEFAULT_THRESHOLD = 55;
	public static final int DEFAULT_MAX_RESULTS = 500;

	/** Years of tolerance when comparing birth or death years. */
	private static final int YEAR_TOLERANCE = 3;

	// Feature weights. They sum to more than 100 on purpose, because a
	// strong match on one dimension should be able to overcome a weak
	// match on another. The threshold is calibrated against real data.
	/** The share of the work that the description of the individuals is: the comparison of the pairs is the rest. */
	private static final double PROFILES_SHARE = 0.3;
	/** The comparisons between two reports of the progress. */
	private static final int PROGRESS_PAIRS = 2_000;
	/** The individuals described between two reports of the progress. */
	private static final int PROGRESS_INDIVIDUALS = 200;

	private static final int W_NAME_FULL = 40;
	private static final int W_NAME_PARTIAL = 25;
	private static final int W_NAME_TOKEN = 10;
	// the names that agree by the similarity of the letters (see NameSimilarity): less than the same names
	private static final int W_NAME_SIMILAR = 20;
	private static final int W_NAME_INITIAL = 15;
	private static final int W_NAME_TOKEN_SIMILAR = 6;
	private static final int W_PARENTS_ALL = 20;
	private static final int W_PARENTS_SOME = 12;
	private static final int W_CHILDREN_SOME = 12;
	private static final int W_SPOUSES_SOME = 10;
	private static final int W_SIBLINGS_SOME = 8;
	private static final int W_BIRTH_NEAR = 15;
	private static final int W_DEATH_NEAR = 10;
	private static final int W_EVENT_OVERLAP = 5;
	private static final int W_ATTRIBUTE_OVERLAP = 5;
	private static final int W_SOURCE_OVERLAP = 5;
	private static final int W_PLACE_OVERLAP = 5;
	// Second pass: the same facts compared by name, for the records that do not share an identifier (a tree imported twice has no
	// parent, spouse, place in common, only ones with the same name). Smaller than the weights of the shared records.
	private static final int W_PARENTS_NAMES = 12;
	private static final int W_SPOUSES_NAMES = 8;
	private static final int W_PLACES_NAMES = 4;
	private static final int W_OCCUPATIONS = 4;
	private static final int W_CHILDREN_NAMES = 8;
	private static final int W_ATTRIBUTE_TEXTS = 3;
	private static final int W_SOURCE_TITLES = 3;


	/** Tunable parameters of the search. */
	public static final class Options{
		public int threshold = DEFAULT_THRESHOLD;
		public int maxResults = DEFAULT_MAX_RESULTS;
		public boolean requireSameSex = true;
		/** When {@code true}, pairs already covered by an identity hypothesis are skipped. */
		public boolean skipExistingHypotheses = true;
	}

	/** The cached, precomputed description of one individual. */
	public record Profile(
		String id,
		String displayName,
		String sex,
		Set<String> givenTokens,
		Set<String> familyTokens,
		Set<String> fullNameVariants,
		Integer birthYear,
		Integer deathYear,
		Set<String> parentIds,
		Set<String> childIds,
		Set<String> siblingIds,
		Set<String> spouseIds,
		Set<String> eventIds,
		Set<String> attributeKeys,
		Set<String> sourceIds,
		Set<String> placeIds,
		/** The names of the parents (normalized), to compare the parents that do not share an identifier. */
		Set<String> parentNames,
		/** The names of the spouses (normalized). */
		Set<String> spouseNames,
		/** The names of the places of the events (normalized). */
		Set<String> placeNames,
		/** The occupations (normalized). */
		Set<String> occupations,
		/** The names of the children (normalized). */
		Set<String> childNames,
		/** The text of the attributes other than the occupations (residence, religion, ...), by type (normalized). */
		Map<String, Set<String>> attributeTexts,
		/** The titles of the cited sources (normalized). */
		Set<String> sourceTitles){}

	/** One matched feature, with its contribution to the total score. */
	public record FeatureMatch(String feature, int score, String detail){}

	/** One candidate pair, with the score and the matched/different features. */
	public record Candidate(
		Profile left,
		Profile right,
		int score,
		List<FeatureMatch> matches,
		List<String> differences){}


	private DuplicateFinderService(){}


	/**
	 * Finds duplicate candidates in the model.
	 *
	 * @param model   the model; must not be {@code null}
	 * @param options the search parameters; must not be {@code null}
	 * @return the candidate list, sorted by descending score; never
	 *         {@code null}
	 */
	public static List<Candidate> find(final FLEFModel model, final Options options){
		return find(model, options, fraction -> {});
	}

	/**
	 * Finds duplicate candidates in the model, telling how far it got, so that the user knows when the data will be there.
	 * <p>
	 * The progress is the share of the work done, from 0 to 1, and it never goes back: the first third is the description of each
	 * individual (the cost is the number of individuals), the rest is the comparison of the pairs that share a block (the cost is the
	 * number of those pairs, known before the first one is compared). It is called from the thread that runs the search, so a listener
	 * that touches Swing has to hand over to the event dispatch thread. If that thread is interrupted the search stops and gives
	 * nothing: a newer search replaces it.
	 * </p>
	 *
	 * @param model    the model; must not be {@code null}
	 * @param options  the search parameters; must not be {@code null}
	 * @param progress told, from time to time, the share of the work done (0 to 1); must not be {@code null}
	 * @return the candidate list, sorted by descending score; never {@code null}
	 */
	public static List<Candidate> find(final FLEFModel model, final Options options, final DoubleConsumer progress){
		final Map<String, Profile> profiles = buildProfiles(model, fraction -> progress.accept(fraction * PROFILES_SHARE));
		if(profiles.size() < 2){
			progress.accept(1.);
			return List.of();
		}

		final Set<String> existingHypotheses = (options.skipExistingHypotheses
			? existingHypothesisPairs(model)
			: Set.of());

		final Map<String, Set<Profile>> blocks = buildBlocks(profiles.values(), options.requireSameSex);
		final Set<String> seenPairs = new HashSet<>();
		final List<Candidate> candidates = new ArrayList<>();

		long totalPairs = 0L;
		for(final Set<Profile> blockSet : blocks.values())
			totalPairs += (long)blockSet.size() * (blockSet.size() - 1) / 2;
		long donePairs = 0L;
		long reportedPairs = 0L;
		progress.accept(PROFILES_SHARE);

		for(final Set<Profile> blockSet : blocks.values()){
			if(Thread.currentThread().isInterrupted())
				return List.of();

			// Materialize the block as a list for indexed iteration. The set
			// has already removed duplicate entries of the same profile, but
			// the explicit identity guard below is kept as a second line of
			// defense: no code path in this method should ever compare an
			// individual with itself.
			final List<Profile> block = new ArrayList<>(blockSet);
			final int size = block.size();
			for(int i = 0; i < size; i ++){
				for(int j = i + 1; j < size; j ++){
					final Profile a = block.get(i);
					final Profile b = block.get(j);

					// A profile is never equal to itself in a candidate pair.
					// This guard is redundant with the set-based
					// deduplication above, but it protects against future
					// changes to the blocking logic.
					if(a.id().equals(b.id()))
						continue;

					final String pairKey = pairKey(a.id(), b.id());
					if(!seenPairs.add(pairKey))
						continue;
					if(existingHypotheses.contains(pairKey))
						continue;
					final Candidate c = compare(a, b, options);
					if(c != null)
						candidates.add(c);
				}

				//the pairs of this row are done
				donePairs += size - 1 - i;
				if(donePairs - reportedPairs >= PROGRESS_PAIRS || i == size - 1){
					reportedPairs = donePairs;
					progress.accept(PROFILES_SHARE + (1. - PROFILES_SHARE) * donePairs / Math.max(1L, totalPairs));
				}
				if((i & 63) == 0 && Thread.currentThread().isInterrupted())
					return List.of();
			}
		}

		progress.accept(1.);
		candidates.sort(Comparator
			.comparingInt(Candidate::score).reversed()
			.thenComparing(c -> c.left().id())
			.thenComparing(c -> c.right().id()));

		return candidates.size() > options.maxResults
			? List.copyOf(candidates.subList(0, options.maxResults))
			: List.copyOf(candidates);
	}


	/* ======================================================================
	 *                          Profile building
	 * ====================================================================== */

	private static Map<String, Profile> buildProfiles(final FLEFModel model, final DoubleConsumer progress){
		final List<FLEFRecord> individuals = PrivacyService.visible(model, model.getRecordsByType(IndividualReader.TYPE));

		// Index relationships once. Each relationship is stored under
		// both endpoints, so a single pass builds every lookup we need.
		final Map<String, List<FLEFRecord>> relationshipsByEndpoint = new HashMap<>();
		final List<FLEFRecord> relationships = PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE));
		for(final FLEFRecord relationship : relationships){
			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String object = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(subject != null)
				relationshipsByEndpoint.computeIfAbsent(subject, k -> new ArrayList<>())
					.add(relationship);
			if(object != null && !object.equals(subject))
				relationshipsByEndpoint.computeIfAbsent(object, k -> new ArrayList<>())
					.add(relationship);
		}

		// Index participations by participant.
		final Map<String, List<FLEFRecord>> participationsByIndividual = new HashMap<>();
		final List<FLEFRecord> participations = PrivacyService.visible(model, model.getRecordsByType(EventParticipationReader.TYPE));
		for(final FLEFRecord participation : participations){
			final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation, "participant");
			if(participantBlock == null)
				continue;
			final FLEFRecord oneof = participantBlock.getTheOnlyChild();
			if(!IndividualReader.isOfType(oneof))
				continue;
			final FLEFRecord ref = oneof.getTheOnlyChild();
			final String id = (ref != null? ref.getValue(): oneof.getValue());
			if(id != null)
				participationsByIndividual.computeIfAbsent(id, k -> new ArrayList<>())
					.add(participation);
		}

		// Index attributes by individual.
		final Map<String, List<FLEFRecord>> attributesByIndividual = new HashMap<>();
		final List<FLEFRecord> attributes = PrivacyService.visible(model, model.getRecordsByType(IndividualAttributeReader.TYPE));
		for(final FLEFRecord attribute : attributes){
			final String id = FLEFRecordHelper.getChildValue(attribute, IndividualReader.TYPE);
			if(id != null)
				attributesByIndividual.computeIfAbsent(id, k -> new ArrayList<>())
					.add(attribute);
		}

		// Index events by id, so date extraction does not scan the whole
		// event list per participation.
		final Map<String, FLEFRecord> eventsById = new LinkedHashMap<>();
		final List<FLEFRecord> events = PrivacyService.visible(model, model.getRecordsByType(EventReader.TYPE));
		for(final FLEFRecord event : events)
			if(event.getId() != null)
				eventsById.put(event.getId(), event);

		// Build the parent -> children map once, to compute siblings in
		// O(1) per individual.
		final Map<String, Set<String>> childrenByParent = new HashMap<>();
		for(final FLEFRecord rel : relationships){
			final String type = FLEFRecordHelper.getChildValue(rel, "type");
			if(!RelationshipReader.isTypeBiologicalChild(type))
				continue;

			final String child = rel.extractReferencedId("subject", IndividualReader.TYPE);
			final String target = rel.extractReferencedId("target", IndividualReader.TYPE);
			if(child != null && target != null)
				childrenByParent.computeIfAbsent(target, k -> new LinkedHashSet<>())
					.add(child);
		}

		//the names of the individuals and of the places, to compare by name what has no identifier in common
		final Map<String, Set<String>> individualNames = new HashMap<>();
		for(final FLEFRecord individual : individuals)
			if(individual.getId() != null)
				individualNames.put(individual.getId(), extractNames(individual).fullNameVariants());
		final Map<String, Set<String>> placeNames = new HashMap<>();
		for(final FLEFRecord place : PrivacyService.visible(model, model.getRecordsByType(PlaceReader.TYPE)))
			if(place.getId() != null){
				final Set<String> names = new LinkedHashSet<>();
				for(final String name : PlaceReader.extractNames(place)){
					final String normalized = TextSimilarity.normalize(name);
					if(!normalized.isEmpty())
						names.add(normalized);
				}
				placeNames.put(place.getId(), names);
			}

		final Map<String, String> sourceTitles = new HashMap<>();
		for(final FLEFRecord source : SourceHelper.listAllSources(model))
			if(source.getId() != null){
				final String title = TextSimilarity.normalize(SourceHelper.sourceTitle(source));
				if(!title.isEmpty() && !title.equals(TextSimilarity.normalize(source.getId())))
					sourceTitles.put(source.getId(), title);
			}

		final Map<String, Profile> profiles = new LinkedHashMap<>();
		//the indexes above are a tenth of the description of the individuals, each of these is the rest
		progress.accept(0.1);
		int built = 0;
		for(final FLEFRecord individual : individuals){
			if(individual.getId() == null)
				continue;
			profiles.put(individual.getId(), buildProfile(individual,
				relationshipsByEndpoint, participationsByIndividual,
				attributesByIndividual, eventsById, childrenByParent, individualNames, placeNames, sourceTitles));
			built ++;
			if(built % PROGRESS_INDIVIDUALS == 0){
				progress.accept(0.1 + 0.9 * built / individuals.size());
				if(Thread.currentThread().isInterrupted())
					return profiles;
			}
		}
		progress.accept(1.);
		return profiles;
	}

	private static Profile buildProfile(final FLEFRecord individual,
		final Map<String, List<FLEFRecord>> relationshipsByEndpoint,
		final Map<String, List<FLEFRecord>> participationsByIndividual,
		final Map<String, List<FLEFRecord>> attributesByIndividual,
		final Map<String, FLEFRecord> eventsById,
		final Map<String, Set<String>> childrenByParent,
		final Map<String, Set<String>> individualNames,
		final Map<String, Set<String>> placeNamesById,
		final Map<String, String> sourceTitlesById){
		final String id = individual.getId();
		final String sex = FLEFRecordHelper.getChildValue(individual, "sex");
		final NameTokens names = extractNames(individual);
		final String displayName = (names.fullNameVariants.isEmpty()
			? id
			: firstVariant(names.fullNameVariants));

		// Relationships.
		final Set<String> parents = new LinkedHashSet<>();
		final Set<String> children = new LinkedHashSet<>();
		final Set<String> spouses = new LinkedHashSet<>();
		for(final FLEFRecord rel : relationshipsByEndpoint.getOrDefault(id, List.of())){
			final String type = FLEFRecordHelper.getChildValue(rel, "type");
			if(type == null)
				continue;
			final String subject = rel.extractReferencedId("subject", IndividualReader.TYPE);
			final String target = rel.extractReferencedId("target", IndividualReader.TYPE);
			if(subject == null || target == null)
				continue;

			if(RelationshipReader.isTypeBiologicalChild(type)){
				if(id.equals(subject))
					parents.add(target);
				else if(id.equals(target))
					children.add(subject);
			}
			else if(RelationshipReader.isTypePartner(type)){
				if(id.equals(subject))
					spouses.add(target);
				else if(id.equals(target))
					spouses.add(subject);
			}
		}

		// Siblings: children of the same parents, minus self.
		final Set<String> siblings = new LinkedHashSet<>();
		for(final String parent : parents){
			for(final String child : childrenByParent.getOrDefault(parent, Set.of()))
				if(!child.equals(id))
					siblings.add(child);
		}

		// Events.
		final Set<String> eventIds = new LinkedHashSet<>();
		Integer birthYear = null;
		Integer deathYear = null;
		for(final FLEFRecord participation : participationsByIndividual.getOrDefault(id, List.of())){
			final String eventId = FLEFRecordHelper.getChildValue(participation, "event");
			if(eventId == null)
				continue;
			eventIds.add(eventId);
			final FLEFRecord event = eventsById.get(eventId);
			if(event == null)
				continue;
			final String eventType = FLEFRecordHelper.getChildValue(event, "type");
			if(eventType == null)
				continue;
			final String type = eventType.toLowerCase(Locale.ROOT);
			final Integer year = extractYearFromEvent(event);
			if(EventReader.ENUM_TYPE_BIRTH.equals(type) && birthYear == null)
				birthYear = year;
			else if(EventReader.ENUM_TYPE_DEATH.equals(type) && deathYear == null)
				deathYear = year;
		}

		// Attributes.
		final Set<String> attributeKeys = new LinkedHashSet<>();
		final Set<String> occupations = new LinkedHashSet<>();
		final Map<String, Set<String>> attributeTexts = new LinkedHashMap<>();
		for(final FLEFRecord attr : attributesByIndividual.getOrDefault(id, List.of())){
			final String type = FLEFRecordHelper.getChildValue(attr, "type");
			final String value = FLEFRecordHelper.getChildValue(attr, "value");
			if(IndividualAttributeReader.ENUM_TYPE_OCCUPATION.equals(type) && value != null){
				final String occupation = TextSimilarity.normalize(value);
				if(!occupation.isEmpty())
					occupations.add(occupation);
			}
			else if(type != null && value != null){
				final String text = TextSimilarity.normalize(value);
				if(!text.isEmpty())
					attributeTexts.computeIfAbsent(type, k -> new LinkedHashSet<>())
						.add(text);
			}
			if(type != null)
				attributeKeys.add(type + ":" + (value != null? normalize(value): StringUtils.EMPTY));
		}

		// Sources: from the individual itself, from its events, and from
		// its relationships.
		final Set<String> sourceIds = new LinkedHashSet<>();
		collectSources(individual, sourceIds);
		for(final FLEFRecord participation : participationsByIndividual.getOrDefault(id, List.of())){
			final String eventId = FLEFRecordHelper.getChildValue(participation, "event");
			final FLEFRecord event = (eventId != null? eventsById.get(eventId): null);
			if(event != null)
				collectSources(event, sourceIds);
			collectSources(participation, sourceIds);
		}
		for(final FLEFRecord rel : relationshipsByEndpoint.getOrDefault(id, List.of()))
			collectSources(rel, sourceIds);

		// Places: from the events the individual participated in.
		final Set<String> placeIds = new LinkedHashSet<>();
		for(final FLEFRecord participation : participationsByIndividual.getOrDefault(id, List.of())){
			final String eventId = FLEFRecordHelper.getChildValue(participation, "event");
			final FLEFRecord event = (eventId != null? eventsById.get(eventId): null);
			if(event == null)
				continue;
			final FLEFRecord place = FLEFRecordHelper.findChild(event, "place");
			if(place != null){
				final String placeId = FLEFRecordHelper.getChildValue(place, "place");
				if(placeId != null)
					placeIds.add(placeId);
			}
		}

		return new Profile(id, displayName, sex,
			names.givenTokens, names.familyTokens, names.fullNameVariants,
			birthYear, deathYear,
			parents, children, siblings, spouses,
			eventIds, attributeKeys, sourceIds, placeIds,
			namesOf(parents, individualNames), namesOf(spouses, individualNames), placeNamesOf(placeIds, placeNamesById), occupations,
			namesOf(children, individualNames), attributeTexts, titlesOf(sourceIds, sourceTitlesById));
	}

	private static Set<String> namesOf(final Set<String> ids, final Map<String, Set<String>> namesById){
		final Set<String> names = new LinkedHashSet<>();
		for(final String id : ids){
			for(final String name : namesById.getOrDefault(id, Set.of())){
				final String normalized = TextSimilarity.normalize(name);
				if(!normalized.isEmpty())
					names.add(normalized);
			}
		}
		return names;
	}

	private static Set<String> titlesOf(final Set<String> sourceIds, final Map<String, String> titlesById){
		final Set<String> titles = new LinkedHashSet<>();
		for(final String sourceId : sourceIds){
			final String title = titlesById.get(sourceId);
			if(title != null)
				titles.add(title);
		}
		return titles;
	}

	private static Set<String> placeNamesOf(final Set<String> placeIds, final Map<String, Set<String>> namesById){
		final Set<String> names = new LinkedHashSet<>();
		for(final String placeId : placeIds)
			names.addAll(namesById.getOrDefault(placeId, Set.of()));
		return names;
	}


	/* ======================================================================
	 *                          Name extraction
	 * ====================================================================== */

	private record NameTokens(
		Set<String> givenTokens,
		Set<String> familyTokens,
		Set<String> fullNameVariants){}

	/**
	 * Extracts the name tokens from every {@code name} block of the
	 * individual. Each block is a {@code PersonalNameStructure} whose
	 * {@code part} children carry a {@code type} and a {@code value}.
	 * The full-name variant of a block is the concatenation of all its
	 * parts, in order.
	 */
	private static NameTokens extractNames(final FLEFRecord individual){
		final Set<String> given = new LinkedHashSet<>();
		final Set<String> family = new LinkedHashSet<>();
		final Set<String> full = new LinkedHashSet<>();

		for(final FLEFRecord nameBlock : individual.getChildren()){
			if(!"name".equalsIgnoreCase(nameBlock.getTag()))
				continue;
			final StringBuilder fullName = new StringBuilder();
			for(final FLEFRecord part : nameBlock.getChildren()){
				if(!"part".equalsIgnoreCase(part.getTag()))
					continue;
				final String type = FLEFRecordHelper.getChildValue(part, "type");
				final String value = FLEFRecordHelper.getChildValue(part, "value");
				if(StringUtils.isEmpty(value))
					continue;
				final String norm = normalize(value);
				if(fullName.length() > 0)
					fullName.append(' ');
				fullName.append(norm);
				if(type != null){
					final String t = type.toLowerCase(Locale.ROOT);
					if("given".equals(t))
						given.add(norm);
					else if("family".equals(t))
						family.add(norm);
				}
			}
			if(fullName.length() > 0)
				full.add(fullName.toString());
		}
		return new NameTokens(given, family, full);
	}

	private static String firstVariant(final Set<String> variants){
		return variants.iterator().next();
	}

	/**
	 * Normalizes a string for comparison: lower-case, NFD decomposition
	 * with combining marks stripped, punctuation replaced by spaces,
	 * whitespace collapsed.
	 */
	private static String normalize(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		String s = text.toLowerCase(Locale.ROOT);
		s = Normalizer.normalize(s, Normalizer.Form.NFD);
		s = COMBINING_MARKS.matcher(s).replaceAll(StringUtils.EMPTY);
		s = PUNCTUATION_RUN.matcher(s).replaceAll(StringUtils.SPACE);
		s = StringUtils.normalizeSpace(s);
		return s;
	}


	/* ======================================================================
	 *                          Event dates
	 * ====================================================================== */

	/**
	 * Extracts the year from the {@code date} block of an event, or
	 * {@code null} when no year can be determined. The method accepts
	 * the common forms produced by the protocol and by the tools in this
	 * application: a direct value, a {@code full_date}, a {@code decade}
	 * start year, and a {@code century} ordinal.
	 */
	private static Integer extractYearFromEvent(final FLEFRecord event){
		final FLEFRecord date = FLEFRecordHelper.findChild(event, "date");
		if(date == null)
			return null;

		//the date structure of the protocol (point, bounded, spanning; calendar; precision): the normalizer reads all of them
		try{
			final TemporalSpan span = DateNormalizer.normalize(date);
			final NormalizedDate representative = (span != null? span.representativeDate(): null);
			if(representative != null)
				return CalendarType.jdnToProlepticGregorianLocalDate(representative.jdn()).getYear();
		}
		catch(final RuntimeException ignored){
			//a date that cannot be normalized is read as text, as before
		}

		final String raw = findFirstValue(date);
		return extractYear(raw);
	}

	private static String findFirstValue(final FLEFRecord record){
		final String direct = FLEFRecordHelper.getChildValue(record, "value");
		if(direct != null && !direct.isBlank())
			return direct;
		for(final FLEFRecord child : record.getChildren()){
			final String v = findFirstValue(child);
			if(v != null)
				return v;
		}
		return null;
	}

	private static Integer extractYear(final String raw){
		if(StringUtils.isEmpty(raw))
			return null;
		final String s = raw.trim();
		// Accept "1894", "1894-03", "1894-03-01", or "17 JUN 1894".
		final Matcher m = Pattern.compile("(\\d{3,4})").matcher(s);
		if(!m.find())
			return null;
		try{
			return Integer.parseInt(m.group(1));
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}


	/* ======================================================================
	 *                          Sources
	 * ====================================================================== */

	private static void collectSources(final FLEFRecord record, final Set<String> out){
		for(final FLEFRecord child : record.getChildren()){
			if(!"source".equalsIgnoreCase(child.getTag()))
				continue;
			final String sourceId = FLEFRecordHelper.getChildValue(child, "source");
			if(sourceId != null)
				out.add(sourceId);
			else{
				// Nested form: source { source S1, ... }
				final FLEFRecord inner = FLEFRecordHelper.findChild(child, "source");
				if(inner != null){
					final String v = (inner.getTheOnlyChild() != null
						? inner.getTheOnlyChild().getValue(): inner.getValue());
					if(v != null)
						out.add(v);
				}
			}
		}
	}


	/* ======================================================================
	 *                          Blocking
	 * ====================================================================== */

	/**
	 * Builds the blocking index. Each profile is placed under one block
	 * for its given-name prefix and one for its family-name prefix,
	 * together with its sex (only if {@code separateBySex}). Profiles that share neither a given-name
	 * prefix nor a family-name prefix are never compared.
	 * <p>
	 * Blocks are sets, not lists, because a profile can legitimately
	 * produce the same prefix through more than one of its name variants:
	 * for example, {@code Giovanni} and {@code Gianni} both reduce to the
	 * prefix {@code gi}, and the profile would otherwise be added twice to
	 * the same block. The set deduplicates by profile identity, so the
	 * comparison loop never sees the same individual twice in the same
	 * block.
	 */
	private static Map<String, Set<Profile>> buildBlocks(
		final Collection<Profile> profiles, final boolean separateBySex){
		final Map<String, Set<Profile>> blocks = new LinkedHashMap<>();
		for(final Profile p : profiles){
			//the sex is part of the key only when the pairs of different sex are not wanted: otherwise they would never be compared
			final String sex = (!separateBySex? "": (p.sex() != null? p.sex().toLowerCase(Locale.ROOT): "?"));
			for(final String given : p.givenTokens()){
				final String prefix = (given.length() >= 2? given.substring(0, 2): given);
				blocks.computeIfAbsent("g:" + sex + ":" + prefix, k -> new LinkedHashSet<>())
					.add(p);
			}
			for(final String family : p.familyTokens()){
				final String prefix = (family.length() >= 2? family.substring(0, 2): family);
				blocks.computeIfAbsent("f:" + sex + ":" + prefix, k -> new LinkedHashSet<>())
					.add(p);
			}
			//the spellings that differ in the first letter or in the vowels (Catarina, Katarina) meet by the consonants
			for(final String name : p.givenTokens())
				addSkeletonBlock(blocks, "sg:" + sex + ":", name, p);
			for(final String name : p.familyTokens())
				addSkeletonBlock(blocks, "sf:" + sex + ":", name, p);
		}
		return blocks;
	}

	private static void addSkeletonBlock(final Map<String, Set<Profile>> blocks, final String prefix, final String name,
			final Profile profile){
		final String skeleton = NameSimilarity.skeleton(name);
		//a skeleton of one or two letters puts together too many names
		if(skeleton.length() >= 3)
			blocks.computeIfAbsent(prefix + skeleton.substring(0, 3), k -> new LinkedHashSet<>())
				.add(profile);
	}

	private static String pairKey(final String a, final String b){
		return (a.compareTo(b) <= 0? a + "|" + b: b + "|" + a);
	}

	/**
	 * Returns the set of pairs already covered by an identity hypothesis
	 * in the model, so the finder can skip them.
	 */
	private static Set<String> existingHypothesisPairs(final FLEFModel model){
		final Set<String> pairs = new HashSet<>();
		final List<FLEFRecord> identityHypotheses = PrivacyService.visible(model, model.getRecordsByType(IdentityHypothesisReader.TYPE));
		for(final FLEFRecord identityHypothesis : identityHypotheses){
			final List<String> candidates = new ArrayList<>();
			for(final FLEFRecord child : identityHypothesis.getChildren()){
				if(!"identity".equalsIgnoreCase(child.getTag()))
					continue;
				final FLEFRecord oneof = child.getTheOnlyChild();
				if(oneof == null)
					continue;
				final FLEFRecord ref = oneof.getTheOnlyChild();
				final String id = (ref != null? ref.getValue(): oneof.getValue());
				if(id != null)
					candidates.add(id);
			}
			if(candidates.size() >= 2)
				pairs.add(pairKey(candidates.get(0), candidates.get(1)));
		}
		return pairs;
	}


	/* ======================================================================
	 *                          Comparison
	 * ====================================================================== */

	/**
	 * Compares two profiles and returns a candidate when the score
	 * exceeds the threshold, or {@code null} otherwise.
	 */
	private static Candidate compare(final Profile a, final Profile b, final Options options){
		// Hard filter: known and different sex.
		if(options.requireSameSex && a.sex() != null && b.sex() != null
			&& !a.sex().equalsIgnoreCase(b.sex()))
			return null;

		final List<FeatureMatch> matches = new ArrayList<>();
		final List<String> differences = new ArrayList<>();
		int score = 0;

		// Names.
		final int nameScore = scoreNames(a, b, matches, differences);
		score += nameScore;

		// Parents.
		if(!a.parentIds().isEmpty() && !b.parentIds().isEmpty()){
			final int common = intersectionSize(a.parentIds(), b.parentIds());
			if(common > 0){
				final int c = (common == a.parentIds().size() && common == b.parentIds().size()
					? W_PARENTS_ALL: W_PARENTS_SOME);
				score += c;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.parents"), c,
					I18N.tf("text.duplicates.detail.parents", common, compact(a.parentIds()), compact(b.parentIds()))));
			}
			else{
				//no parent in common: two imports of the same tree have parents with the same name, not the same record
				final String[] pair = agreeingNames(a.parentNames(), b.parentNames(), NameKind.PERSON);
				if(pair != null){
					score += W_PARENTS_NAMES;
					matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.parents_names"), W_PARENTS_NAMES,
						I18N.tf("text.duplicates.detail.similar_names", pair[0], pair[1])));
				}
				else
					differences.add(I18N.t("text.duplicates.diff.parents"));
			}
		}

		// Children.
		if(!a.childIds().isEmpty() && !b.childIds().isEmpty()){
			final int common = intersectionSize(a.childIds(), b.childIds());
			if(common > 0){
				score += W_CHILDREN_SOME;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.children"), W_CHILDREN_SOME,
					I18N.tf("text.duplicates.detail.shared", common)));
			}
			else{
				final String[] pair = agreeingNames(a.childNames(), b.childNames(), NameKind.PERSON);
				if(pair != null){
					score += W_CHILDREN_NAMES;
					matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.children_names"), W_CHILDREN_NAMES,
						I18N.tf("text.duplicates.detail.similar_names", pair[0], pair[1])));
				}
				else
					differences.add(I18N.t("text.duplicates.diff.children"));
			}
		}

		// Spouses.
		if(!a.spouseIds().isEmpty() && !b.spouseIds().isEmpty()){
			final int common = intersectionSize(a.spouseIds(), b.spouseIds());
			if(common > 0){
				score += W_SPOUSES_SOME;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.spouses"), W_SPOUSES_SOME,
					I18N.tf("text.duplicates.detail.shared", common)));
			}
			else{
				final String[] pair = agreeingNames(a.spouseNames(), b.spouseNames(), NameKind.PERSON);
				if(pair != null){
					score += W_SPOUSES_NAMES;
					matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.spouses_names"), W_SPOUSES_NAMES,
						I18N.tf("text.duplicates.detail.similar_names", pair[0], pair[1])));
				}
				else
					differences.add(I18N.t("text.duplicates.diff.spouses"));
			}
		}

		// Siblings.
		if(!a.siblingIds().isEmpty() && !b.siblingIds().isEmpty()){
			final int common = intersectionSize(a.siblingIds(), b.siblingIds());
			if(common > 0){
				score += W_SIBLINGS_SOME;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.siblings"), W_SIBLINGS_SOME,
					I18N.tf("text.duplicates.detail.shared", common)));
			}
		}

		// Birth year.
		if(a.birthYear() != null && b.birthYear() != null){
			final int diff = Math.abs(a.birthYear() - b.birthYear());
			if(diff <= YEAR_TOLERANCE){
				score += W_BIRTH_NEAR;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.birth_year"), W_BIRTH_NEAR,
					a.birthYear() + " / " + b.birthYear() + " (Δ " + diff + ")"));
			}
			else
				differences.add(I18N.tf("text.duplicates.diff.birth_year", a.birthYear(), b.birthYear()));
		}

		// Death year.
		if(a.deathYear() != null && b.deathYear() != null){
			final int diff = Math.abs(a.deathYear() - b.deathYear());
			if(diff <= YEAR_TOLERANCE){
				score += W_DEATH_NEAR;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.death_year"), W_DEATH_NEAR,
					a.deathYear() + " / " + b.deathYear() + " (Δ " + diff + ")"));
			}
		}

		// Events, attributes, sources, places.
		score += overlapBonus(a.eventIds(), b.eventIds(), W_EVENT_OVERLAP,
			I18N.t("text.duplicates.feature.events"), matches);
		score += overlapBonus(a.attributeKeys(), b.attributeKeys(), W_ATTRIBUTE_OVERLAP,
			I18N.t("text.duplicates.feature.attributes"), matches);
		score += overlapBonus(a.sourceIds(), b.sourceIds(), W_SOURCE_OVERLAP,
			I18N.t("text.duplicates.feature.sources"), matches);
		final int placesInCommon = overlapBonus(a.placeIds(), b.placeIds(), W_PLACE_OVERLAP,
			I18N.t("text.duplicates.feature.places"), matches);
		score += placesInCommon;

		// Second pass, by name: places and occupations that are the same words in different records.
		if(placesInCommon == 0){
			final String[] pair = agreeingNames(a.placeNames(), b.placeNames(), NameKind.PLACE);
			if(pair != null){
				score += W_PLACES_NAMES;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.places_names"), W_PLACES_NAMES,
					I18N.tf("text.duplicates.detail.similar_names", pair[0], pair[1])));
			}
		}
		final String[] attributeText = agreeingAttributeTexts(a.attributeTexts(), b.attributeTexts());
		if(attributeText != null){
			score += W_ATTRIBUTE_TEXTS;
			matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.attribute_texts"), W_ATTRIBUTE_TEXTS,
				I18N.tf("text.duplicates.detail.similar_attributes", attributeText[0], attributeText[1], attributeText[2])));
		}
		if(!matches.stream().anyMatch(match -> match.feature().equals(I18N.t("text.duplicates.feature.sources")))){
			final String[] title = agreeingNames(a.sourceTitles(), b.sourceTitles(), NameKind.TITLE);
			if(title != null){
				score += W_SOURCE_TITLES;
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.source_titles"), W_SOURCE_TITLES,
					I18N.tf("text.duplicates.detail.similar_names", title[0], title[1])));
			}
		}
		final String[] occupations = agreeingNames(a.occupations(), b.occupations(), NameKind.TEXT);
		if(occupations != null){
			score += W_OCCUPATIONS;
			matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.occupations"), W_OCCUPATIONS,
				I18N.tf("text.duplicates.detail.similar_names", occupations[0], occupations[1])));
		}

		if(score < options.threshold)
			return null;

		return new Candidate(a, b, score, matches, differences);
	}

	private static int scoreNames(final Profile a, final Profile b,
		final List<FeatureMatch> matches, final List<String> differences){
		if(!a.fullNameVariants().isEmpty() && !b.fullNameVariants().isEmpty()){
			final Set<String> intersection = new HashSet<>(a.fullNameVariants());
			intersection.retainAll(b.fullNameVariants());
			if(!intersection.isEmpty()){
				matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.name"), W_NAME_FULL,
					I18N.tf("text.duplicates.detail.exact", intersection.iterator().next())));
				return W_NAME_FULL;
			}
		}
		final boolean givenShared = !Collections.disjoint(a.givenTokens(), b.givenTokens());
		final boolean familyShared = !Collections.disjoint(a.familyTokens(), b.familyTokens());
		if(givenShared && familyShared){
			matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.name"), W_NAME_PARTIAL,
				I18N.t("text.duplicates.detail.given_family")));
			return W_NAME_PARTIAL;
		}

		//what is not the same name may be the same name spelled in another way: the similarity of the letters decides, with no list of names
		final NameSimilarity.Match given = NameSimilarity.best(a.givenTokens(), b.givenTokens());
		NameSimilarity.Match family = NameSimilarity.best(a.familyTokens(), b.familyTokens());
		//an initial is a given name: a family name that is only an initial says nothing
		if(family.level() == NameSimilarity.Level.INITIAL)
			family = new NameSimilarity.Match(NameSimilarity.Level.NONE, null, null);
		if(given.agrees() && family.agrees()){
			final boolean initial = (given.level() == NameSimilarity.Level.INITIAL);
			final int weight = (initial? W_NAME_INITIAL: W_NAME_SIMILAR);
			matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.name"), weight,
				I18N.tf("text.duplicates.detail.similar_given_family", describe(given), describe(family))));
			return weight;
		}
		if(givenShared || familyShared){
			matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.name"), W_NAME_TOKEN,
				I18N.t(givenShared? "text.duplicates.detail.given": "text.duplicates.detail.family")));
			return W_NAME_TOKEN;
		}
		final NameSimilarity.Match one = (family.level() == NameSimilarity.Level.SIMILAR? family
			: (given.level() == NameSimilarity.Level.SIMILAR? given: null));
		if(one != null){
			matches.add(new FeatureMatch(I18N.t("text.duplicates.feature.name"), W_NAME_TOKEN_SIMILAR,
				I18N.tf("text.duplicates.detail.similar_token", describe(one))));
			return W_NAME_TOKEN_SIMILAR;
		}
		differences.add(I18N.t("text.duplicates.diff.no_name"));
		return 0;
	}

	/** @return	The two names of a match, as they are shown ({@code giovanni / giovani}). */
	private static String describe(final NameSimilarity.Match match){
		return (match.first().equals(match.second())? match.first(): match.first() + " / " + match.second());
	}

	private static int overlapBonus(final Set<String> a, final Set<String> b,
		final int weight, final String feature, final List<FeatureMatch> matches){
		if(a.isEmpty() || b.isEmpty())
			return 0;
		final int common = intersectionSize(a, b);
		if(common <= 0)
			return 0;
		matches.add(new FeatureMatch(feature, weight, I18N.tf("text.duplicates.detail.shared", common)));
		return weight;
	}

	/**
	 * Looks for an attribute type that both have, with a text of the first that agrees with a text of the second.
	 *
	 * @return	{type, text of the first, text of the second}, {@code null} if none agrees.
	 */
	public static String[] agreeingAttributeTexts(final Map<String, Set<String>> first, final Map<String, Set<String>> second){
		for(final Map.Entry<String, Set<String>> entry : first.entrySet()){
			final Set<String> other = second.get(entry.getKey());
			if(other == null)
				continue;

			final String[] pair = agreeingNames(entry.getValue(), other, NameKind.TEXT);
			if(pair != null)
				return new String[]{entry.getKey(), pair[0], pair[1]};
		}
		return null;
	}

	/** What the texts compared by name are: the rules of agreement differ. */
	public enum NameKind{
		/** Names of people: a name with more words agrees only if the shorter one has at least two ("Giovanni" is not "Giovanni Rossi"). */
		PERSON,
		/** Names of places: a name with more words is another place ("San Martino" is not "San Martino di Lupari"). */
		PLACE,
		/** Free text, such as an occupation: it agrees if the words of one are among the words of the other. */
		TEXT,
		/** Titles of works: they agree if they are equal or a spelling variant, never because one is part of the other. */
		TITLE
	}

	/**
	 * Looks for a name of the first group that agrees with a name of the second: equal, a spelling variant, or (by the rules of the kind)
	 * one contained in the other. The texts are already normalized.
	 *
	 * @return	The two names that agree (the equal ones first), {@code null} if none does or a group is empty.
	 */
	public static String[] agreeingNames(final Set<String> first, final Set<String> second, final NameKind kind){
		String[] found = null;
		for(final String a : first)
			for(final String b : second){
				final TextSimilarity.Agreement agreement = (kind == NameKind.PLACE
					? TextSimilarity.comparePlaceNormalized(a, b)
					: TextSimilarity.compareNormalized(a, b));
				if(agreement == null || agreement == TextSimilarity.Agreement.DIFFERENT)
					continue;
				if(agreement == TextSimilarity.Agreement.EXACT)
					return new String[]{a, b};
				if(kind == NameKind.TITLE && agreement == TextSimilarity.Agreement.SUBSET)
					continue;
				if(found == null && agreement == TextSimilarity.Agreement.SUBSET && kind == NameKind.PERSON
						&& Math.min(StringUtils.split(a).length, StringUtils.split(b).length) < 2)
					continue;
				if(found == null)
					found = new String[]{a, b};
			}
		return found;
	}

	private static int intersectionSize(final Set<String> a, final Set<String> b){
		final Set<String> smaller = (a.size() <= b.size()? a: b);
		final Set<String> larger = (a.size() <= b.size()? b: a);
		int n = 0;
		for(final String s : smaller)
			if(larger.contains(s))
				n ++;
		return n;
	}

	private static String compact(final Set<String> ids){
		if(ids.isEmpty())
			return "-";
		final StringBuilder sb = new StringBuilder();
		int i = 0;
		for(final String id : ids){
			if(i >= 3){
				sb.append(", …");
				break;
			}
			if(i > 0)
				sb.append(", ");
			sb.append(id);
			i ++;
		}
		return sb.toString();
	}

}
