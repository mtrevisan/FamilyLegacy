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
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing an INDIVIDUAL_EVENT_RECORD.
 * <p>
 * Supports BIRTH, ADOPTION, and generic event types.
 */
public class EventDialog extends BaseRecordDialog{

	// ========== Basic tab components ==========
	private final JTextField idField = new JTextField(10);
	private final JComboBox<String> typeCombo = new JComboBox<>();
	private final JTextField familyField = new JTextField(15);
	private final JTextField twinField = new JTextField(30);
	private final JComboBox<String> pedigreePartner1Combo = new JComboBox<>(new String[]{"", "biological", "adopted", "foster", "guardian"});
	private final JComboBox<String> pedigreePartner2Combo = new JComboBox<>(new String[]{"", "biological", "adopted", "foster", "guardian"});

	// ========== Details tab components (EVENT_STRUCTURE) ==========
	private final JTextArea descriptionArea = new JTextArea(3, 30);
	private final JTextField dateField = new JTextField(20);
	private final JTextField placeField = new JTextField(15);
	private final JComboBox<String> placeCertaintyCombo = new JComboBox<>(new String[]{"", "challenged", "disproven", "proven"});
	private final JComboBox<String> placeCredibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});
	private final JTextField agencyField = new JTextField(20);
	private final JTextField causeField = new JTextField(20);
	private final JComboBox<String> causeCertaintyCombo = new JComboBox<>(new String[]{"", "challenged", "disproven", "proven"});
	private final JComboBox<String> causeCredibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});

	// ========== Assessments tab components ==========
	private final JComboBox<String> eventCertaintyCombo = new JComboBox<>(new String[]{"", "challenged", "disproven", "proven"});
	private final JComboBox<String> eventCredibilityCombo = new JComboBox<>(new String[]{"", "0", "1", "2", "3"});
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
		super(parent, model, record, "Edit Event");
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
		super(parent, model, "New Event");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(700, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		// Initialize the type combo with all possible event types
		initTypeCombo();

		// Create tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		JPanel basicPanel = new JPanel(new MigLayout("fill", "[right]rel[grow]"));
		idField.setEditable(false);
		idField.setText(record.getId());
		basicPanel.add(new JLabel("ID:"), "align label");
		basicPanel.add(idField, "grow,wrap");
		basicPanel.add(new JLabel("Type:"), "align label");
		basicPanel.add(typeCombo, "grow,wrap");
		basicPanel.add(new JLabel("Family (ID):"), "align label");
		basicPanel.add(familyField, "grow,wrap");
		basicPanel.add(new JLabel("Twins (IDs, comma separated):"), "align label");
		basicPanel.add(twinField, "grow,wrap");
		basicPanel.add(new JLabel("Pedigree Partner 1:"), "align label");
		basicPanel.add(pedigreePartner1Combo, "grow,wrap");
		basicPanel.add(new JLabel("Pedigree Partner 2:"), "align label");
		basicPanel.add(pedigreePartner2Combo, "grow");
		tabbedPane.addTab("Basic", basicPanel);

		// --- Details tab (EVENT_STRUCTURE) ---
		JPanel detailsPanel = new JPanel(new MigLayout("fill", "[right]rel[grow]"));
		detailsPanel.add(new JLabel("Description:"), "align label,top");
		JScrollPane descScroll = new JScrollPane(descriptionArea);
		descScroll.setPreferredSize(new Dimension(200, 80));
		detailsPanel.add(descScroll, "grow,wrap");

		detailsPanel.add(new JLabel("Date:"), "align label");
		detailsPanel.add(dateField, "grow,wrap");

		detailsPanel.add(new JLabel("Place (ID):"), "align label");
		detailsPanel.add(placeField, "grow,wrap");
		detailsPanel.add(new JLabel("  Place Certainty:"), "align label");
		detailsPanel.add(placeCertaintyCombo, "grow,wrap");
		detailsPanel.add(new JLabel("  Place Credibility:"), "align label");
		detailsPanel.add(placeCredibilityCombo, "grow,wrap");

		detailsPanel.add(new JLabel("Agency:"), "align label");
		detailsPanel.add(agencyField, "grow,wrap");

		detailsPanel.add(new JLabel("Cause:"), "align label");
		detailsPanel.add(causeField, "grow,wrap");
		detailsPanel.add(new JLabel("  Cause Certainty:"), "align label");
		detailsPanel.add(causeCertaintyCombo, "grow,wrap");
		detailsPanel.add(new JLabel("  Cause Credibility:"), "align label");
		detailsPanel.add(causeCredibilityCombo, "grow");
		tabbedPane.addTab("Details", detailsPanel);

		// --- Assessments tab ---
		JPanel assessmentsPanel = new JPanel(new MigLayout("fill", "[right]rel[grow]"));
		assessmentsPanel.add(new JLabel("Event Certainty:"), "align label");
		assessmentsPanel.add(eventCertaintyCombo, "grow,wrap");
		assessmentsPanel.add(new JLabel("Event Credibility:"), "align label");
		assessmentsPanel.add(eventCredibilityCombo, "grow,wrap");
		assessmentsPanel.add(restrictionCheckBox, "span 2,wrap");
		tabbedPane.addTab("Assessments", assessmentsPanel);

		// --- Links tab ---
		JPanel linksPanel = new JPanel(new MigLayout("fill", "[right]rel[grow]"));
		linksPanel.add(new JLabel("Cultural Norms (IDs):"), "align label");
		linksPanel.add(culturalNormsField, "grow,wrap");
		linksPanel.add(new JLabel("Notes (IDs):"), "align label");
		linksPanel.add(notesField, "grow,wrap");
		linksPanel.add(new JLabel("Sources (IDs):"), "align label");
		linksPanel.add(sourcesField, "grow");
		tabbedPane.addTab("Links", linksPanel);

		// --- Conclusion tab ---
		JPanel conclusionPanel = new JPanel(new BorderLayout());
		conclusionPanel.setBorder(new TitledBorder("Conclusion"));
		conclusionPanel.add(new JScrollPane(conclusionArea), BorderLayout.CENTER);
		tabbedPane.addTab("Conclusion", conclusionPanel);

		// --- Modification tab ---
		JPanel modificationPanel = new JPanel(new BorderLayout());
		modificationPanel.setBorder(new TitledBorder("Modification History"));
		modificationPanel.add(new JScrollPane(modificationArea), BorderLayout.CENTER);
		tabbedPane.addTab("Modification", modificationPanel);

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
		for(String t : otherTypes){
			types.add(t);
		}

		for(String t : types){
			typeCombo.addItem(t);
		}
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
		pedigreePartner1Combo.setEnabled(isAdoption);
		pedigreePartner2Combo.setEnabled(isAdoption);

		if(isAdoption){
			familyField.setBackground(new Color(255, 255, 200));
		}
		else{
			familyField.setBackground(UIManager.getColor("TextField.background"));
		}
	}

	@Override
	protected void loadData(){
		String type = getChildValue("TYPE");
		if(type != null) typeCombo.setSelectedItem(type);

		familyField.setText(getChildValue("FAMILY"));
		twinField.setText(getChildValuesAsString("TWIN"));
		pedigreePartner1Combo.setSelectedItem(getChildValue("PEDIGREE_PARTNER1"));
		pedigreePartner2Combo.setSelectedItem(getChildValue("PEDIGREE_PARTNER2"));

		descriptionArea.setText(getChildValue("DESCRIPTION"));
		dateField.setText(getChildValue("DATE"));
		placeField.setText(getChildValue("PLACE"));

		FLEFRecord place = findChild("PLACE");
		if(place != null){
			placeCertaintyCombo.setSelectedItem(getChildValue(place, "CERTAINTY"));
			placeCredibilityCombo.setSelectedItem(getChildValue(place, "CREDIBILITY"));
		}

		agencyField.setText(getChildValue("AGENCY"));
		causeField.setText(getChildValue("CAUSE"));

		FLEFRecord cause = findChild("CAUSE");
		if(cause != null){
			causeCertaintyCombo.setSelectedItem(getChildValue(cause, "CERTAINTY"));
			causeCredibilityCombo.setSelectedItem(getChildValue(cause, "CREDIBILITY"));
		}

		eventCertaintyCombo.setSelectedItem(getChildValue("CERTAINTY"));
		eventCredibilityCombo.setSelectedItem(getChildValue("CREDIBILITY"));
		restrictionCheckBox.setSelected("confidential".equals(getChildValue("RESTRICTION")));

		culturalNormsField.setText(getChildValuesAsString("CULTURAL_NORM"));
		notesField.setText(getChildValuesAsString("NOTE"));
		sourcesField.setText(getChildValuesAsString("SOURCE_CITATION"));
		conclusionArea.setText(getChildValue("CONCLUSION"));
		modificationArea.setText(getChildValue("MODIFICATION"));

		onTypeChanged(null);
	}

	@Override
	protected void saveRecord(){
		String type = (String)typeCombo.getSelectedItem();
		if(type == null || type.isEmpty()){
			showError("Validation Error", "Type is required.");
			return;
		}

		if("ADOPTION".equals(type) && familyField.getText().trim().isEmpty()){
			showError("Validation Error", "Family is mandatory for ADOPTION.");
			return;
		}

		record.getChildren().clear();

		addChild("TYPE", 1, type);
		addChild("FAMILY", 1, familyField.getText().trim());
		addChildrenFromString("TWIN", twinField.getText().trim());
		addChild("PEDIGREE_PARTNER1", 1, (String)pedigreePartner1Combo.getSelectedItem());
		addChild("PEDIGREE_PARTNER2", 1, (String)pedigreePartner2Combo.getSelectedItem());

		addChild("DESCRIPTION", 1, descriptionArea.getText().trim());
		addChild("DATE", 1, dateField.getText().trim());

		String placeVal = placeField.getText().trim();
		if(!placeVal.isEmpty()){
			FLEFRecord place = new FLEFRecord();
			place.setLevel(1);
			place.setTag("PLACE");
			place.setValue(placeVal);
			record.addChild(place);
			addChild(place, "CERTAINTY", 2, (String)placeCertaintyCombo.getSelectedItem());
			addChild(place, "CREDIBILITY", 2, (String)placeCredibilityCombo.getSelectedItem());
		}

		addChild("AGENCY", 1, agencyField.getText().trim());

		String causeVal = causeField.getText().trim();
		if(!causeVal.isEmpty()){
			FLEFRecord cause = new FLEFRecord();
			cause.setLevel(1);
			cause.setTag("CAUSE");
			cause.setValue(causeVal);
			record.addChild(cause);
			addChild(cause, "CERTAINTY", 2, (String)causeCertaintyCombo.getSelectedItem());
			addChild(cause, "CREDIBILITY", 2, (String)causeCredibilityCombo.getSelectedItem());
		}

		addChild("CERTAINTY", 1, (String)eventCertaintyCombo.getSelectedItem());
		addChild("CREDIBILITY", 1, (String)eventCredibilityCombo.getSelectedItem());
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
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("EVENT");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "EVENT", "E");
	}

	// ==================== Main for testing ====================

	public static void main(String[] args){
		FLEFModel model = new FLEFModel();

		FLEFRecord event = new FLEFRecord();
		event.setId("E1");
		event.setType("EVENT");
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
