package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;


import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;


/**
 * A modal dialog that lets the user pick a date using a calendar table.
 * Returns a {@link LocalDate} or {@code null} if cancelled.
 * Layout is managed by MigLayout.
 */
public class DatePickerDialog extends JDialog{

	// Current selected date (may change during interaction)
	private LocalDate selectedDate;

	// UI components
	private final JLabel monthYearLabel;
	private final JTable dayTable;
	private final DayTableModel dayModel;


	/**
	 * Constructs a date picker dialog.
	 *
	 * @param parent      the parent window (can be null)
	 * @param initialDate the date to show initially; if null, today is used
	 */
	public DatePickerDialog(final Window parent, final LocalDate initialDate){
		super(parent, "Select Date", ModalityType.APPLICATION_MODAL);

		// Date initially shown when dialog opens
		selectedDate = (initialDate != null? initialDate: LocalDate.now());

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);

		// --- Main panel with MigLayout ---
		// Layout constraints: wrap after each row, padding 15px, gaps 10px
		final JPanel mainPanel = new JPanel(new MigLayout("ins 15,gap 10,wrap 1", "[grow,fill]", "[]10[]10[]"));

		// --- Navigation row: previous, month/year, next ---
		// Use a sub-panel with MigLayout to keep them together
		final JPanel navPanel = new JPanel(new MigLayout("ins 0,gap 5", "[][grow,center][]"));
		final JButton prevMonthButton = new JButton("<");
		final JButton nextMonthButton = new JButton(">");
		monthYearLabel = new JLabel(StringUtils.EMPTY, SwingConstants.CENTER);
		monthYearLabel.setFont(monthYearLabel.getFont().deriveFont(Font.BOLD, 16f));

		navPanel.add(prevMonthButton, "align left");
		navPanel.add(monthYearLabel, "grow,center");
		navPanel.add(nextMonthButton, "align right");

		mainPanel.add(navPanel, "growx,wrap");

		// --- Calendar table ---
		dayModel = new DayTableModel(selectedDate);
		dayTable = new JTable(dayModel);
		dayTable.setRowHeight(30);
		dayTable.setFont(dayTable.getFont().deriveFont(14f));
		dayTable.setShowGrid(false);
		dayTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dayTable.setCellSelectionEnabled(true);
		dayTable.setDefaultRenderer(Object.class, new DayCellRenderer());

		// Mouse click on table: select a day
		dayTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				final int row = dayTable.getSelectedRow();
				final int col = dayTable.getSelectedColumn();
				if(row >= 0 && col >= 0){
					final LocalDate day = dayModel.getDateAt(row, col);
					if(day != null){
						selectedDate = day;

						// refresh to highlight selected day
						dayTable.repaint();
					}
				}
			}
		});

		final JScrollPane scrollPane = new JScrollPane(dayTable);
		scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		scrollPane.setPreferredSize(new Dimension(280, 180));

		mainPanel.add(scrollPane, "grow,push,wrap");

		// --- Button panel (Today, OK, Cancel) ---
		// Use MigLayout to align buttons to the right
		final JPanel buttonPanel = new JPanel(new MigLayout("ins 0,gap 5", "[][][grow]"));
		final JButton todayButton = new JButton("Today");
		final JButton okButton = new JButton("OK");
		final JButton cancelButton = new JButton("Cancel");

		todayButton.addActionListener(e -> goToToday());
		okButton.addActionListener(e -> dispose());
		cancelButton.addActionListener(e -> {
			selectedDate = null;

			dispose();
		});

		// Add buttons: Today left, OK center, Cancel right with grow pushing it
		buttonPanel.add(todayButton, "align left");
		buttonPanel.add(okButton, "align center");
		buttonPanel.add(cancelButton, "align right,grow");

		mainPanel.add(buttonPanel, "growx,align right");

		add(mainPanel);

		pack();

		setLocationRelativeTo(parent);

		// Initial display update
		updateView();

		// Navigation actions
		prevMonthButton.addActionListener(e -> changeMonth(-1));
		nextMonthButton.addActionListener(e -> changeMonth(1));
	}

	/** Updates the month/year label and highlights the selected date. */
	private void updateView(){
		final YearMonth ym = dayModel.getCurrentYearMonth();
		monthYearLabel.setText(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
		dayTable.clearSelection();
		final int[] pos = dayModel.findDatePosition(selectedDate);
		if(pos != null)
			dayTable.changeSelection(pos[0], pos[1], false, false);
		dayTable.repaint();
	}

	/** Moves the calendar by delta months (positive = forward). */
	private void changeMonth(final int delta){
		dayModel.changeMonth(delta);

		updateView();
	}

	/** Resets the calendar to today's date. */
	private void goToToday(){
		final LocalDate today = LocalDate.now();
		selectedDate = today;
		dayModel.setCurrentDate(today);

		updateView();
	}

	/**
	 * Shows the dialog and returns the selected date.
	 *
	 * @return the selected {@link LocalDate}, or {@code null} if cancelled.
	 */
	public LocalDate showDialog(){
		setVisible(true);

		return selectedDate;
	}

	// ================ Table Model ================

	/**
	 * Table model that represents a calendar month.
	 * Each cell contains a day-of-month number (or null for empty cells).
	 */
	private static class DayTableModel extends AbstractTableModel{
		private YearMonth currentYearMonth;
		// 1 = Monday, 7 = Sunday (ISO)
		private final int firstDayOfWeek;

		public DayTableModel(final LocalDate initialDate){
			this.currentYearMonth = YearMonth.from(initialDate);
			this.firstDayOfWeek = 1;
		}

		public void setCurrentDate(final LocalDate date){
			this.currentYearMonth = YearMonth.from(date);

			fireTableDataChanged();
		}

		public void changeMonth(final int delta){
			currentYearMonth = currentYearMonth.plusMonths(delta);

			fireTableDataChanged();
		}

		public YearMonth getCurrentYearMonth(){
			return currentYearMonth;
		}

		@Override
		public int getRowCount(){
			// maximum weeks in a month
			return 6;
		}

		@Override
		public int getColumnCount(){
			// 7 days a week
			return 7;
		}

		/**
		 * Returns the day-of-month for the given cell, or -1 if the cell is empty.
		 */
		private int getDayOfMonth(final int row, final int col){
			// First day of the month (as LocalDate)
			final LocalDate first = currentYearMonth.atDay(1);
			// Offset to align with firstDayOfWeek (1=Monday)
			final int offset = (first.getDayOfWeek().getValue() - firstDayOfWeek + 7) % 7;
			final int day = row * 7 + col - offset + 1;

			return (day >= 1 && day <= currentYearMonth.lengthOfMonth()? day: -1);
		}

		/**
		 * Returns the LocalDate at the given cell, or null if the cell is empty.
		 */
		public LocalDate getDateAt(final int row, final int col){
			final int day = getDayOfMonth(row, col);
			if(day == -1)
				return null;

			return currentYearMonth.atDay(day);
		}

		/**
		 * Finds the (row, col) position of a given date within the current month.
		 * Returns null if the date does not belong to the current month.
		 */
		public int[] findDatePosition(final LocalDate date){
			if(date == null || !currentYearMonth.equals(YearMonth.from(date)))
				return null;

			final LocalDate first = currentYearMonth.atDay(1);
			final int offset = (first.getDayOfWeek().getValue() - firstDayOfWeek + 7) % 7;
			final int dayOfMonth = date.getDayOfMonth();
			final int index = dayOfMonth + offset - 1;
			return new int[]{index / 7, index % 7};
		}

		@Override
		public Object getValueAt(final int row, final int col){
			final int day = getDayOfMonth(row, col);
			return (day != -1? day: null);
		}

		@Override
		public String getColumnName(final int col){
			// Column headers: Mon, Tue, ...
			final DayOfWeek dow = DayOfWeek.of((col + firstDayOfWeek - 1) % 7 + 1);
			return dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
		}
	}

	// ================ Cell Renderer ================

	/**
	 * Renders each table cell:
	 * - Empty cells are blank.
	 * - The selected date is highlighted with a blue background.
	 * - The current day is shown with a small border.
	 * - Other days are plain.
	 */
	private static class DayCellRenderer extends DefaultTableCellRenderer{
		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);

			if(value == null){
				label.setText(StringUtils.EMPTY);
				label.setBackground(table.getBackground());

				return label;
			}

			// The value is an Integer (day of month)
			final int day = (Integer)value;
			label.setText(String.valueOf(day));

			// Get the actual date for this cell from the model
			final DayTableModel model = (DayTableModel)table.getModel();
			final LocalDate cellDate = model.getDateAt(row, column);
			final LocalDate selected = ((DatePickerDialog)SwingUtilities.getWindowAncestor(table))
				.selectedDate;

			// Highlight if this cell is the currently selected date
			if(cellDate != null && cellDate.equals(selected)){
				label.setBackground(new Color(0x4A90E2));
				label.setForeground(Color.WHITE);
				label.setOpaque(true);
			}
			else{
				// Default style for other days
				label.setBackground(table.getBackground());
				label.setForeground(table.getForeground());
				// If it's today, draw a border around it
				if(cellDate != null && cellDate.equals(LocalDate.now()))
					label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
				else
					label.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
				label.setOpaque(true);
			}

			// Center the text horizontally and vertically
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setVerticalAlignment(SwingConstants.CENTER);

			return label;
		}
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}


		SwingUtilities.invokeLater(() -> {
			final DatePickerDialog dialog = new DatePickerDialog(null, LocalDate.now());
			final LocalDate date = dialog.showDialog();

			System.out.println(date);
		});
	}

}
