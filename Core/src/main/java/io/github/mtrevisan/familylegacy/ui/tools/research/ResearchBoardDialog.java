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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.components.lists.PrivacyBadge;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDialogs;
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
 * Single panel that shows the whole research workflow as a tree:
 * {@code question -> activity -> task}, with the conclusions of each question next to its activities.
 * <p>
 * The tree is built by {@link ResearchTreeBuilder}. Selecting a node shows the details of its record on the right;
 * double-clicking it (or pressing <em>Edit</em>) opens the usual record editor, and the tree is rebuilt after a save,
 * keeping the selection. The filter field keeps only the branches whose text matches.
 * <p>
 * The dialog does not replace the management dialogs of the {@code Research} menu: those remain the tabular way to
 * browse and delete records of one type.
 */
public final class ResearchBoardDialog extends JDialog{

	private final ToolContext context;
	private final JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode()));
	private final JTextArea details = new JTextArea();
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);


	public ResearchBoardDialog(final ToolContext context){
		super(context.owner(), "Research Board", ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createContent(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1200, 640));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel("Filter:"));
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
		newMenu.add(newItem("Research question", ResearchTreeBuilder.Kind.QUESTION));
		newMenu.add(newItem("Research activity", ResearchTreeBuilder.Kind.ACTIVITY));
		newMenu.add(newItem("Research task", ResearchTreeBuilder.Kind.TASK));
		newMenu.add(newItem("Conclusion", ResearchTreeBuilder.Kind.CONCLUSION));
		newButton.addActionListener(e -> newMenu.show(newButton, 0, newButton.getHeight()));
		toolbar.add(newButton);

		final JButton editButton = new JButton(I18N.t("button.edit"));
		editButton.addActionListener(e -> openEditor(selectedNode()));
		toolbar.add(editButton);

		final JButton expandButton = new JButton("Expand all");
		expandButton.addActionListener(e -> expandAll());
		toolbar.add(expandButton);

		final JButton collapseButton = new JButton("Collapse all");
		collapseButton.addActionListener(e -> collapseAll());
		toolbar.add(collapseButton);

		return toolbar;
	}

	private JMenuItem newItem(final String text, final ResearchTreeBuilder.Kind kind){
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
		treeScroll.setBorder(BorderFactory.createTitledBorder("Questions, activities, tasks, conclusions"));
		final JScrollPane detailScroll = new JScrollPane(details);
		detailScroll.setBorder(BorderFactory.createTitledBorder("Details"));

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
		final ResearchTreeBuilder.Node selected = selectedNode();
		final String selectedId = (selected != null && selected.record() != null? selected.record().getId(): null);

		final FLEFModel model = context.model();
		final String needle = searchField.getText();

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		int questions = 0;
		for(final ResearchTreeBuilder.Node node : ResearchTreeBuilder.build(model)){
			final ResearchTreeBuilder.Node filtered = node.filter(needle);
			if(filtered == null)
				continue;

			root.add(toTreeNode(filtered));
			if(filtered.kind() == ResearchTreeBuilder.Kind.QUESTION)
				questions ++;
		}
		tree.setModel(new DefaultTreeModel(root));
		expandAll();
		if(selectedId != null)
			select(root, selectedId);

		statusLabel.setText(questions + (questions == 1? " question": " questions"));
		showDetails(selectedNode());
	}

	private static DefaultMutableTreeNode toTreeNode(final ResearchTreeBuilder.Node node){
		final DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
		for(final ResearchTreeBuilder.Node child : node.children())
			treeNode.add(toTreeNode(child));
		return treeNode;
	}

	private void select(final DefaultMutableTreeNode root, final String recordId){
		final Enumeration<?> nodes = root.preorderEnumeration();
		while(nodes.hasMoreElements()){
			final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)nodes.nextElement();
			if(treeNode.getUserObject() instanceof ResearchTreeBuilder.Node node
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

	private ResearchTreeBuilder.Node selectedNode(){
		final TreePath path = tree.getSelectionPath();
		if(path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode treeNode
				&& treeNode.getUserObject() instanceof ResearchTreeBuilder.Node node)
			return node;
		return null;
	}


	/* ======================================================================
	 *                          Editing
	 * ====================================================================== */

	private void openEditor(final ResearchTreeBuilder.Node node){
		if(node != null && node.record() != null)
			openEditor(node.kind(), node.record());
	}

	private void openEditor(final ResearchTreeBuilder.Kind kind, final FLEFRecord record){
		final FLEFModel model = context.model();
		final BaseRecordDialog dialog = switch(kind){
			case QUESTION -> (record == null
				? ResearchQuestionHandler.getInstance().createNewDialog(this, model)
				: ResearchQuestionHandler.getInstance().createEditDialog(this, model, record));
			case ACTIVITY -> (record == null
				? ResearchActivityHandler.getInstance().createNewDialog(this, model)
				: ResearchActivityHandler.getInstance().createEditDialog(this, model, record));
			case TASK -> (record == null
				? ResearchTaskHandler.getInstance().createNewDialog(this, model)
				: ResearchTaskHandler.getInstance().createEditDialog(this, model, record));
			case CONCLUSION -> (record == null
				? ConclusionHandler.getInstance().createNewDialog(this, model)
				: ConclusionHandler.getInstance().createEditDialog(this, model, record));
			case ORPHANS -> null;
		};
		if(dialog == null)
			return;

		dialog.setVisible(true);

		if(dialog.isSaved())
			reload();
	}


	/* ======================================================================
	 *                          Details
	 * ====================================================================== */

	private void showDetails(final ResearchTreeBuilder.Node node){
		details.setText(describe(node));
		details.setCaretPosition(0);
	}

	private static String describe(final ResearchTreeBuilder.Node node){
		if(node == null)
			return StringUtils.EMPTY;
		final FLEFRecord record = node.record();
		if(record == null)
			return "Records that reference no existing research question.";

		final StringBuilder sb = new StringBuilder();
		switch(node.kind()){
			case QUESTION -> {
				append(sb, "Question", ResearchHelper.questionTitle(record));
				append(sb, "Text", ResearchHelper.questionText(record));
				append(sb, "Status", ResearchHelper.status(record));
				append(sb, "Rationale", ResearchHelper.firstTextValue(record, ResearchQuestionReader.TAG_RATIONALE));
				append(sb, "Closed on", ResearchHelper.firstTextValue(record, ResearchQuestionReader.TAG_CLOSED_DATE));
				append(sb, "Conclusion", ResearchHelper.firstTextValue(record, ResearchQuestionReader.TAG_CONCLUSION));
				append(sb, "Confidence", ResearchHelper.firstTextValue(record, ResearchQuestionReader.TAG_CONCLUSION_CONFIDENCE));
				append(sb, "Activities", Integer.toString(node.count(ResearchTreeBuilder.Kind.ACTIVITY)));
				append(sb, "Tasks", Integer.toString(node.count(ResearchTreeBuilder.Kind.TASK)));
				append(sb, "Conclusions", Integer.toString(node.count(ResearchTreeBuilder.Kind.CONCLUSION)));
			}
			case ACTIVITY -> {
				append(sb, "Type", ResearchHelper.activityType(record));
				append(sb, "Action", ResearchHelper.action(record));
				append(sb, "Status", ResearchHelper.status(record));
				append(sb, "Result", ResearchHelper.activityResult(record));
				append(sb, "Observation", ResearchHelper.firstTextValue(record, ResearchActivityReader.TAG_OBSERVATION));
				append(sb, "Conclusion", ResearchHelper.firstTextValue(record, ResearchActivityReader.TAG_CONCLUSION));
				append(sb, "Confidence", ResearchHelper.firstTextValue(record, ResearchActivityReader.TAG_CONCLUSION_CONFIDENCE));
				append(sb, "Tasks created", Integer.toString(node.count(ResearchTreeBuilder.Kind.TASK)));
			}
			case TASK -> {
				append(sb, "Description", ResearchHelper.taskDescription(record));
				append(sb, "Status", ResearchHelper.status(record));
				append(sb, "Priority", ResearchHelper.taskPriority(record));
				append(sb, "Due", ResearchHelper.firstTextValue(record, ResearchTaskReader.TAG_DUE_DATE));
				append(sb, "Outcome", ResearchHelper.firstTextValue(record, ResearchTaskReader.TAG_OUTCOME));
			}
			case CONCLUSION -> {
				append(sb, "Issue", ResearchHelper.conclusionIssue(record));
				append(sb, "Proof status", ResearchHelper.proofStatus(record));
				append(sb, "Narrative", ResearchHelper.firstTextValue(record, ConclusionReader.TAG_NARRATIVE));
			}
			case ORPHANS -> {}
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

			if(value instanceof DefaultMutableTreeNode treeNode && treeNode.getUserObject() instanceof ResearchTreeBuilder.Node node){
				setText("[" + node.kind().symbol() + "] " + node.label());
				//a padlock signals a restricted or confidential record
				setIcon(PrivacyBadge.of(PrivacyService.resolver(model)
					.describe(node.record())));
			}
			return this;
		}

	}

}
