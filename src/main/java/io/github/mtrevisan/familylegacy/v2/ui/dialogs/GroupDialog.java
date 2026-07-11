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
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dialog for editing a GROUP_RECORD according to FLEF 0.0.9.
 * <p>
 * A group can be of genealogical, historical, or general interest.
 * Examples: households, neighborhoods, fraternities, communes, orphanages, etc.
 */
public class GroupDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 413684899528463158L;


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new CalendarHandler());
	}

	// ==================== Basic fields ====================
	private final JTextField idField = new JTextField(10);
	private final JTextField nameField = new JTextField(30);
	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		"", "neighborhood", "fraternity", "ladies club", "literary society",
		"commune", "orphanage", "group home", "household", "workplace",
		"school", "church", "military unit", "association", "club"
	});
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// ========== INDIVIDUAL (0:M) ==========
	private final DefaultListModel<String> individualListModel = new DefaultListModel<>();
	private final JList<String> individualList = new JList<>(individualListModel);
	private final List<String> individualIds = new ArrayList<>();

	// ========== FAMILY (0:M) ==========
	private final DefaultListModel<String> familyListModel = new DefaultListModel<>();
	private final JList<String> familyList = new JList<>(familyListModel);
	private final List<String> familyIds = new ArrayList<>();

	// ========== EVENT (0:M) ==========
	private final DefaultListModel<String> eventListModel = new DefaultListModel<>();
	private final JList<String> eventList = new JList<>(eventListModel);
	private final List<String> eventIds = new ArrayList<>();

	// ========== NOTE (0:M) ==========
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== SOURCE_CITATION (0:M) ==========
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// ========== MODIFICATION (1:1) ==========
	private final ModificationPanel modificationPanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> familyHandler = HandlerRegistry.getHandler("FAMILY");
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ==================== Constructors ====================
	public GroupDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, model, "Edit Group", record);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 700));
		pack();
		setLocationRelativeTo(parent);
	}

	public GroupDialog(Frame parent, FLEFModel model){
		super(parent, model, "New Group", null);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 700));
		pack();
		setLocationRelativeTo(parent);
	}

	// ==================== UI Initialization ====================
	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		tabbedPane.addTab("Basic", createBasicPanel());

		// --- Members tab ---
		tabbedPane.addTab("Members", createMembersPanel());

		// --- Events tab ---
		tabbedPane.addTab("Events", createEventsPanel());

		// --- Notes tab ---
		tabbedPane.addTab("Notes", createNotesPanel());

		// --- Source Citations tab ---
		tabbedPane.addTab("Source Citations", createSourceCitationsPanel());

		// --- Modification tab ---
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> saveRecord());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Panel factories ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		panel.add(new JLabel("Name:"), "align label");
		panel.add(nameField, "growx,wrap");

		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx,wrap");

		panel.add(restrictionCheckBox, "span 2,wrap");

		return panel;
	}

	private JPanel createMembersPanel(){
		JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Individuals list
		JPanel individualPanel = createReferenceListPanel(
			"Individuals",
			individualList,
			individualListModel,
			individualIds,
			this::addIndividual,
			this::removeIndividual
		);

		// Families list
		JPanel familyPanel = createReferenceListPanel(
			"Families",
			familyList,
			familyListModel,
			familyIds,
			this::addFamily,
			this::removeFamily
		);

		panel.add(individualPanel);
		panel.add(familyPanel);

		return panel;
	}

	private JPanel createEventsPanel(){
		return createListPanel("Events", eventList, eventListModel,
			this::addEvent, this::editEvent, this::deleteEvent);
	}

	private JPanel createNotesPanel(){
		return createListPanel("Notes", noteList, noteListModel,
			this::addNote, this::editNote, this::deleteNote);
	}

	private JPanel createSourceCitationsPanel(){
		return createListPanel("Source Citations", sourceCitationList, sourceCitationListModel,
			this::addSourceCitation, this::editSourceCitation, this::deleteSourceCitation);
	}

	// ==================== Reference list panel helper ====================

	private JPanel createReferenceListPanel(String title, JList<String> list, DefaultListModel<String> model,
		List<String> ids, Runnable addAction, Runnable removeAction){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder(title));

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 150));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton removeBtn = new JButton("Remove");
		btnPanel.add(addBtn);
		btnPanel.add(removeBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		list.addListSelectionListener(e -> removeBtn.setEnabled(list.getSelectedIndex() != -1));
		removeBtn.setEnabled(false);

		addBtn.addActionListener(e -> addAction.run());
		removeBtn.addActionListener(e -> removeAction.run());

		return panel;
	}

	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
		Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder(title));

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2)
					editAction.run();
			}
		});
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addAction.run());
		newBtn.addActionListener(e -> createNewItemForList(list, model));
		editBtn.addActionListener(e -> editAction.run());
		deleteBtn.addActionListener(e -> deleteAction.run());

		return panel;
	}

	// ==================== Members methods ====================

	private void addIndividual(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null && !individualIds.contains(selectedId)){
				individualIds.add(selectedId);
				individualListModel.addElement(getIndividualDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void removeIndividual(){
		int idx = individualList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this individual from the group?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			individualIds.remove(idx);
			individualListModel.remove(idx);
		}
	}

	private void addFamily(){
		if(familyHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Family handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, familyHandler, selectedId -> {
			if(selectedId != null && !familyIds.contains(selectedId)){
				familyIds.add(selectedId);
				familyListModel.addElement(getFamilyDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void removeFamily(){
		int idx = familyList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this family from the group?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			familyIds.remove(idx);
			familyListModel.remove(idx);
		}
	}

	private String getIndividualDisplayName(String id){
		if(individualHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return individualHandler.getDisplayName(rec);
		}
		return id;
	}

	private String getFamilyDisplayName(String id){
		if(familyHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return familyHandler.getDisplayName(rec);
		}
		return id;
	}

	// ==================== Event methods ====================

	private void addEvent(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, eventHandler, selectedId -> {
			if(selectedId != null && !eventIds.contains(selectedId)){
				eventIds.add(selectedId);
				eventListModel.addElement(getEventDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = eventIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return;
		JDialog dialog = eventHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		eventListModel.set(idx, getEventDisplayName(id));
	}

	private void deleteEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this event?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			eventIds.remove(idx);
			eventListModel.remove(idx);
		}
	}

	private String getEventDisplayName(String id){
		if(eventHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return eventHandler.getDisplayName(rec);
		}
		return id;
	}

	// ==================== Note methods ====================

	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return noteHandler.getDisplayName(rec);
		}
		return id;
	}

	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteListModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return;
		JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteListModel.remove(idx);
		}
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Source Citation methods ====================

	private void addSourceCitation(){
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setLevel(1);
				citation.setTag("SOURCE_CITATION");
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
			}
		}
	}

	private void editSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = sourceCitationRecords.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceCitationRecords.set(idx, updated);
				sourceCitationListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void deleteSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			sourceCitationRecords.remove(idx);
			sourceCitationListModel.remove(idx);
		}
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null)
				return sourceHandler.getDisplayName(rec);
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JDialog dialog = sourceHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
	}

	private void createNewEvent(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JDialog dialog = eventHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
	}

	private void createNewItemForList(JList<String> list, DefaultListModel<String> model){
		if(list == eventList){
			createNewEvent();
			return;
		}
		if(list == noteList){
			createNewNote();
			return;
		}
		if(list == sourceCitationList){
			createNewSource();
			return;
		}
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());
		nameField.setText(FLEFRecordUtils.getChildValue(record, "NAME"));
		typeCombo.setSelectedItem(FLEFRecordUtils.getChildValue(record, "TYPE"));
		restrictionCheckBox.setSelected("confidential".equals(FLEFRecordUtils.getChildValue(record, "RESTRICTION")));

		// --- INDIVIDUALS ---
		individualIds.clear();
		individualListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("INDIVIDUAL".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				individualIds.add(id);
				individualListModel.addElement(getIndividualDisplayName(id));
			}
		}

		// --- FAMILIES ---
		familyIds.clear();
		familyListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("FAMILY".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				familyIds.add(id);
				familyListModel.addElement(getFamilyDisplayName(id));
			}
		}

		// --- EVENTS ---
		eventIds.clear();
		eventListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("EVENT".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				eventIds.add(id);
				eventListModel.addElement(getEventDisplayName(id));
			}
		}

		// --- NOTES ---
		noteIds.clear();
		noteListModel.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : record.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
			}
		}

		// --- SOURCE CITATIONS ---
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// --- MODIFICATION ---
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		record.getChildren().clear();

		// NAME (1:1) - required
		String name = nameField.getText().trim();
		if(!name.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "NAME", name);
		}

		// TYPE (0:1)
		String type = (String)typeCombo.getSelectedItem();
		if(type != null && !type.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "TYPE", type);
		}

		// RESTRICTION (0:1)
		FLEFRecordUtils.updateChildValue(record, "RESTRICTION",
			restrictionCheckBox.isSelected()? "confidential": null);

		// INDIVIDUALS
		for(String id : individualIds){
			FLEFRecordUtils.addChild(record, "INDIVIDUAL", 1, id);
		}

		// FAMILIES
		for(String id : familyIds){
			FLEFRecordUtils.addChild(record, "FAMILY", 1, id);
		}

		// EVENTS
		for(String id : eventIds){
			FLEFRecordUtils.addChild(record, "EVENT", 1, id);
		}

		// NOTES
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		// SOURCE CITATIONS
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// MODIFICATION
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// NAME (1:1) - required
		if(nameField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"NAME is required for a group.\nPlease enter a group name.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			nameField.requestFocusInWindow();
			return false;
		}

		// MODIFICATION_STRUCTURE (1:1) - required if group has data
		return modificationPanel.validateRequiredFields();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("GROUP");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "GROUP", "G");
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();
		// Tutti gli handler vengono registrati automaticamente tramite i blocchi static

		// Aggiungi alcuni dati di esempio
		FLEFRecord ind = new FLEFRecord();
		ind.setId("I1");
		ind.setType("INDIVIDUAL");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		FLEFRecord given = new FLEFRecord();
		given.setLevel(2);
		given.setTag("INDIVIDUAL_NAME");
		given.setValue("John");
		name.addChild(given);
		FLEFRecord family = new FLEFRecord();
		family.setLevel(2);
		family.setTag("FAMILY_NAME");
		family.setValue("Doe");
		name.addChild(family);
		ind.addChild(name);
		model.addRecord(ind);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Group Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Group");
			btn.addActionListener(e -> {
				GroupDialog dialog = new GroupDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Group saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
