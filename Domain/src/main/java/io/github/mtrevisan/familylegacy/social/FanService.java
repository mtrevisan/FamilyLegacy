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
package io.github.mtrevisan.familylegacy.social;

import io.github.mtrevisan.familylegacy.dossier.DossierFormatting;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Builds the FAN table (Friends, Associates, Neighbors: the method of E. S. Mills to get around a brick wall) of an individual: the
 * people who appear with him in the same events, the strongest link first.
 * <p>
 * Godparents and witnesses that recur are often relatives or fellow villagers, and the parish registers note them almost always.
 * </p>
 * <p>
 * <b>Who.</b> Every other individual that takes part, with any role, in an event of the subject. The links between <em>all</em> the
 * participants of an event count, not only the ones with the subject: that is how two godparents of the same child can be seen as a
 * possible couple (see {@link FanSuggestion}). The records the privacy policy hides are not read.
 * </p>
 * <p>
 * <b>Strength</b> of a link: two points for each shared event, one for each different pair of roles beyond the first (principal-witness
 * and principal-midwife make two pairs) and one if the shared events are not all in the same year (a tie that lasts). The relatives
 * of the subject (spouses, parents, children, siblings) are listed after the others, whatever their score.
 * </p>
 * <p>
 * Nothing is written to the model: the suggestions are hints for a human, never applied.
 * </p>
 */
public final class FanService{

	/** The role of the one an event is about. */
	private static final String ROLE_PRINCIPAL = EventParticipationReader.ENUM_ROLE_PRINCIPAL;
	/** From how many shared events a person is said to recur. */
	static final int RECURRING_MINIMUM = 2;
	/** The first day of the Gregorian calendar, as a Julian day number: before it the years are the Julian ones. */
	private static final long GREGORIAN_START_JDN = 2_299_161L;
	/** The Julian day number of 1970-01-01. */
	private static final long EPOCH_JDN = 2_440_588L;

	private static final Comparator<FanEntry> ORDER = Comparator.comparing(FanEntry::kin)
		.thenComparing(Comparator.comparingInt(FanEntry::score).reversed())
		.thenComparing(FanEntry::name, String.CASE_INSENSITIVE_ORDER)
		.thenComparing(FanEntry::personId);


	private final FLEFModel model;
	private final DossierFormatting formatting;


	public FanService(final FLEFModel model){
		this.model = model;
		this.formatting = new DossierFormatting(model);
	}


	/**
	 * Builds the FAN table of the given individual. The model is read as it is now: nothing is kept between two calls.
	 *
	 * @param subjectId the identifier of the individual; can be {@code null}
	 * @return the table, empty if the individual is unknown or hidden, or nobody appears with it
	 */
	public FanResult build(final String subjectId){
		if(StringUtils.isEmpty(subjectId) || !model.hasRecord(subjectId))
			return FanResult.empty();
		final FLEFRecord subject = model.getRecordById(subjectId);
		if(!IndividualReader.isOfType(subject) || PrivacyService.isHidden(model, subject))
			return FanResult.empty();

		//the participants of every event, and how many times each individual has a role other than principal
		final Map<String, List<Participation>> participantsByEvent = new HashMap<>();
		final Map<String, Integer> roleTotals = new HashMap<>();
		for(final FLEFRecord participation : PrivacyService.visible(model, model.getRecordsByType(EventParticipationReader.TYPE))){
			final String personId = EventParticipationReader.extractParticipantRef(participation,
				EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL);
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(personId == null || eventId == null)
				continue;

			final String role = normalizeRole(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE));
			participantsByEvent.computeIfAbsent(eventId, k -> new ArrayList<>())
				.add(new Participation(personId, role));
			if(!role.isEmpty() && !ROLE_PRINCIPAL.equals(role))
				roleTotals.merge(personId, 1, Integer::sum);
		}

		final Kinship kinship = new Kinship(model);
		final Map<String, Accumulator> others = new TreeMap<>();
		final Map<String, EventInfo> eventInfos = new HashMap<>();
		final List<String> subjectEventIds = new ArrayList<>();
		//in a deterministic order, so that the table does not depend on the hash order
		for(final Map.Entry<String, List<Participation>> entry : new TreeMap<>(participantsByEvent).entrySet()){
			final String eventId = entry.getKey();
			final List<String> ownRoles = new ArrayList<>();
			for(final Participation participation : entry.getValue())
				if(participation.personId().equals(subjectId))
					ownRoles.add(participation.role());
			if(ownRoles.isEmpty())
				continue;

			subjectEventIds.add(eventId);
			final EventInfo info = eventInfos.computeIfAbsent(eventId, this::describeEvent);
			for(final Participation participation : entry.getValue()){
				if(participation.personId().equals(subjectId) || !isVisibleIndividual(participation.personId()))
					continue;

				final Accumulator accumulator = others.computeIfAbsent(participation.personId(), k -> new Accumulator());
				if(accumulator.eventIds.add(eventId))
					for(final String ownRole : ownRoles)
						accumulator.ownRoles.merge(ownRole, 1, Integer::sum);
				accumulator.otherRoles.merge(participation.role(), 1, Integer::sum);
				for(final String ownRole : ownRoles)
					accumulator.pairs.add(ownRole + ">" + participation.role());
				accumulator.addEvent(info);
			}
		}

		final List<FanEntry> entries = new ArrayList<>(others.size());
		for(final Map.Entry<String, Accumulator> entry : others.entrySet())
			entries.add(toEntry(entry.getKey(), entry.getValue(), eventInfos, kinship.areRelated(subjectId, entry.getKey()),
				roleTotals.getOrDefault(entry.getKey(), 0)));
		entries.sort(ORDER);

		return new FanResult(subjectId, entries, suggestions(subjectId, entries, subjectEventIds, participantsByEvent, kinship));
	}


	private FanEntry toEntry(final String personId, final Accumulator accumulator, final Map<String, EventInfo> eventInfos,
			final boolean kin, final int roleTotal){
		final List<String> eventIds = new ArrayList<>(accumulator.eventIds);
		eventIds.sort(Comparator.comparing((String id) -> eventInfos.get(id).jdn(), Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Comparator.naturalOrder()));

		final Integer firstYear = (accumulator.years.isEmpty()? null: accumulator.years.first());
		final Integer lastYear = (accumulator.years.isEmpty()? null: accumulator.years.last());
		final boolean lasting = (accumulator.years.size() > 1);
		final int linkTypes = accumulator.pairs.size();
		final int score = 2 * eventIds.size() + Math.max(0, linkTypes - 1) + (lasting? 1: 0);
		return new FanEntry(personId, nameOf(personId), eventIds, linkTypes, score, accumulator.ownRoles, accumulator.otherRoles,
			firstYear, lastYear, new ArrayList<>(accumulator.places), kin, roleTotal);
	}

	private List<FanSuggestion> suggestions(final String subjectId, final List<FanEntry> entries, final List<String> subjectEventIds,
			final Map<String, List<Participation>> participantsByEvent, final Kinship kinship){
		final List<FanSuggestion> suggestions = new ArrayList<>();

		//two with the same role other than principal in the same event, not known to be related: they might be a couple
		final Map<String, Set<String>> coupleEvents = new TreeMap<>();
		for(final String eventId : subjectEventIds){
			final Map<String, Set<String>> byRole = new TreeMap<>();
			for(final Participation participation : participantsByEvent.get(eventId))
				if(!participation.role().isEmpty() && !ROLE_PRINCIPAL.equals(participation.role())
						&& !participation.personId().equals(subjectId) && isVisibleIndividual(participation.personId()))
					byRole.computeIfAbsent(participation.role(), k -> new TreeSet<>())
						.add(participation.personId());
			for(final Set<String> sameRole : byRole.values()){
				final List<String> people = new ArrayList<>(sameRole);
				for(int i = 0; i < people.size(); i ++)
					for(int j = i + 1; j < people.size(); j ++)
						if(mightBeCouple(people.get(i), people.get(j), kinship))
							coupleEvents.computeIfAbsent(people.get(i) + "\u0000" + people.get(j), k -> new LinkedHashSet<>())
								.add(eventId);
			}
		}
		for(final Map.Entry<String, Set<String>> entry : coupleEvents.entrySet()){
			final int separator = entry.getKey().indexOf('\u0000');
			suggestions.add(new FanSuggestion(FanSuggestion.Kind.POSSIBLE_COUPLE,
				List.of(entry.getKey().substring(0, separator), entry.getKey().substring(separator + 1)),
				new ArrayList<>(entry.getValue())));
		}

		//a person that recurs, with no known kinship: a candidate relative or fellow villager
		for(final FanEntry entry : entries)
			if(!entry.kin() && entry.sharedEvents() >= RECURRING_MINIMUM)
				suggestions.add(new FanSuggestion(FanSuggestion.Kind.RECURRING_WITHOUT_KINSHIP, List.of(entry.personId()),
					entry.eventIds()));
		return suggestions;
	}

	private boolean mightBeCouple(final String firstId, final String secondId, final Kinship kinship){
		if(kinship.areRelated(firstId, secondId))
			return false;

		final SexType first = sexOf(firstId);
		final SexType second = sexOf(secondId);
		return (second == SexType.UNKNOWN || first != second);
	}

	private SexType sexOf(final String individualId){
		final FLEFRecord record = model.getRecordById(individualId);
		return (record != null? PreferredSex.of(record, model): SexType.UNKNOWN);
	}


	private boolean isVisibleIndividual(final String id){
		final FLEFRecord record = model.getRecordById(id);
		return (IndividualReader.isOfType(record) && !PrivacyService.isHidden(model, record));
	}

	private String nameOf(final String individualId){
		final FLEFRecord record = model.getRecordById(individualId);
		if(record == null)
			return individualId;

		final String name = IndividualReader.displayText(record);
		return (StringUtils.isNotBlank(name)? name: individualId);
	}

	private EventInfo describeEvent(final String eventId){
		final FLEFRecord event = model.getRecordById(eventId);
		if(event == null)
			return new EventInfo(null, StringUtils.EMPTY);

		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		final TemporalSpan span = (dateStruct != null? DateNormalizer.normalize(dateStruct): null);
		NormalizedDate date = null;
		if(span != null)
			date = (span.start() != null? span.start(): span.end());
		return new EventInfo((date != null? date.jdn(): null), formatting.resolvePlaceName(event));
	}

	/** @return the year of a Julian day number, in the Julian calendar before the Gregorian one existed */
	static int yearOf(final long jdn){
		if(jdn >= GREGORIAN_START_JDN)
			return LocalDate.ofEpochDay(jdn - EPOCH_JDN).getYear();

		//the Julian calendar (Richards algorithm)
		final long c = jdn + 32_082L;
		final long d = (4L * c + 3L) / 1_461L;
		final long e = c - 1_461L * d / 4L;
		final long m = (5L * e + 2L) / 153L;
		return (int)(d - 4_800L + m / 10L);
	}

	private static String normalizeRole(final String role){
		return (role != null? role.trim().toLowerCase(Locale.ROOT): StringUtils.EMPTY);
	}


	private record Participation(String personId, String role){}

	/** When (the Julian day number, {@code null} if undated) and where an event is. */
	private record EventInfo(Long jdn, String place){}

	/** What is known so far about the link between the subject and another person. */
	private static final class Accumulator{

		private final Set<String> eventIds = new LinkedHashSet<>();
		private final Map<String, Integer> ownRoles = new LinkedHashMap<>();
		private final Map<String, Integer> otherRoles = new LinkedHashMap<>();
		private final Set<String> pairs = new HashSet<>();
		private final TreeSet<Integer> years = new TreeSet<>();
		private final Set<String> places = new LinkedHashSet<>();


		void addEvent(final EventInfo info){
			if(info.jdn() != null)
				years.add(yearOf(info.jdn()));
			if(StringUtils.isNotEmpty(info.place()))
				places.add(info.place());
		}

	}

	/** Who is a relative of whom, as far as the relationships say: spouses and partners, parents and children, siblings. */
	private static final class Kinship{

		private final Map<String, Set<String>> relatives = new HashMap<>();
		private final Map<String, Set<String>> parents = new HashMap<>();


		Kinship(final FLEFModel model){
			for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE))){
				final String type = RelationshipReader.extractType(relationship);
				final boolean child = (type != null && RelationshipReader.isTypeChild(type));
				if(!child && (type == null || !RelationshipReader.isTypePartner(type)))
					continue;

				final String subjectId = individualOf(relationship, RelationshipReader.TAG_SUBJECT);
				final String targetId = individualOf(relationship, RelationshipReader.TAG_TARGET);
				if(subjectId == null || targetId == null)
					continue;

				relatives.computeIfAbsent(subjectId, k -> new HashSet<>())
					.add(targetId);
				relatives.computeIfAbsent(targetId, k -> new HashSet<>())
					.add(subjectId);
				//the subject of a child relationship is the child
				if(child)
					parents.computeIfAbsent(subjectId, k -> new HashSet<>())
						.add(targetId);
			}
		}

		private static String individualOf(final FLEFRecord relationship, final String tag){
			final FLEFRecord field = FLEFRecordHelper.findChild(relationship, tag);
			final FLEFRecord ref = (field != null? field.getTheOnlyChild(): null);
			return (ref != null && RelationshipReader.TAG_INDIVIDUAL.equalsIgnoreCase(ref.getTag())? ref.getValue(): null);
		}

		boolean areRelated(final String firstId, final String secondId){
			if(relatives.getOrDefault(firstId, Set.of()).contains(secondId))
				return true;

			final Set<String> firstParents = parents.getOrDefault(firstId, Set.of());
			for(final String parent : parents.getOrDefault(secondId, Set.of()))
				if(firstParents.contains(parent))
					return true;
			return false;
		}

	}

}
