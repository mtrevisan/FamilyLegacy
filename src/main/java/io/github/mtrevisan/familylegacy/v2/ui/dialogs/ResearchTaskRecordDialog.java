package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.*;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;


/* ONGOING */
/**
 * Dialog for editing a {@code ResearchTaskRecord} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchTaskRecord {
 *   id: LocalID
 *   description: Text
 *   question*: Xref&lt;ResearchQuestionRecord&gt;
 *   created_by?: Xref&lt;ResearchActivityRecord&gt;
 *   status: enum { open, in_progress, completed, abandoned }
 *   priority?: enum { low, normal, high }
 *   due_date?: Date
 *   outcome?: Text
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class ResearchTaskRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String TAG_DESCRIPTION = "DESCRIPTION";
	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_CREATED_BY = "CREATED_BY";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_PRIORITY = "PRIORITY";
	private static final String TAG_DUE_DATE = "DUE_DATE";
	private static final String TAG_OUTCOME = "OUTCOME";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	static{
		HandlerRegistry.register(new ResearchTaskHandler());
	}

	private final BindingManager bindingManager = new BindingManager();
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]10[]5[]5[]"));

	// Fields
	private final BoundTextArea descriptionArea;
	private final ResearchQuestionListPanel questionPanel;
	private final ResearchActivityParentField createdByField;
	private final BoundComboBox<String> statusCombo;
	private final BoundComboBox<String> priorityCombo;
	private final DateField dueDateField;
	private final BoundTextArea outcomeArea;

	// Restriction and Modification
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	/**
	 * Creates a new dialog to create a new record.
	 */
	public static ResearchTaskRecordDialog createNew(Dialog parent, FLEFModel model){
		return new ResearchTaskRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 */
	public static ResearchTaskRecordDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null) throw new IllegalArgumentException("Record cannot be null");
		return new ResearchTaskRecordDialog(parent, model, record);
	}

	private ResearchTaskRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchTaskHandler.TYPE));

		// Initialize components
		descriptionArea = new BoundTextArea(TAG_DESCRIPTION, 3, 30);
		questionPanel = new ResearchQuestionListPanel(model, parent);
		createdByField = new ResearchActivityParentField(parent, model);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{"", "open", "in_progress", "completed", "abandoned"});
		priorityCombo = new BoundComboBox<>(TAG_PRIORITY, new String[]{"", "low", "normal", "high"});
		dueDateField = DateField.createWithWrapperTag(TAG_DUE_DATE, parent, "Due Date", model);
		outcomeArea = new BoundTextArea(TAG_OUTCOME, 3, 30);

		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, parent);
		modificationPanel = new ModificationPanel(parent);

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(descriptionArea);
		bindingManager.bind(statusCombo);
		bindingManager.bind(priorityCombo);
		bindingManager.bind(outcomeArea);

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
		// Description
		mainPanel.add(new JLabel("Description*:"), "align label");
		JScrollPane descScroll = GUIHelper.createScrollPane(descriptionArea);
		descScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(descScroll, "growx,wrap");

		// Status
		mainPanel.add(new JLabel("Status*:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// Priority
		mainPanel.add(new JLabel("Priority:"), "align label");
		mainPanel.add(priorityCombo, "growx,wrap");

		// Due Date
		mainPanel.add(new JLabel("Due Date:"), "align label");
		mainPanel.add(dueDateField, "growx,wrap");

		// Outcome
		mainPanel.add(new JLabel("Outcome:"), "align label");
		JScrollPane outcomeScroll = GUIHelper.createScrollPane(outcomeArea);
		outcomeScroll.setPreferredSize(new Dimension(200, 80));
		mainPanel.add(outcomeScroll, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(questionPanel, "growx");
		panel.add(createdByField, "growx");
		return panel;
	}

	@Override
	protected void loadData(){
		// Load simple fields
		descriptionArea.setText(FLEFRecordHelper.getChildValue(record, TAG_DESCRIPTION));
		statusCombo.setSelectedItem(StringUtils.defaultString(
			FLEFRecordHelper.getChildValue(record, TAG_STATUS)));
		priorityCombo.setSelectedItem(StringUtils.defaultString(
			FLEFRecordHelper.getChildValue(record, TAG_PRIORITY)));
		outcomeArea.setText(FLEFRecordHelper.getChildValue(record, TAG_OUTCOME));

		// Load due date
		dueDateField.load(record);

		// Load questions
		questionPanel.load(record);

		// Load created_by
		createdByField.load(record);

		// Load restriction and modification
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// Description is required
		String desc = descriptionArea.getText().trim();
		if(StringUtils.isEmpty(desc)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Description is required.",
				tabbedPane, mainPanel, descriptionArea);
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

		return true;
	}

	@Override
	protected void saveData(){
		// Clear existing children to avoid duplication
		FLEFRecordHelper.removeChildren(record, TAG_DESCRIPTION);
		FLEFRecordHelper.removeChildren(record, TAG_STATUS);
		FLEFRecordHelper.removeChildren(record, TAG_PRIORITY);
		FLEFRecordHelper.removeChildren(record, TAG_OUTCOME);
		FLEFRecordHelper.removeChildren(record, TAG_CREATED_BY);

		// Save simple fields
		FLEFRecordHelper.updateChildValue(record, TAG_DESCRIPTION, descriptionArea.getText().trim());
		FLEFRecordHelper.updateChildValue(record, TAG_STATUS, (String)statusCombo.getSelectedItem());
		String priority = (String)priorityCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(priority)){
			FLEFRecordHelper.updateChildValue(record, TAG_PRIORITY, priority);
		}
		FLEFRecordHelper.updateChildValue(record, TAG_OUTCOME, outcomeArea.getText().trim());

		// Save due date
		dueDateField.save(record);

		// Save questions
		questionPanel.save(record);

		// Save created_by
		createdByField.save(record);

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

			ResearchTaskRecordDialog dialog = ResearchTaskRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
