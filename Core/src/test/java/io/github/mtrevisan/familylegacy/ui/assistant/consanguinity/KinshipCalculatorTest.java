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

import io.github.mtrevisan.familylegacy.consanguinity.KinshipCalculator;
import io.github.mtrevisan.familylegacy.consanguinity.KinshipResult;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The coefficient of relationship of the kinship calculator is the exact one, with the inbreeding of the ancestors. */
class KinshipCalculatorTest{

	private static final double EPSILON = 1e-9;

	//Aldo and Berta are siblings (children of Franco and Maria); Carlo is the child of Aldo and Berta, so he is inbred (F = 1/4);
	//Dino is the child of Elio, another child of Franco and Maria
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Franco /Rossi/
		1 SEX M
		0 @I2@ INDI
		1 NAME Maria /Bianchi/
		1 SEX F
		0 @I3@ INDI
		1 NAME Aldo /Rossi/
		1 SEX M
		0 @I4@ INDI
		1 NAME Berta /Rossi/
		1 SEX F
		0 @I5@ INDI
		1 NAME Carlo /Rossi/
		1 SEX M
		0 @I6@ INDI
		1 NAME Elio /Rossi/
		1 SEX M
		0 @I7@ INDI
		1 NAME Wanda /Verdi/
		1 SEX F
		0 @I8@ INDI
		1 NAME Dino /Rossi/
		1 SEX M
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 CHIL @I3@
		1 CHIL @I4@
		1 CHIL @I6@
		0 @F2@ FAM
		1 HUSB @I3@
		1 WIFE @I4@
		1 CHIL @I5@
		0 @F3@ FAM
		1 HUSB @I6@
		1 WIFE @I7@
		1 CHIL @I8@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static String idOf(final FLEFModel model, final String firstName){
		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualReader.TYPE);
		for(final FLEFRecord individual : individuals)
			if(IndividualReader.extractPrimaryFullname(individual).startsWith(firstName))
				return individual.getId();
		throw new IllegalArgumentException(firstName);
	}


	@Test
	void theRelationshipIsDescribedWithTheNamesAndTheTermsOfTheSex() throws IOException{
		final FLEFModel model = convert();
		final KinshipCalculator calculator = KinshipCalculator.forModel(model);
		final String franco = idOf(model, "Franco");
		final String aldo = idOf(model, "Aldo");
		final String berta = idOf(model, "Berta");
		final String elio = idOf(model, "Elio");
		final String dino = idOf(model, "Dino");

		final String father = calculator.calculate(franco, aldo).relationshipDescription();
		Assertions.assertTrue(father.contains("Franco") && father.contains(" is the father of ") && father.contains("Aldo"), father);
		final String brothers = calculator.calculate(aldo, elio).relationshipDescription();
		Assertions.assertTrue(brothers.contains(" are full brothers"), brothers);
		final String uncle = calculator.calculate(aldo, dino).relationshipDescription();
		Assertions.assertTrue(uncle.contains("Aldo") && uncle.contains(" is the uncle of ") && uncle.contains("Dino"), uncle);
		final String aunt = calculator.calculate(dino, berta).relationshipDescription();
		Assertions.assertTrue(aunt.contains("Berta") && aunt.contains(" is the aunt of ") && aunt.contains("Dino"), aunt);

		Assertions.assertEquals("uncle", calculator.shortTerm(aldo, dino));
		Assertions.assertEquals("nephew", calculator.shortTerm(dino, aldo));
		Assertions.assertEquals("brother", calculator.shortTerm(aldo, elio));
		Assertions.assertEquals("unrelated", calculator.shortTerm(aldo, null));
	}

	/** What the reports ask: the terms in the language of the report, not in the one of the interface. */
	private static KinshipCalculator inLanguageOf(final FLEFModel model, final ReportLanguage language){
		return KinshipCalculator.forModel(model)
			.withVocabulary(new ReportLabels(language).kinship());
	}

	@Test
	void theTermsOfAReportAreInTheLanguageOfTheReport() throws IOException{
		final FLEFModel model = convert();
		final String aldo = idOf(model, "Aldo");
		final String berta = idOf(model, "Berta");
		final String elio = idOf(model, "Elio");
		final String dino = idOf(model, "Dino");
		final String carlo = idOf(model, "Carlo");

		final KinshipCalculator italian = inLanguageOf(model, ReportLanguage.ITALIAN);
		Assertions.assertEquals("zio", italian.shortTerm(aldo, dino));
		Assertions.assertEquals("zia", italian.shortTerm(berta, dino));
		Assertions.assertEquals("nipote", italian.shortTerm(dino, aldo));
		Assertions.assertEquals("fratello", italian.shortTerm(aldo, elio));
		Assertions.assertEquals("cugino di primo grado", italian.shortTerm(dino, carlo));

		Assertions.assertEquals("Onkel", inLanguageOf(model, ReportLanguage.GERMAN).shortTerm(aldo, dino));
		Assertions.assertEquals("oncle", inLanguageOf(model, ReportLanguage.FRENCH).shortTerm(aldo, dino));
		Assertions.assertEquals("t\u00edo", inLanguageOf(model, ReportLanguage.SPANISH).shortTerm(aldo, dino));
		Assertions.assertEquals("tio", inLanguageOf(model, ReportLanguage.PORTUGUESE).shortTerm(aldo, dino));

		//a language with no words of its own says them in English, as the rest of the report does
		Assertions.assertEquals("uncle", inLanguageOf(model, ReportLanguage.JAPANESE).shortTerm(aldo, dino));
		//and the calculator of the interface is not changed by the report
		Assertions.assertEquals("uncle", KinshipCalculator.forModel(model).shortTerm(aldo, dino));
	}

	@Test
	void siblingsHaveOneHalf() throws IOException{
		final FLEFModel model = convert();
		final KinshipCalculator calculator = KinshipCalculator.forModel(model);

		final KinshipResult result = calculator.calculate(idOf(model, "Aldo"), idOf(model, "Elio"));

		Assertions.assertEquals(0.5, result.relationshipCoefficient(), EPSILON);
	}

	@Test
	void firstCousinsHaveOneEighth() throws IOException{
		final FLEFModel model = convert();
		final KinshipCalculator calculator = KinshipCalculator.forModel(model);

		final KinshipResult result = calculator.calculate(idOf(model, "Carlo"), idOf(model, "Dino"));

		//Carlo is inbred (F = 1/4): the exact value is not the sum of (1/2)^(dA+dB) over the paths
		final double expected = 0.25 / Math.sqrt(1.25);
		Assertions.assertEquals(expected, result.relationshipCoefficient(), EPSILON);
	}

	@Test
	void theInbreedingOfAnAncestorIsCounted() throws IOException{
		final FLEFModel model = convert();
		final KinshipCalculator calculator = KinshipCalculator.forModel(model);

		//the sum of (1/2)^(dA+dB) over the common ancestors would give 1/2 + 1/8 + 1/8 = 0.75
		final KinshipResult result = calculator.calculate(idOf(model, "Aldo"), idOf(model, "Carlo"));

		Assertions.assertEquals(0.75 / Math.sqrt(1.25), result.relationshipCoefficient(), EPSILON);
	}

}
