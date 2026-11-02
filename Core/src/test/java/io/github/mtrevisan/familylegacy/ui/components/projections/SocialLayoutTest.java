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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialEdgeDirection;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialEdgeRef;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialFilters;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialGraph;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialLayout;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialLayoutMode;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialNodeRef;
import io.github.mtrevisan.familylegacy.ui.components.projections.social.SocialRelationCategory;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalEntityType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/** The boxes of the social network never touch, in either layout, and the radial tree says that it is one. */
class SocialLayoutTest{

	private static final int MAX_DEGREE = 3;


	private static SocialNodeRef node(final String id, final int degree){
		final FLEFRecord record = FLEFRecord.createMainRecord(id, "individual");
		return new SocialNodeRef(new TemporalEntityRef(TemporalEntityType.INDIVIDUAL, id, record, id), degree,
			SocialRelationCategory.OTHER);
	}

	private static SocialEdgeRef edge(final SocialNodeRef from, final SocialNodeRef to){
		final String id = "R-" + from.id() + "-" + to.id();
		return new SocialEdgeRef(from.entity(), to.entity(), "associate", null, SocialRelationCategory.OTHER,
			SocialEdgeDirection.UNDIRECTED, TemporalSpan.point(NormalizedDate.exact(2_400_000L, DatePrecision.YEAR)),
			FLEFRecord.createMainRecord(id, "relationship"));
	}

	/** A focus with {@code first} contacts, each with {@code second} contacts, each with {@code third} contacts. */
	private static SocialGraph graph(final int first, final int second, final int third){
		final SocialNodeRef center = node("C", 0);
		final List<SocialNodeRef> nodes = new ArrayList<>();
		final List<SocialEdgeRef> edges = new ArrayList<>();
		nodes.add(center);
		for(int i = 0; i < first; i ++){
			final SocialNodeRef a = node("A" + i, 1);
			nodes.add(a);
			edges.add(edge(center, a));
			for(int j = 0; j < second; j ++){
				final SocialNodeRef b = node("B" + i + "_" + j, 2);
				nodes.add(b);
				edges.add(edge(a, b));
				for(int k = 0; k < third; k ++){
					final SocialNodeRef c = node("D" + i + "_" + j + "_" + k, 3);
					nodes.add(c);
					edges.add(edge(b, c));
				}
			}
		}
		return SocialGraph.of(center, nodes, edges, MAX_DEGREE, SocialFilters.all(MAX_DEGREE));
	}

	private static void assertNoTouching(final SocialLayout layout, final String description){
		final List<Map.Entry<SocialNodeRef, Rectangle>> entries = new ArrayList<>(layout.nodeBounds().entrySet());
		for(int i = 0; i < entries.size(); i ++)
			for(int j = i + 1; j < entries.size(); j ++){
				//a margin: touching is as bad as overlapping
				final Rectangle first = new Rectangle(entries.get(i).getValue());
				first.grow(1, 1);
				Assertions.assertFalse(first.intersects(entries.get(j).getValue()),
					description + ": " + entries.get(i).getKey().id() + " touches " + entries.get(j).getKey().id());
			}
	}


	@Test
	void theBoxesOfTheConcentricLayoutDoNotTouch(){
		for(final int[] shape : new int[][]{{3, 0, 0}, {8, 3, 0}, {12, 4, 2}, {1, 1, 1}, {30, 0, 0}, {5, 12, 0}})
			assertNoTouching(SocialLayout.compute(graph(shape[0], shape[1], shape[2]), SocialLayoutMode.CONCENTRIC),
				"concentric " + shape[0] + "/" + shape[1] + "/" + shape[2]);
	}

	@Test
	void theBoxesOfTheRadialTreeDoNotTouch(){
		for(final int[] shape : new int[][]{{3, 0, 0}, {8, 3, 0}, {12, 4, 2}, {1, 1, 1}, {30, 0, 0}, {5, 12, 0}, {2, 6, 6}})
			assertNoTouching(SocialLayout.compute(graph(shape[0], shape[1], shape[2]), SocialLayoutMode.RADIAL_TREE),
				"radial " + shape[0] + "/" + shape[1] + "/" + shape[2]);
	}

	@Test
	void theLayoutKnowsWhichModeItIs(){
		Assertions.assertEquals(SocialLayoutMode.CONCENTRIC, SocialLayout.compute(graph(3, 2, 0), SocialLayoutMode.CONCENTRIC).mode());
		Assertions.assertEquals(SocialLayoutMode.RADIAL_TREE, SocialLayout.compute(graph(3, 2, 0), SocialLayoutMode.RADIAL_TREE).mode());
	}

	@Test
	void everyNodeHasABox(){
		final SocialGraph graph = graph(6, 3, 0);

		Assertions.assertEquals(graph.nodes().size(), SocialLayout.compute(graph, SocialLayoutMode.CONCENTRIC).nodeBounds().size());
		Assertions.assertEquals(graph.nodes().size(), SocialLayout.compute(graph, SocialLayoutMode.RADIAL_TREE).nodeBounds().size());
	}

}
