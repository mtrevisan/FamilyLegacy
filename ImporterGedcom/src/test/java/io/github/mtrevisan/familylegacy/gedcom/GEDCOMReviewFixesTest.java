package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.importer.ImportIssue;
import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMDateConverter;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.PlaceCache;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** Defects found reviewing the converters. */
class GEDCOMReviewFixesTest{

	private record Result(FLEFModel model, ImportReport report){}

	private static Result convertWhole(final String gedcom) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		final ImportReport report = new ImportReport();
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, report);
		return new Result(model, report);
	}

	private static Result convert(final String body) throws IOException{
		return convertWhole("0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n");
	}

	private static boolean reported(final ImportReport report, final String itemStart){
		for(final ImportIssue issue : report.issues())
			if(ImportReasons.OTHER.equals(issue.reasonKey()) && issue.item().startsWith(itemStart))
				return true;
		return false;
	}

	private static List<FLEFRecord> residences(final FLEFModel model){
		return model.getRecordsByType("INDIVIDUAL_ATTRIBUTE").stream()
			.filter(attribute -> "residence".equals(FLEFRecordHelper.getChildValue(attribute, "type")))
			.toList();
	}

	@Test
	void nameWithoutTheClosingSlashKeepsItsSurname() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME John /Smith
			""").model();

		final FLEFRecord name = FLEFRecordHelper.findChild(model.getRecordById("I1"), "name");
		final List<String> parts = FLEFRecordHelper.findChildren(name, "part").stream()
			.map(part -> FLEFRecordHelper.getChildValue(part, "type") + "=" + FLEFRecordHelper.getChildValue(part, "value"))
			.toList();
		Assertions.assertEquals(List.of("given=John", "family=Smith"), parts);
	}

	@Test
	void addressContainedInThePlaceKeepsTheWholePlace() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME John /Smith/
			1 RESI
			2 ADDR Via Roma 5
			2 PLAC Via Roma 5, Treviso, Italia
			""").model();

		final List<String> names = model.getRecordsByType("PLACE").stream()
			.map(place -> FLEFRecordHelper.getChildValue(FLEFRecordHelper.findChild(place, "name"), "value"))
			.toList();
		Assertions.assertTrue(names.contains("Treviso"), names.toString());
		Assertions.assertTrue(names.contains("Italia"), names.toString());
	}

	@Test
	void twoCensusesOfTheSameDateInDifferentPlacesGiveTwoResidences() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 CENS Y
			2 DATE 4 APR 1940
			2 PLAC Passaic, New Jersey, United States of America
			1 CENS Y
			2 DATE 4 APR 1940
			2 PLAC Clifton, New Jersey, United States of America
			""").model();

		Assertions.assertEquals(2, residences(model).size());
	}

	@Test
	void blankAdditionalFileDoesNotLeaveANullChild() throws IOException{
		final FLEFModel model = convert("""
			0 @M1@ OBJE
			1 FILE a.jpg
			1 FILE
			""").model();

		for(final FLEFRecord child : model.getRecordById("M1").getChildren())
			Assertions.assertNotNull(child);
	}

	@Test
	void danglingAssociationIsReported() throws IOException{
		final Result result = convert("""
			0 @I1@ INDI
			1 NAME John /Smith/
			1 ASSO @I99@
			2 RELA Godfather
			""");

		Assertions.assertTrue(reported(result.report(), "ASSO"), result.report().issues().toString());
	}

	@Test
	void recordsWithoutXrefAreReported() throws IOException{
		final Result result = convert("""
			0 FAM
			1 HUSB @I1@
			0 SOUR
			1 TITL Book
			0 REPO
			1 NAME Archive
			0 OBJE
			1 FILE a.jpg
			""");

		for(final String tag : List.of("FAM", "SOUR", "REPO", "OBJE"))
			Assertions.assertTrue(reported(result.report(), tag), tag + StringUtils.SPACE + result.report().issues());
	}

	@Test
	void fileWithoutHeaderGetsOne() throws IOException{
		final FLEFModel model = convertWhole("0 @I1@ INDI\n1 NAME John /Smith/\n0 TRLR\n").model();

		Assertions.assertTrue(model.getHeader().hasData());
		Assertions.assertNotNull(FLEFRecordHelper.findChild(model.getHeader(), "protocol"));
	}

	@Test
	void doubleDatedYearIsJulian() {
		final FLEFRecord date = GEDCOMDateConverter.buildDateStructure("date", "1 FEB 1750/51");

		Assertions.assertEquals("julian", FLEFRecordHelper.getChildValue(date, "value.point.calendar"));
	}

	@Test
	void timeWithASingleDigitHourIsRead() {
		final GEDCOMNode date = new GEDCOMNode(2, "DATE", "1 JAN 2000");
		date.addChild(new GEDCOMNode(3, "TIME", "9:05"));

		Assertions.assertEquals("2000-01-01T09:05:00Z", GEDCOMDateConverter.toDateTime(date));
	}

	@Test
	void coordinatesOutOfRangeAreRejected() {
		Assertions.assertNull(PlaceCache.toISO6709("N95", "E10"));
		Assertions.assertNull(PlaceCache.toISO6709("N45", "E190"));
		Assertions.assertEquals("+45.5+010/", PlaceCache.toISO6709("N45.5", "E10"));
	}

	@Test
	void mapperTablesCannotBeChanged() {
		Assertions.assertThrows(UnsupportedOperationException.class, () -> GEDCOMMapper.EVENT_MAP.put("X", "y"));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> GEDCOMMapper.ATTRIBUTE_MAP.put("X", "y"));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> GEDCOMMapper.ROLE_MAP.put("X", "y"));
	}

}
