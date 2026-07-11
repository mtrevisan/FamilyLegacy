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
import io.github.mtrevisan.familylegacy.v2.ui.components.DateStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.DocumentStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SourceDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 8722200901398839002L;


	static{
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CalendarHandler());
	}

	// Basic fields
	private final JTextField idField = new JTextField(10);
	private final JTextField titleField = new JTextField(30);
	private final JTextField authorField = new JTextField(30);
	private final JTextField publisherField = new JTextField(30);
	private final JComboBox<String> mediaTypeCombo = new JComboBox<>(new String[]{
		"", "audio", "book", "card", "electronic", "fiche", "film",
		"magazine", "manuscript", "map", "newspaper", "photo",
		"tombstone", "video"
	});
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// Place
	private final JTextField placeDisplayField = new JTextField(20);
	private final JButton placeBrowseBtn = new JButton("Browse...");
	private final JButton placeClearBtn = new JButton("Clear");
	private String selectedPlaceId;
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("Place Evidence");

	// Date
	private final DateStructurePanel datePanel;

	// Repository Citations
	private final DefaultListModel<String> repositoryListModel = new DefaultListModel<>();
	private final JList<String> repositoryList = new JList<>(repositoryListModel);
	private final List<FLEFRecord> repositoryRecords = new ArrayList<>();

	// Document
	private final DocumentStructurePanel documentPanel;

	// Source Citations
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// Notes
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// Modification
	private final ModificationPanel modificationPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler("PLACE");
	private final RecordTypeHandler<?> repositoryHandler = HandlerRegistry.getHandler("REPOSITORY");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ==================== Constructors ====================
	public SourceDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, model, "Edit Source", record);

		this.datePanel = new DateStructurePanel(model, this);
		this.documentPanel = new DocumentStructurePanel(model, this);
		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(950, 800));
		pack();
		setLocationRelativeTo(parent);
	}

	public SourceDialog(Frame parent, FLEFModel model){
		super(parent, model, "New Source", null);

		this.datePanel = new DateStructurePanel(model, this);
		this.documentPanel = new DocumentStructurePanel(model, this);
		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(950, 800));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		tabbedPane.addTab("Basic", createBasicPanel());
		tabbedPane.addTab("Place & Date", createPlaceDatePanel());
		tabbedPane.addTab("Repositories", createRepositoryCitationsPanel());
		tabbedPane.addTab("Document", documentPanel);
		tabbedPane.addTab("Source Citations", createSourceCitationsPanel());
		tabbedPane.addTab("Notes", createNotesPanel());
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

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

		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		panel.add(new JLabel("Title:"), "align label");
		panel.add(titleField, "growx,wrap");
		panel.add(new JLabel("Author:"), "align label");
		panel.add(authorField, "growx,wrap");
		panel.add(new JLabel("Publisher:"), "align label");
		panel.add(publisherField, "growx,wrap");
		panel.add(new JLabel("Media Type:"), "align label");
		panel.add(mediaTypeCombo, "growx,wrap");
		panel.add(restrictionCheckBox, "span 2");

		return panel;
	}

	private JPanel createPlaceDatePanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		panel.add(new JLabel("Place:"), "align label");
		placeDisplayField.setEditable(false);
		placeDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel placePanel = new JPanel(new BorderLayout(5, 5));
		placePanel.add(placeDisplayField, BorderLayout.CENTER);
		JPanel placeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		placeBtnPanel.add(placeBrowseBtn);
		placeBtnPanel.add(placeClearBtn);
		placePanel.add(placeBtnPanel, BorderLayout.EAST);
		panel.add(placePanel, "growx,wrap");

		placeBrowseBtn.addActionListener(e -> browsePlace());
		placeClearBtn.addActionListener(e -> {
			selectedPlaceId = null;
			placeDisplayField.setText("");
		});

		// Place Evidence Qualifiers (CERTAINTY + CREDIBILITY)
		panel.add(placeQualifiers, "span 2,growx,wrap");

		panel.add(new JLabel("Date:"), "align label,top");
		panel.add(datePanel, "growx");

		return panel;
	}

	private JPanel createRepositoryCitationsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Repository Citation"));

		repositoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		repositoryList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editRepositoryCitation();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(repositoryList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New Repository");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		repositoryList.addListSelectionListener(e -> {
			boolean selected = repositoryList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addRepositoryCitation());
		newBtn.addActionListener(e -> createNewRepository());
		editBtn.addActionListener(e -> editRepositoryCitation());
		deleteBtn.addActionListener(e -> deleteRepositoryCitation());

		return panel;
	}

	private JPanel createSourceCitationsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Source Citation"));

		sourceCitationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sourceCitationList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editSourceCitation();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(sourceCitationList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New Source");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		sourceCitationList.addListSelectionListener(e -> {
			boolean selected = sourceCitationList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addSourceCitation());
		newBtn.addActionListener(e -> createNewSource());
		editBtn.addActionListener(e -> editSourceCitation());
		deleteBtn.addActionListener(e -> deleteSourceCitation());

		return panel;
	}

	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Note"));

		noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNote();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(noteList);
		scrollPane.setPreferredSize(new Dimension(200, 80));
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

		noteList.addListSelectionListener(e -> {
			boolean selected = noteList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addNote());
		newBtn.addActionListener(e -> createNewNote());
		editBtn.addActionListener(e -> editNote());
		deleteBtn.addActionListener(e -> deleteNote());

		return panel;
	}

	// ==================== Place methods ====================

	private void browsePlace(){
		if(placeHandler == null){
			JOptionPane.showMessageDialog(this, "Place handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, placeHandler, selectedId -> {
			if(selectedId != null){
				selectedPlaceId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					placeDisplayField.setText(placeHandler.getDisplayName(rec));
				}
				else{
					placeDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Repository Citation methods ====================

	private String getRepositoryCitationDisplay(FLEFRecord citation){
		String repoId = citation.getValue();
		if(repoId != null && repositoryHandler != null){
			FLEFRecord rec = model.getRecordById(repoId);
			if(rec != null){
				String display = repositoryHandler.getDisplayName(rec);
				String location = FLEFRecordUtils.getChildValue(citation, "LOCATION");
				if(location != null && !location.isEmpty()){
					return display + " (loc: " + location + ")";
				}
				return display;
			}
		}
		return repoId != null? repoId: "[empty]";
	}

	private void loadRepositoryCitations(){
		repositoryListModel.clear();
		repositoryRecords.clear();
		for(FLEFRecord child : record.getChildren()){
			if("REPOSITORY_CITATION".equals(child.getTag())){
				repositoryRecords.add(child);
				repositoryListModel.addElement(getRepositoryCitationDisplay(child));
			}
		}
	}

	private void addRepositoryCitation(){
		if(repositoryHandler == null){
			JOptionPane.showMessageDialog(this, "Repository handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, repositoryHandler, selectedId -> {
			if(selectedId != null){
				// Usa la firma a 4 parametri (nessun default)
				String location = JOptionPane.showInputDialog(
					this,
					"Enter location within repository:",
					"Location",
					JOptionPane.PLAIN_MESSAGE
				);
				FLEFRecord citation = new FLEFRecord();
				citation.setLevel(1);
				citation.setTag("REPOSITORY_CITATION");
				citation.setValue(selectedId);
				if(location != null && !location.trim().isEmpty()){
					FLEFRecordUtils.updateChildValue(citation, "LOCATION", location.trim());
				}
				repositoryRecords.add(citation);
				repositoryListModel.addElement(getRepositoryCitationDisplay(citation));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editRepositoryCitation(){
		int idx = repositoryList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = repositoryRecords.get(idx);
		String repoId = existing.getValue();
		String location = FLEFRecordUtils.getChildValue(existing, "LOCATION");

		if(repositoryHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Repository handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Mostra un dialog per selezionare un nuovo repository
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, repositoryHandler, selectedId -> {
			if(selectedId != null){
				// Usa la firma a 7 parametri per avere il valore predefinito
				String newLocation = (String)JOptionPane.showInputDialog(
					this,
					"Enter location within repository:",
					"Location",
					JOptionPane.PLAIN_MESSAGE,
					null,
					null,
					location != null? location: ""
				);
				existing.setValue(selectedId);
				if(newLocation != null && !newLocation.trim().isEmpty()){
					FLEFRecordUtils.updateChildValue(existing, "LOCATION", newLocation.trim());
				}
				else{
					FLEFRecordUtils.removeChildren(existing, "LOCATION");
				}
				repositoryRecords.set(idx, existing);
				repositoryListModel.set(idx, getRepositoryCitationDisplay(existing));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void deleteRepositoryCitation(){
		int idx = repositoryList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this repository citation?"))
			return;
		repositoryRecords.remove(idx);
		repositoryListModel.remove(idx);
	}

	private void createNewRepository(){
		if(repositoryHandler == null){
			JOptionPane.showMessageDialog(this, "Repository handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = repositoryHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
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
		if(!showConfirm("Confirm", "Remove this source citation?"))
			return;
		sourceCitationRecords.remove(idx);
		sourceCitationListModel.remove(idx);
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null){
				return sourceHandler.getDisplayName(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = sourceHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
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

	private void loadNotes(){
		noteListModel.clear();
		noteIds.clear();
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
	}

	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
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
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
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
		if(!showConfirm("Confirm", "Remove this note reference?"))
			return;
		String removedId = noteIds.remove(idx);
		noteDisplayMap.remove(removedId);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
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

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());
		titleField.setText(FLEFRecordUtils.getChildValue(record, "TITLE"));
		authorField.setText(FLEFRecordUtils.getChildValue(record, "AUTHOR"));
		publisherField.setText(FLEFRecordUtils.getChildValue(record, "PUBLISHER"));
		mediaTypeCombo.setSelectedItem(FLEFRecordUtils.getChildValue(record, "MEDIA_TYPE"));
		restrictionCheckBox.setSelected("confidential".equals(FLEFRecordUtils.getChildValue(record, "RESTRICTION")));

		// Place
		FLEFRecord place = FLEFRecordUtils.findChild(record, "PLACE");
		if(place != null){
			String placeId = place.getValue();
			if(placeId != null && !placeId.isEmpty()){
				selectedPlaceId = placeId;
				FLEFRecord rec = model.getRecordById(placeId);
				if(rec != null && placeHandler != null){
					placeDisplayField.setText(placeHandler.getDisplayName(rec));
				}
				else{
					placeDisplayField.setText(placeId);
				}
			}
			String placeCert = FLEFRecordUtils.getChildValue(place, "CERTAINTY");
			String placeCred = FLEFRecordUtils.getChildValue(place, "CREDIBILITY");
			placeQualifiers.load(placeCert, placeCred);
		}

		// Date
		FLEFRecord date = FLEFRecordUtils.findChild(record, "DATE");
		datePanel.loadFromRecord(date);

		// Repository Citations
		loadRepositoryCitations();

		// Document
		FLEFRecord doc = FLEFRecordUtils.findChild(record, "DOCUMENT_STRUCTURE");
		documentPanel.loadFromRecord(doc);

		// Source Citations
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// Notes
		loadNotes();

		// Modification
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return modificationPanel.validateRequiredFields();
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		record.getChildren().clear();

		// Basic fields
		String title = titleField.getText().trim();
		if(!title.isEmpty()) FLEFRecordUtils.updateChildValue(record, "TITLE", title);
		String author = authorField.getText().trim();
		if(!author.isEmpty()) FLEFRecordUtils.updateChildValue(record, "AUTHOR", author);
		String publisher = publisherField.getText().trim();
		if(!publisher.isEmpty()) FLEFRecordUtils.updateChildValue(record, "PUBLISHER", publisher);
		String mediaType = (String)mediaTypeCombo.getSelectedItem();
		if(mediaType != null && !mediaType.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "MEDIA_TYPE", mediaType);
		}
		FLEFRecordUtils.updateChildValue(record, "RESTRICTION",
			restrictionCheckBox.isSelected()? "confidential": null);

		// Place
		if(selectedPlaceId != null && !selectedPlaceId.isEmpty()){
			FLEFRecord place = new FLEFRecord();
			place.setLevel(1);
			place.setTag("PLACE");
			place.setValue(selectedPlaceId);
			record.addChild(place);
			String pCert = placeQualifiers.getCertainty();
			if(pCert != null && !pCert.isEmpty()){
				FLEFRecordUtils.updateChildValue(place, "CERTAINTY", pCert);
			}
			String pCred = placeQualifiers.getCredibility();
			if(pCred != null && !pCred.isEmpty()){
				FLEFRecordUtils.updateChildValue(place, "CREDIBILITY", pCred);
			}
		}

		// Date
		if(datePanel.hasData()){
			FLEFRecord dateRecord = datePanel.saveToRecord(null);
			if(dateRecord != null){
				dateRecord.setLevel(1);
				dateRecord.setTag("DATE");
				record.addChild(dateRecord);
			}
		}

		// Repository Citations
		for(FLEFRecord citation : repositoryRecords){
			citation.setLevel(1);
			citation.setTag("REPOSITORY_CITATION");
			record.addChild(citation);
		}

		// Document
		if(documentPanel.hasData()){
			FLEFRecord doc = documentPanel.saveToRecord(null);
			if(doc != null){
				doc.setLevel(1);
				doc.setTag("DOCUMENT_STRUCTURE");
				record.addChild(doc);
			}
		}

		// Source Citations
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// Notes
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		// Modification
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("SOURCE");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "SOURCE", "S");
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

		// Aggiungi place di esempio
		FLEFRecord place = new FLEFRecord();
		place.setId("P1");
		place.setType("PLACE");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		name.setValue("Rome");
		place.addChild(name);
		model.addRecord(place);

		// Aggiungi repository di esempio
		FLEFRecord repo = new FLEFRecord();
		repo.setId("R1");
		repo.setType("REPOSITORY");
		FLEFRecord repoName = new FLEFRecord();
		repoName.setLevel(1);
		repoName.setTag("NAME");
		repoName.setValue("National Archives");
		repo.addChild(repoName);
		model.addRecord(repo);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Source Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Source");
			btn.addActionListener(e -> {
				SourceDialog dialog = new SourceDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Source saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
