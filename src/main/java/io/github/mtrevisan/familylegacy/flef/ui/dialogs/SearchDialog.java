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

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.Debouncer;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.SearchParser;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.CommonSearchPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.FilteredTablePanelInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.RecordListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchAllPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchAllRecord;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchCitationPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchCulturalNormPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchEventPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchGroupPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchMediaPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchNotePanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchPersonPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchPlacePanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchRepositoryPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchResearchStatusPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchSourcePanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCalendarOriginalID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoCrop;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPlaceID;


public final class SearchDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 7854594091959021095L;


	private static final class SearchData{
		private final Class<? extends CommonSearchPanel> panel;
		private CommonSearchPanel instance;

		private static SearchData create(final Class<? extends CommonSearchPanel> panel){
			return new SearchData(panel);
		}

		private SearchData(final Class<? extends CommonSearchPanel> panel){
			this.panel = panel;
		}
	}

	private static final List<Map.Entry<String, SearchData>> PANE_NAME_TABLE = new ArrayList<>(12);
	static{
		addPane("All", SearchAllPanel.class);
		addPane("Repositories", SearchRepositoryPanel.class);
		addPane("Sources", SearchSourcePanel.class);
		addPane("Citations", SearchCitationPanel.class);
		addPane("Places", SearchPlacePanel.class);
		addPane("Media", SearchMediaPanel.class);
		addPane("Notes", SearchNotePanel.class);
		addPane("Persons", SearchPersonPanel.class);
		addPane("Groups", SearchGroupPanel.class);
		addPane("Events", SearchEventPanel.class);
		addPane("Cultural norms", SearchCulturalNormPanel.class);
		addPane("Research statuses", SearchResearchStatusPanel.class);
	}

	private static void addPane(final String title, final Class<? extends CommonSearchPanel> panel){
		PANE_NAME_TABLE.add(new AbstractMap.SimpleEntry<>(title, SearchData.create(panel)));
	}


	private JLabel filterLabel;
	private JTextField filterField;
	private final JTabbedPane recordTabbedPane = new JTabbedPane();

	private final Debouncer<SearchDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, CommonListDialog.DEBOUNCE_TIME);


	public static SearchDialog create(final Frame parent){
		return new SearchDialog(parent);
	}


	private SearchDialog(final Frame parent){
		super(parent, true);


		initComponents();
	}


	public SearchDialog withLinkListener(final RecordListenerInterface listener){
		for(final Map.Entry<String, SearchData> entry : PANE_NAME_TABLE)
			entry.getValue().instance.setLinkListener(listener);

		return this;
	}

	private void initComponents(){
		initRecordComponents();

		initLayout();
	}

	private void initRecordComponents(){
		setTitle("Search");

		filterLabel = new JLabel("Filter:");
		filterField = new JTextField();

		try{
			for(final Map.Entry<String, SearchData> entry : PANE_NAME_TABLE){
				final SearchData searchData = entry.getValue();

				final Class<? extends CommonSearchPanel> panelClass = searchData.panel;
				final Method createMethod = panelClass.getMethod("create");

				final Object instance = createMethod.invoke(null);

				searchData.instance = (CommonSearchPanel)instance;
			}
		}
		catch(final NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored){}


		filterLabel.setLabelFor(filterField);
		GUIHelper.addUndoCapability(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(SearchDialog.this);
			}
		});
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		initRecordLayout();

		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap related");
		add(recordTabbedPane, "grow");

		pack();
	}

	private void initRecordLayout(){
		PANE_NAME_TABLE.forEach(entry -> recordTabbedPane.add(entry.getKey(), entry.getValue().instance));
	}

	private void closeAction(final ActionEvent evt){
		setVisible(false);
	}

	public void loadData(){
		final List<SearchAllRecord> allTableData = new ArrayList<>(0);
		for(final Map.Entry<String, SearchData> entry : PANE_NAME_TABLE){
			final SearchData searchData = entry.getValue();

			if(searchData.instance.getTableName() != null){
				final CommonSearchPanel instance = searchData.instance;
				instance.loadData();
				allTableData.addAll(instance.exportTableData());
			}
		}
		final SearchAllPanel searchAllPanel = (SearchAllPanel)PANE_NAME_TABLE.getFirst()
			.getValue()
			.instance;
		searchAllPanel.loadData(allTableData);
	}

	private void filterTableBy(final JDialog panel){
		final String filterText = GUIHelper.getTextTrimmed(filterField);
		final Map<String, Object> parsedFilterText = SearchParser.parseSearchQuery(filterText);
		final String plainFilterText = (String)parsedFilterText.get(SearchParser.PARAMETER_SEARCH_TEXT);
		@SuppressWarnings("unchecked")
		final List<Map.Entry<String, String>> additionalFields = (List<Map.Entry<String, String>>)parsedFilterText.get(
			SearchParser.PARAMETER_ADDITIONAL_FIELDS);

		for(int i = 0, length = recordTabbedPane.getComponentCount(); i < length; i ++){
			final FilteredTablePanelInterface component = (FilteredTablePanelInterface)recordTabbedPane.getComponent(i);

			component.setFilterAndSorting(plainFilterText, additionalFields);
		}
	}


	public static String getTableName(final String paneTitle){
		for(final Map.Entry<String, SearchData> entry : PANE_NAME_TABLE)
			if(paneTitle.equals(entry.getKey()))
				return entry.getValue().instance
					.getTableName();
		return null;
	}

	public static String getPaneTitle(final String tableName){
		for(final Map.Entry<String, SearchData> entry : PANE_NAME_TABLE)
			if(tableName.equals(entry.getValue().instance.getTableName()))
				return entry.getKey();
		return null;
	}

	public void showDialog(){
		setLocationRelativeTo(getParent());
		setVisible(true);
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> citation1 = new HashMap<>();
		citation1.put("id", 1);
		citation1.put("source_id", 1);
		citation1.put("location", "here");
		citation1.put("extract", "text 1");
		citation1.put("extract_locale", "en-US");
		citation1.put("extract_type", "transcript");
		Repository.save(EntityManager.NODE_NAME_CITATION, citation1);

		final Map<String, Object> assertion1 = new HashMap<>();
		assertion1.put("id", 1);
		assertion1.put("citation_id", 1);
		assertion1.put("reference_table", "citation");
		assertion1.put("reference_id", 1);
		assertion1.put("role", "father");
		assertion1.put("certainty", "certain");
		assertion1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.save(EntityManager.NODE_NAME_ASSERTION, assertion1);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "text 1");
		localizedText1.put("locale", "it");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "text 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "kana");
		localizedText2.put("transcription_type", "romanized");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText2);

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

		final Map<String, Object> localizedTextJunction1 = new HashMap<>();
		localizedTextJunction1.put("reference_type", "name");
		Repository.upsertRelationship(EntityManager.NODE_NAME_LOCALIZED_TEXT, extractRecordID(localizedText1),
			EntityManager.NODE_NAME_PLACE, extractRecordID(place1),
			EntityManager.RELATIONSHIP_NAME_FOR, localizedTextJunction1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedTextJunction2 = new HashMap<>();
		localizedTextJunction2.put("reference_type", "name");
		Repository.upsertRelationship(EntityManager.NODE_NAME_LOCALIZED_TEXT, extractRecordID(localizedText2),
			EntityManager.NODE_NAME_PLACE, extractRecordID(place1),
			EntityManager.RELATIONSHIP_NAME_FOR, localizedTextJunction2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedTextJunction3 = new HashMap<>();
		localizedTextJunction3.put("reference_type", "extract");
		Repository.upsertRelationship(EntityManager.NODE_NAME_LOCALIZED_TEXT, extractRecordID(localizedText2),
			EntityManager.NODE_NAME_CITATION, extractRecordID(citation1),
			EntityManager.RELATIONSHIP_NAME_FOR, localizedTextJunction3,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> source1 = new HashMap<>();
		source1.put("id", 1);
		source1.put("repository_id", 1);
		source1.put("identifier", "source 1");
		source1.put("type", "marriage certificate");
		source1.put("author", "author 1");
		source1.put("place_id", 1);
		source1.put("date_id", 1);
		source1.put("location", "location 1");
		Repository.save(EntityManager.NODE_NAME_SOURCE, source1);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("id", 2);
		source2.put("repository_id", 1);
		source2.put("identifier", "source 2");
		source2.put("type", "newspaper");
		source2.put("author", "author 2");
		source2.put("location", "location 2");
		Repository.save(EntityManager.NODE_NAME_SOURCE, source2);

		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("id", 1);
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		repository1.put("person_id", 1);
		repository1.put("place_id", 1);
		Repository.save(EntityManager.NODE_NAME_REPOSITORY, repository1);

		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		person1.put("photo_id", 3);
		person1.put("photo_crop", "0 0 5 10");
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
		eventType1.put("super_type_id", 2);
		eventType1.put("type", "birth");
		eventType1.put("category", "birth");
		Repository.save(EntityManager.NODE_NAME_EVENT_TYPE, eventType1);
		final Map<String, Object> eventType2 = new HashMap<>();
		eventType2.put("id", 2);
		eventType2.put("super_type_id", 15);
		eventType2.put("type", "death");
		eventType2.put("category", "death");
		Repository.save(EntityManager.NODE_NAME_EVENT_TYPE, eventType2);
		final Map<String, Object> eventType3 = new HashMap<>();
		eventType3.put("id", 3);
		eventType3.put("super_type_id", 10);
		eventType3.put("type", "marriage");
		eventType3.put("category", "union");
		Repository.save(EntityManager.NODE_NAME_EVENT_TYPE, eventType3);

		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("id", 1);
		eventSuperType1.put("super_type", "Historical events");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType1);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("id", 2);
		eventSuperType2.put("super_type", "Personal origins");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType2);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("id", 3);
		eventSuperType3.put("super_type", "Physical description");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType3);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("id", 4);
		eventSuperType4.put("super_type", "Citizenship and migration");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType4);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("id", 5);
		eventSuperType5.put("super_type", "Real estate assets");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType5);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("id", 6);
		eventSuperType6.put("super_type", "Education");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType6);
		final Map<String, Object> eventSuperType7 = new HashMap<>();
		eventSuperType7.put("id", 7);
		eventSuperType7.put("super_type", "Work and Career");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType7);
		final Map<String, Object> eventSuperType8 = new HashMap<>();
		eventSuperType8.put("id", 8);
		eventSuperType8.put("super_type", "Legal Events and Documents");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType8);
		final Map<String, Object> eventSuperType9 = new HashMap<>();
		eventSuperType9.put("id", 9);
		eventSuperType9.put("super_type", "Health problems and habits");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType9);
		final Map<String, Object> eventSuperType10 = new HashMap<>();
		eventSuperType10.put("id", 10);
		eventSuperType10.put("super_type", "Marriage and family life");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType10);
		final Map<String, Object> eventSuperType11 = new HashMap<>();
		eventSuperType11.put("id", 11);
		eventSuperType11.put("super_type", "Military");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType11);
		final Map<String, Object> eventSuperType12 = new HashMap<>();
		eventSuperType12.put("id", 12);
		eventSuperType12.put("super_type", "Confinement");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType12);
		final Map<String, Object> eventSuperType13 = new HashMap<>();
		eventSuperType13.put("id", 13);
		eventSuperType13.put("super_type", "Transfers and travel");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType13);
		final Map<String, Object> eventSuperType14 = new HashMap<>();
		eventSuperType14.put("id", 14);
		eventSuperType14.put("super_type", "Accolades");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType14);
		final Map<String, Object> eventSuperType15 = new HashMap<>();
		eventSuperType15.put("id", 15);
		eventSuperType15.put("super_type", "Death and burial");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType15);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("id", 16);
		eventSuperType16.put("super_type", "Others");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType16);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("id", 17);
		eventSuperType17.put("super_type", "Religious events");
		Repository.save(EntityManager.NODE_NAME_EVENT_SUPER_TYPE, eventSuperType17);

		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("id", 1);
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("calendar_original_id", 2);
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.save(EntityManager.NODE_NAME_HISTORIC_DATE, historicDate1);
		final Map<String, Object> historicDate2 = new HashMap<>();
		historicDate2.put("id", 2);
		historicDate2.put("date", "1 JAN 1800");
		Repository.save(EntityManager.NODE_NAME_HISTORIC_DATE, historicDate2);

		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		Repository.save(EntityManager.NODE_NAME_CALENDAR, calendar1);
		final Map<String, Object> calendar2 = new HashMap<>();
		calendar2.put("id", 2);
		calendar2.put("type", "julian");
		Repository.save(EntityManager.NODE_NAME_CALENDAR, calendar2);
		final Map<String, Object> calendar3 = new HashMap<>();
		calendar3.put("id", 3);
		calendar3.put("type", "venetan");
		Repository.save(EntityManager.NODE_NAME_CALENDAR, calendar3);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 2");
		note2.put("reference_table", "note");
		note2.put("reference_id", 2);
		Repository.save(EntityManager.NODE_NAME_NOTE, note2);
		final Map<String, Object> note3 = new HashMap<>();
		note3.put("id", 3);
		note3.put("note", "note for repository");
		note3.put("reference_table", "repository");
		note3.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note3);
		final Map<String, Object> note4 = new HashMap<>();
		note4.put("id", 4);
		note4.put("note", "something to say");
		note4.put("reference_table", "modification");
		note4.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note4);
		final Map<String, Object> note5 = new HashMap<>();
		note5.put("id", 5);
		note5.put("note", "something more to say");
		note5.put("reference_table", "modification");
		note5.put("reference_id", 2);
		Repository.save(EntityManager.NODE_NAME_NOTE, note5);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		Repository.save(EntityManager.NODE_NAME_MEDIA, media1);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("id", 2);
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		Repository.save(EntityManager.NODE_NAME_MEDIA, media2);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("id", 3);
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		Repository.save(EntityManager.NODE_NAME_MEDIA, media3);

		final Map<String, Object> mediaJunction1 = new HashMap<>();
		mediaJunction1.put("photo_crop", "0 0 10 50");
		Repository.upsertRelationship(EntityManager.NODE_NAME_MEDIA, extractRecordID(media3), EntityManager.NODE_NAME_REPOSITORY, 1,
			EntityManager.RELATIONSHIP_NAME_FOR, mediaJunction1, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final TreeMap<Integer, Map<String, Object>> mediaJunctions = new TreeMap<>();

		final Map<String, Object> culturalNorm1 = new HashMap<>();
		culturalNorm1.put("id", 1);
		culturalNorm1.put("identifier", "rule 1 id");
		culturalNorm1.put("description", "rule 1");
		culturalNorm1.put("place_id", 1);
		culturalNorm1.put("certainty", "certain");
		culturalNorm1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.save(EntityManager.NODE_NAME_CULTURAL_NORM, culturalNorm1);

		final Map<String, Object> culturalNormJunction1 = new HashMap<>();
		culturalNormJunction1.put("id", 1);
		culturalNormJunction1.put("certainty", "probable");
		culturalNormJunction1.put("credibility", "probable");
		Repository.upsertRelationship(EntityManager.NODE_NAME_CULTURAL_NORM, extractRecordID(culturalNorm1),
			EntityManager.NODE_NAME_PERSON_NAME, extractRecordID(personName1),
			EntityManager.RELATIONSHIP_NAME_SUPPORTED_BY, culturalNormJunction1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> modification1 = new HashMap<>();
		modification1.put("id", 1);
		modification1.put("reference_table", "repository");
		modification1.put("reference_id", 1);
		modification1.put("update_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
		Repository.save(EntityManager.NODE_NAME_MODIFICATION, modification1);
		final Map<String, Object> modification2 = new HashMap<>();
		modification2.put("id", 2);
		modification2.put("reference_table", "repository");
		modification2.put("reference_id", 1);
		modification2.put("update_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		Repository.save(EntityManager.NODE_NAME_MODIFICATION, modification2);

		final Map<String, Object> researchStatus1 = new HashMap<>();
		researchStatus1.put("id", 1);
		researchStatus1.put("reference_table", "date");
		researchStatus1.put("reference_id", 1);
		researchStatus1.put("identifier", "research 1");
		researchStatus1.put("description", "see people, do things");
		researchStatus1.put("status", "open");
		researchStatus1.put("priority", 2);
		Repository.save(EntityManager.NODE_NAME_RESEARCH_STATUS, researchStatus1);
		final Map<String, Object> researchStatus2 = new HashMap<>();
		researchStatus2.put("id", 2);
		researchStatus2.put("reference_table", "repository");
		researchStatus2.put("reference_id", 1);
		researchStatus2.put("identifier", "identifier 1");
		researchStatus2.put("description", "some description");
		researchStatus2.put("status", "open");
		researchStatus2.put("priority", 0);
		researchStatus2.put("creation_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
		Repository.save(EntityManager.NODE_NAME_RESEARCH_STATUS, researchStatus2);
		final Map<String, Object> researchStatus3 = new HashMap<>();
		researchStatus3.put("id", 3);
		researchStatus3.put("reference_table", "repository");
		researchStatus3.put("reference_id", 1);
		researchStatus3.put("identifier", "identifier 2");
		researchStatus3.put("description", "another description");
		researchStatus3.put("status", "active");
		researchStatus3.put("priority", 1);
		researchStatus3.put("creation_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		Repository.save(EntityManager.NODE_NAME_RESEARCH_STATUS, researchStatus3);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final RecordListenerInterface linkListener = new RecordListenerInterface(){
				@Override
				public void onRecordSelect(final String table, final Integer id){
					System.out.println("onRecordSelect " + table + " " + id);

					CommonListDialog recordDialog = null;
					switch(table){
						case "repository" -> recordDialog = RepositoryDialog.createShowOnly(parent);
						case "source" -> recordDialog = SourceDialog.createShowOnly(parent);
						case "citation" -> recordDialog = CitationDialog.createShowOnly(parent);
						case "place" -> recordDialog = PlaceDialog.createShowOnly(parent);
						case "media" -> recordDialog = MediaDialog.createShowOnly(parent)
							.withBasePath(FileHelper.documentsDirectory());
						case "note" -> recordDialog = NoteDialog.createShowOnly(parent);
						case "person" -> recordDialog = PersonDialog.createShowOnly(parent);
						case "group" -> recordDialog = GroupDialog.createShowOnly(parent);
						case "event" -> recordDialog = EventDialog.createShowOnly(parent);
						case "cultural_norm" -> recordDialog = CulturalNormDialog.createShowOnly(parent);
						case "research_status" -> recordDialog = ResearchStatusDialog.createShowOnly(parent);
					}
					if(recordDialog != null){
						recordDialog.loadData(id);

						recordDialog.showDialog();
					}
				}

				@Override
				public void onRecordEdit(final String table, final Integer id){
					System.out.println("onRecordEdit " + table + " " + id);

					CommonListDialog recordDialog = null;
					switch(table){
						case "repository" -> recordDialog = RepositoryDialog.createEditOnly(parent);
						case "source" -> recordDialog = SourceDialog.createEditOnly(parent);
						case "citation" -> recordDialog = CitationDialog.createEditOnly(parent);
						case "place" -> recordDialog = PlaceDialog.createEditOnly(parent);
						case "media" -> recordDialog = MediaDialog.createEditOnly(parent)
							.withBasePath(FileHelper.documentsDirectory());
						case "note" -> recordDialog = NoteDialog.createEditOnly(parent);
						case "person" -> recordDialog = PersonDialog.createEditOnly(parent);
						case "group" -> recordDialog = GroupDialog.createEditOnly(parent);
						case "event" -> recordDialog = EventDialog.createEditOnly(parent);
						case "cultural_norm" -> recordDialog = CulturalNormDialog.createEditOnly(parent);
						case "research_status" -> recordDialog = ResearchStatusDialog.createEditOnly(parent);
					}
					if(recordDialog != null){
						recordDialog.loadData(id);

						recordDialog.showDialog();
					}
				}
			};
			final SearchDialog dialog = create(parent)
				.withLinkListener(linkListener);
			dialog.loadData();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					final Map<String, Object> container = editCommand.getContainer();
					switch(editCommand.getType()){
						case SEARCH -> { if(dialog.isShowing()) dialog.loadData(); }

						//from: repository
						case SOURCE -> {
							final Integer repositoryID = extractRecordID(container);
							final SourceDialog sourceDialog = SourceDialog.createSelectOnly(parent)
								.withFilterOnRepositoryID(repositoryID);
							sourceDialog.loadData();

							sourceDialog.showDialog();
						}

						//from: source
						case CITATION -> {
							final Integer sourceID = extractRecordID(container);
							final CitationDialog citationDialog = CitationDialog.createSelectOnly(parent)
								.withFilterOnSourceID(sourceID);
							citationDialog.loadData();

							citationDialog.showDialog();
						}

						//from: citation, person, person name, group, media, place, cultural norm, historic date, calendar
						case ASSERTION -> {
							final String tableName = editCommand.getIdentifier();
							final Integer recordID = extractRecordID(container);
							final AssertionDialog assertionDialog = AssertionDialog.createSelectOnly(parent)
								.withReference(tableName, recordID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}


						//from: source, event, cultural norm, media
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.createRecordOnly(parent);
							final Integer dateID = extractRecordDateID(container);
							historicDateDialog.loadData(dateID);

							historicDateDialog.showDialog();
						}

						//from: historic date
						case CALENDAR_ORIGINAL -> {
							final CalendarDialog calendarDialog = CalendarDialog.createRecordOnly(parent);
							final Integer calendarID = extractRecordCalendarOriginalID(container);
							calendarDialog.loadData(calendarID);

							calendarDialog.showDialog();
						}


						//from: repository, source, event, cultural norm
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.createShowOnly(parent);
							final Integer placeID = extractRecordPlaceID(container);
							placeDialog.loadData(placeID);

							placeDialog.showDialog();
						}


						//from: repository, source, citation, assertion, historic date, calendar, person, person name, group, event,
						// cultural norm, media, place
						case NOTE -> {
							final String tableName = editCommand.getIdentifier();
							final Integer recordID = extractRecordID(container);
							final NoteDialog noteDialog = NoteDialog.createSelectOnly(parent)
								.withReference(tableName, recordID);
							noteDialog.loadData();

							noteDialog.showDialog();
						}


						//from: citation
						case LOCALIZED_EXTRACT -> {
							final String tableName = editCommand.getIdentifier();
							final Integer recordID = extractRecordID(container);
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createRecordOnlySimpleText(parent)
								.withReference(tableName, recordID, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT);
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}

						//from: person name
						case LOCALIZED_PERSON_NAME -> {
							final Integer personNameID = extractRecordID(container);
							final LocalizedPersonNameDialog localizedTextDialog = LocalizedPersonNameDialog.createSelectOnly(parent)
								.withReference(personNameID);
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}

						//from: place
						case LOCALIZED_PLACE_NAME -> {
							final String tableName = editCommand.getIdentifier();
							final Integer placeID = extractRecordID(container);
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSelectOnly(parent)
								.withReference(tableName, placeID, EntityManager.LOCALIZED_TEXT_TYPE_NAME);
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}


						//from: repository, source, citation, assertion, person, person name, group, event, cultural norm, note, place
						case MEDIA -> {
							final String tableName = editCommand.getIdentifier();
							final Integer recordID = extractRecordID(container);
							final MediaDialog mediaDialog = MediaDialog.createSelectOnlyForMedia(parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(tableName, recordID);
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}

						//from: person, group, place
						case PHOTO -> {
							final MediaDialog photoDialog;
							final Integer photoID = extractRecordPhotoID(container);
							if(photoID != null){
								photoDialog = MediaDialog.createEditOnlyForPhoto(parent)
									.withBasePath(FileHelper.documentsDirectory());
								photoDialog.loadData(photoID);
							}
							else{
								photoDialog = MediaDialog.createForPhoto(parent)
									.withBasePath(FileHelper.documentsDirectory());
								photoDialog.loadData();
							}

							photoDialog.showDialog();
						}

						//from: person, group, media, place
						case PHOTO_CROP -> {
							try{
								final PhotoCropDialog photoCropDialog = PhotoCropDialog.createSelectOnly(parent);
								final Integer recordID = extractRecordID(container);
								final String photoCrop = extractRecordPhotoCrop(container);
								photoCropDialog.loadData(recordID, photoCrop);

								photoCropDialog.setSize(420, 295);
								photoCropDialog.showDialog();
							}
							catch(final IOException ignored){}
						}


						//from: repository
						case PERSON -> {
							final PersonDialog personDialog = PersonDialog.createShowOnly(parent);
							final Integer personID = extractRecordPersonID(container);
							personDialog.loadData(personID);

							personDialog.showDialog();
						}

						//from: person
						case PERSON_NAME -> {
							final Integer personID = extractRecordID(container);
							final PersonNameDialog personNameDialog = PersonNameDialog.createSelectOnly(parent)
								.withReference(personID);
							personNameDialog.loadData();

							personNameDialog.showDialog();
						}


						//from: person, group, place
						case GROUP -> {
							final String tableName = editCommand.getIdentifier();
							final Integer recordID = extractRecordID(container);
							final GroupDialog groupDialog = GroupDialog.createSelectOnly(parent)
								.withReference(tableName, recordID);
							groupDialog.loadData();

							groupDialog.showDialog();
						}


						//from: calendar, person, person name, group, cultural norm, media, place
						case EVENT -> {
							final String tableName = editCommand.getIdentifier();
							final Integer recordID = extractRecordID(container);
							final EventDialog eventDialog = EventDialog.createSelectOnly(parent)
								.withReference(tableName, recordID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}


						//from: assertion, person name, group, note
						case CULTURAL_NORM -> {
							final String tableName = editCommand.getIdentifier();
							final Integer recordID = extractRecordID(container);
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.createSelectOnly(parent)
								.withReference(tableName, recordID);
							culturalNormDialog.loadData();

							culturalNormDialog.showDialog();
						}


						//from: modification notes
						case MODIFICATION_HISTORY -> {
							final int recordID = extractRecordID(container);
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + recordID);
							changeNoteDialog.loadData(noteID);

							changeNoteDialog.showDialog();
						}


						//from: assertion, calendar, citation, cultural norm, event, group, historic date, localized person name, localized text,
						// media, note, person name, place, repository, source
						case RESEARCH_STATUS -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final ResearchStatusDialog researchStatusDialog = (showOnly
								? ResearchStatusDialog.createShowOnly(parent)
								: ResearchStatusDialog.createEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + researchStatusID);
							researchStatusDialog.loadData(researchStatusID);

							researchStatusDialog.showDialog();
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.showDialog();
		});
	}

}
