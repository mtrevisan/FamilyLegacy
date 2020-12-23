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
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class CulturalRuleCitationDialog extends JDialog implements TextPreviewListenerInterface{

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
	private final JButton removeButton = new JButton("Remove");
	private final JLabel ruleTitleLabel = new JLabel("Title:");
	private final JTextField ruleTitleField = new JTextField();
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleFilteredComboBox localeComboBox = new LocaleFilteredComboBox();
	private final JLabel descriptionLabel = new JLabel("Description:");
	private TextPreviewPane descriptionPreviewView;
	private final JButton placeButton = new JButton("Place");
	private final JLabel placeCertaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> placeCertaintyComboBox = new JComboBox<>(CERTAINTY_MODEL);
	private final JLabel placeCredibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> placeCredibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<CulturalRuleCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private final Flef store;


	public CulturalRuleCitationDialog(final Flef store, final Frame parent){
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
				filterDebouncer.call(CulturalRuleCitationDialog.this);
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
			removeButton.setEnabled(true);

			final int selectedRow = rulesTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0){
				final String selectedRuleID = (String)rulesTable.getValueAt(selectedRow, TABLE_INDEX_RULE_ID);
				final GedcomNode selectedRule = store.getCulturalRule(selectedRuleID);
				okButton.putClientProperty(KEY_RULE_ID, selectedRuleID);
				ruleTitleField.setText(store.traverse(selectedRule, "TITLE").getValue());

				final String languageTag = store.traverse(selectedRule, "LOCALE").getValue();
				final String text = store.traverse(selectedRule, "DESCRIPTION").getValue();
				descriptionPreviewView.setText(getTitle(), languageTag, text);

				//TODO

				okButton.setEnabled(true);
			}
		});
		rulesTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && rulesTable.rowAtPoint(evt.getPoint()) >= 0)
					//fire edit event
					editAction();
			}
		});

		addButton.addActionListener(evt -> {
			final GedcomNode newRule = store.create("CULTURAL_RULE");

			final Runnable onCloseGracefully = () -> {
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
		removeButton.setEnabled(false);
		removeButton.addActionListener(evt -> deleteAction());

		ruleTitleLabel.setLabelFor(ruleTitleField);
		ruleTitleField.setEnabled(false);

		localeLabel.setLabelFor(localeComboBox);

		descriptionPreviewView = new TextPreviewPane(this);

		//TODO
//		placeButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE_CITATION, repository)));
		final JPanel placePanel = new JPanel();
		placePanel.setBorder(BorderFactory.createTitledBorder("Place"));
		placePanel.setLayout(new MigLayout("", "[grow]"));
		placePanel.add(placeButton, "sizegroup button2,grow,wrap");
		placePanel.add(placeCertaintyLabel, "align label,split 2");
		placePanel.add(placeCertaintyComboBox, "grow,wrap");
		placePanel.add(placeCredibilityLabel, "align label,split 2");
		placePanel.add(placeCredibilityComboBox, "grow");

		notesButton.setEnabled(false);
		//TODO
//		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, repository)));

		sourcesButton.setEnabled(false);
		//TODO
//		sourcesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, group)));

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//remove all reference to rules from the container
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

		setLayout(new MigLayout("", "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(rulesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button");
		add(editButton, "tag edit,sizegroup button");
		add(removeButton, "tag remove,sizegroup button,wrap paragraph");
		add(ruleTitleLabel, "align label,sizegroup label,split 2");
		add(ruleTitleField, "grow,wrap");
		add(localeLabel, "align label,split 2,sizegroup label");
		add(localeComboBox, "wrap");
		add(descriptionLabel, "wrap");
		add(descriptionPreviewView, "span 2,grow,wrap paragraph");
		add(placePanel, "grow,wrap paragraph");
		add(notesButton, "grow,wrap");
		add(sourcesButton, "grow,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
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
		model.removeRow(rulesTable.convertRowIndexToModel(rulesTable.getSelectedRow()));
		removeButton.setEnabled(false);
	}

	@Override
	public void onPreviewStateChange(final boolean previewVisible){
		TextPreviewListenerInterface.centerDivider(this, previewVisible);
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

	private void filterTableBy(final CulturalRuleCitationDialog panel){
		final String title = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(title, TABLE_INDEX_RULE_ID, TABLE_INDEX_RULE_TITLE);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)rulesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)rulesTable.getModel();
			sorter = new TableRowSorter<>(model);
			rulesTable.setRowSorter(sorter);
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


	private static class CulturalRuleTableModel extends DefaultTableModel{

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
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.5.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final CulturalRuleCitationDialog dialog = new CulturalRuleCitationDialog(store, new JFrame());
			dialog.loadData(container);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 700);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
