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
package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
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
 * Filter panel for Event records: event type, description, date, location, agency, and cause reason.
 */
public class EventFilterPanel extends JPanel implements RecordFilterPanel{

	private final JComboBox<String> typeCombo = new JComboBox<>(GUIHelper.fillCombo(EventReader.TYPES, I18N.t("search.combo.any")));
	private final JTextField descriptionField = new JTextField(20);
	private final JTextField dateField = new JTextField(10);
	private final JComboBox<String> calendarCombo = new JComboBox<>(DateReader.CALENDARS);
	private final JTextField placeField = new JTextField(20);
	private final JTextField agencyField = new JTextField(20);
	private final JTextField causeReasonField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public EventFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.event"))));

		add(new JLabel(I18N.t("dialog.event.type") + ":"));
		add(typeCombo, "growx");
		add(new JLabel(I18N.t("dialog.event.description") + ":"));
		add(descriptionField, "growx");
		add(new JLabel(I18N.t("dialog.event.date") + ":"));
		add(dateField, "growx");
		add(new JLabel(I18N.t("dialog.date.calendar") + ":"));
		add(calendarCombo, "growx");
		add(new JLabel(I18N.t("dialog.place") + ":"));
		add(placeField, "growx");
		add(new JLabel(I18N.t("dialog.event.agency") + ":"));
		add(agencyField, "growx");
		add(new JLabel(I18N.t("dialog.event.cause.reason") + ":"));
		add(causeReasonField, "growx");
	}

	private void setupListeners(){
		typeCombo.addActionListener(e -> fireChanged());

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

		descriptionField.getDocument()
			.addDocumentListener(docListener);
		dateField.getDocument()
			.addDocumentListener(docListener);
		placeField.getDocument()
			.addDocumentListener(docListener);
		agencyField.getDocument()
			.addDocumentListener(docListener);
		causeReasonField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(EventReader.TAG_TYPE, getEventType());
		filters.put(EventReader.TAG_DESCRIPTION, getDescription());
		filters.put(EventReader.TAG_DATE, getDate());
		filters.put(DateReader.TAG_CALENDAR, getCalendar());
		filters.put(EventReader.TAG_PLACE, getPlace());
		filters.put(EventReader.TAG_AGENCY, getAgency());
		filters.put(EventReader.TAG_CAUSE_REASON, getCauseReason());
		return filters;
	}

	public String getEventType(){
		return (typeCombo.getSelectedIndex() == 0? null: (String)typeCombo.getSelectedItem());
	}

	public String getDescription(){
		return descriptionField.getText()
			.trim();
	}

	public String getDate(){
		return dateField.getText()
			.trim();
	}

	public String getCalendar(){
		return (String)calendarCombo.getSelectedItem();
	}

	public String getPlace(){
		return placeField.getText()
			.trim();
	}

	public String getAgency(){
		return agencyField.getText()
			.trim();
	}

	public String getCauseReason(){
		return causeReasonField.getText()
			.trim();
	}

}
