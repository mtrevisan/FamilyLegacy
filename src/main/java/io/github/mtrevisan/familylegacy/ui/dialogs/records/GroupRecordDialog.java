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
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.SourceDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
	private final JButton eventButton = new JButton("Events");
	private final JButton noteButton = new JButton("Notes");
	private final JButton sourceButton = new JButton("Sources");
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

		eventButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.EVENT_CITATION, group)));

		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, group)));

		sourceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, group)));

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
		add(eventButton, "sizegroup button,grow,wrap");
		add(noteButton, "sizegroup button,grow,wrap");
		add(sourceButton, "sizegroup button,grow,wrap paragraph");
		add(okButton, "tag ok,span,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
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
		store.load("/gedg/flef_0.0.8.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode group = store.getGroups().get(0);


		final JFrame parent = new JFrame();
		EventQueue.invokeLater(() -> {
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case EVENT -> {
							//TODO
//							final EventDialog dialog = new EventDialog(store, parent);
//							dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
//
//							dialog.setSize(550, 250);
//							dialog.setLocationRelativeTo(parent);
//							dialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog dialog = NoteDialog.createNote(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle("Note for " + note.getID());
							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(500, 330);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE -> {
							final SourceDialog dialog = new SourceDialog(store, parent);
							dialog.setTitle(group.getID() != null
								? "Source for group " + group.getID()
								: "Source for new group");
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(946, 396);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
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
