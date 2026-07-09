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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.*;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;


/**
 * Dialog for editing an INDIVIDUAL_RECORD according to FLEF 0.0.9.
 * <p>
 * Features:
 * <ul>
 *   <li>Basic: ID, SEX, RESTRICTION</li>
 *   <li>Names: PERSONAL_NAME_STRUCTURE (0:M) with full NameDialog</li>
 *   <li>Family: FAMILY_CHILD and FAMILY_PARTNER with details (CERTAINTY, CREDIBILITY, NOTE, CONCLUSION)</li>
 *   <li>Associations: ASSOCIATION @<XREF:ID>@ and @VOID@ with NOTE and SOURCE_CITATION</li>
 *   <li>Aliases: ALIAS @<XREF:INDIVIDUAL>@</li>
 *   <li>Events: EVENT @<XREF:EVENT>@</li>
 *   <li>Group Citations: GROUP_CITATION</li>
 *   <li>Cultural Norms: CULTURAL_NORM</li>
 *   <li>Preferred Image: PREFERRED_IMAGE with CROP</li>
 *   <li>Notes, Sources, Conclusions, Modification (text areas)</li>
 * </ul>
 */
public class IndividualDialog extends BaseRecordDialog{

	// ==================== Static initializer for handlers ====================
	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}

	// ==================== Basic fields ====================
	private final JTextField idField = new JTextField(10);
	private final JComboBox<String> sexCombo = new JComboBox<>(new String[]{"", "MALE", "FEMALE", "UNKNOWN"});
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// ==================== Names ====================
	private final DefaultListModel<String> nameListModel = new DefaultListModel<>();
	private final JList<String> nameList = new JList<>(nameListModel);
	private final List<FLEFRecord> nameRecords = new ArrayList<>();

	// ==================== Family links ====================
	private final DefaultListModel<String> childFamilyListModel = new DefaultListModel<>();
	private final JList<String> childFamilyList = new JList<>(childFamilyListModel);
	private final List<String> childFamilyIds = new ArrayList<>();
	private final Map<String, FLEFRecord> childFamilyLinkDetails = new HashMap<>();

	private final DefaultListModel<String> partnerFamilyListModel = new DefaultListModel<>();
	private final JList<String> partnerFamilyList = new JList<>(partnerFamilyListModel);
	private final List<String> partnerFamilyIds = new ArrayList<>();
	private final Map<String, FLEFRecord> partnerFamilyLinkDetails = new HashMap<>();

	// ==================== Associations ====================
	private final DefaultListModel<String> associationListModel = new DefaultListModel<>();
	private final JList<String> associationList = new JList<>(associationListModel);
	private final Map<String, FLEFRecord> associationRecords = new HashMap<>();

	// ==================== Aliases ====================
	private final DefaultListModel<String> aliasListModel = new DefaultListModel<>();
	private final JList<String> aliasList = new JList<>(aliasListModel);
	private final List<String> aliasIds = new ArrayList<>();

	// ==================== Events ====================
	private final DefaultListModel<String> eventListModel = new DefaultListModel<>();
	private final JList<String> eventList = new JList<>(eventListModel);
	private final List<String> eventIds = new ArrayList<>();

	// ==================== Group Citations ====================
	private final DefaultListModel<String> groupCitationListModel = new DefaultListModel<>();
	private final JList<String> groupCitationList = new JList<>(groupCitationListModel);
	private final List<String> groupCitationIds = new ArrayList<>();

	// ==================== Cultural Norms ====================
	private final DefaultListModel<String> culturalNormListModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormListModel);
	private final List<String> culturalNormIds = new ArrayList<>();

	// ==================== Preferred Image ====================
	private final JTextField preferredImageField = new JTextField(15);
	private final JTextField cropField = new JTextField(20);

	// ==================== Placeholder tabs ====================
	private final JTextArea notesArea = new JTextArea(5, 30);
	private final JTextArea sourcesArea = new JTextArea(5, 30);
	private final JTextArea conclusionsArea = new JTextArea(5, 30);
	private final JTextArea modificationArea = new JTextArea(5, 30);

	// ==================== Buttons ====================
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ==================== Handlers ====================
	private final RecordTypeHandler<?> familyHandler = HandlerRegistry.getHandler("FAMILY");
	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");
	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	/**
	 * Constructor for editing an existing individual.
	 */
	public IndividualDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, "Edit Individual");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(950, 700));
		pack();
		setLocationRelativeTo(parent);
	}

	/**
	 * Constructor for creating a new individual.
	 */
	public IndividualDialog(Frame parent, FLEFModel model){
		super(parent, model, "New Individual");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(950, 700));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// ===== Basic tab =====
		JPanel basicPanel = new JPanel(new MigLayout("fill", "[right]rel[grow]", "[]10[]10[]10"));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		idField.setEditable(false);
		idField.setText(record.getId());
		basicPanel.add(new JLabel("ID:"), "align label");
		basicPanel.add(idField, "grow,wrap");

		basicPanel.add(new JLabel("Sex:"), "align label");
		basicPanel.add(sexCombo, "grow,wrap");

		basicPanel.add(restrictionCheckBox, "span 2,wrap");

		tabbedPane.addTab("Basic", basicPanel);

		// ===== Names tab =====
		JPanel namesPanel = createListPanel(
			"Personal Names (0:M)",
			nameList,
			nameListModel,
			this::addName,
			this::editName,
			this::deleteName
		);
		tabbedPane.addTab("Names", namesPanel);

		// ===== Family tab =====
		JPanel familyPanel = new JPanel(new GridLayout(1, 2, 10, 10));
		familyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel childPanel = createListPanel(
			"Child of Families (0:M)",
			childFamilyList,
			childFamilyListModel,
			this::addChildFamily,
			this::editChildFamily,
			this::deleteChildFamily
		);

		JPanel partnerPanel = createListPanel(
			"Partner in Families (0:M)",
			partnerFamilyList,
			partnerFamilyListModel,
			this::addPartnerFamily,
			this::editPartnerFamily,
			this::deletePartnerFamily
		);

		familyPanel.add(childPanel);
		familyPanel.add(partnerPanel);
		tabbedPane.addTab("Family", familyPanel);

		// ===== Associations tab =====
		JPanel associationsPanel = createListPanel(
			"Associations (0:M)",
			associationList,
			associationListModel,
			this::addAssociation,
			this::editAssociation,
			this::deleteAssociation
		);
		tabbedPane.addTab("Associations", associationsPanel);

		// ===== Aliases tab =====
		JPanel aliasesPanel = createListPanel(
			"Aliases (0:M)",
			aliasList,
			aliasListModel,
			this::addAlias,
			this::editAlias,
			this::deleteAlias
		);
		tabbedPane.addTab("Aliases", aliasesPanel);

		// ===== Events tab =====
		JPanel eventsPanel = createListPanel(
			"Events (0:M)",
			eventList,
			eventListModel,
			this::addEvent,
			this::editEvent,
			this::deleteEvent
		);
		tabbedPane.addTab("Events", eventsPanel);

		// ===== Group Citations tab =====
		JPanel groupCitationsPanel = createListPanel(
			"Group Citations (0:M)",
			groupCitationList,
			groupCitationListModel,
			this::addGroupCitation,
			this::editGroupCitation,
			this::deleteGroupCitation
		);
		tabbedPane.addTab("Group Citations", groupCitationsPanel);

		// ===== Cultural Norms tab =====
		JPanel culturalNormsPanel = createListPanel(
			"Cultural Norms (0:M)",
			culturalNormList,
			culturalNormListModel,
			this::addCulturalNorm,
			this::editCulturalNorm,
			this::deleteCulturalNorm
		);
		tabbedPane.addTab("Cultural Norms", culturalNormsPanel);

		// ===== Preferred Image tab =====
		JPanel imagePanel = new JPanel(new MigLayout("fill", "[right]rel[grow]", "[]10[]10"));
		imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		imagePanel.add(new JLabel("Preferred Image (Source ID):"), "align label");
		JPanel prefPanel = new JPanel(new BorderLayout(5, 0));
		prefPanel.add(preferredImageField, BorderLayout.CENTER);
		JButton browseBtn = new JButton("Browse...");
		prefPanel.add(browseBtn, BorderLayout.EAST);
		imagePanel.add(prefPanel, "grow,wrap");

		imagePanel.add(new JLabel("Crop (top left bottom right):"), "align label");
		imagePanel.add(cropField, "grow");

		browseBtn.addActionListener(e -> browsePreferredImage());

		tabbedPane.addTab("Preferred Image", imagePanel);

		// ===== Notes tab =====
		JPanel notesPanel = createTextAreaPanel(notesArea, "Notes");
		tabbedPane.addTab("Notes", notesPanel);

		// ===== Sources tab =====
		JPanel sourcesPanel = createTextAreaPanel(sourcesArea, "Source Citations");
		tabbedPane.addTab("Sources", sourcesPanel);

		// ===== Conclusions tab =====
		JPanel conclusionsPanel = createTextAreaPanel(conclusionsArea, "Conclusions");
		tabbedPane.addTab("Conclusions", conclusionsPanel);

		// ===== Modification tab =====
		JPanel modificationPanel = createTextAreaPanel(modificationArea, "Modification History");
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// ===== Button panel =====
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> saveRecord());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Helper methods for creating panels ====================

	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
		Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder(title));

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 150));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		addBtn.addActionListener(e -> addAction.run());
		editBtn.addActionListener(e -> editAction.run());
		deleteBtn.addActionListener(e -> deleteAction.run());

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		return panel;
	}

	private JPanel createTextAreaPanel(JTextArea area, String title){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(title));
		JScrollPane scroll = new JScrollPane(area);
		scroll.setPreferredSize(new Dimension(200, 150));
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	// ==================== Data loading ====================

	@Override
	protected void loadData(){
		// Basic info
		idField.setText(record.getId());
		sexCombo.setSelectedItem(getChildValue("SEX"));
		restrictionCheckBox.setSelected("confidential".equals(getChildValue("RESTRICTION")));

		// Names
		loadNames();

		// Family links
		loadFamilyLinks();

		// Associations
		loadAssociations();

		// Aliases
		loadAliases();

		// Events
		loadEvents();

		// Group citations
		loadGroupCitations();

		// Cultural norms
		loadCulturalNorms();

		// Preferred image
		loadPreferredImage();

		// Placeholders
		notesArea.setText(getChildValue("NOTE"));
		sourcesArea.setText(getChildValue("SOURCE_CITATION"));
		conclusionsArea.setText(getChildValue("CONCLUSION"));
		modificationArea.setText(getChildValue("MODIFICATION"));
	}

	// ==================== Names handling ====================

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
		String given = getChildValue(nameRecord, "INDIVIDUAL_NAME");
		String family = getChildValue(nameRecord, "FAMILY_NAME");
		String type = getChildValue(nameRecord, "TYPE");
		String title = getChildValue(nameRecord, "TITLE");
		String suffix = null;
		FLEFRecord givenNode = findChild(nameRecord, "INDIVIDUAL_NAME");
		if(givenNode != null){
			suffix = getChildValue(givenNode, "SUFFIX");
		}
		String nickname = getChildValue(nameRecord, "INDIVIDUAL_NICKNAME");
		String familyNickname = getChildValue(nameRecord, "FAMILY_NICKNAME");

		StringBuilder sb = new StringBuilder();
		if(title != null) sb.append(title).append(" ");
		if(given != null) sb.append(given);
		if(suffix != null) sb.append(" ").append(suffix);
		if(family != null){
			if(sb.length() > 0) sb.append(" ");
			sb.append(family);
		}
		if(nickname != null) sb.append(" (\"").append(nickname).append("\")");
		if(familyNickname != null) sb.append(" [fam: ").append(familyNickname).append("]");
		if(type != null) sb.append(" (").append(type).append(")");
		if(sb.length() == 0) sb.append("[unnamed]");
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
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this name?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			FLEFRecord nameRecord = nameRecords.remove(idx);
			record.getChildren().remove(nameRecord);
			nameListModel.remove(idx);
		}
	}

	// ==================== Family links handling ====================

	private void loadFamilyLinks(){
		// Child families
		childFamilyListModel.clear();
		childFamilyIds.clear();
		childFamilyLinkDetails.clear();
		for(FLEFRecord child : record.getChildren()){
			if("FAMILY_CHILD".equals(child.getTag()) && child.getValue() != null){
				String familyId = child.getValue();
				childFamilyIds.add(familyId);
				childFamilyLinkDetails.put(familyId, child);
				childFamilyListModel.addElement(getFamilyDisplayName(familyId));
			}
		}

		// Partner families
		partnerFamilyListModel.clear();
		partnerFamilyIds.clear();
		partnerFamilyLinkDetails.clear();
		for(FLEFRecord child : record.getChildren()){
			if("FAMILY_PARTNER".equals(child.getTag()) && child.getValue() != null){
				String familyId = child.getValue();
				partnerFamilyIds.add(familyId);
				partnerFamilyLinkDetails.put(familyId, child);
				partnerFamilyListModel.addElement(getFamilyDisplayName(familyId));
			}
		}
	}

	private String getFamilyDisplayName(String familyId){
		if(familyHandler != null){
			FLEFRecord family = model.getRecordById(familyId);
			if(family != null){
				return familyHandler.getDisplayName(family);
			}
		}
		return familyId;
	}

	private void addChildFamily(){
		if(familyHandler == null){
			showError("Error", "Family handler not registered!");
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, familyHandler, selectedId -> {
			if(selectedId != null && !childFamilyIds.contains(selectedId)){
				FLEFRecord linkRecord = showFamilyLinkDialog(selectedId, "FAMILY_CHILD", null);
				if(linkRecord != null){
					childFamilyIds.add(selectedId);
					childFamilyLinkDetails.put(selectedId, linkRecord);
					childFamilyListModel.addElement(getFamilyDisplayName(selectedId));
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editChildFamily(){
		int idx = childFamilyList.getSelectedIndex();
		if(idx == -1) return;
		String familyId = childFamilyIds.get(idx);
		FLEFRecord existingLink = childFamilyLinkDetails.get(familyId);
		if(existingLink == null){
			existingLink = new FLEFRecord();
			existingLink.setLevel(1);
			existingLink.setTag("FAMILY_CHILD");
			existingLink.setValue(familyId);
		}
		FLEFRecord updatedLink = showFamilyLinkDialog(familyId, "FAMILY_CHILD", existingLink);
		if(updatedLink != null){
			childFamilyLinkDetails.put(familyId, updatedLink);
			childFamilyListModel.set(idx, getFamilyDisplayName(familyId));
		}
	}

	private void deleteChildFamily(){
		int idx = childFamilyList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this family link?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String familyId = childFamilyIds.remove(idx);
			childFamilyLinkDetails.remove(familyId);
			childFamilyListModel.remove(idx);
		}
	}

	private void addPartnerFamily(){
		if(familyHandler == null){
			showError("Error", "Family handler not registered!");
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, familyHandler, selectedId -> {
			if(selectedId != null && !partnerFamilyIds.contains(selectedId)){
				FLEFRecord linkRecord = showFamilyLinkDialog(selectedId, "FAMILY_PARTNER", null);
				if(linkRecord != null){
					partnerFamilyIds.add(selectedId);
					partnerFamilyLinkDetails.put(selectedId, linkRecord);
					partnerFamilyListModel.addElement(getFamilyDisplayName(selectedId));
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editPartnerFamily(){
		int idx = partnerFamilyList.getSelectedIndex();
		if(idx == -1) return;
		String familyId = partnerFamilyIds.get(idx);
		FLEFRecord existingLink = partnerFamilyLinkDetails.get(familyId);
		if(existingLink == null){
			existingLink = new FLEFRecord();
			existingLink.setLevel(1);
			existingLink.setTag("FAMILY_PARTNER");
			existingLink.setValue(familyId);
		}
		FLEFRecord updatedLink = showFamilyLinkDialog(familyId, "FAMILY_PARTNER", existingLink);
		if(updatedLink != null){
			partnerFamilyLinkDetails.put(familyId, updatedLink);
			partnerFamilyListModel.set(idx, getFamilyDisplayName(familyId));
		}
	}

	private void deletePartnerFamily(){
		int idx = partnerFamilyList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this family link?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String familyId = partnerFamilyIds.remove(idx);
			partnerFamilyLinkDetails.remove(familyId);
			partnerFamilyListModel.remove(idx);
		}
	}

	private FLEFRecord showFamilyLinkDialog(String familyId, String linkType, FLEFRecord existingLink){
		FamilyLinkDialog dialog = new FamilyLinkDialog(this, model, familyId, linkType, existingLink);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			return dialog.getLinkRecord();
		}
		return null;
	}

	// ==================== Associations handling ====================

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
		boolean isVoid = "@VOID@".equals(value) || association.getId() != null && "VOID".equals(association.getId());

		if(isVoid){
			String name = getChildValue(association, "NAME");
			return "VOID: " + (name != null && !name.isEmpty()? name: "[unnamed]");
		}
		else{
			String targetId = value != null? value: association.getId();
			if(targetId != null){
				FLEFRecord target = model.getRecordById(targetId);
				if(target != null){
					RecordTypeHandler<?> handler = HandlerRegistry.getHandler(target.getType());
					if(handler != null){
						return handler.getDisplayName(target);
					}
				}
			}
			return "ID: " + targetId;
		}
	}

	private void addAssociation(){
		AssociationDialog dialog = new AssociationDialog(this, model, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord association = dialog.getAssociationRecord();
			String display = getAssociationDisplay(association);
			associationListModel.addElement(display);
			associationRecords.put(display, association);
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
		AssociationDialog dialog = new AssociationDialog(this, model, existing);
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
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this association?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String key = associationListModel.get(idx);
			associationRecords.remove(key);
			associationListModel.remove(idx);
		}
	}

	// ==================== Aliases handling ====================

	private void loadAliases(){
		aliasListModel.clear();
		aliasIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("ALIAS".equals(child.getTag()) && child.getValue() != null){
				String aliasId = child.getValue();
				aliasIds.add(aliasId);
				aliasListModel.addElement(getIndividualDisplayName(aliasId));
			}
		}
	}

	private String getIndividualDisplayName(String individualId){
		if(individualHandler != null){
			FLEFRecord ind = model.getRecordById(individualId);
			if(ind != null){
				return individualHandler.getDisplayName(ind);
			}
		}
		return individualId;
	}

	private void addAlias(){
		if(individualHandler == null){
			showError("Error", "Individual handler not registered!");
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null && !aliasIds.contains(selectedId)){
				aliasIds.add(selectedId);
				aliasListModel.addElement(getIndividualDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editAlias(){
		int idx = aliasList.getSelectedIndex();
		if(idx == -1) return;
		String aliasId = aliasIds.get(idx);
		if(individualHandler == null) return;
		FLEFRecord alias = model.getRecordById(aliasId);
		if(alias == null){
			showError("Error", "Alias not found: " + aliasId);
			return;
		}
		JDialog dialog = (JDialog)individualHandler.createEditDialog(getParentFrame(), model, alias);
		dialog.setVisible(true);
		aliasListModel.set(idx, getIndividualDisplayName(aliasId));
	}

	private void deleteAlias(){
		int idx = aliasList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this alias?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			aliasIds.remove(idx);
			aliasListModel.remove(idx);
		}
	}

	// ==================== Events handling ====================

	private void loadEvents(){
		eventListModel.clear();
		eventIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("EVENT".equals(child.getTag()) && child.getValue() != null){
				String eventId = child.getValue();
				eventIds.add(eventId);
				eventListModel.addElement(getEventDisplayName(eventId));
			}
		}
	}

	private String getEventDisplayName(String eventId){
		if(eventHandler != null){
			FLEFRecord event = model.getRecordById(eventId);
			if(event != null){
				return eventHandler.getDisplayName(event);
			}
		}
		return eventId;
	}

	private void addEvent(){
		if(eventHandler == null){
			showError("Error", "Event handler not registered!");
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, eventHandler, selectedId -> {
			if(selectedId != null && !eventIds.contains(selectedId)){
				eventIds.add(selectedId);
				eventListModel.addElement(getEventDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1) return;
		String eventId = eventIds.get(idx);
		if(eventHandler == null) return;
		FLEFRecord event = model.getRecordById(eventId);
		if(event == null){
			showError("Error", "Event not found: " + eventId);
			return;
		}
		JDialog dialog = (JDialog)eventHandler.createEditDialog(getParentFrame(), model, event);
		dialog.setVisible(true);
		eventListModel.set(idx, getEventDisplayName(eventId));
	}

	private void deleteEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this event?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			eventIds.remove(idx);
			eventListModel.remove(idx);
		}
	}

	// ==================== Group Citations handling ====================

	private void loadGroupCitations(){
		groupCitationListModel.clear();
		groupCitationIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("GROUP_CITATION".equals(child.getTag()) && child.getValue() != null){
				String groupId = child.getValue();
				groupCitationIds.add(groupId);
				groupCitationListModel.addElement(getGroupDisplayName(groupId));
			}
		}
	}

	private String getGroupDisplayName(String groupId){
		if(groupHandler != null){
			FLEFRecord group = model.getRecordById(groupId);
			if(group != null){
				return groupHandler.getDisplayName(group);
			}
		}
		return groupId;
	}

	private void addGroupCitation(){
		if(groupHandler == null){
			showError("Error", "Group handler not registered!");
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, groupHandler, selectedId -> {
			if(selectedId != null && !groupCitationIds.contains(selectedId)){
				groupCitationIds.add(selectedId);
				groupCitationListModel.addElement(getGroupDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editGroupCitation(){
		int idx = groupCitationList.getSelectedIndex();
		if(idx == -1) return;
		String groupId = groupCitationIds.get(idx);
		if(groupHandler == null) return;
		FLEFRecord group = model.getRecordById(groupId);
		if(group == null){
			showError("Error", "Group not found: " + groupId);
			return;
		}
		JDialog dialog = (JDialog)groupHandler.createEditDialog(getParentFrame(), model, group);
		dialog.setVisible(true);
		groupCitationListModel.set(idx, getGroupDisplayName(groupId));
	}

	private void deleteGroupCitation(){
		int idx = groupCitationList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this group citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			groupCitationIds.remove(idx);
			groupCitationListModel.remove(idx);
		}
	}

	// ==================== Cultural Norms handling ====================

	private void loadCulturalNorms(){
		culturalNormListModel.clear();
		culturalNormIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String normId = child.getValue();
				culturalNormIds.add(normId);
				culturalNormListModel.addElement(getCulturalNormDisplayName(normId));
			}
		}
	}

	private String getCulturalNormDisplayName(String normId){
		if(culturalNormHandler != null){
			FLEFRecord norm = model.getRecordById(normId);
			if(norm != null){
				return culturalNormHandler.getDisplayName(norm);
			}
		}
		return normId;
	}

	private void addCulturalNorm(){
		if(culturalNormHandler == null){
			showError("Error", "Cultural norm handler not registered!");
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				culturalNormListModel.addElement(getCulturalNormDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1) return;
		String normId = culturalNormIds.get(idx);
		if(culturalNormHandler == null) return;
		FLEFRecord norm = model.getRecordById(normId);
		if(norm == null){
			showError("Error", "Cultural norm not found: " + normId);
			return;
		}
		JDialog dialog = (JDialog)culturalNormHandler.createEditDialog(getParentFrame(), model, norm);
		dialog.setVisible(true);
		culturalNormListModel.set(idx, getCulturalNormDisplayName(normId));
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1) return;
		int confirm = JOptionPane.showConfirmDialog(this, "Remove this cultural norm?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			culturalNormIds.remove(idx);
			culturalNormListModel.remove(idx);
		}
	}

	// ==================== Preferred Image handling ====================

	private void loadPreferredImage(){
		FLEFRecord prefImage = findChild("PREFERRED_IMAGE");
		if(prefImage != null){
			preferredImageField.setText(prefImage.getValue());
			cropField.setText(getChildValue(prefImage, "CROP"));
		}
		else{
			preferredImageField.setText("");
			cropField.setText("");
		}
	}

	private void browsePreferredImage(){
		if(sourceHandler == null){
			showError("Error", "Source handler not registered!");
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, sourceHandler, selectedId -> {
			if(selectedId != null){
				preferredImageField.setText(selectedId);
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		// Basic info
		updateChildValue("SEX", (String)sexCombo.getSelectedItem());
		updateChildValue("RESTRICTION", restrictionCheckBox.isSelected()? "confidential": null);

		// Names: they are already in the record's children

		// Family links: rebuild from stored details
		FLEFRecordUtils.removeChildren(record, "FAMILY_CHILD");
		FLEFRecordUtils.removeChildren(record, "FAMILY_PARTNER");
		for(String id : childFamilyIds){
			FLEFRecord link = childFamilyLinkDetails.get(id);
			if(link != null){
				record.addChild(link);
			}
			else{
				FLEFRecord childLink = new FLEFRecord();
				childLink.setLevel(1);
				childLink.setTag("FAMILY_CHILD");
				childLink.setValue(id);
				record.addChild(childLink);
			}
		}
		for(String id : partnerFamilyIds){
			FLEFRecord link = partnerFamilyLinkDetails.get(id);
			if(link != null){
				record.addChild(link);
			}
			else{
				FLEFRecord partnerLink = new FLEFRecord();
				partnerLink.setLevel(1);
				partnerLink.setTag("FAMILY_PARTNER");
				partnerLink.setValue(id);
				record.addChild(partnerLink);
			}
		}

		// Associations: rebuild from stored records
		FLEFRecordUtils.removeChildren(record, "ASSOCIATION");
		for(String key : Collections.list(associationListModel.elements())){
			FLEFRecord assoc = associationRecords.get(key);
			if(assoc != null){
				record.addChild(assoc);
			}
		}

		// Aliases: rebuild
		FLEFRecordUtils.removeChildren(record, "ALIAS");
		for(String id : aliasIds){
			FLEFRecordUtils.addChild(record, "ALIAS", 1, id);
		}

		// Events: rebuild
		FLEFRecordUtils.removeChildren(record, "EVENT");
		for(String id : eventIds){
			FLEFRecordUtils.addChild(record, "EVENT", 1, id);
		}

		// Group citations: rebuild
		FLEFRecordUtils.removeChildren(record, "GROUP_CITATION");
		for(String id : groupCitationIds){
			FLEFRecordUtils.addChild(record, "GROUP_CITATION", 1, id);
		}

		// Cultural norms: rebuild
		FLEFRecordUtils.removeChildren(record, "CULTURAL_NORM");
		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(record, "CULTURAL_NORM", 1, id);
		}

		// Preferred image
		String prefImage = preferredImageField.getText().trim();
		String crop = cropField.getText().trim();
		FLEFRecordUtils.removeChildren(record, "PREFERRED_IMAGE");
		if(!prefImage.isEmpty()){
			FLEFRecord pref = new FLEFRecord();
			pref.setLevel(1);
			pref.setTag("PREFERRED_IMAGE");
			pref.setValue(prefImage);
			record.addChild(pref);
			if(!crop.isEmpty()){
				FLEFRecord cropChild = new FLEFRecord();
				cropChild.setLevel(2);
				cropChild.setTag("CROP");
				cropChild.setValue(crop);
				pref.addChild(cropChild);
			}
		}

		// Placeholders
		updateChildValue("NOTE", notesArea.getText().trim());
		updateChildValue("SOURCE_CITATION", sourcesArea.getText().trim());
		updateChildValue("CONCLUSION", conclusionsArea.getText().trim());
		updateChildValue("MODIFICATION", modificationArea.getText().trim());

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("INDIVIDUAL");
		newRecord.setId(generateNewId());
		return newRecord;
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

	// ==================== Main for testing ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Add sample individual
		FLEFRecord ind = new FLEFRecord();
		ind.setId("I1");
		ind.setType("INDIVIDUAL");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		FLEFRecord given = new FLEFRecord();
		given.setLevel(2);
		given.setTag("INDIVIDUAL_NAME");
		given.setValue("John");
		name.addChild(given);
		FLEFRecord family = new FLEFRecord();
		family.setLevel(2);
		family.setTag("FAMILY_NAME");
		family.setValue("Doe");
		name.addChild(family);
		ind.addChild(name);
		model.addRecord(ind);

		// Add sample family
		FLEFRecord fam = new FLEFRecord();
		fam.setId("F1");
		fam.setType("FAMILY");
		model.addRecord(fam);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Individual Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton editBtn = new JButton("Edit Individual I1");
			editBtn.addActionListener(e -> {
				FLEFRecord rec = model.getRecordById("I1");
				if(rec != null){
					IndividualDialog dialog = new IndividualDialog(frame, model, rec);
					dialog.setVisible(true);
					System.out.println("Individual updated.");
				}
			});

			JButton newBtn = new JButton("New Individual");
			newBtn.addActionListener(e -> {
				IndividualDialog dialog = new IndividualDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("New individual created.");
			});

			frame.add(editBtn);
			frame.add(newBtn);
			frame.setVisible(true);
			System.out.println("Test frame is visible.");
		});
	}

}
