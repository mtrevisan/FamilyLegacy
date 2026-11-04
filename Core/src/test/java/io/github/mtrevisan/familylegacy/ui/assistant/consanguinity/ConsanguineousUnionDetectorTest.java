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
package io.github.mtrevisan.familylegacy.ui.assistant.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.AncestryGraph;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguineousUnionDetector;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReportText;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/** The couples whose partners are related, and the individuals whose parents are. */
class ConsanguineousUnionDetectorTest{

	//Xavier and Yvonne are first cousins (grandchildren of Gino and Gina), married, with a child (Zeno)
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Gino /Rossi/
		1 SEX M
		0 @I2@ INDI
		1 NAME Gina /Bianchi/
		1 SEX F
		0 @I3@ INDI
		1 NAME Aldo /Rossi/
		1 SEX M
		0 @I4@ INDI
		1 NAME Berta /Rossi/
		1 SEX F
		0 @I5@ INDI
		1 NAME Wanda /Verdi/
		1 SEX F
		0 @I6@ INDI
		1 NAME Walter /Neri/
		1 SEX M
		0 @I7@ INDI
		1 NAME Xavier /Rossi/
		1 SEX M
		0 @I8@ INDI
		1 NAME Yvonne /Neri/
		1 SEX F
		0 @I9@ INDI
		1 NAME Zeno /Rossi/
		1 SEX M
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 CHIL @I3@
		1 CHIL @I4@
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I5@
		1 CHIL @I7@
		0 @F3@ FAM
		1 HUSB @I6@
		1 WIFE @I4@
		1 CHIL @I8@
		0 @F4@ FAM
		1 HUSB @I7@
		1 WIFE @I8@
		1 CHIL @I9@
		1 MARR
		2 DATE 1 JAN 1900
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}


	@Test
	void theMarriageOfFirstCousinsIsReported() throws IOException{
		final ConsanguinityReport report = ConsanguineousUnionDetector.detect(convert());

		Assertions.assertEquals(1, report.unions().size());
		final ConsanguinityReport.ConsanguineousUnion union = report.unions().getFirst();
		Assertions.assertEquals(Set.of("Xavier", "Yvonne"), Set.of(StringUtils.substringBefore(union.firstName(), " "), StringUtils.substringBefore(union.secondName(), " ")));
		Assertions.assertEquals(0.0625, union.kinship(), 1e-12);
		Assertions.assertEquals(0.125, union.relationship(), 1e-12);
		Assertions.assertEquals("first cousins", union.term());
		Assertions.assertFalse(union.relationshipIds().isEmpty(), "the marriage is recorded");
		Assertions.assertEquals(1, union.childIds().size());
		Assertions.assertEquals(2, union.commonAncestors().size(), "the two grandparents");
	}

	@Test
	void theChildOfFirstCousinsIsInbred() throws IOException{
		final ConsanguinityReport report = ConsanguineousUnionDetector.detect(convert());

		Assertions.assertEquals(1, report.inbredIndividuals().size());
		final ConsanguinityReport.InbredIndividual zeno = report.inbredIndividuals().getFirst();
		Assertions.assertEquals(0.0625, zeno.inbreeding(), 1e-12);
		Assertions.assertTrue(zeno.name().contains("Zeno"), zeno.name());
		Assertions.assertEquals("first cousins", zeno.term());
	}

	@Test
	void theTextOfTheReportSaysWhoAndHowMuch() throws IOException{
		final FLEFModel model = convert();
		final ConsanguinityReport report = ConsanguineousUnionDetector.detect(model);

		final String text = ConsanguinityReportText.format(report, id -> ConsanguineousUnionDetector.nameOf(model, id));
		Assertions.assertTrue(text.contains("first cousins"), text);
		Assertions.assertTrue(text.contains("6.25%"), text);
		Assertions.assertTrue(text.contains("Zeno"), text);
	}

	@Test
	void aCoupleWithoutAUnionIsFoundThroughTheirChild(){
		//the cousins have a child and no union recorded
		final Map<String, List<String>> parents = new LinkedHashMap<>();
		parents.put("A", List.of("G1", "G2"));
		parents.put("B", List.of("G1", "G2"));
		parents.put("X", List.of("A", "WA"));
		parents.put("Y", List.of("B", "WB"));
		parents.put("Z", List.of("X", "Y"));
		final AncestryGraph graph = new AncestryGraph(parents);
		final ConsanguineousUnionDetector.Couple couple = new ConsanguineousUnionDetector.Couple("X", "Y", Set.of(), List.of("Z"));

		final ConsanguinityReport report = ConsanguineousUnionDetector.detect(graph, List.of(couple), id -> id,
			ConsanguineousUnionDetector.DEFAULT_MINIMUM_KINSHIP, ConsanguineousUnionDetector.DEFAULT_MINIMUM_INBREEDING);

		Assertions.assertEquals(1, report.unions().size());
		Assertions.assertTrue(report.unions().getFirst().relationshipIds().isEmpty());
	}

	@Test
	void theMinimumKinshipLeavesOutTheDistantRelatives(){
		//third cousins: kinship 1/256
		final Map<String, List<String>> parents = new LinkedHashMap<>();
		parents.put("A1", List.of("G1", "G2"));
		parents.put("A2", List.of("A1", "W1"));
		parents.put("A3", List.of("A2", "W2"));
		parents.put("X", List.of("A3", "W3"));
		parents.put("B1", List.of("G1", "G2"));
		parents.put("B2", List.of("B1", "V1"));
		parents.put("B3", List.of("B2", "V2"));
		parents.put("Y", List.of("B3", "V3"));
		final AncestryGraph graph = new AncestryGraph(parents);
		final ConsanguineousUnionDetector.Couple couple = new ConsanguineousUnionDetector.Couple("X", "Y", Set.of("R1"), List.of());

		final ConsanguinityReport third = ConsanguineousUnionDetector.detect(graph, List.of(couple), id -> id, 1. / 256., 1.);
		final ConsanguinityReport stricter = ConsanguineousUnionDetector.detect(graph, List.of(couple), id -> id, 1. / 128., 1.);

		Assertions.assertEquals(1, third.unions().size());
		Assertions.assertEquals(1. / 256., third.unions().getFirst().kinship(), 1e-12);
		Assertions.assertTrue(stricter.unions().isEmpty());
	}

	@Test
	void unrelatedPartnersAreNotReported(){
		final Map<String, List<String>> parents = new LinkedHashMap<>();
		parents.put("X", List.of("F1", "M1"));
		parents.put("Y", List.of("F2", "M2"));
		final ConsanguineousUnionDetector.Couple couple = new ConsanguineousUnionDetector.Couple("X", "Y", Set.of("R1"), List.of());

		final ConsanguinityReport report = ConsanguineousUnionDetector.detect(new AncestryGraph(parents), List.of(couple), id -> id,
			0., 0.);

		Assertions.assertTrue(report.isEmpty(), "a minimum of zero does not report the unrelated");
	}

	@Test
	void theClosestRelativesComeFirst(){
		final Map<String, List<String>> parents = new LinkedHashMap<>();
		parents.put("S1", List.of("F", "M"));
		parents.put("S2", List.of("F", "M"));
		parents.put("A", List.of("G1", "G2"));
		parents.put("B", List.of("G1", "G2"));
		parents.put("X", List.of("A", "WA"));
		parents.put("Y", List.of("B", "WB"));
		final List<ConsanguineousUnionDetector.Couple> couples = List.of(
			new ConsanguineousUnionDetector.Couple("X", "Y", Set.of("R1"), List.of()),
			new ConsanguineousUnionDetector.Couple("S1", "S2", Set.of("R2"), List.of()));

		final ConsanguinityReport report = ConsanguineousUnionDetector.detect(new AncestryGraph(parents), couples, id -> id,
			ConsanguineousUnionDetector.DEFAULT_MINIMUM_KINSHIP, ConsanguineousUnionDetector.DEFAULT_MINIMUM_INBREEDING);

		Assertions.assertEquals("siblings", report.unions().get(0).term());
		Assertions.assertEquals("first cousins", report.unions().get(1).term());
	}

}
