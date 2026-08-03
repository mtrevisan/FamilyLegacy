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
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Dialog for editing a {@code GROUP_ATTRIBUTE_RECORD} according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * GROUP_ATTRIBUTE_RECORD :=
 * n @<XREF:GROUP_ATTRIBUTE>@ GROUP_ATTRIBUTE    {1:1}
 *   +1 GROUP @<XREF:GROUP>@    {1:1}
 *   +1 TYPE [ RESIDENCE | CHILDREN_COUNT | SOCIAL_CLASS | <ATTRIBUTE_TYPE> ]    {1:1}
 *   +1 VALUE <TEXT>    {0:1}
 *   +1 <<DATE_STRUCTURE>>    {0:1}
 *   +1 VALID_FROM    {0:1}
 *     +2 <<DATE_STRUCTURE>>    {1:1}
 *   +1 VALID_TO    {0:1}
 *     +2 <<DATE_STRUCTURE>>    {1:1}
 *   +1 <<PLACE_STRUCTURE>>    {0:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<CONCLUSION_STRUCTURE>>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 * <p>
 * This dialog provides a user interface for creating and editing group attributes.
 */
public class GroupAttributeDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212973L;


	private final GroupHandler groupHandler = new GroupHandler();
	private final SourceHandler sourceHandler = new SourceHandler();
	private final NoteHandler noteHandler = new NoteHandler();
	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();

	// UI components
	private final JTextField groupDisplayField = new JTextField(20);
	private final JButton browseGroupBtn = new JButton("Browse...");
	private final JButton editGroupBtn = new JButton("Edit");
	private final JButton clearGroupBtn = new JButton("Clear");
	private String selectedGroupId;

	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "RESIDENCE", "CHILDREN_COUNT", "SOCIAL_CLASS"});

	// VALUE - multi-line text area with scroll
	private final JTextArea valueArea = new JTextArea(3, 30);
	private final JScrollPane valueScrollPane = GUIHelper.createScrollPane(valueArea);

	// DATE_STRUCTURE (simplified to a single text field for now)
	private final JTextField dateField = new JTextField(15);

	// VALID_FROM (simplified)
	private final JTextField validFromField = new JTextField(15);

	// VALID_TO (simplified)
	private final JTextField validToField = new JTextField(15);

	// PLACE_STRUCTURE (simplified to a text field for now)
	private final JTextField placeField = new JTextField(20);
	private final JButton browsePlaceBtn = new JButton("Browse...");
	private final JButton clearPlaceBtn = new JButton("Clear");
	private String selectedPlaceId;

	// Panels
	private RestrictionPanel restrictionPanel;
	private ConclusionPanel conclusionPanel;
	private ModificationPanel modificationPanel;

	// Source Citations
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// Evidence Qualifiers (simplified to a combo)
	private final JComboBox<String> certaintyCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "proven", "challenged", "disproven"});
	private final JComboBox<String> credibilityCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "0", "1", "2", "3"});

	// ----- Factory methods -----
	public static GroupAttributeDialog createNew(Dialog parent, FLEFModel model){
		return new GroupAttributeDialog(parent, model, null);
	}

	public static GroupAttributeDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");
		return new GroupAttributeDialog(parent, model, record);
	}

	// ----- Constructor -----
	private GroupAttributeDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupAttributeHandler.TYPE));

		initComponents();
		loadData();
		pack();
		setLocationRelativeTo(parent);
	}

	// ----- Initialisation -----
	protected void initComponents(){
		modificationPanel = new ModificationPanel(this, model);
		restrictionPanel = new RestrictionPanel("RESTRICTION", this);
		conclusionPanel = new ConclusionPanel("CONCLUSION", this, model);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		setLayout(new MigLayout("fillx"));
		add(tabbedPane, "growx,push");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[right]rel[grow]", "[]5[]5[]10[]5[]5[]5[]"));

		// GROUP
		JPanel groupPanel = new JPanel(new BorderLayout(5, 5));
		groupDisplayField.setEditable(false);
		groupDisplayField.setBackground(UIManager.getColor("TextField.background"));
		groupPanel.add(groupDisplayField, BorderLayout.CENTER);

		JPanel groupBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		groupBtnPanel.add(browseGroupBtn);
		groupBtnPanel.add(editGroupBtn);
		groupBtnPanel.add(clearGroupBtn);
		groupPanel.add(groupBtnPanel, BorderLayout.EAST);

		panel.add(new JLabel("Group:"), "align label");
		panel.add(groupPanel, "growx,wrap");

		browseGroupBtn.addActionListener(e -> browseGroup());
		editGroupBtn.addActionListener(e -> editGroup());
		clearGroupBtn.addActionListener(e -> clearGroup());
		editGroupBtn.setEnabled(false);
		clearGroupBtn.setEnabled(false);

		// TYPE
		panel.add(new JLabel("Type:"), "align label");
		typeCombo.setEditable(true);
		panel.add(typeCombo, "growx,wrap");

		// VALUE
		panel.add(new JLabel("Value:"), "align label,top");
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		panel.add(valueScrollPane, "growx,wrap");

		// DATE_STRUCTURE
		panel.add(new JLabel("Date:"), "align label");
		panel.add(dateField, "growx,wrap");

		// VALID_FROM
		panel.add(new JLabel("Valid From:"), "align label");
		panel.add(validFromField, "growx,wrap");

		// VALID_TO
		panel.add(new JLabel("Valid To:"), "align label");
		panel.add(validToField, "growx,wrap");

		// PLACE_STRUCTURE
		JPanel placePanel = new JPanel(new BorderLayout(5, 5));
		placeField.setEditable(false);
		placeField.setBackground(UIManager.getColor("TextField.background"));
		placePanel.add(placeField, BorderLayout.CENTER);

		JPanel placeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		placeBtnPanel.add(browsePlaceBtn);
		placeBtnPanel.add(clearPlaceBtn);
		placePanel.add(placeBtnPanel, BorderLayout.EAST);

		panel.add(new JLabel("Place:"), "align label");
		panel.add(placePanel, "growx,wrap");

		browsePlaceBtn.addActionListener(e -> browsePlace());
		clearPlaceBtn.addActionListener(e -> clearPlace());
		clearPlaceBtn.setEnabled(false);

		return panel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]5[]"));
		panel.add(createSourceCitationPanel(), "growx");
		panel.add(createEvidenceQualifiersPanel(), "growx");
		return panel;
	}

	private JPanel createSourceCitationPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Source Citations"));
		sourceCitationList.setVisibleRowCount(4);
		sourceCitationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(sourceCitationList,
			this::editSourceCitation,
			this::createNewSource,
			this::deleteSourceCitation,
			builder -> {
				builder.item("Create New...", this::createNewSource);
				builder.item("Add Existing...", this::addSourceCitation);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editSourceCitation);
				builder.selectionSensitiveItem("Clear", this::deleteSourceCitation);
			}
		);

		JScrollPane scrollPane = GUIHelper.createScrollPane(sourceCitationList);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	private JPanel createEvidenceQualifiersPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx,ins 5", "[right]rel[grow]", "[]5[]"));
		panel.setBorder(new TitledBorder("Evidence Qualifiers"));

		// CERTAINTY
		panel.add(new JLabel("Certainty:"), "align label");
		panel.add(certaintyCombo, "growx,wrap");

		// CREDIBILITY
		panel.add(new JLabel("Credibility:"), "align label");
		panel.add(credibilityCombo, "growx,wrap");

		return panel;
	}


	private void browseGroup(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, groupHandler, selectedId -> {
			if(selectedId != null){
				selectGroup(selectedId);
			}
		});
		dialog.setVisible(true);
	}

	private void editGroup(){
		if(selectedGroupId == null || selectedGroupId.isEmpty()){
			JOptionPane.showMessageDialog(this, "No group selected to edit.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		FLEFRecord group = model.getRecordById(selectedGroupId);
		if(group == null){
			JOptionPane.showMessageDialog(this, "Group record not found: " + selectedGroupId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JDialog dialog = groupHandler.createEditDialog(this, model, group);
		dialog.setVisible(true);

		// Refresh display (name may have changed)
		updateGroupDisplay(selectedGroupId);
	}

	private void clearGroup(){
		selectedGroupId = null;
		groupDisplayField.setText(StringUtils.EMPTY);
		editGroupBtn.setEnabled(false);
		clearGroupBtn.setEnabled(false);
	}

	private void selectGroup(String groupId){
		selectedGroupId = groupId;
		updateGroupDisplay(groupId);
		editGroupBtn.setEnabled(true);
		clearGroupBtn.setEnabled(true);
	}

	private void updateGroupDisplay(String groupId){
		FLEFRecord group = model.getRecordById(groupId);
		if(group != null){
			groupDisplayField.setText(groupHandler.getDisplayText(group, model));
		}
		else{
			groupDisplayField.setText(groupId);
		}
	}


	private void browsePlace(){
		// TODO: Implement Place selection when PlaceDialog is available
		JOptionPane.showMessageDialog(this,
			"Place selection is not yet implemented.\nPlace ID can be entered manually for now.",
			"Info", JOptionPane.INFORMATION_MESSAGE);

		String placeId = JOptionPane.showInputDialog(this, "Enter Place ID (e.g., @P123@):");
		if(!StringUtils.isEmpty(placeId)){
			selectedPlaceId = placeId.trim();
			placeField.setText(selectedPlaceId);
			clearPlaceBtn.setEnabled(true);
		}
	}

	private void clearPlace(){
		selectedPlaceId = null;
		placeField.setText(StringUtils.EMPTY);
		clearPlaceBtn.setEnabled(false);
	}


	private void addSourceCitation(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, sourceHandler, selectedId -> {
			if(selectedId != null){
				FLEFRecord citation = FLEFRecord.createChildWithValue("SOURCE", selectedId);
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		dialog.setVisible(true);
	}

	private void editSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord existing = sourceCitationRecords.get(idx);
		SourceCitationDialog dialog = SourceCitationDialog.createEdit(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getRecord();
			if(updated != null){
				sourceCitationRecords.set(idx, updated);
				sourceCitationListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void deleteSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1) return;

		if(!showConfirm("Confirm", "Remove this source citation?")) return;

		sourceCitationRecords.remove(idx);
		sourceCitationListModel.remove(idx);
	}

	private void createNewSource(){
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}

		JDialog dialog = sourceHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				FLEFRecord citation = FLEFRecord.createChildWithValue("SOURCE", id);
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
				return;
			}
		}
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null){
				return sourceHandler.getDisplayText(rec, model);
			}
			return sourceId;
		}
		return "[empty]";
	}

	@Override
	protected void loadData(){
		// GROUP
		String groupId = FLEFRecordHelper.getChildValue(record, "GROUP");
		if(groupId != null && !groupId.isEmpty()){
			selectGroup(groupId);
		}

		// TYPE
		String type = FLEFRecordHelper.getChildValue(record, "TYPE");
		typeCombo.setSelectedItem(StringUtils.defaultString(type));

		// VALUE
		String value = FLEFRecordHelper.getChildValue(record, "VALUE");
		valueArea.setText(StringUtils.defaultString(value));

		// DATE_STRUCTURE (simplified)
		FLEFRecord dateStruct = FLEFRecordHelper.findChild(record, "DATE_STRUCTURE");
		if(dateStruct != null){
			FLEFRecord dateValue = FLEFRecordHelper.findChild(dateStruct, "DATE_VALUE");
			if(dateValue != null){
				// For simplicity, we read the first child's value
				FLEFRecord qualified = FLEFRecordHelper.findChild(dateValue, "QUALIFIED_DATE");
				if(qualified != null){
					FLEFRecord single = FLEFRecordHelper.findChild(qualified, "SINGLE_DATE");
					if(single != null){
						FLEFRecord fullDate = FLEFRecordHelper.findChild(single, "FULL_DATE");
						if(fullDate != null){
							dateField.setText(fullDate.getValue());
						}
					}
				}
			}
		}

		// VALID_FROM
		FLEFRecord validFrom = FLEFRecordHelper.findChild(record, "VALID_FROM");
		if(validFrom != null){
			FLEFRecord dateStructFrom = FLEFRecordHelper.findChild(validFrom, "DATE_STRUCTURE");
			if(dateStructFrom != null){
				String fromDate = FLEFRecordHelper.getChildValue(dateStructFrom, "DATE");
				validFromField.setText(StringUtils.defaultString(fromDate));
			}
		}

		// VALID_TO
		FLEFRecord validTo = FLEFRecordHelper.findChild(record, "VALID_TO");
		if(validTo != null){
			FLEFRecord dateStructTo = FLEFRecordHelper.findChild(validTo, "DATE_STRUCTURE");
			if(dateStructTo != null){
				String toDate = FLEFRecordHelper.getChildValue(dateStructTo, "DATE");
				validToField.setText(StringUtils.defaultString(toDate));
			}
		}

		// PLACE_STRUCTURE
		FLEFRecord placeStruct = FLEFRecordHelper.findChild(record, "PLACE_STRUCTURE");
		if(placeStruct != null){
			selectedPlaceId = placeStruct.getValue();
			placeField.setText(StringUtils.defaultString(selectedPlaceId));
			clearPlaceBtn.setEnabled(selectedPlaceId != null);
		}

		// SOURCE_CITATION
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// EVIDENCE_QUALIFIERS
		FLEFRecord evidence = FLEFRecordHelper.findChild(record, "EVIDENCE_QUALIFIERS");
		if(evidence != null){
			String certainty = FLEFRecordHelper.getChildValue(evidence, "CERTAINTY");
			certaintyCombo.setSelectedItem(StringUtils.defaultString(certainty));

			String credibility = FLEFRecordHelper.getChildValue(evidence, "CREDIBILITY");
			credibilityCombo.setSelectedItem(StringUtils.defaultString(credibility));
		}

		// RESTRICTION_STRUCTURE
		restrictionPanel.load(record);

		// CONCLUSION_STRUCTURE
		conclusionPanel.load(record);

		// MODIFICATION_STRUCTURE
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// GROUP is required
		if(selectedGroupId == null || selectedGroupId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Group is required.\nPlease select a group.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		// TYPE is required
		String type = (String)typeCombo.getSelectedItem();
		if(type == null || type.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Type is required.\nPlease select an attribute type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		// VALUE is required for some types (optional for others)
		// For now, we allow empty value

		// Validate dates format
		String date = dateField.getText().trim();
		if(!date.isEmpty() && !date.matches("\\d{4}-\\d{2}-\\d{2}")){
			JOptionPane.showMessageDialog(this,
				"Date must be in ISO 8601 format (YYYY-MM-DD).",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		String validFrom = validFromField.getText().trim();
		if(!validFrom.isEmpty() && !validFrom.matches("\\d{4}-\\d{2}-\\d{2}")){
			JOptionPane.showMessageDialog(this,
				"Valid From must be in ISO 8601 format (YYYY-MM-DD).",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		String validTo = validToField.getText().trim();
		if(!validTo.isEmpty() && !validTo.matches("\\d{4}-\\d{2}-\\d{2}")){
			JOptionPane.showMessageDialog(this,
				"Valid To must be in ISO 8601 format (YYYY-MM-DD).",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(restrictionPanel.hasData() && !restrictionPanel.validateData())
			return false;

		return (!conclusionPanel.hasData() || conclusionPanel.validateData());
	}

	@Override
	protected void saveData(){
		// GROUP
		FLEFRecordHelper.updateChildValue(record, "GROUP", selectedGroupId);

		// TYPE
		String type = (String)typeCombo.getSelectedItem();
		FLEFRecordHelper.updateChildValue(record, "TYPE", type);

		// VALUE
		String value = valueArea.getText().trim();
		FLEFRecordHelper.updateChildValue(record, "VALUE", value);

		// DATE_STRUCTURE (simplified)
		String date = dateField.getText().trim();
		if(!date.isEmpty()){
			FLEFRecord dateStruct = FLEFRecord.createChild("DATE_STRUCTURE");

			FLEFRecord dateValue = FLEFRecord.createChild("DATE_VALUE");
			FLEFRecord valueNode = FLEFRecord.createChild("VALUE");
			dateValue.addChild(valueNode);

			FLEFRecord qualifiedDate = FLEFRecord.createChild("QUALIFIED_DATE");
			valueNode.addChild(qualifiedDate);

			FLEFRecord singleDate = FLEFRecord.createChild("SINGLE_DATE");
			qualifiedDate.addChild(singleDate);

			FLEFRecord fullDate = FLEFRecord.createChildWithValue("FULL_DATE", date);
			singleDate.addChild(fullDate);

			FLEFRecord calendar = FLEFRecord.createChildWithValue("CALENDAR", "gregorian");
			fullDate.addChild(calendar);

			dateStruct.addChild(dateValue);
			record.addChild(dateStruct);
		}

		// VALID_FROM (simplified)
		String validFrom = validFromField.getText().trim();
		if(!validFrom.isEmpty()){
			FLEFRecord validFromStruct = FLEFRecord.createChild("VALID_FROM");
			FLEFRecord dateStruct = FLEFRecord.createChild("DATE_STRUCTURE");
			FLEFRecord dateNode = FLEFRecord.createChildWithValue("DATE", validFrom);
			dateStruct.addChild(dateNode);
			validFromStruct.addChild(dateStruct);
			record.addChild(validFromStruct);
		}

		// VALID_TO (simplified)
		String validTo = validToField.getText().trim();
		if(!validTo.isEmpty()){
			FLEFRecord validToStruct = FLEFRecord.createChild("VALID_TO");
			FLEFRecord dateStruct = FLEFRecord.createChild("DATE_STRUCTURE");
			FLEFRecord dateNode = FLEFRecord.createChildWithValue("DATE", validTo);
			dateStruct.addChild(dateNode);
			validToStruct.addChild(dateStruct);
			record.addChild(validToStruct);
		}

		// PLACE_STRUCTURE (simplified)
		if(selectedPlaceId != null && !selectedPlaceId.isEmpty()){
			FLEFRecord placeStruct = FLEFRecord.createChildWithValue("PLACE_STRUCTURE", selectedPlaceId);
			record.addChild(placeStruct);
		}

		// SOURCE_CITATION
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setTag("SOURCE");
			record.addChild(citation);
		}

		// EVIDENCE_QUALIFIERS
		String certainty = (String)certaintyCombo.getSelectedItem();
		String credibility = (String)credibilityCombo.getSelectedItem();

		if((certainty != null && !certainty.isEmpty()) || (credibility != null && !credibility.isEmpty())){
			FLEFRecordHelper.updateChildValue(record, "CERTAINTY", certainty);
			FLEFRecordHelper.updateChildValue(record, "CREDIBILITY", credibility);
		}

		// RESTRICTION_STRUCTURE
		if(restrictionPanel.hasData())
			restrictionPanel.save(record);

		// CONCLUSION_STRUCTURE
		if(conclusionPanel.hasData()){
			FLEFRecord conclusion = conclusionPanel.save(null);
			if(conclusion != null){
				conclusion.setTag("CONCLUSION");
				record.addChild(conclusion);
			}
		}

		// MODIFICATION_STRUCTURE
		modificationPanel.save(record);
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Group Attribute Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Group Attribute");
			btn.addActionListener(e -> {
				GroupAttributeDialog dialog = GroupAttributeDialog.createNew(null, model);
				dialog.setVisible(true);

				System.out.println("Group Attribute saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
