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


	public LifespanStripController(final MultiLifespanStripPanel strip,
		final CollapsibleBar toggleBar, final Runnable onVisibilityChanged){
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
