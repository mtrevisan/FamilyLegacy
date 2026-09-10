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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.socialnetworkgraph;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.swing_viewer.DefaultView;
import org.graphstream.ui.view.camera.Camera;
import org.graphstream.ui.view.util.InteractiveElement;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.EnumSet;


class MouseNavigationHandler extends MouseAdapter{

	private final SocialNetworkGraphPanel panel;
	private final DefaultView view;
	private final Graph graph;

	private Point pressPoint;
	private Point3 initialViewCenter;
	private GraphicElement draggedElement;
	private boolean isDragging;


	public MouseNavigationHandler(final SocialNetworkGraphPanel panel, final DefaultView view, final Graph graph){
		this.panel = panel;
		this.view = view;
		this.graph = graph;
	}


	@Override
	public void mousePressed(final MouseEvent e){
		if(SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)){
			pressPoint = e.getPoint();
			initialViewCenter = new Point3(view.getCamera().getViewCenter());
			isDragging = false;

			if(SwingUtilities.isLeftMouseButton(e))
				draggedElement = view.findGraphicElementAt(EnumSet.of(InteractiveElement.NODE), e.getX(), e.getY());
		}
	}

	@Override
	public void mouseDragged(final MouseEvent e){
		if(pressPoint == null)
			return;

		final Point currentPoint = e.getPoint();
		final int dx = currentPoint.x - pressPoint.x;
		final int dy = currentPoint.y - pressPoint.y;

		if(Math.abs(dx) > 2 || Math.abs(dy) > 2)
			isDragging = true;

		if(isDragging){
			final Camera camera = view.getCamera();

			if(draggedElement != null){
				// 1. NODE DRAG: Freeze layout calculations while actively holding/moving the node
				final Node node = graph.getNode(draggedElement.getId());
				if(node != null){
					node.setAttribute("layout.frozen");

					final Point3 mouseGu = camera.transformPxToGu(currentPoint.x, currentPoint.y);
					node.setAttribute("xyz", mouseGu.x, mouseGu.y, 0.0);
				}
			}
			else if(initialViewCenter != null){
				// 2. CANVAS PAN: Translate camera view center
				final Point3 startGu = camera.transformPxToGu(pressPoint.x, pressPoint.y);
				final Point3 currentGu = camera.transformPxToGu(currentPoint.x, currentPoint.y);

				final double deltaX = startGu.x - currentGu.x;
				final double deltaY = startGu.y - currentGu.y;

				camera.setViewCenter(initialViewCenter.x + deltaX, initialViewCenter.y + deltaY, initialViewCenter.z);
			}
			view.repaint();
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e){
		if(SwingUtilities.isLeftMouseButton(e)){
			if(isDragging && draggedElement != null){
				// Unfreeze node so auto-layout algorithm resumes optimization from the new position
				final Node node = graph.getNode(draggedElement.getId());
				if(node != null)
					node.removeAttribute("layout.frozen");
			}
			else if(!isDragging){
				// Single left click on background: Recenter view
				final Camera camera = view.getCamera();
				final Point3 clickPointGu = camera.transformPxToGu(e.getX(), e.getY());
				camera.setViewCenter(clickPointGu.x, clickPointGu.y, clickPointGu.z);
				view.repaint();
			}
		}
		else if(SwingUtilities.isRightMouseButton(e)){
			if(!isDragging){
				// Single right click: Trigger record action dialog
				final GraphicElement element = view.findGraphicElementAt(EnumSet.of(InteractiveElement.NODE), e.getX(),
					e.getY());

				if(element != null)
					panel.buttonPushed(element.getId());
			}
		}

		pressPoint = null;
		initialViewCenter = null;
		draggedElement = null;
		isDragging = false;
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e){
		final Point mousePt = e.getPoint();
		final Camera camera = view.getCamera();

		final Point3 beforeZoomGu = camera.transformPxToGu(mousePt.x, mousePt.y);

		final double factor = (e.getPreciseWheelRotation() < 0? 0.85: 1.15);
		final double currentPercent = camera.getViewPercent();
		final double newPercent = Math.clamp(currentPercent * factor, 0.01, 10.);

		camera.setViewPercent(newPercent);

		final Point3 afterZoomGu = camera.transformPxToGu(mousePt.x, mousePt.y);

		final Point3 currentCenter = camera.getViewCenter();
		final double deltaX = beforeZoomGu.x - afterZoomGu.x;
		final double deltaY = beforeZoomGu.y - afterZoomGu.y;

		camera.setViewCenter(currentCenter.x + deltaX, currentCenter.y + deltaY, currentCenter.z);
		view.repaint();
	}

}
