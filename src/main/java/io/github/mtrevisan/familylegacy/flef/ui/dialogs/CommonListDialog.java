/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.Debouncer;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ValidDataListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.panels.HistoryPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.ResearchStatusPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.searches.RecordListenerInterface;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;


public abstract class CommonListDialog extends CommonRecordDialog implements ValidDataListenerInterface{

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonListDialog.class);

	private static final String ACTION_MAP_KEY_INSERT = "insert";
	private static final String ACTION_MAP_KEY_DELETE = "delete";

	/** [ms] */
	protected static final int DEBOUNCE_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);
	private static final int TABLE_PREFERRED_WIDTH_ID = 25;

	protected static final int TABLE_INDEX_ID = 0;
	protected static final int TABLE_INDEX_FILTER = 1;
	private static final int TABLE_ROWS_SHOWN = 5;

	private static final String RECORD_PANEL_NAME_HISTORY = "history";
	private static final String RECORD_PANEL_NAME_RESEARCH_STATUS = "research status";


	//store components:
	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	protected final JTable recordTable = new JTable(new DefaultTableModel(getTableColumnNames(), 0){
		@Serial
		private static final long serialVersionUID = 6564164142990308313L;

		@Override
		public Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return false;
		}
	});
	private final JScrollPane recordTableScrollPane = new JScrollPane(recordTable);
	private final JButton newRecordButton = new JButton("New");
	private final JButton unselectRecordButton = new JButton("Unselect");
	protected final JButton deleteRecordButton = new JButton("Delete");
	//collection components:
	private final JButton addButton = new JButton("Add >>");
	private final JButton removeButton = new JButton("<< Remove");
	protected final JTable collectionTable = new JTable(new DefaultTableModel(getTableColumnNames(), 0){
		@Serial
		private static final long serialVersionUID = -7128683751768432136L;

		@Override
		public Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return false;
		}
	});
	private final JScrollPane collectionTableScrollPane = new JScrollPane(collectionTable);
	//record components:
	protected final JTabbedPane recordTabbedPane = new JTabbedPane();

	private final Debouncer<CommonListDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCE_TIME);

	private int previousIndex = -1;

	protected volatile boolean selectRecordOnly;
	private final Set<Component> viewOnlyComponents = new HashSet<>(List.of(recordTabbedPane));
	private final Map<Integer, Component> components = new HashMap<>(0);
	protected volatile boolean hideUnselectButton;
	protected volatile boolean showRecordOnly;
	protected volatile boolean showRecordHistory;
	protected volatile boolean showRecordResearchStatus;


	private HistoryPanel historyPanel;
	private ResearchStatusPanel researchStatusPanel;


	protected CommonListDialog(final Frame parent){
		super(parent);

		showRecordHistory = true;
		showRecordResearchStatus = true;
	}


	@Override
	protected void initComponents(){
		initStoreComponents();

		final CommonListDialog dialog = this;
		final RecordListenerInterface modificationLinkListener = new RecordListenerInterface(){
			@Override
			public void onRecordSelect(final String table, final Integer id){
				EventBusService.publish(EditEvent.create(EditEvent.EditType.MODIFICATION_HISTORY_SHOW, dialog,
					Map.of("id", extractRecordID(selectedRecord), "noteID", id)));
			}

			@Override
			public void onRecordEdit(final String table, final Integer id){
				EventBusService.publish(EditEvent.create(EditEvent.EditType.MODIFICATION_HISTORY_EDIT, dialog,
					Map.of("id", extractRecordID(selectedRecord), "noteID", id)));
			}
		};
		final RecordListenerInterface researchStatusLinkListener = new RecordListenerInterface(){
			@Override
			public void onRecordSelect(final String table, final Integer id){
			EventBusService.publish(EditEvent.create(EditEvent.EditType.RESEARCH_STATUS_SHOW, dialog,
					Map.of("id", extractRecordID(selectedRecord), "researchStatusID", id)));
			}

			@Override
			public void onRecordEdit(final String table, final Integer id){
				EventBusService.publish(EditEvent.create(EditEvent.EditType.RESEARCH_STATUS_EDIT, dialog,
					Map.of("id", extractRecordID(selectedRecord), "researchStatusID", id)));
			}
		};
		historyPanel = HistoryPanel.create()
			.withLinkListener(modificationLinkListener);
		researchStatusPanel = ResearchStatusPanel.create()
			.withLinkListener(researchStatusLinkListener);

		initRecordComponents();

		addValidDataListenerToMandatoryFields(this);
	}

	protected void initStoreComponents(){
		filterLabel.setLabelFor(filterField);
		GUIHelper.addUndoCapability(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(CommonListDialog.this);
			}
		});

		recordTable.setFocusable(true);
		recordTable.setGridColor(GRID_COLOR);
		recordTable.setDragEnabled(true);
		recordTable.setDropMode(DropMode.INSERT_ROWS);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_ID, TABLE_PREFERRED_WIDTH_ID);

		//define selection
		recordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recordTable.getSelectionModel()
			.addListSelectionListener(evt -> {
				if(!ignoreEvents && !evt.getValueIsAdjusting() && recordTable.getSelectedRow() >= 0)
					selectAction();
			});

		//add sorter
		recordTable.setAutoCreateRowSorter(true);
		final Comparator<?>[] comparators = getTableColumnComparators();
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(getRecordTableModel());
		final int columnCount = recordTable.getColumnCount();
		for(int columnIndex = 0; columnIndex < columnCount; columnIndex ++){
			final Comparator<?> comparator = comparators[columnIndex];

			if(comparator != null)
				sorter.setComparator(columnIndex, comparator);
		}
		recordTable.setRowSorter(sorter);

		final TableColumnModel recordColumnModel = recordTable.getColumnModel();
		final Border cellBorder = new EmptyBorder(new Insets(2, 5, 2, 5));
		final int[] alignments = getTableColumnAlignments();
		final JTableHeader tableHeader = recordTable.getTableHeader();
		final Font tableFont = recordTable.getFont();
		final Font headerFont = tableFont.deriveFont(Font.BOLD);
		tableHeader.setDefaultRenderer(new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column){
				final Component headerCell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				String cellText = null;
				if(value != null){
					cellText = value.toString();
					final FontMetrics fm = headerCell.getFontMetrics(tableFont);
					final int textWidth = fm.stringWidth(cellText);
					final Insets insets = ((Container)headerCell).getInsets();
					final int cellWidth = recordColumnModel.getColumn(column).getWidth()
						- insets.left - insets.right;

					if(textWidth <= cellWidth)
						cellText = null;
				}
				setToolTipText(cellText);

				setBorder(cellBorder);

				final int alignment = alignments[table.convertColumnIndexToModel(column)];
				setHorizontalAlignment(alignment);
				setFont(headerFont);

				return headerCell;
			}
		});

		//add tooltip
		for(int columnIndex = 0; columnIndex < columnCount; columnIndex ++){
			final TableColumn column = recordColumnModel.getColumn(columnIndex);
			final int alignment = alignments[columnIndex];

			column.setCellRenderer(new DefaultTableCellRenderer(){
				@Override
				public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
						final boolean hasFocus, final int row, final int column){
					final Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					String cellText = null;
					if(value != null){
						cellText = value.toString();
						final FontMetrics fm = cell.getFontMetrics(tableFont);
						final int textWidth = fm.stringWidth(cellText);
						final Insets insets = ((Container)cell).getInsets();
						final int cellWidth = recordColumnModel.getColumn(column).getWidth()
							- insets.left - insets.right;

						if(textWidth <= cellWidth)
							cellText = null;
					}
					setToolTipText(cellText);

					setBorder(cellBorder);

					setHorizontalAlignment(alignment);

					return cell;
				}
			});
		}

		//hide filter column
		final TableColumn hiddenColumn = recordColumnModel.getColumn(TABLE_INDEX_FILTER);
		recordColumnModel.removeColumn(hiddenColumn);

		//enable map key insert also if the scroll pane is select (delete will be skipped if no line has been selected)
		recordTableScrollPane.getViewport().addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				final Point point = evt.getPoint();
				final int rowAtPoint = recordTable.rowAtPoint(point);
				if(rowAtPoint == -1)
					recordTable.requestFocus();
			}
		});
		final InputMap recordTableInputMap = recordTable.getInputMap(JComponent.WHEN_FOCUSED);
		recordTableInputMap.put(GUIHelper.INSERT_STROKE, ACTION_MAP_KEY_INSERT);
		recordTableInputMap.put(GUIHelper.DELETE_STROKE, ACTION_MAP_KEY_DELETE);
		final ActionMap recordTableActionMap = recordTable.getActionMap();
		recordTableActionMap.put(ACTION_MAP_KEY_INSERT, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -6235301416538799926L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				newAction();
			}
		});
		recordTableActionMap.put(ACTION_MAP_KEY_DELETE, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -898408312774616906L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				deleteAction();
			}
		});
		final Dimension viewSize = new Dimension();
		viewSize.width = recordColumnModel.getTotalColumnWidth();
		viewSize.height = TABLE_ROWS_SHOWN * recordTable.getRowHeight();
		recordTable.setPreferredScrollableViewportSize(viewSize);

		newRecordButton.addActionListener(evt -> newAction());
		newRecordButton.setVisible(!selectRecordOnly);
		unselectRecordButton.setEnabled(false);
		unselectRecordButton.addActionListener(evt -> unselectAction());
		unselectRecordButton.setVisible(!hideUnselectButton);
		deleteRecordButton.setEnabled(false);
		deleteRecordButton.addActionListener(evt -> deleteAction());
		deleteRecordButton.setVisible(!selectRecordOnly);

		final boolean useCollection = useCollection();
		addButton.setEnabled(false);
		addButton.addActionListener(evt -> addToCollectionAction());
		addButton.setVisible(useCollection && !showCollectionOnly);
		removeButton.setEnabled(false);
		removeButton.addActionListener(evt -> removeFromCollectionAction());
		removeButton.setVisible(useCollection && !showCollectionOnly);

		collectionTable.setFocusable(false);
		collectionTable.setGridColor(GRID_COLOR);
		TableHelper.setColumnFixedWidth(collectionTable, TABLE_INDEX_ID, TABLE_PREFERRED_WIDTH_ID);
		collectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		collectionTable.getSelectionModel()
			.addListSelectionListener(evt -> {
				if(!evt.getValueIsAdjusting() && collectionTable.getSelectedRow() >= 0)
					removeButton.setEnabled(true);
			});
		collectionTable.setAutoCreateRowSorter(true);
		final TableRowSorter<TableModel> collectionSorter = new TableRowSorter<>(getCollectionTableModel());
		for(int columnIndex = 0; columnIndex < columnCount; columnIndex ++){
			final Comparator<?> comparator = comparators[columnIndex];

			if(comparator != null)
				collectionSorter.setComparator(columnIndex, comparator);
		}
		collectionTable.setRowSorter(collectionSorter);
		final TableColumnModel collectionColumnModel = collectionTable.getColumnModel();
		collectionTable.getTableHeader()
			.setDefaultRenderer(new DefaultTableCellRenderer(){
				@Override
				public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column){
					final Component headerCell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					String cellText = null;
					if(value != null){
						cellText = value.toString();
						final FontMetrics fm = headerCell.getFontMetrics(tableFont);
						final int textWidth = fm.stringWidth(cellText);
						final Insets insets = ((Container)headerCell).getInsets();
						final int cellWidth = collectionColumnModel.getColumn(column).getWidth()
							- insets.left - insets.right;

						if(textWidth <= cellWidth)
							cellText = null;
					}
					setToolTipText(cellText);

					setBorder(cellBorder);

					final int alignment = alignments[table.convertColumnIndexToModel(column)];
					setHorizontalAlignment(alignment);
					setFont(headerFont);

					return headerCell;
				}
			});
		final TableColumn collectionHiddenColumn = collectionColumnModel.getColumn(TABLE_INDEX_FILTER);
		collectionColumnModel.removeColumn(collectionHiddenColumn);
	}

	protected abstract void initRecordComponents();

	@Override
	protected void initDialog(){
		super.initDialog();

		//data edit dialog
		getRootPane().registerKeyboardAction(this::editDialogAction, GUIHelper.CONTROL_E, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	@Override
	protected final void initLayout(){
		initRecordLayout(recordTabbedPane);
		if(showRecordHistory)
			recordTabbedPane.add(RECORD_PANEL_NAME_HISTORY, historyPanel);
		if(showRecordResearchStatus)
			recordTabbedPane.add(RECORD_PANEL_NAME_RESEARCH_STATUS, researchStatusPanel);

		if(showRecordOnly){
//			recordTabbedPane.setBorder(BorderFactory.createTitledBorder("Record"));

			filterLabel.setVisible(false);
			filterField.setVisible(false);
			recordTableScrollPane.setVisible(false);
			newRecordButton.setVisible(false);
			unselectRecordButton.setVisible(false);
			deleteRecordButton.setVisible(false);
		}
		final boolean useCollection = useCollection();
		collectionTableScrollPane.setVisible(useCollection);
		final boolean showRecordTabbedPane = (!selectRecordOnly || recordTabbedPane.getComponentCount() > 0);
		if(!showRecordTabbedPane)
			recordTabbedPane.setVisible(false);

		setLayout(new MigLayout(StringUtils.EMPTY, (useCollection? "[sizegroup main,grow][sizegroup main,grow]": "[grow]")));
		add(filterLabel, "align label,split 2,hidemode 3");
		add(filterField, "grow,wrap related,hidemode 3");
		add(recordTableScrollPane, "grow,wrap paragraph,hidemode 3");
		add(newRecordButton, "sizegroup btn,tag add,split 3,align right,hidemode 3");
		add(unselectRecordButton, "sizegroup btn,tag unselect,gapleft 30,align right,hidemode 3"
			+ (deleteRecordButton.isVisible()? StringUtils.EMPTY: ",wrap paragraph"));
		add(deleteRecordButton, "sizegroup btn,tag delete,gapleft 30,hidemode 3"
			+ (deleteRecordButton.isVisible()? ",wrap paragraph": StringUtils.EMPTY));
		add(removeButton, "sizegroup btn,tag add,split 2,align right,hidemode 3");
		add(addButton, "sizegroup btn,tag unselect,gapleft 30,align right,hidemode 3,wrap paragraph");
		add(recordTabbedPane, "grow,hidemode 3");
		if(useCollection)
			add(collectionTableScrollPane, "grow,span 1 10,wrap,cell 1 0");

		if(selectRecordOnly && showRecordOnly)
			GUIHelper.setDisabled(recordTabbedPane, viewOnlyComponents);

		pack();
	}

	protected void addViewOnlyComponents(final JButton... components){
		Collections.addAll(viewOnlyComponents, components);
	}

	@Override
	public boolean isViewOnlyComponent(final int componentID){
		return viewOnlyComponents.contains(getButtonComponent(componentID));
	}

	protected void addButtonComponent(final int buttonID, JButton button){
		components.put(buttonID, button);
	}

	protected Component getButtonComponent(final int componentID){
		return components.get(componentID);
	}

	protected DefaultTableModel getRecordTableModel(){
		return (DefaultTableModel)recordTable.getModel();
	}

	protected DefaultTableModel getCollectionTableModel(){
		return (DefaultTableModel)collectionTable.getModel();
	}

	public Map<String, Object> extractRelationshipData(final int collectionID, final int dataColumnIndex){
		final DefaultTableModel model = getCollectionTableModel();
		//cycle through each row of the model and find the one with the given `collectionID`
		for(int row = 0, length = model.getRowCount(); row < length; row ++){
			final int viewRowIndex = recordTable.convertRowIndexToView(row);
			final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
			final Integer recordID = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_ID);
			if(recordID == collectionID)
				return (Map<String, Object>)model.getValueAt(row, dataColumnIndex);
		}
		return Collections.emptyMap();
	}

	public final boolean selectData(final int recordID){
		final DefaultTableModel model = getRecordTableModel();
		for(int row = 0, length = model.getRowCount(); row < length; row ++){
			final int viewRowIndex = recordTable.convertRowIndexToView(row);
			final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

			if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
				recordTable.setRowSelectionInterval(viewRowIndex, viewRowIndex);
				return true;
			}
		}

		LOGGER.info("{} id {} does not exists", getTableName(), recordID);

		return false;
	}

	public final boolean selectFirstData(){
		final DefaultTableModel model = getRecordTableModel();
		if(model.getRowCount() > 0){
			final int viewRowIndex = recordTable.convertRowIndexToView(0);
			recordTable.setRowSelectionInterval(viewRowIndex, viewRowIndex);
			return true;
		}
		return false;
	}

	protected void setButtonSelectEnableAndBorder(final JButton button, final boolean hasData){
		button.setEnabled(!selectRecordOnly || hasData);
		GUIHelper.addBorder(button, hasData, DATA_BUTTON_BORDER_COLOR);
	}

	protected void setButtonEnableAndBorder(final JButton button, final boolean hasData){
		button.setEnabled(hasData);
		GUIHelper.addBorder(button, hasData, DATA_BUTTON_BORDER_COLOR);
	}

	protected void setCheckBoxEnableAndBorder(final JCheckBox checkBox, final boolean isSelected){
		checkBox.setEnabled(!selectRecordOnly);
		checkBox.setSelected(isSelected);
	}

	private void editDialogAction(final ActionEvent evt){
		if(!selectRecordOnly)
			return;

		selectRecordOnly = false;

		//get visible pane name
		final String visiblePaneName = recordTabbedPane.getTitleAt(recordTabbedPane.getSelectedIndex());

		GUIHelper.setEnabled(recordTabbedPane);

		resetDialog();

		initLayout();

		//restore previously selected pane
		final int paneIndex = recordTabbedPane.indexOfTab(visiblePaneName);
		if(paneIndex >= 0)
			recordTabbedPane.setSelectedIndex(paneIndex);
	}

	private void resetDialog(){
		recordTabbedPane.removeAll();

		getContentPane().removeAll();
	}

	private void filterTableBy(final JDialog panel){
		final String title = GUIHelper.getTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_FILTER);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	protected abstract String[] getTableColumnNames();

	protected abstract int[] getTableColumnAlignments();

	protected abstract Comparator<?>[] getTableColumnComparators();

	public void loadData(final Integer id){
		if(id == null){
			LOGGER.error("Cannot call loadData with null ID");

			throw new IllegalArgumentException("Cannot call loadData with null ID");
		}

		final String tableName = getTableName();
		final Map<String, Object> record = Repository.findByID(tableName, id);
		final String capitalizedTableName = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
		setTitle(capitalizedTableName + " ID " + id);

		selectedRecord = (record != null? new HashMap<>(record): new HashMap<>());
		selectedRecordID = extractRecordID(selectedRecord);
		selectedRecordLink = null;

		selectActionInner();
	}

	public void reloadResearchStatusTable(){
		researchStatusPanel.loadData();
	}

	@Override
	protected final void selectAction(){
		if(!validateData()){
			final ListSelectionModel selectionModel = recordTable.getSelectionModel();
			ignoreEvents = true;
			if(previousIndex != -1)
				selectionModel.setSelectionInterval(previousIndex, previousIndex);
			else
				selectionModel.clearSelection();
			ignoreEvents = false;
		}
		else{
			previousIndex = recordTable.getSelectedRow();

			final Map<String, Object> record = getSelectedRecord();
			if(record == null)
				return;

			if(dataHasChanged())
				modifiedRecords.addModifiedRecord(selectedRecord);
			selectedRecord = new HashMap<>(record);
			selectedRecordID = extractRecordID(record);
			selectedRecordLink = null;

			GUIHelper.setEnabled(recordPanel, (!showRecordOnly || !selectRecordOnly));

			selectActionInner();

			//enable unselect button only if the record is not new
			final int modelRowIndex = recordTable.convertRowIndexToModel(previousIndex);

			final TableModel model = getRecordTableModel();
			final Integer recordIdentifier = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_ID);
			unselectRecordButton.setEnabled(recordIdentifier != null);

			deleteRecordButton.setEnabled(!selectRecordOnly);
			addButton.setEnabled(useCollection() && !showCollectionOnly && canAddToCollection(selectedRecordID));
		}

		GUIHelper.executeOnEventDispatchThread(this::requestFocusAfterSelect);
	}

	private void selectActionInner(){
		updateRecordHash();

		if(!selectRecordOnly)
			GUIHelper.setEnabled(recordTabbedPane);

		if(newRecordDefault != null)
			newRecordDefault.accept(selectedRecord);


		ignoreEvents = true;

		fillData();

		final String tableName = getTableName();
		final int recordID = extractRecordID(selectedRecord);
		if(showRecordHistory){
			historyPanel.withReference(tableName, recordID);
			historyPanel.loadData();
		}
		if(showRecordResearchStatus){
			researchStatusPanel.withReference(tableName, recordID);
			researchStatusPanel.loadData();
		}

		ignoreEvents = false;
	}

	@Override
	public Map<String, Object> getSelectedRecord(){
		final int viewRowIndex = recordTable.getSelectedRow();
		if(viewRowIndex == -1)
			//no row selected
			return null;

		final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

		final TableModel model = getRecordTableModel();
		final Integer recordID = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_ID);

		return Repository.findByID(getTableName(), recordID);
	}

	protected Integer getCollectionSelectedRecordID(){
		final int viewRowIndex = collectionTable.getSelectedRow();
		if(viewRowIndex == -1)
			//no row selected
			return null;

		final int modelRowIndex = collectionTable.convertRowIndexToModel(viewRowIndex);

		final TableModel model = getCollectionTableModel();
		return (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_ID);
	}

	public final void showNewRecord(){
		newAction();
	}

	private void newAction(){
		newRecordButton.setEnabled(false);

		if(selectedRecord != null && !validateData())
			return;

		ignoreEvents = true;

		//create a new record
		final int nextRecordID = Repository.upsert(new HashMap<>(), getTableName());

		//reset filter
		filterField.setText(null);

		//add to table
		final DefaultTableModel model = getRecordTableModel();
		final int oldSize = model.getRowCount();
		model.setRowCount(oldSize + 1);
		model.setValueAt(nextRecordID, oldSize, TABLE_INDEX_ID);
		//resort rows
		final RowSorter<? extends TableModel> recordTableSorter = recordTable.getRowSorter();
		recordTableSorter.setSortKeys(recordTableSorter.getSortKeys());

		ignoreEvents = false;

		//select the newly created record
		final int newRowIndex = recordTable.convertRowIndexToView(oldSize);
		recordTable.setRowSelectionInterval(newRowIndex, newRowIndex);
		//make selected row visible
		recordTable.scrollRectToVisible(recordTable.getCellRect(newRowIndex, 0, true));

		setMandatoryFieldsBackgroundColor(MANDATORY_BACKGROUND_COLOR);
	}

	protected void requestFocusAfterSelect(){}

	protected void unselectAction(){
		//clear previously selected row
		selectedRecord = null;
		selectedRecordLink = null;

		ignoreEvents = true;
		clearData();
		ignoreEvents = false;

		GUIHelper.setDisabled(recordTabbedPane, viewOnlyComponents);
		unselectRecordButton.setEnabled(false);
		deleteRecordButton.setEnabled(false);
		addButton.setEnabled(false);

		//clear selection from table
		recordTable.clearSelection();
	}

	private void deleteAction(){
		final int viewRowIndex = recordTable.getSelectedRow();
		if(viewRowIndex < 0)
			//no row selected
			return;

		final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

		final DefaultTableModel model = getRecordTableModel();
		final Integer recordID = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_ID);
		modifiedRecords.addRemovedRecordID(recordID);

		//clear previously selected row
		selectedRecord = null;
		selectedRecordLink = null;

		ignoreEvents = true;
		clearData();
		ignoreEvents = false;

		GUIHelper.setDisabled(recordTabbedPane, viewOnlyComponents);
		newRecordButton.setEnabled(!selectRecordOnly);
		unselectRecordButton.setEnabled(false);
		deleteRecordButton.setEnabled(false);
		addButton.setEnabled(false);
		setMandatoryFieldsBackgroundColor(Color.WHITE);


		//remove from database
		if(Repository.deleteNode(getTableName(), recordID))
			//remove row from table
			model.removeRow(modelRowIndex);
	}

	protected boolean canAddToCollection(final int recordID){
		return !collectionIDs.contains(recordID);
	}

	protected void addToCollectionAction(){}

	/**
	 * Performs final actions required after adding a record to the collection.
	 * <p>
	 * This method sorts the collection table and disables the add button once the record has been added. Specifically, it extracts the
	 * record ID from the selected record, adds it to the collection of record IDs, and then disables the add button to prevent further
	 * additions without re-selection.
	 * </p>
	 */
	protected void finalizeAddToCollectionAction(){
		final TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>)collectionTable.getRowSorter();
		sorter.sort();


		final Integer recordID = extractRecordID(selectedRecord);
		collectionIDs.add(recordID);
		addButton.setEnabled(false);
	}

	private void removeFromCollectionAction(){
		//remove selected record from collection table:

		final Integer collectionRecordID = getCollectionSelectedRecordID();

		final int viewRowIndex = collectionTable.getSelectedRow();
		final int modelRowIndex = collectionTable.convertRowIndexToModel(viewRowIndex);
		final DefaultTableModel model = getCollectionTableModel();
		model.removeRow(modelRowIndex);


		if(collectionRecordID != null){
			collectionIDs.remove(collectionRecordID);
			if(collectionRecordID.equals(selectedRecordID))
				addButton.setEnabled(true);
		}
		removeButton.setEnabled(false);
	}

	@Override
	public void onValidationChange(final boolean valid){
		if(newRecordButton != null)
			newRecordButton.setEnabled(!selectRecordOnly && (selectedRecord == null || valid));
		if(unselectRecordButton != null)
			unselectRecordButton.setEnabled(selectedRecord != null);
	}

	public void showDialog(){
		setLocationRelativeTo(getParent());
		setVisible(true);
	}

}
