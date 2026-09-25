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
package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge.TagSuggester.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Function;


public final class AutocompleteController{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutocompleteController.class);

	private static final int POPUP_WIDTH = 240;
	private static final int POPUP_HEIGHT = 200;


	private enum PopupMode{ TAG, VALUE }


	private final JTextArea area;
	private final TagSuggester suggester;
	private final int indentSize;
	private final Function<JTextArea, String> textFor;
	private final Function<JTextArea, Integer> offsetFor;

	private final JWindow popup;
	private final DefaultListModel<Object> listModel = new DefaultListModel<>();
	private final JList<Object> list = new JList<>(listModel);

	private boolean popupVisible;
	private PopupMode popupMode;
	private boolean suppressNextDocumentEvent;

	private KeyEventDispatcher popupKeyDispatcher;
	private final KeyEventDispatcher triggerDispatcher;


	public AutocompleteController(final JTextArea area, final Window owner,
		final TagSuggester suggester, final int indentSize,
		final Function<JTextArea, String> textFor,
		final Function<JTextArea, Integer> offsetFor){
		this.area = area;
		this.suggester = suggester;
		this.indentSize = Math.max(1, indentSize);
		this.textFor = textFor;
		this.offsetFor = offsetFor;

		this.popup = new JWindow(owner);
		this.popup.setFocusableWindowState(false);

		this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.list.setFont(area.getFont());
		this.list.setCellRenderer(new SuggestionRenderer());
		this.list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				acceptSelected();
			}
		});

		final JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createLineBorder(new Color(0xB0B0B0)));
		popup.setLayout(new BorderLayout());
		popup.add(scroll, BorderLayout.CENTER);
		popup.setSize(POPUP_WIDTH, POPUP_HEIGHT);

		triggerDispatcher = this::handleTriggerKey;
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(triggerDispatcher);

		installDocumentListener();
		installSmartEnter(area, this.indentSize);
		installTabAsSpaces(area, this.indentSize);
	}


	/* ======================================================================
	 *                          Trigger
	 * ====================================================================== */

	private void installDocumentListener(){
		area.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ onDocumentChanged(); }
			@Override public void removeUpdate(final DocumentEvent e){ onDocumentChanged(); }
			@Override public void changedUpdate(final DocumentEvent e){ /* plain text */ }
		});
	}

	private void onDocumentChanged(){
		if(suppressNextDocumentEvent){
			suppressNextDocumentEvent = false;
			return;
		}

		SwingUtilities.invokeLater(this::refreshOrAutoTrigger);
	}

	private void refreshOrAutoTrigger(){
		if(popupVisible){
			refreshOrClose();
			return;
		}
		maybeAutoTriggerValue();
	}

	private void maybeAutoTriggerValue(){
		final String fullText = textFor.apply(area);
		final int caret = caretInFullText();

		if(!TagSuggester.isValuePosition(fullText, caret))
			return;

		final List<String> values = suggester.valueSuggestionsFor(fullText, caret);
		if(values.isEmpty())
			return;

		LOGGER.info("Autocomplete: auto-triggered {} value suggestions", values.size());
		showValuePopup(values);
	}

	private boolean handleTriggerKey(final KeyEvent e){
		if(e.getID() != KeyEvent.KEY_PRESSED)
			return false;

		final int mods = e.getModifiersEx();
		final boolean ctrl = (mods & InputEvent.CTRL_DOWN_MASK) != 0;
		final boolean other = (mods & (InputEvent.SHIFT_DOWN_MASK
			| InputEvent.ALT_DOWN_MASK)) != 0;
		if(!ctrl || other || e.getKeyCode() != KeyEvent.VK_SPACE)
			return false;

		final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getFocusOwner();
		if(owner != area)
			return false;

		openPopup();
		return true;
	}


	/* ======================================================================
	 *                          Context helpers
	 * ====================================================================== */

	private int caretInFullText(){
		final Integer offset = offsetFor.apply(area);
		return (offset != null? offset: 0) + area.getCaretPosition();
	}


	/* ======================================================================
	 *                          Popup lifecycle
	 * ====================================================================== */

	private void openPopup(){
		final String fullText = textFor.apply(area);
		final int caret = caretInFullText();

		if(TagSuggester.isValuePosition(fullText, caret)){
			final List<String> values = suggester.valueSuggestionsFor(fullText, caret);
			if(values.isEmpty()){
				LOGGER.info("Autocomplete: no value suggestions");
				return;
			}
			LOGGER.info("Autocomplete: {} value suggestions", values.size());
			showValuePopup(values);
			return;
		}

		final List<Suggestion> suggestions = suggester.suggestionsFor(fullText, caret);
		if(suggestions.isEmpty()){
			LOGGER.info("Autocomplete: no tag suggestions");
			return;
		}
		LOGGER.info("Autocomplete: {} tag suggestions", suggestions.size());
		showTagPopup(suggestions);
	}

	private void refreshOrClose(){
		final String fullText = textFor.apply(area);
		final int caret = caretInFullText();
		final boolean atValuePosition = TagSuggester.isValuePosition(fullText, caret);

		if(popupMode == PopupMode.VALUE){
			if(!atValuePosition){
				closePopup();
				return;
			}
			final List<String> values = suggester.valueSuggestionsFor(fullText, caret);
			if(values.isEmpty())
				closePopup();
			else
				showValuePopup(values);
			return;
		}

		if(popupMode == PopupMode.TAG){
			if(atValuePosition){
				closePopup();
				return;
			}
			final List<Suggestion> suggestions = suggester.suggestionsFor(fullText, caret);
			if(suggestions.isEmpty())
				closePopup();
			else
				showTagPopup(suggestions);
		}
	}

	private void showTagPopup(final List<Suggestion> suggestions){
		listModel.clear();
		for(final Suggestion s : suggestions)
			listModel.addElement(s);
		list.setSelectedIndex(0);
		popupMode = PopupMode.TAG;
		showOrRefreshPopup();
	}

	private void showValuePopup(final List<String> values){
		listModel.clear();
		for(final String v : values)
			listModel.addElement(new ValueItem(v));
		list.setSelectedIndex(0);
		popupMode = PopupMode.VALUE;
		showOrRefreshPopup();
	}

	private void showOrRefreshPopup(){
		if(popupVisible){
			repositionPopup();
			return;
		}

		repositionPopup();
		popup.setVisible(true);
		popupVisible = true;

		if(popupKeyDispatcher == null){
			popupKeyDispatcher = this::handlePopupKey;
			KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addKeyEventDispatcher(popupKeyDispatcher);
		}

		SwingUtilities.invokeLater(area::requestFocusInWindow);
	}

	private void repositionPopup(){
		final Rectangle caretRect = caretRect(area, area.getCaretPosition());
		if(caretRect == null)
			return;

		final Point loc = new Point(caretRect.x, caretRect.y + caretRect.height);
		SwingUtilities.convertPointToScreen(loc, area);

		final int screenH = popup.getToolkit().getScreenSize().height;
		final int y = (loc.y + POPUP_HEIGHT > screenH
			? loc.y - POPUP_HEIGHT - caretRect.height
			: loc.y);
		popup.setLocation(loc.x, y);
	}

	private void closePopup(){
		if(!popupVisible)
			return;
		popup.setVisible(false);
		popupVisible = false;
		popupMode = null;
		if(popupKeyDispatcher != null){
			KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.removeKeyEventDispatcher(popupKeyDispatcher);
			popupKeyDispatcher = null;
		}
	}


	/* ======================================================================
	 *                          Popup key handling
	 * ====================================================================== */

	private boolean handlePopupKey(final KeyEvent e){
		if(!popupVisible || e.getID() != KeyEvent.KEY_PRESSED)
			return false;

		switch(e.getKeyCode()){
			case KeyEvent.VK_UP -> {
				final int idx = list.getSelectedIndex();
				if(idx > 0)
					list.setSelectedIndex(idx - 1);
				return true;
			}
			case KeyEvent.VK_DOWN -> {
				final int idx = list.getSelectedIndex();
				if(idx < listModel.size() - 1)
					list.setSelectedIndex(idx + 1);
				return true;
			}
			case KeyEvent.VK_ENTER, KeyEvent.VK_TAB -> {
				acceptSelected();
				return true;
			}
			case KeyEvent.VK_ESCAPE -> {
				closePopup();
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private void acceptSelected(){
		final Object selected = list.getSelectedValue();
		closePopup();
		if(selected instanceof Suggestion s)
			insertSuggestion(s);
		else if(selected instanceof ValueItem v)
			insertValue(v.value());
	}


	/* ======================================================================
	 *                          Insertion
	 * ====================================================================== */

	private void insertSuggestion(final Suggestion s){
		final String fullText = textFor.apply(area);
		final int caretInFull = caretInFullText();
		final TagSuggester.Context ctx = suggester.resolveContext(fullText, caretInFull);
		final String prefix = ctx.prefix();
		final String indent = ctx.indent();

		final int caret = area.getCaretPosition();
		final int start = caret - prefix.length();

		final String insertion;
		final int caretOffset;

		if(s.block()){
			final String childIndent = indent + " ".repeat(indentSize);
			final StringBuilder sb = new StringBuilder();
			sb.append(s.tag()).append(" {\n");

			if(s.prefill() != null){
				sb.append(childIndent).append(s.prefill()).append(" \n");
				final int bodyStart = (s.tag() + " {\n").length();
				caretOffset = bodyStart + childIndent.length()
					+ s.prefill().length() + 1;
			}
			else{
				sb.append(childIndent).append('\n');
				final int bodyStart = (s.tag() + " {\n").length();
				caretOffset = bodyStart + childIndent.length();
			}

			sb.append(indent).append('}');
			insertion = sb.toString();
		}
		else{
			insertion = s.tag() + " ";
			caretOffset = insertion.length();
		}

		try{
			area.getDocument().remove(start, prefix.length());
			area.getDocument().insertString(start, insertion, null);
			suppressNextDocumentEvent = true;
			area.setCaretPosition(start + caretOffset);

			// If the inserted tag is a leaf, the caret is now at a value
			// position. When the field expects a value with a known set
			// of options, open the value popup right away, so accepting
			// a tag leads seamlessly to accepting its value.
			if(!s.block())
				SwingUtilities.invokeLater(this::maybeAutoTriggerValue);
		}
		catch(final BadLocationException ex){
			LOGGER.debug("Autocomplete insertion failed: {}", ex.getMessage());
		}
	}

	private void insertValue(final String value){
		final String fullText = textFor.apply(area);
		final int caretInFull = caretInFullText();
		final TagSuggester.Context ctx = suggester.resolveContext(fullText, caretInFull);
		final String prefix = ctx.prefix();

		final int c = area.getCaretPosition();
		final int start = c - prefix.length();
		try{
			area.getDocument().remove(start, prefix.length());
			area.getDocument().insertString(start, value, null);
			suppressNextDocumentEvent = true;
			area.setCaretPosition(start + value.length());
		}
		catch(final BadLocationException ex){
			LOGGER.debug("Value insertion failed: {}", ex.getMessage());
		}
	}


	/* ======================================================================
	 *                          Smart Enter
	 * ====================================================================== */

	/**
	 * Installs the smart-Enter behavior on the given text area: pressing
	 * Enter inserts a newline followed by an indent computed from the
	 * current line's context.
	 * <p>
	 * The rules are the usual ones of a structured editor:
	 * <ul>
	 *   <li>the caret is at the end of a line that ends with {@code {}:
	 *       the closing brace is added on its own line, the body line
	 *       is indented one level deeper, and the caret goes on the
	 *       body line;</li>
	 *   <li>a closing brace is on the same line right after the caret:
	 *       it is moved to its own line, the body line is inserted in
	 *       between;</li>
	 *   <li>the caret is at the end of a line whose only content is
	 *       {@code }}: the new line is indented one level shallower;</li>
	 *   <li>any other case: the new line keeps the same indent as the
	 *       current line.</li>
	 * </ul>
	 *
	 * @param area       the text area to enhance
	 * @param indentSize the number of spaces per indentation level
	 */
	static void installSmartEnter(final JTextArea area, final int indentSize){
		final String actionKey = "merge-smart-enter";
		area.getInputMap()
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), actionKey);
		area.getActionMap()
			.put(actionKey, new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					insertSmartNewline(area, indentSize);
				}
			});
	}

	private static void insertSmartNewline(final JTextArea area, final int indentSize){
		try{
			final int caret = area.getCaretPosition();
			final String text = area.getText();

			int lineStart = caret;
			while(lineStart > 0 && text.charAt(lineStart - 1) != '\n')
				lineStart --;
			int indentEnd = lineStart;
			while(indentEnd < caret
				&& (text.charAt(indentEnd) == ' ' || text.charAt(indentEnd) == '\t'))
				indentEnd ++;
			final String currentIndent = text.substring(lineStart, indentEnd);

			final String beforeCaret = text.substring(indentEnd, caret);
			final String content = beforeCaret.trim();

			// Look ahead past any whitespace on the current line, to see
			// whether a closing brace is already there.
			int lookahead = caret;
			while(lookahead < text.length()
				&& (text.charAt(lookahead) == ' ' || text.charAt(lookahead) == '\t'))
				lookahead ++;
			final boolean closeBraceAhead = (lookahead < text.length()
				&& text.charAt(lookahead) == '}');

			final String innerIndent = currentIndent + " ".repeat(indentSize);

			if(content.endsWith("{") && !closeBraceAhead){
				final String insertion = "\n" + innerIndent + "\n" + currentIndent + "}";
				area.getDocument().insertString(caret, insertion, null);
				area.setCaretPosition(caret + 1 + innerIndent.length());
			}
			else if(closeBraceAhead){
				if(lookahead > caret)
					area.getDocument().remove(caret, lookahead - caret);
				final String insertion = "\n" + innerIndent + "\n" + currentIndent;
				area.getDocument().insertString(caret, insertion, null);
				area.setCaretPosition(caret + 1 + innerIndent.length());
			}
			else if(content.equals("}")){
				final String outerIndent = currentIndent.substring(0,
					Math.max(0, currentIndent.length() - indentSize));
				area.getDocument().insertString(caret, "\n" + outerIndent, null);
				area.setCaretPosition(caret + 1 + outerIndent.length());
			}
			else{
				area.getDocument().insertString(caret, "\n" + currentIndent, null);
				area.setCaretPosition(caret + 1 + currentIndent.length());
			}
		}
		catch(final BadLocationException ex){
			// The caret is always inside the document.
		}
	}


	/* ======================================================================
	 *                          Tab as spaces
	 * ====================================================================== */

	/**
	 * Installs the Tab / Shift+Tab bindings on the given text area.
	 * <p>
	 * Tab inserts a fixed number of spaces (the dialog's indent unit),
	 * never a tab character. When the selection spans more than one
	 * line, every line in the selection is indented; Shift+Tab performs
	 * the reverse, removing up to {@code indentSize} leading spaces from
	 * every line in the selection.
	 *
	 * @param area       the text area to enhance
	 * @param indentSize the number of spaces per indentation level
	 */
	static void installTabAsSpaces(final JTextArea area, final int indentSize){
		final int unit = Math.max(1, indentSize);

		area.getInputMap()
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "merge-indent");
		area.getActionMap()
			.put("merge-indent", new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					indentSelection(area, unit, true);
				}
			});

		area.getInputMap()
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK),
				"merge-deindent");
		area.getActionMap()
			.put("merge-deindent", new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					indentSelection(area, unit, false);
				}
			});
	}

	private static void indentSelection(final JTextArea area, final int unit, final boolean indent){
		final int selStart = area.getSelectionStart();
		final int selEnd = area.getSelectionEnd();

		try{
			if(selStart == selEnd || sameLine(area, selStart, selEnd)){
				if(indent){
					area.getDocument().insertString(selStart, " ".repeat(unit), null);
					area.setCaretPosition(selStart + unit);
				}
				else{
					int removed = 0;
					while(removed < unit && selStart - removed > 0
						&& area.getText(selStart - removed - 1, 1).equals(" "))
						removed ++;
					if(removed > 0){
						area.getDocument().remove(selStart - removed, removed);
						area.setCaretPosition(selStart - removed);
					}
				}
				return;
			}

			final String text = area.getText();
			final int firstLineStart = text.lastIndexOf('\n', selStart - 1) + 1;
			final int lastLineEnd = (text.indexOf('\n', selEnd) < 0
				? text.length()
				: text.indexOf('\n', selEnd));

			final String block = text.substring(firstLineStart, lastLineEnd);
			final String[] lines = block.split("\n", -1);

			final StringBuilder out = new StringBuilder();
			for(int i = 0; i < lines.length; i ++){
				if(i > 0)
					out.append('\n');
				if(indent)
					out.append(" ".repeat(unit)).append(lines[i]);
				else{
					int removed = 0;
					while(removed < unit && removed < lines[i].length()
						&& lines[i].charAt(removed) == ' ')
						removed ++;
					out.append(lines[i].substring(removed));
				}
			}

			area.getDocument().remove(firstLineStart, lastLineEnd - firstLineStart);
			area.getDocument().insertString(firstLineStart, out.toString(), null);
			area.setSelectionStart(firstLineStart);
			area.setSelectionEnd(firstLineStart + out.length());
		}
		catch(final BadLocationException ex){
			// The offsets are always valid.
		}
	}

	private static boolean sameLine(final JTextArea area, final int a, final int b){
		final int newlineAfterA = area.getText().indexOf('\n', a);
		return newlineAfterA < 0 || newlineAfterA >= b;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static Rectangle caretRect(final JTextArea area, final int caret){
		try{
			final Rectangle2D r = area.modelToView2D(caret);
			if(r == null)
				return null;
			return new Rectangle((int)r.getX(), (int)r.getY(),
				(int)r.getWidth(), (int)r.getHeight());
		}
		catch(final BadLocationException ex){
			return null;
		}
	}

	private record ValueItem(String value){}


	private static final class SuggestionRenderer extends DefaultListCellRenderer{

		private static final Color COLOR_SUFFIX = new Color(0x888888);

		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object value,
			final int index, final boolean isSelected, final boolean cellHasFocus){
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if(value instanceof Suggestion s){
				final String suffix = s.cardinality().suffix();
				final String base = (s.block()? s.tag() + "  {…}": s.tag());
				if(suffix.isEmpty())
					setText(base);
				else
					setText("<html>" + escapeHtml(base)
						+ " <font color='#" + hex(COLOR_SUFFIX) + "'>"
						+ suffix + "</font></html>");
				setFont(list.getFont().deriveFont(s.block()? Font.BOLD: Font.PLAIN));
			}
			else if(value instanceof ValueItem v){
				setText(v.value());
				setFont(list.getFont().deriveFont(Font.PLAIN));
			}
			return this;
		}

		private static String escapeHtml(final String s){
			return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}

		private static String hex(final Color c){
			return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
		}
	}

}
