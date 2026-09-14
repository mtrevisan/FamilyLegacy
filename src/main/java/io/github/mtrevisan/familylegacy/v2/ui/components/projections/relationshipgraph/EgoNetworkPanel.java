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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.SpatialNavigation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EgoNetworkGroupPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EgoNetworkIndividualPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.CollapsibleBar;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph.services.lifespan.GlobalEventTimelinePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ViewportPanSupport;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Panel responsible for rendering an Ego-centric network (Hub & Spoke),
 * connecting an individual or group to parents, partners, children,
 * groups, and associates.
 * <p>
 * The network canvas is hosted inside a {@link JScrollPane}, so that when
 * the layout (particularly the children row, which can grow arbitrarily)
 * exceeds the viewport, scrollbars appear and all boxes remain reachable.
 * <p>
 * Selection and navigation are two distinct actions:
 * <ul>
 *   <li><b>single click on a name label</b> navigates, re-rooting the
 *       network on that entity;</li>
 *   <li><b>single click anywhere else on a panel</b> selects the entity:
 *       the panel is highlighted with a red border and any detail panel
 *       observing the selection is populated, without re-rooting the
 *       network.</li>
 * </ul>
 * Two independent callbacks can be installed to forward the current
 * selection to external panels: one for individuals, one for groups.
 */
public class EgoNetworkPanel extends JPanel implements TreeChangeListener, IndividualListener, GroupListener{

	@Serial
	private static final long serialVersionUID = 7192849102849102941L;


	private static final Logger LOGGER = LoggerFactory.getLogger(EgoNetworkPanel.class);


	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);
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

	/**
	 * Id of the currently selected individual, or {@code null} when no
	 * individual is selected.
	 */
	private String selectedIndividualId;

	/**
	 * Id of the currently selected group, or {@code null} when no group
	 * is selected. A group and an individual cannot be selected at the
	 * same time.
	 */
	private String selectedGroupId;

	/** Optional callback invoked whenever the ego changes due to user navigation. */
	private Consumer<String> navigationCallback;
	/** When {@code true}, {@link #loadNetwork(String)} does not notify the navigation callback. */
	private boolean suppressNavigationNotification;


	public EgoNetworkPanel(final TreeLayout treeLayout, final FLEFModel model){
		this.treeLayout = treeLayout;
		this.model = model;
		this.networkService = new EgoNetworkService(model);
		this.networkMutator = new EgoNetworkMutator(model, networkService, this);

		setBackground(BACKGROUND_COLOR_APPLICATION);
		setOpaque(true);

		setupLayoutShortcut(this);


		// Scroll pane around the network canvas. The canvas implements
		// Scrollable so that when the content is smaller than the viewport
		// it stretches to fill the viewport (and MigLayout centers it),
		// while when it is larger, scrollbars appear.
		this.networkScrollPane = new JScrollPane(networkCanvas,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.networkScrollPane.setBorder(null);
		this.networkScrollPane.getVerticalScrollBar()
			.setUnitIncrement(16);
		this.networkScrollPane.getHorizontalScrollBar()
			.setUnitIncrement(16);
		this.networkScrollPane.setBackground(BACKGROUND_COLOR_APPLICATION);
		this.networkScrollPane.getViewport()
			.setBackground(BACKGROUND_COLOR_APPLICATION);
		this.networkScrollPane.getViewport()
			.setOpaque(true);

		this.timeline = new GlobalEventTimelinePanel(model);
		this.timeline.setVisible(false);

		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(toggleBar, BorderLayout.NORTH);
		bottom.add(timeline, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		add(networkScrollPane, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		toggleBar.withListener(this::toggleTimeline);

		setupTimelineShortcut(this);
	}


	public void loadNetwork(final String egoId){
		this.currentEgoId = egoId;
		this.selectedIndividualId = null;
		this.selectedGroupId = null;

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

		if(rootEgoNode != null)
			buildLayout();

		ViewportPanSupport.install(networkScrollPane, networkCanvas);

		// Preserve the selection when the selected entity is still part
		// of the network. Otherwise, fall back to the current ego.
		if(selectedGroupId != null && groupToPanelMap.keySet().stream()
			.noneMatch(g -> selectedGroupId.equals(g.getId())))
			selectedGroupId = null;
		if(selectedGroupId == null
			&& (selectedIndividualId == null
			|| nodeToPanelMap.keySet().stream()
			.noneMatch(n -> selectedIndividualId.equals(n.getEgoId()))))
			selectedIndividualId = currentEgoId;

		applySelection();
		updateTimelineParticipants();

		networkCanvas.revalidate();
		networkCanvas.repaint();

		// Reset the scroll position to the top-left when the network is
		// rebuilt, so that the user sees the new layout from its origin.
		networkScrollPane.getViewport()
			.setViewPosition(new Point(0, 0));

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
		for(final Map.Entry<EgoNode, JPanel> entry : nodeToPanelMap.entrySet()){
			final EgoNode node = entry.getKey();
			final JPanel panel = entry.getValue();
			final boolean isSelected = (selectedIndividualId != null
				&& selectedIndividualId.equals(node.getEgoId()));

			applyIndividualSelectionToPanel(panel, isSelected);
		}
		for(final Map.Entry<FLEFRecord, JPanel> entry : groupToPanelMap.entrySet()){
			final FLEFRecord group = entry.getKey();
			final JPanel panel = entry.getValue();
			final boolean isSelected = (selectedGroupId != null
				&& selectedGroupId.equals(group.getId()));

			applyGroupSelectionToPanel(panel, isSelected);
		}
	}

	private static void applyIndividualSelectionToPanel(final Component panel, final boolean isSelected){
		if(panel instanceof IndividualPanel individualPanel)
			individualPanel.withSelected(isSelected);
		if(panel instanceof Container container)
			for(final Component child : container.getComponents())
				applyIndividualSelectionToPanel(child, isSelected);
	}

	private static void applyGroupSelectionToPanel(final Component panel, final boolean isSelected){
		if(panel instanceof GroupPanel groupPanel)
			groupPanel.withSelected(isSelected);
		if(panel instanceof Container container)
			for(final Component child : container.getComponents())
				applyGroupSelectionToPanel(child, isSelected);
	}

	private void updateTimelineParticipants(){
		final Set<String> ids = new HashSet<>();
		for(final EgoNode node : nodeToPanelMap.keySet()){
			final String id = node.getEgoId();
			if(id != null)
				ids.add(id);
		}
		for(final FLEFRecord group : groupToPanelMap.keySet()){
			final String id = group.getId();
			if(id != null)
				ids.add(id);
		}
		timeline.setParticipantFilter(ids);
	}

	private void buildLayout(){
		networkCanvas.setLayout(new MigLayout("ins " + CANVAS_PADDING + ",align center center",
			"[grow,center]", "[grow,center]"));

		final JPanel centerGrid = new JPanel(new MigLayout("ins 10",
			"[grow 100,sg col,fill][center][grow 100,sg col,fill]",
			"[grow 100,sg row,fill][center][grow 100,sg row,fill]"));
		centerGrid.setOpaque(false);

		// Center: Central Ego Panel.
		final JPanel egoContainer = createEgoContainer(rootEgoNode);
		centerGrid.add(egoContainer, "cell 1 1");

		// Top: Parents / Super-groups.
		parents = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARENT);
		if(!parents.isEmpty()){
			final JPanel parentsContainer = createNodesContainer(parents);
			centerGrid.add(parentsContainer, "cell 1 0,align center bottom,gapbottom 15");
		}

		// Left: Partners & Associates.
		final Set<EgoNode> partners = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER);
		associates = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE);
		if(!partners.isEmpty() || !associates.isEmpty()){
			final JPanel leftContainer = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,center]", "[grow,center]"));
			leftContainer.setOpaque(false);
			if(!partners.isEmpty())
				leftContainer.add(createNodesContainer(partners), "grow");
			if(!associates.isEmpty())
				leftContainer.add(createNodesContainer(associates), "grow");
			centerGrid.add(leftContainer, "cell 0 1,align right center,gapright 15");
		}

		// Right: Groups.
		groups = rootEgoNode.getGroupRecords();
		if(!groups.isEmpty()){
			final JPanel groupsContainer = createGroupsContainer(groups);
			centerGrid.add(groupsContainer, "cell 2 1,align left center,gapleft 15");
		}

		// Bottom: Children / Sub-groups.
		children = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.CHILD);
		if(!children.isEmpty()){
			final JPanel childrenContainer = createNodesContainer(children);
			centerGrid.add(childrenContainer, "cell 1 2,align center top,gaptop 15");
		}

		networkCanvas.add(centerGrid, "grow");

		// Compute the content preferred size after the grid has been
		// fully populated, and propagate it to the canvas, so that the
		// enclosing scroll pane can decide whether to show scrollbars.
		networkCanvas.revalidate();
		final Dimension contentPref = centerGrid.getPreferredSize();
		networkCanvas.setPreferredSize(new Dimension(
			contentPref.width + 2 * CANVAS_PADDING,
			contentPref.height + 2 * CANVAS_PADDING));
		networkCanvas.revalidate();
	}

	private JPanel createEgoContainer(final EgoNode node){
		final JPanel container = createCardPanel(node, BoxPanelType.PRIMARY);

		nodeToPanelMap.put(node, container);
		return container;
	}

	private JPanel createNodesContainer(final Set<EgoNode> nodes){
		final JPanel container = new JPanel(new MigLayout("ins 5,wrap 2", "[grow,center]", "[grow,center]"));
		container.setOpaque(false);

		for(final EgoNode node : nodes){
			final JPanel panel = createCardPanel(node, BoxPanelType.SECONDARY);

			final String tooltipText = buildTooltipText(node.getRelationsWithEgo());
			if(tooltipText != null)
				panel.setToolTipText(tooltipText);

			nodeToPanelMap.put(node, panel);

			container.add(panel, "grow");
		}
		return container;
	}

	private JPanel createGroupsContainer(final Set<FLEFRecord> groups){
		final JPanel container = new JPanel(new MigLayout("ins 5,wrap 1", "[grow,center]", "[grow,center]"));
		container.setOpaque(false);

		for(final FLEFRecord groupRecord : groups){
			final GroupPanel panel = GroupPanel.create(BoxPanelType.SECONDARY, model)
				.withGroupData(GroupData.create(groupRecord))
				.withListener(this, new EgoNetworkGroupPopupMenuFactory());

			final List<EgoNode.RelationInfo> relations = rootEgoNode.getGroupRelationInfo(groupRecord);
			final String tooltipText = buildTooltipText(relations);
			if(tooltipText != null)
				panel.setToolTipText(tooltipText);

			groupToPanelMap.put(groupRecord, panel);
			container.add(panel, "grow");
		}
		return container;
	}

	private JPanel createCardPanel(final EgoNode node, final BoxPanelType boxType){
		final FLEFRecord record = node.getEgoRecord();
		if(record != null && GroupHandler.TYPE.equalsIgnoreCase(record.getTag())){
			final GroupPanel groupPanel = GroupPanel.create(boxType, model)
				.withGroupData(GroupData.create(record))
				.withListener(this, new EgoNetworkGroupPopupMenuFactory());
			if(boxType == BoxPanelType.PRIMARY)
				groupPanel.withSelected(true);

			return groupPanel;
		}

		return IndividualPanel.create(boxType, model)
			.withIndividualData(node.getEgoData())
			.withListener(this,
				(boxType == BoxPanelType.PRIMARY
					? EgoNetworkIndividualPopupMenuFactory.createForEgo()
					: EgoNetworkIndividualPopupMenuFactory.createForChild()));
	}

	private String buildTooltipText(final List<EgoNode.RelationInfo> relations){
		if(relations == null || relations.isEmpty())
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		for(int i = 0; i < relations.size(); i++){
			final EgoNode.RelationInfo info = relations.get(i);
			if(i > 0)
				sb.append("<hr>");
			if(info.isInverse())
				sb.append(" <i>(Inverse)</i>")
					.append("<br>");
			if(info.type() != null)
				sb.append("<b>Type:</b> ")
					.append(escapeHtml(info.type()));
			if(info.role() != null){
				if(info.type() != null)
					sb.append("<br>");
				sb.append("<b>Role:</b> ")
					.append(escapeHtml(info.role()));
			}
		}
		sb.append("</html>");
		return sb.toString();
	}

	private static String escapeHtml(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	public EgoNetworkPanel withSelectionCallback(final Consumer<String> callback){
		this.selectionCallback = callback;

		return this;
	}

	public EgoNetworkPanel withGroupSelectionCallback(final Consumer<String> callback){
		this.groupSelectionCallback = callback;

		return this;
	}

	public String getSelectedEntityId(){
		return (selectedIndividualId != null? selectedIndividualId: selectedGroupId);
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
			setBackground(BACKGROUND_COLOR_APPLICATION);
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
		public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation,
			final int direction){
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation,
			final int direction){
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
		SwingUtilities.invokeLater(() -> loadNetwork(rootEntityId));
	}

	@Override
	public void onEntityEdit(final FLEFRecord record){
		if(record == null)
			return;

		final FLEFRecord editedRecord = showEditRecordDialog(record);
		if(editedRecord != null){
			LOGGER.debug("Entity edited: {}", editedRecord.getId());

			networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
		}
	}

	@Override
	public void onIndividualSelected(final IndividualPanel selectedPanel, final FLEFRecord individual){
		if(individual == null || individual.getId() == null)
			return;

		selectedIndividualId = individual.getId();
		selectedGroupId = null;

		applySelection();
		if(selectionCallback != null)
			selectionCallback.accept(selectedIndividualId);
	}

	@Override
	public void onGroupSelected(final GroupPanel selectedPanel, final FLEFRecord group){
		if(group == null || group.getId() == null)
			return;

		selectedGroupId = group.getId();
		selectedIndividualId = null;

		applySelection();
		if(groupSelectionCallback != null)
			groupSelectionCallback.accept(selectedGroupId);
	}

	@Override
	public void onEntitySelected(final FLEFRecord record){
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
			this,
			"Are you sure you want to remove " + name + "?",
			"Confirm Removal",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if(confirm == JOptionPane.YES_OPTION)
			networkMutator.removeEntity(record, currentEgoId);
	}

	@Override
	public void onIndividualAddOrConnect(final IndividualPanel selectedPanel, final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? () -> showCreateRecordDialog(IndividualHandler.class)
			: () -> showSearchRecordDialog(IndividualHandler.class));

		performRelationOperation(fnOperation, false, INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES);
	}

	@Override
	public void onChildAddOrConnect(final IndividualPanel selectedPanel, final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? () -> showCreateRecordDialog(IndividualHandler.class)
			: () -> showSearchRecordDialog(IndividualHandler.class));

		performRelationOperation(fnOperation, false, INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	}

	@Override
	public void onGroupAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? () -> showCreateRecordDialog(GroupHandler.class)
			: () -> showSearchRecordDialog(GroupHandler.class));

		final FLEFRecord egoRecord = (rootEgoNode != null? rootEgoNode.getEgoRecord(): null);
		final String[] allowedTypes = (egoRecord != null && GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag())
			? GROUP_TO_GROUP_TYPES
			: INDIVIDUAL_TO_GROUP_TYPES);

		performRelationOperation(fnOperation, false, allowedTypes);
	}

	private void performRelationOperation(final Supplier<FLEFRecord> recordSupplier, final boolean isPaste,
		final String[] allowedTypes){
		final FLEFRecord targetRecord = recordSupplier.get();
		if(targetRecord == null)
			return;

		performRelationOperationOnRecord(targetRecord, isPaste, allowedTypes);
	}

	private void performRelationOperationOnRecord(final FLEFRecord targetRecord, final boolean isPaste,
		final String[] allowedTypes){
		if(currentEgoId == null || rootEgoNode == null)
			return;

		final FLEFRecord egoRecord = rootEgoNode.getEgoRecord();
		if(egoRecord == null)
			return;

		if(isPaste)
			networkMutator.unlinkRelationship(egoRecord, targetRecord, currentEgoId);

		final String selectedType = selectRelationshipType(targetRecord, allowedTypes);
		if(selectedType == null)
			return;

		final String[] pair = resolveSubjectTarget(selectedType, egoRecord, targetRecord);
		final String subjectId = pair[0];
		final String targetId = pair[1];

		networkMutator.createRelationship(subjectId, targetId, selectedType);
		networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
	}

	private String[] resolveSubjectTarget(final String type, final FLEFRecord egoRecord, final FLEFRecord otherRecord){
		final String egoId = egoRecord.getId();
		final String otherId = otherRecord.getId();
		final boolean egoIsGroup = GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag());

		if(isChildType(type))
			return new String[]{otherId, egoId};

		if(ENUM_TYPE_GROUP_MEMBER.equals(type)){
			if(egoIsGroup)
				return new String[]{otherId, egoId};
			return new String[]{egoId, otherId};
		}

		if(ENUM_TYPE_PART_OF.equals(type))
			return new String[]{egoId, otherId};

		return new String[]{egoId, otherId};
	}

	private String selectRelationshipType(final FLEFRecord targetRecord, final String[] allowedTypes){
		if(allowedTypes.length == 1)
			return allowedTypes[0];

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final String label = (GroupHandler.TYPE.equalsIgnoreCase(targetRecord.getTag())
			? GroupHandler.getInstance().getDisplayText(targetRecord, model)
			: IndividualHandler.getInstance().getDisplayText(targetRecord, model));

		final List<RelationshipTypeSelectionDialog.Item> items = List.of(
			new RelationshipTypeSelectionDialog.Item(label, allowedTypes[0])
		);

		final RelationshipTypeSelectionDialog dialog = new RelationshipTypeSelectionDialog(parent, items, allowedTypes);
		dialog.setVisible(true);

		final List<String> result = dialog.getSelectedTypes();
		return (result == null || result.isEmpty()? null: result.getFirst());
	}

	private boolean isChildType(final String type){
		return ArrayUtils.contains(INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES, type.toLowerCase(Locale.ROOT));
	}

	@Override
	public void onIndividualUnlink(final IndividualPanel selectedPanel, final FLEFRecord record){
		if(record == null || selectedPanel == null)
			return;

		final UnlinkRelationshipsDialog dialog = showUnlinkRelationshipsDialog(record);
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

		final UnlinkRelationshipsDialog dialog = showUnlinkRelationshipsDialog(record);
		final List<String> toRemove = dialog.getSelectedRelationshipIds();
		if(!toRemove.isEmpty()){
			networkMutator.removeRelationships(toRemove);
			networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
		}
	}

	private UnlinkRelationshipsDialog showUnlinkRelationshipsDialog(final FLEFRecord record){
		final Set<EgoNode> parents;
		final Set<EgoNode> associates;
		final Set<FLEFRecord> groups;
		final Set<EgoNode> children;
		if(Objects.equals(record.getId(), currentEgoId)){
			parents = this.parents;
			associates = this.associates;
			groups = this.groups;
			children = this.children;
		}
		else{
			final EgoNode node = networkService.buildEgoNetwork(record.getId());
			parents = node.getRelatedNodes(EgoNode.RelationshipCategory.PARENT);
			associates = node.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE);
			groups = node.getGroupRecords();
			children = node.getRelatedNodes(EgoNode.RelationshipCategory.CHILD);
		}
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final UnlinkRelationshipsDialog dialog = new UnlinkRelationshipsDialog(parent, model, record.getId(),
			extractNodeIds(parents), extractNodeIds(associates), extractRecordIds(groups),
			extractNodeIds(children));
		dialog.setVisible(true);

		return dialog;
	}

	private static Set<String> extractNodeIds(final Set<EgoNode> nodes){
		return nodes.stream()
			.map(EgoNode::getEgoId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	private static Set<String> extractRecordIds(final Set<FLEFRecord> records){
		return records.stream()
			.map(FLEFRecord::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	@Override
	public void onEntityRelocate(final FLEFRecord group){
		if(group == null)
			return;

		LOGGER.debug("Relocate group {} to clipboard", group.getId());

		RelationClipboard.getInstance()
			.setRecord(group);
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
		if(!clipboard.hasRecord())
			return;

		if(rootEgoNode == null)
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

		performRelationOperationOnRecord(source, true, allowedTypes);

		clipboard.clear();
	}

	private FLEFRecord showEditRecordDialog(final FLEFRecord record){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final RecordTypeHandler<?> handler = (IndividualHandler.TYPE.equalsIgnoreCase(record.getTag())
			? IndividualHandler.getInstance()
			: GroupHandler.getInstance());
		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, record);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private FLEFRecord showCreateRecordDialog(final Class<? extends RecordTypeHandler<?>> handlerClass){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final RecordTypeHandler<?> handler = resolveHandler(handlerClass);

		final BaseRecordDialog dialog = handler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private FLEFRecord showSearchRecordDialog(final Class<? extends RecordTypeHandler<?>> handlerClass){
		final FLEFRecord[] result = {null};
		final Window parent = SwingUtilities.getWindowAncestor(this);

		@SuppressWarnings("unchecked") final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(
			parent, model,
			(record, handler) -> result[0] = record,
			handlerClass);
		dialog.setVisible(true);

		return result[0];
	}

	private static RecordTypeHandler<?> resolveHandler(final Class<? extends RecordTypeHandler<?>> handlerClass){
		if(GroupHandler.class.equals(handlerClass))
			return GroupHandler.getInstance();

		if(IndividualHandler.class.equals(handlerClass))
			return IndividualHandler.getInstance();

		throw new IllegalArgumentException("Unsupported handler class: " + handlerClass);
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
		final String id = (selectedIndividualId != null? selectedIndividualId: selectedGroupId);
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
		final Map<String, Rectangle> bounds = collectVisibleBounds();
		if(bounds.isEmpty())
			return;

		String current = (selectedIndividualId != null? selectedIndividualId: selectedGroupId);
		if(current == null || !bounds.containsKey(current))
			current = currentEgoId;
		if(current == null || !bounds.containsKey(current))
			return;

		final String next = SpatialNavigation.next(bounds, current, direction);
		if(next == null)
			return;

		// Determine whether the target is an individual or a group.
		final FLEFRecord record = model.getRecordById(next);
		if(record != null && GroupHandler.TYPE.equalsIgnoreCase(record.getTag())){
			if(next.equals(selectedGroupId))
				return;
			selectedGroupId = next;
			selectedIndividualId = null;
		}
		else{
			if(next.equals(selectedIndividualId))
				return;
			selectedIndividualId = next;
			selectedGroupId = null;
		}

		applySelection();
	}

	/**
	 * Confirms the current selection by re-rooting the ego network on it.
	 * <p>
	 * Behaves like a click on a name: the network is rebuilt around the
	 * selected entity, and the navigation is pushed into the history.
	 * When nothing is selected, the call is a no-op.
	 */
	public void confirmSelection(){
		final String id = (selectedIndividualId != null? selectedIndividualId: selectedGroupId);
		if(id == null)
			return;

		final FLEFRecord record = model.getRecordById(id);
		if(record != null)
			onEntitySelected(record);
	}

	/**
	 * Collects the on-screen bounds of every visible individual and group
	 * panel.
	 */
	private Map<String, Rectangle> collectVisibleBounds(){
		final Map<String, Rectangle> result = new LinkedHashMap<>();
		for(final Map.Entry<EgoNode, JPanel> entry : nodeToPanelMap.entrySet()){
			final String id = entry.getKey().getEgoId();
			if(id != null)
				addBounds(id, entry.getValue(), result);
		}
		for(final Map.Entry<FLEFRecord, JPanel> entry : groupToPanelMap.entrySet()){
			final String id = entry.getKey().getId();
			if(id != null)
				addBounds(id, entry.getValue(), result);
		}
		return result;
	}

	private static void addBounds(final String id, final Component component,
		final Map<String, Rectangle> out){
		try{
			final Point p = component.getLocationOnScreen();
			out.putIfAbsent(id, new Rectangle(p.x, p.y,
				component.getWidth(), component.getHeight()));
		}
		catch(final java.awt.IllegalComponentStateException ignored){
			// The panel is not showing; skip it.
		}
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
		this.treeLayout = (this.treeLayout == TreeLayout.VERTICAL
			? TreeLayout.HORIZONTAL
			: TreeLayout.VERTICAL);

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
		this.navigationCallback = callback;

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
			loadNetwork(egoId);
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
			panel.loadNetwork(individualId);

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
