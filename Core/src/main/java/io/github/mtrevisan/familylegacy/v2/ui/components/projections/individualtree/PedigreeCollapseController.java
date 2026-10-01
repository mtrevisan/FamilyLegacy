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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreeCollapse;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreeCollapseDetector;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreeCollapseDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreePath;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeNode;

import java.awt.Window;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Detects pedigree collapses in the current tree and decorates the
 * individual panels with the collapse badge.
 * <p>
 * A collapse is the appearance of the same individual in more than one
 * place in the tree, which typically indicates endogamy or a shared
 * ancestor. The controller detects the collapses once per tree rebuild,
 * applies the badge to every panel that shows an affected individual,
 * and exposes the full list so the user can open a report dialog.
 */
public final class PedigreeCollapseController{

	private final FLEFModel model;

	/** Collapses detected in the current tree, keyed by individual id. */
	private Map<String, PedigreeCollapse> collapses = Map.of();


	public PedigreeCollapseController(final FLEFModel model){
		this.model = model;
	}


	/** Re-detects the collapses for the given root. */
	public void detect(final TreeNode rootNode){
		final List<PedigreeCollapse> list = PedigreeCollapseDetector.detect(rootNode, model);
		collapses = list.stream()
			.collect(Collectors.toMap(PedigreeCollapse::individualId, Function.identity()));
	}

	/** Applies the badge to every panel that shows an affected individual. */
	public void apply(final Map<TreeNode, PartnersPanel> nodeToPanelMap){
		for(final Map.Entry<TreeNode, PartnersPanel> entry : nodeToPanelMap.entrySet()){
			final TreeNode node = entry.getKey();
			final PartnersPanel panel = entry.getValue();
			applyToSlot(panel.getFatherPanel(), node.getFather());
			applyToSlot(panel.getMotherPanel(), node.getMother());
		}
	}

	private void applyToSlot(final IndividualPanel slot, final TreeNode relative){
		if(slot == null || relative == null)
			return;

		final String id = relative.getIndividualId();
		if(id == null)
			return;

		final PedigreeCollapse collapse = collapses.get(id);
		if(collapse == null)
			return;

		slot.withCollapseInfo(collapse.occurrenceCount(), buildTooltip(collapse));
	}

	/** Opens the report dialog listing every collapse. */
	public void showDialog(final Window parent){
		final List<PedigreeCollapse> list = List.copyOf(collapses.values());
		final PedigreeCollapseDialog dialog = new PedigreeCollapseDialog(parent, list);
		dialog.setVisible(true);
	}


	private static String buildTooltip(final PedigreeCollapse collapse){
		final StringBuilder sb = new StringBuilder("<html><b>Pedigree collapse</b><br>");
		sb.append("This individual appears ")
			.append(collapse.occurrenceCount())
			.append(" times in the tree.<br><br>");
		sb.append("<b>Paths from the root:</b><br>");
		for(final PedigreePath path : collapse.paths())
			sb.append("&nbsp;&nbsp;").append(path.code())
				.append(" — ").append(path.describe())
				.append("<br>");
		sb.append("</html>");
		return sb.toString();
	}

}
