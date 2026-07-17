package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.CardLayout;


/**
 * Panel for a single date (ISO, CENTURY, or DECADE) with approximate support.
 */
public class SingleDatePanel extends JPanel{
	private final JComboBox<String> dateTypeCombo = new JComboBox<>(new String[]{"ISO", "CENTURY", "DECADE"});

	// ISO
	private final JTextField isoField = new JTextField(15);

	// CENTURY
	private final JTextField centuryField = new JTextField(5);
	private final JComboBox<String> centuryPartCombo = new JComboBox<>(new String[]{"", "first_quarter", "second_quarter", "third_quarter", "fourth_quarter", "first_half", "second_half", "early", "mid", "late"});

	// DECADE
	private final JTextField decadeField = new JTextField(5);

	// CALENDAR (common to all)
	private final JComboBox<String> calendarCombo = new JComboBox<>(new String[]{"gregorian", "julian", "islamic", "hebrew", "chinese", "indian", "buddhist", "french-republican", "coptic", "soviet eternal", "ethiopian", "mayan"});

	// APPROXIMATE
	private final ApproximatePanel approxPanel = new ApproximatePanel();

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	SingleDatePanel(){
		initComponents();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 0, fillx", "[right]rel[grow]", "[]5[]5"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		// Date type combo
		JPanel typePanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typePanel.add(dateTypeCombo, "growx");
		add(typePanel, "growx,wrap");

		// Card panel for ISO, CENTURY, DECADE
		// ISO
		JPanel isoPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		isoPanel.add(new JLabel("Date:"), "align label");
		isoPanel.add(isoField, "growx");
		isoField.setToolTipText("ISO 8601 date (e.g., 2024-01-01)");

		// CENTURY
		JPanel centuryPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow][right]rel[grow]"));
		centuryPanel.add(new JLabel("Century:"), "align label");
		centuryPanel.add(centuryField, "growx");
		centuryPanel.add(new JLabel("Part:"), "align label");
		centuryPanel.add(centuryPartCombo, "growx");
		centuryField.setToolTipText("e.g., 15 for 15th century");

		// DECADE
		JPanel decadePanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		decadePanel.add(new JLabel("Decade:"), "align label");
		decadePanel.add(decadeField, "growx");
		decadeField.setToolTipText("e.g., 1490 for the 1490s");

		cardPanel.add(isoPanel, "ISO");
		cardPanel.add(centuryPanel, "CENTURY");
		cardPanel.add(decadePanel, "DECADE");
		add(cardPanel, "growx,wrap");

		// Calendar
		JPanel calendarPanel = new JPanel(new MigLayout("ins 0, fillx", "[right]rel[grow]"));
		calendarPanel.add(new JLabel("Calendar:"), "align label");
		calendarCombo.setEditable(true);
		calendarPanel.add(calendarCombo, "growx");
		add(calendarPanel, "growx,wrap");

		// Approximate
		add(approxPanel, "growx,wrap");

		// Event listeners
		dateTypeCombo.addActionListener(e -> {
			cardLayout.show(cardPanel, (String)dateTypeCombo.getSelectedItem());
		});
	}

	// ==================== Load / Save ====================

	public void loadFromQualifiedDate(FLEFRecord qualifiedDate){
		if(qualifiedDate == null){
			clear();
			return;
		}

		// Load APPROXIMATE
		FLEFRecord approx = FLEFRecordUtils.findChild(qualifiedDate, "APPROXIMATE");
		approxPanel.loadFromRecord(approx);

		// Load SINGLE_DATE
		FLEFRecord singleDate = FLEFRecordUtils.findChild(qualifiedDate, "SINGLE_DATE");
		if(singleDate == null) return;

		FLEFRecord iso = FLEFRecordUtils.findChild(singleDate, "ISO");
		if(iso != null){
			dateTypeCombo.setSelectedItem("ISO");
			isoField.setText(iso.getValue() != null? iso.getValue(): "");
			String calendar = FLEFRecordUtils.getChildValue(iso, "CALENDAR");
			calendarCombo.setSelectedItem(calendar != null? calendar: "gregorian");
		}
		else{
			FLEFRecord century = FLEFRecordUtils.findChild(singleDate, "CENTURY");
			if(century != null){
				dateTypeCombo.setSelectedItem("CENTURY");
				centuryField.setText(century.getValue() != null? century.getValue(): "");
				String part = FLEFRecordUtils.getChildValue(century, "PART");
				centuryPartCombo.setSelectedItem(part != null? part: "");
				String calendar = FLEFRecordUtils.getChildValue(century, "CALENDAR");
				calendarCombo.setSelectedItem(calendar != null? calendar: "gregorian");
			}
			else{
				FLEFRecord decade = FLEFRecordUtils.findChild(singleDate, "DECADE");
				if(decade != null){
					dateTypeCombo.setSelectedItem("DECADE");
					decadeField.setText(decade.getValue() != null? decade.getValue(): "");
					String calendar = FLEFRecordUtils.getChildValue(decade, "CALENDAR");
					calendarCombo.setSelectedItem(calendar != null? calendar: "gregorian");
				}
			}
		}

		cardLayout.show(cardPanel, (String)dateTypeCombo.getSelectedItem());
	}

	public FLEFRecord saveToQualifiedDate(FLEFRecord target){
		FLEFRecord record = target != null? target: new FLEFRecord();
		if(target == null){
			record.setTag("QUALIFIED_DATE");
		}

		// APPROXIMATE
		approxPanel.saveToRecord(record);

		// SINGLE_DATE
		FLEFRecord singleDate = FLEFRecord.createChild(1, "SINGLE_DATE");

		String dateType = (String)dateTypeCombo.getSelectedItem();
		String calendar = (String)calendarCombo.getSelectedItem();

		if("ISO".equals(dateType)){
			String isoDate = isoField.getText().trim();
			if(!isoDate.isEmpty()){
				FLEFRecord iso = FLEFRecord.createChildWithValue(2, "ISO", isoDate);
				if(calendar != null && !calendar.isEmpty()){
					FLEFRecordUtils.updateChildValue(iso, "CALENDAR", calendar);
				}
				singleDate.addChild(iso);
			}
		}
		else if("CENTURY".equals(dateType)){
			String century = centuryField.getText().trim();
			if(!century.isEmpty()){
				FLEFRecord centuryRec = FLEFRecord.createChildWithValue(2, "CENTURY", century);
				String part = (String)centuryPartCombo.getSelectedItem();
				if(part != null && !part.isEmpty()){
					FLEFRecordUtils.updateChildValue(centuryRec, "PART", part);
				}
				if(calendar != null && !calendar.isEmpty()){
					FLEFRecordUtils.updateChildValue(centuryRec, "CALENDAR", calendar);
				}
				singleDate.addChild(centuryRec);
			}
		}
		else if("DECADE".equals(dateType)){
			String decade = decadeField.getText().trim();
			if(!decade.isEmpty()){
				FLEFRecord decadeRec = FLEFRecord.createChildWithValue(2, "DECADE", decade);
				if(calendar != null && !calendar.isEmpty()){
					FLEFRecordUtils.updateChildValue(decadeRec, "CALENDAR", calendar);
				}
				singleDate.addChild(decadeRec);
			}
		}

		if(!singleDate.getChildren().isEmpty()){
			record.addChild(singleDate);
		}

		// If no data, return null
		if(!record.hasChildren() && !approxPanel.hasData()){
			return null;
		}

		return record;
	}

	public void clear(){
		dateTypeCombo.setSelectedIndex(0);
		isoField.setText("");
		centuryField.setText("");
		centuryPartCombo.setSelectedIndex(0);
		decadeField.setText("");
		calendarCombo.setSelectedItem("gregorian");
		approxPanel.clear();
		cardLayout.show(cardPanel, "ISO");
	}

	public boolean hasData(){
		return !isoField.getText().trim().isEmpty()
					 || !centuryField.getText().trim().isEmpty()
					 || !decadeField.getText().trim().isEmpty()
					 || approxPanel.hasData();
	}

	public boolean validateRequiredFields(){
		String dateType = (String)dateTypeCombo.getSelectedItem();
		if("ISO".equals(dateType) && isoField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this, "Date is required for ISO type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if("CENTURY".equals(dateType) && centuryField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this, "Century is required for CENTURY type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if("DECADE".equals(dateType) && decadeField.getText().trim().isEmpty()){
			JOptionPane.showMessageDialog(this, "Decade is required for DECADE type.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return approxPanel.validateRequiredFields();
	}
}
