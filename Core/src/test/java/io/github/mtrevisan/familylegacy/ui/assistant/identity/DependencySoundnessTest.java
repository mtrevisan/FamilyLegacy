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
package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.IdentitySettings;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.ui.assistant.consanguinity.ConsanguinityAssistantContributor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;


/**
 * An assistant that says which kinds of record its findings depend on is not asked again when the other kinds change, so a kind left out
 * by mistake would give stale findings. This checks it on a model that has every kind the import makes: the findings do not change when
 * all the records of a kind that is not declared are taken away.
 */
class DependencySoundnessTest{

	/** Two couples of cousins that marry, two records of the same man, with events, places, sources, repositories and notes. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @R1@ REPO
		1 NAME State Archive of Treviso
		0 @S1@ SOUR
		1 TITL Parish register of Treviso 1850
		1 REPO @R1@
		0 @N1@ NOTE A note about the family
		0 @I1@ INDI
		1 NAME Giovanni /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1800
		2 PLAC Treviso, Veneto, Italy
		0 @I2@ INDI
		1 NAME Maria /Bianchi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1805
		0 @I3@ INDI
		1 NAME Luigi /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1830
		1 FAMC @F1@
		0 @I4@ INDI
		1 NAME Anna /Rossi/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1832
		1 FAMC @F1@
		0 @I5@ INDI
		1 NAME Paolo /Verdi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1858
		2 PLAC Padova, Veneto, Italy
		1 FAMC @F2@
		0 @I6@ INDI
		1 NAME Paola /Neri/
		1 SEX F
		1 BIRT
		2 DATE 1 JAN 1860
		1 FAMC @F3@
		0 @I7@ INDI
		1 NAME Marco /Verdi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1885
		1 DEAT
		2 DATE 1 JAN 1950
		1 FAMC @F4@
		1 NOTE @N1@
		0 @I8@ INDI
		1 NAME Marco /Verdi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1885
		2 SOUR @S1@
		1 DEAT
		2 DATE 1 JAN 1950
		0 @F1@ FAM
		1 HUSB @I1@
		1 WIFE @I2@
		1 CHIL @I3@
		1 CHIL @I4@
		0 @F2@ FAM
		1 HUSB @I3@
		1 CHIL @I5@
		0 @F3@ FAM
		1 WIFE @I4@
		1 CHIL @I6@
		0 @F4@ FAM
		1 HUSB @I5@
		1 WIFE @I6@
		1 CHIL @I7@
		""";


	private static FLEFModel model() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	/** @return the model without the records of a kind (the other records are the same instances) */
	private static FLEFModel without(final FLEFModel model, final String kind){
		final FLEFModel copy = new FLEFModel();
		copy.setHeader(model.getHeader());
		for(final FLEFRecord record : model.getRecords())
			if(!kind.equalsIgnoreCase(record.getTag()))
				copy.addRecord(record);
		return copy;
	}

	/** @return what a finding says, without its actions (the actions are objects that are never equal) */
	private static List<String> summary(final List<Diagnostic> findings){
		return findings.stream()
			.map(d -> d.severity() + "|" + d.title() + "|" + d.explanation() + "|" + d.recordIds() + "|" + d.key())
			.sorted()
			.toList();
	}

	private static void assertSound(final DiagnosticProvider provider, final FLEFModel model){
		final Set<String> declared = provider.dependsOn();
		Assertions.assertNotNull(declared);
		final List<String> expected = summary(provider.diagnose(new AssistantContext(null, null, model)));
		Assertions.assertFalse(expected.isEmpty(), "the model must make the assistant find something, or the check proves nothing");

		final Set<String> kinds = new TreeSet<>();
		for(final FLEFRecord record : model.getRecords())
			kinds.add(record.getTag().toLowerCase(Locale.ROOT));
		kinds.removeIf(kind -> declared.stream().anyMatch(kind::equalsIgnoreCase));
		//a model with nothing that is not declared would prove nothing either
		Assertions.assertFalse(kinds.isEmpty());

		for(final String kind : kinds)
			Assertions.assertEquals(expected, summary(provider.diagnose(new AssistantContext(null, null, without(model, kind)))),
				"the findings changed without the records of kind " + kind + ", that the assistant does not declare");
	}


	@Test
	void theConsanguinityAssistantReadsOnlyTheKindsItDeclares() throws IOException{
		final DiagnosticProvider provider = new ConsanguinityAssistantContributor().diagnosticProviders().getFirst();

		assertSound(provider, model());
	}

	@Test
	void theIdentityAssistantReadsOnlyTheKindsItDeclaresForTheIndividuals() throws IOException{
		final DiagnosticProvider provider = new IdentityAssistantContributor(() -> IdentitySettings.DEFAULTS)
			.diagnosticProviders().getFirst();

		assertSound(provider, model());
	}

	@Test
	void theIdentityAssistantTellsWhatElseItDependsOn(){
		final IdentitySettings settings = new IdentitySettings(70, 50, 90, 70, 70);
		final DiagnosticProvider provider = new IdentityAssistantContributor(() -> settings).diagnosticProviders().getFirst();

		Assertions.assertEquals(settings, provider.environment());
	}

}
