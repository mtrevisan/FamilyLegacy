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
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/** Conversions that used to lose information: NOTE sources, CHAN, PLAC.FORM, custom tags, custom names, SLGC, ages, evidence. */
class GEDCOMLossyConversionsTest{

	private static final String NO_SOURCE_LINES = "";

	private static final String EVIDENCE_TYPE = FLEFRecordHelper.composePath(SourceCitationReader.TAG_EVIDENCE,
		EvidenceQualifiersReader.TAG_EVIDENCE_TYPE);
	private static final String EVIDENCE_INFORMATION_TYPE = FLEFRecordHelper.composePath(SourceCitationReader.TAG_EVIDENCE,
		EvidenceQualifiersReader.TAG_INFORMATION_TYPE);
	private static final String EVENT_CITATION_SOURCE = FLEFRecordHelper.composePath(EventReader.TAG_SOURCE, SourceCitationReader.TAG_SOURCE);
	private static final String EVENT_DATE_POINT = FLEFRecordHelper.composePath(EventReader.TAG_DATE, DateReader.TAG_VALUE_POINT,
		DateReader.TAG_VALUE, DateReader.TAG_FULL_DATE);
	private static final String PARTICIPATION_NOTE_TEXT = FLEFRecordHelper.composePath(EventParticipationReader.TAG_NOTE, NoteReader.TAG_TEXT);


	private static FLEFModel convert(final String body) throws IOException{
		return convert(body, "");
	}

	/** @param headerLines	The lines of the header (level 1 and below). */
	private static FLEFModel convert(final String body, final String headerLines) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + headerLines + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static FLEFRecord eventOfType(final FLEFModel model, final String type){
		return eventsOfType(model, type).stream()
			.findFirst()
			.orElse(null);
	}

	private static List<FLEFRecord> eventsOfType(final FLEFModel model, final String type){
		return model.getRecordsByType(EventReader.TYPE).stream()
			.filter(event -> type.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
			.toList();
	}

	private static FLEFRecord participation(final FLEFModel model, final FLEFRecord event, final String individualId){
		return model.getRecordsByType(EventParticipationReader.TYPE).stream()
			.filter(participation -> event.getId().equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT))
				&& individualId.equals(FLEFRecordHelper.getChildValue(participation, FLEFRecordHelper.composePath(
				EventParticipationReader.TAG_PARTICIPANT, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL))))
			.findFirst()
			.orElse(null);
	}

	private static List<String> placeTypes(final FLEFModel model, final String name){
		final List<String> types = new ArrayList<>();
		for(final FLEFRecord place : model.getRecordsByType(PlaceReader.TYPE))
			if(name.equals(FLEFRecordHelper.getChildValue(place, FLEFRecordHelper.composePath(PlaceReader.TAG_NAME, NameReader.TAG_VALUE))))
				types.add(FLEFRecordHelper.getChildValue(place, PlaceReader.TAG_TYPE));
		return types;
	}


	//---- 1. NOTE record: SOUR ----

	@Test
	void sourceCitationsOfANoteRecord() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Register
			0 @N1@ NOTE Shared note
			1 SOUR @S1@
			2 PAGE 5
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 NOTE @N1@
			""");

		final FLEFRecord note = FLEFRecordHelper.findChild(model.getRecordById("I1"), NoteReader.TAG_NOTE);
		Assertions.assertEquals("Shared note", FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TEXT));
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(note, FLEFRecordHelper.composePath(NoteReader.TAG_SOURCE,
			SourceCitationReader.TAG_SOURCE)));
		Assertions.assertEquals("5", FLEFRecordHelper.getChildValue(note, FLEFRecordHelper.composePath(NoteReader.TAG_SOURCE,
			SourceCitationReader.TAG_LOCATOR)));
		//the audit is the last field of the note
		Assertions.assertEquals(NoteReader.TAG_AUDIT, note.getChildren().getLast().getTag());
	}


	//---- 2. CHAN ----

	@Test
	void changeWithoutCreationIsTheCreationAndNotAnUpdate() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 CHAN
			2 DATE 5 MAR 2020
			3 TIME 10:00:00
			2 NOTE Edited
			""");

		final FLEFRecord audit = FLEFRecordHelper.findChild(model.getRecordById("I1"), AuditReader.TAG_AUDIT);
		Assertions.assertEquals("2020-03-05T10:00:00Z", FLEFRecordHelper.getChildValue(audit, AuditReader.TAG_CREATION_DATE));
		//a creation is not an update: with no CREA the date of the change is the creation, and there is no update
		Assertions.assertNull(FLEFRecordHelper.findChild(audit, AuditReader.TAG_UPDATE));
		Assertions.assertEquals("Edited", FLEFRecordHelper.getChildValue(audit, FLEFRecordHelper.composePath(AuditReader.TAG_CREATION,
			AuditReader.TAG_COMMENT)));
	}


	@Test
	void anElementWithoutADateIsAsOldAsItsRecordOrAsTheFile() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 1900
			1 CHAN
			2 DATE 5 MAR 2020
			3 TIME 10:00:00
			0 @I2@ INDI
			1 NAME Luigi /Rossi/
			1 DEAT
			2 DATE 1950
			""", """
			1 DATE 1 JAN 2021
			2 TIME 08:30:00
			""");

		//the birth has no change of its own: it takes the one of its individual
		Assertions.assertEquals("2020-03-05T10:00:00Z", creationOfEvent(model, EventReader.ENUM_TYPE_BIRTH));
		//the individual has none, neither: the one of the file
		Assertions.assertEquals("2021-01-01T08:30:00Z", creationOfEvent(model, EventReader.ENUM_TYPE_DEATH));
	}

	private static String creationOfEvent(final FLEFModel model, final String type){
		return FLEFRecordHelper.getChildValue(eventOfType(model, type), FLEFRecordHelper.composePath(AuditReader.TAG_AUDIT,
			AuditReader.TAG_CREATION, AuditReader.TAG_DATE));
	}


	//---- 3. PLAC.FORM ----

	@Test
	void placeLevelsAreTypedByTheForm() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 1900
			2 PLAC Treviso, Treviso, Veneto, Italia
			3 FORM City, County, State, Country
			1 DEAT
			2 DATE 1980
			2 PLAC Padova, Pd
			3 FORM Locality, Region
			""");

		Assertions.assertEquals(List.of(PlaceReader.ENUM_TYPE_COUNTRY), placeTypes(model, "Italia"));
		Assertions.assertEquals(List.of(PlaceReader.ENUM_TYPE_PROVINCE), placeTypes(model, "Veneto"));
		Assertions.assertTrue(placeTypes(model, "Treviso").containsAll(List.of(PlaceReader.ENUM_TYPE_CITY, PlaceReader.ENUM_TYPE_COUNTY)));
		//an unknown level stays untyped
		Assertions.assertEquals(List.of(PlaceReader.ENUM_TYPE_REGION), placeTypes(model, "Pd"));
		Assertions.assertEquals(1, placeTypes(model, "Padova").size());
		Assertions.assertNull(placeTypes(model, "Padova").getFirst());
	}

	@Test
	void headerFormIsTheDefaultOne() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 1900
			2 PLAC Treviso, Italia
			""", "1 PLAC\n2 FORM City, Country\n");

		Assertions.assertEquals(List.of(PlaceReader.ENUM_TYPE_CITY), placeTypes(model, "Treviso"));
		Assertions.assertEquals(List.of(PlaceReader.ENUM_TYPE_COUNTRY), placeTypes(model, "Italia"));
	}


	//---- 4. custom tags of individuals and families ----

	@Test
	void todoBecomesAResearchTask() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 _TODO Find the baptism record
			0 @I2@ INDI
			1 NAME Bruno /Verdi/
			0 @F1@ FAM
			1 HUSB @I2@
			1 _TODO
			2 DESC Check the will
			""");

		final List<FLEFRecord> tasks = model.getRecordsByType(ResearchTaskReader.TYPE);
		Assertions.assertEquals(List.of("Check the will", "Find the baptism record"), tasks.stream()
			.map(task -> FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_DESCRIPTION))
			.sorted()
			.toList());
		Assertions.assertTrue(tasks.stream()
			.allMatch(task -> ResearchTaskReader.ENUM_STATUS_OPEN.equals(FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_STATUS))));

		// each task is linked to a question that targets the individual or the family the to-do belongs to
		final FLEFRecord individualTask = tasks.stream()
			.filter(task -> "Find the baptism record".equals(FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_DESCRIPTION)))
			.findFirst().orElseThrow();
		final FLEFRecord individualQuestion = model.getRecordById(FLEFRecordHelper.getChildValue(individualTask, ResearchTaskReader.TAG_QUESTION));
		Assertions.assertTrue(IndividualReader.isOfType(model.getRecordById(FLEFRecordHelper.getChildValue(individualQuestion,
			FLEFRecordHelper.composePath(ResearchQuestionReader.TAG_TARGET, AgentReader.TAG_INDIVIDUAL)))));
		final FLEFRecord groupTask = tasks.stream()
			.filter(task -> "Check the will".equals(FLEFRecordHelper.getChildValue(task, ResearchTaskReader.TAG_DESCRIPTION)))
			.findFirst().orElseThrow();
		final FLEFRecord groupQuestion = model.getRecordById(FLEFRecordHelper.getChildValue(groupTask, ResearchTaskReader.TAG_QUESTION));
		Assertions.assertTrue(GroupReader.isOfType(model.getRecordById(FLEFRecordHelper.getChildValue(groupQuestion,
			FLEFRecordHelper.composePath(ResearchQuestionReader.TAG_TARGET, AgentReader.TAG_GROUP)))));
	}

	@Test
	void customTagsWithDataBecomeEvents() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 _MILT
			2 DATE 1915
			2 PLAC Roma
			1 _UID 1234
			1 _FLAG Y
			1 _CRE
			2 DATE 1 JAN 2020
			""");

		final FLEFRecord military = eventOfType(model, "milt");
		Assertions.assertNotNull(military);
		Assertions.assertEquals("1915", FLEFRecordHelper.getChildValue(military, EVENT_DATE_POINT));
		Assertions.assertNotNull(participation(model, military, "I1"));
		//the tags without data stay skipped
		Assertions.assertEquals(1, model.getRecordsByType(EventReader.TYPE).size());
	}


	//---- 5. custom names ----

	@Test
	void customNames() throws IOException{
		final FLEFRecord individual = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 _MARNM Anna /Bianchi/
			1 _AKA Annie /Rossi/
			""").getRecordById("I1");

		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME);
		final FLEFRecord married = names.stream()
			.filter(name -> NameReader.ENUM_PERSONAL_TYPE_MARRIED.equals(FLEFRecordHelper.getChildValue(name, NameReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals(List.of("Anna", "Bianchi"), FLEFRecordHelper.findChildren(married, NameReader.TAG_PART).stream()
			.map(part -> FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_VALUE))
			.toList());
		final FLEFRecord alias = names.stream()
			.filter(name -> NameReader.ENUM_PERSONAL_TYPE_ALIAS.equals(FLEFRecordHelper.getChildValue(name, NameReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("Annie", FLEFRecordHelper.getChildValue(FLEFRecordHelper.findChildren(alias, NameReader.TAG_PART).get(0),
			NameReader.TAG_PART_VALUE));
	}


	//---- 6. SLGC.FAMC ----

	@Test
	void sealingToAFamilyLinksTheGroup() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 SLGC
			2 DATE 1 JAN 1950
			2 FAMC @F1@
			0 @F1@ FAM
			1 CHIL @I1@
			""");

		final FLEFRecord sealing = eventOfType(model, GEDCOMMapper.mapEvent(GEDCOMMapper.TAG_SEALING_CHILD, GEDCOMMapper.TAG_SEALING_CHILD));
		Assertions.assertNotNull(sealing);
		//the FAM becomes a group: the link points to the id the group got
		final String groupId = model.getRecordsByType(GroupReader.TYPE).get(0).getId();
		Assertions.assertTrue(FLEFRecordHelper.findChildren(sealing, EventReader.TAG_NOTE).stream()
			.anyMatch(note -> FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TEXT).contains(GEDCOMMapper.FLEF_LINK_PREFIX + groupId)));
	}


	//---- age at an event: calculated birth ----

	private static final String CENSUS_WITH_AGE = """
		0 @S1@ SOUR
		1 TITL Census book
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 CENS
		2 DATE 1850
		2 AGE %s
		2 SOUR @S1@
		3 PAGE 7
		""";

	@Test
	void ageBecomesACalculatedBirth() throws IOException{
		final FLEFModel model = convert(CENSUS_WITH_AGE.formatted("25y"));

		final FLEFRecord birth = eventOfType(model, EventReader.ENUM_TYPE_BIRTH);
		Assertions.assertEquals("1825", FLEFRecordHelper.getChildValue(birth, EVENT_DATE_POINT));
		Assertions.assertEquals(DateReader.ENUM_BASIS_CALCULATED, FLEFRecordHelper.getChildValue(birth, FLEFRecordHelper.composePath(
			EventReader.TAG_DATE, DateReader.TAG_VALUE_POINT, DateReader.TAG_APPROXIMATE, DateReader.TAG_BASIS)));
		Assertions.assertEquals(ConfidenceReader.ENUM_LOW, FLEFRecordHelper.getChildValue(birth, EventReader.TAG_CONFIDENCE));
		//the sources are those of the census, as indirect evidence
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(birth, EVENT_CITATION_SOURCE));
		Assertions.assertEquals("7", FLEFRecordHelper.getChildValue(birth, FLEFRecordHelper.composePath(EventReader.TAG_SOURCE,
			SourceCitationReader.TAG_LOCATOR)));
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT, FLEFRecordHelper.getChildValue(
			FLEFRecordHelper.findChild(birth, EventReader.TAG_SOURCE), EVIDENCE_TYPE));
		final FLEFRecord birthParticipation = participation(model, birth, "I1");
		Assertions.assertNotNull(birthParticipation);
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_PRINCIPAL, FLEFRecordHelper.getChildValue(birthParticipation,
			EventParticipationReader.TAG_ROLE));

		//the note of the census participation says that the age was converted
		final String note = FLEFRecordHelper.getChildValue(participation(model, eventOfType(model, "census"), "I1"), PARTICIPATION_NOTE_TEXT);
		Assertions.assertTrue(note.startsWith("25y") && note.contains(birth.getId()));
	}

	@Test
	void lessThanAgeIsALowerBound() throws IOException{
		final FLEFModel model = convert(CENSUS_WITH_AGE.formatted("< 8y"));

		final FLEFRecord birth = eventOfType(model, EventReader.ENUM_TYPE_BIRTH);
		Assertions.assertEquals("1842", FLEFRecordHelper.getChildValue(birth, FLEFRecordHelper.composePath(EventReader.TAG_DATE,
			DateReader.TAG_VALUE_BOUNDED_NOT_BEFORE, DateReader.TAG_VALUE, DateReader.TAG_FULL_DATE)));
		Assertions.assertNull(FLEFRecordHelper.findChild(birth, FLEFRecordHelper.composePath(EventReader.TAG_DATE, DateReader.TAG_VALUE_POINT)));
	}

	@Test
	void childAgeStaysANote() throws IOException{
		final FLEFModel model = convert(CENSUS_WITH_AGE.formatted("CHILD"));

		Assertions.assertTrue(eventsOfType(model, EventReader.ENUM_TYPE_BIRTH).isEmpty());
		Assertions.assertEquals("CHILD", FLEFRecordHelper.getChildValue(participation(model, eventOfType(model, "census"), "I1"),
			PARTICIPATION_NOTE_TEXT));
	}

	@Test
	void ageOfAnIndividualWithADatedBirthStaysANote() throws IOException{
		final FLEFModel model = convert(CENSUS_WITH_AGE.formatted("25y") + "1 BIRT\n2 DATE 12 MAR 1826\n");

		Assertions.assertEquals(1, eventsOfType(model, EventReader.ENUM_TYPE_BIRTH).size());
		Assertions.assertEquals("25y", FLEFRecordHelper.getChildValue(participation(model, eventOfType(model, "census"), "I1"),
			PARTICIPATION_NOTE_TEXT));
	}

	@Test
	void ageWithoutSourceLinksTheEvent() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 CENS
			2 DATE 1850
			2 AGE 25y
			""");

		final FLEFRecord census = eventOfType(model, "census");
		final FLEFRecord birth = eventOfType(model, EventReader.ENUM_TYPE_BIRTH);
		Assertions.assertNull(FLEFRecordHelper.findChild(birth, EventReader.TAG_SOURCE));
		Assertions.assertTrue(FLEFRecordHelper.getChildValue(birth, FLEFRecordHelper.composePath(EventReader.TAG_NOTE, NoteReader.TAG_TEXT))
			.contains(GEDCOMMapper.FLEF_LINK_PREFIX + census.getId()));
	}

	@Test
	void fullDateIsCalculatedFromFullAge() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 CENS
			2 DATE 10 MAR 1850
			2 AGE 25y 2m 3d
			""");

		Assertions.assertEquals("7 JAN 1825", FLEFRecordHelper.getChildValue(eventOfType(model, EventReader.ENUM_TYPE_BIRTH), EVENT_DATE_POINT));
	}


	//---- evidence qualifiers by the event cited ----

	/** Converts an event of an individual citing a source, and returns the evidence type of the citation. */
	private static String citationEvidence(final String event, final String eventLines, final String sourceLines) throws IOException{
		final FLEFModel model = convert("0 @S1@ SOUR\n1 TITL Register\n" + sourceLines + "0 @I1@ INDI\n1 NAME Anna /Rossi/\n1 " + event
			+ "\n2 DATE 1900\n2 SOUR @S1@\n" + eventLines);
		return FLEFRecordHelper.getChildValue(FLEFRecordHelper.findChild(model.getRecordsByType(EventReader.TYPE).getFirst(),
			EventReader.TAG_SOURCE), EVIDENCE_TYPE);
	}

	@Test
	void citedEventEqualToTheEventIsDirect() throws IOException{
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT, citationEvidence("BIRT", "3 EVEN BIRT\n", NO_SOURCE_LINES));
	}

	@Test
	void citedEventDifferentFromTheEventIsIndirect() throws IOException{
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT, citationEvidence("BIRT", "3 EVEN DEAT\n", NO_SOURCE_LINES));
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT, citationEvidence("DEAT", "3 EVEN BURI\n", NO_SOURCE_LINES));
	}

	@Test
	void primaryInformationOfADifferentEventIsSecondary() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Register
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 1900
			2 SOUR @S1@
			3 EVEN CHR
			3 QUAY 3
			""");

		final FLEFRecord citation = FLEFRecordHelper.findChild(eventOfType(model, EventReader.ENUM_TYPE_BIRTH), EventReader.TAG_SOURCE);
		Assertions.assertEquals(EvidenceQualifiersReader.INFORMATION_TYPE_SECONDARY, FLEFRecordHelper.getChildValue(citation,
			EVIDENCE_INFORMATION_TYPE));
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT, FLEFRecordHelper.getChildValue(citation, EVIDENCE_TYPE));
	}

	@Test
	void eventsCoveredByTheSource() throws IOException{
		//the source covers christenings only
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT, citationEvidence("BIRT", "", "1 DATA\n2 EVEN CHR\n"));
		//the source covers births too
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT, citationEvidence("BIRT", "", "1 DATA\n2 EVEN CHR, BIRT\n"));
	}

	@Test
	void withoutCitedEventTheEvidenceIsUnchanged() throws IOException{
		Assertions.assertNull(citationEvidence("BIRT", "", NO_SOURCE_LINES));
	}


	//---- 12. Data without a field of its own is kept as notes ----

	@Test
	void keptNotesFollowTheOtherNotesAndPrecedeTheAudit() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 REFN 123
			1 _FOO bar
			1 NOTE A note
			1 SEX X
			""");

		final FLEFRecord individual = model.getRecordById("I1");
		final List<String> tags = individual.getChildren().stream()
			.map(FLEFRecord::getTag)
			.toList();
		Assertions.assertEquals(AuditReader.TAG_AUDIT, tags.getLast());
		final List<String> titles = FLEFRecordHelper.findChildren(individual, NoteReader.TAG_NOTE).stream()
			.map(note -> FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE))
			.toList();
		Assertions.assertEquals(java.util.Arrays.asList(null, "REFN", "Sex", "GEDCOM _FOO"), titles);
		//the notes are contiguous
		final int first = tags.indexOf(NoteReader.TAG_NOTE);
		Assertions.assertEquals(first + 3, tags.lastIndexOf(NoteReader.TAG_NOTE));
	}


	//---- 13. A note of a source that is a date, or a place and a date ----

	@Test
	void sourceNoteThatIsOnlyADateIsTheDateOfTheSource() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Parish registers
			1 NOTE 1954
			""");

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertEquals("1954", FLEFRecordHelper.getChildValue(source, "date.value.point.value.full_date"));
		Assertions.assertNull(FLEFRecordHelper.findChild(source, NoteReader.TAG_NOTE));
	}

	@Test
	void sourceNoteWithPlaceAndYearGivesThePlaceAndTheDate() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Parish registers
			1 NOTE Padova : 1954
			""");

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertEquals("1954", FLEFRecordHelper.getChildValue(source, "date.value.point.value.full_date"));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(source, "place"));
		Assertions.assertNull(FLEFRecordHelper.findChild(source, NoteReader.TAG_NOTE));
	}

	@Test
	void sourceNoteThatIsAnythingElseStaysANote() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Parish registers
			1 NOTE Kept in the sacristy
			""");

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertNull(FLEFRecordHelper.findChild(source, "date"));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(source, NoteReader.TAG_NOTE));
	}

}
