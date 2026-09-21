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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Utility methods for walking the component subtree of a view and
 * picking up the {@link IndividualPanel} instances it contains.
 * <p>
 * The tree is not a flat container: couple panels and the sibling panel
 * each hold their own boxes, and boxes may be nested. Every consumer
 * that needs to iterate over the visible individuals (bounds
 * computation for spatial navigation, id collection for the lifespan
 * strip) shares the same traversal, which is defined here once.
 */
public final class IndividualPanelFinder{

	private IndividualPanelFinder(){}


	/**
	 * Visits every {@link IndividualPanel} in the subtree rooted at the
	 * given component, in depth-first order.
	 */
	public static void visit(final Component root, final Consumer<IndividualPanel> visitor){
		if(root instanceof IndividualPanel panel)
			visitor.accept(panel);
		if(root instanceof Container container)
			for(final Component child : container.getComponents())
				visit(child, visitor);
	}

	/**
	 * Returns the ids of every {@link IndividualPanel} with non-empty
	 * data found under the given component, in insertion order.
	 */
	public static Set<String> collectIds(final Component root){
		final Set<String> ids = new LinkedHashSet<>();
		visit(root, panel -> {
			final IndividualData data = panel.getData();
			if(data != null && !data.isEmpty() && data.getId() != null)
				ids.add(data.getId());
		});
		return ids;
	}

	/**
	 * Returns the on-screen bounds of every {@link IndividualPanel} with
	 * non-empty data found under the given component.
	 * <p>
	 * Screen coordinates are used because the layout coordinates are not
	 * uniform across the tree: each panel has its own local origin, and
	 * converting them to a common system by walking parent by parent
	 * would duplicate what Swing already does.
	 * <p>
	 * When the same individual appears more than once (pedigree
	 * collapse), the first occurrence wins: the spatial navigation needs
	 * one position per individual, and any of them is a valid target.
	 */
	public static Map<String, Rectangle> collectScreenBounds(final Component root){
		final Map<String, Rectangle> result = new LinkedHashMap<>();
		visit(root, panel -> {
			final IndividualData data = panel.getData();
			if(data == null || data.isEmpty() || data.getId() == null)
				return;

			try{
				final Point p = panel.getLocationOnScreen();
				result.putIfAbsent(data.getId(), new Rectangle(p.x, p.y,
					panel.getWidth(), panel.getHeight()));
			}
			catch(final IllegalComponentStateException ignored){
				// The panel is not showing; skip it.
			}
		});
		return result;
	}

}
