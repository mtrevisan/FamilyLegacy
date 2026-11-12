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
package io.github.mtrevisan.familylegacy.comparison;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.events.EventSummary;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.NameAnatomyService;
import io.github.mtrevisan.familylegacy.io.model.readers.NamePart;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * The facts of two or more individuals, one under the other, to tell by eye whether they are the same person: the names, the sex, when and
 * where they were born, baptised, died and buried, their parents, spouses and children (by name, not by identifier: two records of the same
 * person can have parents that are two records too), their occupations, residences and other attributes, and the sources that cite them.
 * <p>
 * The duplicate finder says <em>that</em> two individuals look alike and why in a few words; this says <em>what</em> each one has. It is the
 * answer to "which of these three Giovanni Masutti is mine": for each fact the individuals that differ are told (a different date, a different
 * father) and the ones with something missing (a place on one side only) too.
 * <p>
 * Read only. A fact an individual has several of (two residences, three children) is written once, in order, separated by a semicolon;
 * a fact no one has is not a row.
 */
public final class IndividualFacts{

	/** How the individuals stand on a fact. */
	public enum State{
		/** They say the same (or only one says something, with no one to contradict it: not with two). */
		SAME,
		/** At least two of them say something, and it is not the same. */
		DIFFERENT,
		/** Some say something and some say nothing. */
		MISSING
	}

	/**
	 * A fact.
	 *
	 * @param key	what it is (the end of the key {@code text.facts.row.<key>} of its label)
	 * @param label	what it is, as text
	 * @param values	what each individual says (empty text if nothing), in the order of the individuals
	 * @param state	how they stand
	 */
	public record Row(String key, String label, List<String> values, State state){}

	/**
	 * @param individuals	the identifiers of the individuals, in order
	 * @param names	the name of each one, with its identifier ("Giovanni Masutti [I12]")
	 * @param rows	the facts that at least one has
	 */
	public record Comparison(List<String> individuals, List<String> names, List<Row> rows){

		/** @return	whether there is nothing to compare: fewer than two individuals */
		public boolean isEmpty(){
			return individuals.size() < 2;
		}
	}


	/** The facts, in the order of the rows. */
	private static final List<String> KEYS = List.of("name", "sex", "birth_date", "birth_place", "baptism", "death_date", "death_place",
		"burial", "parents", "spouses", "children", "occupations", "residences", "attributes", "sources");
	private static final String SEPARATOR = "; ";
	private static final String SEPARATOR_OF_NAMES = " / ";


	private IndividualFacts(){}


	/**
	 * @param model	the model
	 * @param individualIds	the individuals to compare, in the order of the columns; what is not an individual of the model is left out
	 * @return	the comparison (empty if fewer than two individuals remain)
	 */
	public static Comparison compare(final FLEFModel model, final List<String> individualIds){
		final List<FLEFRecord> people = new ArrayList<>(individualIds.size());
		final Set<String> seen = new LinkedHashSet<>();
		for(final String id : individualIds){
			final FLEFRecord person = (id != null? model.getRecordById(id): null);
			if(IndividualReader.isOfType(person) && seen.add(id))
				people.add(person);
		}
		if(people.size() < 2)
			return new Comparison(List.of(), List.of(), List.of());

		final RelationIndex index = RelationIndex.build(model);
		final Map<String, List<EventHelper.Participation>> participations = EventHelper.participationsByEvent(model);

		final List<Map<String, String>> facts = new ArrayList<>(people.size());
		final List<String> ids = new ArrayList<>(people.size());
		final List<String> names = new ArrayList<>(people.size());
		for(final FLEFRecord person : people){
			facts.add(factsOf(model, index, participations, person));
			ids.add(person.getId());
			names.add(nameOf(person, person.getId()) + " [" + person.getId() + "]");
		}

		final List<Row> rows = new ArrayList<>(KEYS.size());
		for(final String key : KEYS){
			final List<String> values = new ArrayList<>(people.size());
			for(final Map<String, String> fact : facts)
				values.add(fact.getOrDefault(key, StringUtils.EMPTY));
			if(values.stream().allMatch(StringUtils::isEmpty))
				continue;

			rows.add(new Row(key, I18N.t("text.facts.row." + key), values, stateOf(values)));
		}
		return new Comparison(ids, names, rows);
	}

	/** @return	how the individuals stand on a fact: the values are compared without capitals and with one space between the words */
	static State stateOf(final List<String> values){
		final Set<String> distinct = new LinkedHashSet<>();
		boolean empty = false;
		for(final String value : values){
			if(StringUtils.isBlank(value))
				empty = true;
			else
				distinct.add(normalized(value));
		}
		if(distinct.size() > 1)
			return State.DIFFERENT;
		return (empty && !distinct.isEmpty()? State.MISSING: State.SAME);
	}

	private static String normalized(final String value){
		return StringUtils.normalizeSpace(value).toLowerCase(Locale.ROOT);
	}


	/** The facts of an individual by key (a fact it has not is not in the map). */
	private static Map<String, String> factsOf(final FLEFModel model, final RelationIndex index,
			final Map<String, List<EventHelper.Participation>> participations, final FLEFRecord person){
		final Map<String, String> facts = new LinkedHashMap<>();
		final String id = person.getId();

		final List<String> allNames = new ArrayList<>();
		for(final Name name : NameAnatomyService.extractForIndividual(person)){
			final String text = join(name);
			if(!text.isEmpty() && !allNames.contains(text))
				allNames.add(text);
		}
		put(facts, "name", String.join(SEPARATOR_OF_NAMES, allNames));
		final SexType sex = PreferredSex.of(person, model);
		if(sex != SexType.UNKNOWN)
			put(facts, "sex", sex.getRawSex());

		//the events the individual is the main one of (a witness of a birth is not born in it)
		final FLEFRecord birth = eventOf(index, participations, person, List.of(EventReader.ENUM_TYPE_BIRTH));
		put(facts, "birth_date", EventHelper.eventDateRaw(birth));
		put(facts, "birth_place", EventSummary.placeName(birth, model));
		final FLEFRecord baptism = eventOf(index, participations, person, List.of(EventReader.CUSTOM_TYPE_BAPTISM, EventReader.CUSTOM_TYPE_CHRISTENING));
		put(facts, "baptism", dateAndPlace(model, baptism));
		final FLEFRecord death = eventOf(index, participations, person, List.of(EventReader.ENUM_TYPE_DEATH));
		put(facts, "death_date", EventHelper.eventDateRaw(death));
		put(facts, "death_place", EventSummary.placeName(death, model));
		final FLEFRecord burial = eventOf(index, participations, person, List.of(EventReader.ENUM_TYPE_BURIAL));
		put(facts, "burial", dateAndPlace(model, burial));

		put(facts, "parents", namesOf(index.parentsOf(person)));
		put(facts, "spouses", namesOf(index.spousesOf(person)));
		put(facts, "children", namesOf(index.childrenOf(person)));

		final Collection<String> occupations = new TreeSet<>();
		final Collection<String> residences = new TreeSet<>();
		final Collection<String> others = new TreeSet<>();
		for(final FLEFRecord attribute : index.attributesOf(person)){
			final String type = FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE);
			final String value = StringUtils.trimToNull(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE));
			//the sex is the fact of its own row (the preferred one)
			if(value == null || IndividualAttributeReader.ENUM_TYPE_SEX.equals(type))
				continue;

			if(IndividualAttributeReader.ENUM_TYPE_OCCUPATION.equals(type))
				occupations.add(value);
			else if(IndividualAttributeReader.ENUM_TYPE_RESIDENCE.equals(type))
				residences.add(value);
			else
				others.add((type != null? type.replace('_', ' ') + ": ": StringUtils.EMPTY) + value);
		}
		put(facts, "occupations", String.join(SEPARATOR, occupations));
		put(facts, "residences", String.join(SEPARATOR, residences));
		put(facts, "attributes", String.join(SEPARATOR, others));

		final Set<String> titles = new TreeSet<>();
		collectSourceTitles(model, person, titles);
		for(final FLEFRecord event : index.eventsOf(person))
			collectSourceTitles(model, event, titles);
		put(facts, "sources", String.join(SEPARATOR, titles));
		return facts;
	}

	private static void put(final Map<String, String> facts, final String key, final String value){
		if(StringUtils.isNotBlank(value))
			facts.put(key, value.strip());
	}

	/** @return	The first event of one of the types that the individual is the main one of ({@code null} if none). */
	private static FLEFRecord eventOf(final RelationIndex index, final Map<String, List<EventHelper.Participation>> participations,
			final FLEFRecord person, final List<String> types){
		for(final String type : types)
			for(final FLEFRecord event : index.eventsOf(person)){
				if(!type.equalsIgnoreCase(EventHelper.eventType(event)))
					continue;

				for(final EventHelper.Participation participation : participations.getOrDefault(event.getId(), List.of()))
					if(person.getId().equals(participation.participantId())
							&& (StringUtils.isBlank(participation.role()) || EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(participation.role())))
						return event;
			}
		return null;
	}

	private static String dateAndPlace(final FLEFModel model, final FLEFRecord event){
		if(event == null)
			return null;

		final StringBuilder text = new StringBuilder();
		final String date = EventHelper.eventDateRaw(event);
		if(date != null)
			text.append(date);
		final String place = EventSummary.placeName(event, model);
		if(StringUtils.isNotBlank(place)){
			if(!text.isEmpty())
				text.append(", ");
			text.append(place);
		}
		return text.toString();
	}

	/** @return	The names (without identifiers: they are compared with the ones of another individual), in alphabetical order. */
	private static String namesOf(final List<FLEFRecord> people){
		final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for(final FLEFRecord person : people)
			names.add(nameOf(person, person.getId()));
		return String.join(SEPARATOR, names);
	}

	/** @return	The first name of the individual as text ("Giovanni Masutti"), the fallback if it has none. */
	private static String nameOf(final FLEFRecord person, final String fallback){
		final List<Name> names = NameAnatomyService.extractForIndividual(person);
		if(!names.isEmpty()){
			final String text = join(names.getFirst());
			if(!text.isEmpty())
				return text;
		}
		return fallback;
	}

	private static String join(final Name name){
		final StringBuilder text = new StringBuilder();
		for(final NamePart part : name.parts()){
			final String value = part.value();
			if(StringUtils.isBlank(value))
				continue;

			if(!text.isEmpty())
				text.append(' ');
			text.append(value.strip());
		}
		return text.toString();
	}

	private static void collectSourceTitles(final FLEFModel model, final FLEFRecord record, final Set<String> titles){
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_SOURCE)){
			final String sourceId = SourceCitationReader.extractSource(citation);
			final FLEFRecord source = (sourceId != null? model.getRecordById(sourceId): null);
			if(source != null){
				final String title = SourceHelper.sourceTitle(source);
				if(StringUtils.isNotBlank(title))
					titles.add(title.strip());
			}
		}
	}

}
