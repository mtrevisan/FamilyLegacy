/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class NoteRecordDialog extends CommonRecordDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -8696687603069555837L;

	private static final String TABLE_NAME = "note";


	private JLabel noteLabel;
	private TextPreviewPane noteTextArea;
	private JLabel localeLabel;
	private JTextField localeField;

	private JCheckBox restrictionCheckBox;


	public NoteRecordDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public NoteRecordDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
		super.setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected void initRecordComponents(){
		setTitle("Note");

		noteLabel = new JLabel("Note:");
		noteTextArea = TextPreviewPane.createWithPreview(this);
		noteTextArea.setTextViewFont(noteLabel.getFont());
		localeLabel = new JLabel("Locale:");
		localeField = new JTextField();

		restrictionCheckBox = new JCheckBox("Confidential");


		GUIHelper.bindLabelTextChange(noteLabel, noteTextArea, evt -> saveData());
		noteTextArea.setTextViewBackgroundColor(MANDATORY_COMBOBOX_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, evt -> saveData());

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordPanel){
		recordPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanel.add(noteLabel, "align label,top,sizegroup label,split 2");
		recordPanel.add(noteTextArea, "grow,wrap related");
		recordPanel.add(localeLabel, "align label,sizegroup label,split 2");
		recordPanel.add(localeField, "grow,wrap paragraph");
		recordPanel.add(restrictionCheckBox);
	}


	public void showNewRecord(){
		//create a new record
		final Map<String, Object> newTable = new HashMap<>();
		final TreeMap<Integer, Map<String, Object>> storeTables = getRecords(getTableName());
		final int nextRecordID = extractNextRecordID(storeTables);
		newTable.put("id", nextRecordID);
		storeTables.put(nextRecordID, newTable);

		loadData(nextRecordID);
	}

	public boolean loadData(final int recordID){
		selectedRecord = getRecords(TABLE_NAME)
			.get(recordID);

		if(selectedRecord != null){
			fillData();

			return true;
		}
		return false;
	}

	@Override
	public void loadData(){}

	@Override
	protected void fillData(){
		final int noteID = extractRecordID(selectedRecord);
		final String note = extractRecordNote(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		setTitle("Note " + noteID);
		noteTextArea.setText("Note " + extractRecordID(selectedRecord), note, locale);
		localeField.setText(locale);

		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		noteTextArea.clear();
		noteTextArea.setTextViewBackgroundColor(Color.WHITE);
		localeField.setText(null);

		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		final String note = noteTextArea.getText();
		if(!validData(note)){
			JOptionPane.showMessageDialog(getParent(), "Note field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			noteTextArea.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String note = noteTextArea.getText();
		final String locale = localeField.getText();

		selectedRecord.put("note", note);
		selectedRecord.put("locale", locale);
	}


	private static String extractRecordNote(final Map<String, Object> record){
		return (String)record.get("note");
	}

	private static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
	}


	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put(TABLE_NAME, notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "a note");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 2");
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 2);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put(TABLE_NAME_RESTRICTION, restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();

			final NoteRecordDialog dialog = new NoteRecordDialog(store, parent);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.loadData(extractRecordID(note1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(420, 285);
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}
