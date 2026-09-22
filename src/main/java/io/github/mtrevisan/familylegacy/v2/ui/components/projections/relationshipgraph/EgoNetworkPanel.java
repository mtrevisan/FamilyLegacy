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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.SpatialNavigation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.CollapsibleBar;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph.services.lifespan.GlobalEventTimelinePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ViewportPanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Panel responsible for rendering an Ego-centric network (Hub & Spoke).
 * Extracted delegation logic to {@link EgoNetworkSelectionModel}, {@link EgoNetworkLayoutBuilder},
 * and {@link EgoNetworkActionHandler}.
 */
public class EgoNetworkPanel extends JPanel implements TreeChangeListener, IndividualListener, GroupListener{

	@Serial
	private static final long serialVersionUID = 7192849102849102941L;


	private static final Logger LOGGER = LoggerFactory.getLogger(EgoNetworkPanel.class);


	private static final Color BACKGROUND_COLOR = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;

	private static final String ENUM_TYPE_GROUP_MEMBER = "group_member";
	private static final String ENUM_TYPE_PART_OF = "part_of";

	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child"
	};
	/**
	 * Relationship types allowed when the Ego is an individual and the
	 * other entity is an individual. The list combines the five spouse
	 * types with the generic associate type.
	 */
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES = new String[]{
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner", "associate"
	};
	private static final String[] INDIVIDUAL_TO_GROUP_TYPES = new String[]{ENUM_TYPE_GROUP_MEMBER, "associate"};
	private static final String[] GROUP_TO_GROUP_TYPES = new String[]{ENUM_TYPE_PART_OF, "associate"};

	private static final String ACTION_TOGGLE_EGO_NETWORK_LAYOUT = "toggleEgoNetworkLayout";
	public static final String ACTION_TOGGLE_EVENTS_TIMELINE = "toggleEventsTimeline";

	/** Outer padding of the canvas around the center grid, in pixels. */
	private static final int CANVAS_PADDING = 20;


	private TreeLayout treeLayout;

	private final FLEFModel model;
	private final EgoNetworkService networkService;
	private final EgoNetworkMutator networkMutator;
	private final EgoNetworkSelectionModel selectionModel = new EgoNetworkSelectionModel();
	private final EgoNetworkActionHandler actionHandler;

	private String currentEgoId;
	private EgoNode rootEgoNode;
	private Set<EgoNode> parents;
	private Set<EgoNode> associates;
	private Set<FLEFRecord> groups;
	private Set<EgoNode> children;

	private final Map<EgoNode, JPanel> nodeToPanelMap = new HashMap<>();
	private final Map<FLEFRecord, JPanel> groupToPanelMap = new HashMap<>();

	/** Canvas that hosts the ego network layout. */
	private final NetworkCanvas networkCanvas = new NetworkCanvas();
	/** Scroll pane that hosts the canvas, so the network is fully scrollable. */
	private final JScrollPane networkScrollPane;
	/** Collapsible event timeline, filtered to the visible entities. */
	private final GlobalEventTimelinePanel timeline;
	/** Collapsible bar at the bottom that toggles the timeline. */
	private final CollapsibleBar toggleBar = new CollapsibleBar("Events");
	/** Whether the timeline is currently expanded. */
	private boolean timelineVisible;

	/** Optional callback invoked whenever an individual is selected. */
	private Consumer<String> selectionCallback;

	/** Optional callback invoked whenever a group is selected. */
	private Consumer<String> groupSelectionCallback;
	/** Optional callback invoked whenever the ego changes due to user navigation. */
	private Consumer<String> navigationCallback;
	/** When {@code true}, {@link #load(String)} does not notify the navigation callback. */
	private boolean suppressNavigationNotification;


	public EgoNetworkPanel(final TreeLayout treeLayout, final FLEFModel model){
		this.treeLayout = treeLayout;
		this.model = model;
		this.networkService = new EgoNetworkService(model);
		this.networkMutator = new EgoNetworkMutator(model, networkService, this);
		this.actionHandler = new EgoNetworkActionHandler(this, model, networkMutator);

		setBackground(BACKGROUND_COLOR);
		setOpaque(true);

		setupLayoutShortcut(this);


		// Scroll pane around the network canvas. The canvas implements
		// Scrollable so that when the content is smaller than the viewport
		// it stretches to fill the viewport (and MigLayout centers it),
		// while when it is larger, scrollbars appear.
		networkScrollPane = new JScrollPane(networkCanvas,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		networkScrollPane.setBorder(null);
		final JViewport viewport = networkScrollPane.getViewport();
		viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		viewport.setBackground(BACKGROUND_COLOR);
		networkScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		networkScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

		timeline = new GlobalEventTimelinePanel(model);
		timeline.setVisible(false);

		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(toggleBar, BorderLayout.NORTH);
		bottom.add(timeline, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		add(networkScrollPane, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		toggleBar.withListener(this::toggleTimeline);

		setupTimelineShortcut(this);
	}


	public void load(final String egoId){
		currentEgoId = egoId;
		selectionModel.clearSelection();

		refreshNetwork();

		if(!suppressNavigationNotification && navigationCallback != null && egoId != null)
			navigationCallback.accept(egoId);
	}

	private void refreshNetwork(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refreshNetwork);

			return;
		}

		rootEgoNode = networkService.buildEgoNetwork(currentEgoId);

		networkCanvas.removeAll();
		nodeToPanelMap.clear();
		groupToPanelMap.clear();

		if(rootEgoNode != null){
			parents = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARENT);
			associates = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE);
			groups = rootEgoNode.getGroupRecords();
			children = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.CHILD);

			EgoNetworkLayoutBuilder.build(networkCanvas, rootEgoNode, model, this, this,
				nodeToPanelMap, groupToPanelMap, parents, associates, groups, children, CANVAS_PADDING);
		}

		ViewportPanSupport.install(networkScrollPane, networkCanvas);

		if(selectionModel.getSelectedGroupId() != null && groupToPanelMap.keySet().stream()
				.noneMatch(g -> selectionModel.getSelectedGroupId().equals(g.getId())))
			selectionModel.setSelectedGroupId(null);

		if(selectionModel.getSelectedGroupId() == null
				&& (selectionModel.getSelectedIndividualId() == null
				|| nodeToPanelMap.keySet().stream().noneMatch(n -> selectionModel.getSelectedIndividualId().equals(n.getEgoId()))))
			selectionModel.setSelectedIndividualId(currentEgoId);

		applySelection();
		updateTimelineParticipants();

		networkCanvas.revalidate();
		networkCanvas.repaint();

		networkScrollPane.getViewport().setViewPosition(new Point(0, 0));

		revalidate();
		repaint();
		if(getParent() != null){
			getParent().revalidate();
			getParent().repaint();
		}
	}

	private void setupTimelineShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_T_STROKE, ACTION_TOGGLE_EVENTS_TIMELINE);
		actionMap.put(ACTION_TOGGLE_EVENTS_TIMELINE, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -7201948301928374917L;

			@Override
			public void actionPerformed(final ActionEvent e){
				toggleTimeline();
			}
		});
	}

	private void toggleTimeline(){
		timelineVisible = !timelineVisible;
		timeline.setVisible(timelineVisible);
		toggleBar.setExpanded(timelineVisible);

		revalidate();
		repaint();
	}

	/**
	 * Walks every panel currently hosted by the network and applies the
	 * selection state to individuals and groups.
	 */
	private void applySelection(){
		selectionModel.applySelection(nodeToPanelMap, groupToPanelMap);
	}

	private void updateTimelineParticipants(){
		final Set<String> ids = new HashSet<>();
		for(final EgoNode node : nodeToPanelMap.keySet())
			if(node.getEgoId() != null)
				ids.add(node.getEgoId());
		for(final FLEFRecord group : groupToPanelMap.keySet())
			if(group.getId() != null)
				ids.add(group.getId());
		timeline.setParticipantFilter(ids);
	}

	public EgoNetworkPanel withSelectionCallback(final Consumer<String> callback){
		selectionCallback = callback;

		return this;
	}

	public EgoNetworkPanel withGroupSelectionCallback(final Consumer<String> callback){
		groupSelectionCallback = callback;

		return this;
	}

	public String getSelectedEntityId(){
		return selectionModel.getSelectedEntityId();
	}

	/**
	 * Sub-panel that hosts the ego network layout and draws the connection
	 * lines.
	 * <p>
	 * The canvas implements {@link Scrollable} so that the enclosing
	 * {@link JScrollPane} stretches it to fill the viewport when the
	 * content is smaller than the viewport (and the MigLayout centers the
	 * content), while keeping the natural preferred size — and therefore
	 * showing scrollbars — when the content is larger.
	 */
	private final class NetworkCanvas extends JPanel implements Scrollable{

		@Serial
		private static final long serialVersionUID = -2194057381948273841L;


		NetworkCanvas(){
			setBackground(BACKGROUND_COLOR);
			setOpaque(true);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(CONNECTION_LINE_COLOR);
			g2.setStroke(PartnersPanel.CONNECTION_STROKE);

			EgoNetworkRenderer.drawNetworkLines(g2, rootEgoNode, nodeToPanelMap, groupToPanelMap, this);
		}


		/* ==================================================================
		 *                          Scrollable
		 * ================================================================== */

		@Override
		public Dimension getPreferredScrollableViewportSize(){
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction){
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction){
			return 64;
		}

		@Override
		public boolean getScrollableTracksViewportWidth(){
			final Container parent = getParent();
			return (parent != null && getPreferredSize().width < parent.getWidth());
		}

		@Override
		public boolean getScrollableTracksViewportHeight(){
			final Container parent = getParent();
			return (parent != null && getPreferredSize().height < parent.getHeight());
		}

	}

	@Override
	public void onTreeStructureChanged(final String rootEntityId){
		SwingUtilities.invokeLater(() -> load(rootEntityId));
	}

	@Override
	public void onEntityEdit(final FLEFRecord record){
		if(record == null)
			return;

		final FLEFRecord editedRecord = actionHandler.showEditRecordDialog(record);
		if(editedRecord != null){
			LOGGER.debug("Entity edited: {}", editedRecord.getId());

			networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
		}
	}

	@Override
	public void onIndividualSelected(final IndividualPanel selectedPanel, final FLEFRecord individual){
		if(individual == null || individual.getId() == null)
			return;

		selectionModel.setSelectedIndividualId(individual.getId());
		applySelection();
		if(selectionCallback != null)
			selectionCallback.accept(selectionModel.getSelectedIndividualId());
	}

	@Override
	public void onGroupSelected(final GroupPanel selectedPanel, final FLEFRecord group){
		if(group == null || group.getId() == null)
			return;

		selectionModel.setSelectedGroupId(group.getId());
		applySelection();
		if(groupSelectionCallback != null)
			groupSelectionCallback.accept(selectionModel.getSelectedGroupId());
	}

	@Override
	public void onRootEntitySelected(final FLEFRecord record){
		if(record == null || record.getId() == null)
			return;

		networkMutator.navigateToEgo(record.getId());
	}

	@Override
	public void onEntityRemove(final FLEFRecord record){
		if(record == null)
			return;

		final String name = GroupHandler.TYPE.equalsIgnoreCase(record.getTag())
			? GroupHandler.getInstance().getDisplayText(record, model)
			: IndividualHandler.getInstance().getDisplayText(record, model);

		final int confirm = JOptionPane.showConfirmDialog(
			this, "Are you sure you want to remove " + name + "?", "Confirm Removal",
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
		);
		if(confirm == JOptionPane.YES_OPTION)
			networkMutator.removeEntity(record, currentEgoId);
	}

	@Override
	public void onIndividualAddOrConnect(final IndividualPanel selectedPanel, final TreeOperation operation){
		actionHandler.performRelationOperation(
			(operation == TreeOperation.ADD
				? () -> actionHandler.showCreateRecordDialog(IndividualHandler.class)
				: () -> actionHandler.showSearchRecordDialog(IndividualHandler.class)),
			false, INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES, currentEgoId, rootEgoNode);
	}

	@Override
	public void onChildAddOrConnect(final IndividualPanel selectedPanel, final TreeOperation operation){
		actionHandler.performRelationOperation(
			(operation == TreeOperation.ADD
				? () -> actionHandler.showCreateRecordDialog(IndividualHandler.class)
				: () -> actionHandler.showSearchRecordDialog(IndividualHandler.class)),
			false, INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES, currentEgoId, rootEgoNode);
	}

	@Override
	public void onGroupAddOrConnect(final TreeOperation operation){
		final FLEFRecord egoRecord = (rootEgoNode != null? rootEgoNode.getEgoRecord(): null);
		final String[] allowedTypes = (egoRecord != null && GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag())
			? GROUP_TO_GROUP_TYPES
			: INDIVIDUAL_TO_GROUP_TYPES);

		actionHandler.performRelationOperation(
			(operation == TreeOperation.ADD
				? () -> actionHandler.showCreateRecordDialog(GroupHandler.class)
				: () -> actionHandler.showSearchRecordDialog(GroupHandler.class)),
			false, allowedTypes, currentEgoId, rootEgoNode);
	}

	@Override
	public void onIndividualUnlink(final IndividualPanel selectedPanel, final FLEFRecord record){
		if(record == null || selectedPanel == null)
			return;

		final UnlinkRelationshipsDialog dialog = actionHandler.showUnlinkRelationshipsDialog(record, currentEgoId,
			networkService, parents, associates, groups, children);
		final List<String> toRemove = dialog.getSelectedRelationshipIds();
		if(!toRemove.isEmpty()){
			networkMutator.removeRelationships(toRemove);
			networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
		}
	}

	@Override
	public void onGroupUnlink(final GroupPanel selectedPanel, final FLEFRecord record){
		if(record == null || selectedPanel == null)
			return;

		final UnlinkRelationshipsDialog dialog = actionHandler.showUnlinkRelationshipsDialog(record, currentEgoId,
			networkService, parents, associates, groups, children);
		final List<String> toRemove = dialog.getSelectedRelationshipIds();
		if(!toRemove.isEmpty()){
			networkMutator.removeRelationships(toRemove);
			networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
		}
	}

	@Override
	public void onEntityRelocate(final FLEFRecord group){
		if(group == null)
			return;

		LOGGER.debug("Relocate group {} to clipboard", group.getId());

		RelationClipboard.getInstance().setRecord(group);
	}

	@Override
	public void onIndividualPaste(final IndividualPanel selectedPanel){
		onEntityPaste();
	}

	@Override
	public void onGroupPaste(){
		onEntityPaste();
	}

	private void onEntityPaste(){
		final RelationClipboard clipboard = RelationClipboard.getInstance();
		if(!clipboard.hasRecord() || rootEgoNode == null)
			return;

		final FLEFRecord egoRecord = rootEgoNode.getEgoRecord();
		if(egoRecord == null)
			return;

		final FLEFRecord source = clipboard.getRecord();

		final boolean egoIsGroup = GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag());
		final boolean sourceIsGroup = GroupHandler.TYPE.equalsIgnoreCase(source.getTag());

		final String[] allowedTypes;
		if(egoIsGroup && sourceIsGroup)
			allowedTypes = GROUP_TO_GROUP_TYPES;
		else if(!egoIsGroup && !sourceIsGroup)
			allowedTypes = INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES;
		else
			allowedTypes = INDIVIDUAL_TO_GROUP_TYPES;

		actionHandler.performRelationOperationOnRecord(source, true, allowedTypes, currentEgoId, rootEgoNode);
		clipboard.clear();
	}

	public String getCurrentEgoId(){
		return currentEgoId;
	}

	/**
	 * Opens the edit dialog for the currently selected individual or
	 * group, if any.
	 * <p>
	 * The selection is the entity highlighted with the red border, which
	 * may differ from the current ego. When neither an individual nor a
	 * group is selected, the call is a no-op.
	 */
	public void editCurrentSelection(){
		final String id = selectionModel.getSelectedEntityId();
		if(id == null)
			return;

		final FLEFRecord record = model.getRecordById(id);
		if(record != null)
			onEntityEdit(record);
	}

	/**
	 * Moves the visual selection to the closest entity in the given
	 * direction.
	 * <p>
	 * The selection can be an individual or a group. When nothing is
	 * selected, the search starts from the current ego. When the selection
	 * has no neighbour in that direction, the call is a no-op.
	 *
	 * @param direction the direction to move; must not be {@code null}
	 */
	public void moveSelection(final SpatialNavigation.Direction direction){
		selectionModel.moveSelection(direction, currentEgoId, model, nodeToPanelMap, groupToPanelMap);
	}

	/**
	 * Confirms the current selection by re-rooting the ego network on it.
	 * <p>
	 * Behaves like a click on a name: the network is rebuilt around the
	 * selected entity, and the navigation is pushed into the history.
	 * When nothing is selected, the call is a no-op.
	 */
	public void egoConfirmSelection(final String selectedId){
		if(selectedId == null)
			return;

		final FLEFRecord record = model.getRecordById(selectedId);
		if(record != null)
			onRootEntitySelected(record);
	}

	public void setupLayoutShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_L_STROKE, ACTION_TOGGLE_EGO_NETWORK_LAYOUT);
		actionMap.put(ACTION_TOGGLE_EGO_NETWORK_LAYOUT, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -3819204812049102941L;

			@Override
			public void actionPerformed(final ActionEvent e){
				toggleLayout();
			}
		});
	}

	private void toggleLayout(){
		treeLayout = (treeLayout == TreeLayout.VERTICAL? TreeLayout.HORIZONTAL: TreeLayout.VERTICAL);
		refreshNetwork();

		final Window window = SwingUtilities.getWindowAncestor(this);
		if(window != null){
			window.pack();
			window.setLocationRelativeTo(null);
		}
	}

	/**
	 * Registers a callback invoked whenever the ego of the network changes
	 * due to user navigation (click on a name). Used by the enclosing
	 * container to record the navigation in the back/forward history.
	 *
	 * @param callback the callback; may be {@code null} to remove it
	 */
	public EgoNetworkPanel withNavigationCallback(final Consumer<String> callback){
		navigationCallback = callback;

		return this;
	}

	/**
	 * Loads the given individual as the new ego without notifying the
	 * navigation callback. Used by the back/forward history to move the
	 * cursor without generating a new history entry.
	 *
	 * @param egoId the individual id
	 */
	public void navigateTo(final String egoId){
		suppressNavigationNotification = true;
		try{
			load(egoId);
		}
		finally{
			suppressNavigationNotification = false;
		}
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final String modelUri = "/tests/TGMZ.flef";
		final String individualId = "I1";

		final String content;
		try(final InputStream is = EgoNetworkPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final EgoNetworkPanel panel = new EgoNetworkPanel(TreeLayout.VERTICAL, model);
			panel.load(individualId);

			final JFrame frame = new JFrame("Ego Network View");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
