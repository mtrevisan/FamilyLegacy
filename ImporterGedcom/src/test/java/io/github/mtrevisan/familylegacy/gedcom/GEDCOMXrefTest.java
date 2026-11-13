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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The cross-references that are not of the form letters+digits (or that start with F) still link the records. */
class GEDCOMXrefTest{

	private static FLEFModel convert(final String body) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static List<FLEFRecord> relationships(final FLEFModel model, final String type){
		return model.getRecordsByType("RELATIONSHIP").stream()
			.filter(relationship -> type.equals(FLEFRecordHelper.getChildValue(relationship, "type")))
			.toList();
	}

	private static void assertFamilyIsLinked(final FLEFModel model){
		Assertions.assertEquals(3, model.getRecordsByType("INDIVIDUAL").size());
		Assertions.assertEquals(1, model.getRecordsByType("GROUP").size());
		//husband, wife and child belong to the group
		final List<FLEFRecord> members = relationships(model, "group_member");
		Assertions.assertEquals(3, members.size(), members.toString());
		for(final FLEFRecord member : members){
			final String individualId = FLEFRecordHelper.getChildValue(FLEFRecordHelper.findChild(member, "subject"), "individual");
			final String groupId = FLEFRecordHelper.getChildValue(FLEFRecordHelper.findChild(member, "target"), "group");
			Assertions.assertTrue(model.hasRecord(individualId), individualId);
			Assertions.assertTrue(model.hasRecord(groupId), groupId);
		}
		//the spouse link and the two parent-child links, besides the memberships
		Assertions.assertTrue(model.getRecordsByType("RELATIONSHIP").size() >= 3 + 3, model.getRecordsByType("RELATIONSHIP").toString());
	}

	@Test
	void numericAndMixedCaseIndividualReferencesLink() throws IOException{
		final FLEFModel model = convert("""
			0 @1@ INDI
			1 NAME John /Doe/
			1 SEX M
			1 FAMS @F1@
			0 @Ind2@ INDI
			1 NAME Jane /Roe/
			1 SEX F
			1 FAMS @F1@
			0 @i_3@ INDI
			1 NAME Baby /Doe/
			1 FAMC @F1@
			0 @F1@ FAM
			1 HUSB @1@
			1 WIFE @Ind2@
			1 CHIL @i_3@
			""");

		assertFamilyIsLinked(model);
	}

	@Test
	void familyReferencesStartingWithFLink() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME John /Doe/
			1 FAMS @FAM1@
			0 @I2@ INDI
			1 NAME Jane /Roe/
			1 FAMS @FAM1@
			0 @I3@ INDI
			1 NAME Baby /Doe/
			1 FAMC @FAM1@
			0 @FAM1@ FAM
			1 HUSB @I1@
			1 WIFE @I2@
			1 CHIL @I3@
			""");

		assertFamilyIsLinked(model);
	}

	@Test
	void familyReferenceWithLettersAfterTheNumberLinks() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME John /Doe/
			1 FAMS @F1a@
			0 @I2@ INDI
			1 NAME Jane /Roe/
			1 FAMS @F1a@
			0 @I3@ INDI
			1 NAME Baby /Doe/
			1 FAMC @F1a@
			0 @F1a@ FAM
			1 HUSB @I1@
			1 WIFE @I2@
			1 CHIL @I3@
			""");

		assertFamilyIsLinked(model);
	}

	@Test
	void generatedIdsDoNotCollideWithExistingOnes() throws IOException{
		//"@1@" is rewritten, and "I1" is taken
		final FLEFModel model = convert("""
			0 @1@ INDI
			1 NAME John /Doe/
			0 @I1@ INDI
			1 NAME Jane /Roe/
			""");

		Assertions.assertEquals(2, model.getRecordsByType("INDIVIDUAL").size());
	}

	@Test
	void individualOfAnIndividualFamilyKeepsHisStandardId() throws IOException{
		final FLEFModel model = convert("""
			0 @I7@ INDI
			1 NAME John /Doe/
			""");

		Assertions.assertTrue(model.hasRecord("I7"));
	}

	@Test
	void stepChildrenOfManyFamiliesAreFound() throws IOException{
		//A (I1) has a child (I3) with B (I2); then A marries C (I4): I3 is a step child of C
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME A /X/
			1 FAMS @F1@
			1 FAMS @F2@
			0 @I2@ INDI
			1 NAME B /X/
			1 FAMS @F1@
			0 @I3@ INDI
			1 NAME Child /X/
			1 FAMC @F1@
			0 @I4@ INDI
			1 NAME C /X/
			1 FAMS @F2@
			0 @F1@ FAM
			1 HUSB @I1@
			1 WIFE @I2@
			1 CHIL @I3@
			0 @F2@ FAM
			1 HUSB @I1@
			1 WIFE @I4@
			""");

		Assertions.assertEquals(1, relationships(model, "step_child").size());
	}

}
