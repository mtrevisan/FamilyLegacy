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
package io.github.mtrevisan.familylegacy.ui.components.projections.groupnetwork;

import io.github.mtrevisan.familylegacy.groups.GroupNetwork;
import io.github.mtrevisan.familylegacy.groups.GroupNetworkBuilder;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline.GroupYearControl;
import io.github.mtrevisan.familylegacy.ui.helpers.ViewportPanSupport;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * The network of the groups around a group (view B of the group views, see {@link GroupNetwork}): the groups as boxes, the wholes above
 * their parts, a thick line for {@code part_of} and a dashed one for {@code associate}, a badge on every group with its number of members.
 * <p>
 * The view is always a neighborhood of a group ({@code Levels}: how many relationships away), and can be restricted to a year, to one kind
 * of relationship, and made to count the members of the subgroups. A click on a group selects it, a double click puts it at the center; a
 * drag moves the view, the buttons or Ctrl and the wheel zoom it. A tooltip tells the group (its name and members) or the relationship under
 * the pointer (what it is, its role and its dates). The layout is {@link GroupNetworkLayout}.
 */
public final class GroupNetworkPanel extends JPanel{

	private static final Color COLOR_NODE = new Color(238, 242, 247);
	private static final Color COLOR_CENTER = new Color(255, 233, 168);
	private static final Color COLOR_SELECTED = new Color(207, 227, 255);
	private static final Color COLOR_BORDER = new Color(90, 100, 115);
	private static final Color COLOR_TEXT = new Color(40, 40, 40);
	private static final Color COLOR_BADGE = new Color(40, 110, 120);
	private static final Color COLOR_EDGE = new Color(90, 100, 115);
	private static final Stroke STROKE_PART_OF = new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke STROKE_ASSOCIATE = new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
		new float[]{7f, 5f}, 0f);
	private static final int BADGE_RADIUS = 11;
	private static final int MAX_DEPTH = 6;
	/** How near to a relationship, in pixels of the screen, the pointer has to be to tell about it. */
	private static final double EDGE_HIT_DISTANCE = 5.;
	private static final double MIN_ZOOM = 0.3;
	private static final double MAX_ZOOM = 3.;
	private static final double ZOOM_STEP = 1.25;

	private final FLEFModel model;
	private final Canvas canvas = new Canvas();
	private final JScrollPane scrollPane = new JScrollPane(canvas);
	private final GroupYearControl yearControl = new GroupYearControl();
	private final JSpinner depthSpinner = new JSpinner(new SpinnerNumberModel(GroupNetworkBuilder.DEFAULT_DEPTH, 0, MAX_DEPTH, 1));
	private final JCheckBox partOfBox = new JCheckBox(I18N.t("label.group_network.part_of"), true);
	private final JCheckBox associateBox = new JCheckBox(I18N.t("label.group_network.associate"), true);
	private final JCheckBox subgroupMembersBox = new JCheckBox(I18N.t("label.group_network.subgroup_members"), false);
	private final JLabel title = new JLabel();

	private String centerId;
	private GroupNetwork network = GroupNetwork.empty();
	private GroupNetworkLayout.Layout layout = GroupNetworkLayout.compute(network);
	private String selectedId;
	private double zoom = 1.;
	private Consumer<String> selectionCallback;


	public GroupNetworkPanel(final FLEFModel model){
		this.model = model;

		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		toolbar.add(title);
		toolbar.add(new JLabel(I18N.t("label.group_network.depth")));
		toolbar.add(depthSpinner);
		toolbar.add(partOfBox);
		toolbar.add(associateBox);
		toolbar.add(subgroupMembersBox);
		toolbar.add(yearControl);
		toolbar.add(zoomButton("−", I18N.t("tooltip.group_network.zoom_out"), () -> setZoom(zoom / ZOOM_STEP)));
		toolbar.add(zoomButton("+", I18N.t("tooltip.group_network.zoom_in"), () -> setZoom(zoom * ZOOM_STEP)));
		toolbar.add(zoomButton("⤢", I18N.t("tooltip.group_network.zoom_fit"), this::zoomToFit));

		depthSpinner.addChangeListener(e -> refresh());
		partOfBox.addActionListener(e -> refresh());
		associateBox.addActionListener(e -> refresh());
		subgroupMembersBox.addActionListener(e -> refresh());
		yearControl.withListener(this::refresh);

		scrollPane.getViewport().setBackground(Color.WHITE);
		ViewportPanSupport.install(scrollPane, canvas);

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		add(new Legend(), BorderLayout.SOUTH);
	}


	private static JButton zoomButton(final String text, final String tooltip, final Runnable action){
		final JButton button = new JButton(text);
		button.setToolTipText(tooltip);
		button.setMargin(new java.awt.Insets(0, 6, 0, 6));
		button.addActionListener(e -> action.run());
		return button;
	}


	/**
	 * Tells the panel to be told when a group is selected.
	 *
	 * @param callback	Called with the identifier of the selected group.
	 * @return	This panel.
	 */
	public GroupNetworkPanel withSelectionCallback(final Consumer<String> callback){
		selectionCallback = callback;

		return this;
	}

	/**
	 * Puts the given group at the center of the network.
	 *
	 * @param groupId	The identifier of the group ({@code null} shows nothing).
	 */
	public void showGroup(final String groupId){
		centerId = groupId;
		selectedId = null;

		refresh();
	}

	/** Builds the network again, with the levels, the year and the choices of the toolbar (also after the data have changed). */
	public void refresh(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refresh);

			return;
		}

		network = GroupNetworkBuilder.build(model, centerId, (Integer)depthSpinner.getValue(), getYear(), kinds(),
			subgroupMembersBox.isSelected());
		layout = GroupNetworkLayout.compute(network);
		if(selectedId != null && !layout.bounds().containsKey(selectedId))
			selectedId = null;

		title.setText(network.isEmpty()? "": network.nodes().getFirst().label());
		updateSize();
		canvas.repaint();
	}

	private Set<GroupNetwork.Kind> kinds(){
		final Set<GroupNetwork.Kind> kinds = EnumSet.noneOf(GroupNetwork.Kind.class);
		if(partOfBox.isSelected())
			kinds.add(GroupNetwork.Kind.PART_OF);
		if(associateBox.isSelected())
			kinds.add(GroupNetwork.Kind.ASSOCIATE);
		return kinds;
	}

	/** @return	The year the network is restricted to, {@code null} for any. */
	public Integer getYear(){
		return yearControl.getYear();
	}

	/**
	 * Restricts the network to a year.
	 *
	 * @param year	The year, {@code null} for any.
	 */
	public void setYear(final Integer year){
		yearControl.setYear(year);
	}

	/**
	 * @param depth	How many relationships away from the center a group can be.
	 */
	public void setDepth(final int depth){
		depthSpinner.setValue(Math.clamp(depth, 0, MAX_DEPTH));
	}

	/**
	 * Shows or hides a kind of relationship (a kind that is hidden is not followed either).
	 *
	 * @param kind   the kind
	 * @param shown  whether to show it
	 */
	public void setKindShown(final GroupNetwork.Kind kind, final boolean shown){
		(kind == GroupNetwork.Kind.PART_OF? partOfBox: associateBox).setSelected(shown);

		refresh();
	}

	/**
	 * @param include	Whether the members of the subgroups count for a group.
	 */
	public void setIncludeSubgroupMembers(final boolean include){
		subgroupMembersBox.setSelected(include);

		refresh();
	}

	/**
	 * Zooms the view.
	 *
	 * @param factor	The zoom, 1 is the natural size (kept between {@value #MIN_ZOOM} and {@value #MAX_ZOOM}).
	 */
	public void setZoom(final double factor){
		zoom = Math.clamp(factor, MIN_ZOOM, MAX_ZOOM);
		updateSize();
		canvas.repaint();
	}

	/** Zooms so that the whole network is in the window, never more than the natural size. */
	public void zoomToFit(){
		final Dimension view = scrollPane.getViewport().getExtentSize();
		final Dimension size = layout.size();
		if(view.width <= 0 || view.height <= 0 || size.width <= 0 || size.height <= 0)
			return;

		setZoom(Math.min(1., Math.min((double)view.width / size.width, (double)view.height / size.height)));
	}

	/** @return	The zoom, 1 is the natural size. */
	public double getZoom(){
		return zoom;
	}

	/** @return	The network shown. */
	public GroupNetwork getNetwork(){
		return network;
	}

	/** @return	The identifier of the group at the center, {@code null} if none. */
	public String getCenterId(){
		return centerId;
	}

	/** @return	The identifier of the selected group, {@code null} if none. */
	public String getSelectedId(){
		return selectedId;
	}

	/** @return	The rectangle of a group in the layout (before the zoom), {@code null} if it is not shown. */
	Rectangle nodeBounds(final String groupId){
		return layout.bounds().get(groupId);
	}

	/**
	 * @param point	A point of the layout (before the zoom).
	 * @return	The text of the tooltip there: about the group, or the relationship, under the point; {@code null} if there is none.
	 */
	String toolTipAt(final Point point){
		final String id = groupAt(point);
		if(id != null)
			for(final GroupNetwork.Node node : network.nodes())
				if(node.id().equals(id))
					return I18N.tf("tooltip.group_network.node", node.label(), node.members(), node.id());

		final GroupNetwork.Edge edge = edgeAt(point, EDGE_HIT_DISTANCE / zoom);
		return (edge != null? describe(edge): null);
	}


	private void updateSize(){
		final Dimension size = layout.size();
		canvas.setPreferredSize(new Dimension((int)Math.ceil(size.width * zoom), (int)Math.ceil(size.height * zoom)));
		canvas.revalidate();
	}

	private String groupAt(final Point point){
		for(final Map.Entry<String, Rectangle> entry : layout.bounds().entrySet())
			if(entry.getValue().contains(point))
				return entry.getKey();
		return null;
	}

	private GroupNetwork.Edge edgeAt(final Point point, final double distance){
		GroupNetwork.Edge nearest = null;
		double nearestDistance = distance;
		for(final GroupNetwork.Edge edge : network.edges())
			for(final Line2D segment : segments(edge)){
				final double d = segment.ptSegDist(point);
				if(d <= nearestDistance){
					nearest = edge;
					nearestDistance = d;
				}
			}
		return nearest;
	}

	private String describe(final GroupNetwork.Edge edge){
		final String first = labelOf(edge.firstId());
		final String second = labelOf(edge.secondId());
		final StringBuilder sb = new StringBuilder(I18N.tf(
			(edge.kind() == GroupNetwork.Kind.PART_OF? "tooltip.group_network.part_of": "tooltip.group_network.associate"), first, second));
		if(StringUtils.isNotBlank(edge.role()))
			sb.append(" - ").append(I18N.tf("tooltip.group_network.role", edge.role()));
		if(StringUtils.isNotBlank(edge.dates()))
			sb.append(" - ").append(edge.dates().strip());
		return sb.toString();
	}

	private String labelOf(final String groupId){
		for(final GroupNetwork.Node node : network.nodes())
			if(node.id().equals(groupId))
				return node.label();
		return groupId;
	}

	/** The segments of the line of a relationship, in the coordinates of the layout. */
	private List<Line2D> segments(final GroupNetwork.Edge edge){
		final Rectangle first = layout.bounds().get(edge.firstId());
		final Rectangle second = layout.bounds().get(edge.secondId());
		if(first == null || second == null)
			return List.of();

		//a part is under its whole; an association from the upper box to the lower one
		if(edge.kind() == GroupNetwork.Kind.PART_OF)
			return stepped(second, first);
		return (first.y <= second.y? stepped(first, second): stepped(second, first));
	}

	/** A line from the upper box to the lower one: down from the bottom of the first, across, down to the top of the second. */
	private static List<Line2D> stepped(final Rectangle upper, final Rectangle lower){
		if(upper.y == lower.y){
			//the same level: side by side
			final Rectangle left = (upper.x <= lower.x? upper: lower);
			final Rectangle right = (left == upper? lower: upper);
			return List.of(new Line2D.Double(left.x + left.width, left.y + left.height / 2., right.x, right.y + right.height / 2.));
		}

		final double x1 = upper.x + upper.width / 2.;
		final double y1 = upper.y + upper.height;
		final double x2 = lower.x + lower.width / 2.;
		final double y2 = lower.y;
		final double middle = (y1 + y2) / 2.;
		final List<Line2D> segments = new ArrayList<>(3);
		segments.add(new Line2D.Double(x1, y1, x1, middle));
		segments.add(new Line2D.Double(x1, middle, x2, middle));
		segments.add(new Line2D.Double(x2, middle, x2, y2));
		return segments;
	}

	private static Point toLayout(final Point screen, final double zoom){
		return new Point((int)Math.round(screen.x / zoom), (int)Math.round(screen.y / zoom));
	}


	private final class Canvas extends JPanel{

		private Canvas(){
			setBackground(Color.WHITE);
			ToolTipManager.sharedInstance().registerComponent(this);
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(!SwingUtilities.isLeftMouseButton(e))
						return;

					final String id = groupAt(toLayout(e.getPoint(), zoom));
					if(id == null)
						return;

					if(e.getClickCount() == 2)
						showGroup(id);
					else{
						selectedId = id;
						repaint();
						if(selectionCallback != null)
							selectionCallback.accept(id);
					}
				}
			});
			addMouseWheelListener(e -> {
				if(e.isControlDown())
					setZoom(zoom * (e.getWheelRotation() < 0? ZOOM_STEP: 1. / ZOOM_STEP));
				else
					//the wheel scrolls, as it would without this listener
					scrollPane.dispatchEvent(translated(e));
			});
		}

		private MouseWheelEvent translated(final MouseWheelEvent e){
			return (MouseWheelEvent)SwingUtilities.convertMouseEvent(this, e, scrollPane);
		}


		@Override
		public String getToolTipText(final MouseEvent event){
			return toolTipAt(toLayout(event.getPoint(), zoom));
		}

		@Override
		protected void paintComponent(final Graphics graphics){
			super.paintComponent(graphics);

			final Graphics2D g = (Graphics2D)graphics.create();
			try{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.scale(zoom, zoom);

				for(final GroupNetwork.Edge edge : network.edges())
					drawEdge(g, edge);
				for(final GroupNetwork.Node node : network.nodes())
					drawNode(g, node);
			}
			finally{
				g.dispose();
			}
		}

		private void drawEdge(final Graphics2D g, final GroupNetwork.Edge edge){
			g.setColor(COLOR_EDGE);
			g.setStroke(edge.kind() == GroupNetwork.Kind.PART_OF? STROKE_PART_OF: STROKE_ASSOCIATE);
			for(final Line2D segment : segments(edge))
				g.draw(segment);
		}

		private void drawNode(final Graphics2D g, final GroupNetwork.Node node){
			final Rectangle box = layout.bounds().get(node.id());
			if(box == null)
				return;

			g.setStroke(new BasicStroke(node.id().equals(selectedId)? 2.4f: 1.2f));
			g.setColor(node.center()? COLOR_CENTER: (node.id().equals(selectedId)? COLOR_SELECTED: COLOR_NODE));
			g.fillRoundRect(box.x, box.y, box.width, box.height, 12, 12);
			g.setColor(COLOR_BORDER);
			g.drawRoundRect(box.x, box.y, box.width, box.height, 12, 12);

			g.setColor(COLOR_TEXT);
			final Font original = g.getFont();
			g.setFont(original.deriveFont(node.center()? Font.BOLD: Font.PLAIN));
			drawLabel(g, node.label(), box);
			g.setFont(original);

			//the number of members
			final int centerX = box.x + box.width - BADGE_RADIUS - 2;
			final int centerY = box.y + BADGE_RADIUS + 2;
			drawBadge(g, centerX, centerY, Integer.toString(node.members()));
		}

		/** The name of the group, on two lines if it does not fit in one; what does not fit in two is cut (the tooltip has all of it). */
		private void drawLabel(final Graphics2D g, final String label, final Rectangle box){
			final FontMetrics metrics = g.getFontMetrics();
			final int width = box.width - 2 * BADGE_RADIUS - 10;
			final List<String> lines = wrap(metrics, label, width);
			final int lineHeight = metrics.getHeight();
			final int top = box.y + (box.height - lines.size() * lineHeight) / 2 + metrics.getAscent();
			for(int i = 0; i < lines.size(); i ++)
				g.drawString(lines.get(i), box.x + 8, top + i * lineHeight);
		}

	}


	/**
	 * @param metrics   the font metrics
	 * @param text      the text
	 * @param maxWidth  the width of a line
	 * @return	The text on one line if it fits, otherwise on two, broken at the space nearest the middle, the second cut with an ellipsis if
	 * 	it is still too long.
	 */
	static List<String> wrap(final FontMetrics metrics, final String text, final int maxWidth){
		if(metrics.stringWidth(text) <= maxWidth)
			return List.of(text);

		final int middle = text.length() / 2;
		int split = -1;
		for(int offset = 0; offset < text.length(); offset ++){
			if(middle - offset >= 0 && text.charAt(middle - offset) == ' '){
				split = middle - offset;
				break;
			}
			if(middle + offset < text.length() && text.charAt(middle + offset) == ' '){
				split = middle + offset;
				break;
			}
		}
		if(split < 0)
			return List.of(truncate(metrics, text, maxWidth));
		return List.of(truncate(metrics, text.substring(0, split), maxWidth), truncate(metrics, text.substring(split + 1), maxWidth));
	}

	private static String truncate(final FontMetrics metrics, final String text, final int maxWidth){
		if(metrics.stringWidth(text) <= maxWidth)
			return text;

		int end = text.length();
		while(end > 0 && metrics.stringWidth(text.substring(0, end) + "…") > maxWidth)
			end --;
		return (end > 0? text.substring(0, end) + "…": "…");
	}

	private static void drawBadge(final Graphics2D g, final int centerX, final int centerY, final String text){
		g.setColor(COLOR_BADGE);
		g.fillOval(centerX - BADGE_RADIUS, centerY - BADGE_RADIUS, 2 * BADGE_RADIUS, 2 * BADGE_RADIUS);
		g.setColor(Color.WHITE);
		final Font original = g.getFont();
		g.setFont(original.deriveFont(Font.BOLD, original.getSize2D() - 1f));
		final FontMetrics metrics = g.getFontMetrics();
		g.drawString(text, centerX - metrics.stringWidth(text) / 2, centerY + (metrics.getAscent() - metrics.getDescent()) / 2);
		g.setFont(original);
	}


	/** What the lines, the badge and the highlighted box mean. */
	private static final class Legend extends JPanel{

		private static final int HEIGHT = 26;
		private static final int LINE_LENGTH = 34;

		private Legend(){
			setPreferredSize(new Dimension(10, HEIGHT));
			setBackground(Color.WHITE);
			setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xC8C8C8)));
		}


		@Override
		protected void paintComponent(final Graphics graphics){
			super.paintComponent(graphics);

			final Graphics2D g = (Graphics2D)graphics.create();
			try{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				final FontMetrics metrics = g.getFontMetrics();
				final int y = HEIGHT / 2 + 1;
				int x = 10;

				g.setColor(COLOR_EDGE);
				g.setStroke(STROKE_PART_OF);
				g.drawLine(x, y, x + LINE_LENGTH, y);
				x = text(g, metrics, I18N.t("legend.group_network.part_of"), x + LINE_LENGTH + 6, y) + 22;

				g.setColor(COLOR_EDGE);
				g.setStroke(STROKE_ASSOCIATE);
				g.drawLine(x, y, x + LINE_LENGTH, y);
				x = text(g, metrics, I18N.t("legend.group_network.associate"), x + LINE_LENGTH + 6, y) + 22;

				drawBadge(g, x + BADGE_RADIUS, y, "n");
				x = text(g, metrics, I18N.t("legend.group_network.members"), x + 2 * BADGE_RADIUS + 6, y) + 22;

				g.setStroke(new BasicStroke(1.2f));
				g.setColor(COLOR_CENTER);
				g.fillRoundRect(x, y - 7, 22, 14, 6, 6);
				g.setColor(COLOR_BORDER);
				g.drawRoundRect(x, y - 7, 22, 14, 6, 6);
				text(g, metrics, I18N.t("legend.group_network.center"), x + 28, y);
			}
			finally{
				g.dispose();
			}
		}

		/** @return	The x after the text. */
		private static int text(final Graphics2D g, final FontMetrics metrics, final String text, final int x, final int y){
			g.setColor(COLOR_TEXT);
			g.drawString(text, x, y + (metrics.getAscent() - metrics.getDescent()) / 2);
			return x + metrics.stringWidth(text);
		}

	}

}
