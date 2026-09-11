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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.Side;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;

import javax.swing.JPanel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Helper class to determine the context (child, parent) of a selected
 * {@link IndividualPanel} inside the ancestor tree.
 * <p>
 * The context describes what kind of entity the user clicked on and what
 * entities are naturally related to it. Callers must always test
 * {@link Context#type} before accessing the parent-specific fields
 * ({@code partnerPanel}, {@code side}, {@code targetId}), because they are
 * {@code null} when the context is {@link Context.Type#CHILD}.
 */
public final class TreeContextHelper{

	private TreeContextHelper(){}


	/**
	 * Represents the context of a selected panel.
	 */
	public static class Context{
		public enum Type{
			CHILD,
			PARENT
		}

		public final Type type;
		public final FLEFRecord individual;
		public final PartnersPanel partnerPanel;
		public final Side side;
		public final List<String> childrenId;
		public final String targetId;

		private Context(final Type type, final FLEFRecord individual, final PartnersPanel partnerPanel,
				final Side side, final List<String> childrenId, final String targetId){
			this.type = type;
			this.individual = individual;
			this.partnerPanel = partnerPanel;
			this.side = side;
			this.childrenId = (childrenId != null? List.copyOf(childrenId): List.of());
			this.targetId = targetId;
		}

		public static Context forChild(final FLEFRecord individual){
			return new Context(Type.CHILD, individual, null, null, null, null);
		}

		public static Context forPartner(final FLEFRecord individual, final PartnersPanel panel, final Side side,
			final List<String> childrenId, final String targetId){
			return new Context(Type.PARENT, individual, panel, side, childrenId, targetId);
		}

		/** Returns whether this is a CHILD context. */
		public boolean isChildContext(){
			return (type == Type.CHILD);
		}

		/** Returns whether this is a PARENT context. */
		public boolean isParentContext(){
			return (type == Type.PARENT);
		}

		/** Returns whether a partner panel is available. */
		public boolean hasPartnerPanel(){
			return (partnerPanel != null);
		}

		/** Returns whether the context carries at least one child id. */
		public boolean hasChildren(){
			return !childrenId.isEmpty();
		}
	}


	/**
	 * Determines the context of the selected panel.
	 *
	 * @param selectedPanel  the panel that was clicked
	 * @param nodeToPanelMap map from {@code TreeNode} to {@code PartnersPanel}
	 * @return the Context, or {@code null} if it cannot be determined
	 */
	static Context determineContext(final JPanel selectedPanel, final Map<TreeNode, PartnersPanel> nodeToPanelMap){
		if(selectedPanel == null)
			return null;

		// Extract the clicked individual record, if any
		final FLEFRecord currentRecord = (selectedPanel instanceof IndividualPanel ip && ip.getData() != null
			? ip.getData().getIndividual()
			: null);

		// Check if inside a SiblingsPanel -> CHILD context
		if(findContainingSiblingsPanel(selectedPanel.getParent()) != null)
			return Context.forChild(currentRecord);

		// Check if inside a PartnersPanel
		final PartnersPanel partnerPanel = PartnersPanel.findContainingPartnersPanel(selectedPanel.getParent());
		if(partnerPanel == null)
			return null;

		final Side side = (selectedPanel instanceof IndividualPanel ip? partnerPanel.getSideOf(ip): null);
		if(side == null)
			return null;

		final List<String> childrenId = collectChildrenIds(partnerPanel, nodeToPanelMap);

		final IndividualPanel oppositePanel = (side == Side.LEFT
			? partnerPanel.getMotherPanel()
			: partnerPanel.getFatherPanel());
		final String targetId = (oppositePanel != null && oppositePanel.getData() != null
			? oppositePanel.getData().getId()
			: null);

		return Context.forPartner(currentRecord, partnerPanel, side, childrenId, targetId);
	}

	/**
	 * Collects the ids of all children associated with the given couple
	 * panel. Handles both individual nodes (whose id is the individual)
	 * and couple-container nodes (whose id must be extracted from the
	 * associated siblings data).
	 */
	private static List<String> collectChildrenIds(final PartnersPanel partnerPanel,
		final Map<TreeNode, PartnersPanel> nodeToPanelMap){
		final List<String> childrenId = new ArrayList<>();
		for(final Map.Entry<TreeNode, PartnersPanel> entry : nodeToPanelMap.entrySet()){
			if(entry.getValue() != partnerPanel)
				continue;
			final TreeNode node = entry.getKey();
			if(node.getBiologicalChildrenData() == null)
				continue;
			if(node.getIndividual() == null){
				// Couple-container node: expand the siblings list
				node.getBiologicalChildrenData()
					.getSiblings()
					.stream()
					.map(IndividualData::getId)
					.forEach(childrenId::add);
			}
			else{
				// Individual node with its own children data
				final String id = node.getIndividualId();
				if(id != null)
					childrenId.add(id);
			}
		}
		return childrenId;
	}

	/**
	 * Finds the nearest SiblingsPanel ancestor, if any.
	 */
	private static SiblingsPanel findContainingSiblingsPanel(final Component parent){
		Component current = parent;
		while(current != null && !(current instanceof SiblingsPanel))
			current = current.getParent();
		return (SiblingsPanel)current;
	}

}
