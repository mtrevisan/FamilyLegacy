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
package io.github.mtrevisan.familylegacy.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Filter panel for Individual records: event type, date range, location.
 */
public class IndividualFilterPanel extends JPanel implements RecordFilterPanel{

	private final JComboBox<String> sexCombo = new JComboBox<>(GUIHelper.fillCombo(IndividualReader.SEXES, I18N.t("search.combo.any")));
	private final JComboBox<String> eventTypeCombo = new JComboBox<>(GUIHelper.fillCombo(EventReader.TYPES, I18N.t("search.combo.any")));
	private final JTextField dateFromField = new JTextField(10);
	private final JComboBox<String> calendarFromCombo = new JComboBox<>(DateReader.CALENDARS);
	private final JTextField dateToField = new JTextField(10);
	private final JComboBox<String> calendarToCombo = new JComboBox<>(DateReader.CALENDARS);
	private final JTextField placeField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public IndividualFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.event"))));

		add(new JLabel(I18N.t("dialog.individual.sex") + ":"));
		add(sexCombo, "growx");
		add(new JLabel(I18N.t("dialog.event.type.extended") + ":"));
		add(eventTypeCombo, "growx");
		add(new JLabel(I18N.t("search.date.from") + ":"));
		add(dateFromField, "growx");
		add(new JLabel(I18N.t("search.date.calendar.from") + ":"));
		add(calendarFromCombo, "growx");
		add(new JLabel(I18N.t("search.date.to") + ":"));
		add(dateToField, "growx");
		add(new JLabel(I18N.t("search.date.calendar.to") + ":"));
		add(calendarToCombo, "growx");
		add(new JLabel(I18N.t("search.place") + ":"));
		add(placeField, "growx");
	}

	private void setupListeners(){
		sexCombo.addActionListener(e -> fireChanged());
		eventTypeCombo.addActionListener(e -> fireChanged());


		final DocumentListener docListener = new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				fireChanged();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				fireChanged();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				fireChanged();
			}
		};

		dateFromField.getDocument()
			.addDocumentListener(docListener);
		calendarFromCombo.addActionListener(e -> fireChanged());
		dateToField.getDocument()
			.addDocumentListener(docListener);
		calendarToCombo.addActionListener(e -> fireChanged());
		placeField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			// The actual criteria update is handled by the parent dialog.
			// We just notify that something changed.
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(IndividualReader.TAG_SEX, getSex());
		filters.put(EventReader.TAG_TYPE, getEventType());
		filters.put(IndividualSearchStrategy.KEY_DATE_FROM, getEventDateFrom());
		filters.put(IndividualSearchStrategy.KEY_CALENDAR_FROM, getEventCalendarFrom());
		filters.put(IndividualSearchStrategy.KEY_DATE_TO, getEventDateTo());
		filters.put(IndividualSearchStrategy.KEY_CALENDAR_TO, getEventCalendarTo());
		filters.put(EventReader.TAG_PLACE, getEventPlace());
		return filters;
	}

	// Getters for the parent to read values
	public String getSex(){
		return (sexCombo.getSelectedIndex() == 0? null: (String)sexCombo.getSelectedItem());
	}

	public String getEventType(){
		return (eventTypeCombo.getSelectedIndex() == 0? null: (String)eventTypeCombo.getSelectedItem());
	}

	public String getEventDateFrom(){
		return dateFromField.getText()
			.trim();
	}

	public String getEventCalendarFrom(){
		return (String)calendarFromCombo.getSelectedItem();
	}

	public String getEventDateTo(){
		return dateToField.getText()
			.trim();
	}

	public String getEventCalendarTo(){
		return (String)calendarToCombo.getSelectedItem();
	}

	public String getEventPlace(){
		return placeField.getText()
			.trim();
	}

}
