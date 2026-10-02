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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Builds the pedigree-tree preview that opens a multi-person report.
 *
 * <p>The direct line is drawn with box-drawing characters (U+2500 block) so
 * it renders consistently in every output format. Collateral relatives are
 * listed separately, and for each one the section shows:</p>
 * <ul>
 *   <li>its kinship term relative to the root, resolved through the
 *       {@link io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.services.kinship.KinshipCalculator};</li>
 *   <li>the minimal chain of ancestors and descendants that connects the
 *       collateral to the root — the shortest path through parent, child and
 *       spouse edges. This is the "minimal tree" the reader needs to place
 *       the collateral in the family, without re-drawing the whole graph.</li>
 * </ul>
 *
 * <p>Collaterals are not integrated into the main tree because a top-down
 * pedigree tree cannot host lateral branches without duplicating nodes or
 * turning into a general graph. Listing them separately, with their path to
 * the root, is both readable and unambiguous.</p>
 */
final class PedigreeTreeSection implements SectionBuilder{

	/** The direction the tree is drawn in. */
	enum Direction{
		ANCESTORS,
		DESCENDANTS
	}

	private static final String BRANCH = "\u251C\u2500\u2500 ";  // "├── "
	private static final String LAST_BRANCH = "\u2514\u2500\u2500 ";  // "└── "
	private static final String VERTICAL = "\u2502   ";            // "│   "
	private static final String SPACE = "    ";
	private static final String ARROW = " \u2192 ";             // " → "


	private final ReportContext ctx;
	private final List<FLEFRecord> directLine;
	private final List<FLEFRecord> collaterals;
	private final Direction direction;


	PedigreeTreeSection(final ReportContext ctx,
		final List<FLEFRecord> directLine,
		final List<FLEFRecord> collaterals,
		final Direction direction){
		this.ctx = ctx;
		this.directLine = List.copyOf(directLine);
		this.collaterals = List.copyOf(collaterals);
		this.direction = direction;
	}


	@Override
	public List<ReportSection> build(){
		final List<ReportSection> out = new ArrayList<>();
		if(directLine.isEmpty())
			return out;

		out.add(new ReportSection.Heading(1, direction == Direction.ANCESTORS
			? ctx.labels.sections().ancestorTree()
			: ctx.labels.sections().descendantTree()));
		out.add(new ReportSection.Paragraph(ctx.labels.sections().pedigreeTreeNote()));
		out.add(new ReportSection.Paragraph(renderTree()));

		if(!collaterals.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.sections().collateralRelatives()));
			for(final FLEFRecord c : collaterals)
				appendCollateral(out, c);
		}
		return out;
	}


	/* ======================================================================
	 *                          Tree rendering
	 * ====================================================================== */

	private String renderTree(){
		final StringBuilder sb = new StringBuilder();
		final FLEFRecord root = directLine.getFirst();
		sb.append(ReportFormatters.escape(ctx.displayText(root)))
			.append('\n');
		renderChildren(sb, root, "");
		return sb.toString();
	}

	private void renderChildren(final StringBuilder sb, final FLEFRecord node,
		final String prefix){
		final List<FLEFRecord> next = (direction == Direction.ANCESTORS
			? ctx.index.parentsOf(node)
			: ctx.index.childrenOf(node));

		final List<FLEFRecord> inLine = new ArrayList<>(next.size());
		for(final FLEFRecord r : next)
			if(directLine.contains(r))
				inLine.add(r);

		for(int i = 0; i < inLine.size(); i ++){
			final boolean last = (i == inLine.size() - 1);
			sb.append(prefix)
				.append(last? LAST_BRANCH: BRANCH)
				.append(ReportFormatters.escape(ctx.displayText(inLine.get(i))))
				.append('\n');
			renderChildren(sb, inLine.get(i), prefix + (last? SPACE: VERTICAL));
		}
	}


	/* ======================================================================
	 *                          Collateral rendering
	 * ====================================================================== */

	/**
	 * Appends one collateral relative with its kinship term and its minimal
	 * chain to the root. The kinship term is computed by the
	 * {@code KinshipCalculator}, so it follows the same vocabulary used
	 * everywhere else in the report ("uncle", "first cousin once removed", …).
	 */
	private void appendCollateral(final List<ReportSection> out, final FLEFRecord c){
		final FLEFRecord root = directLine.getFirst();
		final String kinship = ctx.kinship().shortTerm(c.getId(), root.getId());
		final String name = ReportFormatters.escape(ctx.displayText(c));

		final StringBuilder header = new StringBuilder("- **")
			.append(name).append("**");
		if(!kinship.isBlank() && !"self".equals(kinship) && !"unrelated".equals(kinship))
			header.append(" — *").append(ReportFormatters.escape(kinship))
				.append("* of ").append(ReportFormatters.escape(ctx.displayText(root)));
		out.add(new ReportSection.Paragraph(header.toString()));

		final List<FLEFRecord> path = findPath(c, root);
		if(path.size() > 1)
			out.add(new ReportSection.Paragraph("  `" + renderPath(path) + "`"));
	}

	private String renderPath(final List<FLEFRecord> path){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size(); i ++){
			if(i > 0)
				sb.append(ARROW);
			sb.append(ReportFormatters.escape(ctx.displayText(path.get(i))));
		}
		return sb.toString();
	}


	/* ======================================================================
	 *                          Path finding
	 * ====================================================================== */

	/**
	 * Returns the shortest path from {@code from} to {@code to}, walking
	 * parent, child and spouse edges. The result includes both endpoints.
	 * Returns an empty list when no path exists (disconnected family).
	 *
	 * <p>The walk is a breadth-first search, so the returned path minimizes
	 * the number of intermediate relatives. For an uncle, this yields the
	 * four-node chain {@code root → father → grandfather → uncle}; for a
	 * spouse, it yields {@code root → … → descendant → spouse}; for a
	 * step-child, it yields {@code root → … → descendant → spouse → step-child}.</p>
	 */
	private List<FLEFRecord> findPath(final FLEFRecord from, final FLEFRecord to){
		if(from == null || to == null)
			return List.of();
		if(from.getId().equals(to.getId()))
			return List.of(from);

		final Map<String, FLEFRecord> predecessors = new HashMap<>();
		final Set<String> seen = new HashSet<>();
		final Deque<FLEFRecord> queue = new ArrayDeque<>();

		queue.add(from);
		seen.add(from.getId());

		while(!queue.isEmpty()){
			final FLEFRecord cur = queue.poll();
			for(final FLEFRecord neighbor : neighborsOf(cur)){
				if(!seen.add(neighbor.getId()))
					continue;
				predecessors.put(neighbor.getId(), cur);
				if(neighbor.getId().equals(to.getId())){
					final List<FLEFRecord> path = new ArrayList<>();
					FLEFRecord node = neighbor;
					while(node != null){
						path.add(node);
						node = predecessors.get(node.getId());
					}
					Collections.reverse(path);
					return path;
				}
				queue.add(neighbor);
			}
		}
		return List.of();
	}

	private List<FLEFRecord> neighborsOf(final FLEFRecord rec){
		final List<FLEFRecord> out = new ArrayList<>();
		out.addAll(ctx.index.parentsOf(rec));
		out.addAll(ctx.index.childrenOf(rec));
		out.addAll(ctx.index.spousesOf(rec));
		return out;
	}

}
