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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
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
 * Helper class to determine the context (child, parent, partner) of a selected IndividualPanel.
 */
final class TreeContextHelper{

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
			this.childrenId = childrenId;
			this.targetId = targetId;
		}

		public static Context forChild(final FLEFRecord individual){
			return new Context(Type.CHILD, individual, null, null, null, null);
		}

		public static Context forPartner(final FLEFRecord individual, final PartnersPanel panel, final Side side,
				final List<String> childrenId, final String targetId){
			return new Context(Type.PARENT, individual, panel, side, childrenId, targetId);
		}
	}


	/**
	 * Determines the context of the selected panel.
	 *
	 * @param selectedPanel the panel that was clicked
	 * @param nodeToPanelMap map from AncestorNode to PartnersPanel (to find child ID)
	 * @return the Context, or {@code null} if it cannot be determined
	 */
	public static Context determineContext(final JPanel selectedPanel,
			final Map<TreeNode, PartnersPanel> nodeToPanelMap, final FLEFModel model){
		if(selectedPanel == null)
			return null;

		// Extract current individual record from the clicked panel if present
		final FLEFRecord currentRecord = (selectedPanel instanceof IndividualPanel individualPanel
				&& individualPanel.getData() != null
			? individualPanel.getData().getIndividual()
			: null);

		// Check if inside a SiblingsPanel -> CHILD context
		final Component parent = findContainingSiblingsPanel(selectedPanel.getParent());
		if(parent != null)
			return Context.forChild(currentRecord);

		// Check if inside a PartnersPanel
		final PartnersPanel partnerPanel = PartnersPanel.findContainingPartnersPanel(selectedPanel.getParent());
		if(partnerPanel == null)
			return null;

		final Side side = (selectedPanel instanceof IndividualPanel individualPanel
			? partnerPanel.getSideOf(individualPanel)
			: null);
		if(side == null)
			return null;

		// Context: add a parent to the existing individual on the opposite side and to the children associated with
		// this PartnersPanel
		final List<String> childrenId = new ArrayList<>();
		// For the couple container, children are stored here
		nodeToPanelMap.entrySet().stream()
			.filter(entry -> entry.getValue() == partnerPanel)
			.map(Map.Entry::getKey)
			.filter(node -> node.getBiologicalChildrenData() != null)
			.forEachOrdered(node -> {
				if(node.getIndividual() == null)
					node.getBiologicalChildrenData().getSiblings().stream()
						.map(IndividualData::getId)
						.forEach(childrenId::add);
				else
					childrenId.add(node.getIndividualId());
			});

		final IndividualPanel oppositePanel = (side == Side.LEFT
			? partnerPanel.getMotherPanel()
			: partnerPanel.getFatherPanel());
		final String targetId = (oppositePanel != null && oppositePanel.getData() != null
			? oppositePanel.getData().getId()
			: null);

		return Context.forPartner(currentRecord, partnerPanel, side, childrenId, targetId);
	}

	/**
	 * Finds the nearest SiblingsPanel ancestor, if any.
	 *
	 * @param parent the component to start searching from
	 * @return the SiblingsPanel ancestor, or {@code null} if none
	 */
	private static SiblingsPanel findContainingSiblingsPanel(Component parent){
		while(parent != null && !(parent instanceof SiblingsPanel))
			parent = parent.getParent();
		return (SiblingsPanel)parent;
	}

}