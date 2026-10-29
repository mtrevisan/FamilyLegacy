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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.merger.TextSimilarity.Agreement;
import io.github.mtrevisan.familylegacy.io.model.readers.GeoCoordinate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Compares two records of the same kind and tells how likely they describe the same real-world entity.
 * <p>
 * Only the kinds that can be the subject of an IdentityHypothesisRecord are compared (see {@link #MATCHABLE_KINDS}): the other kinds
 * are assertions about those entities, and are never merged on likeness (see {@link AssertionDeduplicator}).
 * <p>
 * The weights are tuned on genealogical common sense rather than estimated from data: they are meant to keep namesakes apart (a
 * father and a son, or cousins named after the same grandfather, share names and often place and decade) and to merge two copies
 * of the same research.
 */
final class EntityMatcher{

	/** Kinds that can be merged, in the order they are processed: a kind is compared after the kinds its comparison relies on. */
	static final List<String> MATCHABLE_KINDS = List.of("repository", "media", "source", "place", "cultural_norm",
		"historic_event", ModelIndex.KIND_INDIVIDUAL, ModelIndex.KIND_GROUP);
	/** Kinds compared also through the records related to them, which are merged in the same phase. */
	static final List<String> RELATIONAL_KINDS = List.of(ModelIndex.KIND_INDIVIDUAL, ModelIndex.KIND_GROUP, ModelIndex.KIND_PLACE,
		ModelIndex.KIND_SOURCE);

	private static final Set<String> BIRTH_TYPES = Set.of("birth");
	private static final Set<String> BIRTH_PROXY_TYPES = Set.of("baptism", "christening");
	private static final Set<String> DEATH_TYPES = Set.of("death");
	private static final Set<String> DEATH_PROXY_TYPES = Set.of("burial", "cremation");
	/** Events compared on their own: the others are compared only as being the same event. */
	private static final Set<String> OWN_EVENT_TYPES = Set.of("birth", "baptism", "christening", "death", "burial", "cremation");

	private static final String TAG_NAME = "name";
	private static final String TAG_PART = "part";
	private static final String TAG_VALUE = "value";
	private static final String TAG_VARIANT = "variant";
	private static final String TAG_TITLE = "title";
	private static final String TAG_AUTHOR = "author";
	private static final String TAG_PUBLISHER = "publisher";
	private static final String TAG_MEDIA_TYPE = "media_type";
	private static final String TAG_REPOSITORY = "repository";
	private static final String TAG_MEDIA = "media";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_CONTACT = "contact";
	private static final String TAG_LOCATION = "location";
	private static final String TAG_COORDINATES = "coordinates";
	private static final String TAG_URI = "uri";
	private static final String TAG_KIND = "kind";
	private static final String TAG_SEX = "sex";
	private static final String AGENT_TEXT = "text";
	private static final String PART_GIVEN = "given";
	private static final String PART_FAMILY = "family";
	private static final Set<String> KNOWN_SEXES = Set.of("male", "female");

	private static final double DAYS_PER_YEAR = 365.2425;
	private static final double MAX_LIFESPAN_YEARS = 120.;
	/** JDN of 1 January 1 CE (proleptic Gregorian), to turn a JDN into an approximate year for blocking. */
	private static final double JDN_YEAR_ONE = 1_721_426.;

	// prior offsets: the weight at which a pair of the kind has even odds of being a match
	private static final double PRIOR_INDIVIDUAL = 9.;
	private static final double PRIOR_GROUP = 5.;
	private static final double PRIOR_PLACE = 4.5;
	private static final double PRIOR_SOURCE = 4.;
	private static final double PRIOR_REPOSITORY = 3.5;
	private static final double PRIOR_MEDIA = 0.;
	private static final double PRIOR_CONTEXT = 4.;


	private final ModelIndex index;
	private final RecordSignature signature;
	/** Whether the compared records come from two different files. */
	private final boolean crossFile;
	private final Map<String, IndividualFeatures> individualFeatures = new HashMap<>();


	EntityMatcher(final ModelIndex index, final RecordSignature signature, final boolean crossFile){
		this.index = index;
		this.signature = signature;
		this.crossFile = crossFile;
	}


	/* ======================================================================
	 *                          Blocking
	 * ====================================================================== */

	/**
	 * Keys of the blocks a record belongs to: only records sharing a key are compared. A record without keys is never compared.
	 */
	Set<String> blockingKeys(final FLEFRecord record){
		final Set<String> keys = new LinkedHashSet<>();
		switch(record.getTag()){
			case ModelIndex.KIND_INDIVIDUAL -> {
				final IndividualFeatures features = individual(record.getId());
				for(final String given : features.givens){
					for(final String family : features.families)
						keys.add("f:" + TextSimilarity.prefix(family, 3) + "|" + TextSimilarity.prefix(given, 1));
					if(features.families.isEmpty())
						keys.add("g:" + TextSimilarity.prefix(given, 4));
					for(final TemporalSpan birth : features.births)
						keys.add("b:" + TextSimilarity.prefix(given, 3) + "|" + decade(birth));
				}
			}
			case ModelIndex.KIND_GROUP -> {
				for(final String member : index.membersOf(record.getId()))
					keys.add("m:" + member);
				for(final String name : names(record, TAG_NAME))
					keys.add("n:" + TextSimilarity.prefix(name, 4));
			}
			case "media" -> {
				final String uri = ModelIndex.value(record, TAG_URI);
				if(uri != null)
					keys.add("u:" + uri.strip().toLowerCase(Locale.ROOT));
			}
			case "source" -> {
				for(final String title : names(record, TAG_TITLE))
					keys.add("t:" + TextSimilarity.prefix(title, 12));
				// sources titled differently are compared when they share a scan, a call number, or a cited entry
				for(final String media : values(record, TAG_MEDIA))
					keys.add("m:" + media);
				for(final Map.Entry<String, Set<String>> callNumbers : callNumbers(record).entrySet())
					for(final String locator : callNumbers.getValue())
						keys.add("r:" + callNumbers.getKey() + "|" + locator);
				for(final String usage : index.locatedUsagesOf(record.getId()))
					keys.add("c:" + usage);
			}
			case ModelIndex.KIND_PLACE -> {
				for(final String leaf : placeNames(record).leaves())
					keys.add("n:" + TextSimilarity.prefix(leaf, 4));
				for(final String usage : index.usagesOf(record.getId()))
					keys.add("u:" + usage);
			}
			case "cultural_norm", "historic_event" -> {
				final String title = TextSimilarity.normalize(ModelIndex.value(record, TAG_TITLE));
				if(!title.isEmpty())
					keys.add("t:" + TextSimilarity.prefix(title, 4));
				final String type = ModelIndex.value(record, ModelIndex.TAG_TYPE);
				if(type != null)
					keys.add("y:" + type);
			}
			default -> {
				for(final String name : names(record, TAG_NAME))
					keys.add("n:" + TextSimilarity.prefix(name, 4));
			}
		}
		return keys;
	}

	private static String decade(final TemporalSpan span){
		final NormalizedDate date = span.representativeDate();
		if(date == null || date.jdn() == Long.MIN_VALUE || date.jdn() == Long.MAX_VALUE)
			return "?";
		return Long.toString((long)Math.floor(((date.jdn() - JDN_YEAR_ONE) / DAYS_PER_YEAR + 1.) / 10.));
	}


	/* ======================================================================
	 *                          Comparison
	 * ====================================================================== */

	MatchScore compare(final FLEFRecord a, final FLEFRecord b){
		// the researcher's judgment prevails over any likeness
		if(index.isIdentityDisproven(a.getId(), b.getId()))
			return new MatchScore.Builder(0.)
				.veto("identity disproven by a conclusion")
				.build();

		return switch(a.getTag()){
			case ModelIndex.KIND_INDIVIDUAL -> compareIndividuals(a, b);
			case ModelIndex.KIND_GROUP -> compareGroups(a, b);
			case ModelIndex.KIND_PLACE -> comparePlaces(a, b);
			case "source" -> compareSources(a, b);
			case "repository" -> compareRepositories(a, b);
			case "media" -> compareMedia(a, b);
			case "cultural_norm", "historic_event" -> compareContexts(a, b);
			default -> new MatchScore.Builder(0.)
				.veto("kind '" + a.getTag() + "' is not an identity candidate")
				.build();
		};
	}

	private MatchScore compareIndividuals(final FLEFRecord a, final FLEFRecord b){
		final IndividualFeatures fa = individual(a.getId());
		final IndividualFeatures fb = individual(b.getId());
		final MatchScore.Builder score = new MatchScore.Builder(PRIOR_INDIVIDUAL);

		final String sexA = ModelIndex.value(a, TAG_SEX);
		final String sexB = ModelIndex.value(b, TAG_SEX);
		if(sexA != null && sexB != null && KNOWN_SEXES.contains(sexA) && KNOWN_SEXES.contains(sexB)){
			if(!sexA.equals(sexB))
				return score.veto("opposite sexes")
					.build();
			score.add(0.5, "same sex");
		}

		addAgreement(score, TextSimilarity.best(fa.givens, fb.givens), "given name", 4., 2.5, 1.5, -4.);
		addAgreement(score, TextSimilarity.best(fa.families, fb.families), "family name", 3.5, 2., 1.5, -3.);

		addDates(score, fa.births, fb.births, fa.birthIsProxy || fb.birthIsProxy, "birth", 6., 3.);
		addDates(score, fa.deaths, fb.deaths, fa.deathIsProxy || fb.deathIsProxy, "death", 5., 3.);
		checkLifespan(score, fa, fb);
		if(score.isVetoed())
			return score.build();

		addPlaces(score, fa.birthPlaces, fb.birthPlaces, "birth place");

		final Relatives parents = matchRelatives(index.parentsOf(a.getId()), index.parentsOf(b.getId()));
		if(parents != null){
			if(parents.matched() == 0)
				score.add(-3., "parents differ");
			else
				score.add((parents.matched() >= 2? 5.: 3.) + 2. * parents.sameRecord(),
					(parents.matched() >= 2? "both parents agree": "a parent agrees")
						+ (parents.sameRecord() > 0? " (merged records)": StringUtils.EMPTY));
		}
		final Relatives spouses = matchRelatives(index.spousesOf(a.getId()), index.spousesOf(b.getId()));
		if(spouses != null){
			if(spouses.matched() == 0)
				// remarriages make different spouses weak evidence
				score.add(-1., "spouses differ");
			else
				score.add(spouses.sameRecord() > 0? 6.: 3., "a spouse agrees" + (spouses.sameRecord() > 0? " (merged record)": StringUtils.EMPTY));
		}
		final Relatives children = matchRelatives(index.childrenOf(a.getId()), index.childrenOf(b.getId()));
		// a record often lists only some of the children: different children are no evidence against
		if(children != null && children.matched() > 0)
			score.add(children.sameRecord() > 0? 6.: 3., "a child agrees" + (children.sameRecord() > 0? " (merged record)": StringUtils.EMPTY));

		addNeighborhood(score, fa, fb);

		return score.build();
	}

	/**
	 * Records about the individual as their subject that state nearly the same thing are evidence of the identity: the same entry of
	 * the same source cited for both, the same attributes, the same other events. Their absence is no evidence against: two
	 * researchers cite and record different things.
	 */
	private static void addNeighborhood(final MatchScore.Builder score, final IndividualFeatures fa, final IndividualFeatures fb){
		int sameEntries = 0;
		int sameSources = 0;
		final Set<String> sourcesB = new HashSet<>();
		for(final String citation : fb.citations)
			sourcesB.add(citation.substring(0, citation.indexOf('|')));
		for(final String citation : fa.citations){
			final int separator = citation.indexOf('|');
			// an entry is identified by its locator; without one, the citation names a whole source
			if(separator < citation.length() - 1 && fb.citations.contains(citation))
				sameEntries ++;
			else if(sourcesB.contains(citation.substring(0, separator)))
				sameSources ++;
		}
		if(sameEntries > 0)
			score.add(sameEntries >= 3? 6.: (sameEntries == 2? 5.: 3.), sameEntries + " same cited entries");
		else if(sameSources > 0)
			score.add(1., "same source cited");

		final int sameAttributes = intersection(fa.attributes, fb.attributes);
		if(sameAttributes > 0)
			score.add(Math.min(2., sameAttributes), sameAttributes + " same attributes");
		final int sameEvents = intersection(fa.otherEvents, fb.otherEvents);
		if(sameEvents > 0)
			score.add(Math.min(3., 1.5 * sameEvents), sameEvents + " same other events");
	}

	private static int intersection(final Set<String> a, final Set<String> b){
		int count = 0;
		for(final String element : a)
			if(b.contains(element))
				count ++;
		return count;
	}

	private void addDates(final MatchScore.Builder score, final List<TemporalSpan> spansA, final List<TemporalSpan> spansB,
			final boolean proxy, final String label, final double exactWeight, final double compatibleWeight){
		if(spansA.isEmpty() || spansB.isEmpty())
			return;

		double gap = Double.MAX_VALUE;
		boolean sameDay = false;
		boolean precise = !proxy;
		for(final TemporalSpan spanA : spansA)
			for(final TemporalSpan spanB : spansB){
				gap = Math.min(gap, gapInDays(spanA, spanB));
				sameDay |= isSameDay(spanA, spanB);
				precise &= isPreciseDay(spanA) && isPreciseDay(spanB);
			}
		// a baptism or a burial is a few days or months away from the birth or death it stands for
		if(proxy)
			gap = Math.max(0., gap - DAYS_PER_YEAR);

		final double years = gap / DAYS_PER_YEAR;
		if(sameDay && !proxy)
			score.add(exactWeight, "same " + label + " day");
		else if(gap == 0.)
			score.add(proxy? Math.min(compatibleWeight, 2.5): compatibleWeight, "compatible " + label + " dates");
		else if(precise && gap <= 31.)
			// a copying error moves a precise date by a few days, or swaps day and month
			score.add(1., label + " dates within a month");
		else if(precise && years <= 25.)
			// two precise dates months apart describe two events: e.g. a child named after a sibling who died young
			score.add(years <= 1.? -4.: -6., "precise " + label + " dates " + Math.round(gap) + " days apart");
		else if(years <= 2.)
			score.add(1., label + " dates within 2 years");
		else if(years <= 5.)
			score.add(-2., label + " dates within 5 years");
		else if(years <= 25.)
			score.add(-6., label + " dates " + Math.round(years) + " years apart");
		else
			score.veto(label + " dates " + Math.round(years) + " years apart");
	}

	/**
	 * Excludes pairs where one is born after the other died, or whose joint lifespan is not plausible.
	 */
	private static void checkLifespan(final MatchScore.Builder score, final IndividualFeatures fa, final IndividualFeatures fb){
		final double earliestBirth = Math.min(earliest(fa.births), earliest(fb.births));
		final double latestDeath = Math.max(latest(fa.deaths), latest(fb.deaths));
		if(bornAfterDeath(fa.births, fa.birthIsProxy || fb.deathIsProxy, fb.deaths)
				|| bornAfterDeath(fb.births, fb.birthIsProxy || fa.deathIsProxy, fa.deaths))
			score.veto("born after the death of the other");
		else if(earliestBirth != Double.MAX_VALUE && latestDeath != -Double.MAX_VALUE
				&& (latestDeath - earliestBirth) / DAYS_PER_YEAR > MAX_LIFESPAN_YEARS)
			score.veto("joint lifespan above " + Math.round(MAX_LIFESPAN_YEARS) + " years");
	}

	/**
	 * The intervals already cover the precision and the approximation of the dates, so no further tolerance is needed, except for a
	 * baptism or a burial standing for the birth or the death.
	 */
	private static boolean bornAfterDeath(final List<TemporalSpan> births, final boolean proxy, final List<TemporalSpan> deaths){
		final double tolerance = (proxy? DAYS_PER_YEAR / 4.: 0.);
		for(final TemporalSpan birth : births)
			for(final TemporalSpan death : deaths)
				if(interval(birth)[0] > interval(death)[1] + tolerance)
					return true;
		return false;
	}

	private static double earliest(final List<TemporalSpan> spans){
		double earliest = Double.MAX_VALUE;
		for(final TemporalSpan span : spans){
			final double low = interval(span)[0];
			if(low != -Double.MAX_VALUE)
				earliest = Math.min(earliest, low);
		}
		return earliest;
	}

	private static double latest(final List<TemporalSpan> spans){
		double latest = -Double.MAX_VALUE;
		for(final TemporalSpan span : spans){
			final double low = interval(span)[0];
			if(low != -Double.MAX_VALUE)
				latest = Math.max(latest, low);
		}
		return latest;
	}

	private void addPlaces(final MatchScore.Builder score, final Set<String> placesA, final Set<String> placesB,
			final String label){
		if(placesA.isEmpty() || placesB.isEmpty())
			return;

		for(final String place : placesA)
			if(placesB.contains(place)){
				score.add(2., "same " + label);
				return;
			}

		final Agreement agreement = TextSimilarity.bestPlace(placeNames(placesA), placeNames(placesB));
		if(agreement == null)
			return;
		switch(agreement){
			case EXACT, SUBSET -> score.add(1.5, label + " names agree");
			case SIMILAR -> score.add(0.5, label + " names similar");
			case DIFFERENT -> score.add(-1., label + " differs");
		}
	}

	private List<String> placeNames(final Set<String> placeIds){
		final List<String> names = new ArrayList<>();
		for(final String placeId : placeIds){
			final FLEFRecord place = index.record(placeId);
			if(place != null)
				names.addAll(placeNames(place).leaves());
		}
		return names;
	}

	/**
	 * How many relatives of one record are also relatives of the other.
	 *
	 * @param matched    relatives in common, as the same record or as a namesake
	 * @param sameRecord relatives in common as the same record, when that is evidence (see {@link #matchRelatives})
	 */
	private record Relatives(int matched, int sameRecord){}

	/**
	 * When merging two files, a relative that is the same record means that both files assert the relationship to the same
	 * (already merged) person: stronger than a namesake. Inside one file it means the opposite: two namesakes with the same parents
	 * are typically siblings, a child named after a sibling who died young.
	 *
	 * @return the relatives in common, or {@code null} when either has none
	 */
	private Relatives matchRelatives(final Set<String> relativesA, final Set<String> relativesB){
		if(relativesA.isEmpty() || relativesB.isEmpty())
			return null;

		int matched = 0;
		int sameRecord = 0;
		for(final String relativeA : relativesA)
			for(final String relativeB : relativesB){
				final boolean same = relativeA.equals(relativeB);
				if(same || isSameNamed(relativeA, relativeB)){
					matched ++;
					if(same && crossFile)
						sameRecord ++;
					break;
				}
			}
		return new Relatives(matched, sameRecord);
	}

	private boolean isSameNamed(final String idA, final String idB){
		final FLEFRecord a = index.record(idA);
		final FLEFRecord b = index.record(idB);
		if(a == null || b == null || !ModelIndex.KIND_INDIVIDUAL.equals(a.getTag()) || !ModelIndex.KIND_INDIVIDUAL.equals(b.getTag()))
			return false;

		final IndividualFeatures fa = individual(idA);
		final IndividualFeatures fb = individual(idB);
		final Agreement given = TextSimilarity.best(fa.givens, fb.givens);
		final Agreement family = TextSimilarity.best(fa.families, fb.families);
		return (given != null && given.isAtLeast(Agreement.SUBSET)
			&& (family == null || family.isAtLeast(Agreement.SIMILAR)));
	}

	private MatchScore compareGroups(final FLEFRecord a, final FLEFRecord b){
		final MatchScore.Builder score = new MatchScore.Builder(PRIOR_GROUP);

		addAgreement(score, TextSimilarity.best(names(a, TAG_NAME), names(b, TAG_NAME)), "name", 3., 1.5, 1.5, -2.);
		addType(score, a, b, 0.5, -4.);

		final Set<String> membersA = index.membersOf(a.getId());
		final Set<String> membersB = index.membersOf(b.getId());
		if(!membersA.isEmpty() && !membersB.isEmpty()){
			final Set<String> common = new HashSet<>(membersA);
			common.retainAll(membersB);
			final Set<String> all = new HashSet<>(membersA);
			all.addAll(membersB);
			final double jaccard = (double)common.size() / all.size();
			if(jaccard == 1.)
				score.add(10., "same members");
			else if(jaccard >= 0.5)
				score.add(7., "most members in common");
			else if(jaccard > 0.)
				score.add(3., "some members in common");
			else
				score.add(-6., "no member in common");
		}

		return score.build();
	}

	private MatchScore comparePlaces(final FLEFRecord a, final FLEFRecord b){
		final MatchScore.Builder score = new MatchScore.Builder(PRIOR_PLACE);

		// places the researcher related to each other are different records on purpose
		if(index.placeAncestorsOf(a.getId()).contains(b.getId()) || index.placeAncestorsOf(b.getId()).contains(a.getId()))
			return score.veto("one place is part of the other")
				.build();
		if(index.placeSuccessionsOf(a.getId()).contains(b.getId()))
			return score.veto("places linked by succession, merger, or division")
				.build();

		final String typeA = ModelIndex.value(a, ModelIndex.TAG_TYPE);
		final String typeB = ModelIndex.value(b, ModelIndex.TAG_TYPE);
		if(typeA != null && typeB != null){
			if(typeA.equals(typeB))
				score.add(1., "same type");
			// the type of a place is fixed for the lifetime of its record: inside a file a change of type is a different record, while
			// two researchers may classify the same settlement differently
			else if(crossFile && placeTypeFamily(typeA).equals(placeTypeFamily(typeB)))
				score.add(-2., "types differ (" + typeA + ", " + typeB + ")");
			else
				return score.veto("different place types (" + typeA + ", " + typeB + ")")
					.build();
		}

		final PlaceNames namesA = placeNames(a);
		final PlaceNames namesB = placeNames(b);
		addAgreement(score, TextSimilarity.bestPlace(namesA.leaves(), namesB.leaves()), "name", 5., 2.5, 2.5, -5.);

		final GeoCoordinate coordinateA = coordinates(a);
		final GeoCoordinate coordinateB = coordinates(b);
		if(coordinateA != null && coordinateB != null){
			final double km = distanceInKm(coordinateA, coordinateB);
			if(km <= 2.)
				score.add(4., "same location");
			else if(km <= 15.)
				score.add(1.5, "close locations");
			else if(km <= 50.)
				score.add(-1., "locations " + Math.round(km) + " km apart");
			else
				score.add(-6., "locations " + Math.round(km) + " km apart");
		}

		// the jurisdiction, at any level: the ancestors in the hierarchy and the qualifiers written in the name
		// (`San Martino, Treviso` and a San Martino part of a province part of Treviso agree)
		final Set<String> jurisdictionA = jurisdiction(a, namesA);
		final Set<String> jurisdictionB = jurisdiction(b, namesB);
		boolean differentJurisdictions = false;
		if(!jurisdictionA.isEmpty() && !jurisdictionB.isEmpty()){
			final int common = intersection(jurisdictionA, jurisdictionB);
			if(common >= 2)
				score.add(4., "same jurisdiction");
			else if(common == 1)
				score.add(3., "a jurisdiction agrees");
			else{
				score.add(-4., "different jurisdictions");
				differentJurisdictions = true;
			}
		}
		// the hierarchy is in other records: two places identical in their own record but in different jurisdictions are namesakes
		if(!differentJurisdictions)
			addIdenticalContent(score, a, b);

		// the subdivisions, from below: two Trevisos with the same hamlets
		final int commonChildren = intersection(childNames(a), childNames(b));
		if(commonChildren > 0)
			score.add(commonChildren >= 3? 4.: 2., commonChildren + " subdivisions in common");

		// the uses: the same birth, residence, or burial (of records already merged) took place there
		final int commonUses = intersection(index.usagesOf(a.getId()), index.usagesOf(b.getId()));
		if(commonUses > 0)
			score.add(commonUses >= 2? 5.: 3., commonUses + " uses in common");

		return score.build();
	}

	/**
	 * The names of a place, split at the commas: the first part names the place, the others qualify it with its jurisdiction
	 * (e.g. {@code San Martino, Treviso, Veneto}).
	 *
	 * @param leaves     the normalized names of the place itself
	 * @param qualifiers the normalized names of the jurisdictions written in the names
	 */
	private record PlaceNames(List<String> leaves, Set<String> qualifiers){}

	private static PlaceNames placeNames(final FLEFRecord place){
		final List<String> texts = new ArrayList<>();
		for(final FLEFRecord name : FieldMerger.children(place, TAG_NAME)){
			texts.add(ModelIndex.value(name, TAG_VALUE));
			addVariants(name, texts);
		}

		final List<String> leaves = new ArrayList<>();
		final Set<String> qualifiers = new LinkedHashSet<>();
		for(final String text : texts){
			if(text == null)
				continue;

			final String[] parts = (text.isEmpty()? new String[]{text}: StringUtils.splitPreserveAllTokens(text, ','));
			leaves.add(parts[0]);
			for(int i = 1; i < parts.length; i ++){
				final String qualifier = TextSimilarity.normalize(parts[i]);
				if(!qualifier.isEmpty())
					qualifiers.add(qualifier);
			}
		}
		return new PlaceNames(TextSimilarity.normalizeAll(leaves), qualifiers);
	}

	/**
	 * The jurisdictions of a place, as names (qualifiers and names of the ancestors) and as ids of the ancestors.
	 */
	private Set<String> jurisdiction(final FLEFRecord place, final PlaceNames names){
		final Set<String> jurisdiction = new HashSet<>(names.qualifiers());
		for(final String ancestorId : index.placeAncestorsOf(place.getId())){
			jurisdiction.add("@" + ancestorId);
			final FLEFRecord ancestor = index.record(ancestorId);
			if(ancestor != null)
				jurisdiction.addAll(placeNames(ancestor).leaves());
		}
		return jurisdiction;
	}

	private Set<String> childNames(final FLEFRecord place){
		final Set<String> names = new HashSet<>();
		for(final String childId : index.placeChildrenOf(place.getId())){
			final FLEFRecord child = index.record(childId);
			if(child != null)
				names.addAll(placeNames(child).leaves());
		}
		return names;
	}

	/**
	 * Types two researchers may use for the same place: the kinds of settlement, of civil and of ecclesiastical jurisdiction.
	 */
	private static String placeTypeFamily(final String type){
		return switch(type){
			case "hamlet", "village", "town", "municipality", "city", "metropolitan_area" -> "settlement";
			case "county", "province", "department", "district", "region", "macro_region" -> "jurisdiction";
			case "parish", "diocese", "archdiocese" -> "ecclesiastical";
			case "address", "building", "street" -> "address";
			default -> type;
		};
	}

	private MatchScore compareSources(final FLEFRecord a, final FLEFRecord b){
		final MatchScore.Builder score = new MatchScore.Builder(PRIOR_SOURCE);
		addIdenticalContent(score, a, b);

		addAgreement(score, TextSimilarity.best(names(a, TAG_TITLE), names(b, TAG_TITLE)), "title", 6., 3., 3., -6.);

		final Set<String> authorsA = agents(a);
		final Set<String> authorsB = agents(b);
		if(!authorsA.isEmpty() && !authorsB.isEmpty()){
			final Agreement agreement = TextSimilarity.best(authorsA, authorsB);
			if(agreement != null && agreement.isAtLeast(Agreement.SIMILAR))
				score.add(1.5, "author agrees");
			else
				score.add(-1.5, "authors differ");
		}

		final Agreement publisher = TextSimilarity.best(normalized(ModelIndex.value(a, TAG_PUBLISHER)),
			normalized(ModelIndex.value(b, TAG_PUBLISHER)));
		if(publisher != null)
			score.add(publisher.isAtLeast(Agreement.SIMILAR)? 1.: -1., publisher.isAtLeast(Agreement.SIMILAR)? "same publisher": "publishers differ");

		final String mediaTypeA = ModelIndex.value(a, TAG_MEDIA_TYPE);
		final String mediaTypeB = ModelIndex.value(b, TAG_MEDIA_TYPE);
		if(mediaTypeA != null && mediaTypeB != null)
			score.add(mediaTypeA.equals(mediaTypeB)? 0.5: -1.5, mediaTypeA.equals(mediaTypeB)? "same media type": "media types differ");

		addRecordDates(score, a, b);

		addRepositoryCitations(score, callNumbers(a), callNumbers(b));

		// the scans or recordings of the source (media are merged on their resource before sources)
		final Set<String> mediaA = values(a, TAG_MEDIA);
		final Set<String> mediaB = values(b, TAG_MEDIA);
		if(!mediaA.isEmpty() && !mediaB.isEmpty()){
			final int common = intersection(mediaA, mediaB);
			if(common > 0)
				score.add(6., common + " media in common");
			else
				// a new scan of the same document is still the same document
				score.add(-1., "different media");
		}

		// the uses: the same entries cited for the same assertions about the same (already merged) records
		final int sameEntries = intersection(index.locatedUsagesOf(a.getId()), index.locatedUsagesOf(b.getId()));
		if(sameEntries > 0)
			score.add(sameEntries >= 2? 6.: 4., sameEntries + " same entries cited for the same assertions");
		else if(intersection(index.usagesOf(a.getId()), index.usagesOf(b.getId())) > 0)
			score.add(1.5, "cited for the same assertions");

		return score.build();
	}

	/**
	 * A call number (the locator of a repository citation) identifies an item inside its repository: the same call number in the
	 * same repository is the same item, different call numbers are different items.
	 */
	private static void addRepositoryCitations(final MatchScore.Builder score, final Map<String, Set<String>> callNumbersA,
			final Map<String, Set<String>> callNumbersB){
		if(callNumbersA.isEmpty() || callNumbersB.isEmpty())
			return;

		boolean sameRepository = false;
		boolean differentItems = false;
		for(final Map.Entry<String, Set<String>> entry : callNumbersA.entrySet()){
			final Set<String> locatorsB = callNumbersB.get(entry.getKey());
			if(locatorsB == null)
				continue;

			sameRepository = true;
			final Set<String> locatorsA = entry.getValue();
			if(!locatorsA.isEmpty() && !locatorsB.isEmpty()){
				if(intersection(locatorsA, locatorsB) > 0){
					score.add(5., "same call number in the same repository");
					return;
				}
				differentItems = true;
			}
		}
		if(differentItems)
			score.add(-3., "different call numbers in the same repository");
		else if(sameRepository)
			score.add(1.5, "same repository");
		else
			score.add(-0.5, "repositories differ");
	}

	/**
	 * The repositories of a source, each with the normalized call numbers it is cited with.
	 */
	private static Map<String, Set<String>> callNumbers(final FLEFRecord source){
		final Map<String, Set<String>> callNumbers = new HashMap<>();
		for(final FLEFRecord citation : FieldMerger.children(source, TAG_REPOSITORY)){
			final String repository = ModelIndex.value(citation, TAG_REPOSITORY);
			if(repository == null)
				continue;

			final Set<String> locators = callNumbers.computeIfAbsent(repository, k -> new HashSet<>());
			final String locator = TextSimilarity.normalize(ModelIndex.value(citation, TAG_LOCATOR));
			if(!locator.isEmpty())
				locators.add(locator);
		}
		return callNumbers;
	}

	/**
	 * The values of the repeated leaf {@code tag} (e.g. the media of a source).
	 */
	private static Set<String> values(final FLEFRecord record, final String tag){
		final Set<String> values = new HashSet<>();
		for(final FLEFRecord child : FieldMerger.children(record, tag))
			if(child.getValue() != null)
				values.add(child.getValue());
		return values;
	}

	private MatchScore compareRepositories(final FLEFRecord a, final FLEFRecord b){
		final MatchScore.Builder score = new MatchScore.Builder(PRIOR_REPOSITORY);
		addIdenticalContent(score, a, b);

		addAgreement(score, TextSimilarity.best(names(a, TAG_NAME), names(b, TAG_NAME)), "name", 6., 3., 3., -6.);
		addCitedPlace(score, a, b);

		final List<String> contactsA = new ArrayList<>();
		for(final FLEFRecord contact : FieldMerger.children(a, TAG_CONTACT))
			contactsA.add(TextSimilarity.normalize(ModelIndex.value(contact, TAG_VALUE)));
		for(final FLEFRecord contact : FieldMerger.children(b, TAG_CONTACT))
			if(contactsA.contains(TextSimilarity.normalize(ModelIndex.value(contact, TAG_VALUE)))){
				score.add(2., "same contact");
				break;
			}

		return score.build();
	}

	private static MatchScore compareMedia(final FLEFRecord a, final FLEFRecord b){
		final MatchScore.Builder score = new MatchScore.Builder(PRIOR_MEDIA);

		final String kindA = ModelIndex.value(a, TAG_KIND);
		final String kindB = ModelIndex.value(b, TAG_KIND);
		final String uriA = ModelIndex.value(a, TAG_URI);
		final String uriB = ModelIndex.value(b, TAG_URI);
		if(kindA != null && kindB != null && !kindA.equals(kindB))
			score.veto("different kinds of media");
		else if(uriA == null || uriB == null || !uriA.strip().equalsIgnoreCase(uriB.strip()))
			score.veto("different resources");
		else
			score.add(12., "same resource");

		return score.build();
	}

	private MatchScore compareContexts(final FLEFRecord a, final FLEFRecord b){
		final MatchScore.Builder score = new MatchScore.Builder(PRIOR_CONTEXT);
		addIdenticalContent(score, a, b);

		addAgreement(score, TextSimilarity.best(normalized(ModelIndex.value(a, TAG_TITLE)),
			normalized(ModelIndex.value(b, TAG_TITLE))), "title", 5., 2.5, 2.5, -5.);
		addType(score, a, b, 1., -4.);
		addRecordDates(score, a, b);
		addCitedPlace(score, a, b);

		return score.build();
	}


	/* ======================================================================
	 *                          Features
	 * ====================================================================== */

	/**
	 * Places, sources, repositories, cultural norms, and historic events are identified by what they state: two records stating
	 * exactly the same (evidence aside) are likely copies of one another. Individuals and groups are not, since their facts live in
	 * other records.
	 */
	private void addIdenticalContent(final MatchScore.Builder score, final FLEFRecord a, final FLEFRecord b){
		if(signature.ofRecord(a).equals(signature.ofRecord(b)))
			score.add(4., "identical records");
	}

	private static void addAgreement(final MatchScore.Builder score, final Agreement agreement, final String label,
			final double exact, final double subset, final double similar, final double different){
		if(agreement == null)
			return;

		switch(agreement){
			case EXACT -> score.add(exact, label + " agrees");
			case SUBSET -> score.add(subset, label + " partially agrees");
			case SIMILAR -> score.add(similar, label + " similar");
			case DIFFERENT -> score.add(different, label + " differs");
		}
	}

	private static void addType(final MatchScore.Builder score, final FLEFRecord a, final FLEFRecord b, final double same,
			final double different){
		final String typeA = ModelIndex.value(a, ModelIndex.TAG_TYPE);
		final String typeB = ModelIndex.value(b, ModelIndex.TAG_TYPE);
		if(typeA != null && typeB != null)
			score.add(typeA.equals(typeB)? same: different, typeA.equals(typeB)? "same type": "types differ");
	}

	/**
	 * Compares the {@code date} of two records (a source, a historic event).
	 */
	private static void addRecordDates(final MatchScore.Builder score, final FLEFRecord a, final FLEFRecord b){
		final FLEFRecord dateA = FieldMerger.first(a, ModelIndex.TAG_DATE);
		final FLEFRecord dateB = FieldMerger.first(b, ModelIndex.TAG_DATE);
		final TemporalSpan spanA = ModelIndex.span(dateA);
		final TemporalSpan spanB = ModelIndex.span(dateB);
		if(spanA == null || spanB == null)
			return;

		final double gap = gapInDays(spanA, spanB);
		if(gap == 0.)
			score.add(1.5, "compatible dates");
		else if(gap > DAYS_PER_YEAR)
			score.add(-3., "dates differ");
	}

	/**
	 * Compares the {@code place} citation of two records (a repository, a cultural norm, a historic event).
	 */
	private void addCitedPlace(final MatchScore.Builder score, final FLEFRecord a, final FLEFRecord b){
		final Set<String> placeA = references(a, ModelIndex.TAG_PLACE, ModelIndex.TAG_PLACE);
		final Set<String> placeB = references(b, ModelIndex.TAG_PLACE, ModelIndex.TAG_PLACE);
		addPlaces(score, placeA, placeB, "place");
	}

	private IndividualFeatures individual(final String id){
		return individualFeatures.computeIfAbsent(id, k -> new IndividualFeatures(index.record(k), index));
	}

	/**
	 * Normalized values, and variants, of the {@code NameStructure}s of a record under the given tag.
	 */
	static List<String> names(final FLEFRecord record, final String tag){
		final List<String> texts = new ArrayList<>();
		for(final FLEFRecord name : FieldMerger.children(record, tag)){
			texts.add(ModelIndex.value(name, TAG_VALUE));
			addVariants(name, texts);
		}
		return TextSimilarity.normalizeAll(texts);
	}

	private static void addVariants(final FLEFRecord owner, final Collection<String> texts){
		for(final FLEFRecord variant : FieldMerger.children(owner, TAG_VARIANT))
			for(final FLEFRecord arm : variant.getChildren())
				texts.add(ModelIndex.value(arm, TAG_VALUE));
	}

	private static List<String> normalized(final String text){
		final String normalized = TextSimilarity.normalize(text);
		return (normalized.isEmpty()? List.of(): List.of(normalized));
	}

	/**
	 * Authors as normalized text, or as {@code @<Id>} when the author is a record of the file.
	 */
	private static Set<String> agents(final FLEFRecord source){
		final Set<String> agents = new HashSet<>();
		for(final FLEFRecord author : FieldMerger.children(source, TAG_AUTHOR)){
			final String arm = ModelIndex.armTag(author);
			final String value = ModelIndex.armValue(author);
			if(value != null)
				agents.add(AGENT_TEXT.equals(arm)? TextSimilarity.normalize(value): "@" + value);
		}
		agents.remove(StringUtils.EMPTY);
		return agents;
	}

	/**
	 * Ids referenced by {@code <tag>[*].<referenceTag>}.
	 */
	private static Set<String> references(final FLEFRecord record, final String tag, final String referenceTag){
		final Set<String> ids = new HashSet<>();
		for(final FLEFRecord citation : FieldMerger.children(record, tag)){
			final String id = ModelIndex.value(citation, referenceTag);
			if(id != null)
				ids.add(id);
		}
		return ids;
	}

	private static GeoCoordinate coordinates(final FLEFRecord place){
		final FLEFRecord location = FieldMerger.first(place, TAG_LOCATION);
		return (location != null? GeoCoordinate.parse(ModelIndex.value(location, TAG_COORDINATES)): null);
	}

	private static double distanceInKm(final GeoCoordinate a, final GeoCoordinate b){
		final double lat1 = Math.toRadians(a.latitude());
		final double lat2 = Math.toRadians(b.latitude());
		final double dLat = lat2 - lat1;
		final double dLon = Math.toRadians(b.longitude() - a.longitude());
		final double h = Math.sin(dLat / 2.) * Math.sin(dLat / 2.)
			+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2.) * Math.sin(dLon / 2.);
		return 2. * 6371. * Math.asin(Math.min(1., Math.sqrt(h)));
	}

	/**
	 * Days between two spans, {@code 0} when they overlap. The span of a point covers its precision (a year-precise date covers its
	 * whole year), and an approximate bound is widened by two years.
	 */
	private static double gapInDays(final TemporalSpan a, final TemporalSpan b){
		final double[] intervalA = interval(a);
		final double[] intervalB = interval(b);
		return Math.max(0., Math.max(intervalA[0], intervalB[0]) - Math.min(intervalA[1], intervalB[1]));
	}

	private static double[] interval(final TemporalSpan span){
		double low = (span.minJdn() == Long.MIN_VALUE? -Double.MAX_VALUE: span.minJdn());
		double high;
		if(span.isPoint())
			high = span.start().jdn() + span.start().precisionSpanInDays() - 1;
		else
			high = (span.maxJdn() == Long.MAX_VALUE? Double.MAX_VALUE
				: span.maxJdn() + span.end().precisionSpanInDays() - 1);
		if(span.isApproximate()){
			if(low != -Double.MAX_VALUE)
				low -= 2. * DAYS_PER_YEAR;
			if(high != Double.MAX_VALUE)
				high += 2. * DAYS_PER_YEAR;
		}
		return new double[]{low, high};
	}

	private static boolean isPreciseDay(final TemporalSpan span){
		return (span.isPoint() && !span.isApproximate() && span.start().precisionSpanInDays() == 1);
	}

	private static boolean isSameDay(final TemporalSpan a, final TemporalSpan b){
		return (a.isPoint() && b.isPoint() && !a.isApproximate() && !b.isApproximate()
			&& a.start().precisionSpanInDays() == 1 && b.start().precisionSpanInDays() == 1
			&& a.start().jdn() == b.start().jdn());
	}


	/**
	 * What is known of an individual for the comparison, computed once.
	 */
	private static final class IndividualFeatures{

		private final List<String> givens;
		private final List<String> families;
		private final List<TemporalSpan> births;
		private final boolean birthIsProxy;
		private final List<TemporalSpan> deaths;
		private final boolean deathIsProxy;
		private final Set<String> birthPlaces;
		private final Set<String> citations;
		private final Set<String> attributes;
		private final Set<String> otherEvents;


		private IndividualFeatures(final FLEFRecord individual, final ModelIndex index){
			final List<String> givenTexts = new ArrayList<>();
			final List<String> familyTexts = new ArrayList<>();
			if(individual != null)
				for(final FLEFRecord name : FieldMerger.children(individual, TAG_NAME)){
					final StringBuilder given = new StringBuilder();
					final StringBuilder family = new StringBuilder();
					for(final FLEFRecord part : FieldMerger.children(name, TAG_PART)){
						final String type = ModelIndex.value(part, ModelIndex.TAG_TYPE);
						final String value = ModelIndex.value(part, TAG_VALUE);
						if(value == null)
							continue;

						if(PART_GIVEN.equals(type)){
							given.append(value).append(' ');
							addVariants(part, givenTexts);
						}
						else if(PART_FAMILY.equals(type)){
							family.append(value).append(' ');
							addVariants(part, familyTexts);
						}
					}
					givenTexts.add(given.toString());
					familyTexts.add(family.toString());
				}
			givens = TextSimilarity.normalizeAll(givenTexts);
			families = TextSimilarity.normalizeAll(familyTexts);

			final String id = (individual != null? individual.getId(): null);
			final List<TemporalSpan> birthSpans = index.principalEventSpans(id, BIRTH_TYPES);
			birthIsProxy = birthSpans.isEmpty();
			births = (birthIsProxy? index.principalEventSpans(id, BIRTH_PROXY_TYPES): birthSpans);
			final List<TemporalSpan> deathSpans = index.principalEventSpans(id, DEATH_TYPES);
			deathIsProxy = deathSpans.isEmpty();
			deaths = (deathIsProxy? index.principalEventSpans(id, DEATH_PROXY_TYPES): deathSpans);

			final Set<String> places = new LinkedHashSet<>(index.principalEventPlaces(id, BIRTH_TYPES));
			places.addAll(index.principalEventPlaces(id, BIRTH_PROXY_TYPES));
			birthPlaces = places;

			citations = index.subjectCitations(id);
			attributes = index.attributeKeys(id);
			otherEvents = index.otherPrincipalEventKeys(id, OWN_EVENT_TYPES);
		}

	}

}
