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

import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.GedcomTexts;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/** The custom EVEN/FACT types that FLEF has a place for: a retirement home, a membership, a twin, a midwife, who a person is named after. */
class GEDCOMCustomFactsTest{

	private static FLEFModel convert(final String body) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, new ImportReport());
	}

	private static List<FLEFRecord> eventsOfType(final FLEFModel model, final String type){
		final List<FLEFRecord> events = new ArrayList<>();
		for(final FLEFRecord event : model.getRecordsByType(EventReader.TYPE))
			if(type.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
				events.add(event);
		return events;
	}

	private static String noteText(final FLEFRecord record, final String title){
		for(final FLEFRecord note : FLEFRecordHelper.findChildren(record, NoteReader.TAG_NOTE))
			if(title.equals(FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE)))
				return FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TEXT);
		return null;
	}

	private static String nameOf(final FLEFRecord individual){
		final StringBuilder name = new StringBuilder();
		for(final FLEFRecord part : FLEFRecordHelper.findChildren(FLEFRecordHelper.findChild(individual, IndividualReader.TAG_NAME),
				NameReader.TAG_PART))
			name.append(name.isEmpty()? StringUtils.EMPTY: StringUtils.SPACE).append(FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_VALUE));
		return name.toString();
	}


	@Test
	void aRetirementHomeIsAResidenceThatKeepsItsLabel() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 EVEN
			2 TYPE Retirement Home
			2 DATE 1967
			""");

		Assertions.assertTrue(eventsOfType(model, "retirement_home").isEmpty());
		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeReader.TYPE);
		Assertions.assertEquals(1, attributes.size());
		final FLEFRecord residence = attributes.getFirst();
		Assertions.assertEquals(IndividualAttributeReader.ENUM_TYPE_RESIDENCE,
			FLEFRecordHelper.getChildValue(residence, IndividualAttributeReader.TAG_TYPE));
		Assertions.assertEquals("Retirement Home", noteText(residence, GedcomTexts.t("title.type")));
		Assertions.assertEquals(IndividualAttributeReader.TAG_AUDIT,
			residence.getChildren().getLast().getTag(), "the audit is the last field");
	}


	@Test
	void aMembershipIsTheSameGroupForTheSameName() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 EVEN Tropical Lodge No. 56
			2 TYPE Membership
			2 DATE 1962
			0 @I2@ INDI
			1 NAME Carlo /Bianchi/
			1 EVEN Tropical Lodge No. 56
			2 TYPE Membership
			1 EVEN Scottish Rite
			2 TYPE Membership
			""");

		Assertions.assertTrue(eventsOfType(model, "membership").isEmpty());
		final List<FLEFRecord> groups = model.getRecordsByType(GroupReader.TYPE);
		Assertions.assertEquals(2, groups.size());
		final List<FLEFRecord> memberships = model.getRecordsByType(RelationshipReader.TYPE);
		Assertions.assertEquals(3, memberships.size());
		for(final FLEFRecord membership : memberships)
			Assertions.assertEquals(RelationshipReader.ENUM_TYPE_GROUP_MEMBER, RelationshipReader.extractType(membership));
		Assertions.assertEquals(GroupReader.ENUM_TYPE_ASSOCIATION, FLEFRecordHelper.getChildValue(groups.getFirst(), GroupReader.TAG_TYPE));
	}

	@Test
	void aMembershipWithoutNameOrWithAPlaceStaysAnEvent() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 EVEN
			2 TYPE Membership
			2 NOTE Some club
			1 EVEN Rotary
			2 TYPE Membership
			2 PLAC Treviso, Italia
			""");

		Assertions.assertEquals(2, eventsOfType(model, "membership").size());
		Assertions.assertTrue(model.getRecordsByType(GroupReader.TYPE).isEmpty());
	}


	@Test
	void whoAPersonIsNamedAfterIsANote() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 EVEN the grandmother
			2 TYPE Named After
			""");

		Assertions.assertTrue(eventsOfType(model, "named_after").isEmpty());
		Assertions.assertEquals("the grandmother",
			noteText(model.getRecordsByType(IndividualReader.TYPE).getFirst(), GedcomTexts.t("title.named_after")));
	}

	@Test
	void aNamedAfterWithADateStaysAnEvent() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 EVEN the grandmother
			2 TYPE Named After
			2 DATE 1950
			""");

		Assertions.assertEquals(1, eventsOfType(model, "named_after").size());
	}


	private static final String SIBLINGS = """
		0 @F1@ FAM
		1 HUSB @I3@
		1 WIFE @I4@
		1 CHIL @I1@
		1 CHIL @I2@
		1 CHIL @I5@
		0 @I3@ INDI
		1 NAME Paolo /Rossi/
		0 @I4@ INDI
		1 NAME Lucia /Verdi/
		0 @I5@ INDI
		1 NAME Marta /Rossi/
		1 FAMC @F1@
		1 BIRT
		2 DATE 5 MAR 1931
		""";

	@Test
	void theTwinIsTheSiblingBornTheSameDayThatTheValueNames() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Angela /Rossi/
			1 FAMC @F1@
			1 BIRT
			2 DATE 12 JAN 1930
			1 EVEN to Giovanni Rossi
			2 TYPE Twin
			0 @I2@ INDI
			1 NAME Giovanni Domenico /Rossi/
			1 FAMC @F1@
			1 BIRT
			2 DATE 12 JAN 1930
			""" + SIBLINGS);

		Assertions.assertTrue(eventsOfType(model, "twin").isEmpty());
		final List<FLEFRecord> twins = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE))
			if(RelationshipReader.ENUM_TYPE_ASSOCIATE.equals(RelationshipReader.extractType(relationship)))
				twins.add(relationship);
		Assertions.assertEquals(1, twins.size());
		final FLEFRecord twin = twins.getFirst();
		Assertions.assertEquals("twin", FLEFRecordHelper.getChildValue(twin, RelationshipReader.TAG_ROLE));
		Assertions.assertEquals("I2", FLEFRecordHelper.getChildValue(twin, RelationshipReader.TAG_SUBJECT_INDIVIDUAL));
		Assertions.assertEquals("I1", FLEFRecordHelper.getChildValue(twin, RelationshipReader.TAG_TARGET_INDIVIDUAL));
	}

	@Test
	void theTwinStatedByBothSidesIsOneRelationship() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Angela /Rossi/
			1 FAMC @F1@
			1 BIRT
			2 DATE 12 JAN 1930
			1 EVEN to Giovanni Rossi
			2 TYPE Twin
			0 @I2@ INDI
			1 NAME Giovanni Domenico /Rossi/
			1 FAMC @F1@
			1 BIRT
			2 DATE 12 JAN 1930
			1 EVEN to Angela
			2 TYPE Twin
			""" + SIBLINGS);

		int associates = 0;
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE))
			if(RelationshipReader.ENUM_TYPE_ASSOCIATE.equals(RelationshipReader.extractType(relationship)))
				associates ++;
		Assertions.assertEquals(1, associates);
		Assertions.assertTrue(eventsOfType(model, "twin").isEmpty());
	}

	@Test
	void aTwinThatIsNotCertainStaysAnEvent() throws IOException{
		//the sibling of the value is born another day
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Angela /Rossi/
			1 FAMC @F1@
			1 BIRT
			2 DATE 12 JAN 1930
			1 EVEN to Marta Rossi
			2 TYPE Twin
			""" + SIBLINGS);

		Assertions.assertEquals(1, eventsOfType(model, "twin").size());
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE))
			Assertions.assertNotEquals(RelationshipReader.ENUM_TYPE_ASSOCIATE, RelationshipReader.extractType(relationship));
	}


	@Test
	void theMidwifeTakesPartInTheBirthWithThatRole() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 12 MAY 1940
			1 EVEN Maria Bianchi (born 25 JAN 1879 in Treviso)
			2 TYPE Midwife
			0 @I2@ INDI
			1 NAME Carlo /Verdi/
			1 BIRT
			2 DATE 3 JUN 1942
			1 EVEN Maria Bianchi
			2 TYPE Midwife
			""");

		Assertions.assertTrue(eventsOfType(model, "midwife").isEmpty());
		final List<FLEFRecord> midwives = new ArrayList<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE))
			if("midwife".equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE)))
				midwives.add(participation);
		Assertions.assertEquals(2, midwives.size());
		//the same name is the same midwife
		final String midwifeId = FLEFRecordHelper.getChildValue(midwives.getFirst(), FLEFRecordHelper.composePath(
			EventParticipationReader.TAG_PARTICIPANT, AgentReader.TAG_INDIVIDUAL));
		Assertions.assertEquals(midwifeId, FLEFRecordHelper.getChildValue(midwives.get(1), FLEFRecordHelper.composePath(
			EventParticipationReader.TAG_PARTICIPANT, AgentReader.TAG_INDIVIDUAL)));
		Assertions.assertEquals("Maria Bianchi", nameOf(model.getRecordById(midwifeId)));
		Assertions.assertEquals(3, model.getRecordsByType(IndividualReader.TYPE).size());
		//the event is the birth of the baby, and what was written after the name is kept
		final FLEFRecord birth = model.getRecordById(FLEFRecordHelper.getChildValue(midwives.getFirst(), EventParticipationReader.TAG_EVENT));
		Assertions.assertEquals(EventReader.ENUM_TYPE_BIRTH, FLEFRecordHelper.getChildValue(birth, EventReader.TAG_TYPE));
		Assertions.assertEquals("Maria Bianchi (born 25 JAN 1879 in Treviso)", noteText(midwives.getFirst(), GedcomTexts.t("title.midwife")));
	}

	@Test
	void aMidwifeWithoutBirthOrWithoutNameStaysAnEvent() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 EVEN Maria Bianchi
			2 TYPE Midwife
			0 @I2@ INDI
			1 NAME Carlo /Verdi/
			1 BIRT
			2 DATE 3 JUN 1942
			1 EVEN
			2 TYPE Midwife
			""");

		Assertions.assertEquals(2, eventsOfType(model, "midwife").size());
		Assertions.assertEquals(2, model.getRecordsByType(IndividualReader.TYPE).size());
	}

}
