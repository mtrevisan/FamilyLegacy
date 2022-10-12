/**
 * Copyright (c) 2022 Mauro Trevisan
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

import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TagPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Predicate;


public class EventsPanel extends JPanel{

	/**
	 * [ms]
	 */
	private static final int DEBOUNCER_TIME = 400;

	private final Debouncer<EventsPanel> filterDebouncer = new Debouncer<>(this::filterEventBy, DEBOUNCER_TIME);

	private final JTextField eventField = new JTextField();
	private final JButton eventAddButton = new JButton("Add");
	private final TagPanel tagPanel = new TagPanel();

	private final Predicate<String> eventExists;
	private volatile String formerFilterEvent;


	public EventsPanel(final Predicate<String> eventExists){
		super(new MigLayout(StringUtils.EMPTY, "[grow]"));

		this.eventExists = eventExists;

		final JLabel eventLabel = new JLabel("Event(s):");
		eventLabel.setLabelFor(eventField);
		eventField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(EventsPanel.this);
			}
		});
		eventAddButton.setEnabled(false);
		eventAddButton.addActionListener(this::eventAddButtonAction);
		final JScrollPane eventScrollPane = new JScrollPane();
		eventScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
		eventScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		eventScrollPane.setViewportView(tagPanel);

		setBorder(BorderFactory.createTitledBorder("Events"));
		add(eventLabel, "align label,split 3");
		add(eventField, "grow");
		add(eventAddButton, "wrap");
		add(eventScrollPane, "grow,height 46");
	}

	private void filterEventBy(final EventsPanel dialog){
		final String newEvent = eventField.getText().trim();
		if(newEvent.equals(formerFilterEvent))
			return;

		formerFilterEvent = newEvent;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = eventExists.test(newEvent);
		eventAddButton.setEnabled(StringUtils.isNotBlank(newEvent) && !alreadyContained);


		tagPanel.applyFilter(StringUtils.isNotBlank(newEvent)? newEvent: null);
	}

	private void eventAddButtonAction(final ActionEvent evt){
		final String newEvent = eventField.getText().trim();
		final boolean containsEvent = eventExists.test(newEvent);
		if(!containsEvent){
			tagPanel.addTag(newEvent);

			//reset input
			eventField.setText(null);
			tagPanel.applyFilter(null);
		}
		else
			JOptionPane.showOptionDialog(this, "This event is already present", "Warning!",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
	}

	public void addTag(final String... tags){
		tagPanel.addTag(tags);
	}

	public void addTag(final Iterable<String> tags){
		tagPanel.addTag(tags);
	}

	public List<String> getTags(){
		return tagPanel.getTags();
	}

}