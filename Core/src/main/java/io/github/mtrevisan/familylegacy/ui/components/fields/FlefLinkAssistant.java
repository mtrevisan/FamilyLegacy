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
import io.github.mtrevisan.familylegacy.ui.components.searches.service.FindRecordDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;


/**
 * Helps to write the links of a note ({@code [text](flef:<Id>)}, {@code [text](flef:confidential)}): when the user types {@code flef:} after
 * the text of a link ({@code [text](}), a popup offers to choose a record (its identifier is written after the colon) or to mark the text as confidential. The user
 * can ignore it and go on typing: it goes away at the next key.
 */
public final class FlefLinkAssistant{

	/** What starts a link to a record, or to confidential data. */
	static final String PREFIX = "flef:";
	/** What the text of a link ends with: a link {@code [text](flef:...)} is closed by a parenthesis. */
	private static final char CLOSE = ')';
	private static final String LINK_OPEN = "](";
	static final String CONFIDENTIAL = "confidential";


	private final JTextComponent field;
	private final Window parent;
	private final FLEFModel model;
	private final JPopupMenu popup = new JPopupMenu();
	private final List<JMenuItem> items = new ArrayList<>(2);
	/** The item of the popup the arrows are on (Enter takes it). */
	private int selected;


	private FlefLinkAssistant(final JTextComponent field, final Window parent, final FLEFModel model){
		this.field = field;
		this.parent = parent;
		this.model = model;

		final JMenuItem record = new JMenuItem(I18N.t("text.flef_link.record"));
		record.addActionListener(e -> chooseRecord());
		popup.add(record);
		items.add(record);
		final JMenuItem confidential = new JMenuItem(I18N.t("text.flef_link.confidential"));
		confidential.addActionListener(e -> insert(CONFIDENTIAL));
		popup.add(confidential);
		items.add(confidential);
	}

	/**
	 * The keys of the popup, while it is shown: the arrows move from an item to the other, Enter takes the one the arrows are on, Escape closes
	 * the popup (and nothing else). The focus stays in the field, so the user can also go on typing.
	 *
	 * @param event	The key that was pressed.
	 * @return	Whether the popup took the key (the field must not).
	 */
	boolean handleKey(final KeyEvent event){
		if(!popup.isVisible())
			return false;

		switch(event.getKeyCode()){
			case KeyEvent.VK_DOWN -> select(selected + 1);
			case KeyEvent.VK_UP -> select(selected - 1);
			case KeyEvent.VK_ENTER -> items.get(selected)
				.doClick(0);
			case KeyEvent.VK_ESCAPE -> popup.setVisible(false);
			default -> {
				return false;
			}
		}
		event.consume();
		return true;
	}

	/** Puts the arrows on an item of the popup (after the last comes the first). */
	private void select(final int index){
		selected = Math.floorMod(index, items.size());
		MenuSelectionManager.defaultManager()
			.setSelectedPath(new MenuElement[]{popup, items.get(selected)});
	}

	/**
	 * Makes the field offer the links when the user types {@code flef:}.
	 *
	 * @param field	The text field (a text where the links of a note can be written).
	 * @param parent	The window the dialog to choose the record is shown from.
	 * @param model	The model whose records can be linked.
	 * @return	The assistant of the field.
	 */
	public static FlefLinkAssistant install(final JTextComponent field, final Window parent, final FLEFModel model){
		final FlefLinkAssistant assistant = new FlefLinkAssistant(field, parent, model);
		field.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(final KeyEvent e){
				assistant.handleKey(e);
			}
		});
		field.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					if(assistant.typedPrefix(e))
						SwingUtilities.invokeLater(assistant::showPopup);
					else
						assistant.hidePopup();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					assistant.hidePopup();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					//(a change of style: not of the text)
				}
			});
		return assistant;
	}


	/** @return	Whether the user has just typed the colon that completes the start of a link ({@code [text](flef:}; one character: a pasted text offers nothing). */
	boolean typedPrefix(final DocumentEvent event){
		return isLinkTyped(event.getDocument(), event.getOffset(), event.getLength());
	}

	/**
	 * @param document	The text.
	 * @param offset	Where the text was inserted.
	 * @param length	How long the inserted text is.
	 * @return	Whether the insertion is the colon that completes {@code flef:} in the well formed start of a link: an opening bracket, a text (that
	 * 	has no brackets) and then {@code ](flef:}.
	 */
	static boolean isLinkTyped(final Document document, final int offset, final int length){
		final int start = LINK_OPEN.length() + PREFIX.length() - 1;
		if(length != 1 || offset < start)
			return false;

		try{
			if(!(LINK_OPEN + PREFIX).equals(document.getText(offset - start, start + 1)))
				return false;

			//the text of the link: from the last opening bracket before it, with no bracket inside and something in it
			final String before = document.getText(0, offset - start);
			final int open = before.lastIndexOf('[');
			if(open < 0)
				return false;

			final String text = before.substring(open + 1);
			return (!text.isBlank() && text.indexOf(']') < 0);
		}
		catch(final BadLocationException e){
			return false;
		}
	}

	void showPopup(){
		if(!field.isShowing())
			return;

		try{
			final Rectangle2D caret = field.modelToView2D(field.getCaretPosition());
			if(caret != null){
				popup.show(field, (int)caret.getX(), (int)(caret.getY() + caret.getHeight()));
				//(the first item is the one Enter takes, until the arrows say another)
				select(0);
			}
		}
		catch(final BadLocationException ignored){
			//the text changed meanwhile: nothing to offer
		}
		//(the user goes on typing in the field, not in the menu)
		field.requestFocusInWindow();
	}

	private void hidePopup(){
		if(popup.isVisible())
			SwingUtilities.invokeLater(() -> popup.setVisible(false));
	}

	private void chooseRecord(){
		popup.setVisible(false);

		final FindRecordDialog.Found found = FindRecordDialog.showAndGet(parent, model);
		if(found != null && found.record().getId() != null)
			insert(found.record().getId());
		else
			field.requestFocusInWindow();
	}

	/** Writes what follows {@code flef:} at the caret, and closes the link if it is open and has nothing that closes it. */
	void insert(final String target){
		popup.setVisible(false);

		try{
			final Document document = field.getDocument();
			final int caret = field.getCaretPosition();
			final boolean opens = (caret >= PREFIX.length() + LINK_OPEN.length()
				&& LINK_OPEN.equals(document.getText(caret - PREFIX.length() - LINK_OPEN.length(), LINK_OPEN.length())));
			final boolean closed = (caret < document.getLength() && document.getText(caret, 1).charAt(0) == CLOSE);
			final String text = target + (opens && !closed? String.valueOf(CLOSE): StringUtils.EMPTY);
			document.insertString(caret, text, null);
			field.setCaretPosition(caret + text.length());
		}
		catch(final BadLocationException ignored){
			//the text changed meanwhile: nothing is written
		}
		field.requestFocusInWindow();
	}

}
