package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.*;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.*;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
import java.util.List;


/* ONGOING */
/**
 * Dialog for editing a {@code ResearchActivityRecord} according to FLEF 0.1.1.
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
 *   target?: ResearchTarget
 *   source*: Xref&lt;SourceRecord&gt;
 *   search_scope?: struct {
 *     type: enum { entire_source, index_only, partial_source, selected_entries }
 *     detail?: Text
 *   }
 *   result?: enum { positive, negative, inconclusive, conflicting, unavailable }
 *   observation?: Text
 *   conclusion?: Text
 *   conclusion_confidence?: enum { low, medium, high }
 *   parent?: Xref&lt;ResearchActivityRecord&gt;
 *   task*: Xref&lt;ResearchTaskRecord&gt;
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class ResearchActivityRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 1L;

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
		HandlerRegistry.register(new ResearchQuestionHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new ResearchTaskHandler());
	}

	private final BindingManager bindingManager = new BindingManager();
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]5[]10[]5[]5[]10[]5[]5[]10[]"));

	// Fields
	private final ResearchQuestionListPanel questionPanel;
	private final DateField dateField;
	private final BoundComboBox<String> activityTypeCombo;
	private final BoundComboBox<String> statusCombo;
	private final BoundTextArea actionArea;
	private final ParticipantField targetField;

	// Search Scope
	private final BoundComboBox<String> searchScopeTypeCombo;
	private final BoundTextArea searchScopeDetailArea;

	// Result fields
	private final BoundComboBox<String> resultCombo;
	private final BoundTextArea observationArea;
	private final BoundTextArea conclusionArea;
	private final BoundComboBox<String> conclusionConfidenceCombo;

	// Parent and Tasks
	private final ParticipantField parentField;
	private final ParticipantField taskPanel;

	// Restriction and Modification
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	/**
	 * Creates a new dialog to create a new record.
	 */
	public static ResearchActivityRecordDialog createNew(Dialog parent, FLEFModel model){
		return new ResearchActivityRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 */
	public static ResearchActivityRecordDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null) throw new IllegalArgumentException("Record cannot be null");
		return new ResearchActivityRecordDialog(parent, model, record);
	}

	private ResearchActivityRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchActivityHandler.TYPE));

		// Initialize components
		questionPanel = new ResearchQuestionListPanel(model, parent);
		dateField = DateField.createWithWrapperTag(TAG_DATE, parent, "Activity Date", model);
		activityTypeCombo = new BoundComboBox<>(TAG_ACTIVITY_TYPE, new String[]{
			"", "search", "review", "analysis", "correspondence", "interview", "hypothesis"
		});
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			"", "planned", "in_progress", "completed", "abandoned"
		});
		actionArea = new BoundTextArea(TAG_ACTION, 3, 30);
		targetField = ParticipantField.create(TAG_TARGET, this, model);
		targetField.setHandlerTypes(List.of(IndividualHandler.TYPE, GroupHandler.TYPE, EventHandler.TYPE,
			EventParticipationHandler.TYPE, RelationshipHandler.TYPE, IndividualAttributeHandler.TYPE,
			GroupAttributeHandler.TYPE, PlaceRelationshipHandler.TYPE, SourceHandler.TYPE, DocumentHandler.TYPE,
			IdentityHypothesisHandler.TYPE, CulturalNormHandler.TYPE, HistoricEventHandler.TYPE));
		searchScopeTypeCombo = new BoundComboBox<>(TAG_SEARCH_SCOPE_TYPE, new String[]{
			"",
			"entire_source",
			"index_only",
			"partial_source",
			"selected_entries"
		});
		searchScopeDetailArea = new BoundTextArea(TAG_SEARCH_SCOPE_DETAIL, 3, 30);

		detailField = new JTextField(20);
		detailField.setToolTipText("Optional detail about the search scope");
		resultCombo = new BoundComboBox<>(TAG_RESULT, new String[]{
			"", "positive", "negative", "inconclusive", "conflicting", "unavailable"
		});
		observationArea = new BoundTextArea(TAG_OBSERVATION, 3, 30);
		conclusionArea = new BoundTextArea(TAG_CONCLUSION, 3, 30);
		conclusionConfidenceCombo = new BoundComboBox<>(TAG_CONCLUSION_CONFIDENCE, new String[]{"", "low", "medium", "high"});
		parentField = ParticipantField.create(TAG_PARENT, this, model);
		parentField.setHandlerType(ResearchActivityHandler.TYPE);
		taskPanel = ParticipantField.create(TAG_TARGET, this, model);
		taskPanel.setHandlerType(ResearchTaskHandler.TYPE);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, parent);
		modificationPanel = new ModificationPanel(parent);

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(activityTypeCombo);
		bindingManager.bind(statusCombo);
		bindingManager.bind(actionArea);
		bindingManager.bind(resultCombo);
		bindingManager.bind(observationArea);
		bindingManager.bind(conclusionArea);
		bindingManager.bind(conclusionConfidenceCombo);

		setLayout(new MigLayout("ins 10,fillx,top"));
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// Date
		mainPanel.add(new JLabel("Date*:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		// Activity Type
		mainPanel.add(new JLabel("Activity Type*:"), "align label");
		mainPanel.add(activityTypeCombo, "growx,wrap");

		// Status
		mainPanel.add(new JLabel("Status*:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// Action
		mainPanel.add(new JLabel("Action*:"), "align label");
		JScrollPane actionScroll = GUIHelper.createScrollPane(actionArea);
		actionScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(actionScroll, "growx,wrap");

		// Target
		mainPanel.add(new JLabel("Target:"), "align label");
		mainPanel.add(targetField, "growx,wrap");

		// Search Scope
		add(new JLabel("Type:"), "align label");
		add(searchScopeTypeCombo, "growx,wrap");
		add(new JLabel("Detail:"), "align label");
		add(searchScopeDetailArea, "growx");

		// Result
		mainPanel.add(new JLabel("Result:"), "align label");
		mainPanel.add(resultCombo, "growx,wrap");

		// Observation
		mainPanel.add(new JLabel("Observation:"), "align label");
		JScrollPane observationScroll = GUIHelper.createScrollPane(observationArea);
		observationScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(observationScroll, "growx,wrap");

		// Conclusion
		mainPanel.add(new JLabel("Conclusion:"), "align label");
		JScrollPane conclusionScroll = GUIHelper.createScrollPane(conclusionArea);
		conclusionScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(conclusionScroll, "growx,wrap");

		// Conclusion Confidence
		mainPanel.add(new JLabel("Conclusion Confidence:"), "align label");
		mainPanel.add(conclusionConfidenceCombo, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]10[]10[]"));
		panel.add(questionPanel, "growx");
		panel.add(parentField, "growx");
		panel.add(taskPanel, "growx");
		return panel;
	}

	@Override
	protected void loadData(){
		// Load date
		dateField.load(record);

		// Load simple fields (activity_type, status, action, result, conclusion_confidence)
		bindingManager.load(record);

		// Load action separately (it's a BoundTextArea)
		actionArea.setText(FLEFRecordHelper.getChildValue(record, TAG_ACTION));

		// Load target
		targetField.load(record);

		// Load search scope
		String type = FLEFRecordHelper.getChildValue(record, TAG_SEARCH_SCOPE_TYPE);
		searchScopeTypeCombo.setSelectedItem(StringUtils.defaultString(type));
		String detail = FLEFRecordHelper.getChildValue(record, TAG_SEARCH_SCOPE_DETAIL);
		searchScopeDetailArea.setText(StringUtils.defaultString(detail));

		// Load observation and conclusion
		observationArea.setText(FLEFRecordHelper.getChildValue(record, TAG_OBSERVATION));
		conclusionArea.setText(FLEFRecordHelper.getChildValue(record, TAG_CONCLUSION));

		// Load parent
		parentField.load(record);

		// Load questions
		questionPanel.load(record);

		// Load tasks
		taskPanel.load(record);

		// Load restriction and modification
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// Date is required
		if(!dateField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Date is required.",
				tabbedPane, mainPanel, dateField);
			return false;
		}

		// Activity type is required
		String activityType = (String)activityTypeCombo.getSelectedItem();
		if(StringUtils.isEmpty(activityType)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Activity type is required.",
				tabbedPane, mainPanel, activityTypeCombo);
			return false;
		}

		// Status is required
		String status = (String)statusCombo.getSelectedItem();
		if(StringUtils.isEmpty(status)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Status is required.",
				tabbedPane, mainPanel, statusCombo);
			return false;
		}

		// Action is required
		String action = actionArea.getText().trim();
		if(StringUtils.isEmpty(action)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Action is required.",
				tabbedPane, mainPanel, actionArea);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		// Clear existing children to avoid duplication
		FLEFRecordHelper.removeChildren(record, TAG_ACTION);
		FLEFRecordHelper.removeChildren(record, TAG_TARGET);
		FLEFRecordHelper.removeChildren(record, TAG_SEARCH_SCOPE_TYPE);
		FLEFRecordHelper.removeChildren(record, TAG_SEARCH_SCOPE_DETAIL);
		FLEFRecordHelper.removeChildren(record, TAG_OBSERVATION);
		FLEFRecordHelper.removeChildren(record, TAG_CONCLUSION);
		FLEFRecordHelper.removeChildren(record, TAG_PARENT);

		// Save date
		dateField.save(record);

		// Save simple fields (activity_type, status, result, conclusion_confidence)
		bindingManager.save(record);

		// Save action
		FLEFRecordHelper.updateChildValue(record, TAG_ACTION, actionArea.getText().trim());

		// Save target
		targetField.save(record);

		// Save search scope
		String type = (String)searchScopeTypeCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(type)){
			FLEFRecordHelper.updateChildValue(record, TAG_SEARCH_SCOPE_TYPE, type);
		}
		String detail = searchScopeDetailArea.getText().trim();
		if(StringUtils.isNotEmpty(detail)){
			FLEFRecordHelper.updateChildValue(record, TAG_SEARCH_SCOPE_DETAIL, detail);
		}

		// Save observation and conclusion
		FLEFRecordHelper.updateChildValue(record, TAG_OBSERVATION, observationArea.getText().trim());
		FLEFRecordHelper.updateChildValue(record, TAG_CONCLUSION, conclusionArea.getText().trim());

		// Save parent
		parentField.save(record);

		// Save questions (removes existing children with TAG_QUESTION)
		questionPanel.save(record);

		// Save tasks (removes existing children with TAG_TASK)
		taskPanel.save(record);

		// Save restriction and modification
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}

	// ------------------------------------------------------------------------
	// Main for testing
	// ------------------------------------------------------------------------

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}
		SwingUtilities.invokeLater(() -> {
			FLEFModel model = new FLEFModel();
			// Add dummy records for testing
			FLEFRecord q1 = FLEFRecord.createChild("research_question");
			q1.setId("@RQ1@");
			model.addRecord(q1);

			ResearchActivityRecordDialog dialog = ResearchActivityRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
