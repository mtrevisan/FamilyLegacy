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
package io.github.mtrevisan.familylegacy.ui.components.projections.places;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.tools.ReportDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
import java.util.Locale;
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
 * substring on the place name and type) and a combo box to restrict the
 * tree to a single relation type (administrative, ecclesiastical,
 * judicial, cadastral, geographic). When the combo is set to the "All"
 * entry, every relation is shown.
 * <p>
 * <b>Details.</b> A side panel shows the full information of the
 * selected place: type, coordinates, notes, incoming relations
 * (parents), and outgoing relations (children), each with its type and
 * validity dates. A double-click opens the record editor through the
 * registered {@link RecordTypeHandler}.
 */
public final class PlaceHierarchyPanel extends JPanel{

	/** Sentinel for the "no filter" entry of the relation type combo. */
	private static final String FILTER_ALL_KEY = "any";

	private static final int TREE_MIN_WIDTH = 360;
	private static final int DETAILS_MIN_WIDTH = 300;


	private final FLEFModel model;

	private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Places");
	private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
	private final JTree tree = new JTree(treeModel){
		@Override
		protected void processMouseEvent(final MouseEvent e){
			// If it's a double click, consume/prevent expansion and trigger editor instead
			if(e.getID() == MouseEvent.MOUSE_PRESSED && e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)){
				final int row = getRowForLocation(e.getX(), e.getY());
				if(row != -1){
					// Consume the event so BasicTreeUI doesn't see the double-click and doesn't toggle
					e.consume();

					openEditorForSelection();

					return;
				}
			}
			super.processMouseEvent(e);
		}
	};

	private final JTextField searchField = new JTextField(20);
	private final BoundComboBox<String> relationTypeFilter = new BoundComboBox<>(PlaceRelationshipReader.TAG_TYPE);
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

		relationTypeFilter.addActionListener(e -> rebuildTree());
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
		relationTypeFilter.setI18NPrefix("enum.place_relationship.type");
		relationTypeFilter.setPreferredSize(new Dimension(180,
			relationTypeFilter.getPreferredSize().height));
		toolbar.add(relationTypeFilter);

		final JButton reloadButton = new JButton("Reload");
		reloadButton.addActionListener(e -> reload());
		toolbar.add(reloadButton);

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

		detailsPanel = new PlaceDetailsPanel();
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
		// data, and prepend the "All" entry. The sentinel is a display
		// label, not a protocol type, so it is compared by identity in
		// the filter logic below.
		final Set<String> types = new TreeSet<>(PlaceHierarchyService.declaredRelationTypes());
		for(final PlaceHierarchyService.PlaceRelation rel : hierarchy.relations())
			if(rel.relationType() != null && !rel.relationType().isBlank())
				types.add(rel.relationType());

		final String previous = (String)relationTypeFilter.getSelectedItem();
		final List<String> items = new ArrayList<>();
		items.add(FILTER_ALL_KEY);
		items.addAll(types);
		relationTypeFilter.setModel(new DefaultComboBoxModel<>(items.toArray(new String[0])));
		if(previous != null && items.contains(previous))
			relationTypeFilter.setSelectedItem(previous);
		else
			relationTypeFilter.setSelectedItem(FILTER_ALL_KEY);
	}

	private void rebuildTree(){
		treeRoot.removeAllChildren();

		final String filter = (String)relationTypeFilter.getSelectedItem();
		final String search = searchField.getText();
		final boolean hasFilter = (filter != null && !FILTER_ALL_KEY.equals(filter));

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
	private DefaultMutableTreeNode buildNode(final String placeId, final PlaceHierarchyService.PlaceRelation incoming,
			final Set<String> path, final boolean hasFilter, final String filter, final String search){
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
		final String needle = search.trim().toLowerCase(Locale.ROOT);
		if(needle.isEmpty())
			return true;
		if(place.name() != null && place.name().toLowerCase(Locale.ROOT).contains(needle))
			return true;
		if(place.type() != null && place.type().toLowerCase(Locale.ROOT).contains(needle))
			return true;
		return false;
	}

	private void expandFirstLevels(final int levels){
		int row = 0;
		while(row < tree.getRowCount() && tree.getPathForRow(row).getPathCount() <= levels){
			tree.expandRow(row);
			row ++;
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

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = handler.createEditDialog(owner, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			// Check if the place name was changed to match an existing place, and merge them if necessary
			mergeDuplicatePlacesIfAny(record, new HashSet<>());

			removeDuplicateRelationships();

			// NOTE: already done by rebuildRelationTypeModel when setting relationTypeFilter
			//reload();
		}
	}

	private void mergeDuplicatePlacesIfAny(final FLEFRecord modifiedRecord, final Set<String> visited){
		if(!visited.add(modifiedRecord.getId()))
			return;

		if(!PlaceHandler.isOfType(modifiedRecord))
			return;

		final String modifiedName = extractPlaceName(modifiedRecord);
		if(StringUtils.isEmpty(modifiedName))
			return;

		// Search for another place record with the exact same name
		String activeId = modifiedRecord.getId();
		FLEFRecord targetPlace = null;
		final List<FLEFRecord> records = model.getRecords();
		for(final FLEFRecord record : records){
			if(PlaceHandler.isOfType(record) && !record.getId().equals(activeId)
					&& modifiedName.equalsIgnoreCase(extractPlaceName(record))){
				targetPlace = record;

				break;
			}
		}

		if(targetPlace != null){
			final String oldId = activeId;
			final String newId = targetPlace.getId();

			// Replace all references pointing to oldId with newId across the model
			model.replaceRecordId(oldId, newId);

			// Remove the redundant duplicate record from the tree view UI
			final DefaultMutableTreeNode duplicateNode = findNodeById(treeRoot, oldId);
			if(duplicateNode != null)
				treeModel.removeNodeFromParent(duplicateNode);

			// Clear selection and details if the removed node was currently selected
			if(oldId.equals(selectedPlaceId())){
				tree.clearSelection();

				detailsPanel.showPlace(null, hierarchy);
			}

			activeId = newId;
		}

		// Recursively check children and parents to merge matching sub-structures
		final List<FLEFRecord> relatedRecords = new ArrayList<>();
		for(final FLEFRecord record : records)
			if(PlaceRelationshipHandler.isOfType(record)){
				final FLEFRecord subj = extractRelationshipPlace(record, PlaceRelationshipReader.TAG_SUBJECT);
				final FLEFRecord obj = extractRelationshipPlace(record, PlaceRelationshipReader.TAG_OBJECT);
				if(subj != null && activeId.equals(subj.getValue()))
					relatedRecords.add(obj);
				else if(obj != null && activeId.equals(obj.getValue()))
					relatedRecords.add(subj);
			}

		for(final FLEFRecord nextRecord : relatedRecords)
			mergeDuplicatePlacesIfAny(nextRecord, visited);
	}

	private static String extractPlaceName(final FLEFRecord placeRecord){
		for(final FLEFRecord child : placeRecord.getChildren())
			if(PlaceReader.TAG_NAME.equals(child.getTag()))
				for(final FLEFRecord sub : child.getChildren())
					if(NameReader.TAG_VALUE.equals(sub.getTag()) && sub.getValue() != null)
						return sub.getValue().trim();
		return null;
	}

	private static FLEFRecord extractRelationshipPlace(final FLEFRecord relRecord, final String sideTag){
		for(final FLEFRecord child : relRecord.getChildren())
			if(sideTag.equals(child.getTag()))
				for(final FLEFRecord sub : child.getChildren())
					if(PlaceHandler.isOfType(sub))
						return sub;
		return null;
	}

	private void removeDuplicateRelationships(){
		final Set<String> seenRelations = new HashSet<>();
		final List<FLEFRecord> toRemove = new ArrayList<>();

		for(final FLEFRecord rec : new ArrayList<>(model.getRecords())){
			if(PlaceRelationshipHandler.isOfType(rec)){
				final FLEFRecord subj = extractRelationshipPlace(rec, PlaceRelationshipReader.TAG_SUBJECT);
				final FLEFRecord obj = extractRelationshipPlace(rec, PlaceRelationshipReader.TAG_OBJECT);
				final String type = extractRelationshipType(rec);

				// Create a unique signature for this relationship direction and type
				final String signature = type
					+ "|" + (subj != null? subj.getValue(): StringUtils.EMPTY)
					+ "|" + (obj != null? obj.getValue(): StringUtils.EMPTY);
				if(!seenRelations.add(signature))
					toRemove.add(rec);
			}
		}

		for(final FLEFRecord rec : toRemove)
			model.removeRecord(rec.getId());
	}

	private static String extractRelationshipType(final FLEFRecord relRecord){
		for(final FLEFRecord child : relRecord.getChildren())
			if(PlaceRelationshipReader.TAG_TYPE.equalsIgnoreCase(child.getTag()) && child.getValue() != null)
				return child.getValue();
		return StringUtils.EMPTY;
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

	/**
	 * Selects the first node in the model that carries the given place
	 * id. The walk is performed on the model, not on the visible rows,
	 * so a place that is inside a collapsed branch is still found and
	 * the tree is expanded to reveal it.
	 */
	private void restoreSelection(final String placeId){
		if(placeId == null)
			return;

		final DefaultMutableTreeNode found = findNodeById(treeRoot, placeId);
		if(found == null)
			return;

		final TreePath path = new TreePath(found.getPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
	}

	private static DefaultMutableTreeNode findNodeById(final DefaultMutableTreeNode root, final String placeId){
		for(int i = 0; i < root.getChildCount(); i ++){
			final DefaultMutableTreeNode child = (DefaultMutableTreeNode)root.getChildAt(i);
			if(child.getUserObject() instanceof PlaceTreePayload payload
					&& payload.place().id().equals(placeId))
				return child;

			final DefaultMutableTreeNode nested = findNodeById(child, placeId);
			if(nested != null)
				return nested;
		}
		return null;
	}

	private void updateStatus(){
		final int placeCount = hierarchy.places().size();
		final int relationCount = hierarchy.relations().size();
		final int rootCount = hierarchy.rootIds().size();
		statusLabel.setText(placeCount + " places, " + relationCount + " relations, " + rootCount + " roots");
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
	 * name, type, coordinates, notes, and the list of incoming and
	 * outgoing relations with their type and validity. The panel is
	 * read-only; to edit a place, the user double-clicks it in the tree.
	 */
	private static final class PlaceDetailsPanel extends JPanel{

		private final JTextPane content = new JTextPane();

		PlaceDetailsPanel(){
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

			if(!place.notes().isEmpty()){
				sb.append("<h3 style='margin:12px 0 4px 0'>Notes</h3>");
				for(final String note : place.notes())
					sb.append("<p style='margin:2px 0'>").append(escape(note)).append("</p>");
			}

			sb.append("</body></html>");
			content.setText(sb.toString());
			content.setCaretPosition(0);
		}

		private static void appendRelationInfo(final StringBuilder sb, final PlaceHierarchyService.PlaceRelation rel){
			if(rel.relationType() != null && !rel.relationType().isBlank())
				sb.append(" <i>— ").append(escape(formatRelationType(rel.relationType()))).append("</i>");
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
	 * marked with a trailing {@code (cycle)}.
	 * <p>
	 * The default leaf and folder icons are kept, so the tree reads like
	 * any other Swing tree in the application.
	 */
	private static final class PlaceTreeCellRenderer extends DefaultTreeCellRenderer{

		@Override
		public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
				final boolean expanded, final boolean leaf, final int row, final boolean hasFocus){
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

			if(value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof PlaceTreePayload payload)
				setText(buildLabel(payload));
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
					sb.append(" <i><font color='#666'>— ")
						.append(escape(formatRelationType(rel.relationType())))
						.append("</font></i>");
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


	/* ======================================================================
	 *                          Shared helpers
	 * ====================================================================== */

	/**
	 * Formats a protocol relation type for display: underscores become
	 * spaces. The raw protocol value is preserved in the model and in
	 * the relation record; only the label shown to the user is
	 * prettified.
	 */
	private static String formatRelationType(final String type){
		return (type != null? type.replace('_', ' '): StringUtils.EMPTY);
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";

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
			frame.add(panel, BorderLayout.CENTER);
			frame.setSize(1000, 640);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
