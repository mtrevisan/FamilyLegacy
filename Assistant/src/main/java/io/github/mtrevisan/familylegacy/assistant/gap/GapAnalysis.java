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
package io.github.mtrevisan.familylegacy.assistant.gap;

import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.Sentences;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * The Research Gap Analysis: it gathers, in one place, what is missing from the research (a birth, a death, a source, a piece of the
 * chronology, a settled identity) and, for each gap, what to do about it: the searches to make (which record, where, for which years)
 * and the repositories that may keep it (the ones of the tree at the places of the individual, then the kinds of archive that usually
 * hold that record).
 * <p>
 * It adds nothing to the findings of the other assistants: it groups them by the gap they are about and it works out the suggestions
 * from what is known of the individuals. The tasks come from the findings ({@code TaskProposals}). Everything is a suggestion, by
 * rules of thumb: nothing is written to the file.
 * </p>
 */
public final class GapAnalysis{

	/** The years from the birth of a parent to the birth of a child that are plausible (the youngest and the oldest age at the birth). */
	private static final int YOUNGEST_PARENT = 16;
	private static final int OLDEST_PARENT = 50;
	/** The years before the first child within which the marriage of the parents is looked for. */
	private static final int MARRIAGE_BEFORE_CHILD = 10;
	/** The age that a death is searched up to. */
	private static final int LONGEST_LIFE = 110;
	/** The age a person is taken to appear in a census household (a census names the household of the time). */
	private static final int CENSUS_AGE = 90;
	private static final int MAX_PLACES = 3;
	private static final int MAX_REPOSITORIES = 5;
	private static final String DASH = "–";


	/** What a gap is about. */
	public enum Gap{

		MISSING_BIRTH,
		MISSING_DEATH,
		MISSING_SOURCE,
		INCOMPLETE_CHRONOLOGY,
		IDENTITY_PROBLEM;


		/**
		 * @param kind the kind of a finding ({@code Diagnostic#kind()})
		 * @return the gap the finding is about, or {@code null} if it is not about one of them
		 */
		public static Gap of(final String kind){
			if(kind == null)
				return null;

			if(kind.equals("gap.no_birth") || kind.equals("gap.no_birth_place") || kind.equals("planner.brick_wall"))
				return MISSING_BIRTH;
			if(kind.startsWith("gap.no_death"))
				return MISSING_DEATH;
			if(kind.equals("gap.unsourced_vital_event"))
				return MISSING_SOURCE;
			if(kind.equals("gap.census_without_residence") || kind.equals("gap.children_without_marriage"))
				return INCOMPLETE_CHRONOLOGY;
			if(kind.startsWith("identity.possible_duplicate") || kind.equals("conclusion.unresolved_identity"))
				return IDENTITY_PROBLEM;
			return null;
		}

		/** @return the name of the gap */
		public String label(){
			return I18N.t("gap.analysis.gap." + name().toLowerCase(Locale.ROOT));
		}

	}

	/**
	 * A gap with what to do about it.
	 *
	 * @param gap          what is missing
	 * @param finding      the finding that tells it
	 * @param persons      the names of the individuals it is about
	 * @param searches     the searches to make, the most useful first
	 * @param repositories where the records may be kept: the repositories of the tree at the places of the individuals, then the kinds
	 *                     of archive that usually hold the record
	 */
	public record Entry(Gap gap, Diagnostic finding, List<String> persons, List<String> searches, List<String> repositories){

		public Entry{
			persons = List.copyOf(persons);
			searches = List.copyOf(searches);
			repositories = List.copyOf(repositories);
		}

	}


	private GapAnalysis(){}


	/**
	 * @param findings the findings of the assistants, in the order they are shown
	 * @param data     the research
	 * @return the gaps among the findings, by the kind of gap and then in the order of the findings, each with its searches and its
	 * repositories
	 */
	public static List<Entry> analyse(final List<Diagnostic> findings, final GapData data){
		final List<Entry> entries = new ArrayList<>();
		for(final Gap gap : Gap.values())
			for(final Diagnostic finding : findings)
				if(gap == Gap.of(finding.kind()))
					entries.add(entry(gap, finding, data));
		return entries;
	}

	private static Entry entry(final Gap gap, final Diagnostic finding, final GapData data){
		final List<GapData.Person> persons = personsOf(finding, data);
		final List<String> searches = switch(gap){
			case MISSING_BIRTH -> birthSearches(persons, data);
			case MISSING_DEATH -> deathSearches(persons, data);
			case MISSING_SOURCE -> sourceSearches(finding, persons, data);
			case INCOMPLETE_CHRONOLOGY -> chronologySearches(finding, persons, data);
			case IDENTITY_PROBLEM -> identitySearches(persons);
		};
		return new Entry(gap, finding, persons.stream().map(GapData.Person::name).toList(), searches, repositories(gap, persons));
	}

	/** @return the individuals a finding is about (an event stands for its principals) */
	private static List<GapData.Person> personsOf(final Diagnostic finding, final GapData data){
		final Set<String> ids = new LinkedHashSet<>();
		for(final String id : finding.recordIds()){
			if(data.persons().containsKey(id))
				ids.add(id);
			else
				data.events().stream()
					.filter(event -> event.id().equals(id))
					.forEach(event -> ids.addAll(event.principals()));
		}
		return ids.stream()
			.map(data.persons()::get)
			.filter(person -> person != null)
			.toList();
	}


	private static List<String> birthSearches(final List<GapData.Person> persons, final GapData data){
		final List<String> searches = new ArrayList<>();
		for(final GapData.Person person : persons){
			final int[] window = birthWindow(person, data);
			final String where = places(person);
			searches.add(searchAt("gap.analysis.search.birth", window, where, person.name()));
			searches.add(search("gap.analysis.search.birth.family", window, person.name()));
		}
		return searches;
	}

	private static List<String> deathSearches(final List<GapData.Person> persons, final GapData data){
		final List<String> searches = new ArrayList<>();
		for(final GapData.Person person : persons){
			final int[] window = deathWindow(person, data);
			final String where = places(person);
			searches.add(searchAt("gap.analysis.search.death", window, where, person.name()));
			searches.add(search("gap.analysis.search.death.estate", window, person.name()));
		}
		return searches;
	}

	private static List<String> sourceSearches(final Diagnostic finding, final List<GapData.Person> persons, final GapData data){
		final String type = data.events().stream()
			.filter(event -> finding.recordIds().contains(event.id()))
			.map(GapData.Event::type)
			.findFirst()
			.orElse(null);
		final String register = switch(type == null? "": type){
			case EventReader.ENUM_TYPE_BIRTH -> "gap.analysis.search.source.birth";
			case EventReader.ENUM_TYPE_DEATH -> "gap.analysis.search.source.death";
			case EventReader.ENUM_TYPE_MARRIAGE -> "gap.analysis.search.source.marriage";
			default -> "gap.analysis.search.source";
		};

		final List<String> searches = new ArrayList<>();
		for(final GapData.Person person : persons){
			final String where = places(person);
			searches.add(searchAt(register, null, where, person.name()));
		}
		if(persons.isEmpty())
			searches.add(I18N.t("gap.analysis.search.source.generic"));
		searches.add(I18N.t("gap.analysis.search.source.second_copy"));
		return searches;
	}

	private static List<String> chronologySearches(final Diagnostic finding, final List<GapData.Person> persons, final GapData data){
		final List<String> searches = new ArrayList<>();
		if(finding.kind().equals("gap.children_without_marriage")){
			//the first two are the couple, the others their children
			final List<GapData.Person> couple = persons.subList(0, Math.min(2, persons.size()));
			Integer firstChild = null;
			for(final GapData.Person child : persons.subList(couple.size(), persons.size()))
				if(child.birthYear() != null && (firstChild == null || child.birthYear() < firstChild))
					firstChild = child.birthYear();
			final int[] window = (firstChild != null? new int[]{firstChild - MARRIAGE_BEFORE_CHILD, firstChild}: null);
			final String names = Sentences.list(couple.stream()
				.map(GapData.Person::name)
				.toList());
			final String where = places(couple);
			searches.add(searchAt("gap.analysis.search.marriage", window, where, names));
			return searches;
		}

		for(final GapData.Person person : persons){
			final int[] window = lifeWindow(person, data);
			final String where = places(person);
			searches.add(searchAt("gap.analysis.search.census", window, where, person.name()));
		}
		return searches;
	}

	private static List<String> identitySearches(final List<GapData.Person> persons){
		final List<String> searches = new ArrayList<>();
		if(persons.size() > 1)
			searches.add(I18N.tf("gap.analysis.search.identity.compare", Sentences.list(persons.stream()
				.map(GapData.Person::name)
				.toList())));
		searches.add(I18N.t("gap.analysis.search.identity.tell_apart"));
		final String where = places(persons);
		if(where != null)
			searches.add(I18N.tf("gap.analysis.search.identity.places", where));
		return searches;
	}


	/**
	 * @return the years the birth of an individual with no birth recorded may fall in: the year, if they have one, or the years that the
	 * first child, or the parents, allow (and the death, if it is known); {@code null} if the relatives say nothing
	 */
	static int[] birthWindow(final GapData.Person person, final GapData data){
		if(person.birthYear() != null)
			return new int[]{person.birthYear(), person.birthYear()};

		Integer from = null;
		Integer to = null;
		for(final GapData.ParentLink link : data.parents()){
			if(link.parentId().equals(person.id())){
				final GapData.Person child = data.persons().get(link.childId());
				if(child != null && child.birthYear() != null){
					from = max(from, child.birthYear() - OLDEST_PARENT);
					to = min(to, child.birthYear() - YOUNGEST_PARENT);
				}
			}
			else if(link.childId().equals(person.id())){
				final GapData.Person parent = data.persons().get(link.parentId());
				if(parent != null && parent.birthYear() != null){
					from = max(from, parent.birthYear() + YOUNGEST_PARENT);
					to = min(to, parent.birthYear() + OLDEST_PARENT);
				}
			}
		}
		if(person.deathYear() != null)
			to = min(to, person.deathYear());
		return (from != null && to != null && from <= to? new int[]{from, to}: null);
	}

	/** @return the years the death may fall in: from the last year known of the individual to the longest life, up to today */
	static int[] deathWindow(final GapData.Person person, final GapData data){
		if(person.birthYear() == null)
			return null;

		return new int[]{person.birthYear(), Math.min(person.birthYear() + LONGEST_LIFE, data.today().getYear())};
	}

	/** @return the years the individual may appear in a census: their life up to the age of a household member */
	private static int[] lifeWindow(final GapData.Person person, final GapData data){
		if(person.birthYear() == null)
			return null;

		return new int[]{person.birthYear(), Math.min(person.birthYear() + CENSUS_AGE, data.today().getYear())};
	}

	/** @return the search with the places in it if there are some ({@code base.place}), else the one without them */
	private static String searchAt(final String base, final int[] window, final String where, final Object... lead){
		if(where == null)
			return search(base, window, lead);

		final Object[] arguments = Arrays.copyOf(lead, lead.length + 1);
		arguments[lead.length] = where;
		return search(base + ".place", window, arguments);
	}

	/** @return the message with the years in it if there are some ({@code key.years}), else the one without them */
	private static String search(final String key, final int[] window, final Object... arguments){
		if(window == null)
			return I18N.tf(key, arguments);

		final Object[] withYears = Arrays.copyOf(arguments, arguments.length + 1);
		withYears[arguments.length] = (window[0] == window[1]? String.valueOf(window[0]): window[0] + DASH + window[1]);
		return I18N.tf(key + ".years", withYears);
	}

	/** @return the places of the individuals, in a line, or {@code null} if there is none */
	private static String places(final GapData.Person person){
		return places(List.of(person));
	}

	private static String places(final List<GapData.Person> persons){
		final Set<String> places = new LinkedHashSet<>();
		for(final GapData.Person person : persons)
			places.addAll(person.places());
		return (places.isEmpty()? null: Sentences.list(places.stream()
			.limit(MAX_PLACES)
			.toList()));
	}

	private static List<String> repositories(final Gap gap, final List<GapData.Person> persons){
		final Set<String> names = new LinkedHashSet<>();
		for(final GapData.Person person : persons)
			names.addAll(person.repositories());

		final List<String> repositories = new ArrayList<>(names.stream()
			.sorted(Comparator.naturalOrder())
			.limit(MAX_REPOSITORIES)
			.toList());
		repositories.add(I18N.t("gap.analysis.repository.kind." + gap.name().toLowerCase(Locale.ROOT)));
		return repositories;
	}

	private static Integer min(final Integer a, final int b){
		return (a == null? b: Math.min(a, b));
	}

	private static Integer max(final Integer a, final int b){
		return (a == null? b: Math.max(a, b));
	}

}
