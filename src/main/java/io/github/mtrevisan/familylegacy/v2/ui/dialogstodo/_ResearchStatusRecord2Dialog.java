package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
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
 * Dialog for editing a {@code ResearchStatusRecord} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchStatusRecord {
 *   id: LocalID
 *   question: Text
 *   status?: enum { active, completed, blocked }
 *   priority?: enum { high, medium, low }
 *   association*: struct { target: XrefOrVoid&lt;LocalID&gt;, name?: Text }
 *   blocked_by*: Xref&lt;ResearchStatusRecord&gt;
 *   plan?: Text
 *   resolution?: Text
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class _ResearchStatusRecord2Dialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 4693851314612375503L;

	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_PRIORITY = "PRIORITY";
	private static final String TAG_PLAN = "PLAN";
	private static final String TAG_RESOLUTION = "RESOLUTION";

	static{
		HandlerRegistry.register(new ResearchStatusHandler());
	}

	private final BindingManager bindingManager = new BindingManager();
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]5[]10[]5[]10[]"));

	// Fields
	private final BoundTextArea questionArea;
	private final BoundComboBox<String> statusCombo;
	private final BoundComboBox<String> priorityCombo;
	private final BoundTextArea planArea;
	private final BoundTextArea resolutionArea;

	// List panels
//	private final ResearchAssociationListPanel associationPanel;
//	private final BlockedByListPanel blockedByPanel;

	// Modification
	private final ModificationPanel modificationPanel;

	/**
	 * Creates a new dialog to create a new record.
	 */
	public static _ResearchStatusRecord2Dialog createNew(Dialog parent, FLEFModel model){
		return new _ResearchStatusRecord2Dialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 */
	public static _ResearchStatusRecord2Dialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null) throw new IllegalArgumentException("Record cannot be null");
		return new _ResearchStatusRecord2Dialog(parent, model, record);
	}

	private _ResearchStatusRecord2Dialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchStatusHandler.TYPE));

		// Initialize components
		questionArea = new BoundTextArea(TAG_QUESTION, 3, 30);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{"", "active", "completed", "blocked"});
		priorityCombo = new BoundComboBox<>(TAG_PRIORITY, new String[]{"", "high", "medium", "low"});
		planArea = new BoundTextArea(TAG_PLAN, 3, 30);
		resolutionArea = new BoundTextArea(TAG_RESOLUTION, 3, 30);

//		associationPanel = new ResearchAssociationListPanel(this, model);
//		blockedByPanel = new BlockedByListPanel(this, model);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(questionArea);
		bindingManager.bind(statusCombo);
		bindingManager.bind(priorityCombo);
		bindingManager.bind(planArea);
		bindingManager.bind(resolutionArea);

		setLayout(new MigLayout("ins 10,fillx,top"));
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// Question
		mainPanel.add(new JLabel("Question*:"), "align label");
		JScrollPane questionScroll = GUIHelper.createScrollPane(questionArea);
		questionScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(questionScroll, "growx,wrap");

		// Status
		mainPanel.add(new JLabel("Status:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// Priority
		mainPanel.add(new JLabel("Priority:"), "align label");
		mainPanel.add(priorityCombo, "growx,wrap");

		// Plan
		mainPanel.add(new JLabel("Plan:"), "align label");
		JScrollPane planScroll = GUIHelper.createScrollPane(planArea);
		planScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(planScroll, "growx,wrap");

		// Resolution
		mainPanel.add(new JLabel("Resolution:"), "align label");
		JScrollPane resolutionScroll = GUIHelper.createScrollPane(resolutionArea);
		resolutionScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(resolutionScroll, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
//		panel.add(associationPanel, "growx");
//		panel.add(blockedByPanel, "growx");
		return panel;
	}

	@Override
	protected void loadData(){
		// Load simple fields
		questionArea.setText(FLEFRecordHelper.getChildValue(record, TAG_QUESTION));
		String status = FLEFRecordHelper.getChildValue(record, TAG_STATUS);
		statusCombo.setSelectedItem(StringUtils.defaultString(status));
		String priority = FLEFRecordHelper.getChildValue(record, TAG_PRIORITY);
		priorityCombo.setSelectedItem(StringUtils.defaultString(priority));
		planArea.setText(FLEFRecordHelper.getChildValue(record, TAG_PLAN));
		resolutionArea.setText(FLEFRecordHelper.getChildValue(record, TAG_RESOLUTION));

		// Load association and blocked_by lists
//		associationPanel.load(record);
//		blockedByPanel.load(record);

		// Load modification
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// Question is required
		String question = questionArea.getText().trim();
		if(StringUtils.isEmpty(question)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Question is required.",
				tabbedPane, mainPanel, questionArea);
			return false;
		}

		// Check for circular blocked_by references (basic check)
		// A more thorough check would require full graph analysis
		String currentId = record != null? record.getId(): null;
		if(StringUtils.isNotEmpty(currentId)){
//			for(String blockedId : blockedByPanel.getBlockedByIds()){
//				if(currentId.equals(blockedId)){
//					GUIHelper.showValidationErrorAndFocus(this,
//						"A research status cannot be blocked by itself.",
//						tabbedPane, tabbedPane.getComponentAt(1), blockedByPanel);
//					return false;
//				}
//			}
		}

		return true;
	}

	@Override
	protected void saveData(){
		// Clear existing children to avoid duplication
		FLEFRecordHelper.removeChildren(record, TAG_QUESTION);
		FLEFRecordHelper.removeChildren(record, TAG_STATUS);
		FLEFRecordHelper.removeChildren(record, TAG_PRIORITY);
		FLEFRecordHelper.removeChildren(record, TAG_PLAN);
		FLEFRecordHelper.removeChildren(record, TAG_RESOLUTION);

		// Save simple fields
		FLEFRecordHelper.updateChildValue(record, TAG_QUESTION, questionArea.getText().trim());
		String status = (String)statusCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(status)){
			FLEFRecordHelper.updateChildValue(record, TAG_STATUS, status);
		}
		String priority = (String)priorityCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(priority)){
			FLEFRecordHelper.updateChildValue(record, TAG_PRIORITY, priority);
		}
		FLEFRecordHelper.updateChildValue(record, TAG_PLAN, planArea.getText().trim());
		FLEFRecordHelper.updateChildValue(record, TAG_RESOLUTION, resolutionArea.getText().trim());

		// Save lists
//		associationPanel.save(record);
//		blockedByPanel.save(record);

		// Save modification
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
			_ResearchStatusRecord2Dialog dialog = _ResearchStatusRecord2Dialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
