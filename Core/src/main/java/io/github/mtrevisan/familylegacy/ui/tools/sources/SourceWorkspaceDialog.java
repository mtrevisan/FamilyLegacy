/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.sources.SourceWorkspace;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;


/**
 * The source-centric workspace: the sources on the left, listed by repository, by kind of repository or by shelf mark (the collection or
 * record group); what hangs from the selected source on the right, one tab for each kind of record:
 * <ul>
 *   <li><b>Citations</b>: every citation of the source, with the record that carries it, the locator and the stance; selecting one lists
 *   below the individuals it is about, so the way back from the source ends at a person (source -> citation -> individual);</li>
 *   <li><b>Individuals</b>, <b>Families</b> and <b>Events</b> that the source backs;</li>
 *   <li><b>Extracts</b> of the source and the <b>media</b> it refers to.</li>
 * </ul>
 * A double-click opens the record to fix it on the spot; the workspace is read again afterwards. Nothing here changes the file by itself.
 */
final class SourceWorkspaceDialog extends JDialog{

	private static final int SOURCE_LIST_WIDTH = 300;

	private final FLEFModel model;

	private final JComboBox<SourceWorkspace.Grouping> grouping = new JComboBox<>(SourceWorkspace.Grouping.values());
	private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private final JTree tree = new JTree(new DefaultTreeModel(root));
	private final JLabel summary = new JLabel(I18N.t("text.source_workspace.select"));
	private final JTabbedPane tabs = new JTabbedPane();

	private String selectedSourceId;
	/** Whether the tree is being rebuilt: the selection that this causes is not a choice of the user. */
	private boolean loading;
	private SourceWorkspace workspace;
	private JTable peopleTable;
	private final List<FLEFRecord> citationOwners = new ArrayList<>();
	private final List<List<FLEFRecord>> citationPeople = new ArrayList<>();
	private final List<FLEFRecord> peopleRecords = new ArrayList<>();


	/** The sources of a leaf of the tree. */
	private record SourceItem(FLEFRecord source, String title){

		@Override
		public String toString(){
			return title;
		}

	}


	/**
	 * @param owner	The window the dialog is over.
	 * @param model	The model.
	 * @param sourceId	The source to start from ({@code null} for none).
	 */
	SourceWorkspaceDialog(final Window owner, final FLEFModel model, final String sourceId){
		super(owner, I18N.t("dialog.source_workspace.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;
		selectedSourceId = sourceId;

		grouping.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
			final JLabel label = new JLabel(groupingLabel(value));
			label.setOpaque(true);
			label.setBackground(isSelected? list.getSelectionBackground(): list.getBackground());
			label.setForeground(isSelected? list.getSelectionForeground(): list.getForeground());
			return label;
		});
		grouping.addActionListener(e -> {
			if(!loading)
				reloadTree();
		});

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(e -> {
			if(loading)
				return;

			final Object node = (tree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode n? n.getUserObject(): null);
			if(node instanceof SourceItem item){
				selectedSourceId = item.source().getId();
				showSource();
			}
		});

		final JPanel sources = new JPanel(new BorderLayout(4, 4));
		final JPanel groupBy = new JPanel(new BorderLayout(6, 0));
		groupBy.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
		groupBy.add(new JLabel(I18N.t("text.source_workspace.group_by")), BorderLayout.WEST);
		groupBy.add(grouping, BorderLayout.CENTER);
		sources.add(groupBy, BorderLayout.NORTH);
		sources.add(new JScrollPane(tree), BorderLayout.CENTER);
		sources.setPreferredSize(new Dimension(SOURCE_LIST_WIDTH, 520));

		summary.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
		final JLabel hint = new JLabel(I18N.t("text.source_workspace.hint"));
		hint.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
		final JPanel header = new JPanel(new BorderLayout());
		header.add(summary, BorderLayout.NORTH);
		header.add(hint, BorderLayout.SOUTH);

		final JPanel details = new JPanel(new BorderLayout());
		details.add(header, BorderLayout.NORTH);
		details.add(tabs, BorderLayout.CENTER);
		details.setPreferredSize(new Dimension(760, 520));

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sources, details);
		split.setBorder(null);

		setLayout(new BorderLayout());
		add(split, BorderLayout.CENTER);

		reloadTree();

		pack();
		setLocationRelativeTo(owner);
	}


	private static String groupingLabel(final SourceWorkspace.Grouping value){
		return I18N.t("text.source_workspace.group." + value.name().toLowerCase(Locale.ROOT));
	}

	private String groupLabel(final String key){
		if(key.isEmpty())
			return I18N.t("text.source_workspace.no_group");

		return (grouping.getSelectedItem() == SourceWorkspace.Grouping.REPOSITORY_TYPE
			? I18N.t("enum.repository.type." + key, key)
			: key);
	}

	/** The type of a record as the user reads it ("individual attribute"), not as it is written in the file. */
	private static String typeLabel(final String tag){
		return I18N.t("confirmation.exist_record." + tag.toLowerCase(Locale.ROOT), tag);
	}


	/* ======================================================================
	 *                          Sources
	 * ====================================================================== */

	/** Builds the list of the sources again, and selects the one in view (or the first one if it is not there any more). */
	private void reloadTree(){
		loading = true;
		try{
			root.removeAllChildren();
			final SourceWorkspace.Grouping chosen = (SourceWorkspace.Grouping)grouping.getSelectedItem();
			for(final SourceWorkspace.SourceGroup group : SourceWorkspace.group(model, chosen)){
				final DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(groupLabel(group.key()) + " (" + group.sources().size() + ")");
				for(final FLEFRecord source : group.sources())
					groupNode.add(new DefaultMutableTreeNode(new SourceItem(source, SourceHelper.sourceTitle(source))));
				root.add(groupNode);
			}
			((DefaultTreeModel)tree.getModel()).reload();
			for(int row = 0; row < tree.getRowCount(); row ++)
				tree.expandRow(row);

			final TreePath path = findPath(selectedSourceId);
			if(path != null){
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
			}
			else
				tree.clearSelection();
		}
		finally{
			loading = false;
		}

		showSource();
	}

	private TreePath findPath(final String sourceId){
		if(sourceId == null)
			return null;

		final Enumeration<?> nodes = root.depthFirstEnumeration();
		while(nodes.hasMoreElements()){
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodes.nextElement();
			if(node.getUserObject() instanceof SourceItem item && sourceId.equals(item.source().getId()))
				return new TreePath(node.getPath());
		}
		return null;
	}


	/* ======================================================================
	 *                          What hangs from the source
	 * ====================================================================== */

	/** Reads the model again for the selected source and builds the tabs, keeping the one in view. */
	private void showSource(){
		final int selectedTab = Math.max(tabs.getSelectedIndex(), 0);
		tabs.removeAll();
		citationOwners.clear();
		citationPeople.clear();
		peopleRecords.clear();
		peopleTable = null;

		workspace = SourceWorkspace.of(model, selectedSourceId);
		if(workspace == null){
			summary.setText(I18N.t("text.source_workspace.select"));
			return;
		}

		summary.setText(I18N.tf("text.source_workspace.summary", workspace.citations().size(), workspace.individuals().size(),
			workspace.families().size(), workspace.events().size(), workspace.extracts().size(), workspace.media().size()));

		tabs.addTab(I18N.t("tab.title.workspace_citations"), createCitationsTab());
		tabs.addTab(I18N.t("tab.title.workspace_individuals"), createRecordsTab(workspace.individuals()));
		tabs.addTab(I18N.t("tab.title.workspace_families"), createRecordsTab(workspace.families()));
		tabs.addTab(I18N.t("tab.title.workspace_events"), createRecordsTab(workspace.events()));
		tabs.addTab(I18N.t("tab.title.workspace_extracts"), createRecordsTab(workspace.extracts()));
		tabs.addTab(I18N.t("tab.title.workspace_media"), createRecordsTab(workspace.media()));
		tabs.setSelectedIndex(Math.min(selectedTab, tabs.getTabCount() - 1));
	}

	private JPanel createCitationsTab(){
		final List<Object[]> rows = new ArrayList<>();
		for(final SourceWorkspace.Citation citation : workspace.citations()){
			final List<String> names = new ArrayList<>(citation.individuals().size());
			for(final FLEFRecord person : citation.individuals())
				names.add(RecordDisplay.describe(person, model));
			rows.add(new Object[]{typeLabel(citation.owner().getTag()) + ": " + RecordDisplay.describe(citation.owner(), model),
				citation.locator(), citation.stance(), String.join("; ", names)});
			citationOwners.add(citation.owner());
			citationPeople.add(citation.individuals());
		}
		final JTable citations = ToolDialogs.createTable(new RowTableModel(new String[]{I18N.t("column.provenance.owner"),
			I18N.t("column.provenance.locator"), I18N.t("column.provenance.stance"), I18N.t("column.individuals")}, rows),
			row -> open(citationOwners, row));
		citations.getColumnModel().getColumn(0).setPreferredWidth(300);
		citations.getColumnModel().getColumn(3).setPreferredWidth(240);
		citations.getSelectionModel()
			.addListSelectionListener(e -> {
				if(!e.getValueIsAdjusting())
					showPeopleOf(citations);
			});

		peopleTable = createPlainTable(List.of(), row -> open(peopleRecords, row));
		final JScrollPane people = new JScrollPane(peopleTable);
		people.setBorder(BorderFactory.createTitledBorder(I18N.t("border.workspace_people")));

		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(citations), people);
		split.setResizeWeight(0.65);
		split.setBorder(null);

		final JPanel tab = new JPanel(new BorderLayout());
		tab.add(split, BorderLayout.CENTER);
		return tab;
	}

	/** Lists the individuals of the selected citation: the way back from the source ends here. */
	private void showPeopleOf(final JTable citations){
		peopleRecords.clear();
		final int viewRow = citations.getSelectedRow();
		if(viewRow >= 0)
			peopleRecords.addAll(citationPeople.get(citations.convertRowIndexToModel(viewRow)));

		final List<Object[]> rows = new ArrayList<>(peopleRecords.size());
		for(final FLEFRecord person : peopleRecords)
			rows.add(new Object[]{person.getId(), RecordDisplay.describe(person, model)});
		peopleTable.setModel(new RowTableModel(new String[]{I18N.t("column.id"), I18N.t("column.record")}, rows));
		peopleTable.getColumnModel().getColumn(0).setPreferredWidth(110);
		peopleTable.getColumnModel().getColumn(1).setPreferredWidth(600);
	}

	/** A tab with the id and the description of the records. */
	private JPanel createRecordsTab(final List<FLEFRecord> records){
		final List<FLEFRecord> opened = List.copyOf(records);
		final List<Object[]> rows = new ArrayList<>(opened.size());
		for(final FLEFRecord record : opened)
			rows.add(new Object[]{record.getId(), RecordDisplay.describe(record, model)});
		final JTable table = ToolDialogs.createTable(new RowTableModel(new String[]{I18N.t("column.id"), I18N.t("column.record")}, rows),
			row -> open(opened, row));
		table.getColumnModel().getColumn(0).setPreferredWidth(110);
		table.getColumnModel().getColumn(1).setPreferredWidth(600);

		final JPanel tab = new JPanel(new BorderLayout());
		tab.add(new JScrollPane(table), BorderLayout.CENTER);
		return tab;
	}

	/** A table whose model is replaced as the selection changes: the sorter that the factory installs would stay bound to the first model. */
	private static JTable createPlainTable(final List<Object[]> rows, final IntConsumer onOpen){
		final JTable table = ToolDialogs.createTable(new RowTableModel(new String[]{I18N.t("column.id"), I18N.t("column.record")}, rows),
			onOpen);
		table.setAutoCreateRowSorter(false);
		table.setRowSorter(null);
		return table;
	}

	private void open(final List<FLEFRecord> records, final int row){
		if(row < 0 || row >= records.size())
			return;

		ToolDialogs.openRecordEditor(this, model, records.get(row));

		//what was edited can change the groups, the title, the citations
		reloadTree();
	}

}
