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
package io.github.mtrevisan.familylegacy.v2.ui.binding;

import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;


/**
 * An editable JComboBox that dynamically filters items based on text input.
 *
 * @param <E>	The type of the elements in this combo box.
 */
public class FilteredComboBox<E> extends JComboBox<E>{

	private final List<E> originalItems = new ArrayList<>();
	private final DefaultComboBoxModel<E> model;
	private JTextComponent editorComponent;
	private boolean isFiltering;


	public FilteredComboBox(){
		this(Collections.emptyList());
	}

	public FilteredComboBox(final List<E> items){
		super();

		this.model = new DefaultComboBoxModel<>();
		setModel(this.model);
		setEditable(true);

		setupEditorListener();

		if(items != null)
			setItems(items);


		// Find the element with the longest string and use it as a prototype
		if(!originalItems.isEmpty()){
			final E longest = originalItems.stream()
				.max(Comparator.comparingInt(e -> e.toString().length()))
				.orElse(null);
			setPrototypeDisplayValue(longest);
		}
	}


	/**
	 * Updates the full master list of items and resets the filter.
	 *
	 * @param items	The new list of items.
	 */
	public void setItems(final List<E> items){
		this.originalItems.clear();
		if(items != null)
			this.originalItems.addAll(items);

		refilter(getText());
	}

	private void setupEditorListener(){
		this.editorComponent = (JTextComponent)getEditor().getEditorComponent();

		// Listen for all text modifications (typing, pasting, backspace, delete)
		this.editorComponent.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				onTextChanged();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				onTextChanged();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				onTextChanged();
			}
		});

		// Hide popup on Enter or Escape key presses
		this.editorComponent.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(final KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE)
					hidePopup();
			}
		});
	}

	private void onTextChanged(){
		if(isFiltering)
			return;

		SwingUtilities.invokeLater(() -> {
			final String currentText = getText();
			refilter(currentText);
		});
	}

	private synchronized void refilter(final String textToMatch){
		if(editorComponent == null)
			return;

		isFiltering = true;

		final int caretPosition = editorComponent.getCaretPosition();
		final String searchText = (textToMatch == null? StringUtils.EMPTY: textToMatch.trim().toLowerCase());

		model.removeAllElements();

		if(searchText.isEmpty()){
			// Restore all items if text is empty
			for(final E item : originalItems)
				model.addElement(item);

			hidePopup();
		}
		else{
			// Filter items based on containment (case-insensitive)
			int matchCount = 0;
			for(final E item : originalItems)
				if(item != null && item.toString().toLowerCase().contains(searchText)){
					model.addElement(item);
					matchCount ++;
				}

			if(matchCount <= 0)
				hidePopup();
			else if(!isPopupVisible())
				showPopup();
		}

		// Restore editor text and cursor position without resetting user input
		editorComponent.setText(textToMatch);
		editorComponent.setCaretPosition(Math.min(caretPosition, editorComponent.getText().length()));

		isFiltering = false;
	}

	private String getText(){
		return (editorComponent != null? editorComponent.getText(): StringUtils.EMPTY);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		HandlerRegistry.scanHandlers();

		// Extract BCP 47 language tags (e.g. "en-US - English (United States)")
		final TreeSet<String> languageCodes = new TreeSet<>(Comparator.naturalOrder());
		for(final Locale locale : Locale.getAvailableLocales()){
			final String tag = locale.toLanguageTag();
			final String displayName = locale.getDisplayName(Locale.ENGLISH);
			if(!tag.equals("und") && !tag.isBlank()){
				if(!displayName.isBlank())
					languageCodes.add(tag + " - " + displayName);
				else
					languageCodes.add(tag);
			}
		}

		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame("Filtered Language Codes Demo");
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setSize(450, 150);
			frame.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 30));

			final FilteredComboBox<String> comboBox = new FilteredComboBox<>(new ArrayList<>(languageCodes));

			frame.add(comboBox);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
