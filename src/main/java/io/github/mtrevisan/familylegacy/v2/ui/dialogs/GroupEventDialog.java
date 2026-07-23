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
import io.github.mtrevisan.familylegacy.v2.ui.components.EventStructurePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a GROUP_EVENT_RECORD according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * GROUP_EVENT_RECORD :=
 *   n @<XREF:EVENT>@ EVENT    {1:1}
 *     +1 TYPE <EVENT_TYPE>    {1:1}
 *     +1 <<EVENT_STRUCTURE>>    {0:1}
 *     +1 GROUP @<XREF:GROUP>@    {0:M}
 * </pre>
 */
public class GroupEventDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -6191086615039410003L;


	static{
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new CalendarHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupEventHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JTextField typeField = new JTextField(20);
	private String selectedEventTypeId;

	// ========== EVENT_STRUCTURE (0:1) ==========
	private final EventStructurePanel eventStructurePanel;

	// ========== GROUP (0:M) ==========
	private final DefaultListModel<String> groupListModel = new DefaultListModel<>();
	private final JList<String> groupList = new JList<>(groupListModel);
	private final List<String> groupIds = new ArrayList<>();

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");

	// ==================== Constructors ====================
	public GroupEventDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Group Event", model, record);

		this.eventStructurePanel = new EventStructurePanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	public GroupEventDialog(Frame parent, FLEFModel model){
		super(parent, "New Group Event", model, null);

		this.eventStructurePanel = new EventStructurePanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(900, 750));
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

		// --- Event Structure tab ---
		tabbedPane.addTab("Event Structure", eventStructurePanel);

		// --- Groups tab ---
		tabbedPane.addTab("Groups", createGroupsPanel());

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

		// TYPE (1:1) - marked with an asterisk
		panel.add(new JLabel("Type*:"), "align label");
		typeField.setEditable(false);
		typeField.setBackground(UIManager.getColor("TextField.background"));
		JPanel typePanel = new JPanel(new BorderLayout(5, 5));
		typePanel.add(typeField, BorderLayout.CENTER);
		panel.add(typePanel, "growx");

		return panel;
	}

	private JPanel createGroupsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Group"));

		groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editGroup();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(groupList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New Group");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		groupList.addListSelectionListener(e -> {
			boolean selected = groupList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addGroup());
		newBtn.addActionListener(e -> createNewGroup());
		editBtn.addActionListener(e -> editGroup());
		deleteBtn.addActionListener(e -> deleteGroup());

		return panel;
	}

	// ==================== Group methods ====================

	private String getGroupDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return groupHandler.getDisplayName(rec);
		}
		return id;
	}

	private void loadGroups(){
		groupListModel.clear();
		groupIds.clear();
		for(FLEFRecord child : record.getChildren()){
			if("GROUP".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				groupIds.add(id);
				groupListModel.addElement(getGroupDisplayName(id));
			}
		}
	}

	private void addGroup(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, groupHandler, selectedId -> {
			if(selectedId != null && !groupIds.contains(selectedId)){
				groupIds.add(selectedId);
				groupListModel.addElement(getGroupDisplayName(selectedId));
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editGroup(){
		int idx = groupList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = groupIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Group not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = groupHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		groupListModel.set(idx, getGroupDisplayName(id));
	}

	private void deleteGroup(){
		int idx = groupList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this group reference?"))
			return;
		groupIds.remove(idx);
		groupListModel.remove(idx);
	}

	private void createNewGroup(){
		JDialog dialog = groupHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// TYPE (1:1)
		String typeId = FLEFRecordUtils.getChildValue(record, "TYPE");
		if(typeId != null && !typeId.isEmpty()){
			selectedEventTypeId = typeId;
			FLEFRecord rec = model.getRecordById(typeId);
			if(rec != null){
				typeField.setText(eventHandler.getDisplayName(rec));
			}
			else{
				typeField.setText(typeId);
			}
		}

		// EVENT_STRUCTURE (0:1)
		FLEFRecord eventStruct = FLEFRecordUtils.findChild(record, "EVENT_STRUCTURE");
		eventStructurePanel.loadFromRecord(eventStruct);

		// GROUP (0:M)
		loadGroups();
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// TYPE (1:1) - required
		if(selectedEventTypeId == null || selectedEventTypeId.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"TYPE is required.\nPlease select an event type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// EVENT_STRUCTURE (0:1) - validate if present
		return (!eventStructurePanel.hasData() || eventStructurePanel.validateRequiredFields());
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// TYPE (1:1)
		FLEFRecordUtils.updateChildValue(record, "TYPE", selectedEventTypeId);

		// EVENT_STRUCTURE (0:1)
		if(eventStructurePanel.hasData()){
			FLEFRecord eventStruct = eventStructurePanel.saveToRecord(null);
			if(eventStruct != null){
				eventStruct.setLevel(1);
				eventStruct.setTag("EVENT_STRUCTURE");
				record.addChild(eventStruct);
			}
		}

		// GROUP (0:M)
		for(String id : groupIds){
			FLEFRecordUtils.addChild(record, "GROUP", id);
		}

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}

	// ==================== Overrides ====================

	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = FLEFRecord.createMainRecord(generateNewId(), "EVENT");
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "EVENT", "E");
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi un evento di esempio per il TYPE
		FLEFRecord eventType = FLEFRecord.createMainRecord("E1", "EVENT");
		FLEFRecord type = new FLEFRecord();
		type.setLevel(1);
		type.setTag("TYPE");
		type.setValue("BIRTH");
		eventType.addChild(type);
		model.addRecord(eventType);

		// Aggiungi un gruppo di esempio
		FLEFRecord group = FLEFRecord.createMainRecord("G1", "GROUP");
		FLEFRecord name = new FLEFRecord();
		name.setLevel(1);
		name.setTag("NAME");
		name.setValue("Sample Group");
		group.addChild(name);
		model.addRecord(group);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Group Event Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Group Event");
			btn.addActionListener(e -> {
				GroupEventDialog dialog = new GroupEventDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Group Event saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
