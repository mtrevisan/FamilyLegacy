package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.function.Consumer;


/* TODO */
/**
 * Filter panel for Repository records: location and contact details.
 */
public class RepositoryFilterPanel extends JPanel{

	private final JTextField locationField = new JTextField(20);
	private final JTextField emailField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public RepositoryFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Repository Filters"));

		add(new JLabel("Location:"));
		add(locationField, "growx");
		add(new JLabel("Email:"));
		add(emailField, "growx");
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

		locationField.getDocument().addDocumentListener(docListener);
		emailField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	public String getLocationContains(){
		return locationField.getText().trim();
	}

	public String getEmailContains(){
		return emailField.getText().trim();
	}

}
