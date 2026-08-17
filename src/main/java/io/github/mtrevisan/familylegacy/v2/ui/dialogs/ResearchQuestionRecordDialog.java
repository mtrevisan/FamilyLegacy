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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.ResearchQuestionStatusPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code RESEARCH_QUESTION_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchQuestionRecord {
 *   id: LocalID
 *   title: Text
 *   question: Text
 *   target*: ResearchTarget
 *   status: enum { open, on_hold, resolved, disproven }
 *   conclusion?: Text
 *   conclusion_confidence?: enum { low, medium, high }
 *   rationale?: Text
 *   created: Date
 *   closed?: Date
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
 *   source: Xref&lt;SourceRecord&gt;
 *   document: Xref&lt;DocumentRecord&gt;
 *   identity_hypothesis: Xref&lt;IdentityHypothesisRecord&gt;
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 *   void: struct {}
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

	@Serial
	private static final long serialVersionUID = 4693851314612375503L;


	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_CONCLUSION_CONFIDENCE = "CONCLUSION_CONFIDENCE";
	private static final String TAG_RATIONALE = "RATIONALE";
	private static final String TAG_CREATED = "CREATED";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";

	private static final String TAG_RESEARCH_ACTIVITY = "RESEARCH_ACTIVITY";
	private static final String TAG_RESEARCH_TASK = "RESEARCH_TASK";
	private static final String TAG_CONCLUSION = "CONCLUSION";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField titleField;
	private final BoundTextArea questionArea;
	private final ParticipantField targetField;
	private final ResearchQuestionStatusPanel statusPanel;
	private final BoundTextArea conclusionArea;
	private final BoundComboBox<String> conclusionConfidenceCombo;
	private final BoundTextArea rationaleArea;
	private final BoundTextField createdField;


	public static ResearchQuestionRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ResearchQuestionRecordDialog::new);
	}

	public static ResearchQuestionRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ResearchQuestionRecordDialog::new);
	}


	private ResearchQuestionRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ResearchQuestionHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]10[]10[]");

		titleField = new BoundTextField(TAG_TITLE);
		questionArea = new BoundTextArea(TAG_QUESTION, 3, 30);
		targetField = ParticipantField.create(TAG_TARGET, this, model);
		targetField.setHandlerTypes(IndividualHandler.class, GroupHandler.class, EventHandler.class,
			EventParticipationHandler.class, RelationshipHandler.class, IndividualAttributeHandler.class,
			GroupAttributeHandler.class, PlaceRelationshipHandler.class, SourceHandler.class, DocumentHandler.class,
			IdentityHypothesisHandler.class, CulturalNormHandler.class, HistoricEventHandler.class);
		statusPanel = new ResearchQuestionStatusPanel();
		conclusionArea = new BoundTextArea(TAG_CONCLUSION, 3, 30);
		conclusionConfidenceCombo = new BoundComboBox<>(TAG_CONCLUSION_CONFIDENCE, new String[]{
			StringUtils.EMPTY,
			"low", "medium", "high"});
		rationaleArea = new BoundTextArea(TAG_RATIONALE, 3, 30);
		createdField = new BoundTextField(TAG_CREATED);
		createdField.setEditable(false);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONCLUSION, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, ResearchQuestionHandler.class)
			.withComponent(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION, TAG_RESEARCH_ACTIVITY, "Research Activities", ResearchActivityHandler.class, ResearchQuestionHandler.class)
			.withComponent(PanelKey.RESEARCH_TASK, TAG_RESEARCH_TASK, "Research Tasks", ResearchTaskHandler.class, ResearchQuestionHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(titleField);
		components.bind(questionArea);
		components.bind(conclusionArea);
		components.bind(conclusionConfidenceCombo);
		components.bind(rationaleArea);
		components.bind(createdField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// title
		GUIHelper.addLabeledComponent(propertiesPanel, "Title*:", titleField);

		// question
		GUIHelper.addLabeledComponent(propertiesPanel, "Question*:", questionArea);

		// target
		GUIHelper.addLabeledComponent(propertiesPanel, "Target:", targetField);

		// status
		GUIHelper.addLabeledComponent(propertiesPanel, "Status*:", statusPanel);

		// conclusion panel:
		final JPanel conclusionPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		conclusionPanel.setBorder(BorderFactory.createTitledBorder("Conclusion"));
		// conclusion
		GUIHelper.addComponent(conclusionPanel, conclusionArea);
		// confidence
		GUIHelper.addLabeledComponent(conclusionPanel, "Confidence:", conclusionConfidenceCombo);
		GUIHelper.addComponent(propertiesPanel, conclusionPanel);

		// rationale
		GUIHelper.addLabeledComponent(propertiesPanel, "Rationale:", rationaleArea);

		// created
		//hidden

		// closed
		//calculated

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		final JPanel researchActivityPanel = components.getPanel(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION);
		GUIHelper.addComponent(panel, researchActivityPanel);

		final JPanel researchTaskPanel = components.getPanel(PanelKey.RESEARCH_TASK);
		GUIHelper.addComponent(panel, researchTaskPanel);

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION);
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

		targetField.load(record);
		statusPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(titleField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Title is required.",
				tabbedPane, propertiesPanel, titleField);
			return false;
		}

		if(questionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Question is required.",
				tabbedPane, propertiesPanel, questionArea);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		if(createdField.isEmpty()){
			final String creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
			createdField.setText(creationDate);
		}

		components.save(record);

		targetField.saveReferences(record);
		statusPanel.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord document = FLEFRecord.createMainRecord("D1", "DOCUMENT");

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(document);
		};
		GUIHelper.launch(ResearchQuestionRecordDialog::createEdit, modelFiller, document);
	}

}
