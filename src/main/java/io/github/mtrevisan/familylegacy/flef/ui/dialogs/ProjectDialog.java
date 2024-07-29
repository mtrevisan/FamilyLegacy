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

import io.github.mtrevisan.familylegacy.flef.db.DatabaseManager;
import io.github.mtrevisan.familylegacy.flef.db.DatabaseManagerInterface;
import io.github.mtrevisan.familylegacy.flef.helpers.DependencyInjector;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.IOException;
import java.io.Serial;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class ProjectDialog extends CommonRecordDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -3776676890876630508L;

	private static final String TABLE_NAME = "project";

	private static final String PROTOCOL_NAME_DEFAULT = "Family LEgacy Format";
	private static final String PROTOCOL_VERSION_DEFAULT = "0.0.10";


	private JLabel copyrightLabel;
	private JTextField copyrightField;
	private JLabel noteLabel;
	private TextPreviewPane noteTextPreview;
	private JLabel localeLabel;
	private JTextField localeField;


	public static ProjectDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new ProjectDialog(store, parent);
	}


	private ProjectDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public ProjectDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected void initRecordComponents(){
		setTitle("Project");

		copyrightLabel = new JLabel("Copyright:");
		copyrightField = new JTextField();

		noteLabel = new JLabel("Note:");
		noteTextPreview = TextPreviewPane.createWithPreview(this);
		noteTextPreview.setTextViewFont(copyrightField.getFont());

		localeLabel = new JLabel("Locale:");
		localeField = new JTextField();


		GUIHelper.bindLabelTextChangeUndo(copyrightLabel, copyrightField, this::saveData);

		GUIHelper.bindLabelTextChange(noteLabel, noteTextPreview, this::saveData);

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordPanel){
		recordPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanel.add(copyrightLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(copyrightField, "grow,wrap paragraph");
		recordPanel.add(noteLabel, "align label,top,sizegroup lbl,split 2");
		recordPanel.add(noteTextPreview, "grow,wrap related");
		recordPanel.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(localeField, "grow");
	}

	@Override
	public void loadData(){
		selectedRecord = getRecords(TABLE_NAME)
			.computeIfAbsent(1, k -> new HashMap<>());

		ignoreEvents = true;
		fillData();
		ignoreEvents = false;
	}

	@Override
	protected void fillData(){
		final String copyright = extractRecordCopyright(selectedRecord);
		final String note = extractRecordNote(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);

		copyrightField.setText(copyright);
		noteTextPreview.setText("Project note", note, locale);
		localeField.setText(locale);
	}

	@Override
	protected void clearData(){
		copyrightField.setText(null);
		noteTextPreview.clear();
		localeField.setText(null);
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
		final String updateDate = extractUpdateDate(selectedRecord);

		selectedRecord.put("protocol_name", PROTOCOL_NAME_DEFAULT);
		selectedRecord.put("protocol_version", PROTOCOL_VERSION_DEFAULT);
		selectedRecord.put("copyright", copyright);
		selectedRecord.put("note", note);
		selectedRecord.put("locale", locale);
		selectedRecord.put((updateDate == null? "creation_date": "update_date"), now);

		return true;
	}


	private static String extractRecordCopyright(final Map<String, Object> record){
		return (String)record.get("copyright");
	}

	private static String extractRecordNote(final Map<String, Object> record){
		return (String)record.get("note");
	}

	private static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
	}

	private static String extractUpdateDate(final Map<String, Object> record){
		return (String)record.get("update_date");
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

		final TreeMap<Integer, Map<String, Object>> projects = new TreeMap<>();
		store.put(TABLE_NAME, projects);
		final Map<String, Object> project = new HashMap<>();
		project.put("id", 1);
		project.put("protocol_name", PROTOCOL_NAME_DEFAULT);
		project.put("protocol_version", PROTOCOL_VERSION_DEFAULT);
		project.put("copyright", "(c) 2024");
		project.put("note", "some notes");
		project.put("locale", "en-US");
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		project.put("creation_date", now);
		project.put("update_date", now);
		projects.put((Integer)project.get("id"), project);

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

			final DependencyInjector injector = new DependencyInjector();
			final DatabaseManager dbManager = new DatabaseManager("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
			try{
				final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
				dbManager.initialize(grammarFile);

				dbManager.insertDatabase(store);
			}
			catch(final SQLException | IOException e){
				throw new RuntimeException(e);
			}
			injector.register(DatabaseManagerInterface.class, dbManager);

			final ProjectDialog dialog = create(store, parent);
			injector.injectDependencies(dialog);
			dialog.initComponents();
			dialog.loadData();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.setSize(420, 282);
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
