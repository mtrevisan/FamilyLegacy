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
package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;


/**
 * Validate File and the Research Assistant do not report the same things, on purpose (see the README of the assistant): the validation
 * says what is PROVABLE for every day a date may stand for, the assistant says that too, where the user is working, and adds what is only
 * a convention or a hint. This test keeps the two lists from drifting apart without anyone deciding it:
 * <ul>
 *		<li>every kind of the validation is classified here, so a new check of the validation obliges to say whether the assistant has it
 *		too;</li>
 *		<li>the kind of the assistant that a classification names exists;</li>
 *		<li>the rules on the dates that both have are errors in both (the validation reports them as errors by construction).</li>
 * </ul>
 */
class ValidationAssistantParityTest{

	private static final String ISSUE_PREFIX = "text.consistency.issue.";
	private static final String RESOLVED_REASON = "the assistant is about the decision: a warning, and nothing once a conclusion resolves it; "
		+ "the validation only sees the file, and reports an error";


	private enum Relation{
		/** The assistant has the same check, with the same severity. */
		SAME,
		/** The assistant has a check of the same fact, but it does not give it the severity of the validation: the reason says why. */
		DIFFERENT_SEVERITY,
		/** Only the validation has it, and it should stay so (structure of the file, files on disk, text of the messages). */
		VALIDATION_ONLY,
		/** Only the validation has it, but the assistant should have it too: a to-do, not a decision. */
		MISSING_IN_ASSISTANT
	}

	private record Entry(Relation relation, String assistantKind, String note){}


	private static Entry same(final String assistantKind){
		return new Entry(Relation.SAME, assistantKind, null);
	}

	private static Entry different(final String assistantKind, final String reason){
		return new Entry(Relation.DIFFERENT_SEVERITY, assistantKind, reason);
	}

	private static Entry only(final String reason){
		return new Entry(Relation.VALIDATION_ONLY, null, reason);
	}

	private static Entry missing(final String note){
		return new Entry(Relation.MISSING_IN_ASSISTANT, null, note);
	}

	/** Every kind of {@code ConsistencyService}, {@code DateConsistency}, {@code MediaFiles} and {@code FLEFValidator}. */
	private static final Map<String, Entry> VALIDATION = new LinkedHashMap<>();
	static{
		//dates: provable for every day the dates may stand for
		VALIDATION.put("death_before_birth", same("chronology.death_before_birth"));
		VALIDATION.put("event_before_birth", same("chronology.event_before_birth"));
		VALIDATION.put("event_after_death", same("chronology.event_after_death"));
		VALIDATION.put("child_before_parent", same("chronology.child_before_parent"));
		VALIDATION.put("born_after_mother_death", same("chronology.born_after_mother_death"));
		VALIDATION.put("born_after_father_death", same("chronology.born_after_father_death"));
		VALIDATION.put("union_before_birth", same("chronology.union_before_birth"));
		VALIDATION.put("union_after_death", same("chronology.union_after_death"));
		VALIDATION.put("membership_before_birth", same("chronology.membership_before_birth"));
		VALIDATION.put("membership_after_death", same("chronology.membership_after_death"));
		VALIDATION.put("membership_ended_before_birth", same("chronology.membership_ended_before_birth"));
		VALIDATION.put("date_invalid", same("chronology.date_invalid"));
		VALIDATION.put("date_not_checked", different("chronology.date_not_checked",
			"not a problem of the file, so information here (the indicator counts findings), a warning in the validation"));
		VALIDATION.put(FLEFValidator.KIND_VALIDITY, same("relationship.dates_reversed"));

		//relationships and parentage
		VALIDATION.put("self_relationship", same("relationship.self_relationship"));
		VALIDATION.put("duplicate_relationship", same("relationship.duplicate_relationship"));
		VALIDATION.put("relationship_type", same("relationship.invalid_for_participants"));
		VALIDATION.put("parents", different("conclusion.too_many_parents", RESOLVED_REASON));
		VALIDATION.put("same_sex_parents", different("conclusion.same_sex_parents", RESOLVED_REASON));
		VALIDATION.put("cycle", same("conclusion.own_ancestor"));
		VALIDATION.put("child_married_to_parent", same("relationship.child_married_to_parent"));
		VALIDATION.put("sibling_and_parent", same("relationship.sibling_and_parent"));

		//places
		VALIDATION.put("self_place_relationship", same("place.self_relation"));
		VALIDATION.put("place_cycle", same("place.cycle.part_of"));
		VALIDATION.put("succession_cycle", same("place.cycle.succession"));

		//what only the structure of the file or the disk can say
		VALIDATION.put("no_name", only("an individual without a name"));
		VALIDATION.put("event_no_participants", only("an event nobody took part in"));
		VALIDATION.put("event_no_principal", only("an event without a principal participant"));
		VALIDATION.put("resolves_missing", only("a conclusion resolves a record that does not exist"));
		VALIDATION.put("preferred", only("a preferred conclusion that is not among the ones it resolves"));
		VALIDATION.put("disproven_few", only("a disproven conclusion with too few records to choose from"));
		VALIDATION.put("disproven_preferred", only("a disproven conclusion that is the preferred one"));
		VALIDATION.put("source_cycle", only("sources derived from each other"));
		VALIDATION.put("source_derived_from_itself", only("a source derived from itself"));
		VALIDATION.put("group_cycle", only("groups that are part of each other"));
		VALIDATION.put("media_missing", only("the file of a media is not on the disk"));
		VALIDATION.put("incomplete", only("the structural checks could not be completed"));

		//texts that are not kinds
		VALIDATION.put("and_more", only("text that sums up the issues beyond the limit"));
		VALIDATION.put("fathers", only("word used in the message of 'parents'"));
		VALIDATION.put("mothers", only("word used in the message of 'parents'"));
	}


	private static Properties messages(){
		final Properties messages = new Properties();
		//the file is in the Domain module, whose resources the class loader of this module does not see
		final Path path = Path.of("..", "Domain", "src", "main", "resources", "i18n", "ui", "messages.properties");
		try(final Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
			messages.load(reader);
		}
		catch(final IOException ioe){
			throw new IllegalStateException(ioe);
		}
		return messages;
	}

	/** The kinds of the validation, as the messages know them. */
	private static Set<String> kindsOfTheValidation(final Properties messages){
		final Set<String> kinds = new TreeSet<>();
		for(final String key : messages.stringPropertyNames())
			if(key.startsWith(ISSUE_PREFIX) && key.indexOf('.', ISSUE_PREFIX.length()) < 0)
				kinds.add(key.substring(ISSUE_PREFIX.length()));
		kinds.add(FLEFValidator.KIND_VALIDITY);
		return kinds;
	}


	@Test
	void everyKindOfTheValidationIsClassified(){
		final Set<String> unclassified = kindsOfTheValidation(messages());
		unclassified.removeAll(VALIDATION.keySet());

		Assertions.assertTrue(unclassified.isEmpty(), "Classify these kinds of Validate File, in VALIDATION: does the Research Assistant "
			+ "need the same check? " + unclassified);
	}

	@Test
	void everyClassifiedKindStillExistsInTheValidation(){
		final Set<String> gone = new TreeSet<>(VALIDATION.keySet());
		gone.removeAll(kindsOfTheValidation(messages()));

		Assertions.assertTrue(gone.isEmpty(), "These kinds are not in the validation any more, drop them from VALIDATION: " + gone);
	}

	@Test
	void theKindsOfTheAssistantThatAreNamedExist(){
		final Properties messages = messages();
		final List<String> unknown = new ArrayList<>();
		for(final Map.Entry<String, Entry> entry : VALIDATION.entrySet()){
			final String assistantKind = entry.getValue().assistantKind();
			if(assistantKind != null && !messages.containsKey(assistantKind + ".title"))
				unknown.add(entry.getKey() + " -> " + assistantKind);
		}

		Assertions.assertTrue(unknown.isEmpty(), "The assistant has no such finding: " + unknown);
	}

	@Test
	void everyEntryIsConsistentWithItsRelation(){
		for(final Map.Entry<String, Entry> entry : VALIDATION.entrySet()){
			final Entry value = entry.getValue();
			final boolean hasAssistant = (value.assistantKind() != null);
			final boolean shouldHaveAssistant = (value.relation() == Relation.SAME || value.relation() == Relation.DIFFERENT_SEVERITY);
			Assertions.assertEquals(shouldHaveAssistant, hasAssistant, entry.getKey());
			if(value.relation() != Relation.SAME)
				Assertions.assertNotNull(value.note(), entry.getKey() + " needs a reason");
		}
	}


	/* ---- the rules on the dates are errors in the assistant, as they are in the validation ---- */

	private static DateRange day(final String gregorianDate){
		return DateRange.day(CalendarType.GREGORIAN.parseToJdn(gregorianDate));
	}

	private static ChronologyData.Person person(final String id, final SexType sex, final DateRange birth, final DateRange death){
		return new ChronologyData.Person(id, "Person " + id, sex, birth, death, (birth != null? "birth of " + id: null),
			(death != null? "death of " + id: null));
	}

	private static List<Diagnostic> check(final List<ChronologyData.Person> persons, final List<ChronologyData.LifeEvent> events,
			final List<ChronologyData.ParentLink> parents, final List<ChronologyData.Union> unions,
			final List<ChronologyData.Membership> memberships){
		final Map<String, ChronologyData.Person> byId = new LinkedHashMap<>();
		for(final ChronologyData.Person person : persons)
			byId.put(person.id(), person);
		return ChronologyChecker.check(new ChronologyData(byId, events, parents, unions, memberships));
	}

	/** The scenario of each rule on the dates that both have. */
	private static Map<String, List<Diagnostic>> scenarios(){
		final Map<String, List<Diagnostic>> scenarios = new LinkedHashMap<>();
		scenarios.put("death_before_birth", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1799"))), List.of(), List.of(), List.of(), List.of()));
		scenarios.put("event_before_birth", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), null)),
			List.of(new ChronologyData.LifeEvent("e", "a", EventReader.ENUM_TYPE_CENSUS, day("1 June 1799"), "date of e", true)),
			List.of(), List.of(), List.of()));
		scenarios.put("event_after_death", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1860"))),
			List.of(new ChronologyData.LifeEvent("e", "a", EventReader.ENUM_TYPE_CENSUS, day("1 June 1861"), "date of e", true)),
			List.of(), List.of(), List.of()));
		scenarios.put("child_before_parent", check(
			List.of(person("m", SexType.FEMALE, day("1 June 1800"), null), person("c", SexType.MALE, day("1 June 1790"), null)),
			List.of(), List.of(new ChronologyData.ParentLink("r", "c", "m", true)), List.of(), List.of()));
		scenarios.put("born_after_mother_death", check(
			List.of(person("m", SexType.FEMALE, day("1 June 1800"), day("1 June 1820")),
				person("c", SexType.MALE, day("1 June 1825"), null)),
			List.of(), List.of(new ChronologyData.ParentLink("r", "c", "m", true)), List.of(), List.of()));
		scenarios.put("born_after_father_death", check(
			List.of(person("f", SexType.MALE, day("1 June 1800"), day("1 June 1820")),
				person("c", SexType.MALE, day("1 June 1825"), null)),
			List.of(), List.of(new ChronologyData.ParentLink("r", "c", "f", true)), List.of(), List.of()));
		scenarios.put("union_before_birth", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), null), person("b", SexType.FEMALE, day("1 June 1810"), null)),
			List.of(), List.of(), List.of(new ChronologyData.Union("u", "a", "b", true, day("1 June 1805"), null, false, "begin of u")), List.of()));
		scenarios.put("union_after_death", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1850")),
				person("b", SexType.FEMALE, day("1 June 1810"), day("1 June 1850"))),
			List.of(), List.of(), List.of(new ChronologyData.Union("u", "a", "b", true, day("1 June 1851"), null, false, "begin of u")), List.of()));
		scenarios.put("membership_before_birth", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), null)), List.of(), List.of(), List.of(),
			List.of(new ChronologyData.Membership("m", "a", day("1 June 1799"), null, "begin of m", ""))));
		scenarios.put("membership_after_death", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), day("1 June 1860"))), List.of(), List.of(), List.of(),
			List.of(new ChronologyData.Membership("m", "a", day("1 June 1861"), null, "begin of m", ""))));
		scenarios.put("membership_ended_before_birth", check(
			List.of(person("a", SexType.MALE, day("1 June 1800"), null)), List.of(), List.of(), List.of(),
			List.of(new ChronologyData.Membership("m", "a", null, day("1 June 1799"), "", "end of m"))));
		return scenarios;
	}

	@Test
	void theRulesOnTheDatesBothHaveAreErrorsInTheAssistant(){
		final List<String> wrong = new ArrayList<>();
		for(final Map.Entry<String, List<Diagnostic>> scenario : scenarios().entrySet()){
			final String kind = "chronology." + scenario.getKey() + "|";
			final boolean error = scenario.getValue().stream()
				.anyMatch(finding -> finding.key() != null && finding.key().startsWith(kind)
					&& finding.severity() == Diagnostic.Severity.ERROR);
			if(!error)
				wrong.add(scenario.getKey());
		}

		Assertions.assertTrue(wrong.isEmpty(), "The assistant does not report these as errors, as Validate File does: " + wrong);
	}

	@Test
	void theScenariosCoverEveryRuleOnTheDatesThatBothHave(){
		final Set<String> sameChronology = new TreeSet<>();
		for(final Map.Entry<String, Entry> entry : VALIDATION.entrySet())
			if(entry.getValue().relation() == Relation.SAME && entry.getValue().assistantKind().startsWith("chronology."))
				sameChronology.add(entry.getKey());

		//the date that cannot be read is a warning, in its own test
		sameChronology.remove("date_invalid");

		Assertions.assertEquals(sameChronology, new TreeSet<>(scenarios().keySet()));
	}


	@Test
	void theDatesThatCannotBeReadAreAWarningAndTheOnesNotCheckedAreInformation(){
		final List<Diagnostic> findings = ChronologyChecker.check(new ChronologyData(Map.of(), List.of(), List.of(), List.of(), List.of(),
			List.of(new ChronologyData.UnreadableDate("e1", "Birth of A", "date", "31 February 1800", false),
				new ChronologyData.UnreadableDate("e2", "Birth of B", "date", "1 Muharram 1200", true))));

		Assertions.assertEquals(2, findings.size());
		Assertions.assertTrue(findings.get(0).key().startsWith("chronology.date_invalid|"));
		Assertions.assertEquals(Diagnostic.Severity.WARNING, findings.get(0).severity());
		Assertions.assertTrue(findings.get(1).key().startsWith("chronology.date_not_checked|"));
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, findings.get(1).severity());
	}

}
