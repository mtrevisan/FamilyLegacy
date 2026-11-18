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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityLoops;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.List;


/** What the consanguinity loops dialog says: the line from the ancestor down to the individual, and the summary. */
class ConsanguinityLoopsDialogTest{

	private static FLEFModel model(final String... ids){
		final FLEFModel model = new FLEFModel();
		for(final String id : ids)
			model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE));
		return model;
	}


	@Test
	void aLineIsWrittenFromTheAncestorDownToTheIndividual(){
		final FLEFModel model = model("I1", "I2", "I3", "I4");
		final String arrow = " → ";

		Assertions.assertEquals(name(model, "I3") + arrow + name(model, "I2") + arrow + name(model, "I1") + arrow + "I4",
			ConsanguinityLoopsDialog.lineText(model, List.of("I1", "I2", "I3"), "I4"));
		//a parent that is the ancestor itself
		Assertions.assertEquals(name(model, "I1") + arrow + "I4", ConsanguinityLoopsDialog.lineText(model, List.of("I1"), "I4"));
	}

	private static String name(final FLEFModel model, final String id){
		return RecordDisplay.describe(model.getRecordById(id), model);
	}

	@Test
	void smallCoefficientsKeepTheDigitsThatTellThemApart(){
		Assertions.assertEquals("6.25 %", ConsanguinityLoopsDialog.percent(0.0625));
		Assertions.assertEquals("0.0244 %", ConsanguinityLoopsDialog.percent(0.000244140625));
		Assertions.assertEquals("0.00 %", ConsanguinityLoopsDialog.percent(0.));
	}

	@Test
	void theSummaryTellsWhyThereAreNoLoops(){
		final ConsanguinityLoops.Result none = new ConsanguinityLoops.Result("I1", null, null, 0., List.of(), false);
		final ConsanguinityLoops.Result unrelated = new ConsanguinityLoops.Result("I1", "F", "M", 0., List.of(), false);

		Assertions.assertTrue(ConsanguinityLoopsDialog.summaryOf(none).contains("two known biological parents"));
		Assertions.assertTrue(ConsanguinityLoopsDialog.summaryOf(unrelated).contains("not related"));
	}

	@Test
	void theSummaryGivesTheCoefficientAndTheNumberOfLoops(){
		final ConsanguinityLoops.Loop loop = new ConsanguinityLoops.Loop("G1", List.of("X", "A", "G1"), List.of("Y", "B", "G1"), 1. / 32.);
		final ConsanguinityLoops.Result result = new ConsanguinityLoops.Result("Z", "X", "Y", 0.0625, List.of(loop, loop), false);

		final String summary = ConsanguinityLoopsDialog.summaryOf(result);

		Assertions.assertTrue(summary.contains("6.25 %"), summary);
		Assertions.assertTrue(summary.contains("2 loop(s)"), summary);
		Assertions.assertFalse(summary.contains("Only the"), summary);
	}

	@Test
	void theWindowOpensWithTheLoopsAndIsClosed() throws Exception{
		org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model("X", "Y", "G1", "Z");
		final ConsanguinityLoops.Loop loop = new ConsanguinityLoops.Loop("G1", List.of("X", "G1"), List.of("Y", "G1"), 1. / 8.);
		final ConsanguinityLoops.Result result = new ConsanguinityLoops.Result("Z", "X", "Y", 0.125, List.of(loop), false);
		final int[] components = new int[1];

		javax.swing.SwingUtilities.invokeAndWait(() -> {
			final ConsanguinityLoopsDialog dialog = new ConsanguinityLoopsDialog(null, model, result);
			components[0] = dialog.getContentPane().getComponentCount();
			dialog.dispose();
		});

		Assertions.assertTrue(components[0] > 0);
	}

}
