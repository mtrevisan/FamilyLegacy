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
 * Filter panel for CulturalNorm records: norm type, place/region, and time period.
 */
public class CulturalNormFilterPanel extends JPanel{

	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		"Any", "naming_convention", "inheritance_law", "marriage_custom",
		"religious_practice", "social_status", "burial_custom", "other", "unknown"
	});
	private final JTextField locationField = new JTextField(20);
	private final JTextField periodField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public CulturalNormFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Cultural Norm Filters"));

		add(new JLabel("Type:"));
		add(typeCombo, "growx");
		add(new JLabel("Location / Region:"));
		add(locationField, "growx");
		add(new JLabel("Time period:"));
		add(periodField, "growx");
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

		locationField.getDocument().addDocumentListener(docListener);
		periodField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	public String getNormType(){
		return (typeCombo.getSelectedIndex() == 0 ? null : (String)typeCombo.getSelectedItem());
	}

	public String getLocationContains(){
		return locationField.getText().trim();
	}

	public String getTimePeriodContains(){
		return periodField.getText().trim();
	}

}
