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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;


//TODO
public class PlaceDialog extends JDialog implements ActionListener{

	private static final long serialVersionUID = 2060676490438789694L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private static final DefaultComboBoxModel<String> CERTAINTY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Challenged",
		"Disproven",
		"Proven"});
	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

	private final JLabel nameLabel = new JLabel("Name:");
	private final JTextField nameField = new JTextField();
	private final JLabel addressLabel = new JLabel("Address:");
	private final JTextField addressField = new JTextField();
	private final JLabel addressHierarchyLabel = new JLabel("Hierarchy:");
	private final JTextField addressHierarchyField = new JTextField();
	private final JButton culturalRulesButton = new JButton("Cultural rules");
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JLabel latitudeLabel = new JLabel("Latitude:");
	private final JTextField latitudeField = new JTextField();
	private final JLabel longitudeLabel = new JLabel("Longitude:");
	private final JTextField longitudeField = new JTextField();
	private final JLabel certaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> certaintyComboBox = new JComboBox<>(CERTAINTY_MODEL);
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	//TODO
	private final JLabel subordinateLabel = new JLabel("Subordinate to:");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode place;
	private volatile boolean updating;
	private int dataHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public PlaceDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		setTitle("Place");

		GUIHelper.bindLabelTextChangeUndo(nameLabel, nameField, evt -> dataChanged());

		GUIHelper.bindLabelTextChangeUndo(addressLabel, addressField, evt -> dataChanged());

		GUIHelper.bindLabelTextChangeUndo(addressHierarchyLabel, addressHierarchyField, evt -> dataChanged());

		culturalRulesButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, place)));

		notesButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, place)));

		sourcesButton.addActionListener(evt -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, place)));

		final JPanel addressPanel = new JPanel();
		addressPanel.setBorder(BorderFactory.createTitledBorder("Address"));
		addressPanel.setLayout(new MigLayout("", "[grow]"));
		addressPanel.add(addressLabel, "align label,split 2,sizegroup label");
		addressPanel.add(addressField, "grow,wrap");
		addressPanel.add(addressHierarchyLabel, "align label,split 2,sizegroup label");
		addressPanel.add(addressHierarchyField, "grow,wrap");
		addressPanel.add(culturalRulesButton, "grow,wrap");
		addressPanel.add(notesButton, "grow,wrap");
		addressPanel.add(sourcesButton, "grow");

		latitudeLabel.setLabelFor(latitudeField);
		longitudeLabel.setLabelFor(longitudeField);
		certaintyLabel.setLabelFor(certaintyComboBox);
		credibilityLabel.setLabelFor(credibilityComboBox);

		final JPanel mapPanel = new JPanel();
		mapPanel.setBorder(BorderFactory.createTitledBorder("Coordinates"));
		mapPanel.setLayout(new MigLayout("", "[grow]"));
		mapPanel.add(latitudeLabel, "align label,split 2,sizegroup label");
		mapPanel.add(latitudeField, "grow,wrap");
		mapPanel.add(longitudeLabel, "align label,split 2,sizegroup label");
		mapPanel.add(longitudeField, "grow,wrap");
		mapPanel.add(certaintyLabel, "align label,split 2");
		mapPanel.add(certaintyComboBox, "wrap");
		mapPanel.add(credibilityLabel, "align label,split 2");
		mapPanel.add(credibilityComboBox);

		//TODO

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
		cancelButton.addActionListener(this::actionPerformed);


		setLayout(new MigLayout("", "[grow]"));
		add(nameLabel, "align label,sizegroup label,split 2");
		add(nameField, "grow,wrap");
		add(addressPanel, "grow,wrap");
		add(mapPanel, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void dataChanged(){
		//TODO
		if(!updating)
			okButton.setEnabled(calculateDataHash() != dataHash);
	}

	private int calculateDataHash(){
		//TODO
		final int nameHash = nameField.getText()
			.hashCode();
		final int addressHash = addressField.getText()
			.hashCode();
		final int addressHierarchyHash = addressHierarchyField.getText()
			.hashCode();
		return nameHash ^ addressHash ^ addressHierarchyHash;
	}

	private void okAction(){
		//TODO
	}

	public void loadData(final GedcomNode place, final Consumer<Object> onCloseGracefully){
		updating = true;

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

		updating = false;

		dataHash = calculateDataHash();

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
		store.load("/gedg/flef_0.0.6.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode place = store.getPlaces().get(0);

		EventQueue.invokeLater(() -> {
			final PlaceDialog dialog = new PlaceDialog(store, new JFrame());
			dialog.loadData(place, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 600);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
