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
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.DocumentStructureDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
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


public class CalendarRecordDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 4728999064397477461L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

/*
  +1 CREATION_DATE    {1:1}
    +2 DATE <CREATION_DATE>    {1:1}
  +1 CHANGE_DATE    {0:M}
    +2 DATE <CHANGE_DATE>    {1:1}
    +2 NOTE @<XREF:NOTE>@    {0:1}
*/
	//TODO mandatory
	private final JLabel typeLabel = new JLabel("Type:");
	private final JTextField typeField = new JTextField();
	//TODO 0 or 1
	private final JButton descriptionButton = new JButton("Description");
	//TODO 0 to M
	private final JButton culturalRulesButton = new JButton("Cultural rules");
	//TODO 0 to M
	private final JButton notesButton = new JButton("Notes");
	//TODO 0 to M
	private final JButton sourcesButton = new JButton("Sources");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode calendar;

	private Consumer<Object> onAccept;
	private final Flef store;


	public CalendarRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		typeLabel.setLabelFor(typeField);

		descriptionButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, calendar)));
		culturalRulesButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_RULE_CITATION, calendar)));

		sourcesButton.setEnabled(false);
		sourcesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, calendar)));

		notesButton.setEnabled(false);
		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, calendar)));

		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> setVisible(false);
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(typeLabel, "align label,split 2");
		add(typeField, "grow,wrap");
		add(descriptionButton, "grow,wrap");
		add(culturalRulesButton, "grow,wrap");
		add(notesButton, "sizegroup button2,grow,wrap paragraph");
		add(sourcesButton, "sizegroup button2,grow,wrap");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private boolean sourceContainsEvent(final String event){
		boolean containsEvent = false;
		final List<GedcomNode> events = store.traverseAsList(calendar, "EVENT[]");
		for(int i = 0; !containsEvent && i < events.size(); i ++)
			if(events.get(i).getValue().equalsIgnoreCase(event))
				containsEvent = true;
		return containsEvent;
	}

	private void okAction(){
		final String title = typeField.getText();

		calendar.replaceChildValue("TITLE", title);
		//TODO
	}

	public final void loadData(final GedcomNode calendar, final Consumer<Object> onAccept){
		this.calendar = calendar;
		this.onAccept = onAccept;

		final String id = calendar.getID();
		setTitle(id != null? "Calendar " + id: "New Calendar");

		final StringJoiner events = new StringJoiner(", ");
		for(final GedcomNode event : store.traverseAsList(calendar, "EVENT[]"))
			events.add(event.getValue());
		final String title = store.traverse(calendar, "TITLE").getValue();
		final String author = store.traverse(calendar, "AUTHOR").getValue();
		final String publicationFacts = store.traverse(calendar, "PUBLICATION_FACTS").getValue();
		final GedcomNode dateNode = store.traverse(calendar, "DATE");
		//TODO
		final GedcomNode place = store.traverse(calendar, "PLACE");
		final GedcomNode placeCertainty = store.traverse(calendar, "PLACE.CERTAINTY");
		final GedcomNode placeCredibility = store.traverse(calendar, "PLACE.CREDIBILITY");
		final String mediaType = store.traverse(calendar, "MEDIA_TYPE").getValue();

		typeField.setText(title);
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
		final GedcomNode calendar = store.getCalendars().get(0);

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

			final CalendarRecordDialog dialog = new CalendarRecordDialog(store, parent);
			dialog.loadData(calendar, null);

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
