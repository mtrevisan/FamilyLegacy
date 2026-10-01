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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

/**
 * Layout algorithm used to position the nodes of the Social Network view.
 * <p>
 * All layouts are <b>deterministic</b>: for a given graph and a given
 * {@code maxDegree}, the produced positions are always identical. This
 * guarantees that panning, filtering or re-selecting the focus individual
 * does not shuffle the graph visually, and that screenshots and reports are
 * reproducible.
 * <p>
 * New layouts can be added without touching the renderers, because the
 * layout stage produces only an abstract position map
 * ({@code SocialNodeRef → Point2D}) consumed by the rendering stage.
 */
public enum SocialLayoutMode{

	/**
	 * Concentric rings layout. The focus individual is placed at the
	 * center, its direct contacts on the first ring, their contacts on the
	 * second ring, and so on up to the configured maximum degree.
	 * <p>
	 * The angular position of each node is derived from the position of
	 * its parent in the breadth-first traversal, so that nodes sharing a
	 * parent remain adjacent. This layout makes the degree of separation
	 * from the focus immediately readable and is the default for the
	 * Social Network view.
	 */
	CONCENTRIC("Concentric"),

	/**
	 * Radial tree layout. The graph is drawn as a tree rooted at the focus
	 * individual, with sibling subtrees distributed around the root.
	 * Cross-edges (cycles, multiple parents) are drawn as additional arcs
	 * on top of the tree structure.
	 * <p>
	 * Compared to {@link #CONCENTRIC}, this layout preserves the local
	 * tree structure of the graph rather than the degree of separation, so
	 * it is more readable when the user wants to follow chains of
	 * relationships (e.g. "the contacts of my contacts") rather than
	 * count how far each node is from the focus.
	 */
	RADIAL_TREE("Radial tree");


	private final String displayLabel;


	SocialLayoutMode(final String displayLabel){
		this.displayLabel = displayLabel;
	}


	/**
	 * Returns a human-readable label suitable for display in combo boxes
	 * and menus.
	 *
	 * @return the display label, never {@code null}
	 */
	public String getDisplayLabel(){
		return displayLabel;
	}

	/**
	 * Returns whether this layout places nodes on concentric rings around
	 * the focus individual.
	 *
	 * @return {@code true} for {@link #CONCENTRIC}
	 */
	public boolean isConcentric(){
		return (this == CONCENTRIC);
	}

	/**
	 * Returns whether this layout places nodes as a radial tree rooted at
	 * the focus individual.
	 *
	 * @return {@code true} for {@link #RADIAL_TREE}
	 */
	public boolean isRadialTree(){
		return (this == RADIAL_TREE);
	}

	@Override
	public String toString(){
		return displayLabel;
	}

}
