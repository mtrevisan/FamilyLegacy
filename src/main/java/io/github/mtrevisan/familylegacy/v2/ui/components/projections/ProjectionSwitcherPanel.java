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

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.IndividualTreePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph.EgoNetworkPanel;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


/**
 * Container that hosts the ancestor tree and the ego network views side by
 * side and allows the user to switch between them with a keyboard
 * shortcut ({@code Ctrl+Tab}).
 * <p>
 * The switcher guarantees that the two views remain synchronized on the
 * same root individual: whenever the user switches, the current root of
 * the outgoing view is read and applied to the incoming view before the
 * transition begins. The two views therefore always represent the same
 * person, only from two different perspectives.
 * <p>
 * The transition is a cross-fade: before swapping the panels, the outgoing
 * view is captured into an off-screen image; the incoming view is then
 * installed and painted normally, and the captured image is drawn on top
 * of it with a decreasing alpha value. This gives the visual impression of
 * a soft dissolve without requiring both panels to be visible at the same
 * time, which keeps the Swing component hierarchy simple and avoids
 * layout artifacts.
 * <p>
 * The switcher is not thread-safe and is meant to be used from the Swing
 * Event Dispatch Thread.
 */
public final class ProjectionSwitcherPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 4815718233118059642L;


	/** Default generation depth used when restoring the tree view. */
	private static final int DEFAULT_MAX_GENERATIONS = 4;
	/** Duration of the cross-fade, in milliseconds. */
	private static final int TRANSITION_DURATION_MS = 260;
	/** Interval between animation frames, in milliseconds (~60 fps). */
	private static final int TRANSITION_FRAME_MS = 16;
	/** Alpha value below which the outgoing snapshot is discarded. */
	private static final float MIN_VISIBLE_ALPHA = 0.01f;

	private static final String ACTION_SWITCH_PROJECTION = "switchProjection";


	private final IndividualTreePanel treePanel;
	private final EgoNetworkPanel egoPanel;

	/**
	 * The panel currently installed as the single child of this container.
	 * Always equal to either {@link #treePanel} or {@link #egoPanel}.
	 */
	private JPanel currentPanel;

	/**
	 * Off-screen snapshot of the outgoing panel, drawn on top of the
	 * incoming panel during a transition. {@code null} outside of a
	 * transition.
	 */
	private BufferedImage outgoingSnapshot;

	/**
	 * Opacity of {@link #outgoingSnapshot} during a transition, between 1
	 * (fully visible, transition just started) and 0 (invisible, transition
	 * finished).
	 */
	private float outgoingAlpha;

	/**
	 * Timer driving the fade animation. {@code null} outside of a
	 * transition.
	 */
	private Timer transitionTimer;


	/**
	 * Constructor.
	 *
	 * @param model the FLEF model shared by both views (must not be
	 *              {@code null})
	 */
	public ProjectionSwitcherPanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.treePanel = new IndividualTreePanel(TreeType.BIOLOGICAL, TreeLayout.VERTICAL, model)
			.withShowPartner();
		this.egoPanel = new EgoNetworkPanel(TreeLayout.VERTICAL, model);

		setLayout(new BorderLayout());
		currentPanel = treePanel;
		add(treePanel, BorderLayout.CENTER);

		setupSwitchShortcut(this);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Loads the given individual as the root of the currently visible
	 * view. The other view is left untouched until the next switch.
	 *
	 * @param individualId the id of the individual to use as root; may be
	 *                     {@code null} to leave the view unchanged
	 */
	public void loadRoot(final String individualId){
		if(individualId == null)
			return;

		if(currentPanel == treePanel)
			treePanel.loadTree(individualId, DEFAULT_MAX_GENERATIONS);
		else
			egoPanel.loadNetwork(individualId);
	}

	/**
	 * Returns the panel currently displayed.
	 *
	 * @return the current panel, never {@code null}
	 */
	public JPanel getCurrentPanel(){
		return currentPanel;
	}

	/**
	 * Returns the ancestor tree panel.
	 *
	 * @return the tree panel, never {@code null}
	 */
	public IndividualTreePanel getTreePanel(){
		return treePanel;
	}

	/**
	 * Returns the ego network panel.
	 *
	 * @return the ego panel, never {@code null}
	 */
	public EgoNetworkPanel getEgoPanel(){
		return egoPanel;
	}


	/* ======================================================================
	 *                          Shortcut
	 * ====================================================================== */

	/**
	 * Installs the {@code Ctrl+Tab} shortcut that switches between the two
	 * views.
	 *
	 * @param component the component that receives the shortcut
	 */
	private void setupSwitchShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_E_STROKE, ACTION_SWITCH_PROJECTION);
		actionMap.put(ACTION_SWITCH_PROJECTION, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -5518239024119050317L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switchProjection();
			}
		});
	}


	/* ======================================================================
	 *                          Switching
	 * ====================================================================== */

	/**
	 * Switches to the other view, synchronizing the root individual and
	 * starting the fade transition.
	 */
	private void switchProjection(){
		if(currentPanel == treePanel){
			final String rootId = treePanel.getRootIndividualId();
			if(rootId != null)
				egoPanel.loadNetwork(rootId);

			transitionTo(egoPanel);
		}
		else{
			final String rootId = egoPanel.getCurrentEgoId();
			if(rootId != null)
				treePanel.loadTree(rootId, DEFAULT_MAX_GENERATIONS);

			transitionTo(treePanel);
		}
	}

	/**
	 * Installs the given panel as the new visible view and starts the fade
	 * transition from the previous one.
	 *
	 * @param target the panel to show (must not be {@code null})
	 */
	private void transitionTo(final JPanel target){
		if(target == null || target == currentPanel)
			return;

		// Cancel any in-flight transition before starting a new one.
		stopTransition();

		// Capture the outgoing panel while it is still laid out and visible.
		// If the container has no size yet (e.g. not shown), skip the
		// animation entirely and perform an immediate swap.
		final int width = getWidth();
		final int height = getHeight();
		if(width <= 0 || height <= 0){
			swapPanels(target);

			return;
		}

		outgoingSnapshot = snapshot(currentPanel, width, height);
		outgoingAlpha = 1.f;

		// Install the incoming panel and force the layout to run before
		// the first frame of the transition, so that the user never sees
		// an empty area underneath the fading snapshot.
		swapPanels(target);

		startTransitionTimer();
	}

	/**
	 * Removes the current panel, installs the target panel as the single
	 * child, and forces a synchronous layout pass.
	 *
	 * @param target the panel to install
	 */
	private void swapPanels(final JPanel target){
		removeAll();
		add(target, BorderLayout.CENTER);
		currentPanel = target;

		revalidate();

		// Force layout synchronously, so that the incoming panel is fully
		// painted on the very first frame of the transition.
		validate();
		repaint();
	}


	/* ======================================================================
	 *                          Fade animation
	 * ====================================================================== */

	private void startTransitionTimer(){
		final long startTime = System.currentTimeMillis();
		transitionTimer = new Timer(TRANSITION_FRAME_MS, e -> {
			final long elapsed = System.currentTimeMillis() - startTime;
			final float progress = Math.min(1.f, elapsed / (float)TRANSITION_DURATION_MS);

			outgoingAlpha = 1.f - progress;
			if(outgoingAlpha <= MIN_VISIBLE_ALPHA){
				outgoingAlpha = 0.f;
				outgoingSnapshot = null;
				stopTransition();
			}
			repaint();
		});
		transitionTimer.start();
	}

	private void stopTransition(){
		if(transitionTimer != null){
			transitionTimer.stop();
			transitionTimer = null;
		}
		outgoingSnapshot = null;
		outgoingAlpha = 0.f;
	}


	/* ======================================================================
	 *                          Painting
	 * ====================================================================== */

	@Override
	public void paint(final Graphics g){
		// Paint the container and its children (the incoming panel) first.
		super.paint(g);

		// Then overlay the snapshot of the outgoing panel, if a transition
		// is in progress. The snapshot is drawn with decreasing opacity,
		// producing a cross-fade.
		if(outgoingSnapshot != null && outgoingAlpha > 0.f){
			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, outgoingAlpha));
				g2.drawImage(outgoingSnapshot, 0, 0, null);
			}
			finally{
				g2.dispose();
			}
		}
	}

	/**
	 * Captures the given panel into an off-screen image of the requested
	 * size.
	 *
	 * @param component the panel to capture (must not be {@code null})
	 * @param width     the width of the image, in pixels
	 * @param height    the height of the image, in pixels
	 * @return the captured image
	 */
	private static BufferedImage snapshot(final JComponent component, final int width, final int height){
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try{
			component.paint(g);
		}
		finally{
			g.dispose();
		}
		return image;
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";
		final String individualId = "I1";

		final String content;
		try(final InputStream is = ProjectionSwitcherPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final ProjectionSwitcherPanel panel = new ProjectionSwitcherPanel(model);
			panel.loadRoot(individualId);

			final JFrame frame = new JFrame("Projection Switcher View");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
