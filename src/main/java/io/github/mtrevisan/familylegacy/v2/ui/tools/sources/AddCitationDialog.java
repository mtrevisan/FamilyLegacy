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
package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

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
import java.awt.Window;


/**
 * Dialog that helps the user attach a {@code SourceCitation} to a target
 * record.
 * <p>
 * The user picks the target record (any record type that can carry a
 * citation), picks the source, fills in the locator, and chooses whether
 * to add an extract. When the target has no editor yet, the dialog
 * simply records the intent and offers to open the source for editing.
 * <p>
 * The dialog is deliberately thin: it does not duplicate the citation
 * editor that lives inside the target record's dialog. Its job is to
 * give the user a starting point when they already know which source
 * they want to cite but do not want to navigate to the target first.
 */
public final class AddCitationDialog extends JDialog{

	private final ToolContext context;

	private final JTextField targetField = new JTextField(28);
	private final JTextField sourceField = new JTextField(28);
	private final JTextField locatorField = new JTextField(28);

	private FLEFRecord targetRecord;
	private FLEFRecord sourceRecord;


	public AddCitationDialog(final ToolContext context){
		super(context.owner(), "Add Citation", ModalityType.APPLICATION_MODAL);

		this.context = context;

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

		// Target row.
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel("Target record:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		targetField.setEditable(false);
		form.add(targetField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickTarget = new JButton("Choose…");
		pickTarget.addActionListener(e -> chooseTarget());
		form.add(pickTarget, gbc);

		// Source row.
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel("Source:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		sourceField.setEditable(false);
		form.add(sourceField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickSource = new JButton("Choose…");
		pickSource.addActionListener(e -> chooseSource());
		form.add(pickSource, gbc);

		// Locator row.
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		form.add(new JLabel("Locator:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(locatorField, gbc);
		gbc.gridwidth = 1;

		// Hint.
		gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
		gbc.insets = new Insets(12, 4, 4, 4);
		final JLabel hint = new JLabel("<html><i>After confirming, the dialog opens the "
			+ "target record's editor so the citation can be completed in place.</i></html>");
		hint.setForeground(java.awt.Color.GRAY);
		form.add(hint, gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton("Continue");
		ok.addActionListener(e -> onContinue());
		buttons.add(ok);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(cancel);

		return buttons;
	}


	/* ======================================================================
	 *                          Actions
	 * ====================================================================== */

	private void chooseTarget(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler.class,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler.class,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler.class,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler.class,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			targetRecord = chosen[0];
			targetField.setText(describe(chosen[0]));
		}
	}

	private void chooseSource(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			sourceRecord = chosen[0];
			sourceField.setText(SourceHelper.sourceTitle(chosen[0]));
		}
	}

	private void onContinue(){
		if(targetRecord == null){
			JOptionPane.showMessageDialog(this,
				"Choose a target record first.",
				"Add Citation", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(sourceRecord == null){
			JOptionPane.showMessageDialog(this,
				"Choose a source first.",
				"Add Citation", JOptionPane.WARNING_MESSAGE);
			return;
		}

		dispose();

		// Open the target record's editor. The editor already contains
		// the Sources tab where the user can add the citation with the
		// locator just entered.
		final BaseRecordDialog editor = openEditorFor(targetRecord);
		if(editor == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No editor is wired for the target record type "
					+ targetRecord.getTag() + ".\n"
					+ "Open the record manually to add the citation.",
				"Add Citation", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		editor.setVisible(true);

		// Pre-fill hint: after the editor closes, if the user saved, the
		// citation (if any) has already been attached. The locator is not
		// injected automatically because the editor manages its own
		// citation list; the user pastes it there.
	}

	private BaseRecordDialog openEditorFor(final FLEFRecord record){
		final Window owner = context.owner();
		final FLEFModel model = context.model();
		final String tag = record.getTag();
		if("individual".equalsIgnoreCase(tag))
			return io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler.getInstance()
				.createEditDialog(owner, model, record);
		if("source".equalsIgnoreCase(tag))
			return SourceHandler.getInstance().createEditDialog(owner, model, record);
		if("event".equalsIgnoreCase(tag))
			return io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler.getInstance()
				.createEditDialog(owner, model, record);
		if("place".equalsIgnoreCase(tag))
			return io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler.getInstance()
				.createEditDialog(owner, model, record);
		if("relationship".equalsIgnoreCase(tag))
			return io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler.getInstance()
				.createEditDialog(owner, model, record);
		return null;
	}

	private static String describe(final FLEFRecord record){
		return record.getTag() + StringUtils.SPACE + record.getId();
	}

}
