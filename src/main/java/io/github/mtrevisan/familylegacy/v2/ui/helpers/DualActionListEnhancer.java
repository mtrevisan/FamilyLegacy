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
