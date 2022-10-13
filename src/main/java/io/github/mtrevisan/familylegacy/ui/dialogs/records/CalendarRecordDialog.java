/**
 * Copyright (c) 2022 Mauro Trevisan
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
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.CulturalNormCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;


public class CalendarRecordDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 4728999064397477461L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private static final DefaultComboBoxModel<String> TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist",
		"french-republican", "coptic", "soviet eternal", "ethiopian", "mayan"});

	private static final ImageIcon ICON_CULTURAL_NORM = ResourceHelper.getImage("/images/culturalNorm.png", 20, 20);
	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);
	private static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png", 20, 20);

/*
  +1 CREATION_DATE    {1:1}
    +2 DATE <CREATION_DATE>    {1:1}
  +1 CHANGE_DATE    {0:M}
    +2 DATE <CHANGE_DATE>    {1:1}
    +2 NOTE @<XREF:NOTE>@    {0:1}
*/
	//TODO mandatory
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(TYPE_MODEL);
	//TODO 0 to M
	private final JButton culturalNormButton = new JButton(ICON_CULTURAL_NORM);
	//TODO 0 to M
	private final JButton noteButton = new JButton(ICON_NOTE);
	//TODO 0 to M
	private final JButton sourceButton = new JButton(ICON_SOURCE);
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode calendar;
	private long originalCulturalNormsHash;
	private long originalNotesHash;
	private long originalSourcesHash;

	private Consumer<Object> onAccept;
	private final Flef store;


	public CalendarRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		typeLabel.setLabelFor(typeComboBox);
		AutoCompleteDecorator.decorate(typeComboBox);

		final Border originalButtonBorder = okButton.getBorder();
		noteButton.setToolTipText("Add cultural norm");
		culturalNormButton.addActionListener(evt -> {
			final Consumer<Object> onAccept = ignored -> {
				culturalNormButton.setBorder(calculateCulturalNormsHashCode() != originalCulturalNormsHash
					? new LineBorder(Color.BLUE)
					: originalButtonBorder);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM_CITATION, calendar, onAccept));
		});

		noteButton.setToolTipText("Add note");
		noteButton.addActionListener(evt -> {
			final Consumer<Object> onAccept = ignored -> {
				noteButton.setBorder(calculateNotesHashCode() != originalNotesHash
					? new LineBorder(Color.BLUE)
					: originalButtonBorder);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, calendar, onAccept));
		});

		sourceButton.setToolTipText("Add source");
		sourceButton.addActionListener(evt -> {
			final Consumer<Object> onAccept = ignored -> {
				sourceButton.setBorder(calculateSourcesHashCode() != originalSourcesHash
					? new LineBorder(Color.BLUE)
					: originalButtonBorder);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, calendar, onAccept));
		});

		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> setVisible(false);
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);


		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
//		setLayout(new MigLayout("debug", "[grow]"));
		add(typeLabel, "align label,split 2");
		add(typeComboBox, "grow,wrap");
		add(culturalNormButton, "split 3,sizegroup button2,center");
		add(noteButton, "sizegroup button2,center");
		add(sourceButton, "sizegroup button2,center,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private int calculateCulturalNormsHashCode(){
		return store.traverseAsList(calendar, "CULTURAL_NORM[]").hashCode();
	}

	private int calculateNotesHashCode(){
		return store.traverseAsList(calendar, "NOTE[]").hashCode();
	}

	private int calculateSourcesHashCode(){
		return store.traverseAsList(calendar, "SOURCE[]").hashCode();
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
		final String type = (String)typeComboBox.getSelectedItem();

		calendar.replaceChildValue("TYPE", type);
		//TODO
	}

	public final void loadData(final GedcomNode calendar, final Consumer<Object> onAccept){
		this.calendar = calendar;
		this.onAccept = onAccept;

		originalCulturalNormsHash = calculateCulturalNormsHashCode();
		originalNotesHash = calculateNotesHashCode();
		originalSourcesHash = calculateSourcesHashCode();

		final StringJoiner events = new StringJoiner(", ");
		for(final GedcomNode event : store.traverseAsList(calendar, "EVENT[]"))
			events.add(event.getValue());
		final String type = store.traverse(calendar, "TYPE").getValue();
		final String author = store.traverse(calendar, "AUTHOR").getValue();
		final String publicationFacts = store.traverse(calendar, "PUBLICATION_FACTS").getValue();
		final GedcomNode dateNode = store.traverse(calendar, "DATE");
		//TODO
		final GedcomNode place = store.traverse(calendar, "PLACE");
		final GedcomNode placeCertainty = store.traverse(calendar, "PLACE.CERTAINTY");
		final GedcomNode placeCredibility = store.traverse(calendar, "PLACE.CREDIBILITY");
		final String mediaType = store.traverse(calendar, "MEDIA_TYPE").getValue();

		typeComboBox.setSelectedItem(type);
		sourceButton.setEnabled(true);
		noteButton.setEnabled(true);
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
		final GedcomNode calendar = store.getCalendars().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand) throws IOException{
					switch(editCommand.getType()){
						case CULTURAL_NORM_CITATION -> {
							final CulturalNormCitationDialog dialog = new CulturalNormCitationDialog(store, parent);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								//show a cultural norm input dialog
								dialog.addAction();

							dialog.setSize(450, 260);
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
						case SOURCE_CITATION -> {
							final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								dialog.addAction();

							dialog.setSize(450, 650);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final CalendarRecordDialog dialog = new CalendarRecordDialog(store, parent);
			dialog.setTitle(calendar.getID() != null? "Calendar " + calendar.getID(): "New Calendar");
			dialog.loadData(calendar, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(300, 155);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
