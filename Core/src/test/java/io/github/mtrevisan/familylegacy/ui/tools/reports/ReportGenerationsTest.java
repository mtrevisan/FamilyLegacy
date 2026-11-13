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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The ancestor and descendant reports follow as many generations as asked, the root not counted. */
class ReportGenerationsTest{

	/** I1 is the child of I2 and I3, I2 of I4 and I5, I4 of I6 and I7: four generations. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 FAMC @F1@
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 SEX M
		1 FAMS @F1@
		1 FAMC @F2@
		0 @I3@ INDI
		1 NAME Carla /Bianchi/
		1 SEX F
		1 FAMS @F1@
		0 @I4@ INDI
		1 NAME Dario /Rossi/
		1 SEX M
		1 FAMS @F2@
		1 FAMC @F3@
		0 @I5@ INDI
		1 NAME Elena /Verdi/
		1 SEX F
		1 FAMS @F2@
		0 @I6@ INDI
		1 NAME Franco /Rossi/
		1 SEX M
		1 FAMS @F3@
		0 @I7@ INDI
		1 NAME Giulia /Neri/
		1 SEX F
		1 FAMS @F3@
		0 @F1@ FAM
		1 HUSB @I2@
		1 WIFE @I3@
		1 CHIL @I1@
		0 @F2@ FAM
		1 HUSB @I4@
		1 WIFE @I5@
		1 CHIL @I2@
		0 @F3@ FAM
		1 HUSB @I6@
		1 WIFE @I7@
		1 CHIL @I4@
		""";

	private static FLEFModel model;


	@BeforeAll
	static void load() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		model = new GEDCOMToFLEFConverter().convert(roots);
	}


	/** @return	How many persons the report covers: the rows of its index of the individuals. */
	private static int persons(final ReportType type, final String root, final int generations){
		final ReportConfig config = ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)
			.with(ReportOption.GENERATIONS, generations)
			.with(ReportOption.INDEX_INDIVIDUALS, true);
		final ReportDocument document = ReportBuilder.build(type, model, model.getRecordById(root), config, ReportProgressListener.NOOP);

		boolean inIndex = false;
		for(final ReportSection section : document.sections()){
			if(section instanceof final ReportSection.Heading heading)
				inIndex = "Index of Individuals".equals(heading.text());
			else if(inIndex && section instanceof final ReportSection.Table table)
				return table.rows().size();
		}
		throw new AssertionError("no index of the individuals");
	}


	@Test
	void theAncestorsAreFollowedAsFarAsAsked(){
		Assertions.assertEquals(7, persons(ReportType.ANCESTOR, "I1", 0));
		Assertions.assertEquals(3, persons(ReportType.ANCESTOR, "I1", 1));
		Assertions.assertEquals(5, persons(ReportType.ANCESTOR, "I1", 2));
		Assertions.assertEquals(7, persons(ReportType.ANCESTOR, "I1", 3));
		Assertions.assertEquals(7, persons(ReportType.ANCESTOR, "I1", 20));
	}

	@Test
	void theDescendantsAreFollowedAsFarAsAsked(){
		//the line of I6 is I6, I4, I2, I1
		Assertions.assertEquals(4, persons(ReportType.DESCENDANT, "I6", 0));
		Assertions.assertEquals(2, persons(ReportType.DESCENDANT, "I6", 1));
		Assertions.assertEquals(3, persons(ReportType.DESCENDANT, "I6", 2));
		Assertions.assertEquals(4, persons(ReportType.DESCENDANT, "I6", 3));
	}

	@Test
	void theGenerationsOfAReportThatHasNoLinesAreIgnored(){
		final ReportConfig all = ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW).with(ReportOption.GENERATIONS, 0);
		final ReportConfig one = ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW).with(ReportOption.GENERATIONS, 1);

		Assertions.assertEquals(
			ReportBuilder.build(ReportType.INDIVIDUAL, model, model.getRecordById("I1"), all, ReportProgressListener.NOOP).sections().toString(),
			ReportBuilder.build(ReportType.INDIVIDUAL, model, model.getRecordById("I1"), one, ReportProgressListener.NOOP).sections().toString());
	}

}
