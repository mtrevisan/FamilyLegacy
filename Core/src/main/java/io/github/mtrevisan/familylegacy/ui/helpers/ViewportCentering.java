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
package io.github.mtrevisan.familylegacy.ui.helpers;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Supplier;


/**
 * Scrolls a scroll pane so that a component of its view is in the middle of the viewport, horizontally, vertically or both. It is what
 * a view needs to <em>start</em> centered: when the content is smaller than the viewport the layout already centers it (and there is
 * nothing to scroll); when it is larger, the viewport would otherwise start at the top left corner.
 */
public final class ViewportCentering{

	/** How long, after it is asked for, the centering follows the layout of the view. */
	private static final int SETTLE_MILLIS = 2_000;
	private static final String SETTLING_KEY = ViewportCentering.class.getName() + ".settling";


	private ViewportCentering(){}


	/**
	 * Scrolls now, if the viewport has a size.
	 *
	 * @param scroll        the scroll pane
	 * @param target        a component inside the view of the scroll pane (the view itself is fine)
	 * @param horizontally  whether to center it horizontally (the horizontal position is left as it is otherwise)
	 * @param vertically    whether to center it vertically
	 * @return	Whether the viewport had a size (so the position was set).
	 */
	public static boolean centerOn(final JScrollPane scroll, final Component target, final boolean horizontally,
			final boolean vertically){
		final JViewport viewport = scroll.getViewport();
		final Component view = viewport.getView();
		final Dimension extent = viewport.getExtentSize();
		if(view == null || target == null || extent.width <= 0 || extent.height <= 0)
			return false;

		final Rectangle bounds = (target == view
			? new Rectangle(0, 0, view.getWidth(), view.getHeight())
			: SwingUtilities.convertRectangle(target.getParent(), target.getBounds(), view));
		final Dimension size = view.getSize();
		final Point position = viewport.getViewPosition();

		final int x = (horizontally
			? clamp(bounds.x + bounds.width / 2 - extent.width / 2, size.width - extent.width)
			: position.x);
		final int y = (vertically
			? clamp(bounds.y + bounds.height / 2 - extent.height / 2, size.height - extent.height)
			: position.y);
		viewport.setViewPosition(new Point(x, y));
		return true;
	}

	/**
	 * Centers when the layout is done, and keeps the component in the middle while the layout settles: a view is laid out in several
	 * steps (the viewport has no size until it is shown, the content is built and measured, the scroll bars come and go), and each step
	 * moves the middle. It stops at the first action of the user on the scroll pane (a wheel, a click or a drag), or after
	 * {@value #SETTLE_MILLIS} ms, so that the user is never fought.
	 *
	 * @param scroll        the scroll pane
	 * @param target        gives the component to center, when it is time (it may have been rebuilt meanwhile)
	 * @param horizontally  whether to center it horizontally
	 * @param vertically    whether to center it vertically
	 */
	public static void centerWhenReady(final JScrollPane scroll, final Supplier<Component> target, final boolean horizontally,
			final boolean vertically){
		SwingUtilities.invokeLater(() -> {
			if(scroll.getClientProperty(SETTLING_KEY) instanceof Settling previous)
				previous.stop();

			final Settling settling = new Settling(scroll, target, horizontally, vertically);
			scroll.putClientProperty(SETTLING_KEY, settling);
			settling.start();
		});
	}


	/** The centering of a view that is still being laid out. */
	private static final class Settling extends ComponentAdapter{

		private final JScrollPane scroll;
		private final Supplier<Component> target;
		private final boolean horizontally;
		private final boolean vertically;
		private final MouseAdapter userAction = new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				stop();
			}

			@Override
			public void mouseWheelMoved(final MouseWheelEvent e){
				stop();
			}
		};
		private final Timer timer = new Timer(SETTLE_MILLIS, e -> stop());
		private Component view;
		private boolean active;


		Settling(final JScrollPane scroll, final Supplier<Component> target, final boolean horizontally, final boolean vertically){
			this.scroll = scroll;
			this.target = target;
			this.horizontally = horizontally;
			this.vertically = vertically;
			timer.setRepeats(false);
		}


		void start(){
			active = true;
			view = scroll.getViewport().getView();
			scroll.getViewport().addComponentListener(this);
			if(view != null)
				view.addComponentListener(this);
			scroll.addMouseWheelListener(userAction);
			scroll.getVerticalScrollBar().addMouseListener(userAction);
			scroll.getHorizontalScrollBar().addMouseListener(userAction);
			if(view != null)
				view.addMouseListener(userAction);
			timer.start();
			center();
		}

		void stop(){
			if(!active)
				return;

			active = false;
			timer.stop();
			scroll.getViewport().removeComponentListener(this);
			if(view != null){
				view.removeComponentListener(this);
				view.removeMouseListener(userAction);
			}
			scroll.removeMouseWheelListener(userAction);
			scroll.getVerticalScrollBar().removeMouseListener(userAction);
			scroll.getHorizontalScrollBar().removeMouseListener(userAction);
			if(scroll.getClientProperty(SETTLING_KEY) == this)
				scroll.putClientProperty(SETTLING_KEY, null);
		}

		private void center(){
			centerOn(scroll, target.get(), horizontally, vertically);
		}

		@Override
		public void componentResized(final ComponentEvent e){
			if(active)
				//the view is laid out after the viewport (or the content) is resized
				SwingUtilities.invokeLater(() -> {
					if(active)
						center();
				});
		}

	}


	/** @return	The position kept between 0 and the farthest the viewport can go ({@code max} may be negative: the content fits). */
	private static int clamp(final int position, final int max){
		return Math.max(0, Math.min(position, max));
	}

}
