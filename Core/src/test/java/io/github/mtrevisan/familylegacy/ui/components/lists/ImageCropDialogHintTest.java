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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;


/** The crop dialog can say, under the image, that the crop also tags a person in it; with nothing to say it shows nothing. */
class ImageCropDialogHintTest{

	private static JLabel hintOf(final Container container){
		for(final Component component : container.getComponents()){
			//(the image itself is a label too)
			if(component.getClass() == JLabel.class && component instanceof JLabel label)
				return label;
			if(component instanceof Container child){
				final JLabel found = hintOf(child);
				if(found != null)
					return found;
			}
		}
		return null;
	}


	@Test
	void theHintIsShownOnlyWhenThereIsOne(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialog needs a display");
		final ImageCropDialog dialog = ImageCropDialog.create(null);
		try{
			final JLabel hint = hintOf(dialog.getContentPane());
			Assertions.assertNotNull(hint);
			Assertions.assertFalse(hint.isVisible(), "no hint by default");

			dialog.setHint("The crop is also a region");
			Assertions.assertTrue(hint.isVisible());
			Assertions.assertEquals("The crop is also a region", hint.getText());

			dialog.setHint(null);
			Assertions.assertFalse(hint.isVisible());
			dialog.setHint("   ");
			Assertions.assertFalse(hint.isVisible());
		}
		finally{
			dialog.dispose();
		}
	}

}