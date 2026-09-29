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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.util.EnumMap;
import java.util.Map;


/**
 * Panel for editing a single date (FULL_DATE, DECADE, or CENTURY) with optional APPROXIMATE.
 * <p>
 * Structure:
 * <pre>
 *   full_date: struct {
 *     value: HistoricalDate
 *     approximate?: Approximate
 *     calendar: CalendarType | Text
 *   }
 *   decade: struct {
 *     start_year: Int
 *     approximate?: Approximate
 *     calendar: CalendarType | Text
 *   }
 *   century: struct {
 *     ordinal: Int
 *     part?: CenturyPart
 *     approximate?: Approximate
 *     calendar: CalendarType | Text
 *   }
 * }
 * </pre>
 */
public class SingleDatePanel extends JPanel{

	private static final String DOT = ".";
	private static final String TAG_FULL_DATE_VALUE = DateField.TAG_FULL_DATE + DOT + DateField.TAG_VALUE;
	private static final String TAG_DECADE_START_YEAR = DateField.TAG_DECADE + DOT + DateField.TAG_START_YEAR;
	private static final String TAG_CENTURY_ORDINAL = DateField.TAG_CENTURY + DOT + DateField.TAG_ORDINAL;
	private static final String TAG_CENTURY_PART = DateField.TAG_CENTURY + DOT + DateField.TAG_PART;

	public static final String ENUM_PART_FIRST_QUARTER = "first_quarter";
	public static final String ENUM_PART_SECOND_QUARTER = "second_quarter";
	public static final String ENUM_PART_THIRD_QUARTER = "third_quarter";
	public static final String ENUM_PART_FOURTH_QUARTER = "fourth_quarter";
	public static final String ENUM_PART_FIRST_HALF = "first_half";
	public static final String ENUM_PART_SECOND_HALF = "second_half";
	public static final String ENUM_PART_EARLY = "early";
	public static final String ENUM_PART_MID = "mid";
	public static final String ENUM_PART_LATE = "late";
	private static final String[] CENTURY_PARTS = {
		StringUtils.EMPTY,
		ENUM_PART_FIRST_QUARTER, ENUM_PART_SECOND_QUARTER, ENUM_PART_THIRD_QUARTER, ENUM_PART_FOURTH_QUARTER,
		ENUM_PART_FIRST_HALF, ENUM_PART_SECOND_HALF,
		ENUM_PART_EARLY, ENUM_PART_MID, ENUM_PART_LATE
	};

	private static final String ENUM_CALENDAR_GREGORIAN = "gregorian";
	public static final String[] CALENDARS = {
		ENUM_CALENDAR_GREGORIAN, "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic",
		"soviet eternal", "ethiopian", "mayan"
	};


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
		fullDateValueField = new BoundTextField(TAG_FULL_DATE_VALUE);
		decadeStartYearField = new BoundTextField(TAG_DECADE_START_YEAR);
		centuryOrdinalField = new BoundTextField(TAG_CENTURY_ORDINAL);
		centuryPartCombo = new BoundComboBox<>(TAG_CENTURY_PART, CENTURY_PARTS);
		calendarCombo = new BoundComboBox<>(DateField.TAG_CALENDAR, CALENDARS);
		calendarCombo.setEditable(true);
		approxPanel = new ApproximatePanel(DateField.TAG_APPROXIMATE, parent, model);

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
		GUIHelper.addLabeledComponent(fullDatePanel, I18N.t("dialog.date.full.date") + ":", fullDateValueField);

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
				GUIHelper.setComponentVisible(fullDateValueField, (selected == DateType.FULL_DATE));
				GUIHelper.setComponentVisible(decadeStartYearField, (selected == DateType.DECADE));
				GUIHelper.setComponentVisible(centuryOrdinalField, (selected == DateType.CENTURY));
				GUIHelper.setComponentVisible(centuryPartCombo, (selected == DateType.CENTURY));

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

		approxPanel.setPath(singleDateType.getTagName() + DOT + DateField.TAG_APPROXIMATE);
		calendarCombo.setPath(singleDateType.getTagName() + DOT + DateField.TAG_CALENDAR);

		approxPanel.loadFromRecord(record);

		bindingManager.load(record);

		cardLayout.show(cardPanel, singleDateType.name());
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

		approxPanel.setPath(singleDateType.getTagName() + DOT + DateField.TAG_CALENDAR);
		calendarCombo.setPath(singleDateType.getTagName() + DOT + DateField.TAG_CALENDAR);

		final FLEFRecord record = FLEFRecord.createChildWithTag(DateField.TAG_VALUE);

		bindingManager.save(record);

		approxPanel.saveToRecord(record);

		return (record.hasData()? record: FLEFRecord.createEmpty());
	}

	public void clear(){
		singleDateTypeCombo.setSelectedIndex(0);
		fieldMap.values()
			.forEach(field -> field.setText(StringUtils.EMPTY));
		centuryPartCombo.setSelectedIndex(0);
		calendarCombo.setSelectedItem(ENUM_CALENDAR_GREGORIAN);
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

		return approxPanel.validateData();
	}

}
