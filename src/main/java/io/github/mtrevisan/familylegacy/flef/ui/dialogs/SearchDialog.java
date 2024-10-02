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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCalendarOriginalID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoCrop;
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

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("identifier", "another place 1");
		place2.put("name", "name of the another place");
		int place2ID = Repository.upsert(place2, EntityManager.NODE_PLACE);

		final Map<String, Object> person1 = new HashMap<>();
		person1.put("photo_crop", "0 0 5 10");
		int person1ID = Repository.upsert(person1, EntityManager.NODE_PERSON);
		int person2ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person3ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person4ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person5ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		int repository1ID = Repository.upsert(repository1, EntityManager.NODE_REPOSITORY);
		Repository.upsertRelationship(EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_CREATED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person1ID,
			EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.RELATIONSHIP_OWNS, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int date1ID = Repository.upsert(historicDate1, EntityManager.NODE_HISTORIC_DATE);
		final Map<String, Object> historicDate2 = new HashMap<>();
		historicDate2.put("date", "1 JAN 1800");
		int date2ID = Repository.upsert(historicDate2, EntityManager.NODE_HISTORIC_DATE);

		final Map<String, Object> source1 = new HashMap<>();
		source1.put("identifier", "source 1");
		source1.put("type", "marriage certificate");
		source1.put("author", "author 1");
		source1.put("location", "location 1");
		int source1ID = Repository.upsert(source1, EntityManager.NODE_SOURCE);
		Repository.upsertRelationship(EntityManager.NODE_SOURCE, source1ID,
			EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.RELATIONSHIP_STORED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_SOURCE, source1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_CREATED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_SOURCE, source1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_CREATED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("identifier", "source 2");
		source2.put("type", "newspaper");
		source2.put("author", "author 2");
		source2.put("location", "location 2");
		int source2ID = Repository.upsert(source2, EntityManager.NODE_SOURCE);
		Repository.upsertRelationship(EntityManager.NODE_SOURCE, source2ID,
			EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.RELATIONSHIP_STORED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> citation1 = new HashMap<>();
		citation1.put("location", "here");
		citation1.put("extract", "text 1");
		citation1.put("extract_locale", "en-US");
		citation1.put("extract_type", "transcript");
		int citation1ID = Repository.upsert(citation1, EntityManager.NODE_CITATION);
		Repository.upsertRelationship(EntityManager.NODE_CITATION, citation1ID,
			EntityManager.NODE_SOURCE, source1ID,
			EntityManager.RELATIONSHIP_QUOTES, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> assertion1 = new HashMap<>();
		assertion1.put("role", "father");
		assertion1.put("certainty", "certain");
		assertion1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int assertion1ID = Repository.upsert(assertion1, EntityManager.NODE_ASSERTION);
		Repository.upsertRelationship(EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.NODE_CITATION, citation1ID,
			EntityManager.RELATIONSHIP_INFERRED_FROM, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("text", "text 1");
		localizedText1.put("locale", "it");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		int localizedText1ID = Repository.upsert(localizedText1, EntityManager.NODE_LOCALIZED_TEXT);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("text", "text 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "kana");
		localizedText2.put("transcription_type", "romanized");
		int localizedText2ID = Repository.upsert(localizedText2, EntityManager.NODE_LOCALIZED_TEXT);

		final Map<String, Object> localizedTextRelationship1 = new HashMap<>();
		localizedTextRelationship1.put("type", "name");
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, extractRecordID(localizedText1),
			EntityManager.NODE_PLACE, extractRecordID(place1),
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedTextRelationship2 = new HashMap<>();
		localizedTextRelationship2.put("type", "name");
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, extractRecordID(localizedText2),
			EntityManager.NODE_PLACE, extractRecordID(place1),
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedTextRelationship3 = new HashMap<>();
		localizedTextRelationship3.put("type", "extract");
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, extractRecordID(localizedText2),
			EntityManager.NODE_CITATION, extractRecordID(citation1),
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship3,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		int group1ID = Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("type", "family");
		int group2ID = Repository.upsert(group2, EntityManager.NODE_GROUP);

		final Map<String, Object> groupRelationship11 = new HashMap<>();
		groupRelationship11.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 1,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship11, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship2 = new HashMap<>();
		groupRelationship2.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 2,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship2, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship13 = new HashMap<>();
		groupRelationship13.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 1,
			EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship13, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship3 = new HashMap<>();
		groupRelationship3.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 3,
			EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship3, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship4 = new HashMap<>();
		groupRelationship4.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 4,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship4, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship5 = new HashMap<>();
		groupRelationship5.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 5,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship5, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship6 = new HashMap<>();
		groupRelationship6.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 4,
			EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship6, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

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
			EntityManager.NODE_PERSON, person1ID,
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
			EntityManager.NODE_HISTORIC_DATE, date2ID,
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
		eventType1.put("category", "birth");
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
		eventType2.put("category", "death");
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

		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("super_type", "Historical events");
		Repository.upsert(eventSuperType1, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("super_type", "Personal origins");
		int eventSuperType2ID = Repository.upsert(eventSuperType2, EntityManager.NODE_EVENT_SUPER_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, eventType1ID,
			EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType2ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
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
		int eventSuperType10ID = Repository.upsert(eventSuperType10, EntityManager.NODE_EVENT_SUPER_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, eventType3ID,
			EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType10ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
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
		int eventSuperType15ID = Repository.upsert(eventSuperType15, EntityManager.NODE_EVENT_SUPER_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, eventType2ID,
			EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType15ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("super_type", "Others");
		Repository.upsert(eventSuperType16, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("super_type", "Religious events");
		Repository.upsert(eventSuperType17, EntityManager.NODE_EVENT_SUPER_TYPE);

		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("type", "gregorian");
		int calendar1ID = Repository.upsert(calendar1, EntityManager.NODE_CALENDAR);
		final Map<String, Object> calendar2 = new HashMap<>();
		calendar2.put("type", "julian");
		int calendar2ID = Repository.upsert(calendar2, EntityManager.NODE_CALENDAR);
		Repository.upsertRelationship(EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.NODE_CALENDAR, calendar2ID,
			EntityManager.RELATIONSHIP_EXPRESSED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> calendar3 = new HashMap<>();
		calendar3.put("type", "venetan");
		int calendar3ID = Repository.upsert(calendar3, EntityManager.NODE_CALENDAR);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 2");
		int note2ID = Repository.upsert(note2, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note2ID,
			EntityManager.NODE_NOTE, note2ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note3 = new HashMap<>();
		note3.put("note", "note for repository");
		int note3ID = Repository.upsert(note3, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note3ID,
			EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note4 = new HashMap<>();
		note4.put("note", "something to say");
		int note4ID = Repository.upsert(note4, EntityManager.NODE_NOTE);
		final Map<String, Object> note5 = new HashMap<>();
		note5.put("note", "something more to say");
		int note5ID = Repository.upsert(note5, EntityManager.NODE_NOTE);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		int media1ID = Repository.upsert(media1, EntityManager.NODE_MEDIA);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		int media2ID = Repository.upsert(media2, EntityManager.NODE_MEDIA);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		int media3ID = Repository.upsert(media3, EntityManager.NODE_MEDIA);
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person1ID,
			EntityManager.NODE_MEDIA, media3ID,
			EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> mediaRelationship1 = new HashMap<>();
		mediaRelationship1.put("photo_crop", "0 0 10 50");
		Repository.upsertRelationship(EntityManager.NODE_MEDIA, extractRecordID(media3), EntityManager.NODE_REPOSITORY, 1,
			EntityManager.RELATIONSHIP_FOR, mediaRelationship1, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> culturalNorm1 = new HashMap<>();
		culturalNorm1.put("identifier", "rule 1 id");
		culturalNorm1.put("description", "rule 1");
		culturalNorm1.put("certainty", "certain");
		culturalNorm1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int culturalNorm1ID = Repository.upsert(culturalNorm1, EntityManager.NODE_CULTURAL_NORM);
		Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_APPLIES_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> culturalNormRelationship1 = new HashMap<>();
		culturalNormRelationship1.put("certainty", "probable");
		culturalNormRelationship1.put("credibility", "probable");
		Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, extractRecordID(culturalNorm1),
			EntityManager.NODE_PERSON_NAME, extractRecordID(personName1),
			EntityManager.RELATIONSHIP_SUPPORTED_BY, culturalNormRelationship1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> modification1 = new HashMap<>();
		modification1.put("update_date", EntityManager.now());
		int modification1ID = Repository.upsert(modification1, EntityManager.NODE_MODIFICATION);
		Repository.upsertRelationship(EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.NODE_MODIFICATION, modification1ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note4ID,
			EntityManager.NODE_MODIFICATION, modification1ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> modification2 = new HashMap<>();
		modification2.put("update_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		int modification2ID = Repository.upsert(modification2, EntityManager.NODE_MODIFICATION);
		Repository.upsertRelationship(EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.NODE_MODIFICATION, modification2ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note5ID,
			EntityManager.NODE_MODIFICATION, modification2ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> researchStatus1 = new HashMap<>();
		researchStatus1.put("identifier", "research 1");
		researchStatus1.put("description", "see people, do things");
		researchStatus1.put("status", "open");
		researchStatus1.put("priority", 2);
		int researchStatus1ID = Repository.upsert(researchStatus1, EntityManager.NODE_RESEARCH_STATUS);
		Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, researchStatus1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> researchStatus2 = new HashMap<>();
		researchStatus2.put("identifier", "identifier 1");
		researchStatus2.put("description", "some description");
		researchStatus2.put("status", "open");
		researchStatus2.put("priority", 0);
		researchStatus2.put("creation_date", EntityManager.now());
		int researchStatus2ID = Repository.upsert(researchStatus2, EntityManager.NODE_RESEARCH_STATUS);
		Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, researchStatus2ID,
			EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> researchStatus3 = new HashMap<>();
		researchStatus3.put("identifier", "identifier 2");
		researchStatus3.put("description", "another description");
		researchStatus3.put("status", "active");
		researchStatus3.put("priority", 1);
		researchStatus3.put("creation_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		int researchStatus3ID = Repository.upsert(researchStatus3, EntityManager.NODE_RESEARCH_STATUS);
		Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, researchStatus3ID,
			EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);


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
					final String tableName = editCommand.getIdentifier();
					final Integer containerID = extractRecordID(container);
					switch(editCommand.getType()){
						case SEARCH -> { if(dialog.isShowing()) dialog.loadData(); }

						//from: repository
						case SOURCE -> {
							final SourceDialog sourceDialog = SourceDialog.createSelectOnly(parent)
								.withFilterOnRepositoryID(containerID);
							sourceDialog.loadData();

							sourceDialog.showDialog();
						}

						//from: source
						case CITATION -> {
							final CitationDialog citationDialog = CitationDialog.createSelectOnly(parent)
								.withFilterOnSourceID(containerID);
							citationDialog.loadData();

							citationDialog.showDialog();
						}

						//from: citation, person, person name, group, media, place, cultural norm, historic date, calendar
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.createSelectOnly(parent)
								.withReference(tableName, containerID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}


						//from: source, event, cultural norm, media
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.createRecordOnly(parent);
							//FIXME
							final Integer dateID = extractRecordDateID(container);
							historicDateDialog.loadData(dateID);

							historicDateDialog.showDialog();
						}

						//from: historic date
						case CALENDAR_ORIGINAL -> {
							final CalendarDialog calendarDialog = CalendarDialog.createRecordOnly(parent);
							//FIXME
							final Integer calendarID = extractRecordCalendarOriginalID(container);
							calendarDialog.loadData(calendarID);

							calendarDialog.showDialog();
						}


						//from: repository, source, event, cultural norm
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.createShowOnly(parent);
							//FIXME
							final Integer placeID = extractRecordPlaceID(container);
							placeDialog.loadData(placeID);

							placeDialog.showDialog();
						}


						//from: repository, source, citation, assertion, historic date, calendar, person, person name, group, event,
						// cultural norm, media, place
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.createSelectOnly(parent)
								.withReference(tableName, containerID);
							noteDialog.loadData();

							noteDialog.showDialog();
						}


						//from: citation
						case LOCALIZED_EXTRACT -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createRecordOnlySimpleText(parent)
								.withReference(tableName, containerID, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT);
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}

						//from: person name
						case LOCALIZED_PERSON_NAME -> {
							final LocalizedPersonNameDialog localizedTextDialog = LocalizedPersonNameDialog.createSelectOnly(parent)
								.withReference(containerID);
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}

						//from: place
						case LOCALIZED_PLACE_NAME -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSelectOnly(parent)
								.withReference(tableName, containerID, EntityManager.LOCALIZED_TEXT_TYPE_NAME);
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}


						//from: repository, source, citation, assertion, person, person name, group, event, cultural norm, note, place
						case MEDIA -> {
							final MediaDialog mediaDialog = MediaDialog.createSelectOnlyForMedia(parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(tableName, containerID);
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}

						//from: person, group, place
						case PHOTO -> {
							final MediaDialog photoDialog;
							final Map<String, Object> photoRecord = Repository.getDepiction(tableName, containerID);
							final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
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
								final Map.Entry<String, Map<String, Object>> referencedNode = Repository.findReferencingNode(
									tableName, containerID,
									EntityManager.RELATIONSHIP_DEPICTED_BY);
								if(referencedNode != null){
									final String photoCrop = extractRecordPhotoCrop(referencedNode.getValue());
									photoCropDialog.loadData(containerID, photoCrop);
								}

								final String photoCrop = extractRecordPhotoCrop(container);
								photoCropDialog.loadData(containerID, photoCrop);

								photoCropDialog.setSize(420, 295);
								photoCropDialog.showDialog();
							}
							catch(final IOException ignored){}
						}


						//from: repository
						case PERSON -> {
							final PersonDialog personDialog = PersonDialog.createShowOnly(parent);
							//FIXME
							final Integer personID = extractRecordPersonID(container);
							personDialog.loadData(personID);

							personDialog.showDialog();
						}

						//from: person
						case PERSON_NAME -> {
							final PersonNameDialog personNameDialog = PersonNameDialog.createSelectOnly(parent)
								.withReference(containerID);
							personNameDialog.loadData();

							personNameDialog.showDialog();
						}


						//from: person, group, place
						case GROUP -> {
							final GroupDialog groupDialog = GroupDialog.createSelectOnly(parent)
								.withReference(tableName, containerID);
							groupDialog.loadData();

							groupDialog.showDialog();
						}


						//from: calendar, person, person name, group, cultural norm, media, place
						case EVENT -> {
							final EventDialog eventDialog = EventDialog.createSelectOnly(parent)
								.withReference(tableName, containerID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}


						//from: assertion, person name, group, note
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.createSelectOnly(parent)
								.withReference(tableName, containerID);
							culturalNormDialog.loadData();

							culturalNormDialog.showDialog();
						}


						//from: modification notes
						case MODIFICATION_HISTORY -> {
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + containerID);
							changeNoteDialog.loadData(noteID);

							changeNoteDialog.showDialog();
						}


						//from: assertion, calendar, citation, cultural norm, event, group, historic date, localized person name, localized text,
						// media, note, person name, place, repository, source
						case RESEARCH_STATUS -> {
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
