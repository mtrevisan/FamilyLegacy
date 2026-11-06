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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDiffDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicReference;


/** "Compare Side by Side" opens the read-only comparison of the two records, and changes nothing. */
class CompareRecordsActionTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TAG_SEX, "M")));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TAG_SEX, "F")));
		return model;
	}

	/**
	 * Runs the action on the event dispatch thread, closing the comparison dialog as soon as it is on the screen (it is modal: the action
	 * returns when it is closed).
	 *
	 * @return	The title of the dialog that was shown, {@code null} if none.
	 */
	private static String run(final CompareRecordsAction action, final FLEFModel model) throws Exception{
		final AtomicReference<String> shown = new AtomicReference<>();
		final Timer closer = new Timer(50, e -> {
			for(final Window window : Window.getWindows())
				if(window instanceof RecordDiffDialog dialog && dialog.isVisible()){
					shown.set(dialog.getTitle());
					dialog.dispose();
				}
		});
		closer.start();
		try{
			SwingUtilities.invokeAndWait(() -> action.execute(new AssistantContext(null, null, model)));
		}
		finally{
			closer.stop();
		}
		return shown.get();
	}


	@Test
	void theComparisonOpensWithItsTitleAndChangesNothing() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The comparison needs a display");
		final FLEFModel model = model();
		final int count = model.getRecordCount();
		final String before = model.getRecordById("I1").toString();

		final String title = run(new CompareRecordsAction("I1", "I2", "Compare A vs B"), model);

		Assertions.assertEquals("Compare A vs B", title);
		Assertions.assertEquals(count, model.getRecordCount());
		Assertions.assertEquals(before, model.getRecordById("I1").toString());
	}

	@Test
	void aRecordThatIsGoneOpensNothing() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The comparison needs a display");

		Assertions.assertNull(run(new CompareRecordsAction("I1", "I404", "t"), model()));
	}

	@Test
	void aModelThatIsMissingOpensNothing() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The comparison needs a display");
		final AtomicReference<String> shown = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> new CompareRecordsAction("I1", "I2", "t").execute(new AssistantContext(null, null, null)));

		Assertions.assertNull(shown.get());
	}

}
