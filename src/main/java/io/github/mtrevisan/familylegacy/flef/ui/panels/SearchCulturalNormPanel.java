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


public class SearchCulturalNormPanel extends CommonLinkPanel{

	@Serial
	private static final long serialVersionUID = -4361406306346697722L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;
	private static final int TABLE_INDEX_DESCRIPTION = 3;
	private static final int TABLE_INDEX_PLACE = 4;

	private static final int TABLE_PREFERRED_WIDTH_IDENTIFIER = 250;
	private static final int TABLE_PREFERRED_WIDTH_DESCRIPTION = 150;
	private static final int TABLE_PREFERRED_WIDTH_PLACE = 250;

	private static final String TABLE_NAME_CULTURAL_NORM = "cultural_norm";
	private static final String TABLE_NAME_PLACE = "place";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	private static final String TABLE_NAME_LOCALIZED_TEXT_JUNCTION = "localized_text_junction";


	public static SearchCulturalNormPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new SearchCulturalNormPanel(store);
	}


	private SearchCulturalNormPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		super(store);


		initComponents();
	}


	public final SearchCulturalNormPanel withLinkListener(final RecordListenerInterface linkListener){
		super.setLinkListener(linkListener);

		return this;
	}

	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_IDENTIFIER, 0, TABLE_PREFERRED_WIDTH_IDENTIFIER);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_DESCRIPTION, 0, TABLE_PREFERRED_WIDTH_DESCRIPTION);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME_CULTURAL_NORM;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier", "Description", "Place"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Object> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, textComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME_CULTURAL_NORM);
		final Map<Integer, Map<String, Object>> places = getRecords(TABLE_NAME_PLACE);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordIdentifier(container);
			final String description = extractRecordDescription(container);
			final Integer placeID = extractRecordPlaceID(container);
			final String placeName = (placeID != null? extractRecordName(places.get(placeID)): null);
			final String placeNameLocale = extractRecordLocale(container);
			final List<Map<String, Object>> transcribedPlaceNames = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
				key, SearchCulturalNormPanel::extractRecordReferenceType, "name");
			final FilterString filter = FilterString.create()
				.add(key)
				.add(identifier)
				.add(description)
				.add(placeName)
				.add(placeNameLocale);
			for(final Map<String, Object> transcribedPlaceName : transcribedPlaceNames)
				filter.add(extractRecordText(transcribedPlaceName));
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(description, row, TABLE_INDEX_DESCRIPTION);
			model.setValueAt(placeName, row, TABLE_INDEX_PLACE);

			tableData.add(new SearchAllRecord(key, TABLE_NAME_CULTURAL_NORM, filterData, identifier));

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
					&& TABLE_NAME_PLACE.equals(extractRecordReferenceTable(value))
					&& Objects.equals(selectedRecordID, extractRecordReferenceID(value)))){
				final Map<String, Object> localizedText = localizedTexts.get(extractRecordLocalizedTextID(value));
				matchedRecords.add(localizedText);
			}
		});
		return matchedRecords;
	}

	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
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

	private static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
	}

	private static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	private static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	private static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	private static String extractRecordReferenceType(final Map<String, Object> record){
		return (String)record.get("reference_type");
	}

	private static Integer extractRecordLocalizedTextID(final Map<String, Object> record){
		return (Integer)record.get("localized_text_id");
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> culturalNorms = new TreeMap<>();
		store.put("cultural_norm", culturalNorms);
		final Map<String, Object> culturalNorm = new HashMap<>();
		culturalNorm.put("id", 1);
		culturalNorm.put("identifier", "rule 1 id");
		culturalNorm.put("description", "rule 1");
		culturalNorm.put("place_id", 1);
		culturalNorm.put("certainty", "certain");
		culturalNorm.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		culturalNorms.put((Integer)culturalNorm.get("id"), culturalNorm);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		places.put((Integer)place1.get("id"), place1);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "place name 1");
		localizedText1.put("locale", "en");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "place name 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "IPA");
		localizedText2.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText2.get("id"), localizedText2);

		final TreeMap<Integer, Map<String, Object>> localizedTextJunctions = new TreeMap<>();
		store.put("localized_text_junction", localizedTextJunctions);
		final Map<String, Object> localizedTextJunction1 = new HashMap<>();
		localizedTextJunction1.put("id", 1);
		localizedTextJunction1.put("localized_text_id", 1);
		localizedTextJunction1.put("reference_type", "name");
		localizedTextJunction1.put("reference_table", "place");
		localizedTextJunction1.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction1.get("id"), localizedTextJunction1);
		final Map<String, Object> localizedTextJunction2 = new HashMap<>();
		localizedTextJunction2.put("id", 2);
		localizedTextJunction2.put("localized_text_id", 2);
		localizedTextJunction2.put("reference_type", "name");
		localizedTextJunction2.put("reference_table", "place");
		localizedTextJunction2.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction2.get("id"), localizedTextJunction2);

		final RecordListenerInterface linkListener = new RecordListenerInterface(){
			@Override
			public void onRecordSelected(final String table, final Integer id){
				System.out.println("onRecordSelected " + table + " " + id);
			}
		};

		EventQueue.invokeLater(() -> {
			final SearchCulturalNormPanel panel = create(store)
				.withLinkListener(linkListener);
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
