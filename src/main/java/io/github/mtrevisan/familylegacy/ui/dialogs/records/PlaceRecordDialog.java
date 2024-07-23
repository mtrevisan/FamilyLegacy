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

import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.SourceDialog;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
	+1 CREATION    {1:1}
		+2 DATE <CREATION_DATE>    {1:1}
	+1 UPDATE    {0:M}
		+2 DATE <UPDATE_DATE>    {1:1}
		+2 NOTE @<XREF:NOTE>@    {0:1}
*/
public class PlaceRecordDialog extends JDialog implements ActionListener{

	@Serial
	private static final long serialVersionUID = 2060676490438789694L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

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
	private final JComboBox<String> certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());
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


	void initComponents(){
		setTitle("Place");

		GUIHelper.bindLabelTextChangeUndo(nameLabel, nameField, this::dataChanged);

		GUIHelper.bindLabelTextChangeUndo(addressLabel, addressField, this::dataChanged);

		GUIHelper.bindLabelTextChangeUndo(addressHierarchyLabel, addressHierarchyField, this::dataChanged);

		culturalNormButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM, place)));

		noteButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, place)));

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

		GUIHelper.bindLabelTextChangeUndo(latitudeLabel, latitudeField, this::dataChanged);

		GUIHelper.bindLabelTextChangeUndo(longitudeLabel, longitudeField, this::dataChanged);

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

	public boolean loadData(final GedcomNode place, final Consumer<Object> onCloseGracefully){
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

		return false;
	}

	@Override
	public void actionPerformed(final ActionEvent evt){
		dispose();
	}

	public final void showNewRecord(){
		//TODO
//		newAction();
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
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case SOURCE_CITATION -> {
							final SourceDialog dialog = new SourceDialog(store, parent);
							dialog.setTitle(place.getID() != null
								? "Source citations for place " + place.getID()
								: "Source citations for new place");
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(946, 396);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
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
