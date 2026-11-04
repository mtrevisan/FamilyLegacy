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
package io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline;

import io.github.mtrevisan.familylegacy.groups.GroupTimelineBuilder;
import io.github.mtrevisan.familylegacy.groups.IndividualCareerBuilder;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrack;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalAxis;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalSpanRenderer;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalZoomController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/** Zooming a group view: every bar, undated and open ones included, is placed and drawn at every zoom, and it follows the zoom. */
class GroupViewZoomTest{

	private static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";

	//Anna is the president of the Club from 1900 to 1910 and then a member with no end; she is a guest of Youth with no date, and of the
	//Rivals from 1920 to 1925 (ended)
	private static final String FLEF = HEADER
		+ "individual I1 {\n\tname {\n\t\tpart {\n\t\t\ttype given\n\t\t\tvalue Anna\n\t\t}\n\t}\n}\n"
		+ group("G1", "Club") + group("G2", "Youth") + group("G3", "Rivals")
		+ membership("R1", "G1", "president", "1900-01-01", "1910-01-01", null)
		+ membership("R2", "G1", "member", "1910-01-01", null, null)
		+ membership("R3", "G2", "guest", null, null, null)
		+ membership("R4", "G3", "guest", "1920-01-01", "1925-01-01", "ended");


	private static String group(final String id, final String name){
		return "group " + id + " {\n\tname {\n\t\ttype legal\n\t\tvalue " + name + "\n\t}\n}\n";
	}

	private static String membership(final String id, final String target, final String role, final String from, final String to,
			final String status){
		final StringBuilder sb = new StringBuilder("relationship " + id + " {\n\tsubject.individual I1\n\ttarget.group " + target
			+ "\n\ttype group_member\n\trole " + role + "\n");
		if(from != null)
			sb.append("\tvalid_from.value.point.value.full_date ").append(from).append('\n');
		if(to != null)
			sb.append("\tvalid_to.value.point.value.full_date ").append(to).append('\n');
		if(status != null)
			sb.append("\tstatus ").append(status).append('\n');
		return sb.append("}\n").toString();
	}

	private static TemporalProjectionModel career(){
		final FLEFModel model = new FLEFParser().parse(FLEF);
		return IndividualCareerBuilder.build(model, "I1");
	}

	private static List<TemporalEntry> entries(final TemporalProjectionModel model){
		final List<TemporalEntry> entries = new ArrayList<>();
		for(final TemporalRow row : model.rows())
			for(final TemporalTrack track : row.tracks())
				entries.addAll(track.entries());
		return entries;
	}

	private static TemporalAxis axis(final TemporalProjectionModel model){
		final TemporalAxis axis = new TemporalAxis(model.domainStart(), model.domainEnd());
		axis.setViewportWidth(800);
		axis.fitToDomain();
		return axis;
	}

	/** Places and draws every bar on the axis as it is now. */
	private static List<Rectangle> drawAll(final TemporalProjectionModel model, final TemporalAxis axis){
		final BufferedImage image = new BufferedImage(900, 40, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final List<Rectangle> rectangles = new ArrayList<>();
		try{
			for(final TemporalEntry entry : entries(model)){
				final Rectangle rect = axis.spanToRect(entry.span(), 5, 14);
				rectangles.add(rect);
				if(rect != null)
					TemporalSpanRenderer.drawSpan(g, rect, entry.span(), new Color(70, 120, 190), false);
			}
		}
		finally{
			g.dispose();
		}
		return rectangles;
	}


	@Test
	void everyBarCanBeZoomedAndDrawnAtEveryLevel(){
		final TemporalProjectionModel model = career();
		final TemporalAxis axis = axis(model);
		final TemporalZoomController zoom = new TemporalZoomController(() -> axis);

		drawAll(model, axis);
		for(int i = 0; i < 12; i ++){
			zoom.zoomAtCursor(true, 400);
			drawAll(model, axis);
		}
		for(int i = 0; i < 24; i ++){
			zoom.zoomAtCursor(false, 400);
			drawAll(model, axis);
		}
	}

	@Test
	void theBarsGetWiderWhenZoomedIn(){
		final TemporalProjectionModel model = career();
		final TemporalAxis axis = axis(model);
		final TemporalZoomController zoom = new TemporalZoomController(() -> axis);
		final TemporalEntry dated = entries(model).stream()
			.filter(entry -> entry.label().startsWith("guest") && !entry.label().contains("dates"))
			.findFirst()
			.orElseThrow();

		final Rectangle before = axis.spanToRect(dated.span(), 0, 10);
		//the axis starts on the first date: zoom in around the date of the bar
		final int anchor = before.x + before.width / 2;
		zoom.zoomAtCursor(true, anchor);
		final Rectangle after = axis.spanToRect(dated.span(), 0, 10);

		Assertions.assertNotNull(before);
		Assertions.assertNotNull(after);
		Assertions.assertTrue(after.width > before.width, "a bar is wider when zoomed in: " + before.width + " -> " + after.width);
	}

	/** The component of the panel that shows the bars: it is the one that zooms with Ctrl and the wheel. */
	private static Component contentCanvas(final Component root){
		if(root.getClass().getSimpleName().equals("ContentCanvas"))
			return root;
		if(root instanceof Container container)
			for(final Component child : container.getComponents()){
				final Component found = contentCanvas(child);
				if(found != null)
					return found;
			}
		return null;
	}

	private static BufferedImage paint(final JComponent panel){
		final BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		try{
			panel.paint(g);
		}
		finally{
			g.dispose();
		}
		return image;
	}

	private static boolean sameImage(final BufferedImage a, final BufferedImage b){
		for(int y = 0; y < a.getHeight(); y ++)
			for(int x = 0; x < a.getWidth(); x ++)
				if(a.getRGB(x, y) != b.getRGB(x, y))
					return false;
		return true;
	}

	/** Lets Swing process what is queued (the resize of a window is told to its components only then). */
	private static void flush() throws InvocationTargetException, InterruptedException{
		for(int i = 0; i < 3; i ++)
			SwingUtilities.invokeAndWait(() -> {});
	}

	private static MouseWheelEvent wheel(final Component canvas, final int rotation){
		return new MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), InputEvent.CTRL_DOWN_MASK, 300, 20, 0, false,
			MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation);
	}

	/**
	 * Shows a view in a window and zooms it with Ctrl and the wheel, in and out, painting it after every step.
	 *
	 * @param factory  creates the view, already showing what it has to show
	 * @param name     the name of the images saved when the environment variable GROUP_VIEW_IMAGES names a folder
	 */
	private static void zoomsWithTheWheel(final Supplier<JComponent> factory, final String name)
			throws InvocationTargetException, InterruptedException{
		final JComponent[] view = new JComponent[1];
		final JFrame[] frame = new JFrame[1];
		final List<BufferedImage> images = new ArrayList<>();
		try{
			SwingUtilities.invokeAndWait(() -> {
				view[0] = factory.get();
				frame[0] = new JFrame();
				frame[0].add(view[0]);
				frame[0].setSize(1000, 420);
				frame[0].setVisible(true);
			});
			//the window is shown: its components are told their size, and the axis its width
			flush();

			SwingUtilities.invokeAndWait(() -> {
				final Component canvas = contentCanvas(view[0]);
				Assertions.assertNotNull(canvas, "the content of the projection");
				images.add(paint(view[0]));
				for(int i = 0; i < 6; i ++){
					canvas.dispatchEvent(wheel(canvas, -1));
					images.add(paint(view[0]));
				}
				for(int i = 0; i < 12; i ++){
					canvas.dispatchEvent(wheel(canvas, 1));
					images.add(paint(view[0]));
				}
				save(images.get(0), name + "-0-start");
				save(images.get(3), name + "-1-zoomed-in");
				save(images.getLast(), name + "-2-zoomed-out");
			});
		}
		finally{
			SwingUtilities.invokeAndWait(() -> {
				if(frame[0] != null)
					frame[0].dispose();
			});
		}

		Assertions.assertFalse(sameImage(images.get(0), images.get(3)), "the bars change with the zoom");
		Assertions.assertFalse(sameImage(images.get(3), images.getLast()), "and again when zooming out");
	}

	@Test
	void theCareerPanelZoomsWithTheWheelAndRedrawsEveryTime() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);

		zoomsWithTheWheel(() -> {
			final IndividualCareerPanel panel = new IndividualCareerPanel(model);
			panel.showIndividual("I1");
			return panel;
		}, "career");
	}

	@Test
	void theTimelinePanelZoomsWithTheWheelAndRedrawsEveryTime() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The views need a display");
		final FLEFModel model = new FLEFParser().parse(FLEF);

		zoomsWithTheWheel(() -> {
			final GroupTimelinePanel panel = new GroupTimelinePanel(model);
			panel.showGroup("G1");
			return panel;
		}, "timeline");
	}

	@Test
	void aBarWithNoEndIsSeenBecauseTheAxisGoesOnAfterItsStart(){
		//on the timeline of the Club the last date is the end of the presidency, 1 January 1910, and the membership begins there
		final TemporalProjectionModel timeline = GroupTimelineBuilder.build(new FLEFParser().parse(FLEF), "G1");

		Assertions.assertEquals(2_415_021L, timeline.domainStart().jdn(), "1 January 1900");
		Assertions.assertTrue(timeline.domainEnd().jdn() >= 2_418_673L + 365L, "the axis goes on after the start of the open bar: "
			+ timeline.domainEnd().jdn());

		final TemporalAxis axis = axis(timeline);
		final TemporalEntry member = entries(timeline).stream().filter(entry -> entry.role().equals("member")).findFirst().orElseThrow();
		final Rectangle rect = axis.spanToRect(member.span(), 0, 10);
		Assertions.assertNotNull(rect, "the open bar is on the screen");
		Assertions.assertTrue(rect.width > 20, "and it is wide enough to be seen: " + rect.width);
	}

	@Test
	void aBarOutsideTheWindowIsClippedOrLeftOutAndNeverThrows(){
		final TemporalProjectionModel model = career();
		final TemporalAxis axis = axis(model);
		final TemporalZoomController zoom = new TemporalZoomController(() -> axis);
		//zoomed in on 1910: the bar of the president ends there, the member begins, the Rivals are far on the right
		final TemporalEntry member = entries(model).stream().filter(entry -> entry.role().equals("member")).findFirst().orElseThrow();
		final TemporalEntry president = entries(model).stream().filter(entry -> entry.role().equals("president")).findFirst().orElseThrow();
		final TemporalEntry rivals = entries(model).stream().filter(entry -> entry.label().contains("ended")).findFirst().orElseThrow();
		final int anchor = axis.spanToRect(member.span(), 0, 10).x;
		for(int i = 0; i < 6; i ++)
			zoom.zoomAtCursor(true, anchor);

		final Rectangle partlyOutside = axis.spanToRect(member.span(), 0, 10);
		Assertions.assertNotNull(partlyOutside, "the bar of the member begins in the window and goes beyond its right edge");
		Assertions.assertEquals(axis.viewportWidth(), partlyOutside.x + partlyOutside.width, "clipped at the right edge");
		Assertions.assertNull(axis.spanToRect(rivals.span(), 0, 10), "far on the right: not seen");
		final Rectangle ends = axis.spanToRect(president.span(), 0, 10);
		Assertions.assertTrue(ends == null || ends.x >= 0, "a bar that ends before the window is left out, not drawn as a sliver");
	}

	private static void save(final BufferedImage image, final String name){
		final String dir = System.getenv("GROUP_VIEW_IMAGES");
		if(dir == null)
			return;

		try{
			ImageIO.write(image, "png", new java.io.File(dir, name + ".png"));
		}
		catch(final java.io.IOException e){
			throw new java.io.UncheckedIOException(e);
		}
	}

}
