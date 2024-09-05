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

import io.github.mtrevisan.familylegacy.flef.db.EntityManager;
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
import java.util.TreeMap;
import java.util.function.Function;

import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordLocalizedTextID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordText;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordType;


public class SearchPlacePanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -2826101432109515521L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;
	private static final int TABLE_INDEX_NAME = 3;
	private static final int TABLE_INDEX_LOCALE = 4;
	private static final int TABLE_INDEX_TYPE = 5;

	private static final int TABLE_PREFERRED_WIDTH_IDENTIFIER = 250;
	private static final int TABLE_PREFERRED_WIDTH_NAME = 150;
	private static final int TABLE_PREFERRED_WIDTH_LOCALE = 43;
	private static final int TABLE_PREFERRED_WIDTH_TYPE = 180;

	private static final String TABLE_NAME = "place";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	private static final String TABLE_NAME_LOCALIZED_TEXT_JUNCTION = "localized_text_junction";


	public static SearchPlacePanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new SearchPlacePanel(store);
	}


	private SearchPlacePanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		super(store);


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_IDENTIFIER, 0, TABLE_PREFERRED_WIDTH_IDENTIFIER);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_NAME, TABLE_PREFERRED_WIDTH_NAME);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_LOCALE, 0, TABLE_PREFERRED_WIDTH_LOCALE);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_TYPE, 0, TABLE_PREFERRED_WIDTH_TYPE);
	}

	@Override
	public String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier", "Name", "Locale", "Type"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT,
			SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, textComparator, textComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordIdentifier(container);
			final String name = extractRecordName(container);
			final String nameLocale = extractRecordLocale(container);
			final List<Map<String, Object>> transcribedNames = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
				key, EntityManager::extractRecordReferenceType, EntityManager.LOCALIZED_TEXT_TYPE_NAME);
			final String type = extractRecordType(container);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(identifier)
				.add(name)
				.add(nameLocale);
			for(final Map<String, Object> transcribedName : transcribedNames)
				filter.add(extractRecordText(transcribedName));
			filter.add(type);
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(name, row, TABLE_INDEX_NAME);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);

			tableData.add(new SearchAllRecord(key, TABLE_NAME, filterData, name));

			row ++;
		}
	}


	private <T> List<Map<String, Object>> extractReferences(final String fromTable, final Integer selectedRecordID,
			final Function<Map<String, Object>, T> filter, final T filterValue){
		final List<Map<String, Object>> matchedRecords = new ArrayList<>(0);
		final NavigableMap<Integer, Map<String, Object>> localizedTexts = getRecords(TABLE_NAME_LOCALIZED_TEXT);
		final NavigableMap<Integer, Map<String, Object>> records = getRecords(fromTable);
		records.forEach((key, value) -> {
			if(((filter == null || Objects.equals(filterValue, filter.apply(value)))
					&& TABLE_NAME.equals(extractRecordReferenceTable(value))
					&& Objects.equals(selectedRecordID, extractRecordReferenceID(value)))){
				final Map<String, Object> localizedText = localizedTexts.get(extractRecordLocalizedTextID(value));
				matchedRecords.add(localizedText);
			}
		});
		return matchedRecords;
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
			public void onRecordSelect(final String table, final Integer id){
				System.out.println("onRecordSelect " + table + " " + id);
			}

			@Override
			public void onRecordEdit(final String table, final Integer id){
				System.out.println("onRecordEdit " + table + " " + id);
			}
		};

		EventQueue.invokeLater(() -> {
			final SearchPlacePanel panel = create(store);
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
