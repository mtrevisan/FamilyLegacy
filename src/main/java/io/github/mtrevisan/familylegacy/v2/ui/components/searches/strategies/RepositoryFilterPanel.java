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
 * Filter panel for Repository records: repository name, custodian reference, and location.
 */
public class RepositoryFilterPanel extends JPanel implements RecordFilterPanel{

	static final String FILTER_KEY_NAME = "name";
	static final String FILTER_KEY_CUSTODIAN = "custodian";
	static final String FILTER_KEY_LOCATION = "location";


	private final JTextField nameField = new JTextField(20);
	private final JTextField custodianField = new JTextField(20);
	private final JTextField locationField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public RepositoryFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Repository Filters"));

		add(new JLabel("Name:"));
		add(nameField, "growx");
		add(new JLabel("Custodian:"));
		add(custodianField, "growx");
		add(new JLabel("Location:"));
		add(locationField, "growx");
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

		nameField.getDocument()
			.addDocumentListener(docListener);
		custodianField.getDocument()
			.addDocumentListener(docListener);
		locationField.getDocument()
			.addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_NAME, getRepositoryName());
		filters.put(FILTER_KEY_CUSTODIAN, getCustodian());
		filters.put(FILTER_KEY_LOCATION, getRepositoryLocation());
		return filters;
	}

	public String getRepositoryName(){
		return nameField.getText()
			.trim();
	}

	public String getCustodian(){
		return custodianField.getText()
			.trim();
	}

	public String getRepositoryLocation(){
		return locationField.getText()
			.trim();
	}

}
