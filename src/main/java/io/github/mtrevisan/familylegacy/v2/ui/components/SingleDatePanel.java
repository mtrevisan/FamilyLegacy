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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.EnumMap;
import java.util.Map;


/* DONE */
/**
 * Panel for editing a single date (FULL_DATE, DECADE, or CENTURY) with optional APPROXIMATE.
 * <p>
 * Structure:
 * <pre>
 * SingleDate = oneof {
 *   full_date: struct {
 *     value: HistoricalDate
 *     calendar: CalendarType | Text
 *   }
 *   decade: struct {
 *     start_year: Int
 *     calendar: CalendarType | Text
 *   }
 *   century: struct {
 *     ordinal: Int
 *     part?: CenturyPart
 *     calendar: CalendarType | Text
 *   }
 * }
 * </pre>
 */
public class SingleDatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 3393161879295516317L;


	private static final String TAG_FULL_DATE = "FULL_DATE";
	private static final String TAG_DECADE = "DECADE";
	private static final String TAG_CENTURY = "CENTURY";
	private static final String TAG_PART = "PART";
	private static final String CALENDAR = "CALENDAR";
	private static final String TAG_CALENDAR = "CALENDAR";
	private static final String TAG_APPROXIMATE = "APPROXIMATE";
	private static final String TAG_POINT = "POINT";

	private static final String DOT = ".";

	private final BindingManager bindingManager = new BindingManager();

	private final JComboBox<DateType> dateTypeCombo = new JComboBox<>(DateType.values());

	private final BoundTextField fullDateField;
	private final BoundTextField decadeField;
	private final BoundTextField centuryField;
	private final BoundComboBox<String> centuryPartCombo;
	private final BoundComboBox<String> calendarCombo;
	private final ApproximatePanel approxPanel;

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	private final Map<DateType, BoundTextField> fieldMap = new EnumMap<>(DateType.class);


	public SingleDatePanel(final Dialog parent, final FLEFModel model){
		fullDateField = new BoundTextField(TAG_FULL_DATE, 15);
		decadeField = new BoundTextField(TAG_DECADE, 5);
		centuryField = new BoundTextField(TAG_CENTURY, 5);
		centuryPartCombo = new BoundComboBox<>(TAG_PART, new String[]{StringUtils.EMPTY,
			"first_quarter", "second_quarter", "third_quarter", "fourth_quarter",
			"first_half", "second_half",
			"early", "mid", "late"});
		calendarCombo = new BoundComboBox<>(TAG_CALENDAR, new String[]{"gregorian", "julian", "islamic", "hebrew",
			"chinese", "indian", "buddhist", "french-republican", "coptic", "soviet eternal", "ethiopian", "mayan"});
		approxPanel = new ApproximatePanel(TAG_APPROXIMATE, parent, model);

		fieldMap.put(DateType.FULL_DATE, fullDateField);
		fieldMap.put(DateType.DECADE, decadeField);
		fieldMap.put(DateType.CENTURY, centuryField);


		initComponents();
	}


	private void initComponents(){
		bindingManager.bind(fullDateField);
		bindingManager.bind(decadeField);
		bindingManager.bind(centuryField);
		bindingManager.bind(centuryPartCombo);
		bindingManager.bind(calendarCombo);


		setLayout(new MigLayout("ins 0,fillx", "[right]rel[grow]", "[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		// Date type combo
		final JPanel typePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typePanel.add(dateTypeCombo, "growx");
		add(typePanel, "growx,wrap");

		// Card panel for FULL_DATE, DECADE, CENTURY
		final JPanel fullDatePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		fullDatePanel.add(new JLabel("Full date:"), "align label");
		fullDatePanel.add(fullDateField, "growx");

		final JPanel centuryPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow][right]rel[grow]"));
		centuryPanel.add(new JLabel("Century:"), "align label");
		centuryPanel.add(centuryField, "growx");
		centuryPanel.add(new JLabel("Part:"), "align label");
		centuryPanel.add(centuryPartCombo, "growx");
		centuryField.setToolTipText("e.g., 15 for 15th century");

		final JPanel decadePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		decadePanel.add(new JLabel("Decade:"), "align label");
		decadePanel.add(decadeField, "growx");
		decadeField.setToolTipText("e.g., 1490 for the 1490s");

		cardPanel.add(fullDatePanel, DateType.FULL_DATE.name());
		cardPanel.add(decadePanel, DateType.DECADE.name());
		cardPanel.add(centuryPanel, DateType.CENTURY.name());
		add(cardPanel, "growx,wrap");

		// Calendar
		final JPanel calendarPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		calendarPanel.add(new JLabel("Calendar:"), "align label");
		calendarCombo.setEditable(true);
		calendarPanel.add(calendarCombo, "growx");
		add(calendarPanel, "growx,wrap");

		// Approximate
		add(approxPanel, "growx,wrap");

		dateTypeCombo.addActionListener(e -> {
			final DateType selected = (DateType)dateTypeCombo.getSelectedItem();
			if(selected != null)
				cardLayout.show(cardPanel, selected.name());
		});
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final DateType type = DateType.fromNode(record);
		dateTypeCombo.setSelectedItem(type);

		approxPanel.loadFromRecord(record);

		centuryPartCombo.setPath(type.getTagName() + DOT + TAG_PART);
		calendarCombo.setPath(type.getTagName() + DOT + CALENDAR);
		bindingManager.load(record);

		cardLayout.show(cardPanel, type.name());
	}

	/**
	 * Saves the current date into the given target record.
	 * The target record will contain the chosen date tag (FULL_DATE, DECADE, or CENTURY)
	 * and optionally APPROXIMATE.
	 *
	 * @return the target record (or null if no data)
	 */
	public FLEFRecord save(){
		if(!hasData())
			return null;

		// Clear non-selected fields automatically using the Map
		fieldMap.forEach((type, field) -> {
			if(type != dateTypeCombo.getSelectedItem())
				field.setText(StringUtils.EMPTY);
		});

		final DateType selectedType = (DateType)dateTypeCombo.getSelectedItem();
		if(selectedType != DateType.CENTURY)
			centuryPartCombo.setText(StringUtils.EMPTY);

		final FLEFRecord record = FLEFRecord.createChild(TAG_POINT);
		centuryPartCombo.setPath(selectedType.getTagName() + DOT + TAG_PART);
		calendarCombo.setPath(selectedType.getTagName() + DOT + TAG_CALENDAR);

		bindingManager.save(record);

		approxPanel.saveToRecord(record);

		return (record.hasData()? record: FLEFRecord.createEmpty());
	}

	public void clear(){
		dateTypeCombo.setSelectedIndex(0);
		fieldMap.values()
			.forEach(field -> field.setText(StringUtils.EMPTY));
		centuryPartCombo.setSelectedIndex(0);
		calendarCombo.setSelectedItem("gregorian");
		approxPanel.clear();
		cardLayout.show(cardPanel, DateType.FULL_DATE.name());
	}

	public boolean hasData(){
		final DateType selected = (DateType)dateTypeCombo.getSelectedItem();
		if(selected == null)
			return false;

		final BoundTextField activeField = fieldMap.get(selected);
		return activeField != null && !activeField.isEmpty();
	}

	public boolean validateData(){
		final DateType selected = (DateType)dateTypeCombo.getSelectedItem();
		if(selected != null){
			final BoundTextField activeField = fieldMap.get(selected);
			if(activeField != null && activeField.isEmpty()){
				JOptionPane.showMessageDialog(this, selected.getErrorMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);

				return false;
			}
		}

		final String calendar = (String)calendarCombo.getSelectedItem();
		if(StringUtils.isEmpty(calendar)){
			JOptionPane.showMessageDialog(this, "Calendar is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return approxPanel.validateData();
	}

}
