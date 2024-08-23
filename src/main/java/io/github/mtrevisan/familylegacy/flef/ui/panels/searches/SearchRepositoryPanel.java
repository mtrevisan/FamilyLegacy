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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;


public class SearchRepositoryPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = 6415793447255484171L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;
	private static final int TABLE_INDEX_TYPE = 3;
	private static final int TABLE_INDEX_PERSON = 4;
	private static final int TABLE_INDEX_PLACE = 5;

	private static final int TABLE_PREFERRED_WIDTH_IDENTIFIER = 250;
	private static final int TABLE_PREFERRED_WIDTH_TYPE = 180;
	private static final int TABLE_PREFERRED_WIDTH_NAME = 150;
	private static final int TABLE_PREFERRED_WIDTH_PLACE = 250;

	private static final String TABLE_NAME = "repository";
	private static final String TABLE_NAME_LOCALIZED_PERSON_NAME = "localized_person_name";
	private static final String TABLE_NAME_PERSON_NAME = "person_name";
	private static final String TABLE_NAME_PLACE = "place";


	public static SearchRepositoryPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new SearchRepositoryPanel(store);
	}


	private SearchRepositoryPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		super(store);


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_IDENTIFIER, 0, TABLE_PREFERRED_WIDTH_IDENTIFIER);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_TYPE, 0, TABLE_PREFERRED_WIDTH_TYPE);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PERSON, TABLE_PREFERRED_WIDTH_NAME);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
	}

	@Override
	public String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier", "Type", "Name", "Place"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT,
			SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<String> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, textComparator, textComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
		final Map<Integer, Map<String, Object>> places = getRecords(TABLE_NAME_PLACE);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordIdentifier(container);
			final String type = extractRecordType(container);
			final Integer personID = extractRecordPersonID(container);
			final String personName = extractFirstName(personID);
			final List<String> personAllNames = extractAllNames(personID);
			final Integer placeID = extractRecordPlaceID(container);
			final String place = (placeID != null? extractRecordName(places.get(placeID)): null);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(identifier)
				.add(type);
			for(final String name : personAllNames)
				filter.add(name);
			filter.add(place);
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);
			model.setValueAt(personName, row, TABLE_INDEX_PERSON);
			model.setValueAt(place, row, TABLE_INDEX_PLACE);

			tableData.add(new SearchAllRecord(key, TABLE_NAME, filterData, identifier));

			row ++;
		}
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static String extractRecordName(final Map<String, Object> record){
		return (String)record.get("name");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static Integer extractRecordPersonNameID(final Map<String, Object> record){
		return (Integer)record.get("person_name_id");
	}

	private static String extractRecordPersonalName(final Map<String, Object> record){
		return (String)record.get("personal_name");
	}

	private static String extractRecordFamilyName(final Map<String, Object> record){
		return (String)record.get("family_name");
	}

	private String extractFirstName(final Integer personID){
		return getRecords(TABLE_NAME_PERSON_NAME)
			.values().stream()
			.filter(entry -> Objects.equals(personID, extractRecordPersonID(entry)))
			.map(SearchRepositoryPanel::extractName)
			.findFirst()
			.orElse(null);
	}

	private static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
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
					.map(SearchRepositoryPanel::extractName)
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


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put("source", sources);
		final Map<String, Object> source1 = new HashMap<>();
		source1.put("id", 1);
		source1.put("identifier", "source 1");
		source1.put("type", "marriage certificate");
		source1.put("author", "author 1");
		source1.put("place_id", 1);
		source1.put("date_id", 1);
		source1.put("location", "location 1");
		sources.put((Integer)source1.get("id"), source1);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("id", 2);
		source2.put("identifier", "source 2");
		source2.put("type", "newspaper");
		source2.put("author", "author 2");
		source2.put("location", "location 2");
		sources.put((Integer)source2.get("id"), source2);

		final TreeMap<Integer, Map<String, Object>> repositories = new TreeMap<>();
		store.put("repository", repositories);
		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("id", 1);
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		repositories.put((Integer)repository1.get("id"), repository1);

		final TreeMap<Integer, Map<String, Object>> historicDates = new TreeMap<>();
		store.put("historic_date", historicDates);
		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("id", 1);
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("calendar_original_id", 1);
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		historicDates.put((Integer)historicDate1.get("id"), historicDate1);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		place1.put("type", "province");
		place1.put("coordinate", "45.65, 12.19");
		place1.put("coordinate_system", "WGS84");
		place1.put("coordinate_credibility", "certain");
		places.put((Integer)place1.get("id"), place1);

		final RecordListenerInterface linkListener = new RecordListenerInterface(){
			@Override
			public void onRecordSelected(final String table, final Integer id){
				System.out.println("onRecordSelected " + table + " " + id);
			}
		};

		EventQueue.invokeLater(() -> {
			final SearchRepositoryPanel panel = create(store);
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
