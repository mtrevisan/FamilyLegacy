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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.ResearchQuestionStatusPanel;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code RESEARCH_QUESTION_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record ResearchQuestionRecord {
 *   title: Text
 *   question: Text
 *   target*: ResearchTarget
 *   status: enum { open, on_hold, resolved, disproven }
 *   conclusion?: Text
 *   conclusion_confidence?: Confidence
 *   rationale?: Text
 *   closed_date?: Date
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * ResearchTarget = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   event: Xref&lt;EventRecord&gt;
 *   event_participation: Xref&lt;EventParticipationRecord&gt;
 *   relationship: Xref&lt;RelationshipRecord&gt;
 *   individual_attribute: Xref&lt;IndividualAttributeRecord&gt;
 *   group_attribute: Xref&lt;GroupAttributeRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 *   place_relationship: Xref&lt;PlaceRelationshipRecord&gt;
 *   repository: Xref&lt;RepositoryRecord&gt;
 *   source: Xref&lt;SourceRecord&gt;
 *   document: Xref&lt;DocumentRecord&gt;
 *   conclusion: Xref&lt;ConclusionRecord&gt;
 *   identity_hypothesis: Xref&lt;IdentityHypothesisRecord&gt;
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): title, question, target, status, conclusion, conclusion_confidence, rationale, created, closed
 * Tab 6 (Research): ResearchActivityRecord (question contains this question), ResearchTaskRecord (question contains this question), ConclusionRecord (research contains this question)
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ResearchQuestionRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final BoundTextField titleField;
	private final BoundTextArea questionArea;
	private final EntityListPanel targetPanel;
	private final ResearchQuestionStatusPanel statusPanel;
	private final BoundTextArea conclusionArea;
	private final BoundComboBox<String> conclusionConfidenceCombo;
	private final BoundTextArea rationaleArea;


	public static ResearchQuestionRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, ResearchQuestionRecordDialog::new);
	}

	public static ResearchQuestionRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ResearchQuestionRecordDialog::new);
	}


	private ResearchQuestionRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ResearchQuestionHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]10[]");

		titleField = new BoundTextField(ResearchQuestionReader.TAG_TITLE);
		questionArea = new BoundTextArea(ResearchQuestionReader.TAG_QUESTION, 3, 30);
		targetPanel = EntityListPanel.createForOneOfReference(ResearchQuestionReader.TAG_TARGET, this, I18N.t("dialog.research_question.target"), model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, EventHandler.class,
				EventParticipationHandler.class, RelationshipHandler.class, IndividualAttributeHandler.class,
				GroupAttributeHandler.class, PlaceHandler.class, PlaceRelationshipHandler.class, RepositoryHandler.class,
				SourceHandler.class, DocumentHandler.class, ConclusionHandler.class, IdentityHypothesisHandler.class,
				CulturalNormHandler.class, HistoricEventHandler.class);
		statusPanel = new ResearchQuestionStatusPanel();
		conclusionArea = new BoundTextArea(ResearchQuestionReader.TAG_CONCLUSION, 3, 30);
		conclusionConfidenceCombo = new BoundComboBox<>(ResearchQuestionReader.TAG_CONCLUSION_CONFIDENCE, GUIHelper.fillCombo(ConfidenceReader.CONFIDENCES, null));
		conclusionConfidenceCombo.setI18NPrefix("enum.confidence");
		rationaleArea = new BoundTextArea(ResearchQuestionReader.TAG_RATIONALE, 3, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.CONCLUSION_ON_RESEARCH, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION, ResearchActivityHandler.TYPE, I18N.t("dialog.component.research.activities"))
			.withComponent(PanelKey.RESEARCH_TASK_ON_QUESTION, ResearchTaskHandler.TYPE, I18N.t("dialog.component.research.follow_ups"))
			.withComponent(PanelKey.PRIVACY, ResearchQuestionReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, ResearchQuestionReader.TAG_AUDIT, null)
			.build();

		components.bind(titleField);
		components.bind(questionArea);
		components.bind(conclusionArea);
		components.bind(conclusionConfidenceCombo);
		components.bind(rationaleArea);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// title
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.research_question.title") + "*:", titleField);

		// question
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.research_question.question") + "*:", questionArea);

		// target
		GUIHelper.addComponent(propertiesPanel, targetPanel);

		// status
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.research_question.status") + "*:", statusPanel);

		// conclusion panel:
		final JPanel conclusionPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		conclusionPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.research_question.conclusion")));
		// conclusion
		GUIHelper.addComponent(conclusionPanel, conclusionArea);
		// confidence
		GUIHelper.addLabeledComponent(conclusionPanel, I18N.t("dialog.research_question.confidence") + ":", conclusionConfidenceCombo);
		GUIHelper.addComponent(propertiesPanel, conclusionPanel);

		// rationale
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.research_question.rationale") + ":", rationaleArea);

		// closed date
		//calculated

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		final JPanel researchActivityPanel = components.getPanel(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION);
		GUIHelper.addComponent(panel, researchActivityPanel);

		final JPanel researchTaskPanel = components.getPanel(PanelKey.RESEARCH_TASK_ON_QUESTION);
		GUIHelper.addComponent(panel, researchTaskPanel);

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESEARCH);
		GUIHelper.addComponent(panel, conclusionPanel);

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

		targetPanel.load(record);
		statusPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(titleField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.research_question.title")),
				tabbedPane, propertiesPanel, titleField);
			return false;
		}

		if(questionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.research_question.question")),
				tabbedPane, propertiesPanel, questionArea);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		targetPanel.save(record);
		statusPanel.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(ResearchQuestionRecordDialog::createEdit, "/tests/test.flef", "RQ1");
	}

}
