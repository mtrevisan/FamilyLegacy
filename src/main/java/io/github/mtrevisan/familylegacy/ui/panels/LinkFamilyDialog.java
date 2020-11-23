package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;


public class LinkFamilyDialog extends JDialog{

	private static final long serialVersionUID = -3246390161022821225L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final String NAMES_SEPARATOR = ", ";

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int TABLE_INDEX_MARRIAGE_ID = 0;
	private static final int TABLE_INDEX_SPOUSE1 = 1;
	private static final int TABLE_INDEX_SPOUSE1_ADDITIONAL_NAMES = 2;
	private static final int TABLE_INDEX_SPOUSE1_BIRTH_YEAR = 3;
	private static final int TABLE_INDEX_SPOUSE1_DEATH_YEAR = 4;
	private static final int TABLE_INDEX_SPOUSE1_ID = 5;
	private static final int TABLE_INDEX_SPOUSE2 = 6;
	private static final int TABLE_INDEX_SPOUSE2_ADDITIONAL_NAMES = 7;
	private static final int TABLE_INDEX_SPOUSE2_BIRTH_YEAR = 8;
	private static final int TABLE_INDEX_SPOUSE2_DEATH_YEAR = 9;
	private static final int TABLE_INDEX_SPOUSE2_ID = 10;
	private static final int TABLE_INDEX_MARRIAGE_YEAR = 11;
	private static final int TABLE_INDEX_MARRIAGE_PLACE = 12;


	private final JLabel filterSpouseLabel = new JLabel("Filter:");
	private final JTextField filterSpouseField = new JTextField();
	private JTable familiesTable;
	private final JScrollPane familiesScrollPane = new JScrollPane();
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<LinkFamilyDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);
	private RowFilter<DefaultTableModel, Object> marriageIDFilter;
	private RowFilter<DefaultTableModel, Object> spouse1Filter;
	private RowFilter<DefaultTableModel, Object> spouse1AdditionalNamesFilter;
	private RowFilter<DefaultTableModel, Object> spouse1IDFilter;
	private RowFilter<DefaultTableModel, Object> spouse2Filter;
	private RowFilter<DefaultTableModel, Object> spouse2AdditionalNamesFilter;
	private RowFilter<DefaultTableModel, Object> spouse2IDFilter;

	private Flef store;
	private SelectionListenerInterface listener;


	public LinkFamilyDialog(final Flef store, final SelectionListenerInterface listener, final Frame parent){
		super(parent, true);

		this.store = store;
		this.listener = listener;

		initComponents();

		loadData();
	}

	private void initComponents(){
		familiesTable = new JTable(new DefaultTableModel(new String[]{"ID",
				"Spouse 1", "", "", "", "Spouse 1 ID",
				"Spouse 2", "", "", "", "Spouse 2 ID",
				"Date", "Place"}, 0){
			@Override
			public Class<?> getColumnClass(final int column){
				final Object value = getValueAt(0, column);
				return (value != null? value.getClass(): Object.class);
			}

			@Override
			public boolean isCellEditable(final int row, final int column){
				return false;
			}
		});
		familiesTable.setAutoCreateRowSorter(true);
		familiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		familiesTable.setFocusable(false);
		familiesTable.setGridColor(GRID_COLOR);
		familiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		familiesTable.getTableHeader().setFont(familiesTable.getFont().deriveFont(Font.BOLD));
		final DefaultTableCellRenderer rightAlignedRenderer = new DefaultTableCellRenderer();
		rightAlignedRenderer.setHorizontalAlignment(JLabel.RIGHT);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_MARRIAGE_ID, 0, 40);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1, 0, 150);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1_BIRTH_YEAR, 0, 35)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1_DEATH_YEAR, 0, 35)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1_ID, 0, 40);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2, 0, 150);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2_BIRTH_YEAR, 0, 35)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2_DEATH_YEAR, 0, 35)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2_ID, 0, 40);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_MARRIAGE_YEAR, 0, 35)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_MARRIAGE_PLACE, 0, 250);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(familiesTable.getModel());
		final Comparator<String> idDateComparator = (value1, value2) -> {
			if(!Character.isDigit(value1.charAt(0)))
				value1 = value1.substring(1);
			if(!Character.isDigit(value2.charAt(0)))
				value2 = value2.substring(1);
			return Integer.compare(Integer.parseInt(value1), Integer.parseInt(value2));
		};
		sorter.setComparator(TABLE_INDEX_MARRIAGE_ID, idDateComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE1_ID, idDateComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE2_ID, idDateComparator);
		//put approximated years after exact years
		sorter.setComparator(TABLE_INDEX_SPOUSE1_BIRTH_YEAR, idDateComparator.thenComparingInt(year -> year.charAt(0)));
		sorter.setComparator(TABLE_INDEX_SPOUSE1_DEATH_YEAR, idDateComparator.thenComparingInt(year -> year.charAt(0)));
		sorter.setComparator(TABLE_INDEX_SPOUSE2_BIRTH_YEAR, idDateComparator.thenComparingInt(year -> year.charAt(0)));
		sorter.setComparator(TABLE_INDEX_SPOUSE2_DEATH_YEAR, idDateComparator.thenComparingInt(year -> year.charAt(0)));
		sorter.setComparator(TABLE_INDEX_MARRIAGE_YEAR, idDateComparator.thenComparingInt(year -> year.charAt(0)));
		familiesTable.setRowSorter(sorter);
		final ListSelectionModel selectionModel = familiesTable.getSelectionModel();
		if(listener != null)
			selectionModel.addListSelectionListener(e -> {
				if(!e.getValueIsAdjusting() && familiesTable.getRowSelectionAllowed())
					listener.onNodeSelected(getSelectedFamily());
			});
		familiesScrollPane.setViewportView(familiesTable);

		filterSpouseLabel.setLabelFor(filterSpouseField);
		filterSpouseField.setEnabled(false);
		filterSpouseField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(LinkFamilyDialog.this);
			}
		});

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			if(listener != null){
				final GedcomNode selectedFamily = getSelectedFamily();
				listener.onNodeSelected(selectedFamily);
			}

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterSpouseLabel, "align label,split 2");
		add(filterSpouseField, "grow");
		add(familiesScrollPane, "newline,wrap paragraph");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void loadData(){
		final List<GedcomNode> families = store.getFamilies();
		okButton.setEnabled(!families.isEmpty());

		final DefaultTableModel familiesModel = (DefaultTableModel)familiesTable.getModel();

		int size = families.size();
		if(size > 0){
			familiesModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode family = families.get(row);

				familiesModel.setValueAt(family.getID(), row, TABLE_INDEX_MARRIAGE_ID);
				loadSpouseData(row, familiesModel, family, "SPOUSE1", TABLE_INDEX_SPOUSE1, TABLE_INDEX_SPOUSE1_ADDITIONAL_NAMES,
					TABLE_INDEX_SPOUSE1_BIRTH_YEAR, TABLE_INDEX_SPOUSE1_DEATH_YEAR, TABLE_INDEX_SPOUSE1_ID);
				loadSpouseData(row, familiesModel, family, "SPOUSE2", TABLE_INDEX_SPOUSE2, TABLE_INDEX_SPOUSE2_ADDITIONAL_NAMES,
					TABLE_INDEX_SPOUSE2_BIRTH_YEAR, TABLE_INDEX_SPOUSE2_DEATH_YEAR, TABLE_INDEX_SPOUSE2_ID);
				familiesModel.setValueAt(FamilyPanel.extractEarliestMarriageYear(family, store), row, TABLE_INDEX_MARRIAGE_YEAR);
				familiesModel.setValueAt(FamilyPanel.extractEarliestMarriagePlace(family, store), row, TABLE_INDEX_MARRIAGE_PLACE);
			}

			final TableColumnModel columnModel = familiesTable.getColumnModel();
			columnModel.removeColumn(familiesTable.getColumn(familiesTable.getColumnName(TABLE_INDEX_SPOUSE1_ADDITIONAL_NAMES)));
			columnModel.removeColumn(familiesTable.getColumn(familiesTable.getColumnName(TABLE_INDEX_SPOUSE2_ADDITIONAL_NAMES)));
			filterSpouseField.setEnabled(true);
		}
	}

	private void loadSpouseData(final int row, final DefaultTableModel familiesModel, final GedcomNode family, final String spouseTag,
			final int tableIndexSpouse, final int tableIndexSpouseAdditionalNames, final int tableIndexSpouseBirthYear,
			final int tableIndexSpouseDeathYear, final int tableIndexSpouseId){
		final GedcomNode spouse = store.getIndividual(store.traverse(family, spouseTag).getXRef());
		final List<String[]> spouseName = IndividualPanel.extractCompleteName(spouse, store);
		if(!spouseName.isEmpty()){
			final String[] firstPersonalName = spouseName.get(0);
			familiesModel.setValueAt(firstPersonalName[0] + NAMES_SEPARATOR + firstPersonalName[1], row, tableIndexSpouse);
			for(int i = 1; i < spouseName.size(); i ++){
				final String[] nthPersonalName = spouseName.get(i);
				familiesModel.setValueAt(nthPersonalName[0] + NAMES_SEPARATOR + nthPersonalName[1], row, tableIndexSpouseAdditionalNames);
			}
		}
		familiesModel.setValueAt(IndividualPanel.extractBirthYear(spouse, store), row, tableIndexSpouseBirthYear);
		familiesModel.setValueAt(IndividualPanel.extractDeathYear(spouse, store), row, tableIndexSpouseDeathYear);
		if(spouse != null)
			familiesModel.setValueAt(spouse.getID(), row, tableIndexSpouseId);
	}

	public void reset(){
		((DefaultTableModel)familiesTable.getModel()).setRowCount(0);
		filterSpouseField.setEnabled(false);
	}

	private void filterTableBy(final LinkFamilyDialog panel){
		final String text = filterSpouseField.getText();
		marriageIDFilter = createTextFilter(text, TABLE_INDEX_MARRIAGE_ID);
		spouse1Filter = createTextFilter(text, TABLE_INDEX_SPOUSE1);
		spouse1AdditionalNamesFilter = createTextFilter(text, TABLE_INDEX_SPOUSE1_ADDITIONAL_NAMES);
		spouse1IDFilter = createTextFilter(text, TABLE_INDEX_SPOUSE1_ID);
		spouse2Filter = createTextFilter(text, TABLE_INDEX_SPOUSE2);
		spouse2AdditionalNamesFilter = createTextFilter(text, TABLE_INDEX_SPOUSE2_ADDITIONAL_NAMES);
		spouse2IDFilter = createTextFilter(text, TABLE_INDEX_SPOUSE2_ID);

		final RowFilter<DefaultTableModel, Object> filter = RowFilter.andFilter(Arrays.asList(marriageIDFilter, spouse1Filter,
			spouse1AdditionalNamesFilter, spouse1IDFilter, spouse2Filter, spouse2AdditionalNamesFilter, spouse2IDFilter));

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)familiesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)familiesTable.getModel();
			sorter = new TableRowSorter<>(model);
			familiesTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}

	private RowFilter<DefaultTableModel, Object> createTextFilter(final String text, final int columnIndex){
		RowFilter<DefaultTableModel, Object> rf = null;
		if(StringUtils.isNotBlank(text)){
			try{
				rf = RowFilter.regexFilter("(?i)" + text, columnIndex);
			}
			catch(final PatternSyntaxException ignored){
				//if current expression doesn't parse, don't update.
			}
		}
		return rf;
	}

	public GedcomNode getSelectedFamily(){
		final int viewRow = familiesTable.getSelectedRow();
		GedcomNode family = null;
		if(viewRow >= 0){
			final int selectedRow = familiesTable.convertRowIndexToModel(viewRow);
			family = store.getFamilies().get(selectedRow);
		}
		return family;
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

		final SelectionListenerInterface listener = new SelectionListenerInterface(){
			@Override
			public void onNodeSelected(final GedcomNode node){
				System.out.println("onNodeSelected " + node.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			final LinkFamilyDialog dialog = new LinkFamilyDialog(storeFlef, listener, new javax.swing.JFrame());

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(700, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);

			final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			final Runnable task = () -> dialog.loadData();
			scheduler.schedule(task, 3, TimeUnit.SECONDS);
		});
	}

}
