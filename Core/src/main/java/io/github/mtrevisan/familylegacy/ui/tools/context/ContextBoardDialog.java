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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.components.lists.PrivacyBadge;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ContextImpactRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);


	public ContextBoardDialog(final ToolContext context){
		super(context.owner(), "Context Board", ModalityType.APPLICATION_MODAL);

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
		toolbar.add(searchField);
		searchField.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					reload();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					reload();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					reload();
				}
			});

		final JButton newButton = new JButton(I18N.t("button.new"));
		final JPopupMenu newMenu = new JPopupMenu();
		newMenu.add(newItem("Historic event", ContextTreeBuilder.Kind.HISTORIC_EVENT));
		newMenu.add(newItem("Cultural norm", ContextTreeBuilder.Kind.CULTURAL_NORM));
		newMenu.add(newItem("Context impact", ContextTreeBuilder.Kind.IMPACT));
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
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		footer.add(statusLabel);
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());
		footer.add(close);
		return footer;
	}


	/**
	 * Rebuilds the tree from the model, applying the filter and keeping the selected record selected when it is still
	 * visible.
	 */
	private void reload(){
		final ContextTreeBuilder.Node selected = selectedNode();
		final String selectedId = (selected != null && selected.record() != null? selected.record().getId(): null);

		final FLEFModel model = context.model();
		final String needle = searchField.getText();

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		int contexts = 0;
		for(final ContextTreeBuilder.Node node : ContextTreeBuilder.build(model)){
			final ContextTreeBuilder.Node filtered = node.filter(needle);
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

		statusLabel.setText(contexts + (contexts == 1? " context": " contexts"));
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

	private static String describe(final ContextTreeBuilder.Node node){
		if(node == null)
			return StringUtils.EMPTY;
		final FLEFRecord record = node.record();
		if(record == null)
			return switch(node.kind()){
				case ORPHANS -> "Context impacts that cite no existing historic event or cultural norm.";
				case HISTORIC_EVENTS -> "Events of the past that may explain or influence what the records document.";
				default -> "Customs, laws, and rules that may explain or constrain what the records document.";
			};

		final StringBuilder sb = new StringBuilder();
		switch(node.kind()){
			case HISTORIC_EVENT -> {
				append(sb, "Title", HistoricEventReader.extractTitle(record));
				append(sb, "Type", HistoricEventReader.extractType(record));
				append(sb, "Date", ResearchHelper.firstTextValue(record, HistoricEventReader.TAG_DATE));
				append(sb, "Place", ResearchHelper.firstTextValue(record, HistoricEventReader.TAG_PLACE));
				append(sb, "Confidence", ResearchHelper.firstTextValue(record, HistoricEventReader.TAG_CONFIDENCE));
				append(sb, "Impacts", Integer.toString(node.children().size()));
			}
			case CULTURAL_NORM -> {
				append(sb, "Title", CulturalNormReader.extractTitle(record));
				append(sb, "Type", CulturalNormReader.extractType(record));
				append(sb, "Place", ResearchHelper.firstTextValue(record, CulturalNormReader.TAG_PLACE));
				append(sb, "Valid from", ResearchHelper.firstTextValue(record, CulturalNormReader.TAG_VALID_FROM));
				append(sb, "Valid to", ResearchHelper.firstTextValue(record, CulturalNormReader.TAG_VALID_TO));
				append(sb, "Confidence", ResearchHelper.firstTextValue(record, CulturalNormReader.TAG_CONFIDENCE));
				append(sb, "Impacts", Integer.toString(node.children().size()));
			}
			case IMPACT -> {
				append(sb, "Impact", node.label());
				append(sb, "Impact type", ContextImpactReader.extractImpactType(record));
				append(sb, "Rationale", ResearchHelper.firstTextValue(record, ContextImpactReader.TAG_RATIONALE));
				append(sb, "Confidence", ResearchHelper.firstTextValue(record, ContextImpactReader.TAG_CONFIDENCE));
			}
			case HISTORIC_EVENTS, CULTURAL_NORMS, ORPHANS -> {}
		}
		append(sb, "Id", record.getId());
		return sb.toString();
	}

	private static void append(final StringBuilder sb, final String name, final String value){
		if(StringUtils.isNotBlank(value))
			sb.append(name)
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
				setText("[" + node.kind().symbol() + "] " + node.label());
				//a padlock signals a restricted or confidential record
				setIcon(PrivacyBadge.of(PrivacyService.resolver(model)
					.describe(node.record())));
			}
			return this;
		}

	}

}
