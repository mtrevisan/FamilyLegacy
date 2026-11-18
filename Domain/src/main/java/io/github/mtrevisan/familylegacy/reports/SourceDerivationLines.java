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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.familylegacy.sources.SourceDerivationTree;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * What a bibliography says of the derivations of a source: the sources it is made from (with the locator, and "contradicts" if it
 * does not agree with them), the originals at the top of its chain, and the sources made from it.
 */
final class SourceDerivationLines{

	private SourceDerivationLines(){}


	/**
	 * Adds to the details of a source what it is derived from: the sources it is made from, and the originals when they are not the same.
	 * Nothing is added for an original.
	 */
	static void appendRows(final ReportContext ctx, final String sourceId, final List<String> rows){
		final SourceDerivationTree derivations = ctx.derivations();
		final List<SourceDerivationTree.Node> parents = derivations.ancestors(sourceId);
		if(parents.isEmpty())
			return;

		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().sourceDerivedFrom(), join(ctx, parents));

		//the originals are told apart only if the source is made from something that is made from them in turn
		final Set<String> direct = new LinkedHashSet<>();
		for(final SourceDerivationTree.Node parent : parents)
			direct.add(parent.id());
		final List<String> originals = derivations.originals(sourceId).stream()
			.filter(id -> !direct.contains(id))
			.toList();
		if(!originals.isEmpty())
			ReportFormatters.appendIfPresent(rows, ctx.labels.sections().sourceOriginal(),
				String.join("; ", originals.stream().map(ctx::sourceTitle).toList()));
	}

	/** The sources that are made from the source, as a section; nothing if there are none. */
	static List<ReportSection> derivatives(final ReportContext ctx, final String sourceId){
		final List<SourceDerivationTree.Node> children = ctx.derivations().descendants(sourceId);
		if(children.isEmpty())
			return List.of();

		final List<String> items = new ArrayList<>(children.size());
		for(final SourceDerivationTree.Node child : children)
			items.add(ReportFormatters.escape(describe(ctx, child)));
		return List.of(new ReportSection.Heading(2, ctx.labels.sections().sourceDerivatives()), new ReportSection.BulletList(items));
	}


	private static String join(final ReportContext ctx, final List<SourceDerivationTree.Node> nodes){
		final List<String> items = new ArrayList<>(nodes.size());
		for(final SourceDerivationTree.Node node : nodes)
			items.add(describe(ctx, node));
		return String.join("; ", items);
	}

	/** "Title — locator (Contradicts)". */
	private static String describe(final ReportContext ctx, final SourceDerivationTree.Node node){
		final StringBuilder text = new StringBuilder(ctx.sourceTitle(node.id()));
		if(node.locator() != null)
			text.append(" — ").append(node.locator());
		if(node.contradicts())
			text.append(" (").append(ctx.labels.sections().stanceContradicts()).append(')');
		return text.toString();
	}

}
