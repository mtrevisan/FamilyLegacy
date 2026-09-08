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
 * Filter panel for Document records: format, mime type, and location.
 */
public class DocumentFilterPanel extends JPanel{

	private final JComboBox<String> formatCombo = new JComboBox<>(new String[]{
		"Any", "digital", "physical", "unknown"
	});
	private final JTextField mimeTypeField = new JTextField(20);
	private final JTextField locationField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public DocumentFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Document Filters"));

		add(new JLabel("Format:"));
		add(formatCombo, "growx");
		add(new JLabel("MIME type:"));
		add(mimeTypeField, "growx");
		add(new JLabel("Location:"));
		add(locationField, "growx");
	}

	private void setupListeners(){
		formatCombo.addActionListener(e -> fireChanged());

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

		mimeTypeField.getDocument().addDocumentListener(docListener);
		locationField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	public String getFormat(){
		return (formatCombo.getSelectedIndex() == 0 ? null : (String)formatCombo.getSelectedItem());
	}

	public String getMimeType(){
		return mimeTypeField.getText().trim();
	}

	public String getLocationContains(){
		return locationField.getText().trim();
	}

}
