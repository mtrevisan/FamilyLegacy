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
 * Filter panel for ResearchActivity records: activity type, status, action, result, and observation.
 */
public class ResearchActivityFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_ACTIVITY_TYPE = "activityType";
	static final String FILTER_KEY_STATUS = "status";
	static final String FILTER_KEY_ACTION = "action";
	static final String FILTER_KEY_RESULT = "result";
	static final String FILTER_KEY_OBSERVATION = "observation";


	private final JComboBox<String> activityTypeCombo = new JComboBox<>(new String[]{
		"Any",
		"search",
		"review",
		"analysis",
		"correspondence",
		"interview",
		"hypothesis"
	});
	private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
		"Any",
		"planned",
		"in_progress",
		"completed",
		"abandoned"
	});
	private final JTextField actionField = new JTextField(20);
	private final JComboBox<String> resultCombo = new JComboBox<>(new String[]{
		"Any",
		"positive",
		"negative",
		"inconclusive",
		"conflicting",
		"unavailable"
	});
	private final JTextField observationField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public ResearchActivityFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Research Activity Filters"));

		add(new JLabel("Type:"));
		add(activityTypeCombo, "growx");
		add(new JLabel("Status:"));
		add(statusCombo, "growx");
		add(new JLabel("Action:"));
		add(actionField, "growx");
		add(new JLabel("Result:"));
		add(resultCombo, "growx");
		add(new JLabel("Observation:"));
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
		filters.put(FILTER_KEY_ACTIVITY_TYPE, getActivityType());
		filters.put(FILTER_KEY_STATUS, getStatus());
		filters.put(FILTER_KEY_ACTION, getAction());
		filters.put(FILTER_KEY_RESULT, getResult());
		filters.put(FILTER_KEY_OBSERVATION, getObservation());
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
