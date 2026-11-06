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
package io.github.mtrevisan.familylegacy.ui.tools.validate;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.FileValidation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/** The validation tells how far it is, and its report opens at once with a progress bar. */
class ValidationProgressTest{

	private static FLEFModel model(final int individuals) throws IOException{
		final StringBuilder gedcom = new StringBuilder("0 HEAD\n1 CHAR UTF-8\n");
		for(int i = 0; i < individuals; i ++)
			gedcom.append("0 @I").append(i + 1).append("@ INDI\n1 NAME Mario").append(i).append(" /Rossi/\n1 SEX M\n1 BIRT\n2 DATE 1 JAN 1850\n");
		return new GEDCOMToFLEFConverter().convert(new GEDCOMParser().parse(new StringReader(gedcom.toString())));
	}

	private static void assertMonotone(final List<Double> values){
		Assertions.assertFalse(values.isEmpty());
		double previous = 0.;
		for(final double value : values){
			Assertions.assertTrue(value >= 0. && value <= 1., String.valueOf(value));
			Assertions.assertTrue(value >= previous - 1e-12, value + " after " + previous);
			previous = value;
		}
	}


	@Test
	void theValidationOfTheFileTellsHowFarItIsAndEndsWithAll() throws IOException{
		final List<Double> values = new ArrayList<>();

		FileValidation.run(new FLEFValidator(MergeGrammar.instance()), model(300), null, values::add);

		assertMonotone(values);
		Assertions.assertEquals(1., values.getLast(), 1e-12);
		//the records are many: it reports many times, not only at the end
		Assertions.assertTrue(values.size() > 6, String.valueOf(values.size()));
		//and the first reports are of the grammar, the larger share of the work
		Assertions.assertTrue(values.getFirst() < 0.85, String.valueOf(values.getFirst()));
	}

	@Test
	void theValidatorAloneTellsHowFarItIsThroughTheRecords() throws IOException{
		final List<Double> values = new ArrayList<>();

		new FLEFValidator(MergeGrammar.instance()).validateAllStructured(model(300), values::add);

		assertMonotone(values);
		Assertions.assertEquals(1., values.getLast(), 1e-12);
		Assertions.assertTrue(values.stream().anyMatch(value -> value > 0.1 && value < 0.7), values.toString());
	}

	@Test
	void theResultIsTheSameWithOrWithoutTheProgress() throws IOException{
		final FLEFModel model = model(120);
		final FLEFValidator validator = new FLEFValidator(MergeGrammar.instance());

		final List<FLEFValidator.ValidationError> without = FileValidation.run(validator, model, null);
		final List<FLEFValidator.ValidationError> with = FileValidation.run(validator, model, null, fraction -> {});

		Assertions.assertEquals(without, with);
	}

	@Test
	void aValidationThatIsInterruptedStopsInsteadOfGoingOn() throws Exception{
		final FLEFModel model = model(600);
		final AtomicReference<Object> result = new AtomicReference<>();
		final Thread thread = new Thread(() -> result.set(FileValidation.run(new FLEFValidator(MergeGrammar.instance()), model, null,
			fraction -> Thread.currentThread().interrupt())));

		thread.start();
		thread.join(60_000);

		Assertions.assertFalse(thread.isAlive());
		Assertions.assertNotNull(result.get(), "it returns what it had, it does not throw");
	}


	/* ---- the dialog ---- */

	private static JProgressBar bar(final Container container){
		for(final Component child : container.getComponents()){
			if(child instanceof JProgressBar bar)
				return bar;
			if(child instanceof Container nested){
				final JProgressBar found = bar(nested);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	private static void labels(final Container container, final List<String> texts){
		for(final Component child : container.getComponents()){
			if(child instanceof JLabel label && label.getText() != null)
				texts.add(label.getText());
			if(child instanceof Container nested)
				labels(nested, texts);
		}
	}

	private static JButton button(final Container container, final String text){
		for(final Component child : container.getComponents()){
			if(child instanceof JButton button && text.equals(button.getText()))
				return button;
			if(child instanceof Container nested){
				final JButton found = button(nested, text);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	@Test
	void theReportOpensAtOnceWithTheBarAndShowsWhatTheValidationFindsWhenItIsDone() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		final CountDownLatch halfWay = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicReference<ValidationReportDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(ValidationReportDialog.validating(null, model, () -> model, (view, progress) -> {
			progress.accept(0.5);
			halfWay.countDown();
			try{
				release.await(30, TimeUnit.SECONDS);
			}
			catch(final InterruptedException e){
				Thread.currentThread().interrupt();
			}
			progress.accept(1.);
			return List.of(new FLEFValidator.ValidationError("A problem", "I1", FLEFValidator.Severity.ERROR, "cycle"));
		})));
		final ValidationReportDialog dialog = created.get();
		final JProgressBar bar = bar(dialog);
		Assertions.assertTrue(halfWay.await(30, TimeUnit.SECONDS));
		//let the progress of the worker reach the event dispatch thread
		Thread.sleep(300);
		SwingUtilities.invokeAndWait(() -> {});

		final List<String> during = new ArrayList<>();
		final boolean[] state = new boolean[2];
		SwingUtilities.invokeAndWait(() -> {
			labels(dialog, during);
			state[0] = bar.isVisible();
			state[1] = button(dialog, "Re-run").isEnabled();
		});

		//it is validating: the bar is there, at half, the status says so, and the report cannot be run again meanwhile
		Assertions.assertTrue(state[0], "the bar is visible while it validates");
		Assertions.assertEquals(50, bar.getValue());
		Assertions.assertTrue(during.contains("Validating\u2026"), during.toString());
		Assertions.assertFalse(state[1], "Re-run is disabled while it validates");

		release.countDown();
		final long deadline = System.currentTimeMillis() + 30_000L;
		while(bar.isVisible() && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		SwingUtilities.invokeAndWait(() -> {});
		final List<String> after = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			labels(dialog, after);
			state[0] = bar.isVisible();
			state[1] = button(dialog, "Re-run").isEnabled();
			dialog.dispose();
		});

		Assertions.assertFalse(state[0], "the bar goes away when it is done");
		Assertions.assertTrue(state[1], "Re-run is enabled again");
		Assertions.assertTrue(after.stream().anyMatch(text -> text.contains("1 issue found")), after.toString());
	}

	@Test
	void closingTheReportWhileItValidatesStopsTheValidation() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final FLEFModel model = new FLEFModel();
		final CountDownLatch started = new CountDownLatch(1);
		final CountDownLatch interrupted = new CountDownLatch(1);
		final AtomicReference<ValidationReportDialog> created = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> created.set(ValidationReportDialog.validating(null, model, () -> model, (view, progress) -> {
			started.countDown();
			try{
				Thread.sleep(60_000);
			}
			catch(final InterruptedException e){
				interrupted.countDown();
			}
			return List.of();
		})));
		Assertions.assertTrue(started.await(30, TimeUnit.SECONDS));

		SwingUtilities.invokeAndWait(() -> created.get().dispose());

		Assertions.assertTrue(interrupted.await(30, TimeUnit.SECONDS), "the validation is cancelled with the report");
	}

}
