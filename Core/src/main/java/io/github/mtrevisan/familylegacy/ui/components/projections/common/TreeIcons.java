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
package io.github.mtrevisan.familylegacy.ui.components.projections.common;

import javax.swing.Icon;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;


/**
 * Shared cache of the small navigation arrows used by the tree panels
 * ({@link io.github.mtrevisan.familylegacy.ui.components.projections.individual.PartnersPanel}
 * and {@link io.github.mtrevisan.familylegacy.ui.components.projections.individual.SiblingsPanel}).
 * <p>
 * Two arrows are needed, one for each direction of the tree:
 * <ul>
 *   <li><b>ancestors</b> — points up in the vertical layout (parents and
 *       grandparents are above the box) and right in the horizontal one
 *       (they are to the right of the box);</li>
 *   <li><b>descendants</b> — points down in the vertical layout (children
 *       are below the box) and left in the horizontal one (they are to the
 *       left of the box).</li>
 * </ul>
 * Both arrows share the same dimensions, so the class exposes a single
 * {@link #ARROW_SIZE} that both panels use to reserve the right amount of
 * space in their layouts.
 * <p>
 * The four icons are created at class-load time and shared by every
 * consumer. They are drawn as vector shapes ({@link TreeArrowIcon}) in the colour of the
 * component they are on (a lighter one when it is disabled).
 */
public final class TreeIcons{

	/** Height of the arrow, in pixels. */
	public static final int ARROW_HEIGHT = 12;
	/** Width-to-height ratio of the arrow image. */
	private static final double ARROW_ASPECT_RATIO = 3501. / 2662.;

	/** Preferred size of an arrow icon. Both panels reserve this space. */
	public static final Dimension ARROW_SIZE = new Dimension(
		(int)((float)ARROW_HEIGHT / ARROW_ASPECT_RATIO), ARROW_HEIGHT);

	/**
	 * The area of the arrows that point sideways (the horizontal layout): as wide as the others, lower, as the images they
	 * replace were (the label that holds them is as tall as {@link #ARROW_SIZE}, and centres them).
	 */
	private static final Dimension SIDEWAYS_ARROW_SIZE = new Dimension(ARROW_SIZE.width, 8);

	private static final Map<TreeLayout, Icon> ANCESTORS = new EnumMap<>(TreeLayout.class);
	private static final Map<TreeLayout, Icon> DESCENDANTS = new EnumMap<>(TreeLayout.class);


	static{
		// Ancestor arrow: points up in the vertical layout, right in the
		// horizontal one (ancestors are above, or to the right of, the box).
		ANCESTORS.put(TreeLayout.VERTICAL, new TreeArrowIcon(TreeArrowIcon.Direction.UP, ARROW_SIZE));
		ANCESTORS.put(TreeLayout.HORIZONTAL, new TreeArrowIcon(TreeArrowIcon.Direction.RIGHT, SIDEWAYS_ARROW_SIZE));

		// Descendant arrow: points down in the vertical layout, left in the
		// horizontal one (children are below, or to the left of, the box).
		DESCENDANTS.put(TreeLayout.VERTICAL, new TreeArrowIcon(TreeArrowIcon.Direction.DOWN, ARROW_SIZE));
		DESCENDANTS.put(TreeLayout.HORIZONTAL, new TreeArrowIcon(TreeArrowIcon.Direction.LEFT, SIDEWAYS_ARROW_SIZE));
	}


	private TreeIcons(){}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Returns the arrow that indicates the presence of ancestors beyond
	 * the visible depth, pre-oriented for the given layout.
	 *
	 * @param treeLayout the tree orientation; must not be {@code null}
	 * @return the shared arrow icon, never {@code null}
	 */
	public static Icon ancestors(final TreeLayout treeLayout){
		return ANCESTORS.get(treeLayout);
	}

	/**
	 * Returns the arrow that indicates the presence of descendants beyond
	 * the visible depth, pre-oriented for the given layout.
	 *
	 * @param treeLayout the tree orientation; must not be {@code null}
	 * @return the shared arrow icon, never {@code null}
	 */
	public static Icon descendants(final TreeLayout treeLayout){
		return DESCENDANTS.get(treeLayout);
	}

}
