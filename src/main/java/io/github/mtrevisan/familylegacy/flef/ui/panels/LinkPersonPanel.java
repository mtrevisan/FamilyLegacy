/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import io.github.mtrevisan.familylegacy.flef.helpers.parsers.DateParser;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.SearchParser;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;


public class LinkPersonPanel extends JPanel implements FilteredTablePanelInterface{

	@Serial
	private static final long serialVersionUID = -1361635723036701664L;

	private static final Color GRID_COLOR = new Color(230, 230, 230);
	private static final int TABLE_PREFERRED_WIDTH_ID = 25;

	private static final int TABLE_INDEX_ID = 0;
	private static final int TABLE_INDEX_FILTER = 1;
	private static final int TABLE_INDEX_PERSON_NAME = 2;
	private static final int TABLE_INDEX_PERSON_BIRTH_YEAR = 3;
	private static final int TABLE_INDEX_PERSON_BIRTH_PLACE = 4;
	private static final int TABLE_INDEX_PERSON_DEATH_YEAR = 5;
	private static final int TABLE_INDEX_PERSON_DEATH_PLACE = 6;
	private static final int TABLE_ROWS_SHOWN = 5;

	private static final int TABLE_PREFERRED_WIDTH_YEAR = 43;
	private static final int TABLE_PREFERRED_WIDTH_PLACE = 250;
	private static final int TABLE_PREFERRED_WIDTH_NAME = 150;

	private static final String TABLE_NAME_PERSON = "person";
	private static final String TABLE_NAME_LOCALIZED_PERSON_NAME = "localized_person_name";
	private static final String TABLE_NAME_PERSON_NAME = "person_name";
	private static final String TABLE_NAME_EVENT = "event";
	private static final String TABLE_NAME_EVENT_TYPE = "event_type";
	private static final String TABLE_NAME_HISTORIC_DATE = "historic_date";
	private static final String TABLE_NAME_CALENDAR = "calendar";
	private static final String TABLE_NAME_PLACE = "place";

	private static final String EVENT_TYPE_CATEGORY_BIRTH = "birth";
	private static final String EVENT_TYPE_CATEGORY_DEATH = "death";

	private static final String NO_DATA = StringUtils.EMPTY;


	private JTable recordTable;
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	private LinkListenerInterface linkListener;


	public static LinkPersonPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new LinkPersonPanel(store);
	}


	private LinkPersonPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		this.store = store;
	}


	public final LinkPersonPanel withLinkListener(final LinkListenerInterface linkListener){
		this.linkListener = linkListener;

		return this;
	}

	public void initComponents(){
		//store components:
		recordTable = new JTable(new DefaultTableModel(getTableColumnNames(), 0){
			@Serial
			private static final long serialVersionUID = 6564164142990308313L;

			@Override
			public Class<?> getColumnClass(final int column){
				return String.class;
			}

			@Override
			public boolean isCellEditable(final int row, final int column){
				return false;
			}
		});


		recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		recordTable.setFocusable(true);
		recordTable.setGridColor(GRID_COLOR);
		recordTable.setDragEnabled(true);
		recordTable.setDropMode(DropMode.INSERT_ROWS);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_ID, TABLE_PREFERRED_WIDTH_ID);

		//define selection
		recordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recordTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && recordTable.getSelectedRow() >= 0)
					selectAction();
			}
		});

		//add sorter
		recordTable.setAutoCreateRowSorter(true);
		final Comparator<?>[] comparators = getTableColumnComparators();
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(getRecordTableModel());
		final int columnCount = recordTable.getColumnCount();
		for(int columnIndex = 0; columnIndex < columnCount; columnIndex ++){
			final Comparator<?> comparator = comparators[columnIndex];

			if(comparator != null)
				sorter.setComparator(columnIndex, comparator);
		}
		recordTable.setRowSorter(sorter);

		final TableColumnModel columnModel = recordTable.getColumnModel();
		final EmptyBorder cellBorder = new EmptyBorder(new Insets(2, 5, 2, 5));
		final int[] alignments = getTableColumnAlignments();
		final JTableHeader tableHeader = recordTable.getTableHeader();
		final Font tableFont = recordTable.getFont();
		final Font headerFont = tableFont.deriveFont(Font.BOLD);
		tableHeader.setDefaultRenderer(new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
				final Component headerCell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				String cellText = null;
				if(value != null){
					cellText = value.toString();
					final FontMetrics fm = headerCell.getFontMetrics(tableFont);
					final int textWidth = fm.stringWidth(cellText);
					final Insets insets = ((JComponent)headerCell).getInsets();
					final int cellWidth = columnModel.getColumn(column).getWidth()
						- insets.left - insets.right;

					if(textWidth <= cellWidth)
						cellText = null;
				}
				setToolTipText(cellText);

				setBorder(cellBorder);

				final int alignment = alignments[table.convertColumnIndexToModel(column)];
				setHorizontalAlignment(alignment);
				setFont(headerFont);

				return headerCell;
			}
		});

		//add tooltip
		for(int columnIndex = 0; columnIndex < columnCount; columnIndex ++){
			final TableColumn column = columnModel.getColumn(columnIndex);
			final int alignment = alignments[columnIndex];

			column.setCellRenderer(new DefaultTableCellRenderer(){
				@Override
				public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column){
					final Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					String cellText = null;
					if(value != null){
						cellText = value.toString();
						final FontMetrics fm = cell.getFontMetrics(tableFont);
						final int textWidth = fm.stringWidth(cellText);
						final Insets insets = ((JComponent)cell).getInsets();
						final int cellWidth = columnModel.getColumn(column).getWidth()
							- insets.left - insets.right;

						if(textWidth <= cellWidth)
							cellText = null;
					}
					setToolTipText(cellText);

					setBorder(cellBorder);

					setHorizontalAlignment(alignment);

					return cell;
				}
			});
		}

		//hide filter column
		final TableColumn hiddenColumn = columnModel.getColumn(TABLE_INDEX_FILTER);
		columnModel.removeColumn(hiddenColumn);

		final Dimension viewSize = new Dimension();
		viewSize.width = columnModel
			.getTotalColumnWidth();
		viewSize.height = TABLE_ROWS_SHOWN * recordTable.getRowHeight();
		recordTable.setPreferredScrollableViewportSize(viewSize);


		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PERSON_NAME, 0, TABLE_PREFERRED_WIDTH_NAME);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PERSON_BIRTH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PERSON_BIRTH_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PERSON_DEATH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PERSON_DEATH_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);


		initLayout();
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	protected void initLayout(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(new JScrollPane(recordTable), "grow");
	}

	private String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Person",
			"birth year", "birth place",
			"death year", "death place"};
	}

	private int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT,
			SwingConstants.RIGHT, SwingConstants.LEFT,
			SwingConstants.RIGHT, SwingConstants.LEFT};
	}

	private Comparator<?>[] getTableColumnComparators(){
		final Comparator<Object> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator,
			numericComparator, textComparator,
			numericComparator, textComparator};
	}

	private DefaultTableModel getRecordTableModel(){
		return (DefaultTableModel)recordTable.getModel();
	}

	private void selectAction(){
		if(linkListener != null){
			final int viewRowIndex = recordTable.getSelectedRow();
			final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
			final TableModel model = getRecordTableModel();
			final Integer recordIdentifier = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_ID);

			linkListener.onRecordSelected(TABLE_NAME_PERSON, recordIdentifier);
		}
	}


	public void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME_PERSON);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Integer personID : records.keySet()){
			final String personName = extractFirstName(personID);
			final List<String> personAllNames = extractAllNames(personID);
			final Map<String, Object> earliestPersonBirthYearAndPlace = extractEarliestBirthYearAndPlace(personID);
			final Map<String, Object> latestPersonDeathYearAndPlace = extractLatestDeathYearAndPlace(personID);
			final String personBirthYear = (String)earliestPersonBirthYearAndPlace.get("year");
			final String personBirthPlace = (String)earliestPersonBirthYearAndPlace.get("place");
			final String personDeathYear = (String)latestPersonDeathYearAndPlace.get("year");
			final String personDeathPlace = (String)latestPersonDeathYearAndPlace.get("place");
			final FilterString filter = FilterString.create()
				.add(personID);
			for(final String name : personAllNames)
				filter.add(name);
			filter.add(personBirthYear)
				.add(personBirthPlace)
				.add(personDeathYear)
				.add(personDeathPlace);

			model.setValueAt(personID, row, TABLE_INDEX_ID);
			model.setValueAt(filter.toString(), row, TABLE_INDEX_FILTER);
			model.setValueAt((personName != null? personName: NO_DATA), row, TABLE_INDEX_PERSON_NAME);
			model.setValueAt((personBirthYear != null? personBirthYear: NO_DATA), row, TABLE_INDEX_PERSON_BIRTH_YEAR);
			model.setValueAt((personBirthPlace != null? personBirthPlace: NO_DATA), row, TABLE_INDEX_PERSON_BIRTH_PLACE);
			model.setValueAt((personDeathYear != null? personDeathYear: NO_DATA), row, TABLE_INDEX_PERSON_DEATH_YEAR);
			model.setValueAt((personDeathPlace != null? personDeathPlace: NO_DATA), row, TABLE_INDEX_PERSON_DEATH_PLACE);

			row ++;
		}
	}

	@Override
	public void setFilterAndSorting(final String filterText, final List<Map.Entry<String, String>> additionalFields){
		//extract filter
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(filterText, TABLE_INDEX_FILTER);
		//extract ordering
		final List<RowSorter.SortKey> sortKeys = SearchParser.getSortKeys(additionalFields, getTableColumnNames());

		//apply filter and ordering
		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
		sorter.setSortKeys(!sortKeys.isEmpty()? sortKeys: null);
	}


	private NavigableMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	private static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	private String extractFirstName(final Integer personID){
		return getRecords(TABLE_NAME_PERSON_NAME)
			.values().stream()
			.filter(entry -> Objects.equals(personID, extractRecordPersonID(entry)))
			.map(LinkPersonPanel::extractName)
			.findFirst()
			.orElse(null);
	}

	private List<String> extractAllNames(final Integer personID){
		final NavigableMap<Integer, Map<String, Object>> localizedPersonNames = getRecords(TABLE_NAME_LOCALIZED_PERSON_NAME);
		final List<String> names = new ArrayList<>(0);
		getRecords(TABLE_NAME_PERSON_NAME)
			.values().stream()
			.filter(record -> Objects.equals(personID, extractRecordPersonID(record)))
			.forEach(record -> {
				names.add(extractName(record));

				//extract transliterations
				final Integer personNameID = extractRecordID(record);
				localizedPersonNames
					.values().stream()
					.filter(record2 -> Objects.equals(personNameID, extractRecordPersonNameID(record2)))
					.map(LinkPersonPanel::extractName)
					.filter(name -> !name.isEmpty())
					.forEach(names::add);
			});
		return names;
	}

	private static String extractName(final Map<String, Object> record){
		final String personalName = extractRecordPersonalName(record);
		final String familyName = extractRecordFamilyName(record);
		final StringJoiner name = new StringJoiner(", ");
		if(personalName != null)
			name.add(personalName);
		if(familyName != null)
			name.add(familyName);
		return name.toString();
	}

	private Map<String, Object> extractEarliestBirthYearAndPlace(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Map<Integer, Map<String, Object>> places = getRecords(TABLE_NAME_PLACE);
		final Function<Map.Entry<LocalDate, Map<String, Object>>, Map<String, Object>> extractor = entry -> {
			final String year = Integer.toString(entry.getKey().getYear());
			final Integer placeID = extractRecordPlaceID(entry.getValue());
			final String place = (placeID != null? extractRecordName(places.get(placeID)): null);
			final Map<String, Object> result = new HashMap<>(2);
			result.put("year", year);
			result.put("place", place);
			return result;
		};
		final Map<String, Object> result = extractData(personID, EVENT_TYPE_CATEGORY_BIRTH, comparator, extractor);
		return (result != null? result: Collections.emptyMap());
	}

	private Map<String, Object> extractLatestDeathYearAndPlace(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Map<Integer, Map<String, Object>> places = getRecords(TABLE_NAME_PLACE);
		final Function<Map.Entry<LocalDate, Map<String, Object>>, Map<String, Object>> extractor = entry -> {
			final String year = Integer.toString(entry.getKey().getYear());
			final Integer placeID = extractRecordPlaceID(entry.getValue());
			final String place = (placeID != null? extractRecordName(places.get(placeID)): null);
			final Map<String, Object> result = new HashMap<>(2);
			result.put("year", year);
			result.put("place", place);
			return result;
		};
		final Map<String, Object> result = extractData(personID, EVENT_TYPE_CATEGORY_DEATH, comparator.reversed(), extractor);
		return (result != null? result: Collections.emptyMap());
	}

	private <T> T extractData(final Integer referenceID, final String eventTypeCategory, final Comparator<LocalDate> comparator,
			final Function<Map.Entry<LocalDate, Map<String, Object>>, T> extractor){
		final Map<Integer, Map<String, Object>> storeEventTypes = getRecords(TABLE_NAME_EVENT_TYPE);
		final Map<Integer, Map<String, Object>> historicDates = getRecords(TABLE_NAME_HISTORIC_DATE);
		final Map<Integer, Map<String, Object>> calendars = getRecords(TABLE_NAME_CALENDAR);
		final Set<String> eventTypes = getEventTypes(eventTypeCategory);
		return getRecords(TABLE_NAME_EVENT)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(referenceID, extractRecordReferenceID(entry)))
			.filter(entry -> {
				final Integer recordTypeID = extractRecordTypeID(entry);
				final String recordType = extractRecordType(storeEventTypes.get(recordTypeID));
				return eventTypes.contains(recordType);
			})
			.map(entry -> {
				final Map<String, Object> dateEntry = historicDates.get(extractRecordDateID(entry));
				final String dateValue = extractRecordDate(dateEntry);
				final Integer calendarID = extractRecordCalendarID(dateEntry);
				final String calendarType = (calendarID != null? extractRecordType(calendars.get(calendarID)): null);
				final LocalDate parsedDate = DateParser.parse(dateValue, calendarType);
				return (parsedDate != null? new AbstractMap.SimpleEntry<>(parsedDate, entry): null);
			})
			.filter(Objects::nonNull)
			.min(Map.Entry.comparingByKey(comparator))
			.map(extractor)
			.orElse(null);
	}

	private Set<String> getEventTypes(final String category){
		return getRecords(TABLE_NAME_EVENT_TYPE)
			.values().stream()
			.filter(entry -> Objects.equals(category, extractRecordCategory(entry)))
			.map(LinkPersonPanel::extractRecordType)
			.collect(Collectors.toSet());
	}

	private static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	private static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static String extractRecordCategory(final Map<String, Object> record){
		return (String)record.get("category");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordTypeID(final Map<String, Object> record){
		return (Integer)record.get("type_id");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static String extractRecordDate(final Map<String, Object> record){
		return (String)record.get("date");
	}

	private static Integer extractRecordCalendarID(final Map<String, Object> record){
		return (Integer)record.get("calendar_id");
	}

	private static String extractRecordPersonalName(final Map<String, Object> record){
		return (String)record.get("personal_name");
	}

	private static String extractRecordFamilyName(final Map<String, Object> record){
		return (String)record.get("family_name");
	}

	private static String extractRecordName(final Map<String, Object> record){
		return (String)record.get("name");
	}

	private static Integer extractRecordPersonNameID(final Map<String, Object> record){
		return (Integer)record.get("person_name_id");
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		persons.put((Integer)person1.get("id"), person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		persons.put((Integer)person2.get("id"), person2);
		final Map<String, Object> person3 = new HashMap<>();
		person3.put("id", 3);
		persons.put((Integer)person3.get("id"), person3);
		final Map<String, Object> person4 = new HashMap<>();
		person4.put("id", 4);
		persons.put((Integer)person4.get("id"), person4);
		final Map<String, Object> person5 = new HashMap<>();
		person5.put("id", 5);
		persons.put((Integer)person5.get("id"), person5);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "personal name");
		personName1.put("family_name", "family name");
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("personal_name", "personal name 2");
		personName2.put("family_name", "family name 2");
		personName2.put("type", "death name");
		personNames.put((Integer)personName2.get("id"), personName2);
		final Map<String, Object> personName3 = new HashMap<>();
		personName3.put("id", 3);
		personName3.put("person_id", 2);
		personName3.put("personal_name", "personal name 3");
		personName3.put("family_name", "family name 3");
		personName3.put("type", "other name");
		personNames.put((Integer)personName3.get("id"), personName3);

		final TreeMap<Integer, Map<String, Object>> localizedPersonNames = new TreeMap<>();
		store.put("localized_person_name", localizedPersonNames);
		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("id", 1);
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("person_name_id", 1);
		localizedPersonNames.put((Integer)localizedPersonName1.get("id"), localizedPersonName1);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("id", 2);
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("person_name_id", 1);
		localizedPersonNames.put((Integer)localizedPersonName2.get("id"), localizedPersonName2);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("id", 3);
		localizedPersonName3.put("personal_name", "other");
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("person_name_id", 1);
		localizedPersonNames.put((Integer)localizedPersonName3.get("id"), localizedPersonName3);

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type_id", 1);
		event1.put("description", "a birth");
		event1.put("place_id", 1);
		event1.put("date_id", 1);
		event1.put("reference_table", "person");
		event1.put("reference_id", 1);
		events.put((Integer)event1.get("id"), event1);
		final Map<String, Object> event2 = new HashMap<>();
		event2.put("id", 2);
		event2.put("type_id", 1);
		event2.put("description", "another birth");
		event2.put("place_id", 2);
		event2.put("date_id", 2);
		event2.put("reference_table", "person");
		event2.put("reference_id", 1);
		events.put((Integer)event2.get("id"), event2);
		final Map<String, Object> event3 = new HashMap<>();
		event3.put("id", 3);
		event3.put("type_id", 2);
		event3.put("date_id", 1);
		event3.put("reference_table", "person");
		event3.put("reference_id", 2);
		events.put((Integer)event3.get("id"), event3);
		final Map<String, Object> event4 = new HashMap<>();
		event4.put("id", 4);
		event4.put("type_id", 3);
		event4.put("date_id", 1);
		event4.put("place_id", 1);
		event4.put("reference_table", "group");
		event4.put("reference_id", 1);
		events.put((Integer)event4.get("id"), event4);

		final TreeMap<Integer, Map<String, Object>> eventTypes = new TreeMap<>();
		store.put("event_type", eventTypes);
		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("type", "birth");
		eventType1.put("category", EVENT_TYPE_CATEGORY_BIRTH);
		eventTypes.put((Integer)eventType1.get("id"), eventType1);
		final Map<String, Object> eventType2 = new HashMap<>();
		eventType2.put("id", 2);
		eventType2.put("type", "death");
		eventType2.put("category", EVENT_TYPE_CATEGORY_DEATH);
		eventTypes.put((Integer)eventType2.get("id"), eventType2);
		final Map<String, Object> eventType3 = new HashMap<>();
		eventType3.put("id", 3);
		eventType3.put("type", "marriage");
		eventType3.put("category", "union");
		eventTypes.put((Integer)eventType3.get("id"), eventType3);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		places.put((Integer)place1.get("id"), place1);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("id", 2);
		place2.put("identifier", "another place 1");
		place2.put("name", "name of the another place");
		places.put((Integer)place2.get("id"), place2);

		final TreeMap<Integer, Map<String, Object>> historicDates = new TreeMap<>();
		store.put("historic_date", historicDates);
		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("id", 1);
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("calendar_id", 1);
		historicDates.put((Integer)historicDate1.get("id"), historicDate1);
		final Map<String, Object> historicDate2 = new HashMap<>();
		historicDate2.put("id", 2);
		historicDate2.put("date", "1 JAN 1800");
		historicDate2.put("calendar_id", 1);
		historicDates.put((Integer)historicDate2.get("id"), historicDate2);

		final TreeMap<Integer, Map<String, Object>> calendars = new TreeMap<>();
		store.put("calendar", calendars);
		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		calendars.put((Integer)calendar1.get("id"), calendar1);

		final LinkListenerInterface linkListener = new LinkListenerInterface(){
			@Override
			public void onRecordSelected(final String table, final Integer id){
				System.out.println("onRecordSelected " + table + " " + id);
			}
		};

		EventQueue.invokeLater(() -> {
			final LinkPersonPanel panel = create(store)
				.withLinkListener(linkListener);
			panel.initComponents();
			panel.loadData();

			final JFrame frame = new JFrame();
			final Container contentPane = frame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
