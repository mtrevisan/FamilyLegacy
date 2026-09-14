package io.github.mtrevisan.familylegacy.v2.ui.tools.duplicates;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.text.Normalizer;
import java.util.ArrayList;
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

	public static final int DEFAULT_THRESHOLD = 55;
	public static final int DEFAULT_MAX_RESULTS = 500;

	/** Years of tolerance when comparing birth or death years. */
	private static final int YEAR_TOLERANCE = 2;

	// Feature weights. They sum to more than 100 on purpose, because a
	// strong match on one dimension should be able to overcome a weak
	// match on another. The threshold is calibrated against real data.
	private static final int W_NAME_FULL = 40;
	private static final int W_NAME_PARTIAL = 25;
	private static final int W_NAME_TOKEN = 10;
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

	private static final List<String> SPOUSE_TYPES = List.of(
		"civil_spouse", "religious_spouse", "customary_spouse",
		"cohabiting_partner", "engaged_partner");


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
		Set<String> placeIds){}

	/** One matched feature, with its contribution to the total score. */
	public record FeatureMatch(String feature, int score, String detail){}

	/** One candidate pair, with the score and the matched/different features. */
	public record Candidate(
		Profile left,
		Profile right,
		int score,
		List<FeatureMatch> matches,
		List<String> differences){}


	private DuplicateFinderService(){
	}


	/**
	 * Finds duplicate candidates in the model.
	 *
	 * @param model   the model; must not be {@code null}
	 * @param options the search parameters; must not be {@code null}
	 * @return the candidate list, sorted by descending score; never
	 *         {@code null}
	 */
	public static List<Candidate> find(final FLEFModel model, final Options options){
		final Map<String, Profile> profiles = buildProfiles(model);
		if(profiles.size() < 2)
			return List.of();

		final Set<String> existingHypotheses = (options.skipExistingHypotheses
			? existingHypothesisPairs(model)
			: Set.of());

		final Map<String, Set<Profile>> blocks = buildBlocks(profiles.values());
		final Set<String> seenPairs = new HashSet<>();
		final List<Candidate> candidates = new ArrayList<>();

		for(final Set<Profile> blockSet : blocks.values()){
			// Materialize the block as a list for indexed iteration. The set
			// has already removed duplicate entries of the same profile, but
			// the explicit identity guard below is kept as a second line of
			// defense: no code path in this method should ever compare an
			// individual with itself.
			final List<Profile> block = new ArrayList<>(blockSet);
			final int size = block.size();
			for(int i = 0; i < size; i++){
				for(int j = i + 1; j < size; j++){
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
			}
		}

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

	private static Map<String, Profile> buildProfiles(final FLEFModel model){
		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualHandler.TYPE);

		// Index relationships once. Each relationship is stored under
		// both endpoints, so a single pass builds every lookup we need.
		final Map<String, List<FLEFRecord>> relationshipsByEndpoint = new HashMap<>();
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String subject = rel.extractReferencedId("subject", IndividualHandler.TYPE);
			final String target = rel.extractReferencedId("target", IndividualHandler.TYPE);
			if(subject != null)
				relationshipsByEndpoint.computeIfAbsent(subject, k -> new ArrayList<>()).add(rel);
			if(target != null && !target.equals(subject))
				relationshipsByEndpoint.computeIfAbsent(target, k -> new ArrayList<>()).add(rel);
		}

		// Index participations by participant.
		final Map<String, List<FLEFRecord>> participationsByIndividual = new HashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation, "participant");
			if(participantBlock == null)
				continue;
			final FLEFRecord oneof = participantBlock.getTheOnlyChild();
			if(oneof == null || !IndividualHandler.TYPE.equalsIgnoreCase(oneof.getTag()))
				continue;
			final FLEFRecord ref = oneof.getTheOnlyChild();
			final String id = (ref != null? ref.getValue(): oneof.getValue());
			if(id != null)
				participationsByIndividual.computeIfAbsent(id, k -> new ArrayList<>()).add(participation);
		}

		// Index attributes by individual.
		final Map<String, List<FLEFRecord>> attributesByIndividual = new HashMap<>();
		for(final FLEFRecord attr : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String id = FLEFRecordHelper.getChildValue(attr, IndividualHandler.TYPE);
			if(id != null)
				attributesByIndividual.computeIfAbsent(id, k -> new ArrayList<>()).add(attr);
		}

		// Index events by id, so date extraction does not scan the whole
		// event list per participation.
		final Map<String, FLEFRecord> eventsById = new LinkedHashMap<>();
		for(final FLEFRecord event : model.getRecordsByType(EventHandler.TYPE))
			if(event.getId() != null)
				eventsById.put(event.getId(), event);

		// Build the parent -> children map once, to compute siblings in
		// O(1) per individual.
		final Map<String, Set<String>> childrenByParent = new HashMap<>();
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(rel, "type");
			if(type == null || !"biological_child".equalsIgnoreCase(type))
				continue;
			final String child = rel.extractReferencedId("subject", IndividualHandler.TYPE);
			final String parent = rel.extractReferencedId("target", IndividualHandler.TYPE);
			if(child != null && parent != null)
				childrenByParent.computeIfAbsent(parent, k -> new LinkedHashSet<>()).add(child);
		}

		final Map<String, Profile> profiles = new LinkedHashMap<>();
		for(final FLEFRecord individual : individuals){
			if(individual.getId() == null)
				continue;
			profiles.put(individual.getId(), buildProfile(individual,
				relationshipsByEndpoint, participationsByIndividual,
				attributesByIndividual, eventsById, childrenByParent));
		}
		return profiles;
	}

	private static Profile buildProfile(final FLEFRecord individual,
		final Map<String, List<FLEFRecord>> relationshipsByEndpoint,
		final Map<String, List<FLEFRecord>> participationsByIndividual,
		final Map<String, List<FLEFRecord>> attributesByIndividual,
		final Map<String, FLEFRecord> eventsById,
		final Map<String, Set<String>> childrenByParent){
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
			final String subject = rel.extractReferencedId("subject", IndividualHandler.TYPE);
			final String target = rel.extractReferencedId("target", IndividualHandler.TYPE);
			if(subject == null || target == null)
				continue;

			if("biological_child".equalsIgnoreCase(type)){
				if(id.equals(subject))
					parents.add(target);
				else if(id.equals(target))
					children.add(subject);
			}
			else if(SPOUSE_TYPES.contains(type.toLowerCase(Locale.ROOT))){
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
			if("birth".equals(type) && birthYear == null)
				birthYear = year;
			else if("death".equals(type) && deathYear == null)
				deathYear = year;
		}

		// Attributes.
		final Set<String> attributeKeys = new LinkedHashSet<>();
		for(final FLEFRecord attr : attributesByIndividual.getOrDefault(id, List.of())){
			final String type = FLEFRecordHelper.getChildValue(attr, "type");
			final String value = FLEFRecordHelper.getChildValue(attr, "value");
			if(type != null)
				attributeKeys.add(type + ":" + (value != null? normalize(value): ""));
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
			eventIds, attributeKeys, sourceIds, placeIds);
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
				if(value == null || value.isBlank())
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
			return "";
		String s = text.toLowerCase(Locale.ROOT);
		s = Normalizer.normalize(s, Normalizer.Form.NFD);
		s = s.replaceAll("\\p{M}+", "");
		s = s.replaceAll("[\\p{Punct}]+", " ");
		s = s.replaceAll("\\s+", " ").trim();
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
		if(raw == null || raw.isBlank())
			return null;
		final String s = raw.trim();
		// Accept "1894", "1894-03", "1894-03-01", or "17 JUN 1894".
		final java.util.regex.Matcher m = java.util.regex.Pattern
			.compile("(\\d{3,4})").matcher(s);
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
	 * together with its sex. Profiles that share neither a given-name
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
		final java.util.Collection<Profile> profiles){
		final Map<String, Set<Profile>> blocks = new LinkedHashMap<>();
		for(final Profile p : profiles){
			final String sex = (p.sex() != null? p.sex().toLowerCase(Locale.ROOT): "?");
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
		}
		return blocks;
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
		for(final FLEFRecord h : model.getRecordsByType(IdentityHypothesisHandler.TYPE)){
			final List<String> candidates = new ArrayList<>();
			for(final FLEFRecord child : h.getChildren()){
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
				matches.add(new FeatureMatch("Parents", c,
					common + " shared (" + compact(a.parentIds()) + " / " + compact(b.parentIds()) + ")"));
			}
			else
				differences.add("different parents");
		}

		// Children.
		if(!a.childIds().isEmpty() && !b.childIds().isEmpty()){
			final int common = intersectionSize(a.childIds(), b.childIds());
			if(common > 0){
				score += W_CHILDREN_SOME;
				matches.add(new FeatureMatch("Children", W_CHILDREN_SOME,
					common + " shared"));
			}
			else
				differences.add("different children");
		}

		// Spouses.
		if(!a.spouseIds().isEmpty() && !b.spouseIds().isEmpty()){
			final int common = intersectionSize(a.spouseIds(), b.spouseIds());
			if(common > 0){
				score += W_SPOUSES_SOME;
				matches.add(new FeatureMatch("Spouses", W_SPOUSES_SOME,
					common + " shared"));
			}
			else
				differences.add("different spouses");
		}

		// Siblings.
		if(!a.siblingIds().isEmpty() && !b.siblingIds().isEmpty()){
			final int common = intersectionSize(a.siblingIds(), b.siblingIds());
			if(common > 0){
				score += W_SIBLINGS_SOME;
				matches.add(new FeatureMatch("Siblings", W_SIBLINGS_SOME,
					common + " shared"));
			}
		}

		// Birth year.
		if(a.birthYear() != null && b.birthYear() != null){
			final int diff = Math.abs(a.birthYear() - b.birthYear());
			if(diff <= YEAR_TOLERANCE){
				score += W_BIRTH_NEAR;
				matches.add(new FeatureMatch("Birth year", W_BIRTH_NEAR,
					a.birthYear() + " / " + b.birthYear() + " (Δ " + diff + ")"));
			}
			else
				differences.add("birth year " + a.birthYear() + " vs " + b.birthYear());
		}

		// Death year.
		if(a.deathYear() != null && b.deathYear() != null){
			final int diff = Math.abs(a.deathYear() - b.deathYear());
			if(diff <= YEAR_TOLERANCE){
				score += W_DEATH_NEAR;
				matches.add(new FeatureMatch("Death year", W_DEATH_NEAR,
					a.deathYear() + " / " + b.deathYear() + " (Δ " + diff + ")"));
			}
		}

		// Events, attributes, sources, places.
		score += overlapBonus(a.eventIds(), b.eventIds(), W_EVENT_OVERLAP,
			"Events", matches);
		score += overlapBonus(a.attributeKeys(), b.attributeKeys(), W_ATTRIBUTE_OVERLAP,
			"Attributes", matches);
		score += overlapBonus(a.sourceIds(), b.sourceIds(), W_SOURCE_OVERLAP,
			"Sources", matches);
		score += overlapBonus(a.placeIds(), b.placeIds(), W_PLACE_OVERLAP,
			"Places", matches);

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
				matches.add(new FeatureMatch("Name", W_NAME_FULL,
					"exact match: " + intersection.iterator().next()));
				return W_NAME_FULL;
			}
		}
		final boolean givenShared = !Collections.disjoint(a.givenTokens(), b.givenTokens());
		final boolean familyShared = !Collections.disjoint(a.familyTokens(), b.familyTokens());
		if(givenShared && familyShared){
			matches.add(new FeatureMatch("Name", W_NAME_PARTIAL,
				"shared given and family tokens"));
			return W_NAME_PARTIAL;
		}
		if(givenShared || familyShared){
			matches.add(new FeatureMatch("Name", W_NAME_TOKEN,
				givenShared? "shared given token": "shared family token"));
			return W_NAME_TOKEN;
		}
		differences.add("no shared name token");
		return 0;
	}

	private static int overlapBonus(final Set<String> a, final Set<String> b,
		final int weight, final String feature, final List<FeatureMatch> matches){
		if(a.isEmpty() || b.isEmpty())
			return 0;
		final int common = intersectionSize(a, b);
		if(common <= 0)
			return 0;
		matches.add(new FeatureMatch(feature, weight, common + " shared"));
		return weight;
	}

	private static int intersectionSize(final Set<String> a, final Set<String> b){
		final Set<String> smaller = (a.size() <= b.size()? a: b);
		final Set<String> larger = (a.size() <= b.size()? b: a);
		int n = 0;
		for(final String s : smaller)
			if(larger.contains(s))
				n++;
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
			i++;
		}
		return sb.toString();
	}

}
