package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
 * Dialog that links one or more individuals to a group by creating
 * {@code group_member} relationships.
 * <p>
 * The dialog collects three inputs: the target group, the set of
 * individuals to add, and an optional role. On confirmation, it creates
 * one relationship record per individual, so the model always carries
 * one edge per membership and a single member can be removed without
 * touching the others.
 */
public final class AddMemberDialog extends JDialog{

	private final ToolContext context;

	private final JTextField groupField = new JTextField(24);
	private final JTextArea membersArea = new JTextArea(6, 24);
	private final JTextField roleField = new JTextField(24);

	private FLEFRecord group;
	private final List<FLEFRecord> members = new ArrayList<>();


	public AddMemberDialog(final ToolContext context){
		super(context.owner(), "Add Members", ModalityType.APPLICATION_MODAL);
		this.context = context;

		groupField.setEditable(false);
		membersArea.setEditable(false);
		roleField.setText("member");

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

		// Group row.
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel("Group:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(groupField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickGroup = new JButton("Choose…");
		pickGroup.addActionListener(e -> chooseGroup());
		form.add(pickGroup, gbc);

		// Members row.
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
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

		// Role row.
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		form.add(new JLabel("Role:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(roleField, gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Add members");
		ok.addActionListener(e -> onConfirm());
		buttons.add(ok);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	private void chooseGroup(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			GroupHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			group = chosen[0];
			groupField.setText(GroupHelper.displayName(group) + "  [" + group.getId() + "]");
		}
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
			final String name = io.github.mtrevisan.familylegacy.v2.ui.tools.places.PlaceHelper
				.displayName(members.get(i));
			sb.append(name != null? name: members.get(i).getId())
				.append("  [").append(members.get(i).getId()).append(']');
		}
		membersArea.setText(sb.toString());
		membersArea.setCaretPosition(0);
	}

	private void onConfirm(){
		if(group == null){
			JOptionPane.showMessageDialog(this,
				"Choose a group first.",
				"Add Members", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(members.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Choose at least one individual.",
				"Add Members", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final String role = roleField.getText();
		for(final FLEFRecord member : members)
			GroupHelper.createMembership(context.model(), member.getId(), group.getId(),
				role, RelationshipHandler.ID_PREFIX);

		dispose();
	}

}
