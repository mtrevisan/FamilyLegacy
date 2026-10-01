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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.ContactReader;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContactNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Window;


/**
 * Structure:
 * <pre>
 * struct ContactStructure {
 *   uri: Text
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
 * Tab 1 (Properties): uri, type, name
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ContactStructureDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final BoundTextField valueField;
	private final BoundComboBox<String> typeCombo;
	private final EntityListPanel namePanel;
	private final BoundTextArea noteArea;
	private final JScrollPane noteAreaScroll;


	public static ContactStructureDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, ContactStructureDialog::new);
	}

	public static ContactStructureDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ContactStructureDialog::new);
	}


	private ContactStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ContactHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]");

		valueField = new BoundTextField(ContactReader.TAG_VALUE);
		typeCombo = new BoundComboBox<>(ContactReader.TAG_TYPE, GUIHelper.fillCombo(ContactReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.contact.type");
		namePanel = EntityListPanel.createForStructure(ContactReader.TAG_NAME, this, I18N.t("dialog.name.name"), model,
			ContactNameHandler.class);
		noteArea = new BoundTextArea(ContactReader.TAG_NOTE, 3, 25);
		noteArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		noteAreaScroll = new JScrollPane(noteArea);
		noteAreaScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.name.note")));

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.PRIVACY, ContactReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, ContactReader.TAG_AUDIT, null)
			.build();

		components.bind(valueField);
		components.bind(typeCombo);
		components.bind(noteArea);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// address
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.name.value") + "*:", valueField);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.name.type") + ":", typeCombo);

		// name
		GUIHelper.addComponent(propertiesPanel, namePanel);

		// note
		GUIHelper.addComponent(propertiesPanel, noteAreaScroll);

		return propertiesPanel;
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

		namePanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.name.value")),
				tabbedPane, propertiesPanel, valueField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		namePanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ContactStructureDialog::createNew);
	}

}
