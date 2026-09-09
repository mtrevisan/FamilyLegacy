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
 * Filter panel for Event records: event type, description, date, location, agency, and cause reason.
 */
public class EventFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_EVENT_TYPE = "eventType";
	static final String FILTER_KEY_DESCRIPTION = "description";
	static final String FILTER_KEY_DATE = "date";
	static final String FILTER_KEY_CALENDAR = "calendar";
	static final String FILTER_KEY_LOCATION = "location";
	static final String FILTER_KEY_AGENCY = "agency";
	static final String FILTER_KEY_CAUSE_REASON = "causeReason";


	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		"Any",
		"birth", "death", "adoption", "graduation", "immigration", "naturalization", "bankruptcy",
		"guardianship", "coroner_report", "cremation", "burial", "education", "retirement",
		"military_induction", "military_muster_roll", "military_service", "military_award",
		"military_release", "military_discharge", "military_resignation", "military_retirement",
		"prison", "pardon", "jury_duty", "illness", "hospitalization", "medical_procedure", "honor",
		"deportation", "internment", "liberation", "emancipation", "relocation", "emigration",
		"census", "deed", "escrow", "chancery", "will", "probate",
		"engagement", "marriage_bann", "marriage_contract", "marriage_license", "marriage_settlement",
		"marriage", "divorce_filed", "divorce_decree", "divorce", "annulment"
	});
	private final JTextField descriptionField = new JTextField(20);
	private final JTextField dateField = new JTextField(10);
	private final JComboBox<String> calendarCombo = new JComboBox<>(new String[]{
		"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic",
		"soviet eternal", "ethiopian", "mayan"});
	private final JTextField locationField = new JTextField(20);
	private final JTextField agencyField = new JTextField(20);
	private final JTextField causeReasonField = new JTextField(20);

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
		add(typeCombo, "growx");
		add(new JLabel("Description:"));
		add(descriptionField, "growx");
		add(new JLabel("Date:"));
		add(dateField, "growx");
		add(new JLabel("Calendar:"));
		add(calendarCombo, "growx");
		add(new JLabel("Location:"));
		add(locationField, "growx");
		add(new JLabel("Agency:"));
		add(agencyField, "growx");
		add(new JLabel("Cause reason:"));
		add(causeReasonField, "growx");
	}

	private void setupListeners(){
		typeCombo.addActionListener(e -> fireChanged());

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
		dateField.getDocument()
			.addDocumentListener(docListener);
		locationField.getDocument()
			.addDocumentListener(docListener);
		agencyField.getDocument()
			.addDocumentListener(docListener);
		causeReasonField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_EVENT_TYPE, getEventType());
		filters.put(FILTER_KEY_DESCRIPTION, getDescription());
		filters.put(FILTER_KEY_DATE, getDate());
		filters.put(FILTER_KEY_CALENDAR, getCalendar());
		filters.put(FILTER_KEY_LOCATION, getPlace());
		filters.put(FILTER_KEY_AGENCY, getAgency());
		filters.put(FILTER_KEY_CAUSE_REASON, getCauseReason());
		return filters;
	}

	public String getEventType(){
		return (typeCombo.getSelectedIndex() == 0? null: (String)typeCombo.getSelectedItem());
	}

	public String getDescription(){
		return descriptionField.getText()
			.trim();
	}

	public String getDate(){
		return dateField.getText()
			.trim();
	}

	public String getCalendar(){
		return (String)calendarCombo.getSelectedItem();
	}

	public String getPlace(){
		return locationField.getText()
			.trim();
	}

	public String getAgency(){
		return agencyField.getText()
			.trim();
	}

	public String getCauseReason(){
		return causeReasonField.getText()
			.trim();
	}

}
