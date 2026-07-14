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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a NOTE_RECORD according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * NOTE_RECORD :=
 *   n @<XREF:NOTE>@ NOTE    {1:1}
 *     +1 VALUE <SUBMITTER_TEXT>    {1:1}
 *     +1 MIME <MIME_TYPE>    {0:1}
 *     +1 LOCALE <LOCALE_CODE>    {0:1}
 *     +1 TRANSLATION <SUBMITTER_TRANSLATED_TEXT>    {0:M}
 *       +2 LOCALE <LOCALE_CODE>    {0:1}
 *       +2 <<MODIFICATION_STRUCTURE>>    {1:1}
 *     +1 <<SOURCE_CITATION>>    {0:M}
 *     +1 RESTRICTION <confidential>    {0:1}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class NoteDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 57751279346094768L;


	static{
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new CalendarHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
	}

	// ========== Basic fields ==========
	private final JTextField idField = new JTextField(10);
	private final JTextArea valueArea = new JTextArea(5, 30);
	private final JTextField mimeField = new JTextField(15);
	private final JTextField localeField = new JTextField(10);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// ========== TRANSLATION (0:M) ==========
	private final DefaultListModel<String> translationListModel = new DefaultListModel<>();
	private final JList<String> translationList = new JList<>(translationListModel);
	private final List<FLEFRecord> translationRecords = new ArrayList<>();

	// ========== SOURCE_CITATION (0:M) ==========
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// ========== MODIFICATION (1:1) ==========
	private final ModificationPanel modificationPanel;

	// ========== Buttons ==========
	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	// ========== Handlers ==========
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");

	// ==================== Constructors ====================
	public NoteDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, "Edit Note", model, record);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 750));
		pack();
		setLocationRelativeTo(parent);
	}

	public NoteDialog(Frame parent, FLEFModel model){
		super(parent, "New Note", model, null);

		this.modificationPanel = new ModificationPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(850, 750));
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

		// --- Translations tab ---
		tabbedPane.addTab("Translations", createTranslationsPanel());

		// --- Source Citations tab ---
		tabbedPane.addTab("Source Citations", createSourceCitationsPanel());

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
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]", "[]10[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ID (read-only)
		idField.setEditable(false);
		idField.setText(record.getId());
		panel.add(new JLabel("ID:"), "align label");
		panel.add(idField, "growx,wrap");

		// VALUE (1:1) - marked with an asterisk
		panel.add(new JLabel("Value*:"), "align label,top");
		JScrollPane valueScroll = new JScrollPane(valueArea);
		valueScroll.setPreferredSize(new Dimension(200, 80));
		panel.add(valueScroll, "growx,wrap");

		// MIME (0:1)
		panel.add(new JLabel("MIME:"), "align label");
		panel.add(mimeField, "growx,wrap");

		// LOCALE (0:1)
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeField, "growx,wrap");

		// RESTRICTION (0:1)
		panel.add(restrictionCheckBox, "span 2");

		return panel;
	}

	private JPanel createTranslationsPanel(){
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder("Translation"));

		translationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		translationList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editTranslation();
				}
			}
		});
		JScrollPane scrollPane = new JScrollPane(translationList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
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

		translationList.addListSelectionListener(e -> {
			boolean selected = translationList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addTranslation());
		newBtn.addActionListener(e -> createNewTranslation());
		editBtn.addActionListener(e -> editTranslation());
		deleteBtn.addActionListener(e -> deleteTranslation());

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
		JScrollPane scrollPane = new JScrollPane(sourceCitationList);
		scrollPane.setPreferredSize(new Dimension(200, 100));
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

	// ==================== Translation methods ====================

	private String getTranslationDisplay(FLEFRecord trans){
		String locale = FLEFRecordUtils.getChildValue(trans, "LOCALE");
		String value = FLEFRecordUtils.getChildValue(trans, "VALUE");
		if(locale != null && !locale.isEmpty() && value != null && !value.isEmpty()){
			return locale + ": " + value;
		}
		else if(locale != null && !locale.isEmpty()){
			return locale + ": [empty]";
		}
		else if(value != null && !value.isEmpty()){
			return value;
		}
		return "[empty]";
	}

	private void loadTranslations(){
		translationListModel.clear();
		translationRecords.clear();
		for(FLEFRecord child : record.getChildren()){
			if("TRANSLATION".equals(child.getTag())){
				translationRecords.add(child);
				translationListModel.addElement(getTranslationDisplay(child));
			}
		}
	}

	private void addTranslation(){
		TranslationDialog dialog = new TranslationDialog(this, model);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord trans = dialog.getTranslationRecord();
			if(trans != null){
				trans.setLevel(1);
				trans.setTag("TRANSLATION");
				translationRecords.add(trans);
				translationListModel.addElement(getTranslationDisplay(trans));
			}
		}
	}

	private void editTranslation(){
		int idx = translationList.getSelectedIndex();
		if(idx == -1)
			return;
		FLEFRecord existing = translationRecords.get(idx);
		TranslationDialog dialog = new TranslationDialog(this, model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getTranslationRecord();
			if(updated != null){
				translationRecords.set(idx, updated);
				translationListModel.set(idx, getTranslationDisplay(updated));
			}
		}
	}

	private void deleteTranslation(){
		int idx = translationList.getSelectedIndex();
		if(idx == -1)
			return;
		if(!showConfirm("Confirm", "Remove this translation?"))
			return;
		translationRecords.remove(idx);
		translationListModel.remove(idx);
	}

	private void createNewTranslation(){
		addTranslation();
	}

	// ==================== Source Citation methods ====================

	private void addSourceCitation(){
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, null);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setLevel(1);
				citation.setTag("SOURCE_CITATION");
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
			if(rec != null && sourceHandler != null){
				return sourceHandler.getDisplayName(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JDialog dialog = sourceHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
	}

	// ==================== Load Data ====================

	@Override
	protected void loadData(){
		idField.setText(record.getId());

		// VALUE (1:1)
		valueArea.setText(FLEFRecordUtils.getChildValue(record, "VALUE"));

		// MIME (0:1)
		mimeField.setText(FLEFRecordUtils.getChildValue(record, "MIME"));

		// LOCALE (0:1)
		localeField.setText(FLEFRecordUtils.getChildValue(record, "LOCALE"));

		// RESTRICTION (0:1)
		restrictionCheckBox.setSelected("confidential".equals(FLEFRecordUtils.getChildValue(record, "RESTRICTION")));

		// TRANSLATION (0:M)
		loadTranslations();

		// SOURCE_CITATION (0:M)
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// MODIFICATION (1:1)
		modificationPanel.loadFromRecord(record);
	}

	// ==================== Validation ====================

	@Override
	protected boolean validateData(){
		// VALUE (1:1) - required
		if(valueArea.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this,
				"VALUE is required.\nPlease enter the note content.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			valueArea.requestFocusInWindow();
			return false;
		}

		// MODIFICATION_STRUCTURE (1:1) - required
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return modificationPanel.validateRequiredFields();
	}

	// ==================== Save ====================

	@Override
	protected void saveRecord(){
		// Validation is already done by save() before calling this method
		record.getChildren().clear();

		// VALUE (1:1)
		String value = valueArea.getText().trim();
		if(!value.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "VALUE", value);
		}

		// MIME (0:1)
		String mime = mimeField.getText().trim();
		if(!mime.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "MIME", mime);
		}

		// LOCALE (0:1)
		String locale = localeField.getText().trim();
		if(!locale.isEmpty()){
			FLEFRecordUtils.updateChildValue(record, "LOCALE", locale);
		}

		// RESTRICTION (0:1)
		FLEFRecordUtils.updateChildValue(record, "RESTRICTION",
			restrictionCheckBox.isSelected()? "confidential": null);

		// TRANSLATION (0:M)
		for(FLEFRecord trans : translationRecords){
			trans.setLevel(1);
			trans.setTag("TRANSLATION");
			record.addChild(trans);
		}

		// SOURCE_CITATION (0:M)
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
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
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("NOTE");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "NOTE", "N");
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}

	// ==================== Main per test ====================

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		// Aggiungi una fonte di esempio per le source citations
		FLEFRecord source = new FLEFRecord();
		source.setId("S1");
		source.setType("SOURCE");
		FLEFRecord title = new FLEFRecord();
		title.setLevel(1);
		title.setTag("TITLE");
		title.setValue("Sample Book");
		source.addChild(title);
		model.addRecord(source);

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Note Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Note");
			btn.addActionListener(e -> {
				NoteDialog dialog = new NoteDialog(frame, model);
				dialog.setVisible(true);
				System.out.println("Note saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
