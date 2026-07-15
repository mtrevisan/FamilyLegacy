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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
 * Reusable panel for editing a CONCLUSION_STRUCTURE.
 * <p>
 * Structure:
 * <pre>
 * CONCLUSION_STRUCTURE :=
 *   +1 CONTEXT <RESOLUTION_CONTEXT>    {1:1}
 *   +1 RESOLVES @<XREF:EVENT>@    {0:M}
 *   +1 PREFERRED @<XREF:EVENT>@    {0:1}
 *   +1 PROOF_STATUS <PROOF_STATUS_VALUE>    {1:1}
 *   +1 NARRATIVE <PROOF_NARRATIVE_TEXT>    {0:1}
 *     +2 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 DATE <CONCLUSION_DATE>    {0:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class ConclusionPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -6705559964442556599L;

	private final FLEFModel model;
	private final Component parent;
	private final ModificationPanel modificationPanel;

	private final JTextField contextField = new JTextField(15);
	private final DefaultListModel<String> resolvesModel = new DefaultListModel<>();
	private final JList<String> resolvesList = new JList<>(resolvesModel);
	private final List<String> resolvesIds = new ArrayList<>();
	private final JTextField preferredField = new JTextField(15);
	private final JButton setPreferredBtn = new JButton("Set from Resolves");
	private final JComboBox<String> proofStatusCombo = new JComboBox<>(new String[]{
		"", "unresearched", "conflicting_evidence", "preponderance_of_evidence",
		"proven", "disproven"
	});
	private final JTextArea narrativeArea = new JTextArea(3, 20);
	private final DefaultListModel<String> narrativeNoteModel = new DefaultListModel<>();
	private final JList<String> narrativeNoteList = new JList<>(narrativeNoteModel);
	private final List<String> narrativeNoteIds = new ArrayList<>();
	private final Map<String, String> narrativeNoteDisplayMap = new HashMap<>();
	private final JTextField dateField = new JTextField(15);
	private final DefaultListModel<String> sourceModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceModel);
	private final List<String> sourceIds = new ArrayList<>();
	private final Map<String, String> sourceDisplayMap = new HashMap<>();
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");


	public ConclusionPanel(FLEFModel model, Component parent){
		this.model = model;
		this.parent = parent;
		this.modificationPanel = new ModificationPanel(model, parent);
		initComponents();
	}


	private void initComponents(){
		setLayout(new BorderLayout());

		JTabbedPane tabbedPane = new JTabbedPane();

		// ==================== Main Tab (fields up to Date) ====================
		JPanel mainPanel = new JPanel(new MigLayout("ins 5", "[right]rel[grow]", "[]5[]5[]5[]5[]5[]5[]"));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// CONTEXT
		mainPanel.add(new JLabel("Context:"), "align label");
		mainPanel.add(contextField, "growx,wrap");

		// RESOLVES - with popup menu and keyboard shortcuts
		mainPanel.add(new JLabel("Resolves (Events):"), "align label,top");
		JPanel resolvesPanel = createPopupListPanel(resolvesList, resolvesModel,
			this::addResolves, this::editResolves, this::removeResolves,
			this::createNewResolves);
		mainPanel.add(resolvesPanel, "growx,wrap");

		// PREFERRED
		mainPanel.add(new JLabel("Preferred Event:"), "align label");
		preferredField.setEditable(false);
		preferredField.setBackground(UIManager.getColor("TextField.background"));
		JPanel preferredPanel = new JPanel(new BorderLayout(5, 5));
		preferredPanel.add(preferredField, BorderLayout.CENTER);
		preferredPanel.add(setPreferredBtn, BorderLayout.EAST);
		mainPanel.add(preferredPanel, "growx,wrap");
		setPreferredBtn.addActionListener(e -> setPreferredFromResolves());

		// PROOF_STATUS
		mainPanel.add(new JLabel("Proof Status:"), "align label");
		mainPanel.add(proofStatusCombo, "growx,wrap");

		// NARRATIVE
		mainPanel.add(new JLabel("Narrative:"), "align label,top");
		JScrollPane narrScroll = new JScrollPane(narrativeArea);
		narrScroll.setPreferredSize(new Dimension(200, 60));
		mainPanel.add(narrScroll, "growx,wrap");

		// NARRATIVE NOTES
		mainPanel.add(new JLabel("Narrative Notes:"), "align label,top");
		JPanel narrativeNotePanel = createPopupListPanel("Narrative Note References",
			narrativeNoteList, narrativeNoteModel,
			this::addNarrativeNote, this::editNarrativeNote, this::removeNarrativeNote,
			this::createNewNarrativeNote);
		mainPanel.add(narrativeNotePanel, "growx,wrap");

		// DATE
		mainPanel.add(new JLabel("Date:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		tabbedPane.addTab("Main", mainPanel);

		// ==================== References Tab ====================
		JPanel referencesPanel = new JPanel(new MigLayout("ins 5", "[right]rel[grow]", "[]5[]5[]5[]"));
		referencesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// SOURCE CITATIONS
		referencesPanel.add(new JLabel("Source Citations:"), "align label,top");
		JPanel sourcePanel = createPopupListPanel("Source References",
			sourceList, sourceModel,
			this::addSource, this::editSource, this::removeSource,
			this::createNewSource);
		referencesPanel.add(sourcePanel, "growx,wrap");

		// NOTES
		referencesPanel.add(new JLabel("Notes:"), "align label,top");
		JPanel notePanel = createPopupListPanel("Note References",
			noteList, noteModel,
			this::addNote, this::editNote, this::removeNote,
			this::createNewNote);
		referencesPanel.add(notePanel, "growx,wrap");

		// MODIFICATION
		referencesPanel.add(new JLabel("Modification:"), "align label,top");
		referencesPanel.add(modificationPanel, "growx,wrap");

		tabbedPane.addTab("References", referencesPanel);

		add(tabbedPane, BorderLayout.CENTER);
	}


	// ==================== Generic popup list panel (no buttons) ====================
	private JPanel createPopupListPanel(String title, JList<String> list, DefaultListModel<String> model,
		Runnable addAction, Runnable editAction, Runnable deleteAction,
		Runnable newAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

		// Popup menu
		JPopupMenu popup = new JPopupMenu();
		JMenuItem addItem = new JMenuItem("Add Existing...");
		JMenuItem newItem = new JMenuItem("Create New...");
		JMenuItem editItem = new JMenuItem("Edit");
		JMenuItem deleteItem = new JMenuItem("Delete");
		popup.add(addItem);
		popup.add(newItem);
		popup.addSeparator();
		popup.add(editItem);
		popup.add(deleteItem);

		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});

		// Keyboard shortcuts: Ins = New, Canc = Delete
		list.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					newAction.run();
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteAction.run();
					e.consume();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(list.getPreferredScrollableViewportSize());
		panel.add(scrollPane, "growx,wrap");

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editItem.setEnabled(selected);
			deleteItem.setEnabled(selected);
		});
		editItem.setEnabled(false);
		deleteItem.setEnabled(false);

		addItem.addActionListener(e -> addAction.run());
		newItem.addActionListener(e -> newAction.run());
		editItem.addActionListener(e -> editAction.run());
		deleteItem.addActionListener(e -> deleteAction.run());

		return panel;
	}


	// Overloaded for Resolves panel (no title, as title is outside)
	private JPanel createPopupListPanel(JList<String> list, DefaultListModel<String> model,
		Runnable addAction, Runnable editAction, Runnable deleteAction,
		Runnable newAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		list.setVisibleRowCount(4);
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

		JPopupMenu popup = new JPopupMenu();
		JMenuItem addItem = new JMenuItem("Add Existing...");
		JMenuItem newItem = new JMenuItem("Create New...");
		JMenuItem editItem = new JMenuItem("Edit");
		JMenuItem deleteItem = new JMenuItem("Delete");
		popup.add(addItem);
		popup.add(newItem);
		popup.addSeparator();
		popup.add(editItem);
		popup.add(deleteItem);

		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});

		list.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					newAction.run();
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteAction.run();
					e.consume();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(list.getPreferredScrollableViewportSize());
		panel.add(scrollPane, "growx,wrap");

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editItem.setEnabled(selected);
			deleteItem.setEnabled(selected);
		});
		editItem.setEnabled(false);
		deleteItem.setEnabled(false);

		addItem.addActionListener(e -> addAction.run());
		newItem.addActionListener(e -> newAction.run());
		editItem.addActionListener(e -> editAction.run());
		deleteItem.addActionListener(e -> deleteAction.run());

		return panel;
	}


	// ==================== RESOLVES methods ====================

	private void addResolves(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(parent, "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, eventHandler, selectedId -> {
			if(selectedId != null && !resolvesIds.contains(selectedId)){
				resolvesIds.add(selectedId);
				resolvesModel.addElement(selectedId);
			}
		});
		dialog.setVisible(true);
	}

	private void editResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1) return;
		String currentId = resolvesIds.get(idx);
		String newId = (String)JOptionPane.showInputDialog(
			parent,
			"Enter new Event ID:",
			"Edit Resolves",
			JOptionPane.PLAIN_MESSAGE,
			null,
			null,
			currentId
		);
		if(newId == null || newId.trim().isEmpty()) return;
		String trimmed = newId.trim();
		if(!trimmed.equals(currentId) && resolvesIds.contains(trimmed)){
			JOptionPane.showMessageDialog(parent, "ID already in use.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return;
		}
		String preferredId = preferredField.getText().trim();
		if(preferredId.equals(currentId)){
			preferredField.setText(trimmed);
		}
		resolvesIds.set(idx, trimmed);
		resolvesModel.set(idx, trimmed);
	}

	private void removeResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1) return;
		String removedId = resolvesIds.get(idx);
		int confirm = JOptionPane.showConfirmDialog(parent, "Remove this event reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String preferredId = preferredField.getText().trim();
			if(preferredId.equals(removedId)){
				preferredField.setText("");
			}
			resolvesIds.remove(idx);
			resolvesModel.remove(idx);
		}
	}

	private void createNewResolves(){
		// For Resolves, "Create New" means create a new Event and add it
		if(eventHandler == null){
			JOptionPane.showMessageDialog(parent, "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("EVENT")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = eventHandler.createNewDialog((parent instanceof Frame? (Frame)parent: null), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("EVENT")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty() && !resolvesIds.contains(newId)){
			resolvesIds.add(newId);
			resolvesModel.addElement(newId);
		}
	}

	private void setPreferredFromResolves(){
		int idx = resolvesList.getSelectedIndex();
		if(idx == -1){
			JOptionPane.showMessageDialog(parent, "Please select an event from the Resolves list first.",
				"No Selection", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String selectedId = resolvesIds.get(idx);
		preferredField.setText(selectedId);
	}


	// ==================== Narrative Note methods ====================

	private String getNarrativeNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return noteHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void addNarrativeNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, noteHandler, selectedId -> {
			if(selectedId != null && !narrativeNoteIds.contains(selectedId)){
				narrativeNoteIds.add(selectedId);
				String display = getNarrativeNoteDisplayName(selectedId);
				narrativeNoteDisplayMap.put(selectedId, display);
				narrativeNoteModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNarrativeNote(){
		int idx = narrativeNoteList.getSelectedIndex();
		if(idx == -1) return;
		String id = narrativeNoteIds.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog((parent instanceof Frame? (Frame)parent: null), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNarrativeNoteDisplayName(id);
		narrativeNoteDisplayMap.put(id, newDisplay);
		narrativeNoteModel.set(idx, newDisplay);
	}

	private void removeNarrativeNote(){
		int idx = narrativeNoteList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(parent, "Remove this narrative note?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = narrativeNoteIds.remove(idx);
			narrativeNoteDisplayMap.remove(removedId);
			narrativeNoteModel.remove(idx);
		}
	}

	private void createNewNarrativeNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(narrativeNoteIds);
		JDialog dialog = noteHandler.createNewDialog((parent instanceof Frame? (Frame)parent: null), model);
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


	// ==================== Source Citation methods ====================

	private String getSourceDisplayName(String id){
		if(sourceHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return sourceHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void addSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(parent, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, sourceHandler, selectedId -> {
			if(selectedId != null && !sourceIds.contains(selectedId)){
				sourceIds.add(selectedId);
				String display = getSourceDisplayName(selectedId);
				sourceDisplayMap.put(selectedId, display);
				sourceModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;
		String id = sourceIds.get(idx);
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(parent, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Source not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = sourceHandler.createEditDialog((parent instanceof Frame? (Frame)parent: null), model, rec);
		dialog.setVisible(true);
		String newDisplay = getSourceDisplayName(id);
		sourceDisplayMap.put(id, newDisplay);
		sourceModel.set(idx, newDisplay);
	}

	private void removeSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(parent, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = sourceIds.remove(idx);
			sourceDisplayMap.remove(removedId);
			sourceModel.remove(idx);
		}
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(parent, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(sourceIds);
		JDialog dialog = sourceHandler.createNewDialog((parent instanceof Frame? (Frame)parent: null), model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !sourceIds.contains(id)){
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourceModel.addElement(display);
				break;
			}
		}
	}


	// ==================== Note methods ====================

	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return noteHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		String id = noteIds.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog((parent instanceof Frame? (Frame)parent: null), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void removeNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(parent, "Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteModel.remove(idx);
		}
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog((parent instanceof Frame? (Frame)parent: null), model);
		dialog.setVisible(true);
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


	// ==================== Public API ====================

	public void loadFromRecord(FLEFRecord conclusionRecord){
		contextField.setText("");
		resolvesModel.clear();
		resolvesIds.clear();
		preferredField.setText("");
		proofStatusCombo.setSelectedItem("");
		narrativeArea.setText("");
		narrativeNoteModel.clear();
		narrativeNoteIds.clear();
		narrativeNoteDisplayMap.clear();
		dateField.setText("");
		sourceModel.clear();
		sourceIds.clear();
		sourceDisplayMap.clear();
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		modificationPanel.clear();

		if(conclusionRecord == null){
			return;
		}

		contextField.setText(FLEFRecordUtils.getChildValue(conclusionRecord, "CONTEXT"));

		for(FLEFRecord child : conclusionRecord.getChildren()){
			if("RESOLVES".equals(child.getTag()) && child.getValue() != null){
				resolvesIds.add(child.getValue());
				resolvesModel.addElement(child.getValue());
			}
		}

		String preferred = FLEFRecordUtils.getChildValue(conclusionRecord, "PREFERRED");
		preferredField.setText(preferred != null? preferred: "");

		String proofStatus = FLEFRecordUtils.getChildValue(conclusionRecord, "PROOF_STATUS");
		proofStatusCombo.setSelectedItem(proofStatus != null? proofStatus: "");

		narrativeArea.setText(FLEFRecordUtils.getChildValue(conclusionRecord, "NARRATIVE"));

		FLEFRecord narrative = FLEFRecordUtils.findChild(conclusionRecord, "NARRATIVE");
		if(narrative != null){
			for(FLEFRecord child : narrative.getChildren()){
				if("NOTE".equals(child.getTag()) && child.getValue() != null){
					String id = child.getValue();
					narrativeNoteIds.add(id);
					String display = getNarrativeNoteDisplayName(id);
					narrativeNoteDisplayMap.put(id, display);
					narrativeNoteModel.addElement(display);
				}
			}
		}

		dateField.setText(FLEFRecordUtils.getChildValue(conclusionRecord, "DATE"));

		for(FLEFRecord child : conclusionRecord.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourceModel.addElement(display);
			}
		}

		for(FLEFRecord child : conclusionRecord.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}

		modificationPanel.loadFromRecord(conclusionRecord);
	}

	public FLEFRecord saveToRecord(FLEFRecord conclusionRecord){
		if(!validateRequiredFields()){
			return null;
		}

		if(conclusionRecord == null){
			conclusionRecord = new FLEFRecord();
			conclusionRecord.setLevel(2);
			conclusionRecord.setTag("CONCLUSION");
		}

		conclusionRecord.getChildren().clear();

		String context = contextField.getText().trim();
		if(!context.isEmpty()){
			FLEFRecordUtils.updateChildValue(conclusionRecord, "CONTEXT", context);
		}

		for(String id : resolvesIds){
			FLEFRecordUtils.addChild(conclusionRecord, "RESOLVES", 3, id);
		}

		String preferred = preferredField.getText().trim();
		if(!preferred.isEmpty()){
			FLEFRecordUtils.updateChildValue(conclusionRecord, "PREFERRED", preferred);
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(proofStatus != null && !proofStatus.isEmpty()){
			FLEFRecordUtils.updateChildValue(conclusionRecord, "PROOF_STATUS", proofStatus);
		}

		String narrative = narrativeArea.getText().trim();
		if(!narrative.isEmpty() || !narrativeNoteIds.isEmpty()){
			FLEFRecord narrativeRecord = FLEFRecord.createChildWithValue(3, "NARRATIVE", narrative);
			conclusionRecord.addChild(narrativeRecord);
			for(String id : narrativeNoteIds){
				FLEFRecordUtils.addChild(narrativeRecord, "NOTE", 4, id);
			}
		}

		String date = dateField.getText().trim();
		if(!date.isEmpty()){
			FLEFRecordUtils.updateChildValue(conclusionRecord, "DATE", date);
		}

		for(String id : sourceIds){
			FLEFRecordUtils.addChild(conclusionRecord, "SOURCE_CITATION", 3, id);
		}

		for(String id : noteIds){
			FLEFRecordUtils.addChild(conclusionRecord, "NOTE", 3, id);
		}

		modificationPanel.saveToRecord(conclusionRecord);

		return conclusionRecord;
	}

	public boolean validateRequiredFields(){
		if(!hasData()){
			return true;
		}

		if(contextField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"CONTEXT is required for a conclusion.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			contextField.requestFocusInWindow();
			return false;
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(proofStatus == null || proofStatus.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				"PROOF_STATUS is required for a conclusion.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			proofStatusCombo.requestFocusInWindow();
			return false;
		}

		return true;
	}

	public boolean hasData(){
		return !contextField.getText().trim().isEmpty() ||
			!resolvesModel.isEmpty() ||
			!preferredField.getText().trim().isEmpty() ||
			(proofStatusCombo.getSelectedItem() != null &&
				!((String)proofStatusCombo.getSelectedItem()).isEmpty()) ||
			!narrativeArea.getText().trim().isEmpty() ||
			!narrativeNoteModel.isEmpty() ||
			!dateField.getText().trim().isEmpty() ||
			!sourceModel.isEmpty() ||
			!noteModel.isEmpty() ||
			modificationPanel.hasData();
	}

	public void clear(){
		contextField.setText("");
		resolvesModel.clear();
		resolvesIds.clear();
		preferredField.setText("");
		proofStatusCombo.setSelectedItem("");
		narrativeArea.setText("");
		narrativeNoteModel.clear();
		narrativeNoteIds.clear();
		narrativeNoteDisplayMap.clear();
		dateField.setText("");
		sourceModel.clear();
		sourceIds.clear();
		sourceDisplayMap.clear();
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		modificationPanel.clear();
	}

}
