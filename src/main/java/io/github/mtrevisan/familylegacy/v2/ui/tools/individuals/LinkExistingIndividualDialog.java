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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
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
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog that creates an arbitrary relationship between two existing
 * individuals.
 * <p>
 * Unlike the "add relative" dialog, which fixes the semantics from the
 * role (parent, spouse, child, sibling), this dialog exposes the
 * relationship type explicitly: the user picks a subject, a target, and
 * a type, and the model is updated accordingly. This is the right tool
 * for less common cases: a step-parent, a foster child, an adoption, an
 * engagement, and so on.
 * <p>
 * The dialog displays a live preview of the relationship being created,
 * so the user can verify the direction of the edge before confirming.
 */
public final class LinkExistingIndividualDialog extends JDialog{

	private final ToolContext context;

	private final JTextField subjectField = new JTextField(24);
	private final JTextField targetField = new JTextField(24);
	private final JComboBox<String> typeCombo = new JComboBox<>();
	private final JLabel previewLabel = new JLabel(StringUtils.SPACE);

	private FLEFRecord subject;
	private FLEFRecord target;


	public LinkExistingIndividualDialog(final ToolContext context){
		super(context.owner(), "Link Existing Individual", ModalityType.APPLICATION_MODAL);

		this.context = context;

		subjectField.setEditable(false);
		targetField.setEditable(false);
		fillTypeCombo();

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(620, 320));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		updatePreview();
	}

	private void fillTypeCombo(){
		final List<String> types = new ArrayList<>();
		types.addAll(IndividualHelper.CHILD_RELATION_TYPES);
		types.addAll(IndividualHelper.SPOUSE_RELATION_TYPES);
		for(final String t : types)
			typeCombo.addItem(t);
		typeCombo.addActionListener(e -> updatePreview());
	}

	private JPanel createForm(){
		final JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel("Subject:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(subjectField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickSubject = new JButton("Choose…");
		pickSubject.addActionListener(e -> chooseSubject());
		form.add(pickSubject, gbc);

		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel("Target:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(targetField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickTarget = new JButton("Choose…");
		pickTarget.addActionListener(e -> chooseTarget());
		form.add(pickTarget, gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		form.add(new JLabel("Relation type:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(typeCombo, gbc);
		gbc.gridwidth = 1;

		gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
		gbc.insets = new Insets(12, 4, 4, 4);
		previewLabel.setForeground(java.awt.Color.DARK_GRAY);
		form.add(previewLabel, gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Link");
		ok.addActionListener(e -> onConfirm());
		buttons.add(ok);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	private void chooseSubject(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			subject = chosen[0];
			subjectField.setText(IndividualHelper.displayName(subject)
				+ "  [" + subject.getId() + "]");
			updatePreview();
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
			updatePreview();
		}
	}

	private void updatePreview(){
		if(subject == null || target == null){
			previewLabel.setText(StringUtils.SPACE);
			return;
		}
		final String type = (String)typeCombo.getSelectedItem();
		if(type == null){
			previewLabel.setText(StringUtils.SPACE);
			return;
		}
		previewLabel.setText("<html><i>"
			+ escape(IndividualHelper.displayName(subject))
			+ " is the <b>" + escape(type) + "</b> of "
			+ escape(IndividualHelper.displayName(target))
			+ "</i></html>");
	}

	private void onConfirm(){
		if(subject == null || target == null){
			JOptionPane.showMessageDialog(this,
				"Choose both a subject and a target.",
				"Link Existing Individual", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(subject.getId().equals(target.getId())){
			JOptionPane.showMessageDialog(this,
				"The subject and the target must be different individuals.",
				"Link Existing Individual", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final String type = (String)typeCombo.getSelectedItem();
		if(type == null)
			return;

		IndividualHelper.createRelationship(context.model(), subject.getId(), target.getId(),
			type, RelationshipHandler.ID_PREFIX);
		dispose();
	}

	private static String escape(final String s){
		return ReportDialog.escape(s);
	}

}
