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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;

import javax.swing.JPanel;
import java.awt.geom.Path2D;
import java.util.Map;


public class TreeLayoutEngine implements LayoutEngine{

	@Override
	public SiblingsPanel buildLayout(final JPanel canvas, final TreeNode rootNode, final boolean showPartner,
			final int maxAncestors, final FLEFModel model, final Map<TreeNode, PartnersPanel> nodeToPanelMap,
			final IndividualTreeGraphListener treeListener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory, final TreeLayout treeLayout){
		return TreeLayoutBuilder.buildLayout(canvas, rootNode, showPartner,
			maxAncestors, model, nodeToPanelMap, treeListener, popupFactory, treeLayout);
	}


	/**
	 * Builds a cached {@link Path2D} containing all connection lines for the tree.
	 */
	@Override
	public Path2D buildTreePath(final TreeLayout treeLayout, final TreeNode rootNode,
			final Map<TreeNode, PartnersPanel> nodeToPanelMap, final SiblingsPanel childrenPanel, final JPanel canvas){
		return LayoutRenderer.buildTreePath(treeLayout, rootNode, nodeToPanelMap, childrenPanel, canvas);
	}

}
