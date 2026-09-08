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
 * Filter panel for HistoricEvent records: type, title, date, and location.
 */
public class HistoricEventFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_TYPE = "type";
	static final String FILTER_KEY_TITLE = "title";
	static final String FILTER_KEY_DATE = "date";
	static final String FILTER_KEY_CALENDAR = "calendar";
	static final String FILTER_KEY_PLACE = "place";


	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		"Any",
		"war",
		"epidemic",
		"famine",
		"migration",
		"legal_reform",
		"political_change",
		"territorial_change",
		"natural_disaster",
		"economic_crisis",
		"scientific_discovery",
		"religious_reform",
		"social_movement",
		"pandemic"
	});
	private final JTextField titleField = new JTextField(20);
	private final JTextField dateField = new JTextField(10);
	private final JComboBox<String> calendarCombo = new JComboBox<>(new String[]{
		"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic",
		"soviet eternal", "ethiopian", "mayan"});
	private final JTextField locationField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public HistoricEventFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Historic Event Filters"));

		add(new JLabel("Type:"));
		add(typeCombo, "growx");
		add(new JLabel("Title:"));
		add(titleField, "growx");
		add(new JLabel("Date:"));
		add(dateField, "growx");
		add(new JLabel("Calendar from:"));
		add(calendarCombo, "growx");
		add(new JLabel("Place:"));
		add(locationField, "growx");
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

		titleField.getDocument().addDocumentListener(docListener);
		dateField.getDocument().addDocumentListener(docListener);
		locationField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_TYPE, getType());
		filters.put(FILTER_KEY_TITLE, getTitle());
		filters.put(FILTER_KEY_DATE, getDate());
		filters.put(FILTER_KEY_CALENDAR, getCalendar());
		filters.put(FILTER_KEY_PLACE, getPlace());
		return filters;
	}

	public String getType(){
		return (typeCombo.getSelectedIndex() == 0? null: (String)typeCombo.getSelectedItem());
	}

	public String getTitle(){
		return titleField.getText()
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

}
