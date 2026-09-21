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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Dialog that merges two individual records into one.
 * <p>
 * The merge proceeds by re-pointing, not by copying:
 * <ol>
 *   <li>every relationship whose subject or target is the source is
 *       re-pointed to the target;</li>
 *   <li>every event participation whose participant is the source is
 *       re-pointed to the target;</li>
 *   <li>every attribute record whose individual is the source is
 *       re-pointed to the target;</li>
 *   <li>the source record is deleted, together with any relationship
 *       that still refers to it.</li>
 * </ol>
 * The re-pointing is preferable to copying because it avoids creating
 * duplicate events, duplicate citations, and duplicate memberships that
 * would then have to be de-duplicated by hand. Sources, notes, and
 * audit blocks that were attached to the source are not carried over;
 * the dialog warns the user before proceeding.
 * <p>
 * Two records that are already linked by an {@code IdentityHypothesis}
 * are highlighted in the preview, so the user can see that the pair has
 * already been considered.
 */
public final class MergeIndividualsDialog extends JDialog{

	private final ToolContext context;

	private final JTextField sourceField = new JTextField(24);
	private final JTextField targetField = new JTextField(24);
	private final JLabel sourcePreview = new JLabel(" ");
	private final JLabel targetPreview = new JLabel(" ");

	private FLEFRecord source;
	private FLEFRecord target;


	public MergeIndividualsDialog(final ToolContext context){
		super(context.owner(), "Merge Individuals", ModalityType.APPLICATION_MODAL);

		this.context = context;

		sourceField.setEditable(false);
		targetField.setEditable(false);

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(760, 400));

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

		// Source row.
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel("Source (to be deleted):"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(sourceField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickSource = new JButton("Choose…");
		pickSource.addActionListener(e -> chooseSource());
		form.add(pickSource, gbc);

		// Source preview.
		gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
		sourcePreview.setForeground(Color.DARK_GRAY);
		sourcePreview.setFont(sourcePreview.getFont().deriveFont(
			sourcePreview.getFont().getSize2D() - 1f));
		form.add(sourcePreview, gbc);
		gbc.gridwidth = 1;

		// Target row.
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		form.add(new JLabel("Target (to be kept):"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(targetField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickTarget = new JButton("Choose…");
		pickTarget.addActionListener(e -> chooseTarget());
		form.add(pickTarget, gbc);

		// Target preview.
		gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2;
		targetPreview.setForeground(Color.DARK_GRAY);
		targetPreview.setFont(targetPreview.getFont().deriveFont(
			targetPreview.getFont().getSize2D() - 1f));
		form.add(targetPreview, gbc);
		gbc.gridwidth = 1;

		// Warning.
		gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
		gbc.insets = new Insets(16, 4, 4, 4);
		final JLabel warning = new JLabel("<html><i>Relationships, events, and attributes "
			+ "of the source will be re-pointed to the target.<br>The source's own "
			+ "sources, notes, and audit blocks will be lost.</i></html>");
		warning.setForeground(new Color(160, 60, 40));
		form.add(warning, gbc);

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
			io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			source = chosen[0];
			sourceField.setText(IndividualHelper.displayName(source)
				+ "  [" + source.getId() + "]");
			updatePreview(source, sourcePreview);
		}
	}

	private void chooseTarget(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			target = chosen[0];
			targetField.setText(IndividualHelper.displayName(target)
				+ "  [" + target.getId() + "]");
			updatePreview(target, targetPreview);
		}
	}

	private void updatePreview(final FLEFRecord individual, final JLabel label){
		final IndividualHelper.MergePreview preview =
			IndividualHelper.buildMergePreview(context.model(), individual.getId());
		label.setText(String.format(
			"%s, %s — parents: %d, children: %d, spouses: %d, events: %d",
			preview.name(), preview.sex() != null? preview.sex(): "sex unknown",
			preview.parentCount(), preview.childCount(),
			preview.spouseCount(), preview.eventCount()));
	}


	/* ======================================================================
	 *                          Merge execution
	 * ====================================================================== */

	private void onConfirm(){
		if(source == null || target == null){
			JOptionPane.showMessageDialog(this,
				"Choose both a source and a target.",
				"Merge Individuals", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(source.getId().equals(target.getId())){
			JOptionPane.showMessageDialog(this,
				"The source and the target must be different individuals.",
				"Merge Individuals", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final String message = "Merge " + IndividualHelper.displayName(source)
			+ " into " + IndividualHelper.displayName(target) + "?\n\n"
			+ "The source record will be deleted. Relationships, events, and "
			+ "attributes will be re-pointed to the target.";
		final int confirm = JOptionPane.showConfirmDialog(this, message,
			"Confirm Merge", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		final String sourceId = source.getId();
		final String targetId = target.getId();

		// 1. Re-point relationships, avoiding the creation of self-loops
		//    and of duplicate edges.
		repointRelationships(sourceId, targetId);

		// 2. Re-point event participations.
		repointEventParticipations(sourceId, targetId);

		// 3. Re-point attribute records.
		repointAttributeRecords(sourceId, targetId);

		// 4. Delete any remaining relationship that involves the source,
		//    then delete the source record itself.
		final List<String> leftovers = IndividualHelper.relationshipIdsForIndividual(
			context.model(), sourceId);
		for(final String relId : leftovers)
			context.model().removeRecord(relId);
		context.model().removeRecord(sourceId);

		dispose();
	}

	/**
	 * Re-points every relationship that has the source as an endpoint to
	 * the target. Relationships that would become self-loops (both
	 * endpoints equal after the re-point) are deleted, and duplicated
	 * edges are collapsed so the model does not end up with the same
	 * relationship twice.
	 */
	private void repointRelationships(final String sourceId, final String targetId){
		final Set<String> existingEdges = new LinkedHashSet<>();
		for(final FLEFRecord rel : context.model().getRecordsByType(
			IndividualHelper.TYPE_RELATIONSHIP)){
			final String subject = rel.extractReferencedId(
				IndividualHelper.TAG_SUBJECT, IndividualHelper.TYPE_INDIVIDUAL);
			final String target = rel.extractReferencedId(
				IndividualHelper.TAG_TARGET, IndividualHelper.TYPE_INDIVIDUAL);
			final String type = FLEFRecordHelper.getChildValue(rel,
				IndividualHelper.TAG_TYPE);
			if(subject != null && target != null && type != null
				&& !sourceId.equals(subject) && !sourceId.equals(target))
				existingEdges.add(edgeKey(subject, target, type));
		}

		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord rel : context.model().getRecordsByType(
			IndividualHelper.TYPE_RELATIONSHIP)){
			final String subject = rel.extractReferencedId(
				IndividualHelper.TAG_SUBJECT, IndividualHelper.TYPE_INDIVIDUAL);
			final String target = rel.extractReferencedId(
				IndividualHelper.TAG_TARGET, IndividualHelper.TYPE_INDIVIDUAL);
			final String type = FLEFRecordHelper.getChildValue(rel,
				IndividualHelper.TAG_TYPE);
			if(type == null)
				continue;

			final boolean sourceIsSubject = sourceId.equals(subject);
			final boolean sourceIsTarget = sourceId.equals(target);
			if(!sourceIsSubject && !sourceIsTarget)
				continue;

			final String newSubject = (sourceIsSubject? targetId: subject);
			final String newTarget = (sourceIsTarget? targetId: target);

			// Self-loop after re-point: the edge disappears.
			if(newSubject != null && newSubject.equals(newTarget)){
				toRemove.add(rel.getId());
				continue;
			}
			// Duplicate edge after re-point: the existing one wins.
			final String key = edgeKey(newSubject, newTarget, type);
			if(existingEdges.contains(key)){
				toRemove.add(rel.getId());
				continue;
			}
			// Re-point.
			if(sourceIsSubject)
				IndividualHelper.setRelationshipEndpoint(rel,
					IndividualHelper.TAG_SUBJECT, newSubject);
			if(sourceIsTarget)
				IndividualHelper.setRelationshipEndpoint(rel,
					IndividualHelper.TAG_TARGET, newTarget);
			existingEdges.add(key);
		}
		for(final String relId : toRemove)
			context.model().removeRecord(relId);
	}

	private static String edgeKey(final String subject, final String target, final String type){
		return (subject != null? subject: "?") + "|" + (target != null? target: "?")
			+ "|" + type;
	}

	/**
	 * Re-points every {@code event_participation} whose participant is
	 * the source to the target. Duplicates are collapsed: when the
	 * target already participates in the same event with the same role,
	 * the source's participation is deleted instead of re-pointed.
	 */
	private void repointEventParticipations(final String sourceId, final String targetId){
		final Set<String> existingParticipations = new LinkedHashSet<>();
		for(final FLEFRecord p : context.model().getRecordsByType("event_participation")){
			final String participant = participantId(p);
			final String eventId = FLEFRecordHelper.getChildValue(p, "event");
			final String role = FLEFRecordHelper.getChildValue(p, "role");
			if(participant != null && eventId != null && !sourceId.equals(participant))
				existingParticipations.add(participantKey(participant, eventId, role));
		}

		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord p : context.model().getRecordsByType("event_participation")){
			final String participant = participantId(p);
			if(!sourceId.equals(participant))
				continue;
			final String eventId = FLEFRecordHelper.getChildValue(p, "event");
			final String role = FLEFRecordHelper.getChildValue(p, "role");
			if(eventId == null){
				toRemove.add(p.getId());
				continue;
			}
			final String key = participantKey(targetId, eventId, role);
			if(existingParticipations.contains(key)){
				toRemove.add(p.getId());
				continue;
			}
			setParticipantId(p, targetId);
			existingParticipations.add(key);
		}
		for(final String id : toRemove)
			context.model().removeRecord(id);
	}

	private static String participantKey(final String participant, final String eventId,
		final String role){
		return participant + "|" + eventId + "|" + (role != null? role: "");
	}

	private String participantId(final FLEFRecord participation){
		final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation,
			"participant");
		if(participantBlock == null)
			return null;
		final FLEFRecord oneof = participantBlock.getTheOnlyChild();
		if(oneof == null || !IndividualHelper.TYPE_INDIVIDUAL.equalsIgnoreCase(oneof.getTag()))
			return null;
		final FLEFRecord ref = oneof.getTheOnlyChild();
		return (ref != null? ref.getValue(): oneof.getValue());
	}

	private void setParticipantId(final FLEFRecord participation, final String newId){
		final FLEFRecord participantBlock = FLEFRecordHelper.findChild(participation,
			"participant");
		if(participantBlock == null)
			return;
		final FLEFRecord oneof = participantBlock.getTheOnlyChild();
		if(oneof == null)
			return;
		final FLEFRecord ref = oneof.getTheOnlyChild();
		if(ref != null)
			ref.setValue(newId);
		else
			oneof.setValue(newId);
	}

	/**
	 * Re-points every {@code individual_attribute} whose individual is
	 * the source to the target. Duplicates are collapsed by type+value.
	 */
	private void repointAttributeRecords(final String sourceId, final String targetId){
		final Set<String> existingAttributes = new LinkedHashSet<>();
		for(final FLEFRecord attr : context.model().getRecordsByType("individual_attribute")){
			final String individualId = FLEFRecordHelper.getChildValue(attr, "individual");
			if(individualId == null || sourceId.equals(individualId))
				continue;
			existingAttributes.add(attributeKey(individualId,
				FLEFRecordHelper.getChildValue(attr, "type"),
				FLEFRecordHelper.getChildValue(attr, "value")));
		}

		final List<String> toRemove = new ArrayList<>();
		for(final FLEFRecord attr : context.model().getRecordsByType("individual_attribute")){
			final String individualId = FLEFRecordHelper.getChildValue(attr, "individual");
			if(!sourceId.equals(individualId))
				continue;
			final String type = FLEFRecordHelper.getChildValue(attr, "type");
			final String value = FLEFRecordHelper.getChildValue(attr, "value");
			final String key = attributeKey(targetId, type, value);
			if(existingAttributes.contains(key)){
				toRemove.add(attr.getId());
				continue;
			}
			setAttributeIndividualId(attr, targetId);
			existingAttributes.add(key);
		}
		for(final String id : toRemove)
			context.model().removeRecord(id);
	}

	private static String attributeKey(final String individualId, final String type,
		final String value){
		return individualId + "|" + (type != null? type: "") + "|" + (value != null? value: "");
	}

	private void setAttributeIndividualId(final FLEFRecord attr, final String newId){
		FLEFRecord individual = FLEFRecordHelper.findChild(attr, "individual");
		if(individual == null)
			return;
		final FLEFRecord ref = individual.getTheOnlyChild();
		if(ref != null)
			ref.setValue(newId);
		else
			individual.setValue(newId);
	}

}
