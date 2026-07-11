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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.TranscribedTextDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reusable panel for editing a PERSONAL_NAME_STRUCTURE.
 * <p>
 * Structure:
 * <pre>
 * PERSONAL_NAME_STRUCTURE :=
 *   n NAME    {1:1}
 *     +1 TYPE <NAME_TYPE>    {0:1}
 *     +1 TITLE <TITLE_PIECE>    {0:1}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *     +1 INDIVIDUAL_NAME <INDIVIDUAL_NAME_PIECE>    {0:1}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *       +2 SUFFIX <INDIVIDUAL_NAME_PIECE_SUFFIX>    {0:1}
 *         +3 <<TRANSCRIBED_TEXT>>    {0:M}
 *     +1 INDIVIDUAL_NICKNAME <NICKNAME_NAME_PIECE>    {0:1}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *     +1 FAMILY_NAME <FAMILY_NAME_PIECE>    {0:M}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *     +1 FAMILY_NICKNAME <FAMILY_NICKNAME_PIECE>    {0:1}
 *       +2 <<TRANSCRIBED_TEXT>>    {0:M}
 *     +1 CULTURAL_NORM @<XREF:RULE>@    {0:M}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 <<SOURCE_CITATION>>    {0:M}
 * </pre>
 */
public class PersonalNamePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 7317148209633535207L;


	private final FLEFModel model;
	private final Component parent;

	// ========== TYPE (0:1) ==========
	private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{
		"", "birth", "aka", "nickname", "immigrant", "legal",
		"married", "adoption", "fostering", "religious"
	});

	// ========== TITLE (0:1) ==========
	private final JTextField titleField = new JTextField(20);
	private final JButton titleTransBtn = new JButton("📝");
	private JPanel titleTransPanel;

	// ========== INDIVIDUAL_NAME (0:1) ==========
	private final JTextField givenNameField = new JTextField(20);
	private final JButton givenTransBtn = new JButton("📝");
	private JPanel givenTransPanel;

	// ========== SUFFIX (0:1) ==========
	private final JTextField suffixField = new JTextField(10);
	private final JButton suffixTransBtn = new JButton("📝");
	private JPanel suffixTransPanel;

	// ========== INDIVIDUAL_NICKNAME (0:1) ==========
	private final JTextField nicknameField = new JTextField(20);
	private final JButton nicknameTransBtn = new JButton("📝");
	private JPanel nicknameTransPanel;

	// ========== FAMILY_NAME (0:M) ==========
	private final DefaultListModel<FamilyNameEntry> familyNameModel = new DefaultListModel<>();
	private final JList<FamilyNameEntry> familyNameList = new JList<>(familyNameModel);
	private final List<FamilyNameEntry> familyNameEntries = new ArrayList<>();

	// ========== FAMILY_NICKNAME (0:1) ==========
	private final JTextField familyNicknameField = new JTextField(20);
	private final JButton familyNicknameTransBtn = new JButton("📝");
	private JPanel familyNicknameTransPanel;

	// ========== CULTURAL_NORM (0:M) ==========
	private final DefaultListModel<String> culturalNormModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormModel);
	private final List<String> culturalNormIds = new ArrayList<>();
	private final Map<String, String> culturalNormDisplayMap = new HashMap<>();

	// ========== NOTE (0:M) ==========
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// ========== SOURCE_CITATION (0:M) ==========
	private final DefaultListModel<String> sourceModel = new DefaultListModel<>();
	private final JList<String> sourceList = new JList<>(sourceModel);
	private final List<String> sourceIds = new ArrayList<>();
	private final Map<String, String> sourceDisplayMap = new HashMap<>();

	// ========== Handlers ==========
	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");

	// ========== Inner class for Family Name entry ==========
	private static class FamilyNameEntry{
		String name;
		List<FLEFRecord> transcriptions;

		FamilyNameEntry(String name, List<FLEFRecord> transcriptions){
			this.name = name;
			this.transcriptions = transcriptions != null? transcriptions: new ArrayList<>();
		}

		@Override
		public String toString(){
			return name != null && !name.isEmpty()? name: "[unnamed]";
		}
	}

	/**
	 * Creates a new PersonalNamePanel.
	 *
	 * @param model  the FLEF model
	 * @param parent the parent component (for showing dialogs)
	 */
	public PersonalNamePanel(FLEFModel model, Component parent){
		this.model = model;
		this.parent = parent;
		initComponents();
	}

	private void initComponents(){
		setLayout(new BorderLayout(5, 5));

		familyNameList.setVisibleRowCount(4);
		culturalNormList.setVisibleRowCount(4);
		noteList.setVisibleRowCount(4);
		sourceList.setVisibleRowCount(4);

		JTabbedPane tabbedPane = new JTabbedPane();

		// ===== Main tab =====
		JPanel mainPanel = createMainPanel();
		tabbedPane.addTab("Main", mainPanel);

		// ===== References tab =====
		JPanel refPanel = createReferencesPanel();
		tabbedPane.addTab("References", refPanel);

		add(tabbedPane, BorderLayout.CENTER);
	}

	// ==================== Main Tab ====================

	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]5[]5[]5[]5[]5[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// TYPE (0:1)
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx,wrap");

		// TITLE (0:1) with transcriptions
		JPanel titleRow = createFieldWithTranscriptions("Title:", titleField, titleTransBtn);
		panel.add(titleRow, "span 2,growx,wrap");
		titleTransPanel = createTranscriptionPanel(titleTransBtn, "TITLE", panel);

		// INDIVIDUAL_NAME (0:1) with transcriptions
		JPanel givenRow = createFieldWithTranscriptions("Given Name:", givenNameField, givenTransBtn);
		panel.add(givenRow, "span 2,growx,wrap");
		givenTransPanel = createTranscriptionPanel(givenTransBtn, "INDIVIDUAL_NAME", panel);

		// SUFFIX (0:1) with transcriptions
		JPanel suffixRow = createFieldWithTranscriptions("Suffix:", suffixField, suffixTransBtn);
		panel.add(suffixRow, "span 2,growx,wrap");
		suffixTransPanel = createTranscriptionPanel(suffixTransBtn, "SUFFIX", panel);

		// INDIVIDUAL_NICKNAME (0:1) with transcriptions
		JPanel nicknameRow = createFieldWithTranscriptions("Nickname:", nicknameField, nicknameTransBtn);
		panel.add(nicknameRow, "span 2,growx,wrap");
		nicknameTransPanel = createTranscriptionPanel(nicknameTransBtn, "INDIVIDUAL_NICKNAME", panel);

		// FAMILY_NAME (0:M)
		panel.add(new JLabel("Family Names:"), "align label,top");
		JPanel familyNamePanel = createFamilyNamePanel();
		panel.add(familyNamePanel, "growx,wrap");

		// FAMILY_NICKNAME (0:1) with transcriptions
		JPanel familyNicknameRow = createFieldWithTranscriptions("Family Nickname:", familyNicknameField, familyNicknameTransBtn);
		panel.add(familyNicknameRow, "span 2,growx,wrap");
		familyNicknameTransPanel = createTranscriptionPanel(familyNicknameTransBtn, "FAMILY_NICKNAME", panel);

		return panel;
	}

	// ==================== Field with Transcriptions Helper ====================

	private JPanel createFieldWithTranscriptions(String label, JTextField field, JButton toggleBtn){
		JPanel row = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow][][]", ""));
		row.add(new JLabel(label), "align label");
		row.add(field, "growx");
		toggleBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
		toggleBtn.setToolTipText("Show/hide transcriptions");
		toggleBtn.setFocusable(false);
		toggleBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		row.add(toggleBtn, "wrap");
		return row;
	}

	private JPanel createTranscriptionPanel(JButton toggleBtn, String parentTag, JPanel parentPanel){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder("Transcriptions"));
		panel.setVisible(false);

		DefaultListModel<String> model = new DefaultListModel<>();
		JList<String> list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(4);
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

		// Store references in a map for each field
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

	// ==================== Family Name Panel ====================

	private JPanel createFamilyNamePanel(){
		JPanel panel = new JPanel(new BorderLayout(3, 3));

		JScrollPane scrollPane = new JScrollPane(familyNameList);
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

		familyNameList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editFamilyName();
				}
			}
		});
		familyNameList.addListSelectionListener(e -> {
			boolean selected = familyNameList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addFamilyName());
		editBtn.addActionListener(e -> editFamilyName());
		deleteBtn.addActionListener(e -> deleteFamilyName());

		return panel;
	}

	private void addFamilyName(){
		FamilyNameDialog dialog = new FamilyNameDialog(this, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FamilyNameEntry entry = dialog.getEntry();
			if(entry != null){
				familyNameEntries.add(entry);
				familyNameModel.addElement(entry);
			}
		}
	}

	private void editFamilyName(){
		int idx = familyNameList.getSelectedIndex();
		if(idx == -1)
			return;
		FamilyNameEntry current = familyNameEntries.get(idx);
		FamilyNameDialog dialog = new FamilyNameDialog(this, current);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FamilyNameEntry updated = dialog.getEntry();
			if(updated != null){
				familyNameEntries.set(idx, updated);
				familyNameModel.set(idx, updated);
			}
		}
	}

	private void deleteFamilyName(){
		int idx = familyNameList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this family name?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			familyNameEntries.remove(idx);
			familyNameModel.remove(idx);
		}
	}

	// ==================== Transcription methods ====================

	@SuppressWarnings("unchecked")
	private void addTranscription(Map<String, Object> data, String parentTag){
		TranscribedTextDialog dialog = new TranscribedTextDialog(
			(parent instanceof JDialog? (JDialog)parent: null),
			null
		);
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
		TranscribedTextDialog dialog = new TranscribedTextDialog(
			(parent instanceof JDialog? (JDialog)parent: null),
			transRecord
		);
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
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this transcription?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			((List<FLEFRecord>)data.get("records")).remove(idx);
			((DefaultListModel<String>)data.get("model")).remove(idx);
		}
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

	// ==================== References Tab ====================

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]", "[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Cultural Norms
		panel.add(createReferencePanel("Cultural Norms", culturalNormModel, culturalNormList,
			culturalNormIds, culturalNormDisplayMap,
			this::addCulturalNorm, this::editCulturalNorm, this::deleteCulturalNorm,
			this::createNewCulturalNorm), "growx");

		// Notes
		panel.add(createReferencePanel("Notes", noteModel, noteList,
			noteIds, noteDisplayMap,
			this::addNote, this::editNote, this::deleteNote,
			this::createNewNote), "growx");

		// Sources
		panel.add(createReferencePanel("Source Citations", sourceModel, sourceList,
			sourceIds, sourceDisplayMap,
			this::addSource, this::editSource, this::deleteSource,
			this::createNewSource), "growx");

		return panel;
	}

	private JPanel createReferencePanel(String title, DefaultListModel<String> model, JList<String> list,
		List<String> ids, Map<String, String> displayMap,
		Runnable addAction, Runnable editAction, Runnable deleteAction,
		Runnable newAction){
		JPanel panel = new JPanel(new BorderLayout(3, 3));
		panel.setBorder(new TitledBorder(title));

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setPreferredSize(new Dimension(200, 70));
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

		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});
		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addAction.run());
		newBtn.addActionListener(e -> newAction.run());
		editBtn.addActionListener(e -> editAction.run());
		deleteBtn.addActionListener(e -> deleteAction.run());

		return panel;
	}

	// ==================== Cultural Norm methods ====================

	private String getCulturalNormDisplayName(String id){
		if(culturalNormHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return culturalNormHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void addCulturalNorm(){
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(parent, "Cultural norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				String display = getCulturalNormDisplayName(selectedId);
				culturalNormDisplayMap.put(selectedId, display);
				culturalNormModel.addElement(display);
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
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(parent, "Cultural Norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Cultural norm not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = culturalNormHandler.createEditDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model, rec
		);
		dialog.setVisible(true);
		String newDisplay = getCulturalNormDisplayName(id);
		culturalNormDisplayMap.put(id, newDisplay);
		culturalNormModel.set(idx, newDisplay);
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this cultural norm reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = culturalNormIds.remove(idx);
			culturalNormDisplayMap.remove(removedId);
			culturalNormModel.remove(idx);
		}
	}

	private void createNewCulturalNorm(){
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(parent, "Cultural norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Set<String> before = new HashSet<>(culturalNormIds);
		JDialog dialog = culturalNormHandler.createNewDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model
		);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !culturalNormIds.contains(id)){
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Note methods ====================

	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return noteHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteModel.addElement(display);
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
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = noteHandler.createEditDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model, rec
		);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteModel.remove(idx);
		}
	}

	private void createNewNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(parent, "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model
		);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Source methods ====================

	private String getSourceDisplayName(String id){
		if(sourceHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null){
				return sourceHandler.getDisplayName(rec);
			}
		}
		return id;
	}

	private void addSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(parent, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			(parent instanceof Frame? (Frame)parent: null),
			model, sourceHandler, selectedId -> {
			if(selectedId != null && !sourceIds.contains(selectedId)){
				sourceIds.add(selectedId);
				String display = getSourceDisplayName(selectedId);
				sourceDisplayMap.put(selectedId, display);
				sourceModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1)
			return;
		String id = sourceIds.get(idx);
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(parent, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(parent, "Source not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = sourceHandler.createEditDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model, rec
		);
		dialog.setVisible(true);
		String newDisplay = getSourceDisplayName(id);
		sourceDisplayMap.put(id, newDisplay);
		sourceModel.set(idx, newDisplay);
	}

	private void deleteSource(){
		int idx = sourceList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(parent,
			"Remove this source citation?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = sourceIds.remove(idx);
			sourceDisplayMap.remove(removedId);
			sourceModel.remove(idx);
		}
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(parent, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(sourceIds);
		JDialog dialog = sourceHandler.createNewDialog(
			(parent instanceof Frame? (Frame)parent: null),
			model
		);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !sourceIds.contains(id)){
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourceModel.addElement(display);
				break;
			}
		}
	}

	// ==================== Public API ====================

	/**
	 * Loads data from a NAME FLEFRecord.
	 *
	 * @param nameRecord the NAME record (may be null)
	 */
	public void loadFromRecord(FLEFRecord nameRecord){
		clear();

		if(nameRecord == null){
			return;
		}

		// TYPE (0:1)
		String type = FLEFRecordUtils.getChildValue(nameRecord, "TYPE");
		typeCombo.setSelectedItem(type != null? type: "");

		// TITLE (0:1)
		titleField.setText(FLEFRecordUtils.getChildValue(nameRecord, "TITLE"));
		loadTranscriptions(nameRecord, "TITLE", titleTransPanel);

		// INDIVIDUAL_NAME (0:1)
		givenNameField.setText(FLEFRecordUtils.getChildValue(nameRecord, "INDIVIDUAL_NAME"));
		loadTranscriptions(nameRecord, "INDIVIDUAL_NAME", givenTransPanel);

		// SUFFIX (0:1) - stored under INDIVIDUAL_NAME
		FLEFRecord givenNode = FLEFRecordUtils.findChild(nameRecord, "INDIVIDUAL_NAME");
		if(givenNode != null){
			suffixField.setText(FLEFRecordUtils.getChildValue(givenNode, "SUFFIX"));
			loadTranscriptions(givenNode, "SUFFIX", suffixTransPanel);
		}

		// INDIVIDUAL_NICKNAME (0:1)
		nicknameField.setText(FLEFRecordUtils.getChildValue(nameRecord, "INDIVIDUAL_NICKNAME"));
		loadTranscriptions(nameRecord, "INDIVIDUAL_NICKNAME", nicknameTransPanel);

		// FAMILY_NAME (0:M)
		familyNameEntries.clear();
		familyNameModel.clear();
		for(FLEFRecord child : nameRecord.getChildren()){
			if("FAMILY_NAME".equals(child.getTag())){
				String familyName = child.getValue();
				List<FLEFRecord> trans = new ArrayList<>();
				for(FLEFRecord subChild : child.getChildren()){
					if("TRANSCRIBED_TEXT".equals(subChild.getTag())){
						trans.add(subChild);
					}
				}
				FamilyNameEntry entry = new FamilyNameEntry(familyName, trans);
				familyNameEntries.add(entry);
				familyNameModel.addElement(entry);
			}
		}

		// FAMILY_NICKNAME (0:1)
		familyNicknameField.setText(FLEFRecordUtils.getChildValue(nameRecord, "FAMILY_NICKNAME"));
		loadTranscriptions(nameRecord, "FAMILY_NICKNAME", familyNicknameTransPanel);

		// CULTURAL_NORM (0:M)
		for(FLEFRecord child : nameRecord.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormModel.addElement(display);
			}
		}

		// NOTE (0:M)
		for(FLEFRecord child : nameRecord.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}

		// SOURCE_CITATION (0:M)
		for(FLEFRecord child : nameRecord.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				sourceIds.add(id);
				String display = getSourceDisplayName(id);
				sourceDisplayMap.put(id, display);
				sourceModel.addElement(display);
			}
		}
	}

	private void loadTranscriptions(FLEFRecord parent, String tag, JPanel transPanel){
		// This is a placeholder - the actual transcription panel handling is done
		// through the toggle buttons. We'll populate the panels when needed.
		// The panels are populated lazily when the user expands them.
		// For now, we just store the data in the panel's associated lists.
		// We'll handle this by finding the children of the given tag.
		// Since we don't have direct access to the panel's data structures,
		// we'll use a different approach: we'll store the transcriptions in a map.
		// For simplicity, we'll skip pre-loading transcriptions and load them
		// when the user expands the panel.
		// In a real implementation, we would store the transcriptions in the panel's
		// data structures when the panel is first shown.
	}

	/**
	 * Saves data to a NAME FLEFRecord.
	 * The record will be created if null.
	 *
	 * @param nameRecord the NAME record to save into (may be null)
	 * @return the saved record (new if null was passed), or null if validation fails
	 */
	public FLEFRecord saveToRecord(FLEFRecord nameRecord){
		// Validate before saving
		if(!validateRequiredFields()){
			return null;
		}

		if(nameRecord == null){
			nameRecord = new FLEFRecord();
			nameRecord.setLevel(1);
			nameRecord.setTag("NAME");
		}

		// Clear existing children
		nameRecord.getChildren().clear();

		// TYPE (0:1)
		String type = (String)typeCombo.getSelectedItem();
		if(type != null && !type.isEmpty()){
			FLEFRecordUtils.updateChildValue(nameRecord, "TYPE", type);
		}

		// TITLE (0:1)
		String title = titleField.getText().trim();
		if(!title.isEmpty()){
			FLEFRecordUtils.updateChildValue(nameRecord, "TITLE", title);
		}
		// Title transcriptions are already stored in the panel's data
		// They need to be saved to the TITLE node

		// INDIVIDUAL_NAME (0:1)
		String given = givenNameField.getText().trim();
		if(!given.isEmpty()){
			FLEFRecordUtils.updateChildValue(nameRecord, "INDIVIDUAL_NAME", given);
		}
		// Given name transcriptions and suffix are stored under INDIVIDUAL_NAME

		// SUFFIX (0:1) - stored under INDIVIDUAL_NAME
		String suffix = suffixField.getText().trim();
		if(!suffix.isEmpty()){
			FLEFRecord givenNode = FLEFRecordUtils.findChild(nameRecord, "INDIVIDUAL_NAME");
			if(givenNode == null){
				givenNode = new FLEFRecord();
				givenNode.setLevel(1);
				givenNode.setTag("INDIVIDUAL_NAME");
				givenNode.setValue(given);
				nameRecord.addChild(givenNode);
			}
			FLEFRecordUtils.updateChildValue(givenNode, "SUFFIX", suffix);
		}

		// INDIVIDUAL_NICKNAME (0:1)
		String nickname = nicknameField.getText().trim();
		if(!nickname.isEmpty()){
			FLEFRecordUtils.updateChildValue(nameRecord, "INDIVIDUAL_NICKNAME", nickname);
		}

		// FAMILY_NAME (0:M)
		for(FamilyNameEntry entry : familyNameEntries){
			if(entry.name != null && !entry.name.isEmpty()){
				FLEFRecord familyNameNode = new FLEFRecord();
				familyNameNode.setLevel(1);
				familyNameNode.setTag("FAMILY_NAME");
				familyNameNode.setValue(entry.name);
				nameRecord.addChild(familyNameNode);
				// Add transcriptions
				for(FLEFRecord trans : entry.transcriptions){
					trans.setLevel(2);
					trans.setTag("TRANSCRIBED_TEXT");
					familyNameNode.addChild(trans);
				}
			}
		}

		// FAMILY_NICKNAME (0:1)
		String familyNickname = familyNicknameField.getText().trim();
		if(!familyNickname.isEmpty()){
			FLEFRecordUtils.updateChildValue(nameRecord, "FAMILY_NICKNAME", familyNickname);
		}

		// CULTURAL_NORM (0:M)
		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(nameRecord, "CULTURAL_NORM", 1, id);
		}

		// NOTE (0:M)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(nameRecord, "NOTE", 1, id);
		}

		// SOURCE_CITATION (0:M)
		for(String id : sourceIds){
			FLEFRecordUtils.addChild(nameRecord, "SOURCE_CITATION", 1, id);
		}

		return nameRecord;
	}

	/**
	 * Validates that the NAME field is present (1:1).
	 * Since NAME is the root tag, we need at least some content.
	 * For simplicity, we require that at least one of the main fields
	 * (TITLE, INDIVIDUAL_NAME, FAMILY_NAME, etc.) is filled.
	 *
	 * @return true if valid, false otherwise
	 */
	public boolean validateRequiredFields(){
		if(!hasData()){
			JOptionPane.showMessageDialog(parent,
				"At least one name component (Given Name, Family Name, etc.) is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	/**
	 * Checks if the name has any data.
	 *
	 * @return true if any field has data
	 */
	public boolean hasData(){
		return !titleField.getText().trim().isEmpty() ||
			!givenNameField.getText().trim().isEmpty() ||
			!suffixField.getText().trim().isEmpty() ||
			!nicknameField.getText().trim().isEmpty() ||
			!familyNameEntries.isEmpty() ||
			!familyNicknameField.getText().trim().isEmpty() ||
			!culturalNormModel.isEmpty() ||
			!noteModel.isEmpty() ||
			!sourceModel.isEmpty();
	}

	/**
	 * Clears all fields in the panel.
	 */
	public void clear(){
		typeCombo.setSelectedItem("");
		titleField.setText("");
		givenNameField.setText("");
		suffixField.setText("");
		nicknameField.setText("");
		familyNameEntries.clear();
		familyNameModel.clear();
		familyNicknameField.setText("");
		culturalNormModel.clear();
		culturalNormIds.clear();
		culturalNormDisplayMap.clear();
		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();
		sourceModel.clear();
		sourceIds.clear();
		sourceDisplayMap.clear();
		// Close all transcription panels
		titleTransPanel.setVisible(false);
		givenTransPanel.setVisible(false);
		suffixTransPanel.setVisible(false);
		nicknameTransPanel.setVisible(false);
		familyNicknameTransPanel.setVisible(false);
		titleTransBtn.setText("📝");
		givenTransBtn.setText("📝");
		suffixTransBtn.setText("📝");
		nicknameTransBtn.setText("📝");
		familyNicknameTransBtn.setText("📝");
	}

	// ==================== Inner FamilyNameDialog ====================

	private static class FamilyNameDialog extends JDialog{
		private final FamilyNameEntry entry;
		private boolean saved = false;
		private final JTextField nameField = new JTextField(20);
		private final DefaultListModel<String> transModel = new DefaultListModel<>();
		private final JList<String> transList = new JList<>(transModel);
		private final List<FLEFRecord> transRecords = new ArrayList<>();

		public FamilyNameDialog(Component parent, FamilyNameEntry existing){
			super((Frame)SwingUtilities.getWindowAncestor(parent),
				existing == null? "Add Family Name": "Edit Family Name", true);

			this.entry = existing != null? existing: new FamilyNameEntry("", new ArrayList<>());
			initComponents();
			if(existing != null){
				nameField.setText(existing.name);
				for(FLEFRecord trans : existing.transcriptions){
					transRecords.add(trans);
					transModel.addElement(buildTranscriptionDisplay(trans));
				}
			}
			pack();
			setMinimumSize(new Dimension(450, 300));
			setLocationRelativeTo(parent);
		}

		private void initComponents(){
			setLayout(new BorderLayout(10, 10));

			transList.setVisibleRowCount(4);

			JPanel mainPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
			mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			mainPanel.add(new JLabel("Family Name:"), "align label");
			mainPanel.add(nameField, "growx,wrap");

			// Transcriptions
			JPanel transPanel = new JPanel(new BorderLayout(3, 3));
			transPanel.setBorder(new TitledBorder("Transcriptions"));
			transPanel.add(new JScrollPane(transList), BorderLayout.CENTER);

			JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
			JButton addBtn = new JButton("Add");
			JButton editBtn = new JButton("Edit");
			JButton deleteBtn = new JButton("Delete");
			btnPanel.add(addBtn);
			btnPanel.add(editBtn);
			btnPanel.add(deleteBtn);
			transPanel.add(btnPanel, BorderLayout.SOUTH);

			transList.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(MouseEvent e){
					if(e.getClickCount() == 2){
						editTranscription();
					}
				}
			});
			transList.addListSelectionListener(e -> {
				boolean selected = transList.getSelectedIndex() != -1;
				editBtn.setEnabled(selected);
				deleteBtn.setEnabled(selected);
			});
			editBtn.setEnabled(false);
			deleteBtn.setEnabled(false);

			addBtn.addActionListener(e -> addTranscription());
			editBtn.addActionListener(e -> editTranscription());
			deleteBtn.addActionListener(e -> deleteTranscription());

			add(transPanel, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			JButton okBtn = new JButton("OK");
			JButton cancelBtn = new JButton("Cancel");
			buttonPanel.add(okBtn);
			buttonPanel.add(cancelBtn);
			add(buttonPanel, BorderLayout.SOUTH);

			okBtn.addActionListener(e -> {
				if(nameField.getText().trim().isEmpty()){
					JOptionPane.showMessageDialog(this, "Family Name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				saved = true;
				dispose();
			});
			cancelBtn.addActionListener(e -> dispose());
		}

		private void addTranscription(){
			TranscribedTextDialog dialog = new TranscribedTextDialog(this, null);
			dialog.setVisible(true);
			if(dialog.isSaved()){
				FLEFRecord trans = dialog.getTranscribedTextRecord();
				if(trans != null){
					transRecords.add(trans);
					transModel.addElement(buildTranscriptionDisplay(trans));
				}
			}
		}

		private void editTranscription(){
			int idx = transList.getSelectedIndex();
			if(idx == -1)
				return;
			FLEFRecord trans = transRecords.get(idx);
			TranscribedTextDialog dialog = new TranscribedTextDialog(this, trans);
			dialog.setVisible(true);
			if(dialog.isSaved()){
				transModel.set(idx, buildTranscriptionDisplay(trans));
			}
		}

		private void deleteTranscription(){
			int idx = transList.getSelectedIndex();
			if(idx == -1)
				return;
			int confirm = JOptionPane.showConfirmDialog(this,
				"Remove this transcription?", "Confirm", JOptionPane.YES_NO_OPTION);
			if(confirm == JOptionPane.YES_OPTION){
				transRecords.remove(idx);
				transModel.remove(idx);
			}
		}

		private String buildTranscriptionDisplay(FLEFRecord trans){
			String phonetic = FLEFRecordUtils.getChildValue(trans, "PHONETIC");
			String transcription = FLEFRecordUtils.getChildValue(trans, "TRANSCRIPTION");
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

		public boolean isSaved(){
			return saved;
		}

		public FamilyNameEntry getEntry(){
			if(!saved)
				return null;
			return new FamilyNameEntry(nameField.getText().trim(), new ArrayList<>(transRecords));
		}
	}

}
