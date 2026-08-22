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
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code ResearchTaskRecord} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchTaskRecord {
 *   id: LocalID
 *   description: Text
 *   question*: Xref&lt;ResearchQuestionRecord&gt;
 *   created_by?: Xref&lt;ResearchActivityRecord&gt;
 *   status: enum { open, in_progress, completed, abandoned }
 *   priority?: enum { low, normal, high }
 *   due_date?: Date
 *   outcome?: Text
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): description, question, created_by, status, priority, due_date, outcome
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ResearchTaskRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -9146822186086957943L;


	private static final String TAG_DESCRIPTION = "DESCRIPTION";
	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_CREATED_BY = "CREATED_BY";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_PRIORITY = "PRIORITY";
	private static final String TAG_DUE_DATE = "DUE_DATE";
	private static final String TAG_OUTCOME = "OUTCOME";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextArea descriptionArea;
	private final EntityReferenceListPanel questionPanel;
	private final ParticipantField createdByField;
	private final BoundComboBox<String> statusCombo;
	private final BoundComboBox<String> priorityCombo;
	private final DateField dueDateField;
	private final BoundTextArea outcomeArea;


	public static ResearchTaskRecordDialog createNew(Dialog parent, FLEFModel model){
		return createNew(parent, model, ResearchTaskRecordDialog::new);
	}

	public static ResearchTaskRecordDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		return createEdit(parent, model, record, ResearchTaskRecordDialog::new);
	}


	private ResearchTaskRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, ResearchTaskHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]10[]10[]");

		descriptionArea = new BoundTextArea(TAG_DESCRIPTION, 3, 30);
		questionPanel = EntityReferenceListPanel.createForRecord(TAG_QUESTION, this, "Research Questions", model)
			.withHandlerTypes(ResearchQuestionHandler.class)
			.withParentEntity(this.record.getId(), ResearchTaskHandler.TYPE);
		createdByField = ParticipantField.create(TAG_CREATED_BY, this, model)
			.withHandlerTypes(ResearchActivityHandler.class);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			"open", "in_progress", "completed", "abandoned"});
		priorityCombo = new BoundComboBox<>(TAG_PRIORITY, new String[]{
			StringUtils.EMPTY,
			"low", "normal", "high"});
		dueDateField = DateField.createWithWrapperTag(TAG_DUE_DATE, this, "Due Date", model);
		outcomeArea = new BoundTextArea(TAG_OUTCOME, 3, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(descriptionArea);
		components.bind(statusCombo);
		components.bind(priorityCombo);
		components.bind(outcomeArea);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// description
		GUIHelper.addLabeledComponent(propertiesPanel, "Description*:", descriptionArea);

		// question
		GUIHelper.addComponent(propertiesPanel, questionPanel);

		// created by
		GUIHelper.addLabeledComponent(propertiesPanel, "Created By:", createdByField);

		// status
		GUIHelper.addLabeledComponent(propertiesPanel, "Status*:", statusCombo);

		// priority
		GUIHelper.addLabeledComponent(propertiesPanel, "Priority:", priorityCombo);

		// due date
		GUIHelper.addLabeledComponent(propertiesPanel, "Due Date:", dueDateField);

		// outcome
		GUIHelper.addLabeledComponent(propertiesPanel, "Outcome:", outcomeArea);

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

		questionPanel.load(record);
		createdByField.load(record);
		dueDateField.load(record);
	}

	@Override
	protected boolean validData(){
		if(descriptionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Description is required.",
				tabbedPane, propertiesPanel, descriptionArea);

			return false;
		}

		if(!statusCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Status is required.",
				tabbedPane, propertiesPanel, statusCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		questionPanel.save(record);
		createdByField.saveReferences(record);
		dueDateField.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord document = FLEFRecord.createMainRecord("D1", "DOCUMENT");

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(document);
		};
		GUIHelper.launch(ResearchTaskRecordDialog::createEdit, modelFiller, document);
	}

}
