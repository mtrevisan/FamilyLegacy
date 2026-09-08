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
 * Filter panel for Document records based on description, mapping projection, and URI.
 */
public class DocumentFilterPanel extends JPanel implements RecordFilterPanel{

	public static final String FILTER_KEY_DESCRIPTION = "description";
	public static final String FILTER_KEY_MAPPING = "mapping";
	public static final String FILTER_KEY_URI = "uri";


	private final JTextField descriptionField = new JTextField(20);
	private final JComboBox<String> mappingCombo = new JComboBox<>(new String[]{
		"Any",
		"planar",
		"spherical_equirectangular",
		"spherical_uv",
		"cubemap",
		"cylindrical_equirectangular_horizontal",
		"cylindrical_equirectangular_vertical"
	});
	private final JTextField uriField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public DocumentFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Document Filters"));

		add(new JLabel("Description:"));
		add(descriptionField, "growx");
		add(new JLabel("Mapping:"));
		add(mappingCombo, "growx");
		add(new JLabel("URI / Path:"));
		add(uriField, "growx");
	}

	private void setupListeners(){
		mappingCombo.addActionListener(e -> fireChanged());

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

		descriptionField.getDocument().addDocumentListener(docListener);
		uriField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null)
			onChanged.accept(null);
	}

	@Override
	public Map<String, String> getFilters(){
		final Map<String, String> filters = new HashMap<>();
		filters.put(FILTER_KEY_DESCRIPTION, getDescription());
		filters.put(FILTER_KEY_MAPPING, getMapping());
		filters.put(FILTER_KEY_URI, getUri());
		return filters;
	}

	public String getDescription(){
		return descriptionField.getText().trim();
	}

	public String getMapping(){
		return (mappingCombo.getSelectedIndex() == 0? null: (String)mappingCombo.getSelectedItem());
	}

	public String getUri(){
		return uriField.getText().trim();
	}

}
