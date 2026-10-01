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
package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import javax.swing.JList;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;


/**
 * Dual‑click enhancer for {@link JList}.
 */
public final class DualActionListEnhancer extends AbstractDualActionEnhancer{

	/**
	 * Static factory method for convenient usage.
	 */
	public static void install(final JList<?> list){
		new DualActionListEnhancer(list);
	}


	private DualActionListEnhancer(final JList<?> list){
		super(list);
	}


	@Override
	protected boolean isOverValidTarget(){
		final Point p = getMousePosition();
		if(p == null)
			return false;

		final JList<?> list = (JList<?>)component;
		final int index = list.locationToIndex(p);
		if(index < 0)
			return false;

		final Rectangle cellBounds = list.getCellBounds(index, index);
		return (cellBounds != null && cellBounds.contains(p));
	}

	@Override
	protected void installComponentSpecificBehavior(){
		// Mouse motion listener to update cursor when moving over items
		final JList<?> list = (JList<?>)component;
		list.addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				// Re‑evaluate "over target" status on each move
				updateCursor();
			}
		});
	}

}
