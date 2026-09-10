package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";


	private TreeLayout treeLayout;
	private final FLEFModel model;
	private final EgoNetworkService networkService;

	private String currentEgoId;
	private EgoNode rootEgoNode;

	private final Map<EgoNode, JPanel> nodeToPanelMap = new HashMap<>();
	private final Map<FLEFRecord, JPanel> groupToPanelMap = new HashMap<>();

	private JPanel selectedPanel;


	public EgoNetworkPanel(final TreeLayout treeLayout, final FLEFModel model){
		this.treeLayout = treeLayout;
		this.model = model;
		this.networkService = new EgoNetworkService(model);

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

		final JPanel centerGrid = new JPanel(new MigLayout("ins 10", "[center][center][center]", "[center][center][center]"));
		centerGrid.setOpaque(false);

		// 1. Center: Central Ego Panel
		final JPanel egoContainer = createEgoContainer(rootEgoNode);
		centerGrid.add(egoContainer, "cell 1 1,grow");

		// 2. Top: Parents / Super-groups
		final Set<EgoNode> parents = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARENT);
		if(!parents.isEmpty()){
			final JPanel parentsContainer = createNodesContainer(parents);
			centerGrid.add(parentsContainer, "cell 1 0,grow,gapbottom 15");
		}

		// 3. Left: Partners & Associates
		final Set<EgoNode> partners = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER);
		final Set<EgoNode> associates = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE);
		if(!partners.isEmpty() || !associates.isEmpty()){
			final JPanel leftContainer = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,center]", "[grow,center]"));
			leftContainer.setOpaque(false);
			if(!partners.isEmpty())
				leftContainer.add(createNodesContainer(partners), "grow");
			if(!associates.isEmpty())
				leftContainer.add(createNodesContainer(associates), "grow");
			centerGrid.add(leftContainer, "cell 0 1, grow, gapright 15");
		}

		// 4. Right: Groups
		final Set<FLEFRecord> groups = rootEgoNode.getGroupRecords();
		if(!groups.isEmpty()){
			final JPanel groupsContainer = createGroupsContainer(groups);
			centerGrid.add(groupsContainer, "cell 2 1,grow,gapleft 15");
		}

		// 5. Bottom: Children / Sub-groups
		final Set<EgoNode> children = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.CHILD);
		if(!children.isEmpty()){
			final JPanel childrenContainer = createNodesContainer(children);
			centerGrid.add(childrenContainer, "cell 1 2,grow,gaptop 15");
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
				.withListener(this);

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
				.withListener(this);

		return IndividualPanel.create(boxType, model)
			.withIndividualData(node.getEgoData())
			.withListener(this);
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

			networkService.invalidateIndices();

			refreshNetwork();
		}
	}

	@Override
	public void onEntitySelected(final FLEFRecord record){
		if(record != null && record.getId() != null)
			loadNetwork(record.getId());
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
		if(confirm == JOptionPane.YES_OPTION){
			model.removeRecord(record.getId());

			networkService.invalidateIndices();

			refreshNetwork();
		}
	}

	@Override
	public void onPanelSelected(final JPanel panel){
		this.selectedPanel = panel;
	}

	@Override
	public void onIndividualAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateRecordDialog
			: this::showSearchRecordDialog);

		performRelationOperation(fnOperation, false);
	}

	@Override
	public void onChildAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateRecordDialog
			: this::showSearchRecordDialog);

		performRelationOperation(fnOperation, false);
	}

	/**
	 * Helper method to locate the corresponding EgoNode for a given UI panel.
	 */
	private EgoNode findNodeForPanel(final JPanel panel){
		if(panel == null)
			return null;

		for(final Map.Entry<EgoNode, JPanel> entry : nodeToPanelMap.entrySet())
			if(entry.getValue() == panel)
				return entry.getKey();
		return null;
	}

	@Override
	public void onGroupAddOrConnect(final TreeOperation operation){
		final Supplier<FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateRecordDialog
			: this::showSearchRecordDialog);

		performRelationOperation(fnOperation, false);
	}

	@Override
	public void showUnlinkDialog(final FLEFRecord record){
		if(record == null)
			return;

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final UnlinkRelationshipsDialog dialog = UnlinkRelationshipsDialog.create(parent, model, record.getId());
		dialog.setVisible(true);

		final List<String> selectedIds = dialog.getSelectedRelationshipIds();
		if(selectedIds.isEmpty())
			return;

		final int confirm = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to remove the selected relationships?",
			"Confirm Unlink",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm == JOptionPane.YES_OPTION){
			for(final String relationshipId : selectedIds)
				model.removeRecord(relationshipId);

			networkService.invalidateIndices();

			refreshNetwork();
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

		if(isPaste){
			final List<String> relationshipIds = extractRelationships(targetRecord.getId());
			for(final String relationshipId : relationshipIds)
				model.removeRecord(relationshipId);
		}

		networkService.invalidateIndices();

		refreshNetwork();
	}

	private List<String> extractRelationships(final String entityId){
		final List<String> result = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(entityId.equals(subjectId) || entityId.equals(targetId))
				result.add(relationship.getId());
		}
		return result;
	}

	private FLEFRecord showEditRecordDialog(final FLEFRecord record){
		if(GroupHandler.TYPE.equalsIgnoreCase(record.getTag()))
			return record;

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createEditDialog(parent, model, record);
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
