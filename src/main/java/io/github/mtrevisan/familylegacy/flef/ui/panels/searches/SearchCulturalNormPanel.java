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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocalizedTextID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordText;


public class SearchCulturalNormPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -4361406306346697722L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;
	private static final int TABLE_INDEX_DESCRIPTION = 3;
	private static final int TABLE_INDEX_PLACE = 4;

	private static final int TABLE_PREFERRED_WIDTH_IDENTIFIER = 250;
	private static final int TABLE_PREFERRED_WIDTH_DESCRIPTION = 150;
	private static final int TABLE_PREFERRED_WIDTH_PLACE = 250;


	public static SearchCulturalNormPanel create(){
		return new SearchCulturalNormPanel();
	}


	private SearchCulturalNormPanel(){
		super();


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_IDENTIFIER, 0, TABLE_PREFERRED_WIDTH_IDENTIFIER);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_DESCRIPTION, 0, TABLE_PREFERRED_WIDTH_DESCRIPTION);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_NAME_CULTURAL_NORM;
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
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, textComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_NAME_CULTURAL_NORM);
		final List<Map<String, Object>> places = Repository.findAll(EntityManager.NODE_NAME_PLACE);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(int i = 0, length = records.size(); i < length; i ++){
			final Map<String, Object> record = records.get(i);

			final Integer recordID = extractRecordID(record);
			final String identifier = extractRecordIdentifier(record);
			final String description = extractRecordDescription(record);
			final Integer placeID = extractRecordPlaceID(record);
			final String placeName = (placeID != null? extractRecordName(places.get(placeID)): null);
			final String placeNameLocale = extractRecordLocale(record);
			final List<Map<String, Object>> transcribedPlaceNames = extractReferences(EntityManager.NODE_NAME_LOCALIZED_TEXT_JUNCTION,
				recordID, EntityManager::extractRecordReferenceType, EntityManager.LOCALIZED_TEXT_TYPE_NAME);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(identifier)
				.add(description)
				.add(placeName)
				.add(placeNameLocale);
			for(final Map<String, Object> transcribedPlaceName : transcribedPlaceNames)
				filter.add(extractRecordText(transcribedPlaceName));
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(description, row, TABLE_INDEX_DESCRIPTION);
			model.setValueAt(placeName, row, TABLE_INDEX_PLACE);

			tableData.add(new SearchAllRecord(recordID, EntityManager.NODE_NAME_CULTURAL_NORM, filterData, identifier));

			row++;
		}
	}


	private <T> List<Map<String, Object>> extractReferences(final String fromTable, final Integer selectedRecordID,
			final Function<Map<String, Object>, T> filter, final T filterValue){
		final List<Map<String, Object>> matchedRecords = new ArrayList<>(0);
		final List<Map<String, Object>> records = Repository.findAll(fromTable);
		final Map<Integer, Map<String, Object>> localizedTexts = Repository.findAllNavigable(EntityManager.NODE_NAME_LOCALIZED_TEXT);
		records.forEach(record -> {
			if(((filter == null || Objects.equals(filterValue, filter.apply(record)))
					&& EntityManager.NODE_NAME_PLACE.equals(extractRecordReferenceTable(record))
					&& Objects.equals(selectedRecordID, extractRecordReferenceID(record)))){
				final Map<String, Object> localizedText = localizedTexts.get(extractRecordLocalizedTextID(record));
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


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> culturalNorm1 = new HashMap<>();
		culturalNorm1.put("id", 1);
		culturalNorm1.put("identifier", "rule 1 id");
		culturalNorm1.put("description", "rule 1");
		culturalNorm1.put("place_id", 1);
		culturalNorm1.put("certainty", "certain");
		culturalNorm1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.save(EntityManager.NODE_NAME_CULTURAL_NORM, culturalNorm1);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		Repository.save(EntityManager.NODE_NAME_PLACE, place1);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "place name 1");
		localizedText1.put("locale", "en");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "place name 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "IPA");
		localizedText2.put("transcription_type", "romanized");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText2);

		final Map<String, Object> localizedTextJunction1 = new HashMap<>();
		localizedTextJunction1.put("type", "name");
		Repository.upsertRelationship(EntityManager.NODE_NAME_LOCALIZED_TEXT, extractRecordID(localizedText1),
			EntityManager.NODE_NAME_PLACE, extractRecordID(place1),
			EntityManager.RELATIONSHIP_NAME_FOR, localizedTextJunction1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedTextJunction2 = new HashMap<>();
		localizedTextJunction2.put("type", "name");
		Repository.upsertRelationship(EntityManager.NODE_NAME_LOCALIZED_TEXT, extractRecordID(localizedText2),
			EntityManager.NODE_NAME_PLACE, extractRecordID(place1),
			EntityManager.RELATIONSHIP_NAME_FOR, localizedTextJunction2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

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
			final SearchCulturalNormPanel panel = create();
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
