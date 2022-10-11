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
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.function.Consumer;


//TODO
public class GroupRecordDialog extends JDialog{

	private final JLabel nameLabel = new JLabel("Name:");
	private final JTextField nameField = new JTextField();
	private final JLabel typeLabel = new JLabel("Type:");
	private final JTextField typeField = new JTextField();
	private final JButton eventsButton = new JButton("Events");
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode group;
	private int groupHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public GroupRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		setTitle("Group");

		nameLabel.setLabelFor(nameField);

		nameField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent event){
				okButton.setEnabled(!nameField.getText().isBlank());
			}
		});

		typeLabel.setLabelFor(typeField);

		eventsButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.EVENT_CITATION, group)));

		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, group)));

		sourcesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, group)));

		okButton.addActionListener(evt -> {
			final String name = nameField.getText();
			final String type = typeField.getText();

			group.replaceChildValue("NAME", name);
			group.replaceChildValue("TYPE", type);

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(nameLabel, "align label,split 2");
		add(nameField, "grow,wrap");
		add(typeLabel, "align label,split 2");
		add(typeField, "grow,wrap paragraph");
		add(eventsButton, "sizegroup button2,grow,wrap");
		add(notesButton, "sizegroup button2,grow,wrap");
		add(sourcesButton, "sizegroup button2,grow,wrap paragraph");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void loadData(final GedcomNode group, final Consumer<Object> onCloseGracefully){
		this.group = group;
		this.onCloseGracefully = onCloseGracefully;

		final String id = group.getID();
		setTitle(id != null? "Group " + id: "New Group");

		final String name = store.traverse(group, "NAME").getValue();
		final String type = store.traverse(group, "TYPE").getValue();

		final int nameHash = Objects.requireNonNullElse(name, StringUtils.EMPTY).hashCode();
		final int typeHash = Objects.requireNonNullElse(type, StringUtils.EMPTY).hashCode();
		groupHash = nameHash ^ typeHash;

		nameField.setText(name);
		typeField.setText(type);

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
		final GedcomNode group = store.getGroups().get(0);


		final JFrame parent = new JFrame();
		EventQueue.invokeLater(() -> {
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand){
					JDialog dialog = null;
					switch(editCommand.getType()){
						case EVENT:
							//TODO
//							dialog = new EventDialog(store, parent);
//							((EventDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
//
//							dialog.setSize(550, 250);
							break;

						case NOTE_CITATION:
							dialog = NoteCitationDialog.createNoteCitation(store, parent);
							((NoteCitationDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(450, 260);
							break;

						case NOTE:
							dialog = NoteRecordDialog.createNote(store, parent);
							final GedcomNode note = editCommand.getContainer();
							((NoteRecordDialog)dialog).setTitle("Note for " + note.getID());
							((NoteRecordDialog)dialog).loadData(note, editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
							break;

						case SOURCE_CITATION:
							//TODO
							dialog = new SourceCitationDialog(store, parent);
							((SourceCitationDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(450, 450);

						case SOURCE:
							//TODO
							dialog = new SourceRecordDialog(store, parent);
							((SourceRecordDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(500, 460);
					}
					if(dialog != null){
						dialog.setLocationRelativeTo(parent);
						dialog.setVisible(true);
					}
				}
			};
			EventBusService.subscribe(listener);

			final GroupRecordDialog dialog = new GroupRecordDialog(store, parent);
			dialog.loadData(group, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(300, 250);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
