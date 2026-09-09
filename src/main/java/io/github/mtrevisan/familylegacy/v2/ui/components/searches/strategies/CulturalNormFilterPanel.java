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

import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
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
 * Filter panel for CulturalNorm records: title, rule type, location, and validity date range.
 */
public class CulturalNormFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_TITLE = "title";
	static final String FILTER_KEY_RULE_TYPE = "ruleType";
	static final String FILTER_KEY_PLACE = "place";
	static final String FILTER_KEY_VALID_FROM = "validFrom";
	static final String FILTER_KEY_CALENDAR_FROM = "calendarFrom";
	static final String FILTER_KEY_VALID_TO = "validTo";
	static final String FILTER_KEY_CALENDAR_TO = "calendarTo";


	private final JTextField titleField = new JTextField(20);
	private final JComboBox<String> ruleTypeCombo = new JComboBox<>(new String[]{
		"Any",
		"age_of_majority",
		"marriage_minimum_age",
		"baptism_age",
		"confirmation_age",
		"military_service_age",
		"retirement_age",
		"naming_convention",
		"surname_transmission",
		"patronymic_system",
		"matronymic_system",
		"title_usage",
		"inheritance_rule",
		"succession_rule",
		"dowry_practice",
		"guardianship_rule",
		"adoption_practice",
		"marriage_practice",
		"marriage_prohibited_degree",
		"widowhood_rule",
		"residence_pattern",
		"household_structure",
		"social_classification",
		"religious_practice",
		"burial_practice",
		"citizenship_rule",
		"legitimacy_rule",
		"age_difference_convention",
		"generational_interval"
	});
	private final JTextField locationField = new JTextField(20);
	private final JTextField dateFromField = new JTextField(10);
	private final JComboBox<String> calendarFromCombo = new JComboBox<>(new String[]{
		"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic",
		"soviet eternal", "ethiopian", "mayan"});
	private final JTextField dateToField = new JTextField(10);
	private final JComboBox<String> calendarToCombo = new JComboBox<>(new String[]{
		"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic",
		"soviet eternal", "ethiopian", "mayan"});

	private final Consumer<SearchCriteria> onChanged;


	public CulturalNormFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Cultural Norm Filters"));

		add(new JLabel("Title:"));
		add(titleField, "growx");
		add(new JLabel("Rule type:"));
		add(ruleTypeCombo, "growx");
		add(new JLabel("Location:"));
		add(locationField, "growx");
		add(new JLabel("Valid from:"));
		add(dateFromField, "growx");
		add(new JLabel("Calendar from:"));
		add(calendarFromCombo, "growx");
		add(new JLabel("Valid to:"));
		add(dateToField, "growx");
		add(new JLabel("Calendar to:"));
		add(calendarToCombo, "growx");
	}

	private void setupListeners(){
		ruleTypeCombo.addActionListener(e -> fireChanged());

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

		titleField.getDocument()
			.addDocumentListener(docListener);
		locationField.getDocument()
			.addDocumentListener(docListener);
		dateFromField.getDocument()
			.addDocumentListener(docListener);
		dateToField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_TITLE, getTitle());
		filters.put(FILTER_KEY_RULE_TYPE, getRuleType());
		filters.put(FILTER_KEY_PLACE, getPlace());
		filters.put(FILTER_KEY_VALID_FROM, getValidFrom());
		filters.put(FILTER_KEY_CALENDAR_FROM, getCalendarFrom());
		filters.put(FILTER_KEY_VALID_TO, getValidTo());
		filters.put(FILTER_KEY_CALENDAR_TO, getCalendarTo());
		return filters;
	}

	public String getTitle(){
		return titleField.getText()
			.trim();
	}

	public String getRuleType(){
		return (ruleTypeCombo.getSelectedIndex() == 0? null: (String)ruleTypeCombo.getSelectedItem());
	}

	public String getPlace(){
		return locationField.getText()
			.trim();
	}

	public String getValidFrom(){
		return dateFromField.getText()
			.trim();
	}

	public String getCalendarFrom(){
		return (String)calendarFromCombo.getSelectedItem();
	}

	public String getValidTo(){
		return dateToField.getText()
			.trim();
	}

	public String getCalendarTo(){
		return (String)calendarToCombo.getSelectedItem();
	}

}
