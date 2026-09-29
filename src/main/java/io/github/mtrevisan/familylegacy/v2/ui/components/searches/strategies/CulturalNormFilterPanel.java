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

import io.github.mtrevisan.familylegacy.v2.ui.components.SingleDatePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
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
 * Filter panel for CulturalNorm records: title, rule type, place, and validity date range.
 */
public class CulturalNormFilterPanel extends JPanel implements RecordFilterPanel{

	private final JTextField titleField = new JTextField(20);
	private final JComboBox<String> ruleTypeCombo = new JComboBox<>(GUIHelper.fillCombo(CulturalNormHandler.RULE_TYPES, I18N.t("search.combo.any")));
	private final JTextField placeField = new JTextField(20);
	private final JTextField dateFromField = new JTextField(10);
	private final JComboBox<String> calendarFromCombo = new JComboBox<>(SingleDatePanel.CALENDARS);
	private final JTextField dateToField = new JTextField(10);
	private final JComboBox<String> calendarToCombo = new JComboBox<>(SingleDatePanel.CALENDARS);

	private final Consumer<SearchCriteria> onChanged;


	public CulturalNormFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.cultural.norm"))));

		add(new JLabel(I18N.t("dialog.cultural.norm.title") + ":"));
		add(titleField, "growx");
		add(new JLabel(I18N.t("dialog.cultural.norm.rule.type") + ":"));
		add(ruleTypeCombo, "growx");
		add(new JLabel(I18N.t("dialog.place") + ":"));
		add(placeField, "growx");
		add(new JLabel(I18N.t("search.date.from") + ":"));
		add(dateFromField, "growx");
		add(new JLabel(I18N.t("search.date.calendar.from") + ":"));
		add(calendarFromCombo, "growx");
		add(new JLabel(I18N.t("search.date.to") + ":"));
		add(dateToField, "growx");
		add(new JLabel(I18N.t("search.date.calendar.to") + ":"));
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
		placeField.getDocument()
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
		filters.put(CulturalNormHandler.TAG_TITLE, getTitle());
		filters.put(CulturalNormHandler.TAG_RULE_TYPE, getRuleType());
		filters.put(PlaceHandler.TYPE, getPlace());
		filters.put(CulturalNormHandler.TAG_VALID_FROM, getValidFrom());
		filters.put(CulturalNormSearchStrategy.KEY_CALENDAR_FROM, getCalendarFrom());
		filters.put(CulturalNormHandler.TAG_VALID_TO, getValidTo());
		filters.put(CulturalNormSearchStrategy.KEY_CALENDAR_TO, getCalendarTo());
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
		return placeField.getText()
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
