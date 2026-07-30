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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordUtils;
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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
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


/* ONGOING */
/**
 * Dialog for editing a {@code GROUP_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * GROUP_RECORD :=
 * n @<XREF:GROUP>@ GROUP    {1:1}
 *   +1 <<NAME_STRUCTURE>>    {0:M}
 *   +1 TYPE <GROUP_TYPE>    {0:1}
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

	private static final String TAG_NAME = "NAME";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_RESTRICTION = "RESTRICTION";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_DOCUMENT_STRUCTURE = "DOCUMENT_STRUCTURE";
	private static final String TAG_FILE = "FILE";
	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_EVENT = "EVENT";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_CROP = "CROP";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new GroupHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final NameListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final CulturalNormListPanel culturalNormPanel;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;

	private final JButton preferredImageButton = new JButton();
	private String preferredImageId;
	private String preferredImageCrop;

	private final RestrictionPanel restrictionPanel;
	private final ConclusionPanel conclusionPanel;
	private final ModificationPanel modificationPanel;

	// Other
	private final EventListPanel eventPanel;
	private final DefaultListModel<String> memberListModel = new DefaultListModel<>();
	private final JList<String> memberList = new JList<>(memberListModel);
	private final List<FLEFRecord> memberRelationshipRecords = new ArrayList<>();
	private final DefaultListModel<String> relationshipListModel = new DefaultListModel<>();
	private final JList<String> relationshipList = new JList<>(relationshipListModel);
	private final List<FLEFRecord> otherRelationshipRecords = new ArrayList<>();


	public static GroupDialog createNew(final Dialog parent, final FLEFModel model){
		return new GroupDialog(parent, model, null);
	}

	public static GroupDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new GroupDialog(parent, model, record);
	}


	private GroupDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupHandler.TYPE));

		namePanel = new NameListPanel(TAG_NAME, this, model);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{StringUtils.EMPTY, "family", "household",
			"neighbourhood", "fraternity", "club", "research group", "literary society", "association", "organisation",
			"tribe"});
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		conclusionPanel = new ConclusionPanel(TAG_CONCLUSION, model, this);
		modificationPanel = new ModificationPanel(this);
		eventPanel = new EventListPanel(model, this);
		culturalNormPanel = new CulturalNormListPanel(model, this);
		notePanel = new NoteListPanel(TAG_NOTE, model, this);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		bindingManager.bind(typeCombo);

		setLayout(new MigLayout("fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]5[]5[]10[]"));

		// preferred image
		preferredImageButton.setPreferredSize(new Dimension(80, 80));
		preferredImageButton.setIcon(createPlaceholderIcon());
		preferredImageButton.setToolTipText("Left-click to select an image, right-click for options");
		preferredImageButton.addActionListener(e -> selectAndCropImage());
		final JPopupMenu imagePopup = new JPopupMenu();
		final JMenuItem clearImageMenuItem = new JMenuItem("Clear");
		clearImageMenuItem.addActionListener(e -> clearImage());
		imagePopup.add(clearImageMenuItem);
		preferredImageButton.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(e.isPopupTrigger())
					imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				if(e.isPopupTrigger())
					imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}
		});
		panel.add(preferredImageButton, "growx, align center");

		// type
		final JPanel typePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typePanel.add(typeCombo, "growx");
		panel.add(typePanel, "growx");

		// names
		panel.add(namePanel, "growx");

		// members
		panel.add(createMembersPanel(), "growx");

		return panel;
	}

	private JPanel createMembersPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx"));
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
		final JScrollPane scrollPane = GUIHelper.createScrollPane(memberList);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]5[]"));
		panel.add(eventPanel, "growx");
		panel.add(createListPanel("Relationships (all)", relationshipList,
			this::addRelationship, this::editRelationship, this::deleteRelationship), "growx");
		panel.add(culturalNormPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}

	// Helper for lists that are not yet abstract (relationships)
	private JPanel createListPanel(final String title, final JList<String> list,
			final Runnable createNewAction, final Runnable editAction, final Runnable deleteAction){
		final JPanel panel = new JPanel(new MigLayout("fillx"));
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
		final JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	private void showRelationshipNotesDialog(final FLEFRecord relationship){
		if(relationship == null)
			return;

		final NoteListEditorDialog dialog = new NoteListEditorDialog(this, model, relationship);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			final int idx = memberRelationshipRecords.indexOf(relationship);
			if(idx != -1)
				memberListModel.set(idx, getRelationshipDisplay(relationship));

			final int otherIdx = otherRelationshipRecords.indexOf(relationship);
			if(otherIdx != -1)
				relationshipListModel.set(otherIdx, getRelationshipDisplay(relationship));
		}
	}

	private Icon createPlaceholderIcon(){
		final BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = img.createGraphics();
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
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, sourceHandler, selectedId -> result[0] = selectedId);
		dialog.setVisible(true);
		final String sourceId = result[0];
		if(sourceId == null)
			return;

		final BufferedImage image = loadImageFromSource(sourceId);
		if(image == null){
			JOptionPane.showMessageDialog(this,
				"Could not load image from the selected source.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final ImageCropDialog cropDialog = new ImageCropDialog(this, image);
		cropDialog.setVisible(true);

		final Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			preferredImageId = sourceId;
			preferredImageCrop = cropRect.x + StringUtils.SPACE + cropRect.y + StringUtils.SPACE + cropRect.width + StringUtils.SPACE + cropRect.height;
			updateImageButton(sourceId);
		}
	}

	private BufferedImage loadImageFromSource(final String sourceId){
		final FLEFRecord source = model.getRecordById(sourceId);
		if(source == null)
			return null;

		final FLEFRecord doc = FLEFRecordUtils.findChild(source, TAG_DOCUMENT_STRUCTURE);
		if(doc == null)
			return null;

		final String filePath = FLEFRecordUtils.getChildValue(doc, TAG_FILE);
		if(filePath == null || filePath.isEmpty())
			return null;

		try{
			final File file = new File(filePath);
			if(!file.exists())
				return null;
			return ImageIO.read(file);
		}
		catch(final IOException e){
			e.printStackTrace();

			return null;
		}
	}

	private void updateImageButton(final String sourceId){
		final BufferedImage img = loadImageFromSource(sourceId);
		if(img != null){
			final Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
			preferredImageButton.setIcon(new ImageIcon(scaled));
		}
		else
			preferredImageButton.setIcon(createPlaceholderIcon());
	}

	private void clearImage(){
		preferredImageId = null;
		preferredImageCrop = null;
		preferredImageButton.setIcon(createPlaceholderIcon());
	}

	private void addMember(){
		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		final String[] selectedIndividualId = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, individualHandler, id -> selectedIndividualId[0] = id);
		dialog.setVisible(true);
		final String individualId = selectedIndividualId[0];
		if(individualId == null)
			return;

		final RelationshipDialog relDialog = new RelationshipDialog(
			this, model, null, getGroupId(), individualId);
		relDialog.setVisible(true);
		if(relDialog.isSaved()){
			final FLEFRecord saved = relDialog.getCitationRecord();
			if(saved != null){
				saved.setTag(TAG_RELATIONSHIP);
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
		final Set<String> before = new HashSet<>();
		for(final FLEFRecord rec : model.getRecordsByType(TAG_INDIVIDUAL)){
			final String id = rec.getId();
			if(id != null)
				before.add(id);
		}

		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		final JDialog dialog = individualHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		String newIndividualId = null;
		for(final FLEFRecord rec : model.getRecordsByType(TAG_INDIVIDUAL)){
			final String id = rec.getId();
			if(id != null && !before.contains(id)){
				newIndividualId = id;

				break;
			}
		}

		if(newIndividualId != null){
			final RelationshipDialog relDialog = new RelationshipDialog(
				this, model, null, getGroupId(), newIndividualId);
			relDialog.setVisible(true);
			if(relDialog.isSaved()){
				final FLEFRecord saved = relDialog.getCitationRecord();
				if(saved != null){
					saved.setTag(TAG_RELATIONSHIP);
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
		final int idx = memberList.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord existing = memberRelationshipRecords.get(idx);
		final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
		final RelationshipDialog dialog = (RelationshipDialog)relationshipHandler.createEditDialog(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				memberRelationshipRecords.set(idx, updated);
				memberListModel.set(idx, getRelationshipDisplay(updated));
				final int otherIdx = otherRelationshipRecords.indexOf(existing);
				if(otherIdx != -1){
					otherRelationshipRecords.set(otherIdx, updated);
					relationshipListModel.set(otherIdx, getRelationshipDisplay(updated));
				}
			}
		}
	}

	private void editMemberIndividual(){
		final int idx = memberList.getSelectedIndex();
		if(idx == -1)
			return;

		final FLEFRecord relationship = memberRelationshipRecords.get(idx);
		if(relationship == null)
			return;

		final FLEFRecord objectChild = relationship.findChild(TAG_OBJECT);
		if(objectChild == null){
			JOptionPane.showMessageDialog(this, "No OBJECT found in this relationship.", "Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final String individualId = objectChild.getValue();
		if(individualId == null)
			return;

		final FLEFRecord individual = model.getRecordById(individualId);
		if(individual == null){
			JOptionPane.showMessageDialog(this, "Individual record not found: " + individualId, "Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
		final JDialog dialog = individualHandler.createEditDialog(this, model, individual);
		dialog.setVisible(true);

		memberListModel.set(idx, getRelationshipDisplay(relationship));
		final int otherIdx = otherRelationshipRecords.indexOf(relationship);
		if(otherIdx != -1)
			relationshipListModel.set(otherIdx, getRelationshipDisplay(relationship));
	}

	private void deleteMember(){
		final int idx = memberList.getSelectedIndex();
		if(idx == -1)
			return;

		if(!showConfirm("Confirm", "Remove this member relationship?"))
			return;

		final FLEFRecord removed = memberRelationshipRecords.remove(idx);
		memberListModel.remove(idx);
		otherRelationshipRecords.remove(removed);
		relationshipListModel.removeElement(getRelationshipDisplay(removed));
	}

	private void addRelationship(){
		final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
		final RelationshipDialog dialog = (RelationshipDialog)relationshipHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord saved = dialog.getCitationRecord();
			if(saved != null){
				saved.setTag(TAG_RELATIONSHIP);
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
		final int idx = relationshipList.getSelectedIndex();
		if(idx == -1)
			return;

		final RecordTypeHandler<?> relationshipHandler = HandlerRegistry.getHandler(RelationshipHandler.TYPE);
		final FLEFRecord existing = otherRelationshipRecords.get(idx);
		final RelationshipDialog dialog = (RelationshipDialog)relationshipHandler.createEditDialog(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				otherRelationshipRecords.set(idx, updated);
				relationshipListModel.set(idx, getRelationshipDisplay(updated));
				refreshMemberList();
			}
		}
	}

	private void deleteRelationship(){
		final int idx = relationshipList.getSelectedIndex();
		if(idx == -1)
			return;

		if(!showConfirm("Confirm", "Remove this relationship?"))
			return;

		otherRelationshipRecords.remove(idx);
		relationshipListModel.remove(idx);
		refreshMemberList();
	}

	private String getRelationshipDisplay(final FLEFRecord relationship){
		final String objectId = FLEFRecordUtils.getChildValue(relationship, TAG_OBJECT);
		final String role = FLEFRecordUtils.getChildValue(relationship, TAG_ROLE);
		final StringBuilder display = new StringBuilder();
		if(objectId != null){
			final FLEFRecord obj = model.getRecordById(objectId);
			if(obj != null){
				final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);
				display.append(individualHandler.getDisplayText(obj));
			}
			else
				display.append(objectId);
		}
		else
			display.append("?");
		if(role != null && !role.isEmpty())
			display.append(" [").append(role).append("]");
		return display.toString();
	}

	private boolean isMemberRelationship(final FLEFRecord relationship){
		final String subjectId = FLEFRecordUtils.getChildValue(relationship, TAG_SUBJECT);
		final String objectId = FLEFRecordUtils.getChildValue(relationship, TAG_OBJECT);
		final String groupId = getGroupId();
		if(groupId == null)
			return false;

		final boolean groupIsSubject = groupId.equals(subjectId);
		final boolean groupIsObject = groupId.equals(objectId);
		if(!groupIsSubject && !groupIsObject)
			return false;

		final String otherId = groupIsSubject? objectId: subjectId;
		if(otherId == null)
			return false;

		final FLEFRecord other = model.getRecordById(otherId);
		return (other != null && TAG_INDIVIDUAL.equals(other.getTag()));
	}

	private void refreshMemberList(){
		memberRelationshipRecords.clear();
		memberListModel.clear();

		for(final FLEFRecord rel : otherRelationshipRecords)
			if(isMemberRelationship(rel)){
				memberRelationshipRecords.add(rel);
				memberListModel.addElement(getRelationshipDisplay(rel));
			}
	}

	@Override
	protected void loadData(){
		bindingManager.load(record);

		// NAME_STRUCTURE
		final List<FLEFRecord> names = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if(TAG_NAME.equals(child.getTag()))
				names.add(child);
		namePanel.setItems(names);

		// Restriction
		restrictionPanel.load(record);

		// Conclusion
		conclusionPanel.load(record);

		// Clear all lists
		memberRelationshipRecords.clear();
		memberListModel.clear();
		otherRelationshipRecords.clear();
		relationshipListModel.clear();

		// Process children for lists
		final List<String> eventIds = new ArrayList<>();
		final List<String> culturalNormIds = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren()){
			final String tag = child.getTag();

			if(TAG_RELATIONSHIP.equals(tag)){
				otherRelationshipRecords.add(child);
				relationshipListModel.addElement(getRelationshipDisplay(child));
				if(isMemberRelationship(child)){
					memberRelationshipRecords.add(child);
					memberListModel.addElement(getRelationshipDisplay(child));
				}
			}
			else if(TAG_EVENT.equals(tag) && child.getValue() != null)
				eventIds.add(child.getValue());
			else if(TAG_CULTURAL_NORM.equals(tag) && child.getValue() != null)
				culturalNormIds.add(child.getValue());
		}

		eventPanel.setItems(eventIds);
		culturalNormPanel.setItems(culturalNormIds);
		notePanel.load(record);
		sourcePanel.load(record);

		// Preferred Image
		final FLEFRecord pref = FLEFRecordUtils.findChild(record, TAG_PREFERRED_IMAGE);
		if(pref != null){
			preferredImageId = pref.getValue();
			preferredImageCrop = FLEFRecordUtils.getChildValue(pref, TAG_CROP);
			updateImageButton(preferredImageId);
		}
		else
			clearImage();

		// Modification
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(restrictionPanel.hasData() && !restrictionPanel.validateData())
			return false;

		if(conclusionPanel.hasData() && !conclusionPanel.validateData())
			return false;

		return true;
	}

	@Override
	protected void saveData(){
		// TYPE is saved by binding manager
		bindingManager.save(record);

		// NAME_STRUCTURE
		for(final FLEFRecord nameRec : namePanel.getItems()){
			nameRec.setTag(TAG_NAME);
			record.addChild(nameRec);
		}

		// CULTURAL_NORM
		for(final String id : culturalNormPanel.getItems())
			FLEFRecordUtils.addChild(record, TAG_CULTURAL_NORM, id);

		// NOTE
		notePanel.save(record);

		// SOURCE_CITATION
		sourcePanel.save(record);

		// PREFERRED_IMAGE
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			final FLEFRecord pref = FLEFRecord.createChildWithValue(TAG_PREFERRED_IMAGE, preferredImageId);
			record.addChild(pref);
			if(preferredImageCrop != null && !preferredImageCrop.isEmpty())
				FLEFRecordUtils.updateChildValue(pref, TAG_CROP, preferredImageCrop);
		}

		// RESTRICTION
		if(restrictionPanel.hasData())
			restrictionPanel.save(record);

		// CONCLUSION
		if(conclusionPanel.hasData()){
			final FLEFRecord conclusion = conclusionPanel.save(null);
			if(conclusion != null){
				conclusion.setTag(TAG_CONCLUSION);
				record.addChild(conclusion);
			}
		}

		// RELATIONSHIP (including members)
		for(final FLEFRecord rel : otherRelationshipRecords){
			rel.setTag(TAG_RELATIONSHIP);
			record.addChild(rel);
		}
		// Ensure member relationships that might not be in otherRelationshipRecords are also saved
		for(final FLEFRecord rel : memberRelationshipRecords)
			if(!otherRelationshipRecords.contains(rel)){
				rel.setTag(TAG_RELATIONSHIP);
				record.addChild(rel);
			}

		// MODIFICATION
		modificationPanel.save(record);
	}

	private String getGroupId(){
		return (record != null? record.getId(): null);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final GroupDialog dialog = GroupDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
