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


/* TODO */
/**
 * Filter panel for IdentityHypothesis records: status, confidence level, and individual references.
 */
public class IdentityHypothesisFilterPanel extends JPanel implements RecordFilterPanel{

	private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
		"Any", "proposed", "under_review", "confirmed", "rejected", "unknown"
	});
	private final JComboBox<String> confidenceCombo = new JComboBox<>(new String[]{
		"Any", "low", "medium", "high", "certain", "unknown"
	});
	private final JTextField individualField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public IdentityHypothesisFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Identity Hypothesis Filters"));

		add(new JLabel("Status:"));
		add(statusCombo, "growx");
		add(new JLabel("Confidence:"));
		add(confidenceCombo, "growx");
		add(new JLabel("Individual:"));
		add(individualField, "growx");
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

		individualField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
//		filters.put("documentType", getDocumentType());
//		filters.put("repositoryContains", getRepositoryContains());
//		filters.put("referenceNumberContains", getReferenceNumberContains());
		return filters;
	}

	public String getStatus(){
		return (statusCombo.getSelectedIndex() == 0 ? null : (String)statusCombo.getSelectedItem());
	}

	public String getConfidence(){
		return (confidenceCombo.getSelectedIndex() == 0 ? null : (String)confidenceCombo.getSelectedItem());
	}

	public String getIndividualContains(){
		return individualField.getText().trim();
	}

}
