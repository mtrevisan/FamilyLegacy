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

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
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
 * Reusable panel for editing an EVENT_STRUCTURE.
 * <p>
 * Structure:
 * <pre>
 * EVENT_STRUCTURE :=
 *   n DESCRIPTION <EVENT_DESCRIPTION_OR_ATTRIBUTE_VALUE>    {0:1}
 *   n <<DATE_STRUCTURE>>    {0:1}
 *   n PLACE @<XREF:PLACE>@    {0:1}
 *     +1 CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
 *     +1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 *   n AGENCY <RESPONSIBLE_AGENCY>    {0:1}
 *   n CAUSE <CAUSE_OF_EVENT>    {0:1}
 *     +1 CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
 *     +1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 *   n CULTURAL_NORM @<XREF:RULE>@    {0:M}
 *   n NOTE @<XREF:NOTE>@    {0:M}
 *   n <<SOURCE_CITATION>>    {0:M}
 *   n CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
 *   n CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 *   n RESTRICTION <confidential>    {0:1}
 *   n <<MODIFICATION_STRUCTURE>>    {1:1}
 *   n CONCLUSION <<CONCLUSION_STRUCTURE>>    {0:1}
 * </pre>
 */
public class EventStructurePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 4957059450168606708L;


	private final FLEFModel model;
	private final Component parent;

	private final JTextArea descriptionArea = new JTextArea(3, 20);

	private final DatePanel datePanel;

	private final JTextField placeDisplayField = new JTextField(20);
	private final JButton placeBrowseBtn = new JButton("Browse...");
	private final JButton placeNewBtn = new JButton("New");
	private final JButton placeClearBtn = new JButton("Clear");
	private String selectedPlaceId;
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("PLACE", "Place Evidence");

	private final JTextField agencyField = new JTextField(20);

	private final JTextField causeField = new JTextField(20);
	private final EvidenceQualifiersPanel causeQualifiers = new EvidenceQualifiersPanel("CAUSE", "Cause Evidence");

	private final DefaultListModel<String> culturalNormModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormModel);
	private final List<String> culturalNormIds = new ArrayList<>();
	private final Map<String, String> culturalNormDisplayMap = new HashMap<>();

	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final DefaultListModel<String> sourceModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceModel);
	private final List<FLEFRecord> sourceRecords = new ArrayList<>();

	private final EvidenceQualifiersPanel eventQualifiers = new EvidenceQualifiersPanel(null, "Event Evidence");

	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final ModificationPanel modificationPanel;

	private final ConclusionPanel conclusionPanel;

	private final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler("PLACE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");
	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");

	/**
	 * Creates a new EventStructurePanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent component (for showing dialogs)
	 */
	public EventStructurePanel(FLEFModel model, Dialog parent){
		this.model = model;
		this.parent = parent;
		this.datePanel = new DatePanel(parent, model);
		this.modificationPanel = new ModificationPanel(parent);
		this.conclusionPanel = new ConclusionPanel(model, parent);

		initComponents();
	}

	private void initComponents(){
		setLayout(new BorderLayout(5, 5));

		culturalNormList.setVisibleRowCount(4);
		noteList.setVisibleRowCount(4);
		sourceList.setVisibleRowCount(4);

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel basicPanel = createBasicPanel();
		tabbedPane.addTab("Basic", basicPanel);

		JPanel notesSourcesPanel = createNotesSourcesPanel();
		tabbedPane.addTab("Notes & Sources", notesSourcesPanel);

		tabbedPane.addTab("Conclusion", conclusionPanel);

		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);
	}


	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]5[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// DESCRIPTION (0:1)
		panel.add(new JLabel("Description:"), "align label,top");
		JScrollPane descScroll = GUIHelper.createScrollPane(descriptionArea);
		panel.add(descScroll, "growx,wrap");

		// DATE_STRUCTURE (0:1)
		panel.add(new JLabel("Date:"), "align label,top");
		panel.add(datePanel, "growx,wrap");

		// PLACE (0:1)
		panel.add(new JLabel("Place:"), "align label");
		placeDisplayField.setEditable(false);
		placeDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel placePanel = new JPanel(new BorderLayout(5, 5));
		placePanel.add(placeDisplayField, BorderLayout.CENTER);
		JPanel placeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		placeBtnPanel.add(placeNewBtn);
		placeBtnPanel.add(placeBrowseBtn);
		placeBtnPanel.add(placeClearBtn);
		placePanel.add(placeBtnPanel, BorderLayout.EAST);
		panel.add(placePanel, "growx,wrap");

		placeNewBtn.addActionListener(e -> createNewPlace());
		placeBrowseBtn.addActionListener(e -> browsePlace());
		placeClearBtn.addActionListener(e -> {
			selectedPlaceId = null;
			placeDisplayField.setText(StringUtils.EMPTY);
		});

		// Place Evidence Qualifiers (CERTAINTY + CREDIBILITY for PLACE)
		panel.add(placeQualifiers, "span 2,growx,wrap");

		// AGENCY (0:1)
		panel.add(new JLabel("Agency:"), "align label");
		panel.add(agencyField, "growx,wrap");

		// CAUSE (0:1)
		panel.add(new JLabel("Cause:"), "align label");
		panel.add(causeField, "growx,wrap");

		// Cause Evidence Qualifiers (CERTAINTY + CREDIBILITY for CAUSE)
		panel.add(causeQualifiers, "span 2,growx,wrap");

		// Event-level Evidence Qualifiers (CERTAINTY + CREDIBILITY for the event itself)
		panel.add(eventQualifiers, "span 2,growx,wrap");

		// RESTRICTION (0:1)
		panel.add(restrictionCheckBox, "span 2,wrap");

		return panel;
	}


	private JPanel createNotesSourcesPanel(){
		JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Cultural Norms (0:M)
		JPanel culturalPanel = createReferencePanel("Cultural Norms",
			culturalNormModel, culturalNormList, culturalNormIds, culturalNormDisplayMap,
			this::addCulturalNorm, this::editCulturalNorm, this::deleteCulturalNorm,
			this::createNewCulturalNorm);
		panel.add(culturalPanel);

		// Notes (0:M)
		JPanel notePanel = createReferencePanel("Notes",
			noteModel, noteList, noteIds, noteDisplayMap,
			this::addNote, this::editNote, this::deleteNote,
			this::createNewNote);
		panel.add(notePanel);

		// Source Citations (0:M) - uses full SourceCitationDialog
		JPanel sourcePanel = createSourceCitationPanel();
		// Add as a third row if we want 2 columns with 3 rows, or make it full width
		// For simplicity, we'll create a new panel with 3 rows (2+1)
		JPanel wrapper = new JPanel(new GridLayout(3, 1, 5, 5));
		wrapper.add(culturalPanel);
		wrapper.add(notePanel);
		wrapper.add(sourcePanel);
		return wrapper;
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
			boolean selected = (list.getSelectedIndex() != -1);
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


	private JPanel createSourceCitationPanel(){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder("Source Citations"));

		sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sourceList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editSourceCitation();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(sourceList);
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

		sourceList.addListSelectionListener(e -> {
			boolean selected = (sourceList.getSelectedIndex() != -1);
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

	private String getSourceCitationDisplay(FLEFRecord sourceCitation){
		String sourceId = sourceCitation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null){
				return sourceHandler.getDisplayText(rec);
			}
		}
		return sourceId != null? sourceId: "[empty]";
	}

	private void loadSourceCitations(){
		sourceModel.clear();
		sourceRecords.clear();
		// Source citations are loaded from the parent EVENT record
		// They will be loaded via loadFromRecord()
	}

	private void addSourceCitation(){
		// Show a dialog to select a source and create a citation
		SourceCitationDialog dialog = new SourceCitationDialog(
			(parent instanceof Dialog? (Dialog)parent: null),
			model,
			null // new citation
		);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				sourceRecords.add(citation);
				sourceModel.addElement(getSourceCitationDisplay(citation));
			}
		}
	}

	private void editSourceCitation(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord citation = sourceRecords.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(
			(parent instanceof Dialog? (Dialog)parent: null),
			model,
			citation
		);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceRecords.set(idx, updated);
				sourceModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void deleteSourceCitation(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			sourceRecords.remove(idx);
			sourceModel.remove(idx);
		}
	}

	private void createNewSource(){
		JDialog dialog = sourceHandler.createNewDialog(
			(parent instanceof Dialog? (Dialog)parent: null),
			model
		);
		dialog.setVisible(true);
	}


	/**
	 * Opens a dialog to browse and select an existing place.
	 */
	private void browsePlace(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Dialog? (Dialog)parent: null),
			model, placeHandler, selectedId -> {
			if(selectedId != null){
				selectedPlaceId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					placeDisplayField.setText(placeHandler.getDisplayText(rec));
				}
				else{
					placeDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	/**
	 * Creates a new place and automatically selects it.
	 */
	private void createNewPlace(){
		// Store current place IDs to detect newly created one
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("PLACE")){
			String id = rec.getId();
			if(id != null){
				before.add(id);
			}
		}

		// Open the PlaceDialog in new mode
		PlaceDialog dialog = PlaceDialog.createNew(
			(parent instanceof Dialog? (Dialog)parent: null),
			model
		);
		dialog.setVisible(true);

		// Find the newly created place
		String newPlaceId = null;
		for(FLEFRecord rec : model.getRecordsByType("PLACE")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newPlaceId = id;
				break;
			}
		}

		// If a new place was created, select it automatically
		if(newPlaceId != null && !newPlaceId.isEmpty()){
			selectedPlaceId = newPlaceId;
			FLEFRecord rec = model.getRecordById(newPlaceId);
			if(rec != null){
				placeDisplayField.setText(placeHandler.getDisplayText(rec));
			}
			else{
				placeDisplayField.setText(newPlaceId);
			}
		}
	}


	private String getCulturalNormDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return culturalNormHandler.getDisplayText(rec);
		}
		return id;
	}

	private void addCulturalNorm(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Dialog? (Dialog)parent: null),
			model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				String display = getCulturalNormDisplayName(selectedId);
				culturalNormDisplayMap.put(selectedId, display);
				culturalNormModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = culturalNormIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Cultural norm not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = culturalNormHandler.createEditDialog(
			(parent instanceof Dialog? (Dialog)parent: null),
			model, rec
		);
		dialog.setVisible(true);
		String newDisplay = getCulturalNormDisplayName(id);
		culturalNormDisplayMap.put(id, newDisplay);
		culturalNormModel.set(idx, newDisplay);
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this cultural norm reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = culturalNormIds.remove(idx);
			culturalNormDisplayMap.remove(removedId);
			culturalNormModel.remove(idx);
		}
	}

	private void createNewCulturalNorm(){
		Set<String> before = new HashSet<>(culturalNormIds);
		JDialog dialog = culturalNormHandler.createNewDialog(
			(parent instanceof Dialog? (Dialog)parent: null),
			model
		);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !culturalNormIds.contains(id)){
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormModel.addElement(display);
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

	private void addNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Dialog? (Dialog)parent: null),
			model, noteHandler, selectedId -> {
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
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(
			(parent instanceof Dialog? (Dialog)parent: null),
			model, rec
		);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteModel.remove(idx);
		}
	}

	private void createNewNote(){
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(
			(parent instanceof Dialog? (Dialog)parent: null),
			model
		);
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


	/**
	 * Loads data from an EVENT_STRUCTURE FLEFRecord.
	 *
	 * @param eventStructure the EVENT_STRUCTURE record (may be null)
	 */
	public void loadFromRecord(FLEFRecord eventStructure){
		clear();

		if(eventStructure == null){
			return;
		}

		// DESCRIPTION (0:1)
		descriptionArea.setText(FLEFRecordUtils.getChildValue(eventStructure, "DESCRIPTION"));

		// DATE_STRUCTURE (0:1)
		FLEFRecord dateRecord = FLEFRecordUtils.findChild(eventStructure, "DATE");
		datePanel.load(dateRecord);

		// PLACE (0:1)
		FLEFRecord place = FLEFRecordUtils.findChild(eventStructure, "PLACE");
		if(place != null){
			String placeId = place.getValue();
			if(placeId != null && !placeId.isEmpty()){
				selectedPlaceId = placeId;
				FLEFRecord rec = model.getRecordById(placeId);
				if(rec != null){
					placeDisplayField.setText(placeHandler.getDisplayText(rec));
				}
				else{
					placeDisplayField.setText(placeId);
				}
			}
			placeQualifiers.load(place);
		}

		// AGENCY (0:1)
		agencyField.setText(FLEFRecordUtils.getChildValue(eventStructure, "AGENCY"));

		// CAUSE (0:1)
		FLEFRecord cause = FLEFRecordUtils.findChild(eventStructure, "CAUSE");
		if(cause != null){
			causeField.setText(cause.getValue());
			causeQualifiers.load(cause);
		}

		// CULTURAL_NORM (0:M)
		for(FLEFRecord child : eventStructure.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormModel.addElement(display);
			}
		}

		// NOTE (0:M)
		for(FLEFRecord child : eventStructure.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}

		// SOURCE_CITATION (0:M)
		sourceRecords.clear();
		sourceModel.clear();
		for(FLEFRecord child : eventStructure.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceRecords.add(child);
				sourceModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// EVENT QUALIFIERS (CERTAINTY + CREDIBILITY for the event itself)
		eventQualifiers.load(eventStructure);

		// RESTRICTION
		String restriction = FLEFRecordUtils.getChildValue(eventStructure, "RESTRICTION");
		restrictionCheckBox.setSelected("confidential".equals(restriction));

		// MODIFICATION_STRUCTURE
		modificationPanel.load(eventStructure);

		// CONCLUSION
		FLEFRecord conclusion = FLEFRecordUtils.findChild(eventStructure, "CONCLUSION");
		conclusionPanel.loadFromRecord(conclusion);
	}

	/**
	 * Saves data to an EVENT_STRUCTURE FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param eventStructure the EVENT_STRUCTURE record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord eventStructure){
		// Validate before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(eventStructure == null){
			eventStructure = FLEFRecord.createChild("EVENT_STRUCTURE");
		}

		// Clear existing children
		FLEFRecordUtils.removeAllChildren(eventStructure);

		// DESCRIPTION (0:1)
		String description = descriptionArea.getText().trim();
		FLEFRecordUtils.updateChildValue(eventStructure, "DESCRIPTION", description);

		// DATE_STRUCTURE (0:1)
		if(datePanel.hasData()){
			FLEFRecord dateRecord = datePanel.save(null);
			if(dateRecord != null){
				dateRecord.setTag("DATE");
				eventStructure.addChild(dateRecord);
			}
		}

		// PLACE (0:1) with its children
		if(selectedPlaceId != null && !selectedPlaceId.isEmpty()){
			FLEFRecord place = FLEFRecord.createChildWithValue("PLACE", FLEFRecordUtils.formatXRef(selectedPlaceId));
			eventStructure.addChild(place);
			String placeCert = placeQualifiers.getCertainty();
			FLEFRecordUtils.updateChildValue(place, "CERTAINTY", placeCert);
			String placeCred = placeQualifiers.getCredibility();
			FLEFRecordUtils.updateChildValue(place, "CREDIBILITY", placeCred);
		}

		// AGENCY (0:1)
		String agency = agencyField.getText().trim();
		FLEFRecordUtils.updateChildValue(eventStructure, "AGENCY", agency);

		// CAUSE (0:1) with its children
		String causeVal = causeField.getText().trim();
		if(!causeVal.isEmpty()){
			FLEFRecord cause = FLEFRecord.createChildWithValue("CAUSE", causeVal);
			eventStructure.addChild(cause);
			String causeCert = causeQualifiers.getCertainty();
			FLEFRecordUtils.updateChildValue(cause, "CERTAINTY", causeCert);
			String causeCred = causeQualifiers.getCredibility();
			FLEFRecordUtils.updateChildValue(cause, "CREDIBILITY", causeCred);
		}

		// CULTURAL_NORM (0:M)
		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(eventStructure, "CULTURAL_NORM", id);
		}

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(eventStructure, "NOTE", id);
		}

		// SOURCE_CITATION (0:M)
		for(FLEFRecord citation : sourceRecords){
			// Ensure the citation has the correct level and tag
			citation.setTag("SOURCE");
			eventStructure.addChild(citation);
		}

		// EVENT QUALIFIERS (CERTAINTY + CREDIBILITY for the event itself)
		String eventCert = eventQualifiers.getCertainty();
		FLEFRecordUtils.updateChildValue(eventStructure, "CERTAINTY", eventCert);
		String eventCred = eventQualifiers.getCredibility();
		FLEFRecordUtils.updateChildValue(eventStructure, "CREDIBILITY", eventCred);

		// RESTRICTION
		String restriction = restrictionCheckBox.isSelected()? "confidential": null;
		FLEFRecordUtils.updateChildValue(eventStructure, "RESTRICTION", restriction);

		// MODIFICATION_STRUCTURE
		modificationPanel.save(eventStructure);

		// CONCLUSION
		if(conclusionPanel.hasData()){
			FLEFRecord conclusion = conclusionPanel.saveToRecord(null);
			if(conclusion != null){
				conclusion.setTag("CONCLUSION");
				eventStructure.addChild(conclusion);
			}
		}

		return eventStructure;
	}

	/**
	 * Validates that required fields (MODIFICATION_STRUCTURE) are filled
	 * if the event structure has any data.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		// If no data at all, validation passes
		if(!hasData()){
			return true;
		}

		// CONCLUSION (0:1) - validate if present
		if(conclusionPanel.hasData()){
			return conclusionPanel.validateRequiredFields();
		}

		return true;
	}

	/**
	 * Checks if the event structure has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return !descriptionArea.getText().trim().isEmpty() ||
			datePanel.hasData() ||
			(selectedPlaceId != null && !selectedPlaceId.isEmpty()) ||
			!agencyField.getText().trim().isEmpty() ||
			!causeField.getText().trim().isEmpty() ||
			!culturalNormModel.isEmpty() ||
			!noteModel.isEmpty() ||
			!sourceModel.isEmpty() ||
			placeQualifiers.hasData() ||
			causeQualifiers.hasData() ||
			eventQualifiers.hasData() ||
			restrictionCheckBox.isSelected() ||
			conclusionPanel.hasData();
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		descriptionArea.setText(StringUtils.EMPTY);
		datePanel.clear();
		selectedPlaceId = null;
		placeDisplayField.setText(StringUtils.EMPTY);
		placeQualifiers.clear();
		agencyField.setText(StringUtils.EMPTY);
		causeField.setText(StringUtils.EMPTY);
		causeQualifiers.clear();
		culturalNormModel.clear();
		culturalNormIds.clear();
		culturalNormDisplayMap.clear();
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		sourceModel.clear();
		sourceRecords.clear();
		eventQualifiers.clear();
		restrictionCheckBox.setSelected(false);
		modificationPanel.clear();
		conclusionPanel.clear();
	}

}
