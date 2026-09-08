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
 * Filter panel for Group records: group type and member name.
 */
public class GroupFilterPanel extends JPanel{

	private final JComboBox<String> groupTypeCombo = new JComboBox<>(new String[]{
		"Any", "family", "household", "religious_community", "military_unit",
		"guild", "organization", "crew", "expedition", "unknown"
	});
	private final JTextField memberNameField = new JTextField(20);

	private final Consumer<SearchCriteria> onChanged;


	public GroupFilterPanel(final Consumer<SearchCriteria> onChanged){
		this.onChanged = onChanged;

		initComponents();

		setupListeners();
	}


	private void initComponents(){
		setLayout(new MigLayout("wrap 2,gap 5", "[][grow,fill]", "[]"));
		setBorder(BorderFactory.createTitledBorder("Group Filters"));

		add(new JLabel("Type:"));
		add(groupTypeCombo, "growx");
		add(new JLabel("Member name:"));
		add(memberNameField, "growx");
	}

	private void setupListeners(){
		groupTypeCombo.addActionListener(e -> fireChanged());

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

		memberNameField.getDocument().addDocumentListener(docListener);
	}

	private void fireChanged(){
		if(onChanged != null){
			onChanged.accept(null);
		}
	}

	public String getGroupType(){
		return (groupTypeCombo.getSelectedIndex() == 0 ? null : (String)groupTypeCombo.getSelectedItem());
	}

	public String getMemberName(){
		return memberNameField.getText().trim();
	}

}
