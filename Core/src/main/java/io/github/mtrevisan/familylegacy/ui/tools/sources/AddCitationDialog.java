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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.ComboBoxSearch;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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

	private final JComboBox<String> stanceCombo = ComboBoxSearch.searchable(new JComboBox<>(SourceCitationReader.STANCES));

	private FLEFRecord targetRecord;
	private FLEFRecord sourceRecord;


	public AddCitationDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.add_citation.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(560, 300));

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
		form.add(new JLabel(I18N.l("label.target_record")), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		targetField.setEditable(false);
		form.add(targetField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickTarget = new JButton(I18N.t("button.choose"));
		pickTarget.addActionListener(e -> chooseTarget());
		form.add(pickTarget, gbc);

		// Source row.
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel(I18N.l("label.source")), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		sourceField.setEditable(false);
		form.add(sourceField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		final JButton pickSource = new JButton(I18N.t("button.choose"));
		pickSource.addActionListener(e -> chooseSource());
		form.add(pickSource, gbc);

		// Locator row.
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		form.add(new JLabel(I18N.l("label.locator")), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(locatorField, gbc);
		gbc.gridwidth = 1;

		stanceCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean selected, final boolean focus){
				return super.getListCellRendererComponent(list, I18N.t("enum.source_citation.stance." + value), index, selected, focus);
			}
		});
		// Stance row.
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
		form.add(new JLabel(I18N.l("label.stance")), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2;
		form.add(stanceCombo, gbc);
		gbc.gridwidth = 1;

		// Hint.
		gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
		gbc.insets = new Insets(12, 4, 4, 4);
		final JLabel hint = new JLabel("<html><i>" + I18N.t("dialog.add_citation.hint") + "</i></html>");
		hint.setForeground(Color.GRAY);
		form.add(hint, gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton ok = new JButton(I18N.t("button.continue"));
		ok.addActionListener(e -> onContinue());
		buttons.add(ok);


		return buttons;
	}


	/* ======================================================================
	 *                          Actions
	 * ====================================================================== */

	private void chooseTarget(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final FLEFModel model = context.model();
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, model,
			(record, handler) -> chosen[0] = record,
			IndividualHandler.class, GroupHandler.class, EventHandler.class, PlaceHandler.class, RelationshipHandler.class);
		dialog.setVisible(true);

		if(chosen[0] != null){
			targetRecord = chosen[0];
			targetField.setText(describe(chosen[0]));
		}
	}

	private void chooseSource(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final FLEFModel model = context.model();
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, model,
			(record, handler) -> chosen[0] = record,
			SourceHandler.class);
		dialog.setVisible(true);

		if(chosen[0] != null){
			sourceRecord = chosen[0];
			sourceField.setText(SourceHelper.sourceTitle(chosen[0]));
		}
	}

	private void onContinue(){
		if(targetRecord == null){
			JOptionPane.showMessageDialog(this,
				I18N.t("dialog.add_citation.no_target"), I18N.t("dialog.add_citation.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(sourceRecord == null){
			JOptionPane.showMessageDialog(this,
				I18N.t("dialog.add_citation.no_source"), I18N.t("dialog.add_citation.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}

		addCitation(targetRecord, sourceRecord, locatorField.getText(), (String)stanceCombo.getSelectedItem());

		final String message = I18N.tf("dialog.add_citation.done", SourceHelper.sourceTitle(sourceRecord),
			RecordDisplay.describe(targetRecord, context.model()));
		dispose();

		//the editor of the record is offered, to complete the citation (extract, evidence qualifiers)
		final RecordDialog editor = openEditorFor(targetRecord);
		if(editor != null && JOptionPane.showConfirmDialog(context.owner(), message + StringUtils.LF + StringUtils.LF
				+ I18N.t("dialog.add_citation.open_editor"), I18N.t("dialog.add_citation.title"), JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
			editor.setVisible(true);
		else if(editor == null)
			JOptionPane.showMessageDialog(context.owner(), message, I18N.t("dialog.add_citation.title"), JOptionPane.INFORMATION_MESSAGE);
	}

	/** Adds to the record a citation of the source, as its editor would: the locator is kept only if it is not blank. */
	static FLEFRecord addCitation(final FLEFRecord target, final FLEFRecord source, final String locator, final String stance){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, source.getId()));
		if(StringUtils.isNotBlank(locator))
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, locator.strip()));
		citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
		target.addChild(citation);
		return citation;
	}

	private RecordDialog openEditorFor(final FLEFRecord record){
		final Window owner = context.owner();
		final FLEFModel model = context.model();
		if(IndividualHandler.isOfType(record))
			return IndividualHandler.getInstance().createEditDialog(owner, model, record);
		if(GroupHandler.isOfType(record))
			return GroupHandler.getInstance().createEditDialog(owner, model, record);
		if(EventHandler.isOfType(record))
			return EventHandler.getInstance().createEditDialog(owner, model, record);
		if(PlaceHandler.isOfType(record))
			return PlaceHandler.getInstance().createEditDialog(owner, model, record);
		if(RelationshipHandler.isOfType(record))
			return RelationshipHandler.getInstance().createEditDialog(owner, model, record);
		return null;
	}

	private String describe(final FLEFRecord record){
		return RecordDisplay.describe(record, context.model());
	}

}
