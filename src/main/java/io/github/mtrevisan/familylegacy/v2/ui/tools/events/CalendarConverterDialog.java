package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;


/**
 * Dialog that converts a date between the Gregorian (ISO) calendar and
 * the calendars supported by the JDK: Julian, Hijrah (Islamic), Minguo,
 * Thai Buddhist, Japanese.
 * <p>
 * The user enters a date in the source calendar, picks the target
 * calendar from the combo box, and the converted date is shown. The
 * conversion is exact for the supported calendars: it uses the
 * {@link java.time.chrono.ChronoLocalDate} implementations provided by
 * the JDK, and a direct Julian Day Number computation for the Julian
 * calendar.
 * <p>
 * The dialog is deliberately simple: it converts a single date, not an
 * interval. For genealogical work that is what is needed most of the
 * time, and it keeps the UI focused.
 */
public final class CalendarConverterDialog extends JDialog{

	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

	/** Calendar identifiers, in the order they appear in the combo. */
	private static final String[] CALENDARS = {
		"Gregorian (ISO)",
		"Julian",
		"Hijrah (Islamic)",
		"Minguo",
		"Thai Buddhist",
		"Japanese"
	};


	private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(1900, -4000, 9999, 1));
	private final JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
	private final JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
	private final JComboBox<String> sourceCalendarCombo = new JComboBox<>(CALENDARS);
	private final JComboBox<String> targetCalendarCombo = new JComboBox<>(CALENDARS);
	private final JTextField resultField = new JTextField(28);

	/** Cached JDN of the last successfully parsed source date. */
	private long lastJdn = Long.MIN_VALUE;


	public CalendarConverterDialog(final ToolContext context){
		super(context.owner(), "Calendar Converter", ModalityType.APPLICATION_MODAL);

		resultField.setEditable(false);

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(520, 260));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		// Convert on any input change.
		sourceCalendarCombo.addActionListener(e -> convert());
		targetCalendarCombo.addActionListener(e -> convert());
		yearSpinner.addChangeListener(e -> convert());
		monthSpinner.addChangeListener(e -> convert());
		daySpinner.addChangeListener(e -> convert());

		SwingUtilities.invokeLater(this::convert);
	}


	private JPanel createForm(){
		final JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Source calendar.
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel("Source calendar:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
		form.add(sourceCalendarCombo, gbc);
		gbc.gridwidth = 1;

		// Year / month / day.
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel("Year:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(yearSpinner, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		form.add(new JLabel("Month:"), gbc);
		gbc.gridx = 3; gbc.weightx = 1;
		form.add(monthSpinner, gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		form.add(new JLabel("Day:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(daySpinner, gbc);

		// Target calendar.
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
		form.add(new JLabel("Target calendar:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
		form.add(targetCalendarCombo, gbc);
		gbc.gridwidth = 1;

		// Result.
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
		form.add(new JLabel("Result:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1;
		form.add(resultField, gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton swap = new JButton("Swap calendars");
		swap.addActionListener(e -> swapCalendars());
		buttons.add(swap);

		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		buttons.add(close);

		return buttons;
	}


	/* ======================================================================
	 *                          Conversion
	 * ====================================================================== */

	private void convert(){
		final int year = (Integer)yearSpinner.getValue();
		final int month = (Integer)monthSpinner.getValue();
		final int day = (Integer)daySpinner.getValue();
		final String sourceCalendar = (String)sourceCalendarCombo.getSelectedItem();
		final String targetCalendar = (String)targetCalendarCombo.getSelectedItem();

		// Step 1: convert the source date to a Julian Day Number.
		final long jdn;
		try{
			jdn = toJulianDayNumber(year, month, day, sourceCalendar);
		}
		catch(final IllegalArgumentException ex){
			resultField.setText(ex.getMessage());
			lastJdn = Long.MIN_VALUE;
			return;
		}
		lastJdn = jdn;

		// Step 2: convert the JDN to the target calendar.
		try{
			resultField.setText(fromJulianDayNumber(jdn, targetCalendar));
		}
		catch(final IllegalArgumentException ex){
			resultField.setText(ex.getMessage());
		}
	}

	private void swapCalendars(){
		final int src = sourceCalendarCombo.getSelectedIndex();
		final int dst = targetCalendarCombo.getSelectedIndex();
		sourceCalendarCombo.setSelectedIndex(dst);
		targetCalendarCombo.setSelectedIndex(src);
		// The listeners fire and convert.
	}


	/* ======================================================================
	 *                          Calendar arithmetic
	 * ====================================================================== */

	/**
	 * Converts a date expressed in the given calendar to a Julian Day
	 * Number. The JDN is the canonical interchange representation used
	 * by the dialog: every conversion goes through it.
	 */
	private static long toJulianDayNumber(final int year, final int month, final int day,
		final String calendar){
		switch(calendar){
			case "Gregorian (ISO)":
				return toJdnIso(year, month, day);
			case "Julian":
				return toJdnJulian(year, month, day);
			case "Hijrah (Islamic)":
				return toJdnFromChrono(year, month, day, "Hijrah");
			case "Minguo":
				return toJdnFromChrono(year, month, day, "Minguo");
			case "Thai Buddhist":
				return toJdnFromChrono(year, month, day, "ThaiBuddhist");
			case "Japanese":
				return toJdnFromChrono(year, month, day, "Japanese");
			default:
				throw new IllegalArgumentException("Unknown calendar: " + calendar);
		}
	}

	private static String fromJulianDayNumber(final long jdn, final String calendar){
		switch(calendar){
			case "Gregorian (ISO)":
				return fromJdnIso(jdn);
			case "Julian":
				return fromJdnJulian(jdn);
			case "Hijrah (Islamic)":
				return fromJdnToChrono(jdn, "Hijrah");
			case "Minguo":
				return fromJdnToChrono(jdn, "Minguo");
			case "Thai Buddhist":
				return fromJdnToChrono(jdn, "ThaiBuddhist");
			case "Japanese":
				return fromJdnToChrono(jdn, "Japanese");
			default:
				throw new IllegalArgumentException("Unknown calendar: " + calendar);
		}
	}


	// -- Gregorian --

	private static long toJdnIso(final int year, final int month, final int day){
		// Validate through the JDK to catch out-of-range dates.
		try{
			LocalDate.of(year, month, day);
		}
		catch(final Exception e){
			throw new IllegalArgumentException("Invalid Gregorian date");
		}
		return gregorianToJdn(year, month, day);
	}

	private static String fromJdnIso(final long jdn){
		final int[] ymd = jdnToGregorian(jdn);
		try{
			return LocalDate.of(ymd[0], ymd[1], ymd[2]).format(ISO);
		}
		catch(final Exception e){
			throw new IllegalArgumentException("Out of representable range");
		}
	}

	// -- Julian --

	private static long toJdnJulian(final int year, final int month, final int day){
		// Validate the Julian date by converting to Gregorian first.
		final long jdn = julianToJdn(year, month, day);
		// Sanity: converting back must yield the same date.
		final int[] back = jdnToJulian(jdn);
		if(back[0] != year || back[1] != month || back[2] != day)
			throw new IllegalArgumentException("Invalid Julian date");
		return jdn;
	}

	private static String fromJdnJulian(final long jdn){
		final int[] ymd = jdnToJulian(jdn);
		return String.format("%04d-%02d-%02d (Julian)", ymd[0], ymd[1], ymd[2]);
	}

	// -- Chrono-based (Hijrah, Minguo, Thai Buddhist, Japanese) --

	private static long toJdnFromChrono(final int year, final int month, final int day,
		final String calendar){
		try{
			final LocalDate iso;
			switch(calendar){
				case "Hijrah":
					iso = LocalDate.from(HijrahDate.of(year, month, day));
					break;
				case "Minguo":
					iso = LocalDate.from(MinguoDate.of(year, month, day));
					break;
				case "ThaiBuddhist":
					iso = LocalDate.from(ThaiBuddhistDate.of(year, month, day));
					break;
				case "Japanese":
					iso = LocalDate.from(JapaneseDate.of(year, month, day));
					break;
				default:
					throw new IllegalArgumentException("Unsupported calendar: " + calendar);
			}
			return gregorianToJdn(iso.getYear(), iso.getMonthValue(), iso.getDayOfMonth());
		}
		catch(final IllegalArgumentException e){
			throw e;
		}
		catch(final Exception e){
			throw new IllegalArgumentException("Invalid " + calendar + " date");
		}
	}

	private static String fromJdnToChrono(final long jdn, final String calendar){
		final int[] ymd = jdnToGregorian(jdn);
		final LocalDate iso;
		try{
			iso = LocalDate.of(ymd[0], ymd[1], ymd[2]);
		}
		catch(final Exception e){
			throw new IllegalArgumentException("Out of representable range");
		}
		try{
			switch(calendar){
				case "Hijrah":
					return HijrahDate.from(iso).toString();
				case "Minguo":
					return MinguoDate.from(iso).toString();
				case "ThaiBuddhist":
					return ThaiBuddhistDate.from(iso).toString();
				case "Japanese":
					return JapaneseDate.from(iso).toString();
				default:
					throw new IllegalArgumentException("Unsupported calendar: " + calendar);
			}
		}
		catch(final Exception e){
			throw new IllegalArgumentException("Out of representable range for " + calendar);
		}
	}


	/* ======================================================================
	 *                          JDN formulas
	 * ====================================================================== */

	/**
	 * Converts a Gregorian date to a Julian Day Number, using the
	 * standard Meeus formula. Valid for the proleptic Gregorian calendar.
	 */
	private static long gregorianToJdn(final int year, final int month, final int day){
		final int a = (14 - month) / 12;
		final int y = year + 4800 - a;
		final int m = month + 12 * a - 3;
		return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L;
	}

	private static int[] jdnToGregorian(final long jdn){
		final long a = jdn + 32044;
		final long b = (4 * a + 3) / 146097;
		final long c = a - (146097 * b) / 4;
		final long d = (4 * c + 3) / 1461;
		final long e = c - (1461 * d) / 4;
		final long m = (5 * e + 2) / 153;
		final int day = (int)(e - (153 * m + 2) / 5 + 1);
		final int month = (int)(m + 3 - 12 * (m / 10));
		final int year = (int)(100 * b + d - 4800 + m / 10);
		return new int[]{year, month, day};
	}

	/**
	 * Converts a Julian date to a Julian Day Number. Valid for the
	 * proleptic Julian calendar.
	 */
	private static long julianToJdn(final int year, final int month, final int day){
		final int a = (14 - month) / 12;
		final int y = year + 4800 - a;
		final int m = month + 12 * a - 3;
		return day + (153L * m + 2) / 5 + 365L * y + y / 4 - 32083L;
	}

	private static int[] jdnToJulian(final long jdn){
		final long c = jdn + 32082;
		final long d = (4 * c + 3) / 1461;
		final long e = c - (1461 * d) / 4;
		final long m = (5 * e + 2) / 153;
		final int day = (int)(e - (153 * m + 2) / 5 + 1);
		final int month = (int)(m + 3 - 12 * (m / 10));
		final int year = (int)(d - 4800 + m / 10);
		return new int[]{year, month, day};
	}

}
