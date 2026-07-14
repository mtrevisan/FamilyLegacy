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
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Dialog for editing a FAMILY_CHILD or FAMILY_PARENT link details.
 * <p>
 * Fields:
 * <ul>
 *   <li>CERTAINTY: challenged, disproven, proven</li>
 *   <li>CREDIBILITY: 0, 1, 2, 3</li>
 *   <li>NOTE: list of note references (0:M) - shows note content, not just ID</li>
 *   <li>CONCLUSION: full CONCLUSION_STRUCTURE with CONTEXT, RESOLVES, PREFERRED, PROOF_STATUS, NARRATIVE, DATE, SOURCE_CITATION, NOTE</li>
 *   <li>MODIFICATION: CREATION (1:1) and UPDATE (0:M) (under CONCLUSION)</li>
 * </ul>
 */
public class FamilyLinkDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -2425933933340943236L;


	private final FLEFModel model;
	private final Frame parentFrame;
	private final String familyId;
	private final String linkType; // "FAMILY_CHILD" or "FAMILY_PARENT"
	private final FLEFRecord existingLink; // may be null for new links
	private boolean saved = false;

	// ========== CERTAINTY & CREDIBILITY ==========
	private final EvidenceQualifiersPanel qualifiersPanel = new EvidenceQualifiersPanel("Link Evidence");

	// ========== NOTE references (direct children of the link) ==========
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== CONCLUSION fields ==========
	private final JTextField conclusionContextField = new JTextField(15);

	// RESOLVES: list of event references (single selection)
	private final DefaultListModel<String> resolvesModel = new DefaultListModel<>();
	private final JList<String> resolvesList = new JList<>(resolvesModel);
	private final List<String> resolvesIds = new ArrayList<>();

	// PREFERRED: single event reference (non-editable, set from resolves list)
	private final JTextField preferredField = new JTextField(15);

	private final JComboBox<String> proofStatusCombo = new JComboBox<>(new String[]{
		"", "unresearched", "conflicting_evidence", "preponderance_of_evidence",
		"proven", "disproven"
	});
	private final JTextArea narrativeArea = new JTextArea(3, 20);

	// NARRATIVE -> NOTE references (0:M)
	private final DefaultListModel<String> narrativeNoteModel = new DefaultListModel<>();
	private final JList<String> narrativeNoteList = new JList<>(narrativeNoteModel);
	private final List<String> narrativeNoteIds = new ArrayList<>();
	private final Map<String, String> narrativeNoteDisplayMap = new HashMap<>();

	private final JTextField conclusionDateField = new JTextField(15);

	// CONCLUSION -> SOURCE_CITATION references (0:M)
	private final DefaultListModel<String> conclusionSourceModel = new DefaultListModel<>();
	private final JList<String> conclusionSourceList = new JList<>(conclusionSourceModel);
	private final List<String> conclusionSourceIds = new ArrayList<>();
	private final Map<String, String> conclusionSourceDisplayMap = new HashMap<>();

	// CONCLUSION -> NOTE references (0:M)
	private final DefaultListModel<String> conclusionNoteModel = new DefaultListModel<>();
	private final JList<String> conclusionNoteList = new JList<>(conclusionNoteModel);
	private final List<String> conclusionNoteIds = new ArrayList<>();
	private final Map<String, String> conclusionNoteDisplayMap = new HashMap<>();

	// ========== MODIFICATION fields (under CONCLUSION) ==========
	// CREATION (1:1)
	private final JTextField creationDateField = new JTextField(15);

	// UPDATE (0:M)
	private final DefaultListModel<String> updateModel = new DefaultListModel<>();
	private final JList<String> updateList = new JList<>(updateModel);
	private final List<UpdateRecord> updateRecords = new ArrayList<>();

	// ========== Handlers ==========
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Inner class to represent an UPDATE record ==========
	private static class UpdateRecord{
		String date;
		String noteId; // NOTE reference (0:1)

		UpdateRecord(String date, String noteId){
			this.date = date;
			this.noteId = noteId;
		}

		@Override
		public String toString(){
			if(date != null && !date.isEmpty()){
				return date + (noteId != null && !noteId.isEmpty()? " (note: " + noteId + ")": "");
			}
			return "(empty)";
		}
	}

	public FamilyLinkDialog(JDialog parent, FLEFModel model, String familyId, String linkType, FLEFRecord existingLink){
		super(parent, "Edit " + linkType + " Link", true);

		this.model = model;
		this.parentFrame = getParentFrame(parent);
		this.familyId = familyId;
		this.linkType = linkType;
		this.existingLink = existingLink;
		initComponents();
		if(existingLink != null){
			loadData();
		}
		pack();
		setMinimumSize(new Dimension(620, 800));
		setLocationRelativeTo(parent);
	}

	private Frame getParentFrame(Component comp){
		Window w = SwingUtilities.getWindowAncestor(comp);
		if(w instanceof Frame){
			return (Frame)w;
		}
		return null;
	}

	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane mainTabbedPane = new JTabbedPane();

		// ===== Main tab (with Notes integrated) =====
		JPanel mainPanel = createMainPanel();
		mainTabbedPane.addTab("Main", mainPanel);

		// ===== Conclusion tab (with sub-tabs) =====
		JPanel conclusionContainer = createConclusionContainer();
		mainTabbedPane.addTab("Conclusion", conclusionContainer);

		add(mainTabbedPane, BorderLayout.CENTER);

		// ===== Button panel =====
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> {
			saved = true;
			dispose();
		});
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Main Panel (with Notes integrated) ====================

	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Family ID (read-only)
		panel.add(new JLabel("Family ID:"), "align label");
		panel.add(new JLabel(familyId), "growx,wrap");

		// Link Type (read-only)
		panel.add(new JLabel("Link Type:"), "align label");
		panel.add(new JLabel(linkType), "growx,wrap");

		// CERTAINTY + CREDIBILITY (grouped in EvidenceQualifiersPanel)
		panel.add(qualifiersPanel, "span 2,growx,wrap");

		// ----- Notes (integrated) -----
		panel.add(new JLabel("Notes:"), "align label,top");
		JPanel notesPanel = createNotesPanel("Note References", noteModel, noteList, noteIds, noteDisplayMap,
			this::addNote, this::editNote, this::removeNote, this::createNewNote);
		panel.add(notesPanel, "growx,wrap");

		return panel;
	}

	// ==================== Notes Panel Helper ====================

	private JPanel createNotesPanel(String title, DefaultListModel<String> model, JList<String> list,
		List<String> ids, Map<String, String> displayMap,
		Runnable addAction, Runnable editAction, Runnable removeAction,
		Runnable newAction){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder(title));

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 70));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New");
		JButton editBtn = new JButton("Edit");
		JButton removeBtn = new JButton("Remove");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(removeBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});
		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			removeBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		removeBtn.setEnabled(false);

		addBtn.addActionListener(e -> addAction.run());
		newBtn.addActionListener(e -> newAction.run());
		editBtn.addActionListener(e -> editAction.run());
		removeBtn.addActionListener(e -> removeAction.run());

		return panel;
	}

	// ==================== Notes methods ====================

	private String getNoteDisplayName(String noteId){
		if(noteHandler != null){
			FLEFRecord note = model.getRecordById(noteId);
			if(note != null){
				return noteHandler.getDisplayName(note);
			}
		}
		return noteId;
	}

	private void loadNotes(){
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : existingLink.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String noteId = child.getValue();
				noteIds.add(noteId);
				String display = getNoteDisplayName(noteId);
				noteDisplayMap.put(noteId, display);
				noteModel.addElement(display);
			}
		}
	}

	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentFrame, model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		String noteId = noteIds.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord note = model.getRecordById(noteId);
		if(note == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + noteId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(parentFrame, model, note);
		dialog.setVisible(true);
		// Update display after edit
		String newDisplay = getNoteDisplayName(noteId);
		noteDisplayMap.put(noteId, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void removeNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteModel.remove(idx);
		}
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(parentFrame, model);
		dialog.setVisible(true);
		// Check if a new note was added to the model
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Narrative Notes methods ====================

	private String getNarrativeNoteDisplayName(String noteId){
		if(noteHandler != null){
			FLEFRecord note = model.getRecordById(noteId);
			if(note != null){
				return noteHandler.getDisplayName(note);
			}
		}
		return noteId;
	}

	private void loadNarrativeNotes(){
		narrativeNoteModel.clear();
		narrativeNoteIds.clear();
		narrativeNoteDisplayMap.clear();
		FLEFRecord narrative = FLEFRecordUtils.findChild(existingLink, "NARRATIVE");
		if(narrative != null){
			for(FLEFRecord child : narrative.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					String noteId = child.getValue();
					narrativeNoteIds.add(noteId);
					String display = getNarrativeNoteDisplayName(noteId);
					narrativeNoteDisplayMap.put(noteId, display);
					narrativeNoteModel.addElement(display);
				}
			}
		}
	}

	private void addNarrativeNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentFrame, model, noteHandler, selectedId -> {
			if(selectedId != null && !narrativeNoteIds.contains(selectedId)){
				narrativeNoteIds.add(selectedId);
				String display = getNarrativeNoteDisplayName(selectedId);
				narrativeNoteDisplayMap.put(selectedId, display);
				narrativeNoteModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNarrativeNote(){
		int idx = narrativeNoteList.getSelectedIndex();
		if(idx == -1)
			return;
		String noteId = narrativeNoteIds.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord note = model.getRecordById(noteId);
		if(note == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + noteId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(parentFrame, model, note);
		dialog.setVisible(true);
		String newDisplay = getNarrativeNoteDisplayName(noteId);
		narrativeNoteDisplayMap.put(noteId, newDisplay);
		narrativeNoteModel.set(idx, newDisplay);
	}

	private void removeNarrativeNote(){
		int idx = narrativeNoteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this narrative note?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = narrativeNoteIds.remove(idx);
			narrativeNoteDisplayMap.remove(removedId);
			narrativeNoteModel.remove(idx);
		}
	}

	private void createNewNarrativeNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(narrativeNoteIds);
		JDialog dialog = noteHandler.createNewDialog(parentFrame, model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !narrativeNoteIds.contains(id)){
				narrativeNoteIds.add(id);
				String display = getNarrativeNoteDisplayName(id);
				narrativeNoteDisplayMap.put(id, display);
				narrativeNoteModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Conclusion Source Citations methods ====================

	private String getSourceDisplayName(String sourceId){
		if(sourceHandler != null){
			FLEFRecord source = model.getRecordById(sourceId);
			if(source != null){
				return sourceHandler.getDisplayName(source);
			}
		}
		return sourceId;
	}

	private void loadConclusionSources(){
		conclusionSourceModel.clear();
		conclusionSourceIds.clear();
		conclusionSourceDisplayMap.clear();
		FLEFRecord conclusion = FLEFRecordUtils.findChild(existingLink, "CONCLUSION");
		if(conclusion != null){
			for(FLEFRecord child : conclusion.getChildren()){
				if("SOURCE_CITATION".equals(child.getTag()) && child.getValue() != null){
					String sourceId = child.getValue();
					conclusionSourceIds.add(sourceId);
					String display = getSourceDisplayName(sourceId);
					conclusionSourceDisplayMap.put(sourceId, display);
					conclusionSourceModel.addElement(display);
				}
			}
		}
	}

	private void addConclusionSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentFrame, model, sourceHandler, selectedId -> {
			if(selectedId != null && !conclusionSourceIds.contains(selectedId)){
				conclusionSourceIds.add(selectedId);
				String display = getSourceDisplayName(selectedId);
				conclusionSourceDisplayMap.put(selectedId, display);
				conclusionSourceModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editConclusionSource(){
		int idx = conclusionSourceList.getSelectedIndex();
		if(idx == -1)
			return;
		String sourceId = conclusionSourceIds.get(idx);
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord source = model.getRecordById(sourceId);
		if(source == null){
			JOptionPane.showMessageDialog(this, "Source not found: " + sourceId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = sourceHandler.createEditDialog(parentFrame, model, source);
		dialog.setVisible(true);
		String newDisplay = getSourceDisplayName(sourceId);
		conclusionSourceDisplayMap.put(sourceId, newDisplay);
		conclusionSourceModel.set(idx, newDisplay);
	}

	private void removeConclusionSource(){
		int idx = conclusionSourceList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = conclusionSourceIds.remove(idx);
			conclusionSourceDisplayMap.remove(removedId);
			conclusionSourceModel.remove(idx);
		}
	}

	private void createNewConclusionSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(conclusionSourceIds);
		JDialog dialog = sourceHandler.createNewDialog(parentFrame, model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !conclusionSourceIds.contains(id)){
				conclusionSourceIds.add(id);
				String display = getSourceDisplayName(id);
				conclusionSourceDisplayMap.put(id, display);
				conclusionSourceModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Conclusion Note methods ====================

	private String getConclusionNoteDisplayName(String noteId){
		if(noteHandler != null){
			FLEFRecord note = model.getRecordById(noteId);
			if(note != null){
				return noteHandler.getDisplayName(note);
			}
		}
		return noteId;
	}

	private void loadConclusionNotes(){
		conclusionNoteModel.clear();
		conclusionNoteIds.clear();
		conclusionNoteDisplayMap.clear();
		FLEFRecord conclusion = FLEFRecordUtils.findChild(existingLink, "CONCLUSION");
		if(conclusion != null){
			for(FLEFRecord child : conclusion.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					String noteId = child.getValue();
					conclusionNoteIds.add(noteId);
					String display = getConclusionNoteDisplayName(noteId);
					conclusionNoteDisplayMap.put(noteId, display);
					conclusionNoteModel.addElement(display);
				}
			}
		}
	}

	private void addConclusionNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentFrame, model, noteHandler, selectedId -> {
			if(selectedId != null && !conclusionNoteIds.contains(selectedId)){
				conclusionNoteIds.add(selectedId);
				String display = getConclusionNoteDisplayName(selectedId);
				conclusionNoteDisplayMap.put(selectedId, display);
				conclusionNoteModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editConclusionNote(){
		int idx = conclusionNoteList.getSelectedIndex();
		if(idx == -1)
			return;
		String noteId = conclusionNoteIds.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord note = model.getRecordById(noteId);
		if(note == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + noteId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(parentFrame, model, note);
		dialog.setVisible(true);
		String newDisplay = getConclusionNoteDisplayName(noteId);
		conclusionNoteDisplayMap.put(noteId, newDisplay);
		conclusionNoteModel.set(idx, newDisplay);
	}

	private void removeConclusionNote(){
		int idx = conclusionNoteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this conclusion note?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = conclusionNoteIds.remove(idx);
			conclusionNoteDisplayMap.remove(removedId);
			conclusionNoteModel.remove(idx);
		}
	}

	private void createNewConclusionNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(conclusionNoteIds);
		JDialog dialog = noteHandler.createNewDialog(parentFrame, model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !conclusionNoteIds.contains(id)){
				conclusionNoteIds.add(id);
				String display = getConclusionNoteDisplayName(id);
				conclusionNoteDisplayMap.put(id, display);
				conclusionNoteModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Conclusion Container (with sub-tabs) ====================

	private JPanel createConclusionContainer(){
		JTabbedPane subTabbedPane = new JTabbedPane();

		// Sub-tab: Conclusion
		JPanel conclusionPanel = createConclusionPanel();
		subTabbedPane.addTab("Conclusion", conclusionPanel);

		// Sub-tab: Modification
		JPanel modificationPanel = createModificationPanel();
		subTabbedPane.addTab("Modification", modificationPanel);

		JPanel container = new JPanel(new BorderLayout());
		container.add(subTabbedPane, BorderLayout.CENTER);
		return container;
	}

	// ==================== Conclusion Panel ====================

	private JPanel createConclusionPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]5[]5[]5[]"));
		panel.setBorder(new TitledBorder("Conclusion"));

		// CONTEXT (1:1)
		panel.add(new JLabel("Context:"), "align label");
		panel.add(conclusionContextField, "growx,wrap");

		// RESOLVES (0:M) - list of event references (single selection)
		panel.add(new JLabel("Resolves (Events):"), "align label,top");
		JPanel resolvesPanel = new JPanel(new BorderLayout(3, 3));
		resolvesPanel.add(new JScrollPane(resolvesList), BorderLayout.CENTER);

		// Buttons panel: Add/Edit/Remove on the left, Set as Preferred on the right
		JPanel resolvesBtnPanel = new JPanel(new BorderLayout());
		JPanel leftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addResolvesBtn = new JButton("Add Event");
		JButton editResolvesBtn = new JButton("Edit");
		JButton removeResolvesBtn = new JButton("Remove");
		leftBtnPanel.add(addResolvesBtn);
		leftBtnPanel.add(editResolvesBtn);
		leftBtnPanel.add(removeResolvesBtn);
		resolvesBtnPanel.add(leftBtnPanel, BorderLayout.WEST);

		JButton setPreferredBtn = new JButton("Set as Preferred");
		setPreferredBtn.setToolTipText("Sets the selected event from the Resolves list as the Preferred event");
		resolvesBtnPanel.add(setPreferredBtn, BorderLayout.EAST);

		resolvesPanel.add(resolvesBtnPanel, BorderLayout.SOUTH);
		panel.add(resolvesPanel, "growx,wrap");

		resolvesList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editResolves();
				}
			}
		});
		resolvesList.addListSelectionListener(e -> {
			boolean selected = resolvesList.getSelectedIndex() != -1;
			editResolvesBtn.setEnabled(selected);
			removeResolvesBtn.setEnabled(selected);
		});
		editResolvesBtn.setEnabled(false);
		removeResolvesBtn.setEnabled(false);

		addResolvesBtn.addActionListener(e -> addResolves());
		editResolvesBtn.addActionListener(e -> editResolves());
		removeResolvesBtn.addActionListener(e -> removeResolves());
		setPreferredBtn.addActionListener(e -> setPreferredFromResolves());

		// PREFERRED (0:1) - single event reference from resolves list (non-editable)
		panel.add(new JLabel("Preferred Event:"), "align label");
		preferredField.setEditable(false);
		preferredField.setBackground(UIManager.getColor("TextField.background"));
		preferredField.setForeground(UIManager.getColor("TextField.foreground"));
		panel.add(preferredField, "growx,wrap");

		// PROOF_STATUS (1:1)
		panel.add(new JLabel("Proof Status:"), "align label");
		panel.add(proofStatusCombo, "growx,wrap");

		// NARRATIVE (0:1)
		panel.add(new JLabel("Narrative:"), "align label,top");
		JScrollPane narrScroll = new JScrollPane(narrativeArea);
		narrScroll.setPreferredSize(new Dimension(200, 60));
		panel.add(narrScroll, "growx,wrap");

		// NARRATIVE -> NOTE (0:M)
		panel.add(new JLabel("Narrative Notes:"), "align label,top");
		JPanel narrativeNotePanel = createNotesPanel("Narrative Note References", narrativeNoteModel,
			narrativeNoteList, narrativeNoteIds, narrativeNoteDisplayMap,
			this::addNarrativeNote, this::editNarrativeNote, this::removeNarrativeNote,
			this::createNewNarrativeNote);
		panel.add(narrativeNotePanel, "growx,wrap");

		// DATE (0:1)
		panel.add(new JLabel("Date:"), "align label");
		panel.add(conclusionDateField, "growx,wrap");

		// SOURCE_CITATION (0:M) - references under CONCLUSION
		panel.add(new JLabel("Source Citations:"), "align label,top");
		JPanel sourcePanel = createNotesPanel("Source References", conclusionSourceModel,
			conclusionSourceList, conclusionSourceIds, conclusionSourceDisplayMap,
			this::addConclusionSource, this::editConclusionSource, this::removeConclusionSource,
			this::createNewConclusionSource);
		panel.add(sourcePanel, "growx,wrap");

		// NOTE (0:M) - references under CONCLUSION
		panel.add(new JLabel("Conclusion Notes:"), "align label,top");
		JPanel conclusionNotePanel = createNotesPanel("Conclusion Note References", conclusionNoteModel,
			conclusionNoteList, conclusionNoteIds, conclusionNoteDisplayMap,
			this::addConclusionNote, this::editConclusionNote, this::removeConclusionNote,
			this::createNewConclusionNote);
		panel.add(conclusionNotePanel, "growx,wrap");

		return panel;
	}

	// ==================== Modification Panel (sub-tab) ====================

	private JPanel createModificationPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]"));
		panel.setBorder(new TitledBorder("Modification (under Conclusion)"));

		// CREATION (1:1)
		panel.add(new JLabel("Creation Date:"), "align label");
		panel.add(creationDateField, "growx,wrap");

		// UPDATE (0:M)
		panel.add(new JLabel("Updates:"), "align label,top");
		JPanel updatePanel = new JPanel(new BorderLayout(3, 3));
		updatePanel.add(new JScrollPane(updateList), BorderLayout.CENTER);

		JPanel updateBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addUpdateBtn = new JButton("Add");
		JButton editUpdateBtn = new JButton("Edit");
		JButton removeUpdateBtn = new JButton("Remove");
		updateBtnPanel.add(addUpdateBtn);
		updateBtnPanel.add(editUpdateBtn);
		updateBtnPanel.add(removeUpdateBtn);
		updatePanel.add(updateBtnPanel, BorderLayout.SOUTH);

		panel.add(updatePanel, "growx,wrap");

		updateList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editUpdate();
				}
			}
		});
		updateList.addListSelectionListener(e -> {
			boolean selected = updateList.getSelectedIndex() != -1;
			editUpdateBtn.setEnabled(selected);
			removeUpdateBtn.setEnabled(selected);
		});
		editUpdateBtn.setEnabled(false);
		removeUpdateBtn.setEnabled(false);

		addUpdateBtn.addActionListener(e -> addUpdate());
		editUpdateBtn.addActionListener(e -> editUpdate());
		removeUpdateBtn.addActionListener(e -> removeUpdate());

		return panel;
	}

	// ==================== Utility methods ====================

	private void setPreferredFromResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1){
			JOptionPane.showMessageDialog(this, "Please select an event from the Resolves list first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String selectedId = resolvesIds.get(idx);
		preferredField.setText(selectedId);
	}

	// ==================== RESOLVES methods ====================

	private void addResolves(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(this, "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentFrame, model, eventHandler, selectedId -> {
			if(selectedId != null && !resolvesIds.contains(selectedId)){
				resolvesIds.add(selectedId);
				resolvesModel.addElement(selectedId);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1)
			return;
		String currentId = resolvesIds.get(idx);
		String newId = (String)JOptionPane.showInputDialog(
			this,
			"Enter new Event ID:",
			"Edit Resolves",
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			currentId
		);
		if(newId == null || newId.trim().isEmpty())
			return;
		String trimmed = newId.trim();
		if(!trimmed.equals(currentId) && resolvesIds.contains(trimmed)){
			JOptionPane.showMessageDialog(this, "ID already in use.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return;
		}
		// If the edited ID was the Preferred event, update the Preferred field
		String preferredId = preferredField.getText().trim();
		if(preferredId.equals(currentId)){
			preferredField.setText(trimmed);
		}
		resolvesIds.set(idx, trimmed);
		resolvesModel.set(idx, trimmed);
	}

	private void removeResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1)
			return;
		String removedId = resolvesIds.get(idx);
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this event reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String preferredId = preferredField.getText().trim();
			if(preferredId.equals(removedId)){
				preferredField.setText("");
			}
			resolvesIds.remove(idx);
			resolvesModel.remove(idx);
		}
	}

	// ==================== UPDATE methods ====================

	private void addUpdate(){
		UpdateRecord newRec = showUpdateDialog(null);
		if(newRec != null){
			updateRecords.add(newRec);
			updateModel.addElement(newRec.toString());
		}
	}

	private void editUpdate(){
		int idx = updateList.getSelectedIndex();
		if(idx == -1)
			return;
		UpdateRecord current = updateRecords.get(idx);
		UpdateRecord updated = showUpdateDialog(current);
		if(updated != null){
			updateRecords.set(idx, updated);
			updateModel.set(idx, updated.toString());
		}
	}

	private void removeUpdate(){
		int idx = updateList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this update?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			updateRecords.remove(idx);
			updateModel.remove(idx);
		}
	}

	/**
	 * Shows a sub-dialog to edit an Update record.
	 * The NOTE is a single reference (0:1) with Browse and Clear buttons.
	 *
	 * @param existing the existing UpdateRecord, or null for new
	 * @return the updated UpdateRecord, or null if cancelled
	 */
	private UpdateRecord showUpdateDialog(UpdateRecord existing){
		JDialog dialog = new JDialog(this, existing == null? "Add Update": "Edit Update", true);
		dialog.setLayout(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]"));
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JTextField dateField = new JTextField(15);
		JTextField noteDisplayField = new JTextField(20);
		noteDisplayField.setEditable(false);
		noteDisplayField.setBackground(UIManager.getColor("TextField.background"));

		if(existing != null){
			dateField.setText(existing.date);
			if(existing.noteId != null && !existing.noteId.isEmpty()){
				String display = getNoteDisplayName(existing.noteId);
				noteDisplayField.setText(display);
			}
		}

		dialog.add(new JLabel("Date:"), "align label");
		dialog.add(dateField, "growx,wrap");

		// Note: 0:1 reference
		dialog.add(new JLabel("Note (optional):"), "align label");
		JPanel notePanel = new JPanel(new BorderLayout(5, 5));
		notePanel.add(noteDisplayField, BorderLayout.CENTER);
		JButton browseBtn = new JButton("Browse...");
		JButton clearBtn = new JButton("Clear");
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		btnPanel.add(browseBtn);
		btnPanel.add(clearBtn);
		notePanel.add(btnPanel, BorderLayout.EAST);
		dialog.add(notePanel, "growx,wrap");

		// Store the selected note ID
		final String[] selectedNoteId = {existing != null? existing.noteId: null};

		browseBtn.addActionListener(e -> {
			if(noteHandler == null){
				JOptionPane.showMessageDialog(dialog, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			GenericSelectionDialog<?> selDialog = new GenericSelectionDialog<>(
				parentFrame, model, noteHandler, selectedId -> {
				if(selectedId != null){
					selectedNoteId[0] = selectedId;
					String display = getNoteDisplayName(selectedId);
					noteDisplayField.setText(display);
				}
			}
			);
			selDialog.setVisible(true);
		});

		clearBtn.addActionListener(e -> {
			selectedNoteId[0] = null;
			noteDisplayField.setText("");
		});

		JPanel btnPanelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanelBottom.add(okBtn);
		btnPanelBottom.add(cancelBtn);
		dialog.add(btnPanelBottom, "span 2,growx");

		final UpdateRecord[] result = {null};
		okBtn.addActionListener(e -> {
			String date = dateField.getText().trim();
			if(date.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Date is required for an update.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			result[0] = new UpdateRecord(date, selectedNoteId[0]);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setMinimumSize(new Dimension(400, 200));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		return result[0];
	}

	// ==================== Load Data ====================

	private void loadData(){
		// Load CERTAINTY & CREDIBILITY
		String certainty = FLEFRecordUtils.getChildValue(existingLink, "CERTAINTY");
		String credibility = FLEFRecordUtils.getChildValue(existingLink, "CREDIBILITY");
		qualifiersPanel.load(certainty, credibility);

		// Load NOTE references
		loadNotes();

		// Load CONCLUSION
		FLEFRecord conclusion = FLEFRecordUtils.findChild(existingLink, "CONCLUSION");
		if(conclusion != null){
			// CONTEXT (1:1)
			conclusionContextField.setText(FLEFRecordUtils.getChildValue(conclusion, "CONTEXT"));

			// RESOLVES (0:M)
			resolvesModel.clear();
			resolvesIds.clear();
			for(FLEFRecord child : conclusion.getChildren()){
				if("RESOLVES".equals(child.getTag()) && child.getValue() != null){
					resolvesIds.add(child.getValue());
					resolvesModel.addElement(child.getValue());
				}
			}

			// PREFERRED (0:1)
			String preferred = FLEFRecordUtils.getChildValue(conclusion, "PREFERRED");
			preferredField.setText(preferred != null? preferred: "");

			// PROOF_STATUS (1:1)
			String proofStatus = FLEFRecordUtils.getChildValue(conclusion, "PROOF_STATUS");
			proofStatusCombo.setSelectedItem(proofStatus != null? proofStatus: "");

			// NARRATIVE (0:1)
			narrativeArea.setText(FLEFRecordUtils.getChildValue(conclusion, "NARRATIVE"));

			// NARRATIVE -> NOTE (0:M)
			loadNarrativeNotes();

			// DATE (0:1)
			conclusionDateField.setText(FLEFRecordUtils.getChildValue(conclusion, "DATE"));

			// SOURCE_CITATION (0:M)
			loadConclusionSources();

			// NOTE (0:M)
			loadConclusionNotes();

			// ===== Load MODIFICATION (under CONCLUSION) =====
			FLEFRecord modification = FLEFRecordUtils.findChild(conclusion, "MODIFICATION");
			if(modification != null){
				// CREATION (1:1)
				FLEFRecord creation = FLEFRecordUtils.findChild(modification, "CREATION");
				if(creation != null){
					creationDateField.setText(FLEFRecordUtils.getChildValue(creation, "DATE"));
				}

				// UPDATE (0:M)
				updateModel.clear();
				updateRecords.clear();
				for(FLEFRecord updateChild : modification.getChildren()){
					if("UPDATE".equals(updateChild.getTag())){
						String date = FLEFRecordUtils.getChildValue(updateChild, "DATE");
						String noteId = null;
						FLEFRecord noteChild = FLEFRecordUtils.findChild(updateChild, "NOTE");
						if(noteChild != null){
							noteId = noteChild.getValue();
						}
						if(date != null){
							UpdateRecord rec = new UpdateRecord(date, noteId);
							updateRecords.add(rec);
							updateModel.addElement(rec.toString());
						}
					}
				}
			}
		}
	}

	// ==================== Save Data ====================

	/**
	 * Returns the complete FLEFRecord for the link with all data.
	 */
	public FLEFRecord getLinkRecord(){
		FLEFRecord record = existingLink != null? existingLink: new FLEFRecord();
		if(existingLink == null){
			record.setLevel(1);
			record.setTag(linkType);
			record.setValue(familyId);
		}

		// CERTAINTY & CREDIBILITY
		String certainty = qualifiersPanel.getCertainty();
		if(certainty != null && !certainty.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "CERTAINTY", certainty);
		}
		String credibility = qualifiersPanel.getCredibility();
		if(credibility != null && !credibility.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "CREDIBILITY", credibility);
		}

		// NOTE references (direct children)
		FLEFRecordUtils.removeChildren(record, "NOTE");
		for(String noteId : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 2, noteId);
		}

		// ----- CONCLUSION -----
		FLEFRecordUtils.removeChildren(record, "CONCLUSION");

		String context = conclusionContextField.getText().trim();
		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		String narrative = narrativeArea.getText().trim();
		String conclusionDate = conclusionDateField.getText().trim();
		String preferred = preferredField.getText().trim();

		if(!context.isEmpty() || !Objects.requireNonNull(proofStatus).isEmpty() || !narrative.isEmpty() ||
			!conclusionDate.isEmpty() || !preferred.isEmpty() ||
			!resolvesIds.isEmpty() || !conclusionSourceIds.isEmpty() ||
			!conclusionNoteIds.isEmpty() || !narrativeNoteIds.isEmpty() ||
			!creationDateField.getText().trim().isEmpty() || !updateRecords.isEmpty()){

			FLEFRecord conclusion = new FLEFRecord();
			conclusion.setLevel(2);
			conclusion.setTag("CONCLUSION");
			record.addChild(conclusion);

			// --- CONCLUSION fields ---
			FLEFRecordUtils.updateChildValue(conclusion, "CONTEXT", context);

			for(String resolveId : resolvesIds){
				FLEFRecordUtils.addChild(conclusion, "RESOLVES", 3, resolveId);
			}

			FLEFRecordUtils.updateChildValue(conclusion, "PREFERRED", preferred);
			FLEFRecordUtils.updateChildValue(conclusion, "PROOF_STATUS", proofStatus);

			// NARRATIVE with its NOTES
			if(!narrative.isEmpty() || !narrativeNoteIds.isEmpty()){
				FLEFRecord narrativeRecord = new FLEFRecord();
				narrativeRecord.setLevel(3);
				narrativeRecord.setTag("NARRATIVE");
				narrativeRecord.setValue(narrative);
				conclusion.addChild(narrativeRecord);
				for(String noteId : narrativeNoteIds){
					FLEFRecordUtils.addChild(narrativeRecord, "NOTE", 4, noteId);
				}
			}

			FLEFRecordUtils.updateChildValue(conclusion, "DATE", conclusionDate);

			for(String sourceId : conclusionSourceIds){
				FLEFRecordUtils.addChild(conclusion, "SOURCE_CITATION", 3, sourceId);
			}
			for(String noteId : conclusionNoteIds){
				FLEFRecordUtils.addChild(conclusion, "NOTE", 3, noteId);
			}

			// ----- MODIFICATION (under CONCLUSION) -----
			String creationDate = creationDateField.getText().trim();
			if(!creationDate.isEmpty() || !updateRecords.isEmpty()){
				FLEFRecord modification = new FLEFRecord();
				modification.setLevel(2);
				modification.setTag("MODIFICATION");
				conclusion.addChild(modification);

				// CREATION (1:1)
				if(!creationDate.isEmpty()){
					FLEFRecord creation = new FLEFRecord();
					creation.setLevel(3);
					creation.setTag("CREATION");
					modification.addChild(creation);
					FLEFRecordUtils.updateChildValue(creation, "DATE", creationDate);
				}

				// UPDATE (0:M)
				for(UpdateRecord upd : updateRecords){
					if(upd.date != null && !upd.date.isEmpty()){
						FLEFRecord updateRec = new FLEFRecord();
						updateRec.setLevel(3);
						updateRec.setTag("UPDATE");
						modification.addChild(updateRec);
						FLEFRecordUtils.updateChildValue(updateRec, "DATE", upd.date);
						if(upd.noteId != null && !upd.noteId.isEmpty()){
							FLEFRecordUtils.addChild(updateRec, "NOTE", 4, upd.noteId);
						}
					}
				}
			}
		}

		return record;
	}

	public boolean isSaved(){
		return saved;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		// Create a minimal model with required handlers
		FLEFModel model = new FLEFModel();
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());

		SwingUtilities.invokeLater(() -> {
			JDialog parent = new JDialog();
			parent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			FamilyLinkDialog dialog = new FamilyLinkDialog(parent, model, "F1", "FAMILY_CHILD", null);
			dialog.setVisible(true);
			if(dialog.isSaved()){
				FLEFRecord record = dialog.getLinkRecord();
				System.out.println("Saved link: " + record);
			}
			System.exit(0);
		});
	}

}
