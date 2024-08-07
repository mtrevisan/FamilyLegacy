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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class SearchDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -1524494907031661540L;

	private static final int TABLE_INDEX_RECORD_UNION_YEAR = 2;
	private static final int TABLE_INDEX_RECORD_UNION_PLACE = 3;
	private static final int TABLE_INDEX_RECORD_PARTNER1_ID = 4;
	private static final int TABLE_INDEX_RECORD_PARTNER1_NAME = 5;
	private static final int TABLE_INDEX_RECORD_PARTNER2_ID = 6;
	private static final int TABLE_INDEX_RECORD_PARTNER2_NAME = 7;

	private static final String TABLE_NAME_GROUP = "group";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_PERSON = "person";

	private static final String NO_DATA = "?";
	private static final String FIGURE_DASH = "\u2012";


	public static SearchDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final SearchDialog dialog = new SearchDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		return dialog;
	}


	private SearchDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public SearchDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return null;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Year", "Place",
			"Partner 1 ID", "Partner 1",
			"Partner 2 ID", "Partner 2"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.LEFT,
			SwingConstants.RIGHT, SwingConstants.LEFT,
			SwingConstants.RIGHT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		return new Comparator<?>[]{Comparator.comparingInt(key -> Integer.parseInt(key.toString())), null, Comparator.naturalOrder()};
	}

	@Override
	protected void initStoreComponents(){
		setTitle("Search");

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME_GROUP);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			//TODO
			final Integer unionID = extractRecordID(container);
			final List<Integer> personIDsInUnion = getPartnerIDs(unionID);
			final String earliestUnionYear = extractEarliestUnionYear(unionID);
			final String earliestUnionPlace = extractEarliestUnionPlace(unionID);
			final Integer partner1ID = (!personIDsInUnion.isEmpty()? personIDsInUnion.removeFirst(): null);
			final String partner1Name = extractFirstName(partner1ID);
			final String earliestPartner1BirthYear = extractBirthYear(partner1ID);
			final String earliestPartner1DeathYear = extractDeathYear(partner1ID);
			final Integer partner2ID = (!personIDsInUnion.isEmpty()? personIDsInUnion.removeFirst(): null);
			final String partner2Name = extractFirstName(partner2ID);
			final String earliestPartner2BirthYear = extractBirthYear(partner2ID);
			final String earliestPartner2DeathYear = extractDeathYear(partner2ID);
			final String partner1Identifier = (partner1Name != null? partner1Name: NO_DATA)
				+ " (" + (earliestPartner1BirthYear != null? earliestPartner1BirthYear: NO_DATA) + FIGURE_DASH
				+ (earliestPartner1DeathYear != null? earliestPartner1DeathYear: NO_DATA) + ")";
			final String partner2Identifier = (partner2Name != null? partner2Name: NO_DATA)
				+ " (" + (earliestPartner2BirthYear != null? earliestPartner2BirthYear: NO_DATA) + FIGURE_DASH
				+ (earliestPartner2DeathYear != null? earliestPartner2DeathYear: NO_DATA) + ")";
			final StringJoiner filter = new StringJoiner(" | ")
				.add(key.toString())
				.add(earliestUnionYear)
				.add(earliestUnionPlace);
			if(partner1ID != null)
				filter.add(partner1ID.toString())
					.add(partner1Name)
					.add(earliestPartner1BirthYear)
					.add(earliestPartner1DeathYear);
			if(partner2ID != null)
				filter.add(partner2ID.toString())
					.add(partner2Name)
					.add(earliestPartner2BirthYear)
					.add(earliestPartner2DeathYear);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(filter.toString(), row, TABLE_INDEX_RECORD_FILTER);
			model.setValueAt(earliestUnionYear, row, TABLE_INDEX_RECORD_UNION_YEAR);
			model.setValueAt(earliestUnionPlace, row, TABLE_INDEX_RECORD_UNION_PLACE);

			model.setValueAt(partner1ID, row, TABLE_INDEX_RECORD_PARTNER1_ID);
			model.setValueAt(partner1Identifier, row, TABLE_INDEX_RECORD_PARTNER1_NAME);

			model.setValueAt(partner2ID, row, TABLE_INDEX_RECORD_PARTNER2_ID);
			model.setValueAt(partner2Identifier, row, TABLE_INDEX_RECORD_PARTNER2_NAME);

			row ++;
		}
	}

	@Override
	protected void fillData(){}

	@Override
	protected void clearData(){}

	@Override
	protected boolean validateData(){
		return true;
	}

	@Override
	protected boolean saveData(){
		return !ignoreEvents;
	}


	private List<Integer> getPartnerIDs(final Integer groupID){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.map(SearchDialog::extractRecordReferenceID)
			.toList();
	}

	private String extractEarliestUnionYear(final Integer unionID){
		final Map<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(unionID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.map(SearchDialog::extractRecordGroupID)
			.map(groups::get)
			.findFirst()
			.map(SearchDialog::extractRecordID);
	}

	private String extractEarliestUnionPlace(final Integer unionID){
		final Map<Integer, Map<String, Object>> groups = getRecords(TABLE_NAME_GROUP);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(unionID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.map(SearchDialog::extractRecordGroupID)
			.map(groups::get)
			.findFirst()
			.map(SearchDialog::extractRecordID);
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	private static String extractRecordRole(final Map<String, Object> record){
		return (String)record.get("role");
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
		source1.put("author", "author 1 APA-style");
		source1.put("place_id", 1);
		source1.put("date_id", 1);
		source1.put("location", "location 1");
		sources.put((Integer)source1.get("id"), source1);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("id", 2);
		source2.put("identifier", "source 2");
		source2.put("type", "newspaper");
		source2.put("author", "author 2 APA-style");
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
		historicDate1.put("calendar_id", 1);
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
		place1.put("name_locale", "en-US");
		place1.put("type", "province");
		place1.put("coordinate", "45.65, 12.19");
		place1.put("coordinate_system", "WGS84");
		place1.put("coordinate_credibility", "certain");
		places.put((Integer)place1.get("id"), place1);

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put("note", notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", "source");
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put("restriction", restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", "source");
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final SearchDialog dialog = createSelectOnly(store, parent);
			dialog.initComponents();
			dialog.loadData();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
