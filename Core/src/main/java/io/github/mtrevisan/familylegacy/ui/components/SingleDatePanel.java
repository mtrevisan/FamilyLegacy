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
package io.github.mtrevisan.familylegacy.ui.components;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.spi.DateInterpretation;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingManager;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


/**
 * Panel for editing a single date struct according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * struct SingleDate {
 *   value: oneof {
 *     full_date: HistoricalDate
 *     decade: Int
 *     century: struct {
 *       ordinal: Int
 *       part?: CenturyPart
 *     }
 *   }
 *   calendar: CalendarType | Text
 *   jdn_first?: Int
 *   jdn_last?: Int
 *   approximate?: Approximate
 * }
 * </pre>
 */
public class SingleDatePanel extends JPanel{

	private final BindingManager bindingManager = new BindingManager();

	private final JComboBox<DateType> singleDateTypeCombo = new JComboBox<>(DateType.values());
	private final BoundTextField fullDateValueField;
	private final BoundTextField decadeStartYearField;
	private final BoundTextField centuryOrdinalField;
	private final BoundComboBox<String> centuryPartCombo;
	private final BoundComboBox<String> calendarCombo;
	private final ApproximatePanel approxPanel;

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	private final Map<DateType, BoundTextField> fieldMap = new EnumMap<>(DateType.class);


	public SingleDatePanel(final Window parent, final FLEFModel model){
		fullDateValueField = new BoundTextField(DateReader.TAG_VALUE_FULL_DATE);
		decadeStartYearField = new BoundTextField(DateReader.TAG_VALUE_DECADE);
		centuryOrdinalField = new BoundTextField(DateReader.TAG_VALUE_CENTURY_ORDINAL);
		centuryPartCombo = new BoundComboBox<>(DateReader.TAG_VALUE_CENTURY_PART, GUIHelper.fillCombo(DateReader.CENTURY_PARTS, null));
		centuryPartCombo.setI18NPrefix("enum.date.century_part");
		calendarCombo = new BoundComboBox<>(DateReader.TAG_CALENDAR, DateReader.CALENDARS);
		calendarCombo.setI18NPrefix("enum.date.calendar");
		calendarCombo.setEditable(true);
		approxPanel = new ApproximatePanel(DateReader.TAG_APPROXIMATE, parent, model);

		fieldMap.put(DateType.FULL_DATE, fullDateValueField);
		fieldMap.put(DateType.DECADE, decadeStartYearField);
		fieldMap.put(DateType.CENTURY, centuryOrdinalField);


		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(fullDateValueField);
		bindingManager.bind(decadeStartYearField);
		bindingManager.bind(centuryOrdinalField);
		bindingManager.bind(centuryPartCombo);
		bindingManager.bind(calendarCombo);


		setLayout(GUIHelper.createLabelFieldLayout(0, "[]10[]10[]20[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		// date type
		final JPanel typePanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(typePanel, I18N.t("dialog.date.type") + ":", singleDateTypeCombo);
		GUIHelper.addComponent(this, typePanel);

		// card panel for FULL_DATE, DECADE, CENTURY:
		final JPanel fullDatePanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(fullDatePanel, I18N.t("dialog.date.full_date") + ":", fullDateValueField);

		final JPanel decadePanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(decadePanel, I18N.t("dialog.date.decade") + ":", decadeStartYearField);
		decadeStartYearField.setToolTipText(I18N.t("dialog.date.decade.tooltip"));

		final JPanel centuryPanel = new JPanel(new MigLayout("ins 0,fillx,wrap 2", "[right]rel[grow,fill]"));
		GUIHelper.addLabeledComponent(centuryPanel, I18N.t("dialog.date.century") + ":", centuryOrdinalField);
		centuryOrdinalField.setToolTipText(I18N.t("dialog.date.century.tooltip"));
		GUIHelper.addLabeledComponent(centuryPanel, I18N.t("dialog.date.part") + ":", centuryPartCombo);

		cardPanel.add(fullDatePanel, DateType.FULL_DATE.name());
		cardPanel.add(decadePanel, DateType.DECADE.name());
		cardPanel.add(centuryPanel, DateType.CENTURY.name());
		GUIHelper.addComponent(this, cardPanel);

		updateCardPanelHeight();

		// calendar
		final JPanel calendarPanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(calendarPanel, I18N.t("dialog.date.calendar") + ":", calendarCombo);
		GUIHelper.addComponent(this, calendarPanel);

		// Approximate
		GUIHelper.addComponent(this, approxPanel);

		singleDateTypeCombo.addActionListener(e -> {
			final DateType selected = (DateType)singleDateTypeCombo.getSelectedItem();
			if(selected != null){
				GUIHelper.setComponentEnabled(fullDateValueField, (selected == DateType.FULL_DATE));
				GUIHelper.setComponentEnabled(decadeStartYearField, (selected == DateType.DECADE));
				GUIHelper.setComponentEnabled(centuryOrdinalField, (selected == DateType.CENTURY));
				GUIHelper.setComponentEnabled(centuryPartCombo, (selected == DateType.CENTURY));

				cardLayout.show(cardPanel, selected.name());

				updateCardPanelHeight();

				SwingUtilities.invokeLater(() -> {
					final Container parent = cardPanel.getParent();
					if(parent != null){
						parent.revalidate();
						parent.repaint();
					}
				});
			}
		});
	}

	private void updateCardPanelHeight(){
		int maxHeight = 0;
		for(final Component card : cardPanel.getComponents()){
			// Force layout to calculate preferred sizes
			final Dimension preferredSize = card.getPreferredSize();
			if(preferredSize == null)
				continue;

			final int h = preferredSize.height;
			if(h > maxHeight)
				maxHeight = h;
		}
		// Set the cardPanel to always have this height
		cardPanel.setPreferredSize(new Dimension(cardPanel.getPreferredSize().width, maxHeight));
		cardPanel.setMinimumSize(new Dimension(cardPanel.getMinimumSize().width, maxHeight));
		cardPanel.setMaximumSize(new Dimension(cardPanel.getMaximumSize().width, maxHeight));
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final DateType singleDateType = DateType.fromNode(record);
		singleDateTypeCombo.setSelectedItem(singleDateType);

		approxPanel.loadFromRecord(record);

		bindingManager.load(record);

		cardLayout.show(cardPanel, singleDateType.name());
	}

	/** @return the calendar code currently selected */
	public String getCalendar(){
		return calendarCombo.getText();
	}

	/** @return the text of the full date currently entered */
	public String getFullDateText(){
		return fullDateValueField.getText();
	}

	/**
	 * Replaces the content with a full date: used when the user applies a suggestion.
	 *
	 * @param calendarCode	the code of the calendar of the date
	 * @param date	the date, in the notation of the calendar
	 */
	public void setFullDate(final String calendarCode, final String date){
		clear();

		singleDateTypeCombo.setSelectedItem(DateType.FULL_DATE);
		fullDateValueField.setText(date);
		calendarCombo.setSelectedItem(calendarCode);
		cardLayout.show(cardPanel, DateType.FULL_DATE.name());
	}

	/**
	 * Saves the current date into the given target record.
	 * The target record will contain the chosen date tag (FULL_DATE, DECADE, or CENTURY)
	 * and optionally APPROXIMATE.
	 *
	 * @return the target record (or {@code null} if no data)
	 */
	public FLEFRecord save(){
		if(!hasData())
			return null;

		// Clear non-selected fields automatically using the Map
		fieldMap.forEach((type, field) -> {
			if(type != singleDateTypeCombo.getSelectedItem())
				field.setText(StringUtils.EMPTY);
		});

		final DateType singleDateType = (DateType)singleDateTypeCombo.getSelectedItem();
		if(singleDateType != DateType.CENTURY)
			centuryPartCombo.setText(StringUtils.EMPTY);

		// Build the value container for the oneof
		final FLEFRecord valueContainer = FLEFRecord.createEmpty();
		bindingManager.save(valueContainer);

		// Build the root SingleDate struct record
		final FLEFRecord record = FLEFRecord.createEmpty();
		if(valueContainer.hasData())
			record.addChildWithTag(DateReader.TAG_VALUE, valueContainer);

		final String calendar = calendarCombo.getText();
		if(StringUtils.isNotEmpty(calendar))
			record.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, calendar));

		approxPanel.saveToRecord(record);

		return (record.hasData()? record: FLEFRecord.createEmpty());
	}

	public void clear(){
		singleDateTypeCombo.setSelectedIndex(0);
		fieldMap.values()
			.forEach(field -> field.setText(StringUtils.EMPTY));
		centuryPartCombo.setSelectedIndex(0);
		calendarCombo.setSelectedItem(DateReader.ENUM_CALENDAR_GREGORIAN);
		approxPanel.clear();
		cardLayout.show(cardPanel, DateType.FULL_DATE.name());
	}

	public boolean hasData(){
		final DateType selected = (DateType)singleDateTypeCombo.getSelectedItem();
		if(selected == null)
			return false;

		final BoundTextField activeField = fieldMap.get(selected);
		return activeField != null && !activeField.isEmpty();
	}

	public boolean validateData(){
		final DateType selected = (DateType)singleDateTypeCombo.getSelectedItem();
		if(selected != null){
			final BoundTextField activeField = fieldMap.get(selected);
			if(activeField != null && activeField.isEmpty()){
				JOptionPane.showMessageDialog(this,
					selected.getErrorMessage(),
					I18N.t("validation.title"), JOptionPane.ERROR_MESSAGE);

				return false;
			}
		}

		final String calendar = (String)calendarCombo.getSelectedItem();
		if(StringUtils.isEmpty(calendar)){
			JOptionPane.showMessageDialog(this,
				I18N.tf("validation.required", I18N.t("dialog.date.calendar")),
				I18N.t("validation.title"), JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(selected == DateType.FULL_DATE && !validateFullDate(calendar))
			return false;

		return approxPanel.validateData();
	}

	/**
	 * Checks that the full date has a single reading, and that it is recognized by its calendar.
	 *
	 * @param calendar	The code of the calendar of the date.
	 * @return	Whether the date can be saved.
	 */
	private boolean validateFullDate(final String calendar){
		final CalendarType calendarType = CalendarType.fromCode(calendar);
		// a calendar unknown to the parsers cannot be checked
		if(!calendarType.getCode().equalsIgnoreCase(calendar))
			return true;

		return (resolveAmbiguousDate(calendarType) && confirmUnrecognizedDate(calendarType));
	}

	/**
	 * Lets the user choose among the readings of a date that can be understood in more than one way (e.g. "est 1850",
	 * summer in Italian and August in Cornish), replacing the date with the chosen one, or go back to edit the date.
	 *
	 * @param calendarType	The calendar of the date.
	 * @return	Whether the date has a single reading (possibly after the choice of the user).
	 */
	private boolean resolveAmbiguousDate(final CalendarType calendarType){
		final String date = fullDateValueField.getText();
		final List<DateInterpretation> interpretations = calendarType.getInterpretations(date, Locale.getDefault());
		if(interpretations.isEmpty())
			return true;

		final JPanel messagePanel = new JPanel(new MigLayout("ins 0,wrap 1"));
		messagePanel.add(new JLabel(I18N.tf("validation.date.ambiguous", date)));
		final ButtonGroup readingGroup = new ButtonGroup();
		final List<JRadioButton> readingButtons = new ArrayList<>(interpretations.size());
		for(final DateInterpretation interpretation : interpretations){
			final JRadioButton readingButton = new JRadioButton(I18N.tf("validation.date.ambiguous.reading",
				interpretation.expression(), interpretation.meaning()));
			readingGroup.add(readingButton);
			readingButtons.add(readingButton);
			messagePanel.add(readingButton, "gapleft 10");
		}
		// the reading chosen by the parser
		readingButtons.getFirst()
			.setSelected(true);

		final Object[] options = {I18N.t("validation.date.ambiguous.choose"), I18N.t("validation.date.ambiguous.edit")};
		final int choice = JOptionPane.showOptionDialog(this, messagePanel, I18N.t("validation.date.ambiguous.title"),
			JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if(choice != 0){
			fullDateValueField.requestFocusInWindow();

			return false;
		}

		for(int i = 0, count = readingButtons.size(); i < count; i ++)
			if(readingButtons.get(i).isSelected())
				fullDateValueField.setText(interpretations.get(i).expression());
		return true;
	}

	/**
	 * Asks the user whether to save a date that its calendar does not recognize, or go back to edit it.
	 *
	 * @param calendarType	The calendar of the date.
	 * @return	Whether the date is recognized, or the user wants to save it anyway.
	 */
	private boolean confirmUnrecognizedDate(final CalendarType calendarType){
		final String date = fullDateValueField.getText();
		final String reason;
		try{
			calendarType.parseToJdn(date);

			return true;
		}
		catch(final UnsupportedOperationException uoe){
			// the date is recognized, but its conversion needs a missing plugin (e.g. the seasons need the Ephemeris)
			return true;
		}
		catch(final RuntimeException re){
			reason = Objects.toString(re.getMessage(), StringUtils.EMPTY);
		}

		final String calendarName = I18N.t("enum.date.calendar." + calendarType.getCode(), calendarType.getCode());
		final int choice = JOptionPane.showConfirmDialog(this,
			I18N.tf("validation.date.unrecognized", date, calendarName, reason),
			I18N.t("validation.date.unrecognized.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(choice != JOptionPane.YES_OPTION){
			fullDateValueField.requestFocusInWindow();

			return false;
		}

		return true;
	}

}
