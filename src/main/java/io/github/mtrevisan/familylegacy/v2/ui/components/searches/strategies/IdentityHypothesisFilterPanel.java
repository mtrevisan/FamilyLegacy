package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Filter panel for IdentityHypothesis records: candidate record reference and comment text.
 */
public class IdentityHypothesisFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_CANDIDATE = "candidate";
	static final String FILTER_KEY_COMMENT = "comment";


	private final JTextField candidateField = new JTextField(20);
	private final JTextField commentField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public IdentityHypothesisFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Identity Hypothesis Filters"));

		add(new JLabel("Candidate:"));
		add(candidateField, "growx");
		add(new JLabel("Comment:"));
		add(commentField, "growx");
	}

	private void setupListeners(){
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

		candidateField.getDocument()
			.addDocumentListener(docListener);
		commentField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_CANDIDATE, getCandidate());
		filters.put(FILTER_KEY_COMMENT, getComment());
		return filters;
	}

	public String getCandidate(){
		return candidateField.getText()
			.trim();
	}

	public String getComment(){
		return commentField.getText()
			.trim();
	}

}
