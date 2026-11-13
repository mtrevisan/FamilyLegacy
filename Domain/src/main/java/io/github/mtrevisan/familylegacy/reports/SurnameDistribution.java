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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.NameAnatomyService;
import io.github.mtrevisan.familylegacy.io.model.readers.NamePart;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.places.PlaceNameResolver;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * The surnames of the file, by the place the people they belong to come from and by the period they lived in: for each place, each
 * surname, how many individuals bear it and between which years they were born (or baptised).
 * <p>
 * It is the question a researcher takes to the archive of a place: which families are there, and when. The place and the year are the ones of
 * the birth; if the birth has none, the ones of the baptism or the christening, in this order (each of the two is looked for by itself:
 * a birth with a date and no place takes its place from the baptism). A surname written in different ways is listed in each of them:
 * the file says they are different, and it is for the researcher to say they are not.
 * <p>
 * Read only, and it judges nothing: an individual with no surname, or with no name, is not in the list.
 */
public final class SurnameDistribution{

	/** The events that say where and when a person comes from, in the order they are looked for. */
	private static final List<String> ORIGIN_EVENT_TYPES = List.of(EventReader.ENUM_TYPE_BIRTH, EventReader.CUSTOM_TYPE_BAPTISM,
		EventReader.CUSTOM_TYPE_CHRISTENING);


	/**
	 * A surname in a place.
	 *
	 * @param surname	the surname, as it is written the first time it is found
	 * @param individuals	how many individuals bear it
	 * @param firstYear	the first year they were born, {@code null} if no one has a date
	 * @param lastYear	the last year, {@code null} if no one has a date
	 * @param undated	how many of the individuals have no date
	 */
	public record Row(String surname, int individuals, Integer firstYear, Integer lastYear, int undated){}

	/**
	 * The surnames of a place.
	 *
	 * @param placeId	the place ({@code null} for the individuals with no place)
	 * @param placeName	the complete name of the place ("Borgo, Treviso, Veneto"), empty for no place
	 * @param individuals	how many individuals are there
	 * @param rows	the surnames, in alphabetical order
	 */
	public record PlaceGroup(String placeId, String placeName, int individuals, List<Row> rows){}

	/**
	 * @param individuals	how many individuals have a surname
	 * @param surnames	how many different surnames there are (as written, without capitals)
	 * @param places	the places, in alphabetical order, the individuals with no place the last
	 */
	public record Result(int individuals, int surnames, List<PlaceGroup> places){}


	private SurnameDistribution(){}


	/**
	 * @param model	the model
	 * @param index	the relations of the model (to find the events of an individual)
	 * @param visible	whether a record can be shown (the privacy policy of the report)
	 * @param locale	the locale the names are sorted in
	 * @return	the distribution
	 */
	public static Result of(final FLEFModel model, final RelationIndex index, final Predicate<FLEFRecord> visible, final Locale locale){
		final PlaceNameResolver places = new PlaceNameResolver(model);
		//place -> surname (without capitals) -> its tally
		final Map<String, Map<String, Tally>> byPlace = new LinkedHashMap<>();
		final Set<String> surnames = new HashSet<>();
		int individuals = 0;

		for(final FLEFRecord person : model.getRecordsByType(IndividualReader.TYPE)){
			if(!visible.test(person))
				continue;

			final String surname = surnameOf(person);
			if(surname == null)
				continue;

			individuals ++;
			final String key = surname.toLowerCase(locale);
			surnames.add(key);
			final Origin origin = originOf(person, index, visible);
			final String placeId = (origin.placeId() != null? origin.placeId(): StringUtils.EMPTY);
			byPlace.computeIfAbsent(placeId, k -> new LinkedHashMap<>())
				.computeIfAbsent(key, k -> new Tally(surname))
				.add(origin.year());
		}

		final Comparator<String> order = ReportCollation.of(locale);
		final List<PlaceGroup> groups = new ArrayList<>(byPlace.size());
		for(final Map.Entry<String, Map<String, Tally>> entry : byPlace.entrySet()){
			final boolean unknown = entry.getKey().isEmpty();
			final List<Row> rows = new ArrayList<>(entry.getValue().size());
			int inPlace = 0;
			for(final Tally tally : entry.getValue().values()){
				rows.add(tally.toRow());
				inPlace += tally.individuals;
			}
			rows.sort(Comparator.comparing(Row::surname, order).thenComparing(Row::surname));
			groups.add(new PlaceGroup((unknown? null: entry.getKey()), (unknown? StringUtils.EMPTY: places.fullName(entry.getKey())), inPlace,
				rows));
		}
		//the places by name, the individuals with no place the last
		groups.sort(Comparator.comparing((final PlaceGroup group) -> group.placeId() == null)
			.thenComparing(PlaceGroup::placeName, order)
			.thenComparing(group -> StringUtils.defaultString(group.placeId())));
		return new Result(individuals, surnames.size(), groups);
	}


	/** The place and the year a person comes from. */
	private record Origin(String placeId, Integer year){}

	private static Origin originOf(final FLEFRecord person, final RelationIndex index, final Predicate<FLEFRecord> visible){
		final List<FLEFRecord> events = index.eventsOf(person);
		String placeId = null;
		Integer year = null;
		for(final String type : ORIGIN_EVENT_TYPES)
			for(final FLEFRecord event : events){
				if(!type.equalsIgnoreCase(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)) || !visible.test(event))
					continue;

				if(placeId == null)
					placeId = EventHelper.eventPlaceId(event);
				if(year == null)
					year = GenealogicalDateHelper.yearOrNull(event);
			}
		return new Origin(placeId, year);
	}

	/** @return	The family name of the first name of the individual (with its prefix: "de Marco"), {@code null} if there is none */
	static String surnameOf(final FLEFRecord person){
		final List<Name> names = NameAnatomyService.extractForIndividual(person);
		if(names.isEmpty())
			return null;

		final StringJoiner surname = new StringJoiner(StringUtils.SPACE);
		for(final NamePart part : names.getFirst().parts()){
			final String value = part.value();
			if(StringUtils.isBlank(value))
				continue;

			if(NameReader.ENUM_PART_TYPE_FAMILY.equals(part.type()) || NameReader.ENUM_PART_TYPE_FAMILY_PREFIX.equals(part.type()))
				surname.add(value.strip());
		}
		final String result = surname.toString();
		return (!result.isEmpty()? result: null);
	}


	/** What is known of a surname in a place, while it is being counted. */
	private static final class Tally{

		private final String surname;
		private int individuals;
		private int undated;
		private Integer firstYear;
		private Integer lastYear;


		private Tally(final String surname){
			this.surname = surname;
		}


		private void add(final Integer year){
			individuals ++;
			if(year == null){
				undated ++;
				return;
			}

			firstYear = (firstYear != null? Math.min(firstYear, year): year);
			lastYear = (lastYear != null? Math.max(lastYear, year): year);
		}

		private Row toRow(){
			return new Row(surname, individuals, firstYear, lastYear, undated);
		}

	}

}
