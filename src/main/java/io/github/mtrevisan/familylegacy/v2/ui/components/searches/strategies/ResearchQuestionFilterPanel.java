package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

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
 * Filter panel for ResearchQuestion records: status, priority, and focus individual.
 */
public class ResearchQuestionFilterPanel extends JPanel{

	private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
		"Any", "open", "in_progress", "resolved", "abandoned", "unknown"
	});
	private final JComboBox<String> priorityCombo = new JComboBox<>(new String[]{
		"Any", "low", "medium", "high", "critical", "unknown"
	});
	private final JTextField focusIndividualField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public ResearchQuestionFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Research Question Filters"));

		add(new JLabel("Status:"));
		add(statusCombo, "growx");
		add(new JLabel("Priority:"));
		add(priorityCombo, "growx");
		add(new JLabel("Focus individual:"));
		add(focusIndividualField, "growx");
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

		focusIndividualField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	public String getStatus(){
		return (statusCombo.getSelectedIndex() == 0 ? null : (String)statusCombo.getSelectedItem());
	}

	public String getPriority(){
		return (priorityCombo.getSelectedIndex() == 0 ? null : (String)priorityCombo.getSelectedItem());
	}

	public String getFocusIndividual(){
		return focusIndividualField.getText().trim();
	}

}
