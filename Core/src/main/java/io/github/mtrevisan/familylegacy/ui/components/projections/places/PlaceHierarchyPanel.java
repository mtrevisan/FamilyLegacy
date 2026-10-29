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
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.FLEFMerger;
import io.github.mtrevisan.familylegacy.io.merger.RecordAbsorber;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyResolver;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.components.lists.PrivacyBadge;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMatcher;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.PlaceHierarchyView;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.ui.tools.tools.merge.MergeGrammar;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


/**
 * Panel that presents the place hierarchy as a navigable tree.
 * <p>
 * The hierarchy is read by {@link PlaceHierarchyService} and rendered as
 * a {@link JTree}. Because the underlying data is a DAG, a place can
 * appear under several parents: the tree duplicates it as needed and
 * marks the duplication in the renderer. Cycles are broken at the first
 * repeated node and flagged in the same way.
 * <p>
 * <b>No toolbar.</b> The panel is a passive view: it does not own the
 * search field or the relation-type filter. Those controls belong to the
 * host, which drives the panel through {@link #setFilters(Set, String, String)}.
 * <p>
 * <b>Filtering semantics.</b> A node is visible when it matches the
 * search filter, or when it is an ancestor of a node that matches. The
 * matching is delegated to {@link SearchMatcher}. A node with no
 * children after filtering is dropped if it does not match the search
 * filter. This applies to every node, roots included. Cycle nodes are
 * also filtered: a cycle that does not match the search filter is
 * dropped, together with its ancestors.
 * <p>
 * <b>Asynchronous rebuilding.</b> The tree is rebuilt on a background
 * thread. The host can listen for progress updates through
 * {@link #PROPERTY_FILTERING_PROGRESS} (an {@link Integer} between 0 and
 * 100) and for completion through {@link #PROPERTY_FILTERING_DONE}.
 * <p>
 * <b>Picker mode.</b> When the panel is embedded in a selection dialog,
 * the host can call {@link #setEditable(boolean) setEditable(false)} to
 * disable the edit-on-double-click behavior and read the selected
 * {@code PlaceRelationshipRecord} through
 * {@link #getSelectedPlaceRecord()}.
 */
public final class PlaceHierarchyPanel extends JPanel implements PlaceHierarchyView{

	/** Property fired with the percentage (0-100) of the tree rebuild. */
	public static final String PROPERTY_FILTERING_PROGRESS = PlaceHierarchyView.PROPERTY_FILTERING_PROGRESS;
	/** Property fired when the asynchronous rebuild completes. */
	public static final String PROPERTY_FILTERING_DONE = PlaceHierarchyView.PROPERTY_FILTERING_DONE;

	private static final int TREE_MIN_WIDTH = 360;
	private static final int DETAILS_MIN_WIDTH = 300;

	private static final String PART_OF_SUFFIX = "_part_of";
	private static final String TAG_SUBJECT_PLACE = "subject.place";
	private static final String TAG_TARGET_PLACE = "target.place";


	private final FLEFModel model;

	/** When {@code false}, the edit-on-double-click behavior is disabled. */
	private boolean editable = true;

	/** Free-text search filter. */
	private String searchFilter = StringUtils.EMPTY;

	/** The admission modes that control how the search filter is applied. */
	private Set<SearchMode> searchModes = Set.of();

	/** Restrict the tree to a single relation type, or {@code null} for all. */
	private String relationTypeFilter;

	/**
	 * The {@code PlaceRelationshipRecord} selected in the tree, or
	 * {@code null} when the current selection is empty or points to a
	 * root place.
	 */
	private FLEFRecord selectedPlaceRecord;

	private DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Places");
	private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
	private final JTree tree = new JTree(treeModel){
		{
			// Disable the built-in double-click-to-expand behavior:
			// a double-click on a node must open the editor and must
			// not also toggle the node expansion. Single-click on the
			// expand/collapse handle still toggles, because that path
			// (BasicTreeUI.Handler.mouseReleased → isLocationInExpandControl)
			// is independent of toggleClickCount.
			setToggleClickCount(0);
		}

		@Override
		protected void processMouseEvent(final MouseEvent e){
			if(e.getID() == MouseEvent.MOUSE_CLICKED && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2
					&& PlaceHierarchyPanel.this.editable){
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

	private final PlaceDetailsPanel detailsPanel = new PlaceDetailsPanel();

	private PlaceHierarchyService.Hierarchy hierarchy;

	/** The active worker that rebuilds the tree, if any. */
	private SwingWorker<DefaultMutableTreeNode, Integer> rebuildWorker;


	public PlaceHierarchyPanel(final FLEFModel model){
		this.model = model;

		setLayout(new BorderLayout());
		add(createCenter(), BorderLayout.CENTER);

		reload();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Re-reads the hierarchy from the model and rebuilds the tree. Called
	 * on construction and whenever the underlying model has changed. The
	 * currently selected place, the search filter, and the relation-type
	 * filter are preserved across the reload.
	 */
	@Override
	public JComponent component(){
		return this;
	}

	@Override
	public void reload(){
		final String previousSelection = selectedPlaceId();

		this.hierarchy = PlaceHierarchyService.load(model);

		rebuildTree();

		restoreSelection(previousSelection);
	}

	/**
	 * Sets all filters at once and triggers an asynchronous tree rebuild.
	 * <p>
	 * Using a single method for all filters avoids starting multiple
	 * concurrent workers when the host changes more than one filter in
	 * the same user action.
	 *
	 * @param modes        the admission modes; may be {@code null}
	 * @param search       the free-text search filter; may be {@code null}
	 * @param relationType the relation type filter; may be {@code null}
	 */
	@Override
	public boolean setFilters(final Set<SearchMode> modes, final String search, final String relationType){
		final Set<SearchMode> normalizedModes = (modes != null? Set.copyOf(modes): Set.of());
		final String normalizedSearch = (search != null? search: StringUtils.EMPTY);
		final String normalizedType = (StringUtils.isBlank(relationType)? null: relationType);

		if(Objects.equals(searchModes, normalizedModes)
			&& Objects.equals(searchFilter, normalizedSearch)
			&& Objects.equals(relationTypeFilter, normalizedType))
			return false;

		this.searchModes = normalizedModes;
		this.searchFilter = normalizedSearch;
		this.relationTypeFilter = normalizedType;

		rebuildTree();
		return true;
	}

	/**
	 * Enables or disables the edit-on-double-click behavior. When the
	 * panel is used as a picker inside a selection dialog, editing must
	 * be disabled: the user is choosing a record to reference, not
	 * editing an existing one.
	 *
	 * @param editable {@code true} to enable the edit-on-double-click
	 *                 behavior, {@code false} to disable it
	 */
	@Override
	public void setEditable(final boolean editable){
		this.editable = editable;
	}

	/**
	 * Returns the {@code PlaceRelationshipRecord} currently selected in
	 * the tree, or {@code null} when the selection is empty or points to
	 * a root place.
	 *
	 * @return the selected relationship, or {@code null}
	 */
	@Override
	public FLEFRecord getSelectedPlaceRecord(){
		return selectedPlaceRecord;
	}


	/* ======================================================================
	 *                          UI composition
	 * ====================================================================== */

	private JComponent createCenter(){
		tree.setRootVisible(true);
		tree.setShowsRootHandles(true);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new PlaceTreeCellRenderer(model));
		tree.addTreeSelectionListener(e -> onTreeSelectionChanged());

		final JScrollPane treeScroll = new JScrollPane(tree);
		treeScroll.setMinimumSize(new Dimension(TREE_MIN_WIDTH, 200));

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


	/* ======================================================================
	 *                          Tree construction
	 * ====================================================================== */

	/**
	 * Rebuilds the tree asynchronously. The new tree structure is
	 * computed on a background thread and applied to the {@link JTree}
	 * on the EDT. The progress is reported in percentage through
	 * {@link #PROPERTY_FILTERING_PROGRESS}, and completion through
	 * {@link #PROPERTY_FILTERING_DONE}.
	 */
	private void rebuildTree(){
		if(rebuildWorker != null && !rebuildWorker.isDone())
			rebuildWorker.cancel(true);

		final String currentSearch = searchFilter;
		final String currentType = relationTypeFilter;
		final Set<SearchMode> currentModes = searchModes;
		final int totalNodes = hierarchy.places().size();
		final AtomicInteger visited = new AtomicInteger(0);
		final AtomicInteger lastPercent = new AtomicInteger(-1);

		// Fire 0% immediately so the progress bar moves from its initial
		// state before the first chunk is processed.
		firePropertyChange(PROPERTY_FILTERING_PROGRESS, null, 0);

		rebuildWorker = new SwingWorker<>(){
			@Override
			protected DefaultMutableTreeNode doInBackground(){
				final DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode("Places");
				for(final String rootId : hierarchy.rootIds()){
					if(isCancelled()) return newRoot;

					final Consumer<Integer> throttledPublisher = percent -> {
						if(percent > lastPercent.getAndSet(percent))
							publish(percent);
					};

					final DefaultMutableTreeNode node = buildNode(rootId, null, new HashSet<>(),
						currentSearch, currentType, currentModes, visited, totalNodes, throttledPublisher);
					if(node != null)
						newRoot.add(node);
				}
				return newRoot;
			}

			@Override
			protected void process(final List<Integer> chunks){
				if(isCancelled())
					return;

				final int latest = chunks.getLast();
				PlaceHierarchyPanel.this.firePropertyChange(PROPERTY_FILTERING_PROGRESS, null, latest);
			}

			@Override
			protected void done(){
				if(isCancelled()) return;

				try{
					PlaceHierarchyPanel.this.treeRoot = get();
					treeModel.setRoot(treeRoot);

					tree.expandPath(new TreePath(treeRoot));

					// Final progress (100%) and completion event. Firing
					// 100 explicitly covers the case where filtering
					// pruned some nodes and `visited` never reached
					// `totalNodes`.
					PlaceHierarchyPanel.this.firePropertyChange(PROPERTY_FILTERING_PROGRESS, null, 100);
					PlaceHierarchyPanel.this.firePropertyChange(PROPERTY_FILTERING_DONE, false, true);
				}
				catch(final Exception ignored){
					// Ignore exceptions from canceled workers, but
					// ensure the progress bar is closed anyway to avoid
					// leaving the host in a "loading" state.
					PlaceHierarchyPanel.this.firePropertyChange(PROPERTY_FILTERING_DONE, false, true);
				}
			}
		};

		rebuildWorker.execute();
	}

	/**
	 * Builds a {@link DefaultMutableTreeNode} for the given place and,
	 * recursively, for its children.
	 * <p>
	 * A node is kept when at least one of the following holds:
	 * <ul>
	 *   <li>it has children after filtering;</li>
	 *   <li>the search filter is active and the node matches it;</li>
	 *   <li>the search filter is empty, the type filter is active, and
	 *       the node is the child endpoint of a matching relation.</li>
	 * </ul>
	 * A leaf node that satisfies none of the above is dropped. This
	 * applies to roots as well: a root whose entire subtree has been
	 * filtered out disappears, so the tree does not show empty
	 * branches.
	 * <p>
	 * The {@code path} set contains the places already visited on the
	 * current path from the root, so a cycle is broken at the first
	 * repeated node and the node is marked as a cycle for the renderer.
	 */
	private DefaultMutableTreeNode buildNode(final String placeId, final PlaceHierarchyService.PlaceRelation incoming,
			final Set<String> path, final String currentSearch, final String currentType,
			final Set<SearchMode> currentModes, final AtomicInteger visited, final int totalNodes,
			final Consumer<Integer> progressPublisher){
		final PlaceHierarchyService.PlaceReference place = hierarchy.places()
			.get(placeId);
		if(place == null)
			return null;

		// Report progress for every node visited, regardless of whether
		// it is kept or dropped. The count is capped so that a filter
		// that prunes many nodes still reaches 100 at the end (the
		// explicit firePropertyChange in done() guarantees the final
		// value).
		final int v = visited.incrementAndGet();
		if(totalNodes > 0 && progressPublisher != null){
			final int percent = Math.min(100, (int)(v * 100. / totalNodes));

			progressPublisher.accept(percent);
		}

		final boolean isCycle = path.contains(placeId);
		final PlaceTreePayload payload = new PlaceTreePayload(place, incoming, isCycle);
		final DefaultMutableTreeNode node = new DefaultMutableTreeNode(payload);

		// Cycle nodes are filtered like any other leaf: if a search filter
		// is active and the cycle node does not match, it is dropped,
		// together with the ancestors that would otherwise survive only
		// because of the cycle.
		if(isCycle){
			if(currentSearch.isBlank() || matches(place, currentSearch, currentModes))
				return node;

			return null;
		}

		final Set<String> newPath = new HashSet<>(path);
		newPath.add(placeId);

		// Step 1: build the children, applying the type filter on edges.
		// The type filter is a property of the *edge*, so it is applied
		// here, before the recursion.
		final boolean hasTypeFilter = (currentType != null);
		for(final PlaceHierarchyService.PlaceRelation rel : hierarchy.childrenOf(placeId)){
			if(hasTypeFilter && !currentType.equals(rel.relationType()))
				continue;

			final DefaultMutableTreeNode child = buildNode(rel.childId(), rel, newPath, currentSearch, currentType,
				currentModes, visited, totalNodes, progressPublisher);
			if(child != null)
				node.add(child);
		}

		final boolean hasSearchFilter = !currentSearch.isBlank();
		if(hasSearchFilter || hasTypeFilter){
			if(node.isLeaf()){
				if(hasSearchFilter){
					if(!matches(place, currentSearch, currentModes))
						return null;
				}
				else if(incoming == null)
					return null;
			}
		}

		return node;
	}

	/**
	 * Returns whether the given place matches the search filter.
	 * <p>
	 * The actual comparison is delegated to {@link SearchMatcher}, the
	 * same component used by every other search strategy in the
	 * application. This keeps the search behaviour consistent: a query
	 * that uses fuzzy or whole-word mode behaves the same way whether
	 * it is evaluated against a flat list or against the hierarchy.
	 * <p>
	 * The target text used for the comparison is the canonical display
	 * text produced by {@link PlaceHandler#getDisplayText(FLEFRecord, FLEFModel)},
	 * which is the same text shown in the flat list. This guarantees
	 * that a place visible in one context is visible in the other.
	 */
	private boolean matches(final PlaceHierarchyService.PlaceReference place,
		final String currentSearch, final Set<SearchMode> currentModes){
		if(currentSearch.isBlank())
			return true;

		final String targetText = place.displayText();
		if(targetText == null)
			return false;

		return SearchMatcher.score(currentSearch, targetText, currentModes)
			.passes();
	}


	/* ======================================================================
	 *                          Selection and details
	 * ====================================================================== */

	private void onTreeSelectionChanged(){
		final PlaceTreePayload payload = selectedPayload();
		selectedPlaceRecord = null;

		if(payload != null){
			final String childId = payload.place().id();
			selectedPlaceRecord = model.getRecordById(childId);
		}

		detailsPanel.showPlace(payload != null? payload.place(): null, hierarchy);
	}

	@Override
	public void openEditorForSelection(){
		if(!editable)
			return;

		final PlaceTreePayload payload = selectedPayload();
		if(payload == null)
			return;

		final FLEFRecord record = model.getRecordById(payload.place().id());
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		final Window owner = SwingUtilities.getWindowAncestor(this);
		final RecordDialog dialog = handler.createEditDialog(owner, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			// the edit may have made the place a copy of another one (e.g. a name changed to that of an existing place)
			mergeDuplicatePlaces(record);

			reload();
		}
	}

	/**
	 * Proposes to merge the edited place with the places that may be the same, as the automatic merge judges them (names,
	 * jurisdiction, coordinates, subdivisions, uses), and merges it following the protocol when the user confirms: the fields are
	 * united, every reference is redirected, the duplicated relationships are collapsed uniting their evidence, and the merge is
	 * recorded in the audit of the surviving place.
	 * <p>
	 * After a merge, the jurisdictions and the subdivisions of the merged place may in turn repeat each other, and are examined the
	 * same way.
	 */
	private void mergeDuplicatePlaces(final FLEFRecord editedPlace){
		final FLEFGrammar grammar = MergeGrammar.instance();
		if(!PlaceHandler.isOfType(editedPlace) || grammar == null)
			return;

		final RecordAbsorber absorber = new RecordAbsorber(grammar);
		final Window owner = SwingUtilities.getWindowAncestor(this);
		final Deque<String> pending = new ArrayDeque<>(List.of(editedPlace.getId()));
		final Set<String> examined = new HashSet<>();
		while(!pending.isEmpty()){
			final String placeId = pending.poll();
			if(!examined.add(placeId) || !model.hasRecord(placeId))
				continue;

			final String survivorId = proposeMerge(absorber, owner, placeId);
			if(survivorId == null){
				// even without a merge, the edit may have repeated a relationship of the place (e.g. the same province twice)
				absorber.collapseDuplicates(model, placeId);
				continue;
			}
			if(survivorId.isEmpty())
				// the user stopped the merging
				return;

			examined.add(survivorId);
			pending.addAll(relatedPlaces(survivorId));
		}
	}

	/**
	 * Asks the user about the candidates of a place, most likely first.
	 *
	 * @return the id of the place it was merged into, {@code null} if it was not merged, an empty string if the user stopped
	 */
	private String proposeMerge(final RecordAbsorber absorber, final Window owner, final String placeId){
		final FLEFRecord place = model.getRecordById(placeId);
		for(final RecordAbsorber.Candidate candidate : absorber.candidates(model, placeId, FLEFMerger.DEFAULT_REVIEW_THRESHOLD)){
			final FLEFRecord other = model.getRecordById(candidate.recordId());
			final String message = String.format(Locale.ROOT, I18N.t("dialog.merge.places.message"),
				displayText(place), placeId, displayText(other), other.getId(), candidate.probability() * 100.,
				candidate.rationale().replace("; ", StringUtils.LF));
			final int choice = JOptionPane.showConfirmDialog(owner, message, I18N.t("dialog.merge.places.title"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			if(choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION)
				return StringUtils.EMPTY;
			if(choice != JOptionPane.YES_OPTION){
				// the judgment is kept in the file: an identity hypothesis disproven by a conclusion
				absorber.rejectIdentity(model, placeId, other.getId(), String.format(Locale.ROOT,
						"Proposed in the place hierarchy (match probability %.3f): %s", candidate.probability(), candidate.rationale()),
					"Judged different places by the researcher in the place hierarchy");
				continue;
			}

			final List<String> conflicts = absorber.mergeFields(other, place);
			absorber.absorb(model, placeId, other.getId(), String.format(Locale.ROOT,
				"confirmed in the place hierarchy (match probability %.3f: %s)", candidate.probability(), candidate.rationale()));
			if(!conflicts.isEmpty())
				JOptionPane.showMessageDialog(owner, I18N.al(I18N.tf("dialog.merge.places.conflicts", placeId)) + StringUtils.LF
					+ String.join(StringUtils.LF, conflicts), I18N.t("dialog.merge.places.title"), JOptionPane.INFORMATION_MESSAGE);
			return other.getId();
		}
		return null;
	}

	/**
	 * The places a place is part of, and the places that are part of it.
	 */
	private List<String> relatedPlaces(final String placeId){
		final List<String> related = new ArrayList<>();
		final List<FLEFRecord> relationships = PrivacyService.visible(model, model.getRecordsByType(PlaceRelationshipHandler.TYPE));
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_TYPE);
			if(type == null || !type.endsWith(PART_OF_SUFFIX))
				continue;

			final String subject = FLEFRecordHelper.getChildValue(relationship, TAG_SUBJECT_PLACE);
			final String target = FLEFRecordHelper.getChildValue(relationship, TAG_TARGET_PLACE);
			if(placeId.equals(subject) && target != null)
				related.add(target);
			else if(placeId.equals(target) && subject != null)
				related.add(subject);
		}
		return related;
	}

	private String displayText(final FLEFRecord place){
		final String text = PlaceHandler.getInstance()
			.getDisplayText(place, model);
		return (StringUtils.isNotBlank(text)? text: place.getId());
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
			if(child.getUserObject() instanceof PlaceTreePayload payload && payload.place().id().equals(placeId))
				return child;

			final DefaultMutableTreeNode nested = findNodeById(child, placeId);
			if(nested != null)
				return nested;
		}
		return null;
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

			// Listener to open links in the system browser
			content.addHyperlinkListener(e -> {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
					if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)){
						try{
							Desktop.getDesktop()
								.browse(e.getURL().toURI());
						}
						catch(final Exception ignored){
							// Log or ignore
						}
					}
				}
			});

			// Listener to change the cursor to a hand when hovering over a link
			content.addHyperlinkListener(e -> {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
					if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)){
						try{
							Desktop.getDesktop()
								.browse(e.getURL().toURI());
						}
						catch(final Exception ignored){
							// Log or ignore
						}
					}
				}
				else if(e.getEventType() == HyperlinkEvent.EventType.ENTERED)
					// Change cursor to hand when hovering over a link
					content.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				else if(e.getEventType() == HyperlinkEvent.EventType.EXITED)
					// Restore default cursor when leaving the link
					content.setCursor(Cursor.getDefaultCursor());
			});

			add(new JScrollPane(content), BorderLayout.CENTER);
		}

		void showPlace(final PlaceHierarchyService.PlaceReference place,
				final PlaceHierarchyService.Hierarchy hierarchy){
			if(place == null){
				content.setText(StringUtils.EMPTY);

				return;
			}

			final StringBuilder sb = new StringBuilder();
			// Base typography and padding for the whole document
			sb.append("<html><body style='font-family: \"Segoe UI\", Helvetica, Arial, sans-serif; font-size: 13px; color: #333333; padding: 16px;'>");

			// Title section
			sb.append("<div style='font-size: 20px; font-weight: bold; color: #202124; margin-bottom: 4px'>")
				.append(escape(place.name()))
				.append("</div>");

			// Metadata line (Type & Coordinates)
			boolean hasMeta = false;
			final StringBuilder metaSb = new StringBuilder();
			if(StringUtils.isNotBlank(place.type())){
				metaSb.append("Type: ").append(escape(place.type()));
				hasMeta = true;
			}
			if(StringUtils.isNotBlank(place.coordinates())){
				if(hasMeta)
					metaSb.append(" &nbsp;•&nbsp; ");

				// Build the map URL (reusing the logic from ShowOnMapTool)
				final String mapUrl = buildMapUrl(place.coordinates(), place.name());
				if(mapUrl != null){
					// Escape & to &amp; to prevent the HTML parser from choking
					final String safeUrl = mapUrl.replace("&", "&amp;");
					metaSb.append("<a href='").append(safeUrl).append("' style='color: #0066cc; text-decoration: underline;'>")
						.append("Coordinates: ").append(escape(place.coordinates()))
						.append("</a>");
				}
				else
					metaSb.append("Coordinates: ").append(escape(place.coordinates()));

				hasMeta = true;
			}
			if(hasMeta)
				sb.append("<div style='font-size: 12px; color: #5f6368; margin-bottom: 20px;'>")
					.append(metaSb)
					.append("</div>");

			// --- Parents Section ---
			final List<PlaceHierarchyService.PlaceRelation> parents = hierarchy.parentsOf(place.id());
			sb.append("<div style='font-size: 11px; font-weight: bold; color: #888888; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 12px; margin-bottom: 8px; border-bottom: 1px solid #eaeaea; padding-bottom: 4px;'>Parents</div>");
			if(!parents.isEmpty()){
				for(final PlaceHierarchyService.PlaceRelation rel : parents){
					final PlaceHierarchyService.PlaceReference parent = hierarchy.places().get(rel.parentId());
					appendRelationRow(sb, parent != null? parent.name(): rel.parentId(), rel);
				}
			}
			else
				sb.append("<div style='color: #999999; font-style: italic;'>No parent declared.</div>");

			// --- Children Section ---
			final List<PlaceHierarchyService.PlaceRelation> children = hierarchy.childrenOf(place.id());
			sb.append("<div style='font-size: 11px; font-weight: bold; color: #888888; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 12px; margin-bottom: 8px; border-bottom: 1px solid #eaeaea; padding-bottom: 4px;'>Children</div>");
			if(!children.isEmpty()){
				for(final PlaceHierarchyService.PlaceRelation rel : children){
					final PlaceHierarchyService.PlaceReference child = hierarchy.places().get(rel.childId());
					appendRelationRow(sb, child != null? child.name(): rel.childId(), rel);
				}
			}
			else
				sb.append("<div style='color: #999999; font-style: italic;'>No child declared.</div>");

			// --- Notes Section ---
			if(!place.notes().isEmpty()){
				sb.append("<div style='font-size: 11px; font-weight: bold; color: #888888; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 12px; margin-bottom: 8px; border-bottom: 1px solid #eaeaea; padding-bottom: 4px;'>Notes</div>");
				for(final String note : place.notes())
					sb.append("<div style='margin-bottom: 6px; line-height: 1.4;'>").append(escape(note)).append("</div>");
			}

			sb.append("</body></html>");
			content.setText(sb.toString());
			content.setCaretPosition(0);
		}

		private static String buildMapUrl(final String coordinates, final String name){
			if(StringUtils.isNotBlank(coordinates)){
				final String[] parts = StringUtils.split(coordinates, ", \t\n\013\f\r");
				if(parts.length >= 2){
					try{
						final double lat = Double.parseDouble(parts[0]);
						final double lon = Double.parseDouble(parts[1]);
						return "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=14/" + lat + "/" + lon;
					}
					catch(final NumberFormatException ignored){
						// Fall through to the search by name.
					}
				}
			}
			if(StringUtils.isNotBlank(name))
				return "https://www.openstreetmap.org/search?query=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
			return null;
		}

		/**
		 * Helper to render a single row in the parents/children list with
		 * a clean, modern layout instead of standard HTML bullets.
		 */
		private static void appendRelationRow(final StringBuilder sb, final String name, final PlaceHierarchyService.PlaceRelation rel){
			sb.append("<div style='margin-bottom: 10px; line-height: 1.4;'>");
			// Place name
			sb.append("<b>").append(escape(name)).append("</b>");

			// Relation type in a subtle blue/grey
			if(rel.relationType() != null && !rel.relationType().isBlank())
				sb.append(" <span style='color: #0066cc; font-style: italic; font-size: 12px;'>— ")
					.append(escape(formatRelationType(rel.relationType())))
					.append("</span>");

			// Dates in a lighter, smaller font
			final String from = rel.validFrom();
			final String to = rel.validTo();
			if(from != null || to != null)
				sb.append(" <span style='color: #999999; font-size: 11px;'>[")
					.append(from != null? escape(from): "?")
					.append(" – ")
					.append(to != null? escape(to): "?")
					.append("]</span>");
			sb.append("</div>");
		}

		private static void appendRelationInfo(final StringBuilder sb, final PlaceHierarchyService.PlaceRelation rel){
			if(rel.relationType() != null && !rel.relationType().isBlank())
				sb.append(" <i>— ").append(escape(formatRelationType(rel.relationType()))).append("</i>");
			final String from = rel.validFrom();
			final String to = rel.validTo();
			if(from != null || to != null)
				sb.append(" <font color='#666'>[")
					.append(from != null? escape(from): "?")
					.append(" – ")
					.append(to != null? escape(to): "?")
					.append("]</font>");
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

		private final FLEFModel model;


		PlaceTreeCellRenderer(final FLEFModel model){
			this.model = model;
		}


		@Override
		public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
				final boolean expanded, final boolean leaf, final int row, final boolean hasFocus){
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

			if(value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof PlaceTreePayload payload){
				setText(buildLabel(payload));

				//a padlock signals a restricted or confidential place
				final PrivacyResolver.Effective privacy = PrivacyService.resolver(model)
					.describe(model.getRecordById(payload.place().id()));
				setIcon(PrivacyBadge.of(privacy));
				setToolTipText(PrivacyBadge.tooltip(privacy));
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
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		EventQueue.invokeLater(() -> {
			final PlaceHierarchyPanel panel = new PlaceHierarchyPanel(model);
			panel.setEditable(true);

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
