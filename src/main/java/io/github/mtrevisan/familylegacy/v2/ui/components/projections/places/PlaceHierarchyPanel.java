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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.places;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;


/**
 * Panel that presents the place hierarchy as a navigable tree.
 * <p>
 * The hierarchy is read by {@link PlaceHierarchyService} and rendered as
 * a {@link JTree}. Because the underlying data is a DAG (a place can be
 * administratively part of one entity and ecclesiastically part of
 * another), a place can appear under several parents: the tree
 * duplicates it as needed and marks the duplication in the renderer.
 * Cycles, which the protocol discourages but does not forbid, are broken
 * at the first repeated node and flagged in the same way.
 * <p>
 * <b>Orphan places.</b> Places with no declared hierarchy are shown as
 * roots without children, alongside the places that are genuinely at the
 * top of a hierarchy. There is no separate "unclassified" section: an
 * orphan place is a legitimate top-level entry, not an error state.
 * <p>
 * <b>Filtering.</b> The toolbar offers a search field (case-insensitive
 * substring on the place name) and a combo box to restrict the tree to a
 * single relation type (administrative, ecclesiastical, judicial,
 * cadastral, geographic). When the combo is set to "All", every relation
 * is shown.
 * <p>
 * <b>Details.</b> A side panel shows the full information of the
 * selected place: type, coordinates, incoming relations (parents), and
 * outgoing relations (children), each with its type and validity dates.
 * A double-click opens the record editor through
 * {@link PlaceHandler#createEditDialog(Window, FLEFModel, FLEFRecord)}.
 */
public final class PlaceHierarchyPanel extends JPanel{

	/** Label used in the relation-type combo to mean "no filter". */
	private static final String FILTER_ALL = "All";

	private static final int TREE_MIN_WIDTH = 360;
	private static final int DETAILS_MIN_WIDTH = 300;


	private final FLEFModel model;

	private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Places");
	private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
	private final JTree tree = new JTree(treeModel);

	private final JTextField searchField = new JTextField(20);
	private final JComboBox<String> relationTypeFilter = new JComboBox<>();
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);

	private PlaceDetailsPanel detailsPanel;

	private PlaceHierarchyService.Hierarchy hierarchy;


	public PlaceHierarchyPanel(final FLEFModel model){
		this.model = Objects.requireNonNull(model, "model must not be null");

		setLayout(new BorderLayout());

		add(createToolbar(), BorderLayout.NORTH);
		add(createCenter(), BorderLayout.CENTER);
		add(createStatusBar(), BorderLayout.SOUTH);

		reload();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Re-reads the hierarchy from the model and rebuilds the tree. Called
	 * on construction and whenever the underlying model has changed. The
	 * currently selected place, the search text, and the relation-type
	 * filter are preserved across the reload.
	 */
	public void reload(){
		final String previousSelection = selectedPlaceId();
		this.hierarchy = PlaceHierarchyService.load(model);
		rebuildRelationTypeModel();
		rebuildTree();
		restoreSelection(previousSelection);
		updateStatus();
	}


	/* ======================================================================
	 *                          UI composition
	 * ====================================================================== */

	private JComponent createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel("Search:"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ rebuildTree(); }
			@Override public void removeUpdate(final DocumentEvent e){ rebuildTree(); }
			@Override public void changedUpdate(final DocumentEvent e){ rebuildTree(); }
		});

		toolbar.add(new JLabel("Relation:"));
		relationTypeFilter.setPreferredSize(new Dimension(180, relationTypeFilter.getPreferredSize().height));
		relationTypeFilter.addActionListener(e -> rebuildTree());
		toolbar.add(relationTypeFilter);

		final JButton expandAll = new JButton("Expand all");
		expandAll.addActionListener(e -> expandAll());
		toolbar.add(expandAll);

		final JButton collapseAll = new JButton("Collapse all");
		collapseAll.addActionListener(e -> collapseAll());
		toolbar.add(collapseAll);

		return toolbar;
	}

	private JComponent createCenter(){
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new PlaceTreeCellRenderer());
		tree.addTreeSelectionListener(e -> onTreeSelectionChanged());
		tree.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditorForSelection();
			}
		});

		final JScrollPane treeScroll = new JScrollPane(tree);
		treeScroll.setMinimumSize(new Dimension(TREE_MIN_WIDTH, 200));

		detailsPanel = new PlaceDetailsPanel(model);
		final JScrollPane detailsScroll = new JScrollPane(detailsPanel);
		detailsScroll.setMinimumSize(new Dimension(DETAILS_MIN_WIDTH, 200));
		detailsScroll.setBorder(BorderFactory.createEmptyBorder());

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			treeScroll, detailsScroll);
		split.setResizeWeight(0.62);
		split.setDividerLocation(0.62);
		split.setContinuousLayout(true);
		return split;
	}

	private JComponent createStatusBar(){
		final JPanel bar = new JPanel(new BorderLayout());
		bar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
		bar.add(statusLabel, BorderLayout.WEST);
		return bar;
	}


	/* ======================================================================
	 *                          Tree construction
	 * ====================================================================== */

	private void rebuildRelationTypeModel(){
		// Start from the declared types, add any custom type found in the
		// data, and prepend the "All" entry.
		final Set<String> types = new TreeSet<>(PlaceHierarchyService.DECLARED_RELATION_TYPES);
		for(final PlaceHierarchyService.PlaceRelation rel : hierarchy.relations())
			if(rel.relationType() != null && !rel.relationType().isBlank())
				types.add(rel.relationType());

		final String previous = (String)relationTypeFilter.getSelectedItem();
		final List<String> items = new ArrayList<>();
		items.add(FILTER_ALL);
		items.addAll(types);
		relationTypeFilter.setModel(new DefaultComboBoxModel<>(items.toArray(new String[0])));
		if(previous != null && items.contains(previous))
			relationTypeFilter.setSelectedItem(previous);
		else
			relationTypeFilter.setSelectedItem(FILTER_ALL);
	}

	private void rebuildTree(){
		treeRoot.removeAllChildren();

		final String filter = (String)relationTypeFilter.getSelectedItem();
		final String search = searchField.getText();
		final boolean hasFilter = (filter != null && !FILTER_ALL.equals(filter));

		for(final String rootId : hierarchy.rootIds()){
			final DefaultMutableTreeNode node = buildNode(rootId, null,
				new HashSet<>(), hasFilter, filter, search);
			if(node != null)
				treeRoot.add(node);
		}

		treeModel.reload();
		// Expand the first two levels by default, so the panel does not
		// open with everything collapsed.
		expandFirstLevels(2);
	}

	/**
	 * Builds a {@link DefaultMutableTreeNode} for the given place and,
	 * recursively, for its children. The {@code path} set contains the
	 * places already visited on the current path from the root, so a
	 * cycle is broken at the first repeated node and the node is marked
	 * as a cycle for the renderer.
	 */
	private DefaultMutableTreeNode buildNode(final String placeId,
		final PlaceHierarchyService.PlaceRelation incoming,
		final Set<String> path,
		final boolean hasFilter,
		final String filter,
		final String search){
		final PlaceHierarchyService.PlaceReference place = hierarchy.places().get(placeId);
		if(place == null)
			return null;

		final boolean isCycle = path.contains(placeId);
		final PlaceTreePayload payload = new PlaceTreePayload(place, incoming, isCycle);
		final DefaultMutableTreeNode node = new DefaultMutableTreeNode(payload);

		if(isCycle)
			return node;

		final Set<String> newPath = new HashSet<>(path);
		newPath.add(placeId);

		for(final PlaceHierarchyService.PlaceRelation rel : hierarchy.childrenOf(placeId)){
			if(hasFilter && !filter.equals(rel.relationType()))
				continue;
			final DefaultMutableTreeNode child = buildNode(rel.childId(), rel, newPath,
				hasFilter, filter, search);
			if(child != null)
				node.add(child);
		}

		// When a search string is active and this node does not match, we
		// still keep it if any of its children match. If neither matches,
		// the node is dropped (unless it is a root, which is always kept
		// to preserve the top-level structure).
		if(search != null && !search.isBlank() && incoming != null){
			if(!matches(place, search) && node.isLeaf())
				return null;
		}

		return node;
	}

	private static boolean matches(final PlaceHierarchyService.PlaceReference place, final String search){
		final String needle = search.trim().toLowerCase(java.util.Locale.ROOT);
		if(needle.isEmpty())
			return true;
		if(place.name() != null && place.name().toLowerCase(java.util.Locale.ROOT).contains(needle))
			return true;
		if(place.type() != null && place.type().toLowerCase(java.util.Locale.ROOT).contains(needle))
			return true;
		return false;
	}

	private void expandFirstLevels(final int levels){
		int row = 0;
		while(row < tree.getRowCount() && tree.getPathForRow(row).getPathCount() <= levels){
			tree.expandRow(row);
			row++;
		}
	}

	private void expandAll(){
		int row = 0;
		while(row < tree.getRowCount()){
			tree.expandRow(row);
			row++;
		}
	}

	private void collapseAll(){
		int row = tree.getRowCount() - 1;
		while(row >= 0){
			tree.collapseRow(row);
			row--;
		}
	}


	/* ======================================================================
	 *                          Selection and details
	 * ====================================================================== */

	private void onTreeSelectionChanged(){
		final PlaceTreePayload payload = selectedPayload();
		detailsPanel.showPlace(payload != null? payload.place(): null, hierarchy);
	}

	private void openEditorForSelection(){
		final PlaceTreePayload payload = selectedPayload();
		if(payload == null)
			return;
		final FLEFRecord record = model.getRecordById(payload.place().id());
		if(record == null)
			return;
		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = PlaceHandler.getInstance()
			.createEditDialog(owner, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			reload();
	}

	private PlaceTreePayload selectedPayload(){
		final TreePath path = tree.getSelectionPath();
		if(path == null)
			return null;
		final Object last = path.getLastPathComponent();
		if(!(last instanceof DefaultMutableTreeNode node))
			return null;
		if(!(node.getUserObject() instanceof PlaceTreePayload payload))
			return null;
		return payload;
	}

	private String selectedPlaceId(){
		final PlaceTreePayload payload = selectedPayload();
		return (payload != null? payload.place().id(): null);
	}

	private void restoreSelection(final String placeId){
		if(placeId == null)
			return;
		// Walk the tree and select the first node that carries the given
		// place id. The tree may contain the same place under several
		// parents; the first match is enough for the user's purpose.
		for(int row = 0; row < tree.getRowCount(); row++){
			final TreePath path = tree.getPathForRow(row);
			if(path == null)
				continue;
			final Object last = path.getLastPathComponent();
			if(!(last instanceof DefaultMutableTreeNode node))
				continue;
			if(node.getUserObject() instanceof PlaceTreePayload payload
				&& payload.place().id().equals(placeId)){
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				return;
			}
		}
	}

	private void updateStatus(){
		final int placeCount = hierarchy.places().size();
		final int relationCount = hierarchy.relations().size();
		final int rootCount = hierarchy.rootIds().size();
		statusLabel.setText(placeCount + " places, " + relationCount
			+ " relations, " + rootCount + " roots");
	}


	/* ======================================================================
	 *                          Payload
	 * ====================================================================== */

	/**
	 * The user object attached to every tree node. Carries the place
	 * reference, the relation that led to the node (or {@code null} for
	 * a root), and a flag telling the renderer whether the node closes
	 * a cycle in the graph.
	 */
	public record PlaceTreePayload(
		PlaceHierarchyService.PlaceReference place,
		PlaceHierarchyService.PlaceRelation incoming,
		boolean cycle){}


	/* ======================================================================
	 *                          Details panel
	 * ====================================================================== */

	/**
	 * Small panel that shows the details of the currently selected place:
	 * name, type, coordinates, and the list of incoming and outgoing
	 * relations with their type and validity. The panel is read-only; to
	 * edit a place, the user double-clicks it in the tree.
	 */
	private static final class PlaceDetailsPanel extends JPanel{

		private final FLEFModel model;
		private final javax.swing.JTextPane content = new javax.swing.JTextPane();

		PlaceDetailsPanel(final FLEFModel model){
			this.model = model;
			setLayout(new BorderLayout());
			content.setEditable(false);
			content.setContentType("text/html");
			add(new JScrollPane(content), BorderLayout.CENTER);
		}

		void showPlace(final PlaceHierarchyService.PlaceReference place,
			final PlaceHierarchyService.Hierarchy hierarchy){
			if(place == null){
				content.setText(StringUtils.EMPTY);
				return;
			}

			final StringBuilder sb = new StringBuilder();
			sb.append("<html><body style='font-family:SansSerif;font-size:12px;padding:8px'>");
			sb.append("<h2 style='margin:0 0 6px 0'>").append(escape(place.name())).append("</h2>");
			if(place.type() != null)
				sb.append("<p><b>Type:</b> ").append(escape(place.type())).append("</p>");
			if(place.coordinates() != null)
				sb.append("<p><b>Coordinates:</b> ").append(escape(place.coordinates())).append("</p>");

			final List<PlaceHierarchyService.PlaceRelation> parents = hierarchy.parentsOf(place.id());
			if(!parents.isEmpty()){
				sb.append("<h3 style='margin:12px 0 4px 0'>Parents</h3><ul>");
				for(final PlaceHierarchyService.PlaceRelation rel : parents){
					final PlaceHierarchyService.PlaceReference parent = hierarchy.places().get(rel.parentId());
					sb.append("<li>").append(escape(parent != null? parent.name(): rel.parentId()));
					appendRelationInfo(sb, rel);
					sb.append("</li>");
				}
				sb.append("</ul>");
			}
			else
				sb.append("<p><i>No parent declared.</i></p>");

			final List<PlaceHierarchyService.PlaceRelation> children = hierarchy.childrenOf(place.id());
			if(!children.isEmpty()){
				sb.append("<h3 style='margin:12px 0 4px 0'>Children</h3><ul>");
				for(final PlaceHierarchyService.PlaceRelation rel : children){
					final PlaceHierarchyService.PlaceReference child = hierarchy.places().get(rel.childId());
					sb.append("<li>").append(escape(child != null? child.name(): rel.childId()));
					appendRelationInfo(sb, rel);
					sb.append("</li>");
				}
				sb.append("</ul>");
			}
			else
				sb.append("<p><i>No child declared.</i></p>");

			sb.append("</body></html>");
			content.setText(sb.toString());
			content.setCaretPosition(0);
		}

		private static void appendRelationInfo(final StringBuilder sb,
			final PlaceHierarchyService.PlaceRelation rel){
			if(rel.relationType() != null && !rel.relationType().isBlank())
				sb.append(" <i>— ").append(escape(rel.relationType())).append("</i>");
			final String from = rel.validFrom();
			final String to = rel.validTo();
			if(from != null || to != null){
				sb.append(" <font color='#666'>[");
				sb.append(from != null? escape(from): "?");
				sb.append(" – ");
				sb.append(to != null? escape(to): "?");
				sb.append("]</font>");
			}
		}

		private static String escape(final String s){
			return ReportDialog.escape(s);
		}
	}


	/* ======================================================================
	 *                          Tree cell renderer
	 * ====================================================================== */

	/**
	 * Renders a place node as a compact label:
	 * <pre>
	 *   Name [type]  — relation_type  [valid_from – valid_to]
	 * </pre>
	 * The relation type and the validity dates come from the incoming
	 * edge, so a root node has neither. A node that closes a cycle is
	 * marked with a trailing "(cycle)".
	 */
	private static final class PlaceTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer{

		@Override
		public java.awt.Component getTreeCellRendererComponent(final JTree tree,
			final Object value, final boolean selected, final boolean expanded,
			final boolean leaf, final int row, final boolean hasFocus){
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

			if(value instanceof DefaultMutableTreeNode node
				&& node.getUserObject() instanceof PlaceTreePayload payload){
				setText(buildLabel(payload));
				setIcon(null);
			}
			else
				setText(value != null? value.toString(): StringUtils.EMPTY);
			return this;
		}

		private static String buildLabel(final PlaceTreePayload payload){
			final StringBuilder sb = new StringBuilder();
			final PlaceHierarchyService.PlaceReference place = payload.place();
			final String name = (place.hasName()? place.name(): place.id());
			sb.append("<html><b>").append(escape(name)).append("</b>");
			if(place.type() != null && !place.type().isBlank())
				sb.append(" <font color='#888'>[").append(escape(place.type())).append("]</font>");
			if(payload.incoming() != null){
				final PlaceHierarchyService.PlaceRelation rel = payload.incoming();
				if(rel.relationType() != null && !rel.relationType().isBlank())
					sb.append(" <i><font color='#666'>— ").append(escape(rel.relationType())).append("</font></i>");
				final String from = rel.validFrom();
				final String to = rel.validTo();
				if(from != null || to != null)
					sb.append(" <font color='#999'>[")
						.append(escape(from != null? from: "?"))
						.append(" – ")
						.append(escape(to != null? to: "?"))
						.append("]</font>");
			}
			if(payload.cycle())
				sb.append(" <font color='#b00020'>(cycle)</font>");
			sb.append("</html>");
			return sb.toString();
		}

		private static String escape(final String s){
			return ReportDialog.escape(s);
		}
	}


	public static void main(String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		String recordId = "P1";

		final String content;
		try(final InputStream is = PlaceHierarchyPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		EventQueue.invokeLater(() -> {
			final PlaceHierarchyPanel panel = new PlaceHierarchyPanel(model);

			final JFrame frame = new JFrame();
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
