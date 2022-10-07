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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


//TODO
public class CulturalRuleRecordDialog extends JDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 3322392561648823462L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_RULE_ID = 0;
	private static final int TABLE_INDEX_RULE_TITLE = 1;

	private static final String KEY_RULE_ID = "ruleID";

	private static final DefaultComboBoxModel<String> CERTAINTY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Challenged",
		"Disproven",
		"Proven"});
	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable rulesTable = new JTable(new CulturalRuleTableModel());
	private final JScrollPane rulesScrollPane = new JScrollPane(rulesTable);
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
	private final JComboBox<String> placeCredibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<CulturalRuleRecordDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private final Flef store;


	public CulturalRuleRecordDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Rule citations");

		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(CulturalRuleRecordDialog.this);
			}
		});

		rulesTable.setAutoCreateRowSorter(true);
		rulesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		rulesTable.setGridColor(GRID_COLOR);
		rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		rulesTable.setDragEnabled(true);
		rulesTable.setDropMode(DropMode.INSERT_ROWS);
		rulesTable.setTransferHandler(new TableTransferHandle(rulesTable));
		rulesTable.getTableHeader().setFont(rulesTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(rulesTable, TABLE_INDEX_RULE_ID, 0, ID_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(rulesTable.getModel());
		sorter.setComparator(TABLE_INDEX_RULE_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_RULE_TITLE, Comparator.naturalOrder());
		rulesTable.setRowSorter(sorter);
		//clicking on a line links it to current source citation
		rulesTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = rulesTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0){
				selectAction(rulesTable.convertRowIndexToModel(selectedRow));
			}
		});
		rulesTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && rulesTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		rulesTable.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		rulesTable.getActionMap()
			.put("delete", new AbstractAction(){
				@Serial
				private static final long serialVersionUID = 3784664925849526371L;

				@Override
				public void actionPerformed(final ActionEvent evt){
					deleteAction();
				}


				@SuppressWarnings("unused")
				@Serial
				private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
					throw new NotSerializableException(getClass().getName());
				}

				@SuppressWarnings("unused")
				@Serial
				private void readObject(final ObjectInputStream is) throws NotSerializableException{
					throw new NotSerializableException(getClass().getName());
				}
			});
		rulesTable.setPreferredScrollableViewportSize(new Dimension(rulesTable.getPreferredSize().width,
			rulesTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> {
			final GedcomNode newRule = store.create("CULTURAL_RULE");

			final Consumer<Object> onCloseGracefully = ignored -> {
				//if ok was pressed, add this rule to the parent container
				final String newRuleID = store.addCulturalRule(newRule);
				container.addChildReference("CULTURAL_RULE", newRuleID);

				//refresh rule list
				loadData();
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_RULE, newRule, onCloseGracefully));
		});
		editButton.addActionListener(evt -> editAction());

		ruleTitleLabel.setLabelFor(ruleTitleField);
		ruleTitleField.setEnabled(false);

		localeLabel.setLabelFor(localeComboBox);
		localeComboBox.setEnabled(false);

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
		placePanel.add(placeButton, "sizegroup button2,grow,wrap");
		placePanel.add(placeCertaintyLabel, "align label,split 2");
		placePanel.add(placeCertaintyComboBox, "wrap");
		placePanel.add(placeCredibilityLabel, "align label,split 2");
		placePanel.add(placeCredibilityComboBox);
		GUIHelper.setEnabled(placePanel, false);

		notesButton.setEnabled(false);
		//TODO
//		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, repository)));

		sourcesButton.setEnabled(false);
		//TODO
//		sourcesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, group)));

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//remove all reference to the rules from the container
			container.removeChildrenWithTag("CULTURAL_RULE");
			//add all the remaining references to rules to the container
			for(int i = 0; i < rulesTable.getRowCount(); i ++){
				final String id = (String)rulesTable.getValueAt(i, TABLE_INDEX_RULE_ID);
				container.addChildReference("CULTURAL_RULE", id);
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
		add(rulesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button2");
		add(editButton, "tag edit,sizegroup button2,wrap paragraph");
		add(ruleTitleLabel, "align label,sizegroup label,split 2");
		add(ruleTitleField, "grow,wrap");
		add(descriptionPanel, "grow,wrap");
		add(placePanel, "grow,wrap paragraph");
		add(notesButton, "grow,wrap");
		add(sourcesButton, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void transferListToContainer(){
		//remove all reference to the  groups from the container
		container.removeChildrenWithTag("CULTURAL_RULE");
		//add all the remaining references to groups to the container
		for(int i = 0; i < rulesTable.getRowCount(); i ++){
			final String id = (String)rulesTable.getValueAt(i, TABLE_INDEX_RULE_ID);
			container.addChildReference("CULTURAL_RULE", id);
		}
	}

	private void selectAction(final int selectedRow){
		final String selectedRuleID = (String)rulesTable.getValueAt(selectedRow, TABLE_INDEX_RULE_ID);
		final GedcomNode selectedRule = store.getCulturalRule(selectedRuleID);
		okButton.putClientProperty(KEY_RULE_ID, selectedRuleID);

		ruleTitleField.setEnabled(true);
		ruleTitleField.setText(store.traverse(selectedRule, "TITLE").getValue());

		localeComboBox.setEnabled(true);

		GUIHelper.setEnabled(descriptionPanel, true);
		final String languageTag = store.traverse(selectedRule, "LOCALE").getValue();
		final String text = store.traverse(selectedRule, "DESCRIPTION").getValue();
		descriptionPreviewView.setText(getTitle(), text, languageTag);

		GUIHelper.setEnabled(placePanel, true);

		notesButton.setEnabled(true);

		sourcesButton.setEnabled(true);

		//TODO

		okButton.setEnabled(true);
	}

	private void editAction(){
		//retrieve selected rule
		final DefaultTableModel model = (DefaultTableModel)rulesTable.getModel();
		final int index = rulesTable.convertRowIndexToModel(rulesTable.getSelectedRow());
		final String ruleXRef = (String)model.getValueAt(index, TABLE_INDEX_RULE_ID);
		final GedcomNode selectedRule = store.getCulturalRule(ruleXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_RULE, selectedRule));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)rulesTable.getModel();
		final int index = rulesTable.convertRowIndexToModel(rulesTable.getSelectedRow());
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

	public void loadData(final GedcomNode container){
		this.container = container;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> rules = store.traverseAsList(container, "CULTURAL_RULE[]");
		final int size = rules.size();
		for(int i = 0; i < size; i ++)
			rules.set(i, store.getCulturalRule(rules.get(i).getXRef()));

		if(size > 0){
			final DefaultTableModel rulesModel = (DefaultTableModel)rulesTable.getModel();
			rulesModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode rule = rules.get(row);

				rulesModel.setValueAt(rule.getID(), row, TABLE_INDEX_RULE_ID);
				rulesModel.setValueAt(store.traverse(rule, "TITLE").getValue(), row, TABLE_INDEX_RULE_TITLE);
			}
		}
	}

	private void filterTableBy(final CulturalRuleRecordDialog panel){
		final String title = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RULE_ID, TABLE_INDEX_RULE_TITLE);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)rulesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)rulesTable.getModel();
			sorter = new TableRowSorter<>(model);
			rulesTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}


	private static class CulturalRuleTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -581310490684534579L;


		CulturalRuleTableModel(){
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


		@SuppressWarnings("unused")
		@Serial
		private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

		@SuppressWarnings("unused")
		@Serial
		private void readObject(final ObjectInputStream is) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.7.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final CulturalRuleRecordDialog dialog = new CulturalRuleRecordDialog(store, new JFrame());
			dialog.loadData(container);

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
