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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/**
 * Dialog for editing a {@code CONCLUSION_RECORD} according to FLEF 0.1.1.
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
public class _ConclusionRecord2Dialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2667811782933374258L;

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
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new IndividualAttributeHandler());
		HandlerRegistry.register(new GroupAttributeHandler());
//		HandlerRegistry.register(new IdentityHypothesisHandler());
		HandlerRegistry.register(new ResearchStatusHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new NoteHandler());
//		HandlerRegistry.register(new RestrictionHandler());
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
	private final ConclusionTargetListPanel resolvesPanel;
	private final JComboBox<String> preferredCombo;

	// References
	private final SourceCitationListPanel sourcePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	public static _ConclusionRecord2Dialog createNew(Dialog parent, FLEFModel model){
		return new _ConclusionRecord2Dialog(parent, model, null);
	}

	public static _ConclusionRecord2Dialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null) throw new IllegalArgumentException("Record cannot be null");
		return new _ConclusionRecord2Dialog(parent, model, record);
	}

	private _ConclusionRecord2Dialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ConclusionHandler.TYPE));

		contextField = new BoundTextField("CONTEXT", 30);
		proofStatusCombo = new BoundComboBox<>(TAG_PROOF_STATUS, new String[]{
			"", "unresearched", "conflicting_evidence", "supported", "proven", "disproven"
		});
		narrativeArea = new BoundTextArea("NARRATIVE", 5, 30);
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
		// context
		mainPanel.add(new JLabel("Context*:"), "align label");
		mainPanel.add(contextField, "growx,wrap");

		// proof status
		mainPanel.add(new JLabel("Proof Status*:"), "align label");
		mainPanel.add(proofStatusCombo, "growx,wrap");

		// narrative
		mainPanel.add(new JLabel("Narrative:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(narrativeArea), "growx,wrap");

		// date
		mainPanel.add(new JLabel("Date:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		// research
		mainPanel.add(new JLabel("Research:"), "align label");
		mainPanel.add(researchPanel, "growx,wrap");

		// resolves
		mainPanel.add(resolvesPanel, "span 2,growx,wrap");

		// preferred
		JPanel preferredPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		preferredPanel.add(new JLabel("Preferred:"), "align label");
		preferredPanel.add(preferredCombo, "growx");
		mainPanel.add(preferredPanel, "span 2,growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(sourcePanel, "growx");
		return panel;
	}

	private void updatePreferredCombo(){
		String currentSelection = (String)preferredCombo.getSelectedItem();
		preferredCombo.removeAllItems();
		preferredCombo.addItem(""); // empty

		for(FLEFRecord target : resolvesPanel.getItems()){
			String display = resolvesPanel.getDisplay(target);
			String id = XRefHelper.extractXRef(target.getValue());
			preferredCombo.addItem(id + " - " + display);
		}

		// Restore selection if possible
		if(currentSelection != null && !currentSelection.isEmpty()){
			for(int i = 0; i < preferredCombo.getItemCount(); i++){
				if(preferredCombo.getItemAt(i).equals(currentSelection)){
					preferredCombo.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	@Override
	protected void loadData(){
		contextField.setText(FLEFRecordHelper.getChildValue(record, TAG_CONTEXT));
		proofStatusCombo.setSelectedItem(FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS));
		narrativeArea.setText(FLEFRecordHelper.getChildValue(record, TAG_NARRATIVE));

		dateField.load(record);
		//TODO
//		researchPanel.load(record);
		resolvesPanel.load(record);

		updatePreferredCombo();

		// Load preferred
		String prefRef = FLEFRecordHelper.getChildValue(record, TAG_PREFERRED);
		if(StringUtils.isNotEmpty(prefRef)){
			String prefId = XRefHelper.extractXRef(prefRef);
			// Find and select in combo
			for(int i = 0; i < preferredCombo.getItemCount(); i++){
				String item = preferredCombo.getItemAt(i);
				if(item != null && item.startsWith(prefId)){
					preferredCombo.setSelectedIndex(i);
					break;
				}
			}
		}

		sourcePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		String context = contextField.getText().trim();
		if(context.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Context is required.",
				tabbedPane, mainPanel, contextField);
			return false;
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(StringUtils.isEmpty(proofStatus)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Proof status is required.",
				tabbedPane, mainPanel, proofStatusCombo);
			return false;
		}

		// Check preferred in resolves constraint
		String selectedPreferred = (String)preferredCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(selectedPreferred)){
			// Extract the ID from the selected item
			int dashIdx = selectedPreferred.indexOf(" - ");
			if(dashIdx > 0){
				String prefId = selectedPreferred.substring(0, dashIdx);
				List<String> resolveIds = resolvesPanel.getTargetIds();
				if(!resolveIds.contains(prefId)){
					GUIHelper.showValidationErrorAndFocus(this,
						"Preferred must be one of the resolves.",
						tabbedPane, mainPanel, preferredCombo);
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected void saveData(){
		// Simple fields
		FLEFRecordHelper.updateChildValue(record, TAG_CONTEXT, contextField.getText().trim());
		FLEFRecordHelper.updateChildValue(record, TAG_PROOF_STATUS, (String)proofStatusCombo.getSelectedItem());
		FLEFRecordHelper.updateChildValue(record, TAG_NARRATIVE, narrativeArea.getText().trim());

		dateField.save(record);
		//TODO
//		researchPanel.save(record);
		resolvesPanel.save(record);

		// Preferred
		FLEFRecordHelper.removeChildren(record, TAG_PREFERRED);
		String selectedPreferred = (String)preferredCombo.getSelectedItem();
		if(StringUtils.isNotEmpty(selectedPreferred)){
			int dashIdx = selectedPreferred.indexOf(" - ");
			if(dashIdx > 0){
				String prefId = selectedPreferred.substring(0, dashIdx);
				FLEFRecordHelper.updateChildValue(record, TAG_PREFERRED, XRefHelper.formatXRef(prefId));
			}
		}

		sourcePanel.save(record);
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
			// Add some dummy records (optional)
			_ConclusionRecord2Dialog dialog = _ConclusionRecord2Dialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
