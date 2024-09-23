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

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTitle;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;


public class SearchMediaPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = 8889784802938785304L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;
	private static final int TABLE_INDEX_TITLE = 3;
	private static final int TABLE_INDEX_TYPE = 4;

	private static final int TABLE_PREFERRED_WIDTH_IDENTIFIER = 250;
	private static final int TABLE_PREFERRED_WIDTH_TITLE = 150;
	private static final int TABLE_PREFERRED_WIDTH_TYPE = 180;


	public static SearchMediaPanel create(){
		return new SearchMediaPanel();
	}


	private SearchMediaPanel(){
		super();


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_IDENTIFIER, 0, TABLE_PREFERRED_WIDTH_IDENTIFIER);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_TITLE, 0, TABLE_PREFERRED_WIDTH_TITLE);
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_TYPE, 0, TABLE_PREFERRED_WIDTH_TYPE);
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_MEDIA;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier", "Title", "Type"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, textComparator};
	}


	@Override
	public void loadData(){
		tableData.clear();


		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_MEDIA);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(int i = 0, length = records.size(); i < length; i ++){
			final Map<String, Object> record = records.get(i);

			final Integer recordID = extractRecordID(record);
			final String identifier = extractRecordIdentifier(record);
			final String title = extractRecordTitle(record);
			final String type = extractRecordType(record);
			final FilterString filter = FilterString.create().add(recordID).add(identifier).add(title).add(type);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(title, row, TABLE_INDEX_TITLE);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);

			tableData.add(new SearchAllRecord(recordID, EntityManager.NODE_MEDIA, filterData, title));

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
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		Repository.save(EntityManager.NODE_MEDIA, media1);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("id", 2);
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		Repository.save(EntityManager.NODE_MEDIA, media2);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("id", 3);
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		Repository.save(EntityManager.NODE_MEDIA, media3);

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
			final SearchMediaPanel panel = create();
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
