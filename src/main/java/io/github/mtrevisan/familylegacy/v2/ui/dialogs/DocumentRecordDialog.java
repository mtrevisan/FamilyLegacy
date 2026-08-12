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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ImagePreviewAccessory;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Dialog;
import java.io.File;
import java.io.Serial;


/* DONE */
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
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class DocumentRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 6128827794273719284L;


	private static final String TAG_FILE = "FILE";
	private static final String TAG_MAPPING = "MAPPING";
	private static final String TAG_DESCRIPTION = "DESCRIPTION";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new DocumentHandler());
		HandlerRegistry.register(new NoteHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField fileField;
	private final BoundComboBox<String> mappingCombo;
	private final BoundTextArea descriptionArea;
	private final EntityReferenceListPanel notePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static DocumentRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, DocumentRecordDialog::new);
	}

	public static DocumentRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, DocumentRecordDialog::new);
	}


	private DocumentRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(DocumentHandler.TYPE));

		fileField = new BoundTextField(TAG_FILE);
		GUIHelper.installBehavior(fileField,
			null,
			null,
			null,
			builder -> {
				builder.item("Set...", this::setNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Clear", fileField::clear);
			});
		mappingCombo = new BoundComboBox<>(TAG_MAPPING, new String[]{
			StringUtils.EMPTY,
			"spherical_UV", "cylindrical_equirectangular_horizontal", "cylindrical_equirectangular_vertical"});
		mappingCombo.setEditable(true);
		descriptionArea = new BoundTextArea(TAG_DESCRIPTION, 3, 25);
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), DocumentHandler.TYPE);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
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

	private void initComponents(){
		bindingManager.bind(fileField);
		bindingManager.bind(mappingCombo);
		bindingManager.bind(descriptionArea);


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// name
		mainPanel.add(new JLabel("File*:"), "align label");
		mainPanel.add(fileField, "growx,wrap");

		// mapping
		mainPanel.add(new JLabel("Mapping:"), "align label");
		mainPanel.add(mappingCombo, "growx,wrap");

		// description
		mainPanel.add(new JLabel("Description:"), "align label,top");
		mainPanel.add(GUIHelper.createScrollPane(descriptionArea), "growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]"));
		panel.add(notePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		notePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(fileField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Document file is required.",
				tabbedPane, mainPanel, fileField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		notePanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(DocumentRecordDialog::createNew);
	}

}
