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
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
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
import java.util.Locale;
import java.util.function.Consumer;


public final class NoteRecordDialog extends JDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -4624021267879013105L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private static final ImageIcon TRANSLATION = ResourceHelper.getImage("/images/translation.png", 20, 20);
	private static final ImageIcon CITATION = ResourceHelper.getImage("/images/citation.png", 20, 20);

	private TextPreviewPane textPreviewView;
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleComboBox localeComboBox = new LocaleComboBox();
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton addTranslationButton = new JButton(TRANSLATION);
	private final JButton addCitationButton = new JButton(CITATION);
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode note;
	private long originalNoteTranslationsHash;
	private long originalNoteSourcesHash;

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

	private static NoteRecordDialog createChangeNote(final Flef store, final Frame parent){
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

		final Border originalButtonBorder = okButton.getBorder();
		addTranslationButton.setToolTipText("Add translation");
		addTranslationButton.addActionListener(evt -> {
			final Consumer<Object> onAccept = ignored -> {
				addTranslationButton.setBorder(calculateTranslationsHashCode() != originalNoteTranslationsHash
					? new LineBorder(Color.BLUE)
					: originalButtonBorder);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_TRANSLATION_CITATION, note, onAccept));
		});
		addCitationButton.setToolTipText("Add citation");
		addCitationButton.addActionListener(evt -> {
			final GedcomNode newSourceCitation = store.create("SOURCE");

			final Consumer<Object> onAccept = ignored -> {
				//add node from source citation dialog
				note.addChild(newSourceCitation);

				addCitationButton.setBorder(calculateSourcesHashCode() != originalNoteSourcesHash
					? new LineBorder(Color.BLUE)
					: originalButtonBorder);

				//put focus on the ok button
				okButton.grabFocus();
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, newSourceCitation, onAccept));
		});

		final ActionListener acceptAction = evt -> okAction();
		final ActionListener cancelAction = evt -> setVisible(false);
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(acceptAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow][]"));
		add(textPreviewView, "spanx 2,spany 2,grow");
		add(addTranslationButton, "tag add,sizegroup button2,wrap");
		add(addCitationButton, "tag add,top,sizegroup button2,wrap");
		add(localeLabel, "align label,spanx 3,split 2,sizegroup label");
		add(localeComboBox, "spanx 3,wrap");
		add(restrictionCheckBox, "spanx 3,wrap");
		add(helpButton, "tag help2,spanx 3,split 3,sizegroup button");
		add(okButton, "tag ok,spanx 3,sizegroup button");
		add(cancelButton, "tag cancel,spanx 3,sizegroup button");
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
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
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
		final String text = textPreviewView.getText();
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		final String creationDate = store.traverse(note, "CREATION_DATE.DATE").getValue();
		if(creationDate == null){
			note.clearAll();
			note.withValue(fromNoteText(text))
				.addChildValue("LOCALE", localeComboBox.getSelectedLanguageTag())
				.addChildValue("RESTRICTION", (restrictionCheckBox.isSelected()? "confidential": null))
				.addChild(
					store.create("CREATION_DATE")
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
			changeNoteDialog.setTitle("Change note for " + note.getID());
			changeNoteDialog.loadData(changeNote, ignored -> {
				note.clearAll();
				note.withValue(fromNoteText(text))
					.addChildValue("LOCALE", localeComboBox.getSelectedLanguageTag())
					.addChildValue("RESTRICTION", (restrictionCheckBox.isSelected()? "confidential": null))
					.addChild(
						store.create("CREATION_DATE")
							.addChildValue("DATE", creationDate)
					)
					.addChild(
						store.create("CHANGE_DATE")
							.addChildValue("DATE", now)
							.addChildValue("NOTE", fromNoteText(changeNote.getValue()))
					);

				if(onAccept != null)
					onAccept.accept(this);
			});

			changeNoteDialog.setSize(450, 500);
			changeNoteDialog.setLocationRelativeTo(this);
			changeNoteDialog.setVisible(true);
		}
	}

	public void loadData(final GedcomNode note, final Consumer<Object> onAccept){
		this.note = note;
		this.onAccept = onAccept;

		originalNoteTranslationsHash = calculateTranslationsHashCode();
		originalNoteSourcesHash = calculateSourcesHashCode();

		final String text = toNoteText(note);
		final String languageTag = store.traverse(note, "LOCALE").getValue();
		final String restriction = store.traverse(note, "RESTRICTION").getValue();

		textPreviewView.setText(getTitle(), text, languageTag);

		if(languageTag != null)
			localeComboBox.setSelectedItem(Locale.forLanguageTag(languageTag));

		restrictionCheckBox.setSelected("confidential".equals(restriction));
	}

	private int calculateTranslationsHashCode(){
		return store.traverseAsList(note, "TRANSLATION[]").hashCode();
	}

	private int calculateSourcesHashCode(){
		return store.traverseAsList(note, "SOURCE[]").hashCode();
	}

	private static String toNoteText(final GedcomNode note){
		return StringUtils.replace(note.getValue(), "\\n", "\n");
	}

	public static String toVisualText(final GedcomNode note){
		return StringUtils.replace(note.getValue(), "\\n", "↵");
	}

	private static String fromNoteText(final String text){
		return StringUtils.replace(text, "\n", "\\n");
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
		//with change date
		final GedcomNode note = store.getNotes().get(1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand){
					System.out.println("Received event " + editCommand);

					if(editCommand.getType() == EditEvent.EditType.NOTE_TRANSLATION){
						final GedcomNode noteTranslation = editCommand.getContainer();
						final NoteRecordDialog noteDialog = createNoteTranslation(store, parent);
						noteDialog.setTitle("Translation for " + note.getID());
						noteDialog.loadData(noteTranslation, editCommand.getOnCloseGracefully());

						noteDialog.setSize(550, 350);
						noteDialog.setLocationRelativeTo(parent);
						noteDialog.setVisible(true);
					}
					else if(editCommand.getType() == EditEvent.EditType.NOTE_TRANSLATION_CITATION){
						final GedcomNode noteTranslationCitation = editCommand.getContainer();
						final NoteCitationDialog noteTranslationCitationDialog = NoteCitationDialog.createNoteTranslationCitation(store, parent);
						noteTranslationCitationDialog.setTitle("Note translation citation for " + note.getID());
						if(!noteTranslationCitationDialog.loadData(noteTranslationCitation, editCommand.getOnCloseGracefully()))
							//show a note input dialog
							noteTranslationCitationDialog.addAction();

						noteTranslationCitationDialog.setSize(550, 450);
						noteTranslationCitationDialog.setLocationRelativeTo(parent);
						noteTranslationCitationDialog.setVisible(true);
					}
					else if(editCommand.getType() == EditEvent.EditType.SOURCE){
						final GedcomNode source = editCommand.getContainer();
						final SourceRecordDialog sourceRecordDialog = new SourceRecordDialog(store, parent);
						sourceRecordDialog.setTitle("Source for " + note.getID());
						sourceRecordDialog.loadData(source, editCommand.getOnCloseGracefully());

						sourceRecordDialog.setSize(500, 650);
						sourceRecordDialog.setLocationRelativeTo(parent);
						sourceRecordDialog.setVisible(true);
					}
					else if(editCommand.getType() == EditEvent.EditType.SOURCE_CITATION){
						final GedcomNode sourceCitation = editCommand.getContainer();
						final SourceCitationDialog sourceCitationDialog = new SourceCitationDialog(store, parent);
						sourceCitationDialog.setTitle("Source citation for " + note.getID());
						if(!sourceCitationDialog.loadData(sourceCitation, editCommand.getOnCloseGracefully()))
							//show a source input dialog
							sourceCitationDialog.addAction();

						sourceCitationDialog.setSize(550, 450);
						sourceCitationDialog.setLocationRelativeTo(parent);
						sourceCitationDialog.setVisible(true);
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
			dialog.setSize(500, 340);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
