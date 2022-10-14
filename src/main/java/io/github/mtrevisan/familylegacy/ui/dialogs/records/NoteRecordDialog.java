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
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;


public final class NoteRecordDialog extends JDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -4624021267879013105L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private static final ImageIcon ICON_TRANSLATION = ResourceHelper.getImage("/images/translation.png", 20, 20);
	private static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png", 20, 20);

	private TextPreviewPane textPreviewView;
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleComboBox localeComboBox = new LocaleComboBox();
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton translationButton = new JButton(ICON_TRANSLATION);
	private final JButton sourceButton = new JButton(ICON_SOURCE);
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode note;

	private Consumer<Object> onAccept;
	private final Flef store;


	public static NoteRecordDialog createNote(final Flef store, final Frame parent){
		final NoteRecordDialog dialog = new NoteRecordDialog(store, parent);
		dialog.initComponents();
		return dialog;
	}

	public static NoteRecordDialog createNoteTranslation(final Flef store, final Frame parent){
		final NoteRecordDialog dialog = new NoteRecordDialog(store, parent);
		dialog.changeNoteInitComponents();
		return dialog;
	}

	static NoteRecordDialog createChangeNote(final Flef store, final Frame parent){
		final NoteRecordDialog dialog = new NoteRecordDialog(store, parent);
		dialog.changeNoteInitComponents();
		return dialog;
	}


	private NoteRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;
	}

	private void initComponents(){
		textPreviewView = TextPreviewPane.createWithPreview(this);

		localeLabel.setLabelFor(localeComboBox);
		localeComboBox.addActionListener(evt -> textChanged());

		restrictionCheckBox.addActionListener(evt -> textChanged());

		translationButton.setToolTipText("Add translation");
		translationButton.addActionListener(evt -> {
			final Consumer<Object> onAccept = ignored -> {
				translationButton.setBorder(store.traverseAsList(note, "TRANSLATION[]").isEmpty()
					? UIManager.getBorder("Button.border")
					: new LineBorder(Color.BLUE));

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_TRANSLATION_CITATION, note, onAccept));
		});

		sourceButton.setToolTipText("Add source");
		sourceButton.addActionListener(evt -> {
			final Consumer<Object> onAccept = ignored -> {
				sourceButton.setBorder(store.traverseAsList(note, "SOURCE[]").isEmpty()
					? UIManager.getBorder("Button.border")
					: new LineBorder(Color.BLUE));

				//put focus on the ok button
				okButton.grabFocus();
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, note, onAccept));
		});

		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> setVisible(false);
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow][]"));
		add(textPreviewView, "spanx 2,spany 2,grow");
		add(translationButton, "tag add,sizegroup button,wrap");
		add(sourceButton, "tag add,top,sizegroup button,wrap");
		add(localeLabel, "align label,spanx 3,split 2,sizegroup label");
		add(localeComboBox, "spanx 3,wrap");
		add(restrictionCheckBox, "spanx 3,wrap");
		add(helpButton, "tag help2,spanx 3,split 3,sizegroup button2");
		add(okButton, "tag ok,spanx 3,sizegroup button2");
		add(cancelButton, "tag cancel,spanx 3,sizegroup button2");
	}

	private void changeNoteInitComponents(){
		textPreviewView = TextPreviewPane.createWithoutPreview(this);

		localeLabel.setLabelFor(localeComboBox);
		localeComboBox.addActionListener(evt -> textChanged());

		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> setVisible(false);
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[fill,grow][][]"));
		add(textPreviewView, "grow,wrap");
		add(localeLabel, "align label,split 2,sizegroup label");
		add(localeComboBox, "wrap");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	@Override
	public void textChanged(){
		okButton.setEnabled(StringUtils.isNotBlank(textPreviewView.getText()));
	}

	@Override
	@SuppressWarnings("BooleanParameter")
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	private void okAction(){
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());

		final String text = fromNoteText(textPreviewView.getText());
		final List<GedcomNode> translations = store.traverseAsList(note, "TRANSLATION[]");
		final List<GedcomNode> sources = store.traverseAsList(note, "SOURCE[]");
		final GedcomNode creation = store.traverse(note, "CREATION");
		if(creation.isEmpty()){
			note.clearAll();
			note.withValue(text)
				.addChildValue("LOCALE", localeComboBox.getSelectedLanguageTag())
				.addChildren(translations)
				.addChildren(sources)
				.addChildValue("RESTRICTION", (restrictionCheckBox.isSelected()? "confidential": null))
				.addChild(
					store.create("CREATION")
						.addChildValue("DATE", now)
				);

			if(onAccept != null)
				onAccept.accept(this);

			setVisible(false);
		}
		else{
			//show note record dialog
			final GedcomNode changeNote = store.create("NOTE");
			final NoteRecordDialog changeNoteDialog = createChangeNote(store, (Frame)getParent());
			changeNoteDialog.setTitle("Change note for note " + note.getID());
			changeNoteDialog.loadData(changeNote, ignored -> {
				final List<GedcomNode> update = store.traverseAsList(note, "UPDATE[]");
				note.clearAll();
				note.withValue(text)
					.addChildValue("LOCALE", localeComboBox.getSelectedLanguageTag())
					.addChildren(translations)
					.addChildren(sources)
					.addChildValue("RESTRICTION", (restrictionCheckBox.isSelected()? "confidential": null))
					.addChild(creation)
					.addChildren(update)
					.addChild(
						store.create("UPDATE")
							.addChildValue("DATE", now)
							.addChildValue("NOTE", fromNoteText(changeNote.getValue()))
					);

				if(onAccept != null)
					onAccept.accept(this);

				setVisible(false);
			});

			changeNoteDialog.setSize(450, 500);
			changeNoteDialog.setLocationRelativeTo(this);
			changeNoteDialog.setVisible(true);
		}
	}

	public void loadData(final GedcomNode note, final Consumer<Object> onAccept){
		this.note = note;
		this.onAccept = onAccept;

		final String text = toNoteText(note);
		final String languageTag = store.traverse(note, "LOCALE").getValue();
		final String restriction = store.traverse(note, "RESTRICTION").getValue();

		textPreviewView.setText(getTitle(), text, languageTag);

		if(languageTag != null)
			localeComboBox.setSelectedItem(Locale.forLanguageTag(languageTag));

		restrictionCheckBox.setSelected("confidential".equals(restriction));

		translationButton.setBorder(store.traverseAsList(note, "TRANSLATION[]").isEmpty()
			? UIManager.getBorder("Button.border")
			: new LineBorder(Color.BLUE));
		sourceButton.setBorder(store.traverseAsList(note, "SOURCE[]").isEmpty()
			? UIManager.getBorder("Button.border")
			: new LineBorder(Color.BLUE));
	}

	private static String toNoteText(final GedcomNode note){
		return StringUtils.replace(note.getValue(), "\\n", "\n");
	}

	public static String toVisualText(final GedcomNode note){
		return StringUtils.replace(note.getValue(), "\\n", "↵");
	}

	static String fromNoteText(final String text){
		return StringUtils.replace(text, "\n", "\\n");
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.8.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		//without creation date
//		final GedcomNode note = store.getNotes().get(0);
		//with change date
		final GedcomNode note = store.getNotes().get(1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					final String forNote = (note.getID() != null? " for note " + note.getID(): " for new note");
					switch(editCommand.getType()){
						case NOTE_TRANSLATION -> {
							final NoteRecordDialog dialog = createNoteTranslation(store, parent);
							final GedcomNode noteTranslation = editCommand.getContainer();
							dialog.setTitle(StringUtils.isNotBlank(noteTranslation.getValue())
								? "Translation for language " + store.traverse(noteTranslation, "LOCALE").getValue() + forNote
								: "New translation" + forNote
							);
							dialog.loadData(noteTranslation, editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_TRANSLATION_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteTranslationCitation(store, parent);
							dialog.setTitle("Translation citations" + forNote);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								//show a note input dialog
								dialog.addAction();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE -> {
							final SourceRecordDialog dialog = new SourceRecordDialog(store, parent);
							final GedcomNode source = editCommand.getContainer();
							dialog.setTitle(source.getID() != null
								? "Source " + source.getID() + forNote
								: "New source" + forNote);
							dialog.loadData(source, editCommand.getOnCloseGracefully());

							dialog.setSize(500, 650);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE_CITATION -> {
							final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
							dialog.setTitle("Source citations" + forNote);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								//show a source input dialog
								dialog.addAction();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final NoteRecordDialog dialog = createNote(store, parent);
			dialog.setTitle("Note for " + note.getID());
			dialog.loadData(note, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 330);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
