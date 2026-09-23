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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.IndividualTreeGraphListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama.CoordinateAssigner;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama.CrossingReducer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama.DummyNodes;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama.Graph;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama.Layerer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;

import javax.swing.JPanel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class GraphLayoutBuilder{

	private GraphLayoutBuilder(){}


	public static SiblingsPanel buildLayout(final JPanel canvas, final Set<TreeNode> allTreeNodes,
			final boolean showPartner, final FLEFModel model, final Map<TreeNode, PartnersPanel> nodeToPanelMap,
			final IndividualTreeGraphListener treeListener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory, final TreeLayout treeLayout){
		if(allTreeNodes == null || allTreeNodes.isEmpty())
			return null;

		// Build Sugiyama DAG
		final Graph graph = new Graph();
		for(final TreeNode tn : allTreeNodes){
			final String id = tn.getIndividualId();
			graph.addNode(id, tn, false);
		}
		// Connect edges (Parent -> Child)
		for(final TreeNode child : allTreeNodes){
			final String childId = child.getIndividualId();

			if(child.getFather() != null){
				final String fatherId = child.getFather()
					.getIndividualId();
				if(fatherId != null)
					graph.addEdge(fatherId, childId);
			}
			if(child.getMother() != null){
				final String motherId = child.getMother()
					.getIndividualId();
				if(motherId != null)
					graph.addEdge(motherId, childId);
			}
		}

		// Step 1: Layer Assignment & Multi-Ahnentafel computation for consanguinity
		final Map<Graph.Node, List<Long>> ahnentafelMap = new HashMap<>();
		final List<List<Graph.Node>> layers = Layerer.assignLayers(graph, ahnentafelMap);

		// Step 2: Edge Normalization
		DummyNodes.normalizeEdges(graph, layers);

		// Step 3: Crossing Reduction (Order-Preserving Barycenter)
		CrossingReducer.reduceCrossings(layers);

		// Step 4: Populate Swing Canvas
		final SiblingsPanel siblingsPanel = CoordinateAssigner.populateCanvas(canvas, layers, model,
			nodeToPanelMap, treeListener, popupFactory, treeLayout, showPartner);

		canvas.setPreferredSize(canvas.getPreferredSize());

		return siblingsPanel;
	}

}
