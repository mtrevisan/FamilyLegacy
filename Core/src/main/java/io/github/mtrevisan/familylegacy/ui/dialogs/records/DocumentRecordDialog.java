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
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.DocumentReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.File;
import java.io.IOException;


/**
 * Dialog for editing a {@code DOCUMENT_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record DocumentRecord {
 *   id: LocalID
 *   uri: Uri
 *   mapping?: enum { spherical_UV, cylindrical_equirectangular_horizontal, cylindrical_equirectangular_vertical } | Text
 *   description?: Text
 *   note*: Xref&lt;NoteRecord&gt;
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): uri, mapping, description
 * Tab 6 (Research): ResearchQuestionRecord (target[document] = this document)
 * Tab 7 (Sources): SourceRecord (document contains this document)
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class DocumentRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final BoundTextField uriField;
	private final BoundComboBox<String> mappingCombo;
	private final BoundTextArea descriptionArea;


	public static DocumentRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, DocumentRecordDialog::new);
	}

	public static DocumentRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, DocumentRecordDialog::new);
	}


	private DocumentRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, DocumentHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]");

		uriField = new BoundTextField(DocumentReader.TAG_URI);
		BindingsHelper.installBehavior(uriField,
			this::setNewItem, null,
			null, null,
			builder -> {
				builder.item(I18N.t("popupmenu.set"), this::setNewItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.clear"), uriField::clear);
			});
		mappingCombo = new BoundComboBox<>(DocumentReader.TAG_MAPPING, GUIHelper.fillCombo(DocumentReader.MAPPINGS, null));
		mappingCombo.setI18NPrefix("enum.document.mapping");
		mappingCombo.setEditable(true);
		descriptionArea = new BoundTextArea(DocumentReader.TAG_DESCRIPTION, 3, 25);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE_ON_DOCUMENT, SourceHandler.TYPE, I18N.t("dialog.component.sources"))
			.withComponent(PanelKey.NOTE, DocumentReader.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, DocumentReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, DocumentReader.TAG_AUDIT, null)
			.build();

		components.bind(uriField);
		components.bind(mappingCombo);
		components.bind(descriptionArea);


		finalizeDialog(parent);
	}

	private void setNewItem(){
		final File selectedFile = GUIHelper.selectImageFile(getParent(), uriField.getText());
		if(selectedFile != null)
			uriField.setText(selectedFile.getAbsolutePath());
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// file
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.document.uri") + "*:", uriField);

		// mapping
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.document.mapping") + ":", mappingCombo);

		// description
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.document.description") + ":", descriptionArea);

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
				I18N.tf("validation.required", I18N.t("dialog.document.uri")),
				tabbedPane, propertiesPanel, uriField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(DocumentRecordDialog::createEdit, "/tests/test.flef", "D1");
	}

}
