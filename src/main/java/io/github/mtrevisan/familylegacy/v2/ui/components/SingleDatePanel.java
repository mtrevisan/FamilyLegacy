package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
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


/**
 * Panel for editing a single date (FULL_DATE, DECADE, or CENTURY) with optional APPROXIMATE.
 * <p>
 * This panel operates directly on the real FLEF tags:
 * - FULL_DATE, DECADE, CENTURY (mutually exclusive)
 * - CALENDAR (required for all)
 * - APPROXIMATE (with BASIS, MARGIN, CULTURAL_NORM)
 */
public class SingleDatePanel extends JPanel{

	private static final String DATE_TYPE_FULL_DATE = "Full Date";
	private static final String DATE_TYPE_DECADE = "Decade";
	private static final String DATE_TYPE_CENTURY = "Century";

	private final BindingManager bindingManager = new BindingManager();

	private final JComboBox<String> dateTypeCombo = new JComboBox<>(new String[]{DATE_TYPE_FULL_DATE, DATE_TYPE_DECADE, DATE_TYPE_CENTURY});

	// FULL_DATE
	private final BoundTextField fullDateField = new BoundTextField("FULL_DATE", 15);

	// DECADE
	private final BoundTextField decadeField = new BoundTextField("DECADE", 5);

	// CENTURY
	private final BoundTextField centuryField = new BoundTextField("CENTURY", 5);
	private final BoundComboBox<String> centuryPartCombo = new BoundComboBox<>("PART", new String[]{StringUtils.EMPTY, "first_quarter", "second_quarter", "third_quarter", "fourth_quarter", "first_half", "second_half", "early", "mid", "late"});

	// CALENDAR (common to all)
	private final BoundComboBox<String> calendarCombo = new BoundComboBox<>("CALENDAR", new String[]{"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic", "soviet eternal", "ethiopian", "mayan"});

	// APPROXIMATE
	private final ApproximatePanel approxPanel;

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	// The target record that this panel edits (the date node, e.g., POINT, NOT_BEFORE, FROM)
	private FLEFRecord dateNode;


	public SingleDatePanel(final FLEFModel model){
		this.approxPanel = new ApproximatePanel(model);

		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx", "[right]rel[grow]", "[]5[]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		// Register bound components
		bindingManager.bind(fullDateField);
		bindingManager.bind(centuryField);
		bindingManager.bind(centuryPartCombo);
		bindingManager.bind(decadeField);
		bindingManager.bind(calendarCombo);

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

		cardPanel.add(fullDatePanel, DATE_TYPE_FULL_DATE);
		cardPanel.add(decadePanel, DATE_TYPE_DECADE);
		cardPanel.add(centuryPanel, DATE_TYPE_CENTURY);
		add(cardPanel, "growx,wrap");

		// Calendar
		final JPanel calendarPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		calendarPanel.add(new JLabel("Calendar:"), "align label");
		calendarCombo.setEditable(true);
		calendarPanel.add(calendarCombo, "growx");
		add(calendarPanel, "growx,wrap");

		// Approximate
		add(approxPanel, "growx,wrap");

		dateTypeCombo.addActionListener(e -> cardLayout.show(cardPanel, (String)dateTypeCombo.getSelectedItem()));
	}

	/**
	 * Loads data from a node that contains the actual date tags (FULL_DATE, DECADE, CENTURY)
	 * and optionally APPROXIMATE.
	 *
	 * @param dateNode the record containing the date (e.g., POINT, NOT_BEFORE, FROM)
	 */
	public void loadFromRecord(final FLEFRecord dateNode){
		clear();

		if(dateNode == null)
			return;

		this.dateNode = dateNode;

		final String baseType = extractBaseType();

		// Load APPROXIMATE (direct child of dateNode)
		final FLEFRecord approx = FLEFRecordUtils.findChild(dateNode, "APPROXIMATE");
		approxPanel.loadFromRecord(approx);

		// Load bound fields using BindingManager
		calendarCombo.setPath(baseType + ".CALENDAR");
		bindingManager.loadFromRecord(dateNode);

		// Ensure the correct card is shown based on the date type
		final String selectedType = (String)dateTypeCombo.getSelectedItem();
		if(selectedType != null && !selectedType.isEmpty()){
			// Auto-detect from children: FULL_DATE, CENTURY, DECADE
			if(FLEFRecordUtils.findChild(dateNode, "FULL_DATE") != null)
				dateTypeCombo.setSelectedItem(DATE_TYPE_FULL_DATE);
			else if(FLEFRecordUtils.findChild(dateNode, "DECADE") != null)
				dateTypeCombo.setSelectedItem(DATE_TYPE_DECADE);
			else if(FLEFRecordUtils.findChild(dateNode, "CENTURY") != null)
				dateTypeCombo.setSelectedItem(DATE_TYPE_CENTURY);
			else
				dateTypeCombo.setSelectedIndex(0);
		}
		else
			dateTypeCombo.setSelectedIndex(0);

		cardLayout.show(cardPanel, (String)dateTypeCombo.getSelectedItem());
	}

	/**
	 * Saves the current date into the given target record.
	 * The target record will contain the chosen date tag (FULL_DATE, DECADE, or CENTURY)
	 * and optionally APPROXIMATE.
	 *
	 * @param target the record to save into (e.g., POINT, NOT_BEFORE, FROM)
	 * @return the target record (or null if no data)
	 */
	public FLEFRecord saveToRecord(FLEFRecord target){
		if(!hasData())
			return null;

		if(target == null)
			target = new FLEFRecord();
		FLEFRecordUtils.removeAllChildren(target);

		final String baseType = extractBaseType();

		// Save bound fields (FULL_DATE, DECADE, CENTURY, PART, CALENDAR, etc.)
		calendarCombo.setPath(baseType + ".CALENDAR");
		bindingManager.saveToRecord(target);

		// Save APPROXIMATE (as a child of target)
		approxPanel.saveToRecord(target);

		// Remove any empty child records that might have been created by binding manager
		// (e.g., if no data, but we already have data)

		return target.hasChildren()? target: null;
	}

	private String extractBaseType(){
		String baseType = null;
		final String selectedDateType = (String)dateTypeCombo.getSelectedItem();
		if(DATE_TYPE_FULL_DATE.equals(selectedDateType))
			baseType = "FULL_DATE";
		else if(DATE_TYPE_DECADE.equals(selectedDateType))
			baseType = "DECADE";
		else if(DATE_TYPE_CENTURY.equals(selectedDateType))
			baseType = "CENTURY";
		return baseType;
	}

	public void clear(){
		dateTypeCombo.setSelectedIndex(0);
		fullDateField.setText(StringUtils.EMPTY);
		decadeField.setText(StringUtils.EMPTY);
		centuryField.setText(StringUtils.EMPTY);
		centuryPartCombo.setSelectedIndex(0);
		calendarCombo.setSelectedItem("gregorian");
		approxPanel.clear();
		cardLayout.show(cardPanel, DATE_TYPE_FULL_DATE);
		dateNode = null;
	}

	public boolean hasData(){
		final String dateType = (String)dateTypeCombo.getSelectedItem();
		if(DATE_TYPE_FULL_DATE.equals(dateType))
			return !fullDateField.isEmpty();

		if(DATE_TYPE_DECADE.equals(dateType))
			return !decadeField.isEmpty();

		if(DATE_TYPE_CENTURY.equals(dateType))
			return !centuryField.isEmpty();

		return false;
	}

	public boolean validateRequiredFields(){
		final String dateType = (String)dateTypeCombo.getSelectedItem();

		if(DATE_TYPE_FULL_DATE.equals(dateType) && fullDateField.isEmpty()){
			JOptionPane.showMessageDialog(this, "Date is required for FULL DATE type.", "Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}
		if(DATE_TYPE_DECADE.equals(dateType) && decadeField.isEmpty()){
			JOptionPane.showMessageDialog(this, "Decade is required for DECADE type.", "Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}
		if(DATE_TYPE_CENTURY.equals(dateType) && centuryField.isEmpty()){
			JOptionPane.showMessageDialog(this, "Century is required for CENTURY type.", "Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		final String calendar = (String)calendarCombo.getSelectedItem();
		if(calendar == null || calendar.trim().isEmpty()){
			JOptionPane.showMessageDialog(this, "Calendar is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return approxPanel.validateRequiredFields();
	}

}
