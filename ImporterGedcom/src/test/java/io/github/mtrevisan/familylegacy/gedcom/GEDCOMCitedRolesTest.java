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

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The roles stated by the citations (SOUR.EVEN.ROLE): the synonyms are mapped, the kinship is not reported. */
class GEDCOMCitedRolesTest{

	private static ImportReport convert(final String body) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n"));
		final ImportReport report = new ImportReport();
		new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, report);
		return report;
	}

	private static boolean reported(final ImportReport report, final String role){
		return report.issues().stream()
			.anyMatch(issue -> ImportReasons.SOURCE_DETAIL.equals(issue.reasonKey()) && issue.item().contains(role));
	}


	@Test
	void theObviousSynonymsAreMapped(){
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_GRANTOR, GEDCOMMapper.mapEventRole("seller"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_GRANTEE, GEDCOMMapper.mapEventRole("Buyer"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_LANDLORD, GEDCOMMapper.mapEventRole("lessor"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_TENANT, GEDCOMMapper.mapEventRole("lessee"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_SPONSOR, GEDCOMMapper.mapEventRole("godfather"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_OFFICIANT, GEDCOMMapper.mapEventRole("priest"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_MIDWIFE, GEDCOMMapper.mapEventRole("midwife"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_POWER_OF_ATTORNEY, GEDCOMMapper.mapEventRole("Power of Attorney"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_POWER_OF_ATTORNEY, GEDCOMMapper.mapEventRole("power-of-attorney"));
		Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_GRANTOR, GEDCOMMapper.mapEventRole("venditore"));
	}

	@Test
	void aKinshipIsNotARole(){
		Assertions.assertNull(GEDCOMMapper.mapEventRole("spouse"));
		Assertions.assertTrue(GEDCOMMapper.isKinshipRole("Spouse"));
		Assertions.assertTrue(GEDCOMMapper.isKinshipRole("HUSB"));
		Assertions.assertTrue(GEDCOMMapper.isKinshipRole("father-in-law"));
		Assertions.assertTrue(GEDCOMMapper.isKinshipRole("Father"));
		Assertions.assertTrue(GEDCOMMapper.isKinshipRole("mother"));
		Assertions.assertTrue(GEDCOMMapper.isKinshipRole("FATH"));
		Assertions.assertFalse(GEDCOMMapper.isKinshipRole("seller"));
		Assertions.assertFalse(GEDCOMMapper.isKinshipRole(null));
	}

	@Test
	void aKinshipRoleOfACitationIsNotReported() throws IOException{
		final ImportReport report = convert("""
			0 @S1@ SOUR
			1 TITL Marriage register
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 1 JAN 1850
			2 SOUR @S1@
			3 EVEN MARR
			4 ROLE spouse
			""");

		Assertions.assertFalse(reported(report, "spouse"), report.issues().toString());
	}

	@Test
	void fatherAndMotherAreNotReportedEither() throws IOException{
		final ImportReport report = convert("""
			0 @S1@ SOUR
			1 TITL Marriage register
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 SOUR @S1@
			3 EVEN MARR
			4 ROLE father
			1 DEAT
			2 SOUR @S1@
			3 EVEN MARR
			4 ROLE mother
			1 SOUR @S1@
			2 EVEN MARR
			3 ROLE FATH
			""");

		Assertions.assertTrue(report.issues().stream().noneMatch(issue -> ImportReasons.SOURCE_DETAIL.equals(issue.reasonKey())),
			report.issues().toString());
	}

	@Test
	void aRoleThatIsNotUnderstoodStillIsReported() throws IOException{
		final ImportReport report = convert("""
			0 @S1@ SOUR
			1 TITL Deed
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 DATE 1 JAN 1850
			2 SOUR @S1@
			3 EVEN DEED
			4 ROLE chief of the village
			""");

		Assertions.assertTrue(reported(report, "chief of the village"), report.issues().toString());
	}

}
