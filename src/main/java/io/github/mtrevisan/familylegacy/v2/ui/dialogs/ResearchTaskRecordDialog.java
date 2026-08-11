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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
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
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
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
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new ResearchTaskHandler());
		HandlerRegistry.register(new ResearchActivityHandler());
		HandlerRegistry.register(new ResearchQuestionHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]10[]10[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextArea descriptionArea;
	private final EntityReferenceListPanel questionPanel;
	private final ParticipantField createdByField;
	private final BoundComboBox<String> statusCombo;
	private final BoundComboBox<String> priorityCombo;
	private final DateField dueDateField;
	private final BoundTextArea outcomeArea;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static ResearchTaskRecordDialog createNew(Dialog parent, FLEFModel model){
		return createNew(parent, model, ResearchTaskRecordDialog::new);
	}

	public static ResearchTaskRecordDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		return createEdit(parent, model, record, ResearchTaskRecordDialog::new);
	}


	private ResearchTaskRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchTaskHandler.TYPE));

		descriptionArea = new BoundTextArea(TAG_DESCRIPTION, 3, 30);
		questionPanel = EntityReferenceListPanel.createForRecord(TAG_QUESTION, this, "Research Questions", model, ResearchQuestionHandler.TYPE)
			.withParentEntity(this.record.getId(), ResearchTaskHandler.TYPE);
		createdByField = ParticipantField.create(TAG_CREATED_BY, this, model);
		createdByField.setHandlerType(ResearchActivityHandler.TYPE);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			StringUtils.EMPTY,
			"open", "in_progress", "completed", "abandoned"});
		priorityCombo = new BoundComboBox<>(TAG_PRIORITY, new String[]{
			StringUtils.EMPTY,
			"low", "normal", "high"});
		dueDateField = DateField.createWithWrapperTag(TAG_DUE_DATE, this, "Due Date", model);
		outcomeArea = new BoundTextArea(TAG_OUTCOME, 3, 30);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(descriptionArea);
		bindingManager.bind(statusCombo);
		bindingManager.bind(priorityCombo);
		bindingManager.bind(outcomeArea);


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// description
		mainPanel.add(new JLabel("Description*:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(descriptionArea), "growx,wrap");

		// question
		mainPanel.add(questionPanel, "span 2,growx,wrap");

		// status
		mainPanel.add(new JLabel("Created By:"), "align label");
		mainPanel.add(createdByField, "growx,wrap");

		// status
		mainPanel.add(new JLabel("Status*:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// priority
		mainPanel.add(new JLabel("Priority:"), "align label");
		mainPanel.add(priorityCombo, "growx,wrap");

		// due date
		mainPanel.add(new JLabel("Due Date:"), "align label");
		mainPanel.add(dueDateField, "growx,wrap");

		// outcome
		mainPanel.add(new JLabel("Outcome:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(outcomeArea), "growx,wrap");

		return mainPanel;
	}

	@Override
	protected void loadData(){
		bindingManager.load(record);

		questionPanel.load(record);
		createdByField.load(record);
		dueDateField.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(descriptionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Description is required.",
				tabbedPane, mainPanel, descriptionArea);

			return false;
		}

		if(!statusCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Status is required.",
				tabbedPane, mainPanel, statusCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.removeChildren(record, TAG_DESCRIPTION);
		FLEFRecordHelper.removeChildren(record, TAG_STATUS);
		FLEFRecordHelper.removeChildren(record, TAG_PRIORITY);
		FLEFRecordHelper.removeChildren(record, TAG_OUTCOME);
		FLEFRecordHelper.removeChildren(record, TAG_CREATED_BY);

		bindingManager.save(record);

		questionPanel.save(record);
		createdByField.saveReferences(record);
		dueDateField.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ResearchTaskRecordDialog::createNew);
	}

}
