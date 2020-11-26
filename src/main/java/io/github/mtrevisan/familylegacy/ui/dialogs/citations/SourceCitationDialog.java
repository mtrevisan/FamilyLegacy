/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.dialogs.citations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


//TODO
public class SourceCitationDialog extends JDialog{

	private static final long serialVersionUID = -602003630068140060L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_SOURCE_ID = 0;
	private static final int TABLE_INDEX_SOURCE_TYPE = 1;
	private static final int TABLE_INDEX_SOURCE_TITLE = 2;

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable sourcesTable = new JTable(new SourcesTableModel());
	private final JScrollPane sourcesScrollPane = new JScrollPane(sourcesTable);
	private final JButton addButton = new JButton("Add");
	private final JButton removeButton = new JButton("Remove");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<SourceCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private final Flef store;


	public SourceCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Source citations");

		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(SourceCitationDialog.this);
			}
		});

		sourcesTable.setAutoCreateRowSorter(true);
		sourcesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		sourcesTable.setFocusable(false);
		sourcesTable.setGridColor(GRID_COLOR);
		sourcesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sourcesTable.getTableHeader().setFont(sourcesTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(sourcesTable, TABLE_INDEX_SOURCE_ID, 0, ID_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(sourcesTable.getModel());
		final Comparator<String> idComparator = (value1, value2) -> {
			//NOTE: here it is assumed that all the IDs starts with a character followed by a number, and that years can begin with `~`
			final int v1 = Integer.parseInt(Character.isDigit(value1.charAt(0))? value1: value1.substring(1));
			final int v2 = Integer.parseInt(Character.isDigit(value2.charAt(0))? value2: value2.substring(1));
			return Integer.compare(v1, v2);
		};
		sorter.setComparator(TABLE_INDEX_SOURCE_ID, idComparator);
		sorter.setComparator(TABLE_INDEX_SOURCE_TYPE, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_SOURCE_TITLE, Comparator.naturalOrder());
		sourcesTable.setRowSorter(sorter);
		sourcesTable.getSelectionModel().addListSelectionListener(event -> removeButton.setEnabled(true));

		addButton.addActionListener(evt -> {
			//TODO
		});
		removeButton.setEnabled(false);
		removeButton.addActionListener(evt -> {
			//TODO
			final DefaultTableModel model = (DefaultTableModel)sourcesTable.getModel();
			model.removeRow(sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()));
			removeButton.setEnabled(false);
		});

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//TODO
//			if(listener != null){
//				final GedcomNode selectedFamily = getSelectedFamily();
//				listener.onNodeSelected(selectedFamily, SelectedNodeType.FAMILY, panelReference);
//			}

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(sourcesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 2,sizegroup button2");
		add(removeButton, "tag remove,sizegroup button2,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	public void loadData(final GedcomNode container){
		this.container = container;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> sources = store.traverseAsList(container, "SOURCE[]");
		final int size = sources.size();
		for(int i = 0; i < size; i ++)
			sources.set(i, store.getSource(sources.get(i).getXRef()));

		if(size > 0){
			final DefaultTableModel sourcesModel = (DefaultTableModel)sourcesTable.getModel();
			sourcesModel.setRowCount(size);

//		n SOURCE @<XREF:SOURCE>@    {1:1}	/* An xref ID of a source record. */
//			+1 PAGE <WHERE_WITHIN_SOURCE>    {0:1}	/* Specific location with in the information referenced. The data in this field should be in the form of a label and value pair (eg. 'Film: 1234567, Frame: 344, Line: 28'). */
//			+1 ROLE <ROLE_IN_EVENT>    {0:1}	/* Indicates what role this person or family played in the event that is being cited in this context. Known values are: CHILD, FATHER, HUSBAND, MOTHER, WIFE, SPOUSE, etc. */
//			+1 NOTE @<XREF:NOTE>@    {0:M}	/* An xref ID of a note record. */
//			+1 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}	/* A quantitative evaluation of the credibility of a piece of information, based upon its supporting evidence. Some systems use this feature to rank multiple conflicting opinions for display of most likely information first. It is not intended to eliminate the receiver's need to evaluate the evidence for themselves. 0 = unreliable/estimated data 1 = Questionable reliability of evidence 2 = Secondary evidence, data officially recorded sometime after event 3 = Direct and primary evidence used, or by dominance of the evidence. */

			for(int row = 0; row < size; row ++){
				final GedcomNode source = sources.get(row);

				sourcesModel.setValueAt(source.getID(), row, TABLE_INDEX_SOURCE_ID);
				sourcesModel.setValueAt(store.traverse(source, "TYPE").getValue(), row, TABLE_INDEX_SOURCE_TYPE);
				sourcesModel.setValueAt(store.traverse(source, "TITLE").getValue(), row, TABLE_INDEX_SOURCE_TITLE);
			}
		}
	}

	private void filterTableBy(final SourceCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_SOURCE_ID, TABLE_INDEX_SOURCE_TYPE,
			TABLE_INDEX_SOURCE_TITLE);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) sourcesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel) sourcesTable.getModel();
			sorter = new TableRowSorter<>(model);
			sourcesTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}

	private RowFilter<DefaultTableModel, Object> createTextFilter(final String text, final int... columnIndexes){
		if(StringUtils.isNotBlank(text)){
			try{
				//split input text around spaces and commas
				final String[] components = StringUtils.split(text, " ,");
				final Collection<RowFilter<DefaultTableModel, Object>> andFilters = new ArrayList<>(components.length);
				for(final String component : components)
					andFilters.add(RowFilter.regexFilter("(?i)(?:" + Pattern.quote(component) + ")", columnIndexes));
				return RowFilter.andFilter(andFilters);
			}
			catch(final PatternSyntaxException ignored){
				//current expression doesn't parse, ignore it
			}
		}
		return null;
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.3.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final SourceCitationDialog dialog = new SourceCitationDialog(store, new JFrame());
			dialog.loadData(container);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}


	private static class SourcesTableModel extends DefaultTableModel{

		private static final long serialVersionUID = -1111881848211175897L;


		SourcesTableModel(){
			super(new String[]{"ID", "Type", "Title"}, 0);
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return false;
		}
	}

}
