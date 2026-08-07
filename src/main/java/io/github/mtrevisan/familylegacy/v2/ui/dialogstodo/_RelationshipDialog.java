package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dialog for editing a {@code RELATIONSHIP} structure according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * RELATIONSHIP_RECORD :=
 * n @<XREF:RELATIONSHIP>@ RELATIONSHIP    {1:1}
 *   +1 SUBJECT @<XREF:ID>@|@VOID@    {1:1}
 *   +1 OBJECT @<XREF:ID>@|@VOID@    {1:1}
 *   +1 TYPE <RELATIONSHIP_TYPE>    {1:1}
 *   +1 ROLE <RELATIONSHIP_ROLE>    {0:1}
 *   +1 STATUS <RELATIONSHIP_STATUS>    {0:1}
 *   +1 <<DATE_STRUCTURE>>    {0:1}
 *   +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 CONCLUSION <<CONCLUSION_STRUCTURE>>    {0:1}
 *   +1 RESTRICTION <confidential>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 * <p>
 * This dialog currently handles:
 * - SUBJECT, OBJECT (with browse/edit/clear)
 * - TYPE, ROLE, CREDIBILITY (simple fields via binding)
 * - NOTE (list of references)
 */
public class _RelationshipDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 6392435736491575834L;


	static{
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new NoteHandler());
	}

	private final Dialog parent;

	private final FLEFModel model;
	private final FLEFRecord existingCitation; // may be null for new
	private final String prefilledSubjectId;
	private final String prefilledObjectId;
	private boolean saved = false;

	private final BindingManager bindingManager = new BindingManager();

	private final JTextField subjectDisplayField = new JTextField(20);
	private final JButton browseSubjectBtn = new JButton("Browse...");
	private final JButton editSubjectBtn = new JButton("Edit");
	private final JButton clearSubjectBtn = new JButton("Clear");
	private String selectedSubjectId;

	private final JTextField objectDisplayField = new JTextField(20);
	private final JButton browseObjectBtn = new JButton("Browse...");
	private final JButton editObjectBtn = new JButton("Edit");
	private final JButton clearObjectBtn = new JButton("Clear");
	private String selectedObjectId;

	private final BoundTextField typeField;
	private final BoundTextField roleField;
	private final BoundComboBox<String> credibilityCombo;

	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);

	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");


	public _RelationshipDialog(Dialog parent, FLEFModel model, FLEFRecord existingCitation){
		this(parent, model, existingCitation, null, null);
	}

	/**
	 * Creates a new RelationshipDialog.
	 *
	 * @param parent             the parent frame
	 * @param model              the FLEF model
	 * @param existingCitation   an existing relationship record to edit, or null for a new one
	 * @param prefilledSubjectId a subject ID to pre-fill (may be null)
	 * @param prefilledObjectId  an object ID to pre-fill (may be null)
	 */
	public _RelationshipDialog(Dialog parent, FLEFModel model, FLEFRecord existingCitation,
		String prefilledSubjectId, String prefilledObjectId){
		super(parent, (existingCitation == null? "Add Relationship": "Edit Relationship"), true);

		this.model = model;
		this.parent = parent;
		this.existingCitation = existingCitation;
		this.prefilledSubjectId = prefilledSubjectId;
		this.prefilledObjectId = prefilledObjectId;

		typeField = new BoundTextField("TYPE", 15);
		roleField = new BoundTextField("ROLE", 15);
		credibilityCombo = new BoundComboBox<>("CREDIBILITY",
			new String[]{StringUtils.EMPTY, "0", "1", "2", "3"});

		initComponents();
		if(existingCitation != null){
			loadData();
		}
		else{
			// Apply pre-filled IDs if provided
			if(prefilledSubjectId != null){
				selectSubject(prefilledSubjectId);
				browseSubjectBtn.setEnabled(false);
				clearSubjectBtn.setEnabled(false);
				subjectDisplayField.setEditable(false);
				subjectDisplayField.setBackground(UIManager.getColor("TextField.background"));
			}
			if(prefilledObjectId != null){
				selectObject(prefilledObjectId);
				browseObjectBtn.setEnabled(false);
				clearObjectBtn.setEnabled(false);
				objectDisplayField.setEditable(false);
				objectDisplayField.setBackground(UIManager.getColor("TextField.background"));
			}
		}
		pack();
		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		bindingManager.bind(typeField);
		bindingManager.bind(roleField);
		bindingManager.bind(credibilityCombo);

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		JPanel basicPanel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]10[]10[]10[]10[]"));

		// SUBJECT
		basicPanel.add(new JLabel("Subject:"), "align label");
		subjectDisplayField.setEditable(false);
		subjectDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel subjectPanel = new JPanel(new BorderLayout(5, 5));
		subjectPanel.add(subjectDisplayField, BorderLayout.CENTER);
		JPanel subjectBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		subjectBtnPanel.add(browseSubjectBtn);
		subjectBtnPanel.add(editSubjectBtn);
		subjectBtnPanel.add(clearSubjectBtn);
		subjectPanel.add(subjectBtnPanel, BorderLayout.EAST);
		basicPanel.add(subjectPanel, "growx,wrap");

		browseSubjectBtn.addActionListener(e -> browseSubject());
		editSubjectBtn.addActionListener(e -> editSubject());
		clearSubjectBtn.addActionListener(e -> {
			selectedSubjectId = null;
			subjectDisplayField.setText(StringUtils.EMPTY);
			editSubjectBtn.setEnabled(false);
		});
		editSubjectBtn.setEnabled(false);

		// OBJECT
		basicPanel.add(new JLabel("Object:"), "align label");
		objectDisplayField.setEditable(false);
		objectDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel objectPanel = new JPanel(new BorderLayout(5, 5));
		objectPanel.add(objectDisplayField, BorderLayout.CENTER);
		JPanel objectBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		objectBtnPanel.add(browseObjectBtn);
		objectBtnPanel.add(editObjectBtn);
		objectBtnPanel.add(clearObjectBtn);
		objectPanel.add(objectBtnPanel, BorderLayout.EAST);
		basicPanel.add(objectPanel, "growx,wrap");

		browseObjectBtn.addActionListener(e -> browseObject());
		editObjectBtn.addActionListener(e -> editObject());
		clearObjectBtn.addActionListener(e -> {
			selectedObjectId = null;
			objectDisplayField.setText(StringUtils.EMPTY);
			editObjectBtn.setEnabled(false);
		});
		editObjectBtn.setEnabled(false);

		// TYPE – bound field
		basicPanel.add(new JLabel("Type:"), "align label");
		basicPanel.add(typeField, "growx,wrap");

		// ROLE – bound field
		basicPanel.add(new JLabel("Role:"), "align label");
		basicPanel.add(roleField, "growx,wrap");

		// CREDIBILITY – bound combo
		basicPanel.add(new JLabel("Credibility:"), "align label");
		basicPanel.add(credibilityCombo, "growx,wrap");

		// NOTES (0:M)
		basicPanel.add(new JLabel("Notes:"), "align label,top");
		JPanel notesPanel = createNotesPanel();
		basicPanel.add(notesPanel, "growx,wrap");

		tabbedPane.addTab("Basic", basicPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> {
			if(validateFields()){
				saved = true;
				dispose();
			}
		});
		cancelButton.addActionListener(e -> dispose());
	}


	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder("Note References"));

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

		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNote();
				}
			}
		});
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


	private void browseSubject(){
		// For now, we assume the subject is a group (common use case).
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, groupHandler, selectedItem -> {
			final String selectedId = selectedItem.getValue();
			if(selectedId != null){
				selectSubject(selectedId);
			}
		});
		dialog.setVisible(true);
	}

	private void editSubject(){
		if(selectedSubjectId == null || selectedSubjectId.isEmpty()){
			JOptionPane.showMessageDialog(this, "No subject selected to edit.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		browseSubject();
	}

	private void selectSubject(String id){
		selectedSubjectId = id;
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			String displayName = null;
			if("GROUP".equals(rec.getTag())){
				displayName = groupHandler.getDisplayText(rec, model);
			}
			else if("INDIVIDUAL".equals(rec.getTag())){
				displayName = individualHandler.getDisplayText(rec, model);
			}
			subjectDisplayField.setText(displayName != null? displayName: id);
		}
		else{
			subjectDisplayField.setText(id);
		}
		editSubjectBtn.setEnabled(true);
		clearSubjectBtn.setEnabled(true);
	}


	private void browseObject(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, individualHandler, selectedItem -> {
			final String selectedId = selectedItem.getValue();
			if(selectedId != null){
				selectObject(selectedId);
			}
		});
		dialog.setVisible(true);
	}

	private void editObject(){
		if(selectedObjectId == null || selectedObjectId.isEmpty()){
			JOptionPane.showMessageDialog(this, "No object selected to edit.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		browseObject();
	}

	private void selectObject(String id){
		selectedObjectId = id;
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			objectDisplayField.setText(individualHandler.getDisplayText(rec, model));
		}
		else{
			objectDisplayField.setText(id);
		}
		editObjectBtn.setEnabled(true);
		clearObjectBtn.setEnabled(true);
	}


	private String getNoteDisplayName(String id){
		FLEFRecord note = model.getRecordById(id);
		if(note != null){
			return noteHandler.getDisplayText(note, model);
		}
		return id;
	}

	private void addNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, noteHandler, selectedItem -> {
			final String selectedId = selectedItem.getValue();
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
		String noteId = noteIds.get(idx);
		FLEFRecord note = model.getRecordById(noteId);
		if(note == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + noteId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(this, model, note);
		dialog.setVisible(true);

		String newDisplay = getNoteDisplayName(noteId);
		noteDisplayMap.put(noteId, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteModel.remove(idx);
		}
	}

	private void createNewNote(){
		Set<String> before = new java.util.HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(this, model);
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


	private void loadData(){
		// SUBJECT
		String subjectId = FLEFRecordHelper.getChildValue(existingCitation, "SUBJECT");
		if(subjectId != null && !subjectId.isEmpty()){
			selectSubject(subjectId);
			if(prefilledSubjectId != null && prefilledSubjectId.equals(subjectId)){
				browseSubjectBtn.setEnabled(false);
				clearSubjectBtn.setEnabled(false);
			}
		}
		else if(prefilledSubjectId != null){
			selectSubject(prefilledSubjectId);
			browseSubjectBtn.setEnabled(false);
			clearSubjectBtn.setEnabled(false);
		}

		// OBJECT
		String objectId = FLEFRecordHelper.getChildValue(existingCitation, "OBJECT");
		if(objectId != null && !objectId.isEmpty()){
			selectObject(objectId);
			if(prefilledObjectId != null && prefilledObjectId.equals(objectId)){
				browseObjectBtn.setEnabled(false);
				clearObjectBtn.setEnabled(false);
			}
		}
		else if(prefilledObjectId != null){
			selectObject(prefilledObjectId);
			browseObjectBtn.setEnabled(false);
			clearObjectBtn.setEnabled(false);
		}

		// ---- Load simple fields via binding manager ----
		bindingManager.load(existingCitation);

		// NOTE (0:M) – manual
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : existingCitation.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}
	}


	private boolean validateFields(){
		if(selectedSubjectId == null || selectedSubjectId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Subject is required.\nPlease select a subject.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(selectedObjectId == null || selectedObjectId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Object is required.\nPlease select an object.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		// TYPE is required – we rely on the bound field; but we must check the value from the field.
		// However, the bound field's value might be trimmed. We'll check the actual text.
		if(typeField.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Type is required.\nPlease enter a relationship type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}


	/**
	 * Returns the relationship record with all modifications applied.
	 * If this is a new record, a new FLEFRecord is created.
	 *
	 * @return the relationship record, or null if not saved
	 */
	public FLEFRecord getCitationRecord(){
		if(!saved){
			return null;
		}

		FLEFRecord record = existingCitation != null? existingCitation: FLEFRecord.createChild("RELATIONSHIP");

		// ---- Manual fields: SUBJECT, OBJECT, NOTES ----
		// SUBJECT
		FLEFRecordHelper.updateChildValue(record, "SUBJECT", selectedSubjectId);
		// OBJECT
		FLEFRecordHelper.updateChildValue(record, "OBJECT", selectedObjectId);

		// NOTE – remove all and re-add
		FLEFRecordHelper.removeChildren(record, "NOTE");
		for(String noteId : noteIds){
			FLEFRecordHelper.addChild(record, "NOTE", noteId);
		}

		// ---- Simple fields via binding manager (TYPE, ROLE, CREDIBILITY) ----
		// This will create/update the children with correct levels
		bindingManager.save(record);

		return record;
	}

	public boolean isSaved(){
		return saved;
	}

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Relationship Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Relationship");
			btn.addActionListener(e -> {
				_RelationshipDialog dialog = new _RelationshipDialog(null, model, null, "G1", "I1");
				dialog.setVisible(true);

				System.out.println("Relationship saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
