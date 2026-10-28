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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaRecordDialog.class);


	private final JPanel propertiesPanel;

	private final BoundTextField uriField;
	private final BoundComboBox<String> kindCombo;
	private final BoundComboBox<String> mappingCombo;
	private final BoundTextArea descriptionArea;


	public static MediaRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, MediaRecordDialog::new);
	}

	public static MediaRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, MediaRecordDialog::new);
	}


	private MediaRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, MediaHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]5[]10[]");

		uriField = new BoundTextField(MediaReader.TAG_URI);
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
		descriptionArea = new BoundTextArea(MediaReader.TAG_DESCRIPTION, 3, 25);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
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
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.media.uri") + "*:", uriField);

		// kind
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.media.kind") + "*:", kindCombo);

		// mapping
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.media.mapping") + ":", mappingCombo);

		// description
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.media.description") + ":", descriptionArea);

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		// research question
		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

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
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(MediaRecordDialog::createEdit, "/tests/test.flef", "D1");
	}

}
