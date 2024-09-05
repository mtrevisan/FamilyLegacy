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
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.CommonSearchPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.RecordListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.SearchAllRecord;

import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordNote;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordUpdateDate;


public class HistoryPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -2548198057540800760L;

	private static final int TABLE_INDEX_DATE = 2;
	private static final int TABLE_INDEX_NOTE = 3;

	private static final int TABLE_PREFERRED_WIDTH_DATE = 120;

	private static final String TABLE_NAME_MODIFICATION = "modification";
	private static final String TABLE_NAME_NOTE = "note";

	public static final DateTimeFormatter HUMAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss", Locale.US)
		.withZone(ZoneId.systemDefault());

	private String filterReferenceTable;
	private int filterReferenceID;


	public static HistoryPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new HistoryPanel(store);
	}


	private HistoryPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		super(store);


		initComponents();
	}


	private void initComponents(){
		//hide ID column
		final TableColumnModel columnModel = recordTable.getColumnModel();
		final TableColumn hiddenColumn = columnModel.getColumn(TABLE_INDEX_ID);
		columnModel.removeColumn(hiddenColumn);

		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_DATE, 0, TABLE_PREFERRED_WIDTH_DATE);
	}

	public final HistoryPanel withLinkListener(final RecordListenerInterface linkListener){
		super.setLinkListener(linkListener);

		return this;
	}

	public HistoryPanel withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		return this;
	}

	@Override
	public String getTableName(){
		return TABLE_NAME_MODIFICATION;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Date", "Modification note"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> dateComparator = GUIHelper.getHumanDateComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, dateComparator, textComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final Map<Integer, Map<String, Object>> recordModifications = getRecords(TABLE_NAME_MODIFICATION)
			.entrySet().stream()
			.filter(entry -> filterReferenceTable.equals(extractRecordReferenceTable(entry.getValue()))
				&& filterReferenceID == extractRecordReferenceID(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		final Map<Integer, Map<String, Object>> recordsNotes = getRecords(TABLE_NAME_NOTE)
			.entrySet().stream()
			.filter(entry -> TABLE_NAME_MODIFICATION.equals(extractRecordReferenceTable(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(recordModifications.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> recordModification : recordModifications.entrySet()){
			final Integer key = recordModification.getKey();
			final Map<String, Object> containerModification = recordModification.getValue();
			final Map<String, Object> containerNote = recordsNotes.values().stream()
				.filter(record -> Objects.equals(key, extractRecordReferenceID(record)))
				.findFirst()
				.orElse(Collections.emptyMap());

			final Integer noteID = extractRecordID(containerNote);
			final String note = extractRecordNote(containerNote);
			final String recordUpdateDate = extractRecordUpdateDate(containerModification);
			final String humanReadableDateTime = (recordUpdateDate != null
				? HUMAN_DATE_FORMATTER.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(recordUpdateDate))
				: null);
			final FilterString filter = FilterString.create()
				.add(noteID)
				.add(humanReadableDateTime)
				.add(note);
			final String filterData = filter.toString();

			model.setValueAt(noteID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(humanReadableDateTime, row, TABLE_INDEX_DATE);
			model.setValueAt(note, row, TABLE_INDEX_NOTE);

			tableData.add(new SearchAllRecord(key, TABLE_NAME_MODIFICATION, filterData, note));

			row ++;
		}
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> modifications = new TreeMap<>();
		store.put("modification", modifications);
		final Map<String, Object> modification1 = new HashMap<>();
		modification1.put("id", 1);
		modification1.put("reference_table", "person_name");
		modification1.put("reference_id", 1);
		modification1.put("update_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
		modifications.put((Integer)modification1.get("id"), modification1);
		final Map<String, Object> modification2 = new HashMap<>();
		modification2.put("id", 2);
		modification2.put("reference_table", "person_name");
		modification2.put("reference_id", 1);
		modification2.put("update_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		modifications.put((Integer)modification2.get("id"), modification2);

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put("note", notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "something to say");
		note1.put("reference_table", "modification");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "something more to say");
		note2.put("reference_table", "modification");
		note2.put("reference_id", 2);
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
			final HistoryPanel panel = create(store)
				.withReference("person_name", 1)
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
