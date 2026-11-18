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
package io.github.mtrevisan.familylegacy.ui.components.searches;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.search.SearchMatcher;
import io.github.mtrevisan.familylegacy.search.SearchMode;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


/**
 * The search field of the lists and the boards, with the same powers of the record selection dialog: the text is matched word by word
 * ({@link SearchMatcher}), and the check boxes narrow it to whole words and widen it to typos (fuzzy); where the notes and the
 * citations of a record can be searched too, a third one tells so.
 * <p>
 * The listeners are told once the user has stopped typing, so a long list is not filtered at every key.
 */
public final class SearchBox extends JPanel{

	private static final int DELAY_MS = 250;


	private final JTextField field;
	private final JCheckBox wholeWord = new JCheckBox(I18N.t("search.mode.whole_word"), false);
	private final JCheckBox fuzzy = new JCheckBox(I18N.t("search.mode.fuzzy"), false);
	private final JCheckBox notes;
	private final List<Runnable> listeners = new ArrayList<>();
	private final Timer timer = new Timer(DELAY_MS, e -> fire());


	/**
	 * @param columns	The width of the text field.
	 * @param withNotes	Whether the search can include the notes and the citations (the text of the record is not all there is to find).
	 */
	public SearchBox(final int columns, final boolean withNotes){
		super(new FlowLayout(FlowLayout.LEFT, 6, 0));

		field = new JTextField(columns);
		notes = (withNotes? new JCheckBox(I18N.t("search.mode.notes_and_citations"), false): null);
		timer.setRepeats(false);
		field.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					timer.restart();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					timer.restart();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					timer.restart();
				}
			});

		add(field);
		add(wholeWord);
		add(fuzzy);
		wholeWord.addActionListener(e -> fire());
		fuzzy.addActionListener(e -> fire());
		if(notes != null){
			add(notes);
			notes.addActionListener(e -> fire());
		}
		setOpaque(false);
	}


	/** Tells when the text or a mode changes (once the user has stopped typing). */
	public void onChange(final Runnable listener){
		listeners.add(listener);
	}

	private void fire(){
		for(final Runnable listener : listeners)
			listener.run();
	}

	public JTextField field(){
		return field;
	}

	/** @return	What the user typed, without the spaces around it. */
	public String query(){
		return field.getText().strip();
	}

	public boolean isBlank(){
		return query().isEmpty();
	}

	public Set<SearchMode> modes(){
		final Set<SearchMode> modes = EnumSet.noneOf(SearchMode.class);
		if(fuzzy.isSelected())
			modes.add(SearchMode.FUZZY);
		if(wholeWord.isSelected())
			modes.add(SearchMode.WHOLE_WORD);
		if(notes != null && notes.isSelected())
			modes.add(SearchMode.NOTES_AND_CITATIONS);
		return modes;
	}

	/**
	 * @param fields	The texts of a row (the fields are matched together, so the words of the query can be in different ones).
	 * @return	Whether the row matches the query (always, if the query is blank).
	 */
	public boolean matches(final String... fields){
		return matches(query(), modes(), fields);
	}

	/** The same for a query and some modes that are already read (to match many rows off the event dispatch thread). */
	public static boolean matches(final String query, final Set<SearchMode> modes, final String... fields){
		if(query.isEmpty())
			return true;

		final StringBuilder target = new StringBuilder();
		for(final String field : fields){
			if(field == null)
				continue;

			if(!target.isEmpty())
				target.append(' ');
			target.append(field);
		}
		return SearchMatcher.score(query, target.toString(), modes).passes();
	}

	/** @return	A test for a text (for the trees that prune what does not match), reading the query and the modes now. */
	public Predicate<String> predicate(){
		final String query = query();
		final Set<SearchMode> modes = modes();
		return text -> matches(query, modes, text);
	}

}
