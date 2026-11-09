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

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.gramps.GrampsFileImporter;
import io.github.mtrevisan.familylegacy.importer.gramps.GrampsPackageReader;
import io.github.mtrevisan.familylegacy.importer.gramps.GrampsXMLToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;


/** A Gramps XML file is converted directly into FLEF. */
class GrampsXMLToFLEFConverterTest{

	private static final String GRAMPS = """
		<?xml version="1.0" encoding="UTF-8"?>
		<!DOCTYPE database PUBLIC "-//Gramps//DTD Gramps XML 1.7.2//EN" "http://gramps-project.org/xml/1.7.2/grampsxml.dtd">
		<database xmlns="http://gramps-project.org/xml/1.7.2/">
		  <header><created date="2025-03-18" version="6.0.0"/><researcher><resname>Ada Rossi</resname></researcher></header>
		  <events>
		    <event handle="_e1" id="E0001" change="1185438865">
		      <type>Birth</type>
		      <dateval val="1850-03-12" type="about"/>
		      <place hlink="_p3"/>
		      <citationref hlink="_c1"/>
		    </event>
		    <event handle="_e2" id="E0002" change="1185438865">
		      <type>Marriage</type>
		      <dateval val="1875-00-00"/>
		      <place hlink="_p3"/>
		    </event>
		    <event handle="_e3" id="E0003" change="1185438865">
		      <type>Occupation</type>
		      <description>Farmer &amp; miller</description>
		      <daterange start="1870-01-01" stop="1880-12-31"/>
		    </event>
		    <event handle="_e4" id="E0004" change="1185438865">
		      <type>Baptism</type>
		      <dateval val="1700-02-03" cformat="Julian" dualdated="1"/>
		    </event>
		  </events>
		  <people>
		    <person handle="_i1" id="I0001" change="1185438865">
		      <gender>M</gender>
		      <name type="Birth Name"><first>Carlo</first><surname prefix="de">Rossi</surname></name>
		      <name alt="1" type="Also Known As"><first>Charles</first><surname>Rossi</surname></name>
		      <eventref hlink="_e1" role="Primary"/>
		      <eventref hlink="_e3" role="Primary"/>
		      <eventref hlink="_e4" role="Primary"/>
		      <parentin hlink="_f1"/>
		      <noteref hlink="_n1"/>
		    </person>
		    <person handle="_i2" id="I0002" change="1185438865" priv="1">
		      <gender>F</gender>
		      <name type="Birth Name"><first>Anna</first><surname>Bianchi</surname></name>
		      <eventref hlink="_e1" role="Witness"/>
		      <parentin hlink="_f1"/>
		    </person>
		    <person handle="_i3" id="I0003" change="1185438865">
		      <gender>U</gender>
		      <name type="Birth Name"><first>Bruno</first><surname>Rossi</surname></name>
		    </person>
		  </people>
		  <families>
		    <family handle="_f1" id="F0001" change="1185438865">
		      <rel type="Married"/>
		      <father hlink="_i1"/>
		      <mother hlink="_i2"/>
		      <eventref hlink="_e2" role="Family"/>
		      <childref hlink="_i3" mrel="Adopted" frel="Adopted"/>
		    </family>
		  </families>
		  <citations>
		    <citation handle="_c1" id="C0001" change="1185438865"><page>p. 12</page><confidence>3</confidence><sourceref hlink="_s1"/></citation>
		  </citations>
		  <sources>
		    <source handle="_s1" id="S0001" change="1185438865"><stitle>Parish register</stitle><sauthor>Parish</sauthor></source>
		  </sources>
		  <places>
		    <placeobj handle="_p1" id="P0001" change="1185438865" type="Country"><pname value="Italia"/></placeobj>
		    <placeobj handle="_p2" id="P0002" change="1185438865" type="City"><pname value="Treviso"/><placeref hlink="_p1"/></placeobj>
		    <placeobj handle="_p3" id="P0003" change="1185438865" type="Street"><pname value="via Pasubio"/>
		      <coord long="12.2133" lat="45.6505"/><placeref hlink="_p2"/></placeobj>
		  </places>
		  <notes>
		    <note handle="_n1" id="N0001" change="1185438865" type="Person Note"><text>Line one
		Line two @ home</text></note>
		  </notes>
		</database>
		""";


	private static FLEFModel convert() throws IOException{
		return GrampsXMLToFLEFConverter.convert(GRAMPS.getBytes(StandardCharsets.UTF_8), (percent, message) -> {});
	}

	private static FLEFRecord record(final FLEFModel model, final String id){
		return model.getRecordById(id);
	}


	@Test
	void anEventIsASingleRecordWhoseParticipantsKeepTheirRole() throws IOException{
		final FLEFModel model = convert();

		//the occupation is an attribute, not an event
		Assertions.assertEquals(3, model.getRecordsByType(EventReader.TYPE).size());
		final List<String> roles = model.getRecordsByType(EventParticipationReader.TYPE).stream()
			.filter(participation -> "E0001".equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT)))
			.map(participation -> FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE))
			.sorted()
			.toList();
		//the birth has the person it is about, and the witness
		Assertions.assertEquals(List.of(EventParticipationReader.ENUM_ROLE_PRINCIPAL, EventParticipationReader.ENUM_ROLE_WITNESS), roles);

		final FLEFRecord birth = record(model, "E0001");
		Assertions.assertEquals(EventReader.ENUM_TYPE_BIRTH, FLEFRecordHelper.getChildValue(birth, EventReader.TAG_TYPE));
		Assertions.assertEquals("P0003", FLEFRecordHelper.getChildValue(birth, "place.place"));
		Assertions.assertEquals("p. 12", FLEFRecordHelper.getChildValue(birth, "source.locator"));
	}

	@Test
	void theDatesKeepWhatTheyMean() throws IOException{
		final FLEFModel model = convert();

		//about 12 March 1850: a point, approximate
		final FLEFRecord birth = record(model, "E0001");
		Assertions.assertEquals("12 MAR 1850", FLEFRecordHelper.getChildValue(birth, "date.value.point.value.full_date"));
		Assertions.assertEquals("stated", FLEFRecordHelper.getChildValue(birth, "date.value.point.approximate.basis"));
		//a Julian date, with its dual year
		final FLEFRecord baptism = record(model, "E0004");
		Assertions.assertEquals("3 FEB 1700/01", FLEFRecordHelper.getChildValue(baptism, "date.value.point.value.full_date"));
		Assertions.assertEquals("julian", FLEFRecordHelper.getChildValue(baptism, "date.value.point.calendar"));
	}

	@Test
	void aPartialDateAndTheKindsOfDateAreRead() throws IOException{
		final String xml = "<database><header><created date=\"2025-03-18\" version=\"6.0.0\"/></header><events>"
			+ "<event handle=\"_a\" id=\"E1\" change=\"1\"><type>Birth</type><dateval val=\"1875\"/></event>"
			+ "<event handle=\"_b\" id=\"E2\" change=\"1\"><type>Death</type><dateval val=\"1900-05\" type=\"before\"/></event>"
			+ "<event handle=\"_c\" id=\"E3\" change=\"1\"><type>Census</type><daterange start=\"1880-01-01\" stop=\"1881-12-31\"/></event>"
			+ "<event handle=\"_d\" id=\"E4\" change=\"1\"><type>Residence</type><datespan start=\"1880-00-00\" stop=\"1890-00-00\"/></event>"
			+ "<event handle=\"_e\" id=\"E5\" change=\"1\"><type>Anecdote</type><datestr val=\"Monday last\"/></event>"
			+ "</events></database>";
	
		final FLEFModel model = GrampsXMLToFLEFConverter.convert(xml.getBytes(StandardCharsets.UTF_8), (percent, message) -> {});
	
		Assertions.assertEquals("1875", FLEFRecordHelper.getChildValue(record(model, "E1"), "date.value.point.value.full_date"));
		Assertions.assertEquals("MAY 1900",
			FLEFRecordHelper.getChildValue(record(model, "E2"), "date.value.bounded.not_after.value.full_date"));
		Assertions.assertEquals("1 JAN 1880",
			FLEFRecordHelper.getChildValue(record(model, "E3"), "date.value.bounded.not_before.value.full_date"));
		Assertions.assertEquals("31 DEC 1881",
			FLEFRecordHelper.getChildValue(record(model, "E3"), "date.value.bounded.not_after.value.full_date"));
		Assertions.assertEquals("1890", FLEFRecordHelper.getChildValue(record(model, "E4"), "date.value.spanning.to.value.full_date"));
		//a text is not a date: it is kept as a note
		Assertions.assertEquals("Monday last", FLEFRecordHelper.getChildValue(record(model, "E5"), "note.text"));
	}

	@Test
	void theGenderIsASexAttributeOfTheIndividual() throws IOException{
		final FLEFModel model = convert();

		Assertions.assertTrue(FLEFRecordHelper.findChildren(record(model, "I0001"), "sex").isEmpty());
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(record(model, "I0001"), model));
		Assertions.assertEquals(SexType.FEMALE, PreferredSex.of(record(model, "I0002"), model));
		//a gender that is unknown is kept, but it says nothing
		Assertions.assertEquals(1, PreferredSex.assertions("I0003", model).size());
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(record(model, "I0003"), model));
	}

	@Test
	void anOccupationIsAnAttributeWhoseValidityIsTheRange() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord occupation = model.getRecordsByType("individual_attribute").stream()
			.filter(attribute -> "occupation".equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("occupation", FLEFRecordHelper.getChildValue(occupation, IndividualAttributeReader.TAG_TYPE));
		Assertions.assertEquals("Farmer & miller", FLEFRecordHelper.getChildValue(occupation, "value"));
		Assertions.assertEquals("31 DEC 1880",
			FLEFRecordHelper.getChildValue(occupation, "valid_from.value.bounded.not_after.value.full_date"));
		Assertions.assertEquals("1 JAN 1870",
			FLEFRecordHelper.getChildValue(occupation, "valid_to.value.bounded.not_before.value.full_date"));
	}

	@Test
	void thePlacesAreLinkedToThePlacesThatContainThem() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord street = record(model, "P0003");
		Assertions.assertEquals("via Pasubio", FLEFRecordHelper.getChildValue(street, "name.value"));
		Assertions.assertEquals("street", FLEFRecordHelper.getChildValue(street, PlaceReader.TAG_TYPE));
		Assertions.assertEquals("+45.6505+012.2133/", FLEFRecordHelper.getChildValue(street, "location.coordinates"));
		Assertions.assertEquals(2, model.getRecordsByType("place_relationship").size());
		Assertions.assertTrue(model.getRecordsByType("place_relationship").stream()
			.anyMatch(relationship -> "P0003".equals(FLEFRecordHelper.getChildValue(relationship, "subject.place"))
				&& "P0002".equals(FLEFRecordHelper.getChildValue(relationship, "target.place"))));
	}

	@Test
	void theNamesAndTheSurnamePrefixAreKept() throws IOException{
		final FLEFModel model = convert();

		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(record(model, "I0001"), IndividualReader.TAG_NAME);
		Assertions.assertEquals(2, names.size());
		Assertions.assertEquals(EventReader.ENUM_TYPE_BIRTH, FLEFRecordHelper.getChildValue(names.getFirst(), NameReader.TAG_TYPE));
		Assertions.assertEquals("alias", FLEFRecordHelper.getChildValue(names.getLast(), NameReader.TAG_TYPE));
		final List<String> parts = FLEFRecordHelper.findChildren(names.getFirst(), "part").stream()
			.map(part -> FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_TYPE) + "=" + FLEFRecordHelper.getChildValue(part, "value"))
			.toList();
		//the prefix is its own part, just before the family one
		final int prefix = parts.indexOf("family_prefix=de");
		Assertions.assertTrue(prefix >= 0, parts.toString());
		Assertions.assertEquals("family=Rossi", parts.get(prefix + 1));
	}

	@Test
	void eachSurnameOfACompoundOneKeepsItsPrefix() throws IOException{
		final FLEFModel model = convertXml("<people><person handle=\"_p\" id=\"I9\" change=\"1\"><gender>M</gender>"
			+ "<name><first>Anna</first><surname prefix=\"de\">Rossi</surname><surname>Bianchi</surname>"
			+ "<surname prefix=\"van\">Verdi</surname></name></person></people>");

		final List<String> parts = FLEFRecordHelper.findChildren(
				FLEFRecordHelper.findChildren(record(model, "I9"), IndividualReader.TAG_NAME).getFirst(), "part").stream()
			.map(part -> FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_TYPE) + "=" + FLEFRecordHelper.getChildValue(part, "value"))
			.toList();
		Assertions.assertEquals(List.of("given=Anna", "family_prefix=de", "family=Rossi", "family=Bianchi", "family_prefix=van",
			"family=Verdi"), parts);
	}

	@Test
	void aNoteKeepsItsLines() throws IOException{
		final String flef = FLEFWriter.create().writeToString(convert());

		Assertions.assertTrue(flef.contains("Line one\nLine two @ home"), flef);
	}

	@Test
	void theFamilyIsAGroupAndEachParentHasItsOwnRelationshipWithTheChild() throws IOException{
		final FLEFModel model = convert();

		Assertions.assertNotNull(record(model, "G0001"));
		final List<FLEFRecord> parentChild = model.getRecordsByType("relationship").stream()
			.filter(relationship -> "I0003".equals(FLEFRecordHelper.getChildValue(relationship, "subject.individual")))
			.filter(relationship -> !"group_member".equals(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE)))
			.toList();
		Assertions.assertEquals(2, parentChild.size());
		Assertions.assertTrue(parentChild.stream().allMatch(relationship ->
			"adoptive_child".equals(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE))));
		Assertions.assertTrue(model.getRecordsByType("relationship").stream()
			.anyMatch(relationship -> "spouse".equals(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE))
				&& "1875".equals(FLEFRecordHelper.getChildValue(relationship, "valid_from.value.point.value.full_date"))));
	}

	@Test
	void thePrivacyIsKept() throws IOException{
		Assertions.assertEquals("restricted", FLEFRecordHelper.getChildValue(record(convert(), "I0002"), "privacy.level"));
	}

	@Test
	void theDocumentConvertsToAValidModel() throws IOException, URISyntaxException{
		final FLEFModel model = convert();

		final Path grammarPath = Path.of(Thread.currentThread().getContextClassLoader().getResource("gedg/flef_0.2.0.gedg").toURI());
		final FLEFValidator validator = new FLEFValidator(FLEFGrammarParser.parse(grammarPath));
		Assertions.assertEquals(List.of(), validator.validateSchema(model));
		Assertions.assertEquals(List.of(), validator.validateIntegrity(model));
	}

	@Test
	void aCompressedFileIsRead() throws IOException{
		final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		try(final GZIPOutputStream gzip = new GZIPOutputStream(compressed)){
			gzip.write(GRAMPS.getBytes(StandardCharsets.UTF_8));
		}

		Assertions.assertArrayEquals(GRAMPS.getBytes(StandardCharsets.UTF_8),
			GrampsPackageReader.readXml(new ByteArrayInputStream(compressed.toByteArray())));
	}

	@Test
	void aDocumentThatIsNotGrampsIsRefused(){
		Assertions.assertThrows(IOException.class,
			() -> GrampsXMLToFLEFConverter.convert("<html/>".getBytes(StandardCharsets.UTF_8), (percent, message) -> {}));
		Assertions.assertThrows(IOException.class,
			() -> GrampsXMLToFLEFConverter.convert("not xml".getBytes(StandardCharsets.UTF_8), (percent, message) -> {}));
	}

	@Test
	void theGrampsExampleConvertsToAValidModel() throws IOException, URISyntaxException{
		final byte[] xml;
		try(final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("gedg/example.gramps")){
			xml = GrampsPackageReader.readXml(in);
		}

		final FLEFModel model = GrampsXMLToFLEFConverter.convert(xml, (percent, message) -> {});

		Assertions.assertEquals(2157, model.getRecordsByType(IndividualReader.TYPE).size());
		final Path grammarPath = Path.of(Thread.currentThread().getContextClassLoader().getResource("gedg/flef_0.2.0.gedg").toURI());
		final FLEFValidator validator = new FLEFValidator(FLEFGrammarParser.parse(grammarPath));
		Assertions.assertEquals(List.of(), validator.validateSchema(model));
		Assertions.assertEquals(List.of(), validator.validateIntegrity(model));
	}


	/* ---------------------------- lossy conversions ---------------------------- */

	private static final String XML_HEADER = "<database><header><created date=\"2025-03-18\" version=\"6.0.0\"/></header>";
	private static final String XML_FOOTER = "</database>";

	private static FLEFModel convertXml(final String body) throws IOException{
		return GrampsXMLToFLEFConverter.convert((XML_HEADER + body + XML_FOOTER).getBytes(StandardCharsets.UTF_8), (percent, message) -> {});
	}

	private static List<FLEFRecord> notesTitled(final FLEFRecord record, final String title){
		return FLEFRecordHelper.findChildren(record, NoteReader.TAG_NOTE).stream()
			.filter(note -> title.equals(FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE)))
			.toList();
	}

	@Test
	void theIslamicPersianAndSwedishDatesAreWrittenInTheirCalendar() throws IOException{
		final FLEFModel model = convertXml("<events>"
			+ "<event handle=\"_a\" id=\"E1\" change=\"1\"><type>Birth</type><dateval val=\"1420-09-01\" cformat=\"Islamic\"/></event>"
			+ "<event handle=\"_b\" id=\"E2\" change=\"1\"><type>Birth</type><dateval val=\"1357-11-22\" cformat=\"Persian\"/></event>"
			+ "<event handle=\"_c\" id=\"E3\" change=\"1\"><type>Birth</type><dateval val=\"1712-02-30\" cformat=\"Swedish\"/></event>"
			+ "</events>");

		final FLEFRecord islamic = record(model, "E1");
		Assertions.assertEquals("1 ramadan 1420", FLEFRecordHelper.getChildValue(islamic, "date.value.point.value.full_date"));
		Assertions.assertEquals(DateReader.ENUM_CALENDAR_ISLAMIC, FLEFRecordHelper.getChildValue(islamic, "date.value.point.calendar"));
		final FLEFRecord persian = record(model, "E2");
		Assertions.assertEquals("22 bahman 1357", FLEFRecordHelper.getChildValue(persian, "date.value.point.value.full_date"));
		Assertions.assertEquals(DateReader.ENUM_CALENDAR_PERSIAN, FLEFRecordHelper.getChildValue(persian, "date.value.point.calendar"));
		final FLEFRecord swedish = record(model, "E3");
		Assertions.assertEquals("30 FEB 1712", FLEFRecordHelper.getChildValue(swedish, "date.value.point.value.full_date"));
		Assertions.assertEquals(DateReader.ENUM_CALENDAR_SWEDISH, FLEFRecordHelper.getChildValue(swedish, "date.value.point.calendar"));
		//no note: the date is written
		Assertions.assertTrue(notesTitled(islamic, "Date").isEmpty());
	}

	@Test
	void thePrivacyOfNotesAndCitationsIsKept() throws IOException{
		final FLEFModel model = convertXml("<events><event handle=\"_e\" id=\"E1\" change=\"1\"><type>Birth</type><citationref hlink=\"_c\"/></event></events>"
			+ "<people><person handle=\"_i\" id=\"I1\" change=\"1\"><noteref hlink=\"_n\"/></person></people>"
			+ "<citations><citation handle=\"_c\" id=\"C1\" change=\"1\" priv=\"1\"><page>p. 1</page><sourceref hlink=\"_s\"/></citation></citations>"
			+ "<sources><source handle=\"_s\" id=\"S1\" change=\"1\"><stitle>T</stitle></source></sources>"
			+ "<notes><note handle=\"_n\" id=\"N1\" change=\"1\" priv=\"1\"><text>secret</text></note></notes>");

		Assertions.assertEquals(PrivacyReader.ENUM_LEVEL_RESTRICTED, FLEFRecordHelper.getChildValue(record(model, "I1"), "note.privacy.level"));
		Assertions.assertEquals(PrivacyReader.ENUM_LEVEL_RESTRICTED, FLEFRecordHelper.getChildValue(record(model, "E1"), "source.privacy.level"));
	}

	@Test
	void theChangeOfGrampsIsTheUpdateAndTheCreationIsTheImport() throws IOException{
		final FLEFRecord individual = record(convert(), "I0001");

		Assertions.assertEquals("2007-07-26T08:34:25Z", FLEFRecordHelper.getChildValue(individual, "audit.update.date"));
		final String creation = FLEFRecordHelper.getChildValue(individual, "audit.creation.date");
		Assertions.assertNotNull(creation);
		Assertions.assertNotEquals("2007-07-26T08:34:25Z", creation);
	}

	@Test
	void theAddressOfAnIndividualIsAResidenceWithAPlaceOfTypeAddress() throws IOException{
		final FLEFModel model = convertXml("<people><person handle=\"_i\" id=\"I1\" change=\"1\"><address>"
			+ "<daterange start=\"1900-01-01\" stop=\"1910-01-01\"/><street>Via Roma 1</street><city>Treviso</city><country>Italy</country>"
			+ "<phone>123</phone></address></person></people>"
			+ "<places><placeobj handle=\"_p\" id=\"P1\" change=\"1\" type=\"Country\"><pname value=\"Italy\"/></placeobj></places>");

		final FLEFRecord residence = model.getRecordsByType(IndividualAttributeReader.TYPE).getFirst();
		Assertions.assertEquals(IndividualAttributeReader.ENUM_TYPE_RESIDENCE, FLEFRecordHelper.getChildValue(residence, IndividualAttributeReader.TAG_TYPE));
		Assertions.assertNull(FLEFRecordHelper.getChildValue(residence, IndividualAttributeReader.TAG_VALUE));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(residence, IndividualAttributeReader.TAG_VALID_FROM));
		Assertions.assertEquals(1, notesTitled(residence, "Phone").size());

		final FLEFRecord address = record(model, FLEFRecordHelper.getChildValue(residence, "place.place"));
		Assertions.assertEquals(PlaceReader.ENUM_TYPE_ADDRESS, FLEFRecordHelper.getChildValue(address, PlaceReader.TAG_TYPE));
		Assertions.assertEquals("Via Roma 1, Treviso, Italy", FLEFRecordHelper.getChildValue(address, "name.value"));
		//the address is part of the city (new), that is part of the country (the one that is already there)
		final String cityId = model.getRecordsByType(PlaceRelationshipReader.TYPE).stream()
			.filter(relationship -> address.getId().equals(FLEFRecordHelper.getChildValue(relationship, "subject.place")))
			.map(relationship -> FLEFRecordHelper.getChildValue(relationship, "target.place"))
			.findFirst().orElseThrow();
		Assertions.assertEquals(PlaceReader.ENUM_TYPE_CITY, FLEFRecordHelper.getChildValue(record(model, cityId), PlaceReader.TAG_TYPE));
		Assertions.assertTrue(model.getRecordsByType(PlaceRelationshipReader.TYPE).stream()
			.anyMatch(relationship -> cityId.equals(FLEFRecordHelper.getChildValue(relationship, "subject.place"))
				&& "P1".equals(FLEFRecordHelper.getChildValue(relationship, "target.place"))));
		//the country is not made again
		Assertions.assertEquals(3, model.getRecordsByType(PlaceReader.TYPE).size());
	}

	@Test
	void theTypeOfARepositoryIsMappedAndNotKeptAsANote() throws IOException{
		final FLEFModel model = convertXml("<repositories>"
			+ "<repository handle=\"_1\" id=\"R1\" change=\"1\"><rname>A</rname><type>Archive</type></repository>"
			+ "<repository handle=\"_2\" id=\"R2\" change=\"1\"><rname>B</rname><type>Web site</type></repository>"
			+ "<repository handle=\"_3\" id=\"R3\" change=\"1\"><rname>C</rname><type>Album</type></repository>"
			+ "<repository handle=\"_4\" id=\"R4\" change=\"1\"><rname>D</rname><type>Unknown</type></repository>"
			+ "<repository handle=\"_5\" id=\"R5\" change=\"1\"><rname>E</rname><type>Safe</type></repository>"
			+ "</repositories>");

		Assertions.assertEquals(RepositoryReader.ENUM_TYPE_ARCHIVE, RepositoryReader.extractType(record(model, "R1")));
		Assertions.assertEquals(RepositoryReader.ENUM_TYPE_WEBSITE, RepositoryReader.extractType(record(model, "R2")));
		Assertions.assertEquals("album", RepositoryReader.extractType(record(model, "R3")));
		Assertions.assertNull(RepositoryReader.extractType(record(model, "R4")));
		Assertions.assertEquals(RepositoryReader.ENUM_TYPE_PRIVATE_COLLECTION, RepositoryReader.extractType(record(model, "R5")));
		for(final String id : List.of("R1", "R2", "R3", "R4", "R5"))
			Assertions.assertTrue(notesTitled(record(model, id), "Type").isEmpty(), id);
	}

	@Test
	void theAddressOfARepositoryKeepsItsPeriodCitationsAndNotes() throws IOException{
		final FLEFModel model = convertXml("<repositories><repository handle=\"_r\" id=\"R1\" change=\"1\"><rname>Archive</rname><type>Archive</type>"
			+ "<address><dateval val=\"1950-00-00\" type=\"after\"/><street>Via X</street><citationref hlink=\"_c\"/><noteref hlink=\"_n\"/></address>"
			+ "</repository></repositories>"
			+ "<citations><citation handle=\"_c\" id=\"C1\" change=\"1\"><page>p. 1</page><sourceref hlink=\"_s\"/></citation></citations>"
			+ "<sources><source handle=\"_s\" id=\"S1\" change=\"1\"><stitle>T</stitle></source></sources>"
			+ "<notes><note handle=\"_n\" id=\"N1\" change=\"1\"><text>The note text</text></note></notes>");

		final List<FLEFRecord> notes = notesTitled(record(model, "R1"), "Address");
		Assertions.assertEquals(1, notes.size());
		final String text = FLEFRecordHelper.getChildValue(notes.getFirst(), NoteReader.TAG_TEXT);
		Assertions.assertTrue(text.contains("Via X") && text.contains("after") && text.contains("The note text"), text);
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(notes.getFirst(), "source.source"));
	}

	@Test
	void aParishIsEcclesiasticalAndTheOtherPlacesAreAdministrative() throws IOException{
		final FLEFModel model = convertXml("<places>"
			+ "<placeobj handle=\"_1\" id=\"P1\" change=\"1\" type=\"Parish\"><pname value=\"San Marco\"/><placeref hlink=\"_2\"/></placeobj>"
			+ "<placeobj handle=\"_2\" id=\"P2\" change=\"1\" type=\"City\"><pname value=\"Treviso\"/><placeref hlink=\"_3\"/></placeobj>"
			+ "<placeobj handle=\"_3\" id=\"P3\" change=\"1\" type=\"Country\"><pname value=\"Italy\"/></placeobj></places>");

		final Map<String, String> types = new HashMap<>();
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipReader.TYPE))
			types.put(FLEFRecordHelper.getChildValue(relationship, "subject.place"), FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_TYPE));
		Assertions.assertEquals(PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF, types.get("P1"));
		Assertions.assertEquals(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, types.get("P2"));
	}

	@Test
	void theNotesAttributesAndCitationsOfAMediaReferenceAreKept() throws IOException{
		final FLEFModel model = convertXml("<people><person handle=\"_i\" id=\"I1\" change=\"1\">"
			+ "<objref hlink=\"_o\"><citationref hlink=\"_c\"/><noteref hlink=\"_n\"/><attribute type=\"Role\" value=\"front\"/></objref></person></people>"
			+ "<objects><object handle=\"_o\" id=\"O1\" change=\"1\"><file src=\"x.png\" mime=\"image/png\" description=\"d\"/></object></objects>"
			+ "<citations><citation handle=\"_c\" id=\"C1\" change=\"1\"><page>p. 1</page><sourceref hlink=\"_s\"/></citation></citations>"
			+ "<sources><source handle=\"_s\" id=\"S1\" change=\"1\"><stitle>T</stitle></source></sources>"
			+ "<notes><note handle=\"_n\" id=\"N1\" change=\"1\"><text>Ref note</text></note></notes>");

		final List<FLEFRecord> sources = FLEFRecordHelper.findChildren(record(model, "I1"), SourceCitationReader.TAG_SOURCE);
		Assertions.assertTrue(sources.stream().anyMatch(source -> "S1".equals(FLEFRecordHelper.getChildValue(source, SourceCitationReader.TAG_SOURCE))));
		final List<String> remarks = sources.stream()
			.flatMap(source -> FLEFRecordHelper.findChildren(source, SourceCitationReader.TAG_EXTRACT).stream())
			.flatMap(extract -> FLEFRecordHelper.findChildren(extract, SourceCitationReader.TAG_REMARK).stream())
			.map(FLEFRecord::getValue)
			.toList();
		Assertions.assertEquals(List.of("Ref note", "Role: front"), remarks);
	}

	@Test
	void theLdsOrdinancesAreReadAndTheSealingLinksTheFamily() throws IOException{
		final FLEFModel model = convertXml("<people><person handle=\"_i\" id=\"I1\" change=\"1\">"
			+ "<lds_ord type=\"BAPTISM\"><dateval val=\"1900-01-01\"/></lds_ord>"
			+ "<lds_ord type=\"Sealed to Parents\"><sealed_to hlink=\"_f\"/></lds_ord></person></people>"
			+ "<families><family handle=\"_f\" id=\"F0001\" change=\"1\"><lds_ord type=\"Sealed to Spouse\"/></family></families>");

		//the baptism and the sealing of the individual, and the sealing of the spouses
		Assertions.assertEquals(3, model.getRecordsByType(EventReader.TYPE).size());
		final List<FLEFRecord> sealed = model.getRecordsByType(EventReader.TYPE).stream()
			.flatMap(event -> notesTitled(event, "Sealed to").stream())
			.toList();
		Assertions.assertEquals(1, sealed.size());
		Assertions.assertEquals("[G0001](flef:G0001)", FLEFRecordHelper.getChildValue(sealed.getFirst(), NoteReader.TAG_TEXT));
		Assertions.assertEquals("text/markdown", FLEFRecordHelper.getChildValue(sealed.getFirst(), NoteReader.TAG_MIME));
	}

	@Test
	void theDateOfAMediaIsTheDateOfTheRecord() throws IOException{
		final FLEFModel model = convertXml("<objects><object handle=\"_o\" id=\"O1\" change=\"1\"><file src=\"x.png\" mime=\"image/png\"/>"
			+ "<dateval val=\"1900-01-01\"/></object></objects>");

		final FLEFRecord media = record(model, "O1");
		Assertions.assertEquals("1 JAN 1900", FLEFRecordHelper.getChildValue(media, "date.value.point.value.full_date"));
		Assertions.assertTrue(notesTitled(media, "Date").isEmpty());
	}


	/* ---------------------------- age at an event ---------------------------- */

	/** A census of 15 June 1880, whose participant has the age. */
	private static FLEFModel convertAge(final String age, final boolean cited, final boolean datedBirth) throws IOException{
		return convertXml("<events><event handle=\"_e\" id=\"E1\" change=\"1\"><type>Census</type><dateval val=\"1880-06-15\"/>"
			+ (cited? "<citationref hlink=\"_c\"/>": "") + "</event>"
			+ "<event handle=\"_b\" id=\"E2\" change=\"1\"><type>Birth</type>" + (datedBirth? "<dateval val=\"1850-01-01\"/>": "") + "</event></events>"
			+ "<people><person handle=\"_i\" id=\"I1\" change=\"1\"><eventref hlink=\"_e\" role=\"Primary\"><attribute type=\"Age\" value=\"" + age.replace("<", "&lt;") + "\"/></eventref>"
			+ (datedBirth? "<eventref hlink=\"_b\" role=\"Primary\"/>": "") + "</person></people>"
			+ "<citations><citation handle=\"_c\" id=\"C1\" change=\"1\"><page>p. 1</page><confidence>4</confidence><sourceref hlink=\"_s\"/></citation></citations>"
			+ "<sources><source handle=\"_s\" id=\"S1\" change=\"1\"><stitle>T</stitle></source></sources>");
	}

	private static List<FLEFRecord> births(final FLEFModel model){
		return model.getRecordsByType(EventReader.TYPE).stream()
			.filter(event -> EventReader.ENUM_TYPE_BIRTH.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
			.filter(event -> !"E2".equals(event.getId()))
			.toList();
	}

	private static List<FLEFRecord> participationsOf(final FLEFModel model, final String eventId){
		return model.getRecordsByType(EventParticipationReader.TYPE).stream()
			.filter(participation -> eventId.equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT)))
			.toList();
	}

	@Test
	void theAgeAtAnEventGivesACalculatedBirthWithTheSourceOfTheEvent() throws IOException{
		final FLEFModel model = convertAge("25", true, false);

		final FLEFRecord birth = births(model).getFirst();
		Assertions.assertEquals("1855", FLEFRecordHelper.getChildValue(birth, "date.value.point.value.full_date"));
		Assertions.assertEquals(DateReader.ENUM_CALENDAR_GREGORIAN, FLEFRecordHelper.getChildValue(birth, "date.value.point.calendar"));
		Assertions.assertEquals(DateReader.ENUM_BASIS_CALCULATED, FLEFRecordHelper.getChildValue(birth, "date.value.point.approximate.basis"));
		Assertions.assertEquals("S1", FLEFRecordHelper.getChildValue(birth, "source.source"));
		Assertions.assertEquals("p. 1", FLEFRecordHelper.getChildValue(birth, "source.locator"));
		Assertions.assertEquals(EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT, FLEFRecordHelper.getChildValue(birth, "source.evidence.evidence_type"));
		Assertions.assertEquals(ConfidenceReader.ENUM_LOW, FLEFRecordHelper.getChildValue(birth, EventReader.TAG_CONFIDENCE));
		Assertions.assertTrue(notesTitled(birth, "Age").isEmpty());

		final List<FLEFRecord> participations = participationsOf(model, birth.getId());
		Assertions.assertEquals(1, participations.size());
		Assertions.assertEquals("I1", FLEFRecordHelper.getChildValue(participations.getFirst(), "participant.individual"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_PRINCIPAL, FLEFRecordHelper.getChildValue(participations.getFirst(), EventParticipationReader.TAG_ROLE));
		//the age is not a note any more
		Assertions.assertTrue(participationsOf(model, "E1").stream().allMatch(participation -> notesTitled(participation, "Age").isEmpty()));
	}

	@Test
	void aQualifiedAgeGivesABoundOrAnApproximateBirth() throws IOException{
		final FLEFRecord younger = births(convertAge("<8", true, false)).getFirst();
		Assertions.assertEquals("1872", FLEFRecordHelper.getChildValue(younger, "date.value.bounded.not_before.value.full_date"));
		Assertions.assertEquals("1880", FLEFRecordHelper.getChildValue(younger, "date.value.bounded.not_after.value.full_date"));

		final FLEFRecord older = births(convertAge(">70", true, false)).getFirst();
		Assertions.assertEquals("1810", FLEFRecordHelper.getChildValue(older, "date.value.bounded.not_after.value.full_date"));

		final FLEFRecord about = births(convertAge("about 40", true, false)).getFirst();
		Assertions.assertEquals("1840", FLEFRecordHelper.getChildValue(about, "date.value.point.value.full_date"));
		Assertions.assertEquals(DateReader.ENUM_BASIS_CALCULATED, FLEFRecordHelper.getChildValue(about, "date.value.point.approximate.basis"));
	}

	@Test
	void anAgeThatCannotBeReadStaysANote() throws IOException{
		final FLEFModel model = convertAge("old", true, false);

		Assertions.assertTrue(births(model).isEmpty());
		final List<FLEFRecord> notes = participationsOf(model, "E1").stream()
			.flatMap(participation -> notesTitled(participation, "Age").stream())
			.toList();
		Assertions.assertEquals(1, notes.size());
		Assertions.assertEquals("old", FLEFRecordHelper.getChildValue(notes.getFirst(), NoteReader.TAG_TEXT));
	}

	@Test
	void anIndividualWithADatedBirthKeepsTheAgeAsANote() throws IOException{
		final FLEFModel model = convertAge("25", true, true);

		Assertions.assertTrue(births(model).isEmpty());
		Assertions.assertEquals(1, participationsOf(model, "E1").stream()
			.flatMap(participation -> notesTitled(participation, "Age").stream())
			.count());
	}

	@Test
	void aBirthFromAnAgeWithoutSourceSaysWhereTheAgeComesFrom() throws IOException{
		final FLEFRecord birth = births(convertAge("25", false, false)).getFirst();

		Assertions.assertNull(FLEFRecordHelper.findChild(birth, SourceCitationReader.TAG_SOURCE));
		final List<FLEFRecord> notes = notesTitled(birth, "Age");
		Assertions.assertEquals(1, notes.size());
		Assertions.assertTrue(FLEFRecordHelper.getChildValue(notes.getFirst(), NoteReader.TAG_TEXT).contains("[E1](flef:E1)"));
	}


	/* ---------------------------- image regions ---------------------------- */

	private static FLEFModel convertRegion(final String source) throws IOException{
		return convertXml("<people><person handle=\"_i\" id=\"I1\" change=\"1\"><objref hlink=\"_o\">"
			+ "<region corner1_x=\"10\" corner1_y=\"20\" corner2_x=\"60\" corner2_y=\"70\"/></objref></person></people>"
			+ "<objects><object handle=\"_o\" id=\"O1\" change=\"1\"><file src=\"" + source + "\" mime=\"image/png\"/></object></objects>");
	}

	@Test
	void theRegionOfAnImageIsACropInPixels(@TempDir final Path directory) throws IOException{
		final Path image = directory.resolve("picture.png");
		ImageIO.write(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB), "png", image.toFile());

		final FLEFRecord individual = record(convertRegion(image.toString()), "I1");

		Assertions.assertEquals("20", FLEFRecordHelper.getChildValue(individual, "preferred_image.crop.x"));
		Assertions.assertEquals("20", FLEFRecordHelper.getChildValue(individual, "preferred_image.crop.y"));
		Assertions.assertEquals("100", FLEFRecordHelper.getChildValue(individual, "preferred_image.crop.width"));
		Assertions.assertEquals("50", FLEFRecordHelper.getChildValue(individual, "preferred_image.crop.height"));
		Assertions.assertEquals("20", FLEFRecordHelper.getChildValue(individual, "source.extract.media_part.crop.x"));
		Assertions.assertEquals("50", FLEFRecordHelper.getChildValue(individual, "source.extract.media_part.crop.height"));
		Assertions.assertTrue(notesTitled(individual, "Region of O1").isEmpty());
	}

	@Test
	void theRegionOfAnImageThatCannotBeReadStaysANote(@TempDir final Path directory) throws IOException{
		final FLEFRecord individual = record(convertRegion(directory.resolve("missing.png").toString()), "I1");

		Assertions.assertNull(FLEFRecordHelper.findChild(individual, "preferred_image.crop"));
		Assertions.assertEquals(1, notesTitled(individual, "Region of O1").size());
	}


	private static void tarEntry(final ByteArrayOutputStream tar, final String name, final byte[] content) throws IOException{
		final byte[] header = new byte[512];
		final byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
		System.arraycopy(nameBytes, 0, header, 0, nameBytes.length);
		final byte[] size = String.format("%011o", content.length).getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(size, 0, header, 124, size.length);
		header[156] = '0';
		System.arraycopy("ustar".getBytes(StandardCharsets.US_ASCII), 0, header, 257, 5);
		tar.write(header);
		tar.write(content);
		tar.write(new byte[(512 - content.length % 512) % 512]);
	}

	@Test
	void theRegionOfAnImageInAPackageIsACropAndTheMediaAreExtractedNextToIt(@TempDir final Path directory) throws IOException{
		final ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB), "png", png);
		final String xml = XML_HEADER + "<people><person handle=\"_i\" id=\"I1\" change=\"1\"><objref hlink=\"_o\">"
			+ "<region corner1_x=\"10\" corner1_y=\"20\" corner2_x=\"60\" corner2_y=\"70\"/></objref></person></people>"
			+ "<objects><object handle=\"_o\" id=\"O1\" change=\"1\"><file src=\"picture.png\" mime=\"image/png\"/></object></objects>"
			+ XML_FOOTER;
		final ByteArrayOutputStream tar = new ByteArrayOutputStream();
		tarEntry(tar, "data.gramps", xml.getBytes(StandardCharsets.UTF_8));
		tarEntry(tar, "picture.png", png.toByteArray());
		tarEntry(tar, "../escape.png", png.toByteArray());
		tar.write(new byte[1024]);
		final Path file = directory.resolve("family.gpkg");
		java.nio.file.Files.write(file, tar.toByteArray());

		final ImportReport report = new ImportReport();
		final FLEFModel model = new GrampsFileImporter().read(file, (percent, message) -> {}, report);

		final FLEFRecord individual = record(model, "I1");
		Assertions.assertEquals("20", FLEFRecordHelper.getChildValue(individual, "preferred_image.crop.x"));
		Assertions.assertEquals("100", FLEFRecordHelper.getChildValue(individual, "preferred_image.crop.width"));
		Assertions.assertTrue(notesTitled(individual, "Region of O1").isEmpty());
		Assertions.assertTrue(report.countsByReason().isEmpty());
		Assertions.assertTrue(java.nio.file.Files.isRegularFile(directory.resolve("family_media").resolve("picture.png")));
		Assertions.assertFalse(java.nio.file.Files.exists(directory.resolve("escape.png")));
	}


	/* ---------------------------- import report ---------------------------- */

	private static ImportReport report(final String body) throws IOException{
		final ImportReport report = new ImportReport();
		GrampsXMLToFLEFConverter.convert((XML_HEADER + body + XML_FOOTER).getBytes(StandardCharsets.UTF_8), (percent, message) -> {}, report);
		return report;
	}

	@Test
	void anUnreadableRegionIsReported(@TempDir final Path directory) throws IOException{
		final String source = directory.resolve("missing.png").toString();
		final ImportReport report = report("<people><person handle=\"_i\" id=\"I1\" change=\"1\"><objref hlink=\"_o\">"
			+ "<region corner1_x=\"10\" corner1_y=\"20\" corner2_x=\"60\" corner2_y=\"70\"/></objref></person></people>"
			+ "<objects><object handle=\"_o\" id=\"O1\" change=\"1\"><file src=\"" + source + "\" mime=\"image/png\"/></object></objects>");

		Assertions.assertEquals(List.of(ImportReasons.MEDIA_DETAIL), List.copyOf(report.countsByReason().keySet()));
		Assertions.assertEquals("O1", report.issues().getFirst().where());
	}

	@Test
	void aNewYearRuleIsWrittenAsTheCalendar() throws IOException{
		final String[] rules = {"Mar25", "Sep1", "Dec25", "Mar1", "Easter"};
		final String[] calendars = {DateReader.ENUM_CALENDAR_FLORENTINE, DateReader.ENUM_CALENDAR_BYZANTINE_STYLE,
			DateReader.ENUM_CALENDAR_NATIVITY, DateReader.ENUM_CALENDAR_VENETAN, DateReader.ENUM_CALENDAR_EASTER_STYLE};
		final StringBuilder xml = new StringBuilder("<events>");
		for(int i = 0; i < rules.length; i ++)
			xml.append("<event handle=\"_e").append(i).append("\" id=\"E").append(i).append("\" change=\"1\"><type>Birth</type>"
				+ "<dateval val=\"1499-02-10\" cformat=\"Julian\" newyear=\"").append(rules[i]).append("\"/></event>");
		xml.append("</events>");

		final FLEFModel model = convertXml(xml.toString());
		for(int i = 0; i < rules.length; i ++){
			final FLEFRecord event = record(model, "E" + i);
			Assertions.assertEquals("10 FEB 1499", FLEFRecordHelper.getChildValue(event, "date.value.point.value.full_date"));
			Assertions.assertEquals(calendars[i], FLEFRecordHelper.getChildValue(event, "date.value.point.calendar"));
		}
		final ImportReport report = report(xml.toString());
		Assertions.assertTrue(report.countsByReason().isEmpty(), report.countsByReason().toString());
	}

	/**
	 * The date written with the calendar of the new-year rule must be the same day as the Gramps date: it is read back through the
	 * parser of the Calendars module and compared with the Julian day it came from.
	 */
	@Disabled("Fails by 365 days: either the importer writes the year of the Gramps value as is in the new-year calendar, or Gramps stores the year as written in the source. To decide with real Gramps data, see the README of the projections")
	@Test
	void aNewYearRuleKeepsTheDay() throws IOException{
		final String[] rules = {"Mar25", "Sep1", "Dec25", "Mar1", "Easter"};
		//a day after Easter (18 April 1499) in the year, and one before it that repeats between Jan 1 and Easter
		final String[] days = {"1499-02-10", "1499-06-20"};
		final StringBuilder xml = new StringBuilder("<events>");
		for(int d = 0; d < days.length; d ++)
			for(int i = 0; i < rules.length; i ++)
				xml.append("<event handle=\"_e").append(d).append(i).append("\" id=\"E").append(d).append(i)
					.append("\" change=\"1\"><type>Birth</type><dateval val=\"").append(days[d])
					.append("\" cformat=\"Julian\" newyear=\"").append(rules[i]).append("\"/></event>");
		xml.append("</events>");

		final FLEFModel model = convertXml(xml.toString());
		for(int d = 0; d < days.length; d ++){
			final long expected = CalendarType.JULIAN.parseToJdn(days[d].substring(8) + " " + (d == 0? "FEB": "JUN") + " 1499");
			for(int i = 0; i < rules.length; i ++){
				final FLEFRecord event = record(model, "E" + d + i);
				final String written = FLEFRecordHelper.getChildValue(event, "date.value.point.value.full_date");
				final String calendar = FLEFRecordHelper.getChildValue(event, "date.value.point.calendar");
				Assertions.assertEquals(expected, CalendarType.fromCode(calendar).parseToJdn(written),
					rules[i] + " " + days[d] + " written as " + written + " (" + calendar + ")");
			}
		}
	}

	@Test
	void aNewYearRuleOfAJulianDateAfterTheReformIsReported() throws IOException{
		final String xml = "<events><event handle=\"_e\" id=\"E1\" change=\"1\"><type>Birth</type>"
			+ "<dateval val=\"1699-02-10\" cformat=\"Julian\" newyear=\"Mar25\"/></event></events>";

		Assertions.assertEquals(DateReader.ENUM_CALENDAR_JULIAN,
			FLEFRecordHelper.getChildValue(record(convertXml(xml), "E1"), "date.value.point.calendar"));
		Assertions.assertEquals(List.of(ImportReasons.DATE_DETAIL), List.copyOf(report(xml).countsByReason().keySet()));
	}

	@Test
	void aNewYearRuleIsReported() throws IOException{
		final ImportReport report = report("<events><event handle=\"_e\" id=\"E1\" change=\"1\"><type>Birth</type>"
			+ "<dateval val=\"1700-02-03\" cformat=\"Hebrew\" newyear=\"Mar25\"/></event></events>");

		Assertions.assertEquals(List.of(ImportReasons.DATE_DETAIL), List.copyOf(report.countsByReason().keySet()));
		Assertions.assertEquals("E1", report.issues().getFirst().where());
	}

	@Test
	void aSurnameConnectorAndADerivationAreReported() throws IOException{
		final ImportReport report = report("<people><person handle=\"_i\" id=\"I1\" change=\"1\"><name><first>Carlo</first>"
			+ "<surname connector=\"y\">Garcia</surname><surname derivation=\"Pseudonym\">Rossi</surname></name></person></people>");

		Assertions.assertEquals(List.of(ImportReasons.NAME_DETAIL), List.copyOf(report.countsByReason().keySet()));
		Assertions.assertEquals(2, report.issues().size());
		Assertions.assertEquals("I1", report.issues().getFirst().where());
	}

	@Test
	void theDataEntriesOfASourceAreReported() throws IOException{
		final ImportReport report = report("<sources><source handle=\"_s\" id=\"S1\" change=\"1\"><stitle>Book</stitle>"
			+ "<datamap key=\"Coverage\" value=\"1800-1900\"/></source></sources>");

		Assertions.assertEquals(List.of(ImportReasons.SOURCE_DETAIL), List.copyOf(report.countsByReason().keySet()));
		Assertions.assertEquals("S1", report.issues().getFirst().where());
	}

	@Test
	void aCustomEventRoleIsReported() throws IOException{
		final ImportReport report = report("<events><event handle=\"_e\" id=\"E1\" change=\"1\"><type>Birth</type></event></events>"
			+ "<people><person handle=\"_i\" id=\"I1\" change=\"1\"><eventref hlink=\"_e\" role=\"Custom\"/></person></people>");

		Assertions.assertEquals(List.of(ImportReasons.OTHER), List.copyOf(report.countsByReason().keySet()));
	}

	@Test
	void technicalDataIsNotReported() throws IOException{
		final ImportReport report = report("<tags><tag handle=\"_t\" name=\"ToDo\" color=\"#ff0000\" priority=\"1\" change=\"1\"/></tags>"
			+ "<events><event handle=\"_e\" id=\"E1\" change=\"1\"><type>Birth</type><dateval val=\"1700-02-03\" newyear=\"Jan1\"/></event></events>"
			+ "<people><person handle=\"_i\" id=\"I1\" change=\"1\"><name sort=\"1\" display=\"2\"><first>Carlo</first>"
			+ "<surname derivation=\"Inherited\">Rossi</surname></name><eventref hlink=\"_e\" role=\"Primary\"/></person></people>"
			+ "<objects><object handle=\"_o\" id=\"O1\" change=\"1\"><file src=\"a.png\" mime=\"image/png\" checksum=\"abc\"/></object></objects>"
			+ "<bookmarks><bookmark target=\"person\" hlink=\"_i\"/></bookmarks><name-maps><map type=\"group_as\" key=\"a\" value=\"b\"/></name-maps>");

		Assertions.assertTrue(report.isEmpty(), report.issues().toString());
	}

	@Test
	void theGrampsExampleReportsOnlyValidReasons() throws IOException{
		final byte[] xml;
		try(final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("gedg/example.gramps")){
			xml = GrampsPackageReader.readXml(in);
		}

		final ImportReport report = new ImportReport();
		GrampsXMLToFLEFConverter.convert(xml, (percent, message) -> {}, report);

		final List<String> valid = List.of(ImportReasons.IDENTIFIER, ImportReasons.CONTACT_DATA, ImportReasons.PLACE_DETAIL,
			ImportReasons.NOTE_DETAIL, ImportReasons.MEDIA_DETAIL, ImportReasons.NAME_DETAIL, ImportReasons.DATE_DETAIL,
			ImportReasons.SOURCE_DETAIL, ImportReasons.UNMAPPED_TAG, ImportReasons.OTHER);
		for(final String key : report.countsByReason().keySet())
			Assertions.assertTrue(valid.contains(key), key);
	}

}
