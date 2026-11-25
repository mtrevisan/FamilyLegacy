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
package io.github.mtrevisan.familylegacy.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.ego.EgoNode;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility class for rendering orthogonal connection lines between the
 * central Ego node and satellite nodes.
 * <p>
 * Every line is dotted: FLEF does not record whether a relationship is still active, so the node carries no status that
 * could choose another style.
 * <p>
 * The edge to a partner that is related to the ego (see {@link EgoNode#getConsanguineousUnion()}) carries a badge "C" in the middle.
 * It is a possibility to look into, as in the consanguinity report: the explanation is in the tooltip of the partner.
 */
final class EgoNetworkRenderer{

	private static final Stroke STROKE_UNKNOWN = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
		10f, new float[]{2f, 4f}, 0f);


	private static final int BADGE_RADIUS = 8;
	private static final Stroke BADGE_STROKE = new BasicStroke(1.2f);
	private static final Color BADGE_FILL = new Color(0xFFC107);
	private static final Color BADGE_BORDER = new Color(0x5D4037);
	private static final String BADGE_TEXT = "C";


	private EgoNetworkRenderer(){}


	/**
	 * Draws connection lines between the central Ego panel and all connected satellite panels.
	 *
	 * @param g2              the graphics context
	 * @param rootEgoNode     the root EgoNode containing relational structure
	 * @param nodeToPanelMap  map associating EgoNode objects to their UI JPanel
	 * @param groupToPanelMap map associating Group FLEFRecords to their UI JPanel
	 * @param container       the parent container for coordinate conversion
	 */
	public static void drawNetworkLines(final Graphics2D g2, final EgoNode rootEgoNode,
			final Map<EgoNode, JPanel> nodeToPanelMap, final Map<FLEFRecord, JPanel> groupToPanelMap,
			final Component container){
		if(rootEgoNode == null)
			return;

		final JPanel centerPanel = nodeToPanelMap.get(rootEgoNode);
		if(centerPanel == null)
			return;

		final Rectangle egoBounds = getPanelBounds(centerPanel, container);
		final SegmentLedger ledger = new SegmentLedger();

		final List<JPanel> targets = new ArrayList<>(0);
		final List<Boolean> consanguineous = new ArrayList<>(0);
		for(final EgoNode.RelationshipCategory category : new EgoNode.RelationshipCategory[]{EgoNode.RelationshipCategory.PARENT,
				EgoNode.RelationshipCategory.PARTNER, EgoNode.RelationshipCategory.CHILD, EgoNode.RelationshipCategory.ASSOCIATE})
			for(final EgoNode node : rootEgoNode.getRelatedNodes(category)){
				final JPanel panel = nodeToPanelMap.get(node);
				if(panel != null){
					targets.add(panel);
					//only partners carry a union mark
					consanguineous.add(node.getConsanguineousUnion() != null);
				}
			}
		for(final FLEFRecord groupRecord : rootEgoNode.getGroupRecords()){
			final JPanel panel = groupToPanelMap.get(groupRecord);
			if(panel != null){
				targets.add(panel);
				consanguineous.add(Boolean.FALSE);
			}
		}

		//a trunk for each side, placed in the gap between the ego and the nearest card of that side: the segments never run over
		//the cards, and the cards of a side hang from the same bar
		int rightTrunk = Integer.MAX_VALUE;
		int leftTrunk = Integer.MIN_VALUE;
		final List<Rectangle> bounds = new ArrayList<>(targets.size());
		for(final JPanel target : targets){
			final Rectangle r = getPanelBounds(target, container);
			bounds.add(r);
			if(r.x >= egoBounds.x + egoBounds.width)
				rightTrunk = Math.min(rightTrunk, r.x);
			else if(r.x + r.width <= egoBounds.x)
				leftTrunk = Math.max(leftTrunk, r.x + r.width);
		}
		if(rightTrunk != Integer.MAX_VALUE)
			rightTrunk = (egoBounds.x + egoBounds.width + rightTrunk) / 2;
		if(leftTrunk != Integer.MIN_VALUE)
			leftTrunk = (leftTrunk + egoBounds.x) / 2;

		final int egoCenterX = egoBounds.x + egoBounds.width / 2;
		final int egoCenterY = egoBounds.y + egoBounds.height / 2;
		for(int i = 0; i < bounds.size(); i ++){
			final Rectangle r = bounds.get(i);
			final int targetY = r.y + r.height / 2;
			final Point badge;
			g2.setStroke(STROKE_UNKNOWN);
			if(r.x >= egoBounds.x + egoBounds.width){
				final int startX = egoBounds.x + egoBounds.width;
				drawHorizontalBranch(g2, startX, egoCenterY, rightTrunk, r.x, targetY, ledger);
				badge = new Point((startX + r.x) / 2, targetY);
			}
			else if(r.x + r.width <= egoBounds.x){
				final int startX = egoBounds.x;
				drawHorizontalBranch(g2, startX, egoCenterY, leftTrunk, r.x + r.width, targetY, ledger);
				badge = new Point((startX + r.x + r.width) / 2, targetY);
			}
			else{
				//above or below: a vertical line from the edge of the ego to the edge of the card
				final int targetX = Math.max(egoBounds.x, Math.min(egoBounds.x + egoBounds.width, r.x + r.width / 2));
				final boolean above = (r.y + r.height / 2 < egoCenterY);
				final int fromY = (above? egoBounds.y: egoBounds.y + egoBounds.height);
				final int toY = (above? r.y + r.height: r.y);
				ledger.draw(g2, targetX, fromY, targetX, toY);
				badge = new Point(targetX, (fromY + toY) / 2);
			}
			if(consanguineous.get(i))
				drawConsanguinityBadge(g2, badge);
		}
	}

	/** Ego edge, horizontal to the trunk, along the trunk, horizontal to the card (straight when the heights coincide). */
	private static void drawHorizontalBranch(final Graphics2D g2, final int startX, final int startY, final int trunkX,
			final int endX, final int endY, final SegmentLedger ledger){
		if(startY == endY){
			ledger.draw(g2, startX, startY, endX, endY);
			return;
		}

		ledger.draw(g2, startX, startY, trunkX, startY);
		ledger.draw(g2, trunkX, startY, trunkX, endY);
		ledger.draw(g2, trunkX, endY, endX, endY);
	}

	private static Rectangle getPanelBounds(final JPanel panel, final Component container){
		return SwingUtilities.convertRectangle(panel, new Rectangle(0, 0, panel.getWidth(), panel.getHeight()), container);
	}

	/** Draws the badge that signals a consanguineous union: a small amber disc with a "C". */
	private static void drawConsanguinityBadge(final Graphics2D g2, final Point center){
		final Object antialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		final Font originalFont = g2.getFont();
		final Color originalColor = g2.getColor();
		final Stroke originalStroke = g2.getStroke();
		try{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final int x = center.x - BADGE_RADIUS;
			final int y = center.y - BADGE_RADIUS;
			final int diameter = 2 * BADGE_RADIUS;

			g2.setStroke(BADGE_STROKE);
			g2.setColor(BADGE_FILL);
			g2.fillOval(x, y, diameter, diameter);
			g2.setColor(BADGE_BORDER);
			g2.drawOval(x, y, diameter, diameter);

			g2.setFont(originalFont.deriveFont(Font.BOLD, BADGE_RADIUS + 1f));
			final FontMetrics metrics = g2.getFontMetrics();
			g2.drawString(BADGE_TEXT, center.x - metrics.stringWidth(BADGE_TEXT) / 2,
				center.y + (metrics.getAscent() - metrics.getDescent()) / 2);
		}
		finally{
			g2.setStroke(originalStroke);
			g2.setColor(originalColor);
			g2.setFont(originalFont);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				(antialiasing != null? antialiasing: RenderingHints.VALUE_ANTIALIAS_DEFAULT));
		}
	}

	/**
	 * Remembers the axis-aligned segments already drawn, so that a segment (or part of it) lying over another one is not
	 * drawn again: a dotted line drawn twice, with a different dash phase, would look solid.
	 */
	private static final class SegmentLedger{

		/** Covered intervals of the horizontal segments, by y: each interval is {from, to}. */
		private final Map<Integer, List<int[]>> horizontal = new HashMap<>(0);
		/** Covered intervals of the vertical segments, by x: each interval is {from, to}. */
		private final Map<Integer, List<int[]>> vertical = new HashMap<>(0);


		void draw(final Graphics2D g2, final int x1, final int y1, final int x2, final int y2){
			if(y1 == y2){
				for(final int[] part : uncovered(horizontal.computeIfAbsent(y1, k -> new ArrayList<>(1)),
						Math.min(x1, x2), Math.max(x1, x2)))
					g2.drawLine(part[0], y1, part[1], y1);
			}
			else if(x1 == x2){
				for(final int[] part : uncovered(vertical.computeIfAbsent(x1, k -> new ArrayList<>(1)),
						Math.min(y1, y2), Math.max(y1, y2)))
					g2.drawLine(x1, part[0], x1, part[1]);
			}
			else
				g2.drawLine(x1, y1, x2, y2);
		}

		/** Returns the parts of [from, to] not yet covered, and marks the whole interval as covered. */
		private static List<int[]> uncovered(final List<int[]> covered, final int from, final int to){
			final List<int[]> result = new ArrayList<>(1);
			int cursor = from;
			final List<int[]> sorted = new ArrayList<>(covered);
			sorted.sort(Comparator.comparingInt(interval -> interval[0]));
			for(final int[] interval : sorted){
				if(interval[1] <= cursor)
					continue;
				if(interval[0] >= to)
					break;

				if(interval[0] > cursor)
					result.add(new int[]{cursor, interval[0]});
				cursor = Math.max(cursor, interval[1]);
			}
			if(cursor < to)
				result.add(new int[]{cursor, to});
			if(from < to)
				covered.add(new int[]{from, to});
			return result;
		}

	}

}
