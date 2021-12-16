/**
 * Copyright (c) 2020 Mauro Trevisan
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
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.ContactStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;


//TODO
public class RepositoryRecordDialog extends JDialog{

	private final JLabel nameLabel = new JLabel("Name:");
	private final JTextField nameField = new JTextField();
	private final JButton individualButton = new JButton("Individual");
	private final JButton placeButton = new JButton("Place");
	private final JButton contactsButton = new JButton("Contacts");
	private final JButton notesButton = new JButton("Notes");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode repository;
	private Runnable onCloseGracefully;
	private final Flef store;


	public RepositoryRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		setTitle("Repository");

		nameLabel.setLabelFor(nameField);

		individualButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.INDIVIDUAL_CITATION, repository)));

		placeButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE_CITATION, repository)));

		contactsButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.CONTACT, repository)));

		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, repository)));

		okButton.addActionListener(evt -> {
			final String name = nameField.getText();
//			final String title = titleField.getText();
//			final String extractLanguageTag = extractLocaleComboBox.getSelectedLanguageTag();

			repository.replaceChildValue("NAME", name);
//			repository.replaceChildValue("TITLE", title);
//			final GedcomNode extractLocaleNode = store.traverse(repository, "EXTRACT.LOCALE");
//			if(!extractLocaleNode.isEmpty())
//				extractLocaleNode.withValue(extractLanguageTag);

			if(onCloseGracefully != null)
				onCloseGracefully.run();

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());


		setLayout(new MigLayout("", "[grow]"));
		add(nameLabel, "align label,split 2");
		add(nameField, "grow,wrap paragraph");
		add(individualButton, "sizegroup button2,grow,wrap");
		add(placeButton, "sizegroup button2,grow,wrap");
		add(contactsButton, "sizegroup button2,grow,wrap");
		add(notesButton, "sizegroup button2,grow,wrap paragraph");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void loadData(final GedcomNode repository, final Runnable onCloseGracefully){
		this.repository = repository;
		this.onCloseGracefully = onCloseGracefully;

		setTitle("Repository " + repository.getID());

		final String name = store.traverse(repository, "NAME").getValue();
		final String title = store.traverse(repository, "TITLE").getValue();
		final String author = store.traverse(repository, "AUTHOR").getValue();
		final String publicationFacts = store.traverse(repository, "PUBLICATION_FACTS").getValue();
		final GedcomNode dateNode = store.traverse(repository, "DATE");
		final GedcomNode place = store.traverse(repository, "PLACE");
		final GedcomNode placeCertainty = store.traverse(repository, "PLACE.CERTAINTY");
		final GedcomNode placeCredibility = store.traverse(repository, "PLACE.CREDIBILITY");
		final boolean hasRepositories = !store.traverseAsList(repository, "REPOSITORY[]").isEmpty();
		final String mediaType = store.traverse(repository, "MEDIA_TYPE").getValue();
		final boolean hasFiles = !store.traverseAsList(repository, "FILE[]").isEmpty();
		final boolean hasNotes = !store.traverseAsList(repository, "NOTE[]").isEmpty();

		nameField.setText(name);
//		titleField.setText(title);
//		authorField.setText(author);
//		publicationFactsField.setText(publicationFacts);

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
		final GedcomNode repository = store.getRepositories().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand) throws IOException{
					JDialog dialog = null;
					switch(editCommand.getType()){
						case PLACE -> {
							dialog = new PlaceRecordDialog(store, parent);
							((PlaceRecordDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							dialog.setSize(350, 430);
						}
						case CONTACT -> {
							dialog = new ContactStructureDialog(store, parent);
							((ContactStructureDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							dialog.setSize(350, 430);
						}
						case NOTE_CITATION -> {
							dialog = new NoteCitationDialog(store, parent);
							((NoteCitationDialog)dialog).loadData(editCommand.getContainer());
							dialog.setSize(450, 260);
						}
						case NOTE -> {
							dialog = new NoteRecordDialog(store, parent);
							((NoteRecordDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							dialog.setSize(550, 350);
						}
					}
					if(dialog != null){
						dialog.setLocationRelativeTo(parent);
						dialog.setVisible(true);
					}
				}
			};
			EventBusService.subscribe(listener);

			final RepositoryRecordDialog dialog = new RepositoryRecordDialog(store, parent);
			dialog.loadData(repository, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(320, 250);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}