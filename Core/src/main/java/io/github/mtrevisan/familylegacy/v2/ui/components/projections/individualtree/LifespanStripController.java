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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.CollapsibleBar;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.lifespan.MultiLifespanStripPanel;

import java.awt.Component;
import java.util.Set;


/**
 * Manages the collapsible lifespan strip at the bottom of the tree.
 * <p>
 * The strip is a read-only view: it lists the lifespans of every
 * individual currently visible in the tree. Its content and its
 * visibility are two independent concerns, both handled here.
 * <p>
 * The visibility is toggled by the {@code Ctrl+T} shortcut and by the
 * collapsible bar. The content is refreshed after every tree rebuild
 * by walking the canvas and collecting the visible individual ids.
 */
public final class LifespanStripController{

	private final MultiLifespanStripPanel strip;
	private final CollapsibleBar toggleBar;
	private final Runnable onVisibilityChanged;

	private boolean visible;


	public LifespanStripController(final MultiLifespanStripPanel strip, final CollapsibleBar toggleBar,
			final Runnable onVisibilityChanged){
		this.strip = strip;
		this.toggleBar = toggleBar;
		this.onVisibilityChanged = onVisibilityChanged;

		// The strip starts hidden; the toggle bar reflects that.
		strip.setVisible(false);
		toggleBar.setExpanded(false);
	}


	/** Toggles the strip on or off. */
	public void toggle(){
		visible = !visible;
		strip.setVisible(visible);
		toggleBar.setExpanded(visible);
		if(onVisibilityChanged != null)
			onVisibilityChanged.run();
	}

	/**
	 * Refreshes the strip content by collecting the ids of every visible
	 * individual in the given canvas.
	 */
	public void update(final Component treeCanvas){
		final Set<String> ids = IndividualPanelFinder.collectIds(treeCanvas);
		strip.setIndividuals(ids);
	}

}
