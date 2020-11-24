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
package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.ui.utilities.IndividualTableCellRenderer;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class LinkIndividualDialog extends JDialog{

	private static final long serialVersionUID = -609243281331916645L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final String NAMES_SEPARATOR = ", ";

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 43;
	private static final int SEX_PREFERRED_WIDTH = 20;
	private static final int NAME_PREFERRED_WIDTH = 150;
	private static final int YEAR_PREFERRED_WIDTH = 43;
	private static final int PLACE_PREFERRED_WIDTH = 250;

	private static final int TABLE_INDEX_INDIVIDUAL_ID = 0;
	public static final int TABLE_INDEX_SEX = 1;
	public static final int TABLE_INDEX_NAME = 2;
	private static final int TABLE_INDEX_BIRTH_YEAR = 3;
	private static final int TABLE_INDEX_BIRTH_PLACE = 4;
	private static final int TABLE_INDEX_DEATH_YEAR = 5;
	private static final int TABLE_INDEX_DEATH_PLACE = 6;
	public static final int TABLE_INDEX_ADDITIONAL_NAMES = 7;


	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private JTable individualsTable;
	private final JScrollPane individualsScrollPane = new JScrollPane();
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<LinkIndividualDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private final Flef store;
	private final SelectionListenerInterface listener;


	public LinkIndividualDialog(final Flef store, final SelectionListenerInterface listener, final Frame parent){
		super(parent, true);

		this.store = store;
		this.listener = listener;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Link individual");

		individualsTable = new JTable(new IndividualsTableModel());
		individualsTable.setAutoCreateRowSorter(true);
		individualsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		individualsTable.setFocusable(false);
		individualsTable.setGridColor(GRID_COLOR);
		individualsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		individualsTable.getTableHeader().setFont(individualsTable.getFont().deriveFont(Font.BOLD));
		final TableCellRenderer nameRenderer = new IndividualTableCellRenderer();
		individualsTable.setDefaultRenderer(String.class, nameRenderer);
		final IndividualTableCellRenderer rightAlignedRenderer = new IndividualTableCellRenderer();
		rightAlignedRenderer.setHorizontalAlignment(JLabel.RIGHT);
		TableHelper.setColumnWidth(individualsTable, TABLE_INDEX_INDIVIDUAL_ID, 0, ID_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(individualsTable, TABLE_INDEX_SEX, 0, SEX_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(individualsTable, TABLE_INDEX_NAME, 0, NAME_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(individualsTable, TABLE_INDEX_BIRTH_YEAR, 0, YEAR_PREFERRED_WIDTH)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(individualsTable, TABLE_INDEX_BIRTH_PLACE, 0, PLACE_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(individualsTable, TABLE_INDEX_DEATH_YEAR, 0, YEAR_PREFERRED_WIDTH)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(individualsTable, TABLE_INDEX_DEATH_PLACE, 0, PLACE_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(individualsTable.getModel());
		final Comparator<String> idDateComparator = (value1, value2) -> {
			//NOTE: here it is assumed that all the IDs starts with a character followed by a number, and that years can begin with `~`
			final int v1 = Integer.parseInt(Character.isDigit(value1.charAt(0))? value1: value1.substring(1));
			final int v2 = Integer.parseInt(Character.isDigit(value2.charAt(0))? value2: value2.substring(1));
			return Integer.compare(v1, v2);
		};
		//put approximated years after exact years
		final Comparator<String> dateWithApproximationComparator = idDateComparator.thenComparingInt(year -> year.charAt(0));
		sorter.setComparator(TABLE_INDEX_INDIVIDUAL_ID, idDateComparator);
		sorter.setComparator(TABLE_INDEX_BIRTH_YEAR, dateWithApproximationComparator);
		sorter.setComparator(TABLE_INDEX_DEATH_YEAR, dateWithApproximationComparator);
		individualsTable.setRowSorter(sorter);
		individualsScrollPane.setViewportView(individualsTable);

		filterLabel.setLabelFor(filterField);
		filterField.setEnabled(false);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(LinkIndividualDialog.this);
			}
		});

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			if(listener != null){
				final GedcomNode selectedIndividual = getSelectedIndividual();
				listener.onNodeSelected(selectedIndividual);
			}

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterLabel, "align label,split 2");
		add(filterField, "grow");
		add(individualsScrollPane, "newline,width 100%,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void loadData(){
		final List<GedcomNode> individuals = store.getIndividuals();
		okButton.setEnabled(!individuals.isEmpty());

		final DefaultTableModel individualsModel = (DefaultTableModel)individualsTable.getModel();

		final int size = individuals.size();
		if(size > 0){
			individualsModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode individual = individuals.get(row);

				individualsModel.setValueAt(individual.getID(), row, TABLE_INDEX_INDIVIDUAL_ID);
				individualsModel.setValueAt(IndividualPanel.extractSex(individual, store).getCode(), row, TABLE_INDEX_SEX);
				final List<String[]> name = IndividualPanel.extractCompleteName(individual, store);
				if(!name.isEmpty()){
					final String[] firstPersonalName = name.get(0);
					individualsModel.setValueAt(firstPersonalName[0] + NAMES_SEPARATOR + firstPersonalName[1], row, TABLE_INDEX_NAME);
					final StringJoiner sj = new StringJoiner("<br>");
					for(int i = 1; i < name.size(); i ++){
						final String[] nthPersonalName = name.get(i);
						sj.add(nthPersonalName[0] + NAMES_SEPARATOR + nthPersonalName[1]);
					}
					if(sj.length() > 0)
						individualsModel.setValueAt("<html>" + sj + "</html>", row, TABLE_INDEX_ADDITIONAL_NAMES);
				}
				individualsModel.setValueAt(individual.getID(), row, TABLE_INDEX_INDIVIDUAL_ID);
				individualsModel.setValueAt(IndividualPanel.extractBirthYear(individual, store), row, TABLE_INDEX_BIRTH_YEAR);
				individualsModel.setValueAt(IndividualPanel.extractBirthPlace(individual, store), row, TABLE_INDEX_BIRTH_PLACE);
				individualsModel.setValueAt(IndividualPanel.extractDeathYear(individual, store), row, TABLE_INDEX_DEATH_YEAR);
				individualsModel.setValueAt(IndividualPanel.extractDeathPlace(individual, store), row, TABLE_INDEX_DEATH_PLACE);
			}

			final TableColumnModel columnModel = individualsTable.getColumnModel();
			final TableColumn column = individualsTable.getColumn(individualsTable.getColumnName(TABLE_INDEX_ADDITIONAL_NAMES));
			columnModel.removeColumn(column);

			filterField.setEnabled(true);
		}
	}

	public void reset(){
		((DefaultTableModel) individualsTable.getModel()).setRowCount(0);
		filterField.setEnabled(false);
	}

	private void filterTableBy(final LinkIndividualDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_INDIVIDUAL_ID, TABLE_INDEX_NAME,
			TABLE_INDEX_ADDITIONAL_NAMES);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)individualsTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)individualsTable.getModel();
			sorter = new TableRowSorter<>(model);
			individualsTable.setRowSorter(sorter);
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

	public GedcomNode getSelectedIndividual(){
		final int viewRow = individualsTable.getSelectedRow();
		GedcomNode individual = null;
		if(viewRow >= 0){
			final int selectedRow = individualsTable.convertRowIndexToModel(viewRow);
			individual = store.getIndividuals().get(selectedRow);
		}
		return individual;
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Store storeGedcom = new Gedcom();
		final Flef storeFlef = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
			.transform();

		final SelectionListenerInterface listener = node -> System.out.println("onNodeSelected " + node.getID());

		EventQueue.invokeLater(() -> {
			final LinkIndividualDialog dialog = new LinkIndividualDialog(storeFlef, listener, new JFrame());

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(700, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);

			final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.schedule(dialog::loadData, 3, TimeUnit.SECONDS);
		});
	}


	private static class IndividualsTableModel extends DefaultTableModel{

		private static final long serialVersionUID = 2228949520472700949L;


		IndividualsTableModel(){
			super(new String[]{"ID", "Sex", "Name", "Birth year", "Birth place", "Death year", "Death place", "additional names"}, 0);
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
