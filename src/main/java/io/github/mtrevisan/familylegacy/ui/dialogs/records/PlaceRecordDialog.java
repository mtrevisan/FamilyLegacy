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
import io.github.mtrevisan.familylegacy.ui.utilities.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.function.Consumer;


//TODO
/*
	+1 NAME <PLACE_NAME>    {0:1}
		+2 <<TRANSCRIBED_TEXT>>    {0:M}
	+1 ADDRESS <ADDRESS_LINE>    {0:M}
		+2 <<TRANSCRIBED_TEXT>>    {0:M}
		+2 HIERARCHY <ADDRESS_HIERARCHY>    {0:1}
		+2 CULTURAL_NORM @<XREF:RULE>@    {0:M}
		+2 NOTE @<XREF:NOTE>@    {0:M}
		+2 <<SOURCE_CITATION>>    {0:M}
	+1 MAP    {0:1}
		+2 LATITUDE <PLACE_LATITUDE>    {1:1}
		+2 LONGITUDE <PLACE_LONGITUDE>    {1:1}
		+2 CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
		+2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
	+1 SUBORDINATE @<XREF:PLACE>@    {0:1}
	+1 CREATION_DATE    {1:1}
		+2 DATE <CREATION_DATE>    {1:1}
	+1 CHANGE_DATE    {0:M}
		+2 DATE <CHANGE_DATE>    {1:1}
		+2 NOTE @<XREF:NOTE>@    {0:1}
*/
public class PlaceRecordDialog extends JDialog implements ActionListener{

	@Serial
	private static final long serialVersionUID = 2060676490438789694L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private static final DefaultComboBoxModel<String> CERTAINTY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Challenged",
		"Disproven",
		"Proven"});
	private static final CredibilityComboBoxModel CREDIBILITY_MODEL = new CredibilityComboBoxModel();

	private final JLabel nameLabel = new JLabel("Name:");
	private final JTextField nameField = new JTextField();
	private final JLabel addressLabel = new JLabel("Address:");
	private final JTextField addressField = new JTextField();
	private final JLabel addressHierarchyLabel = new JLabel("Hierarchy:");
	private final JTextField addressHierarchyField = new JTextField();
	private final JButton culturalNormButton = new JButton("Cultural norms");
	private final JButton noteButton = new JButton("Notes");
	private final JButton sourceButton = new JButton("Sources");
	private final JLabel latitudeLabel = new JLabel("Latitude:");
	private final JTextField latitudeField = new JTextField();
	private final JLabel longitudeLabel = new JLabel("Longitude:");
	private final JTextField longitudeField = new JTextField();
	private final JLabel certaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> certaintyComboBox = new JComboBox<>(CERTAINTY_MODEL);
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final JLabel subordinateLabel = new JLabel("Subordinate to:");
	//TODO
private final JTextField subordinateField = new JTextField();
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode place;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public PlaceRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		setTitle("Place");

		GUIHelper.bindLabelTextChangeUndo(nameLabel, nameField, evt -> dataChanged());

		GUIHelper.bindLabelTextChangeUndo(addressLabel, addressField, evt -> dataChanged());

		GUIHelper.bindLabelTextChangeUndo(addressHierarchyLabel, addressHierarchyField, evt -> dataChanged());

		culturalNormButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM_CITATION, place)));

		noteButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, place)));

		sourceButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, place)));

		final JPanel addressPanel = new JPanel();
		addressPanel.setBorder(BorderFactory.createTitledBorder("Address"));
		addressPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		addressPanel.add(addressLabel, "align label,split 2,sizegroup labelAddress");
		addressPanel.add(addressField, "grow,wrap");
		addressPanel.add(addressHierarchyLabel, "align label,split 2,sizegroup labelAddress");
		addressPanel.add(addressHierarchyField, "grow,wrap");
		addressPanel.add(culturalNormButton, "grow,wrap");
		addressPanel.add(noteButton, "grow,wrap");
		addressPanel.add(sourceButton, "grow");

		GUIHelper.bindLabelTextChangeUndo(latitudeLabel, latitudeField, evt -> dataChanged());

		GUIHelper.bindLabelTextChangeUndo(longitudeLabel, longitudeField, evt -> dataChanged());

		certaintyLabel.setLabelFor(certaintyComboBox);

		credibilityLabel.setLabelFor(credibilityComboBox);

		final JPanel mapPanel = new JPanel();
		mapPanel.setBorder(BorderFactory.createTitledBorder("Coordinates"));
		mapPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		mapPanel.add(latitudeLabel, "align label,split 2,sizegroup labelMap");
		mapPanel.add(latitudeField, "grow,wrap");
		mapPanel.add(longitudeLabel, "align label,split 2,sizegroup labelMap");
		mapPanel.add(longitudeField, "grow,wrap");
		mapPanel.add(certaintyLabel, "align label,split 2,sizegroup labelMap");
		mapPanel.add(certaintyComboBox, "wrap");
		mapPanel.add(credibilityLabel, "align label,split 2,sizegroup labelMap");
		mapPanel.add(credibilityComboBox);

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			//TODO remember, when saving the whole gedcom, to remove all non-referenced places!

			dispose();
		});
		getRootPane().registerKeyboardAction(this, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
		cancelButton.addActionListener(this);


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(nameLabel, "align label,sizegroup label,split 2");
		add(nameField, "grow,wrap");
		add(addressPanel, "grow,wrap");
		add(mapPanel, "grow,wrap");
		add(subordinateLabel, "align label,sizegroup label,split 2");
		add(subordinateField, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	public void dataChanged(){
		//TODO
	}

	private void okAction(){
		//TODO
		System.out.println();
	}

	public void loadData(final GedcomNode place, final Consumer<Object> onCloseGracefully){
		this.place = place;
		this.onCloseGracefully = onCloseGracefully;

		//TODO
		final String id = place.getID();
		setTitle(id != null? "Place " + id: "New Place");

		final String name = store.traverse(place, "NAME").getValue();
		final GedcomNode addressNode = store.traverse(place, "ADDRESS");
		final String address = addressNode.getValue();
		final String addressHierarchy = store.traverse(addressNode, "HIERARCHY").getValue();

		nameField.setText(name);
		addressField.setText(address);
		addressHierarchyField.setText(addressHierarchy);

		repaint();
	}

	@Override
	public void actionPerformed(final ActionEvent evt){
		dispose();
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
		final GedcomNode place = store.getPlaces().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case SOURCE_CITATION -> {
							final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								dialog.addAction();

							dialog.setSize(450, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE -> {
							final SourceRecordDialog dialog = new SourceRecordDialog(store, parent);
							dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(500, 540);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteCitation(store, parent);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								//show a note input dialog
								dialog.addAction();

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

			final PlaceRecordDialog dialog = new PlaceRecordDialog(store, parent);
			dialog.loadData(place, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 470);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
