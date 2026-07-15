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
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.*;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ScrollableContainerHost;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
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


/**
 * Dialog for editing a {@code GROUP_RECORD} according to FLEF 0.1.0.
 * <p>
 * The group record represents an aggregation of genealogical entities (family, household,
 * club, neighbourhood, etc.). Membership is expressed via {@code RELATIONSHIP_RECORD}
 * objects that link the group to individuals (or other entities) with a specific
 * {@code TYPE} and optionally a {@code ROLE}.
 * <p>
 * The group itself has one or more names (via {@code NAME_STRUCTURE}), a type, and
 * various other properties (preferred image, restrictions, conclusions, etc.).
 *
 * @see <a href="https://github.com/your-repo/flef">FLEF Specification 0.1.0</a>
 */
public class GroupDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212972L;

	// Handlers
	private final IndividualHandler individualHandler = new IndividualHandler();
	private final RelationshipHandler relationshipHandler = new RelationshipHandler();
	private final EventHandler eventHandler = new EventHandler();
	private final NoteHandler noteHandler = new NoteHandler();
	private final SourceHandler sourceHandler = new SourceHandler();
	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();

	// Preferred Image
	private String preferredImageId;
	private String preferredImageCrop;
	private final JButton preferredImageButton = new JButton();

	// UI components
	private final JTextField nameField = new JTextField();
	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"", "family", "neighborhood", "club", "research group", "household"});
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// Members (relationships that link the group to individuals)
	private final DefaultListModel<String> memberListModel = new DefaultListModel<>();
	private final JList<String> memberList = new JList<>(memberListModel);
	private final List<FLEFRecord> memberRelationshipRecords = new ArrayList<>();

	// Events
	private final DefaultListModel<String> eventListModel = new DefaultListModel<>();
	private final JList<String> eventList = new JList<>(eventListModel);
	private final List<String> eventIds = new ArrayList<>();

	// All other relationships (non‑member)
	private final DefaultListModel<String> relationshipListModel = new DefaultListModel<>();
	private final JList<String> relationshipList = new JList<>(relationshipListModel);
	private final List<FLEFRecord> otherRelationshipRecords = new ArrayList<>();

	// Cultural Norms
	private final DefaultListModel<String> culturalNormListModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormListModel);
	private final List<String> culturalNormIds = new ArrayList<>();

	// Notes (top-level)
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// Source Citations
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// Modification & Conclusion
	private ModificationPanel modificationPanel;
	private ConclusionPanel conclusionPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ----- Factory methods -----
	public static GroupDialog createNew(Frame parent, FLEFModel model){
		return new GroupDialog(parent, model, null);
	}

	public static GroupDialog createEdit(Frame parent, FLEFModel model, FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");
		return new GroupDialog(parent, model, record);
	}

	// ----- Constructor -----
	private GroupDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		initComponents();
		loadData();
		setMinimumSize(new Dimension(500, 550));
		pack();
		setLocationRelativeTo(parent);
	}

	private static String buildTitle(FLEFModel model, FLEFRecord record){
		return (record == null
			? "New Group - " + FLEFRecordUtils.generateNewId(model, GroupHandler.TYPE, GroupHandler.ID_PREFIX) + "*"
			: "Edit Group - " + record.getId());
	}

	// ----- Initialisation -----
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

	// ==================== Main Panel ====================
	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, wrap 1", "[grow]", "[]5[]5[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
				if(e.isPopupTrigger()) imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()) imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}
		});
		panel.add(preferredImageButton, "growx, align center");

		// Name (obligatory) – for now we keep a single name; the model supports multiple names.
		JPanel namePanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		namePanel.add(new JLabel("Name:"), "align label");
		namePanel.add(nameField, "growx");
		panel.add(namePanel, "growx");

		// Type
		JPanel typePanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typeCombo.setEditable(true);
		typePanel.add(typeCombo, "growx");
		panel.add(typePanel, "growx");

		// Members (unified)
		JPanel membersPanel = createMembersPanel();
		panel.add(membersPanel, "growx");

		panel.add(restrictionCheckBox, "growx,align left");

		return panel;
	}

	// ----- Members panel (relationships that link the group to individuals) -----
	private JPanel createMembersPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Members"));
		memberList.setVisibleRowCount(4);
		memberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Build a custom popup menu using the builder.
		// We define: Create New, Add Existing, Edit Relationship, Delete, and Notes.
		// All selection‑sensitive items are enabled only when a member is selected.
		GUIHelper.installBehaviour(memberList,
			() -> memberList.getSelectedIndex() >= 0,           // hasSelection
			this::editMemberRelationship,                       // double‑click → edit relationship
			this::createNewMember,                              // INSERT key → create new member
			this::deleteMember,                                 // DELETE key → delete member
			builder -> {
				builder.item("Create New...", this::createNewMember);
				builder.item("Add Existing...", this::addMember);
				builder.separator();
				builder.selectionSensitiveItem("Edit Relationship...", this::editMemberRelationship);
				builder.selectionSensitiveItem("Edit Individual...", this::editMemberIndividual);
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

		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(memberList,
			ScrollableContainerHost.ScrollType.VERTICAL));
		// Ensure minimum height to show 4 rows even when empty
		FontMetrics fm = memberList.getFontMetrics(memberList.getFont());
		int rowHeight = fm.getHeight() + 2;
		scrollPane.setPreferredSize(new Dimension(0, 4 * rowHeight + 10));
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	/**
	 * Shows a dialog to manage notes attached to a relationship record.
	 * This dialog allows the user to add, remove, create, and edit notes
	 * that are directly attached as children of the relationship record.
	 */
	private void showRelationshipNotesDialog(FLEFRecord relationship){
		if(relationship == null) return;
		NoteListEditorDialog dialog = new NoteListEditorDialog(this, model, relationship);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			// Refresh the display in the members list (if the relationship is in the members list)
			int idx = memberRelationshipRecords.indexOf(relationship);
			if(idx != -1){
				memberListModel.set(idx, getRelationshipDisplay(relationship));
			}
			// Also update the relationship list in the References tab
			int otherIdx = otherRelationshipRecords.indexOf(relationship);
			if(otherIdx != -1){
				relationshipListModel.set(otherIdx, getRelationshipDisplay(relationship));
			}
		}
	}

	// ==================== References Panel ====================
	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, wrap 1", "[grow]", "[]5[]5[]5[]5"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createListPanel("Events",
				eventList, eventListModel,
				this::addEvent, this::editEvent, this::deleteEvent),
			"growx");

		panel.add(createListPanel("Relationships (all)",
				relationshipList, relationshipListModel,
				this::addRelationship, this::editRelationship, this::deleteRelationship),
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

	// ----- Generic list panel builder -----
	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
		Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installBehaviour(list,
			() -> list.getSelectedIndex() >= 0,
			() -> createNewItemForList(list, model),
			addAction,
			editAction,
			deleteAction,
			null);

		JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(list,
			ScrollableContainerHost.ScrollType.VERTICAL));
		FontMetrics fm = list.getFontMetrics(list.getFont());
		int rowHeight = fm.getHeight() + 2;
		scrollPane.setPreferredSize(new Dimension(0, 4 * rowHeight + 10));
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	// ----- Helper to create new item from list context -----
	private void createNewItemForList(JList<String> list, DefaultListModel<String> model){
		if(list == eventList){
			createNewEvent();
		}
		else if(list == relationshipList){
			createNewRelationship();
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

	// ==================== Preferred Image ====================
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

	private void selectAndCropImage(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, sourceHandler, selectedId -> result[0] = selectedId);
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

		ImageCropDialog cropDialog = new ImageCropDialog(getParentFrame(), image);
		cropDialog.setVisible(true);

		Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			preferredImageId = sourceId;
			preferredImageCrop = cropRect.x + " " + cropRect.y + " " + cropRect.width + " " + cropRect.height;
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

	// ==================== Inner Crop Dialog ====================
	private static class ImageCropDialog extends JDialog{
		private final BufferedImage image;
		private Rectangle cropRect;
		private final CropPanel cropPanel;

		public ImageCropDialog(Frame parent, BufferedImage image){
			super(parent, "Select Crop Area", true);
			this.image = image;
			cropPanel = new CropPanel(image);
			initComponents();
			pack();
			setSize(600, 500);
			setLocationRelativeTo(parent);
		}

		private void initComponents(){
			setLayout(new BorderLayout(5, 5));

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			JButton okBtn = new JButton("OK");
			JButton cancelBtn = new JButton("Cancel");
			buttonPanel.add(okBtn);
			buttonPanel.add(cancelBtn);

			add(cropPanel, BorderLayout.CENTER);
			add(buttonPanel, BorderLayout.SOUTH);

			okBtn.addActionListener(e -> {
				cropRect = cropPanel.getCropRect();
				dispose();
			});
			cancelBtn.addActionListener(e -> {
				cropRect = null;
				dispose();
			});
		}

		public Rectangle getCrop(){
			return cropRect;
		}

		private static class CropPanel extends JPanel{
			private final BufferedImage image;
			private final int imgWidth;
			private final int imgHeight;
			private Rectangle rect;
			private Point start;
			private boolean drawing;

			public CropPanel(BufferedImage image){
				this.image = image;
				this.imgWidth = image.getWidth();
				this.imgHeight = image.getHeight();
				setPreferredSize(new Dimension(500, 400));
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

				MouseAdapter adapter = new MouseAdapter(){
					@Override
					public void mousePressed(MouseEvent e){
						start = e.getPoint();
						rect = null;
						drawing = true;
						repaint();
					}

					@Override
					public void mouseDragged(MouseEvent e){
						if(start != null && drawing){
							int x = Math.min(start.x, e.getX());
							int y = Math.min(start.y, e.getY());
							int w = Math.abs(e.getX() - start.x);
							int h = Math.abs(e.getY() - start.y);
							rect = new Rectangle(x, y, w, h);
							repaint();
						}
					}

					@Override
					public void mouseReleased(MouseEvent e){
						drawing = false;
					}
				};
				addMouseListener(adapter);
				addMouseMotionListener(adapter);
			}

			@Override
			protected void paintComponent(Graphics g){
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D)g;

				int panelWidth = getWidth();
				int panelHeight = getHeight();
				double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
				int scaledW = (int)(imgWidth * scale);
				int scaledH = (int)(imgHeight * scale);
				int x = (panelWidth - scaledW) / 2;
				int y = (panelHeight - scaledH) / 2;
				g2.drawImage(image, x, y, scaledW, scaledH, null);

				if(rect != null){
					g2.setColor(Color.RED);
					g2.drawRect(rect.x, rect.y, rect.width, rect.height);
					g2.setColor(new Color(255, 0, 0, 50));
					g2.fillRect(rect.x, rect.y, rect.width, rect.height);
				}
			}

			public Rectangle getCropRect(){
				if(rect == null) return null;
				int panelWidth = getWidth();
				int panelHeight = getHeight();
				double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
				int scaledW = (int)(imgWidth * scale);
				int scaledH = (int)(imgHeight * scale);
				int offsetX = (panelWidth - scaledW) / 2;
				int offsetY = (panelHeight - scaledH) / 2;

				int imgX = (int)((rect.x - offsetX) / scale);
				int imgY = (int)((rect.y - offsetY) / scale);
				int imgW = (int)(rect.width / scale);
				int imgH = (int)(rect.height / scale);

				imgX = Math.clamp(imgX, 0, imgWidth - 1);
				imgY = Math.clamp(imgY, 0, imgHeight - 1);
				imgW = Math.min(imgW, imgWidth - imgX);
				imgH = Math.min(imgH, imgHeight - imgY);
				return new Rectangle(imgX, imgY, imgW, imgH);
			}
		}
	}

	// ==================== Member methods (relationships) ====================

	/**
	 * Adds an existing relationship record to the members list.
	 * Opens the RelationshipDialog with a new relationship pre‑configured to link
	 * this group to a selected individual.
	 */
	private void addMember(){
		if(relationshipHandler == null || individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// First select an individual
		final String[] selectedIndividualId = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, id -> selectedIndividualId[0] = id);
		dialog.setVisible(true);
		String individualId = selectedIndividualId[0];
		if(individualId == null) return;

		// Open RelationshipDialog with pre-filled subject (this group) and object (selected individual)
		RelationshipDialog relDialog = new RelationshipDialog(
			getParentFrame(), model, null, getGroupId(), individualId);
		relDialog.setVisible(true);
		if(relDialog.isSaved()){
			FLEFRecord saved = relDialog.getCitationRecord();
			if(saved != null){
				saved.setLevel(1);
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

	private FLEFRecord getFlefRecord(String individualId){
		// Create a relationship template
		FLEFRecord template = new FLEFRecord();
		template.setTag("RELATIONSHIP");
		// SUBJECT = this group
		FLEFRecord subject = FLEFRecord.createChildWithValue(2, "SUBJECT", getGroupId());
		template.addChild(subject);
		// OBJECT = selected individual
		FLEFRecord object = FLEFRecord.createChildWithValue(2, "OBJECT", individualId);
		template.addChild(object);
		// Add a default TYPE
		FLEFRecord type = FLEFRecord.createChildWithValue(2, "TYPE", "group_member");
		template.addChild(type);
		return template;
	}

	/**
	 * Creates a new individual and then a new relationship linking that individual to this group.
	 */
	private void createNewMember(){
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
		String newIndividualId = null;
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newIndividualId = id;
				break;
			}
		}
		if(newIndividualId != null){
			// Open RelationshipDialog with pre-filled subject (this group) and object (new individual)
			RelationshipDialog relDialog = new RelationshipDialog(
				getParentFrame(), model, null, getGroupId(), newIndividualId);
			relDialog.setVisible(true);
			if(relDialog.isSaved()){
				FLEFRecord saved = relDialog.getCitationRecord();
				if(saved != null){
					saved.setLevel(1);
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

	/**
	 * Factory method to create a RELATIONSHIP record linking this group to an individual.
	 * The group is the SUBJECT, the individual is the OBJECT, and the TYPE is "group_member".
	 * The user can change the type/role later via the edit dialog.
	 */
	private FLEFRecord createMemberRelationship(String individualId){
		if(individualId == null) return null;
		FLEFRecord rel = getFlefRecord(individualId);
		// ROLE (optional default)
		FLEFRecord role = FLEFRecord.createChildWithValue(2, "ROLE", "member");
		rel.addChild(role);
		// Minimal MODIFICATION_STRUCTURE (will be added by the handler later if missing)
		return rel;
	}

	/**
	 * Edits the selected member relationship.
	 */
	private void editMemberRelationship(){
		int idx = memberList.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord existing = memberRelationshipRecords.get(idx);
		if(relationshipHandler == null) return;
		RelationshipDialog dialog = relationshipHandler.createEditDialog(getParentFrame(), model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				memberRelationshipRecords.set(idx, updated);
				memberListModel.set(idx, getRelationshipDisplay(updated));
				// Also update in otherRelationshipRecords
				int otherIdx = otherRelationshipRecords.indexOf(existing);
				if(otherIdx != -1){
					otherRelationshipRecords.set(otherIdx, updated);
					relationshipListModel.set(otherIdx, getRelationshipDisplay(updated));
				}
			}
		}
	}

	/**
	 * Opens the edit dialog for the individual associated with the selected member relationship.
	 */
	private void editMemberIndividual(){
		int idx = memberList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord relationship = memberRelationshipRecords.get(idx);
		if(relationship == null)
			return;

		// Get the OBJECT child (which is the individual)
		FLEFRecord objectChild = relationship.findChild("OBJECT");
		if(objectChild == null){
			JOptionPane.showMessageDialog(this, "No OBJECT found in this relationship.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String individualId = objectChild.getValue();
		if(individualId == null)
			return;

		FLEFRecord individual = model.getRecordById(individualId);
		if(individual == null){
			JOptionPane.showMessageDialog(this, "Individual record not found: " + individualId, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if(individualHandler == null){
			JOptionPane.showMessageDialog(this, "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JDialog dialog = individualHandler.createEditDialog(getParentFrame(), model, individual);
		dialog.setVisible(true);

		// After editing, refresh the display for this relationship (name may have changed)
		memberListModel.set(idx, getRelationshipDisplay(relationship));
		// Also update in the "Relationships (all)" list if present
		int otherIdx = otherRelationshipRecords.indexOf(relationship);
		if(otherIdx != -1){
			relationshipListModel.set(otherIdx, getRelationshipDisplay(relationship));
		}
	}

	/**
	 * Deletes the selected member relationship.
	 */
	private void deleteMember(){
		int idx = memberList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this member relationship?")) return;
		FLEFRecord removed = memberRelationshipRecords.remove(idx);
		memberListModel.remove(idx);
		// Also remove from otherRelationshipRecords
		otherRelationshipRecords.remove(removed);
		relationshipListModel.removeElement(getRelationshipDisplay(removed));
	}

	// ==================== Note helper methods (shared) ====================
	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null) return noteHandler.getDisplayName(rec);
		}
		return id;
	}

	private void addNoteToList(DefaultListModel<String> listModel, List<String> ids, Map<String, String> displayMap){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, noteHandler, selectedId -> {
			if(selectedId != null && !ids.contains(selectedId)){
				ids.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				displayMap.put(selectedId, display);
				listModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNoteFromList(JList<String> list, DefaultListModel<String> listModel,
		List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;
		String id = ids.get(idx);
		if(noteHandler == null) return;
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		displayMap.put(id, newDisplay);
		listModel.set(idx, newDisplay);
	}

	private void deleteNoteFromList(JList<String> list, DefaultListModel<String> listModel,
		List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this note?")) return;
		String removedId = ids.remove(idx);
		displayMap.remove(removedId);
		listModel.remove(idx);
	}

	private void createNewNoteForList(DefaultListModel<String> listModel, List<String> ids, Map<String, String> displayMap){
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
				listModel.addElement(display);
				break;
			}
		}
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
		if(idx == -1) return;
		String id = eventIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = eventHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		eventListModel.set(idx, getEventDisplayName(id));
	}

	private void deleteEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this event?")) return;
		eventIds.remove(idx);
		eventListModel.remove(idx);
	}

	private String getEventDisplayName(String id){
		if(eventHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null) return eventHandler.getDisplayName(rec);
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
		for(FLEFRecord rec : model.getRecordsByType("EVENT")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				eventIds.add(id);
				eventListModel.addElement(getEventDisplayName(id));
				return;
			}
		}
	}

	// ==================== Relationship methods (for the References tab) ====================
	private void addRelationship(){
		addRelationship(null);
	}

	private boolean addRelationship(String preSelectedGroupId){
		if(relationshipHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Relationship handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		FLEFRecord record = null;
		RelationshipDialog dialog = relationshipHandler.createEditDialog(getParentFrame(), model, record);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord saved = dialog.getCitationRecord();
			if(saved != null){
				saved.setLevel(1);
				saved.setTag("RELATIONSHIP");
				otherRelationshipRecords.add(saved);
				relationshipListModel.addElement(getRelationshipDisplay(saved));
				if(isMemberRelationship(saved)){
					memberRelationshipRecords.add(saved);
					memberListModel.addElement(getRelationshipDisplay(saved));
				}
				return true;
			}
		}
		return false;
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
		return other != null && "INDIVIDUAL".equals(other.getType());
	}

	private void editRelationship(){
		if(relationshipHandler == null) return;
		int idx = relationshipList.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord existing = otherRelationshipRecords.get(idx);
		RelationshipDialog dialog = relationshipHandler.createEditDialog(getParentFrame(), model, existing);
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
//		String subjectId = FLEFRecordUtils.getChildValue(relationship, "SUBJECT");
		String objectId = FLEFRecordUtils.getChildValue(relationship, "OBJECT");
//		String type = FLEFRecordUtils.getChildValue(relationship, "TYPE");
		String role = FLEFRecordUtils.getChildValue(relationship, "ROLE");
		StringBuilder display = new StringBuilder();
		if(objectId != null){
			FLEFRecord obj = model.getRecordById(objectId);
			if(obj != null && individualHandler != null){
				display.append(individualHandler.getDisplayName(obj));
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

	private void createNewRelationship(){
		if(relationshipHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Relationship handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		RelationshipDialog dialog = relationshipHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord saved = dialog.getCitationRecord();
			if(saved != null){
				saved.setLevel(1);
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

	/**
	 * Refreshes the members list by scanning all relationships again.
	 */
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
		if(culturalNormHandler == null) return;
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1) return;
		String id = culturalNormIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = culturalNormHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		culturalNormListModel.set(idx, getCulturalNormDisplayName(id));
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this cultural norm?")) return;
		culturalNormIds.remove(idx);
		culturalNormListModel.remove(idx);
	}

	private String getCulturalNormDisplayName(String id){
		if(culturalNormHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null) return culturalNormHandler.getDisplayName(rec);
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
		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				culturalNormIds.add(id);
				culturalNormListModel.addElement(getCulturalNormDisplayName(id));
				return;
			}
		}
	}

	// ==================== Note methods (top-level) ====================
	private void addNote(){
		addNoteToList(noteListModel, noteIds, noteDisplayMap);
	}

	private void editNote(){
		editNoteFromList(noteList, noteListModel, noteIds, noteDisplayMap);
	}

	private void deleteNote(){
		deleteNoteFromList(noteList, noteListModel, noteIds, noteDisplayMap);
	}

	private void createNewNote(){
		createNewNoteForList(noteListModel, noteIds, noteDisplayMap);
	}

	// ==================== Source Citation methods ====================
	private void addSourceCitation(){
		addSourceCitation(null);
	}

	private boolean addSourceCitation(String preSelectedSourceId){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		FLEFRecord citationRecord;
		if(preSelectedSourceId != null && !preSelectedSourceId.isEmpty()){
			citationRecord = FLEFRecord.createChildWithValue(1, "SOURCE_CITATION", preSelectedSourceId);
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
		if(idx == -1) return;
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
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this source citation?")) return;
		sourceCitationRecords.remove(idx);
		sourceCitationListModel.remove(idx);
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		String role = FLEFRecordUtils.getChildValue(citation, "ROLE");
		StringBuilder display = new StringBuilder();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null){
				display.append(sourceHandler.getDisplayName(rec));
			}
			else{
				display.append(sourceId);
			}
		}
		else{
			display.append("[empty]");
		}
		if(role != null && !role.isEmpty()){
			display.append(" [").append(role).append("]");
		}
		return display.toString();
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
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				addSourceCitation(id);
				return;
			}
		}
	}

	// ==================== Load Data ====================
	@Override
	protected void loadData(){
		setTitle(buildTitle(model, record));

		// Basic fields: NAME_STRUCTURE -> first TEXT_VALUE
		FLEFRecord nameStruct = FLEFRecordUtils.findChild(record, "NAME_STRUCTURE");
		if(nameStruct != null){
			String name = FLEFRecordUtils.getChildValue(nameStruct, "VALUE");
			nameField.setText(name != null? name: "");
		}
		else{
			nameField.setText("");
		}

		String type = FLEFRecordUtils.getChildValue(record, "TYPE");
		typeCombo.setSelectedItem(type != null? type: "");

		restrictionCheckBox.setSelected("confidential".equals(FLEFRecordUtils.getChildValue(record, "RESTRICTION")));

		// Clear members
		memberRelationshipRecords.clear();
		memberListModel.clear();

		// Reset other lists
		eventIds.clear();
		eventListModel.clear();
		otherRelationshipRecords.clear();
		relationshipListModel.clear();
		culturalNormIds.clear();
		culturalNormListModel.clear();
		noteIds.clear();
		noteListModel.clear();
		noteDisplayMap.clear();
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();

		// Process all children
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
				String id = child.getValue();
				eventIds.add(id);
				eventListModel.addElement(getEventDisplayName(id));
			}
			else if("CULTURAL_NORM".equals(tag) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				culturalNormListModel.addElement(getCulturalNormDisplayName(id));
			}
			else if("NOTE".equals(tag) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
			}
			else if("SOURCE_CITATION".equals(tag)){
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

	// ==================== Validation ====================
	@Override
	protected boolean validateData(){
		if(nameField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Group must have at least one name.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required for a group.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(!modificationPanel.validateRequiredFields()){
			return false;
		}
		return !conclusionPanel.hasData() || conclusionPanel.validateRequiredFields();
	}

	// ==================== Save ====================
	@Override
	protected void saveRecord(){
		record.getChildren().clear();

		// NAME_STRUCTURE
		String name = nameField.getText().trim();
		if(!name.isEmpty()){
			FLEFRecord nameStruct = FLEFRecord.createChild(1, "NAME_STRUCTURE");
			FLEFRecord value = FLEFRecord.createChildWithValue(2, "VALUE", name);
			nameStruct.addChild(value);
			record.addChild(nameStruct);
		}

		// TYPE
		String type = (String)typeCombo.getSelectedItem();
		if(type != null && !type.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "TYPE", type);
		}

		// RESTRICTION
		FLEFRecordUtils.updateChildValue(record, "RESTRICTION",
			restrictionCheckBox.isSelected()? "confidential": null);

		// --- Relationships (including members) ---
		// Save all relationships from otherRelationshipRecords (which includes all)
		for(FLEFRecord rel : otherRelationshipRecords){
			rel.setLevel(1);
			rel.setTag("RELATIONSHIP");
			record.addChild(rel);
		}
		// In case some relationships were added directly to memberRelationshipRecords but not to otherRelationshipRecords,
		// add them as well.
		for(FLEFRecord rel : memberRelationshipRecords){
			if(!otherRelationshipRecords.contains(rel)){
				rel.setLevel(1);
				rel.setTag("RELATIONSHIP");
				record.addChild(rel);
			}
		}

		// --- Other elements ---
		for(String id : eventIds){
			FLEFRecordUtils.addChild(record, "EVENT", 1, id);
		}

		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(record, "CULTURAL_NORM", 1, id);
		}

		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// Preferred Image
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			FLEFRecord pref = FLEFRecord.createChildWithValue(1, "PREFERRED_IMAGE", preferredImageId);
			record.addChild(pref);
			if(preferredImageCrop != null && !preferredImageCrop.isEmpty()){
				FLEFRecordUtils.updateChildValue(pref, "CROP", preferredImageCrop);
			}
		}

		// Modification
		modificationPanel.saveToRecord(record);

		// Conclusion
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

	// ==================== Utility methods ====================

	/**
	 * Returns the ID of the group being edited (or the new ID if it's a new group).
	 */
	private String getGroupId(){
		return record != null? record.getId(): null;
	}

	// ==================== Overrides ====================
	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), GroupHandler.TYPE);
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, GroupHandler.TYPE, GroupHandler.ID_PREFIX);
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}

	// ==================== Main test ====================
	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Group Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Group");
			btn.addActionListener(e -> {
				GroupDialog dialog = GroupDialog.createNew(frame, model);
				dialog.setVisible(true);
				System.out.println("Group saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
