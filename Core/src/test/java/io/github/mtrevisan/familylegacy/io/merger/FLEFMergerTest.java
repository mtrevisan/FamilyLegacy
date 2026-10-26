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

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class FLEFMergerTest{

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC);
	private static final String NOW = "2026-10-26T10:00:00Z";

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = FLEFMergerTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	@Test
	void collidingIdsOfTheIncomingFileAreRenamed(){
		final FLEFModel base = parse(person("I1", "Mario", "Rossi", "male"));
		final FLEFModel incoming = parse(person("I1", "Giulia", "Bianchi", "female")
			+ person("I2", "Anna", "Verdi", "female")
			+ """
			relationship R1 {
				subject.individual I2
				target.individual I1
				type biological_child
				note {
					text Daughter of [Giulia](flef:I1), see also flef:I12.
					mime text/markdown
					audit.creation.date 2020-01-01T00:00:00Z
				}
				audit.creation.date 2020-01-01T00:00:00Z
			}
			""");

		final MergeReport report = merger().merge(base, incoming);
		final FLEFModel merged = report.mergedModel();

		// the base keeps its ids
		Assertions.assertEquals("Mario", givenName(merged.getRecordById("I1")));
		final String giulia = report.idMapping().get("I1");
		Assertions.assertNotEquals("I1", giulia);
		Assertions.assertEquals("Giulia", givenName(merged.getRecordById(giulia)));
		Assertions.assertEquals("I2", report.idMapping().get("I2"));

		final FLEFRecord relationship = merged.getRecordById("R1");
		Assertions.assertEquals(giulia, FLEFRecordHelper.getChildValue(relationship, "target.individual"));
		// the link is rewritten, the link to another id that starts the same is not
		Assertions.assertEquals("Daughter of [Giulia](flef:" + giulia + "), see also flef:I12.",
			FLEFRecordHelper.getChildValue(relationship, "note.text"));
		Assertions.assertTrue(report.decisions(MergeReport.DecisionType.AUTO_MERGED).isEmpty());
	}

	@Test
	void theSameResearchMergedTwiceGivesOneCopy(){
		final String file = person("I1", "Mario", "Rossi", "male")
			+ birth("E1", "EP1", "I1", "15 JUN 1900", "P1")
			+ place("P1", "Treviso");
		final FLEFModel base = parse(file + source("S1", "Parish book of births"));
		final FLEFModel incoming = parse(file.replace("audit.creation.date 2020-01-01T00:00:00Z",
				"audit.creation.date 2021-01-01T00:00:00Z")
			.replace("\tplace.place P1\n", "\tplace.place P1\n\tsource {\n\t\tsource S1\n\t\tstance supports\n\t}\n")
			+ source("S1", "Parish book of births"));

		final MergeReport report = merger().merge(base, incoming);
		final FLEFModel merged = report.mergedModel();

		Assertions.assertEquals(1, merged.getRecordsByType("individual").size());
		Assertions.assertEquals(1, merged.getRecordsByType("event").size());
		Assertions.assertEquals(1, merged.getRecordsByType("event_participation").size());
		Assertions.assertEquals(1, merged.getRecordsByType("place").size());
		Assertions.assertEquals(1, merged.getRecordsByType("source").size());
		// the citation found only in the incoming file is kept
		final FLEFRecord event = merged.getRecordById("E1");
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(event, "source.source"));
		// the survivor states what it absorbed and why
		final String comment = FLEFRecordHelper.getChildValue(merged.getRecordById("I1"), "audit.update.comment");
		Assertions.assertTrue(comment.startsWith("Merged record I1 of the merged file"), comment);
		Assertions.assertTrue(comment.contains("same birth day"), comment);
		Assertions.assertEquals(NOW, FLEFRecordHelper.getChildValue(merged.getRecordById("I1"), "audit.update.date"));
		Assertions.assertEquals("I1", report.idMapping().get("I1"));
		Assertions.assertEquals("E1", report.idMapping().get("E1"));
		Assertions.assertTrue(report.warnings().isEmpty(), report.warnings().toString());
		Assertions.assertEquals(List.of(), schemaErrors(merged));
	}

	@Test
	void namesakesOfDifferentGenerationsAreNotMerged(){
		final FLEFModel model = parse(person("I1", "Giovanni", "Trevisan", "male")
			+ birth("E1", "EP1", "I1", "3 MAR 1870", null)
			+ person("I2", "Giovanni", "Trevisan", "male")
			+ birth("E2", "EP2", "I2", "9 SEP 1901", null));

		final MergeReport report = merger().deduplicate(model);

		Assertions.assertEquals(2, report.mergedModel().getRecordsByType("individual").size());
		Assertions.assertTrue(report.decisions().isEmpty(), report.decisions().toString());
	}

	@Test
	void aChildNamedAfterADeadSiblingIsNotMerged(){
		// same name, same parents, born about two years apart: the second was born after the first died
		final String parents = person("F", "Pietro", "Trevisan", "male") + person("M", "Maria", "Zanon", "female")
			+ child("R1", "I1", "F") + child("R2", "I1", "M") + child("R3", "I2", "F") + child("R4", "I2", "M");
		final FLEFModel model = parse(parents
			+ person("I1", "Beatrice", "Trevisan", "female")
			+ birth("E1", "EP1", "I1", "8 JUN 1806", null)
			+ death("E2", "EP2", "I1", "31 AUG 1807")
			+ person("I2", "Beatrice", "Trevisan", "female")
			+ birth("E3", "EP3", "I2", "19 MAR 1808", null)
			+ death("E4", "EP4", "I2", "25 MAY 1810"));

		final MergeReport report = merger().deduplicate(model);

		Assertions.assertEquals(4, report.mergedModel().getRecordsByType("individual").size());
		Assertions.assertTrue(report.decisions().isEmpty(), report.decisions().toString());
	}

	@Test
	void theSameCitedEntryTipsTheBalance(){
		// a couple without dates: names and spouse alone are not enough, the same entry of the same register is
		final String citation = "\tsource {\n\t\tsource S1\n\t\tlocator p. 12, n. 3\n\t\tstance supports\n\t}\n";
		final String file = source("S1", "Marriages of San Martino 1850-1870")
			+ person("I1", "Mario", "Rossi", "male").replace("\tsex male\n", "\tsex male\n" + citation)
			+ person("I2", "Maria", "Bianchi", "female").replace("\tsex female\n", "\tsex female\n" + citation)
			+ "relationship R1 {\n\tsubject.individual I1\n\ttarget.individual I2\n\ttype spouse\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n}\n";
		final FLEFModel withoutCitations = parse(file.replace(citation, ""));
		final FLEFModel withCitations = parse(file);

		final MergeReport weak = merger().merge(withoutCitations, parse(file.replace(citation, "")));
		final MergeReport strong = merger().merge(withCitations, parse(file));

		Assertions.assertEquals(4, weak.mergedModel().getRecordsByType("individual").size());
		Assertions.assertEquals(2, weak.decisionsNeedingReview().size());
		Assertions.assertEquals(2, strong.mergedModel().getRecordsByType("individual").size());
		Assertions.assertTrue(strong.decisionsNeedingReview().isEmpty(), strong.decisionsNeedingReview().toString());
		Assertions.assertEquals(1, strong.mergedModel().getRecordsByType("relationship").size());
	}

	@Test
	void anUncertainPairBecomesAnIdentityHypothesis(){
		// same name and birth year, nothing else: possibly cousins named after the same grandfather
		final FLEFModel model = parse(person("I1", "Giovanni", "Trevisan", "male")
			+ birth("E1", "EP1", "I1", "1870", null)
			+ person("I2", "Giovanni", "Trevisan", "male")
			+ birth("E2", "EP2", "I2", "1870", null));

		final MergeReport report = merger().deduplicate(model);
		final FLEFModel merged = report.mergedModel();

		Assertions.assertEquals(2, merged.getRecordsByType("individual").size());
		final List<MergeReport.Decision> review = report.decisionsNeedingReview();
		Assertions.assertEquals(1, review.size());
		final FLEFRecord hypothesis = merged.getRecordById(review.getFirst().recordId());
		Assertions.assertEquals("identity_hypothesis", hypothesis.getTag());
		Assertions.assertEquals(List.of("I1", "I2"), ProtocolRules.identities(hypothesis));
		Assertions.assertNotNull(FLEFRecordHelper.getChildValue(hypothesis, "rationale"));
		Assertions.assertEquals(List.of(), schemaErrors(merged));
	}

	@Test
	void oppositeSexesAreNeverTheSamePerson(){
		final FLEFModel model = parse(person("I1", "Andrea", "Rossi", "male")
			+ birth("E1", "EP1", "I1", "15 JUN 1900", null)
			+ person("I2", "Andrea", "Rossi", "female")
			+ birth("E2", "EP2", "I2", "15 JUN 1900", null));

		final MergeReport report = merger().deduplicate(model);

		Assertions.assertEquals(2, report.mergedModel().getRecordsByType("individual").size());
		Assertions.assertTrue(report.decisions().isEmpty());
	}

	@Test
	void twinsKeepTheirOwnBirthEvents(){
		final FLEFModel model = parse(person("I1", "Mario", "Rossi", "male")
			+ birth("E1", "EP1", "I1", "15 JUN 1900", null)
			+ person("I2", "Luigi", "Rossi", "male")
			+ birth("E2", "EP2", "I2", "15 JUN 1900", null));

		final MergeReport report = merger().deduplicate(model);

		Assertions.assertEquals(2, report.mergedModel().getRecordsByType("event").size());
		Assertions.assertEquals(2, report.mergedModel().getRecordsByType("individual").size());
	}

	@Test
	void theAcceptedHypothesisIsDeletedWithItsCascade(){
		final FLEFModel model = parse(person("I1", "Mario", "Rossi", "male")
			+ birth("E1", "EP1", "I1", "15 JUN 1900", null)
			+ person("I2", "Mario", "Rossi", "male")
			+ birth("E2", "EP2", "I2", "15 JUN 1900", null)
			+ """
			identity_hypothesis IH1 {
				identity.individual I1
				identity.individual I2
				audit.creation.date 2020-01-01T00:00:00Z
			}
			conclusion C1 {
				issue identity
				proof_status supported
				resolves.identity_hypothesis IH1
				preferred.identity_hypothesis IH1
				audit.creation.date 2020-01-01T00:00:00Z
			}
			research_question Q1 {
				title Who is Mario?
				question Are I1 and I2 the same person?
				target.individual I1
				target.identity_hypothesis IH1
				status open
				audit.creation.date 2020-01-01T00:00:00Z
			}
			""");

		final MergeReport report = merger().deduplicate(model);
		final FLEFModel merged = report.mergedModel();

		Assertions.assertEquals(1, merged.getRecordsByType("individual").size());
		Assertions.assertNull(merged.getRecordById("IH1"));
		Assertions.assertNull(merged.getRecordById("C1"));
		// a repeated reference to the deleted hypothesis is removed, the rest of the record stays
		final FLEFRecord question = merged.getRecordById("Q1");
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(question, "target").size());
		Assertions.assertEquals("I1", FLEFRecordHelper.getChildValue(question, "target.individual"));
		Assertions.assertEquals(2, report.decisions(MergeReport.DecisionType.DELETED).size());
		Assertions.assertEquals("I1", report.idMapping().get("I2"));
		Assertions.assertEquals(List.of(), schemaErrors(merged));
	}

	@Test
	void singularFieldsAreNotAssignedTwiceAndPrivacyStaysStrictest(){
		final FLEFModel model = parse(person("I1", "Mario", "Rossi", "unknown")
			+ birth("E1", "EP1", "I1", "15 JUN 1900", null)
			+ person("I2", "Mario", "Rossi", "male").replace("\tsex male\n", "\tsex male\n\tprivacy.level confidential\n")
			+ birth("E2", "EP2", "I2", "15 JUN 1900", null));

		final MergeReport report = merger().deduplicate(model);
		final FLEFRecord survivor = report.mergedModel().getRecordsByType("individual").getFirst();

		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(survivor, "sex").size());
		// a known sex wins over an unknown one
		Assertions.assertEquals("male", FLEFRecordHelper.getChildValue(survivor, "sex"));
		Assertions.assertEquals("confidential", FLEFRecordHelper.getChildValue(survivor, "privacy.level"));
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(survivor, "name").size());
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(survivor, "audit").size());
		Assertions.assertEquals(List.of(), schemaErrors(report.mergedModel()));
	}

	@Test
	void competingBirthsAreReported(){
		final String file = person("I1", "Mario", "Rossi", "male")
			+ birth("E1", "EP1", "I1", "15 JUN 1900", null)
			+ death("E2", "EP2", "I1", "2 FEB 1970");
		final FLEFModel base = parse(file);
		final FLEFModel incoming = parse(file.replace("15 JUN 1900", "16 JUN 1900"));

		final MergeReport report = merger().merge(base, incoming);

		Assertions.assertEquals(1, report.mergedModel().getRecordsByType("individual").size());
		// the two births differ and are kept as competing assertions, the two equal deaths are collapsed
		Assertions.assertEquals(3, report.mergedModel().getRecordsByType("event").size());
		Assertions.assertEquals(1, report.warnings().size(), report.warnings().toString());
		Assertions.assertTrue(report.warnings().getFirst().contains("2 birth events"), report.warnings().getFirst());
	}

	@Test
	void aFileMergedWithItselfDoesNotGrow() throws IOException{
		final FLEFModel model = parseResource("/tests/TGC55C.flef");
		final int recordCount = model.getRecordCount();
		final Set<String> errors = new HashSet<>(schemaErrors(model));

		final MergeReport report = merger().merge(model, parseResource("/tests/TGC55C.flef"));

		Assertions.assertEquals(recordCount, model.getRecordCount(), "the input must not be modified");
		for(final String kind : model.getRecordTypes())
			Assertions.assertEquals(model.getRecordsByType(kind).size(), report.mergedModel().getRecordsByType(kind).size(), kind);
		Assertions.assertTrue(report.decisionsNeedingReview().isEmpty(), report.decisionsNeedingReview().toString());
		// every incoming record found its copy in the base (or an identical record, when the base already repeats it)
		for(final FLEFRecord record : model.getRecords()){
			final FLEFRecord target = report.mergedModel().getRecordById(report.idMapping().get(record.getId()));
			Assertions.assertNotNull(target, record.getId());
			Assertions.assertEquals(record.getTag(), target.getTag(), record.getId());
		}
		// the merge introduces no kind of error the file did not already have (records uniting evidence may repeat them)
		final Set<String> newErrors = new HashSet<>(schemaErrors(report.mergedModel()));
		newErrors.removeAll(errors);
		Assertions.assertEquals(Set.of(), newErrors);
	}

	@Test
	void aManualMergeFollowsTheProtocol(){
		final FLEFModel model = parse(person("I1", "Mario", "Rossi", "male")
			+ birth("E1", "EP1", "I1", "15 JUN 1900", null)
			+ person("I2", "Mario Giuseppe", "Rossi", "male")
			+ """
			event_participation EP2 {
				participant.individual I2
				event E1
				role principal
				audit.creation.date 2020-01-01T00:00:00Z
			}
			identity_hypothesis IH1 {
				identity.individual I1
				identity.individual I2
				audit.creation.date 2020-01-01T00:00:00Z
			}
			""");

		final RecordAbsorber.Result result = new RecordAbsorber(grammar, CLOCK)
			.absorb(model, "I2", "I1", "same baptism record");

		Assertions.assertNull(model.getRecordById("I2"));
		Assertions.assertNull(model.getRecordById("IH1"));
		// the participation of I2 became identical to that of I1
		Assertions.assertEquals(1, model.getRecordsByType("event_participation").size());
		Assertions.assertEquals(List.of("EP2"), result.collapsedIds());
		Assertions.assertEquals(List.of("IH1"), result.deletedIds());
		Assertions.assertEquals("Merged record I2: same baptism record",
			FLEFRecordHelper.getChildValue(model.getRecordById("I1"), "audit.update.comment"));
	}


	private static FLEFMerger merger(){
		return new FLEFMerger(grammar, FLEFMerger.DEFAULT_AUTO_THRESHOLD, FLEFMerger.DEFAULT_REVIEW_THRESHOLD, CLOCK);
	}

	private static FLEFModel parse(final String records){
		return new FLEFParser().parse(HEADER + "records {\n" + records + "}\n");
	}

	private static FLEFModel parseResource(final String resource) throws IOException{
		try(final InputStream is = FLEFMergerTest.class.getResourceAsStream(resource)){
			return new FLEFParser().parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	private static List<String> schemaErrors(final FLEFModel model){
		return new FLEFValidator(grammar).validateSchema(model);
	}

	private static String givenName(final FLEFRecord individual){
		for(final FLEFRecord part : FLEFRecordHelper.findChildren(individual, "name.part"))
			if("given".equals(FLEFRecordHelper.getChildValue(part, "type")))
				return FLEFRecordHelper.getChildValue(part, "value");
		return null;
	}

	private static String person(final String id, final String given, final String family, final String sex){
		return "individual " + id + " {\n"
			+ "\tname {\n"
			+ "\t\tpart {\n\t\t\ttype given\n\t\t\tvalue " + given + "\n\t\t}\n"
			+ "\t\tpart {\n\t\t\ttype family\n\t\t\tvalue " + family + "\n\t\t}\n"
			+ "\t}\n"
			+ "\tsex " + sex + "\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String birth(final String eventId, final String participationId, final String individualId, final String date,
			final String placeId){
		return event(eventId, participationId, individualId, "birth", date, placeId);
	}

	private static String death(final String eventId, final String participationId, final String individualId, final String date){
		return event(eventId, participationId, individualId, "death", date, null);
	}

	private static String event(final String eventId, final String participationId, final String individualId, final String type,
			final String date, final String placeId){
		return "event " + eventId + " {\n"
			+ "\ttype " + type + "\n"
			+ "\tdate.value.point {\n\t\tvalue.full_date " + date + "\n\t\tcalendar gregorian\n\t}\n"
			+ (placeId != null? "\tplace.place " + placeId + "\n": "")
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n"
			+ "event_participation " + participationId + " {\n"
			+ "\tparticipant.individual " + individualId + "\n"
			+ "\tevent " + eventId + "\n"
			+ "\trole principal\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String child(final String id, final String childId, final String parentId){
		return "relationship " + id + " {\n"
			+ "\tsubject.individual " + childId + "\n"
			+ "\ttarget.individual " + parentId + "\n"
			+ "\ttype biological_child\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String place(final String id, final String name){
		return "place " + id + " {\n"
			+ "\tname.value " + name + "\n"
			+ "\ttype city\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n";
	}

	private static String source(final String id, final String title){
		return "source " + id + " {\n"
			+ "\ttitle.value " + title + "\n"
			+ "\taudit.creation.date 2020-01-01T00:00:00Z\n"
			+ "}\n";
	}

}
