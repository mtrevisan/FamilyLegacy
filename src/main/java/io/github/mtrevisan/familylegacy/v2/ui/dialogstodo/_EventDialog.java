package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.ItemEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Dialog for editing an {@code INDIVIDUAL_EVENT_RECORD} according to FLEF 0.0.9.
 * <p>
 * Supports BIRTH, ADOPTION, and generic event types.
 */
public class _EventDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -9191829528682252778L;


	private final BindingManager bindingManager = new BindingManager();

	private final JTextField idField = new JTextField(10);
	private final BoundComboBox typeCombo;
	private final JTextField familyField = new JTextField(15);
	private final JTextField twinField = new JTextField(30);
	private final JComboBox<String> relationshipParent1Combo = new JComboBox<>(new String[]{StringUtils.EMPTY, "biological", "adopted", "foster", "guardian"});
	private final JComboBox<String> relationshipParent2Combo = new JComboBox<>(new String[]{StringUtils.EMPTY, "biological", "adopted", "foster", "guardian"});

	private final JTextArea descriptionArea = new JTextArea(3, 30);
	private final JTextField dateField = new JTextField(20);
	private final JTextField placeField = new JTextField(15);
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("PLACE", "Place Evidence");
	private final JTextField agencyField = new JTextField(20);
	private final JTextField causeField = new JTextField(20);
	private final EvidenceQualifiersPanel causeQualifiers = new EvidenceQualifiersPanel("CAUSE", "Cause Evidence");

	private final EvidenceQualifiersPanel eventQualifiers = new EvidenceQualifiersPanel(null, "Event Evidence");
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final JTextField culturalNormsField = new JTextField(30);
	private final JTextField notesField = new JTextField(30);
	private final JTextField sourcesField = new JTextField(30);

	private final JTextArea conclusionArea = new JTextArea(5, 30);

	private final JTextArea modificationArea = new JTextArea(5, 30);


	/**
	 * Creates a dialog to edit an existing event record.
	 */
	public _EventDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(IndividualEventHandler.TYPE));

		typeCombo = new BoundComboBox("TYPE", new String[]{});
		initTypeCombo();

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * Creates a dialog to create a new event record.
	 */
	public _EventDialog(Dialog parent, FLEFModel model){
		super(parent, model, null, HandlerRegistry.getHandler(IndividualEventHandler.TYPE));

		typeCombo = new BoundComboBox("TYPE", new String[]{});
		initTypeCombo();

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		bindingManager.bind(typeCombo);

		// Create tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		JPanel basicPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		idField.setEditable(false);
		idField.setText(record != null? record.getId(): StringUtils.EMPTY);
		basicPanel.add(new JLabel("ID:"), "align label");
		basicPanel.add(idField, "growx,wrap");
		basicPanel.add(new JLabel("Type:"), "align label");
		basicPanel.add(typeCombo, "growx,wrap");
		basicPanel.add(new JLabel("Family (ID):"), "align label");
		basicPanel.add(familyField, "growx,wrap");
		basicPanel.add(new JLabel("Twins (IDs, comma separated):"), "align label");
		basicPanel.add(twinField, "growx,wrap");
		basicPanel.add(new JLabel("Parent 1 Relationship:"), "align label");
		basicPanel.add(relationshipParent1Combo, "growx,wrap");
		basicPanel.add(new JLabel("Parent 2 Relationship:"), "align label");
		basicPanel.add(relationshipParent2Combo, "growx");
		tabbedPane.addTab("Basic", basicPanel);

		// --- Details tab (EVENT_STRUCTURE) ---
		JPanel detailsPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		detailsPanel.add(new JLabel("Description:"), "align label,top");
		JScrollPane descScroll = GUIHelper.createScrollPane(descriptionArea);
		detailsPanel.add(descScroll, "growx,wrap");

		detailsPanel.add(new JLabel("Date:"), "align label");
		detailsPanel.add(dateField, "growx,wrap");

		detailsPanel.add(new JLabel("Place (ID):"), "align label");
		detailsPanel.add(placeField, "growx,wrap");
		// PLACE -> CERTAINTY + CREDIBILITY (grouped in EvidenceQualifiersPanel)
		detailsPanel.add(placeQualifiers, "span 2,growx,wrap");

		detailsPanel.add(new JLabel("Agency:"), "align label");
		detailsPanel.add(agencyField, "growx,wrap");

		detailsPanel.add(new JLabel("Cause:"), "align label");
		detailsPanel.add(causeField, "growx,wrap");
		// CAUSE -> CERTAINTY + CREDIBILITY (grouped in EvidenceQualifiersPanel)
		detailsPanel.add(causeQualifiers, "span 2,growx");
		tabbedPane.addTab("Details", detailsPanel);

		// --- Assessments tab ---
		JPanel assessmentsPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		// EVENT -> CERTAINTY + CREDIBILITY (grouped in EvidenceQualifiersPanel)
		assessmentsPanel.add(eventQualifiers, "span 2,growx,wrap");
		assessmentsPanel.add(restrictionCheckBox, "span 2,wrap");
		tabbedPane.addTab("Assessments", assessmentsPanel);

		// --- Links tab ---
		JPanel linksPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		linksPanel.add(new JLabel("Cultural Norms (IDs):"), "align label");
		linksPanel.add(culturalNormsField, "growx,wrap");
		linksPanel.add(new JLabel("Notes (IDs):"), "align label");
		linksPanel.add(notesField, "growx,wrap");
		linksPanel.add(new JLabel("Sources (IDs):"), "align label");
		linksPanel.add(sourcesField, "growx");
		tabbedPane.addTab("Links", linksPanel);

		// --- Modification tab ---
		JPanel modificationPanel = new JPanel(new BorderLayout());
		modificationPanel.setBorder(new TitledBorder("Modification History"));
		modificationPanel.add(GUIHelper.createScrollPane(modificationArea), BorderLayout.CENTER);
		tabbedPane.addTab("Modification", modificationPanel);

		// --- Conclusion tab ---
		JPanel conclusionPanel = new JPanel(new BorderLayout());
		conclusionPanel.setBorder(new TitledBorder("Conclusion"));
		conclusionPanel.add(GUIHelper.createScrollPane(conclusionArea), BorderLayout.CENTER);
		tabbedPane.addTab("Conclusion", conclusionPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);

		// --- Event listeners ---
		typeCombo.addItemListener(this::onTypeChanged);

		// Initially apply type-specific visibility
		onTypeChanged(null);
	}

	/**
	 * Populates the type combo with all possible event types from the specification.
	 */
	private void initTypeCombo(){
		List<String> types = new ArrayList<>();
		types.add("BIRTH");
		types.add("ADOPTION");

		String[] otherTypes = {
			"CHARACTERISTIC", "ANECDOTE", "DEATH", "CORONER_REPORT", "CREMATION", "BURIAL",
			"RESIDENCE", "EDUCATION", "GRADUATION", "OCCUPATION", "RETIREMENT",
			"MILITARY_INDUCTION", "MILITARY_MUSTER_ROLL", "MILITARY_SERVICE", "MILITARY_RANK",
			"MILITARY_AWARD", "MILITARY_RELEASE", "MILITARY_DISCHARGE", "MILITARY_RESIGNATION",
			"MILITARY_RETIREMENT", "PRISON", "PARDON", "MEMBERSHIP", "JURY_DUTY",
			"MEDICAL", "HOSPITALIZATION", "ILLNESS", "HONOR",
			"HOLOCAUST_DEPORTATION", "HOLOCAUST_ARRIVAL", "HOLOCAUST_LIBERATION", "HOLOCAUST_DEPARTURE",
			"EMANCIPATION", "BANKRUPTCY", "CASTE", "NATIONALITY", "EMIGRATION", "IMMIGRATION",
			"NATURALIZATION", "CENSUS", "SSN", "TITLE", "POSSESSION", "DEED", "ESCROW",
			"CHANCERY", "WILL", "PROBATE", "GUARDIANSHIP",
			"CHILDREN_COUNT", "MARRIAGES_COUNT", "RELIGION"
		};
		java.util.Arrays.sort(otherTypes);
		Collections.addAll(types, otherTypes);

		// Add all types to the combo model
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(types.toArray(new String[0]));
		typeCombo.setModel(model);
		typeCombo.setEditable(true);
	}

	/**
	 * Enables/disables fields based on the selected event type.
	 */
	private void onTypeChanged(ItemEvent e){
		String type = (String)typeCombo.getSelectedItem();
		boolean isBirth = "BIRTH".equals(type);
		boolean isAdoption = "ADOPTION".equals(type);

		familyField.setEnabled(isBirth || isAdoption);
		twinField.setEnabled(isBirth);
		relationshipParent1Combo.setEnabled(isAdoption);
		relationshipParent2Combo.setEnabled(isAdoption);

		if(isAdoption){
			familyField.setBackground(new Color(255, 255, 200));
		}
		else{
			familyField.setBackground(UIManager.getColor("TextField.background"));
		}
	}

	@Override
	protected void loadData(){
		// ---- Simple fields: load via binding manager ----
		bindingManager.load(record);

		// ---- Complex fields: manual load ----
		// Note: FAMILY, TWIN, PARENT1_RELATIONSHIP, PARENT2_RELATIONSHIP are not part of the
		// standard EVENT_RECORD protocol. They are kept here for backward compatibility
		// but should be migrated to RELATIONSHIP_RECORD or EVENT_PARTICIPATION_RECORD.
		familyField.setText(getChildValue("FAMILY"));
		twinField.setText(getChildValuesAsString("TWIN"));
		relationshipParent1Combo.setSelectedItem(getChildValue("PARENT1_RELATIONSHIP"));
		relationshipParent2Combo.setSelectedItem(getChildValue("PARENT2_RELATIONSHIP"));

		descriptionArea.setText(getChildValue("DESCRIPTION"));
		dateField.setText(getChildValue("DATE"));
		placeField.setText(getChildValue("PLACE"));

		FLEFRecord place = findChild("PLACE");
		if(place != null){
			placeQualifiers.load(place);
		}

		agencyField.setText(getChildValue("AGENCY"));
		causeField.setText(getChildValue("CAUSE"));

		FLEFRecord cause = findChild("CAUSE");
		if(cause != null){
			causeQualifiers.load(cause);
		}

		eventQualifiers.load(record);

		restrictionCheckBox.setSelected("confidential".equals(getChildValue("RESTRICTION")));

		culturalNormsField.setText(getChildValuesAsString("CULTURAL_NORM"));
		notesField.setText(getChildValuesAsString("NOTE"));
		sourcesField.setText(getChildValuesAsString("SOURCE"));
		conclusionArea.setText(getChildValue("CONCLUSION"));
		modificationArea.setText(getChildValue("MODIFICATION"));

		onTypeChanged(null);
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		// ---- Save simple fields via binding manager (must be after all other children) ----
		bindingManager.save(record);

		// ---- Complex fields: manual save ----
		// Note: FAMILY, TWIN, PARENT1_RELATIONSHIP, PARENT2_RELATIONSHIP are kept for backward compatibility
		addChild("FAMILY", familyField.getText().trim());
		addChild("TWIN", twinField.getText().trim());
		addChild("PARENT1_RELATIONSHIP", (String)relationshipParent1Combo.getSelectedItem());
		addChild("PARENT2_RELATIONSHIP", (String)relationshipParent2Combo.getSelectedItem());

		addChild("DESCRIPTION", descriptionArea.getText().trim());
		addChild("DATE", dateField.getText().trim());

		String placeVal = placeField.getText().trim();
		if(!placeVal.isEmpty()){
			FLEFRecord place = FLEFRecord.createChildWithValue("PLACE", placeVal);
			record.addChild(place);

			String pSourceType = placeQualifiers.getSourceType();
			if(pSourceType != null && !pSourceType.isEmpty()){
				addChild(place, "SOURCE_TYPE", pSourceType);
			}
			String pInformationType = placeQualifiers.getInformationType();
			if(pInformationType != null && !pInformationType.isEmpty()){
				addChild(place, "INFORMATION_TYPE", pInformationType);
			}
			String pEvidenceType = placeQualifiers.getEvidenceType();
			if(pEvidenceType != null && !pEvidenceType.isEmpty()){
				addChild(place, "EVIDENCE_TYPE", pEvidenceType);
			}
		}

		addChild("AGENCY", agencyField.getText().trim());

		String causeVal = causeField.getText().trim();
		if(!causeVal.isEmpty()){
			FLEFRecord cause = FLEFRecord.createChildWithValue("CAUSE", causeVal);
			record.addChild(cause);

			String cSourceType = placeQualifiers.getSourceType();
			if(cSourceType != null && !cSourceType.isEmpty()){
				addChild(cause, "SOURCE_TYPE", cSourceType);
			}
			String cInformationType = placeQualifiers.getInformationType();
			if(cInformationType != null && !cInformationType.isEmpty()){
				addChild(cause, "INFORMATION_TYPE", cInformationType);
			}
			String cEvidenceType = placeQualifiers.getEvidenceType();
			if(cEvidenceType != null && !cEvidenceType.isEmpty()){
				addChild(cause, "EVIDENCE_TYPE", cEvidenceType);
			}
		}

		String eSourceType = placeQualifiers.getSourceType();
		if(eSourceType != null && !eSourceType.isEmpty()){
			addChild("SOURCE_TYPE", eSourceType);
		}
		String eInformationType = placeQualifiers.getInformationType();
		if(eInformationType != null && !eInformationType.isEmpty()){
			addChild("INFORMATION_TYPE", eInformationType);
		}
		String eEvidenceType = placeQualifiers.getEvidenceType();
		if(eEvidenceType != null && !eEvidenceType.isEmpty()){
			addChild("EVIDENCE_TYPE", eEvidenceType);
		}

		addChild("RESTRICTION", restrictionCheckBox.isSelected()? "confidential": null);

		addChild("CULTURAL_NORM", culturalNormsField.getText().trim());
		addChild("NOTE", notesField.getText().trim());
		addChild("SOURCE", sourcesField.getText().trim());

		addChild("CONCLUSION", conclusionArea.getText().trim());
		addChild("MODIFICATION", modificationArea.getText().trim());
	}


	public static void main(String[] args){
		FLEFModel model = new FLEFModel();

		FLEFRecord event = FLEFRecord.createMainRecord("E1", "EVENT");
		FLEFRecord type = FLEFRecord.createChildWithValue("TYPE", "BIRTH");
		event.addChild(type);
		FLEFRecord family = FLEFRecord.createChildWithValue("FAMILY", "@F1@");
		event.addChild(family);
		FLEFRecord desc = FLEFRecord.createChildWithValue("DESCRIPTION", "Born at home");
		event.addChild(desc);
		model.addRecord(event);

		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Event Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(400, 200);
			frame.setLocationRelativeTo(null);

			JButton editBtn = new JButton("Edit Event E1");
			editBtn.addActionListener(e -> {
				FLEFRecord rec = model.getRecordById("E1");
				if(rec != null){
					_EventDialog dialog = new _EventDialog(null, model, rec);
					dialog.setVisible(true);
				}
			});

			JButton newBtn = new JButton("New Event");
			newBtn.addActionListener(e -> {
				_EventDialog dialog = new _EventDialog(null, model);
				dialog.setVisible(true);
			});

			JPanel panel = new JPanel();
			panel.add(editBtn);
			panel.add(newBtn);
			frame.add(panel);
			frame.setVisible(true);
		});
	}

}
