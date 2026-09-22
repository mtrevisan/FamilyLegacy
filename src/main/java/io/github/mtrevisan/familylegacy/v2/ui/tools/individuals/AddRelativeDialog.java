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
package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * Generic dialog that adds a relative (parent, spouse, child, sibling)
 * to a target individual.
 * <p>
 * The dialog handles four roles with a single implementation:
 * <ul>
 *   <li>{@code PARENT} — creates one biological-child relationship from
 *       the target to the chosen parent. The role (father or mother) is
 *       chosen by sex when known, or by the {@code sex} field of the
 *       new record;</li>
 *   <li>{@code SPOUSE} — creates one spouse relationship between the
 *       target and the chosen partner;</li>
 *   <li>{@code CHILD} — creates one biological-child relationship
 *       from the chosen child to the target (and to the target's
 *       partner, when one exists);</li>
 *   <li>{@code SIBLING} — creates biological-child relationships from
 *       the chosen sibling to every parent of the target.</li>
 * </ul>
 * For each role, the user can either create a new individual or pick an
 * existing one. The dialog never guesses: when a piece of information
 * is missing, the user is asked to provide it.
 */
public final class AddRelativeDialog extends JDialog{

	/** The role of the relative to add. */
	public enum Role{ PARENT, CHILD, SIBLING }

	private final ToolContext context;
	private final Role role;
	private final String targetId;

	private final JTextField targetField = new JTextField(24);
	private final JTextField otherField = new JTextField(24);
	private final JComboBox<String> relationTypeCombo = new JComboBox<>();

	private FLEFRecord other;


	public AddRelativeDialog(final ToolContext context, final Role role, final String targetId){
		super(context.owner(), titleFor(role), ModalityType.APPLICATION_MODAL);

		this.context = context;
		this.role = role;
		this.targetId = targetId;

		targetField.setEditable(false);
		otherField.setEditable(false);
		fillRelationTypes();

		final FLEFRecord target = context.model().getRecordById(targetId);
		targetField.setText(target != null
			? IndividualHelper.displayName(target) + "  [" + targetId + "]"
			: targetId);

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(560, 260));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());
	}


	private static String titleFor(final Role role){
		return switch(role){
			case PARENT -> "Add Parent";
			case CHILD -> "Add Child";
			case SIBLING -> "Add Sibling";
		};
	}

	private void fillRelationTypes(){
		final List<String> types = switch(role){
			case PARENT, CHILD, SIBLING -> IndividualHelper.CHILD_RELATION_TYPES;
		};
		for(final String t : types)
			relationTypeCombo.addItem(t);
	}

	private JPanel createForm(){
		final JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel(labelForTarget(role)), gbc);
		gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
		form.add(targetField, gbc);
		gbc.gridwidth = 1;

		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel(labelForOther(role)), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(otherField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickExisting = new JButton("Choose…");
		pickExisting.addActionListener(e -> chooseExisting());
		form.add(pickExisting, gbc);
		gbc.gridx = 3;
		final JButton createNew = new JButton("New…");
		createNew.addActionListener(e -> createNew());
		form.add(createNew, gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		form.add(new JLabel("Relation type:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
		form.add(relationTypeCombo, gbc);

		return form;
	}

	private static String labelForTarget(final Role role){
		return switch(role){
			case PARENT -> "Child:";
			case CHILD -> "Parent:";
			case SIBLING -> "Individual:";
		};
	}

	private static String labelForOther(final Role role){
		return switch(role){
			case PARENT -> "Parent:";
			case CHILD -> "Child:";
			case SIBLING -> "Sibling:";
		};
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Add");
		ok.addActionListener(e -> onConfirm());
		buttons.add(ok);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	private void chooseExisting(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			other = chosen[0];
			otherField.setText(IndividualHelper.displayName(other) + "  [" + other.getId() + "]");
		}
	}

	private void createNew(){
		final BaseRecordDialog dialog = IndividualHandler.getInstance()
			.createNewDialog(this, context.model());
		dialog.setVisible(true);
		if(dialog.isSaved() && dialog.getRecord() != null){
			other = dialog.getRecord();
			otherField.setText(IndividualHelper.displayName(other) + "  [" + other.getId() + "]");
		}
	}

	private void onConfirm(){
		if(other == null){
			JOptionPane.showMessageDialog(this,
				"Choose or create the " + labelForOther(role).replace(":", StringUtils.EMPTY).toLowerCase() + " first.",
				titleFor(role), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(other.getId() != null && other.getId().equals(targetId)){
			JOptionPane.showMessageDialog(this,
				"The two individuals must be different.",
				titleFor(role), JOptionPane.WARNING_MESSAGE);
			return;
		}

		final String relationType = (String)relationTypeCombo.getSelectedItem();
		if(relationType == null)
			return;

		try{
			switch(role){
				case PARENT -> linkParent(relationType);
				case CHILD -> linkChild(relationType);
				case SIBLING -> linkSibling(relationType);
			}
			dispose();
		}
		catch(final RuntimeException e){
			JOptionPane.showMessageDialog(this,
				"Unable to create the relationship:\n" + e.getMessage(),
				titleFor(role), JOptionPane.ERROR_MESSAGE);
		}
	}


	/* ======================================================================
	 *                          Relationship creation
	 * ====================================================================== */

	/**
	 * Adds the chosen individual as a parent of the target. When the
	 * target already has a parent of the same sex, the existing
	 * relationship is replaced: the new parent takes the place of the
	 * old one, so the model never ends up with two fathers or two
	 * mothers.
	 */
	private void linkParent(final String relationType){
		final String parentSex = IndividualHelper.sex(other);
		if(parentSex != null && IndividualHelper.hasParentOfSex(context.model(), targetId, parentSex)){
			final int replace = JOptionPane.showConfirmDialog(this,
				"The individual already has a " + parentSex + " parent.\n"
					+ "Replace the existing one?",
				"Replace Parent", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(replace != JOptionPane.YES_OPTION)
				return;
			removeExistingParentOfSex(parentSex);
		}
		IndividualHelper.createRelationship(context.model(), targetId, other.getId(),
			relationType, io.github.mtrevisan.familylegacy.v2.ui.handlers
				.RelationshipHandler.ID_PREFIX);
	}

	private void removeExistingParentOfSex(final String parentSex){
		final List<String> parents = IndividualHelper.biologicalParentIds(context.model(), targetId);
		for(final String parentId : parents){
			final FLEFRecord parent = context.model().getRecordById(parentId);
			if(parent == null)
				continue;
			if(parentSex.equalsIgnoreCase(IndividualHelper.sex(parent))){
				for(final String relId : IndividualHelper.relationshipIdsForIndividual(
					context.model(), targetId)){
					final FLEFRecord rel = context.model().getRecordById(relId);
					if(rel == null)
						continue;
					final String type = io.github.mtrevisan.familylegacy.v2.io.model
						.FLEFRecordHelper.getChildValue(rel, IndividualHelper.TAG_TYPE);
					if(!IndividualHelper.REL_BIOLOGICAL_CHILD.equalsIgnoreCase(type))
						continue;
					final String subject = rel.extractReferencedId(
						IndividualHelper.TAG_SUBJECT, IndividualHelper.TYPE_INDIVIDUAL);
					final String target = rel.extractReferencedId(
						IndividualHelper.TAG_TARGET, IndividualHelper.TYPE_INDIVIDUAL);
					if(targetId.equals(subject) && parentId.equals(target)){
						context.model().removeRecord(relId);
						return;
					}
				}
			}
		}
	}

	/**
	 * Adds a spouse relationship between the target and the chosen
	 * individual. Before creating the new relationship, the dialog asks
	 * whether to keep or replace any existing spouse relationships of
	 * the target.
	 */
	private void linkSpouse(final String relationType){
		final String existing = IndividualHelper.firstSpouseId(context.model(), targetId);
		if(existing != null){
			final int choice = JOptionPane.showConfirmDialog(this,
				"The individual already has a recorded spouse.\n"
					+ "Add an additional spouse relationship?",
				"Additional Spouse", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(choice != JOptionPane.YES_OPTION)
				return;
		}
		IndividualHelper.createRelationship(context.model(), targetId, other.getId(),
			relationType, io.github.mtrevisan.familylegacy.v2.ui.handlers
				.RelationshipHandler.ID_PREFIX);
	}

	/**
	 * Adds a biological-child relationship from the chosen child to the
	 * target. When the target has a recorded spouse, the child is linked
	 * to both parents in a single operation.
	 */
	private void linkChild(final String relationType){
		IndividualHelper.createRelationship(context.model(), other.getId(), targetId,
			relationType, io.github.mtrevisan.familylegacy.v2.ui.handlers
				.RelationshipHandler.ID_PREFIX);

		final String spouseId = IndividualHelper.firstSpouseId(context.model(), targetId);
		if(spouseId != null){
			final int alsoLink = JOptionPane.showConfirmDialog(this,
				"Also link the child to the spouse of the parent?",
				"Link Both Parents", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(alsoLink == JOptionPane.YES_OPTION)
				IndividualHelper.createRelationship(context.model(), other.getId(), spouseId,
					relationType, io.github.mtrevisan.familylegacy.v2.ui.handlers
						.RelationshipHandler.ID_PREFIX);
		}
	}

	/**
	 * Adds the chosen individual as a sibling of the target, by linking
	 * it to every parent of the target with the same relationship type.
	 * When the target has no recorded parent, the operation cannot
	 * proceed: the dialog tells the user and stops.
	 */
	private void linkSibling(final String relationType){
		final List<String> parents = IndividualHelper.biologicalParentIds(
			context.model(), targetId);
		if(parents.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"The individual has no recorded parent.\n"
					+ "Add at least one parent before adding a sibling.",
				"Add Sibling", JOptionPane.WARNING_MESSAGE);
			return;
		}
		for(final String parentId : parents)
			IndividualHelper.createRelationship(context.model(), other.getId(), parentId,
				relationType, io.github.mtrevisan.familylegacy.v2.ui.handlers
					.RelationshipHandler.ID_PREFIX);
	}

}
