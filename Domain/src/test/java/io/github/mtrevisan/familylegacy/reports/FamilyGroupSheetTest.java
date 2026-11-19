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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportDateFormat;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The family group sheets (the persons are shown by their ids here, the application provides the names): a sheet for each partner of the individual, with the children of the couple, and one for the children with an unknown other parent. */
class FamilyGroupSheetTest{

	private static FLEFModel model;


	private static String individual(final String id, final String given, final String family){
		return "INDIVIDUAL " + id + " {\n\tNAME {\n\t\tPART {\n\t\t\tTYPE given\n\t\t\tVALUE " + given + "\n\t\t}\n\t\tPART {\n\t\t\tTYPE family\n\t\t\tVALUE "
			+ family + "\n\t\t}\n\t}\n}\n";
	}

	private static String relationship(final String id, final String subject, final String target, final String type){
		return "RELATIONSHIP " + id + " {\n\tSUBJECT.INDIVIDUAL " + subject + "\n\tTARGET.INDIVIDUAL " + target + "\n\tTYPE " + type + "\n}\n";
	}

	private static String event(final String id, final String type, final String date, final String place, final String... participants){
		final StringBuilder sb = new StringBuilder("EVENT " + id + " {\n\tTYPE " + type + "\n");
		if(date != null)
			sb.append("\tDATE.VALUE.POINT {\n\t\tVALUE.FULL_DATE ").append(date).append("\n\t\tCALENDAR gregorian\n\t}\n");
		if(place != null)
			sb.append("\tPLACE.ORIGINAL_TEXT ").append(place).append('\n');
		sb.append("}\n");
		int n = 0;
		for(final String participant : participants)
			sb.append("EVENT_PARTICIPATION EP").append(id).append('_').append(n ++).append(" {\n\tPARTICIPANT.INDIVIDUAL ").append(participant)
				.append("\n\tEVENT ").append(id).append("\n\tROLE principal\n}\n");
		return sb.toString();
	}

	@BeforeAll
	static void load(){
		model = new FLEFParser().parse(
			individual("I1", "Mario", "Rossi") + individual("I2", "Anna", "Bianchi") + individual("I3", "Luca", "Rossi")
				+ individual("I4", "Carla", "Rossi") + individual("I5", "Sara", "Verdi")
				+ relationship("RL1", "I1", "I2", "spouse")
				+ relationship("RL2", "I3", "I1", "biological_child") + relationship("RL3", "I3", "I2", "biological_child")
				+ relationship("RL4", "I4", "I1", "biological_child")
				+ event("E1", "birth", "1888-05-01", "Treviso", "I1")
				+ event("E2", "marriage", "1912-06-15", "Venezia", "I1", "I2")
				+ event("E3", "birth", "1915-03-04", "Padova", "I3"));
	}


	private static ReportConfig config(final ReportDateFormat format){
		return ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL)
			.with(ReportOption.DATE_FORMAT, format);
	}

	private static String text(final ReportDocument document){
		final StringBuilder sb = new StringBuilder(document.title());
		for(final ReportSection section : document.sections())
			sb.append('\n').append(section);
		return sb.toString();
	}

	private static List<String> headings(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(section -> ((ReportSection.Heading)section).text())
			.toList();
	}


	@Test
	void thereIsASheetForThePartnerAndOneForTheChildrenWithAnUnknownOtherParent(){
		final ReportDocument document = new FamilyGroupSheetGenerator(model, model.getRecordById("I1"), config(ReportDateFormat.AS_RECORDED)).generate();

		final List<String> headings = headings(document).stream()
			.filter(heading -> heading.startsWith("I1 – "))
			.toList();
		Assertions.assertEquals(2, headings.size(), headings.toString());
		Assertions.assertTrue(headings.getFirst().contains("I2"), headings.toString());
		Assertions.assertTrue(headings.getLast().contains("other parent unknown"), headings.toString());
	}

	@Test
	void theChildrenAreWithTheCoupleTheyAreOf(){
		final String text = text(new FamilyGroupSheetGenerator(model, model.getRecordById("I1"), config(ReportDateFormat.AS_RECORDED)).generate());

		Assertions.assertTrue(text.contains("I3") && text.contains("I4"), text);
		//I3 is a child of the couple, I4 of I1 alone: I4 comes after the sheet of the couple (the names are the ids: no display provider here)
		Assertions.assertTrue(text.indexOf("I3") < text.indexOf("I4"), text);
		Assertions.assertTrue(text.contains("1915-03-04") && text.contains("Padova"), text);
		Assertions.assertTrue(text.contains("1912-06-15, Venezia"), "the marriage: " + text);
	}

	@Test
	void theDatesAreWrittenAsTheUserChose(){
		final String dayMonthYear = text(new FamilyGroupSheetGenerator(model, model.getRecordById("I1"),
			config(ReportDateFormat.DAY_MONTH_YEAR)).generate());
		final String monthDayYear = text(new FamilyGroupSheetGenerator(model, model.getRecordById("I1"),
			config(ReportDateFormat.MONTH_DAY_YEAR)).generate());
		final String yearOnly = text(new FamilyGroupSheetGenerator(model, model.getRecordById("I1"), config(ReportDateFormat.YEAR_ONLY)).generate());

		Assertions.assertTrue(dayMonthYear.contains("1 May 1888"), dayMonthYear);
		Assertions.assertTrue(monthDayYear.contains("May 1, 1888"), monthDayYear);
		Assertions.assertTrue(yearOnly.contains("1888") && !yearOnly.contains("1888-05-01"), yearOnly);
	}

	@Test
	void anIndividualWithoutFamilyHasASheetWithNoChildren(){
		final String text = text(new FamilyGroupSheetGenerator(model, model.getRecordById("I5"), config(ReportDateFormat.AS_RECORDED)).generate());

		Assertions.assertTrue(text.contains("No children are known."), text);
	}

}
