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

import java.util.ArrayList;
import java.util.List;


public class DummyNodes{

	private DummyNodes(){}


	public static void normalizeEdges(final Graph graph, final List<List<Graph.Node>> layers){
		final List<Graph.Node> allNodes = new ArrayList<>(graph.getNodes().values());
		int dummyCounter = 0;

		for(final Graph.Node src : allNodes){
			final List<Graph.Node> targets = new ArrayList<>(src.getOutgoing());

			for(final Graph.Node tgt : targets){
				if(tgt.getLayer() > src.getLayer() + 1){
					graph.removeEdge(src, tgt);

					Graph.Node previous = src;
					for(int l = src.getLayer() + 1; l < tgt.getLayer(); l ++){
						final String dummyId = "__dummy_" + (dummyCounter ++);
						final Graph.Node dummy = graph.addNode(dummyId, null, true);
						dummy.setLayer(l);
						dummy.setWidth(10);
						dummy.setHeight(10);

						layers.get(l).add(dummy);

						graph.addEdge(previous.getId(), dummy.getId());
						previous = dummy;
					}

					graph.addEdge(previous.getId(), tgt.getId());
				}
			}
		}
	}

}
