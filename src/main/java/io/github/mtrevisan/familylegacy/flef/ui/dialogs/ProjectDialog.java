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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCopyright;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIncludeMediaPayload;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordNote;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordUpdateDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCopyright;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCreationDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordIncludeMediaPayload;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordNote;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordProtocolName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordProtocolVersion;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordUpdateDate;


public final class ProjectDialog extends CommonRecordDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -3776676890876630508L;


	private final JLabel copyrightLabel = new JLabel("Copyright:");
	private final JTextField copyrightField = new JTextField();
	private final JLabel noteLabel = new JLabel("Note:");
	private final TextPreviewPane noteTextPreview = TextPreviewPane.createWithPreview(ProjectDialog.this);
	private final JLabel localeLabel = new JLabel("Locale:");
	private final JTextField localeField = new JTextField();
	private final JLabel includeMediaPayloadLabel = new JLabel("Include media payload:");
	private final JComboBox<String> includeMediaPayloadComboBox = new JComboBox<>(new String[]{"FALSE", "TRUE"});


	public static ProjectDialog create(final Frame parent){
		final ProjectDialog dialog = new ProjectDialog(parent);
		dialog.initialize();
		return dialog;
	}


	private ProjectDialog(final Frame parent){
		super(parent);
	}


	public ProjectDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_NAME_PROJECT;
	}

	@Override
	protected void initComponents(){
		setTitle(StringUtils.capitalize(getTableName()));


		GUIHelper.bindLabelTextChangeUndo(copyrightLabel, copyrightField, this::saveData);

		GUIHelper.bindLabelTextChange(noteLabel, noteTextPreview, this::saveData);
		noteTextPreview.setTextViewFont(copyrightField.getFont());
		noteTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);

		GUIHelper.bindLabelSelectionChange(includeMediaPayloadLabel, includeMediaPayloadComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordPanel){
		recordPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanel.add(copyrightLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(copyrightField, "grow,wrap paragraph");
		recordPanel.add(noteLabel, "align label,top,sizegroup lbl,split 2");
		recordPanel.add(noteTextPreview, "grow,wrap related");
		recordPanel.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(localeField, "grow,wrap paragraph");
		recordPanel.add(includeMediaPayloadLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(includeMediaPayloadComboBox);
	}

	@Override
	public void loadData(){
		final Map<String, Object> record = Repository.findByID(EntityManager.NODE_NAME_PROJECT, 1);
		selectedRecord = (record != null? new HashMap<>(record): new HashMap<>());

		ignoreEvents = true;
		fillData();
		ignoreEvents = false;


		//set focus on first field
		copyrightField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final String copyright = extractRecordCopyright(selectedRecord);
		final String note = extractRecordNote(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final int includeMediaPayload = extractRecordIncludeMediaPayload(selectedRecord);

		copyrightField.setText(copyright);
		noteTextPreview.setText("Project note", note, locale);
		localeField.setText(locale);
		includeMediaPayloadComboBox.setSelectedIndex(includeMediaPayload);
	}

	@Override
	protected void clearData(){
		copyrightField.setText(null);
		noteTextPreview.clear();
		localeField.setText(null);
		includeMediaPayloadComboBox.setSelectedIndex(0);
	}

	@Override
	protected boolean validateData(){
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		//read record panel:
		final String copyright = GUIHelper.getTextTrimmed(copyrightField);
		final String note = noteTextPreview.getTextTrimmed();
		final String locale = GUIHelper.getTextTrimmed(localeField);
		final int includeMediaPayload = includeMediaPayloadComboBox.getSelectedIndex();
		final String updateDate = extractRecordUpdateDate(selectedRecord);

		insertRecordProtocolName(selectedRecord, EntityManager.PROTOCOL_NAME_DEFAULT);
		insertRecordProtocolVersion(selectedRecord, EntityManager.PROTOCOL_VERSION_DEFAULT);
		insertRecordCopyright(selectedRecord, copyright);
		insertRecordNote(selectedRecord, note);
		insertRecordLocale(selectedRecord, locale);
		insertRecordIncludeMediaPayload(selectedRecord, includeMediaPayload);
		if(updateDate == null)
			insertRecordCreationDate(selectedRecord, now);
		else
			insertRecordUpdateDate(selectedRecord, now);

		return true;
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


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> project1 = new HashMap<>();
		project1.put("protocol_name", EntityManager.PROTOCOL_NAME_DEFAULT);
		project1.put("protocol_version", EntityManager.PROTOCOL_VERSION_DEFAULT);
		project1.put("copyright", "(c) 2024");
		project1.put("note", "some notes");
		project1.put("locale", "en-US");
		project1.put("include_media_payload", 1);
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		project1.put("creation_date", now);
		project1.put("update_date", now);
		Repository.save(EntityManager.NODE_NAME_PROJECT, project1);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){}
			};
			EventBusService.subscribe(listener);

			final ProjectDialog dialog = create(parent);
			dialog.loadData();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
