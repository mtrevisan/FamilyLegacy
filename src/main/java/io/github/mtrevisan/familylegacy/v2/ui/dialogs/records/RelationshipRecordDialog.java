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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.Strings;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;
import java.util.List;


/**
 * Dialog for editing a {@code RELATIONSHIP_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record RelationshipRecord {
 *   id: LocalID
 *   subject: RelationshipParticipant
 *   object: RelationshipParticipant
 *   type: enum { biological_child, adoptive_child, foster_child, guarded_child, step_child, civil_spouse, religious_spouse, customary_spouse, cohabiting_partner, engaged_partner, group_member, associate, part_of }
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
 * Tab 1 (Properties): subject, object, type, role, status, valid_from, valid_to, evidence
 * Tab 5 (Context): ContextImpactRecord (object[relationship] = this relationship)
 * Tab 6 (Research): ConclusionRecord (resolves = this relationship), ResearchQuestionRecord (object[relationship] = this relationship)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class RelationshipRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final EntityField subjectField;
	private final BoundComboBox<String> subjectTypeCombo;
	private final EntityField objectField;
	private final BoundTextField subjectRoleField;
	private final BoundComboBox<String> statusCombo;
	private final DateField validFromField;
	private final DateField validToField;


	public static RelationshipRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, RelationshipRecordDialog::new);
	}

	public static RelationshipRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, RelationshipRecordDialog::new);
	}


	private RelationshipRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, RelationshipHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]5[]10[]10[]10[]");

		subjectField = EntityField.createForRecordFromOneofReference(RelationshipReader.TAG_SUBJECT, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class);
		subjectField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> updateTypeCombo());
		objectField = EntityField.createForRecordFromOneofReference(RelationshipReader.TAG_OBJECT, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class);
		objectField.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> updateTypeCombo());
		subjectTypeCombo = new BoundComboBox<>(RelationshipReader.TAG_TYPE, RelationshipHandler.TYPES);
		subjectTypeCombo.setI18NPrefix("enum.relationship.type");
		subjectRoleField = new BoundTextField(RelationshipReader.TAG_ROLE);
		statusCombo = new BoundComboBox<>(RelationshipReader.TAG_STATUS, RelationshipHandler.STATUSES);
		statusCombo.setI18NPrefix("enum.relationship.status");
		validFromField = DateField.createWithWrapperTag(RelationshipReader.TAG_VALID_FROM, this, I18N.t("dialog.date.valid.from"), model);
		validToField = DateField.createWithWrapperTag(RelationshipReader.TAG_VALID_TO, this, I18N.t("dialog.date.valid.to"), model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impact"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, RelationshipReader.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, RelationshipReader.TAG_NOTE, null)
			.withComponent(PanelKey.EVIDENCE, RelationshipReader.TAG_EVIDENCE, I18N.t("dialog.component.evidence"))
			.withComponent(PanelKey.PRIVACY, RelationshipReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, RelationshipReader.TAG_AUDIT, null)
			.build();

		components.bind(subjectTypeCombo);
		components.bind(subjectRoleField);
		components.bind(statusCombo);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}

	private void updateTypeCombo(){
		final FLEFRecord subjectRecord = subjectField.getEntity();
		if(subjectRecord == null)
			return;

		final FLEFRecord objectRecord = objectField.getEntity();
		if(objectRecord == null)
			return;

		final String subjectType = subjectRecord.getTag();
		final String objectType = objectRecord.getTag();
		final String[] validTypes = getValidTypes(subjectType, objectType);
		subjectTypeCombo.updateItems(List.of(validTypes));
		subjectTypeCombo.setEnabled(validTypes.length > 0);
	}

	private String[] getValidTypes(final String subjectType, final String objectType){
		if(subjectType == null || objectType == null)
			return RelationshipHandler.EMPTY_TYPES;

		if(Strings.CI.equals(IndividualHandler.TYPE, subjectType) && Strings.CI.equals(IndividualHandler.TYPE, objectType))
			return RelationshipHandler.INDIVIDUAL_TO_INDIVIDUAL_TYPES;

		if(Strings.CI.equals(IndividualHandler.TYPE, subjectType) && Strings.CI.equals(GroupHandler.TYPE, objectType))
			return RelationshipHandler.INDIVIDUAL_TO_GROUP_TYPES;

		if(Strings.CI.equals(GroupHandler.TYPE, subjectType) && Strings.CI.equals(GroupHandler.TYPE, objectType))
			return RelationshipHandler.GROUP_TO_GROUP_TYPES;

		return RelationshipHandler.GROUP_TO_INDIVIDUAL_TYPES;
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// subject
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.relationship.subject") + "*:", subjectField);

		// (subject) type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.relationship.subject.type") + "*:", subjectTypeCombo);

		// (subject) role
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.relationship.subject.role") + ":", subjectRoleField);

		// object
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.relationship.object") + "*:", objectField);

		// status
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.relationship.status") + ":", statusCombo);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.validity.range")));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid.from") + ":", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid.to") + ":", validToField);
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

		// Image carousel below the source list
		// It will be hidden if no images are found
		GUIHelper.addComponent(panel, imageCarouselPanel);

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
			I18N.tf("error.cannot.set.parent.message", RelationshipHandler.getInstance().getLabel()),
			I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

		return this;
	}

	public RelationshipRecordDialog withSubject(final FLEFRecord subject){
		super.withParentEntity(subject);

		if(parentEntity != null && !parentEntity.isEmpty()){
			subjectField.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentVisible(subjectField, showAll);
			GUIHelper.setComponentVisible(objectField, true);
		}

		return this;
	}

	public RelationshipRecordDialog withObject(final FLEFRecord object){
		super.withParentEntity(object);

		if(parentEntity != null && !parentEntity.isEmpty()){
			objectField.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentVisible(subjectField, true);
			GUIHelper.setComponentVisible(objectField, showAll);
		}

		return this;
	}


	@Override
	protected void loadData(){
		subjectField.load(record);
		objectField.load(record);

		components.load(record);

		validFromField.load(record);
		validToField.load(record);

		updateTypeCombo();


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(!subjectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.relationship.subject")),
				tabbedPane, propertiesPanel, subjectField);

			return false;
		}

		if(!objectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.relationship.object")),
				tabbedPane, propertiesPanel, objectField);

			return false;
		}

		if(subjectField.equals(objectField)){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required.not.same", I18N.t("dialog.relationship.subject"), I18N.t("dialog.relationship.object")),
				tabbedPane, propertiesPanel, subjectField);

			return false;
		}

		if(!subjectTypeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.relationship.subject.type")),
				tabbedPane, propertiesPanel, subjectTypeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		subjectField.saveReferences(record);
		objectField.saveReferences(record);

		components.save(record);

		validFromField.save(record);
		validToField.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(RelationshipRecordDialog::createEdit, "/tests/test.flef", "RL1");
	}

}
