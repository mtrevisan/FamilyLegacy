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

import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;


/**
 * Every modal dialog appears centered, and not taller than the screen allows.
 * <p>
 * A dialog is centered over its owner (over the screen when it has none, or the owner is not on the screen), whatever the order in which
 * its constructor packed it, set its size and asked to be centered: a dialog that is packed after being centered, or whose minimum size
 * is set after, ends up with its corner where the center was. Doing it here, when the dialog is opened, does not depend on each
 * constructor. A dialog that is too tall (a report with all its text in a text area asks for the height of all of it) is brought to
 * {@value #MAX_HEIGHT_PERCENT} % of the height of the screen, and one that is too wide to {@value #MAX_WIDTH_PERCENT} % of its width;
 * the rest scrolls, if it can.
 * </p>
 */
public final class DialogPlacement{

	/** The most height of a dialog, as a percentage of the height of the screen that it is on (without the bars of the system). */
	static final int MAX_HEIGHT_PERCENT = 85;
	/** The most width of a dialog, as a percentage of the width of the screen. */
	static final int MAX_WIDTH_PERCENT = 95;

	private static boolean installed;


	private DialogPlacement(){}


	/** Centers the modal dialogs of the whole application as they open (once: calling it again does nothing). */
	public static synchronized void install(){
		if(installed)
			return;

		installed = true;
		Toolkit.getDefaultToolkit()
			.addAWTEventListener(event -> {
				if(event.getID() == WindowEvent.WINDOW_OPENED && event.getSource() instanceof Dialog dialog && dialog.isModal())
					place(dialog);
			}, AWTEvent.WINDOW_EVENT_MASK);
	}

	/** Centers the dialog over its owner (the screen if it has none) and brings it into the screen. */
	public static void place(final Window window){
		final Window owner = window.getOwner();
		final GraphicsConfiguration configuration = (owner != null && owner.isShowing()? owner.getGraphicsConfiguration()
			: window.getGraphicsConfiguration());
		final Rectangle usable = usableArea(configuration);
		final Rectangle reference = (owner != null && owner.isShowing()? owner.getBounds(): usable);

		final Rectangle placed = place(window.getBounds(), reference, usable);
		if(!placed.equals(window.getBounds()))
			SwingUtilities.invokeLater(() -> window.setBounds(placed));
	}

	/**
	 * @param dialog	The bounds of the dialog.
	 * @param reference	The bounds of what it is centered over (its owner, or the usable area of the screen).
	 * @param usable	The usable area of the screen: the one without the bars of the system.
	 * @return	The bounds of the dialog: not larger than the share of the screen it may take, centered over the reference, and inside the
	 * 	usable area (the center of the reference moves, if it would take the dialog out of it).
	 */
	static Rectangle place(final Rectangle dialog, final Rectangle reference, final Rectangle usable){
		final int width = Math.min(dialog.width, usable.width * MAX_WIDTH_PERCENT / 100);
		final int height = Math.min(dialog.height, usable.height * MAX_HEIGHT_PERCENT / 100);
		int x = reference.x + (reference.width - width) / 2;
		int y = reference.y + (reference.height - height) / 2;
		x = Math.max(usable.x, Math.min(x, usable.x + usable.width - width));
		y = Math.max(usable.y, Math.min(y, usable.y + usable.height - height));
		return new Rectangle(x, y, width, height);
	}


	private static Rectangle usableArea(final GraphicsConfiguration configuration){
		final GraphicsConfiguration screen = (configuration != null? configuration
			: GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
		final Rectangle bounds = screen.getBounds();
		final Insets insets = Toolkit.getDefaultToolkit()
			.getScreenInsets(screen);
		return new Rectangle(bounds.x + insets.left, bounds.y + insets.top, bounds.width - insets.left - insets.right,
			bounds.height - insets.top - insets.bottom);
	}

}
