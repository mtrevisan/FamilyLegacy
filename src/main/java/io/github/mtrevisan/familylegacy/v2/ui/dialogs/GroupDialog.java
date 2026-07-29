/*
 * Copyright (c) 2026 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.CulturalNormListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.EventListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Dialog for editing a {@code GROUP_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * GROUP_RECORD :=
 * n @<XREF:GROUP>@ GROUP    {1:1}
 *   +1 TYPE <GROUP_TYPE>    {0:1}
 *   +1 <<NAME_STRUCTURE>>    {0:M}
 *   +1 CULTURAL_NORM @<XREF:CULTURAL_NORM>@    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 PREFERRED_IMAGE <RESOURCE_URI>    {0:1}
 *     +2 CROP <CROP_COORDINATES>    {0:1}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<CONCLUSION_STRUCTURE>>    {0:M}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class GroupDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212972L;

	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new GroupHandler());
	}

	private final BindingManager bindingManager = new BindingManager();

	// ---- UI components ----
	private final BoundComboBox<String> typeCombo = new BoundComboBox<>("TYPE",
		new String[]{StringUtils.EMPTY, "family", "household", "neighbourhood", "fraternity", "club", "research group",
			"literary society", "association", "organisation", "tribe"});

	private final NameListPanel namePanel;
	private final RestrictionPanel restrictionPanel;
	private final ConclusionPanel conclusionPanel;
	private final ModificationPanel modificationPanel;

	// ---- Lists using AbstractListPanel ----
	private final EventListPanel eventPanel;
	private final CulturalNormListPanel culturalNormPanel;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;

	// ---- Members (special handling for RELATIONSHIP) ----
	private final DefaultListModel<String> memberListModel = new DefaultListModel<>();
	private final JList<String> memberList = new JList<>(memberListModel);
	private final List<FLEFRecord> memberRelationshipRecords = new ArrayList<>();

	// ---- All other relationships ----
	private final DefaultListModel<String> relationshipListModel = new DefaultListModel<>();
	private final JList<String> relationshipList = new JList<>(relationshipListModel);
	private final List<FLEFRecord> otherRelationshipRecords = new ArrayList<>();

	// ---- Preferred Image ----
	private String preferredImageId;
	private String preferredImageCrop;
	private final JButton preferredImageButton = new JButton();

	// ---- Tabs ----
	private final JTabbedPane tabbedPane = new JTabbedPane();

	// ---- Factory methods ----
	public static GroupDialog createNew(Dialog parent, FLEFModel model){
		return new GroupDialog(parent, model, null);
	}

	public static GroupDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null){
			throw new IllegalArgumentException("Record cannot be null");
		}
		return new GroupDialog(parent, model, record);
	}

	// ---- Constructor ----
	private GroupDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupHandler.TYPE));

		// Initialize panels
		this.namePanel = new NameListPanel("NAME", this, model);
		this.restrictionPanel = new RestrictionPanel(this);
		this.conclusionPanel = new ConclusionPanel(model, this);
		this.modificationPanel = new ModificationPanel(this);
		this.eventPanel = new EventListPanel(model, this);
		this.culturalNormPanel = new CulturalNormListPanel(model, this);
		this.notePanel = new NoteListPanel("NOTE", model, this);
		this.sourcePanel = new SourceCitationListPanel("SOURCE", this, model);

		initComponents();
		loadData();
		setMinimumSize(new Dimension(550, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	// ---- Initialisation ----
	protected void initComponents(){
		bindingManager.bind(typeCombo);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		setLayout(new MigLayout("fillx, top"));
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	// ---- Main Panel ----
	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]5[]5[]10[]"));

		// Preferred Image
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

		// Type
		JPanel typePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typePanel.add(typeCombo, "growx");
		panel.add(typePanel, "growx");

		// Names
		panel.add(namePanel, "growx");

		// Members
		panel.add(createMembersPanel(), "growx");

		return panel;
	}

	// ---- Members panel ----
	private JPanel createMembersPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Members"));
		memberList.setVisibleRowCount(4);
		memberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(memberList,
			() -> memberList.getSelectedIndex() >= 0,
			this::editMemberRelationship,
			this::createNewMember,
			this::deleteMember,
			builder -> {
				builder.item("Create New...", this::createNewMember);
				builder.item("Add Existing...", this::addMember);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editMemberIndividual);
				builder.selectionSensitiveItem("Edit Relationship...", this::editMemberRelationship);
				builder.selectionSensitiveItem("Delete", this::deleteMember);
				builder.separator();
				builder.selectionSensitiveItem("Notes...", () -> {
					int idx = memberList.getSelectedIndex();
					if(idx != -1){
						FLEFRecord relationship = memberRelationshipRecords.get(idx);
						showRelationshipNotesDialog(relationship);
					}
				});
			});

		JScrollPane scrollPane = GUIHelper.createScrollPane(memberList);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	// ---- References Panel ----
	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]5[]"));

		panel.add(eventPanel, "growx");
		panel.add(createListPanel("Relationships (all)", relationshipList, relationshipListModel,
			this::addRelationship, this::editRelationship, this::deleteRelationship), "growx");
		panel.add(culturalNormPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");

		return panel;
	}

	// Helper to wrap a panel that already has its own border
	private JPanel createListPanel(String title, JPanel contentPanel){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));
		panel.add(contentPanel, "growx");
		return panel;
	}

	// Helper for lists that are not yet abstract (relationships)
	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
		Runnable createNewAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehavior(list,
			() -> list.getSelectedIndex() >= 0,
			editAction,
			createNewAction,
			deleteAction,
			builder -> {
				builder.item("Create New...", createNewAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Remove", deleteAction);
			});

		JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	// ---- Relationship Notes Dialog ----
	private void showRelationshipNotesDialog(FLEFRecord relationship){
		if(relationship == null) return;
		NoteListEditorDialog dialog = new NoteListEditorDialog(this, model, relationship);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			int idx = memberRelationshipRecords.indexOf(relationship);
			if(idx != -1){
				memberListModel.set(idx, getRelationshipDisplay(relationship));
			}
			int otherIdx = otherRelationshipRecords.indexOf(relationship);
			if(otherIdx != -1){
				relationshipListModel.set(otherIdx, getRelationshipDisplay(relationship));
			}
		}
	}

	// ---- Preferred Image ----
	private Icon createPlaceholderIcon(){
		BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, 80, 80);
		g2.setColor(Color.DARK_GRAY);
		g2.drawString("[No img]", 10, 45);
		g2.dispose();
		return new ImageIcon(img);
	}

	private void selectAndCropImage(){
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final String[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, sourceHandler, selectedId -> result[0] = selectedId);
		dialog.setVisible(true);
		String sourceId = result[0];
		if(sourceId == null) return;

		BufferedImage image = loadImageFromSource(sourceId);
		if(image == null){
			JOptionPane.showMessageDialog(this,
				"Could not load image from the selected source.",
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ImageCropDialog cropDialog = new ImageCropDialog(this, image);
		cropDialog.setVisible(true);

		Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			preferredImageId = sourceId;
			preferredImageCrop = cropRect.x + StringUtils.SPACE + cropRect.y + StringUtils.SPACE + cropRect.width + StringUtils.SPACE + cropRect.height;
			updateImageButton(sourceId);
		}
	}

	private BufferedImage loadImageFromSource(String sourceId){
		FLEFRecord source = model.getRecordById(sourceId);
		if(source == null) return null;

		FLEFRecord doc = FLEFRecordUtils.findChild(source, "DOCUMENT_STRUCTURE");
		if(doc == null) return null;

		String filePath = FLEFRecordUtils.getChildValue(doc, "FILE");
		if(filePath == null || filePath.isEmpty()) return null;

		try{
			File file = new File(filePath);
			if(!file.exists()) return null;
			return ImageIO.read(file);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

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

	private void clearImage(){
		preferredImageId = null;
		preferredImageCrop = null;
		preferredImageButton.setIcon(createPlaceholderIcon());
	}

	// ---- Member methods ----
	private void addMember(){
		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		final String[] selectedIndividualId = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, individualHandler, id -> selectedIndividualId[0] = id);
		dialog.setVisible(true);
		String individualId = selectedIndividualId[0];
		if(individualId == null) return;

		RelationshipDialog relDialog = new RelationshipDialog(
			this, model, null, getGroupId(), individualId);
		relDialog.setVisible(true);
		if(relDialog.isSaved()){
			FLEFRecord saved = relDialog.getCitationRecord();
			if(saved != null){
				saved.setTag("RELATIONSHIP");
				otherRelationshipRecords.add(saved);
				relationshipListModel.addElement(getRelationshipDisplay(saved));
				if(isMemberRelationship(saved)){
					memberRelationshipRecords.add(saved);
					memberListModel.addElement(getRelationshipDisplay(saved));
				}
			}
		}
	}

	private void createNewMember(){
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}

		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		JDialog dialog = individualHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		String newIndividualId = null;
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newIndividualId = id;
				break;
			}
		}

		if(newIndividualId != null){
			RelationshipDialog relDialog = new RelationshipDialog(
				this, model, null, getGroupId(), newIndividualId);
			relDialog.setVisible(true);
			if(relDialog.isSaved()){
				FLEFRecord saved = relDialog.getCitationRecord();
				if(saved != null){
					saved.setTag("RELATIONSHIP");
					otherRelationshipRecords.add(saved);
					relationshipListModel.addElement(getRelationshipDisplay(saved));
					if(isMemberRelationship(saved)){
						memberRelationshipRecords.add(saved);
						memberListModel.addElement(getRelationshipDisplay(saved));
					}
				}
			}
		}
	}

	private void editMemberRelationship(){
		int idx = memberList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord existing = memberRelationshipRecords.get(idx);
		final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
		RelationshipDialog dialog = (RelationshipDialog)relationshipHandler.createEditDialog(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				memberRelationshipRecords.set(idx, updated);
				memberListModel.set(idx, getRelationshipDisplay(updated));
				int otherIdx = otherRelationshipRecords.indexOf(existing);
				if(otherIdx != -1){
					otherRelationshipRecords.set(otherIdx, updated);
					relationshipListModel.set(otherIdx, getRelationshipDisplay(updated));
				}
			}
		}
	}

	private void editMemberIndividual(){
		int idx = memberList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord relationship = memberRelationshipRecords.get(idx);
		if(relationship == null) return;

		FLEFRecord objectChild = relationship.findChild("OBJECT");
		if(objectChild == null){
			JOptionPane.showMessageDialog(this, "No OBJECT found in this relationship.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String individualId = objectChild.getValue();
		if(individualId == null) return;

		FLEFRecord individual = model.getRecordById(individualId);
		if(individual == null){
			JOptionPane.showMessageDialog(this, "Individual record not found: " + individualId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		JDialog dialog = individualHandler.createEditDialog(this, model, individual);
		dialog.setVisible(true);

		memberListModel.set(idx, getRelationshipDisplay(relationship));
		int otherIdx = otherRelationshipRecords.indexOf(relationship);
		if(otherIdx != -1){
			relationshipListModel.set(otherIdx, getRelationshipDisplay(relationship));
		}
	}

	private void deleteMember(){
		int idx = memberList.getSelectedIndex();
		if(idx == -1) return;

		if(!showConfirm("Confirm", "Remove this member relationship?")) return;

		FLEFRecord removed = memberRelationshipRecords.remove(idx);
		memberListModel.remove(idx);
		otherRelationshipRecords.remove(removed);
		relationshipListModel.removeElement(getRelationshipDisplay(removed));
	}

	// ---- Relationship methods (for References tab) ----
	private void addRelationship(){
		final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
		RelationshipDialog dialog = (RelationshipDialog)relationshipHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord saved = dialog.getCitationRecord();
			if(saved != null){
				saved.setTag("RELATIONSHIP");
				otherRelationshipRecords.add(saved);
				relationshipListModel.addElement(getRelationshipDisplay(saved));
				if(isMemberRelationship(saved)){
					memberRelationshipRecords.add(saved);
					memberListModel.addElement(getRelationshipDisplay(saved));
				}
			}
		}
	}

	private void editRelationship(){
		int idx = relationshipList.getSelectedIndex();
		if(idx == -1) return;

		final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
		FLEFRecord existing = otherRelationshipRecords.get(idx);
		RelationshipDialog dialog = (RelationshipDialog)relationshipHandler.createEditDialog(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				otherRelationshipRecords.set(idx, updated);
				relationshipListModel.set(idx, getRelationshipDisplay(updated));
				refreshMemberList();
			}
		}
	}

	private void deleteRelationship(){
		int idx = relationshipList.getSelectedIndex();
		if(idx == -1) return;

		if(!showConfirm("Confirm", "Remove this relationship?")) return;

		otherRelationshipRecords.remove(idx);
		relationshipListModel.remove(idx);
		refreshMemberList();
	}

	private String getRelationshipDisplay(FLEFRecord relationship){
		String objectId = FLEFRecordUtils.getChildValue(relationship, "OBJECT");
		String role = FLEFRecordUtils.getChildValue(relationship, "ROLE");

		StringBuilder display = new StringBuilder();

		if(objectId != null){
			FLEFRecord obj = model.getRecordById(objectId);
			if(obj != null){
				final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
				display.append(individualHandler.getDisplayText(obj));
			}
			else{
				display.append(objectId);
			}
		}
		else{
			display.append("?");
		}

		if(role != null && !role.isEmpty()){
			display.append(" [").append(role).append("]");
		}

		return display.toString();
	}

	private boolean isMemberRelationship(FLEFRecord relationship){
		String subjectId = FLEFRecordUtils.getChildValue(relationship, "SUBJECT");
		String objectId = FLEFRecordUtils.getChildValue(relationship, "OBJECT");
		String groupId = getGroupId();

		if(groupId == null) return false;

		boolean groupIsSubject = groupId.equals(subjectId);
		boolean groupIsObject = groupId.equals(objectId);

		if(!groupIsSubject && !groupIsObject) return false;

		String otherId = groupIsSubject? objectId: subjectId;
		if(otherId == null) return false;

		FLEFRecord other = model.getRecordById(otherId);
		return other != null && "INDIVIDUAL".equals(other.getTag());
	}

	private void refreshMemberList(){
		memberRelationshipRecords.clear();
		memberListModel.clear();

		for(FLEFRecord rel : otherRelationshipRecords){
			if(isMemberRelationship(rel)){
				memberRelationshipRecords.add(rel);
				memberListModel.addElement(getRelationshipDisplay(rel));
			}
		}
	}

	// ---- Load Data ----
	@Override
	protected void loadData(){
		bindingManager.load(record);

		// NAME_STRUCTURE
		List<FLEFRecord> names = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if("NAME".equals(child.getTag())){
				names.add(child);
			}
		}
		namePanel.setItems(names);

		// Restriction
		FLEFRecord restrictionStruct = FLEFRecordUtils.findChild(record, "RESTRICTION");
		restrictionPanel.loadFromRecord(restrictionStruct);

		// Conclusion
		FLEFRecord conclusion = FLEFRecordUtils.findChild(record, "CONCLUSION");
		conclusionPanel.loadFromRecord(conclusion);

		// Clear all lists
		memberRelationshipRecords.clear();
		memberListModel.clear();
		otherRelationshipRecords.clear();
		relationshipListModel.clear();

		// Process children for lists
		List<String> eventIds = new ArrayList<>();
		List<String> culturalNormIds = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			String tag = child.getTag();

			if("RELATIONSHIP".equals(tag)){
				otherRelationshipRecords.add(child);
				relationshipListModel.addElement(getRelationshipDisplay(child));
				if(isMemberRelationship(child)){
					memberRelationshipRecords.add(child);
					memberListModel.addElement(getRelationshipDisplay(child));
				}
			}
			else if("EVENT".equals(tag) && child.getValue() != null){
				eventIds.add(child.getValue());
			}
			else if("CULTURAL_NORM".equals(tag) && child.getValue() != null){
				culturalNormIds.add(child.getValue());
			}
		}

		eventPanel.setItems(eventIds);
		culturalNormPanel.setItems(culturalNormIds);
		notePanel.load(record);
		sourcePanel.load(record);

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
		modificationPanel.load(record);
	}

	// ---- Validation ----
	@Override
	protected boolean validData(){
		if(restrictionPanel.hasData() && !restrictionPanel.validateRequiredFields()){
			return false;
		}
		if(conclusionPanel.hasData() && !conclusionPanel.validateRequiredFields()){
			return false;
		}
		return true;
	}

	// ---- Save ----
	@Override
	protected void saveData(){
		// TYPE is saved by binding manager
		bindingManager.save(record);

		// NAME_STRUCTURE
		for(FLEFRecord nameRec : namePanel.getItems()){
			nameRec.setTag("NAME");
			record.addChild(nameRec);
		}

		// CULTURAL_NORM
		for(String id : culturalNormPanel.getItems()){
			FLEFRecordUtils.addChild(record, "CULTURAL_NORM", id);
		}

		// NOTE
		notePanel.save(record);

		// SOURCE_CITATION
		sourcePanel.save(record);

		// PREFERRED_IMAGE
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			FLEFRecord pref = FLEFRecord.createChildWithValue("PREFERRED_IMAGE", preferredImageId);
			record.addChild(pref);
			if(preferredImageCrop != null && !preferredImageCrop.isEmpty()){
				FLEFRecordUtils.updateChildValue(pref, "CROP", preferredImageCrop);
			}
		}

		// RESTRICTION
		if(restrictionPanel.hasData()){
			restrictionPanel.saveToRecord(record);
		}

		// CONCLUSION
		if(conclusionPanel.hasData()){
			FLEFRecord conclusion = conclusionPanel.saveToRecord(null);
			if(conclusion != null){
				conclusion.setTag("CONCLUSION");
				record.addChild(conclusion);
			}
		}

		// RELATIONSHIP (including members)
		for(FLEFRecord rel : otherRelationshipRecords){
			rel.setTag("RELATIONSHIP");
			record.addChild(rel);
		}
		// Ensure member relationships that might not be in otherRelationshipRecords are also saved
		for(FLEFRecord rel : memberRelationshipRecords){
			if(!otherRelationshipRecords.contains(rel)){
				rel.setTag("RELATIONSHIP");
				record.addChild(rel);
			}
		}

		// MODIFICATION
		modificationPanel.save(record);
	}

	private String getGroupId(){
		return record != null? record.getId(): null;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			GroupDialog dialog = GroupDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
