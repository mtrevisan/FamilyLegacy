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

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;


/**
 * Reusable drag-to-pan support for {@link JScrollPane}-based views.
 * <p>
 * The utility installs a single {@link MouseAdapter} on the scroll
 * pane's view subtree, so that dragging any non-interactive surface
 * moves the viewport. It is designed to work identically on the tree,
 * the Sugiyama graph, and the ego network, and to be re-applied after
 * every rebuild without duplicating listeners.
 * <p>
 * <b>Screen coordinates.</b> The drag anchor and the current point are
 * both taken in screen coordinates, not in the local coordinates of the
 * dragged component. Local coordinates move with the viewport: once the
 * viewport has been scrolled, the same physical point on the screen
 * maps to a different local coordinate, and the delta computed on the
 * next drag event would include the scroll applied on the previous one,
 * producing the classic jitter where the viewport moves forward and
 * then snaps back. Screen coordinates are stable, so the delta is the
 * pure mouse movement.
 * <p>
 * <b>Exclusions.</b> The caller can supply a predicate to skip specific
 * components and their entire subtree. This is how a nested
 * {@link JScrollPane} (for example the sibling panel in the tree) keeps
 * its own scrolling behaviour: the predicate returns {@code true} for
 * that scroll pane, and the recursion never installs the pan listener
 * on it or on its children.
 * <p>
 * <b>Idempotence.</b> The adapter is stored on the scroll pane via a
 * client property, and every component that has been installed is
 * marked with the same adapter reference. Calling {@link #install} again
 * on the same subtree is a no-op for components that already carry the
 * adapter, so it is safe to re-apply the installation after every
 * rebuild.
 */
public final class ViewportPanSupport{

	/** Minimum mouse movement before a drag is treated as a pan. */
	private static final int DRAG_DEAD_ZONE_PX = 3;

	/** Client property key holding the pan adapter on the scroll pane. */
	private static final String ADAPTER_KEY = "ViewportPanSupport.adapter";
	/** Client property key marking a component that has already been installed. */
	private static final String INSTALLED_KEY = "ViewportPanSupport.installed";


	private ViewportPanSupport(){}


	/**
	 * Installs drag-to-pan on the given scroll pane and on every
	 * descendant of {@code root} that is not excluded.
	 *
	 * @param scrollPane the scroll pane whose viewport is moved by the
	 *                   drag; must not be {@code null}
	 * @param root       the top of the subtree to instrument; typically
	 *                   the view of the scroll pane, or a wrapper around
	 *                   it; must not be {@code null}
	 */
	public static void install(final JScrollPane scrollPane, final Component root){
		install(scrollPane, root, c -> false);
	}

	/**
	 * Installs drag-to-pan on the given scroll pane and on every
	 * descendant of {@code root} that is not excluded.
	 * <p>
	 * The {@code exclude} predicate is evaluated for every component
	 * during the recursion. When it returns {@code true}, the component
	 * and its entire subtree are skipped: no listener is installed on
	 * them. This is the mechanism that preserves the scrolling of a
	 * nested scroll pane, or any other interactive surface that must not
	 * trigger the pan.
	 *
	 * @param scrollPane the scroll pane whose viewport is moved by the
	 *                   drag; must not be {@code null}
	 * @param root       the top of the subtree to instrument; typically
	 *                   the view of the scroll pane, or a wrapper around
	 *                   it; must not be {@code null}
	 * @param exclude    predicate returning {@code true} for components
	 *                   that must not trigger the pan; must not be
	 *                   {@code null}
	 */
	public static void install(final JScrollPane scrollPane, final Component root,
		final Predicate<Component> exclude){
		PanAdapter adapter = (PanAdapter)scrollPane.getClientProperty(ADAPTER_KEY);
		if(adapter == null){
			adapter = new PanAdapter(scrollPane);
			scrollPane.putClientProperty(ADAPTER_KEY, adapter);
		}
		installRecursive(adapter, root, exclude);
	}


	/* ======================================================================
	 *                          Recursive installation
	 * ====================================================================== */

	private static void installRecursive(final PanAdapter adapter, final Component component,
		final Predicate<Component> exclude){
		if(exclude.test(component))
			return;

		if(component instanceof JComponent jc){
			if(jc.getClientProperty(INSTALLED_KEY) == adapter)
				return;
			jc.putClientProperty(INSTALLED_KEY, adapter);
			jc.addMouseListener(adapter);
			jc.addMouseMotionListener(adapter);
		}

		if(component instanceof Container container)
			for(final Component child : container.getComponents())
				installRecursive(adapter, child, exclude);
	}


	/* ======================================================================
	 *                          Adapter
	 * ====================================================================== */

	/**
	 * Single mouse adapter shared by every component in one scroll pane.
	 * <p>
	 * The anchor is stored per adapter, not per component, which is the
	 * correct behaviour: Swing uses a mouse grab for the duration of a
	 * press-drag-release sequence, so all drag events go to the component
	 * where the press happened. A single adapter instance receiving
	 * events from different components across different sequences is
	 * enough to track the state.
	 */
	private static final class PanAdapter extends MouseAdapter{

		private final JScrollPane scrollPane;

		/** Anchor of the current drag, in screen coordinates, or {@code null}. */
		private Point anchor;


		PanAdapter(final JScrollPane scrollPane){
			this.scrollPane = scrollPane;
		}

		@Override
		public void mousePressed(final MouseEvent e){
			if(SwingUtilities.isLeftMouseButton(e))
				anchor = e.getLocationOnScreen();
		}

		@Override
		public void mouseDragged(final MouseEvent e){
			if(anchor == null || !SwingUtilities.isLeftMouseButton(e))
				return;

			final Point now = e.getLocationOnScreen();
			final int dx = now.x - anchor.x;
			final int dy = now.y - anchor.y;
			if(Math.abs(dx) < DRAG_DEAD_ZONE_PX && Math.abs(dy) < DRAG_DEAD_ZONE_PX)
				return;

			final Point vp = scrollPane.getViewport()
				.getViewPosition();
			scrollPane.getViewport()
				.setViewPosition(new Point(
					Math.max(0, vp.x - dx),
					Math.max(0, vp.y - dy)));
			anchor = now;
		}

		@Override
		public void mouseReleased(final MouseEvent e){
			anchor = null;
		}

	}

}
