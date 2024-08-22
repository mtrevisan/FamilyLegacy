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
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchMediaPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchNotePanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchPersonPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchPlacePanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchRepositoryPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchResearchStatusPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchSourcePanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchUnionPanel;
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
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public final class SearchDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 7854594091959021095L;


	private static final class SearchData{
		private final String tableName;
		private final Class<? extends CommonSearchPanel> panel;
		private CommonSearchPanel instance;
		private Method loadDataMethod;
		private Method exportTableDataMethod;

		private static SearchData create(final Class<? extends CommonSearchPanel> panel){
			return new SearchData(null, panel);
		}

		private static SearchData create(final String tableName, final Class<? extends CommonSearchPanel> panel){
			return new SearchData(tableName, panel);
		}

		private SearchData(final String tableName, final Class<? extends CommonSearchPanel> panel){
			this.tableName = tableName;
			this.panel = panel;
		}
	}

	private static final Map<String, SearchData> PANE_NAME_TABLE = new TreeMap<>();
	static{
		PANE_NAME_TABLE.put("All", SearchData.create(SearchAllPanel.class));
		PANE_NAME_TABLE.put("Citations", SearchData.create("citation", SearchCitationPanel.class));
		PANE_NAME_TABLE.put("Sources", SearchData.create("source", SearchSourcePanel.class));
		PANE_NAME_TABLE.put("Repositories", SearchData.create("repository", SearchRepositoryPanel.class));
		PANE_NAME_TABLE.put("Places", SearchData.create("place", SearchPlacePanel.class));
		PANE_NAME_TABLE.put("Notes", SearchData.create("note", SearchNotePanel.class));
		PANE_NAME_TABLE.put("Medias", SearchData.create("media", SearchMediaPanel.class));
		PANE_NAME_TABLE.put("Persons", SearchData.create("person", SearchPersonPanel.class));
		PANE_NAME_TABLE.put("Groups", SearchData.create("group", SearchUnionPanel.class));
		PANE_NAME_TABLE.put("Events", SearchData.create("event", SearchEventPanel.class));
		PANE_NAME_TABLE.put("Cultural norms", SearchData.create("cultural_norm", SearchCulturalNormPanel.class));
		PANE_NAME_TABLE.put("Research statuses", SearchData.create("research_status", SearchResearchStatusPanel.class));
	}


	private JLabel filterLabel;
	private JTextField filterField;
	private final JTabbedPane recordTabbedPane = new JTabbedPane();

	private final Debouncer<SearchDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, CommonListDialog.DEBOUNCE_TIME);

	private Method loadDataAllMethod;

	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;


	public static SearchDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new SearchDialog(store, parent);
	}


	private SearchDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(parent, true);

		this.store = store;


		initComponents();
	}


	public SearchDialog withLinkListener(final RecordListenerInterface listener){
		try{
			final Method setLinkListenerMethod = CommonSearchPanel.class.getDeclaredMethod("setLinkListener",
				RecordListenerInterface.class);
			setLinkListenerMethod.setAccessible(true);

			for(final SearchData searchData : PANE_NAME_TABLE.values())
				setLinkListenerMethod.invoke(searchData.instance, listener);
		}
		catch(final NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored){}

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
			for(final SearchData searchData : PANE_NAME_TABLE.values()){
				final Class<? extends CommonSearchPanel> panelClass = searchData.panel;
				final Method createMethod = panelClass.getMethod("create", Map.class);
				final Method loadDataMethod = panelClass.getMethod("loadData");
				if(searchData.tableName == null)
					loadDataAllMethod = panelClass.getMethod("loadData", List.class);
				final Method exportTableDataMethod = (searchData.tableName != null? panelClass.getMethod("exportTableData"): null);

				final Object instance = createMethod.invoke(null, store);

				searchData.instance = (CommonSearchPanel)instance;
				searchData.loadDataMethod = loadDataMethod;
				searchData.exportTableDataMethod = exportTableDataMethod;
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
		PANE_NAME_TABLE.forEach((key, value) -> recordTabbedPane.add(key, value.instance));
	}

	private void closeAction(final ActionEvent evt){
		setVisible(false);
	}

	@SuppressWarnings("unchecked")
	public void loadData(){
		try{
			final List<SearchAllRecord> allTableData = new ArrayList<>(0);
			for(final SearchData searchData : PANE_NAME_TABLE.values()){
				if(searchData.tableName != null){
					final CommonSearchPanel instance = searchData.instance;
					searchData.loadDataMethod.invoke(instance);
					allTableData.addAll((List<SearchAllRecord>)searchData.exportTableDataMethod.invoke(instance));
				}
			}
			final CommonSearchPanel searchAllPanel = PANE_NAME_TABLE.values().iterator()
				.next()
				.instance;
			loadDataAllMethod.invoke(searchAllPanel, allTableData);
		}
		catch(final InvocationTargetException | IllegalAccessException ignored){}
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
		return PANE_NAME_TABLE.get(paneTitle)
			.tableName;
	}

	public static String getPaneTitle(final String tableName){
		return PANE_NAME_TABLE.entrySet().stream()
			.filter(entry -> tableName.equals(entry.getValue().tableName))
			.findFirst()
			.map(Map.Entry::getKey)
			.orElse(null);
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> citations = new TreeMap<>();
		store.put("citation", citations);
		final Map<String, Object> citation = new HashMap<>();
		citation.put("id", 1);
		citation.put("source_id", 1);
		citation.put("location", "here");
		citation.put("extract", "text 1");
		citation.put("extract_locale", "en-US");
		citation.put("extract_type", "transcript");
		citations.put((Integer)citation.get("id"), citation);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "text 1");
		localizedText1.put("locale", "it");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "text 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "kana");
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
		final Map<String, Object> localizedTextJunction3 = new HashMap<>();
		localizedTextJunction3.put("id", 3);
		localizedTextJunction3.put("localized_text_id", 2);
		localizedTextJunction3.put("reference_type", "extract");
		localizedTextJunction3.put("reference_table", "citation");
		localizedTextJunction3.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction3.get("id"), localizedTextJunction3);

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put("source", sources);
		final Map<String, Object> source1 = new HashMap<>();
		source1.put("id", 1);
		source1.put("repository_id", 1);
		source1.put("identifier", "source 1");
		source1.put("type", "marriage certificate");
		source1.put("author", "author 1");
		source1.put("place_id", 1);
		source1.put("date_id", 1);
		source1.put("location", "location 1");
		sources.put((Integer)source1.get("id"), source1);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("id", 2);
		source2.put("repository_id", 1);
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

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		persons.put((Integer)person1.get("id"), person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		persons.put((Integer)person2.get("id"), person2);
		final Map<String, Object> person3 = new HashMap<>();
		person3.put("id", 3);
		persons.put((Integer)person3.get("id"), person3);
		final Map<String, Object> person4 = new HashMap<>();
		person4.put("id", 4);
		persons.put((Integer)person4.get("id"), person4);
		final Map<String, Object> person5 = new HashMap<>();
		person5.put("id", 5);
		persons.put((Integer)person5.get("id"), person5);

		final TreeMap<Integer, Map<String, Object>> groups = new TreeMap<>();
		store.put("group", groups);
		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		groups.put((Integer)group1.get("id"), group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "family");
		groups.put((Integer)group2.get("id"), group2);

		final TreeMap<Integer, Map<String, Object>> groupJunctions = new TreeMap<>();
		store.put("group_junction", groupJunctions);
		final Map<String, Object> groupJunction11 = new HashMap<>();
		groupJunction11.put("id", 1);
		groupJunction11.put("group_id", 1);
		groupJunction11.put("reference_table", "person");
		groupJunction11.put("reference_id", 1);
		groupJunction11.put("role", "partner");
		groupJunctions.put((Integer)groupJunction11.get("id"), groupJunction11);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("id", 2);
		groupJunction2.put("group_id", 1);
		groupJunction2.put("reference_table", "person");
		groupJunction2.put("reference_id", 2);
		groupJunction2.put("role", "partner");
		groupJunctions.put((Integer)groupJunction2.get("id"), groupJunction2);
		final Map<String, Object> groupJunction13 = new HashMap<>();
		groupJunction13.put("id", 3);
		groupJunction13.put("group_id", 2);
		groupJunction13.put("reference_table", "person");
		groupJunction13.put("reference_id", 1);
		groupJunction13.put("role", "partner");
		groupJunctions.put((Integer)groupJunction13.get("id"), groupJunction13);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("id", 4);
		groupJunction3.put("group_id", 2);
		groupJunction3.put("reference_table", "person");
		groupJunction3.put("reference_id", 3);
		groupJunction3.put("role", "partner");
		groupJunctions.put((Integer)groupJunction3.get("id"), groupJunction3);
		final Map<String, Object> groupJunction4 = new HashMap<>();
		groupJunction4.put("id", 5);
		groupJunction4.put("group_id", 1);
		groupJunction4.put("reference_table", "person");
		groupJunction4.put("reference_id", 4);
		groupJunction4.put("role", "child");
		groupJunctions.put((Integer)groupJunction4.get("id"), groupJunction4);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("id", 6);
		groupJunction5.put("group_id", 1);
		groupJunction5.put("reference_table", "person");
		groupJunction5.put("reference_id", 5);
		groupJunction5.put("role", "child");
		groupJunctions.put((Integer)groupJunction5.get("id"), groupJunction5);
		final Map<String, Object> groupJunction6 = new HashMap<>();
		groupJunction6.put("id", 7);
		groupJunction6.put("group_id", 2);
		groupJunction6.put("reference_table", "person");
		groupJunction6.put("reference_id", 4);
		groupJunction6.put("role", "partner");
		groupJunctions.put((Integer)groupJunction6.get("id"), groupJunction6);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "personal name");
		personName1.put("family_name", "family name");
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("personal_name", "personal name 2");
		personName2.put("family_name", "family name 2");
		personName2.put("type", "death name");
		personNames.put((Integer)personName2.get("id"), personName2);
		final Map<String, Object> personName3 = new HashMap<>();
		personName3.put("id", 3);
		personName3.put("person_id", 2);
		personName3.put("personal_name", "personal name 3");
		personName3.put("family_name", "family name 3");
		personName3.put("type", "other name");
		personNames.put((Integer)personName3.get("id"), personName3);

		final TreeMap<Integer, Map<String, Object>> localizedPersonNames = new TreeMap<>();
		store.put("localized_person_name", localizedPersonNames);
		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("id", 1);
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("person_name_id", 1);
		localizedPersonNames.put((Integer)localizedPersonName1.get("id"), localizedPersonName1);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("id", 2);
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("person_name_id", 1);
		localizedPersonNames.put((Integer)localizedPersonName2.get("id"), localizedPersonName2);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("id", 3);
		localizedPersonName3.put("personal_name", "other");
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("person_name_id", 1);
		localizedPersonNames.put((Integer)localizedPersonName3.get("id"), localizedPersonName3);

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type_id", 1);
		event1.put("description", "a birth");
		event1.put("place_id", 1);
		event1.put("date_id", 1);
		event1.put("reference_table", "person");
		event1.put("reference_id", 1);
		events.put((Integer)event1.get("id"), event1);
		final Map<String, Object> event2 = new HashMap<>();
		event2.put("id", 2);
		event2.put("type_id", 1);
		event2.put("description", "another birth");
		event2.put("place_id", 2);
		event2.put("date_id", 2);
		event2.put("reference_table", "person");
		event2.put("reference_id", 1);
		events.put((Integer)event2.get("id"), event2);
		final Map<String, Object> event3 = new HashMap<>();
		event3.put("id", 3);
		event3.put("type_id", 2);
		event3.put("date_id", 1);
		event3.put("reference_table", "person");
		event3.put("reference_id", 2);
		events.put((Integer)event3.get("id"), event3);
		final Map<String, Object> event4 = new HashMap<>();
		event4.put("id", 4);
		event4.put("type_id", 3);
		event4.put("date_id", 1);
		event4.put("place_id", 1);
		event4.put("reference_table", "group");
		event4.put("reference_id", 1);
		events.put((Integer)event4.get("id"), event4);

		final TreeMap<Integer, Map<String, Object>> eventTypes = new TreeMap<>();
		store.put("event_type", eventTypes);
		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("super_type_id", 2);
		eventType1.put("type", "birth");
		eventType1.put("category", "birth");
		eventTypes.put((Integer)eventType1.get("id"), eventType1);
		final Map<String, Object> eventType2 = new HashMap<>();
		eventType2.put("id", 2);
		eventType2.put("super_type_id", 15);
		eventType2.put("type", "death");
		eventType2.put("category", "death");
		eventTypes.put((Integer)eventType2.get("id"), eventType2);
		final Map<String, Object> eventType3 = new HashMap<>();
		eventType3.put("id", 3);
		eventType3.put("super_type_id", 10);
		eventType3.put("type", "marriage");
		eventType3.put("category", "union");
		eventTypes.put((Integer)eventType3.get("id"), eventType3);

		final TreeMap<Integer, Map<String, Object>> eventSuperTypes = new TreeMap<>();
		store.put("event_super_type", eventSuperTypes);
		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("id", 1);
		eventSuperType1.put("super_type", "Historical events");
		eventSuperTypes.put((Integer)eventSuperType1.get("id"), eventSuperType1);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("id", 2);
		eventSuperType2.put("super_type", "Personal origins");
		eventSuperTypes.put((Integer)eventSuperType2.get("id"), eventSuperType2);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("id", 3);
		eventSuperType3.put("super_type", "Physical description");
		eventSuperTypes.put((Integer)eventSuperType3.get("id"), eventSuperType3);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("id", 4);
		eventSuperType4.put("super_type", "Citizenship and migration");
		eventSuperTypes.put((Integer)eventSuperType4.get("id"), eventSuperType4);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("id", 5);
		eventSuperType5.put("super_type", "Real estate assets");
		eventSuperTypes.put((Integer)eventSuperType5.get("id"), eventSuperType5);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("id", 6);
		eventSuperType6.put("super_type", "Education");
		eventSuperTypes.put((Integer)eventSuperType6.get("id"), eventSuperType6);
		final Map<String, Object> eventSuperType7 = new HashMap<>();
		eventSuperType7.put("id", 7);
		eventSuperType7.put("super_type", "Work and Career");
		eventSuperTypes.put((Integer)eventSuperType7.get("id"), eventSuperType7);
		final Map<String, Object> eventSuperType8 = new HashMap<>();
		eventSuperType8.put("id", 8);
		eventSuperType8.put("super_type", "Legal Events and Documents");
		eventSuperTypes.put((Integer)eventSuperType8.get("id"), eventSuperType8);
		final Map<String, Object> eventSuperType9 = new HashMap<>();
		eventSuperType9.put("id", 9);
		eventSuperType9.put("super_type", "Health problems and habits");
		eventSuperTypes.put((Integer)eventSuperType9.get("id"), eventSuperType9);
		final Map<String, Object> eventSuperType10 = new HashMap<>();
		eventSuperType10.put("id", 10);
		eventSuperType10.put("super_type", "Marriage and family life");
		eventSuperTypes.put((Integer)eventSuperType10.get("id"), eventSuperType10);
		final Map<String, Object> eventSuperType11 = new HashMap<>();
		eventSuperType11.put("id", 11);
		eventSuperType11.put("super_type", "Military");
		eventSuperTypes.put((Integer)eventSuperType11.get("id"), eventSuperType11);
		final Map<String, Object> eventSuperType12 = new HashMap<>();
		eventSuperType12.put("id", 12);
		eventSuperType12.put("super_type", "Confinement");
		eventSuperTypes.put((Integer)eventSuperType12.get("id"), eventSuperType12);
		final Map<String, Object> eventSuperType13 = new HashMap<>();
		eventSuperType13.put("id", 13);
		eventSuperType13.put("super_type", "Transfers and travel");
		eventSuperTypes.put((Integer)eventSuperType13.get("id"), eventSuperType13);
		final Map<String, Object> eventSuperType14 = new HashMap<>();
		eventSuperType14.put("id", 14);
		eventSuperType14.put("super_type", "Accolades");
		eventSuperTypes.put((Integer)eventSuperType14.get("id"), eventSuperType14);
		final Map<String, Object> eventSuperType15 = new HashMap<>();
		eventSuperType15.put("id", 15);
		eventSuperType15.put("super_type", "Death and burial");
		eventSuperTypes.put((Integer)eventSuperType15.get("id"), eventSuperType15);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("id", 16);
		eventSuperType16.put("super_type", "Others");
		eventSuperTypes.put((Integer)eventSuperType16.get("id"), eventSuperType16);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("id", 17);
		eventSuperType17.put("super_type", "Religious events");
		eventSuperTypes.put((Integer)eventSuperType17.get("id"), eventSuperType17);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		places.put((Integer)place1.get("id"), place1);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("id", 2);
		place2.put("identifier", "another place 1");
		place2.put("name", "name of the another place");
		places.put((Integer)place2.get("id"), place2);

		final TreeMap<Integer, Map<String, Object>> historicDates = new TreeMap<>();
		store.put("historic_date", historicDates);
		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("id", 1);
		historicDate1.put("date", "27 FEB 1976");
		historicDates.put((Integer)historicDate1.get("id"), historicDate1);
		final Map<String, Object> historicDate2 = new HashMap<>();
		historicDate2.put("id", 2);
		historicDate2.put("date", "1 JAN 1800");
		historicDates.put((Integer)historicDate2.get("id"), historicDate2);

		final TreeMap<Integer, Map<String, Object>> calendars = new TreeMap<>();
		store.put("calendar", calendars);
		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		calendars.put((Integer)calendar1.get("id"), calendar1);

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
		note2.put("note", "note 2");
		note2.put("reference_table", "note");
		note2.put("reference_id", 2);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> medias = new TreeMap<>();
		store.put("media", medias);
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		medias.put((Integer)media1.get("id"), media1);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("id", 2);
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		medias.put((Integer)media2.get("id"), media2);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("id", 3);
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		medias.put((Integer)media3.get("id"), media3);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "18 OCT 2000");
		dates.put((Integer)date1.get("id"), date1);

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

		final TreeMap<Integer, Map<String, Object>> researchStatuses = new TreeMap<>();
		store.put("research_status", researchStatuses);
		final Map<String, Object> researchStatus = new HashMap<>();
		researchStatus.put("id", 1);
		researchStatus.put("reference_table", "date");
		researchStatus.put("reference_id", 1);
		researchStatus.put("identifier", "research 1");
		researchStatus.put("description", "see people, do things");
		researchStatus.put("status", "open");
		researchStatus.put("priority", 2);
		researchStatuses.put((Integer)researchStatus.get("id"), researchStatus);

		final RecordListenerInterface linkListener = new RecordListenerInterface(){
			@Override
			public void onRecordSelected(final String table, final Integer id){
				System.out.println("onRecordSelected " + table + " " + id);
			}
		};

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final SearchDialog dialog = create(store, parent)
				.withLinkListener(linkListener);
			dialog.loadData();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}