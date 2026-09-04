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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.Serial;
import java.util.Collections;
import java.util.List;


/**
 * Dialog for editing a {@code RELATIONSHIP_RECORD} according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * record RelationshipRecord {
 *   id: LocalID
 *   subject: RelationshipParticipant
 *   target: RelationshipParticipant
 *   type: enum { biological_child, adoptive_child, foster_child, guarded_child, step_child, civil_spouse, religious_spouse, customary_spouse, cohabiting_partner, engaged_partner, group_member, associate } | Text
 *   role?: Text
 *   status?: enum { active, ended, unknown }
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * RelationshipParticipant = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): subject, target, type, role, status, valid_from, valid_to, evidence
 * Tab 5 (Context): ContextImpactRecord (target[relationship] = this relationship)
 * Tab 6 (Research): ConclusionRecord (resolves = this relationship), ResearchQuestionRecord (target[relationship] = this relationship)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class RelationshipRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -6390551689993360839L;


	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";

	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";


	private static final List<String> INDIVIDUAL_TO_INDIVIDUAL_TYPES = List.of(
		"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child",
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	);
	private static final List<String> INDIVIDUAL_TO_GROUP_TYPES = List.of(
		"group_member", "associate"
	);
	private static final List<String> GROUP_TO_GROUP_TYPES = List.of(
		"associate"
	);
	private static final List<String> GROUP_TO_INDIVIDUAL_TYPES = Collections.emptyList();


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final EntityField subjectField;
	private final BoundComboBox<String> subjectTypeCombo;
	private final EntityField targetField;
	private final BoundTextField subjectRoleField;
	private final BoundComboBox<String> statusCombo;
	private final DateField validFromField;
	private final DateField validToField;


	public static RelationshipRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, RelationshipRecordDialog::new);
	}

	public static RelationshipRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, RelationshipRecordDialog::new);
	}


	private RelationshipRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, RelationshipHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]5[]10[]10[]10[]");

		subjectField = EntityField.createForRecordFromOneofReference(TAG_SUBJECT, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class);
		subjectField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> updateTypeCombo());
		targetField = EntityField.createForRecordFromOneofReference(TAG_TARGET, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class);
		targetField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> updateTypeCombo());
		subjectTypeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child",
			"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
			"group_member", "associate"
		});
		subjectTypeCombo.setEditable(true);
		subjectRoleField = new BoundTextField(TAG_ROLE);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			StringUtils.EMPTY,
			"active", "ended", "unknown"
		});
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts")
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions")
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions")
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations")
			.withComponent(PanelKey.NOTE, TAG_NOTE, null)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence")
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null)
			.build();

		components.bind(subjectTypeCombo);
		components.bind(subjectRoleField);
		components.bind(statusCombo);


		finalizeDialog(parent);
	}

	private void updateTypeCombo(){
		final FLEFRecord subjectRecord = subjectField.getEntity();
		if(subjectRecord == null)
			return;

		final FLEFRecord targetRecord = targetField.getEntity();
		if(targetRecord == null)
			return;

		final String subjectType = subjectRecord.getTag();
		final String targetType = targetRecord.getTag();
		final List<String> validTypes = getValidTypes(subjectType, targetType);
		subjectTypeCombo.updateItems(validTypes);
		subjectTypeCombo.setEnabled(!validTypes.isEmpty());
	}

	private List<String> getValidTypes(final String subjectType, final String objectType){
		if(subjectType == null || objectType == null)
			return Collections.emptyList();

		if(Strings.CI.equals(IndividualHandler.TYPE, subjectType) && Strings.CI.equals(IndividualHandler.TYPE, objectType))
			return INDIVIDUAL_TO_INDIVIDUAL_TYPES;

		if(Strings.CI.equals(IndividualHandler.TYPE, subjectType) && Strings.CI.equals(GroupHandler.TYPE, objectType))
			return INDIVIDUAL_TO_GROUP_TYPES;

		if(Strings.CI.equals(GroupHandler.TYPE, subjectType) && Strings.CI.equals(GroupHandler.TYPE, objectType))
			return GROUP_TO_GROUP_TYPES;

		return GROUP_TO_INDIVIDUAL_TYPES;
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// subject
		GUIHelper.addLabeledComponent(propertiesPanel, "Subject*:", subjectField);

		// (subject) type
		GUIHelper.addLabeledComponent(propertiesPanel, "Subject Type*:", subjectTypeCombo);

		// (subject) role
		GUIHelper.addLabeledComponent(propertiesPanel, "Subject Role:", subjectRoleField);

		// target
		GUIHelper.addLabeledComponent(propertiesPanel, "Target*:", targetField);

		// status
		GUIHelper.addLabeledComponent(propertiesPanel, "Status:", statusCombo);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, "Valid From:", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, "Valid To:", validToField);
		GUIHelper.addComponent(propertiesPanel, validityPanel);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel contextPanel = components.getPanel(PanelKey.CONTEXT_IMPACT_ON_TARGET);
		GUIHelper.addComponent(panel, contextPanel);

		return panel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// conclusion
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

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
	public BaseRecordDialog withParentEntity(final FLEFRecord parent){
		JOptionPane.showMessageDialog(this,
			"Cannot set parent on a Relationship Record.",
			"Error", JOptionPane.ERROR_MESSAGE);

		return this;
	}

	public RelationshipRecordDialog withSubject(final FLEFRecord subject){
		super.withParentEntity(subject);

		if(parentEntity != null && !parentEntity.isEmpty()){
			subjectField.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentVisible(subjectField, showAll);
			GUIHelper.setComponentVisible(targetField, true);
		}

		return this;
	}

	public RelationshipRecordDialog withObject(final FLEFRecord object){
		super.withParentEntity(object);

		if(parentEntity != null && !parentEntity.isEmpty()){
			targetField.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentVisible(subjectField, true);
			GUIHelper.setComponentVisible(targetField, showAll);
		}

		return this;
	}


	@Override
	protected void loadData(){
		subjectField.load(record);
		targetField.load(record);

		components.load(record);

		validFromField.load(record);
		validToField.load(record);

		updateTypeCombo();
	}

	@Override
	protected boolean validData(){
		if(!subjectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Subject is required.",
				tabbedPane, propertiesPanel, subjectField);

			return false;
		}

		if(!targetField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Object is required.",
				tabbedPane, propertiesPanel, targetField);

			return false;
		}

		if(subjectField.equals(targetField)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Subject and Object must not be the same entity.",
				tabbedPane, propertiesPanel, subjectField);

			return false;
		}

		if(!subjectTypeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type is required.",
				tabbedPane, propertiesPanel, subjectTypeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		subjectField.saveReferences(record);
		targetField.saveReferences(record);

		components.save(record);

		validFromField.save(record);
		validToField.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(RelationshipRecordDialog::createEdit, "/tests/test.flef", "RL1");
	}

}
