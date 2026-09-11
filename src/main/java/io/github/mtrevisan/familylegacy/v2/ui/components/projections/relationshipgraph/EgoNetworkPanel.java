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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import net.miginfocom.swing.MigLayout;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Panel responsible for rendering an Ego-centric network (Hub & Spoke),
 * connecting an individual or group to parents, partners, children, groups, and associates.
 */
public class EgoNetworkPanel extends JPanel implements TreeChangeListener, IndividualListener, GroupListener{

	@Serial
	private static final long serialVersionUID = 7192849102849102941L;


	private static final Logger LOGGER = LoggerFactory.getLogger(EgoNetworkPanel.class);


	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;

	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child"
	};
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_PARTNER_TYPES = new String[]{
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner"
	};
	private static final String[] INDIVIDUAL_TO_GROUP_TYPES = new String[]{"group_member", "associate"};
	private static final String[] GROUP_TO_GROUP_TYPES = new String[]{"part_of", "associate"};


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

		final JPanel centerGrid = new JPanel(new MigLayout("ins 10", "[grow 100,sg col,fill][center][grow 100,sg col,fill]", "[grow 100,sg row,fill][center][grow 100,sg row,fill]"));
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
			.withListener(this, new EgoNetworkIndividualPopupMenuFactory());
	}

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
					.append(info.type());
			if(info.role() != null){
				if(info.type() != null)
					sb.append("<br>");
				sb.append("<b>Role:</b> ")
					.append(info.role());
			}
		}
		sb.append("</html>");
		return sb.toString();
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
			? this::showCreateRecordDialog
			: this::showSearchRecordDialog);

		performRelationOperation(fnOperation, false, INDIVIDUAL_TO_INDIVIDUAL_PARTNER_TYPES);
	}

	@Override
	public void onChildAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateRecordDialog
			: this::showSearchRecordDialog);

		performRelationOperation(fnOperation, false, INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	}

	@Override
	public void onGroupAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateRecordDialog
			: this::showSearchRecordDialog);

		final FLEFRecord egoRecord = (rootEgoNode != null? rootEgoNode.getEgoRecord(): null);
		final String[] allowedTypes = (egoRecord != null && GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag())
			? GROUP_TO_GROUP_TYPES
			: INDIVIDUAL_TO_GROUP_TYPES);
		performRelationOperation(fnOperation, false, allowedTypes);
	}

	private void performRelationOperation(final Supplier<FLEFRecord> recordSupplier, final boolean isPaste,
			final String[] allowedTypes){
		final FLEFRecord targetRecord = recordSupplier.get();
		if(targetRecord == null || currentEgoId == null || rootEgoNode == null)
			return;

		final FLEFRecord egoRecord = rootEgoNode.getEgoRecord();
		if(egoRecord == null)
			return;

		if(!model.hasRecord(targetRecord.getId()))
			model.addRecord(targetRecord);

		if(isPaste)
			networkMutator.unlinkRelationship(egoRecord, targetRecord, currentEgoId);

		// Determine selected relationship type
		final String selectedType;
		if(allowedTypes.length == 1)
			selectedType = allowedTypes[0];
		else{
			final Window parent = SwingUtilities.getWindowAncestor(this);
			final String label = IndividualHandler.TYPE.equalsIgnoreCase(targetRecord.getTag())
				? IndividualHandler.getInstance().getDisplayText(targetRecord, model)
				: GroupHandler.getInstance().getDisplayText(targetRecord, model);

			final List<RelationshipTypeSelectionDialog.Item> items = List.of(
				new RelationshipTypeSelectionDialog.Item(label, allowedTypes[0])
			);

			final RelationshipTypeSelectionDialog dialog = new RelationshipTypeSelectionDialog(parent, items,
				allowedTypes);
			dialog.setVisible(true);

			final List<String> result = dialog.getSelectedTypes();
			if(result == null || result.isEmpty())
				return;

			selectedType = result.getFirst();
		}

		// Subject is ALWAYS the entity whose role is described relative to Target
		// e.g., child -> parent, spouse -> spouse, member -> group
		final String subjectId;
		final String targetId;

		if(isChildType(selectedType)){
			// Target is the child (subject), Ego is the parent (target)
			subjectId = targetRecord.getId();
			targetId = egoRecord.getId();
		}
		else{
			// Standard orientation (Ego -> Target)
			subjectId = egoRecord.getId();
			targetId = targetRecord.getId();
		}

		networkMutator.createRelationship(subjectId, targetId, selectedType);

		networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
	}

	private boolean isChildType(final String type){
		return ("biological_child".equals(type)
			|| "adoptive_child".equals(type)
			|| "foster_child".equals(type)
			|| "guarded_child".equals(type)
			|| "step_child".equals(type));
	}

	@Override
	public void onEntityUnlink(final FLEFRecord record){
		if(record == null || rootEgoNode == null)
			return;

		final Set<EgoNode> parents;
		final Set<EgoNode> partners;
		final Set<EgoNode> associates;
		final Set<FLEFRecord> groups;
		final Set<EgoNode> children;
		if(record.getId().equals(currentEgoId)){
			parents = this.parents;
			partners = this.partners;
			associates = this.associates;
			groups = this.groups;
			children = this.children;
		}
		else{
			final EgoNode node = networkService.buildEgoNetwork(record.getId());
			parents = node.getRelatedNodes(EgoNode.RelationshipCategory.PARENT);
			partners = node.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER);
			associates = node.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE);
			groups = node.getGroupRecords();
			children = node.getRelatedNodes(EgoNode.RelationshipCategory.CHILD);
		}
		//TODO create another UnlinkRelationshipsDialog where the following data is passed directly (the id I mean)

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final UnlinkRelationshipsDialog dialog = UnlinkRelationshipsDialog.create(parent, model, relationshipTypeFilter,
			record.getId());
		dialog.setVisible(true);

		final List<String> selectedIds = dialog.getSelectedRelationshipIds();
		if(selectedIds.isEmpty())
			return;

		// Confirm with the user
		final int confirm = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to remove the selected relationships?",
			"Confirm Unlink",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm == JOptionPane.YES_OPTION){
			networkMutator.unlinkRelationship(rootEgoNode.getEgoRecord(), record, currentEgoId);

			networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
		}
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

		final FLEFRecord source = clipboard.getRecord();
		performRelationOperation(
			() -> source,
			true
		);

		clipboard.clear();
	}

	private void performRelationOperation(final Supplier<FLEFRecord> recordSupplier, final boolean isPaste){
//		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap);
//		if(ctx == null)
//			return;

		final FLEFRecord targetRecord = recordSupplier.get();
		if(targetRecord == null || currentEgoId == null)
			return;

		// Ensure the record exists in the model
		if(!model.hasRecord(targetRecord.getId()))
			model.addRecord(targetRecord);

		final FLEFRecord egoRecord = rootEgoNode.getEgoRecord();
		if(egoRecord == null)
			return;

		if(isPaste)
			networkMutator.unlinkRelationship(egoRecord, targetRecord, currentEgoId);

		// Default relationship type depending on target entity type
		final String relationshipType = (GroupHandler.TYPE.equalsIgnoreCase(targetRecord.getTag())
			? "member"
			: "associate");

		// Create bidirectional or directed relationship
		networkMutator.createRelationship(egoRecord.getId(), targetRecord.getId(), relationshipType);

		networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
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

	private FLEFRecord showCreateRecordDialog(){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private FLEFRecord showSearchRecordDialog(){
		final FLEFRecord[] result = {null};
		final Window parent = SwingUtilities.getWindowAncestor(this);
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(parent, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);

		return result[0];
	}

	public String getCurrentEgoId(){
		return currentEgoId;
	}

	public void setupLayoutShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_L_STROKE, "toggleEgoNetworkLayout");
		actionMap.put("toggleEgoNetworkLayout", new AbstractAction(){
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
		final String egoId = "I1";

		final String content;
		try(final InputStream is = EgoNetworkPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final EgoNetworkPanel panel = new EgoNetworkPanel(TreeLayout.VERTICAL, model);
			panel.loadNetwork(egoId);

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
