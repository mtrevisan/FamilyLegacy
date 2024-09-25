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
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
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
import java.util.List;
import java.util.Map;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordSuperType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordSuperTypeID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTypeID;


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


	public static SearchEventPanel create(){
		return new SearchEventPanel();
	}


	private SearchEventPanel(){
		super();


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
		return EntityManager.NODE_EVENT;
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


		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_EVENT);
		final Map<Integer, Map<String, Object>> types = Repository.findAllNavigable(EntityManager.NODE_EVENT_TYPE);
		final Map<Integer, Map<String, Object>> superTypes = Repository.findAllNavigable(EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<Integer, Map<String, Object>> places = Repository.findAllNavigable(EntityManager.NODE_PLACE);
		final Map<Integer, Map<String, Object>> historicDates = Repository.findAllNavigable(EntityManager.NODE_HISTORIC_DATE);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(int i = 0, length = records.size(); i < length; i ++){
			final Map<String, Object> record = records.get(i);

			final Integer recordID = extractRecordID(record);
			final Integer typeID = extractRecordTypeID(record);
			final Map<String, Object> eventType = types.get(typeID);
			final String type = extractRecordType(eventType);
			final Integer superTypeID = extractRecordSuperTypeID(eventType);
			final String superType = (superTypeID != null? extractRecordSuperType(superTypes.get(superTypeID)): null);
			final String description = extractRecordDescription(record);
			final Integer placeID = extractRecordPlaceID(record);
			final String place = (placeID != null? extractRecordName(places.get(placeID)): null);
			final Integer dateID = extractRecordDateID(record);
			final Map<String, Object> dateEntry = (dateID != null? historicDates.get(dateID): null);
			final String dateValue = extractRecordDate(dateEntry);
			final LocalDate parsedDate = DateParser.parse(dateValue);
			final Integer year = (parsedDate != null? parsedDate.getYear(): null);
			final FilterString filter = FilterString.create().add(recordID).add(superType).add(type).add(description).add(place).add(year);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);
			model.setValueAt(description, row, TABLE_INDEX_DESCRIPTION);
			model.setValueAt(place, row, TABLE_INDEX_PLACE);
			model.setValueAt(year, row, TABLE_INDEX_DATE);

			tableData.add(new SearchAllRecord(recordID, EntityManager.NODE_EVENT, filterData, description));

			row++;
		}
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("type_id", 1);
		event1.put("description", "a birth");
		event1.put("place_id", 1);
		event1.put("date_id", 1);
event1.put("reference_table", "person");
event1.put("reference_id", 1);
		Repository.upsert(event1, EntityManager.NODE_EVENT);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("super_type_id", 2);
		eventType1.put("type", "birth");
		eventType1.put("category", "birth");
		Repository.upsert(eventType1, EntityManager.NODE_EVENT_TYPE);

		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("super_type", "Historical events");
		Repository.upsert(eventSuperType1, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("super_type", "Personal origins");
		Repository.upsert(eventSuperType2, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("super_type", "Physical description");
		Repository.upsert(eventSuperType3, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("super_type", "Citizenship and migration");
		Repository.upsert(eventSuperType4, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("super_type", "Real estate assets");
		Repository.upsert(eventSuperType5, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("super_type", "Education");
		Repository.upsert(eventSuperType6, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType7 = new HashMap<>();
		eventSuperType7.put("super_type", "Work and Career");
		Repository.upsert(eventSuperType7, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType8 = new HashMap<>();
		eventSuperType8.put("super_type", "Legal Events and Documents");
		Repository.upsert(eventSuperType8, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType9 = new HashMap<>();
		eventSuperType9.put("super_type", "Health problems and habits");
		Repository.upsert(eventSuperType9, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType10 = new HashMap<>();
		eventSuperType10.put("super_type", "Marriage and family life");
		Repository.upsert(eventSuperType10, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType11 = new HashMap<>();
		eventSuperType11.put("super_type", "Military");
		Repository.upsert(eventSuperType11, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType12 = new HashMap<>();
		eventSuperType12.put("super_type", "Confinement");
		Repository.upsert(eventSuperType12, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType13 = new HashMap<>();
		eventSuperType13.put("super_type", "Transfers and travel");
		Repository.upsert(eventSuperType13, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType14 = new HashMap<>();
		eventSuperType14.put("super_type", "Accolades");
		Repository.upsert(eventSuperType14, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType15 = new HashMap<>();
		eventSuperType15.put("super_type", "Death and burial");
		Repository.upsert(eventSuperType15, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("super_type", "Others");
		Repository.upsert(eventSuperType16, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("super_type", "Religious events");
		Repository.upsert(eventSuperType17, EntityManager.NODE_EVENT_SUPER_TYPE);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		Repository.upsert(place1, EntityManager.NODE_PLACE);

		final Map<String, Object> date1 = new HashMap<>();
		date1.put("date", "18 OCT 2000");
		Repository.upsert(date1, EntityManager.NODE_HISTORIC_DATE);

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
			final SearchEventPanel panel = create();
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
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
