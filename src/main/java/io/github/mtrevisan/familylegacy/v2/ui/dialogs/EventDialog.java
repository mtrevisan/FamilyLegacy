package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Dialog for editing an INDIVIDUAL_EVENT_RECORD.
 * <p>
 * Supports BIRTH, ADOPTION, and generic event types.
 */
public class EventDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -9191829528682252778L;

	private final BindingManager bindingManager = new BindingManager();

	// ========== Basic tab components ==========
	private final JTextField idField = new JTextField(10);
	private final BoundComboBox typeCombo;
	private final JTextField familyField = new JTextField(15);
	private final JTextField twinField = new JTextField(30);
	private final JComboBox<String> relationshipParent1Combo = new JComboBox<>(new String[]{"", "biological", "adopted", "foster", "guardian"});
	private final JComboBox<String> relationshipParent2Combo = new JComboBox<>(new String[]{"", "biological", "adopted", "foster", "guardian"});

	// ========== Details tab components (EVENT_STRUCTURE) ==========
	private final JTextArea descriptionArea = new JTextArea(3, 30);
	private final JTextField dateField = new JTextField(20);
	private final JTextField placeField = new JTextField(15);
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("Place Evidence");
	private final JTextField agencyField = new JTextField(20);
	private final JTextField causeField = new JTextField(20);
	private final EvidenceQualifiersPanel causeQualifiers = new EvidenceQualifiersPanel("Cause Evidence");

	// ========== Assessments tab components ==========
	private final EvidenceQualifiersPanel eventQualifiers = new EvidenceQualifiersPanel("Event Evidence");
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// ========== Links tab components ==========
	private final JTextField culturalNormsField = new JTextField(30);
	private final JTextField notesField = new JTextField(30);
	private final JTextField sourcesField = new JTextField(30);

	// ========== Conclusion tab components ==========
	private final JTextArea conclusionArea = new JTextArea(5, 30);

	// ========== Modification tab components ==========
	private final JTextArea modificationArea = new JTextArea(5, 30);

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	/**
	 * Creates a dialog to edit an existing event record.
	 */
	public EventDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Event", model, record);

		// Initialize bound components before using them
		typeCombo = new BoundComboBox("TYPE", new String[]{});
		initTypeCombo();

		initComponents();
		loadData();
		setMinimumSize(new Dimension(700, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * Creates a dialog to create a new event record.
	 */
	public EventDialog(Frame parent, FLEFModel model){
		super(parent, "New Event", model, null);

		// Initialize bound components before using them
		typeCombo = new BoundComboBox("TYPE", new String[]{});
		initTypeCombo();

		initComponents();
		loadData();
		setMinimumSize(new Dimension(700, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		// Register bound components
		bindingManager.bind(typeCombo);

		// Create tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		JPanel basicPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		idField.setEditable(false);
		idField.setText(record != null? record.getId(): "");
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
		JScrollPane descScroll = new JScrollPane(descriptionArea);
		descScroll.setPreferredSize(new Dimension(200, 80));
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
		modificationPanel.add(new JScrollPane(modificationArea), BorderLayout.CENTER);
		tabbedPane.addTab("Modification", modificationPanel);

		// --- Conclusion tab ---
		JPanel conclusionPanel = new JPanel(new BorderLayout());
		conclusionPanel.setBorder(new TitledBorder("Conclusion"));
		conclusionPanel.add(new JScrollPane(conclusionArea), BorderLayout.CENTER);
		tabbedPane.addTab("Conclusion", conclusionPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		// --- Event listeners ---
		typeCombo.addItemListener(this::onTypeChanged);
		saveButton.addActionListener(e -> saveRecord());
		cancelButton.addActionListener(e -> dispose());

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
		bindingManager.loadFromRecord(record);

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
			String placeCert = getChildValue(place, "CERTAINTY");
			String placeCred = getChildValue(place, "CREDIBILITY");
			placeQualifiers.load(placeCert, placeCred);
		}

		agencyField.setText(getChildValue("AGENCY"));
		causeField.setText(getChildValue("CAUSE"));

		FLEFRecord cause = findChild("CAUSE");
		if(cause != null){
			String causeCert = getChildValue(cause, "CERTAINTY");
			String causeCred = getChildValue(cause, "CREDIBILITY");
			causeQualifiers.load(causeCert, causeCred);
		}

		String eventCert = getChildValue("CERTAINTY");
		String eventCred = getChildValue("CREDIBILITY");
		eventQualifiers.load(eventCert, eventCred);

		restrictionCheckBox.setSelected("confidential".equals(getChildValue("RESTRICTION")));

		culturalNormsField.setText(getChildValuesAsString("CULTURAL_NORM"));
		notesField.setText(getChildValuesAsString("NOTE"));
		sourcesField.setText(getChildValuesAsString("SOURCE_CITATION"));
		conclusionArea.setText(getChildValue("CONCLUSION"));
		modificationArea.setText(getChildValue("MODIFICATION"));

		onTypeChanged(null);
	}

	@Override
	protected boolean validateData(){
		return false;
	}

	@Override
	protected void saveRecord(){
		// ---- Save simple fields via binding manager (must be after all other children) ----
		bindingManager.saveToRecord(record);

		// ---- Complex fields: manual save ----
		// Note: FAMILY, TWIN, PARENT1_RELATIONSHIP, PARENT2_RELATIONSHIP are kept for backward compatibility
		addChild("FAMILY", 1, familyField.getText().trim());
		addChildrenFromString("TWIN", twinField.getText().trim());
		addChild("PARENT1_RELATIONSHIP", 1, (String)relationshipParent1Combo.getSelectedItem());
		addChild("PARENT2_RELATIONSHIP", 1, (String)relationshipParent2Combo.getSelectedItem());

		addChild("DESCRIPTION", 1, descriptionArea.getText().trim());
		addChild("DATE", 1, dateField.getText().trim());

		String placeVal = placeField.getText().trim();
		if(!placeVal.isEmpty()){
			FLEFRecord place = new FLEFRecord();
			place.setLevel(1);
			place.setTag("PLACE");
			place.setValue(placeVal);
			record.addChild(place);

			String pCert = placeQualifiers.getCertainty();
			if(pCert != null && !pCert.isEmpty()){
				addChild(place, "CERTAINTY", 2, pCert);
			}
			String pCred = placeQualifiers.getCredibility();
			if(pCred != null && !pCred.isEmpty()){
				addChild(place, "CREDIBILITY", 2, pCred);
			}
		}

		addChild("AGENCY", 1, agencyField.getText().trim());

		String causeVal = causeField.getText().trim();
		if(!causeVal.isEmpty()){
			FLEFRecord cause = new FLEFRecord();
			cause.setLevel(1);
			cause.setTag("CAUSE");
			cause.setValue(causeVal);
			record.addChild(cause);

			String cCert = causeQualifiers.getCertainty();
			if(cCert != null && !cCert.isEmpty()){
				addChild(cause, "CERTAINTY", 2, cCert);
			}
			String cCred = causeQualifiers.getCredibility();
			if(cCred != null && !cCred.isEmpty()){
				addChild(cause, "CREDIBILITY", 2, cCred);
			}
		}

		String eCert = eventQualifiers.getCertainty();
		if(eCert != null && !eCert.isEmpty()){
			addChild("CERTAINTY", 1, eCert);
		}
		String eCred = eventQualifiers.getCredibility();
		if(eCred != null && !eCred.isEmpty()){
			addChild("CREDIBILITY", 1, eCred);
		}

		addChild("RESTRICTION", 1, restrictionCheckBox.isSelected()? "confidential": null);

		addChildrenFromString("CULTURAL_NORM", culturalNormsField.getText().trim());
		addChildrenFromString("NOTE", notesField.getText().trim());
		addChildrenFromString("SOURCE_CITATION", sourcesField.getText().trim());

		addChild("CONCLUSION", 1, conclusionArea.getText().trim());
		addChild("MODIFICATION", 1, modificationArea.getText().trim());

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), "EVENT");
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "EVENT", "E");
	}


	public static void main(String[] args){
		FLEFModel model = new FLEFModel();

		FLEFRecord event = FLEFRecord.createMainRecord("E1", "EVENT");
		FLEFRecord type = new FLEFRecord();
		type.setLevel(1);
		type.setTag("TYPE");
		type.setValue("BIRTH");
		event.addChild(type);
		FLEFRecord family = new FLEFRecord();
		family.setLevel(1);
		family.setTag("FAMILY");
		family.setValue("F1");
		event.addChild(family);
		FLEFRecord desc = new FLEFRecord();
		desc.setLevel(1);
		desc.setTag("DESCRIPTION");
		desc.setValue("Born at home");
		event.addChild(desc);
		model.addRecord(event);

		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){
		}

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Event Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(400, 200);
			frame.setLocationRelativeTo(null);

			JButton editBtn = new JButton("Edit Event E1");
			editBtn.addActionListener(e -> {
				FLEFRecord rec = model.getRecordById("E1");
				if(rec != null){
					EventDialog dialog = new EventDialog(frame, model, rec);
					dialog.setVisible(true);
				}
			});

			JButton newBtn = new JButton("New Event");
			newBtn.addActionListener(e -> {
				EventDialog dialog = new EventDialog(frame, model);
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
