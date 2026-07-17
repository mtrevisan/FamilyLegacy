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
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
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

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Dialog for editing a PLACE_RECORD according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * PLACE_RECORD :=
 *   n @<XREF:PLACE>@ PLACE    {1:1}
 *     +1 NAME <PLACE_NAME>    {0:1}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *     +1 ADDRESS <ADDRESS_LINE>    {0:M}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *       +2 HIERARCHY <ADDRESS_HIERARCHY>    {0:1}
 *       +2 CULTURAL_NORM @<XREF:RULE>@    {0:M}
 *       +2 NOTE @<XREF:NOTE>@    {0:M}
 *       +2 <<SOURCE_CITATION>>    {0:M}
 *     +1 MAP    {0:1}
 *       +2 LATITUDE <PLACE_LATITUDE>    {1:1}
 *       +2 LONGITUDE <PLACE_LONGITUDE>    {1:1}
 *       +2 CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
 *       +2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
 *     +1 SUBORDINATE @<XREF:PLACE>@    {0:1}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class PlaceDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2581031991500033899L;


	static{
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CalendarHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JTextField nameField = new JTextField(30);
	private final JButton nameTransBtn = new JButton("📝");
	private JPanel nameTransPanel;
	private final DefaultListModel<String> nameTransModel = new DefaultListModel<>();
	private final List<FLEFRecord> nameTransRecords = new ArrayList<>();

	// ========== ADDRESS (0:M) ==========
	private final DefaultListModel<AddressEntry> addressListModel = new DefaultListModel<>();
	private final JList<AddressEntry> addressList = new JList<>(addressListModel);
	private final List<AddressEntry> addressEntries = new ArrayList<>();

	// ========== MAP (0:1) ==========
	private final JTextField latitudeField = new JTextField(15);
	private final JTextField longitudeField = new JTextField(15);
	private final EvidenceQualifiersPanel mapQualifiers = new EvidenceQualifiersPanel("Map Evidence");

	// ========== SUBORDINATE (0:1) ==========
	private final JTextField subordinateDisplayField = new JTextField(20);
	private final JButton subordinateBrowseBtn = new JButton("Browse...");
	private final JButton subordinateClearBtn = new JButton("Clear");
	private String selectedSubordinateId;

	// ========== MODIFICATION (1:1) ==========
	private final ModificationPanel modificationPanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> placeHandler = HandlerRegistry.getHandler("PLACE");
	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ========== Inner class for Address entry ==========
	private static class AddressEntry{
		String address;
		String hierarchy;
		List<FLEFRecord> transcriptions;
		List<String> culturalNormIds;
		List<String> noteIds;
		List<FLEFRecord> sourceCitations;

		AddressEntry(String address, String hierarchy,
			List<FLEFRecord> transcriptions,
			List<String> culturalNormIds,
			List<String> noteIds,
			List<FLEFRecord> sourceCitations){
			this.address = address;
			this.hierarchy = hierarchy;
			this.transcriptions = transcriptions != null? transcriptions: new ArrayList<>();
			this.culturalNormIds = culturalNormIds != null? culturalNormIds: new ArrayList<>();
			this.noteIds = noteIds != null? noteIds: new ArrayList<>();
			this.sourceCitations = sourceCitations != null? sourceCitations: new ArrayList<>();
		}

		@Override
		public String toString(){
			return address != null && !address.isEmpty()? address: "[empty address]";
		}
	}

	// ==================== Constructors ====================
	public PlaceDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Place", model, record);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(950, 800));
		pack();
		setLocationRelativeTo(parent);
	}

	public PlaceDialog(Frame parent, FLEFModel model){
		super(parent, "New Place", model, null);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(950, 800));
		pack();
		setLocationRelativeTo(parent);
	}

	// ==================== UI Initialization ====================
	@Override
	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		tabbedPane.addTab("Basic", createBasicPanel());

		// --- Addresses tab ---
		tabbedPane.addTab("Addresses", createAddressesPanel());

		// --- Map tab ---
		tabbedPane.addTab("Map", createMapPanel());

		// --- Subordinate tab ---
		tabbedPane.addTab("Subordinate", createSubordinatePanel());

		// --- Modification tab ---
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}

	// ==================== Panel factories ====================

	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// NAME (0:1) with transcriptions
		JPanel nameRow = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow][][]", ""));
		nameRow.add(new JLabel("Name:"), "align label");
		nameRow.add(nameField, "growx");
		nameTransBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
		nameTransBtn.setToolTipText("Show/hide transcriptions");
		nameTransBtn.setFocusable(false);
		nameTransBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		nameRow.add(nameTransBtn, "wrap");
		panel.add(nameRow, "span 2,growx,wrap");

		// Transcriptions panel for NAME
		nameTransPanel = createTranscriptionPanel(nameTransBtn, "NAME", panel);
		panel.add(nameTransPanel, "span 2,growx,wrap");

		return panel;
	}

	private JPanel createTranscriptionPanel(JButton toggleBtn, String parentTag, JPanel parentPanel){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder("Transcriptions"));
		panel.setVisible(false);

		DefaultListModel<String> model = new DefaultListModel<>();
		JList<String> list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(3);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 70));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		// Store references in a map
		Map<String, Object> data = new HashMap<>();
		data.put("model", model);
		data.put("list", list);
		data.put("records", new ArrayList<FLEFRecord>());

		addBtn.addActionListener(e -> addTranscription(data, parentTag));
		editBtn.addActionListener(e -> editTranscription(data));
		deleteBtn.addActionListener(e -> deleteTranscription(data, parentTag));

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		toggleBtn.addActionListener(e -> {
			boolean visible = panel.isVisible();
			panel.setVisible(!visible);
			toggleBtn.setText(visible? "📝": "📝⬆");
			SwingUtilities.invokeLater(() -> {
				parentPanel.revalidate();
				parentPanel.repaint();
				Window win = SwingUtilities.getWindowAncestor(parentPanel);
				if(win != null){
					win.pack();
				}
			});
		});

		parentPanel.add(panel, "span 2,growx,wrap");
		return panel;
	}

	@SuppressWarnings("unchecked")
	private void addTranscription(Map<String, Object> data, String parentTag){
		TranscribedTextDialog dialog = new TranscribedTextDialog(this, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord transRecord = dialog.getTranscribedTextRecord();
			if(transRecord != null){
				((List<FLEFRecord>)data.get("records")).add(transRecord);
				((DefaultListModel<String>)data.get("model")).addElement(buildTranscriptionDisplay(transRecord));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void editTranscription(Map<String, Object> data){
		int idx = ((JList<String>)data.get("list")).getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord transRecord = ((List<FLEFRecord>)data.get("records")).get(idx);
		TranscribedTextDialog dialog = new TranscribedTextDialog(this, transRecord);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			((DefaultListModel<String>)data.get("model")).set(idx, buildTranscriptionDisplay(transRecord));
		}
	}

	@SuppressWarnings("unchecked")
	private void deleteTranscription(Map<String, Object> data, String parentTag){
		int idx = ((JList<String>)data.get("list")).getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this transcription?"))
			return;
		((List<FLEFRecord>)data.get("records")).remove(idx);
		((DefaultListModel<String>)data.get("model")).remove(idx);
	}

	private String buildTranscriptionDisplay(FLEFRecord transRecord){
		String phonetic = FLEFRecordUtils.getChildValue(transRecord, "PHONETIC");
		String transcription = FLEFRecordUtils.getChildValue(transRecord, "TRANSCRIPTION");
		StringBuilder sb = new StringBuilder();
		if(phonetic != null) sb.append("phonetic: ").append(phonetic);
		if(transcription != null){
			if(!sb.isEmpty())
				sb.append(" | ");
			sb.append("transcription: ")
				.append(transcription);
		}
		if(sb.isEmpty())
			sb.append("[empty]");
		return sb.toString();
	}

	// ==================== Addresses Panel ====================

	private JPanel createAddressesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Address"));

		addressList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		addressList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAddress();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(addressList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		addressList.addListSelectionListener(e -> {
			boolean selected = addressList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addAddress());
		editBtn.addActionListener(e -> editAddress());
		deleteBtn.addActionListener(e -> deleteAddress());

		return panel;
	}

	private void addAddress(){
		AddressEntry entry = showAddressDialog(null);
		if(entry != null){
			addressEntries.add(entry);
			addressListModel.addElement(entry);
		}
	}

	private void editAddress(){
		int idx = addressList.getSelectedIndex();
		if(idx == -1)
			return;
		AddressEntry existing = addressEntries.get(idx);
		AddressEntry updated = showAddressDialog(existing);
		if(updated != null){
			addressEntries.set(idx, updated);
			addressListModel.set(idx, updated);
		}
	}

	private void deleteAddress(){
		int idx = addressList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this address?"))
			return;
		addressEntries.remove(idx);
		addressListModel.remove(idx);
	}

	private AddressEntry showAddressDialog(AddressEntry existing){
		JDialog dialog = new JDialog(this, existing == null? "Add Address": "Edit Address", true);
		dialog.setLayout(new BorderLayout(10, 10));

		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JTextField addressField = new JTextField(30);
		JTextField hierarchyField = new JTextField(30);

		if(existing != null){
			addressField.setText(existing.address);
			hierarchyField.setText(existing.hierarchy);
		}

		panel.add(new JLabel("Address:"), "align label");
		panel.add(addressField, "growx,wrap");
		panel.add(new JLabel("Hierarchy:"), "align label");
		panel.add(hierarchyField, "growx,wrap");

		// Transcriptions for ADDRESS (simplified: we'll use a list)
		// For a real implementation, we'd need a more sophisticated sub-dialog.
		// For now, we keep it simple.

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		panel.add(btnPanel, "span 2,growx");

		dialog.add(panel, BorderLayout.CENTER);

		final AddressEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String address = addressField.getText().trim();
			if(address.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Address is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			result[0] = new AddressEntry(address, hierarchyField.getText().trim(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setMinimumSize(new Dimension(450, 200));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		return result[0];
	}

	// ==================== Map Panel ====================

	private JPanel createMapPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		panel.add(new JLabel("Latitude:"), "align label");
		panel.add(latitudeField, "growx,wrap");

		panel.add(new JLabel("Longitude:"), "align label");
		panel.add(longitudeField, "growx,wrap");

		// MAP -> CERTAINTY + CREDIBILITY (grouped in EvidenceQualifiersPanel)
		panel.add(mapQualifiers, "span 2,growx,wrap");

		return panel;
	}

	// ==================== Subordinate Panel ====================

	private JPanel createSubordinatePanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		subordinateDisplayField.setEditable(false);
		subordinateDisplayField.setBackground(UIManager.getColor("TextField.background"));
		JPanel subPanel = new JPanel(new BorderLayout(5, 5));
		subPanel.add(subordinateDisplayField, BorderLayout.CENTER);
		JPanel subBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		subBtnPanel.add(subordinateBrowseBtn);
		subBtnPanel.add(subordinateClearBtn);
		subPanel.add(subBtnPanel, BorderLayout.EAST);
		panel.add(new JLabel("Subordinate Place:"), "align label");
		panel.add(subPanel, "growx");

		subordinateBrowseBtn.addActionListener(e -> browseSubordinate());
		subordinateClearBtn.addActionListener(e -> {
			selectedSubordinateId = null;
			subordinateDisplayField.setText("");
		});

		return panel;
	}

	private void browseSubordinate(){
		if(placeHandler == null){
			JOptionPane.showMessageDialog(this, "Place handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, placeHandler, selectedId -> {
			if(selectedId != null){
				// Prevent self-reference
				if(selectedId.equals(record.getId())){
					JOptionPane.showMessageDialog(this, "Cannot reference itself.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				selectedSubordinateId = selectedId;
				FLEFRecord rec = model.getRecordById(selectedId);
				if(rec != null){
					subordinateDisplayField.setText(placeHandler.getDisplayName(rec));
				}
				else{
					subordinateDisplayField.setText(selectedId);
				}
			}
		}
		);
		dialog.setVisible(true);
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// NAME (0:1)
		String name = FLEFRecordUtils.getChildValue(record, "NAME");
		nameField.setText(name != null? name: "");
		loadTranscriptionsForTag(record, "NAME", nameTransModel, nameTransRecords);

		// ADDRESS (0:M)
		addressEntries.clear();
		addressListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("ADDRESS".equals(child.getTag())){
				String address = child.getValue();
				String hierarchy = FLEFRecordUtils.getChildValue(child, "HIERARCHY");
				// Transcriptions for address are stored directly under the ADDRESS node
				List<FLEFRecord> trans = new ArrayList<>();
				List<String> culturalNormIds = new ArrayList<>();
				List<String> noteIds = new ArrayList<>();
				List<FLEFRecord> sourceCitations = new ArrayList<>();

				for(FLEFRecord sub : child.getChildren()){
					if("TRANSCRIBED_TEXT".equals(sub.getTag())){
						trans.add(sub);
					}
					else if("CULTURAL_NORM".equals(sub.getTag()) && sub.getValue() != null){
						culturalNormIds.add(sub.getValue());
					}
					else if("NOTE".equals(sub.getTag()) && sub.getValue() != null){
						noteIds.add(sub.getValue());
					}
					else if("SOURCE_CITATION".equals(sub.getTag())){
						sourceCitations.add(sub);
					}
				}
				AddressEntry entry = new AddressEntry(address, hierarchy, trans, culturalNormIds, noteIds, sourceCitations);
				addressEntries.add(entry);
				addressListModel.addElement(entry);
			}
		}

		// MAP (0:1)
		FLEFRecord map = FLEFRecordUtils.findChild(record, "MAP");
		if(map != null){
			latitudeField.setText(FLEFRecordUtils.getChildValue(map, "LATITUDE"));
			longitudeField.setText(FLEFRecordUtils.getChildValue(map, "LONGITUDE"));
			String mapCert = FLEFRecordUtils.getChildValue(map, "CERTAINTY");
			String mapCred = FLEFRecordUtils.getChildValue(map, "CREDIBILITY");
			mapQualifiers.load(mapCert, mapCred);
		}

		// SUBORDINATE (0:1)
		String subId = FLEFRecordUtils.getChildValue(record, "SUBORDINATE");
		if(subId != null && !subId.isEmpty()){
			selectedSubordinateId = subId;
			FLEFRecord rec = model.getRecordById(subId);
			if(rec != null && placeHandler != null){
				subordinateDisplayField.setText(placeHandler.getDisplayName(rec));
			}
			else{
				subordinateDisplayField.setText(subId);
			}
		}

		// MODIFICATION (1:1)
		modificationPanel.loadFromRecord(record);
	}

	private void loadTranscriptionsForTag(FLEFRecord parent, String tag,
		DefaultListModel<String> model,
		List<FLEFRecord> records){
		model.clear();
		records.clear();
		FLEFRecord node = FLEFRecordUtils.findChild(parent, tag);
		if(node != null){
			for(FLEFRecord child : node.getChildren()){
				if("TRANSCRIBED_TEXT".equals(child.getTag())){
					records.add(child);
					model.addElement(buildTranscriptionDisplay(child));
				}
			}
		}
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// MODIFICATION_STRUCTURE (1:1) - required
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(!modificationPanel.validateRequiredFields()){
			return false;
		}

		// Check that if MAP is present, LATITUDE and LONGITUDE are filled
		boolean hasLat = !latitudeField.getText().trim().isEmpty();
		boolean hasLon = !longitudeField.getText().trim().isEmpty();
		if(hasLat ^ hasLon){
			JOptionPane.showMessageDialog(this,
				"Both LATITUDE and LONGITUDE are required for MAP.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		// Validation is already done by save() before calling this method
		record.getChildren().clear();

		// NAME (0:1) with transcriptions
		String name = nameField.getText().trim();
		if(!name.isEmpty()){
			FLEFRecord nameNode = new FLEFRecord();
			nameNode.setLevel(1);
			nameNode.setTag("NAME");
			nameNode.setValue(name);
			record.addChild(nameNode);
			for(FLEFRecord trans : nameTransRecords){
				trans.setLevel(2);
				trans.setTag("TRANSCRIBED_TEXT");
				nameNode.addChild(trans);
			}
		}

		// ADDRESS (0:M)
		for(AddressEntry entry : addressEntries){
			FLEFRecord addrNode = new FLEFRecord();
			addrNode.setLevel(1);
			addrNode.setTag("ADDRESS");
			addrNode.setValue(entry.address);
			record.addChild(addrNode);

			if(entry.hierarchy != null && !entry.hierarchy.isEmpty()){
				FLEFRecord hier = new FLEFRecord();
				hier.setLevel(2);
				hier.setTag("HIERARCHY");
				hier.setValue(entry.hierarchy);
				addrNode.addChild(hier);
			}

			for(FLEFRecord trans : entry.transcriptions){
				trans.setLevel(2);
				trans.setTag("TRANSCRIBED_TEXT");
				addrNode.addChild(trans);
			}

			for(String id : entry.culturalNormIds){
				FLEFRecordUtils.addChild(addrNode, "CULTURAL_NORM", 2, id);
			}
			for(String id : entry.noteIds){
				FLEFRecordUtils.addChild(addrNode, "NOTE", 2, id);
			}
			for(FLEFRecord citation : entry.sourceCitations){
				citation.setLevel(2);
				citation.setTag("SOURCE_CITATION");
				addrNode.addChild(citation);
			}
		}

		// MAP (0:1)
		String lat = latitudeField.getText().trim();
		String lon = longitudeField.getText().trim();
		if(!lat.isEmpty() && !lon.isEmpty()){
			FLEFRecord mapNode = new FLEFRecord();
			mapNode.setLevel(1);
			mapNode.setTag("MAP");
			record.addChild(mapNode);

			FLEFRecord latNode = new FLEFRecord();
			latNode.setLevel(2);
			latNode.setTag("LATITUDE");
			latNode.setValue(lat);
			mapNode.addChild(latNode);

			FLEFRecord lonNode = new FLEFRecord();
			lonNode.setLevel(2);
			lonNode.setTag("LONGITUDE");
			lonNode.setValue(lon);
			mapNode.addChild(lonNode);

			String cert = mapQualifiers.getCertainty();
			if(cert != null && !cert.isEmpty()){
				FLEFRecordUtils.updateChildValue(mapNode, "CERTAINTY", cert);
			}
			String cred = mapQualifiers.getCredibility();
			if(cred != null && !cred.isEmpty()){
				FLEFRecordUtils.updateChildValue(mapNode, "CREDIBILITY", cred);
			}
		}

		// SUBORDINATE (0:1)
		if(selectedSubordinateId != null && !selectedSubordinateId.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "SUBORDINATE", selectedSubordinateId);
		}

		// MODIFICATION (1:1)
		modificationPanel.saveToRecord(record);

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		return FLEFRecord.createMainRecord(generateNewId(), "PLACE");
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "PLACE", "P");
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi un place di esempio per subordinate
		FLEFRecord parentPlace = FLEFRecord.createMainRecord("P1", "PLACE");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		name.setValue("Italy");
		parentPlace.addChild(name);
		model.addRecord(parentPlace);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Place Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Place");
			btn.addActionListener(e -> {
				PlaceDialog dialog = new PlaceDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Place saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
