package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.importer.gramps.GrampsPackageReader;
import io.github.mtrevisan.familylegacy.importer.gramps.GrampsXMLToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
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
		Assertions.assertEquals(3, model.getRecordsByType("event").size());
		final List<String> roles = model.getRecordsByType("event_participation").stream()
			.filter(participation -> "E0001".equals(FLEFRecordHelper.getChildValue(participation, "event")))
			.map(participation -> FLEFRecordHelper.getChildValue(participation, "role"))
			.sorted()
			.toList();
		//the birth has the person it is about, and the witness
		Assertions.assertEquals(List.of("principal", "witness"), roles);

		final FLEFRecord birth = record(model, "E0001");
		Assertions.assertEquals("birth", FLEFRecordHelper.getChildValue(birth, "type"));
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
	void anOccupationIsAnAttributeWhoseValidityIsTheRange() throws IOException{
		final FLEFModel model = convert();

		final FLEFRecord occupation = model.getRecordsByType("individual_attribute").getFirst();
		Assertions.assertEquals("occupation", FLEFRecordHelper.getChildValue(occupation, "type"));
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
		Assertions.assertEquals("street", FLEFRecordHelper.getChildValue(street, "type"));
		Assertions.assertEquals("+45.6505+012.2133/", FLEFRecordHelper.getChildValue(street, "location.coordinates"));
		Assertions.assertEquals(2, model.getRecordsByType("place_relationship").size());
		Assertions.assertTrue(model.getRecordsByType("place_relationship").stream()
			.anyMatch(relationship -> "P0003".equals(FLEFRecordHelper.getChildValue(relationship, "subject.place"))
				&& "P0002".equals(FLEFRecordHelper.getChildValue(relationship, "target.place"))));
	}

	@Test
	void theNamesAndTheSurnamePrefixAreKept() throws IOException{
		final FLEFModel model = convert();

		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(record(model, "I0001"), "name");
		Assertions.assertEquals(2, names.size());
		Assertions.assertEquals("birth", FLEFRecordHelper.getChildValue(names.getFirst(), "type"));
		Assertions.assertEquals("alias", FLEFRecordHelper.getChildValue(names.getLast(), "type"));
		Assertions.assertEquals("de Rossi", FLEFRecordHelper.findChildren(names.getFirst(), "part").stream()
			.filter(part -> "family".equals(FLEFRecordHelper.getChildValue(part, "type")))
			.map(part -> FLEFRecordHelper.getChildValue(part, "value"))
			.findFirst().orElseThrow());
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
			.filter(relationship -> !"group_member".equals(FLEFRecordHelper.getChildValue(relationship, "type")))
			.toList();
		Assertions.assertEquals(2, parentChild.size());
		Assertions.assertTrue(parentChild.stream().allMatch(relationship ->
			"adoptive_child".equals(FLEFRecordHelper.getChildValue(relationship, "type"))));
		Assertions.assertTrue(model.getRecordsByType("relationship").stream()
			.anyMatch(relationship -> "spouse".equals(FLEFRecordHelper.getChildValue(relationship, "type"))
				&& "1875".equals(FLEFRecordHelper.getChildValue(relationship, "valid_from.value.point.value.full_date"))));
	}

	@Test
	void thePrivacyIsKept() throws IOException{
		Assertions.assertEquals("restricted", FLEFRecordHelper.getChildValue(record(convert(), "I0002"), "privacy.level"));
	}

	@Test
	void theDocumentConvertsToAValidModel() throws IOException, URISyntaxException{
		final FLEFModel model = convert();

		final Path grammarPath = Path.of(GrampsXMLToFLEFConverterTest.class.getResource("/gedg/flef_0.2.0.gedg").toURI());
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
		try(final InputStream in = GrampsXMLToFLEFConverterTest.class.getResourceAsStream("/gedg/example.gramps")){
			xml = GrampsPackageReader.readXml(in);
		}

		final FLEFModel model = GrampsXMLToFLEFConverter.convert(xml, (percent, message) -> {});

		Assertions.assertEquals(2157, model.getRecordsByType("individual").size());
		final Path grammarPath = Path.of(GrampsXMLToFLEFConverterTest.class.getResource("/gedg/flef_0.2.0.gedg").toURI());
		final FLEFValidator validator = new FLEFValidator(FLEFGrammarParser.parse(grammarPath));
		Assertions.assertEquals(List.of(), validator.validateSchema(model));
		Assertions.assertEquals(List.of(), validator.validateIntegrity(model));
	}

}
