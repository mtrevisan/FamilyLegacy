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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code RESEARCH_ACTIVITY_RECORD} according to FLEF 0.1.3.
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
 *   parent_activity?: Xref&lt;ResearchActivityRecord&gt;
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
	private final EntityField parentActivityField;


	public static ResearchActivityRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, ResearchActivityRecordDialog::new);
	}

	public static ResearchActivityRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ResearchActivityRecordDialog::new);
	}


	private ResearchActivityRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ResearchActivityHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]5[]10[]");

		// Initialize components
		activityTypeCombo = new BoundComboBox<>(ResearchActivityReader.TAG_ACTIVITY_TYPE, ResearchActivityReader.TYPES);
		activityTypeCombo.setI18NPrefix("enum.research.activity.type");
		statusCombo = new BoundComboBox<>(ResearchActivityReader.TAG_STATUS, ResearchActivityReader.STATUSES);
		statusCombo.setI18NPrefix("enum.research.activity.status");
		actionArea = new BoundTextArea(ResearchActivityReader.TAG_ACTION, 3, 30);

		targetField = EntityField.createForRecordFromOneofReference(ResearchActivityReader.TAG_TARGET, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, EventHandler.class,
				EventParticipationHandler.class, RelationshipHandler.class, IndividualAttributeHandler.class,
				GroupAttributeHandler.class, PlaceHandler.class, PlaceRelationshipHandler.class, SourceHandler.class,
				DocumentHandler.class, IdentityHypothesisHandler.class, CulturalNormHandler.class, HistoricEventHandler.class)
			.withSaveAsVoid();
		searchScopeTypeCombo = new BoundComboBox<>(ResearchActivityReader.TAG_SEARCH_SCOPE_TYPE, GUIHelper.fillCombo(ResearchActivityReader.SEARCH_SCOPES, null));
		searchScopeTypeCombo.setI18NPrefix("enum.research.activity.search.scope");
		searchScopeDetailArea = new BoundTextArea(ResearchActivityReader.TAG_SEARCH_SCOPE_DETAIL, 3, 30);

		resultCombo = new BoundComboBox<>(ResearchActivityReader.TAG_RESULT, GUIHelper.fillCombo(ResearchActivityReader.RESULTS, null));
		resultCombo.setI18NPrefix("enum.research.activity.result");
		observationArea = new BoundTextArea(ResearchActivityReader.TAG_OBSERVATION, 3, 30);
		conclusionArea = new BoundTextArea(ResearchActivityReader.TAG_CONCLUSION, 3, 30);
		conclusionConfidenceCombo = new BoundComboBox<>(ResearchActivityReader.TAG_CONCLUSION_CONFIDENCE, GUIHelper.fillCombo(ResearchActivityReader.CONFIDENCES, null));
		conclusionConfidenceCombo.setI18NPrefix("enum.confidence");

		parentActivityField = EntityField.createForRecordFromReference(ResearchActivityReader.TAG_PARENT_ACTIVITY, this, model,
			ResearchActivityHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.RESEARCH_QUESTION, ResearchActivityReader.TAG_QUESTION, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.TASK, ResearchActivityReader.TAG_TASK, I18N.t("dialog.component.research.tasks"))
			.withComponent(PanelKey.SOURCE, ResearchActivityReader.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.PRIVACY, ResearchActivityReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, ResearchActivityReader.TAG_AUDIT, null)
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


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// question
		final JPanel notePanel = components.getPanel(PanelKey.RESEARCH_QUESTION);
		GUIHelper.addComponent(propertiesPanel, notePanel);

		// activity type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.research.activity.type") + "*:", activityTypeCombo);

		// status
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.research.activity.status") + "*:", statusCombo);

		// action
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.research.activity.action") + "*:", actionArea);

		return propertiesPanel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// target
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.research.activity.target") + ":", targetField);

		// search scope:
		final JPanel searchScopePanel = GUIHelper.createLabelFieldPanel(5, "[]10[]");
		searchScopePanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.research.activity.search.scope")));
		// type
		GUIHelper.addLabeledComponent(searchScopePanel, I18N.t("dialog.research.activity.search.scope.type") + "*:", searchScopeTypeCombo);
		// detail
		GUIHelper.addLabeledComponent(searchScopePanel, I18N.t("dialog.research.activity.search.scope.detail") + ":", searchScopeDetailArea);
		GUIHelper.addComponent(panel, searchScopePanel);

		return panel;
	}

	@Override
	protected JPanel createFindingsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		// result
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.research.activity.result") + ":", resultCombo);

		// observation
		GUIHelper.addLabeledComponent(panel, I18N.t("dialog.research.activity.observation") + ":", observationArea);

		// conclusion panel:
		final JPanel conclusionPanel = GUIHelper.createLabelFieldPanel(5, "[]10[]");
		conclusionPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.research.activity.conclusion")));
		// conclusion
		GUIHelper.addComponent(conclusionPanel, conclusionArea);
		// confidence
		GUIHelper.addLabeledComponent(conclusionPanel, I18N.t("dialog.research.activity.confidence") + ":", conclusionConfidenceCombo);
		GUIHelper.addComponent(panel, conclusionPanel);

		return panel;
	}

	@Override
	protected JPanel createReferencesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// parent
		final JLabel parentActivityLabel = new JLabel(I18N.t("dialog.research.activity.parent.activity") + ":");
		parentActivityLabel.setLabelFor(parentActivityField);
		panel.add(parentActivityLabel, "align label");
		panel.add(parentActivityField, "growx,wrap");

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

		// Image carousel below the source list
		// It will be hidden if no images are found
		GUIHelper.addComponent(panel, imageCarouselPanel);

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
		parentActivityField.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(!activityTypeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.research.activity.type")),
				tabbedPane, propertiesPanel, activityTypeCombo);

			return false;
		}

		if(!statusCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.research.activity.status")),
				tabbedPane, propertiesPanel, statusCombo);

			return false;
		}

		// Action is required
		if(actionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.research.activity.action")),
				tabbedPane, propertiesPanel, actionArea);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		targetField.saveReferences(record);
		parentActivityField.saveReferences(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(ResearchActivityRecordDialog::createEdit, "/tests/test.flef", "RA1");
	}

}
