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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ImagePreviewAccessory;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Dialog;
import java.io.File;
import java.io.Serial;
import java.util.function.Consumer;


/* ONGOING */
/**
 * Dialog for editing a {@code DOCUMENT_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record DocumentRecord {
 *   id: LocalID
 *   file: Uri
 *   mapping?: enum { spherical_UV, cylindrical_equirectangular_horizontal, cylindrical_equirectangular_vertical } | Text
 *   description?: Text
 *   note*: Xref&lt;NoteRecord&gt;
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): file, mapping, description
 * Tab 6 (Research): ResearchQuestionRecord (target.document = this document)
 * Tab 7 (Sources): SourceRecord (document contains this document)
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class DocumentRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 6128827794273719284L;


	private static final String TAG_FILE = "FILE";
	private static final String TAG_MAPPING = "MAPPING";
	private static final String TAG_DESCRIPTION = "DESCRIPTION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField fileField;
	private final BoundComboBox<String> mappingCombo;
	private final BoundTextArea descriptionArea;


	public static DocumentRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, DocumentRecordDialog::new);
	}

	public static DocumentRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, DocumentRecordDialog::new);
	}


	private DocumentRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, DocumentHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]");

		fileField = new BoundTextField(TAG_FILE);
		GUIHelper.installBehavior(fileField,
			null, null,
			null, null,
			builder -> {
				builder.item("Set…", this::setNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Clear", fileField::clear);
			});
		mappingCombo = new BoundComboBox<>(TAG_MAPPING, new String[]{
			StringUtils.EMPTY,
			"planar", "spherical_equirectangular", "spherical_uv", "cubemap", "cylindrical_equirectangular_horizontal",
			"cylindrical_equirectangular_vertical"});
		mappingCombo.setEditable(true);
		descriptionArea = new BoundTextArea(TAG_DESCRIPTION, 3, 25);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, DocumentHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(fileField);
		components.bind(mappingCombo);
		components.bind(descriptionArea);


		finalizeDialog(parent);
	}

	private void setNewItem(){
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select Image File");
		final String[] extensions = ImageIO.getReaderFileSuffixes();
		final String description = "Supported Images (" + String.join(", ", extensions) + ")";
		fileChooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
		fileChooser.setAccessory(new ImagePreviewAccessory(fileChooser));
		final int userSelection = fileChooser.showOpenDialog(getParent());
		if(userSelection != JFileChooser.APPROVE_OPTION)
			return;

		final File selectedFile = fileChooser.getSelectedFile();
		if(selectedFile == null || !selectedFile.exists()){
			JOptionPane.showMessageDialog(getParent(),
				"Selected file does not exist.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		fileField.setText(selectedFile.getAbsolutePath());
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// file
		GUIHelper.addLabeledComponent(propertiesPanel, "File*:", fileField);

		// mapping
		GUIHelper.addLabeledComponent(propertiesPanel, "Mapping:", mappingCombo);

		// description
		GUIHelper.addLabeledComponent(propertiesPanel, "Description:", descriptionArea);

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

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
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
		if(fileField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Document file is required.",
				tabbedPane, propertiesPanel, fileField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord document = FLEFRecord.createMainRecord("D1", "DOCUMENT");

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(document);
		};
		GUIHelper.launch(DocumentRecordDialog::createEdit, modelFiller, document);
	}

}
