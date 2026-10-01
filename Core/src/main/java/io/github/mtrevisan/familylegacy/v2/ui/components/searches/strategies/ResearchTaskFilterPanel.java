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

import io.github.mtrevisan.familylegacy.v2.io.model.readers.ResearchTaskReader;
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
 * Filter panel for ResearchTask records: description, status, priority, and outcome text.
 */
public class ResearchTaskFilterPanel extends JPanel implements RecordFilterPanel{

	private final JTextField descriptionField = new JTextField(20);
	private final JComboBox<String> statusCombo = new JComboBox<>(GUIHelper.fillCombo(ResearchTaskReader.STATUSES, I18N.t("search.combo.any")));
	private final JComboBox<String> priorityCombo = new JComboBox<>(GUIHelper.fillCombo(ResearchTaskReader.PRIORITIES, I18N.t("search.combo.any")));
	private final JTextField outcomeField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public ResearchTaskFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.research.task"))));

		add(new JLabel(I18N.t("dialog.research.task.description") + ":"));
		add(descriptionField, "growx");
		add(new JLabel(I18N.t("dialog.research.task.status") + ":"));
		add(statusCombo, "growx");
		add(new JLabel(I18N.t("dialog.research.task.priority") + ":"));
		add(priorityCombo, "growx");
		add(new JLabel(I18N.t("dialog.research.task.outcome") + ":"));
		add(outcomeField, "growx");
	}

	private void setupListeners(){
		statusCombo.addActionListener(e -> fireChanged());
		priorityCombo.addActionListener(e -> fireChanged());

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
		outcomeField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(ResearchTaskReader.TAG_DESCRIPTION, getDescription());
		filters.put(ResearchTaskReader.TAG_STATUS, getStatus());
		filters.put(ResearchTaskReader.TAG_PRIORITY, getPriority());
		filters.put(ResearchTaskReader.TAG_OUTCOME, getOutcome());
		return filters;
	}

	public String getDescription(){
		return descriptionField.getText().trim();
	}

	public String getStatus(){
		return (statusCombo.getSelectedIndex() == 0? null: (String)statusCombo.getSelectedItem());
	}

	public String getPriority(){
		return (priorityCombo.getSelectedIndex() == 0? null: (String)priorityCombo.getSelectedItem());
	}

	public String getOutcome(){
		return outcomeField.getText().trim();
	}

}
