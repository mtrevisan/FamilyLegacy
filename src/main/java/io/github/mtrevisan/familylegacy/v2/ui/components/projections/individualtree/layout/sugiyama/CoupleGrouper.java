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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Groups the non-dummy {@link TreeNode}s of a Sugiyama layer into couples
 * ({@code {fatherTn, motherTn}}), based on the actual partnership recorded
 * in the model rather than on their position in the layer.
 */
final class CoupleGrouper{

	private CoupleGrouper(){}


	/** Groups every layer in one call. */
	static List<List<TreeNode[]>> groupAll(final List<List<Graph.Node>> layers,
			final Map<String, TreeNode> treeNodeById){
		final List<List<TreeNode[]>> result = new ArrayList<>(layers.size());
		for(final List<Graph.Node> layer : layers)
			result.add(groupLayer(layer, treeNodeById));
		return result;
	}

	/**
	 * Groups the non-dummy tree nodes of a single layer into couples:
	 * <ul>
	 *   <li>two nodes form a couple when one's {@code getPartnerData()}
	 *       resolves to the other and both are present in this layer;</li>
	 *   <li>a node whose partner is not in this layer becomes a single
	 *       couple, with the other slot left empty.</li>
	 * </ul>
	 * The returned arrays are {@code {fatherTn, motherTn}}, with either
	 * slot possibly {@code null}.
	 */
	static List<TreeNode[]> groupLayer(final List<Graph.Node> layerNodes, final Map<String, TreeNode> treeNodeById){
		final List<TreeNode[]> couples = new ArrayList<>();
		if(layerNodes == null || layerNodes.isEmpty())
			return couples;

		final List<TreeNode> treeNodesInLayer = new ArrayList<>();
		for(final Graph.Node n : layerNodes)
			if(!n.isDummy() && n.getTreeNode() != null)
				treeNodesInLayer.add(n.getTreeNode());
		if(treeNodesInLayer.isEmpty())
			return couples;

		final Set<TreeNode> paired = new HashSet<>();
		for(final TreeNode tn : treeNodesInLayer){
			if(paired.contains(tn))
				continue;

			paired.add(tn);

			final TreeNode partner = findPartner(tn, treeNodesInLayer, treeNodeById, paired);
			if(partner != null){
				paired.add(partner);
				couples.add(orderCouple(tn, partner));
			}
			else
				couples.add(isFemale(tn)
					? new TreeNode[]{null, tn}
					: new TreeNode[]{tn, null});
		}
		return couples;
	}

	/**
	 * Stable key for a couple, used to deduplicate the {@code PartnersPanel}s.
	 * Empty slots are represented by an empty string, so a single parent has
	 * a distinct key from a full couple.
	 */
	static String coupleKey(final TreeNode father, final TreeNode mother){
		return (father != null? father.getIndividualId(): StringUtils.EMPTY)
			+ "+"
			+ (mother != null? mother.getIndividualId(): StringUtils.EMPTY);
	}


	/* ======================================================================
	 *                          Internal
	 * ====================================================================== */

	private static TreeNode findPartner(final TreeNode tn, final List<TreeNode> treeNodesInLayer,
			final Map<String, TreeNode> treeNodeById, final Set<TreeNode> paired){
		final IndividualData partnerData = tn.getPartnerData();
		if(partnerData == null || partnerData.getId() == null)
			return null;

		final TreeNode candidate = treeNodeById.get(partnerData.getId());
		if(candidate == null || paired.contains(candidate) || !treeNodesInLayer.contains(candidate))
			return null;

		return candidate;
	}

	/**
	 * Returns the couple as {@code {fatherTn, motherTn}}, using the sex of
	 * the two nodes to decide the order. When both have the same sex (or
	 * the sex is unknown), the natural order is preserved.
	 */
	private static TreeNode[] orderCouple(final TreeNode a, final TreeNode b){
		return (isFemale(a) && !isFemale(b)
			? new TreeNode[]{b, a}
			: new TreeNode[]{a, b});
	}

	private static boolean isFemale(final TreeNode node){
		final IndividualData data = node.getIndividualData();
		return (data != null && data.getSex() == SexType.FEMALE);
	}

}
