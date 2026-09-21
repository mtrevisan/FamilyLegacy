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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EgoNetworkGroupPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EgoNetworkIndividualPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Handles layout generation and panel hierarchy creation for the ego network canvas.
 */
class EgoNetworkLayoutBuilder{

	public static void build(final JPanel canvas, final EgoNode rootEgoNode, final FLEFModel model,
			final IndividualListener individualListener, final GroupListener groupListener,
			final Map<EgoNode, JPanel> nodeToPanelMap, final Map<FLEFRecord, JPanel> groupToPanelMap,
			final Set<EgoNode> parents, final Set<EgoNode> associates,
			final Set<FLEFRecord> groups, final Set<EgoNode> children,
			final int padding){
		canvas.setLayout(new MigLayout("ins " + padding + ",align center center", "[grow,center]", "[grow,center]"));

		final JPanel centerGrid = new JPanel(new MigLayout("ins 10",
			"[grow 100,sg col,fill][center][grow 100,sg col,fill]",
			"[grow 100,sg row,fill][center][grow 100,sg row,fill]"));
		centerGrid.setOpaque(false);

		// Center
		final JPanel egoContainer = createCardPanel(rootEgoNode, BoxPanelType.PRIMARY, model, individualListener, groupListener);
		nodeToPanelMap.put(rootEgoNode, egoContainer);
		centerGrid.add(egoContainer, "cell 1 1");

		// Top: Parents
		if(!parents.isEmpty()){
			final JPanel parentsContainer = createNodesContainer(parents, model, individualListener, groupListener, nodeToPanelMap);
			centerGrid.add(parentsContainer, "cell 1 0,align center bottom,gapbottom 15");
		}

		// Left: Partners & Associates
		final Set<EgoNode> partners = rootEgoNode.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER);
		if(!partners.isEmpty() || !associates.isEmpty()){
			final JPanel leftContainer = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,center]", "[grow,center]"));
			leftContainer.setOpaque(false);
			if(!partners.isEmpty())
				leftContainer.add(createNodesContainer(partners, model, individualListener, groupListener, nodeToPanelMap), "grow");
			if(!associates.isEmpty())
				leftContainer.add(createNodesContainer(associates, model, individualListener, groupListener, nodeToPanelMap), "grow");
			centerGrid.add(leftContainer, "cell 0 1,align right center,gapright 15");
		}

		// Right: Groups
		if(!groups.isEmpty()){
			final JPanel groupsContainer = createGroupsContainer(rootEgoNode, groups, model, groupListener, groupToPanelMap);
			centerGrid.add(groupsContainer, "cell 2 1,align left center,gapleft 15");
		}

		// Bottom: Children
		if(!children.isEmpty()){
			final JPanel childrenContainer = createNodesContainer(children, model, individualListener, groupListener, nodeToPanelMap);
			centerGrid.add(childrenContainer, "cell 1 2,align center top,gaptop 15");
		}

		canvas.add(centerGrid, "grow");
		canvas.revalidate();
		final Dimension contentPref = centerGrid.getPreferredSize();
		canvas.setPreferredSize(new Dimension(contentPref.width + 2 * padding, contentPref.height + 2 * padding));
		canvas.revalidate();
	}

	private static JPanel createNodesContainer(final Set<EgoNode> nodes, final FLEFModel model,
			final IndividualListener individualListener, final GroupListener groupListener,
			final Map<EgoNode, JPanel> nodeToPanelMap){
		final JPanel container = new JPanel(new MigLayout("ins 5,wrap 2", "[grow,center]", "[grow,center]"));
		container.setOpaque(false);

		for(final EgoNode node : nodes){
			final JPanel panel = createCardPanel(node, BoxPanelType.SECONDARY, model, individualListener, groupListener);
			final String tooltipText = buildTooltipText(node.getRelationsWithEgo());
			if(tooltipText != null)
				panel.setToolTipText(tooltipText);

			nodeToPanelMap.put(node, panel);
			container.add(panel, "grow");
		}
		return container;
	}

	private static JPanel createGroupsContainer(final EgoNode rootEgoNode, final Set<FLEFRecord> groups,
			final FLEFModel model, final GroupListener groupListener, final Map<FLEFRecord, JPanel> groupToPanelMap){
		final JPanel container = new JPanel(new MigLayout("ins 5,wrap 1", "[grow,center]", "[grow,center]"));
		container.setOpaque(false);

		for(final FLEFRecord groupRecord : groups){
			final GroupPanel panel = GroupPanel.create(BoxPanelType.SECONDARY, model)
				.withGroupData(GroupData.create(groupRecord))
				.withListener(groupListener, new EgoNetworkGroupPopupMenuFactory());

			final List<EgoNode.RelationInfo> relations = rootEgoNode.getGroupRelationInfo(groupRecord);
			final String tooltipText = buildTooltipText(relations);
			if(tooltipText != null)
				panel.setToolTipText(tooltipText);

			groupToPanelMap.put(groupRecord, panel);
			container.add(panel, "grow");
		}
		return container;
	}

	private static JPanel createCardPanel(final EgoNode node, final BoxPanelType boxType, final FLEFModel model,
			final IndividualListener individualListener, final GroupListener groupListener){
		final FLEFRecord record = node.getEgoRecord();
		if(record != null && GroupHandler.TYPE.equalsIgnoreCase(record.getTag())){
			final GroupPanel groupPanel = GroupPanel.create(boxType, model)
				.withGroupData(GroupData.create(record))
				.withListener(groupListener, new EgoNetworkGroupPopupMenuFactory());
			if(boxType == BoxPanelType.PRIMARY)
				groupPanel.withSelected(true);

			return groupPanel;
		}

		return IndividualPanel.create(boxType, model)
			.withIndividualData(node.getEgoData())
			.withListener(individualListener,
				(boxType == BoxPanelType.PRIMARY
					? EgoNetworkIndividualPopupMenuFactory.createForEgo()
					: EgoNetworkIndividualPopupMenuFactory.createForChild()));
	}

	private static String buildTooltipText(final List<EgoNode.RelationInfo> relations){
		if(relations == null || relations.isEmpty())
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		for(int i = 0; i < relations.size(); i++){
			final EgoNode.RelationInfo info = relations.get(i);
			if(i > 0)
				sb.append("<hr>");
			if(info.isInverse())
				sb.append(" <i>(Inverse)</i><br>");
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

}
