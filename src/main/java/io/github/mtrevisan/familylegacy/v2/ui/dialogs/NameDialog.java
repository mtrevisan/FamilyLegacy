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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.util.Set;


/**
 * Dialog for editing a {@code PERSONAL_NAME_STRUCTURE} according to FLEF 0.0.9.
 * Uses BoxLayout for complete collapse of hidden panels.
 * ZERO vertical gaps when all transcription panels are hidden.
 * Transcription lists show at least 3 rows and have a "Transcriptions" title.
 */
public class NameDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 5856181538971193737L;


	private final Frame parentFrame;

	private final FLEFModel model;
	private final FLEFRecord nameRecord;
	private boolean saved = false;

	// Main fields
	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		StringUtils.EMPTY, "birth", "aka", "nickname", "immigrant", "legal",
		"married", "adoption", "fostering", "religious"
	});
	private final JTextField titleField = new JTextField(20);
	private final JTextField givenNameField = new JTextField(20);
	private final JTextField suffixField = new JTextField(10);
	private final JTextField nicknameField = new JTextField(20);
	private final JTextField familyNameField = new JTextField(20);
	private final JTextField familyNicknameField = new JTextField(20);

	// Transcription lists (inline accordion)
	private final DefaultListModel<String> titleTransModel = new DefaultListModel<>();
	private final List<FLEFRecord> titleTransRecords = new ArrayList<>();
	private final DefaultListModel<String> givenTransModel = new DefaultListModel<>();
	private final List<FLEFRecord> givenTransRecords = new ArrayList<>();
	private final DefaultListModel<String> suffixTransModel = new DefaultListModel<>();
	private final List<FLEFRecord> suffixTransRecords = new ArrayList<>();
	private final DefaultListModel<String> nicknameTransModel = new DefaultListModel<>();
	private final List<FLEFRecord> nicknameTransRecords = new ArrayList<>();
	private final DefaultListModel<String> familyTransModel = new DefaultListModel<>();
	private final List<FLEFRecord> familyTransRecords = new ArrayList<>();
	private final DefaultListModel<String> familyNicknameTransModel = new DefaultListModel<>();
	private final List<FLEFRecord> familyNicknameTransRecords = new ArrayList<>();

	// References lists
	private final DefaultListModel<String> culturalNormsModel = new DefaultListModel<>();
	private final JList<String> culturalNormsList = new JList<>(culturalNormsModel);
	private final List<String> culturalNormIds = new ArrayList<>();
	private final Map<String, String> culturalNormDisplayMap = new HashMap<>();

	private final DefaultListModel<String> notesModel = new DefaultListModel<>();
	private final JList<String> notesList = new JList<>(notesModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final DefaultListModel<String> sourcesModel = new DefaultListModel<>();
	private final JList<String> sourcesList = new JList<>(sourcesModel);
	private final List<String> sourceIds = new ArrayList<>();
	private final Map<String, String> sourceDisplayMap = new HashMap<>();

	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	// Lazy-initialized list components
	private JList<String> titleList;
	private JList<String> givenList;
	private JList<String> suffixList;
	private JList<String> nicknameList;
	private JList<String> familyList;
	private JList<String> familyNicknameList;

	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	public NameDialog(JDialog parent, FLEFModel model, FLEFRecord nameRecord){
		super(parent, "Edit Name", true);

		this.model = model;
		this.parentFrame = getParentFrame(parent);
		this.nameRecord = nameRecord != null? nameRecord: FLEFRecord.createEmpty();
		initComponents();
		if(nameRecord != null && nameRecord.getChildren() != null){
			loadData();
		}
		pack();
		setMinimumSize(new Dimension(650, 500));
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

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		// Type (no transcriptions)
		JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		typeRow.add(new JLabel("Type:"));
		typeRow.add(typeCombo);
		mainPanel.add(typeRow);

		// Title row
		mainPanel.add(createFieldWithTranscriptions("Title:", titleField,
			titleTransModel, titleTransRecords,
			this::addTitleTranscription, this::editTitleTranscription, this::deleteTitleTranscription));

		// Given Name row
		mainPanel.add(createFieldWithTranscriptions("Given Name:", givenNameField,
			givenTransModel, givenTransRecords,
			this::addGivenTranscription, this::editGivenTranscription, this::deleteGivenTranscription));

		// Suffix row
		mainPanel.add(createFieldWithTranscriptions("Suffix:", suffixField,
			suffixTransModel, suffixTransRecords,
			this::addSuffixTranscription, this::editSuffixTranscription, this::deleteSuffixTranscription));

		// Nickname row
		mainPanel.add(createFieldWithTranscriptions("Nickname:", nicknameField,
			nicknameTransModel, nicknameTransRecords,
			this::addNicknameTranscription, this::editNicknameTranscription, this::deleteNicknameTranscription));

		// Family Name row
		mainPanel.add(createFieldWithTranscriptions("Family Name:", familyNameField,
			familyTransModel, familyTransRecords,
			this::addFamilyTranscription, this::editFamilyTranscription, this::deleteFamilyTranscription));

		// Family Nickname row
		mainPanel.add(createFieldWithTranscriptions("Family Nickname:", familyNicknameField,
			familyNicknameTransModel, familyNicknameTransRecords,
			this::addFamilyNicknameTranscription, this::editFamilyNicknameTranscription,
			this::deleteFamilyNicknameTranscription));

		tabbedPane.addTab("Main", mainPanel);

		JPanel refPanel = new JPanel(new MigLayout("ins 5", "[grow]", "[]10[]10[]"));

		// Cultural Norms
		refPanel.add(createReferencePanel("Cultural Norms", culturalNormsModel, culturalNormsList,
			culturalNormIds, culturalNormDisplayMap,
			this::addCulturalNorm, this::editCulturalNorm, this::deleteCulturalNorm,
			this::createNewCulturalNorm), "growx");

		// Notes
		refPanel.add(createReferencePanel("Notes", notesModel, notesList,
			noteIds, noteDisplayMap,
			this::addNote, this::editNote, this::deleteNote,
			this::createNewNote), "growx");

		// Sources
		refPanel.add(createReferencePanel("Sources", sourcesModel, sourcesList,
			sourceIds, sourceDisplayMap,
			this::addSource, this::editSource, this::deleteSource,
			this::createNewSource), "growx");

		tabbedPane.addTab("References", refPanel);

		add(tabbedPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> {
			saveData();
			saved = true;
			dispose();
		});
		cancelButton.addActionListener(e -> dispose());
	}


	private JPanel createFieldWithTranscriptions(String label, JTextField field,
		DefaultListModel<String> listModel,
		List<FLEFRecord> records,
		Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		top.add(new JLabel(label));
		top.add(field);

		JButton toggleBtn = new JButton("📝");
		toggleBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
		toggleBtn.setToolTipText("Show/hide transcriptions");
		toggleBtn.setFocusable(false);
		toggleBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		top.add(toggleBtn);
		row.add(top);

		JPanel transPanel = new JPanel(new BorderLayout(2, 2));
		transPanel.setBorder(BorderFactory.createTitledBorder("Transcriptions"));
		transPanel.setVisible(false);

		JList<String> list = new JList<>(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(3);
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});

		if(label.startsWith("Title")) titleList = list;
		else if(label.startsWith("Given")) givenList = list;
		else if(label.startsWith("Suffix")) suffixList = list;
		else if(label.startsWith("Nickname") && !label.startsWith("Family Nickname")) nicknameList = list;
		else if(label.startsWith("Family Name")) familyList = list;
		else if(label.startsWith("Family Nickname")) familyNicknameList = list;

		JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		transPanel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		transPanel.add(btnPanel, BorderLayout.SOUTH);

		addBtn.addActionListener(e -> addAction.run());
		editBtn.addActionListener(e -> editAction.run());
		deleteBtn.addActionListener(e -> deleteAction.run());

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		toggleBtn.addActionListener(e -> {
			boolean visible = transPanel.isVisible();
			transPanel.setVisible(!visible);
			toggleBtn.setText(visible? "📝": "📝⬆");
			SwingUtilities.invokeLater(() -> {
				Window win = SwingUtilities.getWindowAncestor(row);
				if(win != null){
					win.pack();
				}
			});
		});

		row.add(transPanel);
		return row;
	}


	private JPanel createReferencePanel(String title, DefaultListModel<String> model, JList<String> list,
		List<String> ids, Map<String, String> displayMap,
		Runnable addAction, Runnable editAction, Runnable deleteAction,
		Runnable newAction){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder(title));

		JScrollPane scrollPane = GUIHelper.createScrollPane(list);
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
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addAction.run());
		newBtn.addActionListener(e -> newAction.run());
		editBtn.addActionListener(e -> editAction.run());
		deleteBtn.addActionListener(e -> deleteAction.run());

		return panel;
	}


	private String getCulturalNormDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return culturalNormHandler.getDisplayText(rec);
		}
		return id;
	}

	private void loadCulturalNorms(){
		culturalNormsModel.clear();
		culturalNormIds.clear();
		culturalNormDisplayMap.clear();
		for(FLEFRecord child : nameRecord.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormsModel.addElement(display);
			}
		}
	}

	private void addCulturalNorm(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				String display = getCulturalNormDisplayName(selectedId);
				culturalNormDisplayMap.put(selectedId, display);
				culturalNormsModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editCulturalNorm(){
		int idx = culturalNormsList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = culturalNormIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Cultural norm not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = culturalNormHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);
		String newDisplay = getCulturalNormDisplayName(id);
		culturalNormDisplayMap.put(id, newDisplay);
		culturalNormsModel.set(idx, newDisplay);
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormsList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this cultural norm reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = culturalNormIds.remove(idx);
			culturalNormDisplayMap.remove(removedId);
			culturalNormsModel.remove(idx);
		}
	}

	/**
	 * Creates a new cultural norm record and automatically adds it to the list.
	 */
	private void createNewCulturalNorm(){
		// Remember the current IDs to detect the new one
		Set<String> beforeIds = new HashSet<>(culturalNormIds);

		JDialog dialog = culturalNormHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		// After the dialog closes, check if a new cultural norm was added
		List<FLEFRecord> allNorms = model.getRecordsByType("CULTURAL_NORM");
		for(FLEFRecord rec : allNorms){
			String id = rec.getId();
			if(id != null && !beforeIds.contains(id) && !culturalNormIds.contains(id)){
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormsModel.addElement(display);
				break;
			}
		}
	}


	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayText(rec);
		}
		return id;
	}

	private void loadNotes(){
		notesModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : nameRecord.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				notesModel.addElement(display);
			}
		}
	}

	private void addNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				notesModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = notesList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		notesModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = notesList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			notesModel.remove(idx);
		}
	}

	/**
	 * Creates a new note record and automatically adds it to the list.
	 */
	private void createNewNote(){
		// Remember the current IDs to detect the new one
		Set<String> beforeIds = new HashSet<>(noteIds);

		JDialog dialog = noteHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		// After the dialog closes, check if a new note was added
		List<FLEFRecord> allNotes = model.getRecordsByType("NOTE");
		for(FLEFRecord rec : allNotes){
			String id = rec.getId();
			if(id != null && !beforeIds.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				notesModel.addElement(display);
				break;
			}
		}
	}


	private String getSourceDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return sourceHandler.getDisplayText(rec);
		}
		return id;
	}

	private void loadSources(){
		sourcesModel.clear();
		sourceIds.clear();
		sourceDisplayMap.clear();
		for(FLEFRecord child : nameRecord.getChildren()){
			if("SOURCE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourcesModel.addElement(display);
			}
		}
	}

	private void addSource(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, sourceHandler, selectedId -> {
			if(selectedId != null && !sourceIds.contains(selectedId)){
				sourceIds.add(selectedId);
				String display = getSourceDisplayName(selectedId);
				sourceDisplayMap.put(selectedId, display);
				sourcesModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editSource(){
		int idx = sourcesList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = sourceIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Source not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = sourceHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);
		String newDisplay = getSourceDisplayName(id);
		sourceDisplayMap.put(id, newDisplay);
		sourcesModel.set(idx, newDisplay);
	}

	private void deleteSource(){
		int idx = sourcesList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = sourceIds.remove(idx);
			sourceDisplayMap.remove(removedId);
			sourcesModel.remove(idx);
		}
	}

	/**
	 * Creates a new source record and automatically adds it to the list.
	 */
	private void createNewSource(){
		// Remember the current IDs to detect the new one
		Set<String> beforeIds = new HashSet<>(sourceIds);

		JDialog dialog = sourceHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		// After the dialog closes, check if a new source was added
		List<FLEFRecord> allSources = model.getRecordsByType("SOURCE");
		for(FLEFRecord rec : allSources){
			String id = rec.getId();
			if(id != null && !beforeIds.contains(id) && !sourceIds.contains(id)){
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourcesModel.addElement(display);
				break;
			}
		}
	}


	private void addTranscription(DefaultListModel<String> model, List<FLEFRecord> records,
		String parentTag){
		TranscribedTextDialog dialog = new TranscribedTextDialog(this, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord transRecord = dialog.getTranscribedTextRecord();
			if(transRecord != null){
				transRecord.setTag("TRANSCRIBED_TEXT");
				FLEFRecord parent = FLEFRecordHelper.findChild(nameRecord, parentTag);
				if(parent == null){
					parent = FLEFRecord.createChild(parentTag);
					nameRecord.addChild(parent);
				}
				parent.addChild(transRecord);
				records.add(transRecord);
				model.addElement(buildTranscriptionDisplay(transRecord));
			}
		}
	}

	private void editTranscription(DefaultListModel<String> model, List<FLEFRecord> records,
		JList<String> list){
		int idx = list.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord transRecord = records.get(idx);
		TranscribedTextDialog dialog = new TranscribedTextDialog(this, transRecord);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			model.set(idx, buildTranscriptionDisplay(transRecord));
		}
	}

	private void deleteTranscription(DefaultListModel<String> model, List<FLEFRecord> records,
		JList<String> list, String parentTag){
		int idx = list.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this transcription?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			FLEFRecord transRecord = records.remove(idx);
			FLEFRecord parent = FLEFRecordHelper.findChild(nameRecord, parentTag);
			if(parent != null){
				parent.getChildren().remove(transRecord);
			}
			model.remove(idx);
		}
	}

	private void addTitleTranscription(){
		addTranscription(titleTransModel, titleTransRecords, "TITLE");
	}

	private void editTitleTranscription(){
		editTranscription(titleTransModel, titleTransRecords, titleList);
	}

	private void deleteTitleTranscription(){
		deleteTranscription(titleTransModel, titleTransRecords, titleList, "TITLE");
	}

	private void addGivenTranscription(){
		addTranscription(givenTransModel, givenTransRecords, "INDIVIDUAL_NAME");
	}

	private void editGivenTranscription(){
		editTranscription(givenTransModel, givenTransRecords, givenList);
	}

	private void deleteGivenTranscription(){
		deleteTranscription(givenTransModel, givenTransRecords, givenList, "INDIVIDUAL_NAME");
	}

	private void addSuffixTranscription(){
		addTranscription(suffixTransModel, suffixTransRecords, "SUFFIX");
	}

	private void editSuffixTranscription(){
		editTranscription(suffixTransModel, suffixTransRecords, suffixList);
	}

	private void deleteSuffixTranscription(){
		deleteTranscription(suffixTransModel, suffixTransRecords, suffixList, "SUFFIX");
	}

	private void addNicknameTranscription(){
		addTranscription(nicknameTransModel, nicknameTransRecords, "INDIVIDUAL_NICKNAME");
	}

	private void editNicknameTranscription(){
		editTranscription(nicknameTransModel, nicknameTransRecords, nicknameList);
	}

	private void deleteNicknameTranscription(){
		deleteTranscription(nicknameTransModel, nicknameTransRecords, nicknameList, "INDIVIDUAL_NICKNAME");
	}

	private void addFamilyTranscription(){
		addTranscription(familyTransModel, familyTransRecords, "FAMILY_NAME");
	}

	private void editFamilyTranscription(){
		editTranscription(familyTransModel, familyTransRecords, familyList);
	}

	private void deleteFamilyTranscription(){
		deleteTranscription(familyTransModel, familyTransRecords, familyList, "FAMILY_NAME");
	}

	private void addFamilyNicknameTranscription(){
		addTranscription(familyNicknameTransModel, familyNicknameTransRecords, "FAMILY_NICKNAME");
	}

	private void editFamilyNicknameTranscription(){
		editTranscription(familyNicknameTransModel, familyNicknameTransRecords, familyNicknameList);
	}

	private void deleteFamilyNicknameTranscription(){
		deleteTranscription(familyNicknameTransModel, familyNicknameTransRecords, familyNicknameList, "FAMILY_NICKNAME");
	}


	private void loadData(){
		// Main fields
		typeCombo.setSelectedItem(FLEFRecordHelper.getChildValue(nameRecord, "TYPE"));
		titleField.setText(FLEFRecordHelper.getChildValue(nameRecord, "TITLE"));
		givenNameField.setText(FLEFRecordHelper.getChildValue(nameRecord, "INDIVIDUAL_NAME"));
		nicknameField.setText(FLEFRecordHelper.getChildValue(nameRecord, "INDIVIDUAL_NICKNAME"));
		familyNameField.setText(FLEFRecordHelper.getChildValue(nameRecord, "FAMILY_NAME"));
		familyNicknameField.setText(FLEFRecordHelper.getChildValue(nameRecord, "FAMILY_NICKNAME"));

		FLEFRecord givenNode = FLEFRecordHelper.findChild(nameRecord, "INDIVIDUAL_NAME");
		suffixField.setText(givenNode != null? FLEFRecordHelper.getChildValue(givenNode, "SUFFIX"): StringUtils.EMPTY);

		// transcriptions
		loadTranscriptions("TITLE", titleTransModel, titleTransRecords);
		loadTranscriptions("INDIVIDUAL_NAME", "TRANSCRIBED_TEXT", givenTransModel, givenTransRecords);
		loadTranscriptions("SUFFIX", "TRANSCRIBED_TEXT", suffixTransModel, suffixTransRecords);
		loadTranscriptions("INDIVIDUAL_NICKNAME", nicknameTransModel, nicknameTransRecords);
		loadTranscriptions("FAMILY_NAME", familyTransModel, familyTransRecords);
		loadTranscriptions("FAMILY_NICKNAME", familyNicknameTransModel, familyNicknameTransRecords);

		// references
		loadCulturalNorms();
		loadNotes();
		loadSources();
	}

	private void loadTranscriptions(String parentTag, DefaultListModel<String> model,
		List<FLEFRecord> records){
		model.clear();
		records.clear();
		FLEFRecord parent = FLEFRecordHelper.findChild(nameRecord, parentTag);
		if(parent == null)
			return;
		for(FLEFRecord child : parent.getChildren()){
			if("TRANSCRIBED_TEXT".equals(child.getTag())){
				records.add(child);
				model.addElement(buildTranscriptionDisplay(child));
			}
		}
	}

	private void loadTranscriptions(String parentTag, String childTag, DefaultListModel<String> model,
		List<FLEFRecord> records){
		model.clear();
		records.clear();
		FLEFRecord parent = FLEFRecordHelper.findChild(nameRecord, parentTag);
		if(parent == null)
			return;
		for(FLEFRecord child : parent.getChildren()){
			if(childTag.equals(child.getTag())){
				records.add(child);
				model.addElement(buildTranscriptionDisplay(child));
			}
		}
	}

	private String buildTranscriptionDisplay(FLEFRecord transRecord){
		String phonetic = FLEFRecordHelper.getChildValue(transRecord, "PHONETIC");
		String transcription = FLEFRecordHelper.getChildValue(transRecord, "TRANSCRIPTION");
		StringBuilder sb = new StringBuilder();
		if(phonetic != null)
			sb.append("phonetic: ")
				.append(phonetic);
		if(transcription != null){
			if(!sb.isEmpty())
				sb.append(" | ");
			sb.append("transcription: ")
				.append(transcription);
		}
		if(sb.isEmpty())
			sb.append("[empty]");
		return sb.toString();
	}

	private void saveData(){
		FLEFRecordHelper.removeAllChildren(nameRecord);

		FLEFRecordHelper.updateChildValue(nameRecord, "TYPE", (String)typeCombo.getSelectedItem());
		FLEFRecordHelper.updateChildValue(nameRecord, "TITLE", titleField.getText().trim());
		FLEFRecordHelper.updateChildValue(nameRecord, "INDIVIDUAL_NAME", givenNameField.getText().trim());
		FLEFRecordHelper.updateChildValue(nameRecord, "INDIVIDUAL_NICKNAME", nicknameField.getText().trim());
		FLEFRecordHelper.updateChildValue(nameRecord, "FAMILY_NAME", familyNameField.getText().trim());
		FLEFRecordHelper.updateChildValue(nameRecord, "FAMILY_NICKNAME", familyNicknameField.getText().trim());

		String suffix = suffixField.getText().trim();
		FLEFRecord givenNode = FLEFRecordHelper.findChild(nameRecord, "INDIVIDUAL_NAME");
		if(givenNode == null){
			givenNode = FLEFRecord.createChildWithValue("INDIVIDUAL_NAME", givenNameField.getText().trim());
			nameRecord.addChild(givenNode);
		}
		FLEFRecordHelper.updateChildValue(givenNode, "SUFFIX", suffix);

		// Ensure parent nodes for transcriptions (transcriptions are already added)
		ensureParentNode("TITLE");
		ensureParentNode("INDIVIDUAL_NAME");
		ensureParentNode("SUFFIX");
		ensureParentNode("INDIVIDUAL_NICKNAME");
		ensureParentNode("FAMILY_NAME");
		ensureParentNode("FAMILY_NICKNAME");

		// References
		FLEFRecordHelper.removeChildren(nameRecord, "CULTURAL_NORM");
		FLEFRecordHelper.removeChildren(nameRecord, "NOTE");
		FLEFRecordHelper.removeChildren(nameRecord, "SOURCE");
		for(String id : culturalNormIds){
			FLEFRecordHelper.addChild(nameRecord, "CULTURAL_NORM", id);
		}
		for(String id : noteIds){
			FLEFRecordHelper.addChild(nameRecord, "NOTE", id);
		}
		for(String id : sourceIds){
			FLEFRecordHelper.addChild(nameRecord, "SOURCE", id);
		}
	}

	private void ensureParentNode(String tag){
		if(FLEFRecordHelper.findChild(nameRecord, tag) == null){
			FLEFRecord parent = FLEFRecord.createChild(tag);
			nameRecord.addChild(parent);
		}
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getNameRecord(){
		return nameRecord;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());

		SwingUtilities.invokeLater(() -> {
			JDialog parent = new JDialog();
			parent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			NameDialog dialog = new NameDialog(parent, model, null);
			dialog.setVisible(true);
			if(dialog.isSaved()){
				FLEFRecord record = dialog.getNameRecord();
				System.out.println("Saved name: " + record);
			}
			System.exit(0);
		});
	}

}
