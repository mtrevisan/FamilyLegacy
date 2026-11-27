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
package io.github.mtrevisan.familylegacy.ui.shell;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The line of the tool bar that tells what a tool did. */
class StatusMessageViewTest{

	@Test
	void itIsHiddenUntilThereIsSomethingToSay(){
		final StatusMessageView view = new StatusMessageView();

		Assertions.assertFalse(view.isVisible());

		view.show("Quick entry created 2 people.");

		Assertions.assertTrue(view.isVisible());
		Assertions.assertEquals("Quick entry created 2 people.", view.getText());
	}

	@Test
	void aNewMessageTakesThePlaceOfTheOldOne(){
		final StatusMessageView view = new StatusMessageView();
		view.show("first");

		view.show("second");

		Assertions.assertEquals("second", view.getText());
		Assertions.assertTrue(view.isVisible());
	}

	@Test
	void aBlankMessageOrTheEndOfTheTimeTakesItAway(){
		final StatusMessageView view = new StatusMessageView();
		view.show("something");

		view.show("  ");
		Assertions.assertFalse(view.isVisible());
		Assertions.assertEquals("", view.getText());

		view.show("again");
		view.clear();
		Assertions.assertFalse(view.isVisible());
	}

}
