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
package io.github.mtrevisan.familylegacy.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.social.FanEntry;
import io.github.mtrevisan.familylegacy.social.FanResult;
import io.github.mtrevisan.familylegacy.social.FanSuggestion;
import io.github.mtrevisan.familylegacy.social.FanText;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;


/**
 * The FAN table (Friends, Associates, Neighbors) of the dossier of an individual: the people who appear with him in the documents,
 * the strongest link first, and the hints of where to search next.
 * <p>
 * It looks like a section of the dossier (a header that opens and closes it) but it is a table: person, shared events, roles of the
 * subject and of the other person, period, place. The relatives are grey and last. A click on the name opens the record of that person.
 * The hints are only text: nothing is created or changed because of them.
 * </p>
 */
final class FanSectionPanel extends JPanel{

	private static final Color KIN_COLOR = new Color(150, 145, 135);
	private static final int ROW_HEIGHT = 20;
	/** The width asked for: the table is stretched to the width of the dossier. */
	private static final int MINIMUM_WIDTH = 100;
	/** The most people listed: the strongest ones. */
	private static final int MAX_ROWS = 100;
	/** The most events listed in the tooltip of a row. */
	private static final int MAX_TOOLTIP_EVENTS = 8;

	private static final int COLUMN_PERSON = 0;
	private static final int COLUMN_EVENTS = 1;
	private static final int COLUMN_ROLES = 2;
	private static final int COLUMN_PERIOD = 3;
	private static final int COLUMN_PLACE = 4;
	private static final String[] COLUMN_KEYS = {"text.fan.column.person", "text.fan.column.events", "text.fan.column.roles",
		"text.fan.column.period", "text.fan.column.place"};
	/** The relative width of the columns. */
	private static final int[] COLUMN_WIDTHS = {125, 44, 105, 55, 60};


	private final FLEFModel model;
	private final FanResult fan;
	private final List<FanEntry> shown;
	private final JPanel contentArea = new JPanel(new BorderLayout());
	private final IndividualDossierPanel.ArrowIndicator arrowIndicator = new IndividualDossierPanel.ArrowIndicator();


	/**
	 * @param model      the model the table was built from
	 * @param fan        the table to show
	 * @param opener     opens the editor of a record
	 * @param expanded   whether the table starts open
	 * @param onToggle   told whether the table is open each time the user opens or closes it, to remember it
	 */
	FanSectionPanel(final FLEFModel model, final FanResult fan, final Consumer<FLEFRecord> opener, final boolean expanded,
			final Consumer<Boolean> onToggle){
		this.model = model;
		this.fan = fan;
		this.shown = (fan.entries().size() > MAX_ROWS? fan.entries().subList(0, MAX_ROWS): fan.entries());

		setLayout(new BorderLayout());
		setBackground(IndividualDossierPanel.SECTION_BACKGROUND);

		final JPanel headerBar = new JPanel(new MigLayout("ins 6 10 6 10,fillx", "[]4[grow]", "[]"));
		headerBar.setBackground(IndividualDossierPanel.HEADER_BACKGROUND);
		headerBar.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, IndividualDossierPanel.HEADER_BORDER));
		headerBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		final JLabel title = new JLabel(I18N.t("enum.dossier.section.fan") + " (" + fan.entries().size() + ")");
		title.setFont(IndividualDossierPanel.HEADER_FONT);
		title.setForeground(IndividualDossierPanel.HEADER_TEXT);
		headerBar.add(arrowIndicator);
		headerBar.add(title, "growx");
		headerBar.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				setExpanded(!contentArea.isVisible());
				onToggle.accept(contentArea.isVisible());
			}

			@Override
			public void mouseEntered(final MouseEvent e){
				headerBar.setBackground(IndividualDossierPanel.HEADER_BACKGROUND_HOVER);
			}

			@Override
			public void mouseExited(final MouseEvent e){
				headerBar.setBackground(IndividualDossierPanel.HEADER_BACKGROUND);
			}
		});

		buildContent(opener);

		add(headerBar, BorderLayout.NORTH);
		add(contentArea, BorderLayout.CENTER);
		setExpanded(expanded);
	}


	/** The width does not depend on the columns or on the text of the hints: the table takes the width of the dossier. */
	@Override
	public Dimension getPreferredSize(){
		return new Dimension(MINIMUM_WIDTH, super.getPreferredSize().height);
	}

	@Override
	public Dimension getMinimumSize(){
		return new Dimension(0, super.getMinimumSize().height);
	}

	private void buildContent(final Consumer<FLEFRecord> opener){
		contentArea.setBackground(IndividualDossierPanel.SECTION_BACKGROUND);

		final JTable table = new JTable(new FanTableModel()){
			@Override
			public String getToolTipText(final MouseEvent event){
				final int row = rowAtPoint(event.getPoint());
				return (row >= 0? tooltipOf(shown.get(convertRowIndexToModel(row))): null);
			}
		};
		table.setRowHeight(ROW_HEIGHT);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setBackground(IndividualDossierPanel.SECTION_BACKGROUND);
		table.setFillsViewportHeight(false);
		table.setAutoCreateRowSorter(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		//the hand is shown on the names only, as they are the only ones that can be clicked
		table.addMouseMotionListener(new MouseAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				final boolean onName = (table.rowAtPoint(e.getPoint()) >= 0
					&& table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint())) == COLUMN_PERSON);
				table.setCursor(Cursor.getPredefinedCursor(onName? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
			}
		});
		table.getTableHeader().setReorderingAllowed(false);
		table.setDefaultRenderer(Object.class, new FanCellRenderer());
		for(int i = 0; i < COLUMN_WIDTHS.length; i ++){
			final TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(COLUMN_WIDTHS[i]);
		}
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				final int row = table.rowAtPoint(e.getPoint());
				//only the name opens the person: the other columns are plain text
				if(row < 0 || !SwingUtilities.isLeftMouseButton(e)
						|| table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint())) != COLUMN_PERSON)
					return;

				final FLEFRecord record = model.getRecordById(shown.get(table.convertRowIndexToModel(row)).personId());
				if(record != null)
					opener.accept(record);
			}
		});

		final JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
		tablePanel.add(table, BorderLayout.CENTER);
		contentArea.add(tablePanel, BorderLayout.CENTER);

		if(!fan.suggestions().isEmpty())
			contentArea.add(buildSuggestions(), BorderLayout.SOUTH);
	}

	private JPanel buildSuggestions(){
		final String subjectName = nameOf(fan.subjectId());
		final JPanel panel = new JPanel(new MigLayout("ins 6 10 6 10,wrap 1,fillx,gapy 2", "[grow,fill]", "[]"));
		panel.setBackground(IndividualDossierPanel.SECTION_BACKGROUND);

		final JLabel heading = new JLabel(I18N.t("text.fan.suggestions"));
		heading.setFont(IndividualDossierPanel.SUBTITLE_FONT);
		heading.setForeground(IndividualDossierPanel.SUBTITLE_COLOR);
		panel.add(heading);

		for(final FanSuggestion suggestion : fan.suggestions()){
			//a single line, clipped if too long, as the rows of the dossier: the whole reasoning is in the tooltip
			final JLabel text = new JLabel(FanText.title(suggestion, subjectName, this::nameOf));
			text.setFont(IndividualDossierPanel.VALUE_FONT);
			text.setForeground(IndividualDossierPanel.VALUE_COLOR);
			text.setToolTipText(FanText.explanation(suggestion, subjectName, this::nameOf));
			panel.add(text, "growx,wmin 0");
		}
		return panel;
	}

	private void setExpanded(final boolean expanded){
		contentArea.setVisible(expanded);
		arrowIndicator.setExpanded(expanded);

		revalidate();
		repaint();
	}


	private String nameOf(final String individualId){
		for(final FanEntry entry : fan.entries())
			if(entry.personId().equals(individualId))
				return entry.name();

		final FLEFRecord record = model.getRecordById(individualId);
		return (record != null? IndividualReader.displayText(record): individualId);
	}

	/** @return what the row says in full: the roles, the shared events, how strong the link is, and whether the person is a relative */
	private String tooltipOf(final FanEntry entry){
		final StringBuilder text = new StringBuilder("<html><b>").append(escape(entry.name())).append("</b><br>")
			.append(escape(FanText.rolePair(entry)))
			.append("<br>").append(escape(I18N.tf("text.fan.tooltip.strength", entry.score())));
		if(entry.kin())
			text.append("<br>").append(escape(I18N.t("text.fan.tooltip.kin")));
		if(entry.roleTotal() > 0)
			text.append("<br>").append(escape(I18N.tf("text.fan.tooltip.role_total", entry.roleTotal())));

		text.append("<br>").append(escape(I18N.t("text.fan.tooltip.events")));
		final List<String> eventIds = entry.eventIds();
		for(int i = 0; i < Math.min(eventIds.size(), MAX_TOOLTIP_EVENTS); i ++){
			final FLEFRecord event = model.getRecordById(eventIds.get(i));
			text.append("<br>&nbsp;&nbsp;").append(escape(event != null? RecordDisplay.describe(event, model): eventIds.get(i)));
		}
		if(eventIds.size() > MAX_TOOLTIP_EVENTS)
			text.append("<br>&nbsp;&nbsp;...");
		text.append("<br><i>").append(escape(I18N.t("text.fan.tooltip.open"))).append("</i></html>");
		return text.toString();
	}

	private static String escape(final String text){
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}


	/** The rows of the table: the people, the strongest first (the user can sort by any column). */
	private final class FanTableModel extends AbstractTableModel{

		@Override
		public int getRowCount(){
			return shown.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMN_KEYS.length;
		}

		@Override
		public String getColumnName(final int column){
			return I18N.t(COLUMN_KEYS[column]);
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == COLUMN_EVENTS? Integer.class: String.class);
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final FanEntry entry = shown.get(row);
			return switch(column){
				case COLUMN_PERSON -> entry.name();
				case COLUMN_EVENTS -> entry.sharedEvents();
				case COLUMN_ROLES -> FanText.rolePair(entry);
				case COLUMN_PERIOD -> FanText.period(entry);
				case COLUMN_PLACE -> FanText.places(entry);
				default -> null;
			};
		}

		/** @return whether the person of the row is a relative of the subject */
		boolean isKin(final int row){
			return shown.get(row).kin();
		}

	}

	/** Greys the relatives and puts the number of events on the right. */
	private static final class FanCellRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final Component component = super.getTableCellRendererComponent(table, value, false, false, row, column);
			setFont(IndividualDossierPanel.VALUE_FONT);
			setHorizontalAlignment(column == COLUMN_EVENTS? SwingConstants.RIGHT: SwingConstants.LEFT);
			final boolean kin = ((FanTableModel)table.getModel()).isKin(table.convertRowIndexToModel(row));
			setForeground(kin? KIN_COLOR: IndividualDossierPanel.VALUE_COLOR);
			setBackground(row % 2 == 1? IndividualDossierPanel.ROW_BACKGROUND_ALT: IndividualDossierPanel.SECTION_BACKGROUND);
			setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
			return component;
		}

	}

}
