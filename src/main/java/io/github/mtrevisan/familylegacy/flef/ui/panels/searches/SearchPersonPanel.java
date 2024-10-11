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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;


public class SearchPersonPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -1361635723036701664L;

	private static final int TABLE_INDEX_PERSON_NAME = 2;
	private static final int TABLE_INDEX_PERSON_BIRTH_YEAR = 3;
	private static final int TABLE_INDEX_PERSON_BIRTH_PLACE = 4;
	private static final int TABLE_INDEX_PERSON_DEATH_YEAR = 5;
	private static final int TABLE_INDEX_PERSON_DEATH_PLACE = 6;

	private static final int TABLE_PREFERRED_WIDTH_YEAR = 43;
	private static final int TABLE_PREFERRED_WIDTH_PLACE = 250;
	private static final int TABLE_PREFERRED_WIDTH_NAME = 150;

	private static final String EVENT_TYPE_CATEGORY_BIRTH = "birth";
	private static final String EVENT_TYPE_CATEGORY_DEATH = "death";


	public static SearchPersonPanel create(){
		return new SearchPersonPanel();
	}


	private SearchPersonPanel(){
		super();


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PERSON_NAME, 0, TABLE_PREFERRED_WIDTH_NAME);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PERSON_BIRTH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PERSON_BIRTH_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PERSON_DEATH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PERSON_DEATH_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_PERSON;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Person",
			"birth", "birth place",
			"death", "death place"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT,
			SwingConstants.RIGHT, SwingConstants.LEFT,
			SwingConstants.RIGHT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator,
			numericComparator, textComparator,
			numericComparator, textComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_PERSON);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(int i = 0, length = records.size(); i < length; i ++){
			final Map<String, Object> record = records.get(i);

			final Integer personID = extractRecordID(record);
			final String personName = extractFirstName(personID);
			final List<String> personAllNames = extractAllNames(personID);
			final Map<String, Object> earliestPersonBirthYearAndPlace = extractEarliestBirthYearAndPlace(personID);
			final Map<String, Object> latestPersonDeathYearAndPlace = extractLatestDeathYearAndPlace(personID);
			final String personBirthYear = extractRecordYear(earliestPersonBirthYearAndPlace);
			final String personBirthPlace = extractRecordPlace(earliestPersonBirthYearAndPlace);
			final String personDeathYear = extractRecordYear(latestPersonDeathYearAndPlace);
			final String personDeathPlace = extractRecordPlace(latestPersonDeathYearAndPlace);
			final FilterString filter = FilterString.create().add(personID);
			for(final String name : personAllNames)
				filter.add(name);
			filter.add(personBirthYear).add(personBirthPlace).add(personDeathYear).add(personDeathPlace);
			final String filterData = filter.toString();

			model.setValueAt(personID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt((personName != null? personName: NO_DATA), row, TABLE_INDEX_PERSON_NAME);
			model.setValueAt((personBirthYear != null? personBirthYear: NO_DATA), row, TABLE_INDEX_PERSON_BIRTH_YEAR);
			model.setValueAt((personBirthPlace != null? personBirthPlace: NO_DATA), row, TABLE_INDEX_PERSON_BIRTH_PLACE);
			model.setValueAt((personDeathYear != null? personDeathYear: NO_DATA), row, TABLE_INDEX_PERSON_DEATH_YEAR);
			model.setValueAt((personDeathPlace != null? personDeathPlace: NO_DATA), row, TABLE_INDEX_PERSON_DEATH_PLACE);

			tableData.add(new SearchAllRecord(personID, EntityManager.NODE_PERSON, filterData, personName));

			row++;
		}
	}


	private String extractFirstName(final Integer personID){
		return Repository.findReferencingNodes(EntityManager.NODE_PERSON_NAME,
				EntityManager.NODE_PERSON, personID,
				EntityManager.RELATIONSHIP_FOR).stream()
			.findFirst()
			.map(SearchPersonPanel::extractName)
			.orElse(null);
	}

	private List<String> extractAllNames(final Integer personID){
		final List<String> names = new ArrayList<>(0);
		Repository.findReferencingNodes(EntityManager.NODE_PERSON_NAME,
				EntityManager.NODE_PERSON, personID,
				EntityManager.RELATIONSHIP_FOR)
			.forEach(record -> {
				names.add(extractName(record));

				//extract transliterations
				final Integer personNameID = extractRecordID(record);
				Repository.findReferencingNodes(EntityManager.NODE_LOCALIZED_PERSON_NAME,
						EntityManager.NODE_PERSON_NAME, personNameID,
						EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR).stream()
					.map(SearchPersonPanel::extractName)
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
		final Function<Map.Entry<LocalDate, Map<String, Object>>, Map<String, Object>> extractor = entry -> {
			final String year = Integer.toString(entry.getKey().getYear());
			final Integer eventID = extractRecordID(entry.getValue());
			final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
				EntityManager.RELATIONSHIP_HAPPENED_IN);
			final Map<String, Object> place = (placeNode != null? placeNode.getValue(): null);
			final String placeName = extractRecordName(place);

			final Map<String, Object> result = new HashMap<>(2);
			result.put("dateYear", year);
			result.put("placeName", placeName);
			return result;
		};
		final Map<String, Object> result = extractData(personID, EVENT_TYPE_CATEGORY_BIRTH, comparator, extractor);
		return (result != null? result: Collections.emptyMap());
	}

	private Map<String, Object> extractLatestDeathYearAndPlace(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Function<Map.Entry<LocalDate, Map<String, Object>>, Map<String, Object>> extractor = entry -> {
			final String dateYear = Integer.toString(entry.getKey().getYear());
			final Integer eventID = extractRecordID(entry.getValue());
			final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
				EntityManager.RELATIONSHIP_HAPPENED_IN);
			final Map<String, Object> place = (placeNode != null? placeNode.getValue(): null);
			final String placeName = extractRecordName(place);

			final Map<String, Object> result = new HashMap<>(2);
			result.put("dateYear", dateYear);
			result.put("placeName", placeName);
			return result;
		};
		final Map<String, Object> result = extractData(personID, EVENT_TYPE_CATEGORY_DEATH, comparator.reversed(), extractor);
		return (result != null? result: Collections.emptyMap());
	}

	private <T> T extractData(final Integer referenceID, final String eventTypeCategory, final Comparator<LocalDate> comparator,
			final Function<Map.Entry<LocalDate, Map<String, Object>>, T> extractor){
		final Set<String> eventTypes = getEventTypes(eventTypeCategory);
		return Repository.findReferencingNodes(EntityManager.NODE_EVENT,
				EntityManager.NODE_PERSON, referenceID,
				EntityManager.RELATIONSHIP_FOR).stream()
			.filter(entry -> {
				final Integer eventID = extractRecordID(entry);
				Map.Entry<String, Map<String, Object>> eventTypeNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
					EntityManager.RELATIONSHIP_OF_TYPE);
				final Map<String, Object> eventType = (eventTypeNode != null? eventTypeNode.getValue(): null);
				return eventTypes.contains(extractRecordType(eventType));
			})
			.map(entry -> {
				final Integer eventID = extractRecordID(entry);
				final Map.Entry<String, Map<String, Object>> dateNode = Repository.findReferencedNode(EntityManager.NODE_EVENT, eventID,
					EntityManager.RELATIONSHIP_HAPPENED_ON);
				final Map<String, Object> date = (dateNode != null? dateNode.getValue(): null);
				final String dateValue = extractRecordDate(date);
				final LocalDate parsedDate = DateParser.parse(dateValue);
				return (parsedDate != null? new AbstractMap.SimpleEntry<>(parsedDate, entry): null);
			})
			.filter(Objects::nonNull)
			.min(Map.Entry.comparingByKey(comparator))
			.map(extractor)
			.orElse(null);
	}

	private Set<String> getEventTypes(final String category){
		return Repository.findAll(EntityManager.NODE_EVENT_TYPE)
			.stream()
			.filter(entry -> Objects.equals(category, extractRecordCategory(entry)))
			.map(EntityManager::extractRecordType)
			.collect(Collectors.toSet());
	}

	private static String extractRecordYear(final Map<String, Object> record){
		return (String)record.get("dateYear");
	}

	private static String extractRecordPlace(final Map<String, Object> record){
		return (String)record.get("placeName");
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		int person1ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person2ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person3ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person4ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person5ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("personal_name", "personal name");
		personName1.put("family_name", "family name");
		personName1.put("type", "birth name");
		int personName1ID = Repository.upsert(personName1, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("personal_name", "personal name 2");
		personName2.put("family_name", "family name 2");
		personName2.put("type", "death name");
		int personName2ID = Repository.upsert(personName2, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName2ID,
			EntityManager.NODE_PERSON, person2ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName3 = new HashMap<>();
		personName3.put("personal_name", "personal name 3");
		personName3.put("family_name", "family name 3");
		personName3.put("type", "other name");
		int personName3ID = Repository.upsert(personName3, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName3ID,
			EntityManager.NODE_PERSON, person2ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		int localizedPersonName1ID = Repository.upsert(localizedPersonName1, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName1ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		int localizedPersonName2ID = Repository.upsert(localizedPersonName2, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName2ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("personal_name", "other");
		localizedPersonName3.put("family_name", "name");
		int localizedPersonName3ID = Repository.upsert(localizedPersonName3, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName3ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("identifier", "another place 1");
		place2.put("name", "name of the another place");
		int place2ID = Repository.upsert(place2, EntityManager.NODE_PLACE);

		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("date", "27 FEB 1976");
		int date1ID = Repository.upsert(historicDate1, EntityManager.NODE_HISTORIC_DATE);
		final Map<String, Object> historicDate2 = new HashMap<>();
		historicDate2.put("date", "1 JAN 1800");
		int date2ID = Repository.upsert(historicDate2, EntityManager.NODE_HISTORIC_DATE);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		group1.put("photo_crop", "0 0 10 20");
		int group1ID = Repository.upsert(group1, EntityManager.NODE_GROUP);

		final Map<String, Object> event1 = new HashMap<>();
		event1.put("description", "a birth");
		int event1ID = Repository.upsert(event1, EntityManager.NODE_EVENT);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_HAPPENED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> event2 = new HashMap<>();
		event2.put("description", "another birth");
		int event2ID = Repository.upsert(event2, EntityManager.NODE_EVENT);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_PLACE, place2ID,
			EntityManager.RELATIONSHIP_HAPPENED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> event3 = new HashMap<>();
		int event3ID = Repository.upsert(event3, EntityManager.NODE_EVENT);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event3ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event3ID,
			EntityManager.NODE_PERSON, person2ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> event4 = new HashMap<>();
		int event4ID = Repository.upsert(event4, EntityManager.NODE_EVENT);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event4ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_HAPPENED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event4ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event4ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("type", "birth");
		eventType1.put("category", EVENT_TYPE_CATEGORY_BIRTH);
		int eventType1ID = Repository.upsert(eventType1, EntityManager.NODE_EVENT_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_EVENT_TYPE, eventType1ID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event2ID,
			EntityManager.NODE_EVENT_TYPE, eventType1ID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> eventType2 = new HashMap<>();
		eventType2.put("type", "death");
		eventType2.put("category", EVENT_TYPE_CATEGORY_DEATH);
		int eventType2ID = Repository.upsert(eventType2, EntityManager.NODE_EVENT_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event3ID,
			EntityManager.NODE_EVENT_TYPE, eventType2ID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> eventType3 = new HashMap<>();
		eventType3.put("type", "marriage");
		eventType3.put("category", "union");
		int eventType3ID = Repository.upsert(eventType3, EntityManager.NODE_EVENT_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event4ID,
			EntityManager.NODE_EVENT_TYPE, eventType3ID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("type", "gregorian");
		int calendar1ID = Repository.upsert(calendar1, EntityManager.NODE_CALENDAR);

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
			final SearchPersonPanel panel = create();
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
