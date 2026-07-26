package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
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
 * Dialog for editing a CULTURAL_NORM_RECORD according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * CULTURAL_NORM_RECORD :=
 *   n @<XREF:RULE>@ CULTURAL_NORM    {1:1}
 *     +1 TITLE <CULTURAL_NORM_DESCRIPTIVE_TITLE>    {0:1}
 *     +1 PLACE @<XREF:PLACE>@    {0:1}
 *       +2 CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
 *       +2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 <<SOURCE_CITATION>>    {0:M}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class CulturalNormDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 950729006569948384L;

	static{
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}

	private final BindingManager bindingManager = new BindingManager();

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final BoundTextField titleField;

	// ========== PLACE (0:1) manual ==========
	private final JTextField placeDisplayField = new JTextField(20);
	private final JButton placeBrowseBtn = new JButton("Browse...");
	private final JButton placeClearBtn = new JButton("Clear");
	private String selectedPlaceId;
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("Place Evidence");

	// ========== NOTE (0:M) manual ==========
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== SOURCE_CITATION (0:M) manual ==========
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
	public CulturalNormDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Cultural Norm", model, record);

		// Initialize bound components before using them
		titleField = new BoundTextField("TITLE", 30);

		this.modificationPanel = new ModificationPanel(this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 700));
		pack();
		setLocationRelativeTo(parent);
	}

	public CulturalNormDialog(Frame parent, FLEFModel model){
		super(parent, "New Cultural Norm", model, null);

		// Initialize bound components before using them
		titleField = new BoundTextField("TITLE", 30);

		this.modificationPanel = new ModificationPanel(this);
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

		// Register bound components
		bindingManager.bind(titleField);

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
		idField.setText(record != null? record.getId(): "");
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// TITLE (0:1) – bound field
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
		JScrollPane scrollPane = GUIHelper.createScrollPane(noteList);
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
		JScrollPane scrollPane = GUIHelper.createScrollPane(sourceCitationList);
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
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(this), model, placeHandler, selectedId -> {
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
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayName(rec);
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
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			GUIHelper.getParentFrame(this), model, noteHandler, selectedId -> {
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
		if(idx == -1) return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(GUIHelper.getParentFrame(this), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this note reference?")) return;
		String removedId = noteIds.remove(idx);
		noteDisplayMap.remove(removedId);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(GUIHelper.getParentFrame(this), model);
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
		SourceCitationDialog dialog = new SourceCitationDialog(GUIHelper.getParentFrame(this), model, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setTag("SOURCE");
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
			}
		}
	}

	private void editSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord existing = sourceCitationRecords.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(GUIHelper.getParentFrame(this), model, existing);
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
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this source citation?")) return;
		sourceCitationRecords.remove(idx);
		sourceCitationListModel.remove(idx);
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null){
				return sourceHandler.getDisplayName(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		JDialog dialog = sourceHandler.createNewDialog(GUIHelper.getParentFrame(this), model);
		dialog.setVisible(true);
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record != null? record.getId(): "");

		// ---- Simple fields: load via binding manager ----
		bindingManager.loadFromRecord(record);

		// ---- Complex fields: manual load ----

		// PLACE (0:1) with CERTAINTY and CREDIBILITY
		FLEFRecord place = FLEFRecordUtils.findChild(record, "PLACE");
		if(place != null){
			String placeId = place.getValue();
			if(placeId != null && !placeId.isEmpty()){
				selectedPlaceId = placeId;
				FLEFRecord rec = model.getRecordById(placeId);
				if(rec != null){
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
			if("SOURCE".equals(child.getTag())){
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
		return true;
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// ---- Save simple fields via binding manager ----
		bindingManager.saveToRecord(record);

		// ---- Complex fields: manual save ----

		// PLACE (0:1) with CERTAINTY and CREDIBILITY
		if(selectedPlaceId != null && !selectedPlaceId.isEmpty()){
			FLEFRecord place = new FLEFRecord();
			place.setTag("PLACE");
			place.setValue(selectedPlaceId);
			record.addChild(place);

			String pCert = placeQualifiers.getCertainty();
			FLEFRecordUtils.updateChildValue(place, "CERTAINTY", pCert);
			String pCred = placeQualifiers.getCredibility();
			FLEFRecordUtils.updateChildValue(place, "CREDIBILITY", pCred);
		}

		// NOTES (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", id);
		}

		// SOURCE CITATIONS (0:M)
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setTag("SOURCE");
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
		return FLEFRecord.createMainRecord(generateNewId(), "CULTURAL_NORM");
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "CULTURAL_NORM", "CN");
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
		name.setTag("NAME");
		name.setValue("Rome");
		place.addChild(name);
		model.addRecord(place);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Cultural Norm Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Cultural Norm");
			btn.addActionListener(e -> {
				CulturalNormDialog dialog = new CulturalNormDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Cultural Norm saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
