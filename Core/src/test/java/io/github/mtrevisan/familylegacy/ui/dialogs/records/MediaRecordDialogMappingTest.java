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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The mapping of a media (spherical, cubemap...) is behind More...: it is in sight at once only for a media that has one. */
class MediaRecordDialogMappingTest{

	private static FLEFRecord media(final String mapping){
		final FLEFRecord record = FLEFRecord.createMainRecord("M1", MediaReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_URI, "/tmp/photo.jpg"))
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_KIND, MediaReader.inferKind("photo.jpg")));
		if(mapping != null)
			record.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_MAPPING, mapping));
		return record;
	}

	private static <T extends Component> List<T> all(final Container container, final Class<T> type){
		final List<T> found = new ArrayList<>();
		for(final Component child : container.getComponents()){
			if(type.isInstance(child))
				found.add(type.cast(child));
			if(child instanceof Container c)
				found.addAll(all(c, type));
		}
		return found;
	}

	private static JComboBox<?> mappingCombo(final MediaRecordDialog dialog){
		//the combo of the mapping is the one that offers the cubemap
		for(final JComboBox<?> combo : all(dialog.getContentPane(), JComboBox.class))
			for(int i = 0; i < combo.getItemCount(); i ++)
				if(MediaReader.ENUM_MAPPING_CUBEMAP.equals(String.valueOf(combo.getItemAt(i))))
					return combo;
		return null;
	}

	private static AbstractButton moreButton(final MediaRecordDialog dialog){
		for(final AbstractButton button : all(dialog.getContentPane(), AbstractButton.class))
			if(button.getText() != null && button.getText().startsWith("More"))
				return button;
		return null;
	}


	@Test
	void anOrdinaryMediaHasTheMappingBehindMore() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord record = media(null);
		model.addRecord(record);

		SwingUtilities.invokeAndWait(() -> {
			final MediaRecordDialog dialog = MediaRecordDialog.createEdit(null, model, record);
			final JComboBox<?> combo = mappingCombo(dialog);
			final AbstractButton more = moreButton(dialog);

			Assertions.assertNotNull(combo);
			Assertions.assertNotNull(more);
			Assertions.assertFalse(combo.isVisible());
			Assertions.assertTrue(more.isVisible());

			more.doClick();

			Assertions.assertTrue(combo.isVisible());
			Assertions.assertFalse(more.isVisible());
			dialog.dispose();
		});
	}

	@Test
	void aPlanarMappingIsTheOrdinaryOne() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord record = media(MediaReader.ENUM_MAPPING_PLANAR);
		model.addRecord(record);

		SwingUtilities.invokeAndWait(() -> {
			final MediaRecordDialog dialog = MediaRecordDialog.createEdit(null, model, record);

			Assertions.assertFalse(mappingCombo(dialog).isVisible());
			dialog.dispose();
		});
	}

	@Test
	void aMediaThatHasAnotherMappingShowsItAtOnce() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final FLEFModel model = new FLEFModel();
		final FLEFRecord record = media(MediaReader.ENUM_MAPPING_CUBEMAP);
		model.addRecord(record);

		SwingUtilities.invokeAndWait(() -> {
			final MediaRecordDialog dialog = MediaRecordDialog.createEdit(null, model, record);
			final JComboBox<?> combo = mappingCombo(dialog);

			Assertions.assertTrue(combo.isVisible());
			Assertions.assertFalse(moreButton(dialog).isVisible(), "there is nothing more to show");
			final JLabel label = null;
			Assertions.assertNull(label);
			dialog.dispose();
		});
	}

}
