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
package io.github.mtrevisan.familylegacy.flef.ui.panels.searches;

import io.github.mtrevisan.familylegacy.flef.helpers.parsers.DateParser;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;

import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class SearchEventPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -4869611094243229616L;

	private static final int TABLE_INDEX_TYPE = 2;
	private static final int TABLE_INDEX_DESCRIPTION = 3;
	private static final int TABLE_INDEX_PLACE = 4;
	private static final int TABLE_INDEX_DATE = 5;

	private static final int TABLE_PREFERRED_WIDTH_TYPE = 80;
	private static final int TABLE_PREFERRED_WIDTH_DESCRIPTION = 250;
	private static final int TABLE_PREFERRED_WIDTH_PLACE = 250;
	private static final int TABLE_PREFERRED_WIDTH_YEAR = 43;

	private static final String TABLE_NAME = "event";
	private static final String TABLE_NAME_EVENT_TYPE = "event_type";
	private static final String TABLE_NAME_EVENT_SUPER_TYPE = "event_super_type";
	private static final String TABLE_NAME_PLACE = "place";
	private static final String TABLE_NAME_HISTORIC_DATE = "historic_date";
	private static final String TABLE_NAME_CALENDAR = "calendar";


	public static SearchEventPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new SearchEventPanel(store);
	}


	private SearchEventPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		super(store);


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_TYPE, 0, TABLE_PREFERRED_WIDTH_TYPE);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_DESCRIPTION, 0, TABLE_PREFERRED_WIDTH_DESCRIPTION);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_DATE, TABLE_PREFERRED_WIDTH_YEAR);
	}

	@Override
	public String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Type", "Description", "Place", "Date"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT,
			SwingConstants.RIGHT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, textComparator, numericComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
		final Map<Integer, Map<String, Object>> types = getRecords(TABLE_NAME_EVENT_TYPE);
		final Map<Integer, Map<String, Object>> superTypes = getRecords(TABLE_NAME_EVENT_SUPER_TYPE);
		final Map<Integer, Map<String, Object>> places = getRecords(TABLE_NAME_PLACE);
		final Map<Integer, Map<String, Object>> historicDates = getRecords(TABLE_NAME_HISTORIC_DATE);
		final Map<Integer, Map<String, Object>> calendars = getRecords(TABLE_NAME_CALENDAR);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final Integer typeID = extractRecordTypeID(container);
			final Map<String, Object> eventType = types.get(typeID);
			final String type = extractRecordType(eventType);
			final Integer superTypeID = extractRecordSuperTypeID(eventType);
			final String superType = (superTypeID != null? extractRecordSuperType(superTypes.get(superTypeID)): null);
			final String description = extractRecordDescription(container);
			final Integer placeID = extractRecordPlaceID(container);
			final String place = (placeID != null? extractRecordName(places.get(placeID)): null);
			final Integer dateID = extractRecordDateID(container);
			final Map<String, Object> dateEntry = (dateID != null? historicDates.get(dateID): null);
			final String dateValue = extractRecordDate(dateEntry);
			final LocalDate parsedDate = DateParser.parse(dateValue);
			final Integer year = (parsedDate != null? parsedDate.getYear(): null);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(superType)
				.add(type)
				.add(description)
				.add(place)
				.add(year);
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);
			model.setValueAt(description, row, TABLE_INDEX_DESCRIPTION);
			model.setValueAt(place, row, TABLE_INDEX_PLACE);
			model.setValueAt(year, row, TABLE_INDEX_DATE);

			tableData.add(new SearchAllRecord(key, TABLE_NAME, filterData, description));

			row ++;
		}
	}


	private static Integer extractRecordSuperTypeID(final Map<String, Object> record){
		return (Integer)record.get("super_type_id");
	}

	private static Integer extractRecordTypeID(final Map<String, Object> record){
		return (Integer)record.get("type_id");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordSuperType(final Map<String, Object> record){
		return (String)record.get("super_type");
	}

	private static String extractRecordDescription(final Map<String, Object> record){
		return (String)record.get("description");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static String extractRecordName(final Map<String, Object> record){
		return (String)record.get("name");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	private static String extractRecordDate(final Map<String, Object> record){
		return (record != null? (String)record.get("date"): null);
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event = new HashMap<>();
		event.put("id", 1);
		event.put("type_id", 1);
		event.put("description", "a birth");
		event.put("place_id", 1);
		event.put("date_id", 1);
		event.put("reference_table", "person");
		event.put("reference_id", 1);
		events.put((Integer)event.get("id"), event);

		final TreeMap<Integer, Map<String, Object>> eventTypes = new TreeMap<>();
		store.put("event_type", eventTypes);
		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("super_type_id", 2);
		eventType1.put("type", "birth");
		eventType1.put("category", "birth");
		eventTypes.put((Integer)eventType1.get("id"), eventType1);

		final TreeMap<Integer, Map<String, Object>> eventSuperTypes = new TreeMap<>();
		store.put("event_super_type", eventSuperTypes);
		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("id", 1);
		eventSuperType1.put("super_type", "Historical events");
		eventSuperTypes.put((Integer)eventSuperType1.get("id"), eventSuperType1);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("id", 2);
		eventSuperType2.put("super_type", "Personal origins");
		eventSuperTypes.put((Integer)eventSuperType2.get("id"), eventSuperType2);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("id", 3);
		eventSuperType3.put("super_type", "Physical description");
		eventSuperTypes.put((Integer)eventSuperType3.get("id"), eventSuperType3);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("id", 4);
		eventSuperType4.put("super_type", "Citizenship and migration");
		eventSuperTypes.put((Integer)eventSuperType4.get("id"), eventSuperType4);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("id", 5);
		eventSuperType5.put("super_type", "Real estate assets");
		eventSuperTypes.put((Integer)eventSuperType5.get("id"), eventSuperType5);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("id", 6);
		eventSuperType6.put("super_type", "Education");
		eventSuperTypes.put((Integer)eventSuperType6.get("id"), eventSuperType6);
		final Map<String, Object> eventSuperType7 = new HashMap<>();
		eventSuperType7.put("id", 7);
		eventSuperType7.put("super_type", "Work and Career");
		eventSuperTypes.put((Integer)eventSuperType7.get("id"), eventSuperType7);
		final Map<String, Object> eventSuperType8 = new HashMap<>();
		eventSuperType8.put("id", 8);
		eventSuperType8.put("super_type", "Legal Events and Documents");
		eventSuperTypes.put((Integer)eventSuperType8.get("id"), eventSuperType8);
		final Map<String, Object> eventSuperType9 = new HashMap<>();
		eventSuperType9.put("id", 9);
		eventSuperType9.put("super_type", "Health problems and habits");
		eventSuperTypes.put((Integer)eventSuperType9.get("id"), eventSuperType9);
		final Map<String, Object> eventSuperType10 = new HashMap<>();
		eventSuperType10.put("id", 10);
		eventSuperType10.put("super_type", "Marriage and family life");
		eventSuperTypes.put((Integer)eventSuperType10.get("id"), eventSuperType10);
		final Map<String, Object> eventSuperType11 = new HashMap<>();
		eventSuperType11.put("id", 11);
		eventSuperType11.put("super_type", "Military");
		eventSuperTypes.put((Integer)eventSuperType11.get("id"), eventSuperType11);
		final Map<String, Object> eventSuperType12 = new HashMap<>();
		eventSuperType12.put("id", 12);
		eventSuperType12.put("super_type", "Confinement");
		eventSuperTypes.put((Integer)eventSuperType12.get("id"), eventSuperType12);
		final Map<String, Object> eventSuperType13 = new HashMap<>();
		eventSuperType13.put("id", 13);
		eventSuperType13.put("super_type", "Transfers and travel");
		eventSuperTypes.put((Integer)eventSuperType13.get("id"), eventSuperType13);
		final Map<String, Object> eventSuperType14 = new HashMap<>();
		eventSuperType14.put("id", 14);
		eventSuperType14.put("super_type", "Accolades");
		eventSuperTypes.put((Integer)eventSuperType14.get("id"), eventSuperType14);
		final Map<String, Object> eventSuperType15 = new HashMap<>();
		eventSuperType15.put("id", 15);
		eventSuperType15.put("super_type", "Death and burial");
		eventSuperTypes.put((Integer)eventSuperType15.get("id"), eventSuperType15);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("id", 16);
		eventSuperType16.put("super_type", "Others");
		eventSuperTypes.put((Integer)eventSuperType16.get("id"), eventSuperType16);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("id", 17);
		eventSuperType17.put("super_type", "Religious events");
		eventSuperTypes.put((Integer)eventSuperType17.get("id"), eventSuperType17);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		places.put((Integer)place1.get("id"), place1);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "18 OCT 2000");
		dates.put((Integer)date1.get("id"), date1);

		final RecordListenerInterface linkListener = new RecordListenerInterface(){
			@Override
			public void onRecordSelect(final String table, final Integer id){
				System.out.println("onRecordSelect " + table + " " + id);
			}

			@Override
			public void onRecordEdit(final String table, final Integer id){
				System.out.println("onRecordEdit " + table + " " + id);
			}
		};

		EventQueue.invokeLater(() -> {
			final SearchEventPanel panel = create(store);
			panel.setLinkListener(linkListener);
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
