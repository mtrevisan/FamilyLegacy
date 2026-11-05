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
package io.github.mtrevisan.familylegacy.ui.tools.dates;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.dates.HistoricalDateAssistantDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDate;
import java.util.function.Supplier;


/**
 * Dialog that performs the three date calculations most useful in genealogical work:
 * <ul>
 *   <li><b>Age at date</b>: from a birth date and a reference date, the age in years, months and days;</li>
 *   <li><b>Interval</b>: from two dates, the time between them;</li>
 *   <li><b>Estimated birth</b>: from a date and an age, the estimated birth date.</li>
 * </ul>
 * The dates are those of the Gregorian calendar, written as {@code YYYY-MM-DD}; the result follows every change of the input, and
 * an input that is not a date is said so instead of leaving the result empty. For the dates in the Julian calendar, in the other
 * calendars of the sources, and for the ages written as "in the 72nd year", the dialog opens the Historical Date Assistant.
 */
final class DateCalculatorDialog extends JDialog{

	private static final int PLAIN_FIELD_COLUMNS = 14;

	private final FLEFModel model;

	// Age tab.
	private final JTextField ageBirthField = dateField();
	private final JTextField ageReferenceField = dateField(LocalDate.now());
	private final JTextField ageResultField = resultField();

	// Interval tab.
	private final JTextField intervalFromField = dateField();
	private final JTextField intervalToField = dateField();
	private final JTextField intervalResultField = resultField();

	// Estimated birth tab.
	private final JTextField estimatedReferenceField = dateField(LocalDate.now());
	private final JSpinner estimatedYearsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 150, 1));
	private final JSpinner estimatedMonthsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1_800, 1));
	private final JSpinner estimatedDaysSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 54_000, 1));
	private final JTextField estimatedResultField = resultField();


	DateCalculatorDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.date_calculator.title"), ModalityType.APPLICATION_MODAL);

		model = context.model();

		final JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(I18N.t("tab.title.age_at_date"), createAgePanel());
		tabs.addTab(I18N.t("tab.title.interval"), createIntervalPanel());
		tabs.addTab(I18N.t("tab.title.estimated_birth"), createEstimatedBirthPanel());

		final JButton assistant = new JButton(I18N.t("button.other_calendars_and_ages"));
		assistant.setToolTipText(I18N.t("tooltip.other_calendars_and_ages"));
		assistant.addActionListener(e -> new HistoricalDateAssistantDialog(this, HistoricalDateAssistantDialog.Tool.AGE, null, null,
			model, null).setVisible(true));

		final JLabel note = new JLabel("<html><i>" + I18N.t("text.date_calculator.note") + "</i></html>");
		note.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));

		setLayout(new BorderLayout(8, 8));
		add(note, BorderLayout.NORTH);
		add(tabs, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(StringUtils.SPACE, assistant, ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		wireListeners();
		computeAll();
	}


	private JPanel createAgePanel(){
		final JPanel panel = form();
		panel.add(new JLabel(I18N.l("label.birth_date")));
		panel.add(ageBirthField, "wrap");
		panel.add(new JLabel(I18N.l("label.reference_date")));
		panel.add(ageReferenceField, "wrap");
		panel.add(new JLabel(I18N.l("label.age")));
		panel.add(ageResultField, "growx");
		return panel;
	}

	private JPanel createIntervalPanel(){
		final JPanel panel = form();
		panel.add(new JLabel(I18N.l("label.from")));
		panel.add(intervalFromField, "wrap");
		panel.add(new JLabel(I18N.l("label.to")));
		panel.add(intervalToField, "wrap");
		panel.add(new JLabel(I18N.l("label.interval")));
		panel.add(intervalResultField, "growx");
		return panel;
	}

	private JPanel createEstimatedBirthPanel(){
		final JPanel panel = form();
		panel.add(new JLabel(I18N.l("label.reference_date")));
		panel.add(estimatedReferenceField, "wrap");
		panel.add(new JLabel(I18N.l("label.age")));
		panel.add(estimatedYearsSpinner, "split 6");
		panel.add(new JLabel(I18N.t("label.years")));
		panel.add(estimatedMonthsSpinner);
		panel.add(new JLabel(I18N.t("label.months")));
		panel.add(estimatedDaysSpinner);
		panel.add(new JLabel(I18N.t("label.days")), "wrap");
		panel.add(new JLabel(I18N.l("label.estimated_birth")));
		panel.add(estimatedResultField, "growx");
		return panel;
	}

	private static JPanel form(){
		return new JPanel(new MigLayout("insets 10,fillx", "[][grow,fill]"));
	}

	private static JTextField dateField(){
		final JTextField field = new JTextField(PLAIN_FIELD_COLUMNS);
		//the format is a hint that disappears as soon as something is typed, not text to be deleted
		field.putClientProperty("JTextField.placeholderText", I18N.t("text.date_calculator.placeholder"));
		field.setToolTipText(I18N.t("text.date_calculator.placeholder"));
		return field;
	}

	private static JTextField dateField(final LocalDate initial){
		final JTextField field = dateField();
		field.setText(initial.toString());
		return field;
	}

	private static JTextField resultField(){
		final JTextField field = new JTextField(28);
		field.setEditable(false);
		return field;
	}


	private void wireListeners(){
		listen(this::computeAge, ageBirthField, ageReferenceField);
		listen(this::computeInterval, intervalFromField, intervalToField);
		listen(this::computeEstimatedBirth, estimatedReferenceField);
		estimatedYearsSpinner.addChangeListener(e -> computeEstimatedBirth());
		estimatedMonthsSpinner.addChangeListener(e -> computeEstimatedBirth());
		estimatedDaysSpinner.addChangeListener(e -> computeEstimatedBirth());
	}

	private static void listen(final Runnable action, final JTextField... fields){
		final DocumentListener listener = new DocumentListener(){
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
		};
		for(final JTextField field : fields)
			field.getDocument()
				.addDocumentListener(listener);
	}

	private void computeAll(){
		computeAge();
		computeInterval();
		computeEstimatedBirth();
	}


	private void computeAge(){
		show(ageResultField, () -> {
			final LocalDate birth = DateCalculations.parseDate(ageBirthField.getText());
			final LocalDate reference = DateCalculations.parseDate(ageReferenceField.getText());
			return (birth != null && reference != null? DateCalculations.formatPeriod(DateCalculations.age(birth, reference)): null);
		});
	}

	private void computeInterval(){
		show(intervalResultField, () -> {
			final LocalDate from = DateCalculations.parseDate(intervalFromField.getText());
			final LocalDate to = DateCalculations.parseDate(intervalToField.getText());
			return (from != null && to != null? DateCalculations.interval(from, to): null);
		});
	}

	private void computeEstimatedBirth(){
		show(estimatedResultField, () -> {
			final LocalDate reference = DateCalculations.parseDate(estimatedReferenceField.getText());
			return (reference != null
				? DateCalculations.estimatedBirth(reference, (int)estimatedYearsSpinner.getValue(), (int)estimatedMonthsSpinner.getValue(),
					(int)estimatedDaysSpinner.getValue()).toString()
				: null);
		});
	}

	/** Shows the result, or the reason there is none (in the color of the errors); nothing if the input is not complete yet. */
	private static void show(final JTextField resultField, final Supplier<String> calculation){
		Color color = UIManager.getColor("TextField.foreground");
		String text;
		try{
			text = StringUtils.defaultString(calculation.get());
		}
		catch(final IllegalArgumentException iae){
			text = iae.getMessage();
			color = ToolDialogs.ERROR_COLOR;
		}
		resultField.setForeground(color);
		resultField.setText(text);
	}

}
