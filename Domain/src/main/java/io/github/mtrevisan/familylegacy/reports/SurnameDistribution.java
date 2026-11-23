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
import io.github.mtrevisan.familylegacy.quality.NameSimilarity;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import org.apache.commons.lang3.StringUtils;

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
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Predicate;


/**
 * The surnames of the file, by the place the people they belong to come from and by the period they lived in: for each place, each
 * surname, how many individuals bear it and between which years they were born.
 * <p>
 * It is the question a researcher takes to the archive of a place: which families are there, and when. The place and the year are the ones of
 * the birth. The place can be taken at a higher level of the hierarchy (the province, the region...), the period can be split by century,
 * and the surnames can be those of all the names of an individual (a married woman is also in her maiden name). A surname written in
 * different ways is listed in each of them: the file says they are different, and it is for the researcher to say they are not;
 * {@link #similarSpellings} puts them side by side.
 * <p>
 * Read only, and it judges nothing: an individual with no surname, or with no name, is not in the list.
 */
public final class SurnameDistribution{

	/** The events that say where and when a person comes from, in the order they are looked for. */
	private static final List<String> ORIGIN_EVENT_TYPES = List.of(EventReader.ENUM_TYPE_BIRTH);
	private static final String KEY_SEPARATOR = "\u0000";


	/**
	 * What the distribution is made of.
	 *
	 * @param level	the level of the place hierarchy the individuals are grouped by
	 * @param period	how the period of a surname is split
	 * @param allNames	whether the surnames of all the names of an individual count, or only the one of the first name
	 */
	public record Options(SurnamePlaceLevel level, SurnamePeriod period, boolean allNames){

		/** The place of the birth, the whole period of a surname, all the names. */
		public static Options defaults(){
			return new Options(SurnamePlaceLevel.PLACE, SurnamePeriod.WHOLE, true);
		}

	}

	/**
	 * A surname in a place.
	 *
	 * @param surname	the surname, as it is written the first time it is found
	 * @param individuals	how many individuals bear it
	 * @param firstYear	the first year they were born, {@code null} if no one has a date
	 * @param lastYear	the last year, {@code null} if no one has a date
	 * @param undated	how many of the individuals have no date
	 * @param century	the century of the birth if the period is split by century, {@code null} for the whole period or for the line of
	 * 	the individuals with no date
	 */
	public record Row(String surname, int individuals, Integer firstYear, Integer lastYear, int undated, Integer century){}

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
	 * @param spellings	for each surname (without capitals) its spelling as it is written the first time, how many individuals bear it and
	 * 	the names of the places they are in
	 */
	public record Result(int individuals, int surnames, List<PlaceGroup> places, Map<String, Spelling> spellings){}

	/**
	 * A surname in the whole file.
	 *
	 * @param surname	the surname, as it is written the first time it is found
	 * @param individuals	how many individuals bear it
	 * @param places	the names of the places they are in (the individuals with no place are not in any)
	 */
	public record Spelling(String surname, int individuals, Set<String> places){}

	/**
	 * Surnames that may be the same one written in different ways.
	 *
	 * @param spellings	the spellings, the most frequent first (there are at least two)
	 * @param individuals	how many individuals bear one of them
	 * @param places	the names of the places where one of them is, in alphabetical order
	 */
	public record SimilarGroup(List<Spelling> spellings, int individuals, List<String> places){}


	private SurnameDistribution(){}


	/**
	 * The surnames of the first name of each individual, by the place of the birth and the whole period.
	 *
	 * @param model	the model
	 * @param index	the relations of the model (to find the events of an individual)
	 * @param visible	whether a record can be shown (the privacy policy of the report)
	 * @param locale	the locale the names are sorted in
	 * @return	the distribution
	 */
	public static Result of(final FLEFModel model, final RelationIndex index, final Predicate<FLEFRecord> visible, final Locale locale){
		return of(model, index, visible, locale, new Options(SurnamePlaceLevel.PLACE, SurnamePeriod.WHOLE, false));
	}

	/**
	 * @param model	the model
	 * @param index	the relations of the model (to find the events of an individual)
	 * @param visible	whether a record can be shown (the privacy policy of the report)
	 * @param locale	the locale the names are sorted in
	 * @param options	what the distribution is made of
	 * @return	the distribution
	 */
	public static Result of(final FLEFModel model, final RelationIndex index, final Predicate<FLEFRecord> visible, final Locale locale,
			final Options options){
		final PlaceNameResolver places = new PlaceNameResolver(model);
		//place -> surname (without capitals) and century -> its tally
		final Map<String, Map<String, Tally>> byPlace = new LinkedHashMap<>();
		//place -> the individuals in it (one that has two surnames counts once)
		final Map<String, Set<String>> peopleByPlace = new HashMap<>();
		final Map<String, Set<String>> placesBySurname = new HashMap<>();
		final Map<String, Tally> bySurname = new HashMap<>();
		final Map<String, String> placeNames = new HashMap<>();
		int individuals = 0;

		for(final FLEFRecord person : model.getRecordsByType(IndividualReader.TYPE)){
			if(!visible.test(person))
				continue;

			final Map<String, String> surnames = surnamesOf(person, options.allNames(), locale);
			if(surnames.isEmpty())
				continue;

			individuals ++;
			final Origin origin = originOf(person, index, visible);
			final String placeId = groupOf(origin.placeId(), options.level(), places);
			final String groupKey = (placeId != null? placeId: StringUtils.EMPTY);
			final Integer century = (origin.year() != null && options.period() == SurnamePeriod.CENTURY? Integer.valueOf(centuryOf(origin.year())): null);
			peopleByPlace.computeIfAbsent(groupKey, k -> new HashSet<>())
				.add(person.getId());
			final Map<String, Tally> group = byPlace.computeIfAbsent(groupKey, k -> new LinkedHashMap<>());
			final String placeName = (placeId != null? placeNames.computeIfAbsent(placeId, places::fullName): null);
			for(final Map.Entry<String, String> surname : surnames.entrySet()){
				group.computeIfAbsent(surname.getKey() + KEY_SEPARATOR + (century != null? century: StringUtils.EMPTY),
						k -> new Tally(surname.getValue(), century))
					.add(origin.year());
				bySurname.computeIfAbsent(surname.getKey(), k -> new Tally(surname.getValue(), null))
					.add(origin.year());
				if(placeName != null)
					placesBySurname.computeIfAbsent(surname.getKey(), k -> new LinkedHashSet<>())
						.add(placeName);
			}
		}

		final Comparator<String> order = ReportCollation.of(locale);
		final List<PlaceGroup> groups = new ArrayList<>(byPlace.size());
		for(final Map.Entry<String, Map<String, Tally>> entry : byPlace.entrySet()){
			final boolean unknown = entry.getKey().isEmpty();
			final List<Row> rows = new ArrayList<>(entry.getValue().size());
			for(final Tally tally : entry.getValue().values())
				rows.add(tally.toRow());
			rows.sort(Comparator.comparing(Row::surname, order)
				.thenComparing(Row::surname)
				.thenComparing(Row::century, Comparator.nullsLast(Comparator.naturalOrder())));
			groups.add(new PlaceGroup((unknown? null: entry.getKey()), (unknown? StringUtils.EMPTY: places.fullName(entry.getKey())),
				peopleByPlace.get(entry.getKey()).size(), rows));
		}
		//the places by name, the individuals with no place the last
		groups.sort(Comparator.comparing((final PlaceGroup group) -> group.placeId() == null)
			.thenComparing(PlaceGroup::placeName, order)
			.thenComparing(group -> StringUtils.defaultString(group.placeId())));

		final Map<String, Spelling> spellings = new TreeMap<>();
		for(final Map.Entry<String, Tally> entry : bySurname.entrySet())
			spellings.put(entry.getKey(), new Spelling(entry.getValue().surname, entry.getValue().individuals,
				placesBySurname.getOrDefault(entry.getKey(), Set.of())));
		return new Result(individuals, spellings.size(), groups, spellings);
	}

	/**
	 * The surnames that may be the same one written in different ways, in the whole file (by the sound and the letters, as the identity assistant compares names; a chain of similar ones is one group).
	 *
	 * @param result	the distribution
	 * @param locale	the locale the places are sorted in
	 * @return	the groups of at least two spellings, the most frequent first
	 */
	public static List<SimilarGroup> similarSpellings(final Result result, final Locale locale){
		//the surnames are compared only with the ones that begin with the same sound (the skeleton), to keep the comparisons few
		final Map<Character, List<String>> blocks = new TreeMap<>();
		final Map<String, String> normalized = new HashMap<>();
		for(final String key : result.spellings().keySet()){
			final String name = StringUtils.stripAccents(key);
			normalized.put(key, name);
			final String skeleton = NameSimilarity.skeleton(name);
			blocks.computeIfAbsent((skeleton.isEmpty()? ' ': skeleton.charAt(0)), k -> new ArrayList<>())
				.add(key);
		}

		final Map<String, String> parent = new HashMap<>();
		for(final List<String> block : blocks.values())
			for(int i = 0; i < block.size(); i ++)
				for(int j = i + 1; j < block.size(); j ++){
					final NameSimilarity.Level level = NameSimilarity.level(normalized.get(block.get(i)), normalized.get(block.get(j)));
					if(level == NameSimilarity.Level.SIMILAR || level == NameSimilarity.Level.EXACT)
						parent.put(root(parent, block.get(i)), root(parent, block.get(j)));
				}

		final Map<String, List<Spelling>> byRoot = new TreeMap<>();
		for(final Map.Entry<String, Spelling> entry : result.spellings().entrySet())
			byRoot.computeIfAbsent(root(parent, entry.getKey()), k -> new ArrayList<>())
				.add(entry.getValue());

		final Comparator<String> order = ReportCollation.of(locale);
		final List<SimilarGroup> out = new ArrayList<>(0);
		for(final List<Spelling> spellings : byRoot.values()){
			if(spellings.size() < 2)
				continue;

			int individuals = 0;
			final Set<String> places = new HashSet<>();
			for(final Spelling spelling : spellings){
				individuals += spelling.individuals();
				places.addAll(spelling.places());
			}
			spellings.sort(Comparator.comparingInt(Spelling::individuals).reversed()
				.thenComparing(Spelling::surname, order));
			out.add(new SimilarGroup(List.copyOf(spellings), individuals, places.stream()
				.sorted(order)
				.toList()));
		}
		out.sort(Comparator.comparingInt(SimilarGroup::individuals).reversed()
			.thenComparing(group -> group.spellings().getFirst().surname(), order));
		return out;
	}

	/** @return	The representative of the set the surname is in (union-find, with the path compressed). */
	private static String root(final Map<String, String> parent, final String key){
		String root = key;
		while(parent.containsKey(root) && !parent.get(root).equals(root))
			root = parent.get(root);
		String current = key;
		while(!current.equals(root)){
			final String next = parent.get(current);
			parent.put(current, root);
			current = next;
		}
		return root;
	}

	/** @return	The place the individuals of a place are grouped by: the nearest place of the level that contains it, the place itself if none. */
	private static String groupOf(final String placeId, final SurnamePlaceLevel level, final PlaceNameResolver places){
		if(placeId == null || level == SurnamePlaceLevel.PLACE)
			return placeId;

		final String container = places.containerOfType(placeId, level.types());
		return (container != null? container: placeId);
	}

	/** @return	The century of a year (the 1st is from 1 to 100). */
	static int centuryOf(final int year){
		return Math.floorDiv(year - 1, 100) + 1;
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
		return (!names.isEmpty()? surnameOf(names.getFirst()): null);
	}

	/**
	 * @param person	the individual
	 * @param allNames	whether all the names count, or only the first
	 * @param locale	the locale the surnames are put without capitals in
	 * @return	the surnames of the individual, without capitals, each with the way it is written the first time (in the order of the names)
	 */
	private static Map<String, String> surnamesOf(final FLEFRecord person, final boolean allNames, final Locale locale){
		final Map<String, String> out = new LinkedHashMap<>(1);
		final List<Name> names = NameAnatomyService.extractForIndividual(person);
		for(final Name name : (allNames? names: names.stream().limit(1).toList())){
			final String surname = surnameOf(name);
			if(surname != null)
				out.putIfAbsent(surname.toLowerCase(locale), surname);
		}
		return out;
	}

	private static String surnameOf(final Name name){
		final StringJoiner surname = new StringJoiner(StringUtils.SPACE);
		for(final NamePart part : name.parts()){
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
		private final Integer century;
		private int individuals;
		private int undated;
		private Integer firstYear;
		private Integer lastYear;


		private Tally(final String surname, final Integer century){
			this.surname = surname;
			this.century = century;
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
			return new Row(surname, individuals, firstYear, lastYear, undated, century);
		}

	}

}
