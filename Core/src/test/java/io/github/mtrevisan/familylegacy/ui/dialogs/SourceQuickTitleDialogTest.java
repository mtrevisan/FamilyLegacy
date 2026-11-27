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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quickentry.SourceQuickEntry;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.SourceRecordDialog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** A source with one plain title shows it as a text; a citation that has no stance supports. */
class SourceQuickTitleDialogTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		model = new FLEFModel();
	}


	private static <T extends Component> List<T> find(final Container root, final Class<T> type){
		final List<T> found = new ArrayList<>();
		for(final Component child : root.getComponents()){
			if(type.isInstance(child))
				found.add(type.cast(child));
			if(child instanceof Container container)
				found.addAll(find(container, type));
		}
		return found;
	}

	/** The text field of the title: the first of the Properties tab. */
	private static JTextField titleField(final SourceRecordDialog dialog){
		return find(dialog.getContentPane(), JTextField.class).getFirst();
	}

	/** @return	Whether the text of the title is shown (its row is, the list of the titles is not). */
	private static boolean titleShown(final SourceRecordDialog dialog){
		return titleField(dialog).getParent().isVisible();
	}

	private static String title(final FLEFRecord source){
		return FLEFRecordHelper.getChildValue(source, FLEFRecordHelper.composePath(SourceReader.TAG_TITLE, SourceReader.TAG_TITLE_VALUE));
	}


	@Test
	void aNewSourceTakesItsTitleFromTheTextField(){
		final SourceRecordDialog dialog = SourceRecordDialog.createNew(null, model);
		titleField(dialog).setText("  Parish of San   Martino ");

		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		final FLEFRecord source = model.getRecordsByType(SourceReader.TYPE).getFirst();
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(source, SourceReader.TAG_TITLE).size());
		Assertions.assertEquals("Parish of San Martino", title(source));
	}

	@Test
	void theTitleOfAnExistingSourceIsShownAndChangedInPlace(){
		final String id = SourceQuickEntry.createSource(model, "Register", null).sourceId();
		final FLEFRecord source = model.getRecordById(id);

		final SourceRecordDialog dialog = SourceRecordDialog.createEdit(null, model, source);
		Assertions.assertEquals("Register", titleField(dialog).getText());
		Assertions.assertTrue(titleShown(dialog));
		titleField(dialog).setText("Register of baptisms");
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals("Register of baptisms", title(source));
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(source, SourceReader.TAG_TITLE).size());
	}

	@Test
	void severalTitlesAreShownAsTheList(){
		final String id = SourceQuickEntry.createSource(model, "Register", null).sourceId();
		final FLEFRecord source = model.getRecordById(id);
		source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE_VALUE, "Registro")));

		final SourceRecordDialog dialog = SourceRecordDialog.createEdit(null, model, source);

		Assertions.assertFalse(titleShown(dialog));
		dialog.save();
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(source, SourceReader.TAG_TITLE).size());
	}

	@Test
	void aTitleWithMoreThanItsTextIsShownAsTheList(){
		final String id = SourceQuickEntry.createSource(model, "Register", null).sourceId();
		final FLEFRecord source = model.getRecordById(id);
		source.getChildren().getFirst()
			.addChild(FLEFRecord.createChildWithTagAndValue("locale", "en"));

		final SourceRecordDialog dialog = SourceRecordDialog.createEdit(null, model, source);

		Assertions.assertFalse(titleShown(dialog));
		dialog.dispose();
	}

	@Test
	void theButtonOfTheSeveralTitlesKeepsTheTextAsTheFirstTitle(){
		final SourceRecordDialog dialog = SourceRecordDialog.createNew(null, model);
		titleField(dialog).setText("Register");
		final JButton several = find(dialog.getContentPane(), JButton.class).getFirst();

		several.doClick();

		Assertions.assertFalse(titleShown(dialog));
		dialog.save();
		Assertions.assertEquals("Register", title(model.getRecordsByType(SourceReader.TYPE).getFirst()));
	}

	@Test
	void aCitationWithNoStanceSupports(){
		final String id = SourceQuickEntry.createSource(model, "Register", null).sourceId();
		final FLEFRecord citation = FLEFRecord.createEmpty()
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, id));

		final SourceCitationDialog dialog = SourceCitationDialog.createEdit(null, model, citation);
		dialog.save();

		Assertions.assertTrue(dialog.isSaved());
		Assertions.assertEquals(SourceCitationReader.STANCE_SUPPORTS, FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE));
	}

	@Test
	void aCitationThatContradictsStaysSo(){
		final String id = SourceQuickEntry.createSource(model, "Register", null).sourceId();
		final FLEFRecord citation = FLEFRecord.createEmpty()
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, id))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_CONTRADICTS));

		final SourceCitationDialog dialog = SourceCitationDialog.createEdit(null, model, citation);
		dialog.save();

		Assertions.assertEquals(SourceCitationReader.STANCE_CONTRADICTS, FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE));
	}

}
