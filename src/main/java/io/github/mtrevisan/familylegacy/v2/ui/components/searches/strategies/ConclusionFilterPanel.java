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
import java.util.function.Consumer;


/* TODO */
/**
 * Filter panel for Conclusion records: confidence level, focus individual, and research question.
 */
public class ConclusionFilterPanel extends JPanel implements RecordFilterPanel{

	private final JComboBox<String> confidenceCombo = new JComboBox<>(new String[]{
		"Any", "low", "medium", "high", "certain", "unknown"
	});
	private final JTextField targetIndividualField = new JTextField(20);
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

		add(new JLabel("Confidence:"));
		add(confidenceCombo, "growx");
		add(new JLabel("Target individual:"));
		add(targetIndividualField, "growx");
		add(new JLabel("Research question:"));
		add(researchQuestionField, "growx");
	}

	private void setupListeners(){
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

		targetIndividualField.getDocument().addDocumentListener(docListener);
		researchQuestionField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	public String getConfidence(){
		return (confidenceCombo.getSelectedIndex() == 0 ? null : (String)confidenceCombo.getSelectedItem());
	}

	public String getTargetIndividualContains(){
		return targetIndividualField.getText().trim();
	}

	public String getResearchQuestionContains(){
		return researchQuestionField.getText().trim();
	}

}
