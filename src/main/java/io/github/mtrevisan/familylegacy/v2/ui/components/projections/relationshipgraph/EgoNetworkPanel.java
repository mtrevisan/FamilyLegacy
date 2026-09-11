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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Panel responsible for rendering an Ego-centric network (Hub & Spoke),
 * connecting an individual or group to parents, partners, children, groups,
 * and associates.
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
	 * Relationship types allowed when the Ego is an individual and the other
	 * entity is an individual. The list combines the five spouse types with
	 * the generic {@code associate} type, which the FLEF protocol permits
	 * between two individuals.
	 */
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES = new String[]{
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner", "associate"
	};
	private static final String[] INDIVIDUAL_TO_GROUP_TYPES = new String[]{ENUM_TYPE_GROUP_MEMBER, "associate"};
	private static final String[] GROUP_TO_GROUP_TYPES = new String[]{ENUM_TYPE_PART_OF, "associate"};

	private static final String ACTION_TOGGLE_EGO_NETWORK_LAYOUT = "toggleEgoNetworkLayout";


	private TreeLayout treeLayout;

	private final FLEFModel model;
	private final EgoNetworkService networkService;
	private final EgoNetworkMutator networkMutator;

	private String currentEgoId;
	private EgoNode rootEgoNode;
	private Set<EgoNode> parents;
	private Set<EgoNode> partners;
	private Set<EgoNode> associates;
	private Set<FLEFRecord> groups;
	private Set<EgoNode> children;

	private final Map<EgoNode, JPanel> nodeToPanelMap = new HashMap<>();
	private final Map<FLEFRecord, JPanel> groupToPanelMap = new HashMap<>();

	private JPanel selectedPanel;


	public EgoNetworkPanel(final TreeLayout treeLayout, final FLEFModel model){
		this.treeLayout = treeLayout;
		this.model = model;
		this.networkService = new EgoNetworkService(model);
		this.networkMutator = new EgoNetworkMutator(model, networkService, this);

		setBackground(BACKGROUND_COLOR_APPLICATION);
		setOpaque(true);

		setupLayoutShortcut(this);
	}


	public void loadNetwork(final String egoId){
		this.currentEgoId = egoId;

		refreshNetwork();
	}

	private void refreshNetwork(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refreshNetwork);

			return;
		}

		rootEgoNode = networkService.buildEgoNetwork(currentEgoId);

		removeAll();
		nodeToPanelMap.clear();
		groupToPanelMap.clear();

		if(rootEgoNode != null)
			buildLayout();

		revalidate();
		repaint();
		if(getParent() != null){
			getParent().revalidate();
			getParent().repaint();
		}
	}

	private void buildLayout(){
		setLayout(new MigLayout("ins 20,align center center", "[grow,center]", "[grow,center]"));

		final JPanel centerGrid = new JPanel(new MigLayout("ins 10",
			"[grow 100,sg col,fill][center][grow 100,sg col,fill]",
			"[grow 100,sg row,fill][center][grow 100,sg row,fill]"));
		centerGrid.setOpaque(false);

		// Center: Central Ego Panel
		final JPanel egoContainer = createEgoContainer(rootEgoNode);
		centerGrid.add(egoContainer, "cell 1 1");

		// Top: Parents / Super-groups
		parents = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARENT);
		if(!parents.isEmpty()){
			final JPanel parentsContainer = createNodesContainer(parents);
			centerGrid.add(parentsContainer, "cell 1 0,align center bottom,gapbottom 15");
		}

		// Left: Partners & Associates
		partners = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER);
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

		// Right: Groups
		groups = rootEgoNode.getGroupRecords();
		if(!groups.isEmpty()){
			final JPanel groupsContainer = createGroupsContainer(groups);
			centerGrid.add(groupsContainer, "cell 2 1,align left center,gapleft 15");
		}

		// Bottom: Children / Sub-groups
		children = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.CHILD);
		if(!children.isEmpty()){
			final JPanel childrenContainer = createNodesContainer(children);
			centerGrid.add(childrenContainer, "cell 1 2,align center top,gaptop 15");
		}

		add(centerGrid, "grow");
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

			// Add Tooltip Popup for Relationship Type and Role
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
		if(record != null && GroupHandler.TYPE.equalsIgnoreCase(record.getTag()))
			return GroupPanel.create(boxType, model)
				.withGroupData(GroupData.create(record))
				.withListener(this, new EgoNetworkGroupPopupMenuFactory());

		return IndividualPanel.create(boxType, model)
			.withIndividualData(node.getEgoData())
			.withListener(this,
				(boxType == BoxPanelType.PRIMARY
					? EgoNetworkIndividualPopupMenuFactory.createForEgo()
					: EgoNetworkIndividualPopupMenuFactory.createForChild()));
	}

	/**
	 * Builds an HTML tooltip from a list of relation metadata. All textual
	 * content is escaped before being embedded, so that role and type values
	 * coming from user-supplied data cannot break the HTML markup.
	 */
	private String buildTooltipText(final List<EgoNode.RelationInfo> relations){
		if(relations == null || relations.isEmpty())
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		for(int i = 0; i < relations.size(); i ++){
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
			return "";
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D g2){
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(CONNECTION_LINE_COLOR);
			g2.setStroke(PartnersPanel.CONNECTION_STROKE);

			EgoNetworkRenderer.drawNetworkLines(g2, rootEgoNode, nodeToPanelMap, groupToPanelMap, this);
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
	public void onEntitySelected(final FLEFRecord record){
		if(record != null && record.getId() != null)
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
	public void onEntitySelected(final JPanel panel){
		this.selectedPanel = panel;
	}

	@Override
	public void onIndividualAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? () -> showCreateRecordDialog(IndividualHandler.class)
			: () -> showSearchRecordDialog(IndividualHandler.class));

		performRelationOperation(fnOperation, false, INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES);
	}

	@Override
	public void onChildAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? () -> showCreateRecordDialog(IndividualHandler.class)
			: () -> showSearchRecordDialog(IndividualHandler.class));

		performRelationOperation(fnOperation, false, INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	}

	@Override
	public void onGroupAddOrConnect(final TreeOperation operation){
		// The entity being added / linked is always a Group, regardless of whether
		// the Ego is an Individual (member -> group) or a Group (sub-group -> super-group).
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

	/**
	 * Performs a relationship mutation starting from an already-resolved target record.
	 * <p>
	 * According to the FLEF protocol, the SUBJECT is the entity whose role is described by TYPE
	 * relative to the TARGET. For example, {@code biological_child(Alice -> John)} means Alice is
	 * the biological child of John.
	 */
	private void performRelationOperationOnRecord(final FLEFRecord targetRecord, final boolean isPaste,
		final String[] allowedTypes){
		if(currentEgoId == null || rootEgoNode == null)
			return;

		final FLEFRecord egoRecord = rootEgoNode.getEgoRecord();
		if(egoRecord == null)
			return;

		// On paste, first remove any pre-existing relationship between Ego and the target
		if(isPaste)
			networkMutator.unlinkRelationship(egoRecord, targetRecord, currentEgoId);

		// Ask the user to choose the FLEF relationship type (when more than one is applicable)
		final String selectedType = selectRelationshipType(targetRecord, allowedTypes);
		if(selectedType == null)
			return;

		// Resolve subject/target according to FLEF semantics
		final String[] pair = resolveSubjectTarget(selectedType, egoRecord, targetRecord);
		final String subjectId = pair[0];
		final String targetId = pair[1];

		networkMutator.createRelationship(subjectId, targetId, selectedType);
		networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
	}

	/**
	 * Resolves the FLEF subject/target orientation for the given relationship type.
	 *
	 * @param type        the relationship type
	 * @param egoRecord   the Ego record
	 * @param otherRecord the newly added/linked record
	 * @return a two-element array {@code [subjectId, targetId]}
	 */
	private String[] resolveSubjectTarget(final String type, final FLEFRecord egoRecord, final FLEFRecord otherRecord){
		final String egoId = egoRecord.getId();
		final String otherId = otherRecord.getId();
		final boolean egoIsGroup = GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag());

		// Child types: SUBJECT = child, TARGET = parent
		// The "other" record is always the child; the Ego is always the parent
		if(isChildType(type))
			return new String[]{otherId, egoId};

		// group_member: (Individual -> Group)
		// The individual is always the subject; the group is always the target
		if(ENUM_TYPE_GROUP_MEMBER.equals(type)){
			if(egoIsGroup)
				// Ego is the group, the other record is the individual member
				return new String[]{otherId, egoId};
			// Ego is the individual, the other record is the group
			return new String[]{egoId, otherId};
		}

		// part_of: (Group -> Group), sub-group -> super-group
		// When linking from the Ego center, the Ego is treated as the sub-group
		if(ENUM_TYPE_PART_OF.equals(type))
			return new String[]{egoId, otherId};

		// Symmetric types (spouse/partner, associate) or default
		return new String[]{egoId, otherId};
	}

	/**
	 * Shows the relationship type selection dialog when multiple types are allowed.
	 * Returns {@code null} if the user cancels.
	 */
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
	public void onEntityUnlink(final FLEFRecord record){
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
	public void onIndividualPaste(final FLEFRecord father, final FLEFRecord mother){
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

		// Pick the allowed type list based on the participant types
		final boolean egoIsGroup = GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag());
		final boolean sourceIsGroup = GroupHandler.TYPE.equalsIgnoreCase(source.getTag());

		final String[] allowedTypes;
		if(egoIsGroup && sourceIsGroup)
			allowedTypes = GROUP_TO_GROUP_TYPES;
		else if(!egoIsGroup && !sourceIsGroup)
			allowedTypes = INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES;
		else
			// Mixed case: individual <-> group (either direction)
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

	/**
	 * Shows the "create new record" dialog for the given handler type.
	 * Used by the individual / group callbacks so that the correct record dialog
	 * (IndividualRecordDialog vs GroupRecordDialog) is presented.
	 *
	 * @param handlerClass the handler class whose dialog should be used
	 *                     ({@code IndividualHandler.class} or {@code GroupHandler.class})
	 * @return the newly created record, or {@code null} if the user cancelled
	 */
	private FLEFRecord showCreateRecordDialog(final Class<? extends RecordTypeHandler<?>> handlerClass){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final RecordTypeHandler<?> handler = resolveHandler(handlerClass);

		final BaseRecordDialog dialog = handler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	/**
	 * Shows the record selection dialog (with optional inline creation) for the given handler type.
	 *
	 * @param handlerClass the handler class to search among
	 *                     ({@code IndividualHandler.class} or {@code GroupHandler.class})
	 * @return the selected record, or {@code null} if the user cancelled
	 */
	private FLEFRecord showSearchRecordDialog(final Class<? extends RecordTypeHandler<?>> handlerClass){
		final FLEFRecord[] result = {null};
		final Window parent = SwingUtilities.getWindowAncestor(this);

		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(
			parent, model,
			(record, handler) -> result[0] = record,
			handlerClass);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Resolves a handler class to its singleton instance.
	 * Only IndividualHandler and GroupHandler are currently supported.
	 *
	 * @param handlerClass the handler class
	 * @return the singleton handler instance
	 * @throws IllegalArgumentException if the class is not supported
	 */
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


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

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
