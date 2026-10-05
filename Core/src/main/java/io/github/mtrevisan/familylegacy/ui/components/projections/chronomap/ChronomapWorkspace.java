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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;


/**
 * Workspace that combines the chronomap (spatial view) and the agora
 * (population list) into a single panel.
 *
 * <p>The two views share one {@link ChronomapIndex}, one
 * {@link PlaceCoordinateResolver}, one {@link ChronomapTimeline} and one
 * {@link WorkspaceSelection}. Because the timeline and the selection are
 * shared, moving the playhead updates both views simultaneously, and
 * selecting an individual in one view highlights it in the other.</p>
 *
 * <p>Layout: a horizontal split with the map on the left and the list on
 * the right, and the timeline spanning the full width below both. The
 * split ratio defaults to 0.65, favoring the map, but the user can drag
 * the divider at any time.</p>
 */
public final class ChronomapWorkspace extends JPanel{

	private static final double DEFAULT_SPLIT_RATIO = 0.65;


	private final ChronomapIndex index;
	private final ChronomapTimeline timeline;
	private final WorkspaceSelection selection = new WorkspaceSelection();

	private final ChronomapPanel mapPanel;
	private final AgoraPanel agoraPanel;


	/* ======================================================================
	 *                          Construction
	 * ====================================================================== */

	/**
	 * Creates a workspace with the default on-disk geocoding cache
	 * ({@code ~/.familylegacy/geocoding.properties}).
	 */
	public ChronomapWorkspace(final FLEFModel model){
		this(model, defaultCacheFile());
	}


	/**
	 * Creates a workspace with an explicit geocoding cache file. Useful
	 * for tests or for applications that store the cache in a
	 * different location.
	 */
	public ChronomapWorkspace(final FLEFModel model, final Path cacheFile){
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, cacheFile);
		this.index = new ChronomapIndex(model, resolver);
		this.timeline = new ChronomapTimeline();

		this.mapPanel = ChronomapPanel.create(model, index, resolver, timeline, selection);
		this.agoraPanel = new AgoraPanel(model, index, timeline, selection);

		buildUI();
		initializeTimeline();
	}


	private void buildUI(){
		setLayout(new BorderLayout());

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel, agoraPanel);
		split.setResizeWeight(DEFAULT_SPLIT_RATIO);
		split.setContinuousLayout(true);
		split.setOneTouchExpandable(true);

		add(split, BorderLayout.CENTER);
		add(timeline, BorderLayout.SOUTH);
	}


	/**
	 * Sets the temporal domain of the shared timeline to the global
	 * range of the whole model, if a range can be determined.
	 */
	private void initializeTimeline(){
		final long[] range = index.computeGlobalDateRange();
		if(range != null && range[0] < range[1])
			timeline.setDomain(range[0], range[1]);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Sets the individuals shown by both the map and the list. The call
	 * is forwarded to both panels in one shot, so the map and the list
	 * always show the same population.
	 */
	public void setIndividuals(final Collection<String> ids){
		final List<String> visible = (ids != null? List.copyOf(ids): List.of());
		mapPanel.setIndividuals(visible);
		agoraPanel.setIndividuals(visible);
	}


	/** Exposes the shared selection, for external synchronization. */
	public WorkspaceSelection selection(){
		return selection;
	}


	/** Exposes the shared index. */
	public ChronomapIndex index(){
		return index;
	}


	/** Exposes the shared timeline. */
	public ChronomapTimeline timeline(){
		return timeline;
	}


	private static Path defaultCacheFile(){
		return Path.of(System.getProperty("user.home"),
			".familylegacy", "geocoding.properties");
	}


	/* ======================================================================
	 *                          Demo
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final String content;
		try(final InputStream is = ChronomapWorkspace.class.getResourceAsStream("/tests/TGMZ.flef")){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		final FLEFModel model = new FLEFParser().parse(content);

		SwingUtilities.invokeLater(() -> {
			final ChronomapWorkspace workspace = new ChronomapWorkspace(model);
			workspace.setIndividuals(model.getRecordsByType(IndividualHandler.TYPE)
				.stream()
				.map(FLEFRecord::getId)
				.toList());

			final JFrame frame = new JFrame("Chronomap workspace");
			frame.add(workspace, BorderLayout.CENTER);
			frame.setSize(1400, 820);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
