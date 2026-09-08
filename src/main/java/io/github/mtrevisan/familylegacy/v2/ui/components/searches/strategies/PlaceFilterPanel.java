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
 * Filter panel for Place records: place type and parent jurisdiction.
 */
public class PlaceFilterPanel extends JPanel{

	private final JComboBox<String> placeTypeCombo = new JComboBox<>(new String[]{
		"Any", "address", "building", "street", "hamlet", "village", "town", "municipality", "city",
		"metropolitan_area", "county", "province", "department", "district", "region",
		"macro_region", "country", "empire", "parish", "diocese", "cemetery", "archive", "unknown"
	});
	private final JTextField parentJurisdictionField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public PlaceFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Place Filters"));

		add(new JLabel("Type:"));
		add(placeTypeCombo, "growx");
		add(new JLabel("Part of (Jurisdiction):"));
		add(parentJurisdictionField, "growx");
	}

	private void setupListeners(){
		placeTypeCombo.addActionListener(e -> fireChanged());

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

		parentJurisdictionField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	// Getters for parent to read criteria
	public String getPlaceType(){
		return (placeTypeCombo.getSelectedIndex() == 0 ? null : (String)placeTypeCombo.getSelectedItem());
	}

	public String getParentJurisdiction(){
		return parentJurisdictionField.getText().trim();
	}

}
