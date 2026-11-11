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
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * No content of a record or structure dialog touches the border of the dialog: there is a margin on every side, as in the dialogs with
 * tabs. (The dialogs without tabs, like the one of a part of a name, used to have none.)
 */
class DialogsMarginTest{

	/** The least space, in pixels, tolerated between the content and the border (the layout gives {@link GUIHelper#DIALOG_MARGIN}). */
	private static final int MIN_MARGIN = 5;

	private static final List<String> TYPES = new ArrayList<>();
	static{
		TYPES.addAll(DialogsEscapeTest.recordTypes());
		TYPES.addAll(DialogsEscapeTest.structureTypes());
	}


	@Test
	void theContentOfEveryDialogStaysAwayFromTheBorder() throws IOException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = DialogsEscapeTest.convert();
		final List<String> touching = new ArrayList<>();

		for(final String type : TYPES){
			final JDialog parent = new JDialog((Window)null, "parent");
			parent.pack();
			final JDialog dialog;
			try{
				dialog = (JDialog)HandlerRegistry.getHandler(type).createNewDialog(parent, model).window();
			}
			catch(final RuntimeException | AssertionError e){
				Assertions.fail("the dialog of " + type + " could not be built: " + e);
				return;
			}

			final Container content = dialog.getContentPane();
			content.doLayout();
			final Component[] children = content.getComponents();
			//the last child is the row of the buttons, docked to the bottom: it has its own space around the buttons
			for(int i = 0; i < children.length - 1; i ++){
				final Component child = children[i];
				if(!child.isVisible())
					continue;

				final int right = content.getWidth() - (child.getX() + child.getWidth());
				final int bottom = content.getHeight() - (child.getY() + child.getHeight());
				final int margin = Math.min(Math.min(child.getX(), child.getY()), Math.min(right, bottom));
				if(margin < MIN_MARGIN)
					touching.add(type + " (" + dialog.getTitle() + "): " + child.getClass().getSimpleName() + ", margin " + margin);
			}
			dialog.dispose();
			parent.dispose();
		}

		Assertions.assertTrue(touching.isEmpty(), "content too close to the border of the dialog: " + touching);
	}

}
