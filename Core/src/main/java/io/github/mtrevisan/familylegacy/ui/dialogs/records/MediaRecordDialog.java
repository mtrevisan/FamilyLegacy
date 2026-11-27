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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.bindings.MediaUriField;
import io.github.mtrevisan.familylegacy.ui.components.media.MediaViewerDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordUsageDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.DateField;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.io.IOException;


/**
 * Dialog for editing a {@code MEDIA_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record MediaRecord {
 *   uri: Uri
 *   kind: enum { image, document, audio, video }
 *   mapping?: enum { planar, spherical_equirectangular, spherical_uv, cubemap, cylindrical_equirectangular_horizontal, cylindrical_equirectangular_vertical } | Text
 *   description?: Text
 *   note*: NoteStructure
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): uri, kind, mapping, description
 * Tab 6 (Research): ResearchQuestionRecord (target[media] = this media)
 * Tab 7 (Sources): SourceRecord (media contains this media)
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class MediaRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final MediaUriField uriField;
	private final BoundComboBox<String> kindCombo;
	private final BoundComboBox<String> mappingCombo;
	private final JButton moreButton = new JButton(I18N.t("dialog.media.more"));
	private final DateField dateField;
	private final BoundTextArea descriptionArea;


	public static MediaRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, MediaRecordDialog::new);
	}

	public static MediaRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, MediaRecordDialog::new);
	}


	private MediaRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, MediaHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]5[]5[]10[]");

		uriField = new MediaUriField(MediaReader.TAG_URI);
		BindingsHelper.installBehavior(uriField,
			this::setNewItem, null,
			null, null,
			builder -> {
				builder.item(I18N.t("popupmenu.set"), this::setNewItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.clear"), this::clearItem);
			});
		kindCombo = new BoundComboBox<>(MediaReader.TAG_KIND, GUIHelper.fillCombo(MediaReader.KINDS, null));
		kindCombo.setI18NPrefix("enum.media.kind");
		mappingCombo = new BoundComboBox<>(MediaReader.TAG_MAPPING, GUIHelper.fillCombo(MediaReader.MAPPINGS, null));
		mappingCombo.setI18NPrefix("enum.media.mapping");
		mappingCombo.setEditable(true);
		dateField = DateField.createWithWrapperTag(MediaReader.TAG_DATE, this, I18N.t("dialog.media.date"), model);
		descriptionArea = new BoundTextArea(MediaReader.TAG_DESCRIPTION, 3, 25);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.RESEARCH_TASK_ON_RELATED, ResearchTaskHandler.TYPE, I18N.t("dialog.component.research.tasks"))
			.withComponent(PanelKey.SOURCE_ON_DOCUMENT, SourceHandler.TYPE, I18N.t("dialog.component.sources"))
			.withComponent(PanelKey.NOTE, MediaReader.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, MediaReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, MediaReader.TAG_AUDIT, null)
			.build();

		components.bind(uriField);
		components.bind(kindCombo);
		components.bind(mappingCombo);
		components.bind(descriptionArea);


		finalizeDialog(parent);
	}

	private void setNewItem(){
		final File selectedFile = GUIHelper.selectDocumentFile(getParent(), uriField.getText());
		if(selectedFile != null){
			uriField.setText(selectedFile.getAbsolutePath());
			if(!kindCombo.isValued())
				kindCombo.setText(MediaReader.inferKind(selectedFile.getName()));
		}
	}

	private void clearItem(){
		uriField.clear();
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// file
		GUIHelper.addLabeledComponent(propertiesPanel, GUIHelper.requiredLabel("dialog.media.uri"), uriField);

		// kind
		GUIHelper.addLabeledComponent(propertiesPanel, GUIHelper.requiredLabel("dialog.media.kind"), kindCombo);

		// mapping
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.media.mapping"), mappingCombo);

		// date of creation or capture
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.media.date"), dateField);

		// description
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.media.description"), descriptionArea);

		//the mapping of the image (spherical, cubemap...) is behind More...: a photograph does not need it
		moreButton.setToolTipText(I18N.t("dialog.media.more.tooltip"));
		moreButton.addActionListener(e -> {
			showMapping(true);
			mappingCombo.requestFocusInWindow();
		});
		propertiesPanel.add(moreButton, "skip 1,align left");

		//every record that uses the media (as a source media, as the media of an extract, as the preferred image or in a media citation)
		final JButton whereUsedButton = new JButton(I18N.t("dialog.media.where_used"));
		whereUsedButton.setEnabled(!isNew);
		if(isNew)
			whereUsedButton.setToolTipText(I18N.t("dialog.media.save_first"));
		whereUsedButton.addActionListener(e -> RecordUsageDialog.show(this, model, record));

		//the image at full size, with the regions that tell who is in it (only for the media that is saved: the viewer reads the record)
		final JButton viewButton = new JButton(I18N.t("dialog.media.view"));
		viewButton.setEnabled(!isNew);
		if(isNew)
			viewButton.setToolTipText(I18N.t("dialog.media.save_first"));
		viewButton.addActionListener(e -> MediaViewerDialog.open(this, model, record));

		//the folder that holds the file, with the file selected
		final JButton viewInFolderButton = new JButton(I18N.t("dialog.media.view_in_folder"));
		viewInFolderButton.addActionListener(e -> {
			final String path = uriField.getText();
			if(path != null && !path.isBlank())
				SavedFileNotice.reveal(new File(path.trim()));
		});

		final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		buttonsPanel.setOpaque(false);
		buttonsPanel.add(whereUsedButton);
		buttonsPanel.add(viewButton);
		buttonsPanel.add(viewInFolderButton);
		GUIHelper.addComponent(propertiesPanel, buttonsPanel);

		//until the record is loaded the mapping is not in sight (loadData shows it for a record that has one)
		showMapping(false);

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// research question
		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		// research tasks that name this record
		GUIHelper.addComponent(panel, components.getPanel(PanelKey.RESEARCH_TASK_ON_RELATED));

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE_ON_DOCUMENT);
		GUIHelper.addComponent(panel, sourcePanel);

		return panel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.NOTE);
		GUIHelper.addComponent(panel, notePanel);

		return panel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		components.load(record);
		dateField.load(record);

		//the mapping is for the images of 360 degrees: it is in sight only for a record that has one that is not the ordinary one
		showMapping(mappingCombo.isValued() && !MediaReader.ENUM_MAPPING_PLANAR.equalsIgnoreCase(mappingCombo.getText()));
	}

	/** Shows the row of the mapping (and takes away the button that shows it), or hides it. */
	private void showMapping(final boolean visible){
		final JLabel label = GUIHelper.getLabeledComponent(mappingCombo);
		if(label != null)
			label.setVisible(visible);
		mappingCombo.setVisible(visible);
		moreButton.setVisible(!visible);

		propertiesPanel.revalidate();
		propertiesPanel.repaint();
	}

	@Override
	protected boolean validData(){
		if(uriField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.media.uri")),
				tabbedPane, propertiesPanel, uriField);

			return false;
		}
		if(!kindCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.media.kind")),
				tabbedPane, propertiesPanel, kindCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);
		dateField.save(record);
	}


	static void main(final String[] args) throws IOException{
		GUIHelper.launch(MediaRecordDialog::createEdit, "/tests/test.flef", "D1");
	}

}
