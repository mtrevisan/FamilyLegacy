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
package io.github.mtrevisan.familylegacy.ui.components.fields;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;


/** The text of a note offers the links when the user types {@code flef:}, and writes what is chosen after it. */
class FlefLinkAssistantTest{

	private static PlainDocument document(final String text) throws BadLocationException{
		final PlainDocument document = new PlainDocument();
		document.insertString(0, text, null);
		return document;
	}


	@Test
	void theColonThatCompletesThePrefixOfAWellFormedLinkIsTheTrigger() throws BadLocationException{
		for(final String text : new String[]{"see [him](flef:", "[a](flef:", "[a [b](flef:", "x [two words](flef:", "[old][new](flef:"}){
			Assertions.assertTrue(FlefLinkAssistant.isLinkTyped(document(text), text.length() - 1, 1), text);
		}
	}

	@Test
	void withoutAnOpeningBracketATextAndTheClosingOneThereIsNoTrigger() throws BadLocationException{
		for(final String text : new String[]{"flef:", "see flef:", "see (flef:", "see ](flef:", "[](flef:", "[ ](flef:", "[a]b](flef:", "see [him] (flef:",
				"[him]flef:", "[him](http:"}){
			Assertions.assertFalse(FlefLinkAssistant.isLinkTyped(document(text), text.length() - 1, 1), text);
		}
	}

	@Test
	void anotherCharacterOrAPasteIsNotTheTrigger() throws BadLocationException{
		Assertions.assertFalse(FlefLinkAssistant.isLinkTyped(document("see [him](flef"), 13, 1), "the f of flef");
		Assertions.assertFalse(FlefLinkAssistant.isLinkTyped(document("see [him](flef:I12"), 15, 3), "a pasted text");
		//(the colon is not the last character of the text: it is typed in the middle)
		Assertions.assertTrue(FlefLinkAssistant.isLinkTyped(document("see [him](flef: and more"), 14, 1));
	}

	private static KeyEvent key(final JTextArea source, final int code){
		return new KeyEvent(source, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, code, KeyEvent.CHAR_UNDEFINED);
	}

	private static String selectedItem(){
		final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
		return (path.length > 1 && path[path.length - 1] instanceof JMenuItem item? item.getText(): null);
	}

	@Test
	void theArrowsMoveInTheShownPopupAndEnterTakesTheItem() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The popup needs a display");
		final JTextArea field = new JTextArea("Son of [Giovanni](flef:");
		final JFrame frame = new JFrame();
		final String[] first = new String[1];
		final String[] afterDown = new String[1];
		final String[] afterWrap = new String[1];
		final boolean[] consumed = new boolean[3];
		final boolean[] hiddenByEscape = new boolean[1];
		final Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() -> {
			try{
				frame.add(field);
				frame.setSize(400, 200);
				frame.setVisible(true);
				field.setCaretPosition(field.getText().length());
				final FlefLinkAssistant assistant = FlefLinkAssistant.install(field, frame, new FLEFModel());

				//(nothing happens to the keys while the popup is not shown)
				consumed[0] = assistant.handleKey(key(field, KeyEvent.VK_DOWN));

				assistant.showPopup();
				first[0] = selectedItem();
				final KeyEvent down = key(field, KeyEvent.VK_DOWN);
				consumed[1] = assistant.handleKey(down);
				afterDown[0] = selectedItem();
				consumed[2] = down.isConsumed();
				//(after the last comes the first again)
				assistant.handleKey(key(field, KeyEvent.VK_DOWN));
				afterWrap[0] = selectedItem();
				assistant.handleKey(key(field, KeyEvent.VK_UP));

				//Enter takes the item the arrows are on: the confidential marker
				assistant.handleKey(key(field, KeyEvent.VK_ENTER));

				//Escape closes the popup, and is taken
				assistant.showPopup();
				assistant.handleKey(key(field, KeyEvent.VK_ESCAPE));
				hiddenByEscape[0] = !assistant.handleKey(key(field, KeyEvent.VK_DOWN));
			}
			catch(final Throwable t){
				failure[0] = t;
			}
			finally{
				frame.dispose();
			}
		});

		Assertions.assertNull(failure[0], String.valueOf(failure[0]));
		Assertions.assertFalse(consumed[0]);
		Assertions.assertEquals(I18N.t("text.flef_link.record"), first[0]);
		Assertions.assertTrue(consumed[1] && consumed[2]);
		Assertions.assertEquals(I18N.t("text.flef_link.confidential"), afterDown[0]);
		Assertions.assertEquals(I18N.t("text.flef_link.record"), afterWrap[0]);
		Assertions.assertEquals("Son of [Giovanni](flef:confidential", field.getText().substring(0, "Son of [Giovanni](flef:confidential".length()));
		Assertions.assertTrue(hiddenByEscape[0]);
	}

	@Test
	void theChosenIdIsWrittenAfterThePrefixAndTheLinkIsClosed(){
		final JTextArea field = new JTextArea("Son of [Giovanni](flef:");
		field.setCaretPosition(field.getText().length());
		final FlefLinkAssistant assistant = FlefLinkAssistant.install(field, null, new FLEFModel());

		assistant.insert("I12");

		Assertions.assertEquals("Son of [Giovanni](flef:I12)", field.getText());
		Assertions.assertEquals(field.getText().length(), field.getCaretPosition());
	}

	@Test
	void aLinkThatIsAlreadyClosedIsNotClosedTwice(){
		final JTextArea field = new JTextArea("[Giovanni](flef:) and more");
		field.setCaretPosition("[Giovanni](flef:".length());
		final FlefLinkAssistant assistant = FlefLinkAssistant.install(field, null, new FLEFModel());

		assistant.insert("I12");

		Assertions.assertEquals("[Giovanni](flef:I12) and more", field.getText());
		Assertions.assertEquals("[Giovanni](flef:I12".length(), field.getCaretPosition());
	}

	@Test
	void theConfidentialMarkerIsWrittenTheSameWay(){
		final JTextArea field = new JTextArea("He was [born at home](flef:");
		field.setCaretPosition(field.getText().length());
		final FlefLinkAssistant assistant = FlefLinkAssistant.install(field, null, new FLEFModel());

		assistant.insert(FlefLinkAssistant.CONFIDENTIAL);

		Assertions.assertEquals("He was [born at home](flef:confidential)", field.getText());
	}

	@Test
	void withoutALinkBeforeThePrefixOnlyTheIdIsWritten(){
		final JTextArea field = new JTextArea("flef:");
		field.setCaretPosition(5);
		final FlefLinkAssistant assistant = FlefLinkAssistant.install(field, null, new FLEFModel());

		assistant.insert("I12");

		Assertions.assertEquals("flef:I12", field.getText());
	}

}
