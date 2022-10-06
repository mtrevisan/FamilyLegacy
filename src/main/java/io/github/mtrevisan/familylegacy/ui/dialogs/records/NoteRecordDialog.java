/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;


public class NoteRecordDialog extends JDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -4624021267879013105L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private TextPreviewPane textPreviewView;
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleComboBox localeComboBox = new LocaleComboBox();
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton addTranslationButton = new JButton("Add translation");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode note;
	private volatile boolean updating;
	private int dataHash;

	private Consumer<Object> onAccept;
	private final Flef store;


	public static NoteRecordDialog createNote(final Flef store, final Frame parent){
		final NoteRecordDialog dialog = new NoteRecordDialog(store, parent);
		dialog.initComponents();
		return dialog;
	}

	public static NoteRecordDialog createChangeNote(final Flef store, final Frame parent){
		final NoteRecordDialog dialog = new NoteRecordDialog(store, parent);
		dialog.changeNoteInitComponents();
		return dialog;
	}


	private NoteRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;
	}

	private void initComponents(){
		setTitle("Note");

		textPreviewView = TextPreviewPane.createWithPreview(this);

		localeLabel.setLabelFor(localeComboBox);
		localeComboBox.addActionListener(evt -> textChanged());

		restrictionCheckBox.addActionListener(evt -> textChanged());

		addTranslationButton.addActionListener(evt -> {
			final GedcomNode newTranslation = store.create("TRANSLATION");

			final Consumer<Object> onAccept = ignored -> {
				//TODO add node from translation note record dialog
				note.addChild(
					newTranslation
				);
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, newTranslation, onAccept));
		});

		final ActionListener acceptAction = evt -> {
			okAction();

			if(onAccept != null)
				onAccept.accept(this);

			dispose();
		};
		final ActionListener cancelAction = evt -> dispose();
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(acceptAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout("", "[grow]"));
		add(textPreviewView, "span 2,grow,wrap");
		add(localeLabel, "align label,split 2,sizegroup label");
		add(localeComboBox, "wrap");
		add(restrictionCheckBox, "wrap paragraph");
		add(addTranslationButton, "tag add,span 2,sizegroup button2,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void changeNoteInitComponents(){
		setTitle("Change note");

		textPreviewView = TextPreviewPane.createWithoutPreview();

		localeLabel.setLabelFor(localeComboBox);
		localeComboBox.addActionListener(evt -> textChanged());

		final ActionListener acceptAction = evt -> {
			okAction();

			if(onAccept != null)
				onAccept.accept(this);

			dispose();
		};
		final ActionListener cancelAction = evt -> dispose();
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(acceptAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout("debug", "[grow]", "[fill,grow][][]"));
		add(textPreviewView, "grow,wrap");
		add(localeLabel, "align label,split 2,sizegroup label");
		add(localeComboBox, "wrap");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	@Override
	public final void textChanged(){
		if(!updating)
			okButton.setEnabled(calculateDataHash() != dataHash);
	}

	private int calculateDataHash(){
		final int textHash = textPreviewView.getText()
			.hashCode();
		final int languageTagHash = localeComboBox.getSelectedLanguageTag()
			.hashCode();
		final int restrictionHash = (restrictionCheckBox.isSelected()? "confidential": StringUtils.EMPTY)
			.hashCode();
		return textHash ^ languageTagHash ^ restrictionHash;
	}

	@Override
	public final void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	private void okAction(){
		final String text = textPreviewView.getText();
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		final String creationDate = store.traverse(note, "CREATION_DATE.DATE").getValue();
		if(creationDate == null){
			note.clearAll();
			note.withValue(StringUtils.replace(text, "\n", "\\n"))
				.addChildValue("LOCALE", localeComboBox.getSelectedLanguageTag())
				.addChildValue("RESTRICTION", (restrictionCheckBox.isSelected()? "confidential": null))
				.addChild(
					store.create("CREATION_DATE")
						.addChildValue("DATE", now)
				);
		}
		else{
			//show note record dialog
			final NoteRecordDialog changeNoteDialog = createChangeNote(store, (Frame)getParent());
			final GedcomNode changeNote = store.create("NOTE");
			changeNoteDialog.loadData(changeNote, ignored -> {
				note.clearAll();
				note.withValue(StringUtils.replace(text, "\n", "\\n"))
					.addChildValue("LOCALE", localeComboBox.getSelectedLanguageTag())
					.addChildValue("RESTRICTION", (restrictionCheckBox.isSelected()? "confidential": null))
					.addChild(
						store.create("CREATION_DATE")
							.addChildValue("DATE", creationDate)
					)
					.addChild(
						store.create("CHANGE_DATE")
							.addChildValue("DATE", now)
							.addChildValue("NOTE", StringUtils.replace(changeNote.getValue(), "\n", "\\n"))
					);
			});

			changeNoteDialog.setSize(450, 500);
			changeNoteDialog.setLocationRelativeTo(this);
			changeNoteDialog.setVisible(true);
		}
	}

	public final void loadData(final GedcomNode note, final Consumer<Object> onAccept){
		updating = true;

		this.note = note;
		this.onAccept = onAccept;

		final String id = note.getID();
		if(id != null)
			setTitle("Note " + id);

		final String text = StringUtils.replace(note.getValue(), "\\n", "\n");
		final String languageTag = store.traverse(note, "LOCALE").getValue();
		final String restriction = store.traverse(note, "RESTRICTION").getValue();

		textPreviewView.setText(getTitle(), text, languageTag);

		if(languageTag != null)
			localeComboBox.setSelectedItem(Locale.forLanguageTag(languageTag));

		restrictionCheckBox.setSelected("confidential".equals(restriction));

		updating = false;

		dataHash = calculateDataHash();

		repaint();
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.7.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		//without creation date
//		final GedcomNode note = store.getNotes().get(0);
		//with creation date
		final GedcomNode note = store.getNotes().get(1);

		EventQueue.invokeLater(() -> {
			final NoteRecordDialog dialog = createNote(store, new JFrame());
			dialog.loadData(note, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 340);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
