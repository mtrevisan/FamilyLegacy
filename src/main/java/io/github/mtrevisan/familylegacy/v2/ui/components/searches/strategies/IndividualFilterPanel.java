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


/**
 * Filter panel for Individual records: event type, date range, location.
 */
public class IndividualFilterPanel extends JPanel{

	private final JComboBox<String> sexCombo = new JComboBox<>(new String[]{"Any", "male", "female"});
	private final JComboBox<String> eventTypeCombo = new JComboBox<>(new String[]{"Any", "birth", "death", "marriage", "baptism", "burial", "residence"});
	private final JTextField dateFromField = new JTextField(10);
	private final JTextField dateToField = new JTextField(10);
	private final JTextField locationField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public IndividualFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Event Filters"));

		add(new JLabel("Sex:"));
		add(sexCombo, "growx");
		add(new JLabel("Event type:"));
		add(eventTypeCombo, "growx");
		add(new JLabel("Date from:"));
		add(dateFromField, "growx");
		add(new JLabel("Date to:"));
		add(dateToField, "growx");
		add(new JLabel("Location:"));
		add(locationField, "growx");
	}

	private void setupListeners(){
		sexCombo.addActionListener(e -> fireChanged());
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

		dateFromField.getDocument()
			.addDocumentListener(docListener);
		dateToField.getDocument()
			.addDocumentListener(docListener);
		locationField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			// The actual criteria update is handled by the parent dialog.
			// We just notify that something changed.
			onChanged.accept(null);
	}

	// Getters for the parent to read values
	public String getSex(){
		return (sexCombo.getSelectedIndex() == 0? null: (String)sexCombo.getSelectedItem());
	}

	public String getEventType(){
		return (eventTypeCombo.getSelectedIndex() == 0? null: (String)eventTypeCombo.getSelectedItem());
	}

	public String getDateFrom(){
		return dateFromField.getText()
			.trim();
	}

	public String getDateTo(){
		return dateToField.getText()
			.trim();
	}

	public String getLocationContains(){
		return locationField.getText()
			.trim();
	}

}
