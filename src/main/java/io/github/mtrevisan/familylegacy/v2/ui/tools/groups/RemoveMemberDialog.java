/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import io.github.mtrevisan.familylegacy.v2.ui.tools.places.PlaceHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Dialog that removes members from a group by deleting the underlying
 * {@code group_member} relationships.
 * <p>
 * The dialog lists the current members of the selected group, allows
 * multi-selection, and on confirmation deletes one relationship per
 * selected member. The member records themselves are never touched: the
 * individuals continue to exist, only the membership edge is removed.
 */
public final class RemoveMemberDialog extends JDialog{

	private final ToolContext context;

	private final JTextField groupField = new JTextField(24);
	private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
	private final JList<Entry> list = new JList<>(listModel);

	private FLEFRecord group;
	/** Map from individual id to the id of the relationship to delete. */
	private final Map<String, String> relationshipIdByMember = new LinkedHashMap<>();


	private record Entry(String id, String label){
		@Override public String toString(){ return label; }
	}


	public RemoveMemberDialog(final ToolContext context){
		super(context.owner(), "Remove Members", ModalityType.APPLICATION_MODAL);

		this.context = context;

		groupField.setEditable(false);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setVisibleRowCount(12);

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
		form.add(new JLabel("Group:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(groupField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickGroup = new JButton("Choose…");
		pickGroup.addActionListener(e -> chooseGroup());
		form.add(pickGroup, gbc);

		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel("Members:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1; gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		form.add(new JScrollPane(list), gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Remove selected");
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
		if(chosen[0] == null)
			return;
		group = chosen[0];
		groupField.setText(GroupHelper.displayName(group) + "  [" + group.getId() + "]");
		loadMembers();
	}

	private void loadMembers(){
		listModel.clear();
		relationshipIdByMember.clear();
		if(group == null)
			return;

		// Build the map id -> relationship once. This is the O(R) pass
		// that replaces the O(R) per member.
		for(final FLEFRecord rel : context.model().getRecordsByType(GroupHelper.TYPE_RELATIONSHIP)){
			final String type = FLEFRecordHelper.getChildValue(rel, GroupHelper.TAG_TYPE);
			if(type == null || !GroupHelper.REL_GROUP_MEMBER.equalsIgnoreCase(type))
				continue;
			final String target = rel.extractReferencedId(GroupHelper.TAG_TARGET, GroupHelper.TYPE_GROUP);
			if(!group.getId().equals(target))
				continue;
			final String subject = rel.extractReferencedId(GroupHelper.TAG_SUBJECT, GroupHelper.TYPE_INDIVIDUAL);
			if(subject == null)
				continue;
			relationshipIdByMember.putIfAbsent(subject, rel.getId());
		}

		for(final Map.Entry<String, String> entry : relationshipIdByMember.entrySet()){
			final FLEFRecord individual = context.model().getRecordById(entry.getKey());
			final String name = (individual != null? PlaceHelper.displayName(individual): entry.getKey());
			listModel.addElement(new Entry(entry.getKey(),
				(name != null? name: entry.getKey()) + "  [" + entry.getKey() + "]"));
		}

		if(listModel.isEmpty())
			listModel.addElement(new Entry(StringUtils.EMPTY, "(no members)"));
	}

	private void onConfirm(){
		if(group == null){
			JOptionPane.showMessageDialog(this,
				"Choose a group first.",
				"Remove Members", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final List<Entry> selected = list.getSelectedValuesList();
		if(selected.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Select at least one member.",
				"Remove Members", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final List<String> toRemove = new ArrayList<>();
		for(final Entry entry : selected)
			if(!entry.id().isEmpty()){
				final String relId = relationshipIdByMember.get(entry.id());
				if(relId != null)
					toRemove.add(relId);
			}

		if(toRemove.isEmpty())
			return;

		final int confirm = JOptionPane.showConfirmDialog(this,
			"Remove " + toRemove.size() + " member(s) from " + GroupHelper.displayName(group) + "?",
			"Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		for(final String relId : toRemove)
			context.model().removeRecord(relId);

		dispose();
	}

}
