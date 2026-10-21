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
package io.github.mtrevisan.familylegacy.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.AgentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Window;


/**
 * Dialog for editing a {@code AGENT_STRUCTURE} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * Agent = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   text: Text
 * }
 * </pre>
 */
public class AgentStructureDialog extends BaseRecordDialog{

	private static final String CARD_NAME_AGENT = "agent";
	private static final String CARD_NAME_TEXT = "text";

	private static final String ENUM_TYPE_AGENT = "agent";
	private static final String ENUM_TYPE_TEXT = "text";


	private final JPanel propertiesPanel;

	private final BoundComboBox<String> typeCombo;
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	private final EntityField agentField;
	private final BoundTextField textField;


	public static AgentStructureDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, AgentStructureDialog::new);
	}

	public static AgentStructureDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, AgentStructureDialog::new);
	}


	private AgentStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, AgentHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[][grow,fill]");

		typeCombo = new BoundComboBox<>(null, new String[]{ENUM_TYPE_AGENT, ENUM_TYPE_TEXT});
		typeCombo.setI18NPrefix("enum.agent.type");
		typeCombo.addActionListener(e -> {
			final String selected = (String)typeCombo.getSelectedItem();
			if(ENUM_TYPE_AGENT.equals(selected))
				cardLayout.show(cardPanel, CARD_NAME_AGENT);
			else
				cardLayout.show(cardPanel, CARD_NAME_TEXT);
		});

		agentField = EntityField.createForRecordFromOneofReference(null, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class);
		textField = new BoundTextField(AgentReader.TAG_TEXT);

		cardPanel.add(agentField, CARD_NAME_AGENT);
		cardPanel.add(textField, CARD_NAME_TEXT);

		finalizeDialog(parent);
	}

	@Override
	protected JPanel createPropertiesPanel(){
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.agent.type") + ":", typeCombo);
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.agent.value") + ":", cardPanel);

		return propertiesPanel;
	}


	@Override
	protected void loadData(){
		if(FLEFRecordHelper.findChild(record, AgentReader.TAG_INDIVIDUAL) != null
				|| FLEFRecordHelper.findChild(record, AgentReader.TAG_GROUP) != null){
			typeCombo.setSelectedItem(ENUM_TYPE_AGENT);
			cardLayout.show(cardPanel, CARD_NAME_AGENT);
			final FLEFRecord agent = model.getRecordById(record.getTheOnlyChild().getValue());
			agentField.load(agent);
		}
		else{
			typeCombo.setSelectedItem(ENUM_TYPE_TEXT);
			cardLayout.show(cardPanel, CARD_NAME_TEXT);
			textField.setText(FLEFRecordHelper.getChildValue(record, AgentReader.TAG_TEXT));
		}
	}

	@Override
	protected boolean validData(){
		final String selected = (String)typeCombo.getSelectedItem();
		if(ENUM_TYPE_AGENT.equals(selected) && agentField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.agent.agent")),
				tabbedPane, propertiesPanel, agentField);

			return false;
		}
		else if(ENUM_TYPE_TEXT.equals(selected) && textField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.agent.text")),
				tabbedPane, propertiesPanel, textField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.removeChildren(record, AgentReader.TAG_INDIVIDUAL, AgentReader.TAG_GROUP, AgentReader.TAG_TEXT);

		final String selected = (String)typeCombo.getSelectedItem();
		if(ENUM_TYPE_AGENT.equals(selected))
			agentField.saveReferences(record);
		else{
			final FLEFRecord textRecord = FLEFRecordHelper.getOrCreateTargetNode(record, AgentReader.TAG_TEXT);
			textRecord.setValue(textField.getText());
		}
	}


	public static void main(final String[] args){
		GUIHelper.launch(AgentStructureDialog::createNew);
	}

}
