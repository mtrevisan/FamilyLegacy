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
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a GROUP_RECORD.
 * <p>
 * Provides a tabbed interface to manage:
 * <ul>
 *   <li>Basic information (ID, NAME, TYPE)</li>
 *   <li>Members (individuals and families)</li>
 *   <li>Events (placeholder)</li>
 *   <li>Notes (placeholder)</li>
 *   <li>Sources (placeholder)</li>
 *   <li>Restriction</li>
 *   <li>Modification history (placeholder)</li>
 * </ul>
 * <p>
 * This dialog works directly with the FLEFModel and updates the record in the model.
 */
public class GroupDialog extends BaseRecordDialog{

	private final JTextField idField = new JTextField(10);
	private final JTextField nameField = new JTextField(20);
	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"", "neighborhood", "fraternity", "ladies club", "literary society"});

	// Members (individuals and families)
	private final DefaultListModel<String> individualListModel = new DefaultListModel<>();
	private final JList<String> individualList = new JList<>(individualListModel);
	private final List<String> individualIds = new ArrayList<>();
	private final DefaultListModel<String> familyListModel = new DefaultListModel<>();
	private final JList<String> familyList = new JList<>(familyListModel);
	private final List<String> familyIds = new ArrayList<>();

	// Placeholders
	private final JTextArea eventsArea = new JTextArea(5, 30);
	private final JTextArea notesArea = new JTextArea(5, 30);
	private final JTextArea sourcesArea = new JTextArea(5, 30);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	/**
	 * Creates a dialog to edit an existing group record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model containing the record
	 * @param record the record to edit (must be of type GROUP)
	 */
	public GroupDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, "Edit Group");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(700, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * Creates a dialog to create a new group record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model where the new record will be added
	 */
	public GroupDialog(Frame parent, FLEFModel model){
		super(parent, model, "New Group");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(700, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));
		JTabbedPane tabbedPane = new JTabbedPane();

		// Basic tab
		JPanel basicPanel = new JPanel(new MigLayout("fill", "[right]rel[grow]"));
		idField.setEditable(false);
		idField.setText(record.getId());
		basicPanel.add(new JLabel("ID:"), "align label");
		basicPanel.add(idField, "grow,wrap");
		basicPanel.add(new JLabel("Name:"), "align label");
		basicPanel.add(nameField, "grow,wrap");
		basicPanel.add(new JLabel("Type:"), "align label");
		basicPanel.add(typeCombo, "grow,wrap");
		basicPanel.add(restrictionCheckBox, "span 2,wrap");
		tabbedPane.addTab("Basic", basicPanel);

		// Members tab
		JPanel membersPanel = new JPanel(new GridLayout(1, 2, 10, 10));
		JPanel individualsPanel = createMemberListPanel("Individuals", individualList, individualListModel, "Add Individual", this::addIndividual, this::removeIndividual);
		JPanel familiesPanel = createMemberListPanel("Families", familyList, familyListModel, "Add Family", this::addFamily, this::removeFamily);
		membersPanel.add(individualsPanel);
		membersPanel.add(familiesPanel);
		tabbedPane.addTab("Members", membersPanel);

		// Other tabs
		tabbedPane.addTab("Events", createTextAreaPanel(eventsArea, "Events"));
		tabbedPane.addTab("Notes", createTextAreaPanel(notesArea, "Notes"));
		tabbedPane.addTab("Sources", createTextAreaPanel(sourcesArea, "Sources"));

		add(tabbedPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> saveRecord());
		cancelButton.addActionListener(e -> dispose());
	}

	private JPanel createMemberListPanel(String title, JList<String> list, DefaultListModel<String> model, String addLabel, Runnable addAction, Runnable removeAction){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createTitledBorder(title));
		panel.add(new JScrollPane(list), BorderLayout.CENTER);
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addBtn = new JButton(addLabel);
		JButton removeBtn = new JButton("Remove");
		btnPanel.add(addBtn);
		btnPanel.add(removeBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);
		addBtn.addActionListener(e -> addAction.run());
		removeBtn.addActionListener(e -> removeAction.run());
		list.addListSelectionListener(e -> removeBtn.setEnabled(list.getSelectedIndex() != -1));
		removeBtn.setEnabled(false);
		return panel;
	}

	private void addIndividual(){
		String id = JOptionPane.showInputDialog(this, "Enter Individual ID:", "Add Individual", JOptionPane.PLAIN_MESSAGE);
		if(id == null || id.trim().isEmpty()) return;
		String trimmed = id.trim();
		if(individualIds.contains(trimmed)){
			JOptionPane.showMessageDialog(this, "Already added.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return;
		}
		individualIds.add(trimmed);
		individualListModel.addElement(trimmed);
	}

	private void removeIndividual(){
		int idx = individualList.getSelectedIndex();
		if(idx == -1) return;
		individualIds.remove(idx);
		individualListModel.remove(idx);
	}

	private void addFamily(){
		String id = JOptionPane.showInputDialog(this, "Enter Family ID:", "Add Family", JOptionPane.PLAIN_MESSAGE);
		if(id == null || id.trim().isEmpty()) return;
		String trimmed = id.trim();
		if(familyIds.contains(trimmed)){
			JOptionPane.showMessageDialog(this, "Already added.", "Duplicate", JOptionPane.WARNING_MESSAGE);
			return;
		}
		familyIds.add(trimmed);
		familyListModel.addElement(trimmed);
	}

	private void removeFamily(){
		int idx = familyList.getSelectedIndex();
		if(idx == -1) return;
		familyIds.remove(idx);
		familyListModel.remove(idx);
	}

	private JPanel createTextAreaPanel(JTextArea area, String title){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(title));
		panel.add(new JScrollPane(area), BorderLayout.CENTER);
		return panel;
	}

	@Override
	protected void loadData(){
		idField.setText(record.getId());
		nameField.setText(getChildValue("NAME"));
		typeCombo.setSelectedItem(getChildValue("TYPE"));
		restrictionCheckBox.setSelected("confidential".equals(getChildValue("RESTRICTION")));

		// Load members
		individualIds.clear();
		individualListModel.clear();
		familyIds.clear();
		familyListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("INDIVIDUAL".equals(child.getTag()) && child.getValue() != null){
				individualIds.add(child.getValue());
				individualListModel.addElement(child.getValue());
			}
			else if("FAMILY".equals(child.getTag()) && child.getValue() != null){
				familyIds.add(child.getValue());
				familyListModel.addElement(child.getValue());
			}
		}

		eventsArea.setText(getChildValue("EVENT"));
		notesArea.setText(getChildValue("NOTE"));
		sourcesArea.setText(getChildValue("SOURCE_CITATION"));
	}

	@Override
	protected void saveRecord(){
		updateChildValue("NAME", nameField.getText().trim());
		updateChildValue("TYPE", (String)typeCombo.getSelectedItem());
		updateChildValue("RESTRICTION", restrictionCheckBox.isSelected()? "confidential": null);

		// Rebuild members
		removeChildren("INDIVIDUAL");
		removeChildren("FAMILY");
		for(String id : individualIds){
			addChild("INDIVIDUAL", 1, id);
		}
		for(String id : familyIds){
			addChild("FAMILY", 1, id);
		}

		updateChildValue("EVENT", eventsArea.getText().trim());
		updateChildValue("NOTE", notesArea.getText().trim());
		updateChildValue("SOURCE_CITATION", sourcesArea.getText().trim());

		if(isNew) model.addRecord(record);
		dispose();
	}

	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("GROUP");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "GROUP", "G");
	}

	// ==================== Main for testing ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		// Create a sample model with some data
		FLEFModel model = new FLEFModel();

		// Add a sample individual
		FLEFRecord ind1 = new FLEFRecord();
		ind1.setId("I1");
		ind1.setType("INDIVIDUAL");
		FLEFRecord name1 = new FLEFRecord();
		name1.setLevel(1);
		name1.setTag("NAME");
		FLEFRecord given1 = new FLEFRecord();
		given1.setLevel(2);
		given1.setTag("INDIVIDUAL_NAME");
		given1.setValue("John");
		name1.addChild(given1);
		FLEFRecord family1 = new FLEFRecord();
		family1.setLevel(2);
		family1.setTag("FAMILY_NAME");
		family1.setValue("Doe");
		name1.addChild(family1);
		ind1.addChild(name1);
		model.addRecord(ind1);

		FLEFRecord ind2 = new FLEFRecord();
		ind2.setId("I2");
		ind2.setType("INDIVIDUAL");
		FLEFRecord name2 = new FLEFRecord();
		name2.setLevel(1);
		name2.setTag("NAME");
		FLEFRecord given2 = new FLEFRecord();
		given2.setLevel(2);
		given2.setTag("INDIVIDUAL_NAME");
		given2.setValue("Jane");
		name2.addChild(given2);
		FLEFRecord family2 = new FLEFRecord();
		family2.setLevel(2);
		family2.setTag("FAMILY_NAME");
		family2.setValue("Smith");
		name2.addChild(family2);
		ind2.addChild(name2);
		model.addRecord(ind2);

		// Add a sample group
		FLEFRecord group = new FLEFRecord();
		group.setId("G1");
		group.setType("GROUP");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		name.setValue("Test Group");
		group.addChild(name);
		FLEFRecord type = new FLEFRecord();
		type.setLevel(1);
		type.setTag("TYPE");
		type.setValue("neighborhood");
		group.addChild(type);
		FLEFRecord member1 = new FLEFRecord();
		member1.setLevel(1);
		member1.setTag("INDIVIDUAL");
		member1.setValue("I1");
		group.addChild(member1);
		FLEFRecord member2 = new FLEFRecord();
		member2.setLevel(1);
		member2.setTag("INDIVIDUAL");
		member2.setValue("I2");
		group.addChild(member2);
		model.addRecord(group);

		// Launch the test frame
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Group Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton editBtn = new JButton("Edit Group G1");
			editBtn.addActionListener(e -> {
				FLEFRecord rec = model.getRecordById("G1");
				if(rec != null){
					GroupDialog dialog = new GroupDialog(frame, model, rec);
					dialog.setVisible(true);
					System.out.println("Group updated. Members: " + rec.getChildren().stream()
						.filter(c -> "INDIVIDUAL".equals(c.getTag()) || "FAMILY".equals(c.getTag())).count());
				}
			});

			JButton newBtn = new JButton("New Group");
			newBtn.addActionListener(e -> {
				GroupDialog dialog = new GroupDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("New group created. Total groups: " + model.getRecordsByType("GROUP").size());
			});

			frame.add(editBtn);
			frame.add(newBtn);
			frame.setVisible(true);

			System.out.println("Test frame is visible. Click a button to open the dialog.");
		});
	}

}
