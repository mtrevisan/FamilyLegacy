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
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.io.Serial;
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

	@Serial
	private static final long serialVersionUID = 3393161879295516317L;


	private static final String DOT = ".";

	private static final String TAG_APPROXIMATE = "APPROXIMATE";

	private static final String TAG_FULL_DATE = "FULL_DATE";
	private static final String TAG_DECADE = "DECADE";
	private static final String TAG_CENTURY = "CENTURY";

	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_START_YEAR = "START_YEAR";
	private static final String TAG_ORDINAL = "ORDINAL";
	private static final String TAG_PART = "PART";
	private static final String TAG_CALENDAR = "CALENDAR";


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


	public SingleDatePanel(final Dialog parent, final FLEFModel model){
		fullDateValueField = new BoundTextField(TAG_FULL_DATE + DOT + TAG_VALUE);
		decadeStartYearField = new BoundTextField(TAG_DECADE + DOT + TAG_START_YEAR);
		centuryOrdinalField = new BoundTextField(TAG_CENTURY + DOT + TAG_ORDINAL);
		centuryPartCombo = new BoundComboBox<>(TAG_CENTURY + DOT + TAG_PART, new String[]{
			StringUtils.EMPTY,
			"first_quarter", "second_quarter", "third_quarter", "fourth_quarter",
			"first_half", "second_half",
			"early", "mid", "late"});
		calendarCombo = new BoundComboBox<>(TAG_CALENDAR, new String[]{
			"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic",
			"soviet eternal", "ethiopian", "mayan"});
		calendarCombo.setEditable(true);
		approxPanel = new ApproximatePanel(TAG_APPROXIMATE, parent, model);

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
		GUIHelper.addLabeledComponent(typePanel, "Type:", singleDateTypeCombo);
		GUIHelper.addComponent(this, typePanel);

		// card panel for FULL_DATE, DECADE, CENTURY:
		final JPanel fullDatePanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(fullDatePanel, "Full date:", fullDateValueField);

		final JPanel decadePanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(decadePanel, "Decade:", decadeStartYearField);
		decadeStartYearField.setToolTipText("e.g., 1490 for the 1490s");

		final JPanel centuryPanel = new JPanel(new MigLayout("ins 0,fillx,wrap 2", "[right]rel[grow,fill]"));
		GUIHelper.addLabeledComponent(centuryPanel, "Century:", centuryOrdinalField);
		GUIHelper.addLabeledComponent(centuryPanel, "Part:", centuryPartCombo);
		centuryOrdinalField.setToolTipText("e.g., 15 for 15th century");

		cardPanel.add(fullDatePanel, DateType.FULL_DATE.name());
		cardPanel.add(decadePanel, DateType.DECADE.name());
		cardPanel.add(centuryPanel, DateType.CENTURY.name());
		GUIHelper.addComponent(this, cardPanel);

		updateCardPanelHeight();

		// calendar
		final JPanel calendarPanel = GUIHelper.createLabelFieldPanel(0, "[]");
		GUIHelper.addLabeledComponent(calendarPanel, "Calendar:", calendarCombo);
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

		cardPanel.revalidate();
		cardPanel.repaint();
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final DateType singleDateType = DateType.fromNode(record);
		singleDateTypeCombo.setSelectedItem(singleDateType);

		approxPanel.setPath(singleDateType.getTagName() + DOT + TAG_APPROXIMATE);
		calendarCombo.setPath(singleDateType.getTagName() + DOT + TAG_CALENDAR);

		approxPanel.loadFromRecord(record);

		bindingManager.load(record);

		cardLayout.show(cardPanel, singleDateType.name());
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
			if(type != singleDateTypeCombo.getSelectedItem())
				field.setText(StringUtils.EMPTY);
		});

		final DateType singleDateType = (DateType)singleDateTypeCombo.getSelectedItem();
		if(singleDateType != DateType.CENTURY)
			centuryPartCombo.setText(StringUtils.EMPTY);

		approxPanel.setPath(singleDateType.getTagName() + DOT + TAG_CALENDAR);
		calendarCombo.setPath(singleDateType.getTagName() + DOT + TAG_CALENDAR);

		final FLEFRecord record = FLEFRecord.createChildWithTag(TAG_VALUE);

		bindingManager.save(record);

		approxPanel.saveToRecord(record);

		return (record.hasData()? record: FLEFRecord.createEmpty());
	}

	public void clear(){
		singleDateTypeCombo.setSelectedIndex(0);
		fieldMap.values()
			.forEach(field -> field.setText(StringUtils.EMPTY));
		centuryPartCombo.setSelectedIndex(0);
		calendarCombo.setSelectedItem("gregorian");
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
