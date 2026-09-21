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
package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;


/**
 * Dialog that performs the three date calculations most useful in
 * genealogical work:
 * <ul>
 *   <li><b>Age at date</b> — given a birth date and a reference date,
 *       returns the age in years, months, and days;</li>
 *   <li><b>Interval</b> — given two dates, returns the interval between
 *       them in years, months, and days;</li>
 *   <li><b>Estimated birth</b> — given a date and an age, returns the
 *       estimated birth date.</li>
 * </ul>
 * All dates are entered in {@code YYYY-MM-DD} form. The dialog validates
 * the input and shows the result immediately on any change.
 */
public final class DateCalculatorDialog extends JDialog{

	private static final String DATE_PLACEHOLDER = "YYYY-MM-DD";

	// Age tab.
	private final JTextField ageBirthField = new JTextField(DATE_PLACEHOLDER, 14);
	private final JTextField ageReferenceField = new JTextField(DATE_PLACEHOLDER, 14);
	private final JTextField ageResultField = new JTextField(28);

	// Interval tab.
	private final JTextField intervalFromField = new JTextField(DATE_PLACEHOLDER, 14);
	private final JTextField intervalToField = new JTextField(DATE_PLACEHOLDER, 14);
	private final JTextField intervalResultField = new JTextField(28);

	// Estimated birth tab.
	private final JTextField ebReferenceField = new JTextField(DATE_PLACEHOLDER, 14);
	private final JTextField ebYearsField = new JTextField("0", 4);
	private final JTextField ebMonthsField = new JTextField("0", 4);
	private final JTextField ebDaysField = new JTextField("0", 4);
	private final JTextField ebResultField = new JTextField(28);


	public DateCalculatorDialog(final ToolContext context){
		super(context.owner(), "Date Calculator", ModalityType.APPLICATION_MODAL);

		ageResultField.setEditable(false);
		intervalResultField.setEditable(false);
		ebResultField.setEditable(false);

		final JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Age at date", createAgePanel());
		tabs.addTab("Interval", createIntervalPanel());
		tabs.addTab("Estimated birth", createEstimatedBirthPanel());

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(560, 320));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		wireListeners();
		computeAll();
	}


	private JPanel createAgePanel(){
		final JPanel panel = form();
		final GridBagConstraints gbc = gbc();
		addRow(panel, gbc, 0, "Birth date:", ageBirthField);
		addRow(panel, gbc, 1, "Reference date:", ageReferenceField);
		addRow(panel, gbc, 2, "Age:", ageResultField);
		return panel;
	}

	private JPanel createIntervalPanel(){
		final JPanel panel = form();
		final GridBagConstraints gbc = gbc();
		addRow(panel, gbc, 0, "From:", intervalFromField);
		addRow(panel, gbc, 1, "To:", intervalToField);
		addRow(panel, gbc, 2, "Interval:", intervalResultField);
		return panel;
	}

	private JPanel createEstimatedBirthPanel(){
		final JPanel panel = form();
		final GridBagConstraints gbc = gbc();
		addRow(panel, gbc, 0, "Reference date:", ebReferenceField);

		// Age row: three small fields.
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		panel.add(new JLabel("Age:"), gbc);
		final JPanel ageRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		ageRow.add(ebYearsField);
		ageRow.add(new JLabel("years"));
		ageRow.add(ebMonthsField);
		ageRow.add(new JLabel("months"));
		ageRow.add(ebDaysField);
		ageRow.add(new JLabel("days"));
		gbc.gridx = 1; gbc.weightx = 1;
		panel.add(ageRow, gbc);

		addRow(panel, gbc, 2, "Estimated birth:", ebResultField);
		return panel;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		buttons.add(close);
		return buttons;
	}


	private void wireListeners(){
		ageBirthField.getDocument().addDocumentListener(listener(this::computeAge));
		ageReferenceField.getDocument().addDocumentListener(listener(this::computeAge));
		intervalFromField.getDocument().addDocumentListener(listener(this::computeInterval));
		intervalToField.getDocument().addDocumentListener(listener(this::computeInterval));
		ebReferenceField.getDocument().addDocumentListener(listener(this::computeEstimatedBirth));
		ebYearsField.getDocument().addDocumentListener(listener(this::computeEstimatedBirth));
		ebMonthsField.getDocument().addDocumentListener(listener(this::computeEstimatedBirth));
		ebDaysField.getDocument().addDocumentListener(listener(this::computeEstimatedBirth));
	}

	private static javax.swing.event.DocumentListener listener(final Runnable action){
		return new javax.swing.event.DocumentListener(){
			@Override public void insertUpdate(final javax.swing.event.DocumentEvent e){ action.run(); }
			@Override public void removeUpdate(final javax.swing.event.DocumentEvent e){ action.run(); }
			@Override public void changedUpdate(final javax.swing.event.DocumentEvent e){ action.run(); }
		};
	}

	private void computeAll(){
		computeAge();
		computeInterval();
		computeEstimatedBirth();
	}


	private void computeAge(){
		final LocalDate birth = parseDate(ageBirthField);
		final LocalDate reference = parseDate(ageReferenceField);
		if(birth == null || reference == null){
			ageResultField.setText("");
			return;
		}
		if(reference.isBefore(birth)){
			ageResultField.setText("Reference date is before the birth date");
			return;
		}
		final Period p = Period.between(birth, reference);
		ageResultField.setText(formatPeriod(p));
	}

	private void computeInterval(){
		final LocalDate from = parseDate(intervalFromField);
		final LocalDate to = parseDate(intervalToField);
		if(from == null || to == null){
			intervalResultField.setText("");
			return;
		}
		final LocalDate a = (from.isBefore(to)? from: to);
		final LocalDate b = (from.isBefore(to)? to: from);
		final Period p = Period.between(a, b);
		final String prefix = (from.isAfter(to)? "− ": "");
		intervalResultField.setText(prefix + formatPeriod(p));
	}

	private void computeEstimatedBirth(){
		final LocalDate reference = parseDate(ebReferenceField);
		if(reference == null){
			ebResultField.setText("");
			return;
		}
		final Integer years = parseInt(ebYearsField);
		final Integer months = parseInt(ebMonthsField);
		final Integer days = parseInt(ebDaysField);
		if(years == null || months == null || days == null){
			ebResultField.setText("Invalid age");
			return;
		}
		LocalDate birth = reference.minusYears(years).minusMonths(months).minusDays(days);
		ebResultField.setText(birth.toString());
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static LocalDate parseDate(final JTextField field){
		final String text = field.getText();
		if(text == null || text.isBlank() || DATE_PLACEHOLDER.equals(text.trim()))
			return null;
		try{
			return LocalDate.parse(text.trim());
		}
		catch(final DateTimeParseException ignored){
			return null;
		}
	}

	private static Integer parseInt(final JTextField field){
		final String text = field.getText();
		if(text == null || text.isBlank())
			return 0;
		try{
			return Integer.parseInt(text.trim());
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	private static String formatPeriod(final Period p){
		final StringBuilder sb = new StringBuilder();
		if(p.getYears() != 0)
			sb.append(p.getYears()).append(p.getYears() == 1? " year": " years");
		if(p.getMonths() != 0){
			if(sb.length() > 0)
				sb.append(", ");
			sb.append(p.getMonths()).append(p.getMonths() == 1? " month": " months");
		}
		if(p.getDays() != 0 || sb.length() == 0){
			if(sb.length() > 0)
				sb.append(", ");
			sb.append(p.getDays()).append(p.getDays() == 1? " day": " days");
		}
		return sb.toString();
	}

	private static JPanel form(){
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		return panel;
	}

	private static GridBagConstraints gbc(){
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		return gbc;
	}

	private static void addRow(final JPanel panel, final GridBagConstraints gbc,
		final int row, final String label, final JTextField field){
		gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
		panel.add(new JLabel(label), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		panel.add(field, gbc);
	}

}
