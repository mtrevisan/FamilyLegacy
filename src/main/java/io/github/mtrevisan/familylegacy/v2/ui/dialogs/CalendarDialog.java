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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dialog for editing a {@code CALENDAR_RECORD} according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * CALENDAR_RECORD :=
 *   n @<XREF:CALENDAR>@ CALENDAR    {1:1}
 *     +1 TYPE <CALENDAR_TYPE>    {1:1}
 *     +1 CULTURAL_NORM @<XREF:RULE>@    {0:M}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 <<SOURCE_CITATION>>    {0:M}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class CalendarDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 7421369052579207567L;


	static{
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CalendarHandler());
	}

	private final JTextField idField = new JTextField(10);
	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		StringUtils.EMPTY, "gregorian", "julian", "islamic", "hebrew", "chinese",
		"indian", "buddhist", "french-republican", "coptic",
		"soviet eternal", "ethiopian", "mayan"
	});

	private final DefaultListModel<String> culturalNormListModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormListModel);
	private final List<String> culturalNormIds = new ArrayList<>();
	private final Map<String, String> culturalNormDisplayMap = new HashMap<>();

	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	private final ModificationPanel modificationPanel;

	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");


	public CalendarDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(CalendarHandler.TYPE));

		this.modificationPanel = new ModificationPanel(this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 650));
		pack();
		setLocationRelativeTo(parent);
	}

	public CalendarDialog(Dialog parent, FLEFModel model){
		super(parent, model, null, HandlerRegistry.getHandler(CalendarHandler.TYPE));

		this.modificationPanel = new ModificationPanel(this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 650));
		pack();
		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		setLayout(new BorderLayout(10, 10));

		JTabbedPane tabbedPane = new JTabbedPane();

		// --- Basic tab ---
		tabbedPane.addTab("Basic", createBasicPanel());

		// --- Cultural Norms tab ---
		tabbedPane.addTab("Cultural Norms", createCulturalNormsPanel());

		// --- Notes tab ---
		tabbedPane.addTab("Notes", createNotesPanel());

		// --- Source Citations tab ---
		tabbedPane.addTab("Source Citations", createSourceCitationsPanel());

		// --- Modification tab ---
		tabbedPane.addTab("Modification", modificationPanel);

		add(tabbedPane, BorderLayout.CENTER);

		// --- Button panel ---
		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	private JPanel createBasicPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]10[]"));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// TYPE
		panel.add(new JLabel("Type*:"), "align label");
		panel.add(typeCombo, "growx");

		return panel;
	}

	private JPanel createCulturalNormsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Cultural Norm"));

		culturalNormList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		culturalNormList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editCulturalNorm();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(culturalNormList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		culturalNormList.addListSelectionListener(e -> {
			boolean selected = culturalNormList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addCulturalNorm());
		newBtn.addActionListener(e -> createNewCulturalNorm());
		editBtn.addActionListener(e -> editCulturalNorm());
		deleteBtn.addActionListener(e -> deleteCulturalNorm());

		return panel;
	}

	private JPanel createNotesPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Note"));

		noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNote();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(noteList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		noteList.addListSelectionListener(e -> {
			boolean selected = noteList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addNote());
		newBtn.addActionListener(e -> createNewNote());
		editBtn.addActionListener(e -> editNote());
		deleteBtn.addActionListener(e -> deleteNote());

		return panel;
	}

	private JPanel createSourceCitationsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Source Citation"));

		sourceCitationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sourceCitationList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editSourceCitation();
				}
			}
		});
		JScrollPane scrollPane = GUIHelper.createScrollPane(sourceCitationList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New Source");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		sourceCitationList.addListSelectionListener(e -> {
			boolean selected = sourceCitationList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addSourceCitation());
		newBtn.addActionListener(e -> createNewSource());
		editBtn.addActionListener(e -> editSourceCitation());
		deleteBtn.addActionListener(e -> deleteSourceCitation());

		return panel;
	}


	private String getCulturalNormDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return culturalNormHandler.getDisplayText(rec, model);
		}
		return id;
	}

	private void loadCulturalNorms(){
		culturalNormListModel.clear();
		culturalNormIds.clear();
		culturalNormDisplayMap.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormListModel.addElement(display);
			}
		}
	}

	private void addCulturalNorm(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				String display = getCulturalNormDisplayName(selectedId);
				culturalNormDisplayMap.put(selectedId, display);
				culturalNormListModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = culturalNormIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Cultural norm not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = culturalNormHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);

		String newDisplay = getCulturalNormDisplayName(id);
		culturalNormDisplayMap.put(id, newDisplay);
		culturalNormListModel.set(idx, newDisplay);
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this cultural norm reference?"))
			return;
		String removedId = culturalNormIds.remove(idx);
		culturalNormDisplayMap.remove(removedId);
		culturalNormListModel.remove(idx);
	}

	private void createNewCulturalNorm(){
		Set<String> before = new HashSet<>(culturalNormIds);
		JDialog dialog = culturalNormHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !culturalNormIds.contains(id)){
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormListModel.addElement(display);
				break;
			}
		}
	}


	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null){
			return noteHandler.getDisplayText(rec, model);
		}
		return id;
	}

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

	private void addNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteListModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(this, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(this, model, rec);
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
		String removedId = noteIds.remove(idx);
		noteDisplayMap.remove(removedId);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(this, model);
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


	private void addSourceCitation(){
		SourceCitationDialog dialog = SourceCitationDialog.createEdit(this, model, null);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getRecord();
			if(citation != null){
				citation.setTag("SOURCE");
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
			}
		}
	}

	private void editSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = sourceCitationRecords.get(idx);
		SourceCitationDialog dialog = SourceCitationDialog.createEdit(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getRecord();
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
			if(rec != null){
				return sourceHandler.getDisplayText(rec, model);
			}
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		JDialog dialog = sourceHandler.createNewDialog(this, model);
		dialog.setVisible(true);
	}


	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// TYPE
		String type = FLEFRecordHelper.getChildValue(record, "TYPE");
		typeCombo.setSelectedItem(StringUtils.defaultString(type));

		// CULTURAL_NORM
		loadCulturalNorms();

		// NOTE
		loadNotes();

		// SOURCE_CITATION (0:M)
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// MODIFICATION
		modificationPanel.load(record);
	}


	@Override
	protected boolean validData(){
		// TYPE
		String type = (String)typeCombo.getSelectedItem();
		if(type == null || type.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"TYPE is required.\nPlease select a calendar type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			typeCombo.requestFocusInWindow();
			return false;
		}

		return true;
	}


	@Override
	protected void saveData(){
		// TYPE
		String type = (String)typeCombo.getSelectedItem();
		FLEFRecordHelper.updateChildValue(record, "TYPE", type);

		// CULTURAL_NORM
		for(String id : culturalNormIds){
			FLEFRecordHelper.addChild(record, "CULTURAL_NORM", id);
		}

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordHelper.addChild(record, "NOTE", id);
		}

		// SOURCE_CITATION (0:M)
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setTag("SOURCE");
			record.addChild(citation);
		}

		// MODIFICATION
		modificationPanel.save(record);
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi una cultural norm di esempio
		FLEFRecord norm = FLEFRecord.createMainRecord("CN1", "CULTURAL_NORM");
		FLEFRecord title = FLEFRecord.createChildWithValue("TITLE", "Napoleonic Code");
		norm.addChild(title);
		model.addRecord(norm);

		// Aggiungi una nota di esempio
		FLEFRecord note = FLEFRecord.createMainRecord("N1", "NOTE");
		FLEFRecord value = FLEFRecord.createChildWithValue("VALUE", "This is a sample note about calendars.");
		note.addChild(value);
		model.addRecord(note);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Calendar Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Calendar");
			btn.addActionListener(e -> {
				CalendarDialog dialog = new CalendarDialog(null, model);
				dialog.setVisible(true);

				System.out.println("Calendar saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
