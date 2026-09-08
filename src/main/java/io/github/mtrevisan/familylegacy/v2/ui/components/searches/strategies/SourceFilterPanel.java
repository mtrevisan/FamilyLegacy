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
 * Filter panel for Source records: medium, repository, and author.
 */
public class SourceFilterPanel extends JPanel{

	private final JComboBox<String> mediumCombo = new JComboBox<>(new String[]{
		"Any", "audio", "book", "card", "electronic", "fiche", "film", "magazine",
		"manuscript", "map", "newspaper", "photo", "tombstone", "video", "other", "unknown"
	});
	private final JTextField repositoryField = new JTextField(20);
	private final JTextField authorField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public SourceFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Source Filters"));

		add(new JLabel("Medium:"));
		add(mediumCombo, "growx");
		add(new JLabel("Repository:"));
		add(repositoryField, "growx");
		add(new JLabel("Author:"));
		add(authorField, "growx");
	}

	private void setupListeners(){
		mediumCombo.addActionListener(e -> fireChanged());

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

		repositoryField.getDocument().addDocumentListener(docListener);
		authorField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	public String getMedium(){
		return (mediumCombo.getSelectedIndex() == 0 ? null : (String)mediumCombo.getSelectedItem());
	}

	public String getRepository(){
		return repositoryField.getText().trim();
	}

	public String getAuthor(){
		return authorField.getText().trim();
	}

}
