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
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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


/**
 * Dialog for editing a HISTORIC_EVENT_RECORD according to FLEF 0.0.9.
 */
public class HistoricEventDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -448723324774870482L;


	static{
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new HistoricEventHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JTextField titleField = new JTextField(30);

	// ========== PLACE (0:1) ==========
	private final JTextField placeDisplayField = new JTextField(20);
	private final JButton placeBrowseBtn = new JButton("Browse...");
	private final JButton placeClearBtn = new JButton("Clear");
	private String selectedPlaceId;
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("Place Evidence");

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
	private final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler("PLACE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ==================== Constructors ====================
	public HistoricEventDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Historic Event", model, record);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 700));
		pack();
		setLocationRelativeTo(parent);
	}

	public HistoricEventDialog(Frame parent, FLEFModel model){
		super(parent, "New Historic Event", model, null);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 700));
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

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Panel factories ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// TITLE (0:1)
		panel.add(new JLabel("Title:"), "align label");
		panel.add(titleField, "growx,wrap");

		// PLACE (0:1)
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

		// PLACE -> CERTAINTY + CREDIBILITY (grouped in EvidenceQualifiersPanel)
		panel.add(placeQualifiers, "span 2,growx,wrap");

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
		scrollPane.setPreferredSize(new Dimension(200, 80));
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

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// TITLE (0:1)
		titleField.setText(FLEFRecordUtils.getChildValue(record, "TITLE"));

		// PLACE (0:1) with CERTAINTY and CREDIBILITY
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

		// NOTES (0:M)
		loadNotes();

		// SOURCE CITATIONS (0:M)
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// MODIFICATION (1:1)
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// MODIFICATION_STRUCTURE (1:1) is required
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
		// Validation is already done by save() before calling this method
		record.getChildren().clear();

		// TITLE (0:1)
		String title = titleField.getText().trim();
		if(!title.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "TITLE", title);
		}

		// PLACE (0:1) with CERTAINTY and CREDIBILITY
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

		// NOTES (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		// SOURCE CITATIONS (0:M)
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// MODIFICATION (1:1)
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), "HISTORIC_EVENT");
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "HISTORIC_EVENT", "H");
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi un place di esempio
		FLEFRecord place = FLEFRecord.createMainRecord("P1", "PLACE");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		name.setValue("Rome");
		place.addChild(name);
		model.addRecord(place);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Historic Event Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Historic Event");
			btn.addActionListener(e -> {
				HistoricEventDialog dialog = new HistoricEventDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Historic Event saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
