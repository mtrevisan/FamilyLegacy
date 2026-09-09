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
package io.github.mtrevisan.familylegacy.v2.ui.components.socialnetworkgraph;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.GroupRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swing_viewer.DefaultView;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


public class SocialNetworkGraphPanel extends JPanel implements ViewerListener{

	@Serial
	private static final long serialVersionUID = 8468882751539785904L;


	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_TYPE = "type";
	private static final String TAG_ROLE = "role";

	private static final String CSS_STYLES = """
		node {
			text-alignment: at-right;
			text-padding: 3px;
			text-background-mode: plain;
			text-background-color: #FFFFFF88;
			text-size: 12;
		}
		node.individual {
			shape: circle;
			fill-color: #4A90E2;
			size: 20px;
			stroke-mode: plain;
			stroke-color: #1D5698;
		}
		node.group {
			shape: box;
			fill-color: #E67E22;
			size: 24px;
			stroke-mode: plain;
			stroke-color: #A04000;
		}
		edge {
			fill-color: #BDC3C7;
			size: 1.5px;
			text-size: 10;
			text-color: #555555;
		}
		""";


	private final Dialog parentDialog;
	private final FLEFModel model;

	private final Graph graph;
	private final DefaultView view;
	private final ViewerPipe viewerPipe;
	private final Timer pumpTimer;

	private String targetIndividualId;
	private Double targetZoomLevel;
	private boolean loop = true;


	public SocialNetworkGraphPanel(final Dialog parentDialog, final FLEFModel model){
		this(parentDialog, model, null);
	}

	public SocialNetworkGraphPanel(final Dialog parentDialog, final FLEFModel model, final String centerIndividualId){
		super(new BorderLayout());

		this.parentDialog = parentDialog;
		this.model = Objects.requireNonNull(model);
		targetIndividualId = centerIndividualId;

		System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		graph = new SingleGraph("SocialNetwork");
		graph.setAttribute("ui.stylesheet", CSS_STYLES);
		graph.setAttribute("ui.quality");
		graph.setAttribute("ui.antialias");
		graph.setAttribute("layout.force", 1.5);
		graph.setAttribute("layout.stabilization-limit", 0.9);

		final SwingViewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
		viewer.enableAutoLayout();

		view = (DefaultView)viewer.addDefaultView(false);

		// Completely disable the native shortcut manager and remove default listeners
		view.setShortcutManager(null);
		for(final java.awt.event.MouseListener ml : view.getMouseListeners())
			view.removeMouseListener(ml);
		for(final java.awt.event.MouseMotionListener mml : view.getMouseMotionListeners())
			view.removeMouseMotionListener(mml);
		for(final java.awt.event.MouseWheelListener mwl : view.getMouseWheelListeners())
			view.removeMouseWheelListener(mwl);

		final MouseNavigationHandler navigationHandler = new MouseNavigationHandler(this, view, graph);
		view.addMouseListener(navigationHandler);
		view.addMouseMotionListener(navigationHandler);
		view.addMouseWheelListener(navigationHandler);

		add(view, BorderLayout.CENTER);

		setupKeyboardShortcuts();

		viewerPipe = viewer.newViewerPipe();
		viewerPipe.addViewerListener(this);
		viewerPipe.addSink(graph);

		pumpTimer = new Timer(50, e -> {
			if(loop){
				viewerPipe.pump();

				centerCameraOnTargetNode();
			}
		});
		pumpTimer.start();

		buildGraph();
	}

	public void centerAndZoomOnIndividual(final String individualId){
		centerAndZoomOnIndividual(individualId, 0.5);
	}

	public void centerAndZoomOnIndividual(final String individualId, final double zoomLevel){
		this.targetIndividualId = individualId;
		this.targetZoomLevel = zoomLevel;
	}

	private void setupKeyboardShortcuts(){
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(GUIHelper.CTRL_F_STROKE, "openSearchDialog");
		getActionMap().put("openSearchDialog", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				openSearchDialog();
			}
		});
	}

	private void openSearchDialog(){
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(null, model,
			(record, handler) -> centerAndZoomOnIndividual(record.getId()),
			IndividualHandler.class, GroupHandler.class);
		dialog.setVisible(true);
	}

	public final void buildGraph(){
		graph.clear();
		graph.setAttribute("ui.stylesheet", CSS_STYLES);

		// 1. Add Individual Nodes
		final Collection<FLEFRecord> individuals = model.getRecordsByType(IndividualHandler.TYPE);
		for(final FLEFRecord record : individuals){
			final String id = record.getId();
			final Node node = graph.addNode(id);
			node.setAttribute("ui.class", "individual");
			node.setAttribute("ui.label", getRecordLabel(record, "INDIVIDUAL"));
			node.setAttribute("record", record);
		}

		// 2. Add Group Nodes provisionally
		final Collection<FLEFRecord> groups = model.getRecordsByType(GroupHandler.TYPE);
		for(final FLEFRecord record : groups){
			final String id = record.getId();
			final Node node = graph.addNode(id);
			node.setAttribute("ui.class", "group");
			node.setAttribute("ui.label", getRecordLabel(record, "GROUP"));
			node.setAttribute("record", record);
		}

		// 3. Add Relationship Edges
		final Collection<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		int edgeIdCounter = 0;
		for(final FLEFRecord relationship : relationships){
			final FLEFRecord subject = FLEFRecordHelper.extractRecordsFromOneOfReference(relationship, TAG_SUBJECT, model)
				.getFirst();
			final FLEFRecord target = FLEFRecordHelper.extractRecordsFromOneOfReference(relationship, TAG_TARGET, model)
				.getFirst();

			if(subject != null && target != null){
				final String subjectId = subject.getId();
				final String targetId = target.getId();

				final Node sourceNode = graph.getNode(subjectId);
				final Node targetNode = graph.getNode(targetId);

				if(sourceNode != null && targetNode != null){
					Edge edge = sourceNode.getEdgeBetween(targetNode);
					if(edge == null)
						edge = targetNode.getEdgeBetween(sourceNode);

					final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
					final String role = FLEFRecordHelper.getChildValue(relationship, TAG_ROLE);
					final String label = (role != null && !role.isEmpty() ? role : type);

					if(edge == null){
						final String edgeId = "edge_" + (++edgeIdCounter);
						edge = graph.addEdge(edgeId, subjectId, targetId, true);
						if(label != null && !label.isEmpty())
							edge.setAttribute("ui.label", label);
					}
					else{
						final String currentLabel = (edge.hasAttribute("ui.label")
							? edge.getAttribute("ui.label").toString()
							: StringUtils.EMPTY);
						final String updatedLabel = (label != null && !label.isEmpty() && !currentLabel.contains(label)
							? (currentLabel.isEmpty()? label: currentLabel + " / " + label)
							: currentLabel);

						final String existingId = edge.getId();
						graph.removeEdge(edge);

						final Edge undirectedEdge = graph.addEdge(existingId, subjectId, targetId, false);
						if(!updatedLabel.isEmpty())
							undirectedEdge.setAttribute("ui.label", updatedLabel);
					}
				}
			}
		}

		// 4. Prune isolated GROUP nodes (degree == 0)
		final List<Node> groupsToRemove = new ArrayList<>();
		for(final Node node : graph)
			if("group".equals(node.getAttribute("ui.class")) && node.getDegree() == 0)
				groupsToRemove.add(node);
		for(final Node node : groupsToRemove)
			graph.removeNode(node);
	}

	private void centerCameraOnTargetNode(){
		if(targetIndividualId == null)
			return;

		final Node targetNode = graph.getNode(targetIndividualId);
		if(targetNode != null && targetNode.hasAttribute("xyz")){
			final Object[] xyz = (Object[])targetNode.getAttribute("xyz");
			if(xyz != null && xyz.length >= 2){
				final double x = ((Number)xyz[0]).doubleValue();
				final double y = ((Number)xyz[1]).doubleValue();
				final double z = (xyz.length > 2? ((Number)xyz[2]).doubleValue(): 0.);

				view.getCamera().setViewCenter(x, y, z);

				if(targetZoomLevel != null){
					view.getCamera().setViewPercent(targetZoomLevel);
					targetZoomLevel = null;
				}

				// Reset target ID to allow free panning/navigation afterwards
				targetIndividualId = null;
			}
		}
	}

	private String getRecordLabel(final FLEFRecord record, final String type){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(type);
		if(handler != null)
			return record.getId();

		return record.getId();
	}

	@Override
	public void buttonPushed(final String id){
		final Node node = graph.getNode(id);
		if(node == null)
			return;

		final FLEFRecord record = (FLEFRecord)node.getAttribute("record");
		if(record != null)
			SwingUtilities.invokeLater(() -> openRecordDialog(record));
	}

	private void openRecordDialog(final FLEFRecord record){
		if(IndividualHandler.TYPE.equalsIgnoreCase(record.getTag())){
			final IndividualRecordDialog dialog = IndividualRecordDialog.createEdit(parentDialog, model, record);
			dialog.setVisible(true);
		}
		else if(GroupHandler.TYPE.equalsIgnoreCase(record.getTag())){
			final GroupRecordDialog dialog = GroupRecordDialog.createEdit(parentDialog, model, record);
			dialog.setVisible(true);
		}
	}

	@Override
	public void buttonReleased(final String id){}

	@Override
	public void mouseOver(final String id){}

	@Override
	public void mouseLeft(final String id){}

	@Override
	public void viewClosed(final String viewName){
		loop = false;
		if(pumpTimer != null)
			pumpTimer.stop();
	}


	public static void main(final String[] args) throws Exception{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";
		final String content;
		try(final InputStream is = SocialNetworkGraphPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame("Social Network Graph Test");
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setSize(1000, 700);
			frame.setLocationRelativeTo(null);

			final SocialNetworkGraphPanel graphPanel = new SocialNetworkGraphPanel(null, model);
			frame.add(graphPanel);

			frame.setVisible(true);
		});
	}

}
