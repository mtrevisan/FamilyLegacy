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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPriority;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordStatus;


public class SearchResearchStatusPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -4361406306346697722L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;
	private static final int TABLE_INDEX_DESCRIPTION = 3;
	private static final int TABLE_INDEX_STATUS = 4;
	private static final int TABLE_INDEX_PRIORITY = 5;

	private static final int TABLE_PREFERRED_WIDTH_IDENTIFIER = 150;
	private static final int TABLE_PREFERRED_WIDTH_DESCRIPTION = 250;
	private static final int TABLE_PREFERRED_WIDTH_STATUS = 50;
	private static final int TABLE_PREFERRED_WIDTH_PRIORITY = 43;


	public static SearchResearchStatusPanel create(){
		return new SearchResearchStatusPanel();
	}


	private SearchResearchStatusPanel(){
		super();


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_IDENTIFIER, 0, TABLE_PREFERRED_WIDTH_IDENTIFIER);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_DESCRIPTION, 0, TABLE_PREFERRED_WIDTH_DESCRIPTION);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_STATUS, 0, TABLE_PREFERRED_WIDTH_STATUS);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PRIORITY, 0, TABLE_PREFERRED_WIDTH_PRIORITY);
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_RESEARCH_STATUS;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier", "Description", "Status", "Priority"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT,
			SwingConstants.RIGHT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, textComparator, numericComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_RESEARCH_STATUS);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){

			final Integer recordID = extractRecordID(record);
			final String identifier = extractRecordIdentifier(record);
			final String description = extractRecordDescription(record);
			final String status = extractRecordStatus(record);
			final Integer priority = extractRecordPriority(record);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(identifier)
				.add(description)
				.add(status)
				.add(priority);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(description, row, TABLE_INDEX_DESCRIPTION);
			model.setValueAt(status, row, TABLE_INDEX_STATUS);
			model.setValueAt((priority != null? String.valueOf(priority): null), row, TABLE_INDEX_PRIORITY);

			tableData.add(new SearchAllRecord(recordID, EntityManager.NODE_RESEARCH_STATUS, filterData, identifier));

			row ++;
		}
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("type", "gregorian");
		int calendar1ID = Repository.upsert(calendar1, EntityManager.NODE_CALENDAR);

		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int date1ID = Repository.upsert(historicDate1, EntityManager.NODE_HISTORIC_DATE);
		Repository.upsertRelationship(EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.NODE_CALENDAR, calendar1ID,
			EntityManager.RELATIONSHIP_EXPRESSED_IN, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> researchStatus1 = new HashMap<>();
		researchStatus1.put("identifier", "research 1");
		researchStatus1.put("description", "see people, do things");
		researchStatus1.put("status", "open");
		researchStatus1.put("priority", 2);
		int researchStatus1ID = Repository.upsert(researchStatus1, EntityManager.NODE_RESEARCH_STATUS);
		Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, researchStatus1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

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
			final SearchResearchStatusPanel panel = create();
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
