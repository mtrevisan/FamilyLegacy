package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import io.github.mtrevisan.familylegacy.v2.ui.tools.places.PlaceHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog that creates a new {@code GroupRecord} and populates it with
 * the individuals chosen by the user.
 * <p>
 * The dialog is a one-shot wizard: the user selects a set of individuals
 * and provides a name and type for the group, and on confirmation the
 * tool creates the group record and one membership relationship per
 * selected individual. The default type is {@code family}, which is the
 * most common case for a group assembled from a hand-picked selection.
 */
public final class CreateFamilyFromSelectionDialog extends JDialog{

	private final ToolContext context;

	private final JTextField nameField = new JTextField(24);
	private final JComboBox<String> typeCombo = new JComboBox<>(
		GroupHelper.DECLARED_GROUP_TYPES.toArray(new String[0]));
	private final JTextArea membersArea = new JTextArea(6, 24);

	private final List<FLEFRecord> members = new ArrayList<>();


	public CreateFamilyFromSelectionDialog(final ToolContext context){
		super(context.owner(), "Create Family From Selection", ModalityType.APPLICATION_MODAL);
		this.context = context;

		typeCombo.setSelectedItem("family");
		membersArea.setEditable(false);

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(560, 380));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());
	}


	private JPanel createForm(){
		final JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel("Name:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(nameField, gbc);
		gbc.gridwidth = 1;

		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel("Type:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(typeCombo, gbc);
		gbc.gridwidth = 1;

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel("Members:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		form.add(new JScrollPane(membersArea), gbc);
		gbc.gridx = 2; gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		final JButton pickMembers = new JButton("Choose…");
		pickMembers.addActionListener(e -> chooseMembers());
		form.add(pickMembers, gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Create family");
		ok.addActionListener(e -> onConfirm());
		buttons.add(ok);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	private void chooseMembers(){
		final List<String> ids = MultiIndividualPickerDialog.pick(this, context.model());
		if(ids.isEmpty())
			return;
		members.clear();
		for(final String id : ids){
			final FLEFRecord record = context.model().getRecordById(id);
			if(record != null)
				members.add(record);
		}
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < members.size(); i++){
			if(i > 0)
				sb.append('\n');
			final String name = PlaceHelper.displayName(members.get(i));
			sb.append(name != null? name: members.get(i).getId())
				.append("  [").append(members.get(i).getId()).append(']');
		}
		membersArea.setText(sb.toString());
		membersArea.setCaretPosition(0);
	}

	private void onConfirm(){
		final String name = nameField.getText();
		if(name == null || name.isBlank()){
			JOptionPane.showMessageDialog(this,
				"Enter a name for the group.",
				"Create Family", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(members.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Choose at least one individual.",
				"Create Family", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final String type = (String)typeCombo.getSelectedItem();
		final FLEFRecord group = GroupHelper.createGroup(context.model(), name,
			type, GroupHandler.ID_PREFIX);
		for(final FLEFRecord member : members)
			GroupHelper.createMembership(context.model(), member.getId(), group.getId(),
				"member", RelationshipHandler.ID_PREFIX);

		dispose();
	}

}
