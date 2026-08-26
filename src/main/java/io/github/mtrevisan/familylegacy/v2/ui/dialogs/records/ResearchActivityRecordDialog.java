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
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


/**
 * Dialog for editing a {@code RESEARCH_ACTIVITY_RECORD} according to FLEF 0.1.2.
 * <p>
 * Structure:
 * <pre>
 * record ResearchActivityRecord {
 *   id: LocalID
 *   question*: Xref&lt;ResearchQuestionRecord&gt;
 *   activity_type: enum { search, review, analysis, correspondence, interview, hypothesis }
 *   status: enum { planned, in_progress, completed, abandoned }
 *   action: Text
 *
 *   target?: ResearchTarget
 *   search_scope?: struct {
 *     type: enum { entire_source, index_only, partial_source, selected_entries }
 *     detail?: Text
 *   }
 *
 *   result?: enum { positive, negative, inconclusive, conflicting, unavailable }
 *   observation?: Text
 *   conclusion?: Text
 *   conclusion_confidence?: enum { low, medium, high }
 *
 *   source*: SourceCitation
 *   parent?: Xref&lt;ResearchActivityRecord&gt;
 *   task*: Xref&lt;ResearchTaskRecord&gt;
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): question, activity_type, status, action, target, search_scope, result, observation, conclusion, conclusion_confidence, parent, task
 * Tab 7 (Sources): source
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ResearchActivityRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3243743327195324702L;


	private static final String DOT = ".";

	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_ACTIVITY_TYPE = "ACTIVITY_TYPE";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_ACTION = "ACTION";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_SEARCH_SCOPE = "SEARCH_SCOPE";
	private static final String TAG_SEARCH_SCOPE_TYPE = TAG_SEARCH_SCOPE + DOT + "TYPE";
	private static final String TAG_SEARCH_SCOPE_DETAIL = TAG_SEARCH_SCOPE + DOT + "DETAIL";
	private static final String TAG_RESULT = "RESULT";
	private static final String TAG_OBSERVATION = "OBSERVATION";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_CONCLUSION_CONFIDENCE = "CONCLUSION_CONFIDENCE";
	private static final String TAG_PARENT = "PARENT";
	private static final String TAG_TASK = "TASK";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> activityTypeCombo;
	private final BoundComboBox<String> statusCombo;
	private final BoundTextArea actionArea;
	private final EntityField targetField;
	private final BoundComboBox<String> searchScopeTypeCombo;
	private final BoundTextArea searchScopeDetailArea;
	private final BoundComboBox<String> resultCombo;
	private final BoundTextArea observationArea;
	private final BoundTextArea conclusionArea;
	private final BoundComboBox<String> conclusionConfidenceCombo;
	private final EntityField parentField;


	public static ResearchActivityRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ResearchActivityRecordDialog::new);
	}

	public static ResearchActivityRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ResearchActivityRecordDialog::new);
	}


	private ResearchActivityRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, ResearchActivityHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]5[]10[]");

		// Initialize components
		activityTypeCombo = new BoundComboBox<>(TAG_ACTIVITY_TYPE, new String[]{
			"search", "review", "analysis", "correspondence", "interview", "hypothesis"
		});
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			"planned", "in_progress", "completed", "abandoned"
		});
		actionArea = new BoundTextArea(TAG_ACTION, 3, 30);

		targetField = EntityField.createForRecordFromOneofReference(TAG_TARGET, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, EventHandler.class,
				EventParticipationHandler.class, RelationshipHandler.class, IndividualAttributeHandler.class,
				GroupAttributeHandler.class, PlaceHandler.class, PlaceRelationshipHandler.class, SourceHandler.class,
				DocumentHandler.class, IdentityHypothesisHandler.class, CulturalNormHandler.class, HistoricEventHandler.class)
			.withSaveAsVoid();
		searchScopeTypeCombo = new BoundComboBox<>(TAG_SEARCH_SCOPE_TYPE, new String[]{
			"entire_source",
			"index_only",
			"partial_source",
			"selected_entries"
		});
		searchScopeDetailArea = new BoundTextArea(TAG_SEARCH_SCOPE_DETAIL, 3, 30);

		resultCombo = new BoundComboBox<>(TAG_RESULT, new String[]{
			StringUtils.EMPTY,
			"positive", "negative", "inconclusive", "conflicting", "unavailable"
		});
		observationArea = new BoundTextArea(TAG_OBSERVATION, 3, 30);
		conclusionArea = new BoundTextArea(TAG_CONCLUSION, 3, 30);
		conclusionConfidenceCombo = new BoundComboBox<>(TAG_CONCLUSION_CONFIDENCE, new String[]{
			StringUtils.EMPTY,
			"low", "medium", "high"});

		parentField = EntityField.createForRecordFromReference(TAG_PARENT, this, model, ResearchActivityHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.RESEARCH_QUESTION, TAG_QUESTION, "Questions", ResearchQuestionHandler.class, ResearchQuestionHandler.class)
			.withComponent(PanelKey.TASK, TAG_TASK, "Tasks", ResearchTaskHandler.class, ResearchTaskHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(activityTypeCombo);
		components.bind(statusCombo);
		components.bind(actionArea);
		components.bind(searchScopeTypeCombo);
		components.bind(searchScopeDetailArea);
		components.bind(resultCombo);
		components.bind(observationArea);
		components.bind(conclusionArea);
		components.bind(conclusionConfidenceCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// question
		final JPanel notePanel = components.getPanel(PanelKey.RESEARCH_QUESTION);
		GUIHelper.addComponent(propertiesPanel, notePanel);

		// activity type
		GUIHelper.addLabeledComponent(propertiesPanel, "Activity Type*:", activityTypeCombo);

		// status
		GUIHelper.addLabeledComponent(propertiesPanel, "Status*:", statusCombo);

		// action
		GUIHelper.addLabeledComponent(propertiesPanel, "Action*:", actionArea);

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// target
		GUIHelper.addLabeledComponent(panel, "Target:", targetField);

		// search scope:
		final JPanel searchScopePanel = GUIHelper.createLabelFieldPanel(5, "[]10[]");
		searchScopePanel.setBorder(BorderFactory.createTitledBorder("Search Scope"));
		// type
		GUIHelper.addLabeledComponent(searchScopePanel, "Type*:", searchScopeTypeCombo);
		// detail
		GUIHelper.addLabeledComponent(searchScopePanel, "Detail:", searchScopeDetailArea);
		GUIHelper.addComponent(panel, searchScopePanel);

		return panel;
	}

	@Override
	protected JPanel createFindingsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		// result
		GUIHelper.addLabeledComponent(panel, "Result:", resultCombo);

		// observation
		GUIHelper.addLabeledComponent(panel, "Observation:", observationArea);

		// conclusion panel:
		final JPanel conclusionPanel = GUIHelper.createLabelFieldPanel(5, "[]10[]");
		conclusionPanel.setBorder(BorderFactory.createTitledBorder("Conclusion"));
		// conclusion
		GUIHelper.addComponent(conclusionPanel, conclusionArea);
		// confidence
		GUIHelper.addLabeledComponent(conclusionPanel, "Confidence:", conclusionConfidenceCombo);
		GUIHelper.addComponent(panel, conclusionPanel);

		return panel;
	}

	@Override
	protected JPanel createReferencesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// parent
		panel.add(new JLabel("Parent:"), "align label");
		panel.add(parentField, "growx,wrap");

		// task
		final JPanel taskPanel = components.getPanel(PanelKey.TASK);
		panel.add(taskPanel, "span 2,growx");

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
		parentField.load(record);
	}

	@Override
	protected boolean validData(){
		if(!activityTypeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Activity type is required.",
				tabbedPane, propertiesPanel, activityTypeCombo);

			return false;
		}

		if(!statusCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Status is required.",
				tabbedPane, propertiesPanel, statusCombo);

			return false;
		}

		// Action is required
		if(actionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Action is required.",
				tabbedPane, propertiesPanel, actionArea);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		targetField.saveReferences(record);
		parentField.saveReferences(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(ResearchActivityRecordDialog::createEdit, "/tests/test.flef", "RA1");
	}

}
