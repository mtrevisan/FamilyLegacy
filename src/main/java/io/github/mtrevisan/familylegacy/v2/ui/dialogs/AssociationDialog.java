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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
 * Dialog for editing an {@code ASSOCIATION} structure according to FLEF 0.0.9.
 * <p>
 * Supports two variants:
 * <ul>
 *   <li>ASSOCIATION @<XREF:ID>@ - reference to an existing individual or family</li>
 *   <li>ASSOCIATION @VOID@ - reference to a record that lacks research (with NAME)</li>
 * </ul>
 */
public class AssociationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -6278867401602648130L;


	private final Frame parentFrame;

	private final FLEFModel model;
	private final FLEFRecord existingAssociation;
	private boolean saved = false;

	private final JRadioButton existingRecordRadio = new JRadioButton("Existing Record", true);
	private final JRadioButton voidRecordRadio = new JRadioButton("Void Record (no research)");
	private final ButtonGroup typeGroup = new ButtonGroup();

	private final JComboBox<String> targetTypeCombo = new JComboBox<>(new String[]{"INDIVIDUAL", "FAMILY"});
	private final JTextField targetDisplayField = new JTextField(20);
	private final JButton browseButton = new JButton("Browse...");

	private final JTextField voidNameField = new JTextField(20);

	private final JPanel cardPanel = new JPanel(new CardLayout());

	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final DefaultListModel<String> sourceModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceModel);
	private final List<String> sourceIds = new ArrayList<>();
	private final Map<String, String> sourceDisplayMap = new HashMap<>();

	private final JButton saveButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> familyHandler = HandlerRegistry.getHandler("FAMILY");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");


	public static AssociationDialog createNew(JDialog parent, FLEFModel model){
		return new AssociationDialog(parent, model, null);
	}

	public static AssociationDialog createEdit(JDialog parent, FLEFModel model, FLEFRecord existingEntry){
		if(existingEntry == null)
			throw new IllegalArgumentException("existingEntry cannot be null");

		return new AssociationDialog(parent, model, existingEntry);
	}

	private AssociationDialog(JDialog parent, FLEFModel model, FLEFRecord existingAssociation){
		super(parent, "Edit Association", true);

		this.model = model;
		this.parentFrame = getParentFrame(parent);
		this.existingAssociation = existingAssociation;
		initComponents();
		if(existingAssociation != null){
			loadData();
		}
		pack();
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

		JPanel mainPanel = new JPanel(new MigLayout("ins 10", "[grow]", "[]10[]10[]10[]"));

		// ---- Association Type (radio buttons) ----
		typeGroup.add(existingRecordRadio);
		typeGroup.add(voidRecordRadio);

		JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		typePanel.add(new JLabel("Association Type:"));
		typePanel.add(existingRecordRadio);
		typePanel.add(voidRecordRadio);
		mainPanel.add(typePanel, "growx,wrap");

		// ---- Card panel: Existing Record or Void Record ----
		cardPanel.setBorder(new TitledBorder("Target"));
		cardPanel.add(createExistingPanel(), "EXISTING");
		cardPanel.add(createVoidPanel(), "VOID");
		mainPanel.add(cardPanel, "growx,wrap");

		// ---- Notes ----
		mainPanel.add(createReferencePanel("Note References", noteModel, noteList,
			noteIds, noteDisplayMap, this::addNote, this::editNote, this::removeNote,
			this::createNewNote), "growx,wrap");

		// ---- Sources ----
		mainPanel.add(createReferencePanel("Source Citations", sourceModel, sourceList,
			sourceIds, sourceDisplayMap, this::addSource, this::editSource, this::removeSource,
			this::createNewSource), "growx");

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


	private JPanel createExistingPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]5[]"));

		panel.add(new JLabel("Target Type:"), "align label");
		panel.add(targetTypeCombo, "growx,wrap");

		panel.add(new JLabel("Target:"), "align label");
		targetDisplayField.setEditable(false);
		targetDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel targetPanel = new JPanel(new BorderLayout(5, 5));
		targetPanel.add(targetDisplayField, BorderLayout.CENTER);
		targetPanel.add(browseButton, BorderLayout.EAST);
		panel.add(targetPanel, "growx,wrap");

		return panel;
	}

	private JPanel createVoidPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]"));

		panel.add(new JLabel("Name:"), "align label");
		panel.add(voidNameField, "growx,wrap");

		return panel;
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
		JButton deleteBtn = new JButton("Remove");
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
		FLEFRecord target = model.getRecordById(targetId);
		if(target != null){
			display = handler.getDisplayText(target, model);
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
			this, model, handler, selectedId -> {
			if(selectedId != null){
				updateTargetDisplay(selectedId);
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

	private void removeNote(){
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

	/**
	 * Creates a new note and automatically adds it to the list.
	 */
	private void createNewNote(){
		// Store current note IDs to detect the newly created one
		Set<String> beforeIds = new HashSet<>(noteIds);

		JDialog dialog = noteHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		// Find the newly created note and add it automatically
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !beforeIds.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
				break;
			}
		}
	}


	private String getSourceDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return sourceHandler.getDisplayText(rec, model);
		}
		return id;
	}

	private void loadSources(){
		sourceModel.clear();
		sourceIds.clear();
		sourceDisplayMap.clear();
		for(FLEFRecord child : existingAssociation.getChildren()){
			if("SOURCE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourceModel.addElement(display);
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
				sourceModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editSource(){
		int idx = sourceList.getSelectedIndex();
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
		sourceModel.set(idx, newDisplay);
	}

	private void removeSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = sourceIds.remove(idx);
			sourceDisplayMap.remove(removedId);
			sourceModel.remove(idx);
		}
	}

	/**
	 * Creates a new source and automatically adds it to the list.
	 */
	private void createNewSource(){
		// Store current source IDs to detect the newly created one
		Set<String> beforeIds = new HashSet<>(sourceIds);

		JDialog dialog = sourceHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		// Find the newly created source and add it automatically
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !beforeIds.contains(id) && !sourceIds.contains(id)){
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourceModel.addElement(display);
				break;
			}
		}
	}


	private void loadData(){
		String value = existingAssociation.getValue();
		boolean isVoidAssociation = XRefHelper.isVoidReference(value) ||
			existingAssociation.getId() != null && XRefHelper.isVoidReference(existingAssociation.getFormattedId());

		if(isVoidAssociation){
			voidRecordRadio.setSelected(true);
			showCard("VOID");
			voidNameField.setText(FLEFRecordHelper.getChildValue(existingAssociation, "NAME"));
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
		FLEFRecord record = existingAssociation != null? existingAssociation: FLEFRecord.createChild("ASSOCIATION");

		boolean isVoidSelected = voidRecordRadio.isSelected();

		if(isVoidSelected){
			record.setId("VOID");
			record.setValue(null);
			String name = voidNameField.getText().trim();
			FLEFRecordHelper.removeChildren(record, "NAME");
			if(!name.isEmpty()){
				FLEFRecord nameChild = FLEFRecord.createChildWithValue("NAME", name);
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
		FLEFRecordHelper.removeChildren(record, "NOTE");
		for(String id : noteIds){
			FLEFRecordHelper.addChild(record, "NOTE", id);
		}

		// SOURCE_CITATION references
		FLEFRecordHelper.removeChildren(record, "SOURCE");
		for(String id : sourceIds){
			FLEFRecordHelper.addChild(record, "SOURCE", id);
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
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());

		SwingUtilities.invokeLater(() -> {
			JDialog parent = new JDialog();
			parent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			AssociationDialog dialog = createEdit(parent, model, null);
			dialog.setVisible(true);

			if(dialog.isSaved()){
				FLEFRecord record = dialog.getAssociationRecord();
				System.out.println("Saved association: " + record);
			}
			System.exit(0);
		});
	}

}
