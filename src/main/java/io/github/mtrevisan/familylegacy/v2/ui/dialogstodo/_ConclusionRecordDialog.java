package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionTargetListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ResearchStatusListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
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
import java.util.List;


/**
 * Dialog for editing a {@code ConclusionRecord} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ConclusionRecord {
 *   id: LocalID
 *   context: Text
 *   resolves*: ConclusionTarget
 *   preferred?: ConclusionTarget
 *   proof_status: enum { unresearched, conflicting_evidence, supported, proven, disproven }
 *   narrative?: Text
 *   research*: Xref&lt;ResearchStatusRecord&gt;
 *   date?: Date
 *   source*: SourceCitation
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 *   require preferred in resolves
 * }
 * </pre>
 */
public class _ConclusionRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4774258540749269809L;

	private static final String TAG_CONTEXT = "CONTEXT";
	private static final String TAG_PROOF_STATUS = "PROOF_STATUS";
	private static final String TAG_NARRATIVE = "NARRATIVE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_RESEARCH = "RESEARCH";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";
	private static final String TAG_PREFERRED = "PREFERRED";

	static{
		HandlerRegistry.register(new ConclusionHandler());
		HandlerRegistry.register(new ResearchStatusHandler());
		HandlerRegistry.register(new SourceHandler());
	}

	private final BindingManager bindingManager = new BindingManager();
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]5[]10[]5[]10[]"));

	// Fields
	private final BoundTextField contextField;
	private final BoundComboBox<String> proofStatusCombo;
	private final BoundTextArea narrativeArea;
	private final DateField dateField;
	private final ResearchStatusListPanel researchPanel;

	// Resolves
	private final ConclusionTargetListPanel resolvesPanel;

	// Preferred (combo populated from resolves)
	private final JComboBox<String> preferredCombo;

	// Sources
	private final SourceCitationListPanel sourcePanel;

	// Restriction and Modification
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	/**
	 * Creates a new dialog to create a new record.
	 */
	public static _ConclusionRecordDialog createNew(Dialog parent, FLEFModel model){
		return new _ConclusionRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 */
	public static _ConclusionRecordDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null) throw new IllegalArgumentException("Record cannot be null");
		return new _ConclusionRecordDialog(parent, model, record);
	}

	private _ConclusionRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ConclusionHandler.TYPE));

		// Initialize components
		contextField = new BoundTextField("CONTEXT", 30);
		proofStatusCombo = new BoundComboBox<>(TAG_PROOF_STATUS, new String[]{
			"", "unresearched", "conflicting_evidence", "supported", "proven", "disproven"
		});
		narrativeArea = new BoundTextArea(TAG_NARRATIVE, 4, 30);
		dateField = DateField.createWithWrapperTag(TAG_DATE, parent, "Conclusion Date", model);
		researchPanel = new ResearchStatusListPanel(parent, model);
		resolvesPanel = new ConclusionTargetListPanel(parent, model);
		preferredCombo = new JComboBox<>();
		preferredCombo.addItem(""); // empty selection

		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, parent, model);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, parent);
		modificationPanel = new ModificationPanel(parent);

		// Update preferred combo when resolves change
		resolvesPanel.addPropertyChangeListener("items", evt -> updatePreferredCombo());


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(contextField);
		bindingManager.bind(proofStatusCombo);
		bindingManager.bind(narrativeArea);

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
		// Context
		mainPanel.add(new JLabel("Context*:"), "align label");
		mainPanel.add(contextField, "growx,wrap");

		// Proof Status
		mainPanel.add(new JLabel("Proof Status*:"), "align label");
		mainPanel.add(proofStatusCombo, "growx,wrap");

		// Narrative
		mainPanel.add(new JLabel("Narrative:"), "align label");
		JScrollPane narrativeScroll = GUIHelper.createScrollPane(narrativeArea);
		narrativeScroll.setPreferredSize(new Dimension(200, 100));
		mainPanel.add(narrativeScroll, "growx,wrap");

		// Date
		mainPanel.add(new JLabel("Date:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		// Research
		mainPanel.add(new JLabel("Research:"), "align label");
		mainPanel.add(researchPanel, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]5[]"));

		// Resolves
		panel.add(resolvesPanel, "growx");

		// Preferred
		JPanel preferredPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		preferredPanel.add(new JLabel("Preferred:"), "align label");
		preferredPanel.add(preferredCombo, "growx");
		panel.add(preferredPanel, "growx");

		// Sources
		panel.add(sourcePanel, "growx");

		return panel;
	}

	private void updatePreferredCombo(){
		String currentSelection = (String)preferredCombo.getSelectedItem();
		preferredCombo.removeAllItems();
		preferredCombo.addItem(""); // empty

		List<String> targetIds = resolvesPanel.getTargetIds();
//		List<String> targetDisplays = resolvesPanel.getTargetDisplayStrings();

		for(int i = 0; i < targetIds.size(); i++){
			String id = targetIds.get(i);
//			String display = targetDisplays.get(i);
//			preferredCombo.addItem(id + " - " + display);
		}

		// Restore selection if possible
		if(StringUtils.isNotEmpty(currentSelection)){
			for(int i = 0; i < preferredCombo.getItemCount(); i++){
				String item = preferredCombo.getItemAt(i);
				if(item != null && item.startsWith(currentSelection)){
					preferredCombo.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	@Override
	protected void loadData(){
		// Load simple fields
		contextField.setText(FLEFRecordHelper.getChildValue(record, TAG_CONTEXT));
		proofStatusCombo.setSelectedItem(StringUtils.defaultString(
			FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS)));
		narrativeArea.setText(FLEFRecordHelper.getChildValue(record, TAG_NARRATIVE));

		// Load date
		dateField.load(record);

		// Load research
//		researchPanel.load(record);

		// Load resolves
		resolvesPanel.load(record);

		// Update preferred combo with resolves
		updatePreferredCombo();

		// Load preferred
//		String prefRef = FLEFRecordHelper.getChildValue(record, TAG_PREFERRED);
//		if(StringUtils.isNotEmpty(prefRef)){
//			String prefId = XRefHelper.extractId(prefRef);
//			for(int i = 0; i < preferredCombo.getItemCount(); i++){
//				String item = preferredCombo.getItemAt(i);
//				if(item != null && item.startsWith(prefId + " - ")){
//					preferredCombo.setSelectedIndex(i);
//					break;
//				}
//			}
//		}

		// Load sources
		sourcePanel.load(record);

		// Load restriction and modification
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// Context is required
		String context = contextField.getText().trim();
		if(StringUtils.isEmpty(context)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Context is required.",
				tabbedPane, mainPanel, contextField);
			return false;
		}

		// Proof status is required
		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(StringUtils.isEmpty(proofStatus)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Proof status is required.",
				tabbedPane, mainPanel, proofStatusCombo);
			return false;
		}

		// Check: preferred must be in resolves
//		String selectedPreferred = (String)preferredCombo.getSelectedItem();
//		if(StringUtils.isNotEmpty(selectedPreferred)){
//			int dashIdx = selectedPreferred.indexOf(" - ");
//			if(dashIdx > 0){
//				String prefId = selectedPreferred.substring(0, dashIdx);
//				List<String> resolveIds = resolvesPanel.getTargetIds();
//				if(!resolveIds.contains(prefId)){
//					GUIHelper.showValidationErrorAndFocus(this,
//						"Preferred must be one of the resolves.",
//						tabbedPane, tabbedPane.getComponentAt(1), preferredCombo);
//					return false;
//				}
//			}
//		}

		return true;
	}

	@Override
	protected void saveData(){
		// Clear existing children
		FLEFRecordHelper.removeChildren(record, TAG_CONTEXT);
		FLEFRecordHelper.removeChildren(record, TAG_PROOF_STATUS);
		FLEFRecordHelper.removeChildren(record, TAG_NARRATIVE);
		FLEFRecordHelper.removeChildren(record, TAG_PREFERRED);

		// Save simple fields
		FLEFRecordHelper.updateChildValue(record, TAG_CONTEXT, contextField.getText().trim());
		FLEFRecordHelper.updateChildValue(record, TAG_PROOF_STATUS, (String)proofStatusCombo.getSelectedItem());
		FLEFRecordHelper.updateChildValue(record, TAG_NARRATIVE, narrativeArea.getText().trim());

		// Save date
		dateField.save(record);

		// Save research
//		researchPanel.save(record);

		// Save resolves
		resolvesPanel.save(record);

		// Save preferred
		String selectedPreferred = (String)preferredCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(selectedPreferred)){
			int dashIdx = selectedPreferred.indexOf(" - ");
			if(dashIdx > 0){
				String prefId = selectedPreferred.substring(0, dashIdx);
				FLEFRecordHelper.updateChildValue(record, TAG_PREFERRED, XRefHelper.formatXRef(prefId));
			}
		}

		// Save sources (this will remove existing children with TAG_SOURCE)
		sourcePanel.save(record);

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
			// Add some dummy records for testing
			FLEFRecord source1 = FLEFRecord.createChild("source");
			source1.setId("@S1@");
			model.addRecord(source1);

			_ConclusionRecordDialog dialog = _ConclusionRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
