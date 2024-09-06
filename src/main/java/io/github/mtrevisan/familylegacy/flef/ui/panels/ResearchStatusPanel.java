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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCreationDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPriority;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordStatus;


public class ResearchStatusPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = 5892334911460066184L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;
	private static final int TABLE_INDEX_STATUS = 3;
	private static final int TABLE_INDEX_PRIORITY = 4;
	private static final int TABLE_INDEX_CREATION_DATE = 5;

	private static final int TABLE_PREFERRED_WIDTH_STATUS = 50;
	private static final int TABLE_PREFERRED_WIDTH_PRIORITY = 52;
	private static final int TABLE_PREFERRED_WIDTH_DATE = 120;

	public static final DateTimeFormatter HUMAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss", Locale.US)
		.withZone(ZoneId.systemDefault());

	private String filterReferenceTable;
	private int filterReferenceID;


	public static ResearchStatusPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new ResearchStatusPanel(store);
	}


	private ResearchStatusPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		super(store);


		initComponents();
	}


	private void initComponents(){
		//hide ID column
		final TableColumnModel columnModel = recordTable.getColumnModel();
		final TableColumn hiddenColumn = columnModel.getColumn(TABLE_INDEX_ID);
		columnModel.removeColumn(hiddenColumn);

		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_STATUS, 0, TABLE_PREFERRED_WIDTH_STATUS);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PRIORITY, 0, TABLE_PREFERRED_WIDTH_PRIORITY);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_CREATION_DATE, 0, TABLE_PREFERRED_WIDTH_DATE);
	}

	public final ResearchStatusPanel withLinkListener(final RecordListenerInterface linkListener){
		super.setLinkListener(linkListener);

		return this;
	}

	public ResearchStatusPanel withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		return this;
	}

	@Override
	public String getTableName(){
		return EntityManager.TABLE_NAME_RESEARCH_STATUS;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier", "Status", "Priority", "Creation date"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.RIGHT, SwingConstants.RIGHT,
			SwingConstants.RIGHT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> dateComparator = GUIHelper.getHumanDateComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, numericComparator, dateComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final Map<Integer, Map<String, Object>> recordResearchStatuses = getRecords(EntityManager.TABLE_NAME_RESEARCH_STATUS)
			.entrySet().stream()
			.filter(entry -> filterReferenceTable.equals(extractRecordReferenceTable(entry.getValue()))
				&& filterReferenceID == extractRecordReferenceID(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(recordResearchStatuses.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> recordResearchStatus : recordResearchStatuses.entrySet()){
			final Integer key = recordResearchStatus.getKey();
			final Map<String, Object> container = recordResearchStatus.getValue();

			final Integer researchStatusID = extractRecordID(container);
			final String identifier = extractRecordIdentifier(container);
			final String status = extractRecordStatus(container);
			final Integer priority = extractRecordPriority(container);
			final String recordCreationDate = extractRecordCreationDate(container);
			final String humanReadableDateTime = (recordCreationDate != null
				? HUMAN_DATE_FORMATTER.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(recordCreationDate))
				: null);
			final FilterString filter = FilterString.create()
				.add(researchStatusID)
				.add(identifier)
				.add(status)
				.add(priority)
				.add(humanReadableDateTime);
			final String filterData = filter.toString();

			model.setValueAt(researchStatusID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(status, row, TABLE_INDEX_STATUS);
			model.setValueAt(priority, row, TABLE_INDEX_PRIORITY);
			model.setValueAt(humanReadableDateTime, row, TABLE_INDEX_CREATION_DATE);

			tableData.add(new SearchAllRecord(key, EntityManager.TABLE_NAME_RESEARCH_STATUS, filterData, identifier));

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

		final TreeMap<Integer, Map<String, Object>> researchStatuses = new TreeMap<>();
		store.put("research_status", researchStatuses);
		final Map<String, Object> researchStatus1 = new HashMap<>();
		researchStatus1.put("id", 1);
		researchStatus1.put("reference_table", "person_name");
		researchStatus1.put("reference_id", 1);
		researchStatus1.put("identifier", "identifier 1");
		researchStatus1.put("description", "some description");
		researchStatus1.put("status", "open");
		researchStatus1.put("priority", 0);
		researchStatus1.put("creation_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
		researchStatuses.put((Integer)researchStatus1.get("id"), researchStatus1);
		final Map<String, Object> researchStatus2 = new HashMap<>();
		researchStatus2.put("id", 2);
		researchStatus2.put("reference_table", "person_name");
		researchStatus2.put("reference_id", 1);
		researchStatus2.put("identifier", "identifier 2");
		researchStatus2.put("description", "another description");
		researchStatus2.put("status", "active");
		researchStatus2.put("priority", 1);
		researchStatus2.put("creation_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		researchStatuses.put((Integer)researchStatus2.get("id"), researchStatus2);

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
			final ResearchStatusPanel panel = create(store)
				.withReference(EntityManager.TABLE_NAME_PERSON_NAME, 1)
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
