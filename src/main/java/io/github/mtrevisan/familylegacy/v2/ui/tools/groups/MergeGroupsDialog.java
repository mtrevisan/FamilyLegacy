package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;


/**
 * Dialog that merges one group into another.
 * <p>
 * The merge proceeds in three steps:
 * <ol>
 *   <li>every {@code group_member} relationship whose target is the
 *       source group is re-pointed to the target group;</li>
 *   <li>every {@code part_of} relationship whose subject is the source
 *       group is re-pointed to the target group, so the sub-groups of
 *       the source become sub-groups of the target;</li>
 *   <li>the source group record is deleted, together with any remaining
 *       relationship that involves it.</li>
 * </ol>
 * The dialog warns the user that sources, notes, and parent links
 * attached to the source group are lost, because there is no obvious
 * place to put them in the target group without risking duplication.
 */
public final class MergeGroupsDialog extends JDialog{

	private final ToolContext context;

	private final JTextField sourceField = new JTextField(24);
	private final JTextField targetField = new JTextField(24);

	private FLEFRecord source;
	private FLEFRecord target;


	public MergeGroupsDialog(final ToolContext context){
		super(context.owner(), "Merge Groups", ModalityType.APPLICATION_MODAL);
		this.context = context;

		sourceField.setEditable(false);
		targetField.setEditable(false);

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(560, 260));

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
		form.add(new JLabel("Source group:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(sourceField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickSource = new JButton("Choose…");
		pickSource.addActionListener(e -> chooseSource());
		form.add(pickSource, gbc);

		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel("Target group:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(targetField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickTarget = new JButton("Choose…");
		pickTarget.addActionListener(e -> chooseTarget());
		form.add(pickTarget, gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
		gbc.insets = new Insets(12, 4, 4, 4);
		final JLabel hint = new JLabel("<html><i>Members and sub-groups of the source are "
			+ "moved to the target.<br>The source's own sources, notes, and parent "
			+ "links are not carried over.</i></html>");
		hint.setForeground(java.awt.Color.GRAY);
		form.add(hint, gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Merge");
		ok.addActionListener(e -> onConfirm());
		buttons.add(ok);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	private void chooseSource(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			GroupHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			source = chosen[0];
			sourceField.setText(GroupHelper.displayName(source) + "  [" + source.getId() + "]");
		}
	}

	private void chooseTarget(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			GroupHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			target = chosen[0];
			targetField.setText(GroupHelper.displayName(target) + "  [" + target.getId() + "]");
		}
	}

	private void onConfirm(){
		if(source == null || target == null){
			JOptionPane.showMessageDialog(this,
				"Choose both a source and a target group.",
				"Merge Groups", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(source.getId().equals(target.getId())){
			JOptionPane.showMessageDialog(this,
				"The source and the target must be different groups.",
				"Merge Groups", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final String message = "Merge " + GroupHelper.displayName(source)
			+ " into " + GroupHelper.displayName(target) + "?\n\n"
			+ "The source group will be deleted.";
		final int confirm = JOptionPane.showConfirmDialog(this, message,
			"Confirm Merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		// Step 1 and 2: re-point the relationships.
		final List<FLEFRecord> relationships =
			context.model().getRecordsByType(GroupHelper.TYPE_RELATIONSHIP);
		for(final FLEFRecord rel : relationships){
			final String type = FLEFRecordHelper.getChildValue(rel, GroupHelper.TAG_TYPE);
			if(type == null)
				continue;
			final String t = type.toLowerCase(java.util.Locale.ROOT);

			if(GroupHelper.REL_GROUP_MEMBER.equals(t)){
				final String targetGroup = rel.extractReferencedId(GroupHelper.TAG_TARGET,
					GroupHelper.TYPE_GROUP);
				if(source.getId().equals(targetGroup))
					setChildValue(rel, GroupHelper.TAG_TARGET, GroupHelper.TYPE_GROUP,
						target.getId());
			}
			else if(GroupHelper.REL_PART_OF.equals(t)){
				final String subjectGroup = rel.extractReferencedId(GroupHelper.TAG_SUBJECT,
					GroupHelper.TYPE_GROUP);
				if(source.getId().equals(subjectGroup))
					setChildValue(rel, GroupHelper.TAG_SUBJECT, GroupHelper.TYPE_GROUP,
						target.getId());
			}
		}

		// Step 3: delete any remaining relationship that involves the
		// source group, then delete the source group itself.
		final List<String> leftovers = GroupHelper.allRelationshipIdsForGroup(
			context.model(), source.getId());
		for(final String relId : leftovers)
			context.model().removeRecord(relId);
		context.model().removeRecord(source.getId());

		dispose();
	}

	/**
	 * Replaces the value of the deepest child carrying the given tag
	 * inside the given wrapper. The wrapper is the direct child of the
	 * relationship with the name {@code wrapperTag} (for example
	 * {@code subject} or {@code target}), and inside it there is a
	 * nested block named after the entity type ({@code group},
	 * {@code individual}) whose only child holds the id.
	 */
	private static void setChildValue(final FLEFRecord relationship,
		final String wrapperTag, final String entityTag, final String newId){
		final FLEFRecord wrapper = FLEFRecordHelper.findChild(relationship, wrapperTag);
		if(wrapper == null)
			return;
		final FLEFRecord entityBlock = wrapper.getTheOnlyChild();
		if(entityBlock == null)
			return;
		final FLEFRecord idRef = entityBlock.getTheOnlyChild();
		if(idRef != null)
			idRef.setValue(newId);
		else
			entityBlock.setValue(newId);
	}

}
