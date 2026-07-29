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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
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
 * Dialog for editing a GROUP_CITATION structure according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * GROUP_CITATION :=
 *   n GROUP @<XREF:GROUP>@    {1:1}
 *     +1 ROLE <ROLE_IN_GROUP>    {0:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 * </pre>
 */
public class GroupCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 6392435736491575834L;


	static{
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new NoteHandler());
	}

	private final Dialog parentDialog;

	private final FLEFModel model;
	private final FLEFRecord existingCitation; // may be null for new
	private boolean saved = false;

	private final JTextField groupDisplayField = new JTextField(20);
	private final JButton browseBtn = new JButton("Browse...");
	private final JButton editBtn = new JButton("Edit");
	private final JButton clearBtn = new JButton("Clear");
	private String selectedGroupId;

	private final JTextField roleField = new JTextField(15);

	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final JComboBox<String> credibilityCombo = new JComboBox<>(new String[]{StringUtils.EMPTY, "0", "1", "2", "3"});

	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");


	public GroupCitationDialog(Dialog parent, FLEFModel model, FLEFRecord existingCitation){
		super(parent, existingCitation == null? "Add Group Citation": "Edit Group Citation", true);
		this.model = model;
		this.parentDialog = parent;
		this.existingCitation = existingCitation;
		initComponents();
		if(existingCitation != null){
			loadData();
		}
		pack();
		setMinimumSize(new Dimension(550, 500));
		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		JPanel basicPanel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]10[]10[]"));

		// GROUP
		basicPanel.add(new JLabel("Group:"), "align label");
		groupDisplayField.setEditable(false);
		groupDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel groupPanel = new JPanel(new BorderLayout(5, 5));
		groupPanel.add(groupDisplayField, BorderLayout.CENTER);
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		btnPanel.add(browseBtn);
		btnPanel.add(editBtn);
		btnPanel.add(clearBtn);
		groupPanel.add(btnPanel, BorderLayout.EAST);
		basicPanel.add(groupPanel, "growx,wrap");

		browseBtn.addActionListener(e -> browseGroup());
		editBtn.addActionListener(e -> editGroup());
		clearBtn.addActionListener(e -> {
			selectedGroupId = null;
			groupDisplayField.setText(StringUtils.EMPTY);
			editBtn.setEnabled(false);
		});

		// Initially disable Edit (Edit is only enabled when a group is selected)
		editBtn.setEnabled(false);

		// ROLE (0:1)
		basicPanel.add(new JLabel("Role:"), "align label");
		basicPanel.add(roleField, "growx,wrap");

		// CREDIBILITY (0:1)
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


	private void browseGroup(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, groupHandler, selectedId -> {
			if(selectedId != null){
				selectGroup(selectedId);
			}
		});
		dialog.setVisible(true);
	}

	private void editGroup(){
		// Edit is only enabled when a group is already selected
		if(selectedGroupId == null || selectedGroupId.isEmpty()){
			JOptionPane.showMessageDialog(this, "No group selected to edit.", "Info", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		// Open the same selection dialog, but allow changing to another group
		browseGroup();
	}

	private void selectGroup(String groupId){
		selectedGroupId = groupId;
		FLEFRecord rec = model.getRecordById(groupId);
		if(rec != null){
			groupDisplayField.setText(groupHandler.getDisplayText(rec));
		}
		else{
			groupDisplayField.setText(groupId);
		}
		editBtn.setEnabled(true);
		clearBtn.setEnabled(true);
	}


	private String getNoteDisplayName(String id){
		FLEFRecord note = model.getRecordById(id);
		if(note != null){
			return noteHandler.getDisplayText(note);
		}
		return id;
	}

	private void addNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentDialog, model, noteHandler, selectedId -> {
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
		if(idx == -1)
			return;
		String noteId = noteIds.get(idx);
		FLEFRecord note = model.getRecordById(noteId);
		if(note == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + noteId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(parentDialog, model, note);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(noteId);
		noteDisplayMap.put(noteId, newDisplay);
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
		Set<String> before = new java.util.HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(parentDialog, model);
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
		// GROUP
		String groupId = existingCitation.getValue();
		if(groupId != null && !groupId.isEmpty()){
			selectGroup(groupId);
		}

		// ROLE
		String role = FLEFRecordUtils.getChildValue(existingCitation, "ROLE");
		roleField.setText(StringUtils.defaultString(role));

		// CREDIBILITY (0:1)
		String credibility = FLEFRecordUtils.getChildValue(existingCitation, "CREDIBILITY");
		credibilityCombo.setSelectedItem(StringUtils.defaultString(credibility));

		// NOTE (0:M)
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
		// GROUP
		if(selectedGroupId == null || selectedGroupId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Group is required.\nPlease select a group.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}


	public FLEFRecord getCitationRecord(){
		if(!saved){
			return null;
		}

		FLEFRecord record = existingCitation != null? existingCitation: FLEFRecord.createChild("GROUP_CITATION");

		// GROUP
		record.setValue(selectedGroupId);

		// ROLE
		String role = roleField.getText().trim();
		FLEFRecordUtils.updateChildValue(record, "ROLE", role);

		// CREDIBILITY
		String credibility = (String)credibilityCombo.getSelectedItem();
		FLEFRecordUtils.updateChildValue(record, "CREDIBILITY", credibility);

		// NOTE (0:M)
		FLEFRecordUtils.removeChildren(record, "NOTE");
		for(String noteId : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", noteId);
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

		FLEFModel model = new FLEFModel();
		// Register all handlers (done via static blocks)

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Group Citation Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Group Citation");
			btn.addActionListener(e -> {
				GroupCitationDialog dialog = new GroupCitationDialog(null, model, null);
				dialog.setVisible(true);
				System.out.println("Group Citation saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
