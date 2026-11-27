/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.components.quickentry;

import io.github.mtrevisan.calendars.spi.DateInterpretation;
import io.github.mtrevisan.familylegacy.dossier.EditorTab;
import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthand;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthandReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quickentry.DateShorthandChecker;
import io.github.mtrevisan.familylegacy.quickentry.DateShorthandDescriber;
import io.github.mtrevisan.familylegacy.quickentry.DateStructureBuilder;
import io.github.mtrevisan.familylegacy.quickentry.LifeFactUpdater;
import io.github.mtrevisan.familylegacy.quickentry.PlaceChain;
import io.github.mtrevisan.familylegacy.quickentry.PlaceLookup;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.HintTextField;
import io.github.mtrevisan.familylegacy.ui.helpers.PickMenu;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * The date and the place of a fact of the life of a person (the birth, the death), as a row of a grid: the title, the date, the place,
 * and under them what the date means (the cells are added to the grid of the panel that holds the rows, so that the rows are aligned
 * with its other fields: see {@link #addTo}).
 * <p>
 * The date is typed in the short form of the date editor (see {@link DateShorthandReader}), with what it means shown under it; the place
 * is a name, or one of the places of the file chosen with the button. The records are not written here: the panel only tells what the
 * user meant ({@link #fact()}) once {@link #prepare()} has checked the date and, if the file has places named like the one typed, has
 * asked which one is meant (a name is never matched silently: the same name may be the one of two places).
 * <p>
 * The panel may show a fact that exists already ({@link #load}): then only what the user changes is to be written ({@link #edit()}),
 * and what is cited (a date with a source, a place with a citation) is shown but not editable.
 */
public final class LifeFactRow{

	private static final int LOOKUP_LIMIT = 10;


	/** Asks the user to pick one of the options. */
	@FunctionalInterface
	interface Asker{

		/** @return	The index of the option picked, -1 if the user did not pick any. */
		int choose(Component parent, String message, String title, List<String> options);

	}

	/** Tells the user that something is wrong. */
	@FunctionalInterface
	interface Reporter{

		void report(Component parent, String message);

	}


	private final Window owner;
	private final FLEFModel model;

	private final JLabel titleLabel;
	private final JLabel placeLabel = new JLabel(I18N.t("dialog.quick_entry.place"));
	private final JTextField dateField = new JTextField(14);
	/** The calendar of the date: the Gregorian one unless the user chooses another (the date is written in the notation of its calendar). */
	private final JComboBox<String> calendarCombo = new JComboBox<>(DateReader.CALENDARS);
	private final JLabel datePreview = new JLabel(StringUtils.SPACE);
	private final HintTextField placeField = new HintTextField(24);

	/** The place chosen with the button, and the text it put in the field (the choice holds while the text is the same). */
	private String pickedId;
	private String pickedText;
	/** The place decided by {@link #prepare()}, and the text it was decided for. */
	private QuickEntryService.PlaceRef place = QuickEntryService.PlaceRef.NONE;
	private String placeDecidedFor = StringUtils.EMPTY;
	private FLEFRecord date;

	/** The event shown ({@code null} for a new fact), with the texts it had when it was shown and what the user changed of it. */
	private FLEFRecord event;
	private String initialDate = StringUtils.EMPTY;
	private String initialCalendar = DateReader.ENUM_CALENDAR_GREGORIAN;
	private String initialPlace = StringUtils.EMPTY;
	private boolean dateChanged;
	private boolean placeChanged;
	private final TabLink moreLabel = new TabLink(EditorTab.PARTICIPATIONS);
	private final TabLink lockedLabel = new TabLink(EditorTab.PARTICIPATIONS);

	private Asker asker = (parent, message, title, options) -> options.indexOf(JOptionPane.showInputDialog(parent, message, title,
		JOptionPane.QUESTION_MESSAGE, null, options.toArray(), options.getFirst()));
	private Reporter reporter = (parent, message) -> MessageDialogs.show(parent, message, I18N.t("validation.title"),
		JOptionPane.ERROR_MESSAGE);


	/**
	 * @param owner	The window that owns the dialogs that the panel opens.
	 * @param model	The model (to look for the places).
	 * @param titleKey	The key of the title of the fact (e.g. {@code dialog.quick_entry.birth_date}).
	 */
	public LifeFactRow(final Window owner, final FLEFModel model, final String titleKey){
		this.owner = owner;
		this.model = model;

		titleLabel = new JLabel(I18N.t(titleKey));
		dateField.setToolTipText(I18N.t("dialog.date.quick_entry.tooltip"));
		placeField.setToolTipText(I18N.t("dialog.quick_entry.place.tooltip"));
		//a place of the file is picked with the right button of the mouse, as every reference
		PickMenu.install(placeField, this::pickPlace, null);
		listen(dateField, this::updateDatePreview);
		calendarCombo.setSelectedItem(DateReader.ENUM_CALENDAR_GREGORIAN);
		calendarCombo.setToolTipText(I18N.t("dialog.quick_entry.calendar.tooltip"));
		calendarCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
					final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, I18N.t("enum.date.calendar." + value, String.valueOf(value)), index, isSelected,
					cellHasFocus);
			}
		});
		calendarCombo.addActionListener(e -> updateDatePreview());

		//what the date means takes room only while there is something to say, so that the rows are evenly spaced
		datePreview.setVisible(false);
	}


	/**
	 * Adds the cells of the row to a grid of five columns (label, field, label, field that grows, button), in two rows: the date and the
	 * place, then the meaning of the date (and, if there are other events of the same kind, the note about them) under the date.
	 *
	 * @param grid	The panel, with a {@code MigLayout} of five columns, whose current row is empty.
	 */
	public void addTo(final JPanel grid){
		grid.add(titleLabel);
		//the date and its calendar, in the cell of the date
		final JPanel dateCell = new JPanel(new MigLayout("ins 0", "[]rel[]"));
		dateCell.add(dateField);
		dateCell.add(calendarCombo);
		grid.add(dateCell, "growx 0");
		grid.add(placeLabel);
		grid.add(placeField, "growx,wrap");
		grid.add(datePreview, "skip 1,span 4,wrap");
		grid.add(moreLabel, "skip 1,span 4,wrap");
		grid.add(lockedLabel, "skip 1,span 4,wrap");
	}

	/** Makes the notes that point to another tab ("see the Participations tab") lead there when clicked. */
	public void setNavigator(final TabLink.Navigator navigator){
		moreLabel.setNavigator(navigator);
		lockedLabel.setNavigator(navigator);
	}

	/** @return	The participation of the individual in the event (what the Participations tab lists), {@code null} if there is none. */
	private FLEFRecord participationIn(final FLEFRecord anEvent, final String individualId){
		if(individualId == null)
			return null;
		for(final FLEFRecord record : model.getRecordsByType(EventParticipationReader.TYPE)){
			final EventHelper.Participation participation = EventHelper.toParticipation(record);
			if(anEvent.getId().equals(participation.eventId()) && individualId.equals(participation.participantId()))
				return record;
		}
		return null;
	}


	/**
	 * Shows an event that exists already: its date and its place (the first of the events if there are more, with a note about the
	 * others). A date or a place that is cited is shown but not editable.
	 *
	 * @param events	The events of the fact (nothing is shown if there is none).
	 * @param individualId	The individual the events are of (the notes that point to the Participations tab lead to its participation).
	 */
	public void load(final List<FLEFRecord> events, final String individualId){
		if(events.isEmpty())
			return;

		event = events.getFirst();
		final FLEFRecord dateStructure = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		String dateText = (dateStructure != null? DateReader.extractAssistantFindingIdalText(dateStructure): null);
		if(StringUtils.isBlank(dateText))
			dateText = EventHelper.eventDateRaw(event);
		initialDate = StringUtils.strip(StringUtils.defaultString(dateText));

		final String placeId = EventHelper.eventPlaceId(event);
		final FLEFRecord place = (placeId != null? model.getRecordById(placeId): null);
		if(place != null){
			initialPlace = StringUtils.normalizeSpace(PlaceHandler.getInstance().getDisplayText(place, model));
			pickedId = placeId;
			pickedText = initialPlace;
		}
		else
			initialPlace = StringUtils.normalizeSpace(StringUtils.defaultString(FLEFRecordHelper.getChildValue(
				FLEFRecordHelper.findChild(event, EventReader.TAG_PLACE), PlaceCitationReader.TAG_ASSISTANT_FINDING_IDAL_TEXT)));

		initialCalendar = calendarOf(dateStructure);
		calendarCombo.setSelectedItem(initialCalendar);
		dateField.setText(initialDate);
		placeField.setText(initialPlace);
		if(!LifeFactUpdater.isDateEditable(event)){
			dateField.setEnabled(false);
			calendarCombo.setEnabled(false);
			dateField.setToolTipText(I18N.t("dialog.quick_entry.locked"));
		}
		if(!LifeFactUpdater.isPlaceEditable(event)){
			placeField.setEnabled(false);
			placeField.setToolTipText(I18N.t("dialog.quick_entry.locked"));
		}
		if(!dateField.isEnabled() || !placeField.isEnabled())
			lockedLabel.show(I18N.t("dialog.quick_entry.locked.note"), participationIn(event, individualId));
		if(events.size() > 1)
			moreLabel.show(I18N.tf("dialog.quick_entry.more", String.valueOf(events.size() - 1)), participationIn(events.get(1), individualId));
	}

	/**
	 * Enables or disables the fields of the row (a row that does not apply is shown but cannot be typed in).
	 *
	 * @param active	Whether the row applies.
	 */
	public void setActive(final boolean active){
		dateField.setEnabled(active);
		calendarCombo.setEnabled(active);
		placeField.setEnabled(active);
	}

	/** @return	The code of the calendar chosen for the date. */
	String calendarCode(){
		return (String)calendarCombo.getSelectedItem();
	}

	/** @return	The calendar of a date structure (the first one it has: the point, or the first end of a bounded or spanning date), gregorian if none. */
	private static String calendarOf(final FLEFRecord dateStructure){
		if(dateStructure != null)
			for(final String path : new String[]{
					FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_CALENDAR),
					FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_BOUNDED, DateReader.TAG_NOT_BEFORE, DateReader.TAG_CALENDAR),
					FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_BOUNDED, DateReader.TAG_NOT_AFTER, DateReader.TAG_CALENDAR),
					FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_SPANNING, DateReader.TAG_FROM, DateReader.TAG_CALENDAR),
					FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_SPANNING, DateReader.TAG_TO, DateReader.TAG_CALENDAR)}){
				final String calendar = FLEFRecordHelper.getChildValue(dateStructure, path);
				if(StringUtils.isNotBlank(calendar) && java.util.Arrays.asList(DateReader.CALENDARS).contains(calendar))
					return calendar;
			}
		return DateReader.ENUM_CALENDAR_GREGORIAN;
	}

	/** Chooses the calendar of the date (what the user would choose). */
	void setCalendar(final String calendar){
		onEventThread(() -> calendarCombo.setSelectedItem(calendar));
	}

	/** Runs the action whenever the date or the place change. */
	public void onChange(final Runnable listener){
		listen(dateField, listener);
		calendarCombo.addActionListener(e -> listener.run());
		listen(placeField, listener);
	}

	/** Types the date and the place (what the user would type). */
	void type(final String date, final String place){
		onEventThread(() -> {
			dateField.setText(date);
			placeField.setText(place);
		});
	}

	/**
	 * Runs the action on the event dispatch thread, where the user interface is changed (the hooks of the tests are called from another
	 * one: a text changed there, with the listeners that change the other components, may deadlock with the layout that runs on the event
	 * thread, which holds the lock of the components and waits for the one of the text).
	 */
	static void onEventThread(final Runnable action){
		if(javax.swing.SwingUtilities.isEventDispatchThread()){
			action.run();

			return;
		}

		try{
			javax.swing.SwingUtilities.invokeAndWait(action);
		}
		catch(final java.lang.reflect.InvocationTargetException ite){
			throw new IllegalStateException(ite.getCause());
		}
		catch(final InterruptedException ie){
			Thread.currentThread()
				.interrupt();
		}
	}

	/** @return	The event that is shown, {@code null} if the fact is new. */
	public FLEFRecord event(){
		return event;
	}

	/** @return	What the user changed of the event that is shown, as {@link #prepare()} settled it. */
	public LifeFactUpdater.Edit edit(){
		return new LifeFactUpdater.Edit(dateChanged, date, placeChanged, place);
	}

	/** @return	Whether nothing has been typed. */
	public boolean isEmpty(){
		return (StringUtils.isBlank(dateField.getText()) && StringUtils.isBlank(placeField.getText()));
	}

	/**
	 * Checks what was typed and settles the place: the user is told if the date cannot be read, and is asked which place is meant if
	 * the file has places named like the one typed.
	 *
	 * @return	Whether the fact is ready ({@code false}: the user must fix the date, or has not decided about the place).
	 */
	public boolean prepare(){
		//a date shown from the file that was not touched is not read again (the text is how it is shown, not what was typed)
		dateChanged = (event != null && (!StringUtils.strip(dateField.getText()).equals(initialDate) || !calendarCode().equals(initialCalendar)));
		date = null;
		if((event == null || dateChanged) && StringUtils.isNotBlank(dateField.getText()))
			try{
				final DateShorthand shorthand = DateShorthandReader.parse(dateField.getText());
				//the calendar tells whether a full date is a date, and which one it is if it can be read in more ways: the date editor asks
				//it too, a date typed here is not worth less
				final Map<String, String> readings = new HashMap<>();
				if(!settleFullDates(shorthand, readings)){
					dateField.requestFocusInWindow();

					return false;
				}

				date = DateStructureBuilder.fromShorthand(DateReader.TAG_DATE, dateField.getText(), calendarCode(),
					fullDate -> readings.getOrDefault(fullDate, fullDate));
			}
			catch(final DateShorthandReader.SyntaxException se){
				reporter.report(dateField, DateShorthandDescriber.describeProblem(se));
				dateField.requestFocusInWindow();

				return false;
			}

		final String name = StringUtils.normalizeSpace(placeField.getText());
		placeChanged = (event != null && !name.equals(initialPlace));
		if(event != null && !placeChanged){
			place = QuickEntryService.PlaceRef.NONE;
			return true;
		}
		if(name.equals(placeDecidedFor))
			return true;

		final QuickEntryService.PlaceRef decided = decidePlace(name);
		if(decided == null)
			return false;

		place = decided;
		placeDecidedFor = name;
		return true;
	}

	/** @return	When and where it happened, as {@link #prepare()} settled it. */
	public QuickEntryService.LifeFact fact(){
		return new QuickEntryService.LifeFact(date, place);
	}


	void setAsker(final Asker asker){
		this.asker = asker;
	}

	void setReporter(final Reporter reporter){
		this.reporter = reporter;
	}

	/**
	 * Settles the full dates of what was typed with the calendar: the user chooses among the readings of an ambiguous one, and is asked
	 * whether to keep one that the calendar does not recognize (or to correct it).
	 *
	 * @param shorthand	The date as typed.
	 * @param readings	Where the reading chosen for a full date is put, by the full date.
	 * @return	Whether the date can be saved ({@code false}: the user wants to correct it).
	 */
	private boolean settleFullDates(final DateShorthand shorthand, final Map<String, String> readings){
		final String calendarCode = calendarCode();
		for(final String fullDate : DateShorthandChecker.fullDatesOf(shorthand)){
			final DateShorthandChecker.Problem problem = DateShorthandChecker.check(fullDate, calendarCode, Locale.getDefault());
			if(problem == null)
				continue;

			if(problem.kind() == DateShorthandChecker.Kind.AMBIGUOUS){
				final List<String> options = new ArrayList<>(problem.readings().size());
				for(final DateInterpretation reading : problem.readings())
					options.add(I18N.tf("validation.date.ambiguous.reading", reading.expression(), reading.meaning()));
				final int index = asker.choose(dateField, I18N.tf("validation.date.ambiguous", fullDate),
					I18N.t("validation.date.ambiguous.title"), options);
				if(index < 0)
					return false;

				readings.put(fullDate, problem.readings().get(index).expression());
			}
			else{
				final String calendarName = I18N.t("enum.date.calendar." + calendarCode, calendarCode);
				final int index = asker.choose(dateField, I18N.tf("validation.date.unrecognized", fullDate, calendarName, problem.reason()),
					I18N.t("validation.date.unrecognized.title"), List.of(I18N.t("dialog.quick_entry.date.correct"),
						I18N.t("dialog.quick_entry.date.keep")));
				//only the second option keeps the date as it was written
				if(index != 1)
					return false;
			}
		}
		return true;
	}

	/** @return	The place meant by the name, {@code null} if the user did not decide. */
	private QuickEntryService.PlaceRef decidePlace(final String name){
		if(name.isEmpty())
			return QuickEntryService.PlaceRef.NONE;
		if(pickedId != null && name.equals(pickedText))
			return QuickEntryService.PlaceRef.existing(pickedId);

		//Padova, Veneto, Italy: the place, then the places that contain it
		final List<String> names = PlaceChain.names(name);
		if(names.isEmpty())
			return QuickEntryService.PlaceRef.NONE;
		if(names.size() > 1)
			return decideChain(names);

		final List<FLEFRecord> candidates = PlaceLookup.find(model, name, LOOKUP_LIMIT);
		if(candidates.isEmpty())
			return QuickEntryService.PlaceRef.named(name);

		final PlaceHandler handler = PlaceHandler.getInstance();
		final List<String> options = new ArrayList<>(candidates.size() + 1);
		for(final FLEFRecord candidate : candidates)
			options.add(handler.getDisplayText(candidate, model));
		options.add(I18N.tf("dialog.quick_entry.place.create", name));
		final int index = asker.choose(placeField, I18N.tf("dialog.quick_entry.place.choose.message", name),
			I18N.t("dialog.quick_entry.place.choose.title"), options);
		if(index < 0)
			return null;

		return (index < candidates.size()
			? QuickEntryService.PlaceRef.existing(candidates.get(index).getId())
			: QuickEntryService.PlaceRef.named(name));
	}

	/**
	 * Settles a chain of places ({@code Padova, Veneto, Italy}) from the largest place down: the user is asked which place of the file is
	 * meant by the largest name (never silently), then a name inside the place that was chosen is that place if the file has only one
	 * with the name there (what makes a place the same is its name and where it is), and asked about if it has more. What the file does
	 * not have is made new, inside the place that was found last.
	 *
	 * @param names	The names, the place first.
	 * @return	The place, {@code null} if the user did not decide.
	 */
	private QuickEntryService.PlaceRef decideChain(final List<String> names){
		final PlaceHandler handler = PlaceHandler.getInstance();
		String parentId = null;
		//the names from this one on are places of the file
		int resolvedFrom = names.size();
		for(int i = names.size() - 1; i >= 0; i --){
			final String name = names.get(i);
			final List<FLEFRecord> candidates = PlaceChain.candidates(model, name, parentId);
			if(candidates.isEmpty())
				break;

			if(parentId != null && candidates.size() == 1){
				parentId = candidates.getFirst().getId();
				resolvedFrom = i;

				continue;
			}

			final List<String> options = new ArrayList<>(candidates.size() + 1);
			for(final FLEFRecord candidate : candidates)
				options.add(handler.getDisplayText(candidate, model));
			options.add(I18N.tf("dialog.quick_entry.place.create", name));
			final int index = asker.choose(placeField, I18N.tf("dialog.quick_entry.place.choose.message", name),
				I18N.t("dialog.quick_entry.place.choose.title"), options);
			if(index < 0)
				return null;
			//a new place, and so is every place inside it
			if(index == candidates.size())
				break;

			parentId = candidates.get(index).getId();
			resolvedFrom = i;
		}

		if(resolvedFrom == 0)
			return QuickEntryService.PlaceRef.existing(parentId);
		return QuickEntryService.PlaceRef.inside(names.getFirst(), names.subList(1, resolvedFrom), parentId);
	}

	private void pickPlace(){
		final FLEFRecord[] picked = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(owner, model,
			(record, handler) -> picked[0] = record, PlaceHandler.class);
		dialog.setVisible(true);

		if(picked[0] == null || !PlaceHandler.isOfType(picked[0]))
			return;

		pickedId = picked[0].getId();
		pickedText = StringUtils.normalizeSpace(PlaceHandler.getInstance().getDisplayText(picked[0], model));
		placeField.setText(pickedText);
	}

	/** Tells what the date means, or what is wrong with it, under the field. */
	private void updateDatePreview(){
		final String text = dateField.getText();
		final boolean visible = datePreview.isVisible();
		if(StringUtils.isBlank(text) || event != null && StringUtils.strip(text).equals(initialDate) && calendarCode().equals(initialCalendar)){
			datePreview.setText(StringUtils.SPACE);
			datePreview.setForeground(UIManager.getColor("Label.foreground"));
			datePreview.setVisible(false);
			if(visible)
				relayout();

			return;
		}

		try{
			final DateShorthand shorthand = DateShorthandReader.parse(text);
			final String calendarCode = calendarCode();
			DateShorthandChecker.Problem problem = null;
			for(final String fullDate : DateShorthandChecker.fullDatesOf(shorthand)){
				problem = DateShorthandChecker.check(fullDate, calendarCode, Locale.getDefault());
				if(problem != null)
					break;
			}
			if(problem == null){
				datePreview.setText(DateShorthandDescriber.describe(shorthand));
				datePreview.setForeground(UIManager.getColor("Label.disabledForeground"));
			}
			else if(problem.kind() == DateShorthandChecker.Kind.AMBIGUOUS){
				datePreview.setText(I18N.tf("dialog.quick_entry.date.ambiguous", problem.date()));
				datePreview.setForeground(UIManager.getColor("Label.disabledForeground"));
			}
			else{
				datePreview.setText(I18N.tf("dialog.quick_entry.date.not_valid", problem.date(),
					I18N.t("enum.date.calendar." + calendarCode, calendarCode)));
				final Color error = UIManager.getColor("Component.error.focusedBorderColor");
				datePreview.setForeground(error != null? error: Color.RED);
			}
			show(visible);
		}
		catch(final DateShorthandReader.SyntaxException se){
			datePreview.setText(DateShorthandDescriber.describeProblem(se));
			final Color error = UIManager.getColor("Component.error.focusedBorderColor");
			datePreview.setForeground(error != null? error: Color.RED);
			show(visible);
		}
	}

	private void show(final boolean wasVisible){
		datePreview.setVisible(true);
		if(!wasVisible)
			relayout();
	}

	/** Lays out the grid again: a line appeared or disappeared. */
	private void relayout(){
		final java.awt.Container grid = datePreview.getParent();
		if(grid != null){
			grid.revalidate();
			grid.repaint();
		}
	}

	/** Runs the action whenever the text of the field changes. */
	static void listen(final javax.swing.text.JTextComponent field, final Runnable action){
		field.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					action.run();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					action.run();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					action.run();
				}
			});
	}

}
