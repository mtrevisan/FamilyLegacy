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

import io.github.mtrevisan.familylegacy.v2.io.model.readers.ResearchQuestionReader;
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
 * Filter panel for ResearchQuestion records: title, question text, status, and confidence.
 */
public class ResearchQuestionFilterPanel extends JPanel implements RecordFilterPanel{

	private final JTextField titleField = new JTextField(20);
	private final JTextField questionField = new JTextField(20);
	private final JComboBox<String> statusCombo = new JComboBox<>(GUIHelper.fillCombo(ResearchQuestionReader.STATUSES, I18N.t("search.combo.any")));
	private final JComboBox<String> confidenceCombo = new JComboBox<>(GUIHelper.fillCombo(ResearchQuestionReader.CONFIDENCES, I18N.t("search.combo.any")));

	private final Consumer<SearchCriteria> onChanged;


	public ResearchQuestionFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.research.question"))));

		add(new JLabel(I18N.t("dialog.research.question.title") + ":"));
		add(titleField, "growx");
		add(new JLabel(I18N.t("dialog.research.question.question") + ":"));
		add(questionField, "growx");
		add(new JLabel(I18N.t("dialog.research.question.status") + ":"));
		add(statusCombo, "growx");
		add(new JLabel(I18N.t("dialog.research.question.confidence") + ":"));
		add(confidenceCombo, "growx");
	}

	private void setupListeners(){
		statusCombo.addActionListener(e -> fireChanged());
		confidenceCombo.addActionListener(e -> fireChanged());

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
		questionField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(ResearchQuestionReader.TAG_TITLE, getTitle());
		filters.put(ResearchQuestionReader.TAG_QUESTION, getQuestion());
		filters.put(ResearchQuestionReader.TAG_STATUS, getStatus());
		filters.put(ResearchQuestionReader.TAG_CONCLUSION_CONFIDENCE, getConfidence());
		return filters;
	}

	public String getTitle(){
		return titleField.getText().trim();
	}

	public String getQuestion(){
		return questionField.getText().trim();
	}

	public String getStatus(){
		return (statusCombo.getSelectedIndex() == 0? null: (String)statusCombo.getSelectedItem());
	}

	public String getConfidence(){
		return (confidenceCombo.getSelectedIndex() == 0? null: (String)confidenceCombo.getSelectedItem());
	}

}
