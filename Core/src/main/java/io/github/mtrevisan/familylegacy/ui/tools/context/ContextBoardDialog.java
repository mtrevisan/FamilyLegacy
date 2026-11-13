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
package io.github.mtrevisan.familylegacy.ui.tools.context;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.events.EventSummary;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.research.ContextTreeBuilder;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.lists.PrivacyBadge;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ContextImpactRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Locale;
import java.util.function.Predicate;


/**
 * Single panel that shows the historical and cultural context as a tree: historic events and cultural norms, each with the
 * impacts it has on the records it influences.
 * <p>
 * The tree is built by {@link ContextTreeBuilder}. Selecting a node shows the details of its record on the right;
 * double-clicking it (or pressing <em>Edit</em>) opens the usual record editor, and the tree is rebuilt after a save,
 * keeping the selection. With a context (or one of its impacts) selected, <em>New &gt; Impact of the selected context</em>
 * opens a new impact whose context is already filled. The filter field keeps only the branches whose text matches.
 */
public final class ContextBoardDialog extends JDialog{

	private final ToolContext context;
	private final JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode()));
	private final JTextArea details = new JTextArea();
	private final SearchBox searchBox = new SearchBox(24, false);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);


	public ContextBoardDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.context_board.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createContent(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1100, 640));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel(I18N.l("label.filter")));
		toolbar.add(searchBox);
		searchBox.onChange(this::reload);

		final JButton newButton = new JButton(I18N.t("button.new"));
		final JPopupMenu newMenu = new JPopupMenu();
		newMenu.add(newItem(I18N.t("confirmation.exist_record.historic_event"), ContextTreeBuilder.Kind.HISTORIC_EVENT));
		newMenu.add(newItem(I18N.t("confirmation.exist_record.cultural_norm"), ContextTreeBuilder.Kind.CULTURAL_NORM));
		newMenu.add(newItem(I18N.t("confirmation.exist_record.context_impact"), ContextTreeBuilder.Kind.IMPACT));
		final JMenuItem selectedContextItem = new JMenuItem(I18N.t("menu.item.selected_impact"));
		selectedContextItem.addActionListener(e -> openNewImpact(selectedContext()));
		newMenu.add(selectedContextItem);
		newButton.addActionListener(e -> {
			selectedContextItem.setEnabled(selectedContext() != null);
			newMenu.show(newButton, 0, newButton.getHeight());
		});
		toolbar.add(newButton);

		final JButton editButton = new JButton(I18N.t("button.edit"));
		editButton.addActionListener(e -> openEditor(selectedNode()));
		toolbar.add(editButton);

		final JButton deleteButton = new JButton(I18N.t("button.delete"));
		deleteButton.addActionListener(e -> deleteSelected());
		toolbar.add(deleteButton);

		final JButton expandButton = new JButton(I18N.t("button.expand_all"));
		expandButton.addActionListener(e -> expandAll());
		toolbar.add(expandButton);

		final JButton collapseButton = new JButton(I18N.t("button.collapse_all"));
		collapseButton.addActionListener(e -> collapseAll());
		toolbar.add(collapseButton);

		return toolbar;
	}

	private JMenuItem newItem(final String text, final ContextTreeBuilder.Kind kind){
		final JMenuItem item = new JMenuItem(text);
		item.addActionListener(e -> openEditor(kind, null));
		return item;
	}

	private JSplitPane createContent(){
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.getSelectionModel()
			.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new NodeRenderer(context.model()));
		tree.addTreeSelectionListener(e -> showDetails(selectedNode()));
		tree.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedNode());
			}
		});

		details.setEditable(false);
		details.setLineWrap(true);
		details.setWrapStyleWord(true);
		details.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		final JScrollPane treeScroll = new JScrollPane(tree);
		treeScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.context_tree")));
		final JScrollPane detailScroll = new JScrollPane(details);
		detailScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.details")));

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailScroll);
		split.setResizeWeight(0.65);
		return split;
	}

	private JPanel createFooter(){
		return GUIHelper.createStatusFooter(statusLabel);
	}


	/**
	 * Rebuilds the tree from the model, applying the filter and keeping the selected record selected when it is still
	 * visible.
	 */
	private void reload(){
		final ContextTreeBuilder.Node selected = selectedNode();
		final String selectedId = (selected != null && selected.record() != null? selected.record().getId(): null);

		final FLEFModel model = context.model();
		final Predicate<String> matcher = searchBox.predicate();

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		int contexts = 0;
		for(final ContextTreeBuilder.Node node : ContextTreeBuilder.build(model)){
			final ContextTreeBuilder.Node filtered = (searchBox.isBlank()? node: node.filter(matcher));
			if(filtered == null)
				continue;

			root.add(toTreeNode(filtered));
			for(final ContextTreeBuilder.Node child : filtered.children())
				if(child.kind().isContext())
					contexts ++;
		}
		tree.setModel(new DefaultTreeModel(root));
		expandAll();
		if(selectedId != null)
			select(root, selectedId);

		statusLabel.setText(I18N.tf("status.contexts", contexts));
		showDetails(selectedNode());
	}

	private static DefaultMutableTreeNode toTreeNode(final ContextTreeBuilder.Node node){
		final DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
		for(final ContextTreeBuilder.Node child : node.children())
			treeNode.add(toTreeNode(child));
		return treeNode;
	}

	private void select(final DefaultMutableTreeNode root, final String recordId){
		final Enumeration<?> nodes = root.preorderEnumeration();
		while(nodes.hasMoreElements()){
			final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)nodes.nextElement();
			if(treeNode.getUserObject() instanceof ContextTreeBuilder.Node node
					&& node.record() != null && recordId.equals(node.record().getId())){
				final TreePath path = new TreePath(treeNode.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				return;
			}
		}
	}

	private void expandAll(){
		for(int row = 0; row < tree.getRowCount(); row ++)
			tree.expandRow(row);
	}

	private void collapseAll(){
		for(int row = tree.getRowCount() - 1; row >= 0; row --)
			tree.collapseRow(row);
	}

	private ContextTreeBuilder.Node selectedNode(){
		final TreePath path = tree.getSelectionPath();
		if(path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode treeNode
				&& treeNode.getUserObject() instanceof ContextTreeBuilder.Node node)
			return node;
		return null;
	}

	/** The selected context, or the context of the selected impact; {@code null} if neither is selected. */
	private FLEFRecord selectedContext(){
		final TreePath path = tree.getSelectionPath();
		if(path == null)
			return null;

		for(int i = path.getPathCount() - 1; i >= 0; i --)
			if(path.getPathComponent(i) instanceof DefaultMutableTreeNode treeNode
					&& treeNode.getUserObject() instanceof ContextTreeBuilder.Node node && node.kind().isContext())
				return node.record();
		return null;
	}


	/* ======================================================================
	 *                          Editing
	 * ====================================================================== */

	private void openEditor(final ContextTreeBuilder.Node node){
		if(node != null && node.record() != null)
			openEditor(node.kind(), node.record());
	}

	private void openEditor(final ContextTreeBuilder.Kind kind, final FLEFRecord record){
		final FLEFModel model = context.model();
		final RecordDialog dialog = switch(kind){
			case HISTORIC_EVENT -> (record == null
				? HistoricEventHandler.getInstance().createNewDialog(this, model)
				: HistoricEventHandler.getInstance().createEditDialog(this, model, record));
			case CULTURAL_NORM -> (record == null
				? CulturalNormHandler.getInstance().createNewDialog(this, model)
				: CulturalNormHandler.getInstance().createEditDialog(this, model, record));
			case IMPACT -> (record == null
				? ContextImpactHandler.getInstance().createNewDialog(this, model)
				: ContextImpactHandler.getInstance().createEditDialog(this, model, record));
			case HISTORIC_EVENTS, CULTURAL_NORMS, ORPHANS -> null;
		};
		show(dialog);
	}

	private void openNewImpact(final FLEFRecord contextRecord){
		if(contextRecord != null)
			show(ContextImpactRecordDialog.createNew(this, context.model())
				.withContext(contextRecord));
	}

	private void deleteSelected(){
		final ContextTreeBuilder.Node node = selectedNode();
		if(node == null || node.record() == null)
			return;

		final FLEFRecord record = node.record();
		String message = I18N.tf("confirmation.delete.context_record.message",
			I18N.t("confirmation.exist_record." + record.getTag().toLowerCase(Locale.ROOT)), node.label());
		//the impacts of a context are not deleted with it: they are left under "(no context)"
		if(node.kind().isContext() && !node.children().isEmpty())
			message += "\n\n" + I18N.tf("confirmation.delete.context_record.orphaned", node.children().size());
		final int confirm = JOptionPane.showConfirmDialog(this, message, I18N.t("confirmation.delete.record.title"),
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		context.model().removeRecord(record.getId());

		reload();
	}

	private void show(final RecordDialog dialog){
		if(dialog == null)
			return;

		dialog.setVisible(true);

		if(dialog.isSaved())
			reload();
	}


	/* ======================================================================
	 *                          Details
	 * ====================================================================== */

	private void showDetails(final ContextTreeBuilder.Node node){
		details.setText(describe(node));
		details.setCaretPosition(0);
	}

	private String describe(final ContextTreeBuilder.Node node){
		if(node == null)
			return (tree.getRowCount() == 0 && searchBox.isBlank()? I18N.t("text.context.empty"): StringUtils.EMPTY);
		final FLEFRecord record = node.record();
		if(record == null)
			return switch(node.kind()){
				case ORPHANS -> I18N.t("text.context.description.orphans");
				case HISTORIC_EVENTS -> I18N.t("text.context.description.historic_events");
				default -> I18N.t("text.context.description.cultural_norms");
			};

		final FLEFModel model = context.model();

		final StringBuilder sb = new StringBuilder();
		switch(node.kind()){
			case HISTORIC_EVENT -> {
				append(sb, "title", HistoricEventReader.extractTitle(record));
				append(sb, "type", HistoricEventReader.extractType(record));
				append(sb, "date", EventHelper.dateText(record, HistoricEventReader.TAG_DATE));
				append(sb, "place", EventSummary.placeName(record, model));
				append(sb, "confidence", ResearchHelper.firstTextValue(record, HistoricEventReader.TAG_CONFIDENCE));
				appendImpacts(sb, node);
			}
			case CULTURAL_NORM -> {
				append(sb, "title", CulturalNormReader.extractTitle(record));
				append(sb, "type", CulturalNormReader.extractType(record));
				append(sb, "place", EventSummary.placeName(record, model));
				append(sb, "valid_from", EventHelper.dateText(record, CulturalNormReader.TAG_VALID_FROM));
				append(sb, "valid_to", EventHelper.dateText(record, CulturalNormReader.TAG_VALID_TO));
				append(sb, "confidence", ResearchHelper.firstTextValue(record, CulturalNormReader.TAG_CONFIDENCE));
				appendImpacts(sb, node);
			}
			case IMPACT -> {
				append(sb, "impact", node.label());
				append(sb, "impact_type", ContextImpactReader.extractImpactType(record));
				append(sb, "rationale", ResearchHelper.firstTextValue(record, ContextImpactReader.TAG_RATIONALE));
				append(sb, "confidence", ResearchHelper.firstTextValue(record, ContextImpactReader.TAG_CONFIDENCE));
			}
			case HISTORIC_EVENTS, CULTURAL_NORMS, ORPHANS -> {}
		}
		append(sb, "id", record.getId());
		return sb.toString();
	}

	/** The records a context acts on, one per line: what the user wants to know of a context besides its own data. */
	private static void appendImpacts(final StringBuilder sb, final ContextTreeBuilder.Node node){
		if(node.children().isEmpty())
			return;

		sb.append(I18N.l("text.context.field.impacts"))
			.append(StringUtils.LF);
		for(final ContextTreeBuilder.Node impact : node.children())
			sb.append("\u2022 ")
				.append(impact.label())
				.append(StringUtils.LF);
		sb.append(StringUtils.LF);
	}

	private static void append(final StringBuilder sb, final String field, final String value){
		if(StringUtils.isNotBlank(value))
			sb.append(I18N.l("text.context.field." + field))
				.append(": ")
				.append(value)
				.append("\n\n");
	}


	private static final class NodeRenderer extends DefaultTreeCellRenderer{

		private final FLEFModel model;


		NodeRenderer(final FLEFModel model){
			this.model = model;
		}


		@Override
		public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
				final boolean expanded, final boolean leaf, final int row, final boolean hasFocus){
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

			if(value instanceof DefaultMutableTreeNode treeNode && treeNode.getUserObject() instanceof ContextTreeBuilder.Node node){
				setText(node.label());
				//a padlock signals a restricted or confidential record
				setIcon(PrivacyBadge.of(PrivacyService.resolver(model)
					.describe(node.record())));
			}
			return this;
		}

	}

}
