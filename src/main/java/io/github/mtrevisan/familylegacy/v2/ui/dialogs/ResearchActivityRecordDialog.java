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
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.TaskListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
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
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* DONE */
/**
 * Dialog for editing a {@code RESEARCH_ACTIVITY_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchActivityRecord {
 *   id: LocalID
 *   question*: Xref&lt;ResearchQuestionRecord&gt;
 *   date: Date
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
 *   source*: Xref&lt;SourceRecord&gt;
 *   parent?: Xref&lt;ResearchActivityRecord&gt;
 *   task*: Xref&lt;ResearchTaskRecord&gt;
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class ResearchActivityRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3243743327195324702L;


	private static final String DOT = ".";

	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_DATE = "DATE";
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
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new ResearchActivityHandler());
		HandlerRegistry.register(new ResearchTaskHandler());
		HandlerRegistry.register(new ResearchQuestionHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new EventParticipationHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new IndividualAttributeHandler());
		HandlerRegistry.register(new GroupAttributeHandler());
		HandlerRegistry.register(new PlaceRelationshipHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new DocumentHandler());
		HandlerRegistry.register(new IdentityHypothesisHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new HistoricEventHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]5[]10[]"));
	private final JPanel searchPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]"));
	private final JPanel findingsPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]"));
	private final JPanel referencesPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final EntityReferenceListPanel questionPanel;
	private final DateField dateField;
	private final BoundComboBox<String> activityTypeCombo;
	private final BoundComboBox<String> statusCombo;
	private final BoundTextArea actionArea;
	private final ParticipantField targetField;
	private final SourceCitationListPanel sourcePanel;
	private final BoundComboBox<String> searchScopeTypeCombo;
	private final BoundTextArea searchScopeDetailArea;
	private final BoundComboBox<String> resultCombo;
	private final BoundTextArea observationArea;
	private final BoundTextArea conclusionArea;
	private final BoundComboBox<String> conclusionConfidenceCombo;
	private final ParticipantField parentField;
	private final TaskListPanel taskPanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static ResearchActivityRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ResearchActivityRecordDialog::new);
	}

	public static ResearchActivityRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ResearchActivityRecordDialog::new);
	}


	private ResearchActivityRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchActivityHandler.TYPE));

		// Initialize components
		questionPanel = new EntityReferenceListPanel(TAG_QUESTION, this, "Research Questions", model, ResearchQuestionHandler.TYPE)
			.withParentEntity(this.record.getId(), ResearchActivityHandler.TYPE);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Activity Date", model);
		activityTypeCombo = new BoundComboBox<>(TAG_ACTIVITY_TYPE, new String[]{
			"search", "review", "analysis", "correspondence", "interview", "hypothesis"
		});
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			"planned", "in_progress", "completed", "abandoned"
		});
		actionArea = new BoundTextArea(TAG_ACTION, 3, 30);
		targetField = ParticipantField.create(TAG_TARGET, this, model);
		targetField.setHandlerTypes(List.of(IndividualHandler.TYPE, GroupHandler.TYPE, EventHandler.TYPE,
			EventParticipationHandler.TYPE, RelationshipHandler.TYPE, IndividualAttributeHandler.TYPE,
			GroupAttributeHandler.TYPE, PlaceRelationshipHandler.TYPE, SourceHandler.TYPE, DocumentHandler.TYPE,
			IdentityHypothesisHandler.TYPE, CulturalNormHandler.TYPE, HistoricEventHandler.TYPE));
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, "Sources", model);
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
		parentField = ParticipantField.create(TAG_PARENT, this, model);
		parentField.setHandlerType(ResearchActivityHandler.TYPE);
		taskPanel = new TaskListPanel(TAG_TASK, this, "Tasks", model);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(activityTypeCombo);
		bindingManager.bind(statusCombo);
		bindingManager.bind(actionArea);
		bindingManager.bind(searchScopeTypeCombo);
		bindingManager.bind(searchScopeDetailArea);
		bindingManager.bind(resultCombo);
		bindingManager.bind(observationArea);
		bindingManager.bind(conclusionArea);
		bindingManager.bind(conclusionConfidenceCombo);


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Search", createSearchPanel());
		tabbedPane.addTab("Findings", createFindingsPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// question
		mainPanel.add(questionPanel, "span 2,growx,wrap");

		// date
		mainPanel.add(new JLabel("Date*:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		// activity type
		mainPanel.add(new JLabel("Activity Type*:"), "align label");
		mainPanel.add(activityTypeCombo, "growx,wrap");

		// status
		mainPanel.add(new JLabel("Status*:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// action
		mainPanel.add(new JLabel("Action*:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(actionArea), "growx,wrap");

		return mainPanel;
	}

	private JPanel createSearchPanel(){
		// target
		searchPanel.add(new JLabel("Target:"), "align label");
		searchPanel.add(targetField, "growx,wrap");

		// search scope
		final JPanel searchScopePanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]10[]"));
		searchScopePanel.setBorder(BorderFactory.createTitledBorder("Search Scope"));
		// type
		searchScopePanel.add(new JLabel("Type*:"), "align label");
		searchScopePanel.add(searchScopeTypeCombo, "growx,wrap");
		// detail
		searchScopePanel.add(new JLabel("Detail:"), "align label");
		searchScopePanel.add(GUIHelper.createScrollPane(searchScopeDetailArea), "growx");
		searchPanel.add(searchScopePanel, "span 2,growx,wrap");

		return searchPanel;
	}

	private JPanel createFindingsPanel(){
		// result
		findingsPanel.add(new JLabel("Result:"), "align label");
		findingsPanel.add(resultCombo, "growx,wrap");

		// observation
		findingsPanel.add(new JLabel("Observation:"), "align label");
		findingsPanel.add(GUIHelper.createScrollPane(observationArea), "growx,wrap");

		// conclusion panel
		final JPanel conclusionPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]10[]"));
		conclusionPanel.setBorder(BorderFactory.createTitledBorder("Conclusion"));
		// conclusion
		conclusionPanel.add(GUIHelper.createScrollPane(conclusionArea), "span 2,growx,wrap");
		// confidence
		conclusionPanel.add(new JLabel("Confidence:"), "align label");
		conclusionPanel.add(conclusionConfidenceCombo, "growx");
		findingsPanel.add(conclusionPanel, "span 2,growx,wrap");

		return findingsPanel;
	}

	private JPanel createReferencesPanel(){
		// source
		referencesPanel.add(sourcePanel, "span 2,growx,wrap");

		// parent
		referencesPanel.add(new JLabel("Parent:"), "align label");
		referencesPanel.add(parentField, "growx,wrap");

		// task
		referencesPanel.add(taskPanel, "span 2,growx");

		return referencesPanel;
	}


	@Override
	protected void loadData(){
		dateField.load(record);

		bindingManager.load(record);

		targetField.load(record);
		parentField.load(record);
		questionPanel.load(record);
		taskPanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(!dateField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Date is required.",
				tabbedPane, mainPanel, dateField);

			return false;
		}

		if(activityTypeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Activity type is required.",
				tabbedPane, mainPanel, activityTypeCombo);

			return false;
		}

		if(statusCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Status is required.",
				tabbedPane, mainPanel, statusCombo);

			return false;
		}

		// Action is required
		if(actionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Action is required.",
				tabbedPane, mainPanel, actionArea);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		// clear existing children to avoid duplication
		FLEFRecordHelper.removeChildren(record, TAG_ACTION);
		FLEFRecordHelper.removeChildren(record, TAG_TARGET);
		FLEFRecordHelper.removeChildren(record, TAG_SEARCH_SCOPE_TYPE);
		FLEFRecordHelper.removeChildren(record, TAG_SEARCH_SCOPE_DETAIL);
		FLEFRecordHelper.removeChildren(record, TAG_OBSERVATION);
		FLEFRecordHelper.removeChildren(record, TAG_CONCLUSION);
		FLEFRecordHelper.removeChildren(record, TAG_PARENT);

		dateField.save(record);

		bindingManager.save(record);

		targetField.saveReferences(record);
		parentField.saveReferences(record);
		questionPanel.saveReferences(record);
		taskPanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ResearchActivityRecordDialog::createNew);
	}

}
