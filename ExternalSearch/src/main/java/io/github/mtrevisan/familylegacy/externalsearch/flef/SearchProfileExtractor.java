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

package io.github.mtrevisan.familylegacy.externalsearch.flef;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.externalsearch.CountryResolver;
import io.github.mtrevisan.familylegacy.externalsearch.Placeholder;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSubject;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.NameAnatomyService;
import io.github.mtrevisan.familylegacy.io.model.readers.NamePart;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariant;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.JulianDay;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.places.PlaceNameResolver;
import io.github.mtrevisan.familylegacy.privacy.PrivacyResolver;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Takes from an individual of a FLEF file what a search on an external site needs: its names (all of them, and their
 * transcriptions), its sex, when and where it was born and died, the names of its parents and of a spouse.
 * <p>
 * Read only. The birth is the event of type birth the individual is the principal of (or, without one, its baptism or christening),
 * the death likewise (or its burial). The years are Gregorian; the window of a year is the uncertainty of the date, widened by
 * {@value #YEAR_TOLERANCE} years around an exact year and by {@value #ABOUT_TOLERANCE} around an "about" one (records disagree), and
 * {@value #OPEN_TOLERANCE} years on an open side ("before 1820"). The relatives that may be living or whose privacy is restricted are
 * left out: their names are not sent to a site.
 */
public final class SearchProfileExtractor{

	/** The years added on each side of an exact year. */
	static final int YEAR_TOLERANCE = 2;
	/** The years added on each side of an "about" year. */
	static final int ABOUT_TOLERANCE = 5;
	/** The years taken on the open side of an interval. */
	static final int OPEN_TOLERANCE = 10;

	/** The parts that make the surname, when there is no family name: the first one found is taken. */
	private static final List<String> SURNAME_SUBSTITUTES = List.of(NameReader.ENUM_PART_TYPE_PATRONYMIC, NameReader.ENUM_PART_TYPE_MATRONYMIC,
		NameReader.ENUM_PART_TYPE_TOPONYMIC, NameReader.ENUM_PART_TYPE_LINEAGE, NameReader.ENUM_PART_TYPE_HOUSE,
		NameReader.ENUM_PART_TYPE_CLAN, NameReader.ENUM_PART_TYPE_TRIBAL);

	/** Places that are inside a town: a search is about the town. */
	private static final Set<String> INSIDE_A_TOWN = Set.of(PlaceReader.ENUM_TYPE_ADDRESS, PlaceReader.ENUM_TYPE_BUILDING,
		PlaceReader.ENUM_TYPE_STREET, PlaceReader.ENUM_TYPE_CEMETERY, PlaceReader.ENUM_TYPE_PARISH);

	/** The relationships between places that say that a place is inside another one. */
	private static final Set<String> CONTAINMENTS = Set.of(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF,
		PlaceRelationshipReader.ENUM_TYPE_GEOGRAPHIC_PART_OF, PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF,
		PlaceRelationshipReader.ENUM_TYPE_JUDICIAL_PART_OF, PlaceRelationshipReader.ENUM_TYPE_CADASTRAL_PART_OF);

	private static final String LABEL_SEPARATOR = ", ";
	private static final char COMMA = ',';


	private final FLEFModel model;
	private final PrivacyResolver privacy;
	private final RelationIndex index;
	private final Map<String, List<EventHelper.Participation>> participations;
	private final PlaceNameResolver placeNames;
	/** Place id -> the id of the place that contains it (the first containment found). */
	private final Map<String, String> containerOf = new HashMap<>();


	private SearchProfileExtractor(final FLEFModel model){
		this.model = model;
		privacy = PrivacyService.resolver(model);
		index = RelationIndex.build(model);
		participations = EventHelper.participationsByEvent(model);
		placeNames = new PlaceNameResolver(model);
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(PlaceRelationshipReader.TYPE))){
			final String type = PlaceRelationshipReader.extractType(relationship);
			if(type != null && !CONTAINMENTS.contains(type.toLowerCase(Locale.ROOT)))
				continue;

			final String childId = relationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE);
			final String parentId = relationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, PlaceReader.TYPE);
			if(childId != null && parentId != null)
				containerOf.putIfAbsent(childId, parentId);
		}
	}


	/**
	 * @param model	The model.
	 * @param individual	An individual of the model.
	 * @return	What the individual gives to a search.
	 * @throws IllegalArgumentException	If the record is not an individual.
	 */
	public static SearchProfile extract(final FLEFModel model, final FLEFRecord individual){
		if(!IndividualReader.isOfType(individual))
			throw new IllegalArgumentException("Not an individual: " + (individual != null? individual.getId(): null));

		return new SearchProfileExtractor(model).profileOf(individual);
	}


	private SearchProfile profileOf(final FLEFRecord individual){
		final PrivacyResolver.Effective effective = privacy.describe(individual);
		final boolean mayBeLiving = privacy.concernsLiving(individual);

		final Map<Placeholder, String> facts = new EnumMap<>(Placeholder.class);
		final SexType sex = PreferredSex.of(individual, model);
		if(sex != SexType.UNKNOWN)
			facts.put(Placeholder.SEX, sex.getRawSex());

		FLEFRecord birth = principalEvent(individual, EventReader.ENUM_TYPE_BIRTH);
		if(birth == null)
			birth = principalEvent(individual, EventReader.CUSTOM_TYPE_BAPTISM, EventReader.CUSTOM_TYPE_CHRISTENING);
		putEvent(facts, birth, Placeholder.BIRTH_YEAR, Placeholder.BIRTH_YEAR_FROM, Placeholder.BIRTH_YEAR_TO, Placeholder.BIRTH_PLACE,
			Placeholder.BIRTH_PLACE_FULL, Placeholder.BIRTH_COUNTRY);
		FLEFRecord death = principalEvent(individual, EventReader.ENUM_TYPE_DEATH);
		if(death == null)
			death = principalEvent(individual, EventReader.ENUM_TYPE_BURIAL);
		putEvent(facts, death, Placeholder.DEATH_YEAR, Placeholder.DEATH_YEAR_FROM, Placeholder.DEATH_YEAR_TO, Placeholder.DEATH_PLACE,
			Placeholder.DEATH_PLACE_FULL, Placeholder.DEATH_COUNTRY);

		for(final FLEFRecord parent : index.parentsOf(individual)){
			if(isRestricted(parent))
				continue;

			final SexType parentSex = PreferredSex.of(parent, model);
			if(parentSex == SexType.MALE && !facts.containsKey(Placeholder.FATHER_GIVEN) && !facts.containsKey(Placeholder.FATHER_SURNAME))
				putName(facts, parent, Placeholder.FATHER_GIVEN, Placeholder.FATHER_SURNAME);
			else if(parentSex == SexType.FEMALE && !facts.containsKey(Placeholder.MOTHER_GIVEN)
					&& !facts.containsKey(Placeholder.MOTHER_SURNAME))
				putName(facts, parent, Placeholder.MOTHER_GIVEN, Placeholder.MOTHER_SURNAME);
		}
		for(final FLEFRecord spouse : index.spousesOf(individual))
			if(!isRestricted(spouse)){
				putName(facts, spouse, Placeholder.SPOUSE_GIVEN, Placeholder.SPOUSE_SURNAME);
				break;
			}

		return new SearchProfile(individual.getId(), nameCandidates(individual), SearchSubject.of(facts), effective.level(), mayBeLiving);
	}

	private boolean isRestricted(final FLEFRecord person){
		return (privacy.describe(person).isRestricted() || privacy.concernsLiving(person));
	}


	/* ======================================================================
	 *                          Names
	 * ====================================================================== */

	/**
	 * @param individual	An individual.
	 * @return	Its names, each followed by its transcriptions (one name for each system of transcription found in its parts), without
	 * 	repetitions.
	 */
	static List<NameCandidate> nameCandidates(final FLEFRecord individual){
		final Map<String, NameCandidate> candidates = new LinkedHashMap<>();
		for(final Name name : NameAnatomyService.extractForIndividual(individual)){
			if(!name.hasParts())
				continue;

			add(candidates, candidate(name, null));
			final Set<String> systems = new LinkedHashSet<>();
			for(final NamePart part : name.parts())
				for(final TextValueVariant variant : part.variants())
					if(variant.isTranscription() && StringUtils.isNotBlank(variant.value()))
						systems.add(transcriptionKey(variant));
			for(final String system : systems)
				add(candidates, candidate(name, system));
		}
		return new ArrayList<>(candidates.values());
	}

	private static void add(final Map<String, NameCandidate> candidates, final NameCandidate candidate){
		if(!candidate.isEmpty())
			candidates.putIfAbsent((candidate.given() + '|' + candidate.surname() + '|' + candidate.full()).toLowerCase(Locale.ROOT), candidate);
	}

	/** @return	The name, with the parts that have a transcription in the given system replaced by it ({@code null}: as written). */
	private static NameCandidate candidate(final Name name, final String system){
		final List<String> given = new ArrayList<>();
		final List<String> family = new ArrayList<>();
		final Map<String, String> substitutes = new HashMap<>();
		final List<String> full = new ArrayList<>();
		for(final NamePart part : name.parts()){
			final String value = valueOf(part, system);
			if(value.isEmpty())
				continue;

			final String type = part.type().toLowerCase(Locale.ROOT);
			if(NameReader.ENUM_PART_TYPE_GIVEN.equals(type))
				given.add(value);
			else if(NameReader.ENUM_PART_TYPE_FAMILY.equals(type) || NameReader.ENUM_PART_TYPE_FAMILY_PREFIX.equals(type))
				family.add(value);
			else if(SURNAME_SUBSTITUTES.contains(type))
				substitutes.putIfAbsent(type, value);
			if(!NameReader.EXCLUDED_PART_TYPES.contains(type))
				full.add(value);
		}
		String surname = String.join(StringUtils.SPACE, family);
		if(surname.isEmpty())
			for(final String type : SURNAME_SUBSTITUTES)
				if(substitutes.containsKey(type)){
					surname = substitutes.get(type);
					break;
				}

		final String label = (system != null? name.displayType() + LABEL_SEPARATOR + system: name.displayType());
		return new NameCandidate(label, String.join(StringUtils.SPACE, given), surname, String.join(StringUtils.SPACE, full));
	}

	private static String valueOf(final NamePart part, final String system){
		if(system != null)
			for(final TextValueVariant variant : part.variants())
				if(variant.isTranscription() && system.equals(transcriptionKey(variant)) && StringUtils.isNotBlank(variant.value()))
					return variant.value().strip();
		return StringUtils.strip(part.value());
	}

	/** @return	The system of a transcription and its type, as shown to the user ("pinyin", "latinized"). */
	private static String transcriptionKey(final TextValueVariant variant){
		final String system = StringUtils.strip(variant.system());
		final String type = StringUtils.strip(variant.type());
		if(StringUtils.isEmpty(system))
			return (StringUtils.isEmpty(type)? variant.kind(): type);
		return (StringUtils.isEmpty(type)? system: system + " " + type);
	}

	/** Puts the given names and the surname of the first name of a relative. */
	private static void putName(final Map<Placeholder, String> facts, final FLEFRecord person, final Placeholder given,
			final Placeholder surname){
		final List<NameCandidate> names = nameCandidates(person);
		if(names.isEmpty())
			return;

		final NameCandidate name = names.getFirst();
		putIfNotBlank(facts, given, name.given());
		putIfNotBlank(facts, surname, name.surname());
	}


	/* ======================================================================
	 *                          Events
	 * ====================================================================== */

	/** @return	The first event of one of the types (in their order) the individual is the principal of, {@code null} if none. */
	private FLEFRecord principalEvent(final FLEFRecord person, final String... types){
		for(final String type : types){
			FLEFRecord found = null;
			for(final FLEFRecord event : index.eventsOf(person)){
				if(!type.equalsIgnoreCase(EventHelper.eventType(event)) || !isPrincipal(person, event))
					continue;

				//one with a date is worth more than one without
				if(found == null || (dateOf(found) == null && dateOf(event) != null))
					found = event;
			}
			if(found != null)
				return found;
		}
		return null;
	}

	private boolean isPrincipal(final FLEFRecord person, final FLEFRecord event){
		for(final EventHelper.Participation participation : participations.getOrDefault(event.getId(), List.of()))
			if(person.getId().equals(participation.participantId())
					&& (StringUtils.isBlank(participation.role()) || EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(participation.role())))
				return true;
		return false;
	}

	private static TemporalSpan dateOf(final FLEFRecord event){
		final FLEFRecord date = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		return (date != null? DateReader.extractTemporalSpan(date): null);
	}

	private void putEvent(final Map<Placeholder, String> facts, final FLEFRecord event, final Placeholder year, final Placeholder from,
			final Placeholder to, final Placeholder place, final Placeholder placeFull, final Placeholder country){
		if(event == null)
			return;

		final int[] years = years(dateOf(event));
		if(years != null){
			facts.put(year, Integer.toString(years[0]));
			facts.put(from, Integer.toString(years[1]));
			facts.put(to, Integer.toString(years[2]));
		}

		final String placeId = EventHelper.eventPlaceId(event);
		final FLEFRecord placeRecord = (placeId != null? model.getRecordById(placeId): null);
		if(placeRecord != null && !privacy.describe(placeRecord).isRestricted()){
			putIfNotBlank(facts, place, PlaceReader.displayName(townOf(placeRecord)));
			putIfNotBlank(facts, placeFull, placeNames.fullName(placeId));
			CountryResolver.resolveLevels(placeNames.nameLevels(placeId))
				.ifPresent(code -> facts.put(country, code));
		}
		else if(placeId == null){
			//only the place as written in the source: the first part is the town, the last one may be the country
			final String text = PlaceCitationReader.extractPlace(event, model);
			final String[] parts = StringUtils.split(text, COMMA);
			if(parts != null && parts.length > 0){
				putIfNotBlank(facts, place, parts[0]);
				putIfNotBlank(facts, placeFull, text);
				CountryResolver.resolve(parts[parts.length - 1])
					.ifPresent(code -> facts.put(country, code));
			}
		}
	}

	/**
	 * @param span	The date of an event.
	 * @return	The representative year, the first and the last year of the window to search in; {@code null} if there is no date.
	 */
	static int[] years(final TemporalSpan span){
		final NormalizedDate representative = (span != null? span.representativeDate(): null);
		if(representative == null)
			return null;

		final int year = yearOf(representative.jdn());
		int from;
		int to;
		if(span.isPoint()){
			from = year;
			//a decade or a century is a window of its own
			to = switch(representative.precision()){
				case DECADE -> from + 9;
				case CENTURY -> from + 99;
				default -> from;
			};
			final int tolerance = (representative.isAbout()? ABOUT_TOLERANCE: YEAR_TOLERANCE);
			if(from == to){
				from -= tolerance;
				to += tolerance;
			}
		}
		else{
			from = (span.start() != null? yearOf(span.start().jdn()): year - OPEN_TOLERANCE);
			to = (span.end() != null? yearOf(span.end().jdn()): year + OPEN_TOLERANCE);
		}
		return new int[]{year, from, to};
	}

	private static int yearOf(final long jdn){
		return JulianDay.toGregorian(jdn)[0];
	}

	/** @return	The place, or the first place that contains it and is not inside a town (a church, a street, a parish). */
	private FLEFRecord townOf(final FLEFRecord place){
		FLEFRecord current = place;
		final Set<String> visited = new HashSet<>();
		while(current != null && visited.add(current.getId())){
			final String type = PlaceReader.extractType(current);
			if(type == null || !INSIDE_A_TOWN.contains(type.toLowerCase(Locale.ROOT)))
				return current;

			final String parentId = containerOf.get(current.getId());
			final FLEFRecord parent = (parentId != null? model.getRecordById(parentId): null);
			if(parent == null)
				return current;
			current = parent;
		}
		return place;
	}

	private static void putIfNotBlank(final Map<Placeholder, String> facts, final Placeholder placeholder, final String value){
		if(StringUtils.isNotBlank(value))
			facts.put(placeholder, value.strip());
	}

}
