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
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordGroupID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonNameID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordRole;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTypeID;


public class SearchGroupPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = 4199513069798359051L;

	private static final int TABLE_INDEX_UNION_YEAR = 2;
	private static final int TABLE_INDEX_UNION_PLACE = 3;
	private static final int TABLE_INDEX_PARTNER1_ID = 4;
	private static final int TABLE_INDEX_PARTNER1_NAME = 5;
	private static final int TABLE_INDEX_PARTNER1_BIRTH_YEAR = 6;
	private static final int TABLE_INDEX_PARTNER1_DEATH_YEAR = 7;
	private static final int TABLE_INDEX_PARTNER2_ID = 8;
	private static final int TABLE_INDEX_PARTNER2_NAME = 9;
	private static final int TABLE_INDEX_PARTNER2_BIRTH_YEAR = 10;
	private static final int TABLE_INDEX_PARTNER2_DEATH_YEAR = 11;

	private static final int TABLE_PREFERRED_WIDTH_YEAR = 43;
	private static final int TABLE_PREFERRED_WIDTH_PLACE = 250;
	private static final int TABLE_PREFERRED_WIDTH_NAME = 150;

	private static final String EVENT_TYPE_CATEGORY_BIRTH = "birth";
	private static final String EVENT_TYPE_CATEGORY_DEATH = "death";
	private static final String EVENT_TYPE_CATEGORY_UNION = "union";


	public static SearchGroupPanel create(){
		return new SearchGroupPanel();
	}


	private SearchGroupPanel(){
		super();


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_UNION_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_UNION_PLACE, 0, TABLE_PREFERRED_WIDTH_PLACE);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PARTNER1_ID, TABLE_PREFERRED_WIDTH_ID);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PARTNER1_NAME, 0, TABLE_PREFERRED_WIDTH_NAME);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PARTNER1_BIRTH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PARTNER1_DEATH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PARTNER2_ID, TABLE_PREFERRED_WIDTH_ID);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PARTNER2_NAME, 0, TABLE_PREFERRED_WIDTH_NAME);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PARTNER2_BIRTH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_PARTNER2_DEATH_YEAR, TABLE_PREFERRED_WIDTH_YEAR);
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_NAME_GROUP;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Year", "Place",
			"ID", "Partner 1", "birth", "death",
			"ID", "Partner 2", "birth", "death"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.LEFT,
			SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.RIGHT,
			SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.RIGHT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator,
			numericComparator, textComparator, numericComparator, numericComparator,
			numericComparator, textComparator, numericComparator, numericComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_NAME_GROUP);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(int i = 0, length = records.size(); i < length; i ++){
			final Map<String, Object> record = records.get(i);

			final Integer recordID = extractRecordID(record);
			final Integer unionID = extractRecordID(record);
			final String type = extractRecordType(record);
			final List<Integer> personIDsInUnion = getPartnerIDs(unionID);
			final String earliestUnionYear = extractEarliestUnionYear(unionID);
			final String earliestUnionPlace = extractEarliestUnionPlace(unionID);
			final Integer partner1ID = (! personIDsInUnion.isEmpty()? personIDsInUnion.removeFirst(): null);
			final String partner1Name = extractFirstName(partner1ID);
			final List<String> partner1AllNames = extractAllNames(partner1ID);
			final String earliestPartner1BirthYear = extractEarliestBirthYear(partner1ID);
			final String latestPartner1DeathYear = extractLatestDeathYear(partner1ID);
			final Integer partner2ID = (! personIDsInUnion.isEmpty()? personIDsInUnion.removeFirst(): null);
			final String partner2Name = extractFirstName(partner2ID);
			final List<String> partner2AllNames = extractAllNames(partner2ID);
			final String earliestPartner2BirthYear = extractEarliestBirthYear(partner2ID);
			final String latestPartner2DeathYear = extractLatestDeathYear(partner2ID);
			final String partner1BirthYear = (earliestPartner1BirthYear != null? earliestPartner1BirthYear: NO_DATA);
			final String partner1DeathYear = (latestPartner1DeathYear != null? latestPartner1DeathYear: NO_DATA);
			final String partner2BirthYear = (earliestPartner2BirthYear != null? earliestPartner2BirthYear: NO_DATA);
			final String partner2DeathYear = (latestPartner2DeathYear != null? latestPartner2DeathYear: NO_DATA);
			final FilterString filter = FilterString.create().add(recordID).add(type).add(earliestUnionYear).add(earliestUnionPlace);
			if(partner1ID != null){
				filter.add(partner1ID);
				for(final String name : partner1AllNames)
					filter.add(name);
				filter.add(earliestPartner1BirthYear).add(latestPartner1DeathYear);
			}
			if(partner2ID != null){
				filter.add(partner2ID);
				for(final String name : partner2AllNames)
					filter.add(name);
				filter.add(earliestPartner2BirthYear).add(latestPartner2DeathYear);
			}
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(earliestUnionYear, row, TABLE_INDEX_UNION_YEAR);
			model.setValueAt(earliestUnionPlace, row, TABLE_INDEX_UNION_PLACE);

			model.setValueAt(partner1ID, row, TABLE_INDEX_PARTNER1_ID);
			model.setValueAt((partner1Name != null? partner1Name: NO_DATA), row, TABLE_INDEX_PARTNER1_NAME);
			model.setValueAt(partner1BirthYear, row, TABLE_INDEX_PARTNER1_BIRTH_YEAR);
			model.setValueAt(partner1DeathYear, row, TABLE_INDEX_PARTNER1_DEATH_YEAR);

			model.setValueAt(partner2ID, row, TABLE_INDEX_PARTNER2_ID);
			model.setValueAt((partner2Name != null? partner2Name: NO_DATA), row, TABLE_INDEX_PARTNER2_NAME);
			model.setValueAt(partner2BirthYear, row, TABLE_INDEX_PARTNER2_BIRTH_YEAR);
			model.setValueAt(partner2DeathYear, row, TABLE_INDEX_PARTNER2_DEATH_YEAR);

			final StringJoiner partners = new StringJoiner(", ");
			if(partner1Name != null)
				partners.add(partner1Name);
			if(partner2Name != null)
				partners.add(partner2Name);
			tableData.add(new SearchAllRecord(recordID, EntityManager.NODE_NAME_GROUP, filterData, (partners.length() > 0? partners.toString(): NO_DATA)));

			row++;
		}
	}


	private List<Integer> getPartnerIDs(final Integer groupID){
		return new ArrayList<>(Repository.findAll(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.stream()
			.filter(entry -> EntityManager.NODE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_PARTNER, extractRecordRole(entry)))
			.map(EntityManager::extractRecordReferenceID)
			.toList());
	}

	private String extractFirstName(final Integer personID){
		return Repository.findAll(EntityManager.NODE_NAME_PERSON_NAME)
			.stream()
			.filter(entry -> Objects.equals(personID, extractRecordPersonID(entry)))
			.map(SearchGroupPanel::extractName)
			.findFirst()
			.orElse(null);
	}

	private List<String> extractAllNames(final Integer personID){
		final List<Map<String, Object>> localizedPersonNames = Repository.findAll(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME);
		final List<String> names = new ArrayList<>(0);
		Repository.findAll(EntityManager.NODE_NAME_PERSON_NAME)
			.stream()
			.filter(record -> Objects.equals(personID, extractRecordPersonID(record)))
			.forEach(record -> {
				names.add(extractName(record));

				//extract transliterations
				final Integer personNameID = extractRecordID(record);
				localizedPersonNames
					.stream()
					.filter(record2 -> Objects.equals(personNameID, extractRecordPersonNameID(record2)))
					.map(SearchGroupPanel::extractName)
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

	private String extractEarliestUnionYear(final Integer unionID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Function<Map.Entry<LocalDate, Map<String, Object>>, String> extractor = entry -> Integer.toString(entry.getKey().getYear());
		return extractData(unionID, EVENT_TYPE_CATEGORY_UNION, comparator, extractor);
	}

	private String extractEarliestBirthYear(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Function<Map.Entry<LocalDate, Map<String, Object>>, String> extractor = entry -> Integer.toString(entry.getKey().getYear());
		return extractData(personID, EVENT_TYPE_CATEGORY_BIRTH, comparator, extractor);
	}

	private String extractLatestDeathYear(final Integer personID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Function<Map.Entry<LocalDate, Map<String, Object>>, String> extractor = entry -> Integer.toString(entry.getKey().getYear());
		return extractData(personID, EVENT_TYPE_CATEGORY_DEATH, comparator.reversed(), extractor);
	}

	private String extractEarliestUnionPlace(final Integer unionID){
		final Comparator<LocalDate> comparator = Comparator.naturalOrder();
		final Map<Integer, Map<String, Object>> places = Repository.findAllNavigable(EntityManager.NODE_NAME_PLACE);
		final Function<Map.Entry<LocalDate, Map<String, Object>>, String> extractor = entry -> {
			final Integer placeID = extractRecordPlaceID(entry.getValue());
			return (placeID != null? extractRecordName(places.get(placeID)): null);
		};
		return extractData(unionID, EVENT_TYPE_CATEGORY_UNION, comparator, extractor);
	}

	private <T> T extractData(final Integer referenceID, final String eventTypeCategory, final Comparator<LocalDate> comparator,
			final Function<Map.Entry<LocalDate, Map<String, Object>>, T> extractor){
		final Map<Integer, Map<String, Object>> storeEventTypes = Repository.findAllNavigable(EntityManager.NODE_NAME_EVENT_TYPE);
		final Map<Integer, Map<String, Object>> historicDates = Repository.findAllNavigable(EntityManager.NODE_NAME_HISTORIC_DATE);
		final String eventReferenceTable = (EVENT_TYPE_CATEGORY_UNION.equals(eventTypeCategory)
			? EntityManager.NODE_NAME_GROUP
			: EntityManager.NODE_NAME_PERSON);
		final Set<String> eventTypes = getEventTypes(eventTypeCategory);
		return Repository.findAll(EntityManager.NODE_NAME_EVENT)
			.stream()
			.filter(entry -> Objects.equals(eventReferenceTable, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(referenceID, extractRecordReferenceID(entry)))
			.filter(entry -> {
				final Integer recordTypeID = extractRecordTypeID(entry);
				final String recordType = extractRecordType(storeEventTypes.get(recordTypeID));
				return eventTypes.contains(recordType);
			})
			.map(entry -> {
				final Map<String, Object> dateEntry = historicDates.get(extractRecordDateID(entry));
				final String dateValue = extractRecordDate(dateEntry);
				final LocalDate parsedDate = DateParser.parse(dateValue);
				return (parsedDate != null? new AbstractMap.SimpleEntry<>(parsedDate, entry): null);
			})
			.filter(Objects::nonNull)
			.min(Map.Entry.comparingByKey(comparator))
			.map(extractor)
			.orElse(null);
	}

	private Set<String> getEventTypes(final String category){
		return Repository.findAll(EntityManager.NODE_NAME_EVENT_TYPE)
			.stream()
			.filter(entry -> Objects.equals(category, extractRecordCategory(entry)))
			.map(EntityManager::extractRecordType)
			.collect(Collectors.toSet());
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		Repository.save(EntityManager.NODE_NAME_PERSON, person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		Repository.save(EntityManager.NODE_NAME_PERSON, person2);
		final Map<String, Object> person3 = new HashMap<>();
		person3.put("id", 3);
		Repository.save(EntityManager.NODE_NAME_PERSON, person3);
		final Map<String, Object> person4 = new HashMap<>();
		person4.put("id", 4);
		Repository.save(EntityManager.NODE_NAME_PERSON, person4);
		final Map<String, Object> person5 = new HashMap<>();
		person5.put("id", 5);
		Repository.save(EntityManager.NODE_NAME_PERSON, person5);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		Repository.save(EntityManager.NODE_NAME_GROUP, group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "family");
		Repository.save(EntityManager.NODE_NAME_GROUP, group2);

		final Map<String, Object> groupJunction11 = new HashMap<>();
		groupJunction11.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 1,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction11, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 2,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction2, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction13 = new HashMap<>();
		groupJunction13.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group2), EntityManager.NODE_NAME_PERSON, 1,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction13, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group2), EntityManager.NODE_NAME_PERSON, 3,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction3, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction4 = new HashMap<>();
		groupJunction4.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 4,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction4, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 5,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction5, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction6 = new HashMap<>();
		groupJunction6.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group2), EntityManager.NODE_NAME_PERSON, 4,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction6, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "personal name");
		personName1.put("family_name", "family name");
		personName1.put("type", "birth name");
		Repository.save(EntityManager.NODE_NAME_PERSON_NAME, personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("personal_name", "personal name 2");
		personName2.put("family_name", "family name 2");
		personName2.put("type", "death name");
		Repository.save(EntityManager.NODE_NAME_PERSON_NAME, personName2);
		final Map<String, Object> personName3 = new HashMap<>();
		personName3.put("id", 3);
		personName3.put("person_id", 2);
		personName3.put("personal_name", "personal name 3");
		personName3.put("family_name", "family name 3");
		personName3.put("type", "other name");
		Repository.save(EntityManager.NODE_NAME_PERSON_NAME, personName3);

		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("id", 1);
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("person_name_id", 1);
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME, localizedPersonName1);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("id", 2);
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("person_name_id", 1);
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME, localizedPersonName2);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("id", 3);
		localizedPersonName3.put("personal_name", "other");
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("person_name_id", 1);
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME, localizedPersonName3);

		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type_id", 1);
		event1.put("description", "a birth");
		event1.put("place_id", 1);
		event1.put("date_id", 1);
		event1.put("reference_table", "person");
		event1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_EVENT, event1);
		final Map<String, Object> event2 = new HashMap<>();
		event2.put("id", 2);
		event2.put("type_id", 1);
		event2.put("description", "another birth");
		event2.put("place_id", 2);
		event2.put("date_id", 2);
		event2.put("reference_table", "person");
		event2.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_EVENT, event2);
		final Map<String, Object> event3 = new HashMap<>();
		event3.put("id", 3);
		event3.put("type_id", 2);
		event3.put("date_id", 1);
		event3.put("reference_table", "person");
		event3.put("reference_id", 2);
		Repository.save(EntityManager.NODE_NAME_EVENT, event3);
		final Map<String, Object> event4 = new HashMap<>();
		event4.put("id", 4);
		event4.put("type_id", 3);
		event4.put("date_id", 1);
		event4.put("place_id", 1);
		event4.put("reference_table", "group");
		event4.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_EVENT, event4);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("type", "birth");
		eventType1.put("category", EVENT_TYPE_CATEGORY_BIRTH);
		Repository.save(EntityManager.NODE_NAME_EVENT_TYPE, eventType1);
		final Map<String, Object> eventType2 = new HashMap<>();
		eventType2.put("id", 2);
		eventType2.put("type", "death");
		eventType2.put("category", EVENT_TYPE_CATEGORY_DEATH);
		Repository.save(EntityManager.NODE_NAME_EVENT_TYPE, eventType2);
		final Map<String, Object> eventType3 = new HashMap<>();
		eventType3.put("id", 3);
		eventType3.put("type", "marriage");
		eventType3.put("category", EVENT_TYPE_CATEGORY_UNION);
		Repository.save(EntityManager.NODE_NAME_EVENT_TYPE, eventType3);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		Repository.save(EntityManager.NODE_NAME_PLACE, place1);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("id", 2);
		place2.put("identifier", "another place 1");
		place2.put("name", "name of the another place");
		Repository.save(EntityManager.NODE_NAME_PLACE, place2);

		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("id", 1);
		historicDate1.put("date", "27 FEB 1976");
		Repository.save(EntityManager.NODE_NAME_HISTORIC_DATE, historicDate1);
		final Map<String, Object> historicDate2 = new HashMap<>();
		historicDate2.put("id", 2);
		historicDate2.put("date", "1 JAN 1800");
		Repository.save(EntityManager.NODE_NAME_HISTORIC_DATE, historicDate2);

		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		Repository.save(EntityManager.NODE_NAME_CALENDAR, calendar1);

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
			final SearchGroupPanel panel = create();
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
