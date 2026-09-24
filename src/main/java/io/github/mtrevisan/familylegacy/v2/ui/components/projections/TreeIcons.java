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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


/**
 * Shared cache of the small navigation arrows used by the tree panels
 * ({@link io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel}
 * and {@link io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel}).
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
 * The four icons are pre-loaded at class-load time and shared by every
 * consumer; the disabled variants (greyed-out versions) are also cached
 * on first request through {@link #disabled(ImageIcon)}.
 */
public final class TreeIcons{

	/** Height of the arrow, in pixels. */
	public static final int ARROW_HEIGHT = 12;
	/** Width-to-height ratio of the arrow image. */
	private static final double ARROW_ASPECT_RATIO = 3501. / 2662.;

	/** Preferred size of an arrow icon. Both panels reserve this space. */
	public static final Dimension ARROW_SIZE = new Dimension(
		(int)((float)ARROW_HEIGHT / ARROW_ASPECT_RATIO), ARROW_HEIGHT);

	private static final Map<TreeLayout, ImageIcon> ANCESTORS = new EnumMap<>(TreeLayout.class);
	private static final Map<TreeLayout, ImageIcon> DESCENDANTS = new EnumMap<>(TreeLayout.class);
	private static final Map<ImageIcon, ImageIcon> DISABLED = new HashMap<>();


	static{
		// Ancestor arrow: points up in the vertical layout, right in the
		// horizontal one (ancestors are above, or to the right of, the box).
		ANCESTORS.put(TreeLayout.VERTICAL,
			ResourceHelper.getResizedImageFromResource("/images/union_up.png", ARROW_SIZE));
		ANCESTORS.put(TreeLayout.HORIZONTAL,
			ResourceHelper.getResizedImageFromResource("/images/union_next.png", ARROW_SIZE));

		// Descendant arrow: points down in the vertical layout, left in the
		// horizontal one (children are below, or to the left of, the box).
		DESCENDANTS.put(TreeLayout.VERTICAL,
			ResourceHelper.getResizedImageFromResource("/images/union_down.png", ARROW_SIZE));
		DESCENDANTS.put(TreeLayout.HORIZONTAL,
			ResourceHelper.getResizedImageFromResource("/images/union_previous.png", ARROW_SIZE));
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
	public static ImageIcon ancestors(final TreeLayout treeLayout){
		return ANCESTORS.get(treeLayout);
	}

	/**
	 * Returns the arrow that indicates the presence of descendants beyond
	 * the visible depth, pre-oriented for the given layout.
	 *
	 * @param treeLayout the tree orientation; must not be {@code null}
	 * @return the shared arrow icon, never {@code null}
	 */
	public static ImageIcon descendants(final TreeLayout treeLayout){
		return DESCENDANTS.get(treeLayout);
	}

	/**
	 * Returns a greyed-out copy of the given icon, caching the result so
	 * repeated requests for the same source return the same instance.
	 *
	 * @param icon the source icon; may be {@code null}
	 * @return the disabled variant, or {@code null} when the input is
	 *         {@code null}
	 */
	public static ImageIcon disabled(final ImageIcon icon){
		if(icon == null)
			return null;

		return DISABLED.computeIfAbsent(icon,
			source -> new ImageIcon(GrayFilter.createDisabledImage(source.getImage())));
	}

}
