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


public class SearchCitationPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -4600075588908748922L;

	private static final int TABLE_INDEX_EXTRACT = 2;

	private static final int TABLE_PREFERRED_WIDTH_EXTRACT = 250;

	private static final String TABLE_NAME = "citation";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	private static final String TABLE_NAME_LOCALIZED_TEXT_JUNCTION = "localized_text_junction";


	public static SearchCitationPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new SearchCitationPanel(store);
	}


	private SearchCitationPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		super(store);


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_EXTRACT, 0, TABLE_PREFERRED_WIDTH_EXTRACT);
	}

	@Override
	public String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Extract"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator};
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

			final String extract = extractRecordExtract(container);
			final String extractLocale = extractRecordExtractLocale(container);
			final String extractType = extractRecordExtractType(container);
			final List<Map<String, Object>> transcribedExtracts = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
				key, SearchCitationPanel::extractRecordReferenceType, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(extract)
				.add(extractLocale)
				.add(extractType);
			for(final Map<String, Object> transcribedExtract : transcribedExtracts)
				filter.add(extractRecordText(transcribedExtract))
					.add(extractRecordLocale(transcribedExtract))
					.add(extractRecordType(transcribedExtract))
					.add(extractRecordTranscription(transcribedExtract))
					.add(extractRecordTranscriptionType(transcribedExtract));
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(extract, row, TABLE_INDEX_EXTRACT);

			tableData.add(new SearchAllRecord(key, TABLE_NAME, filterData, extract));

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

	private static String extractRecordExtract(final Map<String, Object> record){
		return (String)record.get("extract");
	}

	private static String extractRecordExtractLocale(final Map<String, Object> record){
		return (String)record.get("extract_locale");
	}

	private static String extractRecordExtractType(final Map<String, Object> record){
		return (String)record.get("extract_type");
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

	private static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	private static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordTranscription(final Map<String, Object> record){
		return (String)record.get("transcription");
	}

	private static String extractRecordTranscriptionType(final Map<String, Object> record){
		return (String)record.get("transcription_type");
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

		final TreeMap<Integer, Map<String, Object>> assertions = new TreeMap<>();
		store.put("assertion", assertions);
		final Map<String, Object> assertion = new HashMap<>();
		assertion.put("id", 1);
		assertion.put("citation_id", 1);
		assertion.put("reference_table", "table");
		assertion.put("reference_id", 1);
		assertion.put("role", "father");
		assertion.put("certainty", "certain");
		assertion.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		assertions.put((Integer)assertion.get("id"), assertion);

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put("source", sources);
		final Map<String, Object> source = new HashMap<>();
		source.put("id", 1);
		source.put("repository_id", 1);
		source.put("identifier", "source 1");
		sources.put((Integer)source.get("id"), source);

		final TreeMap<Integer, Map<String, Object>> repositories = new TreeMap<>();
		store.put("repository", repositories);
		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("id", 1);
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		repositories.put((Integer)repository1.get("id"), repository1);

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
		final Map<String, Object> localizedTextJunction = new HashMap<>();
		localizedTextJunction.put("id", 1);
		localizedTextJunction.put("localized_text_id", 2);
		localizedTextJunction.put("reference_type", "extract");
		localizedTextJunction.put("reference_table", "citation");
		localizedTextJunction.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction.get("id"), localizedTextJunction);

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
		note2.put("reference_table", "citation");
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

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
			final SearchCitationPanel panel = create(store);
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
