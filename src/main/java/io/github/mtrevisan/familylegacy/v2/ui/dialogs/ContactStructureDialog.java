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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.VariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
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
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class ContactStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3227495851403391698L;


	private static final String TAG_ADDRESS = "ADDRESS";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_NAME = "NAME";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new PartHandler());
		HandlerRegistry.register(new NoteHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField addressField;
	private final BoundComboBox<String> typeCombo;
	private final VariantListPanel nameListPanel;
	private final EntityReferenceListPanel notePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static ContactStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ContactStructureDialog::new);
	}

	public static ContactStructureDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ContactStructureDialog::new);
	}


	private ContactStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PartHandler.TYPE));

		setTitle(record == null? "Add Part": "Edit Part");

		addressField = new BoundTextField(TAG_ADDRESS, 30);
		typeCombo = new BoundComboBox<>(ContactStructureDialog.TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"email", "phone", "mobile", "fax", "website", "blog", "social", "postal", "messaging"
		});
		nameListPanel = new VariantListPanel(TAG_NAME, this, "Variant", model);
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(addressField);
		bindingManager.bind(typeCombo);


		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10, fillx", "[right]rel[grow]", "[]5[]10[]"));

		panel.add(new JLabel("Address*:"), "align label");
		panel.add(addressField, "growx,wrap");

		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx,wrap");

		panel.add(nameListPanel, "span 2,growx,wrap");

		panel.add(notePanel, "span 2,growx,wrap");

		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		nameListPanel.load(record);
		notePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
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
		bindingManager.save(record);

		nameListPanel.save(record);
		notePanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ContactStructureDialog::createNew);
	}

}
