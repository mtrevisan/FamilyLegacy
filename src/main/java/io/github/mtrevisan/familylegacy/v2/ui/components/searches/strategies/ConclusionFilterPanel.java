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
 * Filter panel for Conclusion records: issue, proof status, narrative text, and research question.
 */
public class ConclusionFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_ISSUE = "issue";
	static final String FILTER_KEY_PROOF_STATUS = "proofStatus";
	static final String FILTER_KEY_NARRATIVE = "narrative";
	static final String FILTER_KEY_RESEARCH_QUESTION = "researchQuestion";


	private final JTextField issueField = new JTextField(20);
	private final JComboBox<String> proofStatusCombo = new JComboBox<>(new String[]{
		"Any",
		"conflicting_evidence",
		"supported",
		"proven",
		"disproven"
	});
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
		setBorder(BorderFactory.createTitledBorder("Conclusion Filters"));

		add(new JLabel("Issue:"));
		add(issueField, "growx");
		add(new JLabel("Proof status:"));
		add(proofStatusCombo, "growx");
		add(new JLabel("Narrative:"));
		add(narrativeField, "growx");
		add(new JLabel("Research question:"));
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

		issueField.getDocument().addDocumentListener(docListener);
		narrativeField.getDocument().addDocumentListener(docListener);
		researchQuestionField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_ISSUE, getIssue());
		filters.put(FILTER_KEY_PROOF_STATUS, getProofStatus());
		filters.put(FILTER_KEY_NARRATIVE, getNarrative());
		filters.put(FILTER_KEY_RESEARCH_QUESTION, getResearchQuestion());
		return filters;
	}

	public String getIssue(){
		return issueField.getText().trim();
	}

	public String getProofStatus(){
		return (proofStatusCombo.getSelectedIndex() == 0? null: (String)proofStatusCombo.getSelectedItem());
	}

	public String getNarrative(){
		return narrativeField.getText().trim();
	}

	public String getResearchQuestion(){
		return researchQuestionField.getText().trim();
	}

}
