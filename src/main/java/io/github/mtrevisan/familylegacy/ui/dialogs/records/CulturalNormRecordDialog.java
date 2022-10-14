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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.ui.utilities.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


//TODO
/*
	+1 TITLE <CULTURAL_NORM_DESCRIPTIVE_TITLE>    {0:1}
	+1 PLACE @<XREF:PLACE>@    {0:1}
		+2 CERTAINTY <CERTAINTY_ASSESSMENT>    {0:1}
		+2 CREDIBILITY <CREDIBILITY_ASSESSMENT>    {0:1}
	+1 NOTE @<XREF:NOTE>@    {0:M}
	+1 <<SOURCE_CITATION>>    {0:M}
	+1 CREATION    {1:1}
		+2 DATE <CREATION_DATE>    {1:1}
	+1 UPDATE    {0:M}
		+2 DATE <UPDATE_DATE>    {1:1}
		+2 NOTE @<XREF:NOTE>@    {0:1}
*/
public class CulturalNormRecordDialog extends JDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 3322392561648823462L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_NORM_ID = 0;
	private static final int TABLE_INDEX_RULE_TITLE = 1;

	private static final String KEY_NORM_ID = "normID";

	private static final DefaultComboBoxModel<String> CERTAINTY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Challenged",
		"Disproven",
		"Proven"});

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable culturalNormsTable = new JTable(new CulturalNormTableModel());
	private final JScrollPane culturalNormsScrollPane = new JScrollPane(culturalNormsTable);
	private final JButton addButton = new JButton("Add");
	private final JButton editButton = new JButton("Edit");
	private final JLabel ruleTitleLabel = new JLabel("Title:");
	private final JTextField ruleTitleField = new JTextField();
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleComboBox localeComboBox = new LocaleComboBox();
	private final JPanel descriptionPanel = new JPanel();
	private TextPreviewPane descriptionPreviewView;
	private final JPanel placePanel = new JPanel();
	private final JButton placeButton = new JButton("Place");
	private final JLabel placeCertaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> placeCertaintyComboBox = new JComboBox<>(CERTAINTY_MODEL);
	private final JLabel placeCredibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> placeCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());
	private final JButton noteButton = new JButton("Notes");
	private final JButton sourceButton = new JButton("Sources");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<CulturalNormRecordDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public CulturalNormRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(CulturalNormRecordDialog.this);
			}
		});

		culturalNormsTable.setAutoCreateRowSorter(true);
		culturalNormsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		culturalNormsTable.setGridColor(GRID_COLOR);
		culturalNormsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		culturalNormsTable.setDragEnabled(true);
		culturalNormsTable.setDropMode(DropMode.INSERT_ROWS);
		culturalNormsTable.setTransferHandler(new TableTransferHandle(culturalNormsTable));
		culturalNormsTable.getTableHeader().setFont(culturalNormsTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(culturalNormsTable, TABLE_INDEX_NORM_ID, 0, ID_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(culturalNormsTable.getModel());
		sorter.setComparator(TABLE_INDEX_NORM_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_RULE_TITLE, Comparator.naturalOrder());
		culturalNormsTable.setRowSorter(sorter);
		//clicking on a line links it to current source citation
		culturalNormsTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = culturalNormsTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0){
				selectAction(culturalNormsTable.convertRowIndexToModel(selectedRow));
			}
		});
		culturalNormsTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && culturalNormsTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		culturalNormsTable.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		culturalNormsTable.getActionMap()
			.put("delete", new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent evt){
					deleteAction();
				}
			});
		culturalNormsTable.setPreferredScrollableViewportSize(new Dimension(culturalNormsTable.getPreferredSize().width,
			culturalNormsTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> {
			final GedcomNode culturalNorm = store.create("CULTURAL_NORM");

			final Consumer<Object> onCloseGracefully = ignored -> {
				//if ok was pressed, add this rule to the parent container
				final String culturalNormID = store.addCulturalNorm(culturalNorm);
				container.addChildReference("CULTURAL_NORM", culturalNormID);

				//refresh rule list
				loadData();
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM, culturalNorm, onCloseGracefully));
		});
		editButton.addActionListener(evt -> editAction());

		ruleTitleLabel.setLabelFor(ruleTitleField);
		GUIHelper.setEnabled(ruleTitleLabel, false);

		localeLabel.setLabelFor(localeComboBox);
		GUIHelper.setEnabled(localeLabel, false);

		descriptionPreviewView = TextPreviewPane.createWithPreview(this);
		descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description"));
		descriptionPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		descriptionPanel.add(descriptionPreviewView, "span 2,grow,wrap");
		descriptionPanel.add(localeLabel, "align label,split 2,sizegroup label");
		descriptionPanel.add(localeComboBox);
		GUIHelper.setEnabled(descriptionPanel, false);

		placeCertaintyLabel.setLabelFor(placeCertaintyComboBox);
		placeCredibilityLabel.setLabelFor(placeCredibilityComboBox);

		//TODO
//		placeButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE_CITATION, repository)));
		placePanel.setBorder(BorderFactory.createTitledBorder("Place"));
		placePanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		placePanel.add(placeButton, "sizegroup button,grow,wrap");
		placePanel.add(placeCertaintyLabel, "align label,split 2");
		placePanel.add(placeCertaintyComboBox, "wrap");
		placePanel.add(placeCredibilityLabel, "align label,split 2");
		placePanel.add(placeCredibilityComboBox);
		GUIHelper.setEnabled(placePanel, false);

		noteButton.setEnabled(false);
		//TODO
//		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, repository)));

		sourceButton.setEnabled(false);
		//TODO
//		sourceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, group)));

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//remove all reference to the rules from the container
			container.removeChildrenWithTag("CULTURAL_NORM");
			//add all the remaining references to rules to the container
			for(int i = 0; i < culturalNormsTable.getRowCount(); i ++){
				final String id = (String)culturalNormsTable.getValueAt(i, TABLE_INDEX_NORM_ID);
				container.addChildReference("CULTURAL_NORM", id);
			}

			//TODO
			final String text = descriptionPreviewView.getText();
//			container.withValue(text);

			//TODO remember, when saving the whole gedcom, to remove all non-referenced rules!

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(culturalNormsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button");
		add(editButton, "tag edit,sizegroup button,wrap paragraph");
		add(ruleTitleLabel, "align label,sizegroup label,split 2");
		add(ruleTitleField, "grow,wrap");
		add(descriptionPanel, "grow,wrap");
		add(placePanel, "grow,wrap paragraph");
		add(noteButton, "grow,wrap");
		add(sourceButton, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void transferListToContainer(){
		//remove all reference to the  groups from the container
		container.removeChildrenWithTag("CULTURAL_NORM");
		//add all the remaining references to groups to the container
		for(int i = 0; i < culturalNormsTable.getRowCount(); i ++){
			final String id = (String)culturalNormsTable.getValueAt(i, TABLE_INDEX_NORM_ID);
			container.addChildReference("CULTURAL_NORM", id);
		}
	}

	private void selectAction(final int selectedRow){
		final String selectedNormID = (String)culturalNormsTable.getValueAt(selectedRow, TABLE_INDEX_NORM_ID);
		final GedcomNode selectedRule = store.getCulturalNorm(selectedNormID);
		okButton.putClientProperty(KEY_NORM_ID, selectedNormID);

		GUIHelper.setEnabled(ruleTitleLabel, true);
		ruleTitleField.setText(store.traverse(selectedRule, "TITLE").getValue());

		GUIHelper.setEnabled(localeLabel, true);

		GUIHelper.setEnabled(descriptionPanel, true);
		final String languageTag = store.traverse(selectedRule, "LOCALE").getValue();
		final String text = store.traverse(selectedRule, "DESCRIPTION").getValue();
		descriptionPreviewView.setText(getTitle(), text, languageTag);

		GUIHelper.setEnabled(placePanel, true);

		noteButton.setEnabled(true);

		sourceButton.setEnabled(true);

		//TODO

		okButton.setEnabled(true);
	}

	private void editAction(){
		//retrieve selected rule
		final DefaultTableModel model = (DefaultTableModel)culturalNormsTable.getModel();
		final int index = culturalNormsTable.convertRowIndexToModel(culturalNormsTable.getSelectedRow());
		final String ruleXRef = (String)model.getValueAt(index, TABLE_INDEX_NORM_ID);
		final GedcomNode selectedRule = store.getCulturalNorm(ruleXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM, selectedRule));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)culturalNormsTable.getModel();
		final int index = culturalNormsTable.convertRowIndexToModel(culturalNormsTable.getSelectedRow());
		model.removeRow(index);

		//remove from container
		transferListToContainer();
	}

	@Override
	public void textChanged(){
		//TODO
		okButton.setEnabled(true);
	}

	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	public void loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> rules = store.traverseAsList(container, "CULTURAL_NORM[]");
		final int size = rules.size();
		for(int i = 0; i < size; i ++)
			rules.set(i, store.getCulturalNorm(rules.get(i).getXRef()));

		if(size > 0){
			final DefaultTableModel rulesModel = (DefaultTableModel)culturalNormsTable.getModel();
			rulesModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode rule = rules.get(row);

				rulesModel.setValueAt(rule.getID(), row, TABLE_INDEX_NORM_ID);
				rulesModel.setValueAt(store.traverse(rule, "TITLE").getValue(), row, TABLE_INDEX_RULE_TITLE);
			}
		}
	}

	private void filterTableBy(final CulturalNormRecordDialog panel){
		final String title = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_NORM_ID, TABLE_INDEX_RULE_TITLE);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)culturalNormsTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)culturalNormsTable.getModel();
			sorter = new TableRowSorter<>(model);
			culturalNormsTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}


	private static class CulturalNormTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -581310490684534579L;


		CulturalNormTableModel(){
			super(new String[]{"ID", "Title"}, 0);
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


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.8.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final CulturalNormRecordDialog dialog = new CulturalNormRecordDialog(store, new JFrame());
			dialog.loadData(container, null);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(480, 700);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
