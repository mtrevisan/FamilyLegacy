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
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ImageCropDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.ImageIcon;


/**
 * Dialog for editing a FAMILY_RECORD according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * FAMILY_RECORD :=
 * n @<XREF:FAMILY>@ FAMILY    {1:1}
 *   +1 PARENT1 @<XREF:INDIVIDUAL>@    {0:1}
 *     +2 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 PARENT2 @<XREF:INDIVIDUAL>@    {0:1}
 *     +2 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 CHILD @<XREF:INDIVIDUAL>@    {0:M}
 *     +2 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 EVENT @<XREF:EVENT>@    {0:M}
 *   +1 <<GROUP_CITATION>>    {0:M}
 *   +1 CULTURAL_NORM @<XREF:RULE>@    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 PREFERRED_IMAGE @<XREF:SOURCE>@    {0:1}
 *     +2 CROP <CROP_COORDINATES>    {0:1}
 *   +1 RESTRICTION <confidential>    {0:1}
 *   +1 <<CONCLUSION_STRUCTURE>>    {0:M}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class FamilyDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212971L;


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new GroupCitationHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}

	// Preferred Image
	private String preferredImageId;
	private String preferredImageCrop;
	private final JButton preferredImageButton = new JButton();

	// Basic fields
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// Parent1
	private final JTextField parent1DisplayField = new JTextField(20);
	private String parent1Id;
	private final DefaultListModel<String> parent1NoteModel = new DefaultListModel<>();
	private final JList<String> parent1NoteList = new JList<>(parent1NoteModel);
	private final List<String> parent1NoteIds = new ArrayList<>();
	private final Map<String, String> parent1NoteDisplayMap = new HashMap<>();

	// Parent2
	private final JTextField parent2DisplayField = new JTextField(20);
	private String parent2Id;
	private final DefaultListModel<String> parent2NoteModel = new DefaultListModel<>();
	private final JList<String> parent2NoteList = new JList<>(parent2NoteModel);
	private final List<String> parent2NoteIds = new ArrayList<>();
	private final Map<String, String> parent2NoteDisplayMap = new HashMap<>();

	// Children
	private final DefaultListModel<String> childListModel = new DefaultListModel<>();
	private final JList<String> childList = new JList<>(childListModel);
	private final List<ChildEntry> childEntries = new ArrayList<>();

	// Events
	private final DefaultListModel<String> eventListModel = new DefaultListModel<>();
	private final JList<String> eventList = new JList<>(eventListModel);
	private final List<String> eventIds = new ArrayList<>();

	// Group Citations
	private final DefaultListModel<String> groupCitationListModel = new DefaultListModel<>();
	private final JList<String> groupCitationList = new JList<>(groupCitationListModel);
	private final List<FLEFRecord> groupCitationRecords = new ArrayList<>();

	// Cultural Norms
	private final DefaultListModel<String> culturalNormListModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormListModel);
	private final List<String> culturalNormIds = new ArrayList<>();

	// Notes
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// Source Citations
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// Modification
	private ModificationPanel modificationPanel;

	// Conclusion
	private ConclusionPanel conclusionPanel;


	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler(GroupHandler.TYPE);
	private final RecordTypeHandler<?> groupCitationHandler = HandlerRegistry.getHandler(GroupCitationHandler.TYPE);
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler(EventHandler.TYPE);
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler(CulturalNormHandler.TYPE);


	private static class ChildEntry{
		String childId;
		List<String> noteIds;

		ChildEntry(String childId, List<String> noteIds){
			this.childId = childId;
			this.noteIds = noteIds != null? noteIds: new ArrayList<>();
		}

		@Override
		public String toString(){
			return childId + (noteIds.isEmpty()? "": " (" + noteIds.size() + " notes)");
		}
	}


	public static FamilyDialog createNew(Frame parent, FLEFModel model){
		return new FamilyDialog(parent, model, null);
	}

	public static FamilyDialog createEdit(Frame parent, FLEFModel model, FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new FamilyDialog(parent, model, record);
	}


	private FamilyDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		initComponents();
		loadData();
		setMinimumSize(new Dimension(500, 550));
		pack();
		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFModel model, final FLEFRecord record){
		return (record == null
			? "New Family - " + FLEFRecordUtils.generateNewId(model, FamilyHandler.TYPE, FamilyHandler.ID_PREFIX) + "*"
			: "Edit Family - " + record.getId());
	}


	@Override
	protected void initComponents(){
		modificationPanel = new ModificationPanel(model, this);
		conclusionPanel = new ConclusionPanel(model, this);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);

		setLayout(new MigLayout("fillx"));
		add(tabbedPane, "growx,push");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}


	// ==================== Preferred Image Management ====================

	/**
	 * Creates a placeholder icon for the image button.
	 *
	 * @return the placeholder icon
	 */
	private Icon createPlaceholderIcon(){
		BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, 80, 80);
		g2.setColor(Color.DARK_GRAY);
		g2.drawString("No img", 10, 45);
		g2.dispose();
		return new ImageIcon(img);
	}

	/**
	 * Opens the source selection dialog and, if a source is selected,
	 * loads the image and opens the crop dialog.
	 */
	private void selectAndCropImage(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, sourceHandler,
			selectedId -> result[0] = selectedId);
		dialog.setVisible(true);
		String sourceId = result[0];
		if(sourceId == null)
			return;

		BufferedImage image = loadImageFromSource(sourceId);
		if(image == null){
			JOptionPane.showMessageDialog(this,
				"Could not load image from the selected source.",
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		//FIXME
		ImageCropDialog cropDialog = new ImageCropDialog(getParentFrame(), image);
		cropDialog.setVisible(true);

		Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			preferredImageId = sourceId;
			preferredImageCrop = cropRect.x + " " + cropRect.y + " " + cropRect.width + " " + cropRect.height;
			updateImageButton(sourceId);
		}
	}

	/**
	 * Loads the image from a SOURCE_RECORD.
	 *
	 * @param sourceId the ID of the source record
	 * @return the loaded BufferedImage, or null if not found
	 */
	private BufferedImage loadImageFromSource(String sourceId){
		FLEFRecord source = model.getRecordById(sourceId);
		if(source == null)
			return null;

		FLEFRecord doc = FLEFRecordUtils.findChild(source, "DOCUMENT_STRUCTURE");
		if(doc == null)
			return null;

		String filePath = FLEFRecordUtils.getChildValue(doc, "FILE");
		if(filePath == null || filePath.isEmpty())
			return null;

		try{
			File file = new File(filePath);
			if(!file.exists())
				return null;
			return ImageIO.read(file);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Updates the image button with a thumbnail of the selected image.
	 *
	 * @param sourceId the ID of the source record
	 */
	private void updateImageButton(String sourceId){
		BufferedImage img = loadImageFromSource(sourceId);
		if(img != null){
			Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
			preferredImageButton.setIcon(new ImageIcon(scaled));
		}
		else{
			preferredImageButton.setIcon(createPlaceholderIcon());
		}
	}

	/**
	 * Clears the preferred image (resets state and updates UI).
	 */
	private void clearImage(){
		preferredImageId = null;
		preferredImageCrop = null;
		preferredImageButton.setIcon(createPlaceholderIcon());
	}


	// ==================== Parents & Children Panel ====================
	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, wrap 1", "[grow]", "[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		preferredImageButton.setPreferredSize(new Dimension(80, 80));
		preferredImageButton.setIcon(createPlaceholderIcon());
		preferredImageButton.setToolTipText("Left-click to select an image, right-click for options");

		preferredImageButton.addActionListener(e -> selectAndCropImage());

		JPopupMenu imagePopup = new JPopupMenu();
		JMenuItem clearImageMenuItem = new JMenuItem("Clear");
		clearImageMenuItem.addActionListener(e -> clearImage());
		imagePopup.add(clearImageMenuItem);

		preferredImageButton.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					imagePopup.show(preferredImageButton, e.getX(), e.getY());
				}
			}
			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					imagePopup.show(preferredImageButton, e.getX(), e.getY());
				}
			}
		});

		panel.add(preferredImageButton, "growx, align center");

		JPanel parent1Panel = createParentPanel("Parent 1:", parent1DisplayField,
			parent1NoteList, parent1NoteModel, parent1NoteIds, parent1NoteDisplayMap,
			this::createNewParent1, this::browseParent1, this::editParent1, this::clearParent1, this::notesParent1);
		panel.add(parent1Panel, "growx");

		JPanel parent2Panel = createParentPanel("Parent 2:", parent2DisplayField,
			parent2NoteList, parent2NoteModel, parent2NoteIds, parent2NoteDisplayMap,
			this::createNewParent2, this::browseParent2, this::editParent2, this::clearParent2, this::notesParent2);
		panel.add(parent2Panel, "growx");

		JPanel childrenPanel = createChildrenPanel();
		panel.add(childrenPanel, "growx");

		panel.add(restrictionCheckBox, "growx,align left");

		return panel;
	}


	// ==================== Children Panel ====================
	private JPanel createChildrenPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Children"));
		childList.setVisibleRowCount(4);
		childList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// ---- Popup menu ----
		GUIHelper.installStandardBehaviour(childList,
			() -> (childList.getSelectedIndex() >= 0),
			this::createNewChild,
			this::addChild,
			this::editChild,
			this::deleteChild,
			() -> {
				final int idx = childList.getSelectedIndex();
				if(idx != -1){
					final ChildEntry existing = childEntries.get(idx);
					final ChildEntry updated = showChildNotesDialog(existing.childId, existing);
					if(updated != null){
						childEntries.set(idx, updated);
						childListModel.set(idx, updated.toString());
					}
				}
			});

		// ---- Scroll pane ----
		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(childList,
			ScrollableContainerHost.ScrollType.VERTICAL));
		scrollPane.setPreferredSize(childList.getPreferredScrollableViewportSize());
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}


	// ==================== Parent Panel ====================
	private JPanel createParentPanel(String label, JTextField displayField,
			JList<String> noteList, DefaultListModel<String> noteModel,
			List<String> noteIds, Map<String, String> noteDisplayMap,
			Runnable actionNew, Runnable actionBrowse, Runnable actionEdit, Runnable actionClear, Runnable actionNotes){
		noteList.setVisibleRowCount(4);

		JPanel panel = new JPanel(new MigLayout("insets n n 0 n", "[right]rel[grow][][]", "[]5[]"));
		panel.setBorder(new TitledBorder(label));

		displayField.setEditable(false);
		displayField.setBackground(UIManager.getColor("TextField.background"));

		GUIHelper.installStandardBehaviour(displayField,
			() -> !displayField.getText().isBlank(),
			actionNew,
			actionBrowse,
			actionEdit,
			actionClear,
			actionNotes);

		JPanel idPanel = new JPanel(new MigLayout("ins 0,gap 0,fill", "[grow]", ""));
		idPanel.add(displayField, "grow");
		panel.add(idPanel, "span 3,growx,wrap");
		return panel;
	}

	// ==================== Generic List Panel ====================
	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
			Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installStandardBehaviour(list,
			() -> (list.getSelectedIndex() >= 0),
			() -> createNewItemForList(list, model),
			addAction,
			editAction,
			deleteAction,
			null);

		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(list,
			ScrollableContainerHost.ScrollType.VERTICAL));
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}


	// ==================== References Panel ====================
	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, wrap 1", "[grow]", "[]5[]5[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createListPanel("Events",
				eventList, eventListModel,
				this::addEvent, this::editEvent, this::deleteEvent),
			"growx");

		panel.add(createListPanel("Group Citations",
				groupCitationList, groupCitationListModel,
				this::addGroupCitation, this::editGroupCitation, this::deleteGroupCitation),
			"growx");

		panel.add(createListPanel("Cultural Norms",
				culturalNormList, culturalNormListModel,
				this::addCulturalNorm, this::editCulturalNorm, this::deleteCulturalNorm),
			"growx");

		panel.add(createListPanel("Notes",
				noteList, noteListModel,
				this::addNote, this::editNote, this::deleteNote),
			"growx");

		panel.add(createListPanel("Source Citations",
				sourceCitationList, sourceCitationListModel,
				this::addSourceCitation, this::editSourceCitation, this::deleteSourceCitation),
			"growx");

		return panel;
	}


	// ==================== Parent methods ====================
	private void createNewParent1(){
		createNewParent(id -> {
			parent1Id = id;
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null && individualHandler != null)
				parent1DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				parent1DisplayField.setText(id);
		});
	}

	private void browseParent1(){
		browseParent(selectedId -> {
			parent1Id = selectedId;
			FLEFRecord rec = model.getRecordById(selectedId);
			if(rec != null && individualHandler != null)
				parent1DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				parent1DisplayField.setText(selectedId);
		});
	}

	private void editParent1(){
		editIndividual(parent1Id);

		FLEFRecord rec = model.getRecordById(parent1Id);
		parent1DisplayField.setText(individualHandler.getDisplayName(rec));
	}

	private void clearParent1(){
		parent1Id = null;
		parent1DisplayField.setText("");
	}

	//TODO
	private void notesParent1(){
		parent1Id = null;
		parent1DisplayField.setText("");
	}

	private void createNewParent2(){
		createNewParent(id -> {
			parent2Id = id;
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null && individualHandler != null)
				parent2DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				parent2DisplayField.setText(id);
		});
	}

	private void browseParent2(){
		browseParent(selectedId -> {
			parent2Id = selectedId;
			FLEFRecord rec = model.getRecordById(selectedId);
			if(rec != null && individualHandler != null)
				parent2DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				parent2DisplayField.setText(selectedId);
		});
	}

	private void editParent2(){
		editIndividual(parent2Id);

		FLEFRecord rec = model.getRecordById(parent2Id);
		parent2DisplayField.setText(individualHandler.getDisplayName(rec));
	}

	private void clearParent2(){
		parent2Id = null;
		parent2DisplayField.setText("");
	}

	//TODO
	private void notesParent2(){
		parent2Id = null;
		parent2DisplayField.setText("");
	}

	private void createNewParent(Consumer<String> onCreated){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = individualHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty())
			onCreated.accept(newId);
	}

	private void browseParent(Consumer<String> onSelected){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, onSelected);
		dialog.setVisible(true);
	}

	private void editIndividual(String individualId){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if(individualId == null)
			return;

		FLEFRecord rec = model.getRecordById(individualId);
		if(rec == null)
			return;

		JDialog dialog = individualHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
	}


	// ==================== Parent note methods ====================
	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return noteHandler.getDisplayName(rec);
		}
		return id;
	}

	private void addParent1Note(){
		addNoteToList(parent1NoteModel, parent1NoteIds, parent1NoteDisplayMap);
	}

	private void editParent1Note(){
		editNoteFromList(parent1NoteList, parent1NoteModel, parent1NoteIds, parent1NoteDisplayMap);
	}

	private void deleteParent1Note(){
		deleteNoteFromList(parent1NoteList, parent1NoteModel, parent1NoteIds, parent1NoteDisplayMap);
	}

	private void addParent2Note(){
		addNoteToList(parent2NoteModel, parent2NoteIds, parent2NoteDisplayMap);
	}

	private void editParent2Note(){
		editNoteFromList(parent2NoteList, parent2NoteModel, parent2NoteIds, parent2NoteDisplayMap);
	}

	private void deleteParent2Note(){
		deleteNoteFromList(parent2NoteList, parent2NoteModel, parent2NoteIds, parent2NoteDisplayMap);
	}

	private void addNoteToList(DefaultListModel<String> defaultListModel, List<String> ids, Map<String, String> displayMap){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		RecordBrowserDialog<?> dialog = new RecordBrowserDialog<>(
			getParentFrame(), model, noteHandler, selectedId -> {
			if(selectedId != null && !ids.contains(selectedId)){
				ids.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				displayMap.put(selectedId, display);
				defaultListModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNoteFromList(JList<String> list, DefaultListModel<String> defaultListModel,
			List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1)
			return;
		String id = ids.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return;
		JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		displayMap.put(id, newDisplay);
		defaultListModel.set(idx, newDisplay);
	}

	private void deleteNoteFromList(JList<String> list, DefaultListModel<String> defaultListModel,
		List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this note?"))
			return;
		String removedId = ids.remove(idx);
		displayMap.remove(removedId);
		defaultListModel.remove(idx);
	}

	private void createNewNote(DefaultListModel<String> defaultListModel, List<String> ids, Map<String, String> displayMap){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Set<String> before = new HashSet<>(ids);
		JDialog dialog = noteHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !ids.contains(id)){
				ids.add(id);
				String display = getNoteDisplayName(id);
				displayMap.put(id, display);
				defaultListModel.addElement(display);
				break;
			}
		}
	}


	// ==================== Child methods ====================
	private void addChild(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null){
				final ChildEntry entry = new ChildEntry(selectedId, new ArrayList<>());
				childEntries.add(entry);
				childListModel.addElement(entry.toString());
			}
		});
		dialog.setVisible(true);
	}

	private void createNewChild(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = individualHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty()){
			ChildEntry entry = new ChildEntry(newId, new ArrayList<>());
			childEntries.add(entry);
			childListModel.addElement(entry.toString());
		}
	}

	private void editChild(){
		int idx = childList.getSelectedIndex();
		if(idx == -1)
			return;

		ChildEntry existing = childEntries.get(idx);
		FLEFRecord rec = model.getRecordById(existing.childId);
		if(rec == null)
			return;

		JDialog dialog = individualHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);

		String newDisplay = getNoteDisplayName(existing.childId);
		noteDisplayMap.put(existing.childId, newDisplay);
		childListModel.set(idx, newDisplay);
	}

	private void deleteChild(){
		int idx = childList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this child?"))
			return;

		childEntries.remove(idx);
		childListModel.remove(idx);
	}

	private ChildEntry showChildNotesDialog(String childId, ChildEntry childEntry){
		JDialog dialog = new JDialog(this, "Child Notes", true);
		dialog.setLayout(new MigLayout("fillx"));
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(new JLabel("Child ID:"), "align label");
		panel.add(new JLabel(childId), "growx,wrap");

		DefaultListModel<String> noteModel = new DefaultListModel<>();
		JList<String> noteList = new JList<>(noteModel);
		List<String> noteIds = new ArrayList<>(childEntry != null? childEntry.noteIds: new ArrayList<>());
		Map<String, String> displayMap = new HashMap<>();
		for(String id : noteIds){
			displayMap.put(id, getNoteDisplayName(id));
			noteModel.addElement(displayMap.get(id));
		}

		GUIHelper.installStandardBehaviour(childList,
			() -> (childList.getSelectedIndex() >= 0),
			() -> createNewNote(noteModel, noteIds, displayMap),
			() -> addNoteToList(noteModel, noteIds, displayMap),
			() -> editNoteFromList(noteList, noteModel, noteIds, displayMap),
			() -> deleteNoteFromList(noteList, noteModel, noteIds, displayMap),
			() -> {
				final int idx = childList.getSelectedIndex();
				if(idx != -1){
					final ChildEntry existing = childEntries.get(idx);
					final ChildEntry updated = showChildNotesDialog(existing.childId, existing);
					if(updated != null){
						childEntries.set(idx, updated);
						childListModel.set(idx, updated.toString());
					}
				}
			});

		JPanel notePanel = new JPanel(new MigLayout("fillx"));
		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(noteList,
			ScrollableContainerHost.ScrollType.VERTICAL));
		scrollPane.setPreferredSize(noteList.getPreferredScrollableViewportSize());
		notePanel.add(scrollPane, "growx,wrap");

		panel.add(notePanel, "growx,wrap");
		dialog.add(panel, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		final ChildEntry[] result = {null};
		okBtn.addActionListener(e -> {
			result[0] = new ChildEntry(childId, new ArrayList<>(noteIds));
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		return result[0];
	}


	// ==================== Event methods ====================
	private void addEvent(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, eventHandler, selectedId -> {
			if(selectedId != null && !eventIds.contains(selectedId)){
				eventIds.add(selectedId);
				eventListModel.addElement(getEventDisplayName(selectedId));
			}
		});
		dialog.setVisible(true);
	}

	private void editEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = eventIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return;
		JDialog dialog = eventHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		eventListModel.set(idx, getEventDisplayName(id));
	}

	private void deleteEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this event?"))
			return;
		eventIds.remove(idx);
		eventListModel.remove(idx);
	}

	private String getEventDisplayName(String id){
		if(eventHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return eventHandler.getDisplayName(rec);
		}
		return id;
	}

	private void createNewEvent(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("EVENT")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = eventHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("EVENT")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty() && !eventIds.contains(newId)){
			eventIds.add(newId);
			eventListModel.addElement(getEventDisplayName(newId));
		}
	}


	// ==================== Group Citation methods ====================
	private void addGroupCitation(){
		addGroupCitation(null);
	}

	private boolean addGroupCitation(String preSelectedGroupId){
		if(groupCitationHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Group Citation handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		GroupCitationDialog dialog;
		if(preSelectedGroupId != null && !preSelectedGroupId.isEmpty()){
			FLEFRecord citationRecord = new FLEFRecord();
			citationRecord.setValue(preSelectedGroupId);
			citationRecord.setLevel(1);
			citationRecord.setTag("GROUP_CITATION");
			dialog = (GroupCitationDialog)groupCitationHandler.createEditDialog(getParentFrame(), model, citationRecord);
		}
		else{
			dialog = (GroupCitationDialog)groupCitationHandler.createNewDialog(getParentFrame(), model);
		}
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setLevel(1);
				citation.setTag("GROUP_CITATION");
				groupCitationRecords.add(citation);
				groupCitationListModel.addElement(getGroupCitationDisplay(citation));
				return true;
			}
		}
		return false;
	}

	private void editGroupCitation(){
		if(groupCitationHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Group Citation handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int idx = groupCitationList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = groupCitationRecords.get(idx);
		GroupCitationDialog dialog = (GroupCitationDialog)groupCitationHandler.createEditDialog(getParentFrame(), model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				groupCitationRecords.set(idx, updated);
				groupCitationListModel.set(idx, getGroupCitationDisplay(updated));
			}
		}
	}

	private void deleteGroupCitation(){
		int idx = groupCitationList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this group citation?"))
			return;
		groupCitationRecords.remove(idx);
		groupCitationListModel.remove(idx);
	}

	private String getGroupCitationDisplay(FLEFRecord citation){
		String groupId = citation.getValue();
		if(groupId != null){
			FLEFRecord rec = model.getRecordById(groupId);
			if(rec != null && groupHandler != null)
				return groupHandler.getDisplayName(rec);
			return groupId;
		}
		return "[empty]";
	}

	private void createNewGroup(){
		if(groupHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Group handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("GROUP")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = groupHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newGroupId = null;
		for(FLEFRecord rec : model.getRecordsByType("GROUP")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newGroupId = id;
				break;
			}
		}
		if(newGroupId != null && !newGroupId.isEmpty()){
			boolean saved = addGroupCitation(newGroupId);
			if(!saved){
				model.removeRecord(newGroupId);
				JOptionPane.showMessageDialog(getParentFrame(),
					"Group creation cancelled. The group record has been removed.",
					"Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}


	// ==================== Cultural Norm methods ====================
	private void addCulturalNorm(){
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Cultural Norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				culturalNormListModel.addElement(getCulturalNormDisplayName(selectedId));
			}
		});
		dialog.setVisible(true);
	}

	private void editCulturalNorm(){
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Cultural Norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = culturalNormIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return;
		JDialog dialog = culturalNormHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		culturalNormListModel.set(idx, getCulturalNormDisplayName(id));
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this cultural norm?"))
			return;
		culturalNormIds.remove(idx);
		culturalNormListModel.remove(idx);
	}

	private String getCulturalNormDisplayName(String id){
		if(culturalNormHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return culturalNormHandler.getDisplayName(rec);
		}
		return id;
	}

	private void createNewCulturalNorm(){
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Cultural Norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = culturalNormHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty() && !culturalNormIds.contains(newId)){
			culturalNormIds.add(newId);
			culturalNormListModel.addElement(getCulturalNormDisplayName(newId));
		}
	}


	// ==================== Note methods ====================
	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
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
		});
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null)
			return;
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
		String id = noteIds.remove(idx);
		noteDisplayMap.remove(id);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		createNewNote(noteListModel, noteIds, noteDisplayMap);
	}


	// ==================== Source Citation methods ====================
	private void addSourceCitation(){
		addSourceCitation(null);
	}

	private boolean addSourceCitation(String preSelectedSourceId){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Source Citation handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		FLEFRecord citationRecord;
		if(preSelectedSourceId != null && !preSelectedSourceId.isEmpty()){
			citationRecord = new FLEFRecord();
			citationRecord.setValue(preSelectedSourceId);
			citationRecord.setLevel(1);
			citationRecord.setTag("SOURCE_CITATION");
		}
		else{
			citationRecord = null;
		}
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, citationRecord);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setLevel(1);
				citation.setTag("SOURCE_CITATION");
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
				return true;
			}
		}
		return false;
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
			if(rec != null && sourceHandler != null)
				return sourceHandler.getDisplayName(rec);
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = sourceHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty()){
			addSourceCitation(newId);
		}
	}


	// ==================== createNewItemForList ====================
	private void createNewItemForList(JList<String> list, DefaultListModel<String> model){
		if(list == eventList){
			createNewEvent();
		}
		else if(list == groupCitationList){
			createNewGroup();
		}
		else if(list == culturalNormList){
			createNewCulturalNorm();
		}
		else if(list == noteList){
			createNewNote();
		}
		else if(list == sourceCitationList){
			createNewSource();
		}
	}


	// ==================== Load Data ====================
	@Override
	protected void loadData(){
		setTitle(buildTitle(model, record));

		restrictionCheckBox.setSelected("confidential".equals(FLEFRecordUtils.getChildValue(record, "RESTRICTION")));

		// Parent1
		FLEFRecord p1 = FLEFRecordUtils.findChild(record, "PARENT1");
		if(p1 != null){
			String id = p1.getValue();
			if(id != null && !id.isEmpty()){
				parent1Id = id;
				FLEFRecord rec = model.getRecordById(id);
				if(rec != null && individualHandler != null)
					parent1DisplayField.setText(individualHandler.getDisplayName(rec));
				else
					parent1DisplayField.setText(id);
			}
			loadParentNotes(p1, parent1NoteModel, parent1NoteIds, parent1NoteDisplayMap);
		}

		// Parent2
		FLEFRecord p2 = FLEFRecordUtils.findChild(record, "PARENT2");
		if(p2 != null){
			String id = p2.getValue();
			if(id != null && !id.isEmpty()){
				parent2Id = id;
				FLEFRecord rec = model.getRecordById(id);
				if(rec != null && individualHandler != null)
					parent2DisplayField.setText(individualHandler.getDisplayName(rec));
				else
					parent2DisplayField.setText(id);
			}
			loadParentNotes(p2, parent2NoteModel, parent2NoteIds, parent2NoteDisplayMap);
		}

		// Children
		childEntries.clear();
		childListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CHILD".equals(child.getTag()) && child.getValue() != null){
				String childId = child.getValue();
				List<String> notes = new ArrayList<>();
				for(FLEFRecord noteChild : child.getChildren()){
					if("NOTE".equals(noteChild.getTag()) && noteChild.getValue() != null){
						notes.add(noteChild.getValue());
					}
				}
				childEntries.add(new ChildEntry(childId, notes));
				childListModel.addElement(childEntries.getLast().toString());
			}
		}

		// Events
		eventIds.clear();
		eventListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("EVENT".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				eventIds.add(id);
				eventListModel.addElement(getEventDisplayName(id));
			}
		}

		// Group Citations
		groupCitationRecords.clear();
		groupCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("GROUP_CITATION".equals(child.getTag())){
				groupCitationRecords.add(child);
				groupCitationListModel.addElement(getGroupCitationDisplay(child));
			}
		}

		// Cultural Norms
		culturalNormIds.clear();
		culturalNormListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				culturalNormListModel.addElement(getCulturalNormDisplayName(id));
			}
		}

		// Notes
		noteIds.clear();
		noteListModel.clear();
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

		// Source Citations
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// Preferred Image
		FLEFRecord pref = FLEFRecordUtils.findChild(record, "PREFERRED_IMAGE");
		if(pref != null){
			preferredImageId = pref.getValue();
			preferredImageCrop = FLEFRecordUtils.getChildValue(pref, "CROP");
			updateImageButton(preferredImageId);
		}
		else{
			clearImage();
		}

		// Modification
		modificationPanel.loadFromRecord(record);

		// Conclusion
		FLEFRecord conclusion = FLEFRecordUtils.findChild(record, "CONCLUSION");
		conclusionPanel.loadFromRecord(conclusion);
	}

	private void loadParentNotes(FLEFRecord parentRec, DefaultListModel<String> model,
		List<String> ids, Map<String, String> displayMap){
		model.clear();
		ids.clear();
		displayMap.clear();
		for(FLEFRecord child : parentRec.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				ids.add(id);
				String display = getNoteDisplayName(id);
				displayMap.put(id, display);
				model.addElement(display);
			}
		}
	}


	// ==================== Validation ====================
	@Override
	protected boolean validateData(){
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required for a family.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(!modificationPanel.validateRequiredFields()){
			return false;
		}
		return (!conclusionPanel.hasData() || conclusionPanel.validateRequiredFields());
	}


	// ==================== Save ====================
	@Override
	protected void saveRecord(){
		record.getChildren().clear();

		// RESTRICTION
		FLEFRecordUtils.updateChildValue(record, "RESTRICTION",
			restrictionCheckBox.isSelected()? "confidential": null);

		// PARENT1
		if(parent1Id != null && !parent1Id.isEmpty()){
			FLEFRecord p1 = new FLEFRecord();
			p1.setLevel(1);
			p1.setTag("PARENT1");
			p1.setValue(parent1Id);
			record.addChild(p1);
			for(String noteId : parent1NoteIds){
				FLEFRecordUtils.addChild(p1, "NOTE", 2, noteId);
			}
		}

		// PARENT2
		if(parent2Id != null && !parent2Id.isEmpty()){
			FLEFRecord p2 = new FLEFRecord();
			p2.setLevel(1);
			p2.setTag("PARENT2");
			p2.setValue(parent2Id);
			record.addChild(p2);
			for(String noteId : parent2NoteIds){
				FLEFRecordUtils.addChild(p2, "NOTE", 2, noteId);
			}
		}

		// CHILDREN
		for(ChildEntry entry : childEntries){
			FLEFRecord child = new FLEFRecord();
			child.setLevel(1);
			child.setTag("CHILD");
			child.setValue(entry.childId);
			record.addChild(child);
			for(String noteId : entry.noteIds){
				FLEFRecordUtils.addChild(child, "NOTE", 2, noteId);
			}
		}

		// EVENTS
		for(String id : eventIds){
			FLEFRecordUtils.addChild(record, "EVENT", 1, id);
		}

		// GROUP CITATIONS
		for(FLEFRecord citation : groupCitationRecords){
			citation.setLevel(1);
			citation.setTag("GROUP_CITATION");
			record.addChild(citation);
		}

		// CULTURAL NORMS
		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(record, "CULTURAL_NORM", 1, id);
		}

		// NOTES
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		// SOURCE CITATIONS
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// PREFERRED IMAGE
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			FLEFRecord pref = new FLEFRecord();
			pref.setLevel(1);
			pref.setTag("PREFERRED_IMAGE");
			pref.setValue(preferredImageId);
			record.addChild(pref);
			if(preferredImageCrop != null && !preferredImageCrop.isEmpty()){
				FLEFRecordUtils.updateChildValue(pref, "CROP", preferredImageCrop);
			}
		}

		// MODIFICATION
		modificationPanel.saveToRecord(record);

		// CONCLUSION
		if(conclusionPanel.hasData()){
			FLEFRecord conclusion = conclusionPanel.saveToRecord(null);
			if(conclusion != null){
				conclusion.setLevel(1);
				conclusion.setTag("CONCLUSION");
				record.addChild(conclusion);
			}
		}

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}


	// ==================== Overrides ====================
	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), FamilyHandler.TYPE);
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, FamilyHandler.TYPE, FamilyHandler.ID_PREFIX);
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Family Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Family");
			btn.addActionListener(e -> {
				FamilyDialog dialog = FamilyDialog.createNew(frame, model);
				dialog.setVisible(true);
				System.out.println("Family saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
