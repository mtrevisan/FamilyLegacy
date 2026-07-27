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

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.EventStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Dialog for editing an INDIVIDUAL_EVENT_RECORD according to FLEF 0.0.9.
 * <p>
 * Supports three variants:
 * <ul>
 *   <li>BIRTH: with optional FAMILY and multiple TWIN references</li>
 *   <li>ADOPTION: with mandatory FAMILY and optional PARENT1_RELATIONSHIP/PARENT2</li>
 *   <li>Generic: all other event types with just EVENT_STRUCTURE</li>
 * </ul>
 */
public class IndividualEventDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -8707235728967288527L;


	static{
		HandlerRegistry.register(new IndividualEventHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new CalendarHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new GroupHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JComboBox<String> typeCombo = new JComboBox<>();

	// ========== BIRTH fields ==========
	private final JTextField familyDisplayField = new JTextField(20);
	private final JButton familyBrowseBtn = new JButton("Browse...");
	private final JButton familyClearBtn = new JButton("Clear");
	private String selectedFamilyId;

	private final DefaultListModel<String> twinListModel = new DefaultListModel<>();
	private final JList<String> twinList = new JList<>(twinListModel);
	private final List<String> twinIds = new ArrayList<>();

	// ========== ADOPTION fields ==========
	private final JComboBox<String> relationshipParent1Combo = new JComboBox<>(new String[]{StringUtils.EMPTY, "biological", "adopted", "foster", "guardian"});
	private final JComboBox<String> relationshipParent2Combo = new JComboBox<>(new String[]{StringUtils.EMPTY, "biological", "adopted", "foster", "guardian"});

	// ========== EVENT_STRUCTURE (0:1) ==========
	private final EventStructurePanel eventStructurePanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> familyHandler = HandlerRegistry.getHandler("FAMILY");
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");

	// ==================== Constructors ====================
	public IndividualEventDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Individual Event", model, record, HandlerRegistry.getHandler(IndividualEventHandler.TYPE));

		this.eventStructurePanel = new EventStructurePanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	public IndividualEventDialog(Frame parent, FLEFModel model){
		super(parent, "New Individual Event", model, null, HandlerRegistry.getHandler(IndividualEventHandler.TYPE));

		this.eventStructurePanel = new EventStructurePanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	// ==================== UI Initialization ====================
	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		// Initialize type combo with all possible values
		initTypeCombo();

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		tabbedPane.addTab("Basic", createBasicPanel());

		// --- Event Structure tab ---
		tabbedPane.addTab("Event Structure", eventStructurePanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());

		// --- Listeners ---
		typeCombo.addItemListener(this::onTypeChanged);
		familyBrowseBtn.addActionListener(e -> browseFamily());
		familyClearBtn.addActionListener(e -> {
			selectedFamilyId = null;
			familyDisplayField.setText(StringUtils.EMPTY);
		});
	}

	private void initTypeCombo(){
		// Add special types first
		typeCombo.addItem("BIRTH");
		typeCombo.addItem("ADOPTION");

		// Add all generic types
		String[] genericTypes = {
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
		Arrays.sort(genericTypes);
		for(String type : genericTypes){
			typeCombo.addItem(type);
		}
		typeCombo.setEditable(true);
	}

	private void onTypeChanged(ItemEvent e){
		String selectedType = (String)typeCombo.getSelectedItem();
		boolean isBirth = "BIRTH".equals(selectedType);
		boolean isAdoption = "ADOPTION".equals(selectedType);
		boolean isGeneric = !isBirth && !isAdoption;

		// Family: optional for BIRTH, mandatory for ADOPTION, hidden for generic
		familyDisplayField.setEnabled(isBirth || isAdoption);
		familyBrowseBtn.setEnabled(isBirth || isAdoption);
		familyClearBtn.setEnabled(isBirth || isAdoption);

		// Twin: only for BIRTH
		twinList.setEnabled(isBirth);
		// Relationship: only for ADOPTION
		relationshipParent1Combo.setEnabled(isAdoption);
		relationshipParent2Combo.setEnabled(isAdoption);

		// Visual feedback for mandatory field in ADOPTION
		if(isAdoption){
			familyDisplayField.setBackground(new Color(255, 255, 200));
		}
		else{
			familyDisplayField.setBackground(UIManager.getColor("TextField.background"));
		}
	}

	// ==================== Basic Panel ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// TYPE
		panel.add(new JLabel("Type*:"), "align label");
		panel.add(typeCombo, "growx,wrap");

		// FAMILY (optional for BIRTH, required for ADOPTION)
		panel.add(new JLabel("Family:"), "align label");
		familyDisplayField.setEditable(false);
		familyDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel familyPanel = new JPanel(new BorderLayout(5, 5));
		familyPanel.add(familyDisplayField, BorderLayout.CENTER);
		JPanel familyBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		familyBtnPanel.add(familyBrowseBtn);
		familyBtnPanel.add(familyClearBtn);
		familyPanel.add(familyBtnPanel, BorderLayout.EAST);
		panel.add(familyPanel, "growx,wrap");

		// TWIN (0:M) for BIRTH
		panel.add(new JLabel("Twins:"), "align label,top");
		JPanel twinPanel = new JPanel(new BorderLayout(3, 3));
		JScrollPane twinScroll = GUIHelper.createScrollPane(twinList);
		twinPanel.add(twinScroll, BorderLayout.CENTER);
		JPanel twinBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addTwinBtn = new JButton("Add");
		JButton removeTwinBtn = new JButton("Remove");
		twinBtnPanel.add(addTwinBtn);
		twinBtnPanel.add(removeTwinBtn);
		twinPanel.add(twinBtnPanel, BorderLayout.SOUTH);

		twinList.addListSelectionListener(e -> removeTwinBtn.setEnabled(twinList.getSelectedIndex() != -1));
		removeTwinBtn.setEnabled(false);

		addTwinBtn.addActionListener(e -> addTwin());
		removeTwinBtn.addActionListener(e -> removeTwin());

		panel.add(twinPanel, "growx,wrap");

		// PARENT1_RELATIONSHIP (0:1) for ADOPTION
		panel.add(new JLabel("Parent 1 Relationship:"), "align label");
		panel.add(relationshipParent1Combo, "growx,wrap");

		// PARENT2_RELATIONSHIP (0:1) for ADOPTION
		panel.add(new JLabel("Parent 2 Relationship:"), "align label");
		panel.add(relationshipParent2Combo, "growx");

		// Apply initial state
		onTypeChanged(null);

		return panel;
	}

	// ==================== Family browsing ====================

	private void browseFamily(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(this), model, familyHandler, selectedId -> {
			if(selectedId != null){
				selectedFamilyId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					familyDisplayField.setText(familyHandler.getDisplayName(rec));
				}
				else{
					familyDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Twin methods ====================

	private String getIndividualDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return individualHandler.getDisplayName(rec);
		}
		return id;
	}

	private void loadTwins(){
		twinListModel.clear();
		twinIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("TWIN".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				twinIds.add(id);
				twinListModel.addElement(getIndividualDisplayName(id));
			}
		}
	}

	private void addTwin(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(this), model, individualHandler, selectedId -> {
			if(selectedId != null && !twinIds.contains(selectedId)){
				twinIds.add(selectedId);
				twinListModel.addElement(getIndividualDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void removeTwin(){
		int idx = twinList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this twin reference?"))
			return;
		twinIds.remove(idx);
		twinListModel.remove(idx);
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// TYPE
		String type = FLEFRecordUtils.getChildValue(record, "TYPE");
		if(type != null){
			typeCombo.setSelectedItem(type);
		}

		// FAMILY
		String familyId = FLEFRecordUtils.getChildValue(record, "FAMILY");
		if(familyId != null && !familyId.isEmpty()){
			selectedFamilyId = familyId;
			FLEFRecord rec = model.getRecordById(familyId);
			if(rec != null){
				familyDisplayField.setText(familyHandler.getDisplayName(rec));
			}
			else{
				familyDisplayField.setText(familyId);
			}
		}

		// TWIN (0:M)
		loadTwins();

		// PARENT1_RELATIONSHIP (0:1)
		String p1 = FLEFRecordUtils.getChildValue(record, "PARENT1_RELATIONSHIP");
		relationshipParent1Combo.setSelectedItem(p1 != null? p1: StringUtils.EMPTY);

		// PARENT2_RELATIONSHIP (0:1)
		String p2 = FLEFRecordUtils.getChildValue(record, "PARENT2_RELATIONSHIP");
		relationshipParent2Combo.setSelectedItem(p2 != null? p2: StringUtils.EMPTY);

		// EVENT_STRUCTURE (0:1)
		FLEFRecord eventStruct = FLEFRecordUtils.findChild(record, "EVENT_STRUCTURE");
		eventStructurePanel.loadFromRecord(eventStruct);

		// Apply type-specific visibility
		onTypeChanged(null);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// TYPE
		String type = (String)typeCombo.getSelectedItem();
		if(type == null || type.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"TYPE is required.\nPlease select an event type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			typeCombo.requestFocusInWindow();
			return false;
		}

		// For ADOPTION, FAMILY is mandatory
		if("ADOPTION".equals(type) && (selectedFamilyId == null || selectedFamilyId.isEmpty())){
			JOptionPane.showMessageDialog(this,
				"FAMILY is required for ADOPTION events.\nPlease select a family.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// EVENT_STRUCTURE (0:1) - validate if present
		return (!eventStructurePanel.hasData() || eventStructurePanel.validateRequiredFields());
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// TYPE
		String type = (String)typeCombo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(record, "TYPE", type);

		// FAMILY
		FLEFRecordUtils.updateChildValue(record, "FAMILY", selectedFamilyId);

		// TWIN
		for(String id : twinIds){
			FLEFRecordUtils.addChild(record, "TWIN", id);
		}

		// PARENT1_RELATIONSHIP (0:1)
		String p1 = (String)relationshipParent1Combo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(record, "PARENT1_RELATIONSHIP", p1);

		// PARENT2_RELATIONSHIP (0:1)
		String p2 = (String)relationshipParent2Combo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(record, "PARENT2_RELATIONSHIP", p2);

		// EVENT_STRUCTURE (0:1)
		if(eventStructurePanel.hasData()){
			FLEFRecord eventStruct = eventStructurePanel.saveToRecord(null);
			if(eventStruct != null){
				eventStruct.setTag("EVENT_STRUCTURE");
				record.addChild(eventStruct);
			}
		}

		if(isNew){
			model.addRecord(record);
		}
		isSaved = true;

		dispose();
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi una famiglia di esempio
		FLEFRecord family = FLEFRecord.createMainRecord("F1", "FAMILY");
		model.addRecord(family);

		// Aggiungi un individuo di esempio per i gemelli
		FLEFRecord ind = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		FLEFRecord name = new FLEFRecord();
		name.setTag("NAME");
		FLEFRecord given = new FLEFRecord();
		given.setTag("INDIVIDUAL_NAME");
		given.setValue("John");
		name.addChild(given);
		FLEFRecord familyName = new FLEFRecord();
		familyName.setTag("FAMILY_NAME");
		familyName.setValue("Doe");
		name.addChild(familyName);
		ind.addChild(name);
		model.addRecord(ind);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Individual Event Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Individual Event");
			btn.addActionListener(e -> {
				IndividualEventDialog dialog = new IndividualEventDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Individual Event saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
