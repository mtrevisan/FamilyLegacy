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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.VariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING name with value & variant */
/**
 * Structure:
 * <pre>
 * struct ContactStructure {
 *   address: Text
 *   type?: enum { email, phone, mobile, fax, website, blog, social, postal, messaging } | Text
 *   name?: struct {
 *     value: Text
 *     variant*: TextValueVariant
 *   }
 *   note?: Text
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): address, type, name
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ContactStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 5523786168919512214L;


	private static final String TAG_ADDRESS = "ADDRESS";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_NAME = "NAME";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final BoundTextField addressField;
	private final BoundComboBox<String> typeCombo;
//	private final VariantListPanel nameListPanel;
	private final EntityReferenceListPanel namePanel;


	public static ContactStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ContactStructureDialog::new);
	}

	public static ContactStructureDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ContactStructureDialog::new);
	}


	private ContactStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ContactHandler.class);

		addressField = new BoundTextField(TAG_ADDRESS);
		typeCombo = new BoundComboBox<>(ContactStructureDialog.TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"email", "phone", "mobile", "fax", "website", "blog", "social", "postal", "messaging"
		});
//		nameListPanel = new VariantListPanel(TAG_NAME, this, "Variant", model);
		namePanel = EntityReferenceListPanel.createForStructure(TAG_NAME, this, "Name", model, ContactNameHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(addressField);
		components.bind(typeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]");

		GUIHelper.addLabeledComponent(propertiesPanel, "Address*:", addressField);

		GUIHelper.addLabeledComponent(propertiesPanel, "Type:", typeCombo);

//		GUIHelper.addComponent(propertiesPanel, nameListPanel);
		GUIHelper.addComponent(propertiesPanel, namePanel);

		return propertiesPanel;
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

//		nameListPanel.load(record);
		namePanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(addressField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Address cannot be empty.",
				null, null, addressField);

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

//		nameListPanel.save(record);
		namePanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ContactStructureDialog::createNew);
	}

}
