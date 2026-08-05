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
package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
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
 * Dialog for editing an {@code ALIAS} structure according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * ALIAS @<XREF:INDIVIDUAL>@    {0:M}
 *   +2 CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
 *   +2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 *   +2 NOTE @<XREF:NOTE>@    {0:M}
 * </pre>
 */
public class _AliasDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 4427478554848969493L;


	private final Frame parentFrame;

	private final FLEFModel model;
	private final AliasEntry existingEntry;
	private boolean saved = false;

	private final JTextField aliasDisplayField = new JTextField(20);
	private final JButton browseAliasBtn = new JButton("Browse...");
	private final JButton clearAliasBtn = new JButton("Clear");
	private String selectedAliasId;

	private final EvidenceQualifiersPanel qualifiersPanel = new EvidenceQualifiersPanel(null, "Evidence Qualifiers");

	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);

	/**
	 * Inner class representing an Alias entry.
	 */
	public static class AliasEntry{
		public String aliasId;
		public String sourceType;
		public String informationTYpe;
		public String evidenceType;
		public List<String> noteIds;

		public AliasEntry(String aliasId, String sourceType, final String informationTYpe,
				final String evidenceType, List<String> noteIds){
			this.aliasId = aliasId;
			this.sourceType = sourceType;
			this.informationTYpe = informationTYpe;
			this.evidenceType = evidenceType;
			this.noteIds = noteIds != null? noteIds: new ArrayList<>();
		}

		@Override
		public String toString(){
			return aliasId;
		}
	}

	/**
	 * Creates a dialog to create a new alias.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model
	 */
	public static _AliasDialog createNew(final Frame parent, final FLEFModel model){
		return new _AliasDialog(parent, model, null);
	}

	/**
	 * Creates a dialog to edit an existing alias.
	 *
	 * @param parent        the parent frame
	 * @param model         the FLEF model
	 * @param existingEntry the existing AliasEntry
	 */
	public static _AliasDialog createEdit(final Frame parent, final FLEFModel model, final AliasEntry existingEntry){
		if(existingEntry == null)
			throw new IllegalArgumentException("existingEntry cannot be null");

		return new _AliasDialog(parent, model, existingEntry);
	}

	private _AliasDialog(Frame parent, FLEFModel model, AliasEntry existingEntry){
		super(parent, existingEntry == null? "Add Alias": "Edit Alias", true);

		this.model = model;
		this.parentFrame = parent;
		this.existingEntry = existingEntry != null? existingEntry: new AliasEntry(StringUtils.EMPTY, null, null, null, new ArrayList<>());
		initComponents();
		if(existingEntry != null){
			loadData();
		}
		pack();
		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel basicPanel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]5[]5[]5[]"));

		// ALIAS ID
		basicPanel.add(new JLabel("Alias (Individual):"), "align label");
		aliasDisplayField.setEditable(false);
		aliasDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel aliasPanel = new JPanel(new BorderLayout(5, 5));
		aliasPanel.add(aliasDisplayField, BorderLayout.CENTER);
		JPanel aliasBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		aliasBtnPanel.add(browseAliasBtn);
		aliasBtnPanel.add(clearAliasBtn);
		aliasPanel.add(aliasBtnPanel, BorderLayout.EAST);
		basicPanel.add(aliasPanel, "growx,wrap");

		browseAliasBtn.addActionListener(e -> browseAlias());
		clearAliasBtn.addActionListener(e -> {
			selectedAliasId = null;
			aliasDisplayField.setText(StringUtils.EMPTY);
		});

		// CERTAINTY + CREDIBILITY (0:1 each) - grouped in EvidenceQualifiersPanel
		basicPanel.add(qualifiersPanel, "span 2,growx,wrap");

		tabbedPane.addTab("Basic", basicPanel);

		JPanel notesPanel = createNotesPanel();
		tabbedPane.addTab("Notes", notesPanel);

		add(tabbedPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> {
			if(validateData()){
				saved = true;
				dispose();
			}
		});
		cancelButton.addActionListener(e -> dispose());
	}


	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
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


	private void browseAlias(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, individualHandler, selectedId -> {
			if(selectedId != null){
				selectedAliasId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					aliasDisplayField.setText(individualHandler.getDisplayText(rec, model));
				}
				else{
					aliasDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}


	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayText(rec, model);
		}
		return id;
	}

	private void loadNotes(){
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		if(existingEntry != null){
			for(String noteId : existingEntry.noteIds){
				noteIds.add(noteId);
				String display = getNoteDisplayName(noteId);
				noteDisplayMap.put(noteId, display);
				noteModel.addElement(display);
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
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);

		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void deleteNote(){
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
		Set<String> before = new HashSet<>(noteIds);
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
		// Alias ID
		if(existingEntry.aliasId != null && !existingEntry.aliasId.isEmpty()){
			selectedAliasId = existingEntry.aliasId;
			FLEFRecord rec = model.getRecordById(existingEntry.aliasId);
			if(rec != null){
				aliasDisplayField.setText(individualHandler.getDisplayText(rec, model));
			}
			else{
				aliasDisplayField.setText(existingEntry.aliasId);
			}
		}

		//FIXME Certainty + Credibility
//		qualifiersPanel.load(existingEntry.certainty, existingEntry.credibility);

		// Notes
		loadNotes();
	}

	private boolean validateData(){
		// ALIAS ID
		if(selectedAliasId == null || selectedAliasId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Alias (Individual) is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	/**
	 * Returns the AliasEntry containing all data.
	 *
	 * @return the AliasEntry, or null if the dialog was cancelled
	 */
	public AliasEntry getEntry(){
		if(!saved){
			return null;
		}
		return new AliasEntry(
			selectedAliasId,
			qualifiersPanel.getSourceType(),
			qualifiersPanel.getInformationType(),
			qualifiersPanel.getEvidenceType(),
			new ArrayList<>(noteIds)
		);
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
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new NoteHandler());

		// Add a sample individual
		FLEFRecord ind = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		FLEFRecord name = FLEFRecord.createChild("NAME");
		FLEFRecord given = FLEFRecord.createChildWithValue("INDIVIDUAL_NAME", "John");
		name.addChild(given);
		FLEFRecord family = FLEFRecord.createChildWithValue("FAMILY_NAME", "Doe");
		name.addChild(family);
		ind.addChild(name);
		model.addRecord(ind);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Alias Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("Add Alias");
			btn.addActionListener(e -> {
				_AliasDialog dialog = createNew(frame, model);
				dialog.setVisible(true);

				if(dialog.isSaved()){
					_AliasDialog.AliasEntry entry = dialog.getEntry();
					System.out.println("Alias saved: " + entry.aliasId);
				}
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
