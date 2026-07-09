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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for editing a FAMILY_RECORD.
 * <p>
 * Features:
 * <ul>
 *   <li>Basic information: ID, Partner 1, Partner 2</li>
 *   <li>Children list with display names (e.g., "John Doe (I1)")</li>
 *   <li>Double-click on a child opens the IndividualDialog for editing</li>
 *   <li>"Add Child" opens a selection dialog with search filter</li>
 *   <li>Placeholder tabs for Events, Group Citations, Cultural Norms, Notes, Sources, Conclusions, Modification</li>
 *   <li>Restriction (Confidential checkbox)</li>
 * </ul>
 */
public class FamilyDialog extends BaseRecordDialog {

	// Static initializer to register handlers
	static {
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		// Other handlers will be registered by the application
	}

	// ========== Basic info components ==========
	private final JTextField idField = new JTextField(10);
	private final JTextField partner1Field = new JTextField(15);
	private final JTextField partner2Field = new JTextField(15);

	// ========== Children management ==========
	private final DefaultListModel<String> childrenListModel = new DefaultListModel<>();
	private final JList<String> childrenList = new JList<>(childrenListModel);
	private final List<String> childIds = new ArrayList<>(); // Parallel list for IDs

	// ========== Placeholder tabs ==========
	private final JTextArea eventsArea = new JTextArea(5, 30);
	private final JTextArea groupCitationsArea = new JTextArea(5, 30);
	private final JTextArea culturalNormsArea = new JTextArea(5, 30);
	private final JTextArea notesArea = new JTextArea(5, 30);
	private final JTextArea sourcesArea = new JTextArea(5, 30);
	private final JTextArea conclusionsArea = new JTextArea(5, 30);
	private final JTextArea modificationArea = new JTextArea(5, 30);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handler for child records ==========
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");

	/**
	 * Creates a dialog to edit an existing family record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model containing the record
	 * @param record the record to edit (must be of type FAMILY)
	 */
	public FamilyDialog(Frame parent, FLEFModel model, FLEFRecord record) {
		super(parent, model, record, "Edit Family");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(700, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * Creates a dialog to create a new family record.
	 *
	 * @param parent the parent frame
	 * @param model  the FLEF model where the new record will be added
	 */
	public FamilyDialog(Frame parent, FLEFModel model) {
		super(parent, model, "New Family");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(700, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents() {
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// ===== Basic tab =====
		JPanel basicPanel = new JPanel(new MigLayout("fill", "[right]rel[grow]"));
		idField.setEditable(false);
		idField.setText(record.getId());
		basicPanel.add(new JLabel("ID:"), "align label");
		basicPanel.add(idField, "grow,wrap");
		basicPanel.add(new JLabel("Partner 1 (ID):"), "align label");
		basicPanel.add(partner1Field, "grow,wrap");
		basicPanel.add(new JLabel("Partner 2 (ID):"), "align label");
		basicPanel.add(partner2Field, "grow,wrap");
		basicPanel.add(restrictionCheckBox, "span 2,wrap");
		tabbedPane.addTab("Basic", basicPanel);

		// ===== Children tab =====
		JPanel childrenPanel = new JPanel(new BorderLayout(5, 5));
		childrenPanel.setBorder(new TitledBorder("Children"));

		// List with double-click support
		childrenList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		childrenList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editSelectedChild();
				}
			}
		});
		JScrollPane listScroll = new JScrollPane(childrenList);
		childrenPanel.add(listScroll, BorderLayout.CENTER);

		// Buttons panel
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addChildBtn = new JButton("Add Child");
		JButton removeChildBtn = new JButton("Remove Child");
		JButton editChildBtn = new JButton("Edit Child");
		btnPanel.add(addChildBtn);
		btnPanel.add(editChildBtn);
		btnPanel.add(removeChildBtn);
		childrenPanel.add(btnPanel, BorderLayout.SOUTH);

		tabbedPane.addTab("Children", childrenPanel);

		// ===== Other tabs (placeholders) =====
		tabbedPane.addTab("Events", createPlaceholderPanel(eventsArea, "Events (one per line)"));
		tabbedPane.addTab("Group Citations", createPlaceholderPanel(groupCitationsArea, "Group Citations"));
		tabbedPane.addTab("Cultural Norms", createPlaceholderPanel(culturalNormsArea, "Cultural Norms"));
		tabbedPane.addTab("Notes", createPlaceholderPanel(notesArea, "Notes"));
		tabbedPane.addTab("Sources", createPlaceholderPanel(sourcesArea, "Sources"));
		tabbedPane.addTab("Conclusions", createPlaceholderPanel(conclusionsArea, "Conclusions"));
		tabbedPane.addTab("Modification", createPlaceholderPanel(modificationArea, "Modification"));

		add(tabbedPane, BorderLayout.CENTER);

		// ===== Button panel =====
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		// ===== Event listeners =====
		saveButton.addActionListener(e -> saveRecord());
		cancelButton.addActionListener(e -> dispose());

		addChildBtn.addActionListener(e -> addChild());
		editChildBtn.addActionListener(e -> editSelectedChild());
		removeChildBtn.addActionListener(e -> removeChild());

		childrenList.addListSelectionListener(e -> {
			boolean selected = childrenList.getSelectedIndex() != -1;
			editChildBtn.setEnabled(selected);
			removeChildBtn.setEnabled(selected);
		});
		editChildBtn.setEnabled(false);
		removeChildBtn.setEnabled(false);
	}

	/**
	 * Helper to create a placeholder panel with a text area.
	 */
	private JPanel createPlaceholderPanel(JTextArea area, String title) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(title));
		JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(200, 150));
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	@Override
	protected void loadData() {
		// Basic info
		idField.setText(record.getId());
		partner1Field.setText(getChildValue("PARTNER1"));
		partner2Field.setText(getChildValue("PARTNER2"));
		restrictionCheckBox.setSelected("confidential".equals(getChildValue("RESTRICTION")));

		// Load children
		loadChildren();

		// Placeholder tabs
		eventsArea.setText(getChildValue("EVENT"));
		groupCitationsArea.setText(getChildValue("GROUP_CITATION"));
		culturalNormsArea.setText(getChildValue("CULTURAL_NORM"));
		notesArea.setText(getChildValue("NOTE"));
		sourcesArea.setText(getChildValue("SOURCE_CITATION"));
		conclusionsArea.setText(getChildValue("CONCLUSION"));
		modificationArea.setText(getChildValue("MODIFICATION"));
	}

	/**
	 * Loads the children from the record into the list.
	 */
	private void loadChildren() {
		childrenListModel.clear();
		childIds.clear();

		for (FLEFRecord child : record.getChildren()) {
			if ("CHILD".equals(child.getTag()) && child.getValue() != null && !child.getValue().isEmpty()) {
				String childId = child.getValue();
				childIds.add(childId);

				String display = childId;
				FLEFRecord individual = model.getRecordById(childId);
				if (individual != null && individualHandler != null) {
					display = individualHandler.getDisplayName(individual);
				}
				childrenListModel.addElement(display);
			}
		}
	}

	/**
	 * Adds a new child via selection dialog.
	 */
	private void addChild() {
		if (individualHandler == null) {
			JOptionPane.showMessageDialog(this, "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> selectionDialog = new GenericSelectionDialog<>(
			getParentFrame(),
			model,
			individualHandler,
			selectedId -> {
				if (selectedId != null && !childIds.contains(selectedId)) {
					childIds.add(selectedId);
					FLEFRecord individual = model.getRecordById(selectedId);
					String display = (individual != null) ? individualHandler.getDisplayName(individual) : selectedId;
					childrenListModel.addElement(display);
				}
			}
		);
		selectionDialog.setVisible(true);
	}

	/**
	 * Edits the selected child by opening its IndividualDialog.
	 */
	private void editSelectedChild() {
		int idx = childrenList.getSelectedIndex();
		if (idx == -1 || idx >= childIds.size()) return;

		String childId = childIds.get(idx);
		FLEFRecord individual = model.getRecordById(childId);
		if (individual == null) {
			JOptionPane.showMessageDialog(this, "Child record not found: " + childId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (individualHandler == null) {
			JOptionPane.showMessageDialog(this, "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Open edit dialog for the individual
		JDialog editDialog = (JDialog) individualHandler.createEditDialog(getParentFrame(), model, individual);
		editDialog.setVisible(true);

		// Reload the children list (the individual might have been modified)
		loadChildren();
	}

	/**
	 * Removes the selected child from the list.
	 */
	private void removeChild() {
		int idx = childrenList.getSelectedIndex();
		if (idx == -1 || idx >= childIds.size()) return;

		String childId = childIds.get(idx);
		int confirm = JOptionPane.showConfirmDialog(
			this,
			"Remove child " + childId + " from this family?",
			"Confirm Remove",
			JOptionPane.YES_NO_OPTION
		);
		if (confirm == JOptionPane.YES_OPTION) {
			childIds.remove(idx);
			childrenListModel.remove(idx);
		}
	}

	/**
	 * Gets the parent frame of this dialog.
	 */
	private Frame getParentFrame() {
		Container parent = getParent();
		while (parent != null && !(parent instanceof Frame)) {
			parent = parent.getParent();
		}
		return (Frame) parent;
	}

	@Override
	protected void saveRecord() {
		// Save basic info
		updateChildValue("PARTNER1", partner1Field.getText().trim());
		updateChildValue("PARTNER2", partner2Field.getText().trim());

		// Rebuild children: remove existing and add from the list
		removeChildren("CHILD");
		for (String childId : childIds) {
			addChild("CHILD", 1, childId);
		}

		// Save restriction
		updateChildValue("RESTRICTION", restrictionCheckBox.isSelected() ? "confidential" : null);

		// Save placeholder tabs
		updateChildValue("EVENT", eventsArea.getText().trim());
		updateChildValue("GROUP_CITATION", groupCitationsArea.getText().trim());
		updateChildValue("CULTURAL_NORM", culturalNormsArea.getText().trim());
		updateChildValue("NOTE", notesArea.getText().trim());
		updateChildValue("SOURCE_CITATION", sourcesArea.getText().trim());
		updateChildValue("CONCLUSION", conclusionsArea.getText().trim());
		updateChildValue("MODIFICATION", modificationArea.getText().trim());

		// If new, add to model
		if (isNew) {
			model.addRecord(record);
		}
		dispose();
	}

	@Override
	protected FLEFRecord createNewRecord() {
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("FAMILY");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId() {
		return FLEFRecordUtils.generateNewId(model, "FAMILY", "F");
	}

	// ==================== Main for testing ====================

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}

		// Create a sample model with some data
		FLEFModel model = new FLEFModel();

		// Add sample individuals
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

		// Add a sample family
		FLEFRecord fam = new FLEFRecord();
		fam.setId("F1");
		fam.setType("FAMILY");
		FLEFRecord p1 = new FLEFRecord();
		p1.setLevel(1);
		p1.setTag("PARTNER1");
		p1.setValue("I1");
		fam.addChild(p1);
		FLEFRecord p2 = new FLEFRecord();
		p2.setLevel(1);
		p2.setTag("PARTNER2");
		p2.setValue("I2");
		fam.addChild(p2);
		FLEFRecord child = new FLEFRecord();
		child.setLevel(1);
		child.setTag("CHILD");
		child.setValue("I1");
		fam.addChild(child);
		model.addRecord(fam);

		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		// Launch the test frame
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Family Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton editBtn = new JButton("Edit Family F1");
			editBtn.addActionListener(e -> {
				FLEFRecord rec = model.getRecordById("F1");
				if (rec != null) {
					FamilyDialog dialog = new FamilyDialog(frame, model, rec);
					dialog.setVisible(true);
					System.out.println("Family updated. Children: " + rec.getChildren().stream()
						.filter(c -> "CHILD".equals(c.getTag())).count());
				}
			});

			JButton newBtn = new JButton("New Family");
			newBtn.addActionListener(e -> {
				FamilyDialog dialog = new FamilyDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("New family created. Total families: " + model.getRecordsByType("FAMILY").size());
			});

			frame.add(editBtn);
			frame.add(newBtn);
			frame.setVisible(true);

			System.out.println("Test frame is visible. Click a button to open the dialog.");
		});
	}
}