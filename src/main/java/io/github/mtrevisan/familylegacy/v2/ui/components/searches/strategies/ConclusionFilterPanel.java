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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
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
 * Filter panel for Conclusion records: issue, proof status, narrative text, and research question.
 */
public class ConclusionFilterPanel extends JPanel implements RecordFilterPanel{

	private final JTextField issueField = new JTextField(20);
	private final JComboBox<String> proofStatusCombo = new JComboBox<>(GUIHelper.fillCombo(ConclusionHandler.PROOF_STATUSES, I18N.t("search.combo.any")));
	private final JTextField narrativeField = new JTextField(20);
	private final JTextField researchQuestionField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public ConclusionFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder(I18N.tf("dialog.search.filter.title", I18N.t("dialog.component.conclusion"))));

		add(new JLabel(I18N.t("dialog.conclusion.issue") + ":"));
		add(issueField, "growx");
		add(new JLabel(I18N.t("dialog.conclusion.proof.status") + ":"));
		add(proofStatusCombo, "growx");
		add(new JLabel(I18N.t("dialog.conclusion.narrative") + ":"));
		add(narrativeField, "growx");
		add(new JLabel(I18N.t("dialog.conclusion.research.question") + "Research question:"));
		add(researchQuestionField, "growx");
	}

	private void setupListeners(){
		proofStatusCombo.addActionListener(e -> fireChanged());

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

		issueField.getDocument()
			.addDocumentListener(docListener);
		narrativeField.getDocument()
			.addDocumentListener(docListener);
		researchQuestionField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(ConclusionHandler.TAG_ISSUE, getIssue());
		filters.put(ConclusionHandler.TAG_PROOF_STATUS, getProofStatus());
		filters.put(ConclusionHandler.TAG_NARRATIVE, getNarrative());
		filters.put(ConclusionHandler.TAG_RESEARCH, getResearchQuestion());
		return filters;
	}

	public String getIssue(){
		return issueField.getText()
			.trim();
	}

	public String getProofStatus(){
		return (proofStatusCombo.getSelectedIndex() == 0? null: (String)proofStatusCombo.getSelectedItem());
	}

	public String getNarrative(){
		return narrativeField.getText()
			.trim();
	}

	public String getResearchQuestion(){
		return researchQuestionField.getText()
			.trim();
	}

}
