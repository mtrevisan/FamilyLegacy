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
import io.github.mtrevisan.familylegacy.ui.dialogs.DatePanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.EventsPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.DocumentStructureDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;


public class SourceRecordDialog extends JDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 1754367426928623503L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private static final double DATE_HEIGHT = 17.;
	private static final double DATE_ASPECT_RATIO = 270 / 248.;
	private static final Dimension DATE_SIZE = new Dimension((int)(DATE_HEIGHT / DATE_ASPECT_RATIO), (int)DATE_HEIGHT);

	//https://thenounproject.com/term/weekly-calendar/541199/
	private static final ImageIcon DATE = ResourceHelper.getImage("/images/date.png", DATE_SIZE);

	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});
	private static final DefaultComboBoxModel<String> EXTRACT_TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"transcript", "extract", "abstract"});

	private final EventsPanel eventsPanel = new EventsPanel(this::sourceContainsEvent);
	private final JLabel titleLabel = new JLabel("Title:");
	private final JTextField titleField = new JTextField();
	private final JLabel authorLabel = new JLabel("Author:");
	private final JTextField authorField = new JTextField();
	private final JLabel publicationFactsLabel = new JLabel("Publication facts:");
	private final JTextField publicationFactsField = new JTextField();
	private final DatePanel datePanel = new DatePanel();
	private final LocaleComboBox extractLocaleComboBox = new LocaleComboBox();
	private final JLabel mediaTypeLabel = new JLabel("Media type:");
	private final JTextField mediaTypeField = new JTextField();
	private final JButton placesButton = new JButton("Places");
	private final JButton repositoriesButton = new JButton("Repositories");
	private final JButton documentsButton = new JButton("Documents");
	private final JButton sourcesButton = new JButton("Sources");
	private final JButton notesButton = new JButton("Notes");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode source;

	private Consumer<Object> onAccept;
	private final Flef store;


	public SourceRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		titleLabel.setLabelFor(titleField);

		placesButton.setEnabled(false);
		placesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE_CITATION, source)));

		repositoriesButton.setEnabled(false);
		repositoriesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY_CITATION, source)));

		mediaTypeLabel.setLabelFor(mediaTypeField);

		documentsButton.setEnabled(false);
		documentsButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.DOCUMENT_CITATION, source)));

		sourcesButton.setEnabled(false);
		sourcesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, source)));

		notesButton.setEnabled(false);
		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, source)));

		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> setVisible(false);
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(eventsPanel, "grow,wrap paragraph");
		add(titleLabel, "align label,split 2");
		add(titleField, "grow,wrap");
		add(authorLabel, "align label,split 2");
		add(authorField, "grow,wrap");
		add(publicationFactsLabel, "align label,split 2");
		add(publicationFactsField, "grow,wrap paragraph");
		add(datePanel, "grow,wrap paragraph");
		add(placesButton, "sizegroup button2,grow,wrap");
		add(repositoriesButton, "sizegroup button2,grow,wrap paragraph");
		add(mediaTypeLabel, "align label,split 2");
		add(mediaTypeField, "grow,wrap paragraph");
		add(documentsButton, "sizegroup button2,grow,wrap");
		add(notesButton, "sizegroup button2,grow,wrap");
		add(sourcesButton, "sizegroup button2,grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private boolean sourceContainsEvent(final String event){
		boolean containsEvent = false;
		final List<GedcomNode> events = store.traverseAsList(source, "EVENT[]");
		for(int i = 0; !containsEvent && i < events.size(); i ++)
			if(events.get(i).getValue().equalsIgnoreCase(event))
				containsEvent = true;
		return containsEvent;
	}

	private void okAction(){
		final String event = String.join(",", eventsPanel.getTags());
		final String title = titleField.getText();
		final String mediaType = mediaTypeField.getText();

		source.replaceChildValue("EVENT", event);
		source.replaceChildValue("TITLE", title);
		//TODO
	}

	@Override
	public final void textChanged(){
		//TODO
		okButton.setEnabled(true);
	}

	@Override
	@SuppressWarnings("BooleanParameter")
	public final void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	public final void loadData(final GedcomNode source, final Consumer<Object> onAccept){
		this.source = source;
		this.onAccept = onAccept;

		final String id = source.getID();
		setTitle(id != null? "Source " + id: "New Source");

		final StringJoiner events = new StringJoiner(", ");
		for(final GedcomNode event : store.traverseAsList(source, "EVENT[]"))
			events.add(event.getValue());
		final String title = store.traverse(source, "TITLE").getValue();
		final String author = store.traverse(source, "AUTHOR").getValue();
		final String publicationFacts = store.traverse(source, "PUBLICATION_FACTS").getValue();
		final GedcomNode dateNode = store.traverse(source, "DATE");
		//TODO
		final GedcomNode place = store.traverse(source, "PLACE");
		final GedcomNode placeCertainty = store.traverse(source, "PLACE.CERTAINTY");
		final GedcomNode placeCredibility = store.traverse(source, "PLACE.CREDIBILITY");
		final String mediaType = store.traverse(source, "MEDIA_TYPE").getValue();

		eventsPanel.addTag(StringUtils.split(events.toString(), ','));
		titleField.setText(title);
		authorField.setText(author);
		publicationFactsField.setText(publicationFacts);
		final String date = dateNode.getValue();
		final String calendarXRef = store.traverse(dateNode, "CALENDAR").getXRef();
		final String dateOriginalText = store.traverse(dateNode, "ORIGINAL_TEXT").getValue();
		final String dateCredibility = store.traverse(dateNode, "CREDIBILITY").getValue();
		final int dateCredibilityIndex = (dateCredibility != null? Integer.parseInt(dateCredibility): 0);
		datePanel.loadData(date, calendarXRef, dateOriginalText, dateCredibilityIndex);
		placesButton.setEnabled(true);
		repositoriesButton.setEnabled(true);
		documentsButton.setEnabled(true);
		sourcesButton.setEnabled(true);
		notesButton.setEnabled(true);
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
		final GedcomNode source = store.getSources().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand) throws IOException{
					switch(editCommand.getType()){
						case DOCUMENT_CITATION -> {
							final DocumentStructureDialog dialog = new DocumentStructureDialog(store, parent);
							dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(450, 650);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE_CITATION -> {
							final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
							dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(450, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteCitation(store, parent);
							dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(450, 260);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE -> {
							final NoteRecordDialog dialog = NoteRecordDialog.createNote(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle("Note for " + note.getID());
							dialog.loadData(note, editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final SourceRecordDialog dialog = new SourceRecordDialog(store, parent);
			dialog.loadData(source, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 650);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
