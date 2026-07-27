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
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchStatusHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a RESEARCH_STATUS_RECORD according to FLEF 0.0.9.
 */
public class ResearchStatusDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2955684590804350572L;


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new CalendarHandler());
		HandlerRegistry.register(new ResearchStatusHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "active", "paused", "completed", "blocked"});
	private final JTextField questionField = new JTextField(30);
	private final JComboBox<String> priorityCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "high", "medium", "low"});

	// ========== ASSOCIATION (0:M) ==========
	private final DefaultListModel<String> associationListModel = new DefaultListModel<>();
	private final JList<String> associationList = new JList<>(associationListModel);
	private final List<AssociationEntry> associationEntries = new ArrayList<>();

	// ========== BLOCKED_BY (0:M) ==========
	private final DefaultListModel<String> blockedByListModel = new DefaultListModel<>();
	private final JList<String> blockedByList = new JList<>(blockedByListModel);
	private final List<String> blockedByIds = new ArrayList<>();

	// ========== DESCRIPTION (0:1) ==========
	private final JTextArea descriptionArea = new JTextArea(3, 30);

	// ========== RESOLUTION ==========
	private final JTextArea resolutionArea = new JTextArea(3, 30);

	// ========== MODIFICATION ==========
	private final ModificationPanel modificationPanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> researchHandler = HandlerRegistry.getHandler("RESEARCH_STATUS");
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> familyHandler = HandlerRegistry.getHandler("FAMILY");
	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler("PLACE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ========== Inner class for Association entry ==========
	private static class AssociationEntry{
		boolean isVoid;
		String targetId; // null for void
		String name; // only for void
		List<String> noteIds;

		AssociationEntry(boolean isVoid, String targetId, String name, List<String> noteIds){
			this.isVoid = isVoid;
			this.targetId = targetId;
			this.name = name;
			this.noteIds = noteIds != null? noteIds: new ArrayList<>();
		}

		@Override
		public String toString(){
			if(isVoid){
				return "VOID: " + (name != null && !name.isEmpty()? name: "[unnamed]");
			}
			else{
				return "ID: " + targetId;
			}
		}
	}

	// ==================== Constructors ====================
	public ResearchStatusDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Research Status", model, record, HandlerRegistry.getHandler(ResearchStatusHandler.TYPE));

		this.modificationPanel = new ModificationPanel(this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	public ResearchStatusDialog(Frame parent, FLEFModel model){
		super(parent, "New Research Status", model, null, HandlerRegistry.getHandler(ResearchStatusHandler.TYPE));

		this.modificationPanel = new ModificationPanel(this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 750));
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

		// --- Associations tab ---
		tabbedPane.addTab("Associations", createAssociationsPanel());

		// --- Blocked By tab ---
		tabbedPane.addTab("Blocked By", createBlockedByPanel());

		// --- Modification tab ---
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Panel factories ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// STATUS
		panel.add(new JLabel("Status:"), "align label");
		panel.add(statusCombo, "growx,wrap");

		// QUESTION
		panel.add(new JLabel("Question*:"), "align label");
		panel.add(questionField, "growx,wrap");

		// PRIORITY
		panel.add(new JLabel("Priority:"), "align label");
		panel.add(priorityCombo, "growx,wrap");

		// DESCRIPTION
		panel.add(new JLabel("Description:"), "align label,top");
		JScrollPane descScroll = GUIHelper.createScrollPane(descriptionArea);
		panel.add(descScroll, "growx,wrap");

		// RESOLUTION (0:1)
		panel.add(new JLabel("Resolution:"), "align label,top");
		JScrollPane resScroll = GUIHelper.createScrollPane(resolutionArea);
		panel.add(resScroll, "growx");

		return panel;
	}

	private JPanel createAssociationsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Association"));

		associationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		associationList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAssociation();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(associationList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		associationList.addListSelectionListener(e -> {
			boolean selected = associationList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addAssociation());
		editBtn.addActionListener(e -> editAssociation());
		deleteBtn.addActionListener(e -> deleteAssociation());

		return panel;
	}

	private JPanel createBlockedByPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Blocked By"));

		blockedByList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		blockedByList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editBlockedBy();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(blockedByList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		blockedByList.addListSelectionListener(e -> {
			boolean selected = blockedByList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addBlockedBy());
		editBtn.addActionListener(e -> editBlockedBy());
		deleteBtn.addActionListener(e -> deleteBlockedBy());

		return panel;
	}

	// ==================== Association methods ====================

	private void addAssociation(){
		AssociationDialog dialog = AssociationDialog.createEdit(
			this,
			model,
			null // new association
		);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord assocRecord = dialog.getAssociationRecord();
			String value = assocRecord.getValue();
			boolean isVoid = FLEFRecordUtils.isVoidReference(value) || FLEFRecordUtils.isVoidReference(assocRecord.getId());

			AssociationEntry entry;
			if(isVoid){
				String name = FLEFRecordUtils.getChildValue(assocRecord, "NAME");
				entry = new AssociationEntry(true, null, name, new ArrayList<>());
			}
			else{
				String targetId = value != null? value: assocRecord.getId();
				entry = new AssociationEntry(false, targetId, null, new ArrayList<>());
			}

			// Load notes from the association record
			for(FLEFRecord child : assocRecord.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					entry.noteIds.add(child.getValue());
				}
			}

			associationEntries.add(entry);
			associationListModel.addElement(entry.toString());
		}
	}

	private void editAssociation(){
		int idx = associationList.getSelectedIndex();
		if(idx == -1)
			return;
		AssociationEntry existing = associationEntries.get(idx);

		// Convert to FLEFRecord for the AssociationDialog
		FLEFRecord assocRecord = new FLEFRecord();
		assocRecord.setTag("ASSOCIATION");
		if(existing.isVoid){
			assocRecord.setId("VOID");
			FLEFRecord nameChild = new FLEFRecord();
			nameChild.setTag("NAME");
			nameChild.setValue(existing.name);
			assocRecord.addChild(nameChild);
		}
		else{
			assocRecord.setValue(existing.targetId);
		}
		for(String noteId : existing.noteIds){
			FLEFRecord noteChild = new FLEFRecord();
			noteChild.setTag("NOTE");
			noteChild.setValue(noteId);
			assocRecord.addChild(noteChild);
		}

		AssociationDialog dialog = AssociationDialog.createEdit(
			this,
			model,
			assocRecord
		);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getAssociationRecord();
			String value = updated.getValue();
			boolean isVoid = FLEFRecordUtils.isVoidReference(value) || FLEFRecordUtils.isVoidReference(updated.getId());

			AssociationEntry entry;
			if(isVoid){
				String name = FLEFRecordUtils.getChildValue(updated, "NAME");
				entry = new AssociationEntry(true, null, name, new ArrayList<>());
			}
			else{
				String targetId = value != null? value: updated.getId();
				entry = new AssociationEntry(false, targetId, null, new ArrayList<>());
			}

			// Load notes
			for(FLEFRecord child : updated.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					entry.noteIds.add(child.getValue());
				}
			}

			associationEntries.set(idx, entry);
			associationListModel.set(idx, entry.toString());
		}
	}

	private void deleteAssociation(){
		int idx = associationList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this association?"))
			return;
		associationEntries.remove(idx);
		associationListModel.remove(idx);
	}

	// ==================== Blocked By methods ====================

	private void addBlockedBy(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(this), model, researchHandler, selectedId -> {
			if(selectedId != null && !blockedByIds.contains(selectedId)){
				blockedByIds.add(selectedId);
				blockedByListModel.addElement(getResearchDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editBlockedBy(){
		int idx = blockedByList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = blockedByIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return;
		JDialog dialog = researchHandler.createEditDialog(GUIHelper.getParentFrame(this), model, rec);
		dialog.setVisible(true);
		blockedByListModel.set(idx, getResearchDisplayName(id));
	}

	private void deleteBlockedBy(){
		int idx = blockedByList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this blocked by reference?"))
			return;
		blockedByIds.remove(idx);
		blockedByListModel.remove(idx);
	}

	private String getResearchDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return researchHandler.getDisplayName(rec);
		}
		return id;
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// STATUS
		String status = FLEFRecordUtils.getChildValue(record, "STATUS");
		statusCombo.setSelectedItem(status != null? status: StringUtils.EMPTY);

		// QUESTION
		questionField.setText(FLEFRecordUtils.getChildValue(record, "QUESTION"));

		// PRIORITY
		String priority = FLEFRecordUtils.getChildValue(record, "PRIORITY");
		priorityCombo.setSelectedItem(priority != null? priority: StringUtils.EMPTY);

		// DESCRIPTION
		descriptionArea.setText(FLEFRecordUtils.getChildValue(record, "DESCRIPTION"));

		// RESOLUTION (0:1)
		resolutionArea.setText(FLEFRecordUtils.getChildValue(record, "RESOLUTION"));

		// ASSOCIATION (0:M)
		associationEntries.clear();
		associationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("ASSOCIATION".equals(child.getTag())){
				String value = child.getValue();
				boolean isVoid = FLEFRecordUtils.isVoidReference(value) || FLEFRecordUtils.isVoidReference(child.getId());

				AssociationEntry entry;
				if(isVoid){
					String name = FLEFRecordUtils.getChildValue(child, "NAME");
					entry = new AssociationEntry(true, null, name, new ArrayList<>());
				}
				else{
					String targetId = value != null? value: child.getId();
					entry = new AssociationEntry(false, targetId, null, new ArrayList<>());
				}

				for(FLEFRecord noteChild : child.getChildren()){
					if("NOTE".equals(noteChild.getTag()) && noteChild.getValue() != null){
						entry.noteIds.add(noteChild.getValue());
					}
				}

				associationEntries.add(entry);
				associationListModel.addElement(entry.toString());
			}
		}

		// BLOCKED_BY (0:M)
		blockedByIds.clear();
		blockedByListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("BLOCKED_BY".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				blockedByIds.add(id);
				blockedByListModel.addElement(getResearchDisplayName(id));
			}
		}

		// MODIFICATION
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// QUESTION
		if(questionField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"QUESTION is required.\nPlease enter a research question.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			questionField.requestFocusInWindow();
			return false;
		}

		return true;
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// STATUS
		String status = (String)statusCombo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(record, "STATUS", status);

		// QUESTION
		String question = questionField.getText().trim();
		FLEFRecordUtils.updateChildValue(record, "QUESTION", question);

		// PRIORITY
		String priority = (String)priorityCombo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(record, "PRIORITY", priority);

		// DESCRIPTION
		String description = descriptionArea.getText().trim();
		FLEFRecordUtils.updateChildValue(record, "DESCRIPTION", description);

		// RESOLUTION (0:1)
		String resolution = resolutionArea.getText().trim();
		FLEFRecordUtils.updateChildValue(record, "RESOLUTION", resolution);

		// ASSOCIATION (0:M)
		for(AssociationEntry entry : associationEntries){
			FLEFRecord assoc = new FLEFRecord();
			assoc.setTag("ASSOCIATION");

			if(entry.isVoid){
				assoc.setId("VOID");
				if(entry.name != null && !entry.name.isEmpty()){
					FLEFRecord nameChild = new FLEFRecord();
					nameChild.setTag("NAME");
					nameChild.setValue(entry.name);
					assoc.addChild(nameChild);
				}
			}
			else{
				assoc.setValue(entry.targetId);
			}

			for(String noteId : entry.noteIds){
				FLEFRecord noteChild = new FLEFRecord();
				noteChild.setTag("NOTE");
				noteChild.setValue(noteId);
				assoc.addChild(noteChild);
			}

			record.addChild(assoc);
		}

		// BLOCKED_BY (0:M)
		for(String id : blockedByIds){
			FLEFRecordUtils.addChild(record, "BLOCKED_BY", id);
		}

		// MODIFICATION
		modificationPanel.saveToRecord(record);

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

		// Aggiungi un individuo di esempio per le associazioni
		FLEFRecord ind = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		FLEFRecord name = new FLEFRecord();
		name.setTag("NAME");
		FLEFRecord given = new FLEFRecord();
		given.setTag("INDIVIDUAL_NAME");
		given.setValue("John");
		name.addChild(given);
		FLEFRecord family = new FLEFRecord();
		family.setTag("FAMILY_NAME");
		family.setValue("Doe");
		name.addChild(family);
		ind.addChild(name);
		model.addRecord(ind);

		// Aggiungi un research status di esempio
		FLEFRecord research = FLEFRecord.createMainRecord("R1", "RESEARCH_STATUS");
		FLEFRecord question = new FLEFRecord();
		question.setTag("QUESTION");
		question.setValue("Who was the father of John Doe?");
		research.addChild(question);
		model.addRecord(research);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Research Status Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Research Status");
			btn.addActionListener(e -> {
				ResearchStatusDialog dialog = new ResearchStatusDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Research Status saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
