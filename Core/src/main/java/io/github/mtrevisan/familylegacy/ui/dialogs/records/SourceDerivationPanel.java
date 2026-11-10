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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.sources.SourceDerivationTree;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 * The tree of the derivations of a source: above, the sources it is made from up to the originals, below, the sources made from it.
 * It is read-only, and it is shown only when there are enough related sources ({@link SourceDerivationTree#MIN_RELATED_SOURCES}),
 * because with one or two the <em>Derived from</em> list is all that is needed. Double-clicking a source opens it.
 */
final class SourceDerivationPanel extends JPanel{

	private static final Color CONTRADICTS = new Color(0xB00020);

	private final FLEFModel model;
	private final JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode()));


	SourceDerivationPanel(final FLEFModel model){
		super(new BorderLayout());

		this.model = model;

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new Renderer());
		tree.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2)
					openSource(tree.getPathForLocation(e.getX(), e.getY()));
			}
		});

		final JScrollPane scroll = new JScrollPane(tree);
		scroll.setPreferredSize(new Dimension(400, 160));
		add(scroll, BorderLayout.CENTER);
		setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.source.derivation")));
		setVisible(false);
	}


	/** Builds the tree of the source, and shows the panel only if the tree is worth it. */
	void load(final String sourceId){
		final SourceDerivationTree derivations = new SourceDerivationTree(model);
		final boolean worth = (sourceId != null && derivations.isWorthShowing(sourceId));
		if(worth){
			final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			addGroup(root, I18N.t("dialog.source.derivation.ancestors"), derivations.ancestors(sourceId));
			addGroup(root, I18N.t("dialog.source.derivation.descendants"), derivations.descendants(sourceId));
			tree.setModel(new DefaultTreeModel(root));
			for(int row = 0; row < tree.getRowCount(); row ++)
				tree.expandRow(row);
		}
		setVisible(worth);
	}

	/** @return	The tree shown, for the tests. */
	JTree getTree(){
		return tree;
	}


	private static void addGroup(final DefaultMutableTreeNode root, final String label, final List<SourceDerivationTree.Node> nodes){
		if(nodes.isEmpty())
			return;

		final DefaultMutableTreeNode group = new DefaultMutableTreeNode(label);
		for(final SourceDerivationTree.Node node : nodes)
			group.add(toTreeNode(node));
		root.add(group);
	}

	private static DefaultMutableTreeNode toTreeNode(final SourceDerivationTree.Node node){
		final DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
		for(final SourceDerivationTree.Node child : node.children())
			treeNode.add(toTreeNode(child));
		return treeNode;
	}

	private void openSource(final TreePath path){
		if(path == null || !(((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject() instanceof SourceDerivationTree.Node node))
			return;

		final FLEFRecord source = model.getRecordById(node.id());
		if(source == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		SourceHandler.getInstance().createEditDialog(owner, model, source)
			.setVisible(true);
	}


	/** The text of a node of the tree. */
	static String describe(final SourceDerivationTree.Node node){
		final StringBuilder text = new StringBuilder(node.title());
		if(!node.title().equals(node.id()))
			text.append(" [").append(node.id()).append(']');
		if(node.locator() != null)
			text.append(" – ").append(node.locator());
		if(node.contradicts())
			text.append(" (").append(I18N.t("dialog.source.derivation.contradicts")).append(')');
		if(node.cycle())
			text.append(" (").append(I18N.t("dialog.source.derivation.cycle")).append(')');
		return text.toString();
	}


	private static final class Renderer extends DefaultTreeCellRenderer{

		@Override
		public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected, final boolean expanded,
				final boolean leaf, final int row, final boolean hasFocus){
			final Object object = ((DefaultMutableTreeNode)value).getUserObject();
			final String text = (object instanceof SourceDerivationTree.Node node? describe(node): String.valueOf(object));
			final JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, text, selected, expanded, leaf, row, hasFocus);
			if(!selected && object instanceof SourceDerivationTree.Node node && (node.contradicts() || node.cycle()))
				label.setForeground(CONTRADICTS);
			return label;
		}

	}

}
