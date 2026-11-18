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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/** The FAN table: who appears with an individual, how strongly, and what to look for next. */
class FanServiceTest{

	private static final String AUDIT = "\taudit.creation.date 2026-01-01T00:00:00Z\n";
	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = FanServiceTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	/** An individual with a death event, so that it is not living and is public. */
	private static String individual(final String id){
		return "individual " + id + " {\n" + AUDIT + "}\n"
			+ "event D" + id + " {\n\ttype death\n" + AUDIT + "}\n"
			+ participation("DP" + id, "D" + id, id, "principal");
	}

	private static String sex(final String id, final String individualId, final String sex){
		return "individual_attribute " + id + " {\n\tindividual " + individualId + "\n\ttype sex\n\tvalue " + sex + "\n" + AUDIT + "}\n";
	}

	private static String event(final String id, final String year){
		return "event " + id + " {\n\ttype death\n\tdate.value.point {\n\t\tvalue.full_date " + year + "\n\t\tcalendar gregorian\n\t}\n"
			+ AUDIT + "}\n";
	}

	private static String participation(final String id, final String eventId, final String individualId, final String role){
		return "event_participation " + id + " {\n\tevent " + eventId + "\n\tparticipant.individual " + individualId + "\n\trole " + role
			+ "\n" + AUDIT + "}\n";
	}

	private static String spouse(final String id, final String firstId, final String secondId){
		return "relationship " + id + " {\n\tsubject.individual " + firstId + "\n\ttarget.individual " + secondId
			+ "\n\ttype civil_spouse\n" + AUDIT + "}\n";
	}

	private static FLEFModel model(final String... records){
		return new FLEFParser().parse(HEADER + String.join("", records));
	}


	@Test
	void theStrongerLinkComesFirst(){
		//I2 is witness at two events of I1, I3 at one
		final FLEFModel model = model(individual("I1"), individual("I2"), individual("I3"),
			event("E1", "1890"), event("E2", "1910"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "witness"),
			participation("EP3", "E1", "I3", "witness"),
			participation("EP4", "E2", "I1", "principal"), participation("EP5", "E2", "I2", "witness"));

		final FanResult fan = new FanService(model).build("I1");

		Assertions.assertEquals(List.of("I2", "I3"), fan.entries().stream().map(FanEntry::personId).toList());
		final FanEntry first = fan.entries().get(0);
		Assertions.assertEquals(List.of("E1", "E2"), first.eventIds());
		Assertions.assertEquals(2, first.otherRoles().get("witness"));
		Assertions.assertEquals(2, first.ownRoles().get("principal"));
		Assertions.assertEquals(1890, first.firstYear());
		Assertions.assertEquals(1910, first.lastYear());
		//two events and a tie that lasts, against one event
		Assertions.assertTrue(first.score() > fan.entries().get(1).score());
	}

	@Test
	void everyParticipantOfAnEventIsConsideredAndNotOnlyTheSubject(){
		final FLEFModel model = model(individual("I1"), individual("I2"), individual("I3"),
			event("E1", "1890"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "witness"),
			participation("EP3", "E1", "I3", "witness"));

		final FanResult fan = new FanService(model).build("I2");

		//the other witness is as much a companion of I2 as the principal
		Assertions.assertEquals(List.of("I1", "I3"), fan.entries().stream().map(FanEntry::personId).sorted().toList());
	}

	@Test
	void relativesAreListedAfterTheOthers(){
		final FLEFModel model = model(individual("I1"), individual("I2"), individual("I3"),
			spouse("RL1", "I1", "I2"),
			event("E1", "1890"), event("E2", "1900"), event("E3", "1910"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "principal"),
			participation("EP3", "E2", "I1", "principal"), participation("EP4", "E2", "I2", "principal"),
			participation("EP5", "E3", "I1", "principal"), participation("EP6", "E3", "I3", "witness"));

		final FanResult fan = new FanService(model).build("I1");

		Assertions.assertEquals(List.of("I3", "I2"), fan.entries().stream().map(FanEntry::personId).toList());
		Assertions.assertTrue(fan.entries().get(1).kin());
		Assertions.assertFalse(fan.entries().get(0).kin());
		//the spouse is not a candidate relative
		Assertions.assertTrue(fan.suggestions().isEmpty());
	}

	@Test
	void twoGodparentsOfTheSameChildMightBeACouple(){
		final FLEFModel model = model(individual("I1"), individual("I2"), individual("I3"),
			event("E1", "1890"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "sponsor"),
			participation("EP3", "E1", "I3", "sponsor"));

		final FanResult fan = new FanService(model).build("I1");

		Assertions.assertEquals(1, fan.suggestions().size());
		final FanSuggestion suggestion = fan.suggestions().get(0);
		Assertions.assertEquals(FanSuggestion.Kind.POSSIBLE_COUPLE, suggestion.kind());
		Assertions.assertEquals(List.of("I2", "I3"), suggestion.personIds());
		Assertions.assertEquals(List.of("E1"), suggestion.eventIds());
	}

	@Test
	void twoGodparentsKnownToBeRelativesOrOfTheSameSexAreNotACouple(){
		final FLEFModel related = model(individual("I1"), individual("I2"), individual("I3"), spouse("RL1", "I2", "I3"),
			event("E1", "1890"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "sponsor"),
			participation("EP3", "E1", "I3", "sponsor"));
		Assertions.assertTrue(new FanService(related).build("I1").suggestions().isEmpty());

		final FLEFModel sameSex = model(individual("I1"), individual("I2"), individual("I3"),
			sex("IA1", "I2", "male"), sex("IA2", "I3", "male"),
			event("E1", "1890"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "sponsor"),
			participation("EP3", "E1", "I3", "sponsor"));
		Assertions.assertTrue(new FanService(sameSex).build("I1").suggestions().isEmpty());
	}

	@Test
	void aPersonWhoRecursWithNoKinshipIsACandidateRelative(){
		final FLEFModel model = model(individual("I1"), individual("I2"),
			event("E1", "1890"), event("E2", "1910"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "witness"),
			participation("EP3", "E2", "I1", "principal"), participation("EP4", "E2", "I2", "witness"));

		final FanResult fan = new FanService(model).build("I1");

		Assertions.assertEquals(1, fan.suggestions().size());
		Assertions.assertEquals(FanSuggestion.Kind.RECURRING_WITHOUT_KINSHIP, fan.suggestions().get(0).kind());
		Assertions.assertEquals(List.of("I2"), fan.suggestions().get(0).personIds());
	}

	@Test
	void theTotalOfRolesTellsAProfessionalWitness(){
		final FLEFModel model = model(individual("I1"), individual("I2"), individual("I3"),
			event("E1", "1890"), event("E2", "1891"), event("E3", "1892"),
			participation("EP1", "E1", "I1", "principal"), participation("EP2", "E1", "I2", "witness"),
			participation("EP3", "E2", "I3", "principal"), participation("EP4", "E2", "I2", "witness"),
			participation("EP5", "E3", "I3", "principal"), participation("EP6", "E3", "I2", "witness"));

		final FanEntry entry = new FanService(model).build("I1").entries().get(0);

		Assertions.assertEquals(3, entry.roleTotal());
		Assertions.assertEquals(1, entry.sharedEvents());
	}

	@Test
	void nothingIsReturnedForAnUnknownIndividualOrWhenNobodyAppearsWithIt(){
		final FLEFModel model = model(individual("I1"), event("E1", "1890"), participation("EP1", "E1", "I1", "principal"));
		final FanService service = new FanService(model);

		Assertions.assertTrue(service.build("I1").isEmpty());
		Assertions.assertTrue(service.build("I99").isEmpty());
		Assertions.assertTrue(service.build(null).isEmpty());
	}

	@Test
	void theYearOfAnOldDateIsTheJulianOne(){
		//1 January 1500 Julian is JDN 2268933, the 11th of January Gregorian
		Assertions.assertEquals(1500, FanService.yearOf(2268933L));
		//the Gregorian calendar begins on 15 October 1582
		Assertions.assertEquals(1582, FanService.yearOf(2299161L));
		Assertions.assertEquals(2026, FanService.yearOf(2461042L));
	}

}
