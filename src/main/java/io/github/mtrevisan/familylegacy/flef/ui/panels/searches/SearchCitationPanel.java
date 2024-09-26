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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtract;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtractLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtractType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordText;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTranscription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTranscriptionType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;


public class SearchCitationPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -4600075588908748922L;

	private static final int TABLE_INDEX_EXTRACT = 2;

	private static final int TABLE_PREFERRED_WIDTH_EXTRACT = 250;


	public static SearchCitationPanel create(){
		return new SearchCitationPanel();
	}


	private SearchCitationPanel(){
		super();


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_EXTRACT, 0, TABLE_PREFERRED_WIDTH_EXTRACT);
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_CITATION;
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


		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_CITATION);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(int i = 0, length = records.size(); i < length; i ++){
			final Map<String, Object> record = records.get(i);

			final Integer recordID = extractRecordID(record);
			final String extract = extractRecordExtract(record);
			final String extractLocale = extractRecordExtractLocale(record);
			final String extractType = extractRecordExtractType(record);
			final List<Map<String, Object>> transcribedExtracts = Repository.findReferencingNodes(EntityManager.NODE_LOCALIZED_TEXT,
				EntityManager.NODE_CITATION, recordID,
				EntityManager.RELATIONSHIP_FOR, EntityManager.PROPERTY_TYPE, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT);
			final FilterString filter = FilterString.create().add(recordID).add(extract).add(extractLocale).add(extractType);
			for(final Map<String, Object> transcribedExtract : transcribedExtracts)
				filter.add(extractRecordText(transcribedExtract))
					.add(extractRecordLocale(transcribedExtract))
					.add(extractRecordType(transcribedExtract))
					.add(extractRecordTranscription(transcribedExtract))
					.add(extractRecordTranscriptionType(transcribedExtract));
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(extract, row, TABLE_INDEX_EXTRACT);

			tableData.add(new SearchAllRecord(recordID, EntityManager.NODE_CITATION, filterData, extract));

			row++;
		}
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> citation1 = new HashMap<>();
		citation1.put("source_id", 1);
		citation1.put("location", "here");
		citation1.put("extract", "text 1");
		citation1.put("extract_locale", "en-US");
		citation1.put("extract_type", "transcript");
		Repository.upsert(citation1, EntityManager.NODE_CITATION);

		final Map<String, Object> assertion1 = new HashMap<>();
		assertion1.put("citation_id", 1);
assertion1.put("reference_table", "table");
assertion1.put("reference_id", 1);
		assertion1.put("role", "father");
		assertion1.put("certainty", "certain");
		assertion1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsert(assertion1, EntityManager.NODE_ASSERTION);

		final Map<String, Object> source1 = new HashMap<>();
		source1.put("repository_id", 1);
		source1.put("identifier", "source 1");
		Repository.upsert(source1, EntityManager.NODE_SOURCE);

		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		Repository.upsert(repository1, EntityManager.NODE_REPOSITORY);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("text", "text 1");
		localizedText1.put("locale", "it");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		Repository.upsert(localizedText1, EntityManager.NODE_LOCALIZED_TEXT);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("text", "text 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "kana");
		localizedText2.put("transcription_type", "romanized");
		Repository.upsert(localizedText2, EntityManager.NODE_LOCALIZED_TEXT);

		final Map<String, Object> localizedTextRelationship1 = new HashMap<>();
		localizedTextRelationship1.put("type", "extract");
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, extractRecordID(localizedText1),
			EntityManager.NODE_CITATION, extractRecordID(citation1),
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
note1.put("reference_table", "person");
note1.put("reference_id", 1);
		Repository.upsert(note1, EntityManager.NODE_NOTE);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 2");
note2.put("reference_table", "citation");
note2.put("reference_id", 1);
		Repository.upsert(note2, EntityManager.NODE_NOTE);

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
			final SearchCitationPanel panel = create();
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
