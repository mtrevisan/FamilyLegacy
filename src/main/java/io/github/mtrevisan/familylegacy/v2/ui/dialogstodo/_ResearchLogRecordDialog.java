package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.FollowUpListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ResearchLogTargetField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ResearchStatusListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SearchScopePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchLogHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.io.Serial;


/**
 * Dialog for editing a {@code ResearchLogRecord} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchLogRecord {
 *   id: LocalID
 *   action: Text
 *   target?: XrefOrVoid&lt;LocalID&gt;
 *   source*: Xref&lt;SourceRecord&gt;
 *   search_scope?: struct { type: enum { ... }, detail?: Text }
 *   search_outcome?: enum { found, not_found, partially_found, unreadable, destroyed }
 *   finding?: Text
 *   next_step?: Text
 *   follow_up*: Xref&lt;ResearchLogRecord&gt;
 *   research*: Xref&lt;ResearchStatusRecord&gt;
 *   date: Date
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class _ResearchLogRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2302945418426243014L;

	private static final String TAG_ACTION = "ACTION";
	private static final String TAG_SEARCH_OUTCOME = "SEARCH_OUTCOME";
	private static final String TAG_FINDING = "FINDING";
	private static final String TAG_NEXT_STEP = "NEXT_STEP";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	static{
		HandlerRegistry.register(new ResearchLogHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new ResearchStatusHandler());
	}

	private final BindingManager bindingManager = new BindingManager();
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]5[]10[]5[]10[]10[]"));

	// Fields
	private final BoundTextArea actionArea;
	private final ResearchLogTargetField targetField;
	private final SourceCitationListPanel sourcePanel;
	private final SearchScopePanel searchScopePanel;
	private final BoundComboBox<String> searchOutcomeCombo;
	private final BoundTextArea findingArea;
	private final BoundTextArea nextStepArea;
	private final FollowUpListPanel followUpPanel;
	private final ResearchStatusListPanel researchPanel;
	private final DateField dateField;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	// Handler for source selection
	private final SourceHandler sourceHandler = new SourceHandler();

	/**
	 * Creates a new dialog to create a new record.
	 */
	public static _ResearchLogRecordDialog createNew(Dialog parent, FLEFModel model){
		return new _ResearchLogRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 */
	public static _ResearchLogRecordDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null) throw new IllegalArgumentException("Record cannot be null");
		return new _ResearchLogRecordDialog(parent, model, record);
	}

	private _ResearchLogRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchLogHandler.TYPE));

		// Initialize components
		actionArea = new BoundTextArea(TAG_ACTION, 3, 30);
		targetField = new ResearchLogTargetField(parent, model, sourceHandler);
		sourcePanel = new SourceCitationListPanel("SOURCE", parent, model);
		searchScopePanel = new SearchScopePanel();
		searchOutcomeCombo = new BoundComboBox<>(TAG_SEARCH_OUTCOME, new String[]{
			"", "found", "not_found", "partially_found", "unreadable", "destroyed"
		});
		findingArea = new BoundTextArea(TAG_FINDING, 3, 30);
		nextStepArea = new BoundTextArea(TAG_NEXT_STEP, 3, 30);
		followUpPanel = new FollowUpListPanel(parent, model);
		researchPanel = new ResearchStatusListPanel(parent, model);
		dateField = DateField.createWithWrapperTag(TAG_DATE, parent, "Search Date", model);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, parent);
		modificationPanel = new ModificationPanel(parent);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(actionArea);
		bindingManager.bind(searchOutcomeCombo);
		bindingManager.bind(findingArea);
		bindingManager.bind(nextStepArea);

		setLayout(new MigLayout("ins 10,fillx,top"));
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// Action
		mainPanel.add(new JLabel("Action*:"), "align label");
		JScrollPane actionScroll = GUIHelper.createScrollPane(actionArea);
		actionScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(actionScroll, "growx,wrap");

		// Target
		mainPanel.add(new JLabel("Target:"), "align label");
		mainPanel.add(targetField, "growx,wrap");

		// Search Scope
		mainPanel.add(new JLabel("Search Scope:"), "align label");
		mainPanel.add(searchScopePanel, "growx,wrap");

		// Search Outcome
		mainPanel.add(new JLabel("Search Outcome:"), "align label");
		mainPanel.add(searchOutcomeCombo, "growx,wrap");

		// Finding
		mainPanel.add(new JLabel("Finding:"), "align label");
		JScrollPane findingScroll = GUIHelper.createScrollPane(findingArea);
		findingScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(findingScroll, "growx,wrap");

		// Next Step
		mainPanel.add(new JLabel("Next Step:"), "align label");
		JScrollPane nextStepScroll = GUIHelper.createScrollPane(nextStepArea);
		nextStepScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(nextStepScroll, "growx,wrap");

		// Date
		mainPanel.add(new JLabel("Date*:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]5[]"));
		panel.add(sourcePanel, "growx");
		panel.add(followUpPanel, "growx");
		panel.add(researchPanel, "growx");
		return panel;
	}

	@Override
	protected void loadData(){
		// Load simple fields
		actionArea.setText(FLEFRecordHelper.getChildValue(record, TAG_ACTION));

		String outcome = FLEFRecordHelper.getChildValue(record, TAG_SEARCH_OUTCOME);
		searchOutcomeCombo.setSelectedItem(StringUtils.defaultString(outcome));

		findingArea.setText(FLEFRecordHelper.getChildValue(record, TAG_FINDING));
		nextStepArea.setText(FLEFRecordHelper.getChildValue(record, TAG_NEXT_STEP));

		// Load target
		targetField.load(record, "TARGET");

		// Load search scope
		FLEFRecord searchScope = FLEFRecordHelper.findChild(record, "SEARCH_SCOPE");
		if(searchScope != null){
			searchScopePanel.load(searchScope);
		}

		// Load lists
		sourcePanel.load(record);
		followUpPanel.load(record);
//		researchPanel.load(record);

		// Load date
		dateField.load(record);

		// Load restriction and modification
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// Action is required
		String action = actionArea.getText().trim();
		if(StringUtils.isEmpty(action)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Action is required.",
				tabbedPane, mainPanel, actionArea);
			return false;
		}

		// Date is required
		if(!dateField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Date is required.",
				tabbedPane, mainPanel, dateField);
			return false;
		}

		// Check for cyclic follow_up references (basic check)
//		String currentId = record != null? record.getId(): null;
//		if(StringUtils.isNotEmpty(currentId)){
//			for(String followUpId : followUpPanel.getFollowUpIds()){
//				if(currentId.equals(followUpId)){
//					GUIHelper.showValidationErrorAndFocus(this,
//						"A research log cannot follow up on itself.",
//						tabbedPane, tabbedPane.getComponentAt(1), followUpPanel);
//					return false;
//				}
//			}
//		}

		return true;
	}

	@Override
	protected void saveData(){
		// Clear existing children
		FLEFRecordHelper.removeChildren(record, TAG_ACTION);
		FLEFRecordHelper.removeChildren(record, TAG_SEARCH_OUTCOME);
		FLEFRecordHelper.removeChildren(record, TAG_FINDING);
		FLEFRecordHelper.removeChildren(record, TAG_NEXT_STEP);
		FLEFRecordHelper.removeChildren(record, "TARGET");
		FLEFRecordHelper.removeChildren(record, "SEARCH_SCOPE");

		// Save simple fields
		FLEFRecordHelper.updateChildValue(record, TAG_ACTION, actionArea.getText().trim());

		String outcome = (String)searchOutcomeCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(outcome)){
			FLEFRecordHelper.updateChildValue(record, TAG_SEARCH_OUTCOME, outcome);
		}

		FLEFRecordHelper.updateChildValue(record, TAG_FINDING, findingArea.getText().trim());
		FLEFRecordHelper.updateChildValue(record, TAG_NEXT_STEP, nextStepArea.getText().trim());

		// Save target
		targetField.save(record, "TARGET");

		// Save search scope
		if(searchScopePanel.hasData()){
			FLEFRecord searchScope = FLEFRecord.createChild("SEARCH_SCOPE");
			searchScopePanel.save(searchScope);
			record.addChild(searchScope);
		}

		// Save lists (these will remove existing children with their tags)
		sourcePanel.save(record);
		followUpPanel.save(record);
//		researchPanel.save(record);

		// Save date
		dateField.save(record);

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
			_ResearchLogRecordDialog dialog = _ResearchLogRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
