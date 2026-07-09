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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Dialog for editing an ASSOCIATION structure.
 */
public class AssociationDialog extends JDialog{

	private final FLEFModel model;
	private final Frame parentFrame;
	private final FLEFRecord existingAssociation;
	private boolean saved = false;

	// ========== Type selection ==========
	private final JRadioButton existingRecordRadio = new JRadioButton("Existing Record", true);
	private final JRadioButton voidRecordRadio = new JRadioButton("Void Record (no research)");
	private final ButtonGroup typeGroup = new ButtonGroup();

	// ========== Existing Record panel ==========
	private final JComboBox<String> targetTypeCombo = new JComboBox<>(new String[]{"INDIVIDUAL", "FAMILY"});
	private final JTextField targetDisplayField = new JTextField(20);
	private final JButton browseButton = new JButton("Browse...");

	// ========== Void Record panel ==========
	private final JTextField voidNameField = new JTextField(20);

	// ========== Card panel ==========
	private final JPanel cardPanel = new JPanel(new CardLayout());

	// ========== Notes (0:M) ==========
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== Source Citations (0:M) ==========
	private final DefaultListModel<String> sourceModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceModel);
	private final List<String> sourceIds = new ArrayList<>();
	private final Map<String, String> sourceDisplayMap = new HashMap<>();

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> familyHandler = HandlerRegistry.getHandler("FAMILY");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	public AssociationDialog(JDialog parent, FLEFModel model, FLEFRecord existingAssociation){
		super(parent, "Edit Association", true);
		this.model = model;
		this.parentFrame = getParentFrame(parent);
		this.existingAssociation = existingAssociation;
		initComponents();
		if(existingAssociation != null){
			loadData();
		}
		pack();
		setMinimumSize(new Dimension(550, 500));
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

		JPanel mainPanel = new JPanel(new MigLayout("fill", "[grow]", "[]10[]10[]10[]10"));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ---- Association Type (radio buttons) ----
		typeGroup.add(existingRecordRadio);
		typeGroup.add(voidRecordRadio);

		JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		typePanel.add(new JLabel("Association Type:"));
		typePanel.add(existingRecordRadio);
		typePanel.add(voidRecordRadio);
		mainPanel.add(typePanel, "grow,wrap");

		// ---- Card panel: Existing Record or Void Record ----
		cardPanel.setBorder(new TitledBorder("Target"));
		cardPanel.add(createExistingPanel(), "EXISTING");
		cardPanel.add(createVoidPanel(), "VOID");
		mainPanel.add(cardPanel, "grow,wrap");

		// ---- Notes ----
		mainPanel.add(createReferencePanel("Note References (0:M)", noteModel, noteList,
			noteIds, noteDisplayMap, this::addNote, this::editNote, this::removeNote,
			this::createNewNote), "grow,wrap");

		// ---- Sources ----
		mainPanel.add(createReferencePanel("Source Citations (0:M)", sourceModel, sourceList,
			sourceIds, sourceDisplayMap, this::addSource, this::editSource, this::removeSource,
			this::createNewSource), "grow");

		add(mainPanel, BorderLayout.CENTER);

		// ---- Button panel ----
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		// ---- Listeners ----
		saveButton.addActionListener(e -> {
			saved = true;
			dispose();
		});
		cancelButton.addActionListener(e -> dispose());

		existingRecordRadio.addActionListener(e -> showCard("EXISTING"));
		voidRecordRadio.addActionListener(e -> showCard("VOID"));

		browseButton.addActionListener(e -> browseTarget());

		// ---- Initial state ----
		showCard("EXISTING");
	}

	// ==================== Existing panel ====================

	private JPanel createExistingPanel(){
		JPanel panel = new JPanel(new MigLayout("fill", "[right]rel[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(new JLabel("Target Type:"), "align label");
		panel.add(targetTypeCombo, "grow,wrap");

		panel.add(new JLabel("Target:"), "align label");
		targetDisplayField.setEditable(false);
		targetDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel targetPanel = new JPanel(new BorderLayout(5, 0));
		targetPanel.add(targetDisplayField, BorderLayout.CENTER);
		targetPanel.add(browseButton, BorderLayout.EAST);
		panel.add(targetPanel, "grow,wrap");

		return panel;
	}

	private JPanel createVoidPanel(){
		JPanel panel = new JPanel(new MigLayout("fill", "[right]rel[grow]", "[]5"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(new JLabel("Name:"), "align label");
		panel.add(voidNameField, "grow,wrap");

		return panel;
	}

	// ==================== Reference panel helper ====================

	private JPanel createReferencePanel(String title, DefaultListModel<String> model, JList<String> list,
		List<String> ids, Map<String, String> displayMap,
		Runnable addAction, Runnable editAction, Runnable deleteAction,
		Runnable newAction){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder(title));

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 70));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
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
		removeBtn.addActionListener(e -> deleteAction.run());

		return panel;
	}

	private void showCard(String cardName){
		CardLayout cl = (CardLayout)cardPanel.getLayout();
		cl.show(cardPanel, cardName);
		if("EXISTING".equals(cardName)){
			String text = targetDisplayField.getText();
			if(!text.isEmpty()){
				updateTargetDisplay(text);
			}
		}
	}

	private void updateTargetDisplay(String targetId){
		String targetType = (String)targetTypeCombo.getSelectedItem();
		RecordTypeHandler<?> handler = null;
		if("INDIVIDUAL".equals(targetType)){
			handler = individualHandler;
		}
		else if("FAMILY".equals(targetType)){
			handler = familyHandler;
		}

		String display = targetId;
		if(handler != null){
			FLEFRecord target = model.getRecordById(targetId);
			if(target != null){
				display = handler.getDisplayName(target);
			}
		}
		targetDisplayField.setText(display);
	}

	private void browseTarget(){
		String targetType = (String)targetTypeCombo.getSelectedItem();
		RecordTypeHandler<?> handler = null;
		if("INDIVIDUAL".equals(targetType)){
			handler = individualHandler;
		}
		else if("FAMILY".equals(targetType)){
			handler = familyHandler;
		}

		if(handler == null){
			JOptionPane.showMessageDialog(this, "Handler not registered for " + targetType, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentFrame, model, handler, selectedId -> {
			if(selectedId != null){
				updateTargetDisplay(selectedId);
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Notes methods ====================

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
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : existingAssociation.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
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
		if(idx == -1) return;
		String id = noteIds.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = (JDialog)noteHandler.createEditDialog(parentFrame, model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void removeNote(){
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
		if(noteHandler == null){
			JOptionPane.showMessageDialog(this, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = (JDialog)noteHandler.createNewDialog(parentFrame, model);
		dialog.setVisible(true);
		JOptionPane.showMessageDialog(this, "Note created. Use 'Add' to add it.",
			"Success", JOptionPane.INFORMATION_MESSAGE);
	}

	// ==================== Source methods ====================

	private String getSourceDisplayName(String id){
		if(sourceHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return sourceHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void loadSources(){
		sourceModel.clear();
		sourceIds.clear();
		sourceDisplayMap.clear();
		for(FLEFRecord child : existingAssociation.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourceModel.addElement(display);
			}
		}
	}

	private void addSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parentFrame, model, sourceHandler, selectedId -> {
			if(selectedId != null && !sourceIds.contains(selectedId)){
				sourceIds.add(selectedId);
				String display = getSourceDisplayName(selectedId);
				sourceDisplayMap.put(selectedId, display);
				sourceModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;
		String id = sourceIds.get(idx);
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Source not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = (JDialog)sourceHandler.createEditDialog(parentFrame, model, rec);
		dialog.setVisible(true);
		String newDisplay = getSourceDisplayName(id);
		sourceDisplayMap.put(id, newDisplay);
		sourceModel.set(idx, newDisplay);
	}

	private void removeSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = sourceIds.remove(idx);
			sourceDisplayMap.remove(removedId);
			sourceModel.remove(idx);
		}
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = (JDialog)sourceHandler.createNewDialog(parentFrame, model);
		dialog.setVisible(true);
		JOptionPane.showMessageDialog(this, "Source created. Use 'Add' to add it.",
			"Success", JOptionPane.INFORMATION_MESSAGE);
	}

	// ==================== Load & Save ====================

	private void loadData(){
		String value = existingAssociation.getValue();
		boolean isVoidAssociation = "@VOID@".equals(value) ||
			existingAssociation.getId() != null && "VOID".equals(existingAssociation.getId());

		if(isVoidAssociation){
			voidRecordRadio.setSelected(true);
			showCard("VOID");
			voidNameField.setText(FLEFRecordUtils.getChildValue(existingAssociation, "NAME"));
		}
		else{
			existingRecordRadio.setSelected(true);
			showCard("EXISTING");
			if(value != null){
				updateTargetDisplay(value);
			}
		}

		loadNotes();
		loadSources();
	}

	public FLEFRecord getAssociationRecord(){
		FLEFRecord record = existingAssociation != null? existingAssociation: new FLEFRecord();

		if(existingAssociation == null){
			record.setLevel(1);
			record.setTag("ASSOCIATION");
		}

		boolean isVoidSelected = voidRecordRadio.isSelected();

		if(isVoidSelected){
			record.setId("VOID");
			record.setValue(null);
			String name = voidNameField.getText().trim();
			FLEFRecordUtils.removeChildren(record, "NAME");
			if(!name.isEmpty()){
				FLEFRecord nameChild = new FLEFRecord();
				nameChild.setLevel(2);
				nameChild.setTag("NAME");
				nameChild.setValue(name);
				record.addChild(nameChild);
			}
		}
		else{
			String targetId = targetDisplayField.getText().trim();
			record.setId(null);
			record.setValue(targetId);
			record.getChildren().removeIf(c -> "NAME".equals(c.getTag()));
		}

		// NOTE references
		FLEFRecordUtils.removeChildren(record, "NOTE");
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 2, id);
		}

		// SOURCE_CITATION references
		FLEFRecordUtils.removeChildren(record, "SOURCE_CITATION");
		for(String id : sourceIds){
			FLEFRecordUtils.addChild(record, "SOURCE_CITATION", 2, id);
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
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());

		SwingUtilities.invokeLater(() -> {
			JDialog parent = new JDialog();
			parent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			AssociationDialog dialog = new AssociationDialog(parent, model, null);
			dialog.setVisible(true);
			if(dialog.isSaved()){
				FLEFRecord record = dialog.getAssociationRecord();
				System.out.println("Saved association: " + record);
			}
			System.exit(0);
		});
	}

}
