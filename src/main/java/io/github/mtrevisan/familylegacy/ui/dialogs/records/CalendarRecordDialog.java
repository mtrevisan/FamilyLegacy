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
import io.github.mtrevisan.familylegacy.gedcom.events.DataFormatEvent;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.CulturalNormDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
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
import java.util.List;
import java.util.function.Consumer;


public final class CalendarRecordDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 4728999064397477461L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	private static final DefaultComboBoxModel<String> TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist",
		"french-republican", "coptic", "soviet eternal", "ethiopian", "mayan"});

	private static final ImageIcon ICON_CULTURAL_NORM = ResourceHelper.getImage("/images/culturalNorm.png", 20, 20);
	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);
	private static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png", 20, 20);

	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(TYPE_MODEL);
	private final JButton culturalNormButton = new JButton(ICON_CULTURAL_NORM);
	private final JButton noteButton = new JButton(ICON_NOTE);
	private final JButton sourceButton = new JButton(ICON_SOURCE);
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
		typeLabel.setLabelFor(typeComboBox);
		//read all calendar types and add the custom ones to the model
		final DefaultComboBoxModel<String> calendarModel = (DefaultComboBoxModel<String>)typeComboBox.getModel();
		final List<GedcomNode> calendars = store.getCalendars();
		for(final GedcomNode calendar : calendars){
			final String type = store.traverse(calendar, "TYPE").getValue();
			if(calendarModel.getIndexOf(type) < 0)
				calendarModel.addElement(type);
		}
		typeComboBox.setEditable(true);
		final ActionListener addTypeAction = event -> {
			final int index = typeComboBox.getSelectedIndex();
			if(index < 0 && "comboBoxEdited".equals(event.getActionCommand())){
				final String newType = (String)calendarModel.getSelectedItem();
				if(StringUtils.isNotBlank(newType)){
					calendarModel.addElement(newType);
					typeComboBox.setSelectedItem(newType);
				}
			}
		};
		typeComboBox.addActionListener(addTypeAction);

		culturalNormButton.setToolTipText("Add cultural norm");
		final ActionListener addCulturalNormAction = evt -> {
			final GedcomNode newCulturalNorm = store.create("CULTURAL_NORM");

			final Consumer<Object> onCloseGracefully = ignored -> {
				//if ok was pressed, add this note to the parent container
				final String newCulturalNormID = store.addCulturalNorm(newCulturalNorm);
				calendar.addChildReference("CULTURAL_NORM", newCulturalNormID);

				culturalNormButton.setBorder(store.traverseAsList(calendar, "CULTURAL_NORM[]").isEmpty()
					? UIManager.getBorder("Button.border")
					: new LineBorder(Color.BLUE));

				//put focus on the ok button
				okButton.grabFocus();
			};

			//fire add event
			EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM, calendar, onCloseGracefully));
		};
		culturalNormButton.addActionListener(addCulturalNormAction);

		noteButton.setToolTipText("Add note");
		final ActionListener addNoteAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				noteButton.setBorder(store.traverseAsList(calendar, "NOTE[]").isEmpty()
					? UIManager.getBorder("Button.border")
					: new LineBorder(Color.BLUE));

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, calendar, onAccept));
		};
		noteButton.addActionListener(addNoteAction);

		sourceButton.setToolTipText("Add source");
		final ActionListener addSourceAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				sourceButton.setBorder(store.traverseAsList(calendar, "SOURCE[]").isEmpty()
					? UIManager.getBorder("Button.border")
					: new LineBorder(Color.BLUE));

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, calendar, onAccept));
		};
		sourceButton.addActionListener(addSourceAction);

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
		add(typeComboBox, "grow,wrap");
		add(culturalNormButton, "split 3,sizegroup button,center");
		add(noteButton, "sizegroup button,center");
		add(sourceButton, "sizegroup button,center,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
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
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());

		final String type = (String)typeComboBox.getSelectedItem();
		final Consumer<Object> onCloseGracefully = parent -> setVisible(false);
		if(StringUtils.isBlank(type))
			EventBusService.publish(new DataFormatEvent(DataFormatEvent.DataFormatErrorType.MANDATORY_FIELD_MISSING, "type", calendar, onCloseGracefully));

		final List<GedcomNode> culturalNorms = store.traverseAsList(calendar, "CULTURAL_NORM[]");
		final List<GedcomNode> notes = store.traverseAsList(calendar, "NOTE[]");
		final List<GedcomNode> sources = store.traverseAsList(calendar, "SOURCE[]");
		final GedcomNode creation = store.traverse(calendar, "CREATION");

		if(creation.isEmpty()){
			calendar.clearAll();
			calendar
				.addChildValue("TYPE", type)
				.addChildren(culturalNorms)
				.addChildren(notes)
				.addChildren(sources)
				.addChild(
					store.create("CREATION")
						.addChildValue("DATE", now)
				);

			if(onAccept != null)
				onAccept.accept(this);

			setVisible(false);
		}
		else{
			//show note record dialog
			final GedcomNode changeNote = store.create("NOTE");
			final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
			changeNoteDialog.setTitle("Change note for calendar " + calendar.getID());
			changeNoteDialog.loadData(changeNote, ignored -> {
				final List<GedcomNode> update = store.traverseAsList(calendar, "UPDATE[]");
				calendar.clearAll();
				calendar
					.addChildValue("TYPE", type)
					.addChildren(culturalNorms)
					.addChildren(notes)
					.addChildren(sources)
					.addChild(creation)
					.addChildren(update)
					.addChild(
						store.create("UPDATE")
							.addChildValue("DATE", now)
							.addChildValue("NOTE", NoteDialog.fromNoteText(changeNote.getValue()))
					);

				if(onAccept != null)
					onAccept.accept(this);

				setVisible(false);
			});

			changeNoteDialog.setSize(450, 500);
			changeNoteDialog.setLocationRelativeTo(this);
			changeNoteDialog.setVisible(true);
		}
	}

	public void loadData(final GedcomNode calendar, final Consumer<Object> onAccept){
		this.calendar = calendar;
		this.onAccept = onAccept;

		final String type = store.traverse(calendar, "TYPE").getValue();

		typeComboBox.setSelectedItem(type);
		culturalNormButton.setEnabled(true);
		culturalNormButton.setBorder(store.traverseAsList(calendar, "CULTURAL_NORM[]").isEmpty()
			? UIManager.getBorder("Button.border")
			: new LineBorder(Color.BLUE));
		noteButton.setEnabled(true);
		noteButton.setBorder(store.traverseAsList(calendar, "NOTE[]").isEmpty()
			? UIManager.getBorder("Button.border")
			: new LineBorder(Color.BLUE));
		sourceButton.setEnabled(true);
		sourceButton.setBorder(store.traverseAsList(calendar, "SOURCE[]").isEmpty()
			? UIManager.getBorder("Button.border")
			: new LineBorder(Color.BLUE));
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
				public void error(final DataFormatEvent dataFormatEvent){
					final DataFormatEvent.DataFormatErrorType type = dataFormatEvent.getType();
					final String reference = dataFormatEvent.getReference();
					JOptionPane.showMessageDialog(parent, type + " error on field " + reference, "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					final String forCalendar = (calendar.getID() != null? " for calendar " + calendar.getID(): " for new calendar");
					switch(editCommand.getType()){
						case CULTURAL_NORM -> {
							final CulturalNormDialog dialog = new CulturalNormDialog(store, parent);
							dialog.setTitle("Cultural norm" + forCalendar);
							final GedcomNode culturalNorm = editCommand.getContainer();
							dialog.setTitle(culturalNorm.getID() != null
								? "Cultural norm " + culturalNorm.getID() + forCalendar
								: "New cultural norm" + forCalendar);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(480, 700);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog dialog = NoteDialog.createNote(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Note " + note.getID() + forCalendar
								: "New note" + forCalendar);
							dialog.loadData(note, editCommand.getOnCloseGracefully());

							dialog.setSize(500, 330);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE -> {
							final SourceRecordDialog dialog = new SourceRecordDialog(store, parent);
							final GedcomNode source = editCommand.getContainer();
							dialog.setTitle(source.getID() != null
								? "Source " + source.getID() + forCalendar
								: "New source" + forCalendar);
							dialog.loadData(source, editCommand.getOnCloseGracefully());

							dialog.setSize(500, 650);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE_CITATION -> {
							final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
							dialog.setTitle("Source citations" + forCalendar);
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								dialog.addAction();

							dialog.setSize(550, 650);
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
