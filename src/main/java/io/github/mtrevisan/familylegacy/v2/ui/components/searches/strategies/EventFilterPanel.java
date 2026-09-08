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
 * Filter panel for Event records: event type, date range, and location.
 */
public class EventFilterPanel extends JPanel implements RecordFilterPanel{

	private final JComboBox<String> eventTypeCombo = new JComboBox<>(new String[]{
		"Any", "birth", "baptism", "marriage", "divorce", "death", "burial",
		"residence", "occupation", "immigration", "emigration", "census",
		"military_service", "will", "probate", "other", "unknown"
	});
	private final JTextField dateFromField = new JTextField(10);
	private final JTextField dateToField = new JTextField(10);
	private final JTextField locationField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public EventFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Event Filters"));

		add(new JLabel("Type:"));
		add(eventTypeCombo, "growx");
		add(new JLabel("Date from:"));
		add(dateFromField, "growx");
		add(new JLabel("Date to:"));
		add(dateToField, "growx");
		add(new JLabel("Location:"));
		add(locationField, "growx");
	}

	private void setupListeners(){
		eventTypeCombo.addActionListener(e -> fireChanged());

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

		dateFromField.getDocument().addDocumentListener(docListener);
		dateToField.getDocument().addDocumentListener(docListener);
		locationField.getDocument().addDocumentListener(docListener);
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

	public String getEventType(){
		return (eventTypeCombo.getSelectedIndex() == 0 ? null : (String)eventTypeCombo.getSelectedItem());
	}

	public String getDateFrom(){
		return dateFromField.getText().trim();
	}

	public String getDateTo(){
		return dateToField.getText().trim();
	}

	public String getLocationContains(){
		return locationField.getText().trim();
	}

}
