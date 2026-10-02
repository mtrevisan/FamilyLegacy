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

import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
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
 * Filter panel for ResearchActivity records: activity type, status, action, result, and observation.
 */
public class ResearchActivityFilterPanel extends JPanel implements RecordFilterPanel{

	private final JComboBox<String> activityTypeCombo = new JComboBox<>(GUIHelper.fillCombo(ResearchActivityReader.TYPES, I18N.t("search.combo.any")));
	private final JComboBox<String> statusCombo = new JComboBox<>(GUIHelper.fillCombo(ResearchActivityReader.STATUSES, I18N.t("search.combo.any")));
	private final JTextField actionField = new JTextField(20);
	private final JComboBox<String> resultCombo = new JComboBox<>(GUIHelper.fillCombo(ResearchActivityReader.RESULTS, I18N.t("search.combo.any")));
	private final JTextField observationField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public ResearchActivityFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.research.activity"))));

		add(new JLabel(I18N.t("dialog.research.activity.type") + ":"));
		add(activityTypeCombo, "growx");
		add(new JLabel(I18N.t("dialog.research.activity.status") + ":"));
		add(statusCombo, "growx");
		add(new JLabel(I18N.t("dialog.research.activity.action") + ":"));
		add(actionField, "growx");
		add(new JLabel(I18N.t("dialog.research.activity.result") + ":"));
		add(resultCombo, "growx");
		add(new JLabel(I18N.t("dialog.research.activity.observation") + ":"));
		add(observationField, "growx");
	}

	private void setupListeners(){
		activityTypeCombo.addActionListener(e -> fireChanged());
		statusCombo.addActionListener(e -> fireChanged());
		resultCombo.addActionListener(e -> fireChanged());

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

		actionField.getDocument()
			.addDocumentListener(docListener);
		observationField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(ResearchActivityReader.TAG_ACTIVITY_TYPE, getActivityType());
		filters.put(ResearchActivityReader.TAG_STATUS, getStatus());
		filters.put(ResearchActivityReader.TAG_ACTION, getAction());
		filters.put(ResearchActivityReader.TAG_RESULT, getResult());
		filters.put(ResearchActivityReader.TAG_OBSERVATION, getObservation());
		return filters;
	}

	public String getActivityType(){
		return (activityTypeCombo.getSelectedIndex() == 0? null: (String)activityTypeCombo.getSelectedItem());
	}

	public String getStatus(){
		return (statusCombo.getSelectedIndex() == 0? null: (String)statusCombo.getSelectedItem());
	}

	public String getAction(){
		return actionField.getText().trim();
	}

	public String getResult(){
		return (resultCombo.getSelectedIndex() == 0? null: (String)resultCombo.getSelectedItem());
	}

	public String getObservation(){
		return observationField.getText().trim();
	}

}
