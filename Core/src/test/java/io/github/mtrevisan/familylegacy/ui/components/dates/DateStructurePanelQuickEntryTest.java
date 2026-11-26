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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthandReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;


/**
 * A date typed in the quick entry fills the tabs, the original text, and is saved as the user would have entered it by hand.
 */
class DateStructurePanelQuickEntryTest{

	private static final String POINT = "value.point.";
	private static final String NOT_BEFORE = "value.bounded.not_before.";
	private static final String NOT_AFTER = "value.bounded.not_after.";
	private static final String FROM = "value.spanning.from.";
	private static final String TO = "value.spanning.to.";


	@Test
	void approximatePointWithMargin() throws Exception{
		final FLEFRecord record = quickEntry("abt 1850 ±2");

		Assertions.assertEquals("1850", value(record, POINT + "value.full_date"));
		Assertions.assertEquals("gregorian", value(record, POINT + "calendar"));
		Assertions.assertEquals("stated", value(record, POINT + "approximate.basis"));
		Assertions.assertEquals("P2Y", value(record, POINT + "approximate.margin"));
		Assertions.assertEquals("abt 1850 ±2", value(record, "original_text"));
	}

	@Test
	void calculatedPoint() throws Exception{
		final FLEFRecord record = quickEntry("calc 12 March 1850");

		Assertions.assertEquals("12 March 1850", value(record, POINT + "value.full_date"));
		Assertions.assertEquals("calculated", value(record, POINT + "approximate.basis"));
		Assertions.assertNull(value(record, POINT + "approximate.margin"));
	}

	@Test
	void plainDateKeepsTheOriginalTextEmpty() throws Exception{
		final FLEFRecord record = quickEntry("1850-03-12");

		Assertions.assertEquals("1850-03-12", value(record, POINT + "value.full_date"));
		Assertions.assertNull(value(record, POINT + "approximate"));
		Assertions.assertNull(value(record, "original_text"));
	}

	@Test
	void between() throws Exception{
		final FLEFRecord record = quickEntry("bet abt 1850 and 1860");

		Assertions.assertEquals("1850", value(record, NOT_BEFORE + "value.full_date"));
		Assertions.assertEquals("stated", value(record, NOT_BEFORE + "approximate.basis"));
		Assertions.assertEquals("1860", value(record, NOT_AFTER + "value.full_date"));
		Assertions.assertNull(value(record, NOT_AFTER + "approximate"));
		Assertions.assertNull(value(record, POINT + "value.full_date"));
	}

	@Test
	void before() throws Exception{
		final FLEFRecord record = quickEntry("bef 1850");

		Assertions.assertNull(FLEFRecordHelper.findChild(record, "value.bounded.not_before"));
		Assertions.assertEquals("1850", value(record, NOT_AFTER + "value.full_date"));
	}

	@Test
	void spanningOpenOnOneSide() throws Exception{
		final FLEFRecord record = quickEntry("from 1850");

		Assertions.assertEquals("1850", value(record, FROM + "value.full_date"));
		Assertions.assertNull(FLEFRecordHelper.findChild(record, "value.spanning.to"));

		final FLEFRecord until = quickEntry("until 1860");

		Assertions.assertNull(FLEFRecordHelper.findChild(until, "value.spanning.from"));
		Assertions.assertEquals("1860", value(until, TO + "value.full_date"));
	}

	@Test
	void decadeAndCentury() throws Exception{
		final FLEFRecord decade = quickEntry("1850s");

		Assertions.assertEquals("1850", value(decade, POINT + "value.decade"));
		Assertions.assertNull(value(decade, POINT + "value.full_date"));

		final FLEFRecord century = quickEntry("early 19th century");

		Assertions.assertEquals("19", value(century, POINT + "value.century.ordinal"));
		Assertions.assertEquals("early", value(century, POINT + "value.century.part"));
		Assertions.assertNull(value(century, POINT + "value.full_date"));
	}

	@Test
	void descriptionOfTheExpression(){
		Assertions.assertEquals("Point: 1850 (approx., stated, margin P2Y)",
			DateStructurePanel.describe(DateShorthandReader.parse("abt 1850 ±2")));
		Assertions.assertEquals("Bounded: not before 1850, not after 1860 (approx., calculated)",
			DateStructurePanel.describe(DateShorthandReader.parse("bet 1850 and calc 1860")));
		Assertions.assertEquals("Bounded: not after 1850", DateStructurePanel.describe(DateShorthandReader.parse("bef 1850")));
		Assertions.assertEquals("Spanning: from 1850", DateStructurePanel.describe(DateShorthandReader.parse("from 1850")));
		Assertions.assertEquals("Spanning: until 1860", DateStructurePanel.describe(DateShorthandReader.parse("until 1860")));
		Assertions.assertEquals("Point: 1850s", DateStructurePanel.describe(DateShorthandReader.parse("1850s")));
		Assertions.assertEquals("Point: late 19th c.", DateStructurePanel.describe(DateShorthandReader.parse("late 19th c.")));
	}


	/** Types the expression in the quick entry of a new panel, validates it as the OK button does and gives what would be saved. */
	private static FLEFRecord quickEntry(final String text) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The panel needs a display");

		final FLEFRecord[] saved = new FLEFRecord[1];
		onEdt(() -> {
			final DateStructurePanel panel = new DateStructurePanel(null, new FLEFModel());
			findQuickEntryField(panel).setText(text);

			Assertions.assertTrue(panel.validateData());

			saved[0] = panel.save();
		});
		Assertions.assertNotNull(saved[0]);
		return saved[0];
	}

	private static JTextField findQuickEntryField(final Container panel){
		for(final Component component : panel.getComponents()){
			//the first text field of the panel is the quick entry
			if(component instanceof JTextField field)
				return field;
			if(component instanceof Container child){
				final JTextField field = findQuickEntryField(child);
				if(field != null)
					return field;
			}
		}
		return null;
	}

	private static String value(final FLEFRecord record, final String path){
		return FLEFRecordHelper.getChildValue(record, path);
	}

	private static void onEdt(final Runnable action) throws Exception{
		final Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() -> {
			try{
				action.run();
			}
			catch(final Throwable t){
				failure[0] = t;
			}
		});
		if(failure[0] instanceof RuntimeException re)
			throw re;
		if(failure[0] instanceof Error error)
			throw error;
		if(failure[0] != null)
			throw new IllegalStateException(failure[0]);
	}

}
