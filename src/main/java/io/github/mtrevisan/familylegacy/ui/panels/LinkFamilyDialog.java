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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class LinkFamilyDialog extends JDialog{

	private static final long serialVersionUID = -3246390161022821225L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final String NAMES_SEPARATOR = ", ";

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 40;
	private static final int SPOUSE_NAME_PREFERRED_WIDTH = 150;
	private static final int YEAR_PREFERRED_WIDTH = 40;
	private static final int MARRIAGE_PLACE_PREFERRED_WIDTH = 250;

	private static final int TABLE_INDEX_MARRIAGE_ID = 0;
	private static final int TABLE_INDEX_SPOUSE1 = 1;
	private static final int TABLE_INDEX_SPOUSE1_BIRTH_YEAR = 2;
	private static final int TABLE_INDEX_SPOUSE1_DEATH_YEAR = 3;
	private static final int TABLE_INDEX_SPOUSE1_ID = 4;
	private static final int TABLE_INDEX_SPOUSE2 = 5;
	private static final int TABLE_INDEX_SPOUSE2_BIRTH_YEAR = 6;
	private static final int TABLE_INDEX_SPOUSE2_DEATH_YEAR = 7;
	private static final int TABLE_INDEX_SPOUSE2_ID = 8;
	private static final int TABLE_INDEX_MARRIAGE_YEAR = 9;
	private static final int TABLE_INDEX_MARRIAGE_PLACE = 10;
	private static final int TABLE_INDEX_SPOUSE1_ADDITIONAL_NAMES = 11;
	private static final int TABLE_INDEX_SPOUSE2_ADDITIONAL_NAMES = 12;


	private final JLabel filterSpouseLabel = new JLabel("Filter:");
	private final JTextField filterSpouseField = new JTextField();
	private JTable familiesTable;
	private final JScrollPane familiesScrollPane = new JScrollPane();
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<LinkFamilyDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private final Flef store;
	private final SelectionListenerInterface listener;


	public LinkFamilyDialog(final Flef store, final SelectionListenerInterface listener, final Frame parent){
		super(parent, true);

		this.store = store;
		this.listener = listener;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Link family");

		familiesTable = new JTable(new FamiliesTableModel());
		familiesTable.getColumnModel().setColumnMargin(5);
		familiesTable.setAutoCreateRowSorter(true);
		familiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		familiesTable.setFocusable(false);
		familiesTable.setGridColor(GRID_COLOR);
		familiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		familiesTable.getTableHeader().setFont(familiesTable.getFont().deriveFont(Font.BOLD));
		final DefaultTableCellRenderer rightAlignedRenderer = new DefaultTableCellRenderer();
		rightAlignedRenderer.setHorizontalAlignment(JLabel.RIGHT);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_MARRIAGE_ID, 0, ID_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1, 0, SPOUSE_NAME_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1_BIRTH_YEAR, 0, YEAR_PREFERRED_WIDTH)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1_DEATH_YEAR, 0, YEAR_PREFERRED_WIDTH)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE1_ID, 0, ID_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2, 0, SPOUSE_NAME_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2_BIRTH_YEAR, 0, YEAR_PREFERRED_WIDTH)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2_DEATH_YEAR, 0, YEAR_PREFERRED_WIDTH)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_SPOUSE2_ID, 0, ID_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_MARRIAGE_YEAR, 0, YEAR_PREFERRED_WIDTH)
			.setCellRenderer(rightAlignedRenderer);
		TableHelper.setColumnWidth(familiesTable, TABLE_INDEX_MARRIAGE_PLACE, 0, MARRIAGE_PLACE_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(familiesTable.getModel());
		final Comparator<String> idDateComparator = (value1, value2) -> {
			//NOTE: here it is assumed that all the IDs starts with a character followed by a number, and that years can begin with `~`
			final int v1 = Integer.parseInt(Character.isDigit(value1.charAt(0))? value1: value1.substring(1));
			final int v2 = Integer.parseInt(Character.isDigit(value2.charAt(0))? value2: value2.substring(1));
			return Integer.compare(v1, v2);
		};
		//put approximated years after exact years
		final Comparator<String> dateWithApproximationComparator = idDateComparator.thenComparingInt(year -> year.charAt(0));
		sorter.setComparator(TABLE_INDEX_MARRIAGE_ID, idDateComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE1_ID, idDateComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE2_ID, idDateComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE1_BIRTH_YEAR, dateWithApproximationComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE1_DEATH_YEAR, dateWithApproximationComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE2_BIRTH_YEAR, dateWithApproximationComparator);
		sorter.setComparator(TABLE_INDEX_SPOUSE2_DEATH_YEAR, dateWithApproximationComparator);
		sorter.setComparator(TABLE_INDEX_MARRIAGE_YEAR, dateWithApproximationComparator);
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
		add(familiesScrollPane, "newline,width 100%,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void loadData(){
		final List<GedcomNode> families = store.getFamilies();
		okButton.setEnabled(!families.isEmpty());

		final DefaultTableModel familiesModel = (DefaultTableModel)familiesTable.getModel();

		final int size = families.size();
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
			final TableColumn additionalNames2Column = familiesTable.getColumn(familiesTable.getColumnName(TABLE_INDEX_SPOUSE2_ADDITIONAL_NAMES));
			columnModel.removeColumn(additionalNames2Column);
			final TableColumn additionalNames1Column = familiesTable.getColumn(familiesTable.getColumnName(TABLE_INDEX_SPOUSE1_ADDITIONAL_NAMES));
			columnModel.removeColumn(additionalNames1Column);
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
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_MARRIAGE_ID, TABLE_INDEX_SPOUSE1,
			TABLE_INDEX_SPOUSE1_ID, TABLE_INDEX_SPOUSE2, TABLE_INDEX_SPOUSE2_ID,
			TABLE_INDEX_SPOUSE1_ADDITIONAL_NAMES, TABLE_INDEX_SPOUSE2_ADDITIONAL_NAMES);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)familiesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)familiesTable.getModel();
			sorter = new TableRowSorter<>(model);
			familiesTable.setRowSorter(sorter);
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

		final SelectionListenerInterface listener = node -> System.out.println("onNodeSelected " + node.getID());

		EventQueue.invokeLater(() -> {
			final LinkFamilyDialog dialog = new LinkFamilyDialog(storeFlef, listener, new javax.swing.JFrame());

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

	private static class FamiliesTableModel extends DefaultTableModel{

		private static final long serialVersionUID = -2461556718124651678L;


		FamiliesTableModel(){
			super(new String[]{"ID", "Spouse 1", "", "", "Spouse 1 ID", "Spouse 2", "", "", "Spouse 2 ID", "Date", "Place",
				"spouse1 additional names", "spouse2 additional names"}, 0);
		}

		@Override
		public Class<?> getColumnClass(final int column){
			final Object value = getValueAt(0, column);
			return (value != null? value.getClass(): Object.class);
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return false;
		}
	}

}
