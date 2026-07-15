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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;


/**
 * Dialog for editing an INDIVIDUAL_RECORD according to FLEF 0.0.9.
 */
public class IndividualDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 8666608193574177088L;


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new GroupCitationHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new CalendarHandler());
	}

	// Basic fields
	private final JComboBox<String> sexCombo = new JComboBox<>(new String[]{"", "MALE", "FEMALE", "UNKNOWN"});
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// Names
	private final DefaultListModel<String> nameListModel = new DefaultListModel<>();
	private final JList<String> nameList = new JList<>(nameListModel);
	private final List<FLEFRecord> nameRecords = new ArrayList<>();

	// Family links
	private final DefaultListModel<String> childFamilyListModel = new DefaultListModel<>();
	private final JList<String> childFamilyList = new JList<>(childFamilyListModel);
	private final List<String> childFamilyIds = new ArrayList<>();
	private final Map<String, FLEFRecord> childFamilyLinkDetails = new HashMap<>();

	private final DefaultListModel<String> parentFamilyListModel = new DefaultListModel<>();
	private final JList<String> parentFamilyList = new JList<>(parentFamilyListModel);
	private final List<String> parentFamilyIds = new ArrayList<>();
	private final Map<String, FLEFRecord> parentFamilyLinkDetails = new HashMap<>();

	// Associations
	private final DefaultListModel<String> associationListModel = new DefaultListModel<>();
	private final JList<String> associationList = new JList<>(associationListModel);
	private final Map<String, FLEFRecord> associationRecords = new HashMap<>();

	// Aliases
	private final DefaultListModel<String> aliasListModel = new DefaultListModel<>();
	private final JList<String> aliasList = new JList<>(aliasListModel);
	private final List<AliasDialog.AliasEntry> aliasEntries = new ArrayList<>();

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

	// Preferred Image
	private String preferredImageId;
	private String cropString;
	private final JButton imageButton = new JButton();

	// Modification
	private final ModificationPanel modificationPanel;

	// Conclusion
	private final ConclusionPanel conclusionPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> familyHandler = HandlerRegistry.getHandler("FAMILY");
	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");
	private final RecordTypeHandler<?> groupCitationHandler = HandlerRegistry.getHandler("GROUP_CITATION");
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");


	public static IndividualDialog createNew(Frame parent, FLEFModel model){
		return new IndividualDialog(parent, model, null);
	}

	public static IndividualDialog createEdit(Frame parent, FLEFModel model, FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new IndividualDialog(parent, model, record);
	}


	private IndividualDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, buildTitle(model, record), model, record);

		this.modificationPanel = new ModificationPanel(model, this);
		this.conclusionPanel = new ConclusionPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(500, 650));
		pack();
		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFModel model, final FLEFRecord record){
		return (record == null
			? "New Individual - " + FLEFRecordUtils.generateNewId(model, "INDIVIDUAL", "I") + "*"
			: "Edit Individual - " + record.getId());
	}


	@Override
	protected void initComponents(){
		setLayout(new MigLayout("fillx"));

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Basic", createBasicPanel());   // ID, Sex, Restriction, Preferred Image, Names in fondo
		tabbedPane.addTab("Family", createFamilyPanel());
		tabbedPane.addTab("Associations", createAssociationsPanel());
		tabbedPane.addTab("Aliases", createAliasesPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);

		add(tabbedPane, "growx,push");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}


	// ==================== Basic Panel (con Names in fondo) ====================
	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		panel.add(new JLabel("Sex:"), "align label");
		panel.add(sexCombo, "growx,wrap");

		panel.add(restrictionCheckBox, "span 2,wrap");

		// ---- PREFERRED_IMAGE ----
		// Image button: left-click selects, right-click shows Clear popup
		imageButton.setPreferredSize(new Dimension(80, 80));
		imageButton.setIcon(createPlaceholderIcon());
		imageButton.setToolTipText("Left-click to select an image, right-click for options");

		// Left-click: open GenericSelectionDialog directly
		imageButton.addActionListener(e -> selectAndCropImage());

		// Right-click: popup with "Clear"
		JPopupMenu imagePopup = new JPopupMenu();
		JMenuItem clearImageMenuItem = new JMenuItem("Clear");
		clearImageMenuItem.addActionListener(e -> clearImage());
		imagePopup.add(clearImageMenuItem);

		imageButton.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					imagePopup.show(imageButton, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					imagePopup.show(imageButton, e.getX(), e.getY());
				}
			}
		});

		// Add image button to panel (wrap to next line)
		panel.add(imageButton, "wrap");

		// Personal Names (in fondo)
		panel.add(createNamesPanel(), "span 2, growx, wrap");

		return panel;
	}


	// ==================== Names Panel (riutilizzato da Basic) ====================
	private JPanel createNamesPanel(){
		return createListPanel("Personal Names", nameList, nameListModel,
			null, this::editName, this::deleteName);
	}


	// ==================== Family Panel ====================
	private JPanel createFamilyPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createListPanel("Child of Families",
				childFamilyList, childFamilyListModel,
				this::addChildFamily, this::editChildFamily, this::deleteChildFamily),
			"growx");

		panel.add(createListPanel("Parent in Families",
				parentFamilyList, parentFamilyListModel,
				this::addParentFamily, this::editParentFamily, this::deleteParentFamily),
			"growx");

		return panel;
	}


	// ==================== Associations Panel ====================
	private JPanel createAssociationsPanel(){
		return createListPanel("Associations", associationList, associationListModel,
			this::addAssociation, this::editAssociation, this::deleteAssociation);
	}


	// ==================== Aliases Panel ====================
	private JPanel createAliasesPanel(){
		return createListPanel("Aliases", aliasList, aliasListModel,
			this::addAlias, this::editAlias, this::deleteAlias);
	}


	// ==================== References Panel (stacked) ====================
	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, wrap 1", "[grow]", "[]5[]5[]5[]5[]"));
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


	// ==================== Generic List Panel ====================
	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
		Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Popup menu
		JPopupMenu popup = new JPopupMenu();
		JMenuItem addItem = new JMenuItem("Add Existing...");
		JMenuItem newItem = new JMenuItem("Create New...");
		JMenuItem editItem = new JMenuItem("Edit");
		JMenuItem deleteItem = new JMenuItem("Delete");
		if(addAction != null)
			popup.add(addItem);
		popup.add(newItem);
		popup.addSeparator();
		popup.add(editItem);
		popup.add(deleteItem);

		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});

		// Tasti: Ins = New, Canc = Delete
		list.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					createNewItemForList(list, model);
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteAction.run();
					e.consume();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(list.getPreferredScrollableViewportSize());
		panel.add(scrollPane, "growx,wrap");

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editItem.setEnabled(selected);
			deleteItem.setEnabled(selected);
		});
		editItem.setEnabled(false);
		deleteItem.setEnabled(false);

		if(addAction != null)
			addItem.addActionListener(e -> addAction.run());
		newItem.addActionListener(e -> createNewItemForList(list, model));
		editItem.addActionListener(e -> editAction.run());
		deleteItem.addActionListener(e -> deleteAction.run());

		return panel;
	}


	// ==================== Data loading ====================
	@Override
	protected void loadData(){
		setTitle(buildTitle(model, record));

		// ID is shown in the title, no field to set
		sexCombo.setSelectedItem(FLEFRecordUtils.getChildValue(record, "SEX"));
		restrictionCheckBox.setSelected("confidential".equals(FLEFRecordUtils.getChildValue(record, "RESTRICTION")));
		loadNames();
		loadFamilyLinks();
		loadAssociations();
		loadAliases();
		loadEvents();
		loadGroupCitations();
		loadCulturalNorms();
		loadNotes();
		loadSourceCitations();
		loadPreferredImage();
		modificationPanel.loadFromRecord(record);
		FLEFRecord conclusion = FLEFRecordUtils.findChild(record, "CONCLUSION");
		conclusionPanel.loadFromRecord(conclusion);
	}

	private void loadNames(){
		nameListModel.clear();
		nameRecords.clear();
		for(FLEFRecord child : record.getChildren()){
			if("NAME".equals(child.getTag())){
				nameRecords.add(child);
				nameListModel.addElement(buildNameDisplay(child));
			}
		}
	}

	private String buildNameDisplay(FLEFRecord nameRecord){
		String given = FLEFRecordUtils.getChildValue(nameRecord, "INDIVIDUAL_NAME");
		String family = FLEFRecordUtils.getChildValue(nameRecord, "FAMILY_NAME");
		String type = FLEFRecordUtils.getChildValue(nameRecord, "TYPE");
		StringBuilder sb = new StringBuilder();
		if(given != null) sb.append(given);
		if(family != null){
			if(!sb.isEmpty()) sb.append(" ");
			sb.append(family);
		}
		if(type != null)
			sb.append(" (").append(type).append(")");
		if(sb.isEmpty())
			sb.append("[unnamed]");
		return sb.toString();
	}

	private void addName(){
		NameDialog dialog = new NameDialog(this, model, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord newName = dialog.getNameRecord();
			if(newName != null){
				newName.setLevel(1);
				newName.setTag("NAME");
				record.addChild(newName);
				nameRecords.add(newName);
				nameListModel.addElement(buildNameDisplay(newName));
			}
		}
	}

	private void editName(){
		int idx = nameList.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord nameRecord = nameRecords.get(idx);
		NameDialog dialog = new NameDialog(this, model, nameRecord);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			nameListModel.set(idx, buildNameDisplay(nameRecord));
		}
	}

	private void deleteName(){
		int idx = nameList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this name?")) return;
		FLEFRecord removed = nameRecords.remove(idx);
		record.getChildren().remove(removed);
		nameListModel.remove(idx);
	}


	// ==================== Family links ====================
	private void loadFamilyLinks(){
		childFamilyListModel.clear();
		childFamilyIds.clear();
		childFamilyLinkDetails.clear();
		for(FLEFRecord child : record.getChildren()){
			if("FAMILY_CHILD".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				childFamilyIds.add(id);
				childFamilyLinkDetails.put(id, child);
				childFamilyListModel.addElement(getFamilyDisplayName(id));
			}
		}
		parentFamilyListModel.clear();
		parentFamilyIds.clear();
		parentFamilyLinkDetails.clear();
		for(FLEFRecord child : record.getChildren()){
			if("FAMILY_PARENT".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				parentFamilyIds.add(id);
				parentFamilyLinkDetails.put(id, child);
				parentFamilyListModel.addElement(getFamilyDisplayName(id));
			}
		}
	}

	private String getFamilyDisplayName(String id){
		if(familyHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return familyHandler.getDisplayName(rec);
		}
		return id;
	}

	private void addChildFamily(){
		if(familyHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Family handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, familyHandler, selectedId -> {
			if(selectedId != null && !childFamilyIds.contains(selectedId)){
				FLEFRecord link = showFamilyLinkDialog(selectedId, "FAMILY_CHILD", null);
				if(link != null){
					childFamilyIds.add(selectedId);
					childFamilyLinkDetails.put(selectedId, link);
					childFamilyListModel.addElement(getFamilyDisplayName(selectedId));
				}
			}
		});
		dialog.setVisible(true);
	}

	private void editChildFamily(){
		int idx = childFamilyList.getSelectedIndex();
		if(idx == -1) return;
		String id = childFamilyIds.get(idx);
		FLEFRecord existing = childFamilyLinkDetails.get(id);
		if(existing == null){
			existing = new FLEFRecord();
			existing.setLevel(1);
			existing.setTag("FAMILY_CHILD");
			existing.setValue(id);
		}
		FLEFRecord updated = showFamilyLinkDialog(id, "FAMILY_CHILD", existing);
		if(updated != null){
			childFamilyLinkDetails.put(id, updated);
			childFamilyListModel.set(idx, getFamilyDisplayName(id));
		}
	}

	private void deleteChildFamily(){
		int idx = childFamilyList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this family link?")) return;
		String id = childFamilyIds.remove(idx);
		childFamilyLinkDetails.remove(id);
		childFamilyListModel.remove(idx);
	}

	private void addParentFamily(){
		if(familyHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Family handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, familyHandler, selectedId -> {
			if(selectedId != null && !parentFamilyIds.contains(selectedId)){
				FLEFRecord link = showFamilyLinkDialog(selectedId, "FAMILY_PARENT", null);
				if(link != null){
					parentFamilyIds.add(selectedId);
					parentFamilyLinkDetails.put(selectedId, link);
					parentFamilyListModel.addElement(getFamilyDisplayName(selectedId));
				}
			}
		});
		dialog.setVisible(true);
	}

	private void editParentFamily(){
		int idx = parentFamilyList.getSelectedIndex();
		if(idx == -1) return;
		String id = parentFamilyIds.get(idx);
		FLEFRecord existing = parentFamilyLinkDetails.get(id);
		if(existing == null){
			existing = new FLEFRecord();
			existing.setLevel(1);
			existing.setTag("FAMILY_PARENT");
			existing.setValue(id);
		}
		FLEFRecord updated = showFamilyLinkDialog(id, "FAMILY_PARENT", existing);
		if(updated != null){
			parentFamilyLinkDetails.put(id, updated);
			parentFamilyListModel.set(idx, getFamilyDisplayName(id));
		}
	}

	private void deleteParentFamily(){
		int idx = parentFamilyList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this family link?")) return;
		String id = parentFamilyIds.remove(idx);
		parentFamilyLinkDetails.remove(id);
		parentFamilyListModel.remove(idx);
	}

	private FLEFRecord showFamilyLinkDialog(String familyId, String linkType, FLEFRecord existing){
		FamilyLinkDialog dialog = new FamilyLinkDialog(this, model, familyId, linkType, existing);
		dialog.setVisible(true);
		return dialog.isSaved()? dialog.getLinkRecord(): null;
	}


	// ==================== Associations ====================
	private void loadAssociations(){
		associationListModel.clear();
		associationRecords.clear();
		for(FLEFRecord child : record.getChildren()){
			if("ASSOCIATION".equals(child.getTag())){
				String display = getAssociationDisplay(child);
				associationListModel.addElement(display);
				associationRecords.put(display, child);
			}
		}
	}

	private String getAssociationDisplay(FLEFRecord association){
		String value = association.getValue();
		boolean isVoid = "@VOID@".equals(value) || "VOID".equals(association.getId());
		if(isVoid){
			String name = FLEFRecordUtils.getChildValue(association, "NAME");
			return "VOID: " + (name != null && !name.isEmpty()? name: "[unnamed]");
		}
		else{
			String targetId = value != null? value: association.getId();
			if(targetId != null){
				FLEFRecord target = model.getRecordById(targetId);
				if(target != null){
					RecordTypeHandler<?> handler = HandlerRegistry.getHandler(target.getType());
					if(handler != null)
						return handler.getDisplayName(target);
				}
			}
			return "ID: " + targetId;
		}
	}

	private void addAssociation(){
		AssociationDialog dialog = AssociationDialog.createNew(this, model);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord assoc = dialog.getAssociationRecord();
			String display = getAssociationDisplay(assoc);
			associationListModel.addElement(display);
			associationRecords.put(display, assoc);
		}
	}

	private void editAssociation(){
		int idx = associationList.getSelectedIndex();
		if(idx == -1) return;
		String key = associationListModel.get(idx);
		FLEFRecord existing = associationRecords.get(key);
		if(existing == null){
			for(FLEFRecord child : record.getChildren()){
				if("ASSOCIATION".equals(child.getTag()) && getAssociationDisplay(child).equals(key)){
					existing = child;
					break;
				}
			}
		}
		AssociationDialog dialog = AssociationDialog.createEdit(this, model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getAssociationRecord();
			String display = getAssociationDisplay(updated);
			associationRecords.remove(key);
			associationListModel.set(idx, display);
			associationRecords.put(display, updated);
		}
	}

	private void deleteAssociation(){
		int idx = associationList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this association?")) return;
		String key = associationListModel.get(idx);
		associationRecords.remove(key);
		associationListModel.remove(idx);
	}


	// ==================== Aliases ====================
	private void loadAliases(){
		aliasListModel.clear();
		aliasEntries.clear();
		for(FLEFRecord child : record.getChildren()){
			if("ALIAS".equals(child.getTag()) && child.getValue() != null){
				String aliasId = child.getValue();
				String certainty = FLEFRecordUtils.getChildValue(child, "CERTAINTY");
				String credibility = FLEFRecordUtils.getChildValue(child, "CREDIBILITY");
				List<String> notes = new ArrayList<>();
				for(FLEFRecord noteChild : child.getChildren()){
					if("NOTE".equals(noteChild.getTag()) && noteChild.getValue() != null){
						notes.add(noteChild.getValue());
					}
				}
				AliasDialog.AliasEntry entry = new AliasDialog.AliasEntry(aliasId, certainty, credibility, notes);
				aliasEntries.add(entry);
				aliasListModel.addElement(entry.toString());
			}
		}
	}

	private void addAlias(){
		AliasDialog dialog = AliasDialog.createNew(getParentFrame(), model);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			AliasDialog.AliasEntry entry = dialog.getEntry();
			if(entry != null){
				aliasEntries.add(entry);
				aliasListModel.addElement(entry.toString());
			}
		}
	}

	private void editAlias(){
		int idx = aliasList.getSelectedIndex();
		if(idx == -1) return;
		AliasDialog.AliasEntry existing = aliasEntries.get(idx);
		AliasDialog dialog = AliasDialog.createEdit(getParentFrame(), model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			AliasDialog.AliasEntry updated = dialog.getEntry();
			if(updated != null){
				aliasEntries.set(idx, updated);
				aliasListModel.set(idx, updated.toString());
			}
		}
	}

	private void deleteAlias(){
		int idx = aliasList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this alias?")) return;
		aliasEntries.remove(idx);
		aliasListModel.remove(idx);
	}


	// ==================== Events ====================
	private void loadEvents(){
		eventListModel.clear();
		eventIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("EVENT".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				eventIds.add(id);
				eventListModel.addElement(getEventDisplayName(id));
			}
		}
	}

	private String getEventDisplayName(String id){
		if(eventHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return eventHandler.getDisplayName(rec);
		}
		return id;
	}

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


	// ==================== Group Citations ====================
	private void loadGroupCitations(){
		groupCitationListModel.clear();
		groupCitationRecords.clear();
		for(FLEFRecord child : record.getChildren()){
			if("GROUP_CITATION".equals(child.getTag())){
				groupCitationRecords.add(child);
				groupCitationListModel.addElement(getGroupCitationDisplay(child));
			}
		}
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
		int idx = groupCitationList.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord existing = groupCitationRecords.get(idx);
		if(groupCitationHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Group Citation handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
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
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this group citation?")) return;
		groupCitationRecords.remove(idx);
		groupCitationListModel.remove(idx);
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


	// ==================== Cultural Norms ====================
	private void loadCulturalNorms(){
		culturalNormListModel.clear();
		culturalNormIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				culturalNormListModel.addElement(getCulturalNormDisplayName(id));
			}
		}
	}

	private String getCulturalNormDisplayName(String id){
		if(culturalNormHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return culturalNormHandler.getDisplayName(rec);
		}
		return id;
	}

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


	// ==================== Notes ====================
	private void loadNotes(){
		noteListModel.clear();
		noteIds.clear();
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
	}

	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return noteHandler.getDisplayName(rec);
		}
		return id;
	}

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
		if(idx == -1) return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this note reference?")) return;
		String id = noteIds.remove(idx);
		noteDisplayMap.remove(id);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
				break;
			}
		}
	}


	// ==================== Source Citations ====================
	private void loadSourceCitations(){
		sourceCitationListModel.clear();
		sourceCitationRecords.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}
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
		else if(list == nameList){
			createNewName();
		}
		else if(list == childFamilyList){
			createNewChildFamily();
		}
		else if(list == parentFamilyList){
			createNewParentFamily();
		}
		else if(list == associationList){
			createNewAssociation();
		}
		else if(list == aliasList){
			createNewAlias();
		}
	}

	private void createNewName(){
		// Il metodo addName() apre già il dialog e aggiunge automaticamente il nuovo nome
		addName();
	}

	private void createNewChildFamily(){
		if(familyHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Family handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// Salva gli ID delle family esistenti
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("FAMILY")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		// Crea una nuova famiglia
		JDialog dialog = familyHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		// Trova il nuovo ID
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("FAMILY")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty()){
			// Apri il FamilyLinkDialog con la nuova famiglia pre-selezionata
			FLEFRecord link = showFamilyLinkDialog(newId, "FAMILY_CHILD", null);
			if(link != null){
				childFamilyIds.add(newId);
				childFamilyLinkDetails.put(newId, link);
				childFamilyListModel.addElement(getFamilyDisplayName(newId));
			}
			else{
				// Se l'utente annulla il link dialog, rimuovi la famiglia appena creata
				model.removeRecord(newId);
				JOptionPane.showMessageDialog(getParentFrame(),
					"Family creation cancelled. The family record has been removed.",
					"Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void createNewParentFamily(){
		if(familyHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Family handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("FAMILY")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = familyHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("FAMILY")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty()){
			FLEFRecord link = showFamilyLinkDialog(newId, "FAMILY_PARENT", null);
			if(link != null){
				parentFamilyIds.add(newId);
				parentFamilyLinkDetails.put(newId, link);
				parentFamilyListModel.addElement(getFamilyDisplayName(newId));
			}
			else{
				model.removeRecord(newId);
				JOptionPane.showMessageDialog(getParentFrame(),
					"Family creation cancelled. The family record has been removed.",
					"Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void createNewAssociation(){
		AssociationDialog dialog = AssociationDialog.createNew(this, model);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord assoc = dialog.getAssociationRecord();
			String display = getAssociationDisplay(assoc);
			associationListModel.addElement(display);
			associationRecords.put(display, assoc);
		}
	}

	private void createNewAlias(){
		AliasDialog dialog = AliasDialog.createNew(getParentFrame(), model);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			AliasDialog.AliasEntry entry = dialog.getEntry();
			if(entry != null){
				aliasEntries.add(entry);
				aliasListModel.addElement(entry.toString());
			}
		}
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
		if(sourceId == null) return;

		BufferedImage image = loadImageFromSource(sourceId);
		if(image == null){
			JOptionPane.showMessageDialog(this,
				"Could not load image from the selected source.",
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		//FIXME
//		ImageCropDialog cropDialog = new ImageCropDialog(this, image);
//		cropDialog.setVisible(true);
//
//		Rectangle cropRect = cropDialog.getCrop();
//		if(cropRect != null){
//			preferredImageId = sourceId;
//			cropString = cropRect.x + " " + cropRect.y + " " + cropRect.width + " " + cropRect.height;
//			updateImageButton(sourceId);
//		}
	}

	/**
	 * Loads the image from a SOURCE_RECORD.
	 *
	 * @param sourceId the ID of the source record
	 * @return the loaded BufferedImage, or null if not found
	 */
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

	/**
	 * Updates the image button with a thumbnail of the selected image.
	 *
	 * @param sourceId the ID of the source record
	 */
	private void updateImageButton(String sourceId){
		BufferedImage img = loadImageFromSource(sourceId);
		if(img != null){
			Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
			imageButton.setIcon(new ImageIcon(scaled));
		}
		else{
			imageButton.setIcon(createPlaceholderIcon());
		}
	}

	/**
	 * Clears the preferred image (resets state and updates UI).
	 */
	private void clearImage(){
		preferredImageId = null;
		cropString = null;
		imageButton.setIcon(createPlaceholderIcon());
	}


	// ==================== Preferred Image ====================
	private void loadPreferredImage(){
		FLEFRecord pref = FLEFRecordUtils.findChild(record, "PREFERRED_IMAGE");
		if(pref != null){
			preferredImageId = pref.getValue();
			cropString = FLEFRecordUtils.getChildValue(pref, "CROP");
			updateImageButton(preferredImageId);
		}
		else{
			clearImage();
		}
	}


	// ==================== Validation ====================
	@Override
	protected boolean validateData(){
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required for an individual.\nPlease add a CREATION date.",
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

		// Basic
		FLEFRecordUtils.updateChildValue(record, "SEX", (String)sexCombo.getSelectedItem());
		FLEFRecordUtils.updateChildValue(record, "RESTRICTION",
			restrictionCheckBox.isSelected()? "confidential": null);

		// Names
		for(FLEFRecord name : nameRecords){
			record.addChild(name);
		}

		// Family links
		for(String id : childFamilyIds){
			FLEFRecord link = childFamilyLinkDetails.get(id);
			if(link != null) record.addChild(link);
		}
		for(String id : parentFamilyIds){
			FLEFRecord link = parentFamilyLinkDetails.get(id);
			if(link != null) record.addChild(link);
		}

		// Associations
		for(String key : Collections.list(associationListModel.elements())){
			FLEFRecord assoc = associationRecords.get(key);
			if(assoc != null) record.addChild(assoc);
		}

		// Aliases
		for(AliasDialog.AliasEntry entry : aliasEntries){
			FLEFRecord alias = new FLEFRecord();
			alias.setLevel(1);
			alias.setTag("ALIAS");
			alias.setValue(entry.aliasId);
			FLEFRecordUtils.updateChildValue(alias, "CERTAINTY", entry.certainty);
			FLEFRecordUtils.updateChildValue(alias, "CREDIBILITY", entry.credibility);
			for(String noteId : entry.noteIds){
				FLEFRecordUtils.addChild(alias, "NOTE", 2, noteId);
			}
			record.addChild(alias);
		}

		// Events
		for(String id : eventIds){
			FLEFRecordUtils.addChild(record, "EVENT", 1, id);
		}

		// Group Citations
		for(FLEFRecord citation : groupCitationRecords){
			citation.setLevel(1);
			citation.setTag("GROUP_CITATION");
			record.addChild(citation);
		}

		// Cultural Norms
		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(record, "CULTURAL_NORM", 1, id);
		}

		// Notes
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		// Source Citations
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// Preferred Image
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			FLEFRecord pref = new FLEFRecord();
			pref.setLevel(1);
			pref.setTag("PREFERRED_IMAGE");
			pref.setValue(preferredImageId);
			record.addChild(pref);
			if(cropString != null && !cropString.isEmpty()){
				FLEFRecordUtils.updateChildValue(pref, "CROP", cropString);
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


	// ==================== Overrides ====================
	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), "INDIVIDUAL");
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "INDIVIDUAL", "I");
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Individual Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Individual");
			btn.addActionListener(e -> {
				IndividualDialog dialog = IndividualDialog.createNew(frame, model);
				dialog.setVisible(true);
				System.out.println("Individual saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
